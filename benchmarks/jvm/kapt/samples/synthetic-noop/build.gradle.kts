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
if ((findProperty("stripMetadata") as String?).toBoolean()) {
    kapt { stripMetadata = true }
}

val generatorClasspath = configurations.create("generatorClasspath") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    generatorClasspath(project(":benchmarks:jvm:kapt:source-generator"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${project.bootstrapKotlinVersion}")
    // compileOnly: annotation classes available at compile time but not on runtime classpath.
    // kapt: registers the same artifact on the annotation-processor path.
    compileOnly(project(":benchmarks:jvm:kapt:synthetic-processor"))
    kapt(project(":benchmarks:jvm:kapt:synthetic-processor"))
}

val sampleSize: String = (findProperty("sampleSize") ?: "small") as String
val classCount: Int = when (sampleSize) {
    "small" -> 100
    "medium" -> 500
    "large" -> 2000
    else -> error("Unknown sampleSize: $sampleSize (use small, medium, or large)")
}

val generatorSeed: String = (findProperty("generatorSeed") ?: "42") as String

// Opt-in wide-class mode: -PclassWidth=N generates N properties per class instead of the
// default shape rotation. Absent => sources are byte-identical to the default scenarios.
val classWidth: String? = findProperty("classWidth") as String?

// Opt-in nesting-heavy mode: -PclassNesting=N generates N nested classes per class, so every
// property type resolves through a `$`-containing internal name. Absent => default shapes.
val classNesting: String? = findProperty("classNesting") as String?

// Opt-in body-heavy mode: -PclassBodySize=N generates N inference-heavy methods per class instead
// of the default (essentially body-free) shapes. Absent => default shapes, so the other scenarios
// are unaffected. Purpose: give BODY_RESOLVE something to resolve, so its share can be measured.
val classBodySize: String? = findProperty("classBodySize") as String?

// Opt-in deep-package mode: -PclassPackageDepth=N appends N extra package components, deepening the
// stub output tree. Absent => default package, so default scenarios are unaffected.
val classPackageDepth: String? = findProperty("classPackageDepth") as String?

// Opt-in: make every generated class reference the hand-written research.kapt.base.Base, so that an
// ABI change to Base impacts all of them. Used by the incremental scenario.
val commonBase: String? = findProperty("commonBase") as String?

val generatedSrcDir = layout.buildDirectory.dir("generated/source/research-kapt/main")

val generateSources = tasks.register<JavaExec>("generateSources") {
    classpath = generatorClasspath
    mainClass.set("research.kapt.gen.MainKt")
    outputs.dir(generatedSrcDir)
    inputs.property("classCount", classCount)
    inputs.property("mode", "synthetic")
    inputs.property("seed", generatorSeed)
    inputs.property("classWidth", classWidth ?: "")
    inputs.property("classNesting", classNesting ?: "")
    inputs.property("classPackageDepth", classPackageDepth ?: "")
    inputs.property("classBodySize", classBodySize ?: "")
    inputs.property("commonBase", commonBase ?: "")
    args(
            "--count", classCount.toString(),
            "--mode", "synthetic",
            "--seed", generatorSeed,
            "--out-dir", generatedSrcDir.get().asFile.absolutePath
    )
    if (classWidth != null) args(listOf("--wide", classWidth))
    if (classNesting != null) args(listOf("--nested", classNesting))
    if (classPackageDepth != null) args(listOf("--package-depth", classPackageDepth))
    if (classBodySize != null) args(listOf("--bodies", classBodySize))
    if (commonBase != null) args(listOf("--common-base", commonBase))
}

sourceSets["main"].kotlin.srcDir(generatedSrcDir)

afterEvaluate {
    // afterEvaluate: kaptGenerateStubsKotlin is registered lazily by the kapt plugin.
    tasks.named("compileKotlin") { dependsOn(generateSources) }
    tasks.named("kaptGenerateStubsKotlin") { dependsOn(generateSources) }
}
