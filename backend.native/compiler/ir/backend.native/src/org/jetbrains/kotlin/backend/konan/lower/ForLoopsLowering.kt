/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.util.isSimpleTypeWithQuestionMark
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.util.OperatorNameConventions

/**  This lowering pass optimizes range-based for loops. */
internal class ForLoopsLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val transformer = ForLoopsTransformer(context)
        // Lower loops
        irFile.transformChildrenVoid(transformer)

        // Update references in break/continue.
        irFile.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitBreakContinue(jump: IrBreakContinue): IrExpression {
                transformer.oldLoopToNewLoop[jump.loop]?.let { jump.loop = it }
                return jump
            }
        })
    }
}

private class ForLoopsTransformer(val context: Context) : IrElementTransformerVoidWithContext() {

    private val symbols = context.ir.symbols
    private val iteratorToLoopInfo = mutableMapOf<IrVariableSymbol, ForLoopInfo>()
    internal val oldLoopToNewLoop = mutableMapOf<IrLoop, IrLoop>()

    private val iteratorType = symbols.iterator.descriptor.defaultType.replaceArgumentsWithStarProjections()

    private val scopeOwnerSymbol
        get() = currentScope!!.scope.scopeOwnerSymbol

    private val progressionElementClasses: List<IrClassSymbol> = mutableListOf(symbols.char).apply {
        addAll(symbols.integerClasses)
    }

    private val progressionElementClassesTypes: List<SimpleType> = mutableListOf<SimpleType>().apply {
        progressionElementClasses.mapTo(this) { it.descriptor.defaultType }
    }

    private val progressionElementClassesNullableTypes: List<SimpleType> = mutableListOf<SimpleType>().apply {
        progressionElementClassesTypes.mapTo(this) { it.makeNullableAsSpecified(true) }
    }

    //region Symbols for progression building functions ================================================================
    private fun getProgressionBuildingMethods(name: String): Set<IrFunctionSymbol> =
            getMethodsForProgressionElements(name) {
                it.valueParameters.size == 1 && it.valueParameters[0].type in progressionElementClassesTypes
            }

    private fun getProgressionBuildingExtensions(name: String, pkg: FqName): Set<IrFunctionSymbol> =
            getExtensionsForProgressionElements(name, pkg) {
                it.extensionReceiverParameter?.type in progressionElementClassesTypes &&
                it.valueParameters.size == 1 &&
                it.valueParameters[0].type in progressionElementClassesTypes
            }

    private fun getMethodsForProgressionElements(name: String,
                                                 filter: (SimpleFunctionDescriptor) -> Boolean): Set<IrFunctionSymbol> =
            mutableSetOf<IrFunctionSymbol>().apply {
                progressionElementClasses.flatMapTo(this) { receiver ->
                    receiver.descriptor.unsubstitutedMemberScope
                            .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                            .filter(filter).map { symbols.symbolTable.referenceFunction(it) }
                }
            }

    private fun getExtensionsForProgressionElements(name: String,
                                                    pkg: FqName,
                                                    filter: (SimpleFunctionDescriptor) -> Boolean): Set<IrFunctionSymbol> =
            mutableSetOf<IrFunctionSymbol>().apply {
                progressionElementClasses.flatMapTo(this) { _ /* receiver */ ->
                    context.builtIns.builtInsModule.getPackage(pkg).memberScope
                            .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                            .filter(filter).map { symbols.symbolTable.referenceFunction(it) }
                }
            }

    private val rangeToSymbols by lazy { getProgressionBuildingMethods("rangeTo") }
    private val untilSymbols by lazy { getProgressionBuildingExtensions("until", FqName("kotlin.ranges")) }
    private val downToSymbols by lazy { getProgressionBuildingExtensions("downTo", FqName("kotlin.ranges")) }
    private val stepSymbols by lazy {
        getExtensionsForProgressionElements("step", FqName("kotlin.ranges")) {
            it.extensionReceiverParameter?.type in symbols.progressionClassesTypes &&
                    it.valueParameters.size == 1 &&
                    (KotlinBuiltIns.isLong(it.valueParameters[0].type) || KotlinBuiltIns.isInt(it.valueParameters[0].type))
        }
    }
    //endregion

