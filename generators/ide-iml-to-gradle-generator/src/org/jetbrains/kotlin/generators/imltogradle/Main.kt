/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.imltogradle

import com.google.gson.JsonParser
import org.jetbrains.jps.model.JpsSimpleElement
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.library.JpsLibrary
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.module.*
import org.jetbrains.kotlin.generators.imltogradle.GradleDependencyNotation.IntellijDepGradleDependencyNotation
import java.io.File


private lateinit var intellijModuleNameToGradleDependencyNotationsMapping: Map<String, List<GradleDependencyNotation>>
private val KOTLIN_REPO_ROOT = File(".").canonicalFile
val DEFAULT_KOTLIN_SNAPSHOT_VERSION = KOTLIN_REPO_ROOT.resolve("gradle.properties")
    .readProperty("defaultSnapshotVersion")
    .removeSuffix("-SNAPSHOT")
private val INTELLIJ_REPO_ROOT = KOTLIN_REPO_ROOT.resolve("intellij").resolve("community").takeIf { it.exists() }
    ?: KOTLIN_REPO_ROOT.resolve("intellij")

private val intellijModuleNameToGradleDependencyNotationsMappingManual: List<Pair<String, GradleDependencyNotation>> = listOf(
    "intellij.platform.jps.build" to GradleDependencyNotation("jpsBuildTest()"),
    "intellij.platform.structuralSearch" to IntellijDepGradleDependencyNotation("structuralsearch") // for some reason it's absent in json mapping
)

// These modules are used in Kotlin plugin and IDEA doesn't publish artifact of these modules
private val intellijModulesForWhichGenerateBuildGradle = listOf(
    "intellij.platform.debugger.testFramework",
    "intellij.gradle.tests",
    "intellij.platform.externalSystem.tests",
    "intellij.platform.lang.tests",
    "intellij.platform.testExtensions",
    "intellij.java.compiler.tests",
    "intellij.gradle.toolingExtension.tests",
    "intellij.maven",
)

val jsonUrlPrefixes = mapOf(
    "202" to "https://buildserver.labs.intellij.net/guestAuth/repository/download/ijplatform_IjPlatform202_IntellijArtifactMappings/113235432:id",
    "203" to "https://buildserver.labs.intellij.net/guestAuth/repository/download/ijplatform_IjPlatform203_IntellijArtifactMappings/117989041:id",
    "211" to "https://buildserver.labs.intellij.net/guestAuth/repository/download/ijplatform_IjPlatform211_IntellijArtifactMappings/121258191:id",
    "212" to "https://buildserver.labs.intellij.net/guestAuth/repository/download/ijplatform_IjPlatform211_IntellijArtifactMappings/131509697:id",
)

fun main() {
    val ijCommunityModuleNameToJpsModuleMapping = INTELLIJ_REPO_ROOT.loadJpsProject().modules.associateBy { it.name }

    val skipDirNames = setOf("src", "out", "org", "com", "testData")
    val imlFiles = INTELLIJ_REPO_ROOT
        .walk()
        .onEnter { dir -> dir.name !in skipDirNames }
        .filter {
            it.isFile && it.extension == "iml" &&
                    (it.name.startsWith("kotlin.") ||
                            it.nameWithoutExtension in intellijModulesForWhichGenerateBuildGradle)
        }
        .toList()

    val ideaMajorVersion = KOTLIN_REPO_ROOT.resolve("local.properties").readProperty("attachedIntellijVersion")

    intellijModuleNameToGradleDependencyNotationsMapping = fetchJsonsFromBuildserver(ideaMajorVersion)
        .flatMap { jsonStr ->
            JsonParser.parseString(jsonStr).asJsonArray.mapNotNull { jsonElement ->
                val jsonObject = jsonElement.asJsonObject
                val moduleName = jsonObject.get("moduleName")?.asString ?: return@mapNotNull null
                val jarPath = jsonObject.get("path")?.asString ?: return@mapNotNull null
                moduleName to jarPath
            }
        }
        .filter { (_, jarPath) -> !jarPath.contains("DatabaseTools") && !jarPath.contains("lib/openapi.jar") }
        .groupBy(
            keySelector = { (_, jarPath) -> jarPath },
            valueTransform = { (moduleName, _) -> moduleName }
        )
        .filter { (_, moduleNames) ->
            moduleNames.all { it in ijCommunityModuleNameToJpsModuleMapping }  // filter out ultimate jars
        }
        .flatMap { (jarPath, moduleNames) -> moduleNames.map { it to jarPath } }
        .mapNotNull { (moduleName, jarPath) ->
            moduleName to (GradleDependencyNotation.fromJarPath(jarPath) ?: return@mapNotNull null)
        }
        .plus(intellijModuleNameToGradleDependencyNotationsMappingManual)
        .groupBy(
            keySelector = { (moduleName, _) -> moduleName },
            valueTransform = { (_, dependencyNotation) -> dependencyNotation }
        )

    val imlsInSameDirectory: List<List<File>> = imlFiles.groupBy { it.parentFile }.filter { it.value.size > 1 }.map { it.value }
    if (imlsInSameDirectory.isNotEmpty()) {
        val report = imlsInSameDirectory.joinToString("\n") { "In same directory: " + it.joinToString() }
        error("It's not allowed to have imls in same directory:\n$report")
    }

    imlFiles
        .mapNotNull { imlFile ->
            ijCommunityModuleNameToJpsModuleMapping[imlFile.nameWithoutExtension]?.let { imlFile to it }
        }
        .filter { (_, jpsModule) -> jpsModule.name != "kotlin.util.compiler-dependencies" }
        .forEach { (imlFile, jpsModule) ->
            println("Processing iml ${imlFile}")
            imlFile.parentFile.resolve("build.gradle.kts").writeText(convertJpsModule(imlFile, jpsModule))
        }
}

