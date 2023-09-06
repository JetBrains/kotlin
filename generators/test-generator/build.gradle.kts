
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":core:util.runtime"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(kotlinStdlib())
    testImplementation(libs.junit4)
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(project(":generators"))

    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testRuntimeOnly(project(":core:descriptors.runtime"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
