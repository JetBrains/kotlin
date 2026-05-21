/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.GenerateSequenceStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.LoweringStrategy
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBreak
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import kotlin.collections.get
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.SequenceOfStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.UnknownVariableStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.createSequenceWhile
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.irAsNotNull
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

private const val FOR_EACH = "forEach"
private const val FIND = "find"
private const val FIND_LAST = "findLast"
private const val FIRST = "first"
private const val FIRST_NOT_NULL_OF = "firstNotNullOf"
private const val FIRST_NOT_NULL_OF_OR_NULL = "firstNotNullOfOrNull"
private const val FIRST_OR_NULL = "firstOrNull"
private const val LAST = "last"
private const val LAST_OR_NULL = "lastOrNull"
private const val FILTER_TO = "filterTo"
private const val FILTER_NOT_TO = "filterNotTo"
private const val FILTER_NOT_NULL_TO = "filterNotNullTo"

/**
 * transformation:
 * ```
 * fun myFun(seq: Sequence<Int>) {
 *     val seq2 = seq.map { it * 2 }.map { it + 1 }
 *     for (x in seq) println(x)
 * }
 * ```
 * becomes
 * ```
 * fun myFun(seq: Sequence<Int>) {
 *     val seq2 = seq.map { it * 2 }.map { it + 1 }
 *     for (x in seq) println({ y -> { x -> x * 2 }(y) + 1 }(x))
 * }
 * ```
 *
 * ```
 * val seq = sequenceOf(1, 2, 3).map { it * 2 }.map { it + 1 }
 * for (x in seq) println(x)
 * ```
 * becomes
 * ```
 * val seq = sequenceOf(1, 2, 3).map { it * 2 }.map { it + 1 }
 * {
 *     println({ y -> { x -> x * 2 }(y) + 1 }(1))
 *     println({ y -> { x -> x * 2 }(y) + 1 }(2))
 *     println({ y -> { x -> x * 2 }(y) + 1 }(3))
 * }
 * ```
 */

class SequenceFusionLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val reuseMarker = ReusedSequenceMarker(context)
        irFile.acceptChildrenVoid(reuseMarker)
        val gatherer = SequenceDataGatherer(context)
        irFile.acceptChildrenVoid(gatherer)
        val transformer = SequenceFusionTransformer(context)
        irFile.transformChildrenVoid(transformer)
    }
}

internal sealed class GenerateSequenceInitialValue {
    class InitialValue(val expression: IrExpression) : GenerateSequenceInitialValue()
    class InitialFunction(val function: IrRichFunctionReference) : GenerateSequenceInitialValue()
    object NoInitialValue : GenerateSequenceInitialValue()
}

// sequenceSource is what the sequence was created from, to be substituted if the loop is to be fused
internal sealed class SequenceSource {
    class SequenceOf(val elements: List<IrExpression>, val type: IrType) : SequenceSource()
    class Variable(val variable: IrValueSymbol) : SequenceSource()
    class AsSequence(val iterable: IrExpression) : SequenceSource()
    class GenerateSequence(
        val initialValue: GenerateSequenceInitialValue,
        val generatingFunction: IrRichFunctionReference,
        val sequenceElementType: IrType
    ) : SequenceSource()
}

internal typealias IrBuilderWithParent = Pair<IrBuilderWithScope, IrDeclarationParent>

