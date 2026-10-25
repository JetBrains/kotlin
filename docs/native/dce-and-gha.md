# Kotlin/Native: dead-code elimination and global hierarchy analysis

Two whole-program passes of the second stage work on the shape of the program rather than on data flow: **DCE** decides which
functions can run at all and deletes the rest from the IR, and **GHA** (global hierarchy analysis) numbers every class and
interface so that `is` checks and interface dispatch become a couple of instructions. Both are gated on the closed-world
condition described in [optimizations.md](optimizations.md) (`runGlobalOptimizations`), both feed `RTTIGenerator`, and both
silently shape the ABI that caches, frameworks and the runtime rely on. This page is the deep dive; the pipeline overview,
the DFG model and the phase table are in [optimizations.md](optimizations.md) and are not repeated.

Paths below are relative to `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/`
("BACKEND") unless stated otherwise.

## Where they sit

```
BuildDFG → [inliner] → DevirtualizationAnalysis → Devirtualization → [inliner] → DCEPhase → CoroutinesVarSpilling
         → CreateLLVMDeclarations → GHAPhase → RTTIPhase → EscapeAnalysis → Codegen
```
([TopLevelPhases.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/TopLevelPhases.kt)
`runCodegen`; phase objects in
[LTO.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/LTO.kt)).

| Phase (`-Xdisable-phases` name) | Gate                                              | Output                                                                 |
|---------------------------------|---------------------------------------------------|------------------------------------------------------------------------|
| `DCEPhase`                      | `runGlobalOptimizations` (= `-opt` and no static caches) | `Set<IrSimpleFunction>?` of live functions; dead ones removed from IR; `null` when disabled |
| `GHAPhase`                      | `runGlobalOptimizations && !produce.isCache`      | `layoutBuilder.hierarchyInfo` per class, `context.globalHierarchyAnalysisResult` |

`isCache` covers `STATIC_CACHE`, `DYNAMIC_CACHE` and `HEADER_CACHE`
([CompilerOutputKind.kt](../../native/utils/src/org/jetbrains/kotlin/konan/target/CompilerOutputKind.kt)).

## DCE

### Algorithm

[DCE.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DCE.kt)
`dce()` is ~100 lines; all the intelligence is in the call graph it asks for.

1. **Build the call graph without unfolding.** `CallGraphBuilder(devirtualizedCallSitesUnfoldFactor = -1,
   nonDevirtualizedCallSitesUnfoldFactor = -1)` ("Do not devirtualize anything to keep the graph smaller (albeit less precise
   which is fine for DCE)"; commit `0cdd886e12c8` "DCE: use smaller call graph for less memory consumption"). With `-1` every
   virtual call site stays `isVirtual`, and `CallGraphBuilder.handleFunction`
   ([CallGraphBuilder.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CallGraphBuilder.kt))
   promotes **all** candidate targets to roots via `handleRoot`: the devirtualized set if `devirtualizedCallSite` is present
   (a single-callee site has already been rewritten into a direct `IrCall` by the `Devirtualization` phase, so it arrives as
   a plain `Call`), otherwise every non-abstract inheritor's vtable/itable slot via `typeHierarchy.inheritorsOf`. Receivers of
   `Type.Virtual` (ObjC) are skipped. Edges therefore are: direct calls, constructor calls, `Singleton` accesses (object
   instance = constructor call, `forEachCallSite`), array reads/writes (intrinsic callees), static-initializer calls, and the
   "all overrides" fan-out of every reachable virtual call.
2. **Reference everything reachable.** Every graph node (except static field initializers, which are the DFG's synthetic
   `<field>_init` symbols and have no `IrSimpleFunction`), every non-virtual callee, and `rootExternalFunctions` (roots without
   a DFG body). `referenceFunction` also adds `irFunction.bridgeTarget`: commit `4308a3c8f34b` / KT-74377 — the target can be
   inlined into its bridge and vanish from the graph, but `ClassLayoutBuilder` still needs it to compute layouts (release-only
   segfault with value classes).
