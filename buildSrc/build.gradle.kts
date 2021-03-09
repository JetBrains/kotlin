import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

extra["versions.native-platform"] = "0.14"

buildscript {

    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

    extra["defaultSnapshotVersion"] = kotlinBuildProperties.defaultSnapshotVersion
    kotlinBootstrapFrom(BootstrapOption.SpaceBootstrap(kotlinBuildProperties.kotlinBootstrapVersion!!, cacheRedirectorEnabled))

    repositories {
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
            maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        } else {
            jcenter()
            maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        }

        project.bootstrapKotlinRepo?.let {
            maven(url = it)
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.25")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
        classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:${project.bootstrapKotlinVersion}")
    }
}

logger.info("buildSrcKotlinVersion: " + extra["bootstrapKotlinVersion"])
logger.info("buildSrc kotlin compiler version: " + org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
logger.info("buildSrc stdlib version: " + KotlinVersion.CURRENT)

apply {
    plugin("kotlin")
    plugin("kotlin-sam-with-receiver")
    plugin("groovy")

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
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    maven("https://kotlin.bintray.com/kotlinx")
    gradlePluginPortal()

    extra["bootstrapKotlinRepo"]?.let {
        maven(url = it)
    }
}

val generateCompilerVersion by tasks.registering(VersionGenerator::class) {
    kotlinNativeVersionInResources=true
    defaultVersionFileLocation()
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn(generateCompilerVersion)
}

tasks.clean {
    doFirst {
        val versionSourceDirectory = project.konanVersionGeneratedSrc()
        if (versionSourceDirectory.exists()) {
            versionSourceDirectory.delete()
        }
    }
}

sourceSets["main"].withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
    kotlin.srcDir("src/main/kotlin")
    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        kotlin.srcDir("../kotlin-native/shared/src/library/kotlin")
        kotlin.srcDir("../kotlin-native/shared/src/main/kotlin")
        kotlin.srcDir("../kotlin-native/build-tools/src/main/kotlin")
        kotlin.srcDir("../kotlin-native/tools/kotlin-native-gradle-plugin/src/main/kotlin")
        kotlin.srcDir("../compiler/util-klib/src")
        kotlin.srcDir("../native/utils/src")
    }
    kotlin.srcDir(project.kotlinNativeVersionSrc())
    /**
     * TODO: mentioned bellow and Co it'd be better to move to :kotlin-native:performance:buildSrc,
     * because all this relates to benchmarking.
     */
    kotlin.exclude("**/benchmark/*.kt")
    kotlin.exclude("**/kotlin/MPPTools.kt")
    kotlin.exclude("**/kotlin/RegressionsReporter.kt")
    kotlin.exclude("**/kotlin/RunJvmTask.kt")
    kotlin.exclude("**/kotlin/RunKotlinNativeTask.kt")
    kotlin.exclude("**/kotlin/BuildRegister.kt")
    kotlin.exclude("**/kotlin/benchmarkUtils.kt")
}

tasks.validatePlugins.configure {
    enabled = false
}

java {
    disableAutoTargetJvm()
}

dependencies {
    implementation(kotlin("stdlib", embeddedKotlinVersion))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.25")
    implementation("com.gradle.publish:plugin-publish-plugin:0.12.0")

    implementation("net.rubygrapefruit:native-platform:${property("versions.native-platform")}")
    implementation("net.rubygrapefruit:native-platform-windows-amd64:${property("versions.native-platform")}")
    implementation("net.rubygrapefruit:native-platform-windows-i386:${property("versions.native-platform")}")
    implementation("com.jakewharton.dex:dex-method-list:3.0.0")

    implementation("com.github.jengelman.gradle.plugins:shadow:${rootProject.extra["versions.shadow"]}")
    implementation("net.sf.proguard:proguard-gradle:6.2.2")
    implementation("org.jetbrains.intellij.deps:asm-all:8.0.1")

    implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:0.5")

    implementation("org.gradle:test-retry-gradle-plugin:1.2.0")
    implementation("com.gradle.enterprise:test-distribution-gradle-plugin:1.2.1")

    compileOnly(gradleApi())

    val kotlinVersion = project.bootstrapKotlinVersion
    val ktorVersion  = "1.2.1"
    val slackApiVersion = "1.2.0"
    val metadataVersion = "0.0.1-dev-10"

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.ullink.slack:simpleslackapi:$slackApiVersion")

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-metadata-klib:$metadataVersion")
    if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
        implementation("org.jetbrains.kotlin:kotlin-native-utils:${project.bootstrapKotlinVersion}")
    }
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
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xskip-runtime-version-check",
        "-Xsuppress-version-warnings",
        "-Xopt-in=kotlin.ExperimentalStdlibApi"
    )
}

tasks["build"].dependsOn(":prepare-deps:build")
sourceSets["main"].withConvention(org.gradle.api.tasks.GroovySourceSet::class) {
    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        groovy.srcDir("../kotlin-native/build-tools/src/main/groovy")
    }
}

tasks.named("compileGroovy", GroovyCompile::class.java) {
    classpath += project.files(tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java))
    dependsOn(tasks.named("compileKotlin"))
}

allprojects {
    tasks.register("checkBuild")

    afterEvaluate {
        apply(from = "$rootDir/../gradle/cacheRedirector.gradle.kts")
    }
}

gradlePlugin {
    plugins {
        create("compileToBitcode") {
            id = "compile-to-bitcode"
            implementationClass = "org.jetbrains.kotlin.bitcode.CompileToBitcodePlugin"
        }
        create("runtimeTesting") {
            id = "runtime-testing"
            implementationClass = "org.jetbrains.kotlin.testing.native.RuntimeTestingPlugin"
        }
        create("konan") {
            id = "konan"
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.konan.KonanPlugin"
        }
        // We bundle a shaded version of kotlinx-serialization plugin
        create("kotlinx-serialization-native") {
            id = "kotlinx-serialization-native"
            implementationClass = "shadow.org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin"
        }

        create("org.jetbrains.kotlin.konan") {
            id = "org.jetbrains.kotlin.konan"
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.konan.KonanPlugin"
        }

        create("native") {
            id = "native"
            implementationClass = "org.jetbrains.gradle.plugins.tools.NativePlugin"
        }

        create("native-interop-plugin") {
            id = "native-interop-plugin"
            implementationClass = "org.jetbrains.kotlin.NativeInteropPlugin"
        }
    }
}