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
import javax.lang.model.type.TypeKind

abstract class TreeBasedType<out T : JCTree>(
        val tree: T,
        val treePath: TreePath,
        val javac: JavacWrapper,
        override val annotations: Collection<JavaAnnotation>
) : JavaType, JavaAnnotationOwner {

    companion object {
        fun create(tree: JCTree, treePath: TreePath,
                   javac: JavacWrapper, annotations: Collection<JavaAnnotation>): JavaType {
            val applicableAnnotations = annotations.filterTypeAnnotations()
            return when (tree) {
                is JCTree.JCPrimitiveTypeTree -> TreeBasedPrimitiveType(tree, TreePath(treePath, tree), javac, applicableAnnotations)
                is JCTree.JCArrayTypeTree -> TreeBasedArrayType(tree, TreePath(treePath, tree), javac, applicableAnnotations)
                is JCTree.JCWildcard -> TreeBasedWildcardType(tree, TreePath(treePath, tree), javac, applicableAnnotations)
                is JCTree.JCTypeApply -> TreeBasedGenericClassifierType(tree, TreePath(treePath, tree), javac, applicableAnnotations)
                is JCTree.JCAnnotatedType -> {
                    val underlyingType = tree.underlyingType
                    val newAnnotations = tree.annotations
                            .map { TreeBasedAnnotation(it, javac.getTreePath(it, treePath.compilationUnit), javac) }
                    create(underlyingType, javac.getTreePath(underlyingType, treePath.compilationUnit), javac, newAnnotations)
                }
                is JCTree.JCExpression -> TreeBasedNonGenericClassifierType(tree, TreePath(treePath, tree), javac, applicableAnnotations)
                else -> throw UnsupportedOperationException("Unsupported type: $tree")
            }
        }
    }

    override val isDeprecatedInJavaDoc: Boolean
        get() = false

    override fun findAnnotation(fqName: FqName) = annotations.find { it.classId?.asSingleFqName() == fqName }

    override fun equals(other: Any?) = (other as? TreeBasedType<*>)?.tree == tree

    override fun hashCode() = tree.hashCode()

    override fun toString() = tree.toString()

}

class TreeBasedPrimitiveType(
        tree: JCTree.JCPrimitiveTypeTree,
        treePath: TreePath,
        javac: JavacWrapper,
        annotations: Collection<JavaAnnotation>
) : TreeBasedType<JCTree.JCPrimitiveTypeTree>(tree, treePath, javac, annotations), JavaPrimitiveType {

    override val type: PrimitiveType?
        get() = if (tree.primitiveTypeKind == TypeKind.VOID) {
            null
        }
        else {
            JvmPrimitiveType.get(tree.toString()).primitiveType
        }

}

class TreeBasedArrayType(
        tree: JCTree.JCArrayTypeTree,
        treePath: TreePath,
        javac: JavacWrapper,
        annotations: Collection<JavaAnnotation>
) : TreeBasedType<JCTree.JCArrayTypeTree>(tree, treePath, javac, annotations), JavaArrayType {

    override val componentType: JavaType
        get() = create(tree.elemtype, treePath, javac, annotations)

}

class TreeBasedWildcardType(
        tree: JCTree.JCWildcard,
        treePath: TreePath,
        javac: JavacWrapper,
        annotations: Collection<JavaAnnotation>
) : TreeBasedType<JCTree.JCWildcard>(tree, treePath, javac, annotations), JavaWildcardType {

    override val bound: JavaType?
        get() = tree.bound?.let { create(it, treePath, javac, annotations) }

    override val isExtends: Boolean
        get() = tree.kind.kind == BoundKind.EXTENDS

}

