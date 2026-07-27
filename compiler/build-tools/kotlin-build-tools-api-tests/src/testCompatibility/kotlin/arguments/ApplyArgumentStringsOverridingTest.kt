/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.jvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class ApplyArgumentStringsOverridingTest : BaseCompilationTest() {

    val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
    val compilerVersion = KotlinToolingVersion(kotlinToolchains.getCompilerVersion())
    val isNewBehaviorExpected = compilerVersion >= KotlinToolingVersion("2.5.0-snapshot")

    @Test
    fun `test applyArgumentStrings does not override existing values in new versions`() {
        kotlinToolchains.jvm.jvmCompilationOperation(emptyList(), Path("")) {
            compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_17
            compilerArguments[JvmCompilerArguments.NO_STDLIB] = true

            compilerArguments.applyArgumentStrings(listOf("-no-reflect"))
            if (isNewBehaviorExpected) {
                assertEquals(JvmTarget.JVM_17, compilerArguments[JvmCompilerArguments.JVM_TARGET])
                assertEquals(true, compilerArguments[JvmCompilerArguments.NO_STDLIB])
            } else {
                // Old versions override everything
                assertNotEquals(JvmTarget.JVM_17, compilerArguments[JvmCompilerArguments.JVM_TARGET])
                assertEquals(false, compilerArguments[JvmCompilerArguments.NO_STDLIB])
            }
            assertEquals(true, compilerArguments[JvmCompilerArguments.NO_REFLECT])
        }
    }

    @Test
    fun `test applyArgumentStrings does not override compiler plugins in new versions`() {
        assumeTrue(isNewBehaviorExpected, "This test only works for versions >= 2.5.0-snapshot")

        kotlinToolchains.jvm.jvmCompilationOperation(emptyList(), Path("")) {
            val plugin = org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin(
                "my-plugin",
                listOf(java.io.File("some.jar").toPath()),
                emptyList(),
                emptySet()
            )
            compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(plugin)

            compilerArguments.applyArgumentStrings(listOf("-no-reflect"))

            val plugins = compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS]
            assertEquals(listOf("my-plugin"), plugins.map { it.pluginId })
        }
    }

    @Test
    fun `test applyArgumentStrings updates compiler plugins when related arguments are provided`() {
        assumeTrue(isNewBehaviorExpected, "This test only works for versions >= 2.5.0-snapshot")

        kotlinToolchains.jvm.jvmCompilationOperation(emptyList(), Path("")) {
            compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = emptyList()

            compilerArguments.applyArgumentStrings(listOf("-Xplugin=some-path.jar"))

            val plugins = compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS]
            assertEquals(listOf("___RAW_PLUGINS_APPLIED___"), plugins.map { it.pluginId })
        }
    }
}
