/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.overrides.DefaultFakeOverrideClassFilter
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsMappingState
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsGlobalDeclarationTable
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrDeclarationBase
import org.jetbrains.kotlin.ir.serialization.CarrierDeserializer
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.impl.DeclarationId
import org.jetbrains.kotlin.library.impl.DeclarationIrTableMemoryReader
import org.jetbrains.kotlin.library.impl.IrArrayMemoryReader
import org.jetbrains.kotlin.library.impl.IrIntArrayMemoryReader
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import kotlin.collections.ArrayDeque
import kotlin.collections.HashSet

import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoIrDeclaration

class IcDeserializer(
    val linker: JsIrLinker,
    val context: JsIrBackendContext,
) {
    private val signaturer = IdSignatureSerializer(JsManglerIr)
    private val globalDeclarationTable = JsGlobalDeclarationTable(signaturer, context.irBuiltIns)

    fun injectIcData(module: IrModuleFragment, icData: SerializedIcData) {
        // Prepare per-file indices

        val moduleDeserializer = linker.moduleDeserializer(module.descriptor)

        val fileDeserializers = moduleDeserializer.fileDeserializers()

        val fileQueue = ArrayDeque<IcFileDeserializer>()
        val signatureQueue = ArrayDeque<IdSignature>()
        val kindQueue = ArrayDeque<BinarySymbolData.SymbolKind>()

        fun IdSignature.enqueue(icDeserializer: IcFileDeserializer, kind: BinarySymbolData.SymbolKind) {
            if ("$this" == "private kotlin.js.internal/IntCompanionObject.MIN_VALUE|7841734059694520564[0]:15") {
                1
            }

            if (this !in icDeserializer.visited) {
//                println("   -> $this (${icDeserializer.fileDeserializer.file.name})")
                fileQueue.addLast(icDeserializer)
                signatureQueue.addLast(this)
                kindQueue.addLast(kind)
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

        val existingPublicSymbols = mutableMapOf<IdSignature, IrSymbol>()

        context.irBuiltIns.packageFragment.declarations.forEach {
            existingPublicSymbols[it.symbol.signature!!] = it.symbol
        }

//        val

        context.intrinsics.externalPackageFragment.declarations.forEach {
            val signature = it.symbol.signature ?: globalDeclarationTable.computeSignatureByDeclaration(it)
            existingPublicSymbols[signature] = it.symbol
        }

        fileDeserializers.forEach { fd ->
            fd.symbolDeserializer.deserializedSymbols.entries.forEach { (idSig, symbol) ->
                if (idSig.isPublic) {
                    existingPublicSymbols[idSig] = symbol
                }
            }
        }

        val publicSignatureToIcFileDeserializer = mutableMapOf<IdSignature, IcFileDeserializer>()

        val fdToIcFd = mutableMapOf<IrFileDeserializer, IcFileDeserializer>()

        // Add all signatures withing the module to a queue ( declarations and bodies )
        // TODO add bodies
        println("==== Init ====")
        for (fd in fileDeserializers) {
            val icFileData = pathToIcFileData[fd.file.path] ?: continue

            lateinit var icDeserializer: IcFileDeserializer

            icDeserializer = IcFileDeserializer(
                linker, fd, icFileData,
                { idSig, kind ->
                    val deser = if (idSig is IdSignature.FileLocalSignature) publicSignatureToIcFileDeserializer[idSig] ?: this else this
                    idSig.enqueue(deser, kind)
                },
                pathToFileSymbol = { p -> pathToFileSymbol[p]!! },
                context.mapping.state,
            ) { idSig, kind ->

                if (idSig.toString() == "public kotlin.coroutines/ContinuationInterceptor.key|1144547298251177939[0]") {
                    1
                }

                existingPublicSymbols[idSig] ?: icDeserializer.privateSymbols[idSig] ?: if (moduleDeserializer.contains(idSig)) moduleDeserializer.deserializeIrSymbol(idSig, kind) else null ?: run {
                    if (idSig.isPublic || idSig is IdSignature.FileLocalSignature) {
                        val fileDeserializer = publicSignatureToIcFileDeserializer[idSig.topLevelSignature()]
                            ?: fdToIcFd[fileDeserializers.first { idSig.topLevelSignature() in it.reversedSignatureIndex}]
                            ?: error("file deserializer not found: $idSig")
                        idSig.enqueue(fileDeserializer, kind)
                        fileDeserializer.deserializeIrSymbol(idSig, kind).also {
                            existingPublicSymbols[idSig] = it
                        }
                    } else {
                        idSig.enqueue(icDeserializer, kind)
                        icDeserializer.deserializeIrSymbol(idSig, kind).also {
                            icDeserializer.privateSymbols[idSig] = it
                        }
                    }
                }
            }

            fdToIcFd[fd] = icDeserializer

            fd.symbolDeserializer.deserializedSymbols.entries.forEach { (idSig, symbol) ->
                idSig.enqueue(icDeserializer, IrFileSerializer.protoSymbolKind(symbol))

                if (!idSig.isPublic) {
                    icDeserializer.privateSymbols[idSig] = symbol
                }
            }

            icDeserializer.reversedSignatureIndex.keys.forEach {
                if (it.isPublic || it is IdSignature.FileLocalSignature) {
                    publicSignatureToIcFileDeserializer[it] = icDeserializer
                }
            }

            fd.file.acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitDeclaration(declaration: IrDeclarationBase) {
                    declaration.symbol.signature?.let { idSig ->
                        if (idSig.isPublic || idSig is IdSignature.FileLocalSignature) {
                            existingPublicSymbols[idSig] = declaration.symbol
                            publicSignatureToIcFileDeserializer[idSig] = icDeserializer
                        } else {
                            icDeserializer.privateSymbols[idSig] = declaration.symbol
                        }
                    }
                    super.visitDeclaration(declaration)
                }
            })
        }

        println("==== Queue ==== ")

        while (signatureQueue.isNotEmpty()) {
            val icFileDeserializer = fileQueue.removeFirst()
            val signature = signatureQueue.removeFirst()
            val kind = kindQueue.removeFirst()

            if (signature is IdSignature.FileSignature) continue


            if ("$signature" == "private kotlin.js.internal/IntCompanionObject.MIN_VALUE|7841734059694520564[0]:4638265728071529947") {
                1
            }
            if ("$signature" == "private kotlin.contracts/ContractBuilder.callsInPlace\$default|-8084630878733243362[0]:10") {
                2
            }

//            println("$signature")

//            println("$signature (${icFileDeserializer.fileDeserializer.file.name})")
//            println("  decl:")
            // Deserialize the declaration
            val symbol = existingPublicSymbols[signature] ?: icFileDeserializer.privateSymbols[signature]
            val declaration = if (symbol != null && symbol.isBound) symbol.owner as IrDeclaration else icFileDeserializer.deserializeDeclaration(signature)
//
            if (declaration == null) {
                if (kind != BinarySymbolData.SymbolKind.VARIABLE_SYMBOL) {
                    println("skipped $signature [$kind] (${icFileDeserializer.fileDeserializer.file.name});")
                }
                continue
            }

            icFileDeserializer.signatureToDeclaration[signature] = declaration

//            println("  carriers:")


            // public kotlin.contracts/Returns.implies|-5823325932675831010[0]
            if ("$signature" == "public kotlin.contracts/Returns.implies|-5823325932675831010[0]") {
                1
            }

            icFileDeserializer.injectCarriers(declaration, signature)

//            println("  mappings:")

            icFileDeserializer.mappingsDeserializer(signature, declaration)

//            println(";")
        }

//        // TODO declaration to be deserialized
//        for (icFileDeserializer in allIcDeserializers) {
//            // TODO how to filter out only relevant mappings?
//            context.mapping.state.mappingsDeserializer(icFileDeserializer.icFileData.mappings) {
//                icFileDeserializer.deserializeIrSymbol(it)
//            }
//        }

        for (fd in fdToIcFd.values) {
             for (d in fd.visited) {
                 if (d is PersistentIrDeclarationBase<*> && d.values == null) {
                     error("Declaration ${d.render()} didn't get injected with the carriers")
                 }
             }
        }


        println("==== Order ==== ")

        context.irFactory.stageController.withStage(1000) {

            for (fd in fileDeserializers) {
                val icFileData = pathToIcFileData[fd.file.path] ?: continue
                val order = icFileData.order
                val icDeserializer = fdToIcFd[fd]!!

                if ("ExceptionsH" in icDeserializer.fileDeserializer.file.name) {
                    1
                }

                println(fd.file.name)

                fd.file.declarations.clear()

                IrIntArrayMemoryReader(order.topLevelSignatures).array.forEach {
                    val idSig = icDeserializer.deserializeIdSignature(it)

                    if (idSig in icDeserializer.visited) {
                        val declaration = icDeserializer.signatureToDeclaration[idSig]!!
                        fd.file.declarations += declaration
                    }
                }

                val containerDeclarations = IrArrayMemoryReader(order.containerDeclarationSignatures)
                for (i in 0 until containerDeclarations.entryCount()) {
                    val bytes = containerDeclarations.tableItemBytes(i)
                    val indices = IrIntArrayMemoryReader(bytes).array

                    val containerSig = icDeserializer.deserializeIdSignature(indices[0])

                    if (containerSig in icDeserializer.visited) {
                        val irClass = icDeserializer.signatureToDeclaration[containerSig]!! as IrClass

                        val localSignatureMap = mutableMapOf<IdSignature?, IrSymbol>()
                        irClass.declarations.forEach {
                            localSignatureMap[it.symbol.signature] = it.symbol
                            if (it is IrProperty) {
                                it.backingField?.let { localSignatureMap[it.symbol.signature] = it.symbol }
                                it.getter?.let { localSignatureMap[it.symbol.signature] = it.symbol }
                                it.setter?.let { localSignatureMap[it.symbol.signature] = it.symbol }
                            }
                        }

                        irClass.declarations.clear()

                        for (j in 1 until indices.size) {
                            val idSig = icDeserializer.deserializeIdSignature(indices[j])

                            val symbol = localSignatureMap[idSig] ?: existingPublicSymbols[idSig] ?: icDeserializer.privateSymbols[idSig]

                            val declaration = if (symbol != null && symbol.isBound) symbol.owner as IrDeclaration else icDeserializer.signatureToDeclaration[idSig]
                            if (declaration == null) continue

                            irClass.declarations += declaration
                        }
                    }
                }
            }
        }
    }

    class IcFileDeserializer(
        val linker: JsIrLinker,
        val fileDeserializer: IrFileDeserializer,
        val icFileData: SerializedIcDataForFile,
        val enqueueLocalTopLevelDeclaration: IcFileDeserializer.(IdSignature, BinarySymbolData.SymbolKind) -> Unit,
        val pathToFileSymbol: (String) -> IrFileSymbol,
        val mappingState: JsMappingState,
        val deserializePublicSymbol: (IdSignature, BinarySymbolData.SymbolKind) -> IrSymbol,
    ) {

        val privateSymbols = mutableMapOf<IdSignature, IrSymbol>()

        private val fileReader = FileReaderFromSerializedIrFile(icFileData.file)

        private fun cntToReturnableBlockSymbol(upCnt: Int): IrReturnableBlockSymbol {
            return declarationDeserializer.bodyDeserializer.cntToReturnableBlockSymbol(upCnt)
        }

        val signatureToDeclaration = mutableMapOf<IdSignature, IrDeclaration>()

        private val symbolDeserializer = IrSymbolDeserializer(
            linker.symbolTable,
            fileReader,
            fileDeserializer.file.path,
            emptyList(),
            { idSig, kind -> enqueueLocalTopLevelDeclaration(idSig, kind) },
            { _, s -> s },
            pathToFileSymbol,
            ::cntToReturnableBlockSymbol,
            enqueueAllDeclarations = true,
            deserializePublicSymbol,
        ).also {
            for ((idSig, symbol) in fileDeserializer.symbolDeserializer.deserializedSymbols.entries) {
                it.deserializedSymbols[idSig] = symbol
            }
        }

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
            { _, _, _, -> }, // Don't need to capture bodies?,
            skipMutableState = true,
        )

        private val protoFile: ProtoFile = ProtoFile.parseFrom(icFileData.file.fileData.codedInputStream, ExtensionRegistryLite.newInstance())

        private val carrierDeserializer = CarrierDeserializer(declarationDeserializer, icFileData.carriers)

//        init {
//            IrArrayMemoryReader(icFileData.file.signatures)
//        }

        val reversedSignatureIndex: Map<IdSignature, Int> = protoFile.declarationIdList.map { symbolDeserializer.deserializeIdSignature(it) to it }.toMap()

        val visited = HashSet<IdSignature>()

        val mappingsDeserializer = mappingState.mappingsDeserializer(icFileData.mappings, { code ->
            val symbolData = symbolDeserializer.parseSymbolData(code)
            symbolDeserializer.deserializeIdSignature(symbolData.signatureId)
        }) {
            deserializeIrSymbol(it)
        }

        fun deserializeDeclaration(idSig: IdSignature): IrDeclaration? {
            // Check if the declaration was deserialized before
            // TODO is this needed?
//            val symbol = symbolDeserializer.deserial[idSig]
//            if (symbol != null && symbol.isBound) return symbol.owner as IrDeclaration

//            val originalSymbol = fileDeserializer.symbolDeserializer.deserializedSymbols[idSig]
//            if (originalSymbol != null) return originalSymbol.owner as IrDeclaration

            // Do deserialize stuff
            val idSigIndex = reversedSignatureIndex[idSig] ?: return null
//                error("Not found Idx for $idSig")
            val declarationStream = fileReader.irDeclaration(idSigIndex).codedInputStream
            val declarationProto = ProtoIrDeclaration.parseFrom(declarationStream, ExtensionRegistryLite.newInstance())
            return declarationDeserializer.deserializeDeclaration(declarationProto)
        }

        fun deserializeIrSymbol(code: Long): IrSymbol {
            return symbolDeserializer.deserializeIrSymbol(code)
        }

        fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
            enqueueLocalTopLevelDeclaration(idSig, symbolKind)
            return symbolDeserializer.deserializeIrSymbol(idSig, symbolKind)
        }

        fun deserializeIdSignature(index: Int): IdSignature {
            return symbolDeserializer.deserializeIdSignature(index)
        }

        fun injectCarriers(declaration: IrDeclaration, signature: IdSignature) {
            carrierDeserializer.injectCarriers(declaration, signature)
        }
    }
}

class FileReaderFromSerializedIrFile(val irFile: SerializedIrFile) : IrLibraryFile() {
    val declarationReader = DeclarationIrTableMemoryReader(irFile.declarations)
    val typeReader = IrArrayMemoryReader(irFile.types)
    val signatureReader = IrArrayMemoryReader(irFile.signatures)
    val stringReader = IrArrayMemoryReader(irFile.strings)
    val bodyReader = IrArrayMemoryReader(irFile.bodies)

    override fun irDeclaration(index: Int): ByteArray = declarationReader.tableItemBytes(DeclarationId(index))

    override fun type(index: Int): ByteArray = typeReader.tableItemBytes(index)

    override fun signature(index: Int): ByteArray = signatureReader.tableItemBytes(index)

    override fun string(index: Int): ByteArray = stringReader.tableItemBytes(index)

    override fun body(index: Int): ByteArray = bodyReader.tableItemBytes(index)
}