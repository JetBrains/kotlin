package androidx.compose.plugins.kotlin.compiler.lower

import androidx.compose.plugins.kotlin.ComposableEmitMetadata
import androidx.compose.plugins.kotlin.ComposeFqNames
import androidx.compose.plugins.kotlin.EmitChildrenValueParameterDescriptor
import androidx.compose.plugins.kotlin.KtxNameConventions
import androidx.compose.plugins.kotlin.ValidatedAssignment
import androidx.compose.plugins.kotlin.ValidationType
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.plugins.kotlin.hasPivotalAnnotation
import androidx.compose.plugins.kotlin.irTrace
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
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
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.getIrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.getValueArgument
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.putTypeArguments
import org.jetbrains.kotlin.ir.expressions.putValueArgument
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.util.typeSubstitutionMap
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isUnit

class ComposableCallTransformer(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace
) :
    AbstractComposeLowering(context, symbolRemapper, bindingTrace),
    FileLoweringPass,
    ModuleLoweringPass {

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    private val orFunctionDescriptor = builtIns.builtIns.booleanType.memberScope
        .findFirstFunction("or") { it is FunctionDescriptor && it.isInfix }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    private val declarationStack = mutableListOf<IrSymbolOwner>()

    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
        if (declaration !is IrSymbolOwner) return super.visitDeclaration(declaration)
        try {
            declarationStack.push(declaration)
            return super.visitDeclaration(declaration)
        } finally {
            declarationStack.pop()
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.isTransformedComposableCall() || expression.isSyntheticComposableCall()) {
            val descriptor = expression.symbol.descriptor
            val returnType = descriptor.returnType
            val isUnit = returnType == null || returnType.isUnit() || expression.type.isUnit()
            val isInline = descriptor.isInline || context.irTrace[
                    ComposeWritableSlices.IS_INLINE_COMPOSABLE_CALL,
                    expression
            ] == true
            return if (isUnit && !isInline) {
                DeclarationIrBuilder(context, declarationStack.last().symbol).irBlock {
                    +irComposableCall(expression.transformChildren())
                }
            } else {
                val call = if (isInline)
                    expression.transformChildrenWithoutConvertingLambdas()
                else
                    expression.transformChildren()
                DeclarationIrBuilder(context, declarationStack.last().symbol)
                    .irComposableExpr(call)
            }
        }

        val emitMetadata = context.irTrace[
                ComposeWritableSlices.COMPOSABLE_EMIT_METADATA,
                expression
        ]
        if (emitMetadata != null) {
            return DeclarationIrBuilder(context, declarationStack.last().symbol).irBlock {
                +irComposableEmit(expression.transformChildren(), emitMetadata)
            }
        }
        return super.visitCall(expression)
    }

    private fun IrCall.transformChildrenWithoutConvertingLambdas(): IrCall {
        dispatchReceiver = dispatchReceiver?.transform(this@ComposableCallTransformer, null)
        extensionReceiver = extensionReceiver?.transform(this@ComposableCallTransformer, null)
        for (i in 0 until valueArgumentsCount) {
            val arg = getValueArgument(i) ?: continue
            if (arg is IrFunctionExpression) {
                // we convert function expressions into their lowered lambda equivalents, but we
                // want to avoid doing this for inlined lambda calls.
                putValueArgument(i, super.visitFunctionExpression(arg))
            } else {
                putValueArgument(i, arg.transform(this@ComposableCallTransformer, null))
            }
        }
        return this
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
        if (expression.origin == IrStatementOrigin.LAMBDA) {
            if (expression.function.valueParameters.lastOrNull()?.isComposerParam() == true) {
                return DeclarationIrBuilder(context, declarationStack.last().symbol).irBlock {
                    +expression.transformChildren()
                }
            }
        }
        return super.visitFunctionExpression(expression)
    }

    private fun IrBlockBuilder.irComposableCall(
        original: IrCall
    ): IrExpression {
        val composerArg = original.getValueArgument(original.valueArgumentsCount - 1)!!
        // TODO(lmr): we may want to rewrite this in a way that doesn't do a deepCopy...
        val getComposer = { composerArg.deepCopyWithVariables() }
        return irComposableCallBase(
            original,
            getComposer
        )
    }

    private fun IrBlockBuilder.irComposableCallBase(
        original: IrCall,
        getComposer: () -> IrExpression
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
            .symbol
            .descriptor
            .valueParameters
            .map {
                val arg = original.getValueArgument(it)
                it to getParameterExpression(it, arg)
            }

        val tmpDispatchReceiver = original.dispatchReceiver?.let { irTemporary(it) }
        val tmpExtensionReceiver = original.extensionReceiver?.let { irTemporary(it) }

        val callDescriptor = composerTypeDescriptor
            .unsubstitutedMemberScope
            .findFirstFunction(KtxNameConventions.CALL.identifier) {
                it.valueParameters.size == 3
            }

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

            val validatedArguments = irGetArguments
                .take(irGetArguments.size - 1)
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
                        owner = symbol.descriptor.containingDeclaration
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
                        owner = symbol.descriptor.containingDeclaration
                    ),
                    type = blockParameter.type.toIrType()
                ) {
                    +irCall(
                        callee = IrSimpleFunctionSymbolImpl(original.symbol.descriptor).also {
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
        return irBlock(resultType = original.type) {
            val composerParam = nearestComposer()
            val getComposer = { irGet(composerParam) }
            irComposableExprBase(
                original,
                getComposer
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
            .symbol
            .descriptor
            .valueParameters
            .any { it.hasPivotalAnnotation() }

        // if we don't have any pivotal parameters, we don't use the parameters more than once,
        // so we can just use the original call itself.
        val irGetArguments = original
            .symbol
            .descriptor
            .valueParameters
            .map {
                val arg = original.getValueArgument(it)
                val expr = if (hasPivotals)
                    getParameterExpression(it, arg)
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

        val newCall = if (hasPivotals) irCall(
            callee = IrSimpleFunctionSymbolImpl(original.symbol.descriptor).also {
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
        expr: IrExpression?
    ): () -> IrExpression? {
        if (expr == null)
            return { null }
        return when {
            expr is IrConst<*> ->
                ({ expr.copy() })
            isChildrenParameter(desc, expr) ->
                ({ expr })
            else -> {
                val temp = irTemporary(
                    value = expr,
                    irType = expr.type
                )
                ({ irGet(temp) })
            }
        }
    }

    private fun nearestComposer(): IrValueParameter {
        for (fn in declarationStack.asReversed()) {
            if (fn is IrFunction) {
                val param = fn.valueParameters.lastOrNull()
                if (param != null && param.isComposerParam()) {
                    return param
                }
            }
        }
        error("Couldn't find composer parameter")
    }

    private fun IrBlockBuilder.irComposableEmit(
        original: IrCall,
        emitMetadata: ComposableEmitMetadata
    ): IrExpression {
        val composerParam = nearestComposer()
        return irComposableEmitBase(
            original,
            { irGet(composerParam) },
            emitMetadata
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
            .symbol
            .descriptor
            .valueParameters
            .mapNotNull { desc ->
                original.getValueArgument(desc)?.let { desc to it }
            }
            .map { (desc, expr) ->
                desc.name.asString() to getParameterExpression(desc, expr)
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

                    +irReturn(IrConstructorCallImpl.fromSymbolDescriptor(startOffset, endOffset,
                        ctorCall.candidateDescriptor.returnType!!.toIrType(), ctorCallSymbol)
                        .apply {
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
        return symbol.descriptor.fqNameSafe.toString().hashCode() xor startOffset
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
}
