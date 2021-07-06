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
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite

import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

abstract class BasicIrModuleDeserializer(
    val linker: KotlinIrLinker,
    moduleDescriptor: ModuleDescriptor,
    override val klib: IrLibrary,
    override val strategy: DeserializationStrategy,
    private val containsErrorCode: Boolean = false,
    private val useGlobalSignatures: Boolean = false,
) :
    IrModuleDeserializer(moduleDescriptor) {

    private val fileToDeserializerMap = mutableMapOf<IrFile, IrFileDeserializer>()

    private val moduleDeserializationState = ModuleDeserializationState(linker, this)

    internal val moduleReversedFileIndex = mutableMapOf<IdSignature, FileDeserializationState>()

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

    override fun referenceSimpleFunctionByLocalSignature(file: IrFile, idSignature: IdSignature) : IrSimpleFunctionSymbol =
        fileToDeserializerMap[file]?.symbolDeserializer?.referenceSimpleFunctionByLocalSignature(idSignature)
            ?: error("No deserializer for file $file in module ${moduleDescriptor.name}")

    override fun referencePropertyByLocalSignature(file: IrFile, idSignature: IdSignature): IrPropertySymbol =
        fileToDeserializerMap[file]?.symbolDeserializer?.referencePropertyByLocalSignature(idSignature)
            ?: error("No deserializer for file $file in module ${moduleDescriptor.name}")

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

        val fileReader = IrLibraryFileFromKlib(moduleDeserializer.klib, fileIndex)
        val file = fileReader.createFile(moduleFragment, fileProto)

        val fileDeserializationState = FileDeserializationState(
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
        )

        fileToDeserializerMap[file] = fileDeserializationState.fileDeserializer

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

    override fun addModuleReachableTopLevel(idSig: IdSignature) {
        moduleDeserializationState.addIdSignature(idSig)
    }

    override fun deserializeReachableDeclarations() {
        moduleDeserializationState.deserializeReachableDeclarations()
    }
}

private class ModuleDeserializationState(val linker: KotlinIrLinker, val moduleDeserializer: BasicIrModuleDeserializer) {
    private val filesWithPendingTopLevels = mutableSetOf<FileDeserializationState>()

    fun enqueueFile(fileDeserializationState: FileDeserializationState) {
        filesWithPendingTopLevels.add(fileDeserializationState)
        linker.modulesWithReachableTopLevels.add(moduleDeserializer)
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

    override fun toString(): String = moduleDeserializer.klib.toString()
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