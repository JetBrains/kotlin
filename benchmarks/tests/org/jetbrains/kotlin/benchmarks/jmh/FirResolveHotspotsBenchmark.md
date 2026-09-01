# FirResolveHotspotsBenchmark — baseline & coverage

Multi-feature FIR **resolve-only** microbenchmark (see `FirResolveHotspotsBenchmark.kt`).
Timed path: `FirTotalResolveProcessor.process` / `runResolution`. PSI is trial setup; raw FIR rebuild is invocation setup (outside the `@Benchmark` method).

## Size choice

| Param | Rationale |
|------:|-----------|
| **`size=10`** | Representative multi-feature corpus (overloads + generics/lambdas + nested `with` + UI-like fan-out) at ~0.1 s/op after warmup — enough work per op for stable avgt samples, short enough for multi-fork baselines on a laptop/CI agent. |
| 50 / 100 | Available via `@Param` for stress; not used for the recorded baseline (runtime scales roughly with block count). |

## Baseline command

```bash
export JAVA_HOME=/usr/lib/jvm/amazon-corretto-17.0.9.8.1-linux-x64   # Gradle launcher
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew :benchmarks:testBenchmark \
  -Pinclude='org.jetbrains.kotlin.benchmarks.jmh.FirResolveHotspotsBenchmark' \
  -Pwarmups=5 -Piterations=5 -Psize=10 -Pforks=3 \
  --no-daemon
```

- Mode: JMH `avgt`, iteration time 1 s (kotlinx-benchmark `main` config)
- Threads: 1 (JMH default)
- Forks / warmups / iterations: 3 / 5 / 5 → **Cnt = 15**
- Stdlib: `-Dkotlin.runtime.path=<repo>/dist/kotlinc/lib/kotlin-stdlib.jar` (set by `:benchmarks` `testBenchmark`)
- Working dir: repo root

Optional smoke (not a baseline):

```bash
./gradlew :benchmarks:testBenchmark \
  -Pinclude='org.jetbrains.kotlin.benchmarks.jmh.FirResolveHotspotsBenchmark' \
  -Pwarmups=1 -Piterations=1 -Psize=10 --no-daemon
```

## Environment (this machine)

| Item | Value |
|------|-------|
| Host | Linux 6.17.0-1020-azure x86_64 |
| CPU | AMD EPYC 7763, **8** logical CPUs (4 cores) |
| RAM | **~31 GiB** |
| Gradle launcher `JAVA_HOME` | Amazon Corretto **17.0.9** (`/usr/lib/jvm/amazon-corretto-17.0.9.8.1-linux-x64`) |
| **JMH worker JVM** (from result JSON) | Amazon Corretto **8.392** (`…/amazon-corretto-8.392.08.1-linux-x64/jre/bin/java`) — project test toolchain default |
| JMH heap | No explicit `-Xmx` on `testBenchmark`; ergonomic **MaxHeapSize ≈ 8.4 GiB** on this host |
| Gradle daemon heap | `org.gradle.jvmargs=-Xmx4g` (does not apply to JMH forks) |
| JMH | 1.37 via kotlinx-benchmark |

> Always trust the `jvm` / `jdkVersion` fields in `benchmarks/build/reports/benchmarks/main/<timestamp>/test.json` over `JAVA_HOME` when quoting a baseline.

## Results (real runs — not fabricated)

### Primary baseline (2026-08-07)

| Benchmark | size | Mode | Cnt | Score | Error (99.9% CI half-width) | Unit |
|-----------|-----:|------|----:|------:|----------------------------:|------|
| `FirResolveHotspotsBenchmark.benchmark` | 10 | avgt | 15 | **107.217** | **± 27.534** | ms/op |

- min / avg / max = 76.593 / 107.217 / 153.277; sample stdev ≈ 25.8 ms/op  
- CI (99.9%): \[79.682, 134.751\]  
- Wall: `BUILD SUCCESSFUL in 1m 40s` (mostly up-to-date compile + JMH)  
- JSON: `benchmarks/build/reports/benchmarks/main/2026-08-07T17.20.48.527007243/test.json`

Per-fork measurement raw means (ms/op), fork × iteration:

| Fork | it1 | it2 | it3 | it4 | it5 |
|-----:|----:|----:|----:|----:|----:|
| 1 | 150.6 | 109.7 | 118.2 | 85.0 | 82.6 |
| 2 | 144.6 | 121.0 | 95.8 | 99.7 | 81.8 |
| 3 | 153.3 | 111.4 | 90.7 | 87.1 | 76.6 |

### Verification re-run (same command, same host)

| Score | Error | min / avg / max | stdev | Wall |
|------:|------:|-----------------|------:|------|
| **103.752** | **± 24.076** | 77.263 / 103.752 / 147.957 | 22.5 | 1m 33s |

JSON: `…/2026-08-07T17.28.59.549325301/test.json`

The two means lie inside each other’s 99.9% CI bands (normal run-to-run noise). Scores still trend down within a fork after the configured warmups — treat the ±error as part of the baseline, not a single point estimate.

Non-fatal during trial setup: `WARN: Extension to be removed not found: JavaElementFinder`.

## Coverage vs kotlinconf-app profile hotspots

Profile source: `/workspace/scratch/profiles/REPORT/OPTIMIZATION_OPPORTUNITIES.md` (combined CPU under FIR / `resolveAndCheckFir`).

| Profile band | Profile share (combined CPU) | Bench coverage |
|--------------|-----------------------------:|----------------|
| **B1** call / candidate | ~**13.2%** | **Yes** — `Text` / `item` / `Container` overload sets, trailing-lambda UI fan-out, nested calls |
| **B2** inference / constraints | ~**9.5%** | **Yes** — `remember` / `mutableStateOf` / `mapState` / `maxOfTwo`, lambda args, generic bounds |
| **B4** scopes / lookup | ~**8.8%** | **Yes** — nested `with(theme/density/focus)`, extension receivers `themed` / `withDensity` |
| Checkers / diagnostics | ~**9.2%** (~⅓ of `resolveAndCheckFir`) | **No** — only `FirTotalResolveProcessor` / `runResolution` |
| Raw FIR builder | ~**1.7%** | **Outside timed method** (invocation `@Setup` via `prepareFirForResolve`) |
| FIR2IR / IR lowering / JVM codegen | large backend share | **No** |
| Compose IR plugin / Metro | ~7–12% area on app | **No** — stdlib-only synthetic APIs, no compiler plugins |
| Real app multi-module classpath | n/a | **No** — single synthetic file + dist `kotlin-stdlib.jar` |

**Honest scope:** profile-*shaped* resolve microbench, not bit-identical to kotlinconf-app compile. Use it to regress FIR resolve cost on a fixed corpus; do not treat scores as end-to-end frontend or app wall time.
