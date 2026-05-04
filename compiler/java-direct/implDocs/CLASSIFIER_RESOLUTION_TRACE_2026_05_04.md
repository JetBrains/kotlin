# Classifier Resolution Trace: `Derived extends Base`

This document walks through what happens when java-direct converts the `Base`
reference in `Derived extends Base` for the canonical
`testSimpleHierarchy` (`compiler/testData/diagnostics/tests/jvm/javaDirect/simpleHierarchy.kt`).
It complements [RESOLUTION_PIPELINE.md](RESOLUTION_PIPELINE.md) — the pipeline
doc gives the abstract layering; this one is the concrete trace for the
simplest cross-file same-package supertype reference, plus an honest assessment
of what is already efficient and where the implementation does redundant work.

The reason for writing it down: every "make resolution faster" suggestion in a
review touches at least one of the steps below. Having one canonical trace to
diff against avoids re-deriving the call chain from scratch in each
performance review.

---

## Test setup

```java
// FILE: foo/Base.java
package foo;
public class Base { public void foo() {} }

// FILE: foo/Derived.java
package foo;
public class Derived extends Base {}
```

The reference under analysis is the `Base` token inside `Derived.java`'s
`EXTENDS_LIST`. It is:

- a **simple name** (no dots, no package qualifier),
- in a **different file** from its declaration,
- in the **same package** as its declaration,
- with **no explicit import**.

That combination matters: it forces the resolver through the same-package
fallback at the bottom of the JLS chain, which in turn forces a round-trip
through the FIR symbol provider. None of the earlier resolution steps
(type parameters, local classes, inherited inner classes, explicit imports)
match.

---

## Call stack from `FirJavaClass(Derived)` to `ClassId(foo, Base)`

```
FirJavaClass(Derived).superTypeRefs evaluation
└─ JavaClassOverAst.supertypes                    JavaClassOverAst.kt
   └─ build new JavaClassifierTypeOverAst("Base") on every read (NOT cached)
      └─ JavaTypeConversion.toConeTypeProjection (JavaClassifierType branch)
         └─ toConeKotlinTypeForFlexibleBound      JavaTypeConversion.kt
            ├─ classifier ── computeClassifier()  JavaTypeOverAst.kt
            │     ├─ findTypeParameter("Base")               miss
            │     ├─ findLocalClass("Base")                   miss
            │     │   └─ JavaScopeResolver.findLocalClass     miss
            │     │       └─ localClassProvider("Base")       miss (different file)
            │     ├─ findInheritedTypeParameter("Base")       miss
            │     └─ findLocalClass("Base") again — for parts.size==1 the
            │         multi-part navigation branch repeats the same lookup
            │  → returns null
            └─ classifier == null branch
               ├─ classifierQualifiedName  — recomputes classifier, returns "Base"
               ├─ isResolved              — recomputes classifier again
               └─ resolveTypeName → resolveSymbolBasedClassId(session)
                  └─ JavaClassifierType.resolve(tryResolve, getSupertypeClassIds)
                     └─ JavaResolutionContext.resolve("Base", …)
                        └─ resolveSimpleNameToClassIdImpl
                           ├─ resolveFromExplicitImport            null
                           ├─ resolveFromLocalScope                null
                           │  ├─ findLocalClass("Base")             miss (3rd time)
                           │  └─ aggregated inherited inner classes empty
                           ├─ resolveFromSamePackage(foo, "Base")
                           │  └─ tryResolve(ClassId(foo, Base))
                           │     └─ symbolProvider.getClassLikeSymbolByClassId
                           │        └─ CombinedJavaClassFinder.findClass
                           │           ├─ BinaryJavaClassFinder.findClass    miss
                           │           └─ JavaClassFinderOverAstImpl.findClass
                           │              └─ JavaClassCache.getOrPutIfNotNull
                           │                 → parse foo/Base.java once
                           │  → ClassId(foo, Base)
                           ├─ resolveFromJavaLang                  not reached
                           └─ resolveFromStarImports               not reached
```

Result: `ClassId(foo, Base)` with the parsed `JavaClassOverAst` cached. FIR
wraps this in a `ConeFlexibleType` (`Base..Base?` rendered as `Base!`) via the
`isTriviallyFlexibleHint` path because the resolved class is not in
`JavaToKotlinClassMap.getReadOnlyAsJava()`.

---

## What is already optimal

The architecture is correctly factored — model, resolution, finder, and FIR
conversion are cleanly separated. Inside that factoring, several caches are
already pulling their weight:

