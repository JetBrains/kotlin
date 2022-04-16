plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":analysis:analysis-api-impl-base"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:kt-references"))

    implementation(project(":compiler:backend"))
    implementation(project(":compiler:backend.jvm"))
    implementation(project(":compiler:backend-common"))
    implementation(project(":compiler:backend.common.jvm"))

    testApiJUnit5()
    testImplementation(project(":analysis:analysis-api-providers"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:test-infrastructure-utils"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":analysis:analysis-api-impl-barebone"))
    testImplementation(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))

}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}

testsJar()
