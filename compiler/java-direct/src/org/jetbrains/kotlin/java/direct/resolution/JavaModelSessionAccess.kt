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
