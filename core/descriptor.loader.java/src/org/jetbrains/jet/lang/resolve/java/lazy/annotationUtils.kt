/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.lazy

import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaAnnotationDescriptor
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationOwner
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.resolver.JavaAnnotationResolver

fun LazyJavaResolverContextWithTypes.resolveAnnotations(javaAnnotations: Collection<JavaAnnotation>): List<AnnotationDescriptor>
        = javaAnnotations.map {jAnnotation -> LazyJavaAnnotationDescriptor(this, jAnnotation)}

private fun GlobalJavaResolverContext.hasAnnotation(owner: JavaAnnotationOwner, annotationFqName: FqName): Boolean
        = owner.findAnnotation(annotationFqName) != null || externalAnnotationResolver.findExternalAnnotation(owner, annotationFqName) != null

fun GlobalJavaResolverContext.hasMutableAnnotation(owner: JavaAnnotationOwner): Boolean = hasAnnotation(owner, JavaAnnotationResolver.JETBRAINS_MUTABLE_ANNOTATION)
fun GlobalJavaResolverContext.hasReadOnlyAnnotation(owner: JavaAnnotationOwner): Boolean = hasAnnotation(owner, JavaAnnotationResolver.JETBRAINS_READONLY_ANNOTATION)
fun GlobalJavaResolverContext.hasNotNullAnnotation(owner: JavaAnnotationOwner): Boolean = hasAnnotation(owner, JavaAnnotationResolver.JETBRAINS_NOT_NULL_ANNOTATION)