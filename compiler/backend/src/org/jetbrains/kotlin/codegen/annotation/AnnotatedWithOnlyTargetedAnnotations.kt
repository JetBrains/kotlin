/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.annotation

import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotatedImpl
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName

interface WrappedAnnotated : Annotated {
    val originalAnnotated: Annotated
}

class AnnotatedWithFakeAnnotations(override val originalAnnotated: Annotated, override val annotations: Annotations) : WrappedAnnotated

class AnnotatedWithOnlyTargetedAnnotations(original: Annotated) : Annotated {
    override val annotations: Annotations = UseSiteTargetedAnnotations(original.annotations)

    private class UseSiteTargetedAnnotations(private val additionalAnnotations: Annotations) : Annotations {
        override fun isEmpty() = true

        override fun findAnnotation(fqName: FqName) = null

        override fun getUseSiteTargetedAnnotations() = getAdditionalTargetedAnnotations()

        override fun getAllAnnotations() = getAdditionalTargetedAnnotations()

        override fun iterator() = emptyList<AnnotationDescriptor>().iterator()

        private fun getAdditionalTargetedAnnotations() = additionalAnnotations.getUseSiteTargetedAnnotations()
    }
}

class AnnotatedSimple(annotations: Annotations) : AnnotatedImpl(annotations)
