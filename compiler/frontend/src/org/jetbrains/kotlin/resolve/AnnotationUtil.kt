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

package org.jetbrains.kotlin.resolve.annotations

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

public fun DeclarationDescriptor.hasInlineAnnotation(): Boolean {
    return getAnnotations().findAnnotation(FqName("kotlin.inline")) != null
}

public fun DeclarationDescriptor.hasPlatformStaticAnnotation(): Boolean {
    return getAnnotations().findAnnotation(FqName("kotlin.platform.platformStatic")) != null
}

public fun DeclarationDescriptor.findPublicFieldAnnotation(): AnnotationDescriptor? {
    return getAnnotations().findAnnotation(FqName("kotlin.jvm.publicField"))
}

public fun DeclarationDescriptor.hasIntrinsicAnnotation(): Boolean {
    return getAnnotations().findAnnotation(FqName("kotlin.jvm.internal.Intrinsic")) != null
}

public fun CallableDescriptor.isPlatformStaticInObjectOrClass(): Boolean =
        isPlatformStaticIn { DescriptorUtils.isNonCompanionObject(it) || DescriptorUtils.isClass(it) || DescriptorUtils.isEnumClass(it) }

public fun CallableDescriptor.isPlatformStaticInCompanionObject(): Boolean =
        isPlatformStaticIn { DescriptorUtils.isCompanionObject(it) }

private fun CallableDescriptor.isPlatformStaticIn(predicate: (DeclarationDescriptor) -> Boolean): Boolean =
        when (this) {
            is PropertyAccessorDescriptor -> {
                val propertyDescriptor = getCorrespondingProperty()
                predicate(propertyDescriptor.getContainingDeclaration()!!) &&
                (hasPlatformStaticAnnotation() || propertyDescriptor.hasPlatformStaticAnnotation())
            }
            else -> predicate(getContainingDeclaration()) && hasPlatformStaticAnnotation()
        }

public fun AnnotationDescriptor.argumentValue(parameterName: String): Any? {
    return getAllValueArguments().entrySet()
            .singleOrNull { it.key.getName().asString() == parameterName }
            ?.value?.getValue()
}
