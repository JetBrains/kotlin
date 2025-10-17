/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.native

import org.jetbrains.kotlin.backend.konan.serialization.SerializerOutput

data class KlibWriterInput(
    val serializerOutput: SerializerOutput,
    val customOutputPath: String?,
    val produceHeaderKlib: Boolean,
)