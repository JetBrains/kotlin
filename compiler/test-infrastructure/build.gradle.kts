plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":compiler:fir:entrypoint"))
    testApi(project(":compiler:cli"))
    testApi(intellijCore())

    testRuntimeOnly(project(":core:descriptors.runtime"))

    testImplementation(projectTests(":compiler:test-infrastructure-utils"))
    testImplementation(project(":compiler:fir:cones"))
    testImplementation(project(":compiler:config.jvm"))
    testImplementation(project(":compiler:plugin-api"))
    testImplementation(project(":js:js.config"))
    testImplementation(project(":kotlin-util-klib"))

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

testsJar()
