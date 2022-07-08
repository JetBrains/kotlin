plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:metadata.jvm"))
    api(project(":core:deserialization.common"))
    api(project(":core:deserialization.common.jvm"))
    implementation(project(":core:compiler.common.jvm"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))

    implementation(project(":core:descriptors.jvm"))
    api(project(":compiler:psi"))

}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
