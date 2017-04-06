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

package org.jetbrains.kotlin.wrappers.symbols

import com.intellij.psi.CommonClassNames
import org.jetbrains.kotlin.javac.Javac
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.NoType
import javax.lang.model.type.TypeKind

class JavacClass<T : TypeElement>(element: T,
                                  javac: Javac) : JavacClassifier<TypeElement>(element, javac), JavaClass {

    override val name by lazy { SpecialNames.safeIdentifier(element.simpleName.toString()) }

    override val isAbstract
        get() = element.isAbstract

    override val isStatic
        get() = element.isStatic

    override val isFinal
        get() = element.isFinal

    override val visibility by lazy { element.getVisibility() }

    override val typeParameters by lazy {
        element.typeParameters.map { JavacTypeParameter(it, javac) }
    }

    override val fqName = FqName(element.qualifiedName.toString())

    override val supertypes by lazy {
        element.interfaces.toMutableList().apply {
            if (element.superclass !is NoType) add(element.superclass)

            val hasObject = !none { it.toString() == CommonClassNames.JAVA_LANG_OBJECT }
            if (!hasObject && element.toString() != CommonClassNames.JAVA_LANG_OBJECT) {
                javac.JAVA_LANG_OBJECT?.let { add(it.element.asType()) }
            }
        }.map { JavacClassifierType(it, javac) }
    }

    override val innerClasses by lazy {
        element.enclosedElements
                .filter { it.asType().kind == TypeKind.DECLARED }
                .filterIsInstance(TypeElement::class.java)
                .map { JavacClass(it, javac) }
    }

    override val outerClass by lazy {
        element.enclosingElement?.let {
            if (it.asType().kind != TypeKind.DECLARED) null else JavacClass(it as TypeElement, javac)
        }
    }

    override val isInterface
        get() = element.kind == ElementKind.INTERFACE

    override val isAnnotationType
        get() = element.kind == ElementKind.ANNOTATION_TYPE

    override val isEnum
        get() = element.kind == ElementKind.ENUM

    override val lightClassOriginKind = null

    override val methods by lazy {
        element.enclosedElements
                .filter { it.kind == ElementKind.METHOD }
                .map { JavacMethod(it as ExecutableElement, javac) }
    }

    override val fields by lazy {
        element.enclosedElements
                .filter { it.kind.isField }
                .filter { Name.isValidIdentifier(it.simpleName.toString()) }
                .map { JavacField(it as VariableElement, javac) }
    }

    override val constructors by lazy {
        element.enclosedElements
                .filter { it.kind == ElementKind.CONSTRUCTOR }
                .map { JavacConstructor(it as ExecutableElement, javac) }
    }

}
