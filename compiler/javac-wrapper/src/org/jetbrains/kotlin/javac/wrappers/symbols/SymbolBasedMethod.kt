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
import org.jetbrains.kotlin.load.java.structure.*
import javax.lang.model.element.ExecutableElement

class SymbolBasedMethod(element: ExecutableElement,
                        javac: JavacWrapper) : SymbolBasedMember<ExecutableElement>(element, javac), JavaMethod {

    override val typeParameters: List<JavaTypeParameter>
        get() = element.typeParameters.map { SymbolBasedTypeParameter(it, javac) }

    override val valueParameters: List<JavaValueParameter>
        get() = element.valueParameters(javac)

    override val returnType: JavaType
        get() = SymbolBasedType.create(element.returnType, javac)

    override val hasAnnotationParameterDefaultValue: Boolean
        get() = element.defaultValue != null

}
