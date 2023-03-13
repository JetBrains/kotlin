plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(intellijCore())
    implementation(kotlinStdlib())
    implementation(project(":compiler:psi"))
    api(project(":compiler:cli-base"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-providers"))
    api(project(":analysis:project-structure"))
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
