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

import com.sun.source.util.TreePath
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class TreeBasedAnnotation(
        val annotation: JCTree.JCAnnotation,
        val treePath: TreePath,
        val javac: JavacWrapper
) : JavaElement, JavaAnnotation {

    override val arguments: Collection<JavaAnnotationArgument>
        get() = createAnnotationArguments(this, javac)

    override val classId: ClassId?
        get() = resolve()?.computeClassId() ?: ClassId.topLevel(FqName(annotation.annotationType.toString().substringAfter("@")))

    override fun resolve() =
            javac.resolve(TreePath.getPath(treePath.compilationUnit, annotation.annotationType)) as? JavaClass

}

sealed class TreeBasedAnnotationArgument(override val name: Name,
                                         val javac: JavacWrapper) : JavaAnnotationArgument, JavaElement

class TreeBasedLiteralAnnotationArgument(name: Name,
                                         override val value: Any?,
                                         javac: JavacWrapper) : TreeBasedAnnotationArgument(name, javac), JavaLiteralAnnotationArgument

class TreeBasedReferenceAnnotationArgument(name: Name,
                                           val treePath: TreePath,
                                           val field: JCTree.JCFieldAccess,
                                           javac: JavacWrapper) : TreeBasedAnnotationArgument(name, javac), JavaEnumValueAnnotationArgument {

    override fun resolve(): JavaField? {
        val newTreePath = javac.getTreePath(field.selected, treePath.compilationUnit)
        val javaClass = javac.resolve(newTreePath) as? JavaClass ?: return null
        val fieldName = field.name.toString().let { Name.identifier(it) }

        return javaClass.fields.find { it.name == fieldName }
    }

}

class TreeBasedArrayAnnotationArgument(val args: List<JavaAnnotationArgument>,
                                       name: Name,
                                       javac: JavacWrapper): TreeBasedAnnotationArgument(name, javac), JavaArrayAnnotationArgument {
    override fun getElements() = args

}

private fun createAnnotationArguments(annotation: TreeBasedAnnotation,
                                      javac: JavacWrapper): Collection<JavaAnnotationArgument> {
    val arguments = annotation.annotation.arguments
    val javaClass = annotation.resolve() ?: return emptyList()
    val methods = javaClass.methods

    if (arguments.size != methods.size) return emptyList()

    return methods.mapIndexedNotNull { index, it ->
        createAnnotationArgument(arguments[index], it.name, annotation.treePath, javac)
    }
}

private fun createAnnotationArgument(argument: JCTree.JCExpression,
                                     name: Name,
                                     treePath: TreePath,
                                     javac: JavacWrapper): JavaAnnotationArgument? =
        when (argument) {
            is JCTree.JCLiteral -> TreeBasedLiteralAnnotationArgument(name, argument.value, javac)
            is JCTree.JCFieldAccess -> TreeBasedReferenceAnnotationArgument(name, treePath, argument, javac)
            is JCTree.JCNewArray -> arrayAnnotationArguments(argument.elems, name, treePath, javac)
            else -> null
        }

private fun arrayAnnotationArguments(values: List<JCTree.JCExpression>,
                                     name: Name,
                                     treePath: TreePath,
                                     javac: JavacWrapper): JavaArrayAnnotationArgument =
        values.mapNotNull {
            if (it is JCTree.JCNewArray) {
                arrayAnnotationArguments(it.elems, name, treePath, javac)
            }
            else {
                createAnnotationArgument(it, name, treePath, javac)
            }
        }.let { TreeBasedArrayAnnotationArgument(it, name, javac) }