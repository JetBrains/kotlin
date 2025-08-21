/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.encodings.WobblyTF8
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression as ProtoExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry as ProtoFileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType

class IrFileDeserializer(
    val file: IrFile,
    private val libraryFile: IrLibraryFile,
    fileProto: ProtoFile,
    val symbolDeserializer: IrSymbolDeserializer,
    val declarationDeserializer: IrDeclarationDeserializer,
) {
    val reversedSignatureIndex = fileProto.declarationIdList.associateBy { symbolDeserializer.deserializeIdSignature(it) }

    /** Once deserialized this property is set to `null`. */
    private var protoAnnotationsPendingDeserialization: List<ProtoConstructorCall>? = fileProto.annotationList

    fun deserializeDeclaration(idSig: IdSignature): IrDeclaration {
        return declarationDeserializer.deserializeDeclaration(loadTopLevelDeclarationProto(idSig)).also {
            file.declarations += it
        }
    }

    private fun loadTopLevelDeclarationProto(idSig: IdSignature): ProtoDeclaration {
        val idSigIndex = reversedSignatureIndex[idSig] ?: error("Not found Idx for $idSig")
        return libraryFile.declaration(idSigIndex)
    }

    /**
     * Deserializes file-level annotations for the current [IrFile].
     *
     * The actual deserialization happens just once on the first invocation.
     * The subsequent invocations have no effect.
     *
     * @return If the annotations have been actually deserialized on this invocation.
     */
    fun deserializeFileImplicitDataIfFirstUse(): Boolean {
        protoAnnotationsPendingDeserialization?.let {
            file.annotations += declarationDeserializer.deserializeAnnotations(it)
            protoAnnotationsPendingDeserialization = null

            return true
        }

        return false
    }
}

