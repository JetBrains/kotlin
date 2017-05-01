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
import com.sun.tools.javac.code.BoundKind
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.javac.MockKotlinClassifier
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

abstract class TreeBasedType<out T : JCTree>(val tree: T,
                                             val treePath: TreePath,
                                             val javac: JavacWrapper) : JavaType, JavaAnnotationOwner {

    companion object {
        fun <Type : JCTree> create(tree: Type, treePath: TreePath, javac: JavacWrapper) = when (tree) {
            is JCTree.JCPrimitiveTypeTree -> TreeBasedPrimitiveType(tree, TreePath(treePath, tree), javac)
            is JCTree.JCArrayTypeTree -> TreeBasedArrayType(tree, TreePath(treePath, tree), javac)
            is JCTree.JCWildcard -> TreeBasedWildcardType(tree, TreePath(treePath, tree), javac)
            is JCTree.JCTypeApply -> TreeBasedGenericClassifierType(tree, TreePath(treePath, tree), javac)
            is JCTree.JCExpression -> TreeBasedNonGenericClassifierType(tree, TreePath(treePath, tree), javac)
            else -> throw UnsupportedOperationException("Unsupported type: $tree")
        }
    }

    override val annotations: Collection<JavaAnnotation>
        get() = emptyList()

    override val isDeprecatedInJavaDoc: Boolean
        get() = false

    override fun findAnnotation(fqName: FqName) = annotations.find { it.classId?.asSingleFqName() == fqName }

    override fun equals(other: Any?) = (other as? TreeBasedType<*>)?.tree == tree

    override fun hashCode() = tree.hashCode()

    override fun toString() = tree.toString()

}

class TreeBasedPrimitiveType<out T : JCTree.JCPrimitiveTypeTree>(tree: T,
                                                                 treePath: TreePath,
                                                                 javac: JavacWrapper) : TreeBasedType<T>(tree, treePath, javac), JavaPrimitiveType {

    override val type: PrimitiveType?
        get() = if ("void" == tree.toString()) null else JvmPrimitiveType.get(tree.toString()).primitiveType

}

class TreeBasedArrayType<out T : JCTree.JCArrayTypeTree>(tree: T,
                                                         treePath: TreePath,
                                                         javac: JavacWrapper) : TreeBasedType<T>(tree, treePath, javac), JavaArrayType {

    override val componentType: JavaType
        get() = create(tree.elemtype, treePath, javac)

}

class TreeBasedWildcardType<out T : JCTree.JCWildcard>(tree: T,
                                                       treePath: TreePath,
                                                       javac: JavacWrapper) : TreeBasedType<T>(tree, treePath, javac), JavaWildcardType {

    override val bound: JavaType?
        get() = tree.bound?.let { create(it, treePath, javac) }

    override val isExtends: Boolean
        get() = tree.kind.kind == BoundKind.EXTENDS

}

sealed class TreeBasedClassifierType<out T : JCTree>(tree: T,
                                                     treePath: TreePath,
                                                     javac: JavacWrapper) : TreeBasedType<T>(tree, treePath, javac), JavaClassifierType {

    override val classifier: JavaClassifier?
        get() = javac.resolve(treePath)

    override val classifierQualifiedName: String
        get() = (classifier as? JavaClass)?.fqName?.asString() ?: treePath.leaf.toString()

    override val presentableText: String
        get() = classifierQualifiedName

    private val typeParameter: JCTree.JCTypeParameter?
        get() = treePath
                .flatMap {
                    when (it) {
                        is JCTree.JCClassDecl -> it.typarams
                        is JCTree.JCMethodDecl -> it.typarams
                        else -> emptyList<JCTree.JCTypeParameter>()
                    }
                }
                .find { it.toString().substringBefore(" ") == treePath.leaf.toString() }

}

class TreeBasedNonGenericClassifierType<out T : JCTree.JCExpression>(tree: T,
                                                                     treePath: TreePath,
                                                                     javac: JavacWrapper) : TreeBasedClassifierType<T>(tree, treePath, javac) {

    override val typeArguments: List<JavaType>
        get() = emptyList()

    override val isRaw: Boolean
        get() = (classifier as? MockKotlinClassifier)?.hasTypeParameters
                ?: (classifier as? JavaClass)?.typeParameters?.isNotEmpty()
                ?: false

}

class TreeBasedGenericClassifierType<out T : JCTree.JCTypeApply>(tree: T,
                                                                 treePath: TreePath,
                                                                 javac: JavacWrapper) : TreeBasedClassifierType<T>(tree, treePath, javac) {

    override val typeArguments: List<JavaType>
        get() = tree.arguments.map { create(it, treePath, javac) }

    override val isRaw: Boolean
        get() = false

}