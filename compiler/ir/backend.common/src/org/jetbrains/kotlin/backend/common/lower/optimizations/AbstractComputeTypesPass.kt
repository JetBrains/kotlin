/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.optimizations

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.config.logMultiple
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.isFinalClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.atMostOne
import org.jetbrains.kotlin.utils.copy
import org.jetbrains.kotlin.utils.forEachBit
import org.jetbrains.kotlin.utils.mapEachBit
import java.util.BitSet

/**
 * Uses a local data flow analysis to compute more concrete types for some IR nodes (variables, `IrGetValue`s and
 * control flow merge points), and drops the `IMPLICIT_CAST`s which become redundant as a result.
 *
 * The inliner erases generics aggressively, so after inlining a lot of IR nodes have a much less concrete type
 * than the values actually flowing into them. Restoring some of that information allows the backends to avoid
 * generating redundant casts and (un)boxing.
 *
 * The only backend-specific bit is [getInlinedClassOrNull], which describes the type's unboxed representation.
 */
abstract class AbstractComputeTypesPass(val context: LoweringContext) : BodyLoweringPass, ProvenImplicitCastBuilder {
    private val unitType = context.irBuiltIns.unitType

    /**
     * The class whose representation is used to store values of this type unboxed, or `null` if values of this type
     * are stored as a boxed reference.
     *
     * Used to avoid switching a type to one with a different boxing, which would change the generated code
     * in a way that is surprising for the user (see KT-86678 and KT-87165).
     */
    protected abstract fun IrType.getInlinedClassOrNull(): IrClass?

    /**
     * Whether a value of this type may have its type replaced with a computed one.
     *
     * A backend may give a type a meaning beyond its erased class: the types in Wasm's `kotlin.wasm.internal.reftypes`
     * encode the wasm type in their type arguments, which [leastCommonAncestor] would drop.
     */
    protected open fun isTypeReplacementAllowed(type: IrType): Boolean = true

    private fun IrClass.superClassesHierarchy(): List<IrClass> {
        val result = mutableListOf<IrClass>()
        var clazz = this
        while (!clazz.isAny()) {
            result.add(clazz)
            val superClass = clazz.superTypes.map { it.erasedUpperBound }.atMostOne { !it.isInterface }
                    ?: context.irBuiltIns.anyClass.owner
            clazz = superClass
        }
        result.add(clazz)

        result.reverse()
        return result
    }

    private fun leastCommonAncestor(types: List<IrType>): IrType? {
        if (types.isEmpty()) return null
        if (types.size == 1) return types[0]

        val isNullable = types.any { it.isNullable() }
        val classes = types.map { it.erasedUpperBound }
        // Since the analysis is local, nothing we can do about interfaces: if an interface is written to a variable,
        // we cannot replace its type with some class without knowing the whole types' hierarchy.
        if (classes.any { it.isInterface }) return null
        var commonAncestor = classes[0]
        var superClasses = commonAncestor.superClassesHierarchy()
        for (i in 1 until classes.size) {
            if (commonAncestor.isAny()) break
            val curClass = classes[i]
            val curSuperClasses = curClass.superClassesHierarchy()
            if (commonAncestor in curSuperClasses)
                continue
            if (curClass in superClasses) {
                commonAncestor = curClass
                superClasses = curSuperClasses
                continue
            }
            var idx = 0
            while (idx < superClasses.size && idx < curSuperClasses.size && superClasses[idx] == curSuperClasses[idx])
                ++idx
            commonAncestor = superClasses[idx - 1]
            superClasses = superClasses.take(idx)
        }

        return commonAncestor.defaultType.let { if (isNullable) it.makeNullable() else it }
    }

