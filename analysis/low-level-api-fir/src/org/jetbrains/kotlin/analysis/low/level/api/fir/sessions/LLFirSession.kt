/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.SoftValueCleaner
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import java.util.concurrent.atomic.AtomicLong

@OptIn(PrivateSessionConstructor::class)
abstract class LLFirSession(
    val ktModule: KtModule,
    override val builtinTypes: BuiltinTypes,
    kind: Kind
) : FirSession(sessionProvider = null, kind) {
    abstract fun getScopeSession(): ScopeSession

    val project: Project
        get() = ktModule.project

    /**
     * Whether the [LLFirSession] is valid. The session should not be used if it is invalid.
     */
    @Volatile
    var isValid: Boolean = true
        private set

    /**
     * Creates a [ModificationTracker] which tracks the validity of this session via [isValid].
     */
    fun createValidityTracker(): ModificationTracker = ValidityModificationTracker()

    private inner class ValidityModificationTracker : ModificationTracker {
        private var count = AtomicLong()

        override fun getModificationCount(): Long {
            if (isValid) return 0

            // When the session is invalid, we cannot simply return a static modification count of 1. For example, consider situations where
            // a cached value was created with an already invalid session (so it remembers the modification count of 1). Then, if we return
            // a static modification count of 1, the modification count never changes and the cached value misses that the session has been
            // invalidated. Hence, `count` is incremented on each modification count access.
            return count.incrementAndGet()
        }
    }

    private val lazyDisposable: Lazy<Disposable> = lazy {
        val disposable = Disposer.newDisposable()

        // `LLFirSessionCache` is used as a disposable parent so that disposal is triggered after the Kotlin plugin is unloaded. We don't
        // register a module as a disposable parent, because (1) IJ `Module`s may persist beyond the plugin's lifetime, (2) not all
        // `KtModule`s have a corresponding `Module`, and (3) sessions are invalidated (and subsequently cleaned up) when their module is
        // removed.
        Disposer.register(LLFirSessionCache.getInstance(project), disposable)

        disposable
    }

    /**
     * Returns an already registered [Disposable] which is alive until the session is invalidated. It can be used as a parent disposable for
     * disposable session components, such as [resolve extensions][org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtension].
     * When the session is invalidated or garbage-collected, all disposable session components will be disposed with this parent disposable.
     *
     * Because not all sessions have disposable components, this disposable is created and registered on-demand with the first call to
     * [requestDisposable]. This avoids polluting [Disposer] with unneeded disposables.
     *
     * The disposable must only be requested during session creation, before the session is added to [LLFirSessionCache].
     */
    fun requestDisposable(): Disposable = lazyDisposable.value

    /**
     * Creates a [SoftValueCleaner] that performs cleanup after the session has been invalidated or reclaimed. This cleanup will mark the
     * session as invalid and dispose its disposable (if requested during session creation).
     */
    internal fun createCleaner(): SoftValueCleaner<LLFirSession> {
        val disposable = if (lazyDisposable.isInitialized()) lazyDisposable.value else null
        return LLFirSessionCleaner(disposable)
    }

    /**
     * [LLFirSessionCleaner] must not keep a strong reference to its associated [LLFirSession], because otherwise the soft reference-based
     * garbage collection of unused sessions will not work.
     *
     * @param disposable The associated [LLFirSession]'s [disposable]. Keeping a separate reference ensures that the disposable can be
     *  disposed even after the session has been reclaimed by the GC.
     */
    private class LLFirSessionCleaner(private val disposable: Disposable?) : SoftValueCleaner<LLFirSession> {
        override fun cleanUp(value: LLFirSession?) {
            // If both the session and the disposable are present, we can check their consistency. Otherwise, this is not possible, because
            // we cannot store the session in the session cleaner (otherwise the session will never be garbage-collected).
            if (value != null && disposable != null) {
                require(value.lazyDisposable.isInitialized()) {
                    "The session to clean up should have an initialized disposable when a disposable is also registered with this" +
                            " cleaner. The session to clean up might not be consistent with the session from which this cleaner was created."
                }

                require(value.lazyDisposable.value == disposable) {
                    "The session to clean up should have a disposable that is equal to the disposable registered with this cleaner. The" +
                            " session to clean up might not be consistent with the session from which this cleaner was created."
                }
            }

            value?.isValid = false
            disposable?.let { Disposer.dispose(it) }
        }
    }

    override fun toString(): String {
        return "${this::class.simpleName} for ${ktModule.moduleDescription}"
    }
}

abstract class LLFirModuleSession(
    ktModule: KtModule,
    builtinTypes: BuiltinTypes,
    kind: Kind
) : LLFirSession(ktModule, builtinTypes, kind)

val FirElementWithResolveState.llFirSession: LLFirSession
    get() = moduleData.session as LLFirSession

val FirBasedSymbol<*>.llFirSession: LLFirSession
    get() = moduleData.session as LLFirSession