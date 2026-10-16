/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Direct supertypes of Java source classes resolved by K2 are recorded in the Java-only IC hierarchy maps.
 */
class IncrementalJavaSupertypesTest : AbstractIncrementalJvmCompilerRunnerTest() {
    @Test
    fun testJavaSourceSupertypesAreRecorded() {
        val sourceRoot = File(workingDir, "src").apply { mkdirs() }
        val outputDirectory = File(workingDir, "out").apply { mkdirs() }
        val cacheDirectory = File(workingDir, "caches").apply { mkdirs() }

        File(sourceRoot, "Base.java").writeText("public abstract class Base { public String getUrl() { return \"foo\"; } }")
        File(sourceRoot, "Derived.java").writeText("public class Derived extends Base { @Override public String getUrl() { return \"bar\"; } }")
        File(sourceRoot, "usage.kt").writeText("class Usage { val url: String = Derived().url }")

        (val exitCode, val compileErrors) =
            make(cacheDirectory, outputDirectory, listOf(sourceRoot), createCompilerArguments(outputDirectory, workingDir))
        assertEquals(ExitCode.OK, exitCode, compileErrors.joinToString("\n"))

        val base = FqName("Base")
        val derived = FqName("Derived")
        IncrementalJvmCache(File(cacheDirectory, "caches-jvm/jvm"), IncrementalCompilationContext(), outputDirectory).use { cache ->
            assertEquals(setOf(base), cache.getJavaSupertypesOf(derived).toSet())
            assertEquals(setOf(derived), cache.getJavaSubtypesOf(base).toSet())
        }
    }
}
