/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.JsMapping
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.impl.IrLongArrayMemoryReader
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite

import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

class IcModuleDeserializer(
    val irFactory: PersistentIrFactory,
    val mapping: JsMapping,
    val linker: JsIrLinker,
    val icData: SerializedIcData,
    moduleDescriptor: ModuleDescriptor,
    override val klib: IrLibrary,
    override val strategy: DeserializationStrategy,
    private val containsErrorCode: Boolean = false,
    private val useGlobalSignatures: Boolean = false,
) : IrModuleDeserializer(moduleDescriptor) {

    private val fileToDeserializerMap = mutableMapOf<IrFile, IrFileDeserializer>()

    internal val moduleReversedFileIndex = mutableMapOf<IdSignature, IcFileDeserializer>()
    internal val icModuleReversedFileIndex = mutableMapOf<IdSignature, IcFileDeserializer>()

    override val moduleDependencies by lazy {
        moduleDescriptor.allDependencyModules.filter { it != moduleDescriptor }.map { linker.resolveModuleDeserializer(it, null) }
    }

    override fun fileDeserializers(): Collection<IrFileDeserializer> {
        return fileToDeserializerMap.values
    }

    override fun init(delegate: IrModuleDeserializer) {
        val fileCount = klib.fileCount()

        val files = ArrayList<IrFile>(fileCount)

        for (i in 0 until fileCount) {
            val fileStream = klib.file(i).codedInputStream
            val fileProto = ProtoFile.parseFrom(fileStream, ExtensionRegistryLite.newInstance())
            files.add(deserializeIrFile(fileProto, i, delegate, containsErrorCode))
        }

        moduleFragment.files.addAll(files)

        fileToDeserializerMap.values.forEach { it.symbolDeserializer.deserializeExpectActualMapping() }
    }

    private fun IrSymbolDeserializer.deserializeExpectActualMapping() {
        actuals.forEach {
            val expectSymbol = parseSymbolData(it.expectSymbol)
            val actualSymbol = parseSymbolData(it.actualSymbol)

            val expect = deserializeIdSignature(expectSymbol.signatureId)
            val actual = deserializeIdSignature(actualSymbol.signatureId)

            assert(linker.expectUniqIdToActualUniqId[expect] == null) {
                "Expect signature $expect is already actualized by ${linker.expectUniqIdToActualUniqId[expect]}, while we try to record $actual"
            }
            linker.expectUniqIdToActualUniqId[expect] = actual
            // Non-null only for topLevel declarations.
            findModuleDeserializerForTopLevelId(actual)?.let { md -> linker.topLevelActualUniqItToDeserializer[actual] = md }
        }
    }

    override fun referenceSimpleFunctionByLocalSignature(file: IrFile, idSignature: IdSignature): IrSimpleFunctionSymbol =
        fileToDeserializerMap[file]?.symbolDeserializer?.referenceSimpleFunctionByLocalSignature(idSignature)
            ?: error("No deserializer for file $file in module ${moduleDescriptor.name}")

    override fun referencePropertyByLocalSignature(file: IrFile, idSignature: IdSignature): IrPropertySymbol =
        fileToDeserializerMap[file]?.symbolDeserializer?.referencePropertyByLocalSignature(idSignature)
            ?: error("No deserializer for file $file in module ${moduleDescriptor.name}")

    // TODO: fix to topLevel checker
    override fun contains(idSig: IdSignature): Boolean = idSig in moduleReversedFileIndex || idSig in icModuleReversedFileIndex

    override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        assert(idSig.isPublic)

        if (idSig in icModuleReversedFileIndex) {
            val icDeserializer = icModuleReversedFileIndex[idSig]!!
            return icDeserializer.deserializeIrSymbol(idSig, symbolKind)
        }

        val topLevelSignature = idSig.topLevelSignature()
        val icDeserializer = moduleReversedFileIndex[topLevelSignature]
            ?: error("No file for $topLevelSignature (@ $idSig) in module $moduleDescriptor")

        topLevelSignature.originalEnqueue(icDeserializer)
        idSig.enqueue(icDeserializer)
        linker.modulesWithReachableTopLevels.add(this)

        return icDeserializer.originalFileDeserializer.symbolDeserializer.deserializeIrSymbol(idSig, symbolKind).also {
            linker.deserializedSymbols.add(it)
        }
    }

    override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, linker.builtIns, emptyList())

    private val pathToIcFileData = icData.files.associateBy {
        it.file.path
    }

    private val publicSignatureToIcFileDeserializer = mutableMapOf<IdSignature, IcFileDeserializer>()

    private fun deserializeIrFile(
        fileProto: ProtoFile,
        fileIndex: Int,
        moduleDeserializer: IrModuleDeserializer,
        allowErrorNodes: Boolean
    ): IrFile {

        val fileReader = IrLibraryFileFromKlib(moduleDeserializer.klib, fileIndex)
        val file = fileReader.createFile(moduleFragment, fileProto)

        val icFileData = pathToIcFileData[file.path]!!

        val icDeserializer = IcFileDeserializer(
            linker,
            file,
            fileReader,
            fileProto,
            strategy.needBodies,
            allowErrorNodes,
            strategy.inlineBodies,
            moduleDeserializer,
            useGlobalSignatures,
            linker::handleNoModuleDeserializerFound,
            { fileDeserializer -> originalEnqueue(fileDeserializer) },
            icFileData,
            mapping.state,
            publicSignatureToIcFileDeserializer,
            { fileDeserializer -> enqueue(fileDeserializer) },
        )

        icDeserializers += icDeserializer

        icDeserializer.explicitlyExportedToCompiler.forEach { it.topLevelSignature().originalEnqueue(icDeserializer) }

        fileToDeserializerMap[file] = icDeserializer.originalFileDeserializer

        val topLevelDeclarations = icDeserializer.originalFileDeserializer.reversedSignatureIndex.keys
        topLevelDeclarations.forEach {
            moduleReversedFileIndex.putIfAbsent(it, icDeserializer) // TODO Why not simple put?
        }

        if (strategy.theWholeWorld) {
            icDeserializer.allOriginalDeclarationSignatures().forEach { it.originalEnqueue(icDeserializer) }
        }
        if (strategy.theWholeWorld || strategy.explicitlyExported) {
            linker.modulesWithReachableTopLevels.add(this)
        }

        return file
    }

    override fun addModuleReachableTopLevel(idSig: IdSignature) {
        val fileLocalDeserializationState = moduleReversedFileIndex[idSig] ?: error("No file found for key $idSig")
        idSig.originalEnqueue(fileLocalDeserializationState)
    }

    override fun deserializeReachableDeclarations() {
        while (!originalFileQueue.isEmpty()) {
            originalFileQueue.removeFirst().deserializePendingSignatures()
        }
    }

    val originalFileQueue = ArrayDeque<IcFileDeserializer>()

    fun IdSignature.originalEnqueue(fileDeserializer: IcFileDeserializer) {
        if (fileDeserializer.enqueueForDeserialization(this)) {
            originalFileQueue.addLast(fileDeserializer)
        }
    }

    val fileQueue = ArrayDeque<IcFileDeserializer>()
    val signatureQueue = ArrayDeque<IdSignature>()

    val icDeserializers = mutableListOf<IcFileDeserializer>()
    val classToDeclarationSymbols = mutableMapOf<IrClass, List<IrSymbol>>()

    fun IdSignature.enqueue(icDeserializer: IcFileDeserializer) {
        if (this !in icDeserializer.visited) {
            fileQueue.addLast(icDeserializer)
            signatureQueue.addLast(this)
            icDeserializer.visited += this
        }
    }

    override fun postProcess() {
        icDeserializers.forEach { icDeserializer ->
            if (!icDeserializer.visited.isEmpty()) {
                val file = icDeserializer.originalFileDeserializer.file
                icDeserializer.init()
                icDeserializer.reversedSignatureIndex.keys.forEach {
                    if (it in icModuleReversedFileIndex) error("Duplicate signature $it in both ${icModuleReversedFileIndex[it]!!.originalFileDeserializer.file.path} and in ${file.path}")

                    icModuleReversedFileIndex[it] = icDeserializer
                }
            }
        }

        while (signatureQueue.isNotEmpty()) {
            val icFileDeserializer = fileQueue.removeFirst()
            val signature = signatureQueue.removeFirst()

            val declaration = icFileDeserializer.deserializeDeclaration(signature) ?: continue

            icFileDeserializer.injectCarriers(declaration, signature)

            icFileDeserializer.mappingsDeserializer(signature, declaration)

            // Make sure all members are loaded
            if (declaration is IrClass) {
                icFileDeserializer.loadClassOrder(signature)?.let {
                    classToDeclarationSymbols[declaration] = it
                }
            }
        }

        irFactory.stageController.withStage(1000) {

            for (icDeserializer in icDeserializers) {
                val fd = icDeserializer.originalFileDeserializer
                val order = icDeserializer.icFileData.order

                fd.file.declarations.retainAll { it.isEffectivelyExternal() }

                IrLongArrayMemoryReader(order.topLevelSignatures).array.forEach {
                    val symbolData = icDeserializer.symbolDeserializer.parseSymbolData(it)
                    val idSig = icDeserializer.symbolDeserializer.deserializeIdSignature(symbolData.signatureId)

                    // Don't create unbound symbols for top-level declarations we don't need.
                    if (idSig in icDeserializer.visited) {
                        val declaration = icDeserializer.deserializeIrSymbol(idSig, symbolData.kind).owner as IrDeclaration
                        fd.file.declarations += declaration
                    }
                }
            }

            for ((klass, declarations) in classToDeclarationSymbols.entries) {
                irFactory.stageController.unrestrictDeclarationListsAccess {
                    klass.declarations.clear()
                    for (ds in declarations) {
                        klass.declarations += ds.owner as IrDeclaration
                    }
                }
            }
        }
    }
}