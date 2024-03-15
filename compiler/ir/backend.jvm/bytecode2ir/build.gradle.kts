plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:backend.jvm"))
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}

java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
}

tasks.withType<JavaCompile> {
    options.compilerArgs.minusAssign("-Werror")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>> {
    compilerOptions {
        allWarningsAsErrors = false
    }
}