    //region Util methods ==============================================================================================
    private fun IrExpression.castIfNecessary(progressionType: ProgressionType): IrExpression {
        val type = this.type.toKotlinType()
        assert(type in progressionElementClassesTypes || type in progressionElementClassesNullableTypes)
        return if (type == progressionType.elementType) {
            this
        } else {
            val function = symbols.getFunction(progressionType.numberCastFunctionName, type)
            IrCallImpl(startOffset, endOffset, function.owner.returnType, function)
                    .apply { dispatchReceiver = this@castIfNecessary }
        }
    }

    private fun DeclarationIrBuilder.ensureNotNullable(expression: IrExpression): IrExpression {
        return if (expression.type.isSimpleTypeWithQuestionMark) {
            irImplicitCast(expression, expression.type.makeNotNull())
        } else {
            expression
        }
    }

    private fun IrExpression.unaryMinus(): IrExpression {
        val unaryOperator = symbols.getUnaryOperator(OperatorNameConventions.UNARY_MINUS, type.toKotlinType())
        return IrCallImpl(startOffset, endOffset, unaryOperator.owner.returnType, unaryOperator).apply {
            dispatchReceiver = this@unaryMinus
        }
    }

    private fun ProgressionInfo.defaultStep(startOffset: Int, endOffset: Int): IrExpression =
        progressionType.elementType.let { type ->
            val step = if (increasing) 1 else -1
            when {
                KotlinBuiltIns.isInt(type) || KotlinBuiltIns.isChar(type) ->
                    IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, step)
                KotlinBuiltIns.isLong(type) ->
                    IrConstImpl.long(startOffset, endOffset, context.irBuiltIns.longType, step.toLong())
                else -> throw IllegalArgumentException()
            }
        }

    private fun IrConst<*>.isOne() =
        when (kind) {
            IrConstKind.Long -> value as Long == 1L
            IrConstKind.Int  -> value as Int == 1
            else -> false
        }

    // Used only by the assert.
    private fun stepHasRightType(step: IrExpression, progressionType: ProgressionType) =
            ((progressionType.isCharProgression() || progressionType.isIntProgression()) &&
                    KotlinBuiltIns.isInt(step.type.toKotlinType().makeNotNullable())) ||
            (progressionType.isLongProgression() &&
                    KotlinBuiltIns.isLong(step.type.toKotlinType().makeNotNullable()))

    private fun irCheckProgressionStep(progressionType: ProgressionType,
                                       step: IrExpression): Pair<IrExpression, Boolean> {
        if (step is IrConst<*> &&
            ((step.kind == IrConstKind.Long && step.value as Long > 0) ||
            (step.kind == IrConstKind.Int && step.value as Int > 0))) {
            return step to !step.isOne()
        }
        // The frontend checks if the step has a right type (Long for LongProgression and Int for {Int/Char}Progression)
        // so there is no need to cast it.
        assert(stepHasRightType(step, progressionType))

        val symbol = symbols.checkProgressionStep[step.type.toKotlinType().makeNotNullable()]
                ?: throw IllegalArgumentException("Unknown progression element type: ${step.type}")
        return IrCallImpl(step.startOffset, step.endOffset, symbol.owner.returnType, symbol).apply {
            putValueArgument(0, step)
        } to true
    }

    private fun irGetProgressionLast(progressionType: ProgressionType,
                                     first: IrVariable,
                                     lastExpression: IrExpression,
                                     step: IrVariable): IrExpression {
        val symbol = symbols.getProgressionLast[progressionType.elementType]
                ?: throw IllegalArgumentException("Unknown progression element type: ${lastExpression.type}")
        val startOffset = lastExpression.startOffset
        val endOffset = lastExpression.endOffset
        return IrCallImpl(startOffset, lastExpression.endOffset, symbol.owner.returnType, symbol).apply {
            putValueArgument(0, IrGetValueImpl(startOffset, endOffset, first.type, first.symbol))
            putValueArgument(1, lastExpression.castIfNecessary(progressionType))
            putValueArgument(2, IrGetValueImpl(startOffset, endOffset, step.type, step.symbol))
        }
    }
    //endregion

    //region Util classes ==============================================================================================
    // TODO: Replace with a cast when such support is added in the boxing lowering.
    private data class ProgressionType(val elementType: KotlinType, val numberCastFunctionName: Name) {
        fun isIntProgression()  = KotlinBuiltIns.isInt(elementType)
        fun isLongProgression() = KotlinBuiltIns.isLong(elementType)
        fun isCharProgression() = KotlinBuiltIns.isChar(elementType)
    }

    private data class ProgressionInfo(
            val progressionType: ProgressionType,
            val first: IrExpression,
            val bound: IrExpression,
            val step: IrExpression? = null,
            val increasing: Boolean = true,
            var needLastCalculation: Boolean = false,
            val closed: Boolean = true)

    /** Contains information about variables used in the loop. */
    private data class ForLoopInfo(
            val progressionInfo: ProgressionInfo,
            val inductionVariable: IrVariable,
            val bound: IrVariable,
            val last: IrVariable,
            val step: IrVariable,
            var loopVariable: IrVariable? = null)

    private inner class ProgressionInfoBuilder : IrElementVisitor<ProgressionInfo?, Nothing?> {

        val INT_PROGRESSION = ProgressionType(context.builtIns.intType, Name.identifier("toInt"))
        val LONG_PROGRESSION = ProgressionType(context.builtIns.longType, Name.identifier("toLong"))
        val CHAR_PROGRESSION = ProgressionType(context.builtIns.charType, Name.identifier("toChar"))

        private fun buildRangeTo(expression: IrCall, progressionType: ProgressionType) =
                ProgressionInfo(progressionType,
                        expression.dispatchReceiver!!,
                        expression.getValueArgument(0)!!)

        private fun buildUntil(expression: IrCall, progressionType: ProgressionType): ProgressionInfo =
                ProgressionInfo(progressionType,
                        expression.extensionReceiver!!,
                        expression.getValueArgument(0)!!,
                        closed = false)

        private fun buildDownTo(expression: IrCall, progressionType: ProgressionType) =
                ProgressionInfo(progressionType,
                        expression.extensionReceiver!!,
                        expression.getValueArgument(0)!!,
                        increasing = false)

        private fun buildStep(expression: IrCall, progressionType: ProgressionType) =
                expression.extensionReceiver!!.accept(this, null)?.let {
                    val newStep = expression.getValueArgument(0)!!
                    val (newStepCheck, needBoundCalculation) = irCheckProgressionStep(progressionType, newStep)
                    val step = when {
                        it.step == null -> newStepCheck
                        // There were step calls before. Just add our check in the container or create a new one.
                        it.step is IrStatementContainer -> {
                            it.step.statements.add(newStepCheck)
                            it.step
                        }
                        else -> IrCompositeImpl(expression.startOffset, expression.endOffset, newStep.type).apply {
                            statements.add(it.step)
                            statements.add(newStepCheck)
                        }
                    }
                    ProgressionInfo(progressionType, it.first, it.bound, step, it.increasing, needBoundCalculation, it.closed)
                }

        override fun visitElement(element: IrElement, data: Nothing?): ProgressionInfo? = null

        override fun visitCall(expression: IrCall, data: Nothing?): ProgressionInfo? {
            val type = expression.type.toKotlinType()
            val progressionType = when {
                type.isSubtypeOf(symbols.charProgression.descriptor.defaultType) -> CHAR_PROGRESSION
                type.isSubtypeOf(symbols.intProgression.descriptor.defaultType) -> INT_PROGRESSION
                type.isSubtypeOf(symbols.longProgression.descriptor.defaultType) -> LONG_PROGRESSION
                else -> return null
            }

            // TODO: Process constructors and other factory functions.
            return when (expression.symbol) {
                in rangeToSymbols -> buildRangeTo(expression, progressionType)
                in untilSymbols -> buildUntil(expression, progressionType)
                in downToSymbols -> buildDownTo(expression, progressionType)
                in stepSymbols -> buildStep(expression, progressionType)
                else -> null
            }
        }
    }
    //endregion

    //region Lowering ==================================================================================================
    // Lower a loop header.
    private fun processHeader(variable: IrVariable, initializer: IrCall): IrStatement? {
        assert(variable.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR)
        val symbol = variable.symbol
        if (!variable.descriptor.type.isSubtypeOf(iteratorType)) {
            return null
        }
        assert(symbol !in iteratorToLoopInfo)

        val builder = context.createIrBuilder(scopeOwnerSymbol, variable.startOffset, variable.endOffset)
        // Collect loop info and form the loop header composite.
        val progressionInfo = initializer.dispatchReceiver?.accept(ProgressionInfoBuilder(), null) ?: return null

        with(builder) {
            with(progressionInfo) {
                // Due to features of PSI2IR we can obtain nullable arguments here while actually
                // they are non-nullable (the frontend takes care about this). So we need to cast them to non-nullable.
                val statements = mutableListOf<IrStatement>()

                /**
                 * For this loop:
                 * `for (i in a() .. b() step c() step d())`
                 * We need to call functions in the following order: a, b, c, d.
                 * So we call b() before step calculations and then call last element calculation function (if required).
                 */
                val inductionVariable = scope.createTemporaryVariable(first.castIfNecessary(progressionType),
                        nameHint = "inductionVariable",
                        isMutable = true,
                        origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE).also {
                    statements.add(it)
                }

                val boundValue = scope.createTemporaryVariable(bound.castIfNecessary(progressionType),
                        nameHint = "bound",
                        origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE)
                        .also { statements.add(it) }


                val stepExpression = (if (increasing) step else step?.unaryMinus()) ?: defaultStep(startOffset, endOffset)
                val stepValue = scope.createTemporaryVariable(ensureNotNullable(stepExpression),
                        nameHint = "step",
                        origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE).also {
                    statements.add(it)
                }

                // Calculate the last element of the progression
                // The last element can be:
                //    boundValue, if step is 1 and the range is closed.
                //    boundValue - 1, if step is 1 and the range is open.
                //    getProgressionLast(inductionVariable, boundValue, step), if step != 1 and the range is closed.
                //    getProgressionLast(inductionVariable, boundValue - 1, step), if step != 1 and the range is open.
                var lastExpression: IrExpression? = null
                if (!closed) {
                    val decrementSymbol = symbols.getUnaryOperator(OperatorNameConventions.DEC, boundValue.descriptor.type)
                    lastExpression = irCall(decrementSymbol.owner).apply {
                        dispatchReceiver = irGet(boundValue)
                    }
                }
                if (needLastCalculation) {
                    lastExpression = irGetProgressionLast(progressionType,
                            inductionVariable,
                            lastExpression ?: irGet(boundValue),
                            stepValue)
                }
                val lastValue = if (lastExpression != null) {
                    scope.createTemporaryVariable(lastExpression,
                            nameHint = "last",
                            origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE).also {
                        statements.add(it)
                    }
                } else {
                    boundValue
                }

                iteratorToLoopInfo[symbol] = ForLoopInfo(progressionInfo,
                        inductionVariable,
                        boundValue,
                        lastValue,
                        stepValue)

                return IrCompositeImpl(startOffset, endOffset, context.irBuiltIns.unitType, null, statements)
            }
        }
    }

    // Lower getting a next induction variable value.
    private fun processNext(variable: IrVariable, initializer: IrCall): IrExpression? {
        assert(variable.origin == IrDeclarationOrigin.FOR_LOOP_VARIABLE
                || variable.origin == IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE)
        val irIteratorAccess = initializer.dispatchReceiver as? IrGetValue ?: throw AssertionError()
        val forLoopInfo = iteratorToLoopInfo[irIteratorAccess.symbol] ?: return null  // If we didn't lower a corresponding header.
        val builder = context.createIrBuilder(scopeOwnerSymbol, initializer.startOffset, initializer.endOffset)

        val plusOperator = symbols.getBinaryOperator(
                OperatorNameConventions.PLUS,
                forLoopInfo.inductionVariable.descriptor.type,
                forLoopInfo.step.descriptor.type
        )
        forLoopInfo.loopVariable = variable

        with(builder) {
            variable.initializer = irGet(forLoopInfo.inductionVariable)
            val increment = irSetVar(forLoopInfo.inductionVariable,
                    irCallOp(plusOperator.owner, irGet(forLoopInfo.inductionVariable), irGet(forLoopInfo.step)))
            return IrCompositeImpl(variable.startOffset,
                    variable.endOffset,
                    context.irBuiltIns.unitType,
                    IrStatementOrigin.FOR_LOOP_NEXT,
                    listOf(variable, increment))
        }
    }

    private fun DeclarationIrBuilder.buildMinValueCondition(forLoopInfo: ForLoopInfo): IrExpression {
        // Condition for a corner case: for (i in a until Int.MIN_VALUE) {}.
        // Check if forLoopInfo.bound > MIN_VALUE.
        val progressionType = forLoopInfo.progressionInfo.progressionType
        return irCall(context.irBuiltIns.greaterFunByOperandType[context.irBuiltIns.int]?.symbol!!).apply {
            val minConst = when {
                progressionType.isIntProgression() -> IrConstImpl
                        .int(startOffset, endOffset, context.irBuiltIns.intType, Int.MIN_VALUE)
                progressionType.isCharProgression() -> IrConstImpl
                        .char(startOffset, endOffset, context.irBuiltIns.charType, 0.toChar())
                progressionType.isLongProgression() -> IrConstImpl
                        .long(startOffset, endOffset, context.irBuiltIns.longType, Long.MIN_VALUE)
                else -> throw IllegalArgumentException("Unknown progression type")
            }
            val compareToCall = irCall(symbols.getBinaryOperator(OperatorNameConventions.COMPARE_TO,
                    forLoopInfo.bound.descriptor.type,
                    minConst.type.toKotlinType())).apply {
                dispatchReceiver = irGet(forLoopInfo.bound)
                putValueArgument(0, minConst)
            }
            putValueArgument(0, compareToCall)
            putValueArgument(1, irInt(0))
        }
    }

    // TODO: Eliminate the loop if we can prove that it will not be executed.
    private fun DeclarationIrBuilder.buildEmptyCheck(loop: IrLoop, forLoopInfo: ForLoopInfo): IrExpression {
        val builtIns = context.irBuiltIns
        val increasing = forLoopInfo.progressionInfo.increasing
        val comparingBuiltIn = if (increasing) builtIns.lessOrEqualFunByOperandType[builtIns.int]?.symbol
        else builtIns.greaterOrEqualFunByOperandType[builtIns.int]?.symbol

        // Check if inductionVariable <= last.
        val compareTo = symbols.getBinaryOperator(OperatorNameConventions.COMPARE_TO,
                forLoopInfo.inductionVariable.descriptor.type,
                forLoopInfo.last.descriptor.type)

        val check: IrExpression = irCall(comparingBuiltIn!!).apply {
            putValueArgument(0, irCallOp(compareTo.owner, irGet(forLoopInfo.inductionVariable), irGet(forLoopInfo.last)))
            putValueArgument(1, irInt(0))
        }

        // Process closed and open ranges in different manners.
        return if (forLoopInfo.progressionInfo.closed) {
            irIfThen(check, loop)   // if (inductionVariable <= last) { loop }
        } else {
            // Take into account a corner case: for (i in a until Int.MIN_VALUE) {}.
            // if (inductionVariable <= last && bound > MIN_VALUE) { loop }
            return irIfThen(check, irIfThen(buildMinValueCondition(forLoopInfo), loop))
        }
    }

    private fun DeclarationIrBuilder.buildNewCondition(oldCondition: IrExpression): Pair<IrExpression, ForLoopInfo>? {
        if (oldCondition !is IrCall || oldCondition.origin != IrStatementOrigin.FOR_LOOP_HAS_NEXT) {
            return null
        }

        val irIteratorAccess = oldCondition.dispatchReceiver as? IrGetValue ?: throw AssertionError()
        // Return null if we didn't lower a corresponding header.
        val forLoopInfo = iteratorToLoopInfo[irIteratorAccess.symbol] ?: return null
        assert(forLoopInfo.loopVariable != null)

        return irCall(context.irBuiltIns.booleanNotSymbol).apply {
            val eqeqCall = irCall(context.irBuiltIns.eqeqSymbol).apply {
                putValueArgument(0, irGet(forLoopInfo.loopVariable!!))
                putValueArgument(1, irGet(forLoopInfo.last))
            }
            putValueArgument(0, eqeqCall)
        } to forLoopInfo
    }

    /**
     * This loop
     *
     * for (i in first..last step foo) { ... }
     *
     * is represented in IR in such a manner:
     *
     * val it = (first..last step foo).iterator()
     * while (it.hasNext()) {
     *     val i = it.next()
     *     ...
     * }
     *
     * We transform it into the following loop:
     *
     * var it = first
     * if (it <= last) {  // (it >= last if the progression is decreasing)
     *     do {
     *         val i = it++
     *         ...
     *     } while (i != last)
     * }
     */
    // TODO:  Lower `for (i in a until b)` to loop with precondition: for (i = a; i < b; a++);
    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        if (loop.origin != IrStatementOrigin.FOR_LOOP_INNER_WHILE) {
            return super.visitWhileLoop(loop)
        }

        with(context.createIrBuilder(scopeOwnerSymbol, loop.startOffset, loop.endOffset)) {
            // Transform accesses to the old iterator (see visitVariable method). Store loopVariable in loopInfo.
            // Replace not transparent containers with transparent ones (IrComposite)
            val newBody = loop.body?.transform(this@ForLoopsTransformer, null)?.let {
                if (it is IrContainerExpression && !it.isTransparentScope) {
                    with(it) { IrCompositeImpl(startOffset, endOffset, type, origin, statements) }
                } else {
                    it
                }
            }
            val (newCondition, forLoopInfo) = buildNewCondition(loop.condition) ?: return super.visitWhileLoop(loop)

            val newLoop = IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, loop.origin).apply {
                label = loop.label
                condition = newCondition
                body = newBody
            }
            oldLoopToNewLoop[loop] = newLoop
            // Build a check for an empty progression before the loop.
            return buildEmptyCheck(newLoop, forLoopInfo)
        }
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        val initializer = declaration.initializer
        if (initializer == null || initializer !is IrCall) {
            return super.visitVariable(declaration)
        }
        val result = when (initializer.origin) {
            IrStatementOrigin.FOR_LOOP_ITERATOR -> processHeader(declaration, initializer)
            IrStatementOrigin.FOR_LOOP_NEXT -> processNext(declaration, initializer)
            else -> null
        }
        return result ?: super.visitVariable(declaration)
    }
    //endregion
}

