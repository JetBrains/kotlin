plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":compiler:fir:entrypoint"))
    testApi(project(":compiler:cli"))
    testApi(intellijCoreDep()) { includeJars("intellij-core") }

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))

    testImplementation(projectTests(":compiler:test-infrastructure-utils"))

    testRuntimeOnly(intellijDep()) {
        includeJars("jna", rootProject = rootProject)
    }
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

testsJar()
