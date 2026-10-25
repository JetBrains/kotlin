# Kotlin/Native: whole-program IR optimizations

The second stage ([architecture.md](architecture.md), "runCodegen") owns a closed-world IR of the whole binary. Before that IR is
translated to LLVM IR, a group of interprocedural analyses runs over it: devirtualization, dead-code elimination (DCE), escape
analysis (EA), a pre-codegen inliner, static-initializer call elimination and global hierarchy analysis (GHA). They share one
abstraction, the **DataFlowIR** (DFG), and most of them run only in `-opt` builds without static caches. This page explains that
model, the order and gating of the phases, what each analysis consumes and produces, and how to debug a miscompilation that only
shows up in release builds. LLVM-level optimizations are covered in [llvm-codegen.md](llvm-codegen.md); the lowerings that precede
all of this in [lowerings.md](lowerings.md).

All code paths below are relative to `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/`
("BACKEND") unless stated otherwise.

This page is the overview. Each analysis has an algorithm-level page: [dataflow-ir.md](dataflow-ir.md) (the shared model),
[devirtualization.md](devirtualization.md), [escape-analysis.md](escape-analysis.md), [dce-and-gha.md](dce-and-gha.md),
[pre-codegen-inliner.md](pre-codegen-inliner.md), [static-initializers.md](static-initializers.md) (the whole
initialization story, including the redundant-call removal) and [casts-optimization.md](casts-optimization.md)
(`ComputeTypesPass`, `CastsOptimization`, generic safe casts).

## Key concepts

| Term                       | Meaning                                                                                                                                                                                                                                                                  |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| DataFlowIR / DFG           | A per-function "sea of nodes" abstraction of IR that keeps only what matters for types and object flow: calls, allocations, field/array reads and writes, variables, parameters, returns. [DataFlowIR.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DataFlowIR.kt) |
| `ModuleDFG`                | `functions: Map<FunctionSymbol, Function>` plus the `SymbolTable`. Built once by `BuildDFGPhase`, then mutated in place by every phase that rewrites IR. [DFGBuilder.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DFGBuilder.kt) |
| `FunctionSymbol`           | DFG identity of a function or a static field initializer. `Public`/`Private` for declared, `External` for bodiless ones. Carries attributes (`EXPLICITLY_EXPORTED`, `IS_STATIC_FIELD_INITIALIZER`, …) and, for externals, `escapes`/`pointsTo`.                 |
| `Type.Virtual`             | Marker type meaning "do not reason about this receiver": ObjC classes and forward declarations map to it (`SymbolTable.mapClassReferenceType`).                                                                                                               |
| Root set                   | Functions assumed callable from outside the analysed code. Computed by `DevirtualizationAnalysis.computeRootSet` and shared by every analysis via `CallGraphBuilder`.                                                                                             |
| Unfold factor              | Max number of possible callees for which a virtual call site is expanded into direct edges (call graph) or an `if`-chain of direct calls (IR). `DevirtualizationUnfoldFactors`.                                                                                     |
| `Lifetime`                 | EA verdict for an allocation: `STACK`, `STACK_ARRAY(size)` or `GLOBAL` (others exist but are folded to `GLOBAL`). [ContextUtils.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/ContextUtils.kt) |
| `@Escapes` / `@PointsTo`   | Hand-written EA summaries for `external fun`s in the stdlib. [Escapes.kt](../../compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/ir/annotations/Escapes.kt), [PointsTo.kt](../../compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/ir/annotations/PointsTo.kt) |
| GHA                        | Global Hierarchy Analysis: assigns interval ids to classes and colored ids to interfaces so that type checks and interface dispatch become O(1). Class `GlobalHierarchyAnalysis` in [ClassLayoutBuilder.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/ir/ClassLayoutBuilder.kt) |

## How it works

### Phase order and gating

Everything is sequenced in `runCodegen` of
[TopLevelPhases.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/TopLevelPhases.kt);
the phase objects live in [LTO.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/LTO.kt)
and [BitcodeGeneration.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/BitcodeGeneration.kt).

```
optimize                = config.optimizationsEnabled                       // -opt   (ConfigChecks.shouldOptimize)
runGlobalOptimizations  = optimize && !cachedLibraries.hasStaticCaches
enablePreCodegenInliner = preCodegenInlineThreshold != 0 && runGlobalOptimizations   // -Xbinary=preCodegenInlineThreshold=N
```

