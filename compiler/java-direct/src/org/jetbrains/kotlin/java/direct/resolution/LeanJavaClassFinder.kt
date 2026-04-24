/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId

/**
 * Cross-file class lookup capabilities needed by the resolution context and inherited member resolver.
 *
 * Decouples [JavaResolutionContext] and [JavaInheritedMemberResolver] from the concrete
 * [JavaClassFinderOverAstImpl][org.jetbrains.kotlin.java.direct.JavaClassFinderOverAstImpl],
 * exposing only the three operations they actually use.
 */
internal interface LeanJavaClassFinder {
    /** Checks if a top-level class with the given [classId] is present in the source index. */
    fun isClassInIndex(classId: ClassId): Boolean

    /** Finds a class by [ClassId], returning null if not found. */
    fun findClass(request: JavaClassFinder.Request): JavaClass?

    /** Recursively collects all inner class names from the supertype hierarchy. */
    fun collectInheritedInnerClasses(classId: ClassId): Map<String, Set<ClassId>>
}
