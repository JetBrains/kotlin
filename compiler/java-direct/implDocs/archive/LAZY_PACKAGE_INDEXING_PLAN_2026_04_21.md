# Lazy Per-Package Indexing — Implementation Plan

## Status: Planned (not yet implemented)

---

## 1. Problem

`JavaClassFinderOverAstImpl.buildIndex()` runs in `init {}` and eagerly walks **every** `.java`
file across all source roots. For the `testFrontend` module (141 Java files), profiling with
`ThreadMXBean.currentThreadCpuTime` measured `buildIndex` at **156ms CPU** — roughly **40%** of
java-direct's total 389ms. The walk reads file content for every file (full parse for files
≤ 4096 bytes, lightweight line scan otherwise), regardless of whether the Kotlin code ever
references classes in those packages.

PSI-based class finders don't pay this cost at all: the IDE has pre-built indexes, so the first
`findClass` call is essentially free. The eager `buildIndex` is the single largest structural
gap between java-direct and PSI that is solvable within the module.

## 2. Key Insight

Standard Java projects mirror package structure in the directory tree:
`com.example.Foo` lives at `sourceRoot/com/example/Foo.java`. The IntelliJ VFS
`VirtualFile` API supports efficient directory navigation:

| Method | Cost | Caching |
|--------|------|---------|
| `findChild(name)` | O(1) per segment | VFS caches after first `children` call |
| `children` | One `File.listFiles()` call | Cached by `CoreLocalVirtualFile` |
| `isDirectory`, `name`, `length` | O(1) | Cached |

This means we can navigate to a package directory **on demand** when a specific package is first
queried, instead of recursively walking the entire source tree at startup.

Reference implementation: `JvmDependenciesIndexImpl` (binary class index in
`compiler/cli/cli-base/`) uses exactly this pattern — it caches which roots contain which
packages and navigates via `findChild` chains, never walking the full directory tree eagerly.

## 3. Design Overview

### 3.1 Current Architecture

```
init {} → buildIndex()
  └─ walkSourceRoots()           // recursive VFS walk over ALL .java files
     └─ for each file:
        ├─ tryBuildFileEntryWithFullParse()    // small files: parse + cache JavaClass
        └─ tryBuildFileEntryLightweight()      // large files: line scan, defer parse
     → populates: index[FqName → [className → List<FileEntry>]]
```

All callers (`findClass`, `findPackage`, `knownClassNamesInPackage`, `isClassInIndex`,
`classesInPackage`, `subPackagesOf`) read from the pre-populated `index`.

### 3.2 Proposed Architecture

```
init {} → (nothing, or index rare file-type source roots only)

Any API call for package P:
  └─ ensurePackageIndexed(P)
     └─ index.computeIfAbsent(P) { indexPackageFromDirectories(P) }
        └─ for each source root:
           ├─ findPackageDirectory(root, P)   // findChild("com")?.findChild("example")
           └─ list .java files in that directory
              └─ same tryBuildFileEntry logic as before
```

Each package is indexed **at most once** (via `computeIfAbsent`). Packages never queried are
never scanned. After indexing, all reads are plain map lookups — identical cost to today.

## 4. Detailed Implementation

### 4.1 New Fields

```kotlin
// Replaces the old HashMap-based index.
// Outer: ConcurrentHashMap for thread-safe per-package lazy population.
// Inner: immutable Map built atomically during computeIfAbsent.
private val index: ConcurrentHashMap<FqName, Map<String, List<FileEntry>>> = ConcurrentHashMap()

// Cache: package FqName → list of directories (one per source root that has this package).
// Populated via computeIfAbsent — each package resolved at most once.
private val packageDirectoryCache: ConcurrentHashMap<FqName, List<VirtualFile>> = ConcurrentHashMap()

// Directory source roots (used for lazy indexing) vs file source roots (indexed eagerly).
private val directoryRoots: List<VirtualFile>  // filtered in init
```

### 4.2 Init

```kotlin
init {
    // Classify source roots.
    val (fileRoots, dirRoots) = sourceRoots.partition { !it.isDirectory }
    directoryRoots = dirRoots

    // File-type source roots are a test-only edge case (e.g., a single .java file as root).
    // Index them eagerly — there are at most a handful.
    for (fileRoot in fileRoots) {
        if (fileRoot.name.endsWith(".java")) {
            indexSingleFile(fileRoot)
        }
    }
}
```