| # | Phase (name for `-Xdisable-phases`)              | Runs when                                   | Input → output                                                                   |
|---|--------------------------------------------------|---------------------------------------------|----------------------------------------------------------------------------------|
| 1 | `PropertyAccessorInlineLowering`, `InlineClassPropertyAccessorsLowering` | `optimize`                | IR → IR (trivial accessors inlined; comment: not in debug because of separate compilation and debug info) |
| 2 | `BuildDFG`                                       | `runGlobalOptimizations`                    | IR → `ModuleDFG`; when disabled an **empty** `ModuleDFG` is produced             |
| 3 | `RemoveRedundantCallsToStaticInitializersPhase`  | `enablePreCodegenInliner`                   | IR + DFG → IR (initializer calls removed), DFG patched                           |
| 4 | `PreCodegenInliner`                              | `enablePreCodegenInliner`                   | IR + DFG → IR, DFG patched                                                       |
| 5 | `DevirtualizationAnalysis`                       | `runGlobalOptimizations`                    | DFG → `IrCall.devirtualizedCallSite` attributes                                  |
| 6 | `RemoveRedundantCallsToStaticInitializersPhase`  | `runGlobalOptimizations && !enablePreCodegenInliner` | same as 3 (comment KT-72336: this placement is better but conflicts with the inliner) |
| 7 | `Devirtualization`                               | `runGlobalOptimizations`                    | IR + attributes → IR with direct calls / type-switches, DFG patched              |
| 8 | `RedundantCoercionsCleaner`, `UnboxInlineLowering` (`optimize`) | always / `optimize`          | IR cleanup after devirtualization introduced coercions                           |
| 9 | `PreCodegenInliner` (second run)                 | `enablePreCodegenInliner`                   | inlines what devirtualization made direct                                        |
| 10 | `DCEPhase`                                      | `runGlobalOptimizations`                    | IR + DFG → `Set<IrSimpleFunction>?` of live functions; unreferenced functions removed from IR |
| 11 | `CoroutinesVarSpillingLowering`                 | always                                      |                                                                                  |
| 12 | `CreateLLVMDeclarations`                        | always                                      |                                                                                  |
| 13 | `GHAPhase`                                      | `runGlobalOptimizations && !produce.isCache` | sets `layoutBuilder.hierarchyInfo` per class and `context.globalHierarchyAnalysisResult` |
| 14 | `RTTI`                                          | always                                      | consumes the DCE set: vtable/itable slots of dead functions become null pointers |
| 15 | `EscapeAnalysis`                                | `runGlobalOptimizations`                    | DFG + call graph → `Map<IrElement, Lifetime>` (empty map when disabled)          |
| 16 | `Codegen`                                       | always                                      | consumes lifetimes                                                               |

Phases are disabled in two ways that look identical from outside: the driver passes `disable = true` (the table above), or the user
lists the phase name in `-Xdisable-phases`. Both return `outputIfNotEnabled`
([PhaseEngine.kt](../../compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/phaser/PhaseEngine.kt),
[CompilerPhase.kt](../../compiler/config/src/org/jetbrains/kotlin/config/phaser/CompilerPhase.kt)). The analyses are therefore
individually droppable: every consumer tolerates "no result" (`null` DCE set, empty lifetimes, empty DFG, `ghaEnabled() == false`).

**Why `!hasStaticCaches`.** The comment in `runCodegen` says "It's ok to run global optimizations on a cache as long as it doesn't
have other dependencies (stdlib)". The analyses assume they see all code that can run; a prebuilt static cache contains machine code
that was compiled without knowledge of this program (and whose own vtables, initializers etc. are fixed), so none of the closed-world
rewrites below are sound across that boundary. Historically `-opt` ignored caches entirely
(`ignoreCacheReason = "with global optimizations"` in
[NativeSecondStageCompilationConfig.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/NativeSecondStageCompilationConfig.kt));
commit `7829d2f682d0` (KT-80373, KT-86492) added `-Xbinary=enableReleaseBinaryCache` and states explicitly that "no optimization
passes were adapted to work the incremental mode yet". So today: `-opt` + release caches ⇒ global optimizations off.

