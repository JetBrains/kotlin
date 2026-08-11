# Binary Java class caches: lifetime beyond one compilation

*Status: open design question, not implemented. Written 2026-08-11, after the per-compilation
`BinaryJavaClassCache` landed.*

## 1. What exists today

`BinaryJavaClassCache`
(`compiler/frontend.common.jvm/src/.../load/java/structure/impl/classFiles/BinaryJavaClassCache.kt`) is
created once per compilation in `JvmFrontendPipelinePhase.prepareJvmSessions`, held by
`FirJvmSessionFactory.Context.binaryJavaClassCache` (`null` on the PSI path) and shared by the
`JavaClassFinderOverBinaryIndex` of every binary session. It holds:

| Cache | Key | Value |
|-------|-----|-------|
| `topLevelClassFiles` | package `FqName` + top-level `Name` | the class files of that name, in classpath order |
| `classes` | `BinaryClassFileHandle` + `ClassId` | the loaded `BinaryJavaClass` (or `null`) |
| `classFileNamesInPackage` | package `FqName` | class-file names in the package directory |

The session's own visibility is applied *after* the lookup, through the `BinaryClassFileScope`
handed to each finder. This is the shape of the incumbent `KotlinCliJavaFileManagerImpl`
(project-level `topLevelClassesCache` / `binaryCache`, scope filter applied after the cache), so
java-direct is now at parity with PSI on cache width. Nothing survives a compilation.

## 2. Why going further is feasible

The parsed model is session-independent: `readBinaryJavaClass` builds a `ClassifierResolutionContext`
over a plain `(ClassId) -> JavaClass?` resolver, and `JavaClassFinderOverBinaryIndex` holds no
`FirSession` (unlike the AST side, which does). A `BinaryJavaClass` is therefore a function of the
classpath, not of the compilation — which is exactly the property that a cross-build cache needs.

The injection point is already in place: `BinaryJavaClassCache` is a plain object constructed by the
caller and handed to `FirJvmSessionFactory.Context`, so a build session can supply a longer-lived
instance by passing a different one — no other signature has to change.

## 3. The four questions that must be answered first

1. **Cache key and invalidation.** `ClassId` is not a sufficient key across builds: the same id can
   come from a different jar in a different module's classpath, and jars change between builds. The
   key has to carry the file identity plus a modification stamp, or the entries have to be bucketed
   per classpath entry keyed by that entry's identity/stamp. Half of this has landed: `classes` is
   keyed by the `BinaryClassFileHandle` the class was read from plus its `ClassId`, and
   `BinaryClassFileHandle` requires `equals`/`hashCode` over the file identity *and* its content
   version (`modificationStamp` for the VFS implementation), so within one compilation two roots
   declaring the same name no longer share an entry. What is still missing for a cross-build cache is
   a *stable* identity across builds — the VFS handle's `equals` is a `VirtualFile` identity, i.e. it
   only survives as long as the application environment does.
2. **Memory.** A retained model of the JDK plus the full classpath is unbounded. Everything in the
   compiler that currently outlives a compilation is bounded or trivially droppable:
   `KotlinBinaryClassCache` is a one-element thread-local, and BTA's `ApplicationEnvironmentPin`
   retains only jar handlers, dropped by `clearJarCaches()` on close.
3. **Reachability.** `BinaryJavaClass` still holds a `VirtualFile` (through
   `BinaryClassFileHandle.virtualFile`), so retaining the model retains the VFS, and a cross-build
   cache pins the application environment as a side effect. Acceptable in a daemon, but it must be a
   decision rather than an accident.
4. **Where it plugs in.** BTA already has the right shape: `KotlinToolchains.BuildSession` executes
   several operations "while retaining certain caches", carries a `ProjectId`, and has `close()` /
   `finishProjectCompilation(projectId)` as teardown points;
   `BuildOperationImpl.usesApplicationEnvironment` is the existing precedent for an operation opting
   into cross-operation reuse. The cache would be created by the build session and handed to
   `FirJvmSessionFactory.Context` in place of the one `prepareJvmSessions` builds today, defaulting to
   a fresh instance for plain CLI runs.

## 4. Additional constraints to check when this is picked up

- **Thread safety.** The current maps are plain `HashMap`s, sound because one compilation drives them
  sequentially. A cache shared by concurrent build operations needs concurrent maps, and the
  `getOrPut` around `readBinaryJavaClass` is re-entrant (nested classes recurse into it), which rules
  out a naive `ConcurrentHashMap.computeIfAbsent`.
- **The Kotlin side.** The Kotlin binary stack has its own cache (`KotlinBinaryClassCache`) and reads
  the same files; what a shared, PSI-free read layer would look like is a separate question, written up
  in `CLASS_FILE_READ_LAYER.md`. Any cross-build design here should be checked against it, because both
  need the same class-file identity/stamp key.
- **Scope of applicability.** java-direct serves *all* binary Java class finding only on the FIR/JVM
  CLI path under `-Xjava-direct`; `KotlinCliJavaFileManagerImpl` stays for K1, kapt and
  `CoreJavaDirectoryService`. Any measurement or design here optimises that path only.

## 5. Recommendation

Do not add the injection parameter until there is a measurement of what a warm daemon actually
re-parses per build. The parameter shape is a one-line change; the invalidation contract and the
memory policy are the real work, and both need numbers to be designed against.
