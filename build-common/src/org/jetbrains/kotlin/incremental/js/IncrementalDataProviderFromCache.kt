/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.js

import org.jetbrains.kotlin.incremental.IncrementalJsCache
import org.jetbrains.kotlin.incremental.web.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.web.IrTranslationResultValue
import org.jetbrains.kotlin.incremental.web.TranslationResultValue
import org.jetbrains.kotlin.utils.JsMetadataVersion
import java.io.File

class IncrementalDataProviderFromCache(private val cache: IncrementalJsCache) : IncrementalDataProvider {
    override val headerMetadata: ByteArray
        get() = cache.header

    override val compiledPackageParts: Map<File, TranslationResultValue>
        get() = cache.nonDirtyPackageParts()

    override val metadataVersion: IntArray
        get() = JsMetadataVersion.INSTANCE.toArray() // TODO: store and load correct metadata version

    override val packageMetadata: Map<String, ByteArray>
        get() = cache.packageMetadata()

    override val serializedIrFiles: Map<File, IrTranslationResultValue>
        get() = cache.nonDirtyIrParts()
}
