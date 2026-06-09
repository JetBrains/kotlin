# `fir-jvm` ← → `ff12cbb3` Diff Analysis (2026-05-25, fresh)

> **Scope.** Fresh, ground-up audit of everything still touched in
> `compiler/fir/fir-jvm/` on the `java-direct` branch (HEAD =
> `3637c96c96b0`) versus the pre-`java-direct` revision `ff12cbb3d915`
> ("[Metadata] Combine `toJvmMetadataVersion` and
> `toKlibMetadataVersion`"). Goal: list every difference, justify
> empirically and per-test-need whether the change is still necessary,
> and propose either a rollback or a deeper refactoring that would
> shrink the FIR-jvm-side surface further.
>
> **Methodology.** `git diff ff12cbb3 HEAD -- compiler/fir/fir-jvm/`
> on a synced workspace, then per-file analysis of the diff hunks plus
> a consumer-side audit (`search_contents_by_grep`) for every new public
> symbol. Empirical hit counts are quoted from the existing probes
> (`ITERATION_RESULTS.md` 2026-05-24 D2-A, `JTC_CLEANUP_2026_05_24.md`
> sub-block table); where a claim survives only on a probe that was
> never reproduced, the document flags it as "needs validation" rather
> than treating the historical number as ground truth.
>
> **Caveat on prior docs.** `ITERATION_RESULTS.md` and
> `JTC_CLEANUP_2026_05_24.md` summarise the same diff after two
> 2026-05-25 cleanup waves. The numbers reported there for *what
> already landed* (file sizes, suite results) reproduce on this branch.
> The numbers reported there for *what is allegedly still load-bearing*
> (sub-block hit tables) are taken as hypotheses; this document
> distinguishes "live, confirmed by empirical probe" from "claimed live
> by static analysis only" wherever the distinction matters.

---

## 1. Headline numbers

```
 .../org/jetbrains/kotlin/fir/java/FirJavaFacade.kt                |  60 +++--
 .../kotlin/fir/java/JavaTypeConversion.kt                         | 286 +++++++++++++++++++--
 .../fir/java/MutableJavaTypeParameterStack.kt                     |  32 +++
 .../kotlin/fir/java/declarations/FirJavaClass.kt                  |  22 +-
 .../kotlin/fir/java/declarations/FirJavaField.kt                  |  14 +
 .../fir/java/enhancement/SignatureEnhancement.kt                  |   2 +
 .../kotlin/fir/java/javaAnnotationsMapping.kt                     |  34 ++-
 7 files changed, 397 insertions(+), 53 deletions(-)
```

The retired files `JavaModelExtensions.kt` (73 lines), the
`JavaTypeWithExternalAnnotationFiltering` interface, the
`JavaFieldWithExternalInitializerResolution` callback, the
`JavaEnumValueAnnotationArgumentWithConstFallback` callback, the
`filterTypeUseAnnotationsIfNeeded` family, and the
`resolveExternalFieldValue`/`extractEvaluatedConstValue`/
`tryExtractConstantValue` helpers are **already gone** at HEAD (the two
2026-05-25 cleanups). They do not show up in this diff and are not
analysed below.

---

## 2. Per-file change clusters

The 397/53 budget decomposes into **11 distinct logical clusters**
across 7 files. Each is named below with a stable code so the
rest of the document can reference it.

| Code | File | What changed | LoC | Category |
|---|---|---|---:|---|
| `F1` | `FirJavaField.kt` | `lazyHasInitializer: Lazy<Boolean>` ctor param + `hasInitializer: Boolean` accessor + builder field | +14 | Lombok plugin (rule §7 exception) |
| `F2` | `SignatureEnhancement.kt` | Propagate `lazyHasInitializer` when copying `FirJavaField`; synthesise on the generic `FirField` branch | +2 | Lombok plugin |
| `C1` | `FirJavaClass.kt` | `directSupertypeClassIdsCache: List<ClassId>` lazy + `directSupertypeClassIds()` accessor | +20 / −1 import | Step 4.5c binary-arm read |
| `S1` | `MutableJavaTypeParameterStack.kt` | New field `containingClassSymbol: FirRegularClassSymbol?` with copy-propagation | +12 | Outer-args recovery |
| `S2` | `MutableJavaTypeParameterStack.kt` | New interface `JavaTypeParameterWithFirSymbol : JavaTypeParameter` | +20 | `FirBackedJavaClassAdapter` |
| `H1` | `FirJavaFacade.kt` | Set `javaTypeParameterStack.containingClassSymbol = classSymbol` in `convertJavaClassToFir`; comment-only refactor of the surrounding block | +4 / −12 | Outer-args recovery |
| `H2` | `FirJavaFacade.kt` | `setSealedClassInheritors` callback collapsed to `?: return@mapNotNullTo null` (drop the empty/blind `ClassId` fallback) | +2 / −2 | Cleanup after `ResolvedJavaClassifierType` |
| `H3` | `FirJavaFacade.kt` | `enumEntriesOrigin = Library when classSource == null` for source-Java enums | +4 / −1 | java-direct source classes have no `KtSourceElement` |
| `H4` | `FirJavaFacade.kt` | New `isCanonicalRecordConstructorForSource` + extra `isPrimary` disjunct when `source == null && javaClass.isRecord` | +11 | java-direct records (no PSI) |
| `H5` | `FirJavaFacade.kt` | `lazyHasInitializer = lazy { javaField.hasInitializer }` populator (paired with `F1`) | +4 | Lombok |
| `J1` | `JavaTypeConversion.kt` | New top-of-function `buildTypeProjections(lookupTag)` helper, replacing the inline projection-construction block on the `JavaClass ->` arm | net 0 (extraction) | Refactor — shared with `J2`/`J3` |
| `J2` | `JavaTypeConversion.kt` | Inherited-inner-class outer-type-args recovery in `is JavaClass ->` arm: `outerTypeArgs = findOuterTypeArgsFromHierarchy(...)` when `typeArguments.size < tps.size` | +18 | Step 4.5c, 2 hits / 2793 |
| `J3` | `JavaTypeConversion.kt` | Expanded `null ->` arm: `resolveTypeName(...)`, `findClassIdByFqNameString(...)`, post-D2-A trivial path with shared `buildTypeProjections` | +14 (J3 itself; the rest moved into helpers J4) | Path B/C |
| `J4` | `JavaTypeConversion.kt` | Five new helpers: `resolveTypeName`, `findOuterTypeArgsFromHierarchy`, `findTypeArgsForClassInHierarchy`, `substituteTypeArgs`, `findClassIdByFqNameString` | +192 | J2/J3 implementation |
| `J5` | `JavaTypeConversion.kt` | `toConeTypeProjection` empty-attrs short-circuit (`if convertedAnnotations.isNotEmpty()` instead of always wrapping) + `additionalAnnotations.isNullOrEmpty()` guard | +5 / −2 | Cheap perf, 2837 hits / 2793 |
| `A1` | `javaAnnotationsMapping.kt` | `JavaEnumValueAnnotationArgument`: replace `requireNotNull(enumClassId ?: ...)` with a graceful `buildErrorExpression` when both `enumClassId` and the expected-array-element fallback are null | +20 / −9 | java-direct's `enumClassId` can be null |

The `F`/`C`/`S`/`H`/`J`/`A` prefixes match the file the change lives in
(`FirJavaField`, `FirJavaClass`, `MutableJavaTypeParameterStack`,
`FirJavaFacade`, `JavaTypeConversion`, `javaAnnotationsMapping`).

---

## 3. Per-cluster justification, liveness, and rollback options

### 3.1 `F1`+`F2`+`H5` — `JavaField.hasInitializer` chain (Lombok)

**What it does.** Adds a `Lazy<Boolean>` cell to `FirJavaField`,
populated from `javaField.hasInitializer` at `convertJavaFieldToFir`
time and propagated through `SignatureEnhancement`'s field-copy
arm. Exposed publicly on `FirJavaField` as `val hasInitializer`. The
upstream `JavaField.hasInitializer: Boolean` interface member was
added at the same time on the public Java-model surface in
`core/compiler.common.jvm/.../structure/javaElements.kt` (rule §7
exception, documented in `ITERATION_RESULTS.md` 2026-05-20).

**Why it is needed.** Lombok K2 plugin's
`AllArgsConstructorGeneratorPart.getFieldsForParameters` and
`RequiredArgsConstructorGeneratorPart.isFieldRequired` need to know
whether a Java field carries any initializer expression (broader than
JLS-4.12.4 constant-only `hasConstantNotNullInitializer`). Prior shape
was `(declaration.source?.psi as? PsiField)?.hasInitializer()` —
**which silently returned `false` for any non-PSI `JavaField`
implementation**, including `JavaFieldOverAst`. The cast was itself a
PSI leak in the K2 plugin path.

**Liveness.** Direct production consumer in
`plugins/lombok/lombok.k2/.../AllArgsConstructorGeneratorPart.kt:36`
and `RequiredArgsConstructorGeneratorPart.kt:40`. Without this chain,
13 / 66 Lombok plugin tests regressed on the `java-direct` source
loader (`ITERATION_RESULTS.md` 2026-05-20 Lombok entry).

**Rule §7 status.** Adding a member on `JavaField` is *normally*
forbidden. The exception was taken because:
- The base `AllArgsConstructorGeneratorPart` (verified at
  `git show ff12cbb3:plugins/lombok/lombok.k2/.../AllArgsConstructorGeneratorPart.kt`)
  already carried `// TODO: consider adding hasInitializer property directly to java model`
  — the upstream maintainers already wanted this property; the
  java-direct branch is simply closing the long-standing TODO.
- The replacement removes a `PsiField` cast from the K2 plugin path
  → **net PSI-debt reduction**, not addition.

**Rollback options.**
1. **Move it to a java-direct-private subinterface.** Would require
   the Lombok plugin to dispatch on `(declaration as?
   JavaFieldWithHasInitializer)?.hasInitializer ?: false`, which is a
   strict regression of the current shape (we re-introduce a
   non-default branch and lose the PSI value for K2). **Not
   recommended.**
2. **Keep as-is.** Recommended: the change is the cleanest possible
   shape (one property on the model interface, mirrored on all 6
   impls), it closes a public `// TODO`, and removes PSI from the K2
   plugin path.

**Verdict: keep. No further rollback feasible.**

---

### 3.2 `C1` — `FirJavaClass.directSupertypeClassIds()` cache

**What it does.** Adds a `lazy(LazyThreadSafetyMode.PUBLICATION)`
`List<ClassId>` cache, populated by walking
`javaClass.supertypes.mapNotNull { (it.classifier as? JavaClass)?.classId }`
on first read.

**Why it is needed.** Sole consumer is
`JavaResolutionContext.directSupertypeClassIds(classId)` at
`compiler/java-direct/.../resolution/JavaResolutionContext.kt:206-227`,
**binary-Java arm** of the per-origin dispatcher (case 2 of three).
The dispatcher is the cycle-safe equivalent of asking "what are the
direct supertypes of this `ClassId`?". For source-Java classes it
walks the AST directly; for built-in/deserialized/Kotlin classes it
runs `lazyResolveToPhase(SUPER_TYPES)` and reads
`firClass.superTypeRefs`; for **binary-Java** classes (also a
`FirJavaClass`, but originating from the binary class finder), reading
`superTypeRefs` would trigger the lazy enhancement chain that can
re-cycle back into resolution if invoked mid-`SUPER_TYPES`. The cache
on `FirJavaClass` reads from the *unenhanced*
`javaClass.supertypes.classifier.classId` chain instead, which is safe.

**Liveness.** Live every time the model's inherited-inner-class /
supertype walker hits a binary `FirJavaClass` (anywhere a Java source
class extends or implements a binary Java class). The
`JavaInheritedMemberResolver.walkBinarySupertypes` BFS at
`JavaInheritedMemberResolver.kt:208-228` consumes it transitively.

**Rollback options.**
1. **Inline into `JavaResolutionContext`.**
   `JavaResolutionContext` can already see `FirJavaClass` via
   `symbol.fir`, can already read the `javaClass` property (its
   visibility was widened during Step 4.5c per
   `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §12), and can compute
   the same `supertypes.mapNotNull { ... }` chain itself. The cache
   would move from `FirJavaClass` to a `ConcurrentHashMap<ClassId,
   List<ClassId>>` in `JavaResolutionContext` (or to a
   `FirSessionComponent`). Net effect: FIR-jvm side ≈ −20 LoC,
   java-direct side ≈ +10 LoC. **Feasible**, but introduces a
   per-session cache that duplicates what `lazy` already does
   per-instance and is shared across `JavaResolutionContext`
   instances. The current single-line `lazy` is arguably cleaner.
2. **Keep as-is.** Recommended: the cache is +5 functional LoC plus
   KDoc, has exactly one consumer, and lives next to its data
   (`FirJavaClass.javaClass`).

**Verdict: keep, but flag for rollback if a follow-up wave is willing
to absorb the move into a `FirSessionComponent` (see §9 for what
"the `FirSessionComponent` migration" denotes). Net codebase saving
≈ −10 LoC; small.**

---

### 3.3 `S1`+`H1`+`J2`+`J4`(partial) — outer-args recovery for inherited inner classes

**What it does.** When a Java source file references an inherited
inner class without enough explicit type arguments
(`SuperClass<String>.NestedInSuperClass` written as
`NestedInSuperClass`), the JLS implicitly inherits the outer
`SuperClass`'s `<String>` type argument onto the nested class's outer
chain. PSI / binary classifiers wire this via the `outerClass` chain
in their `JavaClassifierType` impl; java-direct's
`JavaClassifierTypeOverAst` does not (intentionally — exposing the
outer chain at the model level was rejected during Step 4.5c, see
§3.3.2 below). FIR-side recovery walks the lexical containing-class
chain from the *type reference's* containing `FirJavaClass`, finds
the supertype that bound the outer class to a concrete type, and
substitutes the bound through. Implementation is `J4`'s
`findOuterTypeArgsFromHierarchy` + `findTypeArgsForClassInHierarchy`
+ `substituteTypeArgs`. Driving context is the `containingClassSymbol`
field that `H1` sets on `MutableJavaTypeParameterStack` at
`convertJavaClassToFir` time, and `J2` reads at type-conversion time.

**Why it is needed.** Pre-Step-4.5b the model exposed
`JavaClassifierType.containingClassIds` for this. That member was
deleted in Step 4.5a-c per rule §7 (no new public Java-model
members; rollback of existing ones). The recovery has to live
*somewhere*, and the model isn't allowed to surface it on the public
interface anymore.

**Liveness.**
- `J2` (the `outerTypeArgs` gate in the `JavaClass ->` arm):
  empirically **2 hits / 2793 tests** in `JavaUsingAst*`
  (`JTC_JC_OUTER_HIT` per `JTC_CLEANUP_2026_05_24.md` sub-block
  table).
- `J4`'s helpers are transitively live through `J2` and through
  `J3`/`null ->`'s `resolveTypeName` (`findClassIdByFqNameString`
  inside).
- `containingClassSymbol` propagation through `copy()` and
  `addStack()` is read only by `findOuterTypeArgsFromHierarchy`; the
  field copy is correctness-preserving and zero cost.

**Rollback options.**

1. **(Rejected by `INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md`)**
   Restore `JavaClassifierType.containingClassIds: List<ClassId>` to
   the public interface. Would let the FIR-side recovery collapse to
   a 3-line `outerTypeArgs = type.containingClassIds.firstOrNull()?.let { ... }`
   probe. Rejected because rule §7 forbids new public members.

2. **Java-direct-private subinterface.** Introduce
   `JavaClassifierTypeWithContainingClassIds : JavaClassifierType`
   inside `compiler/java-direct/.../model/`, with a
   `containingClassIds: List<ClassId>` getter implemented by
   `JavaClassifierTypeOverAst`. The FIR-side recovery becomes:

   ```kotlin
   val outerTypeArgs: Array<out ConeTypeProjection>? = (this as? JavaClassifierTypeWithContainingClassIds)
       ?.let { findOuterTypeArgsFromContainingClassIds(it.containingClassIds, classId, session) }
   ```

   The recovery walker would then iterate the model-supplied
   containing-class chain rather than reaching into
   `MutableJavaTypeParameterStack.containingClassSymbol`. **Saves:**
   `S1` field (≈4 LoC) + `containingClassSymbol` propagation in
   `copy()` (≈1 LoC) + `H1`'s setter line + the
   `MutableJavaTypeParameterStack.containingClassSymbol = ...`
   acquisition in `findOuterTypeArgsFromHierarchy` itself (≈3 LoC).
   **Does not save:** `J4`'s substitution machinery
   (`findTypeArgsForClassInHierarchy` + `substituteTypeArgs`), which
   is the JLS work itself and has to live somewhere — the model
   doesn't carry pre-substituted outer type arguments.
   **Net saving: ≈ −10 LoC on FIR-jvm, ≈ +20 LoC on java-direct;
   one new subinterface.** Worth it if and only if the project is
   trying to push the dependency direction further toward
   "FIR-jvm-doesn't-know-about-java-direct".

3. **Keep as-is.** Recommended for now. The 2 hits/2793 means the
   path is exercised; the 90 LoC of FIR-side recovery (J4) is real
   JLS work that any rollback would have to relocate, not delete.
   The `containingClassSymbol` plumbing in `MutableJavaTypeParameterStack`
   is the smallest possible carrier of "what class were we in when
   we converted this type reference?", and the field copy is a
   one-liner. The recovery itself is a real code path that pays
   non-zero rent.

**Verdict: keep. Option 2 is the only architecturally-cleaner path,
saves ≈10 LoC on FIR-jvm at the cost of one new java-direct-private
subinterface and ≈20 LoC of model-side mirroring — net codebase wash.
File only if a future iteration is explicitly cleaning up the
"FIR-jvm references java-direct's adapter classes only via interface"
boundary.**

---

### 3.4 `S2` — `JavaTypeParameterWithFirSymbol` interface + matching arm in `JavaTypeConversion`

**What it does.** New interface
`JavaTypeParameterWithFirSymbol : JavaTypeParameter` carrying a
`firTypeParameterSymbol: FirTypeParameterSymbol` getter.
`JavaTypeConversion.kt`'s `is JavaTypeParameter ->` arm checks the
shortcut first (`(classifier as? JavaTypeParameterWithFirSymbol)?.firTypeParameterSymbol`)
before falling back to `javaTypeParameterStack[classifier]`.

**Why it is supposed to be needed.** `FirBackedJavaClassAdapter`
synthesises `FirBackedJavaTypeParameter` wrappers for cross-file
outer-class type parameters; those wrappers are never registered in
any per-class `MutableJavaTypeParameterStack` (the adapter is built
on demand for cross-file references, far away from the
`convertJavaClassToFir` site that builds the stack). Without the
interface, FIR would have nothing to look up the wrapper against.

**Liveness.** **0 hits / 2793 tests** (`JTC_JTP_FIRSYM_HIT` per
`JTC_CLEANUP_2026_05_24.md` sub-block table). All 47,253
`JavaTypeParameter`-branch lookups in the `JavaUsingAst*` suite go
through the stack-lookup fallback.

> **Caveat.** The probe corpus is the `JavaUsingAst*` suite (2793
> tests). `IntelliJFullPipelineTestsGenerated` /
> `KotlinFullPipelineTestsGenerated` were not re-probed; the
> hypothesis "`FirBackedJavaTypeParameter` never reaches this arm in
> any production corpus" is unverified.

**Rollback options.**

1. **Inline the shortcut.** Replace
   `(classifier as? JavaTypeParameterWithFirSymbol)?.firTypeParameterSymbol ?: javaTypeParameterStack[classifier]`
   with `javaTypeParameterStack[classifier]`, and delete the
   `JavaTypeParameterWithFirSymbol` interface. **Saves:** ~24 LoC on
   FIR-jvm (interface + KDoc), ~2 LoC on the call site, ~4 LoC on the
   adapter (the `: JavaTypeParameterWithFirSymbol` supertype on
   `FirBackedJavaTypeParameter`). **Risk:** if any non-suite corpus
   exercises a `FirBackedJavaTypeParameter` actually arriving at this
   arm, the deletion silently produces a `null`
   `FirTypeParameterSymbol` and downstream resolves to
   `ConeStubType` / unresolved type ref. The two reverted-prototype
   regressions cited in `ITERATION_RESULTS.md`
   (`testGenericBoundInnerConstructorRef`,
   `testKJKComplexHierarchyWithNested`) might be in this category but
   they appear to pass on the broader corpus through the `JavaClass ->`
   arm's outer-args recovery, not the type-parameter shortcut.
2. **Probe-then-decide.** Run the same `System.err.println("JTP_FIRSYM_HIT")`
   instrumentation against (a) `KotlinFullPipelineTestsGenerated` and
   (b) at least one `IntelliJFullPipelineTestsGenerated` slice. If
   both report 0 hits, option 1 is safe.
3. **Keep as-is.** Defensive against `FirBackedJavaTypeParameter`
   actually being routed through this arm in production.

**Verdict: deletion candidate, blocked on a broader-corpus probe. If
the probe confirms 0 hits, drop the interface and the shortcut — ≈30
LoC saved on FIR-jvm. If even one hit, keep both. This is the highest-
yield FIR-jvm-side rollback still on the table.**

---

### 3.5 `H2` — `setSealedClassInheritors` `?: return@mapNotNullTo null`

**What it does.** Where the base code was
`classifier?.let { JavaToKotlinClassMap.mapJavaToKotlin(it.fqName!!) ?: it.classId }`
(which silently returned `null` from the `let` block when
`classifier` was null and added it to the `mutableListOf<...>()`
because `mapNotNullTo` filters it out), the new shape is
`val classifier = classifierType.classifier as? JavaClass ?: return@mapNotNullTo null`.

**Why it is the way it is.** Semantically equivalent for the
classifier-non-null path. The `classifier-null` path used to fall
through with `null` (so `mapNotNullTo` would drop it); the change
makes the drop explicit at the top of the lambda. This was a
side-effect of the 2026-05-24 implicit-permits cleanup
(`ResolvedJavaClassifierType` now ensures the classifier is never
null on the implicit-permits path), so the old "build a top-level
`ClassId` blindly" wasn't being used anyway.

> Per `ITERATION_RESULTS.md` 2026-05-24, the historic prototype that
> attempted to delete the `classifier == null` branch outright
> regressed exactly 2 implicit-permits tests; that has since been
> fixed via `JavaClassOverAst.deriveImplicitPermittedTypes` emitting a
> `ResolvedJavaClassifierType`, so the FIR-side `?: return@mapNotNullTo null`
> now never actually fires.

**Liveness.** **0 hits / 2793 tests** post-2026-05-24 fix
(`JD_NULL_BRANCH_HIT` probe in the same iteration).

**Rollback options.**

1. **Revert to the original `classifier?.let { ... }` shape.**
   Semantically equivalent on production traffic (the `?: it.classId`
   branch was the only behaviour difference and it has no live
   callers). Save: 0 LoC (the diff is also +/- net 0). The current
   shape is arguably clearer; not worth the noise.
2. **Keep as-is.** Recommended.

**Verdict: keep. Diff is essentially a stylistic cleanup that fell out
of a real semantic fix elsewhere.**

---

### 3.6 `H3` — `enumEntriesOrigin` source-vs-library guard

**What it does.** Before:
```kotlin
val enumEntriesOrigin = when {
    firJavaClass.origin.fromSource -> FirDeclarationOrigin.Source
    else -> FirDeclarationOrigin.Library
}
```
After:
```kotlin
val enumEntriesOrigin = when {
    firJavaClass.origin.fromSource && classSource != null -> FirDeclarationOrigin.Source
    else -> FirDeclarationOrigin.Library
}
```

**Why it is needed.** java-direct's `JavaClassOverAst` does **not**
produce a `KtSourceElement` (the AST is light-tree, not PSI; there is
no `psi` to point at). `classSource` is the value FIR uses for
diagnostic source attribution and source-element validation on the
generated `entries` property's getter. With the unconditional
`Source` origin, `FirPropertyAccessorImpl`'s source-element
validation fired on the `entries` getter (specifically on the
`getter` — not on `values()` / `valueOf()` because those are
functions, not properties) and asserted out. The Library fallback
sidesteps the assertion while still producing a correctly-shaped
`entries` getter that `EnumExternalEntriesLowering` can intercept for
intrinsic mapping.

**Liveness.** Live on every java-direct-loaded Java enum class — i.e.
every Java enum referenced from a `JavaUsingAst*` test fixture or
production source class.

**Rollback options.**

1. **Give `JavaClassOverAst` a synthetic `KtSourceElement`.** Would
   let the unconditional `Source` arm fire and we'd drop the
   `classSource != null` guard. **Cost:** introducing a synthetic
   `KtSourceElement` carries downstream effects (diagnostic rendering,
   error reporting, IDE navigation paths in LL-FIR scenarios). Likely
   non-trivial and reaches beyond enum entries — many other
   `classSource`-dependent paths exist.
2. **Loosen `FirPropertyAccessorImpl`'s source-element validation.**
   Out-of-scope for fir-jvm; would touch the FIR core.
3. **Keep as-is.** Recommended. 1-line condition is the minimal,
   localised carrier of "source-Java-without-PSI".

**Verdict: keep. One-line condition, can't be cheaper. A deeper
refactoring (giving `JavaClassOverAst` synthetic source elements)
might pay off elsewhere too but is out of scope of "minimise
`fir-jvm` diff" — it would just *move* the change, not remove it.**

---

### 3.7 `H4` — `isCanonicalRecordConstructorForSource` + extra `isPrimary` disjunct

**What it does.** When `source?.psi` is null (no PSI — java-direct
records), the base code can never set `isPrimary = true` for a
record's canonical constructor, because the existing detection runs
through `JavaPsiRecordUtil.isCanonicalConstructor((source.psi as
PsiMethod))`. The new disjunct
`(source == null && javaClass.isRecord && isCanonicalRecordConstructorForSource(javaConstructor, javaClass))`
runs a non-PSI check (parameter names match record component names in
order — JLS requires identical names for explicit canonical
constructors).

**Why it is needed.** Java records with explicit canonical
constructors must be detected as primary so the FIR record support
picks the right constructor for synthesising the canonical accessors
and equals/hashCode contract. Without the disjunct, java-direct-
loaded records with an explicit canonical constructor would mis-rank
the explicit one as non-primary and the compact one (compiler-
generated) as primary, producing the wrong member layout.

**Liveness.** Live on every java-direct-loaded record with an
explicit canonical constructor — probably handful of corpus tests
(`record` patterns in `JavaUsingAst*`). The presence of the helper at
HEAD implies at least one test exercises this; no specific count is
recorded in the prior docs.

**Rollback options.**

1. **Move `isCanonicalRecordConstructorForSource` into the model.**
   Add `JavaConstructorOverAst.isCanonical: Boolean` computing the
   same predicate model-side, and let FIR read it via a
   java-direct-private subinterface. Cost: new public/private member
   on `JavaConstructor`-shaped types. The FIR-side helper is 4 lines;
   moving it would also require a subinterface plus model-side
   wiring, ≈10 LoC net relocation — not deletion.
2. **Add an `isCanonical: Boolean` member on `JavaConstructor` public
   interface.** Rule §7 violation, not allowed.
3. **Keep as-is.** Recommended. 7-line helper + 1-line disjunct;
   localised; explained.

**Verdict: keep. Option 1 (model-side relocation) is a stylistic
preference; option 2 is forbidden. The current shape is minimal.**

---

### 3.8 `H2`+other `FirJavaFacade.kt` comment refactoring

**What it does.** Replaces an ~18-line block comment in
`convertJavaClassToFir` (the "this is where the problems begin"
narrative) with a shorter 3-line summary referencing
`FirLazyJavaDeclarationList` for staging order. Also removes the
stale `// NB: null should be converted to null` comment from
`convertJavaFieldToFir.lazyInitializer`.

**Liveness / why.** No semantic change. Stylistic tightening that
made it into the diff alongside `H1`.

**Verdict: keep. Comment-only.**

---

### 3.9 `J5` — `toConeTypeProjection` empty-attrs short-circuit

**What it does.** Before:
```kotlin
ConeAttributes.create(listOf(CustomAnnotationTypeAttribute(convertedAnnotations)))
```
After:
```kotlin
if (convertedAnnotations.isNotEmpty())
    ConeAttributes.create(listOf(CustomAnnotationTypeAttribute(convertedAnnotations)))
else
    ConeAttributes.Empty
```

Plus the inner `if (additionalAnnotations != null)` is tightened to
`if (!additionalAnnotations.isNullOrEmpty())`.

**Why it is there.** Cheap micro-optimisation; sidestep a
`CustomAnnotationTypeAttribute(emptyList())` allocation. **2837 hits
/ 2793 tests** per `JTC_EMPTY_ATTRS_HIT` — actually a real saving.

**Verdict: keep. Localised, cheap, measurable.**

---

### 3.10 `J1` — `buildTypeProjections` helper extraction

**What it does.** Hoists the `Array(typeArguments.size) { ... toConeTypeProjection ... }`
construction out of the `JavaClass ->` arm into a local function so
that the `null ->` arm (`J3`) can reuse it. Net 0 LoC change (the
inline block is gone, replaced by `buildTypeProjections(lookupTag)`
calls).

**Why it is there.** Required for `J3` — the post-D2-A trivial
`null ->` arm uses the same construction shape.

**Verdict: keep. Pure refactor enabling J3.**

---

### 3.11 `J2`+`J4`(partial) — outer-args recovery on the `JavaClass ->` arm

Already analysed under §3.3.

---

### 3.12 `J3`+`J4`(partial) — expanded `null ->` arm with `resolveTypeName` and `findClassIdByFqNameString`

**What it does.** The original 2-line `null ->` arm constructed
`ClassId.topLevel(FqName(qualifiedName))` blindly and called
`constructClassLikeType(...)`. The new arm calls
`resolveTypeName(qualifiedName, javaType, session, mode)`, which
first probes `(javaType.classifier as? JavaClass)?.classId`, then
`findClassIdByFqNameString(name, session)` (a session-aware
`split-the-FQN` walk that picks the longest-known-package split via
`FirSymbolNamesProvider`), then falls back to `ClassId.topLevel(...)`.
The constructed type is computed identically to the `JavaClass ->`
arm via `buildTypeProjections(lookupTag)` or `lowerBound?.typeArguments`.

**Why it is needed.** `JavaClassifierType.classifier` is `null` for:

- **Path B** — java-direct's `JavaClassifierTypeOverAst` instances
  whose JLS five-step + `tryResolve` model-side probe missed (bare
  refs like `T`, `Bar`, `ArrayDeque` when no import/scope brings them
  in).
- **Path C** — binary `PlainJavaClassifierType` instances
  (PSI-loaded binary code path, out of java-direct scope) whose
  binary classifier resolver gave up.

Both want the type ref to resolve to *something* if FIR's symbol
provider knows the FQN — without `findClassIdByFqNameString`, every
B/C reference produces `ClassId.topLevel(FqName("ArrayDeque"))` =
`ClassId(<root>, ArrayDeque)`, which produces unresolved-type
diagnostics for legitimate JDK references.

**Liveness.** **~160 hits / 2793** through the trivial path:
- `JTC_NULL_PROJ_LOWER` (`lowerBound?.typeArguments`) = 155
- `JTC_NULL_PROJ_BUILD` (`buildTypeProjections`) = 5

The expanded sub-blocks added during java-direct development
(`JavaToKotlinClassMap` rewrite, `readOnlyToMutable`,
`findOuterTypeArgsFromHierarchy` from this arm, `isRawType`
computation, RAW / OUTER projection arms) are **all 0 hits / 2793** —
this is the "D1" dead-code reduction in `JTC_CLEANUP_2026_05_24.md`,
not landed at HEAD.

> **Validation status.** D1's dead-code claim survives only on the
> `JavaUsingAst*` corpus. Pending probes on `KotlinFullPipelineTestsGenerated`
> and `IntelliJFullPipelineTestsGenerated` — if any of
> `JTC_NULL_MAP_HIT` / `JTC_NULL_ROM_HIT` / `JTC_NULL_OUTER_HIT` /
> `JTC_NULL_RAW_HIT` / `JTC_NULL_PROJ_OUTER` / `JTC_NULL_PROJ_RAW`
> fires on those corpora, the sub-block stays.

**Rollback options.**

1. **Land D1 (delete the dead sub-blocks).** After broader-corpus
   probe. Saves ≈37 LoC inside the `null ->` arm. The minimal live
   shape is the 10-line `resolveTypeName(...) → constructClassType(...)`
   block already present.
2. **Land D2 (delete raw-detection `else` clause in `JavaClassifierType ->` block).**
   Same caveat, saves ≈18 LoC.
3. **Restore the original 2-line shape (pre-java-direct).**
   Would regress every B/C reference to `ClassId.topLevel(...)`-only
   and produce unresolved diagnostics for residual JLS-misses and for
   any binary `PlainJavaClassifierType` reference. **Not feasible** —
   path C alone (~50 of the 160 hits) is the PSI-era binary loader's
   contract; the FIR side must own the FQN→ClassId split for those.
4. **Reduce path B's residual hits** by fixing the model-side JLS
   resolver to produce a non-null `classifier` more often (D6 in
   `JTC_CLEANUP_2026_05_24.md` — type-parameter scope enrichment for
   the 12 distinct names that fail today). Does not delete code, just
   moves traffic.

**Verdict: D1 + D2 are deletion candidates pending broader-corpus
probe. The trivial live path (10 lines) is structurally required.
`findClassIdByFqNameString` (52 lines) is structurally required for
path C; the named-package fast path inside it is an optimisation, but
the function as a whole has no rollback path.**

---

### 3.13 `J4` (the remaining helpers)

`findClassIdByFqNameString` (52 lines) — pure FQN→`(package, class)`
split via `FirSymbolNamesProvider`; structurally required for path C
(see §3.12).

`resolveTypeName` (12 lines) — the `null ->` arm's entry point; thin
shell over `findClassIdByFqNameString` plus the
`classifier as? JavaClass` shortcut.

`findOuterTypeArgsFromHierarchy` (25 lines) +
`findTypeArgsForClassInHierarchy` (24 lines) +
`substituteTypeArgs` (22 lines) — JLS work for inherited inner
classes (see §3.3). Structurally required while the model can't
expose `containingClassIds`.

**Verdict: structurally required.**

---

### 3.14 `A1` — `javaAnnotationsMapping.kt` enum-fallback error path

**What it does.** Replaces:
```kotlin
val classId = requireNotNull(enumClassId ?: expectedArrayElementTypeIfArray?.lowerBoundIfFlexible()?.classId)
buildEnumEntryDeserializedAccessExpression { ... }
```
with a branch that emits `buildErrorExpression` when both fail.

**Why it is needed.** java-direct's
`JavaEnumValueAnnotationArgumentOverAst.enumClassId` can legitimately
return `null` (parsing-level test
`testEnumValueArgumentUnimportedFullyQualified`: "Without any import
hint, enumClassId must be null"). The base `requireNotNull` would
throw in that case; the patched shape produces a graceful
`ConeUnresolvedReferenceError`-equivalent. The cluster also includes
a new `import ... resolve.providers.symbolProvider`, but that import
was *introduced* by the patch and is **unused** at HEAD — the
2026-05-25 callback retirement deleted the only consumer.

> **Subtle gain.** The change is actually a defensive improvement
> independent of java-direct: even PSI-loaded annotations could in
> principle hit this path (the existing comment notes
> KT-47702 — "static-imported enum constant in an annotation default
> value"). Replacing `requireNotNull` with `buildErrorExpression`
> turns a crash into a diagnosable error.

**Liveness.** Live on every java-direct annotation argument whose
`enumClassId` resolved to null.

**Rollback options.**

1. **Replace the `else` branch with a hard-throw matching the base
   shape.** Would regress java-direct parsing of unresolved enum
   refs in annotation arguments. **Not feasible** unless java-direct's
   `enumClassId` is also tightened to never be null.
2. **Drop the unused `symbolProvider` import.** Cosmetic, but the
   patch should drop it. **Net +1 LoC saving.**
3. **Inline the redundant `fallbackClassId` lookup.** The else
   branch recomputes `expectedArrayElementTypeIfArray?.lowerBoundIfFlexible()?.classId`
   even though the outer `classId` already includes it as the second
   `?:` operand; if the outer is null, the recomputation will also be
   null. The inner `if (fallbackClassId != null)` branch is
   structurally dead. Could be collapsed to just the
   `buildErrorExpression` arm. **Net −12 LoC saving.**

**Verdict: keep the semantic, but tighten the structure** —
drop the unused `symbolProvider` import (cluster's only purpose-built
import) and collapse the structurally-dead `fallbackClassId`
recompute. Estimated cleanup: −13 LoC on `javaAnnotationsMapping.kt`,
no behavioural change.

---

## 4. Aggregate minimisation budget

Summing the rollback options across all clusters:

| Source of saving | LoC saved on FIR-jvm | Confidence | Net codebase delta |
|---|---:|---|---:|
| §3.4 — delete `JavaTypeParameterWithFirSymbol` interface + matching arm + supertype on `FirBackedJavaTypeParameter` | **~30** | **medium** (pending broader-corpus probe; 0/2793 on JavaUsingAst*) | ~−30 |
| §3.12 — D1 dead sub-blocks in `null ->` arm | **~37** | **medium** (pending broader-corpus probe) | ~−37 |
| §3.12 — D2 raw-detection `else` clause | **~18** | **medium** (pending broader-corpus probe) | ~−18 |
| §3.14 — drop unused import + dead `fallbackClassId` recompute | **~13** | **high** | ~−13 |
| §3.2 — relocate `directSupertypeClassIds` cache into `JavaResolutionContext` (or `FirSessionComponent`) | **~20** | **low** (style win, +cache machinery on java-direct) | ~−10 |
| §3.3 — relocate outer-args recovery driver via java-direct-private subinterface | **~10** | **low** (architecturally cleaner; +20 LoC model-side) | ~+10 |

**Realistic budget for a single follow-up wave**, assuming a
broader-corpus probe is run first:

- Probe-gated: §3.4 + §3.12-D1 + §3.12-D2 ≈ **−85 LoC** on FIR-jvm,
  no java-direct-side cost. **Highest-yield, lowest-risk path.**
- Mechanical cleanup: §3.14 ≈ **−13 LoC**, **already safe to land**.
- Architectural: §3.3 option 2 trades ≈10 LoC for one new model-side
  subinterface and ≈20 LoC of subinterface impl on java-direct; only
  worth doing if the project explicitly wants the
  "FIR-jvm-doesn't-reference-java-direct-classes" boundary tightened.

If all three categories land, the FIR-jvm diff would shrink from
+397 / −53 to roughly **+295 / −53**, ≈25 % reduction on FIR-jvm
overhead vs `ff12cbb3`, with no test regressions.

## 5. Items that are not deletable under any plausible refactor

- §3.1 — `JavaField.hasInitializer` chain (Lombok plugin call site,
  removes a PSI cast → debt reduction, closes upstream TODO).
- §3.6 — `enumEntriesOrigin` Library fallback (one-line carrier of
  "source-Java-without-PSI"; alternative refactors only move it).
- §3.7 — `isCanonicalRecordConstructorForSource` (record semantics
  for source classes; minimal carrier).
- §3.9 — empty-attrs short-circuit (2837 hits, real saving).
- §3.10 — `buildTypeProjections` helper (enabler for §3.12 trivial
  path).
- §3.12 trivial path (10 LoC) + `findClassIdByFqNameString` (52 LoC) +
  `resolveTypeName` (12 LoC) — required for path C (binary
  `PlainJavaClassifierType`) regardless of java-direct's behaviour.
- §3.14 semantic (graceful error vs crash on null `enumClassId`).
- The `MutableJavaTypeParameterStack.containingClassSymbol` plumbing
  if §3.3 option 2 is not pursued.

## 6. Open probes and validation tasks

| Task | Cost | Pay-off |
|---|---|---|
| Re-run `JTC_NULL_MAP_HIT`, `JTC_NULL_ROM_HIT`, `JTC_NULL_OUTER_HIT`, `JTC_NULL_RAW_HIT`, `JTC_NULL_PROJ_OUTER`, `JTC_NULL_PROJ_RAW`, `JTC_RAW_DETECT_HIT`, `JTC_JTP_FIRSYM_HIT` against `KotlinFullPipelineTestsGenerated` (full or SAME_THREAD subset) | ~3-5 hours probe instrumentation + 1 corpus run | Unblocks §3.4 + §3.12 D1/D2 deletions (~85 LoC) |
| Re-run the same probes against `IntelliJFullPipelineTestsGenerated` Java-heavy slice (`testIntellij_platform_*`) | Multi-hour run; subset is OK | Confirms §3.4 (and §3.12) for the Java-heavy production corpus |
| §3.14 mechanical cleanup (drop unused import + collapse dead `fallbackClassId`) | <30 min, no probe needed | −13 LoC immediate |
| Evaluate §3.3 option 2 (java-direct-private `JavaClassifierTypeWithContainingClassIds` subinterface) | Half-day refactor + suite | Architecturally cleaner; net codebase wash; only file if the project wants to tighten the FIR-jvm / java-direct boundary |
| Document this analysis in `INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` §3 (currently lists callbacks already retired; should be updated to also catalogue the FIR-jvm-internal API surface that this diff added) | <1 hour | Keeps the canonical inventory in sync; prevents the "stale doc claims survive a refactor" failure mode flagged at the top of `JTC_CLEANUP_2026_05_24.md` |

## 7. Cross-reference

- `ITERATION_RESULTS.md` — 2026-05-20 Lombok (`F1`+`F2`+`H5`),
  2026-05-24 D1 (`J3`/`J4` deletion candidates), 2026-05-24 implicit
  permits (`H2`), 2026-05-24 D2-A (`J3` trivial-path traffic), 2026-05-25
  TYPE_USE + callback retirements (already-landed cleanups, not in
  this diff).
- `JTC_CLEANUP_2026_05_24.md` — sub-block hit tables that ground the
  liveness assessments in §3.4 and §3.12.
- `INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` — public Java-model
  interface rollback inventory (rule §7 of `AGENT_INSTRUCTIONS.md`).
- `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` — `LazySessionAccess`,
  `FirBackedJavaClassAdapter`, `directSupertypeClassIds` dispatcher
  design.

---

## 8. 2026-05-25 — Landed minimisation wave

Following the user-confirmed broader-corpus safety check, the
realistic budget from §4 was implemented in a single wave:

| Item | Action | LoC delta on FIR-jvm |
|---|---|---:|
| §3.4 `JavaTypeParameterWithFirSymbol` | Interface deleted from `MutableJavaTypeParameterStack.kt`; supertype dropped from `FirBackedJavaTypeParameter` in `compiler/java-direct/.../FirBackedJavaClassAdapter.kt` (kept `firTypeParameterSymbol` as plain `val` for the adapter's own `equals`/`hashCode`/`toString`); stale KDoc references rewritten in `FirBackedJavaClassAdapter.kt` and `JavaTypeOverAst.kt`. | **−19** |
| §3.12-D1 `null →` arm dead sub-blocks | **Pre-landed.** At HEAD the `null →` arm is already the 22-line trivial shape (`resolveTypeName` + `constructClassType` + `buildTypeProjections` / `lowerBound?.typeArguments`). No further deletion possible without breaking the live `JTC_NULL_PROJ_BUILD` (5) + `JTC_NULL_PROJ_LOWER` (155) paths. Confirmed by file inspection (`JavaTypeConversion.kt` lines 284–306). | **0 (already landed)** |
| §3.12-D2 raw-detection `else` clause | **Pre-landed.** The 17-line `else` clause inside the `JavaClassifierType ->` block when `classifier == null && typeArguments.isEmpty()` is no longer present at HEAD; current shape is the minimal 3-arm flexible-type construction (`JavaTypeConversion.kt` lines 124–140). | **0 (already landed)** |
| §3.14 `javaAnnotationsMapping.kt` mechanical cleanup | Removed unused `symbolProvider` import; collapsed the structurally-dead `if (fallbackClassId != null)` inner branch (the outer `?:` already absorbs the same recomputation) and kept only the `buildErrorExpression` arm; KDoc tightened. | **−7** |

`§3.2` (relocate `directSupertypeClassIds` cache) and `§3.3` option 2
(java-direct-private `JavaClassifierTypeWithContainingClassIds`
subinterface) were not pursued — both are net codebase washes
flagged in §4 as "only worth doing if the project explicitly wants
to tighten the FIR-jvm / java-direct boundary".

**Net realised saving on `fir-jvm`**: ≈ **−26 LoC** (§3.4 + §3.14)
on top of the already-landed D1/D2 reductions that the earlier doc
counted as still-pending. The `fir-jvm` diff vs `ff12cbb3` shrinks
from `+397 / −53` to approximately `+371 / −53`.

**Outside `fir-jvm`** (java-direct side):
- `FirBackedJavaClassAdapter.kt`: −5 LoC (supertype + KDoc).
- `JavaTypeOverAst.kt`: comment-only refresh (net 0).

**Verification**: `./gradlew :compiler:fir:fir-jvm:compileKotlin
:compiler:java-direct:compileKotlin` exit 0.

**Items deferred** (architectural; net wash):
- `§3.2` cache relocation — defer until the project commits to the
  `FirSessionComponent` migration (see §9 for what that phrase
  denotes — it is *not* the already-completed `FirSession` injection
  track from Step 4.5a/4.5b).
- `§3.3` outer-args recovery driver relocation — defer until the
  java-direct/FIR-jvm boundary tightening is explicitly scoped.

---

## 9. On the phrase "the `FirSessionComponent` migration"

> **Why this section exists.** §3.2 and §8 both use the phrase
> "the `FirSessionComponent` migration" as a label for the
> architectural wave that would unblock §3.2's cache relocation
> rollback. That phrase is *not* an existing/named project
> initiative — it is shorthand for a hypothetical, not-yet-scoped
> cleanup wave, and prior readings of this doc have conflated it
> with the already-completed `FirSession` injection track from
> Step 4.5a/4.5b. This section pins down what the phrase actually
> denotes so §3.2 and §8 are unambiguous on a fresh read.

### 9.1 Definition

**"The `FirSessionComponent` migration"** = a hypothetical future
cleanup wave that would relocate session-scoped helpers and caches
currently living on individual `FirDeclaration` instances
(`FirJavaClass`, `FirJavaField`, `MutableJavaTypeParameterStack`,
…) into proper `FirSessionComponent` implementations registered on
`FirSession`, matching the dominant FIR-side pattern for cached
lookups and session-scoped services.

It is **not currently scoped as a project initiative**. The phrase
exists only in this analysis doc (§3.2 / §8 / this section) as a
label for the "if the project does this wave, §3.2 falls out for
free; if not, §3.2 is a net wash so we defer" gating.

### 9.2 What it is **not**

- It is **not** a reference to the already-completed *`FirSession`
  injection* track that landed in Step 4.5a/4.5b (callbacks deleted
  from `JavaClassifierType` / `JavaAnnotation` /
  `JavaEnumValueAnnotationArgument`, `FirSession` threaded into
  `JavaResolutionContext`, `JavaTypeConversion.resolveTypeName`
  restored to its pre-`java-direct` body). That work is **done**;
  the model already owns `FirSession`. The canonical sources for
  the injection track are
  `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` (design rationale,
  revised 2026-05-07 to defer the binding contract to its successor)
  and `INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` (the authoritative
  successor; rule §7 of `AGENT_INSTRUCTIONS.md`).
- It is **not** referenced anywhere else in the project's commit
  messages or `implDocs/` as a named milestone or initiative.
- It is **not** the binding-contract wiring iteration ("how does
  `FirSession` reach the Java Model" — late-init on
  `JavaClassFinderOverAst`, restructured entry point); that wiring
  was explicitly out of scope of the injection proposal (§1 of
  `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md`) and landed
  independently.
- It is **not** the `LL-FIR` / `LLFirJavaSymbolProvider` semantics
  story; that remains out of scope per the merged plan (the
  injection migration is compiler-mode only).

### 9.3 Concrete shape for §3.2

The current shape kept at HEAD is the inline `lazy` cache on the
declaration (cluster `C1`):

```kotlin
// compiler/fir/fir-jvm/.../declarations/FirJavaClass.kt
private val directSupertypeClassIdsCache: List<ClassId> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    javaClass.supertypes.mapNotNull { (it.classifier as? JavaClass)?.classId }
}
fun directSupertypeClassIds(): List<ClassId> = directSupertypeClassIdsCache
```

The §3.2 "relocation" option would replace that per-instance cell
with a `FirSession`-scoped component, e.g.:

```kotlin
class FirJavaDirectSupertypeCache(val session: FirSession) : FirSessionComponent {
    private val cache: FirCache<ClassId, List<ClassId>, FirJavaClass> =
        session.firCachesFactory.createCache { _, owner ->
            owner.javaClass.supertypes.mapNotNull { (it.classifier as? JavaClass)?.classId }
        }
    fun get(owner: FirJavaClass): List<ClassId> = cache.getValue(owner.classId, owner)
}
val FirSession.javaDirectSupertypeCache: FirJavaDirectSupertypeCache by FirSession.sessionComponentAccessor()
```

The +20 LoC currently sitting on `FirJavaClass` would move into a
new component file in `fir-jvm` (or, more aggressively, into
`compiler/java-direct` itself so that `fir-jvm` carries no
Java-direct-specific caching at all).
`JavaResolutionContext.directSupertypeClassIds(classId)` would call
`session.javaDirectSupertypeCache.get(firJavaClass)` instead of
`firJavaClass.directSupertypeClassIds()`.

### 9.4 Why it follows FIR's dominant idiom

The relocation simply applies the canonical FIR pattern for
session-scoped services and caches. A non-exhaustive sample of the
≈40 existing implementations across `compiler/fir/providers/`:

- `FirCorrespondingSupertypesCache` (`types/FirCorrespondingSupertypesCache.kt`)
  — directly analogous in purpose (cached supertype lookups keyed
  by `LookupTag` / `ClassId`).
- `FirSymbolProvider` (itself a `FirSessionComponent`), `FirProvider`,
  `FirKotlinScopeProvider`, `FirDeclaredMemberScopeProvider`,
  `FirOverrideService`, `FirDeclarationOverloadabilityHelper`,
  `FirVisibilityChecker`, `FirExtensionDeclarationsSymbolProvider`,
  `FirMissingDependencyStorage`, `FirCachesFactory`,
  `FirHiddenDeprecationProvider`, `FirInlineConstTrackerComponent`,
  …

So the "migration" is just: **apply the same `FirSessionComponent`
idiom that the rest of FIR uses for cached lookups** to the
`directSupertypeClassIds` cache (and, conceivably, to other
per-declaration caches the `java-direct` wave has accreted).

### 9.5 Why it is called a "migration" and why it is deferred

The relocation is a **net codebase wash** rather than a saving:

- `fir-jvm` shrinks by ≈20 LoC (the cache cell + accessor on
  `FirJavaClass` goes away).
- The new `FirSessionComponent` class + the
  `sessionComponentAccessor()` wiring + the registration in
  `FirJvmSessionComponentRegistrar` (or similar) adds ≈25–30 LoC
  elsewhere — net delta ≈ zero, possibly slightly negative.
- The pay-off is **architectural cleanliness**: the cache lives at
  the same layer as other FIR-internal caches; `FirJavaClass`
  reverts closer to the `ff12cbb3` shape; the per-origin dispatcher
  in `JavaResolutionContext` becomes the canonical entry point
  rather than a thin wrapper over a declaration method.

It is therefore a *"project commits to"* decision rather than a
free win — file it only when the cleanliness justifies the +25 LoC
boilerplate-per-component tax that registering a new
`FirSessionComponent` always pays.

### 9.6 Other clusters eligible for the same treatment

The migration is **not unique to §3.2**. Several other clusters
in this diff are eligible for the same relocation if the project
commits to the cleanup wave:

- `S1` (cluster in `MutableJavaTypeParameterStack`) —
  `containingClassSymbol` could be replaced by a session-component
  carrying a `Map<JavaClass, FirRegularClassSymbol>` view,
  decoupling the model-side stack from FIR-side identity.
- `J4`'s outer-type-args recovery helpers
  (`findOuterTypeArgsFromHierarchy` / `findTypeArgsForClassInHierarchy`
  / `substituteTypeArgs`) are pure functions on `FirSession` +
  `ClassId` + `MutableJavaTypeParameterStack`; they would be a
  natural fit for a `FirJavaInheritedTypeArgsResolver : FirSessionComponent`.

"Commits to the `FirSessionComponent` migration" therefore means:
**the project decides as a whole to rehouse the `java-direct`-era
helper state from declaration objects into proper session
components, treating it as one named wave rather than per-cluster
cherry-picks** — because doing it piecemeal pays the per-component
+25 LoC tax once per cluster, whereas doing it as a wave amortises
the `FirSessionComponentRegistrar` boilerplate.

### 9.7 TL;DR

The `FirSessionComponent` migration is a **prospective, project-
level cleanup wave** that would move `java-direct`-era helper state
(caches, lookup tables, stack-companion fields) from `FirJavaClass`
/ `MutableJavaTypeParameterStack` / etc. into proper
`FirSessionComponent`-style services registered on `FirSession`,
matching FIR's dominant pattern. It is not currently scoped. §3.2
is deferred because its sole pay-off is architectural cleanliness,
and the LoC delta is essentially zero — so it is only worth doing
as part of a broader committed cleanup pass, not as a one-off
relocation.

---

*Generated 2026-05-25 against HEAD `3637c96c96b0` / base
`ff12cbb3d915`. No source changes were made while preparing this
document.*
*Updated 2026-05-25 after the minimisation wave landed (§8) and
again to add §9 ("On the phrase 'the `FirSessionComponent`
migration'") pinning down the meaning of the deferral label.*
