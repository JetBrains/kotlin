import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val isStandaloneBuild: Boolean = rootProject.findProject(":idea") == null
    val cacheRedirectorEnabled: Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

    if (isStandaloneBuild) { // only for standalone build:
        repositories {
            if (cacheRedirectorEnabled) {
                maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
            }
            maven("https://dl.bintray.com/kotlin/kotlin-bootstrap")
            jcenter()
        }

        dependencies {
            classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0-dev-5258")
        }
    }
}

val isStandaloneBuild: Boolean = rootProject.findProject(":idea") == null
val cacheRedirectorEnabled: Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true
val localMavenRepo: String? = findProperty("localMavenRepo")?.toString()

if (isStandaloneBuild) { // only for standalone build:
    allprojects {
        configurations.maybeCreate("embedded")

        repositories {
            if (cacheRedirectorEnabled) {
                maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
            }
            jcenter()
            maven("https://dl.bintray.com/kotlin/kotlin-bootstrap")
            localMavenRepo?.let { maven(it) }
        }

        tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }
    }
}

plugins {
    base
}

rootProject.apply {
    from(project.file("gradle/kotlinUltimateProperties.gradle.kts")) // this one must go the first
    from(project.file("gradle/kotlinUltimateTools.gradle.kts"))
    from(project.file("gradle/cidrPluginTools.gradle.kts"))
}

tasks["clean"].doLast { delete("dist") }
