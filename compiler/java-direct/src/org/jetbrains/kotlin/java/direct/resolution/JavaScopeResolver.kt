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
     * Finds a [JavaClass] for a simple name in the AST-side scope.
     *
     * Checks (in order):
     * 1. Inner classes directly declared on the containing class (purely syntactic AST query).
     * 2. Sibling inner classes — inner classes declared on the immediate outer class.
     * 3. Inherited inner classes from the containing class's supertypes
     *    ([JavaInheritedMemberResolver.findInnerClassFromSupertypes], JLS 6.5.2).
     * 4. Inner classes declared on each outer class up the containing chain
     *    (so deeply-nested classes see siblings of every enclosing class).
     * 5. Top-level classes declared in the same compilation unit (`sameFileTopLevelClassProvider`).
     *
     * **Post-Stage-4 role.** As of Stage 4 of the resolver-unification refactoring (see
     * `implDocs/RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`), this method is **no
     * longer in the `ClassId`-resolution path**: `JavaResolutionContext.resolveFromLocalScope`
     * now walks `getContainingClassIds()` and probes the FIR symbol provider via the
     * `tryResolve(ClassId)` callback (and uses [JavaInheritedMemberResolver]'s aggregated
     * map / BFS for inherited inners). What remains here is the AST-side fast path used by
     * the Java model layer:
     *
     *  - [JavaTypeOverAst.computeClassifier] reads the result as a [JavaClassifier], which
     *    must be a structural [JavaClass] (with its full AST-side outer-class chain) for
     *    multi-part name navigation via [JavaClass.findInnerClass] and for outer-class
     *    type-argument substitution to flow through Java-source supertype chains
     *    (`compiler/testData/diagnostics/tests/generics/innerClasses/j+k_complex.kt`
     *    relies on this — see the 2026-05-05 / 2026-05-06 entries in
     *    `compiler/java-direct/ITERATION_RESULTS.md`).
     *  - [org.jetbrains.kotlin.java.direct.JavaClassCache] / [ConstantEvaluator] also need
     *    the AST [JavaClass] to materialise inner-class symbols and constant references.
     *
     * **Stage 5 deferral.** The unification doc's Stage-5 vision shrinks the AST side to
     * "type parameter?" + "[getContainingClassIds]" only. Subsuming this method (and the
     * inherited-inner step in particular) requires giving the AST classifier path a
     * FIR-derived `JavaClass` for cross-file inherited inners, which the existing
     * `getClassLikeSymbol` callback alone does not provide. See
     * [JavaInheritedMemberResolver]'s class KDoc for the trip-wires that motivate the
     * deferral.
     */
    fun findLocalClass(name: Name): JavaClass? {
        // 1. Inner classes of the containing class (purely syntactic AST query, plus
        // [JavaClassOverAst.findInnerClassInSupertypes] same-file supertype walk).
        containingClass?.findInnerClass(name)?.let { return it }
        // 2. Sibling inner classes — inner classes of the immediate outer class.
        // Handles cases like: class J { class AImpl {} class A extends AImpl {} }
        containingClass?.outerClass?.findInnerClass(name)?.let { return it }
        // 3. Inherited inner classes from the containing class's supertypes (JLS 6.5.2).
        // Required for cross-file Java-source supertypes; see KDoc above.
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
            inheritedMemberResolver = inheritedMemberResolver,
            typeParametersInScope = typeParametersInScope,
            inheritedTypeParametersInScope = inheritedTypeParametersInScope,
        )
    }

}
