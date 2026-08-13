# Java-Direct: Iteration Results Log

**Current status**: `:compiler:java-direct:test` full suite green, 2839/2839 (100%). No known won't-fix.

**Last archived**: `implDocs/archive/ITERATION_RESULTS_2026_07_13.md` (entries through 2026-07-13).

---

## How to write entries

This log is read into the agent's context every session, so **entries must stay short**.

- **Newest entry on top.** One entry per landed change or per investigated regression.
- **Cap each entry at ~15 lines / ~150 words.** If the rationale, a trace, or a
  measurement table is longer, put it in a dedicated `implDocs/<TOPIC>.md` and link to it
  from the entry — do not inline it here.
- **Use the fixed fields below.** No free-form multi-paragraph narration; if a field needs
  more than ~2 lines, link out instead.
- **No pasted logs, stacktraces, or diffs.** Quote the single line that matters; link the rest.
- **Archive when this file passes ~600 lines** (see `AGENT_INSTRUCTIONS.md` →
  "Docs Maintenance"): `git mv` it to
  `implDocs/archive/ITERATION_RESULTS_<last-entry-date>.md`, add an archive banner, and
  reset this file to the template below.

### Entry template

```
### YYYY-MM-DD — <one-line title>
- **Change**: what changed and why (1–3 lines).
- **Files**: key files touched (+N/−M LoC if useful).
- **Tests**: suites run + counts (e.g. box 1178/1178, phased 1513/1513).
- **Result**: green / regression fixed / won't-fix — link to a detail doc if there is one.
```

---

<!-- Add new entries below, newest first. -->

### 2026-08-13 — `BinaryClassFileScope` removed: the finder takes the session's classpath
- **Change**: the seam had one production use site — `JavaDirectJavaInterop` built
  `classpath.asBinaryClassFileScope()` only to hand it to `JavaClassFinderOverBinaryIndex` — so it was a
  type between two parties that both know the classpath. The finder now takes the `JvmClasspath` itself
  and answers the restriction with `internal operator fun JvmClasspath.contains(BinaryClassFileHandle)`
  next to it; `BinaryClassFileScope` and `JvmClasspath.asBinaryClassFileScope()` are deleted, so the
  shared seam keeps only `BinaryClassFileIndex`, `BinaryClassFileHandle` and its `isUnder(Path)` check.
  `applyScopeFilter` → `restrictToClasspath`, `findClassWithoutScopeFilter` →
  `findClassAnywhereOnClasspath`; no behaviour change.
- **Files**: `frontend.common.jvm/.../classFiles/BinaryClassFileIndex.kt`, `.../BinaryJavaClassCache.kt`,
  `java-direct/.../JavaClassFinderOverBinaryIndex.kt`, `.../JavaDirectJavaInterop.kt`,
  `java-direct/test/.../ClasspathRestrictionTest.kt` (was `BinaryClassFileScopeTest.kt`),
  `implDocs/PSI_FREE_ROADMAP.md` §3/§4, `implDocs/BINARY_CLASS_CACHE_LIFETIME.md` §1.
- **Tests**: java-direct 21774/0; PSI gate `PhasedJvmDiagnosticLightTreeTestGenerated` 10993/0
  (mandatory: the PSI file manager shares `readBinaryJavaClass`); `IncrementalK2FirICJvmCompilerRunnerTest`
  371/0 — the one path with a non-empty exclusion list.
- **Result**: green; one fewer type on the seam, and the scope policy sits where the classpath is used.

