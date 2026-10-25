# Knowledge base status

Coverage, known gaps and open questions for the Kotlin/Native knowledge base in this directory. Update it when you add a page,
verify a claim, or resolve a question (then also remove the question from the page's "Open questions").

## Coverage

| Page | Covers | Verified against |
|------|--------|------------------|
| [README.md](README.md) | index, reading paths, conventions | — |
| [architecture.md](architecture.md) | module map with Gradle paths, module dependencies, two-stage overview, distribution | build scripts, `K2Native.kt`, `TopLevelPhases.kt` |
| [compilation-model.md](compilation-model.md) | user-visible model: klibs, binaries, caches, interop, export (pre-existing) | — |
| [second-stage-pipeline.md](second-stage-pipeline.md) | CLI stage selection, `KonanDriver`, per-output-kind flows, fragments, `runCodegen` order, phase contexts, flags | `K2Native.kt`, `KonanDriver.kt`, `NativeCompilerDriver.kt`, `TopLevelPhases.kt`, `Machinery.kt`, generated argument classes |
| [lowerings.md](lowerings.md) | both lowering stages, synchronization points, per-file invariant, gated lowerings, validation, catalogue of Native lowerings | `NativeLoweringPhases.kt`, `lower/*`, `cli-native-klib`, `NativeLoweringPrerequisitesTest` |
| [optimizations.md](optimizations.md) | DataFlowIR, root set, devirtualization, DCE, escape analysis, GHA, pre-codegen inliner, gating | `optimizations/*`, `TopLevelPhases.kt`, FileCheck tests |
| [llvm-codegen.md](llvm-codegen.md) | IR → LLVM IR, declarations and naming, RTTI emission, debug info, runtime module linking, LLVM pipelines, object files, linker | `llvm/*`, `CompilerOutput.kt`, `OptimizationPipeline.kt`, `Linker.kt`, `libllvmext` |
| [compiler-runtime-contract.md](compiler-runtime-contract.md) | object layout, TypeInfo, boxing, runtime entry points and annotations, thread states, GC roots, initializers, exceptions, binary options → constants | `TypeInfo.h`, `Memory.h`, `RTTIGenerator.kt`, `Runtime.kt`, `ContextUtils.kt`, `CompilerConstants.hpp`, stdlib annotations |
| [runtime.md](runtime.md) | lifecycle, thread states and STW, roots, allocation, GC variants and barriers, scheduler, finalizers, logging, variants and tests | `runtime/src/main/cpp/mm/*`, `gc/*`, `gcScheduler/*`, `alloc/*`, gtests |
| [caches.md](caches.md) | `CacheBuilder`, cache layout and flavors, per-file caches, additional cache info, dependency tracking, cache ABI, prelink, test modes | `CacheBuilder.kt`, `CachedLibraries.kt`, `CacheStorage.kt`, `DependenciesTracker.kt`, `CachesAbiLowering.kt`, test settings |
| [klib-linking.md](klib-linking.md) | first-stage klib production, resolution, K1 descriptors, IR deserialization strategies, partial linkage | `cli-native-klib`, `serialization.native`, `K1Frontend.kt`, `linkKlibs.kt`, `serialization/*` |
| [cinterop.md](cinterop.md) | cinterop tool, `.def` files, cinterop klibs, metadata → IR, interop lowerings and C stubs, call modes, ObjC interop, platform libs | `Interop/*`, `cinterop.deserialization`, `InteropLowering.kt`, `CStubsManager.kt`, `platformLibs` |
| [export.md](export.md) | ObjC export flow and codegen, runtime side, exposure and naming rules, C export, Swift export hooks | `NativeCompilerDriver.kt`, `objcexport/*`, `llvm/objcexport/*`, `ObjCExport.mm`, `cexport/*` |
| [debugging.md](debugging.md) | symptom → subsystem → flags → tests | the pages above, `LoggedData.kt`, `BinaryOptions.kt` |
| [glossary.md](glossary.md) | terms with pointers to the canonical page | the pages above |

## Highest-value entry points

- Orientation: [architecture.md](architecture.md) → [second-stage-pipeline.md](second-stage-pipeline.md).
- Any change touching both `llvm/` and `runtime/`: [compiler-runtime-contract.md](compiler-runtime-contract.md).
- Miscompilation: [lowerings.md](lowerings.md) (debug and release) then [optimizations.md](optimizations.md) (release only).
- Crash in a user program: [debugging.md](debugging.md) → [runtime.md](runtime.md).
- "Works without caches": [caches.md](caches.md).

## Cross-subsystem contracts

Facts that span pages and are the usual cause of "I changed X and Y broke". Each is explained on the linked page.

- **The compiler and the runtime encode the ABI twice.** `TypeInfo`, `ObjHeader`, `ExtraObjectData`, the init-phase constants
  and `KotlinObjCClassInfo` have a C++ and a Kotlin definition; `compiler_interface.bc` only supplies struct layouts and
  function signatures by name. [compiler-runtime-contract.md](compiler-runtime-contract.md)
- **Stage-1 lowering output is klib ABI**; the stage-2 lowering lists and the `NativeInlineFunctionResolver` copy of list 1 must
  stay in sync. [lowerings.md](lowerings.md), [klib-linking.md](klib-linking.md)
- **Whole-program optimizations are off with any static cache** (`runGlobalOptimizations`), GHA also when producing a cache;
  release builds ignore caches unless `-Xbinary=enableReleaseBinaryCache=true`. [optimizations.md](optimizations.md),
  [caches.md](caches.md)
- **Everything that influences codegen must be in the cache flavor** (`appendCommonCacheFlavor`): `NativeRuntimeConstants`,
  GC/allocator choices, Swift export. Class field layout and inline-body indices are stored in caches, so layout rules and the
  klib IR format invalidate them. [caches.md](caches.md), [compiler-runtime-contract.md](compiler-runtime-contract.md)
- **Codegen pattern-matches lowering origins** (`DECLARATION_ORIGIN_BRIDGE_METHOD`, `StaticInitializersOrigins`,
  `DECLARATION_ORIGIN_COROUTINE_IMPL`, `IR_DECLARATION_ORIGIN_VOLATILE`) and expects erased call types and explicit box/unbox.
  [lowerings.md](lowerings.md), [llvm-codegen.md](llvm-codegen.md)
- **Lowerings that create declarations must run before `BuildDFG`** (sealed DFG symbol table). [optimizations.md](optimizations.md)
- **The runtime module list is duplicated** in `RuntimeModule.kt` and `runtime/build.gradle.kts`; `-Xruntime-logs` tags and
  levels are duplicated in `RuntimeLogging.kt` and `Logging.hpp`. [llvm-codegen.md](llvm-codegen.md), [runtime.md](runtime.md)
- **Only interop bridges switch thread state**; every C/ObjC/Swift → Kotlin entry goes through `CalledFromNativeGuard`
  (lazy runtime init + Runnable). Stack-allocated objects are tagged and must never be referenced from the heap or an
  `ExternalRCRef`. [compiler-runtime-contract.md](compiler-runtime-contract.md), [runtime.md](runtime.md), [export.md](export.md)
- **Frameworks and libraries have no entry point**, so every public function and vtable/itable member is a DCE root; exported API
  survives implicitly. [optimizations.md](optimizations.md), [export.md](export.md)
- **ObjC export header and code spec agree by construction** (same `ObjCExportedInterface`), not by a check; adapters are
  emitted per final binary and serialized into caches. [export.md](export.md), [caches.md](caches.md)
- **cinterop klibs carry metadata only**; IR is synthesized at link time, duplicated declarations across cinterop klibs resolve to
  the first deserialized (KT-89825), and `.def` `linkerOpts` / `staticLibraries` reach the link only for klibs that contributed
  code. [cinterop.md](cinterop.md), [klib-linking.md](klib-linking.md)
- **Per-file stdlib caches**: the `Runtime.kt` fragment must own everything the C++ runtime references by name. [caches.md](caches.md)
- **Partial-linkage issues are stored in caches and replayed** by consuming compilations (KT-78253). [klib-linking.md](klib-linking.md)

## Known gaps

Not covered, or only by a pointer:
- Swift export internals ([docs/swift-export](../swift-export/README.md) exists), the klib commonizer, and the Analysis-API-based
  ObjC header generator.
- The Kotlin Gradle plugin side of Native (tasks, cinterop and cache configuration, incremental compilation driving) beyond the
  few flags noted in caches.md.
- Test infrastructure internals beyond [HACKING.md](../../kotlin-native/HACKING.md#test-infrastructure-overview).
- The platform-library build tooling (`kotlin-native/build-tools`), benchmarks (`kotlin-native/performance`), XCTest launcher,
  the LLDB plugin (`llvmDebugInfoC`, `runtime/src/debug`), Breakpad crash handler.
- Dynamic caches (not built automatically, no test mode) and `-Xcompile-from-bitcode` beyond their entry points.
- No page explains the `kotlin.native.internal` intrinsics as a set; they are mentioned where a subsystem uses them.

## Open questions

Collected from the pages; each is also listed at the end of its page.

### second-stage-pipeline.md
- Which phases are safe to disable with `-Xdisable-phases` beyond optimization and verification phases.
- Whether file order after `mergeDependencies` still matters (TODOs in `partiallyLowerModuleWithDependencies`).

### lowerings.md
- Why `TestProcessor`, `UpgradeCallableReferences`, `NativeAssertionWrapperLowering` appear in both stage lists (hypothesis:
  compatibility with klibs produced before the stage-1 lowering existed); idempotence of `UpgradeCallableReferences` unverified.
- Whether the "reverse topological order" TODO is a real initializer-order bug or harmless under lazy file initialization.
- Semantics of Native `-Xprint-ir` / `-Xprint-files` not traced.
- `-Xphases-to-dump=IrLowering` (corner phases) is likely inert for Native since `K2Native` passes no `phasesToExecute`.
- Whether per-file dumps of the same phase collide (`PhaserState.phaseCount`, TODO in `DumperVerifier.kt`).
- Compatibility of `checkUnboundSymbols = true` in the before-lowerings validation with partial-linkage stubs across cache modes.

### optimizations.md
- Why GHA is allowed for frameworks/dynamic libs but not caches (only `produce.isCache` is checked; the framework restriction is
  only in the codegen fast path).
- Library-mode EA uses non-devirtualized unfold factor -1 with a TODO about private virtual functions; unverified whether STACK
  is ever produced for objects passed to virtual calls in libraries.
- KT-72336: intended final ordering of `PreCodegenInliner` vs `RemoveRedundantCallsToStaticInitializersPhase` undocumented.
- `TAKE_NAMES` true in `SymbolTable`, false in `DevirtualizationAnalysis`; EA's `kfun:kotlin.` prefix check depends on names.
- No recorded rationale for `allowedToAlloc = 65536` or the unfold factors.
- Whether ObjC export bridges can reference a private Kotlin method removed by DCE in library mode.

### llvm-codegen.md
- Whether `Lifetime.LOCAL` / `SlotType.ARENA` is still produced by escape analysis (`FunctionGenerationContext.call` throws on
  `ARENA`; arena code is commented out).
- Full list of phases accepting `-Xsave-llvm-ir-after`.
- What `cCallMode = Direct` changes in emitted cinterop calls beyond skipping the klib bitcode.
- Hypothesis: `runLLVMPassesInCompiler` is a temporary gate; the Kotlin and C++ implementations of hide-symbols and
  remove-safepoints must stay equivalent meanwhile.
- Interaction of `-g` with `-opt` across `Linker` and `BitcodeCompiler`.

### compiler-runtime-contract.md
- Who consumes `@ExportForCompiler`? No backend lookup of the annotation was found.
- `TF_LEAK_DETECTOR_CANDIDATE = 16` and `TF_HAS_FREEZE_HOOK = 128` in `RTTIGenerator.kt` have no runtime counterpart; the
  runtime uses bit 16 for `TF_KOTLIN_OBJC_CLASS`. Dead constants or latent conflict?
- `ShadowStack.hpp` TODO says stack objects are "incompatible"; codegen stores them in root slots. Hypothesis: stale TODO.
- Exact consumers of `ExtendedTypeInfo` beyond `debug.bc` (emitted only in non-optimized builds).
- Whether `SlotType.ARENA` / `PARAM_IF_ARENA` lifetimes still produce distinct code.

### runtime.md
- Any caller of `Init_and_run_start` with `memoryDeInit=0`, or any remaining option for runtime teardown at exit? None found.
- `Platform.isMemoryLeakCheckerActive` exists in stdlib but no runtime leak checker was found.
- Lock ordering (gcLock → ThreadRegistry → GlobalsRegistry / ExternalRCRefRegistry spin locks) is inferred, not documented.
- What `GC.collect()` does under `gc=noop` / manual scheduler was not traced.
- `AppStateTracking`: only consumer found is the adaptive scheduler timer.
- XCTest launcher runtime init path (`Konan_create_testSuite`) and `src/debug` (LLDB plugin support) not examined.

### caches.md
- How `reportLog` cache messages ("IC analysis results", "CACHING ...") reach users (hypothesis: `-verbose` / Gradle `--info`).
- Why the `KFunctionImpl.kt` fragment specifically owns the function-interface files (mechanism known, rationale not in code).
- Whether native binaries bundled in a cached cinterop klib end up inside the static cache archive.
- Dynamic caches: nothing builds them automatically and no test mode exercises them; support level unverified.
- No cleanup of old fingerprinted auto-cache directories was found in the compiler; KGP side not checked beyond `KotlinNativeLink`.

### klib-linking.md
- Is `-Xpartial-linkage={enable|disable}` intentionally ignored (only the log level reaches `PartialLinkageConfig`)?
- Are any Native-specific IR attributes serialized into klib IR? `NativeIrAttributes.kt` has only in-memory `irFlag`s.
- Exact root set deserialized under `EXPLICITLY_EXPORTED` for non-exported klibs not traced end to end.
- Whether the `TopologicalLibraryOrder` TODOs in `TopLevelPhases.kt` still describe a real reversal now that order comes from
  `KlibDAG`.
- `KlibDAGBuilder` fallback for klibs without a signature index not read in detail.

### cinterop.md
- Whether any IDE / Analysis API flow runs the cinterop tool (no references found; the IDE consumes metadata only).
- Future of `entryPoint=` and `-Xcompile-source` (indirect-only, KT-79747 / KT-79749).
- Why platform libs remain `-Xccall-mode indirect` (only KT-82062 cited).
- Determinism of `includedHeaders` for non-sysroot headers (content hash) and whether KGP / commonizer rely on it.
- Whether compiling all non-C++ stubs as `.m` on Apple can miscompile pure-C bridges (TODO in `CStubsManager`).
- Hypothesis: duplicate declarations across cinterop klibs resolve to the first klib in `KlibDAG` order.
- Exact ordering dependency between `InteropLowering` and `SpecialInteropIntrinsicsLowering`.

### export.md
- Any consumer of the analysis-api header generator besides IDE packaging (ReadMe mentions Fleet)?
- The runtime does not detect ObjC class-name clashes between frameworks with the same prefix; exact behaviour unverified.
- `ObjCExportLazy` (`-Xemit-lazy-objc-header`) has "partially duplicated" exposure logic; no test compares it with
  `ObjCExportMapper`.
- How `SerializedObjCAdapter` / `objCAdapters` are consumed when building caches for a framework was not traced.
- Whether `BuildCExportsFromIr` is meant to replace the K1 path (goldens require byte-identical output).
- Status of the TODO about emitting RTTI per module instead of whole-world.
