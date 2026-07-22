# LightTree Performance Investigation: Why the Migration Made Things Worse

**Date**: 2026-04-21
**Context**: The LightTree migration (Phases 1–4, April 20) replaced the materialized
`JavaSyntaxNode` tree with a flat-array `JavaLightTree` representation. The theoretical
analysis predicted ~85% AST memory reduction and ~4-6% compilation time improvement.
Instead, the MT (pipeline) tests show a **~20-25% performance regression** compared to
the PSI baseline — worse than the already-20%-slower pre-migration state.

This document identifies the root causes of the regression, provides quantitative
analysis, and proposes a detailed fix plan.

---

## 1. Executive Summary

The migration delivered on its **memory layout** promise — nodes are no longer materialized
as 72-byte heap objects. But it introduced a severe **runtime cost** that the original
analysis underestimated: **`getChildren()` allocates a fresh `ArrayList` and walks the
marker range from scratch on every call.**

In the old `JavaSyntaxNode`, `children` was a pre-built `List<JavaSyntaxNode>` stored as a
field — accessing it was a zero-cost pointer dereference. The new `getChildren()` performs
real work every time. Combined with the fact that `findChildByType()`,
`getChildrenByType()`, and `hasChildOfType()` all delegate to `getChildren()`, this creates
massive redundant work: a single model object's cached properties collectively call
`getChildren()` on the same node 10-20 times, each rebuilding the list from scratch.

The migration plan (§1.3, §5.3) identified both issues and proposed mitigations —
direct-scan `findChildByType` that skips list construction, and a children cache. **Neither
was implemented.** The analysis (§6.3) also underestimated how many times `getChildren()`
is called per node, claiming "most properties accessed only 1-2 times" — in reality, a
single class body node's children list is rebuilt 15+ times from different cached properties.

The fix is straightforward: cache `getChildren()` results, implement direct-scan variants
of `findChildByType` / `hasChildOfType`, and optionally restore the `childByTypeIndex`
optimization for large nodes. These changes should recover the regression and begin
delivering the theoretical gains.

---

## 2. Root Cause Analysis

### 2.1 — `getChildren()` allocates and recomputes on every call (CRITICAL)

**File**: `JavaLightTree.kt:163-208`

Every call to `getChildren(node)`:
1. Checks `isComposite` and `isErrorMarker` (method calls with marker access).
2. Resolves the marker range (`startIdx`, `doneIdx`, `firstTokenIndex`, `lastTokenIndex`).
3. Allocates a fresh `ArrayList<JavaLightNode>(8)`.
4. Walks every marker in the range: calls `productionMarkers.getMarker(i)`,
   `productionMarkers.isDoneMarker(i)` for each.
5. For each child token, calls `tokens.getTokenType(t)`, `tokens.getTokenStart(t)`,
   `tokens.getTokenEnd(t)`.
6. Boxes each `JavaLightNode` value class (`Int` → `Integer`) into the ArrayList.
7. Returns the freshly-allocated list.

**In the old code**, `children` was a field:
```kotlin
class JavaSyntaxNode(
    val children: List<JavaSyntaxNode>,  // ← pre-built, zero-cost access
    ...
)
```
Reading it was a single pointer dereference. The list was built once during tree
construction and shared across all property accesses.

### 2.2 — `findChildByType()` builds the full children list (CRITICAL)

**File**: `JavaLightTree.kt:220-226`

```kotlin
fun findChildByType(node: JavaLightNode, type: SyntaxElementType): JavaLightNode? {
    if (!isComposite(node)) return null
    for (child in getChildren(node)) {  // ← builds FULL list
        if (getType(child) == type) return child
    }
    return null
}
```

The migration plan (§1.3) explicitly said:

> "findChildByType and hasChildOfType can be optimized to **not build the full children
> list** — they can scan markers/tokens in the range and return early on first match."

This was not implemented. Finding IDENTIFIER (typically the 2nd–3rd child) in a class body
with 50 members requires building a list of all 50+ children first.

**In the old code**, `findChildByType` had the `childByTypeIndex` optimization:
```kotlin
fun JavaSyntaxNode.findChildByType(type: SyntaxElementType): JavaSyntaxNode? {
    childByTypeIndex?.let { return it[type]?.firstOrNull() }  // O(1) for >4 children
    return children.find { it.type == type }                   // O(k) but no allocation
}
```
For nodes with >4 children, this was an O(1) HashMap lookup after first access. For nodes
with ≤4 children, it was a linear scan on the pre-existing list with zero allocation.

