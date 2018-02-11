/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor

fun ClassDescriptor.underlyingRepresentation(): ValueParameterDescriptor? {
    if (!isInline) return null
    return unsubstitutedPrimaryConstructor?.valueParameters?.singleOrNull()
}

fun DeclarationDescriptor.isInlineClass() = this is ClassDescriptor && this.isInline