plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":compiler:fir:entrypoint"))
    testApi(project(":compiler:cli"))
    testApi(intellijCore())

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))

    testImplementation(projectTests(":compiler:test-infrastructure-utils"))

    testRuntimeOnly(commonDependency("net.java.dev.jna:jna"))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

testsJar()
