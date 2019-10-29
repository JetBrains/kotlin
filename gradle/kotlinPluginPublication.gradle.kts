apply(plugin = "maven-publish")

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("KotlinPlugin") {
            val artifactName = if (project.name == "idea-plugin") "kotlin-plugin" else project.name
            artifactId = "$artifactName-${IdeVersionConfigurator.currentIde.name.toLowerCase()}"
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            pom {
                name.set("${project.group}:$artifactId")
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
            url = uri(findProperty("deployRepoUrl") ?: findProperty("deploy-url") ?: "${rootProject.buildDir}/repo")
            authentication {
                val mavenUser = findProperty("kotlin.bintray.user") as String?
                val mavenPass = findProperty("kotlin.bintray.password") as String?
                if (url.scheme != "file" && mavenUser != null && mavenPass != null) {
                    credentials {
                        username = mavenUser
                        password = mavenPass
                    }
                }
            }
        }
    }
}

// Disable default `publish` task so publishing will not be done during maven artifact publish
// We should use specialized tasks since we have multiple publications in project
tasks.named("publish") {
    enabled = false
    dependsOn.clear()
}