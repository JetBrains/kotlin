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

internal class IrFileDeserializer(
    val linker: KotlinIrLinker,
    val file: IrFile,
    private val fileReader: IrLibraryFile,
    fileProto: ProtoFile,
    val symbolDeserializer: IrSymbolDeserializer,
    val declarationDeserializer: IrDeclarationDeserializer,
) {
    val reversedSignatureIndex = fileProto.declarationIdList.map { symbolDeserializer.deserializeIdSignature(it) to it }.toMap()

    private var annotations: List<ProtoConstructorCall>? = fileProto.annotationList

    internal fun deserializeDeclaration(idSig: IdSignature): IrDeclaration {
        return declarationDeserializer.deserializeDeclaration(loadTopLevelDeclarationProto(idSig)).also {
            file.declarations += it
        }
    }

    private fun readDeclaration(index: Int): CodedInputStream =
        fileReader.irDeclaration(index).codedInputStream

    private fun loadTopLevelDeclarationProto(idSig: IdSignature): ProtoDeclaration {
        val idSigIndex = reversedSignatureIndex[idSig] ?: error("Not found Idx for $idSig")
        return ProtoDeclaration.parseFrom(readDeclaration(idSigIndex), ExtensionRegistryLite.newInstance())
    }

    fun deserializeFileImplicitDataIfFirstUse() {
        annotations?.let {
            file.annotations += declarationDeserializer.deserializeAnnotations(it)
            annotations = null
        }
    }
}

internal class FileDeserializationState(
    val linker: KotlinIrLinker,
    file: IrFile,
    fileReader: IrLibraryFile,
    fileProto: ProtoFile,
    deserializeBodies: Boolean,
    allowErrorNodes: Boolean,
    deserializeInlineFunctions: Boolean,
    moduleDeserializer: IrModuleDeserializer,
    handleNoModuleDeserializerFound: (IdSignature) -> IrModuleDeserializer,
) {

    val symbolDeserializer = IrSymbolDeserializer(linker, fileReader, this, fileProto.actualsList, moduleDeserializer, handleNoModuleDeserializerFound)

    private val declarationDeserializer = IrDeclarationDeserializer(
        linker,
        linker.logger,
        linker.builtIns,
        linker.symbolTable,
        linker.symbolTable.irFactory,
        fileReader,
        file,
        linker.deserializeFakeOverrides,
        allowErrorNodes,
        deserializeInlineFunctions,
        deserializeBodies,
        symbolDeserializer,
    )

    val fileDeserializer = IrFileDeserializer(linker, file, fileReader, fileProto, symbolDeserializer, declarationDeserializer)

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

    fun addIdSignature(key: IdSignature) {
        reachableTopLevels.add(key)
    }

    fun enqueueAllDeclarations() {
        reachableTopLevels.addAll(fileDeserializer.reversedSignatureIndex.keys)
    }

    fun deserializeAllFileReachableTopLevel() {
        while (reachableTopLevels.isNotEmpty()) {
            val reachableKey = reachableTopLevels.first()

            val existedSymbol = symbolDeserializer.deserializedSymbols[reachableKey]
            if (existedSymbol == null || !existedSymbol.isBound) {
                fileDeserializer.deserializeDeclaration(reachableKey)
            }

            reachableTopLevels.remove(reachableKey)
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