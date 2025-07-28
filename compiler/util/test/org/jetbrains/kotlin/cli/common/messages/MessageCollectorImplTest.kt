/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages

import org.junit.Assert
import org.junit.Test

class MessageCollectorImplTest {
    @Test
    fun testReportAndGetMessages() {
        val collector = MessageCollectorImpl()
        
        // Test that initially the collector has no messages
        Assert.assertTrue(collector.messages.isEmpty())
        
        // Report a message and verify it's added
        collector.report(CompilerMessageSeverity.INFO, "Info message")
        Assert.assertEquals(1, collector.messages.size)
        Assert.assertEquals(CompilerMessageSeverity.INFO, collector.messages[0].severity)
        Assert.assertEquals("Info message", collector.messages[0].message)
        Assert.assertNull(collector.messages[0].location)
        
        // Report another message with location and verify it's added
        val location = CompilerMessageLocation.create("test.kt", 1, 1, null)
        collector.report(CompilerMessageSeverity.WARNING, "Warning message", location)
        Assert.assertEquals(2, collector.messages.size)
        Assert.assertEquals(CompilerMessageSeverity.WARNING, collector.messages[1].severity)
        Assert.assertEquals("Warning message", collector.messages[1].message)
        Assert.assertEquals(location, collector.messages[1].location)
    }
    
    @Test
    fun testHasErrors() {
        val collector = MessageCollectorImpl()
        
        // Test that initially there are no errors
        Assert.assertFalse(collector.hasErrors())
        
        // Report non-error messages and verify hasErrors() is still false
        collector.report(CompilerMessageSeverity.INFO, "Info message")
        collector.report(CompilerMessageSeverity.WARNING, "Warning message")
        Assert.assertFalse(collector.hasErrors())
        
        // Report an error message and verify hasErrors() is true
        collector.report(CompilerMessageSeverity.ERROR, "Error message")
        Assert.assertTrue(collector.hasErrors())
    }
    
    @Test
    fun testClear() {
        val collector = MessageCollectorImpl()
        
        // Add some messages
        collector.report(CompilerMessageSeverity.INFO, "Info message")
        collector.report(CompilerMessageSeverity.ERROR, "Error message")
        Assert.assertEquals(2, collector.messages.size)
        Assert.assertTrue(collector.hasErrors())
        
        // Clear the collector and verify it's empty
        collector.clear()
        Assert.assertTrue(collector.messages.isEmpty())
        Assert.assertFalse(collector.hasErrors())
    }
    
    @Test
    fun testForward() {
        val collector1 = MessageCollectorImpl()
        val collector2 = MessageCollectorImpl()
        
        // Add messages to collector1
        collector1.report(CompilerMessageSeverity.INFO, "Info message")
        val location = CompilerMessageLocation.create("test.kt", 1, 1, null)
        collector1.report(CompilerMessageSeverity.ERROR, "Error message", location)
        
        // Forward messages to collector2
        collector1.forward(collector2)
        
        // Verify collector2 has the same messages
        Assert.assertEquals(2, collector2.messages.size)
        Assert.assertEquals(CompilerMessageSeverity.INFO, collector2.messages[0].severity)
        Assert.assertEquals("Info message", collector2.messages[0].message)
        Assert.assertNull(collector2.messages[0].location)
        Assert.assertEquals(CompilerMessageSeverity.ERROR, collector2.messages[1].severity)
        Assert.assertEquals("Error message", collector2.messages[1].message)
        Assert.assertEquals(location, collector2.messages[1].location)
    }
    
    @Test
    fun testErrors() {
        val collector = MessageCollectorImpl()
        
        // Add various messages
        collector.report(CompilerMessageSeverity.INFO, "Info message")
        collector.report(CompilerMessageSeverity.WARNING, "Warning message")
        collector.report(CompilerMessageSeverity.ERROR, "Error message 1")
        collector.report(CompilerMessageSeverity.ERROR, "Error message 2")
        
        // Verify errors list contains only error messages
        Assert.assertEquals(2, collector.errors.size)
        Assert.assertEquals("Error message 1", collector.errors[0].message)
        Assert.assertEquals("Error message 2", collector.errors[1].message)
    }
    
    @Test
    fun testToString() {
        val collector = MessageCollectorImpl()
        
        // Empty collector
        Assert.assertEquals("", collector.toString())
        
        // Add a message
        collector.report(CompilerMessageSeverity.INFO, "Info message")
        Assert.assertEquals("info: Info message", collector.toString())
        
        // Add another message with location
        val location = CompilerMessageLocation.create("test.kt", 1, 1, null)
        collector.report(CompilerMessageSeverity.ERROR, "Error message", location)
        Assert.assertEquals("info: Info message\ntest.kt (1:1): error: Error message", collector.toString())
    }
    
    @Test
    fun testMessageToString() {
        // Test message without location
        val message1 = MessageCollectorImpl.Message(CompilerMessageSeverity.INFO, "Info message", null)
        Assert.assertEquals("info: Info message", message1.toString())
        
        // Test message with location
        val location = CompilerMessageLocation.create("test.kt", 1, 1, null)
        val message2 = MessageCollectorImpl.Message(CompilerMessageSeverity.ERROR, "Error message", location)
        Assert.assertEquals("test.kt (1:1): error: Error message", message2.toString())
    }
}