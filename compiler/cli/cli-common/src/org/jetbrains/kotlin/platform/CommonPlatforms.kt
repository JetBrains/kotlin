/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatforms.defaultJsPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms.unspecifiedJvmPlatform
import org.jetbrains.kotlin.platform.konan.KonanPlatforms
import org.jetbrains.kotlin.platform.konan.KonanPlatforms.defaultKonanPlatform

@Suppress("DEPRECATION_ERROR")
object CommonPlatforms {

    @Deprecated(
        message = "Should be accessed only by compatibility layer, other clients should use 'unspecifiedJvmPlatform'",
        level = DeprecationLevel.ERROR
    )
    object CompatCommonPlatform : TargetPlatform(
        setOf(
            unspecifiedJvmPlatform.single(),
            defaultJsPlatform.single(),
            defaultKonanPlatform.single()
        )
    ), org.jetbrains.kotlin.analyzer.common.CommonPlatform

    val defaultCommonPlatform: TargetPlatform
        get() = CompatCommonPlatform

    val allSimplePlatforms: List<TargetPlatform>
        get() = sequence {
            yieldAll(JvmPlatforms.allJvmPlatforms)
            yieldAll(KonanPlatforms.allKonanPlatforms)
            yieldAll(JsPlatforms.allJsPlatforms)

            // TODO(dsavvinov): extensions points?
        }.toList()
}

