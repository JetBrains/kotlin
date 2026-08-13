# PSI-Free Roadmap (java-direct)

Current as of 2026-08-12. Replaces `implDocs/archive/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`
and `implDocs/archive/BINARY_SOURCE_DIVIDE_REVIEW_2026_07_22.md`.

## 1. Two axes, not one

Every earlier document treated "PSI-free" and "platform-free" as a single goal, and deferred both.
They are separable, and only the first one is done:

| Axis | Meaning | State |
|------|---------|-------|
| **PSI-free** | no `com.intellij.psi.*`, `com.intellij.core.*`, `KotlinCliJavaFileManagerImpl`, `GlobalSearchScope` | **done for `compiler/java-direct/src`**, enforced by `JavaDirectModuleBoundaryTest`; since 2026-08-12 also for the *shared API*, which no longer carries a search scope at all |
| **platform-free** | no `VirtualFile`, no `JvmDependenciesIndex`, no IntelliJ application environment | not started; the seam is prepared |

`GlobalSearchScope` is the trap on the first axis: it lives in package `com.intellij.psi.search`
even though it is semantically a `VirtualFile` predicate, so it counts as PSI. It used to be smuggled
across module boundaries as `AbstractProjectFileSearchScope`, an interface with one implementation
and a downcast at every point of use; that type is now deleted — see §3.

## 2. What java-direct depends on now

`compiler/java-direct/build.gradle.kts` no longer has `implementation(project(":compiler:cli"))`
nor `implementation(project(":compiler:fir:entrypoint"))`; the `main` source set sees
`frontend.common`, `frontend.common.jvm`, `fir:resolve`, `fir:fir-jvm`, `plugin-api`,
`core:compiler.common.jvm` and `compileOnly(intellijCore())`.

The `fir:entrypoint` edge existed only because the abstractions java-direct implements were
declared next to the PSI default that also implements them. They now live below it:
`FirJavaInterop` in `:compiler:fir:fir-jvm` next to the `FirJavaFacade` it produces, and
`JvmCompilationEnvironment` + `JvmClasspath` in `:compiler:frontend.common.jvm` (package
`org.jetbrains.kotlin.jvm.environment`; the environment was formerly `AbstractProjectEnvironment`
in `fir:entrypoint`). The environment hands out only a `KotlinClassFinder`, a `PackagePartProvider`
and a `JavaModuleResolver`, and mentions FIR nowhere — so a PSI-free environment is expressible.

The only IntelliJ types left in the module are the standalone syntax library
(`com.intellij.java.syntax.*`, `com.intellij.platform.syntax.*`) plus the light-tree types
`Language`, `LighterASTNode`, `Ref`, `LanguageLevel`, `IElementType` and
`FlyweightCapableTreeStructure`. They go away together with the Kotlin light tree, which is a
separate effort. `JavaDirectModuleBoundaryTest` pins exactly this allowlist against the module's
compiled output, so anything else is a test failure rather than a silent regression.

## 3. Seams introduced

