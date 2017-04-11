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
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
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

fun JCTree.annotations() = when (this) {
    is JCTree.JCMethodDecl -> mods?.annotations
    is JCTree.JCClassDecl -> mods?.annotations
    is JCTree.JCVariableDecl -> mods?.annotations
    is JCTree.JCTypeParameter -> annotations
    else -> null
} ?: emptyList<JCTree.JCAnnotation>()

fun TreePath.resolve(javac: JavacWrapper): JavaClassifier? {
    val name = leaf.toString().substringBefore("<").substringAfter("@")
    val compilationUnit = compilationUnit as JCTree.JCCompilationUnit

    return resolveImport(name, compilationUnit, javac) ?: tryToResolve(name, compilationUnit, javac)
}

private fun resolveImport(name: String,
                          compilationUnit: JCTree.JCCompilationUnit,
                          javac: JavacWrapper) = with(compilationUnit.imports) {
    find { it.qualifiedIdentifier.toString().endsWith(".$name") }
            ?.let {
                it.qualifiedIdentifier.toString()
                        .let(::FqName)
                        .let { javac.findClass(it) ?: javac.getKotlinClassifier(it) }
            }
    ?: filter { it.qualifiedIdentifier.toString().endsWith("*") }
            .map {
                val fqName = "${it.qualifiedIdentifier.toString().substringBefore("*")}$name".let(::FqName)
                javac.findClass(fqName) ?: javac.getKotlinClassifier(fqName)
            }
            .firstOrNull()
}

private fun TreePath.tryToResolve(name: String,
                          compilationUnit: JCTree.JCCompilationUnit,
                          javac: JavacWrapper): JavaClassifier? {
    // try to find an inner class
    (parentPath.find { it is JCTree.JCClassDecl } as? JCTree.JCClassDecl)?.let { outerClass ->
        javac.findClass(FqName("${compilationUnit.packageName}.${outerClass.name}.$name"))
                ?.let { return it }
    }

    // try to find a package class
    javac.findClass(FqName("${compilationUnit.packageName}.$name"))?.let { return it }
    //try to find a Kotlin class
    javac.getKotlinClassifier(FqName("${compilationUnit.packageName}.$name"))?.let { return it }
    // try to find a class from java.lang package
    javac.findClass(FqName("java.lang.$name"))?.let { return it }
    // try to find a class with fqName = name
    javac.findClass(FqName(name))?.let { return it }

    // try to find a type parameter
    typeParameter(javac)?.let { return it }

    return javac.getKotlinClassifier(FqName(name))
}

private fun TreePath.typeParameter(javac: JavacWrapper) = filter { it is JCTree.JCClassDecl || it is JCTree.JCMethodDecl }
        .flatMap {
            when (it) {
                is JCTree.JCClassDecl -> it.typarams
                is JCTree.JCMethodDecl -> it.typarams
                else -> emptyList<JCTree.JCTypeParameter>()
            }
        }
        .find { it.toString().substringBefore(" ") == leaf.toString() }
        ?.let {
            JCTypeParameter(it,
                            javac.getTreePath(it, compilationUnit),
                            javac)
        }

fun JavaClass.computeClassId(): ClassId? {
    val outer = outerClass
    outer?.let {
        val parentClassId = outer.computeClassId() ?: return null
        return parentClassId.createNestedClassId(name)
    }

    return fqName?.let { ClassId.topLevel(it) }
}