### 2026-08-12 — the binary lookup is restricted by classpath *root*, not by a PSI scope
- **Change**: `binaryClassFileScope(classpath)` built `classFile.virtualFile in psiSearchScope(classpath)`
  — a per-candidate IntelliJ query, written when a scope was an opaque file set. `JvmClasspath` is
  root-shaped and sealed now, so the restriction is `JvmClasspath.asBinaryClassFileScope()` over a new
  `BinaryClassFileHandle.isUnder(Path)`: `Roots` tests its roots, `ProjectLibraries` only its
  exclusions (an index *is* the compilation's classpath). `isUnder` reproduces `ClassPathScope.contains`
  — an archive entry belongs to the archive itself, a loose class file to any enclosing directory. The
  CLI helper and `createJavaDirectJavaInterop`'s scope-factory parameter are gone, and
  `BinaryClassFileHandle.virtualFile` is now private to `BinaryJavaClassReader.kt` (only `BinaryJavaClass`
  reads it) — one step towards removing it (`PSI_FREE_ROADMAP.md` §4).
- **Files**: `frontend.common.jvm/.../classFiles/BinaryClassFileIndex.kt`, `.../BinaryJavaClassReader.kt`,
  `cli/.../CliBinaryClassFileIndex.kt`, `cli-jvm/.../JavaInterop.kt`, `java-direct/.../JavaDirectJavaInterop.kt`,
  new `java-direct/test/.../BinaryClassFileScopeTest.kt`, `implDocs/PSI_FREE_ROADMAP.md` §3/§4,
  `implDocs/BINARY_CLASS_CACHE_LIFETIME.md` §1, `implDocs/CLASS_FILE_READ_LAYER.md` §4.
- **Tests**: java-direct 21771/0; `BinaryClassFileScopeTest` 3/0 (needed: java-direct sessions pass
  `ProjectLibraries()` with no exclusions, so no suite exercises a non-empty root list — that is the
  incremental output directories and an HMPP fragment's classpath); `IncrementalK2FirICJvmCompilerRunnerTest`
  371/0; PSI gate `PhasedJvmDiagnosticLightTreeTestGenerated` 10993/0 (mandatory: the PSI file manager
  shares `readBinaryJavaClass`).
- **Result**: green; removes a PSI query from the hot binary lookup path.

### 2026-08-12 — the Java-sources scope leaves the API; test fixtures follow the flag
- **Change**: `psiJavaInterop` took a `(FirModuleData) -> GlobalSearchScope`, but all four non-CLI
  callers passed the same K1-era expression (`filesScope(<module's Kotlin files>)` ∪
  `AllJavaSourcesInProjectScope`), whose Kotlin half is filtered out again by
  `FilterOutKotlinSourceFilesScope` in `JavaClassFinderImpl` and whose Java half *is* the default —
  so the parameter is now `withJavaSources: Boolean = true` and `GlobalSearchScope` no longer appears
  in any cross-module signature of the Java view. `FirFrontendFacade`/`FirReplFrontendFacade` lost
  their `FirModuleData -> GlobalSearchScope` maps, `newModuleSearchScope` and (in the first) two
  parameters of `createModuleBasedSession`; `FirTestSessionFactoryHelper.createSessionForTests` lost
  the scope parameter of both overloads and all callers use `javaInterop(configuration)`, i.e. the
  fixtures honour `-Xjava-direct` for the first time.
- **Files**: `cli/.../VfsBasedProjectEnvironment.kt`, `cli-jvm/.../JavaInterop.kt`,
  `tests-common-new/.../FirFrontendFacade.kt`, `.../FirReplFrontendFacade.kt`,
  `tests-compiler-utils/.../FirTestSessionFactoryHelper.kt`, `.../session/FirSessionFactoryHelper.kt`
  (stale import), `.../codegen/GenerationUtils.kt`, `legacy-fir-tests/.../AbstractFirTypeEnhancementTest.kt`,
  `modularized-tests/.../FirResolveModularizedTotalKotlinTestPure.kt`,
  `benchmarks/.../AbstractSimpleFileBenchmark.kt`, `implDocs/PSI_FREE_ROADMAP.md` §7/§8.
- **Tests**: java-direct 21771/0; gates `PhasedJvmDiagnosticLightTreeTestGenerated` 10993/0 and
  `*CompileKotlinAgainstKotlin*` 153/0; `FirTypeEnhancementTestGenerated` 289/0 (its `.java` files are
  in-memory `LightVirtualFile`s, covered by `AllJavaSourcesInProjectScope`), `*ForeignAnnotations*`
  982/0, scripting/REPL 402/0, kapt 351/0 (`GenerationUtils`); `:benchmarks:compileTestKotlin` green.
- **Result**: green, behaviour-preserving off `-Xjava-direct`.

### 2026-08-12 — metadata compilation states that it reads no Java; Compose follows the flag
- **Change**: `prepareMetadataSessions` passed `psiJavaInterop()`, implying metadata compilation
  resolves Java through PSI. It creates a `FirJvmSessionFactory.Context` only to register the JVM
  session components and never calls `create*Session` on that factory, so no facade is ever built —
  it now passes the new `NoJavaInterop`, which fails loudly if that ever changes. The Compose test
  facade switched to the shared `javaInterop(configuration)` helper (its module already depends on
  `:compiler:cli-jvm`), so it is no longer the last self-deciding consumer.
- **Files**: `fir/fir-jvm/.../session/FirJavaInterop.kt` (+21), `cli/.../FirSessionConstructionUtils.kt`,
  `plugins/compose/.../facade/K2CompilerFacade.kt` (+2 unused imports removed),
  `implDocs/PSI_FREE_ROADMAP.md` §7.
- **Tests**: `MetadataDiagnosticTestGenerated` 11/0, `JvmLightTreeBlackBoxCodegenWithSeparateKmpCompilationTestGenerated`
  269/0 (both run `prepareMetadataSessions`); `fir-jvm`/`cli`/`cli-metadata`/`cli-jvm` compile.
- **Result**: green, no behaviour change. Compose integration-tests still cannot be compiled offline.

### 2026-08-12 — `-Xjava-direct` is honoured by every JVM pipeline; the PSI-scope escape hatch is gone
- **Change**: new `VfsBasedProjectEnvironment.javaInterop(configuration, withJavaSources)` in
  `:compiler:cli-jvm` (the lowest module seeing both `:compiler:cli` and `:compiler:java-direct`)
  derives the Java view from `useJavaDirect`. `JvmFrontendPipelinePhase`, `prepareJKlibSessions`,
  `K2ScriptingCompilerEnvironment`, `K2ReplCompiler` and `CollectAdditionalScriptSourcesExtension`
  all call it, so JKlib and scripting stop hardcoding PSI. `withJavaSources` is one switch because
  the peers describe `.java` sources in different currencies. Then `PsiScopeJvmClasspath` /
  `GlobalSearchScope.asJvmClasspath()` were **deleted** and `JvmClasspath` sealed: its two users
  were expressible as classpaths — `JKlibIrCompilationPhase` used
  `notScope(AllJavaSourcesInProjectScope)` for a package-part provider (`.java` files are never
  package parts) and `FirTestSessionFactoryHelper` was passed exactly
  `ProjectScope.getLibrariesScope(project)`.
- **Files**: new `cli/cli-jvm/.../cli/jvm/compiler/JavaInterop.kt`; `JvmFrontendPipelinePhase.kt`
  (−3 private helpers), `FirJKlibSessionFactory.kt`, `JKlibIrCompilationPhase.kt`,
  `VfsBasedProjectEnvironment.kt`, `JvmClasspath.kt`, 3 scripting sites, 2 fixtures;
  `PSI_FREE_ROADMAP.md` §3/§4/§7 + new §8 (remaining PSI in the API and how it goes).
- **Tests**: java-direct 21771/0; JKlib 843/0; scripting-tests; both gates
  (`PhasedJvmDiagnosticLightTree`, `*CompileKotlinAgainstKotlin*`);
  `IncrementalK2FirICJvmCompilerRunnerTest` — all green.
- **Result**: green. Remaining PSI in a cross-module signature: only
  `psiJavaInterop(javaSources: (FirModuleData) -> GlobalSearchScope)` — plan in `PSI_FREE_ROADMAP.md` §8.

### 2026-08-12 — PSI search scopes leave the API: `JvmClasspath` and a Java view split by role
- **Change**: `AbstractProjectFileSearchScope` is **deleted**. It was never an abstraction over PSI —
  one implementation (`PsiBasedProjectFileSearchScope`), a downcast (`asPsiSearchScope()`) at every
  point of use, and IDEA semantics (`not()` only means something against an ambient "all files in the
  project", which a compilation does not have; `ANY.minus` already threw). It is replaced by
  `JvmClasspath`: `Roots(List<Path>)` or `ProjectLibraries(excludedRoots)`, i.e. what
  `DependencyListForCliModule` already speaks. `JvmCompilationEnvironment` is down to
  `getKotlinClassFinder` / `getPackagePartProvider` / `getJavaModuleResolver`; the six scope producers
  are gone, two of which (`getSearchScopeByIoFiles`, `getSearchScopeBySourceFiles`) had **zero callers
  repo-wide**, and `allowOutOfProjectRoots` went with them, never having been `true` in any
  compilation. `FirJavaInterop` is split by role — `createBinaryJavaFacade(classpath)` /
  `createJavaSourcesFacade()` — which deletes java-direct's `scope === javaSourcesScope` identity
  check and its `IdentityHashMap`.
- **Why**: the goal was to get PSI scopes out of the API; everything else fell out of it. The
  incremental-compilation "hack" the OSIP-191 comment predicted would go away has gone away: the
  libraries scope is now `ProjectLibraries(excludedRoots = outputDir)`, and the second subtraction
  (`- sourceScope`, the explicit PSI file set ∪ "any `.java` file") is simply gone — a classpath
  consumed by a `.class` reader has no source files in it by construction. `getSearchScopeByPsiFiles`
  and its one caller disappeared with it, so the Kotlin PSI file set no longer crosses any module
  boundary. The only remaining classpath → `GlobalSearchScope` conversion is
  `VfsBasedProjectEnvironment.psiSearchScope`, private to `:compiler:cli` apart from the documented
  `PsiScopeJvmClasspath` escape hatch (legacy JKlib IR phase, two test fixtures) — removed by the
  2026-08-12 entry above.
- **Files**: `JvmClasspath.kt` (new), `JvmCompilationEnvironment.kt`, `FirJavaInterop.kt`,
  `VfsBasedProjectEnvironment.kt`, `CliBinaryClassFileIndex.kt`, `IncrementalCompilationContext*.kt`,
  `FirJvmSessionFactory.kt`, `FirJvmIncrementalCompilationSymbolProviders.kt`,
  `FirMetadataSessionFactory.kt`, `FirJKlibSessionFactory.kt`, `JvmFrontendPipelinePhase.kt`,
  `JavaDirectJavaInterop.kt`, scripting/REPL (5 files), test fixtures (7 files);
  `frontend.common/.../search/AbstractProjectFileSearchScope.kt` deleted.
- **Tests**: java-direct 21771/0; PSI gate `PhasedJvmDiagnosticLightTree` 10993/0;
  `*CompileKotlinAgainstKotlin*` 153/0; `IncrementalK2FirICJvmCompilerRunnerTest` 371/0;
  jklib.tests 843/0; scripting-tests 402/0.
- **Result**: green.

### 2026-08-12 — both directions of the Java bridge in one object; the environment leaves FIR
- **Change**: `FirJavaFacadeFactory` → `FirJavaInterop`, which now also owns
  `registerKotlinDeclarationsForJava` (was `AbstractProjectEnvironment.registerAsJavaElementFinder`).
  The PSI implementation registers `FirJavaElementFinder`, java-direct leaves the default no-op, and the
  parallel `needRegisterJavaElementFinder` flag is deleted from both session factories and all 10 call
  sites — it was a second copy of the same PSI/not-PSI decision and had already drifted (JKlib passed
  `true` unconditionally).
- **Why**: that method was the last FIR reference in the environment and the one member unimplementable
  without PSI. With it gone, `AbstractProjectEnvironment` moved to `:compiler:frontend.common.jvm` as
  `JvmCompilationEnvironment` (package `org.jetbrains.kotlin.jvm.environment`), next to the
  `KotlinClassFinder`/`PackagePartProvider`/`JavaModuleResolver` it hands out; a PSI-free environment is
  now expressible, and `org.jetbrains.kotlin.fir.session` is no longer a split package.
- **Files**: `FirJavaInterop.kt` (fir-jvm, renamed), `JvmCompilationEnvironment.kt` (new location),
  `VfsBasedProjectEnvironment.kt`, `FirJvmSessionFactory.kt`, `FirJKlibSessionFactory.kt`,
  `JavaDirectJavaInterop.kt` (renamed), `JvmFrontendPipelinePhase.kt`, scripting + test fixtures.
- **Tests**: see below.
- **Result**: green.

### 2026-08-12 — the Java implementation is stated, never defaulted
- **Change**: `JvmCompilationEnvironment.getFirJavaFacade` is gone. It made the PSI Java view a property
  of the environment — obtainable by anyone holding one, and therefore the invisible default that let the
  incremental-compilation consumer drift onto PSI under `-Xjava-direct`. Its body moved to
  `:compiler:cli` as `VfsBasedProjectEnvironment.psiJavaInterop()`, a free-function peer of
  java-direct's `createJavaDirectJavaInterop`: two implementations, neither privileged.
  `FirJvmSessionFactory.Context.javaInterop` is now a required constructor parameter, so a
  consumer which does not choose no longer silently gets PSI. That surfaced `FirJKlibSessionFactory` as
  a compile error, as intended: its `createLibrarySession`/`createSourceSession` take a
  `FirJavaInterop` instead of reaching for the environment, and `prepareJKlibSessions` supplies
  the PSI one — same behaviour as before, but now explicit and one parameter away from java-direct.
- **Files**: `fir-jvm/.../session/environment/JvmCompilationEnvironment.kt`,
  `fir-jvm/.../session/FirJavaInterop.kt`, `cli/.../VfsBasedProjectEnvironment.kt`,
  `fir/entrypoint/.../FirJvmSessionFactory.kt`, `cli-jklib/.../FirJKlibSessionFactory.kt`, and the ten
  `Context(...)` construction sites (`JvmFrontendPipelinePhase`, `FirSessionConstructionUtils`,
  `FirFrontendFacade`, `FirReplFrontendFacade`, `FirSessionFactoryHelper`, `K2ReplCompiler`,
  `K2ScriptingCompilerEnvironment` ×2, `CollectAdditionalScriptSourcesExtension`, Compose
  `K2CompilerFacade`); `implDocs/PSI_FREE_ROADMAP.md` §3/§7.
- **Tests**: `compileKotlin` of `fir:fir-jvm`, `fir:entrypoint`, `cli`, `cli-jvm`, `cli-jklib`,
  `java-direct`, `kotlin-scripting-compiler` and the fixture modules; `:compiler:java-direct:test`;
  `PhasedJvmDiagnosticLightTreeTestGenerated`; `*CompileKotlinAgainstKotlin*`;
  `IncrementalK2FirICJvmCompilerRunnerTestGenerated`; `:compiler:jklib.tests:test`.
- **Result**: green; no behaviour change. The Compose integration-tests module could not be compiled
  offline (its `protobuf-test-classes` needs `com.google.protobuf:protoc`, unrelated); the edit there is
  one added argument plus an import.

### 2026-08-12 — the seam abstractions moved below the PSI default; `java-direct → fir:entrypoint` dropped
- **Change**: java-direct depended on `:compiler:fir:entrypoint` only for two symbols — the abstraction
  it implements (`FirJavaInterop`) and the scope type in its signatures
  (`AbstractProjectFileSearchScope`) — both of which sat next to the PSI default that also implements
  them. Two destinations, because the two types are on different layers:
  `AbstractProjectFileSearchScope` imports nothing and has non-JVM users (metadata and JKlib
  pipelines), so it went to `:compiler:frontend.common` under the neutral package
  `org.jetbrains.kotlin.search`; `JvmCompilationEnvironment` is JVM-specific in five of its ten
  members (`KotlinClassFinder`, `PackagePartProvider`, `JavaModuleResolver`,
  `registerAsJavaElementFinder`, `getFirJavaFacade`) and has a single JVM implementation, so it went
  to `:compiler:fir:fir-jvm` together with `FirJavaInterop`, whose `FirJavaFacade` already lives
  there. The `fir.session[.environment]` packages are unchanged, so only the ~20 scope imports moved.
- **Files**: `frontend.common/.../search/AbstractProjectFileSearchScope.kt` (new),
  `fir-jvm/.../session/environment/JvmCompilationEnvironment.kt` and
  `fir-jvm/.../session/FirJavaInterop.kt` (moved out of `fir:entrypoint`),
  `fir-jvm/build.gradle.kts` (`frontend.common`, `frontend.common.jvm`, `core:compiler.common.jvm`
  become `api` — they are in the moved public signatures), `java-direct/build.gradle.kts`
  (`fir:entrypoint` out, `frontend.common` in), plus the import in 17 CLI/JKlib/scripting/test-fixture
  files and `implDocs/PSI_FREE_ROADMAP.md` §2/§3/§4.
- **Tests**: `compileKotlin` of `frontend.common`, `fir:fir-jvm`, `fir:entrypoint`, `java-direct`,
  `cli`, `cli-jvm`, `cli-jklib`, `kotlin-scripting-compiler` and the test-fixture modules;
  `:compiler:java-direct:test` (box + phased, `JavaDirectModuleBoundaryTest`, `JavaParsingTest`);
  both gates for the shared files touched (`PhasedJvmDiagnosticLightTreeTestGenerated`,
  `CompileKotlinAgainstKotlin`).
- **Result**: green; move only, no behaviour change.

### 2026-08-12 — `Context.binaryJavaClassCache` removed as a leftover
- **Change**: after the `FirJavaInterop` round the cache was written into
  `FirJvmSessionFactory.Context` but read by nobody: the only consumer is the java-direct
  `JavaClassFinderOverBinaryIndex`, which gets it from `createJavaDirectJavaInterop`, and the
  factory is itself the context's per-compilation Java decision. The field (and its `null`-means-PSI
  double of `javaInterop == null`) is gone; the cache is constructed inside the java-direct
  branch in `prepareJvmSessions`, so its lifetime is still the compilation, now stated where it is held.
  Nothing else was left dangling: no unused parameters or imports remain from the round, and
  `FirJKlibSessionFactory` is the only self-deciding consumer, already recorded in
  `implDocs/PSI_FREE_ROADMAP.md` §7.
- **Files**: `FirJvmSessionFactory.kt`, `JvmFrontendPipelinePhase.kt`, `BinaryJavaClassCache.kt` (KDoc),
  `implDocs/BINARY_CLASS_CACHE_LIFETIME.md`, `implDocs/CLASS_FILE_READ_LAYER.md`,
  `implDocs/PSI_FREE_ROADMAP.md`.
- **Tests**: `:compiler:java-direct:test` full suite; `PhasedJvmDiagnosticLightTreeTestGenerated` and
  `CompileKotlinAgainstKotlin` gates for the shared `JvmFrontendPipelinePhase.kt`.
- **Result**: green; no behaviour change.

### 2026-08-11 — the Java implementation is chosen once per compilation (`FirJavaInterop`)
- **Change**: the choice between the PSI Java view and java-direct was a `createJavaFacade` lambda passed
  into each construction site, so sites which did not know about it silently kept the PSI default — most
  notably the symbol provider for the *precompiled binaries* of incremental compilation. It is now a
  `FirJavaInterop` held by `FirJvmSessionFactory.Context` (default `psiJavaInterop()`), read by
  the library/source sessions, `IncrementalCompilationContext.createSymbolProviders` (takes the context
  instead of the project environment), the HMPP-common JVM provider and the scripting/REPL library session.
  `createJavaDirectJavaFacadeBuilder` → `createJavaDirectJavaInterop`. Remaining consumer:
  `FirJKlibSessionFactory`, see `implDocs/PSI_FREE_ROADMAP.md` §7.
- **Files**: `FirJavaInterop.kt` (new), `FirJvmSessionFactory.kt`,
  `FirJvmIncrementalCompilationSymbolProviders.kt`, `JvmFrontendPipelinePhase.kt`,
  `JavaDirectFacadeFactory.kt` (renamed), `sessionUtils.kt`, `K2ReplCompiler.kt`,
  `FirFrontendFacade.kt`, `FirSessionFactoryHelper.kt`.
- **Tests**: `:compiler:java-direct:test` full suite; `PhasedJvmDiagnosticLightTreeTestGenerated` and
  `CompileKotlinAgainstKotlin` gates for the shared `JvmFrontendPipelinePhase.kt`.
- **Result**: green; behaviour changes only under `-Xjava-direct`, where the IC precompiled-binaries
  provider now reads Java through java-direct instead of PSI.

### 2026-08-11 — binary class cache keyed by class file, `BinaryClassFileHandle` identity contract
- **Change**: `implDocs/CLASS_FILE_READ_LAYER.md` §6 first step. `BinaryClassFileHandle` now requires
  `equals`/`hashCode` over the file identity *and* its content version (the VFS implementation snapshots
  `modificationStamp`), and the classes read from class files moved into `BinaryJavaClasses`, keyed by the
  handle plus the `ClassId` inside that file — the handle alone is not a key, since one class file also
  declares every class nested in it. Removes the "which root won for whoever asked first" caveat, for the
  PSI `KotlinCliJavaFileManagerImpl` cache as well (both share `readBinaryJavaClass`), and is the key both
  approach B and any cross-build cache need. §7 of the same doc answers whether this retains more than
  the PSI path: no new kind of data, no new order of magnitude.
- **Files**: `BinaryClassFileIndex.kt`, `BinaryJavaClassReader.kt`, `BinaryJavaClassCache.kt`,
  `KotlinCliJavaFileManagerImpl.kt`, `implDocs/CLASS_FILE_READ_LAYER.md`,
  `implDocs/BINARY_CLASS_CACHE_LIFETIME.md`.
- **Tests**: `:compiler:java-direct:test` full suite; PSI gate (`PhasedJvmDiagnosticLightTreeTestGenerated`)
  and `CompileKotlinAgainstKotlin` — both required here because the PSI binary reader shares the cache
  type — 0 failures.
- **Result**: green; no behaviour change beyond finer cache keys.

### 2026-08-11 — `BinaryJavaClassCache` moved to `FirJvmSessionFactory.Context`
- **Change**: the cache was created inside `createJavaDirectJavaFacadeBuilder`, so "per compilation" was
  an accident of a closure and the object was unreachable from anything but the Java facade. The type
  moved down to `frontend.common.jvm/.../classFiles/` (it has no java-direct-specific content) and is
  now a nullable `Context.binaryJavaClassCache`, constructed in `prepareJvmSessions` next to the index
  and `null` for the PSI facade. Lifetime is now "as long as the context", stated in one place, and the
  BTA-supplied instance of `implDocs/BINARY_CLASS_CACHE_LIFETIME.md` §4 becomes a parameter change only.
  Where a *shared* Kotlin+Java class-file read layer should go: `implDocs/CLASS_FILE_READ_LAYER.md`.
- **Files**: `BinaryJavaClassCache.kt` (moved), `JavaDirectFacadeBuilder.kt`,
  `JavaClassFinderOverBinaryIndex.kt`, `FirJvmSessionFactory.kt`, `fir/entrypoint/build.gradle.kts`,
  `JvmFrontendPipelinePhase.kt`.
- **Tests**: `:compiler:java-direct:test` full suite; both gates for the shared phase file
  (`PhasedJvmDiagnosticLightTreeTestGenerated`, `CompileKotlinAgainstKotlin`) — 0 failures.
- **Result**: green; no behaviour change (same object, one owner earlier in the pipeline).

### 2026-08-11 — binary seam split into `BinaryClassFileIndex` + `BinaryClassFileScope`, caches per compilation
- **Change**: the seam carried both the classpath and the session's visibility, which made
  `JavaClassFinderOverBinaryIndex` read as a finder over a finder. It is now a scope-free
  `BinaryClassFileIndex` (CLI impl `CliBinaryClassFileIndex`) plus a one-method `BinaryClassFileScope`
  supplied per session. That makes the index a pure function of the classpath, so the class-file
  lookups and the loaded classes moved from the per-scope finder into `BinaryJavaClassCache`, created
  once per compilation and shared by every session — the width the PSI `KotlinCliJavaFileManagerImpl`
  already had. Lifetimes beyond one compilation: `implDocs/BINARY_CLASS_CACHE_LIFETIME.md`.
- **Files**: `BinaryClassFileIndex.kt`, `CliBinaryClassFileIndex.kt`, `BinaryJavaClassCache.kt`,
  `JavaClassFinderOverBinaryIndex.kt`, `JavaDirectFacadeBuilder.kt`, `JvmFrontendPipelinePhase.kt`.
- **Tests**: `:compiler:java-direct:test` box + phased, no failures.
- **Result**: green; behaviour-preserving apart from the wider cache.

### 2026-08-11 — binary seam renamed to `BinaryClassFileFinder`, candidate pair moved to the use site
- **Change**: the seam is a per-scope lookup over the classpath index, not a set of roots, so it now
  follows the `KotlinClassFinder` / `VirtualFileFinder` naming: `BinaryClassRoots` →
  `BinaryClassFileFinder`, `JvmDependenciesIndexBinaryRoots` → `JvmDependenciesIndexClassFileFinder`,
  `binaryClassRootsForScope()` → `binaryClassFileFinderForScope()`. The interface now returns the
  candidate class files in classpath order plus `isInSearchScope`; picking the scoped and the
  cross-reference answer, and the cache record holding both, are private to
  `JavaClassFinderOverBinaryIndex`.
- **Files**: `BinaryClassFileFinder.kt`, `JvmDependenciesIndexClassFileFinder.kt`,
  `JavaClassFinderOverBinaryIndex.kt`, `JavaDirectFacadeBuilder.kt`, `JvmFrontendPipelinePhase.kt`.
- **Tests**: `:compiler:java-direct:test` box + phased, no failures.
- **Result**: green; behaviour-preserving rename.

### 2026-08-07 — `compiler/java-direct/src` is PSI-free and off `:compiler:cli`
- **Change**: java-direct now receives only abstract inputs. New `BinaryClassRoots` /
  `TopLevelClassFileCandidates` / `BinaryClassFileHandle` seam in `frontend.common.jvm`, implemented
  by `JvmDependenciesIndexBinaryRoots` in `:compiler:cli`, which is now the only owner of the
  session's `GlobalSearchScope`, the `asPsiSearchScope()` downcast and the `ct.sym` extension choice.
  `JavaModuleFinder` and the Java source roots are passed in instead of fished out of
  `CoreJavaFileManager` / `CLIConfigurationKeys`, which also removes the `?: EMPTY` fallback that
  silently disabled `import module M;`. `readBinaryJavaClass` takes a handle; `JavaModuleInfo.read`
  takes a `ClassIdToJavaClass`. `FirJavaFacadeForSource` → `FirJavaFacadeForModule`.
  `FirJavaElementFinder` proved unreachable under java-direct (all four entry points throwing, full
  suites still green), so registration is gated on `!useJavaDirect`. Details: `implDocs/PSI_FREE_ROADMAP.md`.
- **Files**: `BinaryClassRoots.kt` (new), `JvmDependenciesIndexBinaryRoots.kt` (new),
  `JavaDirectModuleBoundaryTest.kt` (new), `JavaClassFinderOverBinaryIndex.kt`,
  `JavaDirectFacadeBuilder.kt`, `build.gradle.kts`, `JvmFrontendPipelinePhase.kt`,
  `BinaryJavaClassReader.kt`, `JavaModuleInfo.kt`, `CliJavaModuleFinder.kt`,
  `ClasspathRootsResolver.kt`, `KotlinCliJavaFileManagerImpl.kt`, `FirJavaFacade.kt`,
  `JvmClassFileBasedSymbolProvider.kt`, `LLFirJavaSymbolProvider.kt`.
- **Tests**: `JavaUsingAst{Phased,Box}TestGenerated` 2798/2798 + `JavaParsing*` +
  `JavaDirectModuleBoundaryTest` green; PSI gate `PhasedJvmDiagnosticLightTreeTestGenerated` and
  `CompileKotlinAgainstKotlin` gate green before and after.
- **Result**: green. `BinaryClassFileHandle.virtualFile` stays as the one transitional accessor —
  `BinaryJavaClass` is still `VirtualFile`-bound; that is the platform-free (NIO) axis.

### 2026-08-07 — Module import declarations (`import module M;`, JLS 7.5.5 / KT-84499)
- **Change**: two independent gaps. (1) Parser: `FileParser` rolls back to lexeme 0 when a file has
  no package statement, and `rollbackTo` leaves `myTokenTypeChecked = true`, so `tokenType` reported
  the *leading trivia* token — `getImportType` then never saw `module` and the file was parsed as a
  `MODULE` declaration, losing every class. The builder only force-skips trivia there when a token
  remapper is installed, so `parse.kt` now installs an identity one. Affected any file with a
  leading comment/blank line and no package. (2) Resolution: new `moduleImports` bucket in
  `JavaImports`, expanded to the module's unqualified exports plus its `requires transitive`
  closure, and consulted between JLS 7.5.2 and 7.5.4 in `resolveFromModuleImports`.
- **Files**: `parse.kt`, `JavaModuleImportedPackages.kt` (new), `JavaImportResolver.kt`,
  `JavaResolutionContext.kt`, `JavaTypeResolver.kt`, `JavaClassFinderOverAstImpl.kt`,
  `JavaDirectFacadeBuilder.kt`, `KotlinCliJavaFileManagerImpl.kt` (`javaModuleFinder` exposed as a
  nullable read-only property).
- **Tests**: `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated` + `JavaParsing*`
  green (0 failures); PSI gate `PhasedJvmDiagnosticLightTreeTestGenerated` green (11001);
  `CompileKotlinAgainstKotlin` gate green (153).
- **Result**: green; `testJavaModuleImportDeclarations` passes.

### 2026-07-31 — Box guards for the JLS accessibility check, incl. a genuinely *binary* Java supertype
- **Change**: complements the phased guard below with two box tests. Unlike diagnostics tests
  (which hard-code `DependencyKind.Source`), a codegen `// MODULE: lib` dependency is really
  compiled: `JavaCompilerFacade.compileJavaFiles` javac-compiles `lib`'s `.java` into `lib`'s output
  dir, and `JvmEnvironmentConfigurator.registerModuleDependencies` puts that dir on `main`'s
  classpath — so `main` reads `a.Base` as a **binary** Java class through
  `FirBackedJavaClassAdapter`, i.e. the exact shape of the IJ-FP `testIntellij_exceptionAnalyzer`
  regression. So a box test *can* express what a phased test cannot.
- **Files**: `testData/codegen/box/javaDirect/packagePrivateInheritedNestedClassNotVisibleAcrossPackages.kt`
  (source arm, single module) and
  `testData/codegen/boxJvm/javaDirect/packagePrivateInheritedNestedClassFromBinaryModule.kt`
  (adapter arm, `lib` → `main`) — both new; no generator change needed.
- **Tests**: full `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated` green
  (2795 executed, 0 failures, 0 errors); shared-data gate green in
  `Fir{LightTree,Psi}BlackBoxCodegenTestGenerated$Box{,Jvm}$JavaDirect`.
- **Result**: green; non-vacuity verified with two probes — forcing
  `FirBackedJavaClassAdapter.visibility` to `Visibilities.Public` fails *only* the binary-module
  test, while short-circuiting `isInheritedNestedClassAccessible` to `true` fails *both*. Both
  probes reverted (`git diff` on `src` empty). Box data needs no golden file, so rule 5/6 concerns
  about shared diagnostic data do not apply.

### 2026-07-31 — Phased guard for the adapter's JLS accessibility check (via `// MODULE`)
- **Change**: the `FirBackedJavaClassAdapter.visibility` arm of `isInheritedNestedClassAccessible`
  (2026-07-14, IJ-FP `testIntellij_exceptionAnalyzer`) had no small-test guard: the adapter is only
  consulted for classes outside the module's own Java source index, and a phased test always gets
  Java as sources. `// MODULE: lib` / `// MODULE: main(lib)` closes that gap — a dependency module's
  Java class is not in `main`'s `JavaSourceRoot` set (`JavaDirectFacadeBuilder.kt:40`), so
  `classifierAdapterFor` returns the adapter exactly as it does for binary Java.
- **Files**: `testData/diagnostics/tests/jvm/javaDirect/packagePrivateInheritedNestedClassFromOtherModule.kt` (new).
- **Tests**: new test green in `JavaUsingAstPhasedTestGenerated` and in both PSI gates
  (`PhasedJvmDiagnostic{LightTree,Psi}TestGenerated`); full `JavaUsingAstPhasedTestGenerated`
  re-run green, 0 FAILED.
- **Result**: green; non-vacuity verified — forcing `visibility` back to `Visibilities.Public`
  makes it fail ("Actual data differs"). Note: diagnostics tests hard-code
  `DependencyKind.Source`, so this is *not* a compiled-jar dependency; a genuinely binary
  Java dependency still needs `PROVIDE_JAVA_AS_BINARIES` (foreign-annotations roots only) or a
  box test.

### 2026-07-30 — Binary index remeasured on `KotlinFullPipelineTestsGenerated`; not the M1 0.5%
- **Change**: `JavaClassFinderOverBinaryIndex` no longer builds an `FqName` per lookup: the top-level
  cache is now two-level, keyed by the `classId`'s own `packageFqName` + outermost class `Name`
  (new `FqName.topLevelName()`), so the string concatenation, `pathSegments()` list, `FqName`/
  `FqNameUnsafe` pair and re-hash are gone from the hot path (378994 lookups, ~1.5M objects per
  corpus). Paired in-run A/B: **631 -> 465 ns per hit lookup (-26%)**, but only ~54 ms per corpus.
- **Measured** (413-module FP corpus, JDK 8 launcher, 8 GB heap, M5 Max): the whole binary finder is
  **9.47 s = 0.99%** of 958 s of compilation, of which 72% is `readBinaryJavaClass` on 69617 class
  files — work master does through the same reader — 18% `knownClassNamesInPackage` (2.05M name
  strings, 35306 traversals), 5% top-level index traversals. Allocation/GC with `-Xjava-direct`
  on vs off is **identical** (216-221 GB per corpus, 112-119 young GCs, 8-9 full GCs), so extra
  garbage is not the mechanism. The regression does not reproduce here: two A/B batches disagree on
  the sign (±4%), i.e. developer-machine wall time cannot resolve 0.5%.
- **Files**: `JavaClassFinderOverBinaryIndex.kt`, review doc §12 (method, tables, and seven ranked
  strategies for locating the gap on the mini), this file. All harnesses removed.
- **Tests**: java-direct box+phased green; full `KotlinFullPipelineTestsGenerated` (413 modules)
  green.
- **Result**: index exonerated as the carrier of the M1 delta; see §12.5 for next steps.

### 2026-07-30 — `TopLevelClassFiles` shape: measured, holder kept
- **Change**: comment-only. Benchmarked the merged cache's value shape (holder object vs two-element
  array vs `SmartList`-style encoded slot vs the pre-merge two maps) on the real workload from §10
  (1469/1615 finders, 19k/12k cached keys, 77k/42k lookups per suite). The **whole** cache costs
  <1 ms and ~1.4 MB per full suite; the spread between shapes is ~0.15 ms/suite (~0.1 µs per
  compilation). The array is not faster — it is the slowest single-map variant on a 20× corpus
  (26.7 ms vs 23.4 ms) and loses the type/names; the encoded slot saves 33% of the allocation but
  trades the scoped/unscoped distinction for an unchecked encoding. Kept the named holder, added a
  KDoc pointer to `implDocs/BINARY_SOURCE_DIVIDE_REVIEW_2026_07_22.md` §11.
- **Files**: `JavaClassFinderOverBinaryIndex.kt` (KDoc +5), review doc §11, this file.
- **Tests**: java-direct box+phased green (no behaviour change).
- **Result**: won't-fix by measurement — see §11.

### 2026-07-30 — One top-level-class cache in the binary finder; roll back two no-op shared-file diffs
- **Change**: (1) `JavaClassFinderOverBinaryIndex` had two `FqName -> VirtualFile?` maps differing
  only in `firstOrNull { it in scope }` vs `firstOrNull()`. Measured over both suites (3084 finder
  instances): 69–75% of scoped keys were also in the all-scope map, **every** shared key had an
  identical value, and no lookup ever saw >1 candidate or a candidate outside `scope`
  (`multiCandidate=0`, `noCandidateInScope=0`) — see
  `implDocs/BINARY_SOURCE_DIVIDE_REVIEW_2026_07_22.md` §10. Replaced by one
  `FqName -> TopLevelClassFiles(anywhere, inScope)` cache filled in a single index pass, which keeps
  the two answers distinct while dropping ~12k redundant index lookups per suite (−39%).
  (2) Reverted `FirJvmConflictsChecker` + the `FirSession.getJavaClassLikeSymbolByClassId` forwarder
  and `FirDirectJavaActualDeclarationExtractor` to master: master already bypassed the composite
  provider, so both diffs were pure restyling, and no fixture covers the Kotlin-vs-*binary*-Java
  redeclaration case the checker's comment described.
- **Files**: `JavaClassFinderOverBinaryIndex.kt` (−12/+15); reverted `FirJvmConflictsChecker.kt`,
  `fir-jvm/JavaSymbolProvider.kt`, `FirDirectJavaActualDeclarationExtractor.kt` (shared-file diff
  vs master for these three is now zero).
- **Tests**: java-direct box+phased green, `PhasedJvmDiagnosticLightTreeTestGenerated` green,
  `FirLightTreeBlackBoxCodegenTestGenerated*CompileKotlinAgainstKotlin*` green.
- **Result**: green.

### 2026-07-29 — Fail-safe finder dispatch: identify the source scope, default to binary
- **Change**: `createJavaDirectJavaFacadeBuilder` now takes `javaSourcesScope` instead of
  `librariesScope` and dispatches `scope === javaSourcesScope -> JavaClassFinderOverAstImpl`, every
  other scope -> `JavaClassFinderOverBinaryIndex` over *that* scope. Previously the single
  whitelisted case was the library scope, so any new binary scope (IC output,
  HMPP-fragment classpath) would silently have been answered by the source AST finder. Memo map is
  now an `IdentityHashMap` keyed by the scope object (was `System.identityHashCode` + ct.sym flag in
  a data class — collision-prone with several scopes in play); the `CliVirtualFileFinderFactory`
  lookup is hoisted out of the lambda. Also routed the HMPP-common library session
  (`JvmFrontendPipelinePhase.kt:397`) through `javaDirectFacade`: it already asks for
  `context.librariesScope`, so it reuses the *same* memoized binary finder, and it is a literal
  no-op when `useJavaDirect` is off. The IC precompiled-binaries provider still constructs the PSI
  facade deliberately — it has no test coverage (see the 2026-07-29 review notes).
- **Files**: `JavaDirectFacadeBuilder.kt` (−13/+16), `JvmFrontendPipelinePhase.kt` (2 lines),
  `implDocs/ARCHITECTURE.md` §3.
- **Tests**: java-direct box+phased 2792/2792, `PhasedJvmDiagnosticLightTreeTestGenerated`
  10988/10991 (3 pre-existing skips).
- **Result**: green. The green java-direct suite is now itself evidence the source branch is taken:
  if the scope identity failed to match, the source session would receive the binary finder and
  every Java-source test would fail.

### 2026-07-29 — Express binary/source sidedness by wiring, not by two lookup vocabularies
- **Change**: dropped both parallel APIs added by the divide commit. (1) Removed the source-only
  probes `isInSourceIndex` / `hasPackageInSources` / `sourceClassNamesInPackage` from
  `JavaClassFinder` + `FirJavaFacade`; every production finder is already single-sided by
  construction, so `JavaSymbolProvider` is back on `hasTopLevelClassOf` / `hasPackage` /
  `knownClassNamesInPackage` (this restores a real AST-index gate on java-direct, where
  `isInSourceIndex` was a constant `true`, and re-enables the facade caches). (2) Deleted the
  `JvmBinaryClassFinderInputs` seam: `JvmBinaryClassFinderInputsOverIndex` became
  `JavaClassFinderOverBinaryIndex : JavaClassFinder`, absorbing `LibraryJavaClassFinder` /
  `BinaryPackageInfoJavaPackage`, so `JvmClassFileBasedSymbolProvider` has one input
  (`javaFacade`), no `?:` fallbacks and no flag awareness. The `packagePartProvider` fallback in
  `hasPackage` (for `@file:JvmPackageName`) is now unconditional.
- **Files**: `JavaClassFinder.kt`, `FirJavaFacade.kt`, `JavaSymbolProvider.kt`,
  `JvmClassFileBasedSymbolProvider.kt`, `FirJvmSessionFactory.kt`, `JvmFrontendPipelinePhase.kt`,
  `JavaDirectFacadeBuilder.kt`, +`JavaClassFinderOverBinaryIndex.kt`,
  −`JvmBinaryClassFinderInputs.kt`, −`JvmBinaryClassFinderInputsOverIndex.kt`.
- **Tests**: java-direct box+phased 2792/2792, `PhasedJvmDiagnosticLightTreeTestGenerated`
  10991/10991, `FirLightTreeBlackBoxCodegenTestGenerated` (full) 10643/10643 incl.
  `CompileKotlinAgainstKotlin`.
- **Result**: green. See the superseded banner in
  `implDocs/BINARY_SOURCE_DIVIDE_REVIEW_2026_07_22.md`.

### 2026-07-29 — Re-derive the KT-74097 guard rationale after enum-entry annotations went lazy
- **Change**: re-traced whether the lazy Java annotation lists retire any cycle breaker. They do
  not: they removed the only known crashing trigger (`@Deprecated` enum constant,
  `testIntellij_vcs_git`), demoting `cycleSafeClassLikeSymbol` to genuine defense-in-depth, but the
  cycle class stays reachable — the `declarations` lazy reads `FirJavaClass.typeParameters`, whose
  bound enhancement iterates the *class's own* annotations via `extractDefaultQualifiers` (plus a raw
  outer-class `getClassLikeSymbolByClassId`); the enum-entry `returnTypeRef` is still eager because
  `SignatureEnhancement` requires a resolved ref; and 3 of 5 guard call sites carry no annotation
  (const-field values, `@Target`, type-argument substitution). The other breakers
  (`cycleGuardedSupertypeWalk`, supertype memoization, local `visited` sets) are annotation-agnostic.
  Docs/comments updated accordingly; also fixed the stale `JavaSupertypeLoopChecker` name (the code
  is `cycleGuardedSupertypeWalk` / `JavaModelSupertypeWalkGuard`).
