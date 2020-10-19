/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.debugInformation

import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.Event
import com.sun.jdi.event.LocatableEvent
import org.jetbrains.kotlin.test.KotlinTestUtils.assertEqualsToFile
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

    override fun checkResult(wholeFile: File, loggedItems: List<Any>) {
        val actual = mutableListOf<String>()
        val lines = wholeFile.readLines()
        val forceStepInto = lines.any { it.startsWith(FORCE_STEP_INTO_MARKER) }

        val actualLineNumbers = compressRunsWithoutLinenumber(loggedItems as List<LocatableEvent>, LocatableEvent::location)
            .filter {
                val location = it.location()
                // Ignore synthetic code with no line number information
                // unless force step into behavior is requested.
                forceStepInto || !location.method().isSynthetic
            }
            .map { "// ${it.location().formatAsExpectation()}" }
        val actualLineNumbersIterator = actualLineNumbers.iterator()

        val lineIterator = lines.iterator()
        for (line in lineIterator) {
            actual.add(line)
            if (line.startsWith(LINENUMBERS_MARKER) || line.startsWith(FORCE_STEP_INTO_MARKER)) break
        }

        var currentBackend = TargetBackend.ANY
        for (line in lineIterator) {
            if (line.startsWith(LINENUMBERS_MARKER)) {
                actual.add(line)
                currentBackend = when (line) {
                    LINENUMBERS_MARKER -> TargetBackend.ANY
                    JVM_LINENUMBER_MARKER -> TargetBackend.JVM
                    JVM_IR_LINENUMBER_MARKER -> TargetBackend.JVM_IR
                    else -> error("Expected JVM backend: $line")
                }
                continue
            }
            if (currentBackend == TargetBackend.ANY || currentBackend == backend) {
                if (actualLineNumbersIterator.hasNext()) {
                    actual.add(actualLineNumbersIterator.next())
                }
            } else {
                actual.add(line)
            }
        }

        actualLineNumbersIterator.forEach { actual.add(it) }

        assertEqualsToFile(wholeFile, actual.joinToString("\n"))
    }
}

