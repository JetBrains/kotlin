/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import java.io.File
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import kotlin.collections.plusAssign
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

data class ModuleData(
    val rootPath: String,
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
    val isCommon: Boolean,
) {
    val qualifiedName get() = if (name in qualifier) qualifier else "$name.$qualifier"

    val classpath = rawClasspath.map { it.fixPath(rootPath) }
    val sources = rawSources.map { it.fixPath(rootPath) }
    val javaSourceRoots = rawJavaSourceRoots.map { JavaSourceRootData(it.path.fixPath(rootPath), it.packagePrefix) }
    val friendDirs = rawFriendDirs.map { it.fixPath(rootPath) }
    val jdkHome = rawJdkHome?.fixPath(rootPath)
    val modularJdkRoot = rawModularJdkRoot?.fixPath(rootPath)

    /**
     * Raw compiler arguments, as it was passed to original module build
     */
    var arguments: CommonCompilerArguments? = null
}

data class JavaSourceRootData<Path : Any>(val path: Path, val packagePrefix: String?)

internal fun String.fixPath(rootPath: String): File = File(rootPath, this.removePrefix("/"))

internal fun loadModuleDumpFile(file: File, config: ModularizedTestConfig): List<ModuleData> {
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
                        val m = readModule(xr, config)
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

private fun readModule(xr: XMLStreamReader, config: ModularizedTestConfig): ModuleData {
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
                    config.rootPathPrefix,
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

