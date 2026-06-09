/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * Per-position immutable scope **data** for Java source resolution.
 *
 * Forked via [withTypeParameters] / [withInheritedTypeParameters] / [withContainingClass].
 */
internal class JavaScopeContext(
    /**
     * Same-file top-level class provider, keyed by simple name. Used by:
     * - [findClassInCurrentScope] step 5 — the AST classifier fast path's same-file fallback.
     * - [resolveFromSameFile] — Step 2 of the JLS 6.4.1 simple-name dispatcher, so a
     *   same-file top-level class shadows single-type imports (a cross-file same-package class
     *   does *not*).
     */
    val sameFileTopLevelClassProvider: (Name) -> JavaClass?,
    val containingClass: JavaClass?,
    /** Type parameters with HIGH priority (method/class own params, win over inner class names). */
    val typeParametersInScope: Map<String, JavaTypeParameter> = emptyMap(),
    /** Type parameters with LOW priority (outer class inherited params, shadowed by inner class names). */
    val inheritedTypeParametersInScope: Map<String, JavaTypeParameter> = emptyMap(),
    /**
     * Lazily computed aggregated inherited inner classes for the entire containing class chain.
     * Maps simpleName -> Set<ClassId> across the containing class and all its outer classes.
     * Cached to avoid re-walking the outer class chain on every simple-name resolution.
     *
     * Keyed by [containingClass]: shared by reference across [withTypeParameters] /
     * [withInheritedTypeParameters] forks (containing class unchanged) and reset on
     * [withContainingClass] (containing class changed, aggregated inherited inner classes may differ).
     */
    val inheritedInnerCache: InheritedInnerCache = InheritedInnerCache(),
) {
    class InheritedInnerCache {
        @Volatile
        var value: Map<String, Set<ClassId>>? = null
    }

    /**
     * Creates a new scope with additional OWN type parameters (high priority).
     * Used when entering a class or method that declares type parameters.
     * Own type params take priority over inner class names of the containing class.
     */
    fun withTypeParameters(typeParams: List<JavaTypeParameter>): JavaScopeContext {
        if (typeParams.isEmpty()) return this
        val newScope = typeParametersInScope + typeParams.associateBy { it.name.asString() }
        return JavaScopeContext(
            sameFileTopLevelClassProvider, containingClass, newScope,
            inheritedTypeParametersInScope,
            inheritedInnerCache, // share — containingClass unchanged
        )
    }

    /**
     * Creates a new scope with INHERITED type parameters from an outer class (low priority).
     * Used for static nested types where outer class type params are visible but can be
     * shadowed by inner class names of the static nested type itself.
     */
    fun withInheritedTypeParameters(typeParams: List<JavaTypeParameter>): JavaScopeContext {
        if (typeParams.isEmpty()) return this
        val newInherited = inheritedTypeParametersInScope + typeParams.associateBy { it.name.asString() }
        return JavaScopeContext(
            sameFileTopLevelClassProvider, containingClass, typeParametersInScope,
            newInherited,
            inheritedInnerCache, // share — containingClass unchanged
        )
    }

    /**
     * Creates a new scope for members of the given class.
     * Inner class references will be resolved against this class.
     */
    fun withContainingClass(newContainingClass: JavaClass): JavaScopeContext {
        return JavaScopeContext(
            sameFileTopLevelClassProvider,
            containingClass = newContainingClass,
            typeParametersInScope = typeParametersInScope,
            inheritedTypeParametersInScope = inheritedTypeParametersInScope,
            // fresh InheritedInnerCache — containingClass changed
        )
    }
}