3. **Keep `invoke` of `kotlin.FunctionN`.** An explicit visitor marks `invoke` whose parent class `defaultType.isFunction()`
   ("TODO: Generalize somehow, not that graceful"; present since the port to the new driver, `576e8ba12737`). *Unverified:*
   the reason; Hypothesis: codegen of function references/`KFunctionImpl` needs these abstract declarations to compute itable
   places.
4. **Delete.** `IrFile.declarations` lose unreferenced `IrSimpleFunction`s; `IrClass.declarations` lose unreferenced **real**
   ones (`isReal`: fake overrides stay, they carry no code); property getters/setters are nulled. `visitClass` contains an
   intended exemption `if (declaration == context.symbols.nativePointed)` (since `4ccc9b616964`, no comment), but it compares
   an `IrClass` with an `IrClassSymbol` (`symbols.nativePointed` is used with `isSubtypeOfClass` elsewhere), so it is never
   true and `NativePointed` members are treated like any other. Classes, fields and constructors are never deleted —
   constructors are already lowered to functions by `ConstructorsLowering` and are deleted as such.

The returned set is what `RTTIPhase` consumes; the IR mutation is what makes `CreateLLVMDeclarations` and `Codegen` skip dead
functions — there is no "is this function dead" check in `IrToBitcode.kt`; the function simply is no longer in the module.

### Root set, per output kind

`DevirtualizationAnalysis.computeRootSet`
([DevirtualizationAnalysis.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DevirtualizationAnalysis.kt))
is shared by DCE, devirtualization, EA and the inliner:

| Source                                | Program (`entryPoint != null`)      | Framework / static / dynamic / test bundle / cache                     |
|---------------------------------------|-------------------------------------|------------------------------------------------------------------------|
| Entry point                           | `main` (only root of this class)    | —                                                                      |
| `FunctionSymbol.Public`, exported `External` | —                            | all (Public = `KonanBinaryInterface.isExported`, i.e. public/protected ABI or `@SymbolName`/`@GCUnsafeCall`/`@ExportForCppRuntime`/`@CName`/`@ExportForCompiler`) |
| Every function in any vtable/itable   | —                                   | all (`classMap.values.flatMap { vtable + itable }`)                    |
| Static field initializers, eager static initializers | always              | always                                                                 |
| `EXPLICITLY_EXPORTED`                 | always                              | always                                                                 |
| Associated-object constructors        | always ("Conservatively assume each associated object could be called" — `findAssociatedObject` reflection) | always |
| `IrRawFunctionReference` targets      | always ("leaking to the native world", e.g. `staticCFunction`) | always                                      |

`EXPLICITLY_EXPORTED` is set in `SymbolTable.mapFunction`
([DataFlowIR.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DataFlowIR.kt))
for `@ExportForCppRuntime`, `@ExportedBridge` (Swift export), `@BindReverseBridgeToMethod`, external ObjC methods
(`getExternalObjCMethodInfo() != null`, marked `TODO-DCE-OBJC-INIT` since the original DCE commit `63d763338dcf`) and
`@ObjCMethodImp`. Consequence for libraries: because **every vtable/itable member is a root**, DCE in a framework never nulls a
vtable slot; it only removes non-overridable functions that are not public ABI (private/internal top-level or final members)
and unreachable from public API. In a program, by contrast, any override no reachable virtual call can target is deleted.

Reflection has no special support beyond the associated-object rule: `::class`, `typeOf`, enum `entries` are ordinary calls
after lowering and are kept by ordinary reachability. `KClass.isInstance` goes through `Kotlin_TypeInfo_isInstance`
([KClassImpl.kt](../../kotlin-native/runtime/src/main/kotlin/kotlin/native/internal/KClassImpl.kt)) and needs no function.

### How the result is consumed

