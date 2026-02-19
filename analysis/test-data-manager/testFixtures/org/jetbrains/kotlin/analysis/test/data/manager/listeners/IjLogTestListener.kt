/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.listeners

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * JUnit Platform listener that outputs IntelliJ ijLog XML format to stdout.
 *
 * IntelliJ IDEA's Gradle integration expects this XML format (wrapped in `<ijLog>...</ijLog>` tags)
 * to display test progress in the test runner UI, showing the test tree, progress, pass/fail status,
 * and enabling navigation to test sources.
 *
 * Test IDs are JUnit's `uniqueId` values (e.g., `[engine:junit-jupiter]/[class:FooTest]/[method:testBar()]`),
 * which are globally unique across all modules and engines. This avoids ID clashes when running tests
 * from multiple modules in parallel (each module has its own listener instance).
 *
 * @param suiteName Name of the test suite. It is used to substitute the default "JUnit Jupiter"
 * @param idPrefix Prefix to prepend to each test ID (e.g., `[:analysis]/[[knm]]/1/`) to ensure uniqueness across different runns
 *
 * Key references:
 * - [GradleTestsExecutionConsoleOutputProcessor](https://github.com/JetBrains/intellij-community/blob/ddb933b7888158ca4cf1c1f28e4492b10952ac28/plugins/gradle/java/src/execution/test/runner/events/GradleTestsExecutionConsoleOutputProcessor.java#L15)
 * - [GradleXmlTestEventConverter](https://github.com/JetBrains/intellij-community/blob/ddb933b7888158ca4cf1c1f28e4492b10952ac28/plugins/gradle/java/src/execution/test/runner/events/GradleXmlTestEventConverter.kt#L17)
 *
 * @see org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner
 */
internal class IjLogTestListener(
    private val suiteName: String,
    private val idPrefix: String,
) : TestExecutionListener {
    private val startTimes = ConcurrentHashMap<String, Long>()

    private val TestIdentifier.escapedUniqueId: String
        get() = escapeXml(idPrefix + uniqueId)

    private val TestIdentifier.escapedParentId: String
        get() = parentId.map { escapeXml(idPrefix + it) }.orElse("") ?: ""

    @Suppress("SpellCheckingInspection")
    private fun writeLog(xml: String) {
        val escaped = xml
            .replace("\r\n", "<ijLogEol/>")
            .replace("\n\r", "<ijLogEol/>")
            .replace("\n", "<ijLogEol/>")
            .replace("\r", "<ijLogEol/>")

        // It is crutial to deliver messages in one chunk. Otherwise, they might not be properly read by the IDE
        synchronized(System.out) {
            System.out.flush()
            print("<ijLog>$escaped</ijLog>\n")
            System.out.flush()
        }
    }

    // A workaround to replace "JUnit Jupiter" with a custom message in the test tree
    private val TestIdentifier.enhancedDisplayName: String
        get() = if (!parentId.isPresent && isContainer) {
            suiteName
        } else {
            displayName
        }

    override fun executionStarted(testIdentifier: TestIdentifier) {
        startTimes[testIdentifier.uniqueId] = System.currentTimeMillis()

        val escapedId = testIdentifier.escapedUniqueId
        val escapedParentId = testIdentifier.escapedParentId
        val escapedDisplayName = escapeXml(testIdentifier.enhancedDisplayName)
        val escapedClassName = testIdentifier.escapedClassName
        val escapedMethodName = testIdentifier.escapedMethodName

        val eventType = if (testIdentifier.isContainer) "beforeSuite" else "beforeTest"

        val xml = buildString {
            append("<event type=\"$eventType\">")
            append("<test id=\"$escapedId\" parentId=\"$escapedParentId\">")
            append("<descriptor name=\"$escapedMethodName\" displayName=\"$escapedDisplayName\" className=\"$escapedClassName\"/>")
            append("</test>")
            append("</event>")
        }

        writeLog(xml)
    }

    override fun executionFinished(testIdentifier: TestIdentifier, result: TestExecutionResult) {
        val escapedId = testIdentifier.escapedUniqueId
        val escapedParentId = testIdentifier.escapedParentId
        val escapedDisplayName = escapeXml(testIdentifier.enhancedDisplayName)
        val escapedClassName = testIdentifier.escapedClassName
        val escapedMethodName = testIdentifier.escapedMethodName

        val startTime = startTimes.remove(testIdentifier.uniqueId) ?: System.currentTimeMillis()
        val endTime = System.currentTimeMillis()

        val eventType = if (testIdentifier.isContainer) "afterSuite" else "afterTest"
        val resultType = when (result.status) {
            TestExecutionResult.Status.SUCCESSFUL -> "SUCCESS"
            TestExecutionResult.Status.FAILED -> "FAILURE"
            TestExecutionResult.Status.ABORTED -> "SKIPPED"
        }

        val xml = buildString {
            append("<event type=\"$eventType\">")
            append("<test id=\"$escapedId\" parentId=\"$escapedParentId\">")
            append("<descriptor name=\"$escapedMethodName\" displayName=\"$escapedDisplayName\" className=\"$escapedClassName\"/>")
            append("<result resultType=\"$resultType\" startTime=\"$startTime\" endTime=\"$endTime\">")

            if (result.status == TestExecutionResult.Status.FAILED) {
                val throwable = result.throwable.orElse(null)
                append("<errorMsg>${encodeCdata(throwable?.message)}</errorMsg>")
                append("<exceptionName>${encodeCdata(throwable?.javaClass?.name)}</exceptionName>")
                append("<stackTrace>${encodeCdata(throwable?.stackTraceToString())}</stackTrace>")

                if (throwable is AssertionFailedError &&
                    throwable.expected != null &&
                    throwable.actual != null
                ) {
                    val expectedValue = throwable.expected.value
                    val actualValue = throwable.actual.value

                    val expectedText = if (expectedValue is FileInfo) {
                        String(expectedValue.contents, Charsets.UTF_8)
                    } else {
                        expectedValue?.toString() ?: ""
                    }

                    val actualText = actualValue?.toString() ?: ""

                    append("<failureType>comparison</failureType>")
                    append("<expected>${encodeCdata(expectedText)}</expected>")
                    append("<actual>${encodeCdata(actualText)}</actual>")

                    if (expectedValue is FileInfo) {
                        append("<filePath>${encodeCdata(expectedValue.path)}</filePath>")
                    }
                } else {
                    val failureType = when (throwable) {
                        is AssertionError -> "assertionFailed"
                        else -> "error"
                    }

                    append("<failureType>$failureType</failureType>")
                }
            }

            append("</result>")
            append("</test>")
            append("</event>")
        }

        writeLog(xml)
    }

    override fun executionSkipped(testIdentifier: TestIdentifier, reason: String) {
        val escapedId = testIdentifier.escapedUniqueId
        val escapedParentId = testIdentifier.escapedParentId
        val escapedDisplayName = escapeXml(testIdentifier.enhancedDisplayName)
        val escapedClassName = testIdentifier.escapedClassName
        val escapedMethodName = testIdentifier.escapedMethodName

        val currentTime = System.currentTimeMillis()

        val eventType = if (testIdentifier.isContainer) "afterSuite" else "afterTest"

        val xml = buildString {
            append("<event type=\"$eventType\">")
            append("<test id=\"$escapedId\" parentId=\"$escapedParentId\">")
            append("<descriptor name=\"$escapedMethodName\" displayName=\"$escapedDisplayName\" className=\"$escapedClassName\"/>")
            append("<result resultType=\"SKIPPED\" startTime=\"$currentTime\" endTime=\"$currentTime\"/>")
            append("</test>")
            append("</event>")
        }

        writeLog(xml)
    }
}

private fun encodeCdata(s: String?): String {
    val bytes = (s ?: "").toByteArray(Charsets.UTF_8)
    val encoded = Base64.getEncoder().encodeToString(bytes)
    return "<![CDATA[$encoded]]>"
}

private val TestIdentifier.escapedClassName: String
    get() = escapeXml(this.className)

private val TestIdentifier.className: String
    get() = when (val source = this.source.orElse(null)) {
        is MethodSource -> source.className
        is ClassSource -> source.className
        else -> ""
    }

private val TestIdentifier.escapedMethodName: String
    get() = escapeXml(this.methodName)

private val TestIdentifier.methodName: String
    get() = when (val source = this.source.orElse(null)) {
        is MethodSource -> source.methodName
        else -> this.displayName
    }

private fun escapeXml(s: String): String = s
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")
