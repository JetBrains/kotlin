# PSI `JavaClassFinder` Usage and Replacement (java-direct, current `HEAD`)

> **Status snapshot.** Baseline is `HEAD` (commit `b300d9ac8536`, see `ITERATION_RESULTS.md`).
> The `java-direct` plugin is wired up unconditionally in `JvmConfigurationPipelinePhase`, so every
> production JVM-FIR compilation goes through `JavaClassFinderOverAstFactory`. This document is a
> design analysis only — **no production source files are modified.**
>
> See also: [`AGENT_INSTRUCTIONS.md`](../AGENT_INSTRUCTIONS.md),
> [`implDocs/ARCHITECTURE.md`](ARCHITECTURE.md),
> [`implDocs/RESOLUTION_PIPELINE.md`](RESOLUTION_PIPELINE.md),
> [`implDocs/archive/EXTERNAL_DEPENDENCIES_RESOLUTION_ANALYSIS.md`](archive/EXTERNAL_DEPENDENCIES_RESOLUTION_ANALYSIS.md),
> [`implDocs/archive/rethinking-structure.md`](archive/rethinking-structure.md),
> [`implDocs/archive/initial-analysis.md`](archive/initial-analysis.md).

---

## 0. Executive Summary

Even when `java-direct` is enabled, the legacy PSI-based `JavaClassFinderImpl`
(`compiler/frontend.java/src/.../JavaClassFinderImpl.kt`) is still installed as the **binary half**
of `CombinedJavaClassFinder` for both the source and library FIR sessions. The `java-direct` AST
finder (`JavaClassFinderOverAstImpl`) only knows about Java *source* roots, so every FIR query that
ultimately needs JDK/JAR data — including `findPackage`, `knownClassNamesInPackage`,
`hasTopLevelClassOf`, and `findClass` for binary `JavaClass` materialization — falls through to
PSI.

The design proposed here delivers the replacement in **three phases**, ordered by risk. The
strategic goal is to remove the IntelliJ-platform / PSI dependency from the JVM-FIR compilation
path entirely; PSI lives on for at most 1–2 releases, and only as a *source-side* fallback.

* **Phase 1 — stepping stone (`BinaryJavaClassFinder`).** A new `BinaryJavaClassFinder` backed by
  `JvmDependenciesIndex` / `KotlinClassFinder` (already injected into the same FIR session) and
  the existing ASM-based `BinaryJavaClass` replaces the PSI binary half of
  `CombinedJavaClassFinder`. This phase preserves the existing `FirJavaFacade.classFinder`
  contract so the change is observationally equivalent and a true PSI-vs-index A/B test is
  possible. It is **not** a destination and is **not** kept alive for 1–2 releases — it is a
  low-risk landing zone for the new binary data source. PSI is removed from this layer as soon as
  the new finder is green.
* **Phase 2 — structural refactoring (the actual end-state for the call graph).**
  `JavaSymbolProvider` becomes source-only and the binary lookups (callers (A)–(I) catalogued in
  Part 1) move into `JvmClassFileBasedSymbolProvider`, which already owns the
  `JvmDependenciesIndex` / `KotlinClassFinder` data sources. `CombinedJavaClassFinder` *and* the
  `BinaryJavaClassFinder` introduced in Phase 1 disappear; `FirJavaFacade.classFinder` becomes a
  source-only finder. This is the design philosophy the codebase already follows for *Kotlin*
  binaries (one symbol provider per data source) — Phase 2 extends the same shape to Java.
* **Phase 3 — transitional source-only switch (1–2 releases).** `JavaClassFinderFactory` is
  re-purposed as a *source-finder* factory whose flag chooses `JavaClassFinderOverAstImpl`
  (java-direct) or `JavaClassFinderImpl` scoped to source files (legacy PSI source path). This is
  the **only** place PSI remains in the `java-direct`-enabled compiler. After 1–2 releases —
  once the AST source path has been validated against PSI source parity in production — PSI is
  removed from the compiler entirely, alongside the broader effort to shed the IntelliJ-platform
  dependency.

**Today** — `CombinedJavaClassFinder` with PSI as the binary half:

```
   FirJavaFacade.classFinder
            │
            ▼
   CombinedJavaClassFinder
       ├─► JavaClassFinderOverAstImpl   (source roots, AST)
       └─► JavaClassFinderImpl          (PSI / KotlinJavaPsiFacade — JDK / JAR)
```

**Phase 1 (stepping stone, short-lived)** — PSI binary half swapped for an index-based peer:

```
   FirJavaFacade.classFinder
            │
            ▼
   CombinedJavaClassFinder
       ├─► JavaClassFinderOverAstImpl   (source roots, AST)
       └─► BinaryJavaClassFinder        (JvmDependenciesIndex / KotlinClassFinder + BinaryJavaClass)
```

**Phase 2 + Phase 3 (end-state)** — abstraction collapses; deserializer owns binaries; PSI lives
only briefly as a source-side fallback:

```
   FirJavaFacade.classFinder            JvmClassFileBasedSymbolProvider
   (source-only)                         (binary, owns its data source directly)
            │                                       │
            ▼                                       ▼
   ┌─────────────────────────┐           JvmDependenciesIndex / KotlinClassFinder
   │ JavaClassFinderOverAst  │           (the BinaryJavaClassFinder of Phase 1
   │   ‖  (Phase 3 flag)     │            no longer exists; the deserializer
   │ JavaClassFinderImpl     │            consumes the index directly)
   │ (source scope only;     │
   │  removed after 1–2      │
   │  releases)              │
   └─────────────────────────┘
```

---

# Part 1 — Why and where the PSI `JavaClassFinder` is used

## 1.1 Wiring overview

### 1.1.1 `JavaDirectPluginRegistrar` registers a factory unconditionally

In `compiler/cli/cli-jvm/src/.../JvmConfigurationPipelinePhase.kt` the plugin is currently
registered for every JVM compilation (the language-feature gate is commented out):

```kotlin
//        if (configuration.languageVersionSettings.supportsFeature(LanguageFeature.JavaDirect) || arguments.javaDirect) {
            configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, JavaDirectPluginRegistrar())
//        }
```

The registrar then installs the AST factory:

```kotlin
// compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt:18-27
class JavaDirectPluginRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        JavaClassFinderFactory.registerExtension(JavaClassFinderOverAstFactory(configuration))
    }
    ...
}
```

### 1.1.2 `JavaClassFinderOverAstFactory` builds `CombinedJavaClassFinder(sourceFinder, PSI)`

```kotlin
// compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt:29-54
class JavaClassFinderOverAstFactory(private val configuration: CompilerConfiguration) : JavaClassFinderFactory {
    override fun createJavaClassFinder(
        scope: AbstractProjectFileSearchScope,
        annotationProvider: JavaAnnotationProvider?,
        localFs: VirtualFileSystem,
        defaultFinderProvider: (() -> JavaClassFinder)?,
    ): JavaClassFinder {
        val roots: List<VirtualFile> = configuration.javaSourceRoots.mapNotNull(localFs::findFileByPath)

        // For library session (no Java sources), just use the default finder
        if (roots.isEmpty()) {
            return defaultFinderProvider?.invoke()
                ?: throw IllegalStateException("No Java source roots and no default finder provider")
        }

        val sourceFinder = JavaClassFinderOverAstImpl(roots)
        val binaryFinder = defaultFinderProvider?.invoke() ?: return sourceFinder
        return CombinedJavaClassFinder(sourceFinder, binaryFinder)
    }
}
```

Key observation: the factory uses `configuration.javaSourceRoots` regardless of which
`fileSearchScope` it is asked to serve. A normal `kotlinc` invocation has the same Java source
roots configured for source and library sessions alike, therefore:

* **Source session** — `roots` is non-empty → `CombinedJavaClassFinder(source, PSI-binary)`.
* **Library session** — same `configuration.javaSourceRoots`, so `roots` is also non-empty → the
  library session's `FirJavaFacade` ALSO holds `CombinedJavaClassFinder(source, PSI-binary)`.
* Only when `configuration.javaSourceRoots` is genuinely empty for a session (rare in practice — it
  happens, e.g., for a Kotlin-only module without Java sources) does the factory short-circuit to
  the bare PSI finder.

### 1.1.3 `VfsBasedProjectEnvironment.getFirJavaFacade` is the single injection point

