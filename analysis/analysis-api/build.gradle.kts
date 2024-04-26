import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

kotlin {
    explicitApiWarning()
}

dependencies {
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    compileOnly(project(":compiler:psi"))
    implementation(project(":compiler:backend"))
    compileOnly(project(":core:compiler.common"))
    compileOnly(project(":core:compiler.common.jvm"))
    compileOnly(project(":core:compiler.common.js"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:kt-references"))
    api(project(":analysis:project-structure"))

    api(intellijCore())
    api(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    api(libs.guava)
}

kotlin {
    explicitApi()
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")
}

testsJar()

projectTest {
    workingDir = rootDir
}
