# Java-Direct Performance Review

**Date**: 2026-04-20
**Previous version**: archive/PERFORMANCE_REVIEW.md (2026-04-17)

**Context**: java-direct is ~20% slower than the PSI-based approach on large pipeline tests
(`KotlinFullPipelineTestsGenerated` and similar). The original review (archived) identified a set
of hot spots; a significant fraction of them have been addressed by the two "another set of
optimizations" commits and by the LightTree migration (Phases 1–4). This document supersedes the
archived review: it records what was done, what is still relevant, and adds findings that surfaced
during the re-audit.

---

## What changed since the original review

- **AST representation switched to flat-array `JavaLightTree`** (LightTree migration Phases 1–4).
  Eliminates per-node `JavaSyntaxNode` object overhead and `List<JavaSyntaxNode>` children
  allocation — closes the original reviews's items 5.1 and 5.2 (the two largest structural
  contributors to the gap vs. PSI).
- **`@Volatile`-backed cached properties** replaced `by lazy(PUBLICATION)` on the highest-volume
  model classes: `JavaClassOverAst`, `JavaMemberOverAst` and its subclasses,
  `JavaClassifierTypeOverAst`. Closes most of 1.1.
- **`rawTypeNameParts`** added as a cached split of `rawTypeName` — closes 2.1. The dependent
  properties (`classifier`, `classifierQualifiedName`, `isTriviallyFlexibleHint`) now also
  consult the cached `classifier` first, closing 2.2.
- **`resolveNestedClassToClassIdFromParts`** passes `List<String>` through the recursion,
  avoiding O(n²) split/join — closes 2.3.
- **`FileEntry.fileBaseName`** is precomputed once instead of recomputed per entry in
  `knownClassNamesInPackage` — closes 3.4.
- **`JavaTypeOverAst.annotations`** is now cached (via `by lazy`) — partially closes 1.3 for the
  common type path.
- **Per-call `HashMap` in `resolve()`** is now allocated only for dotted names (the rare path);
  simple names skip it entirely. Partially closes 3.1.

## Executive summary of what is still relevant

The single biggest remaining structural gap (eager `buildIndex()` + retained AST) is
architectural and intentionally out of scope for incremental optimization.

Among the smaller remaining items, four deliver most of the value and fit within a single
session:

1. Cache `annotations` / `modifierList` on `JavaValueParameterOverAst` and
   `JavaTypeParameterOverAst` — currently these are recomputed on every access.
2. Cache `JavaAnnotationOverAst.annotationName` (and its derivatives `classId` / `isResolved`).
3. Convert `JavaTypeOverAst.annotations` from `by lazy` to the `@Volatile` pattern for
   consistency with the rest of the model layer.
4. Replace `getOrPut` on `ConcurrentHashMap` caches with `computeIfAbsent` for the expensive
   deterministic computations (supertype graph, inherited inner classes) to prevent
   concurrent double-compute.

Two thread-safety caveats were also found during the re-audit (see §6 below) that are unrelated
to performance but worth addressing in the same pass: two plain `HashMap` caches live on paths
that the rest of the module assumes to be concurrent.

---

## 1. Status of original review items

Each row is keyed to the corresponding section number in the archived review.

