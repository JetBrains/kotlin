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


fun Project.konanVersionGeneratedSrc() = rootProject.findProject(":kotlin-native")?.file("../buildSrc/build/version-generated/src/generated") ?: file("build/version-generated/src/generated")
fun Project.kotlinNativeVersionSrc():File {
    val kotlinNativeProject = rootProject.findProject(":kotlin-native")
    return if(kotlinNativeProject != null){
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
fun Project.kotlinNativeProperties() = Properties().apply{
    val kotlinNativeProperyFile = File(this@kotlinNativeProperties.konanRootDir(), "gradle.properties")
    if (!kotlinNativeProperyFile.exists())
        return@apply
    FileReader(kotlinNativeProperyFile).use {
        load(it)
    }
}

val Project.kotlinNativeVersionInResources:Boolean
    get() = kotlinNativeProperties()["kotlinNativeVersionInResources"]?.toString()?.toBoolean() ?: false

fun Project.kotlinNativeVersionResourceFile() = File("${project.findProperty("kotlin_root")!!}/buildSrc/build/version-generated/META-INF/kotlin-native.compiler.version")
fun Project.kotlinNativeVersionValue(): CompilerVersion? {
    return if (this.kotlinNativeVersionInResources)
        kotlinNativeVersionResourceFile().let { file ->
            file.readLines().single().parseCompilerVersion()
        }
    else null
}


open class VersionGenerator: DefaultTask() {
    private val kotlinNativeProperties = project.kotlinNativeProperties()

    @Input
    var kotlinNativeVersionInResources = project.kotlinNativeVersionInResources

    @OutputDirectory
    val versionSourceDirectory = project.file("build/version-generated")

    @OutputFile
    open var versionFile: File? = project.file("${versionSourceDirectory.path}/org/jetbrains/kotlin/konan/CompilerVersionGenerated.kt")

    @Input
    open val konanVersion =  kotlinNativeProperties["konanVersion"].toString()


    // TeamCity passes all configuration parameters into a build script as project properties.
    // Thus we can use them here instead of environment variables.
    @Optional
    @Input
    open val buildNumber = project.findProperty("build.number")?.toString()

    @Input
    open val meta = kotlinNativeProperties["konanMetaVersion"]?.let{ MetaVersion.valueOf(it.toString().toUpperCase()) } ?: MetaVersion.DEV

    private val versionPattern = Pattern.compile(
        "^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-M(\\p{Digit}))?(?:-(\\p{Alpha}\\p{Alnum}*))?(?:-(\\d+))?$"
    )

    fun defaultVersionFileLocation() {
        versionFile = if (kotlinNativeVersionInResources)
            project.file("${versionSourceDirectory.path}/META-INF/kotlin-native.compiler.version")
        else
            project.file("${versionSourceDirectory.path}/org/jetbrains/kotlin/konan/CompilerVersionGenerated.kt")
    }

    @TaskAction
    open fun generateVersion() {
        val matcher = versionPattern.matcher(konanVersion)
        require(matcher.matches()) { "Cannot parse Kotlin/Native version: \$konanVersion" }
        val major = matcher.group(1).toInt()
        val minor = matcher.group(2).toInt()
        val maintenanceStr = matcher.group(3)
        val maintenance = maintenanceStr?.toInt() ?: 0
        val milestoneStr = matcher.group(4)
        val milestone = milestoneStr?.toInt() ?: -1
        project.logger.info("BUILD_NUMBER: $buildNumber")
        var build = -1
        if (buildNumber != null) {
            val buildNumberSplit = buildNumber!!.split("-".toRegex()).toTypedArray()
            build = buildNumberSplit[buildNumberSplit.size - 1].toInt() // //7-dev-buildcount
        }

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
