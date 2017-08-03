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

package org.jetbrains.kotlin.javac.resolve

import com.sun.source.tree.Tree
import com.sun.source.util.TreePath
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.javac.wrappers.trees.TreeBasedClass
import org.jetbrains.kotlin.javac.wrappers.trees.TreeBasedTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ClassifierResolver(private val javac: JavacWrapper) {

    private val cache = hashMapOf<Tree, JavaClassifier?>()
    private val beingResolved = hashSetOf<Tree>()

    fun resolve(treePath: TreePath): JavaClassifier? = with (treePath) {
        if (cache.containsKey(leaf)) return cache[leaf]
        if (treePath.leaf in beingResolved) return null
        beingResolved(treePath.leaf)

        return tryToResolve().apply {
            cache[leaf] = this
            removeBeingResolved(treePath.leaf)
        }
    }

    // to avoid StackOverflow when there are cyclic dependencies
    private fun beingResolved(tree: Tree) {
        if (tree is JCTree.JCTypeApply) {
            beingResolved(tree.clazz)
        }
        if (tree is JCTree.JCFieldAccess) {
            beingResolved.add(tree)
            beingResolved(tree.selected)
        }
        else beingResolved.add(tree)
    }

    private fun removeBeingResolved(tree: Tree) {
        if (tree is JCTree.JCTypeApply) {
            beingResolved(tree.clazz)
        }
        if (tree is JCTree.JCFieldAccess) {
            beingResolved.remove(tree)
            beingResolved(tree.selected)
        }
        else beingResolved.remove(tree)
    }

    private fun pathSegments(path: String): List<String> {
        val pathSegments = arrayListOf<String>()
        var numberOfBrackets = 0
        var builder = StringBuilder()
        path.forEach { char ->
            when (char) {
                '<' -> numberOfBrackets++
                '>' -> numberOfBrackets--
                '.' -> {
                    if (numberOfBrackets == 0) {
                        pathSegments.add(builder.toString())
                        builder = StringBuilder()
                    }
                }
                '@' -> {}
                else -> if (numberOfBrackets == 0) builder.append(char)
            }
        }

        return pathSegments.apply { add(builder.toString()) }
    }

    private fun TreePath.tryToResolve(): JavaClassifier? {
        val pathSegments = pathSegments(leaf.toString())

        return tryToGetTypeParameterFromMethod()?.let { return it } ?:
               createResolutionScope(this).findClass(pathSegments.first(), pathSegments)
    }

    private fun TreePath.tryToGetTypeParameterFromMethod(): TreeBasedTypeParameter? =
            (find { it is JCTree.JCMethodDecl } as? JCTree.JCMethodDecl)
                    ?.typarams?.find { it.name.toString() == leaf.toString() }
                    ?.let {
                        TreeBasedTypeParameter(it,
                                               javac.getTreePath(it, compilationUnit),
                                               javac)
                    }

    private fun createResolutionScope(treePath: TreePath): Scope = CurrentClassAndInnerScope(javac, treePath)

}

private abstract class Scope(protected val javac: JavacWrapper,
                             protected val treePath: TreePath) {

    protected val helper = ResolveHelper(javac, treePath)

    abstract val parent: Scope?

    abstract fun findClass(name: String, pathSegments: List<String>): JavaClassifier?

}

private class GlobalScope(javac: JavacWrapper, treePath: TreePath) : Scope(javac, treePath) {

    override val parent: Scope?
        get() = null

    override fun findClass(name: String, pathSegments: List<String>): JavaClass? {
        findByFqName(pathSegments)?.let { return it }

        return helper.findJavaOrKotlinClass(classId("java.lang", name))?.let { javaClass ->
            helper.getJavaClassFromPathSegments(javaClass, pathSegments)
        }
    }

    private fun findByFqName(pathSegments: List<String>): JavaClass? {
        pathSegments.forEachIndexed { index, _ ->
            if (index != 0) {
                val packageFqName = pathSegments.take(index).joinToString(separator = ".")
                helper.findPackage(packageFqName)?.let { pack ->
                    val className = pathSegments.drop(index)
                    helper.findJavaOrKotlinClass(ClassId(pack, Name.identifier(className.first())))?.let { javaClass ->
                        return helper.getJavaClassFromPathSegments(javaClass, className)
                    }
                }
            }
        }

        // try to find in <root>
        return helper.findJavaOrKotlinClass(classId("", pathSegments.first()))?.let { javaClass ->
            helper.getJavaClassFromPathSegments(javaClass, pathSegments)
        }
    }

}