- **Files**: `resolution/JavaModelSessionAccess.kt`, `test/.../JavaCycleBreakerTest.kt` (comments
  only), `AGENT_INSTRUCTIONS.md`.
- **Tests**: not run — comment/docs-only, verified via `git diff` that no code line changed.
- **Result**: green (no code changes).

### 2026-07-29 — Clamp the lightweight scanner's brace/paren balance at zero
- **Change**: review of `extractFileInfoLightweight` against its production ancestor
  `SingleJavaFileRootsIndex.JavaSourceClassIdReader` (cli-base). Unmatched closers drove the
  balances negative, so `atTypeDeclaration()` (`== 0`) stopped firing and every top-level type
  after a stray `}`/`)` was lost — for a stray `}` before the file's namesake class the file is
  dropped from the index entirely (`tryBuildFileEntry` requires the base name). Now clamped,
  mirroring `Kotlin.flex`'s `if (lBraceCount == 0) popState() else lBraceCount--`; no-op on
  well-formed input. Also dropped a dangling KDoc, the redundant `if (at(SEMICOLON)) advance()`
  (the class-scan loop skips it anyway) and mapped an empty package name to `null`.
  Divergences from the ancestor are intentional and stay: the `break@loop` on a non-`package`
  token (upstream appends the first class name to the package on a missing `;`), no `isPackageInfo`
  arm (`JavaPackageIndexer` routes `package-info.java` to `JavaPackageInfoIndexer` first), and
  `when (lexer.getTokenType())` instead of four `at()` calls. `getTokenType()` itself is a cached
  field read (`JavaLexer.locateToken`: `if (myTokenType != null) return`), so repeated calls need
  no hoisting; `getTokenText()` is the allocating one, and it is only reached at balance 0.
