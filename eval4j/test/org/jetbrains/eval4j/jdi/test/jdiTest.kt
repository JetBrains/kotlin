package org.jetbrains.eval4j.jdi.test

import org.jetbrains.eval4j.*
import com.sun.jdi
import junit.framework.TestSuite
import org.jetbrains.eval4j.test.buildTestSuite
import junit.framework.TestCase
import org.jetbrains.eval4j.interpreterLoop
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import org.jetbrains.eval4j.ExceptionThrown
import org.jetbrains.eval4j.MethodDescription
import org.jetbrains.eval4j.ValueReturned
import org.jetbrains.eval4j.jdi.*

val DEBUGEE_CLASS = javaClass<Debugee>()

fun suite(): TestSuite {
    val connectors = jdi.Bootstrap.virtualMachineManager().launchingConnectors()
    val connector = connectors[0]
    println("Using connector $connector")

    val connectorArgs = connector.defaultArguments()

    val debugeeName = DEBUGEE_CLASS.getName()
    println("Debugee name: $debugeeName")
    connectorArgs["main"]!!.setValue(debugeeName)
    connectorArgs["options"]!!.setValue("-classpath out/production/eval4j:out/test/eval4j")
    val vm = connector.launch(connectorArgs)!!

    val req = vm.eventRequestManager().createClassPrepareRequest()
    req.addClassFilter("*.Debugee")
    req.enable()

    val latch = CountDownLatch(1)
    var classLoader : jdi.ClassLoaderReference? = null
    var thread : jdi.ThreadReference? = null

    Thread {
        val eventQueue = vm.eventQueue()
        @mainLoop while (true) {
            val eventSet = eventQueue.remove()
            for (event in eventSet.eventIterator()) {
                when (event) {
                    is jdi.event.ClassPrepareEvent -> {
                        val _class = event.referenceType()!!
                        if (_class.name() == debugeeName) {
                            for (l in _class.allLineLocations()) {
                                if (l.method().name() == "main") {
                                    classLoader = l.method().declaringType().classLoader()
                                    val breakpointRequest = vm.eventRequestManager().createBreakpointRequest(l)
                                    breakpointRequest.enable()
                                    println("Breakpoint: $breakpointRequest")
                                    vm.resume()
                                    break
                                }
                            }
                        }
                    }
                    is jdi.event.BreakpointEvent -> {
                        println("Suspended at: " + event.location())

                        thread = event.thread()
                        latch.countDown()

                        break @mainLoop
                    }
                    else -> {}
                }
            }
        }
    }.start()

    vm.resume()

    latch.await()

    var remainingTests = AtomicInteger(0)

    val suite = buildTestSuite {
        methodNode, ownerClass, expected ->
        remainingTests.incrementAndGet()
        object : TestCase("test" + methodNode.name.capitalize()) {

            override fun runTest() {
                val eval = JDIEval(
                        vm, classLoader!!, thread!!
                )
                val value = interpreterLoop(
                        methodNode,
                        makeInitialFrame(methodNode, listOf()),
                        eval
                )

                fun jdi.ObjectReference?.callToString(): String? {
                    if (this == null) return "null"
                    return (eval.invokeMethod(
                                                this.asValue(),
                                                MethodDescription(
                                                        "java/lang/Object",
                                                        "toString",
                                                        "()Ljava/lang/String;",
                                                        false
                                                ),
                                                listOf()).jdiObj as jdi.StringReference).value()

                }

                try {
                    if (value is ExceptionThrown) {
                        val str = value.exception.jdiObj.callToString()
                        System.err.println("Exception: $str")
                    }

                    if (expected is ValueReturned && value is ValueReturned && value.result is ObjectValue) {
                        assertEquals(expected.result.obj.toString(), value.result.jdiObj.callToString())
                    }
                    else {
                        assertEquals(expected, value)
                    }
                }
                finally {
                    if (remainingTests.decrementAndGet() == 0) vm.resume()
                }

            }
        }
    }

    return suite
}