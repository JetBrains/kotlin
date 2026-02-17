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
import org.jetbrains.kotlin.ir.builders.irCall
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
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * transformation:
 * ```
 * val seq = sequenceOf(1, 2, 3).map { it * 2 }.map { it + 1 }
 * for (x in seq) println(x)
 * ```
 * becomes
 * ```
 * val seq = sequenceOf(1, 2, 3)
 * val f1 = { y -> { x -> x * 2 }(y) + 1 }
 * for (x in seq) println(f1(x))
 * ```
 */

class SequenceFusionLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (context.config.enableDebugMode) return
        val transformer = SequenceFusionTransformer(context)
        irFile.transformChildrenVoid(transformer)
    }
}

// onNext for a given sequence expression stores a composition of functions applied to the base sequence via `map`;
// it will be used similarly to `f1` given in the example above
private class SequenceData(
    val onNext: (IrExpression) -> IrExpression = { it },
) {
    fun lift(builder: IrBuilderWithScope, function: IrRichFunctionReference): SequenceData {
        val newOnNext = combineFunctions(
            this.onNext
        ) { argument ->
            builder.callRichFunctionReference(function, argument)
        }
        val newSequenceData = SequenceData(
            newOnNext,
        )
        return newSequenceData
    }
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
    accumulator: (IrExpression) -> IrExpression,
    newFunction: (IrExpression) -> IrExpression
): (IrExpression) -> IrExpression = { x -> newFunction(accumulator(x)) }

// this is stored for expressions, intended to be passed either to value declarations or to for loops iterated over the expression result
private var IrExpression.sequenceDataOfExpression: SequenceData? by irAttribute(true)

// this is stored to be one of the future sources of sequence data of expressions
private var IrValueDeclaration.sequenceDataOfVariable: SequenceData? by irAttribute(true)
// In general, sequence data is gathered from `sequenceOf` or existing sequence variables, modified `by` map calls,
// and consumed by for loops and variable declarations

private class SequenceFusionTransformer(val context: JvmBackendContext) : IrElementTransformerVoidWithContext() {
    private fun isExpressionSequence(element: IrElement): Boolean {
        val sequenceSymbol = context.symbols.sequence ?: return false
        val type = when (element) {
            is IrExpression -> element.type
            is IrVariable -> element.type
            else -> return false
        }
        return type.isSubtypeOfClass(sequenceSymbol)
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        val result = super.visitVariable(declaration)
        if (!isExpressionSequence(declaration)) return result
        declaration.symbol.owner.sequenceDataOfVariable =
            declaration.initializer?.sequenceDataOfExpression ?: return result
        return result
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

    // removes map calls from the IR tree while storing a composed function resulting from composing the removed mapped functions
    override fun visitCall(expression: IrCall): IrExpression {
        // the invariant is that `super.visitCall(expression)` returns an expression which does not contain any map calls except on the outermost layer
        val expressionNoDeepMaps = super.visitCall(expression) as IrCall
        if (!isExpressionSequence(expressionNoDeepMaps)) return expressionNoDeepMaps
        val functionName = expressionNoDeepMaps.symbol.owner.name.asString()
        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        when (functionName) {
            "map" -> {
                // remove calls of `map` and compose the mapped function with `onNext`
                val mapReceiver = expressionNoDeepMaps.arguments.getOrNull(0) ?: return expressionNoDeepMaps
                val mappedFunction = expressionNoDeepMaps.arguments.getOrNull(1) ?: return expressionNoDeepMaps
                if (mappedFunction !is IrRichFunctionReference
                    || hasLambdaCapturedVariables(mappedFunction.invokeFunction)
                ) return expressionNoDeepMaps
                val expressionNoMaps = expressionNoDeepMaps.arguments.getOrNull(0) ?: expressionNoDeepMaps
                expressionNoMaps.sequenceDataOfExpression =
                    mapReceiver.sequenceDataOfExpression?.lift(builder, mappedFunction) ?: return expressionNoDeepMaps
                return expressionNoMaps
            }
            "sequenceOf" -> {
                expressionNoDeepMaps.sequenceDataOfExpression = SequenceData()
            }
        }
        return expressionNoDeepMaps
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val result = super.visitGetValue(expression) as IrGetValue
        result.sequenceDataOfExpression = result.symbol.owner.sequenceDataOfVariable
        return result
    }

    private fun matchWithSequenceIteration(block: IrBlock): Pair<IrWhileLoop, SequenceData>? {
        if (block.origin != IrStatementOrigin.FOR_LOOP) return null

        // extract loop iterator variable and loop body from IrBlock
        if (block.statements.size != 2) return null
        val iteratorDeclaration = block.statements[0] as? IrVariable ?: return null
        val loop = block.statements[1] as? IrWhileLoop ?: return null

        val possiblySequenceInitializer = iteratorDeclaration.initializer as? IrCall ?: return null
        val iterable = possiblySequenceInitializer.arguments.firstOrNull() as? IrGetValue ?: return null
        if (!isExpressionSequence(iterable)) return null
        val sequenceData = iterable.sequenceDataOfExpression ?: return null
        return Pair(loop, sequenceData)
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        val result = super.visitBlock(expression) as IrBlock
        val (loop, sequenceData) = matchWithSequenceIteration(result) ?: return result
        val transformer = SequenceForLoopBodyTransformer(sequenceData.onNext)
        loop.transformChildrenVoid(transformer)
        return result
    }

    // replaces `it.next()` with `onNext(it.next())`, where `onNext` is a composition of all mapped functions in the iterated expression
    private inner class SequenceForLoopBodyTransformer(
        val onNext: (IrExpression) -> IrExpression,
    ) : IrElementTransformerVoidWithContext() {
        override fun visitCall(expression: IrCall): IrExpression {
            val result = super.visitCall(expression) as IrCall
            if (expression.origin != IrStatementOrigin.FOR_LOOP_NEXT)
                return result
            return onNext(result)
        }
    }
}

