plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(project(":compiler:fir:entrypoint"))
    testImplementation(project(":compiler:cli"))
    testImplementation(intellijCore())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    testRuntimeOnly(project(":core:descriptors.runtime"))

    // This dependency is needed only for FileComparisonFailure
    testImplementation(intellijJavaRt())
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

testsJar()
