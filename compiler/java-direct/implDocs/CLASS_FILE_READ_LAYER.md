# The class-file read layer: what replaces `KotlinBinaryClassCache`

*Status: analysis + proposals. Written 2026-08-11, together with the move of `BinaryJavaClassCache` into
`FirJvmSessionFactory.Context`. Only the cheap first step of §6 has landed (2026-08-11): the
`BinaryClassFileHandle` identity/stamp contract and the handle-keyed `BinaryJavaClassCache.classes`.
Approaches A–D are unimplemented.*

Companion of `BINARY_CLASS_CACHE_LIFETIME.md`, which asks *how long* the Java-model cache may live.
This document asks a different question: **which entity should own reading a `.class` file** once the
compiler is PSI/VFS-free, given that today two independent stacks read the very same files — the
Kotlin one through `KotlinBinaryClassCache`, the Java one through `BinaryJavaClassCache`.

## 1. What `KotlinBinaryClassCache` actually is

`compiler/frontend.common.jvm/src/org/jetbrains/kotlin/load/kotlin/KotlinBinaryClassCache.kt`:

- An IntelliJ application **service** (`Disposable`), registered by `KotlinCoreEnvironment` for the
  CLI and by `StandaloneProjectFactory` for Standalone AA.
- Its state is a `ThreadLocal<RequestCache>`, and a `RequestCache` holds **exactly one entry**:
  `virtualFile`, `modificationStamp`, `result`. A request for any other file evicts it. `dispose()`
  nulls the entries and calls `cache.remove()`, with a comment stating that otherwise each instance
  "would transitively retain VFS resulting in OutOfMemoryError".
- The value is a `KotlinClassFinder.Result`: either `KotlinClass` — a `VirtualFileKotlinClass` (parsed
  `KotlinClassHeader`, and it **retains the `VirtualFile`**) plus the class-file `ByteArray` — or
  `ClassFileContent` (bytes only, for a non-Kotlin class file).
- The miss path is `VirtualFileKotlinClass.create(file, metadataVersion, fileContent, perfManager)`
  inside `ApplicationManager.runReadAction`, i.e. read bytes + parse the `@Metadata` header.

So, despite the name, it is **not** a classpath cache. It is a *one-deep memo of the last file read on
this thread*, which exists because the call pattern immediately asks for the same file again (find the
class, then read its header, then read its content).

## 2. Who reaches it, and how

- The single compiler entry point is `VirtualFileFinder.findKotlinClassOrContent` (both overloads:
  by `ClassId`, and by `JavaClass` via `VirtualFileBoundJavaClass.virtualFile`). Everything on the CLI
  path arrives here: `CliVirtualFileFinder` → `KotlinClassFinder` →
  `JvmClassFileBasedSymbolProvider` (`computePackagePartInfo` line 78, `extractClassMetadata`
  line 178) and the deserializers below it.
- The IDE/AA side wraps it in `ClsKotlinBinaryClassCache`
  (`analysis/decompiled/decompiler-to-file-stubs/.../ClsKotlinBinaryClassCache.kt`), which adds a
  VFS-`UserData`/`FileAttributes` layer on top and is used by `ClsClassFinder`,
  `DirectoryBasedClassFinder`, `KotlinClsStubBuilder`, `KotlinClassFileDecompiler`.
- JPS and kapt reach it only transitively, by running the frontend.
- PSI/VFS coupling to remove: `VirtualFile` (key *and* retained value), `ApplicationManager`
  (service lookup + read action), `JavaClassFileType` and `PsiJavaModule.MODULE_INFO_CLS_FILE`
  (the two guards at the top of `getKotlinBinaryClassOrClassFileContent`), `Disposable`.

## 3. The duplication this creates on the java-direct path

For one binary class `Foo` that turns out to be a *Java* class, the compiler today:

1. locates the class file twice — `VirtualFileFinder.findVirtualFileWithHeader` through the
   `JvmDependenciesIndex`, and `BinaryClassFileIndex.findTopLevelClassFiles` through the same index;
2. reads the bytes once on the Kotlin side (to discover there is no `@Metadata`) and then hands them
   to the Java side as `knownContent` (`JvmClassFileBasedSymbolProvider.extractClassMetadata`
   line 184), so the *bytes* are in fact already shared — but only for this one call shape;
