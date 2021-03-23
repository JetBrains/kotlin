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
                entryPoint = "sample.foo"
            }
        }
    }
    sourceSets {
        val macosMain by getting {}
    }
}