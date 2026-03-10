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
import org.jetbrains.kotlin.ir.IrStatement
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
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

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
        val transformer = SequenceFusionTransformer(context)
        irFile.transformChildrenVoid(transformer)
    }
}

sealed class GenerateInitialValue {
    class InitialValue(val expression: IrExpression) : GenerateInitialValue()
    class InitialFunction(val function: IrRichFunctionReference) : GenerateInitialValue()
    class NoInitialValue : GenerateInitialValue()
}

// sequenceSource is what the sequence was created from, to be substituted if the loop is to be fused
private sealed class SequenceSource {
    class SequenceOf(val elements: List<IrExpression>) : SequenceSource()
    class Variable(val variable: IrValueSymbol) : SequenceSource()
    class GenerateSequence(val initialValue: GenerateInitialValue, val generatingFunction: IrRichFunctionReference) : SequenceSource()
}

private class SequenceData(
    val mapReplacement: MapReplacement = { _, argument -> argument },
    val sequenceSource: SequenceSource? = null,
    val filterReplacement: FilterReplacement = { _, valueGenerator, expressionModifier -> expressionModifier(valueGenerator) },
) {
    // mapReplacement for a given sequence expression stores a composition of functions applied to the base sequence via `map`
    typealias MapReplacement = (IrBuilderWithScope, IrExpression) -> IrExpression

    fun applyMap(function: IrRichFunctionReference): SequenceData =
        SequenceData(
            composeMapReplacements(
                this.mapReplacement
            ) { builder, argument ->
                builder.callRichFunctionReference(function, argument)
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
    typealias FilterReplacement = (IrBuilderWithScope, IrExpression, (IrExpression) -> IrExpression) -> IrExpression

    fun createNewFilterSegment(
        filterFunction: IrRichFunctionReference,
    ): FilterReplacement = { builder, valueGenerator, expressionDependentOnValue ->
        builder.irBlock {
            val newValue = irTemporary(mapReplacement(builder, valueGenerator))
            val willStay = irTemporary(callRichFunctionReference(filterFunction, irGet(newValue)))
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

private fun IrBuilderWithScope.callRichFunctionReference(ref: IrRichFunctionReference, vararg args: IrExpression): IrExpression {
    val freshRef = deepCopyAndPatch(ref, this)
    return irCall(freshRef.overriddenFunctionSymbol).apply {
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

private inline fun <reified T : IrElement> deepCopyAndPatch(element: T, builder: IrBuilderWithScope): T {
    val elementCopy = element.deepCopyWithSymbols()
    val parent = builder.scope.scopeOwnerSymbol.owner as? IrDeclarationParent
        ?: error("Provided builder didn't have scopeOwnerSymbol as an IrDeclarationParent")
    elementCopy.patchDeclarationParents(parent)
    return elementCopy
}

// this is stored for expressions, intended to be passed either to value declarations or to for loops iterated over the expression result
private var IrExpression.sequenceDataOfExpression: SequenceData? by irAttribute(true)

// this is stored to be one of the future sources of sequence data of expressions
private var IrValueDeclaration.sequenceDataOfVariable: SequenceData? by irAttribute(true)
// In general, sequence data is gathered from `sequenceOf` or existing sequence variables, modified `by` map calls,
// and consumed by for loops and variable declarations

private class SequenceFusionTransformer(val context: JvmBackendContext) : IrElementTransformerVoidWithContext() {
    private fun isElementSequence(element: IrElement): Boolean {
        val sequenceSymbol = context.symbols.sequence ?: return false
        val type = when (element) {
            is IrExpression -> element.type
            is IrVariable -> element.type
            else -> return false
        }
        return type.isSubtypeOfClass(sequenceSymbol)
    }

    // assigns the sequence data found on the right-hand side to the value declaration
    private inline fun <reified T : IrElement, reified R> visitLValue(
        node: T,
        sequenceDataProvider: (T) -> SequenceData?,
        sequenceDataConsumer: (T) -> IrValueDeclaration,
        check: (T) -> Boolean,
        superVisit: (T) -> R
    ): R {
        val visitResult = superVisit(node)
        val lValueAfterVisit = visitResult as? T ?: return visitResult
        if (!check(lValueAfterVisit)) return visitResult
        sequenceDataConsumer(lValueAfterVisit).sequenceDataOfVariable = sequenceDataProvider(lValueAfterVisit)
        return lValueAfterVisit as? R ?: visitResult
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        return visitLValue(
            node = declaration,
            sequenceDataProvider = { it.initializer?.sequenceDataOfExpression },
            sequenceDataConsumer = { it.symbol.owner },
            check = { isElementSequence(it) },
            superVisit = { super.visitVariable(it) }
        )
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        return visitLValue(
            node = expression,
            sequenceDataProvider = { it.value.sequenceDataOfExpression },
            sequenceDataConsumer = { it.symbol.owner },
            check = { isElementSequence(it.value) },
            superVisit = { super.visitSetValue(it) }
        )
    }

    private fun hasLambdaCapturedVariables(lambda: IrSimpleFunction): Boolean {
        val localsAndParameters = HashSet<IrValueSymbol>()
        for (parameter in lambda.parameters) {
            (parameter as? IrValueDeclaration)?.symbol?.let(localsAndParameters::add)
        }

        var anyCaptured = false
        lambda.body?.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                if (anyCaptured) return
                element.acceptChildrenVoid(this)
            }

            override fun visitGetValue(expression: IrGetValue) {
                if (expression.symbol !in localsAndParameters) {
                    anyCaptured = true
                }
            }

            override fun visitDeclaration(declaration: IrDeclarationBase) {
                (declaration as? IrValueDeclaration)?.symbol?.let(localsAndParameters::add)
                super.visitDeclaration(declaration)
            }
        })
        return anyCaptured
    }

    private inline fun tryToApplyFunction(
        call: IrCall,
        applyFunction: (SequenceData, IrRichFunctionReference) -> SequenceData
    ) {
        val receiver = call.arguments.getOrNull(0) ?: return
        val fnArg = call.arguments.getOrNull(1) ?: return
        val fnRef = fnArg as? IrRichFunctionReference ?: return
        if (hasLambdaCapturedVariables(fnRef.invokeFunction)) return

        val receiverData = receiver.sequenceDataOfExpression ?: return
        call.sequenceDataOfExpression = applyFunction(receiverData, fnRef)
        return
    }

    private fun matchWithGenerateSequence(expression: IrCall) {
        val (initialValue, func) = when (expression.arguments.size) {
            1 -> {
                // generateSequence(() -> T?)
                val func = expression.arguments[0] as? IrRichFunctionReference ?: return
                GenerateInitialValue.NoInitialValue() to func
            }
            2 -> {
                val initialValueOrFunction = expression.arguments[0]
                val func = expression.arguments[1] as? IrRichFunctionReference ?: return
                when (initialValueOrFunction) {
                    is IrRichFunctionReference -> {
                        // generateSequence(() -> T?, (T) -> T?)
                        GenerateInitialValue.InitialFunction(initialValueOrFunction) to func
                    }
                    else -> {
                        // generateSequence(T?, (T) -> T?)
                        if (initialValueOrFunction !is IrExpression) return
                        GenerateInitialValue.InitialValue(initialValueOrFunction) to func
                    }
                }
            }
            else -> null
        } ?: return
        expression.sequenceDataOfExpression = SequenceData(sequenceSource = SequenceSource.GenerateSequence(initialValue, func))
    }

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)
        // now the receiver of the call has assigned appropriate sequence data
        if (!isElementSequence(expression)) return expression
        val functionName = expression.symbol.owner.name.asString()
        when (functionName) {
            "map" -> tryToApplyFunction(expression, SequenceData::applyMap)
            "filter" -> tryToApplyFunction(expression, SequenceData::applyFilter)
            "generateSequence" -> matchWithGenerateSequence(expression)
            "sequenceOf" -> {
                // store the sequence of arguments inside the sequence source
                if (expression.arguments.size != 1) return expression
                val argument = expression.arguments.getOrNull(0) ?: return expression
                val sequenceOfArguments: List<IrExpression>
                if (argument is IrVararg) {
                    // sequenceOf(vararg arguments)
                    if (argument.elements.any { it is IrSpreadElement }) return expression // skip lowering sequenceOf with spread arguments
                    sequenceOfArguments = argument.elements.map { it as IrExpression }
                } else {
                    // sequenceOf(argument)
                    sequenceOfArguments = listOf(argument)
                }
                expression.sequenceDataOfExpression = SequenceData(
                    sequenceSource = SequenceSource.SequenceOf(sequenceOfArguments)
                )
            }
        }
        return expression
    }

    // assigns sequence data of the variable to the corresponding expression
    override fun visitGetValue(expression: IrGetValue): IrExpression {
        super.visitGetValue(expression)
        // now the children have assigned appropriate sequence data
        if (!isElementSequence(expression)) return expression
        expression.sequenceDataOfExpression = expression.symbol.owner.sequenceDataOfVariable
                // even if we know nothing about the variable, it could be the case that it will be transformed later, and this can be lowered
            ?: SequenceData(sequenceSource = SequenceSource.Variable(expression.symbol))
        return expression
    }

    private fun matchWithSequenceIteration(block: IrBlock): IrExpression? {
        if (block.origin != IrStatementOrigin.FOR_LOOP) return null

        // extract loop iterator variable and loop body from IrBlock
        if (block.statements.size != 2) return null
        val iteratorDeclaration = block.statements[0] as? IrVariable ?: return null
        if (block.statements[1] !is IrWhileLoop) return null

        val possiblySequenceInitializer = iteratorDeclaration.initializer as? IrCall ?: return null
        val iterable = possiblySequenceInitializer.arguments.firstOrNull() ?: return null
        if (!isElementSequence(iterable)) return null
        return iterable
    }

    private fun getInnerMostReceiverSequenceData(expression: IrExpression): SequenceData? {
        when (expression) {
            is IrCall -> {
                val callee = expression.symbol.owner
                // TODO: replace by isProducer: has a sequence return type and does not receive a sequence as argument
                if (callee.name.asString() == "sequenceOf") return expression.sequenceDataOfExpression
                val receiver = expression.arguments.getOrNull(0) ?: return null
                return getInnerMostReceiverSequenceData(receiver)
            }
            is IrGetValue -> return expression.symbol.owner.sequenceDataOfVariable
            else -> return null
        }
    }

    private fun lookupForLoopVariable(loopBody: IrBlock): IrVariable? {
        val statement = loopBody.statements.filterIsInstance<IrVariable>()
            .singleOrNull { v -> v.origin == IrDeclarationOrigin.FOR_LOOP_VARIABLE } ?: return null
        return statement
    }

    private fun lowerLoopBody(
        sequenceData: SequenceData,
        builder: IrBuilderWithScope,
        initialValue: IrExpression,
        body: IrBlock
    ): IrExpression {
        return sequenceData.filterReplacement(
            builder,
            initialValue,
        ) { resultOfFilterReplacement ->
            val newBody = deepCopyAndPatch(body, builder)
            val loopVariable = lookupForLoopVariable(newBody)
            loopVariable?.initializer = sequenceData.mapReplacement(builder, resultOfFilterReplacement)
            newBody
        }
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
    private fun lowerFromSequenceOf(
        builder: IrBuilderWithScope,
        sequenceData: SequenceData,
        loopBody: IrBlock,
        sequenceSource: SequenceSource.SequenceOf,
    ): IrExpression {
        return builder.irBlock {
            sequenceSource.elements.forEach { sequenceOfValue ->
                val sequenceOfValueCopy = deepCopyAndPatch(sequenceOfValue, builder)
                +lowerLoopBody(sequenceData, builder, sequenceOfValueCopy, loopBody)
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
    private fun lowerFromUnknownVariable(
        builder: IrBuilderWithScope,
        sequenceSource: SequenceSource.Variable,
        sequenceData: SequenceData,
        loopBlock: IrBlock,
    ): IrBlock? {
        val newBlock = deepCopyAndPatch(loopBlock, builder)
        val iteratorDeclaration = newBlock.statements[0] as IrVariable
        val loop = newBlock.statements[1] as IrWhileLoop
        val iteratorInitializer = iteratorDeclaration.initializer as? IrCall ?: return null
        val body = loop.body as? IrBlock ?: return null
        val iteratorNext = lookupForLoopVariable(body)?.initializer ?: return null

        iteratorInitializer.arguments[0] = builder.irGet(sequenceSource.variable.owner)
        loop.body = lowerLoopBody(sequenceData, builder, iteratorNext, body)
        return newBlock
    }

    private data class IteratorReplacementForGenerateSequence(
        val iteratorVariable: IrVariable,
        val nextExpression: IrExpression,
        val hasNextReplacement: IrExpression,
    )

    private fun createIteratorReplacement(
        initialExpression: IrExpression,
        evaluateNext: (IrVariable) -> IrExpression,
        builder: IrBuilderWithScope,
    ): IteratorReplacementForGenerateSequence {
        with(builder) {
            val sequenceElement = scope.createTemporaryVariable(
                initialExpression,
                isMutable = true,
                irType = initialExpression.type.makeNullable(),
            )
            val condition = irNotEquals(irGet(sequenceElement), irNull())
            val next = evaluateNext(sequenceElement)
            return IteratorReplacementForGenerateSequence(sequenceElement, condition, next)
        }
    }

    private fun extractGenerateSequenceReplacements(
        sequenceSource: SequenceSource.GenerateSequence,
        builder: IrBuilderWithScope,
        generatingFunction: IrRichFunctionReference
    ): IteratorReplacementForGenerateSequence {
        val nextFromCurrent: (IrVariable) -> IrExpression = {
            builder.callRichFunctionReference(generatingFunction, builder.irNotNull(builder.irGet(it)))
        }
        return when (val initialValue = sequenceSource.initialValue) {
            is GenerateInitialValue.InitialValue -> {
                val newExpression = deepCopyAndPatch(initialValue.expression, builder)
                createIteratorReplacement(
                    newExpression,
                    nextFromCurrent,
                    builder
                )
            }
            is GenerateInitialValue.InitialFunction -> {
                createIteratorReplacement(
                    builder.callRichFunctionReference(initialValue.function),
                    nextFromCurrent,
                    builder
                )
            }
            is GenerateInitialValue.NoInitialValue -> {
                createIteratorReplacement(
                    builder.callRichFunctionReference(generatingFunction),
                    { builder.callRichFunctionReference(generatingFunction) },
                    builder
                )
            }
        }
    }

    private fun lowerFromGenerateSequence(
        builder: IrBuilderWithScope,
        sequenceData: SequenceData,
        sequenceSource: SequenceSource.GenerateSequence,
        loopBody: IrBlock,
    ): IrExpression {
        val (inductionVariable, newCondition, iteratorNextReplacement) = extractGenerateSequenceReplacements(
            sequenceSource,
            builder,
            sequenceSource.generatingFunction
        )
        return builder.irBlock {
            +inductionVariable
            +irWhile().apply {
                condition = newCondition
                body = builder.irBlock {
                    val currentSequenceElement = irTemporary(irNotNull(irGet(inductionVariable)))
                    +irSet(inductionVariable, iteratorNextReplacement)
                    +lowerLoopBody(sequenceData, builder, irGet(currentSequenceElement), loopBody)
                }
            }
        }
    }

    // This is where the actual transformation takes place
    override fun visitBlock(expression: IrBlock): IrExpression {
        val result = super.visitBlock(expression)
        if (result !is IrBlock) return result

        val iterable = matchWithSequenceIteration(result) ?: return result
        val innerMostReceiverSequenceData = getInnerMostReceiverSequenceData(iterable) ?: return result
        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val sequenceSource = innerMostReceiverSequenceData.sequenceSource ?: return result
        val sequenceData = iterable.sequenceDataOfExpression ?: return result
        val loop = result.statements[1] as IrWhileLoop
        val loopBody = loop.body as? IrBlock ?: return result

        when (sequenceSource) {
            is SequenceSource.SequenceOf -> {
                return lowerFromSequenceOf(builder, sequenceData, loopBody, sequenceSource)
            }
            is SequenceSource.Variable -> {
                // if iterable is not IrGetValue, we do not lower, we cannot substitute sequenceSource for sequence.map(...) or sequence.filter(...)
                if (iterable !is IrGetValue) {
                    return result
                }
                return lowerFromUnknownVariable(builder, sequenceSource, innerMostReceiverSequenceData, result) ?: result
            }
            is SequenceSource.GenerateSequence -> {
                return lowerFromGenerateSequence(builder, sequenceData, sequenceSource, loopBody)
            }
        }
    }
}