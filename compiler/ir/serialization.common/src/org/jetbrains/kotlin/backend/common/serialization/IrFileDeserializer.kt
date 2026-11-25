/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.types.defaultTypeWithoutArguments
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.components.KlibIrComponent
import org.jetbrains.kotlin.library.encodings.WobblyTF8
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.utils.pollFirst
import org.jetbrains.kotlin.utils.putToMultiMap
import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry as ProtoFileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IrAnnotation as ProtoAnnotation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression as ProtoExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType

class IrFileDeserializer(
    val file: IrFile,
    private val libraryFile: IrLibraryFile,
    fileProto: ProtoFile,
    val symbolDeserializer: IrSymbolDeserializer,
    private val declarationDeserializer: IrDeclarationDeserializer,
) {
    /**
     * All top-level declarations in this file that are available for deserialization:
     * the signature and the kind of the declaration mapped to its ID in the KLIB (see [IrLibraryFile.declaration]).
     */
    val topLevelDeclarationIndex: Map<SignatureAndKind, Int> = buildMap {
        val symbolIds: List<Long> = fileProto.topLevelDeclarationSymbolsList
        if (symbolIds.isNotEmpty()) {
            // Iterate through symbol data [signatureId + kind].
            for (symbolId in symbolIds) {
                val symbolData = BinarySymbolData.decode(symbolId)
                val signature = symbolDeserializer.deserializeIdSignature(symbolData.signatureId)
                this[SignatureAndKind(signature, symbolData.kind)] = symbolData.signatureId
            }
        } else {
            // A fallback for old KLIBs: Iterate through signatures, load symbol kinds from Proto messages.
            for (signatureId in fileProto.legacyTopLevelDeclarationSignaturesList) {
                val signature = symbolDeserializer.deserializeIdSignature(signatureId)

                val kind: BinarySymbolData.SymbolKind = when (val declarationKind = libraryFile.declarationKind(signatureId)) {
                    ProtoDeclaration.DeclaratorCase.IR_CLASS -> BinarySymbolData.SymbolKind.CLASS_SYMBOL
                    ProtoDeclaration.DeclaratorCase.IR_FUNCTION -> BinarySymbolData.SymbolKind.FUNCTION_SYMBOL
                    ProtoDeclaration.DeclaratorCase.IR_PROPERTY -> BinarySymbolData.SymbolKind.PROPERTY_SYMBOL
                    ProtoDeclaration.DeclaratorCase.IR_TYPE_ALIAS -> {
                        if (declarationDeserializer.settings.deserializeTypeAliases)
                            BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL
                        else
                            continue
                    }
                    ProtoDeclaration.DeclaratorCase.IR_FIELD -> {
                        // There are some legacy KLIBs with top-level fields.
                        BinarySymbolData.SymbolKind.FIELD_SYMBOL
                    }
                    else -> error(
                        "Unexpected top-level declaration kind $declarationKind" +
                                " for signature ${signature::class.java.simpleName} ${signature.render()}" +
                                " in file ${file.fileEntry.name} of module ${file.module.name}"
                    )
                }

                this[SignatureAndKind(signature, kind)] = signatureId
            }
        }
    }

    /** The cache of [TopLevelCallableIdAndKind] representing a top-level callable and the corresponding signatures */
    private val topLevelCallables: Map<TopLevelCallableIdAndKind, List<IdSignature>> by lazy {
        buildMap<TopLevelCallableIdAndKind, MutableList<IdSignature>> {
            for ([signature, kind] in topLevelDeclarationIndex.keys) {
                // Only `IdSignature.CommonSignature` can denote a top-level declaration visible to other files/modules.
                if (signature !is IdSignature.CommonSignature) continue

                if (kind == BinarySymbolData.SymbolKind.FUNCTION_SYMBOL || kind == BinarySymbolData.SymbolKind.PROPERTY_SYMBOL) {
                    putToMultiMap(TopLevelCallableIdAndKind(signature, kind), signature)
                }
            }
        }
    }

    // TODO(KT-91417): Drop this legacy index after completely migrating to signature+symbolKind addressing scheme.
    private val reversedSignatureIndex: Map<IdSignature, Int> = topLevelDeclarationIndex.mapKeys { it.key.signature }

    /** Once deserialized this property is set to `null`. */
    private var protoAnnotationsPendingDeserialization: List<ProtoAnnotation>? = fileProto.annotationList

    internal fun deserializeDeclaration(idSig: IdSignature): IrDeclaration? {
        val proto = loadTopLevelDeclarationProto(idSig)
        if (!declarationDeserializer.deserializeTypeAliases && proto.declaratorCase == ProtoDeclaration.DeclaratorCase.IR_TYPE_ALIAS)
            return null
        return declarationDeserializer.deserializeDeclaration(proto, file.startOffset).also {
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
            file.annotations += declarationDeserializer.deserializeAnnotations(it, file.startOffset)
            protoAnnotationsPendingDeserialization = null

            return true
        }

        return false
    }

    internal fun collectAllMatchingSignatures(
        topLevelCallableId: TopLevelCallableIdAndKind,
        output: MutableCollection<IdSignature>,
    ) {
        topLevelCallables[topLevelCallableId]?.let { output += it }
    }
}

