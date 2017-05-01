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
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class TreeBasedTypeParameter<out T : JCTree.JCTypeParameter>(tree: T,
                                                             treePath: TreePath,
                                                             javac: JavacWrapper) : TreeBasedElement<T>(tree, treePath, javac), JavaTypeParameter {

    override val name: Name
        get() = Name.identifier(tree.name.toString())

    override val annotations: Collection<JavaAnnotation> by lazy { tree.annotations().map { TreeBasedAnnotation(it, treePath, javac) } }

    override fun findAnnotation(fqName: FqName) = annotations.firstOrNull { it.classId?.asSingleFqName() == fqName }

    override val isDeprecatedInJavaDoc: Boolean
        get() = false

    override val upperBounds: Collection<JavaClassifierType>
        get() = tree.bounds.map {
            when (it) {
                is JCTree.JCTypeApply -> TreeBasedGenericClassifierType(it, TreePath(treePath, it), javac)
                is JCTree.JCIdent -> TreeBasedNonGenericClassifierType(it, TreePath(treePath, it), javac)
                else -> null
            }
        }.filterNotNull()

}