plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Common klib reader and writer"

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-util-io"))
    embedded(project(":core:metadata"))
    testImplementation(libs.junit4)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

configureKotlinCompileTasksGradleCompatibility()

publish()

standardPublicJars()