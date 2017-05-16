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
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import javax.lang.model.element.VariableElement

class JavacValueParameter<out T : VariableElement>(element: T, private val elementName : String,
                                                   override val isVararg : Boolean,
                                                   javac: Javac) : JavacElement<T>(element, javac), JavaValueParameter {

    override val annotations
        get() = element.annotationMirrors.map { JavacAnnotation(it, javac) }

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = element.annotationMirrors
            .filter { it.toString() == fqName.asString() }
            .firstOrNull()
            ?.let {JavacAnnotation(it, javac)}

    override val isDeprecatedInJavaDoc
        get() =  javac.isDeprecated(element)

    override val name
        get() = Name.identifier(elementName)

    override val type
        get() = JavacType.create(element.asType(), javac)

}