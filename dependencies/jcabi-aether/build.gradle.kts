plugins {
    `java-library`
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
}

group = "org.jetbrains.kotlin"
version = "1.0-dev-3"

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
}

val aether by configurations.creating
dependencies {
    aether("com.jcabi:jcabi-aether:1.0-SNAPSHOT")
}

val jar = tasks.named<Jar>("jar") {
    from(provider {
        provider {
            zipTree(
                aether.resolvedConfiguration
                    .firstLevelModuleDependencies
                    .single()
                    .moduleArtifacts
                    .single()
                    .file
            )
        }
    })
}

apply(from="../publishing.gradle.kts")

configure<PublishingExtension> {
    publications.getByName<MavenPublication>("maven") {
        pom.withXml {
            asNode().appendNode("dependencies").apply {
                aether.resolvedConfiguration.firstLevelModuleDependencies.single().children.forEach {
                    appendNode("dependency").apply {
                        appendNode("groupId", it.moduleGroup)
                        appendNode("artifactId", it.moduleName)
                        appendNode("version", it.moduleVersion)
                        appendNode("scope", "compile")
                    }
                }
            }
        }
    }
}
