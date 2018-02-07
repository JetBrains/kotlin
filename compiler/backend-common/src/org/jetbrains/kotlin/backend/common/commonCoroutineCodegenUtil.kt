/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides
import org.jetbrains.kotlin.resolve.DescriptorUtils.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.resolve.calls.checkers.COROUTINE_CONTEXT_1_2_20_FQ_NAME
import org.jetbrains.kotlin.resolve.calls.checkers.COROUTINE_CONTEXT_FQ_NAME
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

val SUSPEND_COROUTINE_OR_RETURN_NAME = Name.identifier("suspendCoroutineOrReturn")
val INTERCEPTED_NAME = Name.identifier("intercepted")
val COROUTINE_SUSPENDED_NAME = Name.identifier("COROUTINE_SUSPENDED")

val SUSPEND_COROUTINE_UNINTERCEPTED_OR_RETURN_NAME = Name.identifier("suspendCoroutineUninterceptedOrReturn")

fun FunctionDescriptor.isBuiltInIntercepted(): Boolean {
    if (name != INTERCEPTED_NAME) return false
    val original =
        module.getPackage(COROUTINES_INTRINSICS_PACKAGE_FQ_NAME).memberScope
            .getContributedFunctions(INTERCEPTED_NAME, NoLookupLocation.FROM_BACKEND)
            .singleOrNull() as CallableDescriptor
    return DescriptorEquivalenceForOverrides.areEquivalent(original, this)
}

fun FunctionDescriptor.isBuiltInSuspendCoroutineOrReturn(): Boolean {
    if (name != SUSPEND_COROUTINE_OR_RETURN_NAME) return false

    val originalDeclaration = getBuiltInSuspendCoroutineOrReturn() ?: return false

    return DescriptorEquivalenceForOverrides.areEquivalent(
        originalDeclaration, this
    )
}

fun FunctionDescriptor.getBuiltInSuspendCoroutineOrReturn() =
    module.getPackage(COROUTINES_INTRINSICS_PACKAGE_FQ_NAME).memberScope
        .getContributedFunctions(SUSPEND_COROUTINE_OR_RETURN_NAME, NoLookupLocation.FROM_BACKEND)
        .singleOrNull()

fun FunctionDescriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn(): Boolean {
    if (name != SUSPEND_COROUTINE_UNINTERCEPTED_OR_RETURN_NAME) return false
    val original = module.getPackage(COROUTINES_INTRINSICS_PACKAGE_FQ_NAME).memberScope
        .getContributedFunctions(SUSPEND_COROUTINE_UNINTERCEPTED_OR_RETURN_NAME, NoLookupLocation.FROM_BACKEND)
        .singleOrNull() as CallableDescriptor
    return DescriptorEquivalenceForOverrides.areEquivalent(original, this)
}
