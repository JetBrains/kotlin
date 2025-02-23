plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:cli-common"))
    implementation(project(":compiler:resolution.common"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:frontend:cfg"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:backend.jvm"))
    implementation(project(":compiler:light-classes"))
    implementation(project(":compiler:javac-wrapper"))
    implementation(project(":native:frontend.native"))
    implementation(project(":wasm:wasm.frontend"))
    implementation(project(":kotlin-util-klib"))
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

allprojects {
    optInToExperimentalCompilerApi()
}

testsJar {}

projectTest {
    workingDir = rootDir
}

generatedConfigurationKeys("CLIConfigurationKeys")
