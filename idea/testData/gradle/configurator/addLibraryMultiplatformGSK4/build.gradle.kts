plugins {
    kotlin("multiplatform") version "1.3.41"
}

repositories {
    mavenCentral()
}

group = "com.example"
version = "0.0.1"

kotlin {
    sourceSets {
        jvm()
        val commonMain by getting {
            dependencies { 
                implementation(kotlin("reflect"))
            }
        }
        val jvmMain by getting
    }
}