- **Files**: `util/JavaSourceIndex.kt` (+7/−12); `JavaParsingLightweightScannerTest.kt` (+47, 2 tests).
- **Tests**: `JavaParsing*` 16/16; box+phased 2792 executed / 0 FAILED. Both new tests fail
  without the clamp (`got [Broken]`, `got [Foo]`).
- **Result**: green (error-tolerance fix).

### 2026-07-29 — Delete the `JavaSourceFileReader` abstraction; read via `File.readText`
- **Change**: after the `VirtualFile`→`File` switch the interface had a single implementation
  (`DefaultJavaSourceFileReader`), `walkSourceRoots` had no callers (it served the deleted eager
  `buildIndex`), and no production or test call site ever substituted a reader — the parameter was
  threaded through four collaborators for nothing. Replaced by one internal
  `readJavaSourceFileText(File): String?`; the reader parameter is gone from `JavaClassCache`,
  `JavaPackageIndexer`, `JavaPackageInfoIndexer`, `JavaSupertypeGraph`,
  `JavaClassFinderOverAstImpl` and `extractFileInfoLightweight`. Content is now read with
  `File.readText()` instead of `String(readBytes(), UTF_8)`: measured over 1503 repo `.java` files
  (37 MB, interleaved rounds, 7 samples) the medians are 46 ms vs 48 ms — ~2 ms of pure read time
  per full corpus, i.e. negligible against lexing/parsing.
