/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import com.gradle.publish.PluginBundleExtension
import plugins.signLibraryPublication

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("org.jetbrains.dokka")
    `maven-publish`
    id("com.gradle.plugin-publish")
}

// Enable signing for publications into Gradle Plugin Portal
val signPublication = !version.toString().contains("-SNAPSHOT") &&
        (project.gradle.startParameter.taskNames.contains("publishPlugins") || signLibraryPublication)

configureCommonPublicationSettingsForGradle(signPublication)
configureKotlinCompileTasksGradleCompatibility()
extensions.extraProperties["kotlin.stdlib.default.dependency"] = "false"

// common plugin bundle configuration
configure<PluginBundleExtension> {
    website = "https://kotlinlang.org/"
    vcsUrl = "https://github.com/jetbrains/kotlin"
    tags = listOf("kotlin")
}

publishing {
    publications {
        withType<MavenPublication>().configureEach {
            if (name.endsWith("PluginMarkerMaven")) {
                pom {
                    // https://github.com/gradle/gradle/issues/8754
                    // and https://github.com/gradle/gradle/issues/6155
                    packaging = "pom"
                }
            }
        }
    }
}

tasks {
    named("install") {
        dependsOn(named("validatePlugins"))
    }
}

val commonSourceSet = createGradleCommonSourceSet()
reconfigureMainSourcesSetForGradlePlugin(commonSourceSet)
publishShadowedJar(sourceSets[SourceSet.MAIN_SOURCE_SET_NAME], commonSourceSet)

// Disabling this task, so "com.gradle.plugin-publish" will not publish unshadowed jar into Gradle Plugin Portal
// Without it 'jar' task is asked to run by "com.gradle.plugin-publish" even if artifacts are removed. The problem
// is that 'jar' task runs after shadow task plus their outputs has the same name leading to '.jar' file overwrite.
tasks.named("jar") {
    enabled = false
}

if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
    // Used for Gradle 7.0 version
    val gradle70SourceSet = createGradlePluginVariant(
        GradlePluginVariant.GRADLE_70,
        commonSourceSet = commonSourceSet
    )
    publishShadowedJar(gradle70SourceSet, commonSourceSet)

    // Used for Gradle 7.1+ versions
    val gradle71SourceSet = createGradlePluginVariant(
        GradlePluginVariant.GRADLE_71,
        commonSourceSet = commonSourceSet
    )
    publishShadowedJar(gradle71SourceSet, commonSourceSet)

    // Used for Gradle 7.5+ versions
    val gradle75SourceSet = createGradlePluginVariant(
        GradlePluginVariant.GRADLE_75,
        commonSourceSet = commonSourceSet
    )
    publishShadowedJar(gradle75SourceSet, commonSourceSet)
}
