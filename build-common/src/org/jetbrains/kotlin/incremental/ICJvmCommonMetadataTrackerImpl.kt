/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.components.ICJvmCommonMetadataTracker
import java.io.File

class ICJvmCommonMetadataTrackerImpl : ICJvmCommonMetadataTracker {
    val commonMetadataByModule: Map<String, Map<File, ByteArray>>
        field = hashMapOf<String, MutableMap<File, ByteArray>>()

    override fun report(fragmentName: String, sourceFile: File, metadata: ByteArray) {
        commonMetadataByModule.getOrPut(fragmentName) { hashMapOf() }[sourceFile] = metadata
    }
}
