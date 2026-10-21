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

val mapstructVersion: String = (findProperty("mapstruct.version") ?: "1.6.3") as String

val generatorClasspath = configurations.create("generatorClasspath") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    generatorClasspath(project(":benchmarks:jvm:kapt:source-generator"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${project.bootstrapKotlinVersion}")
    // implementation: runtime mapper interface + helpers.
    // kapt: registers the MapStruct processor on the annotation-processor path.
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    kapt("org.mapstruct:mapstruct-processor:$mapstructVersion")
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
    inputs.property("mode", "mapstruct")
    inputs.property("seed", generatorSeed)
    args(
        "--count", classCount.toString(),
        "--mode", "mapstruct",
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
