package plugins

import org.codehaus.groovy.runtime.InvokerHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.maven.GroovyMavenDeployer

import org.gradle.api.plugins.MavenRepositoryHandlerConvention
import org.gradle.api.tasks.Upload
import org.gradle.kotlin.dsl.*


/**
 * Configures a Kotlin module for publication.
 *
 */
open class PublishedKotlinModule : Plugin<Project> {

    override fun apply(project: Project) {

        project.run {

            plugins.apply("maven")

            (tasks.getByName("uploadArchives") as Upload).apply {

                val preparePublication = project.rootProject.tasks.getByName("preparePublication")
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
                                            "organization" {
                                                "name"("JetBrains")
                                                "url"("https://www.jetbrains.com")
                                            }
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
                    }
                }
            }
        }
    }
}
