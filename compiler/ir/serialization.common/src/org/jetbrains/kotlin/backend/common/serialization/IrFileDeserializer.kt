/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.linkage.IrDeserializer
import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.types.defaultTypeWithoutArguments
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.isClassSignature
import org.jetbrains.kotlin.library.components.KlibIrComponent
import org.jetbrains.kotlin.library.encodings.WobblyTF8
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry as ProtoFileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IrAnnotation as ProtoAnnotation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression as ProtoExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType

abstract class IrFileDeserializer {
    abstract val file: IrFile
    abstract val symbolDeserializer: IrSymbolDeserializer
    abstract val declarationDeserializer: IrDeclarationDeserializer
    abstract val reversedSignatureIndex: Map<IdSignature, Int>

    abstract fun deserializeDeclaration(idSig: IdSignature): IrDeclaration

    /**
     * Deserializes file-level annotations for the current [IrFile].
     *
     * The actual deserialization happens just once on the first invocation.
     * The subsequent invocations have no effect.
     *
     * @return If the annotations have been actually deserialized on this invocation.
     */
    abstract fun deserializeFileImplicitDataIfFirstUse(): Boolean

    abstract fun getAllMatchingSignatures(callableId: CallableId, signatureKind: IrDeserializer.TopLevelSymbolKind): List<IdSignature>
}

class IrFileDeserializerImpl(
    override val file: IrFile,
    private val libraryFile: IrLibraryFile,
    fileProto: ProtoFile,
    override val symbolDeserializer: IrSymbolDeserializer,
    override val declarationDeserializer: IrDeclarationDeserializer,
) : IrFileDeserializer() {
    override val reversedSignatureIndex = fileProto.declarationIdList.associateBy { symbolDeserializer.deserializeIdSignature(it) }

    private val callableIdToSignature = buildMap<CallableId, MutableList<IdSignature>> {
        reversedSignatureIndex.keys.forEach { idSig ->
            if (idSig !is IdSignature.CommonSignature) return@forEach
            if (idSig.isClassSignature()) return@forEach
            val callableId = CallableId(idSig.packageFqName(), Name.identifier(idSig.declarationFqName))
            getOrPut(callableId) { mutableListOf() } += idSig
        }
    }

    /** Once deserialized this property is set to `null`. */
    private var protoAnnotationsPendingDeserialization: List<ProtoAnnotation>? = fileProto.annotationList

    override fun deserializeDeclaration(idSig: IdSignature): IrDeclaration {
        return declarationDeserializer.deserializeDeclaration(loadTopLevelDeclarationProto(idSig), file.startOffset).also {
            // Type alias can be accidentally deserialized twice. We shouldn't add it into the declaration list for the second time.
            // It would be better to avoid type alias deserialization all together, but we don't know that until we parse proto.
            if (it is IrTypeAlias && file.declarations.contains(it)) return@also
            file.declarations += it
        }
    }

    private fun loadTopLevelDeclarationProto(idSig: IdSignature): ProtoDeclaration {
        val idSigIndex = reversedSignatureIndex[idSig] ?: error("Not found Idx for $idSig")
        return libraryFile.declaration(idSigIndex)
    }

    override fun deserializeFileImplicitDataIfFirstUse(): Boolean {
        protoAnnotationsPendingDeserialization?.let {
            file.annotations += declarationDeserializer.deserializeAnnotations(it, file.startOffset)
            protoAnnotationsPendingDeserialization = null

            return true
        }

        return false
    }

    override fun getAllMatchingSignatures(callableId: CallableId, signatureKind: IrDeserializer.TopLevelSymbolKind): List<IdSignature> {
        val topLevelCallableSignature = callableIdToSignature[callableId] ?: return emptyList()
        return buildList {
            for (topLevelSignature in topLevelCallableSignature) {
                val index = reversedSignatureIndex[topLevelSignature] ?: continue
                val proto = libraryFile.declaration(index)
                when (signatureKind) {
                    IrDeserializer.TopLevelSymbolKind.FUNCTION_SYMBOL ->
                        if (proto.declaratorCase == ProtoDeclaration.DeclaratorCase.IR_FUNCTION) add(topLevelSignature)
                    IrDeserializer.TopLevelSymbolKind.PROPERTY_SYMBOL ->
                        if (proto.declaratorCase == ProtoDeclaration.DeclaratorCase.IR_PROPERTY) add(topLevelSignature)
                    else -> error("Unexpected signature kind: $signatureKind")
                }
            }
        }
    }
}

abstract class FileDeserializationState {
    abstract val fileIndex: Int
    abstract val file: IrFile
    abstract val fileReader: IrLibraryFileFromBytes
    abstract val declarationDeserializer: IrDeclarationDeserializer
    abstract val fileDeserializer: IrFileDeserializer

