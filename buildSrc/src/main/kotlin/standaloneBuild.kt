/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.kotlin.dsl.*

// absolute path to "cidr-native" sub-project in standalone Kotlin Ultimate build
private const val CIDR_NATIVE_SUBPROJECT_PATH_IN_KOTLIN_ULTIMATE = ":cidr-native"

val Project.isStandaloneBuild
    get() = rootProject.findProject(CIDR_NATIVE_SUBPROJECT_PATH_IN_KOTLIN_ULTIMATE) != null

fun Project.ideaPluginJarDep(): Any {
    return if (isStandaloneBuild) {
        // reuse JAR artifact from downloaded plugin
        val ideaPluginForCidrDir: String by rootProject.extra
        fileTree(ideaPluginForCidrDir) {
            include("lib/kotlin-plugin.jar")
        }
    }
    else
        // depend on the artifact to be build
        dependencies.project(":prepare:idea-plugin", configuration = "runtimeJar")
}

fun Project.addIdeaNativeModuleDeps() {
    dependencies {

        if (isStandaloneBuild) {
            // contents of Kotlin plugin
            val ideaPluginForCidrDir: String by rootProject.extra
            val ideaPluginJars = fileTree(ideaPluginForCidrDir) {
                exclude(EXCLUDES_LIST_FROM_IDEA_PLUGIN)
            }
            add("compile", ideaPluginJars)

            // IntelliJ platform (out of CLion distribution)
            val clionDir: String by rootProject.extra
            val cidrPlatform = fileTree(clionDir) {
                include("lib/*.jar")
                exclude("lib/kotlin*.jar") // because Kotlin should be taken from Kotlin plugin
                exclude("lib/clion*.jar") // don't take scrambled CLion JAR
            }
            add("compile", cidrPlatform)

            // standard CIDR plugins
            val cidrPlugins = fileTree(clionDir) {
                include("plugins/cidr-*/lib/*.jar")
                include("plugins/gradle/lib/*.jar")
            }
            add("compile", cidrPlugins)

            // Java APIs (private artifact that goes together with CLion builds)
            val clionPlatformDepsDir: String by rootProject.extra
            val cidrPlatformDeps = fileTree(clionPlatformDepsDir) { include(PLATFORM_DEPS_JAR_NAME) }
            add("compile", cidrPlatformDeps)
        } else {
            // Gradle projects with Kotlin/Native-specific logic
            // (automatically brings all the necessary transient dependencies, include deps on IntelliJ platform)
            add("compile", project(":idea:idea-native"))
            add("compile", project(":idea:idea-gradle-native"))

            // Java APIs (from Big Kotlin project)
            val javaApis = add(
                    "compile",
                    "kotlin.build.custom.deps:intellij:${rootProject.extra["versions.intellijSdk"]}"
            ) as ExternalModuleDependency

            with(javaApis) {
                artifact {
                    name = "java-api"
                    type = "jar"
                    extension = "jar"
                }
                artifact {
                    name = "java-impl"
                    type = "jar"
                    extension = "jar"
                }
                isTransitive = false
            }
        }
    }
}
