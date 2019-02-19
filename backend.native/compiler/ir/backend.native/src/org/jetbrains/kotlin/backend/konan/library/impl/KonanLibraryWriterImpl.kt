/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.library.impl

import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryWriter
import org.jetbrains.kotlin.backend.konan.library.LinkData
import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.konan.properties.*
import org.jetbrains.kotlin.konan.target.KonanTarget

abstract class FileBasedLibraryWriter (val file: File): KonanLibraryWriter

/**
 * Requires non-null [target].
 */
class LibraryWriterImpl(
        override val libDir: File,
        moduleName: String,
        override val versions: KonanLibraryVersioning,
        override val target: KonanTarget,
        val nopack: Boolean = false
): FileBasedLibraryWriter(libDir), KonanLibraryLayout {

    constructor(path: String, moduleName: String, versions: KonanLibraryVersioning, target: KonanTarget, nopack: Boolean):
        this(File(path), moduleName, versions, target, nopack)

    override val libraryName = libDir.path
    val klibFile 
       get() = File("${libDir.path}.$KLIB_FILE_EXTENSION")

    // TODO: Experiment with separate bitcode files.
    // Per package or per class.
    val mainBitcodeFile = File(kotlinDir, "program.kt.bc")
    override val mainBitcodeFileName = mainBitcodeFile.path
    val manifestProperties = Properties()

    init {
        // TODO: figure out the proper policy here.
        libDir.deleteRecursively()
        klibFile.delete()
        libDir.mkdirs()
        linkdataDir.mkdirs()
        targetDir.mkdirs()
        kotlinDir.mkdirs()
        nativeDir.mkdirs()
        includedDir.mkdirs()
        resourcesDir.mkdirs()
        irDir.mkdirs()
        // TODO: <name>:<hash> will go somewhere around here.
        manifestProperties.setProperty(KLIB_PROPERTY_UNIQUE_NAME, moduleName)
        manifestProperties.writeKonanLibraryVersioning(versions)
    }

    var llvmModule: LLVMModuleRef? = null

    override fun addKotlinBitcode(llvmModule: LLVMModuleRef) {
        this.llvmModule = llvmModule
        LLVMWriteBitcodeToFile(llvmModule, mainBitcodeFileName)
    }

    override fun addLinkData(linkData: LinkData) {
        MetadataWriterImpl(this).addLinkData(linkData)
    }

    override fun addNativeBitcode(library: String) {
        val basename = File(library).name
        File(library).copyTo(File(nativeDir, basename)) 
    }

    override fun addIncludedBinary(library: String) {
        val basename = File(library).name
        File(library).copyTo(File(includedDir, basename)) 
    }

    override fun addLinkDependencies(libraries: List<KonanLibrary>) {
        if (libraries.isEmpty()) {
            manifestProperties.remove(KLIB_PROPERTY_DEPENDS)
            // make sure there are no leftovers from the .def file.
            return
        } else {
            val newValue = libraries.joinToString(" ") { it.uniqueName }
            manifestProperties.setProperty(KLIB_PROPERTY_DEPENDS, newValue)
            libraries.forEach { it ->
                if (it.versions.libraryVersion != null) {
                    manifestProperties.setProperty("${KLIB_PROPERTY_DEPENDENCY_VERSION}_${it.uniqueName}", it.versions.libraryVersion)
                }
            }
        }
    }

    override fun addManifestAddend(properties: Properties) {
        manifestProperties.putAll(properties)
    }

    override fun addDataFlowGraph(dataFlowGraph: ByteArray) {
        dataFlowGraphFile.writeBytes(dataFlowGraph)
    }

    override fun commit() {
        manifestProperties.saveToFile(manifestFile)
        if (!nopack) {
            libDir.zipDirAs(klibFile)
            libDir.deleteRecursively()
        }
    }
}

internal fun buildLibrary(
        natives: List<String>,
        included: List<String>,
        linkDependencies: List<KonanLibrary>,
        linkData: LinkData,
        versions: KonanLibraryVersioning,
        target: KonanTarget,
        output: String,
        moduleName: String,
        llvmModule: LLVMModuleRef?,
        nopack: Boolean,
        manifestProperties: Properties?,
        dataFlowGraph: ByteArray?): KonanLibraryWriter {

    val library = LibraryWriterImpl(File(output), moduleName, versions, target, nopack)

    llvmModule?.let { library.addKotlinBitcode(it) }
    library.addLinkData(linkData)
    natives.forEach {
        library.addNativeBitcode(it)
    }
    included.forEach {
        library.addIncludedBinary(it)
    }
    manifestProperties?.let { library.addManifestAddend(it) }
    library.addLinkDependencies(linkDependencies)
    dataFlowGraph?.let { library.addDataFlowGraph(it) }

    library.commit()
    return library
}

