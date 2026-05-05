/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.Name

/**
 * Manages type parameter scoping and local class lookup for Java source resolution.
 *
 * Responsible for:
 * - Type parameter lookup (own high-priority params and inherited low-priority params)
 * - Local class resolution (inner classes, sibling classes, supertype-inherited classes, top-level classes)
 *
 * This class encapsulates the scoping logic that was previously embedded in [JavaResolutionContext].
 */
internal class JavaScopeResolver(
    private val sameFileTopLevelClassProvider: (Name) -> JavaClass?,
    private val containingClass: JavaClass?,
    private val inheritedMemberResolver: JavaInheritedMemberResolver,
    /** Type parameters with HIGH priority (method/class own params, win over inner class names). */
    val typeParametersInScope: Map<String, JavaTypeParameter> = emptyMap(),
    /** Type parameters with LOW priority (outer class inherited params, shadowed by inner class names). */
    val inheritedTypeParametersInScope: Map<String, JavaTypeParameter> = emptyMap(),
) {

    /** Returns type parameters with HIGH priority (method/class own params, win over inner class names). */
    fun findTypeParameter(name: String): JavaTypeParameter? = typeParametersInScope[name]

    /** Returns type parameters with LOW priority (outer class inherited params, shadowed by inner class names). */
    fun findInheritedTypeParameter(name: String): JavaTypeParameter? = inheritedTypeParametersInScope[name]

    /**
     * Finds a class by simple name in the AST-side scope.
     *
     * Checks (in order):
     * 1. Inner classes directly declared on the containing class chain (purely syntactic).
     * 2. Sibling inner classes — inner classes declared on the immediate outer class.
     * 3. Inherited inner classes from supertypes of the containing class
     *    (`findInnerClassFromSupertypes`, JLS 6.5.2).
     * 4. Inner classes declared on each outer class up the containing chain
     *    (so deeply-nested classes see siblings of every enclosing class).
     * 5. Top-level classes declared in the same compilation unit (`sameFileTopLevelClassProvider`).
     *
     * Stage 2 of the resolver-unification refactoring (see
     * `implDocs/RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`) narrowed this method by
     * removing the AST-side inherited-inner-class walk on each *outer* class in step 4 —
     * that path was redundant with the aggregated-map / BFS lookup performed by
     * [JavaResolutionContext.resolveFromLocalScope] step 2b for outer-of-outer cases.
     * The supertype walk on the *containing* class is retained (step 3) because it is
     * load-bearing for cases like `compiler/testData/diagnostics/tests/generics/innerClasses/j+k_complex.kt`
     * where the same-file aggregated-map / BFS path arrives at the inner class through
     * a different ClassId shape (the `JavaClass.fqName` path resolves directly to the
     * inherited inner's source-side ClassId, while the aggregated map is keyed by
     * supertype ClassIds that the FIR side has not yet materialized at the resolution
     * point).
     */
    fun findLocalClass(name: Name): JavaClass? {
        // 1. Inner classes of the containing class (purely syntactic AST query).
        containingClass?.findInnerClass(name)?.let { return it }
        // 2. Sibling inner classes — inner classes of the immediate outer class.
        // Handles cases like: class J { class AImpl {} class A extends AImpl {} }
        containingClass?.outerClass?.findInnerClass(name)?.let { return it }
        // 3. Inherited inner classes from the containing class's supertypes (JLS 6.5.2).
        // Retained as a load-bearing case (see KDoc above).
        containingClass?.let { cls ->
            inheritedMemberResolver.findInnerClassFromSupertypes(name, cls, mutableSetOf())?.let { return it }
        }
        // 4. Inner classes of each outer class up the containing chain.
        // For deeply-nested classes (Outer { Inner1 { Inner2 { ... } } }) Inner2 must
        // see siblings of every enclosing class.
        var outer = containingClass?.outerClass
        while (outer != null) {
            outer.outerClass?.findInnerClass(name)?.let { return it }
            outer = outer.outerClass
        }
        // 5. Top-level classes declared in the same compilation unit.
        return sameFileTopLevelClassProvider(name)
    }

    /**
     * Creates a new scope with additional OWN type parameters (high priority).
     * Used when entering a class or method that declares type parameters.
     * Own type params take priority over inner class names of the containing class.
     */
    fun withTypeParameters(typeParams: List<JavaTypeParameter>): JavaScopeResolver {
        if (typeParams.isEmpty()) return this
        val newScope = typeParametersInScope + typeParams.associateBy { it.name.asString() }
        return JavaScopeResolver(
            sameFileTopLevelClassProvider, containingClass, inheritedMemberResolver, newScope,
            inheritedTypeParametersInScope,
        )
    }

    /**
     * Creates a new scope with INHERITED type parameters from an outer class (low priority).
     * Used for static nested types where outer class type params are visible but can be
     * shadowed by inner class names of the static nested type itself.
     */
    fun withInheritedTypeParameters(typeParams: List<JavaTypeParameter>): JavaScopeResolver {
        if (typeParams.isEmpty()) return this
        val newInherited = inheritedTypeParametersInScope + typeParams.associateBy { it.name.asString() }
        return JavaScopeResolver(
            sameFileTopLevelClassProvider, containingClass, inheritedMemberResolver, typeParametersInScope,
            newInherited,
        )
    }

    /**
     * Creates a new scope for members of the given class.
     * Inner class references will be resolved against this class.
     */
    fun withContainingClass(newContainingClass: JavaClass): JavaScopeResolver {
        return JavaScopeResolver(
            sameFileTopLevelClassProvider,
            containingClass = newContainingClass,
            inheritedMemberResolver,
            typeParametersInScope,
            inheritedTypeParametersInScope,
        )
    }

}
