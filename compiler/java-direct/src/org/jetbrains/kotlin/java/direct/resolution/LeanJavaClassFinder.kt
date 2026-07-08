/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId

/**
 * Cross-file class lookup capabilities needed by the resolution context and inherited-class
 * resolution.
 *
 * Decouples [JavaResolutionContext] and [resolveInheritedInnerClassToClassId] from the concrete
 * [JavaClassFinderOverAstImpl][org.jetbrains.kotlin.java.direct.JavaClassFinderOverAstImpl],
 * exposing only the operations they actually use.
 */
internal interface LeanJavaClassFinder {
    /** Checks if a top-level class with the given [classId] is present in the source index. */
    fun isClassInIndex(classId: ClassId): Boolean

    /** Finds a class by [ClassId], returning null if not found. */
    fun findClass(request: JavaClassFinder.Request): JavaClass?

    /** Recursively collects all inner class names from the supertype hierarchy. */
    fun collectInheritedInnerClasses(classId: ClassId): Map<String, Set<ClassId>>

    /**
     * Returns the direct supertype `ClassId`s for a Java source [classId], resolved using
     * **that class's own file imports** (not the caller's). Used by transitive supertype
     * walks so each level of the hierarchy is resolved against the right import scope.
     *
     * Includes both source and binary supertype `ClassId`s (caller decides existence via
     * a `tryResolve` probe / per-origin dispatcher).
     */
    fun getDirectSupertypes(classId: ClassId): List<ClassId>
}
