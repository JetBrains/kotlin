# Methodology

This document explains what the kapt-benchmark measures, how, and the assumptions baked into the measurement design.

## Why kapt performance matters

kapt (Kotlin annotation processing tool) is the legacy bridge between Kotlin source code and Java annotation processors. It generates Java stubs from Kotlin source so that processors like Dagger, Hilt, Room, and MapStruct — written for the Java annotation-processing API — can process Kotlin-annotated declarations.

KSP (Kotlin Symbol Processing) is the modern alternative, designed to integrate directly with Kotlin's compiler frontend without the Java-stub intermediate step. KSP is generally faster, and most major OSS Kotlin projects have migrated to it.

However, **kapt is still in use** for processors that haven't migrated (Hilt is the most prominent example), and for projects that haven't yet undertaken the migration. Improving kapt performance therefore directly improves build times for a significant body of real-world Kotlin code.

## What this benchmark measures

The headline metric is **total `:kaptKotlin` task wall time** per iteration, as reported by gradle-profiler's `--benchmark` mode. This includes:

- Gradle task setup and execution overhead.
- Kotlin compilation (FIR resolution + IR generation + JVM bytecode).
- All three internal kapt phases (`kapt.initial`, `kapt.stubs`, `kapt.apt`).
- Per-task input/output tracking.

It does **not** include:

