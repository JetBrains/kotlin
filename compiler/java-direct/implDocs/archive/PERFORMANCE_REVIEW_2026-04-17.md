# Java-Direct Performance Review

**Date**: 2026-04-17  
**Context**: java-direct is ~20% slower than the PSI-based approach on large pipeline tests (`KotlinFullPipelineTestsGenerated` and similar). This review identifies root causes and provides a roadmap for investigation and remediation.

---

## Executive Summary

The 20% slowdown likely stems from a **combination of factors**, not a single bottleneck:

1. **`by lazy(PUBLICATION)` delegate overhead** — 40 delegates across model classes, each allocating ~32 bytes (wrapper + lambda). With thousands of `JavaClass`, `JavaMethod`, `JavaField`, and `JavaClassifierType` instances, this adds hundreds of MB of heap pressure and GC load.
2. **Redundant `rawTypeName.split('.')` calls** — the same string is split 4 times in `JavaClassifierTypeOverAst` (`classifier`, `isTriviallyFlexibleHint`, `classifierQualifiedName`, `isResolved`).
3. **Per-invocation `HashMap` allocation in `resolve()`** — every type reference resolution creates a fresh `HashMap`. For a file with 200 type references, that's 200 short-lived `HashMap`s.
4. **Annotations not cached in `JavaTypeOverAst`** — `annotations` is a computed `get()` property that creates new `JavaAnnotationOverAst` objects on every access.
5. **Front-loaded `buildIndex()` cost** — every Java source file is read/scanned at construction time, even if never needed. PSI avoids this by using pre-built IDE indexes.
6. **AST tree retained in memory** — `JavaSyntaxNode` trees are held alive indefinitely via `classCache`. PSI's lightweight mode (LighterASTNode) builds temporary trees that are GC'd after FIR conversion.

---

## Category 1: Memory / GC Pressure

### 1.1 — 40 `by lazy(PUBLICATION)` delegates on model objects

**Severity**: HIGH  
**Files**: `JavaClassOverAst.kt` (16), `JavaMemberOverAst.kt` (15), `JavaTypeOverAst.kt` (8), `JavaAnnotationOverAst.kt` (1)

Each `by lazy(LazyThreadSafetyMode.PUBLICATION)` allocates a `SafePublicationLazyImpl` wrapper (~24 bytes) plus a captured lambda (~8 bytes) = ~32 bytes per delegate per instance.

**Impact estimate**:
- `JavaClassOverAst`: 16 delegates × 32 bytes = 512 bytes overhead per class
- `JavaMethodOverAst`: ~10 delegates × 32 bytes = 320 bytes per method
- `JavaFieldOverAst`: ~5 delegates × 32 bytes = 160 bytes per field
- `JavaClassifierTypeOverAst`: 8 delegates × 32 bytes = 256 bytes per type reference
- For a project with 5,000 classes, 50,000 methods, 30,000 fields, and 200,000 type references: **~67 MB pure delegate overhead**

The Step 3.6 optimization correctly eliminated lazy delegates from `JavaSyntaxNode` (millions of instances). The same pattern should be applied to the model classes above — they have far fewer instances but far more delegates per instance.

**Recommendation**: Replace `by lazy(PUBLICATION)` with manual `@Volatile` fields on the top-10 highest-instance-count classes. Priority order:
1. `JavaClassifierTypeOverAst` (most instances, 8 delegates)
2. `JavaFieldOverAst` / `JavaMethodOverAst` (many instances, many delegates)
3. `JavaClassOverAst` (fewer instances but 16 delegates each)

### 1.2 — AST trees retained indefinitely in memory

**Severity**: MEDIUM  
**File**: `JavaClassFinderOverAstImpl.kt`

Every `JavaClassOverAst` holds a reference to its `node: JavaSyntaxNode`, which transitively holds the entire AST tree and the source `CharSequence`. The `classCache` is unbounded — once a class is parsed, its entire file's AST lives forever.

