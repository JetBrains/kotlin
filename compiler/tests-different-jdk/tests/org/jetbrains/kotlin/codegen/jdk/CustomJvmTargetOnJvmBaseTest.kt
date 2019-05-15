/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.jdk

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.CodegenTestCase.BOX_IN_SEPARATE_PROCESS_PORT
import org.jetbrains.kotlin.test.RunOnlyJdk6Test
import org.jetbrains.kotlin.test.SuiteRunnerForCustomJdk
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.junit.runners.Suite
import java.io.File
import kotlin.test.assertTrue

/*
 * NB: ALL NECESSARY FLAGS ARE PASSED THROUGH Gradle
 */

@Suite.SuiteClasses(
    BlackBoxCodegenTestGenerated::class,
    BlackBoxInlineCodegenTestGenerated::class,
    CompileKotlinAgainstInlineKotlinTestGenerated::class,
    CompileKotlinAgainstKotlinTestGenerated::class,
    BlackBoxAgainstJavaCodegenTestGenerated::class
)
abstract class CustomJvmTargetOnJvmBaseTest

@RunOnlyJdk6Test
@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTarget6OnJvm6 : CustomJvmTargetOnJvmBaseTest() {

    companion object {

        private lateinit var jdkProcess: Process

        @JvmStatic
        @BeforeClass
        fun setUp() {
            println("Configuring JDK6 Test server...")
            val jdkPath = System.getenv("JDK_16") ?: error("JDK_16 is not optional to run this test")

            val executable = File(jdkPath, "bin/java").canonicalPath
            val main = "org.jetbrains.kotlin.test.clientserver.TestProcessServer"
            val classpath =
                System.getProperty("kotlin.test.box.in.separate.process.server.classpath") ?: System.getProperty("java.class.path")

            println("Server classpath: $classpath")
            val port = BOX_IN_SEPARATE_PROCESS_PORT ?: error("kotlin.test.box.in.separate.process.port is not specified")
            val builder = ProcessBuilder(executable, "-cp", classpath, main, port)

            builder.inheritIO()

            println("Starting JDK 6 server $executable...")
            jdkProcess = builder.start()
            Thread.sleep(2000)
            assertTrue(jdkProcess.isAlive, "Test server process hasn't started")
            println("Test server started!")
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    tearDown()
                }
            })
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            println("Stopping JDK 6 server...")
            if (::jdkProcess.isInitialized) {
                jdkProcess.destroy()
            }
        }
    }
}

@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTarget8OnJvm8 : CustomJvmTargetOnJvmBaseTest()

@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTarget6OnJvm9 : CustomJvmTargetOnJvmBaseTest()

@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTarget8OnJvm9 : CustomJvmTargetOnJvmBaseTest()

@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTarget9OnJvm9 : CustomJvmTargetOnJvmBaseTest()

@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTarget6OnJvmLast : CustomJvmTargetOnJvmBaseTest()

@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTarget8OnJvmLast : CustomJvmTargetOnJvmBaseTest()

@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTargetLastOnJvmLast : CustomJvmTargetOnJvmBaseTest()
