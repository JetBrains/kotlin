/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.impl.*

class ICData(val icData: List<SerializedIrFile>, val containsErrorCode: Boolean)

class ICKotlinLibrary(private val icData: List<SerializedIrFile>) : IrLibrary {
    override val dataFlowGraph: ByteArray? = null

    private inline fun <K, R : IrTableReader<K>> Array<R?>.itemBytes(fileIndex: Int, key: K, factory: () -> R): ByteArray {
        val reader = this[fileIndex] ?: factory().also { this[fileIndex] = it }

        return reader.tableItemBytes(key)
    }

    private inline fun <R : IrArrayReader> Array<R?>.itemBytes(fileIndex: Int, index: Int, factory: () -> R): ByteArray {
        val reader = this[fileIndex] ?: factory().also { this[fileIndex] = it }

        return reader.tableItemBytes(index)
    }

    private val indexedDeclarations = arrayOfNulls<DeclarationIrTableMemoryReader>(icData.size)
    private val indexedTypes = arrayOfNulls<IrArrayMemoryReader>(icData.size)
    private val indexedSignatures = arrayOfNulls<IrArrayMemoryReader>(icData.size)
    private val indexedStrings = arrayOfNulls<IrArrayMemoryReader>(icData.size)
    private val indexedBodies = arrayOfNulls<IrArrayMemoryReader>(icData.size)

    override fun irDeclaration(index: Int, fileIndex: Int): ByteArray =
        indexedDeclarations.itemBytes(fileIndex, DeclarationId(index)) {
            DeclarationIrTableMemoryReader(icData[fileIndex].declarations)
        }

    override fun type(index: Int, fileIndex: Int): ByteArray =
        indexedTypes.itemBytes(fileIndex, index) {
            IrArrayMemoryReader(icData[fileIndex].types)
        }

    override fun signature(index: Int, fileIndex: Int): ByteArray =
        indexedSignatures.itemBytes(fileIndex, index) {
            IrArrayMemoryReader(icData[fileIndex].signatures)
        }

    override fun string(index: Int, fileIndex: Int): ByteArray =
        indexedStrings.itemBytes(fileIndex, index) {
            IrArrayMemoryReader(icData[fileIndex].strings)
        }

    override fun body(index: Int, fileIndex: Int): ByteArray =
        indexedBodies.itemBytes(fileIndex, index) {
            IrArrayMemoryReader(icData[fileIndex].bodies)
        }

    override fun file(index: Int): ByteArray = icData[index].fileData

    override fun fileCount(): Int = icData.size
}

class CurrentModuleWithICDeserializer(
    private val delegate: IrModuleDeserializer,
    private val symbolTable: SymbolTable,
    private val irBuiltIns: IrBuiltIns,
    icData: List<SerializedIrFile>,
    icReaderFactory: (IrLibrary) -> IrModuleDeserializer) :
    IrModuleDeserializer(delegate.moduleDescriptor) {

    private val dirtyDeclarations = mutableMapOf<IdSignature, IrSymbol>()
    private val icKlib = ICKotlinLibrary(icData)

    private val icDeserializer: IrModuleDeserializer = icReaderFactory(icKlib)

    override fun contains(idSig: IdSignature): Boolean {
        return idSig in dirtyDeclarations || idSig.topLevelSignature() in icDeserializer || idSig in delegate
    }

    override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        dirtyDeclarations[idSig]?.let { return it }

        if (idSig.topLevelSignature() in icDeserializer) return icDeserializer.deserializeIrSymbol(idSig, symbolKind)

        return delegate.deserializeIrSymbol(idSig, symbolKind)
    }

    override fun addModuleReachableTopLevel(idSig: IdSignature) {
        assert(idSig in icDeserializer)
        icDeserializer.addModuleReachableTopLevel(idSig)
    }

    override fun deserializeReachableDeclarations() {
        icDeserializer.deserializeReachableDeclarations()
    }

    private fun DeclarationDescriptor.isDirtyDescriptor(): Boolean {
        if (this is PropertyAccessorDescriptor) return correspondingProperty.isDirtyDescriptor()
        // Since descriptors for FO methods of `kotlin.Any` (toString, equals, hashCode) are Deserialized even in
        // dirty files make test more precise checking containing declaration for non-static members
        if (this is CallableMemberDescriptor && dispatchReceiverParameter != null) {
            return containingDeclaration.isDirtyDescriptor()
        }
        return this !is DeserializedDescriptor
    }

    override fun init(delegate: IrModuleDeserializer) {
        val knownBuiltIns = irBuiltIns.knownBuiltins.map { (it as IrSymbolOwner).symbol }.toSet()
        symbolTable.forEachPublicSymbol {
            if (it.descriptor.isDirtyDescriptor()) { // public && non-deserialized should be dirty symbol
                if (it !in knownBuiltIns) {
                    dirtyDeclarations[it.signature!!] = it
                }
            }
        }

        icDeserializer.init(delegate)
    }

    override fun toString(): String = "Incremental Cache Klib"

    override val klib: IrLibrary
        get() = icDeserializer.klib

    override val moduleFragment: IrModuleFragment
        get() = delegate.moduleFragment
    override val moduleDependencies: Collection<IrModuleDeserializer>
        get() = delegate.moduleDependencies
    override val isCurrent: Boolean
        get() = delegate.isCurrent

    override fun fileDeserializers(): Collection<IrFileDeserializer> {
        return delegate.fileDeserializers()
    }
}