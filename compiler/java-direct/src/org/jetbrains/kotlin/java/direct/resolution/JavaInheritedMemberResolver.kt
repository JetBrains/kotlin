/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.load.java.structure.impl.splitCanonicalFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Resolves inherited inner classes from supertype hierarchies (JLS 6.5.2 — inherited member
 * types are in scope).
 *
 * Three entry points:
 * - [findInnerClassFromSupertypes] returns a [JavaClass] with its full AST-side outer-class
 *   chain, needed to thread outer-class type arguments for generic outer classes.
 * - [resolveInheritedInnerClassToClassId] returns a bare `ClassId` via a single, origin-agnostic
 *   BFS ([walkSupertypeClassIds]), with one exception for `containingClass`'s own direct
 *   supertypes — see that function's KDoc.
 * - [resolveInheritedNestedClassId] also returns a bare `ClassId`, but starting from a single
 *   already-resolved `ClassId` of any origin rather than a containing-class chain; it reuses
 *   [walkSupertypeClassIds] directly, seeded with that `ClassId`'s own direct supertypes.
 */
internal class JavaInheritedMemberResolver(
    private val classFinder: LeanJavaClassFinder?,
    private val sameFileTopLevelClassProvider: (Name) -> JavaClass?,
) {

    /**
     * Searches for an inner class with the given name in the supertype hierarchy.
     *
     * Returns null if multiple inner classes with the same name are found (ambiguity),
     * matching `javac`'s `MISSING_DEPENDENCY_CLASS` error. Uses the [classFinder] (if
     * available) to detect cross-file ambiguities and to materialize the inherited
     * `JavaClass` for cross-file Java-source supertypes; falls back to
     * [sameFileTopLevelClassProvider] for same-file supertypes.
     *
     * Ambiguity is compared within each arm, not across them: a match from the same-file loop
     * below (including one surfaced by its own recursive calls into a deeper supertype's
     * cross-file/binary/Kotlin arms) is returned without checking whether [classFinder] or
     * [resolveBinaryOrKotlinInherited] would also match a different candidate among [javaClass]'s
     * own supertypes, and a non-null [classFinder] result — ambiguous or not — skips
     * [resolveBinaryOrKotlinInherited] entirely. Same accepted source/binary mixed-hierarchy
     * trade-off as review.md comment #7.
     *
     * @param resolveBinaryOrKotlinInherited the binary/Kotlin tail, tried only after the
     *        same-file and cross-file-source arms above have found nothing for [javaClass]
     *        itself: resolves an inherited inner class of [javaClass] to a `ClassId` by reusing
     *        [resolveInheritedInnerClassToClassId]'s generic ladder (already binary/Kotlin-aware
     *        via [directSupertypeClassIds]).
     * @param classifierAdapterFor materializes the `ClassId` found by
     *        [resolveBinaryOrKotlinInherited] back into a navigable [JavaClass] — routes
     *        source-backed results to their canonical [JavaClassOverAst], wraps binary/Kotlin
     *        results in a [FirBackedJavaClassAdapter].
     */
    fun findInnerClassFromSupertypes(
        name: Name,
        javaClass: JavaClassOverAst,
        visited: MutableSet<JavaClass>,
        resolveBinaryOrKotlinInherited: (JavaClass, Name) -> ClassId?,
        classifierAdapterFor: (ClassId) -> JavaClass?,
    ): JavaClass? {
        if (!visited.add(javaClass)) return null

        var foundInnerClass: JavaClass? = null

        // Same-file supertypes — local resolution by simple name via resolveSameFileSupertype,
        // not classFinder.collectInheritedInnerClasses below: the latter's candidate generator
        // (JavaSupertypeGraph.resolveSupertypeReference) deliberately avoids triggering
        // resolution and so declines dotted supertype references, which would miss a same-file
        // qualified supertype like `class Foo extends x.S`; same-file JavaClassOverAst data is
        // also already free to navigate in memory, so caching it there would buy nothing. Cross-
        // file source supertypes are handled by the classFinder fallback below; binary/Kotlin
        // supertypes by the tail.
        for (supertype in javaClass.supertypes) {
            val supertypeClass = resolveSameFileSupertype(supertype) ?: continue
            (supertypeClass.findInnerClass(name) ?: findInnerClassFromSupertypes(name, supertypeClass, visited, resolveBinaryOrKotlinInherited, classifierAdapterFor))?.let {
                if (foundInnerClass == null) foundInnerClass = it else return null
            }
        }

        if (foundInnerClass != null) return foundInnerClass

        val containingClassId = javaClass.classId ?: return null
        if (classFinder != null) {
            val candidates = classFinder.collectInheritedInnerClasses(containingClassId)[name.asString()]
            // A non-null `candidates` map entry means the source hierarchy has an opinion on
            // [name] (including ambiguity, via `singleOrNull()` returning null for size > 1) and
            // must not fall through to the tail below, which could otherwise silently pick one
            // of the ambiguous candidates and mask the ambiguity.
            if (candidates != null) {
                return candidates.singleOrNull()?.let { classFinder.findClass(JavaClassFinder.Request(it)) }
            }
        }

        // Binary/Kotlin tail — the same-file and cross-file-source arms above found nothing for
        // `javaClass`'s own supertypes; fall through to the generic ClassId ladder, which is
        // already binary/Kotlin-aware.
        val inheritedId = resolveBinaryOrKotlinInherited(javaClass, name) ?: return null
        return classifierAdapterFor(inheritedId)
    }

    /**
     * Resolves a same-file supertype reference to the [JavaClassOverAst] it denotes.
     *
     * Only AST-backed same-file classes are produced here: the outermost segment comes from
     * [sameFileTopLevelClassProvider] and each nested segment from [JavaClass.findInnerClass], both
     * of which yield [JavaClassOverAst] for same-file classes. The result is narrowed accordingly so
     * [findInnerClassFromSupertypes] keeps recursing only over AST classes.
     */
    private fun resolveSameFileSupertype(supertype: JavaClassifierType): JavaClassOverAst? {
        val segments = supertype.presentableText.splitCanonicalFqName().map { it.substringBefore('<').trim() }
        // An empty segment (or no segments at all) only arises from parser error-recovery AST
        // shapes for a malformed extends/implements clause — not reachable for well-formed code.
        // The error is supposed to be reported by javac.
        if (segments.isEmpty() || segments.any { it.isEmpty() }) return null
        var resolved = sameFileTopLevelClassProvider(Name.identifier(segments.first())) as? JavaClassOverAst ?: return null
        for (i in 1 until segments.size) {
            resolved = resolved.findInnerClass(Name.identifier(segments[i])) as? JavaClassOverAst ?: return null
        }
        return resolved
    }

    /**
     * Tries to resolve a simple name as an inner class inherited from supertypes, via a single
     * origin-agnostic BFS ([walkSupertypeClassIds]) over [containingClass]'s ancestors.
     *
     * [containingClass]'s own direct supertypes are the one exception: they are read from raw
     * AST text via [resolveWithoutInheritance] rather than through [directSupertypeClassIds].
     * [containingClass]'s own `SUPER_TYPES` FIR phase can still be on the call stack here —
     * e.g. resolving a name used inside [containingClass]'s own extends/implements clause goes
     * through this same lookup before [containingClass]'s supertypes have finished resolving.
     * Reading `.classifier` at this level, as [directSupertypeClassIds]'s source-Java arm does,
     * would re-enter that in-progress computation. Every ancestor beyond this first level is
     * safe to walk via [directSupertypeClassIds]: none of them is the class currently being
     * resolved (regression test:
     * `JavaParsingTypeResolutionTest.testResolveInheritedInnerClassToClassIdNeverQueriesContainingClassOwnSupertypeClassIds`).
     *
     * Only [containingClass]'s own supertypes are searched, not those of its outer classes —
     * callers needing outer-class coverage walk the containing-class chain themselves and call
     * this once per level, preserving the JLS 6.4.1 shadowing rule between levels.
     *
     * @param resolveWithoutInheritance resolves a name without checking inherited inner
     *        classes, avoiding infinite recursion back into this method.
     */
    fun resolveInheritedInnerClassToClassId(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        directSupertypeClassIds: (ClassId) -> List<ClassId>,
        containingClass: JavaClass?,
        resolveWithoutInheritance: (String) -> ClassId?,
    ): ClassId? {
        containingClass ?: return null

        // `splitCanonicalFqName()` splits per dotted segment before stripping generics, so a
        // qualified reference with type arguments on a non-final segment (`a.B<String>.C`)
        // still yields the full `a.B.C`.
        val initialAncestorIds = containingClass.supertypes.mapNotNull { st ->
            val segments = st.presentableText.splitCanonicalFqName().map { it.substringBefore('<').trim() }
            if (segments.isEmpty() || segments.any { it.isEmpty() }) null
            else resolveWithoutInheritance(segments.joinToString("."))
        }

        return walkSupertypeClassIds(simpleName, initialAncestorIds, directSupertypeClassIds, tryResolve, mutableSetOf())
    }

    /**
     * Resolves [simpleName] to an inherited nested-class `ClassId`, searching [classId]'s own
     * supertypes rather than a containing-class chain — used for qualified `Outer.Nested`
     * references once `Outer` is already resolved to a `ClassId`, whatever its origin (source,
     * binary Java, or Kotlin). Reuses [walkSupertypeClassIds] directly, seeded with [classId]'s
     * own direct supertypes.
     */
    fun resolveInheritedNestedClassId(
        simpleName: String,
        classId: ClassId,
        tryResolve: (ClassId) -> Boolean,
        directSupertypeClassIds: (ClassId) -> List<ClassId>,
    ): ClassId? = walkSupertypeClassIds(
        simpleName, directSupertypeClassIds(classId), directSupertypeClassIds, tryResolve, mutableSetOf(),
    )

    /**
     * BFS over ancestor `ClassId`s: at every level, probes `ancestorId.SimpleName` for each id in
     * [initialAncestorIds] / the current level via [tryResolve], then expands each unmatched id
     * to its own direct supertypes via [directSupertypeClassIds] for the next level —
     * origin-agnostic per hop, so source, Kotlin, and binary Java ancestors can all appear in the
     * same walk and are compared for ambiguity together. Shares [visited] across calls so
     * cross-call ambiguity is still detected. Termination relies solely on [visited] (bounded by
     * the finite set of distinct `ClassId`s reachable from [initialAncestorIds]) plus
     * [directSupertypeClassIds]'s own per-session cycle guard.
     *
     * Returns the found inner-class `ClassId`, or `null` if nothing was found or if ambiguity is
     * detected (two different matches).
     */
    private fun walkSupertypeClassIds(
        simpleName: String,
        initialAncestorIds: List<ClassId>,
        directSupertypeClassIds: (ClassId) -> List<ClassId>,
        tryResolve: (ClassId) -> Boolean,
        visited: MutableSet<ClassId>,
    ): ClassId? {
        var foundClassId: ClassId? = null
        var currentLevelIds = initialAncestorIds

        while (currentLevelIds.isNotEmpty()) {
            val nextLevelIds = mutableListOf<ClassId>()

            for (ancestorId in currentLevelIds) {
                if (!visited.add(ancestorId)) continue

                val innerClassId = ancestorId.createNestedClassId(Name.identifier(simpleName))
                if (tryResolve(innerClassId)) {
                    if (foundClassId != null && foundClassId != innerClassId) return null
                    foundClassId = innerClassId
                }
                if (foundClassId == null) {
                    nextLevelIds.addAll(directSupertypeClassIds(ancestorId))
                }
            }

            if (foundClassId != null) return foundClassId
            currentLevelIds = nextLevelIds
        }
        return null
    }
}

internal fun fqNameInPackageToClassId(fqName: FqName, packageFqName: FqName): ClassId {
    val fqnString = fqName.asString()
    val pkgString = packageFqName.asString()

    val className = if (pkgString.isEmpty()) {
        fqnString
    } else if (fqnString.startsWith(pkgString) && fqnString.length > pkgString.length && fqnString[pkgString.length] == '.') {
        fqnString.substring(pkgString.length + 1)
    } else {
        fqnString
    }

    return ClassId(packageFqName, FqName(className), isLocal = false)
}
