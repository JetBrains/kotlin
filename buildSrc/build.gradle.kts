import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

extra["versions.native-platform"] = "0.14"

buildscript {

    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

    extra["defaultSnapshotVersion"] = kotlinBuildProperties.defaultSnapshotVersion
    kotlinBootstrapFrom(BootstrapOption.SpaceBootstrap(kotlinBuildProperties.kotlinBootstrapVersion!!, cacheRedirectorEnabled))

    repositories {
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
            maven("https://cache-redirector.jetbrains.com/kotlin.bintray.com/kotlin-dependencies")
        } else {
            jcenter()
            maven("https://kotlin.bintray.com/kotlin-dependencies")
        }

        project.bootstrapKotlinRepo?.let {
            maven(url = it)
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.21")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
        classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:${project.bootstrapKotlinVersion}")
    }
}

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

logger.info("buildSrcKotlinVersion: " + extra["bootstrapKotlinVersion"])
logger.info("buildSrc kotlin compiler version: " + org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
logger.info("buildSrc stdlib version: " + KotlinVersion.CURRENT)

apply {
    plugin("kotlin")
    plugin("kotlin-sam-with-receiver")

    from("../gradle/checkCacheability.gradle.kts")
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        register("jps-compatible") {
            id = "jps-compatible"
            implementationClass = "org.jetbrains.kotlin.pill.JpsCompatiblePlugin"
        }
    }
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

fun Project.getBooleanProperty(name: String): Boolean? = this.findProperty(name)?.let {
    val v = it.toString()
    if (v.isBlank()) true
    else v.toBoolean()
}

rootProject.apply {
    from(rootProject.file("../gradle/versions.gradle.kts"))
}

val isTeamcityBuild = kotlinBuildProperties.isTeamcityBuild
val intellijUltimateEnabled by extra(kotlinBuildProperties.intellijUltimateEnabled)
val intellijSeparateSdks by extra(project.getBooleanProperty("intellijSeparateSdks") ?: false)

extra["intellijReleaseType"] = when {
    extra["versions.intellijSdk"]?.toString()?.contains("-EAP-") == true -> "snapshots"
    extra["versions.intellijSdk"]?.toString()?.endsWith("SNAPSHOT") == true -> "nightly"
    else -> "releases"
}

extra["versions.androidDxSources"] = "5.0.0_r2"

extra["customDepsOrg"] = "kotlin.build"

repositories {
    jcenter()
    maven("https://jetbrains.bintray.com/intellij-third-party-dependencies/")
    maven("https://kotlin.bintray.com/kotlin-dependencies")
    gradlePluginPortal()

    extra["bootstrapKotlinRepo"]?.let {
        maven(url = it)
    }
}

dependencies {
    implementation(kotlin("stdlib", embeddedKotlinVersion))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.21")
    implementation("com.gradle.publish:plugin-publish-plugin:0.12.0")

    implementation("net.rubygrapefruit:native-platform:${property("versions.native-platform")}")
    implementation("net.rubygrapefruit:native-platform-windows-amd64:${property("versions.native-platform")}")
    implementation("net.rubygrapefruit:native-platform-windows-i386:${property("versions.native-platform")}")
    implementation("com.jakewharton.dex:dex-method-list:3.0.0")

    implementation("com.github.jengelman.gradle.plugins:shadow:${rootProject.extra["versions.shadow"]}")
    implementation("net.sf.proguard:proguard-gradle:6.2.2")
    implementation("org.jetbrains.intellij.deps:asm-all:8.0.1")

    implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:0.5")

    implementation("org.gradle:test-retry-gradle-plugin:1.1.9")
    implementation("com.gradle.enterprise:test-distribution-gradle-plugin:1.2.1")
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

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs +=
        listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xskip-runtime-version-check")
}

tasks["build"].dependsOn(":prepare-deps:build")

allprojects {
    tasks.register("checkBuild")

    afterEvaluate {
        apply(from = "$rootDir/../gradle/cacheRedirector.gradle.kts")
    }
}
