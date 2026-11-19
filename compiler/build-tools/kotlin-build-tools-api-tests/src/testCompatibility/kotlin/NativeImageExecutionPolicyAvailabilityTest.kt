/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.Path

class NativeImageExecutionPolicyAvailabilityTest : BaseCompilationTest() {

    @DisplayName("Native image policies are available from Kotlin compiler version 2.5.20 and fail with a clear message before it")
    @BtaVersionsOnlyCompilationTest
    fun testAvailability(testToolchains: KotlinToolchains) {
        // The [v1] toolchain is the bare compat adapter; consumers load it through loadImplementation, which adds the wrappers
        val kotlinToolchains = if (testToolchains::class.simpleName == "KotlinToolchainsV1Adapter") {
            KotlinToolchains.loadImplementation(btaClassloader)
        } else {
            testToolchains
        }
        val distPath = Path("native-image-dist")

        if (KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) < KotlinToolingVersion(2, 5, 20, "snapshot")) {
            val processPolicyException = assertThrows<IllegalStateException> {
                kotlinToolchains.createNativeImageProcessExecutionPolicy(distPath)
            }
            assertEquals("Native image execution policies are supported from compiler version 2.5.20.", processPolicyException.message)

            val daemonPolicyException = assertThrows<IllegalStateException> {
                kotlinToolchains.nativeImageDaemonExecutionPolicyBuilder(distPath)
            }
            assertEquals("Native image execution policies are supported from compiler version 2.5.20.", daemonPolicyException.message)
        } else {
            assertEquals(distPath, kotlinToolchains.createNativeImageProcessExecutionPolicy(distPath).distPath)
            assertEquals(distPath, kotlinToolchains.nativeImageDaemonExecutionPolicyBuilder(distPath).build().distPath)
        }
    }
}