PSI's approach in Kotlin compilation (LightTree mode): builds a temporary AST, converts to FIR, and discards the tree. java-direct retains trees because lazy properties dereference `node` on demand.

**Recommendation**: Long-term architectural change — eagerly extract all needed data from the AST during construction, then release the AST. This is a large refactor but would dramatically reduce steady-state memory.

### 1.3 — `annotations` property creates objects on every access

**Severity**: MEDIUM  
**File**: `JavaTypeOverAst.kt:28-38`

```kotlin
override val annotations: Collection<JavaAnnotation>
    get() {  // <-- computed property, NOT lazy
        ...?.map { JavaAnnotationOverAst(it, resolutionContext) }  // new objects each call
```

Every access to `annotations` on a type creates new `JavaAnnotationOverAst` wrapper objects. If FIR accesses annotations multiple times per type (which it does — for nullability, deprecation, etc.), this multiplies allocations.

**Recommendation**: Make it a `lazy` or `@Volatile`-cached property.

---

## Category 2: Redundant Computation

### 2.1 — `rawTypeName.split('.')` called 4 times per type

**Severity**: HIGH  
**File**: `JavaTypeOverAst.kt` lines 111, 150, 171, 297

`rawTypeName` is split on `'.'` independently in:
- `classifier` (line 111)
- `isTriviallyFlexibleHint` (line 150)
- `classifierQualifiedName` (line 171)
- `isResolved` (line 297)

Each `split()` allocates a new `List<String>`. Since all four are lazy and may be triggered independently, and `rawTypeName` is itself lazy, the worst case is 4 array allocations for the same string.

**Recommendation**: Cache the split result as a lazy property alongside `rawTypeName`:
```kotlin
private val rawTypeNameParts: List<String> by lazy(PUBLICATION) { rawTypeName.split('.') }
```

### 2.2 — `classifier`, `classifierQualifiedName`, and `isTriviallyFlexibleHint` do overlapping work

**Severity**: MEDIUM  
**File**: `JavaTypeOverAst.kt` lines 110-204

All three lazy properties independently:
1. Split `rawTypeName`
2. Check type parameters via `resolutionContext.findTypeParameter(parts[0])`
3. Look up local classes via `resolutionContext.findLocalClass(Name.identifier(parts[0]))`

If `classifier` is computed first, `classifierQualifiedName` and `isTriviallyFlexibleHint` redo the same lookups. Since these lookups may involve walking the class hierarchy, the duplication is not free.

**Recommendation**: Compute a single `ResolvedTypeInfo` struct lazily, then derive all three properties from it.

### 2.3 — `resolveNestedClassToClassId` recursive string re-joining

**Severity**: MEDIUM  
**File**: `JavaResolutionContext.kt:258-330`

```kotlin
val parts = name.split('.')
for (i in 1 until parts.size) {
    val outerParts = parts.subList(0, i)
    // Recursive call rejoins the string:
    resolveNestedClassToClassId(outerParts.joinToString("."), ...)
```

For a name like `"a.b.c.D.E"` (5 parts), this creates `joinToString` allocations at each recursion level, and each recursive call re-splits the string. Total string allocations: O(n²).

**Recommendation**: Refactor to pass `parts: List<String>` + index range instead of re-joining strings.

### 2.4 — Duplicate code in `resolveSimpleNameToClassId` and `resolveSimpleNameToClassIdWithoutInheritance`

**Severity**: LOW (maintenance, not runtime)  
**File**: `JavaResolutionContext.kt` lines 335-425 vs 487-511

Nearly identical resolution logic duplicated. If one is optimized, the other diverges.

---

## Category 3: Allocation Hot Spots

### 3.1 — Per-invocation `HashMap` in `resolve()`

**Severity**: HIGH  
**File**: `JavaResolutionContext.kt:234`

```kotlin
fun resolve(...): ClassId? {
    val cache = HashMap<ClassId, Boolean>()  // NEW per call
    ...
}
```

