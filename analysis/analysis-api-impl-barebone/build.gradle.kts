plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:psi"))
    api(project(":analysis:analysis-api-providers"))
    api(project(":analysis:project-structure"))
    api(intellijCore())

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:tests-common-new"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()