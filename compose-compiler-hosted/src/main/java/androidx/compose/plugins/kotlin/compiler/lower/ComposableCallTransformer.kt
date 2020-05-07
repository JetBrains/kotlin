package androidx.compose.plugins.kotlin.compiler.lower

import androidx.compose.plugins.kotlin.ComposableEmitMetadata
import androidx.compose.plugins.kotlin.EmitChildrenValueParameterDescriptor
import androidx.compose.plugins.kotlin.KtxNameConventions
import androidx.compose.plugins.kotlin.ValidatedAssignment
import androidx.compose.plugins.kotlin.ValidationType
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.plugins.kotlin.irTrace
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irReturnUnit
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.getIrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.getValueArgument
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.putTypeArguments
import org.jetbrains.kotlin.ir.expressions.putValueArgument
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

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
                val param = fn.composerParam()
                if (param != null) {
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
