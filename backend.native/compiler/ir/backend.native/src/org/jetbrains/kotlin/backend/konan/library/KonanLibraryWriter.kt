/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.library

import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.konan.serialization.UniqId
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.KonanLibraryVersioning
import org.jetbrains.kotlin.konan.properties.Properties

interface KonanLibraryWriter {
    val versions: KonanLibraryVersioning
    fun addLinkData(linkData: LinkData)
    fun addNativeBitcode(library: String)
    fun addIncludedBinary(library: String)
    fun addKotlinBitcode(llvmModule: LLVMModuleRef)
    fun addLinkDependencies(libraries: List<KonanLibrary>)
    fun addManifestAddend(properties: Properties)
    fun addDataFlowGraph(dataFlowGraph: ByteArray)
    val mainBitcodeFileName: String
    fun commit()
}

class LinkData(
    val module: ByteArray,
    val fragments: List<List<ByteArray>>,
    val fragmentNames: List<String>,
    val ir: SerializedIr? = null
)

class SerializedIr (
    val module: ByteArray,
    val declarations: Map<UniqId, ByteArray>,
    val debugIndex: Map<UniqId, String>
)

interface MetadataWriter {
    fun addLinkData(linkData: LinkData)
}
