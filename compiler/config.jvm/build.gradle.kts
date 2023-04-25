plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:config"))
    api(project(":core:compiler.common.jvm"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
