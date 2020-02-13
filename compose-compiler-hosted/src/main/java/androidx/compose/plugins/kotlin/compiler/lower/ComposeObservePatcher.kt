/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin.compiler.lower

import androidx.compose.plugins.kotlin.ComposeFqNames
import androidx.compose.plugins.kotlin.KtxNameConventions
import androidx.compose.plugins.kotlin.KtxNameConventions.UPDATE_SCOPE
import androidx.compose.plugins.kotlin.isEmitInline
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.util.OperatorNameConventions

class ComposeObservePatcher(
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

    override fun visitFunction(declaration: IrFunction): IrStatement {
        super.visitFunction(declaration)
        return visitFunctionForComposerParam(declaration)
    }

    private fun visitFunctionForComposerParam(declaration: IrFunction): IrStatement {
        // Only insert observe scopes in non-empty composable function
        if (declaration.body == null)
            return declaration

        val descriptor = declaration.descriptor

        // Do not insert observe scope in an inline function
        if (descriptor.isInline)
            return declaration

        // Do not insert an observe scope in an inline composable lambda
        descriptor.findPsi()?.let { psi ->
            (psi as? KtFunctionLiteral)?.let {
                if (InlineUtil.isInlinedArgument(
                        it,
                        context.bindingContext,
                        false
                    )
                )
                    return declaration
                if (it.isEmitInline(context.bindingContext)) {
                    return declaration
                }
            }
        }

        // Do not insert an observe scope if the function has a return result
        if (descriptor.returnType.let { it == null || !it.isUnit() })
            return declaration

        // Do not insert an observe scope if the function hasn't been transformed by the
        // ComposerParamTransformer and has a synthetic "composer param" as its last parameter
        val param = declaration.valueParameters.lastOrNull()
        if (param == null || !param.isComposerParam()) return declaration

        // Check if the descriptor has restart scope calls resolved
        if (descriptor is SimpleFunctionDescriptor &&
            // Lambdas should be ignored. All composable lambdas are wrapped by a restartable
            // function wrapper by ComposerLambdaMemoization which supplies the startRestartGroup/
            // endRestartGroup pair on behalf of the lambda.
            declaration.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA &&
            declaration.origin != IrDeclarationOrigin.LOCAL_FUNCTION_NO_CLOSURE) {

            return functionWithRestartGroup(
                declaration
            ) { irGet(param) }
        }

        return declaration
    }

    private fun functionWithRestartGroup(
        original: IrFunction,
        getComposer: DeclarationIrBuilder.() -> IrExpression
    ): IrStatement {
        val oldBody = original.body

        val startRestartGroupDescriptor = composerTypeDescriptor
            .unsubstitutedMemberScope
            .findFirstFunction(KtxNameConventions.STARTRESTARTGROUP.identifier) {
                it.valueParameters.size == 1
            }

        val endRestartGroupDescriptor = composerTypeDescriptor
            .unsubstitutedMemberScope
            .findFirstFunction(KtxNameConventions.ENDRESTARTGROUP.identifier) {
                it.valueParameters.size == 0
            }

        // Create call to get the composer
        val unitType = context.irBuiltIns.unitType

        val irBuilder = DeclarationIrBuilder(context, original.symbol)

        // Create call to startRestartGroup
        val startRestartGroup = irMethodCall(
            irBuilder.getComposer(),
            startRestartGroupDescriptor
        ).apply {
            putValueArgument(
                0,
                keyExpression(
                    symbol.descriptor,
                    original.startOffset,
                    context.builtIns.intType.toIrType()
                )
            )
        }

        // Create call to endRestartGroup
        val endRestartGroup = irMethodCall(irBuilder.getComposer(), endRestartGroupDescriptor)

        val updateScopeDescriptor =
            endRestartGroupDescriptor.returnType?.memberScope?.getContributedFunctions(
                UPDATE_SCOPE,
                NoLookupLocation.FROM_BACKEND
            )?.singleOrNull { it.valueParameters.first().type.arguments.size == 2 }
                ?: error("new updateScope not found in result type of endRestartGroup")
        val updateScopeArgument:
                    (outerBuilder: IrBlockBuilder) -> IrExpression =
            if (original.isZeroParameterComposableUnitLambda()) { _ ->
                // If we are in an invoke function for a callable class with no
                // parameters then the `this` parameter can be used for the endRestartGroup.
                // If isUnitInvoke() returns true then dispatchReceiverParameter is not
                // null.
                irBuilder.irGet(original.dispatchReceiverParameter!!)
            } else { outerBuilder ->
                // Create self-invoke lambda
                val blockParameterDescriptor =
                    updateScopeDescriptor.valueParameters.singleOrNull()
                        ?: error("expected a single block parameter for updateScope")
                val blockParameterType = blockParameterDescriptor.type
                val selfSymbol = original.symbol

                val lambdaDescriptor = AnonymousFunctionDescriptor(
                    original.descriptor,
                    Annotations.EMPTY,
                    CallableMemberDescriptor.Kind.DECLARATION,
                    SourceElement.NO_SOURCE,
                    false
                )

                val passedInComposerParameter = ValueParameterDescriptorImpl(
                    containingDeclaration = lambdaDescriptor,
                    original = null,
                    index = 0,
                    annotations = Annotations.EMPTY,
                    name = KtxNameConventions.COMPOSER_PARAMETER,
                    outType = composerTypeDescriptor.defaultType.makeNullable(),
                    declaresDefaultValue = false,
                    isCrossinline = false,
                    isNoinline = false,
                    varargElementType = null,
                    source = SourceElement.NO_SOURCE
                )

                lambdaDescriptor.apply {
                    initialize(
                        null,
                        null,
                        emptyList(),
                        listOf(passedInComposerParameter),
                        blockParameterType,
                        Modality.FINAL,
                        Visibilities.LOCAL
                    )
                }

                val fn = IrFunctionImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
                    IrSimpleFunctionSymbolImpl(lambdaDescriptor),
                    context.irBuiltIns.unitType
                ).also { fn ->
                    fn.parent = original
                    val localIrBuilder = DeclarationIrBuilder(context, fn.symbol)
                    fn.addValueParameter(
                        KtxNameConventions.COMPOSER_PARAMETER.identifier,
                        composerTypeDescriptor.defaultType.toIrType().makeNullable()
                    )
                    fn.body = localIrBuilder.irBlockBody {
                        // Call the function again with the same parameters
                        +irReturn(irCall(selfSymbol).apply {
                            original.valueParameters
                                .filter { !it.isComposerParam() }
                                .forEachIndexed {
                                    index, valueParameter ->
                                putValueArgument(
                                    index, IrGetValueImpl(
                                        UNDEFINED_OFFSET,
                                        UNDEFINED_OFFSET,
                                        valueParameter.type,
                                        valueParameter.symbol
                                    )
                                )
                            }
                            putValueArgument(
                                symbol.descriptor.valueParameters.size - 1,
                                IrGetValueImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    composerTypeDescriptor.defaultType.toIrType(),
                                    fn.valueParameters[0].symbol
                                )
                            )
                            symbol.descriptor.dispatchReceiverParameter?.let {
                                // Ensure we get the correct type by trying to avoid
                                // going through a KotlinType if possible.
                                val parameter = original.dispatchReceiverParameter
                                    ?: error("Expected dispatch receiver on declaration")
                                val receiver = irGet(
                                    parameter.type,
                                    parameter.symbol
                                )

                                // Save the dispatch receiver into a temporary created in
                                // the outer scope because direct references to the
                                // receiver sometimes cause an invalid name, "$<this>", to
                                // be generated.
                                val tmp = outerBuilder.irTemporary(
                                    value = receiver,
                                    nameHint = "rcvr",
                                    irType = parameter.type
                                )
                                dispatchReceiver = irGet(tmp)
                            }
                            symbol.descriptor.extensionReceiverParameter?.let {
                                    receiverDescriptor ->
                                extensionReceiver = irGet(
                                    receiverDescriptor.type.toIrType(),
                                    original.extensionReceiverParameter?.symbol
                                        ?: error(
                                            "Expected extension receiver on declaration"
                                        )
                                )
                            }
                            original.typeParameters.forEachIndexed { index, parameter ->
                                putTypeArgument(index, parameter.defaultType)
                            }
                        })
                    }
                }
                irBuilder.irBlock(origin = IrStatementOrigin.LAMBDA) {
                    +fn
                    +IrFunctionReferenceImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        blockParameterType.toIrType(),
                        fn.symbol,
                        0,
                        IrStatementOrigin.LAMBDA
                    )
                }
            }

        val endRestartGroupCallBlock = irBuilder.irBlock(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET
        ) {
            val result = irTemporary(endRestartGroup)
            val updateScopeSymbol = referenceSimpleFunction(updateScopeDescriptor)
            +irIfThen(irNot(irEqeqeq(irGet(result.type, result.symbol), irNull())),
                IrCallImpl(startOffset, endOffset, updateScopeDescriptor.returnType!!.toIrType(),
                    updateScopeSymbol).apply {
                    dispatchReceiver = irGet(result.type, result.symbol)
                    putValueArgument(
                        0,
                        updateScopeArgument(this@irBlock)
                    )
                }
            )
        }

        when (oldBody) {
            is IrBlockBody -> {
                val earlyReturn = findPotentialEarly(oldBody)
                if (earlyReturn != null) {
                    if (earlyReturn is IrReturn &&
                        oldBody.statements.lastOrNull() == earlyReturn) {
                        // Transform block from:
                        // {
                        //   ...
                        //   return value
                        // }
                        // to:
                        // {
                        //  composer.startRestartGroup()
                        //  ...
                        //  val tmp = value
                        //  composer.endRestartGroup()
                        //  return tmp
                        // }
                        original.body = irBuilder.irBlockBody {
                            +startRestartGroup
                            oldBody.statements
                                .take(oldBody.statements.size - 1)
                                .forEach { +it }
                            val temp = irTemporary(earlyReturn.value)
                            +endRestartGroupCallBlock
                            +irReturn(irGet(temp))
                        }
                    } else {
                        // Transform the block into
                        // composer.startRestartGroup()
                        // try {
                        //   ... old statements ...
                        // } finally {
                        //    composer.endRestartGroup()
                        // }
                        original.body = irBuilder.irBlockBody {
                            +IrTryImpl(
                                oldBody.startOffset, oldBody.endOffset, unitType,
                                IrBlockImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    unitType
                                ).apply {
                                    statements.add(startRestartGroup)
                                    statements.addAll(oldBody.statements)
                                },
                                catches = emptyList(),
                                finallyExpression = endRestartGroupCallBlock
                            )
                        }
                    }
                } else {
                    // Insert the start and end calls into the block
                    oldBody.statements.add(0, startRestartGroup)
                    oldBody.statements.add(endRestartGroupCallBlock)
                }
                return original
            }
            else -> {
                // Composable function do not use IrExpressionBody as they are converted
                // by the call lowering to IrBlockBody to introduce the call temporaries.
                error("Encountered IrExpressionBOdy when IrBlockBody was expected")
            }
        }
    }

    private fun irCall(descriptor: FunctionDescriptor): IrCall {
        val type = descriptor.returnType?.toIrType() ?: error("Expected a return type")
        val symbol = referenceFunction(descriptor)
        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            symbol
        )
    }

    private fun irMethodCall(target: IrExpression, descriptor: FunctionDescriptor): IrCall {
        return irCall(descriptor).apply {
            dispatchReceiver = target
        }
    }

    private fun keyExpression(
        descriptor: CallableMemberDescriptor,
        sourceOffset: Int,
        intType: IrType
    ): IrExpression {
        val sourceKey = getKeyValue(descriptor, sourceOffset)
        return IrConstImpl.int(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            intType,
            sourceKey
        )
    }

    private fun IrFunction.isZeroParameterComposableUnitLambda(): Boolean {
        if (name.isSpecial || name != OperatorNameConventions.INVOKE || !returnType.isUnit())
            return false
        val type = dispatchReceiverParameter?.type ?: return false
        return valueParameters.size == 1 &&
                    type.getClass()?.superTypes?.any {
                        val fqName = it.classifierOrNull?.descriptor?.fqNameSafe
                        fqName == ComposeFqNames.Function1
                    } == true &&
                    valueParameters[0].isComposerParam()
    }
}

private fun findPotentialEarly(block: IrBlockBody): IrExpression? {
    var result: IrExpression? = null
    block.accept(object : IrElementVisitor<Unit, Unit> {
        override fun visitElement(element: IrElement, data: Unit) {
            if (result == null)
                element.acceptChildren(this, Unit)
        }

        override fun visitBreak(jump: IrBreak, data: Unit) {
            result = jump
        }

        override fun visitContinue(jump: IrContinue, data: Unit) {
            result = jump
        }

        override fun visitReturn(expression: IrReturn, data: Unit) {
            result = expression
        }

        override fun visitDeclaration(declaration: IrDeclaration, data: Unit) {
            // Skip bodies of declarations
        }
    }, Unit)
    return result
}

internal fun getKeyValue(descriptor: DeclarationDescriptor, startOffset: Int): Int =
    descriptor.fqNameSafe.toString().hashCode() xor startOffset