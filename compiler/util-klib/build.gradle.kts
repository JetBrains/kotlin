plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

description = "Common klib reader and writer"

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    api("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
    api(project(":kotlin-util-io"))

    compileOnly(project(":core:metadata")) { exclude("org.jetbrains.kotlin", "kotlin-stdlib") }

    embedded(project(":core:metadata")) { isTransitive = false }

    testImplementation(libs.junit4)

    testFixturesApi("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

configureKotlinCompileTasksGradleCompatibility()

publish()

standardPublicJars()