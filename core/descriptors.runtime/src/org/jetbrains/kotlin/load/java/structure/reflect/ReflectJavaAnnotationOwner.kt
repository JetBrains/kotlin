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

import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner
import org.jetbrains.kotlin.name.FqName
import java.lang.reflect.AnnotatedElement

public trait ReflectJavaAnnotationOwner : JavaAnnotationOwner {
    val element: AnnotatedElement

    override fun getAnnotations() = getAnnotations(element.getDeclaredAnnotations())

    override fun findAnnotation(fqName: FqName) = findAnnotation(element.getDeclaredAnnotations(), fqName)
}

fun getAnnotations(annotations: Array<Annotation>): List<ReflectJavaAnnotation> {
    return annotations.map { ReflectJavaAnnotation(it) }
}

fun findAnnotation(annotations: Array<Annotation>, fqName: FqName): ReflectJavaAnnotation? {
    return annotations.firstOrNull { it.annotationType().fqName == fqName }?.let { ReflectJavaAnnotation(it) }
}
