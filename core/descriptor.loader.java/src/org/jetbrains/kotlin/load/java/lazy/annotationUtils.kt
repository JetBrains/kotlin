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

package org.jetbrains.kotlin.load.java.lazy

import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.lazy.descriptors.resolveAnnotation

class LazyJavaAnnotations(
        c: LazyJavaResolverContext,
        val annotationOwner: JavaAnnotationOwner,
        private val extraLookup: (FqName) -> JavaAnnotation? = { null }
) : Annotations {
    private val annotationDescriptors = c.storageManager.createMemoizedFunctionWithNullableValues {
        annotation: JavaAnnotation ->
        c.resolveAnnotation(annotation)
    }

    override fun findAnnotation(fqName: FqName): AnnotationDescriptor? {
        val jAnnotation = annotationOwner.findAnnotation(fqName) ?: extraLookup(fqName)
        if (jAnnotation == null) return null

        return annotationDescriptors(jAnnotation)
    }

    override fun iterator() = annotationOwner.getAnnotations().stream().map { annotationDescriptors(it) }.filterNotNull().iterator()

    override fun isEmpty() = !iterator().hasNext()
}

fun LazyJavaResolverContext.resolveAnnotations(annotationsOwner: JavaAnnotationOwner): Annotations
        = LazyJavaAnnotations(this, annotationsOwner) { fqName -> externalAnnotationResolver.findExternalAnnotation(annotationsOwner, fqName) }

private fun GlobalJavaResolverContext.hasAnnotation(owner: JavaAnnotationOwner, annotationFqName: FqName): Boolean
        = owner.findAnnotation(annotationFqName) != null || externalAnnotationResolver.findExternalAnnotation(owner, annotationFqName) != null

fun GlobalJavaResolverContext.hasMutableAnnotation(owner: JavaAnnotationOwner): Boolean = hasAnnotation(owner, JvmAnnotationNames.JETBRAINS_MUTABLE_ANNOTATION)
fun GlobalJavaResolverContext.hasReadOnlyAnnotation(owner: JavaAnnotationOwner): Boolean = hasAnnotation(owner, JvmAnnotationNames.JETBRAINS_READONLY_ANNOTATION)
fun GlobalJavaResolverContext.hasNotNullAnnotation(owner: JavaAnnotationOwner): Boolean = hasAnnotation(owner, JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION)
