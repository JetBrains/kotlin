package plugins

import org.codehaus.groovy.runtime.InvokerHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.maven.MavenResolver

import org.gradle.api.plugins.MavenRepositoryHandlerConvention
import org.gradle.api.tasks.Upload
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension


/**
 * Configures a Kotlin module for publication.
 *
 */
open class PublishedKotlinModule : Plugin<Project> {

    override fun apply(project: Project) {

        project.run {

            plugins.apply("maven")

            if (!project.hasProperty("prebuiltJar")) {
                plugins.apply("signing")

                val signingProp = project.rootProject.properties["signingRequired"]
                val signingRequired = when (signingProp) {
                    is Boolean -> signingProp == true
                    is String -> listOf("true", "yes").contains(signingProp.toLowerCase().trim())
                    else -> project.rootProject.extra["isSonatypeRelease"] as? Boolean == true
                }

                configure<SigningExtension> {
                    isRequired = signingRequired
                    sign(configurations["archives"])
                }

                (tasks.getByName("signArchives") as Sign).apply {
                    enabled = signingRequired
                }
            }

            fun MavenResolver.configurePom() {
                pom.project {
                    withGroovyBuilder {
                        "licenses" {
                            "license" {
                                "name"("The Apache Software License, Version 2.0")
                                "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                "distribution"("repo")
                            }
                        }
                        "name"("${project.group}:${project.name}")
                        "packaging"("jar")
                        // optionally artifactId can be defined here
                        "description"(project.description)
                        "url"("https://kotlinlang.org/")
                        "licenses" {
                            "license" {
                                "name"("The Apache License, Version 2.0")
                                "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        "scm" {
                            "url"("https://github.com/JetBrains/kotlin")
                            "connection"("scm:git:https://github.com/JetBrains/kotlin.git")
                            "developerConnection"("scm:git:https://github.com/JetBrains/kotlin.git")
                        }
                        "developers" {
                            "developer" {
                                "name"("Kotlin Team")
                                setProperty("organization", "JetBrains")
                                "organizationUrl"("https://www.jetbrains.com")
                            }
                        }
                    }
                }
                pom.whenConfigured {
                    dependencies.removeIf {
                        InvokerHelper.getMetaClass(it).getProperty(it, "scope") == "test"
                    }
                }
            }

            val preparePublication = project.rootProject.tasks.getByName("preparePublication")

            val uploadArchives = (tasks.getByName("uploadArchives") as Upload).apply {

                dependsOn(preparePublication)

                val username: String? by preparePublication.extra
                val password: String? by preparePublication.extra

                repositories {
                    withConvention(MavenRepositoryHandlerConvention::class) {

                        mavenDeployer {
                            withGroovyBuilder {
                                "repository"("url" to uri(preparePublication.extra["repoUrl"]))

                                if (username != null && password != null) {
                                    "authentication"("userName" to username, "password" to password)
                                }
                            }

                            configurePom()
                        }
                    }
                }
            }

            val install = if (tasks.names.contains("install")) tasks.getByName("install") as Upload
                          else tasks.create("install", Upload::class.java)
            install.apply {
                configuration = project.configurations.getByName(Dependency.ARCHIVES_CONFIGURATION)
                description = "Installs the 'archives' artifacts into the local Maven repository."
                repositories {
                    withConvention(MavenRepositoryHandlerConvention::class) {
                        mavenInstaller {
                            configurePom()
                        }
                    }
                }
            }

            tasks.create("publish") {
                dependsOn(uploadArchives)
            }
        }
    }
}
