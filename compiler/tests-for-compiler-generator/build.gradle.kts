plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntimeOnly(intellijDep()) // Should come before compiler, because of "progarded" stuff needed for tests
    testImplementation(kotlinStdlib())

    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler"))
    testImplementation(projectTests(":compiler:fir:raw-fir:psi2fir"))
    testImplementation(projectTests(":compiler:fir:raw-fir:light-tree2fir"))
    testImplementation(projectTests(":compiler:fir:fir2ir"))
    testImplementation(projectTests(":compiler:fir:analysis-tests"))
    testImplementation(projectTests(":compiler:visualizer"))
    testImplementation(projectTests(":generators:test-generator"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

val generateTests by generator("org.jetbrains.kotlin.test.generators.GenerateCompilerTestsKt") {
    dependsOn(":compiler:generateTestData")
}

testsJar()
