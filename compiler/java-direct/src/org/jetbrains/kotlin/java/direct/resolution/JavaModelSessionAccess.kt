/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds
import java.util.concurrent.ConcurrentHashMap

/**
 * Nullable variant of [org.jetbrains.kotlin.fir.resolve.providers.symbolProvider].
 * Needed for parsing-level unit tests that use barely initialized sessions.
 */
internal val FirSession.nullableSymbolProvider: FirSymbolProvider? by FirSession.nullableSessionComponentAccessor()

/**
 * Per-session set of [ClassId]s currently being resolved by [cycleSafeClassLikeSymbol].
 * Re-entrant probes for an in-flight [ClassId] return `null`/`false` to break the
 * `FirJavaClass.declarations` PUBLICATION-lazy cycle (KT-74097): symbol-provider lookup
 * materialises declarations, which calls back into model resolution, which probes here again.
 * See [JavaCycleBreakerTest].
 *
 * Tied to session (a re-entrant probe can arrive through a different per-file context wrapping
 * the same session) and concurrent. Sessions without it skip the guard but cannot enter the
 * cycle anyway — [cycleSafeClassLikeSymbol] short-circuits at the missing [FirSymbolProvider].
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
 * Per-session set of [ClassId]s whose Java supertype graph is currently being walked by
 * [cycleGuardedSupertypeWalk]. Reentry returns the caller-supplied default without recursing,
 * bounding direct (`A extends A`) and indirect (`A -> B -> A`) Java inheritance cycles — only
 * possible in malformed source, but recursing on them would end in a `StackOverflowError`.
 * See [JavaCycleBreakerTest].
 *
 * Tied to session and concurrent, for the same reasons as [JavaModelInFlightResolutions].
 * Sessions without it run the walk unguarded.
 */
internal class JavaModelSupertypeWalkGuard : FirSessionComponent {
    val classIds: MutableSet<ClassId> = ConcurrentHashMap.newKeySet()
}

private val FirSession.javaModelSupertypeWalkGuard: JavaModelSupertypeWalkGuard?
        by FirSession.nullableSessionComponentAccessor()

@OptIn(SessionConfiguration::class)
internal fun FirSession.registerJavaModelSupertypeWalkGuardIfAbsent() {
    if (javaModelSupertypeWalkGuard == null) {
        register(JavaModelSupertypeWalkGuard::class, JavaModelSupertypeWalkGuard())
    }
}

/**
 * Runs [block] guarded against re-entry on [classId]'s supertype walk.
 *
 * Mirrors [cycleSafeClassLikeSymbol], but bounds the Java inheritance-graph cycle rather than the
 * KT-74097 PUBLICATION-lazy cycle. Sessions without [JavaModelSupertypeWalkGuard] run [block]
 * unguarded.
 */
internal fun <R> FirSession.cycleGuardedSupertypeWalk(classId: ClassId, default: R, block: () -> R): R {
    val active = javaModelSupertypeWalkGuard?.classIds
    if (active != null && !active.add(classId)) return default
    return try {
        block()
    } finally {
        active?.remove(classId)
    }
}

/**
 * Per-session `ClassId -> List<ClassId>` cache of resolved direct-supertype `ClassId`s for
 * [directSupertypeClassIds]. A class's direct supertypes are a pure function of the class and
 * the session; without this cache the transitive supertype-closure walks of
 * inherited-inner-class resolution are exponential in the hierarchy depth and effectively hang
 * large mixed Kotlin/Java compilations. Sessions without it fall back to the un-cached walk.
 */
internal class JavaModelDirectSupertypeCache : FirSessionComponent {
    val classIdToSupertypes: ConcurrentHashMap<ClassId, List<ClassId>> = ConcurrentHashMap()
}

private val FirSession.javaModelDirectSupertypeCache: JavaModelDirectSupertypeCache?
        by FirSession.nullableSessionComponentAccessor()

/** Registers a [JavaModelDirectSupertypeCache] on this session if one is not already present. */
@OptIn(SessionConfiguration::class)
internal fun FirSession.registerJavaModelDirectSupertypeCacheIfAbsent() {
    if (javaModelDirectSupertypeCache == null) {
        register(JavaModelDirectSupertypeCache::class, JavaModelDirectSupertypeCache())
    }
}

/**
 * Memoizes [compute] per [classId] on the session. Only non-empty results are cached: an empty
 * result is what the cycle guards return for an in-flight [classId], so caching it could pin a
 * transient empty over a class that does have supertypes; recomputing an empty result is cheap.
 */
