import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("common-configuration")
    kotlin("jvm")
    kotlin("kapt")
}

jvmToolchains {
    jdkVersion = JdkMajorVersion.JDK_17_0
    targetBytecodeVersion = JdkMajorVersion.JDK_17_0
}

// Opt-in: -PstripMetadata=true drops @kotlin.Metadata annotations from generated stubs.
// Absent => kapt's default (false), so the other scenarios are unaffected.
if (providers.gradleProperty("stripMetadata").orNull.toBoolean()) {
    kapt { stripMetadata = true }
}

val daggerVersion: String = providers.gradleProperty("dagger.version").getOrElse("2.59.2")

// dagger-compiler pulls an older kotlin-metadata-jvm that rejects @Metadata emitted by
// a newer compiler ("Provided Metadata instance has version X, while maximum supported
// version is Y"). Force the version matching kotlin.version. This must cover
// kaptClasspath_kaptKotlin (the configuration the kapt task actually resolves), not
// just the declarable `kapt` configuration.
configurations.all {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-metadata-jvm:${project.bootstrapKotlinVersion}")
}

val generatorClasspath = configurations.create("generatorClasspath") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    generatorClasspath(project(":benchmarks:jvm:kapt:source-generator"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${project.bootstrapKotlinVersion}")
    // implementation: runtime artifacts (Dagger annotations + javax.inject).
    // kapt: registers the Dagger processor on the annotation-processor path.
    implementation("com.google.dagger:dagger:$daggerVersion")
    kapt("com.google.dagger:dagger-compiler:$daggerVersion")
    implementation("javax.inject:javax.inject:1")
}

val sampleSize: String = providers.gradleProperty("sampleSize").getOrElse("small")
val classCount: Int = when (sampleSize) {
    "small" -> 100
    "medium" -> 500
    "large" -> 2000
    else -> error("Unknown sampleSize: $sampleSize (use small, medium, or large)")
}

val generatorSeed: String = providers.gradleProperty("generatorSeed").getOrElse("42")

val generatedSrcDir = layout.buildDirectory.dir("generated/source/research-kapt/main")

val generateSources = tasks.register<JavaExec>("generateSources") {
    classpath = generatorClasspath
    mainClass.set("research.kapt.gen.MainKt")
    outputs.dir(generatedSrcDir)
    inputs.property("classCount", classCount)
    inputs.property("mode", "dagger")
    inputs.property("seed", generatorSeed)
    args(
        "--count", classCount.toString(),
        "--mode", "dagger",
        "--seed", generatorSeed,
        "--out-dir", generatedSrcDir.get().asFile.absolutePath
    )
}

sourceSets["main"].kotlin.srcDir(generateSources)

// The samples have no tests. kapt registers stub/AP tasks for the empty test compilation
// anyway; the stub task lacks @SkipWhenEmpty, so it executes on every test-compiling
// build doing no work and (being new) trips the build-cache check on CI.
tasks.withType<KotlinCompile>().matching { it.name == "kaptGenerateStubsTestKotlin" }.configureEach { enabled = false }