`indexSingleFile` runs the existing `tryBuildFileEntry` logic for a single file and merges
the result into `index`.  This handles the `testClassFinderWithPackage` test that passes a
`.java` file as a source root with a package declaration that doesn't match the file location.

### 4.3 `findPackageDirectories`

Navigates from each source root to the directory corresponding to a package, caching results.

```kotlin
private fun findPackageDirectories(packageFqName: FqName): List<VirtualFile> {
    if (packageFqName.isRoot) return directoryRoots
    return packageDirectoryCache.computeIfAbsent(packageFqName) {
        val segments = packageFqName.pathSegments().map { it.asString() }
        directoryRoots.mapNotNull { root ->
            var dir: VirtualFile = root
            for (segment in segments) {
                dir = dir.findChild(segment) ?: return@mapNotNull null
                if (!dir.isDirectory) return@mapNotNull null
            }
            dir
        }
    }
}
```

Cost: O(depth) VFS lookups per root on first call; O(1) CHM `get` on subsequent calls.
For `com.example.sub` with 2 source roots: 6 `findChild` calls maximum on cold path.

### 4.4 `ensurePackageIndexed`

The core lazy-indexing method.  Every public API method calls this before reading from `index`.

```kotlin
private fun ensurePackageIndexed(packageFqName: FqName): Map<String, List<FileEntry>> {
    return index.computeIfAbsent(packageFqName) { fqName ->
        indexPackageFromDirectories(fqName)
    }
}
```

`computeIfAbsent` is atomic: only one thread indexes a given package; other threads
block until the lambda completes, then see the finished result.

### 4.5 `indexPackageFromDirectories`

Replaces `buildIndex` for a single package.

```kotlin
private fun indexPackageFromDirectories(packageFqName: FqName): Map<String, List<FileEntry>> {
    val dirs = findPackageDirectories(packageFqName)
    if (dirs.isEmpty()) return emptyMap()

    val byName = HashMap<String, MutableList<FileEntry>>()

    for (dir in dirs) {
        val children = dir.children ?: continue
        for (file in children) {
            if (file.isDirectory) continue
            val name = file.name
            if (!name.endsWith(".java")) continue

            if (name == "package-info.java") {
                indexPackageInfo(file, packageFqName)
                continue
            }

            val entry = tryBuildFileEntry(file, packageFqName) ?: continue
            // tryBuildFileEntry validates that the file's declared package matches
            // packageFqName.  Mismatched files return null.
            for (className in entry.topLevelClassNames) {
                byName.getOrPut(className) { mutableListOf() }.add(entry)
            }
        }
    }

    return byName  // immutable after this point — never modified, only read
}
```

The returned map is stored via `computeIfAbsent` and is never mutated, so concurrent reads from
any thread are safe without further synchronization.

### 4.6 Modified `tryBuildFileEntry`

Adds an `expectedPackage` parameter for directory/package validation:

```kotlin
private fun tryBuildFileEntry(file: VirtualFile, expectedPackage: FqName): FileEntry? {
    return if (file.length <= SMALL_FILE_SIZE_THRESHOLD) {
        tryBuildFileEntryWithFullParse(file, expectedPackage)
    } else {
        tryBuildFileEntryLightweight(file, expectedPackage)
    }
}
```

**Full-parse path** — adds a package validation check:
```kotlin
private fun tryBuildFileEntryWithFullParse(file: VirtualFile, expectedPackage: FqName): FileEntry? {
    val source = sourceFileReader.readFileContent(file) ?: return null
    val tree = parseJavaToLightTree(source, 0)
    val root = tree.getRoot()

    // Extract actual package from AST
    val packageStmt = tree.findChildByType(root, JavaSyntaxElementType.PACKAGE_STATEMENT)
    val packageName = packageStmt?.let { /* ... existing logic ... */ }
    val packageFqName = if (packageName != null) FqName(packageName) else FqName.ROOT

    // NEW: validate against expected directory-derived package
    if (packageFqName != expectedPackage) return null

    // ... rest unchanged (extract class names, eagerly cache JavaClass instances) ...
}
```

