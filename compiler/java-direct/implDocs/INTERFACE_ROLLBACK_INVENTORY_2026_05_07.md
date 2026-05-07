# Java-Model Interface Rollback Inventory — 2026-05-07

> **Status / scope.** Authoritative goal-statement for the public Java-model interface
> rollback. Supersedes the narrower scope of
> [`FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md`](FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md)
> for purposes of *what comes off the public interface*. The proposal still owns the
> design rationale for the callback-deletion half (its §6 dispatcher and §6.1
> `JavaSupertypeLoopChecker` stand). Steps 4.5a / 4.5b sequencing in the proposal is
> withdrawn; the corrective iterations below replace it.
>
> Read this file before editing any
> `core/compiler.common.jvm/src/.../load/java/structure/*` interface, any
> `JavaTypeOverAst` / `JavaAnnotationOverAst` resolution path, or
> `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt`.

---

## 1. Goal

The public `core/compiler.common.jvm/src/.../load/java/structure/*` Java-model interfaces
(`JavaType`, `JavaClassifierType`, `JavaAnnotation`, `JavaField`, `JavaAnnotationArgument`,
and friends) are consumed by **three** in-tree implementations: PSI-backed, binary, and
`java-direct`. The architectural intent of `java-direct` is that all three present the
**same public surface** to FIR — FIR conversion code reads `classifier?.classId`,
`annotation.classId`, `field.initializerValue`, etc. without branching on the
implementation.

During `java-direct`'s development a number of new members were added to those public
interfaces to thread FIR-side knowledge back into the model through callbacks (the
model couldn't see `FirSession`, so resolution it could not perform on its own had to
be supplied from outside). With `FirSession` now injected (per the foundation iteration
recorded in `ITERATION_RESULTS.md` 2026-05-06), those bridges are **debt** — the model
can now resolve internally, and the additions should come back off.

**The rollback goal, stated as an invariant:** the diff of every
`core/compiler.common.jvm/.../load/java/structure/*.kt` file from `master` to the
post-rollback HEAD is a **net deletion** of `java-direct`-introduced members; no new
public members appear there. Members that genuinely cannot be removed (perf-sensitive
protocols) move to a `java-direct`-private subinterface inside
`compiler/java-direct/src/.../model/`, not back onto the public surface.

This is the binding contract for `AGENT_INSTRUCTIONS.md` rule 7.

---

## 2. Inventory of `java-direct`-introduced public-interface members

Every member added during `java-direct` development. File:line refers to current HEAD
(post-Step-4.5a). The *Why added* column records the original problem the member
solved; the *Rollback path* column records how it comes off given `FirSession`
injection.

### 2.1 In `javaTypes.kt`

| Member | File:line | Why added | Rollback path | Status |
|---|---|---|---|---|
| `JavaType.needsTypeUseAnnotationFiltering` | `javaTypes.kt:29` | Lets the FIR caller skip allocating the filter-callback closure on impls (PSI/binary) that pre-filter at structure-build time. java-direct can't pre-filter without resolving annotation FQNs first. | Audit eager pre-filtering cost (Step C). If acceptable, java-direct pre-filters at structure-build time using its injected `FirSession` → both members come off. If not, move both to a java-direct-private subinterface. | **Open (Step C)** |
| `JavaType.filterTypeUseAnnotations(isTypeUseAnnotation)` | `javaTypes.kt:48` | Same — pulls a callback into the model for filtering. | Same as above. | **Open (Step C)** |
| `JavaClassifierType.isResolved` | _(deleted 2026-05-07)_ | Gates whether `classifierQualifiedName` is reliable. java-direct returns `false` when the simple name didn't resolve to a known class. | Confirmed dead on FIR side (no production callers). Pure cleanup; no replacement needed. | **Done (2026-05-07)** |
| `JavaClassifierType.resolvedClassId` | `javaTypes.kt:84` | **Side-channel added in Step 4.5a (2026-05-06).** Exposes the model's resolved `ClassId` for cross-file references because `classifier` was left `null`. The intended design (§3 of the proposal) was that `classifier?.classId` would be reliable post-injection; the implementer kept `classifier == null` for cross-file refs and added this property instead. | Build `FirBackedJavaClassAdapter`; populate `classifier` for every cross-file reference; FIR's `resolveTypeName` reverts to its pre-`java-direct` body. | **Open (Step 4.5b)** |
| `JavaClassifierType.isTriviallyFlexibleHint` | `javaTypes.kt:98` | PSI checks "is trivially flexible" via the resolved classifier; java-direct's `classifier` is null for cross-file refs, so this hint reproduces the answer. | Same as `resolvedClassId` — once `classifier` is reliably populated, FIR can call `isTriviallyFlexible(classifier)` directly. | **Open (Step 4.5b)** |
| `JavaClassifierType.containingClassIds` | `javaTypes.kt:113` | Innermost-to-outermost `ClassId`s of the containing-class chain, used by FIR's `findOuterTypeArgsFromHierarchy` (`JavaTypeConversion.kt:429`) for inherited-inner type-arg substitution. PSI uses `PsiSubstitutor` for the same job; binary uses the resolved classifier's `outerClass` chain. | After 4.5b lands, the `JavaClass`-shaped adapter has a real `outerClass` chain. Refactor `findOuterTypeArgsFromHierarchy` to walk `outerClass` instead of consuming `List<ClassId>` and the property comes off. | **Open (Step 4.5c)** |

