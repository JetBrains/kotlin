/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.plugins.gradle.model.ExternalDependency
import java.io.File
import java.io.Serializable

typealias KotlinDependency = ExternalDependency

interface KotlinModule : Serializable {
    val name: String
    val platform: KotlinPlatform
    val dependencies: Set<KotlinDependency>
    val isTestModule: Boolean
}

interface KotlinSourceSet : KotlinModule {
    val languageSettings: KotlinLanguageSettings
    val sourceDirs: Set<File>
    val resourceDirs: Set<File>
    val dependsOnSourceSets: Set<String>
}

interface KotlinLanguageSettings : Serializable {
    val languageVersion: String?
    val apiVersion: String?
    val isProgressiveMode: Boolean
    val enabledLanguageFeatures: Set<String>
}

interface KotlinCompilationOutput : Serializable {
    val classesDirs: Set<File>
    val effectiveClassesDir: File?
    val resourcesDir: File?
}

interface KotlinCompilationArguments : Serializable {
    val defaultArguments: List<String>
    val currentArguments: List<String>
}

interface KotlinCompilation : KotlinModule {
    val sourceSets: Collection<KotlinSourceSet>
    val target: KotlinTarget
    val output: KotlinCompilationOutput
    val arguments: KotlinCompilationArguments
    val dependencyClasspath: List<String>

    companion object {
        const val MAIN_COMPILATION_NAME = "main"
        const val TEST_COMPILATION_NAME = "test"
    }
}

enum class KotlinPlatform(val id: String) {
    COMMON("common"),
    JVM("jvm"),
    JS("js"),
    ANDROID("androidJvm");

    companion object {
        fun byId(id: String) = values().firstOrNull { it.id == id }
    }
}

interface KotlinTargetJar : Serializable {
    val archiveFile: File?
}

interface KotlinTarget : Serializable {
    val name: String
    val disambiguationClassifier: String?
    val platform: KotlinPlatform
    val compilations: Collection<KotlinCompilation>
    val jar: KotlinTargetJar?
}

interface ExtraFeatures : Serializable {
    val coroutinesState: String?
}

interface KotlinMPPGradleModel : Serializable {
    val sourceSets: Map<String, KotlinSourceSet>
    val targets: Collection<KotlinTarget>
    val extraFeatures: ExtraFeatures
}