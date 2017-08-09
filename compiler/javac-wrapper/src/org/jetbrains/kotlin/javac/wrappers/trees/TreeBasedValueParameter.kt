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

import com.sun.source.tree.CompilationUnitTree
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class TreeBasedValueParameter(
        tree: JCTree.JCVariableDecl,
        compilationUnit: CompilationUnitTree,
        javac: JavacWrapper,
        private val containingElement: JavaElement
) : TreeBasedElement<JCTree.JCVariableDecl>(tree, compilationUnit, javac), JavaValueParameter {

    override val annotations: Collection<TreeBasedAnnotation> by lazy {
        tree.annotations().map { TreeBasedAnnotation(it, compilationUnit, javac, containingElement) }
    }

    override fun findAnnotation(fqName: FqName) =
            annotations
                    .filter { it.annotation.annotationType.toString().endsWith(fqName.shortName().asString()) }
                    .find { it.classId?.asSingleFqName() == fqName }

    override val isDeprecatedInJavaDoc: Boolean
        get() = javac.isDeprecatedInJavaDoc(tree, compilationUnit)

    override val name: Name
        get() = Name.identifier(tree.name.toString())

    override val type: JavaType
        get() = TreeBasedType.create(tree.getType(), compilationUnit, javac, annotations, containingElement)

    override val isVararg: Boolean
        get() = tree.modifiers.flags and Flags.VARARGS != 0L
}