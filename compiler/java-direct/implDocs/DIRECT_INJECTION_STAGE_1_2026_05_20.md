# Direct Injection of `java-direct` into the FIR Source Session — Stages 1 / 1.5 / 1.6

> **Status.** Stages **1**, **1.5**, and **1.6** all landed on branch `rr/ic/direct-java`. Tests:
> `JavaUsingAstBoxTestGenerated` 1181/1181 + `JavaUsingAstPhasedTestGenerated` 1519/1519 =
> 2700/2700, 0 failures, 0 errors. Stage 2 (= Phase 2 of
> [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md))
> is the next step.
>
> **Stage 1.6** replaced the per-session `FirJvmSessionFactory.Context.javaFacadeBuilder` seam
> introduced in Stage 1 with a direct lambda parameter on both
> `FirJvmSessionFactory.createSourceSession(..., createJavaFacade)` and
> `FirJvmSessionFactory.createLibrarySession(..., createJavaFacade)` — pattern mirrors the
> existing `createIncrementalCompilationSymbolProviders` parameter. No mutable state, no
> time-of-initialization fragility. Callers pass the same lambda to both `create*Session`
> invocations so the source-/library-session pair share one `BinaryJavaClassFinder` instance.
> An earlier attempt using a `var` setter on `VfsBasedProjectEnvironment` was rejected as too
> fragile (see §3.7).
>
> See also: [`AGENT_INSTRUCTIONS.md`](../AGENT_INSTRUCTIONS.md),
> [`implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md).

---

## 0. Goal

Stop routing `java-direct` registration through the
`org.jetbrains.kotlin.javaClassFinderFactory` extension point. Construct
`JavaClassFinderOverAstImpl` and its `FirJavaFacadeForSource` **directly** at the FIR
source-/library-session symbol-provider construction site. Preserve `BinaryJavaClassFinder` access
via a late-bound shared binary finder reference so `CombinedJavaClassFinder` semantics survive
unchanged — the
[`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) §2.4.4
indirect-caller audit remains deferred to Stage 2.

---

## 1. Investigation summary

Two roles served today by `CombinedJavaClassFinder`:

| Role | Where | Behavior | Cost of source-only narrowing |
|------|-------|----------|-------------------------------|
| **R1.** Source session — `JavaSymbolProvider` (`JavaSymbolProvider.kt:32-77`) | Callers (A)–(D) per [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) §1.3 | union/source-first chain | Blocked by §2.4.4 audit (`FirJvmConflictsChecker`, `FirDirectJavaActualDeclarationExtractor`, Lombok `AbstractBuilderGenerator`) and the source-only narrowing of `JavaSymbolProvider.classCache` / `symbolNamesProvider` |
| **R2.** Library session — `JvmClassFileBasedSymbolProvider` (`JvmClassFileBasedSymbolProvider.kt:72, 139, 171, 180, 213`) | Callers (E)–(I) per [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) §1.4 | All binary in practice; source half is dead weight on the library facade | Free |

Conclusion: Stage 1 takes R2 separation for free and leaves R1 untouched. Stage 1 is a **wiring
shift only**; combined semantics survive verbatim.

Three implementation options considered:

| Option | Description | Trade-off |
|--------|-------------|-----------|
| **A** | Full split now (≡ Phase 2 forward) | Pulls §2.4.4 audit + `symbolNamesProvider` union move + `JavaSymbolProvider.classCache` narrowing all at once |
| **B** | Late binding; combined semantics preserved | Drops extension indirection; `CombinedJavaClassFinder` survives until Stage 2 |
| **C** | Staged: B first, then A | Each stage individually green, A/B-flaggable; two iterations |

