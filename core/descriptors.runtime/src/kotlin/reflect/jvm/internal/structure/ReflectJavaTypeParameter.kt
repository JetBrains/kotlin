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

package kotlin.reflect.jvm.internal.structure

import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.Name
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.TypeVariable

class ReflectJavaTypeParameter(
    val typeVariable: TypeVariable<*>
) : ReflectJavaElement(), JavaTypeParameter, ReflectJavaAnnotationOwner {
    override val upperBounds: List<ReflectJavaClassifierType>
        get() {
            val bounds = typeVariable.bounds.map(::ReflectJavaClassifierType)
            if (bounds.singleOrNull()?.reflectType == Any::class.java) return emptyList()
            return bounds
        }

    override val element: AnnotatedElement?
        // TypeVariable is AnnotatedElement only in JDK8
        get() = typeVariable as? AnnotatedElement

    override val name: Name
        get() = Name.identifier(typeVariable.name)

    override fun equals(other: Any?) = other is ReflectJavaTypeParameter && typeVariable == other.typeVariable

    override fun hashCode() = typeVariable.hashCode()

    override fun toString() = this::class.java.name + ": " + typeVariable
}