- **`JavaClassCache.getOrPutIfNotNull`** memoises both `findClass` results and
  the parsed `JavaClassOverAst` for `foo/Base.java`. Even though FIR's
  `tryResolve` callback can probe `ClassId(foo, Base)` repeatedly, parsing
  happens exactly once.
- **`packageIndexer.ensurePackageIndexed`** is per-package idempotent: one
  directory walk for `foo/`, then it is a `HashMap` lookup for the rest of the
  compilation.
- **`JavaResolutionContext.resolve` for dotted names** caches `tryResolve`
  results in a per-call `HashMap`, so prefix-splitting in
  `resolveNestedClassToClassIdFromParts` does not duplicate symbol-provider
  probes for shared prefixes (`com.x` for every deeper dotted reference).
- **`inheritedInnerCache` / `getAggregatedInheritedInnerClasses`** is per-
  resolution-context lazy + volatile, sharing one map across
  `withTypeParameters` / `withInheritedTypeParameters` derivatives.
- **`innerClassCache` on `JavaClassOverAst`** keeps `JavaTypeParameter`
  identity stable, which FIR requires.
- **`JavaSupertypeGraph` + `LeanJavaClassFinder.collectInheritedInnerClasses`**
  keeps "supertypes of a Java source class" as a separate cheaper query —
  important because `getResolvedSupertypeClassIds` deliberately refuses to
  walk Java source supertypes via `superTypeRefs` (would recurse into the
  exact code path traced above).

---

## What is redundant for this case

Concrete excess work observed for one `Derived → Base` edge in the trace
above. Each item is a candidate optimisation — order roughly by expected
payoff per implementation cost.

### 1. `JavaClassOverAst.supertypes` is rebuilt on every read

```kotlin
override val supertypes: Collection<JavaClassifierType>
    get() {
        val result = mutableListOf<JavaClassifierType>()
        ... // build EXTENDS_LIST + IMPLEMENTS_LIST
        return result
    }
```

A fresh `mutableListOf(...)` plus a fresh `JavaClassifierTypeOverAst` per
supertype reference is allocated on every property read. FIR reads
`supertypes` at supertype-resolution time per `FirJavaClass`, but
enhancement, override checking, and inherited-inner-class queries can re-read
it. A `@Volatile`-backed cache (same shape as `_typeParameters` directly above
this property in `JavaClassOverAst.kt`) eliminates redundant allocations
without changing semantics.

### 2. `JavaClassifierTypeOverAst` does not memoise `classifier`

`computeClassifier()` runs at least three times during a single
`toConeKotlinTypeForFlexibleBound` invocation:

- once for `classifier`,
- once inside `classifierQualifiedName`,
- once inside `isResolved`.

For our `Base` case, each run does the same `findTypeParameter`, `findLocalClass`,
`findInheritedTypeParameter` traversal of `Derived`'s scope and produces
`null`. `isTriviallyFlexibleHint` adds a fourth pass through
`classifierQualifiedName`, which itself recomputes the classifier.

Caching `classifier` (e.g. `Lazy<JavaClassifier?>` or a sentinel-encoded
`@Volatile` field) is a one-line fix and would dominate the savings on this
edge.

### 3. `computeClassifier` does the same `findLocalClass` twice

For `parts.size == 1`, step 2 of the simple-name branch and the start of the
multi-part navigation branch both call
`resolutionContext.findLocalClass(Name.identifier(parts[0]))`. A single call
followed by `if (parts.size == 1) return current` is equivalent and faster.

### 4. `resolveSimpleNameToClassIdImpl` calls `findLocalClass` a third time

Inside `resolveFromLocalScope`, after `computeClassifier` already concluded
the simple-name local lookup misses, the same lookup is performed again.
For cross-file references the local-scope step is a guaranteed miss. An
"AST classifier was already null" hint threaded through `resolve(...)` would
let the resolver short-circuit step 2 and skip the inner-class hierarchy
walk on `Derived`.

### 5. Same-package resolution always round-trips through the FIR symbol provider

For `Base`, `tryResolve(ClassId(foo, Base))` goes:

```
symbolProvider.getClassLikeSymbolByClassId
 → JvmSymbolProvider
 → CombinedJavaClassFinder.findClass
   → BinaryJavaClassFinder.findClass            miss (no foo/Base.class)
   → JavaClassFinderOverAstImpl.findClass       hit (already in index)
```

The source half *already knew the answer cheaply*:
`JavaClassFinderOverAstImpl.isClassInIndex(ClassId(foo, Base))` is just a
hash hit on `packageIndexer.ensurePackageIndexed("foo")` — and the package
was indexed already for `Derived`'s own resolution.

