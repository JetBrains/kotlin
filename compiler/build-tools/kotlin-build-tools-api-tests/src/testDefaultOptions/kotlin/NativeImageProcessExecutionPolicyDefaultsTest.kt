/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.defaults

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NativeImageProcessExecutionPolicyDefaultsTest {
    @Test
    fun testDistPathHasNoDefault() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val builder = kotlinToolchains.nativeImageProcessExecutionPolicyBuilder()
        assertThrows<IllegalStateException> { builder[ExecutionPolicy.NativeImageProcess.DIST_PATH] }
        assertThrows<IllegalStateException> { builder.build() }
    }
}
