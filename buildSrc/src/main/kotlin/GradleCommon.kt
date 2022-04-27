/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.plugin.GradlePluginApiVersion
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin
import org.jetbrains.dokka.DokkaVersion
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.project.model.KotlinPlatformTypeAttribute
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes
import java.util.*

/**
 * Gradle plugins common variants.
 *
 * [minimalSupportedGradleVersion] - minimal Gradle version that is supported in this variant
 * [gradleApiVersion] - Gradle API dependency version. Usually should be the same as [minimalSupportedGradleVersion].
 */
enum class GradlePluginVariant(
    val sourceSetName: String,
    val minimalSupportedGradleVersion: String,
    val gradleApiVersion: String
) {
    GRADLE_MIN("main", "6.7", "6.9"),
    GRADLE_70("gradle70", "7.0", "7.0"),
    //GRADLE_71("gradle71", "7.1", "7.1"),
}

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
 * Common sources for all variants.
 * Should contain classes that are independent of Gradle API version or using minimal supported Gradle api.
 */
fun Project.createGradleCommonSourceSet(): SourceSet {
    val commonSourceSet = sourceSets.create("common") {
        excludeGradleCommonDependencies(this)

        // Adding Gradle API to separate configuration, so version will not leak into variants
        val commonGradleApiConfiguration = configurations.create("commonGradleApiCompileOnly") {
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = true
        }
        configurations[compileClasspathConfigurationName].extendsFrom(commonGradleApiConfiguration)

        dependencies {
            compileOnlyConfigurationName(kotlinStdlib())
            "commonGradleApiCompileOnly"("dev.gradleplugins:gradle-api:7.2")
            if (this@createGradleCommonSourceSet.name != "kotlin-gradle-plugin-api" &&
                this@createGradleCommonSourceSet.name != "android-test-fixes"
            ) {
                compileOnlyConfigurationName(project(":kotlin-gradle-plugin-api")) {
                    capabilities {
                        requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-api-common")
                    }
                }
            }
        }
    }

    plugins.withType<JavaLibraryPlugin>().configureEach {
        this@createGradleCommonSourceSet.extensions.configure<JavaPluginExtension> {
            registerFeature(commonSourceSet.name) {
                usingSourceSet(commonSourceSet)
                disablePublication()
            }
        }
    }

    // Common outputs will also produce '${project.name}.kotlin_module' file, so we need to avoid
    // files clash
    tasks.named<KotlinCompile>("compile${commonSourceSet.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}Kotlin") {
        kotlinOptions {
            moduleName = "${this@createGradleCommonSourceSet.name}_${commonSourceSet.name}"
        }
    }

    return commonSourceSet
}

/**
 * Fixes wired SourceSet does not expose compiled common classes and common resources as secondary variant
 * which is used in the Kotlin Project compilation.
 */
private fun Project.fixWiredSourceSetSecondaryVariants(
    wireSourceSet: SourceSet,
    commonSourceSet: SourceSet
) {
    configurations
        .matching {
            it.name == wireSourceSet.apiElementsConfigurationName ||
                    it.name == wireSourceSet.runtimeElementsConfigurationName
        }
        .configureEach {
            outgoing {
                variants.maybeCreate("classes").apply {
                    attributes {
                        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
                    }
                    (commonSourceSet.output.classesDirs.files + wireSourceSet.output.classesDirs.files)
                        .toSet()
                        .forEach {
                            if (!artifacts.files.contains(it)) {
                                artifact(it) {
                                    type = ArtifactTypeDefinition.JVM_CLASS_DIRECTORY
                                }
                            }
                        }
                }
            }
        }

    configurations
        .matching { it.name == wireSourceSet.runtimeElementsConfigurationName }
        .configureEach {
            outgoing {
                val resourcesDirectories = listOfNotNull(
                    commonSourceSet.output.resourcesDir,
                    wireSourceSet.output.resourcesDir
                )

                if (resourcesDirectories.isNotEmpty()) {
                    variants.maybeCreate("resources").apply {
                        attributes {
                            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.RESOURCES))
                        }
                        resourcesDirectories.forEach {
                            if (!artifacts.files.contains(it)) {
                                artifact(it) {
                                    type = ArtifactTypeDefinition.JVM_RESOURCES_DIRECTORY
                                }
                            }
                        }
                    }
                }
            }
        }
}

/**
 * Make [wireSourceSet] to extend [commonSourceSet].
 */
