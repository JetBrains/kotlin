plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":core:compiler.common"))
    implementation(project(":compiler:light-classes"))
    implementation(project(":analysis:analysis-api-providers"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:project-structure"))
    implementation(project(":analysis:decompiled:light-classes-for-decompiled"))
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
