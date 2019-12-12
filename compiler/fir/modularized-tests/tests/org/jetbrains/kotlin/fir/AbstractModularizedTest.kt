/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

data class ModuleData(
    val name: String,
    val rawOutputDir: String,
    val qualifier: String,
    val rawClasspath: List<String>,
    val rawSources: List<String>,
    val rawJavaSourceRoots: List<String>,
    val isCommon: Boolean
) {
    val qualifiedName get() = if (name in qualifier) qualifier else "$name.$qualifier"

    val outputDir = File(ROOT_PATH_PREFIX, rawOutputDir.removePrefix("/"))
    val classpath = rawClasspath.map { File(ROOT_PATH_PREFIX, it.removePrefix("/")) }
    val sources = rawSources.map { File(ROOT_PATH_PREFIX, it.removePrefix("/")) }
    val javaSourceRoots = rawJavaSourceRoots.map { File(ROOT_PATH_PREFIX, it.removePrefix("/")) }
}

private fun NodeList.toList(): List<Node> {
    val list = mutableListOf<Node>()
    for (index in 0 until this.length) {
        list += item(index)
    }
    return list
}


private val Node.childNodesList get() = childNodes.toList()

val ROOT_PATH_PREFIX = System.getProperty("fir.bench.prefix", "/")

abstract class AbstractModularizedTest : KtUsefulTestCase() {
    private val folderDateFormat = SimpleDateFormat("yyyy-MM-dd")
    private lateinit var startDate: Date

    protected fun reportDir() = File(ROOT_PATH_PREFIX).resolve(FIR_LOGS_PATH).resolve(folderDateFormat.format(startDate))
        .also {
            it.mkdirs()
        }

    protected val reportDateStr by lazy {
        val reportDateFormat = SimpleDateFormat("yyyy-MM-dd__HH-mm")
        reportDateFormat.format(startDate)
    }

    override fun setUp() {
        super.setUp()
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = false
        startDate = Date()
    }

    override fun tearDown() {
        super.tearDown()
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = true
    }

    private fun loadModule(file: File): ModuleData {

        val factory = DocumentBuilderFactory.newInstance()
        factory.isIgnoringComments = true
        factory.isIgnoringElementContentWhitespace = true
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(file)
        val moduleElement = document.childNodes.item(0).childNodesList.first { it.nodeType == Node.ELEMENT_NODE }
        val moduleName = moduleElement.attributes.getNamedItem("name").nodeValue
        val outputDir = moduleElement.attributes.getNamedItem("outputDir").nodeValue
        val moduleNameQualifier = outputDir.substringAfterLast("/")
        val javaSourceRoots = mutableListOf<String>()
        val classpath = mutableListOf<String>()
        val sources = mutableListOf<String>()
        var isCommon = false

        for (index in 0 until moduleElement.childNodes.length) {
            val item = moduleElement.childNodes.item(index)

            if (item.nodeName == "classpath") {
                val path = item.attributes.getNamedItem("path").nodeValue
                if (path != outputDir) {
                    classpath += path
                }
            }
            if (item.nodeName == "javaSourceRoots") {
                javaSourceRoots += item.attributes.getNamedItem("path").nodeValue
            }
            if (item.nodeName == "sources") {
                sources += item.attributes.getNamedItem("path").nodeValue
            }
            if (item.nodeName == "commonSources") {
                isCommon = true
            }
        }

        return ModuleData(moduleName, outputDir, moduleNameQualifier, classpath, sources, javaSourceRoots, isCommon)
    }


    protected abstract fun beforePass()
    protected abstract fun afterPass(pass: Int)
    protected open fun afterAllPasses() {}
    protected abstract fun processModule(moduleData: ModuleData): ProcessorAction

    protected fun runTestOnce(pass: Int) {
        beforePass()
        val testDataPath = System.getProperty("fir.bench.jps.dir")?.toString() ?: "/Users/jetbrains/jps"
        val root = File(testDataPath)

        println("BASE PATH: ${root.absolutePath}")

        val filterRegex = (System.getProperty("fir.bench.filter") ?: ".*").toRegex()
        val modules =
            root.listFiles().filter { it.extension == "xml" }
                .sortedBy { it.lastModified() }.map { loadModule(it) }
                .filter { it.rawOutputDir.matches(filterRegex) }
                .filter { !it.isCommon }


        for (module in modules.progress(step = 0.0) { "Analyzing ${it.qualifiedName}" }) {
            if (processModule(module).stop()) {
                break
            }
        }

        afterPass(pass)
    }
}
