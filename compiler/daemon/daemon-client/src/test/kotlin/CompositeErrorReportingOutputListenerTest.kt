/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CompositeErrorReportingOutputListenerTest {
    @Test
    @DisplayName("Empty composite listener doesn't produce any synthetic output")
    fun testEmptyNoOutput() {
        val compositeErrorReportingOutputListener = CompositeDaemonErrorReportingOutputListener()
        val retrievedProblems = compositeErrorReportingOutputListener.retrieveProblems()
        assert(retrievedProblems.isEmpty()) {
            "Empty compose listener should not produce any problems, but got: $retrievedProblems"
        }
    }

    @Test
    @DisplayName("Empty composite listener doesn't hold any output")
    fun testEmpty() {
        val compositeErrorReportingOutputListener = CompositeDaemonErrorReportingOutputListener()
        compositeErrorReportingOutputListener.onOutputLine("blah-blah")
        val retrievedProblems = compositeErrorReportingOutputListener.retrieveProblems()
        assert(retrievedProblems.isEmpty()) {
            "Empty compose listener should not produce any problems, but got: $retrievedProblems"
        }
    }

    @Test
    fun singleComposedListenerNoOutput() {
        val compositeErrorReportingOutputListener = CompositeDaemonErrorReportingOutputListener(MockErrorReportingOutputListener())
        val retrievedProblems = compositeErrorReportingOutputListener.retrieveProblems()
        assert(retrievedProblems.isEmpty()) {
            "Empty compose listener should not produce any problems, but got: $retrievedProblems"
        }
    }

    @Test
    fun singleComposedListener() {
        val compositeErrorReportingOutputListener = CompositeDaemonErrorReportingOutputListener(MockErrorReportingOutputListener())
        compositeErrorReportingOutputListener.onOutputLine("message 0")
        compositeErrorReportingOutputListener.onOutputLine("message 1")
        val retrievedProblems = compositeErrorReportingOutputListener.retrieveProblems()
        assertEquals(2, retrievedProblems.size)
        for ((index, retrievedProblem) in retrievedProblems.withIndex()) {
            assertEquals("message $index", retrievedProblem)
        }
    }

    @Test
    fun multipleComposedListeners() {
        val compositeErrorReportingOutputListener = CompositeDaemonErrorReportingOutputListener(
            MockErrorReportingOutputListener(),
            MockErrorReportingOutputListener(),
            MockErrorReportingOutputListener()
        )
        val numberOfListeners = 3
        val numberOfMessages = 2
        compositeErrorReportingOutputListener.onOutputLine("message 0")
        compositeErrorReportingOutputListener.onOutputLine("message 1")
        val retrievedProblems = compositeErrorReportingOutputListener.retrieveProblems()
        assertEquals(numberOfMessages * numberOfListeners, retrievedProblems.size)
        for ((index, retrievedProblem) in retrievedProblems.withIndex()) {
            assertEquals("message ${index % numberOfMessages}", retrievedProblem) {
                "Current index: $index, retrieved problems: $retrievedProblems"
            }
        }
    }
}

private class MockErrorReportingOutputListener : DaemonProblemReportingOutputListener {
    private val problems = mutableListOf<String>()

    override fun onOutputLine(line: String) {
        problems.add(line)
    }

    override fun retrieveProblems() = problems
}