- **RTTI.** `RTTIInput.referencedFunctions` → `RTTIGenerator.vtable()` and `interfaceTableRecords()` emit `llvm.nullPointer`
  for an implementation not in the set
  ([RTTIGenerator.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/RTTIGenerator.kt)).
  The slot *layout* is unchanged (indices come from `ClassLayoutBuilder` over all declared members); only the pointer is null.
  So if a "dead" method is reached at run time through a vtable — a wrong root set, a class instantiated by code the analysis
  cannot see — the program calls address 0 and dies with a segfault, not with a Kotlin exception. This is the
  "null function pointer in `kfun:`" symptom in [debugging.md](debugging.md).
- **ObjC export** creates its own `RTTIGenerator(generationState, referencedFunctions = null)` ("TODO: pass referenced
  functions explicitly",
  [ObjCExportCodeGenerator.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/objcexport/ObjCExportCodeGenerator.kt)).
  It is safe only because frameworks are in library mode where vtable/itable members are roots; type adapters copy vtables
  of exported classes whose methods are therefore all live.
- **Codegen** never sees dead functions (removed from IR); the `ModuleCallsChecker` runs after DCE for the same reason
  ([llvm-codegen.md](llvm-codegen.md)).
- **LLVM** still runs `internalize` + `globaldce` on final binaries ([llvm-codegen.md](llvm-codegen.md)), so with DCE off
  (debug, caches) unreferenced *non-virtual* code is still dropped at link time; what LLVM cannot drop is anything reachable
  from a vtable/itable constant — which is exactly what IR-level DCE nulls out. That is the binary-size cost of static caches
  and of `-g`: all overrides of all classes survive. KT-74377's discussion states the trade-off explicitly: DCE removing
  overrides "contributes to DCE quality" and "code size is a concern for KMP on iOS".

### Ordering constraints

- **After `Devirtualization`**: single-callee virtual calls are then plain `IrCall`s, so the non-unfolding call graph sees them
  as direct edges instead of promoting all overrides to roots. Running DCE earlier would be sound but keep far more code.
- **After the second pre-codegen inliner run** ([pre-codegen-inliner.md](pre-codegen-inliner.md)): inlined callees lose
  their last caller and are then deleted; this is why FileCheck tests cannot run with the inliner on (commit `1c56325df9ee`:
  "every function might be inlined and DCEd afterwards").
- **Before `CreateLLVMDeclarations`/`RTTI`/`Codegen`**, and before `GHAPhase` (GHA walks the IR, but only classes, which DCE
  never removes, so this order is a convenience rather than a necessity).

## GHA

### Purpose

Without GHA, `is C` is a runtime call `IsSubtype` that walks `superType_` links (or linearly scans `implementedInterfaces_`
for an interface), and an interface call is `LookupInterfaceTableRecord` doing a linear/binary search
([Types.cpp](../../kotlin-native/runtime/src/main/cpp/Types.cpp), [TypeInfo.cpp](../../kotlin-native/runtime/src/main/cpp/TypeInfo.cpp)).
GHA makes both O(1): a class check becomes two integer comparisons, an interface check/call becomes `and` + `gep` + load.
Introduced by commits `8a6146ac0881` "Implemented fast type check for classes" and `2fe39525d497` "Implemented fast
interface call & check" (both 2019).

### Algorithm ([ClassLayoutBuilder.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/ir/ClassLayoutBuilder.kt), `GlobalHierarchyAnalysis.run`)

**Classes: DFS intervals.** Build `immediateInheritors` for every non-interface class (super = `getSuperClassNotAny() ?: Any`),
then DFS from `Any` with a global counter: `enterTime` on entry, `exitTime` after the subtree. `v` is an ancestor of `w` iff
`interval(v) ⊇ interval(w)`; since intervals are nested or disjoint, a TypeInfo needs to store only its own `enterTime`
(`classId_`), and `w is v` is `v.lo <= w.classId <= v.hi`. Stored as `ClassGlobalHierarchyInfo(classIdLo = enterTime,
classIdHi = exitTime, interfaceId = 0)`. `Any` gets `enterTime = -1` "in order to correctly generate classes for ObjC
blocks": `generateSyntheticInterfaceImpl` (used by
[BlockPointerSupport.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/objcexport/BlockPointerSupport.kt)
for ObjC blocks wrapped as Kotlin function objects) hard-codes `ClassGlobalHierarchyInfo(-1, -1, 0)`, so such objects pass
`is Any` (`-1 >= -1`) and fail every other class check (all real classes have `lo >= 1`).

