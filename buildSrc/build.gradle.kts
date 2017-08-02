
buildscript {
    extra["kotlin_version"] = file("../kotlin-version-for-gradle.txt").readText().trim()
    extra["kotlin_gradle_plugin_version"] = extra["kotlin_version"]
    extra["repos"] = listOf(
            "https://dl.bintray.com/kotlin/kotlin-dev",
            "https://repo.gradle.org/gradle/repo",
            "https://plugins.gradle.org/m2",
            "http://repository.jetbrains.com/utils/")

    repositories {
        for (repo in (rootProject.extra["repos"] as List<String>)) {
            maven { setUrl(repo) }
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlin_version"]}")
        classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:${rootProject.extra["kotlin_version"]}")
    }
}

apply {
    plugin("kotlin")
    plugin("kotlin-sam-with-receiver")
}

plugins {
    `kotlin-dsl`
}

repositories {
    for (repo in (rootProject.extra["repos"] as List<String>)) {
        maven { setUrl(repo) }
    }
}

dependencies {
    // TODO: adding the dep to the plugin breaks the build unexpectedly, resolve and uncomment
//    compile("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlin_version"]}")
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.`samWithReceiver`(configure: org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension.() -> Unit): Unit =
        extensions.configure("samWithReceiver", configure)