internal class SequenceData(
    val mapReplacement: MapReplacement,
    val sequenceSource: SequenceSource,
    val newLoopPrologue: LoopPrologue,
    val declarationsBeforeLoop: PreLoopDeclarations,
    val offsets: List<Pair<Int, Int>>,
) {
    // mapReplacement for a given sequence expression stores a composition of functions applied to the base sequence via `map`
    internal fun interface MapReplacement {
        operator fun invoke(
            builderWithParent: IrBuilderWithParent,
            expression: IrExpression
        ): IrExpression
    }

    internal fun interface LoopPrologue {
        operator fun invoke(
            builder: IrBuilderWithParent,
            loop: IrLoop,
            expression: IrExpression,
            transformBody: (IrExpression) -> IrExpression
        ): IrExpression
    }

    internal fun interface PreLoopDeclarations {
        operator fun invoke(
            builder: IrBuilderWithScope
        ): MutableList<IrVariable>
    }

    internal fun createMapReplacement(function: IrRichFunctionReference): MapReplacement =
        { builderWithParent: IrBuilderWithParent, argument: IrExpression ->
            builderWithParent.first.callRichFunctionReference(function, builderWithParent.second, argument)
        }

    internal fun createMapReplacement(function: IrRichFunctionReference, index: IrExpression): MapReplacement =
        { builderWithParent: IrBuilderWithParent, argument: IrExpression ->
            builderWithParent.first.callRichFunctionReference(function, builderWithParent.second, index, argument)
        }

    internal fun applyMap(newMapReplacement: MapReplacement, offsets: Pair<Int, Int>): SequenceData {
        return SequenceData(
            composeMapReplacements(this.mapReplacement, newMapReplacement),
            this.sequenceSource,
            this.newLoopPrologue,
            this.declarationsBeforeLoop,
            this.offsets + offsets
        )
    }

    private fun composeMapReplacements(
        accumulator: MapReplacement,
        newFunction: MapReplacement,
    ): MapReplacement = { builder, argument -> newFunction(builder, accumulator(builder, argument)) }

    /**
     * Filter replacement is constructed like this:
     * ``Block`
     *    { initialValue, expressionDependentOnValue ->
     *        val value1 = firstMapReplacement(initialValue)
     *       val isNotFiltered1 = firstFilter(value1)
     *       if (isNotFiltered1) {
     *           val value2 = secondMapReplacement(value1)
     *           val isNotFiltered2 = secondFilter(value2)
     *           if (isNotFiltered2) {
     *               ... {
     *                   expressionDependentOnValue(finalValue)
     *               }
     *           }
     *        }
     *    }
     * ```
     */

    internal fun createNewFilterSegment(
        filterFunction: IrRichFunctionReference,
    ): LoopPrologue = { builderWithParent, _, valueGenerator, expressionDependentOnValue ->
        builderWithParent.first.irBlock {
            val newValue = irTemporary(mapReplacement(builderWithParent, valueGenerator))
            val willStay = irTemporary(callRichFunctionReference(filterFunction, builderWithParent.second, irGet(newValue)))
            +irIfThen(context.irBuiltIns.unitType, irGet(willStay), expressionDependentOnValue(irGet(newValue)))
        }
    }

    internal fun createNewFilterNotSegment(
        filterFunction: IrRichFunctionReference,
    ): LoopPrologue = { builderWithParent, _, valueGenerator, expressionDependentOnValue ->
        builderWithParent.first.irBlock {
            val newValue = irTemporary(mapReplacement(builderWithParent, valueGenerator))
            val willStay = irTemporary(callRichFunctionReference(filterFunction, builderWithParent.second, irGet(newValue)))
            +irIfThen(context.irBuiltIns.unitType, irNot(irGet(willStay)), expressionDependentOnValue(irGet(newValue)))
        }
    }

    internal fun createNewFilterNotNullSegment(): LoopPrologue = { builderWithParent, _, valueGenerator, expressionDependentOnValue ->
        builderWithParent.first.irBlock {
            val newValue = irTemporary(mapReplacement(builderWithParent, valueGenerator))
            val willStay = irTemporary(irNot(irEquals(irGet(newValue), irNull())))
            +irIfThen(context.irBuiltIns.unitType, irGet(willStay), expressionDependentOnValue(irGet(newValue)))
        }
    }

    private fun composeFilterReplacements(accumulator: LoopPrologue, nextSegment: LoopPrologue): LoopPrologue =
        { builder, loop, valueGenerator, expressionDependentOnValue ->
            accumulator(builder, loop, valueGenerator) { nextValue -> nextSegment(builder, loop, nextValue, expressionDependentOnValue) }
        }

    internal fun applyFilter(
        offsets: Pair<Int, Int>,
        newSegment: LoopPrologue,
    ): SequenceData {
        val newLoopPrologue = composeFilterReplacements(this@SequenceData.newLoopPrologue, newSegment)
        return SequenceData(
            defaultMapReplacement,
            sequenceSource,
            newLoopPrologue,
            declarationsBeforeLoop,
            this.offsets + offsets
        )
    }

    /**
     * Take replacement has two parts: the take variable declaration and the actual place where we check if we have taken enough
     * ```
     * seq.someMapsAndFilters.take(n)
     * for (i in seq) {body(i)}
     * ```
     * becomes
     * ```
     * val takeCount = 0
     * while(...) {
     *     filterReplacement
     *     mapReplacement
     *     takeCount++
     *     if (takeCount > n) break
     *     body(mapReplacementValue)
     * }
     * ```
     * filterReplacement + mapReplacement is inside the loop already
     */

    private fun createNewTakeVariable(builder: IrBuilderWithScope): IrVariable {
        return builder.scope.createTemporaryVariable(
            builder.irInt(0),
            isMutable = true,
            nameHint = "takeVar"
        )
    }

    private fun createTakeReplacement(
        builderWithParent: IrBuilderWithParent,
        valueGenerator: IrExpression,
        expressionDependentOnValue: (IrExpression) -> IrExpression,
        getOrCreateTakeVariable: (IrBuilderWithScope) -> IrVariable,
        loop: IrLoop,
        takeArgument: IrExpression,
    ): IrExpression {
        val builder = builderWithParent.first
        val takeVariable = getOrCreateTakeVariable(builder)
        val classifier = takeVariable.type.classifierOrNull
        val lessThanSymbol = builder.context.irBuiltIns.lessFunByOperandType[classifier]
            ?: error("No lessThan function found for type ${takeVariable.type}")
        return builder.irBlock {
            val tmp = irTemporary(mapReplacement(builderWithParent, valueGenerator))

            // takeVariable++
            +irSet(takeVariable, irCall(context.irBuiltIns.intPlusSymbol).apply {
                dispatchReceiver = irGet(takeVariable)
                arguments[1] = irInt(1)
            })

            // if (takeVariable > takeArgument) break
            +irIfThen(
                builder.context.irBuiltIns.unitType,
                irCall(lessThanSymbol).apply {
                    arguments[0] = takeArgument.deepCopyWithSymbols(builderWithParent.second)
                    arguments[1] = irGet(takeVariable)
                },
                irBreak(loop)
            )
            +expressionDependentOnValue(irGet(tmp))
        }
    }

    fun applyTake(
        takeArgument: IrExpression,
        offsets: Pair<Int, Int>,
    ): SequenceData {
        val takeVariableCell = object {
            var value: IrVariable? = null
        }
        val getOrCreateTakeVariable = { builder: IrBuilderWithScope ->
            takeVariableCell.value ?: createNewTakeVariable(builder).also {
                takeVariableCell.value = it
            }
        }

        val newFilterReplacement = composeFilterReplacements(
            this.newLoopPrologue
        ) { builderWithParent, loop, valueGenerator, expressionDependentOnValue ->
            createTakeReplacement(
                builderWithParent,
                valueGenerator,
                expressionDependentOnValue,
                getOrCreateTakeVariable,
                loop,
                takeArgument,
            )
        }

        val newTakeVariableDeclarations = { builder: IrBuilderWithScope ->
            val takeVariable = getOrCreateTakeVariable(builder)
            val declarations = declarationsBeforeLoop(builder)
            declarations.add(takeVariable)
            declarations
        }

        return SequenceData(
            defaultMapReplacement,
            this.sequenceSource,
            newFilterReplacement,
            newTakeVariableDeclarations,
            this.offsets + offsets
        )
    }

    internal fun addDeclaration(declaration: IrVariable): SequenceData {
        val newDeclarations = { builder: IrBuilderWithScope ->
            val declarations = declarationsBeforeLoop(builder)
            declarations.add(declaration)
            declarations
        }
        return SequenceData(
            this.mapReplacement,
            this.sequenceSource,
            this.newLoopPrologue,
            newDeclarations,
            this.offsets
        )
    }

    internal fun addDeclarationExpectingBuilder(declaration: (IrBuilderWithScope) -> IrVariable): SequenceData {
        val newDeclarations = { builder: IrBuilderWithScope ->
            val declarations = declarationsBeforeLoop(builder)
            declarations.add(declaration(builder))
            declarations
        }
        return SequenceData(
            this.mapReplacement,
            this.sequenceSource,
            this.newLoopPrologue,
            newDeclarations,
            this.offsets
        )
    }

    companion object {
        val defaultMapReplacement: MapReplacement = { _, value -> value }
        val defaultLoopPrologue: LoopPrologue = { _, _, value, expressionExpectingValue -> expressionExpectingValue(value) }
        val defaultTakeVariableDeclarations: (IrBuilderWithScope) -> MutableList<IrVariable> =
            { _ -> mutableListOf() }
    }
}

