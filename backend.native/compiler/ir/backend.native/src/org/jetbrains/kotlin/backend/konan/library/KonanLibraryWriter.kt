/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.library

import llvm.LLVMModuleRef
import org.jetbrains.kotlin.konan.library.KonanLibrary

const val KLIB_CURRENT_ABI_VERSION = 1

interface KonanLibraryWriter {
    fun addLinkData(linkData: LinkData)
    fun addNativeBitcode(library: String)
    fun addIncludedBinary(library: String)
    fun addKotlinBitcode(llvmModule: LLVMModuleRef)
    fun addLinkDependencies(libraries: List<KonanLibrary>)
    fun addManifestAddend(path: String)
    fun addDataFlowGraph(dataFlowGraph: ByteArray)
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
