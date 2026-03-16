/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irWhile
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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isImmutable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

private const val ITERATOR = "iterator"
private const val HAS_NEXT = "hasNext"
private const val NEXT = "next"
private const val SEQUENCE_OF = "sequenceOf"
private const val AS_SEQUENCE = "asSequence"
private const val GENERATE_SEQUENCE = "generateSequence"
private const val MAP = "map"
private const val FILTER = "filter"

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
        // appends sequence data to the appropriate IrElements
        irFile.acceptChildrenVoid(gatherer)
        val transformer = SequenceFusionTransformer(context)
        irFile.transformChildrenVoid(transformer)
    }
}

sealed class GenerateSequenceInitialValue {
    class InitialValue(val expression: IrExpression) : GenerateSequenceInitialValue()
    class InitialFunction(val function: IrRichFunctionReference) : GenerateSequenceInitialValue()
    class NoInitialValue : GenerateSequenceInitialValue()
}

// sequenceSource is what the sequence was created from, to be substituted if the loop is to be fused
private sealed class SequenceSource {
    class SequenceOf : SequenceSource()
    class Variable(val variable: IrValueSymbol) : SequenceSource()
    class GenerateSequence(val initialValue: GenerateSequenceInitialValue, val generatingFunction: IrRichFunctionReference) :
        SequenceSource()

    class AsSequence(val iterable: IrExpression) : SequenceSource()
}

typealias IrBuilderWithParent = Pair<IrBuilderWithScope, IrDeclarationParent>