internal fun IrBuilderWithScope.callRichFunctionReference(
    ref: IrRichFunctionReference,
    parent: IrDeclarationParent,
    vararg args: IrExpression,
): IrExpression {
    val freshRef = ref.deepCopyWithSymbols(parent)
    val functionType = freshRef.type as? IrSimpleType
    val returnType = functionType?.arguments?.lastOrNull()?.typeOrNull ?: freshRef.overriddenFunctionSymbol.owner.returnType
    return irCall(freshRef.overriddenFunctionSymbol, returnType).apply {
        dispatchReceiver = freshRef
        arguments.assignFrom(listOf(freshRef) + args)
    }
}

internal fun isElementSequence(context: JvmBackendContext, element: IrElement): Boolean {
    val sequenceSymbol = context.symbols.sequence ?: return false
    val type = when (element) {
        is IrExpression -> element.type
        is IrVariable -> element.type
        else -> return false
    }
    return type.isSubtypeOfClass(sequenceSymbol)
}

internal fun getInnerMostReceiver(expression: IrExpression): IrExpression? {
    when (expression) {
        is IrCall -> {
            val receiver = expression.arguments.getOrNull(0) ?: return null
            return getInnerMostReceiver(receiver)
        }
        is IrGetValue -> return expression
        else -> return null
    }
}

