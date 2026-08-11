/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers

import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.ConsumerBodyBuilder
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceDataGatherer
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceSource
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.addTransformers
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.containsUnlowerableCalls
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.gatherVarargArgumentFromSequenceOf
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.getBaseTypeFromSequenceScopeFunction
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.getGenericTypeFromExpression
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.irReturnTo
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.irReturnUnitTo
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.isFusedVariableCopy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.isSequenceScopeBuilderCall
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.isSequenceScopeYield
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.sequenceDataOfExpression
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irReturnableBlock
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.builders.irUnit
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name

private class YieldReplacer(
    val builderWithParent: IrBuilderWithParent,
    val consumeFunction: IrFunction,
    val returnableBlock: IrReturnableBlock,
    val oldLambdaSymbol: IrSimpleFunctionSymbol,
    val context: JvmBackendContext,
) : IrElementTransformerVoid() {
    // set when a call can not be replaced, e.g. `yieldAll` of an unsupported type or another suspend call; the fusion is then abandoned
    var shouldTerminate = false
    override fun visitCall(expression: IrCall): IrExpression {
        if (shouldTerminate) return super.visitCall(expression)
        val callee = expression.symbol.owner
        val functionName =
            if (expression.isYieldOf(oldLambdaSymbol.owner) && callee.isSequenceScopeYield()) callee.name.asString() else null
        return when (functionName) {
            "yield" -> {
                val yieldArgument = expression.arguments.getOrNull(1) ?: error("yield argument is null")
                yieldReplacement(builderWithParent, yieldArgument, consumeFunction)
            }
            "yieldAll" -> {
                val yieldAllArgument = expression.arguments.getOrNull(1) ?: error("yieldAll argument is null")
                val parameterType = callee.parameters[1].type

                val yieldReplacement = when {
                    parameterType.isSubtypeOfClass(context.irBuiltIns.iterableClass) -> yieldAllIterableReplacement(
                        builderWithParent,
                        yieldAllArgument,
                        consumeFunction
                    )
                    parameterType.isSubtypeOfClass(context.irBuiltIns.iteratorClass) -> yieldAllIteratorReplacement(
                        builderWithParent,
                        yieldAllArgument,
                        consumeFunction
                    )
                    context.symbols.sequence?.let { parameterType.isSubtypeOfClass(it) } == true -> yieldAllSequenceReplacement(
                        builderWithParent,
                        yieldAllArgument,
                        consumeFunction
                    )
                    else -> null
                }
                if (yieldReplacement != null)
                    yieldReplacement
                else {
                    shouldTerminate = true
                    super.visitCall(expression)
                }
            }
            else -> {
                if (expression.symbol.owner.isSuspend) shouldTerminate = true
                super.visitCall(expression)
            }
        }
    }

    private fun yieldReplacement(
        builderWithParent: IrBuilderWithParent,
        argument: IrExpression,
        consumeFunction: IrFunction,
    ): IrExpression {
        val [builder, parent] = builderWithParent
        return with(builder) {
            val consumeCall = irCall(consumeFunction.symbol, type = context.irBuiltIns.booleanType).apply {
                arguments[0] = argument.deepCopyWithSymbols(parent)
            }
            val notConsumed = irNot(consumeCall)
            irIfThen(context.irBuiltIns.unitType, notConsumed, irReturnUnitTo(returnableBlock.symbol))
        }
    }

    private fun yieldAllSequenceReplacement(
        builderWithParent: IrBuilderWithParent,
        sequenceProducer: IrExpression,
        consumeFunction: IrFunction,
    ): IrExpression? {
        val gatherer = SequenceDataGatherer(context)
        sequenceProducer.accept(gatherer, null)
        val sequenceData = sequenceProducer.sequenceDataOfExpression ?: return null
        val producerStrategy = sequenceData.sequenceSource.createProducerStrategy(builderWithParent.first, context)
        return fuseYieldAll(producerStrategy, sequenceData, consumeFunction)
    }

    // In the case of yieldAll(sequence { ... }) and early termination from it,
    // the inner consumer function would have no way to stop the outer sequence,
    // so the stopped flag is used.
    private fun fuseYieldAll(producerStrategy: ProducerStrategy, sequenceData: SequenceData, consumeFunction: IrFunction): IrExpression? {
        with(builderWithParent.first) {
            val stopped = scope.createTemporaryVariable(irFalse(), "yieldAllStopped", isMutable = true)
            val consumerBodyBuilder: ConsumerBodyBuilder = { sequenceElement ->
                irBlock {
                    val consumeCall = irCall(consumeFunction.symbol, type = context.irBuiltIns.booleanType).apply {
                        arguments[0] = irGet(sequenceElement)
                    }
                    val shouldContinue = irTemporary(consumeCall, nameHint = "shouldContinue")
                    +irIfThen(context.irBuiltIns.unitType, irNot(irGet(shouldContinue)), irSet(stopped, irTrue()))
                    +irGet(shouldContinue)
                }
            }
            val consumerReplacement = SequenceReplacement(emptyList(), null, consumerBodyBuilder, irUnit())
            val sequenceReplacement = addTransformers(consumerReplacement, sequenceData, builderWithParent, this@YieldReplacer.context)
            val fusedLoop = producerStrategy.fuseConsumer(builderWithParent, sequenceData, sequenceReplacement) ?: return null
            return irBlock {
                +stopped
                +fusedLoop
                +irIfThen(context.irBuiltIns.unitType, irGet(stopped), irReturnUnitTo(returnableBlock.symbol))
            }.apply { if (sequenceData.isVariableCopy) isFusedVariableCopy = true }
        }
    }

    // yieldAll(iterator)
    // should be changed into
    // while (iterator.hasNext()) {
    //     val shouldContinue = consume(iterator.next())
    //     if (!shouldContinue) return Unit
    // }
    private fun yieldAllIteratorReplacement(
        builderWithParent: IrBuilderWithParent,
        iterator: IrExpression,
        consumeFunction: IrFunction,
    ): IrExpression? = with(builderWithParent.first) {
        val iteratorDeclaration = scope.createTemporaryVariable(iterator, isMutable = false, nameHint = "yieldAllIterator")
        val buildIteratorCall = { functionName: String ->
            builderWithParent.buildCallWithReceiver(
                irGet(iteratorDeclaration),
                iterator.type,
                functionName
            )
        }
        val hasNextCall = buildIteratorCall("hasNext") ?: return null
        val nextCall = buildIteratorCall("next") ?: return null
        val consumeCall = irCall(consumeFunction.symbol, type = context.irBuiltIns.booleanType).apply {
            arguments[0] = nextCall
        }
        return irBlock {
            +iteratorDeclaration
            +createSequenceWhile().apply {
                condition = hasNextCall
                body = irBlock {
                    +irIfThen(
                        context.irBuiltIns.unitType,
                        irNot(consumeCall),
                        irReturnUnitTo(returnableBlock.symbol)
                    )
                }
            }
        }
    }

    private val listLikeFactoryFqNames = setOf(
        "kotlin.collections.CollectionsKt.listOf",
        "kotlin.collections.CollectionsKt.mutableListOf",
        "kotlin.collections.CollectionsKt.arrayListOf",
    )

    private fun replaceKnownListWithSequenceOf(iterable: IrCall): IrExpression? {
        val argument = iterable.arguments.singleOrNull()

        val baseType = getGenericTypeFromExpression(iterable, context) ?: return null
        val [strategy, sequenceData] = if (argument != null) {
            val elements = gatherVarargArgumentFromSequenceOf(argument) ?: return null
            val source = SequenceSource.SequenceOf(elements, baseType)
            SequenceOfStrategy(source, context) to SequenceData(source)
        } else if (iterable.arguments.isEmpty()) {
            val source = SequenceSource.Empty
            EmptySequenceStrategy to SequenceData(source)
        } else return null

        return fuseYieldAll(strategy, sequenceData, consumeFunction)
    }

    private fun yieldAllIterableReplacement(
        builderWithParent: IrBuilderWithParent,
        iterable: IrExpression,
        consumeFunction: IrFunction,
    ): IrExpression? {
        val builder = builderWithParent.first
        if (iterable is IrCall) {
            val fqName = iterable.symbol.owner.fqNameWhenAvailable?.asString()
            if (fqName in listLikeFactoryFqNames) return replaceKnownListWithSequenceOf(iterable)
        }

        val collectionClass = context.irBuiltIns.collectionClass
        val collectionType = collectionClass.starProjectedType
        val isEmptyFunction = collectionClass.owner.functions.single { it.name.asString() == "isEmpty" }

        with(builder) {
            val iterableVariable = scope.createTemporaryVariable(iterable, nameHint = "yieldAllIterable")
            val iteratorCall = builderWithParent.buildCallWithReceiver(irGet(iterableVariable), iterable.type, "iterator") ?: return null
            val loop = yieldAllIteratorReplacement(builderWithParent, iteratorCall, consumeFunction) ?: return null
            // if collection is empty, don't call iterator()
            val isEmptyCollection = irCall(context.irBuiltIns.andandSymbol).apply {
                arguments[0] = irIs(irGet(iterableVariable), collectionType)
                arguments[1] = irCall(isEmptyFunction).apply { arguments[0] = irAs(irGet(iterableVariable), collectionType) }
            }
            return irBlock {
                +iterableVariable
                +irIfThen(context.irBuiltIns.unitType, irNot(isEmptyCollection), loop)
            }
        }
    }
}

