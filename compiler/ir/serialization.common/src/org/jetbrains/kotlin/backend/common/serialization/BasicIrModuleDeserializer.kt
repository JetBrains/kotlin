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
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite

import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

abstract class BasicIrModuleDeserializer(
    val linker: KotlinIrLinker,
    moduleDescriptor: ModuleDescriptor,
    override val klib: IrLibrary,
    override val strategy: DeserializationStrategy,
    val handleNoModuleDeserializerFound: (IdSignature) -> IrModuleDeserializer,
    val resolveModuleDeserializer: (ModuleDescriptor) -> IrModuleDeserializer,
    private val containsErrorCode: Boolean = false
) :
    IrModuleDeserializer(moduleDescriptor) {

    private val fileDeserializers = mutableListOf<IrFileDeserializer>()

    private val moduleDeserializationState = ModuleDeserializationState(linker, this)

    internal val moduleReversedFileIndex = mutableMapOf<IdSignature, FileDeserializationState>()

    override val moduleDependencies by lazy {
        moduleDescriptor.allDependencyModules.filter { it != moduleDescriptor }.map { resolveModuleDeserializer(it) }
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

        fileDeserializers.forEach { it.symbolDeserializer.deserializeExpectActualMapping() }
    }

    // TODO: fix to topLevel checker
    override fun contains(idSig: IdSignature): Boolean = idSig in moduleReversedFileIndex

    override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        assert(idSig.isPublic)

        val topLevelSignature = idSig.topLevelSignature()
        val fileLocalDeserializationState = moduleReversedFileIndex[topLevelSignature]
            ?: error("No file for $topLevelSignature (@ $idSig) in module $moduleDescriptor")

        fileLocalDeserializationState.addIdSignature(topLevelSignature)
        moduleDeserializationState.enqueueFile(fileLocalDeserializationState)

        return fileLocalDeserializationState.fileDeserializer.symbolDeserializer.deserializeIrSymbol(idSig, symbolKind).also {
            linker.deserializedSymbols.add(it)
        }
    }

    override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, linker.builtIns, emptyList())

    private fun deserializeIrFile(fileProto: ProtoFile, fileIndex: Int, moduleDeserializer: IrModuleDeserializer, allowErrorNodes: Boolean): IrFile {

        val fileReader = IrLibraryFile(moduleDeserializer.klib, fileIndex)
        val file = fileReader.createFile(moduleDescriptor, fileProto)

        val fileDeserializationState = FileDeserializationState(
            linker,
            file,
            fileReader,
            fileProto,
            strategy.needBodies,
            allowErrorNodes,
            strategy.inlineBodies,
            moduleDeserializer,
            handleNoModuleDeserializerFound,
        )

        fileDeserializers += fileDeserializationState.fileDeserializer

        val topLevelDeclarations = fileDeserializationState.fileDeserializer.reversedSignatureIndex.keys
        topLevelDeclarations.forEach {
            moduleReversedFileIndex.putIfAbsent(it, fileDeserializationState) // TODO Why not simple put?
        }

        if (strategy.theWholeWorld) {
            fileDeserializationState.enqueueAllDeclarations()
        }
        if (strategy.theWholeWorld || strategy.explicitlyExported) {
            moduleDeserializationState.enqueueFile(fileDeserializationState)
        }

        return file
    }

    // TODO useless
    override fun addModuleReachableTopLevel(idSig: IdSignature) {
        moduleDeserializationState.addIdSignature(idSig)
    }
}

internal class ModuleDeserializationState(val linker: KotlinIrLinker, val moduleDeserializer: BasicIrModuleDeserializer) {
    private val filesWithPendingTopLevels = mutableSetOf<FileDeserializationState>()

    fun enqueueFile(fileDeserializationState: FileDeserializationState) {
        filesWithPendingTopLevels.add(fileDeserializationState)
        linker.modulesWithReachableTopLevels.add(this)
    }

    fun addIdSignature(key: IdSignature) {
        val fileLocalDeserializationState = moduleDeserializer.moduleReversedFileIndex[key] ?: error("No file found for key $key")
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
}