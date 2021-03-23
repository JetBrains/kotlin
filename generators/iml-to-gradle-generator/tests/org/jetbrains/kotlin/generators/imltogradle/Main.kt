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
import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.library.JpsLibrary
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.module.*
import org.jetbrains.jps.model.serialization.JpsProjectLoader
import org.jetbrains.jps.model.serialization.library.JpsLibraryTableSerializer
import java.io.File

const val KOTLIN_IDE_DIR_NAME = "kotlin-ide"

private lateinit var kotlinIdeJpsModuleNameToGradleModuleNameMapping: Map<String, String>
private lateinit var intellijModuleNameToGradleDependencyNotationsMapping: Map<String, List<GradleDependencyNotation>>
private lateinit var intellijModuleNameToJpsModuleMapping: Map<String, JpsModule>
private val KOTLIN_REPO_ROOT = File(".").canonicalFile

private val intellijModuleNameToGradleDependencyNotationsMappingManual: Map<String, List<GradleDependencyNotation>> = mapOf(
    "intellij.platform.debugger.testFramework" to listOf(), // TODO()
    "intellij.completionMlRanking" to listOf(),
    "intellij.platform.jps.model.tests" to listOf(),
    "intellij.platform.externalSystem.tests" to listOf(),
    "intellij.gradle.toolingExtension.tests" to listOf(),
    "intellij.gradle.tests" to listOf(),

    // Transitive exported dependencies
    "intellij.platform.lang.tests" to listOf(),
)

private val knownBrokenLibraryJars = listOf("lucene-core", "coverage-report") // TODO resolve jar in ivy repo?

val SKIP_IML = listOf(
    "kotlin-ide/kotlin-compiler-classpath/kotlin.util.compiler-classpath.iml"
)

fun main(args: Array<String>) {
    val intellijCommunityRoot = args.singleOrNull()
        ?.let { File(it) }
        ?: error("Usage: ./gradlew generateGradleByIml -Pargs=\"path/to/intellij-community\"")

    intellijModuleNameToJpsModuleMapping = intellijCommunityRoot.loadJpsProject().modules.associateBy { it.name }

    val kotlinIdeFile = KOTLIN_REPO_ROOT.resolve(KOTLIN_IDE_DIR_NAME)
    val imlFiles = kotlinIdeFile.walk()
        .filter { it.isFile && it.extension == "iml" && it.name.startsWith("kotlin.") }
        .filter { file -> SKIP_IML.none { file.endsWith(it) } }
        .toList()

    kotlinIdeJpsModuleNameToGradleModuleNameMapping = imlFiles.associate {
        Pair(it.nameWithoutExtension, ":" + it.parentFile.relativeTo(KOTLIN_REPO_ROOT).path.replace("/", ":"))
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

    projectLevelLibraryNameToJpsLibraryMapping = kotlinIdeFile.resolve(".idea/libraries").listFiles()!!
        .map { jpsLibraryXmlFile ->
            val libraryTable = jpsLibraryXmlFile.readXml()
            JpsLibraryTableSerializer.loadLibrary(libraryTable.children.single())!!
        }
        .associateBy { it.name }

    for ((imlFile, jpsModule) in imlFiles.zip(JpsProjectLoader.loadModules(imlFiles.map { it.toPath() }, null, mapOf()))) {
        imlFile.parentFile.resolve("build.gradle.kts").writeText(convertJpsModule(imlFile, jpsModule))
    }
}

fun jpsLikeJarDependency(
    dependencyNotation: String,
    scope: JpsJavaDependencyScope,
    dependencyConfiguration: String?,
    exported: Boolean
): String {
    val scopeArg = "JpsDepScope.$scope"
    val exportedArg = "exported = true".takeIf { exported }
    return "jpsLikeJarDependency(${listOfNotNull(dependencyNotation, scopeArg, dependencyConfiguration, exportedArg).joinToString()})"
}

fun jpsLikeModuleDependency(
    moduleName: String,
    scope: JpsJavaDependencyScope,
    exported: Boolean
): String {
    val scopeArg = "JpsDepScope.$scope"
    val exportedArg = "exported = true".takeIf { exported }
    return "jpsLikeModuleDependency(${listOfNotNull("\"$moduleName\"", scopeArg, exportedArg).joinToString()})"
}

