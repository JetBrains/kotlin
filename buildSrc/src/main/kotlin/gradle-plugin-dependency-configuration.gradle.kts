/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import plugins.KotlinBuildPublishingPlugin.Companion.DEFAULT_MAIN_PUBLICATION_NAME
import plugins.signLibraryPublication

plugins {
    `java-library`
    kotlin("jvm")
    `maven-publish`
}

configureCommonPublicationSettingsForGradle(signLibraryPublication)
configureKotlinCompileTasksGradleCompatibility()
addBomCheckTask()
extensions.extraProperties["kotlin.stdlib.default.dependency"] = "false"

val commonSourceSet = createGradleCommonSourceSet()
reconfigureMainSourcesSetForGradlePlugin(commonSourceSet)

// Used for Gradle 7.0 version
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_70,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 7.1+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_71,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 7.4+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_74,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 7.5+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_75,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 7.6+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_76,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 8.0+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_80,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 8.1+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_81,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

publishing {
    publications {
        register<MavenPublication>(DEFAULT_MAIN_PUBLICATION_NAME) {
            from(components["java"])
            suppressAllPomMetadataWarnings() // Don't warn about additional published variants
        }
    }
}
