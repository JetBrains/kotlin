/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

import java.io.File

interface ICJvmMetadataTracker {
    fun report(fragmentName: String, sourceFile: File, metadata: ByteArray)

    object DoNothing : ICJvmMetadataTracker {
        override fun report(fragmentName: String, sourceFile: File, metadata: ByteArray) {
        }
    }
}
