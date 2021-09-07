plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:config"))
    api(project(":core:descriptors.jvm"))
    compileOnly("org.jetbrains.intellij.deps:asm-all:9.1")
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
