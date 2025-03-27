plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generated-sources")
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
    runtimeOnly("com.intellij.platform:kotlinx-coroutines-core-jvm:1.8.0-intellij-13")
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

projectTest {
    workingDir = rootDir
}

generatedConfigurationKeys("CLIConfigurationKeys")
