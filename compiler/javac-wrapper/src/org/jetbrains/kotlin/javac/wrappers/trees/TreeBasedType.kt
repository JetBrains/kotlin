/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.javac.wrappers.trees

import com.sun.source.util.TreePath
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.name.FqName

abstract class TreeBasedType<out T : JCTree>(val tree: T,
                                             val treePath: TreePath,
                                             val javac: JavacWrapper) : JavaType, JavaAnnotationOwner {

    companion object {
        fun <Type : JCTree> create(tree: Type, treePath: TreePath, javac: JavacWrapper) = when (tree) {
            is JCTree.JCPrimitiveTypeTree -> TreeBasedPrimitiveType(tree, TreePath(treePath, tree), javac)
            is JCTree.JCArrayTypeTree -> TreeBasedArrayType(tree, TreePath(treePath, tree), javac)
            is JCTree.JCWildcard -> TreeBasedWildcardType(tree, TreePath(treePath, tree), javac)
            is JCTree.JCTypeApply -> TreeBasedClassifierTypeWithTypeArgument(tree, TreePath(treePath, tree), javac)
            is JCTree.JCExpression -> TreeBasedClassifierTypeWithoutTypeArgument(tree, TreePath(treePath, tree), javac)
            else -> throw UnsupportedOperationException("Unsupported type: $tree")
        }
    }

    override val annotations
        get() = emptyList<TreeBasedAnnotation>()

    override val isDeprecatedInJavaDoc
        get() = false

    override fun findAnnotation(fqName: FqName) = annotations.find { it.classId?.asSingleFqName() == fqName }

    override fun equals(other: Any?) = (other as? TreeBasedType<*>)?.tree == tree

    override fun hashCode() = tree.hashCode()

    override fun toString() = tree.toString()

}