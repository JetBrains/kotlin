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

package org.jetbrains.kotlin.descriptors.annotations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils

public class AnnotationsImpl(private val annotations: List<AnnotationDescriptor>) : Annotations {
    override fun isEmpty() = annotations.isEmpty()

    override fun findAnnotation(fqName: FqName) = annotations.firstOrNull {
        val descriptor = it.getType().getConstructor().getDeclarationDescriptor()
        descriptor is ClassDescriptor && fqName.toUnsafe() == DescriptorUtils.getFqName(descriptor)
    }

    override fun findExternalAnnotation(fqName: FqName) = null

    override fun iterator() = annotations.iterator()

    override fun toString() = annotations.toString()
}