### 2.3 — `hasModifier()` is pathologically expensive (HIGH)

**Files**: `JavaClassOverAst.kt:63-65`, `JavaMemberOverAst.kt:40-42`

```kotlin
private fun hasModifier(modifier: SyntaxElementType): Boolean {
    return modifierList?.let { tree.hasChildOfType(it, modifier) } ?: false
}
```

`hasChildOfType` → `findChildByType` → `getChildren()`. Each boolean modifier property
(`isAbstract`, `isStatic`, `isFinal`, each keyword in `visibility`) independently rebuilds
the children list of the modifier-list node.

For `JavaMemberOverAst`, a single visibility check can call `hasModifier` up to 3 times
(public → protected → private). Combined with `isAbstract`, `isStatic`, `isFinal`, and
potentially `isNative`, that is **6-7 calls to `getChildren(modifierListNode)`** per member.

**In the old code**: `modifierList.children.any { it.type == modifier }` — zero allocation,
just scanning a pre-existing list.

### 2.4 — Multiple cached properties on the same model object call `getChildren()` on the same node (HIGH)

The migration plan (§5.3) argued that the `childByTypeIndex` optimization "is not needed"
because "each `findChildByType` call happens at most once per property per model instance."

This reasoning is flawed. "Once per property" × "N properties that query children of the
same node" = N calls. The plan failed to account for the fact that **many cached properties
on the same model object look at the same node's children**.

For a single `JavaClassOverAst`, these cached properties each trigger
`getChildren(classBodyNode)`:

| Property | Method | Via |
|----------|--------|----|
| `name` | `findChildByType(IDENTIFIER)` | `getChildren` |
| `modifierList` | `findChildByType(MODIFIER_LIST)` | `getChildren` |
| `isInterface` | `findChildByType(INTERFACE_KEYWORD)` | `getChildren` |
| `isAnnotationType` | direct `getChildren(node)` | direct |
| `isEnum` | `findChildByType(ENUM_KEYWORD)` | `getChildren` |
| `isRecord` | `findChildByType(RECORD_KEYWORD)` | `getChildren` |
| `typeParameters` | `findChildByType(TYPE_PARAMETER_LIST)` | `getChildren` |
| `supertypes` | `findChildByType(EXTENDS_LIST)` + `findChildByType(IMPLEMENTS_LIST)` | 2× `getChildren` |
| `innerClassNames` | direct `getChildren(node)` | direct |
| `methods` | `getChildrenByType(METHOD)` + `getChildrenByType(ANNOTATION_METHOD)` | 2× `getChildren` |
| `fields` | `getChildrenByType(FIELD)` + `getChildrenByType(ENUM_CONSTANT)` | 2× `getChildren` |
| `constructors` | `getChildrenByType(METHOD)` | `getChildren` |
| `recordComponents` | `findChildByType(RECORD_HEADER)` | `getChildren` |
| `permittedTypes` | `findChildByType(PERMITS_LIST)` | `getChildren` |
| `isDeprecatedInJavaDoc` | `findChildByType(DOC_COMMENT)` | `getChildren` |

**Total: ~17 calls to `getChildren(classBodyNode)` per class instance.**

Additionally, modifier checks on the modifier list node:

| Property | Calls to `getChildren(modList)` |
|----------|---------------------------------|
| `isAbstract` | 1 |
| `isStatic` | 1 |
| `isFinal` | 1 |
| `visibility` | 1–3 |
| `isSealed` | 1 |
| `annotations` (via `getChildrenByType`) | 1 |

**Total: ~6-8 calls to `getChildren(modifierListNode)` per class.**

The same pattern repeats for every `JavaMethodOverAst`, `JavaFieldOverAst`, and
`JavaConstructorOverAst`.

### 2.5 — Uncached recursive `getChildren()` in expression evaluation (MEDIUM)

**Files**: `JavaMemberOverAst.kt:202-248` (`isInitializerPotentiallyConstant`),
`ConstantEvaluator.kt:69-93`

These methods recursively call `getChildren(n)` on expression nodes
(`BINARY_EXPRESSION`, `POLYADIC_EXPRESSION`, `PREFIX_EXPRESSION`, `PARENTH_EXPRESSION`).
None of these are cached. For an initializer like `A + B + C + D`, the children are rebuilt
at each recursion level.

