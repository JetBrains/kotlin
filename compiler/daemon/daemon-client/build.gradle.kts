description = "Kotlin Daemon Client"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    api("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
    compileOnly(project(":daemon-common")) { exclude("org.jetbrains.kotlin", "kotlin-stdlib") }
    compileOnly(project(":js:js.config")) { exclude("org.jetbrains.kotlin", "kotlin-stdlib") }

    embedded(project(":daemon-common")) { isTransitive = false }
    testCompileOnly(project(":daemon-common"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}

configureKotlinCompileTasksGradleCompatibility()

publish()

runtimeJar()
sourcesJar()
javadocJar()
