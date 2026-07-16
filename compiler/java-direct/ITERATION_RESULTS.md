# Java-Direct: Iteration Results Log

**Current status**: `:compiler:java-direct:test` full suite green, 2839/2839 (100%). No known won't-fix.

**Last archived**: `implDocs/archive/ITERATION_RESULTS_2026_07_13.md` (entries through 2026-07-13).

---

## How to write entries

This log is read into the agent's context every session, so **entries must stay short**.

- **Newest entry on top.** One entry per landed change or per investigated regression.
- **Cap each entry at ~15 lines / ~150 words.** If the rationale, a trace, or a
  measurement table is longer, put it in a dedicated `implDocs/<TOPIC>.md` and link to it
  from the entry — do not inline it here.
- **Use the fixed fields below.** No free-form multi-paragraph narration; if a field needs
  more than ~2 lines, link out instead.
- **No pasted logs, stacktraces, or diffs.** Quote the single line that matters; link the rest.
- **Archive when this file passes ~600 lines** (see `AGENT_INSTRUCTIONS.md` →
  "Docs Maintenance"): `git mv` it to
  `implDocs/archive/ITERATION_RESULTS_<last-entry-date>.md`, add an archive banner, and
  reset this file to the template below.

### Entry template

```
### YYYY-MM-DD — <one-line title>
- **Change**: what changed and why (1–3 lines).
- **Files**: key files touched (+N/−M LoC if useful).
- **Tests**: suites run + counts (e.g. box 1178/1178, phased 1513/1513).
- **Result**: green / regression fixed / won't-fix — link to a detail doc if there is one.
```

---

<!-- Add new entries below, newest first. -->

### 2026-07-16 — Rewrite `resolveQualifiedNameToClassIdFromParts` as a left-to-right JLS 6.5.4 pass
- **Change**: replaced the recursive try-every-split loop (outer-prefix enumeration + per-prefix
  recursion, O(n²) probes) with a single non-recursive left-to-right pass that mirrors javac's
  PackageOrTypeName classification: first segment as a simple type name in scope (JLS 6.5.4.1),
  else grow the package prefix until a segment names a top-level type (JLS 6.5.4.2), then a
  member-type descent probing declared-then-inherited at every segment (JLS 6.5.5.2). The loose
  `probeFqnSplits` fallback survives, but is now reached only when the JLS pass fails — the sole
  divergence from javac remains the package/type name clash pinned by
  `qualifiedNamePackageClassClash.kt` (KT-87813).
- **Files**: `resolution/JavaTypeResolver.kt` (−36/+28); Scenario D refreshed in `ReadMe.md` and
  `implDocs/RESOLUTION_SCHEMA.md`.
- **Tests**: box+phased suite green (0 FAILED); `JavaParsing*` unit tests green.
- **Result**: simplification landed (behavior-preserving on the whole suite).

### 2026-07-15 — Drop redundant `JavaToKotlinClassMap` disjunct in `resolveFromJavaLang`
- **Change**: `resolveFromJavaLang` accepted a name when either `JavaToKotlinClassMap.mapJavaToKotlin`
  hit **or** `classExists` was true; the map disjunct is dead. It only ever probes `ClassId(java.lang, X)`,
  and `classExists` resolves those via the symbol provider whose JVM builtins arm answers only `kotlin.*`
  ids (`StandardClassIds.builtInsPackages`), so a `java.lang.*` lookup never returns a `BuiltIns`-origin
  symbol filtered by `tryResolve` — it hits the JDK library class instead. Every mapped `java.lang` fqName
  (Object/String/Number/CharSequence/Comparable/Throwable/Cloneable/Iterable/Enum/wrappers/Deprecated)
  exists in both full JDK and mockJDK, so `classExists` already covers exactly what the map would.
  Collapsed to `classExists(classId, fullResolution)`, matching `resolveFromSamePackage`; better PSI
  parity (no divergence in the no-JDK case). Removed the now-unused `JavaToKotlinClassMap` import.
