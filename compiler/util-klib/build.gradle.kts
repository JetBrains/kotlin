plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("java-test-fixtures")
}

description = "Common klib reader and writer"

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    api("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
    api(project(":kotlin-util-io"))

    compileOnly(project(":core:metadata")) { exclude("org.jetbrains.kotlin", "kotlin-stdlib") }

    embedded(project(":core:metadata")) { isTransitive = false }

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
    testFixturesApi(libs.junit.jupiter.api)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

configureKotlinCompileTasksGradleCompatibility()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}

publish()

standardPublicJars()
