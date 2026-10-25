# Kotlin/Native glossary

Terms that recur in the K/N code base, commit messages and issues, with the page where each is explained. Terms are grouped by
area; within a group they are in the order you are likely to meet them.

## Compilation model and driver

| Term | Meaning | See |
|------|---------|-----|
| **konan**, **K/N** | The historical name of the Kotlin/Native compiler. It survives in package names (`org.jetbrains.kotlin.backend.konan`), `konan.properties`, `KONAN_DATA_DIR`, `konanc`, `_kexe`. | [architecture.md](architecture.md) |
| **First stage / second stage** | `.kt → .klib` (K2 frontend, LLVM-free) and `.klib → binary` (the backend). Selected by `-produce library` vs anything else. | [architecture.md](architecture.md#big-picture), [second-stage-pipeline.md](second-stage-pipeline.md) |
| **One-stage mode** | Sources plus a binary output kind on one command line: the CLI writes a temporary klib, then runs the second stage on it. Gradle never uses it. | [second-stage-pipeline.md](second-stage-pipeline.md#1-cli-entry-choosing-the-stage) |
| **Output kind** (`CompilerOutputKind`) | `-produce` value: `PROGRAM`, `STATIC`, `DYNAMIC`, `FRAMEWORK`, `TEST_BUNDLE`, `STATIC_CACHE`, `DYNAMIC_CACHE`, `HEADER_CACHE`, `LIBRARY`; the driver dispatches one flow per kind. | [compilation-model.md](compilation-model.md), [second-stage-pipeline.md](second-stage-pipeline.md#3-nativecompilerdriverrun-one-flow-per-output-kind) |
| **kexe** | File extension of a K/N executable; also the `_kexe` suffix added to a module name that collides with a library's. | [second-stage-pipeline.md](second-stage-pipeline.md#2-konandriverrun-preparing-the-compilation) |
| **Binary option** (`-Xbinary=name=value`) | Options that change the produced binary (GC, allocator, asserts, …). Defined in `native/binary-options`; defaults resolved in `NativeSecondStageCompilationConfig`. Non-default values become part of the cache flavor. | [second-stage-pipeline.md](second-stage-pipeline.md), [caches.md](caches.md) |
| **Fragment** (`BackendJobFragment`) | The unit of codegen: one IR module plus the state for one LLVM module. One per compilation, or one per file when building per-file caches. | [second-stage-pipeline.md](second-stage-pipeline.md#4-runbackend-fragments-lowerings-codegen) |
| **LLVM module specification** | Decides which IR modules/declarations are compiled into *this* LLVM module and which come from caches (`containsDeclaration` = "is it external?"). | [caches.md](caches.md), [llvm-codegen.md](llvm-codegen.md) |
| **Phase** | A `NamedCompilerPhase`. Lowering phases are named after the lowering class; the name is what `-Xdisable-phases`, `-Xphases-to-dump-after`, `-Xlist-phases` use. | [second-stage-pipeline.md](second-stage-pipeline.md#key-concepts) |
| **Phase context** | What a phase can see: `NativeBackendContext` (whole compilation), `NativeGenerationState` (one fragment), `LinkKlibsContext`, `BitcodePostProcessingContext`. | [second-stage-pipeline.md](second-stage-pipeline.md#contexts-what-each-phase-can-see) |
| **Distribution** (`dist`, `konanHome`) | `kotlin-native/dist`: compiler jar, runtime bitcode, stdlib and platform klibs, prebuilt caches, `konan/konan.properties`. Tests and KGP load the compiler from here. | [architecture.md](architecture.md#distribution), [targets-and-toolchains.md](targets-and-toolchains.md#the-distribution-layout-and-discovery) |
| **Platform libraries** | Prebuilt cinterop klibs for the target's system libraries (`klib/platform/<target>`), added by default unless `-no-default-libs`. | [cinterop.md](cinterop.md), [klib-linking.md](klib-linking.md), [platform-libraries.md](platform-libraries.md) |

## Klibs and linking

| Term | Meaning | See |
|------|---------|-----|
| **Included library** (`-Xinclude`) | A klib compiled as part of the main module: fully deserialized, its descriptors are "source" modules. One-stage mode uses it for the intermediate klib. | [klib-linking.md](klib-linking.md) |
| **Exported library** (`-Xexport-library`) | A klib whose whole public API is deserialized (strategy ALL) so it can be exposed through ObjC/C export. Opposed to a merely linked dependency. | [klib-linking.md](klib-linking.md), [export.md](export.md) |
| **Header klib** | A klib holding only the public ABI (`publicAbiOnly`, manifest `header=true`), produced with `-Xheader-klib-path`. | [klib-linking.md](klib-linking.md) |
| **Metadata klib** | `-Xmetadata-klib` output: metadata only, no IR, no `depends`. Used for shared source sets; the commonizer's output is of this kind too. | [klib-linking.md](klib-linking.md), [commonizer.md](commonizer.md) |
| **cinterop klib** | A klib produced by the cinterop tool: metadata only (`interop=true`, `ir_provider=kotlin.native.cinterop`) plus compiled C bridges; IR is synthesized at link time. | [cinterop.md](cinterop.md) |
| **Forward declarations module** | Synthetic klib-less module for `cnames.structs.*` / `objcnames.*`, stubbed on demand. | [klib-linking.md](klib-linking.md) |
| **KlibDAG** | Dependency graph between loaded klibs computed from signature indices, not from IR links; source of the reverse topological module order. | [klib-linking.md](klib-linking.md) |
| **Deserialization strategy** | Per-file policy in the IR linker: `ALL`, `EXPLICITLY_EXPORTED`, `ONLY_DECLARATION_HEADERS`, `WITH_INLINE_BODIES`, `ON_DEMAND`; chosen by library role and cache state. | [klib-linking.md](klib-linking.md) |
| **K1 descriptors** | The second stage still builds descriptor modules for every klib (`runK1Frontend`); needed by IR linking, the symbol table and the K1-based export paths. | [klib-linking.md](klib-linking.md) |
| **Partial linkage (PL)** | Replacing unresolvable declarations with stubs that throw `IrLinkageError` at run time instead of failing the build. Issues found while building a cache are stored in it and *replayed* by later compilations. | [klib-linking.md](klib-linking.md), [caches.md](caches.md) |

## Lowerings

| Term | Meaning | See |
|------|---------|-----|
| **Pre-serialization lowering** | A first-stage lowering run before klib IR serialization; its output is part of every klib (klib ABI). | [lowerings.md](lowerings.md) |
| **Lowering list** | The two Kotlin lists in `NativeLoweringPhases.kt` (`getLoweringsUpToAndIncludingSyntheticAccessors`, `getLoweringsAfterInlining`) that define the second-stage order. | [lowerings.md](lowerings.md) |
| **Lowering synchronization point** | A boundary in `runAllLowerings` where every file of every fragment has been lowered to the same list end; validation and the all-functions inliner run only there. | [lowerings.md](lowerings.md) |
| **`FileLowerState`** | Per-file counters for synthesized names (coroutines, function references, C stubs), reset before each file. | [lowerings.md](lowerings.md) |
| **`@PhasePrerequisites`** | Annotation declaring which lowerings must run earlier; checked only by `NativeLoweringPrerequisitesTest`. | [lowerings.md](lowerings.md) |
| **`genericSafeCasts`** | Binary option (default: on without `-opt`) enabling `CastsOptimization` and the `BOX_UNBOX_CASTS` bridges policy. | [lowerings.md](lowerings.md) |

## Whole-program optimizations

| Term | Meaning | See |
|------|---------|-----|
| **DataFlowIR / DFG** (`ModuleDFG`) | Control-flow-free per-function graph of calls, allocations, field accesses and variables over a sealed symbol table; shared by devirtualization, DCE, escape analysis and the inliner. | [optimizations.md](optimizations.md) |
| **Root set** | Functions assumed callable from outside: only the entry point for programs; all public/exported functions and vtable/itable members for every other output kind. | [optimizations.md](optimizations.md) |
| **Devirtualized call site** | `IrCall.devirtualizedCallSite` attribute holding the possible (receiver type, callee) pairs computed by VTA. | [optimizations.md](optimizations.md) |
| **Unfold factor** | Maximum number of candidate callees for which a virtual call is expanded into direct calls. | [optimizations.md](optimizations.md) |
| **GHA** (Global Hierarchy Analysis) | Assigns class interval ids and colored interface ids, enabling O(1) subtype checks (`IsSubclassFast`) and interface dispatch. Off with caches. | [optimizations.md](optimizations.md) |
| **Lifetime** | Escape-analysis verdict per allocation (`STACK`, `STACK_ARRAY`, `GLOBAL`, …) consumed by codegen to choose stack vs heap allocation and the result slot. | [optimizations.md](optimizations.md), [llvm-codegen.md](llvm-codegen.md) |
| **`@Escapes` / `@PointsTo`** | Hand-written escape-analysis summaries required on stdlib `external fun`s with object parameters; enforced by `EscapeAnalysisChecker` in the first stage. | [optimizations.md](optimizations.md), [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **`Type.Virtual`** | DFG marker type for ObjC and forward-declared classes that disables devirtualization. | [optimizations.md](optimizations.md) |

## Codegen and the compiler–runtime contract

| Term | Meaning | See |
|------|---------|-----|
| **ObjHeader** | The one-word object header: a tagged pointer to `TypeInfo` or to the meta-object; low bits tag HEAP (0), PERMANENT (1), STACK (3). | [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **TypeInfo** | Per-class constant (layout, supertypes, vtable, itable, flags) emitted by `RTTIGenerator.kt` and mirrored by `TypeInfo.h`. Both must change together. | [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **Meta-object** (`ExtraObjectData`) | Lazily installed side record (weak refs, ObjC associated object, flags) that replaces the header pointer; its first field is the `TypeInfo*` so `type_info()` keeps working. | [compiler-runtime-contract.md](compiler-runtime-contract.md), [runtime.md](runtime.md) |
| **Permanent object** | Compile-time constant object (string literal, box cache entry, singleton) with tag 1; never collected. | [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **itable** | Per-class interface table of `InterfaceTableRecord {id, vtableSize, vtable}`; perfect-hashed by colored id, or sorted with binary search when `interfaceTableSize_ < 0`. | [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **Shadow stack / frame / slot** | Per-function array of object-pointer slots (prefixed by `FrameOverlay`) registered with `EnterFrame`/`LeaveFrame`; the GC's stack roots. Every local object reference lives in a slot. | [compiler-runtime-contract.md](compiler-runtime-contract.md), [llvm-codegen.md](llvm-codegen.md) |
| **Return slot** (`OBJ_GETTER`) | Hidden trailing `ObjHeader**` parameter of every object-returning function; the callee stores its result there with `UpdateReturnRef`. | [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **Virtual trampoline** | Function under a method's public `kfun:` name that performs vtable/itable dispatch; the real body is `<name>-impl`. Decouples cached callers from vtable layouts. | [llvm-codegen.md](llvm-codegen.md) |
| **`compiler_interface.bc`** | Runtime bitcode module of `touchType`/`touchFunction` symbols that the compiler parses to learn C++ struct layouts, the data layout and function signatures. | [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **Runtime module** | One `.bc` per runtime component (GC, scheduler, allocator, …); `CompilerOutput.kt` selects them from binary options. The list is duplicated in `RuntimeModule.kt` and `runtime/build.gradle.kts`. | [llvm-codegen.md](llvm-codegen.md), [runtime.md](runtime.md) |
| **Mandatory / module / LTO pipelines** | The three always-on LLVM pass pipelines (`OptimizationPipeline.kt`): `kotlin-hide-symbols` + `objc-arc-contract`; `default<O?>`; `internalize,globaldce,lto<O?>`. | [llvm-passes.md](llvm-passes.md), [llvm-codegen.md](llvm-codegen.md) |
| **libllvmext** | C++ extension of LLVM's C API shipping `LLVMKotlinRunPasses` and the `kotlin-*` passes (`kotlin-hide-symbols`, `kotlin-remove-sp`, `kotlin-ssp`, `kotlin-tsan`, `kotlin-calls-checker*`). | [llvm-passes.md](llvm-passes.md), [llvm-codegen.md](llvm-codegen.md) |
| **Symbol prefixes** | `kfun:` functions, `kclass:` TypeInfo, `kvar:` globals, `ktypew:` writable type info, `kobjref:` object refs, `kintf:` / `krefs:` / `kifacetable:` RTTI arrays; `<fq>#internal` for non-exported. | [llvm-codegen.md](llvm-codegen.md) |

## LLVM pass pipeline

| Term | Meaning | See |
|------|---------|-----|
| **Pass-pipeline string** | LLVM's textual pipeline syntax (`default<O3>`, `lto<O1>`, `function(kotlin-remove-sp<inline>)`) that each `LlvmOptimizationPipeline` hands to `PassBuilder::parsePassPipeline`; the same syntax `opt -passes=` takes. | [llvm-passes.md](llvm-passes.md#key-concepts) |
| **`LLVMKotlinRunPasses`** | libllvmext's replacement for `LLVMRunPasses` (`CAPIExtensions.cpp`): builds a `PassBuilder` with Kotlin tuning (`InlinerThreshold`, `MaxDevirtIterations = 0`), registers the `kotlin-*` passes, collects per-pass timings, plumbs `-print-after`. | [llvm-passes.md](llvm-passes.md#how-a-pipeline-string-is-executed) |
| **`inlineForPerformance`** | `!debug && !smallBinary` (`NativeSecondStageCompilationConfig.kt`); gates `performance_inline → alwaysinline` on runtime functions and the `kotlin-remove-sp<inline>` variant. | [llvm-passes.md](llvm-passes.md) |
| **`PERFORMANCE_INLINE`** | Runtime macro (`Common.h`) on functions that must be inlined into Kotlin code in `-opt`; the compiler turns the annotation into `alwaysinline` only when `inlineForPerformance` holds (never with a stdlib static cache in the way). | [llvm-passes.md](llvm-passes.md#the-custom-passes), [runtime.md](runtime.md) |
| **`RuntimeLinkageStrategy`** (`-Xbinary=linkRuntime`) | `Raw` links the runtime `.bc` files as they are; `Optimize` merges them and runs mandatory + `default<O3>` on them first. Default `Optimize` only with `-g`. | [llvm-passes.md](llvm-passes.md) |
| **`kotlin-remove-sp`** | libllvmext pass (`RemoveRedundantSafepoints.cpp`) deleting redundant prologue safepoints; it matches the callee by the literal name `Kotlin_mm_safePointFunctionPrologue`, and `<inline>` also inlines the surviving one. | [llvm-passes.md](llvm-passes.md#the-custom-passes), [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **`LLVMKotlin` plugin** (`libLLVMKotlin`) | The libllvmext objects built as a standard LLVM pass plugin (`opt --load-pass-plugin`); used only by FileCheck tests and not shipped in the distribution. | [llvm-passes.md](llvm-passes.md) |
| **`FILECHECK_STAGE`** | Test directive naming the phase after which `-Xsave-llvm-ir-after` writes `out.<stage>.ll` for FileCheck (`CStubs`, `Codegen`, `LTOBitcodeOptimization`, …); forces a standalone test. | [test-infrastructure.md](test-infrastructure.md#filecheck-tests), [llvm-passes.md](llvm-passes.md) |

## Intrinsics and compiler-facing annotations

| Term | Meaning | See |
|------|---------|-----|
| **Typed intrinsic** | An `external fun` annotated `@TypedIntrinsic(kind)`; codegen never emits a call to it, the `kind` string selects a case in `IntrinsicGenerator.kt` or in a lowering. | [intrinsics.md](intrinsics.md#key-concepts) |
| **`IntrinsicType`** (intrinsic kind) | The `kind` strings, declared twice: `const val`s in the stdlib (`kotlin.native.internal.IntrinsicType`) and the compiler enum in `native/base/.../IntrinsicType.kt`, matched by `IntrinsicType.valueOf(kind)`. | [intrinsics.md](intrinsics.md) |
| **Lowered / generated / special intrinsic** | A kind a lowering must rewrite before codegen ("should be handled by previous lowering phase" otherwise); a kind `IntrinsicGenerator` emits code for; a kind evaluated before its arguments because they must be constants (`tryEvaluateSpecialCall`: `IMMUTABLE_BLOB`, `OBJC_GET_SELECTOR`). | [intrinsics.md](intrinsics.md#part-2--typedintrinsic-kinds-by-family) |
| **Constant-constructor intrinsic** | A constructor annotated `@ConstantConstructorIntrinsic(kind)` used only inside `IrConstantObject`; `evaluateConstantConstructorFields` fills the fields (`KCLASS_IMPL`, `OBJC_KCLASS_IMPL`). | [intrinsics.md](intrinsics.md) |
| **`@GCUnsafeCall(name)`** | Binds an `external fun` to a runtime C symbol (`Kotlin_*`) called directly in Runnable state; the LLVM prototype is derived from the Kotlin signature and never checked against the C++ one. | [intrinsics.md](intrinsics.md#outbound-gcunsafecall-and-symbolname), [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **`@ExportForCppRuntime(name)`** | Makes a Kotlin function callable from the C++ runtime under a fixed C name (`funSymbolNameImpl`); also a DCE/devirtualization root through `ANNOTATIONS_TO_TREAT_AS_EXPORTED`. | [intrinsics.md](intrinsics.md#inbound-exportforcppruntime) |
| **`@ExportForCompiler`** | Marks a declaration the compiler references by name: counted as exported by `KonanMangler.ANNOTATIONS_TO_TREAT_AS_EXPORTED`, hidden from ObjC headers by `hasExportForCompilerAnnotation`. | [intrinsics.md](intrinsics.md) |
| **`BackendNativeSymbols`** | Lazy table (`Ir.kt`) of about 150 stdlib `ClassId`/`CallableId`s that lowerings and codegen either emit calls to (`ThrowNullPointerException`, `initInstance`) or pattern-match on (`executeImpl`, `saveCoroutineState`). | [intrinsics.md](intrinsics.md#part-3--functions-recognized-by-symbol) |

## Debug information and the debugger

| Term | Meaning | See |
|------|---------|-----|
| **Light debug** (`-Xadd-light-debug`) | Locations only (`!dbg`, subprograms, files), no variables or types; default on for Apple targets, so every `-opt` Apple binary has a line table and a dSYM. | [debug-info.md](debug-info.md#step-1--modes-and-gating) |
| **`DebugInfo`**, **`libLLVMDebugInfoC`** | Per-fragment owner of the `DIBuilder`, compile unit and caches (`DebugUtils.kt`); the C shim over `llvm::DIBuilder` (`kotlin-native/llvmDebugInfoC`) for what the LLVM C API lacks (`inlinedAt` locations, transparent stepping). | [debug-info.md](debug-info.md) |
| **`LocationInfo`**, **`inlinedAt`** | `(scope, line, column, inlinedAt?)`, the compiler-side mirror of a `DILocation`; `inlinedAt` is itself a `LocationInfo`, so inline chains nest. | [debug-info.md](debug-info.md#step-4--locations) |
| **`InlinedBlockScope`** | The `CodeContext` codegen wraps around an `IrInlinedFunctionBlock`: a `FileScope` over the callee's file whose locations carry `inlinedAt` pointing at the call site. | [debug-info.md](debug-info.md#step-5--inline-functions-and-lambdas-the-inlinedat-model), [pre-codegen-inliner.md](pre-codegen-inliner.md) |
| **Transparent stepping** | `DISubprogram` flag `SPFlagIsTransparentStepping` (Apple LLVM only): lldb steps through the function into its callee; used for `@TransparentForDebugger` bridges and virtual trampolines (`-Xbinary=enableDebugTransparentStepping`). | [debug-info.md](debug-info.md) |
| **Debug trampoline** | An extra basic block with its own location before a `when` or an inlined block so a breakpoint on that line has an address; `-Xg-generate-debug-trampoline=enable`, off by default, ignored without `-g`. | [debug-info.md](debug-info.md) |
| **`<NODEBUG>`** | Name prefix on the subprograms of lowered `Throwable` constructors; the patched libbacktrace turns it into `is_nodebug` and stack traces hide those frames (CoreSymbolication does not). | [debug-info.md](debug-info.md), [exceptions.md](exceptions.md) |
| **`<compiler-generated>`** | Pseudo `DIFile` (directory `""`) given to bridges such as `objc2kotlin_*`; the lldb stop hook keeps stepping while the frame is in it. | [debug-info.md](debug-info.md#the-lldb-side-konan_lldbpy), [export.md](export.md) |
| **`ExtendedTypeInfo`** | Second field of `TypeInfo`: field count/offsets/types/names plus the table of debug operations; null in `-opt`. | [debug-info.md](debug-info.md#step-7--what-the-runtime-contributes-extendedtypeinfo-and-debugbc), [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **`Konan_Debug*`**, **`debug.bc`** | Weak exported C functions of the `debug` runtime module (linked only with `-g`), each dispatching through `ExtendedTypeInfo.debugOperations_` so the debugger works across runtime versions. | [debug-info.md](debug-info.md) |
| **`konan_lldb.py`** | The LLDB plugin shipped as `<dist>/tools/konan_lldb.py`: summary and synthetic-children provider for `ObjHeader *`, three commands, a stop hook. | [debug-info.md](debug-info.md#the-lldb-side-konan_lldbpy) |
| **`sourceInfoType`** | Binary option choosing the symbolicator for `Throwable` stack traces: `coresymbolication` (default with `-g` on Apple), `libbacktrace`, `noop` (default otherwise, i.e. every `-opt` and every non-Apple `-g` build). | [debug-info.md](debug-info.md#stack-traces-symbolication-at-run-time), [exceptions.md](exceptions.md#step-7--stack-traces-capture-symbolication-format) |

## Exceptions and stack traces

| Term | Meaning | See |
|------|---------|-----|
| **`ExceptionObjHolder`** | The C++ exception type carrying a Kotlin `Throwable` (an `OwningExternalRCRef` in `ExceptionObjHolderImpl`); its RTTI symbol `_ZTI18ExceptionObjHolder` is what generated landing pads filter on. | [exceptions.md](exceptions.md#key-concepts) |
| **Filtering exception handler** | `filteringExceptionHandler`: the landing pad placed around calls into foreign code; Kotlin exceptions pass, `NSException` may be wrapped (`objc-wrap`), everything else terminates. | [exceptions.md](exceptions.md#step-4--the-interop-boundary-filtering-landing-pads-and-foreign-exceptions) |
| **Cleanup landing pad** | `landingpad … cleanup` block that only pops the shadow-stack frame (and restores the thread state for bridges) and `resume`s; emitted only for functions foreign code may call (`needCleanupLandingpadAndLeaveFrame`). | [exceptions.md](exceptions.md#step-2--unwinding-through-kotlin-frames-landing-pads-and-the-shadow-stack) |
| **`SetCurrentFrame`** | Runtime call a catching landing pad makes right after `landingpad`: frames of unwound Kotlin functions are never popped individually, the catcher repairs the shadow stack in bulk (`CheckCurrentFrame` asserts it in debug). | [exceptions.md](exceptions.md), [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **`ForeignException`** | `kotlinx.cinterop.ForeignException(nativeException)`: the Kotlin wrapper of an `NSException` caught in `objc-wrap` mode. | [exceptions.md](exceptions.md), [cinterop.md](cinterop.md) |
| **Unhandled exception hook** | `kotlin.native.setUnhandledExceptionHook`; stored in `UnhandledExceptionHookHolder`, run by `Kotlin_runUnhandledExceptionHook` before termination; setting it also reinstalls the Kotlin terminate handler. | [exceptions.md](exceptions.md#step-6--unhandled-exceptions-and-termination) |
| **Terminate handler registry** | `TerminateHandlersRegistry` (`Exceptions.cpp`): a LIFO stack of `std::terminate_handler`s; the Kotlin handler unwraps `ExceptionObjHolder` and delegates everything else to the previous one. | [exceptions.md](exceptions.md) |
| **`effectiveThrowsClasses`** | The `@Throws` classes an exported method may surface as `NSError`, taken from the topmost overridden declaration; `CancellationException` for `suspend` without `@Throws`. | [exceptions.md](exceptions.md#step-5--the-objcswift-export-bridge), [export.md](export.md) |

## Coroutines

| Term | Meaning | See |
|------|---------|-----|
| **Tail suspend call** | A suspend call whose value is directly returned (or is the last statement of a `Unit` function) and is not inside `try`; needs no state machine (`TailSuspendCallsCollector`). | [coroutines.md](coroutines.md#step-1--classifying-suspend-calls) |
| **Coroutine class** (`$<fn>COROUTINE$<n>`) | `private class … : ContinuationImpl` (or `RestrictedContinuationImpl`) generated per suspend function with a non-tail suspend call; holds parameters, `label` and spilled locals. | [coroutines.md](coroutines.md#step-2--the-coroutine-class) |
| **`invokeSuspend`** | The state-machine method of the coroutine class, overriding `BaseContinuationImpl.invokeSuspend(Result<Any?>): Any?`; the only body `CoroutinesVarSpillingLowering` visits and the one function the pre-codegen inliner never inlines. | [coroutines.md](coroutines.md) |
| **`label`** | Field of type `NativePtr` holding the LLVM `blockaddress` to resume at (`indirectbr`); `null` means "not started". | [coroutines.md](coroutines.md#step-6--codegen) |
| **`IrSuspendableExpression`**, **`IrSuspensionPoint`** | Native-only IR nodes: the resumable body with its `suspensionPointId`, and one suspension point with `result` (save state, call, return if suspended) and `resumeResult` (restore state, unwrap `Result`). | [coroutines.md](coroutines.md#step-3--the-body-expressionslicer-and-the-suspension-nodes) |
| **`saveCoroutineState` / `restoreCoroutineState`** | `@TypedIntrinsic` placeholders inserted by the suspend lowering and replaced by field stores/loads by `CoroutinesVarSpillingLowering`; `SaveCoroutineState` DFG nodes in between. | [coroutines.md](coroutines.md#step-5--liveness-and-spilling), [dataflow-ir.md](dataflow-ir.md) |
| **`$completion`** | The continuation parameter appended to every suspend function that is not a state machine (origin `CONTINUATION`); the function's origin becomes `LOWERED_SUSPEND_FUNCTION` and its return type `Any?`. | [coroutines.md](coroutines.md#step-4--the-continuation-parameter) |
| **`liveVariablesAtSuspensionPoint`**, **`visibleVariablesAtSuspensionPoint`** | IR attributes on a suspension point written by `CoroutinesLivenessAnalysis` (or by its fallback when the phase is disabled: every visible variable) telling spilling what to store; not copied with the node (`copyByDefault = false`). | [coroutines.md](coroutines.md#step-5--liveness-and-spilling) |
| **`TF_SUSPEND_FUNCTION`** | `TypeInfo` flag (`1 << 5`) set by `RTTIGenerator` on `SuspendFunctionN` interfaces; `ObjCExport.mm` relies on it to recognise suspend function objects. | [coroutines.md](coroutines.md#interop-and-export), [compiler-runtime-contract.md](compiler-runtime-contract.md) |

## Runtime

| Term | Meaning | See |
|------|---------|-----|
| **Mutator** | A thread registered in `ThreadRegistry` that runs Kotlin code. GC, finalizer and timer threads are not mutators. | [runtime.md](runtime.md) |
| **Thread state** (kRunnable / kNative) | Runnable may touch the heap and must reach safepoints; Native must not touch the heap and the GC may run without it. Codegen switches state only around interop bridges. | [compiler-runtime-contract.md](compiler-runtime-contract.md), [runtime.md](runtime.md) |
| **Safepoint** | `mm::safePoint()`: a load of one global function pointer, non-null only while a `SafePointActivator` exists; the slow path runs scheduler/GC hooks and suspension. Emitted in function prologues and loop bodies. | [runtime.md](runtime.md) |
| **Stop-the-world (STW)** | All mutators parked at a safepoint or in Native; requested with `TryRequestThreadsSuspension`. The kNative→kRunnable transition is itself a safepoint, so Native threads are never signalled. | [runtime.md](runtime.md) |
| **Epoch** | One GC cycle number tracked by `GCStateHolder` (scheduled / started / finished / finalized). | [runtime.md](runtime.md) |
| **CMS / PMCS / STMS / noop** | The GC implementations: Concurrent Mark & Sweep (default), Parallel Mark Concurrent Sweep, Stop-the-world Mark & Sweep, none. Only CMS has a (deletion) write barrier. | [runtime.md](runtime.md) |
| **GC scheduler / pacer** | Decides when to trigger a GC: adaptive (default), aggressive (every safepoint; stress testing), manual. | [runtime.md](runtime.md) |
| **Mutator assists** | Scheduler policy that parks mutators at safepoints until the current epoch finishes once the heap passes its target. | [runtime.md](runtime.md) |
| **ExternalRCRef** | Reference-counted handle to a Kotlin object held from outside the heap (ObjC, Swift, C stable pointers); rc > 0 makes it a GC root, rc == 0 makes it weak. | [runtime.md](runtime.md), [export.md](export.md) |
| **Calls checker** | Sanitizer-like instrumentation (`checkStateAtExternalCalls`) that catches external calls made in the wrong thread state. | [compiler-runtime-contract.md](compiler-runtime-contract.md) |

## Caches

| Term | Meaning | See |
|------|---------|-----|
| **Compiler cache** | A precompiled klib (`.a`/`.dylib` plus `ir/` side files) reused by later second-stage compilations. Kinds: static, dynamic, header. | [compilation-model.md](compilation-model.md#compiler-caches), [caches.md](caches.md) |
| **Cache flavor**, **system / user cache flavor** | Directory name encoding target, debug/opt and every codegen-affecting option (`appendCommonCacheFlavor`). `systemCacheFlavorString` adds `-system` plus the runtime options (`runtime_debug`, `allocator`, `gc`, …); `userCacheFlavorString` adds `-user-pl` (`-swift_export` when enabled), e.g. `macos_arm64-gSTATIC-user-pl`. | [caches.md](caches.md#versioning-and-invalidation), [incremental-compilation.md](incremental-compilation.md) |
| **System cache** | Prebuilt caches of distribution libraries under `<dist>/klib/cache/<system flavor>`. | [caches.md](caches.md) |
| **Auto cache** | Cache of a klib under a `-Xauto-cache-from` root, built on demand into a fingerprinted directory. | [caches.md](caches.md) |
| **Per-file cache** | One object per klib file (`<name>-per-file-cache/<fileId>`), the basis of incremental second-stage compilation. | [caches.md](caches.md) |
| **Additional cache info** | The `ir/` side files (class fields, inline body references, eager-init files, trivial getters, ObjC adapters, PL issues) that let consumers skip IR deserialization. | [caches.md](caches.md) |
| **Cache ABI** | `INTERNAL_ABI_ORIGIN` accessors (`CachesAbiSupport`) exposing private state across object files; produced by `ExportCachesAbiVisitor`, used by `ImportCachesAbiTransformer`. | [caches.md](caches.md), [lowerings.md](lowerings.md) |
| **Bitcode dependency** (`bitcode_deps`), **strong vs weak** | Per-cache record of which libraries/files its code references; drives link-time cache selection and incremental dirtiness. A weak edge (`DependenciesTracker.FileDependency.weak`, e.g. a call through a virtual trampoline) is used only at link time and propagates dirtiness only from removed files. | [caches.md](caches.md), [incremental-compilation.md](incremental-compilation.md) |
| **Prelink** | `ld -r` of the main object with all static caches into one `.o` before the final link; `shouldPerformPreLink` = static-library output, final binary, `-Xpre-link-caches=enable` (Apple). | [caches.md](caches.md) |

## Export

| Term | Meaning | See |
|------|---------|-----|
| **Top-level name prefix** | Abbreviation of the framework name prepended to ObjC class names; forced to `Kotlin` under Swift export. | [export.md](export.md) |
| **Base method** | A Kotlin method overriding nothing exposed; ObjC selectors and bridges are computed per base method. | [export.md](export.md) |
| **MethodBridge / TypeBridge** | Header-generator description of how each parameter and return value crosses the boundary; shared with LLVM codegen. | [export.md](export.md) |
| **Type adapter** (`ObjCTypeAdapter`) | Per-class constant with ObjC name, TypeInfo, vtable/itable copies and method adapters; consumed by the runtime in `+initialize`. | [export.md](export.md) |
| **Reverse adapter** | `kotlin2objc_*` bridge letting Kotlin dispatch into an ObjC/Swift subclass override. | [export.md](export.md) |
| **`objc2kotlin_*` / `kotlin2objc_*`** | Naming of the generated bridge functions: ObjC→Kotlin IMPs (`generateObjCImp`, located in `<compiler-generated>` for the debugger, stepped through by the lldb stop hook) and Kotlin→ObjC reverse bridges. | [export.md](export.md), [debug-info.md](debug-info.md) |
| **WritableTypeInfo** | Mutable companion of TypeInfo holding `convertToRetained`, the ObjC class and the type adapter. | [export.md](export.md), [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **Symbols struct / kref** | C export's `<prefix>_ExportedSymbols` function-pointer struct and `{pinned}` stable-pointer handles. | [export.md](export.md) |
| **Swift export** | Separate Analysis-API-based tool generating Swift plus Kotlin bridges (`@ExportedBridge`), compiled by K/N. | [export.md](export.md), [docs/swift-export](../swift-export/README.md) |

## Interop

| Term | Meaning | See |
|------|---------|-----|
| **`.def` file** | The cinterop input: headers, `headerFilter`, `compilerOpts`, `linkerOpts`, `staticLibraries`, `language`, `package`, `foreignExceptionMode`, …, with per-target property suffixes. | [cinterop.md](cinterop.md) |
| **Stub IR** | The cinterop tool's own representation of the generated Kotlin API (`StubIr.kt`), emitted as klib metadata for Native or as Kotlin source for the compiler's own JVM bindings. | [cinterop.md](cinterop.md) |
| **Indexer** | The libclang-based part of cinterop that walks headers and collects declarations; nullability and ObjC specifics come through `libclangext`. | [cinterop.md](cinterop.md) |
| **`@CCall(id)` / `@CCall.Direct(name)`** | Stub annotations choosing an indirect call through a function pointer exported from the klib's bitcode, or a direct `__asm`-named symbol; selected by `CCallMode` on the tool (`-Xccall-mode`) and the compiler (`-Xbinary=cCallMode`). Platform libs are built indirect. | [cinterop.md](cinterop.md) |
| **C bridge** (`knbridge`) | A compiler-generated C function per interop call site, carried in `@KotlinToCBridge` / `@CToKotlinBridge` annotations and compiled by clang in the `CStubs` phase. | [cinterop.md](cinterop.md), [lowerings.md](lowerings.md) |
| **Forward declaration packages** | `cnames.structs`, `objcnames.classes`, `objcnames.protocols`: synthesized from the `includedForwardDeclarations` manifest entries. | [cinterop.md](cinterop.md), [klib-linking.md](klib-linking.md) |
| **Header mode** (`-Xheader-mode`) | cinterop output with metadata only, no bridge validation or bitcode (`header=true`); C errors surface at compile time instead. | [cinterop.md](cinterop.md) |
| **Foreign exception mode** | Per-klib `foreignExceptionMode` (`terminate` or `objc-wrap`) controlling the filtering landing pad around bridge calls. | [exceptions.md](exceptions.md), [cinterop.md](cinterop.md), [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **Associated object** (`associatedObject_`) | Pointer slot in a Kotlin object's `ExtraObjectData` linking it to its ObjC counterpart (a `KotlinBase` wrapper, or the retained foreign ObjC object a Kotlin wrapper stands for); `ObjHeader::GetAssociatedObject`, released by `ExtraObjectData::ReleaseAssociatedObject` when the object is swept. | [cinterop.md](cinterop.md), [export.md](export.md), [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **`ObjCBackRef`** | The `ExternalRCRefImpl` stored in the `kotlinBody` ivar of a Kotlin subclass of an ObjC class: `rc_ > 0` keeps the Kotlin object rooted; `retain`/`release`/`_tryRetain` act on that count instead of the NSObject refcount (`ObjCBackRef.hpp`, `ObjCInterop.mm`). | [cinterop.md](cinterop.md), [runtime.md](runtime.md) |
| **`NSObjectToKotlin` category** | `NSObject` category in `ObjCExport.mm` / `ObjCExportClasses.mm` implementing `toKotlin:` and `releaseAsAssociatedObject`: a foreign ObjC object is wrapped in a fresh Kotlin object (`Kotlin_ObjCExport_convertUnmappedObjCObject`), `KotlinBase` returns the wrapped object. | [cinterop.md](cinterop.md), [export.md](export.md) |
| **`releaseAsAssociatedObject`** | Selector the GC sends (`Kotlin_ObjCExport_releaseAssociatedObject`) when it frees an object with an ObjC counterpart; the only path that drops the wrapper's NSObject refcount, so a wrapper lives until the Kotlin object is swept. | [export.md](export.md) |
| **`KotlinObjCClassInfo`** | Compiler-emitted descriptor from which the runtime lazily creates the ObjC class for a Kotlin subclass of an ObjC class (`CreateKotlinObjCClass`). | [cinterop.md](cinterop.md) |

## Whole-program analyses: algorithm vocabulary

| Term | Meaning | See |
|------|---------|-----|
| **DFG symbol table** | `DataFlowIR.SymbolTable`, sealed after `populateWith`; maps classes, functions and fields to DFG symbols. Lowerings that create declarations must run before it is built. | [dataflow-ir.md](dataflow-ir.md) |
| **`FunctionSymbol.External`** | DFG symbol of a bodiless function (external, builtin operator, cinterop stub); opaque to every analysis, carries `@Escapes`/`@PointsTo` when present. | [dataflow-ir.md](dataflow-ir.md) |
| **`symbolTableIndex`** | ≥ 0 iff a function can be reached through a vtable/itable slot; −1 otherwise. | [dataflow-ir.md](dataflow-ir.md) |
| **Scope (DFG)**, **`SaveCoroutineState`** | Loop-nesting node group with a depth (the DFG's only notion of structure); node listing the live variables spilled at a suspension point. | [dataflow-ir.md](dataflow-ir.md) |
| **VTA / RTA** | Variable Type Analysis (set constraints assigning each value node a set of possible classes, the devirtualization algorithm) bounded by Rapid Type Analysis (`instantiatingClasses`, the classes whose allocation is reachable). | [devirtualization.md](devirtualization.md) |
| **Constraint graph**, **cast edge**, **bad edge** | Devirtualization's graph of Source/Ordinary nodes; a `CastEdge` filters the flowing types by a bitset; a bad edge is a cast edge against the topological order, handled by the greedy/BFS fixpoint. | [devirtualization.md](devirtualization.md) |
| **Virtual dispatch hub** | Shared fan-out node group per (receiver type, interface id, slot) that replaced per-site callee unfolding. | [devirtualization.md](devirtualization.md) |
| **Useful node** | Constraint node from which a devirtualizable receiver is reachable; only those are propagated. | [devirtualization.md](devirtualization.md) |
| **Points-to graph (PTG)**, **escape origin**, **drain** | Escape analysis' per-function graph; a node whose contents leave the function (return, global, thrown, external call); the sink node escaping references drain into. | [escape-analysis.md](escape-analysis.md) |
| **Intestines**, **EA depth** | The pseudo-field for array/object contents (`inte$tines`); the scope depth used to decide stack eligibility. | [escape-analysis.md](escape-analysis.md) |
| **Exiled object** | Any `Alloc` whose computed lifetime is not `STACK`/`GLOBAL` (`ARGUMENT`, `RETURN_VALUE`, `INDIRECT_RETURN_VALUE`, `LOCAL`), plus stack candidates that are over-budget arrays or have a finalizer: `computeLifetimes` folds them to `GLOBAL` and, with `escapeAnalysisPropagateExiledToHeapObjects=true` (default, required by CMS), turns the node into an escape origin ("Forcing node … to escape") so its referents go to the heap too. | [escape-analysis.md](escape-analysis.md#key-concepts) |
| **`superQualifier` on devirtualized calls** | `irDevirtualizedCall` rewrites every resolved call with `superQualifierSymbol` = the callee's class, so codegen's `callDirect` is taken even for an `open` override; visible as `superQualifier='CLASS X'` in IR dumps. | [devirtualization.md](devirtualization.md) |
| **Function summary** (`CompressedPointsToGraph`) | Compressed PTG of a callee (`P<i>`, `RET`, `D<k>`) embedded at call sites. | [escape-analysis.md](escape-analysis.md) |
| **`kAllowHeapToStackRefs`** | GC mark trait: whether dead heap objects may still reference stack objects (true for STMS/PMCS, false for CMS). | [escape-analysis.md](escape-analysis.md), [gc.md](gc.md) |
| **Class interval**, **interface color**, **forbiddenness graph** | GHA's DFS enter/exit ids (`TypeInfo.classId_` = enter time; subclass test is an interval check); the greedy coloring of interfaces (`MAX_BITS_PER_COLOR = 5`) that makes itables perfectly hashable; the graph it colors (`InterfacesForbiddennessGraph`: two interfaces are adjacent when one class implements both). | [dce-and-gha.md](dce-and-gha.md) |
| **Tricolor DFS** | White/grey/black depth-first traversal of the call graph used by the pre-codegen inliner (bottom-up, cycle-aware) and described for the DFG call graph; grey-on-grey edges are the cycles. | [pre-codegen-inliner.md](pre-codegen-inliner.md), [dataflow-ir.md](dataflow-ir.md) |
| **Fast vs sorted itable** | `interfaceTableSize_ >= 0`: mask `size-1`, ≤ 32 entries; `< 0`: `-count` entries sorted by id, binary search (`LookupInterfaceTableRecord`). | [dce-and-gha.md](dce-and-gha.md) |
| **`referencedFunctions`**, **bridgeTarget rule** | The DCE result handed to `RTTIPhase`; DCE keeps bridge targets because class layouts are computed from them (KT-74377). | [dce-and-gha.md](dce-and-gha.md) |
| **Pre-codegen inliner** | IR-level optimization inliner (`preCodegenInlineThreshold`, default 0 = off), distinct from the language-level `inline` function inliner and from LLVM inlining; opt-out `@kotlin.native.NoInline`. | [pre-codegen-inliner.md](pre-codegen-inliner.md) |
| **`NO_CAST_NEEDED`** | Proof-token block around an `IMPLICIT_CAST` that tells `Autoboxing` no checked cast is needed. | [casts-optimization.md](casts-optimization.md) |
| **Cast bridge**, **`BridgesPolicy`** | With `genericSafeCasts`, overrides with narrower erased parameters get bridges (`BOX_UNBOX_CASTS`) whose casts `Autoboxing` materializes. | [casts-optimization.md](casts-optimization.md) |
| **Predicate / leaf term / phantom variable** | `CastsOptimization` vocabulary: CNF predicates over bitset terms (`v is C`, `v == null`) tracked along SSA-like phantom aliases. | [casts-optimization.md](casts-optimization.md) |

## Initialization

| Term | Meaning | See |
|------|---------|-----|
| **Container** | The unit of static initialization: an `IrFile` or an `IrClass`, each with its own state word and initializer functions. | [static-initializers.md](static-initializers.md) |
| **Static initializer**, **`$clinit_trigger`** | `$init_global`, `$init_thread_local` and their `_eager` variants (recognised by origin, not name); the trigger function whose call is prepended to functions and constructors of the container. | [static-initializers.md](static-initializers.md) |
| **State word** | `state_global$`/`state_thread_local$`: `FILE_*` values 0 / 1 (initializing, with thread id) / 2 (initialized) / 3 (failed, sticky). | [static-initializers.md](static-initializers.md) |
| **`InitNode`**, **init phases 0/1/2** | Runtime list of eager initializers (`_Konan_init_<lib>` → `_Konan_constructors`); phases allocate TLS, run globals (first runtime only), run thread-local globals. | [static-initializers.md](static-initializers.md), [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| **`@EagerInitialization`**, **`@ThreadLocal`** | Opt-out of lazy initialization for a top-level property (experimental, deprecated); per-thread global. | [static-initializers.md](static-initializers.md) |
| **`new_companion_initialization`** | Per-klib manifest flag (set by `LanguageFeature.CompanionBlocks`) selecting the class-initialization order (`$clinit_trigger` chain through supertypes). | [static-initializers.md](static-initializers.md) |

## GC and allocator internals

| Term | Meaning | See |
|------|---------|-----|
| **SATB**, **deletion barrier**, **allocate-black** | CMS marking discipline: snapshot-at-the-beginning; `beforeHeapRefUpdate` remembers the overwritten reference; new objects are marked on allocation while barriers are on. | [gc.md](gc.md) |
| **Weak-read barrier**, **barriers phase** | Barrier on weak reads during `kWeakProcessing`; CMS phases `kDisabled` / `kMarkClosure` / `kWeakProcessing`. | [gc.md](gc.md) |
| **MarkPacer**, **auxiliary GC thread**, **cooperative mutator** | PMCS's phase controller (`kReady` / `kRootSet` / `kParallelMark`), its extra marking threads (`auxGCThreads`), and mutators that mark while suspended (`gcMutatorsCooperate`). | [gc.md](gc.md) |
| **ParallelProcessor / batch** | Work *sharing* (not stealing) between marking workers in batches; termination via `batchesEverShared`. | [gc.md](gc.md) |
| **`GC::ObjectData`**, **`kNoQueueMark`** | Per-object GC header 8 bytes before `ObjHeader`; its `next_` word doubles as mark bit and mark-queue link: `nullptr` = white, `kNoQueueMark` (= 1) = marked but not queued (`tryMark` CAS, `markUncontended` for fresh objects), otherwise the queue link. | [gc.md](gc.md), [allocator.md](allocator.md) |
| **`GCHandle` / `GCInfo`**, **`HeapGrowthController`** | Per-epoch statistics (`GC.lastGCInfo`); the adaptive scheduler's target computation. | [gc.md](gc.md) |
| **Cell**, **page kinds**, **size class** | 8-byte allocation unit; `FixedBlockPage` (≤ 128 cells), `NextFitPage` (≤ ~256 KiB), `SingleObjectPage`; fixed-block cell counts 1..128. | [allocator.md](allocator.md) |
| **Free run** (`FixedCellRange`) | A contiguous range of free blocks inside a `FixedBlockPage`; the fast path pops the first cell of the current range (`nextFree_.first += blockSize_`) and, when it is exhausted, loads the next range from the range's last cell. | [allocator.md](allocator.md#fast-path-slow-path-page-acquisition) |
| **`FrameOverlay`**, **`SafePointActivator`** | The header `{previous, parameters, count}` of every shadow-stack frame the GC walks lock-free; the RAII object whose existence makes the global safepoint function pointer non-null (STW requests, the aggressive scheduler). | [compiler-runtime-contract.md](compiler-runtime-contract.md), [runtime.md](runtime.md), [gc.md](gc.md) |
| **`PageStore`**, **`Heap`**, **`CustomAllocator`** | Per-size-class page stacks (`empty_`/`ready_`/`used_`/`unswept_`); the global heap; the per-thread allocator state (fast path without atomics). | [allocator.md](allocator.md) |
| **`ExtraObjectCell`**, **finalizer queue** | Storage for meta-objects; per-thread queues handed to the "GC finalizer processor" thread or the main-thread processor (`objcDisposeOnMain`). | [allocator.md](allocator.md) |
| **`AllocatedSizeTracker`**, **`pagedAllocator`**, **`mmapTag`** | Accounting fed to the scheduler on page overflow; per-object mode used with sanitizers; Apple VM tag (246) for `vmmap`. | [allocator.md](allocator.md) |

## Incremental compilation

| Term | Meaning | See |
|------|---------|-----|
| **IC'ed library** | A klib passed explicitly on the CLI, not external and not stdlib, cached per file under `-Xic-cache-dir/<flavor>` (`isSubjectOfIC`). | [incremental-compilation.md](incremental-compilation.md) |
| **File id** (`cacheFileId`), **file fingerprint** | `CacheSupport.cacheFileId` = `<package or ROOT>.<hash36(absolute source path)>`, the per-file cache directory name (moving the checkout invalidates all IC state); `SerializedIrFileFingerprint` over all serialized parts of a klib file. | [incremental-compilation.md](incremental-compilation.md), [caches.md](caches.md) |
| **Changed / dirty / removed file**, **strong vs weak dependency** | Dirty-file analysis vocabulary: changed files plus strong-dependency closure (reverse `bitcode_deps`); weak edges (virtual trampolines) propagate only from removed files. | [incremental-compilation.md](incremental-compilation.md) |
| **Stale cache**, **full rebuild**, **cache batch** | `staleCacheReason` (metadata, compiler fingerprint, dependencies fingerprint, companion-init mode, klib hash); `needFullRebuild`; several libraries built in one spawned compilation (`batchedCacheBuild`). | [incremental-compilation.md](incremental-compilation.md) |
| **Compiler fingerprint** | `<dist>/konan/compiler.fingerprint`, the only compiler-version key of IC and auto caches. | [incremental-compilation.md](incremental-compilation.md), [compatibility-and-migration.md](compatibility-and-migration.md) |

## Platform libraries, compatibility, tooling

| Term | Meaning | See |
|------|---------|-----|
| **Platform `.def`**, **`.def.disabled`**, **`depends =`** | Sources of platform libraries under `kotlin-native/platformLibs/src/platform/<family>/`; disabled libraries; the auto-generated dependency line (Apple families). | [platform-libraries.md](platform-libraries.md) |
| **Prebuilt vs light distribution** | `kotlin.native.distribution.type`: platform libs shipped, or generated on the user's machine by KGP via `generatePlatformLibraries`. | [platform-libraries.md](platform-libraries.md), [tooling-integration.md](tooling-integration.md) |
| **Cacheable target**, **cross bundle** | `cacheableTargets.<host>` in `konan.properties`; a Linux/Windows distribution with Darwin platform klibs merged in. | [platform-libraries.md](platform-libraries.md), [targets-and-toolchains.md](targets-and-toolchains.md) |
| **Commonized distribution klibs** | Output of `commonizeNativeDistribution` under `<dist>/klib/commonized/<URL-encoded KGP version>/<identity string>/`, used by shared source sets and the IDE; cached by a `.success` marker keyed only by KGP version and `konanHome`. | [commonizer.md](commonizer.md), [platform-libraries.md](platform-libraries.md), [tooling-integration.md](tooling-integration.md) |
| **ABI window** | `KotlinAbiVersion.FIRST_SUPPORTED .. CURRENT` (1.6.0 .. 2.5.0), applied by `KlibLoader` to klibs that have IR. | [compatibility-and-migration.md](compatibility-and-migration.md) |
| **`ExportKlibToOlderAbiVersion`** / **`KlibAbiCompatibilityLevel`** | Opt-in forward export of a klib to the previous ABI level. | [compatibility-and-migration.md](compatibility-and-migration.md) |
| **Special compatibility checker** | `KonanLibrarySpecialCompatibilityChecker`: verifies the stdlib's version against `konan.properties` `compilerVersion`; platform and user libs are not checked (KT-68322). | [compatibility-and-migration.md](compatibility-and-migration.md) |
| **Soft-deprecated target** | `softDeprecatedTargets` in `KonanDriver.kt` (compiler strong warning), maintained separately from KGP's `KonanTarget.deprecatedTargets`. | [compatibility-and-migration.md](compatibility-and-migration.md) |
| **Argument lifecycle** | `introduced` / `stabilized` / `deprecated` / `removedVersion` metadata of a compiler argument (`KotlinReleaseVersionLifecycle.kt` in `compiler/arguments`); `-Xbinary` option names have none. | [compatibility-and-migration.md](compatibility-and-migration.md) |
| **Embeddable compiler jar**, **`daemonMain`** | `konan/lib/kotlin-native-compiler-embeddable.jar` loaded by KGP in a cached classloader and entered through `MainKt.daemonMain` (in-process mode) or forked with `kotlin.native.disableCompilerDaemon=true`. | [tooling-integration.md](tooling-integration.md) |
| **`KotlinNativeToolRunner`**, **toolchain provisioning** | KGP's runner for compiler/cinterop/commonizer; download and unpacking of the distribution (`kotlin.native.home`, `kotlin.native.version`, `provisioned.ok`). | [tooling-integration.md](tooling-integration.md) |
| **`UnitStats`** | JSON produced by `-Xdump-perf`, parsed by KGP for build reports. | [tooling-integration.md](tooling-integration.md) |
| **Commonizer target** | `LeafCommonizerTarget(name)` wraps a `KonanTarget` name; `SharedCommonizerTarget(leaves)` is a flat set of leaves and the only kind of output target. Marks commonized klibs through the `commonizer_target` / `commonizer_native_targets` manifest keys. | [commonizer.md](commonizer.md), [tooling-integration.md](tooling-integration.md) |
| **`ktest_*` flags** | The generated test runner's CLI (`--ktest_logger=TEAMCITY`, filters), a KGP contract. | [tooling-integration.md](tooling-integration.md), [test-infrastructure.md](test-infrastructure.md) |

## Targets, toolchains and the distribution

| Term | Meaning | See |
|------|---------|-----|
| **`KonanTarget`** (target name) | Sealed-class object `(name, Family, Architecture)`; the name (`ios_arm64`) is the identity used by the CLI, klib manifests, directory names, KGP and the commonizer. | [targets-and-toolchains.md](targets-and-toolchains.md#the-target-model) |
| **Enabled target** (`enabledByHost`) | A target buildable into a binary on the current host (`HostManager.enabledByHost[host]`); klibs can be produced for any target on any host. | [targets-and-toolchains.md](targets-and-toolchains.md#hosts-and-the-support-matrix) |
| **`Configurables`**, **`Platform`**, **`PlatformManager`** | Typed per-target view over `konan.properties` (one interface per family); `Configurables` + lazily built `ClangArgs` + `LinkerFlags`; `PlatformManager(distribution)` eagerly loads one `Configurables` per enabled target. | [targets-and-toolchains.md](targets-and-toolchains.md#from-properties-to-configurables) |
| **Dependency** (native) | A named archive (`llvm-21-aarch64-macos-essentials-97`, `target-sysroot-xcode_…`) under `$KONAN_DATA_DIR/dependencies/<name>`; downloaded through `dependencies/cache` (with a `.lock`) and recorded in `dependencies/.extracted`, which is never re-validated. | [targets-and-toolchains.md](targets-and-toolchains.md#dependencies-naming-resolution-download) |
| **`targetToolchain`**, **`targetSysRoot`**, **`llvmHome`** | The three roots everything else derives from: where the `ld`/`clang` drivers live, where headers and libraries live, where the bundled LLVM lives. | [targets-and-toolchains.md](targets-and-toolchains.md#toolchains) |
| **LLVM variant** (`-Xllvm-variant=user\|dev\|dev-with-asserts\|<path>`) | Which LLVM build supplies `clang++`, `libLLVM` and tools, by rewriting `llvmHome.<host>`: `user` = the "essentials" archive, `dev` adds `opt`, `llc`, `FileCheck`; the build picks archives through `kotlin.native.llvm.<slot>.<host>.*` slots in `gradle.properties`. | [targets-and-toolchains.md](targets-and-toolchains.md#llvm-selection), [llvm-passes.md](llvm-passes.md) |
| **`TargetWithSanitizer`** | Build-side key `target[_tsan\|_asan]` under which the runtime bitcode is compiled and published. | [targets-and-toolchains.md](targets-and-toolchains.md#runtime-build-per-target) |
| **Whole-Xcode provisioning** (`useProvisionedXcode`) | Toolchain mode in which a whole Xcode downloaded as the `xcode_<ver>_<build>` dependency replaces the locally installed one; turned on by the build (`PlatformManagerProvider`) and by tests under `kotlin.native.internalServer.wholeXcode`. | [targets-and-toolchains.md](targets-and-toolchains.md#apple-xcode-coupling) |
| **Internal server** (`KONAN_USE_INTERNAL_SERVER=0\|1\|auto`) | Enables the `remote:internal` dependency candidates (Apple and Windows SDK parts); `auto` = hostname ends in `labs.intellij.net` and resolves. | [targets-and-toolchains.md](targets-and-toolchains.md#dependencies-naming-resolution-download) |

## Commonizer

| Term | Meaning | See |
|------|---------|-----|
| **Identity string** | `toString()` of a `CommonizerTarget`: the leaf name, or `(a, b, c)` with the leaves sorted; the output directory name, the `commonizer_target` manifest value and the CLI syntax (`parseCommonizerTarget`). | [commonizer.md](commonizer.md#key-concepts) |
| **CIR** | "Commonizer IR": `CirRoot ⊃ CirModule ⊃ CirPackage ⊃ CirClass / CirTypeAlias / CirFunction / CirProperty / CirClassConstructor` with `CirType`s, built from `kotlinx-metadata-klib` and heavily interned. | [commonizer.md](commonizer.md#step-2--deserialization-into-per-target-cir-trees) |
| **Merged tree** (`CirNode`) | One node per declaration name/signature holding `targetDeclarations` (one slot per input target) and a lazy `commonDeclaration`; only the common slot is serialized, leaf klibs are never rewritten. | [commonizer.md](commonizer.md#step-3--merging-into-one-node-tree) |
| **Approximation key** | How callables are matched across targets: name plus parameter/receiver type signatures (`FunctionApproximationKey`, `PropertyApproximationKey`, `ConstructorApproximationKey`). | [commonizer.md](commonizer.md) |
| **Type-alias substitution** | When a type's classifier differs per target, `AssociatedClassifierIdsResolver` collects the ids connected through type-alias edges on any target and `CirTypeDistance` picks the closest candidate that resolves on every target. | [commonizer.md](commonizer.md#step-6--types-type-alias-substitution-and-the-number-features) |
| **Optimistic number commonization** (`@UnsafeNumber`) | `-optimistic-numbers` (default on): numeric types of different widths commonize to the narrowest one, tagged `@kotlinx.cinterop.UnsafeNumber(actualPlatformTypes)`. | [commonizer.md](commonizer.md#step-6--types-type-alias-substitution-and-the-number-features) |
| **Platform integer commonization** | `-platform-integers` (default off): `Int`/`Long` and friends become `kotlin.PlatformInt` & co. when every input's width matches the hand-maintained `PlatformWidthIndex` table. | [commonizer.md](commonizer.md#step-6--types-type-alias-substitution-and-the-number-features) |
| **Lifted-up declaration** | A declaration emitted in the common output as is, not as `expect`: every type alias and every `const val` whose value is equal on all targets (`CirLiftedUpDeclaration.isLiftedUp`). | [commonizer.md](commonizer.md) |
| **`CInteropCommonizerGroup`** | KGP's batching unit for `commonizeCInterop`: one CLI run per group covering all targets and interops of the dependents it contains, output under `build/classes/kotlin/commonizer/`. | [commonizer.md](commonizer.md#dependencies-between-runs-and-the-target-hierarchy) |
| **`CommonizerDependency`** | A `-dependency-libraries` entry: `TargetedCommonizerDependency` (`<identity string>::<path>`, visible to one target) or `NonTargetedCommonizerDependency` (bare path, visible to all). | [commonizer.md](commonizer.md#dependencies-between-runs-and-the-target-hierarchy) |

## Test infrastructure

| Term | Meaning | See |
|------|---------|-----|
| **`TestCase`**, **`TestModule`** | One test data file after preprocessing (modules, kind, free compiler args, checks, expected failure); modules are `Exclusive` (one test case), `Shared` (support klib compiled once) or `Given` (external klib, `-Pkn.customKlibs`). | [test-infrastructure.md](test-infrastructure.md#key-concepts) |
| **`TestKind`** | `REGULAR` (joins a shared executable), `STANDALONE` (own executable with test runner), `STANDALONE_NO_TR` (own `main`), `STANDALONE_LLDB`, `STANDALONE_STEPPING`; `-Pkn.forceStandalone=true` turns `REGULAR` into `STANDALONE`. | [test-infrastructure.md](test-infrastructure.md) |
| **`TestMode`** | `-Pkn.mode`: `TWO_STAGE_MULTI_MODULE` (default: sources → klib, then `-Xinclude=<klib>` → program) or `ONE_STAGE_MULTI_MODULE` (sources → program). | [test-infrastructure.md](test-infrastructure.md#from-gradle-property-to-compiler-flag) |
| **Nominal package name** | Package derived from the test data path (`codegen/box/foo/bar.kt` → `codegen.box.foo.bar`); regular tests are re-packaged under it so one binary holds many and `--ktest_filter=<pkg>.*` selects one. | [test-infrastructure.md](test-infrastructure.md) |
| **Test dump file** (`-Xdump-tests-to`) | `<exe>.dump`, written by the compiler's `TestsDumper` with one `pkg.Suite:function` line per registered test; the infrastructure reads it to create the JUnit dynamic tests. | [test-infrastructure.md](test-infrastructure.md#step-4--what-the-compiler-contributes-the-test-runner-and-the-dump) |
| **TC logger**, **`TCTestOutputFilter`** | The `--ktest_logger=TEAMCITY` output format of the in-binary test runner, and the parser that turns it back into passed/failed/ignored sets. | [test-infrastructure.md](test-infrastructure.md) |
| **`Executor`** | `native/executors`: runs a binary for the test target (`HostExecutor`, `XcodeSimulatorExecutor`, `EmulatorExecutor`, `RosettaExecutor`, XCTest executors, `NoOpExecutor` for `-Pkn.compileOnly=true`). | [test-infrastructure.md](test-infrastructure.md#step-5--running) |
| **System cache dir** (tests) | `<dist>/klib/cache/<target>-gSTATIC-system/`, the stdlib per-file cache the default `STATIC_ONLY_DIST` mode consumes through `-Xauto-cache-from=<dist>/klib`; `OPT` runs default to `NO`. | [test-infrastructure.md](test-infrastructure.md#cache-modes), [caches.md](caches.md) |
| **FileCheck prefixes** | Cross product of `CHECK`, `CHECK-<abi>`, `CHECK-<TARGET>`, `CHECK-APPLE`, `CHECK-SMALLBINARY`/`-BIGBINARY` with the suffixes `-DEBUG` / `-OPT` / `-CACHE_<alias>`, e.g. `CHECK-AAPCS-DEBUG`, `CHECK-CACHE_STATIC_ONLY_DIST`. | [test-infrastructure.md](test-infrastructure.md#filecheck-tests) |
