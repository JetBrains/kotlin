plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":analysis:light-classes-base"))
    api(project(":compiler:util"))
    api(project(":compiler:backend"))
    api(project(":compiler:frontend"))
    api(project(":compiler:frontend.java"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    compileOnly(commonDependency("com.google.guava:guava"))
}

sourceSets {
    "main" { projectDefault() }
}
