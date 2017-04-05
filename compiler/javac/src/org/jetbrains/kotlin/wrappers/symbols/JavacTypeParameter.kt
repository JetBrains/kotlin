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

import org.jetbrains.kotlin.javac.Javac
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.SpecialNames
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.TypeVariable

class JavacTypeParameter<out T : TypeParameterElement>(element: T,
                                                       javac: Javac) : JavacClassifier<T>(element, javac), JavaTypeParameter {

    override val name
        get() = SpecialNames.safeIdentifier(element.simpleName.toString())

    override val upperBounds: Collection<JavaClassifierType>
        get() = listOf(JavacClassifierType((element.asType() as TypeVariable).upperBound, javac))

}