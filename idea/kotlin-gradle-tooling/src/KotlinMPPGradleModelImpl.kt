/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import java.io.File

class KotlinSourceSetImpl(
    override val name: String,
    override val languageSettings: KotlinLanguageSettings,
    override val sourceDirs: Set<File>,
    override val resourceDirs: Set<File>,
    override val dependencies: Set<KotlinDependency>,
    override val dependsOnSourceSets: Set<String>
) : KotlinSourceSet {
    override var platform: KotlinPlatform = KotlinPlatform.COMMON
        internal set

    override var isTestModule: Boolean = false
        internal set

    override fun toString() = name
}

class KotlinLanguageSettingsImpl(
    override val languageVersion: String?,
    override val apiVersion: String?,
    override val isProgressiveMode: Boolean,
    override val enabledLanguageFeatures: Set<String>
) : KotlinLanguageSettings

class KotlinCompilationOutputImpl(
    override val classesDirs: Set<File>,
    override val effectiveClassesDir: File?,
    override val resourcesDir: File?
) : KotlinCompilationOutput

class KotlinCompilationArgumentsImpl(
    override val defaultArguments: List<String>,
    override val currentArguments: List<String>
) : KotlinCompilationArguments

class KotlinCompilationImpl(
    override val name: String,
    override val sourceSets: Collection<KotlinSourceSet>,
    override val dependencies: Set<KotlinDependency>,
    override val output: KotlinCompilationOutput,
    override val arguments: KotlinCompilationArguments,
    override val dependencyClasspath: List<String>
) : KotlinCompilation {
    override lateinit var target: KotlinTarget
        internal set

    override val platform: KotlinPlatform
        get() = target.platform

    override val isTestModule: Boolean
        get() = name == KotlinCompilation.TEST_COMPILATION_NAME

    override fun toString() = name
}

class KotlinTargetJarImpl(
    override val archiveFile: File?
) : KotlinTargetJar

class KotlinTargetImpl(
    override val name: String,
    override val disambiguationClassifier: String?,
    override val platform: KotlinPlatform,
    override val compilations: Collection<KotlinCompilation>,
    override val jar: KotlinTargetJar?
) : KotlinTarget {
    override fun toString() = name
}

class ExtraFeaturesImpl(
    override val coroutinesState: String?
) : ExtraFeatures

class KotlinMPPGradleModelImpl(
    override val sourceSets: Map<String, KotlinSourceSet>,
    override val targets: Collection<KotlinTarget>,
    override val extraFeatures: ExtraFeatures
) : KotlinMPPGradleModel