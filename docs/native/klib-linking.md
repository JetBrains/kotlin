# Kotlin/Native: klib production and linking

How the first stage writes a `.klib`, and how the second stage loads a set of klibs, builds K1 descriptors for them,
deserializes their IR into one closed world, and tolerates mismatches between them (partial linkage). This page is about the
boundary between the two stages; the surrounding pipeline is in [architecture.md](architecture.md) and
[second-stage-pipeline.md](second-stage-pipeline.md), the user-facing model in [compilation-model.md](compilation-model.md).
The generic klib format, ABI versioning and cross-module inlining are documented elsewhere and only linked here:
[klib inlining](../inlining/klib-inining.md), [uklibs](../klibs/uklibs/Uklibs.md). Terms are in [glossary.md](glossary.md).

Paths below are relative to this file. `BACKEND` means
`../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan`, `SER_NATIVE` means
`../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization`, `SER_COMMON` means
`../../compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common`.

## Role and fit

Everything the second stage compiles comes from klibs: there are no sources at that point
([`K1FrontendPhase`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/K1Frontend.kt)
asserts `sourceFiles.isEmpty()`). The klib boundary is therefore the compatibility surface of K/N: the first stage decides
which IR shape (after pre-serialization lowerings) and which signatures land in the klib, and the second stage must link
klibs produced by different compiler versions, by cinterop (metadata only, no IR), and from compiler caches (see
[caches.md](caches.md)). Three code areas share the work: `compiler/cli/cli-native-klib` (writing), `compiler/ir/serialization.native`
and `native/cinterop.deserialization` (LLVM-free (de)serialization, usable by both stages), and `BACKEND/linkKlibs.kt` plus
`BACKEND/serialization/` (the second-stage glue, including caches).

## Key concepts

| Term                        | Meaning here                                                                                                                                                                                                                                                     |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| klib                        | Zip or directory with manifest, metadata, serialized IR, signature index, optional bitcode. Written by `KlibWriter` in [`writeKlib.kt`](../../compiler/cli/cli-native-klib/src/main/kotlin/org/jetbrains/kotlin/native/writeKlib.kt).                              |
| header klib                 | Klib with public ABI only (`publicAbiOnly`, manifest `header=true`), produced next to the full klib by `-Xheader-klib-path`. See [`NativeKlibIrPhase.kt`](../../compiler/cli/cli-native-klib/src/main/kotlin/org/jetbrains/kotlin/native/pipeline/NativeKlibIrPhase.kt). |
| metadata klib               | `-Xmetadata-klib`: FIR is serialized to metadata, no IR, no `depends`, no `native_targets` limitation; used for shared source sets. Branch `NativeMetadataSerializationPipelinePhase`.                                                                           |
| cinterop klib               | Klib with manifest `interop=true`, `ir_provider=kotlin.native.cinterop`, exactly one `package`. Carries metadata only; IR is built at link time by [`CInteropKlibMetadata2IRTransformer`](../../native/cinterop.deserialization/src/main/kotlin/org/jetbrains/kotlin/backend/konan/serialization/CInteropKlibMetadata2IRTransformer.kt). |
| `LoadedNativeKlibs`         | Result of resolution: `all` (stdlib first), `friends`, `exported`, `included`, `toAddToCache` ([`LoadedKlibs.kt`](../../compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/LoadedKlibs.kt)).                                              |
| included library            | `-Xinclude=<klib>`: compiled *as part of the main module* (one-stage mode uses it for the intermediate klib). Fully deserialized; its descriptors are in `sourcesModules`.                                                                                         |
| exported library            | `-Xexport-library`: deserialized with `DeserializationStrategy.ALL` so every public declaration exists for ObjC/C export.                                                                                                                                         |
| forward declarations module | Synthetic module (no klib) for `cnames.structs.*`/`objcnames.*`; classes are stubbed on demand by [`KonanForwardDeclarationModuleDeserializer`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/KonanForwardDeclarationModuleDeserializer.kt). |
| `IdSignature`               | Stable identity of a declaration across klibs, computed by `KonanManglerIr`/`KonanManglerDesc` ([`KonanMangler.kt`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/KonanMangler.kt)). Cinterop ones carry `IS_NATIVE_INTEROP_LIBRARY`. |
| `KlibDAG`                   | Dependency graph between loaded klibs computed from signature indices, not from IR links ([`KlibDAG.kt`](../../native/cinterop.deserialization/src/main/kotlin/org/jetbrains/kotlin/backend/konan/library/KlibDAG.kt)).                                            |
| partial linkage (PL)        | Linking continues when a signature is missing or a classifier is inconsistent; the use site becomes a stub that throws `IrLinkageError` at runtime.                                                                                                              |

