plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    implementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

configureKotlinCompileTasksGradleCompatibility()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}

publish()

standardPublicJars()
