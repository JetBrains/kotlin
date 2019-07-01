/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.platform.konan.KonanPlatforms
import org.jetbrains.kotlin.resolve.TargetPlatform

@Deprecated(
    message = "This class is deprecated and will be removed soon, use API from 'org.jetbrains.kotlin.platform.*' packages instead",
    replaceWith = ReplaceWith("KonanPlatforms.defaultKonanPlatform", "org.jetbrains.kotlin.platform.konan.KonanPlatforms"),
    level = DeprecationLevel.ERROR
)
interface KonanPlatform : TargetPlatform {
    @JvmDefault
    override val platformName: String
        get() = "Native"

    companion object {
        @JvmField
        val INSTANCE: KonanPlatform = KonanPlatforms.CompatKonanPlatform
    }
}
