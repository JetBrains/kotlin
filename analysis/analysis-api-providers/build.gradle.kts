plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":core:compiler.common"))
    implementation(project(":analysis:project-structure"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

kotlin {
    explicitApi()
}