    /**
     * Schedule deserialization of a top-level declaration with the given signature.
     */
    abstract fun addIdSignature(topLevelDeclarationSignature: IdSignature)

    /**
     * Schedule deserialization of all top-level declarations in this file.
     */
    abstract fun enqueueAllDeclarations()

    /**
     * Deserialize all top-level declarations previously scheduled for deserialization in the current file.
     */
    abstract fun deserializeAllFileReachableTopLevel()
}

class FileDeserializationStateImpl(
    private val linker: KotlinIrLinker,
    override val fileIndex: Int,
    override val file: IrFile,
    override val fileReader: IrLibraryFileFromBytes,
    fileProto: ProtoFile,
    settings: IrDeserializationSettings,
    moduleDeserializer: IrModuleDeserializer
): FileDeserializationState() {

    private val symbolDeserializer = IrSymbolDeserializer(
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

    override val declarationDeserializer = IrDeclarationDeserializer(
        linker.unitClass.defaultTypeWithoutArguments,
        linker.nothingClass.defaultTypeWithoutArguments,
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
        fileEntryDeserializer = linker.fileEntryDeserializer,
    )

    override val fileDeserializer = IrFileDeserializerImpl(file, fileReader, fileProto, symbolDeserializer, declarationDeserializer)

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

    override fun addIdSignature(topLevelDeclarationSignature: IdSignature) {
        reachableTopLevels.add(topLevelDeclarationSignature)
    }

    override fun enqueueAllDeclarations() {
        reachableTopLevels.addAll(fileDeserializer.reversedSignatureIndex.keys)
    }

    override fun deserializeAllFileReachableTopLevel() {
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
        val extensionRegistryLite: ExtensionRegistryLite = ExtensionRegistryLite.getEmptyRegistry()
    }
}

class IrKlibBytesSource(private val ir: KlibIrComponent, private val fileIndex: Int) : IrLibraryBytesSource() {
    override fun irDeclaration(index: Int): ByteArray = ir.declaration(index, fileIndex)
    override fun type(index: Int): ByteArray = ir.type(index, fileIndex)
    override fun signature(index: Int): ByteArray = ir.signature(index, fileIndex)
    override fun string(index: Int): ByteArray = ir.stringLiteral(index, fileIndex)
    override fun body(index: Int): ByteArray = ir.body(index, fileIndex)
    override fun debugInfo(index: Int): ByteArray? = ir.signatureDebugInfo(index, fileIndex)
    override fun fileEntry(index: Int): ByteArray? = ir.irFileEntry(index, fileIndex)
}

fun IrLibraryFile.deserializeFqName(fqn: List<Int>): String =
    fqn.joinToString(".", transform = ::string)

fun IrLibraryFile.createFile(module: IrModuleFragment, fileProto: ProtoFile, fileEntryDeserializer: FileEntryDeserializer): IrFile {
    val fileEntry = fileEntryDeserializer.fileEntry(this, fileProto)
    val fqName = FqName(deserializeFqName(fileProto.fqNameList))
    val packageFragmentDescriptor = EmptyPackageFragmentDescriptor(module.descriptor, fqName)
    val symbol = IrFileSymbolImpl(packageFragmentDescriptor)
    return IrFileImpl(fileEntry, symbol, fqName, module)
}

fun IrLibraryFile.deserializeFileEntryName(fileEntryProto: ProtoFileEntry): String = when {
    fileEntryProto.hasName() -> string(fileEntryProto.name)
    fileEntryProto.hasNameOld() -> fileEntryProto.nameOld
    else -> error("Malformed KLIB: File entry has no name")
}

fun IrLibraryFile.fileEntry(protoFile: ProtoFile): FileEntry =
    if (protoFile.hasFileEntryId())
        fileEntry(protoFile.fileEntryId) ?: error("Invalid KLib: cannot read file entry by its index")
    else {
        require(protoFile.hasFileEntry()) { "Invalid KLib: either fileEntry or fileEntryId must be present" }
        protoFile.fileEntry
    }

fun KlibIrComponent.fileEntry(protoFile: ProtoFile, fileIndex: Int): FileEntry =
    if (protoFile.hasFileEntryId()) {
        val fileEntry = irFileEntry(protoFile.fileEntryId, fileIndex) ?: error("Invalid KLib: cannot read file entry by its index")
        ProtoFileEntry.parseFrom(fileEntry)
    } else {
        require(protoFile.hasFileEntry()) {
            "Invalid KLib: either fileEntry or valid fileEntryId must be present. Valid fileEntryId is a valid index in existing file entries table"
        }
        protoFile.fileEntry
    }