```kotlin
// compiler/cli/src/.../VfsBasedProjectEnvironment.kt:205-224
override fun getFirJavaFacade(
    firSession: FirSession,
    baseModuleData: FirModuleData,
    fileSearchScope: AbstractProjectFileSearchScope
): FirJavaFacadeForSource {
    val javaAnnotationProvider = firSession.javaAnnotationProvider
    val localFs = knownFileSystems.first { it.protocol == StandardFileSystems.FILE_PROTOCOL }
    val psiSearchScope = fileSearchScope.asPsiSearchScope()
    val defaultFinderProvider: () -> JavaClassFinder = {
        project.createJavaClassFinder(psiSearchScope, javaAnnotationProvider)   // <-- PSI
    }
    val javaClassFinder = extensionsStorage?.get(JavaClassFinderFactory)?.firstOrNull()
        ?.createJavaClassFinder(fileSearchScope, javaAnnotationProvider, localFs, defaultFinderProvider)
        ?: defaultFinderProvider()
    return FirJavaFacadeForSource(firSession, baseModuleData, javaClassFinder)
}
```

The closure `defaultFinderProvider` is **the** entry point that produces a `JavaClassFinderImpl`
via `Project.createJavaClassFinder` (`compiler/frontend.java/src/.../JavaClassFinderImpl.kt`).
It is invoked in three observable places:

* `JavaClassFinderOverAstFactory.createJavaClassFinder` — almost always (see 1.1.2).
* `getFirJavaFacade`'s own fallback — when no `JavaClassFinderFactory` extension is registered.
* It is also reachable in the `roots.isEmpty()` shortcut of the factory.

`getFirJavaFacade` itself is the constructor seam used by every JVM-FIR session factory:

```kotlin
// compiler/fir/entrypoint/src/.../FirJvmSessionFactory.kt:159-160 (source session)
val javaSymbolProvider =
    JavaSymbolProvider(session, projectEnvironment.getFirJavaFacade(session, moduleData, javaSourcesScope))

// compiler/fir/entrypoint/src/.../FirJvmSessionFactory.kt:106  (library session, inside JvmClassFileBasedSymbolProvider)
projectEnvironment.getFirJavaFacade(session, moduleData, context.librariesScope)

// compiler/cli/cli-jklib/src/.../FirJKlibSessionFactory.kt:162-163 (JKlib mirrors the same pattern)
val javaSymbolProvider =
    JavaSymbolProvider(session, projectEnvironment.getFirJavaFacade(session, moduleData, javaSourcesScope))
```

### 1.1.4 Test fixture wiring is intentionally narrower

`compiler/java-direct/testFixtures/.../VfsBasedProjectEnvironmentOverAst.kt:43-55` overrides
`getFirJavaFacade` so that **only** the source session uses `JavaClassFinderOverAstImpl` and the
library session falls back to plain PSI without any `CombinedJavaClassFinder`:

```kotlin
override fun getFirJavaFacade(...): FirJavaFacadeForSource {
    if (fileSearchScope === librariesScope) {
        return super.getFirJavaFacade(firSession, baseModuleData, fileSearchScope) // PSI-only
    }
    val javaClassFinder = JavaClassFinderOverAstImpl(javaSourceRootVFiles)
    return FirJavaFacadeForSource(firSession, baseModuleData, javaClassFinder)
}
```

The production path therefore exposes a *broader* PSI surface than the test path — the library
session in production uses `CombinedJavaClassFinder` (source-first then PSI), whereas the tests
only ever exercise PSI directly for libraries. This is significant because the
"source-then-binary" union over-reports `knownClassNamesInPackage` for the library session: see
1.4.

## 1.2 `FirJavaFacade` accessors and their fall-through to PSI

`FirJavaFacade` is the only consumer of `classFinder` in production FIR. Every accessor and its
delegated `CombinedJavaClassFinder` method:

| Accessor (file:line) | `CombinedJavaClassFinder` method | Fall-through to PSI happens when… |
|---|---|---|
| `findClass(classId, knownContent)` — `FirJavaFacade.kt:74-77` | `findClass(Request)` — source-first if `sourceFinder.isClassInIndex(classId)`, otherwise binary | `classId` is NOT in the AST source index (every JDK/JAR class), or the source lookup returned `null` |
| `hasPackage(fqName)` — `FirJavaFacade.kt:79-80` (via `packageCache` line 64-70) | `findPackage(fqName, mayHaveAnnotations)` — source-first then binary | `sourceFinder.findPackage` returned `null` (e.g. `java.util`) |
| `hasTopLevelClassOf(classId)` — `FirJavaFacade.kt:82-85` | indirect — uses `knownClassNamesInPackage(classId.packageFqName)` | always — see next row |
| `knownClassNamesInPackage(fqName)` — `FirJavaFacade.kt:87-91` (cached `classFinder::knownClassNamesInPackage` at line 72) | `knownClassNamesInPackage(fqName)` — **UNION of source + binary** (`CombinedJavaClassFinder.kt:74-82`) | always — both halves are queried and merged |
| `canComputeKnownClassNamesInPackage()` — `FirJavaFacade.kt:89` | `canComputeKnownClassNamesInPackage()` — `source OR binary` | always — both finders are asked |

The `findPackage` and `findClass` paths are **source-first then PSI**, but `knownClassNamesInPackage`
is **union-of-both**. That distinction matters for the library session: the library
`JvmClassFileBasedSymbolProvider` reuses the union as a quick "is this package in the universe?"
filter (1.4), so the result includes source-only Java classes that the binary deserializer has no
business looking at. Today this is harmless (those names just lead to a later
`hasTopLevelClassOf → findClass → null` chain in the deserializer), but the same union shape will
constrain the replacement design (see 2.3).

## 1.3 Source-session callers (`JavaSymbolProvider`)

```kotlin
// compiler/fir/fir-jvm/src/.../JavaSymbolProvider.kt:32-77
open class JavaSymbolProvider(
    session: FirSession,
    protected val javaFacade: FirJavaFacade,
) : FirSymbolProvider(session) {

    protected val classCache: FirCache<ClassId, FirRegularClassSymbol?, ClassCacheContext?> =
        session.firCachesFactory.createCache createValue@{ classId, context ->
            val javaClass = context?.foundJavaClass ?: javaFacade.findClass(classId) ?: return@createValue null   // (B)
            val symbol = FirRegularClassSymbol(classId)
            javaFacade.convertJavaClassToFir(symbol, context?.parentClassSymbol, javaClass)
            symbol
        }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? =
        if (javaFacade.hasTopLevelClassOf(classId)) getClassLikeSymbolByClassId(classId, null) else null         // (A)

    override fun hasPackage(fqName: FqName): Boolean = javaFacade.hasPackage(fqName)                            // (C)

    override val symbolNamesProvider: FirSymbolNamesProvider = object : FirSymbolNamesProviderWithoutCallables() {
        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
            javaFacade.knownClassNamesInPackage(packageFqName)?.mapToSetOrEmpty { Name.identifier(it) }         // (D)
    }
}
```

| # | Callsite (file:line) | Facade accessor used | What PSI gives that the AST source index doesn't | Failure mode if PSI removed naïvely |
|---|---|---|---|---|
| **(A)** | `getClassLikeSymbolByClassId` — `JavaSymbolProvider.kt:49-50` | `hasTopLevelClassOf(classId)` → `knownClassNamesInPackage(classId.packageFqName)` (union) | The binary half lists JDK/JAR top-level class names, so the gate also passes for `java.lang.String`, `java.util.List`, etc. | Without the binary union, the gate would return `false` for every classpath class, and FIR would conclude that no Java class with that ClassId exists — every reference to a JDK/library Java class would be unresolved. |
| **(B)** | `classCache.createValue` — `JavaSymbolProvider.kt:41-47` | `findClass(classId)` (source-first then binary) | A real `JavaClass` instance for binary classes (PSI-backed `JavaClassImpl`). | Without the binary half, only Java classes from this module's source roots could be materialised. Every supertype reference, parameter type, etc., that points to a JDK/library class would yield `null` and break enhancement. This is the path that synthesizes a `JavaClass` from a binary `.class` — see also 1.4 (B'). |
| **(C)** | `hasPackage(fqName)` — `JavaSymbolProvider.kt:70` | `hasPackage` → `findPackage` (source-first then binary) | Existence of JDK packages (`java.lang`, `kotlin`, …) and library packages. | Removing PSI would make every binary package report as missing, breaking package-prefix imports and existence checks in the resolver. |
| **(D)** | `symbolNamesProvider.getTopLevelClassifierNamesInPackage` — `JavaSymbolProvider.kt:72-77` | `knownClassNamesInPackage` (union of source + binary) | Top-level class *names* in JDK/JAR packages, used by `FirSymbolNamesProvider` callers (e.g. completion-style lookups in tooling, package-importing flows). | Without the binary half, the names provider would only enumerate this module's source classes — package-level enumeration would be incomplete. |

`JavaSymbolProvider` is registered as `FirSession.javaSymbolProvider` (line 80) and is the central
re-entry point for all the indirect callers in 1.5.

