/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import org.jetbrains.kotlin.caches.resolve.CompositeCompilerServices
import org.jetbrains.kotlin.js.resolve.JsPlatformCompilerServices
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.JsPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.konan.KonanPlatform
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformCompilerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformCompilerServices
import java.lang.IllegalStateException

val TargetPlatform.findCompilerServices: PlatformDependentCompilerServices
    get() =
        when {
            isCommon() -> CompositeCompilerServices(this.componentPlatforms.map { it.findCompilerServices })
            else -> single().findCompilerServices
        }

val SimplePlatform.findCompilerServices: PlatformDependentCompilerServices
    get() = when (this) {
        is JvmPlatform -> JvmPlatformCompilerServices
        is JsPlatform -> JsPlatformCompilerServices
        is KonanPlatform -> NativePlatformCompilerServices
        else -> throw IllegalStateException("Unknown platform $this")
    }