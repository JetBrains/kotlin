/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.Name

/**
 * Stateless type-parameter scoping and current-scope class lookup for Java source resolution —
 * the scope-side counterpart of the name dispatchers in [JavaTypeResolver].
 */

/** Returns type parameters with HIGH priority (method/class own params, win over inner class names). */
context(c: JavaResolutionContext)
internal fun findTypeParameter(name: String): JavaTypeParameter? =
    c.scopeContext.typeParametersInScope[name]

/**
 * Returns type parameters with LOW priority (outer class inherited params, shadowed by inner class
 * names). The sole reader of [JavaScopeContext.inheritedTypeParametersInScope]; exists only for the
 * PSI-parity quirk documented there and is removable with it.
 * TODO: remove (KT-87797)
 */
context(c: JavaResolutionContext)
internal fun findInheritedTypeParameter(name: String): JavaTypeParameter? =
    c.scopeContext.inheritedTypeParametersInScope[name]

/**
 * Finds a [JavaClass] for a simple name in the AST-side scope: declared-plus-inherited member
 * types at every level of the containing-class chain (innermost to outermost — per JLS 6.4.1 an
 * inner level shadows an enclosing one), then same-file top-level classes.
 *
 * Returns a structural [JavaClass] (not a bare `ClassId`) because callers need its AST-side
 * outer-class chain for further navigation and outer-class type-argument substitution.
 */
context(c: JavaResolutionContext)
internal fun findClassInCurrentScope(name: Name): JavaClass? {
    val scope = c.scopeContext
    var current = scope.containingClass
    while (current != null) {
        declaredOrFullyInherited(current, name)?.let { return it }
        current = current.outerClass
    }
    return scope.sameFileTopLevelClassProvider(name)
}

context(c: JavaResolutionContext)
internal fun findClassInCurrentScope(name: String): JavaClass? = findClassInCurrentScope(Name.identifier(name))

/**
 * Declared lookup first, then the same-file inherited-member-type walk. [JavaClass.findInnerClass]
 * is declared-only (matching the PSI / binary implementations); callers that need inherited member
 * types in scope invoke this explicitly.
 */
internal fun JavaClass.declaredOrSameFileInherited(name: Name): JavaClass? =
    findInnerClass(name)
        ?: (this as? JavaClassOverAst)?.let { findInnerClassInSameFileSupertypes(it, name, mutableSetOf()) }

/**
 * [declaredOrSameFileInherited] extended with cross-file Java source, binary Java, and Kotlin
 * inherited member types — the full declared-plus-inherited lookup for one level of a
 * containing-class chain.
 */
context(c: JavaResolutionContext)
internal fun declaredOrFullyInherited(cls: JavaClass, name: Name): JavaClass? {
    cls.declaredOrSameFileInherited(name)?.let { return it }
    val astClass = cls as? JavaClassOverAst ?: return null
    return findInnerClassFromSupertypes(name, astClass)
}

/**
 * Searches for an inner class in the supertypes of [cls], working purely on raw AST text
 * ([JavaClassOverAst.directSupertypeRefNames]) — reading `javaClass.supertypes` here would
 * re-enter type construction and recurse infinitely. [findInnerClassFromSupertypes], in turn,
 * needs resolved supertypes to detect cross-file ambiguities this raw-text walk cannot see.
 *
 * Each supertype reference is resolved within the walked class's *own*
 * [JavaClassOverAst.resolutionContext] — the caller's ambient context would mis-resolve names
 * and can loop.
 */
internal fun findInnerClassInSameFileSupertypes(
    cls: JavaClassOverAst,
    name: Name,
    visited: MutableSet<String>,
): JavaClass? {
    if (!visited.add(cls.fqName.asString())) return null
    for (supertypeRef in cls.directSupertypeRefNames) {
        val supertypeClass = with(cls.resolutionContext) {
            resolveSameFileSupertypeRefToClass(supertypeRef)
        } ?: continue
        // Declared-only probe; supertype inheritance is handled by the recursive call below.
        supertypeClass.findInnerClass(name)?.let { return it }
        findInnerClassInSameFileSupertypes(supertypeClass, name, visited)?.let { return it }
    }
    return null
}

/**
 * Resolves a raw same-file supertype reference (`S`, `x.S`, …) to the [JavaClassOverAst] it
 * denotes: the head segment via [findClassInCurrentScope], the tail via the declared-only
 * [JavaClass.findInnerClass] (which cannot re-enter the supertype walk). A package-qualified
 * reference (`extends com.example.Base`) has a head that is not a class in scope, so it returns
 * `null` here and is picked up by the cross-file / `ClassId` paths instead.
 */
context(c: JavaResolutionContext)
private fun resolveSameFileSupertypeRefToClass(supertypeRef: String): JavaClassOverAst? {
    val parts = supertypeRef.split('.')
    var current = findClassInCurrentScope(parts[0]) as? JavaClassOverAst ?: return null
    for (i in 1 until parts.size) {
        current = current.findInnerClass(Name.identifier(parts[i])) as? JavaClassOverAst ?: return null
    }
    return current
}
