
buildscript {
    val buildSrcKotlinVersion: String by extra(findProperty("buildSrc.kotlin.version")?.toString() ?: embeddedKotlinVersion)
    val buildSrcKotlinRepo: String? by extra(findProperty("buildSrc.kotlin.repo") as String?)
    extra["versions.shadow"] = "2.0.2"
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
    `java-gradle-plugin`
}

gradlePlugin {
    (plugins) {
        "pill-configurable" {
            id = "pill-configurable"
            implementationClass = "org.jetbrains.kotlin.pill.PillConfigurablePlugin"
        }
        "jps-compatible" {
            id = "jps-compatible"
            implementationClass = "org.jetbrains.kotlin.pill.JpsCompatiblePlugin"
        }
    }
}

fun Project.getBooleanProperty(name: String): Boolean? = this.findProperty(name)?.let {
    val v = it.toString()
    if (v.isBlank()) true
    else v.toBoolean()
}

rootProject.apply {
    from(rootProject.file("../versions.gradle.kts"))
}

val isTeamcityBuild = project.hasProperty("teamcity") || System.getenv("TEAMCITY_VERSION") != null
val intellijUltimateEnabled by extra(project.getBooleanProperty("intellijUltimateEnabled") ?: isTeamcityBuild)
val intellijSeparateSdks by extra(project.getBooleanProperty("intellijSeparateSdks") ?: false)

extra["intellijRepo"] = "https://www.jetbrains.com/intellij-repository"

extra["intellijReleaseType"] = if (extra["versions.intellijSdk"]?.toString()?.endsWith("SNAPSHOT") == true)
    "snapshots"
else
    "releases"

extra["versions.androidDxSources"] = "5.0.0_r2"

extra["customDepsOrg"] = "kotlin.build.custom.deps"

repositories {
    extra["buildSrcKotlinRepo"]?.let {
        maven(url = it)
    }
    jcenter()
}

dependencies {
    compile("net.rubygrapefruit:native-platform:${property("versions.native-platform")}")
    compile("net.rubygrapefruit:native-platform-windows-amd64:${property("versions.native-platform")}")
    compile("net.rubygrapefruit:native-platform-windows-i386:${property("versions.native-platform")}")
    compile("com.jakewharton.dex:dex-method-list:3.0.0")
    // TODO: adding the dep to the plugin breaks the build unexpectedly, resolve and uncomment
//    compile("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["bootstrap_kotlin_version"]}")
    // Shadow plugin is used in many projects of the main build. Once it's no longer used in buildSrc, please move this dependency to the root project
    compile("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    compile("org.ow2.asm:asm-all:6.0_BETA")
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.`samWithReceiver`(configure: org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension.() -> Unit): Unit =
        extensions.configure("samWithReceiver", configure)

tasks["build"].dependsOn(":prepare-deps:android-dx:build", ":prepare-deps:intellij-sdk:build")
