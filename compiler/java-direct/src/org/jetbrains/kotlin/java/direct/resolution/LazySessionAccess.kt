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
 * Set of `(FirSession, ClassId)` pairs whose model-side resolution is currently in flight.
 *
 * The set is the **semantical** re-entrance guard used by [LazySessionAccess.tryResolve] /
 * [LazySessionAccess.classLikeSymbol]: each pair represents a concrete in-flight resolution
 * (a specific [ClassId] currently being resolved on a specific [FirSession]). Re-entrant
 * requests for the same pair return the fallback to break the cycle described below;
 * unrelated pairs (different [FirSession] or different [ClassId]) never interfere.
 *
 * **Why a re-entrance guard is needed.** [LazySessionAccess.tryResolve] /
 * [LazySessionAccess.classLikeSymbol] delegate to
 * [FirSymbolProvider.getClassLikeSymbolByClassId]. The FIR composite chain includes
 * `FirExtensionDeclarationsSymbolProvider`, whose nested-class branch (see
 * `generateClassLikeDeclaration`) computes a `FirNestedClassifierScopeImpl` over the outer
 * class. Building that scope's `classIndex` forces `FirJavaClass.declarations`, which is a
 * `LazyThreadSafetyMode.PUBLICATION` lazy and therefore lets same-thread re-entrance
 * recurse silently (KT-74097). Materialising the declarations runs `convertJavaFieldToFir`
 * → `setAnnotationsFromJava` → `JavaAnnotation.classId`, which calls back into the model's
 * resolver, which probes via [LazySessionAccess.tryResolve] again. PUBLICATION re-entry
 * **restarts** the materialisation block from the beginning, so the second iteration
 * reaches the same field/annotation, mounts the same probe, and the inner request short-
 * circuits on this set. Without the guard the chain spirals into a `StackOverflowError` —
 * see `IntelliJFullPipelineTestsGenerated.testIntellij_vcs_git`.
 *
 * **Why session-scoped (not thread-scoped).** A thread-local flag silently desynchronises
 * under any cooperative-scheduling model — for example a coroutine resuming on a different
 * thread mid-resolution would lose the in-flight bit and recurse anew. Keying by
 * [FirSession] ties the guard to the resolution scope, which is invariant under thread
 * switches: the cycle exists because of FIR-side `FirJavaClass.declarations` lazies on the
 * session, so the in-flight set must be shared across all [LazySessionAccess] instances
 * that wrap the same session — including the inner re-entrant call dispatched from a
 * different per-file `CompilationUnitContext`, which owns a fresh [LazySessionAccess]
 * value but the same underlying session.
 *
 * **Why per-[ClassId] (not boolean).** Tracking individual [ClassId]s — rather than a
 * single coarse "is anything in flight on this session" bit — keeps the semantics precise:
 * only re-entrant requests for the *same* [ClassId] on the *same* session are short-
 * circuited; unrelated probes that happen to nest inside each other proceed normally.
 * This matches the actual cycle pattern: PUBLICATION re-entry restarts the materialisation
 * compute block, so the second iteration processes the same field/annotation pair, hits
 * the same probe, and finds the [ClassId] already in flight. Concurrent and unrelated
 * resolutions on the same session are not blocked by each other.
 *
 * **Effect of the fallback.** A re-entrant [LazySessionAccess.tryResolve] returns
 * `false`; a re-entrant [LazySessionAccess.classLikeSymbol] returns `null`. The model-side
 * caller's existing fallback paths take over: [JavaAnnotationOverAst.computeClassId]
 * falls back to `ClassId.topLevel(FqName(reference))`, matching pre-Step-4.5a behaviour
 * and the parsing-level test-fixture path; cross-file type classifier resolution falls
 * back to `null` classifier (the binary-classpath / `findClassIdByFqNameString` path on
 * the FIR side then takes over). These fallbacks may be less precise than a full
 * FIR-backed resolution, but they preserve compilation progress. The guard fires only
 * on the inner re-entrant call; the outer call (which holds the entry) finishes its
 * FIR-backed lookup with full precision.
 *
 * **Memory.** Pairs are removed on `finally` after every guarded call, so the set never
 * holds entries longer than the resolution itself. A backing
 * [ConcurrentHashMap.newKeySet] keeps lookups thread-safe without external locking.
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
     * **Laziness contract**: must NOT be called from cache-population code. The typed
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
