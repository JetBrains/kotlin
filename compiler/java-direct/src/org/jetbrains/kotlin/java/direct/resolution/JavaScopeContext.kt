/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.Name

/**
 * Per-position immutable scope data for Java source resolution.
 *
 * Forked via [withTypeParameters] / [withInheritedTypeParameters] / [withContainingClass].
 */
internal class JavaScopeContext(
    /**
     * Same-file top-level class provider, indexed by simple name. Needed because a same-file
     * top-level class shadows single-type imports (JLS 6.4.1) while a cross-file same-package
     * class does not, and the two share the same `ClassId`.
     */
    val sameFileTopLevelClassProvider: (Name) -> JavaClass?,
    val containingClass: JavaClass?,
    /** Type parameters with HIGH priority (method/class own params, win over inner class names). */
    val typeParametersInScope: Map<String, JavaTypeParameter> = emptyMap(),
    /**
     * Type parameters with LOW priority (outer class inherited params, shadowed by inner class names).
     *
     * Exists solely for PSI parity described in [JavaClassOverAst.findInnerClassImpl]
     * TODO: remove (KT-87797)
     */
    val inheritedTypeParametersInScope: Map<String, JavaTypeParameter> = emptyMap(),
) {
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
        )
    }

    /**
     * Creates a new scope with INHERITED type parameters from an outer class (low priority).
     * Used for static nested types where outer class type params are visible but can be
     * shadowed by inner class names of the static nested type itself. See
     * [inheritedTypeParametersInScope] — this whole path exists only for PSI parity and is
     * removable with it.
     * TODO: remove (KT-87797)
     */
    fun withInheritedTypeParameters(typeParams: List<JavaTypeParameter>): JavaScopeContext {
        if (typeParams.isEmpty()) return this
        val newInherited = inheritedTypeParametersInScope + typeParams.associateBy { it.name.asString() }
        return JavaScopeContext(
            sameFileTopLevelClassProvider, containingClass, typeParametersInScope,
            newInherited,
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
        )
    }
}