internal var IrValueDeclaration.usageCounter: Int? by irAttribute(false)

private class ReusedSequenceMarker(val context: JvmBackendContext) : IrVisitorVoid() {
    val sequences = mutableSetOf<IrVariable>()
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitGetValue(expression: IrGetValue) {
        if (sequences.any { it == expression.symbol.owner }) {
            expression.symbol.owner.usageCounter = (expression.symbol.owner.usageCounter ?: 0) + 1
        }
        super.visitGetValue(expression)
    }

    override fun visitVariable(declaration: IrVariable) {
        if (declaration.initializer != null && isElementSequence(context, declaration.initializer!!)) sequences.add(declaration)
        super.visitVariable(declaration)
    }
}

private fun lookupForLoopVariable(loopBody: IrBlock): IrVariable? = loopBody.statements.filterIsInstance<IrVariable>()
    .singleOrNull { v -> v.origin == IrDeclarationOrigin.FOR_LOOP_VARIABLE }

private class SequenceFusionTransformer(val context: JvmBackendContext) : IrElementTransformerVoidWithContext() {
    private data class LoopData(
        val loop: IrLoop?,
        val loopVariable: IrVariable,
        val loopBody: IrBlock,
    )

    private fun gatherLoopData(block: IrBlock, parent: IrDeclarationParent): LoopData? {
        if (block.origin != IrStatementOrigin.FOR_LOOP) return null

        // extract loop iterator variable and loop body from IrBlock
        if (block.statements.size != 2) return null
        val blockCopy = block.deepCopyWithSymbols(parent)
        val iteratorDeclaration = blockCopy.statements[0] as? IrVariable ?: return null
        val loop = blockCopy.statements[1] as? IrWhileLoop ?: return null

        val possiblySequenceInitializer = iteratorDeclaration.initializer as? IrCall ?: return null
        val iterable = possiblySequenceInitializer.arguments.firstOrNull() ?: return null
        if (!isElementSequence(context, iterable)) return null
        if (loop.body !is IrBlock) return null
        val body = loop.body as IrBlock
        val loopVariable = lookupForLoopVariable(body) ?: return null
        body.statements.remove(loopVariable)
        return LoopData(loop, loopVariable, body)
    }

