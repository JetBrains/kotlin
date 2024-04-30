plugins {
    kotlin("jvm") version "1.9.23"
    `java-gradle-plugin`
    `maven-publish`
}

group = "org.jetbrains.kotlin"

dependencies {
    compileOnly("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:1.9.23")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("apple-privacy-manifests") {
            id = "org.jetbrains.kotlin.apple-privacy-manifests"
            implementationClass = "org.jetbrains.kotlin.PrivacyManifestsPlugin"
        }
    }
}

val functionalTest by sourceSets.creating
gradlePlugin.testSourceSets(functionalTest)

configurations[functionalTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())

val cleanFunctionalTest = tasks.register<Delete>("cleanFunctionalTest") {
    setDelete(layout.buildDirectory.dir("functionalTest"))
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = configurations[functionalTest.runtimeClasspathConfigurationName] + functionalTest.output
    dependsOn(
        tasks.named("publishAllPublicationsToMavenRepository"),
        cleanFunctionalTest
    )
    useJUnitPlatform()
}

publishing {
    repositories {
        maven(layout.projectDirectory.dir("repo"))
    }
}