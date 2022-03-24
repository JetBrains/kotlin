plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(intellijCore())
    implementation(kotlinStdlib())
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:cli"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-providers"))
    api(project(":analysis:project-structure"))
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
