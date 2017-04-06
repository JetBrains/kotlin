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
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName

abstract class ClassifierType<out T : JCTree>(tree: T,
                                              treePath: TreePath,
                                              javac: JavacWrapper) : JCType<T>(tree, treePath, javac), JavaClassifierType {
    override val classifier by lazy { getClassifier(treePath, javac) }

    override val canonicalText
        get() = (classifier as? JavaClass)?.fqName?.asString() ?: treePath.leaf.toString()

    override val presentableText
        get() = canonicalText

    private val typeParameter
        get() = treePath.filter { it is JCTree.JCClassDecl || it is JCTree.JCMethodDecl }
                .flatMap {
                    when (it) {
                        is JCTree.JCClassDecl -> it.typarams
                        is JCTree.JCMethodDecl -> it.typarams
                        else -> emptyList<JCTypeParameter<*>>()
                    }
                }
                .find { it.toString().substringBefore(" ") == treePath.leaf.toString() }

}

class JCClassifierType<out T : JCTree.JCExpression>(tree: T,
                                                    treePath: TreePath,
                                                    javac: JavacWrapper) : ClassifierType<T>(tree, treePath, javac) {

    override val typeArguments: List<JavaType>
        get() = emptyList()

    override val isRaw
        get() = (classifier as? JavaClass)?.typeParameters?.isNotEmpty() ?: false

}

class JCClassifierTypeWithTypeArgument<out T : JCTree.JCTypeApply>(tree: T,
                                                                   treePath: TreePath,
                                                                   javac: JavacWrapper) : ClassifierType<T>(tree, treePath, javac) {

    override val typeArguments
        get() = tree.arguments.map { create(it, treePath, javac) }

    override val isRaw
        get() = false

}

private fun getClassifier(treePath: TreePath, javac: JavacWrapper) = treePath.resolve(javac).let {
    it.second
    ?: stubs[it.first]
    ?: typeParameter(treePath, javac)
    ?: createStubClassifier(it.first)
}

private fun typeParameter(treePath: TreePath, javac: JavacWrapper) = treePath
        .filter { it is JCTree.JCClassDecl || it is JCTree.JCMethodDecl }
        .flatMap {
            when (it) {
                is JCTree.JCClassDecl -> it.typarams
                is JCTree.JCMethodDecl -> it.typarams
                else -> emptyList<JCTree.JCTypeParameter>()
            }
        }
        .find { it.toString().substringBefore(" ") == treePath.leaf.toString() }
        ?.let {
            JCTypeParameter(it,
                            javac.getTreePath(it, treePath.compilationUnit),
                            javac)
        }

private val stubs = hashMapOf<FqName, JavaClass>()

private fun createStubClassifier(fqn: FqName) = object : JavaClass {
    override val isAbstract: Boolean
        get() = false

    override val isStatic: Boolean
        get() = false

    override val isFinal: Boolean
        get() = false

    override val visibility: Visibility
        get() = Visibilities.PUBLIC

    override val typeParameters: List<JavaTypeParameter>
        get() = emptyList()

    override val fqName
        get() = fqn

    override val supertypes: Collection<JavaClassifierType>
        get() = emptyList()

    override val innerClasses: Collection<JavaClass>
        get() = emptyList()

    override val outerClass: JavaClass?
        get() = null

    override val isInterface: Boolean
        get() = false

    override val isAnnotationType: Boolean
        get() = false

    override val isEnum: Boolean
        get() = false

    override val lightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    override val methods: Collection<JavaMethod>
        get() = emptyList()

    override val fields: Collection<JavaField>
        get() = emptyList()

    override val constructors: Collection<JavaConstructor>
        get() = emptyList()

    override val name
        get() = fqn.shortNameOrSpecial()

    override val annotations
        get() = emptyList<JavaAnnotation>()

    override val isDeprecatedInJavaDoc: Boolean
        get() = false

    override fun findAnnotation(fqName: FqName) = null

}.also { stubs[fqn] = it }