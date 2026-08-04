/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers.*
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers.TransformerStrategy
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getAllSubstitutedSupertypes
import org.jetbrains.kotlin.ir.util.isFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * transformation:
 * ```
 * fun myFun(seq: Sequence<Int>) {
 *     val seq2 = seq.map { it * 2 }.map { it + 1 }
 *     for (x in seq2) println(x)
 * }
 * ```
 * becomes (see `UnknownVariableStrategy`)
 * ```
 * fun myFun(seq: Sequence<Int>) {
 *     val seq2 = seq.map { it * 2 }.map { it + 1 }
 *     val iterator = seq.iterator()
 *     while (iterator.hasNext()) println({ y -> { x -> x * 2 }(y) + 1 }(iterator.next()))
 * }
 * ```
 *
 * ```
 * val seq = sequenceOf(1, 2, 3).map { it * 2 }.map { it + 1 }
 * for (x in seq) println(x)
 * ```
 * becomes (see `SequenceOfStrategy`)
 * ```
 * val seq = sequenceOf(1, 2, 3).map { it * 2 }.map { it + 1 }
 * val e0 = 1; val e1 = 2; val e2 = 3
 * var n = 0
 * while (n < 3) {
 *     val x = when (n) { 0 -> e0; 1 -> e1; 2 -> e2; else -> throw NoWhenBranchMatchedException() }
 *     println({ y -> { x -> x * 2 }(y) + 1 }(x))
 *     n++
 * }
 * ```
 */

internal typealias ConsumerBodyBuilder = (IrValueDeclaration) -> IrContainerExpression

internal data class SequenceReplacement(
    val initialDeclarations: List<IrStatement>,
    val mainBodyBuilder: ConsumerBodyBuilder,
    val finalExpression: IrExpression,
)

class SequenceFusionLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val reuseMarker = ReusedSequenceMarker(context)
        irFile.acceptChildrenVoid(reuseMarker)
        val transformer = SequenceFusionTransformer(context)
        irFile.transformChildrenVoid(transformer)
    }
}

internal sealed class GenerateSequenceInitialValue {
    // generateSequence(seed: T?, nextFunction: (T) -> T?): Sequence<T>
    class Seed(val expression: IrExpression) : GenerateSequenceInitialValue()

    // generateSequence(seedFunction: () -> T?, nextFunction: (T) -> T?): Sequence<T>
    class SeedFunction(val function: IrExpression) : GenerateSequenceInitialValue()

    // generateSequence(nextFunction: () -> T?): Sequence<T>
    object NextFunction : GenerateSequenceInitialValue()
}

internal typealias IrBuilderWithParent = Pair<IrBuilderWithScope, IrDeclarationParent>

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

internal fun getGenericTypeFromExpression(sequence: IrExpression, context: JvmBackendContext): IrType? {
    val type = sequence.type as? IrSimpleType ?: return null
    val irClass = type.classOrNull?.owner ?: return null
    val elementContainers = listOfNotNull(context.irBuiltIns.iterableClass, context.symbols.sequence)
    val supertype = (getAllSubstitutedSupertypes(irClass) + irClass.defaultType)
        .firstOrNull { it.classOrNull in elementContainers } ?: return null
    val elementType = supertype.arguments.singleOrNull()?.typeOrNull ?: return null
    val typeArguments = type.arguments.map { it.typeOrNull ?: return null }
    return elementType.substitute(irClass.typeParameters, typeArguments)
}

internal fun IrBuilderWithScope.callPredicate(
    predicate: IrExpression,
    parent: IrDeclarationParent,
    vararg args: IrExpression,
): IrExpression {
    return when (predicate) {
        is IrRichFunctionReference -> callRichFunctionReference(predicate, parent, *args)
        else -> {
            val invokeFunction = predicate.type.classOrNull?.owner?.declarations
                ?.filterIsInstance<IrSimpleFunction>()
                ?.firstOrNull { it.name.asString() == "invoke" && it.parameters.size == args.size + 1 }
                ?: error("Didn't find invoke for the predicate: ${predicate.dump()}")
            val returnType = (predicate.type as? IrSimpleType)?.arguments?.lastOrNull()?.typeOrNull ?: invokeFunction.returnType
            irCall(invokeFunction.symbol, returnType).apply {
                dispatchReceiver = predicate.deepCopyWithSymbols(parent)
                args.forEachIndexed { index, arg ->
                    arguments[index + 1] = arg.deepCopyWithSymbols(parent)
                }
            }
        }
    }
}

