description = "Kotlin Daemon Client"

plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    api(kotlin("stdlib", coreDepsVersion))
    compileOnly(project(":daemon-common")) { exclude("org.jetbrains.kotlin", "kotlin-stdlib") }
    compileOnly(project(":js:js.config")) { exclude("org.jetbrains.kotlin", "kotlin-stdlib") }

    embedded(project(":daemon-common")) { isTransitive = false }
    testCompileOnly(project(":daemon-common"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testRuntimeOnly(libs.junit.jupiter.engine)
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}

configureKotlinCompileTasksGradleCompatibility()

publish()

runtimeJar()
sourcesJar()
javadocJar()