sealed class TreeBasedClassifierType<out T : JCTree>(
        tree: T,
        treePath: TreePath,
        javac: JavacWrapper,
        annotations: Collection<JavaAnnotation>
) : TreeBasedType<T>(tree, treePath, javac, annotations), JavaClassifierType {

    override val classifier: JavaClassifier?
        get() = javac.resolve(treePath)

    override val classifierQualifiedName: String
        get() = (classifier as? JavaClass)?.fqName?.asString() ?: treePath.leaf.toString().substringBefore("<")

    override val presentableText: String
        get() = classifierQualifiedName

    override val typeArguments: List<JavaType>
        get() {
            val classifier = classifier as? JavaClass ?: return emptyList()
            if (classifier is MockKotlinClassifier || classifier.isStatic) return emptyList()

            return arrayListOf<JavaClass>().apply {
                var outer = classifier.outerClass
                while (outer != null && !outer.isStatic) {
                    add(outer)
                    outer = outer.outerClass
                }
            }.flatMap { it.typeParameters.map(::TreeBasedTypeParameterType) }
        }

    private val typeParameter: JCTree.JCTypeParameter?
        get() = treePath.flatMap {
                    when (it) {
                        is JCTree.JCClassDecl -> it.typarams
                        is JCTree.JCMethodDecl -> it.typarams
                        else -> emptyList<JCTree.JCTypeParameter>()
                    }
                }
                .find { it.toString().substringBefore(" ") == treePath.leaf.toString() }

}

class TreeBasedTypeParameterType(override val classifier: JavaTypeParameter) : JavaClassifierType {

    override val typeArguments: List<JavaType>
        get() = emptyList()

    override val isRaw: Boolean
        get() = false

    override val annotations: Collection<JavaAnnotation>
        get() = classifier.annotations.filterTypeAnnotations()

    override val classifierQualifiedName: String
        get() = classifier.name.asString()

    override val presentableText: String
        get() = classifierQualifiedName

    override fun findAnnotation(fqName: FqName) = annotations.find { it.classId?.asSingleFqName() == fqName }

    override val isDeprecatedInJavaDoc: Boolean
        get() = false
}

class TreeBasedNonGenericClassifierType(
        tree: JCTree.JCExpression,
        treePath: TreePath,
        javac: JavacWrapper,
        annotations: Collection<JavaAnnotation>
) : TreeBasedClassifierType<JCTree.JCExpression>(tree, treePath, javac, annotations) {

    override val isRaw: Boolean
        get() = (classifier as? MockKotlinClassifier)?.hasTypeParameters
                ?: (classifier as? JavaClass)?.typeParameters?.isNotEmpty()
                ?: false

}

class TreeBasedGenericClassifierType(
        tree: JCTree.JCTypeApply,
        treePath: TreePath,
        javac: JavacWrapper,
        annotations: Collection<JavaAnnotation>
) : TreeBasedClassifierType<JCTree.JCTypeApply>(tree, treePath, javac, annotations) {

    override val classifier: JavaClassifier?
        get() {
            val newTree = tree.clazz
            return if (newTree is JCTree.JCAnnotatedType) {
                javac.resolve(javac.getTreePath(newTree.underlyingType, treePath.compilationUnit))
            }
            else super.classifier
        }

    override val annotations: Collection<JavaAnnotation>
        get() {
            val newTree = tree.clazz
            return (newTree as? JCTree.JCAnnotatedType)?.annotations?.map { TreeBasedAnnotation(it, javac.getTreePath(it, treePath.compilationUnit), javac) }
                           ?.toMutableList<JavaAnnotation>()
                           ?.apply { addAll(super.annotations) }
                   ?: super.annotations
        }

    override val typeArguments: List<JavaType>
        get() = tree.arguments.map { create(it, treePath, javac, emptyList()) }
                .toMutableList()
                .apply { addAll(super.typeArguments) }

    override val isRaw: Boolean
        get() = classifier.let {
            when (it) {
                is MockKotlinClassifier -> tree.arguments.size != it.typeParametersNumber
                else -> tree.arguments.size != (classifier as? JavaClass)?.typeParameters?.size
            }
        }

}