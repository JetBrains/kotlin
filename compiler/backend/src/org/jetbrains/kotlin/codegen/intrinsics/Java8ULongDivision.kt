/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor

abstract class UnsignedLongDivisionIntrinsic : IntrinsicMethod() {
    override fun isApplicableToOverload(descriptor: CallableMemberDescriptor): Boolean {
        if (descriptor.containingDeclaration is PackageFragmentDescriptor) return true
        val singleValueParameterTypeDescriptor = descriptor.valueParameters.single().type.constructor.declarationDescriptor
            ?: throw AssertionError("Unexpected descriptor for unsigned long division intrinsic: $descriptor")
        return singleValueParameterTypeDescriptor.name.asString() == "ULong"
    }
}


class Java8ULongDivide : UnsignedLongDivisionIntrinsic() {
    override fun toCallable(method: CallableMethod): Callable =
        createIntrinsicCallable(method) {
            it.invokestatic("java/lang/Long", "divideUnsigned", "(JJ)J", false)
        }
}


class Java8ULongRemainder : UnsignedLongDivisionIntrinsic() {
    override fun toCallable(method: CallableMethod): Callable =
        createIntrinsicCallable(method) {
            it.invokestatic("java/lang/Long", "remainderUnsigned", "(JJ)J", false)
        }
}