internal fun IrBuilderWithScope.increment(incrementVariable: IrVariable, context: JvmBackendContext): IrExpression {
    val exception = irCallConstructor(context.symbols.arithmeticExceptionCtorString, emptyList()).apply {
        arguments[0] = irString("Index overflow has happened.")
    }

    val throwExpression = irThrow(exception)

    val intLessThanFunction = context.irBuiltIns.lessFunByOperandType[context.irBuiltIns.intClass] ?: error("No less function for int")
    val isNegative = irCall(intLessThanFunction, context.irBuiltIns.booleanType).apply {
        arguments[0] = irGet(incrementVariable)
        arguments[1] = irInt(0)
    }
    val checkOverflow = irIfThen(
        isNegative,
        throwExpression
    )
    return irBlock {
        +irSet(incrementVariable, irCall(context.irBuiltIns.intPlusSymbol).apply {
            dispatchReceiver = irGet(incrementVariable)
            arguments[1] = irInt(1)
        })
        +checkOverflow
    }
}

internal fun IrBuilderWithScope.irReturnTo(target: IrReturnTargetSymbol, value: IrExpression): IrExpression =
    IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, target, value)

internal fun isSequenceType(context: JvmBackendContext, element: IrElement): Boolean {
    val sequenceSymbol = context.symbols.sequence ?: return false
    val type = when (element) {
        is IrExpression -> element.type
        is IrVariable -> element.type
        else -> return false
    }
    return type.isSubtypeOfClass(sequenceSymbol)
}

// The `next()` call can be wrapped in an IMPLICIT_NOTNULL type-cast when the element type has enhanced nullability from Java
private fun IrExpression.findForLoopNextCall(): IrCall? = when (this) {
    is IrCall -> takeIf { origin == IrStatementOrigin.FOR_LOOP_NEXT }
    is IrTypeOperatorCall -> argument.findForLoopNextCall()
    else -> null
}

internal fun IrExpression.replaceForLoopNextCall(replacement: IrExpression): IrExpression = when (this) {
    is IrCall -> replacement
    is IrTypeOperatorCall -> apply { argument = argument.replaceForLoopNextCall(replacement) }
    else -> error("Unexpected for loop variable initializer: ${dump()}")
}

private fun lookupForLoopVariable(loopBody: IrBlock): IrVariable? = loopBody.statements.filterIsInstance<IrVariable>()
    .singleOrNull { it.initializer?.findForLoopNextCall() != null }

private val sequenceScopeFqName = FqName("kotlin.sequences.SequenceScope")

internal fun IrFunction.isSequenceScopeBuilderCall(): Boolean {
    val receiverType = parameters.getOrNull(0)?.type as? IrSimpleType ?: return false
    val receiverClass = receiverType.classOrNull?.owner ?: return false
    return receiverClass.fqNameWhenAvailable == sequenceScopeFqName
}

internal fun IrFunction.isSequenceScopeYield(): Boolean =
    name.asString().let { it == "yield" || it == "yieldAll" } && (parent as? IrClass)?.fqNameWhenAvailable == sequenceScopeFqName

internal fun containsUnlowerableCalls(
    body: IrBody,
    allowSequenceYields: Boolean
): Boolean {
    var result = false

    body.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall) {
            val callee = expression.symbol.owner
            val isSequenceScopeCall = callee.isSequenceScopeBuilderCall()
            val isAllowedSequenceCall = allowSequenceYields && callee.isSequenceScopeYield()

            if (callee.isSuspend && !isAllowedSequenceCall) {
                result = true
            }

            if (isSequenceScopeCall && !isAllowedSequenceCall) {
                result = true
            }

            super.visitCall(expression)
        }
    })
    return result
}

internal fun getPredicateArgument(expression: IrCall, argument: Int, isEvaluatedOnce: Boolean = false): IrExpression? {
    val predicate = expression.arguments.getOrNull(argument) ?: return null
    // unless it is evaluated once before the iteration (see `evaluateFunctionArgumentOnce`), the predicate is copied to every place
    if (!isEvaluatedOnce && !predicate.isFunctionExpressionSafeToReevaluate()) return null
    if (predicate !is IrRichFunctionReference && !predicate.type.isFunctionOrKFunction()) return null

    val body = predicate.asFunction()?.body
    if (body != null && containsUnlowerableCalls(body, allowSequenceYields = false)) return null
    return predicate
}

internal fun IrExpression.isFunctionExpressionSafeToReevaluate(): Boolean = when (this) {
    is IrRichFunctionReference -> boundValues.all { it.isStableRead() }
    is IrGetValue -> (symbol.owner as? IrVariable)?.isVar != true
    is IrTypeOperatorCall -> operator == IrTypeOperator.IMPLICIT_CAST && argument.isFunctionExpressionSafeToReevaluate()
    else -> false
}

internal fun IrBuilderWithParent.evaluateFunctionArgumentOnce(function: IrExpression): Pair<IrVariable?, IrExpression> {
    if (function.isFunctionExpressionSafeToReevaluate()) return null to function
    val [builder, parent] = this
    val variable = builder.scope.createTemporaryVariable(function.deepCopyWithSymbols(parent), nameHint = "functionArgument")
    return variable to builder.irGet(variable)
}

