
buildscript {
    val buildSrcKotlinVersion: String by extra(findProperty("buildSrc.kotlin.version")?.toString() ?: embeddedKotlinVersion)
    val buildSrcKotlinRepo: String? by extra(findProperty("buildSrc.kotlin.repo") as String?)
    extra["versions.shadow"] = "2.0.1"
    extra["versions.intellij-plugin"] = "0.3.0-SNAPSHOT"

    repositories {
        buildSrcKotlinRepo?.let {
            maven(url = it)
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$buildSrcKotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:$buildSrcKotlinVersion")
    }
}

logger.info("buildSrcKotlinVersion: " + extra["buildSrcKotlinVersion"])
logger.info("buildSrc kotlin compiler version: " + org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
logger.info("buildSrc stdlib version: " + KotlinVersion.CURRENT)

apply {
    plugin("kotlin")
    plugin("kotlin-sam-with-receiver")
}

plugins {
    `kotlin-dsl`
}

repositories {
    extra["buildSrcKotlinRepo"]?.let {
        maven(url = it)
    }
    maven(url = "https://dl.bintray.com/kotlin/kotlin-dev") // for dex-method-list
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") // for intellij plugin
    maven(url = "http://dl.bintray.com/jetbrains/intellij-plugin-service") // for intellij plugin
    jcenter()
}

dependencies {
    compile(files("../dependencies/native-platform-uberjar.jar"))
    compile("com.jakewharton.dex:dex-method-list:2.0.0-alpha")
//    compile("net.rubygrapefruit:native-platform:0.14")
    // TODO: adding the dep to the plugin breaks the build unexpectedly, resolve and uncomment
//    compile("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["bootstrap_kotlin_version"]}")
    compile("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    compile("org.ow2.asm:asm-all:6.0_BETA")
    compile("org.jetbrains.intellij.plugins:gradle-intellij-plugin:${property("versions.intellij-plugin")}")
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.`samWithReceiver`(configure: org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension.() -> Unit): Unit =
        extensions.configure("samWithReceiver", configure)