private class SequenceData(
    val mapReplacement: MapReplacement = { _, argument -> argument },
    val sequenceSource: SequenceSource? = null,
    val filterReplacement: FilterReplacement = { _, initialValue, expressionModifier -> expressionModifier(initialValue) },
) {
    // mapReplacement for a given sequence expression stores a composition of functions applied to the base sequence via `map`
    typealias MapReplacement = (IrBuilderWithParent, IrExpression) -> IrExpression

    fun applyMap(function: IrRichFunctionReference): SequenceData =
        SequenceData(
            composeMapReplacements(
                this.mapReplacement
            ) { (builder, parent), argument ->
                builder.callRichFunctionReference(function, parent, argument)
            },
            this.sequenceSource,
            this.filterReplacement
        )

    private fun composeMapReplacements(
        accumulator: MapReplacement,
        newFunction: MapReplacement,
    ): MapReplacement = { builder, argument -> newFunction(builder, accumulator(builder, argument)) }
    /**
     * Filter replacement is constructed like this:
     * ```
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
    typealias FilterReplacement = (IrBuilderWithParent, IrExpression, (IrExpression) -> IrExpression) -> IrExpression

    fun createNewFilterSegment(
        filterFunction: IrRichFunctionReference,
    ): FilterReplacement = { (builder, parent), valueGenerator, expressionDependentOnValue ->
        builder.irBlock {
            val newValue = irTemporary(mapReplacement(builder to parent, valueGenerator))
            val willStay = irTemporary(callRichFunctionReference(filterFunction, parent, irGet(newValue)))
            +irIfThen(context.irBuiltIns.unitType, irGet(willStay), expressionDependentOnValue(irGet(newValue)))
        }
    }

    fun composeFilterReplacements(accumulator: FilterReplacement, nextSegment: FilterReplacement): FilterReplacement =
        { builder, valueGenerator, expressionDependentOnValue ->
            accumulator(builder, valueGenerator) { nextValue -> nextSegment(builder, nextValue, expressionDependentOnValue) }
        }

    fun applyFilter(
        filterFunction: IrRichFunctionReference,
    ): SequenceData {
        val newFilterReplacement = composeFilterReplacements(filterReplacement, createNewFilterSegment(filterFunction))
        // NOTE: mapReplacement is not reassigned, it is reset back to identity
        return SequenceData(
            sequenceSource = sequenceSource,
            filterReplacement = newFilterReplacement,
        )
    }
}

private fun IrBuilderWithScope.callRichFunctionReference(
    ref: IrRichFunctionReference,
    parent: IrDeclarationParent,
    vararg args: IrExpression,
): IrExpression {
    val freshRef = ref.deepCopyWithSymbols(parent)
    val functionType = freshRef.type as? IrSimpleType
    val returnType = functionType?.arguments?.lastOrNull()?.typeOrNull ?: freshRef.overriddenFunctionSymbol.owner.returnType
    return irCall(freshRef.overriddenFunctionSymbol, returnType).apply {
        dispatchReceiver = freshRef
        var index = 1
        for (arg in args) {
            arguments[index++] = arg
        }
    }
}

private fun IrBuilderWithScope.irNotNull(value: IrExpression): IrExpression {
    val nonNullType = value.type.makeNotNull()
    return IrTypeOperatorCallImpl(
        startOffset,
        endOffset,
        nonNullType,
        IrTypeOperator.IMPLICIT_NOTNULL,
        nonNullType,
        value
    )
}

// this is stored for expressions, intended to be passed either to value declarations or to for loops iterated over the expression result
private var IrExpression.sequenceDataOfExpression: SequenceData? by irAttribute(true)

// this is stored to be one of the future sources of sequence data of expressions
private var IrValueDeclaration.sequenceDataOfVariable: SequenceData? by irAttribute(true)
// In general, sequence data is gathered from `sequenceOf` or existing sequence variables, modified `by` map calls,
// and consumed by for loops and variable declarations

private var IrValueDeclaration.usageCounter: Int? by irAttribute(false)

private fun isElementSequence(context: JvmBackendContext, element: IrElement): Boolean {
    val sequenceSymbol = context.symbols.sequence ?: return false
    val type = when (element) {
        is IrExpression -> element.type
        is IrVariable -> element.type
        else -> return false
    }
    return type.isSubtypeOfClass(sequenceSymbol)
}

private fun getInnerMostReceiver(context: JvmBackendContext, expression: IrExpression): IrExpression? {
    when (expression) {
        is IrCall -> {
            if (isSequenceProducer(context, expression)) return expression
            val receiver = expression.arguments.getOrNull(0) ?: return null
            return getInnerMostReceiver(context, receiver)
        }
        is IrGetValue -> return expression
        else -> return null
    }
}

private fun isSequenceProducer(context: JvmBackendContext, expression: IrCall): Boolean {
    if (!isElementSequence(context, expression)) return false
    if (expression.arguments.any { argument -> argument?.let { isElementSequence(context, it) } ?: false }) return false
    // no arguments are sequences, yet it returns a sequence
    return true
}

private class SequenceDataGatherer(val context: JvmBackendContext) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitVariable(declaration: IrVariable) {
        super.visitVariable(declaration)
        if (declaration.isVar) return
        if (!isElementSequence(context, declaration)) return
        val expressionSequenceData = declaration.initializer?.sequenceDataOfExpression
        declaration.symbol.owner.sequenceDataOfVariable = if (expressionSequenceData?.sequenceSource is SequenceSource.GenerateSequence &&
            expressionSequenceData.sequenceSource.initialValue is GenerateSequenceInitialValue.NoInitialValue &&
            (declaration.usageCounter ?: 0) > 1
        ) {
            SequenceData(
                sequenceSource = SequenceSource.Variable(declaration.symbol),
            )
        } else {
            expressionSequenceData
        }
    }

    // assigns sequence data of the variable to the corresponding expression
    override fun visitGetValue(expression: IrGetValue) {
        super.visitGetValue(expression)
        // now the children have assigned appropriate sequence data
        if (!isElementSequence(context, expression)) return
        expression.sequenceDataOfExpression = expression.symbol.owner.sequenceDataOfVariable
                // even if we know nothing about the variable, it could be the case that it will be transformed later, and this can be lowered
            ?: SequenceData(sequenceSource = SequenceSource.Variable(expression.symbol))
    }

    private fun hasLambdaCapturedVariables(function: IrFunction): Boolean {
        val localSymbols = hashSetOf<IrValueSymbol>()
        var hasCaptured = false

        function.parameters.forEach { localSymbols += it.symbol }

        function.body?.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                if (!hasCaptured) {
                    element.acceptChildrenVoid(this)
                }
            }

            override fun visitFunction(declaration: IrFunction) {} // skip nested functions

            override fun visitDeclaration(declaration: IrDeclarationBase) {
                if (declaration is IrValueDeclaration) {
                    localSymbols += declaration.symbol
                }
                super.visitDeclaration(declaration)
            }

            override fun visitGetValue(expression: IrGetValue) {
                if (expression.symbol !in localSymbols) {
                    hasCaptured = true
                }
            }
        })

        return hasCaptured
    }

    private fun isSafeToLower(reference: IrRichFunctionReference): Boolean {
        if (reference.boundValues.isNotEmpty()) return false
        if (reference.invokeFunction.dispatchReceiverParameter != null) return false
        if (hasLambdaCapturedVariables(reference.invokeFunction)) return false
        return true
    }

    // checks if the applied function is safe to be lowered, then updates the sequence data if it is
    private inline fun tryToApplyFunction(
        call: IrCall,
        applyFunction: (SequenceData, IrRichFunctionReference) -> SequenceData
    ) {
        val receiver = call.arguments.getOrNull(0) ?: return
        val fnArg = call.arguments.getOrNull(1) ?: return
        val fnRef = fnArg as? IrRichFunctionReference ?: return
        if (!isSafeToLower(fnRef)) return

        val receiverData = receiver.sequenceDataOfExpression ?: return
        call.sequenceDataOfExpression = applyFunction(receiverData, fnRef)
    }

    private fun containsMutable(expression: IrExpression): Boolean {
        var found = false
        expression.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                if (!found) {
                    element.acceptChildrenVoid(this)
                }
            }

            override fun visitGetValue(expression: IrGetValue) {
                val variable = expression.symbol.owner as? IrVariable ?: return
                if (variable.isVar) {
                    found = true
                }
            }
        })
        return found
    }

    private fun matchWithGenerateSequence(expression: IrCall) {
        val (initialValue, func) = when (expression.arguments.size) {
            1 -> {
                // generateSequence(() -> T?)
                val func = expression.arguments.getOrNull(0) as? IrRichFunctionReference ?: return
                GenerateSequenceInitialValue.NoInitialValue() to func
            }
            2 -> {
                val initialValueOrFunction = expression.arguments.getOrNull(0)
                val func = expression.arguments.getOrNull(1) as? IrRichFunctionReference ?: return
                when (initialValueOrFunction) {
                    is IrRichFunctionReference -> {
                        // generateSequence(() -> T?, (T) -> T?)
                        GenerateSequenceInitialValue.InitialFunction(initialValueOrFunction) to func
                    }
                    else -> {
                        // generateSequence(T?, (T) -> T?)
                        if (initialValueOrFunction == null) return
                        if (containsMutable(initialValueOrFunction)) return
                        GenerateSequenceInitialValue.InitialValue(initialValueOrFunction) to func
                    }
                }
            }
            else -> {
                return
            }
        }
        expression.sequenceDataOfExpression = SequenceData(sequenceSource = SequenceSource.GenerateSequence(initialValue, func))
    }


    private fun matchWithSequenceOf(expression: IrCall) {
        // store the sequence of arguments inside the sequence source
        if (expression.arguments.size > 1) return
        if (expression.arguments.isEmpty()) {
            expression.sequenceDataOfExpression = SequenceData(sequenceSource = SequenceSource.SequenceOf())
            return
        }
        expression.sequenceDataOfExpression = SequenceData(
            sequenceSource = SequenceSource.SequenceOf()
        )
    }

    private fun matchWithAsSequence(expression: IrCall) {
        val innerMostReceiver = getInnerMostReceiver(context, expression) ?: return
        val receiver = expression.arguments.getOrNull(0) ?: return
        if (innerMostReceiver !is IrGetValue) return
        val receiverVariable = innerMostReceiver.symbol.owner
        if (!receiverVariable.isImmutable || !receiverVariable.type.isSubtypeOfClass(context.irBuiltIns.iterableClass)) return
        expression.sequenceDataOfExpression = SequenceData(
            sequenceSource = SequenceSource.AsSequence(receiver)
        )
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)
        if (!isElementSequence(context, expression)) return
        val functionName = expression.symbol.owner.name.asString()
        when (functionName) {
            MAP -> tryToApplyFunction(expression, SequenceData::applyMap)
            FILTER -> tryToApplyFunction(expression, SequenceData::applyFilter)
            GENERATE_SEQUENCE -> matchWithGenerateSequence(expression)
            SEQUENCE_OF -> matchWithSequenceOf(expression)
            AS_SEQUENCE -> matchWithAsSequence(expression)
        }
    }
}

// it is only used after the correct format was checked, so unsafe casts always succeed
private class LoopData(
    val block: IrBlock,
) {
    val iteratorDeclaration: IrVariable
        get() = block.statements[0] as IrVariable

    val loop: IrWhileLoop
        get() = block.statements[1] as IrWhileLoop

    val iterable: IrExpression
        get() = (iteratorDeclaration.initializer as IrCall).arguments[0]!!

    val loopBody: IrBlock
        get() = loop.body as IrBlock

    val nextDeclaration: IrVariable
        get() = loopBody.statements[0] as IrVariable

    fun deepCopy(parent: IrDeclarationParent): LoopData = LoopData(block.deepCopyWithSymbols(parent))
}

private class LoopBodyTransformer(
    val builder: IrBuilderWithScope,
    val oldVariable: IrVariable,
    val newVariable: IrVariable,
) : IrElementTransformerVoidWithContext() {
    override fun visitGetValue(expression: IrGetValue): IrExpression {
        if (expression.symbol == oldVariable.symbol) {
            check(expression.type == newVariable.type)
            return builder.irGet(
                newVariable
            ).apply {
                startOffset = expression.startOffset
                endOffset = expression.endOffset
            }
        }
        return super.visitGetValue(expression)
    }
}

private class BreakContinueUpdater(
    val newLoop: IrLoop,
    val oldLoop: IrLoop
) : IrElementTransformerVoidWithContext() {
    override fun visitBreakContinue(jump: IrBreakContinue): IrExpression {
        if (jump.loop == oldLoop)
            jump.loop = newLoop
        return super.visitBreakContinue(jump)
    }
}

private class ReusedSequenceMarker(val context: JvmBackendContext) : IrVisitorVoid() {
    val sequences = mutableListOf<IrVariable>()
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

private sealed class LoweringStrategy {
    abstract fun lowerLoop(
        builderWithParent: IrBuilderWithParent,
        loopData: LoopData,
        sequenceData: SequenceData,
    ): IrExpression?

    /**
     * Transforms loop body:
     * ```
     *  {
     *      val next = iterator.next()
     *      body(next)
     *  }
     * ```
     * into
     * ```
     *  {
     *      val mappedValue = mapReplacement(filterReplacement(initialValue))
     *      body(mappedValue)
     *  }
     * ```
     */
    protected fun addMapAndFilterReplacementsToBody(
        builderWithParent: IrBuilderWithParent,
        bodyRewriter: (IrVariable) -> IrBlock,
        sequenceData: SequenceData,
        initialValue: IrExpression,
        newBodyOrigin: IrStatementOrigin?,
    ): IrExpression {
        return sequenceData.filterReplacement(
            builderWithParent,
            initialValue,
        ) { filteredValue ->
            val builder = builderWithParent.first
            val mappedValue = sequenceData.mapReplacement(builderWithParent, filteredValue)
            builder.irBlock(origin = newBodyOrigin) {
                val newLoopVariable = scope.createTemporaryVariable(
                    mappedValue,
                    origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE,
                    nameHint = null,
                    inventUniqueName = true,
                )
                +newLoopVariable
                +bodyRewriter(newLoopVariable)
            }
        }
    }

    protected fun createBodyExpectingNewLoopVariable(
        builder: IrBuilderWithScope,
        oldLoopVariable: IrVariable,
        body: IrBlock,
        newLoop: IrLoop,
        oldLoop: IrLoop,
    ): (IrVariable) -> IrBlock = { newLoopVariable ->
        body.transformChildrenVoid(LoopBodyTransformer(builder, oldLoopVariable, newLoopVariable))
        body.transformChildrenVoid(BreakContinueUpdater(newLoop, oldLoop))
        body
    }

    protected fun transformLoopBodyPreservingLoopVariable(
        builderWithParent: IrBuilderWithParent,
        sequenceData: SequenceData,
        loopData: LoopData,
    ): IrBlock {
        val (builder, parent) = builderWithParent
        val copiedLoopData = loopData.deepCopy(parent)
        val loopVariable = lookupForLoopVariable(copiedLoopData.loopBody) ?: return copiedLoopData.block
        copiedLoopData.loopBody.statements.remove(loopVariable)
        val bodyRewriter =
            createBodyExpectingNewLoopVariable(builder, loopVariable, copiedLoopData.loopBody, copiedLoopData.loop, loopData.loop)
        copiedLoopData.loop.body = builder.irBlock {
            +loopVariable
            +addMapAndFilterReplacementsToBody(
                builderWithParent,
                bodyRewriter,
                sequenceData,
                irGet(loopVariable),
                IrStatementOrigin.FOR_LOOP_INNER_WHILE,
            )
        }
        return copiedLoopData.block
    }

    /**
     * If we know that a sequence is a transformation of sequenceOf to which we know the arguments to,
     * we transform a loop into a block evaluating the loop body on each element of the sequence.
     * ```
     * val seq = sequenceOf(1, 2).map { it - 1 }
     * for (el in seq) println(el)
     * ```
     * becomes
     * ```
     * {
     * println({ it - 1 }(1))
     * println({ it - 1 }(2))
     * }
     * ```
     * */
    class SequenceOfStrategy : LoweringStrategy() {
        override fun lowerLoop(builderWithParent: IrBuilderWithParent, loopData: LoopData, sequenceData: SequenceData): IrExpression? {
            return null
        }
    }

    class GenerateSequenceStrategy(val source: SequenceSource.GenerateSequence) : LoweringStrategy() {
        override fun lowerLoop(
            builderWithParent: IrBuilderWithParent,
            loopData: LoopData,
            sequenceData: SequenceData,
        ): IrExpression? {
            val (builder, parent) = builderWithParent
            val generatingFunction = source.generatingFunction
            val nextFromCurrent: (IrVariable) -> IrExpression = { variable ->
                builder.callRichFunctionReference(generatingFunction, parent, builder.irNotNull(builder.irGet(variable)))
            }

            val (initialExpression, nextEvaluator: (IrVariable) -> IrExpression) = when (val initialValue = source.initialValue) {
                is GenerateSequenceInitialValue.InitialValue -> {
                    initialValue.expression.deepCopyWithSymbols(parent) to nextFromCurrent
                }
                is GenerateSequenceInitialValue.InitialFunction -> {
                    builder.callRichFunctionReference(initialValue.function, parent) to nextFromCurrent
                }
                is GenerateSequenceInitialValue.NoInitialValue -> {
                    builder.callRichFunctionReference(
                        generatingFunction,
                        parent
                    ) to { _: IrVariable -> builder.callRichFunctionReference(generatingFunction, parent) }
                }
            }
            val (inductionVariable, newCondition, iteratorNextReplacement) =
                IteratorReplacementForGenerateSequence.create(initialExpression, nextEvaluator, builder)
            return buildNewGenerateSequenceLoop(
                builderWithParent,
                sequenceData,
                loopData,
                inductionVariable,
                newCondition,
                iteratorNextReplacement
            )
        }

        private data class IteratorReplacementForGenerateSequence(
            val iteratorVariable: IrVariable,
            val nextExpression: IrExpression,
            val hasNextReplacement: IrExpression,
        ) {
            companion object {
                fun create(
                    initialExpression: IrExpression,
                    evaluateNext: (IrVariable) -> IrExpression,
                    builder: IrBuilderWithScope,
                ): IteratorReplacementForGenerateSequence = with(builder) {
                    val sequenceElement = scope.createTemporaryVariable(
                        initialExpression,
                        isMutable = true,
                        irType = initialExpression.type.makeNullable(),
                        origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR
                    )
                    val condition = irNotEquals(irGet(sequenceElement), irNull())
                    val next = evaluateNext(sequenceElement)
                    return IteratorReplacementForGenerateSequence(sequenceElement, condition, next)
                }
            }
        }

        private fun buildNewGenerateSequenceLoop(
            builderWithParent: IrBuilderWithParent,
            sequenceData: SequenceData,
            loopData: LoopData,
            inductionVariable: IrVariable,
            newCondition: IrExpression,
            iteratorNextReplacement: IrExpression,
        ): IrExpression? = builderWithParent.first.irBlock(origin = IrStatementOrigin.FOR_LOOP) {
            +inductionVariable
            +irWhile(origin = IrStatementOrigin.FOR_LOOP_INNER_WHILE).apply {
                condition = newCondition
                body = irBlock {
                    val parent = builderWithParent.second
                    val currentSequenceElement = irTemporary(irNotNull(irGet(inductionVariable)))
                    +irSet(inductionVariable, iteratorNextReplacement)
                    val newBody = loopData.loopBody.deepCopyWithSymbols(parent)
                    val loopVariable = lookupForLoopVariable(newBody) ?: return null
                    newBody.statements.remove(loopVariable)
                    val bodyRewriter = createBodyExpectingNewLoopVariable(this, loopVariable, newBody, this@apply, loopData.loop)

                    +addMapAndFilterReplacementsToBody(
                        builderWithParent,
                        bodyRewriter,
                        sequenceData,
                        irGet(currentSequenceElement),
                        IrStatementOrigin.FOR_LOOP_INNER_WHILE,
                    )
                }
            }
        }
    }

    /**
     * We cannot fuse if we iterate over some transformation of a variable, for example,
     * ```
     * fun myFun(sequence: Sequence<Int>) {
     *     val seq2 = sequence.map { it * 2 }
     *     for (el in seq2.map { it + 1 }) {
     *         println(el)
     *     }
     * }
     * ```
     * cannot be lowered, because there is no way of applying { it * 2 } before { it + 1 } without changing the declaration of seq2.
     * But
     * ```
     * fun myFun(sequence: Sequence<Int>) {
     *     val seq2 = sequence.map { it * 2 }
     *     for (el in seq2) {
     *         println(el)
     *     }
     * }
     * ```
     * can be lowered into
     * ```
     * fun myFun(sequence: Sequence<Int>) {
     *     val seq2 = sequence.map { it * 2 }
     *     for (el in seq) {
     *         println({ it * 2 }(el))
     *     }
     * }
     * ```
     * */
    class UnknownVariableStrategy(val newIteratorTarget: IrExpression) : LoweringStrategy() {
        override fun lowerLoop(builderWithParent: IrBuilderWithParent, loopData: LoopData, sequenceData: SequenceData): IrExpression? {
            // if iterable is not IrGetValue, we do not lower, we cannot substitute sequenceSource for sequence.map(...) or sequence.filter(...)
            if (loopData.iterable !is IrGetValue) {
                return null
            }
            val wasUpdated = updateIteratorCalls(
                loopData,
                builderWithParent.first,
                newIteratorTarget,
            )
            if (!wasUpdated) return null
            return transformLoopBodyPreservingLoopVariable(builderWithParent, sequenceData, loopData)
        }

        // updates .iterator(), .hasNext() and .next() calls to be called on newIteratorTarget
        private fun updateIteratorCalls(
            loopData: LoopData,
            builder: IrBuilderWithScope,
            newIteratorTarget: IrExpression,
        ): Boolean = with(builder) {
            val baseType = (newIteratorTarget.type as? IrSimpleType)?.arguments?.getOrNull(0)?.typeOrNull ?: return false
            val iteratorType = context.irBuiltIns.iteratorClass.typeWith(baseType)
            val iteratorCall = rebuildCallWithDifferentReceiver(newIteratorTarget, newIteratorTarget.type, ITERATOR) ?: return false
            val nextCall = rebuildCallWithDifferentReceiver(irGet(loopData.iteratorDeclaration), iteratorType, NEXT) ?: return false
            val hasNextCall = rebuildCallWithDifferentReceiver(irGet(loopData.iteratorDeclaration), iteratorType, HAS_NEXT) ?: return false

            loopData.apply {
                iteratorDeclaration.apply {
                    initializer = iteratorCall
                    type = iteratorType
                }
                nextDeclaration.apply {
                    initializer = nextCall
                    type = baseType
                }
                loop.condition = hasNextCall
            }
            return true
        }

        private fun IrBuilderWithScope.rebuildCallWithDifferentReceiver(
            receiver: IrExpression,
            receiverType: IrType,
            functionName: String,
        ): IrCall? {
            val function = receiverType.getClass()?.functions?.singleOrNull { function ->
                function.name.asString() == functionName && function.parameters.size == 1
            } ?: return null
            return irCall(function.symbol).apply {
                arguments[0] = receiver
            }
        }
    }
}