## 1.4 Library-session callers (`JvmClassFileBasedSymbolProvider`)

```kotlin
// compiler/fir/fir-jvm/src/.../deserialization/JvmClassFileBasedSymbolProvider.kt
private fun computePackagePartInfo(packageFqName: FqName, partName: String): PackagePartsCacheData? {
    val classId = ClassId.topLevel(...)
    if (!javaFacade.hasTopLevelClassOf(classId)) return null                                          // (E) line 72
    ...
}

override fun knownTopLevelClassesInPackage(packageFqName: FqName): Set<String>? =
    javaFacade.knownClassNamesInPackage(packageFqName)                                                // (F) line 139

override fun extractClassMetadata(classId: ClassId, parentContext: FirDeserializationContext?): ClassMetadataFindResult? {
    if (!javaFacade.hasTopLevelClassOf(classId)) return null                                          // (G) line 171
    val result = kotlinClassFinder.findKotlinClassOrContent(classId, ownMetadataVersion)
    if (result !is KotlinClassFinder.Result.KotlinClass) {
        ...
        val knownContent = (result as? KotlinClassFinder.Result.ClassFileContent)?.content
        val javaClass = javaFacade.findClass(classId, knownContent) ?: return null                    // (H) line 180
        return ClassMetadataFindResult.NoMetadata { symbol ->
            javaFacade.convertJavaClassToFir(symbol, classId.outerClassId?.let(::getClass), javaClass)
        }
    }
    ...
}

override fun hasPackage(fqName: FqName): Boolean = javaFacade.hasPackage(fqName)                      // (I) lines 212-213
```

| # | Callsite (file:line) | Facade accessor | What PSI gives | Failure mode if PSI removed naïvely |
|---|---|---|---|---|
| **(E)** | `computePackagePartInfo` — `JvmClassFileBasedSymbolProvider.kt:72` | `hasTopLevelClassOf` (union) | Quick gate — is this package-part class anywhere on the classpath? | Returns `false` for every `.class` package-part on the classpath; package-level deserialization breaks. |
| **(F)** | `knownTopLevelClassesInPackage` — `JvmClassFileBasedSymbolProvider.kt:139` | `knownClassNamesInPackage` (union) | Names of top-level classes in a binary package (used by deserialized symbol provider for fast `hasName` checks). | Returns `null`/empty for binary-only packages; the deserializer falls back to slow paths or incorrectly reports "no such class". |
| **(G)** | `extractClassMetadata` — `JvmClassFileBasedSymbolProvider.kt:171` | `hasTopLevelClassOf` | Same gate as (E), guards the more expensive metadata lookup. | Same as (E) — every binary class read would short-circuit to `null`. |
| **(H)** | `extractClassMetadata` *no-metadata branch* — `JvmClassFileBasedSymbolProvider.kt:180` | `findClass(classId, knownContent)` (source-first then binary) | A real `JavaClass` for a pure-Java binary `.class` (one that has no `@kotlin.Metadata`). This is **the only place** in FIR core that asks PSI to materialise a `JavaClass` for an *already-loaded* binary `.class` — `knownContent` is the byte array `kotlinClassFinder` just read. | Without the binary half, every Java-only `.class` on the classpath (most of the JDK and most Java libraries) would yield `null` → no symbol → unresolved at use sites. |
| **(I)** | `hasPackage` — `JvmClassFileBasedSymbolProvider.kt:212-213` | `hasPackage` → `findPackage` | Existence of binary packages on the classpath. | Same as 1.3 (C). |

The interplay between `kotlinClassFinder.findKotlinClassOrContent` (which returns the
`.class` byte content via `KotlinClassFinder.Result.ClassFileContent`) and `javaFacade.findClass`
in (H) is what makes a clean replacement possible — the bytes are already in hand by the time PSI
is asked to "find" the class. We exploit this in 2.2.

## 1.5 Indirect callers via `session.javaSymbolProvider`

Every consumer of `FirSession.javaSymbolProvider.getClassLikeSymbolByClassId` reaches the chain
1.3 (A) → (B) → PSI for binary classes:

| Caller | File:line | Reason |
|---|---|---|
| `FirJvmConflictsChecker` | `compiler/fir/checkers/checkers.jvm/src/.../FirJvmConflictsChecker.kt:37` | Detect JVM redeclaration between Kotlin and Java classes; the Java side is fetched via the JavaSymbolProvider, and the **classifier-redeclaration diagnostic** for an external Java class would fire only because PSI surfaced it. |
| `FirDirectJavaActualDeclarationExtractor` | `compiler/fir/fir2ir/jvm-backend/src/.../FirDirectJavaActualDeclarationExtractor.kt:31-43` | Direct Java actualization (`expect class` → Java class). The lookup goes through `getClassLikeSymbolByClassId(expectIrClass.classIdOrFail)` — which is *exactly* the call path that hits PSI for a JDK/library actualizer. |
| `KaFirJavaInteroperabilityComponent` | `analysis/analysis-api-fir/src/.../KaFirJavaInteroperabilityComponent.kt:250` | LL-FIR Analysis API entry; **out of scope** for `java-direct` since LL has its own facade (1.6). |
| Lombok plugin `AbstractBuilderGenerator` | `plugins/lombok/lombok.k2/src/.../AbstractBuilderGenerator.kt:164, 271` | Discovers an existing Java builder class to decide whether to generate the methods. For a builder declared in a binary library, the lookup traverses 1.3 (A)/(B) → PSI. |

There are no other production FIR consumers of `JavaSymbolProvider` — `LLJvmSessionConfiguration`,
`LLFirSessionFactory`, `LLJvmSessionComponentRegistration`, etc., are all in the LL pipeline.

## 1.6 LL-FIR sibling pipeline (informative only)

The LL Analysis API has a parallel set of types — `LLFirJavaSymbolProvider` (`analysis/low-level-api-fir/src/.../symbolProviders/LLFirJavaSymbolProvider.kt:31`),
`LLCombinedJavaSymbolProvider` (`analysis/low-level-api-fir/src/.../symbolProviders/combined/LLCombinedJavaSymbolProvider.kt:41-46, 104-108`),
`LLJvmSessionConfiguration.kt:34`, `LLStubOriginLibrarySymbolProviderFactory.kt:51`. These build
their own `firJavaFacade` (typically also via `project.createJavaClassFinder`) and **do not** read
the `JavaClassFinderFactory` extension that `java-direct` registers. They are out of scope for
this work, but the doc lists them so a reader doesn't try to fix the `java-direct` PSI usage by
touching LL.

`LLCombinedJavaSymbolProvider.kt:72` is also the only production caller of the multi-result
`JavaClassFinder.findClasses(...)` in FIR — see next section.

## 1.7 `JavaClassFinder.findClasses` (multi-result) is PSI-legacy only

Search across the project (excluding `CombinedJavaClassFinder` itself, which only delegates):

* `compiler/frontend.java/src/.../JavaClassFinderImpl.kt:53` — the implementation.
* `analysis/low-level-api-fir/src/.../LLCombinedJavaSymbolProvider.kt:72` — sole LL caller.

There is **no** production FIR / `java-direct` caller of `findClasses`. A replacement
`BinaryJavaClassFinder` therefore does not need to support the multi-result form at all (a single
`findClass` is enough; `JavaClassFinderOverAstImpl.findClasses` exists only to satisfy the
interface contract — see 1.8).

## 1.8 Why `JavaClassFinderOverAstImpl` cannot replace PSI today

`compiler/java-direct/src/.../JavaClassFinderOverAstImpl.kt:37-141` indexes only Java *source*
roots. Concretely:

* **Coverage gap.** `sourceRoots: List<VirtualFile>` are only the configured `javaSourceRoots`
  (`JavaDirectPluginRegistrar.kt:37`). JDK classes, JARs, `ct.sym`, and `.class` files are not
  visited.
* **`knownClassNamesInPackage` is source-only** (line 105-106). Used as a `union` half by
  `CombinedJavaClassFinder.knownClassNamesInPackage` and as a strict fallback by
  `FirJavaFacade.hasTopLevelClassOf`. Without a binary peer, every binary-only package would
  appear empty.
* **`canComputeKnownClassNamesInPackage = true`** (line 108). This means the AST half always
  participates in `FirJavaFacade.knownClassNamesInPackage` — a binary peer is mandatory if PSI is
  to be removed.
* **No ASM bridge.** The AST finder has no path to read a binary `.class` and turn it into a
  `JavaClass`. A binary peer needs an ASM-backed reader; see 2.2.

# Part 2 — How to replace the PSI `JavaClassFinder`

## 2.1 Goals and constraints

### Strategic goals (apply across all three phases)

