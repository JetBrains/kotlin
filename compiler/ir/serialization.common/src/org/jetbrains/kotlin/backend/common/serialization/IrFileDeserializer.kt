/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.serialization.encodings.*
import org.jetbrains.kotlin.backend.common.serialization.proto.Actual
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

internal open class IrFileDeserializer(
    val linker: KotlinIrLinker,
    val logger: LoggingContext,
    val builtIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    val file: IrFile,
    private val fileReader: IrLibraryFile,
    fileProto: ProtoFile,
    deserializeBodies: Boolean,
    deserializeFakeOverrides: Boolean,
    allowErrorNodes: Boolean,
    deserializeInlineFunctions: Boolean,
    private val moduleDeserializer: IrModuleDeserializer,
    val handleNoModuleDeserializerFound: (IdSignature) -> IrModuleDeserializer,
) {
    protected val irFactory: IrFactory get() = symbolTable.irFactory

    val symbolDeserializer = IrSymbolDeserializer(linker, fileReader,this, fileProto.actualsList)

    val reversedSignatureIndex = fileProto.declarationIdList.map { symbolDeserializer.deserializeIdSignature(it) to it }.toMap()

    private var annotations: List<ProtoConstructorCall>? = fileProto.annotationList

    private val declarationDeserializer = IrDeclarationDeserializer(
        linker,
        logger,
        builtIns,
        symbolTable,
        irFactory,
        fileReader,
        file,
        deserializeFakeOverrides,
        allowErrorNodes,
        deserializeInlineFunctions,
        deserializeBodies,
        symbolDeserializer,
    )

    inner class FileDeserializationState(explicitlyExportedToCompilerList: List<Long>) {
        private val reachableTopLevels = LinkedHashSet<IdSignature>()

        init {
            // Explicitly exported declarations (e.g. top-level initializers) must be deserialized before all other declarations.
            // Thus we schedule their deserialization in deserializer's constructor.
            explicitlyExportedToCompilerList.forEach {
                val symbolData = symbolDeserializer.parseSymbolData(it)
                val sig = symbolDeserializer.deserializeIdSignature(symbolData.signatureId)
                assert(!sig.isPackageSignature())
                addIdSignature(sig.topLevelSignature())
            }
        }

        fun addIdSignature(key: IdSignature) {
            reachableTopLevels.add(key)
        }

        fun addAllIdSignatures(keys: Iterable<IdSignature>) {
            reachableTopLevels.addAll(keys)
        }

        fun processPendingDeclarations() {
            while (reachableTopLevels.isNotEmpty()) {
                val reachableKey = reachableTopLevels.first()

                val existedSymbol = symbolDeserializer.deserializedSymbols[reachableKey]
                if (existedSymbol == null || !existedSymbol.isBound) {
                    val declaration = deserializeDeclaration(reachableKey)
                    file.declarations.add(declaration)
                }

                reachableTopLevels.remove(reachableKey)
            }
        }
    }

    val fileLocalDeserializationState = FileDeserializationState(fileProto.explicitlyExportedToCompilerList)


    fun deserializeDeclaration(idSig: IdSignature): IrDeclaration {
        return declarationDeserializer.deserializeDeclaration(loadTopLevelDeclarationProto(idSig))
    }

    private fun readDeclaration(index: Int): CodedInputStream =
        fileReader.irDeclaration(index).codedInputStream

    private fun loadTopLevelDeclarationProto(idSig: IdSignature): ProtoDeclaration {
        val idSigIndex = reversedSignatureIndex[idSig] ?: error("Not found Idx for $idSig")
        return ProtoDeclaration.parseFrom(readDeclaration(idSigIndex), ExtensionRegistryLite.newInstance())
    }

    internal fun getModuleForTopLevelId(idSignature: IdSignature): IrModuleDeserializer? {
        if (idSignature in moduleDeserializer) return moduleDeserializer
        return moduleDeserializer.moduleDependencies.firstOrNull { idSignature in it }
    }

    internal fun findModuleDeserializer(idSig: IdSignature): IrModuleDeserializer {
        assert(idSig.isPublic)

        val topLevelSig = idSig.topLevelSignature()
        if (topLevelSig in moduleDeserializer) return moduleDeserializer
        return moduleDeserializer.moduleDependencies.firstOrNull { topLevelSig in it } ?: handleNoModuleDeserializerFound(idSig)
    }

    fun deserializeFileImplicitDataIfFirstUse() {
        annotations?.let {
            file.annotations += declarationDeserializer.deserializeAnnotations(it)
            annotations = null
        }
    }

    fun deserializeAllFileReachableTopLevel() {
        fileLocalDeserializationState.processPendingDeclarations()
    }

    fun enqueueAllDeclarations() {
        fileLocalDeserializationState.addAllIdSignatures(reversedSignatureIndex.keys)
    }
}

internal class IrLibraryFile(private val klib: IrLibrary, private val fileIndex: Int) {
    fun irDeclaration(index: Int): ByteArray = klib.irDeclaration(index, fileIndex)
    fun type(index: Int): ByteArray = klib.type(index, fileIndex)
    fun signature(index: Int): ByteArray = klib.signature(index, fileIndex)
    fun string(index: Int): ByteArray = klib.string(index, fileIndex)
    fun body(index: Int): ByteArray = klib.body(index, fileIndex)
}

internal fun IrLibraryFile.deserializeString(index: Int): String = String(string(index))

internal fun IrLibraryFile.deserializeFqName(fqn: List<Int>): String =
    fqn.joinToString(".", transform = ::deserializeString)

internal fun IrLibraryFile.createFile(moduleDescriptor: ModuleDescriptor, fileProto: ProtoFile): IrFile {
    val fileName = fileProto.fileEntry.name
    val fileEntry = NaiveSourceBasedFileEntryImpl(fileName, fileProto.fileEntry.lineStartOffsetsList.toIntArray())
    val fqName = FqName(deserializeFqName(fileProto.fqNameList))
    val packageFragmentDescriptor = EmptyPackageFragmentDescriptor(moduleDescriptor, fqName)
    val symbol = IrFileSymbolImpl(packageFragmentDescriptor)
    return IrFileImpl(fileEntry, symbol, fqName)
}