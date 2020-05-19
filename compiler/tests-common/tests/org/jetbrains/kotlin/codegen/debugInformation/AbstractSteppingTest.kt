/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.debugInformation

import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.Event
import com.sun.jdi.event.LocatableEvent
import junit.framework.TestCase
import org.jetbrains.kotlin.test.TargetBackend
import org.junit.AfterClass
import org.junit.BeforeClass
import java.io.File

abstract class AbstractSteppingTest : AbstractDebugTest() {

    override val virtualMachine: VirtualMachine = Companion.virtualMachine
    override val proxyPort: Int = Companion.proxyPort

    companion object {
        const val LINENUMBERS_MARKER = "// LINENUMBERS"
        const val FORCE_STEP_INTO_MARKER = "// FORCE_STEP_INTO"
        const val JVM_LINENUMBER_MARKER = "$LINENUMBERS_MARKER JVM"
        const val JVM_IR_LINENUMBER_MARKER = "$LINENUMBERS_MARKER JVM_IR"
        var proxyPort = 0
        lateinit var process: Process
        lateinit var virtualMachine: VirtualMachine

        @BeforeClass
        @JvmStatic
        fun setUpTest() {
            val (process, port) = startDebuggeeProcess()
            this.process = process
            virtualMachine = attachDebugger(port)
            setUpVM(virtualMachine)

            proxyPort = getProxyPort(process)
        }

        @AfterClass
        @JvmStatic
        fun tearDownTest() {
            process.destroy()
        }
    }

    override fun storeStep(loggedItems: ArrayList<Any>, event: Event) {
        assert(event is LocatableEvent)
        loggedItems.add(event)
    }

    data class SteppingExpectations(val forceStepInto: Boolean, val lineNumbers: String)

    private fun readExpectations(wholeFile: File): SteppingExpectations {
        val expected = mutableListOf<String>()
        val lines = wholeFile.readLines().dropWhile {
            !it.startsWith(LINENUMBERS_MARKER) && !it.startsWith(FORCE_STEP_INTO_MARKER)
        }
        var forceStepInto = false
        var currentBackend = TargetBackend.ANY
        for (line in lines) {
            if (line.trim() == FORCE_STEP_INTO_MARKER) {
                forceStepInto = true
                continue
            }
            if (line.startsWith(LINENUMBERS_MARKER)) {
                currentBackend = when (line) {
                    LINENUMBERS_MARKER -> TargetBackend.ANY
                    JVM_LINENUMBER_MARKER -> TargetBackend.JVM
                    JVM_IR_LINENUMBER_MARKER -> TargetBackend.JVM_IR
                    else -> error("Expected JVM backend")
                }
                continue
            }
            if (currentBackend == TargetBackend.ANY || currentBackend == backend) {
                expected.add(line.drop(3).trim())
            }
        }
        return SteppingExpectations(forceStepInto, expected.joinToString("\n"))
    }

    override fun checkResult(wholeFile: File, loggedItems: List<Any>) {
        val (forceStepInto, expectedLineNumbers) = readExpectations(wholeFile)
        val actualLineNumbers = loggedItems
            .filter {
                val location = (it as LocatableEvent).location()
                // Ignore synthetic code with no line number information
                // unless force step into behavior is requested.
                forceStepInto || !location.method().isSynthetic
            }
            .map { event ->
                val location = (event as LocatableEvent).location()
                val synthetic = if (location.method().isSynthetic) " (synthetic)" else ""
                "${location.sourceName()}:${location.lineNumber()} ${location.method().name()}$synthetic"
            }
        TestCase.assertEquals(expectedLineNumbers, actualLineNumbers.joinToString("\n"))
    }
}

