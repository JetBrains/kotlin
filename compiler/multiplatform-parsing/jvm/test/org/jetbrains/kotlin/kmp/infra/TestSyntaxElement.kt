/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import org.jetbrains.kotlin.KtSourceFileLinesMapping

fun <T> List<TestSyntaxElement<T>>.dump(sourceLinesMapping: KtSourceFileLinesMapping?, text: String?): String =
    buildString { this@dump.forEach { appendDump(it, indent = 0, sourceLinesMapping, text) } }

abstract class TestSyntaxElement<out T>(val name: String, val start: Int, val end: Int, val syntaxElement: T, val children: List<TestSyntaxElement<T>>) {
    fun dump(sourceLinesMapping: KtSourceFileLinesMapping?, text: String?): String =
        StringBuilder().apply { appendDump(this@TestSyntaxElement, indent = 0, sourceLinesMapping, text) }.toString()

    override fun toString(): String = dump(sourceLinesMapping = null, text = null)
}

class TestToken<T>(name: String, start: Int, end: Int, token: T, children: List<TestToken<T>>) :
    TestSyntaxElement<T>(name, start, end, token, children)

class TestParseNode<T>(name: String, start: Int, end: Int, parseNode: T, children: List<TestParseNode<out T>>) :
    TestSyntaxElement<T>(name, start, end, parseNode, children)

private fun <T> StringBuilder.appendDump(testSyntaxElement: TestSyntaxElement<T>, indent: Int, sourceLinesMapping: KtSourceFileLinesMapping?, text: String?) {
    if (isNotEmpty()) {
        appendLine()
    }
    (0 until indent).forEach { _ -> append("  ") }
    append(testSyntaxElement.name)

    val start = testSyntaxElement.start
    val end = testSyntaxElement.end

    if (text != null) {
        fun valueContainsNewLine(): Boolean {
            for (index in start until end) {
                if (text[index].let { it == '\n' || it == '\r' }) return true
            }
            return false
        }

        if (!valueContainsNewLine()) {
            val elementValue = text.subSequence(start, end)
            if (elementValue != testSyntaxElement.name) { // Don't print redundant value when they match element identifiers (keywords for example)
                append(" `")
                append(elementValue)
                append('`')
            }
        }
    }

    append(" [")

    var previousLine = -1

    fun appendLocation(location: Int) {
        if (sourceLinesMapping != null) {
            val (line, column) = sourceLinesMapping.getLineAndColumnByOffset(location)
            // It's more text-editor-friendly to start lines and columns with `1`
            if (line != previousLine) {
                append(line + 1)
                append(':')
            }
            previousLine = line
            append(column + 1)
        } else {
            append(location)
        }
    }

    appendLocation(start)
    append("..")
    appendLocation(end)
    append(')')

    testSyntaxElement.children.forEach { appendDump(it, indent + 1, sourceLinesMapping, text) }
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