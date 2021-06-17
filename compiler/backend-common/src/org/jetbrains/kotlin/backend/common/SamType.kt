/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.resolve.sam.getAbstractMembers
import org.jetbrains.kotlin.types.KotlinType

class SamType constructor(val type: KotlinType) {

    val classDescriptor: ClassDescriptor
        get() = type.constructor.declarationDescriptor as? ClassDescriptor ?: error("Sam/Fun interface not a class descriptor: $type")

    val kotlinFunctionType: KotlinType
        get() = classDescriptor.defaultFunctionTypeForSamInterface!!

    val originalAbstractMethod: SimpleFunctionDescriptor
        get() = getAbstractMembers(classDescriptor)[0] as SimpleFunctionDescriptor

    override fun equals(other: Any?): Boolean {
        return other is SamType && type == other.type
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String {
        return "SamType($type)"
    }
}

