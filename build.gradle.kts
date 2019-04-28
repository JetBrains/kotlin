import org.jetbrains.kotlin.ultimate.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    if (rootProject.findProject(":cidr-native") != null) { // only for standalone build:
        val cacheRedirectorEnabled: Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

        repositories {
            if (cacheRedirectorEnabled) { maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com") }
            jcenter()
        }

        dependencies {
            classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$embeddedKotlinVersion")
        }
    }
}

if (isStandaloneBuild) { // only for standalone build:
    val cacheRedirectorEnabled: Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

    allprojects {
        configurations.maybeCreate("embedded")

        repositories {
            if (cacheRedirectorEnabled) { maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com") }
            jcenter()
        }

        tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }
    }
}

plugins {
    base
}

rootProject.apply {
    from(ultimateProject(":").file("versions.gradle.kts"))
}

tasks["clean"].doLast { delete("dist") }
