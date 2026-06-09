/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.util.findTopLevelClassNode
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

/**
 * Positional **data** for resolving type references within a Java file.
 *
 * Its data is two immutable records:
 *  - [fileContext] — per-file data (package, imports, class finder, session, cycle
 *    checker, inherited-member resolver), shared across every scope variant.
 *  - [scopeContext] — per-position data (containing class, type parameters in scope, same-file
 *    top-level class provider, inherited-inner cache).
 *
 * Scope transitions ([withTypeParameters] / [withInheritedTypeParameters] / [withContainingClass])
 * fork a new record.
 *
 * Collaborators:
 * - [JavaImportResolver] — import extraction and package name parsing (stateless).
 * - [JavaTypeResolver] — the JLS 6.4.1 / 6.5.2 type-name dispatcher and session probes.
 * - [JavaScopeResolver] — type-parameter scoping and AST current-scope class lookup.
 * - [JavaInheritedMemberResolver] — supertype hierarchy traversal for inner classes.
 */
class JavaResolutionContext private constructor(
    internal val fileContext: JavaFileContext,
    internal val scopeContext: JavaScopeContext,
) {
    val packageFqName: FqName get() = fileContext.packageFqName

    /**
     * Creates a new context with additional OWN type parameters (high priority).
     * Used when entering a class or method that declares type parameters.
     * Own type params take priority over inner class names of the containing class.
     */
    fun withTypeParameters(typeParams: List<JavaTypeParameter>): JavaResolutionContext {
        if (typeParams.isEmpty()) return this
        return JavaResolutionContext(fileContext, scopeContext.withTypeParameters(typeParams))
    }

    /**
     * Creates a new context with INHERITED type parameters from an outer class (low priority).
     * Used for static nested types where outer class type params are visible but can be
     * shadowed by inner class names of the static nested type itself.
     */
    fun withInheritedTypeParameters(typeParams: List<JavaTypeParameter>): JavaResolutionContext {
        if (typeParams.isEmpty()) return this
        return JavaResolutionContext(fileContext, scopeContext.withInheritedTypeParameters(typeParams))
    }

    /**
     * Creates a new context for members of the given class.
     * Inner class references will be resolved against this class.
     */
    fun withContainingClass(newContainingClass: JavaClass): JavaResolutionContext {
        return JavaResolutionContext(fileContext, scopeContext.withContainingClass(newContainingClass))
    }

    companion object {
        internal fun create(
            tree: JavaLightTree,
            session: FirSession,
            classFinder: LeanJavaClassFinder? = null,
        ): JavaResolutionContext {
            val root = tree.getRoot()
            val packageFqName = JavaImportResolver.extractPackageName(tree, root)
            val imports = JavaImportResolver.extractImports(tree, root)

            // Same-file top-level classes indexed lazily to avoid circular initialization.
            // ConcurrentHashMap + computeIfAbsent so that concurrent FIR resolution of
            // different members in the same file does not race on cache updates (and, critically,
            // does not produce two distinct JavaClassOverAst instances for the same top-level
            // class — FIR matches type parameters by object identity.
            var contextRef: JavaResolutionContext? = null
            val sameFileTopLevelClassCache = ConcurrentHashMap<Name, JavaClass>()

            val sameFileTopLevelClassProvider: (Name) -> JavaClass? = { name ->
                sameFileTopLevelClassCache[name] ?: findTopLevelClassNode(tree, root, name)?.let { classNode ->
                    sameFileTopLevelClassCache.computeIfAbsent(name) {
                        JavaClassOverAst(classNode, tree, contextRef!!, outerClass = null)
                    }
                }
            }

            val inheritedMemberResolver = JavaInheritedMemberResolver(
                packageFqName, classFinder, sameFileTopLevelClassProvider,
            )
            val scopeContext = JavaScopeContext(
                sameFileTopLevelClassProvider,
                containingClass = null,
            )

            val fileContext = JavaFileContext(
                packageFqName, imports,
                inheritedMemberResolver, classFinder,
                session = session,
            )
            return JavaResolutionContext(fileContext, scopeContext).also {
                contextRef = it
            }
        }
    }
}
