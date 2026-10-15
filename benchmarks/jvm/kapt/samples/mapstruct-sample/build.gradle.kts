plugins {
    kotlin("jvm")
    kotlin("kapt")
}

kotlin {
    jvmToolchain(17)
}

// Opt-in: -PstripMetadata=true drops @kotlin.Metadata annotations from generated stubs.
// Absent => kapt's default (false), so the other scenarios are unaffected.
if ((findProperty("stripMetadata") as String?).toBoolean()) {
    kapt { stripMetadata = true }
}

val mapstructVersion: String = (findProperty("mapstruct.version") ?: "1.6.3") as String

dependencies {
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

val generateSources by tasks.registering(JavaExec::class) {
    classpath = project(":source-generator").the<SourceSetContainer>()["main"].runtimeClasspath
    mainClass.set("research.kapt.gen.MainKt")
    outputs.dir(generatedSrcDir)
    inputs.property("classCount", classCount)
    inputs.property("mode", "mapstruct")
    inputs.property("seed", generatorSeed)
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            "--count", classCount.toString(),
            "--mode", "mapstruct",
            "--seed", generatorSeed,
            "--out-dir", generatedSrcDir.get().asFile.absolutePath
        )
    })
}

sourceSets["main"].kotlin.srcDir(generatedSrcDir)

afterEvaluate {
    // afterEvaluate: kaptGenerateStubsKotlin is registered lazily by the kapt plugin.
    tasks.named("compileKotlin") { dependsOn(generateSources) }
    tasks.named("kaptGenerateStubsKotlin") { dependsOn(generateSources) }
}
