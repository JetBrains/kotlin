/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializerKind
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinAbiVersion


class WebLazyIrModuleDeserializer(
    moduleDescriptor: ModuleDescriptor,
    libraryAbiVersion: KotlinAbiVersion,
    private val builtIns: IrBuiltIns,
    private val stubGenerator: DeclarationStubGenerator
) : IrModuleDeserializer(moduleDescriptor, libraryAbiVersion) {
    private val dependencies = emptyList<IrModuleDeserializer>()

    // TODO: implement proper check whether `idSig` belongs to this module
    override fun contains(idSig: IdSignature): Boolean = true

    private val descriptorFinder = DescriptorByIdSignatureFinderImpl(moduleDescriptor, WebManglerDesc)

    override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
        val descriptor = descriptorFinder.findDescriptorBySignature(idSig) ?: return null

        val declaration = stubGenerator.run {
            when (symbolKind) {
                BinarySymbolData.SymbolKind.CLASS_SYMBOL -> generateClassStub(descriptor as ClassDescriptor)
                BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> generatePropertyStub(descriptor as PropertyDescriptor)
                BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> generateFunctionStub(descriptor as FunctionDescriptor)
                BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> generateConstructorStub(descriptor as ClassConstructorDescriptor)
                BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> generateEnumEntryStub(descriptor as ClassDescriptor)
                BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> generateTypeAliasStub(descriptor as TypeAliasDescriptor)
                else -> error("Unexpected type $symbolKind for sig $idSig")
            }
        }

        return declaration.symbol
    }

    override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing = error("No descriptor found for $idSig")

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun declareIrSymbol(symbol: IrSymbol) {
        if (symbol is IrFieldSymbol) {
            declareFieldStub(symbol)
        } else {
            stubGenerator.generateMemberStub(symbol.descriptor)
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun declareFieldStub(symbol: IrFieldSymbol): IrField {
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


    override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, builtIns, emptyList())
    override val moduleDependencies: Collection<IrModuleDeserializer> = dependencies

    override val kind get() = IrModuleDeserializerKind.SYNTHETIC
}

