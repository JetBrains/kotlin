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

import com.sun.source.tree.Tree
import com.sun.source.util.TreePath
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.javac.MockKotlinClassifier
import org.jetbrains.kotlin.load.java.JavaVisibilities
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

    abstract val parent: Scope?

    abstract fun findClass(name: String, pathSegments: List<String>): JavaClassifier?

    protected fun getJavaClassFromPathSegments(javaClass: JavaClass,
                                               pathSegments: List<String>) =
            if (pathSegments.size == 1) {
                javaClass
            }
            else {
                javaClass.findInnerOrNested(pathSegments.drop(1))
            }

    protected fun findImport(pathSegments: List<String>): JavaClass? {
        pathSegments.forEachIndexed { index, _ ->
            if (index == pathSegments.lastIndex) return null
            val packageFqName = pathSegments.dropLast(index + 1).joinToString(separator = ".")
            findPackage(packageFqName)?.let { pack ->
                val className = pathSegments.takeLast(index + 1)
                return findJavaOrKotlinClass(ClassId(pack, Name.identifier(className.first())))?.let { javaClass ->
                    getJavaClassFromPathSegments(javaClass, className)
                }
            }
        }

        return null
    }

    protected fun findJavaOrKotlinClass(classId: ClassId) = javac.findClass(classId) ?: javac.getKotlinClassifier(classId)

    protected fun JavaClass.findInnerOrNested(name: Name, checkedSupertypes: HashSet<JavaClass> = hashSetOf()): JavaClass? {
        findVisibleInnerOrNestedClass(name)?.let {
            checkedSupertypes.addAll(collectAllSupertypes())
            return it
        }

        return supertypes
                .mapNotNull {
                    (it.classifier as? JavaClass)?.let { supertype ->
                        if (supertype !in checkedSupertypes) {
                            supertype.findInnerOrNested(name, checkedSupertypes)
                        } else null
                    }
                }.singleOrNull()
    }

    protected fun findPackage(packageName: String): FqName? {
        val fqName = if (packageName.isNotBlank()) FqName(packageName) else FqName.ROOT
        javac.hasKotlinPackage(fqName)?.let { return it }

        return javac.findPackage(fqName)?.fqName
    }

    private fun JavaClass.findVisibleInnerOrNestedClass(name: Name) = findInnerClass(name)?.let { innerOrNestedClass ->
        when (innerOrNestedClass.visibility) {
            Visibilities.PRIVATE -> null
            JavaVisibilities.PACKAGE_VISIBILITY -> {
                val classId = (innerOrNestedClass as? MockKotlinClassifier)?.classId ?: innerOrNestedClass.computeClassId()
                if (classId?.packageFqName?.asString() == (treePath.compilationUnit.packageName?.toString() ?: "")) innerOrNestedClass else null
            }
            else -> innerOrNestedClass
        }
    }

    private fun JavaClass.collectAllSupertypes(): Set<JavaClass> =
            hashSetOf(this).apply {
                supertypes.mapNotNull { it.classifier as? JavaClass }.forEach { addAll(it.collectAllSupertypes()) }
            }

    private fun JavaClass.findInnerOrNested(pathSegments: List<String>): JavaClass? =
            pathSegments.fold(this) { javaClass, it -> javaClass.findInnerOrNested(Name.identifier(it)) ?: return null }

}

private class GlobalScope(javac: JavacWrapper, treePath: TreePath) : Scope(javac, treePath) {

    override val parent: Scope?
        get() = null

    override fun findClass(name: String, pathSegments: List<String>): JavaClass? {
        findByFqName(pathSegments)?.let { return it }

        return findJavaOrKotlinClass(classId("java.lang", name))?.let { javaClass ->
            getJavaClassFromPathSegments(javaClass, pathSegments)
        }
    }

    private fun findByFqName(pathSegments: List<String>): JavaClass? {
        pathSegments.forEachIndexed { index, _ ->
            if (index != 0) {
                val packageFqName = pathSegments.take(index).joinToString(separator = ".")
                findPackage(packageFqName)?.let { pack ->
                    val className = pathSegments.drop(index)
                    findJavaOrKotlinClass(ClassId(pack, Name.identifier(className.first())))?.let { javaClass ->
                        return getJavaClassFromPathSegments(javaClass, className)
                    }
                }
            }
        }

        // try to find in <root>
        return findJavaOrKotlinClass(classId("", pathSegments.first()))?.let { javaClass ->
            getJavaClassFromPathSegments(javaClass, pathSegments)
        }
    }

}

private class ImportOnDemandScope(javac: JavacWrapper,
                                  treePath: TreePath) : Scope(javac, treePath) {

    override val parent: Scope
        get() = GlobalScope(javac, treePath)

    override fun findClass(name: String, pathSegments: List<String>): JavaClassifier? {
        asteriskImports()
                .mapNotNullTo(hashSetOf()) { findImport("$it$name".split(".")) }
                .takeIf { it.isNotEmpty() }
                ?.let {
                    return it.singleOrNull()?.let { javaClass ->
                        getJavaClassFromPathSegments(javaClass, pathSegments)
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
        findJavaOrKotlinClass(classId(treePath.compilationUnit.packageName?.toString() ?: "", name))
                ?.let { javaClass ->
                    return getJavaClassFromPathSegments(javaClass, pathSegments)
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

        return findImport(imports.first().split("."))
                ?.let { javaClass -> getJavaClassFromPathSegments(javaClass, pathSegments) }
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

            it.findInnerOrNested(identifier)?.let { javaClass -> return getJavaClassFromPathSegments(javaClass, pathSegments) }

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