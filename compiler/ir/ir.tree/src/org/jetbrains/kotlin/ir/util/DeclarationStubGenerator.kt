/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.*
import org.jetbrains.kotlin.ir.descriptors.WrappedDeclarationDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyDescriptor
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.factories.IrDeclarationFactory
import org.jetbrains.kotlin.ir.factories.createValueParameter
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class DeclarationStubGenerator(
    moduleDescriptor: ModuleDescriptor,
    val symbolTable: SymbolTable,
    languageVersionSettings: LanguageVersionSettings,
    private val irDeclarationFactory: IrDeclarationFactory,
    val extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY
) : IrProvider {
    private val lazyTable = symbolTable.lazyWrapper

    internal var unboundSymbolGeneration: Boolean
        get() = lazyTable.stubGenerator != null
        set(value) {
            lazyTable.stubGenerator = if (value) this else null
        }

    private lateinit var irProviders_: List<IrProvider>

    fun setIrProviders(value: List<IrProvider>) {
        irProviders_ = value
        irProviders_.filterIsInstance<LazyIrProvider>().forEach { it.declarationStubGenerator = this }
    }

    val typeTranslator =
        TypeTranslator(lazyTable, languageVersionSettings, moduleDescriptor.builtIns, LazyScopedTypeParametersResolver(lazyTable), true)
    private val constantValueGenerator = ConstantValueGenerator(moduleDescriptor, lazyTable)

    private val facadeClassMap = mutableMapOf<DeserializedContainerSource, IrClass?>()

    init {
        typeTranslator.constantValueGenerator = constantValueGenerator
        constantValueGenerator.typeTranslator = typeTranslator
    }

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? = when {
        // Special case: generating field for an already generated property.
        symbol is IrFieldSymbol && (symbol.descriptor as? WrappedPropertyDescriptor)?.isBound() == true ->
            generateStubBySymbol(symbol)
        symbol.descriptor is WrappedDeclarationDescriptor<*> ->  null
        else -> generateStubBySymbol(symbol)
    }

    fun generateOrGetEmptyExternalPackageFragmentStub(descriptor: PackageFragmentDescriptor): IrExternalPackageFragment {
        val referenced = symbolTable.referenceExternalPackageFragment(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        return symbolTable.declareExternalPackageFragment(descriptor)
    }

    fun generateOrGetFacadeClass(descriptor: DeclarationDescriptor): IrClass? {
        val directMember = descriptor.safeAs<PropertyAccessorDescriptor>()?.correspondingProperty ?: descriptor
        val packageFragment = directMember.containingDeclaration as? PackageFragmentDescriptor ?: return null
        val containerSource = directMember.safeAs<DescriptorWithContainerSource>()?.containerSource ?: return null
        return facadeClassMap.getOrPut(containerSource) {
            extensions.generateFacadeClass(containerSource)?.also { facade ->
                val packageStub = generateOrGetEmptyExternalPackageFragmentStub(packageFragment)
                facade.parent = packageStub
                packageStub.declarations.add(facade)
            }
        }
    }


    fun generateMemberStub(descriptor: DeclarationDescriptor): IrDeclaration =
        when (descriptor) {
            is ClassDescriptor ->
                if (DescriptorUtils.isEnumEntry(descriptor))
                    generateEnumEntryStub(descriptor)
                else
                    generateClassStub(descriptor)
            is ClassConstructorDescriptor ->
                generateConstructorStub(descriptor)
            is FunctionDescriptor ->
                generateFunctionStub(descriptor)
            is PropertyDescriptor ->
                generatePropertyStub(descriptor)
            is TypeAliasDescriptor ->
                generateTypeAliasStub(descriptor)
            else ->
                throw AssertionError("Unexpected member descriptor: $descriptor")
        }

    private fun generateStubBySymbol(symbol: IrSymbol): IrDeclaration = when (symbol) {
        is IrFieldSymbol ->
            generateFieldStub(symbol.descriptor)
        is IrTypeParameterSymbol ->
            generateOrGetTypeParameterStub(symbol.descriptor)
        else ->
            generateMemberStub(symbol.descriptor)
    }

    private fun computeOrigin(descriptor: DeclarationDescriptor): IrDeclarationOrigin =
        extensions.computeExternalDeclarationOrigin(descriptor) ?: IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB

    internal fun generatePropertyStub(
        descriptor: PropertyDescriptor,
        bindingContext: BindingContext? = null
    ): IrProperty {
        val referenced = symbolTable.referenceProperty(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }

        val origin = computeOrigin(descriptor)
        return symbolTable.declareProperty(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor.original,
            isDelegated = @Suppress("DEPRECATION") descriptor.isDelegated
        ) {
            IrLazyProperty(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, it, this, typeTranslator, irDeclarationFactory, bindingContext)
        }
    }

    fun generateFieldStub(descriptor: PropertyDescriptor): IrField {
        val referenced = symbolTable.referenceField(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }

        val origin =
            if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
                IrDeclarationOrigin.FAKE_OVERRIDE
            else computeOrigin(descriptor)

        return symbolTable.declareField(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor.original, descriptor.type.toIrType()) {
            IrLazyField(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, it, this, typeTranslator, irDeclarationFactory)
        }
    }

    fun generateFunctionStub(descriptor: FunctionDescriptor, createPropertyIfNeeded: Boolean = true): IrSimpleFunction {
        val referenced = symbolTable.referenceSimpleFunction(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }

        if (createPropertyIfNeeded && descriptor is PropertyGetterDescriptor) {
            val propertySymbol = symbolTable.referenceProperty(descriptor.correspondingProperty)
            val property = irProviders_.getDeclaration(propertySymbol) as IrProperty
            return property.getter!!
        }
        if (createPropertyIfNeeded && descriptor is PropertySetterDescriptor) {
            val propertySymbol = symbolTable.referenceProperty(descriptor.correspondingProperty)
            val property = irProviders_.getDeclaration(propertySymbol) as IrProperty
            return property.setter!!
        }

        val origin =
            if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
                IrDeclarationOrigin.FAKE_OVERRIDE
            else computeOrigin(descriptor)
        return symbolTable.declareSimpleFunction(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            origin,
            descriptor.original
        ) {
            IrLazyFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, it, this, typeTranslator, irDeclarationFactory)
        }
    }

    internal fun generateConstructorStub(descriptor: ClassConstructorDescriptor): IrConstructor {
        val referenced = symbolTable.referenceConstructor(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }

        val origin = computeOrigin(descriptor)
        return symbolTable.declareConstructor(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor.original
        ) {
            IrLazyConstructor(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, it, this, typeTranslator, irDeclarationFactory)
        }
    }

    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    internal fun generateValueParameterStub(descriptor: ValueParameterDescriptor): IrValueParameter {
        return irDeclarationFactory.createValueParameter(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, computeOrigin(descriptor),
            descriptor, descriptor.type.toIrType(), descriptor.varargElementType?.toIrType()
        ).also { irValueParameter ->
            if (descriptor.declaresDefaultValue()) {
                irValueParameter.defaultValue =
                    IrExpressionBodyImpl(
                        IrErrorExpressionImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, descriptor.type.toIrType(),
                            "Stub expression for default value of ${descriptor.name}"
                        )
                    )
            }
        }
    }

    internal fun generateClassStub(descriptor: ClassDescriptor): IrClass {
        val referenceClass = symbolTable.referenceClass(descriptor)
        if (referenceClass.isBound) {
            return referenceClass.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.declareClass(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor) {
            IrLazyClass(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, it, this, typeTranslator, irDeclarationFactory)
        }
    }

    internal fun generateEnumEntryStub(descriptor: ClassDescriptor): IrEnumEntry {
        val referenced = symbolTable.referenceEnumEntry(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.declareEnumEntry(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor) {
            IrLazyEnumEntryImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, it, this, typeTranslator, irDeclarationFactory)
        }
    }

    internal fun generateOrGetTypeParameterStub(descriptor: TypeParameterDescriptor): IrTypeParameter {
        val referenced = symbolTable.referenceTypeParameter(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.declareGlobalTypeParameter(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor) {
            IrLazyTypeParameter(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, it, this, typeTranslator, irDeclarationFactory)
        }
    }

    internal fun generateOrGetScopedTypeParameterStub(descriptor: TypeParameterDescriptor): IrTypeParameter {
        val referenced = symbolTable.referenceTypeParameter(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.declareScopedTypeParameter(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor) {
            IrLazyTypeParameter(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, it, this, typeTranslator, irDeclarationFactory)
        }
    }

    internal fun generateTypeAliasStub(descriptor: TypeAliasDescriptor): IrTypeAlias {
        val referenced = symbolTable.referenceTypeAlias(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.declareTypeAlias(descriptor) {
            IrLazyTypeAlias(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, it.descriptor.name, it.descriptor.visibility, it.descriptor.isActual,
                this, typeTranslator, irDeclarationFactory
            )
        }
    }
}