**Lightweight path** — same pattern:
```kotlin
private fun tryBuildFileEntryLightweight(file: VirtualFile, expectedPackage: FqName): FileEntry? {
    val info = extractFileInfoLightweight(file, sourceFileReader) ?: return null
    val actualPackage = if (info.packageName != null) FqName(info.packageName) else FqName.ROOT

    // NEW: validate
    if (actualPackage != expectedPackage) return null

    // ... rest unchanged ...
}
```

Files whose declared package doesn't match their directory are skipped. This is the same behavior
as `javac`, which requires package declarations to match directory structure.

### 4.7 Modified `indexPackageInfo`

Needs to accept `expectedPackage` to validate that the `package-info.java` belongs to the
expected package. Also, `packageAnnotationNodes` must be a `ConcurrentHashMap` since different
packages can be indexed concurrently.

```kotlin
// Changed from HashMap to ConcurrentHashMap
private val packageAnnotationNodes: ConcurrentHashMap<FqName, List<JavaAnnotation>> =
    ConcurrentHashMap()

private fun indexPackageInfo(file: VirtualFile, expectedPackage: FqName) {
    val source = sourceFileReader.readFileContent(file) ?: return
    val tree = parseJavaToLightTree(source, 0)
    val root = tree.getRoot()

    val packageStmt = tree.findChildByType(root, JavaSyntaxElementType.PACKAGE_STATEMENT)
    val packageName = /* ... extract ... */
    val packageFqName = if (packageName != null) FqName(packageName) else FqName.ROOT

    // Validate
    if (packageFqName != expectedPackage) return

    // ... extract annotations as before ...

    if (annotations.isNotEmpty()) {
        packageAnnotationNodes[packageFqName] = annotations.toList()  // immutable snapshot
    }
}
```

### 4.8 Modified Public API Methods

Each method gets a single `ensurePackageIndexed` call at the top.  The rest of the logic
stays the same, except `index[fqName]` lookups now hit a `ConcurrentHashMap`.

**`isClassInIndex`**:
```kotlin
fun isClassInIndex(classId: ClassId): Boolean {
    val topLevelName = classId.relativeClassName.pathSegments().firstOrNull()?.asString()
        ?: return false
    return ensurePackageIndexed(classId.packageFqName).containsKey(topLevelName)
}
```

Rationale for triggering full indexing (not just filename check): non-canonical classes like
`class Helper` inside `Main.java` would be missed by `findChild("Helper.java")`. The
`testNonCanonicalTopLevelClassVisibility` test verifies this behavior.

**`findClass`** / **`findClasses`**: calls `ensurePackageIndexed` at the top of `findClasses`;
rest unchanged.

**`findPackage`**: calls `ensurePackageIndexed`; checks `index.containsKey(fqName)`.

**`knownClassNamesInPackage`**: calls `ensurePackageIndexed`; rest unchanged.

**`classesInPackage`**: calls `ensurePackageIndexed`; rest unchanged.

**`subPackagesOf`**: **does NOT call `ensurePackageIndexed`** — uses directory listing directly:

```kotlin
internal fun subPackagesOf(fqName: FqName): Collection<FqName> {
    val dirs = if (fqName.isRoot) directoryRoots else findPackageDirectories(fqName)
    val result = mutableSetOf<FqName>()
    for (dir in dirs) {
        val children = dir.children ?: continue
        for (child in children) {
            if (child.isDirectory) {
                result.add(fqName.child(Name.identifier(child.name)))
            }
        }
    }
    return result
}
```

This is simpler and faster than the current implementation (which iterates all index keys
with `asString().startsWith(prefix)` matching — O(total_packages * string_length)).

### 4.9 `sameClassInSameFilePackage` Callback

The `JavaSupertypeGraph` receives a callback `sameClassInSameFilePackage: (FqName, String) -> Boolean`
that checks the index.  Update the callback to trigger lazy indexing:

```kotlin
sameClassInSameFilePackage = { pkg, name ->
    ensurePackageIndexed(pkg).containsKey(name)
}
```

### 4.10 `findFilesForClass`

Called by `JavaSupertypeGraph` to get candidate files for a class.  Must also trigger indexing:

