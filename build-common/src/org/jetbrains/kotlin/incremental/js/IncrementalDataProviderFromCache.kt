/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.js

import org.jetbrains.kotlin.incremental.IncrementalJsCache
import java.io.File

class IncrementalDataProviderFromCache(private val cache: IncrementalJsCache) : IncrementalDataProvider {
    override val compiledPackageParts: Map<File, TranslationResultValue>
        get() = cache.nonDirtyPackageParts()

    override val serializedIrFiles: Map<File, IrTranslationResultValue>
        get() = cache.nonDirtyIrParts()

    override val serializedIrInlineFiles: Map<File, IrTranslationResultValue>
        get() = cache.nonDirtyIrInlineParts()
}

class KmpIncrementalDataProviderFromCache(
    private val cache: IncrementalJsCache,
) : KmpIncrementalDataProvider, IncrementalDataProvider by IncrementalDataProviderFromCache(cache) {

    override fun metadata(fragmentName: String): Map<File, ByteArray> {
        return cache.nonDirtyMetadata(fragmentName)
    }
}
