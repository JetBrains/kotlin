# Java-Direct: Agent Instructions

**Current status**: 1178/1178 box + 1513/1513 phased (2793/2793, 100%). No
known won't-fix.
The module is feature-complete on the `JavaUsingAst*` suite; active work is
optimization, the merged PSI-removal × resolver-unification refactoring, and
closing the IJ-FP regression delta.

> **Caveat on historical numbers.** Before 2026-04-28 the `JavaUsingAst*` test
> generators did **not** actually route `// FILE: *.java` blocks through
> `java-direct`'s AST — every Java class fell through to PSI's
> `JavaClassFinderImpl`. Any "feature complete" / `1168/1168 box` /
> `1454/1456 phased` claim dated before 2026-04-28 was against the PSI loader,
> not `java-direct`. Treat older docs and archive entries with that lens.
> See `implDocs/archive/ITERATION_RESULTS_2026_05_11.md` (entry
> *Test framework wiring: java-direct AST was never used — 2026-04-28*).

**Key files**: `JavaClassOverAst.kt`, `JavaTypeOverAst.kt`, `JavaMemberOverAst.kt`,
`JavaResolutionContext.kt`, `JavaClassFinderOverAstImpl.kt`,
`BinaryJavaClassFinder.kt`, `JavaModelSessionAccess.kt`,
`JavaSupertypeLoopChecker.kt`.
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
   java-direct result usually means the java-direct implementation is wrong.
   *Rare exception*: tests that depend on JDK-version-specific javac behaviour
   (e.g. user code in `java.util.*` rejected by JDK 17's module seal) may be
   genuinely won't-fix on the java-direct test worker — record them with the
   investigation evidence in the iteration log before declaring won't-fix
   (cf. archived iteration 58 in `ITERATIONS_52_71_DETAILS.md`).

7. **No new public members on Java-model interfaces** in `core/compiler.common.jvm/src/.../load/java/structure/`
   (`JavaType`, `JavaClassifierType`, `JavaAnnotation`, `JavaField`, `JavaAnnotationArgument`,
   etc.). The architectural goal of `java-direct` is that the model presents the same
   public interface surface as PSI/binary impls; members added during `java-direct`
   development are debt — do not add more, prefer rolling back existing ones. If a
   rollback is genuinely impossible (perf or correctness cost), put the protocol on a
   `java-direct`-private subinterface inside `compiler/java-direct/src/.../model/` and
   record the obstacle in `ITERATION_RESULTS.md`. **This rule supersedes any in-flight
   design doc that suggests adding a new member as a "bridge", "hint", or "side-channel".**
   See `implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` for the rollback inventory.

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
# Both suites together (~2793 tests) — preferred for verification
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --tests "JavaUsingAstBoxTestGenerated" --stacktrace --rerun-tasks --no-build-cache 2>&1 | tee "$JD_TMP/jd_test.txt"

# Box tests only (~1178)
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstBoxTestGenerated" --stacktrace --rerun-tasks --no-build-cache 2>&1 | tee "$JD_TMP/jdb_test.txt"

# Phased/diagnostic tests only (~1513)
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

## Critical Patterns (do not break)

- **`JavaModelSessionAccess.kt` is the single chokepoint** through which the model reads
  `FirSession.symbolProvider`. Its **`(session, classId)`-keyed re-entrance guard** breaks the
  KT-74097 cycle (`LazyThreadSafetyMode.PUBLICATION` lazies recurse silently on
  same-thread re-entrance). Do **not** add another `ThreadLocal` /
  `FirSession.symbolProvider` consumer in `compiler/java-direct/.../resolution/` —
  funnel every probe through the two extensions `FirSession.cycleSafeClassLikeSymbol` /
  `FirSession.cycleSafeTryResolveClass`.
- **`JavaSupertypeLoopChecker.guarded(classId)`** bounds supertype walks against
  cycles. When a helper both *enters* the guard and *calls another helper that
  re-enters with the same `classId`*, the inner call returns `emptyList()`
  silently (cf. archived 2026-05-08 `findInheritedNestedClass` double-guard fix).
  Hoist the supertype lookup *out* of the guard region instead.
- **`FirJavaClass.directSupertypeClassIds()`** (variant C of
  `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §12 Q1) is the supported
  cross-origin supertype read; the old `getResolvedSupertypeClassIds` callback
  has been deleted.
- **`FirDeclarationOrigin.Java.Source` vs `Java.Library`** — `Java.Source`
  classes have *lazy* `superTypeRefs` (accessing them mid-resolution causes
  premature-resolution cycles); `Java.Library` classes have pre-populated
  `nonEnhancedSuperTypes` and are safe to read. Always distinguish.
- **Constant values must be coerced to the field's declared primitive type**
  in `JavaFieldOverAst.{initializerValue, resolveInitializerValue}` (JLS 5.1
  widening + 5.2 narrowing of constant expressions). PSI's
  `PsiField.computeConstantValue()` does this automatically; java-direct must
  do it explicitly or the JVM IR backend emits malformed bytecode that crashes
  ASM's `Frame.merge` with `NegativeArraySizeException` (cf. archived
  2026-05-11 entry).
- **TYPE_USE annotations on `T[]` return types** must NOT be placed on the
  outer array wrapper's `annotations` list (FIR's `AbstractSignatureParts.kt`
  KT-24392 filter only removes them from *container* annotations, not
  *typeAnnotations*). Place them on the component for varargs, leave the outer
  array wrapper's member annotations empty for non-varargs.

## Binary Class Finder Flag

`-Pkotlin.javaDirect.useBinaryClassFinder=true` switches the binary half of
`CombinedJavaClassFinder` from the legacy PSI finder to the index-based
`BinaryJavaClassFinder` (Phase 1 of the PSI-removal plan in
`implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`). Default is **OFF** in
production; the flag is exercised by the test JVM via the `systemProperty`
passthrough in `compiler/java-direct/build.gradle.kts`.

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
| `implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` | **Authoritative goal-statement** for the public Java-model interface rollback. Read before touching any `core/compiler.common.jvm/.../structure/*` interface or any `JavaTypeOverAst` / `JavaAnnotationOverAst` resolution path. |
| `implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` | Design of `LazySessionAccess`, the `resolvedClassId` hint, `directSupertypeClassIds()`, and the Step 4.5a-c public-interface deletions. |
| `implDocs/MERGED_REFACTORING_PLAN_2026_05_04.md` | PSI removal × resolver unification — Stages 1-4 plan, dependencies, and acceptance criteria. |
| `implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` | Three-phase PSI removal plan; `BinaryJavaClassFinder` design. |
| `implDocs/IJ_FP_REGRESSION_ANALYSIS_2026_05_10.md` | IntelliJ-full-pipeline regression categorisation (Cat A-E). |
| `implDocs/ARCHITECTURE.md` | Callback patterns, key files, JLS implicit rules, common fixes |
| `implDocs/RESOLUTION_PIPELINE.md` | Before any resolution fix |
| `implDocs/INVESTIGATION_TECHNIQUES.md` | Debugging, AST inspection, measurement recipes |
| `ITERATION_RESULTS.md` | Current iteration log (new entries on top) |
| `implDocs/archive/` | Historical iterations, completed plans, measurement data; `ITERATION_RESULTS_2026_05_11.md` is the most recent archive |

---

*Last updated: 2026-05-12 (status refresh post-IJ-FP delta cleanup; framework-wiring caveat added; Critical Patterns section added; reference table extended.)*
