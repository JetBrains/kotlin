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
import org.jetbrains.kotlin.javac.Javac
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import javax.lang.model.element.Modifier

val JCTree.JCModifiers.isAbstract
    get() = Modifier.ABSTRACT in getFlags()

val JCTree.JCModifiers.isFinal
    get() = Modifier.FINAL in getFlags()

val JCTree.JCModifiers.isStatic
    get() = Modifier.STATIC in getFlags()

val JCTree.JCModifiers.visibility
    get() = getFlags().let {
        when {
            Modifier.PUBLIC in it -> Visibilities.PUBLIC
            Modifier.PRIVATE in it -> Visibilities.PRIVATE
            Modifier.PROTECTED in it -> {
                if (Modifier.STATIC in it) JavaVisibilities.PROTECTED_STATIC_VISIBILITY else JavaVisibilities.PROTECTED_AND_PACKAGE
            }
            else -> JavaVisibilities.PACKAGE_VISIBILITY
        }
    }

fun TreePath.resolve(javac: Javac): Pair<FqName, JavaClass?> {
    val name = leaf.toString().substringBefore("<").substringAfter("@")
    val compilationUnit = compilationUnit as JCTree.JCCompilationUnit

    return resolveImport(name, compilationUnit, javac)
           ?: javac.findClass(FqName("java.lang.$name"))?.let { return it.fqName!! to it }
           ?: javac.findClass(FqName(name))?.let { return it.fqName!! to it }
           ?: tryToResolve(name, compilationUnit, javac)
}

private fun resolveImport(name: String,
                          compilationUnit: JCTree.JCCompilationUnit,
                          javac: Javac) = with(compilationUnit.imports) {
    firstOrNull { it.qualifiedIdentifier.toString().endsWith(".$name") }
            ?.let {
                it.qualifiedIdentifier.toString().let(::FqName)
                        .let { it to javac.findClass(it) }
            }
    ?: filter { it.qualifiedIdentifier.toString().endsWith("*") }
            .map {
                javac.findClassesFromPackage(it.qualifiedIdentifier.toString()
                                                     .substringBeforeLast(".").let(::FqName))
                        .find { it.name.identifier == name }
                        ?.let { it.fqName!! to it }
            }
            .firstOrNull()
}

private fun TreePath.tryToResolve(name: String,
                                  compilationUnit: JCTree.JCCompilationUnit,
                                  javac: Javac): Pair<FqName, JavaClass?> {
    val simpleName = name.substringAfterLast(".")
    val classes = javac.findClassesFromPackage(FqName("${compilationUnit.packageName}"))
            .filter { it.name.asString() == simpleName }

    val outerClass = parentPath.find { it is JCTree.JCClassDecl } as? JCTree.JCClassDecl
                     ?: return FqName("${compilationUnit.packageName}.$name") to null

    classes.find {
        it.fqName!!.asString().contains(simpleName)
        && it.fqName!!.asString().contains(outerClass.name.toString())
    }?.let { return it.fqName!! to it }

    classes.find { it.outerClass == null }?.let { return it.fqName!! to it }

    return FqName("${compilationUnit.packageName}.$name") to null
}

fun JavaClass.computeClassId(): ClassId? {
    val outer = outerClass
    outer?.let {
        val parentClassId = outer.computeClassId() ?: return null
        return parentClassId.createNestedClassId(name)
    }

    return fqName?.let { ClassId.topLevel(it) }
}