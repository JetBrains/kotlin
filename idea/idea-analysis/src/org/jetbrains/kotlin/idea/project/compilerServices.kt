/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import org.jetbrains.kotlin.analyzer.common.CommonPlatformCompilerServices
import org.jetbrains.kotlin.js.resolve.JsPlatformCompilerServices
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformCompilerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformCompilerServices
import java.lang.IllegalStateException

val TargetPlatform.findCompilerServices: PlatformDependentCompilerServices
    get() =
        when {
            isJvm() -> JvmPlatformCompilerServices
            isJs() -> JsPlatformCompilerServices
            isNative() -> NativePlatformCompilerServices
            isCommon() -> CommonPlatformCompilerServices
            else -> throw IllegalStateException("Unknown platform $this")
        }