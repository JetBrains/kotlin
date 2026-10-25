# Kotlin/Native glossary

Terms that recur in the K/N code base, commit messages and issues, with the page where each is explained. Terms are grouped by
area; within a group they are in the order you are likely to meet them.

## Compilation model and driver

| Term | Meaning | See |
|------|---------|-----|
| **konan**, **K/N** | The historical name of the Kotlin/Native compiler. It survives in package names (`org.jetbrains.kotlin.backend.konan`), `konan.properties`, `KONAN_DATA_DIR`, `konanc`, `_kexe`. | [architecture.md](architecture.md) |
| **First stage / second stage** | `.kt → .klib` (K2 frontend, LLVM-free) and `.klib → binary` (the backend). Selected by `-produce library` vs anything else. | [architecture.md](architecture.md#big-picture), [second-stage-pipeline.md](second-stage-pipeline.md) |
| **One-stage mode** | Sources plus a binary output kind on one command line: the CLI writes a temporary klib, then runs the second stage on it. Gradle never uses it. | [second-stage-pipeline.md](second-stage-pipeline.md#1-cli-entry-choosing-the-stage) |
| **Output kind** | `CompilerOutputKind`: `PROGRAM`, `STATIC`, `DYNAMIC`, `FRAMEWORK`, `TEST_BUNDLE`, `STATIC_CACHE`, `DYNAMIC_CACHE`, `HEADER_CACHE`, `LIBRARY`. | [second-stage-pipeline.md](second-stage-pipeline.md#3-nativecompilerdriverrun-one-flow-per-output-kind) |
| **kexe** | File extension of a K/N executable; also the `_kexe` suffix added to a module name that collides with a library's. | [second-stage-pipeline.md](second-stage-pipeline.md#2-konandriverrun-preparing-the-compilation) |
| **Binary option** (`-Xbinary=name=value`) | Options that change the produced binary (GC, allocator, asserts, …). Defined in `native/binary-options`; defaults resolved in `NativeSecondStageCompilationConfig`. Non-default values become part of the cache flavor. | [second-stage-pipeline.md](second-stage-pipeline.md), [caches.md](caches.md) |
| **Fragment** (`BackendJobFragment`) | The unit of codegen: one IR module plus the state for one LLVM module. One per compilation, or one per file when building per-file caches. | [second-stage-pipeline.md](second-stage-pipeline.md#4-runbackend-fragments-lowerings-codegen) |
| **LLVM module specification** | Decides which IR modules/declarations are compiled into *this* LLVM module and which come from caches (`containsDeclaration` = "is it external?"). | [caches.md](caches.md), [llvm-codegen.md](llvm-codegen.md) |
| **Phase** | A `NamedCompilerPhase`. Lowering phases are named after the lowering class; the name is what `-Xdisable-phases`, `-Xphases-to-dump-after`, `-Xlist-phases` use. | [second-stage-pipeline.md](second-stage-pipeline.md#key-concepts) |
| **Phase context** | What a phase can see: `NativeBackendContext` (whole compilation), `NativeGenerationState` (one fragment), `LinkKlibsContext`, `BitcodePostProcessingContext`. | [second-stage-pipeline.md](second-stage-pipeline.md#contexts-what-each-phase-can-see) |
| **Distribution** (`dist`, `konanHome`) | `kotlin-native/dist`: compiler jar, runtime bitcode, stdlib and platform klibs, prebuilt caches. Tests and KGP load the compiler from here. | [architecture.md](architecture.md#distribution) |
| **Platform libraries** | Prebuilt cinterop klibs for the target's system libraries (`klib/platform/<target>`), added by default unless `-no-default-libs`. | [cinterop.md](cinterop.md), [klib-linking.md](klib-linking.md) |

## Klibs and linking

| Term | Meaning | See |
|------|---------|-----|
| **Included library** (`-Xinclude`) | A klib compiled as part of the main module: fully deserialized, its descriptors are "source" modules. One-stage mode uses it for the intermediate klib. | [klib-linking.md](klib-linking.md) |
| **Exported library** (`-Xexport-library`) | A klib whose whole public API is deserialized (strategy ALL) so it can be exposed through ObjC/C export. Opposed to a merely linked dependency. | [klib-linking.md](klib-linking.md), [export.md](export.md) |
| **Header klib** | A klib holding only the public ABI (`publicAbiOnly`, manifest `header=true`), produced with `-Xheader-klib-path`. | [klib-linking.md](klib-linking.md) |
| **Metadata klib** | `-Xmetadata-klib` output: metadata only, no IR, no `depends`. Used for shared source sets and commonization. | [klib-linking.md](klib-linking.md) |
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
| **Mandatory / module / LTO pipelines** | The three LLVM pass pipelines run on the merged module (`OptimizationPipeline.kt`). | [llvm-codegen.md](llvm-codegen.md) |
| **libllvmext** | C++ extension of LLVM's C API shipping the `kotlin-*` passes (`kotlin-hide-symbols`, `kotlin-remove-sp`, `kotlin-ssp`, `kotlin-tsan`, `kotlin-calls-checker*`). | [llvm-codegen.md](llvm-codegen.md) |
| **Symbol prefixes** | `kfun:` functions, `kclass:` TypeInfo, `kvar:` globals, `ktypew:` writable type info, `kobjref:` object refs, `kintf:` / `krefs:` / `kifacetable:` RTTI arrays; `<fq>#internal` for non-exported. | [llvm-codegen.md](llvm-codegen.md) |

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
| **Cache flavor** | Directory name encoding target, debug/opt and every codegen-affecting option (`appendCommonCacheFlavor`); system vs user flavor. | [caches.md](caches.md) |
| **System cache** | Prebuilt caches of distribution libraries under `<dist>/klib/cache/<system flavor>`. | [caches.md](caches.md) |
| **Auto cache** | Cache of a klib under a `-Xauto-cache-from` root, built on demand into a fingerprinted directory. | [caches.md](caches.md) |
| **Per-file cache** | One object per klib file (`<name>-per-file-cache/<fileId>`), the basis of incremental second-stage compilation. | [caches.md](caches.md) |
| **Additional cache info** | The `ir/` side files (class fields, inline body references, eager-init files, trivial getters, ObjC adapters, PL issues) that let consumers skip IR deserialization. | [caches.md](caches.md) |
| **Cache ABI** | `INTERNAL_ABI_ORIGIN` accessors (`CachesAbiSupport`) exposing private state across object files; produced by `ExportCachesAbiVisitor`, used by `ImportCachesAbiTransformer`. | [caches.md](caches.md), [lowerings.md](lowerings.md) |
| **Bitcode dependency** (`bitcode_deps`) | Per-cache record of which libraries/files its code references; drives link-time cache selection and incremental dirtiness. | [caches.md](caches.md) |
| **Prelink** | `ld -r` of the main object with static caches into one `.o`; Apple, static-library outputs, opt-in via `-Xpre-link-caches=enable`. | [caches.md](caches.md) |

## Export

| Term | Meaning | See |
|------|---------|-----|
| **Top-level name prefix** | Abbreviation of the framework name prepended to ObjC class names; forced to `Kotlin` under Swift export. | [export.md](export.md) |
| **Base method** | A Kotlin method overriding nothing exposed; ObjC selectors and bridges are computed per base method. | [export.md](export.md) |
| **MethodBridge / TypeBridge** | Header-generator description of how each parameter and return value crosses the boundary; shared with LLVM codegen. | [export.md](export.md) |
| **Type adapter** (`ObjCTypeAdapter`) | Per-class constant with ObjC name, TypeInfo, vtable/itable copies and method adapters; consumed by the runtime in `+initialize`. | [export.md](export.md) |
| **Reverse adapter** | `kotlin2objc_*` bridge letting Kotlin dispatch into an ObjC/Swift subclass override. | [export.md](export.md) |
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
| **Foreign exception mode** | Per-klib `foreignExceptionMode` (`terminate` or `objc-wrap`) controlling the filtering landing pad around bridge calls. | [cinterop.md](cinterop.md), [compiler-runtime-contract.md](compiler-runtime-contract.md) |
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
| **Intestines**, **EA depth**, **exiled object** | The pseudo-field for array/object contents (`inte$tines`); the scope depth used to decide stack eligibility; a local object demoted to the heap that is then treated as an escape origin (`escapeAnalysisPropagateExiledToHeapObjects`). | [escape-analysis.md](escape-analysis.md) |
| **Function summary** (`CompressedPointsToGraph`) | Compressed PTG of a callee (`P<i>`, `RET`, `D<k>`) embedded at call sites. | [escape-analysis.md](escape-analysis.md) |
| **`kAllowHeapToStackRefs`** | GC mark trait: whether dead heap objects may still reference stack objects (true for STMS/PMCS, false for CMS). | [escape-analysis.md](escape-analysis.md), [gc.md](gc.md) |
| **Class interval**, **interface color** | GHA's DFS enter/exit ids (`TypeInfo.classId_` = enter time; subclass test is an interval check) and the greedy coloring of interfaces (`MAX_BITS_PER_COLOR = 5`) that makes itables perfectly hashable. | [dce-and-gha.md](dce-and-gha.md) |
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
| **`GC::ObjectData`** | Per-object GC header 8 bytes before `ObjHeader`; its mark bit doubles as the mark-queue link. | [gc.md](gc.md), [allocator.md](allocator.md) |
| **`GCHandle` / `GCInfo`**, **`HeapGrowthController`** | Per-epoch statistics (`GC.lastGCInfo`); the adaptive scheduler's target computation. | [gc.md](gc.md) |
| **Cell**, **page kinds**, **size class** | 8-byte allocation unit; `FixedBlockPage` (≤ 128 cells), `NextFitPage` (≤ ~256 KiB), `SingleObjectPage`; fixed-block cell counts 1..128. | [allocator.md](allocator.md) |
| **`PageStore`**, **`Heap`**, **`CustomAllocator`** | Per-size-class page stacks (`empty_`/`ready_`/`used_`/`unswept_`); the global heap; the per-thread allocator state (fast path without atomics). | [allocator.md](allocator.md) |
| **`ExtraObjectCell`**, **finalizer queue** | Storage for meta-objects; per-thread queues handed to the "GC finalizer processor" thread or the main-thread processor (`objcDisposeOnMain`). | [allocator.md](allocator.md) |
| **`AllocatedSizeTracker`**, **`pagedAllocator`**, **`mmapTag`** | Accounting fed to the scheduler on page overflow; per-object mode used with sanitizers; Apple VM tag (246) for `vmmap`. | [allocator.md](allocator.md) |

## Incremental compilation

| Term | Meaning | See |
|------|---------|-----|
| **IC'ed library** | A klib passed explicitly on the CLI, not external and not stdlib, cached per file under `-Xic-cache-dir/<flavor>` (`isSubjectOfIC`). | [incremental-compilation.md](incremental-compilation.md) |
| **File id**, **file fingerprint** | `cacheFileId` = package + hash of the absolute source path (moving the checkout invalidates all IC state); `SerializedIrFileFingerprint` over all serialized parts of a klib file. | [incremental-compilation.md](incremental-compilation.md) |
| **Changed / dirty / removed file**, **strong vs weak dependency** | Dirty-file analysis vocabulary: changed files plus strong-dependency closure (reverse `bitcode_deps`); weak edges (virtual trampolines) propagate only from removed files. | [incremental-compilation.md](incremental-compilation.md) |
| **Stale cache**, **full rebuild**, **cache batch** | `staleCacheReason` (metadata, compiler fingerprint, dependencies fingerprint, companion-init mode, klib hash); `needFullRebuild`; several libraries built in one spawned compilation (`batchedCacheBuild`). | [incremental-compilation.md](incremental-compilation.md) |
| **Compiler fingerprint** | `<dist>/konan/compiler.fingerprint`, the only compiler-version key of IC and auto caches. | [incremental-compilation.md](incremental-compilation.md), [compatibility-and-migration.md](compatibility-and-migration.md) |

## Platform libraries, compatibility, tooling

| Term | Meaning | See |
|------|---------|-----|
| **Platform `.def`**, **`.def.disabled`**, **`depends =`** | Sources of platform libraries under `kotlin-native/platformLibs/src/platform/<family>/`; disabled libraries; the auto-generated dependency line (Apple families). | [platform-libraries.md](platform-libraries.md) |
| **Prebuilt vs light distribution** | `kotlin.native.distribution.type`: platform libs shipped, or generated on the user's machine by KGP via `generatePlatformLibraries`. | [platform-libraries.md](platform-libraries.md), [tooling-integration.md](tooling-integration.md) |
| **Cacheable target**, **cross bundle** | `cacheableTargets.<host>` in `konan.properties`; a Linux/Windows distribution with Darwin platform klibs merged in. | [platform-libraries.md](platform-libraries.md) |
| **Commonized distribution klibs** | Output of `commonizeNativeDistribution` under `<dist>/klib/commonized/<KGP version>`, used by shared source sets and the IDE. | [platform-libraries.md](platform-libraries.md), [tooling-integration.md](tooling-integration.md) |
| **ABI window** | `KotlinAbiVersion.FIRST_SUPPORTED .. CURRENT` (1.6.0 .. 2.5.0), applied by `KlibLoader` to klibs that have IR. | [compatibility-and-migration.md](compatibility-and-migration.md) |
| **`ExportKlibToOlderAbiVersion`** / **`KlibAbiCompatibilityLevel`** | Opt-in forward export of a klib to the previous ABI level. | [compatibility-and-migration.md](compatibility-and-migration.md) |
| **Special compatibility checker** | `KonanLibrarySpecialCompatibilityChecker`: verifies the stdlib's version against `konan.properties` `compilerVersion`; platform and user libs are not checked (KT-68322). | [compatibility-and-migration.md](compatibility-and-migration.md) |
| **Soft-deprecated target** | `softDeprecatedTargets` in `KonanDriver.kt` (compiler strong warning), maintained separately from KGP's `KonanTarget.deprecatedTargets`. | [compatibility-and-migration.md](compatibility-and-migration.md) |
| **Argument lifecycle** | `introduced` / `stabilized` / `deprecated` / `removedVersion` metadata in `compiler/arguments`; `-Xbinary` option names have none. | [compatibility-and-migration.md](compatibility-and-migration.md) |
| **Embeddable compiler jar**, **`daemonMain`** | `konan/lib/kotlin-native-compiler-embeddable.jar` loaded by KGP in a cached classloader and entered through `MainKt.daemonMain` (in-process mode) or forked with `kotlin.native.disableCompilerDaemon=true`. | [tooling-integration.md](tooling-integration.md) |
| **`KotlinNativeToolRunner`**, **toolchain provisioning** | KGP's runner for compiler/cinterop/commonizer; download and unpacking of the distribution (`kotlin.native.home`, `kotlin.native.version`, `provisioned.ok`). | [tooling-integration.md](tooling-integration.md) |
| **`UnitStats`** | JSON produced by `-Xdump-perf`, parsed by KGP for build reports. | [tooling-integration.md](tooling-integration.md) |
| **Commonizer target** | `CommonizerTarget` (leaf or shared) and the `commonizer_target` manifest key marking commonized klibs. | [tooling-integration.md](tooling-integration.md) |
| **`ktest_*` flags** | The generated test runner's CLI (`--ktest_logger=TEAMCITY`, filters), a KGP contract. | [tooling-integration.md](tooling-integration.md) |
