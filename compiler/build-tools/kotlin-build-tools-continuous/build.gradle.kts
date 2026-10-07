import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Continuous incremental compilation on top of the Kotlin Build Tools API"

plugins {
    id("common-configuration")
    kotlin("jvm")
    id("test-inputs-check")
}

configureKotlinCompileTasksGradleCompatibility()

val btaImplementation = configurations.detachedConfiguration(
    dependencies.project(":compiler:build-tools:kotlin-build-tools-impl")
)
val errorTolerancePlugin = configurations.detachedConfiguration(
    dependencies.project(":plugins:error-tolerance:compiler-plugin-embeddable")
).apply { isTransitive = false }

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))
    api(project(":compiler:build-tools:kotlin-build-tools-api"))

    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    explicitApi()
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}

publish()

standardPublicJars()

projectTests {
    testTask {
        addClasspathProperty(btaImplementation, "kotlin.build-tools.continuous.test.implementationClasspath")
        addClasspathProperty(errorTolerancePlugin, "kotlin.build-tools.continuous.test.errorTolerancePlugin")
    }
    withJvmStdlibAndReflect()
}
