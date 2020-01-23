package androidx.compose.plugins.kotlin.compiler.lower

import androidx.compose.plugins.kotlin.COMPOSABLE_EMIT_OR_CALL
import androidx.compose.plugins.kotlin.ComposableCallableDescriptor
import androidx.compose.plugins.kotlin.ComposableEmitDescriptor
import androidx.compose.plugins.kotlin.ComposableEmitMetadata
import androidx.compose.plugins.kotlin.ComposableFunctionDescriptor
import androidx.compose.plugins.kotlin.ComposeFlags
import androidx.compose.plugins.kotlin.ComposeFqNames
import androidx.compose.plugins.kotlin.ComposerMetadata
import androidx.compose.plugins.kotlin.EmitChildrenValueParameterDescriptor
import androidx.compose.plugins.kotlin.KtxNameConventions
import androidx.compose.plugins.kotlin.ValidatedAssignment
import androidx.compose.plugins.kotlin.ValidationType
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices.COMPOSABLE_EMIT_DESCRIPTOR
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices.COMPOSABLE_FUNCTION_DESCRIPTOR
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices.COMPOSABLE_PROPERTY_DESCRIPTOR
import androidx.compose.plugins.kotlin.hasPivotalAnnotation
import androidx.compose.plugins.kotlin.irTrace
import androidx.compose.plugins.kotlin.isMarkedStable
import androidx.compose.plugins.kotlin.isSpecialType
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irReturnUnit
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.builders.setSourceRange
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.getIrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.getValueArgument
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.putTypeArguments
import org.jetbrains.kotlin.ir.expressions.putValueArgument
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.util.typeSubstitutionMap
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class ComposableCallTransformer(
    context: JvmBackendContext,
    symbolRemapper: DeepCopySymbolRemapper
) :
    AbstractComposeLowering(context, symbolRemapper),
    FileLoweringPass,
    ModuleLoweringPass {

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    private val orFunctionDescriptor = builtIns.builtIns.booleanType.memberScope
        .findFirstFunction("or") { it is FunctionDescriptor && it.isInfix }

    private val jvmContext get() = context

    private fun KotlinType?.isStable(): Boolean {
        if (this == null) return false

        val trace = jvmContext.state.bindingTrace
        val calculated = trace.get(ComposeWritableSlices.STABLE_TYPE, this)
        return if (calculated == null) {
            val isStable = !isError &&
                    !isTypeParameter() &&
                    !isSpecialType &&
                    (
                            KotlinBuiltIns.isPrimitiveType(this) ||
                                    isFunctionType ||
                                    isEnum ||
                                    isMarkedStable() ||
                                    (
                                            isNullable() &&
                                                    makeNotNullable().isStable()
                                            )
                            )
            trace.record(ComposeWritableSlices.STABLE_TYPE, this, isStable)
            isStable
        } else calculated
    }

    private val KotlinType.isEnum
        get() =
            (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.ENUM_CLASS

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    val declarationStack = mutableListOf<IrFunction>()

    override fun visitFunction(declaration: IrFunction): IrStatement {
        try {
            declarationStack.push(declaration)
            return super.visitFunction(declaration)
        } finally {
            declarationStack.pop()
        }
    }

    private fun findIrCall(expr: IrStatement): IrCall {
        return when (expr) {
            is IrCall -> expr
            is IrTypeOperatorCall -> when (expr.operator) {
                IrTypeOperator.IMPLICIT_CAST -> findIrCall(expr.argument)
                IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> findIrCall(expr.argument)
                else -> error("Unhandled IrTypeOperatorCall: ${expr.operator}")
            }
            is IrBlock -> when (expr.origin) {
                IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL ->
                    findIrCall(expr.statements.last())
                else -> error("Unhandled IrBlock origin: ${expr.origin}")
            }
            else -> error("Unhandled IrExpression: ${expr::class.java.simpleName}")
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (!ComposeFlags.COMPOSER_PARAM) return super.visitCall(expression)
        if (expression.isTransformedComposableCall()) {
            val descriptor = expression.descriptor
            val returnType = descriptor.returnType
            return if ((returnType == null || returnType.isUnit()) && !descriptor.isInline) {
                val meta = context.state.irTrace[
                        ComposeWritableSlices.COMPOSER_IR_METADATA,
                        expression
                ]
                if (meta == null) error("Couldn't find Composer Metadata")
                context.createIrBuilder(declarationStack.last().symbol).irBlock {
                    +irComposableCall(expression.transformChildren(), meta)
                }
            } else {
                context
                    .createIrBuilder(declarationStack.last().symbol)
                    .irComposableExpr(expression.transformChildren())
            }
        }
        val emitMetadata = context.state.irTrace[
                ComposeWritableSlices.COMPOSABLE_EMIT_METADATA,
                expression
        ]
        if(emitMetadata != null) {
            return context.createIrBuilder(declarationStack.last().symbol).irBlock {
                +irComposableEmit(expression.transformChildren(), emitMetadata)
            }
        }
        return super.visitCall(expression)
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (ComposeFlags.COMPOSER_PARAM) return super.visitBlock(expression)
        if (expression.origin != COMPOSABLE_EMIT_OR_CALL) {
            return super.visitBlock(expression)
        }

        val descriptor = context.state.irTrace[COMPOSABLE_FUNCTION_DESCRIPTOR, expression]

        if (descriptor != null) {
            val (composerCall, emitOrCall) = deconstructComposeBlock(expression)

            val transformedComposerCall = composerCall.transformChildren()
            val transformed = emitOrCall.transformChildren()

            val returnType = descriptor.returnType
            return if ((returnType == null || returnType.isUnit()) && !descriptor.isInline) {
                context.createIrBuilder(declarationStack.last().symbol).irBlock {
                    +irComposableCall(transformedComposerCall, transformed, descriptor)
                }
            } else {
                context
                    .createIrBuilder(declarationStack.last().symbol)
                    .irComposableExpr(transformedComposerCall, transformed, descriptor)
            }
        }

        val emitDescriptor = context.state.irTrace[COMPOSABLE_EMIT_DESCRIPTOR, expression]

        if (emitDescriptor != null) {
            val (composerCall, emitOrCall) = deconstructComposeBlock(expression)
            val transformedComposerCall = composerCall.transformChildren()
            val transformed = emitOrCall.transformChildren()
            return context.createIrBuilder(declarationStack.last().symbol).irBlock {
                +irComposableEmit(transformedComposerCall, transformed, emitDescriptor)
            }
        }

        val property = context.state.irTrace[COMPOSABLE_PROPERTY_DESCRIPTOR, expression]

        if (property != null) {
            val (composerCall, emitOrCall) = deconstructComposeBlock(expression)

            val transformedComposerCall = composerCall.transformChildren()
            val transformed = emitOrCall.transformChildren()

            return context
                .createIrBuilder(declarationStack.last().symbol)
                .irComposableExpr(transformedComposerCall, transformed, property)
        }

        error(
            "Expected ComposableFunctionDescriptor or ComposableEmitDescriptor\n" +
            "Found: $descriptor"
        )
    }

    private fun deconstructComposeBlock(expression: IrBlock): Pair<IrCall, IrCall> {
        assert(expression.statements.size == 2)
        // the first statement should represent the call to get the composer
        // the second statement should represent the composable call or emit
        val (first, second) = expression.statements
        val composerCall = findIrCall(first)
        val emitOrCall = findIrCall(second)
        return composerCall to emitOrCall
    }

    private fun IrExpression.isReorderTemporaryVariable(): Boolean {
        return this is IrGetValue &&
                symbol.owner.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
    }

    private fun IrExpression.unwrapReorderTemporaryVariable(): IrExpression {
        val getValue = this as IrGetValue
        val variable = getValue.symbol.owner as IrVariableImpl
        return variable.initializer!!
    }

    private fun IrBlockBuilder.irComposableCall(
        original: IrCall,
        meta: ComposerMetadata
    ): IrExpression {
        assert(ComposeFlags.COMPOSER_PARAM)
        val composerArg = original.getValueArgument(original.valueArgumentsCount - 1)!!
        // TODO(lmr): we may want to rewrite this in a way that doesn't do a deepCopy...
        val getComposer = { composerArg.deepCopyWithVariables() }
        return irComposableCallBase(
            original,
            getComposer,
            meta
        )
    }

    private fun IrBlockBuilder.irComposableCall(
        composerCall: IrCall,
        original: IrCall,
        descriptor: ComposableFunctionDescriptor
    ): IrExpression {
        assert(!ComposeFlags.COMPOSER_PARAM)
        val composerTemp = irTemporary(composerCall)
        val meta = descriptor.composerMetadata
        return irComposableCallBase(
            original,
            { irGet(composerTemp) },
            meta
        )
    }

    private fun IrBlockBuilder.irComposableCallBase(
        original: IrCall,
        getComposer: () -> IrExpression,
        meta: ComposerMetadata
    ): IrExpression {

        /*

        Foo(text="foo")

        // transforms into

        val attr_text = "foo"
        composer.call(
            key = 123,
            invalid = { changed(attr_text) },
            block = { Foo(attr_text) }
        )
         */
        // TODO(lmr): the way we grab temporaries here feels wrong. We should investigate the right
        // way to do this. Additionally, we are creating temporary vars for variables which is
        // causing larger stack space than needed in our generated code.

        val irGetArguments = original
            .descriptor
            .valueParameters
            .map {
                val arg = original.getValueArgument(it)
                it to getParameterExpression(it, arg, unwrapTemp = !ComposeFlags.COMPOSER_PARAM)
            }

        val tmpDispatchReceiver = original.dispatchReceiver?.let { irTemporary(it) }
        val tmpExtensionReceiver = original.extensionReceiver?.let { irTemporary(it) }

        // TODO(lmr): when we move to function body skipping, we can remove the need for this
        //  entirely which means we will no longer need the ComposerMetadata concept.
        val callDescriptor = meta
            .callDescriptors
            .first { it.typeParametersCount == 0 }

        val joinKeyDescriptor = composerTypeDescriptor
            .unsubstitutedMemberScope
            .findFirstFunction(KtxNameConventions.JOINKEY.identifier) {
                it.valueParameters.size == 2
            }

        val callParameters = callDescriptor.valueParameters
            .map { it.name to it }
            .toMap()

        fun getCallParameter(name: Name) = callParameters[name]
            ?: error("Expected $name parameter to exist")

        return irCall(
            callee = referenceFunction(callDescriptor),
            type = builtIns.unitType // TODO(lmr): refactor call(...) to return a type
        ).apply {
            dispatchReceiver = getComposer()

            putValueArgument(
                getCallParameter(KtxNameConventions.CALL_KEY_PARAMETER),
                irGroupKey(
                    original = original,
                    getComposer = getComposer,
                    joinKey = joinKeyDescriptor,
                    pivotals = irGetArguments.mapNotNull { (param, getExpr) ->
                        if (!param.hasPivotalAnnotation()) null
                        else getExpr()
                    }
                )
            )

            val invalidParameter = getCallParameter(KtxNameConventions.CALL_INVALID_PARAMETER)

            val validatorType = invalidParameter.type.getReceiverTypeFromFunctionType()
                ?: error("Expected validator type to be on receiver of the invalid lambda")

            val changedDescriptor = validatorType
                .memberScope
                .findFirstFunction("changed") { it.typeParametersCount == 1 }

            val validatedArguments: List<IrExpression> =
                irGetArguments
                    .filter { (desc, _) -> !desc.isComposerParam() }
                    .mapNotNull { (_, getExpr) -> getExpr() } +
                        listOfNotNull(
                            tmpDispatchReceiver?.let { irGet(it) },
                            tmpExtensionReceiver?.let { irGet(it) }
                        )

            val isSkippable = validatedArguments.all { it.type.toKotlinType().isStable() }

            putValueArgument(
                invalidParameter,
                irLambdaExpression(
                    original.startOffset,
                    original.endOffset,
                    descriptor = createFunctionDescriptor(
                        type = invalidParameter.type,
                        owner = descriptor.containingDeclaration
                    ),
                    type = invalidParameter.type.toIrType()
                ) { fn ->
                    if (!isSkippable) {
                        // if it's not skippable, we don't validate any arguments.
                        +irReturn(irTrue())
                    } else {
                        val validationCalls = validatedArguments
                            .map {
                                irChangedCall(
                                    changedDescriptor = changedDescriptor,
                                    receiver = irGet(fn.extensionReceiverParameter!!),
                                    attributeValue = it
                                )
                            }

                        // all as one expression: a or b or c ... or z
                        +irReturn(when (validationCalls.size) {
                            0 -> irFalse()
                            1 -> validationCalls.single()
                            else -> validationCalls.reduce { accumulator, value ->
                                when {
                                    // if it is a constant, the value is `false`
                                    accumulator is IrConst<*> -> value
                                    value is IrConst<*> -> accumulator
                                    else -> irOr(accumulator, value)
                                }
                            }
                        })
                    }
                }
            )

            val blockParameter = getCallParameter(KtxNameConventions.CALL_BLOCK_PARAMETER)

            putValueArgument(
                blockParameter,
                irLambdaExpression(
                    original.startOffset,
                    original.endOffset,
                    descriptor = createFunctionDescriptor(
                        type = blockParameter.type,
                        owner = descriptor.containingDeclaration
                    ),
                    type = blockParameter.type.toIrType()
                ) {
                    +irCall(
                        callee = IrSimpleFunctionSymbolImpl(original.descriptor).also {
                            it.bind(original.symbol.owner as IrSimpleFunction)
                        },
                        type = original.type
                    ).apply {
                        copyTypeArgumentsFrom(original)

                        dispatchReceiver = tmpDispatchReceiver?.let { irGet(it) }
                        extensionReceiver = tmpExtensionReceiver?.let { irGet(it) }

                        irGetArguments.forEach { (param, getExpr) ->
                            putValueArgument(param, getExpr())
                        }
                    }
                }
            )
        }
    }

    private fun DeclarationIrBuilder.irComposableExpr(
        original: IrCall
    ): IrExpression {
        assert(ComposeFlags.COMPOSER_PARAM)
        return irBlock(resultType = original.descriptor.returnType?.toIrType()) {
            val composerParam = nearestComposer()
            val getComposer = { irGet(composerParam) }
            irComposableExprBase(
                original,
                getComposer
            )
        }
    }

    private fun DeclarationIrBuilder.irComposableExpr(
        composerCall: IrCall,
        original: IrCall,
        descriptor: ComposableCallableDescriptor
    ): IrExpression {
        assert(!ComposeFlags.COMPOSER_PARAM)
        return irBlock(resultType = descriptor.returnType?.toIrType()) {
            val composerTemp = irTemporary(composerCall)
            irComposableExprBase(
                original,
                { irGet(composerTemp) }
            )
        }
    }

    private fun IrBlockBuilder.irComposableExprBase(
        original: IrCall,
        getComposer: () -> IrExpression
    ) {
        /*

        Foo(text="foo")

        // transforms into

        composer.startExpr(123)
        val result = Foo(text="foo")
        composer.endExpr()
        result
         */

        // TODO(lmr): the way we grab temporaries here feels wrong. We should investigate the right
        // way to do this. Additionally, we are creating temporary vars for variables which is
        // causing larger stack space than needed in our generated code.

        // for composableExpr, we only need to create temporaries if there are any pivotals
        val hasPivotals = original
            .descriptor
            .valueParameters
            .any { it.hasPivotalAnnotation() }

        // if we don't have any pivotal parameters, we don't use the parameters more than once,
        // so we can just use the original call itself. This only works with the new composer param
        // code gen though since the old codegen produced IrBlocks around the calls that break this
        val shouldCreateNewCall = hasPivotals || !ComposeFlags.COMPOSER_PARAM

        val irGetArguments = original
            .descriptor
            .valueParameters
            .map {
                val arg = original.getValueArgument(it)
                val expr = if (shouldCreateNewCall)
                    // if the composer param flag is turned off, we end up removing all of the
                    // temporaries created by reordering and creating new ones. if the flag is
                    // turned on, then we end up using the temporaries that were already created,
                    // so we don't want to unwrap them
                    getParameterExpression(it, arg, unwrapTemp = !ComposeFlags.COMPOSER_PARAM)
                else
                    ({ arg })
                it to expr
            }

        val startExpr = composerTypeDescriptor
            .unsubstitutedMemberScope
            .findFirstFunction(KtxNameConventions.START_EXPR.identifier) {
                it.valueParameters.size == 1
            }

        val endExpr = composerTypeDescriptor
            .unsubstitutedMemberScope
            .findFirstFunction(KtxNameConventions.END_EXPR.identifier) {
                it.valueParameters.size == 0
            }

        val joinKeyDescriptor = composerTypeDescriptor
            .unsubstitutedMemberScope
            .findFirstFunction(KtxNameConventions.JOINKEY.identifier) {
                it.valueParameters.size == 2
            }

        val startCall = irCall(
            callee = referenceFunction(startExpr),
            type = builtIns.unitType
        ).apply {
            dispatchReceiver = getComposer()
            putValueArgument(
                startExpr.valueParameters.first(),
                irGroupKey(
                    original = original,
                    getComposer = getComposer,
                    joinKey = joinKeyDescriptor,
                    pivotals = irGetArguments.mapNotNull { (param, getExpr) ->
                        if (!param.hasPivotalAnnotation()) null
                        else getExpr()
                    }
                )
            )
        }

        val newCall = if (shouldCreateNewCall) irCall(
            callee = IrSimpleFunctionSymbolImpl(original.descriptor).also {
                it.bind(original.symbol.owner as IrSimpleFunction)
            },
            type = original.type
        ).apply {
            copyTypeArgumentsFrom(original)

            dispatchReceiver = original.dispatchReceiver
            extensionReceiver = original.extensionReceiver

            irGetArguments.forEach { (param, getExpr) ->
                putValueArgument(param, getExpr())
            }
        } else original

        val endCall = irCall(
            callee = referenceFunction(endExpr),
            type = builtIns.unitType
        ).apply {
            dispatchReceiver = getComposer()
        }

        val hasResult = !original.type.isUnit()

        if (hasResult) {
            +startCall
            val tmpResult = irTemporary(newCall, irType = original.type)
            +endCall
            +irGet(tmpResult)
        } else {
            +startCall
            +newCall
            +endCall
        }
    }

    private fun isChildrenParameter(desc: ValueParameterDescriptor, expr: IrExpression): Boolean {
        return expr is IrFunctionExpression &&
                expr.origin == IrStatementOrigin.LAMBDA &&
                desc is EmitChildrenValueParameterDescriptor
    }

    private fun IrBlockBuilder.getParameterExpression(
        desc: ValueParameterDescriptor,
        expr: IrExpression?,
        unwrapTemp: Boolean = true
    ): () -> IrExpression? {
        if (expr == null)
            return { null }
        if (unwrapTemp && expr.isReorderTemporaryVariable()) {
            return getParameterExpression(desc, expr.unwrapReorderTemporaryVariable())
        }
        return when {
            expr is IrConst<*> ->
                ({ expr.copy() })
            isChildrenParameter(desc, expr) ->
                ({ expr })
            else -> {
                val temp = irTemporary(
                    covertLambdaIfNecessary(expr),
                    irType = expr.type
                )
                ({ irGet(temp) })
            }
        }
    }

    private fun nearestComposer(): IrValueParameter {
        for (fn in declarationStack.asReversed().asSequence()) {
            val param = fn.valueParameters.lastOrNull()
            if (param != null && param.isComposerParam()) {
                return param
            }
        }
        error("Couldn't find composer parameter")
    }

    private fun IrBlockBuilder.irComposableEmit(
        original: IrCall,
        emitMetadata: ComposableEmitMetadata
    ): IrExpression {
        assert(ComposeFlags.COMPOSER_PARAM)
        val composerParam = nearestComposer()
        return irComposableEmitBase(
            original,
            { irGet(composerParam) },
            emitMetadata
        )
    }

    private fun IrBlockBuilder.irComposableEmit(
        composerCall: IrCall,
        original: IrCall,
        descriptor: ComposableEmitDescriptor
    ): IrExpression {
        assert(!ComposeFlags.COMPOSER_PARAM)
        val composerTemp = irTemporary(composerCall)
        return irComposableEmitBase(
            original,
            { irGet(composerTemp) },
            descriptor
        )
    }

    private fun IrBlockBuilder.irComposableEmitBase(
        original: IrCall,
        getComposer: () -> IrExpression,
        emitMetadata: ComposableEmitMetadata
    ): IrExpression {
        /*

        TextView(text="foo")

        // transforms into

        val attr_text = "foo"
        composer.emit(
            key = 123,
            ctor = { context -> TextView(context) },
            update = { set(attr_text) { text -> this.text = text } }
        )
         */
        val parametersByName = original
            .descriptor
            .valueParameters
            .mapNotNull { desc ->
                original.getValueArgument(desc)?.let { desc to it }
            }
            .map { (desc, expr) ->
                desc.name.asString() to getParameterExpression(
                    desc,
                    expr,
                    unwrapTemp = !ComposeFlags.COMPOSER_PARAM
                )
            }
            .toMap()

        val emitCall = emitMetadata.emitCall
        val emitFunctionDescriptor = emitCall.candidateDescriptor

        val emitParameters = emitFunctionDescriptor.valueParameters
            .map { it.name to it }
            .toMap()

        fun getEmitParameter(name: Name) = emitParameters[name]
            ?: error("Expected $name parameter to exist")

        val emitFunctionSymbol = referenceFunction(emitFunctionDescriptor)

        val joinKeyDescriptor = composerTypeDescriptor
            .unsubstitutedMemberScope
            .findFirstFunction(KtxNameConventions.JOINKEY.identifier) {
                it.valueParameters.size == 2
            }

        fun irGetParameter(name: String): IrExpression = parametersByName[name]?.invoke()
            ?: error("No parameter found with name $name")

        return irCall(
            callee = emitFunctionSymbol,
            type = builtIns.unitType
        ).apply {
            dispatchReceiver = getComposer()
            // TODO(lmr): extensionReceiver.
            // We would want to do this to enable "emit" and "call" implementations that are
            // extensions on the composer

            putTypeArguments(emitCall.typeArguments) { it.toIrType() }

            putValueArgument(
                getEmitParameter(KtxNameConventions.EMIT_KEY_PARAMETER),
                irGroupKey(
                    original = original,
                    getComposer = getComposer,
                    joinKey = joinKeyDescriptor,
                    pivotals = emitMetadata.pivotals.map { irGetParameter(it) }
                )
            )

            val ctorParam = getEmitParameter(KtxNameConventions.EMIT_CTOR_PARAMETER)

            val ctorLambdaDescriptor = createFunctionDescriptor(ctorParam.type)

            putValueArgument(
                ctorParam,
                irLambdaExpression(
                    original.startOffset,
                    original.endOffset,
                    descriptor = ctorLambdaDescriptor,
                    type = ctorParam.type.toIrType()
                ) { fn ->

                    val ctorCall = emitMetadata.ctorCall

                    val ctorCallSymbol = referenceConstructor(
                        ctorCall.candidateDescriptor as ClassConstructorDescriptor
                    )

                    +irReturn(irCall(ctorCallSymbol).apply {
                        putTypeArguments(ctorCall.typeArguments) { it.toIrType() }
                        ctorLambdaDescriptor.valueParameters.zip(
                            ctorCall
                                .candidateDescriptor!!
                                .valueParameters
                        ) { a, b ->
                            putValueArgument(
                                b,
                                irGet(fn.getIrValueParameter(a))
                            )
                        }
                        emitMetadata.ctorParams.forEach { name ->
                            val param = ctorCall
                                .candidateDescriptor
                                .valueParameters
                                .firstOrNull { it.name.identifier == name }
                            if (param != null) {
                                putValueArgument(
                                    param,
                                    irGetParameter(name)
                                )
                            }
                        }
                    })
                }
            )

            val updateParam = getEmitParameter(
                KtxNameConventions
                    .EMIT_UPDATER_PARAMETER
            )

            val updateLambdaDescriptor = createFunctionDescriptor(updateParam.type)

            putValueArgument(
                updateParam,
                irLambdaExpression(
                    original.startOffset,
                    original.endOffset,
                    descriptor = updateLambdaDescriptor,
                    type = updateParam.type.toIrType()
                ) { fn ->
                    emitMetadata.validations.forEach {
                        // set(attr_text) { text -> this.text = text }
                        val arg = irGetParameter(it.name)
                        +irValidatedAssignment(
                            arg.startOffset,
                            arg.endOffset,
                            memoizing = true,
                            validation = it,
                            receiver = irGet(fn.extensionReceiverParameter!!),
                            attributeValue = arg
                        )
                    }
                    +irReturnUnit()
                }
            )

            if (emitMetadata.hasChildren) {
                val bodyParam = getEmitParameter(KtxNameConventions.EMIT_CHILDREN_PARAMETER)

                val childrenExpr = irGetParameter("\$CHILDREN")

                putValueArgument(
                    bodyParam,
                    childrenExpr
                )
            }
        }
    }

    private fun IrBuilderWithScope.irGroupKey(
        original: IrCall,
        joinKey: FunctionDescriptor,
        getComposer: () -> IrExpression,
        pivotals: List<IrExpression>
    ): IrExpression {
        val keyValueExpression = irInt(original.sourceLocationHash())
        return if (pivotals.isEmpty()) keyValueExpression
        else (listOf(keyValueExpression) + pivotals).reduce { accumulator, value ->
            irCall(
                callee = referenceFunction(joinKey),
                type = joinKey.returnType!!.toIrType()
            ).apply {
                dispatchReceiver = getComposer()
                putValueArgument(0, accumulator)
                putValueArgument(1, value)
            }
        }
    }

    private fun IrCall.sourceLocationHash(): Int {
        return descriptor.fqNameSafe.toString().hashCode() xor startOffset
    }

    private fun IrBuilderWithScope.irChangedCall(
        changedDescriptor: FunctionDescriptor,
        receiver: IrExpression,
        attributeValue: IrExpression
    ): IrExpression {
        // TODO(lmr): make it so we can use the "changed" overloads with primitive types.
        // Right now this is causing a lot of boxing/unboxing for primitives
        return if (attributeValue is IrConst<*>) irFalse()
        else irCall(
            callee = referenceFunction(changedDescriptor),
            type = changedDescriptor.returnType?.toIrType()!!
        ).apply {
            putTypeArgument(0, attributeValue.type)
            dispatchReceiver = receiver
            putValueArgument(0, attributeValue)
        }
    }

    private fun IrBuilderWithScope.irOr(
        left: IrExpression,
        right: IrExpression
    ): IrExpression {
        return irCall(
            callee = referenceFunction(orFunctionDescriptor),
            type = builtIns.booleanType
        ).apply {
            dispatchReceiver = left
            putValueArgument(0, right)
        }
    }

    private fun IrBuilderWithScope.irValidatedAssignment(
        startOffset: Int,
        endOffset: Int,
        memoizing: Boolean,
        validation: ValidatedAssignment,
        receiver: IrExpression,
        attributeValue: IrExpression
    ): IrExpression {
        // for emit, fnDescriptor is Validator.(Value) -> Unit    or Validator.(Value, Element.(Value) -> Unit) -> Unit
        // for call, fnDescriptor is Validator.(Value) -> Boolean or Validator.(Value, (Value) -> Unit) -> Boolean

        // in emit, the element is passed through an extension parameter
        // in call, the element is passed through a capture scope
        val validationCall =
            if (memoizing) validation.validationCall
            else validation.uncheckedValidationCall

        if (validationCall == null) error("Expected validationCall to be non-null")

        val validationCallDescriptor = validationCall.candidateDescriptor as FunctionDescriptor

        return irCall(
            callee = referenceFunction(validationCallDescriptor),
            type = validationCallDescriptor.returnType?.toIrType()!!
        ).apply {

            dispatchReceiver = receiver
            // TODO(lmr): extensionReceiver.
            // This might be something we want to be able to do in the cases where we want to
            // build extension `changed(...)`  or `set(..) { ... }` methods

            putTypeArguments(validationCall.typeArguments) { it.toIrType() }

            putValueArgument(0, attributeValue)
            val assignment = validation.assignment
            if (assignment != null && validation.validationType != ValidationType.CHANGED) {
                val assignmentLambdaDescriptor = validation.assignmentLambda
                    ?: error("Expected assignmentLambda to be non-null")
                val assignmentDescriptor = assignment.candidateDescriptor.original

                val assignmentSymbol = when (assignmentDescriptor) {
                    is PropertyDescriptor -> referenceFunction(
                        assignmentDescriptor.setter!!
                    )
                    else -> referenceFunction(assignmentDescriptor)
                }
                val assignmentValueParameterDescriptor = assignmentLambdaDescriptor
                    .valueParameters[0]

                putValueArgument(
                    1,
                    irLambdaExpression(
                        startOffset,
                        endOffset,
                        assignmentLambdaDescriptor,
                        validationCallDescriptor.valueParameters[1].type.toIrType()
                    ) { fn ->
                        +irReturn(
                            irCall(
                                callee = assignmentSymbol,
                                type = builtIns.unitType
                            ).apply {
                                putTypeArguments(assignment.typeArguments) { it.toIrType() }
                                when (assignment.explicitReceiverKind) {
                                    ExplicitReceiverKind.DISPATCH_RECEIVER -> {
                                        dispatchReceiver = irGet(fn.extensionReceiverParameter!!)
                                    }
                                    ExplicitReceiverKind.EXTENSION_RECEIVER -> {
                                        extensionReceiver = irGet(fn.extensionReceiverParameter!!)
                                    }
                                    ExplicitReceiverKind.BOTH_RECEIVERS -> {
                                        // NOTE(lmr): This should not be possible. This would have
                                        // to be an extension method on the ComposerUpdater class
                                        // itself for the emittable type.
                                        error(
                                            "Extension instance methods are not allowed for " +
                                                    "assignments"
                                        )
                                    }
                                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> {
                                        // NOTE(lmr): This is not possible
                                        error("Static methods are invalid for assignments")
                                    }
                                }
                                putValueArgument(
                                    0,
                                    irGet(
                                        fn.getIrValueParameter(assignmentValueParameterDescriptor)
                                    )
                                )
                            }
                        )
                    }
                )
            }
        }
    }

    private fun IrBuilderWithScope.irLambdaExpression(
        startOffset: Int,
        endOffset: Int,
        descriptor: FunctionDescriptor,
        type: IrType,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrExpression {
        val symbol = IrSimpleFunctionSymbolImpl(descriptor)

        val returnType = descriptor.returnType!!.toIrType()

        val lambda = IrFunctionImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
            symbol,
            returnType
        ).also {
            it.parent = scope.getLocalDeclarationParent()
            it.createParameterDeclarations()
            it.body = jvmContext
                .createIrBuilder(symbol)
                .irBlockBody { body(it) }
        }

        return irBlock(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrStatementOrigin.LAMBDA,
            resultType = type
        ) {
            +lambda
            +IrFunctionReferenceImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = type,
                symbol = symbol,
                descriptor = descriptor,
                typeArgumentsCount = descriptor.typeParametersCount,
                origin = IrStatementOrigin.LAMBDA
            )
        }
    }

    private fun IrFunction.createParameterDeclarations() {
        fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            this.startOffset ?: UNDEFINED_OFFSET,
            this.endOffset ?: UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            this,
            type.toIrType(),
            (this as? ValueParameterDescriptor)?.varargElementType?.toIrType()
        ).also {
            it.parent = this@createParameterDeclarations
        }

        fun TypeParameterDescriptor.irTypeParameter() = IrTypeParameterImpl(
            this.startOffset ?: UNDEFINED_OFFSET,
            this.endOffset ?: UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrTypeParameterSymbolImpl(this)
        ).also {
            it.parent = this@createParameterDeclarations
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        assert(valueParameters.isEmpty())
        descriptor.valueParameters.mapTo(valueParameters) { it.irValueParameter() }

        assert(typeParameters.isEmpty())
        descriptor.typeParameters.mapTo(typeParameters) { it.irTypeParameter() }
    }

    /**
     * Convert a function-reference into a inner class constructor call.
     *
     * This is a transformed copy of the work done in CallableReferenceLowering to allow the
     * [ComposeObservePatcher] access to the this parameter.
     */
    private fun IrBlockBuilder.covertLambdaIfNecessary(expression: IrExpression): IrExpression {
        val functionExpression = expression as? IrFunctionExpression ?: return expression

        val function = functionExpression.function

        if (!function.isComposable()) return expression

        // A temporary node created so the code matches more closely to the
        // CallableReferenceLowering code that was copied.
        val functionReference = IrFunctionReferenceImpl(
            -1,
            -1,
            expression.type,
            function.symbol,
            function.descriptor,
            0,
            expression.origin
        )

        val context = this@ComposableCallTransformer.context
        val superType = context.ir.symbols.lambdaClass.typeWith()
        val parameterTypes = (functionExpression.type as IrSimpleType).arguments.map {
            (it as IrTypeProjection).type
        }
        val functionSuperClass = context.ir.symbols.getJvmFunctionClass(
            parameterTypes.size - 1
        )
        val jvmClass = functionSuperClass.typeWith(parameterTypes)
        val boundReceiver = functionReference.getArgumentsWithIr().singleOrNull()
        val typeArgumentsMap = functionReference.typeSubstitutionMap
        val callee = functionReference.symbol.owner
        var constructor: IrConstructor? = null
        val irClass = buildClass {
            setSourceRange(functionReference)
            visibility = Visibilities.LOCAL
            origin = JvmLoweredDeclarationOrigin.LAMBDA_IMPL
            name = Name.special("<function reference to ${callee.fqNameWhenAvailable}>")
        }.apply {
            parent = scope.getLocalDeclarationParent()
            superTypes += superType
            superTypes += jvmClass
            createImplicitParameterDeclarationWithWrappedDescriptor()
        }.also { irClass ->
            // Add constructor
            val superConstructor = superType.getClass()!!.constructors.single {
                it.valueParameters.size == if (boundReceiver != null) 2 else 1
            }
            constructor = irClass.addConstructor {
                setSourceRange(functionReference)
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                returnType = irClass.defaultType
                isPrimary = true
            }.apply {
                boundReceiver?.first?.let { param ->
                    valueParameters += param.copyTo(
                        irFunction = this,
                        index = 0,
                        type = param.type.substitute(typeArgumentsMap)
                    )
                }
                body = context.createJvmIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                    +irDelegatingConstructorCall(superConstructor).apply {
                        putValueArgument(0, irInt(parameterTypes.size - 1))
                        if (boundReceiver != null)
                            putValueArgument(1, irGet(valueParameters.first()))
                    }
                    +IrInstanceInitializerCallImpl(
                        startOffset,
                        endOffset,
                        irClass.symbol,
                        context.irBuiltIns.unitType
                    )
                }
            }

            // Add the invoke method
            val superMethod = functionSuperClass.functions.single {
                it.owner.modality == Modality.ABSTRACT
            }
            irClass.addFunction {
                name = superMethod.owner.name
                returnType = callee.returnType
                isSuspend = callee.isSuspend
            }.apply {
                overriddenSymbols += superMethod
                dispatchReceiverParameter = parentAsClass.thisReceiver!!.copyTo(this)
                annotations += callee.annotations
                if (annotations.findAnnotation(ComposeFqNames.Composable) == null) {
                    expression.type.annotations.findAnnotation(ComposeFqNames.Composable)?.let {
                        annotations += it
                    }
                }
                val bindingContext = context.state.bindingContext
                bindingContext.get(
                    ComposeWritableSlices.RESTART_COMPOSER,
                    function.descriptor as SimpleFunctionDescriptor
                )?.let {
                    val trace = context.state.bindingTrace
                    trace.record(
                        ComposeWritableSlices.RESTART_COMPOSER,
                        descriptor as SimpleFunctionDescriptor,
                        it
                    )
                }
                val valueParameterMap =
                    callee.explicitParameters.withIndex().associate { (index, param) ->
                        param to param.copyTo(this, index = index)
                    }
                valueParameters += valueParameterMap.values
                body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                    callee.body?.statements?.forEach { statement ->
                        +statement.transform(object : IrElementTransformerVoid() {
                            override fun visitGetValue(expression: IrGetValue): IrExpression {
                                val replacement = valueParameterMap[expression.symbol.owner]
                                    ?: return super.visitGetValue(expression)

                                at(expression.startOffset, expression.endOffset)
                                return irGet(replacement)
                            }

                            override fun visitReturn(expression: IrReturn): IrExpression =
                                if (expression.returnTargetSymbol != callee.symbol) {
                                    super.visitReturn(expression)
                                } else {
                                    at(expression.startOffset, expression.endOffset)
                                    irReturn(expression.value.transform(this, null))
                                }

                            override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                                if (declaration.parent == callee)
                                    declaration.parent = this@apply
                                return super.visitDeclaration(declaration)
                            }
                        }, null)
                    }
                }
            }
        }

        return irBlock {
            +irClass
            +irCall(constructor!!.symbol).apply {
                if (valueArgumentsCount > 0) putValueArgument(0, boundReceiver!!.second)
            }
        }
    }

    private fun IrBuilderWithScope.createFunctionDescriptor(
        type: KotlinType,
        owner: DeclarationDescriptor = scope.scopeOwner
    ): FunctionDescriptor {
        return AnonymousFunctionDescriptor(
            owner,
            Annotations.EMPTY,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE,
            false
        ).apply {
            initialize(
                type.getReceiverTypeFromFunctionType()?.let {
                    DescriptorFactory.createExtensionReceiverParameterForCallable(
                        this,
                        it,
                        Annotations.EMPTY
                    )
                },
                null,
                emptyList(),
                type.getValueParameterTypesFromFunctionType().mapIndexed { i, t ->
                    ValueParameterDescriptorImpl(
                        containingDeclaration = this,
                        original = null,
                        index = i,
                        annotations = Annotations.EMPTY,
                        name = t.type.extractParameterNameFromFunctionTypeArgument()
                            ?: Name.identifier("p$i"),
                        outType = t.type,
                        declaresDefaultValue = false,
                        isCrossinline = false,
                        isNoinline = false,
                        varargElementType = null,
                        source = SourceElement.NO_SOURCE
                    )
                },
                type.getReturnTypeFromFunctionType(),
                Modality.FINAL,
                Visibilities.LOCAL,
                null
            )
            isOperator = false
            isInfix = false
            isExternal = false
            isInline = false
            isTailrec = false
            isSuspend = false
            isExpect = false
            isActual = false
        }
    }
}
