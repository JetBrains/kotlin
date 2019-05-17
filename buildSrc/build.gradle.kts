// NOTE: This buildfile file is completely ignored when running composite build `kotlin` + `kotlin-ultimate`.

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
