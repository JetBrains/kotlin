/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicLongFieldUpdater

/**
 * An [LLFirSession] stores all symbols, components, and configuration needed for the resolution of Kotlin code/binaries from a [KtModule].
 *
 * ### Invalidation
 *
 * [LLFirSession] will be invalidated by [LLFirSessionInvalidationService] when its [KtModule] or one of the module's dependencies is
 * modified, or when a global modification event occurs. Sessions are managed by [LLFirSessionCache], which holds a soft reference to its
 * [LLFirSession]s. This allows a session to be garbage collected when it is softly reachable. The session's [LLFirSessionCleaner] ensures
 * that its associated [Disposable] is properly disposed even after garbage collection.
 *
 * When a session is invalidated after a modification event, the [LLFirSessionInvalidationEventPublisher] will publish a
 * [session invalidation event][LLFirSessionInvalidationTopics]. This allows entities whose lifetime depends on the session's lifetime to be
 * invalidated with the session. Such an event is not published when the session is garbage collected due to being softly reachable, because
 * the [LLFirSessionCleaner] is not guaranteed to be executed in a write action. If we try to publish a session invalidation event outside
 * a write action, another thread might already have built another [LLFirSession] for the same [KtModule], causing a race between the new
 * session and the session invalidation event (which can only refer to the [KtModule] because the session has already been garbage
 * collected).
 *
 * Because of this, it's important that cached entities which depend on a session's lifetime (and therefore its session invalidation events)
 * are *exactly as softly reachable* as the [LLFirSession]. This means that the cached entity should keep a strong reference to the session,
 * but the entity itself should be softly reachable if not currently in use. For example, `KaFirSession`s are softly reachable via
 * `KaFirSessionProvider`, but keep a strong reference to the [LLFirSession].
 */
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
        internal set

    /**
     * Creates a [ModificationTracker] which tracks the validity of this session via [isValid].
     */
    fun createValidityTracker(): ModificationTracker = LLFirSessionValidityModificationTracker(WeakReference(this))

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
     * disposable session components, such as [resolve extensions][org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtension].
     * When the session is invalidated or garbage-collected, all disposable session components will be disposed with this parent disposable.
     *
     * Because not all sessions have disposable components, this disposable is created and registered on-demand with the first call to
     * [requestDisposable]. This avoids polluting [Disposer] with unneeded disposables.
     *
     * The disposable must only be requested during session creation, before the session is added to [LLFirSessionCache].
     */
    fun requestDisposable(): Disposable = lazyDisposable.value

    /**
     * A [Disposable] that has already been requested with [requestDisposable], or `null` otherwise.
     */
    internal val requestedDisposableOrNull: Disposable?
        get() = if (lazyDisposable.isInitialized()) lazyDisposable.value else null

    override fun toString(): String {
        return "${this::class.simpleName} for ${ktModule.moduleDescription}"
    }
}

/**
 * The validity tracker must not strongly reference the session to avoid leaking it, as the validity tracker may survive it.
 *
 * Similarly, by convention, the [LLFirSession] doesn't keep a strong reference to the validity tracker, to avoid overly complicated
 * reference cycles (from the developer's perspective).
 */
private class LLFirSessionValidityModificationTracker(private val sessionRef: WeakReference<LLFirSession>) : ModificationTracker {
    @Suppress("Unused")
    @Volatile
    private var count = 0L

    override fun getModificationCount(): Long {
        if (sessionRef.get()?.isValid == true) return 0

        // When the session is invalid, we cannot simply return a static modification count of 1. For example, consider situations where
        // a cached value was created with an already invalid session (so it remembers the modification count of 1). Then, if we return
        // a static modification count of 1, the modification count never changes and the cached value misses that the session has been
        // invalidated. Hence, `count` is incremented on each modification count access.
        return COUNT_UPDATER.incrementAndGet(this)
    }

    companion object {
        private val COUNT_UPDATER = AtomicLongFieldUpdater.newUpdater(LLFirSessionValidityModificationTracker::class.java, "count")
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
