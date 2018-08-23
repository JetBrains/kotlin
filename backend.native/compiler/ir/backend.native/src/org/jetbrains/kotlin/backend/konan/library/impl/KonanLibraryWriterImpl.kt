/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.library.impl

import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryWriter
import org.jetbrains.kotlin.backend.konan.library.LinkData
import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.properties.*
import org.jetbrains.kotlin.konan.target.KonanTarget

abstract class FileBasedLibraryWriter (val file: File, val currentAbiVersion: Int): KonanLibraryWriter

/**
 * Requires non-null [target].
 */
class LibraryWriterImpl(
        override val libDir: File,
        moduleName: String,
        currentAbiVersion: Int,
        override val target: KonanTarget,
        val nopack: Boolean = false
): FileBasedLibraryWriter(libDir, currentAbiVersion), KonanLibraryLayout {

    constructor(path: String, moduleName: String, currentAbiVersion: Int, target: KonanTarget, nopack: Boolean):
        this(File(path), moduleName, currentAbiVersion, target, nopack)

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
        // TODO: <name>:<hash> will go somewhere around here.
        manifestProperties.setProperty(KLIB_PROPERTY_UNIQUE_NAME, moduleName)
        manifestProperties.setProperty(KLIB_PROPERTY_ABI_VERSION, currentAbiVersion.toString())
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
        }
    }

    override fun addManifestAddend(path: String) {
        val properties = File(path).loadProperties()
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
        abiVersion: Int,
        target: KonanTarget,
        output: String,
        moduleName: String,
        llvmModule: LLVMModuleRef,
        nopack: Boolean,
        manifest: String?,
        dataFlowGraph: ByteArray?): KonanLibraryWriter {

    val library = LibraryWriterImpl(output, moduleName, abiVersion, target, nopack)

    library.addKotlinBitcode(llvmModule)
    library.addLinkData(linkData)
    natives.forEach {
        library.addNativeBitcode(it)
    }
    included.forEach {
        library.addIncludedBinary(it)
    }
    manifest ?.let { library.addManifestAddend(it) }
    library.addLinkDependencies(linkDependencies)
    dataFlowGraph?.let { library.addDataFlowGraph(it) }

    library.commit()
    return library
}

