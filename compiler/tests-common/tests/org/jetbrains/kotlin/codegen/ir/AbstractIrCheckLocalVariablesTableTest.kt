/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.ir

import com.intellij.openapi.util.Comparing
import org.jetbrains.kotlin.codegen.AbstractCheckLocalVariablesTableTest
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.junit.ComparisonFailure
import java.nio.charset.Charset

abstract class AbstractIrCheckLocalVariablesTableTest : AbstractCheckLocalVariablesTableTest() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        assert(environment != null)
        environment.configuration.put(JVMConfigurationKeys.IR, true)
    }

    override fun doCompare(text: String?, actualLocalVariables: MutableList<LocalVariable>) {
        val actual = getActualVariablesAsList(actualLocalVariables)
        val expected = getExpectedVariablesAsList()
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

    private fun getExpectedVariablesAsList(): List<String> {
        return ktFile.readLines(Charset.forName("utf-8"))
            .filter { line -> line.startsWith("// VARIABLE ") }
            .filter { !it.contains("NAME=\$i\$") }
            .map { line -> line.replaceFirst("INDEX=\\d+".toRegex(), "INDEX=*") } // Ignore index
            .sorted()
    }
}