A "is this a same-package source class?" pre-check using
`LeanJavaClassFinder` directly from the resolution context (already wired in
via `unitContext.classFinder`) would let `resolveFromSamePackage` answer
without touching the FIR symbol provider in the most common case. Binary
classes in the same package fall through to the existing path, so the
fast-path is purely additive.

### 6. The `classifier == null` branch in `JavaTypeConversion` does duplicate symbol-provider work

`resolveSymbolBasedClassId` produces a `ClassId`, then the conversion code
calls `classId.toLookupTag().toRegularClassSymbol(session)?.typeParameterSymbols`
to compute `hasTypeParams` for raw-type detection. That is the same lookup
`tryResolve` already performed inside `resolve(...)`. Caching the
`tryResolve(classId)` symbol on the `JavaClassifierType` so the post-
resolution `toRegularClassSymbol` call piggy-backs on it removes the duplicate.
Same point applies to `isRaw` for cross-file types.

### 7. `isTriviallyFlexibleHint` re-runs the entire chain

The current implementation walks `classifier`, `classifierQualifiedName`,
`isUnambiguouslyCrossFileClass`, and the `JAVA_READ_ONLY_FQ_NAMES` set
membership test. With (2) and (6) in place, this collapses to a single set
membership test against the cached resolved `ClassId`.

---

## Net assessment

The end state — `ClassId(foo, Base)` resolved from same-package via
`JavaClassFinderOverAstImpl`, parse cached in `JavaClassCache` — is
correct, and every step in the trace exists for a reason. The architecture
itself does not need restructuring: the model / resolution context / package
indexer / class cache / FIR conversion split is the right one, and each
boundary serves a real concern.

The cost is **per-property recomputation in `JavaClassifierTypeOverAst`** plus
the **un-cached `JavaClassOverAst.supertypes` rebuild** on top. For one
simple cross-file same-package reference the current implementation:

- builds a new `JavaClassifierTypeOverAst` for every read of `supertypes`,
- runs `computeClassifier()` 3–4 times against `Derived`'s scope, each time
  doing the same scope / inner / inherited walk,
- runs the JLS 5-step resolution chain through the FIR symbol provider,
  which round-trips through `CombinedJavaClassFinder` for what could have
  been a same-package `LeanJavaClassFinder` hit,
- never caches the resolved `ClassId` on the `JavaClassifierType`, so any
  subsequent property read (`isRaw`, `isTriviallyFlexibleHint`,
  `classifierQualifiedName`) re-runs the chain.

Two small caches close most of this gap:

1. `@Volatile` cache for `JavaClassOverAst.supertypes` (item 1).
2. `Lazy<JavaClassifier?>` (or sentinel-encoded `@Volatile`) for
   `JavaClassifierTypeOverAst.classifier` (item 2). Subsumes (3) and (4) in
   practice.

Two slightly larger improvements close the rest:

3. Same-package `LeanJavaClassFinder` short-circuit in
   `resolveFromSamePackage` (item 5).
4. Cache the resolved `ClassId` on the `JavaClassifierType` and reuse it for
   raw-type / flexibility decisions (items 6 and 7).

In short: **functionally optimal — every step has a reason and the result is
correct — but the per-property recomputation in `JavaClassifierTypeOverAst`
is the obvious place to optimise next**, and the same-package fast path in
`resolveFromSamePackage` is the second.

---

## Cross-references

- [RESOLUTION_PIPELINE.md](RESOLUTION_PIPELINE.md) — the abstract pipeline
  this trace is a concrete instantiation of.
- [ARCHITECTURE.md](ARCHITECTURE.md) — module layering and component
  responsibilities.
- `JavaTypeOverAst.kt` — `JavaClassifierTypeOverAst.computeClassifier`,
  `classifierQualifiedName`, `isResolved`, `isTriviallyFlexibleHint`.
- `JavaResolutionContext.kt` — `resolve` /
  `resolveSimpleNameToClassIdImpl`.
- `JavaClassFinderOverAstImpl.kt` — `findClass` / `isClassInIndex` and the
  `LeanJavaClassFinder` companion.
- `JavaTypeConversion.kt` (shared FIR) — `toConeKotlinTypeForFlexibleBound`
  null-classifier branch.

Test data: `compiler/testData/diagnostics/tests/jvm/javaDirect/simpleHierarchy.kt`.

---

*Created: 2026-05-04 (`testSimpleHierarchy` trace + redundancy assessment)*
