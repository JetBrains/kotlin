/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

internal const val NATIVE_PTR_NAME = "NativePtr"
internal const val NON_NULL_NATIVE_PTR_NAME = "NonNullNativePtr"

object KonanFqNames {
    val function = FqName("kotlin.Function")
    val kFunction = FqName("kotlin.reflect.KFunction")
    val packageName = FqName("kotlin.native")
    val internalPackageName = FqName("kotlin.native.internal")
    val nativePtr = internalPackageName.child(Name.identifier(NATIVE_PTR_NAME)).toUnsafe()
    val nonNullNativePtr = internalPackageName.child(Name.identifier(NON_NULL_NATIVE_PTR_NAME)).toUnsafe()
    val throws = FqName("kotlin.native.Throws")
    val threadLocal = FqName("kotlin.native.concurrent.ThreadLocal")
    val sharedImmutable = FqName("kotlin.native.concurrent.SharedImmutable")
    val frozen = FqName("kotlin.native.internal.Frozen")
    val typedIntrinsic = FqName("kotlin.native.internal.TypedIntrinsic")
    val objCMethod = FqName("kotlinx.cinterop.ObjCMethod")
}
