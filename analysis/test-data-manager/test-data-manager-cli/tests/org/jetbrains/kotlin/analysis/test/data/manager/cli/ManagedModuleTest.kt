/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ManagedModuleTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    private val root: Path
        get() = temporaryDirectory.resolve("repo")

    @Test
    fun `the modules come from gradle with their declared test data`() {
        val output = listOf(
            "$ gradlew -q $MANAGED_MODULE_TASK",
            "$MANAGED_MODULE_PREFIX:z\t$root/z\t$root/z/testData\t$root/shared/testData\r",
            "$MANAGED_MODULE_PREFIX:a:b\t$root/plugins/b\t${temporaryDirectory.resolve("outside")}",
            "$MANAGED_MODULE_PREFIX:empty\t$root/empty",
        ).joinToString("\n")
        assertEquals(
            listOf(
                Triple(":a:b", "plugins/b", emptyList()),
                Triple(":empty", "empty", emptyList()),
                Triple(":z", "z", listOf("z/testData", "shared/testData")),
            ),
            parseManagedModules(output, root).map { Triple(it.project, it.directory, it.testData) },
        )
    }

    @Test
    fun `a module has tests of the paths in or around its test data`() {
        val module = ManagedModule(":m", "m", listOf("compiler/testData/diagnostics"))
        val inOrAround = listOf(
            "", "compiler", "compiler/testData", "compiler/testData/diagnostics", "compiler/testData/diagnostics/a.kt",
            "compiler/testData/diagnostics/a",
        )
        for (path in inOrAround) {
            assertTrue(module.mayHaveTestsOf(path, directoryOnly = false), path)
        }

        for (path in listOf("compiler/testData/diag", "compiler/test", "compiler/testData/codegen", "compiler/testData/diagnostics2")) {
            assertFalse(module.mayHaveTestsOf(path, directoryOnly = false), path)
        }

        assertTrue(ManagedModule(":m", "m", listOf("a/b.c")).mayHaveTestsOf("a/b", directoryOnly = false))
        // The repository root, declared as the test data, holds every path
        assertTrue(ManagedModule(":m", "m", listOf("")).mayHaveTestsOf("a/b", directoryOnly = true))
        assertFalse(ManagedModule(":m", "m", listOf("a/b.c")).mayHaveTestsOf("a/b", directoryOnly = true))
    }
}
