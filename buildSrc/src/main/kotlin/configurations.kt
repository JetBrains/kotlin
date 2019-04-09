/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Action
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo
import org.gradle.kotlin.dsl.accessors.runtime.addExternalModuleDependencyTo
import org.gradle.kotlin.dsl.add

fun DependencyHandler.embedded(dependencyNotation: Any): Dependency? =
    add("embedded", dependencyNotation)

fun DependencyHandler.embedded(
    dependencyNotation: String,
    dependencyConfiguration: Action<ExternalModuleDependency>
): ExternalModuleDependency =
    addDependencyTo(this, "embedded", dependencyNotation, dependencyConfiguration)

fun DependencyHandler.embedded(
    group: String,
    name: String,
    version: String? = null,
    configuration: String? = null,
    classifier: String? = null,
    ext: String? = null,
    dependencyConfiguration: Action<ExternalModuleDependency>? = null
): ExternalModuleDependency = addExternalModuleDependencyTo(
    this, "embedded", group, name, version, configuration, classifier, ext, dependencyConfiguration
)

fun <T : ModuleDependency> DependencyHandler.embedded(
    dependency: T,
    dependencyConfiguration: T.() -> Unit
): T = add("embedded", dependency, dependencyConfiguration)
