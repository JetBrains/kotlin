plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check")
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    implementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

configureKotlinCompileTasksGradleCompatibility()

projectTests {
    testTask()
}

publish()

standardPublicJars()
