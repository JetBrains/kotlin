
buildscript {
    val buildSrcKotlinVersion: String by extra(findProperty("buildSrc.kotlin.version")?.toString() ?: embeddedKotlinVersion)
    extra["buildSrcKotlinRepo"] = findProperty("buildSrc.kotlin.repo")
    extra["versions.shadow"] = "2.0.1"

    repositories {
        extra["buildSrcKotlinRepo"]?.let {
            maven { setUrl(it) }
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
        maven { setUrl(it) }
    }
//    maven { setUrl("https://repo.gradle.org/gradle/libs-releases-local") }
    jcenter()
}

dependencies {
    compile(files("../dependencies/native-platform-uberjar.jar"))
//    compile("net.rubygrapefruit:native-platform:0.14")
    // TODO: adding the dep to the plugin breaks the build unexpectedly, resolve and uncomment
//    compile("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["bootstrap_kotlin_version"]}")
    compile("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.`samWithReceiver`(configure: org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension.() -> Unit): Unit =
        extensions.configure("samWithReceiver", configure)
