# Kotlin/Native IC fuzzer

Tools to fuzz Kotlin/Native incremental compilation (IC):

- `harness.py`: a differential runner. No LLM inside, so it is fast and deterministic.
- `../workflows/kn-ic-fuzz.js`: a Workflow that uses LLM agents to write scenarios, run them, check findings, and reduce them.

## The oracle

An incremental build must behave the same as a clean build of the same sources.

For every step of a scenario, the harness compiles the klibs once and links the program twice:

1. **IC link.** It uses an IC cache directory that lives across all steps.
2. **Clean link.** It uses a new, empty IC cache directory.

Then it compares the two links:

| Finding | Severity | Meaning |
|---|---|---|
| `OUTPUT_MISMATCH` | bug | Program output or exit code differs. Usually a stale cache: some code was not rebuilt. |
| `IC_LINK_FAILURE` | bug | The IC link fails (for example, undefined or duplicate symbols), but the clean link works. |
| `IC_CRASH` | bug | The compiler crashes on the IC link only. |
| `IC_ACCEPTS_BROKEN` | bug | The clean link fails, but the IC link works. The IC hides an error. |
| `DIAGNOSTICS_MISMATCH` | bug | Warnings or errors differ, for example partial linkage warnings. |
| `COMPILER_CRASH` | bug | The clean build crashes too. This is a compiler bug, but not an IC bug. |
| `CACHE_STATE_MISMATCH` | latent | A cached file's `metadata.properties`, `bin/bitcode_deps` or `ir/*` differs from the clean build. The binary is still right, but a later step may break. |
| `STALE_CACHE_DIR` | latent | The IC directory keeps a per-file cache that a clean build does not create. |
| `NOOP_REBUILD` | perf | After the last step, a relink with no edits still rebuilds caches. |
| `UNSTABLE_FINGERPRINT` | perf | After the last step, recompiling the klibs from the same sources makes files dirty. |
| `DEPS_DRIFT` | info | `bin/bitcode_deps` differs for a file that IC did not rebuild. This is by design: the file keeps its old code, and that code's old dependencies (see `unchanged cache:` in the corpus). It does not change the verdict and is not a signal. |
| `STALE_LIBRARY_CACHE` | info | The cache of a library that left the project stays on disk. It does not change the verdict. |

Other verdicts:

- `INVALID`: the scenario is broken. A klib does not compile, the clean link fails without an `expected compilation failure` directive, or the output is not deterministic (the clean binary runs twice).
- `PASS`: no findings.

Runs mirror KGP debug builds and the IC test fixture:

- Each module is compiled to a klib with `-XXLanguage:-IrIntraModuleInlinerBeforeKlibSerialization`.
- The link uses `-Xinclude=main.klib -g -Xpartial-linkage-loglevel=error`, plus `-Xenable-incremental-compilation -Xic-cache-dir=... -Xauto-cache-from=<ext dir>`.

## Scenario format

The format is the one in `native/native.tests/testData/caches/ic/`. A finding can become a regression test there with little work.

```
scenario/
  project.info
  lib/module.info   lib/a.kt   lib/a.1.kt   lib/b.kt
  main/module.info  main/main.kt  main/main.1.kt
```

`project.info`:

```
MODULES: lib, main

STEP 0:
    libs: lib, main
STEP 1..2:
    libs: lib, main
```

`<module>/module.info` (every module needs an entry for every step):

```
STEP 0:
    dependencies: lib
    friends: lib                // optional; friend modules must also be dependencies
    arguments: -Xcompanion-blocks // optional; extra compiler flags
    modifications:
        U : a.kt -> a.kt        // copy scenario file a.kt to source file a.kt
STEP 1:
    dependencies: lib
    modifications:
        U : a.1.kt -> a.kt      // replace a.kt with the content of a.1.kt
        D : b.kt                // delete source file b.kt
```

Rules:

- **Module order.** `libs:` gives the build order for that step. The harness rebuilds only the listed modules. It always rebuilds `main`.
- **The `main` module.** It is required, and it holds the program. Two forms work:
  - `fun main()` that prints values. Use this when fuzzing: an output diff tells more than a failed assert.
  - `@Test` functions, as in the corpus. The harness then adds `-generate-test-runner`.
- **External modules.** A module whose name starts with `external` goes to the `-Xauto-cache-from` directory. It gets a monolithic auto-cache, not per-file IC.
- **cinterop modules.** A module whose source directory has a `.def` file becomes a cinterop klib.
- **Ignored directives.** `added cache:`, `modified cache:` and `unchanged cache:` are allowed but ignored.
- **Honored directives.** `expected compilation failure:` is honored: on that step both links may fail.

## Commands

Run all commands from the repository root. Warm up once before parallel runs. The first link of a flavor builds the stdlib caches inside the distribution (about 45s).

```bash
python3 .claude/kn-ic-fuzz/harness.py warmup
```

```bash
python3 .claude/kn-ic-fuzz/harness.py run path/to/scenario --out build/kn-ic-fuzz/try1
```

`run` prints a short summary. Full data is in `<out>/result.json`, and the compiler logs are in `<out>/steps/<n>/`. Useful flags:

- `--variant unbatched`: adds an IC link with `-Xbinary=batchedCacheBuild=false`.
- `--variant threads4`: adds an IC link with `-Xbackend-threads=4`.
- `--variant NAME:"FLAGS"`: adds an IC link with your own flags. Each variant gets its own IC directory.
- `--roundtrip`: adds steps that go back through the earlier states (A, B, C, B, A).
- `--self-check`: links cleanly twice per step. A difference between the two clean links means harness noise.
- `--dist DIR`: uses another compiler. Example: an older prebuilt from `~/.konan`, to check that known bugs are found.

