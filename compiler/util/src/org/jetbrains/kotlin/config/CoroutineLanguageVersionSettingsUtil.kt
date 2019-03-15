/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

fun LanguageVersionSettings.coroutinesPackageFqName(): FqName {
    return coroutinesPackageFqName(isReleaseCoroutines())
}

fun LanguageVersionSettings.isReleaseCoroutines() = supportsFeature(LanguageFeature.ReleaseCoroutines)

private fun coroutinesPackageFqName(isReleaseCoroutines: Boolean): FqName {
    return if (isReleaseCoroutines)
        DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_RELEASE
    else
        DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_EXPERIMENTAL
}

fun LanguageVersionSettings.coroutinesIntrinsicsPackageFqName() =
    coroutinesPackageFqName().child(Name.identifier("intrinsics"))

fun LanguageVersionSettings.continuationInterfaceFqName() =
    coroutinesPackageFqName().child(Name.identifier("Continuation"))

fun LanguageVersionSettings.restrictsSuspensionFqName() =
    coroutinesPackageFqName().child(Name.identifier("RestrictsSuspension"))

fun FqName.isBuiltInCoroutineContext(languageVersionSettings: LanguageVersionSettings) =
    if (languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines))
        this == DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_RELEASE.child(Name.identifier("coroutineContext"))
    else
        this == DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_EXPERIMENTAL.child(Name.identifier("coroutineContext")) ||
                this == DescriptorUtils.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME_EXPERIMENTAL.child(Name.identifier("coroutineContext"))