private fun getSequenceElementTypeIfLowerable(parent: IrDeclarationParent, sequenceScope: IrRichFunctionReference): IrType? {
    // The consumer is moved into a local non-inline function, which breaks code that is only valid directly
    // in an inline function body, for example, `it is T` where `T` is a reified type parameter of the inline function
    if (parent.isInsideInlineFunction()) return null
    if (containsUnlowerableCalls(sequenceScope.invokeFunction.body!!, allowSequenceYields = true)) return null
    if (sequenceScope.invokeFunction.hasYieldsWhichCanObserveConsumer()) return null
    return getBaseTypeFromSequenceScopeFunction(sequenceScope)
}

// The consumer is moved into a local non-suspend function, so it cannot contain jumps to targets outside it and suspend calls
private fun isConsumerFunctionLowerable(localConsumerFunction: IrFunction): Boolean = !localConsumerFunction.containsJumpsOutside()
        && !containsUnlowerableCalls(localConsumerFunction.body!!, allowSequenceYields = false)

internal class SequenceBuilderStrategy(
    val sequenceScope: IrRichFunctionReference,
    val context: JvmBackendContext,
) : ProducerStrategy() {

    override fun fuseConsumer(
        builderWithParent: IrBuilderWithParent,
        sequenceData: SequenceData,
        sequenceReplacement: SequenceReplacement,
    ): IrContainerExpression? {
        val [builder, parent] = builderWithParent
        val baseType = getSequenceElementTypeIfLowerable(parent, sequenceScope) ?: return null
        // Here we cannot just inline the consumer, because we would have to inline it at every yield.
        // Consumers already assume that their lambda is only used once, so this would break IR.
        // Even if we could inline it, this could result in quadratic code size blowup.
        val localConsumerFunction = buildLocalConsumerFunction(builder, parent, baseType, sequenceReplacement.mainBodyBuilder)
        if (!isConsumerFunctionLowerable(localConsumerFunction)) return null

        val sequenceBuilderBody = builder.irReturnableBlock(context.irBuiltIns.unitType) {
            +sequenceScope.invokeFunction.body!!.deepCopyWithSymbols(parent).statements
        }
        sequenceBuilderBody.statements.add(builder.irReturnUnitTo(sequenceBuilderBody.symbol))
        val sequenceBuilderBodyWithCheck = builder.skipIfNeeded(sequenceReplacement.skipIteration, sequenceBuilderBody)
        val fusedSequenceBlock = builder.irBlock(resultType = sequenceReplacement.finalExpression.type) {
            +sequenceReplacement.initialDeclarations
            +localConsumerFunction
            +sequenceBuilderBodyWithCheck
            +sequenceReplacement.finalExpression
        }
        val oldLambdaSymbol = sequenceScope.invokeFunction.symbol
        val yieldReplacer = YieldReplacer(
            builderWithParent,
            localConsumerFunction,
            sequenceBuilderBody,
            oldLambdaSymbol,
            context,
        )

        fusedSequenceBlock.transformChildren(yieldReplacer, null)
        if (yieldReplacer.shouldTerminate) return null
        // this is checked after the yield replacer, because uses of `this` other than `this.yield/yieldAll` are not fusable
        if (fusedSequenceBlock.usesAnyOf(sequenceScope.invokeFunction.parameters)) return null

        fusedSequenceBlock.retargetReturns(oldLambdaSymbol, sequenceBuilderBody.symbol)
        fusedSequenceBlock.patchDeclarationParents(parent)
        return fusedSequenceBlock
    }

    private fun buildLocalConsumerFunction(
        builder: IrBuilderWithScope,
        parent: IrDeclarationParent,
        elementType: IrType,
        consumerBodyBuilder: ConsumerBodyBuilder,
    ): IrFunction {
        val consumerTarget = context.irFactory.buildFun {
            name = Name.identifier("sequenceYieldConsumer")
            returnType = context.irBuiltIns.booleanType
            visibility = DescriptorVisibilities.LOCAL
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        }.apply {
            this.parent = parent
        }

        val elementParam = consumerTarget.addValueParameter("element", elementType)

        consumerTarget.body = builder.irBlockBody {
            +irReturnTo(consumerTarget.symbol, consumerBodyBuilder(elementParam))
        }
        consumerTarget.body?.patchDeclarationParents(consumerTarget)

        return consumerTarget
    }
}