* **Remove the IntelliJ-platform / PSI dependency** from the JVM-FIR compilation path. PSI is
  removed from binary lookups at the end of Phase 1 and from source lookups at the end of
  Phase 3. There is no `BinaryJavaClassFinder` and no `CombinedJavaClassFinder` in the
  end-state, and PSI does not survive past Phase 3.
* **Reuse data already loaded** by FIR's deserialized binary providers — concretely
  `KotlinClassFinder` (`core/deserialization.common.jvm/src/.../KotlinClassFinder.kt:24`) and the
  `JvmDependenciesIndex` (`compiler/cli/cli-base/src/.../JvmDependenciesIndex.kt:27`) that backs
  it via `CliVirtualFileFinder` (`compiler/cli/cli-base/src/.../CliVirtualFileFinder.kt`). After
  Phase 2 the deserializer owns those data sources directly; no separate binary-side
  `JavaClassFinder` exists.
* **Align the Java story with the existing Kotlin-binary story** — one symbol provider per data
  source. After Phase 2, `JvmClassFileBasedSymbolProvider` is the single owner of binary Java
  lookups (matching how it already owns binary Kotlin lookups), and `JavaSymbolProvider` /
  `FirJavaFacade.classFinder` are source-only.
* **No production source-file changes** in this iteration. This document is design-only; the
  migration steps below are sequenced for landing in subsequent iterations.

### Phase 1 constraints (stepping stone; observational equivalence)

These constraints apply *only* to the intermediate `BinaryJavaClassFinder` step (§2.2) and are
deliberately tight so that Phase 1 is purely a wiring change, not a behavioural change:

* **Eliminate the PSI binary half** of `CombinedJavaClassFinder` from the `java-direct`
  production wiring (factory in 1.1.2).
* **Preserve the existing semantics** of `FirJavaFacade.knownClassNamesInPackage` /
  `hasTopLevelClassOf` / `findClass`, including the union-of-both shape (1.2). The replacement
  must be observationally equivalent for the existing 2793/2793 (100%) `JavaUsingAst*` test runs
  so that PSI-vs-index can be A/B-tested behind a flag.
* **Avoid the `FirJavaFacade ↔ JvmClassFileBasedSymbolProvider` cycle.** The deserializer already
  calls `javaFacade.knownClassNamesInPackage` (1.4-F) and `javaFacade.findClass` (1.4-H). Building
  the new `FirJavaFacade.classFinder` *on top of* the deserializer would create a feedback loop
  through the facade. The Phase 1 finder must therefore be a **peer** of the deserializer, fed by
  the same `JvmDependenciesIndex` / `KotlinClassFinder` snapshot. (Phase 2 dissolves both the
  facade's binary half and the cycle by construction — see §2.4.)

### Phase 2 / Phase 3 constraints (structural)

* **Phase 2 is a behavioural refactoring**, not a wiring swap. The semantic-equivalence
  guarantee of Phase 1 is intentionally relaxed: `FirJavaFacade.classFinder` no longer answers
  binary queries, and `FirSession.javaSymbolProvider`'s contract narrows to "Java classes from
  the source scope". Indirect callers (1.5) must be re-routed to the proper provider chain (see
  §2.4).
* **Phase 3 narrows PSI to a single role** — the source-side finder behind a flag. The flag
  default flips to AST as soon as parity is verified, and PSI is removed from the compiler at
  the end of the 1–2 release transitional window. No PSI residue is intended past that point in
  the `java-direct`-enabled compiler.
* **LL-FIR is out of scope** in all three phases (1.6). LL keeps its own
  `LLFirJavaSymbolProvider` / `LLCombinedJavaSymbolProvider` chain, which does not consult
  `JavaClassFinderFactory` and is therefore unaffected by the switch.

## 2.2 Phase 1 — `BinaryJavaClassFinder` over `JvmDependenciesIndex` / `KotlinClassFinder`

> **Phase 1 is a stepping stone.** It is the smallest change that lets us land the new binary
> data source — `JvmDependenciesIndex` / `KotlinClassFinder` + `BinaryJavaClass` — under
> production load while keeping `FirJavaFacade.classFinder`'s observable contract intact, so a
> `-P` flag can A/B-test it against PSI without touching any caller. **It is dissolved in
> Phase 2** (§2.4) — both `BinaryJavaClassFinder` and `CombinedJavaClassFinder` disappear once
> binary lookups move into `JvmClassFileBasedSymbolProvider`. Phase 1 must therefore be designed
> for *easy removal*, not for long-term residence.

A new class — placement: `compiler/java-direct/src/.../BinaryJavaClassFinder.kt`, or a
JVM-FIR-shared module if other clients want it — implementing `JavaClassFinder` whose data sources
are:

```kotlin
class BinaryJavaClassFinder(
    private val kotlinClassFinder: KotlinClassFinder,           // already injected, FirJvmSessionFactory.kt:98
    private val index: JvmDependenciesIndex,                    // backs CliVirtualFileFinder
    private val scope: AbstractProjectFileSearchScope,          // same scope FirJavaFacade was given
    private val classifierResolutionContext: ClassifierResolutionContext, // for BinaryJavaClass
    private val signatureParser: BinaryClassSignatureParser,
) : JavaClassFinder {

    override fun findClass(request: JavaClassFinder.Request): JavaClass? {
        // Bypass kotlinClassFinder: we want a JavaClass, not a KotlinJvmBinaryClass.
        // Use the same JvmDependenciesIndex traversal as CliVirtualFileFinder.findBinaryOrSigClass.
        val virtualFile = index.findClassVirtualFiles(request.classId, BINARY_CLASS_AND_SIG_EXTENSIONS)
            .firstOrNull { it in scope.asPsiSearchScope() } ?: return null
        val bytes = request.knownContent ?: virtualFile.contentsToByteArray()
        return BinaryJavaClass(virtualFile, request.classId.asSingleFqName(), classifierResolutionContext,
                               signatureParser, outerClass = null, classContent = bytes)
    }

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> =
        listOfNotNull(findClass(request))                                // 1.7: only one result needed

    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        var found = false
        index.traverseDirectoriesInPackage(fqName, JavaRoot.OnlyBinary) { _, _ -> found = true; false }
        return if (found) BinaryJavaPackage(fqName) else null
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> {
        val result = HashSet<String>()
        index.traverseClassVirtualFilesInPackage(packageFqName, BINARY_CLASS_AND_SIG_EXTENSIONS) { vf ->
            result.add(vf.nameWithoutExtension); true
        }
        return result
    }

    override fun canComputeKnownClassNamesInPackage(): Boolean = true
}
```

### 2.2.1 What replaces what

Mapping back to the catalogue from Part 1 (callsite → replacement source):

| Callsite (Part 1) | Facade accessor | Replacement source in `BinaryJavaClassFinder` |
|---|---|---|
| 1.3 (A) `JavaSymbolProvider.getClassLikeSymbolByClassId` | `hasTopLevelClassOf` → `knownClassNamesInPackage` | `JvmDependenciesIndex.traverseClassVirtualFilesInPackage(BINARY_CLASS_AND_SIG_EXTENSIONS)` |
| 1.3 (B) `JavaSymbolProvider.classCache.createValue` | `findClass(classId)` | `JvmDependenciesIndex.findClassVirtualFiles(...)` → `BinaryJavaClass(virtualFile, ..., classContent = bytes)` |
| 1.3 (C) `JavaSymbolProvider.hasPackage` | `hasPackage` → `findPackage` | `JvmDependenciesIndex.traverseDirectoriesInPackage(JavaRoot.OnlyBinary)` |
| 1.3 (D) `JavaSymbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage` | `knownClassNamesInPackage` | same as (A) |
| 1.4 (E) `computePackagePartInfo` | `hasTopLevelClassOf` | same as (A) |
| 1.4 (F) `knownTopLevelClassesInPackage` | `knownClassNamesInPackage` | same as (A) |
| 1.4 (G) `extractClassMetadata` gate | `hasTopLevelClassOf` | same as (A) |
| 1.4 (H) `extractClassMetadata` no-metadata branch | `findClass(classId, knownContent)` | `BinaryJavaClass(... classContent = request.knownContent ?: virtualFile.contentsToByteArray())` — **the bytes are already in hand**, courtesy of `KotlinClassFinder.Result.ClassFileContent` |
| 1.4 (I) `hasPackage` | `hasPackage` → `findPackage` | same as (C) |
| 1.5 indirect (checker, fir2ir, Lombok) | re-enters 1.3 (A)/(B) | covered by the rows above |

### 2.2.2 Wiring change in `JavaClassFinderOverAstFactory`

The factory becomes:

