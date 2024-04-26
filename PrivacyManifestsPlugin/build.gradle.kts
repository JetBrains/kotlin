plugins {
    kotlin("jvm") version "1.9.23"
    `java-gradle-plugin`
    `maven-publish`
}

group = "org.kmp_apple_privacy_manifests"

dependencies {
    compileOnly("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:1.9.23")
    testImplementation("junit:junit:4.13.1")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("kmp_apple_privacy_manifests") {
            id = "org.kmp_apple_privacy_manifests.publication"
            implementationClass = "org.kmp_apple_privacy_manifests.PrivacyManifestsPlugin"
        }
    }
}

val functionalTest by sourceSets.creating
gradlePlugin.testSourceSets(functionalTest)

configurations[functionalTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())

val functionalTestTask = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = configurations[functionalTest.runtimeClasspathConfigurationName] + functionalTest.output
    dependsOn(
        tasks.named("publishAllPublicationsToMavenRepository")
    )
}

publishing {
    repositories {
        maven(layout.projectDirectory.dir("repo"))
    }
}