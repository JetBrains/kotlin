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
| [coroutines.md](coroutines.md) | tail-call classification, generated coroutine class, `IrSuspendableExpression`/`IrSuspensionPoint`, liveness and late spilling, `indirectbr` codegen, stdlib side, GC/EA view, exceptions, ObjC export bridges | `NativeSuspendFunctionLowering.kt`, `AbstractSuspendFunctionsLowering.kt`, `CoroutinesLivenessAnalysis.kt`, `CoroutinesVarSpillingLowering.kt`, `IrToBitcode.kt`, `IntrinsicsNative.kt`, FileCheck tests |
| [exceptions.md](exceptions.md) | throw → unwinding → landing pads, `ExceptionObjHolder`, interop filtering, ObjC/Swift `NSError`, unhandled exceptions and termination, stack traces and symbolication | `CodeGenerator.kt`, `IrToBitcode.kt`, `Exceptions.cpp`, `ObjCExceptions.cpp`, `StackTrace.cpp`, `SourceInfo.cpp`, `ObjCExportCodeGenerator.kt`, standalone tests |
| [debug-info.md](debug-info.md) | DWARF emission, scopes and `inlinedAt`, variables and types, `ExtendedTypeInfo`, `Konan_Debug*`, `konan_lldb.py`, symbolication, dSYM and linking | `DebugUtils.kt`, `IrToBitcode.kt`, `llvmDebugInfoC`, `KDebug.cpp`, `konan_lldb.py`, `SourceInfo.cpp`, lldb tests |
| [intrinsics.md](intrinsics.md) | annotations catalogue, all `IntrinsicType` kinds by family and handler, symbol-recognized stdlib functions, `@GCUnsafeCall`/`@ExportForCppRuntime` call directions, extension checklist | `Annotations.kt`, `IntrinsicType.kt` (both copies), `IntrinsicGenerator.kt`, `Ir.kt`, `IrUtils.kt`, `EscapeAnalysisChecker.kt`, `KonanMangler.kt` |
| [llvm-passes.md](llvm-passes.md) | per-mode pipelines and `-passes` strings, bitcode post-processing phases, `LLVMKotlinRunPasses`, custom `kotlin-*` passes, runtime inlining, attributes, debugging with `opt` | `OptimizationPipeline.kt`, `BitcodePhases.kt`, `libllvmext/src/main/cpp/*`, `RuntimeLinkage.kt`, `PerformanceInline.kt`, FileCheck tests |
| [targets-and-toolchains.md](targets-and-toolchains.md) | `KonanTarget`/`HostManager`, `konan.properties` lookup rules, dependency downloader, Clang and linker drivers, Xcode coupling, distribution layout, runtime build per target, add/remove a target | `KonanTarget.kt`, `HostManager.kt`, `KonanProperties.kt`, `Configurables.kt`, `DependencyProcessor.kt`, `ClangArgs.kt`, `Linker.kt`, `Xcode.kt`, `NativeDistribution.kt`, git history of target additions |
| [commonizer.md](commonizer.md) | CIR, deserialization, merged node tree, transformers, per-declaration commonizers, type-alias substitution, optimistic/platform numbers, serialization, hierarchical runs and dependencies, KGP tasks and cache | `native/commonizer/src/**`, `commonizer-api`, KGP `*Commoniz*` tasks, commonizer tests |
| [test-infrastructure.md](test-infrastructure.md) | test families, `-Pkn.*` → settings → flags, life of a blackbox test, grouping and compilation caches, test runner protocol, directives, FileCheck, cache modes, test generation, CI aggregates | `native/native.tests/testFixtures/**`, `nativeTest.kt`, `TestProcessor.kt`, `TestsDumper.kt`, `build.gradle.kts` files |
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
  [tooling-integration.md](tooling-integration.md), [targets-and-toolchains.md](targets-and-toolchains.md).
