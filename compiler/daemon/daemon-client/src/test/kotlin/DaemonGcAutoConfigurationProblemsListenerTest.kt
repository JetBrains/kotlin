/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DaemonGcAutoConfigurationProblemsListenerTest {
    private val gcAutoConfiguration = KotlinCompilerClient.GcAutoConfiguration()
    private val gcProblemsListener = DaemonGcAutoConfigurationProblemsListener(gcAutoConfiguration, 0)

    @Test
    @DisplayName("GC auto-selection isn't disabled when there's no output")
    fun testSideEffectWhenNoOutputs() {
        assertEquals(true, gcAutoConfiguration.shouldAutoConfigureGc)
        val retrievedProblems = gcProblemsListener.retrieveProblems()
        assert(retrievedProblems.isEmpty()) {
            "GC problems listener should not produce any problems, but got: $retrievedProblems"
        }
    }

    @Test
    @DisplayName("GC auto-selection isn't disabled when there's problems detected")
    fun testSideEffectWhenNoProblemOccurs() {
        gcProblemsListener.onOutputLine("Daemon is running")
        assertEquals(true, gcAutoConfiguration.shouldAutoConfigureGc)
        val retrievedProblems = gcProblemsListener.retrieveProblems()
        assert(retrievedProblems.isEmpty()) {
            "GC problems listener should not produce any problems, but got: $retrievedProblems"
        }
    }

    @Test
    @DisplayName("GC auto-selection is disabled on multiple garbage collectors selected")
    fun testSideEffectWhenProblemOccurs() {
        gcProblemsListener.onOutputLine("Multiple garbage collectors selected")
        assertEquals(false, gcAutoConfiguration.shouldAutoConfigureGc)
        assert(gcProblemsListener.retrieveProblems().isNotEmpty()) {
            "GC problems listener should produce problems"
        }
    }

    @Test
    @DisplayName("GC auto-selection is disabled when startup problem occurs more than 2 times")
    fun testSideEffectWhenUnknownProblemOccurs() {
        val firstGcProblemsListener = DaemonGcAutoConfigurationProblemsListener(gcAutoConfiguration, 0)
        firstGcProblemsListener.retrieveProblems().run {
            assert(isEmpty()) {
                "GC problems listener should not produce any problems, but got: $this"
            }
        }
        assertEquals(true, gcAutoConfiguration.shouldAutoConfigureGc)
        val secondGcProblemsListener = DaemonGcAutoConfigurationProblemsListener(gcAutoConfiguration, 1)
        assert(secondGcProblemsListener.retrieveProblems().isNotEmpty()) {
            "GC problems listener should produce problems"
        }
        assertEquals(false, gcAutoConfiguration.shouldAutoConfigureGc)
    }

    @Test
    @DisplayName("No problems reported when GC auto-selection is disabled")
    fun testNoProblemsReportedWHenAutoConfigurationIsDisabled() {
        gcAutoConfiguration.shouldAutoConfigureGc = false
        gcProblemsListener.onOutputLine("Multiple garbage collectors selected")
        val retrievedProblems = gcProblemsListener.retrieveProblems()
        assert(retrievedProblems.isEmpty()) {
            "GC problems listener should not produce any problems, but got: $retrievedProblems"
        }
    }

    @Test
    @DisplayName("GC auto-selection is disabled on multiple garbage collectors selected")
    fun testErrorMessageWithDefaultGc() {
        gcProblemsListener.onOutputLine("Multiple garbage collectors selected")
        val errorMessages = gcProblemsListener.retrieveProblems()
        assertEquals(2, errorMessages.size)
        assert("Parallel GC" in errorMessages[0]) {
            "Expected error message to mention Parallel GC, got:\n" + errorMessages[0]
        }
        assert("-XX:-Use${gcAutoConfiguration.preferredGc}GC" in errorMessages[1]) {
            "Expected error message to mention how to disable GC auto-selection, got:\n" + errorMessages[1]
        }
    }

    @Test
    @DisplayName("GC auto-selection is disabled on multiple garbage collectors selected with custom GC")
    fun testErrorMessageWithCustomGc() {
        val gcAutoConfiguration = KotlinCompilerClient.GcAutoConfiguration(preferredGc = "G1")
        val gcProblemsListener = DaemonGcAutoConfigurationProblemsListener(gcAutoConfiguration, 0)
        gcProblemsListener.onOutputLine("Multiple garbage collectors selected")
        val errorMessages = gcProblemsListener.retrieveProblems()
        assertEquals(2, errorMessages.size)
        assert("G1 GC" in errorMessages[0]) {
            "Expected error message to mention G1 GC, got:\n" + errorMessages[0]
        }
        assert("-XX:-Use${gcAutoConfiguration.preferredGc}GC" in errorMessages[1]) {
            "Expected error message to mention how to disable GC auto-selection, got:\n" + errorMessages[1]
        }
    }
}