class FileDeserializationState(
    val linker: KotlinIrLinker,
    val fileIndex: Int,
    val file: IrFile,
    val fileReader: IrLibraryFileFromBytes,
    fileProto: ProtoFile,
    settings: IrDeserializationSettings,
    moduleDeserializer: IrModuleDeserializer
) {

    val symbolDeserializer = IrSymbolDeserializer(
        symbolTable = linker.symbolTable,
        libraryFile = fileReader,
        fileSymbol = file.symbol,
        enqueueLocalTopLevelDeclaration = ::addIdSignature,
        deserializedSymbolPostProcessor = linker.deserializedSymbolPostProcessor,
        irInterner = linker.irInterner,
        deserializePublicSymbolWithOwnerInUnknownFile = { idSignature, symbolKind ->
            // Dispatch it through IR linker to find the concrete file deserializer, and then deserialize the symbol
            // with the associated symbol deserialized.
            linker.deserializeOrReturnUnboundIrSymbolIfPartialLinkageEnabled(idSignature, symbolKind, moduleDeserializer)
        })

    val declarationDeserializer = IrDeclarationDeserializer(
        linker.builtIns,
        linker.symbolTable,
        linker.symbolTable.irFactory,
        fileReader,
        file,
        settings,
        symbolDeserializer,
        onDeserializedClass = { clazz, signature ->
            linker.fakeOverrideBuilder.enqueueClass(clazz, signature, moduleDeserializer.compatibilityMode)
        },
        needToDeserializeFakeOverrides = { clazz ->
            !linker.fakeOverrideBuilder.platformSpecificClassFilter.needToConstructFakeOverrides(clazz)
        },
        specialProcessingForMismatchedSymbolKind = runIf(linker.partialLinkageSupport.isEnabled) {
            { deserializedSymbol, fallbackSymbolKind ->
                referenceDeserializedSymbol(
                    symbolTable = linker.symbolTable,
                    fileSymbol = null,
                    symbolKind = fallbackSymbolKind ?: error("No fallback symbol kind specified for symbol $deserializedSymbol"),
                    idSig = deserializedSymbol.signature?.takeIf { it.isPubliclyVisible }
                        ?: error("No public signature for symbol $deserializedSymbol")
                )
            }
        },
        irInterner = linker.irInterner,
    )

    val fileDeserializer = IrFileDeserializer(file, fileReader, fileProto, symbolDeserializer, declarationDeserializer)

    /**
     * This is the queue of top-level declarations in the current file to be deserialized.
     *
     * A declaration can be enqueued using one of the available ways: [addIdSignature], [enqueueAllDeclarations].
     * The deserialization happens on invocation of [deserializeAllFileReachableTopLevel].
     *
     * Note 1: The signature is removed from the queue during deserialization of the corresponding declaration.
     *
     * Note 2: Since we don't know the state of a declaration for a certain [IdSignature], there are no
     * guarantees that the queue contains only items that has NEVER been attempted to be deserialized before.
     * It actually may contain signatures of already deserialized declarations. In that case, the deserialization
     * does not happen (as there is nothing effectively to deserialize), but the signature is removed from the queue.
     */
    private val reachableTopLevels = LinkedHashSet<IdSignature>()

    init {
        // Explicitly exported declarations (e.g. top-level initializers) must be deserialized before all other declarations.
        // Thus we schedule their deserialization in deserializer's constructor.
        fileProto.explicitlyExportedToCompilerList.forEach {
            val symbolData = symbolDeserializer.parseSymbolData(it)
            val sig = symbolDeserializer.deserializeIdSignature(symbolData.signatureId)
            assert(!sig.isPackageSignature())
            addIdSignature(sig.topLevelSignature())
        }
    }

    /**
     * Schedule deserialization of a top-level declaration with the given signature.
     */
    fun addIdSignature(topLevelDeclarationSignature: IdSignature) {
        reachableTopLevels.add(topLevelDeclarationSignature)
    }

    /**
     * Schedule deserialization of all top-level declarations in this file.
     */
    fun enqueueAllDeclarations() {
        reachableTopLevels.addAll(fileDeserializer.reversedSignatureIndex.keys)
    }

    /**
     * Deserialize all top-level declarations previously scheduled for deserialization in the current file.
     */
    fun deserializeAllFileReachableTopLevel() {
        while (reachableTopLevels.isNotEmpty()) {
            val topLevelDeclarationSignature = reachableTopLevels.first()

            val topLevelDeclarationSymbol = symbolDeserializer.deserializedSymbolsWithOwnersInCurrentFile[topLevelDeclarationSignature]
            if (topLevelDeclarationSymbol == null || !topLevelDeclarationSymbol.isBound) {
                // Perform actual deserialization:
                val topLevelDeclaration = fileDeserializer.deserializeDeclaration(topLevelDeclarationSignature)

                // Enqueue the deserialized declaration to the PL engine for further processing.
                linker.partialLinkageSupport.enqueueDeclaration(topLevelDeclaration)
            }

            // Remove it from the queue:
            reachableTopLevels.remove(topLevelDeclarationSignature)
        }
    }
}

abstract class IrLibraryFile {
    abstract fun declaration(index: Int): ProtoDeclaration
    abstract fun inlineDeclaration(index: Int): ProtoDeclaration
    abstract fun type(index: Int): ProtoType
    abstract fun signature(index: Int): ProtoIdSignature
    abstract fun string(index: Int): String
    abstract fun expressionBody(index: Int): ProtoExpression
    abstract fun statementBody(index: Int): ProtoStatement
    abstract fun debugInfo(index: Int): String?
    abstract fun fileEntry(index: Int): ProtoFileEntry?
}

abstract class IrLibraryBytesSource {
    abstract fun irDeclaration(index: Int): ByteArray
    abstract fun irInlineDeclaration(index: Int): ByteArray
    abstract fun type(index: Int): ByteArray
    abstract fun signature(index: Int): ByteArray
    abstract fun string(index: Int): ByteArray
    abstract fun body(index: Int): ByteArray
    abstract fun debugInfo(index: Int): ByteArray?
    abstract fun fileEntry(index: Int): ByteArray?
}

class IrLibraryFileFromBytes(private val bytesSource: IrLibraryBytesSource) : IrLibraryFile() {

    override fun declaration(index: Int): ProtoDeclaration =
        ProtoDeclaration.parseFrom(bytesSource.irDeclaration(index).codedInputStream, extensionRegistryLite)

    override fun inlineDeclaration(index: Int): ProtoDeclaration =
        ProtoDeclaration.parseFrom(bytesSource.irInlineDeclaration(index).codedInputStream, extensionRegistryLite)