| Seam | Where | Purpose |
|------|-------|---------|
| `BinaryClassFileIndex`, `BinaryClassFileHandle` (`isUnder(Path)`) | `frontend.common.jvm/.../classFiles/BinaryClassFileIndex.kt` | the binary classpath of one compilation, with no scope object and no `JvmDependenciesIndex`. A session's part of it is *not* a seam type: `JavaClassFinderOverBinaryIndex` takes the session's `JvmClasspath` and tests root membership itself (`internal operator fun JvmClasspath.contains(BinaryClassFileHandle)`), so the only thing shared is the root check on the handle |
| `JvmClasspath` (sealed: `Roots` / `ProjectLibraries(excludedRoots)`) | `frontend.common.jvm/.../jvm/environment/JvmClasspath.kt` | *replaces* `AbstractProjectFileSearchScope`: a part of the classpath named by its roots, not an opaque file set with set algebra. No complement, no ambient universe, one shape per real use. Sealed since 2026-08-12: the `PsiScopeJvmClasspath` escape hatch is gone, so a scope cannot re-enter the description |
| `CliBinaryClassFileIndex` + `CliVirtualFileFinderFactory.binaryClassFileIndex()` | `compiler/cli/.../CliBinaryClassFileIndex.kt` | the `ct.sym` `.sig` extension choice. It no longer builds the session's scope: that was `classFile.virtualFile in psiSearchScope(classpath)`, a per-file IntelliJ query, and is now root membership on the handle |
| `BinaryJavaClassCache` | `frontend.common.jvm/.../classFiles/BinaryJavaClassCache.kt`, held by the compilation's java-direct `FirJavaInterop` | the class-file lookups and loaded binary classes of one compilation, shared by every session; the injection point for a longer-lived cache (`BINARY_CLASS_CACHE_LIFETIME.md`, `CLASS_FILE_READ_LAYER.md`) |
| `FirJavaInterop` | `fir/fir-jvm/.../session/FirJavaInterop.kt`, held by `FirJvmSessionFactory.Context` (required, no default) | which Java implementation serves the compilation, as one per-compilation decision instead of a `createJavaFacade` lambda per construction site; `createJavaDirectJavaInterop` (java-direct) and `VfsBasedProjectEnvironment.psiJavaInterop()` (`compiler/cli/`) are peers. Split by *role*: `createBinaryJavaFacade(classpath)` and `createJavaSourcesFacade()` |
| `javaModuleFinder: JavaModuleFinder` parameter | `JavaDirectJavaInterop.kt` | replaces a `CoreJavaFileManager` service lookup; `import module M;` no longer silently degrades |
| `javaSourceRoots: List<JavaSourceRootEntry>` parameter | `JavaDirectJavaInterop.kt` | replaces reading `CLIConfigurationKeys.CONTENT_ROOTS` inside the module |
| `VfsBasedProjectEnvironment.javaInterop(configuration, withJavaSources)` | `compiler/cli/cli-jvm/.../cli/jvm/compiler/JavaInterop.kt` | the one place which knows how each peer is built, so a JVM-hosted pipeline follows `-Xjava-direct` instead of deciding on its own. `:compiler:cli-jvm` is the lowest module seeing both `:compiler:cli` and `:compiler:java-direct` |
| `readBinaryJavaClass(topLevelClassFile: BinaryClassFileHandle, …)` | `frontend.common.jvm/.../BinaryJavaClassReader.kt` | lets the reader be driven from a handle instead of a `VirtualFile` |
| `JavaModuleInfo.read(file, classesByClassId)` | `frontend.common.jvm/.../JavaModuleInfo.kt` | takes a `ClassIdToJavaClass` resolver instead of a `KotlinCliJavaFileManager` + scope |

All CLI and VFS construction happens in `JvmFrontendPipelinePhase` and in the `javaInterop` helper
next to it; java-direct receives only abstract inputs.

## 4. Still platform-bound

`BinaryClassFileHandle.virtualFile` is the one remaining transitional accessor, and since 2026-08-12
it is *private* to `BinaryJavaClassReader.kt`: restricting a lookup to a part of the classpath was its
last other reader. It cannot be removed while `BinaryJavaClass` is `VirtualFile`-bound: the class
implements `VirtualFileBoundJavaClass` and resolves its nested classes through
`virtualFile.parent.findChild(...)`. Nothing in java-direct reads the accessor.

The platform-free axis needs, in rough dependency order:

- `BinaryJavaClass` / `VirtualFileBoundJavaClass` — nested-class lookup over a directory handle;
- `VirtualFileKotlinClass`, `CliVirtualFileFinder`, `VirtualFileFinderFactory`,
  `KotlinBinaryClassCache` — the Kotlin-side binary reader;
- `JvmDependenciesIndexImpl`, `JvmDependenciesDynamicCompoundIndex`, `JavaRoot` — the classpath
  index behind `CliBinaryClassFileIndex`;
