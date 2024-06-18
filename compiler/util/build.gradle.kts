import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(kotlinStdlib())
    api(project(":compiler:compiler.version"))
    api(project(":core:util.runtime"))

    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:log4j"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    compileOnly(jpsModel()) { isTransitive = false }
    compileOnly(jpsModelImpl()) { isTransitive = false }

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(intellijCore())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit4)
}

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir(File(rootDir, "resources"))
    }
    "test" {
        projectDefault()
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}

testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
}