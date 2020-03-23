/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.debugInformation

import com.sun.jdi.Location
import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.Event
import com.sun.jdi.event.LocatableEvent
import junit.framework.TestCase
import org.junit.AfterClass
import org.junit.BeforeClass
import java.io.File

abstract class AbstractSteppingTest : AbstractDebugTest() {

    override val virtualMachine: VirtualMachine = Companion.virtualMachine
    override val proxyPort: Int = Companion.proxyPort

    companion object {
        const val LINENUMBER_PREFIX = "// LINENUMBERS"
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

    private fun Location.toString(stratum: String): String =
        "${sourceName(stratum)}:${lineNumber(stratum)}"

    override fun checkResult(wholeFile: File, loggedItems: List<Any>) {
        val expectedLineNumbers = wholeFile
            .readLines()
            .dropWhile { !it.startsWith(LINENUMBER_PREFIX) }
            .drop(1).joinToString("\n") { it.drop(2).trim() }
        val linesWithCallSites = mutableMapOf<Any?, Set<Int>>()
        val actualLineNumbers = loggedItems.map { event ->
            val location = (event as LocatableEvent).location()
            val parentType = location.declaringType()
            // If a line number is not in KotlinDebug at all, calling `location.lineNumber("KotlinDebug")`
            // will produce some undefined garbage, so we need to check this first.
            val parentTypeCallSites = linesWithCallSites.getOrPut(parentType) {
                // If the KotlinDebug stratum is not present at all, `allLineLocations` will instead return
                // the default stratum, i.e. Kotlin; we don't want to treat *all* lines as call sites.
                if ("KotlinDebug" !in parentType.availableStrata())
                    setOf()
                else
                    parentType.allLineLocations("KotlinDebug", null).mapTo(mutableSetOf()) { it.lineNumber("Java") }
            }
            if (location.lineNumber("Java") in parentTypeCallSites)
                "${location.toString("Kotlin")} @ ${location.toString("KotlinDebug")}"
            else
                location.toString("Kotlin")
        }
        TestCase.assertEquals(expectedLineNumbers, actualLineNumbers.joinToString("\n"))
    }
}

