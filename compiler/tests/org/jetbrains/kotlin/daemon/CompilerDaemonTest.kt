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

package org.jetbrains.kotlin.daemon

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.CliBaseTest
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.rmi.CompilerId
import org.jetbrains.kotlin.rmi.DaemonJVMOptions
import org.jetbrains.kotlin.rmi.DaemonOptions
import org.jetbrains.kotlin.rmi.kotlinr.KotlinCompilerClient
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.ByteArrayOutputStream
import java.io.File

val KOTLIN_DAEMON_TEST_PORT = 19753

public class CompilerDaemonTest : KotlinIntegrationTestBase() {

    data class CompilerResults(val resultCode: Int, val out: String)

    val daemonOptions = DaemonOptions(port = KOTLIN_DAEMON_TEST_PORT)
    val daemonLaunchingOptions = DaemonJVMOptions()
    val compilerId by lazy { CompilerId.makeCompilerId( File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-compiler.jar"),
                                                        File("dependencies/bootstrap-compiler/Kotlin/kotlinc/lib/kotlin-runtime.jar"),
                                                        File("dependencies/bootstrap-compiler/Kotlin/kotlinc/lib/kotlin-reflect.jar")) }

    private fun compileOnDaemon(args: Array<out String>): CompilerResults {
        val daemon = KotlinCompilerClient.connectToCompileService(compilerId, daemonLaunchingOptions, daemonOptions, System.err, autostart = true, checkId = true)
        TestCase.assertNotNull("failed to connect daemon", daemon)
        val strm = ByteArrayOutputStream()
        val code = KotlinCompilerClient.compile(daemon!!, args, strm)
        return CompilerResults(code, strm.toString())
    }

    private fun runDaemonCompilerTwice(logName: String, vararg arguments: String): Unit {
        KotlinCompilerClient.shutdownCompileService(daemonOptions)

        try {
            val res1 = compileOnDaemon(arguments)
            TestCase.assertEquals("first compilation failed:\n${res1.out}", 0, res1.resultCode)
            val res2 = compileOnDaemon(arguments)
            TestCase.assertEquals("second compilation failed:\n${res2.out}", 0, res2.resultCode)
            TestCase.assertEquals("build results differ", CliBaseTest.removePerfOutput(res1.out), CliBaseTest.removePerfOutput(res2.out))
            // TODO: add performance comparison assert
        }
        finally {
            KotlinCompilerClient.shutdownCompileService(daemonOptions)
        }
    }

    private fun getTestBaseDir(): String = JetTestUtils.getTestDataPathBase() + "/integration/smoke/" + getTestName(true)

    private fun run(logName: String, vararg args: String): Int = runJava(getTestBaseDir(), logName, *args)

    public fun testHelloApp() {
        val jar = tmpdir.absolutePath + File.separator + "hello.jar"

        runDaemonCompilerTwice("hello.compile", "-include-runtime", File(getTestBaseDir(), "hello.kt").absolutePath, "-d", jar)
        run("hello.run", "-cp", jar, "Hello.HelloPackage")
    }
}