```kotlin
private fun findFilesForClass(classId: ClassId): List<FileEntry> {
    val topLevelName = classId.relativeClassName.pathSegments().firstOrNull()?.asString()
        ?: return emptyList()
    val byName = ensurePackageIndexed(classId.packageFqName)
    return byName[topLevelName] ?: emptyList()
}
```

### 4.11 `indexSingleFile` (file-type source roots)

Handles the rare case where a source root is a single `.java` file (not a directory).  Used
only in tests today.

```kotlin
private fun indexSingleFile(file: VirtualFile) {
    // No expected-package validation — the file can declare any package.
    val entry = tryBuildFileEntryNoValidation(file) ?: return
    index.compute(entry.packageFqName) { _, existing ->
        val byName = existing?.toMutableMap() ?: HashMap()
        for (className in entry.topLevelClassNames) {
            val list = (byName[className] as? MutableList) ?: mutableListOf<FileEntry>().also { byName[className] = it }
            list.add(entry)
        }
        byName
    }
}
```

Or, more simply: run the old `tryBuildFileEntry(file)` (without package validation) and merge
into `index`.  Since this runs in `init {}` (single-threaded), we can use a simple
`HashMap` initially and then wrap it.  The simplest approach: keep the existing
`tryBuildFileEntryWithFullParse` / `tryBuildFileEntryLightweight` unchanged (they don't need
validation for file roots) and have the validated variants as new overloads.

### 4.12 `walkSourceRoots` — No Longer Used

The `walkSourceRoots` method on `JavaSourceFileReader` was designed for the eager full-tree walk
in `buildIndex`.  With lazy indexing, it is no longer called from `JavaClassFinderOverAstImpl`.
Options:
- Keep it for backward compatibility (other potential callers, tests)
- Mark it `@Deprecated` with a note about the lazy approach
- Leave as-is (no action needed — unused code is harmless)

## 5. Thread Safety Analysis

| Field | Contention | Safety Mechanism |
|-------|-----------|------------------|
| `index: ConcurrentHashMap<FqName, Map<...>>` | Different threads index different packages concurrently | `computeIfAbsent`: one thread per key, happens-before for reads |
| `packageDirectoryCache: ConcurrentHashMap<FqName, List<VirtualFile>>` | Same pattern | `computeIfAbsent` |
| `packageAnnotationNodes: ConcurrentHashMap<FqName, List<JavaAnnotation>>` | Written during package indexing | Written inside `computeIfAbsent` lambda that is serialized per-package |
| `classCache: ConcurrentHashMap<ClassId, JavaClass>` | Already concurrent | No change |
| `negativeClassCache: ConcurrentHashSet<ClassId>` | Already concurrent | No change |
| `packageCache: ConcurrentHashMap<FqName, JavaPackage>` | Already concurrent | No change |
| Inner maps (`Map<String, List<FileEntry>>`) | Immutable after creation | Created in `computeIfAbsent`, returned, never modified |

**Potential deadlock**: `computeIfAbsent` on `ConcurrentHashMap` can deadlock if the lambda
recursively triggers another `computeIfAbsent` on the **same** map for a **different** key
(this is a known `ConcurrentHashMap` limitation in JDK 8+). Can this happen?

- `indexPackageFromDirectories(pkg)` calls `tryBuildFileEntryWithFullParse` which creates
  `JavaResolutionContext` and caches `JavaClass` instances.  None of these trigger
  `ensurePackageIndexed` for a different package.
- `indexPackageInfo` only writes to `packageAnnotationNodes`, not to `index`.
- `findPackageDirectories` uses its own separate `packageDirectoryCache` CHM, not `index`.

So no, the recursive-`computeIfAbsent`-on-same-CHM scenario does not arise.

## 6. Changes to Existing Tests

### Tests that need adaptation

| Test | Issue | Fix |
|------|-------|-----|
| `testClassFinderWithPackage` | Source root is a single FILE (`Hello.java`) declaring `package example`, not in an `example/` subdirectory | Handled by `indexSingleFile` in init — file roots are indexed eagerly without directory/package validation |

### Tests that work unchanged

All other `JavaParsingClassFinderTest` methods create proper directory structures matching
packages (e.g., `tempDir/com/example/ClassA.java` with `package com.example`). Lazy indexing
will scan the directory when the package is first queried.

### Tests that implicitly validate the change

