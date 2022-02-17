/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import java.io.*
import java.util.regex.Pattern
import java.util.Properties
import org.gradle.api.Project
import org.jetbrains.kotlin.konan.*


fun Project.konanVersionGeneratedSrc() =
    rootProject.findProject(":kotlin-native")?.file("../buildSrc/build/version-generated/src/generated")
        ?: file("build/version-generated/src/generated")

fun Project.kotlinNativeVersionSrc(): File {
    val kotlinNativeProject = rootProject.findProject(":kotlin-native")
    return if (kotlinNativeProject != null) {
        if (kotlinNativeVersionInResources)
            kotlinNativeProject.file("${findProperty("kotlin_root")!!}/buildSrc/src/kotlin-native-binary-version/kotlin")
        else
            kotlinNativeProject.file("../buildSrc/build/version-generated/src/generated")
    } else {
        if (kotlinNativeVersionInResources)
            file("src/kotlin-native-binary-version/kotlin")
        else
            file("build/version-generated/src/generated")
    }
}

fun Project.konanRootDir() = rootProject.findProject(":kotlin-native")?.projectDir ?: file("../kotlin-native")

fun Project.kotlinNativeProperties() = Properties().apply {
    val kotlinNativeProperyFile = File(this@kotlinNativeProperties.konanRootDir(), "gradle.properties")
    if (!kotlinNativeProperyFile.exists())
        return@apply
    FileReader(kotlinNativeProperyFile).use {
        load(it)
    }
}

val Project.kotlinNativeVersionInResources: Boolean
    get() = kotlinNativeProperties()["kotlinNativeVersionInResources"]?.toString()?.toBoolean() ?: false

fun Project.kotlinNativeVersionResourceFile() = File(
    "${project.findProperty("kotlin_root")!!}" +
            "/buildSrc/build/version-generated/META-INF/kotlin-native.compiler.version"
)

fun Project.kotlinNativeVersionValue(): CompilerVersion? {
    return if (this.kotlinNativeVersionInResources)
        kotlinNativeVersionResourceFile().let { file ->
            file.readLines().single().parseCompilerVersion()
        }
    else null
}


open class VersionGenerator : DefaultTask() {
    private val kotlinNativeProperties = project.kotlinNativeProperties()

    @Input
    var kotlinNativeVersionInResources = project.kotlinNativeVersionInResources

    @OutputDirectory
    val versionSourceDirectory = project.file("build/version-generated")

    @OutputFile
    open var versionFile: File? = project.file("${versionSourceDirectory.path}/org/jetbrains/kotlin/konan/CompilerVersionGenerated.kt")

    @Input
    open val konanVersion = project.findProperty("konanVersion") as? String ?: kotlinNativeProperties["konanVersion"].toString()


    // TeamCity passes all configuration parameters into a build script as project properties.
    // Thus we can use them here instead of environment variables.
    @Optional
    @Input
    open val buildNumber = project.findProperty("build.number")?.toString()

    @get:Input
    open val meta = (project.findProperty("konanMetaVersion") as? String
        ?: kotlinNativeProperties["konanMetaVersion"])?.let { MetaVersion.findAppropriate(it.toString()) }
        ?: MetaVersion.DEV

    fun defaultVersionFileLocation() {
        versionFile = if (kotlinNativeVersionInResources)
            project.file("${versionSourceDirectory.path}/META-INF/kotlin-native.compiler.version")
        else
            project.file("${versionSourceDirectory.path}/org/jetbrains/kotlin/konan/CompilerVersionGenerated.kt")
    }

    @TaskAction
    open fun generateVersion() {
        val matchResult = CompilerVersion.versionPattern.matchEntire(konanVersion)
        requireNotNull(matchResult) { "Cannot parse Kotlin/Native version: $konanVersion" }
        val major = matchResult.groups.get(1)?.value?.toInt() ?: throw IllegalArgumentException("Unable to parse major in $konanVersion")
        val minor = matchResult.groups.get(2)?.value?.toInt() ?: throw IllegalArgumentException("Unable to parse minor in $konanVersion")
        val maintenance = matchResult.groups.get(3)?.value?.toInt() ?: 0
        val milestone = -1 // isn't used any more

        project.logger.info("BUILD_NUMBER: $buildNumber")
        val buildNumberSplit = buildNumber?.split("-".toRegex())?.toTypedArray()
        val build = buildNumberSplit?.get(buildNumberSplit.size - 1)?.toIntOrNull() ?: -1

        val versionObject = CompilerVersionImpl(meta, major, minor, maintenance, milestone, build)
        versionObject.serialize()
    }

    private fun CompilerVersion.serialize() {
        versionFile!!.parentFile.mkdirs()
        project.logger.info("version file: ${versionFile}")
        if (!kotlinNativeVersionInResources) {
            PrintWriter(versionFile).use { printWriter ->
                printWriter.println(
                    """|package org.jetbrains.kotlin.konan
                       |internal val currentCompilerVersion: CompilerVersion =
                       |CompilerVersionImpl($meta, $major, $minor,
                       |                    $maintenance, $milestone, $build)
                       |val CompilerVersion.Companion.CURRENT: CompilerVersion
                       |get() = currentCompilerVersion""".trimMargin()
                )
            }
        } else {
            PrintStream(versionFile).use {
                it.println(this)
            }
        }
    }
}
