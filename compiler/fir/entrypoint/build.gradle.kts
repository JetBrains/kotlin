plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:descriptors.jvm"))
    api(project(":compiler:frontend.java"))
    api(project(":compiler:fir:java"))

    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:checkers"))

    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
