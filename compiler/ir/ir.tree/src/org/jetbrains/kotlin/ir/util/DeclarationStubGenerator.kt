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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class DeclarationStubGenerator(
    val moduleDescriptor: ModuleDescriptor,
    val symbolTable: SymbolTable,
    val irBuiltIns: IrBuiltIns,
    val extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY,
) : IrProvider {
    protected val lazyTable = symbolTable.lazyWrapper

    init {
        extensions.registerDeclarations(symbolTable)
    }

    val lock: IrLock
        get() = symbolTable.lock

    var unboundSymbolGeneration: Boolean
        get() = lazyTable.stubGenerator != null
        set(value) {
            lazyTable.stubGenerator = if (value) this else null
        }

    abstract val typeTranslator: TypeTranslator

    private val facadeClassMap = mutableMapOf<DeserializedContainerSource, IrClass?>()

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {
        // Special case: generating field for an already generated property.
        // -- this used to be done via a trick (ab)using WrappedDescriptors. Not clear if this code should ever be invoked,
        // and if so, how it can be reproduced without them.
//        if (symbol is IrFieldSymbol && (symbol.descriptor as? WrappedPropertyDescriptor)?.isBound() == true) {
//            return generateStubBySymbol(symbol, symbol.descriptor)
//        }
        val descriptor = if (!symbol.hasDescriptor)
            findDescriptorBySignature(
                symbol.signature
                    ?: error("Symbol is not public API. Expected signature for symbol: ${symbol.descriptor}")
            )
        else
            symbol.descriptor
        if (descriptor == null) return null
        return generateStubBySymbol(symbol, descriptor)
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
        val containerSource = extensions.getContainerSource(directMember) ?: return null
        return facadeClassMap.getOrPut(containerSource) {
            extensions.generateFacadeClass(symbolTable.irFactory, containerSource, this)?.also { facade ->
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

    fun generatePropertyStub(descriptor: PropertyDescriptor): IrProperty {
        val referenced = symbolTable.referenceProperty(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }

        val origin = computeOrigin(descriptor)
        return symbolTable.declareProperty(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor.original, descriptor.isDelegated
        ) {
            IrLazyProperty(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name, descriptor.visibility, descriptor.modality,
                descriptor.isVar, descriptor.isConst, descriptor.isLateInit,
                descriptor.isDelegated, descriptor.isEffectivelyExternal(), descriptor.isExpect,
                isFakeOverride = (origin == IrDeclarationOrigin.FAKE_OVERRIDE)
                        || descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                stubGenerator = this, typeTranslator,
            )
        }
    }

    fun generateFieldStub(descriptor: PropertyDescriptor): IrField {
        val referenced = symbolTable.referenceField(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }

        return symbolTable.declareField(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, computeOrigin(descriptor), descriptor.original, descriptor.type.toIrType()
        ) {
            IrLazyField(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, computeOrigin(descriptor),
                it, descriptor,
                descriptor.name, descriptor.visibility,
                isFinal = !descriptor.isVar,
                isExternal = descriptor.isEffectivelyExternal(),
                isStatic = (descriptor.dispatchReceiverParameter == null),
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
            val property = generatePropertyStub(descriptor.correspondingProperty)
            return property.getter!!
        }
        if (createPropertyIfNeeded && descriptor is PropertySetterDescriptor) {
            val property = generatePropertyStub(descriptor.correspondingProperty)
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
                isFakeOverride = (origin == IrDeclarationOrigin.FAKE_OVERRIDE),
                isOperator = descriptor.isOperator, isInfix = descriptor.isInfix,
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
            descriptor.original
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

    internal fun generateValueParameterStub(descriptor: ValueParameterDescriptor): IrValueParameter = with(descriptor) {
        IrLazyValueParameter(UNDEFINED_OFFSET, UNDEFINED_OFFSET, computeOrigin(this), IrValueParameterSymbolImpl(this), this, name, index,
                             type.toIrType(), varargElementType?.toIrType(), isCrossinline, isNoinline, isHidden = false, isAssignable = false, this@DeclarationStubGenerator, typeTranslator)
        .also { irValueParameter ->
            if (descriptor.declaresDefaultValue()) {
                irValueParameter.defaultValue = irValueParameter.createStubDefaultValue()
            }
        }
    }

    // in IR Generator enums also have special handling, but here we have not enough data for it
    // probably, that is not a problem, because you can't add new enum value to external module
    private fun getEffectiveModality(classDescriptor: ClassDescriptor): Modality =
        if (DescriptorUtils.isAnnotationClass(classDescriptor))
            Modality.OPEN
        else
            classDescriptor.modality

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
                descriptor.name, descriptor.kind, descriptor.visibility, getEffectiveModality(descriptor),
                isCompanion = descriptor.isCompanionObject,
                isInner = descriptor.isInner,
                isData = descriptor.isData,
                isExternal = descriptor.isEffectivelyExternal(),
                isInline = descriptor.isInlineClass(),
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
        val referenced = symbolTable.referenceScopedTypeParameter(descriptor)
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
            is IdSignature.CommonSignature -> findDescriptorForPublicSignature(signature)
            else -> error("only PublicSignature or AccessorSignature should reach this point, got $signature")
        }

    private fun findDescriptorForAccessorSignature(signature: IdSignature.AccessorSignature): DeclarationDescriptor? {
        val propertyDescriptor = findDescriptorBySignature(signature.propertySignature) as? PropertyDescriptor ?: return null
        val shortName = signature.accessorSignature.shortName
        return propertyDescriptor.accessors.singleOrNull { it.name.asString() == shortName }
    }

    private fun findDescriptorForPublicSignature(signature: IdSignature.CommonSignature): DeclarationDescriptor? {
        val packageDescriptor = moduleDescriptor.getPackage(signature.packageFqName())
        val nameSegments = signature.nameSegments
        val toplevelDescriptors = packageDescriptor.memberScope.getDescriptorsFiltered { name -> name.asString() == nameSegments.first() }
        val candidates = nameSegments.drop(1).fold(toplevelDescriptors) { acc, current ->
            acc.flatMap { container ->
                val classDescriptor = container as? ClassDescriptor ?: return@flatMap emptyList()
                classDescriptor.constructors.filter { it.name.asString() == current } +
                        classDescriptor.unsubstitutedMemberScope.getDescriptorsFiltered { name -> name.asString() == current }
            }
        }
        return candidates.firstOrNull { symbolTable.signaturer.composeSignature(it) == signature }
    }
}
