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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
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

private sealed class SequenceSource {
    class SequenceOf(val elements: List<IrExpression>) : SequenceSource()
    class Variable(val variable: IrValueSymbol) : SequenceSource()
}

// onNext for a given sequence expression stores a composition of functions applied to the base sequence via `map`;
// it will be used similarly to the lambda given in the example above
// sequenceSource is what the sequence was created from, to be substituted if the loop is to be fused
private class SequenceData(
    val onNext: (IrBuilderWithScope, IrExpression) -> IrExpression = { _, argument -> argument },
    val sequenceSource: SequenceSource? = null,
) {
    fun lift(function: IrRichFunctionReference): SequenceData =
        SequenceData(
            combineFunctions(
                this.onNext
            ) { builder, argument ->
                builder.callRichFunctionReference(function, argument)
            },
            this.sequenceSource
        )
}

private fun IrBuilderWithScope.callRichFunctionReference(ref: IrRichFunctionReference, arg: IrExpression): IrExpression {
    val freshRef = ref.deepCopyWithSymbols()
    val parent = scope.scopeOwnerSymbol.owner as IrDeclarationParent
    freshRef.patchDeclarationParents(parent)
    return irCall(freshRef.overriddenFunctionSymbol).apply {
        dispatchReceiver = freshRef
        arguments[1] = arg
    }
}