- `VfsBasedProjectEnvironment.psiSearchScope(JvmClasspath)` (`compiler/cli/`) — the single
  classpath → `GlobalSearchScope` adapter, and now the *only* direction of travel: since
  `JvmClasspath` is sealed and `PsiScopeJvmClasspath` is deleted, there is no way back from a scope
  to a classpath. It serves the Kotlin class finder and the package-part provider only; the binary
  Java lookup left it in 2026-08-12, when the classpath restriction became root membership
  (`JvmClasspath.contains(BinaryClassFileHandle)`, private to java-direct). The remaining
  implementation of `isUnder` is the
  `VirtualFile`-path form of what `ClassPathScope.contains` did — an archive entry belongs to the
  archive, a loose class file to any enclosing directory — and a future NIO index answers it
  directly, which is what makes `BinaryClassFileHandle.virtualFile` removable.
  `ClasspathRestrictionTest` (`compiler/java-direct/test/`) pins both cases, because the
  compilations that pass a non-empty root list (the incremental output directories, an HMPP
  fragment's classpath) have no java-direct suite coverage.

That switch has LL-API, incremental-compilation and scripting consumers, so it is a separate
effort; `implDocs/archive/EXTERNAL_DEPENDENCIES_RESOLUTION_ANALYSIS.md` §5.2/§6.2 is the only
written design for it.

## 5. Kotlin-side notes

- `FirJavaFacadeForModule` (was `FirJavaFacadeForSource`) is used by both source and
  library sessions. Whether a session sees Java sources or class files is decided by its
  `JavaClassFinder`, not by the facade.
- `JvmClassFileBasedSymbolProvider.javaFacade` is the *binary* Java view of its session; on the
  java-direct path it is backed by `JavaClassFinderOverBinaryIndex`. Do not reintroduce a
  `JvmBinaryClassFinderInputs`-style seam for this — it was tried and deleted.
- `FirJavaElementFinder` is **unreachable** under java-direct. Evidence: with `findClass`,
  `findClasses`, `getClasses` and `findPackage` all throwing, the full `JavaUsingAst*` suites passed
  2798/2798. Registering it is therefore `FirJavaInterop.registerKotlinDeclarationsForJava`, which
  the PSI implementation performs and the java-direct one leaves a no-op — there is no separate
  `needRegisterJavaElementFinder` flag to keep in sync. kapt is unaffected — it runs as a
  `FirAnalysisHandlerExtension` and returns before `prepareJvmSessions`; `-Xuse-javac` was removed
  in 2.4.0.

## 6. Out of scope

- Source elements and the Kotlin light-tree PSI residue (`KtSourceElement`, `IElementType`, …).
- `ClasspathRootsResolver`, `SingleJavaFileRootsIndex` (KT-88100 merge TODO at
  `JavaSourceIndex.kt`), `JvmPackagePartProvider`.
- Deleting `KotlinCliJavaFileManagerImpl` — still required by K1, kapt, `CoreJavaDirectoryService`
  and the `usePsiClassFilesReading` mode. The goal reached here is that the FIR JVM java-direct path
  no longer needs it, which is the precondition for a later deletion.

## 7. Who decides which Java implementation a session uses

The choice lives in `FirJvmSessionFactory.Context.javaInterop`, so everything constructed from
that context — the library and source sessions, the symbol provider for the precompiled binaries of
incremental compilation (`FirJvmIncrementalCompilationSymbolProviders`), the JVM interpretation of an
HMPP common fragment's classpath, the scripting/REPL additional-libraries session — follows the
compilation's decision.

`FirJavaInterop` covers **both directions** of the Kotlin/Java bridge: the two `create*JavaFacade`
methods give FIR the Java declarations of the compilation, `registerKotlinDeclarationsForJava`
exposes a source session's Kotlin declarations to Java resolution (the `FirJavaElementFinder` PSI
stubs). They are one object because they are one decision, and because as two knobs they had already
drifted apart.

