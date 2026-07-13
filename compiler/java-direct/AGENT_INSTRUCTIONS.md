# Java-Direct: Agent Instructions

**Current status**: 1178/1178 box + 1513/1513 phased (2793/2793, 100%). No
known won't-fix.
The module is feature-complete on the `JavaUsingAst*` suite. Active work is
optimization, **PSI-removal Phase 3** (source-only PSI/AST switch — see
`implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`), and closing the IJ-FP
regression delta. The public Java-model interface rollback (rule 7) and the
resolver-unification residue have landed.

> **Caveat on historical numbers.** Before 2026-04-28 the `JavaUsingAst*`
> generators did **not** route `// FILE: *.java` blocks through `java-direct`'s
> AST (they fell through to PSI's `JavaClassFinderImpl`), so any test counts in
> docs/archives dated before 2026-04-28 are against the PSI loader, not
> `java-direct`. See `implDocs/archive/ITERATION_RESULTS_2026_05_11.md`.

**Key files**: `JavaClassOverAst.kt`, `JavaTypeOverAst.kt`, `JavaMemberOverAst.kt`,
`JavaResolutionContext.kt`, `JavaClassFinderOverAstImpl.kt`,
`JvmBinaryClassFinderInputsOverIndex.kt`, `JavaModelSessionAccess.kt`,
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
   The rollback has **landed** (the public interface now matches the pre-`java-direct`
   shape); the rule remains a standing constraint. Historical inventory:
   `implDocs/archive/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md`.

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

**Gradle project path is `:compiler:java-direct`.** Test task = `:compiler:java-direct:test`.

**Do not pass `--rerun-tasks` or `--no-build-cache`** on routine test runs — they force full rebuilds even when nothing changed. Gradle's input-change detection reruns the `test` task automatically when sources, test data, or test code change. Use `--rerun` (test-task-only) when you need to re-execute a test whose inputs did not change (e.g. checking flakiness, or after externally clearing results). Use `--rerun-tasks` only for measurement runs where a clean execution is required for valid timing.

```bash
# Both suites together (~2793 tests) — preferred for verification
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --tests "JavaUsingAstBoxTestGenerated" --stacktrace 2>&1 | tee "$JD_TMP/jd_test.txt"

# Box tests only (~1178)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated" --stacktrace 2>&1 | tee "$JD_TMP/jdb_test.txt"

# Phased/diagnostic tests only (~1513)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --stacktrace 2>&1 | tee "$JD_TMP/jdp_test.txt"

# Unit tests (MUST stay green)
./gradlew :compiler:java-direct:test --tests "JavaParsingTest" --stacktrace -q

# Single test (use --rerun if re-executing a test whose inputs are unchanged)
./gradlew :compiler:java-direct:test --tests "*JavaUsingAstBoxTestGenerated.*testSpecificName*" --stacktrace -q --rerun 2>&1 | tee "$JD_TMP/single_test.txt"

# PSI regression (only after shared FIR file or test data changes)
./gradlew :compiler:fir:analysis-tests:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" --stacktrace -q 2>&1 | tee "$JD_TMP/psi_test.txt"

# Cross-module bytecode-vs-source regression (MUST run after any edit to shared FIR JVM files
# below — covers cases the PSI gate above does not, e.g. `@file:JvmPackageName`-shifted
# top-level callables read across modules. See ITERATION_RESULTS.md 2026-06-02 entry).
./gradlew :compiler:fir:fir2ir:test --tests "*FirLightTreeBlackBoxCodegenTestGenerated*CompileKotlinAgainstKotlin*" --stacktrace -q 2>&1 | tee "$JD_TMP/ckk_test.txt"
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
- `compiler/fir/fir-jvm/src/.../JavaSymbolProvider.kt`
- `compiler/fir/fir-jvm/src/.../deserialization/JvmClassFileBasedSymbolProvider.kt`
- `compiler/fir/fir-jvm/src/.../deserialization/JvmBinaryClassFinderInputs.kt`
- `compiler/fir/entrypoint/src/.../session/FirJvmSessionFactory.kt`
- `compiler/cli/cli-jvm/src/.../pipeline/jvm/JvmFrontendPipelinePhase.kt`
- `core/compiler.common.jvm/src/.../load/java/structure/*.kt`

For any edit to these: always compare with upstream first —
```bash
git show origin/master:<path> | grep -A10 "relevantFunction"
```
Then run BOTH regression gates (PSI **and** `CompileKotlinAgainstKotlin`) BEFORE and AFTER
the change. If new failures appear in either gate, **revert immediately**. The
`CompileKotlinAgainstKotlin` gate is the only routine suite that exercises
cross-module bytecode-vs-source resolution (`@file:JvmPackageName` shifting,
`MULTIFILE_CLASS_PART`, etc.) — the PSI gate alone is insufficient for any edit
to the deserializer / library-session wiring path.

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
5. **Document** — append a short entry to `ITERATION_RESULTS.md` using the template's
   fixed fields (see *Docs Maintenance*). Keep extended traces in a linked `implDocs/` note.

---

## What NOT to Do

- Don't rerun Gradle for a different view of results — grep the saved log file.
- Don't chain shell commands with `&&` — one command per tool call.
- Don't let subagents run Gradle.
- Don't use `--info`/`--debug` unless specifically necessary.
- Don't pass `--rerun-tasks` or `--no-build-cache` on routine test runs — they discard valid cache hits and make every run a full rebuild. Trust Gradle's up-to-date check; use `--rerun` on the test task if you genuinely need to re-execute unchanged tests.
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
  funnel every probe through `FirSession.cycleSafeClassLikeSymbol` (the builtins-filtered
  class-existence probe `tryResolve` in `JavaTypeResolver.kt` is layered directly on top of it).
  - **Why the guard and not "just make the annotations lazy" (reviewer Q on `JavaCycleBreakerTest`,
    KT-74097).** A recurring reviewer suggestion is to stop resolving annotations eagerly while
    `FirJavaClass.declarations` is materialised — "we already have `FirLazyJavaAnnotationList` for
    that." The hint correctly names the *trigger*: regular Java members (`FirJavaField` /
    `FirJavaMethod`, params, type params) already defer annotations via `FirLazyJavaAnnotationList`,
    but the **enum-entry arm** of `convertJavaFieldToFir` (`buildEnumEntry` in `FirJavaFacade.kt`)
    resolves them *eagerly* (`setAnnotationsFromJava` + `replaceDeprecationsProvider(... getDeprecationsProviderFromAnnotations ...)`),
    which re-resolves the very in-flight `ClassId` (the `@Deprecated` enum constant in the
    `IntelliJFullPipelineTestsGenerated.testIntellij_vcs_git` reproducer) → the self-cycle. It is
    **not** a local `java-direct` fix, for three reasons: (1) `FirLazyJavaAnnotationList` is a
    Java-declaration-specific slot (`FirJavaField`/`FirJavaMethod`); a `FirEnumEntry` is a generic
    `FirVariable` whose `annotations` are set eagerly via `replaceAnnotations`, so there is no
    drop-in lazy slot — it would need a new lazy mechanism or a Java-specific enum-entry node;
    (2) `convertJavaFieldToFir` / `buildEnumEntry` live in the shared `fir-jvm` module, so switching
    enum-entry annotations to lazy is a compiler-wide change with ordering knock-ons (deprecation
    info is forced right there and is often needed early); (3) even if applied, laziness removes only
    *this* trigger, not the cycle class — the guard protects the resolution chokepoint itself and
    bounds re-entrant probes from *any* path, whereas laziness merely defers the same self-referential
    lookup to whenever the annotation is finally forced. So the `cycleSafeClassLikeSymbol` guard stays
    as the self-contained, defense-in-depth fix; making enum entries defer annotations like other Java
    members (or fixing the PUBLICATION-lazy re-entrance itself) is an **upstream follow-up under
    KT-74097**, not a replacement for the guard.
- **`JavaSupertypeLoopChecker.guarded(classId)`** bounds supertype walks against
  cycles. When a helper both *enters* the guard and *calls another helper that
  re-enters with the same `classId`*, the inner call returns `emptyList()`
  silently (cf. archived 2026-05-08 `findInheritedNestedClass` double-guard fix).
  Hoist the supertype lookup *out* of the guard region instead.
- **`FirJavaClass.directSupertypeClassIds()`** (variant C of
  `implDocs/archive/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §12 Q1) is the supported
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

## Simplification Discipline (justifying complexity, not defending it)

This module's resolution code went through many rounds where a split pipeline, an
overload, or a lambda parameter was defended as "necessary" — and turned out, after
the user pushed back two or three times, to exist only for a unit test's convenience
or to rest on a comment's reasoning that a later refactor had already invalidated.
The goal below is to reach the same simplified end state in one pass instead of
several.

- **Default to one generic path.** When the same operation (e.g. resolving a name,
  walking a hierarchy) is implemented separately per representation/origin (source
  vs. binary vs. Kotlin, same-file vs. cross-file), treat that split as a hypothesis
  to disprove, not a given. Actively look for the one existing generic mechanism
  (a callback-parameterized walk, a shared adapter) that the specialized arms could
  route through instead of writing a new one.
- **"A unit test injects a fake here" is not a production justification.** If a
  function's extra parameter, overload, or lambda exists only so a test can pass in
  a fake/stub, the fix is to change the test, not to keep the parameter: write an
  end-to-end/integration test that exercises the real production wiring (a real
  finder, a real session, a real AST), or add a narrow test double at the correct
  architectural boundary. Before answering "we need this for testing," check whether
  a same-shape end-to-end test already exists elsewhere in the module and can be
  copied.
- **Re-derive, don't recite.** A comment or a verbal justification for why some
  complexity is "necessary" or "load-bearing" must be re-verified against the
  *current* code on every review pass — trace the actual current call sites and data
  flow again — rather than restated from an earlier round's reasoning, which may
  already be stale after intervening refactors (this happened more than once this
  session: a documented split's real reason had silently changed after a merge).
- **Answer capability questions with evidence, not assumption.** "Is this tested?",
  "is this reachable?", "does this detect ambiguity?", "is this parameter used?" must
  be answered by actually grepping call sites / running the suspect scenario, not by
  inference from the code's shape. If no test demonstrates a claimed hazard, either
  add one immediately in the same pass, or treat the claim as unproven and prefer the
  simpler design.
- **Treat a repeated "are we sure?" from the user as a cue to re-investigate, not to
  restate the previous answer.** If the same question comes back a second time, that
  is a signal the first investigation was insufficient — redo it from scratch with a
  concrete test or a concrete reachable call path as the outcome, don't re-justify
  the status quo with the same argument.
- **Healthy resistance is still expected — but only backed by evidence.** Push back
  on a proposed simplification when you can produce, within the same investigation
  pass, either a concrete regression test that fails without the extra complexity, or
  a specific reachable call path/cycle that it guards against. If neither can be
  produced, implement the simplification rather than defending the status quo on
  hypothetical grounds.
- **Prefer collapsing multi-parameter overloads with exactly one production caller.**
  When a function has a generic, lambda-parameterized form used by only one call
  site (the rest being test-only), fold it into a single context-bound function and
  replace the lost test flexibility with an integration test against the real path —
  mirrors this session's collapse of `resolveInheritedInnerClassToClassId` and
  `walkSupertypeClassIds` down to their `JavaResolutionContext`-bound, parameter-free
  production shape.

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
| `implDocs/MERGED_REFACTORING_PLAN_2026_05_04.md` | PSI removal × resolver unification — Stages 1-4 plan, dependencies, and acceptance criteria. |
| `implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` | Three-phase PSI removal plan; Phases 1-2 landed, **Phase 3** (source-only PSI/AST switch) is the next effort. |
| `implDocs/IJ_FP_REGRESSION_ANALYSIS_2026_05_10.md` | IntelliJ-full-pipeline regression categorisation (Cat A-E). **The tracked next step** — but re-baseline first: its code references are stale (see the doc's status banner). |
| `implDocs/ARCHITECTURE.md` | Callback patterns, key files, JLS implicit rules, common fixes. |
| `implDocs/RESOLUTION_PIPELINE.md` | Before any resolution fix. |
| `implDocs/INVESTIGATION_TECHNIQUES.md` | Debugging, AST inspection, measurement recipes. |
| `ITERATION_RESULTS.md` | Current iteration log — template + brevity rules; new entries on top. |
| `implDocs/archive/` | Historical iterations and **landed** design docs: the interface-rollback inventory, the FIRSESSION-injection proposal, the JTC / TYPE_USE / `fir-jvm` cleanups, the resolution-pipeline collapse, the model-side outer-arg recovery, the `review.md` responses, and per-iteration logs. `ITERATION_RESULTS_2026_07_13.md` is the most recent log archive. |

---

## Source Comment Conventions

Comments in `compiler/java-direct/src/` are reviewed alongside the code. Write them
for a future reader of the **merged** module, not as a development journal — this avoids
a recurring cleanup pass before review. Apply these rules when adding or editing any
comment or KDoc:

- **No references to `implDocs/` docs.** They are transient and must never be mentioned
  in source comments — not by filename, not by section number (`§6.x`), not by stage/phase
  label (`Stage 2`, `Phase 3`, `pre-§6.5`). Put the explanation itself in the comment.
- **Describe the current state only.** The module is unmerged, so comments must not narrate
  past or superseded attempts ("used to live behind…", "the old first-segment shortcut",
  "before the … cleanup", "now deleted `BinaryJavaClassFinder`", dated history). Drop the
  history; keep what is true today.
- **Avoid `javac-wrapper` / `TreeBased*` references** (the module is obsolete and being
  removed). Keep only genuinely useful `javac` / PSI / JLS parity notes that aid understanding.
- **Don't restate what a one-level usages search reveals** (callers, single call sites, line
  numbers in other files) unless it is essential for understanding.
- **Don't duplicate comments on declaration and use sites.** Prefer a declaration-site comment;
  if the use site needs a note, keep it to a short cross-reference rather than repeating the
  full explanation.
- **Prefer bulleted lists over prose** for multi-point explanatory comments; omit trivial
  information and introductory filler sentences.

---

## Docs Maintenance

Keep the working doc set small — these files are read into context every session.

- **`ITERATION_RESULTS.md` is append-only and short.** New entry on top, using the
  template's fixed fields (`Change` / `Files` / `Tests` / `Result`). Cap each entry at
  ~15 lines / ~150 words; long rationale, traces, or measurement tables go into a
  dedicated `implDocs/<TOPIC>.md` and are linked, never inlined. No pasted logs/diffs.
- **Archive the log when it passes ~600 lines.** `git mv` it to
  `implDocs/archive/ITERATION_RESULTS_<last-entry-date>.md`, add an archive banner
  (Archive Date / Coverage / Result / warning), then reset `ITERATION_RESULTS.md` to its
  template.
- **Archive an `implDocs/` doc once its refactoring has fully landed or been superseded.**
  Move it to `implDocs/archive/` and repoint any references here. Keep only living
  references (`ARCHITECTURE`, `RESOLUTION_PIPELINE`, `INVESTIGATION_TECHNIQUES`) and docs
  for *active* work in `implDocs/`.

---

*Last updated: 2026-07-13 (docs cleanup: archived the iteration log →
`implDocs/archive/ITERATION_RESULTS_2026_07_13.md` and reset the live log to its template;
moved the landed `COLLAPSE_RESOLUTION_PIPELINES_2026_07_06`, `MODEL_SIDE_OUTER_ARG_RECOVERY_2026_06_10`
and `REVIEW_MD_RESPONSES_2026_07_08` docs (plus the raw `review.md` and the resolved `r*_3_*`
review rounds) into the archive with banners; repointed the one living reference in
`RESOLUTION_SCHEMA.md`; flagged `IJ_FP_REGRESSION_ANALYSIS_2026_05_10.md` as the active next step
needing re-baselining.)*

*Previously: 2026-07-08 (added the Simplification Discipline section: default to one
generic path over per-representation splits, treat "a unit test needs a fake" as a code
smell rather than a justification, re-verify complexity claims against current code every
pass instead of reciting earlier reasoning, and back pushback on simplification with a
concrete test or call path or else implement it — distilled from repeated
push-collapse-push cycles on the resolver-unification work.)*

*Previously: 2026-06-15 (added the Source Comment Conventions section so review-ready
comment style — no `implDocs`/stage references, current-state only, no `javac-wrapper`
mentions, no decl/use-site duplication, bullets over prose — is the default and needs no
cleanup pass before review.)*

*Previously: 2026-06-09 (docs cleanup: archived `ITERATION_RESULTS` →
`implDocs/archive/ITERATION_RESULTS_2026_06_01.md` and reset the log to a structured,
capped template; moved 10 landed/superseded design docs — incl. the interface-rollback
inventory and FIRSESSION-injection proposal — into `implDocs/archive/`; removed the
obsolete Binary-Class-Finder-Flag section; added this Docs Maintenance section.)*
