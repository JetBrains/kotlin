description = "Compiler runner + daemon client unshaded"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":kotlin-build-common"))
    api(project(":kotlin-daemon-client"))
    api(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-preloader"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":daemon-common"))
    compileOnly(project(":daemon-common-new"))
    compileOnly(project(":compiler:util"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
