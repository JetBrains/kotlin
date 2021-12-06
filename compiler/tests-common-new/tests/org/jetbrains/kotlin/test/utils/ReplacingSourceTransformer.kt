/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.test.model.TestFile
import java.util.function.Function

class ReplacingSourceTransformer(val from: String, val to: String) : Function<String, String>, (String) -> String {
    init {
        require(from.isNotEmpty()) { "Cannot replace empty string" }
        require(from.lines().size == 1) { "Multiline text cannot be replaced yet" }
        require(to.lines().size == from.lines().size) { "Number of lines cannot change" }
    }

    private val replacements: MutableMap<String?, List<List<String>>> = mutableMapOf()

    fun invokeForTestFile(testFile: TestFile?, source: String): String {
        val value = source.lines().map { it.split(from) }
        replacements[testFile?.relativePath] = value
        return value.joinToString("\n") { it.joinToString(to) }
    }

    fun revertForFile(testFile: TestFile?, actual: String): String = actual.lines().mapIndexed { index, line ->
        val partition = replacements[testFile?.relativePath]?.elementAtOrNull(index) ?: return@mapIndexed line
        if (partition.joinToString(to) == line) partition.joinToString(from) else line
    }.joinToString("\n")

    override fun apply(source: String): String = invokeForTestFile(null, source)
    override fun invoke(source: String): String = invokeForTestFile(null, source)
}