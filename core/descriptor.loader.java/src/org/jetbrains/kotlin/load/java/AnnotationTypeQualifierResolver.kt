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

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.resolveClassByFqName
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

private val TYPE_QUALIFIER_NICKNAME_FQNAME = FqName("javax.annotation.meta.TypeQualifierNickname")
private val TYPE_QUALIFIER_FQNAME = FqName("javax.annotation.meta.TypeQualifier")

class AnnotationTypeQualifierResolver(storageManager: StorageManager) {
    private val resolvedNicknames =
            storageManager.createMemoizedFunctionWithNullableValues(this::computeTypeQualifierNickname)

    private fun computeTypeQualifierNickname(classDescriptor: ClassDescriptor): AnnotationDescriptor? {
        if (!classDescriptor.annotations.hasAnnotation(TYPE_QUALIFIER_NICKNAME_FQNAME)) return null

        return classDescriptor.annotations.firstNotNullResult(this::resolveTypeQualifierAnnotation)
    }

    private fun resolveTypeQualifierNickname(classDescriptor: ClassDescriptor): AnnotationDescriptor? {
        if (classDescriptor.kind != ClassKind.ANNOTATION_CLASS) return null

        return resolvedNicknames(classDescriptor)
    }

    fun resolveTypeQualifierAnnotation(annotationDescriptor: AnnotationDescriptor): AnnotationDescriptor? {
        val annotationClass = annotationDescriptor.annotationClass ?: return null
        if (annotationClass.isAnnotatedWithTypeQualifier) return annotationDescriptor

        return resolveTypeQualifierNickname(annotationClass)
    }

    fun isTypeQualifier(moduleDescriptor: ModuleDescriptor, classFqName: FqName): Boolean {
        val classDescriptor = moduleDescriptor.resolveClassByFqName(
                classFqName, NoLookupLocation.FROM_JAVA_LOADER
        ) ?: return false

        if (classDescriptor.isTypeQualifierAnnotation) return true

        return resolveTypeQualifierNickname(classDescriptor) != null
    }
}

private val ClassDescriptor.isAnnotatedWithTypeQualifier: Boolean
    get() = annotations.hasAnnotation(TYPE_QUALIFIER_FQNAME)