private fun IrExpression.isStableRead(): Boolean = when (this) {
    is IrConst -> true
    is IrGetValue -> (symbol.owner as? IrVariable)?.isVar != true
    is IrGetField -> symbol.owner.isFinal && receiver?.isStableRead() != false
    is IrTypeOperatorCall -> operator == IrTypeOperator.IMPLICIT_CAST && argument.isStableRead()
    else -> false
}

internal fun IrExpression.asFunction(): IrFunction? =
    when (this) {
        is IrTypeOperatorCall -> argument.asFunction()
        is IrRichFunctionReference -> invokeFunction
        is IrGetValue -> {
            val variable = symbol.owner as? IrVariable
                ?: return null
            if (variable.isVar) return null
            variable.initializer?.asFunction()
        }
        else -> null
    }

internal data class LoopData(
    val loop: IrLoop?,
    val loopVariable: IrVariable,
    val loopBody: IrBlock,
)

internal fun gatherLoopData(block: IrBlock, parent: IrDeclarationParent): LoopData? {
    if (block.origin != IrStatementOrigin.FOR_LOOP) return null

    // extract loop iterator variable and loop body from IrBlock
    if (block.statements.size != 2) return null
    val blockCopy = block.deepCopyWithSymbols(parent)
    val loop = blockCopy.statements[1] as? IrWhileLoop ?: return null
    val body = loop.body as? IrBlock ?: return null
    val loopVariable = lookupForLoopVariable(body) ?: return null
    body.statements.remove(loopVariable)
    return LoopData(loop, loopVariable, body)
}

private class SequenceFusionTransformer(val context: JvmBackendContext) : IrElementTransformerVoidWithContext() {
    override fun visitBlock(expression: IrBlock): IrExpression {
        val visitedExpression = super.visitBlock(expression)
        if (visitedExpression !is IrBlock) return visitedExpression
        if (visitedExpression.origin != IrStatementOrigin.FOR_LOOP) return visitedExpression

        val parent = currentDeclarationParent ?: return visitedExpression
        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val receiver =
            ((expression.statements.getOrNull(0) as? IrVariable)?.initializer as? IrCall)?.arguments?.getOrNull(0)
                ?: return visitedExpression
        val gatherer = SequenceDataGatherer(context)
        receiver.accept(gatherer, null)
        val sequenceData = receiver.sequenceDataOfExpression ?: return visitedExpression
        val data = ConsumerData(context, builder, parent, sequenceData)
        val consumerStrategy = createConsumerStrategy(visitedExpression, data) ?: return visitedExpression
        return fuse(consumerStrategy, sequenceData, builder to parent) ?: visitedExpression
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val visitedExpression = super.visitCall(expression) as? IrCall ?: return expression
        val functionFQName = expression.symbol.owner.fqNameWhenAvailable?.asString() ?: return visitedExpression
        if (!isConsumerFqName(functionFQName)) return visitedExpression
        val parent = currentDeclarationParent ?: return visitedExpression
        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val receiver = expression.arguments.getOrNull(0) ?: return visitedExpression
        val gatherer = SequenceDataGatherer(context)
        receiver.accept(gatherer, null)
        val sequenceData = receiver.sequenceDataOfExpression ?: return visitedExpression
        val data = ConsumerData(context, builder, parent, sequenceData)
        val consumerStrategy =
            createConsumerStrategy(visitedExpression, functionFQName, data, context)
                ?: return visitedExpression
        return fuse(consumerStrategy, sequenceData, builder to parent) ?: visitedExpression
    }

    private fun fuse(
        consumerStrategy: ConsumerStrategy,
        sequenceData: SequenceData,
        builderWithParent: IrBuilderWithParent,
    ): IrExpression? {
        // null if the consumer cannot be fused with its arguments
        val consumerReplacement = consumerStrategy.createSequenceReplacement() ?: return null
        val sequenceReplacement = addTransformers(consumerReplacement, sequenceData, builderWithParent, context)
        val producerStrategy = sequenceData.sequenceSource.createProducerStrategy(builderWithParent.first, context)
        return producerStrategy.fuseConsumer(builderWithParent, sequenceData, sequenceReplacement)
            ?.apply { if (sequenceData.isVariableCopy) isFusedVariableCopy = true }
    }
}

internal fun addTransformers(
    consumerReplacement: SequenceReplacement,
    sequenceData: SequenceData,
    builderWithParent: IrBuilderWithParent,
    context: JvmBackendContext
): SequenceReplacement {
    var sequenceReplacement = consumerReplacement
    // each transformer wraps the body built for the transformers and the consumer after it,
    // so they are applied from the consumer back to the source
    for (transformer in sequenceData.transformers.reversed()) {
        val transformerStrategy = TransformerStrategy.create(transformer, builderWithParent, context)
        sequenceReplacement =
            transformerStrategy.addTransformerToBodyBuilder(sequenceReplacement)
    }
    return sequenceReplacement
}