    /**
     * Whether this `IMPLICIT_CAST` is a no-op, i.e. its argument's type already conveys everything the cast does,
     * so that the cast can be dropped and the argument's (possibly more concrete) type used in its place.
     */
    private fun IrTypeOperatorCall.isRedundantImplicitCast(): Boolean {
        if (this.operator != IrTypeOperator.IMPLICIT_CAST) return false
        // The cast makes the value nullable, which the argument's type does not express.
        if (!this.typeOperand.isNullable() && this.type.isNullable()) return false
        // The cast removes the nullability, which the argument's type does not express either. Dropping it would
        // *widen* the computed type instead of narrowing it, and for a type whose nullability decides its
        // representation (`Char?` vs `Char` in Wasm) that also breaks the generated code.
        if (!this.typeOperand.isNullable() && this.argument.type.isNullable()) return false
        // The cast also boxes or unboxes the value, and that conversion is not recoverable from the use site
        // alone: the autoboxing lowerings do not re-derive the result type inside an `IrInlinedFunctionBlock`.
        if (this.argument.type.getInlinedClassOrNull() != this.typeOperand.getInlinedClassOrNull()) return false
        return this.argument.type.erasedUpperBound.symbol.isSubtypeOfClass(this.typeOperand.erasedUpperBound.symbol)
    }

    private fun IrTypeOperatorCall.tryShortcutToArgument(): IrType? {
        if (!isRedundantImplicitCast()) return null
        return (this.argument as? IrTypeOperatorCall)?.tryShortcutToArgument() ?: this.argument.type
    }

    /**
     * Whether the computed [this] type may replace [declaredType].
     *
     * The point of this pass is to compute *more* concrete types, and replacing a type with a wider one is not just
     * useless but wrong: the reads of the value have already been type checked against the declared type, and some
     * backends rely on the declared type to insert the narrowing (`WasmTypeOperatorLowering` does so for variable
     * initializers, so a Wasm variable initializer may legitimately be wider than the variable itself).
     */
    private fun IrType.isAtLeastAsConcreteAs(declaredType: IrType): Boolean =
            isTypeReplacementAllowed(declaredType) && isTypeReplacementAllowed(this)
                    && (declaredType.isNullable() || !this.isNullable())
                    && this.erasedUpperBound.symbol.isSubtypeOfClass(declaredType.erasedUpperBound.symbol)

    private fun List<IrExpression>.computeType() = leastCommonAncestor(
            this.map { (it as? IrTypeOperatorCall)?.tryShortcutToArgument() ?: it.type }
                    .distinct()
                    .filterNot { it.isNothing() }
    )