- **Files**: `resolution/JavaTypeResolver.kt` (−5/+1).
- **Tests**: box+phased suite green (0 FAILED/0 errors).
- **Result**: simplification landed (reviewer question — `classExists` alone suffices).

### 2026-07-15 — Nameless Java method recovered as a constructor (`testNamelessInJava`)
- **Change**: `JavaClassOverAst.constructors` required a constructor `METHOD` node to have both no
  return `TYPE` **and** an `IDENTIFIER`, so a malformed nameless declaration like `void () {}`
  (its `void` is an error element, not a return type) was dropped; with no explicit constructor a
  public default constructor was synthesized, so `class K : Nameless()` saw a visible constructor and
  produced no diagnostic. PSI (same syntax parser) treats any no-return-type method as a
  (package-private) constructor, suppressing the default one → `INVISIBLE_REFERENCE`. Dropped the
  `IDENTIFIER` requirement so constructor detection mirrors PSI's `getReturnTypeElement() == null`.
- **Files**: `model/JavaClassOverAst.kt` (constructors filter). Test data unchanged (shared golden).
- **Tests**: full box+phased suite 2839/2839 (0 FAILED); `JavaParsingTest`/`JavaLightTreeTest` green.
- **Result**: regression fixed (new test from master merge; SDK 261 golden).

### 2026-07-14 — Skip inaccessible inherited nested classes (IJ-FP regression: `testIntellij_exceptionAnalyzer`)
- **Change**: `walkSupertypeClassIds` accepted the first inherited nested class of a matching simple
  name regardless of accessibility, so a package-private nested type in a supertype from another
  package (e.g. `SimpleColoredComponent.TextRenderer`) wrongly shadowed a same-named top-level class,
  producing a spurious `RETURN_TYPE_MISMATCH`. Now filtered by JLS 6.6/8.2 accessibility: `private`
  never inherited, package-private only within the declaring package; inaccessible matches expand
  deeper instead of resolving. Visibility read cycle-safely via new `FirJavaClass.nonEnhancedVisibility`
  (from `originalStatus`, no lazy `status`) surfaced through `FirBackedJavaClassAdapter.visibility`.
- **Files**: `resolution/JavaInheritedClassResolver.kt`, `resolution/FirBackedJavaClassAdapter.kt`,
  `fir-jvm/.../FirJavaClass.kt` (+`nonEnhancedVisibility`); test
  `codegen/box/javaDirect/packagePrivateInheritedNestedClassNotVisibleAcrossPackages.kt`.
- **Tests**: box 1179/1179, phased 1513/1513 (0 FAILED); `IntelliJFullPipelineTestsGenerated.testIntellij_exceptionAnalyzer`
  green under java-direct; PSI + CompileKotlinAgainstKotlin gates green (shared `FirJavaClass` edit).
- **Result**: regression fixed.

### 2026-07-13 — Memoize `JavaClassOverAst.supertypes` (IJ-FP regression)
- **Change**: `supertypes` was a recomputing `get()` that returned fresh `JavaClassifierTypeOverAst`
  instances on every read; FIR forces it from two lazy slots per class (`FirJavaClass.superTypeRefs`
  enhancement and `directSupertypeClassIdsCache`), so each supertype's `classifier` (a per-instance
  lazy hitting the symbol provider) resolved from cold twice. Made it `by lazy(PUBLICATION)` so both
  reads share instances and each supertype resolves once. List build allocates wrappers only → still
  resolution-safe.
- **Files**: `model/JavaClassOverAst.kt` (supertypes get()→by lazy).
- **Tests**: box 1178/1178, phased 1513/1513 (0 FAILED).
- **Result**: regression fixed. IntelliJ `testIntellij_platform_ide_impl` warm frontend (isolated bench,
  4 iters, same build): java-direct 21.5s wall / 20.8s CPU vs legacy 25.3s / 23.6s — was ~+8% before.
