plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:frontend.common"))
    api(project(":compiler:frontend.common-psi"))
    api(project(":compiler:frontend.common.jvm"))
    api(project(":compiler:resolution.common.jvm"))
    api(project(":core:compiler.common.jvm"))
    implementation(project(":analysis:project-structure"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
}

sourceSets {
    "main" { projectDefault() }
}