The facade side is split **by role**, not by scope: `createBinaryJavaFacade(session, moduleData,
classpath)` and `createJavaSourcesFacade(session, moduleData)`. Which files are "the `.java` sources
of this compilation" is the implementation's own knowledge — java-direct has its `javaSourceRoots`,
the PSI peer uses `AllJavaSourcesInProjectScope` (or nothing, in scripting and the REPL). This is
what removed java-direct's `fileSearchScope === javaSourcesScope` identity check and the
`IdentityHashMap` that made it work.

The choice itself is *derived from the compiler configuration*, in one shared helper:
`VfsBasedProjectEnvironment.javaInterop(configuration, withJavaSources)`
(`compiler/cli/cli-jvm/.../cli/jvm/compiler/JavaInterop.kt`). Every JVM-hosted pipeline calls it —
the JVM CLI pipeline, `prepareJKlibSessions`, the scripting compiler, the REPL and the script
additional-sources extension — so `-Xjava-direct` is effective everywhere rather than in one
pipeline. `withJavaSources` is a single switch on purpose: java-direct describes its `.java` sources
as source roots and the PSI peer as a search scope, and a caller must not be able to state the two
inconsistently. "All or none" is also the whole observable domain: the multi-module test
infrastructure used to hand the PSI peer a per-module scope, but its Kotlin half was filtered out
again by `FilterOutKotlinSourceFilesScope` and its Java half was `AllJavaSourcesInProjectScope`, so
the fixtures now call the same helper as every pipeline (§8, item 1).

One consumer states that it makes **no** choice: `FirSessionConstructionUtils.prepareMetadataSessions`
(metadata compilation) passes `NoJavaInterop` (`fir/fir-jvm/.../session/FirJavaInterop.kt`). It builds
a `FirJvmSessionFactory.Context` only to register the JVM session components —
`FirMetadataSessionFactory` never calls `FirJvmSessionFactory.createLibrarySession`/`createSourceSession`,
so no facade is ever created, and `-Xjava-direct` cannot reach that configuration anyway
(`USE_JAVA_DIRECT` is written only from the JVM arguments). Naming one of the two peers there would have
implied that metadata compilation resolves Java through it; `NoJavaInterop` fails loudly instead, at the
place where the decision was skipped. (It also could not call the helper: `:compiler:cli` is *below*
`:compiler:cli-jvm`.)

There is **no default**, and no way to obtain a Java view without stating a choice:

- `JvmCompilationEnvironment` has neither `getFirJavaFacade` nor `registerAsJavaElementFinder`. An
  environment gives the JVM views of a `JvmClasspath`; which Java implementation reads it, and
  whether Kotlin is exposed back to it, is not a property of it.
- The two implementations are peers of each other, both constructed by the caller:
  `VfsBasedProjectEnvironment.psiJavaInterop()` (`compiler/cli/.../VfsBasedProjectEnvironment.kt`)
  and `createJavaDirectJavaInterop` (`compiler/java-direct/.../JavaDirectJavaInterop.kt`).
- `Context.javaInterop` is a required constructor parameter, so a new consumer cannot inherit
  the PSI path by omission — which is exactly how the incremental-compilation consumer had drifted.

`FirJKlibSessionFactory` (`compiler/cli/cli-jklib/`) is a sibling of `FirJvmSessionFactory` with its
own `Context`; `createLibrarySession`/`createSourceSession` take a `FirJavaInterop` instead of
reaching for the environment, and `prepareJKlibSessions` now derives it from the configuration like
every other pipeline. `FirJavaElementFinder` registration follows from the same object, so the
former unconditional `needRegisterJavaElementFinder = true` cannot come back.

The LL-API/IDE and K1 sides are out of scope entirely (see above).

The shared (non-CLI) test infrastructure follows the same helper through `FirFrontendFacade`, so it
*can* run java-direct, but no suite enables it and two defects block doing so — measured, with the
per-suite results, in `TEST_INFRA_JAVA_DIRECT.md`. Only JKlib, incremental compilation, the HMPP
common fragment and scripting were verified to work under a forced-on java-direct.

