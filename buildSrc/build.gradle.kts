
buildscript {
    // TODO: find a way to reuse bootstrap.kotlin.* props from the main project
//    java.util.Properties().also { it.load(java.io.FileInputStream("gradle.properties")) }
    extra["bootstrap_kotlin_version"] = System.getProperty("bootstrap.kotlin.version") ?: embeddedKotlinVersion

    repositories {
        System.getProperty("bootstrap.kotlin.repo")?.let {
            maven { setUrl(it) }
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["bootstrap_kotlin_version"]}")
        classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:${rootProject.extra["bootstrap_kotlin_version"]}")
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
    System.getProperty("bootstrap.kotlin.repo")?.let {
        maven { setUrl(it) }
    }
}

dependencies {
    // TODO: adding the dep to the plugin breaks the build unexpectedly, resolve and uncomment
//    compile("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["bootstrap_kotlin_version"]}")
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.`samWithReceiver`(configure: org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension.() -> Unit): Unit =
        extensions.configure("samWithReceiver", configure)
