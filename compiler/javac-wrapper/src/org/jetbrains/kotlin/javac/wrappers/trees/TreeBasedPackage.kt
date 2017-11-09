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

import com.intellij.openapi.vfs.VirtualFile
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.MapBasedJavaAnnotationOwner
import org.jetbrains.kotlin.load.java.structure.buildLazyValueForMap
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import javax.tools.JavaFileObject

class TreeBasedPackage(val name: String, val javac: JavacWrapper, val unit: JCTree.JCCompilationUnit) : JavaPackage, MapBasedJavaAnnotationOwner {

    override val fqName: FqName
        get() = FqName(name)

    override val subPackages: Collection<JavaPackage>
        get() = javac.findSubPackages(fqName)

    val virtualFile: VirtualFile? by lazy {
        javac.toVirtualFile(unit.sourceFile)
    }

    override fun getClasses(nameFilter: (Name) -> Boolean) =
            javac.findClassesFromPackage(fqName).filter { nameFilter(it.fqName!!.shortName()) }

    override val annotations: Collection<JavaAnnotation>
        get() = javac.getPackageAnnotationsFromSources(fqName).map { TreeBasedAnnotation(it, unit, javac, this) }

    override val annotationsByFqName: Map<FqName?, JavaAnnotation> by buildLazyValueForMap()

    override fun equals(other: Any?) = (other as? TreeBasedPackage)?.name == name

    override fun hashCode() = name.hashCode()

    override fun toString() = name

}
