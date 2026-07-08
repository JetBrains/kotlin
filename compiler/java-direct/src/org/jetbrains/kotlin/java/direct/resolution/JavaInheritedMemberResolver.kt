/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.splitCanonicalFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Resolves inherited inner classes from supertype hierarchies (JLS 6.5.2 — inherited member
 * types are in scope).
 *
 * One generic, origin-agnostic path handles same-file, cross-file source, binary Java, and
 * Kotlin supertypes alike:
 * - [resolveInheritedInnerClassToClassId] returns a bare `ClassId` via a single BFS
 *   ([walkSupertypeClassIds]), with one exception for `containingClass`'s own direct
 *   supertypes — see that function's KDoc.
 * - [findInnerClassFromSupertypes] materializes that `ClassId` back into a navigable [JavaClass]
 *   with its full AST-side outer-class chain, needed to thread outer-class type arguments for
 *   generic outer classes.
 */
internal class JavaInheritedMemberResolver {

    /**
     * Searches for an inner class with the given name in [javaClass]'s supertype hierarchy.
     *
     * Returns null if multiple inner classes with the same name are found (ambiguity),
     * matching `javac`'s `MISSING_DEPENDENCY_CLASS` error — enforced by [resolveInherited]'s BFS,
     * which compares every same-file, cross-file-source, binary-Java, and Kotlin candidate
     * together (regression test:
     * `testData/diagnostics/tests/jvm/javaDirect/ambiguousInheritedInnerClassAcrossSourceAndKotlinSupertypes.kt`).
     *
     * @param resolveInherited resolves an inherited inner class of [javaClass] to a `ClassId`,
     *        via [resolveInheritedInnerClassToClassId]'s generic ladder.
     * @param classifierAdapterFor materializes the `ClassId` found by [resolveInherited] back
     *        into a navigable [JavaClass] — routes source-backed results to their canonical
     *        [JavaClassOverAst], wraps binary/Kotlin results in a [FirBackedJavaClassAdapter].
     */
    fun findInnerClassFromSupertypes(
        name: Name,
        javaClass: JavaClass,
        resolveInherited: (JavaClass, Name) -> ClassId?,
        classifierAdapterFor: (ClassId) -> JavaClass?,
    ): JavaClass? {
        val inheritedId = resolveInherited(javaClass, name) ?: return null
        return classifierAdapterFor(inheritedId)
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
     * [JavaTypeResolver.findInheritedNestedClass] reuses this same safety for its qualified
     * `Outer.Nested` lookup (regression test: `qualifiedInheritedNestedClassInOwnImplementsClause.kt`).
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
        // still yields the full `a.B.C`. An empty segment (or no segments at all) only arises
        // from parser error-recovery AST shapes for a malformed extends/implements clause —
        // javac is expected to report the error, so declining here rather than crashing is
        // purely defensive.
        val initialAncestorIds = containingClass.supertypes.mapNotNull { st ->
            val segments = st.presentableText.splitCanonicalFqName().map { it.substringBefore('<').trim() }
            if (segments.isEmpty() || segments.any { it.isEmpty() }) null
            else resolveWithoutInheritance(segments.joinToString("."))
        }

        return walkSupertypeClassIds(simpleName, initialAncestorIds, directSupertypeClassIds, tryResolve, mutableSetOf())
    }

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
                } else {
                    // Expansion is decided per ancestor, not by whether some other ancestor at
                    // this level already matched — otherwise a match found via one ancestor
                    // would stop an unrelated sibling ancestor from being expanded, hiding a
                    // deeper conflicting match one or more levels further down (regression test:
                    // testAmbiguousInheritedInnerClassAcrossSourceAndKotlinSupertypes.kt).
                    nextLevelIds.addAll(directSupertypeClassIds(ancestorId))
                }
            }

            currentLevelIds = nextLevelIds
        }
        return foundClassId
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