| # | Original item | Status | Evidence / remaining gap |
|---|---------------|--------|--------------------------|
| 1.1 | 40 `by lazy(PUBLICATION)` delegates on model objects | **Mostly done** | `@Volatile`-backed fields in `JavaClassOverAst`, `JavaMemberOverAst`, `JavaClassifierTypeOverAst`. Three residual delegates — see §2 items 3–5. |
| 1.2 | AST trees retained indefinitely | **Not done** | Every cached `JavaClassOverAst` still holds `node` + `tree`. Architectural; see §5. |
| 1.3 | `annotations` property creates objects on every access | **Partially done** | `JavaTypeOverAst.annotations` cached; `JavaTypeParameterOverAst.annotations` and `JavaValueParameterOverAst.annotations` still uncached `get()` properties. |
| 2.1 | `rawTypeName.split('.')` called 4× | **Done** | `rawTypeNameParts` cached. |
| 2.2 | `classifier` / `classifierQualifiedName` / `isTriviallyFlexibleHint` overlap | **Done** | Dependents consult the cached `classifier` first; `@Volatile` caching removes the remaining overlap. |
| 2.3 | `resolveNestedClassToClassId` O(n²) string work | **Done** | `resolveNestedClassToClassIdFromParts` operates on `List<String>`. |
| 2.4 | Duplicated `resolve*WithoutInheritance` / `resolve*` | **Not done** | Maintenance risk only. |
| 3.1 | Per-call `HashMap` in `resolve()` | **Partially done** | Only the dotted-name path allocates; simple names are now HashMap-free. A context-level cache across calls would close the rest. |
| 3.2 | `Name.identifier()` allocations in resolution paths | **Not done** | Still called per probe in `resolveSimpleNameToClassId` and star-import loops. Minor but cumulative. |
| 3.3 | `subPackagesOf` O(n) with string allocs | **Not done** | `removePrefix` + multiple `asString()` per entry. Low frequency, low priority. |
| 3.4 | `knownClassNamesInPackage` per-entry `removeSuffix` | **Done** | `FileEntry.fileBaseName` precomputed. |
| 4.1 | `buildIndex()` front-loads file I/O | **Not done** | Architectural; see §5. |
| 4.2 | `SMALL_FILE_SIZE_THRESHOLD` unprofiled | **Not done** | Still 4096, no profile data. |
| 5.1 | Materialized AST vs. LightTree | **Done** | `JavaLightTree` (flat-array) in place. |
| 5.2 | `List<JavaSyntaxNode>` children allocation | **Done** | Children derived on demand from the flat array. |

---

## 2. Still-relevant items, ranked by priority

### Quick wins (expected to be doable in one session)

1. **Cache `JavaValueParameterOverAst.annotations` and `modifierList`**
   (`compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt`, bottom of file).
   Both are plain `get()` properties. Every read of `type`, `annotations`, and `isVararg`
   re-walks the AST; `annotations` also re-allocates `JavaAnnotationOverAst` wrappers on each
   read. Parameters are iterated repeatedly during FIR overload resolution.

2. **Cache `JavaTypeParameterOverAst.annotations`**
   (`compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`).
   Same shape — still a `get()` property recomputing on every access.

3. **Cache `JavaAnnotationOverAst.annotationName` (and its derivatives)**
   (`compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaAnnotationOverAst.kt`).
   `annotationName` is accessed 3× per annotation (from `classId`, `isResolved`, and
   `resolveAnnotation`). A single `@Volatile String?` cache removes the repeated AST walk for
   all three.

4. **Convert `JavaTypeOverAst.annotations` from `by lazy` to `@Volatile`**
   (`compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt:29-42`).
   This is the base-class delegate inherited by every `JavaType*OverAst` — highest instance
   count of any remaining delegate. Consistent with the rest of the module and saves the
   `SafePublicationLazyImpl` wrapper per type instance.

5. **Replace `getOrPut` with `computeIfAbsent` on `ConcurrentHashMap` caches**
   where the compute is expensive and deterministic:
   - `JavaSupertypeGraph.supertypeCache` (re-parses files on cold path)
   - `JavaSupertypeGraph.inheritedInnerClassesCache` (recursive walk)
   - `JavaClassFinderOverAstImpl.classCache` / `packageCache`
   - `JavaClassOverAst.innerClassCache`

   `getOrPut` on `ConcurrentHashMap` is non-atomic — two concurrent threads can both compute
   the value and one result is dropped. For the first two caches this wastes real work
   (file re-parsing, supertype walks); for the others it's closer to a wash but should be
   made atomic for correctness-by-construction.

### Medium effort

6. **Context-level `tryResolve` cache across `resolve()` calls.** Today's per-call HashMap
   (only for dotted names) disappears between calls. A per-`JavaResolutionContext` cache
   (scoped to one compilation unit, where the callback is deterministic) would memoize
   prefixes reused across different type references in the same file.

7. **Deduplicate `resolveSimpleNameToClassId` / `resolveSimpleNameToClassIdWithoutInheritance`**
   (and the nested counterparts). Near-identical logic lives in both; a single parametrized
   implementation would prevent quiet divergence when one is updated.

