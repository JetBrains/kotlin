/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.lazy.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class DeclarationStubGenerator(
    val moduleDescriptor: ModuleDescriptor,
    val symbolTable: SymbolTable,
    languageVersionSettings: LanguageVersionSettings,
    val extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY
) : IrProvider {
    private val lazyTable = symbolTable.lazyWrapper

    var unboundSymbolGeneration: Boolean
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

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {
        // Special case: generating field for an already generated property.
        if (symbol is IrFieldSymbol && (symbol.descriptor as? WrappedPropertyDescriptor)?.isBound() == true) {
            return generateStubBySymbol(symbol, symbol.descriptor)
        }
        val descriptor = if (symbol.descriptor is WrappedDeclarationDescriptor<*>)
            findDescriptorBySignature(symbol.signature)
        else
            symbol.descriptor
        if (descriptor == null) return null
        return generateStubBySymbol(symbol, descriptor).also {
            symbol.descriptor.bind(it)
        }
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
            is TypeParameterDescriptor ->
                generateOrGetTypeParameterStub(descriptor)
            else ->
                throw AssertionError("Unexpected member descriptor: $descriptor")
        }

    private fun generateStubBySymbol(symbol: IrSymbol, descriptor: DeclarationDescriptor): IrDeclaration = when (symbol) {
        is IrFieldSymbol ->
            generateFieldStub(descriptor as PropertyDescriptor)
        is IrTypeParameterSymbol ->
            generateOrGetTypeParameterStub(descriptor as TypeParameterDescriptor)
        else ->
            generateMemberStub(descriptor)
    }

    private fun computeOrigin(descriptor: DeclarationDescriptor): IrDeclarationOrigin =
        extensions.computeExternalDeclarationOrigin(descriptor) ?: IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB

    fun generatePropertyStub(
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
            IrLazyProperty(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name, descriptor.visibility, descriptor.modality,
                descriptor.isVar, descriptor.isConst, descriptor.isLateInit,
                @Suppress("DEPRECATION") descriptor.isDelegated, descriptor.isEffectivelyExternal(), descriptor.isExpect,
                isFakeOverride = (origin == IrDeclarationOrigin.FAKE_OVERRIDE),
                stubGenerator = this, typeTranslator = typeTranslator, bindingContext = bindingContext
            )
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
            IrLazyField(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name, descriptor.visibility,
                isFinal = !descriptor.isVar,
                isExternal = descriptor.isEffectivelyExternal(),
                isStatic = (descriptor.dispatchReceiverParameter == null),
                isFakeOverride = (origin == IrDeclarationOrigin.FAKE_OVERRIDE),
                stubGenerator = this, typeTranslator = typeTranslator
            )
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
        return symbolTable.declareSimpleFunction(descriptor.original) {
            IrLazyFunction(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name, descriptor.visibility, descriptor.modality,
                descriptor.isInline, descriptor.isExternal, descriptor.isTailrec, descriptor.isSuspend, descriptor.isExpect,
                isFakeOverride = (origin == IrDeclarationOrigin.FAKE_OVERRIDE), isOperator = descriptor.isOperator,
                stubGenerator = this, typeTranslator = typeTranslator
            )
        }
    }

    fun generateConstructorStub(descriptor: ClassConstructorDescriptor): IrConstructor {
        val referenced = symbolTable.referenceConstructor(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }

        val origin = computeOrigin(descriptor)
        return symbolTable.declareConstructor(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor.original
        ) {
            IrLazyConstructor(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name, descriptor.visibility,
                descriptor.isInline, descriptor.isEffectivelyExternal(), descriptor.isPrimary, descriptor.isExpect,
                this, typeTranslator
            )
        }
    }

    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    internal fun generateValueParameterStub(descriptor: ValueParameterDescriptor): IrValueParameter {
        return IrValueParameterImpl(
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

    fun generateClassStub(descriptor: ClassDescriptor): IrClass {
        val referenceClass = symbolTable.referenceClass(descriptor)
        if (referenceClass.isBound) {
            return referenceClass.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.declareClass(descriptor) {
            IrLazyClass(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name, descriptor.kind, descriptor.visibility, descriptor.modality,
                isCompanion = descriptor.isCompanionObject,
                isInner = descriptor.isInner,
                isData = descriptor.isData,
                isExternal = descriptor.isEffectivelyExternal(),
                isInline = descriptor.isInline,
                isExpect = descriptor.isExpect,
                isFun = descriptor.isFun,
                stubGenerator = this,
                typeTranslator = typeTranslator
            )
        }
    }

    fun generateEnumEntryStub(descriptor: ClassDescriptor): IrEnumEntry {
        val referenced = symbolTable.referenceEnumEntry(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.declareEnumEntry(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor) {
            IrLazyEnumEntryImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                this, typeTranslator
            )
        }
    }

    internal fun generateOrGetTypeParameterStub(descriptor: TypeParameterDescriptor): IrTypeParameter {
        val referenced = symbolTable.referenceTypeParameter(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.declareGlobalTypeParameter(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor) {
            IrLazyTypeParameter(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name,
                descriptor.index,
                descriptor.isReified,
                descriptor.variance,
                this, typeTranslator
            )
        }
    }

    internal fun generateOrGetScopedTypeParameterStub(descriptor: TypeParameterDescriptor): IrTypeParameter {
        val referenced = symbolTable.referenceTypeParameter(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.declareScopedTypeParameter(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor) {
            IrLazyTypeParameter(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name,
                descriptor.index,
                descriptor.isReified,
                descriptor.variance,
                this, typeTranslator)
        }
    }

    fun generateTypeAliasStub(descriptor: TypeAliasDescriptor): IrTypeAlias {
        val referenced = symbolTable.referenceTypeAlias(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.declareTypeAlias(descriptor) {
            IrLazyTypeAlias(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name, descriptor.visibility, descriptor.isActual,
                this, typeTranslator
            )
        }
    }

    private fun findDescriptorBySignature(signature: IdSignature): DeclarationDescriptor? = when (signature) {
            is IdSignature.AccessorSignature -> findDescriptorForAccessorSignature(signature)
            is IdSignature.PublicSignature -> findDescriptorForPublicSignature(signature)
            else -> error("only PublicSignature or AccessorSignature should reach this point, got $signature")
        }

    private fun findDescriptorForAccessorSignature(signature: IdSignature.AccessorSignature): DeclarationDescriptor? {
        val propertyDescriptor = findDescriptorBySignature(signature.propertySignature) as? PropertyDescriptor ?: return null
        return propertyDescriptor.accessors.singleOrNull {
            it.name == signature.accessorSignature.declarationFqn.shortName()
        }
    }

    private fun findDescriptorForPublicSignature(signature: IdSignature.PublicSignature): DeclarationDescriptor? {
        val packageDescriptor = moduleDescriptor.getPackage(signature.packageFqName())
        val pathSegments = signature.declarationFqn.pathSegments()
        val toplevelDescriptors = packageDescriptor.memberScope.getContributedDescriptors { name -> name == pathSegments.first() }
            .filter { it.name == pathSegments.first() }
        val candidates = pathSegments.drop(1).fold(toplevelDescriptors) { acc, current ->
            acc.flatMap { container ->
                val classDescriptor = container as? ClassDescriptor ?: return@flatMap emptyList()
                val nextStepCandidates = classDescriptor.constructors +
                        classDescriptor.unsubstitutedMemberScope.getContributedDescriptors { name -> name == current }
                nextStepCandidates.filter { it.name == current }
            }
        }
        return candidates.firstOrNull { symbolTable.signaturer.composeSignature(it) == signature }
    }

    private fun DeclarationDescriptor.bind(declaration: IrDeclaration) {
        when (this) {
            is WrappedValueParameterDescriptor -> bind(declaration as IrValueParameter)
            is WrappedReceiverParameterDescriptor -> bind(declaration as IrValueParameter)
            is WrappedTypeParameterDescriptor -> bind(declaration as IrTypeParameter)
            is WrappedVariableDescriptor -> bind(declaration as IrVariable)
            is WrappedVariableDescriptorWithAccessor -> bind(declaration as IrLocalDelegatedProperty)
            is WrappedSimpleFunctionDescriptor -> bind(declaration as IrSimpleFunction)
            is WrappedClassConstructorDescriptor -> bind(declaration as IrConstructor)
            is WrappedClassDescriptor -> bind(declaration as IrClass)
            is WrappedEnumEntryDescriptor -> bind(declaration as IrEnumEntry)
            is WrappedPropertyDescriptor -> (declaration as? IrProperty)?.let { bind(it) }
            is WrappedTypeAliasDescriptor -> bind(declaration as IrTypeAlias)
            is WrappedFieldDescriptor -> bind(declaration as IrField)
            else -> {}
        }
    }
}
