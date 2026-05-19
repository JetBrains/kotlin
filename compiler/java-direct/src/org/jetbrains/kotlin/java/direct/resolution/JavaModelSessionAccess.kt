/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import java.util.concurrent.ConcurrentHashMap

/**
 * Nullable variant of [org.jetbrains.kotlin.fir.resolve.providers.symbolProvider]: returns `null`
 * when the session has no [FirSymbolProvider] registered. Parsing-level unit fixtures build a
 * bare-bones [FirSession] without one and must short-circuit instead of throwing.
 */
internal val FirSession.nullableSymbolProvider: FirSymbolProvider? by FirSession.nullableSessionComponentAccessor()

/**
 * Per-session set of [ClassId]s currently being resolved by [cycleSafeClassLikeSymbol].
 *
 * Re-entrant probes for an in-flight [ClassId] return `null`/`false` to break the
 * `FirJavaClass.declarations` PUBLICATION-lazy cycle (KT-74097): symbol-provider lookup
 * materialises declarations, which calls back into model resolution, which probes here again.
 *
 * Registered by [registerJavaModelInFlightResolutionsIfAbsent]; sessions without it skip the
 * guard but cannot enter the cycle anyway — [cycleSafeClassLikeSymbol] short-circuits at the
 * missing [FirSymbolProvider] before any recursion.
 */
internal class JavaModelInFlightResolutions : FirSessionComponent {
    val classIds: MutableSet<ClassId> = ConcurrentHashMap.newKeySet()
}

private val FirSession.javaModelInFlightResolutions: JavaModelInFlightResolutions?
        by FirSession.nullableSessionComponentAccessor()

/** Registers a [JavaModelInFlightResolutions] on this session if one is not already present. */
@OptIn(SessionConfiguration::class)
internal fun FirSession.registerJavaModelInFlightResolutionsIfAbsent() {
    if (javaModelInFlightResolutions == null) {
        register(JavaModelInFlightResolutions::class, JavaModelInFlightResolutions())
    }
}

/**
 * Returns the FIR class-like symbol for [classId], or `null` if the symbol provider is missing,
 * does not know [classId], or [classId] is already being resolved on this session (KT-74097
 * cycle break).
 *
 * Resolution-time only; must not be called from cache-population code (`JavaClassCache`,
 * `LeanJavaClassFinder.indexFile`, `JavaSupertypeGraph`-population).
 */
internal fun FirSession.cycleSafeClassLikeSymbol(classId: ClassId): FirClassLikeSymbol<*>? {
    val inFlight = javaModelInFlightResolutions?.classIds
    if (inFlight != null && !inFlight.add(classId)) return null
    return try {
        nullableSymbolProvider?.getClassLikeSymbolByClassId(classId)
    } finally {
        inFlight?.remove(classId)
    }
}

/**
 * `true` if [classId] resolves through this session's symbol provider to a non-builtin class
 * (`origin != BuiltIns`, mirroring PSI behaviour for stdlib classes whose `.class` files exist
 * only when stdlib is on the classpath).
 *
 * Returns `false` under the same conditions as [cycleSafeClassLikeSymbol] returning `null`.
 */
internal fun FirSession.cycleSafeTryResolveClass(classId: ClassId): Boolean {
    val symbol = cycleSafeClassLikeSymbol(classId) ?: return false
    return symbol.origin != FirDeclarationOrigin.BuiltIns
}