```kotlin
override fun createJavaClassFinder(
    scope: AbstractProjectFileSearchScope,
    annotationProvider: JavaAnnotationProvider?,
    localFs: VirtualFileSystem,
    defaultFinderProvider: (() -> JavaClassFinder)?,
): JavaClassFinder {
    val roots: List<VirtualFile> = configuration.javaSourceRoots.mapNotNull(localFs::findFileByPath)

    // NEW: build a BinaryJavaClassFinder from JvmDependenciesIndex / KotlinClassFinder injected by the environment.
    val binaryFinder: JavaClassFinder = BinaryJavaClassFinder(
        kotlinClassFinder = ...,         // hand-off from VfsBasedProjectEnvironment
        index = ...,                     //   — both are available on the project environment
        scope = scope,
        classifierResolutionContext = sharedClassifierResolutionContext,
        signatureParser = sharedSignatureParser,
    )

    if (roots.isEmpty()) return binaryFinder
    val sourceFinder = JavaClassFinderOverAstImpl(roots)
    return CombinedJavaClassFinder(sourceFinder, binaryFinder)
}
```

Two operational notes:

* `JavaClassFinderFactory.createJavaClassFinder`'s current signature gives the factory a closure
  to build the *PSI* default; after the change, the closure is no longer used by `java-direct`.
  Until the migration is complete, the factory keeps the closure in its signature for backward
  compatibility but ignores it.
* The `kotlinClassFinder` / `JvmDependenciesIndex` instances need to be obtained from the
  `AbstractProjectEnvironment` for the same `scope` the facade was asked to serve — see
  `compiler/fir/entrypoint/src/.../AbstractProjectEnvironment.kt:44` (`getKotlinClassFinder`). For
  the index, `VfsBasedProjectEnvironment` already exposes it indirectly via
  `getKotlinClassFinder` returning a `CliVirtualFileFinder`; we either widen that API or pass the
  index to `JavaClassFinderFactory.createJavaClassFinder` as a new parameter.

### 2.2.3 What we get from `BinaryJavaClass` for free

`compiler/frontend.common.jvm/src/.../load/java/structure/impl/classFiles/BinaryJavaClass.kt:24-32`
already accepts the byte content and reads it via `ClassReader(classContent ?: virtualFile.contentsToByteArray())`:

```kotlin
class BinaryJavaClass(
    override val virtualFile: VirtualFile,
    override val fqName: FqName,
    internal val context: ClassifierResolutionContext,
    private val signatureParser: BinaryClassSignatureParser,
    override var access: Int = 0,
    override val outerClass: JavaClass?,
    classContent: ByteArray? = null,
) : ...
```

It implements `VirtualFileBoundJavaClass`, the same shape `JvmClassFileBasedSymbolProvider`
expects from the no-metadata branch (1.4-H). No new ASM-backed `JavaClass` is needed — the same
class K1 has been using for years is reused.

## 2.3 Phase 1 — Avoiding the deserializer ↔ class-finder cycle

> Phase-1-specific design concern. In Phase 2 the cycle disappears by construction because the
> deserializer no longer calls back into `FirJavaFacade.classFinder` for binary queries — see
> §2.4.

Today's call graph (with PSI-binary):

```
JvmClassFileBasedSymbolProvider.knownTopLevelClassesInPackage
    └─► FirJavaFacade.knownClassNamesInPackage
             └─► CombinedJavaClassFinder.knownClassNamesInPackage
                      ├─► JavaClassFinderOverAstImpl.knownClassNamesInPackage   (source roots)
                      └─► JavaClassFinderImpl.knownClassNamesInPackage          (KotlinJavaPsiFacade
                                                                                  → PsiElementFinder
                                                                                  → JvmDependenciesIndex)
```

If `BinaryJavaClassFinder` were built on top of `JvmClassFileBasedSymbolProvider`'s caches, you
get the cycle:

```
JvmClassFileBasedSymbolProvider.knownTopLevelClassesInPackage
    └─► FirJavaFacade.knownClassNamesInPackage
             └─► CombinedJavaClassFinder.knownClassNamesInPackage
                      └─► BinaryJavaClassFinder.knownClassNamesInPackage
                               └─► (back into JvmClassFileBasedSymbolProvider) ← LOOP
```

The proposal in 2.2 avoids this because `BinaryJavaClassFinder` reads from `JvmDependenciesIndex`
*directly* — the same index `CliVirtualFileFinder` walks. Both
`JvmClassFileBasedSymbolProvider.kotlinClassFinder` (`compiler/fir/fir-jvm/src/.../JvmClassFileBasedSymbolProvider.kt:53`)
and the new `BinaryJavaClassFinder` are siblings, both hanging off the same `JvmDependenciesIndex`
snapshot:

```
JvmDependenciesIndex   (single shared instance per project environment)
        │
        ├──► CliVirtualFileFinder ──► KotlinClassFinder ──► JvmClassFileBasedSymbolProvider
        │                                                       │
        │                                                       ▼
        │                                              FirJavaFacade.classFinder
        │                                                       │
        └────────────────────────────────────► BinaryJavaClassFinder (peer, NEW)
```

The deserializer continues to call `javaFacade.knownClassNamesInPackage` exactly as today
(1.4-F); it is now answered by an index walk identical to the one `kotlinClassFinder` uses, just
through the `JavaClassFinder` interface. The `extractClassMetadata` no-metadata branch (1.4-H)
also keeps its current shape:

```kotlin
val knownContent = (result as? KotlinClassFinder.Result.ClassFileContent)?.content
val javaClass = javaFacade.findClass(classId, knownContent) ?: return null
```

`knownContent` is already the bytes the new finder needs — no extra I/O.

## 2.4 Phase 2 — Structural refactoring (collapse the abstraction)

> **Phase 2 is the actual end-state for the call graph.** The `BinaryJavaClassFinder` introduced
> in Phase 1 *and* `CombinedJavaClassFinder` are removed. `FirJavaFacade.classFinder` becomes
> source-only. Binary lookups (callers (A)–(I) catalogued in Part 1) move into
> `JvmClassFileBasedSymbolProvider`, which already owns the `JvmDependenciesIndex` /
> `KotlinClassFinder` data sources used in Phase 1.

### 2.4.1 What changes

| Component | Before Phase 2 | After Phase 2 |
|---|---|---|
| `FirJavaFacade.classFinder` | `CombinedJavaClassFinder(source-AST, BinaryJavaClassFinder)` | `JavaClassFinderOverAstImpl` (source roots only) |
| `JavaSymbolProvider` (1.3 (A)–(D)) | Asks the facade for source ∪ binary | Source-only. `getClassLikeSymbolByClassId` and `classCache` only resolve classes whose `classId` is in the AST source index. `hasPackage` / `symbolNamesProvider` answer for source roots only. |
| `JvmClassFileBasedSymbolProvider` (1.4 (E)–(I)) | Calls `javaFacade.hasTopLevelClassOf` / `knownClassNamesInPackage` / `findClass` / `hasPackage` | Calls `JvmDependenciesIndex` / `KotlinClassFinder` directly, via small helpers; the no-metadata branch (1.4-H) builds `BinaryJavaClass(... classContent = (result as ClassFileContent).content ...)` in place. |
| `CombinedJavaClassFinder` | Source-first / union | **Deleted.** |
| `BinaryJavaClassFinder` (Phase 1) | Wired as the binary half | **Deleted.** Its index-walk helpers may be moved into `JvmClassFileBasedSymbolProvider` or a small `internal` utility object. |
| `FirSession.javaSymbolProvider` contract | "Any Java class, source or binary" | "Java classes from the source scope" — narrowed. |
| Indirect callers (1.5) | Call `session.javaSymbolProvider.getClassLikeSymbolByClassId(...)` and rely on the binary fall-through | Re-routed to `session.symbolProvider` (the union chain that includes the deserializer), with a Java-origin filter where the caller specifically wants Java-only. |

### 2.4.2 New end-state call graph

```
FirJvmSessionFactory
   ├─► JavaSymbolProvider                               (source-only)
   │       └─► FirJavaFacade.classFinder
   │              └─► JavaClassFinderOverAstImpl        (source roots, AST)
   │
   └─► JvmClassFileBasedSymbolProvider                  (binary, owns its data source)
           ├─► KotlinClassFinder.findKotlinClassOrContent  (Kotlin metadata path, unchanged)
           ├─► JvmDependenciesIndex.findClassVirtualFiles   (binary Java findClass)
           ├─► JvmDependenciesIndex.traverseClassVirtualFilesInPackage (knownTopLevelClassesInPackage)
           ├─► JvmDependenciesIndex.traverseDirectoriesInPackage(JavaRoot.OnlyBinary) (hasPackage)
           └─► BinaryJavaClass(virtualFile, ..., classContent = bytes) (no-metadata branch)
```

The cycle from §2.3 disappears by construction — there is no `FirJavaFacade.classFinder` edge
from the deserializer anymore.

