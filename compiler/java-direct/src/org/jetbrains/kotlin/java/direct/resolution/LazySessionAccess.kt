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

/**
 * Nullable variant of [org.jetbrains.kotlin.fir.resolve.providers.symbolProvider]: returns `null`
 * when the session has no [FirSymbolProvider] component registered (parsing-level test fixtures
 * that use a bare-bones [FirSession]) instead of throwing. Used only by [LazySessionAccess].
 */
private val FirSession.nullableSymbolProvider: FirSymbolProvider? by FirSession.nullableSessionComponentAccessor()

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
     * **Laziness contract** (per [implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md] §7
     * mode 1 / §8 mode 1): must NOT be called from cache-population code. The typed
     * wrapper structurally enforces this by being unreachable from cache-population
     * components, which take only AST-shaped inputs.
     */
    fun classLikeSymbol(classId: ClassId): FirClassLikeSymbol<*>? =
        symbolProviderOrNull?.getClassLikeSymbolByClassId(classId)

    /**
     * `true` if the symbol provider knows [classId] and the result is not a Kotlin builtin.
     *
     * The builtins filter mirrors PSI behaviour for stdlib classes: PSI resolves through
     * compiled `.class` files and light classes, while Kotlin builtins (origin=BuiltIns)
     * exist only in FIR's symbol provider with no `.class` backing. When stdlib is on the
     * classpath these classes have origin=Library instead, so the filter is a no-op there.
     *
     * Returns `false` when no symbol provider is registered on the session.
     *
     * Mirrors the body of the deleted `tryResolve` lambda that
     * `JavaTypeConversion.resolveSymbolBasedClassId` used pre-Step-4.5a.
     */
    fun tryResolve(classId: ClassId): Boolean {
        val symbol = symbolProviderOrNull?.getClassLikeSymbolByClassId(classId) ?: return false
        return symbol.origin != FirDeclarationOrigin.BuiltIns
    }
}
