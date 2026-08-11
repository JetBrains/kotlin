# PSI-Free Roadmap (java-direct)

Current as of 2026-08-07. Replaces `implDocs/archive/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`
and `implDocs/archive/BINARY_SOURCE_DIVIDE_REVIEW_2026_07_22.md`.

## 1. Two axes, not one

Every earlier document treated "PSI-free" and "platform-free" as a single goal, and deferred both.
They are separable, and only the first one is done:

| Axis | Meaning | State |
|------|---------|-------|
| **PSI-free** | no `com.intellij.psi.*`, `com.intellij.core.*`, `KotlinCliJavaFileManagerImpl`, `GlobalSearchScope` | **done for `compiler/java-direct/src`**, enforced by `JavaDirectModuleBoundaryTest` |
| **platform-free** | no `VirtualFile`, no `JvmDependenciesIndex`, no IntelliJ application environment | not started; the seam is prepared |

`GlobalSearchScope` is the trap on the first axis: it lives in package `com.intellij.psi.search`
even though it is semantically a `VirtualFile` predicate, so it counts as PSI.

## 2. What java-direct depends on now

`compiler/java-direct/build.gradle.kts` no longer has `implementation(project(":compiler:cli"))`;
the `main` source set sees `frontend.common.jvm`, `fir:resolve`, `fir:fir-jvm`, `fir:entrypoint`,
`plugin-api`, `core:compiler.common.jvm` and `compileOnly(intellijCore())`.

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
| `CliBinaryClassFileIndex` + `CliVirtualFileFinderFactory.binaryClassFileIndex()` + `binaryClassFileScope()` | `compiler/cli/.../CliBinaryClassFileIndex.kt` | the only place that owns a `GlobalSearchScope`, the `asPsiSearchScope()` downcast and the `ct.sym` `.sig` extension choice |
| `BinaryJavaClassCache` | `frontend.common.jvm/.../classFiles/BinaryJavaClassCache.kt`, held by `FirJvmSessionFactory.Context` | the class-file lookups and loaded binary classes of one compilation, shared by every session; the injection point for a longer-lived cache (`BINARY_CLASS_CACHE_LIFETIME.md`, `CLASS_FILE_READ_LAYER.md`) |
| `FirJavaFacadeFactory` | `fir/entrypoint/.../session/FirJavaFacadeFactory.kt`, held by `FirJvmSessionFactory.Context` | which Java implementation serves a scope, as one per-compilation decision instead of a `createJavaFacade` lambda per construction site; `createJavaDirectJavaFacadeFactory` supplies the java-direct one, `psiJavaFacadeFactory()` is the default |
| `javaModuleFinder: JavaModuleFinder` parameter | `JavaDirectFacadeFactory.kt` | replaces a `CoreJavaFileManager` service lookup; `import module M;` no longer silently degrades |
| `javaSourceRoots: List<JavaSourceRootEntry>` parameter | `JavaDirectFacadeFactory.kt` | replaces reading `CLIConfigurationKeys.CONTENT_ROOTS` inside the module |
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
  index behind `JvmDependenciesIndexBinaryRoots`;
- `PsiBasedProjectFileSearchScope` — the sole implementation of `AbstractProjectFileSearchScope`.

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
  2798/2798. `JvmFrontendPipelinePhase` therefore passes
  `needRegisterJavaElementFinder = !configuration.useJavaDirect`. kapt is unaffected — it runs as a
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

Since the choice lives in `FirJvmSessionFactory.Context.javaFacadeFactory`, everything constructed
from that context — the library and source sessions, the symbol provider for the precompiled
binaries of incremental compilation (`FirJvmIncrementalCompilationSymbolProviders`), the JVM
interpretation of an HMPP common fragment's classpath, the scripting/REPL additional-libraries
session — follows the compilation's decision, and a context which chooses nothing stays on PSI.

What is left:

- `FirJKlibSessionFactory` (`compiler/cli/cli-jklib/`) is a sibling of `FirJvmSessionFactory` with
  its own `Context`, and calls `projectEnvironment.getFirJavaFacade` directly for both the JVM
  classpath and the module's own `.java` files; it also registers `FirJavaElementFinder`
  unconditionally. Bringing it to the same denominator means giving its `Context` a
  `FirJavaFacadeFactory` and deriving it in `prepareJKlibSessions` as `prepareJvmSessions` does.
  Untested for java-direct today, so it needs the JKlib suites as a gate.
- The LL-API/IDE and K1 sides are out of scope entirely (see above).