    private data class FunctionData(val function: IrRichFunctionReference)

    private fun gatherFunctionData(call: IrCall, parent: IrDeclarationParent): FunctionData? {
        val function = call.arguments.getOrNull(1) as? IrRichFunctionReference ?: return null
        val copiedFunction = function.deepCopyWithSymbols(parent)
        return FunctionData(copiedFunction)
    }

    private fun SequenceSource.createStrategy(
        builder: IrBuilderWithScope,
    ): LoweringStrategy = when (this) {
        is SequenceSource.AsSequence -> UnknownVariableStrategy(this.iterable)
        is SequenceSource.GenerateSequence -> GenerateSequenceStrategy(this)
        is SequenceSource.SequenceOf -> SequenceOfStrategy(this)
        is SequenceSource.Variable -> UnknownVariableStrategy(builder.irGet(this.variable.owner))
    }

    private fun createFirstBody(builder: IrBuilderWithScope): Pair<IrLoop, (IrExpression, IrVariable) -> IrExpression> {
        val loop = builder.createSequenceWhile()
        val updateVariableBlock = { argument: IrExpression, resultVariable: IrVariable ->
            builder.irBlock {
                +irSet(resultVariable, argument)
                +irBreak(loop)
            }
        }
        return loop to updateVariableBlock
    }

    private fun createLastBody(builder: IrBuilderWithScope): Pair<IrLoop, (IrExpression, IrVariable) -> IrExpression> {
        val loop = builder.createSequenceWhile()
        val updateVariableBlock = { argument: IrExpression, resultVariable: IrVariable ->
            builder.irBlock {
                +irSet(resultVariable, argument)
            }
        }
        return loop to updateVariableBlock
    }

    private fun createFirstLastDeclarations(
        expression: IrCall,
        builder: IrBuilderWithScope,
    ): Pair<IrVariable, IrVariable> {
        val resultVariable = builder.scope.createTemporaryVariable(
            builder.irNull(),
            isMutable = true,
            irType = expression.type.makeNullable(),
            nameHint = "FirstLastResult"
        )
        val skippedIterationVariable = builder.scope.createTemporaryVariable(
            builder.irTrue(),
            isMutable = true,
            irType = context.irBuiltIns.booleanType,
            nameHint = "skippedIteration"
        )
        return resultVariable to skippedIterationVariable
    }

    private fun createFirstLastBody(
        strategy: LoweringStrategy,
        builderWithParent: IrBuilderWithParent,
        oldBody: (IrVariable) -> IrContainerExpression,
        sequenceData: SequenceData,
        loop: IrLoop,
        isOrNull: Boolean,
        skippedIterationVariable: IrVariable,
    ): IrContainerExpression? {
        val builder = builderWithParent.first
        val newBody =
            strategy.lowerLoop(
                builderWithParent,
                oldBody,
                sequenceData,
                loop,
                null
            )
                ?: return null

        val notFoundStatement = if (isOrNull) null else builder.irIfThen(
            context.irBuiltIns.unitType,
            builder.irGet(skippedIterationVariable),
            builder.irThrow(
                builder.irCallConstructor(context.symbols.noSuchElementExceptionCtorString, emptyList()).apply {
                    arguments[0] = builder.irString("Sequence is empty.")
                }
            )
        )

        if (notFoundStatement != null)
            newBody.statements.add(notFoundStatement)
        return newBody
    }

