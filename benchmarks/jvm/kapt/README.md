# kapt-benchmark

A reproducible benchmark for measuring Kotlin annotation processing (kapt) performance on three synthetic workloads. Designed for Kotlin compiler contributors, performance engineers, and anyone evaluating kapt characteristics on a representative codebase.

## TL;DR

```bash
brew install gradle-profiler          # one-time (macOS); see Prerequisites for Linux/Windows
./scripts/run_all_benchmarks.sh        # runs the 9-cell default suite
```

Requires JDK 17 and Python 3.7+. See [Quickstart](#quickstart) for prerequisites and [METHODOLOGY.md](METHODOLOGY.md) for what's measured.

Example output:

```
Scenario                              |  N | Mean (ms) | Stderr |        95% CI (ms)
synthetic-noop large (2000 classes)   | 20 |    4039.3 |   89.4 | [3864.0, 4214.7]
dagger-sample large (2000 classes)    | 20 |    7872.5 |  109.2 | [7658.5, 8086.5]
mapstruct-sample large (2000 classes) | 20 |    6037.9 |  117.5 | [5807.6, 6268.3]
...
```

## What this measures

- **Total `:kaptKotlin` task wall time** per iteration, using gradle-profiler's `--benchmark` mode.
- Three workload types stress different kapt patterns: a no-op processor (kapt-internal cost only), Dagger (real graph validation), and MapStruct (real mapper code generation).
- Three sample sizes per workload: small (100 classes), medium (500), large (2000 classes).
- Per-scenario statistics: 20 measured iterations after 2 warmup iterations; the included Python summary script computes mean, standard error, and 95% CI.

## Quickstart

### Prerequisites

- **JDK 17.** Tested with Eclipse Temurin / Zulu 17.0.10. Newer JDK 17 patch versions should work.
- **gradle-profiler** (any 0.20+ release; tested with 0.24.0). Install:
  - macOS (Homebrew): `brew install gradle-profiler`
  - Linux / Windows: download from [GitHub releases](https://github.com/gradle/gradle-profiler/releases) and add `bin/` to your `PATH`.
- **Python 3.7+** (only for the summary script; the benchmark itself runs without Python).

### Run a benchmark

```bash
git clone <repo-url> kapt-benchmark
cd kapt-benchmark

# Smoke-test that everything builds:
./gradlew :samples:synthetic-noop:kaptKotlin -PsampleSize=small

# Run the benchmark:
gradle-profiler --benchmark \
    --scenario-file kapt.scenarios \
    synthetic_large

# Summarize the output:
python3 scripts/summarize_benchmark.py profile-out/benchmark.csv
```

Expected output (numbers vary by hardware):

```
Scenario                              |  N |  Mean (ms) | Stderr |          95% CI (ms)
synthetic-noop large (2000 classes)   | 20 |     1180.5 |   26.3 | [1128.9, 1232.1]
```

To run the full suite (all 9 scenarios), see [Run all benchmarks](#run-all-benchmarks) below.

## Run all benchmarks

Each scenario runs 2 warmups + 20 measured iterations. Wall time per scenario ranges from ~1 minute (synthetic small) to ~4 minutes (dagger large). Total for all 9: ~30 minutes on an M3 Max.

Sizes map to class counts: small=100, medium=500, large=2000.

The repository includes a wrapper script that runs a set of scenarios and prints one combined summary table at the end.

The script writes each scenario's output to `profile-out`, then summarizes them in a single table.

To re-render the combined table from results already on disk (without rerunning anything):

```bash
python3 scripts/summarize_benchmark.py profile-out/benchmark.csv
```

## Workloads

- **`synthetic-noop`** — uses a custom no-op annotation processor (does nothing). The cleanest baseline for "kapt-internal cost only" — `kapt.apt` is essentially zero, so the measurement is dominated by kapt's own stub generation and Kotlin compilation.
- **`dagger-sample`** — uses Dagger 2.59.2 with `@Inject` injections across the generated classes. Heavy `kapt.apt` (graph validation), dominates total task wall on this workload.
- **`mapstruct-sample`** — uses MapStruct 1.6.3 with `@Mapper` interfaces. Real mapper code-generation work in `kapt.apt`; medium-weight processor.

## Customizing

- **Sample size:** `-PsampleSize=small|medium|large` (100 / 500 / 2000 generated classes).
- **Source generator seed:** `-PgeneratorSeed=42` (any integer for deterministic source generation).
- **Kotlin version:** edit `gradle.properties` and change `kotlin.version=2.3.21` to whatever public Kotlin release you want to benchmark (subject to plugin DSL compatibility).
- **Dependency versions:** edit `gradle.properties` for `dagger.version` / `mapstruct.version`.
- **Iteration counts:** edit the `kapt.scenarios` file; gradle-profiler uses the `warm-ups` and `iterations` keys.

See [METHODOLOGY.md](METHODOLOGY.md) for a deep dive on what's measured, how, and known limitations.

## Reproducing reference measurements

We tested this benchmark on:
- Hardware: Apple M3 Max, 16 cores, 64 GB RAM
- OS: macOS 14.1 (arm64)
- JDK: Eclipse Temurin 17.0.10
- Kotlin: 2.3.21
- Gradle: 8.14.4
- gradle-profiler: 0.24.0

Numbers will vary on your hardware. The benchmark is designed for **relative** comparisons (version-to-version, configuration-to-configuration on the same machine), not absolute reproducibility across hardware.

## License

Apache-2.0. See [LICENSE](LICENSE).