fun convertJpsLibrary(lib: JpsLibrary, scope: JpsJavaDependencyScope, exported: Boolean): List<JpsLikeDependency> {
    val mavenRepositoryLibraryDescriptor = lib.properties
        .safeAs<JpsSimpleElement<*>>()?.data?.safeAs<JpsMavenRepositoryLibraryDescriptor>()

    val kotlincArtifactId = lib.getRootUrls(JpsOrderRootType.COMPILED).asSequence()
        .mapNotNull { url ->
            url.removeSuffix(".jar!/")
                .takeIf { it.endsWith(DEFAULT_KOTLIN_SNAPSHOT_VERSION) }
                ?.removeSuffix("-$DEFAULT_KOTLIN_SNAPSHOT_VERSION")
                ?.substringAfterLast("/")
        }
        .firstOrNull()

    return when {
        lib.name == "kotlin-stdlib-jdk8" || lib.name == "kotlinc.kotlin-stdlib" -> {
            listOf(
                JpsLikeJarDependency("kotlinStdlib()", scope, dependencyConfiguration = null, exported = exported),
                // TODO remove hack (for some reason we have to specify :kotlin-stdlib-jdk7 explicitly, otherwise compilation doesn't pass)
                JpsLikeJarDependency("project(\":kotlin-stdlib-jdk7\")", scope, dependencyConfiguration = null, exported = exported)
            )
        }
        kotlincArtifactId != null -> {
            val dependencyNotation =
                if (KOTLIN_REPO_ROOT.resolve("prepare/ide-plugin-dependencies/$kotlincArtifactId").exists())
                    "project(\":prepare:ide-plugin-dependencies:$kotlincArtifactId\")"
                else
                    "project(\":$kotlincArtifactId\")"
            listOf(JpsLikeJarDependency(dependencyNotation, scope, dependencyConfiguration = null, exported = exported))
        }
        mavenRepositoryLibraryDescriptor == null -> {
            lib.getRootUrls(JpsOrderRootType.COMPILED)
                .map {
                    val relativeToCommunity = it.removePrefix("jar://").removeSuffix("!/")
                        .removePrefix(KOTLIN_REPO_ROOT.canonicalPath.replace("\\", "/"))
                        .also {
                            check(it.startsWith("/intellij/")) { "Only jars from Community repo are accepted $it" }
                        }
                        .removePrefix("/intellij/").removePrefix("community/")
                    JpsLikeJarDependency(
                        "files(intellijCommunityDir.resolve(\"$relativeToCommunity\").canonicalPath)",
                        scope,
                        dependencyConfiguration = null,
                        exported = exported
                    )
                }
        }
        else -> {
            val dependencyNotation = "\"${mavenRepositoryLibraryDescriptor.mavenId}\""
            val dependencyConfiguration =
                "{ isTransitive = false }".takeIf { !mavenRepositoryLibraryDescriptor.isIncludeTransitiveDependencies }
            listOf(JpsLikeJarDependency(dependencyNotation, scope, dependencyConfiguration, exported = exported))
        }
    }
}

fun convertIntellijDependencyNotFollowingTransitive(dep: JpsDependencyDescriptor, exported: Boolean): List<JpsLikeDependency> {
    return when (val moduleOrLibrary = dep.moduleOrLibrary) {
        is Either.First -> {
            val moduleName = moduleOrLibrary.value.name
            if (moduleName in intellijModulesForWhichGenerateBuildGradle) {
                listOf(JpsLikeModuleDependency(":kotlin-ide.$moduleName", dep.scope, exported))
            } else {
                intellijModuleNameToGradleDependencyNotationsMapping[moduleName]
                    .also { if (it == null) println("WARNING: Cannot find GradleDependencyNotation for $moduleName") }
                    ?.map { JpsLikeJarDependency(it.dependencyNotation, dep.scope, it.dependencyConfiguration, exported) }
                    ?: emptyList()
            }
        }
        is Either.Second -> convertJpsLibrary(moduleOrLibrary.value, dep.scope, exported)
    }
}