- Debugger or stack-trace complaints: [debug-info.md](debug-info.md), [exceptions.md](exceptions.md).
- A test behaves differently from the command line: [test-infrastructure.md](test-infrastructure.md).
- Adding a runtime-called function or an intrinsic: [intrinsics.md](intrinsics.md).

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
  `daemonMain` (looked up by name with `singleOrNull`, so an overload breaks every K/N task), `-Xdump-perf` JSON, message
  renderers, manifest keys, `ktest_*` flags, `konan.properties` keys, the printed stack-trace format
  (`KotlinNativeStackTraceParser`). [tooling-integration.md](tooling-integration.md), [exceptions.md](exceptions.md)
- **Coroutine liveness is computed early and consumed late.** `CoroutinesLivenessAnalysis` records live variables on each
  `IrSuspensionPoint`; devirtualization, `UnboxInlineLowering`, the inliner and DCE run in between and must neither remove a
  listed variable nor introduce one live across a suspension; only the pre-codegen inliner recomputes. The attributes are not
  copied by default, so a pass that copies a suspension point drops them. [coroutines.md](coroutines.md), [dataflow-ir.md](dataflow-ir.md)
- **Only one EA edge keeps stack objects alive across suspension** (`SaveCoroutineState → this.intestines` over the live
  variable list); it relies on `ExpressionSlicer` hoisting every impure argument into a temporary. [coroutines.md](coroutines.md),
  [escape-analysis.md](escape-analysis.md)
- **Kotlin frames have no landing pads; the shadow stack is reset by `SetCurrentFrame` at the catching frame.** Every function
  callable from foreign code needs a cleanup landing pad, every call into foreign code a filtering handler; a C++ exception that
  reaches a Kotlin `catch` is reinterpreted as a Kotlin one because catch clauses never check `typeid`. [exceptions.md](exceptions.md),
  [compiler-runtime-contract.md](compiler-runtime-contract.md), [cinterop.md](cinterop.md)
- **The stdlib and the compiler are coupled by name**: `IntrinsicType` strings (two copies matched by `Enum.valueOf`),
  `BackendNativeSymbols` lookups, `@GCUnsafeCall` callee names (prototype derived from the Kotlin signature, never checked
  against C++), `@ExportForCppRuntime` names the runtime calls. [intrinsics.md](intrinsics.md), [compatibility-and-migration.md](compatibility-and-migration.md)
- **LLVM-level passes trust codegen attributes**: `nounwind` on `RUNTIME_NOTHROW` runtime functions, the literal callee name
  `Kotlin_mm_safePointFunctionPrologue` and its `NO_INLINE` for `kotlin-remove-sp`, `performance_inline` annotations, and
  `targetCpu`/`targetCpuFeatures` matching how the runtime bitcode was compiled. [llvm-passes.md](llvm-passes.md),
  [targets-and-toolchains.md](targets-and-toolchains.md)
