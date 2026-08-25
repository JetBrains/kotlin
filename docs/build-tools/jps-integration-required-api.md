# What JPS needs from the Build Tools API

Inventory of the compiler-side entities the Kotlin JPS builder relies on today, and their current status in the
Build Tools API (BTA). It is the input for a BTA "JPS mode" that lets `jps/jps-plugin` go through BTA without
changing JPS behaviour.

Scope: JVM only. `KotlinUnsupportedModuleBuildTarget` disables every other platform in JPS.

Status legend: **exposed** — usable from `kotlin-build-tools-api` today; **partial** — exists but in the wrong module,
the wrong shape, or not on every execution path; **missing** — no BTA equivalent.

## TL;DR

Everything that would have to become new BTA **API** surface. Entities already exposed, and JPS-side plumbing that BTA
replaces rather than exposes, are not listed here.

**Trackers (`Services` entries)**

1. `ExpectActualTracker` — `report(expectedFile, actualFile)`, `reportExpectOfLenientStub(expectedFile)`.
   Place on **`BaseCompilationOperation`** — JS already consumes it.
2. `InlineConstTracker` — `report(filePath, owner, name, constType)`; plus `ConstantRef` unless JPS aggregates itself.
   Place on **`JvmCompilationOperation`**.
3. `EnumWhenTracker` — `report(whenExpressionFilePath, enumClassFqName)`.
   Place on **`JvmCompilationOperation`**.
