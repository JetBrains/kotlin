plugins {
    id("common-configuration")
    kotlin("jvm")
    kotlin("kapt")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xdetailed-perf")
    }
}

jvmToolchains {
    jdkVersion = JdkMajorVersion.JDK_17_0
    targetBytecodeVersion = JdkMajorVersion.JDK_17_0
}

// Opt-in: -PstripMetadata=true drops @kotlin.Metadata annotations from generated stubs.
// Absent => kapt's default (false), so the other scenarios are unaffected.
if ((findProperty("stripMetadata") as String?).toBoolean()) {
    kapt { stripMetadata = true }
}

val daggerVersion: String = (findProperty("dagger.version") ?: "2.59.2") as String

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

val sampleSize: String = (findProperty("sampleSize") ?: "small") as String
val classCount: Int = when (sampleSize) {
    "small" -> 100
    "medium" -> 500
    "large" -> 2000
    else -> error("Unknown sampleSize: $sampleSize (use small, medium, or large)")
}

val generatorSeed: String = (findProperty("generatorSeed") ?: "42") as String

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

sourceSets["main"].kotlin.srcDir(generatedSrcDir)

afterEvaluate {
    // afterEvaluate: kaptGenerateStubsKotlin is registered lazily by the kapt plugin.
    tasks.named("compileKotlin") { dependsOn(generateSources) }
    tasks.named("kaptGenerateStubsKotlin") { dependsOn(generateSources) }
}
