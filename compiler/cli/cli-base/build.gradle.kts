plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:cli-common"))
    api(project(":compiler:resolution.common"))
    api(project(":compiler:frontend.java"))
    api(project(":compiler:frontend:cfg"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:backend.jvm"))
    api(project(":compiler:light-classes"))
    api(project(":compiler:javac-wrapper"))
    api(project(":compiler:ir.serialization.jvm"))
    api(project(":js:js.translator"))
    api(project(":native:frontend.native"))
    api(project(":wasm:wasm.frontend"))
    api(project(":kotlin-util-klib"))
    api(project(":kotlin-util-klib-metadata"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
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
