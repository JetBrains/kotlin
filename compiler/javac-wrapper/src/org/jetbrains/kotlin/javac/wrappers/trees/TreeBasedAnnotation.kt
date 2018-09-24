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

import com.sun.source.tree.CompilationUnitTree
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.JavaClassWithClassId
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.javac.resolve.ConstantEvaluator
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class TreeBasedAnnotation(
        val annotation: JCTree.JCAnnotation,
        val compilationUnit: CompilationUnitTree,
        val javac: JavacWrapper,
        val onElement: JavaElement
) : JavaElement, JavaAnnotation {

    override val arguments: Collection<JavaAnnotationArgument>
        get() = createAnnotationArguments(this, javac, onElement)

    override val classId: ClassId?
        get() = (resolve() as? JavaClassWithClassId)?.classId ?: ClassId.topLevel(FqName(annotation.annotationType.toString().substringAfter("@")))

    override fun resolve() = javac.resolve(annotation.annotationType, compilationUnit, onElement) as? JavaClass

}

sealed class TreeBasedAnnotationArgument(override val name: Name,
                                         val javac: JavacWrapper) : JavaAnnotationArgument, JavaElement

class TreeBasedLiteralAnnotationArgument(name: Name,
                                         override val value: Any?,
                                         javac: JavacWrapper) : TreeBasedAnnotationArgument(name, javac), JavaLiteralAnnotationArgument

class TreeBasedReferenceAnnotationArgument(
        name: Name,
        private val compilationUnit: CompilationUnitTree,
        private val field: JCTree.JCFieldAccess,
        javac: JavacWrapper,
        private val onElement: JavaElement
) : TreeBasedAnnotationArgument(name, javac), JavaEnumValueAnnotationArgument {
    // TODO: do not run resolve here
    private val javaField: JavaField? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val javaClass = javac.resolve(field.selected, compilationUnit, onElement) as? JavaClass
        val fieldName = Name.identifier(field.name.toString())

        javaClass?.fields?.find { it.name == fieldName }
    }

    override val enumClassId: ClassId?
        get() = javaField?.containingClass?.classId

    override val entryName: Name?
        get() = javaField?.name
}

class TreeBasedArrayAnnotationArgument(val args: List<JavaAnnotationArgument>,
                                       name: Name,
                                       javac: JavacWrapper) : TreeBasedAnnotationArgument(name, javac), JavaArrayAnnotationArgument {
    override fun getElements() = args

}

class TreeBasedJavaClassObjectAnnotationArgument(private val type: JCTree.JCExpression,
                                                 name: Name,
                                                 private val compilationUnit: CompilationUnitTree,
                                                 javac: JavacWrapper,
                                                 private val onElement: JavaElement) : TreeBasedAnnotationArgument(name, javac), JavaClassObjectAnnotationArgument {

    override fun getReferencedType(): JavaType =
            TreeBasedType.create(type, compilationUnit, javac, emptyList(), onElement)

}

class TreeBasedAnnotationAsAnnotationArgument(private val annotation: JCTree.JCAnnotation,
                                              name: Name,
                                              private val compilationUnit: CompilationUnitTree,
                                              javac: JavacWrapper,
                                              private val onElement: JavaElement) : TreeBasedAnnotationArgument(name, javac), JavaAnnotationAsAnnotationArgument {
    override fun getAnnotation(): JavaAnnotation =
            TreeBasedAnnotation(annotation, compilationUnit, javac, onElement)

}

private fun createAnnotationArguments(annotation: TreeBasedAnnotation,
                                      javac: JavacWrapper,
                                      onElement: JavaElement): Collection<JavaAnnotationArgument> =
        annotation.annotation.arguments.mapNotNull {
            val name = if (it is JCTree.JCAssign) Name.identifier(it.lhs.toString()) else Name.identifier("value")
            createAnnotationArgument(it, name, annotation.compilationUnit, javac, annotation, onElement)
        }

private fun createAnnotationArgument(argument: JCTree.JCExpression,
                                     name: Name,
                                     compilationUnit: CompilationUnitTree,
                                     javac: JavacWrapper,
                                     annotation: TreeBasedAnnotation,
                                     onElement: JavaElement): JavaAnnotationArgument? =
        when (argument) {
            is JCTree.JCLiteral -> TreeBasedLiteralAnnotationArgument(name, argument.value, javac)
            is JCTree.JCFieldAccess -> {
                if (argument.name.contentEquals("class")) {
                    TreeBasedJavaClassObjectAnnotationArgument(argument.selected, name, compilationUnit, javac, onElement)
                }
                else {
                    TreeBasedReferenceAnnotationArgument(name, compilationUnit, argument, javac, onElement)
                }
            }
            is JCTree.JCAssign -> createAnnotationArgument(argument.rhs, name, compilationUnit, javac, annotation, onElement)
            is JCTree.JCNewArray -> TreeBasedArrayAnnotationArgument(argument.elems.mapNotNull { createAnnotationArgument(it, name, compilationUnit, javac, annotation, onElement) }, name, javac)
            is JCTree.JCAnnotation -> TreeBasedAnnotationAsAnnotationArgument(argument, name, compilationUnit, javac, onElement)
            is JCTree.JCParens -> createAnnotationArgument(argument.expr, name, compilationUnit, javac, annotation, onElement)
            is JCTree.JCBinary -> resolveArgumentValue(argument, annotation, name, compilationUnit, javac)
            is JCTree.JCUnary -> resolveArgumentValue(argument, annotation, name, compilationUnit, javac)
            else -> throw UnsupportedOperationException("Unknown annotation argument $argument")
        }

private fun resolveArgumentValue(argument: JCTree.JCExpression,
                                 annotation: TreeBasedAnnotation,
                                 name: Name,
                                 compilationUnit: CompilationUnitTree,
                                 javac: JavacWrapper): JavaAnnotationArgument? {
    val containingAnnotation = annotation.resolve() ?: return null
    val evaluator = ConstantEvaluator(containingAnnotation, javac, compilationUnit)

    return evaluator.getValue(argument)?.let { TreeBasedLiteralAnnotationArgument(name, it, javac) }
}