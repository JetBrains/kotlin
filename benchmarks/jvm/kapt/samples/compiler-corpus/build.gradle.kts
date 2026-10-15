plugins {
    kotlin("jvm")
    kotlin("kapt")
}

kotlin {
    jvmToolchain(17)

    // Extension-level, deliberately. Two earlier attempts failed silently:
    //  - on kaptGenerateStubsKotlin directly: KaptGenerateStubsConfig.kt:153-157 resets the stub
    //    task's freeCompilerArgs to null and re-registers compileKotlin's as its convention;
    //  - via tasks.named("compileKotlin"): configuration avoidance means that block never runs
    //    when only :kaptGenerateStubsKotlin is requested, so the convention saw unmodified args.
    // The extension's options are what KaptGenerateStubsConfig.kt:38 reads, so they always apply.
    compilerOptions {
        // The corpus is other people's code compiled out of its home module: warnings are noise,
        // and several files legitimately use internal/experimental compiler API.
        freeCompilerArgs.addAll("-nowarn", "-Xskip-prerelease-check")
        // Arm C: -PheaderMode=true adds -Xheader-mode. Expected to be inert on kapt's PSI path,
        // since headerMode is implemented in light-tree2fir only and kapt forces PSI.
        if ((findProperty("headerMode") as String?).toBoolean()) {
            freeCompilerArgs.add("-Xheader-mode")
        }
        // -PreportPerf=true asks the compiler for its own per-phase timings (Initialization,
        // Analysis, TranslationToIr, Backend). Attribution only: it perturbs wall time, so the
        // headline deltas come from the clean run, not from a build with this on.
        if ((findProperty("reportPerf") as String?).toBoolean()) {
            freeCompilerArgs.add("-Xreport-perf")
            // -Xdump-perf writes a structured report; -Xreport-perf alone logs at LOGGING
            // severity, which Gradle only surfaces at --debug.
            freeCompilerArgs.add("-Xdump-perf=" + (findProperty("perfOut") as String? ?: "/tmp/perf") + "/")
        }
    }
}

val kotlinVersion: String = property("kotlin.version") as String

// Step 19's corpus is real Kotlin compiler source, copied from the repo. It compiles against
// kotlin-compiler-embeddable, which already contains these very classes plus everything they
// reference — so the corpus resolves without reconstructing the compiler's module graph.
dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    compileOnly(project(":synthetic-processor"))
    kapt(project(":synthetic-processor"))
}

// Arm selector. `-PbodyMode=stripped` compiles the body-stripped copy of the corpus instead of
// the original. Both trees are checked in under src/, so each arm is an ordinary task input and
// Gradle's up-to-date checks behave normally.
val bodyMode: String = (findProperty("bodyMode") ?: "baseline") as String
val corpusDir = when (bodyMode) {
    "baseline" -> "src/baseline/kotlin"
    "stripped" -> "src/stripped/kotlin"
    else -> error("Unknown bodyMode: $bodyMode (use baseline or stripped)")
}

kotlin.sourceSets["main"].kotlin.setSrcDirs(listOf(corpusDir))

