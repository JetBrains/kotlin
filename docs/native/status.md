# Knowledge base status

Coverage, cross-subsystem contracts, implied contracts that nothing enforces, known gaps and open questions for the
Kotlin/Native knowledge base in this directory. Update it when you add a page, verify a claim, or resolve a question (then
also remove the question from the page's "Open questions").

## Coverage

| Page | Covers | Verified against |
|------|--------|------------------|
| [README.md](README.md) | index, reading paths, conventions | — |
| [architecture.md](architecture.md) | module map with Gradle paths, module dependencies, two-stage overview, distribution | build scripts, `K2Native.kt`, `TopLevelPhases.kt` |
| [compilation-model.md](compilation-model.md) | user-visible model: klibs, binaries, caches, interop, export (pre-existing) | — |
| [second-stage-pipeline.md](second-stage-pipeline.md) | CLI stage selection, `KonanDriver`, per-output-kind flows, fragments, `runCodegen` order, phase contexts, flags | `K2Native.kt`, `KonanDriver.kt`, `NativeCompilerDriver.kt`, `TopLevelPhases.kt`, `Machinery.kt`, generated argument classes |
| [lowerings.md](lowerings.md) | both lowering stages, synchronization points, per-file invariant, gated lowerings, validation, catalogue of Native lowerings | `NativeLoweringPhases.kt`, `lower/*`, `cli-native-klib`, `NativeLoweringPrerequisitesTest` |
| [optimizations.md](optimizations.md) | overview and gating of the whole-program analyses | `optimizations/*`, `TopLevelPhases.kt`, FileCheck tests |
| [llvm-codegen.md](llvm-codegen.md) | IR → LLVM IR, declarations and naming, RTTI emission, debug info, runtime module linking, LLVM pipelines, object files, linker | `llvm/*`, `CompilerOutput.kt`, `OptimizationPipeline.kt`, `Linker.kt`, `libllvmext` |
| [compiler-runtime-contract.md](compiler-runtime-contract.md) | object layout, TypeInfo, boxing, runtime entry points and annotations, thread states, GC roots, initializers, exceptions, binary options → constants | `TypeInfo.h`, `Memory.h`, `RTTIGenerator.kt`, `Runtime.kt`, `ContextUtils.kt`, `CompilerConstants.hpp`, stdlib annotations |
| [runtime.md](runtime.md) | lifecycle, thread states and STW, roots, allocation and GC overview, scheduler summary, logging, variants and tests | `runtime/src/main/cpp/mm/*`, `gc/*`, `gcScheduler/*`, `alloc/*`, gtests |
| [caches.md](caches.md) | `CacheBuilder`, cache layout and flavors, per-file caches, additional cache info, dependency tracking, cache ABI, prelink, test modes | `CacheBuilder.kt`, `CachedLibraries.kt`, `CacheStorage.kt`, `DependenciesTracker.kt`, `CachesAbiLowering.kt`, test settings |
| [klib-linking.md](klib-linking.md) | first-stage klib production, resolution, K1 descriptors, IR deserialization strategies, partial linkage | `cli-native-klib`, `serialization.native`, `K1Frontend.kt`, `linkKlibs.kt`, `serialization/*` |
| [cinterop.md](cinterop.md) | cinterop tool, `.def` files, cinterop klibs, metadata → IR, interop lowerings and C stubs, call modes, ObjC interop | `Interop/*`, `cinterop.deserialization`, `InteropLowering.kt`, `CStubsManager.kt` |
| [export.md](export.md) | ObjC export flow and codegen, runtime side, exposure and naming rules, C export, Swift export hooks | `NativeCompilerDriver.kt`, `objcexport/*`, `llvm/objcexport/*`, `ObjCExport.mm`, `cexport/*` |
| [dataflow-ir.md](dataflow-ir.md) | DFG model (symbol table, node kinds, what is dropped), builder passes, call graph, rebuild protocol, dumps | `DataFlowIR.kt`, `DFGBuilder.kt`, `CallGraphBuilder.kt`, `GraphAlgorithms.kt` |
| [devirtualization.md](devirtualization.md) | root set, constraint graph, condensation, propagation, result model, IR rewrite, consumers | `DevirtualizationAnalysis.kt`, phases, `IrToBitcode.kt`, FileCheck tests |
| [escape-analysis.md](escape-analysis.md) | PTG construction, summaries, SCC fixpoint, lifetimes, annotation language, codegen and runtime contract, option history | `EscapeAnalysis.kt`, `Escapes.kt`, `PointsTo.kt`, `EscapeAnalysisChecker.kt`, `CodeGenerator.kt`, GC mark traits |
| [dce-and-gha.md](dce-and-gha.md) | DCE reachability and root set, RTTI consumption; GHA intervals, interface coloring, itable schemes, runtime consumers, frameworks | `DCE.kt`, `ClassLayoutBuilder.kt`, `RTTIGenerator.kt`, `Types.cpp`, `ObjCExport.mm` |
| [pre-codegen-inliner.md](pre-codegen-inliner.md) | option, two runs, candidate selection, filters, rewrite, repair, three-inliner comparison | `PreCodegenInliner.kt`, `NativeIrInliner.kt`, `TopLevelPhases.kt` |
| [static-initializers.md](static-initializers.md) | container classification, trigger insertion, objects/enums/companions, codegen fast path, runtime state machine, eager path, caches, redundant-call removal | `StaticInitializersLowering.kt`, `ObjectClassLowering.kt`, `StaticInitializersOptimization.kt`, `IrToBitcode.kt`, `Runtime.cpp` |
| [casts-optimization.md](casts-optimization.md) | generic safe casts in Autoboxing/bridges, `CastsOptimization` predicates, `ComputeTypesPass`, pipeline order, codegen and runtime checks | `CastsOptimization.kt`, `ComputeTypesPass.kt`, `Autoboxing.kt`, `BridgesBuilding.kt`, `Types.cpp` |
| [incremental-compilation.md](incremental-compilation.md) | KGP flags, IC entry, staleness, dirty-file analysis, rebuild, link, correctness table, test grammar | `KotlinNativeLink.kt`, `CacheBuilder.kt`, `CacheStorage.kt`, `DependenciesTracker.kt`, `testData/caches/ic` |
| [gc.md](gc.md) | common framework, STMS/PMCS/CMS/noop algorithms, barriers, termination, scheduler numbers, codegen requirements, interop, options and history | `gc/*`, `gcScheduler/*`, `mm/*`, `GC.kt`, `NativeSecondStageCompilationConfig.kt` |
| [allocator.md](allocator.md) | API, custom allocator pages and paths, sweep, finalizer queues, OS memory, accounting, legacy allocator, options and history | `alloc/*`, `main/cpp/alloc/*`, `RuntimeModule.kt` |
| [platform-libraries.md](platform-libraries.md) | `.def` sources and conventions, `depends`, build tasks and cinterop args, SDK/Xcode, consumption, KGP/IDE/commonizer, contracts | `platformLibs/*`, `build-tools`, `konan.properties`, `loadNativeKlibs.kt`, KGP sources |
| [compatibility-and-migration.md](compatibility-and-migration.md) | every compatibility surface, its enforcement, migration examples, checklist | `KotlinAbiVersion.kt`, `klibArguments.kt`, checkers, `BinaryOptions.kt`, `KonanTarget.kt`, `compiler/arguments` |
| [tooling-integration.md](tooling-integration.md) | KGP target model, tasks and invocations, execution and provisioning, IDE, commonizer, CLI tools, contracts | KGP sources, `cli-runner/main.kt`, `fir-native`, commonizer |
| [debugging.md](debugging.md) | symptom → subsystem → flags → tests | the pages above, `LoggedData.kt`, `BinaryOptions.kt` |
| [glossary.md](glossary.md) | terms with pointers to the canonical page | the pages above |

## Highest-value entry points

- Orientation: [architecture.md](architecture.md) → [second-stage-pipeline.md](second-stage-pipeline.md).
- Any change touching both `llvm/` and `runtime/`: [compiler-runtime-contract.md](compiler-runtime-contract.md).
- Miscompilation: [lowerings.md](lowerings.md) (debug and release) then [optimizations.md](optimizations.md) and its deep dives
  (release only).
- Crash in a user program: [debugging.md](debugging.md) → [runtime.md](runtime.md) → [gc.md](gc.md) / [allocator.md](allocator.md).
- "Works without caches" or "stale after an incremental build": [caches.md](caches.md), [incremental-compilation.md](incremental-compilation.md).
- Is this change safe to ship: [compatibility-and-migration.md](compatibility-and-migration.md), [platform-libraries.md](platform-libraries.md),
  [tooling-integration.md](tooling-integration.md).

## Cross-subsystem contracts

Facts that span pages and are the usual cause of "I changed X and Y broke". Each is explained on the linked page.

- **The compiler and the runtime encode the ABI twice.** `TypeInfo`, `ObjHeader`, `ExtraObjectData`, the init-phase and
  `FILE_*` constants and `KotlinObjCClassInfo` have a C++ and a Kotlin definition; `compiler_interface.bc` only supplies struct
  layouts and function signatures by name. [compiler-runtime-contract.md](compiler-runtime-contract.md),
  [static-initializers.md](static-initializers.md)
- **GHA ids and itable layout are ABI** between `RTTIGenerator`, `CodeGenerator`, `Types.cpp` and `ObjCExport.mm`
  (`classId_` interval semantics, `interfaceTableSize_` sign, `id & (size-1)`, max 32 entries); runtime-created ObjC subclasses
  inherit `classId_`. [dce-and-gha.md](dce-and-gha.md)
- **Stage-1 lowering output is klib ABI**; the stage-2 lowering lists and the `NativeInlineFunctionResolver` copy of list 1 must
  stay in sync. [lowerings.md](lowerings.md), [klib-linking.md](klib-linking.md)
- **Whole-program optimizations are off with any static cache** (`runGlobalOptimizations`), GHA also when producing a cache;
  release builds ignore caches unless `-Xbinary=enableReleaseBinaryCache=true`, and `-opt` therefore silently disables IC.
  [optimizations.md](optimizations.md), [caches.md](caches.md), [incremental-compilation.md](incremental-compilation.md)
- **Declaration-creating lowerings must run before `BuildDFG`** (sealed symbol table); any IR rewrite between `BuildDFG` and
  `EscapeAnalysis` must rebuild the changed functions' DFG (and coroutine liveness). [dataflow-ir.md](dataflow-ir.md)
- **New ways of exposing Kotlin functions to the outside must be rooted** in `computeRootSet` / `EXPLICITLY_EXPORTED`; otherwise
  release builds may DCE them, call the wrong override, or throw "Unexpected receiver type". In library mode every public
  function is a root, which is what export and cinterop callbacks rely on. [devirtualization.md](devirtualization.md),
  [dce-and-gha.md](dce-and-gha.md)
- **Escape analysis is only as sound as the stdlib annotations and the GC variant**: `@Escapes`/`@PointsTo` are checked for
  presence, not correctness; CMS forbids heap→stack references, hence `escapeAnalysisPropagateExiledToHeapObjects`.
  [escape-analysis.md](escape-analysis.md), [gc.md](gc.md)
- **Everything that influences codegen must be in the cache flavor** (`appendCommonCacheFlavor`); options outside it
  (`-Xdebug-prefix-map`) need a clean build. Class field layout and inline-body indices are stored in caches, so layout rules and
  the klib IR format invalidate them. [caches.md](caches.md), [incremental-compilation.md](incremental-compilation.md)
- **A lowering that bakes another file's data into generated code must record a strong dependency**
  (`dependenciesTracker.add`; `NativeEnumWhenLowering` is the model), otherwise IC produces stale binaries.
  [incremental-compilation.md](incremental-compilation.md)
- **Static-initializer trigger calls must stay the first two statements** of a function (the optimizer, `calledInitializer` and
  the constant-object scan rely on it); initializer functions are recognised by origin. [static-initializers.md](static-initializers.md)
- **The `NO_CAST_NEEDED` protocol**: passes between `CastsOptimization` / `TypeOperatorLowering` / `GenericCallsReturnTypeEraser`
  and `Autoboxing` must keep the block as exactly one `IMPLICIT_CAST`. [casts-optimization.md](casts-optimization.md)
- **Codegen pattern-matches lowering origins** and expects erased call types and explicit box/unbox. [lowerings.md](lowerings.md),
  [llvm-codegen.md](llvm-codegen.md)
- **Every heap reference store goes through the `UpdateHeapRef` family** (CMS deletion barrier); a raw store is a bug only under
  CMS. This is also what makes user cache flavors GC-agnostic. [gc.md](gc.md), [compatibility-and-migration.md](compatibility-and-migration.md)
- **The allocator contract**: zeroed memory, 8-byte alignment only (`instanceAlignment_` is never read), `GC::ObjectData` 8 bytes
  before the header, sizes recomputed from `TypeInfo`; stack and permanent objects never reach it. [allocator.md](allocator.md)
- **The runtime module list, logging tags, scheduler defaults and init constants are duplicated** (`RuntimeModule.kt` ↔
  `runtime/build.gradle.kts`; `RuntimeLogging.kt` ↔ `Logging.hpp`; `GCSchedulerConfig.hpp` ↔ `GC.kt`; `IrToBitcode.kt` ↔
  `Runtime.cpp`). [llvm-codegen.md](llvm-codegen.md), [runtime.md](runtime.md), [gc.md](gc.md)
- **Only interop bridges switch thread state**; every C/ObjC/Swift → Kotlin entry goes through `CalledFromNativeGuard`.
  [compiler-runtime-contract.md](compiler-runtime-contract.md), [export.md](export.md)
- **ObjC export header and code spec agree by construction**, not by a check; adapters are emitted per final binary.
  [export.md](export.md)
- **cinterop klibs and platform libraries are opaque to every analysis** (`FunctionSymbol.External`, pessimistic for EA, never
  devirtualized or inlined, no static initializers) and carry metadata only; `.def` `linkerOpts` reach the link only for klibs
  that contributed code. [cinterop.md](cinterop.md), [platform-libraries.md](platform-libraries.md)
- **KGP hard-codes compiler contracts**: flag spellings, output naming, `konan/lib/kotlin-native-compiler-embeddable.jar`,
  `daemonMain`, `-Xdump-perf` JSON, message renderers, manifest keys, `ktest_*` flags, `konan.properties` keys.
  [tooling-integration.md](tooling-integration.md)

## Implied contracts not enforced by any check or test

Collected from the pages' "(e)" findings. Each is believed to hold today because of how the artifacts are built together, not
because something would fail if it stopped holding.

| Contract | Why it holds today | Page |
|----------|--------------------|------|
| Platform libraries match the compiler and LLVM version | same build produces both; only the ABI window and `native_targets` are checked, the special compatibility checker covers the stdlib only | [platform-libraries.md](platform-libraries.md), [compatibility-and-migration.md](compatibility-and-migration.md) |
| System caches match the platform klibs and the flavor string | build-side `cacheFlavor` mirrors `systemCacheFlavorString` by hand | [platform-libraries.md](platform-libraries.md) |
| Generated code is GC/scheduler/allocator-agnostic (user cache flavor omits them) | all MM access is through runtime calls | [compatibility-and-migration.md](compatibility-and-migration.md), [gc.md](gc.md) |
| Auto caches never expire by compiler version | keyed by library fingerprints and flavor; only IC has `compiler.fingerprint` | [caches.md](caches.md), [incremental-compilation.md](incremental-compilation.md) |
| Mangler / `IdSignature` output is stable across releases unless `ir_signature_versions` changes | dump-abi goldens with a fixed compiler only | [compatibility-and-migration.md](compatibility-and-migration.md) |
| The second stage accepts klib IR produced with and without each pre-serialization `LanguageFeature` | klib-compatibility tests cover one previous release | [compatibility-and-migration.md](compatibility-and-migration.md), [lowerings.md](lowerings.md) |
| Binary option names and values are stable | no lifecycle metadata; unknown options are a warning and are dropped | [compatibility-and-migration.md](compatibility-and-migration.md) |
| KGP's argument model matches the downloaded compiler | only when versions are equal; `kotlin.native.version` override untested | [tooling-integration.md](tooling-integration.md) |
| ObjC/C header names are stable across compiler versions for unchanged input | golden headers for one compiler only | [export.md](export.md), [compatibility-and-migration.md](compatibility-and-migration.md) |
| `softDeprecatedTargets` (compiler) and `KonanTarget.deprecatedTargets` (KGP) describe one policy | maintained separately; they disagree today | [compatibility-and-migration.md](compatibility-and-migration.md) |
| `@Escapes`/`@PointsTo` annotations are *correct*, and their bit encoding matches the stdlib klib | checker verifies presence and shape only; a wrong annotation is silent memory corruption in `-opt` | [escape-analysis.md](escape-analysis.md) |
| `TypeInfo.classId_` of runtime-created ObjC subclasses stays inside the superclass interval | `createTypeInfo` copies the superclass id | [dce-and-gha.md](dce-and-gha.md) |
| Binary and static caches agree on "no GHA ids" | both fall back to runtime `IsSubtype` / hash ids; no id versioning exists | [dce-and-gha.md](dce-and-gha.md), [caches.md](caches.md) |
| Making a function non-public does not remove an external caller | library-mode DCE roots are "all public functions"; no feature-specific rooting | [dce-and-gha.md](dce-and-gha.md) |
| Shipped platform libraries are `-Xccall-mode indirect`; light-distribution ones are `both` | different generators; `cCallMode=Direct` fails on platform calls | [platform-libraries.md](platform-libraries.md) |
| Platform API is pinned to the SDK of `xcodeVersion` at build time; users only need `minimalXcodeVersion` | header moves between SDKs change `platform.*` packages (source-compat events) | [platform-libraries.md](platform-libraries.md) |
| `depends` in platform `.def`s is complete and acyclic | generated for Apple by `defFileDependencies`; duplicates otherwise resolve by deserializer order (KT-89825) | [platform-libraries.md](platform-libraries.md) |
| IC file identity (package + absolute path hash) and the flavor string | duplicated in KGP integration-test helpers; moving a checkout invalidates all IC state | [incremental-compilation.md](incremental-compilation.md) |
| Lazy top-level initialization is the semantics of every klib, old or new | second-stage decision since 2021; opt-outs are deprecated | [static-initializers.md](static-initializers.md) |
| Class-initialization order mode is per klib and mixed modes coexist in one binary | manifest flag; caches rejected only when the mode differs | [static-initializers.md](static-initializers.md) |
| `genericSafeCasts` differs between `-g` and `-opt` on purpose | performance; enabling it in release would surface new CCEs (KT-77676) | [casts-optimization.md](casts-optimization.md) |
| Pre-codegen inlining stays opt-in | default 0; turning it on changes stack traces, size, FileCheck expectations (KT-72336) | [pre-codegen-inliner.md](pre-codegen-inliner.md) |
| `objcDisposeOnMain` stays true and the main run loop spins | UIKit code relies on main-thread `dealloc` of associated objects | [gc.md](gc.md) |
| `GC.collect()` is a finalize barrier under tracing GCs and a no-op under `noop` | tests must `IGNORE_NATIVE: gcType=NOOP` | [gc.md](gc.md) |
| OOM aborts rather than throwing `OutOfMemoryError`; only 8-byte alignment is guaranteed | both allocators | [allocator.md](allocator.md) |
| `compiler_version` and `depends` manifest properties are informational | no compiler reader found | [compatibility-and-migration.md](compatibility-and-migration.md) |
| Cross bundles combine Darwin and host distributions from one revision | unchecked | [platform-libraries.md](platform-libraries.md) |

## Findings worth an owner's attention

Recorded as open questions on the pages, listed here because they look like latent defects rather than documentation gaps:

- `RTTIGenerator.kt` defines `TF_LEAK_DETECTOR_CANDIDATE = 16` while the runtime's `TypeInfo.h` uses bit 16 for
  `TF_KOTLIN_OBJC_CLASS` ([compiler-runtime-contract.md](compiler-runtime-contract.md)).
- `DCE.kt` `visitClass` compares an `IrClass` with `context.symbols.nativePointed` (a symbol), so the `NativePointed` exemption
  never matches ([dce-and-gha.md](dce-and-gha.md)).
- `createInitBody` embeds `llvm.tlsCount` per scope while thread-local state slots are allocated lazily; two files with lazy
  `@ThreadLocal` initializers may register TLS records of different sizes for the same key, which the debug runtime asserts
  against ([static-initializers.md](static-initializers.md)).
- `-Xpartial-linkage={enable|disable}` is parsed but nothing non-generated reads it ([klib-linking.md](klib-linking.md)).
- `objcDisposeWithRunLoop` is emitted as a runtime constant with no runtime reader found ([gc.md](gc.md)).
- The light-distribution platform-library generator omits `-Xccall-mode indirect` and names cache directories without the
  `-system` suffix the compiler uses ([platform-libraries.md](platform-libraries.md), [tooling-integration.md](tooling-integration.md)).
- `reversedWholeLibraryDependencies` is built but never traversed by the IC dirty-file analysis ([incremental-compilation.md](incremental-compilation.md)).
- The two `TopologicalLibraryOrder` TODOs in `TopLevelPhases.kt` refer to a class that no longer exists ([klib-linking.md](klib-linking.md)).

## Known gaps

Not covered, or only by a pointer:
- Swift export internals (skipped on purpose; [docs/swift-export](../swift-export/README.md) exists), the klib commonizer's
  algorithm, and the Analysis-API-based ObjC header generator.
- Test infrastructure internals beyond [HACKING.md](../../kotlin-native/HACKING.md#test-infrastructure-overview).
- Benchmarks (`kotlin-native/performance`), the XCTest launcher, the LLDB plugin (`llvmDebugInfoC`, `runtime/src/debug`),
  the Breakpad crash handler.
- Dynamic caches (not built automatically, no test mode) and `-Xcompile-from-bitcode` beyond their entry points.
- The `kotlin.native.internal` intrinsics as a set; they are mentioned where a subsystem uses them.
- The LLVM pass pipeline is described in [llvm-codegen.md](llvm-codegen.md) but not at the level of individual passes.

## Open questions

Collected from the pages; each is also listed at the end of its page.

### second-stage-pipeline.md
- Which phases are safe to disable with `-Xdisable-phases` beyond optimization and verification phases.
- Whether file order after `mergeDependencies` still matters (TODOs in `partiallyLowerModuleWithDependencies`).

### lowerings.md
- Why `TestProcessor`, `UpgradeCallableReferences`, `NativeAssertionWrapperLowering` appear in both stage lists; idempotence of
  `UpgradeCallableReferences` unverified.
- Semantics of Native `-Xprint-ir` / `-Xprint-files` not traced; `-Xphases-to-dump=IrLowering` likely inert for Native.
- Whether per-file dumps of the same phase collide; compatibility of `checkUnboundSymbols = true` with partial-linkage stubs.

### optimizations.md, dataflow-ir.md, devirtualization.md
- Why GHA is allowed for frameworks/dynamic libs but not caches (only `produce.isCache` is checked).
- KT-72336: intended final ordering of `PreCodegenInliner` vs `RemoveRedundantCallsToStaticInitializersPhase`.
- `TAKE_NAMES` true in `SymbolTable`, false in `DevirtualizationAnalysis`; EA's `kfun:kotlin.` prefix check depends on names.
- No recorded evaluation of `maxNumberOfIterations = 2`, the bad-edge heuristic, or the unfold factors 3/5.
- The "Devirtualized from external modules" log section looks always empty.
- No automated check that post-`BuildDFG` lowerings create no declarations (only `require(!sealed)`).
- `inheritorsOf(Type.Virtual)` is empty (non-reflexive); `restoreCoroutineState` is a no-op node; `choosePrimary` precision.
- Library-mode handling of private classes constructed only in exported inline function bodies.

### escape-analysis.md
- `SlotType.STACK` branch in `FunctionGenerationContext.call()` and `StackLocalsManager.clean(refsOnly)` appear dead.
- No rationale for excluding `kotlin.concurrent` / `kotlin.native.concurrent` externals from annotation use, or for
  `allowedToAlloc = 65536`.
- Does library-mode EA ever yield STACK for an object passed to a virtual call? `Depths.GLOBAL` only via the fallback?
- Nothing tests compressed summaries themselves.

### dce-and-gha.md, pre-codegen-inliner.md
- Why `kotlin.FunctionN.invoke` is force-kept by DCE; the `TODO-DCE-OBJC-INIT` rule; dead interfaces still consume colors.
- Whether STATIC/DYNAMIC libraries with ObjC interop need the FRAMEWORK slow-itable path.
- Debug line tables for cross-file pre-codegen inlining; call graph not refreshed between callers; KGP exposure of the option.

### static-initializers.md
- The TLS record size mismatch above; whether the KT-68797 cache-ABI `<field>_get` accessor can be reached for a lazy field.
- Thread-local init skipped while the same thread runs the global initializer; only `Worker.execute` modelled as a thread
  boundary; eager initializer exceptions escaping `initRuntime`; `IrStatementOriginFieldPreInit` apparently dead.

### casts-optimization.md
- Which "inaccuracies in IR types" justify the second `ComputeTypesPass` run; `@PhasePrerequisites(FinallyBlocksLowering)`.
- Is `-opt` + `genericSafeCasts=true` a supported configuration; is casting to the nullable expected type deliberate.
- No rationale for `MaxSize` / loop constants; design of KT-77671.

### llvm-codegen.md
- Full list of phases accepting `-Xsave-llvm-ir-after`; what `cCallMode = Direct` changes in emitted calls.
- Hypothesis: `runLLVMPassesInCompiler` is a temporary gate; the Kotlin and C++ pass implementations must stay equivalent.
- Interaction of `-g` with `-opt` across `Linker` and `BitcodeCompiler`.

### compiler-runtime-contract.md
- Who consumes `@ExportForCompiler`; the `TF_*` flag collision above; the `ShadowStack.hpp` "incompatible" TODO;
  consumers of `ExtendedTypeInfo` beyond `debug.bc`.

### runtime.md, gc.md, allocator.md
- Any remaining runtime teardown path (`memoryDeInit=0`); `Platform.isMemoryLeakCheckerActive` without a leak checker.
- Lock ordering (gcLock → ThreadRegistry → registries) is inferred, not documented.
- `objcDisposeWithRunLoop` reader; MinGW CMS default vs `gcMarkSingleThreaded` only for PMCS; registry lock held across the
  whole CMS mark; arbitrary ParallelProcessor / SafePointTracker constants; `GCStatistics` asserts ≤ 2 pauses; is
  `concurrentWeakSweep=false` supported.
- `instanceAlignment_` never read; why `disableMmap` defaults true when `pagedAllocator=false`; `MAP_POPULATE` rationale;
  GC thread spinning on `concurrentSweepersCount_`; arbitrary page sizes.
- XCTest launcher init path and `src/debug` not examined.

### caches.md, incremental-compilation.md
- How `reportLog` cache / "IC analysis results" messages reach users (hypothesis: `-verbose` / Gradle `--info`).
- Why the `KFunctionImpl.kt` fragment owns the function-interface files; whether cinterop native binaries end up inside static
  cache archives; dynamic caches' support level; no cleanup of old auto-cache or IC directories.
- `reversedWholeLibraryDependencies` dead or planned; main LLVM module content when fully IC'ed; KGP IC dir not declared as
  `@LocalState`; the KT-81760 Linux duplicate-symbol fix is undocumented.

### klib-linking.md, compatibility-and-migration.md
- Is `-Xpartial-linkage={enable|disable}` intentionally ignored; are any Native-specific IR attributes serialized.
- Root set under `EXPLICITLY_EXPORTED` for non-exported klibs; `KlibDAGBuilder` fallback without a signature index.
- Who reads manifest `depends`; how a `metadata_version` incompatibility surfaces; should platform libs be version-checked;
  `softDeprecatedTargets` vs `deprecatedTargets` divergence; KGP ↔ compiler mismatch failure mode; no cross-version ObjC
  header test; the GC-agnostic codegen assumption is neither stated nor tested.

### cinterop.md, platform-libraries.md
- Whether any IDE / Analysis API flow runs the cinterop tool; future of `entryPoint=` / `-Xcompile-source`; why platform libs
  stay indirect (KT-82062); `includedHeaders` determinism across Xcode point releases; compiling pure-C stubs as `.m`;
  ordering of `InteropLowering` vs `SpecialInteropIntrinsicsLowering`.
- Light-distribution call mode and cache directory naming; no version check for platform klibs; `.def.disabled` policy.

### export.md
- Consumers of the analysis-api header generator; ObjC class-name clashes between frameworks; `ObjCExportLazy` vs
  `ObjCExportMapper` divergence; `objCAdapters` when building caches for a framework; whether `BuildCExportsFromIr` replaces the
  K1 path; per-module RTTI TODO.

### tooling-integration.md
- No backend reader for `-Xexternal-dependencies`; `buildKotlinNativeBinaryLinkerArgs` has no callers; `CommonizerCLI` alias
  versioning; how the IDE learns the distribution root; no test pins the `UnitStats` JSON schema.
