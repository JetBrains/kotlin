/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.FirCacheWithInvalidation
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.logErrorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import java.util.concurrent.ConcurrentHashMap

internal class FirThreadSafeCache<K : Any, V, CONTEXT>(
    private val map: ConcurrentHashMap<K, Any> = ConcurrentHashMap<K, Any>(),
    private val createValue: (K, CONTEXT) -> V,
) : FirCache<K, V, CONTEXT>(), FirCacheWithInvalidation<K, V, CONTEXT> {
    override fun getValue(key: K, context: CONTEXT): V = map.getOrPutWithNullableValue(key) {
        createValue(it, context)
    }

    override fun getValueIfComputed(key: K): V? = map[key]?.nullValueToNull()

    override fun fixInconsistentValue(
        key: K,
        context: CONTEXT & Any,
        inconsistencyMessage: String,
        mapping: (oldValue: V, newValue: V & Any) -> V & Any,
    ): V & Any {
        val newValue = createValue(key, context)
        checkWithAttachment(
            newValue != null,
            message = { "A value for requested key & context must not be null due to the contract" },
        ) {
            buildAttachments(key, context, newValue)
        }

        LOG.logErrorWithAttachment(inconsistencyMessage) {
            buildAttachments(key, context, newValue)
        }

        val result = map.merge(key, newValue) { old, _ ->
            mapping(old.nullValueToNull(), newValue)
        }

        @Suppress("UNCHECKED_CAST")
        return result as (V & Any)
    }

    private fun ExceptionAttachmentBuilder.buildAttachments(key: K, context: CONTEXT, value: V) {
        withEntry("key", key.toString())

        if (context is PsiElement) {
            withPsiEntry("context", context)
        } else {
            withEntry("context", context.toString())
        }

        val unwrapped = (value as? Collection<*>)?.singleOrNull() ?: value
        when (unwrapped) {
            is FirElement -> withFirEntry("value", unwrapped)
            is FirBasedSymbol<*> -> withFirSymbolEntry("value", unwrapped)
            else -> withEntry("value", unwrapped.toString())
        }
    }
}

private val LOG = Logger.getInstance(FirThreadSafeCache::class.java)