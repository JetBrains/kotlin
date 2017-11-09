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

import com.sun.source.tree.CompilationUnitTree
import com.sun.source.tree.Tree
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.name.Name

class IdentifierResolver(private val javac: JavacWrapper) {

    fun resolve(tree: Tree, compilationUnit: CompilationUnitTree, containingClass: JavaClass): JavaField? {
        if (tree is JCTree.JCIdent) {
            val fieldName = Name.identifier(tree.name.toString())
            return CurrentClassAndInnerFieldScope(javac, compilationUnit).findField(containingClass, fieldName)
        }
        else if (tree is JCTree.JCFieldAccess) {
            val javaClass = javac.resolve(tree.selected, compilationUnit, containingClass) as? JavaClass ?: return null
            if (javaClass is MockKotlinClassifier) {
                return javaClass.findField(tree.name.toString())
            }

            val fieldName = Name.identifier(tree.name.toString())
            return CurrentClassAndInnerFieldScope(javac, compilationUnit, null).findField(javaClass, fieldName)
        }

        return null
    }

}

private abstract class FieldScope(protected val javac: JavacWrapper,
                                  protected val compilationUnit: CompilationUnitTree) {

    protected val helper = ResolveHelper(javac, compilationUnit)

    abstract val parent: FieldScope?

    abstract fun findField(javaClass: JavaClass, name: Name): JavaField?

    protected fun JavaClass.findFieldIncludingSupertypes(name: Name, checkedSupertypes: HashSet<JavaClass> = hashSetOf()): JavaField? {
        fields.find { it.name == name }?.let {
            checkedSupertypes.addAll(collectAllSupertypes())
            return it
        }
        return supertypes
                .mapNotNull {
                    val classifier = it.classifier as? JavaClass
                    if (classifier !in checkedSupertypes) {
                        if (classifier is MockKotlinClassifier) {
                            classifier.findField(name.asString())
                        }
                        else {
                            classifier?.findFieldIncludingSupertypes(name, checkedSupertypes)
                        }
                    }
                    else null
                }.singleOrNull()
    }

}

private class StaticImportOnDemandFieldScope(javac: JavacWrapper,
                                             compilationUnit: CompilationUnitTree) : FieldScope(javac, compilationUnit) {
    override val parent: FieldScope?
        get() = null

    override fun findField(javaClass: JavaClass, name: Name): JavaField? {
        val foundFields = hashSetOf<JavaField>()

        staticAsteriskImports().forEach { import ->
            val pathSegments = import.split(".")
            val importedClass = helper.findImport(pathSegments)
            if (importedClass is MockKotlinClassifier) {
                return importedClass.findField(name.asString())
            }

            importedClass?.findFieldIncludingSupertypes(name)?.let { foundFields.add(it) }
        }

        return foundFields.singleOrNull()
    }

    private fun staticAsteriskImports() =
            (compilationUnit as JCTree.JCCompilationUnit).imports
                    .filter { it.staticImport }
                    .mapNotNull {
                        val fqName = it.qualifiedIdentifier.toString()
                        if (fqName.endsWith("*")) {
                            fqName.dropLast(2)
                        }
                        else null
                    }

}

private class StaticImportFieldScope(javac: JavacWrapper,
                                     compilationUnit: CompilationUnitTree) : FieldScope(javac, compilationUnit) {

    override val parent: FieldScope
        get() = StaticImportOnDemandFieldScope(javac, compilationUnit)

    override fun findField(javaClass: JavaClass, name: Name): JavaField? {
        val staticImports = staticImports(name.asString()).toSet().takeIf { it.isNotEmpty() }
                            ?: return parent.findField(javaClass, name)

        val import = staticImports.singleOrNull() ?: return null
        val pathSegments = import.split(".").dropLast(1)
        val importedClass = helper.findImport(pathSegments)
        if (importedClass is MockKotlinClassifier) {
            return importedClass.findField(name.asString())
        }

        return importedClass?.findFieldIncludingSupertypes(name)

    }

    private fun staticImports(fieldName: String) =
            (compilationUnit as JCTree.JCCompilationUnit).imports
                    .filter { it.staticImport }
                    .mapNotNull {
                        val import = it.qualifiedIdentifier as? JCTree.JCFieldAccess
                        val importedField = import?.name?.toString()
                        if (importedField == fieldName) {
                            import.toString()
                        }
                        else null
                    }

}

private class CurrentClassAndInnerFieldScope(javac: JavacWrapper,
                                             compilationUnit: CompilationUnitTree,
                                             override val parent: FieldScope? = StaticImportFieldScope(javac, compilationUnit)) : FieldScope(javac, compilationUnit) {

    override fun findField(javaClass: JavaClass, name: Name): JavaField? {
        javaClass.enclosingClasses().forEach {
            it.findFieldIncludingSupertypes(name)?.let { return it }
        }

        return parent?.findField(javaClass, name)
    }

    private fun JavaClass.enclosingClasses(): List<JavaClass> = arrayListOf<JavaClass>().also { classes ->
        classes.add(this)
        outerClass?.let { classes.addAll(it.enclosingClasses()) }
    }

}