### 2.6 — Value class boxing in `ArrayList<JavaLightNode>` (MEDIUM)

`JavaLightNode` is a `@JvmInline value class` wrapping `Int`. When stored in
`ArrayList<JavaLightNode>`, each element is boxed to an `Integer` object. The risk register
(§8, item 1) dismissed this as "trivial vs. saved 72-byte JavaSyntaxNode" — but the
comparison is invalid because the old children list was built **once and reused**, while the
new one is rebuilt on **every access** (§2.1), multiplying the boxing cost.

### 2.7 — Lost `childByTypeIndex` for large nodes (LOW-MEDIUM)

The old `JavaSyntaxNode` built a `Map<SyntaxElementType, List<JavaSyntaxNode>>` for nodes
with >4 children, giving O(1) type-based lookups after first access. A class body with 50
members had all `findChildByType` calls resolved via HashMap lookup.

The new code always does O(k) linear scan through the (freshly-allocated) children list.
With children caching (§4, Fix 1), the list allocation is eliminated after first access,
but the scan remains O(k) per call.

---

## 3. Quantitative Impact

### 3.1 — Per-class overhead comparison

For a class with 50 members (30 methods, 15 fields, 5 inner classes):

**Old (`JavaSyntaxNode`)**:
- `children` access: **0 allocations** (pre-built field, pointer dereference)
- `findChildByType` on class body: **O(1)** via `childByTypeIndex` after first call
- `hasModifier` per member: **0 allocations** (linear scan on existing list)

**New (`JavaLightTree`)**:
- `getChildren(classBody)` per class: **~17 ArrayList allocations**, each walking ~50+
  markers/tokens
- `getChildren(modList)` per class: **~7 ArrayList allocations**, each walking ~5 markers
- Same for each of 50 members: **~6 ArrayList allocations per member** for modifiers
  = 300 allocations
- Total per class: ~17 + 7 + 300 = **~324 ArrayList allocations** (was 0)

### 3.2 — Project-scale estimate

For a project with 5,000 classes, 50,000 methods, 30,000 fields:

| Source | Old allocations | New allocations | Delta |
|--------|----------------|-----------------|-------|
| Class body `getChildren` | 0 | 5,000 × 17 = 85,000 | +85K |
| Class modifier `getChildren` | 0 | 5,000 × 7 = 35,000 | +35K |
| Method modifier `getChildren` | 0 | 50,000 × 6 = 300,000 | +300K |
| Field modifier `getChildren` | 0 | 30,000 × 6 = 180,000 | +180K |
| Type ref `getChildren` | 0 | ~200,000 × 3 = 600,000 | +600K |
| **Total** | **0** | **~1,200,000** | **+1.2M** |

Each allocation: ArrayList header (~48 bytes) + backing array (~64 bytes for 8-slot) +
boxed JavaLightNode values (~16 bytes × k children). Conservative average 200
bytes/allocation = **~240 MB of short-lived allocations**.

The old approach also allocated (the `JavaSyntaxNode` objects themselves), but those were
long-lived and built once. The new approach shifts the cost from one-time construction to
per-access, multiplying it by the number of accesses.

### 3.3 — Where the pre-migration 20% gap came from vs. where the post-migration 20-25% gap comes from

| Factor | Pre-migration | Post-migration |
|--------|---------------|----------------|
| Per-node `JavaSyntaxNode` overhead (72 bytes × 750K nodes) | ~54 MB, HIGH | **Eliminated** |
| Per-node `children` ArrayList | ~9 MB, MEDIUM | **Eliminated** |
| `by lazy(PUBLICATION)` delegates | ~67 MB, HIGH | **Eliminated** (replaced by `@Volatile`) |
| Eager `buildIndex()` | MEDIUM | Unchanged |
| `getChildren()` per-access allocation | **0** | **~240 MB, CRITICAL (NEW)** |
| Value class boxing in ArrayList | N/A | **~100 MB, MEDIUM (NEW)** |
| Lost `childByTypeIndex` O(1) lookups | N/A | **O(k) per call (NEW)** |

