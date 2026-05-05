# The `TYPE_USE` Problem in `java-direct` — 2026-05-04

> **Scope.** This document explains the "`TYPE_USE` problem" as it appears in
> `java-direct`: what it is, why the code looks the way it does today, why the current
> shape cannot be changed without a complete overhaul of the `java-direct` architecture,
> and which simplifications become possible — separately — under the resolver-unification
> track and under the PSI-removal track. It is a consolidated retrospective; the per-iteration
> archaeology lives in `implDocs/archive/`.
>
> See also: [`ARCHITECTURE.md`](ARCHITECTURE.md),
> [`RESOLUTION_PIPELINE.md`](RESOLUTION_PIPELINE.md),
> [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md),
> [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md),
> [`MERGED_REFACTORING_PLAN_2026_05_04.md`](MERGED_REFACTORING_PLAN_2026_05_04.md),
> [`archive/ITERATIONS_7_16_DETAILS.md`](archive/ITERATIONS_7_16_DETAILS.md),
> [`archive/ITERATIONS_17_23_DETAILS.md`](archive/ITERATIONS_17_23_DETAILS.md),
> [`archive/FAILURE_ANALYSIS_ITERATION_22.md`](archive/FAILURE_ANALYSIS_ITERATION_22.md),
> [`archive/ITERATIONS_37_51_DETAILS.md`](archive/ITERATIONS_37_51_DETAILS.md).

---

## 1. The problem statement

Java distinguishes annotation **targets** (`ElementType.TYPE_USE`, `ElementType.METHOD`,
`ElementType.FIELD`, …). The same syntactic position — for example the return type of a
method — can carry annotations that semantically belong to *the type* (TYPE_USE: e.g.
`@org.jetbrains.annotations.NotNull` on the return type) and annotations that belong to
*the declaration* (e.g. `@Override` on the method). The Kotlin frontend has to:

1. Build `FirJavaTypeRef` / `ConeAttributes` from only the type-position annotations.
2. Build the declaration's annotation list from only the declaration-position annotations.
3. Never double-attach an annotation, never drop a legitimate one.

To do that, the compiler must, **for each annotation occurrence**, decide whether the
annotation class itself is `@Target(TYPE_USE)`. That decision is what this document calls
the **TYPE_USE problem**: it requires resolving the annotation class — which can come
from anywhere (JDK, classpath binary, Kotlin source, the same Java source set being
compiled) — to inspect its declared `@Target`.

The PSI and javac-wrapper backends solve this at **structure-build time**: each has its
own class resolver (PSI's reference machinery; javac's symbol table) and can pre-filter
annotations into the correct buckets before FIR ever sees them
(`compiler/javac-wrapper/.../utils.kt` `filterTypeAnnotations` calls
`JavaAnnotation.resolve()` synchronously and reads `@Target`).

`java-direct` cannot do this, and that is the entire problem.

## 2. The current shape of the solution

The handling is split across three layers, with the actual *filter* on the model side and
the *predicate that needs FIR* on the FIR side, behind a perf gate.

| Layer | Role | Concrete API |
|---|---|---|
| Model (`core/compiler.common.jvm`) | Decides *which* annotations are filtered (per syntactic position) | `JavaType.filterTypeUseAnnotations(isTypeUse)` |
| Model (`java-direct`) | Owns the position split (extra / modifierList / direct / member) | `JavaTypeOverAst.filterTypeUseAnnotations` |
| FIR (`fir-jvm`) | Supplies the predicate that needs `session.symbolProvider` | `isTypeUseAnnotationClass(fqName, session)` |
| FIR call site | Gated by `needsTypeUseAnnotationFiltering` to keep the PSI hot path zero‑cost | `JavaTypeConversion.toFirJavaTypeRef`, `toConeTypeProjection` |

So:

- The **filtering itself** lives in the model layer (`filterTypeUseAnnotations` is a
  member of `JavaType`).
- The **predicate** the model needs (`is this `ClassId`/FQN actually a TYPE_USE
  annotation class?`) lives in FIR, because answering it requires
  `session.symbolProvider`.
- A Boolean **perf gate** `needsTypeUseAnnotationFiltering` (default `false`,
  java-direct overrides to `true`) keeps the PSI default path closure-free.

This is the same pattern as every other model→symbol bridge in this subsystem:

