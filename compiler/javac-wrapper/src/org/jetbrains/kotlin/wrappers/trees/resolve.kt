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
import org.jetbrains.kotlin.javac.MockKotlinClassifier
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.name.FqName

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
    if (name.contains(".")) {
        // try to find a class with fqName = name
        javac.findClass(FqName(name))?.let { return it }
        javac.getKotlinClassifier(FqName(name))?.let { return it }

        val nameParts = name.split(".")

        var javaClass = resolveImport(nameParts[0], compilationUnit, javac)
                        ?: javac.findClass(FqName("${compilationUnit.packageName}.${nameParts[0]}"))

        if (javaClass != null) {
            nameParts.drop(1).forEach {
                javaClass?.tryToResolveInner(it, javac)
                        ?.let { javaClass = it }
                ?: let {
                    javaClass = null
                    return@forEach
                }
            }
            javaClass?.let { return it }
        }
    }

    // try to find an inner class
    findOuterClass(javac)
            ?.tryToResolveInner(name, javac)
            ?.let { return it }

    // try to find a package class
    javac.findClass(FqName("${compilationUnit.packageName}.$name"))?.let { return it }
    //try to find a Kotlin class
    javac.getKotlinClassifier(FqName("${compilationUnit.packageName}.$name"))?.let { return it }
    // try to find a class from java.lang package
    javac.findClass(FqName("java.lang.$name"))?.let { return it }

    // try to find a type parameter
    return typeParameter(javac)?.let { return it }
}

private fun TreePath.findOuterClass(javac: JavacWrapper) = filterIsInstance<JCTree.JCClassDecl>()
        .filter { it.extending != leaf && !it.implementing.contains(leaf) }
        .reversed()
        .joinToString(separator = ".", prefix = "${compilationUnit.packageName}.") { it.simpleName }
        .let { javac.findClass(FqName(it)) }

private fun JavaClass.tryToResolveInner(name: String, javac: JavacWrapper): JavaClass? {
    javac.findClass(FqName("${fqName!!.asString()}.$name"))?.let { return it }

    supertypes
            .mapNotNull { it.classifier as? JavaClass }
            .filter { it !is MockKotlinClassifier }
            .forEach { it.tryToResolveInner(name, javac)?.let { return it } }

    return null
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