    override fun type(index: Int): ProtoType = ProtoType.parseFrom(bytesSource.type(index).codedInputStream, extensionRegistryLite)

    override fun signature(index: Int): ProtoIdSignature =
        ProtoIdSignature.parseFrom(bytesSource.signature(index).codedInputStream, extensionRegistryLite)

    override fun string(index: Int): String = WobblyTF8.decode(bytesSource.string(index))

    override fun expressionBody(index: Int): ProtoExpression =
        ProtoExpression.parseFrom(bytesSource.body(index).codedInputStream, extensionRegistryLite)

    override fun statementBody(index: Int): ProtoStatement =
        ProtoStatement.parseFrom(bytesSource.body(index).codedInputStream, extensionRegistryLite)

    override fun debugInfo(index: Int): String? = bytesSource.debugInfo(index)?.let { WobblyTF8.decode(it) }
    override fun fileEntry(index: Int): ProtoFileEntry? = bytesSource.fileEntry(index)?.let {
        ProtoFileEntry.parseFrom(it, extensionRegistryLite)
    }

    companion object {
        val extensionRegistryLite: ExtensionRegistryLite = ExtensionRegistryLite.newInstance()
    }
}

class IrKlibBytesSource(private val klib: IrLibrary, private val fileIndex: Int) : IrLibraryBytesSource() {
    override fun irDeclaration(index: Int): ByteArray = klib.irDeclaration(index, fileIndex)
    override fun irInlineDeclaration(index: Int): ByteArray = klib.irInlineDeclaration(index, fileIndex)
    override fun type(index: Int): ByteArray = klib.type(index, fileIndex)
    override fun signature(index: Int): ByteArray = klib.signature(index, fileIndex)
    override fun string(index: Int): ByteArray = klib.string(index, fileIndex)
    override fun body(index: Int): ByteArray = klib.body(index, fileIndex)
    override fun debugInfo(index: Int): ByteArray? = klib.debugInfo(index, fileIndex)
    override fun fileEntry(index: Int): ByteArray? = klib.fileEntry(index, fileIndex)
}

fun IrLibraryFile.deserializeFqName(fqn: List<Int>): String =
    fqn.joinToString(".", transform = ::string)

fun IrLibraryFile.createFile(module: IrModuleFragment, fileProto: ProtoFile): IrFile {
    val fileEntry = deserializeFileEntry(fileEntry(fileProto))
    val fqName = FqName(deserializeFqName(fileProto.fqNameList))
    val packageFragmentDescriptor = EmptyPackageFragmentDescriptor(module.descriptor, fqName)
    val symbol = IrFileSymbolImpl(packageFragmentDescriptor)
    return IrFileImpl(fileEntry, symbol, fqName, module)
}

internal fun deserializeFileEntry(fileEntryProto: ProtoFileEntry): IrFileEntry =
    NaiveSourceBasedFileEntryImpl(
        name = fileEntryProto.name,
        lineStartOffsets = fileEntryProto.lineStartOffsetList.toIntArray(),
        firstRelevantLineIndex = fileEntryProto.firstRelevantLineIndex
    )


fun IrLibraryFile.fileEntry(protoFile: ProtoFile): FileEntry =
    if (protoFile.hasFileEntryId())
        fileEntry(protoFile.fileEntryId) ?: error("Invalid KLib: cannot read file entry by its index")
    else {
        require(protoFile.hasFileEntry()) { "Invalid KLib: either fileEntry or fileEntryId must be present" }
        protoFile.fileEntry
    }

fun IrLibrary.fileEntry(protoFile: ProtoFile, fileIndex: Int): FileEntry =
    if (protoFile.hasFileEntryId() && hasFileEntriesTable) {
        val fileEntry = fileEntry(protoFile.fileEntryId, fileIndex) ?: error("Invalid KLib: cannot read file entry by its index")
        ProtoFileEntry.parseFrom(fileEntry)
    } else {
        require(protoFile.hasFileEntry()) {
            "Invalid KLib: either fileEntry or valid fileEntryId must be present. Valid fileEntryId is a valid index in existing file entries table"
        }
        protoFile.fileEntry
    }
