import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    // workaround for KGP build metrics reports: https://github.com/gradle/gradle/issues/20001
    project.extensions.extraProperties["kotlin.build.report.output"] = null

    val versionPropertiesFile = project.rootProject.projectDir.parentFile.resolve("gradle/versions.properties")
    val versionProperties = java.util.Properties()
    versionPropertiesFile.inputStream().use { propInput ->
        versionProperties.load(propInput)
    }
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.google.code.gson" && requested.name == "gson") {
                useVersion(versionProperties["versions.gson"] as String)
                because("Force using same gson version because of https://github.com/google/gson/pull/1991")
            }
        }
    }
}

logger.info("buildSrcKotlinVersion: " + extra["bootstrapKotlinVersion"])
logger.info("buildSrc kotlin compiler version: " + org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
logger.info("buildSrc stdlib version: " + KotlinVersion.CURRENT)

apply {
    from("../gradle/checkCacheability.gradle.kts")
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `groovy`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.sam.with.receiver")
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
val intellijSeparateSdks by extra(project.getBooleanProperty("intellijSeparateSdks") ?: false)

extra["intellijReleaseType"] = when {
    extra["versions.intellijSdk"]?.toString()?.contains("-EAP-") == true -> "snapshots"
    extra["versions.intellijSdk"]?.toString()?.endsWith("SNAPSHOT") == true -> "nightly"
    else -> "releases"
}

extra["versions.androidDxSources"] = "5.0.0_r2"
extra["customDepsOrg"] = "kotlin.build"

repositories {
    mavenCentral()
    maven("https://maven.google.com/")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    gradlePluginPortal()

    extra["bootstrapKotlinRepo"]?.let {
        maven(url = it)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
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
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    implementation("com.gradle.publish:plugin-publish-plugin:1.0.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.8.0-dev-189")

    implementation("com.jakewharton.dex:dex-member-list:4.1.1")

    implementation("gradle.plugin.com.github.johnrengelman:shadow:${rootProject.extra["versions.shadow"]}") {
        // https://github.com/johnrengelman/shadow/issues/807
        exclude("org.ow2.asm")
    }
    implementation("net.sf.proguard:proguard-gradle:6.2.2")

    implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:1.0.1")

    implementation("org.gradle:test-retry-gradle-plugin:1.2.0")
    compileOnly("com.gradle:gradle-enterprise-gradle-plugin:3.11.2")

    compileOnly(gradleApi())

    val kotlinVersion = project.bootstrapKotlinVersion
    val ktorVersion  = "1.2.1"
    val slackApiVersion = "1.2.0"
    val metadataVersion = "0.0.1-dev-10"

    // See https://github.com/gradle/gradle/issues/22510
    implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:2.4.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.ullink.slack:simpleslackapi:$slackApiVersion") {
        exclude(group = "com.google.code.gson", module = "gson") // Workaround for Gradle dependency resolution error
    }
    implementation("com.google.code.gson:gson:2.8.9") // Workaround for Gradle dependency resolution error

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.5.0")
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

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.allWarningsAsErrors = true
    kotlinOptions.freeCompilerArgs += listOf(
        "-opt-in=kotlin.RequiresOptIn",
        "-Xsuppress-version-warnings",
        "-opt-in=kotlin.ExperimentalStdlibApi"
    )
}

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
        create("compilationDatabase") {
            id = "compilation-database"
            implementationClass = "org.jetbrains.kotlin.cpp.CompilationDatabasePlugin"
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
