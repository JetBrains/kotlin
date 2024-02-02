/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.konan.fir.test.cases.session.builder

import org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.session.builder.AbstractStandaloneSessionBuilderAgainstStdlibTest
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.library.konanCommonLibraryPath
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.junit.jupiter.api.Test
import java.io.File

class NativeStandaloneSessionBuilderAgainstStdlibTest : AbstractStandaloneSessionBuilderAgainstStdlibTest() {
    @Test
    fun testKotlinStdlibJvm() {
        val konanHome = KotlinNativePaths.homePath
        doTestKotlinStdLibResolve(
            NativePlatforms.unspecifiedNativePlatform,
            konanHome.resolve(konanCommonLibraryPath(KONAN_STDLIB_NAME)).toPath()
        )
    }
}