internal fun FirSession.memoizedDirectSupertypeClassIds(
    classId: ClassId,
    compute: () -> List<ClassId>,
): List<ClassId> {
    val cache = javaModelDirectSupertypeCache?.classIdToSupertypes ?: return compute()
    cache[classId]?.let { return it }
    val result = compute()
    if (result.isNotEmpty()) cache[classId] = result
    return result
}

/**
 * Per-session `ClassId -> Boolean` cache of TYPE_USE-ness for annotation classes.
 *
 * Populated by [isTypeUseAnnotationClass]; the predicate is a static property of the annotation
 * class, so the cache key is just the [ClassId] and entries never need invalidation.
 */
internal class JavaModelTypeUseClassIdCache : FirSessionComponent {
    val classIdToIsTypeUse: ConcurrentHashMap<ClassId, Boolean> = ConcurrentHashMap()
}

private val FirSession.javaModelTypeUseClassIdCache: JavaModelTypeUseClassIdCache?
        by FirSession.nullableSessionComponentAccessor()

@OptIn(SessionConfiguration::class)
internal fun FirSession.registerJavaModelTypeUseCacheIfAbsent() {
    if (javaModelTypeUseClassIdCache == null) {
        register(JavaModelTypeUseClassIdCache::class, JavaModelTypeUseClassIdCache())
    }
}

/**
 * Whether the annotation class identified by [classId] carries `@Target(ElementType.TYPE_USE)`
 * (Java) or `@Target(AnnotationTarget.TYPE)` (Kotlin), using [FirSession.symbolProvider] for the
 * `@Target` lookup.
 *
 * Result is cached on the session via [JavaModelTypeUseClassIdCache].
 */
internal fun FirSession.isTypeUseAnnotationClass(classId: ClassId): Boolean {
    val cache = javaModelTypeUseClassIdCache?.classIdToIsTypeUse
    return if (cache != null) cache.getOrPut(classId) { computeIsTypeUseAnnotationClass(classId) }
    else computeIsTypeUseAnnotationClass(classId)
}

@OptIn(SymbolInternals::class)
private fun FirSession.computeIsTypeUseAnnotationClass(classId: ClassId): Boolean {
    val symbol = cycleSafeClassLikeSymbol(classId) ?: return false
    // Reject cross-package matches: PSI-based class finders match by simple name alone and may
    // return a class from a different package. Treating such results as TYPE_USE would conflate
    // unrelated annotations sharing the same simple name.
    if (symbol.classId != classId) return false
    val annotationClass = symbol.fir as? FirRegularClass ?: return false
    val targetAnnotation = annotationClass.annotations.find { firAnnotation ->
        val targetClassId = firAnnotation.annotationTypeRef.coneType.classId
        targetClassId == JvmStandardClassIds.Annotations.Java.Target ||
                targetClassId == StandardClassIds.Annotations.Target
    } ?: return false
    return hasTypeUseTarget(targetAnnotation)
}

/** Checks whether a @Target annotation lists TYPE_USE (Java) or TYPE (Kotlin) among its targets. */
private fun hasTypeUseTarget(targetAnnotation: FirAnnotation): Boolean {
    val argumentMapping = targetAnnotation.argumentMapping.mapping
    if (argumentMapping.isEmpty()) return false
    val argument = argumentMapping.values.firstOrNull() ?: return false
    return when (argument) {
        is FirVarargArgumentsExpression -> argument.arguments.any { isTypeUseElement(it) }
        is FirCollectionLiteral -> argument.argumentList.arguments.any { isTypeUseElement(it) }
        else -> isTypeUseElement(argument)
    }
}

/** Checks whether [expr] denotes `ElementType.TYPE_USE` (Java) or `AnnotationTarget.TYPE` (Kotlin). */
private fun isTypeUseElement(expr: FirExpression): Boolean = when (expr) {
    is FirEnumEntryDeserializedAccessExpression -> {
        val entryName = expr.enumEntryName.asString()
        entryName == "TYPE_USE" || entryName == "TYPE"
    }
    is FirPropertyAccessExpression -> {
        val calleeReference = expr.calleeReference
        if (calleeReference is FirResolvedNamedReference) {
            val name = calleeReference.name.asString()
            name == "TYPE_USE" || name == "TYPE"
        } else false
    }
    else -> false
}
