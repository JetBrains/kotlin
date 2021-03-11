/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.overrides.DefaultFakeOverrideClassFilter
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.serialization.CarrierDeserializer
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.impl.IrArrayMemoryReader
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import kotlin.collections.ArrayDeque
import kotlin.collections.HashSet

import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoIrDeclaration

class IcDeserializer(
    val linker: JsIrLinker,
    val context: JsIrBackendContext,
) {
    fun injectIcData(module: IrModuleFragment, icData: SerializedIcData) {
        // Prepare per-file indices

        val moduleDeserializer = linker.moduleDeserializer(module.descriptor)

        val fileDeserializers = moduleDeserializer.fileDeserializers()

        val fileQueue = ArrayDeque<IcFileDeserializer>()
        val signatureQueue = ArrayDeque<IdSignature>()

        val publicSignatureToIcFileDeserializer = mutableMapOf<IdSignature, IcFileDeserializer>()

        fun IdSignature.enqueue(icDeserializer: IcFileDeserializer) {
            if (this !in icDeserializer.visited) {
                fileQueue.addLast(icDeserializer)
                signatureQueue.addLast(this)
                icDeserializer.visited += this
            }
        }

        val pathToIcFileData = icData.files.associateBy {
            it.file.path
        }

        val pathToFileSymbol = mutableMapOf<String, IrFileSymbol>()
        for (fd in fileDeserializers) {
            pathToFileSymbol[fd.file.path] = fd.file.symbol
        }

        val intrinsicSignatureToSymbol = context.irBuiltIns.packageFragment.declarations.associate {
            val signature = it.symbol.signature ?:
                error("sdfds")
            signature to it.symbol
        }

        val allIcDeserializers = mutableListOf<IcFileDeserializer>()

        // Add all signatures withing the module to a queue ( declarations and bodies )
        // TODO add bodies
        for (fd in fileDeserializers) {
            val icFileData = pathToIcFileData[fd.file.path] ?: continue

            val icDeserializer = IcFileDeserializer(
                linker, fd, icFileData,
                {
                    it.enqueue(this)
                },
                pathToFileSymbol = { p -> pathToFileSymbol[p]!! },
            ) { idSig, kind ->
                publicSignatureToIcFileDeserializer[idSig]?.deserializeIrSymbol(idSig, kind)
                    ?: intrinsicSignatureToSymbol[idSig]
                    ?: moduleDeserializer.deserializeIrSymbol(idSig, kind)
            }

            allIcDeserializers += icDeserializer

            fd.symbolDeserializer.deserializedSymbols.keys.forEach {
                it.enqueue(icDeserializer)

                if (it.isPublic) {
                    publicSignatureToIcFileDeserializer[it] = icDeserializer
                }
            }

            icDeserializer.reversedSignatureIndex.keys.forEach {
                if (it.isPublic) {
                    publicSignatureToIcFileDeserializer[it] = icDeserializer
                }
            }
        }

        while (signatureQueue.isNotEmpty()) {
            val icFileDeserializer = fileQueue.removeFirst()
            val signature = signatureQueue.removeFirst()

            // Deserialize the declaration
            val declaration = icFileDeserializer.deserializeDeclaration(signature)
//
//            if (declaration == null) {
//                println("declaration skipped: $signature")
//                continue
//            }

            icFileDeserializer.injectCarriers(declaration)
        }

        // TODO declaration to be deserialized
        for (icFileDeserializer in allIcDeserializers) {
            // TODO how to filter out only relevant mappings?
            context.mapping.state.deserializeMappings(icFileDeserializer.icFileData.mappings) {
                icFileDeserializer.deserializeIrSymbol(it)
            }
        }
    }

    class IcFileDeserializer(
        val linker: JsIrLinker,
        val fileDeserializer: IrFileDeserializer,
        val icFileData: SerializedIcDataForFile,
        val enqueueLocalTopLevelDeclaration: IcFileDeserializer.(IdSignature) -> Unit,
        val pathToFileSymbol: (String) -> IrFileSymbol,
        val deserializePublicSymbol: (IdSignature, BinarySymbolData.SymbolKind) -> IrSymbol,
    ) {

        private val fileReader = FileReaderFromSerializedIrFile(icFileData.file)

        private fun cntToReturnableBlockSymbol(upCnt: Int): IrReturnableBlockSymbol {
            return declarationDeserializer.bodyDeserializer.cntToReturnableBlockSymbol(upCnt)
        }

        private val symbolDeserializer = IrSymbolDeserializer(
            linker.symbolTable,
            fileReader,
            emptyList(),
            { enqueueLocalTopLevelDeclaration(it) },
            { _, s -> s },
            pathToFileSymbol,
            ::cntToReturnableBlockSymbol,
            deserializePublicSymbol,
        )

        private val declarationDeserializer = IrDeclarationDeserializer(
            linker.builtIns,
            linker.symbolTable,
            linker.symbolTable.irFactory,
            fileReader,
            fileDeserializer.file,
            fileDeserializer.declarationDeserializer.allowErrorNodes,
            deserializeInlineFunctions = true,
            deserializeBodies = true,
            symbolDeserializer,
            DefaultFakeOverrideClassFilter,
            linker.fakeOverrideBuilder,
            { _, _, _, -> }, // Don't need to capture bodies?
        )

        private val protoFile: ProtoFile = ProtoFile.parseFrom(icFileData.file.fileData.codedInputStream, ExtensionRegistryLite.newInstance())

        private val carrierDeserializer = CarrierDeserializer(declarationDeserializer, icFileData.carriers)

        val reversedSignatureIndex: Map<IdSignature, Int> = protoFile.declarationIdList.map { symbolDeserializer.deserializeIdSignature(it) to it }.toMap()

        val visited = HashSet<IdSignature>()

        fun deserializeDeclaration(idSig: IdSignature): IrDeclaration {
            // Check if the declaration was deserialized before
            // TODO is this needed?
            val symbol = symbolDeserializer.deserializedSymbols[idSig]
            if (symbol != null && symbol.isBound) return symbol.owner as IrDeclaration

            val originalSymbol = fileDeserializer.symbolDeserializer.deserializedSymbols[idSig]
            if (originalSymbol != null) return originalSymbol.owner as IrDeclaration

            // Do deserialize stuff
            val idSigIndex = reversedSignatureIndex[idSig] ?: //return null
                error("Not found Idx for $idSig")
            val declarationStream = fileReader.irDeclaration(idSigIndex).codedInputStream
            val declarationProto = ProtoIrDeclaration.parseFrom(declarationStream, ExtensionRegistryLite.newInstance())
            return declarationDeserializer.deserializeDeclaration(declarationProto)
        }

        fun deserializeIrSymbol(code: Long): IrSymbol {
            return symbolDeserializer.deserializeIrSymbol(code)
        }

        fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
            enqueueLocalTopLevelDeclaration(idSig)
            return symbolDeserializer.deserializeIrSymbol(idSig, symbolKind)
        }

        fun injectCarriers(declaration: IrDeclaration) {
            carrierDeserializer.injectCarriers(declaration)
        }
    }
}

class FileReaderFromSerializedIrFile(val irFile: SerializedIrFile) : IrLibraryFile() {
    val declarationReader = IrArrayMemoryReader(irFile.declarations)
    val typeReader = IrArrayMemoryReader(irFile.types)
    val signatureReader = IrArrayMemoryReader(irFile.signatures)
    val stringReader = IrArrayMemoryReader(irFile.strings)
    val bodyReader = IrArrayMemoryReader(irFile.bodies)

    override fun irDeclaration(index: Int): ByteArray = declarationReader.tableItemBytes(index)

    override fun type(index: Int): ByteArray = typeReader.tableItemBytes(index)

    override fun signature(index: Int): ByteArray = signatureReader.tableItemBytes(index)

    override fun string(index: Int): ByteArray = stringReader.tableItemBytes(index)

    override fun body(index: Int): ByteArray = bodyReader.tableItemBytes(index)
}