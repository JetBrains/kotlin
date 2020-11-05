/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.serialization.encodings.*
import org.jetbrains.kotlin.backend.common.serialization.proto.Actual
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature.IdsigCase.*
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
import org.jetbrains.kotlin.backend.common.serialization.proto.AccessorIdSignature as ProtoAccessorIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature as ProtoFileLocalIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.PublicIdSignature as ProtoPublicIdSignature

internal open class IrFileDeserializer(
    val logger: LoggingContext,
    val builtIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    val file: IrFile,
    private val fileReader: IrLibraryFile,
    protected var deserializeBodies: Boolean,
    deserializeFakeOverrides: Boolean,
    fakeOverrideQueue: MutableList<IrClass>,
    allowErrorNodes: Boolean,
    private var annotations: List<ProtoConstructorCall>?,
    private val actuals: List<Actual>,
    deserializeInlineFunctions: Boolean,
    private val moduleDeserializer: IrModuleDeserializer,
    fakeOverrideBuilder: FakeOverrideBuilder,
    val expectUniqIdToActualUniqId: MutableMap<IdSignature, IdSignature>,
    val topLevelActualUniqItToDeserializer: MutableMap<IdSignature, IrModuleDeserializer>,
    val handleNoModuleDeserializerFound: (IdSignature) -> IrModuleDeserializer,
    val haveSeen: MutableSet<IrSymbol>,
    val referenceDeserializedSymbol: (BinarySymbolData.SymbolKind, IdSignature) -> IrSymbol,
) {
    protected val irFactory: IrFactory get() = symbolTable.irFactory

    var reversedSignatureIndex = emptyMap<IdSignature, Int>()

    private val declarationDeserializer = IrDeclarationDeserializer(
        logger,
        builtIns,
        symbolTable,
        irFactory,
        fileReader,
        deserializeFakeOverrides,
        fakeOverrideQueue,
        allowErrorNodes,
        deserializeInlineFunctions,
        deserializeBodies,
        fakeOverrideBuilder,
        this
    )

    inner class FileDeserializationState {
        private val reachableTopLevels = LinkedHashSet<IdSignature>()
        val deserializedSymbols = mutableMapOf<IdSignature, IrSymbol>()

        fun addIdSignature(key: IdSignature) {
            reachableTopLevels.add(key)
        }

        fun processPendingDeclarations() {
            while (reachableTopLevels.isNotEmpty()) {
                val reachableKey = reachableTopLevels.first()

                val existedSymbol = deserializedSymbols[reachableKey]
                if (existedSymbol == null || !existedSymbol.isBound) {
                    val declaration = deserializeDeclaration(reachableKey)
                    file.declarations.add(declaration)
                }

                reachableTopLevels.remove(reachableKey)
            }
        }
    }

    val fileLocalDeserializationState = FileDeserializationState()

    fun deserializeDeclaration(idSig: IdSignature): IrDeclaration {
        return declarationDeserializer.deserializeDeclaration(loadTopLevelDeclarationProto(idSig), file)
    }

    fun deserializeExpectActualMapping() {
        actuals.forEach {
            val expectSymbol = parseSymbolData(it.expectSymbol)
            val actualSymbol = parseSymbolData(it.actualSymbol)

            val expect = deserializeIdSignature(expectSymbol.signatureId)
            val actual = deserializeIdSignature(actualSymbol.signatureId)

            assert(expectUniqIdToActualUniqId[expect] == null) {
                "Expect signature $expect is already actualized by ${expectUniqIdToActualUniqId[expect]}, while we try to record $actual"
            }
            expectUniqIdToActualUniqId[expect] = actual
            // Non-null only for topLevel declarations.
            getModuleForTopLevelId(actual)?.let { md -> topLevelActualUniqItToDeserializer[actual] = md }
        }
    }

    private fun readDeclaration(index: Int): CodedInputStream =
        fileReader.irDeclaration(index).codedInputStream

    private fun loadTopLevelDeclarationProto(idSig: IdSignature): ProtoDeclaration {
        val idSigIndex = reversedSignatureIndex[idSig] ?: error("Not found Idx for $idSig")
        return ProtoDeclaration.parseFrom(readDeclaration(idSigIndex), ExtensionRegistryLite.newInstance())
    }

    private fun readSignature(index: Int): CodedInputStream =
        fileReader.signature(index).codedInputStream

    private fun loadSignatureProto(index: Int): ProtoIdSignature {
        return ProtoIdSignature.parseFrom(readSignature(index), ExtensionRegistryLite.newInstance())
    }

    private fun getModuleForTopLevelId(idSignature: IdSignature): IrModuleDeserializer? {
        if (idSignature in moduleDeserializer) return moduleDeserializer
        return moduleDeserializer.moduleDependencies.firstOrNull { idSignature in it }
    }

    private fun findModuleDeserializer(idSig: IdSignature): IrModuleDeserializer {
        assert(idSig.isPublic)

        val topLevelSig = idSig.topLevelSignature()
        if (topLevelSig in moduleDeserializer) return moduleDeserializer
        return moduleDeserializer.moduleDependencies.firstOrNull { topLevelSig in it } ?: handleNoModuleDeserializerFound(idSig)
    }

    private fun referenceIrSymbolData(symbol: IrSymbol, signature: IdSignature) {
        assert(signature.isLocal)
        fileLocalDeserializationState.deserializedSymbols.putIfAbsent(signature, symbol)
    }

    private fun deserializeIrLocalSymbolData(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        assert(idSig.isLocal)

        if (idSig.hasTopLevel) {
            fileLocalDeserializationState.addIdSignature(idSig.topLevelSignature())
        }

        return fileLocalDeserializationState.deserializedSymbols.getOrPut(idSig) {
            referenceDeserializedSymbol(symbolKind, idSig)
        }
    }

    private fun deserializeIrSymbolData(idSignature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (idSignature.isLocal) return deserializeIrLocalSymbolData(idSignature, symbolKind)

        return findModuleDeserializer(idSignature).deserializeIrSymbol(idSignature, symbolKind).also {
            haveSeen.add(it)
        }
    }

    fun deserializeIrSymbolToDeclare(code: Long): Pair<IrSymbol, IdSignature> {
        val symbolData = parseSymbolData(code)
        val signature = deserializeIdSignature(symbolData.signatureId)
        return Pair(deserializeIrSymbolData(signature, symbolData.kind), signature)
    }

    fun parseSymbolData(code: Long): BinarySymbolData = BinarySymbolData.decode(code)

    fun deserializeIrSymbol(code: Long): IrSymbol {
        val symbolData = parseSymbolData(code)
        val signature = deserializeIdSignature(symbolData.signatureId)
        return deserializeIrSymbolData(signature, symbolData.kind)
    }

    fun deserializeIdSignature(index: Int): IdSignature {
        val sigData = loadSignatureProto(index)
        return deserializeSignatureData(sigData)
    }

    fun referenceIrSymbol(symbol: IrSymbol, signature: IdSignature) {
        referenceIrSymbolData(symbol, signature)
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

    private val delegatedSymbolMap = mutableMapOf<IrSymbol, IrSymbol>()

    internal fun deserializeIrSymbolAndRemap(code: Long): IrSymbol {
        // TODO: could be simplified
        return deserializeIrSymbol(code).let {
            delegatedSymbolMap[it] ?: it
        }
    }

    internal fun recordDelegatedSymbol(symbol: IrSymbol) {
        if (symbol is IrDelegatingSymbol<*, *, *>) {
            delegatedSymbolMap[symbol] = symbol.delegate
        }
    }

    internal fun eraseDelegatedSymbol(symbol: IrSymbol) {
        if (symbol is IrDelegatingSymbol<*, *, *>) {
            delegatedSymbolMap.remove(symbol)
        }
    }

    /* -------------------------------------------------------------- */

    // TODO: Think about isolating id signature related logic behind corresponding interface

    private fun deserializePublicIdSignature(proto: ProtoPublicIdSignature): IdSignature.PublicSignature {
        val pkg = fileReader.deserializeFqName(proto.packageFqNameList)
        val cls = fileReader.deserializeFqName(proto.declarationFqNameList)
        val memberId = if (proto.hasMemberUniqId()) proto.memberUniqId else null

        return IdSignature.PublicSignature(pkg, cls, memberId, proto.flags)
    }

    private fun deserializeAccessorIdSignature(proto: ProtoAccessorIdSignature): IdSignature.AccessorSignature {
        val propertySignature = deserializeIdSignature(proto.propertySignature)
        require(propertySignature is IdSignature.PublicSignature) { "For public accessor corresponding property supposed to be public as well" }
        val name = fileReader.deserializeString(proto.name)
        val hash = proto.accessorHashId
        val mask = proto.flags

        val accessorSignature =
            IdSignature.PublicSignature(propertySignature.packageFqName, "${propertySignature.declarationFqName}.$name", hash, mask)

        return IdSignature.AccessorSignature(propertySignature, accessorSignature)
    }

    private fun deserializeFileLocalIdSignature(proto: ProtoFileLocalIdSignature): IdSignature.FileLocalSignature {
        return IdSignature.FileLocalSignature(deserializeIdSignature(proto.container), proto.localId)
    }

    private fun deserializeScopeLocalIdSignature(proto: Int): IdSignature.ScopeLocalDeclaration {
        return IdSignature.ScopeLocalDeclaration(proto)
    }

    fun deserializeSignatureData(proto: ProtoIdSignature): IdSignature {
        return when (proto.idsigCase) {
            PUBLIC_SIG -> deserializePublicIdSignature(proto.publicSig)
            ACCESSOR_SIG -> deserializeAccessorIdSignature(proto.accessorSig)
            PRIVATE_SIG -> deserializeFileLocalIdSignature(proto.privateSig)
            SCOPED_LOCAL_SIG -> deserializeScopeLocalIdSignature(proto.scopedLocalSig)
            else -> error("Unexpected IdSignature kind: ${proto.idsigCase}")
        }
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