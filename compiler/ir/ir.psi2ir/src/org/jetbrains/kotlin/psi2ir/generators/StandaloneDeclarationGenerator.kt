/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPureElement
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.kotlin.psi2ir.endOffsetOrUndefined
import org.jetbrains.kotlin.psi2ir.startOffsetOrUndefined
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.types.KotlinType

class StandaloneDeclarationGenerator(private val context: GeneratorContext) {
    private val typeTranslator = context.typeTranslator
    private val symbolTable = context.symbolTable

    // TODO: use this generator in psi2ir too

    fun KotlinType.toIrType() = typeTranslator.translateType(this)

    protected fun generateGlobalTypeParametersDeclarations(
        irTypeParametersOwner: IrTypeParametersContainer,
        from: List<TypeParameterDescriptor>
    ) {
        generateTypeParameterDeclarations(irTypeParametersOwner, from) { startOffset, endOffset, typeParameterDescriptor ->
            symbolTable.declareGlobalTypeParameter(startOffset, endOffset, IrDeclarationOrigin.DEFINED, typeParameterDescriptor)
        }
    }

    fun generateScopedTypeParameterDeclarations(
        irTypeParametersOwner: IrTypeParametersContainer,
        from: List<TypeParameterDescriptor>
    ) {
        generateTypeParameterDeclarations(irTypeParametersOwner, from) { startOffset, endOffset, typeParameterDescriptor ->
            symbolTable.declareScopedTypeParameter(startOffset, endOffset, IrDeclarationOrigin.DEFINED, typeParameterDescriptor)
        }
    }

    private fun generateTypeParameterDeclarations(
        irTypeParametersOwner: IrTypeParametersContainer,
        from: List<TypeParameterDescriptor>,
        declareTypeParameter: (Int, Int, TypeParameterDescriptor) -> IrTypeParameter
    ) {
        irTypeParametersOwner.typeParameters += from.map { typeParameterDescriptor ->
            val ktTypeParameterDeclaration = DescriptorToSourceUtils.getSourceFromDescriptor(typeParameterDescriptor)
            val startOffset = ktTypeParameterDeclaration.startOffsetOrUndefined
            val endOffset = ktTypeParameterDeclaration.endOffsetOrUndefined
            declareTypeParameter(
                startOffset,
                endOffset,
                typeParameterDescriptor
            ).also {
                it.parent = irTypeParametersOwner
            }
        }

        for (irTypeParameter in irTypeParametersOwner.typeParameters) {
            irTypeParameter.descriptor.upperBounds.mapTo(irTypeParameter.superTypes) {
                it.toIrType()
            }
        }
    }

    fun generateClass(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor, symbol: IrClassSymbol): IrClass {

        val irClass = IrClassImpl(startOffset, endOffset, origin, symbol, descriptor)

        symbolTable.withScope(irClass) {
            irClass.metadata = MetadataSource.Class(descriptor)

            generateGlobalTypeParametersDeclarations(irClass, descriptor.declaredTypeParameters)
            irClass.superTypes = descriptor.typeConstructor.supertypes.map {
                it.toIrType()
            }

            irClass.thisReceiver = context.symbolTable.declareValueParameter(
                startOffset, endOffset,
                IrDeclarationOrigin.INSTANCE_RECEIVER,
                descriptor.thisAsReceiverParameter,
                descriptor.thisAsReceiverParameter.type.toIrType()
            ).also { it.parent = irClass }
        }

        return irClass
    }

    fun generateEnumEntry(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor, symbol: IrEnumEntrySymbol): IrEnumEntry {
        // TODO: corresponging class?
        val irEntry = IrEnumEntryImpl(startOffset, endOffset, origin, symbol, descriptor.name)

        return irEntry
    }

    fun generateTypeAlias(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeAliasDescriptor,
        symbol: IrTypeAliasSymbol
    ): IrTypeAlias {
        val irAlias = IrTypeAliasImpl.fromSymbolDescriptor(
            startOffset, endOffset, symbol, descriptor.expandedType.toIrType(), origin, descriptor
        )

        generateGlobalTypeParametersDeclarations(irAlias, descriptor.declaredTypeParameters)

        return irAlias
    }

