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

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMLinkModules2
import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.serialization.Base64
import org.jetbrains.kotlin.backend.konan.serialization.deserializeModule
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import java.io.File

interface KonanLibraryReader {
    val libraryName: String
    val moduleName: String
    val moduleDescriptor: ModuleDescriptorImpl
    val bitcodePaths: List<String>
}

abstract class FileBasedLibraryReader(
    val file: File, 
    val configuration: CompilerConfiguration, 
    val reader: MetadataReader): KonanLibraryReader {

    override val libraryName: String
        get() = file.path

    protected val namedModuleData by lazy {
        val currentAbiVersion = configuration.get(KonanConfigKeys.ABI_VERSION)!!
        reader.loadSerializedModule(currentAbiVersion)
    }

    override val moduleName: String
        get() = namedModuleData.name

    protected val tableOfContentsAsString : String
        get() = namedModuleData.base64

    protected fun packageMetadata(fqName: String): Base64 =
        reader.loadSerializedPackageFragment(fqName)

    override val moduleDescriptor: ModuleDescriptorImpl by lazy {
        deserializeModule(configuration, 
            {it -> packageMetadata(it)}, 
            tableOfContentsAsString, moduleName)
    }
}

class KtBcLibraryReader(file: File, configuration: CompilerConfiguration) 
    : FileBasedLibraryReader(file, configuration, KtBcMetadataReader(file)) {

    public constructor(path: String, configuration: CompilerConfiguration) : this(File(path), configuration) 

    override val bitcodePaths: List<String>
        get() = listOf(libraryName)

}

// TODO: Get rid of the configuration here.
class SplitLibraryReader(val libDir: File, configuration: CompilerConfiguration) 
    : FileBasedLibraryReader(libDir, configuration, SplitMetadataReader(libDir)) {

    public constructor(path: String, configuration: CompilerConfiguration) : this(File(path), configuration) 

    val klibFile = File("${libDir.path}.klib")

    init {
        unpackIfNeeded()
    }

    // TODO: Search path processing is also goes somewhere around here.
    fun unpackIfNeeded() {
        // TODO: Clarify the policy here.
        if (libDir.exists()) {
            if (libDir.isDirectory()) return
        }
        if (!klibFile.exists()) {
            error("Could not find neither $libDir nor $klibFile.")
        }
        if (klibFile.isFile()) {
            klibFile.unzipAs(libDir)

            if (!libDir.exists()) error("Could not unpack $klibFile as $libDir.")
        } else {
            error("Expected $klibFile to be a regular libDir.")
        }
    }

    private val File.dirAbsolutePaths: List<String>
        get() = this.listFiles()!!.toList()!!.map{it->it.absolutePath}

    private val targetDir: File
        get() {
            val target = TargetManager(configuration).currentName
            val dir = File(libDir, target)
            return dir
        }

    override val bitcodePaths: List<String>
        get() = File(targetDir, "kotlin").dirAbsolutePaths + 
                File(targetDir, "native").dirAbsolutePaths

}
/* ------------ writer part ----------------*/

interface KonanLibraryWriter {

    fun addLinkData(linkData: LinkData)
    fun addNativeBitcode(library: String)
    fun addKotlinBitcode(llvmModule: LLVMModuleRef)
    fun commit()

    val mainBitcodeFileName: String
}

class LinkData(
    val abiVersion: Int,
    val module: String,
    val moduleName: String,
    val fragments: List<String>,
    val fragmentNames: List<String> )

abstract class FileBasedLibraryWriter (
    val file: File): KonanLibraryWriter {
}

class KtBcLibraryWriter(file: File, val llvmModule: LLVMModuleRef) 
    : FileBasedLibraryWriter(file) {

    override val mainBitcodeFileName = file.path

    public constructor(path: String, llvmModule: LLVMModuleRef) 
        : this(File(path), llvmModule)

    override fun addKotlinBitcode(llvmModule: LLVMModuleRef) {
        // This is a noop for .kt.bc based libraries,
        // because the bitcode itself is the container.
    }

    override fun addLinkData(linkData: LinkData) {
        MetadataGenerator(llvmModule).addLinkData(linkData)
    }

    override fun addNativeBitcode(library: String) {

        val libraryModule = parseBitcodeFile(library)
        val failed = LLVMLinkModules2(llvmModule, libraryModule)
        if (failed != 0) {
            throw Error("failed to link $library") // TODO: retrieve error message from LLVM.
        }
    }

    override fun commit() {
        LLVMWriteBitcodeToFile(llvmModule, file.path)
    }
}

class SplitLibraryWriter(val libDir: File, target: String, val nopack: Boolean = false): FileBasedLibraryWriter(libDir) {
    public constructor(path: String, target: String, nopack: Boolean): this(File(path), target, nopack)

    val klibFile = File("${libDir.path}.klib")
    val linkdataDir = File(libDir, "linkdata")
    val resourcesDir = File(libDir, "resources")
    val targetDir = File(libDir, target)
    val kotlinDir = File(targetDir, "kotlin")
    val nativeDir = File(targetDir, "native")
    // TODO: Experiment with separate bitcode files.
    // Per package or per class.
    val mainBitcodeFile = File(kotlinDir, "program.kt.bc")
    override val mainBitcodeFileName = mainBitcodeFile.path

    init {
        // TODO: figure out the proper policy here.
        libDir.deleteRecursively()
        klibFile.delete()
        libDir.mkdirs()
        linkdataDir.mkdirs()
        targetDir.mkdirs()
        kotlinDir.mkdirs()
        nativeDir.mkdirs()
        resourcesDir.mkdirs()
    }

    var llvmModule: LLVMModuleRef? = null

    override fun addKotlinBitcode(llvmModule: LLVMModuleRef) {
        this.llvmModule = llvmModule
        LLVMWriteBitcodeToFile(llvmModule, mainBitcodeFileName)
    }

    override fun addLinkData(linkData: LinkData) {
        SplitMetadataGenerator(linkdataDir).addLinkData(linkData)
    }

    override fun addNativeBitcode(library: String) {
        val basename = File(library).getName()
        File(library).copyTo(File(nativeDir, basename)) 
    }

    override fun commit() {
        if (!nopack) {
            // This is no-op for the Split library.
            // Or should we zip the directory?
            libDir.zipDirAs(klibFile)
            libDir.deleteRecursively()
        }
    }
}

