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
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.Name

class TreeBasedMethod(tree: JCTree.JCMethodDecl,
                      treePath: TreePath,
                      containingClass: JavaClass,
                      javac: JavacWrapper) : TreeBasedMember<JCTree.JCMethodDecl>(tree, treePath, containingClass, javac), JavaMethod {

    override val name: Name
        get() = Name.identifier(tree.name.toString())

    override val isAbstract: Boolean
        get() = if (containingClass.isInterface && !tree.modifiers.hasDefaultModifier) true else tree.modifiers.isAbstract

    override val isStatic: Boolean
        get() = tree.modifiers.isStatic

    override val isFinal: Boolean
        get() = tree.modifiers.isFinal

    override val visibility: Visibility
        get() = if (containingClass.isInterface) Visibilities.PUBLIC else tree.modifiers.visibility

    override val typeParameters: List<JavaTypeParameter>
        get() = tree.typeParameters.map { TreeBasedTypeParameter(it, TreePath(treePath, it), javac) }

    override val valueParameters: List<JavaValueParameter>
        get() = tree.parameters
                .map { TreeBasedValueParameter(it, TreePath(treePath, it), javac) }

    override val returnType: JavaType
        get() = TreeBasedType.create(tree.returnType, treePath, javac)

    override val hasAnnotationParameterDefaultValue: Boolean
        get() = tree.defaultValue != null
}