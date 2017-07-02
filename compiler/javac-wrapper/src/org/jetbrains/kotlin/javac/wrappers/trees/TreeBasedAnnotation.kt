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
import com.sun.source.util.TreePath
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class TreeBasedAnnotation(
        private val annotation: JCTree.JCAnnotation,
        private val compilationUnit: CompilationUnitTree,
        private val javac: JavacWrapper
) : JavaElement, JavaAnnotation {

    constructor(
            annotation: JCTree.JCAnnotation,
            treePath: TreePath,
            javac: JavacWrapper
    ) : this(annotation, treePath.compilationUnit, javac)

    override val arguments: Collection<JavaAnnotationArgument>
        get() = annotation.arguments.map { TreeBasedAnnotationArgument(Name.identifier(it.toString())) }

    override val classId: ClassId?
        get() = resolve()?.computeClassId()

    override fun resolve() =
            javac.resolve(TreePath.getPath(compilationUnit, annotation.annotationType)) as? JavaClass

}

class TreeBasedAnnotationArgument(override val name: Name) : JavaAnnotationArgument, JavaElement
