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
 * Stateless type-parameter scoping and current-scope class lookup for Java source resolution,
 * operating upon the given [JavaResolutionContext].
 *
 * Responsible for:
 * - Type parameter lookup (own high-priority params and inherited low-priority params)
 * - Current scope class resolution (inner classes, sibling classes, supertype-inherited classes,
 *   top-level classes)
 *
 * This is the scope-side counterpart of the simple/qualified-name dispatcher in [JavaTypeResolver].
 */

/** Returns type parameters with HIGH priority (method/class own params, win over inner class names). */
context(c: JavaResolutionContext)
internal fun findTypeParameter(name: String): JavaTypeParameter? =
    c.scopeContext.typeParametersInScope[name]

/** Returns type parameters with LOW priority (outer class inherited params, shadowed by inner class names). */
context(c: JavaResolutionContext)
internal fun findInheritedTypeParameter(name: String): JavaTypeParameter? =
    c.scopeContext.inheritedTypeParametersInScope[name]

/**
 * Finds a [JavaClass] for a simple name in the AST-side scope.
 *
 * Checks (in order):
 * 1. Declared-plus-fully-inherited member types ([declaredOrFullyInherited]) at every level of
 *    the containing-class chain, innermost to outermost — the containing class itself, then its
 *    outer class, then that class's outer class, and so on. Per JLS 6.4.1, a member type declared
 *    or inherited at an inner level shadows one declared or inherited at an enclosing level, so
 *    this single loop (rather than a special-cased innermost level) preserves that shadowing for
 *    every representation the inherited half of the lookup reaches: same-file supertypes,
 *    cross-file Java source, binary Java, and Kotlin.
 * 2. Top-level classes declared in the same file (`sameFileTopLevelClassProvider`).
 *
 *  - [org.jetbrains.kotlin.java.direct.model.JavaTypeOverAst]'s `computeClassifier` reads the
 *    result as a [org.jetbrains.kotlin.load.java.structure.JavaClassifier], which must be a
 *    structural [JavaClass] (with its full AST-side outer-class chain) for multi-part name
 *    navigation via [JavaClass.findInnerClass] and for outer-class type-argument substitution to
 *    flow through Java-source supertype chains.
 *  - [org.jetbrains.kotlin.java.direct.JavaClassCache] / [org.jetbrains.kotlin.java.direct.util.ConstantEvaluator]
 *    also need the AST [JavaClass] to materialise inner-class symbols and constant references.
 */
context(c: JavaResolutionContext)
internal fun findClassInCurrentScope(name: Name): JavaClass? {
    val scope = c.scopeContext
    // 1. Declared-plus-fully-inherited lookup at every level of the containing-class chain.
    var current = scope.containingClass
    while (current != null) {
        declaredOrFullyInherited(current, name)?.let { return it }
        current = current.outerClass
    }
    // 2. Top-level classes declared in the same file.
    return scope.sameFileTopLevelClassProvider(name)
}

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
 * inherited member types ([JavaInheritedMemberResolver.findInnerClassFromSupertypes]) — the full
 * declared-plus-inherited lookup for one level of a containing-class chain.
 *
 * Used by [findClassInCurrentScope] at every level of the chain, and by
 * [org.jetbrains.kotlin.java.direct.model.JavaTypeOverAst]'s multi-part navigation loop for the
 * same reason: an intermediate segment of a qualified reference can be inherited from any of
 * those representations, not just a same-file supertype.
 */
context(c: JavaResolutionContext)
internal fun declaredOrFullyInherited(cls: JavaClass, name: Name): JavaClass? {
    cls.declaredOrSameFileInherited(name)?.let { return it }
    val astClass = cls as? JavaClassOverAst ?: return null
    return c.fileContext.inheritedMemberResolver.findInnerClassFromSupertypes(
        name, astClass, mutableSetOf(),
        resolveBinaryOrKotlinInherited = { containingClass, innerName ->
            resolveInheritedInnerClassToClassId(innerName.asString(), { tryResolve(it) }, containingClass, includeOuterClasses = false)
        },
        classifierAdapterFor = { classifierAdapterFor(it) },
    )
}

/**
 * Searches for an inner class in the supertypes of [cls], working purely on raw AST text
 * ([JavaClassOverAst.directSupertypeRefNames]).
 *
 * This is intentionally distinct from [JavaInheritedMemberResolver.findInnerClassFromSupertypes]:
 *
 * | Aspect            | This function (same-file AST walk)                     | `JavaInheritedMemberResolver`                          |
 * |-------------------|--------------------------------------------------------|--------------------------------------------------------|
 * | Input             | Raw `EXTENDS_LIST` / `IMPLEMENTS_LIST` AST text        | Resolved `javaClass.supertypes` (full `JavaClassifierType`) |
 * | Resolution depth  | Reference navigation via [resolveSameFileSupertypeRefToClass] | Full classifier resolution + cross-file ambiguity check |
 * | Recursion guard   | `visited: MutableSet<String>` of FQN strings           | `visited: MutableSet<JavaClass>` of model instances    |
 *
 * The two paths cannot be unified because **this walk must avoid triggering full type
 * resolution** — reading `javaClass.supertypes` here would re-enter type construction, which
 * itself calls `classifier → findLocalClass → findInnerClass`, producing infinite recursion.
 * Conversely, the inherited-member resolver requires resolved supertypes to detect cross-file
 * ambiguities that simple-name AST scanning cannot see.
 *
 * Each supertype reference is resolved within the walked class's *own* [JavaClassOverAst.resolutionContext]
 * (its own imports/scope); using the caller's ambient context instead would mis-resolve names and
 * can loop.
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
 * Resolves a raw same-file supertype reference — a [JavaClassOverAst.directSupertypeRefNames]
 * entry such as `S`, `x.S`, or `com.example.Base` — to the [JavaClassOverAst] it denotes within the
 * current (same-file) scope, navigating every dotted segment through the module's own reference
 * resolution rather than guessing with `substringBefore('.')`.
 *
 * Behaviour on the two tricky cases:
 * - **Qualified-nested same-file supertype** (`class x1 extends x.S`, both top-level in this file):
 *   resolves the head `x` via [findClassInCurrentScope] and then navigates `.S` via
 *   [JavaClass.findInnerClass], yielding `x.S` — so member types inherited from `x.S` are found.
 * - **Package-qualified supertype** (`extends com.example.Base`): the head `com` is not a class in
 *   scope, so navigation returns `null` and the reference is correctly *declined* by this same-file
 *   walk — it is owned by the cross-file / `ClassId` paths
 *   ([JavaInheritedMemberResolver.findInnerClassFromSupertypes], [resolve]).
 *
 * The tail segments are navigated with the declared-only [JavaClass.findInnerClass] (a written
 * `x.S` names a concrete declared nested type), which also keeps this resolution from re-entering
 * the supertype walk and preserves the recursion bound carried by the caller's `visited` set.
 */
context(c: JavaResolutionContext)
private fun resolveSameFileSupertypeRefToClass(supertypeRef: String): JavaClassOverAst? {
    val parts = supertypeRef.split('.')
    var current = findClassInCurrentScope(Name.identifier(parts[0])) as? JavaClassOverAst ?: return null
    for (i in 1 until parts.size) {
        current = current.findInnerClass(Name.identifier(parts[i])) as? JavaClassOverAst ?: return null
    }
    return current
}
