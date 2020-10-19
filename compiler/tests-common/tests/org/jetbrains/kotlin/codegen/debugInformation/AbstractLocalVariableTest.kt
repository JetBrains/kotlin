package org.jetbrains.kotlin.codegen.debugInformation

import com.sun.jdi.*
import com.sun.jdi.event.Event
import com.sun.jdi.event.LocatableEvent
import junit.framework.TestCase
import org.jetbrains.kotlin.test.TargetBackend
import org.junit.AfterClass
import org.junit.BeforeClass
import java.io.File

abstract class AbstractLocalVariableTest : AbstractDebugTest() {

    override val virtualMachine: VirtualMachine = Companion.virtualMachine
    override val proxyPort: Int = Companion.proxyPort

    interface LocalValue

    class LocalPrimitive(val value: String, val valueType: String) : LocalValue {
        override fun toString(): String {
            return "$value:$valueType"
        }
    }

    class LocalReference(val id: String, val referenceType: String) : LocalValue {
        override fun toString(): String {
            return "$referenceType"
        }
    }

    class LocalNullValue : LocalValue {
        override fun toString(): String {
            return "null"
        }
    }

    class LocalVariableRecord(
        val variable: String,
        val variableType: String,
        val value: LocalValue
    ) {
        override fun toString(): String {
            return "$variable:$variableType=$value"
        }
    }

    companion object {
        const val LOCAL_VARIABLES_MARKER = "// LOCAL VARIABLES"
        const val JVM_LOCAL_VARIABLES_MARKER = "$LOCAL_VARIABLES_MARKER JVM"
        const val JVM_IR_LOCAL_VARIABLES_MARKER = "$LOCAL_VARIABLES_MARKER JVM_IR"

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

    data class LVTStep(
        val location : Location,
        val visibleVars: Collection<LocalVariableRecord>
    )

    override fun storeStep(loggedItems: ArrayList<Any>, event: Event) {
        val locatableEvent = event as LocatableEvent
        waitUntil { locatableEvent.thread().isSuspended }
        val location = locatableEvent.location()
        if (location.method().isSynthetic) return
        val frame = locatableEvent.thread().frame(0)
        val visibleVars = try {
            frame.visibleVariables().map { variable -> toRecord(frame, variable) }
        } catch (e: AbsentInformationException) {
            // LVT Completely absent - not distinguished from an empty table
            listOf()
        }
        loggedItems.add(LVTStep(location, visibleVars))
    }

    override fun checkResult(wholeFile: File, loggedItems: List<Any>) {
        val lines = wholeFile.readLines()

        val expectedLocalVariables = mutableListOf<String>()
        val lineIterator = lines.iterator()
        for (line in lineIterator) {
            if (line.startsWith(LOCAL_VARIABLES_MARKER)) break
        }

        var currentBackend = TargetBackend.ANY
        for (line in lineIterator) {
            if (line.trim() == "") continue
            if (line.startsWith(LOCAL_VARIABLES_MARKER)) {
                currentBackend = when (line) {
                    LOCAL_VARIABLES_MARKER -> TargetBackend.ANY
                    JVM_LOCAL_VARIABLES_MARKER -> TargetBackend.JVM
                    JVM_IR_LOCAL_VARIABLES_MARKER -> TargetBackend.JVM_IR
                    else -> error("Expected JVM backend: $line")
                }
                continue
            }
            if (currentBackend == TargetBackend.ANY || currentBackend == backend) {
                expectedLocalVariables.add(line)
            }
        }

        val compressedLog = compressRunsWithoutLinenumber(loggedItems as List<LVTStep>, LVTStep::location)
        val actualLocalVariables = compressedLog.joinToString("\n") {
            "// ${it.location.formatAsExpectation()}: ${it.visibleVars.joinToString(", ")}".trim()
        }

        TestCase.assertEquals(expectedLocalVariables.joinToString("\n"), actualLocalVariables)
    }

    private fun toRecord(frame: StackFrame, variable: LocalVariable): LocalVariableRecord {
        val value = frame.getValue(variable)
        val valueRecord = if (value == null) {
            LocalNullValue()
        } else if (value is ObjectReference && value.referenceType().name() != "java.lang.String") {
            LocalReference(value.uniqueID().toString(), value.referenceType().name())
        } else {
            LocalPrimitive(value.toString(), value.type().name())
        }
        return LocalVariableRecord(variable.name(), variable.typeName(), valueRecord)
    }

    private fun waitUntil(condition: () -> Boolean) {
        while (!condition()) {
            Thread.sleep(10)
        }
    }
}