| Concern | Model side | FIR side (callback) |
|---|---|---|
| Resolve a classifier reference | `JavaClassifierType.resolve(tryResolve)` | classifier resolution that consults `symbolProvider` |
| Resolve an annotation class | `JavaAnnotation.resolveAnnotation(tryResolve)` | same, plus `@Target` etc. |
| **Filter TYPE_USE annotations** | `JavaType.filterTypeUseAnnotations(isTypeUse)` | `isTypeUseAnnotationClass(fqName, session)` |
| Read a constant field initializer | `JavaField.resolveInitializerValue(...)` | `resolveExternalFieldValue(...)` |

TYPE_USE filtering is the fourth instance of one architectural rule, not a special case.

## 3. Why it landed in this form

Three iterations decided the current shape, all preserved in `implDocs/archive/`:

1. **Iter 15** ([`archive/ITERATIONS_7_16_DETAILS.md`](archive/ITERATIONS_7_16_DETAILS.md) §Iter 15).
   First attempt was a **hardcoded blocklist** of non-TYPE_USE names (`@Override`,
   `@Deprecated`, …). Explicitly rejected as unmaintainable: every new annotation in the
   ecosystem would silently get the wrong placement.

2. **Iter 22** ([`archive/FAILURE_ANALYSIS_ITERATION_22.md`](archive/FAILURE_ANALYSIS_ITERATION_22.md) §Category 1;
   [`archive/ITERATIONS_17_23_DETAILS.md`](archive/ITERATIONS_17_23_DETAILS.md) §Iter 22).
   Switched to the callback pattern. The failure analysis is explicit
   (lines 92–98) about why pushing the **filtering itself** into FIR was wrong:
   > *Failed approach #2 (FIR-level filtering): Adding filtering directly in shared FIR
   > code (`JavaTypeConversion.kt`) caused regressions in PSI-based tests because:
   > javac-wrapper already filters at Java structure level; adding FIR-level filtering
   > double-filtered and broke expected behavior.*

   This is the load-bearing architectural commitment: PSI and javac-wrapper **pre-filter
   at structure-build time**. java-direct has no symbol table of its own — only
   `session.symbolProvider` — so it cannot pre-filter at structure-build time. The
   callback bridges the gap.

3. **Iter 43–44** ([`archive/ITERATIONS_37_51_DETAILS.md`](archive/ITERATIONS_37_51_DETAILS.md)).
   Refined the model-level half: annotations were split into "type-position annotations"
   (TYPE_USE by syntactic position, returned unconditionally) and `memberAnnotations`
   (must run through the predicate). This is the current shape of
   `JavaTypeOverAst.filterTypeUseAnnotations`. Iter 43 also added the FQN/`ClassId`
   verification step inside `isTypeUseAnnotationClass` to fix a star-imported
   `org.jetbrains.annotations.NotNull` matching by simple name in the binary class
   finder.

The perf gate is more recent: `ITERATION_RESULTS.md` (~lines 565–700) records a measured
**≈+5 % / +11 s** regression on `KotlinFullPipelineTestsGenerated` (414 modules) traced to
the closure allocation in
`filterTypeUseAnnotations { fqName -> isTypeUseAnnotationClass(fqName, session) }` firing
once per Java type-ref during enhancement. The Boolean gate (default `false`,
java-direct overrides to `true`) was added to keep the PSI default path closure-free.

## 4. Why the current form is justified by current invariants

Every structural piece is load-bearing. Each is anchored to an architectural rule that
the project has not chosen to relax.

1. **Architecture Decision #1** ([`ARCHITECTURE.md`](ARCHITECTURE.md) line 11):
   > *Java Model provides names, FIR resolves them via `session.symbolProvider`. **No
   > `FirSession` access in Java Model**.*

   The predicate cannot move into the model layer without breaching this rule. The model
   layer is in `core/compiler.common.jvm`, has zero FIR dependencies, and is shared with
   K1 descriptors and other clients.

2. **The PSI / javac-wrapper double-filter problem is intrinsic.** PSI and javac-wrapper
   resolve the annotation class at structure-build time with their own machinery. Pushing
   the filter into shared FIR code would re-filter inputs that are already filtered,
   dropping legitimate annotations. The default impl
   `fun filterTypeUseAnnotations(...) = annotations` is the only correct behavior for
   those impls — and Iter 22 has the test failures to prove it.

