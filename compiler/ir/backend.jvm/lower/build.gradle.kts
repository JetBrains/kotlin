plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:backend.jvm"))
    implementation(project(":compiler:ir.tree.impl"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
