
buildscript {
    val buildSrcKotlinVersion: String by extra(findProperty("buildSrc.kotlin.version")?.toString() ?: embeddedKotlinVersion)
    val buildSrcKotlinRepo: String? by extra(findProperty("buildSrc.kotlin.repo") as String?)
    extra["versions.shadow"] = "2.0.1"
    extra["versions.intellij-plugin"] = "0.3.0-SNAPSHOT"
    extra["versions.native-platform"] = "0.14"

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

fun Project.getBooleanProperty(name: String): Boolean? = this.findProperty("intellijUltimateEnabled")?.let {
    val v = it.toString()
    if (v.isBlank()) true
    else v.toBoolean()
}

val intellijUltimateEnabled = project.getBooleanProperty("intellijUltimateEnabled")
                              ?: project.hasProperty("teamcity")
                              || System.getenv("TEAMCITY_VERSION") != null
val intellijSeparateSdks = project.getBooleanProperty("intellijSeparateSdks") ?: false
extra["intellijUltimateEnabled"] = intellijUltimateEnabled
extra["intellijSeparateSdks"] = intellijSeparateSdks
extra["intellijRepo"] = "https://www.jetbrains.com/intellij-repository"
extra["intellijReleaseType"] = "releases" // or "snapshots"
extra["versions.intellijSdk"] = "172.4343.14"
extra["versions.androidBuildTools"] = "r23.0.1"
extra["versions.androidDxSources"] = "5.0.0_r2"
extra["versions.idea.NodeJS"] = "172.3757.32"

extra["customDepsRepo"] = "$rootDir/repo"
extra["customDepsOrg"] = "kotlin.build.custom.deps"

repositories {
    extra["buildSrcKotlinRepo"]?.let {
        maven(url = it)
    }
    maven(url = "https://dl.bintray.com/kotlin/kotlin-dev") // for dex-method-list
    maven(url = "https://repo.gradle.org/gradle/libs-releases-local") // for native-platform
    jcenter()
}

dependencies {
    compile("net.rubygrapefruit:native-platform:${property("versions.native-platform")}")
    compile("net.rubygrapefruit:native-platform-windows-amd64:${property("versions.native-platform")}")
    compile("net.rubygrapefruit:native-platform-windows-i386:${property("versions.native-platform")}")
    compile("com.jakewharton.dex:dex-method-list:2.0.0-alpha")
    // TODO: adding the dep to the plugin breaks the build unexpectedly, resolve and uncomment
//    compile("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["bootstrap_kotlin_version"]}")
    compile("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    compile("org.ow2.asm:asm-all:6.0_BETA")
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.`samWithReceiver`(configure: org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension.() -> Unit): Unit =
        extensions.configure("samWithReceiver", configure)

tasks["build"].dependsOn(":prepare-deps:android-dx:build", ":prepare-deps:intellij-sdk:build")
