plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":kotlin-annotations-jvm"))
    api(project(":core:descriptors"))
    api(project(":core:deserialization"))
    api(project(":compiler:util"))
    api(project(":compiler:config"))
    api(project(":compiler:container"))
    api(project(":compiler:resolution"))
    api(project(":compiler:psi"))
    api(project(":compiler:frontend.common"))
    api(project(":compiler:frontend.common-psi"))
    api(project(":kotlin-script-runtime"))
    api(commonDependency("io.javaslang","javaslang"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("trove4j", "guava", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}