- Configuration phase (`./gradlew help` runs before benchmark iterations).
- Daemon startup (excluded by gradle-profiler's warmup iterations).
- Other Gradle tasks (the benchmark targets `:kaptKotlin` specifically).

We use gradle-profiler in `--benchmark` mode, which:

1. Runs 2 warmup iterations to stabilize the Gradle daemon and JIT compilation.
2. Discards warmup measurements.
3. Runs 20 measured iterations, recording wall time per iteration.
4. Restarts the daemon between scenarios (so cross-scenario JIT state doesn't bleed).

The output is `profile-out/benchmark.csv` plus a chart at `profile-out/benchmark.html`.

## Workload design

The benchmark uses **synthetically generated Kotlin source** rather than real-world OSS projects. This trade-off is deliberate:

- **Pros:** identical source across runs (deterministic), identical source across workloads (only the processor differs), controlled scale (small / medium / large via a property).
- **Cons:** synthetic source doesn't capture all real-world Kotlin patterns. Notable gaps: no suspend functions, no value/inline classes, no expect/actual declarations, no large companion objects, no top-level extension functions.

The source generator (in `source-generator/`) produces classes with varied shapes — `*_Data`, `*_Base`, `*_Derived`, `*_Nested`, `*_Generic`, `*_Members` — to stress different code paths in kapt's stub generator (inheritance hierarchies, nested types, generics with bounds, etc.). All generators use a fixed seed (default 42) for determinism.

We did not use real OSS projects because:

- Most major Kotlin OSS projects have migrated to KSP.
- The Android-Hilt apps that still use kapt require Android SDK setup, which limits cross-platform reproducibility.
- A synthetic, deterministic workload makes the benchmark's measurement story cleaner — every run uses identical source code; differences come from kapt itself.

## Why three workloads?

Each workload isolates a different cost shape in the kapt task:

- **`synthetic-noop`**: a custom no-op annotation processor whose `process()` method returns immediately. Result: `kapt.apt` (annotation processing) is essentially zero, and the measured task wall is dominated by kapt's own internal work (stub generation + Kotlin compilation). This is the cleanest signal of kapt-internal cost.

- **`dagger-sample`**: Dagger 2.59.2 with `@Inject` constructors. Dagger's annotation processor does substantial work — graph validation, code generation for `@Module` and `@Component` bindings — so `kapt.apt` dominates total wall time (typically ~70% of total on a 2000-class workload). Use this to measure how kapt behaves when paired with a "heavy" real processor.

- **`mapstruct-sample`**: MapStruct 1.6.3 with `@Mapper` interfaces. MapStruct generates implementation classes for declared mapper methods. Mid-weight processor: `kapt.apt` is non-trivial (~50% of total) but doesn't dominate as much as Dagger.

The three together demonstrate the variety of kapt-task shapes that real projects produce.

## Statistical analysis

The included Python script (`scripts/summarize_benchmark.py`) computes:

- **N**: number of measured iterations (default 20).
- **Mean**: arithmetic mean of measured wall times.
- **Stderr**: standard error of the mean (= sample standard deviation / √N).
- **95% CI**: confidence interval half-width = 1.96 × stderr, assuming normality.

With N=20 and typical kapt task wall p90/p10 ratios of 1.5–2.0, 95% CI half-widths land around ±3–5% relative on the mean. If you need tighter intervals:

- Increase `iterations = 20` in the scenario files (linear cost).
- Run multiple gradle-profiler invocations and combine the results (better for catching daemon/JIT drift across runs).

The script does **not** test for normality, do bootstrap CI, or handle outliers — it's intentionally minimal. If your distribution is heavy-tailed (look at the `.html` chart that gradle-profiler emits), the normal-approximation CI is optimistic. For more rigor, use the `numpy` / `scipy` toolkit (not bundled, to keep the script standard-library only).

## Hardware and environment caveats

- **Filesystem differences matter.** macOS APFS, Linux ext4, and Windows NTFS each have different concurrent-write semantics, which can affect kapt's parallel file writes (when applicable). Cross-platform comparison of absolute numbers is unreliable; relative comparisons on the same machine are the intended use case.

- **JIT warmup matters.** The 2 warmup iterations let HotSpot compile hot methods. If you observe a downward trend across measured iterations, increase warmup count.

- **Background load matters.** Run benchmarks with no other CPU- or disk-intensive workloads competing. macOS users: disable Time Machine, Spotlight indexing, and antivirus scanning of the project directory.

- **Daemon state matters.** gradle-profiler restarts the daemon between scenarios within a file. Within a scenario, the daemon stays alive across warmup and measured iterations.

- **In-process compilation strategy.** All scenarios use `-Pkotlin.compiler.execution.strategy=in-process` to ensure kapt runs in the same JVM as Gradle, making the timing tractable. Without this, kapt may run in a daemon JVM where some Gradle measurement nuances apply.

## Customizing the benchmark

### Adding a workload

1. Create a new module under `samples/<your-workload>/` with a `build.gradle.kts` modeled on `samples/dagger-sample/build.gradle.kts`.
2. Add `:benchmarks:jvm:kapt:samples:<your-workload>` to `include(...)` in `settings.gradle.kts`.
3. Create `scenarios/<your-workload>.scenarios` with at least one scenario block.
4. Wire your annotation processor of choice into the `dependencies { kapt(...) }` block.

### Changing class counts

Edit the `when (sampleSize)` blocks in each sample's `build.gradle.kts`:

```kotlin
val classCount: Int = when (sampleSize) {
    "small" -> 100
    "medium" -> 500
    "large" -> 2000
    "extra-large" -> 5000   // Add new sizes here
    else -> error("Unknown sampleSize: $sampleSize")
}
```

Then add corresponding scenarios in the scenario files.

### Comparing Kotlin versions

Please see Kotlin repo bootstrapping options for options how to select a different Kotlin version. Then, re-run the benchmark. 

### Profiling deeper than wall time

gradle-profiler supports `--profile async-profiler` (CPU flame graphs) and `--profile heap-dump` (allocation snapshots). See gradle-profiler's documentation for setup. These are useful when investigating *why* a phase takes the time it does, not just *how long* it takes.

## Known limitations

- **Synthetic source doesn't capture all real-world Kotlin patterns.** See "Workload design" above.
- **Single workload size per scenario file.** Each scenario is one (workload, size) pair; there's no built-in mechanism for cross-size comparison within a single gradle-profiler invocation.
- **No `kapt.apt` sub-decomposition.** The benchmark measures `kapt.apt` as an opaque whole; per-processor or per-round timing is not exposed by public kapt.
- **No `kapt.initial` sub-decomposition.** Same; FIR build / Fir2Ir / JVM bytegen are not separately timed by public kapt.
- **macOS reference numbers only.** Linux and Windows reference numbers are not included.

## Reproducing the reference numbers

Run the 9-cell default suite (3 workloads × 3 sizes) and summarize (`--list` shows the opt-in cells too):

```bash
cd kapt-benchmark
for w in synthetic dagger mapstruct; do
    for s in small medium large; do
        gradle-profiler --benchmark \
            --output-dir profile-out/${w}_${s} \
            --scenario-file scenarios/${w}.scenarios \
            ${w}_${s}
    done
done
for d in profile-out/*; do
    echo "=== $d ==="
    python3 scripts/summarize_benchmark.py "$d/benchmark.csv"
done
```

Total compute: ~30 minutes wall on an M3 Max. Each scenario takes ~1–4 minutes depending on workload size and machine speed.
