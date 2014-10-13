/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.annotations

import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils

public fun DeclarationDescriptor.hasInlineAnnotation(): Boolean {
    return getAnnotations().findAnnotation(FqName("kotlin.inline")) != null
}

public fun DeclarationDescriptor.hasPlatformStaticAnnotation(): Boolean {
    return getAnnotations().findAnnotation(FqName("kotlin.platform.platformStatic")) != null
}

public fun CallableDescriptor.isPlatformStaticInObject(): Boolean =
        DescriptorUtils.isObject(getContainingDeclaration()) && hasPlatformStaticAnnotation()

public fun CallableDescriptor.isPlatformStaticInClassObject(): Boolean =
        DescriptorUtils.isClassObject(getContainingDeclaration()) && hasPlatformStaticAnnotation()