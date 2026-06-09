/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
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
 * Concretely, this fires when an annotation on a member of a Java class `C` has a simple name
 * that resolves to a candidate [ClassId] nested in `C`, and that candidate is probed through a
 * symbol provider that builds `C`'s nested-classifier scope by forcing `FirJavaClass.declarations`.
 * Materialising those declarations re-converts the same annotated member, which re-probes the same
 * candidate [ClassId]; the in-flight set short-circuits the second probe so resolution falls back
 * (e.g. to the top-level [ClassId] for that simple name) instead of recursing.
 *
 * See [JavaCycleBreakerTest] for more details.
 *
 * The marker is keyed per session: a re-entrant probe can arrive through a
 * different per-file context that wraps the same session, so the in-flight set must be shared
 * across them. It is a concurrent set, so the guard stays correct should resolution ever run on
 * more than one thread.
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

/**
 * Per-session set of [ClassId]s whose Java supertype graph is currently being walked by
 * [cycleGuardedSupertypeWalk].
 *
 * Re-entry to a [ClassId] already on the set returns the caller-supplied default without
 * recursing, which bounds direct (`A extends A`) and indirect (`A -> B -> A`) Java inheritance
 * cycles. Such cycles can only come from malformed Java source during error recovery; an
 * unbounded supertype walk over them (`directSupertypeClassIds` / `findInheritedNestedClass`)
 * would otherwise recurse until a `StackOverflowError`.
 *
 * See [JavaCycleBreakerTest] for more details.
 *
 * Keyed per session for the same reason as [JavaModelInFlightResolutions]: a re-entrant walk can
 * arrive through a different per-file context that wraps the same session, so the active set must
 * be shared across them. It is a concurrent set, so the guard stays correct should resolution ever
 * run on more than one thread.
 *
 * Registered by [registerJavaModelSupertypeWalkGuardIfAbsent]; sessions without it run the walk
 * unguarded (parsing-level fixtures never build the cyclic supertype graphs that need bounding).
 */
internal class JavaModelSupertypeWalkGuard : FirSessionComponent {
    val classIds: MutableSet<ClassId> = ConcurrentHashMap.newKeySet()
}

private val FirSession.javaModelSupertypeWalkGuard: JavaModelSupertypeWalkGuard?
        by FirSession.nullableSessionComponentAccessor()

/** Registers a [JavaModelSupertypeWalkGuard] on this session if one is not already present. */
@OptIn(SessionConfiguration::class)
internal fun FirSession.registerJavaModelSupertypeWalkGuardIfAbsent() {
    if (javaModelSupertypeWalkGuard == null) {
        register(JavaModelSupertypeWalkGuard::class, JavaModelSupertypeWalkGuard())
    }
}

/**
 * Runs [block] guarded against re-entry on [classId]'s supertype walk: if [classId] is already
 * being walked on this session, returns [default] without invoking [block]; otherwise marks
 * [classId] in-flight, runs [block], and clears the mark in a `finally`.
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
 * Per-session `ClassId -> Boolean` cache of TYPE_USE-ness for annotation classes.
 *
 * Populated by [isTypeUseAnnotationClass]; the predicate is a static property of the annotation
 * class, so the cache key is just the [ClassId] and entries never need invalidation.
 *
 * Registered by [registerJavaModelTypeUseCacheIfAbsent]; sessions without it fall back to the
 * un-cached path inside [isTypeUseAnnotationClass].
 */
internal class JavaModelTypeUseClassIdCache : FirSessionComponent {
    val classIdToIsTypeUse: ConcurrentHashMap<ClassId, Boolean> = ConcurrentHashMap()
}

private val FirSession.javaModelTypeUseClassIdCache: JavaModelTypeUseClassIdCache?
        by FirSession.nullableSessionComponentAccessor()

/** Registers a [JavaModelTypeUseClassIdCache] on this session if one is not already present. */
@OptIn(SessionConfiguration::class)
internal fun FirSession.registerJavaModelTypeUseCacheIfAbsent() {
    if (javaModelTypeUseClassIdCache == null) {
        register(JavaModelTypeUseClassIdCache::class, JavaModelTypeUseClassIdCache())
    }
}

/**
 * Whether the annotation class identified by [classId] carries `@Target(ElementType.TYPE_USE)`
 * (Java) or `@Target(AnnotationTarget.TYPE)` (Kotlin). Mirrors javac-wrapper's structure-build-time
 * `filterTypeAnnotations` predicate, using [FirSession.symbolProvider] for the @Target lookup.
 *
 * Result is cached on the session via [JavaModelTypeUseClassIdCache] so each annotation class
 * is probed at most once per build.
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
    // return a class from a different package (see KotlinCliJavaFileManagerImpl.findClass fallback
    // paths). Treating such results as TYPE_USE would conflate unrelated annotations sharing the
    // same simple name.
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
