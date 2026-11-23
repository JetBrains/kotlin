/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.buildtools.tests.fuzzing.harness.IcFuzzHarness
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/** The valid programs the compiler crashes on or rejects, found by the fuzzing. */
@DisplayName("The compiler compiles valid programs")
class ValidProgramCompilationTest {
    @TempDir
    lateinit var workingDirectory: Path

    private fun assertCompiles(@Language("kotlin") source: String) = IcFuzzHarness(workingDirectory).use { harness ->
        val build = harness.build(mapOf("a.kt" to "package p0\n\n$source\n")).rebuild
        assertTrue(build.isSuccessful) { "${build.outcome}\n${build.errors.joinToString("\n")}\n${build.crash?.stackTraceToString().orEmpty()}" }
    }

    @Test
    @DisplayName("a call using a default value of an inline function with @JvmExposeBoxed, without crashing")
    fun testJvmExposeBoxedInlineFunctionWithDefaultValue() = assertCompiles(
        """
            |@OptIn(ExperimentalStdlibApi::class)
            |@JvmExposeBoxed
            |inline fun f4(a0: Int? = 0): Int? = a0
            |
            |fun f5() = f4()
        """.trimMargin(),
    )

    @Test
    @DisplayName("a value class inheriting a Unit property from an interface, without a declaration clash")
    fun testValueClassInheritingUnitProperty() = assertCompiles(
        """
            |interface I {
            |    val p: Unit get() = Unit
            |}
            |
            |@JvmInline
            |value class V(val v: Int) : I
        """.trimMargin(),
    )

    @Test
    @DisplayName("a value class overriding a Unit property of an interface, without an accidental override")
    fun testValueClassOverridingUnitProperty() = assertCompiles(
        """
            |interface I {
            |    val p: Unit
            |}
            |
            |@JvmInline
            |value class V(val v: Int) : I {
            |    override val p: Unit get() = Unit
            |}
        """.trimMargin(),
    )
}