private class ImportOnDemandScope(javac: JavacWrapper,
                                  treePath: TreePath) : Scope(javac, treePath) {

    override val parent: Scope
        get() = GlobalScope(javac, treePath)

    override fun findClass(name: String, pathSegments: List<String>): JavaClassifier? {
        asteriskImports()
                .mapNotNullTo(hashSetOf()) { helper.findImport("$it$name".split(".")) }
                .takeIf { it.isNotEmpty() }
                ?.let {
                    return it.singleOrNull()?.let { javaClass ->
                        helper.getJavaClassFromPathSegments(javaClass, pathSegments)
                    }
                }

        return parent.findClass(name, pathSegments)
    }

    private fun asteriskImports() =
        treePath.compilationUnit.imports
                .mapNotNull {
                    val fqName = it.qualifiedIdentifier.toString()
                    if (fqName.endsWith("*")) {
                        fqName.dropLast(1)
                    }
                    else null
                }

}

private class PackageScope(javac: JavacWrapper,
                           treePath: TreePath) : Scope(javac, treePath) {

    override val parent: Scope
        get() = ImportOnDemandScope(javac, treePath)

    override fun findClass(name: String, pathSegments: List<String>): JavaClassifier? {
        helper.findJavaOrKotlinClass(classId(treePath.compilationUnit.packageName?.toString() ?: "", name))
                ?.let { javaClass ->
                    return helper.getJavaClassFromPathSegments(javaClass, pathSegments)
                }

        return parent.findClass(name, pathSegments)
    }

}

private class SingleTypeImportScope(javac: JavacWrapper,
                                    treePath: TreePath) : Scope(javac, treePath) {

    override val parent: Scope
        get() = PackageScope(javac, treePath)

    override fun findClass(name: String, pathSegments: List<String>): JavaClassifier? {
        val imports = imports(name).toSet().takeIf { it.isNotEmpty() }
                      ?: return parent.findClass(name, pathSegments)

        imports.singleOrNull() ?: return null

        return helper.findImport(imports.first().split("."))
                ?.let { javaClass -> helper.getJavaClassFromPathSegments(javaClass, pathSegments) }
    }

    private fun imports(firstSegment: String) =
        (treePath.compilationUnit as JCTree.JCCompilationUnit).imports
                .mapNotNull {
                    val fqName = it.qualifiedIdentifier.toString()
                    if (fqName.endsWith(".$firstSegment")) {
                        fqName
                    }
                    else null
                }

}

private class CurrentClassAndInnerScope(javac: JavacWrapper,
                                        treePath: TreePath) : Scope(javac, treePath) {

    override val parent: Scope
        get() = SingleTypeImportScope(javac, treePath)

    override fun findClass(name: String, pathSegments: List<String>): JavaClassifier? {
        val identifier = Name.identifier(name)
        treePath.enclosingClasses.forEach {
            (it as? TreeBasedClass)?.typeParameters
                    ?.find { typeParameter -> typeParameter.name == identifier }
                    ?.let { typeParameter -> return typeParameter }

            helper.findInnerOrNested(it, identifier)?.let { javaClass -> return helper.getJavaClassFromPathSegments(javaClass, pathSegments) }

            if (it.name == identifier && pathSegments.size == 1) return it
        }

        return parent.findClass(name, pathSegments)
    }

    private val TreePath.enclosingClasses: List<JavaClass>
        get() {
            val outerClasses = filterIsInstance<JCTree.JCClassDecl>()
                    .dropWhile { it.extending == leaf || leaf in it.implementing }
                    .asReversed()
                    .map { it.simpleName.toString() }

            val packageName = compilationUnit.packageName?.toString() ?: ""
            val outermostClassName = outerClasses.firstOrNull() ?: return emptyList()

            val outermostClassId = classId(packageName, outermostClassName)
            var outermostClass = javac.findClass(outermostClassId) ?: return emptyList()

            val classes = arrayListOf<JavaClass>()
            classes.add(outermostClass)

            for (it in outerClasses.drop(1)) {
                outermostClass = outermostClass.findInnerClass(Name.identifier(it))
                                 ?: throw AssertionError("Couldn't find a class ($it) that is surely defined in ${outermostClass.fqName?.asString()}")
                classes.add(outermostClass)
            }

            return classes.reversed()
        }

}

fun classId(packageName: String = "", className: String) = ClassId(FqName(packageName), Name.identifier(className))