3. parses the file twice: once as a `KotlinClassHeader`, once as a `BinaryJavaClass`.

The bytes-sharing in (2) is why the header parse is cheap to keep separate, and why the interesting
overlap is **(1) the lookup**, not the parsed models. `BinaryJavaClassCache.topLevelClassFiles` and
`VirtualFileFinder`'s per-scope lookups answer literally the same question.

## 4. What the successor entity has to provide

Stripped of PSI, the role is:

- **identity** of a class file, usable as a cache key and stable across the sessions of one
  compilation — VFS gives `VirtualFile` + `modificationStamp`; a PSI-free layer needs a handle with
  an identity and a stamp. `BinaryClassFileHandle` now *is* that: implementations must define
  `equals`/`hashCode` over the file identity together with its content version, and the VFS
  implementation snapshots `modificationStamp` when the handle is created;
- **bytes on demand**, once per file, so that the Kotlin header parse and the Java model build do not
  read twice;
- **a place for the header parse result**, since `KotlinClassHeader` is what decides which of the two
  models is built at all;
- **an explicit lifetime and disposal**, replacing `Disposable` + the thread-local removal, because
  whatever holds bytes or models keeps memory alive (and, until `BinaryClassFileHandle` stops
  exposing `virtualFile`, keeps the VFS alive too).

## 5. Approaches

### A. Leave `KotlinBinaryClassCache` alone; keep the two stacks separate

Do nothing beyond what has landed: the Kotlin side keeps its thread-local one-deep memo, the Java side
keeps `BinaryJavaClassCache`, and the bytes continue to be passed across as `knownContent`.

- *Pro*: zero risk; the duplicated *parse* is already avoided for the common case by `knownContent`;
  the duplicated *lookup* is an index hit, not a file read.
- *Con*: the Kotlin side stays PSI/VFS-bound, so the PSI-free goal is not reachable for the Kotlin
  binary path; nothing is shared by construction, so the two caches can disagree about which file wins
  for a name.
- *When*: correct until the Kotlin binary path is actually in scope for PSI removal (per
  `PSI_FREE_ROADMAP.md` §6 it is not today).

### B. A `ClassFileReader`/`BinaryClassFileContent` layer under both stacks (recommended target)

Introduce one per-compilation object in `frontend.common.jvm/.../classFiles/` that owns
*locate → read bytes → parse `@Metadata` header*, keyed by class-file handle:

```
class BinaryClassFileContents(index: BinaryClassFileIndex) {
    fun bytes(file: BinaryClassFileHandle): ByteArray
    fun kotlinHeader(file: BinaryClassFileHandle): KotlinClassHeader?   // null => Java class file
}
```

`BinaryJavaClassCache` then reads its bytes from here, and a PSI-free `KotlinClassFinder`
implementation (the analogue of `CliVirtualFileFinder`, over `BinaryClassFileIndex` instead of
`VirtualFileFinder`) does too. `KotlinBinaryClassCache` stays for PSI callers and simply stops being
on the java-direct path.

- *Pro*: kills the double lookup and the double read; the header parse decides once which model to
  build; both stacks become functions of the classpath, which is the precondition for any longer-lived
  cache (see `BINARY_CLASS_CACHE_LIFETIME.md`).
- *Con*: needs a second `KotlinClassFinder` implementation, which is a much bigger step than the Java
  side was, because `KotlinClassFinder.Result.KotlinClass` is `KotlinJvmBinaryClass` — whose PSI-based
  implementation (`VirtualFileKotlinClass`) is also used for `containingLibraryPath`, source-file
  attribution and by the decompiler.
- *Cost driver*: not the cache, but making `KotlinJvmBinaryClass` constructible without a
  `VirtualFile`.

### C. Merge into a single "binary classpath session state" object

Go one step further than B: one object per compilation that owns the index, the contents, the Kotlin
headers *and* the Java models (today `BinaryJavaClassCache` is exactly the first half of that).

- *Pro*: one lifetime, one disposal point, one obvious place for the BTA-supplied longer-lived
  instance; one object to hand over instead of three.
