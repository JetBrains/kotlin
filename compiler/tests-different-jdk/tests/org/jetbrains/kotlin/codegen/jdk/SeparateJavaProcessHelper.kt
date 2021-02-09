/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.jdk

import org.jetbrains.kotlin.codegen.CodegenTestCase
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.test.assertTrue

object SeparateJavaProcessHelper {
    private lateinit var jdkProcess: Process

    private val lock = ReentrantLock()
    private var counter = 0

    fun setUp() {
        lock.withLock {
            if (counter == 0) {
                initJdkProcess()
            }
            counter += 1
        }
    }

    fun tearDown() {
        lock.withLock {
            counter -= 1
            if (counter == 0) {
                destroyJdkProcess()
            }
        }
    }

    private fun initJdkProcess() {
        println("Configuring JDK6 Test server...")
        val jdkPath = System.getenv("JDK_16") ?: error("JDK_16 is not optional to run this test")

        val executable = File(jdkPath, "bin/java").canonicalPath
        val main = "org.jetbrains.kotlin.test.clientserver.TestProcessServer"
        val classpath =
            System.getProperty("kotlin.test.box.in.separate.process.server.classpath") ?: System.getProperty("java.class.path")

        println("Server classpath: $classpath")
        val port = CodegenTestCase.BOX_IN_SEPARATE_PROCESS_PORT ?: error("kotlin.test.box.in.separate.process.port is not specified")
        val builder = ProcessBuilder(executable, "-cp", classpath, main, port)

        builder.inheritIO()

        println("Starting JDK 6 server $executable...")
        jdkProcess = builder.start()
        Thread.sleep(2000)
        assertTrue(jdkProcess.isAlive, "Test server process hasn't started")
        println("Test server started!")
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                destroyJdkProcess()
            }
        })
    }

    private fun destroyJdkProcess() {
        println("Stopping JDK 6 server...")
        if (::jdkProcess.isInitialized) {
            jdkProcess.destroy()
        }
    }
}