**GHA and caches.** GHA is additionally skipped when the output *is* a cache (`produce.isCache`): ids are assigned by a DFS over the
entire hierarchy, so a cache compiled separately would get ids inconsistent with the final program. Codegen checks
`context.ghaEnabled()` and falls back to runtime `IsSubtype`/binary-search itable lookups
([CodeGenerator.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/CodeGenerator.kt)
`checkIsSubtype`, `lookupInterfaceTableRecord`; [RTTIGenerator.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/RTTIGenerator.kt)
uses `ClassGlobalHierarchyInfo.DUMMY`). Frameworks never take the "all itables are small" fast path either (`produce != FRAMEWORK`
in `CodeGenerator.kt`), since ObjC subclasses can add interfaces at runtime.

### The DataFlowIR model

`ModuleDFGBuilder.build()` first runs `SymbolTable.populateWith(irModule)`, which maps every class to a `DataFlowIR.Type` (with
`superTypes`, a `vtable` and per-interface `itable` taken from `ClassLayoutBuilder`) and every function with a body, plus every
top-level field initializer, to a `FunctionSymbol`. The table is then **sealed**: `typeHierarchy` is built from it and adding types
afterwards is an error. Then `FunctionDFGBuilder.build(declaration)` turns each body into a `FunctionBody`:

- First pass (`ElementFinderVisitor`): collect "interesting" expressions, the possible values of each variable, returns, throws,
  catch parameters and coroutine live variables. `VariableValues.computeClosure()` makes variable values transitive.
- Second pass: emit nodes. Control flow is discarded; a `Variable` node holds all possible values, `Scope` nodes form a tree (used by
  EA for `STACK` lifetimes), `Edge.castToType` records casts, `Call` nodes keep `irCallSite` so results can be mapped back to IR.
  Virtual calls become `VtableCall`/`ItableCall` with the receiver type and slot index.

Nodes that matter for the consumers: `Alloc{Instance,Array}`, `Singleton` (object instance access, treated as a constructor call by
`CallGraphBuilder.forEachCallSite`), `FieldRead/FieldWrite`, `ArrayRead/ArrayWrite` (carry the intrinsic callee so arrays look like
calls with `@PointsTo` summaries), `FunctionReference`, `SaveCoroutineState`.

Debugging aid: `DataFlowIR.Function.debugString()`; `context.logMultiple { }` dumps the symbol table, the constraint graph, the
condensation and devirtualization statistics when the backend logger is enabled (`-Xverbose-phases=<name>` turns it on per phase).

### Root set and call graph (shared by all analyses)

`DevirtualizationAnalysis.computeRootSet`
([DevirtualizationAnalysis.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DevirtualizationAnalysis.kt))
decides what the "outside world" can call:

- **Program** (`symbols.entryPoint != null`, which is only the case for `-produce program`, see `getMainCallableId` in
  [Ir.kt](../../compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/ir/Ir.kt)): the entry point only.
- **Anything else** (framework, static/dynamic library, cache, test bundle): every `FunctionSymbol.Public`, every exported
  `External`, and every function sitting in some vtable/itable. `Public` means `KonanBinaryInterface.isExported`
  ([BinaryInterface.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/BinaryInterface.kt)):
  the mangler's visibility-based export check, or one of `ANNOTATIONS_TO_TREAT_AS_EXPORTED` (`@SymbolName`, `@GCUnsafeCall`,
  `@ExportForCppRuntime`, `@CName`, `@ExportForCompiler`) in
  [KonanMangler.kt](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/KonanMangler.kt).
  This is how ObjC export and C export keep their targets alive: the exported binaries have no entry point, so all public API is a
  root; there is no ObjC-export-specific logic in `optimizations/`.