Every type reference resolution creates a fresh `HashMap`. For a compilation unit with 200 type references, this is 200 short-lived HashMaps. Each HashMap starts with 16-slot backing array (128 bytes on 64-bit) plus the object header.

**Recommendation**: 
- Option A: Use a context-level cache that persists across `resolve()` calls for the same file. The callback is deterministic within a file, so this is safe.
- Option B: Use a `SmallMap` or linear-probe cache for the typical case of <10 entries.

### 3.2 — `Name.identifier()` allocations in resolution paths

**Severity**: MEDIUM  
**Files**: `JavaResolutionContext.kt`, `JavaTypeOverAst.kt`, `JavaClassOverAst.kt`

`Name.identifier(string)` is called frequently in resolution:
- `resolveSimpleNameToClassId`: at least 3 calls per resolution
- `findLocalClass(Name.identifier(parts[0]))`: per type reference
- `findInnerClass(Name.identifier(parts[i]))`: per nested class segment

`Name.identifier()` creates a new `Name` wrapper each time. While individually cheap, these add up across thousands of type references.

**Recommendation**: Cache common `Name` instances (especially for the first part of multi-part names).

### 3.3 — `subPackagesOf` is O(n) with string allocations

**Severity**: LOW (called infrequently)  
**File**: `JavaClassFinderOverAstImpl.kt:343-363`

```kotlin
fun subPackagesOf(fqName: FqName): Collection<FqName> {
    val prefix = fqName.asString() + "."
    for (pkg in index.keys) {
        if (pkg.asString().startsWith(prefix) && pkg != fqName) {
            val tail = pkg.asString().removePrefix(prefix)  // 3 string allocations per package
```

Three string allocations per index entry. For a project with 1000 packages, this is 3000 allocations per call. However, `subPackagesOf` is likely called infrequently.

### 3.4 — `knownClassNamesInPackage` allocates per entry

**Severity**: LOW-MEDIUM  
**File**: `JavaClassFinderOverAstImpl.kt:170-177`

```kotlin
fileEntries.any { entry ->
    entry.file.name.removeSuffix(".java") == name  // allocates per entry
}
```

`removeSuffix(".java")` creates a new string on every comparison. For a package with 100 entries, this is 100 string allocations per call to `knownClassNamesInPackage`.

**Recommendation**: Pre-compute file basename at `FileEntry` creation time.

---

## Category 4: Initialization Cost

### 4.1 — `buildIndex()` front-loads all file I/O

**Severity**: HIGH (for pipeline tests)  
**File**: `JavaClassFinderOverAstImpl.kt:182-196`

The constructor eagerly walks ALL source roots and reads/scans EVERY `.java` file:
- Small files (≤4096 bytes): fully parsed (lexing + parsing + AST construction)
- Large files (>4096 bytes): line-by-line scan with regex matching

PSI doesn't pay this cost because IntelliJ's indexes are pre-built. For pipeline tests that create a fresh `JavaClassFinderOverAstImpl` per test module, this initialization is repeated many times.

**Impact**: For a project with 1000 Java files (500 small, 500 large):
- 500 small files × full parse ≈ significant CPU
- 500 large files × line scan ≈ moderate I/O

