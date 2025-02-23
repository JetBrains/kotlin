plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(project(":compiler:backend"))
    testImplementation(project(":compiler:backend-common"))
    testImplementation(project(":compiler:backend.common.jvm"))
    testImplementation(project(":compiler:fir:entrypoint"))
    testImplementation(project(":compiler:cli"))
    testImplementation(intellijCore())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    testRuntimeOnly(project(":core:descriptors.runtime"))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

testsJar()
