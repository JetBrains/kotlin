/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.jvmCompilationOperation
import org.jetbrains.kotlin.buildtools.forward.tests.btaClassloader
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class ApplyArgumentStringsOverridingTest : BaseCompilationTest() {

    @Test
    fun `test applyArgumentStrings does not override existing values in new versions`() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val apiVersion =
            KotlinToolingVersion(
                KotlinToolchains::class.java.methods.firstOrNull { it.name == "getVersion" }?.invoke(null)?.toString() ?: "2.4.0"
            )
        val isNewBehaviorExpected = apiVersion >= KotlinToolingVersion("2.5.0-snapshot")


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
}
