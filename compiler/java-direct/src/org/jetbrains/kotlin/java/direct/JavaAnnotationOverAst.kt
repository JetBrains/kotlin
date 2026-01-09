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
    source: CharSequence
) : JavaElementOverAst(node, source), JavaAnnotation {
    override val arguments: Collection<JavaAnnotationArgument>
        get() {
            val parameterList = node.findChildByType("ANNOTATION_PARAMETER_LIST")
            if (parameterList == null) return emptyList()
            
            return parameterList.getChildrenByType("NAME_VALUE_PAIR").map { 
                JavaAnnotationArgumentOverAst(it, source)
            }
        }

    override val classId: ClassId?
        get() {
            val reference = node.findChildByType("JAVA_CODE_REFERENCE")?.text ?: return null
            return ClassId.topLevel(FqName(reference))
        }

    override fun resolve(): JavaClass? = null
}

class JavaAnnotationArgumentOverAst(
    node: JavaSyntaxNode,
    source: CharSequence
) : JavaElementOverAst(node, source), JavaAnnotationArgument {
    override val name: Name?
        get() = node.findChildByType("IDENTIFIER")?.let { Name.identifier(it.text) }

    // This is a simplified implementation of argument parsing
    fun getArgumentValue(): Any? = null
}
