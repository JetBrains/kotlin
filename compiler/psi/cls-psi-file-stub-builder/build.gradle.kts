plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:psi"))
    api(project(":core:deserialization.common"))
    api(project(":core:deserialization.common.jvm"))
    api(project(":core:deserialization"))
    implementation(project(":compiler:psi:cls-psi-stub-builder"))
    implementation(project(":core:deserialization.common.jvm.impl"))
    implementation(project(":core:compiler.common.jvm"))

    api(intellijCore())

    testImplementation(projectTests(":compiler:tests-common"))

}

sourceSets {
    "main" { projectDefault() }
    "test" {  projectDefault() }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}


testsJar()