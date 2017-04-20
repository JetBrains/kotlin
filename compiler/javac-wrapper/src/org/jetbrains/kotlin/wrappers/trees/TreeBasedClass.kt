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

import com.intellij.psi.CommonClassNames
import com.sun.source.tree.Tree
import com.sun.source.util.TreePath
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeInfo
import org.jetbrains.kotlin.descriptors.Visibilities.PUBLIC
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.wrappers.symbols.SymbolBasedClass
import org.jetbrains.kotlin.wrappers.symbols.SymbolBasedClassifierType
import org.jetbrains.kotlin.wrappers.symbols.SymbolBasedType
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames

class TreeBasedClass<out T : JCTree.JCClassDecl>(tree: T,
                                                 treePath: TreePath,
                                                 javac: JavacWrapper) : TreeBasedElement<T>(tree, treePath, javac), JavaClass {

    override val name
        get() = SpecialNames.safeIdentifier(tree.simpleName.toString())

    override val annotations by lazy { tree.annotations().map { TreeBasedAnnotation(it, treePath, javac) } }

    override fun findAnnotation(fqName: FqName) = annotations.find { it.classId?.asSingleFqName() == fqName }

    override val isDeprecatedInJavaDoc
        get() = findAnnotation(FqName("java.lang.Deprecated")) != null

    override val isAbstract
        get() = tree.modifiers.isAbstract

    override val isStatic
        get() = (outerClass?.isInterface ?: false) || tree.modifiers.isStatic

    override val isFinal
        get() = tree.modifiers.isFinal

    override val visibility
        get() = if (outerClass?.isInterface ?: false) PUBLIC else tree.modifiers.visibility

    override val typeParameters
        get() = tree.typeParameters.map { TreeBasedTypeParameter(it, TreePath(treePath, it), javac) }

    override val fqName = treePath.reversed()
            .joinToString(separator = ".") {
                (it as? JCTree.JCCompilationUnit)?.packageName?.toString()
                ?: (it as JCTree.JCClassDecl).name
            }
            .let(::FqName)

    override val supertypes
        get() = arrayListOf<JavaClassifierType>().apply {
            fun JCTree.mapToJavaClassifierType() = when {
                this is JCTree.JCTypeApply -> TreeBasedClassifierTypeWithTypeArgument(this, TreePath(treePath, this), javac)
                this is JCTree.JCExpression -> TreeBasedClassifierTypeWithoutTypeArgument(this, TreePath(treePath, this), javac)
                else -> null
            }

            if (isEnum) {
                (javac.findClass(FqName("java.lang.Enum")) as? SymbolBasedClass<*>)
                        ?.let { SymbolBasedType.create(it.element.asType(), javac) as? JavaClassifierType }
                        ?.let { add(it) }
            }

            tree.extending.let {
                if (it == null && fqName.asString() != CommonClassNames.JAVA_LANG_OBJECT) {
                    javac.JAVA_LANG_OBJECT?.let { add(SymbolBasedClassifierType(it.element.asType(), javac)) }
                } else {
                    it?.mapToJavaClassifierType()?.let(this::add)
                }
            }

            tree.implementing?.map { it.mapToJavaClassifierType() }?.filterNotNull()?.let(this::addAll)
        }

    override val innerClasses
        get() = tree.members
                .filterIsInstance(JCTree.JCClassDecl::class.java)
                .map { TreeBasedClass(it, TreePath(treePath, it), javac) }

    override val outerClass
        get() = (treePath.parentPath.leaf as? JCTree.JCClassDecl)?.let { TreeBasedClass(it, treePath.parentPath, javac) }

    override val isInterface
        get() = tree.modifiers.flags and Flags.INTERFACE.toLong() != 0L

    override val isAnnotationType
        get() = tree.modifiers.flags and Flags.ANNOTATION.toLong() != 0L

    override val isEnum
        get() = tree.modifiers.flags and Flags.ENUM.toLong() != 0L

    override val lightClassOriginKind = null

    override val methods
        get() = tree.members
                .filterIsInstance(JCTree.JCMethodDecl::class.java)
                .filter { it.kind == Tree.Kind.METHOD }
                .filter { it.name.toString() != "<init>" }
                .map { TreeBasedMethod(it, TreePath(treePath, it), this, javac) }

    override val fields
        get() = tree.members
                .filterIsInstance(JCTree.JCVariableDecl::class.java)
                .map { TreeBasedField(it, TreePath(treePath, it), this, javac) }

    override val constructors
        get() = tree.members
                .filterIsInstance(JCTree.JCMethodDecl::class.java)
                .filter { TreeInfo.isConstructor(it) }
                .map { TreeBasedConstructor(it, TreePath(treePath, it), this, javac) }

}