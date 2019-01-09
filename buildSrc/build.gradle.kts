buildscript {
    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

    repositories {
        if (cacheRedirectorEnabled) { maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com") }
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
    `kotlin-dsl`
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

repositories {
    if (cacheRedirectorEnabled) { maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com") }
    jcenter()
}
