/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.structure.reflect

import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationArgument
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class ReflectJavaAnnotation(val annotation: Annotation) : ReflectJavaElement(), JavaAnnotation {
    override val arguments: Collection<JavaAnnotationArgument>
        get() = annotation.annotationClass.java.declaredMethods.map { method ->
            ReflectJavaAnnotationArgument.create(method.invoke(annotation), Name.identifier(method.name))
        }

    override val classId: ClassId
        get() = annotation.annotationClass.java.classId

    override fun resolve() = ReflectJavaClass(annotation.annotationClass.java)

    override fun equals(other: Any?) = other is ReflectJavaAnnotation && annotation == other.annotation

    override fun hashCode() = annotation.hashCode()

    override fun toString() = this::class.java.name + ": " + annotation
}
