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
 * 1. Inner classes of the containing class — declared members ([JavaClass.findInnerClass]) plus
 *    same-file inherited member types ([findInnerClassInSameFileSupertypes], JLS 8.5).
 * 2. Sibling inner classes — declared and same-file-inherited members of the immediate outer class.
 * 3. Inherited inner classes from the containing class's supertypes
 *    ([JavaInheritedMemberResolver.findInnerClassFromSupertypes], JLS 6.5.2) — the cross-file /
 *    resolved-supertype path.
 * 4. Inner classes (declared + same-file-inherited) of each outer class up the containing chain
 *    (so deeply-nested classes see siblings of every enclosing class).
 * 5. Top-level classes declared in the same file (`sameFileTopLevelClassProvider`).
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
    val inheritedMemberResolver = c.fileContext.inheritedMemberResolver
    // 1. Inner classes of the containing class — declared members (purely syntactic AST query)
    // plus the same-file supertype walk for inherited member types ([findInnerClassInSameFileSupertypes]).
    scope.containingClass?.declaredOrSameFileInherited(name)?.let { return it }
    // 2. Sibling inner classes — declared and same-file-inherited members of the immediate outer class.
    // Handles cases like: class J { class AImpl {} class A extends AImpl {} }
    scope.containingClass?.outerClass?.declaredOrSameFileInherited(name)?.let { return it }
    // 3. Inherited inner classes from the containing class's supertypes (JLS 6.5.2).
    // Required for cross-file Java-source supertypes; see KDoc above.
    // The resolver is read from [JavaFileContext] (per-file, scope-invariant);
    // the scope data holds no reference to it.
    scope.containingClass?.let { cls ->
        inheritedMemberResolver.findInnerClassFromSupertypes(name, cls, mutableSetOf())?.let { return it }
    }
    // 4. Inner classes of each outer class up the containing chain.
    // For deeply-nested classes (Outer { Inner1 { Inner2 { ... } } }) Inner2 must
    // see siblings of every enclosing class.
    var outer = scope.containingClass?.outerClass
    while (outer != null) {
        outer.outerClass?.declaredOrSameFileInherited(name)?.let { return it }
        outer = outer.outerClass
    }
    // 5. Top-level classes declared in the same file.
    return scope.sameFileTopLevelClassProvider(name)
}

/**
 * Declared lookup first, then the same-file inherited-member-type walk. Preserves the resolution
 * order that [JavaClass.findInnerClass] used to perform internally before it was made declared-only
 * (matching the PSI / binary implementations): callers that need inherited member types in scope
 * invoke this explicitly.
 */
internal fun JavaClass.declaredOrSameFileInherited(name: Name): JavaClass? =
    findInnerClass(name)
        ?: (this as? JavaClassOverAst)?.let { findInnerClassInSameFileSupertypes(it, name, mutableSetOf()) }

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
 * (its own imports/scope), exactly as the model-side walk used to do — using the caller's ambient
 * context instead would mis-resolve names and can loop.
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
        // Declared-only probe (findInnerClass no longer walks supertypes itself).
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
 * Behaviour on the two cases the old first-segment shortcut mishandled:
 * - **Qualified-nested same-file supertype** (`class x1 extends x.S`, both top-level in this file):
 *   resolves the head `x` via [findClassInCurrentScope] and then navigates `.S` via
 *   [JavaClass.findInnerClass], yielding `x.S` — so member types inherited from `x.S` are found.
 *   The old shortcut stopped at `x` and probed the inner class on the wrong class.
 * - **Package-qualified supertype** (`extends com.example.Base`): the head `com` is not a class in
 *   scope, so navigation returns `null` and the reference is correctly *declined* by this same-file
 *   walk — it is owned by the cross-file / `ClassId` paths
 *   ([JavaInheritedMemberResolver.findInnerClassFromSupertypes], [resolve]). The old shortcut
 *   instead mistook the package root `com` for a class name.
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
