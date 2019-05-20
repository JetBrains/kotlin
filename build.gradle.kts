import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val includeKotlinUltimate: Boolean = findProperty("includeKotlinUltimate")?.toString()?.toBoolean() == true
    val cacheRedirectorEnabled: Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

    if (!includeKotlinUltimate) { // only for standalone build:
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
}

val includeKotlinUltimate: Boolean = findProperty("includeKotlinUltimate")?.toString()?.toBoolean() == true
val cacheRedirectorEnabled: Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

if (!includeKotlinUltimate) { // only for standalone build:
    allprojects {
        configurations.maybeCreate("embedded")

        repositories {
            if (cacheRedirectorEnabled) {
                maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
            }
            jcenter()
        }

        tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }
    }
}

plugins {
    base
}

rootProject.apply {
    from(project.file("gradle/cidrPluginProperties.gradle.kts")) // this one must go the first
    from(project.file("gradle/ultimateTools.gradle.kts"))
    from(project.file("gradle/cidrPluginTools.gradle.kts"))
}

tasks["clean"].doLast { delete("dist") }