fun Project.wireGradleVariantToCommonGradleVariant(
    wireSourceSet: SourceSet,
    commonSourceSet: SourceSet
) {
    wireSourceSet.compileClasspath += commonSourceSet.output
    wireSourceSet.runtimeClasspath += commonSourceSet.output

    // Allowing to use 'internal' classes/methods from common source code
    (extensions.getByName("kotlin") as KotlinSingleTargetExtension).target.compilations.run {
        getByName(wireSourceSet.name).associateWith(getByName(commonSourceSet.name))
    }

    configurations[wireSourceSet.apiConfigurationName].extendsFrom(
        configurations[commonSourceSet.apiConfigurationName]
    )
    configurations[wireSourceSet.implementationConfigurationName].extendsFrom(
        configurations[commonSourceSet.implementationConfigurationName]
    )
    configurations[wireSourceSet.runtimeOnlyConfigurationName].extendsFrom(
        configurations[commonSourceSet.runtimeOnlyConfigurationName]
    )
    configurations[wireSourceSet.compileOnlyConfigurationName].extendsFrom(
        configurations[commonSourceSet.compileOnlyConfigurationName]
    )

    fixWiredSourceSetSecondaryVariants(wireSourceSet, commonSourceSet)

    tasks.withType<Jar>().configureEach {
        if (name == wireSourceSet.jarTaskName) {
            from(wireSourceSet.output, commonSourceSet.output)
            setupPublicJar(archiveBaseName.get())
            addEmbeddedRuntime()
        } else if (name == wireSourceSet.sourcesJarTaskName) {
            from(wireSourceSet.allSource, commonSourceSet.allSource)
        }
    }
}

/**
 * 'main' sources are used for minimal supported Gradle versions (6.7) up to Gradle 7.0.
 */
fun Project.reconfigureMainSourcesSetForGradlePlugin(
    commonSourceSet: SourceSet
) {
    sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME) {
        plugins.withType<JavaGradlePluginPlugin>().configureEach {
            // Removing Gradle api default dependency added by 'java-gradle-plugin'
            configurations[apiConfigurationName].dependencies.remove(dependencies.gradleApi())
        }

        dependencies {
            "compileOnly"(kotlinStdlib())
            // Decoupling gradle-api artifact from current project Gradle version. Later would be useful for
            // gradle plugin variants
            "compileOnly"("dev.gradleplugins:gradle-api:${GradlePluginVariant.GRADLE_MIN.gradleApiVersion}")
            if (this@reconfigureMainSourcesSetForGradlePlugin.name != "kotlin-gradle-plugin-api" &&
                this@reconfigureMainSourcesSetForGradlePlugin.name != "android-test-fixes"
            ) {
                "api"(project(":kotlin-gradle-plugin-api"))
            }
        }

        excludeGradleCommonDependencies(this)
        wireGradleVariantToCommonGradleVariant(this, commonSourceSet)

        // https://youtrack.jetbrains.com/issue/KT-51913
        // Remove workaround after bootstrap update
        if (configurations["default"].attributes.contains(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE)) {
            configurations["default"].attributes.attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment::class, "no-op")
            )
        }

        plugins.withType<JavaLibraryPlugin>().configureEach {
            this@reconfigureMainSourcesSetForGradlePlugin
                .extensions
                .configure<JavaPluginExtension> {
                    withSourcesJar()
                    if (kotlinBuildProperties.publishGradlePluginsJavadoc) {
                        withJavadocJar()
                    }
                }
        }

        plugins.withId("org.jetbrains.dokka") {
            val dokkaTask = tasks.named<DokkaTask>("dokkaJavadoc") {
                dokkaSourceSets {
                    named(commonSourceSet.name) {
                        suppress.set(false)
                    }

                    named("main") {
                        dependsOn(commonSourceSet)
                    }
                }
            }

            tasks.withType<Jar>().configureEach {
                if (name == javadocJarTaskName) {
                    from(dokkaTask.flatMap { it.outputDirectory })
                }
            }
        }
    }

    // Fix common sources visibility for tests
    sourceSets.named(SourceSet.TEST_SOURCE_SET_NAME) {
        compileClasspath += commonSourceSet.output
        runtimeClasspath += commonSourceSet.output
    }

    // Allowing to use 'internal' classes/methods from common source code
    (extensions.getByName("kotlin") as KotlinSingleTargetExtension).target.compilations.run {
        getByName(SourceSet.TEST_SOURCE_SET_NAME).associateWith(getByName(commonSourceSet.name))
    }
}

/**
 * Adding plugin variants: https://docs.gradle.org/current/userguide/implementing_gradle_plugins.html#plugin-with-variants
 */
