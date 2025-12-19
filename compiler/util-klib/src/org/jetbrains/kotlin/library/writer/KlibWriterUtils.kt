/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.writer

import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.KlibIrWritableComponentImpl
import org.jetbrains.kotlin.library.impl.KlibMetadataWritableComponentImpl

/**
 * An adapter to convert [SerializedMetadata] to [KlibWritableComponent].
 */
fun SerializedMetadata.asWritableComponent(): KlibWritableComponent = KlibMetadataWritableComponentImpl(this)

/**
 * Adapters to convert [SerializedIrModule] to [KlibWritableComponent]s.
 */
fun Collection<SerializedIrFile>.asMainIrWritableComponent(): KlibWritableComponent = KlibIrWritableComponentImpl.ForMainIr(this)
fun SerializedIrFile.asInlinableFunctionsIrWritableComponent(): KlibWritableComponent = KlibIrWritableComponentImpl.ForInlinableFunctionsIr(this)
