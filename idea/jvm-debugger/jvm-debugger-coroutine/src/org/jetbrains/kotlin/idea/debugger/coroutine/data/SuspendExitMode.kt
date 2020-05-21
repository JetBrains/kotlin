/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.data;

enum class SuspendExitMode {
    SUSPEND_LAMBDA, SUSPEND_METHOD_PARAMETER, SUSPEND_METHOD, UNKNOWN, NONE;

    fun isCoroutineFound() =
        this == SUSPEND_LAMBDA || this == SUSPEND_METHOD_PARAMETER

    fun isSuspendMethodParameter() =
        this == SUSPEND_METHOD_PARAMETER
}