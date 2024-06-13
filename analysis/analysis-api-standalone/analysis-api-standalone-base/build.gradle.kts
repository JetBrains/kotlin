plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(intellijCore())
    implementation(kotlinStdlib())
    implementation(project(":compiler:psi"))
    implementation(project(":analysis:analysis-api-impl-base"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":analysis:decompiled:decompiler-native"))
    api(project(":compiler:cli-base"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    api(project(":analysis:analysis-api-platform-interface"))
    api(project(":analysis:project-structure"))
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
