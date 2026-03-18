/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrDeserializationSettings.DeserializeFunctionBodies
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.components.KlibIrComponent
import org.jetbrains.kotlin.library.components.irOrFail
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite

import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

/**
 * @property allowErrorNodes Whether error nodes are allowed during IR deserialization and initialization.
 *   Caution: This setting is not safe to use, as it can lead to crashes in the frontend or backend.
 *   The only legal case for using this setting is the `dump-ir` command of the `klib` command-line tool.
 */
abstract class BasicIrModuleDeserializer(
    val linker: KotlinIrLinker,
    moduleDescriptor: ModuleDescriptor,
    override val strategyResolver: (String) -> DeserializationStrategy,
    libraryAbiVersion: KotlinAbiVersion,
    private val allowErrorNodes: Boolean = false,
) : IrModuleDeserializer(moduleDescriptor, libraryAbiVersion) {

    private val fileToDeserializerMap = mutableMapOf<IrFile, IrFileDeserializer>()

    private val moduleDeserializationState = ModuleDeserializationState()

    protected lateinit var fileDeserializationStates: List<FileDeserializationState>

    protected val moduleReversedFileIndex = hashMapOf<IdSignature, FileDeserializationState>()

    protected open val ir: KlibIrComponent get() = klib.irOrFail

    override val moduleDependencies by lazy {
        moduleDescriptor.allDependencyModules
            .filter { it != moduleDescriptor }
            .map { linker.resolveModuleDeserializer(it, null) }
    }

    override fun fileDeserializers(): Collection<IrFileDeserializer> {
        return fileToDeserializerMap.values.filterNot { strategyResolver(it.file.fileEntry.name).onDemand }
    }

    override fun init(delegate: IrModuleDeserializer) {
        val fileCount = ir.irFileCount
        fileDeserializationStates = buildList {
            for (i in 0 until fileCount) {
                val fileStream = ir.irFile(i).codedInputStream
                val fileProto = ProtoFile.parseFrom(fileStream, ExtensionRegistryLite.getEmptyRegistry())
                val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(ir, i))
                val file = fileReader.createFile(moduleFragment, fileProto, linker.fileEntryDeserializer)

                this += deserializeIrFile(fileProto, file, fileReader, i, delegate, allowErrorNodes)

                if (!strategyResolver(file.fileEntry.name).onDemand)
                    moduleFragment.files.add(file)
            }
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

        return fileLocalDeserializationState.fileDeserializer.symbolDeserializer.deserializeSymbolWithOwnerInCurrentFile(idSig, symbolKind)
    }

    override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing {
        error("No file for ${idSig.topLevelSignature()} (@ $idSig) in module $moduleDescriptor")
    }

    override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor)

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
            IrDeserializationSettings(
                allowErrorNodes = allowErrorNodes,
                deserializeFunctionBodies = when {
                    fileStrategy.needBodies -> DeserializeFunctionBodies.ALL
                    fileStrategy.inlineBodies -> DeserializeFunctionBodies.ONLY_INLINE
                    else -> DeserializeFunctionBodies.NONE
                }
            ),
            moduleDeserializer,
        )

        fileToDeserializerMap[file] = fileDeserializationState.fileDeserializer

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

        return fileDeserializationState
    }

    /**
     * Schedule deserialization of the top-level declaration with the given signature in the given file.
     */
    override fun addModuleReachableTopLevel(topLevelDeclarationSignature: IdSignature) {
        moduleDeserializationState.addIdSignature(topLevelDeclarationSignature)
    }

    /**
     * Run deserialization of top-level declarations previously scheduled for deserialization in the current module.
     */
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
        /**
         * This is the queue of files containing top-level declarations to be deserialized. This is
         * the second-layer queue on top of [FileDeserializationState.reachableTopLevels].
         *
         * A file can be enqueued using one of the available ways: [enqueueFile], [addIdSignature].
         *
         * The deserialization happens on invocation of [deserializeReachableDeclarations]. This in its turn
         * invokes [FileDeserializationState.deserializeAllFileReachableTopLevel] for each scheduled file.
         *
         * Note: A file is removed from the queue after all top-level declarations scheduled for
         * deserialization in that file have been actually deserialized. Later the file can be enqueued
         * once again to deserialize other top-level declaration(s). This process can be repeated multiple times.
         */
        private val filesWithPendingTopLevels = mutableSetOf<FileDeserializationState>()

        /**
         * Enqueue the given file for deserialization of (some) top-level declarations.
         *
         * Note: The declarations that need to be deserialized should be enqueued separately using
         * [FileDeserializationState.addIdSignature] call.
         */
        fun enqueueFile(fileDeserializationState: FileDeserializationState) {
            filesWithPendingTopLevels.add(fileDeserializationState)
            linker.modulesWithReachableTopLevels.add(this@BasicIrModuleDeserializer)
        }

        /**
         * Schedule deserialization of the top-level declaration with the given signature in the given file.
         */
        fun addIdSignature(topLevelDeclarationSignature: IdSignature) {
            val fileLocalDeserializationState = moduleReversedFileIndex[topLevelDeclarationSignature]
                ?: error("No IR file found for top-level declaration signature $topLevelDeclarationSignature")
            fileLocalDeserializationState.addIdSignature(topLevelDeclarationSignature)

            enqueueFile(fileLocalDeserializationState)
        }

        /**
         * Run deserialization of top-level declarations previously scheduled for deserialization in the current module.
         */
        fun deserializeReachableDeclarations() {
            while (filesWithPendingTopLevels.isNotEmpty()) {
                val pendingFileDeserializationState = filesWithPendingTopLevels.first()

                if (pendingFileDeserializationState.fileDeserializer.deserializeFileImplicitDataIfFirstUse()) {
                    // Schedule the IR file for processing by the PL engine only when the implicit file data
                    // is deserialized for the first time.
                    //
                    // Note: Enqueueing the file does not mean all top-level declarations in this file are
                    // also enqueued. This is done separately in `FileDeserializationState.deserializeAllFileReachableTopLevel()`.
                    linker.partialLinkageSupport.enqueueFile(pendingFileDeserializationState.file)
                }
                pendingFileDeserializationState.deserializeAllFileReachableTopLevel()

                filesWithPendingTopLevels.remove(pendingFileDeserializationState)
            }
        }
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