- **Files**: deleted `util/JavaSourceFileReader.kt` (−57), added `util/javaSourceFileText.kt` (+19);
  `JavaClassCache.kt`, `JavaClassFinderOverAstImpl.kt`, `JavaPackageIndexer.kt`,
  `JavaPackageInfoIndexer.kt`, `util/JavaSourceIndex.kt`, `util/JavaSupertypeGraph.kt`; tests
  `JavaParsingTestBase.kt`, `JavaParsingLightweightScannerTest.kt`; `implDocs/ARCHITECTURE.md`.
- **Tests**: `JavaParsing*` green; box+phased 2790 executed / 0 FAILED.
- **Result**: green (behaviour-preserving simplification).

### 2026-07-28 — Replace `FirJavaEnumEntry` with `buildEnumEntry` + lazy `MutableList` annotations
- **Change**: review follow-up on the 2026-07-21 KT-74097 fix — the hand-written `FirJavaEnumEntry`
  duplicated `FirEnumEntryImpl` (~180 LoC) just to host a lazy annotation slot. The tree generator
  gained a per-field opt-in (`LeafBuilder.listFieldsWithVar` + `useVarForListField` DSL in
  `AbstractBuilderConfigurator`; configured only for `builder(enumEntry)` in the FIR
  `BuilderConfigurator`), so default builder generation is unchanged and only
  `FirEnumEntryBuilder.annotations` becomes `var`. The enum-entry arm of
  `convertJavaFieldToFir` uses plain `buildEnumEntry` with a new
  `FirLazyJavaAnnotationMutableList` — an `AbstractMutableList` composing a plain
  `FirLazyJavaAnnotationList` (conversion reused via `toMutableList()` on first mutable access;
  only the 5 abstract members are overridden; cheap `isEmpty` keeps `toMutableOrEmpty()` from
  forcing conversion) — and `FirJavaLazyDeprecationsProvider`. `FirJavaEnumEntry` deleted.
