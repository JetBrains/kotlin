/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.types.KotlinType

val ParameterDescriptor.indexOrMinusOne: Int
    get() = if (this is ValueParameterDescriptor) index else -1

val ParameterDescriptor.varargElementType: KotlinType?
    get() = (this as? ValueParameterDescriptor)?.varargElementType

val ParameterDescriptor.isCrossinline: Boolean
    get() = this is ValueParameterDescriptor && isCrossinline

val ParameterDescriptor.isNoinline: Boolean
    get() = this is ValueParameterDescriptor && isNoinline