private class SequenceFusionTransformer(val context: JvmBackendContext) : IrElementTransformerVoidWithContext() {
    private fun matchWithSequenceIteration(block: IrBlock): LoopData? {
        if (block.origin != IrStatementOrigin.FOR_LOOP) return null

        // extract loop iterator variable and loop body from IrBlock
        if (block.statements.size != 2) return null
        val iteratorDeclaration = block.statements[0] as? IrVariable ?: return null
        val loop = block.statements[1] as? IrWhileLoop ?: return null

        val possiblySequenceInitializer = iteratorDeclaration.initializer as? IrCall ?: return null
        val iterable = possiblySequenceInitializer.arguments.firstOrNull() ?: return null
        if (!isElementSequence(context, iterable)) return null
        if (loop.body !is IrBlock) return null
        return LoopData(block)
    }

    private fun SequenceSource.createStrategy(builder: IrBuilderWithScope): LoweringStrategy {
        return when (this) {
            is SequenceSource.AsSequence -> LoweringStrategy.UnknownVariableStrategy(this.iterable)
            is SequenceSource.GenerateSequence -> LoweringStrategy.GenerateSequenceStrategy(this)
            is SequenceSource.SequenceOf -> LoweringStrategy.SequenceOfStrategy()
            is SequenceSource.Variable -> LoweringStrategy.UnknownVariableStrategy(builder.irGet(this.variable.owner))
        }
    }

    // This is where the actual transformation takes place
    override fun visitBlock(expression: IrBlock): IrExpression {
        val result = super.visitBlock(expression)
        if (result !is IrBlock) return result

        val loopData = matchWithSequenceIteration(result) ?: return result
        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val parent = currentScope?.scope?.scopeOwnerSymbol as? IrDeclarationParent ?: currentDeclarationParent ?: return result
        val sequenceData = loopData.iterable.sequenceDataOfExpression ?: return result
        val sequenceSource = sequenceData.sequenceSource ?: return result
        val strategy = sequenceSource.createStrategy(builder)
        return strategy.lowerLoop(builder to parent, loopData, sequenceData) ?: return result
    }
}