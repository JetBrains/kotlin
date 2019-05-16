buildscript {
    val cacheRedirectorEnabled: Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

    repositories {
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
        }
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$embeddedKotlinVersion")
    }
}

group = "org.jetbrains.kotlin.ultimate"
version = "1.0"

plugins {
    kotlin("jvm") version embeddedKotlinVersion
    `kotlin-dsl-base`
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

rootProject.apply {
    from(rootProject.file("../versions.gradle.kts"))
}

val cacheRedirectorEnabled: Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

repositories {
    if (cacheRedirectorEnabled) {
        maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
    }
    jcenter()
}

// When running in multi-project build ("kotlin" + "kotlin-ultimate"), this buildSrc project is treated as a Gradle included build.
// Gradle will run "jar" task for the included build to produce the necessary classes that should be added to multi-project's build classpath.
// Adding a dependency of this "jar" task on certain "prepare-deps" sub-projects will allow downloading artifacts. And this will work both
// when running in both multi-project build and standalone build.
tasks["jar"].dependsOn(":prepare-deps:platform-deps:build") // "platform-deps" are required in any case

// When running in standalone build, the "build" Gradle task will be executed. This will cause executing "build" task for all sub-projects,
// and downloading ALL artifacts.
// tasks["build"].dependsOn(":prepare-deps:cidr:build") // "cidr" is required only for standalone build
// tasks["build"].dependsOn(":prepare-deps:idea-plugin:build") // "idea-plugin" is required only for standalone build
