plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:psi"))
    api(project(":core:deserialization.common"))
    api(project(":core:deserialization.common.jvm"))
    api(project(":core:deserialization"))
    implementation(project(":core:compiler.common.jvm"))
    testImplementation(projectTests(":compiler:tests-common-new"))

    api(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}