fun convertJpsLibrary(lib: JpsLibrary, scope: JpsJavaDependencyScope, exported: Boolean): String? {
    val mavenRepositoryLibraryDescriptor = lib.properties
        .safeAs<JpsSimpleElement<*>>()?.data
        ?.safeAs<JpsMavenRepositoryLibraryDescriptor>()

    if (mavenRepositoryLibraryDescriptor == null) {
        val ignore = knownBrokenLibraryJars.any { substring ->
            lib.getRootUrls(JpsOrderRootType.COMPILED).any { it.contains(substring) }
        }
        if (ignore) {
            return null
        }
    }
    mavenRepositoryLibraryDescriptor ?: error("Cannot find maven coordinates for library ${lib.name}")

    return when {
        lib.name == "kotlinc.kotlin-stdlib-jdk8" -> {
            jpsLikeJarDependency("kotlinStdlib()", scope, dependencyConfiguration = null, exported)
        }
        lib.name.startsWith("kotlinc.") -> {
            val artifactId = mavenRepositoryLibraryDescriptor.artifactId
            if (KOTLIN_REPO_ROOT.resolve("prepare/ide-plugin-dependencies/$artifactId").exists()) {
                jpsLikeJarDependency(
                    "project(\":prepare:ide-plugin-dependencies:$artifactId\")",
                    scope,
                    dependencyConfiguration = null,
                    exported
                )
            } else {
                jpsLikeJarDependency(
                    "project(\":${mavenRepositoryLibraryDescriptor.artifactId}\")",
                    scope,
                    dependencyConfiguration = null,
                    exported
                )
            }
        }
        else -> {
            jpsLikeJarDependency("\"${mavenRepositoryLibraryDescriptor.mavenId}\"", scope, dependencyConfiguration = null, exported)
        }
    }
}

fun convertJpsModuleDependency(dep: JpsModuleDependency): List<String> {
    val moduleName = dep.moduleReference.moduleName
    when {
        moduleName.startsWith("kotlin.") -> {
            val gradleModuleName = kotlinIdeJpsModuleNameToGradleModuleNameMapping.getValue(moduleName)
            return listOf(jpsLikeModuleDependency(gradleModuleName, dep.scope, dep.isExported))
        }
        moduleName.startsWith("intellij.") -> {
            val ijModule = intellijModuleNameToJpsModuleMapping[dep.moduleReference.moduleName]
                ?: error("Cannot fine intellij module = ${dep.moduleReference}")
            return ijModule.flattenExportedTransitiveDependencies(initialScope = dep.scope, jpsDependencyToDependantModuleIml = { null })
                .filter { it.scope != JpsJavaDependencyScope.RUNTIME } // We are interested in compile classpath transitive dependencies
                .flatMap { dependencyDescriptor ->
                    when (val moduleOrLibrary = dependencyDescriptor.moduleOrLibrary) {
                        is Either.First -> {
                            val dependantModuleName = moduleOrLibrary.value.name
                            intellijModuleNameToGradleDependencyNotationsMapping[dependantModuleName]
                                .orElse { error("Cannot find Gradle notation mapping for module = $dependantModuleName") }
                                .map {
                                    jpsLikeJarDependency(
                                        it.dependencyNotation,
                                        dependencyDescriptor.scope,
                                        it.dependencyConfiguration,
                                        dep.isExported
                                    )
                                }
                                .asSequence()
                        }
                        is Either.Second -> sequenceOf(convertJpsLibrary(moduleOrLibrary.value, dependencyDescriptor.scope, dep.isExported))
                    }
                }
                .filterNotNull()
                .map { "$it // Exported transitive dependency" }
                .toList()
        }
        else -> error("Cannot convert module dependency to Gradle $dep")
    }
}

fun convertJpsDependencyElement(dep: JpsDependencyElement, moduleImlRootElement: Element): List<String> {
    return when (dep) {
        is JpsModuleDependency -> convertJpsModuleDependency(dep)
        is JpsLibraryDependency -> dep.resolve(moduleImlRootElement)
            .orElse { error("Cannot resolve library reference = ${dep.libraryReference}") }
            .let { listOfNotNull(convertJpsLibrary(it, dep.scope, dep.isExported)) }
        else -> error("Unknown dependency: $dep")
    }
}

fun convertJpsModuleSourceRoot(imlFile: File, sourceRoot: JpsModuleSourceRoot): String {
    return when (sourceRoot.rootType) {
        is JavaSourceRootType -> "java.srcDir(\"${sourceRoot.file.relativeTo(imlFile.parentFile)}\")"
        is JavaResourceRootType -> "resources.srcDir(\"${sourceRoot.file.relativeTo(imlFile.parentFile)}\")"
        else -> error("Unknown sourceRoot = $sourceRoot")
    }
}

fun convertJpsModule(imlFile: File, jpsModule: JpsModule): String {
    val (src, test) = jpsModule.sourceRoots
        .groupBy { it.rootType.isForTests }
        .mapValues { entry -> entry.value.joinToString("\n") { convertJpsModuleSourceRoot(imlFile, it) } }
        .let { Pair(it[false] ?: "", it[true] ?: "") }

    val mavenRepos = KOTLIN_REPO_ROOT.resolve(KOTLIN_IDE_DIR_NAME).resolve(".idea/jarRepositories.xml").readXml().traverseChildren()
        .filter { it.getAttributeValue("name") == "url" }
        .map { it.getAttributeValue("value")!! }
        .map { "maven { setUrl(\"$it\") }" }
        .joinToString("\n")

    val moduleImlRootElement = imlFile.readXml()
    val deps = jpsModule.dependencies.flatMap { convertJpsDependencyElement(it, moduleImlRootElement) }
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
        |repositories {
        |    $mavenRepos
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