### 2.4.3 Caller migration sketch (illustrative; not a code change in this iteration)

`JavaSymbolProvider` becomes source-only. The simplest concrete shape:

```kotlin
// compiler/fir/fir-jvm/src/.../JavaSymbolProvider.kt — illustrative end-state
override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? =
    if (javaFacade.isInSourceIndex(classId)) getClassLikeSymbolByClassId(classId, null) else null

override fun hasPackage(fqName: FqName): Boolean = javaFacade.hasPackageInSources(fqName)

override val symbolNamesProvider: FirSymbolNamesProvider = object : FirSymbolNamesProviderWithoutCallables() {
    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
        javaFacade.sourceClassNamesInPackage(packageFqName)?.mapToSetOrEmpty(Name::identifier)
}
```

`JvmClassFileBasedSymbolProvider` takes ownership of the binary lookups it used to delegate:

```kotlin
// compiler/fir/fir-jvm/src/.../deserialization/JvmClassFileBasedSymbolProvider.kt — illustrative
private fun hasBinaryTopLevelClass(classId: ClassId): Boolean =
    classId.shortClassName.asString() in binaryNamesInPackage(classId.packageFqName)

private fun binaryNamesInPackage(pkg: FqName): Set<String> =
    binaryNamesCache.getOrPut(pkg) { index.classNamesIn(pkg, BINARY_CLASS_AND_SIG_EXTENSIONS) }

override fun knownTopLevelClassesInPackage(packageFqName: FqName): Set<String>? =
    binaryNamesInPackage(packageFqName)

override fun extractClassMetadata(classId: ClassId, parentContext: FirDeserializationContext?): ClassMetadataFindResult? {
    if (!hasBinaryTopLevelClass(classId)) return null
    val result = kotlinClassFinder.findKotlinClassOrContent(classId, ownMetadataVersion)
    if (result !is KotlinClassFinder.Result.KotlinClass) {
        val virtualFile = (result as? KotlinClassFinder.Result.ClassFileContent)?.virtualFile ?: return null
        val bytes = (result as? KotlinClassFinder.Result.ClassFileContent)?.content ?: virtualFile.contentsToByteArray()
        val javaClass = BinaryJavaClass(virtualFile, classId.asSingleFqName(), context, signatureParser,
                                        outerClass = null, classContent = bytes)
        return ClassMetadataFindResult.NoMetadata { symbol -> convertJavaClassToFir(symbol, ..., javaClass) }
    }
    ...
}

override fun hasPackage(fqName: FqName): Boolean =
    index.hasBinaryPackage(fqName)
```

The same `BinaryJavaClass` reused in Phase 1 (§2.2.3) is reused here, just owned by a different
component. No new `JavaClass` implementation is required.

### 2.4.4 Indirect-caller audit (1.5)

Every call site listed in 1.5 currently relies on `session.javaSymbolProvider` returning binary
Java classes. After Phase 2 the contract narrows; the audit must update each:

| Caller | Today | After Phase 2 |
|---|---|---|
| `FirJvmConflictsChecker` (`FirJvmConflictsChecker.kt:37`) | `session.javaSymbolProvider.getClassLikeSymbolByClassId(...)` | `session.symbolProvider.getClassLikeSymbolByClassId(...)`, then check `symbol.origin is FirDeclarationOrigin.Java` (source) **or** the deserializer's binary-Java origin. |
| `FirDirectJavaActualDeclarationExtractor` (`FirDirectJavaActualDeclarationExtractor.kt:31-43`) | Same | Same — actualization for a Java *binary* `expect class` actualizer is now satisfied via the deserializer rather than `JavaSymbolProvider`. |
| `KaFirJavaInteroperabilityComponent` (`KaFirJavaInteroperabilityComponent.kt:250`) | LL-only — out of scope (1.6) | Unchanged. LL keeps its own `LLFirJavaSymbolProvider` chain. |
| Lombok `AbstractBuilderGenerator` (`AbstractBuilderGenerator.kt:164, 271`) | Same | Same as `FirJvmConflictsChecker`. The binary builder lookup falls naturally through the deserializer. |

The audit itself is the dominant cost of Phase 2. It is paid once.

### 2.4.5 Why this dissolves the cycle and the abstraction

* **Cycle.** The deserializer no longer asks the facade about binaries. There is no edge from
  `JvmClassFileBasedSymbolProvider` back into `FirJavaFacade.classFinder`, so the F/G/H back-edge
  from §2.3 cannot exist regardless of how `BinaryJavaClassFinder` is wired (it is gone).
* **Abstraction.** `JavaClassFinder` was the universal entry point because PSI had to serve both
  source and binary. Once the deserializer owns binary, the abstraction's only remaining job is
  "find Java classes in source roots" — which is exactly what `JavaClassFinderOverAstImpl` does
  today, scoped to source. `CombinedJavaClassFinder` and `BinaryJavaClassFinder` are removed; the
  factory becomes a *source-finder* factory (Phase 3).

## 2.5 Phase 3 — Source-only PSI/AST switch (transitional, 1–2 releases)

> **Phase 3 is the only place PSI lives in the `java-direct`-enabled compiler.** It is kept as a
> per-source-file fallback behind a flag for **1–2 releases**, then removed entirely. After
> Phase 3 the JVM-FIR compilation path has no `JavaClassFinderImpl`, no `KotlinJavaPsiFacade`, no
> `PsiElementFinder` — the IntelliJ-platform dependency is shed for compilation.

### 2.5.1 What changes

`JavaClassFinderFactory` is re-purposed: it no longer constructs a binary-half closure or a
`CombinedJavaClassFinder`. It produces *the* source `JavaClassFinder` for the configured
`javaSourceRoots`. The flag chooses between two implementations of the same source-only contract:

```kotlin
// compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt — illustrative end-state
class JavaSourceClassFinderFactory(private val configuration: CompilerConfiguration) : JavaClassFinderFactory {
    override fun createJavaClassFinder(
        scope: AbstractProjectFileSearchScope,
        annotationProvider: JavaAnnotationProvider?,
        localFs: VirtualFileSystem,
        defaultFinderProvider: (() -> JavaClassFinder)?,   // stays in the signature; now means "PSI-source"
    ): JavaClassFinder {
        val roots = configuration.javaSourceRoots.mapNotNull(localFs::findFileByPath)
        if (roots.isEmpty()) return EmptyJavaClassFinder    // library session: nothing to find here
        return when (configuration.get(KotlinCompilerConfigurationKeys.JAVA_SOURCE_FINDER)) {
            JavaSourceFinderKind.AST,  null -> JavaClassFinderOverAstImpl(roots)        // default
            JavaSourceFinderKind.PSI         -> defaultFinderProvider!!.invoke()         // legacy PSI-source
        }
    }
}
```

The flag — for example `-P plugin:org.jetbrains.kotlin.java-direct:sourceFinder=psi|ast` —
mirrors the existing `fir.force.javaDirect` style. Default starts at `ast`; `psi` is the safety
fallback for the transition window.

### 2.5.2 Library session

After Phase 2, the library session does not consume `FirJavaFacade.classFinder` for binary
queries; the deserializer owns those. The factory therefore returns `EmptyJavaClassFinder` (or
similar no-op) for the library session — a marker that the source finder has nothing to say
about library scope. `FirJavaFacadeForSource` already gates on its `classFinder` being usable, so
this is a small contract change (or `FirJavaFacade` for library sessions can drop the
`classFinder` field entirely; the choice between "empty finder" and "no finder" is an
implementation detail).

### 2.5.3 What must reach AST/PSI source parity

The two source-only implementations must agree on the full `JavaClassFinder` API for source
classes:

* `findClass(classId)` — same `JavaClass` shape (PSI-backed `JavaClassImpl` vs `JavaElementOverAst`-backed)
  for the same source `.java` file.
* `findPackage(fqName)` — same package presence answer for source roots.
* `knownClassNamesInPackage(fqName)` — same set for a given source package.
* `canComputeKnownClassNamesInPackage()` — both `true`.
* `findClasses(...)` — multi-result. Today only LL exercises this (1.7). If LL is **not** plumbed
  through Phase 3 (it is out of scope; LL has its own chain), `findClasses` parity does not block
  Phase 3, but `JavaClassFinderOverAstImpl` already implements it for interface compliance.

`JavaAnnotationProvider` integration must be wired identically through both implementations so
that `JavaAnnotationOverAst` (AST) and PSI annotations produce the same FIR annotation graph.

### 2.5.4 Removal at the end of the transition

Once the AST source path is green in production for 1–2 releases, the removal step is:

