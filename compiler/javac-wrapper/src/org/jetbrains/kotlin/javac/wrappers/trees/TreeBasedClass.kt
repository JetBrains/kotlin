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
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeInfo
import org.jetbrains.kotlin.descriptors.Visibilities.PUBLIC
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.javac.wrappers.symbols.SymbolBasedClass
import org.jetbrains.kotlin.javac.wrappers.symbols.SymbolBasedClassifierType
import org.jetbrains.kotlin.javac.wrappers.symbols.SymbolBasedType
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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
            .filterIsInstance<JCTree.JCClassDecl>()
            .joinToString(separator = ".",
                          prefix = "${treePath.compilationUnit.packageName}.",
                          transform = JCTree.JCClassDecl::name)
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

            tree.implementing?.map { it.mapToJavaClassifierType() }?.filterNotNull()?.let(this::addAll)
            tree.extending?.let { it.mapToJavaClassifierType()?.let(this::add) }

            if (isEmpty()) {
                javac.JAVA_LANG_OBJECT?.let { add(SymbolBasedClassifierType(it.element.asType(), javac)) }
            }
        }

    val innerClasses by lazy {
        tree.members
                .filterIsInstance(JCTree.JCClassDecl::class.java)
                .map { TreeBasedClass(it, TreePath(treePath, it), javac) }
    }

    override val outerClass by lazy {
        (treePath.parentPath.leaf as? JCTree.JCClassDecl)?.let { TreeBasedClass(it, treePath.parentPath, javac) }
    }

    override val isInterface
        get() = tree.modifiers.flags and Flags.INTERFACE.toLong() != 0L

    override val isAnnotationType
        get() = tree.modifiers.flags and Flags.ANNOTATION.toLong() != 0L

    override val isEnum
        get() = tree.modifiers.flags and Flags.ENUM.toLong() != 0L

    override val lightClassOriginKind = null

    override val methods
        get() = tree.members
                .filter { it.kind == Tree.Kind.METHOD && !TreeInfo.isConstructor(it) }
                .map { TreeBasedMethod(it as JCTree.JCMethodDecl, TreePath(treePath, it), this, javac) }

    override val fields
        get() = tree.members
                .filterIsInstance(JCTree.JCVariableDecl::class.java)
                .map { TreeBasedField(it, TreePath(treePath, it), this, javac) }

    override val constructors
        get() = tree.members
                .filter { TreeInfo.isConstructor(it) }
                .map { TreeBasedConstructor(it as JCTree.JCMethodDecl, TreePath(treePath, it), this, javac) }

    override val innerClassNames
        get() = innerClasses.map(TreeBasedClass<JCTree.JCClassDecl>::name)

    override fun findInnerClass(name: Name) = innerClasses.find { it.name == name }

}