    protected fun declareParameter(descriptor: ParameterDescriptor, ktElement: KtPureElement?, irOwnerElement: IrElement): IrValueParameter {
        return symbolTable.declareValueParameter(
            ktElement?.pureStartOffset ?: irOwnerElement.startOffset,
            ktElement?.pureEndOffset ?: irOwnerElement.endOffset,
            IrDeclarationOrigin.DEFINED,
            descriptor, descriptor.type.toIrType(),
            (descriptor as? ValueParameterDescriptor)?.varargElementType?.toIrType()
        )
    }

    protected fun generateValueParameterDeclarations(
        irFunction: IrFunction,
        functionDescriptor: FunctionDescriptor,
        defaultArgumentFactory: IrFunction.(ValueParameterDescriptor) -> IrExpressionBody?
    ) {

        // TODO: KtElements

        irFunction.dispatchReceiverParameter = functionDescriptor.dispatchReceiverParameter?.let {
            declareParameter(it, null, irFunction)
        }

        irFunction.extensionReceiverParameter = functionDescriptor.extensionReceiverParameter?.let {
            declareParameter(it, null, irFunction)
        }

        // Declare all the value parameters up first.
        irFunction.valueParameters = functionDescriptor.valueParameters.map { valueParameterDescriptor ->
            val ktParameter = DescriptorToSourceUtils.getSourceFromDescriptor(valueParameterDescriptor) as? KtParameter
            declareParameter(valueParameterDescriptor, ktParameter, irFunction).also {
                it.defaultValue = irFunction.defaultArgumentFactory(valueParameterDescriptor)
            }
        }
    }

    fun generateConstructor(
        startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassConstructorDescriptor, symbol: IrConstructorSymbol,
        defaultArgumentFactory: IrFunction.(ValueParameterDescriptor) -> IrExpressionBody? = { null }
    ): IrConstructor {
        val irConstructor = IrConstructorImpl(startOffset, endOffset, origin, symbol, IrUninitializedType, descriptor)
        irConstructor.metadata = MetadataSource.Function(descriptor)

        symbolTable.withScope(irConstructor) {
            val ctorTypeParameters = descriptor.typeParameters.filter { it.containingDeclaration === descriptor }
            generateScopedTypeParameterDeclarations(irConstructor, ctorTypeParameters)
            generateValueParameterDeclarations(irConstructor, descriptor, defaultArgumentFactory)
            irConstructor.returnType = descriptor.returnType.toIrType()
        }

        return irConstructor
    }

    protected fun generateOverridenSymbols(irFunction: IrSimpleFunction, overridens: Collection<FunctionDescriptor>) {
        irFunction.overriddenSymbols = overridens.map { symbolTable.referenceSimpleFunction(it.original) }
    }

    fun generateSimpleFunction(
        startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: FunctionDescriptor, symbol: IrSimpleFunctionSymbol,
        defaultArgumentFactory: IrFunction.(ValueParameterDescriptor) -> IrExpressionBody? = { null }
    ): IrSimpleFunction {
        val irFunction = IrFunctionImpl(startOffset, endOffset, origin, symbol, IrUninitializedType, descriptor)
        irFunction.metadata = MetadataSource.Function(descriptor)

        symbolTable.withScope(descriptor) {
            generateOverridenSymbols(irFunction, descriptor.overriddenDescriptors)
            generateScopedTypeParameterDeclarations(irFunction, descriptor.typeParameters)
            generateValueParameterDeclarations(irFunction, descriptor, defaultArgumentFactory)
            irFunction.returnType = descriptor.returnType?.toIrType() ?: error("Expected return type $descriptor")
        }

        return irFunction
    }

    fun generateProperty(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: PropertyDescriptor, symbol: IrPropertySymbol): IrProperty {
        val irProperty = IrPropertyImpl(
            startOffset, endOffset, origin, symbol,
            name = descriptor.name,
            visibility = descriptor.visibility,
            modality = descriptor.modality,
            isVar = descriptor.isVar,
            isConst = descriptor.isConst,
            isLateinit = descriptor.isLateInit,
            isDelegated = false,
            isExternal = descriptor.isEffectivelyExternal(),
            isExpect = descriptor.isExpect
        )

        irProperty.metadata = MetadataSource.Property(descriptor)

        return irProperty
    }
}