plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":kotlin-annotations-jvm"))
    api(project(":compiler:util"))
    api(project(":compiler:backend-common"))
    api(project(":compiler:frontend"))
    api(project(":compiler:frontend.java"))
    api(project(":compiler:serialization"))
    api(project(":compiler:backend.common.jvm"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    compileOnly(commonDependency("com.google.guava:guava"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