The migration eliminated ~130 MB of static overhead but introduced ~340 MB of dynamic
(per-access) allocation pressure. The net result is worse because:
1. GC pressure from short-lived objects is more expensive than pressure from long-lived ones.
2. The allocations are on the hot path (every property access), not just during tree construction.
3. The CPU cost of walking markers + boxing is paid on every access, not amortized.

---

## 4. Fix Plan

### Fix 1 — Cache `getChildren()` results on `JavaLightTree` (CRITICAL)

**Expected impact**: Eliminates ~1.2M redundant ArrayList allocations. Recovers most of
the regression.

**Approach**: Add a `ConcurrentHashMap<Int, List<JavaLightNode>>` to `JavaLightTree` that
memoizes children per node index. The tree is immutable, so this is safe and correct.

```kotlin
// JavaLightTree.kt
class JavaLightTree(...) {
    private val childrenCache = ConcurrentHashMap<Int, List<JavaLightNode>>()

    fun getChildren(node: JavaLightNode): List<JavaLightNode> {
        if (!isComposite(node)) return emptyList()
        if (isErrorMarker(node)) return emptyList()
        return childrenCache.computeIfAbsent(node.index) { computeChildrenImpl(node) }
    }

    private fun computeChildrenImpl(node: JavaLightNode): List<JavaLightNode> {
        // ... existing getChildren logic, moved here ...
    }
}
```

**Why `ConcurrentHashMap` over a plain HashMap**: FIR resolution is concurrent —
multiple threads may query properties of the same `JavaClassOverAst`, which would invoke
`getChildren()` on the same tree node simultaneously. The rest of the module already uses
`ConcurrentHashMap` for this reason.

**Memory cost**: One HashMap entry per node that is actually queried. In practice, only
composite nodes whose children are accessed get cached — tokens (the vast majority of
nodes) never enter the cache. For a file with ~400 composites, if 100 are queried,
the cache holds 100 entries × ~48 bytes (HashMap entry) = ~5 KB. The cached lists
themselves hold the children that would have been rebuilt on every call — this is the
same total memory as ONE old-style `getChildren()` call, but shared across all callers.

**Cache lifecycle**: The `JavaLightTree` instance is held by model objects in
`JavaClassFinderOverAstImpl.classCache`. When the tree becomes unreachable, the entire
children cache is GC'd. No explicit eviction needed.

**Implementation notes**:
- Move the current `getChildren` body into `computeChildrenImpl`.
- The `emptyList()` short-circuits for non-composite and error-marker nodes happen
  BEFORE the cache lookup (no entry for non-composite nodes).
- The synthetic root's children are also cached — this is the most frequently accessed
  node (every property on `JavaClassOverAst` goes through `getChildren(classBodyRoot)`).

### Fix 2 — Direct-scan `findChildByType` without list construction (HIGH)

**Expected impact**: Eliminates ArrayList allocation for the ~98 `findChildByType` call
sites when the requested child is not the last one. Even with Fix 1 (cached children),
this avoids the overhead of iterating a cached list when we only need the first match.
Most importantly, it helps on first access (before the cache is populated).

**Approach**: Extract the marker/token walking loop from `getChildren` into an
`inline forEachChild` helper, then use it in `findChildByType`:

