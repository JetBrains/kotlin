/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages type parameter scoping and local class lookup for Java source resolution.
 *
 * Responsible for:
 * - Type parameter lookup (own high-priority params and inherited low-priority params)
 * - Local class resolution (inner classes, sibling classes, supertype-inherited classes, top-level classes)
 * - Caching of local class lookup results
 *
 * This class encapsulates the scoping logic that was previously embedded in [JavaResolutionContext].
 */
internal class JavaScopeResolver(
    private val localClassProvider: (Name) -> JavaClass?,
    private val containingClassProvider: (() -> JavaClass?)?,
    private val inheritedMemberResolver: JavaInheritedMemberResolver,
    /** Type parameters with HIGH priority (method/class own params, win over inner class names). */
    val typeParametersInScope: Map<String, JavaTypeParameter> = emptyMap(),
    /** Type parameters with LOW priority (outer class inherited params, shadowed by inner class names). */
    val inheritedTypeParametersInScope: Map<String, JavaTypeParameter> = emptyMap(),
    /**
     * Cache for [findLocalClass] results. Shared across scopes that have the same
     * [containingClassProvider] and [localClassProvider] (i.e., scopes created via
     * [withTypeParameters] / [withInheritedTypeParameters]). A new cache is created
     * when [withContainingClass] changes the containing class.
     *
     * Uses a [java.util.concurrent.ConcurrentHashMap] because FIR resolves types concurrently across members of the
     * same class, and the scope resolver (and therefore this cache) is shared across those
     * resolutions. Null results are encoded via the [FIND_LOCAL_CLASS_NULL] sentinel because
     * [java.util.concurrent.ConcurrentHashMap] does not accept null values.
     */
    private val findLocalClassCache: ConcurrentHashMap<Name, Any> = ConcurrentHashMap(),
) {

    /** Returns type parameters with HIGH priority (method/class own params, win over inner class names). */
    fun findTypeParameter(name: String): JavaTypeParameter? = typeParametersInScope[name]

    /** Returns type parameters with LOW priority (outer class inherited params, shadowed by inner class names). */
    fun findInheritedTypeParameter(name: String): JavaTypeParameter? = inheritedTypeParametersInScope[name]

    /**
     * Finds a class by simple name. Checks:
     * 1. Inner classes of the containing class (if any)
     * 2. Sibling inner classes (inner classes of the outer class)
     * 3. Inner classes of supertypes (JLS 6.5.2 - inherited member types)
     * 4. Inner classes of outer classes' supertypes (for nested inner classes)
     * 5. Top-level classes in the same compilation unit
     */
    fun findLocalClass(name: Name): JavaClass? {
        findLocalClassCache[name]?.let { return if (it === FIND_LOCAL_CLASS_NULL) null else it as JavaClass }
        val cached = findLocalClassCache.computeIfAbsent(name) {
            findLocalClassUncached(it) ?: FIND_LOCAL_CLASS_NULL
        }
        return if (cached === FIND_LOCAL_CLASS_NULL) null else cached as JavaClass
    }

    private fun findLocalClassUncached(name: Name): JavaClass? {
        val containingClass = containingClassProvider?.invoke()
        // First check inner classes of the containing class
        containingClass?.findInnerClass(name)?.let { return it }
        // Then check sibling inner classes (classes in the outer class)
        // This handles cases like: class J { class AImpl {} class A extends AImpl {} }
        containingClass?.outerClass?.findInnerClass(name)?.let { return it }
        // Then check inner classes of supertypes (inherited member types per JLS 6.5.2)
        // This handles cases like: class B extends A { ... } where A has inner class Y
        containingClass?.let { cls ->
            inheritedMemberResolver.findInnerClassFromSupertypes(name, cls, mutableSetOf())?.let { return it }
        }
        // Also check inner classes of outer classes and their supertypes.
        // Walk the full outer class chain: for deeply nested classes like
        // Outer { Inner1 { Inner2 { ... } } }, Inner2 must see siblings of Outer.
        var outer = containingClass?.outerClass
        while (outer != null) {
            outer.outerClass?.findInnerClass(name)?.let { return it }
            inheritedMemberResolver.findInnerClassFromSupertypes(name, outer, mutableSetOf())?.let { return it }
            outer = outer.outerClass
        }
        // Then check top-level classes
        return localClassProvider(name)
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
            localClassProvider, containingClassProvider, inheritedMemberResolver, newScope,
            inheritedTypeParametersInScope,
            findLocalClassCache, // share cache — containingClass unchanged
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
            localClassProvider, containingClassProvider, inheritedMemberResolver, typeParametersInScope,
            newInherited,
            findLocalClassCache, // share cache — containingClass unchanged
        )
    }

    /**
     * Creates a new scope for members of the given class.
     * Inner class references will be resolved against this class.
     */
    fun withContainingClass(containingClass: JavaClass): JavaScopeResolver {
        return JavaScopeResolver(
            localClassProvider,
            containingClassProvider = { containingClass },
            inheritedMemberResolver,
            typeParametersInScope,
            inheritedTypeParametersInScope,
            // new cache — containingClass changed, findLocalClass results may differ
        )
    }

    companion object {
        /** Sentinel for [findLocalClassCache]: "looked up, result was null". */
        private val FIND_LOCAL_CLASS_NULL = Any()
    }
}
