/**
 *         commonMain----
 *         /             \
 *     jvmAndLinuxMain    \
 *     /           \       \
 *  jvm          linuxX64  macosX64
 *
 *  (the tests structure is the same)
 */

buildscript {
    repositories {
        {{kts_kotlin_plugin_repositories}}
    }
}

repositories {
    {{kts_kotlin_plugin_repositories}}
}

plugins {
    kotlin("multiplatform").version("{{kotlin_plugin_version}}")
}

group = "project"
version = "1.0"

kotlin {
    jvm()
    linuxX64()
    macosX64()

    sourceSets {
        val commonMain by getting {
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmAndLinuxMain by creating {
            dependsOn(commonMain)
        }

        val jvmAndLinuxTest by creating {
            dependsOn(commonTest)
        }

        val jvmMain by getting {
            dependsOn(jvmAndLinuxMain)
        }

        val jvmTest by getting {
            dependsOn(jvmAndLinuxTest)
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        val linuxX64Main by getting {
            dependsOn(jvmAndLinuxMain)
        }

        val linuxX64Test by getting {
            dependsOn(jvmAndLinuxTest)
        }
    }
}