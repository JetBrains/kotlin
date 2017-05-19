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

import llvm.LLVMLinkModules2
import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.serialization.Base64
import org.jetbrains.kotlin.backend.konan.serialization.deserializeModule
import org.jetbrains.kotlin.backend.konan.util.File
import org.jetbrains.kotlin.backend.konan.util.copyTo
import org.jetbrains.kotlin.backend.konan.util.unzipAs
import org.jetbrains.kotlin.backend.konan.util.zipDirAs
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl

class KtBcLibraryReader(file: File, currentAbiVersion: Int) 
    : FileBasedLibraryReader(file, currentAbiVersion, KtBcMetadataReader(file)) {

    public constructor(path: String, currentAbiVersion: Int) : this(File(path), currentAbiVersion) 

    override val bitcodePaths: List<String>
        get() = listOf(libraryName)

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
        KtBcMetadataGenerator(llvmModule).addLinkData(linkData)
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


