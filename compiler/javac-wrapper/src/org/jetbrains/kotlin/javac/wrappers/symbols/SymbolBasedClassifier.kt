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
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.name.FqName
import javax.lang.model.element.Element

abstract class SymbolBasedClassifier<out T : Element>(
        element: T,
        javac: JavacWrapper
) : SymbolBasedElement<T>(element, javac), JavaClassifier, JavaAnnotationOwner {

    override val annotations: Collection<JavaAnnotation>
        get() = element.annotationMirrors.map { SymbolBasedAnnotation(it, javac) }

    override fun findAnnotation(fqName: FqName) = element.findAnnotation(fqName, javac)

    override val isDeprecatedInJavaDoc: Boolean
        get() = javac.isDeprecated(element)

}
