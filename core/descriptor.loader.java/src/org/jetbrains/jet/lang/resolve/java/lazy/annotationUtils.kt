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

fun LazyJavaResolverContextWithTypes.resolveAnnotations(javaAnnotations: Collection<JavaAnnotation>): List<AnnotationDescriptor>
        = javaAnnotations.map {jAnnotation -> LazyJavaAnnotationDescriptor(this, jAnnotation)}

// TODO: These methods are a workaround for inability to refactor List<AnnotationDescriptor> -> Annotations at the moment
fun JavaAnnotationOwner.hasMutableAnnotation(): Boolean = false
fun JavaAnnotationOwner.hasReadOnlyAnnotation(): Boolean = false
fun JavaAnnotationOwner.hasNotNullAnnotation(): Boolean = false