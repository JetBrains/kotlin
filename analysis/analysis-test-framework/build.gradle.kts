plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(kotlinStdlib())
    testApi(intellijCore())
    testApiJUnit5()

    testApi(project(":kotlin-test:kotlin-test-junit"))
    testApi(project(":compiler:psi"))
    testApi(project(":analysis:kt-references"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(project(":analysis:analysis-api-providers"))
    testApi(project(":analysis:analysis-api"))
    testApi(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    testApi(project(":analysis:analysis-api-impl-barebone"))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    dependsOn(":plugins:fir-plugin-prototype:plugin-annotations:jar")
    workingDir = rootDir
    useJUnitPlatform()
}

testsJar()

