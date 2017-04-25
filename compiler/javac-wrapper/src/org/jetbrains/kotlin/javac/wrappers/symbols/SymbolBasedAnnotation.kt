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

import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.name.Name
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.TypeElement

open class SymbolBasedAnnotation(val annotationMirror: AnnotationMirror,
                                 val javac: JavacWrapper) : JavaElement, JavaAnnotation {

    override val arguments
        get() = annotationMirror.elementValues
                .map { SymbolBasedAnnotationArgument.create(it.value.value, Name.identifier(it.key.simpleName.toString()), javac) }

    override val classId
        get() = (annotationMirror.annotationType.asElement() as? TypeElement)?.computeClassId()

    override fun resolve() = SymbolBasedClass(annotationMirror.annotationType.asElement() as TypeElement, javac)

}