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

package org.jetbrains.kotlin.android.tests

import com.intellij.util.PlatformUtils
import junit.framework.TestCase
import junit.framework.TestSuite
import org.jetbrains.kotlin.android.tests.download.SDKDownloader
import org.jetbrains.kotlin.android.tests.emulator.Emulator
import org.jetbrains.kotlin.android.tests.gradle.GradleRunner
import org.jetbrains.kotlin.android.tests.run.PermissionManager
import org.junit.Assert
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

class CodegenTestsOnAndroidRunner private constructor(private val pathManager: PathManager) {

    private fun runTestsInEmulator(): TestSuite {
        val rootSuite = TestSuite("Root")

        downloadDependencies()
        val emulator = Emulator(pathManager, Emulator.ARM)
        emulator.createEmulator()

        val gradleRunner = GradleRunner(pathManager)
        //old dex
        cleanAndBuildProject(gradleRunner)

        try {
            emulator.startEmulator()

            try {
                emulator.waitEmulatorStart()

                runTestsOnEmulator(gradleRunner, TestSuite("Dex")).apply {
                    rootSuite.addTest(this)
                }

                renameReport()
                enableD8(true)
                runTestsOnEmulator(gradleRunner, TestSuite("D8")).apply {
                    (0 until this.countTestCases()).forEach {
                        val testCase = testAt(it) as TestCase
                        testCase.name += "_D8"
                    }
                    rootSuite.addTest(this)
                }
            } catch (e: RuntimeException) {
                e.printStackTrace()
                throw e
            } finally {
                emulator.stopEmulator()
            }
        } catch (e: RuntimeException) {
            e.printStackTrace()
            throw e
        } finally {
            emulator.finishEmulatorProcesses()
        }

        return rootSuite
    }

    private fun enableD8(enable: Boolean) {
        val file = File(pathManager.androidTmpFolder, "gradle.properties")
        val lines = file.readLines().map {
            if (it.startsWith("android.enableD8=")) {
                "android.enableD8=$enable"
            } else it
        }
        file.writeText(lines.joinToString("\n"))
    }

    private fun processReport(suite: TestSuite, resultOutput: String) {
        val reportFolder = reportFolder()
        try {
            val testCases = parseSingleReportInFolder(reportFolder)
            testCases.forEach { aCase -> suite.addTest(aCase) }
            Assert.assertNotEquals("There is no test results in report", 0, testCases.size.toLong())
        } catch (e: Exception) {
            throw RuntimeException("Can't parse test results in $reportFolder\n$resultOutput", e)
        }
    }

    private fun renameReport() {
        val reportFolder = File(reportFolder())
        reportFolder.renameTo(File(reportFolder.parentFile, reportFolder.name + "_dex"))
    }

    private fun reportFolder() = pathManager.tmpFolder + "/build/test/results/connected/"

    private fun runTestsOnEmulator(gradleRunner: GradleRunner, suite: TestSuite): TestSuite {
        val platformPrefixProperty = System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, "Idea")
        try {
            val resultOutput = gradleRunner.connectedDebugAndroidTest()
            processReport(suite, resultOutput)
            return suite
        } finally {
            if (platformPrefixProperty != null) {
                System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, platformPrefixProperty)
            } else {
                System.clearProperty(PlatformUtils.PLATFORM_PREFIX_KEY)
            }
        }

    }

    private fun downloadDependencies() {
        val rootForAndroidDependencies = File(pathManager.dependenciesRoot)
        if (!rootForAndroidDependencies.exists()) {
            rootForAndroidDependencies.mkdirs()
        }

        val downloader = SDKDownloader(pathManager)
        downloader.downloadAll()
        downloader.unzipAll()
        PermissionManager.setPermissions(pathManager)
    }

    companion object {

        @JvmStatic
        fun runTestsInEmulator(pathManager: PathManager): TestSuite {
            return CodegenTestsOnAndroidRunner(pathManager).runTestsInEmulator()
        }

        private fun cleanAndBuildProject(gradleRunner: GradleRunner) {
            gradleRunner.clean()
            gradleRunner.build()
        }

        @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
        private fun parseSingleReportInFolder(reportFolder: String): List<TestCase> {
            val folder = File(reportFolder)
            val files = folder.listFiles()!!
            assert(files.size == 1) {
                "Expecting one file but ${files.size}: ${files.joinToString { it.name }}"
            }
            val reportFile = files[0]

            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(reportFile)
            val root = doc.documentElement
            val testCases = root.getElementsByTagName("testcase")

            return (0 until testCases.length).map { i ->
                val item = testCases.item(i) as Element
                val failure = item.getElementsByTagName("failure")
                val name = item.getAttribute("name")
                val clazz = item.getAttribute("classname")

                if (failure.length == 0) {
                    object : TestCase(name) {
                        @Throws(Throwable::class)
                        override fun runTest() {

                        }
                    }
                } else {
                    object : TestCase(name) {
                        @Throws(Throwable::class)
                        override fun runTest() {
                            Assert.fail(failure.item(0).textContent)
                        }
                    }
                }
            }
        }
    }
}
