/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class JavaAnnotationOverAst(
    node: JavaSyntaxNode,
    private val resolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node), JavaAnnotation {
    override val arguments: Collection<JavaAnnotationArgument>
        get() {
            val parameterList = node.findChildByType("ANNOTATION_PARAMETER_LIST")
            if (parameterList == null) return emptyList()
            
            return parameterList.getChildrenByType("NAME_VALUE_PAIR").map { 
                JavaAnnotationArgumentOverAst(it)
            }
        }

    /**
     * The simple or qualified name of the annotation as it appears in source.
     * For `@Deprecated`, returns "Deprecated".
     * For `@java.lang.Deprecated`, returns "java.lang.Deprecated".
     */
    private val annotationName: String?
        get() = node.findChildByType("JAVA_CODE_REFERENCE")?.text

    override val classId: ClassId?
        get() {
            val reference = annotationName ?: return null
            
            // If already qualified (contains dot), use as-is
            if (reference.contains('.')) {
                return ClassId.topLevel(FqName(reference))
            }
            
            // Try to resolve via explicit imports
            val imported = resolutionContext.getSimpleImport(reference)
            if (imported != null) {
                return ClassId.topLevel(imported)
            }
            
            // Return unqualified - FIR will need to resolve via resolveAnnotation
            return ClassId.topLevel(FqName(reference))
        }

    /**
     * Whether this annotation is already resolved.
     * Returns false when the annotation name is unqualified and not explicitly imported.
     */
    override val isResolved: Boolean
        get() {
            val reference = annotationName ?: return true
            // Resolved if fully qualified or explicitly imported
            return reference.contains('.') || resolutionContext.getSimpleImport(reference) != null
        }

    /**
     * Resolves this annotation's class using the provided callback.
     * Uses the same resolution logic as types: same package, java.lang, star imports.
     */
    override fun resolveAnnotation(tryResolve: (String) -> Boolean): String? {
        val reference = annotationName ?: return null
        
        // If already qualified, return as-is (but verify it exists)
        if (reference.contains('.')) {
            return if (tryResolve(reference)) reference else null
        }
        
        // Try to resolve via explicit imports
        val imported = resolutionContext.getSimpleImport(reference)
        if (imported != null) {
            return imported.asString()
        }
        
        // Use the same resolution logic as types: same package, java.lang, star imports
        return resolutionContext.resolveWithCallback(reference, tryResolve)
    }

    override fun resolve(): JavaClass? = null
}

class JavaAnnotationArgumentOverAst(
    node: JavaSyntaxNode
) : JavaElementOverAst(node), JavaAnnotationArgument {
    override val name: Name?
        get() = node.findChildByType("IDENTIFIER")?.let { Name.identifier(it.text) }
}