8. **Profile `SMALL_FILE_SIZE_THRESHOLD`.** 4096 is a guess. Measure total wall time with
   2 KB / 4 KB / 8 KB / 16 KB on a representative pipeline workload and pick the winner.

### Low value (include if convenient, skip otherwise)

9. **Cache `Name.identifier()` for the first segment of multi-part names.** Same name is
   wrapped fresh on every probe through star imports / same-package / `java.lang`.

10. **`subPackagesOf` string allocations.** Precompute `pkg.asString()` once per index entry,
    or keep a `String`-keyed shadow index. Called infrequently.

### Architectural (weeks-scale — explicitly out of scope for incremental work)

11. **Lazy per-package indexing** instead of eager `buildIndex()`. Only parse/index files in a
    package the first time that package is queried.
12. **AST release after extraction.** Eagerly extract everything the model layer needs during
    construction, then drop `node` + `tree`. Matches the PSI stub pattern.

---

## 3. Catalog of residual uncached `get()` properties

A re-audit surfaced several places in the model layer where a `get()` property recomputes
from the AST on every access. These are the specific targets behind §2 items 1–4.

| Class | Property | File | Per-read cost |
|-------|----------|------|---------------|
| `JavaTypeOverAst` | `annotations` | `JavaTypeOverAst.kt:29-42` | `by lazy` (OK but inconsistent) |
| `JavaTypeOverAst` | `filterTypeUseAnnotations` | `JavaTypeOverAst.kt:44-69` | Re-walks AST; also called from FIR per type read. Worth considering a snapshot when both are required. |
| `JavaTypeParameterOverAst` | `annotations` | `JavaTypeOverAst.kt:546-556` | Full re-walk, new wrappers |
| `JavaTypeParameterOverAst` | `name` | `JavaTypeOverAst.kt:528-531` | Re-walk + `Name.identifier` allocation |
| `JavaValueParameterOverAst` | `annotations` | `JavaMemberOverAst.kt` (bottom) | Full re-walk, new wrappers |
| `JavaValueParameterOverAst` | `modifierList` | `JavaMemberOverAst.kt` (bottom) | Re-walk per read of `type` / `annotations` / `isVararg` |
| `JavaValueParameterOverAst` | `type` | `JavaMemberOverAst.kt` (bottom) | Re-builds a fresh `JavaType` each read |
| `JavaAnnotationOverAst` | `annotationName` | `JavaAnnotationOverAst.kt:36-37` | Re-walks AST; read 3× via `classId` / `isResolved` / `resolveAnnotation` |
| `JavaAnnotationOverAst` | `classId` | `JavaAnnotationOverAst.kt:39-53` | Rebuilds `ClassId` + `FqName` per read |
| `JavaAnnotationOverAst` | `arguments` | `JavaAnnotationOverAst.kt:21-27` | Re-walks AST and re-wraps children |

These are all cheap individually; the cumulative cost comes from FIR calling them multiple
times per symbol during overload resolution, annotation processing, and type enhancement.

---

## 4. ConcurrentHashMap audit

The module uses `ConcurrentHashMap` in 9 places. 6 are genuinely needed for concurrent FIR
resolution; 3 are overkill (built during `init{}`, read-only thereafter) and could be plain
`HashMap` if safe publication through the `final` constructor is documented. No map is
missing that should be there except for the two plain `HashMap` caches noted in §6.

