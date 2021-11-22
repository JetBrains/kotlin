/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import com.gradle.publish.PluginBundleExtension

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("org.jetbrains.dokka")
    `maven-publish`
    id("com.gradle.plugin-publish")
}

configureCommonPublicationSettingsForGradle()
configureKotlinCompileTasksGradleCompatibility()
extensions.extraProperties["kotlin.stdlib.default.dependency"] = "false"

// common plugin bundle configuration
configure<PluginBundleExtension> {
    website = "https://kotlinlang.org/"
    vcsUrl = "https://github.com/jetbrains/kotlin"
    tags = listOf("kotlin")
}

configureGradlePluginCommonSettings()
publishShadowedJar(sourceSets[SourceSet.MAIN_SOURCE_SET_NAME])

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
