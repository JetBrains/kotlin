plugins {
    kotlin("multiplatform").version("{{kotlin_plugin_version}}")
}

repositories {
    maven("https://kotlin.bintray.com/kotlin-dev")
    jcenter()
}

group = "project"
version = "1.0"

kotlin {
    jvm() 
    js()
    linuxX64()

    sourceSets {
        val commonMain by getting {
        }

        val commonTest by getting {
        }

        val jvmAndJsMain by creating {
        }

        val jvmAndJsTest by creating {
        }

        val linuxAndJsMain by creating {
        }
        
        val linuxAndJsTest by creating {
        }

        jvm().compilations["main"].defaultSourceSet {
        }

        jvm().compilations["test"].defaultSourceSet {
        }

        js().compilations["main"].defaultSourceSet {
        }

        js().compilations["test"].defaultSourceSet {
        }

        linuxX64().compilations["main"].defaultSourceSet {
        }

        linuxX64().compilations["test"].defaultSourceSet {
        }
    }
}