| Map | File / line | Justified? | Notes |
|-----|-------------|-----------|-------|
| `classCache` | `JavaClassFinderOverAstImpl.kt:67` | **Yes** | Written during `findClass` under concurrent FIR resolution. |
| `negativeClassCache` | `JavaClassFinderOverAstImpl.kt:77` | **Yes** | Same. |
| `packageCache` | `JavaClassFinderOverAstImpl.kt:80` | **Yes** | `getOrPut` from `findPackage`. |
| `supertypeCache` | `JavaSupertypeGraph.kt:44` | **Yes** | Written during concurrent type resolution. |
| `inheritedInnerClassesCache` | `JavaSupertypeGraph.kt:47` | **Yes** | Same. |
| `innerClassCache` | `JavaClassOverAst.kt:156` | **Yes** | Written during concurrent type resolution via `findInnerClass`. |
| `index` (outer) | `JavaClassFinderOverAstImpl.kt:62` | **Overkill** | Written only inside `buildIndex()` in `init{}`; frozen afterwards. Safe publication via `final` fields + `init{}` makes a plain `HashMap` correct. |
| `index` inner maps | `JavaClassFinderOverAstImpl.kt:190` | **Overkill** | Same — all writes happen in `buildIndex`. |
| `packageAnnotationNodes` | `JavaClassFinderOverAstImpl.kt:83` | **Overkill** | Populated only in `indexPackageInfo` during `buildIndex`; read-only afterwards. |

**Recommendation**: the 6 justified maps should additionally switch from `getOrPut` to
`computeIfAbsent` on the expensive ones (see §2 item 5). The 3 overkill maps can be downgraded
to `HashMap` as a documentation-quality improvement; the performance difference is minor but
the "this is read-only after `init{}`" invariant is clearer in code.

---

## 5. Architectural items (out of scope for incremental optimization)

Two items remain from the original review that would each require a multi-day refactor:

- **Lazy per-package indexing** (§2 item 11 above). Today's constructor walks the entire
  source tree and parses every small file (≤ 4 KB). PSI avoids this via pre-built IDE indexes.
  A lazy design would only touch files when their package is first queried.
- **AST release after extraction** (§2 item 12). The LightTree migration shrank per-node
  overhead but did not eliminate retention: every cached `JavaClassOverAst` still holds its
  tree and source text. Eager extraction into plain fields, followed by nulling `tree`, would
  free this memory at the cost of losing lazy semantics.

Both are the "architectural" tier of the archived review. They should remain documented but
not scheduled unless profiling shows that per-run memory or cold-start cost is actually the
dominant remainder of the 20% gap.

---

## 6. Thread-safety caveats found during re-audit

These are not performance items; they are concurrency correctness issues that surfaced while
auditing the `ConcurrentHashMap` usage and share the same fix scope.

The rest of the module assumes FIR resolution is concurrent (every other cache is
`ConcurrentHashMap`). Two plain `HashMap` caches live on hot paths that look racy under that
same assumption:

- **`JavaScopeResolver.findLocalClassCache`** (`JavaScopeResolver.kt:43`). Shared across
  contexts produced by `withTypeParameters` / `withInheritedTypeParameters`, written on
  first access from `findLocalClass`. If FIR resolves multiple members of the same class
  concurrently, this races.
- **`localClassCache` inside `JavaResolutionContext.Companion.create`**
  (`JavaResolutionContext.kt`, `localClassCache = mutableMapOf<Name, JavaClass>()`). Shared
  across all users of that resolution context; same risk.

**Recommendation**: either promote both to `ConcurrentHashMap` (plus `computeIfAbsent`), or
document a precise threading-contract that explains why they are allowed to be plain.

---

## 7. Priority roadmap

| Tier | Items | Expected effort | Expected benefit |
|------|-------|-----------------|------------------|
| Quick wins | §2 items 1–4 | 1 session | Closes remaining per-access allocations in the model layer; keeps the module internally consistent on caching patterns. |
| Quick wins | §2 item 5 | 1 session | Prevents concurrent double-compute in the two most expensive caches. |
| Quick wins | §6 caveats | 1 session | Closes two real thread-safety bugs latent under the module's own concurrency assumptions. |
| Medium | §2 items 6–8 | 2–4 hours each | Context-level resolve cache, deduplication, threshold profiling. |
| Low | §2 items 9–10 | < 1 hour each | Marginal improvements for infrequent paths. |
| Architectural | §5 | Weeks | Only if profiling shows memory/cold-start dominate the remainder of the gap. |

---

## 8. Measurement methodology

Unchanged from the archived review. The `AtomicLong` counters, JFR capture, and comparative
PSI-vs-java-direct test runs documented there still apply. Before and after each change in §7,
run the full `KotlinFullPipelineTestsGenerated` suite with `--rerun-tasks` and record the
delta.
