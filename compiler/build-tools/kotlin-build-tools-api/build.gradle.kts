import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    compileOnly(kotlinStdlib())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupyter.api)
    testRuntimeOnly(libs.junit.jupyter.engine)
    testImplementation(kotlinStdlib())
}

kotlin {
    explicitApi()
}

publish()

standardPublicJars()

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
}