## 8. The PSI that is left in the API, and how it goes

With `AbstractProjectFileSearchScope` deleted, `PsiScopeJvmClasspath` gone and the Java-sources
parameter reduced to a boolean, `GlobalSearchScope` no longer appears in any cross-module signature
of the Java view — only in CLI-internal adapters:

1. **`psiJavaInterop(javaSources: (FirModuleData) -> GlobalSearchScope)`** — *landed: it is now
   `psiJavaInterop(withJavaSources: Boolean = true)`.* It was the last `GlobalSearchScope` in a
   signature called from outside `:compiler:cli`, and the mirror of the problem `JvmClasspath`
   solved for the binary side: the *Java sources* side never got its own description.

   *Plan (obsolete, kept for the reasoning).* Give it one, in the same shape and in
   `frontend.common.jvm` next to `JvmClasspath`:
   `sealed interface JavaSources { object None; object OfThisCompilation; class Roots(List<JavaSourceRootEntry>) }`.
   java-direct already speaks the third form (`javaSourceRoots`), the PSI peer converts it exactly
   as `psiSearchScope` converts a `JvmClasspath` (`EMPTY_SCOPE` / `AllJavaSourcesInProjectScope` /
   a files scope). Then `withJavaSources: Boolean` in the helper becomes `javaSources: JavaSources`,
   the parameter stops being PSI-shaped, and the remaining fixture cases ("this test module's own
   `.java` files", which is a `filesScope` over `KtSourceFile`s) need one more shape —
   `Files(List<File>)` — or stay behind a cli-internal `psiJavaInterop` overload marked as test-only.

   *What happened instead.* Measured against its call sites, the lambda had no observable range
   beyond "all or none", so the parameter became `psiJavaInterop(withJavaSources: Boolean = true)`
   and no `JavaSources` type was needed. All four non-CLI callers passed the same expression —
   `filesScope(<the module's Kotlin files>).uniteWith(AllJavaSourcesInProjectScope)`, the K1-era
   `TopDownAnalyzerFacadeForJVM.newModuleSearchScope` — whose Kotlin half is removed again by
   `FilterOutKotlinSourceFilesScope` in `JavaClassFinderImpl` and cannot reach a `PsiElementFinder`
   either (`FirJavaElementFinder.findClass` ignores its scope), while the Java half is exactly the
   default. So `FirFrontendFacade`/`FirReplFrontendFacade` no longer keep a
   `FirModuleData -> GlobalSearchScope` map, `FirTestSessionFactoryHelper.createSessionForTests` no
   longer takes a scope, and all of them go through `javaInterop(configuration)` — which also makes
   the fixtures honour `-Xjava-direct` for the first time. Should a genuine per-module answer ever be
   needed, it must be described as *files or roots*, not as a scope; nothing wants it today.

2. **`VfsBasedProjectEnvironment.psiSearchScope(JvmClasspath)`** — CLI-internal, and the intended
   end state until the platform-free axis (§4) replaces the index itself. It is not API and needs no
   plan of its own: it disappears together with `JvmDependenciesIndex`.

3. **`VfsBasedProjectEnvironment(project, fileSystems, getPackagePartProviderFn: (GlobalSearchScope) -> PackagePartProvider)`**
   — a constructor parameter, so PSI-shaped state on a PSI-based implementation. It is honest where
   it is; it becomes removable when `JvmPackagePartProvider` stops being scope-driven (§6 lists it
   as out of scope).

Removed on the way here: `PsiScopeJvmClasspath` / `GlobalSearchScope.asJvmClasspath()` had two
users, both of which turned out to be expressible as a classpath rather than needing an escape
hatch — `JKlibIrCompilationPhase` asked for a package-part provider over
`notScope(AllJavaSourcesInProjectScope)`, which only excludes `.java` files from a lookup that
reads `.class` files, and `FirTestSessionFactoryHelper` was handed
`ProjectScope.getLibrariesScope(project)`, which is literally what `JvmClasspath.ProjectLibraries()`
produces.