    private inline fun firstLastDeclarations(
        expression: IrCall,
        sequenceData: SequenceData,
        builderWithParent: IrBuilderWithParent,
        block: (
            builder: IrBuilderWithScope,
            parent: IrDeclarationParent,
            updatedData: SequenceData,
            strategy: LoweringStrategy,
            predicate: IrRichFunctionReference?,
            resultVariable: IrVariable,
            skippedIterationVariable: IrVariable,
        ) -> IrExpression
    ): IrExpression? {
        val builder = builderWithParent.first
        val declarations = createFirstLastDeclarations(expression, builder)
        val resultVariable = declarations.first
        val skippedIterationVariable = declarations.second
        val updatedSequenceData = sequenceData.addDeclaration(resultVariable).addDeclaration(skippedIterationVariable)
        val strategy = updatedSequenceData.sequenceSource.createStrategy(builder)
        val lambda = expression.arguments.getOrNull(1)
        if (lambda != null) {
            if (lambda !is IrRichFunctionReference) return null
        }
        return block(builder, builderWithParent.second, updatedSequenceData, strategy, lambda, resultVariable, skippedIterationVariable)
    }

    private fun handleFirstLast(
        builderWithParent: IrBuilderWithParent,
        expression: IrCall,
        sequenceData: SequenceData,
        updateVariableBlock: (IrExpression, IrVariable) -> IrExpression,
        loop: IrLoop,
        isOrNull: Boolean,
    ): IrExpression? {
        return firstLastDeclarations(
            expression,
            sequenceData,
            builderWithParent
        ) { builder, parent, updatedSequenceData, strategy, predicateLambda, resultVariable, skippedIterationVariable ->
            val predicate = predicateLambda?.let {
                { argument: IrExpression -> builder.callRichFunctionReference(it, parent, argument) }
            }
            val oldBody = { loopVariable: IrVariable ->
                val thenPart = builder.irBlock {
                    +irSet(skippedIterationVariable, builder.irFalse())
                    +updateVariableBlock(irGet(loopVariable), resultVariable)
                }
                builder.irBlock {
                    if (predicate != null) {
                        +irIfThen(
                            context.irBuiltIns.unitType,
                            predicate(irGet(loopVariable)),
                            thenPart
                        )
                    } else +thenPart
                }
            }
            val newBody =
                createFirstLastBody(strategy, builderWithParent, oldBody, updatedSequenceData, loop, isOrNull, skippedIterationVariable)
                    ?: return null
            if (expression.type != resultVariable.type) {
                newBody.statements.add(builder.irAsNotNull(builder.irGet(resultVariable)))
            } else {
                newBody.statements.add(builder.irGet(resultVariable))
            }
            newBody.type = expression.type
            return newBody
        }
    }

    private fun handleFirstNotNullOf(
        builderWithParent: IrBuilderWithParent,
        expression: IrCall,
        sequenceData: SequenceData,
        updateVariableBlock: (IrExpression, IrVariable) -> IrExpression,
        loop: IrLoop,
        isOrNull: Boolean,
    ): IrExpression? {
        return firstLastDeclarations(
            expression,
            sequenceData,
            builderWithParent
        ) { builder, parent, updatedSequenceData, strategy, transformLambda, resultVariable, skippedIterationVariable ->
            val transform = transformLambda?.let {
                { argument: IrExpression -> builder.callRichFunctionReference(it, parent, argument) }
            } ?: return null

            val oldBody = { loopVariable: IrVariable ->
                val resultValue = builder.scope.createTemporaryVariable(transform(builder.irGet(loopVariable)))
                val thenPart = builder.irBlock {
                    +irSet(skippedIterationVariable, builder.irFalse())
                    +updateVariableBlock(irGet(resultValue), resultVariable)
                }
                builder.irBlock {
                    +resultValue
                    +irIfThen(
                        context.irBuiltIns.unitType,
                        irNot(irEquals(irGet(resultValue), irNull())),
                        thenPart
                    )
                }
            }
            val newBody =
                createFirstLastBody(strategy, builderWithParent, oldBody, updatedSequenceData, loop, isOrNull, skippedIterationVariable)
                    ?: return null
            newBody.statements.add(builder.irAsNotNull(builder.irGet(resultVariable)))
            newBody.type = expression.type
            return newBody
        }
    }

