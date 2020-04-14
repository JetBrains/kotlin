package org.jetbrains.kotlin.codegen.debugInformation

import com.sun.jdi.*
import com.sun.jdi.event.Event
import com.sun.jdi.event.LocatableEvent
import com.sun.jdi.event.MethodEntryEvent
import junit.framework.TestCase
import org.junit.AfterClass
import org.junit.BeforeClass
import java.io.File

abstract class AbstractLocalVariableTest : AbstractDebugTest() {

    override val virtualMachine: VirtualMachine = Companion.virtualMachine
    override val proxyPort: Int = Companion.proxyPort

    companion object {
        const val LOCAL_VARIABLES = "// LOCAL VARIABLES"
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
        waitUntil { (event as LocatableEvent).thread().isSuspended }
        val frame = (event as LocatableEvent)
            .thread()
            .frame(0)
        try {
            val visibleInstanceFields = frame.thisObject()?.referenceType()?.visibleFields()
                ?.map { field -> toRecord(event.thread().frame(0), field) }
                ?: listOf()
            val visibleVars = frame.visibleVariables()
                .map { variable -> toRecord(event.thread().frame(0), variable) }
            loggedItems.add("${event.location()}: ${(visibleInstanceFields + visibleVars).joinToString(", ")}".trim())
        } catch (e: AbsentInformationException) {
            loggedItems.add(("${event.location()}: JDI Exception, no local variable information for method ${event.location().method()}").trim())
        }

    }

    override fun checkResult(wholeFile: File, loggedItems: List<Any>) {
        val expectedLocalVariables = wholeFile
            .readLines()
            .dropWhile { !it.startsWith(LOCAL_VARIABLES) }
            .drop(1)
            .map { it.drop(3) }
            .joinToString("\n")
        val actualLocalVariables = loggedItems.joinToString("\n")

        TestCase.assertEquals(expectedLocalVariables, actualLocalVariables)
    }

    private fun toRecord(frame: StackFrame, variable: LocalVariable): String {
        return "LV:${variable.name()}:${frame.getValue(variable)?.type()?.name() ?: "null"}"
    }

    private fun toRecord(frame: StackFrame, field: Field): String {
        return "F:${field.name()}:${frame.thisObject().getValue(field)?.type()?.name() ?: "null"}"
    }

    private fun waitUntil(condition: () -> Boolean) {
        while (!condition()) {
            Thread.sleep(10)
        }
    }
}