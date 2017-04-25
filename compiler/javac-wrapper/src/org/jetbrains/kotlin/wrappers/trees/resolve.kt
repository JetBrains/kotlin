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

import com.sun.source.tree.Tree
import com.sun.source.util.TreePath
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.name.FqName

class TreePathResolverCache(private val javac: JavacWrapper) {

    private val cache = hashMapOf<Tree, JavaClassifier?>()

    fun resolve(treePath: TreePath): JavaClassifier? = with(treePath) {
        if (cache.containsKey(leaf)) return cache[leaf]

        return tryToGetClassifier().apply { cache[leaf] = this }
    }

    private fun TreePath.tryToGetClassifier(): JavaClassifier? {
        val name = leaf.toString().substringBefore("<").substringAfter("@")
        val nameParts = name.split(".")

        with(compilationUnit as JCTree.JCCompilationUnit) {
            tryToResolveInner(name, javac, nameParts)?.let { return it }
            tryToResolvePackageClass(name, javac, nameParts)?.let { return it }
            tryToResolveByFqName(name, javac)?.let { return it }
            tryToResolveSingleTypeImport(name, javac, nameParts)?.let { return it }
            tryToResolveTypeImportOnDemand(name, javac, nameParts)?.let { return it }
            tryToResolveInJavaLang(name, javac)?.let { return it }
        }

        return tryToResolveTypeParameter(javac)
    }

    private fun TreePath.tryToResolveInner(name: String,
                                           javac: JavacWrapper,
                                           nameParts: List<String> = emptyList()): JavaClass? = findEnclosingClasses(javac)
            ?.forEach {
                it.findInner(name, javac, nameParts)?.let { return it }
            }.let { return null }

    private fun TreePath.findEnclosingClasses(javac: JavacWrapper) = filterIsInstance<JCTree.JCClassDecl>()
            .filter { it.extending != leaf && !it.implementing.contains(leaf) }
            .reversed()
            .joinToString(separator = ".", prefix = "${compilationUnit.packageName}.") { it.simpleName }
            .let { javac.findClass(FqName(it)) }
            ?.let {
                arrayListOf(it).apply {
                    var enclosingClass = it.outerClass
                    while (enclosingClass != null) {
                        add(enclosingClass)
                        enclosingClass = enclosingClass.outerClass
                    }
                }
            }

    private fun JCTree.JCCompilationUnit.tryToResolveSingleTypeImport(name: String,
                                                                      javac: JavacWrapper,
                                                                      nameParts: List<String> = emptyList()): JavaClass? {
        nameParts.size
                .takeIf { it > 1 }
                ?.let {
                    imports.filter { it.qualifiedIdentifier.toString().endsWith(".${nameParts.first()}") }
                            .forEach { find(FqName("${it.qualifiedIdentifier}"), javac, nameParts)?.let { return it } }
                            .let { return null }
                }

        return imports
                .find { it.qualifiedIdentifier.toString().endsWith(".$name") }
                ?.let {
                    FqName(it.qualifiedIdentifier.toString())
                            .let { javac.findClass(it) ?: javac.getKotlinClassifier(it) }
                }
    }

    private fun JCTree.JCCompilationUnit.tryToResolvePackageClass(name: String,
                                                                  javac: JavacWrapper,
                                                                  nameParts: List<String> = emptyList()): JavaClass? {
        return nameParts.size
                .takeIf { it > 1 }
                ?.let {
                    find(FqName("$packageName.${nameParts.first()}"), javac, nameParts)
                } ?: javac.findClass(FqName("$packageName.$name")) ?: javac.getKotlinClassifier(FqName("$packageName.$name"))
    }

    private fun JCTree.JCCompilationUnit.tryToResolveTypeImportOnDemand(name: String,
                                                                        javac: JavacWrapper,
                                                                        nameParts: List<String> = emptyList()): JavaClass? {
        with(imports.filter { it.qualifiedIdentifier.toString().endsWith("*") }) {
            nameParts.size
                    .takeIf { it > 1 }
                    ?.let {
                        forEach { pack ->
                            find(FqName("${pack.qualifiedIdentifier.toString().substringBefore("*")}${nameParts.first()}"), javac, nameParts)
                                    ?.let { return it }
                        }.let { return null }
                    }

            forEach {
                val fqName = "${it.qualifiedIdentifier.toString().substringBefore("*")}$name".let(::FqName)
                (javac.findClass(fqName) ?: javac.getKotlinClassifier(fqName))?.let { return it }
            }.let { return null }
        }
    }

    private fun TreePath.tryToResolveTypeParameter(javac: JavacWrapper) =
            filter { it is JCTree.JCClassDecl || it is JCTree.JCMethodDecl }
            .flatMap {
                when (it) {
                    is JCTree.JCClassDecl -> it.typarams
                    is JCTree.JCMethodDecl -> it.typarams
                    else -> emptyList<JCTree.JCTypeParameter>()
                }
            }
            .find { it.toString().substringBefore(" ") == leaf.toString() }
            ?.let {
                TreeBasedTypeParameter(it,
                                       javac.getTreePath(it, compilationUnit),
                                       javac)
            }

}

fun JavaClass.findInner(name: String,
                        javac: JavacWrapper,
                        nameParts: List<String> = emptyList()) : JavaClass? {
    nameParts.size
            .takeIf { it > 1 }
            ?.let { return find(FqName("${fqName!!.asString()}.${nameParts[0]}"), javac, nameParts) }

    if (name == this.fqName?.shortName()?.asString()) return this
    with(FqName("${fqName!!.asString()}.$name")) {
        javac.findClass(this)?.let { return it }
        javac.getKotlinClassifier(this)?.let { return it }
    }

    supertypes
            .mapNotNull { it.classifier as? JavaClass }
            .forEach { it.findInner(name, javac)?.let { return it } }
            .let { return null }
}

fun tryToResolveByFqName(name: String,
                         javac: JavacWrapper) = with (FqName(name)) {
    javac.findClass(this) ?: javac.getKotlinClassifier(this)
}

fun tryToResolveInJavaLang(name: String,
                           javac: JavacWrapper) = javac.findClass(FqName("java.lang.$name"))


fun find(fqName: FqName,
                 javac: JavacWrapper,
                 nameParts: List<String>): JavaClass? {
    val initial = with(fqName) {
        javac.findClass(this)
        ?: javac.getKotlinClassifier(this)
        ?: return null
    }
    nameParts.drop(1).fold(initial) {
        javaClass, it -> javaClass.findInner(it, javac) ?: return null
    }.let { return it }
}