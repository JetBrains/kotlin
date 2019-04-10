/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.CONTENT_ROOTS
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.measureNanoTime


private fun NodeList.toList(): List<Node> {
    val list = mutableListOf<Node>()
    for (index in 0 until this.length) {
        list += item(index)
    }
    return list
}

private val Node.childNodesList get() = childNodes.toList()


private data class XModuleData(val name: String, val classpath: List<File>, val sources: List<File>, val javaSourceRoots: List<File>)

class NonFirResolveModularizedTotalKotlinTest : KtUsefulTestCase() {


    private var totalTime = 0L
    private var files = 0

    private fun runAnalysis(moduleData: XModuleData, environment: KotlinCoreEnvironment) {
        val project = environment.project

        val time = measureNanoTime {
            KotlinToJVMBytecodeCompiler.analyze(environment, null)
        }

        files += environment.getSourceFiles().size
        totalTime += time
        println("Time is ${time * 1e-6} ms")

    }

    private fun processModule(moduleData: XModuleData) {
        val configurationKind = ConfigurationKind.ALL
        val testJdkKind = TestJdkKind.FULL_JDK


        val disposable = Disposer.newDisposable()


        val configuration =
            KotlinTestUtils.newConfiguration(configurationKind, testJdkKind, moduleData.classpath, moduleData.javaSourceRoots)
        configuration.addAll(
            CONTENT_ROOTS,
            moduleData.sources.filter { it.extension == "kt" }.map { KotlinSourceRoot(it.absolutePath, false) })
        configuration.put(MESSAGE_COLLECTOR_KEY, object : MessageCollector {
            override fun clear() {

            }

            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
                if (location != null)
                    print(location.toString())
                print(":")
                print(severity)
                print(":")
                println(message)
            }

            override fun hasErrors(): Boolean {
                return false
            }

        })
        val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

//        Extensions.getArea(environment.project)
//            .getExtensionPoint(PsiElementFinder.EP_NAME)
//            .unregisterExtension(JavaElementFinder::class.java)

        println("Processing module: ${moduleData.name}")
        runAnalysis(moduleData, environment)

        Disposer.dispose(disposable)
    }

    private fun loadModule(file: File): XModuleData {

        val factory = DocumentBuilderFactory.newInstance()
        factory.isIgnoringComments = true
        factory.isIgnoringElementContentWhitespace = true
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(file)
        val moduleElement = document.childNodes.item(0).childNodesList.first { it.nodeType == Node.ELEMENT_NODE }
        val moduleName = moduleElement.attributes.getNamedItem("name").nodeValue
        val javaSourceRoots = mutableListOf<File>()
        val classpath = mutableListOf<File>()
        val sources = mutableListOf<File>()

        for (index in 0 until moduleElement.childNodes.length) {
            val item = moduleElement.childNodes.item(index)

            if (item.nodeName == "classpath") {
                classpath += File(item.attributes.getNamedItem("path").nodeValue)
            }
            if (item.nodeName == "javaSourceRoots") {
                javaSourceRoots += File(item.attributes.getNamedItem("path").nodeValue)
            }
            if (item.nodeName == "sources") {
                sources += File(item.attributes.getNamedItem("path").nodeValue)
            }
        }

        return XModuleData(moduleName, classpath, sources, javaSourceRoots)
    }


    private fun runTestLocal() {
        val testDataPath = "/Users/jetbrains/jps"
        val root = File(testDataPath)

        println("BASE PATH: ${root.absolutePath}")

        val modules =
            root.listFiles().sortedBy { it.lastModified() }.map { loadModule(it) }

        // .sortedBy { !it.sources.any { it.nameWithoutExtension == "KotlinSearchEverywhereClassifier" } }


        for (module in modules.progress { "Analyzing ${it.name}" }) {
            processModule(module)
        }

        println("Total time: ${totalTime * 1e-6} ms, ${(totalTime * 1e-6) / files} ms per file")
        totalTime = 0
        files = 0
    }

    fun testTotalKotlin() {
        //Thread.sleep(5000)
        for (i in 0..2) {
            println("Pass $i")
            runTestLocal()
        }
    }
}