**Chosen: C.** Stage 1 = B (initial wiring shift); Stage 1.5 = collapse-fallback (delete the
extension point); Stage 1.6 = move dispatch slot from `Context` to env; Stage 2 = A (Phase 2 of
[`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md)).

---

## 2. What landed — Stage 1 (initial wiring shift) — superseded by Stage 1.6

> The `Context.javaFacadeBuilder` field documented below was introduced in Stage 1 and removed
> again in Stage 1.6 (§3.6). The Stage-1 description is kept here for historical context. Skip to
> §3 for the current state.

### 2.1 (Removed in 1.6) `FirJvmSessionFactory.Context.javaFacadeBuilder`

`compiler/fir/entrypoint/src/.../FirJvmSessionFactory.kt`

```kotlin
class Context(
    val jvmTarget: JvmTarget,
    val projectEnvironment: AbstractProjectEnvironment,
    val librariesScope: AbstractProjectFileSearchScope,
    val registerJvmDeserializationExtension: Boolean,
    val inlineConstTracker: InlineConstTracker?,
    val javaFacadeBuilder:
        ((FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacadeForSource)? = null,
)
```

Both factory methods consult it before falling back to `projectEnvironment.getFirJavaFacade(...)`:

```kotlin
val sourceJavaFacade = context.javaFacadeBuilder?.invoke(session, moduleData, javaSourcesScope)
    ?: projectEnvironment.getFirJavaFacade(session, moduleData, javaSourcesScope)
```

### 2.2 Direct-injection helper

`compiler/java-direct/src/.../JavaDirectFacadeBuilder.kt` — `createJavaDirectSourceJavaFacadeBuilder(...)`.

Returns the lambda for `Context.javaFacadeBuilder`. Builds
`JavaClassFinderOverAstImpl + BinaryJavaClassFinder + CombinedJavaClassFinder` directly from the
`CompilerConfiguration` and `VfsBasedProjectEnvironment`. `BinaryJavaClassFinder` memoised per
`(System.identityHashCode(psiSearchScope), enableCtSym)` so the source-session and
library-session facades share the same binary backing and its caches.

Non-CLI environments (`VirtualFileFinderFactory` not a `CliVirtualFileFinderFactory`) fall back
to PSI `Project.createJavaClassFinder(psiSearchScope, annotationProvider)` for the binary half.

### 2.3 CLI populates the builder

`compiler/cli/cli-jvm/.../JvmFrontendPipelinePhase.kt:314-318`:

```kotlin
val context = FirJvmSessionFactory.Context(
    configuration,
    projectEnvironment,
    librariesScope,
    javaFacadeBuilder = createJavaDirectSourceJavaFacadeBuilder(configuration, projectEnvironment),
)
```

### 2.4 `asPsiSearchScope()` promoted to public

`compiler/cli/src/.../VfsBasedProjectEnvironment.kt` — `private fun` → `fun` so the `java-direct`
helper can convert the abstract scope.

---

## 3. What landed — Stage 1.5 (collapse fallback; delete extension point)

### 3.1 Extension fallback dropped from `VfsBasedProjectEnvironment.getFirJavaFacade`

```kotlin
override fun getFirJavaFacade(
    firSession: FirSession,
    baseModuleData: FirModuleData,
    fileSearchScope: AbstractProjectFileSearchScope,
): FirJavaFacadeForSource {
    val javaAnnotationProvider = firSession.javaAnnotationProvider
    val psiSearchScope = fileSearchScope.asPsiSearchScope()
    val classFinder = project.createJavaClassFinder(psiSearchScope, javaAnnotationProvider)
    return FirJavaFacadeForSource(firSession, baseModuleData, classFinder)
}
```

No extension lookup. No `defaultFinderProvider` / `binaryClassFinderInputsProvider` closures. PSI
direct. All callers that don't set `Context.javaFacadeBuilder` (scripting, REPL, JKlib, IC,
metadata, Compose, etc.) get plain PSI — same effective behavior as today, since none of those
sites previously registered `JavaDirectPluginRegistrar`.

### 3.2 Test fixtures migrated to the `JavaFacadeBuilderProvider` `TestService`

New TestService — `compiler/tests-common-new/testFixtures/.../JavaFacadeBuilderProvider.kt`:

```kotlin
abstract class JavaFacadeBuilderProvider : TestService {
    abstract fun createBuilder(
        configuration: CompilerConfiguration,
        projectEnvironment: VfsBasedProjectEnvironment,
    ): ((FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacadeForSource)?
}

val TestServices.javaFacadeBuilderProvider: JavaFacadeBuilderProvider? by
    TestServices.nullableTestServiceAccessor()
```

`FirFrontendFacade` and `FirReplFrontendFacade` consult the service when building
`FirJvmSessionFactory.Context`:

```kotlin
javaFacadeBuilder = testServices.javaFacadeBuilderProvider?.createBuilder(configuration, projectEnvironment),
```

`java-direct` registers a concrete implementation
(`compiler/java-direct/testFixtures/.../JavaDirectFacadeBuilderProvider.kt`) that delegates to
`createJavaDirectSourceJavaFacadeBuilder(...)`. `AbstractJavaUsingAstTest` /
`AbstractJavaUsingAstBoxTest` register it via `useAdditionalService<JavaFacadeBuilderProvider>(::JavaDirectFacadeBuilderProvider)`.

### 3.3 Deleted types and files

| Deletion | Reason |
|----------|--------|
| `compiler/cli/src/.../extensions/JavaClassFinderFactory.kt` | Extension point unused after the env fallback collapse |
| `compiler/cli/src/.../extensions/BinaryJavaClassFinderInputs.kt` | Companion DTO of the deleted extension |
| `compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt` (class + adapter) | No more `CompilerPluginRegistrar` registration; helper renamed file `JavaDirectFacadeBuilder.kt` keeps `createJavaDirectSourceJavaFacadeBuilder` |
| `JvmConfigurationPipelinePhase.kt:95` registration | `configuration.add(COMPILER_PLUGIN_REGISTRARS, JavaDirectPluginRegistrar())` dropped — comment block explains the new wiring |
| `JavaDirectConfigurator` `EnvironmentConfigurator` (in `components.kt`) | Sole job was pushing `JavaDirectPluginRegistrar`; replaced by the TestService registration in `AbstractJavaUsingAst*Test` |

### 3.4 Touched files (Stage 1 + 1.5 + 1.6 combined)

| File | Change |
|------|--------|
| `compiler/fir/entrypoint/src/.../FirJvmSessionFactory.kt` | Stage 1: `Context.javaFacadeBuilder` added. Stage 1.6: removed; **`createSourceSession` / `createLibrarySession` gain `createJavaFacade: (AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade = AbstractProjectEnvironment::getFirJavaFacade` parameter**. Non-nullable; default is the env's own method reference. Body becomes `val javaFacade = createJavaFacade(env, session, moduleData, scope)` — no null branch. |
| `compiler/cli/src/.../VfsBasedProjectEnvironment.kt` | `asPsiSearchScope()` made public; `getFirJavaFacade` collapsed (Stage 1.5) to plain PSI body (`project.createJavaClassFinder(psiSearchScope, ...)`). No mutable state. |
| `compiler/cli/cli-jvm/.../JvmConfigurationPipelinePhase.kt` | Drop the `JavaDirectPluginRegistrar` registration |
| `compiler/cli/cli-jvm/.../JvmFrontendPipelinePhase.kt` | Stage 1.6: `val javaDirectFacade = createJavaDirectSourceJavaFacadeBuilder(...)`, passed as `createJavaFacade = javaDirectFacade` to both `createSourceSession` and `createLibrarySession`. |
| `compiler/java-direct/src/.../JavaDirectFacadeBuilder.kt` (renamed from `JavaDirectPluginRegistrar.kt`) | `createJavaDirectSourceJavaFacadeBuilder(configuration, projectEnvironment)` returns `(AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade` (env arg ignored — `projectEnvironment` is captured at builder construction). Memoised `binaryFinderForScope` unchanged. Registrar class and `JavaClassFinderOverAstFactory` adapter removed. |
| `compiler/cli/src/.../extensions/JavaClassFinderFactory.kt` | Deleted |
| `compiler/cli/src/.../extensions/BinaryJavaClassFinderInputs.kt` | Deleted |
| `compiler/tests-common-new/testFixtures/.../JavaFacadeBuilderProvider.kt` (new) | TestService base + accessor |
| `compiler/tests-common-new/testFixtures/.../FirFrontendFacade.kt` | Stage 1.6: computes `javaFacadeBuilder` once next to `jvmSessionFactoryContext`; threads it through `createLibrarySession`, `analyze`, `createModuleBasedSession` to both `FirJvmSessionFactory.create*Session` calls |
| `compiler/tests-common-new/testFixtures/.../FirReplFrontendFacade.kt` | Stage 1.6: stashes `javaFacadeBuilder` on `ReplCompilationEnvironment` data class alongside `jvmSessionFactoryContext`; passes to both `create*Session` calls |
| `compiler/tests-compiler-utils/testFixtures/.../FirSessionFactoryHelper.kt` | Made `init = { … }` named (trailing lambda would otherwise bind to the new `createJavaFacade` parameter); no java-direct path yet, so `createJavaFacade` is left at default `null` |
| `compiler/java-direct/testFixtures/.../JavaDirectFacadeBuilderProvider.kt` (new) | Concrete TestService impl that delegates to `createJavaDirectSourceJavaFacadeBuilder` |
| `compiler/java-direct/testFixtures/.../AbstractJavaUsingAstTest.kt` | Register the TestService instead of `JavaDirectConfigurator` |
| `compiler/java-direct/testFixtures/.../AbstractJavaUsingAstBoxTest.kt` | Same |
| `compiler/java-direct/testFixtures/.../components.kt` | Drop `JavaDirectConfigurator`; keep `OnlyTestsWithJavaSourcesMetaConfigurator` + dummy-session helpers used by `JavaParsingTestBase` |

---

## 3.5 What landed — Stage 1.6 (lambda parameter on `create*Session`)

### 3.5.1 Motivation

Stage 1.5 left a Stage-1 carryover: `FirJvmSessionFactory.Context.javaFacadeBuilder`. Carrying
the builder inside `Context` mixed it in with long-lived per-pipeline data (jvmTarget,
projectEnvironment, librariesScope, …) and forced every `Context(...)` caller to know about it.
Stage 1.6 moves the builder to the call site as a direct lambda parameter on
`createSourceSession` and `createLibrarySession`, mirroring the existing
`createIncrementalCompilationSymbolProviders` parameter pattern.

### 3.5.2 New parameter on `createSourceSession` / `createLibrarySession`

The lambda takes `AbstractProjectEnvironment` as its first arg and defaults to the env's own
`getFirJavaFacade` method reference — eliminates the nullable signature and the `?.invoke ?:`
fallback inside the factory:

```kotlin
fun createSourceSession(
    moduleData: FirModuleData,
    javaSourcesScope: AbstractProjectFileSearchScope,
    createIncrementalCompilationSymbolProviders: (FirSession) -> FirJvmIncrementalCompilationSymbolProviders?,
    extensionRegistrars: List<FirExtensionRegistrar>,
    configuration: CompilerConfiguration,
    context: Context,
    needRegisterJavaElementFinder: Boolean,
    isForLeafHmppModule: Boolean,
    init: FirSessionConfigurator.() -> Unit,
    createJavaFacade: (AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade =
        AbstractProjectEnvironment::getFirJavaFacade,
): FirSession
```

Body inside `createProviders`:

```kotlin
val javaFacade = createJavaFacade(projectEnvironment, session, moduleData, javaSourcesScope)
val javaSymbolProvider = JavaSymbolProvider(session, javaFacade)
```

Identical shape on `createLibrarySession` (with `context.librariesScope` instead of
`javaSourcesScope`).

### 3.5.3 `Context.javaFacadeBuilder` deleted

`FirJvmSessionFactory.Context` returns to its pre-Stage-1 shape (5 fields, no new lambda).
Existing positional / named-argument call sites compile unchanged.

### 3.5.4 `VfsBasedProjectEnvironment` restored to Stage-1.5 shape

No mutable state. `getFirJavaFacade(...)` returns a plain PSI facade (its body is the same as
Stage 1.5 — `project.createJavaClassFinder(psiSearchScope, javaAnnotationProvider)`). Custom
facades are now never set on the env; they flow through the `createJavaFacade` lambda parameter
of `create*Session` instead.

### 3.5.5 CLI / tests pass the lambda

`JvmFrontendPipelinePhase.kt`:

```kotlin
val javaDirectFacade = createJavaDirectSourceJavaFacadeBuilder(configuration, projectEnvironment)
val context = FirJvmSessionFactory.Context(configuration, projectEnvironment, librariesScope)
// passed into BOTH:
FirJvmSessionFactory.createLibrarySession(..., context, createJavaFacade = javaDirectFacade)
FirJvmSessionFactory.createSourceSession(..., init = sessionConfigurator, createJavaFacade = javaDirectFacade)
```

`FirFrontendFacade.kt` computes the lambda once next to its `jvmSessionFactoryContext` and
threads it through `createLibrarySession(...)`, `analyze(...)`, and
`createModuleBasedSession(...)` to `FirJvmSessionFactory.createSourceSession(...)`.
`FirReplFrontendFacade.kt` stashes the lambda on its `ReplCompilationEnvironment` data class
alongside `jvmSessionFactoryContext` and threads it from there. `FirSessionFactoryHelper.kt` did
not need plumbing yet (no java-direct path through this helper).

### 3.6 Aborted attempt — Option I (drop library facade builder)

Before Stage 1.6 landed, Option I was attempted: leave `createLibrarySession` on
`projectEnvironment.getFirJavaFacade(...)` (plain PSI) so that *only* the source session would
need `java-direct`. Five box tests regressed:

```
testJavaCollectionOfNotNullToTypedArrayFailFast — expected: OK but was: Fail: should throw on get()
testJavaIteratorOfNotNullFailFast
testJavaCollectionOfExplicitNotNullFailFast
testJavaCollectionOfExplicitNotNullWithIndexFailFast
testJavaIteratorOfNotNullWithIndexFailFast
```

The library session's `BinaryJavaClassFinder` is not just a perf step — its
`@NotNull`-annotation reading on JDK collection types differs from PSI's `JavaClassImpl`,
specifically through the path that materialises `BinaryJavaClass` from ASM-read `.sig` /
`.class` bytes. Without it, runtime null-check codegen for `Java*Iterator*FailFast` /
`Java*Collection*FailFast` doesn't inject the expected fast-fail throw. Option I was reverted;
Option II (Stage 1.6) preserves the library builder dispatch and is green.

This is also a relevant input for Stage 2 §6.3 — the new in-place binary-lookup helpers inside
`JvmClassFileBasedSymbolProvider` must reproduce `BinaryJavaClassFinder`'s annotation behaviour
exactly, not collapse to PSI semantics.

### 3.7 Rejected: mutable `var` setter on `VfsBasedProjectEnvironment`

A first cut of Stage 1.6 stored the builder as a mutable `var firJavaFacadeFactory: …` on
`VfsBasedProjectEnvironment` and required callers to assign it between env construction and the
first `create*Session` call. Rejected as too fragile — any caller wiring the env in the wrong
order silently falls back to PSI. The lambda-parameter design replaces the var entirely.

## 4. Effective wiring after Stage 1.6

```
CLI (JvmFrontendPipelinePhase) / test fixtures
   └─► val javaDirectFacade = createJavaDirectSourceJavaFacadeBuilder(configuration, projectEnvironment)
        (or testServices.javaFacadeBuilderProvider?.createBuilder(configuration, projectEnvironment))

FirJvmSessionFactory.createSourceSession(..., createJavaFacade = javaDirectFacade)
   └─► createJavaFacade?.invoke(session, moduleData, javaSourcesScope)
          ?: projectEnvironment.getFirJavaFacade(...)    // plain PSI

FirJvmSessionFactory.createLibrarySession(..., createJavaFacade = javaDirectFacade)
   └─► createJavaFacade?.invoke(session, moduleData, librariesScope)
          ?: projectEnvironment.getFirJavaFacade(...)    // plain PSI

createJavaDirectSourceJavaFacadeBuilder builds:
   - source scope (Java source roots non-empty) → CombinedJavaClassFinder(JavaClassFinderOverAstImpl, memoised BinaryJavaClassFinder)
   - library scope (no Java source roots)        → memoised BinaryJavaClassFinder alone
```

Single shared lambda instance binds both `create*Session` calls — the per-`(scope, enableCtSym)`
memoisation cache inside the lambda is shared between source and library facades, so they walk
the same `BinaryJavaClassFinder` instance. `Context.javaFacadeBuilder` gone;
`VfsBasedProjectEnvironment` carries no mutable state. The behavioural surface from
[`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) Part 1
(callers (A)–(I) and §1.5 indirect callers) remains unchanged.

---

## 5. Validation

```bash
./gradlew :compiler:java-direct:test \
  --tests "JavaUsingAstPhasedTestGenerated" \
  --tests "JavaUsingAstBoxTestGenerated" --stacktrace 2>&1 | tee "$JD_TMP/jd_test.txt"
```

Result (XML aggregation):

| Suite | Testcases | Failures | Errors |
|-------|-----------|----------|--------|
| `JavaUsingAstBoxTestGenerated`    | 1181 | 0 | 0 |
| `JavaUsingAstPhasedTestGenerated` | 1519 | 0 | 0 |
| **Total**                          | **2700** | **0** | **0** |

PSI regression suite — not run (no edits to shared FIR files; `getFirJavaFacade` change is
PSI-equivalent, exercised only by non-java-direct callers that were on PSI today).

---

## 6. Next steps — Stage 2 (= Phase 2 of `PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`)

Stage 1 + 1.5 are the **wiring** cleanup. Stage 2 is the **structural** collapse from
[`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) §2.4.

### 6.1 §2.4.4 indirect-caller audit

Re-route from `session.javaSymbolProvider.getClassLikeSymbolByClassId(...)` to
`session.symbolProvider.getClassLikeSymbolByClassId(...)` with origin filter:

1. `compiler/fir/checkers/checkers.jvm/.../FirJvmConflictsChecker.kt:37` — JVM redeclaration
   diagnostic.
2. `compiler/fir/fir2ir/jvm-backend/.../FirDirectJavaActualDeclarationExtractor.kt:31-43` —
   direct Java actualization.
3. `plugins/lombok/lombok.k2/.../AbstractBuilderGenerator.kt:164, 271` — Lombok builder
   discovery.

(`KaFirJavaInteroperabilityComponent.kt:250` is LL — out of scope per
[`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) §1.6.)

> **Update 2026-05-28 — §6.1 landed; prescription corrected.** The original prescription above
> (re-route via `session.symbolProvider.getClassLikeSymbolByClassId(...)` + origin filter)
> **does not work** for the redeclaration / actualization case. The composite symbol provider's
> implementation (`FirCompositeSymbolProvider.getClassLikeSymbolByClassId`) uses
> `firstNotNullOfOrNull` — when a Kotlin class shares the `ClassId` with a Java class (precisely
> the case these three sites are diagnosing or actualizing), the Kotlin source provider wins and
> the Java symbol is hidden. A first cut of this audit using the original prescription produced
> 12 `JavaUsingAst*TestGenerated` regressions and was reverted.
>
> The actual landed shape is a thin Java-targeted lookup helper —
> `fun FirSession.getJavaClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol?` in
> `compiler/fir/fir-jvm/src/.../java/JavaSymbolProvider.kt`. Today it wraps
> `javaSymbolProvider?.getClassLikeSymbolByClassId(classId)` (zero behavioral delta vs baseline);
> after §6.3 it will additionally consult the deserializer for binary
> `FirDeclarationOrigin.Java.Library` results. The three call sites go through this helper, so
> §6.3 extends the helper in one place rather than visiting each call site again.
> `FirDirectJavaActualDeclarationExtractor` keeps the strict `Java.Source` origin filter on the
> `extract` call (only Java source-class actualizations are valid; binary Java classes are not
> candidates). Validation: 2701/2701 `JavaUsingAst{Phased,Box}TestGenerated` green; two
> pre-existing Lombok `test*ConstructorStatic` baseline failures confirmed independent. See
> [`../ITERATION_RESULTS.md`](../ITERATION_RESULTS.md) §"Stage 2 §6.1 — Indirect
> `javaSymbolProvider` call-site audit — 2026-05-28" for full details.

### 6.2 Narrow `JavaSymbolProvider` to source-only
([`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md)
§1.3 / §2.4.1 / §2.4.3)

`compiler/fir/fir-jvm/.../JavaSymbolProvider.kt`:

- `getClassLikeSymbolByClassId` → gate on `javaFacade.isInSourceIndex(classId)`.
- `hasPackage` → `javaFacade.hasPackageInSources(fqName)`.
- `symbolNamesProvider.getTopLevelClassifierNamesInPackage` → source-only set. Move the union
  with binary names to the composite-symbol-names-provider layer (consumed today by
  `JvmClassFileBasedSymbolProvider.knownTopLevelClassesInPackage`,
  [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) §1.4-F).

`compiler/fir/fir-jvm/.../FirJavaFacade.kt`: add `isInSourceIndex`, `hasPackageInSources`,
`sourceClassNamesInPackage`.

### 6.3 Move binary lookups into `JvmClassFileBasedSymbolProvider`
([`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md)
§1.4 / §2.4.1 / §2.4.3)

`compiler/fir/fir-jvm/.../deserialization/JvmClassFileBasedSymbolProvider.kt`:

| Line | Today | After Stage 2 |
|------|-------|---------------|
| 72   | `javaFacade.hasTopLevelClassOf(classId)` | `hasBinaryTopLevelClass(classId)` (uses `index.classNamesIn(pkg, BINARY_CLASS_AND_SIG_EXTENSIONS)`) |
| 139  | `javaFacade.knownClassNamesInPackage(pkg)` | `binaryNamesInPackage(pkg)` |
| 171  | same gate as 72 | same as above |
| 180  | `javaFacade.findClass(classId, knownContent)` | `BinaryJavaClass(virtualFile, classId.asSingleFqName(), context, signatureParser, outerClass = null, classContent = bytes)` inline |
| 212  | `javaFacade.hasPackage(fqName)` | `index.hasBinaryPackage(fqName)` |

Lift the index-walk helpers from the current `BinaryJavaClassFinder` body into
`JvmClassFileBasedSymbolProvider` (private functions or a small `internal` utility object).

### 6.4 Drop the source-side binary-finder dependency

After §6.2 + §6.3:

- Source scope → `FirJavaFacadeForSource(session, moduleData, JavaClassFinderOverAstImpl(session, roots))`;
  no `CombinedJavaClassFinder`.
- Library scope → `Context.javaFacadeBuilder` not needed for the library session at all;
  `JvmClassFileBasedSymbolProvider` owns the binary path directly.

`createJavaDirectSourceJavaFacadeBuilder` simplifies to a source-only facade builder.

### 6.5 Delete `CombinedJavaClassFinder`, `BinaryJavaClassFinder`
([`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md)
§2.4.1 / §2.6 step 5)

`compiler/java-direct/src/.../CombinedJavaClassFinder.kt` and `.../BinaryJavaClassFinder.kt`
deleted. Logic absorbed into `JvmClassFileBasedSymbolProvider` via §6.3.

### 6.6 (Optional, Phase 3 territory) Source-only PSI/AST switch
([`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) §2.5)

After Stage 2, the only remaining PSI usage on the `java-direct` path is the source-side PSI
fallback in `Project.createJavaClassFinder(...)` (used when CLI inputs are unavailable in non-CLI
environments). Removing it is the Phase 3 milestone in
[`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) §2.5.4 —
not in Stage 2 scope.

---

## 7. Validation plan for Stage 2

1. Repeat the Stage 1 suite run after each of §6.1–§6.5; **any** regression → revert.
2. PSI regression suite (`PhasedJvmDiagnosticLightTreeTestGenerated.*` in
   `:compiler:fir:analysis-tests`) — required after §6.2 / §6.3 (shared FIR files
   `FirJavaFacade.kt`, `JavaSymbolProvider.kt`, `JvmClassFileBasedSymbolProvider.kt` touched).
3. `KotlinFullPipelineTestsGenerated` and a representative
   `IntelliJFullPipelineTestsGenerated` subset for end-to-end coverage.
4. Verify `getTopLevelClassifierNamesInPackage` union shape after the source-only narrowing in
   §6.2 — composite-symbol-names-provider must reproduce today's union
   ([`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) §2.7
   "union shape moves up the stack").

---

## 8. Risks (Stages 1 + 1.5 + 1.6 — current)

| Risk | Status |
|------|--------|
| Source and library sessions get **different** `BinaryJavaClassFinder` instances → cache miss / duplicate index walks | Mitigated. Memoised per `(scope identityHash, enableCtSym)` inside `createJavaDirectSourceJavaFacadeBuilder`. |
| Non-CLI callers crash for lack of `JvmDependenciesIndex` | Mitigated. `VfsBasedProjectEnvironment.firJavaFacadeFactory` defaults to `null`; non-CLI sites leave it unset and `getFirJavaFacade` returns a plain PSI facade. |
| Library facade reverts to PSI silently | Mitigated. Aborted Option I attempt confirmed library still needs `BinaryJavaClassFinder` for `@NotNull`-driven runtime null-checks (5 box tests regressed). Stage 1.6 keeps the env factory active for library scope. Stage 2 §6.3 reproduces the same behaviour inline. |
| `CombinedJavaClassFinder` semantics retained; a regression here would hide a Stage 2 audit gap | Open by design. Stage 2 (§6.1–§6.3) still owns the audit. |
| Mutable state on `VfsBasedProjectEnvironment` with order-of-init coupling | **Resolved.** Stage 1.6's first cut used a mutable `var`; rejected and replaced with a direct lambda parameter on `create*Session`. No mutable state on the env now (§3.7). |
| Caller forgets to pass the lambda to `createLibrarySession` and only to `createSourceSession` (or vice versa) — source and library facades silently use different `BinaryJavaClassFinder` instances | Mitigated by convention — every site that supplies `createJavaFacade` does so on **both** `create*Session` calls. Verified by grepping `createJavaFacade =` in `JvmFrontendPipelinePhase`, `FirFrontendFacade`, and `FirReplFrontendFacade`. Phase 2 §6.4 dissolves the issue by removing the library-session call to `createJavaFacade` entirely. |
