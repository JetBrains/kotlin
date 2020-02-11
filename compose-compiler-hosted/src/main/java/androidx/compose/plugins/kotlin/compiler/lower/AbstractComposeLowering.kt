/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.plugins.kotlin.ComposableAnnotationChecker
import androidx.compose.plugins.kotlin.ComposeFqNames
import androidx.compose.plugins.kotlin.KtxNameConventions
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.plugins.kotlin.irTrace
import androidx.compose.plugins.kotlin.isMarkedStable
import androidx.compose.plugins.kotlin.isSpecialType
import org.jetbrains.kotlin.backend.common.descriptors.isFunctionOrKFunctionType
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.ConstantValueGenerator
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.unsubstitutedUnderlyingType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

abstract class AbstractComposeLowering(
    val context: JvmBackendContext,
    val symbolRemapper: DeepCopySymbolRemapper,
    val bindingTrace: BindingTrace
) : IrElementTransformerVoid() {

    protected val typeTranslator =
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

    protected val builtIns = context.irBuiltIns

    protected val composerTypeDescriptor = context.state.module.findClassAcrossModuleDependencies(
        ClassId.topLevel(ComposeFqNames.Composer)
    ) ?: error("Cannot find the Composer class")

    private val symbolTable get() = context.ir.symbols.externalSymbolTable

    fun referenceFunction(descriptor: CallableDescriptor): IrFunctionSymbol {
        return symbolRemapper.getReferencedFunction(symbolTable.referenceFunction(descriptor))
    }

    fun referenceSimpleFunction(descriptor: SimpleFunctionDescriptor): IrSimpleFunctionSymbol {
        return symbolRemapper.getReferencedSimpleFunction(
            symbolTable.referenceSimpleFunction(descriptor)
        )
    }

    fun referenceConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol {
        return symbolRemapper.getReferencedConstructor(symbolTable.referenceConstructor(descriptor))
    }

    fun getTopLevelClass(fqName: FqName): IrClassSymbol {
        val descriptor = context.state.module.getPackage(fqName.parent()).memberScope
            .getContributedClassifier(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor? ?: error("Class is not found: $fqName")
        return symbolTable.referenceClass(descriptor)
    }

    fun getTopLevelFunction(fqName: FqName): IrFunctionSymbol {
        val descriptor = context.state.module.getPackage(fqName.parent()).memberScope
            .getContributedFunctions(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ).singleOrNull() ?: error("Function not found $fqName")
        return symbolTable.referenceSimpleFunction(descriptor)
    }

    fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)

    fun IrValueParameter.isComposerParam(): Boolean =
        (descriptor as? ValueParameterDescriptor)?.isComposerParam() ?: false

    fun ValueParameterDescriptor.isComposerParam(): Boolean =
        name == KtxNameConventions.COMPOSER_PARAMETER &&
                type.constructor.declarationDescriptor?.fqNameSafe == ComposeFqNames.Composer

    fun IrAnnotationContainer.hasComposableAnnotation(): Boolean {
        return annotations.hasAnnotation(ComposeFqNames.Composable)
    }

    fun IrCall.isTransformedComposableCall(): Boolean {
        return context.irTrace[ComposeWritableSlices.IS_COMPOSABLE_CALL, this] ?: false
    }

    fun IrCall.isSyntheticComposableCall(): Boolean {
        return context.irTrace[ComposeWritableSlices.IS_SYNTHETIC_COMPOSABLE_CALL, this] == true
    }

    private val composableChecker = ComposableAnnotationChecker()

    protected val KotlinType.isEnum
        get() =
            (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.ENUM_CLASS

    protected fun KotlinType?.isStable(): Boolean {
        if (this == null) return false

        val trace = bindingTrace
        val calculated = trace.get(ComposeWritableSlices.STABLE_TYPE, this)
        return if (calculated == null) {
            val isStable = !isError &&
                    !isTypeParameter() &&
                    !isSpecialType &&
                    (
                            KotlinBuiltIns.isPrimitiveType(this) ||
                                    isFunctionOrKFunctionType ||
                                    isEnum ||
                                    KotlinBuiltIns.isString(this) ||
                                    isMarkedStable() ||
                                    (
                                            isNullable() &&
                                                    makeNotNullable().isStable()
                                            ) ||
                                    (
                                            isInlineClassType() &&
                                                    unsubstitutedUnderlyingType().isStable()
                                            )
                            )
            trace.record(ComposeWritableSlices.STABLE_TYPE, this, isStable)
            isStable
        } else calculated
    }

    fun FunctionDescriptor.isComposable(): Boolean {
        val composability = composableChecker.analyze(bindingTrace, this)
        return when (composability) {
            ComposableAnnotationChecker.Composability.NOT_COMPOSABLE -> false
            ComposableAnnotationChecker.Composability.MARKED -> true
            ComposableAnnotationChecker.Composability.INFERRED -> true
        }
    }

    fun IrFunction.isComposable(): Boolean = descriptor.isComposable()
    fun IrFunctionExpression.isComposable(): Boolean = function.isComposable()

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

    protected fun IrBuilderWithScope.irLambdaExpression(
        descriptor: FunctionDescriptor,
        type: IrType,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ) = irLambdaExpression(this.startOffset, this.endOffset, descriptor, type, body)

    protected fun IrBuilderWithScope.irLambdaExpression(
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
            it.body = DeclarationIrBuilder(this@AbstractComposeLowering.context, symbol)
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

    protected fun IrBuilderWithScope.createFunctionDescriptor(
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