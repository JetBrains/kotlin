plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generated-sources")
    id("project-tests-convention")
}

dependencies {
    api(project(":compiler:cli-common"))
    api(project(":compiler:resolution.common"))
    api(project(":compiler:frontend:cfg"))
    implementation(project(":compiler:backend.jvm"))
    api(project(":compiler:light-classes"))
    api(project(":compiler:javac-wrapper"))
    implementation(project(":kotlin-util-klib-metadata"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
    compileOnly(libs.intellij.asm)
    runtimeOnly(libs.kotlinx.coroutines.core)
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" { none() }
}

optInToExperimentalCompilerApi()
optInToK1Deprecation()

testsJar {}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit4) {
        workingDir = rootDir
    }
}

generatedConfigurationKeys("CLIConfigurationKeys")
