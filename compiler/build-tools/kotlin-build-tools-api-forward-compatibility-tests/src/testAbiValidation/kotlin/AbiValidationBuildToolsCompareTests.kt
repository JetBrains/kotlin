/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain.Companion.abiValidation
import org.jetbrains.kotlin.buildtools.api.abi.compareAbiTextFilesOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText

class AbiValidationBuildToolsCompareTests {
    @Test
    @DisplayName("Test that compare operation produces the expected diff")
    fun testSimpleCompare() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)

        val appendable = StringBuilder()
        val expected = createTempFile().also { it.toFile().deleteOnExit() }
        val actual = createTempFile().also { it.toFile().deleteOnExit() }

        expected.writeText(
            """
            A()
            B()
            C()
            D()
        """.trimIndent()
        )

        actual.writeText(
            """
            A()
            B1()
            C()
            X()
            D()
        """.trimIndent()
        )
        val operation = toolchain.abiValidation.compareAbiTextFilesOperation(appendable, expected, actual)

        toolchain.createBuildSession().use { it.executeOperation(operation) }

        // skip the first two lines with file names
        val actualDiff = "@@" + appendable.toString().substringAfter("@@")

        val expectedDiff = """
            @@ -1,4 +1,5 @@
             A()
            -B()
            +B1()
             C()
            +X()
             D()
        """.trimIndent()

        assertEquals(expectedDiff, actualDiff)
    }
}