- *Con*: couples the Kotlin and Java binary readers in one type before there is a real reason to;
  makes the object the single hottest shared structure, so its thread-safety story must be solved up
  front rather than later.
- *When*: after B has proven the split, not instead of it.

### D. Push the role down to a platform-free file abstraction (NIO)

Have the read layer sit on the platform-free axis (`java.nio.file.Path` / jar `FileSystem`) rather
than on `BinaryClassFileHandle`, so that the same object serves the NIO axis tracked in
`PSI_FREE_ROADMAP.md`.

- *Pro*: removes the last VFS reachability (point 3 of `BINARY_CLASS_CACHE_LIFETIME.md`) instead of
  hiding it behind a handle; a `Path` + `lastModifiedTime` is a natural cross-build key.
- *Con*: the CLI classpath index is VFS-based (`JvmDependenciesIndex`), so this depends on the NIO
  work landing first; jar `FileSystem` handles have their own lifetime/pinning problem, which is what
  BTA's `ApplicationEnvironmentPin`/`clearJarCaches()` currently manages for VFS.

## 6. Recommendation

- Keep **A** as the current state; nothing here is a regression.
- Treat **B** as the target shape, but note that its real cost is `KotlinJvmBinaryClass` without a
  `VirtualFile`, which should be scoped as its own investigation before anything is built.
- Defer **C** until B exists, and **D** until the NIO axis lands.
- The cheap first step is **done** (2026-08-11): `BinaryClassFileHandle` carries the identity/stamp
  contract, and the classes read from class files live in `BinaryJavaClasses`, keyed by the handle they
  were read from plus the `ClassId` inside that file (one class file declares its top-level class and
  all the classes nested in it, so the handle alone is not a key). This removed the "which root won for
  whoever asked first" caveat, it applies to the PSI `KotlinCliJavaFileManagerImpl` cache as well since
  both share `readBinaryJavaClass`, and it is the key that both B and any cross-build cache need.

## 7. Does `BinaryJavaClassCache` retain more than the PSI path did?

Asked because `KotlinBinaryClassCache` is not really a cache, so it is not the thing to compare against.
The counterpart of `BinaryJavaClassCache` is `KotlinCliJavaFileManagerImpl`, whose caches are
project-level, i.e. per compilation as well:

| PSI path | java-direct path |
|----------|------------------|
| `topLevelClassesCache`: `FqName` -> `SmartList<VirtualFile>` | `topLevelClassFiles`: package -> name -> `Collection<BinaryClassFileHandle>` |
| `binaryCache`: `ClassId` -> `JavaClass?` | `classes`: handle + `ClassId` -> `JavaClass?` |
| not cached (recomputed per call; `FirJavaFacade` caches it per session) | `classFileNamesInPackage`: package -> names |
| `KotlinBinaryClassCache`: one `KotlinClassFinder.Result` per thread, **including the class-file bytes** | — |

So the answer is **no new kind of retained data, and no new order of magnitude**: the dominant items —
the parsed `BinaryJavaClass` models and the per-name candidate file lists — are the same objects the PSI
file manager already kept for the whole compilation, and neither path retains class-file bytes (the
`classContent` of `BinaryJavaClass` is a constructor parameter, not a field; the only bytes retained
anywhere are the single per-thread entry of `KotlinBinaryClassCache`, which exists on both paths).

The differences are bookkeeping, all bounded by the number of class files actually queried:

- one `BinaryClassFileHandle` wrapper (a `VirtualFile` reference plus a `long` stamp) per candidate
  class file, where the PSI list holds the `VirtualFile` directly;
- one inner map per class file in `classes`, instead of one flat map for the compilation;
- up to one entry per root for a name declared in several roots, where the `ClassId` key kept one —
  which is the point of the new key, not a leak;
- `classFileNamesInPackage` is new *at the compilation level*, but the same sets were already retained
  per session by `FirJavaFacade.knownClassNamesInPackage` on both paths, so sharing them across
  sessions removes duplication rather than adding data.

What java-direct does *not* have, and the PSI path does, is a way to trade this for less memory:
`usePsiClassFilesReading` reads through PSI stubs instead of `BinaryJavaClass`. That is a property of the
module, not a consequence of this cache.
