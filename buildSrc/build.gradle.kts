import java.util.Properties

extra["versions.shadow"] = "4.0.3"
extra["versions.native-platform"] = "0.14"

buildscript {
    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

    val buildSrcKotlinVersion: String by extra(findProperty("buildSrc.kotlin.version")?.toString() ?: embeddedKotlinVersion)
    val buildSrcKotlinRepo: String? by extra(findProperty("buildSrc.kotlin.repo") as String?)

    repositories {
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
        } else {
            jcenter()
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

val flags = LocalBuildProperties(project)

val isTeamcityBuild = flags.isTeamcityBuild
val intellijUltimateEnabled by extra(flags.intellijUltimateEnabled)
val intellijSeparateSdks by extra(project.getBooleanProperty("intellijSeparateSdks") ?: false)
val verifyDependencyOutput by extra( getBooleanProperty("kotlin.build.dependency.output.verification") ?: isTeamcityBuild)

extra["intellijReleaseType"] = if (extra["versions.intellijSdk"]?.toString()?.endsWith("SNAPSHOT") == true)
    "snapshots"
else
    "releases"

extra["versions.androidDxSources"] = "5.0.0_r2"

extra["customDepsOrg"] = "kotlin.build"

repositories {
    jcenter()
    maven("https://jetbrains.bintray.com/intellij-third-party-dependencies/")
    maven("https://kotlin.bintray.com/kotlin-dependencies")
    gradlePluginPortal()

    extra["buildSrcKotlinRepo"]?.let {
        maven(url = it)
    }
}

dependencies {
    compile(kotlin("stdlib", embeddedKotlinVersion))
    compile("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.1")

    compile("net.rubygrapefruit:native-platform:${property("versions.native-platform")}")
    compile("net.rubygrapefruit:native-platform-windows-amd64:${property("versions.native-platform")}")
    compile("net.rubygrapefruit:native-platform-windows-i386:${property("versions.native-platform")}")
    compile("com.jakewharton.dex:dex-method-list:3.0.0")

    compile("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    compile("org.jetbrains.intellij.deps:asm-all:7.0.1")

    compile("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:0.5")
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.`samWithReceiver`(configure: org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension.() -> Unit): Unit =
        extensions.configure("samWithReceiver", configure)

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks["build"].dependsOn(":prepare-deps:build")

allprojects {
    tasks.register("checkBuild")

    afterEvaluate {
        apply(from = "$rootDir/../gradle/cacheRedirector.gradle.kts")
    }
}

// TODO: hide these classes in special gradle plugin for kotlin-ultimate which will support local.properties
class LocalBuildPropertiesProvider(private val project: Project) {
    private val localProperties: Properties = Properties()

    val rootProjectDir: File = project.rootProject.rootDir.parentFile

    init {
        rootProjectDir.resolve("local.properties").takeIf { it.isFile }?.let {
            it.reader().use(localProperties::load)
        }
    }

    fun getString(name: String): String? = project.findProperty(name)?.toString() ?: localProperties[name]?.toString()

    fun getBoolean(name: String): Boolean = getString(name)?.toBoolean() == true
}

class LocalBuildProperties(project: Project) {
    val propertiesProvider = LocalBuildPropertiesProvider(project)

    val isTeamcityBuild = propertiesProvider.getString("teamcity") != null || System.getenv("TEAMCITY_VERSION") != null

    val intellijUltimateEnabled =
        (propertiesProvider.getBoolean("intellijUltimateEnabled") || isTeamcityBuild)
                && propertiesProvider.rootProjectDir.resolve("kotlin-ultimate").exists()
}
