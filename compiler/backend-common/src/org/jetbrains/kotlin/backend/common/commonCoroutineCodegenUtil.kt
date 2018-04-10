/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.config.coroutinesIntrinsicsPackageFqName
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.config.LanguageVersionSettings

val SUSPEND_COROUTINE_OR_RETURN_NAME = Name.identifier("suspendCoroutineOrReturn")
val INTERCEPTED_NAME = Name.identifier("intercepted")
val COROUTINE_SUSPENDED_NAME = Name.identifier("COROUTINE_SUSPENDED")

val SUSPEND_COROUTINE_UNINTERCEPTED_OR_RETURN_NAME = Name.identifier("suspendCoroutineUninterceptedOrReturn")

fun FunctionDescriptor.isBuiltInIntercepted(languageVersionSettings: LanguageVersionSettings): Boolean {
    if (name != INTERCEPTED_NAME) return false
    val original =
        module.getPackage(languageVersionSettings.coroutinesIntrinsicsPackageFqName()).memberScope
            .getContributedFunctions(INTERCEPTED_NAME, NoLookupLocation.FROM_BACKEND)
            .singleOrNull() as CallableDescriptor
    return DescriptorEquivalenceForOverrides.areEquivalent(original, this)
}

fun FunctionDescriptor.isBuiltInSuspendCoroutineOrReturn(languageVersionSettings: LanguageVersionSettings): Boolean {
    if (name != SUSPEND_COROUTINE_OR_RETURN_NAME) return false

    val originalDeclaration = getBuiltInSuspendCoroutineOrReturn(languageVersionSettings) ?: return false

    return DescriptorEquivalenceForOverrides.areEquivalent(
        originalDeclaration, this
    )
}

fun FunctionDescriptor.getBuiltInSuspendCoroutineOrReturn(languageVersionSettings: LanguageVersionSettings) =
    module.getPackage(languageVersionSettings.coroutinesIntrinsicsPackageFqName()).memberScope
        .getContributedFunctions(SUSPEND_COROUTINE_OR_RETURN_NAME, NoLookupLocation.FROM_BACKEND)
        .singleOrNull()

fun FunctionDescriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn(languageVersionSettings: LanguageVersionSettings): Boolean {
    if (name != SUSPEND_COROUTINE_UNINTERCEPTED_OR_RETURN_NAME) return false
    val original = module.getPackage(languageVersionSettings.coroutinesIntrinsicsPackageFqName()).memberScope
        .getContributedFunctions(SUSPEND_COROUTINE_UNINTERCEPTED_OR_RETURN_NAME, NoLookupLocation.FROM_BACKEND)
        .singleOrNull() as CallableDescriptor
    return DescriptorEquivalenceForOverrides.areEquivalent(original, this)
}