3. **The performance gate is empirically grounded.** The ≈+5 % regression on a
   multi-module corpus was attributed to the closure cost alone; removing the flag
   without removing the closure cost would re-introduce the regression.

4. **The predicate must consult `@Target` of an arbitrary class** that can be (a) Java
   source parsed by java-direct, (b) Kotlin source, (c) binary `.class` on the classpath,
   (d) a builtin. The only uniform resolver in java-direct's world is
   `session.symbolProvider`. Anything else requires re-introducing javac-style symbol
   tables, which is exactly what `java-direct` exists to avoid.

5. **The FQN/`ClassId` verification step** inside `isTypeUseAnnotationClass`
   (`if (symbol.classId != classId) return false`) is also load-bearing — see Iter 43,
   which fixed the star-imported `@NotNull` matching by simple name in the binary class
   finder. Removing this guard re-opens that bug.

## 5. Why it cannot be changed without an architectural overhaul

The user's question — "should TYPE_USE handling be pushed to the model level?" — surfaces
recurringly. The answer is no, and the reason is structural rather than stylistic.

The framing "`java-direct` handles its own annotations, FIR is asked only for JDK / library
ones" is **wrong**. The split is not "external" vs "internal":

- A `JavaAnnotation` produced by `java-direct` carries a `ClassId` / FQN — a *name*. It
  does not know what class that name refers to, even if the annotation is declared in the
  very same compilation unit.
- There is no `java-direct`-local class lookup table. Decision #1 deliberately forbids
  one.
- Anything that needs to resolve a `ClassId` to a `FirClassSymbol` — to read `@Target`,
  to check `kind`, to walk a supertype — must go through `session.symbolProvider`.

So the callback fires for every annotation, regardless of where the annotation class is
declared:

- `@org.jetbrains.annotations.NotNull` from a binary on the classpath — through
  `symbolProvider`.
- `@java.lang.Override` from the JDK — through `symbolProvider`.
- `@MyAnnotation` declared **in the same Java source set being compiled** — also through
  `symbolProvider`. java-direct has no faster path.
- `@KotlinAnnotation` declared in a Kotlin source under the same module — through
  `symbolProvider`.

To "fix" this by moving filtering into the model would require one of:

- **(a) Breach Decision #1** — wire `FirSession` into the model layer. This re-couples
  `core/compiler.common.jvm` to FIR, breaking K1 descriptor reuse and every other client
  of that module.
- **(b) Build a parallel `java-direct`-local annotation symbol table** — re-introduce
  exactly the symbol-table machinery whose absence is the architectural reason
  `java-direct` exists. Even a "small" symbol table for annotations alone must handle
  classpath binaries, Kotlin sources, JDK builtins, and same-source-set declarations —
  which is a complete duplicate of `session.symbolProvider`'s responsibility.
- **(c) Pre-filter at structure-build time inside `JavaTypeOverAst`** — what PSI and
  javac-wrapper do. This is the cleanest answer in principle, but it requires resolving
  annotation classes during model construction, which means option (a) or (b).

None of these is a localised cleanup; each is a complete overhaul of the layering rule
that distinguishes `java-direct` from javac-wrapper. The current callback shape is the
*result* of refusing to take any of those overhauls.

The two alternatives that look attractive but actually regress something:

- **"Push the predicate into `JavaResolutionContext`."** `JavaResolutionContext` is
  intentionally FIR-agnostic (imports / package / type parameters / containing class).
  Wiring `session.symbolProvider` into it leaks FIR into the resolution context and
  re-opens the layering question for every other callback. The current
  callback-injection pattern already gives the resolution context exactly what it needs
  at the call site, with no static coupling.
- **"Drop `needsTypeUseAnnotationFiltering` — the default impl is a no-op anyway."** The
  gate is not about correctness, it is about not paying for a virtual dispatch + closure
  allocation on every Java type-ref on the PSI hot path. The retrospective in
  `ITERATION_RESULTS.md` (~lines 565–700) is the binding evidence. Don't remove it
  without a perf-validated replacement.

## 6. Simplifications enabled by the **resolver-unification** track

See [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md)
and [`MERGED_REFACTORING_PLAN_2026_05_04.md`](MERGED_REFACTORING_PLAN_2026_05_04.md)
Step 3 for the unification track itself.

