/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.AbstractVariableRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.lower.loops.isInductionVariable
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineScopeResolver
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.findInlineCallSites
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPublicSymbolBase
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.util.OperatorNameConventions

internal val jvmOptimizationLoweringPhase = makeIrFilePhase(
    ::JvmOptimizationLowering,
    name = "JvmOptimizationLowering",
    description = "Optimize code for JVM code generation"
)

class JvmOptimizationLowering(val context: JvmBackendContext) : FileLoweringPass {

    companion object {
        fun isNegation(expression: IrExpression, context: JvmBackendContext): Boolean =
            expression is IrCall &&
                    (expression.symbol as? IrPublicSymbolBase<*>)?.signature == context.irBuiltIns.booleanNotSymbol.signature
    }

    private val IrFunction.isObjectEquals
        get() = name.asString() == "equals" &&
                valueParameters.count() == 1 &&
                valueParameters[0].type.isNullableAny() &&
                extensionReceiverParameter == null &&
                dispatchReceiverParameter != null


    private fun getOperandsIfCallToEQEQOrEquals(call: IrCall): Pair<IrExpression, IrExpression>? =
        when {
            call.symbol == context.irBuiltIns.eqeqSymbol -> {
                val left = call.getValueArgument(0)!!
                val right = call.getValueArgument(1)!!
                left to right
            }

            call.symbol.owner.isObjectEquals -> {
                val left = call.dispatchReceiver!!
                val right = call.getValueArgument(0)!!
                left to right
            }

            else -> null
        }

    private class SafeCallInfo(
        val scopeSymbol: IrSymbol,
        val tmpVal: IrVariable,
        val ifNullBranch: IrBranch,
        val ifNotNullBranch: IrBranch
    )

    private fun parseSafeCall(expression: IrExpression): SafeCallInfo? {
        val block = expression as? IrBlock ?: return null
        if (block.origin != IrStatementOrigin.SAFE_CALL) return null
        if (block.statements.size != 2) return null
        val tmpVal = block.statements[0] as? IrVariable ?: return null
        val scopeOwner = tmpVal.parent as? IrDeclaration ?: return null
        val scopeSymbol = scopeOwner.symbol
        val whenExpr = block.statements[1] as? IrWhen ?: return null
        if (whenExpr.branches.size != 2) return null

        val ifNullBranch = whenExpr.branches[0]
        val ifNullBranchCondition = ifNullBranch.condition
        if (ifNullBranchCondition !is IrCall) return null
        if (ifNullBranchCondition.symbol != context.irBuiltIns.eqeqSymbol) return null
        val arg0 = ifNullBranchCondition.getValueArgument(0)
        if (arg0 !is IrGetValue || arg0.symbol != tmpVal.symbol) return null
        val arg1 = ifNullBranchCondition.getValueArgument(1)
        if (arg1 !is IrConst<*> || arg1.value != null) return null
        val ifNullBranchResult = ifNullBranch.result
        if (ifNullBranchResult !is IrConst<*> || ifNullBranchResult.value != null) return null

        val ifNotNullBranch = whenExpr.branches[1]
        return SafeCallInfo(scopeSymbol, tmpVal, ifNullBranch, ifNotNullBranch)
    }