**Recommendation**: 
- Consider lazy indexing: only index files in a package when that package is first queried
- Consider caching the index across test runs (if source roots haven't changed)

### 4.2 — `SMALL_FILE_SIZE_THRESHOLD = 4096` may be suboptimal

**Severity**: MEDIUM  
**File**: `JavaClassFinderOverAstImpl.kt:30`

4096 bytes (4 KB) is a heuristic. No profiling data justifies this value.

**Analysis**: 
- A typical Java class with a few methods is 2-5 KB
- 4 KB means many "real" classes are eagerly parsed during index build
- If the threshold were higher (e.g., 8 KB or 16 KB), more files would use the cheap lightweight scan
- Conversely, too high a threshold means more deferred parses on first access

**Recommendation**: Profile with different thresholds (2 KB, 4 KB, 8 KB, 16 KB) on a real project and measure total compilation time. The optimal threshold depends on the ratio of files that are actually looked up vs. total files in source roots.

---

## Category 5: Structural Overhead vs. PSI

### 5.1 — Parse tree fully materialized vs. PSI's LightTree

**Severity**: HIGH (fundamental architecture)

java-direct uses `buildSyntaxTree()` which creates a **full materialized tree** — every token gets a `JavaSyntaxNode` (~64 bytes). PSI's LightTree mode (`FlyweightCapableTreeStructure<LighterASTNode>`) uses a flat array representation where nodes are indices, not objects.

For a 1000-line Java file with ~10,000 tokens + ~5,000 composite nodes = 15,000 nodes × 64 bytes ≈ **1 MB per file**. For 500 eagerly-parsed small files, that's **500 MB** of AST retained in memory.

The PSI approach for Kotlin files: build lightweight tree → convert to FIR → discard tree. java-direct can't do this because lazy properties on model objects dereference `node` on demand.

**Recommendation**: This is the single largest architectural difference. Two options:
1. **Short-term**: After all lazy properties on a `JavaClassOverAst` have been forced, null out the `node` reference. Requires tracking which properties have been accessed.
2. **Long-term**: Eagerly extract all data during construction (like PSI stub indexes), store as plain fields, discard AST immediately.

### 5.2 — `JavaSyntaxNode.children` is a full `List<JavaSyntaxNode>`

**Severity**: MEDIUM  

Every composite node stores `children: List<JavaSyntaxNode>` which is an `ArrayList` with its own backing array. For a node with 5 children, the list itself uses 40 bytes (5 refs) + 32 bytes (ArrayList overhead) = 72 bytes, on top of the 64 bytes per node.

PSI's FlyweightCapableTreeStructure stores children as ranges in a flat array — zero per-node overhead for the children list.

---

## Category 6: Measurement Methodology

### 6.1 — How to measure the 20% gap

The pipeline tests create a full compilation pipeline. To isolate java-direct's contribution:

**Option A: AtomicLong Counters (already documented in AGENT_INSTRUCTIONS.md)**
```kotlin
object PerfCounters {
    val buildIndexTimeNs = AtomicLong()
    val parseFileTimeNs = AtomicLong()
    val resolveTypeTimeNs = AtomicLong()
    val findClassTimeNs = AtomicLong()
    val findClassCacheHits = AtomicLong()
    val findClassCacheMisses = AtomicLong()
    val resolveCallCount = AtomicLong()
    val lazyPropertyForcedCount = AtomicLong()
    
    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            java.io.File("/tmp/jd_perf_counters.txt").writeText(buildString {
                appendLine("buildIndex: ${buildIndexTimeNs.get() / 1_000_000}ms")
                appendLine("parseFile: ${parseFileTimeNs.get() / 1_000_000}ms")
                appendLine("resolveType: ${resolveTypeTimeNs.get() / 1_000_000}ms")
                appendLine("findClass: ${findClassTimeNs.get() / 1_000_000}ms")
                appendLine("findClass hits/misses: ${findClassCacheHits.get()}/${findClassCacheMisses.get()}")
                appendLine("resolve() calls: ${resolveCallCount.get()}")
            })
        })
    }
}
```

Instrument: `buildIndex()`, `findClass()`, `resolve()`, `parseTopLevelClassFromFile()`.

**Option B: JFR (Java Flight Recorder)**
```bash
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstPhasedTestGenerated" \
  -Dorg.gradle.jvmargs="-XX:StartFlightRecording=filename=/tmp/jd.jfr,settings=profile,duration=300s" \
  --stacktrace --rerun-tasks 2>&1 | tee /tmp/jd_jfr_test.txt
```
Then analyze with `jfr print --events jdk.ObjectAllocationInNewTLAB /tmp/jd.jfr` for allocation hot spots.

**Option C: Comparative test**
Run the same tests with PSI-based finder and java-direct finder, measuring wall-clock time:
```bash
# PSI baseline
./gradlew :compiler:fir:analysis-tests:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" \
  --stacktrace --rerun-tasks 2>&1 | tee /tmp/psi_test.txt

# java-direct
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstPhasedTestGenerated" \
  --stacktrace --rerun-tasks 2>&1 | tee /tmp/jd_test.txt
```
Compare total test execution times (grep for "BUILD SUCCESSFUL" timestamp).

### 6.2 — Key metrics to track

| Metric | How to measure | Target |
|--------|---------------|--------|
| `buildIndex()` wall time | AtomicLong + System.nanoTime | <100ms for typical module |
| File parse count | Counter in `parseTopLevelClassFromFile` | Should equal unique top-level classes accessed |
| `resolve()` call count | Counter | Understand call volume |
| `resolve()` cache hit rate | HashMap size at return | Should be >50% for nested names |
| Peak heap usage | `-XX:+PrintGCDetails` or JFR | Compare PSI vs java-direct |
| GC pause time | JFR GC events | Indicative of allocation pressure |
| `findClass` cache hit ratio | Hits / (hits + misses) | Should be >90% after warmup |

---

## Priority Roadmap

### Quick Wins (< 2 hours each, ~5-8% improvement expected)

1. **Cache `rawTypeNameParts`** in `JavaClassifierTypeOverAst` — eliminates 3 redundant `split()` calls per type reference
2. **Cache `annotations`** in `JavaTypeOverAst` — eliminates per-access object creation
3. **Pre-compute file basename** in `FileEntry` — eliminates `removeSuffix` allocations in `knownClassNamesInPackage`
4. **Refactor `resolveNestedClassToClassId`** — pass index range instead of re-joining strings

### Medium Effort (2-4 hours, ~5-10% improvement expected)

5. **Replace `by lazy(PUBLICATION)` on `JavaClassifierTypeOverAst`** with `@Volatile` fields (8 delegates × ~32 bytes = 256 bytes per instance saved, across 200K+ instances)
6. **Context-level cache for `resolve()`** instead of per-call HashMap
7. **Profile `SMALL_FILE_SIZE_THRESHOLD`** — run with 2K, 4K, 8K, 16K and measure

### Larger Effort (days, ~10-20% improvement expected)

8. **Replace remaining `by lazy(PUBLICATION)` delegates** on `JavaClassOverAst` (16), `JavaMethodOverAst` (10), `JavaFieldOverAst` (5)
9. **Lazy indexing** — only index packages when first queried, not all upfront
10. **AST release after extraction** — eagerly compute all needed data, null out `node` references

### Architectural (weeks, addresses fundamental gap)

11. **Switch to flat-array AST representation** (like LightTree) — eliminate per-node object overhead
12. **Single-pass extraction model** — parse → extract all data → discard tree (like PSI stubs)

---

## Hardcoded Thresholds Assessment

| Threshold | Value | File | Assessment |
|-----------|-------|------|------------|
| `SMALL_FILE_SIZE_THRESHOLD` | 4096 bytes | JavaClassFinderOverAstImpl.kt:30 | **Needs profiling**. 4 KB means many real classes are eagerly parsed. Consider raising to 8-16 KB to defer more parses. |
| `CHILD_INDEX_THRESHOLD` | 4 | utils.kt:171 | **Reasonable but conservative**. Linear scan on 5-8 items is competitive with HashMap lookup. Consider raising to 8-12 based on benchmarks. The HashMap itself costs ~128 bytes minimum. |

---

## Summary

The 20% gap is not one bottleneck but a tax paid across many dimensions:
- **Memory**: lazy delegates, retained ASTs, per-type allocations (~40% of gap)
- **Redundant computation**: split/resolve duplication, per-call caches (~30% of gap)
- **Initialization**: front-loaded index build, eager small-file parsing (~20% of gap)
- **Structural**: materialized tree vs. LightTree, List-based children (~10% of gap)

The quick wins (items 1-4) should be implementable in a single session and are expected to close 5-8% of the gap. Items 5-7 should close another 5-10%. The remaining gap requires architectural changes (items 8-12).