Shrink a failing scenario. Steps, edits and files are removed while the same finding still shows up:

```bash
python3 .claude/kn-ic-fuzz/harness.py reduce path/to/scenario --out build/kn-ic-fuzz/red1 --max-runs 15
```

Run the existing IC tests through the harness. This is a baseline with no LLM: every case should `PASS`, except the open bugs that the fuzzer found (see "The first campaign" in RESEARCH.md).

```bash
python3 .claude/kn-ic-fuzz/harness.py corpus --filter members/ --roundtrip --variant default --variant unbatched
```

## The workflow

`.claude/workflows/kn-ic-fuzz.js` runs a fuzzing campaign with LLM agents. Its phases:

1. **Prepare.** Warm up the caches. Warn if the dist is older than the latest IC change.
2. **Distill.** Two agents work in parallel:
   - One reads the IC code (`CacheBuilder`, the dependency tracker, the fingerprints) and lists places where an invalidation could be missed (as in WhiteFox).
   - One mines the history of IC fixes and their reproducers into edit classes and coverage gaps (as in Mut4All and IssueMut).
3. **Plan.** Pick diverse targets. A target is either a fresh scenario from a hypothesis, or a mutation or combination of corpus cases (the Kitten baseline). The plan is steered by the IC features seen so far and by earlier findings.
4. **Fuzz.** One agent per target:
   - writes a scenario and runs the harness;
   - repairs it if it is invalid (as in ReFuzzer);
   - mutates it while it passes, pushing on the files that IC did not mark dirty (as in Fuzz4All).
5. **Verify.** An independent agent tries to refute each finding:
   - it reruns with `--self-check`;
   - it checks that the program is deterministic;
   - it names the missed dependency;
   - it looks for a known issue.
   Then the confirmed finding is reduced: first by `harness.py reduce`, then by hand (as in LPR).
6. **Report.** Group the findings by root cause. Promote each reproducer to a corpus-style `@Test` case under `build/kn-ic-fuzz/<runId>/promoted/`. The report comes back as `reportMarkdown` in the workflow result. Subagents may not write report files, so the session that ran the workflow saves it to `build/kn-ic-fuzz/<runId>/report.md`.

Arguments (all optional):

| Argument | Default | Meaning |
|---|---|---|
| `runId` | `run` | The output goes to `build/kn-ic-fuzz/<runId>/`. |
| `rounds` | 1 | Number of rounds of plan, fuzz and verify. A round can learn from the rounds before it. |
| `perRound` | 4 | Targets per round. |
| `mutations` | 2 | Number of mutations of each passing scenario. |
| `variants` | `["default", "unbatched"]` | Harness variants. |
| `focus` | none | A free text topic, for example `"inline functions and fake overrides"`. |
| `targets` | none | Your own targets for round 1. Each is `{slug, title, strategy, plan}`. With this, Distill is skipped. |
| `dist` | the repo dist | Another compiler distribution. |
| `smoke` | `false` | One fixed target with no mutations. Use it to test the plumbing. |

The defaults give about 10 agents. Measured costs:

| Run | Agents | Tokens | Time |
|---|---|---|---|
| A smoke run | 2 | about 130k | about 2 minutes |
| One known bug found end to end (fuzz, verify, reduce, report, on an old dist) | 5 | about 430k | about 18 minutes |
| The defaults on the HEAD dist (run `c1-2026-11-01`: 4 targets, 4 bugs, so no mutations ran) | 17 | about 2.2M | about 98 minutes |

Expect about 2M tokens and 1.5 hours with the defaults. The two Distill agents take most of the time (25-40 minutes each). `{rounds: 3, perRound: 8}` costs about 6-8M. The run stops early after two rounds in a row with no new findings and no new IC features.

Check the plumbing on a known bug. Use an old prebuilt compiler and pass a target with `strategy: "mutate-corpus"` and `seed: "members/openClassToFinal"`. On `2.5.0-dev-6460` this finds KT-87777.

## Costs (Apple Silicon, warm caches)

| Operation | Time |
|---|---|
| Compile one klib | about 1.5s |
| Clean link of a small program | about 2s |
| IC link | 2-3s |
| A 3-module, 4-step scenario | 30-40s, including the no-op checks |

## Writing good scenarios (for people and agents)

- **Aim at cross-file dependencies.** An edit must change something that another file's generated code depends on, while that other file's source stays the same. IC rebuilds a file when its IR hash changes. It also rebuilds files that depend on it through recorded per-file dependencies (strong or weak) or through whole-module dependencies.
- **What the per-file cache keeps** (`ir/`):
  - `class_fields`: object layout;
  - `eager_init`;
  - `inline_bodies`;
  - `trivial_getters`;
  - `objc_adapters`.
  Edits that change these for a file that others use are good targets. So are vtable and itable layout, fake overrides, and inlined constants.
- **Make every effect visible.** Print every value the edit can change, including virtual calls through each type in the hierarchy, `is` checks, `when` over sealed types or enums, default arguments, inlined bodies, and initialization order.
- **Keep the output deterministic.** Do not print `hashCode()`, identity `toString()`, time, or the order of `HashSet` or `HashMap`.
- **Every step must compile and link in a clean build.**
- **Use longer edit sequences than the corpus has.** Examples:
  - revert (A, B, A);
  - add a declaration, then remove it;
  - rename, then rename back;
  - move a declaration between files or libraries, then move it back;
  - toggle library flags.
- **Vary the module layout:**
  - same library or across libraries;
  - transitive (A uses B, B uses C);
  - friend modules (`internal`);
  - external libraries;
  - cinterop.
