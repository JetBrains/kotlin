/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("LibrariesCommon")

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@JvmOverloads
fun Project.configureJava9Compilation(
    moduleName: String,
    moduleOutputs: Collection<FileCollection> = setOf(sourceSets["main"].output)
) {
    configurations["java9CompileClasspath"].extendsFrom(configurations["compileClasspath"])

    tasks.named("compileJava9Java", JavaCompile::class.java) {
        dependsOn(moduleOutputs)

        targetCompatibility = JavaVersion.VERSION_1_9.toString()
        sourceCompatibility = JavaVersion.VERSION_1_9.toString()
        configureTaskToolchain(JdkMajorVersion.JDK_9)

        // module-info.java should be in java9 source set by convention
        val java9SourceSet = sourceSets["java9"].java
        destinationDir = file("${java9SourceSet.outputDir}/META-INF/versions/9")
        options.sourcepath = files(java9SourceSet.srcDirs)
        val compileClasspath = configurations["java9CompileClasspath"]
        val moduleFiles = objects.fileCollection().from(moduleOutputs)
        val modulePath = compileClasspath.filter { it !in moduleFiles.files }
        classpath = objects.fileCollection().from()
        options.compilerArgumentProviders.add(
            Java9AdditionalArgumentsProvider(
                moduleName,
                moduleFiles,
                modulePath
            )
        )
    }
}

private class Java9AdditionalArgumentsProvider(
    private val moduleName: String,
    private val moduleFiles: FileCollection,
    private val modulePath: FileCollection
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf(
        "--module-path", modulePath.asPath,
        "--patch-module", "$moduleName=${moduleFiles.asPath}",
        "-Xlint:-requires-transitive-automatic" // suppress automatic module transitive dependencies in kotlin.test
    )
}

fun Project.disableDeprecatedJvmTargetWarning() {
    if (!kotlinBuildProperties.useFir && !kotlinBuildProperties.disableWerror) {
        val tasksWithWarnings: List<String> by rootProject.extra
        tasks.withType<KotlinCompile>().configureEach {
            if (!tasksWithWarnings.contains(path)) {
                kotlinOptions {
                    allWarningsAsErrors = true
                    freeCompilerArgs += "-Xsuppress-deprecated-jvm-target-warning"
                }
            }
        }
    }
}