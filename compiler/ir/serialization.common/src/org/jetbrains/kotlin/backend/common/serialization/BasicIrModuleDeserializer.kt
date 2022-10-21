/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite

import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

abstract class BasicIrModuleDeserializer(
    val linker: KotlinIrLinker,
    moduleDescriptor: ModuleDescriptor,
    override val klib: IrLibrary,
    override val strategyResolver: (String) -> DeserializationStrategy,
    libraryAbiVersion: KotlinAbiVersion,
    private val containsErrorCode: Boolean = false
) : IrModuleDeserializer(moduleDescriptor, libraryAbiVersion) {

    private val fileToDeserializerMap = mutableMapOf<IrFile, IrFileDeserializer>()

    private val moduleDeserializationState = ModuleDeserializationState()

    protected val moduleReversedFileIndex = mutableMapOf<IdSignature, FileDeserializationState>()

    override val moduleDependencies by lazy {
        moduleDescriptor.allDependencyModules
            .filter { it != moduleDescriptor }
            .map { linker.resolveModuleDeserializer(it, null) }
    }

    override fun fileDeserializers(): Collection<IrFileDeserializer> {
        return fileToDeserializerMap.values.filterNot { strategyResolver(it.file.fileEntry.name).onDemand }
    }

    protected lateinit var fileDeserializationStates: List<FileDeserializationState>

    override fun init(delegate: IrModuleDeserializer) {
        val fileCount = klib.fileCount()

        val fileDeserializationStates = mutableListOf<FileDeserializationState>()

        for (i in 0 until fileCount) {
            val fileStream = klib.file(i).codedInputStream
            val fileProto = ProtoFile.parseFrom(fileStream, ExtensionRegistryLite.newInstance())
            val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(klib, i))
            val file = fileReader.createFile(moduleFragment, fileProto)

            fileDeserializationStates.add(deserializeIrFile(fileProto, file, fileReader, i, delegate, containsErrorCode))
            if (!strategyResolver(file.fileEntry.name).onDemand)
                moduleFragment.files.add(file)
        }

        this.fileDeserializationStates = fileDeserializationStates

        fileToDeserializerMap.values.forEach { it.symbolDeserializer.deserializeExpectActualMapping() }
    }

    private fun IrSymbolDeserializer.deserializeExpectActualMapping() {
        actuals.forEach {
            val expectSymbol = parseSymbolData(it.expectSymbol)
            val actualSymbol = parseSymbolData(it.actualSymbol)

            val expect = deserializeIdSignature(expectSymbol.signatureId)
            val actual = deserializeIdSignature(actualSymbol.signatureId)

            assert(linker.expectIdSignatureToActualIdSignature[expect] == null) {
                "Expect signature $expect is already actualized by ${linker.expectIdSignatureToActualIdSignature[expect]}, while we try to record $actual"
            }
            linker.expectIdSignatureToActualIdSignature[expect] = actual
            // Non-null only for topLevel declarations.
            findModuleDeserializerForTopLevelId(actual)?.let { md -> linker.topLevelActualIdSignatureToModuleDeserializer[actual] = md }
        }
    }

    override fun referenceSimpleFunctionByLocalSignature(file: IrFile, idSignature: IdSignature) : IrSimpleFunctionSymbol =
        fileToDeserializerMap[file]?.symbolDeserializer?.referenceSimpleFunctionByLocalSignature(idSignature)
            ?: error("No deserializer for file $file in module ${moduleDescriptor.name}")

    override fun referencePropertyByLocalSignature(file: IrFile, idSignature: IdSignature): IrPropertySymbol =
        fileToDeserializerMap[file]?.symbolDeserializer?.referencePropertyByLocalSignature(idSignature)
            ?: error("No deserializer for file $file in module ${moduleDescriptor.name}")

    // TODO: fix to topLevel checker
    override fun contains(idSig: IdSignature): Boolean = idSig in moduleReversedFileIndex

    override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
        val topLevelSignature = idSig.topLevelSignature()
        val fileLocalDeserializationState = moduleReversedFileIndex[topLevelSignature] ?: return null

        fileLocalDeserializationState.addIdSignature(topLevelSignature)
        moduleDeserializationState.enqueueFile(fileLocalDeserializationState)

        return fileLocalDeserializationState.fileDeserializer.symbolDeserializer.deserializeIrSymbol(idSig, symbolKind)
    }

    override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing {
        error("No file for ${idSig.topLevelSignature()} (@ $idSig) in module $moduleDescriptor")
    }

    override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, linker.builtIns, emptyList())

    private fun deserializeIrFile(
        fileProto: ProtoFile, file: IrFile, fileReader: IrLibraryFileFromBytes,
        fileIndex: Int, moduleDeserializer: IrModuleDeserializer, allowErrorNodes: Boolean
    ): FileDeserializationState {
        val fileStrategy = strategyResolver(file.fileEntry.name)

        val fileDeserializationState = FileDeserializationState(
            linker,
            fileIndex,
            file,
            fileReader,
            fileProto,
            fileStrategy.needBodies,
            allowErrorNodes,
            fileStrategy.inlineBodies,
            moduleDeserializer
        )

        fileToDeserializerMap[file] = fileDeserializationState.fileDeserializer

        if (!fileStrategy.onDemand) {
            val topLevelDeclarations = fileDeserializationState.fileDeserializer.reversedSignatureIndex.keys
            topLevelDeclarations.forEach {
                moduleReversedFileIndex.putIfAbsent(it, fileDeserializationState) // TODO Why not simple put?
            }

            if (fileStrategy.theWholeWorld) {
                fileDeserializationState.enqueueAllDeclarations()
            }
            if (fileStrategy.theWholeWorld || fileStrategy.explicitlyExported) {
                moduleDeserializationState.enqueueFile(fileDeserializationState)
            }
        }

        return fileDeserializationState
    }

    override fun addModuleReachableTopLevel(idSig: IdSignature) {
        moduleDeserializationState.addIdSignature(idSig)
    }

    override fun deserializeReachableDeclarations() {
        moduleDeserializationState.deserializeReachableDeclarations()
    }

    override fun signatureDeserializerForFile(fileName: String): IdSignatureDeserializer {
        val fileDeserializer = fileToDeserializerMap.entries.find { it.key.fileEntry.name == fileName }?.value
            ?: error("No file deserializer for $fileName")

        return fileDeserializer.symbolDeserializer.signatureDeserializer
    }

    override val kind get() = IrModuleDeserializerKind.DESERIALIZED

    private inner class ModuleDeserializationState {
        private val filesWithPendingTopLevels = mutableSetOf<FileDeserializationState>()

        fun enqueueFile(fileDeserializationState: FileDeserializationState) {
            filesWithPendingTopLevels.add(fileDeserializationState)
            linker.modulesWithReachableTopLevels.add(this@BasicIrModuleDeserializer)
        }

        fun addIdSignature(key: IdSignature) {
            val fileLocalDeserializationState = moduleReversedFileIndex[key] ?: error("No file found for key $key")
            fileLocalDeserializationState.addIdSignature(key)

            enqueueFile(fileLocalDeserializationState)
        }

        fun deserializeReachableDeclarations() {
            while (filesWithPendingTopLevels.isNotEmpty()) {
                val pendingFileDeserializationState = filesWithPendingTopLevels.first()

                pendingFileDeserializationState.fileDeserializer.deserializeFileImplicitDataIfFirstUse()
                pendingFileDeserializationState.deserializeAllFileReachableTopLevel()

                filesWithPendingTopLevels.remove(pendingFileDeserializationState)
            }
        }

        override fun toString(): String = klib.toString()
    }
}

fun IrModuleDeserializer.findModuleDeserializerForTopLevelId(idSignature: IdSignature): IrModuleDeserializer? {
    if (idSignature in this) return this
    return moduleDependencies.firstOrNull { idSignature in it }
}

val ByteArray.codedInputStream: CodedInputStream
    get() {
        val codedInputStream = CodedInputStream.newInstance(this)
        codedInputStream.setRecursionLimit(65535) // The default 64 is blatantly not enough for IR.
        return codedInputStream
    }