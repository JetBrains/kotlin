
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":core:util.runtime"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(kotlinStdlib())
    testApi(libs.junit4)
    testApiJUnit5()
    testApi(project(":generators"))

    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testRuntimeOnly(project(":core:descriptors.runtime"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