4. `ImportTracker` — exists as `CompilerImportTracker` in the impl module only; must move into `kotlin-build-tools-api`.
   Place on **`JvmCompilationOperation`** (closest call — see [API placement](#api-placement-base-versus-jvm-operation)).
5. Pull-based cancellation — a `checkCanceled()` equivalent of `CompilationCanceledStatus`; BTA only has push `cancel()`.
   Place on **`BaseCompilationOperation`**, next to the existing `cancel()`.

   Plus the enabling work, not a new type: **daemon transport for all of the above**. Today only `LOOKUP_TRACKER`
   crosses the daemon boundary; the rest are wired only on the in-process non-IC path, and JPS runs on the daemon.

**Multi-module chunk compilation**

6. A chunk/multi-module compilation operation, or an exemption for `-Xbuild-file` (restricted: warning 2.4.0, error
   2.6.0, KT-75540). Needs per-module output dir, friend dirs, common sources, java source roots and modular JDK root —
   i.e. `KotlinModuleXmlBuilder`, `TargetId`, `JvmSourceRoot` in some exposed form.

**Compilation outputs**

7. Output-item reporting — expose the existing **`ICFileMappingTracker`** rather than inventing a new listener; it
   already carries `recordSourceFilesToOutputFileMapping(sourceFiles, outputFile)`. JPS additionally needs, for JVM
   classes, the class name, class-header kind (multifile facade versus part) and bytes. BTA's operation result is
   currently only a `CompilationResult` enum.

**Incremental caches the compiler reads from**

8. A caller-supplied incremental cache seam — `IncrementalCompilationComponents` plus the compiler-facing
   `IncrementalCache` (`getObsoletePackageParts`, `getObsoleteMultifileClasses`, `getStableMultifileFacadeParts`,
   `getPackagePartData` / `JvmPackagePartProto`, `getModuleMappingData`, `getMetadata`, `getClassFilePath`).
   BTA's incremental compilation owns its caches internally; JPS owns its own.

**Diagnostics**

9. Structured diagnostic listener — severity, message and source location delivered as data rather than a rendered
   string, covering the full severity set JPS branches on (`INFO`, `WARNING`, `STRONG_WARNING`, `FIXED_WARNING`,
   `ERROR`, `EXCEPTION`, `LOGGING`), so JPS can build `CompilerMessage(kind, path, line, column)` and collect
   files-with-errors.

**One-off service**

10. `classesFqNamesByFiles(files): Set<String>` — detects classes that vanished from dirty or removed files.

**Conditional — only if the embedded `kotlin-build-common` is dropped**

11. IC machinery JPS drives itself: `IncrementalCacheCommon`, `IncrementalJvmCache`, `updateIncrementalCache`,
    `ChangesCollector` + `getChangedAndImpactedSymbols`, `LookupStorage` / `LookupSymbol`,
    `IncrementalCompilationContext`, `FileToPathConverter` / `RelativeFileToPathConverter`, `ICReporterBase`,
    `BuildMetaInfo` / `JvmBuildMetaInfo`, cache-version types (`JvmBytecodeBinaryVersion`, `MetadataVersion`).

Minimum for a behaviour-preserving JPS mode: **1-8**. Items **9-10** for exact parity of diagnostics reporting and
dirty-set computation. Item **11** only if JPS is to depend on BTA alone.

## 1. Trackers registered into `Services`

Built in `KotlinBuilder.createCompileEnvironment` (`jps/jps-plugin/src/org/jetbrains/kotlin/jps/build/KotlinBuilder.kt:599`)
and registered in `KotlinModuleBuildTarget.makeServices`
(`jps/jps-plugin/src/org/jetbrains/kotlin/jps/targets/KotlinModuleBuildTarget.kt:269`).

| Tracker | Status | Placement | Notes |
| --- | --- | --- | --- |
| `LookupTracker` | exposed | base (already) | `BaseCompilationOperation.LOOKUP_TRACKER` / `CompilerLookupTracker` (since 2.3.0). JPS consumes `LookupTrackerImpl.lookups` + `pathInterner.values` wholesale (`KotlinBuilder.kt:686`) and again in `LookupUsageRegistrar` for `jps.use.dependency.graph`. The callback shape suffices only if JPS aggregates itself; note the `is LookupTrackerImpl` assertion at `KotlinBuilder.kt:681`. |
| `ImportTracker` | partial | jvm | `CompilerImportTracker` + `BaseCompilationOperationImpl.IMPORT_TRACKER` live in the impl module only, and are wired only on the in-process non-IC path. Needs promotion to the API module and daemon support. |
| `InlineConstTracker` | missing | jvm | `report(filePath, owner, name, constType)`. JPS reads the aggregated `inlineConstMap` and needs `ConstantRef(owner, name, constType)`, unless it aggregates itself (`KotlinJvmModuleBuildTarget.kt:427`). |
| `EnumWhenTracker` | missing | jvm | `report(whenExpressionFilePath, enumClassFqName)`. |
| `ExpectActualTracker` | missing | base | `report(expectedFile, actualFile)` and `reportExpectOfLenientStub(expectedFile)`; consumed by `jpsIncrementalCache.updateComplementaryFiles`. |
| `CompilationCanceledStatus` | partial | base | BTA offers push-based `CancellableBuildOperation.cancel()`; JPS registers a pull-based `checkCanceled()` polling `context.cancelStatus`. |

**Cross-cutting blocker.** In BTA today only `LOOKUP_TRACKER` survives the daemon boundary (forwarded as
`ReportCategory.COMPILER_LOOKUP` in `BtaCompilerServicesWithResultsFacade`). The other trackers are registered only in
`compileInProcessWithoutIc`. JPS runs on the daemon by default (`CompilerMode.JPS_COMPILER`), so exposing the interfaces
is not enough — the daemon transport has to carry them, the way `CompilerCallbackServicesFacadeServer` does today
(`jps/jps-plugin/src/org/jetbrains/kotlin/compilerRunner/JpsCompilerServicesFacadeImpl.kt`).

### API placement: base versus JVM operation

JPS is JVM-only, but that alone does not decide where each option belongs. What decides it is whether the non-JVM
pipelines feed the tracker at all.

At the FIR level, lookup, enum-when and import tracking are already platform-neutral:
`FirAbstractSessionFactory.createSourceSession` is shared by every platform session factory and calls
`registerResolveComponents(..., configuration.lookupTracker, configuration.enumWhenTracker, configuration.importTracker, ...)`,
whose doc comment reads "Resolve components which are same on all platforms"
(`compiler/fir/entrypoint/src/org/jetbrains/kotlin/fir/session/FirAbstractSessionFactory.kt:264`).

`InlineConstTracker` is the exception. `registerCommonComponents` installs the no-op
`FirInlineConstTrackerComponent.Default`, and the real component arrives only through
`registerJavaComponents(inlineConstTracker = ...)`, called only from `FirJvmSessionFactory`. On the IR side
`IrConstFieldInliner.reportOnIr` returns early unless `field.origin == IR_EXTERNAL_JAVA_DECLARATION_STUB`, and
`Fir2IrConfiguration.forKlibCompilation` hardcodes `inlineConstTracker = null`.

`ExpectActualTracker` is genuinely platform-neutral: it is consumed by `IrActualizer` / `ExpectActualCollector` in
`compiler/ir/ir.actualization`, and `Fir2IrConfiguration` passes it through for JVM, JKlib and klib compilation alike.

The decisive difference is in the CLI configuration phases — which trackers each pipeline actually reads out of
`Services`:

| Tracker | JVM (`JvmConfigurationPipelinePhase.kt:118`) | JS / Wasm (`WebConfigurationPhase.kt:198`) | Metadata (`MetadataConfigurationPipelinePhase.kt`) |
| --- | --- | --- | --- |
| `LookupTracker` | yes | yes | no |
| `ExpectActualTracker` | yes (IC only) | yes | no |
| `ICFileMappingTracker` | yes (IC only) | yes | no |
| `ImportTracker` | yes | no | no |
| `EnumWhenTracker` | yes (IC only) | no | no |
| `InlineConstTracker` | yes (IC only) | no | no |

Two details behind the table:

- "IC only" means `JvmConfigurationPipelinePhase.setupIncrementalCompilationServices` reads the tracker only when
  `incrementalCompilationIsEnabled(arguments)` holds — that is, `arguments.incrementalCompilation` or
  `IncrementalCompilation.isEnabledForJvm()`. Only `LookupTracker` and `ImportTracker` are read unconditionally
  ("used by Build Tools API in non-incremental compilations").
- The metadata pipeline reads no trackers at all. `MetadataConfigurationUpdater.fillConfiguration` performs no
  `services[...]` lookups and is not even handed a `Services`. Only two places in the compiler transfer `LookupTracker`
  from `Services` into the `CompilerConfiguration`: `JvmConfigurationPipelinePhase.kt:118` and
  `WebConfigurationPhase.kt:201`.

**Rules of thumb.**

- `ExpectActualTracker` goes on `BaseCompilationOperation`, next to `LOOKUP_TRACKER`. JS consumes it today, so
  putting it on the JVM operation would be factually wrong and would force a breaking move later. Note it is honoured
  by two of the three pipelines: metadata compilation ignores it, which is awkward given metadata compilation is the
  common-source compilation where expect/actual lives.
- `InlineConstTracker` goes on `JvmCompilationOperation`. It is definitionally about Java constants inlined into
  Kotlin, and klib compilation nulls it out. There is no plausible JS meaning.
- `EnumWhenTracker` and `ImportTracker` go on `JvmCompilationOperation`. The mechanism is platform-neutral, but the
  purpose is not: both exist to catch Java-source dependencies (their own KDoc says "track Java enum classes used in
  Kotlin when expressions" and "e.g. Removing of Java file, that Kotlin relies on by importing"). JS, Wasm and Native
  have no Java sources.

**What base placement actually means today.** `GENERATE_COMPILER_REF_INDEX` sits on `BaseCompilationOperation` and is
genuinely honoured by `JvmCompilationOperationImpl`, `JsKlibCompilationOperationImpl` and
`WasmKlibCompilationOperationImpl`. But `LOOKUP_TRACKER` — declared on the same interface — is a silent no-op for
metadata compilation (see the follow-up below). So base placement in BTA currently means "declared for every
operation", not "honoured by every operation".

Do not rely on the declaration site to convey platform applicability. **Whichever interface an option lands on, its
KDoc should state explicitly which platforms honour it**, and an option that cannot work for a given operation should
ideally be rejected rather than silently ignored.

**Caveats.**

- `ImportTracker` is the weakest case for a JVM-only placement. It records all imports, not only Java ones, and BTA
  currently keeps it on `BaseCompilationOperationImpl` (internal, so not a contract yet). Base plus two lines in
  `WebConfigurationPhase` would be defensible if JS incremental compilation is ever expected to want import edges.
- The direction of regret is asymmetric. Base to JVM and JVM to base are both breaking for consumers, but shipping on
  base and never wiring JS means promising something that is not delivered, which is harder to walk back. Under
  `@ExperimentalBuildToolsApi` neither is catastrophic.

**Follow-up: metadata compilation silently discards base trackers.** `KotlinMetadataKlibCompilationOperation` extends
`BaseCompilationOperation`, so `LOOKUP_TRACKER` is settable on it, and `KotlinMetadataKlibCompilationOperationImpl`
(extending `BaseCompilationOperationImpl`) honours it on its own side: `compileInProcessWithoutIc` registers a
`LookupTrackerAdapter` into `Services`, and the daemon path adds `ReportCategory.COMPILER_LOOKUP` to `reportCategories`
whenever the option is non-null. Nothing on the metadata side ever reads it back — `configuration.lookupTracker` stays
null, `registerResolveComponents` receives `lookupTracker = null`, no `FirLookupTrackerComponent` is registered, and no
lookups are recorded. The same holds for `ExpectActualTracker`.

This is out of scope for the JPS work but worth its own issue: either wire the trackers through
`MetadataConfigurationUpdater` (the FIR side already handles them, since `createSourceSession` is shared across all
platform session factories), or make the metadata operation reject options it cannot honour.

## 2. Chunk compilation via `module.xml`

`KotlinJvmModuleBuildTarget.generateChunkModuleDescription` builds a `KotlinModuleXmlBuilder` file and passes it as
`-Xbuild-file` with `destination = null`. A single compiler invocation covers a whole — possibly circular — module chunk,
carrying per-module output dir, friend dirs, common/cross-compiled sources, java source roots, modular JDK root and
classpath roots.

`-Xbuild-file` is explicitly restricted in BTA
(`compiler/build-tools/kotlin-build-tools-generator/src/org/jetbrains/kotlin/buildtools/generator/argumentTransforms.kt:116`
— warning since 2.4.0, error since 2.6.0, "breaks incremental compilation, KT-75540"). Either the JPS mode gets an
exemption, or BTA grows a multi-module compilation operation.

Entities involved: `KotlinModuleXmlBuilder`, `TargetId`, `JvmSourceRoot`.

## 3. Compilation outputs

BTA returns only a `CompilationResult` enum. JPS consumes the full output-item stream:

- `OutputItemsCollector` / `SimpleOutputItem(sourceFiles, outputFile)` -> `GeneratedFile`, `GeneratedJvmClass`
- `GeneratedJvmClass.outputClass` -> `LocalFileKotlinClass`, `JvmClassName`, `KotlinClassHeader.Kind`
  (multifile facade/part detection), `fileContents`
- `MetadataVersion.INSTANCE`, required by `toGeneratedFile`

Used for `OutputConsumer.registerOutputFile` / `registerCompiledClass` (instrumentation), output-to-target attribution,
`markDirtyComplementaryMultifileClasses`, and feeding the incremental caches.

**The compiler already has a service for this: `ICFileMappingTracker`.** Its
`recordSourceFilesToOutputFileMapping(sourceFiles, outputFile)` is invoked from `OutputFileCollection.writeAll`
(`compiler/cli/src/org/jetbrains/kotlin/cli/common/output/outputUtils.kt:57`), and BTA's own
`IncrementalJvmCompilerRunnerBase` already uses it. Both the JVM and the Web configuration phases read it out of
`Services`.

JPS is on the *legacy* channel instead: the daemon formats output messages with `OutputMessageUtil.formatOutputMessage`
and JPS parses them back into `OutputItemsCollectorImpl` (`JpsCompilerServicesFacadeImpl.report` and
`processCompilerOutput`). The information is the same.

So this item does not need a new `OUTPUT_ITEM_LISTENER` option — it needs `ICFileMappingTracker` exposed. The tracker
also reports plugin-related mappings (`recordSourceReferencedByCompilerPlugin`, `recordOutputFileGeneratedForPlugin`,
`recordSourceFileGeneratedForPlugin`), which JPS does not currently consume.

Still genuinely missing: the JVM class detail JPS needs on top of the path mapping — class name, class-header kind and
bytes — which today it obtains by reading the written class files itself.

## 4. Compiler-facing incremental caches

`IncrementalCompilationComponents` plus `org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache`:
`getObsoletePackageParts`, `getObsoleteMultifileClasses`, `getStableMultifileFacadeParts`, `getPackagePartData`
(+ `JvmPackagePartProto`), `getModuleMappingData`, `getMetadata`, `getClassFilePath`.

JPS owns these caches (`JpsIncrementalJvmCache`) and hands them to the compiler per target. BTA's incremental compilation
is snapshot-based and owns its caches internally, so there is no seam for a caller-supplied cache. **Missing**, and
unavoidable for a behaviour-preserving JPS.

### What consumer-managed incremental compilation requires

`JvmConfigurationPipelinePhase.setupIncrementalCompilationServices` is the objective definition of "required for
incremental compilation" — it is where `Services` entries are transferred into the `CompilerConfiguration`.

These are grouped into `JvmClientManagedIncrementalCompilationConfiguration`, set through
`JvmCompilationOperation.INCREMENTAL_COMPILATION`:

**Conditional.** `ICJvmMetadataTracker`, read only when `arguments.multiPlatform && arguments.useMetadataOnIncrementalClasspath`.
JPS never registers it.

**The compiler does not run incrementally here.** It makes a single pass over exactly the sources it is given; it
neither works out what changed nor keeps a record of previous compilations. The configuration only gives a consumer
that already decides its own source set the state it needs to keep deciding correctly.

**Why a sibling of `JvmSnapshotBasedIncrementalCompilationConfiguration`, and not a separate option.** Both are values
of the same `INCREMENTAL_COMPILATION` option, so exactly one of them can be set: who decides the source set is settled
by construction rather than by a validation rule someone has to remember to write. A second, orthogonal option would
have to forbid the combination explicitly — and a missed check there means the services are silently dropped and the
build goes green with stale results.

The sibling relationship costs nothing, because `JvmIncrementalCompilationConfiguration` is an empty marker interface.
What must *not* be inherited is `BaseIncrementalCompilationConfiguration`: all nine of its options (`ROOT_PROJECT_DIR`,
`MODULE_BUILD_DIR`, `BACKUP_CLASSES`, `KEEP_IC_CACHES_IN_MEMORY`, `FORCE_RECOMPILATION`, `OUTPUT_DIRS`,
`UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM`, `MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION`,
`TRACK_CONFIGURATION_INPUTS`) configure the compiler's own IC engine and are no-ops here.

In the implementation, `JvmCompilationOperationImpl.getIcOptionsAccessorOrNull` returns `null` for this configuration,
so `shouldCompileIncrementally()` is false and compilation routes to `compileInProcessWithoutIc`, which registers the
components and trackers through the `registerAdditionalServices` hook on `BaseCompilationOperationImpl`.

**Design against this trap.** Everything in the table above except `LookupTracker` and `ImportTracker` is read only when
`incrementalCompilationIsEnabled(arguments)` holds, and that gate is
`arguments.incrementalCompilation ?: IncrementalCompilation.isEnabledForJvm()`. If the flag ends up false, the compiler
silently ignores every component — the caches included — and the build goes green with wrong incremental results. The
implementation must guarantee the gate is open whenever this configuration is set, rather than trusting
the argument. `JvmCompilationOperationImpl.compileInProcessWithoutIc` does this by forcing
`arguments.incrementalCompilation = true`.

**In-process only, for now.** None of these services cross the RMI boundary to the compile daemon —
`BtaCompilerServicesWithResultsFacade` carries the lookup tracker and nothing else — so the implementation rejects
this configuration under `ExecutionPolicy.WithDaemon` rather than dropping them silently.

## 5. Diagnostics

`MessageCollector`, `CompilerMessageSeverity` (JPS branches on `INFO`, `WARNING`, `STRONG_WARNING`, `FIXED_WARNING`,
`ERROR`, `EXCEPTION`, `LOGGING`) and `CompilerMessageSourceLocation`.

BTA offers only the string-based `KotlinLogger`. `CompilerMessageRenderer` receives severity and location but returns a
`String`, so the structure is lost. JPS needs it structured to build `CompilerMessage(kind, path, line, column)` and to
collect `filesWithErrors` for `JavaBuilderUtil.registerFilesWithErrors`
(`jps/jps-plugin/src/org/jetbrains/kotlin/jps/build/MessageCollectorAdapter.kt`).

Also used: `CompilerRunnerConstants.KOTLIN_COMPILER_NAME` and `CompilerRunnerConstants.INTERNAL_ERROR_PREFIX`.

## 6. One-off compiler service

`classesFqNamesByFiles(files): Set<String>` — a daemon call with an in-process fallback, used to detect classes that
disappeared from dirty or removed files (`KotlinBuilder.kt:250`). **Missing.**

## 7. `kotlin-build-common` entities

`:kotlin-build-common` is currently embedded into the JPS plugin jar
(`repo/kotlin-build-helpers/src/CompilerModules.kt`, `kotlinJpsPluginEmbeddedDependencies`), so these are reachable today
without BTA. They only need exposing if the goal is for JPS to depend on BTA alone:

`IncrementalCacheCommon`, `IncrementalJvmCache`, `updateIncrementalCache`, `ChangesCollector` +
`getChangedAndImpactedSymbols`, `LookupStorage`, `LookupSymbol`, `IncrementalCompilationContext`,
`FileToPathConverter` / `RelativeFileToPathConverter`, `ICReporterBase`, `BuildMetaInfo` / `JvmBuildMetaInfo`
(rebuild on compiler-argument change), `KOTLIN_CACHE_DIRECTORY_NAME`, and `JvmBytecodeBinaryVersion` / `MetadataVersion`
for cache versioning.

BTA's `cri` package (`CriToolchain`, `LookupEntry`, `SubtypeEntry`, `FileIdToPathEntry`) is the existing precedent for
exposing storage-shaped data.

## Already covered — no work needed

- Raw compiler arguments: `CommonToolArguments.applyArgumentStrings` handles JPS's freeform
  `CompilerSettings.additionalArgumentsAsList` and the facet-deserialized arguments.
- Exit codes: `CompilationResult` covers `ExitCode`.
- Build metrics: `BuildMetricsCollector` should cover `JpsBuilderMetricReporter`'s needs.
- Daemon versus in-process selection: `ExecutionPolicy`.
- `KotlinPaths`, `ClassCondition` / preloading, `ProgressReporter`, `Services` itself — JPS-side plumbing that BTA
  replaces rather than exposes.

## Suggested order

1. Sections 1-4 are the minimum for "JPS on BTA without behaviour change".
2. Sections 5-6 are needed for exact parity of diagnostics reporting and dirty-set computation.
3. Section 7 is in scope only if the embedded `kotlin-build-common` dependency is to be dropped.
