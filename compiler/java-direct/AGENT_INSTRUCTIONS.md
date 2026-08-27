# Java-Direct: Agent Instructions

**Read `AGENT_INSTRUCTIONS_COMMON.md` first** — it holds the module-independent rules
(command discipline, Gradle/tee, comment and writing style, simplification discipline,
docs maintenance). This file holds only what is specific to `compiler/java-direct`.

**Current status**: full box + phased suite green, 2839/2839 (100%) — see
`ITERATION_RESULTS.md` for the authoritative per-suite counts. No known won't-fix.
The module is feature-complete on the `JavaUsingAst*` suite. `compiler/java-direct/src`
is PSI-free and no longer depends on `:compiler:cli` (see
`implDocs/PSI_FREE_ROADMAP.md`); active work is optimization, the platform-free
(NIO) axis, and closing the IJ-FP regression delta. The public Java-model interface
rollback (rule 3 below) and the resolver-unification residue have landed.

> **Caveat on historical numbers.** Before 2026-04-28 the `JavaUsingAst*`
> generators did **not** route `// FILE: *.java` blocks through `java-direct`'s
> AST (they fell through to PSI's `JavaClassFinderImpl`), so any test counts in
> docs/archives dated before 2026-04-28 are against the PSI loader, not
> `java-direct`. See `implDocs/archive/ITERATION_RESULTS_2026_05_11.md`.

**Key files**: `JavaClassOverAst.kt`, `JavaTypeOverAst.kt`, `JavaMemberOverAst.kt`,
`JavaResolutionContext.kt`, `JavaClassFinderOverAstImpl.kt`,
`JavaClassFinderOverBinaryIndex.kt`, `JavaModelSessionAccess.kt`.
Full map in `implDocs/ARCHITECTURE.md`.

**Session temp directory** (see common rules): `export JD_TMP="/tmp/jd_$(date +%Y%m%d_%H%M%S)"`,
then `mkdir -p "$JD_TMP"`. All temp file paths below use `$JD_TMP`.

---

## ⚠ Module-Specific Non-Negotiable Rules

(In addition to the common rules — no chaining, tee, no subagent Gradle, no commits.)

1. **NEVER run `-Pkotlin.test.update.test.data=true`** — corrupts shared test data in
   `compiler/testData/` and `compiler/fir/analysis-tests/testData/`.

2. **NEVER modify test data to make java-direct tests pass** — fix the implementation,
   or document it as a known acceptable difference in `ITERATION_RESULTS.md`.
   Test data files are shared between java-direct and PSI test runners; a diverging
   java-direct result usually means the java-direct implementation is wrong.
   *Rare exception*: tests that depend on JDK-version-specific javac behaviour
   (e.g. user code in `java.util.*` rejected by JDK 17's module seal) may be
   genuinely won't-fix on the java-direct test worker — record them with the
   investigation evidence in the iteration log before declaring won't-fix
   (cf. archived iteration 58 in `ITERATIONS_52_71_DETAILS.md`).

3. **No new public members on Java-model interfaces** in `core/compiler.common.jvm/src/.../load/java/structure/`
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

**FIR terminology**: `simpleImports` / `starImports` (NOT singleType / onDemand).

---

## Test Commands

**Gradle project path is `:compiler:java-direct`.** Test task = `:compiler:java-direct:test`.

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
# top-level callables read across modules. See implDocs/archive/ITERATION_RESULTS_2026_07_13.md, 2026-06-02 entry).
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
   fixed fields. Keep extended traces in a linked `implDocs/` note.

---

## What NOT to Do (module-specific)

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
  - **Why the guard survives lazy annotations (reviewer Q on `JavaCycleBreakerTest`, KT-74097).**
    All Java annotations reachable while `FirJavaClass.declarations` is materialised are now
    deferred — members and enum entries via `FirLazyJavaAnnotationList`, enum-entry deprecations
    additionally via `FirJavaLazyDeprecationsProvider` (`FirJavaFacade.kt`).
    That removed the only known crashing trigger (the `@Deprecated` enum constant in
    `IntelliJFullPipelineTestsGenerated.testIntellij_vcs_git`), so the guard is now genuine
    defense-in-depth — but it is **not** dead code, and the cycle class is still reachable:
    (1) the `declarations` lazy reads `FirJavaClass.typeParameters`, whose bound enhancement calls
    `extractDefaultQualifiers`, which iterates the **class's own** `annotations` (and the outer
    class's, through a raw `getClassLikeSymbolByClassId`) — the same self-referential shape, for any
    generic Java class with an unqualified annotation name; (2) the enum-entry arm still resolves its
    `returnTypeRef` eagerly, as `SignatureEnhancement` requires a `FirResolvedTypeRef` there;
    (3) three of the five `cycleSafeClassLikeSymbol` call sites involve no annotation at all
    (const-field values, `@Target`/TYPE_USE lookup, type-argument substitution) — the guard protects
    the chokepoint, not one annotation path. Making the `declarations` lazy stop reading
    `typeParameters`, and the enum-entry `returnTypeRef` lazy, are **upstream follow-ups under
    KT-74097** (shared `fir-jvm`, need both regression gates), not replacements for the guard.
- **`cycleGuardedSupertypeWalk(classId, default) { ... }`** (`JavaModelSessionAccess.kt`,
  backed by `JavaModelSupertypeWalkGuard`) bounds supertype walks against cycles.
  When a helper both *enters* the guard and *calls another helper that
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
| `AGENT_INSTRUCTIONS_COMMON.md` | Module-independent rules: shell/Gradle discipline, comment & writing style, simplification discipline, docs maintenance. Read every session. |
| `implDocs/PSI_FREE_ROADMAP.md` | The PSI-free / platform-free axes: what landed, the seams introduced, and the remaining platform-bound list. Read before touching the binary path or the module's dependencies. |
| `implDocs/IJ_FP_REGRESSION_ANALYSIS_2026_05_10.md` | IntelliJ-full-pipeline regression categorisation (Cat A-E). **The tracked next step** — but re-baseline first: its code references are stale (see the doc's status banner). |
| `implDocs/ARCHITECTURE.md` | Callback patterns, key files, JLS implicit rules, common fixes. |
| `implDocs/RESOLUTION_PIPELINE.md` | Before any resolution fix. |
| `implDocs/RESOLUTION_SCHEMA.md` | Structural map of the `resolution/` package — entities and scenarios; companion to `RESOLUTION_PIPELINE.md`. |
| `implDocs/PERFORMANCE_REVIEW_2026_07_20.md` | Performance review — landed low-risk fixes and the riskier follow-up candidates. |
| `implDocs/PARSING_IMPROVEMENTS.md` | Parsing-pipeline improvement backlog (analysis only, unimplemented). |
| `implDocs/BINARY_CLASS_CACHE_LIFETIME.md` | Open design question: binary Java class caches outliving one compilation (keys, memory, BTA plug-in point). |
| `implDocs/CLASS_FILE_READ_LAYER.md` | Open design question: what replaces `KotlinBinaryClassCache` — one class-file read layer shared by the Kotlin and Java binary stacks. Trace of the current uses + four approaches. |
| `implDocs/TEST_INFRA_JAVA_DIRECT.md` | java-direct in the shared (non-CLI) test infrastructure: the forced-on experiment and its per-suite results, the fixed `dependsOn`-closure Java source-root gap, and the two blockers before a facade-based suite can run java-direct. |
| `implDocs/RAW_TYPE_ARGUMENT_UNIFICATION_2026_08_27.md` | Why `typeArguments` carries `null` for a parameter it supplies no argument for, why `isRaw` is derived from that, and what `firBackedJavaType`'s `declarationChainRoot` is for. **Read before touching `computeTypeArguments` / `isRaw` / `firBackedJavaType`.** |
| `implDocs/IMPLICIT_OUTER_TYPE_ARGUMENTS_REVIEW_DECISIONS_2026_08_26.md` | The earlier round on the same code: the per-outer-class scope check and the `classId` fallback (§§1–3, still current). Its §§4–5 are superseded by the note above. |
| `implDocs/INVESTIGATION_TECHNIQUES.md` | Debugging, AST inspection, measurement recipes. |
| `ITERATION_RESULTS.md` | Current iteration log — template + brevity rules; new entries on top. |
| `implDocs/archive/` | Historical iterations and **landed** design docs: the merged refactoring plan (PSI removal × resolver unification), the interface-rollback inventory, the FIRSESSION-injection proposal, the JTC / TYPE_USE / `fir-jvm` cleanups, the resolution-pipeline collapse, the model-side outer-arg recovery, the `review.md` responses, and per-iteration logs. `ITERATION_RESULTS_2026_08_26.md` is the most recent log archive. |

---

*Last updated: 2026-08-26 (docs pass at branch wrap-up: split the module-independent rules —
shell discipline, ground rules, Source Comment Conventions, the new Explanation & Writing
Style section, Simplification & Review Discipline, Docs Maintenance — into
`AGENT_INSTRUCTIONS_COMMON.md`; the comment/writing rules there were rewritten from the
branch's session-log complaints and a style comparison against human-written modules.
Archived `MERGED_REFACTORING_PLAN_2026_05_04.md` (fully landed) and the iteration log →
`implDocs/archive/ITERATION_RESULTS_2026_08_26.md`; trimmed `ReadMe.md` to the human module
baseline. Earlier update history is in git.)*
