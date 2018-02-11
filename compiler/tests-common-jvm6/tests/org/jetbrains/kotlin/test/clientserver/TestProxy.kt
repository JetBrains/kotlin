/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test.clientserver

import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import java.net.URL
import kotlin.test.fail

class TestProxy(val serverPort: Int, val testClass: String, val classPath: List<URL>) {

    fun runTest(): String {
        return Socket("localhost", serverPort).use { clientSocket ->

            val output = ObjectOutputStream(clientSocket.getOutputStream())
            val input = ObjectInputStream(clientSocket.getInputStream())
            try {
                output.writeObject(MessageHeader.NEW_TEST)
                output.writeObject(testClass)
                output.writeObject("box")

                output.writeObject(MessageHeader.CLASS_PATH)
                //filter out jdk libs
                output.writeObject(filterOutJdkJars(classPath).toTypedArray())

                val message = input.readObject() as MessageHeader
                if (message == MessageHeader.RESULT) {
                    input.readObject() as String
                }
                else if (message == MessageHeader.ERROR) {
                    throw input.readObject() as Exception
                }
                else {
                    fail("Unknown message: $message")
                }
            } finally {
                output.close()
                input.close()
            }
        }
    }

    fun filterOutJdkJars(classPath: List<URL>): List<URL> {
        val javaHome = System.getProperty("java.home")
        val javaFolder = File(javaHome)
        return classPath.filterNot {
            File(it.file).startsWith(javaFolder)
        }
    }
}