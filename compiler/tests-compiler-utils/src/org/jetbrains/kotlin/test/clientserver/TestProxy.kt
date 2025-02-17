/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.clientserver

import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import java.net.URL
import org.jetbrains.kotlin.test.KtAssert.fail

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
                } else if (message == MessageHeader.ERROR) {
                    throw input.readObject() as Throwable
                } else {
                    fail("Unknown message: $message")
                }
            } finally {
                output.close()
                input.close()
            }
        }
    }

    fun runTestNoOutput(): String {
        Socket("localhost", serverPort).use { clientSocket ->
            val output = ObjectOutputStream(clientSocket.getOutputStream())
            try {
                output.writeObject(MessageHeader.NEW_TEST)
                output.writeObject(testClass)
                output.writeObject("box")

                output.writeObject(MessageHeader.CLASS_PATH)
                //filter out jdk libs
                output.writeObject(filterOutJdkJars(classPath).toTypedArray())
                return "OK"
            } finally {
                output.close()
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