Unification does **not** change the architectural decisions of TYPE_USE handling:
Decision #1 stays in force; the model still holds the filter; FIR still owns the
predicate; the perf gate still guards the PSI hot path. But two cleanups become natural
to land in the same iteration as Step 3, because Step 3 canonicalises *exactly* the same
call shape (`session.symbolProvider.getClassLikeSymbolByClassId(classId)` + optional
origin filter) inside the **same file** (`JavaTypeConversion.kt`).

1. **Tighten the predicate signature from `(String) -> Boolean` to `(ClassId) -> Boolean`.**
   - `isTypeUseAnnotationClass(fqName: String, session)` currently calls
     `findClassIdByFqNameString` to recover the `ClassId`, then verifies
     `symbol.classId != classId` to defend against simple-name binary matches.
   - Every other model→FIR callback in this subsystem is already
     `(ClassId) -> Boolean` (`JavaClassifierType.resolve`,
     `JavaAnnotation.resolveAnnotation`).
   - After Step 3, the same file (`JavaTypeConversion.kt`) will have one canonical
     `getClassLikeSymbolByClassId(classId)` call shape and one mismatched FQN-based call
     shape — aligning them is a five-minute follow-up that becomes obvious instead of
     speculative.
   - Drops the FQN→`ClassId` re-probe and the cross-package mismatch heuristic at the
     same time; `ClassId`-based resolution is unambiguous by construction.

2. **Cache TYPE_USE-ness per `ClassId` on the `FirSession`.**
   - `isTypeUseAnnotationClass` is called once per annotation occurrence; in practice
     the same handful of annotation classes (`@NotNull`, `@Nullable`, `@NonNull`, …)
     recur thousands of times per build.
   - A `ConcurrentHashMap<ClassId, Boolean>` cache keyed off the session would amortise
     the symbol lookup and the `@Target` walk. Trivially correct because TYPE_USE-ness
     is a static property of the annotation class.
   - Step 3 lands the parse-counter / symbol-creation-counter perf gate on
     `IntelliJFullPipelineTestsGenerated.testIntellij_platform_externalProcessAuthHelper`.
     If the cache lands in the same iteration, it is measured on the same harness for
     free. Without the cache, the symbol-creation counter will reflect an extra
     `getClassLikeSymbolByClassId` call per `@NotNull` / `@Nullable` occurrence — exactly
     the kind of noise that gate is designed to surface.
   - Reduces the importance of `needsTypeUseAnnotationFiltering` (the dominant cost
     becomes the cache lookup, not the `@Target` walk) but does **not** let it be
     removed; the closure allocation cost is independent of the cache.

Neither cleanup is *required* by the unification plan — it remains forward-compatible
with the current TYPE_USE shape. Both ride for free on Step 3's harness setup.

## 7. Simplifications enabled by the **PSI-removal** track

See [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md)
and [`MERGED_REFACTORING_PLAN_2026_05_04.md`](MERGED_REFACTORING_PLAN_2026_05_04.md)
Steps 6 and 7 for the PSI-removal track itself.

PSI removal proceeds in three phases. The TYPE_USE handling is **forward-compatible with
all three** without code change:

| PSI track step | Touches `isTypeUseAnnotationClass` / model-side `filterTypeUseAnnotations`? | Implication for TYPE_USE |
|---|---|---|
| Step 1 — PSI Phase 1 (landed) | No | None |
| Step 6 — PSI Phase 2 | No | `isTypeUseAnnotationClass` already calls `session.symbolProvider`; it is *already* on the post-Phase-2 canonical path. |
| Step 7 — PSI Phase 3 | No | Annotation `@Target` resolution still goes via `session.symbolProvider`; the source-finder choice is upstream of that. |

Three things change for TYPE_USE handling, two of them documentation only:

1. **Doc-wording follow-up at Step 6** (pure documentation).
   The unification doc and any TYPE_USE narrative that mentions the chain
   `tryResolve(ClassId) → JvmSymbolProvider → CombinedJavaClassFinder → BinaryJavaClassFinder → JavaClassFinderOverAstImpl`
   becomes stale post-Phase-2: `CombinedJavaClassFinder` and `BinaryJavaClassFinder` are
   deleted. The chain shortens to:
   `tryResolve(ClassId) → session.symbolProvider → JvmClassFileBasedSymbolProvider | JavaSymbolProvider → JavaClassFinderOverAstImpl (source side only)`.
   `JavaTypeConversion.kt` and `javaTypes.kt` currently say only "via
   `session.symbolProvider`", which is post-Phase-2-safe; nothing requires editing in the
   TYPE_USE call sites themselves. Refresh only happens if a future doc edit names the
   deleted classes.

