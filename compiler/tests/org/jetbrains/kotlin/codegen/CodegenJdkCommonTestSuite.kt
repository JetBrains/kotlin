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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.test.clientserver.TestProcessServer
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.junit.AfterClass
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.junit.runners.Suite
import java.io.File

/**
 * This suite is used to run java codegen tests under jdk 1.6, 1.8 and 9
 * with different default targets - 1.6, 1.8, 1.9.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
        BlackBoxCodegenTestGenerated::class,
        BlackBoxInlineCodegenTestGenerated::class,
        CompileKotlinAgainstInlineKotlinTestGenerated::class,
        CompileKotlinAgainstKotlinTestGenerated::class,
        BlackBoxAgainstJavaCodegenTestGenerated::class
)
object CodegenJdkCommonTestSuite {

    private var jdkProcess: Process? = null

    @BeforeClass
    @JvmStatic
    fun setUp() {
        val boxInSeparateProcessPort = System.getProperty(CodegenTestCase.RUN_BOX_TEST_IN_SEPARATE_PROCESS_PORT)
        if (boxInSeparateProcessPort != null) {
            val classpath = "out/test/tests-common" +
                            File.pathSeparatorChar +
                            ForTestCompileRuntime.runtimeJarForTests() +
                            File.pathSeparatorChar +
                            ForTestCompileRuntime.kotlinTestJarForTests()

            val jdk16 = System.getenv("JDK_16")
            assertNotNull(jdk16, "Please specify JDK_16 system property to run codegen test in separate process")

            val builder = ProcessBuilder(
                    jdk16 + "/bin/java", "-cp", classpath,
                    TestProcessServer::class.java.name, boxInSeparateProcessPort
            )
            println("Starting separate process to run test: " + builder.command().joinToString())
            builder.inheritIO()
            builder.redirectErrorStream(true)
            jdkProcess = builder.start()
        }
    }

    @AfterClass
    @JvmStatic
    fun tearDown() {
        if (jdkProcess != null) {
            println("Stop separate test process")
            jdkProcess!!.destroy()
        }
    }
}