private fun combineFunctions(
    accumulator: (IrBuilderWithScope, IrExpression) -> IrExpression,
    newFunction: (IrBuilderWithScope, IrExpression) -> IrExpression
): (IrBuilderWithScope, IrExpression) -> IrExpression = { builder, argument -> newFunction(builder, accumulator(builder, argument)) }

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

    override fun visitCall(expression: IrCall): IrExpression {
        val newExpression = super.visitCall(expression)
        if (newExpression !is IrCall) return newExpression
        val functionName = newExpression.symbol.owner.name.asString()
        if (!isElementSequence(newExpression)) return newExpression
        when (functionName) {
            "map" -> {
                // compose the mapped function with `onNext`
                val mapReceiver = newExpression.arguments.getOrNull(0) ?: return newExpression
                val mappedFunction = newExpression.arguments.getOrNull(1) ?: return newExpression
                if (mappedFunction !is IrRichFunctionReference
                    || hasLambdaCapturedVariables(mappedFunction.invokeFunction)
                ) return newExpression
                newExpression.sequenceDataOfExpression =
                    mapReceiver.sequenceDataOfExpression?.lift(mappedFunction) ?: return newExpression
                return newExpression
            }
            "sequenceOf" -> {
                // store the sequence of arguments inside the sequence source
                val vararg = newExpression.arguments.getOrNull(0) as? IrVararg ?: return newExpression
                if (vararg.elements.any { it !is IrExpression }) return newExpression // do not store data of spread arguments
                val sequenceOfArguments = vararg.elements.map { it as IrExpression }
                newExpression.sequenceDataOfExpression = SequenceData(
                    sequenceSource = SequenceSource.SequenceOf(sequenceOfArguments)
                )
            }
        }
        return newExpression
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val result = super.visitGetValue(expression)
        if (!isElementSequence(expression)) return result
        if (result !is IrGetValue) return result
        val oldSequenceData = result.symbol.owner.sequenceDataOfVariable
        val newSequenceData = SequenceData(
            onNext = oldSequenceData?.onNext ?: { _, argument -> argument },
            sequenceSource = oldSequenceData?.sequenceSource ?: SequenceSource.Variable(result.symbol)
        )
        result.sequenceDataOfExpression = newSequenceData
        return result
    }

    private fun matchWithSequenceIteration(block: IrBlock): Triple<IrVariable, IrExpression, IrWhileLoop>? {
        if (block.origin != IrStatementOrigin.FOR_LOOP) return null

        // extract loop iterator variable and loop body from IrBlock
        if (block.statements.size != 2) return null
        val iteratorDeclaration = block.statements[0] as? IrVariable ?: return null
        val loop = block.statements[1] as? IrWhileLoop ?: return null

        val possiblySequenceInitializer = iteratorDeclaration.initializer as? IrCall ?: return null
        val iterable = possiblySequenceInitializer.arguments.firstOrNull() ?: return null
        if (!isElementSequence(iterable)) return null
        return Triple(iteratorDeclaration, iterable, loop)
    }

    private fun getInnerMostReceiverSequenceData(expression: IrExpression): SequenceData? {
        when (expression) {
            is IrCall -> {
                val callee = expression.symbol.owner
                if (callee.name.asString() == "sequenceOf") return expression.sequenceDataOfExpression
                if (expression.arguments.getOrNull(0) == null) return null
                return getInnerMostReceiverSequenceData(expression.arguments[0]!!)
            }
            is IrGetValue -> return expression.symbol.owner.sequenceDataOfVariable
            else -> return null
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
    private fun buildSequenceOfBlock(
        builder: IrBuilderWithScope,
        iterable: IrExpression,
        loop: IrWhileLoop,
        sequenceSource: SequenceSource.SequenceOf,
    ): IrExpression? {
        return builder.irBlock {
            val onNext = iterable.sequenceDataOfExpression?.onNext ?: return null
            sequenceSource.elements.forEach { element ->
                val newLoopBody = loop.deepCopyWithSymbols().body as? IrBlock ?: return null
                val iteratedVariable = newLoopBody.statements.filterIsInstance<IrVariable>()
                    .firstOrNull { it.origin == IrDeclarationOrigin.FOR_LOOP_VARIABLE }?.symbol ?: return null
                newLoopBody.statements.remove(iteratedVariable.owner)

                val temporary = irTemporary(onNext(builder, element.deepCopyWithSymbols()))
                val iteratedVariableReplacer = object : IrElementTransformerVoidWithContext() {
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        return if (expression.symbol == iteratedVariable) irGet(temporary) else expression
                    }
                }
                newLoopBody.transformChildrenVoid(iteratedVariableReplacer)
                +newLoopBody
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
     * cannot be lowered, because there is no way of applying { it * 2 } before { it + 1 }.
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
    private fun modifyLoopBody(
        builder: IrBuilderWithScope,
        iterable: IrExpression,
        sequenceSource: SequenceSource.Variable,
        innerMostExpressionData: SequenceData,
        iteratorDeclaration: IrVariable,
        loop: IrWhileLoop,
    ) {
        if (iterable !is IrGetValue) {
            return
        }
        // replaces the iterable with the sequence source variable
        val declarationTransformer = object : IrElementTransformerVoidWithContext() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                return if (expression.symbol == iterable.symbol) builder.irGet(sequenceSource.variable.owner) else expression
            }
        }
        // replaces `it.next()` with `onNext(it.next())`, where `onNext` is a composition of all mapped functions in the iterated expression
        val loopTransformer = object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression)
                if (result !is IrCall) return result
                if (expression.origin != IrStatementOrigin.FOR_LOOP_NEXT)
                    return result
                return innerMostExpressionData.onNext(builder, result)
            }
        }
        iteratorDeclaration.transformChildrenVoid(declarationTransformer)
        loop.transformChildrenVoid(loopTransformer)
        return
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        val result = super.visitBlock(expression)
        if (result !is IrBlock) return result

        val (iteratorDeclaration, iterable, loop) = matchWithSequenceIteration(result) ?: return result
        val innerMostExpressionData = getInnerMostReceiverSequenceData(iterable) ?: return result
        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val sequenceSource = innerMostExpressionData.sequenceSource ?: return result

        when (sequenceSource) {
            is SequenceSource.SequenceOf -> {
                return buildSequenceOfBlock(builder, iterable, loop, sequenceSource) ?: result
            }
            is SequenceSource.Variable -> {
                modifyLoopBody(builder, iterable, sequenceSource, innerMostExpressionData, iteratorDeclaration, loop)
                return result
            }
        }
    }
}

