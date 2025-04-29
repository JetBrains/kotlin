import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

repositories {
    maven {
        name = "IntellijDependencies"
        url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlinStdlib())
                implementation(libs.org.jetbrains.syntax.api)
            }
            kotlin {
                srcDir("common/src")
            }
        }
        val jvmMain by getting {
            kotlin {
                srcDir("jvm/src")
            }
        }
    }
}