## How it works

### First stage: what Native adds to the common klib pipeline

[`NativeKlibCliPipeline`](../../compiler/cli/cli-native-klib/src/main/kotlin/org/jetbrains/kotlin/native/pipeline/NativeKlibCliPipeline.kt)
chains `NativeConfigurationPhase → NativeFrontendPipelinePhase → (metadata | fir2ir → pre-serialization lowerings →
IR serialization) → NativeKlibWritingPipelinePhase`. IR generation is skipped for `-Xmetadata-klib`, and in header mode
(`-Xheader-mode`, `-Xheader-mode-type=compilation`) unless the module has inline functions, inline accessors or value classes
(`requireIrForHeaderCompilationMode`). Native-specific pieces:

- **Used-library computation.** [`NativeFir2IrPipelinePhase`](../../compiler/cli/cli-native-klib/src/main/kotlin/org/jetbrains/kotlin/native/pipeline/NativeFir2IrPipelinePhase.kt)
  walks every symbol cached by fir2ir, collects the packages of external package fragments (minus forward-declaration packages)
  and maps them to klibs via `metadata.getPackageNames()`. [`FirNativeSerializer.kt`](../../compiler/cli/cli-native-klib/src/main/kotlin/org/jetbrains/kotlin/native/FirNativeSerializer.kt)
  additionally treats every library passed explicitly in `-library`/`-Xinclude` as used. The result becomes the `depends`
  list of the manifest (`legacyNativeDependenciesInManifest`). Metadata klibs get an empty list.
- **Checks on IR before serialization.** `SpecialBackendChecksTraversal` from
  [`compiler/ir/backend.native`](../../compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/lower/SpecialBackendChecksTraversal.kt)
  runs on the fir2ir result (interop restrictions, `@ObjCOverrideInit`, intrinsics; it reports through
  `reportCompilationError`, so a violation aborts with `KonanCompilationException`). After the lowerings,
  [`NativeKlibCheckers`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/checkers/NativeKlibCheckers.kt)
  runs IR diagnostics (currently `NativeVolatileCheck`).
- **Pre-serialization lowerings** come from `nativeLoweringsOfTheFirstPhase` plus, behind
  `LanguageFeature.NativeTestProcessorBeforeSerialization`, `TestProcessor` ([`PreSerializingLowerings.kt`](../../compiler/cli/cli-native-klib/src/main/kotlin/org/jetbrains/kotlin/native/PreSerializingLowerings.kt)).
  Their output *is* the klib ABI; see [lowerings.md](lowerings.md).
- **Serializer subclasses.** [`KonanIrModuleSerializer`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/KonanIrModuleSerializer.kt)
  skips the synthetic C-types file (`cTypeDefinitionsFileName`), because struct/enum IR is regenerated at link time.
  [`KonanIrFileSerializer`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/KonanIrFileSerializer.kt)
  keeps `@ExportForCppRuntime` functions and `@ExportTypeInfo` classes as explicit roots (the runtime needs them even if
  nothing in Kotlin references them) and serializes all members of K1-lazy classes.
- **Manifest.** `writeKlib` sets `abi_version` from `klibAbiVersionForManifest()` (`-Xklib-abi-version` or the ABI
  compatibility level), `native_targets`, `header`, language features and metadata flags, and the `-manifest` custom
  properties; `-nopack` writes a directory. `-Xwrite-dependencies-of-produced-klib-to` dumps the `depends` paths.
