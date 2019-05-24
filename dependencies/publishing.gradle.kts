import com.jfrog.bintray.gradle.BintrayExtension

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2")
    }

    dependencies {
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
    }
}

apply(plugin = "maven-publish")
apply(plugin = "com.jfrog.bintray")

val archives by configurations

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            setArtifacts(archives.artifacts)
        }
    }

    repositories {
        maven {
            url = uri("${rootProject.buildDir}/internal/repo")
        }
    }
}

configure<BintrayExtension> {
    user = findProperty("bintray.user") as String?
    key = findProperty("bintray.apikey") as String?

    setPublications("maven")

    pkg.apply {
        repo = "kotlin-dependencies"
        name = project.name
        userOrg = "kotlin"
    }
}