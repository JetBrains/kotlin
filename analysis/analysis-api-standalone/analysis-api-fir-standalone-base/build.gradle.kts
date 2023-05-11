plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(intellijCore())
    implementation(kotlinStdlib())
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-impl-base"))
    implementation(project(":analysis:analysis-api-fir"))
    implementation(project(":analysis:symbol-light-classes"))
    implementation(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
