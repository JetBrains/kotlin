buildscript {
    repositories {
        {{kts_kotlin_plugin_repositories}}
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:{{kotlin_plugin_version}}")
    }
}

plugins {
    kotlin("multiplatform").version("{{kotlin_plugin_version}}")
}

repositories {
    {{kts_kotlin_plugin_repositories}}
}

kotlin {
    val commonMain by sourceSets.getting {
        dependencies {
            implementation(kotlin("stdlib-common"))
            implementation(kotlin("stdlib"))
        }
    }

    val commonTest by sourceSets.getting {
        dependencies {
            implementation(kotlin("test-common"))
            implementation(kotlin("test-annotations-common"))
        }
    }

    // there is no Linux HMPP shortcut preset, so need to configure targets and common source sets manually
    val linuxMain by sourceSets.creating { dependsOn(commonMain) }
    val linuxTest by sourceSets.creating { dependsOn(commonTest) }

    linuxX64 {
        compilations["main"].defaultSourceSet.dependsOn(linuxMain)
        compilations["test"].defaultSourceSet.dependsOn(linuxTest)
    }

    linuxArm64 {
        compilations["main"].defaultSourceSet.dependsOn(linuxMain)
        compilations["test"].defaultSourceSet.dependsOn(linuxTest)
    }
}
