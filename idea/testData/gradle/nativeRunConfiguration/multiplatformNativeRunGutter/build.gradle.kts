plugins {
    id("org.jetbrains.kotlin.multiplatform") version ("{{kotlin_plugin_version}}")
}
repositories {
    mavenLocal()
}

kotlin {
    macosX64("macos") {
        binaries {
            executable {
                entryPoint = "sample.main"
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val macosMain by getting {
        }
    }
}