fun Project.createGradlePluginVariant(
    variant: GradlePluginVariant,
    commonSourceSet: SourceSet,
    isGradlePlugin: Boolean = true
): SourceSet {
    val variantSourceSet = sourceSets.create(variant.sourceSetName) {
        excludeGradleCommonDependencies(this)
        wireGradleVariantToCommonGradleVariant(this, commonSourceSet)
    }

    plugins.withType<JavaLibraryPlugin>().configureEach {
        extensions.configure<JavaPluginExtension> {
            registerFeature(variantSourceSet.name) {
                usingSourceSet(variantSourceSet)
                if (isGradlePlugin) {
                    capability(project.group.toString(), project.name, project.version.toString())
                }

                if (kotlinBuildProperties.publishGradlePluginsJavadoc) {
                    withJavadocJar()
                }
                withSourcesJar()
            }

            configurations.named(variantSourceSet.apiElementsConfigurationName, commonVariantAttributes())
            configurations.named(variantSourceSet.runtimeElementsConfigurationName, commonVariantAttributes())
        }

        tasks.named<Jar>(variantSourceSet.sourcesJarTaskName) {
            addEmbeddedSources()
        }
    }

    if (kotlinBuildProperties.publishGradlePluginsJavadoc) {
        plugins.withId("org.jetbrains.dokka") {
            val dokkaTask = tasks.register<DokkaTask>(
                "dokka${
                    variantSourceSet.javadocTaskName.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()
                        ) else it.toString()
                    }
                }") {
                description = "Generates documentation in 'javadoc' format for '${variantSourceSet.javadocTaskName}' variant"

                plugins.dependencies.add(
                    project.dependencies.create("org.jetbrains.dokka:javadoc-plugin:${DokkaVersion.version}")
                )

                dokkaSourceSets {
                    named(commonSourceSet.name) {
                        suppress.set(false)
                    }

                    named(variantSourceSet.name) {
                        dependsOn(commonSourceSet)
                        suppress.set(false)
                    }
                }
            }

            tasks.named<Jar>(variantSourceSet.javadocJarTaskName) {
                from(dokkaTask.flatMap { it.outputDirectory })
            }
        }
    }

    plugins.withId("java-gradle-plugin") {
        tasks.named<Copy>(variantSourceSet.processResourcesTaskName) {
            val copyPluginDescriptors = rootSpec.addChild()
            copyPluginDescriptors.into("META-INF/gradle-plugins")
            copyPluginDescriptors.from(tasks.named("pluginDescriptors"))
        }
    }

    configurations.configureEach {
        if (isCanBeConsumed && this@configureEach.name.startsWith(variantSourceSet.name)) {
            attributes {
                attribute(
                    GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                    objects.named(variant.minimalSupportedGradleVersion)
                )
            }
        }
    }

    // KT-52138: Make module name the same for all variants, so KSP could access internal methods/properties
    tasks.named<KotlinCompile>("compile${variantSourceSet.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}Kotlin") {
        kotlinOptions {
            moduleName = this@createGradlePluginVariant.name
        }
    }

    dependencies {
        variantSourceSet.compileOnlyConfigurationName(kotlinStdlib())
        variantSourceSet.compileOnlyConfigurationName("dev.gradleplugins:gradle-api:${variant.gradleApiVersion}")
        if (this@createGradlePluginVariant.name != "kotlin-gradle-plugin-api" &&
            this@createGradlePluginVariant.name != "android-test-fixes"
        ) {
            variantSourceSet.apiConfigurationName(project(":kotlin-gradle-plugin-api")) {
                capabilities {
                    requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-api-${variant.sourceSetName}")
                }
            }
        }
    }

    return variantSourceSet
}

/**
 * All additional configuration attributes in plugin variant should be the same as in the 'main' variant.
 * Otherwise, Gradle <7.0 will fail to select plugin variant.
 */
private fun Project.commonVariantAttributes(): Action<Configuration> = Action<Configuration> {
    attributes {
        attribute(
            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            objects.named(TargetJvmEnvironment.STANDARD_JVM)
        )
        attribute(
            Attribute.of(KotlinPlatformTypeAttribute.uniqueName, String::class.java),
            KotlinPlatformTypeAttribute.JVM
        )
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
    sourceSet: SourceSet,
    commonSourceSet: SourceSet
) {
    val jarTask = tasks.named<Jar>(sourceSet.jarTaskName)

    val shadowJarTask = embeddableCompilerDummyForDependenciesRewriting(
        taskName = "$EMBEDDABLE_COMPILER_TASK_NAME${sourceSet.jarTaskName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
    ) {
        setupPublicJar(
            jarTask.flatMap { it.archiveBaseName },
            jarTask.flatMap { it.archiveClassifier }
        )
        addEmbeddedRuntime()
        from(sourceSet.output)
        from(commonSourceSet.output)

        // When Gradle traverses the inputs, reject the shaded compiler JAR,
        // which leads to the content of that JAR being excluded as well:
        val compilerDummyJarConfiguration: FileCollection = project.configurations.getByName("compilerDummyJar")
        exclude { it.file == compilerDummyJarConfiguration.singleFile }
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
