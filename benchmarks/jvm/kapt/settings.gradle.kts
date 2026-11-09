rootProject.name = "kapt-benchmark"

pluginManagement {
    val kotlinVersion: String = settings.providers.gradleProperty("kotlin.version").get()
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("kapt") version kotlinVersion
    }
    repositories {
        // mavenLocal() first: the Kotlin plugin snapshot lives only in ~/.m2.
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

buildscript {
    // The Kotlin plugin is a -SNAPSHOT from Maven Local. Without this, Gradle would
    // cache the resolved snapshot for 24h and miss a republished local build.
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // mavenLocal() first: kotlin-stdlib and the kapt runtime artifacts for the
        // snapshot version are only available locally.
        mavenLocal()
        mavenCentral()
    }
}

include(
    ":source-generator",
    ":synthetic-processor",
    ":samples:synthetic-noop",
    ":samples:dagger-sample",
    ":samples:mapstruct-sample",
    ":samples:compiler-corpus",
)
