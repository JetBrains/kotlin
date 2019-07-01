/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.eval4j.jdi.test

import com.sun.jdi.*
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.ClassPrepareEvent
import junit.framework.TestCase
import junit.framework.TestSuite
import org.jetbrains.eval4j.*
import org.jetbrains.eval4j.jdi.JDIEval
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.eval4j.jdi.jdiObj
import org.jetbrains.eval4j.jdi.makeInitialFrame
import org.jetbrains.eval4j.test.buildTestSuite
import org.jetbrains.eval4j.test.getTestName
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

val DEBUGEE_CLASS = Debugee::class.java

fun suite(): TestSuite {
    val connectors = Bootstrap.virtualMachineManager().launchingConnectors()
    val connector = connectors[0]
    println("Using connector $connector")

    val connectorArgs = connector.defaultArguments()

    val debugeeName = DEBUGEE_CLASS.name
    println("Debugee name: $debugeeName")
    connectorArgs["main"]!!.setValue(debugeeName)
    connectorArgs["options"]!!.setValue("-classpath out/production/eval4j${File.pathSeparator}out/test/eval4j")
    connectorArgs["vmexec"]!!.setValue(connectorArgs["home"]!!.value() + File.separator + "bin" + File.separator + connectorArgs["vmexec"]!!.value())
    connectorArgs["home"]!!.setValue("")

    val vm = connector.launch(connectorArgs)!!

    val req = vm.eventRequestManager().createClassPrepareRequest()
    req.addClassFilter("*.Debugee")
    req.enable()

    val latch = CountDownLatch(1)
    var classLoader : ClassLoaderReference? = null
    var thread : ThreadReference? = null

    Thread {
        val eventQueue = vm.eventQueue()
        mainLoop@ while (true) {
            val eventSet = eventQueue.remove()
            for (event in eventSet.eventIterator()) {
                when (event) {
                    is ClassPrepareEvent -> {
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
                    is BreakpointEvent -> {
                        println("Suspended at: " + event.location())

                        thread = event.thread()
                        latch.countDown()

                        break@mainLoop
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
        object : TestCase(getTestName(methodNode.name)) {

            override fun runTest() {
                val eval = JDIEval(vm, classLoader!!, thread!!, 0)

                val args = if ((methodNode.access and Opcodes.ACC_STATIC) == 0) {
                    // Instance method
                    val newInstance = eval.newInstance(Type.getType(ownerClass))
                    val thisValue = eval.invokeMethod(newInstance, MethodDescription(ownerClass.name, "<init>", "()V", false), listOf(), true)
                    listOf(thisValue)
                }
                else {
                    listOf()
                }

                val value = interpreterLoop(
                        methodNode,
                        makeInitialFrame(methodNode, args),
                        eval
                )

                fun ObjectReference?.callToString(): String? {
                    if (this == null) return "null"
                    return (eval.invokeMethod(
                                                this.asValue(),
                                                MethodDescription(
                                                        "java/lang/Object",
                                                        "toString",
                                                        "()Ljava/lang/String;",
                                                        false
                                                ),
                                                listOf()).jdiObj as StringReference).value()

                }

                try {
                    if (expected is ValueReturned && value is ValueReturned && value.result is ObjectValue) {
                        assertEquals(expected.result.obj().toString(), value.result.jdiObj.callToString())
                    }
                    else if (expected is ExceptionThrown && value is ExceptionThrown) {
                        val valueObj = value.exception.obj()
                        val actual = if (valueObj is ObjectReference) valueObj.callToString() else valueObj.toString()
                        assertEquals(expected.exception.obj().toString(), actual)
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