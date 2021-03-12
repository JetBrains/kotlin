/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.imltogradle

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsDependencyElement
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.module.JpsModuleDependency
import org.jetbrains.jps.model.module.JpsModuleSourceRoot
import org.jetbrains.jps.model.serialization.JpsProjectLoader
import java.io.File

const val KOTLIN_IDE_DIR_NAME = "kotlin-ide"

lateinit var kotlinIdeJpsModuleNameToGradleModuleNameMapping: Map<String, String>
lateinit var intellijModuleNameToGradleDependencyNotationsMapping: Map<String, List<String>>

val intellijModuleNameToGradleDependencyNotationsMappingManual = mapOf(
    "intellij.platform.debugger.testFramework" to listOf(""), // TODO()
    "intellij.completionMlRanking" to listOf(""),
    "intellij.platform.jps.model.tests" to listOf(""),
    "intellij.platform.externalSystem.tests" to listOf(""),
    "intellij.gradle.toolingExtension.tests" to listOf(""),
    "intellij.gradle.tests" to listOf(""),
)

fun main() {
    val imlFiles = File(".").canonicalFile.resolve(KOTLIN_IDE_DIR_NAME).walk()
        .filter { it.isFile && it.extension == "iml" && it.name.startsWith("kotlin.") }
        .toList()

    kotlinIdeJpsModuleNameToGradleModuleNameMapping = imlFiles.associate {
        Pair(it.nameWithoutExtension, ":" + it.parentFile.relativeTo(File(".").canonicalFile).path.replace("/", ":"))
    }

    intellijModuleNameToGradleDependencyNotationsMapping = intellijModuleNameToGradleDependencyNotationsMappingManual + listOf(
        object {}.javaClass.getResource("/ideaIU-project-structure-mapping.json"),
        object {}.javaClass.getResource("/intellij-core-project-structure-mapping.json")
    )
        .flatMap { jsonUrl ->
            val json = File(jsonUrl!!.toURI()).readText()
            return@flatMap JsonParser.parseString(json).asJsonArray.mapNotNull {
                IntelliJModuleNameToGradleDependencyNotationMappingItem.fromJsonObject(it.asJsonObject)
            }
        }
        .groupBy { it.moduleName }
        .mapValues { entry -> entry.value.map { it.gradleNotation } }

    imlFiles.zip(JpsProjectLoader.loadModules(imlFiles.map { it.toPath() }, null, mapOf()))
        .forEach { (imlFile, jpsModule) ->
            imlFile.parentFile.resolve("build.gradle.kts").writeText(convertJpsModule(imlFile, jpsModule))
        }
}

private data class IntelliJModuleNameToGradleDependencyNotationMappingItem(val moduleName: String, val gradleNotation: String) {
    companion object {
        const val artifactNameSubregex = """([a-zA-Z\-\._1-9]*?)"""

        val libPathToGradleNotationRegex = """^lib\/$artifactNameSubregex\.jar$""".toRegex()
        val pluginsPathToGradleNotationRegex = """^plugins\/$artifactNameSubregex\/.*?$""".toRegex()
        val jarToGradleNotationRegex = """^$artifactNameSubregex\.jar$""".toRegex()

        fun fromJsonObject(json: JsonObject): IntelliJModuleNameToGradleDependencyNotationMappingItem? {
            if (json.get("type").asString != "module-output") {
                return null
            }
            val jarPath = json.get("path").asString

            if (jarPath == "lib/cds/classesLogAgent.jar") {
                return null // TODO
            }

            fun Regex.group() = matchEntire(jarPath)?.groupValues?.get(1)

            val gradleNotation = pluginsPathToGradleNotationRegex.group()?.let { "intellijPluginDep(\"$it\")" }
                ?: libPathToGradleNotationRegex.group()?.let { "intellijDep(), { includeJars(\"$it\") }" }
                ?: jarToGradleNotationRegex.group()?.let { "intellijDep(), { includeJars(\"$it\") }" }
                ?: error("Path $jarPath matches none of the regexes")

            return IntelliJModuleNameToGradleDependencyNotationMappingItem(json.get("moduleName").asString, gradleNotation)
        }
    }
}

fun convertJpsModuleDependency(jpsModuleDependency: JpsModuleDependency): List<String> {
    val moduleName = jpsModuleDependency.moduleReference.moduleName
    if (moduleName.startsWith("kotlin.")) {
        return listOf("implementation(project(\"${kotlinIdeJpsModuleNameToGradleModuleNameMapping[moduleName]}\"))")
    }
    if (moduleName.startsWith("kotlinc.")) {
        return listOf("")
    }
    if (moduleName.startsWith("intellij.")) {
        return intellijModuleNameToGradleDependencyNotationsMapping[moduleName]
            ?.map { "implementation($it)" }
            ?: error("Cannot find mapping for intellij module name = $moduleName")
    }
    error("Unknown module dependency: $moduleName")
}

fun convertJpsDependencyElement(jpsDependencyElement: JpsDependencyElement): List<String> {
    return when (jpsDependencyElement) {
        is JpsModuleDependency -> convertJpsModuleDependency(jpsDependencyElement)
        else -> listOf("")
    }
}

fun convertJpsModuleSourceRoot(imlFile: File, sourceRoot: JpsModuleSourceRoot): String {
    return when (sourceRoot.rootType) {
        is JavaSourceRootType -> "java.srcDir(\"${sourceRoot.file.relativeTo(imlFile.parentFile)}\")";
        is JavaResourceRootType -> "resources.srcDir(\"${sourceRoot.file.relativeTo(imlFile.parentFile)}\")"
        else -> error("Unknown sourceRoot = $sourceRoot")
    }
}

fun convertJpsModule(imlFile: File, jpsModule: JpsModule): String {
    val dependencies = jpsModule.dependenciesList.dependencies

    val (src, test) = jpsModule.sourceRoots
        .groupBy { it.rootType.isForTests }
        .mapValues { entry -> entry.value.joinToString("\n") { convertJpsModuleSourceRoot(imlFile, it) } }
        .let { Pair(it[false] ?: "", it[true] ?: "") }

    val deps = dependencies.flatMap { convertJpsDependencyElement(it) }
        .distinct()
        .filter { it != "implementation()" } // TODO remove hack
        .joinToString("\n")
    return """
        |// GENERATED build.gradle.kts
        |// GENERATED BY ${imlFile.name}
        |// USE `./gradlew generateGradleByIml` TO REGENERATE THIS FILE
        |
        |plugins {
        |    kotlin("jvm")
        |    id("jps-compatible")
        |}
        |
        |dependencies {
        |    $deps
        |}
        |
        |sourceSets {
        |    "main" {
        |        $src
        |    }
        |    "test" {
        |        $test
        |    }
        |}
    """.trimMarginWithInterpolations()
}

fun String.trimMarginWithInterpolations(): String {
    val regex = Regex("""^(\s*\|)(\s*).*$""")
    val out = mutableListOf<String>()
    var prevIndent = ""
    for (line in lines()) {
        val matchResult = regex.matchEntire(line)
        if (matchResult != null) {
            out.add(line.removePrefix(matchResult.groupValues[1]))
            prevIndent = matchResult.groupValues[2]
        } else {
            out.add(prevIndent + line)
        }
    }
    return out.joinToString("\n").trim()
}
