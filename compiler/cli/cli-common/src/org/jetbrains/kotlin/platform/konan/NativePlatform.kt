/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform.konan

import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.TargetPlatform

abstract class NativePlatform : SimplePlatform("Native") {
    override val oldFashionedDescription: String
        get() = "Native "
}

@Suppress("DEPRECATION_ERROR")
object NativePlatforms {
    private object DefaultSimpleNativePlatform : NativePlatform()

    @Deprecated(
        message = "Should be accessed only by compatibility layer, other clients should use 'defaultNativePlatform'",
        level = DeprecationLevel.ERROR
    )
    object CompatNativePlatform : TargetPlatform(setOf(DefaultSimpleNativePlatform)),
        // Needed for backward compatibility, because old code uses INSTANCEOF checks instead of calling extensions
        org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform {
        override val platformName: String
            get() = "Native"
    }

    val defaultNativePlatform: TargetPlatform
        get() = CompatNativePlatform

    val allNativePlatforms: List<TargetPlatform> = listOf(defaultNativePlatform)
}

fun TargetPlatform?.isNative(): Boolean = this?.singleOrNull() is NativePlatform
