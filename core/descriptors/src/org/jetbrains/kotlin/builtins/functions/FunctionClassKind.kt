/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.functions

import org.jetbrains.kotlin.utils.IDEAPlatforms
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

@IDEAPluginsCompatibilityAPI(IDEAPlatforms._223, message = "Please migrate to the FunctionTypeKind", plugins = "android")
enum class FunctionClassKind {
    Function,
    SuspendFunction,
    KFunction,
    KSuspendFunction,
    UNKNOWN;

    companion object {
        fun getFunctionClassKind(functionTypeKind: FunctionTypeKind): FunctionClassKind = when (functionTypeKind) {
            FunctionTypeKind.Function -> Function
            FunctionTypeKind.SuspendFunction -> SuspendFunction
            FunctionTypeKind.KFunction -> KFunction
            FunctionTypeKind.KSuspendFunction -> KSuspendFunction
            else -> UNKNOWN
        }
    }
}
