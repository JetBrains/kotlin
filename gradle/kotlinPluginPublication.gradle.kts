apply(plugin = "maven-publish")

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("KotlinPlugin") {
            val artifactName = if (project.name == "idea-plugin") "kotlin-plugin" else project.name
            artifactId = "$artifactName-${IdeVersionConfigurator.currentIde.name.toLowerCase()}"
            from(components["java"])
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