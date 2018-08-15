/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

fun PropertyDescriptor.hasBackingField(bindingContext: BindingContext?): Boolean = when {
    kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> overriddenDescriptors.any { it.hasBackingField(bindingContext) }
    source is KotlinSourceElement && bindingContext != null -> bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, this) ?: false
    getter != null -> false
    else -> true
}