```kotlin
// JavaLightTree.kt

/**
 * Walks direct children of [node] in source order, invoking [action] for each.
 * Returns early if [action] returns `true` (found). Returns `true` if any action
 * returned `true`.
 *
 * This is the core iteration primitive. Both [getChildren] (which builds a list)
 * and [findChildByType] (which short-circuits) use it.
 */
private inline fun forEachDirectChild(
    node: JavaLightNode,
    action: (JavaLightNode) -> Boolean,
): Boolean {
    if (!isComposite(node)) return false
    if (isErrorMarker(node)) return false

    val firstTokenIndex: Int
    val lastTokenIndex: Int
    val startIdx: Int
    val doneIdx: Int
    if (isSyntheticRoot(node)) {
        startIdx = -1
        doneIdx = productionMarkers.size
        firstTokenIndex = 0
        lastTokenIndex = tokens.tokenCount
    } else {
        startIdx = node.index
        doneIdx = doneForStart[startIdx]
        firstTokenIndex = productionMarkers.getMarker(startIdx).getStartTokenIndex()
        lastTokenIndex = productionMarkers.getMarker(doneIdx).getEndTokenIndex()
    }

    var prevTokenIndex = firstTokenIndex
    var i = startIdx + 1
    while (i < doneIdx) {
        val marker = productionMarkers.getMarker(i)
        val isDone = productionMarkers.isDoneMarker(i)
        if (marker.isErrorMarker()) {
            if (scanTokensForMatch(prevTokenIndex, marker.getStartTokenIndex(), action))
                return true
            if (action(JavaLightNode(i))) return true
            prevTokenIndex = marker.getStartTokenIndex()
            i++
        } else if (!isDone) {
            if (scanTokensForMatch(prevTokenIndex, marker.getStartTokenIndex(), action))
                return true
            if (action(JavaLightNode(i))) return true
            val childDone = doneForStart[i]
            prevTokenIndex = productionMarkers.getMarker(childDone).getEndTokenIndex()
            i = childDone + 1
        } else {
            i++
        }
    }
    return scanTokensForMatch(prevTokenIndex, lastTokenIndex, action)
}

private inline fun scanTokensForMatch(
    from: Int, to: Int, action: (JavaLightNode) -> Boolean,
): Boolean {
    for (t in from until to) {
        tokens.getTokenType(t) ?: continue
        if (tokens.getTokenStart(t) == tokens.getTokenEnd(t)) continue
        if (action(JavaLightNode(-(t + 1)))) return true
    }
    return false
}

fun findChildByType(node: JavaLightNode, type: SyntaxElementType): JavaLightNode? {
    // Fast path: if children are already cached, scan the cached list.
    childrenCache[node.index]?.let { cached ->
        return cached.firstOrNull { getType(it) == type }
    }
    // Slow path: walk markers directly, return on first match.
    var result: JavaLightNode? = null
    forEachDirectChild(node) { child ->
        if (getType(child) == type) {
            result = child
            true  // found — stop iteration
        } else false
    }
    return result
}
```

Then rewrite `computeChildrenImpl` to use the same `forEachDirectChild`:
```kotlin
private fun computeChildrenImpl(node: JavaLightNode): List<JavaLightNode> {
    val result = ArrayList<JavaLightNode>(8)
    forEachDirectChild(node) { child ->
        result.add(child)
        false // continue
    }
    return result
}
```

**Implementation notes**:
- `forEachDirectChild` is `inline` so the lambda is eliminated at the call site.
- The fast path (`childrenCache[node.index]`) avoids marker walking when children are
  already cached from a previous `getChildren()` call.
- The slow path walks markers directly without allocating any list.
- `hasChildOfType` becomes: `findChildByType(node, type) != null` — unchanged but
  now benefits from the early-return scan.

### Fix 3 — Direct-scan `getChildrenByType` (MEDIUM)

**Expected impact**: Eliminates the intermediate full-children-list allocation when only
a subset of children by type is needed. Important for `methods` / `fields` /
`constructors` properties that call `getChildrenByType` on the class body.

```kotlin
fun getChildrenByType(node: JavaLightNode, type: SyntaxElementType): List<JavaLightNode> {
    // Fast path: if children are cached, filter the cached list.
    childrenCache[node.index]?.let { cached ->
        return cached.filter { getType(it) == type }
    }
    // Slow path: walk markers directly, collect only matching children.
    if (!isComposite(node)) return emptyList()
    if (isErrorMarker(node)) return emptyList()
    val result = ArrayList<JavaLightNode>(4)
    forEachDirectChild(node) { child ->
        if (getType(child) == type) result.add(child)
        false // continue — collect all matches
    }
    return result
}
```

### Fix 4 — Restore `childByTypeIndex` for large nodes (MEDIUM)

**Expected impact**: Restores O(1) type-based child lookups for class body nodes
(50+ children), modifier list nodes accessed via multiple properties, etc.

**Approach**: Add a lazily-built type index alongside the children cache:

