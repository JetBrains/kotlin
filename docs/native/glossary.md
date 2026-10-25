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