### 2.2 In `javaElements.kt`

| Member | File:line | Why added | Rollback path | Status |
|---|---|---|---|---|
| `JavaAnnotation.isResolved` | _(deleted 2026-05-07)_ | Gates whether `classId` is reliable. java-direct returns `false` when the annotation reference is unqualified and not imported. | Confirmed dead on FIR side (no production callers). Pure cleanup; no replacement needed. | **Done (2026-05-07)** |
| `JavaField.supportsExternalInitializerResolution` | `javaElements.kt:153` | Lets the FIR caller skip allocating the resolve-callback closure when the impl (PSI/binary) already pre-evaluates literals. java-direct can't pre-evaluate cross-language references (Java field reads Kotlin constant) without resolution. | Audit eager evaluation cost (Step C). If acceptable, java-direct evaluates internally and exposes the answer through `initializerValue` → both members come off. If not, move both to a java-direct-private subinterface. | **Open (Step C)** |
| `JavaField.resolveInitializerValue(resolveReference)` | `javaElements.kt:163` | Same — pulls a resolution callback into the model for cross-language constant evaluation. | Same as above. | **Open (Step C)** |

### 2.3 In `annotationArguments.kt`

| Member | File:line | Why added | Rollback path | Status |
|---|---|---|---|---|
| `JavaEnumValueAnnotationArgument.isResolved` | _(deleted 2026-05-07)_ | Gates whether `enumClassId` is reliable. java-direct returns `false` when the enum reference is unqualified+unimported. | Confirmed dead on FIR side (no production callers). Pure cleanup; no replacement needed. | **Done (2026-05-07)** |
| `JavaEnumValueAnnotationArgument.couldBeConstReference` | `annotationArguments.kt:57` | PSI/binary disambiguate const-fields from enum entries at structure-build time and emit the correct `JavaLiteralAnnotationArgument` / `JavaEnumValueAnnotationArgument`; java-direct can't disambiguate at parse time and emits the enum variant for both cases, so it overrides this to `true` to opt FIR into a const-field fallback path. | Audit (Step C). With the model's resolver in place, java-direct can disambiguate by resolving the reference and emitting the correct argument variant directly — at which point this member comes off (and the FIR-side const-field fallback can be removed). If disambiguation cost is unacceptable, move to a java-direct-private subinterface. | **Open (Step C)** |

### 2.4 Adjacent code that becomes simplifiable (not interface members, but on the same path)

These are not public-interface additions, but they exist solely because of the
`classifier == null` cross-file case. They become simplifiable in lockstep with §3
Step 4.5b:

| Site | File:line | Why it exists | Post-rollback |
|---|---|---|---|
| `isMethodWithOneObjectParameter` `classifier == null` branch | `core/compiler.common.jvm/src/.../load/java/structure/javaLoading.kt:33–37` | Treats an unresolved `Object` reference as `java.lang.Object` because `classifier` is `null` for cross-file refs. | Once `classifier` is reliably populated by the 4.5b adapter, the fallback branch is unreachable and can be deleted. |

