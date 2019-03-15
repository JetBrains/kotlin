/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata


class JsKlibMetadataFileRegistry {
    private val fileIdsImpl = mutableMapOf<JsKlibFileMetadata, Int>()

    fun lookup(file: JsKlibFileMetadata) = fileIdsImpl.getOrPut(file) { fileIdsImpl.size }

    val fileIds: Map<JsKlibFileMetadata, Int>
        get() = fileIdsImpl
}
