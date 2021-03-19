/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.imltogradle

import com.google.gson.JsonParser
import org.jdom.Element
import org.jetbrains.jps.model.JpsSimpleElement
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.impl.JpsJavaExtensionServiceImpl
import org.jetbrains.jps.model.library.JpsLibrary
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor
import org.jetbrains.jps.model.module.*
import org.jetbrains.jps.model.serialization.JpsProjectLoader
import org.jetbrains.jps.model.serialization.library.JpsLibraryTableSerializer
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer
import java.io.File

const val KOTLIN_IDE_DIR_NAME = "kotlin-ide"

private lateinit var kotlinIdeJpsModuleNameToGradleModuleNameMapping: Map<String, String>
private lateinit var intellijModuleNameToGradleDependencyNotationsMapping: Map<String, List<GradleDependencyNotation>>
private lateinit var nameToJpsLibraryMapping: Map<String, JpsLibrary>

private val intellijModuleNameToGradleDependencyNotationsMappingManual: Map<String, List<GradleDependencyNotation>> = mapOf(
    "intellij.platform.debugger.testFramework" to listOf(), // TODO()
    "intellij.completionMlRanking" to listOf(),
    "intellij.platform.jps.model.tests" to listOf(),
    "intellij.platform.externalSystem.tests" to listOf(),
    "intellij.gradle.toolingExtension.tests" to listOf(),
    "intellij.gradle.tests" to listOf(),
)

val SKIP_IML = listOf(
    "kotlin-ide/kotlin-compiler-classpath/kotlin.util.compiler-classpath.iml"
)

fun main() {
    val kotlinIdeFile = File(".").canonicalFile.resolve(KOTLIN_IDE_DIR_NAME)
    val imlFiles = kotlinIdeFile.walk()
        .filter { it.isFile && it.extension == "iml" && it.name.startsWith("kotlin.") }
        .filter { file -> SKIP_IML.none { file.endsWith(it) } }
        .toList()

    kotlinIdeJpsModuleNameToGradleModuleNameMapping = imlFiles.associate {
        Pair(it.nameWithoutExtension, ":" + it.parentFile.relativeTo(File(".").canonicalFile).path.replace("/", ":"))
    }

    intellijModuleNameToGradleDependencyNotationsMapping = intellijModuleNameToGradleDependencyNotationsMappingManual + listOf(
        object {}.javaClass.getResource("/ideaIU-project-structure-mapping.json"),
        object {}.javaClass.getResource("/intellij-core-project-structure-mapping.json")
    )
        .flatMap { jsonUrl ->
            val jsonStr = File(jsonUrl!!.toURI()).readText()
            return@flatMap JsonParser.parseString(jsonStr).asJsonArray.mapNotNull {
                val jsonObject = it.asJsonObject
                if (jsonObject.get("type").asString != "module-output") {
                    return@mapNotNull null
                }
                Pair(
                    jsonObject.get("moduleName").asString,
                    GradleDependencyNotation.fromIntellijJsonObject(jsonObject) ?: return@mapNotNull null
                )
            }
        }
        .groupBy { it.first }
        .mapValues { entry -> entry.value.map { it.second } }

    nameToJpsLibraryMapping = kotlinIdeFile.resolve(".idea/libraries").listFiles()!!
        .map { jpsLibraryXmlFile ->
            val libraryTable = jpsLibraryXmlFile.readXml()
            JpsLibraryTableSerializer.loadLibrary(libraryTable.children.single())!!
        }
        .associateBy { it.name }

    for ((imlFile, jpsModule) in imlFiles.zip(JpsProjectLoader.loadModules(imlFiles.map { it.toPath() }, null, mapOf()))) {
        imlFile.parentFile.resolve("build.gradle.kts").writeText(convertJpsModule(imlFile, jpsModule))
    }
}

fun JpsLibraryDependency.resolve(moduleImlRootElement: Element): JpsLibrary? {
    val libraryName = libraryReference.libraryName
    return moduleImlRootElement.traverseChildren()
        .filter { it.name == JpsModuleRootModelSerializer.LIBRARY_TAG }
        .map { JpsLibraryTableSerializer.loadLibrary(it) }
        .find { it.name == libraryName }
        ?: nameToJpsLibraryMapping[libraryName]
}

fun convertJpsDependencyElement(dep: JpsDependencyElement, moduleImlRootElement: Element): List<String> {
    if (dep is JpsSdkDependency || dep is JpsModuleSourceDependency) {
        return listOf()
    }

    val moduleName = (dep as? JpsModuleDependency)?.moduleReference?.moduleName
    val ext = JpsJavaExtensionServiceImpl.getInstance().getDependencyExtension(dep)
        ?: error("Cannot get dependency extension for $dep")
    val scope = "JpsDepScope.${ext.scope}"
    val exported = "exported = true".takeIf { ext.isExported }

    fun jpsLikeJarDependency(dependencyNotation: String, dependencyConfiguration: String?): String {
        return "jpsLikeJarDependency(${listOfNotNull(dependencyNotation, scope, dependencyConfiguration, exported).joinToString()})"
    }

    fun jpsLikeModuleDependency(moduleName: String): String {
        return "jpsLikeModuleDependency(${listOfNotNull("\"$moduleName\"", scope, exported).joinToString()})"
    }

    if (moduleName?.startsWith("kotlin.") == true) {
        val gradleModuleName = kotlinIdeJpsModuleNameToGradleModuleNameMapping.getValue(moduleName)
        return listOf(jpsLikeModuleDependency(gradleModuleName))
    }
    if (moduleName?.startsWith("intellij.") == true) {
        return intellijModuleNameToGradleDependencyNotationsMapping[moduleName]
            ?.map {
                val args = listOfNotNull(it.dependencyNotation, scope, it.dependencyConfiguration, exported).joinToString()
                jpsLikeJarDependency(it.dependencyNotation, it.dependencyConfiguration)
            }
            ?: error("Cannot find mapping for intellij module name = $moduleName")
    }
    if (dep is JpsLibraryDependency) {
        val libraryName = dep.libraryReference.libraryName
        if (libraryName.startsWith("kotlinc.")) {
            return listOf()
        }
        val jpsLibrary = dep.resolve(moduleImlRootElement) ?: error("Cannot resolve jps library = $libraryName")
        val mavenId = jpsLibrary.properties.safeAs<JpsSimpleElement<*>>()?.data
            ?.safeAs<JpsMavenRepositoryLibraryDescriptor>()?.mavenId
            ?: error("Caanot find maven coordinates for $jpsLibrary")
        return listOf(jpsLikeJarDependency("\"$mavenId\"", dependencyConfiguration = null))
    }
    error("Unknown dependency: $dep")
}

fun convertJpsModuleSourceRoot(imlFile: File, sourceRoot: JpsModuleSourceRoot): String {
    return when (sourceRoot.rootType) {
        is JavaSourceRootType -> "java.srcDir(\"${sourceRoot.file.relativeTo(imlFile.parentFile)}\")"
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

    val moduleImlRootElement = imlFile.readXml()
    val deps = dependencies.flatMap { convertJpsDependencyElement(it, moduleImlRootElement) }
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
        |
        |testsJar()
    """.trimMarginWithInterpolations()
}