- `testNonCanonicalTopLevelClassVisibility`: verifies that `Helper` (secondary class in
  `Main.java`) is findable by `findClass` — confirms `isClassInIndex` must index the package,
  not just check filename
- `testMultiFileClassFinder`: multi-package, cross-package resolution
- `testInheritedInnerClassCrossPackage`: cross-package supertype resolution triggers
  `sameClassInSameFilePackage` which must trigger indexing

## 7. Estimated Performance Impact

### Profiling baseline (testFrontend, 141 Java files)

| Metric | Current (eager) | With lazy indexing | Delta |
|--------|----------------|--------------------|-------|
| `buildIndex` CPU | 156ms | 0ms (shifted to first access) | –156ms init |
| Per-package indexing | 0ms (pre-done) | ~5–10ms per package on first access | distributed |
| `isClassInIndex` (hot path) | ~ns (HashMap lookup) | ~ns (CHM lookup, after first call) | ~same |
| `subPackagesOf` | O(all_packages), string ops | O(dir_children), VFS cached | faster |
| Total java-direct CPU | ~389ms | ~389ms if all packages accessed, less if subset | 0 to –78ms |
| java-direct fraction of pipeline | ~6% | ~6% or less | — |

### Where the real wins are

1. **Modules with partial Java usage**: If Kotlin code only references 50% of Java packages,
   ~78ms saved from deferred indexing. The saved work is not just shifted — it's eliminated.

2. **Large source roots**: For roots with hundreds of packages, the current O(all_files) walk
   becomes O(accessed_packages * files_per_package). If access is sparse, this is dramatically
   better.

3. **Incremental compilation**: Only the changed packages need re-indexing. The directory-based
   approach naturally supports this (just invalidate the package's cache entry).

4. **`subPackagesOf`**: The current O(all_packages) string-prefix scan is replaced by
   O(children_in_directory) VFS listing. For a root with 200 packages, this is a measurable win.

## 8. Implementation Steps

### Step 1: Add `findPackageDirectories` and `ensurePackageIndexed`

Add the new methods without changing any existing behavior. The old `buildIndex` still runs.
This allows testing the new methods in isolation.

### Step 2: Add `expectedPackage` parameter to `tryBuildFileEntry`

Create new overloads that accept `expectedPackage: FqName` and validate. Keep old signatures
for backward compatibility (used by `indexSingleFile` for file-type roots).

### Step 3: Replace `buildIndex` with lazy init

- Remove the `buildIndex()` call from `init {}`
- Partition source roots into file roots (indexed eagerly) and directory roots (lazy)
- Add `ensurePackageIndexed` calls to all public API methods
- Change `index` type from `HashMap` to `ConcurrentHashMap<FqName, Map<String, List<FileEntry>>>`
- Change `packageAnnotationNodes` to `ConcurrentHashMap`
- Update `sameClassInSameFilePackage` callback
- Rewrite `subPackagesOf` to use directory listing

### Step 4: Run tests, profile, validate

- `./gradlew :kotlin-java-direct:test` — all 2769 tests must pass
- `./gradlew dist` + `KotlinFullPipelineTestsGenerated` — pipeline correctness
- Profiling with `ThreadMXBean` counters to measure actual CPU savings

### Step 5: Cleanup

- Consider removing `walkSourceRoots` if no longer called
- Update `ITERATION_RESULTS.md` with results
- Update `ARCHITECTURE.md` if it references `buildIndex`

## 9. Risks and Mitigations

| Risk | Severity | Mitigation |
|------|----------|------------|
| Non-canonical class missed by filename-only check | High (correctness) | `isClassInIndex` triggers full package indexing, not just filename check |
| Files with package/directory mismatch become invisible | Low (matches javac behavior) | Document explicitly; these files are already problematic in javac |
| VFS first-access I/O cost for deep packages | Low | VFS caches directory listings; first access is one `listFiles` per directory level |
| `ConcurrentHashMap.computeIfAbsent` deadlock | Medium | Verified: no recursive `computeIfAbsent` on the same CHM instance; see §5 |
| File-type source roots break | Low (test-only) | Handled by `indexSingleFile` in init |
| `subPackagesOf` returns directories that have no .java files | Low | Current behavior also returns packages for binary-only code; `findPackage` gates on having actual entries |
