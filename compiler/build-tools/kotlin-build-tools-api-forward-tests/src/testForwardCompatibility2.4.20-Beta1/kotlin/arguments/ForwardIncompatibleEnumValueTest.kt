/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

internal class ForwardIncompatibleEnumValueTest : BaseCompilationTest() {

    @Test
    @DisplayName("An enum value the loaded API cannot represent is reported with the enum and value names")
    fun testImplOnlyEnumValueIsReported() {
        assumeTrue(
            JvmTarget.entries.none { it.name == IMPL_ONLY_JVM_TARGET },
            "The loaded kotlin-build-tools-api already declares JvmTarget.$IMPL_ONLY_JVM_TARGET"
        )

        val kotlinToolchain = KotlinToolchains.loadImplementation(btaClassloader)
        val operation = kotlinToolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            // Parsed by the impl into its own JvmTarget mirror, which does have this value.
            compilerArguments.applyArgumentStrings(listOf("-jvm-target=$IMPL_ONLY_JVM_TARGET_VALUE"))
        }.build()

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[JvmCompilerArguments.JVM_TARGET]
        }

        val message = exception.message.orEmpty()
        assertTrue(JvmTarget::class.simpleName!! in message) { "Expected the enum name in the message, but got: $message" }
        assertTrue(IMPL_ONLY_JVM_TARGET in message) { "Expected the offending value in the message, but got: $message" }
    }

    private companion object {
        const val IMPL_ONLY_JVM_TARGET = "JVM_27"
        const val IMPL_ONLY_JVM_TARGET_VALUE = "27"
    }
}
