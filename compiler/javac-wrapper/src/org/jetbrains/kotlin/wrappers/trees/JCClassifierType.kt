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
import org.jetbrains.kotlin.load.java.structure.*

sealed class ClassifierType<out T : JCTree>(tree: T,
                                              treePath: TreePath,
                                              javac: JavacWrapper) : JCType<T>(tree, treePath, javac), JavaClassifierType {

    override val classifier by lazy { treePath.resolve(javac) }

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
                        else -> throw UnsupportedOperationException("${it.kind} cannot have a type parameter")
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
