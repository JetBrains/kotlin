/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin


class KotlinBuildPublishingPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply<MavenPublishPlugin>()
        apply<SigningPlugin>()

        val javaComponent = components.findByName("java") as AdhocComponentWithVariants?
        if (javaComponent != null) {
            val runtimeElements by configurations
            val apiElements by configurations

            val publishedRuntime = configurations.maybeCreate("publishedRuntime").apply {
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                }
                extendsFrom(runtimeElements)
            }

            val publishedCompile = configurations.maybeCreate("publishedCompile").apply {
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
                }
                extendsFrom(apiElements)
            }

            javaComponent.withVariantsFromConfiguration(apiElements) { skip() }

            javaComponent.addVariantsFromConfiguration(publishedCompile) { mapToMavenScope("compile") }
            javaComponent.addVariantsFromConfiguration(publishedRuntime) { mapToMavenScope("runtime") }
        }

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>(PUBLICATION_NAME) {
                    if (javaComponent != null) {
                        from(javaComponent)
                    }

                    pom {
                        packaging = "jar"
                        description.set(project.description)
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
            }

            repositories {
                maven {
                    name = REPOSITORY_NAME
                    url = file("${project.rootDir}/build/repo").toURI()
                }
            }
        }

        configure<SigningExtension> {
            setRequired(provider {
                project.findProperty("signingRequired")?.toString()?.toBoolean()
                    ?: project.property("isSonatypeRelease") as Boolean
            })

            sign(extensions.getByType<PublishingExtension>().publications[PUBLICATION_NAME])
        }

        tasks.register("install") {
            dependsOn(tasks.named("publishToMavenLocal"))
        }

        tasks.named<PublishToMavenRepository>("publish${PUBLICATION_NAME}PublicationTo${REPOSITORY_NAME}Repository") {
            dependsOn(project.rootProject.tasks.named("preparePublication"))
            doFirst {
                val preparePublication = project.rootProject.tasks.named("preparePublication").get()
                val username: String? by preparePublication.extra
                val password: String? by preparePublication.extra
                val repoUrl: String by preparePublication.extra

                repository.apply {
                    url = uri(repoUrl)
                    if (url.scheme != "file" && username != null && password != null) {
                        credentials {
                            this.username = username
                            this.password = password
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val PUBLICATION_NAME = "Main"
        const val REPOSITORY_NAME = "Maven"
    }
}