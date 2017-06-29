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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
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

    private fun irConstOne(startOffset: Int, endOffset: Int) =
            IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, 1)

    private fun irConstMinusOne(startOffset: Int, endOffset: Int) =
            IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, -1)

    private val progressionElementClasses: Set<IrClassSymbol> = mutableSetOf(symbols.char).apply {
        addAll(symbols.integerClasses)
    }

    private val progressionElementClassesTypes: Set<SimpleType> = mutableSetOf<SimpleType>().apply {
        progressionElementClasses.mapTo(this) { it.descriptor.defaultType }
    }

    // Symbols for progression building functions ======================================================================
    private fun getProgressionBuildingMethods(name: String): Set<IrFunctionSymbol> =
            getMethodsForProgressionElements(name) {
                it.valueParameters.size == 1 &&
                        it.valueParameters[0].type in progressionElementClassesTypes
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
                progressionElementClasses.flatMapTo(this) { receiver ->
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

    //region Util methods ==============================================================================================
    private fun IrExpression.castIfNecessary(progressionType: ProgressionType, castToChar: Boolean = true): IrExpression {
        assert(type in progressionElementClassesTypes)
        if (type == progressionType.elementType || (!castToChar && KotlinBuiltIns.isChar(progressionType.elementType))) {
            return this
        }
        return IrCallImpl(startOffset, endOffset, symbols.getFunction(progressionType.numberCastFunctionName, type))
                .apply { dispatchReceiver = this@castIfNecessary }
    }

    private fun IrExpression.unaryMinus(): IrExpression =
            IrCallImpl(startOffset, endOffset, symbols.getUnaryOperator(OperatorNameConventions.UNARY_MINUS, type)).apply {
                dispatchReceiver = this@unaryMinus
            }

    private fun irCheckProgressionStep(progressionType: ProgressionType,
                                       step: IrExpression): IrExpression {
        if (step is IrConst<*> &&
            ((step.kind == IrConstKind.Long && step.value as Long > 0) ||
            (step.kind == IrConstKind.Int && step.value as Int > 0))) {
            return step
        }
        val castedStep = step.castIfNecessary(progressionType, false)
        val symbol = symbols.checkProgressionStep[castedStep.type]
                ?: throw IllegalArgumentException("Unknown progression element type: ${step.type}")
        return IrCallImpl(step.startOffset, step.endOffset, symbol).apply {
            putValueArgument(0, castedStep)
        }
    }

    private fun irGetProgressionBound(progressionType: ProgressionType,
                                      first: IrVariableSymbol,
                                      lastExpression: IrExpression,
                                      step: IrVariableSymbol): IrExpression {
        val symbol = symbols.getProgressionBound[progressionType.elementType]
                ?: throw IllegalArgumentException("Unknown progression element type: ${lastExpression.type}")
        val startOffset = lastExpression.startOffset
        val endOffset = lastExpression.endOffset
        return IrCallImpl(startOffset, lastExpression.endOffset, symbol).apply {
            putValueArgument(0, IrGetValueImpl(startOffset, endOffset, first))
            putValueArgument(1, lastExpression.castIfNecessary(progressionType))
            putValueArgument(2, IrGetValueImpl(startOffset, endOffset, step))
        }
    }
    //endregion

    //region Util classes ====================================================================================================
    // TODO: Replace with a cast when such support is added in the boxing lowering.
    private data class ProgressionType(val elementType: KotlinType,
                                       val numberCastFunctionName: Name)

    private data class ProgressionInfo(
            val progressionType: ProgressionType,
            val first: IrExpression,
            val last: IrExpression,
            val step: IrExpression? = null,
            val increasing: Boolean = true)

    /** Contains information about variables used in the loop. */
    private data class ForLoopInfo(
            val progressionInfo: ProgressionInfo,
            val inductionVariable: IrVariableSymbol,
            val bound: IrVariableSymbol,
            val step: IrVariableSymbol,
            var loopVariable: IrVariableSymbol? = null)

    private inner class ProgressionInfoBuilder : IrElementVisitor<ProgressionInfo?, Nothing?> {

        val INT_PROGRESSION = ProgressionType(context.builtIns.intType, Name.identifier("toInt"))
        val LONG_PROGRESSION = ProgressionType(context.builtIns.longType, Name.identifier("toLong"))
        val CHAR_PROGRESSION = ProgressionType(context.builtIns.charType, Name.identifier("toChar"))

        private fun buildRangeTo(expression: IrCall, progressionType: ProgressionType) =
                ProgressionInfo(progressionType, expression.dispatchReceiver!!, expression.getValueArgument(0)!!)

        private fun buildUntil(expression: IrCall, progressionType: ProgressionType): ProgressionInfo {
            val firstExpression = expression.extensionReceiver!!
            val lastExpression = expression.getValueArgument(0)!!
            val decrementSymbol = symbols.getUnaryOperator(OperatorNameConventions.DEC, lastExpression.type)
            val decrementCall = IrCallImpl(expression.startOffset, expression.endOffset, decrementSymbol).apply {
                dispatchReceiver = lastExpression
            }
            return ProgressionInfo(progressionType, firstExpression, decrementCall)
        }

        private fun buildDownTo(expression: IrCall, progressionType: ProgressionType) =
                ProgressionInfo(progressionType, expression.extensionReceiver!!, expression.getValueArgument(0)!!, increasing = false)

        private fun buildStep(expression: IrCall, progressionType: ProgressionType) =
                expression.extensionReceiver!!.accept(this, null)?.let {
                    val newStep = expression.getValueArgument(0)!!
                    val step = when {
                        it.step == null -> irCheckProgressionStep(progressionType, newStep)
                        // There were step calls before. Just add our check in the container or create a new one.
                        it.step is IrStatementContainer -> {
                            it.step.statements.add(irCheckProgressionStep(progressionType, newStep)); it.step
                        }
                        else -> IrCompositeImpl(expression.startOffset, expression.endOffset, newStep.type).apply {
                            statements.add(it.step)
                            statements.add(irCheckProgressionStep(progressionType, newStep))
                        }
                    }
                    ProgressionInfo(progressionType, it.first, it.last, step, it.increasing)
                }

        override fun visitElement(element: IrElement, data: Nothing?): ProgressionInfo? = null

        override fun visitCall(expression: IrCall, data: Nothing?): ProgressionInfo? {
            val type = expression.type
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

    // Lowering ========================================================================================================
    // Lower a loop header.
    private fun processHeader(variable: IrVariable, initializer: IrCall): IrStatement? {
        val symbol = variable.symbol
        if (!variable.descriptor.type.isSubtypeOf(iteratorType)) {
            return null
        }
        assert(symbol !in iteratorToLoopInfo)

        val builder = context.createIrBuilder(scopeOwnerSymbol, variable.startOffset, variable.endOffset)
        // Collect loop info and form the loop header block.
        val progressionInfo = initializer.dispatchReceiver?.accept(ProgressionInfoBuilder(), null) ?: return null
        return builder.irBlock {
            with(progressionInfo) {
                val inductionVariable = irTemporaryVar(first.castIfNecessary(progressionType), "inductionVariable")
                val stepExpression = if (increasing) { // TODO: Remove the casts.
                    step ?: irConstOne(startOffset, endOffset).castIfNecessary(progressionType, false)
                } else {
                    step?.unaryMinus() ?: irConstMinusOne(startOffset, endOffset).castIfNecessary(progressionType, false)
                }
                val stepValue = irTemporary(stepExpression, "step")
                // TODO: Don't call the check it step is 1 or -1
                val boundExpression = irGetProgressionBound(progressionType, inductionVariable.symbol, last, stepValue.symbol)
                val boundValue = irTemporary(boundExpression, "bound")
                iteratorToLoopInfo[symbol] = ForLoopInfo(progressionInfo,
                        inductionVariable.symbol,
                        boundValue.symbol,
                        stepValue.symbol)
            }
        }
    }


    // Lower getting a next induction variable value.
    private fun processNext(variable: IrVariable, initializer: IrCall): IrExpression? {
        val irIteratorAccess = initializer.dispatchReceiver as? IrGetValue ?: throw AssertionError()
        val forLoopInfo = iteratorToLoopInfo[irIteratorAccess.symbol] ?: return null  // If we didn't lower a corresponding header.
        val builder = context.createIrBuilder(scopeOwnerSymbol, initializer.startOffset, initializer.endOffset)

        // TODO: Cache it.
        val plusOperator = symbols.getBinaryOperator(
                OperatorNameConventions.PLUS,
                forLoopInfo.inductionVariable.descriptor.type,
                forLoopInfo.step.descriptor.type
        )
        forLoopInfo.loopVariable = variable.symbol

        with(builder) {
            variable.initializer = irGet(forLoopInfo.inductionVariable)
            val increment = irSetVar(forLoopInfo.inductionVariable,
                    irCallOp(plusOperator, irGet(forLoopInfo.inductionVariable), irGet(forLoopInfo.step)))
            return IrCompositeImpl(variable.startOffset,
                    variable.endOffset,
                    context.irBuiltIns.unit,
                    IrStatementOrigin.FOR_LOOP_NEXT,
                    listOf(variable, increment))
        }
    }

    private fun DeclarationIrBuilder.buildEmptyCheck(loop: IrLoop, forLoopInfo: ForLoopInfo): IrExpression {
        val increasing = forLoopInfo.progressionInfo.increasing
        val comparingBuiltIn = if (increasing) context.irBuiltIns.lteq0Symbol else context.irBuiltIns.gteq0Symbol

        val compareTo = symbols.getBinaryOperator(OperatorNameConventions.COMPARE_TO,
                forLoopInfo.inductionVariable.descriptor.type,
                forLoopInfo.bound.descriptor.type)

        val check = irCall(comparingBuiltIn).apply {
            putValueArgument(0, irCallOp(compareTo, irGet(forLoopInfo.inductionVariable), irGet(forLoopInfo.bound)))
        }
        return irIfThen(check, loop)
    }

    private fun DeclarationIrBuilder.buildNewCondition(oldCondition: IrExpression): Pair<IrExpression, ForLoopInfo>? {
        if (oldCondition !is IrCall || oldCondition.origin != IrStatementOrigin.FOR_LOOP_HAS_NEXT) {
            return null
        }

        val irIteratorAccess = oldCondition.dispatchReceiver as? IrGetValue ?: throw AssertionError()
        // Return null if we didn't lower a corresponding header.
        val forLoopInfo = iteratorToLoopInfo[irIteratorAccess.symbol] ?: return null
        val loopVariable = forLoopInfo.loopVariable!! // TODO: Check!

        return irCall(context.irBuiltIns.booleanNotSymbol).apply {
            val eqeqCall = irCall(context.irBuiltIns.eqeqSymbol).apply {
                putValueArgument(0, irGet(loopVariable))
                putValueArgument(1, irGet(forLoopInfo.bound))
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
    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        if (loop.origin != IrStatementOrigin.FOR_LOOP_INNER_WHILE) {
            return super.visitWhileLoop(loop)
        }

        with(context.createIrBuilder(scopeOwnerSymbol)) {
            // Transform accesses to the old iterator (see visitVariable method). Store loopVariable in loopInfo.
            val newBody = loop.body?.transform(this@ForLoopsTransformer, null)
            val (newCondition, forLoopInfo) = buildNewCondition(loop.condition) ?: return super.visitWhileLoop(loop)
            assert(forLoopInfo.loopVariable != null)

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
}