1. Delete the `JavaSourceFinderKind.PSI` branch from `JavaSourceClassFinderFactory`.
2. Delete `JavaClassFinderImpl` (`compiler/frontend.java/src/.../JavaClassFinderImpl.kt`) **only
   if no remaining caller uses it**. K1 frontend usage is non-`java-direct` and may keep PSI for
   longer; LL has its own copy of the wiring; cf. 1.6. Removal is therefore module-scoped: the
   factory's PSI branch and the `defaultFinderProvider` in
   `VfsBasedProjectEnvironment.getFirJavaFacade` are dropped from the JVM-FIR / `java-direct`
   path. Anything downstream of that — and the K1 path — is a follow-up effort.
3. Drop the `defaultFinderProvider` parameter from `JavaClassFinderFactory.createJavaClassFinder`
   if no implementation still uses it. (At this point, `java-direct` is the only registered
   factory.)

This is the milestone that completes shedding the IntelliJ-platform dependency from the JVM-FIR
compilation path.

## 2.6 Phased migration plan (timeline + validation)

### Phase 1 — Land the new binary data source

1. **Introduce `BinaryJavaClassFinder`** in `compiler/java-direct/` without touching the factory
   yet. Gate the wiring behind a `-P fir.javaDirect.binaryFinder=…` property. Default-off.
2. **Wire it into `JavaClassFinderOverAstFactory`** behind that flag. PSI fallback remains as
   `defaultFinderProvider()`. Step is *additive* — no behaviour change when the flag is off.
3. **Validate.** Run `JavaUsingAstBoxTestGenerated` (~1178) and `JavaUsingAstPhasedTestGenerated`
   (~1513) — current baseline 2793/2793 green; see `ITERATION_RESULTS.md`. Plus
   `KotlinFullPipelineTestsGenerated` and a representative `IntelliJFullPipelineTestsGenerated`
   subset. Performance comparison via `PhaseCMeasurementCounters` (`AGENT_INSTRUCTIONS.md` §
   Performance Measurement).
4. **Flip default-on; remove the PSI-binary branch** from `JavaClassFinderOverAstFactory`. Phase 1
   ends here — `BinaryJavaClassFinder` is the binary half of `CombinedJavaClassFinder`. **PSI is
   gone from binary lookups.** The `BinaryJavaClassFinder` is **not** kept in the codebase
   long-term — it is a transient state on the way to Phase 2.

### Phase 2 — Collapse the abstraction

1. **Move binary lookup helpers into `JvmClassFileBasedSymbolProvider`** (the index walks
   prototyped in `BinaryJavaClassFinder` are inlined as `private` helpers or moved to a small
   utility shared between source and library symbol providers).
2. **Replace `javaFacade.hasTopLevelClassOf` / `knownClassNamesInPackage` / `findClass` /
   `hasPackage` calls** in `JvmClassFileBasedSymbolProvider` (callers (E)–(I)) with the new
   helpers.
3. **Make `JavaSymbolProvider` source-only** (callers (A)–(D)). `FirJavaFacade.classFinder`
   becomes the source-only `JavaClassFinderOverAstImpl`.
4. **Audit and re-route the indirect callers** listed in 1.5 (`FirJvmConflictsChecker`,
   `FirDirectJavaActualDeclarationExtractor`, Lombok `AbstractBuilderGenerator`). LL is out of
   scope.
5. **Delete `CombinedJavaClassFinder`** (`compiler/java-direct/src/.../CombinedJavaClassFinder.kt`)
   and `BinaryJavaClassFinder`. Update the test fixture `VfsBasedProjectEnvironmentOverAst` to
   reflect the new contract — the library session no longer takes a `JavaClassFinder` at all.
6. **Validate** the full matrix again (same suites as Phase 1, plus a parallel pass that asserts
   `getTopLevelClassifierNamesInPackage` parity since the union is now produced at the
   composite-symbol-provider level rather than inside the class finder).

### Phase 3 — Source-only PSI/AST switch (transitional)

1. **Re-purpose `JavaClassFinderFactory`** (rename if helpful — e.g.
   `JavaSourceClassFinderFactory`) to produce *only* the source finder.
2. **Add the PSI/AST flag** (`-P …:sourceFinder=psi|ast`, default `ast`). The PSI branch wires
   `JavaClassFinderImpl` scoped to `psiSearchScope` for source files only.
3. **Validate** AST/PSI source parity:
   * Run the existing `JavaUsingAst*TestGenerated` matrix with `sourceFinder=ast`.
   * Run a parallel `JavaUsingPsi*TestGenerated` (or parameterized variant) with
     `sourceFinder=psi` so both legs are exercised under CI.
   * `JavaAnnotationProvider` parity: annotation-graph diff between the two source finders for
     each test.
4. **Stabilize for 1–2 releases.** PSI is available as a customer escape hatch during this
   window. **No long-term residence of `BinaryJavaClassFinder` is required for this** —
   `BinaryJavaClassFinder` was already removed at the end of Phase 2.
5. **Remove PSI** at the end of the transition (§2.5.4): drop the `psi` branch of the flag,
   drop the `defaultFinderProvider` closure from `VfsBasedProjectEnvironment.getFirJavaFacade`,
   and (subject to non-`java-direct` callers) move `JavaClassFinderImpl` itself toward deletion.

### Out of scope for all three phases

* LL-FIR (1.6).
* The K1 frontend's own use of PSI (`JavaClassFinderImpl` survives there until K1 is retired).
* The long-term NIO-based `BinaryKotlinClassFinder` proposed in
  `EXTERNAL_DEPENDENCIES_RESOLUTION_ANALYSIS.md` § 5.2.

## 2.7 Risks and open questions

### Phase 1

* **`BinaryJavaClass` dependency surface.** The class lives in `compiler/frontend.common.jvm`
  and is currently used by K1. Reusing it from `java-direct` requires confirming the transitive
  dependencies of `BinaryJavaClass` and `ClassifierResolutionContext` do not introduce new PSI /
  `KotlinJavaPsiFacade` paths into the `java-direct` runtime classpath. If they do, a thin shim
  may be required.
* **Performance vs PSI.** `KotlinJavaPsiFacade` has aggressive caches (`PsiElementFinder` results
  are interned per-project). `JvmDependenciesIndex` is also cached but at a coarser granularity
  (per-package directory walk). Establish a baseline using `PhaseCMeasurementCounters` before
  flipping the default; expected hot spots are `knownClassNamesInPackage` (called on every
  `hasTopLevelClassOf`) and `findPackage` (called inside `packageCache`). The new finder should
  add no eager I/O — only first-touch traversals.
* **Construction order.** `JvmDependenciesIndex` is built inside `KotlinCoreEnvironment` /
  `jvmCompilerPipeline.kt` before the FIR session is created (so that
  `projectEnvironment.getKotlinClassFinder` already sees it in `FirJvmSessionFactory.kt:98, 260`).
  The new finder can be constructed eagerly inside
  `JavaClassFinderOverAstFactory.createJavaClassFinder` without forcing additional I/O.
* **`FirJKlibSessionFactory`** (`compiler/cli/cli-jklib/src/.../FirJKlibSessionFactory.kt:162-173`)
  uses the same `getFirJavaFacade(...)` seam, so the new finder applies automatically. Its test
  suite must be on the validation list to confirm no JKlib-specific binary class was missed.
* **`ct.sym` behaviour.** `CliVirtualFileFinder.findBinaryOrSigClass` enables
  `BINARY_CLASS_AND_SIG_EXTENSIONS` conditionally on `enableSearchInCtSym`. The new finder must
  mirror that flag verbatim — `.sig` files in `ct.sym` need an ASM read just like `.class`, and
  `BinaryJavaClass` is content-driven (it does not care about the source extension).
* **Union semantics in `CombinedJavaClassFinder.knownClassNamesInPackage`.** Today the union of
  source-AST + PSI returns names that include source-only Java classes the binary deserializer
  has no business loading. The new union of source-AST + index keeps the same shape in Phase 1,
  so no observable change is expected.

### Phase 2

* **Indirect-caller audit (§2.4.4).** Four call sites must be re-routed to the proper provider
  chain. The audit is the dominant cost of Phase 2; mistakes manifest as missing diagnostics
  (`FirJvmConflictsChecker`), failed actualization (`FirDirectJavaActualDeclarationExtractor`),
  or skipped Lombok generation. Test coverage for each must be re-verified.
* **`FirSession.javaSymbolProvider` semantics narrow** to "source-only Java". Any reflective use
  in plugins or downstream tooling that assumed the binary fall-through must be re-evaluated.
* **`getTopLevelClassifierNamesInPackage` union shape moves up the stack.** The composite
  `FirSymbolProvider` (or an explicit composite names-provider) must union source names from
  `JavaSymbolProvider` with binary names from `JvmClassFileBasedSymbolProvider`. Tests that
  asserted on the single composite list must be re-verified.
