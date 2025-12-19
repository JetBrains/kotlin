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
 * An adapter to convert [SerializedMetadata] to [KlibComponentWriter].
 */
fun SerializedMetadata.asComponentWriter(): KlibComponentWriter = KlibMetadataComponentWriterImpl(this)

/**
 * An adapter to convert [SerializedIrModule] to [KlibComponentWriter]s.
 */
fun SerializedIrModule.asComponentWriters(): Collection<KlibComponentWriter> = listOfNotNull(
    KlibIrComponentWriterImpl.ForMainIr(files),
    fileWithPreparedInlinableFunctions?.let(KlibIrComponentWriterImpl::ForInlinableFunctionsIr),
)
