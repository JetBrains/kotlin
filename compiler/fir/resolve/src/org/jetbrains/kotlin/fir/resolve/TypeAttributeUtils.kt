/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ParameterNameTypeAttribute
import org.jetbrains.kotlin.fir.types.withAttributes

// See K1 counterpart at org.jetbrains.kotlin.resolve.FunctionDescriptorResolver.removeParameterNameAnnotation
fun ConeKotlinType.removeParameterNameAnnotation(): ConeKotlinType {
    return withAttributes(attributes.remove(ParameterNameTypeAttribute.KEY))
}
