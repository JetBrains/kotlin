/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.writer

import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.KlibIrComponentWriterImpl
import org.jetbrains.kotlin.library.impl.KlibMetadataComponentWriterImpl
import java.nio.file.Path

/**
 * A [KlibWriter] DSL extension to include [SerializedMetadata] to the created library.
 */
fun KlibWriterSpec.includeMetadata(metadata: SerializedMetadata, fragmentTracker: KlibWrittenMetadataPackageFragmentTracker? = null) {
    include(KlibMetadataComponentWriterImpl(metadata, fragmentTracker))
}

/**
 * Receives the mapping between a serialized metadata fragment written to a klib and the source file it originates from.
 */
fun interface KlibWrittenMetadataPackageFragmentTracker {
    /**
     * Reports that the serialized metadata fragment [outputFile] was produced from [sourceFile].
     * [sourceFile] is `null` when the fragment has no originating source file.
     */
    fun recordSourceFile(sourceFile: Path?, outputFile: Path)
}

/**
 * A [KlibWriter] DSL extension to include [SerializedIrModule] to the created library.
 */
fun KlibWriterSpec.includeIr(irModule: SerializedIrModule?) {
    irModule?.files?.let { include(KlibIrComponentWriterImpl.ForMainIr(it)) }
    irModule?.fileWithPreparedInlinableFunctions?.let { include(KlibIrComponentWriterImpl.ForInlinableFunctionsIr(it)) }
}
