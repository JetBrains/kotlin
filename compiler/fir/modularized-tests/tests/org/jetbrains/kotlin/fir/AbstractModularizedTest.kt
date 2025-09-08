/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
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
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

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
    val modules = mutableListOf<ModuleData>()
    var arguments: CommonCompilerArguments? = null

    val xmlFactory = XMLInputFactory.newInstance()
    file.inputStream().use { input ->
        val xr = xmlFactory.createXMLStreamReader(input)
        while (xr.hasNext()) {
            when (xr.next()) {
                XMLStreamConstants.START_ELEMENT -> when (xr.localName) {
                    "compilerArguments" -> {
                        arguments = readCompilerArguments(xr)
                        // Assign to already parsed modules as well
                        modules.forEach { it.arguments = arguments }
                    }
                    "module" -> {
                        val m = readModule(xr)
                        m.arguments = arguments
                        modules += m
                    }
                    else -> {}
                }
                else -> {}
            }
        }
        xr.close()
    }
    return modules
}

private fun readModule(xr: XMLStreamReader): ModuleData {
    // reader is positioned at START_ELEMENT <module>
    val outputDir = xr.getAttributeValue(null, "outputDir") ?: ""
    val moduleName = xr.getAttributeValue(null, "name") ?: ""
    val moduleNameQualifier = outputDir.substringAfterLast("/")
    val timestamp = xr.getAttributeValue(null, "timestamp")?.toLongOrNull() ?: 0L
    val jdkHome = xr.getAttributeValue(null, "jdkHome")

    val javaSourceRoots = mutableListOf<JavaSourceRootData<String>>()
    val classpath = mutableListOf<String>()
    val sources = mutableListOf<String>()
    val friendDirs = mutableListOf<String>()
    val optInAnnotations = mutableListOf<String>()
    var modularJdkRoot: String? = null
    var isCommon = false

    while (xr.hasNext()) {
        when (xr.next()) {
            XMLStreamConstants.START_ELEMENT -> when (xr.localName) {
                "classpath" -> {
                    val path = xr.getAttributeValue(null, "path")
                    if (path != null && path != outputDir) classpath += path
                    skipElement(xr)
                }
                "friendDir" -> {
                    xr.getAttributeValue(null, "path")?.let { friendDirs += it }
                    skipElement(xr)
                }
                "javaSourceRoots" -> {
                    val path = xr.getAttributeValue(null, "path")
                    val pkg = xr.getAttributeValue(null, "packagePrefix")
                    if (path != null) javaSourceRoots += JavaSourceRootData(path, pkg)
                    skipElement(xr)
                }
                "sources" -> {
                    xr.getAttributeValue(null, "path")?.let { sources += it }
                    skipElement(xr)
                }
                "commonSources" -> {
                    isCommon = true
                    skipElement(xr)
                }
                "modularJdkRoot" -> {
                    modularJdkRoot = xr.getAttributeValue(null, "path")
                    skipElement(xr)
                }
                "useOptIn" -> {
                    xr.getAttributeValue(null, "annotation")?.let { optInAnnotations += it }
                    skipElement(xr)
                }
                else -> {
                    // Skip any unknown children fully
                    skipElement(xr)
                }
            }
            XMLStreamConstants.END_ELEMENT -> if (xr.localName == "module") {
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
        }
    }
    error("Unexpected end of XML while reading <module>")
}

private fun readCompilerArguments(xr: XMLStreamReader): CommonCompilerArguments {
    // reader is positioned at START_ELEMENT <compilerArguments>
    val argList = mutableListOf<String>()
    var oldFormatArgs: CommonCompilerArguments? = null
    while (xr.hasNext()) {
        when (xr.next()) {
            XMLStreamConstants.START_ELEMENT -> {
                when (xr.localName) {
                    "arg" -> {
                        xr.getAttributeValue(null, "value")?.let { argList += it }
                        skipElement(xr)
                    }
                    // Old variant that was serialized with com.intellij.util.xmlb + org.jdom
                    "K2JVMCompilerArguments" -> {
                        oldFormatArgs = readCompilerArgumentsOldVariant(xr)
                    }
                    else -> {
                        // Unknown child, skip it entirely to keep parser in sync
                        skipElement(xr)
                    }
                }
            }
            XMLStreamConstants.END_ELEMENT -> if (xr.localName == "compilerArguments") {
                return oldFormatArgs ?: parseCommandLineArguments<K2JVMCompilerArguments>(argList)
            }
        }
    }
    error("Unexpected end of XML while reading <compilerArguments>")
}

// Old XML model variant deserializer using StAX and reflection. (serialized with com.intellij.util.xmlb + org.jdom)
// TODO: drop when no longer needed (KT-80860)
private fun readCompilerArgumentsOldVariant(xr: XMLStreamReader): CommonCompilerArguments {
    // reader is positioned at START_ELEMENT <K2JVMCompilerArguments>
    val args = K2JVMCompilerArguments()

    fun setOption(name: String, value: String) {
        setOptionReflective(args, name, value)
    }

    fun setOptionArray(name: String, values: List<String>) {
        setOptionReflective(args, name, values)
    }

    while (xr.hasNext()) {
        when (xr.next()) {
            XMLStreamConstants.START_ELEMENT -> when (xr.localName) {
                "option" -> {
                    val name = xr.getAttributeValue(null, "name") ?: run {
                        skipElement(xr)
                        continue
                    }
                    val valueAttr = xr.getAttributeValue(null, "value")
                    if (valueAttr != null) {
                        setOption(name, valueAttr)
                        skipElement(xr)
                    } else {
                        // The value is specified via nested <array><option value="..."/></array> or <list><option value="..."/></list>
                        var collected: MutableList<String>? = null
                        loop@ while (xr.hasNext()) {
                            when (xr.next()) {
                                XMLStreamConstants.START_ELEMENT -> if (xr.localName == "array" || xr.localName == "list") {
                                    collected = readStringArray(xr)
                                } else {
                                    skipElement(xr)
                                }
                                XMLStreamConstants.END_ELEMENT -> if (xr.localName == "option") {
                                    break@loop
                                }
                            }
                        }
                        if (collected != null) setOptionArray(name, collected)
                    }
                }
                else -> {
                    skipElement(xr)
                }
            }
            XMLStreamConstants.END_ELEMENT -> if (xr.localName == "K2JVMCompilerArguments") {
                return args
            }
        }
    }
    error("Unexpected end of XML while reading <K2JVMCompilerArguments>")
}

private fun readStringArray(xr: XMLStreamReader): MutableList<String> {
    // reader is positioned at START_ELEMENT <array>/<list>
    val result = mutableListOf<String>()
    while (xr.hasNext()) {
        when (xr.next()) {
            XMLStreamConstants.START_ELEMENT -> if (xr.localName == "option") {
                xr.getAttributeValue(null, "value")?.let { result += it }
                skipElement(xr)
            } else {
                skipElement(xr)
            }
            XMLStreamConstants.END_ELEMENT -> if (xr.localName == "array" || xr.localName == "list") return result
        }
    }
    error("Unexpected end of XML while reading <array>")
}

private fun setOptionReflective(args: Any, name: String, value: Any) {
    // Use Kotlin reflection to find a mutable property and set it with basic type conversions.
    val anyProp = args::class.memberProperties.firstOrNull { it.name == name } ?: return

    @Suppress("UNCHECKED_CAST")
    val prop = anyProp as? KMutableProperty1<Any, Any?> ?: return

    val returnType = prop.returnType
    val classifier = returnType.classifier as? KClass<*>

    val converted: Any? = try {
        when (classifier) {
            Boolean::class -> when (value) {
                is Boolean -> value
                is String -> value.toBooleanStrictOrNull() ?: value.equals("true", ignoreCase = true)
                else -> false
            }
            String::class -> value.toString()
            Array<Any>::class, Array<String>::class, Array::class -> {
                // Expecting Array<String>
                val componentIsString = returnType.arguments.firstOrNull()?.type?.classifier == String::class
                if (componentIsString) {
                    when (value) {
                        is List<*> -> value.filterIsInstance<String>().toTypedArray()
                        is Array<*> -> value.filterIsInstance<String>().toTypedArray()
                        is String -> arrayOf(value)
                        else -> emptyArray<String>()
                    }
                } else null
            }
            List::class, MutableList::class, Collection::class -> {
                // Support List<String> and MutableList<String> properties
                val elementClassifier = returnType.arguments.firstOrNull()?.type?.classifier as? KClass<*>
                if (elementClassifier == String::class) {
                    when (value) {
                        is List<*> -> value.filterIsInstance<String>()
                        is Array<*> -> value.filterIsInstance<String>()
                        is String -> listOf(value)
                        else -> emptyList()
                    }
                } else null
            }
            else -> value
        }
    } catch (_: Throwable) {
        null
    }

    try {
        prop.setter.call(args, converted)
    } catch (_: Throwable) {
        // ignore to keep compatibility if types don't match
    }
}

private fun skipElement(xr: XMLStreamReader) {
    // Assumes the reader is at START_ELEMENT; consumes until matching END_ELEMENT
    var depth = 1
    while (depth > 0 && xr.hasNext()) {
        when (xr.next()) {
            XMLStreamConstants.START_ELEMENT -> depth++
            XMLStreamConstants.END_ELEMENT -> depth--
        }
    }
}

