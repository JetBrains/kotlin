/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.cli.common.ExitCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Compiles generated sources that are known to stress the compiler stack and asserts that they still compile.
 *
 * Pinning the stack size with [assertCompiles] is what makes these tests mean anything. Whether a source overflows
 * depends on the ratio between the available stack and the depth the compiler recurses to, and the JVM default stack
 * differs per platform (1 MB on x86_64, 2 MB on aarch64), so a test that relied on the default would pass or fail
 * depending on the host. The whole pipeline is run rather than just the frontend, because a source large enough to
 * exhaust the stack in the frontend also has to survive fir2ir and codegen.
 *
 * The sources are generated instead of being stored as test data: they are large, repetitive, and only their shape
 * and size matter, so there is nothing to read in a checked-in file.
 *
 * To cover another stack overflow, add a function that generates the source and a test that hands it to
 * [assertCompiles], with a `// ISSUE:`-style reference on the generator explaining which shape it stresses.
 */
class CompilerStackOverflowTest : AbstractKotlinCompilerIntegrationTest() {
    // The sources are generated into tmpdir, so there is no test data directory under compiler/testData.
    override val testDataPath: String
        get() = File(tmpdir, "testData").path

    @Test
    fun testLongStringConcatenationWithLightTree() {
        assertCompiles(LONG_STRING_CONCATENATION_FILE_NAME, longStringConcatenation())
    }

    /**
     * KT-88399: `"a0" + "a1" + ... `, a left-nested chain that nests one `BINARY_EXPRESSION` per operand. Both
     * `FirPrefixAndSuffixSyntaxChecker`, which reaches the leaf adjacent to every literal by climbing one parent at a
     * time, and `KotlinLightParser.reportErrors`, which walks the freshly parsed light tree, used to descend one frame
     * per operand.
     *
     * Keep [operandCount] such that the folded constant stays under the 64 KB JVM constant-pool limit for a string.
     */
    private fun longStringConcatenation(operandCount: Int = 6000): String {
        val literals: String = (0 until operandCount).joinToString(separator = " +\n        ") { "\"a$it\"" }
        return "const val deepChain: String =\n        $literals\n"
    }

    /**
     * Writes [source] to [fileName], compiles it on a thread that gets exactly [stackSize] bytes of
     * stack, and asserts that the compiler reports nothing and exits with [ExitCode.OK].
     */
    private fun assertCompiles(
        fileName: String,
        source: String,
        stackSize: Long = DEFAULT_STACK_SIZE,
    ) {
        testDataDirectory.mkdirs()
        File(testDataDirectory, fileName).writeText(source)

        val output = compileKotlin(
            fileName,
            File(tmpdir, "out-${fileName.removeSuffix(".kt")}"),
            expectedFileName = null,
            stackSize = stackSize,
        )

        assertEquals(
            normalizeOutput("" to ExitCode.OK),
            normalizeOutput(output),
            "Compiling $fileName with a ${stackSize / 1024} KiB stack failed",
        )
    }

    companion object {
        private const val LONG_STRING_CONCATENATION_FILE_NAME: String = "longStringConcatenation.kt"
        private const val DEFAULT_STACK_SIZE: Long = 512L * 1024L
    }
}
