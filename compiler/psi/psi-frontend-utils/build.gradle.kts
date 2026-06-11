plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    implementation(project(":core:compiler.common"))
    implementation(project(":compiler:frontend.common"))

    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(libs.intellij.fastutil)

    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-impl"))

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:psi:psi-api")))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

projectTests {
    testCodebaseTask()
}
