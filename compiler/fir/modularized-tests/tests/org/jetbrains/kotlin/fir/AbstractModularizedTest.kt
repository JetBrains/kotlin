/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.types.AbstractTypeChecker
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

    /**
     * Raw compiler arguments, as it was passed to original module build
     */
    var arguments: CommonCompilerArguments? = null
}

data class JavaSourceRootData<Path : Any>(val path: Path, val packagePrefix: String?)

internal fun String.fixPath(): File = File(ROOT_PATH_PREFIX, this.removePrefix("/"))

private val ROOT_PATH_PREFIX:String = System.getProperty("fir.bench.prefix", "/")
private val OUTPUT_DIR_REGEX_FILTER:String = System.getProperty("fir.bench.filter", ".*")
private val MODULE_NAME_FILTER: String? = System.getProperty("fir.bench.filter.name")

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

    private fun loadModule(moduleElement: Element): ModuleData {
        val outputDir = moduleElement.getAttribute("outputDir").value
        val moduleName = moduleElement.getAttribute("name").value
        val moduleNameQualifier = outputDir.substringAfterLast("/")
        val javaSourceRoots = mutableListOf<JavaSourceRootData<String>>()
        val classpath = mutableListOf<String>()
        val sources = mutableListOf<String>()
        val friendDirs = mutableListOf<String>()
        val optInAnnotations = mutableListOf<String>()
        val timestamp = moduleElement.getAttribute("timestamp")?.longValue ?: 0
        val jdkHome = moduleElement.getAttribute("jdkHome")?.value
        var modularJdkRoot: String? = null
        var isCommon = false

        for (item in moduleElement.children) {
            when (item.name) {
                "classpath" -> {
                    val path = item.getAttribute("path").value
                    if (path != outputDir) {
                        classpath += path
                    }
                }
                "friendDir" -> {
                    val path = item.getAttribute("path").value
                    friendDirs += path
                }
                "javaSourceRoots" -> {
                    javaSourceRoots +=
                        JavaSourceRootData(
                            item.getAttribute("path").value,
                            item.getAttribute("packagePrefix")?.value,
                        )
                }
                "sources" -> sources += item.getAttribute("path").value
                "commonSources" -> isCommon = true
                "modularJdkRoot" -> modularJdkRoot = item.getAttribute("path").value
                "useOptIn" -> optInAnnotations += item.getAttribute("annotation").value
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

    private fun loadModuleDumpFile(file: File): List<ModuleData> {
        val rootElement = JDOMUtil.load(file)
        val modules = rootElement.getChildren("module")
        val arguments = rootElement.getChild("compilerArguments")?.let { loadCompilerArguments(it) }
        return modules.map { node -> loadModule(node).also { it.arguments = arguments } }
    }

    private fun loadCompilerArguments(argumentsRoot: Element): CommonCompilerArguments? {
        val element = argumentsRoot.children.singleOrNull() ?: return null
        return when (element.name) {
            "K2JVMCompilerArguments" -> K2JVMCompilerArguments().also { XmlSerializer.deserializeInto(it, element) }
            else -> null
        }
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

        val filterRegex = OUTPUT_DIR_REGEX_FILTER.toRegex()
        val moduleName = MODULE_NAME_FILTER
        val files = root.listFiles() ?: emptyArray()
        val modules = files.filter { it.extension == "xml" }
            .sortedBy { it.lastModified() }
            .flatMap { loadModuleDumpFile(it) }
            .sortedBy { it.timestamp }
            .filter { it.rawOutputDir.matches(filterRegex) }
            .filter { (moduleName == null) || it.name == moduleName }
            .filter { !it.isCommon }


        for (module in modules.progress(step = 0.0) { "Analyzing ${it.qualifiedName}" }) {
            if (processModule(module).stop()) {
                break
            }
        }

        afterPass(pass)
    }
}


internal fun K2JVMCompilerArguments.jvmTargetIfSupported(): JvmTarget? {
    val specified = jvmTarget?.let { JvmTarget.fromString(it) } ?: return null
    if (specified != JvmTarget.JVM_1_6) return specified
    return null
}