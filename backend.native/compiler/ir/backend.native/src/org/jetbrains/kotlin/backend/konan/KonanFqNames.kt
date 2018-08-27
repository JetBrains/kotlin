/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

internal const val NATIVE_PTR_NAME = "NativePtr"

object KonanFqNames {

    val packageName = FqName("kotlin.native")
    val internalPackageName = FqName("kotlin.native.internal")
    val nativePtr = internalPackageName.child(Name.identifier(NATIVE_PTR_NAME)).toUnsafe()
    val nonNullNativePtr = FqNameUnsafe("kotlin.native.internal.NonNullNativePtr")
    val throws = FqName("kotlin.native.Throws")
}

/**
 * Maximum number of parameters supported in function types (e.g. `FunctionXX`, `KFunctionXX`, `SuspendFunctionXX`).
 */
internal const val KONAN_FUNCTION_INTERFACES_MAX_PARAMETERS = 22
