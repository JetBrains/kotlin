/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.SoftValueCleaner

/**
 * [LLFirSessionCleaner] is a [SoftValueCleaner] which handles cleanup of the session after it has been explicitly invalidated or garbage
 * collected.
 *
 * It must not keep a strong reference to its associated [LLFirSession], because otherwise the soft reference-based garbage collection of
 * unused sessions will not work.
 *
 * @param disposable The associated [LLFirSession]'s [disposable]. Keeping a separate reference ensures that the disposable can be disposed
 *  even after the session has been reclaimed by the GC.
 */
internal class LLFirSessionCleaner(private val disposable: Disposable?) : SoftValueCleaner<LLFirSession> {
    override fun cleanUp(value: LLFirSession?) {
        // If both the session and the disposable are present, we can check their consistency. Otherwise, this is not possible, because
        // we cannot store the session in the session cleaner (otherwise the session will never be garbage-collected).
        if (value != null && disposable != null) {
            val sessionDisposable = value.requestedDisposableOrNull

            require(sessionDisposable != null) {
                "The session to clean up should have a registered disposable when a disposable is also registered with this" +
                        " cleaner. The session to clean up might not be consistent with the session from which this cleaner was created."
            }

            require(sessionDisposable == disposable) {
                "The session to clean up should have a disposable that is equal to the disposable registered with this cleaner. The" +
                        " session to clean up might not be consistent with the session from which this cleaner was created."
            }
        }

        value?.isValid = false
        disposable?.let { Disposer.dispose(it) }

        if (value != null) {
            // We only publish session invalidation events for sessions which haven't been garbage collected yet.
            LLFirSessionInvalidationEventPublisher.getInstance(value.project).collectSession(value)
        }
    }
}
