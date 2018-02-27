import com.gradle.publish.PluginBundleExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the

/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

fun Project.publishToGradlePluginPortal() {
    apply { plugin("com.gradle.plugin-publish") }

    val prebuiltJarPath: String? =
        // Support the old way of specifying pre-built JARs per-project:
        project.findProperty("${project.name}-jar")?.toString() ?:
        // And the new way: using the 'prebuiltMavenRepo' property as a path to a  Maven repo root:
        project.findProperty("prebuiltMavenRepo")?.let { prebuiltMavenRepo ->
            val subfolder = project.group.toString().replace(".", "/") + "/" + project.name + "/" + version
            val jarName = "${project.name}-$version.jar"
            "$prebuiltMavenRepo/$subfolder/$jarName"
        }

    if (prebuiltJarPath != null) {
        println("Dropping archives and using pre-built artifact for ${project.name}: $prebuiltJarPath")

        // Removing and re-creating the configuration is required due to a bug in the Gradle `signing` plugin,
        // it adds a callback for `archives` items deletion which breaks when its own `Signature` artifacts are deleted
        configurations.remove(configurations.getByName("archives"))
        configurations.create("archives")

        artifacts.add("archives", file(prebuiltJarPath)) {
            name = project.name
       }
    }

    findProperty("publishPluginsVersion")?.let { publishPluginsVersion ->
        configurations.getByName("archives").artifacts.all {
            version = publishPluginsVersion
        }
    }

    tasks.getByName("publishPlugins").doFirst {
        val kotlinVersion = property("kotlinVersion").toString()
        require(!kotlinVersion.endsWith("SNAPSHOT")) {
            "Kotlin version is a snapshot version $kotlinVersion. Snapshot versions should not be published."
        }
    }

    the<PluginBundleExtension>().apply {
        website = "https://kotlinlang.org/"
        vcsUrl = "https://github.com/JetBrains/kotlin/"
        description = "Kotlin plugins for Gradle"
        tags = listOf("kotlin")

        mavenCoordinates {
            groupId = "org.jetbrains.kotlin"
        }
    }
}
