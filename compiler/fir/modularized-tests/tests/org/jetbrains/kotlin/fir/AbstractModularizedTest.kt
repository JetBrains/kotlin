/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.setupLanguageVersionSettings
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.test.KotlinTestUtils
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
    val timestamp: Long,
    val rawOutputDir: String,
    val qualifier: String,
    val rawClasspath: List<String>,
    val rawSources: List<String>,
    val rawJavaSourceRoots: List<JavaSourceRootData<String>>,
    val rawFriendDirs: List<String>,
    val optInAnnotations: List<String>,
    val rawModularJdkRoot: String?,
    val rawJdkHome: String?,
    val isCommon: Boolean
) {
    val qualifiedName get() = if (name in qualifier) qualifier else "$name.$qualifier"

    val outputDir = rawOutputDir.fixPath()
    val classpath = rawClasspath.map { it.fixPath() }
    val sources = rawSources.map { it.fixPath() }
    val javaSourceRoots = rawJavaSourceRoots.map { JavaSourceRootData(it.path.fixPath(), it.packagePrefix) }
    val friendDirs = rawFriendDirs.map { it.fixPath() }
    val jdkHome = rawJdkHome?.fixPath()
    val modularJdkRoot = rawModularJdkRoot?.fixPath()
}

data class JavaSourceRootData<Path : Any>(val path: Path, val packagePrefix: String?)

private fun String.fixPath(): File = File(ROOT_PATH_PREFIX, this.removePrefix("/"))

private fun NodeList.toList(): List<Node> {
    val list = mutableListOf<Node>()
    for (index in 0 until this.length) {
        list += item(index)
    }
    return list
}


private val Node.childNodesList get() = childNodes.toList()

private val ROOT_PATH_PREFIX = System.getProperty("fir.bench.prefix", "/")

abstract class AbstractModularizedTest : KtUsefulTestCase() {
    private val folderDateFormat = SimpleDateFormat("yyyy-MM-dd")
    private lateinit var reportDate: Date

    protected fun reportDir() = File(FIR_LOGS_PATH, folderDateFormat.format(reportDate))
        .also {
            it.mkdirs()
        }

    protected val reportDateStr: String by lazy {
        val reportDateFormat = SimpleDateFormat("yyyy-MM-dd__HH-mm")
        reportDateFormat.format(reportDate)
    }

    private fun detectReportDate(): Date {
        val provided = System.getProperty("fir.bench.report.timestamp") ?: return Date()
        return Date(provided.toLong())
    }

    override fun setUp() {
        super.setUp()
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = false
        reportDate = detectReportDate()
    }

    override fun tearDown() {
        super.tearDown()
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = true
    }

    fun createDefaultConfiguration(moduleData: ModuleData): CompilerConfiguration {
        val configuration = KotlinTestUtils.newConfiguration()
        moduleData.javaSourceRoots.forEach {
            configuration.addJavaSourceRoot(it.path, it.packagePrefix)
        }
        configuration.addJvmClasspathRoots(moduleData.classpath)
        configuration.languageVersionSettings = LanguageVersionSettingsImpl(
            LanguageVersion.LATEST_STABLE,
            ApiVersion.LATEST_STABLE,
            analysisFlags = mutableMapOf(AnalysisFlags.optIn to moduleData.optInAnnotations)
        )
        configuration.configureJdkClasspathRoots()

        // in case of modular jdk only
        configuration.putIfNotNull(JVMConfigurationKeys.JDK_HOME, moduleData.modularJdkRoot)

        configuration.addAll(
            CLIConfigurationKeys.CONTENT_ROOTS,
            moduleData.sources.filter { it.extension == "kt" || it.isDirectory }.map { KotlinSourceRoot(it.absolutePath, false) })
        return configuration
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
        val javaSourceRoots = mutableListOf<JavaSourceRootData<String>>()
        val classpath = mutableListOf<String>()
        val sources = mutableListOf<String>()
        val friendDirs = mutableListOf<String>()
        val optInAnnotations = mutableListOf<String>()
        val timestamp = moduleElement.attributes.getNamedItem("timestamp")?.nodeValue?.toLong() ?: 0
        val jdkHome = moduleElement.attributes.getNamedItem("jdkHome")?.nodeValue
        var modularJdkRoot: String? = null
        var isCommon = false

        for (index in 0 until moduleElement.childNodes.length) {
            val item = moduleElement.childNodes.item(index)

            when (item.nodeName) {
                "classpath" -> {
                    val path = item.attributes.getNamedItem("path").nodeValue
                    if (path != outputDir) {
                        classpath += path
                    }
                }
                "friendDir" -> {
                    val path = item.attributes.getNamedItem("path").nodeValue
                    friendDirs += path
                }
                "javaSourceRoots" -> {
                    javaSourceRoots +=
                        JavaSourceRootData(
                            item.attributes.getNamedItem("path").nodeValue,
                            item.attributes.getNamedItem("packagePrefix")?.nodeValue,
                        )
                }
                "sources" -> sources += item.attributes.getNamedItem("path").nodeValue
                "commonSources" -> isCommon = true
                "modularJdkRoot" -> modularJdkRoot = item.attributes.getNamedItem("path").nodeValue
                "useOptIn" -> optInAnnotations += item.attributes.getNamedItem("annotation").nodeValue
            }
        }

        return ModuleData(
            moduleName,
            timestamp,
            outputDir,
            moduleNameQualifier,
            classpath,
            sources,
            javaSourceRoots,
            friendDirs,
            optInAnnotations,
            modularJdkRoot,
            jdkHome,
            isCommon,
        )
    }


    protected abstract fun beforePass(pass: Int)
    protected abstract fun afterPass(pass: Int)
    protected open fun afterAllPasses() {}
    protected abstract fun processModule(moduleData: ModuleData): ProcessorAction

    protected fun runTestOnce(pass: Int) {
        beforePass(pass)
        val testDataPath = System.getProperty("fir.bench.jps.dir")?.toString() ?: "/Users/jetbrains/jps"
        val root = File(testDataPath)

        println("BASE PATH: ${root.absolutePath}")

        val filterRegex = (System.getProperty("fir.bench.filter") ?: ".*").toRegex()
        val files = root.listFiles() ?: emptyArray()
        val modules = files.filter { it.extension == "xml" }
            .sortedBy { it.lastModified() }
            .map { loadModule(it) }
            .sortedBy { it.timestamp }
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