fun convertJpsModuleDependency(dep: JpsModuleDependency): List<JpsLikeDependency> {
    val moduleName = dep.moduleReference.moduleName
    return when {
        moduleName.startsWith("kotlin.") -> {
            listOf(JpsLikeModuleDependency(":kotlin-ide.$moduleName", dep.scope, dep.isExported))
        }
        moduleName.startsWith("intellij.") -> {
            dep.module.orElse { error("Cannot resolve dependency ${dep.moduleReference.moduleName}") }
                .flattenExportedTransitiveDependencies()
                .map { it.copy(scope = it.scope intersectCompileClasspath dep.scope) }
                .filter { it.scope != JpsJavaDependencyScope.RUNTIME } // We are interested only in transitive dependencies which affect compilation
                .flatMap { convertIntellijDependencyNotFollowingTransitive(it, dep.isExported).asSequence() }
                .map { JpsLikeDependencyWithComment(it, "'$moduleName' dependency") }
                .toList()
        }
        else -> error("Cannot convert module dependency to Gradle $dep")
    }
}

fun convertJpsDependencyElement(dep: JpsDependencyElement): List<JpsLikeDependency> {
    return when (dep) {
        is JpsModuleDependency -> convertJpsModuleDependency(dep)
        is JpsLibraryDependency -> dep.library
            .orElse { error("Cannot resolve library reference = ${dep.libraryReference}") }
            .let { convertJpsLibrary(it, dep.scope, dep.isExported) }
        else -> error("Unknown dependency: $dep")
    }
}

fun convertJpsModuleSourceRoot(imlFile: File, sourceRoot: JpsModuleSourceRoot): String {
    return when (sourceRoot.rootType) {
        is JavaSourceRootType -> "java.srcDir(\"${sourceRoot.file.relativeTo(imlFile.parentFile).invariantSeparatorsPath}\")"
        is JavaResourceRootType -> "resources.srcDir(\"${sourceRoot.file.relativeTo(imlFile.parentFile).invariantSeparatorsPath}\")"
        else -> error("Unknown sourceRoot = $sourceRoot")
    }
}

fun convertJpsModule(imlFile: File, jpsModule: JpsModule): String {
    val (src, test) = jpsModule.sourceRoots
        .groupBy { it.rootType.isForTests }
        .mapValues { entry -> entry.value.joinToString("\n") { convertJpsModuleSourceRoot(imlFile, it) } }
        .let { Pair(it[false] ?: "", it[true] ?: "") }

    val mavenRepos = INTELLIJ_REPO_ROOT.resolve(".idea/jarRepositories.xml").readXml().traverseChildren()
        .filter { it.getAttributeValue("name") == "url" }
        .map { it.getAttributeValue("value")!! }
        .map { "maven { setUrl(\"$it\") }" }
        .joinToString("\n")

    fun File.compilerArgsFromIml() = readXml().traverseChildren().singleOrNull { it.name == "compilerSettings" }?.children?.single()
        ?.getAttributeValue("value")

    fun File.compilerArgsFromProjectSettings() = readXml().traverseChildren()
        .singleOrNull { it.getAttributeValue("name") == "additionalArguments" }?.getAttributeValue("value")

    val compilerArgs = imlFile.compilerArgsFromIml()
        .orElse { INTELLIJ_REPO_ROOT.resolve(".idea/kotlinc.xml").compilerArgsFromProjectSettings() }
        ?.split(" ")
        ?.joinToString { "\"$it\"" }
        ?: ""

    val testsJar =
        if (jpsModule.sourceRoots.any { it.rootType.isForTests }) "testsJar()"
        else """
            // Fake empty configuration in order to make `DependencyHandler.projectTests(name: String)` work
            configurations.getOrCreate("tests-jar")
        """.trimIndent()

    val deps = jpsModule.dependencies.flatMap { convertJpsDependencyElement(it) }
        .distinctBy { it.normalizedForComparison() }
        .joinToString("\n") { it.convertToGradleCall() }
    return """
        |// GENERATED build.gradle.kts
        |// GENERATED BY ${imlFile.name}
        |// USE `./gradlew generateIdePluginGradleFiles` TO REGENERATE THIS FILE
        |
        |plugins {
        |    kotlin("jvm")
        |    `java-library` // Add `compileOnlyApi` configuration
        |    id("jps-compatible")
        |}
        |
        |repositories {
        |    $mavenRepos
        |}
        |
        |disableDependencyVerification()
        |
        |dependencies {
        |    implementation(toolsJarApi())
        |    $deps
        |}
        |
        |configurations.all {
        |    exclude(module = "tests-common") // Avoid classes with same FQN clashing
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
        |java {
        |    toolchain {
        |        languageVersion.set(JavaLanguageVersion.of(11))
        |    }
        |}
        |
        |tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile> {
        |    kotlinOptions.freeCompilerArgs = listOf($compilerArgs)
        |    kotlinOptions.useOldBackend = true // KT-45697
        |}
        |
        |$testsJar
    """.trimMarginWithInterpolations()
}
