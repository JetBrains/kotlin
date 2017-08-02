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

interface KonanLibraryWriter {
    fun addLinkData(linkData: LinkData)
    fun addNativeBitcode(library: String)
    fun addKotlinBitcode(llvmModule: LLVMModuleRef)
    fun addManifestAddend(path: String)
    fun addEscapeAnalysis(escapeAnalysis: ByteArray)
    val mainBitcodeFileName: String
    fun commit()
}

class LinkData(
    val module: ByteArray,
    val fragments: List<ByteArray>,
    val fragmentNames: List<String> 
)

interface MetadataWriter {
    fun addLinkData(linkData: LinkData)
}
