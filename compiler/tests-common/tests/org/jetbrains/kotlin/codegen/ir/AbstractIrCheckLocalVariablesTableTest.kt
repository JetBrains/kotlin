/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.ir

import com.intellij.openapi.util.Comparing
import org.jetbrains.kotlin.codegen.AbstractCheckLocalVariablesTableTest
import org.jetbrains.kotlin.test.TargetBackend
import org.junit.ComparisonFailure
import java.io.File

abstract class AbstractIrCheckLocalVariablesTableTest : AbstractCheckLocalVariablesTableTest() {
    override fun doCompare(
        testFile: File,
        text: String,
        actualLocalVariables: List<LocalVariable>
    ) {
        val actual = getActualVariablesAsList(actualLocalVariables)
        val expected = getExpectedVariablesAsList(testFile)
        if (!Comparing.equal(expected, actual)) {
            throw ComparisonFailure(
                "Variables differ from expected",
                expected.joinToString("\n"),
                actual.joinToString("\n")
            )
        }
    }

    private fun getActualVariablesAsList(list: List<LocalVariable>): List<String> {
        return list.map { it.toString() }
            .map { line -> line.replaceFirst("INDEX=\\d+".toRegex(), "INDEX=*") } // Ignore index
            .sorted()
    }

    private fun getExpectedVariablesAsList(testFile: File): List<String> {
        return testFile.readLines()
            .filter { line -> line.startsWith("// VARIABLE ") }
            .filter { !it.contains("NAME=\$i\$") }
            .map { line -> line.replaceFirst("INDEX=\\d+".toRegex(), "INDEX=*") } // Ignore index
            .sorted()
    }

    override fun getBackend() = TargetBackend.JVM_IR
}
