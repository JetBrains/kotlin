plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:psi:psi-api"))
    api(project(":compiler:psi:psi-impl"))
    api(project(":core:deserialization.common"))
    api(project(":core:deserialization.common.jvm"))
    api(project(":core:deserialization"))
    implementation(project(":core:compiler.common.jvm"))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))

    api(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}


