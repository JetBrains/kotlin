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

package org.jetbrains.kotlin.javac.wrappers.symbols

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.CommonClassNames
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.NoType
import javax.lang.model.type.TypeKind
import javax.tools.JavaFileObject

class SymbolBasedClass(element: TypeElement,
                       javac: JavacWrapper,
                       val file: JavaFileObject?) : SymbolBasedClassifier<TypeElement>(element, javac), JavaClass {

    override val name: Name
        get() = Name.identifier(element.simpleName.toString())

    override val isAbstract: Boolean
        get() = element.isAbstract

    override val isStatic: Boolean
        get() = element.isStatic

    override val isFinal: Boolean
        get() = element.isFinal

    override val visibility: Visibility
        get() = element.getVisibility()

    override val typeParameters: List<JavaTypeParameter>
        get() = element.typeParameters.map { SymbolBasedTypeParameter(it, javac) }

    override val fqName: FqName
        get() = FqName(element.qualifiedName.toString())

    override val supertypes: Collection<JavaClassifierType>
        get() = element.interfaces.toMutableList().apply {
            if (element.superclass !is NoType) {
                add(element.superclass)
            } else {
                if (isEmpty() && element.toString() != CommonClassNames.JAVA_LANG_OBJECT) {
                    javac.JAVA_LANG_OBJECT?.let { add(it.element.asType()) }
                }
            }
        }.map { SymbolBasedClassifierType(it, javac) }

    val innerClasses: Map<Name, JavaClass>
        get() = element.enclosedElements
                .filterIsInstance(TypeElement::class.java)
                .map { SymbolBasedClass(it, javac, file) }
                .associateBy(JavaClass::name)

    override val outerClass: JavaClass?
        get() = element.enclosingElement?.let {
            if (it.asType().kind != TypeKind.DECLARED) null else SymbolBasedClass(it as TypeElement, javac, file)
        }

    override val isInterface: Boolean
        get() = element.kind == ElementKind.INTERFACE

    override val isAnnotationType: Boolean
        get() = element.kind == ElementKind.ANNOTATION_TYPE

    override val isEnum: Boolean
        get() = element.kind == ElementKind.ENUM

    override val lightClassOriginKind: LightClassOriginKind?
        get() = null

    override val methods: Collection<JavaMethod>
        get() = element.enclosedElements
                .filter { it.kind == ElementKind.METHOD }
                .map { SymbolBasedMethod(it as ExecutableElement, javac) }

    override val fields: Collection<JavaField>
        get() = element.enclosedElements
                .filter { it.kind.isField && Name.isValidIdentifier(it.simpleName.toString()) }
                .map { SymbolBasedField(it as VariableElement, javac) }

    override val constructors: Collection<JavaConstructor>
        get() = element.enclosedElements
                .filter { it.kind == ElementKind.CONSTRUCTOR }
                .map { SymbolBasedConstructor(it as ExecutableElement, javac) }

    override val innerClassNames: Collection<Name>
        get() = innerClasses.keys

    val virtualFile: VirtualFile? by lazy {
        file?.let { javac.toVirtualFile(it) }
    }

    override fun findInnerClass(name: Name) = innerClasses[name]

}
