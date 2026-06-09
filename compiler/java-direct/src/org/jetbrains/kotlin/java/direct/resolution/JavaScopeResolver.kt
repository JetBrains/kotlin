/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

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
 * 1. Inner classes directly declared on the containing class (purely syntactic AST query).
 * 2. Sibling inner classes — inner classes declared on the immediate outer class.
 * 3. Inherited inner classes from the containing class's supertypes
 *    ([JavaInheritedMemberResolver.findInnerClassFromSupertypes], JLS 6.5.2).
 * 4. Inner classes declared on each outer class up the containing chain
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
    // 1. Inner classes of the containing class (purely syntactic AST query, plus
    // [JavaClassOverAst.findInnerClassInSupertypes] same-file supertype walk).
    scope.containingClass?.findInnerClass(name)?.let { return it }
    // 2. Sibling inner classes — inner classes of the immediate outer class.
    // Handles cases like: class J { class AImpl {} class A extends AImpl {} }
    scope.containingClass?.outerClass?.findInnerClass(name)?.let { return it }
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
        outer.outerClass?.findInnerClass(name)?.let { return it }
        outer = outer.outerClass
    }
    // 5. Top-level classes declared in the same file.
    return scope.sameFileTopLevelClassProvider(name)
}
