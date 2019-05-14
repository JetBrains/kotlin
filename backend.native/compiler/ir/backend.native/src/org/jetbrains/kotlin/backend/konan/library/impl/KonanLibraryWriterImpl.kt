/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.library.impl

import llvm.LLVMModuleRef
import org.jetbrains.kotlin.library.SerializedIr
import org.jetbrains.kotlin.backend.konan.library.BitcodeWriter
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryWriter
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BaseWriterImpl
import org.jetbrains.kotlin.library.impl.IrWriterImpl
import org.jetbrains.kotlin.library.impl.KotlinLibraryLayoutForWriter
import org.jetbrains.kotlin.library.impl.MetadataWriterImpl

class KonanLibraryLayoutForWriter(
    override val libDir: File,
    override val target: KonanTarget
) : KonanLibraryLayout, KotlinLibraryLayoutForWriter(libDir)


/**
 * Requires non-null [target].
 */
class KonanLibraryWriterImpl(
    libDir: File,
    moduleName: String,
    versions: KonanLibraryVersioning,
    target: KonanTarget,
    nopack: Boolean = false,

    val layout: KonanLibraryLayoutForWriter = KonanLibraryLayoutForWriter(libDir, target),

    base: BaseWriter = BaseWriterImpl(layout, moduleName, versions, nopack),
    bitcode: BitcodeWriter = BitcodeWriterImpl(layout),
    metadata: MetadataWriter = MetadataWriterImpl(layout),
    ir: IrWriter = IrWriterImpl(layout)

) : BaseWriter by base, BitcodeWriter by bitcode, MetadataWriter by metadata, IrWriter by ir, KonanLibraryWriter

internal fun buildLibrary(
    natives: List<String>,
    included: List<String>,
    linkDependencies: List<KonanLibrary>,
    metadata: SerializedMetadata,
    ir: SerializedIr,
    versions: KonanLibraryVersioning,
    target: KonanTarget,
    output: String,
    moduleName: String,
    llvmModule: LLVMModuleRef?,
    nopack: Boolean,
    manifestProperties: Properties?,
    dataFlowGraph: ByteArray?
): KonanLibraryLayout {

    val library = KonanLibraryWriterImpl(File(output), moduleName, versions, target, nopack)

    llvmModule?.let { library.addKotlinBitcode(it) }
    library.addMetadata(metadata)
    library.addIr(ir)

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
    return library.layout
}