    private fun IrType.isJvmPrimitive(): Boolean =
        // TODO get rid of type mapper (take care of '@EnhancedNullability', maybe some other stuff).
        AsmUtil.isPrimitive(context.typeMapper.mapType(this))

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(Transformer(irFile.findInlineCallSites(context)), null)
    }

    private inner class Transformer(private val inlineScopeResolver: IrInlineScopeResolver) : IrElementTransformer<IrDeclaration?> {
        private val dontTouchTemporaryVals = HashSet<IrVariable>()

        override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?): IrStatement =
            super.visitDeclaration(declaration, declaration)

        override fun visitCall(expression: IrCall, data: IrDeclaration?): IrExpression {
            expression.transformChildren(this, data)

            if (expression.symbol.owner.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) {
                return optimizePropertyAccess(expression, data)
            }

            if (isNegation(expression, context) && isNegation(expression.dispatchReceiver!!, context)) {
                return (expression.dispatchReceiver as IrCall).dispatchReceiver!!
            }

            getOperandsIfCallToEQEQOrEquals(expression)?.let { (left, right) ->
                if (left.isNullConst() && right.isNullConst())
                    return IrConstImpl.constTrue(expression.startOffset, expression.endOffset, context.irBuiltIns.booleanType)

                if (left.isNullConst() && right is IrConst<*> || right.isNullConst() && left is IrConst<*>)
                    return IrConstImpl.constFalse(expression.startOffset, expression.endOffset, context.irBuiltIns.booleanType)

                if (expression.symbol == context.irBuiltIns.eqeqSymbol) {
                    if (right.type.isJvmPrimitive()) {
                        parseSafeCall(left)?.let { return rewriteSafeCallEqeqPrimitive(it, expression) }
                    }
                    if (left.type.isJvmPrimitive()) {
                        parseSafeCall(right)?.let { return rewritePrimitiveEqeqSafeCall(it, expression) }
                    }
                }
            }

            return expression
        }

        private fun IrBuilderWithScope.ifSafe(safeCall: SafeCallInfo, expr: IrExpression): IrExpression =
            irBlock(origin = IrStatementOrigin.SAFE_CALL) {
                +safeCall.tmpVal
                +irIfThenElse(expr.type, safeCall.ifNullBranch.condition, irFalse(), expr)
            }

        // Fuse safe call with primitive equality to avoid boxing the primitive. `a?.x == p`:
        //     { val tmp = a; if (tmp == null) null else tmp.x } == p`
        // is transformed to:
        //     { val tmp = a; if (tmp == null) false else tmp.x == p }
        // Note that the original IR implied that `p` is always evaluated, but the rewritten version
        // only does so if `a` is not null. This is how the old backend does it, and it's consistent
        // with `a?.x?.equals(p)`.
        private fun rewriteSafeCallEqeqPrimitive(safeCall: SafeCallInfo, eqeqCall: IrCall): IrExpression =
            context.createJvmIrBuilder(safeCall.scopeSymbol).run {
                ifSafe(safeCall, eqeqCall.apply { putValueArgument(0, safeCall.ifNotNullBranch.result) })
            }

        // Fuse safe call with primitive equality to avoid boxing the primitive. 'p == a?.x':
        //     p == { val tmp = a; if (tmp == null) null else tmp.x }
        // is transformed to:
        //     { val tmp_p = p; { val tmp = a; if (tmp == null) false else p == tmp } }
        // Note that `p` is evaluated even if `a` is null, which is again consistent with both the old backend
        // and `p.equals(a?.x)`.
        private fun rewritePrimitiveEqeqSafeCall(safeCall: SafeCallInfo, eqeqCall: IrCall): IrExpression =
            context.createJvmIrBuilder(safeCall.scopeSymbol).run {
                val primitive = eqeqCall.getValueArgument(0)!!
                if (primitive.isTrivial()) {
                    ifSafe(safeCall, eqeqCall.apply { putValueArgument(1, safeCall.ifNotNullBranch.result) })
                } else {
                    // The extra block for `p`'s variable is intentional as adding it into the inner block
                    // would make it no longer look like a safe call to `IfNullExpressionFusionLowering`.
                    irBlock {
                        +ifSafe(safeCall, eqeqCall.apply {
                            putValueArgument(0, irGet(irTemporary(primitive)))
                            putValueArgument(1, safeCall.ifNotNullBranch.result)
                        })
                    }
                }
            }

        private fun optimizePropertyAccess(expression: IrCall, data: IrDeclaration?): IrExpression {
            val accessor = expression.symbol.owner as? IrSimpleFunction ?: return expression
            if (accessor.modality != Modality.FINAL || accessor.isExternal) return expression
            val property = accessor.correspondingPropertySymbol?.owner ?: return expression
            if (property.isLateinit) return expression
            val backingField = property.backingField ?: return expression
            val scope = data?.let(inlineScopeResolver::findContainer) ?: return expression
            if (scope != accessor.parent || scope != backingField.parent) return expression
            val receiver = expression.dispatchReceiver
            return context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset).irBlock(expression) {
                if (backingField.isStatic && receiver != null && receiver !is IrGetValue) {
                    // If the field is static, evaluate the receiver for potential side effects.
                    +receiver.coerceToUnit(context.irBuiltIns, this@JvmOptimizationLowering.context.typeSystem)
                }
                if (accessor.valueParameters.isNotEmpty()) {
                    +irSetField(
                        receiver.takeUnless { backingField.isStatic },
                        backingField,
                        expression.getValueArgument(expression.valueArgumentsCount - 1)!!
                    )
                } else {
                    +irGetField(receiver.takeUnless { backingField.isStatic }, backingField)
                }
            }
        }

        override fun visitWhen(expression: IrWhen, data: IrDeclaration?): IrExpression {
            val isCompilerGenerated = expression.origin == null
            expression.transformChildren(this, data)
            // Remove all branches with constant false condition.
            expression.branches.removeIf {
                it.condition.isFalseConst() && isCompilerGenerated
            }
            if (expression.origin == IrStatementOrigin.ANDAND) {
                assert(
                    expression.type.isBoolean()
                            && expression.branches.size == 2
                            && expression.branches[1].condition.isTrueConst()
                            && expression.branches[1].result.isFalseConst()
                ) {
                    "ANDAND condition should have an 'if true then false' body on its second branch. " +
                            "Failing expression: ${expression.dump()}"
                }
                // Replace conjunction condition with intrinsic "and" function call
                return IrCallImpl.fromSymbolOwner(
                    expression.startOffset,
                    expression.endOffset,
                    context.irBuiltIns.booleanType,
                    context.irBuiltIns.andandSymbol
                ).apply {
                    putValueArgument(0, expression.branches[0].condition)
                    putValueArgument(1, expression.branches[0].result)
                }
            }
            if (expression.origin == IrStatementOrigin.OROR) {
                assert(
                    expression.type.isBoolean()
                            && expression.branches.size == 2
                            && expression.branches[0].result.isTrueConst()
                            && expression.branches[1].condition.isTrueConst()
                ) {
                    "OROR condition should have an 'if a then true' body on its first branch, " +
                            "and an 'if true then b' body on its second branch. " +
                            "Failing expression: ${expression.dump()}"
                }
                return IrCallImpl.fromSymbolOwner(
                    expression.startOffset,
                    expression.endOffset,
                    context.irBuiltIns.booleanType,
                    context.irBuiltIns.ororSymbol
                ).apply {
                    putValueArgument(0, expression.branches[0].condition)
                    putValueArgument(1, expression.branches[1].result)
                }
            }

            // If there are no conditions left, remove the 'when' entirely and replace it with an empty block.
            if (expression.branches.size == 0) {
                return IrBlockImpl(expression.startOffset, expression.endOffset, context.irBuiltIns.unitType)
            }

            // If the only condition that is left has a constant true condition, remove the 'when' in favor of the result.
            val firstBranch = expression.branches.first()
            if (firstBranch.condition.isTrueConst() && isCompilerGenerated) {
                return firstBranch.result
            }

            return expression
        }

        private fun getInlineableValueForTemporaryVal(statement: IrStatement): IrExpression? {
            val variable = statement as? IrVariable ?: return null
            if (variable.origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE || variable.isVar) return null
            if (variable in dontTouchTemporaryVals) return null

            when (val initializer = variable.initializer) {
                is IrConst<*> ->
                    return initializer
                is IrGetValue ->
                    when (val initializerValue = initializer.symbol.owner) {
                        is IrVariable ->
                            return when {
                                initializerValue.isVar ->
                                    null
                                initializerValue.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE ->
                                    getInlineableValueForTemporaryVal(initializerValue)
                                        ?: initializer
                                else ->
                                    initializer
                            }
                        is IrValueParameter ->
                            return if (initializerValue.isAssignable)
                                null
                            else
                                initializer
                    }
            }

            return null
        }

        private fun removeUnnecessaryTemporaryVariables(statements: MutableList<IrStatement>) {
            // Remove declarations of immutable temporary variables that can be inlined.
            statements.removeIf {
                getInlineableValueForTemporaryVal(it) != null
            }

            // Remove a block that contains only two statements: the declaration of a temporary
            // variable and a load of the value of that temporary variable with just the initializer
            // for the temporary variable. We only perform this transformation for compiler generated
            // temporary variables. Local variables can be changed at runtime and therefore eliminating
            // an actual local variable changes debugging behavior.
            //
            // This helps avoid temporary variables even for side-effecting expressions when they are
            // not needed. Having a temporary variable leads to local loads and stores in the
            // generated java bytecode which are not necessary. For example
            //
            //     42.toLong()!!
            //
            // introduces a temporary variable for the toLong() call and a null check
            //    block
            //      temp = 42.toLong()
            //      when (eq(temp, null))
            //        (true) -> throwNep()
            //        (false) -> temp
            //
            // the when is simplified because long is a primitive type, which leaves us with
            //
            //    block
            //      temp = 42.toLong()
            //      temp
            //
            // which can be simplified to simply
            //
            //    block
            //      42.toLong()
            //
            // Doing so we avoid local loads and stores.
            if (statements.size == 2) {
                val first = statements[0]
                val second = statements[1]
                if (first is IrVariable
                    && first.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
                    && second is IrGetValue
                    && first.symbol == second.symbol
                ) {
                    statements.clear()
                    first.initializer?.let { statements.add(it) }
                }
            }
        }

        override fun visitBlockBody(body: IrBlockBody, data: IrDeclaration?): IrBody {
            body.transformChildren(this, data)
            removeUnnecessaryTemporaryVariables(body.statements)
            return body
        }

        override fun visitContainerExpression(expression: IrContainerExpression, data: IrDeclaration?): IrExpression {
            val safeCall = parseSafeCall(expression)
            if (safeCall != null) {
                // Don't optimize out temporary values for safe calls (yet), so that safe call-based equality checks can be optimized.
                dontTouchTemporaryVals.add(safeCall.tmpVal)
            }

            if (expression.origin == IrStatementOrigin.WHEN) {
                // Don't optimize out 'when' subject initialized with a variable,
                // otherwise we might get somewhat weird debugging behavior.
                val subject = expression.statements.firstOrNull()
                if (subject is IrVariable && subject.initializer is IrGetValue) {
                    dontTouchTemporaryVals.add(subject)
                }
            }

            if (expression.origin == IrStatementOrigin.POSTFIX_DECR || expression.origin == IrStatementOrigin.POSTFIX_INCR) {
                expression.rewritePostfixIncrDecr()?.let { return it }
            }

            if (expression.origin == IrStatementOrigin.FOR_LOOP) {
                reuseLoopVariableAsInductionVariableIfPossible(expression)
            }

            expression.transformChildren(this, data)
            removeUnnecessaryTemporaryVariables(expression.statements)
            return expression
        }

        private fun reuseLoopVariableAsInductionVariableIfPossible(irForLoopBlock: IrContainerExpression) {
            if (irForLoopBlock.statements.size != 2) return

            val loopInitialization = irForLoopBlock.statements[0] as? IrComposite ?: return
            val inductionVariableIndex = loopInitialization.statements.indexOfFirst { it.isInductionVariable(context) }
            if (inductionVariableIndex < 0) return
            val inductionVariable = loopInitialization.statements[inductionVariableIndex] as? IrVariable ?: return

            val loopVariablePosition = findLoopVariablePosition(irForLoopBlock.statements[1]) ?: return
            val (loopVariableContainer, loopVariableIndex) = loopVariablePosition
            val loopVariable = loopVariableContainer.statements[loopVariableIndex] as? IrVariable ?: return

            val inductionVariableType = inductionVariable.type
            val loopVariableType = loopVariable.type
            if (loopVariableType.isNullable()) return
            if (loopVariableType.classifierOrNull != inductionVariableType.classifierOrNull) return

            val newLoopVariable = IrVariableImpl(
                loopVariable.startOffset, loopVariable.endOffset, loopVariable.origin,
                IrVariableSymbolImpl(),
                loopVariable.name, loopVariableType,
                isVar = true, // NB original loop variable is 'val'
                isConst = false, isLateinit = false
            )
            newLoopVariable.initializer = inductionVariable.initializer
            newLoopVariable.parent = inductionVariable.parent

            loopInitialization.statements[inductionVariableIndex] = newLoopVariable
            loopVariableContainer.statements.removeAt(loopVariableIndex)
            val remapper = object : AbstractVariableRemapper() {
                override fun remapVariable(value: IrValueDeclaration): IrValueDeclaration? =
                    if (value == inductionVariable || value == loopVariable) newLoopVariable else null
            }
            irForLoopBlock.statements[1].transformChildren(remapper, null)
        }

        private fun findLoopVariablePosition(statement: IrStatement): Pair<IrContainerExpression, Int>? {
            when (statement) {
                is IrDoWhileLoop -> {
                    // Expecting counter loop
                    val doWhileLoop = statement as? IrDoWhileLoop ?: return null
                    if (doWhileLoop.origin != JvmLoweredStatementOrigin.DO_WHILE_COUNTER_LOOP) return null
                    val doWhileLoopBody = doWhileLoop.body as? IrComposite ?: return null
                    if (doWhileLoopBody.origin != IrStatementOrigin.FOR_LOOP_INNER_WHILE) return null
                    val iterationInitialization = doWhileLoopBody.statements[0] as? IrComposite ?: return null
                    val loopVariableIndex = iterationInitialization.statements.indexOfFirst { it.isLoopVariable() }
                    if (loopVariableIndex < 0) return null
                    return iterationInitialization to loopVariableIndex
                }
                is IrWhen -> {
                    // Expecting if-guarded counter loop
                    val doWhileLoop = statement.branches[0].result as? IrDoWhileLoop ?: return null
                    return findLoopVariablePosition(doWhileLoop)
                }
                else -> {
                    return null
                }
            }
        }
        
        private fun IrStatement.isLoopVariable() =
            this is IrVariable && origin == IrDeclarationOrigin.FOR_LOOP_VARIABLE

        private fun IrContainerExpression.rewritePostfixIncrDecr(): IrCall? {
            return when (origin) {
                IrStatementOrigin.POSTFIX_INCR, IrStatementOrigin.POSTFIX_DECR -> {
                    val tmpVar = this.statements[0] as? IrVariable ?: return null
                    val getIncrVar = tmpVar.initializer as? IrGetValue ?: return null
                    if (!getIncrVar.type.isInt()) return null

                    val setVar = this.statements[1] as? IrSetValue ?: return null
                    if (setVar.symbol != getIncrVar.symbol) return null
                    val setVarValue = setVar.value as? IrCall ?: return null
                    val calleeName = setVarValue.symbol.owner.name
                    if (calleeName != OperatorNameConventions.INC && calleeName != OperatorNameConventions.DEC) return null
                    val calleeArg = setVarValue.dispatchReceiver as? IrGetValue ?: return null
                    if (calleeArg.symbol != tmpVar.symbol) return null

                    val getTmpVar = this.statements[2] as? IrGetValue ?: return null
                    if (getTmpVar.symbol != tmpVar.symbol) return null

                    val intrinsicSymbol =
                        if (calleeName == OperatorNameConventions.INC)
                            context.ir.symbols.intPostfixIncr
                        else
                            context.ir.symbols.intPostfixDecr

                    return IrCallImpl.fromSymbolOwner(this.startOffset, this.endOffset, intrinsicSymbol)
                        .apply { putValueArgument(0, getIncrVar) }
                }
                else ->
                    null
            }
        }

        override fun visitGetValue(expression: IrGetValue, data: IrDeclaration?): IrExpression {
            // Replace IrGetValue of an immutable temporary variable with a constant
            // initializer with the constant initializer.
            val variable = expression.symbol.owner
            return when (val replacement = getInlineableValueForTemporaryVal(variable)) {
                is IrConst<*> ->
                    replacement.copyWithOffsets(expression.startOffset, expression.endOffset)
                is IrGetValue ->
                    replacement.copyWithOffsets(expression.startOffset, expression.endOffset)
                else ->
                    expression
            }
        }
    }
}