- **Files**: `generators/tree-generator-common/.../Builder.kt`, `AbstractBuilderPrinter.kt`,
  `config/AbstractBuilderConfigurator.kt`, `fir/tree/tree-generator/.../BuilderConfigurator.kt`,
  regenerated `fir/tree/gen/.../builder/FirEnumEntryBuilder.kt` (single-line `val`→`var`),
  `fir-jvm/.../FirJavaFacade.kt`, `FirJavaAnnotationList.kt`, `javaAnnotationsMapping.kt`;
  deleted `FirJavaEnumEntry.kt`.
- **Tests**: box+phased green (0 FAILED); PSI gate + `CompileKotlinAgainstKotlin` gate green
  (shared fir-jvm + fir-tree edits).
- **Result**: green (behaviour-preserving simplification; laziness retained).

### 2026-07-28 — Comment-style cleanup of the binary/source-divide branch
- **Change**: rewrote/deleted LLM-verbose comments added since the divide commit (added
  comment lines ~290 → ~125): dropped restatements of obvious code, caller inventories,
  counterfactual "rather than" phrasing, per-`@param` chatter, `Stage`/`§`/`implDocs`
  references in shared `fir-jvm` sources; kept short why-notes, API contracts, and
  regression/testData guards. Codified the rules in `AGENT_INSTRUCTIONS.md` →
  *Source Comment Conventions* ("default is no comment" gate, ~3% density baseline).
- **Files**: comment-only edits across ~20 files in `cli-base`, `cli-jvm`, `fir-jvm`,
  `frontend.common.jvm`, `core/compiler.common.jvm`, `java-direct/{src,test}`; docs:
  `AGENT_INSTRUCTIONS.md` (conventions rewrite, reference-table refresh, stale status header).
- **Tests**: not run — comment-only change, verified via `git diff` that no code line changed.
- **Result**: green (no code changes).

### 2026-07-27 — Make Java-source package directory descent case-sensitive
- **Change**: `JavaPackageIndexer`'s per-package directory descent used `File(dir, segment).isDirectory`
  (added 2026-07-21 when the source path moved from `VirtualFile` to `java.io.File`), which is
  case-insensitive on macOS/Windows. A sibling source dir (`syntax/logger`, `platform/ml/session`)
  was wrongly accepted as package `Logger`/`Session`, so nested-class imports like
  `com.intellij.platform.syntax.Logger.Attachment` mis-split into a package prefix and reported
  `UNRESOLVED_IMPORT`. Descent now matches against the parent's real child names via `File.list()`
  (case-sensitive), mirroring the binary index / PSI VFS `findChild`.
