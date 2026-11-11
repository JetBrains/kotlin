/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.js

import org.jetbrains.kotlin.incremental.IncrementalKlibCache
import java.io.File

class IncrementalDataProviderFromCache(private val cache: IncrementalKlibCache) : IncrementalDataProvider {
    override val compiledPackageParts: Map<File, TranslationResultValue>
        get() = cache.nonDirtyPackageParts()

    override val serializedIrFiles: Map<File, IrTranslationResultValue>
        get() = cache.nonDirtyIrParts()

    override val serializedIrInlineFiles: Map<File, IrTranslationResultValue>
        get() = cache.nonDirtyIrInlineParts()
}

class KmpIncrementalDataProviderFromCache(
    private val cache: IncrementalKlibCache,
) : KmpIncrementalDataProvider, IncrementalDataProvider by IncrementalDataProviderFromCache(cache) {

    override fun commonMetadata(fragmentName: String): Map<File, ByteArray> {
        return cache.nonDirtyCommonMetadata(fragmentName)
    }
}