    private fun handleFind(expression: IrCall, builderWithParent: IrBuilderWithParent, isFirst: Boolean): IrExpression? {
        val builder = builderWithParent.first
        val sequenceData = expression.arguments.getOrNull(0)?.sequenceDataOfExpression ?: return null
        val findPredicate = expression.arguments.getOrNull(1) as? IrRichFunctionReference ?: return null
        val loop = builder.createSequenceWhile()
        val resultVariable = builder.scope.createTemporaryVariable(builder.irNull(), isMutable = true, irType = expression.type)
        val findBody = { loopVariable: IrVariable ->
            builder.irBlock {
                val predicateCall = callRichFunctionReference(findPredicate, builderWithParent.second, irGet(loopVariable))
                val isFoundVariable = irTemporary(predicateCall)
                val thenPart = irBlock {
                    +irSet(resultVariable, irGet(loopVariable))
                    if (isFirst) +irBreak(loop)
                }
                +irIfThen(context.irBuiltIns.unitType, irGet(isFoundVariable), thenPart)
            }
        }

        val updatedSequenceData = sequenceData.addDeclaration(resultVariable)
        val strategy = updatedSequenceData.sequenceSource.createStrategy(builder)
        val newBody = strategy.lowerLoop(builderWithParent, findBody, updatedSequenceData, loop, null) ?: return null
        newBody.statements.add(builder.irGet(resultVariable))
        newBody.type = expression.type
        return newBody
    }

    private fun handleFilterTo(builderWithParent: IrBuilderWithParent, expression: IrCall, version: FilterVersion): IrExpression? {
        val builder = builderWithParent.first
        val parent = builderWithParent.second
        val destination = expression.arguments.getOrNull(1) ?: return null
        val predicate = expression.arguments.getOrNull(2) as? IrRichFunctionReference
        if (version !is FilterVersion.FilterNotNull && predicate == null) return null
        val sequenceData = expression.arguments.getOrNull(0)?.sequenceDataOfExpression ?: return null

        val addFunction = context.irBuiltIns.mutableCollectionClass.owner.functions.singleOrNull {
            it.name.asString() == "add" && it.parameters.size == 2
        } ?: return expression
        val loop = builder.createSequenceWhile()
        val destinationVariable = builder.scope.createTemporaryVariable(destination, "filterToDestination")
        val updatedSequenceData = sequenceData.addDeclaration(destinationVariable)
        val body = { loopVariable: IrVariable ->
            builder.irBlock {
                val destinationAddCall = builder.irCall(addFunction).apply {
                    arguments[0] = irGet(destinationVariable)
                    if (version == FilterVersion.FilterNotNull) {
                        arguments[1] = irAsNotNull(irGet(loopVariable))
                    } else {
                        arguments[1] = irGet(loopVariable)
                    }
                }
                val shouldAddVariableCheck = when (version) {
                    FilterVersion.Filter -> callRichFunctionReference(predicate!!, parent, irGet(loopVariable))
                    FilterVersion.FilterNot -> irNot(callRichFunctionReference(predicate!!, parent, irGet(loopVariable)))
                    FilterVersion.FilterNotNull -> irNot(irEquals(irGet(loopVariable), irNull()))
                }
                +irIfThen(
                    context.irBuiltIns.unitType,
                    shouldAddVariableCheck,
                    destinationAddCall
                )
            }
        }

        val strategy = updatedSequenceData.sequenceSource.createStrategy(builder)
        val newBody = strategy.lowerLoop(builderWithParent, body, updatedSequenceData, loop, null) ?: return null
        newBody.statements.add(builder.irGet(destinationVariable))
        newBody.type = destination.type
        return newBody
    }