- **Files**: `JavaPackageIndexer.kt` (new `descendDirectoriesCaseSensitive`, used by
  `findPackageDirectories` + `findPackageDirectoryUnder`).
- **Tests**: box+phased green (0 FAILED); `IntelliJFullPipelineTestsGenerated.testIntellij_platform_syntax`
  and `testIntellij_platform_ml` now pass with java-direct on.
- **Result**: regression fixed.

### 2026-07-22 — Gate the binary seam on `useJavaDirect`; delete dead finder; dedup the ASM binary reader
- **Change**: applied `implDocs/BINARY_SOURCE_DIVIDE_REVIEW_2026_07_22.md` §4.1/§4.2/§4.7.
  §4.1: the binary deserializer seam is now gated on `configuration.useJavaDirect` in
  `prepareJvmSessions` — ON uses `JvmBinaryClassFinderInputsOverIndex`, OFF returns `null` so the
  deserializer falls back to the PSI `FirJavaFacade` binary reader (both source and binary now share
  one flag). §4.2: removed dead `CombinedJavaClassFinder.kt` (no references). §4.7: extracted the
  shared `readBinaryJavaClass` core (caching + inner-class dispatch + `ClassifierResolutionContext`)
  into `frontend.common.jvm`; both `JvmBinaryClassFinderInputsOverIndex` and the binary branch of
  `KotlinCliJavaFileManagerImpl` delegate to it. Investigated §4.1's "turn off PSI finder creation":
  not doable now — `KotlinCliJavaFileManagerImpl` still backs JPMS `module-info` resolution
  (`ClasspathRootsResolver` → `JavaModuleInfo.read` → `findClass`) regardless of the flag, and its
  PSI class-loading branch is already inert by default (`usePsiClassFilesReading=false`).
- **Files**: `cli-jvm/.../JvmFrontendPipelinePhase.kt`, `cli-base/.../KotlinCliJavaFileManagerImpl.kt`,
  `frontend.common.jvm/.../classFiles/BinaryJavaClassReader.kt` (new),
  `JvmBinaryClassFinderInputsOverIndex.kt`; deleted `CombinedJavaClassFinder.kt`.
- **Tests**: box+phased green (0 FAILED); PSI (`PhasedJvmDiagnosticLightTreeTestGenerated`) 0 fail,
  `CompileKotlinAgainstKotlin` 0 fail (shared-pipeline + file-manager edits), `KotlinCliJavaFileManagerTest` 7/7.
- **Result**: green (behaviour-preserving refactor + flag-gated seam).

### 2026-07-21 — Thread `java.io.File` through the Java-source indexing path (drop internal `VirtualFile`)
- **Change**: the module no longer relies on `com.intellij.openapi.vfs.VirtualFile` for its own
  source-file representation/reading; source roots are consumed as `java.io.File` (the CLI's
  `JavaSourceRoot.file` is already a `File`, so the old VFS-resolution step is removed). Per-package
  directory descent uses `File(dir, segment)`/`listFiles()`; content is read via `readBytes()`
  decoded as UTF-8 (unchanged charset). Binary-class-finder/CLI wiring stays on `VirtualFile`
  intentionally (external contract).
- **Files**: `JavaDirectFacadeBuilder.kt`, `JavaPackageIndexer.kt`, `JavaPackageInfoIndexer.kt`,
  `util/JavaSourceFileReader.kt`, `util/JavaSourceIndex.kt`; tests `JavaParsingTestBase.kt`,
  `JavaParsingClassFinderTest.kt`, `JavaParsingLightweightScannerTest.kt`.
- **Tests**: box+phased green (0 FAILED); `JavaParsing*` green.
- **Result**: green. Watch-point: a missing source root now drops later in the pipeline
  (`isDirectory`/`isFile`) rather than at VFS resolution — equivalent end behaviour.

### 2026-07-21 — Defer Java enum-entry annotations via `FirLazyJavaAnnotationList` (KT-74097)
- **Change**: the enum-entry arm of `convertJavaFieldToFir` resolved annotations eagerly while
  materialising `FirJavaClass.declarations`, which could re-enter the same in-flight `ClassId`
  (self-cycle). New `FirJavaEnumEntry` (mirrors `FirJavaField`) backs `annotations`/
  `deprecationsProvider` with `FirLazyJavaAnnotationList`, so no eager resolution happens while
  `declarations` is built. Removed the now-dead `setAnnotationsFromJava`; the
  `cycleSafeClassLikeSymbol` guard is now defense-in-depth, not the sole crash preventer.
- **Files**: `fir-jvm/.../declarations/FirJavaEnumEntry.kt` (new), `fir-jvm/.../FirJavaFacade.kt`,
  `fir-jvm/.../javaAnnotationsMapping.kt`, `JavaCycleBreakerTest.kt` (comment).
- **Tests**: box+phased green; PSI (`PhasedJvmDiagnosticLightTreeTestGenerated`) +
  `CompileKotlinAgainstKotlin` gates green (shared fir-jvm edit).
- **Result**: green.

### 2026-07-21 — Reuse AST name extraction in `JavaSupertypeGraph` (drop `splitCanonicalFqName`)
- **Change**: supertype-reference name splitting no longer re-implements type-resolution via the
  generic-bracket-aware `splitCanonicalFqName` text scan; it reuses the AST-based
  `extractReferenceNameParts` (extracted from `JavaClassifierTypeOverAst` into `JavaTypeOverAst`),
  reading `JAVA_CODE_REFERENCE` identifier segments directly. The generic-argument edge case
  (`a.B<String>.C`) is preserved.
- **Files**: `util/JavaSupertypeGraph.kt`, `model/JavaTypeOverAst.kt`.
- **Tests**: box+phased green (0 FAILED); `JavaParsing*` green.
- **Result**: green.

### 2026-07-21 — Scan Java-lexer tokens in `JavaSourceIndex`; exclude comment/bad tokens from the light-tree root
- **Change**: `extractFileInfoLightweight` scans the Java-lexer token stream
  (`JavaSyntaxDefinition.createLexer`) instead of regex/comment-stripping to find the package name
  and top-level type names, using brace/paren balance for nesting; removed
  `PACKAGE_REGEX`/`DECLARATION_REGEX`, manual comment stripping, and the now-unused reader
  `openLineReader`. `JavaLightTree` synthetic-root children now also exclude comments (root-only,
  each declaration keeps its `DOC_COMMENT` for `@deprecated`) and `BAD_CHARACTER`.
- **Files**: `util/JavaSourceIndex.kt`, `util/JavaSourceFileReader.kt`, `parse/JavaLightTree.kt`.
- **Tests**: `JavaParsing*` (incl. lightweight scanner) green; box+phased green (0 FAILED).
- **Result**: green.

### 2026-07-20 — Perf review: memoize recomputed reads in the Java-source model
- **Change**: the model layer recomputed pure, AST-derived values on every access. Converted the
  hot ones to `by lazy(PUBLICATION)` (same precedent as `supertypes`/`typeParameters`): class
  keyword flags (`isInterface`/`isEnum`/`isRecord`/`isAnnotationType`/`isSealed`),
  `methods`/`fields`/`constructors`/`recordComponents`/`innerClassNames`/`annotations`; per-member
  `resolutionContext`/`valueParameters`/`returnType` and field
  `leadingFieldNode`/`modifierList`/`type`/`initializerNode`/`annotations`; type
  `rawTypeNameParts`/`typeArguments`. Memoizing the class collections is the key enabler — member
  wrappers are now stable, so the per-member lazies actually cache. Behaviour-preserving (pure
  functions of the immutable AST + already-lazy `classifier`).
- **Files**: `model/JavaClassOverAst.kt`, `model/JavaMemberOverAst.kt`, `model/JavaTypeOverAst.kt`.
- **Tests**: box+phased 2767/2767 (0 FAILED); `JavaParsing*` 105/105.
- **Result**: green. Full write-up + reviewed-healthy caches + riskier follow-ups (plain-`HashMap`
  concurrency in `JvmBinaryClassFinderInputsOverIndex`, annotation `classId` memoization, more
  per-type/param lazies) in `implDocs/PERFORMANCE_REVIEW_2026_07_20.md`.

