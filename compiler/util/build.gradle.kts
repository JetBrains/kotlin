import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
    id("project-tests-convention")
    id("test-inputs-check")
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
    testImplementation(platform(libs.junit.bom))
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

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit4)
}
