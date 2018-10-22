/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildUtils.idea

import IntelliJInstrumentCodeTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.gradle.ext.TopLevelArtifact
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Temporary solution for configuring IDEA artifacts based on Gradle copy tasks configurations.
 * This should be replaced with DSL that produces both Gradle copy tasks and IDEA artifacts configuration.
 *
 * TODO: remove this package when DSL described above will be implemented
 */
fun generateIdeArtifacts(rootProject: Project, artifactsFactory: NamedDomainObjectContainer<TopLevelArtifact>) {
    val reportsDir = File(path(rootProject.buildDir.path, "reports", "idea-artifacts-cfg"))
    reportsDir.mkdirs()
    val projectDir = rootProject.projectDir

    File(reportsDir, "01-visitor.report.txt").printWriter().use { visitorReport ->
        val modelBuilder = object : DistModelBuilder(rootProject, visitorReport) {
            // todo: investigate why allCopyActions not working
            override fun transformJarName(name: String): String {
                val name1 = name.replace(Regex("-${java.util.regex.Pattern.quote(rootProject.version.toString())}"), "")

                val name2 = when (name1) {
                    "kotlin-runtime-common.jar" -> "kotlin-runtime.jar"
                    "kotlin-compiler-before-proguard.jar" -> "kotlin-compiler.jar"
                    "kotlin-main-kts-before-proguard.jar" -> "kotlin-main-kts.jar"
                    "kotlin-allopen-compiler-plugin.jar" -> "allopen-compiler-plugin.jar"
                    "kotlin-noarg-compiler-plugin.jar" -> "noarg-compiler-plugin.jar"
                    "kotlin-sam-with-receiver-compiler-plugin.jar" -> "sam-with-receiver-compiler-plugin.jar"
                    "kotlin-android-extensions-runtime.jar" -> "android-extensions-runtime.jar"
                    else -> name1
                }

                val name3 = name2.removePrefix("dist-")

                return name3
            }
        }

        fun visitAllTasks(project: Project) {
            project.tasks.forEach {
                try {
                    when {
                        it is AbstractCopyTask -> modelBuilder.visitCopyTask(it)
                        it is AbstractCompile -> modelBuilder.visitCompileTask(it)
                        it is IntelliJInstrumentCodeTask -> modelBuilder.visitInstrumentTask(it)
                        it.name == "stripMetadata" -> {
                            modelBuilder.rootCtx.log(
                                "STRIP METADATA",
                                "${it.inputs.files.singleFile} -> ${it.outputs.files.singleFile}"
                            )

                            DistCopy(
                                modelBuilder.requirePath(it.outputs.files.singleFile.path),
                                modelBuilder.requirePath(it.inputs.files.singleFile.path)
                            )
                        }
                    }
                } catch (t: Throwable) {
                    logger.error("Error while visiting `$it`", t)
                }
            }

            project.subprojects.forEach {
                visitAllTasks(it)
            }
        }

        visitAllTasks(rootProject)

        // proguard
        DistCopy(
            target = modelBuilder.requirePath(
                path(
                    projectDir.path,
                    "libraries",
                    "reflect",
                    "build",
                    "libs",
                    "kotlin-reflect-proguard.jar"
                )
            ),
            src = modelBuilder.requirePath(path(projectDir.path, "libraries", "reflect", "build", "libs", "kotlin-reflect-shadow.jar"))
        )

        File(reportsDir, "02-vfs.txt").printWriter().use {
            modelBuilder.vfsRoot.printTree(it)
        }
        modelBuilder.checkRefs()

        with(DistModelFlattener()) {
            with(DistModelIdeaArtifactBuilder(rootProject)) {
                File(reportsDir, "03-flattened-vfs.txt").printWriter().use { report ->
                    fun getFlattenned(vfsPath: String): DistVFile =
                        modelBuilder.vfsRoot.relativePath(path(projectDir.path, vfsPath))
                            .flatten()

                    val all = getFlattenned("dist")
                    all.child["artifacts"]
                        ?.removeAll { it != "ideaPlugin" }
                    all.child["artifacts"]
                        ?.child?.get("ideaPlugin")
                        ?.child?.get("Kotlin")
                        ?.removeAll { it != "kotlinc" && it != "lib" }
                    all.removeAll { it.endsWith(".zip") }
                    all.printTree(report)

                    val dist = artifactsFactory.create("dist_auto_reference_dont_use")
                    dist.addFiles(all)
                }
            }
        }
    }
}

private fun path(vararg components: String) = components.joinToString(File.separator)

internal val logger = LoggerFactory.getLogger("ide-artifacts")