- Always: static field initializers and eager static initializers; functions with `EXPLICITLY_EXPORTED` (`@ExportForCppRuntime`,
  `@ExportedBridge`, `@BindReverseBridgeToMethod`, external ObjC methods, `@ObjCMethodImp`; set in `SymbolTable.mapFunction`);
  constructors of associated objects (reflection via `findAssociatedObject`); and targets of `IrRawFunctionReference` ("leaking to
  the native world", e.g. `staticCFunction`).

`CallGraphBuilder` ([CallGraphBuilder.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CallGraphBuilder.kt))
walks the DFG from the roots. A virtual call site is resolved in three ways: if `devirtualizedCallSite` is set and has at most
`devirtualizedCallSitesUnfoldFactor` callees, direct edges; otherwise all non-abstract inheritors of the receiver type via
`typeHierarchy.inheritorsOf` if at most `nonDevirtualizedCallSitesUnfoldFactor`; otherwise it stays a `isVirtual` call site and all
candidates are promoted to roots. Receivers of `Type.Virtual` are skipped (ObjC). The factors chosen per client encode different
precision/size trade-offs: DCE uses `-1/-1` (never unfold, everything reachable is a root), EA uses `5` and `5` for a program but
`-1` for a library ("Can't tolerate any non-devirtualized call site for a library" — unknown subclasses may exist), the inliner `-1/-1`.

### Devirtualization

Variable Type Analysis (Sundaresan et al., OOPSLA'00, cited in the file). `DevirtualizationAnalysisImpl.analyze()` builds a
constraint graph over DFG nodes seeded with the root set, computes `instantiatingClasses` (classes whose constructor is reachable),
condenses it and propagates type sets through cast edges. For each `VirtualCall` it records
`DevirtualizedCallSite(callee, possibleCallees)` on the IR call via the `IrCall.devirtualizedCallSite` attribute (`irAttribute`,
copied on IR copy). Instances of `Type.Virtual` are never devirtualized, abstract classes are never instantiated.

`DevirtualizationAnalysis.devirtualize` (the `Devirtualization` phase) rewrites IR:
- 0 possible callees: replace the call with `throwInvalidReceiverTypeException` (unreachable by construction).
- 1 callee and `optimize`: a direct `IrCall` to the implementation (`irDevirtualizedCall`), with box/unbox coercions re-derived
  from the DFG parameter types (`getTypeConversion`).
- ≤ `IR_DEVIRTUALIZED_{VTABLE,ITABLE}_CALL` (= 3) callees: a `when` on the receiver's type info; branch order is found by
  backtracking so that derived types are tested before base types and the slowest check (interface) lands in the `else` branch. In
  non-`optimize` mode an extra `else` throws, which is what makes devirtualization debuggable with `-g -Xbinary=...`.
- more callees: left virtual.

Every rewritten declaration is re-run through `FunctionDFGBuilder` and stored back into `moduleDFG.functions`.

### DCE

[DCE.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DCE.kt)
`dce()` builds the call graph without unfolding, marks every node and every non-virtual callee as referenced (plus `bridgeTarget`s,
needed for class layout, and `invoke` of function classes), then **removes** unreferenced `IrSimpleFunction`s from files and classes
and nulls unreferenced property accessors. Classes and fields are not removed. The returned set goes to `RTTIPhase`, which writes
`nullPointer` into vtable and itable slots whose implementation is dead; this is why a call through a stale vtable slot after a wrong
DCE decision segfaults on a null function pointer rather than calling the wrong code.

### Escape analysis

[EscapeAnalysis.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/EscapeAnalysis.kt)
has a long header comment that is the authoritative description. Summary: an interprocedural points-to analysis over the call-graph
condensation in reverse topological order. Each function gets a compressed summary (which parameters/return value point to or
escape into which), used at call sites in callers; SCCs are iterated to a fixpoint with divergence guards
(`DivergenceResolutionParams`: 3 attempts, swelling factor 25) falling back to the pessimistic result.

Callee summaries come from three places (`callSiteEAResult` region around line 700):
- declared functions: their computed summary;
- virtual, non-unfolded call sites: pessimistic;
- external functions: `@Escapes`/`@PointsTo` **only if the name starts with `kfun:kotlin.`** and is not under
  `kotlin.concurrent`/`kotlin.native.concurrent`; any other external (cinterop, user `external fun`) is pessimistic. A stdlib external
  with reference parameters and no annotation is a hard `require` failure — the stdlib build enforces this earlier through
  `EscapeAnalysisChecker` ([EscapeAnalysisChecker.kt](../../compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/checkers/EscapeAnalysisChecker.kt),
  run from `SpecialBackendChecksTraversal` in the first stage, meaningful only when compiling the stdlib). The annotation semantics
  (bitmask over dispatch receiver, extension receiver, parameters, return value; `@PointsTo` kinds 1–4 with "intestines" for array
  contents) are documented in
  [Annotations.kt](../../kotlin-native/runtime/src/main/kotlin/kotlin/native/internal/escapeAnalysis/Annotations.kt) in the runtime;
  about 200 stdlib externals carry them.

`computeLifetimes()` turns points-to depth into `Lifetime`: only `STACK` survives, everything else becomes `GLOBAL` (comment: other
lifetimes need arenas). Extra rules: arrays become `STACK_ARRAY(len)` only with a constant non-negative length and a per-function
budget of 65536 bytes (`allowedToAlloc`, smallest first); allocations of classes with a finalizer (`hasFinalizer`, ObjC-backed) are
forced to escape; when `escapeAnalysisPropagateExiledToHeapObjects` (binary option, default `true` for every GC since commit
f7d3b8074e7c; CMS and the memory dumper cannot tolerate heap→stack references, KT-75317, KT-85811) is set, an object demoted
from stack to heap is also treated as an escape origin so that objects it references are not stack-allocated either (KT-69731).
The result map is keyed by the IR allocation element; call results are not recorded and default to `GLOBAL` in codegen. The
full algorithm is in [escape-analysis.md](escape-analysis.md).

Consumption: `CodeGenerator.allocInstance`/`allocArray` call `stackLocalsManager.alloc`/`allocArray` for `STACK`/`STACK_ARRAY`
and the runtime `AllocInstance`/`AllocArray` otherwise. `IrToBitcode.resultLifetime` defaults to `GLOBAL` for elements absent from
the map, so an empty map is always safe. Any exception inside EA is turned into a compilation error whose text tells the user to add
`-Xdisable-phases=EscapeAnalysis`.

### Pre-codegen inliner

[PreCodegenInliner.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/PreCodegenInliner.kt)
is a bottom-up (tricolor DFS over the call graph) inliner that uses DFG node count as the size metric: a callee is inlined if it is
not recursive, has `≤ preCodegenInlineThreshold` DFG nodes, is not `main`, not box/unbox, not a static initializer, not from a
cinterop klib, not `@NoInline`, not `invokeSuspend`, not an inline-class property getter, not a String/array/Throwable constructor.
Callers that are bridges, cinterop functions or the special `KFunctionImpl` constructors are never modified (comment: pattern
recognition in `IrToBitcode.kt`). After inlining it re-runs coroutine liveness analysis and rebuilds the function's DFG. It is off by
default (`preCodegenInlineThreshold` defaults to `0`); tests enable it with `-Xbinary=preCodegenInlineThreshold=40`.

### Static initializer call removal

[StaticInitializersOptimization.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/StaticInitializersOptimization.kt)
(header comment describes the three-step dataflow). `StaticInitializersLowering` ([lowerings.md](lowerings.md)) inserts a call to the
file's global/thread-local initializer as the first statement of every function that needs it. This pass computes, per function and
per call site, the set of files that are definitely initialized, and deletes the leading initializer call where it is redundant
("the very first statements by construction", `take(2)`). It is interprocedural and uses devirtualized call sites, so a wrong
devirtualization can manifest as "my top-level `val` is not initialized".

### Smaller pieces in `optimizations/`

- `ComputeTypesPass` is a lowering, not a DFG client. It runs **twice** in `getLoweringsAfterInlining()`
  ([NativeLoweringPhases.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/NativeLoweringPhases.kt)):
  first right after `NativeFinallyBlocksLowering` ("Inliner erases generics. Trying to restore some of the information and simplify
  IR."), second after `StaticInitializersLowering` ("Running 2nd time not only helps the following heavy analysis but also corrects
  some lowerings' inaccuracies in IR types").
- `CastsOptimization` runs only when `genericSafeCasts` is on (default `!optimizationsEnabled`, "disabled in -opt due to performance
  penalty"); it is a predicate-based elimination of redundant `is`/casts. `CastsLowering` runs only when optimizations are off.
- `NativeForLoopsLowering` plugs the bounds-check-eliminating body transformer into the common for-loop lowering.
- `handlePerformanceInlineAnnotation` marks runtime functions annotated `performance_inline` as `alwaysinline` when
  `inlineForPerformance` (`!debug && !smallBinary`); `RemoveRedundantSafepointsPass` is a thin wrapper over the `libllvmext` pass
  run from `runBitcodePostProcessing` in [Bitcode.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/Bitcode.kt).
  Both are LLVM-level; see [llvm-codegen.md](llvm-codegen.md).

## Invariants and ordering constraints

- **The DFG must mirror the IR.** Every phase that rewrites IR (devirtualization, both inliner runs, static-initializer removal)
  ends with `FunctionDFGBuilder(...).build(declaration)` for each changed declaration and stores it in `moduleDFG.functions`. A new
  IR rewrite between `BuildDFG` and `EscapeAnalysis` that forgets this makes EA/DCE reason about stale bodies — the classic source of
  "stack-allocated object escapes" corruption.
- **The symbol table is sealed after `populateWith`.** Introducing a new class or a new function with a body after `BuildDFG` fails
  with "The symbol table has been sealed". Lowerings that create declarations must run before it (which is why
  `PropertyAccessorInlineLowering` etc. are placed before, and only cleanup-style lowerings after).
- **Devirtualization analysis must precede static-initializer removal and the call graph builders that unfold** — they read
  `devirtualizedCallSite`. DCE must run after devirtualization (so direct callees are known) and before `RTTI`/`Codegen`.
- **GHA must run after DCE and before `RTTI`**, since it assigns ids consumed by `RTTIGenerator` and `CodeGenerator.checkIsSubtype`.
  Interface ids are also used by `SymbolTable` (`iface.classId`) — but when GHA is disabled `classId` falls back to a hash, so the
  itable keys are stable per run either way.
- **Every analysis is optional**: downstream code must keep working with `null`/empty results. Keep this property when adding one.
- **Closed world only for programs.** `entryPoint == null` switches all analyses to "library mode" (every public symbol is a root,
  no unfolding of non-devirtualized calls in EA). ObjC classes are `Type.Virtual` and never devirtualized; cinterop and user
  `external fun`s get pessimistic EA summaries.
- **DCE deletes functions but not classes.** Code that later iterates class members (RTTI, ObjC export) must handle missing
  implementations (`referencedFunctions?.contains(impl) == false` → null slot).
- Phase prerequisites are only checked for lowerings (`@PhasePrerequisites`, e.g. `ComputeTypesPass` requires
  `FinallyBlocksLowering`); the DFG phases rely on the order in `runCodegen` alone.

## Extension points and typical changes

| Change                                                  | Where, and what else must move                                                                                                                                                      |
|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Add a new stdlib `external fun` with reference params   | Annotate it with `@Escapes`/`@Escapes.Nothing`/`@PointsTo` in the runtime sources; `EscapeAnalysisChecker` fails the stdlib build otherwise, and EA `require`s it at link time.    |
| Make something new reachable "from outside"             | Extend `computeRootSet` (or set `EXPLICITLY_EXPORTED` in `SymbolTable.mapFunction`). Both devirtualization and DCE pick it up; EA roots follow through `CallGraphBuilder`.          |
| New IR rewrite inside `runCodegen`                      | Place it relative to `BuildDFG`; if after, rebuild DFG for changed functions; if it creates declarations, place it before.                                                          |
| Change class/interface id layout (GHA)                  | `ClassGlobalHierarchyInfo`, `RTTIGenerator` and the runtime type-check code (`IsSubclassFast`, itable lookup in `kotlin-native/runtime`) form one contract; see [compiler-runtime-contract.md](compiler-runtime-contract.md). |
| New `Lifetime` kind (e.g. `LOCAL` arenas)               | `ContextUtils.Lifetime`, `computeLifetimes`, `CodeGenerator.allocInstance/allocArray`, `StackLocalsManager`, plus GC support in the runtime ([runtime.md](runtime.md)).            |
| New unfold heuristics                                   | `DevirtualizationUnfoldFactors`; IR-side in `devirtualize`, graph-side in `CallGraphBuilder.handleFunction`.                                                                        |
| New binary option for an analysis                       | [BinaryOptions.kt](../../native/binary-options/src/main/kotlin/org/jetbrains/kotlin/config/nativeBinaryOptions/BinaryOptions.kt) → `NativeSecondStageCompilationConfig` → consumer. |

Cross-subsystem impact: caches ([caches.md](caches.md)) are the reason for most gating; ObjC/C export ([export.md](export.md)) rely
on library-mode roots; cinterop ([cinterop.md](cinterop.md)) is opaque to EA and excluded from inlining.

## Investigating bugs here

### Flags

| Flag                                                   | Effect                                                                                                          |
|--------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `-opt` / `-g`                                          | `optimizationsEnabled` (the K/N test infra maps `optimizationMode=OPT` to `-opt`, `DEBUG` to `-g`)              |
| `-Xdisable-phases=EscapeAnalysis,Devirtualization,DevirtualizationAnalysis,DCEPhase,PreCodegenInliner,RemoveRedundantCallsToStaticInitializersPhase,GHAPhase,BuildDFG,ComputeTypesPass,CastsOptimization` | bisect; names are the `name =` strings in `LTO.kt` (lowerings use their class name) |
| `-Xlist-phases`, `-Xverbose-phases=<name>`, `-Xprofile-phases` | enumerate; enable `context.log` output (DFG dumps, constraint graphs, "Devirtualized: N%"); time them |
| `-Xphases-to-dump-after=<name>` (`-Xdump-directory=`, `-Xdump-fqname=`) | IR dump after a phase, e.g. after `Devirtualization` or `DCEPhase`                            |
| `-Xbinary=preCodegenInlineThreshold=N`                 | enable the pre-codegen inliner                                                                                  |
| `-Xbinary=genericSafeCasts=true|false`                 | toggles `CastsOptimization` vs `CastsLowering`                                                                  |
| `-Xbinary=escapeAnalysisPropagateExiledToHeapObjects=` | see EA above                                                                                                    |
| `-Xbinary=enableReleaseBinaryCache=true`               | use caches in `-opt` (turns global optimizations off)                                                           |
| `-Xsave-llvm-ir-after=<phase> -Xsave-llvm-ir-directory=`, `-Xprint-bitcode` | see the LLVM IR, e.g. check for `alloca %"kclassbody:..."` vs `call @AllocInstance` |

There is no dedicated "disable devirtualization" or "disable EA" flag; `-Xdisable-phases` is the mechanism (all flags live in
[K2NativeCompilerArguments.kt](../../compiler/cli/cli-base/gen/org/jetbrains/kotlin/cli/common/arguments/K2NativeCompilerArguments.kt)
and [CommonCompilerArguments.kt](../../compiler/cli/cli-base/gen/org/jetbrains/kotlin/cli/common/arguments/CommonCompilerArguments.kt)).

### Symptoms → where to look

| Symptom                                                              | First suspects                                                                                                                                                 |
|----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Works with `-g`, wrong/crashes with `-opt`; works with `-opt` + caches | Something in this page. Bisect: `-Xdisable-phases=EscapeAnalysis` first, then `Devirtualization`, `DCEPhase`, `RemoveRedundantCallsToStaticInitializersPhase`. |
| Memory corruption, GC crash, use-after-return, object "changes" later | EA: a `STACK` object escaped. Check `-Xsave-llvm-ir-after=Codegen` for `alloca` of the class; look for missing/incorrect `@Escapes`/`@PointsTo` on a stdlib external the code calls, or an IR rewrite that did not rebuild the DFG. |
| `InvalidReceiverTypeException` / wrong override called                | Devirtualization: type set too small. Candidates: a class instantiated outside the root set (reflection, `@ExportForCppRuntime`, raw function reference), a cast not modelled (`Edge.castToType`). Compare with `-Xdisable-phases=Devirtualization`. |
| Null function pointer call / "unresolved" vtable slot at runtime       | DCE removed a function the runtime or export calls. Check `computeRootSet` and `EXPLICITLY_EXPORTED` attributes.                                               |
| Top-level `val` uninitialized / lazy init skipped only in release      | `RemoveRedundantCallsToStaticInitializersPhase`; also `UnboxInlineLowering` interaction (KT-72336 comment).                                                     |
| `IsSubtype`/`is` wrong only in release, or itable lookup failure       | GHA ids vs runtime (`IsSubclassFast`, interface color bits, `MAX_BITS_PER_COLOR`); frameworks/caches must not rely on the fast path.                           |
| "The symbol table has been sealed" / "No IR for: …" compile error      | A declaration created after `BuildDFG`, or a DFG symbol without `irDeclaration` reaching DCE.                                                                  |
| "Escape analysis failure" compile error                               | EA threw; the message already suggests `-Xdisable-phases=EscapeAnalysis`. Reproduce with `-Xverbose-phases=EscapeAnalysis`.                                    |

### Tests

| What                                                                                       | Where / how to run                                                                                                                                                                                                 |
|--------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Runtime checks of stack allocation (`a.isStack()` from `kotlin.native.internal.Debugging`) | `native/native.tests/testData/codegen/escapeAnalysis/` (13 files, `DISABLE_NATIVE: optimizationMode=DEBUG`, `cacheMode=STATIC_*`). Class `FirNativeCodegenLocalTestGenerated`: `./gradlew :native:native.tests:test --tests "org.jetbrains.kotlin.konan.test.blackbox.FirNativeCodegenLocalTestGenerated\$EscapeAnalysis*" -Pkn.optimizationMode=OPT -Pkn.cacheMode=NO` |
| LLVM IR shape via FileCheck (`// FILECHECK_STAGE: CStubs`, `CHECK-OPT:` / `CHECK-DEBUG:`)  | `native/native.tests/testData/codegen/fileCheck/` — `escape_analysis.kt`, `inline.kt`, `inline_property.kt`, `bridges.kt` (`-Xbinary=preCodegenInlineThreshold=40`), `generic_safe_casts.kt`, `kt86948.kt` (`-Xdisable-phases=ComputeTypesPass`), `kt86949.kt` (`-Xdisable-phases=CastsOptimization`), `bce.kt`. Same test class, `…\$FileCheck*`; run with and without `-Pkn.optimizationMode=OPT`. |
| Shared blackbox tests in release mode                                                      | `./gradlew :native:native.tests:codegen-box:test --tests "org.jetbrains.kotlin.konan.test.blackbox.FirNativeCodegenBoxTestGenerated\$Box\$*" -Pkn.optimizationMode=OPT`. ~35 files carry `// IGNORE_NATIVE: optimizationMode=OPT`; `compiler/testData/codegen/box/inline/coercionToUnitBoxing.kt` uses `-Xdisable-phases=DCEPhase`. |
| Test modes relevant here                                                                   | `-Pkn.optimizationMode=OPT`, `-Pkn.cacheMode=NO` (global optimizations are off with `STATIC_ONLY_DIST`, the default), `-Pkn.gcType=CMS` (exercises `escapeAnalysisPropagateExiledToHeapObjects`). See [HACKING.md](../../kotlin-native/HACKING.md#testing). |
| Unit tests                                                                                 | None for `optimizations/`; `kotlin-native/backend.native/tests` contains only samples. `:kotlin-native:backend.native:check` covers other backend units.                                                              |
| Benchmarks                                                                                 | `kotlin-native/performance` (`ring`, `numerical`, `startup`, …; `./gradlew :konanRun` inside that folder, see its README). Use them to judge heuristics (unfold factors, inline threshold), not correctness.         |

Remember that tests load the compiler from `kotlin-native/dist`, so a change here costs a dist rebuild per iteration
([kotlin-native/AGENTS.md](../../kotlin-native/AGENTS.md)).

## Open questions

- Why `GHAPhase` is also allowed for frameworks and dynamic libraries while caches are excluded: the code only checks
  `produce.isCache`; the framework restriction appears only in the codegen fast path. Hypothesis: ids are self-consistent within one
  binary whatever its kind, and only a separately compiled cache breaks that; not confirmed by comments or history.
- `EscapeAnalysis` for libraries: `nonDevirtualizedCallSitesUnfoldFactor = -1` with the TODO "What about private virtual
  functions?" — unverified whether library-mode EA ever yields `STACK` for objects passed to virtual calls.
- KT-72336: the second `PreCodegenInliner` run and the `RemoveRedundantCallsToStaticInitializersPhase` placement are described as
  interim; the intended final ordering is not documented in the tree.
- `TAKE_NAMES` is `true` in `SymbolTable` and `false` in `DevirtualizationAnalysis`; whether names are needed for correctness
  anywhere (e.g. the `kfun:kotlin.` prefix check in EA depends on `FunctionSymbol.name`) was not traced for the `false` case.
- The `allowedToAlloc = 65536` per-function stack-array budget and the unfold factors have no recorded rationale ("TODO: To a
  setting?", "TODO: Think how to evaluate different unfold factors").
- Whether ObjC export bridges (`ObjCExport` created inside `CodegenPhase`) can reference a Kotlin method that DCE removed in
  library mode was not verified; the root set makes all public functions live, so it should not happen, but private methods
  reachable only via generated bridges were not checked.
