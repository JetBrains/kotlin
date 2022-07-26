plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(kotlinStdlib())
    testApi(intellijCore())
    testApiJUnit5()

    testApi(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(project(":analysis:analysis-internal-utils"))
    testApi(project(":compiler:psi"))
    testApi(project(":analysis:kt-references"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(project(":analysis:analysis-api-providers"))
    testApi(project(":analysis:analysis-api"))
    testApi(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    testApi(project(":analysis:analysis-api-impl-barebone"))
    testApi(project(":analysis:analysis-api-impl-base"))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":plugins:fir-plugin-prototype:plugin-annotations:jar")
    workingDir = rootDir
    useJUnitPlatform()
}

testsJar()


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}