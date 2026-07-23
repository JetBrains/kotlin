/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jklib


import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideClassFilter
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JKlibIrLinker(
    module: ModuleDescriptor,
    configuration: CompilerConfiguration,
    symbolTable: SymbolTable,
    val descriptorMangler: JKlibDescriptorMangler,
    private val typeSystemContextFactory: (IrBuiltIns) -> IrTypeSystemContext,
    private val externalOverridabilityConditions: List<IrExternalOverridabilityCondition>,
) : KotlinIrLinker(module, configuration, symbolTable, emptyList()) {
    lateinit var stubGenerator: DeclarationStubGenerator
    override val returnUnboundSymbolsIfSignatureNotFound
        get() = false

    private val javaName = Name.identifier("java")

    private fun resolveMappedBuiltInSymbol(
        idSig: IdSignature,
        mappedSig: IdSignature,
        symbolKind: BinarySymbolData.SymbolKind,
    ): IrSymbol? {
        if (symbolKind == BinarySymbolData.SymbolKind.CLASS_SYMBOL && mappedSig != idSig && mappedSig is IdSignature.CommonSignature) {
            val fqName = if (idSig is IdSignature.CommonSignature) {
                if (idSig.packageFqName.isEmpty()) FqName(idSig.declarationFqName) else FqName("${idSig.packageFqName}.${idSig.declarationFqName}")
            } else {
                if (mappedSig.packageFqName.isEmpty()) FqName(mappedSig.declarationFqName) else FqName("${mappedSig.packageFqName}.${mappedSig.declarationFqName}")
            }
            return JKlibSignatureMapper.getBuiltInClassSymbolForMappedJavaClass(fqName, stubGenerator.irBuiltIns)
        }
        return null
    }

    private fun DeclarationDescriptor.isJavaDescriptor(): Boolean {
        if (this is PackageFragmentDescriptor) {
            return this is LazyJavaPackageFragment || fqName.startsWith(javaName)
        }

        return this is JavaClassDescriptor || this is JavaCallableMemberDescriptor || (containingDeclaration?.isJavaDescriptor() == true)
    }

    override fun platformSpecificSymbol(symbol: IrSymbol): Boolean {
        return symbol.descriptor.isJavaDescriptor()
    }

    override val irMangler: KotlinMangler.IrMangler = JKlibIrMangler()

    override val fakeOverrideBuilder = IrLinkerFakeOverrideProvider(
        linker = this,
        symbolTable = symbolTable,
        mangler = irMangler,
        friendModules = emptyMap(),
        partialLinkageSupport = partialLinkageSupport,
        // Do not construct fake overrides for Java classes. These classes are created with the
        // stub generator and are already complete. Building fake overrides for them will throw
        // an IllegalStateException as class declarations symbols are already bound
        // Note: We use an origin check instead of `clazz.isFromJava()` because parents might
        // not be initialized yet, and `isFromJava()` attempts to access the parent.
        // TODO(KT-86172): Investigate the issue around property fake override and remove this filter.
        platformSpecificClassFilter = object : FakeOverrideClassFilter {
            override fun needToConstructFakeOverrides(clazz: IrClass): Boolean =
                clazz.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        },
        externalOverridabilityConditions = externalOverridabilityConditions
    )

    override fun createTypeSystemContext(irBuiltIns: IrBuiltIns): IrTypeSystemContext =
        typeSystemContextFactory(irBuiltIns)

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean =
        moduleDescriptor === moduleDescriptor.builtIns.builtInsModule

    override fun createModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: KotlinLibrary?,
        strategyResolver: (String) -> DeserializationStrategy,
    ): IrModuleDeserializer {
        if (klib == null) {
            return MetadataJVMModuleDeserializer(moduleDescriptor)
        }

        val libraryAbiVersion = klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT
        return JKlibModuleDeserializer(
            moduleDescriptor,
            klib,
            strategyResolver,
            libraryAbiVersion,
        )
    }

    private fun declareJavaFieldStub(symbol: IrFieldSymbol): IrField {
        return with(stubGenerator) {
            val old = stubGenerator.unboundSymbolGeneration
            try {
                stubGenerator.unboundSymbolGeneration = true
                generateFieldStub(symbol.descriptor)
            } finally {
                stubGenerator.unboundSymbolGeneration = old
            }
        }
    }

    private inner class MetadataJVMModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
    ) : IrModuleDeserializer(moduleDescriptor, KotlinAbiVersion.CURRENT) {
        override val klib: KotlinLibrary get() = error("'klib' is not available for ${this::class.java}")

        override fun contains(idSig: IdSignature): Boolean = resolveDescriptor(idSig) != null

        override fun getDefinedPackageNames(): Set<FqName>? = null

        private val descriptorFinder = DescriptorByIdSignatureFinderImpl(
            moduleDescriptor,
            descriptorMangler,
            DescriptorByIdSignatureFinderImpl.LookupMode.MODULE_ONLY,
        )

        private fun resolveDescriptor(idSig: IdSignature): DeclarationDescriptor? = descriptorFinder.findDescriptorBySignature(idSig)

        override fun tryDeserializeIrSymbol(
            idSig: IdSignature,
            symbolKind: BinarySymbolData.SymbolKind,
        ): IrSymbol? {
            val mappedSig = JKlibSignatureMapper.mapJavaSignatureToKotlinSignature(idSig)
            resolveMappedBuiltInSymbol(idSig, mappedSig, symbolKind)?.let { return it }

            val descriptor = resolveDescriptor(idSig) ?: resolveDescriptor(mappedSig) ?: return null

            if (symbolKind == BinarySymbolData.SymbolKind.CLASS_SYMBOL && descriptor is ClassDescriptor) {
                val fqName = descriptor.fqNameOrNull()
                if (fqName != null) {
                    val symbol = JKlibSignatureMapper.getBuiltInClassSymbolForMappedJavaClass(fqName, stubGenerator.irBuiltIns)
                    if (symbol != null) return symbol
                }
            }

            val declaration = stubGenerator.run {
                when (symbolKind) {
                    BinarySymbolData.SymbolKind.CLASS_SYMBOL -> generateClassStub(descriptor as ClassDescriptor)
                    BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> generatePropertyStub(descriptor as PropertyDescriptor)
                    BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> generateFunctionStub(descriptor as FunctionDescriptor)
                    BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> generateConstructorStub(descriptor as ClassConstructorDescriptor)
                    BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> generateEnumEntryStub(descriptor as ClassDescriptor)
                    BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> generateTypeAliasStub(descriptor as TypeAliasDescriptor)
                    BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL -> generateFieldStub(descriptor as PropertyDescriptor)
                    else -> error("Unexpected type $symbolKind for sig $idSig")
                }
            }

            return declaration.symbol
        }

        override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing = error("No descriptor found for $idSig")

        override fun declareIrSymbol(symbol: IrSymbol) {
            val descriptor = symbol.descriptor
            if (descriptor is ClassDescriptor) {
                val fqName = descriptor.fqNameOrNull()
                if (fqName != null && JKlibSignatureMapper.isMappedJavaPlatformClass(fqName)) {
                    return
                }
            }
            if (symbol is IrFieldSymbol) {
                declareJavaFieldStub(symbol)
            } else {
                stubGenerator.generateMemberStub(symbol.descriptor)
            }
        }

        override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor)

        override val kind
            get() = IrModuleDeserializerKind.SYNTHETIC
    }
    private inner class JKlibModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: KotlinLibrary,
        strategyResolver: (String) -> DeserializationStrategy,
        libraryAbiVersion: KotlinAbiVersion,
    ) : BasicIrModuleDeserializer(
        this,
        moduleDescriptor,
        klib,
        strategyResolver,
        libraryAbiVersion,
    ) {

        private val descriptorByIdSignatureFinder = DescriptorByIdSignatureFinderImpl(
            moduleDescriptor,
            descriptorMangler,
            DescriptorByIdSignatureFinderImpl.LookupMode.MODULE_ONLY,
        )

        private val deserializedSymbols = mutableMapOf<IdSignature, IrSymbol>()

        override fun contains(idSig: IdSignature): Boolean =
            super.contains(idSig) || descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) != null

        override fun getDefinedPackageNames(): Set<FqName> = getPackagesFqNames(moduleDescriptor)

        private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
            val result = mutableSetOf<FqName>()
            val packageFragmentProvider = (module as ModuleDescriptorImpl).packageFragmentProviderForModuleContentWithoutDependencies

            fun getSubPackages(fqName: FqName) {
                if (!packageFragmentProvider.isEmpty(fqName))
                    result += fqName
                val subPackages = packageFragmentProvider.getSubPackagesOf(fqName) { true }
                subPackages.forEach { getSubPackages(it) }
            }

            getSubPackages(FqName.ROOT)
            return result
        }


        override fun tryDeserializeIrSymbol(
            idSig: IdSignature,
            symbolKind: BinarySymbolData.SymbolKind,
        ): IrSymbol? {
            deserializedSymbols[idSig]?.let { return it }
            val mappedSig = JKlibSignatureMapper.mapJavaSignatureToKotlinSignature(idSig)
            if (mappedSig != idSig) {
                deserializedSymbols[mappedSig]?.let { return it }
            }

            resolveMappedBuiltInSymbol(idSig, mappedSig, symbolKind)?.let { return it }

            super.tryDeserializeIrSymbol(mappedSig, symbolKind)?.let { return it }
            if (mappedSig != idSig) {
                super.tryDeserializeIrSymbol(idSig, symbolKind)?.let { return it }
            }

            val descriptor = descriptorByIdSignatureFinder.findDescriptorBySignature(mappedSig)
                ?: descriptorByIdSignatureFinder.findDescriptorBySignature(idSig)
                ?: return null

            if (symbolKind == BinarySymbolData.SymbolKind.CLASS_SYMBOL && descriptor is ClassDescriptor) {
                val fqName = descriptor.fqNameOrNull()
                if (fqName != null) {
                    val symbol = JKlibSignatureMapper.getBuiltInClassSymbolForMappedJavaClass(fqName, stubGenerator.irBuiltIns)
                    if (symbol != null) {
                        deserializedSymbols[idSig] = symbol
                        deserializedSymbols[mappedSig] = symbol
                        return symbol
                    }
                }
            }

            val symbol = (stubGenerator.generateMemberStub(descriptor) as IrSymbolOwner).symbol
            deserializedSymbols[idSig] = symbol
            deserializedSymbols[mappedSig] = symbol
            return symbol
        }
    }

    override fun createCurrentModuleDeserializer(
        moduleFragment: IrModuleFragment,
    ): IrModuleDeserializer = JvmCurrentModuleDeserializer(moduleFragment)

    private inner class JvmCurrentModuleDeserializer(
        moduleFragment: IrModuleFragment,
    ) : CurrentModuleDeserializer(moduleFragment) {
        override fun declareIrSymbol(symbol: IrSymbol) {
            val descriptor = symbol.descriptor

            if (descriptor is ClassDescriptor) {
                val fqName = descriptor.fqNameOrNull()
                if (fqName != null && JKlibSignatureMapper.isMappedJavaPlatformClass(fqName)) {
                    return
                }
            }

            if (descriptor.isJavaDescriptor()) {
                // Wrap java declaration with lazy ir
                if (symbol is IrFieldSymbol) {
                    declareJavaFieldStub(symbol)
                } else {
                    stubGenerator.generateMemberStub(descriptor)
                }
                return
            }

            if (descriptor.isCleanDescriptor()) {
                stubGenerator.generateMemberStub(descriptor)
                return
            }

            super.declareIrSymbol(symbol)
        }
    }
}

private fun DeclarationDescriptor.isCleanDescriptor(): Boolean {
    if (this is PropertyAccessorDescriptor) return correspondingProperty.isCleanDescriptor()
    return this is DeserializedDescriptor
}
