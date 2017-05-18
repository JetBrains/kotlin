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

package org.jetbrains.kotlin.backend.konan.library

import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.TargetManager
import org.jetbrains.kotlin.backend.konan.serialization.Base64
import org.jetbrains.kotlin.backend.konan.serialization.deserializeModule
import org.jetbrains.kotlin.backend.konan.util.File
import org.jetbrains.kotlin.backend.konan.util.copyTo
import org.jetbrains.kotlin.backend.konan.util.unzipAs
import org.jetbrains.kotlin.backend.konan.util.zipDirAs
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl

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

// This scheme describes the Konan Library (klib) layout.
interface SplitScheme {
    val libDir: File
    val target: String?
        // This is a default implementation. Can't make it an assignment.
        get() = null 
    val klibFile 
        get() = File("${libDir.path}.klib")
    val moduleFile 
        get() = File(libDir, "module")
    val linkdataDir 
        get() = File(libDir, "linkdata")
    val resourcesDir 
        get() = File(libDir, "resources")
    val targetDir 
        get() = File(libDir, target!!)
    val kotlinDir 
        get() = File(targetDir, "kotlin")
    val nativeDir 
        get() = File(targetDir, "native")
}

// TODO: Get rid of the configuration here.
class SplitLibraryReader(override val libDir: File, configuration: CompilerConfiguration) 
    : FileBasedLibraryReader(libDir, configuration, SplitMetadataReader(libDir)), 
      SplitScheme  {

    public constructor(path: String, configuration: CompilerConfiguration) : this(File(path), configuration) 

    override val target: String? = TargetManager(configuration).currentName

    init {
        unpackIfNeeded()
    }

    // TODO: Search path processing is also goes somewhere around here.
    fun unpackIfNeeded() {
        // TODO: Clarify the policy here.
        if (libDir.exists) {
            if (libDir.isDirectory) return
        }
        if (!klibFile.exists) {
            error("Could not find neither $libDir nor $klibFile.")
        }
        if (klibFile.isFile) {
            klibFile.unzipAs(libDir)

            if (!libDir.exists) error("Could not unpack $klibFile as $libDir.")
        } else {
            error("Expected $klibFile to be a regular libDir.")
        }
    }

    private val File.dirAbsolutePaths: List<String>
        get() = this.listFiles!!.toList()!!.map{it->it.absolutePath}

    override val bitcodePaths: List<String>
        get() = kotlinDir.dirAbsolutePaths + 
                nativeDir.dirAbsolutePaths

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

class SplitLibraryWriter(override val libDir: File, override val target: String?, val nopack: Boolean = false): 
    FileBasedLibraryWriter(libDir), SplitScheme {

    public constructor(path: String, target: String, nopack: Boolean): this(File(path), target, nopack)

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
        SplitMetadataGenerator(libDir).addLinkData(linkData)
    }

    override fun addNativeBitcode(library: String) {
        val basename = File(library).name
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