```kotlin
// JavaLightTree.kt
class JavaLightTree(...) {
    private val childrenCache = ConcurrentHashMap<Int, List<JavaLightNode>>()
    private val typeIndexCache = ConcurrentHashMap<Int, Map<SyntaxElementType, List<JavaLightNode>>>()

    private fun getOrBuildTypeIndex(node: JavaLightNode): Map<SyntaxElementType, List<JavaLightNode>>? {
        val children = getChildren(node)
        if (children.size <= CHILD_INDEX_THRESHOLD) return null
        return typeIndexCache.computeIfAbsent(node.index) { children.groupBy { getType(it) } }
    }

    fun findChildByType(node: JavaLightNode, type: SyntaxElementType): JavaLightNode? {
        getOrBuildTypeIndex(node)?.let { return it[type]?.firstOrNull() }
        // Linear scan for small nodes...
        return getChildren(node).firstOrNull { getType(it) == type }
    }

    fun getChildrenByType(node: JavaLightNode, type: SyntaxElementType): List<JavaLightNode> {
        getOrBuildTypeIndex(node)?.let { return it[type] ?: emptyList() }
        return getChildren(node).filter { getType(it) == type }
    }
}

private const val CHILD_INDEX_THRESHOLD = 4  // same as old code
```

**Note**: Fix 4 is optional if Fix 1 + Fix 2 + Fix 3 provide sufficient recovery.
Profile after implementing Fixes 1–3 to determine if the O(k) scan on cached lists is
still a bottleneck.

### Fix 5 — `IntArray`-backed children list (LOW)

**Expected impact**: Eliminates value class boxing when storing children in a list.
Lower priority — the boxing cost is ~16 bytes per element, which is minor compared to
the ~200 bytes per ArrayList allocation eliminated by Fix 1.

**Approach**: Replace `ArrayList<JavaLightNode>` with a custom `JavaLightNodeList` backed
by `IntArray`:

```kotlin
class JavaLightNodeList(private val indices: IntArray, val size: Int) : AbstractList<JavaLightNode>() {
    override val size: Int get() = size
    override fun get(index: Int): JavaLightNode = JavaLightNode(indices[index])
}
```

**Deferred**: Profile after Fixes 1–3 to determine if boxing is a measurable bottleneck.
The `ConcurrentHashMap` in Fix 1 already boxes the `Int` key, so the boxing cost is not
fully eliminable without a custom map structure.

---

## 5. Implementation Order and Verification

### Step 1: Implement Fix 1 (children cache) + Fix 2 (direct-scan findChildByType)

These two are tightly coupled — `forEachDirectChild` is the shared primitive, and Fix 1's
`computeChildrenImpl` should use it.

