# Java-Direct: Agent Instructions

**Current status**: 1168/1168 box + 1454/1456 phased (2679/2681, 99.9%), 2 known won't-fix.
The module is feature-complete; active work is optimization and refactoring.

**Key files**: `JavaClassOverAst.kt`, `JavaTypeOverAst.kt`, `JavaMemberOverAst.kt`,
`JavaResolutionContext.kt`, `JavaClassFinderOverAstImpl.kt`.
Full map in `implDocs/ARCHITECTURE.md`.

---

## ⚠ Non-Negotiable Rules (stop immediately if violated)

1. **No command chaining** — NEVER use `&&`, `||`, or `;`. Each command = one tool call.
   Why: the permission system only checks the first token.

2. **Always pipe Gradle output to `tee "$JD_TMP/..."`** — no exceptions.
   If you forgot `tee`: do NOT rerun Gradle. Grep whatever output you have, or ask the user.

3. **Only the main agent runs Gradle** — subagents MUST NOT invoke `./gradlew`.
   Why: parallel builds corrupt each other's test results and cause excessive CPU and disk load.

4. **NEVER create git commits** — all changes must be reviewed by the user first.

5. **NEVER run `-Pkotlin.test.update.test.data=true`** — corrupts shared test data in
   `compiler/testData/` and `compiler/fir/analysis-tests/testData/`.

6. **NEVER modify test data to make java-direct tests pass** — fix the implementation,
   or document it as a known acceptable difference in `ITERATION_RESULTS.md`.
   Test data files are shared between java-direct and PSI test runners; a diverging
   java-direct result means the java-direct implementation is wrong.

---

## Shell Discipline

### Session temp directory

At the start of each session:
```bash
export JD_TMP="/tmp/jd_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$JD_TMP"
```
All temp file paths in this document use `$JD_TMP`. **NEVER write directly to `/tmp/`** —
always use the session directory.

### One command per execution

The permission system matches on the **first token only**. With `cmd1 && cmd2`, only
`cmd1` is checked — `cmd2` runs without review. Run sequential commands as separate
tool calls. `|` (piping) is fine; `&&`, `||`, `;` are not.

### Gradle runs: save output, run once

Every Gradle invocation MUST `tee` its output to `$JD_TMP`. Include `--stacktrace` for
suite and single-test runs. Do NOT use `--info`/`--debug` unless specifically needed.
After a run, **grep the saved file** — never rerun Gradle just to see a different slice.

---

## Ground Rules

- **Use JetBrains MCP tools** for all project file operations (see `.ai/guidelines.md`).
- **Search before reading**: prefer `search_in_files_by_text` / `search_in_files_by_regex`
  over `get_file_text_by_path` for large files — search tools return only matching lines.
- **Oversized MCP results**: when a call exceeds the token limit, the result is auto-saved
  to `~/.claude/projects/.../tool-results/<tool>-<timestamp>.txt`. Filter it with
  `grep`/`jq` via Bash rather than loading the full file into context.
- **Check `git diff` for unintended changes** after every test run.
- **Run `get_file_problems`** (errorsOnly=false) after edits; fix warnings related to your
  changes.
- **FIR terminology**: `simpleImports` / `starImports` (NOT singleType / onDemand).

---

## Test Commands

```bash
# Both suites together (~2681 tests) — preferred for verification
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --tests "JavaUsingAstBoxTestGenerated" --stacktrace --rerun-tasks --no-build-cache 2>&1 | tee "$JD_TMP/jd_test.txt"

# Box tests only (~1168)
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstBoxTestGenerated" --stacktrace --rerun-tasks --no-build-cache 2>&1 | tee "$JD_TMP/jdb_test.txt"

# Phased/diagnostic tests only (~1456)
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --stacktrace --rerun-tasks --no-build-cache 2>&1 | tee "$JD_TMP/jdp_test.txt"

# Unit tests (MUST stay green)
./gradlew :kotlin-java-direct:test --tests "JavaParsingTest" --stacktrace -q

# Single test
./gradlew :kotlin-java-direct:test --tests "*JavaUsingAstBoxTestGenerated.*testSpecificName*" --stacktrace -q --rerun 2>&1 | tee "$JD_TMP/single_test.txt"

# PSI regression (only after shared FIR file or test data changes)
./gradlew :compiler:fir:analysis-tests:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" --stacktrace -q 2>&1 | tee "$JD_TMP/psi_test.txt"
```

### Extracting failures

**Use saved Gradle text output — never XML files** (box/phased share the same results
directory).