    private fun IrElement.getImmediateChildren(): List<IrElement> {
        val result = mutableListOf<IrElement>()
        acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                result.add(element)
                // Do not recurse.
            }
        })
        return result
    }

    /*
     * The ultimate goal of this pass is to use DFA to compute more concrete types of some nodes.
     * Each visitXXX function in the visitor below takes variables values ~before~ the expression and returns
     * variables values ~after~ the expression. The variables values are represented as bit sets: one bit is reserved for
     * each variable write (including phi nodes). The pass is more or less straightforward except for two tricks:
     * one for loops and another for try/catch blocks.
     *
     * Let's start with try/catch blocks. In theory, an exception might be thrown anywhere inside the try clause and then
     * caught by one of the catch clauses. But maintaining precise CFG/phi node for it is kind of ridiculous (too many incoming edges),
     * so a simple approximation is used: save all possible variable writes inside the try clause and promote them to the catch clauses.
     *
     * As for the loops, the usual trick is to iterate the algorithm and merge the results of each iteration
     * until a stable point is reached. Here it's possible to do this merge only for IrGetValue nodes because only they use the
     * variables values being computed (other nodes also use them of course but indirectly).
     */

    private data class VariableWrite(val variable: IrElement, val value: IrExpression)

    private class ControlFlowMergePointInfo(val variable: IrElement, val needValues: Boolean) {
        val variablesValues = BitSet()
        val variableWrites = if (needValues) BitSet() else null
    }

    // Some variables (catch block parameters and suspension point id parameters) are initialized by runtime.
    // Their usages look like uninitialized variable accesses. This is circumvented by putting some non-null value for their writes.
    private val externalWrites = BitSet()
    private val nothingValue = BitSet()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        context.log { "Analyzing ${container.render()}" }

        val allVariablesWrites = mutableListOf<VariableWrite>()
        val variableWriteMap = mutableMapOf<VariableWrite, Int>()
        val variableWrites = mutableMapOf<IrVariable, BitSet>()

        fun BitSet.computeType() = this.mapEachBit { allVariablesWrites[it].value }.computeType()

        fun IrType.trySwitchVariableType(variable: IrVariable) =
                this.takeUnless {
                    // Conservatively handle source code defined variables: if changing the type also alters the boxing,
                    // this might be surprising for the programmer. See KT-86678 and KT-87165 for examples.
                    (variable.origin == IrDeclarationOrigin.DEFINED
                            && this.getInlinedClassOrNull() != variable.type.getInlinedClassOrNull())
                }

        irBody.accept(object : IrVisitor<BitSet, BitSet>() {
            fun getVariableWriteId(variable: IrElement, value: IrExpression) = VariableWrite(variable, value).let { write ->
                variableWriteMap.getOrPut(write) {
                    allVariablesWrites.add(write)
                    val index = allVariablesWrites.size - 1
                    (variable as? IrVariable)?.let {
                        variableWrites.getOrPut(it) { BitSet() }.set(index)
                    }
                    index
                }
            }

            private fun BitSet.format() = buildString {
                append('[')
                var first = true
                forEachBit {
                    if (!first) append(", ")
                    first = false
                    val (variable, value) = allVariablesWrites[it]
                    append((variable as? IrVariable)?.name ?: variable::class.java)
                    append(" = ")
                    append(value::class.java)
                }
                append(']')
            }

            val dummyUnitExpression = IrGetObjectValueImpl(
                    irBody.startOffset, irBody.endOffset, unitType, context.irBuiltIns.unitClass
            )

            // A simplification for handling try/catch blocks: this BitSet stores all the variable writes inside a try clause.
            // This allows for the corresponding catch clauses to see those writes (even when they are overwritten by control flow).
            var catchesVariablesValues: BitSet? = null
            val returnableBlockCFMPInfos = mutableMapOf<IrReturnableBlockSymbol, ControlFlowMergePointInfo>()
            val breaksCFMPInfos = mutableMapOf<IrLoop, ControlFlowMergePointInfo>()
            val continuesCFMPInfos = mutableMapOf<IrLoop, ControlFlowMergePointInfo>()
            val getValueVariablesWrites = mutableMapOf<IrGetValue, BitSet>()
            val doWhileLoopForWhileLoops = mutableMapOf<IrWhileLoop, IrDoWhileLoop>()

            // A merge point's `needValues` must be derived from a node's original type. Otherwise,
            // once a merge point's type becomes a final class, it can never be widened (KT-86949).
            // (The types may iteratively change while handling loops).
            val cfmpNeedValues = mutableMapOf<IrExpression, Boolean>()

            // A factory mimicking the constructor.
            fun ControlFlowMergePointInfo(variable: IrElement) = ControlFlowMergePointInfo(
                    variable,
                    needValues = variable is IrExpression
                            && cfmpNeedValues.getOrPut(variable) { !variable.type.erasedUpperBound.isFinalClass }
            )

            fun controlFlowMergePoint(cfmpInfo: ControlFlowMergePointInfo, value: IrExpression, variablesValues: BitSet): BitSet {
                val result = if (!cfmpInfo.needValues)
                    variablesValues
                else {
                    val id = getVariableWriteId(cfmpInfo.variable, value)
                    cfmpInfo.variableWrites!!.set(id)
                    variablesValues.copy().apply { set(id) }
                }

                cfmpInfo.variablesValues.or(result)
                return result
            }

            override fun visitElement(element: IrElement, data: BitSet): BitSet {
                var result = data
                for (node in element.getImmediateChildren())
                    result = node.accept(this, result)
                return result
            }

            override fun visitReturn(expression: IrReturn, data: BitSet): BitSet {
                val result = expression.value.accept(this, data)
                (expression.returnTargetSymbol as? IrReturnableBlockSymbol)?.let {
                    val cfmpInfo = returnableBlockCFMPInfos[it] ?: error("Unknown returnable block for ${expression.render()}")
                    controlFlowMergePoint(cfmpInfo, expression.value, result)
                }

                return nothingValue
            }

            override fun visitBlock(expression: IrBlock, data: BitSet): BitSet {
                val irReturnableBlock = expression as? IrReturnableBlock
                return if (irReturnableBlock == null) {
                    val result = visitElement(expression, data)
                    (expression.statements.lastOrNull() as? IrExpression)?.type
                            ?.takeIf { it.isAtLeastAsConcreteAs(expression.type) }
                            ?.let { expression.type = it }
                    result
                } else {
                    val cfmpInfo = ControlFlowMergePointInfo(expression)
                    returnableBlockCFMPInfos[irReturnableBlock.symbol] = cfmpInfo
                    visitElement(expression, data)
                    returnableBlockCFMPInfos.remove(irReturnableBlock.symbol)
                    cfmpInfo.variableWrites?.computeType()?.takeIf { it.isAtLeastAsConcreteAs(expression.type) }?.let { expression.type = it }
                    cfmpInfo.variablesValues
                }
            }

            override fun visitWhen(expression: IrWhen, data: BitSet): BitSet {
                val cfmpInfo = ControlFlowMergePointInfo(expression)
                var result = data
                for (branch in expression.branches) {
                    result = branch.condition.accept(this, result)
                    val branchResult = branch.result.accept(this, result)
                    controlFlowMergePoint(cfmpInfo, branch.result, branchResult)
                }
                val isExhaustive = expression.branches.last().isUnconditional()
                if (isExhaustive) {
                    cfmpInfo.variableWrites?.computeType()?.takeIf { it.isAtLeastAsConcreteAs(expression.type) }?.let { expression.type = it }
                } else {
                    // A non-exhaustive when always has type Unit (or Nothing).
                    controlFlowMergePoint(cfmpInfo, dummyUnitExpression, result)
                }

                return cfmpInfo.variablesValues
            }

            override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: BitSet): BitSet {
                variableWrites[expression.suspensionPointIdParameter] = externalWrites
                val cfmpInfo = ControlFlowMergePointInfo(expression)
                val resultVV = expression.result.accept(this, data)
                controlFlowMergePoint(cfmpInfo, expression.result, resultVV)
                val resumeResultVV = expression.resumeResult.accept(this, data)
                controlFlowMergePoint(cfmpInfo, expression.resumeResult, resumeResultVV)

                return cfmpInfo.variablesValues
            }

            override fun visitTry(aTry: IrTry, data: BitSet): BitSet {
                val prevCatchesVV = catchesVariablesValues
                val catchesVV = data.copy()
                catchesVariablesValues = catchesVV
                val cfmpInfo = ControlFlowMergePointInfo(aTry)
                val tryVV = aTry.tryResult.accept(this, data)
                controlFlowMergePoint(cfmpInfo, aTry.tryResult, tryVV)
                prevCatchesVV?.or(catchesVV)
                catchesVariablesValues = prevCatchesVV
                for (aCatch in aTry.catches) {
                    variableWrites[aCatch.catchParameter] = externalWrites
                    val catchVV = aCatch.result.accept(this, catchesVV)
                    controlFlowMergePoint(cfmpInfo, aCatch.result, catchVV)
                }
                cfmpInfo.variableWrites?.computeType()?.takeIf { it.isAtLeastAsConcreteAs(aTry.type) }?.let { aTry.type = it }

                return cfmpInfo.variablesValues
            }

            override fun visitBreak(jump: IrBreak, data: BitSet): BitSet {
                val cfmpInfo = breaksCFMPInfos[jump.loop] ?: error("Break from an unknown loop: ${jump.render()}")
                controlFlowMergePoint(cfmpInfo, dummyUnitExpression, data)

                return nothingValue
            }

            override fun visitContinue(jump: IrContinue, data: BitSet): BitSet {
                val cfmpInfo = continuesCFMPInfos[jump.loop] ?: error("Continue to an unknown loop: ${jump.render()}")
                controlFlowMergePoint(cfmpInfo, dummyUnitExpression, data)

                return nothingValue
            }

            fun handleDoWhileLoop(loop: IrLoop, variablesValues: BitSet): BitSet {
                var vvAtLoopStart = variablesValues

                context.log { "LOOP START: ${vvAtLoopStart.format()}" }

                var iter = 0
                while (true) {
                    ++iter
                    val prevVVAtLoopStart = vvAtLoopStart
                    val breaksCFMPInfo = ControlFlowMergePointInfo(loop)
                    val continuesCFMPInfo = ControlFlowMergePointInfo(loop)
                    breaksCFMPInfos[loop] = breaksCFMPInfo
                    continuesCFMPInfos[loop] = continuesCFMPInfo
                    val vvAtBodyEnd = loop.body?.accept(this, vvAtLoopStart) ?: vvAtLoopStart
                    controlFlowMergePoint(continuesCFMPInfo, dummyUnitExpression, vvAtBodyEnd)
                    // The condition is reached both by falling through the body and by every continue,
                    // so the merged values must be taken here, not just the fall-through ones.
                    val vvAtConditionStart = continuesCFMPInfo.variablesValues
                    val vvAtConditionEnd = loop.condition.accept(this, vvAtConditionStart)
                    vvAtLoopStart = vvAtConditionEnd
                    if (iter > 1) // Merge starting with the second iteration since the first is always executed.
                        vvAtLoopStart.or(prevVVAtLoopStart)

                    context.log { "LOOP ITER #$iter: ${vvAtLoopStart.format()}" }

                    if (vvAtLoopStart == prevVVAtLoopStart) {
                        breaksCFMPInfos.remove(loop)
                        continuesCFMPInfos.remove(loop)
                        // Same goes for the loop's exit: it is reached both by the condition becoming false
                        // and by every break.
                        controlFlowMergePoint(breaksCFMPInfo, dummyUnitExpression, vvAtConditionEnd)
                        return breaksCFMPInfo.variablesValues
                    }
                }
            }

            override fun visitWhileLoop(loop: IrWhileLoop, data: BitSet): BitSet {
                // Replace
                //     while (condition) { .. }
                // with
                //     if (condition) { do { .. } while (condition) }
                val doWhileLoop = doWhileLoopForWhileLoops.getOrPut(loop) {
                    with(loop) { IrDoWhileLoopImpl(startOffset, endOffset, unitType, null) }
                }
                val cfmpInfo = ControlFlowMergePointInfo(doWhileLoop)
                val result = loop.condition.accept(this, data)
                controlFlowMergePoint(cfmpInfo, dummyUnitExpression, result)
                val loopResult = handleDoWhileLoop(loop, result)
                controlFlowMergePoint(cfmpInfo, dummyUnitExpression, loopResult)

                return cfmpInfo.variablesValues
            }

            override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: BitSet) = handleDoWhileLoop(loop, data)

            override fun visitGetValue(expression: IrGetValue, data: BitSet): BitSet {
                val variable = expression.symbol.owner as? IrVariable ?: return data
                val variableWrites = variableWrites[variable]?.copy()?.apply { and(data) }
                        ?: error("A use of uninitialized variable ${variable.render()}")
                val mergedVariableWrites = getValueVariablesWrites.getOrPut(expression) { BitSet() }
                mergedVariableWrites.or(variableWrites)
                expression.type = mergedVariableWrites.computeType()
                        ?.trySwitchVariableType(variable)
                        ?.takeIf { it.isAtLeastAsConcreteAs(variable.type) }
                        ?: variable.type

                context.logMultiple {
                    +expression.render()
                    +"    ${mergedVariableWrites.format()}"
                }

                return data
            }

            fun setVariable(variable: IrVariable, value: IrExpression, variablesValues: BitSet): BitSet {
                val id = getVariableWriteId(variable, value)
                catchesVariablesValues?.set(id)
                val writes = variableWrites[variable] ?: error("A use of uninitialized variable ${variable.render()}")
                return variablesValues.copy().apply {
                    andNot(writes) // Forget all previous values.
                    set(id)
                }
            }

            override fun visitVariable(declaration: IrVariable, data: BitSet) =
                    declaration.initializer?.let {
                        val result = it.accept(this, data)
                        setVariable(declaration, it, result)
                    } ?: data

            override fun visitSetValue(expression: IrSetValue, data: BitSet): BitSet {
                val result = expression.value.accept(this, data)
                // Only the variables' values are tracked: a write to a value parameter (which `TailrecLowering` and
                // `DefaultArgumentStubGenerator` do generate) needs no bookkeeping, since `visitGetValue` never
                // computes a type for a parameter read in the first place.
                val variable = expression.symbol.owner as? IrVariable ?: return result
                return setVariable(variable, expression.value, result)
            }
        }, data = BitSet())

        val irBuilder = context.createIrBuilder(container.symbol)
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitElement(element: IrElement): IrElement {
                element.transformChildrenVoid(this)

                return element
            }

            override fun visitVariable(declaration: IrVariable): IrStatement {
                declaration.transformChildrenVoid(this)

                val values = variableWrites[declaration]?.mapEachBit { allVariablesWrites[it].value }
                val actualType = values?.computeType()
                        ?.trySwitchVariableType(declaration)
                        ?.takeIf { it.isAtLeastAsConcreteAs(declaration.type) }
                if (actualType != null) {
                    declaration.type = if (declaration.origin == IrDeclarationOrigin.DEFINED && declaration.type.isNullable()) {
                        // For `DEFINED` variables, if the original type is nullable, preserve this:
                        actualType.makeNullable()
                        /*
                        Not doing this is also correct, but it might make the compiler generate redundant unboxing.
                        Apart from performance matters, redundant unboxing also breaks the existing (yet questionable)
                        behavior: KT-84727.
                        In that issue, since the value is actually `null`, unboxing it caused a segmentation fault.

                        Note: there are more places in this pass that update expression and declaration types.
                        So, one might want to apply the same approach to all of them. But that turned out to be unnecessary,
                        since in all other cases the redundant unboxing can be removed by `RedundantCoercionsCleaner`.
                        Moreover, without `RedundantCoercionsCleaner`, there is unboxing generated for the reproducer
                        from the issue even without this pass, so we need to rely on it anyway.
                        In other words, it doesn't make sense to think here about the case when that pass is disabled.
                        Finally, there are tests that check other similar scenarios for redundant unboxing.

                        Note: this hack is limited to `DEFINED` variable to make it affect the compiler behavior
                        as little as possible while solving the particular case.
                        Moreover, applying it to inliner-generated variables would partially undermine the purpose of
                        this optimization: eliminate redundant (un)boxing and casts caused by aggressive type erasure
                        done by the inliner.
                        It is possible to have a more precise distinction between user-defined and compiler-generated
                        `IrVariable`s (e.g., some user-defined variable-like entities have different `origin`s), but
                        the whole idea is to keep it as simple and local as possible.
                        */
                    } else {
                        actualType
                    }
                }

                return declaration
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val valueDeclaration = expression.symbol.owner
                return if (expression.type == valueDeclaration.type)
                    expression
                else {
                    val actualType = expression.type
                    expression.type = valueDeclaration.type
                    irBuilder.at(expression).irProvenImplicitCast(expression, actualType)
                }
            }

            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                expression.transformChildrenVoid(this)

                when (expression.operator) {
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                        return if (expression.argument.type.isUnit())
                            expression.argument
                        else expression
                    }

                    IrTypeOperator.IMPLICIT_CAST -> {
                        return if (expression.isRedundantImplicitCast()) expression.argument else expression
                    }

                    else -> return expression
                }
            }
        })
    }
}