### 2026-07-20 — Read package-level default-nullability annotations off binary `package-info.class`
- **Change**: the library-session facade's finder (was a no-op `findPackage`) now materialises a
  binary `<pkg>/package-info.class` and exposes its class-level annotations as the package's
  `JavaPackage.annotations`. Previously those were dropped, so JSR-305/JSpecify package defaults
  (`@ParametersAreNonnullByDefault`, `@TypeQualifierDefault`, `@NullMarked`, …) on a **binary**
  Java package were invisible: a type-variable parameter substituted with an explicitly nullable
  Kotlin type argument stayed nullable instead of becoming definitely-non-null, producing a
  spurious `UNSAFE_CALL` in user code (dokka's `Property<File?>.map { it.relativeToOrSelf(..) }`).
  The finder reuses the same memoised binary index the deserializer reads through; class/package
  existence still routes through the deserializer, so only annotations are added.
- **Files**: `JvmBinaryClassFinderInputsOverIndex.kt` (+`findPackageInfoClass`),
  `JavaDirectFacadeBuilder.kt` (`NoOpJavaClassFinder` → `LibraryJavaClassFinder` +
  `BinaryPackageInfoJavaPackage`), `cli-jvm/…/JvmFrontendPipelinePhase.kt` (thread the shared
  binary-inputs builder into the facade builder); new test
  `codegen/boxJvm/javaInterop/foreignAnnotationsTests/tests/dnnParameterFromBinaryPackageAnnotation.kt`.
- **Tests**: full box+phased suite green (0 FAILED). New reproducer fails without the fix with the
  exact reported symptom (`UNSAFE_CALL … nullable receiver of type 'String?'`) and passes with it.
- **Result**: regression fixed (java-direct now matches PSI on binary package defaults).

### 2026-07-16 — Remove the loose `probeFqnSplits` fallback: commit to the leftmost type like javac
- **Change**: `resolveQualifiedNameToClassIdFromParts` no longer retries a failed name as a plain
  `package.Class` split. Like javac (JLS 6.5.4/6.5.5), once a leftmost type is found the
  interpretation is committed: a failed member-type descent returns the *nonexistent* nested id of
  the committed prefix (full resolution), which stays unresolved downstream — red code, exactly as
  javac reports on a package/type name clash (JLS 6.1). The reentrance-safe flavor returns `null`
  instead, so supertype-walk seeding is never poisoned by a dangling id. `probeFqnSplits` deleted.
- **Tests**: the strict behavior conflicts with the PSI Java model (which loosely resolves the
  package interpretation), so the two tests pinning the loose behavior moved out of the shared
  roots: `qualifiedNamePackageClassClash.kt` deleted from the shared roots and recreated with
  javac-strict expectations in the new java-direct-owned `testData/diagnostics` root (wired into
  `TestGenerator`); the pre-existing `javac/qualifiedExpression/PackageVsClass2.kt` — verified
  against real javac to be red code ("cannot find symbol: class b, location: class a") — is
  skipped via the new `SkipTestsPinningPsiJavaModelDeviationsMetaConfigurator` and mirrored
  strictly in the same root. Strict diagnostics: `MISSING_DEPENDENCY_CLASS` on the call whose Java
  signature uses the clash name + `UNRESOLVED_REFERENCE` on members of the unresolved type.
- **Files**: `resolution/JavaTypeResolver.kt` (−32/+18), `testFixtures/…/components.kt`,
  `testFixtures/…/AbstractJavaUsingAstTest.kt`, `testFixtures/…/TestGenerator.kt`,
  `build.gradle.kts` (own testdata root registered); Scenario D refreshed in `ReadMe.md` and
  `implDocs/RESOLUTION_SCHEMA.md`.
- **Result**: the module is now javac-conformant on qualified-name resolution — the last
  deliberate JLS deviation (KT-87813's unsound loose fallback) is gone. Full module suite green
  (box + phased, 0 FAILED; the skipped PSI-pinning test is mirrored strictly).

### 2026-07-16 — Rewrite `resolveQualifiedNameToClassIdFromParts` as a left-to-right JLS 6.5.4 pass
- **Change**: replaced the recursive try-every-split loop (outer-prefix enumeration + per-prefix
  recursion, O(n²) probes) with a single non-recursive left-to-right pass that mirrors javac's
  PackageOrTypeName classification: first segment as a simple type name in scope (JLS 6.5.4.1),
  else grow the package prefix until a segment names a top-level type (JLS 6.5.4.2), then a
  member-type descent probing declared-then-inherited at every segment (JLS 6.5.5.2). The loose
  `probeFqnSplits` fallback survives, but is now reached only when the JLS pass fails — the sole
  divergence from javac remains the package/type name clash pinned by
  `qualifiedNamePackageClassClash.kt` (KT-87813).
- **Files**: `resolution/JavaTypeResolver.kt` (−36/+28); Scenario D refreshed in `ReadMe.md` and
  `implDocs/RESOLUTION_SCHEMA.md`.
- **Tests**: box+phased suite green (0 FAILED); `JavaParsing*` unit tests green.
- **Result**: simplification landed (behavior-preserving on the whole suite).

### 2026-07-15 — Drop redundant `JavaToKotlinClassMap` disjunct in `resolveFromJavaLang`
- **Change**: `resolveFromJavaLang` accepted a name when either `JavaToKotlinClassMap.mapJavaToKotlin`
  hit **or** `classExists` was true; the map disjunct is dead. It only ever probes `ClassId(java.lang, X)`,
  and `classExists` resolves those via the symbol provider whose JVM builtins arm answers only `kotlin.*`
  ids (`StandardClassIds.builtInsPackages`), so a `java.lang.*` lookup never returns a `BuiltIns`-origin
  symbol filtered by `tryResolve` — it hits the JDK library class instead. Every mapped `java.lang` fqName
  (Object/String/Number/CharSequence/Comparable/Throwable/Cloneable/Iterable/Enum/wrappers/Deprecated)
  exists in both full JDK and mockJDK, so `classExists` already covers exactly what the map would.
  Collapsed to `classExists(classId, fullResolution)`, matching `resolveFromSamePackage`; better PSI
  parity (no divergence in the no-JDK case). Removed the now-unused `JavaToKotlinClassMap` import.
- **Files**: `resolution/JavaTypeResolver.kt` (−5/+1).
- **Tests**: box+phased suite green (0 FAILED/0 errors).
- **Result**: simplification landed (reviewer question — `classExists` alone suffices).

### 2026-07-15 — Nameless Java method recovered as a constructor (`testNamelessInJava`)
- **Change**: `JavaClassOverAst.constructors` required a constructor `METHOD` node to have both no
  return `TYPE` **and** an `IDENTIFIER`, so a malformed nameless declaration like `void () {}`
  (its `void` is an error element, not a return type) was dropped; with no explicit constructor a
  public default constructor was synthesized, so `class K : Nameless()` saw a visible constructor and
  produced no diagnostic. PSI (same syntax parser) treats any no-return-type method as a
  (package-private) constructor, suppressing the default one → `INVISIBLE_REFERENCE`. Dropped the
  `IDENTIFIER` requirement so constructor detection mirrors PSI's `getReturnTypeElement() == null`.
- **Files**: `model/JavaClassOverAst.kt` (constructors filter). Test data unchanged (shared golden).
- **Tests**: full box+phased suite 2839/2839 (0 FAILED); `JavaParsingTest`/`JavaLightTreeTest` green.
- **Result**: regression fixed (new test from master merge; SDK 261 golden).

### 2026-07-14 — Skip inaccessible inherited nested classes (IJ-FP regression: `testIntellij_exceptionAnalyzer`)
- **Change**: `walkSupertypeClassIds` accepted the first inherited nested class of a matching simple
  name regardless of accessibility, so a package-private nested type in a supertype from another
  package (e.g. `SimpleColoredComponent.TextRenderer`) wrongly shadowed a same-named top-level class,
  producing a spurious `RETURN_TYPE_MISMATCH`. Now filtered by JLS 6.6/8.2 accessibility: `private`
  never inherited, package-private only within the declaring package; inaccessible matches expand
  deeper instead of resolving. Visibility read cycle-safely via new `FirJavaClass.nonEnhancedVisibility`
  (from `originalStatus`, no lazy `status`) surfaced through `FirBackedJavaClassAdapter.visibility`.
- **Files**: `resolution/JavaInheritedClassResolver.kt`, `resolution/FirBackedJavaClassAdapter.kt`,
  `fir-jvm/.../FirJavaClass.kt` (+`nonEnhancedVisibility`); test
  `codegen/box/javaDirect/packagePrivateInheritedNestedClassNotVisibleAcrossPackages.kt`.
- **Tests**: box 1179/1179, phased 1513/1513 (0 FAILED); `IntelliJFullPipelineTestsGenerated.testIntellij_exceptionAnalyzer`
  green under java-direct; PSI + CompileKotlinAgainstKotlin gates green (shared `FirJavaClass` edit).
- **Result**: regression fixed.

### 2026-07-13 — Memoize `JavaClassOverAst.supertypes` (IJ-FP regression)
- **Change**: `supertypes` was a recomputing `get()` that returned fresh `JavaClassifierTypeOverAst`
  instances on every read; FIR forces it from two lazy slots per class (`FirJavaClass.superTypeRefs`
  enhancement and `directSupertypeClassIdsCache`), so each supertype's `classifier` (a per-instance
  lazy hitting the symbol provider) resolved from cold twice. Made it `by lazy(PUBLICATION)` so both
  reads share instances and each supertype resolves once. List build allocates wrappers only → still
  resolution-safe.
- **Files**: `model/JavaClassOverAst.kt` (supertypes get()→by lazy).
- **Tests**: box 1178/1178, phased 1513/1513 (0 FAILED).
- **Result**: regression fixed. IntelliJ `testIntellij_platform_ide_impl` warm frontend (isolated bench,
  4 iters, same build): java-direct 21.5s wall / 20.8s CPU vs legacy 25.3s / 23.6s — was ~+8% before.
