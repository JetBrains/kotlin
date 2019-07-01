/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import java.lang.IllegalStateException

val TargetPlatform.findAnalyzerServices: PlatformDependentAnalyzerServices
    get() =
        when {
            isJvm() -> JvmPlatformAnalyzerServices
            isJs() -> JsPlatformAnalyzerServices
            isNative() -> NativePlatformAnalyzerServices
            isCommon() -> CommonPlatformAnalyzerServices
            else -> throw IllegalStateException("Unknown platform $this")
        }