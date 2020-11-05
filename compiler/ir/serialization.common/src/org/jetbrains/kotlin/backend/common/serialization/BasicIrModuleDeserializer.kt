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

    private val fileToDeserializerMap = mutableMapOf<IrFile, IrFileDeserializer>()

    private inner class ModuleDeserializationState {
        private val filesWithPendingTopLevels = mutableSetOf<IrFileDeserializer>()

        fun enqueueFile(fileDeserializer: IrFileDeserializer) {
            filesWithPendingTopLevels.add(fileDeserializer)
            enqueueModule()
        }

        fun addIdSignature(key: IdSignature) {
            val fileDeserializer = moduleReversedFileIndex[key] ?: error("No file found for key $key")
            fileDeserializer.fileLocalDeserializationState.addIdSignature(key)

            enqueueFile(fileDeserializer)
        }

        fun processPendingDeclarations() {
            while (filesWithPendingTopLevels.isNotEmpty()) {
                val pendingDeserializer = filesWithPendingTopLevels.first()

                pendingDeserializer.deserializeFileImplicitDataIfFirstUse()
                pendingDeserializer.deserializeAllFileReachableTopLevel()

                filesWithPendingTopLevels.remove(pendingDeserializer)
            }
        }
    }

    private val moduleDeserializationState = ModuleDeserializationState()
    private val moduleReversedFileIndex = mutableMapOf<IdSignature, IrFileDeserializer>()
    override val moduleDependencies by lazy {
        moduleDescriptor.allDependencyModules.filter { it != moduleDescriptor }.map { resolveModuleDeserializer(it) }
    }

    override fun init(delegate: IrModuleDeserializer) {
        val fileCount = klib.fileCount()

        val files = ArrayList<IrFile>(fileCount)

        for (i in 0 until fileCount) {
            val fileStream = klib.file(i).codedInputStream
            files.add(deserializeIrFile(
                org.jetbrains.kotlin.backend.common.serialization.proto.IrFile.parseFrom(
                    fileStream,
                    ExtensionRegistryLite.newInstance()
                ), i, delegate, containsErrorCode))
        }

        moduleFragment.files.addAll(files)

        fileToDeserializerMap.values.forEach { it.deserializeExpectActualMapping() }
    }

    // TODO: fix to topLevel checker
    override fun contains(idSig: IdSignature): Boolean = idSig in moduleReversedFileIndex

    override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        assert(idSig.isPublic)

        val topLevelSignature = idSig.topLevelSignature()
        val fileDeserializer = moduleReversedFileIndex[topLevelSignature]
            ?: error("No file for $topLevelSignature (@ $idSig) in module $moduleDescriptor")

        val fileDeserializationState = fileDeserializer.fileLocalDeserializationState

        fileDeserializationState.addIdSignature(topLevelSignature)
        moduleDeserializationState.enqueueFile(fileDeserializer)

        return fileDeserializer.symbolDeserializer.deserializeIrSymbol(idSig, symbolKind).also {
            linker.haveSeen.add(it)
        }
    }

    override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, linker.builtIns, emptyList())

    private fun deserializeIrFile(
        fileProto: org.jetbrains.kotlin.backend.common.serialization.proto.IrFile,
        fileIndex: Int,
        moduleDeserializer: IrModuleDeserializer,
        allowErrorNodes: Boolean
    ): IrFile {

        val fileReader = IrLibraryFile(moduleDeserializer.klib, fileIndex)
        val file = fileReader.createFile(moduleDescriptor, fileProto)

        val fileDeserializer =
            IrFileDeserializer(
                linker.logger,
                linker.builtIns,
                linker.symbolTable,
                file,
                fileReader,
                fileProto.explicitlyExportedToCompilerList,
                fileProto.declarationIdList,
                strategy.needBodies,
                linker.deserializeFakeOverrides,
                linker.fakeOverrideClassQueue,
                allowErrorNodes,
                fileProto.annotationList,
                fileProto.actualsList,
                strategy.inlineBodies,
                moduleDeserializer,
                linker.fakeOverrideBuilder,
                linker.expectUniqIdToActualUniqId,
                linker.expectSymbols,
                linker.actualSymbols,
                linker.topLevelActualUniqItToDeserializer,
                handleNoModuleDeserializerFound,
            )

        fileToDeserializerMap[file] = fileDeserializer

        val topLevelDeclarations = fileDeserializer.reversedSignatureIndex.keys
        topLevelDeclarations.forEach {
            moduleReversedFileIndex.putIfAbsent(it, fileDeserializer) // TODO Why not simple put?
        }

        if (strategy.theWholeWorld) {
            fileDeserializer.enqueueAllDeclarations()
            moduleDeserializationState.enqueueFile(fileDeserializer)
        } else if (strategy.explicitlyExported) {
            moduleDeserializationState.enqueueFile(fileDeserializer)
        }

        return file
    }

    override fun deserializeReachableDeclarations() {
        moduleDeserializationState.processPendingDeclarations()
    }

    private fun enqueueModule() {
        linker.modulesWithReachableTopLevels.add(this)
    }

    override fun addModuleReachableTopLevel(idSig: IdSignature) {
        moduleDeserializationState.addIdSignature(idSig)
    }
}