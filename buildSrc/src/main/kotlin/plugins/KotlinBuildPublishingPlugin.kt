/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import java.util.*
import javax.inject.Inject

class KotlinBuildPublishingPlugin @Inject constructor(
    private val componentFactory: SoftwareComponentFactory
) : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply<MavenPublishPlugin>()
        apply<SigningPlugin>()

        val publishedRuntime = configurations.maybeCreate(RUNTIME_CONFIGURATION).apply {
            isCanBeConsumed = false
            isCanBeResolved = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            }
        }

        val publishedCompile = configurations.maybeCreate(COMPILE_CONFIGURATION).apply {
            isCanBeConsumed = false
            isCanBeResolved = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
            }
        }

        val kotlinLibraryComponent = componentFactory.adhoc(ADHOC_COMPONENT_NAME)
        components.add(kotlinLibraryComponent)
        kotlinLibraryComponent.addVariantsFromConfiguration(publishedCompile) { mapToMavenScope("compile") }
        kotlinLibraryComponent.addVariantsFromConfiguration(publishedRuntime) { mapToMavenScope("runtime") }

        pluginManager.withPlugin("java-base") {
            val runtimeElements by configurations
            val apiElements by configurations

            publishedRuntime.extendsFrom(runtimeElements)
            publishedCompile.extendsFrom(apiElements)

            kotlinLibraryComponent.addVariantsFromConfiguration(runtimeElements) {
                mapToMavenScope("runtime")

                if (configurationVariant.artifacts.any { JavaBasePlugin.UNPUBLISHABLE_VARIANT_ARTIFACTS.contains(it.type) }) {
                    skip()
                }
            }
        }

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>(PUBLICATION_NAME) {
                    from(kotlinLibraryComponent)

                    configureKotlinPomAttributes(project)
                }
            }
        }
        configureDefaultPublishing()
    }

    companion object {
        const val PUBLICATION_NAME = "Main"
        const val REPOSITORY_NAME = "Maven"
        const val ADHOC_COMPONENT_NAME = "kotlinLibrary"

        const val COMPILE_CONFIGURATION = "publishedCompile"
        const val RUNTIME_CONFIGURATION = "publishedRuntime"

    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun humanReadableName(name: String) =
    name.split("-").joinToString(separator = " ") { it.capitalize(Locale.ROOT) }

fun MavenPublication.configureKotlinPomAttributes(project: Project, explicitDescription: String? = null) {
    val publication = this
    pom {
        packaging = "jar"
        name.set(humanReadableName(publication.artifactId))
        description.set(explicitDescription ?: project.description ?: humanReadableName(publication.artifactId))
        url.set("https://kotlinlang.org/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        scm {
            url.set("https://github.com/JetBrains/kotlin")
            connection.set("scm:git:https://github.com/JetBrains/kotlin.git")
            developerConnection.set("scm:git:https://github.com/JetBrains/kotlin.git")
        }
        developers {
            developer {
                name.set("Kotlin Team")
                organization.set("JetBrains")
                organizationUrl.set("https://www.jetbrains.com")
            }
        }
    }
}


fun Project.configureDefaultPublishing() {
    configure<PublishingExtension> {
        repositories {
            maven {
                name = KotlinBuildPublishingPlugin.REPOSITORY_NAME
                url = file("${project.rootDir}/build/repo").toURI()
            }
        }
    }

    configureSigning()

    tasks.register("install") {
        dependsOn(tasks.named("publishToMavenLocal"))
    }

    tasks.withType<PublishToMavenRepository>()
        .matching { it.name.endsWith("PublicationTo${KotlinBuildPublishingPlugin.REPOSITORY_NAME}Repository") }
        .all { configureRepository() }
}

private fun Project.configureSigning() {
    val signingRequired = provider {
        project.findProperty("signingRequired")?.toString()?.toBoolean()
            ?: project.property("isSonatypeRelease") as Boolean
    }

    configure<SigningExtension> {
        setRequired(signingRequired)
        sign(extensions.getByType<PublishingExtension>().publications) // all publications
        useGpgCmd()
    }

    tasks.withType<Sign>().configureEach {
        setOnlyIf { signingRequired.get() }
    }
}

fun TaskProvider<PublishToMavenRepository>.configureRepository() =
    configure { configureRepository() }

private fun PublishToMavenRepository.configureRepository() {
    dependsOn(project.rootProject.tasks.named("preparePublication"))
    doFirst {
        val preparePublication = project.rootProject.tasks.named("preparePublication").get()
        val username: String? by preparePublication.extra
        val password: String? by preparePublication.extra
        val repoUrl: String by preparePublication.extra

        repository.apply {
            url = project.uri(repoUrl)
            if (url.scheme != "file" && username != null && password != null) {
                credentials {
                    this.username = username
                    this.password = password
                }
            }
        }
    }
}
