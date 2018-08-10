/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
