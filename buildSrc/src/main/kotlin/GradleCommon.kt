/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

/**
 * Configures common pom configuration parameters
 */
fun Project.configureCommonPublicationSettingsForGradle() {
    plugins.withId("maven-publish") {
        configureDefaultPublishing()

        extensions.configure<PublishingExtension> {
            publications
                .withType<MavenPublication>()
                .configureEach {
                    configureKotlinPomAttributes(project)
                }
        }
    }
}

/**
 * These dependencies will be provided by Gradle, and we should prevent version conflict
 */
fun Configuration.excludeGradleCommonDependencies() {
    dependencies
        .withType<ModuleDependency>()
        .configureEach {
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-script-runtime")
        }
}

/**
 * Exclude Gradle runtime from given SourceSet configurations.
 */
fun Project.excludeGradleCommonDependencies(sourceSet: SourceSet) {
    configurations[sourceSet.implementationConfigurationName].excludeGradleCommonDependencies()
    configurations[sourceSet.apiConfigurationName].excludeGradleCommonDependencies()
    configurations[sourceSet.runtimeOnlyConfigurationName].excludeGradleCommonDependencies()
}

/**
 * 'main' sources are used for Gradle 6.1-6.9 versions.
 * Directories are renamed into 'src/gradle61'.
 */
fun Project.configureGradlePluginCommonSettings() {
    sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME) {
        plugins.withType<JavaGradlePluginPlugin>().configureEach {
            // Removing Gradle api default dependency added by 'java-gradle-plugin'
            configurations[apiConfigurationName].dependencies.remove(dependencies.gradleApi())
        }

        dependencies {
            "compileOnly"(kotlinStdlib())
            // Decoupling gradle-api artifact from current project Gradle version. Later would be useful for
            // gradle plugin variants
            "compileOnly"("dev.gradleplugins:gradle-api:7.1.1")
            if (this@configureGradlePluginCommonSettings.name != "kotlin-gradle-plugin-api") {
                "api"(project(":kotlin-gradle-plugin-api"))
            }
        }

        excludeGradleCommonDependencies(this)

        tasks.withType<Jar>().configureEach {
            if (name == jarTaskName) {
                setupPublicJar(archiveBaseName.get())
                addEmbeddedRuntime()
            } else if (name == sourcesJarTaskName) {
                addEmbeddedSources()
            }
        }

        plugins.withType<JavaLibraryPlugin>().configureEach {
            this@configureGradlePluginCommonSettings
                .extensions
                .configure<JavaPluginExtension> {
                    withSourcesJar()
                    withJavadocJar()
                }
        }

        plugins.withId("org.jetbrains.dokka") {
            val dokkaTask = tasks.named<DokkaTask>("dokkaJavadoc")

            tasks.withType<Jar>().configureEach {
                if (name == javadocJarTaskName) {
                    from(dokkaTask.flatMap { it.outputDirectory })
                }
            }
        }
    }
}

fun Project.configureKotlinCompileTasksGradleCompatibility() {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.languageVersion = "1.4"
        kotlinOptions.apiVersion = "1.4"
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xskip-prerelease-check",
            "-Xsuppress-version-warnings",
            "-Xuse-ir" // Needed as long as languageVersion is less than 1.5.
        )
    }
}

// Will allow combining outputs of multiple SourceSets
fun Project.publishShadowedJar(
    sourceSet: SourceSet
) {
    val jarTask = tasks.named<Jar>(sourceSet.jarTaskName)

    val shadowJarTask = embeddableCompilerDummyForDependenciesRewriting(
        taskName = "$EMBEDDABLE_COMPILER_TASK_NAME${sourceSet.jarTaskName.capitalize()}"
    ) {
        setupPublicJar(
            jarTask.flatMap { it.archiveBaseName },
            jarTask.flatMap { it.archiveClassifier }
        )
        addEmbeddedRuntime()
        from(sourceSet.output)

        // When Gradle traverses the inputs, reject the shaded compiler JAR,
        // which leads to the content of that JAR being excluded as well:
        val compilerDummyJarFile = project.provider { project.configurations.getByName("compilerDummyJar").singleFile }
        exclude { it.file == compilerDummyJarFile.get() }
    }

    // Removing artifact produced by Jar task
    configurations[sourceSet.runtimeElementsConfigurationName]
        .artifacts.removeAll { true }
    configurations[sourceSet.apiElementsConfigurationName]
        .artifacts.removeAll { true }

    // Adding instead artifact from shadow jar task
    configurations {
        artifacts {
            add(sourceSet.runtimeElementsConfigurationName, shadowJarTask)
            add(sourceSet.apiElementsConfigurationName, shadowJarTask)
        }
    }
}