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
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaConstructor
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.name.Name

class TreeBasedConstructor(
        tree: JCTree.JCMethodDecl,
        compilationUnit: CompilationUnitTree,
        containingClass: JavaClass,
        javac: JavacWrapper
) : TreeBasedMember<JCTree.JCMethodDecl>(tree, compilationUnit, containingClass, javac), JavaConstructor {

    override val name: Name
        get() = Name.identifier(tree.name.toString())

    override val isAbstract: Boolean
        get() = false

    override val isStatic: Boolean
        get() = false

    override val isFinal: Boolean
        get() = true

    override val visibility: Visibility
        get() = tree.modifiers.visibility

    override val typeParameters: List<JavaTypeParameter>
        get() = tree.typeParameters.map { TreeBasedTypeParameter(it, compilationUnit, javac, this) }

    override val valueParameters: List<JavaValueParameter>
        get() = tree.parameters.map { TreeBasedValueParameter(it, compilationUnit, javac, this) }

}