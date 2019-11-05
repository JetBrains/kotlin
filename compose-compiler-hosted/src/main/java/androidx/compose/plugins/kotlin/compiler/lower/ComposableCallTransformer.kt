package androidx.compose.plugins.kotlin.compiler.lower

import androidx.compose.plugins.kotlin.COMPOSABLE_EMIT_OR_CALL
import androidx.compose.plugins.kotlin.ComposableEmitDescriptor
import androidx.compose.plugins.kotlin.ComposableFunctionDescriptor
import androidx.compose.plugins.kotlin.EmitChildrenValueParameterDescriptor
import androidx.compose.plugins.kotlin.KtxNameConventions
import androidx.compose.plugins.kotlin.ValidatedAssignment
import androidx.compose.plugins.kotlin.ValidationType
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.plugins.kotlin.hasPivotalAnnotation
import androidx.compose.plugins.kotlin.isMarkedStable
import androidx.compose.plugins.kotlin.isSpecialType
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
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
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irReturnUnit
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
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
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.getValueArgument
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.putTypeArguments
import org.jetbrains.kotlin.ir.expressions.putValueArgument
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.ConstantValueGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class ComposableCallTransformer(val context: JvmBackendContext) :
    IrElementTransformerVoid(),
    FileLoweringPass {

    private val typeTranslator =
        TypeTranslator(
            context.ir.symbols.externalSymbolTable,
            context.state.languageVersionSettings,
            context.builtIns
        ).apply {
            constantValueGenerator = ConstantValueGenerator(
                context.state.module,
                context.ir.symbols.externalSymbolTable
            )
            constantValueGenerator.typeTranslator = this
        }

    private val builtIns = context.irBuiltIns

    private val orFunctionDescriptor = builtIns.builtIns.booleanType.memberScope
        .findFirstFunction("or") { it is FunctionDescriptor && it.isInfix }

    private val jvmContext get() = context

    private val symbolTable get() = context.ir.symbols.externalSymbolTable

    private fun KotlinType?.isStable(): Boolean {
        if (this == null) return false

        val trace = jvmContext.state.bindingTrace
        val calculated = trace.get(ComposeWritableSlices.STABLE_TYPE, this)
        return if (calculated == null) {
            val isStable = !isError &&
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

    private val KotlinType.isEnum get() =
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

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (expression.origin != COMPOSABLE_EMIT_OR_CALL) {
            return super.visitBlock(expression)
        }

        assert(expression.statements.size == 2)
        // the first statement should represent the call to get the composer
        // the second statement should represent the composable call or emit
        val (first, second) = expression.statements
        val composerCall = when {
            first is IrCall -> first
            // the psi->IR phase seems to generate coercions to UNIT, so we won't just find the
            // bare call here.
            first is IrTypeOperatorCall &&
            first.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT &&
            first.argument is IrCall -> first.argument as IrCall
            else -> error("Couldn't find composer call in block")
        }

        val emitOrCall = when {
            second is IrCall -> second
            // the psi -> IR phase seems to generate argument reordering blocks when calls are
            // made with named arguments out of order. In this case we need to find the last
            // statement
            second is IrBlock &&
            second.origin == IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL &&
            second.statements.last() is IrCall -> second.statements.last() as IrCall
            else -> error("Couldn't find composable call in block")
        }

        val descriptor = emitOrCall.descriptor

        return when {
            descriptor.isInline -> expression
            descriptor is ComposableFunctionDescriptor -> {
                val transformedComposerCall = composerCall.transformChildren()
                val transformed = emitOrCall.transformChildren()

                context.createIrBuilder(declarationStack.last().symbol).irBlock {
                    +irComposableCall(transformedComposerCall, transformed, descriptor)
                }
            }
            descriptor is ComposableEmitDescriptor -> {
                val transformedComposerCall = composerCall.transformChildren()
                val transformed = emitOrCall.transformChildren()
                context.createIrBuilder(declarationStack.last().symbol).irBlock {
                    +irComposableEmit(transformedComposerCall, transformed, descriptor)
                }
            }
            else -> error("dont know what to do with $descriptor")
        }
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
        composerCall: IrCall,
        original: IrCall,
        descriptor: ComposableFunctionDescriptor
    ): IrExpression {
        val composerTemp = irTemporary(composerCall)

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
        val composer = descriptor.composerCall.resultingDescriptor as PropertyDescriptor

        // TODO(lmr): the way we grab temporaries here feels wrong. We should investigate the right
        // way to do this. Additionally, we are creating temporary vars for variables which is
        // causing larger stack space than needed in our generated code.

        val irGetArguments = original
            .descriptor
            .valueParameters
            .map {
                val arg = original.getValueArgument(it)
                it to getParameterExpression(it, arg)
            }

        val tmpDispatchReceiver = original.dispatchReceiver?.let { irTemporary(it) }
        val tmpExtensionReceiver = original.extensionReceiver?.let { irTemporary(it) }

        // TODO(lmr): come up with a better way to find this?
        val callDescriptor = descriptor
            .composerMetadata
            .callDescriptors
            .first { it.typeParametersCount == 0 }

        val joinKeyDescriptor = composer
            .type
            .memberScope
            .findFirstFunction(KtxNameConventions.JOINKEY.identifier) {
                it.valueParameters.size == 2
            }

        val callParameters = callDescriptor.valueParameters
            .map { it.name to it }
            .toMap()

        fun getCallParameter(name: Name) = callParameters[name]
            ?: error("Expected $name parameter to exist")

        return irCall(
            callee = symbolTable.referenceFunction(callDescriptor),
            type = builtIns.unitType // TODO(lmr): refactor call(...) to return a type
        ).apply {
            dispatchReceiver = irGet(composerTemp)

            putValueArgument(
                getCallParameter(KtxNameConventions.CALL_KEY_PARAMETER),
                irGroupKey(
                    original = original,
                    getComposer = { irGet(composerTemp) },
                    joinKey = joinKeyDescriptor,
                    pivotals = irGetArguments.mapNotNull { (param, getExpr) ->
                        val expr = getExpr()
                        if (expr == null) null
                        else if (!param.hasPivotalAnnotation()) null
                        else expr
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
                irGetArguments.mapNotNull { (_, getExpr) -> getExpr() } +
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
                        callee = symbolTable.referenceFunction(descriptor.underlyingDescriptor),
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
        if (expr.isReorderTemporaryVariable()) {
            return getParameterExpression(desc, expr.unwrapReorderTemporaryVariable())
        }
        return when {
            expr is IrConst<*> ->
                ({ expr.copy() })
            isChildrenParameter(desc, expr) ->
                ({ expr })
            else -> {
                val temp = irTemporary(
                    expr,
                    typeHint = desc.type,
                    irType = desc.type.toIrType()
                )
                ({ irGet(temp) })
            }
        }
    }

    private fun IrBlockBuilder.irComposableEmit(
        composerCall: IrCall,
        original: IrCall,
        descriptor: ComposableEmitDescriptor
    ): IrExpression {
        val composerTemp = irTemporary(composerCall)
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

        val parametersByName = descriptor
            .valueParameters
            .mapNotNull { desc ->
                original.getValueArgument(desc)?.let { desc to it }
            }
            .map { (desc, expr) ->
                desc.name.asString() to getParameterExpression(desc, expr)
            }
            .toMap()

        val emitCall = descriptor.emitCall
        val emitFunctionDescriptor = emitCall.candidateDescriptor

        val emitParameters = emitFunctionDescriptor.valueParameters
            .map { it.name to it }
            .toMap()

        fun getEmitParameter(name: Name) = emitParameters[name]
            ?: error("Expected $name parameter to exist")

        val emitFunctionSymbol = symbolTable.referenceFunction(emitFunctionDescriptor)

        val joinKeyDescriptor = descriptor.composer
            .getReturnType()
            .memberScope
            .findFirstFunction(KtxNameConventions.JOINKEY.identifier) {
                it.valueParameters.size == 2
            }

        fun irGetParameter(name: String): IrExpression = parametersByName[name]?.invoke()
                ?: error("No parameter found with name $name")

        return irCall(
            callee = emitFunctionSymbol,
            type = builtIns.unitType
        ).apply {
            dispatchReceiver = irGet(composerTemp)
            // TODO(lmr): extensionReceiver.
            // We would want to do this to enable "emit" and "call" implementations that are
            // extensions on the composer

            putTypeArguments(emitCall.typeArguments) { it.toIrType() }

            putValueArgument(
                getEmitParameter(KtxNameConventions.EMIT_KEY_PARAMETER),
                irGroupKey(
                    original = original,
                    getComposer = { irGet(composerTemp) },
                    joinKey = joinKeyDescriptor,
                    pivotals = descriptor.pivotals.map { irGetParameter(it) }
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

                    val ctorCall = descriptor.ctorCall

                    val ctorCallSymbol = symbolTable.referenceConstructor(
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
                        descriptor.ctorParams.forEach { name ->
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

            val updateParam = getEmitParameter(KtxNameConventions
                .EMIT_UPDATER_PARAMETER)

            val updateLambdaDescriptor = createFunctionDescriptor(updateParam.type)

            putValueArgument(
                updateParam,
                irLambdaExpression(
                    original.startOffset,
                    original.endOffset,
                    descriptor = updateLambdaDescriptor,
                    type = updateParam.type.toIrType()
                ) { fn ->
                    descriptor.validations.forEach {
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

            if (descriptor.hasChildren) {
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
                callee = symbolTable.referenceFunction(joinKey),
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
            callee = symbolTable.referenceFunction(changedDescriptor),
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
            callee = symbolTable.referenceFunction(orFunctionDescriptor),
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
            callee = symbolTable.referenceFunction(validationCallDescriptor),
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
                    is PropertyDescriptor -> symbolTable.referenceFunction(
                        assignmentDescriptor.setter!!
                    )
                    else -> symbolTable.referenceFunction(assignmentDescriptor)
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
                                        error("Extension instance methods are not allowed for " +
                                                "assignments")
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

    private fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)

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
                        Annotations.EMPTY)
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
