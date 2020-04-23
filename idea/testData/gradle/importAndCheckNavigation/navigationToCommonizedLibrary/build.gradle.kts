buildscript {
    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-dev") // TODO: use the Gradle plugin from the current build
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0-dev-7568") // TODO: use the Gradle plugin from the current build
    }
}

plugins {
    kotlin("multiplatform")
}

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-dev") // TODO: use the Gradle plugin from the current build
    mavenCentral()
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
