description = "Compiler runner + daemon client unshaded"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":kotlin-daemon-client"))
    implementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-preloader"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":daemon-common"))
    compileOnly(project(":daemon-common-new"))
    compileOnly(project(":compiler:util"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
