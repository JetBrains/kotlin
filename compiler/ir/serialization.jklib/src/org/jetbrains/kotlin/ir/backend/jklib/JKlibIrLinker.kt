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
import org.jetbrains.kotlin.name.Name

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JKlibIrLinker(
    module: ModuleDescriptor,
    configuration: CompilerConfiguration,
    irBuiltIns: IrBuiltIns,
    symbolTable: SymbolTable,
    val stubGenerator: DeclarationStubGenerator,
    val descriptorMangler: JKlibDescriptorMangler,
) : KotlinIrLinker(module, configuration, irBuiltIns, symbolTable, emptyList()) {
    override val returnUnboundSymbolsIfSignatureNotFound
        get() = false

    private val javaName = Name.identifier("java")

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
    )

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

        override fun contains(idSig: IdSignature): Boolean = true

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
            val descriptor = resolveDescriptor(idSig) ?: return null

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
        override val klib: KotlinLibrary,
        strategyResolver: (String) -> DeserializationStrategy,
        libraryAbiVersion: KotlinAbiVersion,
    ) : BasicIrModuleDeserializer(
        this,
        moduleDescriptor,
        strategyResolver,
        libraryAbiVersion,
    ) {

        private val descriptorByIdSignatureFinder = DescriptorByIdSignatureFinderImpl(
            moduleDescriptor,
            descriptorMangler,
            DescriptorByIdSignatureFinderImpl.LookupMode.MODULE_ONLY,
        )

        private val deserializedSymbols = mutableMapOf<IdSignature, IrSymbol>()

        override fun tryDeserializeIrSymbol(
            idSig: IdSignature,
            symbolKind: BinarySymbolData.SymbolKind,
        ): IrSymbol? {
            super.tryDeserializeIrSymbol(idSig, symbolKind)?.let {
                return it
            }
            deserializedSymbols[idSig]?.let {
                return it
            }
            val descriptor = descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) ?: return null
            val symbol = (stubGenerator.generateMemberStub(descriptor) as IrSymbolOwner).symbol
            deserializedSymbols[idSig] = symbol
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
