import org.jetbrains.kotlin.ultimate.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    if (rootProject.findProject(":cidr-native") != null) { // only for standalone build:
        val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

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
    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

    allprojects {
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

setupVersionProperties()

val appcodePlugin by cidrPlugin("AppCode", appcodePluginDir)
val zipAppCodePlugin by zipCidrPlugin("AppCode", appcodeVersion)

val clionPlugin by cidrPlugin("CLion", clionPluginDir)
val zipCLionPlugin by zipCidrPlugin("CLion", clionVersion)

tasks["clean"].doLast { delete("dist") }
