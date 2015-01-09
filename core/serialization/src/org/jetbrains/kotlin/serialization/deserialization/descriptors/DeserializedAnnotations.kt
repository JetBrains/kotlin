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

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.utils.toReadOnlyList
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils

class DeserializedAnnotations(
        storageManager: StorageManager,
        compute: () -> List<AnnotationDescriptor>
) : Annotations {
    private val annotations = storageManager.createLazyValue { compute().toReadOnlyList() }

    override fun isEmpty(): Boolean = annotations().isEmpty()

    override fun findAnnotation(fqName: FqName): AnnotationDescriptor? = annotations().firstOrNull {
        annotation ->
        val descriptor = annotation.getType().getConstructor().getDeclarationDescriptor()
        descriptor is ClassDescriptor && fqName.equalsTo(DescriptorUtils.getFqName(descriptor))
    }

    override fun iterator(): Iterator<AnnotationDescriptor> = annotations().iterator()
}
