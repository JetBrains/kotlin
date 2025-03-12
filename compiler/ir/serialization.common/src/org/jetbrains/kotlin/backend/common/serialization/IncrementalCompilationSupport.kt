/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.isPublicApi
import org.jetbrains.kotlin.ir.util.DelicateSymbolTableApi
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.impl.*

class ICData(val icData: List<SerializedIrFile>, val containsErrorCode: Boolean)

class ICKotlinLibrary(private val icData: List<SerializedIrFile>) : IrLibrary {
    override val hasIr get() = true
    override val hasFileEntriesTable get() = true

    private inline fun Array<DeclarationIdTableReader?>.itemBytes(fileIndex: Int, key: DeclarationId, factory: () -> DeclarationIdTableReader): ByteArray {
        val reader = this[fileIndex] ?: factory().also { this[fileIndex] = it }

        return reader.tableItemBytes(key)
    }

    private inline fun Array<IrArrayReader?>.itemBytes(fileIndex: Int, index: Int, factory: () -> IrArrayReader): ByteArray {
        val reader = this[fileIndex] ?: factory().also { this[fileIndex] = it }

        return reader.tableItemBytes(index)
    }

    private inline fun Array<IrArrayReader?>.itemNullableBytes(fileIndex: Int, index: Int, factory: () -> IrArrayReader?): ByteArray? {
        val reader = this[fileIndex] ?: factory().also { this[fileIndex] = it }

        return reader?.tableItemBytes(index)
    }

    private val indexedDeclarations = arrayOfNulls<DeclarationIdTableReader>(icData.size)
    private val indexedTypes = arrayOfNulls<IrArrayReader>(icData.size)
    private val indexedSignatures = arrayOfNulls<IrArrayReader>(icData.size)
    private val indexedStrings = arrayOfNulls<IrArrayReader>(icData.size)
    private val indexedDebugInfos = arrayOfNulls<IrArrayReader?>(icData.size)
    private val indexedBodies = arrayOfNulls<IrArrayReader>(icData.size)
    private val indexedFileEntries = arrayOfNulls<IrArrayReader>(icData.size)

    override fun irDeclaration(index: Int, fileIndex: Int): ByteArray =
        indexedDeclarations.itemBytes(fileIndex, DeclarationId(index)) {
            DeclarationIdTableReader(icData[fileIndex].declarations)
        }

    override fun type(index: Int, fileIndex: Int): ByteArray =
        indexedTypes.itemBytes(fileIndex, index) {
            IrArrayReader(icData[fileIndex].types)
        }

    override fun signature(index: Int, fileIndex: Int): ByteArray =
        indexedSignatures.itemBytes(fileIndex, index) {
            IrArrayReader(icData[fileIndex].signatures)
        }

    override fun string(index: Int, fileIndex: Int): ByteArray =
        indexedStrings.itemBytes(fileIndex, index) {
            IrArrayReader(icData[fileIndex].strings)
        }

    override fun body(index: Int, fileIndex: Int): ByteArray =
        indexedBodies.itemBytes(fileIndex, index) {
            IrArrayReader(icData[fileIndex].bodies)
        }

    override fun debugInfo(index: Int, fileIndex: Int): ByteArray? =
        indexedDebugInfos.itemNullableBytes(fileIndex, index) {
            icData[fileIndex].debugInfo?.let { IrArrayReader(it) }
        }

    override fun fileEntry(index: Int, fileIndex: Int): ByteArray? =
        indexedFileEntries.itemNullableBytes(fileIndex, index) {
            IrArrayReader(icData[fileIndex].fileEntries)
        }

    override fun file(index: Int): ByteArray = icData[index].fileData

    override fun fileCount(): Int = icData.size

    override fun types(fileIndex: Int): ByteArray = icData[fileIndex].types

    override fun signatures(fileIndex: Int): ByteArray = icData[fileIndex].signatures

    override fun strings(fileIndex: Int): ByteArray = icData[fileIndex].strings

    override fun declarations(fileIndex: Int): ByteArray = icData[fileIndex].declarations

    override fun bodies(fileIndex: Int): ByteArray = icData[fileIndex].bodies

    override fun fileEntries(fileIndex: Int): ByteArray? = icData[fileIndex].fileEntries
}

class CurrentModuleWithICDeserializer(
    private val delegate: IrModuleDeserializer,
    private val symbolTable: SymbolTable,
    private val irBuiltIns: IrBuiltIns,
    icData: List<SerializedIrFile>,
    icReaderFactory: (IrLibrary) -> IrModuleDeserializer) :
    IrModuleDeserializer(delegate.moduleDescriptor, KotlinAbiVersion.CURRENT) {

    private val dirtyDeclarations = hashMapOf<IdSignature, IrSymbol>()
    private val icKlib = ICKotlinLibrary(icData)

    private val icDeserializer: IrModuleDeserializer = icReaderFactory(icKlib)

    override fun contains(idSig: IdSignature): Boolean {
        return idSig in dirtyDeclarations || idSig.topLevelSignature() in icDeserializer || idSig in delegate
    }

    override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        dirtyDeclarations[idSig]?.let { return it }

        if (idSig.topLevelSignature() in icDeserializer) return icDeserializer.deserializeIrSymbolOrFail(idSig, symbolKind)

        return delegate.deserializeIrSymbolOrFail(idSig, symbolKind)
    }

    override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing = delegate.deserializedSymbolNotFound(idSig)

    override fun addModuleReachableTopLevel(topLevelDeclarationSignature: IdSignature) {
        assert(topLevelDeclarationSignature in icDeserializer)
        icDeserializer.addModuleReachableTopLevel(topLevelDeclarationSignature)
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

    @OptIn(DelicateSymbolTableApi::class)
    override fun init(delegate: IrModuleDeserializer) {
        val knownBuiltIns = irBuiltIns.knownBuiltins.map { (it as IrSymbolOwner).symbol }.toSet()
        symbolTable.descriptorExtension.forEachDeclarationSymbol {
            assert(it.isPublicApi)
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
    override val kind: IrModuleDeserializerKind
        get() = delegate.kind

    override fun fileDeserializers(): Collection<IrFileDeserializer> {
        return delegate.fileDeserializers()
    }
}
