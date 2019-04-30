/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatforms.defaultJsPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms.defaultJvmPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms.jvm18
import org.jetbrains.kotlin.platform.konan.KonanPlatforms
import org.jetbrains.kotlin.platform.konan.KonanPlatforms.defaultKonanPlatform

object CommonPlatforms {

    val defaultCommonPlatform: TargetPlatform = TargetPlatform(
        setOf(
            defaultJvmPlatform.single(),
            defaultJsPlatform.single(),
            defaultKonanPlatform.single()
        )
    )

    val allSimplePlatforms: List<TargetPlatform>
        get() = sequence {
            yieldAll(JvmPlatforms.allJvmPlatforms)
            yieldAll(KonanPlatforms.allKonanPlatforms)
            yieldAll(JsPlatforms.allJsPlatforms)

            // TODO(dsavvinov): extensions points?
        }.toList()
}

