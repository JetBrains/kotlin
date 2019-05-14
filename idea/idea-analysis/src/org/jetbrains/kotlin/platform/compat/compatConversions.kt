/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION_ERROR", "TYPEALIAS_EXPANSION_DEPRECATION_ERROR")

package org.jetbrains.kotlin.platform.compat

import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.KonanPlatform
import org.jetbrains.kotlin.platform.konan.KonanPlatforms

typealias OldPlatform = org.jetbrains.kotlin.resolve.TargetPlatform
typealias NewPlatform = org.jetbrains.kotlin.platform.TargetPlatform

fun NewPlatform.toOldPlatform(): OldPlatform = when (val single = singleOrNull()) {
    null -> CommonPlatforms.CompatCommonPlatform
    is JvmPlatform -> JvmPlatforms.CompatJvmPlatform
    is JsPlatform -> JsPlatforms.CompatJsPlatform
    is KonanPlatform -> KonanPlatforms.CompatKonanPlatform
    else -> error("Unknown platform $single")
}