private fun IrCall.isYieldOf(sequenceLambda: IrFunction): Boolean =
    symbol.owner.isSequenceScopeBuilderCall() &&
            (arguments.getOrNull(0) as? IrGetValue)?.symbol?.owner?.parent == sequenceLambda

// inline functions that call their lambda argument without `try`
private val functionsTransparentForYields = setOf(
    "kotlin.StandardKt.repeat",
    "kotlin.StandardKt.let",
    "kotlin.StandardKt.run",
    "kotlin.StandardKt.with",
    "kotlin.StandardKt.apply",
    "kotlin.StandardKt.also",
    "kotlin.collections.CollectionsKt.forEach",
    "kotlin.collections.CollectionsKt.forEachIndexed",
    "kotlin.collections.ArraysKt.forEach",
    "kotlin.collections.ArraysKt.forEachIndexed",
    "kotlin.collections.MapsKt.forEach",
    "kotlin.sequences.SequencesKt.forEach",
    "kotlin.sequences.SequencesKt.forEachIndexed",
)

// try/catch/finally behavior would change when lowered, best to skip lowering if sequenceBuilder contains 'try'
// or functions which aren't provable to not contain 'try'
private fun IrFunction.hasYieldsWhichCanObserveConsumer(): Boolean {
    val sequenceLambda = this
    var result = false
    var observingDepth = 0
    body?.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            if (!result) element.acceptChildrenVoid(this)
        }

        override fun visitTry(aTry: IrTry) {
            observingDepth++
            super.visitTry(aTry)
            observingDepth--
        }

        override fun visitCall(expression: IrCall) {
            if (expression.isYieldOf(sequenceLambda)) {
                if (observingDepth > 0) result = true
                super.visitCall(expression)
                return
            }
            val isTransparent = expression.symbol.owner.fqNameWhenAvailable?.asString() in functionsTransparentForYields
            if (!isTransparent) observingDepth++
            super.visitCall(expression)
            if (!isTransparent) observingDepth--
        }
    })
    return result
}