### 2.5 Already deleted (Step 4.5a, 2026-05-06)

For completeness — these are the three callback methods the proposal already deleted:

| Member | What it did | Replacement |
|---|---|---|
| `JavaClassifierType.resolve(tryResolve, getSupertypeClassIds): ClassId?` | FIR→model callback for cross-origin classifier resolution. | Internal resolution via `JavaResolutionContext.resolve` + `LazySessionAccess`. The interim side-channel `resolvedClassId` is what 4.5b removes. |
| `JavaAnnotation.resolveAnnotation(tryResolve): ClassId?` | Same, for annotations. | `JavaAnnotation.classId` populated internally. |
| `JavaEnumValueAnnotationArgument.resolveEnumClass(tryResolve): ClassId?` | Same, for enum-value args. | `JavaEnumValueAnnotationArgument.enumClassId` populated internally. |

---

## 3. Corrective iteration plan

The merged-plan / proposal sequence (Step 4.5a then Step 4.5b) is withdrawn. The
corrective sequence is below; treat each step as a separate iteration with its own
validation gate per `AGENT_INSTRUCTIONS.md`.

### Step 4.5b — Build `FirBackedJavaClassAdapter`; remove the side-channel + the four laziness-gate properties

**Status update (2026-05-07).** **Partially landed.** Three `isResolved` deletions
already shipped (per `ITERATION_RESULTS.md`'s 2026-05-07 entry). The full deliverable
(adapter + `resolvedClassId` deletion + `isTriviallyFlexibleHint` deletion +
`JavaTypeConversion.resolveTypeName` restore) was prototyped and reverted. The
prototype regressed three cross-file-inner-class tests (`testJ_k_complex`,
`testKJKComplexHierarchyWithNested`, `testGenericBoundInnerConstructorRef`) that
require proper outer-class-chain handling — which is now identified as **Step 4.5c**'s
prerequisite, not Step 4.5b's. **Re-sequenced:** Step 4.5c moves before the
adapter-side completion of 4.5b; the rest of 4.5b lands once 4.5c provides the
structural-`JavaClass`-shaped adapter (or equivalent outer-chain mechanism).

**Goal.** `JavaClassifierTypeOverAst.computeClassifier()` returns a real `JavaClass`
adapter for cross-file references, derived from `firSession.symbolProvider`.
`JavaTypeConversion.resolveTypeName` reverts to its pre-`java-direct` body
(`(javaType.classifier as? JavaClass)?.classId ?: findClassIdByFqNameString(name, session) ?: ClassId.topLevel(FqName(name))`).
Five interface members come off.

**Deletes (public interface — net deletions, no additions):**

- `JavaClassifierType.resolvedClassId` (the Step 4.5a side-channel) **— pending Step 4.5c**
- ~~`JavaClassifierType.isResolved`~~ **— done 2026-05-07**
- `JavaClassifierType.isTriviallyFlexibleHint` **— pending Step 4.5c**
- ~~`JavaAnnotation.isResolved`~~ **— done 2026-05-07**
- ~~`JavaEnumValueAnnotationArgument.isResolved`~~ **— done 2026-05-07**

**Touches.**

| File | Change |
|---|---|
| `core/compiler.common.jvm/src/.../load/java/structure/javaTypes.kt` | Delete `resolvedClassId`, `isResolved`, `isTriviallyFlexibleHint`. |
| `core/compiler.common.jvm/src/.../load/java/structure/javaElements.kt` | Delete `JavaAnnotation.isResolved`. |
| `core/compiler.common.jvm/src/.../load/java/structure/annotationArguments.kt` | Delete `JavaEnumValueAnnotationArgument.isResolved`. |
| **New** `compiler/java-direct/src/.../resolution/FirBackedJavaClassAdapter.kt` | `JavaClass`-shaped adapter over `FirRegularClassSymbol`. For `FirJavaClass` arms: backed by `directSupertypeClassIds()` cache + the underlying `javaClass.supertypes` for full structural data (or the §12 Q1 fallback D). For Kotlin / built-in / deserialized: builds a structural view on demand using `firSession.symbolProvider` for outer-chain recovery. Construction is mediated by `LazySessionAccess` so cache-population code cannot construct adapters. |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | `computeClassifier()` cross-file branch returns `FirBackedJavaClassAdapter` (not `null`). Delete `resolvedClassId` override. Delete `isTriviallyFlexibleHint` override (and `computeIsTriviallyFlexibleHint`). Delete `isResolved` override (if present). The "Cross-file references stay `classifier == null`" comment block (currently lines 128–134) is removed. |
| `compiler/java-direct/src/.../model/JavaAnnotationOverAst.kt` | Delete `isResolved` override; `classId` already returns the resolved value (post-Step-4.5a foundation). Same for the annotation-argument variant. |
| `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` | Restore `resolveTypeName` to its pre-`java-direct` body — delete the `?: javaType.resolvedClassId` middle term. Delete the `isTriviallyFlexibleHint` read at lines 189–193 in favour of the existing `isTriviallyFlexible(classifier)` path. Audit the `isResolved` read at line 322 and the surrounding `isResolved`-gated branches; collapse to the single resolution path now that `classifier` answers in all cases. |
| `compiler/fir/fir-jvm/src/.../javaAnnotationsMapping.kt` | Audit `isResolved` reads on `JavaAnnotation` (if any remain post-4.5a foundation). |
| `compiler/java-direct/ITERATION_RESULTS.md` | Iteration entry. |

**Validation.**

- `JavaUsingAst*` matrix unchanged: 2693 / 2693 (the post-Step-4.5a baseline).
- Trip-wire pair green: `Tests.Generics.InnerClasses.testJ_k_complex` and
  `Tests.J_k.CollectionOverrides.testMapMethodsImplementedInJava`.
- `KJKComplexHierarchyNestedLoop.kt` cross-origin re-entry trip-wire green (per
  the Step 4.5a entry; the `JavaSupertypeLoopChecker` from the foundation
  iteration carries this).
- PSI regression gate: `PhasedJvmDiagnosticLightTreeTestGenerated.*` shows no new
  failures.
- `git diff core/compiler.common.jvm/src/.../load/java/structure/` is **net deletion
  of 5 members; zero additions**.

### Step 4.5c — Eliminate `containingClassIds`

**Goal.** Refactor `findOuterTypeArgsFromHierarchy` (`JavaTypeConversion.kt:429`)
to walk `JavaClass.outerClass` directly instead of consuming `List<ClassId>` from
the type. Once `classifier` is reliably populated by the 4.5b adapter, the outer
chain is reachable through it.

**Deletes:** `JavaClassifierType.containingClassIds`.

**Touches.**

| File | Change |
|---|---|
| `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` | Refactor `findOuterTypeArgsFromHierarchy` to walk via `classifier.outerClass` (or via the resolved `FirRegularClass`'s `containingClassForLocalAttr` / outer chain when `classifier` is a Kotlin-derived adapter). Remove its `containingClassIds: List<ClassId>` parameter. The two call sites at lines 175 and 360 update accordingly. |
| `core/compiler.common.jvm/src/.../load/java/structure/javaTypes.kt` | Delete `containingClassIds`. |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | Delete `containingClassIds` override. |

**Validation.** Same matrix as 4.5b. `git diff core/compiler.common.jvm/.../structure/`
is one further net deletion.

### Step C — Audit and decide on the three perf-sensitive members

**Goal.** Determine whether the three perf-sensitive pairs/singletons —
`needsTypeUseAnnotationFiltering` + `filterTypeUseAnnotations`,
`supportsExternalInitializerResolution` + `resolveInitializerValue`, and
`JavaEnumValueAnnotationArgument.couldBeConstReference` — can be rolled back without
unacceptable parse-time / build-time regression. PSI/binary do not have these members
because they pre-filter / pre-evaluate / pre-disambiguate at structure-build time;
java-direct deferred for performance.

**Method.**

1. Run the existing perf harness (`-Pfir.force.javaDirect=true` plus
   `KotlinFullPipelineTestsGenerated` + `IntelliJFullPipelineTestsGenerated.testIntellij_platform_*`,
   per `AGENT_INSTRUCTIONS.md`'s "Performance Measurement" section) on the post-4.5c HEAD.
2. Prototype eager pre-filtering in the model (TYPE_USE annotation FQNs are resolvable
   via `LazySessionAccess` once parsing-level structure-building has access to it; need
   to confirm the parsing/resolution-time partition does not violate failure mode 1).
3. Same for cross-language constant evaluation in `JavaFieldOverAst` /
   `ConstantEvaluator`.
4. Same for enum-vs-const disambiguation in `JavaAnnotationOverAst`'s argument-emission
   path (resolve the reference and emit `JavaLiteralAnnotationArgument` for compile-time
   constants vs. `JavaEnumValueAnnotationArgument` for real enum entries).
5. Compare delta against current numbers.

**Decision.**

- If eager pre-filtering / pre-evaluation / pre-disambiguation is within noise: roll
  back. All five members come off the public interface entirely; the FIR-side const-field
  fallback path that `couldBeConstReference` gates (see `javaAnnotationsMapping.kt`)
  also comes out.
- If not: keep the protocols but **move them off the public surface**. Define a
  `java-direct`-private subinterface (e.g.,
  `compiler/java-direct/src/.../model/internal/JavaDirectAnnotationFilteringSupport.kt`)
  carrying `needsTypeUseAnnotationFiltering` / `filterTypeUseAnnotations`. FIR-side
  callers cast to that interface (or use a downcast helper) when the impl is
  `java-direct`. Public `core/compiler.common.jvm/.../structure/javaTypes.kt` is
  free of the protocol either way.

**Validation.** Whichever path is taken, the **invariant from §1** stands: zero
`java-direct`-introduced public-interface members remain at HEAD.

---

## 4. Operational rules

These supplement `AGENT_INSTRUCTIONS.md` for any iteration touching the surfaces named
above:

1. **Read this doc first.** The status table in §2 names the rollback path for every
   member. If you're tempted to add a new member as a "bridge" / "hint" /
   "side-channel", consult §2 first; the path that doesn't require an addition will
   be in there.
2. **Net deletions only on `core/compiler.common.jvm/.../structure/*.kt`.** The diff
   of every iteration described in §3 is a net deletion at that path. Reviewer should
   reject any patch that adds a member there.
3. **Side-channels are forbidden.** The 4.5a `resolvedClassId` was the canonical
   example of what not to do. If the work seems to require one, the iteration scope
   is wrong — split or escalate, do not bridge.
4. **Don't widen `firSession.symbolProvider` exposure.** Use the existing
   `LazySessionAccess` wrapper. New resolution-time helpers go on the wrapper or on
   `JavaResolutionContext`; cache-population code stays out.

---

## 5. Cross-references

- [`AGENT_INSTRUCTIONS.md` rule 7](../AGENT_INSTRUCTIONS.md#-non-negotiable-rules-stop-immediately-if-violated)
  — the operational invariant.
- [`FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md`](FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md)
  — superseded for *what comes off the public interface*; still authoritative for §6
  (per-origin dispatcher) and §6.1 (`JavaSupertypeLoopChecker`).
- [`MERGED_REFACTORING_PLAN_2026_05_04.md`](MERGED_REFACTORING_PLAN_2026_05_04.md) —
  the pre-existing execution-order doc; its Steps 4.5a / 4.5b inserts (applied
  2026-05-06) are subsumed by the corrective sequence in §3 above. A follow-up
  amendment to that doc should reference this inventory as the authoritative
  Step 4.5b / 4.5c / Step C source.
- [`ITERATION_RESULTS.md`](../ITERATION_RESULTS.md) — landing log; the
  Step 4.5a 2026-05-06 entry records the side-channel landing that motivated this doc.
- [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md)
  — Step 6 (PSI Phase 2) is unaffected by this rollback; the inventory only touches
  surfaces between FIR and the model, not the PSI removal track.

---

*Authoritative as of 2026-05-07. Update §2 status as iterations land. Future
rollback iterations append entries to `ITERATION_RESULTS.md` and update this doc's
status column accordingly.*
