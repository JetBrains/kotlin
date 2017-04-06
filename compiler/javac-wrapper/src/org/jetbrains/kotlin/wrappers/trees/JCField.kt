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
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.name.Name

class JCField<out T : JCTree.JCVariableDecl>(tree: T,
                                             treePath: TreePath,
                                             containingClass: JavaClass,
                                             javac: JavacWrapper) : JCMember<T>(tree, treePath, containingClass, javac), JavaField {

    override val name
        get() = Name.identifier(tree.name.toString())

    override val isAbstract
        get() = tree.modifiers.isAbstract

    override val isStatic
        get() = containingClass.isInterface || tree.modifiers.isStatic

    override val isFinal
        get() = containingClass.isInterface || tree.modifiers.isFinal

    override val visibility
        get() = if (containingClass.isInterface) Visibilities.PUBLIC else tree.modifiers.visibility

    override val isEnumEntry
        get() = tree.modifiers.flags and Flags.ENUM.toLong() != 0L

    override val type
        get() = JCType.create(tree.getType(), treePath, javac)

}