private fun IrFunction.containsJumpsOutside(): Boolean {
    var result = false
    val innerTargets = mutableSetOf<IrReturnTargetSymbol>()
    val innerLoops = mutableSetOf<IrLoop>()

    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFunction(declaration: IrFunction) {
            innerTargets += declaration.symbol
            super.visitFunction(declaration)
        }

        override fun visitReturnableBlock(expression: IrReturnableBlock) {
            innerTargets += expression.symbol
            super.visitReturnableBlock(expression)
        }

        override fun visitLoop(loop: IrLoop) {
            innerLoops += loop
            super.visitLoop(loop)
        }

        override fun visitReturn(expression: IrReturn) {
            if (expression.returnTargetSymbol !in innerTargets) result = true
            super.visitReturn(expression)
        }

        override fun visitBreakContinue(jump: IrBreakContinue) {
            if (jump.loop !in innerLoops) result = true
            super.visitBreakContinue(jump)
        }
    })
    return result
}

private fun IrDeclarationParent.isInsideInlineFunction(): Boolean =
    generateSequence(this) { (it as? IrDeclaration)?.parent }.any { it is IrFunction && it.isInline }

private fun IrElement.retargetReturns(from: IrReturnTargetSymbol, to: IrReturnTargetSymbol) {
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitReturn(expression: IrReturn) {
            if (expression.returnTargetSymbol == from) expression.returnTargetSymbol = to
            super.visitReturn(expression)
        }
    })
}

private fun IrElement.usesAnyOf(declarations: List<IrValueDeclaration>): Boolean {
    val symbols = declarations.mapTo(mutableSetOf()) { it.symbol }
    var result = false
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            if (!result) element.acceptChildrenVoid(this)
        }

        override fun visitGetValue(expression: IrGetValue) {
            if (expression.symbol in symbols) result = true
        }
    })
    return result
}
