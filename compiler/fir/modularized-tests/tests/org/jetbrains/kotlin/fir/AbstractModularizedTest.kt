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
import org.jetbrains.kotlin.test.kotlinPathsForDistDirectoryForTests
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
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

private val ROOT_PATH_PREFIX: String = System.getProperty("fir.bench.prefix", "/")
private val OUTPUT_DIR_REGEX_FILTER: String = System.getProperty("fir.bench.filter", ".*")
private val MODULE_NAME_FILTER: String? = System.getProperty("fir.bench.filter.name")
private val MODULE_NAME_REGEX_OUT: String? = System.getProperty("fir.bench.filter.out.name")
private val CONTAINS_SOURCES_REGEX_FILTER: String? = System.getProperty("fir.bench.filter.contains.sources")
internal val ENABLE_SLOW_ASSERTIONS: Boolean = System.getProperty("fir.bench.enable.slow.assertions") == "true"

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
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = ENABLE_SLOW_ASSERTIONS
        reportDate = detectReportDate()
    }

    override fun tearDown() {
        super.tearDown()
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = true
    }

    protected abstract fun beforePass(pass: Int)
    protected abstract fun afterPass(pass: Int)
    protected open fun afterAllPasses() {}
    protected abstract fun processModule(moduleData: ModuleData): ProcessorAction

    protected fun runTestOnce(pass: Int) {
        beforePass(pass)
        val testDataPath = System.getProperty("fir.bench.jps.dir") ?: "/Users/jetbrains/jps"
        val root = File(testDataPath)

        println("BASE PATH: ${root.absolutePath}")

        val additionalMessages = mutableListOf<String>()

        val filterRegex = OUTPUT_DIR_REGEX_FILTER.toRegex()
        val moduleName = MODULE_NAME_FILTER
        val moduleNameRegexOutFilter = MODULE_NAME_REGEX_OUT?.toRegex()
        val containsSourcesFilter = CONTAINS_SOURCES_REGEX_FILTER?.toRegex()
        val files = root.listFiles() ?: emptyArray()
        val modules = files.filter {
            it.extension == "xml" && (moduleNameRegexOutFilter == null || !it.name.matches(moduleNameRegexOutFilter))
        }
            .sortedBy { it.lastModified() }
            .flatMap { loadModuleDumpFile(it) }
            .sortedBy { it.timestamp }
            .also { additionalMessages += "Discovered ${it.size} modules" }
            .filter { it.rawOutputDir.matches(filterRegex) }
            .also { additionalMessages += "Filtered by regex to ${it.size} modules" }
            .let { modules ->
                if (containsSourcesFilter != null) {
                    modules.filter { module ->
                        module.rawSources.any { s -> containsSourcesFilter.containsMatchIn(s) }
                    }.also { additionalMessages += "Filtered by source paths to ${it.size} modules" }
                } else modules
            }
            .filter { (moduleName == null) || it.name == moduleName }
            .also { additionalMessages += "Filtered by module name to ${it.size} modules" }
            .filter { !it.isCommon }
            .also { additionalMessages += "Filtered by common flag to ${it.size} modules" }

        if (modules.isEmpty() && IS_UNDER_TEAMCITY) {
            println("------------------------ Flakiness diagnostic ------------------------")
            println("No modules found for pattern `$OUTPUT_DIR_REGEX_FILTER` in `$testDataPath`")
            println("TestData root exists: ${root.exists()}")
            println(files.joinToString(prefix = "Content of testdata root:\n", separator = "\n") { it.absolutePath })
            println()
            additionalMessages.forEach { println(it) }
            println("------------------------------------------------")
        }


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

fun substituteCompilerPluginPathForKnownPlugins(path: String): File? {
    val file = File(path)
    val paths = PathUtil.kotlinPathsForDistDirectoryForTests
    return when {
        file.name.startsWith("kotlinx-serialization") || file.name.startsWith("kotlin-serialization") ->
            paths.jar(KotlinPaths.Jar.SerializationPlugin)
        file.name.startsWith("kotlin-sam-with-receiver") -> paths.jar(KotlinPaths.Jar.SamWithReceiver)
        file.name.startsWith("kotlin-allopen") -> paths.jar(KotlinPaths.Jar.AllOpenPlugin)
        file.name.startsWith("kotlin-noarg") -> paths.jar(KotlinPaths.Jar.NoArgPlugin)
        file.name.startsWith("kotlin-lombok") -> paths.jar(KotlinPaths.Jar.LombokPlugin)
        file.name.startsWith("kotlin-compose-compiler-plugin") -> {
            // compose plugin is not a part of the dist yet, so we have to go an extra mile to get it
            System.getProperty("fir.bench.compose.plugin.classpath")?.split(File.pathSeparator)?.firstOrNull()?.let(::File)
        }
        // Assuming that the rest is the custom compiler plugins, that cannot be kept stable with the new compiler, so we're skipping them
        // If the module is compillable without it - fine, otherwise at least it will hopefully be a stable failure.
        else -> null
    }
}

internal fun loadModuleDumpFile(file: File): List<ModuleData> {
    val rootElement = JDOMUtil.load(file)
    val modules = rootElement.getChildren("module")
    val arguments = rootElement.getChild("compilerArguments")?.let { loadCompilerArguments(it) }
    return modules.map { node -> loadModule(node).also { it.arguments = arguments } }
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

private fun loadCompilerArguments(argumentsRoot: Element): CommonCompilerArguments? {
    val element = argumentsRoot.children.singleOrNull() ?: return null
    return when (element.name) {
        "K2JVMCompilerArguments" -> K2JVMCompilerArguments().also { XmlSerializer.deserializeInto(it, element) }
        else -> null
    }
}