@OptIn(IrLinkerInternalApi::class)
class FileDeserializationState(
    private val linker: KotlinIrLinker,
    val fileIndex: Int,
    val file: IrFile,
    val fileReader: IrLibraryFileFromBytes,
    fileProto: ProtoFile,
    settings: IrDeserializationSettings,
    moduleDeserializer: IrModuleDeserializer
) {
    private val symbolDeserializer = IrSymbolDeserializer(
        symbolTable = linker.symbolTable,
        libraryFile = fileReader,
        fileSymbol = file.symbol,
        deserializedSymbolPostProcessor = linker.deserializedSymbolPostProcessor,
        irInterner = linker.irInterner,
        deserializePublicSymbolWithOwnerInUnknownFile = { idSignature, symbolKind ->
            // Dispatch it through IR linker to find the concrete file deserializer, and then deserialize the symbol
            // with the associated symbol deserialized.
            linker.deserializeOrReturnUnboundIrSymbolIfPartialLinkageEnabled(idSignature, symbolKind, moduleDeserializer)
        })

    val declarationDeserializer = IrDeclarationDeserializer(
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
        specialProcessingForMismatchedSymbolKind = { deserializedSymbol, fallbackSymbolKind ->
            referenceDeserializedSymbol(
                symbolTable = linker.symbolTable,
                fileSymbol = null,
                symbolKind = fallbackSymbolKind ?: error("No fallback symbol kind specified for symbol $deserializedSymbol"),
                idSig = deserializedSymbol.signature?.takeIf { it.isPubliclyVisible }
                    ?: error("No public signature for symbol $deserializedSymbol")
            )
        },
        irInterner = linker.irInterner,
        fileEntryDeserializer = linker.fileEntryDeserializer,
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
    internal fun addIdSignature(topLevelDeclarationSignature: IdSignature) {
        reachableTopLevels.add(topLevelDeclarationSignature)
    }

    /**
     * Schedule deserialization of all top-level declarations in this file.
     */
    internal fun enqueueAllDeclarations() {
        fileDeserializer.topLevelDeclarationIndex.keys.mapTo(reachableTopLevels) { it.signature }
    }

    /**
     * Deserialize all top-level declarations previously scheduled for deserialization in the current file.
     */
    internal fun deserializeAllFileReachableTopLevel() {
        while (true) {
            val topLevelDeclarationSignature = reachableTopLevels.pollFirst() ?: break

            val topLevelDeclarationSymbol = symbolDeserializer.deserializedSymbolsWithOwnersInCurrentFile[topLevelDeclarationSignature]
            if (topLevelDeclarationSymbol == null || !topLevelDeclarationSymbol.isBound) {
                // Perform actual deserialization:
                fileDeserializer.deserializeDeclaration(topLevelDeclarationSignature)?.let {
                    // Enqueue the deserialized declaration to the PL engine for further processing.
                    linker.partialLinkageSupport.enqueueDeclaration(it)
                }
            }
        }
    }
}

abstract class IrLibraryFile {
    abstract fun declarationKind(index: Int): ProtoDeclaration.DeclaratorCase?
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
    // TODO find a better approach KT-90010
    override fun declarationKind(index: Int): ProtoDeclaration.DeclaratorCase? {
        val input = bytesSource.irDeclaration(index).codedInputStream
        return when (input.readTag()) {
            10 -> ProtoDeclaration.DeclaratorCase.IR_ANONYMOUS_INIT
            18 -> ProtoDeclaration.DeclaratorCase.IR_CLASS
            26 -> ProtoDeclaration.DeclaratorCase.IR_CONSTRUCTOR
            34 -> ProtoDeclaration.DeclaratorCase.IR_ENUM_ENTRY
            42 -> ProtoDeclaration.DeclaratorCase.IR_FIELD
            50 -> ProtoDeclaration.DeclaratorCase.IR_FUNCTION
            58 -> ProtoDeclaration.DeclaratorCase.IR_PROPERTY
            66 -> ProtoDeclaration.DeclaratorCase.IR_TYPE_PARAMETER
            74 -> ProtoDeclaration.DeclaratorCase.IR_VARIABLE
            82 -> ProtoDeclaration.DeclaratorCase.IR_VALUE_PARAMETER
            90 -> ProtoDeclaration.DeclaratorCase.IR_LOCAL_DELEGATED_PROPERTY
            98 -> ProtoDeclaration.DeclaratorCase.IR_TYPE_ALIAS
            else -> null
        }
    }

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

fun ProtoFile.getSignatureIdsOfTopLevelDeclarations(): List<Int> {
    val symbolIds = topLevelDeclarationSymbolsList
    return if (symbolIds.isNotEmpty()) {
        symbolIds.map { symbolId -> BinarySymbolData.decode(symbolId).signatureId }
    } else {
        legacyTopLevelDeclarationSignaturesList
    }
}
