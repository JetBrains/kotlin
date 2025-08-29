import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
    id("project-tests-convention")
    id("test-inputs-check")
    // kotlinx-rpc implementation in remote-daemon module uses object that needs to be marked as @Serializable
    // TODO: double check if this is really necessary
    kotlin("plugin.serialization")
}

dependencies {
    api(kotlinStdlib())
    api(project(":compiler:compiler.version"))
    api(project(":core:util.runtime"))

    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:log4j"))
    compileOnly(libs.intellij.asm)
    compileOnly(jpsModel()) { isTransitive = false }
    compileOnly(jpsModelImpl()) { isTransitive = false }

    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(intellijCore())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit4)
    // kotlinx-rpc implementation in remote-daemon module uses object that needs to be marked as @Serializable
    // TODO: double check if this is really necessary
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
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

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit4)
}
