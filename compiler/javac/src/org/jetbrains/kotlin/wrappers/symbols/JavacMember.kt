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
import org.jetbrains.kotlin.load.java.structure.JavaMember
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

abstract class JavacMember<out T : Element>(element: T,
                                            javac: Javac) : JavacElement<T>(element, javac), JavaMember {

    override val containingClass by lazy {
        JavacClass((element.enclosingElement as TypeElement), javac)
    }

    override val annotations by lazy {
        element.annotationMirrors
                .map { JavacAnnotation(it, javac) }
    }

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = element.annotationMirrors
            .filter { it.toString() == fqName.asString() }
            .firstOrNull()
            ?.let { JavacAnnotation(it, javac) }

    override val visibility by lazy {
        element.getVisibility()
    }

    override val name = Name.identifier(element.simpleName.toString())

    override val isDeprecatedInJavaDoc by lazy { javac.isDeprecated(element) }

    override val isAbstract
        get() = element.isAbstract

    override val isStatic
        get() = element.isStatic

    override val isFinal
        get() = element.isFinal

}

fun ExecutableElement.valueParameters(javac: Javac) = let {
    val parameterTypesCount = parameters.size

    parameters.mapIndexed { index, it ->
        val isLastParameter = index == parameterTypesCount - 1
        val parameterName = it.simpleName.toString()
        JavacValueParameter(it, parameterName, if (isLastParameter) isVarArgs else false, javac)
    }
}