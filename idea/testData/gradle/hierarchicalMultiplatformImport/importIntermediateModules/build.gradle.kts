plugins {
    // TODO parametrization
    kotlin("multiplatform").version("1.3.50")
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
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmAndJsMain by creating {
            dependsOn(commonMain)
        }

        val jvmAndJsTest by creating {
            dependsOn(commonTest)
        }

        val linuxAndJsMain by creating {
            dependsOn(commonMain)
        }
        
        val linuxAndJsTest by creating {
            dependsOn(commonTest)
        }

        jvm().compilations["main"].defaultSourceSet {
            dependsOn(jvmAndJsMain)
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }

        jvm().compilations["test"].defaultSourceSet {
            dependsOn(jvmAndJsTest)
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        js().compilations["main"].defaultSourceSet {
            dependsOn(jvmAndJsMain)
            dependsOn(linuxAndJsMain)
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        js().compilations["test"].defaultSourceSet {
            dependsOn(jvmAndJsTest)
            dependsOn(linuxAndJsTest)
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        linuxX64().compilations["main"].defaultSourceSet {
            dependsOn(linuxAndJsMain)
        }

        linuxX64().compilations["test"].defaultSourceSet {
            dependsOn(linuxAndJsTest)
        }
    }
}