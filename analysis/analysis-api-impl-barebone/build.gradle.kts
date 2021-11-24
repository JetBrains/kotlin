plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:psi"))
    api(project(":analysis:analysis-api-providers"))
    api(project(":analysis:project-structure"))
    api(intellijCore())

    testApiJUnit5()
    testApi(project(":kotlin-test:kotlin-test-junit"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:tests-common-new"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()