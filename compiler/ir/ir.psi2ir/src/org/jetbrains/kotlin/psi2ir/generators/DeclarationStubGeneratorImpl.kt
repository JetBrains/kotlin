/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrProvider
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.lazy.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.isValueClass
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addIfNotNull

class DeclarationStubGeneratorImpl(
    moduleDescriptor: ModuleDescriptor,
    symbolTable: SymbolTable,
    irBuiltins: IrBuiltIns,
    override val descriptorFinder: DescriptorByIdSignatureFinder,
    extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY,
) : DeclarationStubGenerator(moduleDescriptor, symbolTable, irBuiltins, extensions) {
    private val lazyTable = symbolTable.lazyWrapper

    override val typeTranslator: TypeTranslator =
        TypeTranslatorImpl(
            lazyTable,
            irBuiltins.languageVersionSettings,
            moduleDescriptor,
            { LazyScopedTypeParametersResolver(lazyTable) },
            true,
            extensions
        )

    override var unboundSymbolGeneration: Boolean
        get() = lazyTable.stubGenerator != null
        set(value) {
            lazyTable.stubGenerator = if (value) this else null
        }

    private val facadeClassMap = mutableMapOf<DeserializedContainerSource, IrClass?>()

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {
        // Special case: generating field for an already generated property.
        // -- this used to be done via a trick (ab)using WrappedDescriptors. Not clear if this code should ever be invoked,
        // and if so, how it can be reproduced without them.
//        if (symbol is IrFieldSymbol && (symbol.descriptor as? WrappedPropertyDescriptor)?.isBound() == true) {
//            return generateStubBySymbol(symbol, symbol.descriptor)
//        }
        val descriptor = if (!symbol.hasDescriptor)
            descriptorFinder.findDescriptorBySignature(
                symbol.signature
                    ?: error("Symbol is not public API. Expected signature for symbol: ${symbol.descriptor}")
            )
        else
            symbol.descriptor
        if (descriptor == null) return null
        return generateStubBySymbol(symbol, descriptor)
    }

    override fun generateOrGetEmptyExternalPackageFragmentStub(descriptor: PackageFragmentDescriptor): IrExternalPackageFragment {
        val referenced = symbolTable.descriptorExtension.referenceExternalPackageFragment(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        return symbolTable.descriptorExtension.declareExternalPackageFragment(descriptor)
    }

    override fun generateOrGetFacadeClass(descriptor: DeclarationDescriptor): IrClass? {
        val directMember = (descriptor as? PropertyAccessorDescriptor)?.correspondingProperty ?: descriptor
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


    override fun generateMemberStub(descriptor: DeclarationDescriptor): IrDeclaration =
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

    override fun generatePropertyStub(descriptor: PropertyDescriptor): IrProperty {
        val referenced = symbolTable.descriptorExtension.referenceProperty(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }

        val origin = computeOrigin(descriptor)
        return symbolTable.descriptorExtension.declareProperty(descriptor.original) {
            IrLazyProperty(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name, descriptor.visibility, descriptor.modality,
                descriptor.isVar, descriptor.isConst, descriptor.isLateInit,
                descriptor.isDelegated, descriptor.isEffectivelyExternal(), descriptor.isExpect,
                isFakeOverride = (origin == IrDeclarationOrigin.FAKE_OVERRIDE)
                        || descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                stubGenerator = this, typeTranslator,
            ).generateParentDeclaration()
        }
    }

    override fun generateFieldStub(descriptor: PropertyDescriptor): IrField {
        val referenced = symbolTable.descriptorExtension.referenceField(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }

        return symbolTable.descriptorExtension.declareField(
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
            ).generateParentDeclaration()
        }
    }

    override fun generateFunctionStub(descriptor: FunctionDescriptor, createPropertyIfNeeded: Boolean): IrSimpleFunction {
        val referenced = symbolTable.descriptorExtension.referenceSimpleFunction(descriptor)
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
        return symbolTable.descriptorExtension.declareSimpleFunction(descriptor.original) {
            IrLazyFunction(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name, descriptor.visibility, descriptor.modality,
                descriptor.isInline, descriptor.isExternal, descriptor.isTailrec, descriptor.isSuspend, descriptor.isExpect,
                isFakeOverride = (origin == IrDeclarationOrigin.FAKE_OVERRIDE),
                isOperator = descriptor.isOperator, isInfix = descriptor.isInfix,
                stubGenerator = this, typeTranslator = typeTranslator
            ).generateParentDeclaration().also {
                it.parameters = it.createValueParameters()
            }
        }
    }

    override fun generateConstructorStub(descriptor: ClassConstructorDescriptor): IrConstructor {
        val referenced = symbolTable.descriptorExtension.referenceConstructor(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }

        val origin = computeOrigin(descriptor)
        return symbolTable.descriptorExtension.declareConstructor(
            descriptor.original
        ) {
            IrLazyConstructor(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name, descriptor.visibility,
                descriptor.isInline, descriptor.isEffectivelyExternal(), descriptor.isPrimary, descriptor.isExpect,
                this, typeTranslator
            ).generateParentDeclaration().also {
                it.parameters = it.createValueParameters()
            }
        }
    }

    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    private fun Psi2IrLazyFunctionBase.createValueParameters(): List<IrValueParameter> =
        typeTranslator.buildWithScope(this) {
            val result = arrayListOf<IrValueParameter>()
            result.addIfNotNull(createReceiverParameter(descriptor.dispatchReceiverParameter, IrParameterKind.DispatchReceiver))
            descriptor.contextReceiverParameters.mapIndexedTo(result) { i, contextReceiverParameter ->
                factory.createValueParameter(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    origin = origin,
                    kind = IrParameterKind.Context,
                    name = Name.identifier("contextReceiverParameter$i"),
                    type = contextReceiverParameter.type.toIrType(),
                    isAssignable = false,
                    symbol = IrValueParameterSymbolImpl(contextReceiverParameter),
                    varargElementType = null,
                    isCrossinline = false,
                    isNoinline = false,
                    isHidden = false,
                ).apply { parent = this@createValueParameters }
            }
            result.addIfNotNull(createReceiverParameter(descriptor.extensionReceiverParameter, IrParameterKind.ExtensionReceiver))
            descriptor.valueParameters.mapTo(result) {
                stubGenerator.generateValueParameterStub(it)
                    .apply { parent = this@createValueParameters }
            }
        }

    override fun generateValueParameterStub(descriptor: ValueParameterDescriptor): IrValueParameter = with(descriptor) {
        IrLazyValueParameter(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, computeOrigin(this),
            IrParameterKind.Regular,
            IrValueParameterSymbolImpl(this), this, name,
            type, varargElementType,
            isCrossinline = isCrossinline, isNoinline = isNoinline, isHidden = false, isAssignable = false,
            stubGenerator = this@DeclarationStubGeneratorImpl, typeTranslator = typeTranslator
        ).also { irValueParameter ->
            irValueParameter.kind = IrParameterKind.Regular
            if (descriptor.declaresDefaultValue()) {
                irValueParameter.defaultValue = irValueParameter.createStubDefaultValue()
            }
        }.generateParentDeclaration()
    }

    private fun Psi2IrLazyFunctionBase.createReceiverParameter(
        parameter: ReceiverParameterDescriptor?,
        kind: IrParameterKind,
    ): IrValueParameter? = typeTranslator.buildWithScope(this) {
        parameter?.generateReceiverParameterStub(kind)?.also {
            it.parent = this@createReceiverParameter
        }
    }

    // in IR Generator enums also have special handling, but here we have not enough data for it
    // probably, that is not a problem, because you can't add new enum value to external module
    private fun getEffectiveModality(classDescriptor: ClassDescriptor): Modality =
        if (DescriptorUtils.isAnnotationClass(classDescriptor))
            Modality.OPEN
        else
            classDescriptor.modality

    override fun generateClassStub(descriptor: ClassDescriptor): IrClass {
        val irClassSymbol = symbolTable.descriptorExtension.referenceClass(descriptor)
        if (irClassSymbol.isBound) {
            return irClassSymbol.owner
        }

        // `irClassSymbol` may have a different descriptor than `descriptor`. For example, a `SymbolTableWithBuiltInsDeduplication` might
        // return a built-in symbol that hasn't been bound yet (e.g. `OptIn`). If the stub generator calls `declareClass` with the incorrect
        // `descriptor`, a symbol created for `descriptor` will be bound, not the built-in symbol which should be. If `generateClassStub` is
        // called twice for such a `descriptor`, an exception will occur because `descriptor`'s symbol will already have been bound.
        //
        // Note as well that not all symbols have descriptors. For such symbols, the `descriptor` argument needs to be used.
        val targetDescriptor = if (irClassSymbol.hasDescriptor) irClassSymbol.descriptor else descriptor
        with(targetDescriptor) {
            val origin = computeOrigin(this)
            return symbolTable.descriptorExtension.declareClass(this) {
                IrLazyClass(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                    it, this,
                    name, kind, visibility, getEffectiveModality(this),
                    isCompanion = isCompanionObject,
                    isInner = isInner,
                    isData = isData,
                    isExternal = isEffectivelyExternal(),
                    isValue = isValueClass(),
                    isExpect = isExpect,
                    isFun = isFun,
                    hasEnumEntries = descriptor is DeserializedClassDescriptor && descriptor.hasEnumEntriesMetadataFlag,
                    stubGenerator = this@DeclarationStubGeneratorImpl,
                    typeTranslator = typeTranslator
                ).generateParentDeclaration()
            }
        }
    }

    override fun generateEnumEntryStub(descriptor: ClassDescriptor): IrEnumEntry {
        val referenced = symbolTable.descriptorExtension.referenceEnumEntry(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.descriptorExtension.declareEnumEntry(descriptor) {
            IrLazyEnumEntryImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                this, typeTranslator
            ).generateParentDeclaration()
        }
    }

    override fun generateOrGetTypeParameterStub(descriptor: TypeParameterDescriptor): IrTypeParameter {
        val referenced = symbolTable.descriptorExtension.referenceTypeParameter(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.descriptorExtension.declareGlobalTypeParameter(descriptor) {
            IrLazyTypeParameter(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name,
                descriptor.index,
                descriptor.isReified,
                descriptor.variance,
                this, typeTranslator
            ).generateParentDeclaration()
        }
    }

    override fun generateOrGetScopedTypeParameterStub(descriptor: TypeParameterDescriptor): IrTypeParameter {
        val referenced = symbolTable.descriptorExtension.referenceScopedTypeParameter(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.descriptorExtension.declareScopedTypeParameter(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor) {
            IrLazyTypeParameter(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name,
                descriptor.index,
                descriptor.isReified,
                descriptor.variance,
                this, typeTranslator
            ).generateParentDeclaration()
        }
    }

    override fun generateTypeAliasStub(descriptor: TypeAliasDescriptor): IrTypeAlias {
        val referenced = symbolTable.descriptorExtension.referenceTypeAlias(descriptor)
        if (referenced.isBound) {
            return referenced.owner
        }
        val origin = computeOrigin(descriptor)
        return symbolTable.descriptorExtension.declareTypeAlias(descriptor) {
            IrLazyTypeAlias(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
                it, descriptor,
                descriptor.name, descriptor.visibility, descriptor.isActual,
                this, typeTranslator
            ).generateParentDeclaration()
        }
    }

    private fun <E : IrLazyDeclarationBase> E.generateParentDeclaration(): E {
        val currentDescriptor = descriptor

        val containingDeclaration =
            ((currentDescriptor as? PropertyAccessorDescriptor)?.correspondingProperty ?: currentDescriptor).containingDeclaration

        parent = when (containingDeclaration) {
            is PackageFragmentDescriptor -> run {
                val parent = this.takeUnless { it is IrClass }?.let {
                    generateOrGetFacadeClass(descriptor)
                } ?: generateOrGetEmptyExternalPackageFragmentStub(containingDeclaration)
                assert(this !in parent.declarations)
                parent.declarations.add(this)
                parent
            }
            is ClassDescriptor -> generateClassStub(containingDeclaration)
            is FunctionDescriptor -> generateFunctionStub(containingDeclaration)
            is PropertyDescriptor -> generateFunctionStub(containingDeclaration.run { getter ?: setter!! })
            is TypeAliasDescriptor -> generateTypeAliasStub(containingDeclaration)
            else -> throw AssertionError("Package or class expected: $containingDeclaration; for $currentDescriptor")
        }

        return this
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
class DeclarationStubGeneratorForNotFoundClasses(
    private val stubGenerator: DeclarationStubGeneratorImpl,
) : IrProvider {

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {
        if (symbol.isBound) return null

        val classDescriptor = symbol.descriptor as? NotFoundClasses.MockClassDescriptor
            ?: return null
        return stubGenerator.generateClassStub(classDescriptor)
    }
}
