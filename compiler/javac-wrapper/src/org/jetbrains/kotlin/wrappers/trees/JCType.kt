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

package org.jetbrains.kotlin.wrappers.trees

import com.sun.source.util.TreePath
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.Javac
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.name.FqName

abstract class JCType<out T : JCTree>(val tree: T,
                                      val treePath: TreePath,
                                      val javac: Javac) : JavaType, JavaAnnotationOwner {

    companion object {
        fun <Type : JCTree> create(tree: Type, treePath: TreePath, javac: Javac) = when (tree) {
            is JCTree.JCPrimitiveTypeTree -> JCPrimitiveType(tree, TreePath(treePath, tree), javac)
            is JCTree.JCArrayTypeTree -> JCArrayType(tree, TreePath(treePath, tree), javac)
            is JCTree.JCWildcard -> JCWildcardType(tree, TreePath(treePath, tree), javac)
            is JCTree.JCTypeApply -> JCClassifierTypeWithTypeArgument(tree, TreePath(treePath, tree), javac)
            is JCTree.JCExpression -> JCClassifierType(tree, TreePath(treePath, tree), javac)
            else -> throw UnsupportedOperationException("Unsupported type: $tree")
        }
    }

    override val annotations
        get() = emptyList<JavaAnnotation>()

    override val isDeprecatedInJavaDoc
        get() = false

    override fun findAnnotation(fqName: FqName) = null

    override fun equals(other: Any?) = (other as? JCType<*>)?.tree == tree

    override fun hashCode() = tree.hashCode()

    override fun toString() = tree.toString()

}