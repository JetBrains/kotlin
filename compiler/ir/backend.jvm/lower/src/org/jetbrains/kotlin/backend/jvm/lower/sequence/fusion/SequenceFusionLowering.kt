/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers.*
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.GenerateSequenceStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.LoweringStrategy
import org.jetbrains.kotlin.ir.IrElement
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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.SequenceOfStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.UnknownVariableStrategy
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

internal const val FOR_EACH = "forEach"
internal const val FIND = "find"
internal const val FIND_LAST = "findLast"
internal const val FIRST = "first"
internal const val FIRST_NOT_NULL_OF = "firstNotNullOf"
internal const val FIRST_NOT_NULL_OF_OR_NULL = "firstNotNullOfOrNull"
internal const val FIRST_OR_NULL = "firstOrNull"
internal const val LAST = "last"
internal const val LAST_OR_NULL = "lastOrNull"
internal const val FILTER_TO = "filterTo"
internal const val FILTER_NOT_TO = "filterNotTo"
internal const val FILTER_NOT_NULL_TO = "filterNotNullTo"

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

    internal fun createStrategy(
        builder: IrBuilderWithScope,
    ): LoweringStrategy = when (this) {
        is AsSequence -> UnknownVariableStrategy(this.iterable)
        is GenerateSequence -> GenerateSequenceStrategy(this)
        is SequenceOf -> SequenceOfStrategy(this)
        is Variable -> UnknownVariableStrategy(builder.irGet(this.variable.owner))
    }
}

internal typealias IrBuilderWithParent = Pair<IrBuilderWithScope, IrDeclarationParent>

internal fun isCallFromKotlinSequences(expression: IrCall): Boolean {
    val packageFqName = expression.symbol.owner.getPackageFragment().packageFqName.asString()
    return packageFqName == "kotlin.sequences"
}

internal fun isSequenceTransformer(expression: IrExpression): Boolean {
    return when (expression) {
        is IrCall -> {
            val name = expression.symbol.owner.name.asString()
            when (name) {
                MAP, MAP_INDEXED, MAP_NOT_NULL, MAP_NOT_NULL_INDEXED, FILTER, FILTER_NOT, FILTER_NOT_NULL, TAKE -> true
                else -> false
            }
        }
        else -> false
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

    override fun visitBlock(expression: IrBlock): IrExpression {
        val result = super.visitBlock(expression)
        if (result !is IrBlock) return result

        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val parent = currentScope?.scope?.scopeOwnerSymbol as? IrDeclarationParent ?: currentDeclarationParent ?: return result
        val loopData = gatherLoopData(result, parent) ?: return result
        val receiver =
            ((expression.statements.getOrNull(0) as? IrVariable)?.initializer as? IrCall)?.arguments?.getOrNull(0) ?: return result
        val gatherer = SequenceDataGatherer(context)
        receiver.accept(gatherer, null)
        val sequenceData = receiver.sequenceDataOfExpression ?: return result
        val strategy = sequenceData.sequenceSource.createStrategy(builder)
        val results = strategy.prepareLoopBody(loopData.loopBody, builder to parent, loopData.loopVariable, loopData.loop)
        val preparedBody = results.first
        val newLoop = results.second
        return strategy.lowerLoop(builder to parent, preparedBody, sequenceData, newLoop, loopData.loopVariable) ?: result
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val functionName = expression.symbol.owner.name.asString()
        val visitedExpression = super.visitCall(expression) as? IrCall ?: return expression
        if (!isCallFromKotlinSequences(visitedExpression)) return visitedExpression
        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val parent =
            currentScope?.scope?.scopeOwnerSymbol as? IrDeclarationParent ?: currentDeclarationParent ?: return visitedExpression
        val receiver = expression.arguments.getOrNull(0) ?: return visitedExpression
        if (!isElementSequence(context, receiver)) return visitedExpression
        val gatherer = SequenceDataGatherer(context)
        receiver.accept(gatherer, null)
        val sequenceData = receiver.sequenceDataOfExpression ?: return visitedExpression
        val newExpression =
            createConsumerBodyReplacementCreator(functionName, context, builder, parent, sequenceData)?.create(visitedExpression)
                ?: return visitedExpression
        return if (isSequenceTransformer(receiver)) {
            builder.irBlock {
                irTemporary(receiver.deepCopyWithSymbols(parent))
                +newExpression
            }
        } else newExpression
    }
}