- **`NativeIrAttributes`** ([`BACKEND/NativeIrAttributes.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/NativeIrAttributes.kt))
  holds `isCalledFromExportedInlineFunction` / `isConstructedFromExportedInlineFunctions`. They are plain `irFlag`s used in the
  second stage; they are not serialized. Unverified: whether any Native-specific attribute is written into the klib IR.
- **One-stage mode** ([`oneStageMode.kt`](../../native/cli-native/src/org/jetbrains/kotlin/cli/bc/oneStageMode.kt)): when
  `K2Native` gets sources and a binary `-produce`, it first runs the klib pipeline with `produce=library` into a temporary
  klib, then re-runs with `contentRoots = []`, `useFir = false`, and the temporary klib added to both `-library` and
  `-Xinclude`. So "one stage" is literally two stages; nothing is shared except the configuration.

### Second stage, step 1: library resolution (`loadNativeKlibs`)

[`loadNativeKlibs`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/loadNativeKlibs.kt)
is called once from `NativeSecondStageCompilationConfig.loadedKlibs`. It configures the common `KlibLoader`
([`KlibLoader.kt`](../../compiler/util-klib/src/org/jetbrains/kotlin/library/loader/KlibLoader.kt)) with:

1. [`KlibNativeDistributionLibraryProvider`](../../native/utils/src/org/jetbrains/kotlin/konan/library/KlibNativeDistributionLibraryProvider.kt):
   `klib/common/stdlib` unless `-nostdlib`, and `klib/platform/<target>/org.jetbrains.kotlin.native.platform.*` unless
   `-no-default-libs` (deprecated spelling `-nodefaultlibs`). `konanHome` (`-Xkonan-data-dir` only changes `~/.konan`) or the
   compiler's own distribution is the root. The provider marks loaded klibs with `isFromKotlinNativeDistribution` and
   `isImplicitlyLoadedFromKotlinNativeDistribution` ([`NativeKlibAttributes.kt`](../../native/utils/src/org/jetbrains/kotlin/konan/library/NativeKlibAttributes.kt)).
2. explicit `-library`/`-l` paths (`konanLibraries`); `-Xinclude`, `-friend-modules`, `-Xexport-library` are *selections*
   from the loaded set, not extra loads;
3. a platform check (`KlibPlatformChecker.Native(target)` or `NativeMetadata` for metadata compilations), the ABI window
   `[FIRST_SUPPORTED, CURRENT]`, and [`KlibNativeManifestTransformer`](../../native/utils/src/org/jetbrains/kotlin/konan/library/KlibNativeManifestTransformer.kt),
   which substitutes `<prop>.<target>` manifest properties for the current target (`linkerOpts`, `includedHeaders`, …,
   [`KonanLibrary.kt`](../../native/utils/src/org/jetbrains/kotlin/konan/library/KonanLibrary.kt)).

Then it rejects libraries with an unknown `ir_provider` (only none or `kotlin.native.cinterop` are accepted), reports loader
problems, removes duplicated unique names (`-Xklib-duplicated-unique-name-strategy`, KT-76785), and runs
[`KonanLibrarySpecialCompatibilityChecker`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/KonanLibrarySpecialCompatibilityChecker.kt),
which compares the stdlib version (read from the distribution's `konan.properties`) with the compiler
(`-Xskip-library-special-compatibility-checks` disables it). Libraries from the distribution and cinterop libraries are
silently dropped from `exported`/`included` with a warning (an error in tests).

There is no `KonanLibraryResolver`/`TopologicalLibraryOrder` any more; the two `TODO` comments mentioning it in
[`TopLevelPhases.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/TopLevelPhases.kt)
(`partiallyLowerModuleWithDependencies`) are stale. Order is now: `LoadedNativeKlibs.all` is CLI order with stdlib first;
the `KlibDAG` (built once per compilation in `NativeSecondStageCompilationConfig`, passed serialized to spawned cache builds
via `serializedKlibDag`) provides `librariesReverseTopoSorted` with `all`-order as the tie-breaker.

### Second stage, step 2: the K1 "frontend" (`runK1Frontend`)

[`K1FrontendPhase`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/K1Frontend.kt)
creates a `ModuleDescriptorImpl` named `<moduleId>` over [`KonanBuiltIns`](../../native/frontend/src/org/jetbrains/kotlin/builtins/konan/KonanBuiltIns.kt)
and asks [`KlibResolvedModuleDescriptorsFactoryImpl.createResolved2`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/KlibResolvedModuleDescriptorsFactoryImpl.kt)
for one deserialized descriptor module per klib (via
[`K1KlibMetadataModuleDescriptorFactoryImpl`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/K1KlibMetadataModuleDescriptorFactoryImpl.kt),
which reads klib metadata only), plus the forward-declarations module. Friend/refines/included relations are encoded as
descriptor module dependencies; included libraries additionally see friends. The main module depends on all of them
(or, when compiling stdlib itself, on nothing but the synthetic `FunctionN` provider). No source analysis happens.

Why descriptors are still needed, concretely:

- `linkKlibs` maps every `ModuleDescriptor` to its klib through `DeserializedKlibModuleOrigin` and creates an
  `IrModuleFragmentImpl(moduleDescriptor)` per klib; `IrBuiltInsForLinker` and `KonanBuiltIns` come from this module.
- The `SymbolTable` of the link phase uses `KonanIdSignaturer(KonanManglerDesc)`
  ([`LinkKlibs.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/LinkKlibs.kt)),
  i.e. descriptor → signature → IR symbol lookups.
- ObjC export (`ProduceObjCExportInterfacePhase`, header generator `impl/k1`) and C export (`BuildCExports`, when
  `cExportUseIrDiscovery` is off) analyse descriptors *before* IR exists, then
  `CreateObjCExportCodeSpecPhase`/`CAdapterGenerator` translate to IR symbols through `symbolTable.descriptorExtension`
  (see [export.md](export.md)). With `-Xomit-framework-binary` the compiler stops after the header, never linking IR.
- Lazy ObjC header dumping (`emitLazyObjcHeaderFile`) runs a K1 container in `setupContainerAndDumpObjcHeader`.

The binding context is cleared by `disposeCallback` once `linkKlibs` is done.

### Second stage, step 3: IR linking (`linkKlibs`)

[`linkKlibs`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/linkKlibs.kt)
(`LinkKlibsPhase`, cannot be disabled) does, in this order:

1. `createIrLinker`: a [`KonanIrLinker`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/KonanIrLinker.kt)
   over the shared `SymbolTable`, with `exportedDependencies = exported + included + librariesToCache`, friend map,
   `IrObjCOverridabilityCondition`, PL support and the `PartialLinkageIssueCollector` sink.
2. `scheduleDependenciesForDeserialization`: for each klib in `all` order, register a module deserializer.
   `KonanIrLinker.createModuleDeserializer` picks the class: forward declarations →
   `KonanForwardDeclarationModuleDeserializer`; cinterop →
   [`KonanInteropModuleDeserializer`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/serialization/KonanInteropModuleDeserializer.kt);
   otherwise [`KonanPartialModuleDeserializer`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/KonanPartialModuleDeserializer.kt).
   The stdlib deserializer is wrapped in `IrModuleDeserializerWithBuiltIns` (`isBuiltInModule`).
3. `ExternalDependenciesGenerator.generateUnboundSymbolsAsDependencies()` drives deserialization from the unbound symbols in
   the table, then `irLinker.postProcess(inOrAfterLinkageStep = true)` builds fake overrides and PL stubs
   ([`KotlinIrLinker.postProcess`](../../compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/KotlinIrLinker.kt)).
4. Platform libraries that were loaded implicitly and from which nothing was linked are dropped
   (`filterOutUnusedPlatformLibraryModules`); C struct/enum bodies are generated for the remaining cinterop modules
   ([`IrImplementationGeneratorForCStructsAndEnums`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/serialization/IrImplementationGeneratorForCStructsAndEnums.kt)).
5. `checkNoUnboundSymbols(... "at the end of IR linkage process")`, then modules are sorted by `KlibDAG` and files and
   stdlib `*Function` classes are sorted by name to make global-initializer order deterministic.

Output (`LinkKlibsOutput`): `irModules` (all useful dependency modules, reverse topological order, no forward-declarations
module), `irModulesToCompile` (a fresh, empty `IrModuleFragmentImpl(moduleDescriptor)` for a normal build, or the modules of
`librariesToCache` for a cache build), `irBuiltIns`, `symbols` (`BackendNativeSymbols`), the `symbolTable`, and the `irLinker`
itself, which stays alive for the whole backend because lowerings deserialize lazily and caches read klib data through
[`ModuleDeserializerProvider`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/serialization/ModuleDeserializerProvider.kt).
Right after linking, `runSpecialBackendChecks` runs the same `SpecialBackendChecksTraversal` as the first stage on
`irModulesToCompile` ([`BackendPhases.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/BackendPhases.kt)),
and `runBackend` first runs
[`FunctionsWithoutBoundCheckGenerator`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/ir/FunctionsWithoutBoundCheckGenerator.kt),
which adds `getWithoutBoundCheck`/`setWithoutBoundCheck` members to every array class in the *shared* built-ins, so they exist
in every fragment regardless of which module is compiled.

#### What is deserialized, and how lazily

`DeserializationStrategy` (`KotlinIrLinker.kt`) is chosen per file:

| Library                                                  | Strategy                                                                                                           |
|----------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| included, exported, being cached                         | `ALL`: every declaration with bodies                                                                               |
| any other klib with IR                                   | `EXPLICITLY_EXPORTED`: declarations marked as explicit roots plus whatever is reached from the symbol table         |
| fully cached (cache exists, not being rebuilt)           | `ONLY_DECLARATION_HEADERS`; header klibs: `WITH_INLINE_BODIES`                                                      |
| file not covered by `CacheDeserializationStrategy`       | `ON_DEMAND` (`KonanPartialModuleDeserializer` wraps the resolver)                                                   |
| cinterop                                                 | metadata → IR on demand, by signature, inside `KonanInteropModuleDeserializer.tryDeserializeIrSymbol`               |

[`CacheDeserializationStrategy`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/CacheDeserializationStrategy.kt)
(`Nothing`, `WholeModule`, `SingleFile`, `MultipleFiles`) comes from `-Xadd-cache` + `-Xfile-to-cache`/`-Xmake-per-file-cache`
through `CacheSupport.librariesToCache`; it restricts which files of the library being cached are compiled. When a cinterop
library is cached, `deserializeAllCStructsAndEnums` loads every struct and enum so that their generated code is in the cache.

"Lazy IR" in the descriptor sense (`IrLazyClass` over K1 descriptors) is no longer how K/N loads dependencies; the only
remnant is [`K1LazyFakeOverrideClassFilter`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/K1LazyFakeOverrideClassFilter.kt),
which tells the fake-override builder to skip K1 lazy classes. Fake overrides are never stored in klibs;
`IrLinkerFakeOverrideProvider` rebuilds them at link time, which is why `KonanInteropModuleDeserializer` returns an unbound
symbol for a member it cannot find.

### Partial linkage

The engine is shared ([`PartialLinkageSupportForLinkerImpl`](../../compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/linkage/partial/PartialLinkageSupportForLinkerImpl.kt)):
during `postProcess`, `exploreClassifiers` marks classifiers with unbound supertypes or bounds, `generateStubsAndPatchUsages`
creates stub declarations for unbound symbols and rewrites every use into a call that throws. Lowerings get
[`PartialLinkageSupportForLowerings`](../../compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/linkage/partial/PartialLinkageSupportForLowerings.kt)
through `NativeBackendContext`. At runtime the stub ends in `kotlin.internal.throwIrLinkageError`
([`IrLinkageError.kt`](../../libraries/stdlib/common-non-jvm/src/kotlin/internal/IrLinkageError.kt)); `KFunctionImpl`/`KPropertyImpl`
in `kotlin-native/runtime` do the same for broken reflection targets.

Native specifics:

- `KonanIrLinker` allows intersection overrides of ObjC class members without a PL error
  (`isMultipleInheritedImplementationsAllowed`).
- `KonanInteropModuleDeserializer.preferLinkingToTheCurrentModule = false`: duplicated declarations across cinterop klibs link
  to whichever module was deserialized first (KT-89825).
- **Replay for caches.** PL issues are only detected for code compiled from IR in the current run. Cached code was linked when
  the cache was built, so `PartialLinkageIssueCollector` records issues
  ([`PartialLinkageIssues.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/serialization/PartialLinkageIssues.kt)),
  `CacheStorage.savePartialLinkageIssues` stores them per file/library, and
  [`replayPartialLinkageIssuesFromCaches`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/PartialLinkageIssuesReplay.kt)
  (called from `KonanDriver.run` after `CacheBuilder`) re-reports them, skipping caches rebuilt in this run, and aborts when the
  level is `ERROR` (KT-78253).
- Flags: `-Xpartial-linkage-loglevel={silent|info|warning|error}`; the default is `SILENT`
  ([`PartialLinkageSettings.kt`](../../compiler/util/src/org/jetbrains/kotlin/config/PartialLinkageSettings.kt)).
  `-Xpartial-linkage={enable|disable}` is parsed in `CommonKlibBasedCompilerArguments` but no non-generated code reads it in
  this tree, so PL cannot be switched off from the CLI. Unverified: whether that is intentional.

## Invariants and ordering constraints

- **The first stage is LLVM-free.** `cli-native-klib`, `compiler/ir/backend.native`, `compiler/ir/serialization.native` and
  `native/cinterop.deserialization` must not depend on `:kotlin-native:backend.native` (see [architecture.md](architecture.md)).
- **Pre-serialization lowerings are ABI.** A klib contains IR *after* `nativeLoweringsOfTheFirstPhase`. Changing those
  lowerings changes what consumers deserialize; old klibs keep the old shape, so the second stage must accept both or the ABI
  version must be bumped ([lowerings.md](lowerings.md), [klib inlining](../inlining/klib-inining.md)).
- **Signatures must be unique and stable.** `KonanIrLinker` binds by `IdSignature`; a second declaration with the same
  signature fails with "already bound" (regular klibs) or is silently shadowed (cinterop). `-Xklib-enable-signature-clash-checks`
  detects clashes at serialization time (`NativeIrSignatureClashTest`).
- **Stdlib first, dependencies before dependents.** `createResolved2` requires stdlib first; `KlibDAG` order feeds
  `irModules`, and `NativeBackendContext` documents that eager initializer order depends on it.
- **No unbound symbols after linking.** `checkNoUnboundSymbols` is a hard error; anything still unbound means a signature was
  not found and PL did not stub it (e.g. requested after `postProcess`).
- **Caches change the strategy, not the graph.** A fully cached library is header-only in IR, so inter-module dependencies
  discovered by the linker are incomplete; this is why `KlibDAG` is computed from signature indices instead
  (`reverseTopoSortUsingKlibDAG` comment).
- **Stage contract for one-stage mode.** `K1FrontendPhase` fails if any source file reaches it; `KonanDriver.splitOntoTwoStages`
  must have split the compilation.

## Extension points and typical changes

| Change                                                   | Where                                                                                                              | Also affects                                    |
|----------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| New IR check for klib producers                          | `NativeKlibCheckers.callCheckers` (`serialization.native/checkers`), diagnostics in `NativeKlibErrors`             | tests in `compiler/testData/diagnostics`         |
| New backend-only semantic check                          | `SpecialBackendChecksTraversal` (runs in both stages)                                                              | first stage error reporting                     |
| New pre-serialization lowering                           | `nativeLoweringsOfTheFirstPhase` + `NativeLoweringPhases.kt`                                                       | klib ABI, klib-compatibility tests              |
| New manifest property                                    | `writeKlib.kt`; reader in `native/utils` `KonanLibrary.kt`; per-target substitution via `substituteFor`            | KGP, commonizer                                 |
| New distribution library location or default library set| `KlibNativeDistributionLibraryProvider`, `loadNativeKlibs`                                                         | `FirNativeSerializer` used-library logic        |
| Native-specific symbol resolution (e.g. interop)         | `KonanIrLinker.createModuleDeserializer`, `KonanInteropModuleDeserializer`, `CInteropKlibMetadata2IRTransformer`  | [cinterop.md](cinterop.md), caches             |
| Data stored in caches per declaration                    | `ModuleDeserializerProvider` → `KonanPartialModuleDeserializer` (`getFileNameOf`, `getKlibFileIndexOf`), `ProtoUtils.kt` | [caches.md](caches.md)                          |

## Investigating bugs

| Symptom                                                                 | Where to look                                                                                                                                         |
|-------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| "There is still an unbound symbol at the end of IR linkage process"     | `UnexpectedUnboundIrSymbols` in [`KotlinIrLinkerIssues.kt`](../../compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/linkage/issues/KotlinIrLinkerIssues.kt). Compare the signature with `klib dump-signatures`; check version skew between klibs (`EnumEntries` feature mismatch is special-cased). |
| "Module X has a reference to symbol ... Neither the module itself nor its dependencies contain such declaration" | `SignatureIdNotFoundInModuleWithDependencies`; thrown from `deserializeOrReturnUnboundIrSymbolIfPartialLinkageEnabled` when PL cannot stub. Platform-library (KT-78063) and cinterop (KT-78062) variants have dedicated hints. |
| Unexpected PL stub / `IrLinkageError` at runtime                         | Run with `-Xpartial-linkage-loglevel=warning` (or `error`) to see the issue; for cached builds check `replayPartialLinkageIssuesFromCaches`. Classifier-level causes: `exploreClassifiers`. |
| "Incompatible ABI version ... in library" / "Library failed platform-specific check" | `KlibLoaderResult` rendering in [`KlibLoaderResult.kt`](../../compiler/util-klib/src/org/jetbrains/kotlin/library/loader/KlibLoaderResult.kt); window set in `loadNativeKlibs`; stdlib version mismatch in `KonanLibrarySpecialCompatibilityChecker`. |
| "Library with unsupported IR provider"                                   | `checkForUnknownIrProviders` in `loadNativeKlibs`.                                                                                                     |
| Declaration missing from a cinterop klib / "No C-Interop symbol found"  | `KonanInteropModuleDeserializer.contains/tryDeserializeIrSymbol` (signature must carry `IS_NATIVE_INTEROP_LIBRARY`, package must match manifest `package`); then `CInteropKlibMetadata2IRTransformer`; struct/enum bodies in `IrImplementationGeneratorForCStructsAndEnums`. |
| Platform library silently absent                                        | `filterOutUnusedPlatformLibraryModules`: implicitly loaded cinterop modules with no linked declarations are dropped.                                  |
| Wrong `depends` in a produced klib                                      | `usedLibraries` in `NativeFir2IrPipelinePhase` and the explicit-library rule in `FirNativeSerializer`.                                                 |
| Non-deterministic initializer order                                     | `sortFilesAndDeclarationsToKeepPipelineDeterministic`, `KlibDAG.librariesReverseTopoSorted`.                                                           |

Useful flags: `-Xphases-to-dump-after=LinkKlibs`, `-Xpartial-linkage-loglevel`, `-Xklib-enable-signature-clash-checks`,
`-Xwrite-dependencies-of-produced-klib-to`, `-Xheader-klib-path`, `-nopack` (inspect the klib as a directory).

### Tests

Test tasks are defined in `native/native.tests/build.gradle.kts` (`nativeTestTask(name, tag)`); generated classes live in
`build/tests-gen` (see [kotlin-native/AGENTS.md](../../kotlin-native/AGENTS.md)).

```bash
# compiler/testData/klib/partial-linkage, via AbstractNativePartialLinkageTest
./gradlew :native:native.tests:partialLinkageTest
# tag "klib": cross-compilation, manifest writing, ...
./gradlew :native:native.tests:klibTest
# dump-ir/-metadata/-signatures, header klibs, synthetic accessors, dump-abi, NativePartialLinkageDiagnosticsTest;
# test data under native/native.tests/testData/klib and compiler/testData/klib
./gradlew :native:native.tests:klib-ir-inliner:test
# first or second stage with another compiler version (downloads kotlin-native-prebuilt)
./gradlew :native:native.tests:klib-compatibility:tasks --all | grep testCustom
# unit tests: LoadedNativeKlibsOrderTest, NativeIrSignatureClashTest, NativeIrSignatureWithUnboundIrTest
./gradlew :compiler:ir.serialization.native:test
```

Generators: [`GenerateNativeTests.kt`](../../native/native.tests/testFixtures/org/jetbrains/kotlin/generators/tests/GenerateNativeTests.kt)
(partial linkage) and [`GenerateKlibNativeTests.kt`](../../native/native.tests/klib-ir-inliner/testFixtures/org/jetbrains/kotlin/generators/tests/GenerateKlibNativeTests.kt).
The klib-compatibility suite downloads `kotlin-native-prebuilt` of another version and runs the first or second stage with it
([`CustomNativeCompilerSecondStageSanity.kt`](../../native/native.tests/klib-compatibility/tests/org/jetbrains/kotlin/konan/test/klib/CustomNativeCompilerSecondStageSanity.kt));
it is skipped on Windows.

## Open questions

- Is the `-Xpartial-linkage={enable|disable}` value intentionally ignored (only the log level reaches `PartialLinkageConfig`)?
- Are there any Native-specific IR attributes or extensions actually serialized into klib IR, beyond what the common
  serializer writes? `NativeIrAttributes.kt` contains only in-memory flags.
- `ExternalDependenciesGenerator` deserializes "explicitly exported" roots for non-exported klibs; the exact set of roots
  (`backendSpecificExplicitRoot` plus what the common serializer marks) was not traced end to end.
- Whether `TopologicalLibraryOrder` TODOs in `TopLevelPhases.kt` still describe a real reversal, now that order comes from
  `KlibDAG` (the code still does `dependenciesToCompile.reversed()`).
- How `KlibDAGBuilder` handles libraries without a signature index (older klibs); `useSignatureIndices` has a fallback path
  that was not read in detail.
