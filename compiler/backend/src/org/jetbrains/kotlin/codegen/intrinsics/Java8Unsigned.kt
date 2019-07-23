/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor

abstract class UnsignedIntrinsic(private val targetDescriptor: String) : IntrinsicMethod() {
    override fun isApplicableToOverload(descriptor: CallableMemberDescriptor): Boolean {
        if (descriptor.containingDeclaration is PackageFragmentDescriptor) return true
        val valueParameter = descriptor.valueParameters.singleOrNull() ?: return true
        val singleValueParameterTypeDescriptor = valueParameter.type.constructor.declarationDescriptor
            ?: throw AssertionError("Unexpected descriptor for unsigned intrinsic: $descriptor")
        return singleValueParameterTypeDescriptor.name.asString() == targetDescriptor
    }
}

class Java8UIntDivide : UnsignedIntrinsic("UInt") {
    override fun toCallable(method: CallableMethod): Callable =
        createIntrinsicCallable(method) {
            it.invokestatic("java/lang/Integer", "divideUnsigned", "(II)I", false)
        }
}

class Java8UIntRemainder : UnsignedIntrinsic("UInt") {
    override fun toCallable(method: CallableMethod): Callable =
        createIntrinsicCallable(method) {
            it.invokestatic("java/lang/Integer", "remainderUnsigned", "(II)I", false)
        }
}

class Java8UIntCompare : UnsignedIntrinsic("UInt") {
    override fun toCallable(method: CallableMethod): Callable =
        createIntrinsicCallable(method) {
            it.invokestatic("java/lang/Integer", "compareUnsigned", "(II)I", false)
        }
}

class Java8UIntToString : UnsignedIntrinsic("UInt") {
    override fun toCallable(method: CallableMethod): Callable =
        createIntrinsicCallable(method) {
            it.invokestatic("java/lang/Integer", "toUnsignedString", "(I)Ljava/lang/String;", false)
        }
}

class Java8ULongDivide : UnsignedIntrinsic("ULong") {
    override fun toCallable(method: CallableMethod): Callable =
        createIntrinsicCallable(method) {
            it.invokestatic("java/lang/Long", "divideUnsigned", "(JJ)J", false)
        }
}

class Java8ULongRemainder : UnsignedIntrinsic("ULong") {
    override fun toCallable(method: CallableMethod): Callable =
        createIntrinsicCallable(method) {
            it.invokestatic("java/lang/Long", "remainderUnsigned", "(JJ)J", false)
        }
}

class Java8ULongCompare : UnsignedIntrinsic("ULong") {
    override fun toCallable(method: CallableMethod): Callable =
        createIntrinsicCallable(method) {
            it.invokestatic("java/lang/Long", "compareUnsigned", "(JJ)I", false)
        }
}

class Java8ULongToString : UnsignedIntrinsic("ULong") {
    override fun toCallable(method: CallableMethod): Callable =
        createIntrinsicCallable(method) {
            it.invokestatic("java/lang/Long", "toUnsignedString", "(J)Ljava/lang/String;", false)
        }
}
