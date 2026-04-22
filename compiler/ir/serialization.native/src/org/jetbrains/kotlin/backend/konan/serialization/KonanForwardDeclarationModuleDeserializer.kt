/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializerKind
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.defaultTypeWithoutArguments
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.metadata.impl.isForwardDeclarationModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind

internal class KonanForwardDeclarationModuleDeserializer(
    moduleDescriptor: ModuleDescriptor,
    private val linker: KonanIrLinker,
) : IrModuleDeserializer(moduleDescriptor, KotlinAbiVersion.Companion.CURRENT) {
    init {
        require(moduleDescriptor.isForwardDeclarationModule)
    }

    override val klib get() = error("'klib' is not available for ${this::class.java}")
    override val kind get() = IrModuleDeserializerKind.SYNTHETIC
    override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor)
    private val symbolTable = linker.symbolTable
    private val declaredClasses = mutableMapOf<IdSignature.CommonSignature, IrClass?>()
    private val declaredPackageFragments = mutableMapOf<FqName, IrExternalPackageFragment>()

    override fun contains(idSig: IdSignature): Boolean =
        idSig is IdSignature.CommonSignature && idSig.packageFqName() in NativeForwardDeclarationKind.packageFqNameToKind

    override fun getDefinedPackageNames(): Set<FqName> =
        NativeForwardDeclarationKind.entries.map { it.packageFqName }.toSet()

    override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
        require(symbolKind == BinarySymbolData.SymbolKind.CLASS_SYMBOL) {
            "Only class could be a Forward declaration $idSig (kind $symbolKind)"
        }

        return declaredClasses.computeIfAbsent(idSig as IdSignature.CommonSignature) {
            buildForwardDeclarationClassStub(it)
        }?.symbol
    }

    private fun buildForwardDeclarationClassStub(signature: IdSignature.CommonSignature): IrClass? {
        val kind = NativeForwardDeclarationKind.packageFqNameToKind[signature.packageFqName()] ?: return null
        val clazz = symbolTable.declareClass(signature, { IrClassSymbolImpl(signature = signature) }) { symbol ->
            IrFactoryImpl.createClass(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                symbol = symbol,
                name = Name.identifier(signature.declarationFqName),
                origin = FORWARD_DECLARATION_ORIGIN,
                kind = kind.classKind,
                visibility = DescriptorVisibilities.PUBLIC,
                modality = Modality.FINAL,
                isExternal = false,
                isCompanion = false,
                isInner = false,
                isData = false,
                isValue = false,
                isExpect = false,
                isFun = false,
                hasEnumEntries = false,
                source = SourceElement.NO_SOURCE,
            )
        }
        clazz.createThisReceiverParameter()

        val superClassSignature = IdSignature.CommonSignature(
            kind.superClassId.packageFqName.asString(),
            kind.superClassId.relativeClassName.asString(),
            null, 0, null
        )
        val superClassSymbol = linker.deserializeOrReturnUnboundIrSymbolIfPartialLinkageEnabled(
            superClassSignature, BinarySymbolData.SymbolKind.CLASS_SYMBOL, this
        ) as IrClassSymbol
        clazz.superTypes = listOf(superClassSymbol.defaultTypeWithoutArguments)

        val irPackage = getOrCreateContainingPackage(kind.packageFqName)
        irPackage.addChild(clazz)
        clazz.parent = irPackage

        return clazz
    }

    private fun getOrCreateContainingPackage(packageFqName: FqName): IrExternalPackageFragment {
        return declaredPackageFragments.computeIfAbsent(packageFqName) {
            val descriptor = EmptyPackageFragmentDescriptor(moduleDescriptor, packageFqName)
            IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(descriptor), packageFqName)
        }
    }

    override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing = error("No forward declaration found for $idSig")

    companion object {
        private val FORWARD_DECLARATION_ORIGIN by IrDeclarationOriginImpl.Regular
    }
}
