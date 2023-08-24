plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":analysis:kt-references"))

    implementation(project(":compiler:psi"))
    implementation(project(":analysis:light-classes-base"))
    implementation(project(":compiler:frontend.java"))
    implementation(intellijCore())

    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
