/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.session.builder

import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Test
import java.nio.file.Paths

class StandaloneSessionBuilderAgainstStdlibTest : AbstractStandaloneSessionBuilderAgainstStdlibTest() {
    @Test
    fun testKotlinStdlibJvm() {
        doTestKotlinStdLibResolve(JvmPlatforms.defaultJvmPlatform, PathUtil.kotlinPathsForDistDirectory.stdlibPath.toPath())
    }

    @Test
    fun testKotlinStdLibCommon() {
        // KT-63493 to avoid using a hardcoded path
        doTestKotlinStdLibResolve(CommonPlatforms.defaultCommonPlatform, Paths.get("dist/common/kotlin-stdlib-common.jar"))
    }

    @Test
    fun testKotlinStdLibJs() {
        doTestKotlinStdLibResolve(JsPlatforms.defaultJsPlatform, PathUtil.kotlinPathsForDistDirectory.jsStdLibKlibPath.toPath())
    }

    @Test
    fun testKotlinStdLibJsWithInvalidKlib() {
        doTestKotlinStdLibResolve(
            JsPlatforms.defaultJsPlatform,
            PathUtil.kotlinPathsForDistDirectory.jsStdLibKlibPath.toPath(),
            additionalStdlibRoots = listOf(
                Paths.get(System.getProperty("java.home")), // directory which exists and does not contain KLibs inside
                PathUtil.kotlinPathsForDistDirectory.stdlibPath.toPath(), // file which exists and not a KLib
            )
        )
    }
}