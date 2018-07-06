/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.coroutinesIntrinsicsPackageFqName
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.Name

val COROUTINE_SUSPENDED_NAME = Name.identifier("COROUTINE_SUSPENDED")

fun FunctionDescriptor.isBuiltInIntercepted(languageVersionSettings: LanguageVersionSettings): Boolean =
    isTopLevelInPackage("intercepted", languageVersionSettings.coroutinesIntrinsicsPackageFqName().asString())

fun FunctionDescriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn(languageVersionSettings: LanguageVersionSettings): Boolean =
    isTopLevelInPackage(
        "suspendCoroutineUninterceptedOrReturn",
        languageVersionSettings.coroutinesIntrinsicsPackageFqName().asString()
    )