    // This is where the actual transformation takes place
    override fun visitBlock(expression: IrBlock): IrExpression {
        val result = super.visitBlock(expression)
        if (result !is IrBlock) return result

        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val parent = currentScope?.scope?.scopeOwnerSymbol as? IrDeclarationParent ?: currentDeclarationParent ?: return result
        val loopData = gatherLoopData(result, parent) ?: return result
        val receiver =
            ((expression.statements.getOrNull(0) as? IrVariable)?.initializer as? IrCall)?.arguments?.getOrNull(0) ?: return result
        val sequenceData = receiver.sequenceDataOfExpression ?: return result
        val strategy = sequenceData.sequenceSource.createStrategy(builder)
        val results = strategy.prepareLoopBody(loopData.loopBody, builder, loopData.loopVariable, loopData.loop)
        val preparedBody = results.first
        val newLoop = results.second
        return strategy.lowerLoop(builder to parent, preparedBody, sequenceData, newLoop, loopData.loopVariable) ?: result
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val functionName = expression.symbol.owner.name.asString()
        val visitedExpression = super.visitCall(expression) as? IrCall ?: return expression
        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val parent =
            currentScope?.scope?.scopeOwnerSymbol as? IrDeclarationParent ?: currentDeclarationParent ?: return visitedExpression
        val receiver = expression.arguments.getOrNull(0) ?: return visitedExpression
        if (!isElementSequence(context, receiver)) return visitedExpression
        val sequenceData = receiver.sequenceDataOfExpression ?: return visitedExpression
        val newExpression = when (functionName) {
            FOR_EACH -> {
                val functionData = gatherFunctionData(visitedExpression, parent) ?: return visitedExpression
                val strategy = sequenceData.sequenceSource.createStrategy(builder)
                strategy.lowerFunction(builder to parent, functionData.function, sequenceData) ?: visitedExpression
            }
            FIND -> handleFind(visitedExpression, builder to parent, isFirst = true)
            FIND_LAST -> handleFind(visitedExpression, builder to parent, isFirst = false)
            FIRST -> {
                val results = createFirstBody(builder)
                val loop = results.first
                val updateVariableBlock = results.second
                handleFirstLast(builder to parent, visitedExpression, sequenceData, updateVariableBlock, loop, isOrNull = false)
            }
            FIRST_OR_NULL -> {
                val results = createFirstBody(builder)
                val loop = results.first
                val updateVariableBlock = results.second
                handleFirstLast(builder to parent, visitedExpression, sequenceData, updateVariableBlock, loop, isOrNull = true)
            }
            FIRST_NOT_NULL_OF -> {
                val results = createFirstBody(builder)
                val loop = results.first
                val updateVariableBlock = results.second
                handleFirstNotNullOf(builder to parent, visitedExpression, sequenceData, updateVariableBlock, loop, isOrNull = false)
            }
            FIRST_NOT_NULL_OF_OR_NULL -> {
                val results = createFirstBody(builder)
                val loop = results.first
                val updateVariableBlock = results.second
                handleFirstNotNullOf(builder to parent, visitedExpression, sequenceData, updateVariableBlock, loop, isOrNull = true)
            }
            LAST -> {
                val results = createLastBody(builder)
                val loop = results.first
                val updateVariableBlock = results.second
                handleFirstLast(builder to parent, visitedExpression, sequenceData, updateVariableBlock, loop, isOrNull = false)
            }
            LAST_OR_NULL -> {
                val results = createLastBody(builder)
                val loop = results.first
                val updateVariableBlock = results.second
                handleFirstLast(builder to parent, visitedExpression, sequenceData, updateVariableBlock, loop, isOrNull = true)
            }
            FILTER_TO -> handleFilterTo(builder to parent, visitedExpression, FilterVersion.Filter)
            FILTER_NOT_TO -> handleFilterTo(builder to parent, visitedExpression, FilterVersion.FilterNot)
            FILTER_NOT_NULL_TO -> handleFilterTo(builder to parent, visitedExpression, FilterVersion.FilterNotNull)
            else -> null
        }
        if (newExpression == null) return visitedExpression
        return if (sequenceData.offsets.size > 1) {
            builder.irBlock {
                irTemporary(receiver.deepCopyWithSymbols(parent))
                +newExpression
            }
        } else newExpression
    }
}