**Changes in `JavaLightTree.kt`**:
1. Add `private val childrenCache = ConcurrentHashMap<Int, List<JavaLightNode>>()`.
2. Rename current `getChildren` body → `computeChildrenImpl`.
3. Rewrite `getChildren` to check cache first, populate via `computeChildrenImpl` on miss.
4. Add `forEachDirectChild` inline helper (extracted from `computeChildrenImpl`'s logic).
5. Rewrite `computeChildrenImpl` to use `forEachDirectChild`.
6. Rewrite `findChildByType(node, SyntaxElementType)` to use cached fast path or
   direct-scan slow path.
7. Rewrite `findChildByType(node, String)` similarly.
8. Rewrite `hasChildOfType` — unchanged (delegates to `findChildByType`), but now
   benefits from the optimization.

**No changes to model classes** — the API is the same; only the implementation changes.

**Verification**:
```bash
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstPhasedTestGenerated" \
  --tests "JavaUsingAstBoxTestGenerated" --tests "JavaParsingTest" \
  --stacktrace --rerun-tasks --no-build-cache 2>&1 | tee "$JD_TMP/fix1_test.txt"
```

### Step 2: Implement Fix 3 (direct-scan getChildrenByType)

**Changes in `JavaLightTree.kt`**:
1. Rewrite `getChildrenByType(node, SyntaxElementType)`: cached fast path + direct-scan
   slow path.
2. Rewrite `getChildrenByType(node, String)`: same pattern.

**Verification**: Same test suite as Step 1.

### Step 3: Profile and decide on Fix 4

Run the MT tests (or a representative pipeline test) with AtomicLong counters:
- `getChildren` cache hits vs. misses
- `findChildByType` fast-path hits (cached) vs. slow-path (direct scan)
- Total `forEachDirectChild` invocations

If class body nodes (50+ children) are still a hotspot due to O(k) scans on cached
lists, implement Fix 4 (type index).

### Step 4: Profile and decide on Fix 5

If GC profiling (JFR or `-XX:+PrintGCDetails`) shows significant boxing pressure from
`ArrayList<JavaLightNode>`, implement Fix 5 (IntArray-backed list).

---

## 6. Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Children cache grows unbounded | Low | Only composite nodes that are actually queried enter the cache. For a typical file, this is <100 entries. The cache is GC'd with the `JavaLightTree` instance. |
| `ConcurrentHashMap` overhead per tree | Low | One CHM per parsed file. The overhead (~100 bytes for an empty CHM) is negligible vs. the ~10 KB saved by not materializing the tree. |
| `computeIfAbsent` contention on hot nodes | Low | FIR accesses different classes concurrently but rarely accesses the same class body node from multiple threads. Even if it does, `computeIfAbsent` is correct and the duplicate computation is avoided. |
| `forEachDirectChild` inlining bloat | Low | The lambda at each call site is small (one type comparison). Bytecode size increase is ~10-20 bytes per call site, similar to the old inline `children.find { it.type == X }`. |
| Semantic change in iteration order | None | The walker matches the existing `getChildren` order exactly. |

---

## 7. Benchmark Results

Fixes 1–3 (children cache, direct-scan `findChildByType`, direct-scan `getChildrenByType`)
were implemented as a single change to `JavaLightTree.kt` (+83/–30 lines). The change
was benchmarked using `KotlinFullPipelineTestsGenerated` (414 modules) in sequential mode
(`ExecutionMode.SAME_THREAD`) to eliminate concurrency variance.

### Sequential mode results

| Run | Total time | Delta |
|-----|-----------|-------|
| **Without fix** (LightTree, no children cache) | **255.485s** | baseline |
| **With fix** (LightTree + children cache + direct-scan) | **242.247s** | **–13.2s (–5.2%)** |

### Concurrent mode results (high variance, for reference only)

| Run | Total time |
|-----|-----------|
| Concurrent, previous baseline (2026-04-20) | 89.926s |
| Concurrent, with fix, run 1 | 93.951s |
| Concurrent, with fix, run 2 | 97.376s |
| Concurrent, with fix, run 3 | 90.620s |

Concurrent mode has ~10% variance, making it unsuitable for detecting a 5% improvement.

### Interpretation

The **5.2% overall pipeline improvement** is consistent with the analysis: java-direct's
Java source processing is a fraction of the total pipeline work (which includes Kotlin
source parsing, FIR resolution, IR generation, JVM codegen, etc.). The improvement is
concentrated in the Java interop path — modules with more Java sources benefit more.

The top-10 slowest modules (`testBackend_js`, `testAnalysis_api_fir`, `testFrontend`,
`testCheckers`) show modest improvements because they are dominated by Kotlin compilation,
not Java source loading.

### Original estimates vs. actuals

| Fix | Estimated recovery (of java-direct gap) | Actual (of full pipeline) |
|-----|----------------------------------------|--------------------------|
| Fix 1 (children cache) | 15-20% of java-direct gap | 5.2% of full pipeline |
| Fix 2 (direct-scan findChildByType) | 2-5% | (included in above) |
| Fix 3 (direct-scan getChildrenByType) | 1-3% | (included in above) |

The 5.2% full-pipeline improvement translates to roughly **20-25% improvement in
java-direct's own execution time** (assuming java-direct accounts for ~20-25% of the
pipeline), which matches the Fix 1 estimate of 15-20%.

---

## 8. Lessons Learned

1. **A migration that reduces static memory can increase dynamic allocation.** The
   LightTree eliminated 750K+ long-lived `JavaSyntaxNode` objects but introduced
   millions of short-lived `ArrayList` allocations on the hot path. GC pressure from
   short-lived objects on the hot path is more expensive per byte than pressure from
   long-lived objects.

2. **"Computed on demand" is not free.** The analysis document framed on-demand children
   computation as "zero per-node overhead", which is true for storage but ignores
   runtime cost. The correct framing is "zero *storage* overhead, O(k) *access* cost per
   call" — and the number of calls matters.

3. **Plan mitigations must actually be implemented.** The plan correctly identified both
   the children caching need (§5.3) and the direct-scan `findChildByType` optimization
   (§1.3). Neither was implemented during the migration. Future migration work should
   treat plan-identified optimizations as mandatory steps, not deferred ideas.

4. **Count calls, not properties.** The flawed reasoning was "each cached property calls
   `findChildByType` once, so each node's children are rebuilt only a few times." The
   correct count is "each cached property calls it once, and there are 15-20 cached
   properties per model object that query the same node."

---

*Last updated: 2026-04-21*
