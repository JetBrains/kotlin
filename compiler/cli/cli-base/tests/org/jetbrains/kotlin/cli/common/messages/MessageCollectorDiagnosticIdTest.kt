/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MessageCollectorDiagnosticIdTest {

    /**
     * A test [MessageCollector] that records all reported messages including diagnostic IDs.
     */
    private class RecordingMessageCollector : MessageCollectorWithDiagnosticId {
        data class Entry(
            val severity: CompilerMessageSeverity,
            val message: String,
            val location: CompilerMessageSourceLocation?,
            val diagnosticId: String?,
        )

        val entries = mutableListOf<Entry>()
        private var hasErrorFlag = false

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
            diagnosticId: String?,
        ) {
            entries.add(Entry(severity, message, location, diagnosticId))
            if (severity.isError) hasErrorFlag = true
        }

        override fun clear() {
            entries.clear()
            hasErrorFlag = false
        }

        override fun hasErrors(): Boolean = hasErrorFlag
    }

    /**
     * A plain [MessageCollector] that does NOT implement [MessageCollectorWithDiagnosticId],
     * used to verify the extension function gracefully falls back to the 3-arg report.
     */
    private class PlainRecordingMessageCollector : MessageCollector {
        val messages = mutableListOf<String>()
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            messages.add(message)
        }
        override fun clear() {
            messages.clear()
        }

        override fun hasErrors(): Boolean = false
    }

    // ---- GroupingMessageCollector tests ----

    @Test
    fun `diagnostic ID is preserved through flush`() {
        val delegate = RecordingMessageCollector()
        val collector = GroupingMessageCollector(delegate, false, false)

        collector.report(CompilerMessageSeverity.ERROR, "some error", null, "UNRESOLVED_REFERENCE")
        collector.flush()

        assertEquals(1, delegate.entries.size)
        assertEquals("UNRESOLVED_REFERENCE", delegate.entries[0].diagnosticId)
    }

    @Test
    fun `non-null diagnostic ID is not downgraded to null on duplicate`() {
        val delegate = RecordingMessageCollector()
        val collector = GroupingMessageCollector(delegate, false, false)

        collector.report(CompilerMessageSeverity.ERROR, "some error", null, "TYPE_MISMATCH")
        collector.report(CompilerMessageSeverity.ERROR, "some error", null, null)
        collector.flush()

        assertEquals(1, delegate.entries.size)
        assertEquals("TYPE_MISMATCH", delegate.entries[0].diagnosticId)
    }

    @Test
    fun `duplicate with different non-null IDs keeps first`() {
        val delegate = RecordingMessageCollector()
        val collector = GroupingMessageCollector(delegate, false, false)

        collector.report(CompilerMessageSeverity.ERROR, "some error", null, "FIRST_ID")
        collector.report(CompilerMessageSeverity.ERROR, "some error", null, "SECOND_ID")
        collector.flush()

        assertEquals(1, delegate.entries.size)
        assertEquals("FIRST_ID", delegate.entries[0].diagnosticId)
    }

    @Test
    fun `OUTPUT severity forwards diagnostic ID immediately`() {
        val delegate = RecordingMessageCollector()
        val collector = GroupingMessageCollector(delegate, false, false)

        collector.report(CompilerMessageSeverity.OUTPUT, "output msg", null, "SOME_ID")

        // Should be forwarded immediately, before flush
        assertEquals(1, delegate.entries.size)
        assertEquals("SOME_ID", delegate.entries[0].diagnosticId)
    }

    @Test
    fun `distinct messages with different IDs are not deduplicated`() {
        val delegate = RecordingMessageCollector()
        val collector = GroupingMessageCollector(delegate, false, false)

        collector.report(CompilerMessageSeverity.ERROR, "error one", null, "ID_A")
        collector.report(CompilerMessageSeverity.ERROR, "error two", null, "ID_B")
        collector.flush()

        assertEquals(2, delegate.entries.size)
    }

    @Test
    fun `null diagnostic ID is preserved when no duplicate exists`() {
        val delegate = RecordingMessageCollector()
        val collector = GroupingMessageCollector(delegate, false, false)

        collector.report(CompilerMessageSeverity.ERROR, "some error", null, null)
        collector.flush()

        assertEquals(1, delegate.entries.size)
        assertNull(delegate.entries[0].diagnosticId)
    }

    @Test
    fun `VERBOSE severity forwards diagnostic ID immediately`() {
        val delegate = RecordingMessageCollector()
        val collector = GroupingMessageCollector(delegate, false, false)

        collector.report(CompilerMessageSeverity.LOGGING, "verbose msg", null, "SOME_ID")

        // Should be forwarded immediately, before flush
        assertEquals(1, delegate.entries.size)
        assertEquals("SOME_ID", delegate.entries[0].diagnosticId)
    }

    // ---- FilteringMessageCollector tests ----

    @Test
    fun `filtering collector forwards diagnostic ID for accepted messages`() {
        val delegate = RecordingMessageCollector()
        val collector = FilteringMessageCollector(delegate) { it == CompilerMessageSeverity.WARNING }

        collector.report(CompilerMessageSeverity.ERROR, "an error", null, "UNRESOLVED_REFERENCE")

        assertEquals(1, delegate.entries.size)
        assertEquals("UNRESOLVED_REFERENCE", delegate.entries[0].diagnosticId)
    }

    @Test
    fun `filtering collector drops declined messages entirely`() {
        val delegate = RecordingMessageCollector()
        val collector = FilteringMessageCollector(delegate) { it == CompilerMessageSeverity.WARNING }

        collector.report(CompilerMessageSeverity.WARNING, "a warning", null, "DEPRECATION")

        assertEquals(0, delegate.entries.size)
    }

    @Test
    fun `filtering collector forwards null diagnostic ID`() {
        val delegate = RecordingMessageCollector()
        val collector = FilteringMessageCollector(delegate) { false }

        collector.report(CompilerMessageSeverity.ERROR, "an error", null, null)

        assertEquals(1, delegate.entries.size)
        assertNull(delegate.entries[0].diagnosticId)
    }

    // ---- Extension function dispatch tests ----

    @Test
    fun `extension dispatches to 4-arg report on MessageCollectorWithDiagnosticId`() {
        val collector = RecordingMessageCollector()
        val asBase: MessageCollector = collector

        asBase.report(CompilerMessageSeverity.ERROR, "msg", null, "MY_DIAGNOSTIC")

        assertEquals(1, collector.entries.size)
        assertEquals("MY_DIAGNOSTIC", collector.entries[0].diagnosticId)
    }

    @Test
    fun `extension dispatches to 3-arg report on plain MessageCollector`() {
        val collector = PlainRecordingMessageCollector()
        val asBase: MessageCollector = collector

        // Should not throw; diagnosticId is silently dropped
        asBase.report(CompilerMessageSeverity.ERROR, "msg", null, "MY_DIAGNOSTIC")

        assertEquals(1, collector.messages.size)
        assertEquals("msg", collector.messages[0])
    }
}
