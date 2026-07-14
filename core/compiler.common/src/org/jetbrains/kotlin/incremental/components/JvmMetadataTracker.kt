/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

import java.io.File

interface JvmMetadataTracker {
    fun report(moduleName: String, sourceFile: File, metadata: ByteArray)

    object DoNothing : JvmMetadataTracker {
        override fun report(moduleName: String, sourceFile: File, metadata: ByteArray) {
        }
    }
}
