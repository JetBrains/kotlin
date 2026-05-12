/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import java.util.concurrent.ConcurrentHashMap

/**
 * Nullable variant of [org.jetbrains.kotlin.fir.resolve.providers.symbolProvider]: returns `null`
 * when the session has no [FirSymbolProvider] component registered (parsing-level test fixtures
 * that use a bare-bones [FirSession]) instead of throwing. Used only by [LazySessionAccess].
 */
private val FirSession.nullableSymbolProvider: FirSymbolProvider? by FirSession.nullableSessionComponentAccessor()

/**
 * Re-entrance guard for [LazySessionAccess.tryResolve] / [LazySessionAccess.classLikeSymbol].
 *
 * Set of `(FirSession, ClassId)` pairs currently being resolved. Re-entrant probes for the
 * same pair return the caller's fallback to break the
 * `FirJavaClass.declarations` PUBLICATION-lazy cycle (KT-74097): symbol-provider lookup
 * forces FIR's nested-class scope, which materialises declarations, which calls back into
 * model annotation/classifier resolution, which probes through here again.
 *
 * Process-global on purpose — top-level rather than a [LazySessionAccess] field — so the
 * guard is shared across every [LazySessionAccess] instance that wraps the same session
 * (independent per-file `CompilationUnitContext`s allocate fresh wrappers on a shared
 * session, and the cycle exists on the session, not the wrapper). Keying by both
 * [FirSession] and [ClassId] keeps the semantics precise: only re-entrant requests for the
 * **same** pair short-circuit; unrelated nested probes proceed normally. Session-scoped
 * (not thread-local) so cooperative scheduling (coroutines crossing threads) cannot lose
 * the in-flight bit.
 *
 * Pairs are removed in `finally`; the backing [ConcurrentHashMap.newKeySet] keeps lookups
 * thread-safe.
 */
private val inFlightResolutions: MutableSet<Pair<FirSession, ClassId>> = ConcurrentHashMap.newKeySet()

/**
 * Runs [block] under the [inFlightResolutions] re-entrance guard, keyed by the
 * `(session, classId)` pair. Returns [reentrantDefault] without invoking [block] if the
 * pair is already in flight; otherwise marks the pair, invokes [block], and clears the
 * mark in `finally`. Top-level so the inline call site is not a value-class member.
 */
private inline fun <R> guardedResolution(
    session: FirSession,
    classId: ClassId,
    reentrantDefault: R,
    block: () -> R,
): R {
    val key = session to classId
    if (!inFlightResolutions.add(key)) return reentrantDefault
    return try {
        block()
    } finally {
        inFlightResolutions.remove(key)
    }
}

/**
 * Capability token for resolution-time access to [FirSession] from inside the Java Model.
 *
 * Held by [JavaResolutionContext] and threaded only along resolution-time code paths;
 * cache-population code (anything reachable from `JavaClassCache`,
 * `LeanJavaClassFinder.indexFile`, or `JavaSupertypeGraph`-population) does NOT see this
 * type and therefore cannot reach `firSession.symbolProvider`.
 *
 * Step 4.5a deliverable per
 * [implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md] §7 mode 1 / §12 Q2 — the typed
 * wrapper makes failure-mode 1 (eager batched symbol lookups inside cache population)
 * **typeable**: a reviewer-and-CI-checkable invariant rather than a code-review one.
 *
 * The wrapper exposes only the surface the resolver needs:
 *  - [classLikeSymbol]: a single [ClassId] → [FirClassLikeSymbol] probe (the model's only
 *    way of asking the FIR symbol provider whether a class exists).
 *  - [tryResolve]: the same probe with the PSI-parity builtins filter applied (rejecting
 *    `FirDeclarationOrigin.BuiltIns` for stdlib classes when stdlib is on the classpath).
 *
 * Both methods are no-ops when the session is missing (parsing-level unit tests that build
 * the model in isolation; see `compiler/java-direct/testFixtures/.../components.kt`); in
 * that mode resolution falls through to AST-only paths.
 */
@JvmInline
internal value class LazySessionAccess(private val session: FirSession) {
    /**
     * `null` when the session has no [FirSymbolProvider] component registered (parsing-level
     * unit-test fixtures that build a bare-bones [FirSession] without configuring it for
     * resolution); otherwise the registered symbol provider.
     *
     * The component is fetched via the **nullable** session-component path so that absent
     * components return `null` rather than throwing — consumers of [LazySessionAccess] in
     * test fixtures keep their AST-only fallback behaviour.
     */
    private val symbolProviderOrNull: FirSymbolProvider?
        get() = session.nullableSymbolProvider

    /**
     * Returns the FIR class-like symbol for [classId], or `null` if the session's symbol
     * provider does not know it (or no symbol provider is registered on the session at all).
     *
     * Returns `null` when the same `(session, classId)` pair is already in flight on this
     * session — the semantical re-entrance guard described on [inFlightResolutions] —
     * to break the declaration-materialisation cycle (KT-74097).
     *
     * **Laziness contract** (per [implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md] §7
     * mode 1 / §8 mode 1): must NOT be called from cache-population code. The typed
     * wrapper structurally enforces this by being unreachable from cache-population
     * components, which take only AST-shaped inputs.
     */
    fun classLikeSymbol(classId: ClassId): FirClassLikeSymbol<*>? =
        guardedResolution(session, classId, reentrantDefault = null) {
            symbolProviderOrNull?.getClassLikeSymbolByClassId(classId)
        }

    /**
     * `true` if the symbol provider knows [classId] and the result is not a Kotlin builtin.
     *
     * The builtins filter mirrors PSI behaviour for stdlib classes: PSI resolves through
     * compiled `.class` files and light classes, while Kotlin builtins (origin=BuiltIns)
     * exist only in FIR's symbol provider with no `.class` backing. When stdlib is on the
     * classpath these classes have origin=Library instead, so the filter is a no-op there.
     *
     * Returns `false` when no symbol provider is registered on the session, or when the
     * same `(session, classId)` pair is already in flight on this session — the
     * semantical re-entrance guard described on [inFlightResolutions], which breaks the
     * declaration-materialisation cycle (KT-74097).
     *
     * Mirrors the body of the deleted `tryResolve` lambda that
     * `JavaTypeConversion.resolveSymbolBasedClassId` used pre-Step-4.5a.
     */
    fun tryResolve(classId: ClassId): Boolean =
        guardedResolution(session, classId, reentrantDefault = false) {
            val symbol = symbolProviderOrNull?.getClassLikeSymbolByClassId(classId)
            symbol != null && symbol.origin != FirDeclarationOrigin.BuiltIns
        }
}
