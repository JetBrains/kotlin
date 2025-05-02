/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import org.jetbrains.kotlin.KtSourceFileLinesMapping

fun <T> List<TestSyntaxElement<T>>.dump(sourceLinesMapping: KtSourceFileLinesMapping? = null): String =
    buildString { this@dump.forEach { appendDump(it, indent = 0, sourceLinesMapping) } }

abstract class TestSyntaxElement<T>(val name: String, val start: Int, val end: Int, val syntaxElement: T, val children: List<TestSyntaxElement<T>>) {
    override fun toString(): String = StringBuilder().apply { appendDump(this@TestSyntaxElement, indent = 0) }.toString()
}

class TestToken<T>(name: String, start: Int, end: Int, token: T, children: List<TestSyntaxElement<T>>) :
    TestSyntaxElement<T>(name, start, end, token, children)

private fun <T> StringBuilder.appendDump(testSyntaxElement: TestSyntaxElement<T>, indent: Int, sourceLinesMapping: KtSourceFileLinesMapping? = null) {
    if (isNotEmpty()) {
        appendLine()
    }
    (0 until indent).forEach { _ -> append("  ") }
    append(testSyntaxElement.name)
    append(" [")

    fun appendLocation(location: Int) {
        if (sourceLinesMapping != null) {
            val (line, column) = sourceLinesMapping.getLineAndColumnByOffset(location)
            // It's more text-editor-friendly to start lines and columns with `1`
            append(line + 1)
            append(':')
            append(column + 1)
        } else {
            append(location)
        }
    }

    appendLocation(testSyntaxElement.start)
    append("..")
    appendLocation(testSyntaxElement.end)
    append(')')

    testSyntaxElement.children.forEach { appendDump(it, indent + 1, sourceLinesMapping) }
}

fun compareSyntaxElements(testSyntaxElement1: TestSyntaxElement<*>, testSyntaxElement2: TestSyntaxElement<*>, comparisonFailedAction: () -> Unit): Long {
    var syntaxElementNumber = 1L // Count all nodes, not only leaf ones

    if (testSyntaxElement1.name != testSyntaxElement2.name ||
        testSyntaxElement1.start != testSyntaxElement2.start ||
        testSyntaxElement1.end != testSyntaxElement2.end
    ) {
        comparisonFailedAction()
    }

    val children1 = testSyntaxElement1.children
    val children2 = testSyntaxElement2.children
    if (children1.size != children2.size) {
        comparisonFailedAction()
    }

    for (index in children1.indices) {
        syntaxElementNumber += compareSyntaxElements(children1[index], children2[index], comparisonFailedAction)
    }

    return syntaxElementNumber
}