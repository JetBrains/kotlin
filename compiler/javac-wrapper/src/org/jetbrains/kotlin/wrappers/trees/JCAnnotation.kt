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
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.name.FqName

class JCAnnotation(val annotation: JCTree.JCAnnotation,
                   val treePath: TreePath,
                   val javac: JavacWrapper) : JavaElement, JavaAnnotation {

    override val arguments
        get() = annotation.arguments
                .map { JCAnnotationArgument(it, FqName(it.toString()), javac) }

    override val classId
        get() = resolve()?.computeClassId()

    override fun resolve() = TreePath.getPath(treePath.compilationUnit, annotation.annotationType).resolve(javac).second

}