- **Debug info is read by three consumers with different assumptions**: LLDB via `konan_lldb.py` (needs `ExtendedTypeInfo`,
  which `-opt` omits, and the `kclass:kotlin.String` symbol in the selected frame's module), the runtime symbolicator
  (`sourceInfoType`, `noop` for every `-opt` and non-Apple build), and dsymutil/strip. [debug-info.md](debug-info.md),
  [exceptions.md](exceptions.md)
- **Target names, `konan.properties` keys and the distribution layout are consumed by KGP, the commonizer, the IDE and
  tests** (`presetName`, `commonizer_target` strings, `klib/platform/<target>`, `klib/cache/<flavor>`, `cacheableTargets.<host>`,
  `llvmVersion.<host>` equal to the LLVM archive's `lib/clang/<ver>`). [targets-and-toolchains.md](targets-and-toolchains.md),
  [commonizer.md](commonizer.md), [test-infrastructure.md](test-infrastructure.md)
- **Commonized klibs are metadata only and carry only the common slot**: no `actual` klibs are produced, leaves stay the original
  platform/cinterop klibs, and the frontend must accept the commonized `expect` next to the platform declaration.
  [commonizer.md](commonizer.md), [klib-linking.md](klib-linking.md)

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
| `compiler_version` and `depends` manifest properties are informational | no compiler reader found; `-Xmetadata-klib` outputs have no `depends` at all | [compatibility-and-migration.md](compatibility-and-migration.md), [commonizer.md](commonizer.md) |
| Cross bundles combine Darwin and host distributions from one revision | unchecked | [platform-libraries.md](platform-libraries.md) |
| Nothing between `CoroutinesLivenessAnalysis` and `CoroutinesVarSpillingLowering` changes the set of live variables at a suspension point | passes in between do not touch them today; a stale list is a silently wrong resume | [coroutines.md](coroutines.md) |
| Stdlib coroutine classes (`BaseContinuationImpl`, `ContinuationImpl`, `SafeContinuation`, `COROUTINE_SUSPENDED` identity, the `Function{N+1}` supertype trick) keep the shape the lowering emits against | same build; `kotlinx.coroutines` tested outside this repository | [coroutines.md](coroutines.md) |
| `$<fn>COROUTINE$<n>` class names are not keyed on by IC, caches or symbolication | per-file counters; classes are private | [coroutines.md](coroutines.md), [incremental-compilation.md](incremental-compilation.md) |
| `@GCUnsafeCall` callees have the C++ signature the Kotlin declaration implies, and do not block or allocate in Runnable state | prototype derived from Kotlin; no check against the runtime; a drift is UB | [intrinsics.md](intrinsics.md) |
| Every `@ExportForCppRuntime` function is called in Runnable state | verified for the launcher path and by the `EnterFrame` assertion only | [intrinsics.md](intrinsics.md) |
| `IntrinsicType` strings in the stdlib and the compiler agree; `BackendNativeSymbols` resolve | same build; surfaces lazily in the first compilation that needs a kind | [intrinsics.md](intrinsics.md) |
| `@NoReorderFields` is only used on final classes without inherited fields | comment in `LlvmDeclarations.packFields`; unchecked | [intrinsics.md](intrinsics.md) |
| Runtime functions marked `RUNTIME_NOTHROW` really never throw | codegen emits `nounwind`; LLVM may delete landing pads | [llvm-passes.md](llvm-passes.md) |
| `kotlin-remove-sp` finds the prologue safepoint by literal name in the entry block | `NO_INLINE` in `mm/Memory.cpp` and codegen placement | [llvm-passes.md](llvm-passes.md) |
| `targetCpu`/`targetCpuFeatures` are compatible with how runtime bitcode was compiled | `runtime_attributes_compatible.kt` is disabled on Android arm64/x86 and Linux arm64 | [llvm-passes.md](llvm-passes.md), [targets-and-toolchains.md](targets-and-toolchains.md) |
| `-Xsave-llvm-ir-after=<Phase>:<pass>` is used single-threaded | `LLVMKotlinRunPasses` writes global `cl::opt`s | [llvm-passes.md](llvm-passes.md) |
| The `dev` and `user` LLVM distributions are the same LLVM version; `llvmVersion.<host>` equals the archive's `lib/clang/<ver>` | by construction of the distribution; libclang `-isystem` path breaks otherwise | [targets-and-toolchains.md](targets-and-toolchains.md), [llvm-passes.md](llvm-passes.md) |
| `cacheableTargets.<host>` matches the caches actually shipped in `klib/cache` | both from one build | [targets-and-toolchains.md](targets-and-toolchains.md), [test-infrastructure.md](test-infrastructure.md) |
| The first stage never touches `PlatformManager`, so klibs can be cross-compiled on any host | KGP defaults `enableKlibsCrossCompilation=true`; no test on a host without the target's toolchain | [targets-and-toolchains.md](targets-and-toolchains.md) |
| Extracted dependencies under `~/.konan/dependencies` are intact | `.extracted` markers are never re-validated | [targets-and-toolchains.md](targets-and-toolchains.md) |
| `osVersionMin` of a target is what the shipped platform libraries were built against | `watchos_device_arm64` hard-codes 9.0 while `minVersion.watchos` is 8.0 | [targets-and-toolchains.md](targets-and-toolchains.md), [compatibility-and-migration.md](compatibility-and-migration.md) |
| The ObjC wrapper of a Kotlin object keeps an NSObject refcount of exactly one until `releaseAsAssociatedObject` | nothing stops ObjC code from `[super retain]` or `objc_setAssociatedObject` with a wrapper as value | [export.md](export.md), [cinterop.md](cinterop.md) |
| Catch clauses in Kotlin only ever see `ExceptionObjHolder` | landing pads use a `null` catch-all without `typeid`; runtime discipline and filtering handlers at interop boundaries | [exceptions.md](exceptions.md) |
| `Kotlin_getCurrentStackTrace`'s skip count matches the `Throwable` constructor structure | one test (`kt49240_stack_trace_completeness.kt`) | [exceptions.md](exceptions.md) |
| Runtime global names agree between `NativeRuntimeOverridableConstants.kt` and `CompilerConstants.cpp` | hand-maintained | [exceptions.md](exceptions.md), [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| `Konan_DebugOperation` numbering is append-only and `ExtendedTypeInfo` keeps its position in `TypeInfo` | `konan_lldb.py` of one version drives binaries of another | [debug-info.md](debug-info.md) |
| `konan_lldb.py` finds `kclass:kotlin.String` in the selected frame's module | single-image programs | [debug-info.md](debug-info.md) |
| Function and bridge naming (`objc2kotlin_*`, `<compiler-generated>`, `<NODEBUG>`) is stable for the debugger scripts | hard-coded in `konan_lldb.py` and libbacktrace patch | [debug-info.md](debug-info.md), [export.md](export.md) |
| Every commonized `expect` has an implementation in each leaf | only the common slot is serialized; leaves are the original klibs; the frontend's handling is partly unverified | [commonizer.md](commonizer.md) |
| `klib/platform` content is what the distribution commonizer cache was built from | cache keyed by KGP version and `konanHome` path only | [commonizer.md](commonizer.md), [platform-libraries.md](platform-libraries.md) |
| `PlatformWidthIndex` lists every target | hand-maintained table; new targets silently excluded from platform-integer commonization | [commonizer.md](commonizer.md), [targets-and-toolchains.md](targets-and-toolchains.md) |
| Test modes passed through `-Pkn.*` produce the compiler flags the test author expects | `IGNORE_NATIVE: target=` values unvalidated; `gcScheduler=` never fires by default; forbidden-args check skipped for ext test data | [test-infrastructure.md](test-infrastructure.md) |
| `IGNORE_NATIVE` tests fail | enforced ("Test did not fail as expected"); `MUTED` tests are skipped instead | [test-infrastructure.md](test-infrastructure.md) |

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
- `userCacheFlavorString` appends `-pl` unconditionally although partial linkage cannot be switched off; KGP integration-test
  helpers hard-code the suffix ([caches.md](caches.md), [incremental-compilation.md](incremental-compilation.md)).

Coroutines ([coroutines.md](coroutines.md)):
- Suspend lambdas are never their own state machines on Native (`buildCoroutine(…, isSuspendLambdaInvokeMethod = false)`
  always), so every created coroutine goes through the `createCoroutineFromSuspendFunction` wrapper and
  `BaseContinuationImpl.create` is dead; the shared `CoroutineBuilder` supports the cheaper mode that JS uses.
- Late variable spilling (`ade70d52e6f4`, empty commit body) relies on an unenforced invariant (see cross-subsystem contracts).
- `DebugProbes` are empty but `probeCoroutineResumed` is called on every `resumeWith`; `ExpressionSlicer` is quadratic
  (TODO); spilled fields are never shared between variables with disjoint live ranges.
- `objcExportSuspendFunctionLaunchThreadRestriction` is read as `!= MAIN`, so an unset option means "any thread".

Exceptions ([exceptions.md](exceptions.md)):
- `KONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG` is referenced but defined nowhere (definitions removed in `de1dd47ab10a`,
  `2a395ad9c5fe`); `ReportBacktraceToIosCrashLog` and the `Kotlin_Throwable_getStackTrace` export are dead, as is
  `kotlin::DisallowSourceInfo()`.
- `SetKonanTerminateHandler()` pushes a handler on every `initRuntime()` (per attached thread) and every
  `setUnhandledExceptionHook`, so the terminate-handler stack grows.
- `ReportUnhandledException` prints the `Uncaught Kotlin exception:` prefix to stdout and the trace to stderr.
- `HandleCurrentExceptionWhenLeavingKotlinCode` is `RUNTIME_NORETURN` but returns normally for non-Kotlin exceptions.
- `Worker.execute` only prints exceptions (TODO), unlike `executeAfter`.

Debug info ([debug-info.md](debug-info.md)):
- `sourceInfoType` defaults to `noop` for every `-opt` build and every non-Apple `-g` build, so `Throwable` traces have no
  `file:line` there even when a line table exists; `<NODEBUG>` frame hiding only works under libbacktrace, not the Apple default.
- `needDebugInfo` gives `var` locals but not `val`s a `DILocalVariable` under light debug; looks accidental.
- Dead code: `ClassScope.members`/`debugFieldDeclaration` (also wrong offset arithmetic), `DebugInfo.module`,
  `Konan_DebugGetOperation` (declared, never defined), `konan_globals_command` (never registered).
- `konan_lldb.py`: `_TYPES` has 10 entries but `_TYPE_CONVERSION` 11 (a `VECTOR128` field raises `IndexError`);
  `Konan_DebugGetTypeNameImpl` leaks a C string per call; `DICreateBasicType` ignores `alignment`; `Boolean` DWARF size is
  `int8` for an `i1`; `SetupConfiguration.kt` misspells `-Xg-generate-debug-tramboline`; `dwarf_util.kt.pp` and the generated
  `Dwarf.kt` disagree on `DW_LANG_Kotlin` (0x0001 vs 0x0026). No FileCheck test covers debug metadata.

Intrinsics ([intrinsics.md](intrinsics.md)):
- `EscapeAnalysisChecker.mustBeLowered` and `IntrinsicGenerator` disagree on which kinds are lowered
  (`CREATE_UNINITIALIZED_INSTANCE`/`_ARRAY`, `CREATE_EMPTY_STRING`, `IDENTITY` listed as lowered but generated;
  `BLOCK_PTR_TO_FUNCTION_OBJECT` missing), so some EA annotations are reported unused instead of validated.
- `OBJC_INIT_BY` sits in the "special intrinsic" error branch although `InteropLowering.lowerObjCInitBy` lowers it.
- Dead annotations: `@kotlin.native.internal.Intrinsic`, `@LeakDetectorCandidate` (the compiler side of the
  `TF_LEAK_DETECTOR_CANDIDATE` collision above); `@Retain`/`@RetainForTarget` and `@Independent` have readers but no stdlib
  users.
- An unannotated `external fun` in user code is not a frontend diagnostic on Native; it dies in the second stage with an
  internal `Error` from `externalSymbolOrThrow`.

LLVM passes and toolchains ([llvm-passes.md](llvm-passes.md), [targets-and-toolchains.md](targets-and-toolchains.md)):
- `-Xllvm-variant=dev-with-asserts` maps to a `konan.properties` key that `LLVMDistributionSource` never writes, and the
  `gradle.properties` URLs for it point at the plain `dev` archives; probably unresolvable.
- `HideSymbolsPass` does not preserve `__stack_chk_*` (TODO); `-Xbinary=stackProtector` with `-produce static_cache` is untested.
- `libllvmext/CMakeLists.txt` lists the deleted `CoverageMappingC.cpp`; `RemoveRedundantSafepointsPass.kt` kdoc describes a gate
  that no longer exists; `GCSchedulerConfig.hpp` still says pages are 256 KiB.
- `DependencyProcessor.kt` reads `downloadingAttemptPauseMs` while `konan.properties` sets `downloadingAttemptIntervalMs`.
- Dead `konan.properties` keys and readers: `osVersionMinFlagLd.*` (14 entries, no reader), `reducedLlvmAppendix`,
  `lld`/`LldFlags` (reader, no entries), `runtimeDefinitions`, `Distribution.additionalPropertyFiles`;
  `defaultArchiveTypeByHost` still lists `MACOS_X64` after `HostManager` dropped it (`5292dc16be63`); a stale
  `WATCHOS_DEVICE_ARM64` TODO in `KonanTargetExtenstions.kt`; `Distribution.getBundleVersion` guesses the version from a `-1`
  substring of the path.

Optimizations ([escape-analysis.md](escape-analysis.md), [devirtualization.md](devirtualization.md)):
- With `escapeAnalysisPropagateExiledToHeapObjects=true` (default for all GCs since `f7d3b8074e7c`), an allocation that only
  flows into a parameter field or the return value becomes an escape origin in its own function (`Escapes: D0`), so callers
  cannot stack-allocate anything stored into such objects; the KDoc example in `escapeAnalysis/Annotations.kt` no longer holds for
  Kotlin-bodied callees. The commit targeted large local arrays; this looks like an unintended widening worth measuring.
- `squashEquivalentNodes` skips nodes whose only parent is a `Source`, so sibling return chains fed by the same constant are
  never merged; parentless `Ordinary` nodes all get signature 0.
- `processCall` logs `WARNING: There is no node N` for every `null` argument and leaves dangling field nodes that force a
  synthetic drain.
- CMS: `marked()` cannot tell grey from black, so the deletion barrier cannot skip black destinations (TODO in
  `cms/Barriers.cpp`) ([gc.md](gc.md)).

Interop and export ([cinterop.md](cinterop.md), [export.md](export.md)):
- `releaseAsAssociatedObjectImp`/`deallocImp` in `ObjCInterop.mm` dispatch to `super` with `// FIXME is this correct?`.
- `collectLlvmModules` drops cinterop klib bitcode only when `cCallMode` is exactly `Direct`; `DirectOrIndirect` links it even
  when every call resolved directly.
- `_Konan_init_<lib>` constructors: an exception from a merged runtime initializer escapes a `generateFunctionNoRuntime`
  function (TODO "shall we put that into the try block?") ([static-initializers.md](static-initializers.md)).

Commonizer ([commonizer.md](commonizer.md)):
- `native-dist-commonize` declares `-targets`, `-copy-stdlib`, `-copy-endorsed-libs` and never reads them; the Xcode 14
  CoreGraphics `allowedDuplicates` hack (`289f540530dc`) is still present; `@kotlin.commonizer.ObjCCallable` is emitted into
  every commonized ObjC callable but declared nowhere in the repository; `CallableValueParametersCommonizer.patchCallables`
  mutates its input declarations; top-level `@Deprecated` functions are dropped at deserialization with no recorded reason;
  `native/commonizer/README.md` is stale (claims no user-cinterop support and shows `actual` klibs that are not produced).

Test infrastructure ([test-infrastructure.md](test-infrastructure.md)):
- `IGNORE_NATIVE: target=` values are not validated (a typo never matches); `TestCompilerArgs.findForbiddenArgs` is applied only
  to predefined test cases; `computeKlibIrInlinerMode` ignores its argument (KT-81022) yet injects two `-XXLanguage:` flags into
  every test; `IGNORE_NATIVE: gcScheduler=` can never fire by default; `NativeCodegenSourceTransformers` and `fromGTestPattern`
  are duplicated without a sync test; `invokeCInterop` cannot capture cinterop output; `-Pkn.compilerOutputInterceptor` is not
  forwarded; `kn.ignoreFailures` does not exist; `MUTED` tests never report an unexpected pass. `TestListing.kt` KDoc and the
  `TestRun.kt` sync comment are stale. HACKING.md's property table was corrected in this round where the code disagreed.

## Known gaps

Not covered, or only by a pointer:
- Swift export internals (skipped on purpose; [docs/swift-export](../swift-export/README.md) exists) and the Analysis-API-based
  ObjC header generator.
- Benchmarks (`kotlin-native/performance`), the XCTest launcher, the Breakpad crash handler.
- Dynamic caches (not built automatically, no test mode) and `-Xcompile-from-bitcode` beyond their entry points.
- `kotlinx.coroutines` internals (outside this repository); only what the compiler and stdlib guarantee is documented.
- Sanitizer builds (`-Xbinary=sanitizer`): how the instrumented runtime reaches the link is unverified.
- `-Xllvm-variant` default: `konan.properties` points `llvmHome.<host>` at the `dev` slot while HACKING.md says `user`; which
  one a shipped distribution resolves to was not settled.

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
- The `TF_*` flag collision above; the `ShadowStack.hpp` "incompatible" TODO. (`@ExportForCompiler`'s consumer is the mangler's
  `ANNOTATIONS_TO_TREAT_AS_EXPORTED`; `ExtendedTypeInfo` is consumed by `debug.bc` only — both resolved in
  [intrinsics.md](intrinsics.md) and [debug-info.md](debug-info.md).)

### coroutines.md
- Does `-opt` fold the `COROUTINE_SUSPENDED` getter into a load; does `-Xphases-to-dump-after=CoroutinesVarSpillingLowering`
  work for a lowering inside `runCodegen`; could EA stack-allocate a coroutine object whose `this` never escapes.
- Why suspend lambdas do not use the lambda-as-state-machine mode; the IC test `suspendFunChange` is disabled on MinGW with no
  recorded reason.

### exceptions.md
- Which component reports a Kotlin exception escaping a Swift-export bridge without `@Throws`; rationale for having no landing
  pads in Kotlin frames and for the `typeid`-less catch-all.

### debug-info.md
- How Fleet/AppCode/Xcode locate `konan_lldb.py`; what lldb shows for a suspended coroutine; why `-Xdebug-info-version=1`
  (DWARF 2, `DW_LANG_C89`) is still the default and the trampoline is off by default.

### intrinsics.md
- Whether every C++ call site of an `@ExportForCppRuntime` function runs Runnable; why `@CanBePrecreated` has `SOURCE`
  retention; whether `@Independent` is still produced for shipped klibs; whether `@ExportForCppRuntime` on a constructor works.

### llvm-passes.md
- Contents of LLVM's `lto<O0>` pipeline; whether any TBAA/`invariant.load`/`dereferenceable` metadata is emitted (grep found
  none); the `dev` vs `user` default (see Known gaps).

### targets-and-toolchains.md
- How `-Xbinary=sanitizer=thread` obtains an instrumented runtime; whether `tools/scripts/update_xcode.sh` is the canonical way to
  build Apple sysroot archives; target tiers are external to the code.

### commonizer.md
- Separate KMP compilation (`-Xfragment-dependencies`): can a platform fragment see both the commonized `expect` and the platform
  class, and which wins; where the `kotlin.PlatformInt` support library lives; who consumes `@kotlin.commonizer.ObjCCallable`.

### test-infrastructure.md
- Whether the thread-state checker works with `STATIC_ONLY_DIST` (HACKING says `cacheMode=NO` is required; code only asserts
  `DEBUG`); whether KGP's `KotlinNativeTest` uses the same `--ktest_*` protocol; the `LLDB_TRACE` directive does not exist.

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