* **`FirJKlibSessionFactory`.** Mirrors the same wiring; its session graph must reflect the new
  contract (library session has no `FirJavaFacade.classFinder`, source session has source-only).
* **Test fixture parity.** `VfsBasedProjectEnvironmentOverAst` (today: source = AST,
  library = PSI) must be updated so the library session no longer hands a class finder at all.
  This is a small fixture change but a contract-shaping one.

### Phase 3

* **AST/PSI source parity** (§2.5.3) is the gate for flipping the default. Two source-only
  finders must agree on `findClass`, `findPackage`, `knownClassNamesInPackage`,
  `canComputeKnownClassNamesInPackage`, and `JavaAnnotationProvider` integration. This requires
  either parameterized test runs or a parallel `JavaUsingPsi*TestGenerated` suite during the
  1–2 release window.
* **`findClasses` parity.** Currently exercised only by LL (1.7). If LL stays on its own chain,
  parity for `findClasses` does not block Phase 3 — but `JavaClassFinderOverAstImpl.findClasses`
  must continue to satisfy the interface contract.
* **PSI removal blast radius.** Deleting `JavaClassFinderImpl` outright depends on no remaining
  in-tree caller (K1 frontend, LL). Phase 3 only commits to removing the PSI source branch from
  the `java-direct` factory and from `VfsBasedProjectEnvironment.getFirJavaFacade`'s closure.
  Full deletion of `JavaClassFinderImpl` is a follow-up tracked separately.
* **LL-FIR boundary.** LL-FIR is unchanged through all three phases. Tooling that goes through LL
  continues to use PSI; the `java-direct` source-only switch does not propagate there.

## 2.8 Alternatives considered (briefly)

* **Skip Phase 1 — go directly from PSI-binary to Phase 2 structural refactoring.** Considered
  and rejected as the *first* step. Skipping Phase 1 means the binary data source
  (`JvmDependenciesIndex` + `BinaryJavaClass`) is validated under production load *only* after
  the call graph has been restructured — a regression there would be hard to localize. Phase 1
  is a risk-isolation device: the wiring change is small, the surface is observationally
  equivalent, and an A/B flag flip reverts it cleanly. Phase 2 follows immediately after, so the
  intermediate state does not linger; this is *not* the path the previous evaluation rejected.
* **Keep `BinaryJavaClassFinder` long-term.** Considered and rejected per the user's
  clarification on this iteration. Keeping the abstraction would preserve a parallel class-finder
  interface on top of a symbol-provider stack that already knows how to find binary classes;
  Phase 2 is mandatory.
* **Keep PSI as a binary-side fallback for 1–2 releases.** Considered and rejected. The
  transitional fallback role belongs to *source* PSI (Phase 3), not binary PSI. Binary PSI would
  re-introduce a `CombinedJavaClassFinder`-shaped abstraction that Phase 2 was designed to
  remove.
* **NIO-based `BinaryKotlinClassFinder`** (long-term direction, see
  `EXTERNAL_DEPENDENCIES_RESOLUTION_ANALYSIS.md` § 5.2). Fully platform-free but a much larger
  surface change — would also subsume `KotlinClassFinder`. Cited as the eventual destination
  after the IntelliJ-platform dependency is shed; not adopted in this iteration.
* **Keep `CombinedJavaClassFinder` but cache PSI calls more aggressively.** Does not satisfy any
  of the three phases' goals; rejected.
* **Build the binary finder on top of `JvmClassFileBasedSymbolProvider`.** Would create the cycle
  documented in §2.3; rejected. (Phase 2 instead inverts the dependency: the deserializer owns
  the index, not the other way around.)

---

## Appendix A — File/line index of touched code

| File | Description |
|---|---|
| `compiler/cli/cli-jvm/src/.../JvmConfigurationPipelinePhase.kt:97-99` | Unconditional registration of `JavaDirectPluginRegistrar` (language-feature gate commented out). |
| `compiler/cli/cli-base/src/.../CliVirtualFileFinder.kt` | Existing FIR-side index reader; pattern to copy in `BinaryJavaClassFinder`. |
| `compiler/cli/cli-base/src/.../JvmDependenciesIndex.kt:27-101` | The classpath index used by both `CliVirtualFileFinder` and the proposed `BinaryJavaClassFinder`. |
| `compiler/cli/cli-jklib/src/.../FirJKlibSessionFactory.kt:162-173` | JKlib session factory; same `getFirJavaFacade` seam. |
| `compiler/cli/src/.../VfsBasedProjectEnvironment.kt:205-224` | Single injection point for `JavaClassFinderFactory`; the closure that constructs `JavaClassFinderImpl` (PSI). |
| `compiler/fir/checkers/checkers.jvm/src/.../FirJvmConflictsChecker.kt:37` | Indirect PSI consumer via `session.javaSymbolProvider`. |
| `compiler/fir/entrypoint/src/.../AbstractProjectEnvironment.kt:44` | `getKotlinClassFinder(scope)` exposes the existing classpath reader. |
| `compiler/fir/entrypoint/src/.../FirJvmSessionFactory.kt:98, 106, 159-161, 260` | Where `getKotlinClassFinder` and `getFirJavaFacade` are stitched together for source/library sessions. |
| `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt:58-91` | All five accessors that go through `classFinder`. |
| `compiler/fir/fir-jvm/src/.../JavaSymbolProvider.kt:32-77` | Source-session callers (A)–(D). |
| `compiler/fir/fir-jvm/src/.../deserialization/JvmClassFileBasedSymbolProvider.kt:53, 65-117, 139, 169-213` | Library-session callers (E)–(I); also injection point of `kotlinClassFinder`. |
| `compiler/fir/fir2ir/jvm-backend/src/.../FirDirectJavaActualDeclarationExtractor.kt:31-43` | Indirect PSI consumer (direct Java actualization). |
| `compiler/frontend.common.jvm/src/.../load/java/structure/impl/classFiles/BinaryJavaClass.kt:24-259` | ASM-backed `JavaClass`; reused as-is by `BinaryJavaClassFinder`. |
| `compiler/frontend.java/src/.../JavaClassFinderImpl.kt:53` | Sole implementation of `findClasses` used in PSI; not reachable from FIR core. |
| `compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt:18-54` | The factory that today builds `CombinedJavaClassFinder(source, PSI)`. |
| `compiler/java-direct/src/.../CombinedJavaClassFinder.kt:23-87` | Source-first / union semantics. |
| `compiler/java-direct/src/.../JavaClassFinderOverAstImpl.kt:37-141` | Source-only AST finder; gaps documented in 1.7. |
| `compiler/java-direct/testFixtures/.../VfsBasedProjectEnvironmentOverAst.kt:43-55` | Test-fixture wiring (PSI-only library session). |
| `core/deserialization.common.jvm/src/.../KotlinClassFinder.kt:24-46` | `KotlinClassFinder.Result.ClassFileContent` makes the bytes available to `findClass(knownContent)`. |
| `plugins/lombok/lombok.k2/src/.../AbstractBuilderGenerator.kt:164, 271` | Indirect PSI consumer via `session.javaSymbolProvider`. |
| `analysis/analysis-api-fir/src/.../KaFirJavaInteroperabilityComponent.kt:250` | LL-only consumer; out of scope. |
| `analysis/low-level-api-fir/src/.../symbolProviders/LLFirJavaSymbolProvider.kt:31` | LL parallel pipeline; out of scope. |
| `analysis/low-level-api-fir/src/.../symbolProviders/combined/LLCombinedJavaSymbolProvider.kt:41-46, 72, 104-108` | LL parallel pipeline; sole `findClasses` caller in FIR. |

## Appendix B — Cross-references

* [`AGENT_INSTRUCTIONS.md`](../AGENT_INSTRUCTIONS.md) — non-negotiable rules, performance harness, test commands.
* [`implDocs/ARCHITECTURE.md`](ARCHITECTURE.md) — `java-direct` callback patterns and Java Model layout.
* [`implDocs/RESOLUTION_PIPELINE.md`](RESOLUTION_PIPELINE.md) — how `JavaClassFinderOverAstImpl` participates in resolution today.
* [`implDocs/archive/EXTERNAL_DEPENDENCIES_RESOLUTION_ANALYSIS.md`](archive/EXTERNAL_DEPENDENCIES_RESOLUTION_ANALYSIS.md) — earlier pass on the same question; sections 5.1–5.3 are the precursors of this design.
* [`implDocs/archive/rethinking-structure.md`](archive/rethinking-structure.md) — the framing of the chicken-and-egg between `FirJavaFacade` and the binary deserializer.
* [`implDocs/archive/initial-analysis.md`](archive/initial-analysis.md) — first iteration analysis, retained for context only.
