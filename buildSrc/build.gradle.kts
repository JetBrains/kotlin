
buildscript {
    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

    val buildSrcKotlinVersion: String by extra(findProperty("buildSrc.kotlin.version")?.toString() ?: embeddedKotlinVersion)
    val buildSrcKotlinRepo: String? by extra(findProperty("buildSrc.kotlin.repo") as String?)
    extra["versions.shadow"] = "4.0.3"
    extra["versions.native-platform"] = "0.14"

    repositories {
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
        }

        buildSrcKotlinRepo?.let {
            maven(url = it)
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$buildSrcKotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:$buildSrcKotlinVersion")
    }
}

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

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
    plugins {
        register("pill-configurable") {
            id = "pill-configurable"
            implementationClass = "org.jetbrains.kotlin.pill.PillConfigurablePlugin"
        }
        register("jps-compatible") {
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
    from(rootProject.file("../gradle/versions.gradle.kts"))
}

val isTeamcityBuild = project.hasProperty("teamcity") || System.getenv("TEAMCITY_VERSION") != null
val intellijUltimateEnabled by extra(project.getBooleanProperty("intellijUltimateEnabled") ?: isTeamcityBuild)
val intellijSeparateSdks by extra(project.getBooleanProperty("intellijSeparateSdks") ?: false)
val verifyDependencyOutput by extra( getBooleanProperty("kotlin.build.dependency.output.verification") ?: isTeamcityBuild)

extra["intellijReleaseType"] = if (extra["versions.intellijSdk"]?.toString()?.endsWith("SNAPSHOT") == true)
    "snapshots"
else
    "releases"

extra["versions.androidDxSources"] = "5.0.0_r2"

extra["customDepsOrg"] = "kotlin.build"

repositories {
    if (cacheRedirectorEnabled) {
        maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
        maven("https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies/")
    }

    extra["buildSrcKotlinRepo"]?.let {
        maven(url = it)
    }

    jcenter()
    maven("https://jetbrains.bintray.com/intellij-third-party-dependencies/")
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    compile("net.rubygrapefruit:native-platform:${property("versions.native-platform")}")
    compile("net.rubygrapefruit:native-platform-windows-amd64:${property("versions.native-platform")}")
    compile("net.rubygrapefruit:native-platform-windows-i386:${property("versions.native-platform")}")
    compile("com.jakewharton.dex:dex-method-list:3.0.0")

    compile("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    compile("org.jetbrains.intellij.deps:asm-all:7.0")

    compile("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:0.4.2")
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.`samWithReceiver`(configure: org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension.() -> Unit): Unit =
        extensions.configure("samWithReceiver", configure)

tasks["build"].dependsOn(":prepare-deps:build")