2. **The structure-side pre-filtering invariant changes scope at Step 7.**
   The reason FIR-level filtering caused regressions in Iter 22 is that PSI **and**
   javac-wrapper both pre-filter at structure-build time. After Step 7, the PSI source
   path retires (behind a flag for a 1–2-release transition window, then removed).
   - **The invariant survives** — javac-wrapper still pre-filters with its own symbol
     table.
   - **The default impl `fun filterTypeUseAnnotations(...) = annotations` must stay** for
     javac-wrapper's `JavaType` impls.
   - But the surface area where it matters shrinks: PSI is no longer a co-tenant of the
     `java-direct` path, so the "double-filter" failure mode caught by Iter 22 cannot
     re-fire from the PSI side.

3. **The perf gate's importance shrinks but does not vanish.**
   `needsTypeUseAnnotationFiltering` exists to keep the PSI hot path closure-free.
   - Steps 1–6: PSI source path still active — gate is fully load-bearing.
   - Step 7 transition window (1–2 releases): PSI source path still selectable behind a
     flag — gate stays load-bearing for parity.
   - Post-Step-7, after the PSI source leg is deleted: the *default* PSI path no longer
     exists in `java-direct`. The gate's purpose narrows to javac-wrapper's `JavaType`
     impls (still default-`false` there). It can plausibly become an internal detail
     rather than a public API knob, but only after the transition window closes and the
     measurement on the agreed testbed confirms the closure cost is no longer paid on
     any default path.

What does **not** become possible under PSI removal:

- **"Push filtering into the model layer"** — Decision #1 is reinforced, not weakened,
  by PSI removal. The merged plan's whole thesis is that FIR is the resolver and the
  model exposes shapes for FIR to fill. The `(ClassId) -> Boolean` callback is the same
  shape as `JavaClassifierType.resolve(tryResolve)` and
  `JavaAnnotation.resolveAnnotation(tryResolve)`; both are explicitly catalogued as the
  model→FIR bridge that the unification doc intends to keep.
- **"Pre-filter at structure-build time inside `JavaTypeOverAst`"** — would require a
  model-local class resolver. PSI removal forbids such resolvers more strongly: the only
  symbol provider in the post-Step-7 world is `session.symbolProvider`.

## 8. Bottom line

The TYPE_USE handling in `java-direct` is not "TYPE_USE handling pushed to FIR". It is
the established model→FIR callback pattern (model owns the *filter*, FIR owns the
*predicate*), gated by a perf flag for the default path. Each of the three structural
decisions — model-level filter, FIR-level predicate, perf gate — is anchored to a
non-negotiable architectural rule:

- Decision #1 (no `FirSession` in the Java Model) keeps the predicate in FIR.
- The PSI / javac-wrapper double-filter regression keeps the filter in the model.
- The measured ≈+5 % regression on `KotlinFullPipelineTestsGenerated` keeps the perf
  gate.

Changing this shape requires either breaching Decision #1, building a parallel
`java-direct`-local annotation symbol table, or otherwise undoing the architectural
distinction between `java-direct` and javac-wrapper — i.e. a complete overhaul of
`java-direct`. None of those is a localised cleanup.

The merged refactoring plan ([`MERGED_REFACTORING_PLAN_2026_05_04.md`](MERGED_REFACTORING_PLAN_2026_05_04.md))
is **forward-compatible** with the current shape end to end. It enables, but does not
require:

- Under **resolver unification** (Step 3): tighten the predicate to
  `(ClassId) -> Boolean` (consistency with every other callback in the same file) and
  cache TYPE_USE-ness per `ClassId` on the session (free measurement window on Step 3's
  perf gate).
- Under **PSI removal** (Steps 6 and 7): documentation refresh if the stale chain quote
  is ever named, and a long-term narrowing of the perf gate's surface area once the PSI
  source path is fully retired.

The architectural decisions that justified the current shape — model owns the filter,
FIR owns the predicate, perf gate guards the PSI hot path — survive both refactoring
tracks, in their entirety.
