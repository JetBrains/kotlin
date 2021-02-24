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
    js()

    sourceSets {
        val pseudoOrphan by creating {
        }

        val includedIntoJvm by creating {
            dependsOn(pseudoOrphan)
        }

        jvm().compilations["main"].source(includedIntoJvm)
    }
}