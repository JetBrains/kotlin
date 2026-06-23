/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.writer

import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.KlibIrComponentWriterImpl
import org.jetbrains.kotlin.library.impl.KlibMetadataComponentWriterImpl

/**
 * A [KlibWriter] DSL extension to include [SerializedMetadata] to the created library.
 */
fun KlibWriterSpec.includeMetadata(metadata: SerializedMetadata) {
    include(KlibMetadataComponentWriterImpl(metadata))
}

/**
 * A [KlibWriter] DSL extension to include [SerializedIrModule] to the created library.
 */
fun KlibWriterSpec.includeIr(irModule: SerializedIrModule?) {
    irModule?.files?.let { include(KlibIrComponentWriterImpl.ForMainIr(it)) }
    irModule?.fileWithPreparedInlinableFunctions?.let { include(KlibIrComponentWriterImpl.ForInlinableFunctionsIr(it)) }
}