```bash
grep "FAILED" "$JD_TMP/jd_test.txt" | sort -u
grep -A5 "FAILED" "$JD_TMP/jd_test.txt" | grep -E "IllegalState|NoSuch|Exception|Error:|UNRESOLVED|MISSING|Actual data" | head -60
```

---

## Shared FIR Files (modify with caution)

- `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt`
- `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt`
- `compiler/fir/fir-jvm/src/.../javaAnnotationsMapping.kt`
- `core/compiler.common.jvm/src/.../load/java/structure/*.kt`

For any edit to these: always compare with upstream first —
```bash
git show origin/master:<path> | grep -A10 "relevantFunction"
```
Then run PSI regression BEFORE and AFTER the change. If new PSI failures appear,
**revert immediately**.

---

## When a regression appears

The module is stable; investigation is now the exception rather than the norm. When a
test regresses:

1. **Triage** — run both suites once, save output, extract failing tests (`grep FAILED`).
2. **Debug 2–3 representative tests** — confirm root cause via exception-based debugging
   (see `implDocs/INVESTIGATION_TECHNIQUES.md`).
3. **Check the reference** — javac-wrapper (`TreeBasedClass.kt`, `TreeBasedField.kt`,
   `TreeBasedMethod.kt`) or PSI (`JavaClassImpl.java`, `JavaMemberImpl.java`).
4. **Implement a minimal fix** — then rerun the full suite. **Any regression → revert.**
   A net improvement of +3/-2 is not acceptable.
5. **Document** — append an entry to `ITERATION_RESULTS.md`.

---

## What NOT to Do

- Don't rerun Gradle for a different view of results — grep the saved log file.
- Don't chain shell commands with `&&` — one command per tool call.
- Don't let subagents run Gradle.
- Don't use `--info`/`--debug` unless specifically necessary.
- Don't hardcode lists for resolution — use the callback pattern
  (see `implDocs/ARCHITECTURE.md`).
- Don't assume AST token names — always verify (see `implDocs/INVESTIGATION_TECHNIQUES.md`).
- Don't change the `findClassId` probe order in `JavaTypeConversion.kt` (shared with PSI)
  — fix in `JavaResolutionContext.resolve()` instead.
- Don't return ambiguous strings from resolution — use `ClassId`-based resolution
  (see `implDocs/RESOLUTION_PIPELINE.md`).
- Don't run `FirLightTreeBlackBoxCodegenTestGenerated.*testName*` — nested `$` silently
  matches nothing.

---

## Performance Measurement

When profiling java-direct code paths:

- **Instrumentation stash**: `git stash show stash@{0}` — the stash named
  `phase-c-instrumentation-v5-v6-measurements` contains a complete `PhaseCMeasurementCounters`
  singleton with `AtomicLong` counters, `ThreadMXBean` CPU brackets, per-classloader dump files,
  and an AWK aggregator script. Pop it to get a ready-made measurement harness.
- **Classloader isolation**: Gradle runs each `*FullPipelineTestsGenerated` test method in its
  own classloader. A Kotlin `object` singleton is per-classloader, not per-JVM. Dump files must
  include `System.identityHashCode(PhaseCMeasurementCounters::class.java)` in the filename to
  avoid overwrites. Aggregate with the `aggregate-phase-c-dumps.sh` script.
- **CPU time**: use `ThreadMXBean.getCurrentThreadCpuTime()` (per-thread, aggregates correctly
  under `CONCURRENT` execution). `System.nanoTime()` is unreliable inside Gradle workers.
- **Forcing java-direct**: `-Pfir.force.javaDirect=true` enables java-direct on all modules
  regardless of model XML. Requires the one-line passthrough in
  `AbstractIsolatedFullPipelineModularizedTest.kt` (currently on HEAD).
- **Corpora**: `KotlinFullPipelineTestsGenerated` (414 modules, 109 with Java sources) for
  mixed workloads; `IntelliJFullPipelineTestsGenerated.testIntellij_platform_*` (446 modules)
  for Java-heavy workloads. The full IntelliJ suite is multi-hour; use subsets.
- See `implDocs/INVESTIGATION_TECHNIQUES.md` for detailed recipes.

---

## Reference Documents

| Document | When to consult |
|----------|----------------|
| `implDocs/ARCHITECTURE.md` | Callback patterns, key files, JLS implicit rules, common fixes |
| `implDocs/RESOLUTION_PIPELINE.md` | Before any resolution fix |
| `implDocs/INVESTIGATION_TECHNIQUES.md` | Debugging, AST inspection, measurement recipes |
| `ITERATION_RESULTS.md` | Current iteration log (new entries on top) |
| `implDocs/archive/` | Historical iterations, completed plans, measurement data |

---

*Last updated: 2026-04-22 (Phases A-E complete; measurement section added)*