**Interfaces: coloring.** `InterfacesForbiddennessGraph.build` registers every interface as a vertex and, for every class,
adds an edge between each pair of its `implementedInterfaces` (the transitive closure of super-interfaces,
[IrUtils.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/ir/IrUtils.kt)).
`computeColoringGreedy` colors vertices in IR order with the smallest free color. `bitsPerColor = ceil(log2(maxColor + 1))`;
each interface gets `interfaceId = color | (ordinalWithinColor << bitsPerColor)`, numbered from 1 per color ("reserve 0 for
invalid value"; `kInvalidInterfaceId` in the runtime). Result: two interfaces implemented by the same class never share the
low `bitsPerColor` bits, hence never collide in a table of size `2^bitsPerColor` indexed by `id & (size - 1)` — a perfect hash
by construction. `context.globalHierarchyAnalysisResult = GlobalHierarchyAnalysisResult(bitsPerColor)` is the only global
output; `ghaEnabled()` is literally "is this `lateinit` initialized"
([NativeBackendContext.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/NativeBackendContext.kt)).

**Per-class itable** (`RTTIGenerator.interfaceTableSkeleton`): try sizes 1, 2, 4, … up to `2^MAX_BITS_PER_COLOR = 32`
("32-items table seems like a good threshold") and take the first power of two in which the class's interface ids do not
collide under `id & (size-1)`. Found → `interfaceTableSize_ = size - 1` (a mask) and records placed at their hashed index,
empty slots have `id = 0`. Not found → records sorted by id, `interfaceTableSize_ = -count`, and a `require` that no two
interfaces share an id. Note the search starts at 1, so a class implementing one interface gets a 1-entry table whatever the
colors, and the optimal size may be smaller than `2^bitsPerColor`. Interfaces, abstract classes and ObjC classes get no
itable (`needInterfaceTable`).

### Consumers

| Consumer | With GHA | Without GHA (`ghaEnabled()` false) |
|----------|----------|-------------------------------------|
| `ClassLayoutBuilder.classId` | interfaces: `hierarchyInfo.interfaceId`; classes: `classIdLo`; Kotlin ObjC classes: 0 | interfaces: `localHash(fqName)`; classes: 0 |
| `RTTIGenerator.generate` → `TypeInfo.classId_` | as above | 0 / hash |
| `VirtualTablesLookup.checkIsSubtype` ([CodeGenerator.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/CodeGenerator.kt)) | class: `IsSubclassFast(typeInfo, lo, hi)` (runtime inline-able function, two compares); interface: `itable[place].id == interfaceId` | `IsSubtype(typeInfo, dstTypeInfo)` runtime walk |
| `VirtualTablesLookup.getVirtualImpl` (interface call) | `getInterfaceTableRecord(...).vtable[methodIndex]` | same, but the record lookup is always branchy (below) |
| `SymbolTable` (DFG) | — built *before* GHA, so `iface.classId` is the hash there; keys are internal to the DFG | hash |
| Devirtualization type switches ([optimizations.md](optimizations.md)) | `checkIsSubtype` as above | runtime calls |

`genInstanceOfImpl` ([IrToBitcode.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/IrToBitcode.kt))
routes ObjC object types to `genInstanceOfObjC` (class/protocol queries via the ObjC runtime), so the id scheme is only ever
applied to Kotlin classes; that is why Kotlin ObjC classes can safely carry `classId = 0`.

`getInterfaceTableRecord` has two shapes:

- **Unconditional fast path** — `gep(itable, interfaceTableSize & interfaceId)` with no branch — only when
  `ghaEnabled() && bitsPerColor <= MAX_BITS_PER_COLOR && produce != FRAMEWORK` ("All interface tables are small and no
  unknown interface inheritance"). If the whole program needs more than 5 bits of color, *some* class could need a table
  larger than 32 and fall back to the sorted form, so every lookup must be prepared for it.
- **Branchy path** otherwise: `if (interfaceTableSize >= 0) fast else LookupInterfaceTableRecord(itable, -size, id)`. The
  runtime lookup ([TypeInfo.cpp](../../kotlin-native/runtime/src/main/cpp/TypeInfo.cpp)) is linear for ≤ 8 entries, binary
  search above, and "never returns nullptr" — for an `is` check the caller compares the returned record's `id`; for a call
  the compiler relies on the interface actually being implemented. Without GHA the RTTI side still builds perfect-hash tables
  when the hashed ids happen not to collide, so the fast branch is usually taken even in debug builds.

Runtime side: `IsSubclassFast` and `IsSubtype` in [Types.cpp](../../kotlin-native/runtime/src/main/cpp/Types.cpp);
`LookupInterfaceTableRecord` in [TypeInfo.cpp](../../kotlin-native/runtime/src/main/cpp/TypeInfo.cpp); the fields
`classId_`, `interfaceTableSize_`, `interfaceTable_` in [TypeInfo.h](../../kotlin-native/runtime/src/main/cpp/TypeInfo.h)
(the comment there points back to `ClassLayoutBuilder`). `Kotlin_TypeInfo_isInstance` (reflection) always uses the slow
`IsSubtype`, so reflection is correct with any ids.

### Why frameworks keep the branch, and why caches get no ids

**Frameworks.** ObjC/Swift subclasses of exported Kotlin classes get a `TypeInfo` synthesized at run time by
`createTypeInfo` in [ObjCExport.mm](../../kotlin-native/runtime/src/main/cpp/ObjCExport.mm):
- `result->classId_ = superType->classId_` — the subclass reuses its Kotlin superclass's interval start, so
  `IsSubclassFast(K.lo, K.hi)` is true for it and false for any Kotlin subclass interval `S ⊂ K` with `S.lo > K.lo`. The
  compile-time ids therefore remain correct for runtime-created classes **without** the runtime knowing the DFS.
- Protocols adopted by the subclass are added as interfaces (`getProtocolsAsInterfaces`, `addITable(typeInfo->classId_, …)`),
  and `buildITable` re-runs the same "power of two ≤ 32 without collisions, else sorted with negative size" construction over
  the merged id set. The compile-time coloring never saw this class, so two same-colored interfaces may now meet, and the
  table may legitimately become the sorted kind. That is the "unknown interface inheritance" the `FRAMEWORK` condition
  refers to: generated code inside a framework must keep the `size >= 0` branch. The runtime algorithm and the compiler's
  `interfaceTableSkeleton` must agree on the encoding — this is part of the compiler/runtime contract
  ([compiler-runtime-contract.md](compiler-runtime-contract.md)).

**Caches.** Ids are a numbering of *one* DFS over *one* hierarchy and a coloring of *one* interface graph. A static cache is
compiled on its own and later linked into programs it has never seen; its stdlib `Any` interval, interface colors and itable
shapes would differ from the consumer's. Hence `GHAPhase` is disabled for cache outputs, and — because
`runGlobalOptimizations` is false whenever static caches are *used* — for every binary that links one. Both sides then agree
on "no ids": `classId_ = 0`, hashed interface ids, runtime `IsSubtype`, branchy itable lookup. There is no versioning of ids;
nothing checks at link time that producer and consumer made the same choice, so a hypothetical "GHA inside caches" would
need its own ABI story ([caches.md](caches.md)).

## Flags, tests, symptoms

| Flag                                              | Effect                                                                           |
|---------------------------------------------------|----------------------------------------------------------------------------------|
| `-opt` (and no static caches)                     | enables both phases                                                              |
| `-Xdisable-phases=DCEPhase`                       | keep all functions; vtables fully populated (used by `fileCheck/bridges.kt`)     |
| `-Xdisable-phases=GHAPhase`                       | `ghaEnabled()` false: runtime type checks, hashed interface ids                  |
| `-Xphases-to-dump-after=DCEPhase`                 | IR after deletion                                                                |
| `-Xbinary=enableReleaseBinaryCache=true`          | caches in `-opt` ⇒ both phases off ([optimizations.md](optimizations.md))        |

Tests: there are no dedicated DCE or GHA tests. Coverage comes from every blackbox test run with `-Pkn.optimizationMode=OPT
-Pkn.cacheMode=NO` (see [optimizations.md](optimizations.md) for commands); FileCheck tests
(`native/native.tests/testData/codegen/fileCheck/`) assert presence/absence of `define … @"kfun:…"` lines and run with the
pre-codegen inliner pinned off. Binary size is tracked only by the performance project
([CodeSizeTask.kt](../../kotlin-native/performance/buildSrc/src/main/kotlin/CodeSizeTask.kt), `CODE_SIZE` metric), not by
tests. Runtime gtests do not cover `LookupInterfaceTableRecord`/`IsSubclassFast` (no matches in `kotlin-native/runtime`).

| Symptom                                                   | Suspect                                                                                                   |
|-----------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| Segfault at address 0 inside a virtual call, release only | DCE nulled a slot: the callee is reachable through something not in the root set (native callback, reflection, runtime). Compare with `-Xdisable-phases=DCEPhase`; extend `computeRootSet` or `EXPLICITLY_EXPORTED`. |
| Linker: undefined `kfun:` referenced from RTTI            | ObjC-export RTTI (no DCE set) or a layout consumer referencing a deleted function; check `bridgeTarget`-style dependencies (KT-74377). |
| `is`/`as` wrong only in `-opt`, works with caches         | GHA: a class with an unexpected `classId_` (runtime-created TypeInfo, a synthetic interface impl) or a stale `hierarchyInfo`. Bisect with `-Xdisable-phases=GHAPhase`. |
| Interface call lands in the wrong method only in `-opt`   | Itable collision: `interfaceTableSkeleton` `require` not triggered means ids were unique; check color assignment vs a class that gained interfaces after GHA (lowering after `GHAPhase`). |
| "Unable to assign interface id"                           | More than `Int.MAX_VALUE >> bitsPerColor` interfaces of one color — practically unreachable.               |

## Contracts others depend on

- `TypeInfo.classId_`, `interfaceTableSize_` sign convention and the `id & (size-1)` placement are ABI between compiler and
  runtime, and between the compiler and `ObjCExport.mm createTypeInfo`/`buildITable`. Changing `MAX_BITS_PER_COLOR` changes
  the `32` literal in `buildITable` too.
- Swift/ObjC subclasses rely on `classId_` inheritance to pass `is`-checks against their Kotlin superclasses in `-opt`
  frameworks.
- Caches rely on GHA being off on both sides; a binary using static caches must not be compiled with ids.
- Library-mode DCE roots (every public symbol and every vtable/itable member) are what export, cinterop callbacks and
  `@ExportForCppRuntime` runtime entry points depend on; there is no per-feature rooting.
- Class layouts must be computed from functions that survive DCE (`bridgeTarget` rule); any new layout dependency between
  functions needs the same treatment.

## Open questions

- Why `invoke` of `kotlin.FunctionN` interfaces is force-kept by DCE; no comment or history beyond the driver port.
- The dead `NativePointed` exemption in `dce()` (`4ccc9b616964`): whether it was ever needed, and whether cinterop codegen
  silently depends on some `NativePointed` member surviving (today nothing enforces it).
- `TODO-DCE-OBJC-INIT`: external ObjC methods are blanket roots; whether a finer rule is intended is not documented.
- Whether DCE could run before `GHAPhase`'s coloring in a way that drops interfaces with no live implementations (today
  classes/interfaces are never removed, so dead interfaces still consume colors).
- The `FRAMEWORK`-only condition does not cover `DYNAMIC`/`STATIC` libraries whose Kotlin classes could in principle be
  subclassed via ObjC when ObjC interop is on; *Hypothesis:* only frameworks expose Kotlin classes to ObjC subclassing.
