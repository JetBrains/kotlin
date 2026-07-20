/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.splitCanonicalFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Resolves inherited inner classes from supertype hierarchies (JLS 6.5.2 — inherited member
 * types are in scope). One origin-agnostic path handles same-file, cross-file source, binary
 * Java, and Kotlin supertypes alike.
 */

/**
 * Searches [javaClass]'s supertype hierarchy for an inner class named [name] and materializes it
 * as a navigable [JavaClass].
 */
context(c: JavaResolutionContext)
internal fun findInnerClassFromSupertypes(name: Name, javaClass: JavaClass): JavaClass? {
    val inheritedId = resolveInheritedInnerClassToClassId(name.asString(), javaClass) ?: return null
    return classifierAdapterFor(inheritedId)
}

/**
 * Resolves [simpleName] as an inner class inherited from [containingClass]'s supertypes.
 *
 * [containingClass]'s own direct supertypes are read from raw AST text rather than through
 * [directSupertypeClassIds]: this lookup can run while [containingClass]'s own supertype list is
 * still being computed (e.g. for a name used inside its own extends/implements clause), and
 * reading `.classifier` at that point would re-enter the in-progress computation. Deeper
 * ancestors cannot be the class currently being resolved, so they are walked via
 * [directSupertypeClassIds].
 *
 * Only [containingClass]'s own supertypes are searched; callers walk the containing-class chain
 * themselves, preserving the JLS 6.4.1 shadowing rule between levels.
 */
context(c: JavaResolutionContext)
internal fun resolveInheritedInnerClassToClassId(simpleName: String, containingClass: JavaClass?): ClassId? {
    containingClass ?: return null

    // `splitCanonicalFqName()` splits per dotted segment before stripping generics, so
    // `a.B<String>.C` yields `a.B.C`. Empty segments only arise from error-recovery AST for a
    // malformed clause; decline defensively rather than crash.
    val initialAncestorIds = containingClass.supertypes.mapNotNull { st ->
        val segments = st.presentableText.splitCanonicalFqName().map { it.substringBefore('<').trim() }
        if (segments.isEmpty() || segments.any { it.isEmpty() }) null
        else resolveWithoutInheritance(segments.joinToString("."))
    }

    return walkSupertypeClassIds(simpleName, initialAncestorIds)
}

/**
 * Resolves [name] without checking inherited inner classes, avoiding infinite recursion back
 * into [resolveInheritedInnerClassToClassId].
 */
context(c: JavaResolutionContext)
private fun resolveWithoutInheritance(name: String): ClassId? =
    if (name.contains('.')) {
        resolveQualifiedNameToClassIdFromParts(name.split('.'), fullResolution = false)
    } else {
        resolveSimpleNameToClassIdImpl(name, fullResolution = false)
    }

/**
 * BFS over ancestor `ClassId`s: probes `ancestorId.simpleName` via [tryResolveInherited] at each
 * level and expands unmatched ancestors via [directSupertypeClassIds]. Returns the single match,
 * or `null` if nothing was found or two different matches make the name ambiguous. Terminates via
 * `visited` plus [directSupertypeClassIds]'s per-session cycle guard.
 */
context(c: JavaResolutionContext)
private fun walkSupertypeClassIds(simpleName: String, initialAncestorIds: List<ClassId>): ClassId? {
    val visited = mutableSetOf<ClassId>()
    var foundClassId: ClassId? = null
    var currentLevelIds = initialAncestorIds

    while (currentLevelIds.isNotEmpty()) {
        val nextLevelIds = mutableListOf<ClassId>()

        for (ancestorId in currentLevelIds) {
            if (!visited.add(ancestorId)) continue

            val innerClassId = ancestorId.createNestedClassId(Name.identifier(simpleName))
            if (tryResolveInherited(innerClassId) && isInheritedNestedClassAccessible(innerClassId)) {
                if (foundClassId != null && foundClassId != innerClassId) return null
                foundClassId = innerClassId
            } else {
                // Also expand past an existing-but-inaccessible match: a member that is not
                // inherited (private, or package-private from another package) is not in scope, so
                // an accessible same-named nested class deeper in the hierarchy must still be found.
                // Expand even when a sibling ancestor at this level already matched, so a
                // conflicting deeper match is still detected as ambiguous.
                nextLevelIds.addAll(directSupertypeClassIds(ancestorId))
            }
        }

        currentLevelIds = nextLevelIds
    }
    return foundClassId
}

/**
 * Whether an inherited nested class is in scope as a member of the class whose supertypes are
 * being walked — i.e. whether it is inherited per JLS 8.2, so it can shadow a same-named top-level
 * or same-package class. A non-inherited nested class is not a member and must not shadow. The
 * walked class is by construction a subtype of every ancestor probed here, so:
 *  - `private` — never inherited;
 *  - package-private — inherited only within the declaring package;
 *  - `public` / `protected` / Kotlin `internal` (public in bytecode) — always inherited by a
 *    subtype, hence in scope.
 *
 * Defaults to accessible when the nested class cannot be materialised.
 */
context(c: JavaResolutionContext)
private fun isInheritedNestedClassAccessible(innerClassId: ClassId): Boolean {
    val nestedClass = classifierAdapterFor(innerClassId) ?: return true
    return when (nestedClass.visibility) {
        Visibilities.Private -> false
        JavaVisibilities.PackageVisibility -> innerClassId.packageFqName == c.packageFqName
        else -> true
    }
}
