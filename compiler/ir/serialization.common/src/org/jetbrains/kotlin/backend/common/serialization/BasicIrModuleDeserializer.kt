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
import org.jetbrains.kotlin.ir.util.StringSignature
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
) :
    IrModuleDeserializer(moduleDescriptor, libraryAbiVersion) {

    private val fileToDeserializerMap = mutableMapOf<IrFile, IrFileDeserializer>()

    private val moduleDeserializationState = ModuleDeserializationState(linker, this)

    val moduleReversedFileIndex = mutableMapOf<StringSignature, FileDeserializationState>()

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

//            val expect = deserializeIdSignature(expectSymbol.signatureId)
            val expect = deserializeSignature(expectSymbol.signatureId)
//            val actual = deserializeIdSignature(actualSymbol.signatureId)
            val actual = deserializeSignature(actualSymbol.signatureId)

            assert(linker.expectUniqIdToActualUniqId[expect] == null) {
                "Expect signature $expect is already actualized by ${linker.expectUniqIdToActualUniqId[expect]}, while we try to record $actual"
            }
            linker.expectUniqIdToActualUniqId[expect] = actual
            // Non-null only for topLevel declarations.
            findModuleDeserializerForTopLevelId(actual)?.let { md -> linker.topLevelActualUniqItToDeserializer[actual] = md }
        }
    }

    override fun referenceSimpleFunctionByLocalSignature(file: IrFile, signature: StringSignature): IrSimpleFunctionSymbol =
        fileToDeserializerMap[file]?.symbolDeserializer?.referenceSimpleFunctionByLocalSignature(signature)
            ?: error("No deserializer for file $file in module ${moduleDescriptor.name}")

    override fun referencePropertyByLocalSignature(file: IrFile, signature: StringSignature): IrPropertySymbol =
        fileToDeserializerMap[file]?.symbolDeserializer?.referencePropertyByLocalSignature(signature)
            ?: error("No deserializer for file $file in module ${moduleDescriptor.name}")

    // TODO: fix to topLevel checker
    override fun contains(signature: StringSignature): Boolean = signature in moduleReversedFileIndex

    override fun deserializeIrSymbol(signature: StringSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        val topLevelSignature = signature.topLevelSignature()
        val fileLocalDeserializationState = moduleReversedFileIndex[topLevelSignature]
            ?: error("No file for $topLevelSignature (@ $signature) in module $moduleDescriptor")

        fileLocalDeserializationState.addIdSignature(topLevelSignature)
        moduleDeserializationState.enqueueFile(fileLocalDeserializationState)

        return fileLocalDeserializationState.fileDeserializer.symbolDeserializer.deserializeIrSymbol(signature, symbolKind)
    }

    override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, linker.builtIns, emptyList())

    private fun deserializeIrFile(fileProto: ProtoFile, fileIndex: Int, moduleDeserializer: IrModuleDeserializer, allowErrorNodes: Boolean): IrFile {

        val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(moduleDeserializer.klib, fileIndex))
        val file = fileReader.createFile(moduleFragment, fileProto)
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

        val topLevelDeclarations = fileDeserializationState.fileDeserializer.reversedSignatureIndex.keys
        topLevelDeclarations.forEach {
            moduleReversedFileIndex[it] = fileDeserializationState
        }

        if (fileStrategy.theWholeWorld) {
            fileDeserializationState.enqueueAllDeclarations()
        }
        if (fileStrategy.theWholeWorld || fileStrategy.explicitlyExported) {
            moduleDeserializationState.enqueueFile(fileDeserializationState)
        }

        return file
    }

    override fun addModuleReachableTopLevel(signature: StringSignature) {
        moduleDeserializationState.addIdSignature(signature)
    }

    override fun deserializeReachableDeclarations() {
        moduleDeserializationState.deserializeReachableDeclarations()
    }

    override fun signatureDeserializerForFile(fileName: String): IdSignatureDeserializer {
        val fileDeserializer = fileToDeserializerMap.entries.find { it.key.fileEntry.name == fileName }?.value
            ?: error("No file deserializer for $fileName")

        TODO("?/")
//        return fileDeserializer.symbolDeserializer.signatureDeserializer
    }

    override val kind get() = IrModuleDeserializerKind.DESERIALIZED
}

private class ModuleDeserializationState(val linker: KotlinIrLinker, val moduleDeserializer: BasicIrModuleDeserializer) {
    private val filesWithPendingTopLevels = mutableSetOf<FileDeserializationState>()

    fun enqueueFile(fileDeserializationState: FileDeserializationState) {
        filesWithPendingTopLevels.add(fileDeserializationState)
        linker.modulesWithReachableTopLevels.add(moduleDeserializer)
    }

    fun addIdSignature(key: StringSignature) {
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

fun IrModuleDeserializer.findModuleDeserializerForTopLevelId(signature: StringSignature): IrModuleDeserializer? {
    if (signature in this) return this
    return moduleDependencies.firstOrNull { signature in it }
}

val ByteArray.codedInputStream: CodedInputStream
    get() {
        val codedInputStream = CodedInputStream.newInstance(this)
        codedInputStream.setRecursionLimit(65535) // The default 64 is blatantly not enough for IR.
        return codedInputStream
    }