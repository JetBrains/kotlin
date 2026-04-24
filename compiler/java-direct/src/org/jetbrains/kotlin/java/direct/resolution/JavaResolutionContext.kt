/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.util.findTopLevelClassNode
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

/** Per-file immutable data shared across all scope variants of a [JavaResolutionContext]. */
internal class JavaFileContext(
    val packageFqName: FqName,
    val imports: JavaImports,
    val classFinder: LeanJavaClassFinder?,
    val session: FirSession,
)

/**
 * Positional **data** for resolving type references within a Java file: an immutable pair of
 * [fileContext] (per-file: package, imports, class finder, session) and [scopeContext]
 * (per-position: containing class, type parameters in scope). Scope transitions fork a new
 * record.
 */
class JavaResolutionContext private constructor(
    internal val fileContext: JavaFileContext,
    internal val scopeContext: JavaScopeContext,
) {
    val packageFqName: FqName get() = fileContext.packageFqName

    /** See [JavaScopeContext.withTypeParameters]. */
    fun withTypeParameters(typeParams: List<JavaTypeParameter>): JavaResolutionContext {
        if (typeParams.isEmpty()) return this
        return JavaResolutionContext(fileContext, scopeContext.withTypeParameters(typeParams))
    }

    /** See [JavaScopeContext.withInheritedTypeParameters]. */
    fun withInheritedTypeParameters(typeParams: List<JavaTypeParameter>): JavaResolutionContext {
        if (typeParams.isEmpty()) return this
        return JavaResolutionContext(fileContext, scopeContext.withInheritedTypeParameters(typeParams))
    }

    /** See [JavaScopeContext.withContainingClass]. */
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

            // Same-file top-level classes are indexed lazily to avoid circular initialization.
            // computeIfAbsent guarantees a single JavaClassOverAst instance per top-level class
            // under concurrent FIR resolution — FIR matches type parameters by object identity.
            var contextRef: JavaResolutionContext? = null
            val sameFileTopLevelClassCache = ConcurrentHashMap<Name, JavaClass>()

            val sameFileTopLevelClassProvider: (Name) -> JavaClass? = { name ->
                sameFileTopLevelClassCache[name] ?: findTopLevelClassNode(tree, root, name)?.let { classNode ->
                    sameFileTopLevelClassCache.computeIfAbsent(name) {
                        JavaClassOverAst(classNode, tree, contextRef!!, outerClass = null)
                    }
                }
            }

            val scopeContext = JavaScopeContext(
                sameFileTopLevelClassProvider,
                containingClass = null,
            )

            val fileContext = JavaFileContext(
                packageFqName, imports,
                classFinder,
                session = session,
            )
            return JavaResolutionContext(fileContext, scopeContext).also {
                contextRef = it
            }
        }
    }
}
