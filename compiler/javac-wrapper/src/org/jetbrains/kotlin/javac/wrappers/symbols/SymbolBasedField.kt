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
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaType
import javax.lang.model.element.ElementKind
import javax.lang.model.element.VariableElement

class SymbolBasedField(
        element: VariableElement,
        containingClass: JavaClass,
        javac: JavacWrapper
) : SymbolBasedMember<VariableElement>(element, containingClass, javac), JavaField {

    override val isEnumEntry: Boolean
        get() = element.kind == ElementKind.ENUM_CONSTANT

    override val type: JavaType
        get() = SymbolBasedType.create(element.asType(), javac)

    override val initializerValue: Any?
        by lazy { element.constantValue }

    override val hasConstantNotNullInitializer: Boolean
        get() = initializerValue != null

}