/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.*

data class PDependencies(val main: List<PDependency>, val deferred: List<PDependency>) {
    fun join(): List<PDependency> {
        return main + deferred
    }
}

fun Project.resolveDependencies(
    configuration: Configuration,
    forTests: Boolean,
    dependencyMappers: List<DependencyMapper>,
    withEmbedded: Boolean = false
): PDependencies {
    val dependencies = mutableListOf<PDependency>()
    val deferred = mutableListOf<PDependency>()

    nextArtifact@ for (artifact in configuration.resolvedConfiguration.resolvedArtifacts) {
        val identifier = artifact.id.componentIdentifier

        for (mapper in dependencyMappers) {
            if (mapper.predicate(artifact)) {
                val mapped = mapper.mapping(artifact)

                if (mapped != null) {
                    mapped.main?.let { dependencies += it }
                    deferred += mapped.deferred
                }

                continue@nextArtifact
            }
        }

        fun addProjectDependency(projectPath: String) {
            val project = rootProject.findProject(projectPath) ?: error("Cannot find project $projectPath")

            fun addSourceSet(name: String, suffix: String): Boolean {
                val sourceSet = project.sourceSets?.findByName(name)?.takeIf { !it.allSource.isEmpty() } ?: return false
                dependencies += PDependency.Module(project.pillModuleName + '.' + suffix)
                return true
            }

            if (forTests) {
                addSourceSet(SourceSet.TEST_SOURCE_SET_NAME, "test") || addSourceSet(SourceSet.MAIN_SOURCE_SET_NAME, "src")
            } else {
                addSourceSet(SourceSet.MAIN_SOURCE_SET_NAME, "src")
            }

            if (withEmbedded) {
                val embeddedConfiguration = project.configurations.findByName(EmbeddedComponents.CONFIGURATION_NAME)
                if (embeddedConfiguration != null) {
                    dependencies += resolveDependencies(embeddedConfiguration, forTests, dependencyMappers, withEmbedded).join()
                }
            }
        }

        when (identifier) {
            is ProjectComponentIdentifier -> addProjectDependency(identifier.projectPath)
            is LibraryBinaryIdentifier -> addProjectDependency(identifier.projectPath)
            is ModuleComponentIdentifier -> {
                val file = artifact.file
                val library = PLibrary(file.name, classes = listOf(file))
                dependencies += PDependency.ModuleLibrary(library)
            }
        }
    }

    for (dependency in configuration.dependencies) {
        if (dependency !is SelfResolvingDependency) {
            continue
        }

        val files = dependency.resolve().takeIf { it.isNotEmpty() } ?: continue
        val library = PLibrary(dependency.name, classes = files.toList())
        deferred += PDependency.ModuleLibrary(library)
    }

    return PDependencies(dependencies, deferred)
}