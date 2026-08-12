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
| `BinaryClassFileIndex`, `BinaryClassFileScope`, `BinaryClassFileHandle` | `frontend.common.jvm/.../classFiles/BinaryClassFileIndex.kt` | the binary classpath of one compilation and the part of it one session sees, with no scope object and no `JvmDependenciesIndex` |
| `JvmClasspath` (`Roots` / `ProjectLibraries(excludedRoots)`) | `frontend.common.jvm/.../jvm/environment/JvmClasspath.kt` | *replaces* `AbstractProjectFileSearchScope`: a part of the classpath named by its roots, not an opaque file set with set algebra. No complement, no ambient universe, one shape per real use |
| `CliBinaryClassFileIndex` + `CliVirtualFileFinderFactory.binaryClassFileIndex()` + `VfsBasedProjectEnvironment.binaryClassFileScope()` | `compiler/cli/.../CliBinaryClassFileIndex.kt` | the `ct.sym` `.sig` extension choice, and the per-file scope predicate built from `psiSearchScope(classpath)` |
| `BinaryJavaClassCache` | `frontend.common.jvm/.../classFiles/BinaryJavaClassCache.kt`, held by the compilation's java-direct `FirJavaInterop` | the class-file lookups and loaded binary classes of one compilation, shared by every session; the injection point for a longer-lived cache (`BINARY_CLASS_CACHE_LIFETIME.md`, `CLASS_FILE_READ_LAYER.md`) |
| `FirJavaInterop` | `fir/fir-jvm/.../session/FirJavaInterop.kt`, held by `FirJvmSessionFactory.Context` (required, no default) | which Java implementation serves the compilation, as one per-compilation decision instead of a `createJavaFacade` lambda per construction site; `createJavaDirectJavaInterop` (java-direct) and `VfsBasedProjectEnvironment.psiJavaInterop()` (`compiler/cli/`) are peers. Split by *role*: `createBinaryJavaFacade(classpath)` and `createJavaSourcesFacade()` |
| `javaModuleFinder: JavaModuleFinder` parameter | `JavaDirectJavaInterop.kt` | replaces a `CoreJavaFileManager` service lookup; `import module M;` no longer silently degrades |
| `javaSourceRoots: List<JavaSourceRootEntry>` parameter | `JavaDirectJavaInterop.kt` | replaces reading `CLIConfigurationKeys.CONTENT_ROOTS` inside the module |
| `readBinaryJavaClass(topLevelClassFile: BinaryClassFileHandle, …)` | `frontend.common.jvm/.../BinaryJavaClassReader.kt` | lets the reader be driven from a handle instead of a `VirtualFile` |
| `JavaModuleInfo.read(file, classesByClassId)` | `frontend.common.jvm/.../JavaModuleInfo.kt` | takes a `ClassIdToJavaClass` resolver instead of a `KotlinCliJavaFileManager` + scope |

All CLI and VFS construction happens in `JvmFrontendPipelinePhase`; java-direct receives only
abstract inputs.

## 4. Still platform-bound

`BinaryClassFileHandle.virtualFile` is the one remaining transitional accessor. It cannot be
removed while `BinaryJavaClass` is `VirtualFile`-bound: the class implements
`VirtualFileBoundJavaClass` and resolves its nested classes through
`virtualFile.parent.findChild(...)`. Nothing in java-direct reads the accessor.

The platform-free axis needs, in rough dependency order:

- `BinaryJavaClass` / `VirtualFileBoundJavaClass` — nested-class lookup over a directory handle;
- `VirtualFileKotlinClass`, `CliVirtualFileFinder`, `VirtualFileFinderFactory`,
  `KotlinBinaryClassCache` — the Kotlin-side binary reader;
- `JvmDependenciesIndexImpl`, `JvmDependenciesDynamicCompoundIndex`, `JavaRoot` — the classpath
  index behind `CliBinaryClassFileIndex`;
- `VfsBasedProjectEnvironment.psiSearchScope(JvmClasspath)` (`compiler/cli/`) — the single
  classpath → `GlobalSearchScope` adapter. A root-list classpath can be honoured directly by a
  future NIO index, which is what makes `BinaryClassFileHandle.virtualFile` removable.

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
of this compilation" is the implementation's own knowledge — java-direct has its
`javaSourceRoots`, the PSI peer takes a per-module scope function (`AllJavaSourcesInProjectScope` in
the CLI, empty for scripting and the REPL, the module's own files in the test infrastructure). This
is what removed java-direct's `fileSearchScope === javaSourcesScope` identity check and the
`IdentityHashMap` that made it work.

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
own `Context`; `createLibrarySession`/`createSourceSession` now take a `FirJavaInterop` instead
of reaching for the environment, and `prepareJKlibSessions` passes `psiJavaInterop()`. It is
therefore *explicitly* on PSI, for the JVM classpath and for the module's own `.java` files alike,
and it therefore also registers `FirJavaElementFinder`, since that now follows from the same object.
Wiring java-direct into it means deriving the interop there as `prepareJvmSessions` does — one
argument, nothing else; untested for java-direct today, so it needs the JKlib suites as a gate.

The LL-API/IDE and K1 sides are out of scope entirely (see above).
