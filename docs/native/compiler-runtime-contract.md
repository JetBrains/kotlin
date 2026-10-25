# Kotlin/Native: the compiler ↔ runtime contract

The K/N backend emits LLVM IR that is linked with the C++ runtime bitcode (see [architecture.md](architecture.md), "Runtime").
The two sides share no header: the compiler encodes the runtime's data structures and calling conventions in Kotlin code, and
the runtime assumes what generated code does in prologues, around calls and when it stores references. This page describes that
shared ABI. The runtime's internals (GC algorithms, thread registry) are in [runtime.md](runtime.md); how the compiler
emits LLVM is in [llvm-codegen.md](llvm-codegen.md).

Paths below: `RT` = `kotlin-native/runtime/src/main/cpp`, `BE` = `kotlin-native/backend.native/.../backend/konan`,
`KT` = `kotlin-native/runtime/src/main/kotlin`.

## Terminology

| Term                   | Meaning                                                                                                             |
|------------------------|---------------------------------------------------------------------------------------------------------------------|
| `ObjHeader`            | The one-word header of every Kotlin object: a tagged pointer to its `TypeInfo` or meta-object ([Memory.h])          |
| `TypeInfo`             | Per-class constant: layout, supertypes, vtable, itable, flags ([TypeInfo.h], emitted by [RTTIGenerator.kt])         |
| meta-object            | Lazily installed `ExtraObjectData` that takes the header slot and points to the `TypeInfo` ([ExtraObjectData.hpp])   |
| permanent object       | Compile-time constant object (string literal, box cache entry, singleton); header tag `OBJECT_TAG_PERMANENT`        |
| vtable / itable        | Function pointer array after `TypeInfo`; perfect-hash or sorted table of `InterfaceTableRecord`s for interfaces     |
| class id               | DFS interval number (classes) or colored interface id, computed by `GlobalHierarchyAnalysis` ([ClassLayoutBuilder.kt]) |
| shadow stack / frame   | Per-function array of object slots registered via `EnterFrame`/`LeaveFrame`; the GC's stack roots ([ShadowStack.hpp]) |
| return slot            | Extra trailing `ObjHeader**` parameter of every object-returning function (`OBJ_GETTER` in [Memory.h])              |
| thread state           | `kRunnable` (may touch Kotlin objects, must poll safepoints) vs `kNative` (may block; GC proceeds without it)         |
| safepoint              | Call the GC can use to suspend a runnable thread: function prologues and loop back-edges                            |
| `compiler_interface.bc`| Runtime bitcode module the compiler parses to learn struct layouts and function signatures ([CompilerCInterface.cpp]) |
| `@ExportForCppRuntime` | Kotlin function with a fixed C symbol so the C++ runtime can call it; the reverse direction of `@SymbolName`          |

## How the compiler learns the runtime's shape

Nothing is hardcoded as C declarations. At build time `kotlin-native/runtime/src/compiler_interface` is compiled to
`compiler_interface.bc` ([RuntimeModule.kt] `COMPILER_INTERFACE`, path via [RuntimeModulesConfig.kt]). It consists of `touch`
macros ([CompilerCInterface.cpp]): `touchType(TypeInfo)` emits a global `touchTypeInfo` of that struct type, and
`touchFunction(AllocInstance)` emits a function returning `&AllocInstance`, so both symbols survive in the bitcode.

The compiler parses this module once per generation state ([NativeGenerationState.kt] `runtimeDelegate` → class `Runtime` in
[Runtime.kt]):
- struct types by name: `getStructType("TypeInfo")` looks for `struct.TypeInfo` or the value type of global `touchTypeInfo`;
  the generated module takes its `datalayout` and `target triple` from the runtime module (`CodegenLlvmHelpers.init`);
- functions by name: `importRtFunction("AllocInstance", returnsObjectType = true)` in [ContextUtils.kt] copies the LLVM function
  type and attributes from the runtime module (`importFunction`). `returnsObjectType` tells codegen to pass a return slot;
- a few symbols are declared with an explicit signature instead (`externalNativeRuntimeFunction`: C++ personality,
  `__cxa_begin_catch`, `_ZSt9terminatev`), and typeinfo globals by name (`_ZTI18ExceptionObjHolder`).

Consequence: adding a runtime function the compiler should call requires (1) the C++ definition, (2) a `touchFunction` line,
(3) an `importRtFunction` in `ContextUtils.kt`. Forgetting (2) fails at compile time with `function X not found`.

Field offsets are *not* read from the module: `RTTIGenerator.TypeInfo` lists fields positionally, and
`VirtualTablesLookup` uses literal indices (`structGep(typeInfoType, ti, 9 /* interfaceTableSize_ */)`, `10`) in
[CodeGenerator.kt]. Reordering `struct TypeInfo` therefore silently breaks codegen.

## Object layout

Runtime side: `struct ObjHeader { TypeInfo* typeInfoOrMeta_; }` and `struct ArrayHeader { ObjHeader; uint32_t count_; }`
([Memory.h]). The low 2 bits of the header word are tags: `OBJECT_TAG_HEAP = 0`, `PERMANENT = 1`, `STACK = 3`, mask `3`.
`type_info()` clears the tag and reads `->typeInfo_`: this works for both a real `TypeInfo` (whose first field is a
self-pointer) and an `ExtraObjectData`, whose first field is a `const TypeInfo*` at the same offset
(`static_assert(offsetof(ExtraObjectData, typeInfo_) == offsetof(TypeInfo, typeInfo_))` in [ExtraObjectData.hpp]).
The meta-object is installed lazily (`ObjHeader::createMetaObject`, [mm/Memory.cpp]) for weak references, ObjC associated
objects and flags; the read is deliberately relaxed, see the long comment on `type_info()`.

Compiler side:
- The tag constants are duplicated: `permanentTag` in [KotlinStaticData.kt] (constant objects), `immTypeInfoMask` and
  `setTypeInfoForStackObject` (`| 3`) in [CodeGenerator.kt]. Generated code reads the `TypeInfo` of an object only through
  the runtime call `Kotlin_Any_getTypeInfo` (`loadTypeInfo`), which is why the tag logic is not inlined in codegen
  (commit `e84a22108246 Uninlined type info loading`).
- Fields: `ClassLayoutBuilder.getFields` ([ClassLayoutBuilder.kt]) = superclass fields as a prefix, then declared fields
  sorted by size descending (unless `@NoReorderFields`). With `-Xbinary=packFields` the order comes from `packFields` in
  [LlvmDeclarations.kt]. `createClassBody` builds the LLVM struct `kclassbody:<fqname>#internal` with `ObjHeader` as
  element 0 and explicit padding; `alignment = max(8, max field alignment)`, `@Volatile` fields need
  `max(size, abiAlignment)` (`requiredAlignment`). For classes from cached libraries the field list is deserialized
  (`ClassFieldsDeserializer`), so a layout rule change invalidates caches (see [caches.md](caches.md)).
- Size: `instanceSize_ = LLVMStoreSizeOfType(body)`; for arrays and `String` it is `-ABISizeOf(element)`
  (`RTTIGenerator.getInstanceSize`, `arrayClasses` map). The runtime computes array size as
  `AlignUp(AlignUp(sizeof(ArrayHeader), elemSize) + elemSize*count, 8)` (`KArray::descriptor::size` in [KObject.hpp]) and
  object size as `instanceSize_`. `kObjectAlignment = 8` ([Alignment.hpp]) is mirrored by `Runtime.objectAlignment = 8`.
- `objOffsets_` are the byte offsets of reference fields (sorted); the GC traverses them through `processObjectInMark`,
  which the compiler sets to `Kotlin_processObjectInMark`, `Kotlin_processEmptyObjectInMark` (no refs) or
  `Kotlin_processArrayInMark` (`kotlin.Array`). `objOffsetsCount_ = 1` for `kotlin.Array` "to mark it as non-leaf".
- `String` has its own header (`StringHeader` in [KString.h]: `hashCode_`, `flags_`); the compiler reads
  `stringHeaderExtraSize` from the runtime module rather than assuming it.

## TypeInfo: fields, vtable, itable

`struct TypeInfo` ([TypeInfo.h]) and `RTTIGenerator.TypeInfo` ([RTTIGenerator.kt]) must list the same fields in the same
order. The header comment names three more copies to update: `ObjectTestSupport TypeInfoHolder` ([ObjectTestSupport.hpp],
runtime gtests) and `createTypeInfo` in [ObjCExport.mm], which builds `TypeInfo`s at run time for ObjC subclasses.

| Field                                    | Written by the compiler                                                   | Read by the runtime                          |
|------------------------------------------|---------------------------------------------------------------------------|----------------------------------------------|
| `typeInfo_`                              | self pointer                                                              | `ObjHeader::type_info`, `AsMetaObject`       |
| `extendedInfo_`                          | field names/offsets/types, only in non-optimized builds (`makeExtendedInfo`) | debugger (`debug.bc`), `IsLayoutCompatible` |
| `instanceSize_`, `instanceAlignment_`    | see above                                                                 | allocator ([KObject.hpp], [CustomAllocator.cpp]) |
| `superType_`                             | `Any` for arrays and Kotlin ObjC classes; null for `Any`                  | `IsSubtype` walks the chain ([Types.cpp])    |
| `objOffsets_`, `objOffsetsCount_`        | reference field offsets                                                   | marking, `IsLayoutCompatible`                |
| `implementedInterfaces_`/`Count_`        | all interfaces with RTTI                                                  | `IsSubtype` for interfaces (linear scan)     |
| `interfaceTableSize_`, `interfaceTable_` | itable (below)                                                            | `LookupInterfaceTableRecord`, ObjC export    |
| `packageName_`, `relativeName_`, `flags_`| strings as permanent `String` objects; `TF_*` flags                       | reflection (`Kotlin_TypeInfo_get*Name`), finalizers, ObjC |
| `classId_`                               | DFS interval start (classes) or colored id (interfaces); 0 for ObjC classes | `IsSubclassFast(lo, hi)`                    |
| `writableInfo_`                          | per-class mutable data for ObjC export (`KONAN_TYPE_INFO_HAS_WRITABLE_PART`) | [WritableTypeInfo.hpp], ObjC export       |
| `associatedObjects`                      | null-terminated `AssociatedObjectTableRecord[]` from `@AssociatedObjectKey` | `Kotlin_TypeInfo_findAssociatedObject`     |
| `processObjectInMark`                    | one of three runtime functions                                            | GC mark                                      |
| vtable                                   | `ConstArray` appended right after the struct (`llvm.struct(typeInfo, vtable)`) | `TypeInfo::vtable()` = `this + 1`       |

Flags: `Konan_TypeFlags` in `TypeInfo.h` vs the `TF_*` constants at the bottom of `RTTIGenerator.kt`. They have drifted:
the compiler defines `TF_LEAK_DETECTOR_CANDIDATE = 16` and `TF_HAS_FREEZE_HOOK = 128`, both unused, while the runtime uses
bit 16 for `TF_KOTLIN_OBJC_CLASS` (set only by `createTypeInfo` in `ObjCExport.mm`). `Konan_RuntimeType` (`RT_*`) is
mirrored by `primitiveRuntimeTypeMap`.

Virtual dispatch (`VirtualTablesLookup.getVirtualImpl`, [CodeGenerator.kt]): class methods load `vtable[index]` with the
index from `ClassLayoutBuilder.vtableEntries` (inherited slots first, new slots sorted by mangled name). Interface methods
use the itable, whose design is documented in the comment in `GlobalHierarchyAnalysis.run` ([ClassLayoutBuilder.kt]):
interfaces are colored so that no class implements two interfaces of the same color; the color is kept in the low bits of
the interface id; each class gets a power-of-two table where `itable[id & (size-1)].id == id`. `interfaceTableSize_` stores
`size - 1` for this fast path, or `-count` when a sorted table is used and the runtime's `LookupInterfaceTableRecord`
does a binary search ([TypeInfo.h]). When global hierarchy analysis is off (`ghaEnabled()` false, e.g. with caches), class
ids are 0 or a hash, and type checks fall back to the runtime `IsSubtype`. Interface tables are omitted for interfaces,
abstract classes and ObjC classes (`needInterfaceTable`), and `FRAMEWORK` output always keeps the slow-path branch.

`IsLayoutCompatible` in [TypeInfo.h] is what makes `@ExportTypeInfo`-exported types (`theByteArrayTypeInfo`, … in
[Arrays.kt]) usable from C++: the compiler emits an exported global holding the `TypeInfo*` (`exportTypeInfoIfRequired`).

## Boxing

Inline classes are boxed through synthesized `<Name-box>`/`<Name-unbox>` functions ([Boxing.kt], `getBoxFunction`).
Primitive boxes are cached: the compiler emits permanent `{ObjHeader, value}` arrays `INT_CACHE`, … and the constants
`INT_RANGE_FROM`/`_TO` for each `BoxCache` entry ([BoxCache.kt] gives the ranges; `initializeCachedBoxes` in
`Boxing.kt`; only the module containing stdlib defines them, others declare). The stdlib ([KT Boxing.kt]) calls
`inIntBoxCache`/`getCachedIntBox` via `@GCUnsafeCall`, implemented in [RT Boxing.cpp] over those `extern` globals
("Keep naming of these in sync with codegen part"). Codegen also folds `IrConstantPrimitive` to a cache element directly
(`toBoxCacheValue`), so changing a range changes which objects are identical (`===`).

## Runtime entry points and Kotlin-side annotations

Direction runtime → Kotlin: `Exceptions.h` declares `ThrowNullPointerException`, `StaticInitializationFailure`, … as
"implemented in Kotlin"; they are `@ExportForCppRuntime` functions in [RuntimeUtils.kt]. The runtime also calls
`Kotlin_runUnhandledExceptionHook`, `Konan_start` ([EntryPoint.kt]) and ~110 more such functions.

| Annotation ([KT internal Annotations.kt], [KT native Annotations.kt])  | Consumer and effect                                                                         |
|----------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `@ExportForCppRuntime(name)`           | `BinaryInterface.funSymbolNameImpl`: fixed symbol name. Also a DCE root (`EXPLICITLY_EXPORTED` in [DataFlowIR.kt]), gets a cleanup landing pad + `LeaveFrame` ([CodeGenerator.kt] `needCleanupLandingpadAndLeaveFrame`), and the file is kept out of per-file cache splitting (`isReferencedByNativeRuntime`, [TopLevelPhases.kt]) |
| `@ExportTypeInfo(name)`                | `RTTIGenerator.exportTypeInfoIfRequired` emits a global `name` = `TypeInfo*`; same `isReferencedByNativeRuntime` rule |
| `@SymbolName(name)` on `external fun`  | `externalSymbolOrThrow` ([IrUtils.kt]): call the C symbol directly, **no** thread-state switch, exceptions not filtered |
| `@GCUnsafeCall(callee)`                | Same resolution as `@SymbolName` (runtime-implemented intrinsic); the callee is responsible for GC safety     |
| `@ForceNativeThreadState`, `-Xbinary=forceNativeThreadStateForFunctions` | Switch to Native around the direct call and force a filtering exception handler ([IrToBitcode.kt] `call`), for blocking `@SymbolName` functions |
| `@FilterExceptions(mode)`              | Install `filteringExceptionHandler`: foreign C++/ObjC exceptions terminate (default) or are wrapped (`objc_wrap`) |
| `@TypedIntrinsic(kind)`                | [IntrinsicGenerator.kt] emits LLVM instead of a call; kinds in [Intrinsics.kt]                               |
| `@ExportForCompiler`                   | Marks declarations the compiler looks up by name (e.g. `KClassImpl`, `createCleaner`); no backend consumer of the annotation itself was found, see Open questions |
| `@Escapes(mask)`, `@Escapes.Nothing`, `@PointsTo(...)` | Escape analysis summaries for `external fun` with object parameters ([EscapeAnalysis.kt] `fromAnnotations`); the first stage checks every stdlib `external fun` has one ([EscapeAnalysisChecker.kt]) |
| `@HasFinalizer`                        | `TF_HAS_FINALIZER` ([IrUtils.kt] `hasFinalizer`); runtime queues such objects for finalization ([FinalizerHooks.cpp]) |
| `@NoReorderFields`                     | Keep declaration order of fields (used for classes the runtime reads field-by-field)                        |
| `@ExportedBridge`, `@ImportedBridge`   | Swift export bridges: C→Kotlin bridges get runtime init + `Runnable` switch; Kotlin→C bridges switch to Native |

## Thread states and safepoints

Runtime: `ThreadState { kRunnable, kNative }` ([Memory.h]); `SwitchThreadState` asserts the state actually changes unless
`reentrant` ([ThreadState.hpp]); a Runnable→Native→Runnable transition is where the GC may park the thread
(`ThreadSuspensionData::suspendIfRequested`, [ThreadSuspension.hpp]). Entry points that touch Kotlin memory assert
`kRunnable` (`EnterFrame`, `RegisterGlobal`, … in [mm/Memory.cpp]). Blocking in Runnable blocks the GC; running Kotlin
code in Native races with the GC ([externalCallsChecker README]).

What codegen emits (`FunctionGenerationContext.epilogue()` in [CodeGenerator.kt] builds the real prologue):

```
prologue:   slots = alloca ptr[slotCount]; memset(slots, 0)              ; only if the frame has object slots
            Kotlin_initRuntimeIfNeeded(); switchThreadStateRunnable()     ; only C→Kotlin bridges (switchToRunnable)
            EnterFrame(slots, skipSlots /*params*/, slotCount)
            Kotlin_mm_safePointFunctionPrologue()                          ; unless needSafePoint=false / forbidRuntime
body:       ... while/do-while bodies start with Kotlin_mm_safePointWhileLoopBody() (IrToBitcode.kt)
return:     UpdateReturnRef(returnSlot, value); LeaveFrame(...); [switchThreadStateNative() for bridges]
```

Around external calls ([IrToBitcode.kt] `call(function, llvmCallable, ...)`): only `KOTLIN_TO_C_BRIDGE` functions (cinterop
stubs), `@ImportedBridge` and `@ForceNativeThreadState` get `switchThreadState(Native)` before and `Runnable` after; the
exception landing pad for such a call also switches back (`gxxLandingpad(switchThreadState = true)`), which is why a
state switch without a filtering handler is rejected with a `check`. In non-optimized builds the `_debug` no-inline
variants are used (KT-67567, [ContextUtils.kt]). Functions generated with `generateFunctionNoRuntime` (global ctors) may
not touch the runtime at all (`forbidRuntime`). The `RemoveRedundantSafepoints` LLVM pass ([RemoveRedundantSafepoints.cpp])
later drops prologue safepoints made redundant by inlining. `-Xbinary=checkStateAtExternalCalls=true` links
`impl_externalCallsChecker.bc` ([CompilerOutput.kt]) and the `CallsChecker` pass in libllvmext instruments every call to a
function not defined in the module; see the README for the algorithm and the `GoodFunctionNames` allowlist.

Rules: a runtime function called from generated code runs in Runnable and must switch itself (`ThreadStateGuard`,
`CallWithThreadState`) before blocking; a `@SymbolName` callee is in Runnable too; C++ code called from arbitrary threads
uses `CalledFromNativeGuard` to attach and become Runnable before touching objects ([Memory.h]).

## GC roots from generated code

- **Shadow stack.** Each frame's object slots form `FrameOverlay {previous, parameters, count}` followed by slots
  ([Memory.h]); `EnterFrame` links it into `ThreadData::shadowStack()`, `LeaveFrame` unlinks it ([ShadowStack.hpp]).
  Slot allocation is `FunctionGenerationContext.alloca(isObjectType = true)` ([CodeGenerator.kt]) and
  [VariableManager.kt] gives every reference-typed local a slot (`createVariable`: "we always create slot for object
  references"); SSA values between stores are not roots, so a reference must be in a slot across any call that can
  trigger GC. Object parameters stored on the stack are counted in `skipSlots` and skipped by the iterator (`begin()`
  adds `parameters`). Landing pads call `SetCurrentFrame(slots)` to drop frames skipped by unwinding; functions callable
  from C++ also get a cleanup landing pad that calls `LeaveFrame`. `CheckCurrentFrame` verifies this in debug.
- **Return slot.** Object-returning functions receive `ObjHeader** result` last (`OBJ_GETTER`, `RETURN_OBJ` →
  `UpdateReturnRef`); callers pass a slot chosen by the escape-analysis `Lifetime`/`SlotType` ([ContextUtils.kt]).
- **Globals.** Reference-typed static fields are registered once by the eager file initializer through `RegisterGlobal`
  ([IrToBitcode.kt] `initializedGlobals` → `GlobalsRegistry` [GlobalsRegistry.hpp]). Writes use `UpdateHeapRef`
  (`storeHeapRef`) and `UpdateVolatileHeapRef`/`CompareAndSwapVolatileHeapRef` for `@Volatile`.
- **Thread-locals.** `AddTLSRecord(memory, __KonanTlsKey, count)` during `ALLOC_THREAD_LOCAL_GLOBALS`; access via
  `LookupTLS(key, index)`; roots come from `ThreadLocalStorage` ([RootSet.hpp]).
- **Stack-allocated objects** (escape analysis, `Lifetime.STACK`): `StackLocalsManagerImpl.alloc` allocas the body,
  tags the header `OBJECT_TAG_STACK`, and stores the pointer into an ordinary root slot. The runtime asserts that heap
  objects never reference stack objects ([MarkAndSweepUtils.hpp]) and that no `ExternalRCRef` points to one
  ([ExternalRCRef.cpp]); `ShadowStack.hpp` still carries a TODO about stack objects.
- **Exceptions in flight** are roots through `ExceptionObjHolderImpl`'s `OwningExternalRCRef` ([ExceptionObjHolder.cpp]).

## Allocation, initialization, exceptions, lifecycle

- **Allocation:** `AllocInstance(typeInfo, result)` / `AllocArrayInstance(typeInfo, n, result)` ([mm/Memory.cpp]);
  codegen passes `typeInfoForAllocation` and the lifetime-chosen slot (`allocInstance`, [CodeGenerator.kt]).
- **File initializers:** for each scope the compiler emits an `Initializer` of type `void(int32, MemoryState*)`
  (`kInitFuncType` ↔ `typedef Initializer` in [Runtime.h]), wraps it in a static `InitNode` and a global ctor that calls
  `AppendToInitializersTail` (built with `forbidRuntime`). `initRuntime` ([Runtime.cpp]) walks the list with
  `ALLOC_THREAD_LOCAL_GLOBALS = 0`, `INIT_GLOBALS = 1` (first runtime only), `INIT_THREAD_LOCAL_GLOBALS = 2`; both files
  say "Must be synchronized". Lazy (per-file) initialization inlines a check of the state word against
  `FILE_INITIALIZED = 2` and calls `CallInitGlobalPossiblyLock(state, init, typeInfo)` or `CallInitThreadLocal`
  ([IrToBitcode.kt] `evaluateFileGlobalInitializerCall`); the runtime encodes `FILE_BEING_INITIALIZED | threadId << 2`
  and reports failures through the Kotlin `StaticInitializationFailure`.
- **Exceptions:** `ThrowException(obj)` → `ExceptionObjHolder::Throw` → C++ `throw` ([Exceptions.cpp]). Generated code
  uses the Itanium personality (`__gxx_personality_seh0` on `MINGW_X64`), a `landingpad` clause on
  `_ZTI18ExceptionObjHolder`, then `__cxa_begin_catch` / `Kotlin_getExceptionObject(holder, slot)` / `__cxa_end_catch`
  ([CodeGenerator.kt] `kotlinExceptionRtti`, `catchKotlinException`). Kotlin `catch` clauses are `genInstanceOfImpl`
  checks on the extracted object ([IrToBitcode.kt] `CatchingScope`). Foreign exceptions reaching a
  `filteringExceptionHandler` either terminate or, on Apple targets with `OBJC_WRAP`, become a Kotlin
  `ForeignException` via `createForeignException`. Unhandled Kotlin exceptions reach the runtime terminate handler
  installed by `SetKonanTerminateHandler`, which calls the Kotlin hook (`Kotlin_runUnhandledExceptionHook`).
- **Finalizers / Cleaner:** `TF_HAS_FINALIZER` from `@HasFinalizer` tells the allocator to queue the object
  ([FinalizerHooks.cpp], [CustomAllocator.cpp]); `createCleaner` in [Cleaner.kt] is `@ExportForCompiler`. Details in
  [runtime.md](runtime.md).
- **ExternalRCRef:** `Kotlin_mm_createRetainedExternalRCRef` / `release` / `dispose` are imported by codegen for ObjC
  blocks ([ExternalRCRef.hpp]); see [export.md](export.md) and [runtime.md](runtime.md).

## Binary options → runtime constants

Two mechanisms, documented in [CompilerConstants.hpp] / [CompilerConstants.cpp] and mirrored by
[NativeRuntimeConstants.kt] / [NativeRuntimeOverridableConstants.kt]:

| Mechanism                           | Runtime side                                             | Compiler side                                                 | Trade-off                                          |
|-------------------------------------|----------------------------------------------------------|---------------------------------------------------------------|----------------------------------------------------|
| `extern const int32_t Kotlin_*`     | declared only (`CompilerConstants.hpp`)                  | defined as constant globals in a separate `constants` module (`generateRuntimeConstantsModule`, [IrToBitcode.kt]; linked in [CompilerOutput.kt]) | LLVM can fold them, but they are baked into caches: `appendCommonCacheFlavor` in [NativeSecondStageCompilationConfig.kt] must key the cache on them (e.g. `-asserts`, `-check_state_at_external_calls`) |
| `RUNTIME_WEAK int32_t Kotlin_*`     | weak definition with default (`CompilerConstants.cpp`)   | `overrideRuntimeGlobals` replaces the weak global (`replaceExternalWeakOrCommonGlobalFromNativeRuntime`, [LlvmUtils.kt]) | overridable after caches are built; not foldable   |

Examples of the first kind: `Kotlin_runtimeAssertsMode` (`-Xbinary=runtimeAssertionsMode=ignore|log|panic`,
[RuntimeAssertsMode.kt]), `Kotlin_needDebugInfo`, `Kotlin_pagedAllocator`. Of the second: `Kotlin_gcMutatorsCooperate`,
`Kotlin_auxGCThreads`, `Kotlin_runtimeLogs` (array indexed by `logging::Tag`). Enums are duplicated with "Must match"
comments: `RuntimeAssertsMode`, `AppStateTracking`, and `LoggingLevel`/`LoggingTag` in [RuntimeLogging.kt] vs
[Logging.hpp]. The runtime gtests define their own values in [CompilerGenerated.cpp] (asserts = panic), so a new constant
must be added there too. The only environment override found in the runtime is `KOTLIN_NATIVE_AVAILABLE_PROCESSORS`
([Runtime.cpp]); runtime options are otherwise compile-time.

## Changes come in pairs

Commits that touched both `TypeInfo.h` and `RTTIGenerator.kt` (from `git log`):
- `a11f6fd9cbbb [K/N] Support of aligned fields`: added `instanceAlignment_` to `TypeInfo.h`, `Alignment.hpp`,
  `ObjCExport.mm`, `ObjectTestSupport.hpp` and, on the compiler side, `ClassLayoutBuilder`, `LlvmDeclarations`,
  `RTTIGenerator`, `Runtime.kt`, the IR linker (field alignment in klibs).
- `98251f42f240 [K/N] Specialize marking call for objects`: added `processObjectInMark` and three `touchFunction`s;
  changed every GC implementation plus `RTTIGenerator` and `ContextUtils`.
- `e84a22108246 Uninlined type info loading`: a `Types.h` declaration, a `touchFunction`, one `importRtFunction`, and
  `CodeGenerator.loadTypeInfo` became a call.
- `44010a04b13b Extracted compiler interface from native runtime` created `compiler_interface` itself.

## Invariants and ordering constraints

- Struct field order of `TypeInfo`, `ExtendedTypeInfo`, `InterfaceTableRecord`, `AssociatedObjectTableRecord`,
  `ObjHeader`, `ArrayHeader`, `FrameOverlay`, `InitNode` is positional on the compiler side; `RTTIGenerator` indices and
  `structGep` literals must follow any change. Enum values (`TF_*`, `RT_*`, init phases, `FILE_*`, assert modes, log
  tags, `OBJECT_TAG_*`) are duplicated verbatim.
- Every object-returning runtime function takes the return slot last and must `RETURN_OBJ`; `importRtFunction(name, true)`
  must agree with the C++ signature or the call passes the wrong argument count.
- Generated code may call the runtime only in Runnable; `EnterFrame` precedes the first safepoint and any allocation;
  `LeaveFrame` (or `SetCurrentFrame` in a landing pad) must restore the shadow stack before the frame dies.
- A reference that lives across a call that may GC must be in a slot, a global registered with `RegisterGlobal`, TLS, or
  an `ExternalRCRef`. Heap objects must not point to stack objects (escape analysis guarantees it; debug runtime asserts).
- Thread-local initializers run only after all shared globals of the file are initialized (the inline check in
  `evaluateFileThreadLocalInitializerCall`); `INIT_GLOBALS` runs once per process, the TLS phases once per runtime.
- The data layout and target triple of the generated module come from the runtime module, so the runtime must be built
  for exactly the target being compiled (`dist/konan/targets/<target>/native/`).

## Making a typical change

| Change                                     | Touch                                                                                                              |
|--------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| New runtime function called by codegen     | C++ definition; `touchFunction` in [CompilerCInterface.cpp]; `importRtFunction` in [ContextUtils.kt]; if it returns an object, `OBJ_GETTER` + `returnsObjectType = true` |
| New Kotlin function called by the runtime  | `@ExportForCppRuntime` (+ `@UsedFromCompilerGeneratedCode`/`@PublishedApi` if the compiler also references it); `extern "C"` declaration in C++; a stub in [CompilerGenerated.cpp] if gtests link it |
| New `TypeInfo` field or flag               | [TypeInfo.h], [RTTIGenerator.kt] (both the `TypeInfo` class and `generateSyntheticInterfaceImpl`), [ObjectTestSupport.hpp], [ObjCExport.mm] `createTypeInfo`; check `structGep` literals in [CodeGenerator.kt] |
| New compile-time runtime constant          | `CompilerConstants.hpp/.cpp`, `NativeRuntimeConstants.kt` or the overridable variant, `IrToBitcode.kt` emission, [CompilerGenerated.cpp], cache flavor in [NativeSecondStageCompilationConfig.kt] if it is of the constant kind |
| New runtime module (GC, allocator, …)      | [RuntimeModule.kt], `kotlin-native/runtime/build.gradle.kts`, selection in [CompilerOutput.kt]; cache flavor if it affects linked code |
| Object layout rule (field order, alignment)| [ClassLayoutBuilder.kt], [LlvmDeclarations.kt]; `ClassFieldsDeserializer` for cached classes; runtime size formulas in [KObject.hpp]; test data under `native/native.tests/testData/codegen` |

## Investigating bugs

| Symptom                                                        | Where to look                                                                                                      |
|----------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `runtime assert: ...` / `Unexpected thread state` / `Illegal thread state switch` | `RuntimeAssert` fires only with `-Xbinary=runtimeAssertionsMode=panic` (or `log`); the test infra adds `panic` whenever assertions are enabled for the test's optimization mode ([TestCompilation.kt]). Thread-state failures point at a missing `switchThreadState` pair in codegen or a runtime function called in the wrong state ([ThreadState.hpp]) |
| Deadlock with GC waiting                                       | A blocking call in Runnable: run with `-Xbinary=checkStateAtExternalCalls=true` (`-Pkn.useThreadStateChecker=true` in tests; debug mode, no caches) and read the external calls checker report |
| Segfault reading `TypeInfo`, wrong vtable target, `IsSubtype` miss | layout drift between `TypeInfo.h` and `RTTIGenerator.kt`; dump LLVM IR with `-Xsave-llvm-ir-after=Codegen` and inspect `ktype:`/`kifacetable:` globals; `TypeInfoTest.cpp`, `codegen/interfaceCallsNCasts` |
| Object corrupted after GC, use-after-free                      | a reference not in a slot: compare `EnterFrame` slot count with stores in the IR dump; `ShadowStackTest.cpp`, `RootSetTest.cpp`; `-Pkn.gcScheduler=AGGRESSIVE` (GC at every safepoint) and `-Pkn.gcType=` to vary GCs; `testData/gc`, `codegen/escapeAnalysis` |
| Runtime logging                                                | `-Xruntime-logs=gc=info,mm=debug` (tag=level pairs; see [K2NativeCompilerArguments.kt], tags in [RuntimeLogging.kt]); example [runtimeLogging/logging.kt] |
| Static initialization order / `StaticInitializationFailure`    | `evaluateFile*InitializerCall` in [IrToBitcode.kt], `CallInitGlobalPossiblyLock` in [Runtime.cpp]; `testData/codegen/initializers` |
| Debugging the runtime itself                                   | build it with `kotlin.native.isNativeRuntimeDebugInfoEnabled=true` and compile with `-Xbinary=stripDebugInfoFromNativeLibs=false` ([HACKING.md](../../kotlin-native/HACKING.md)) |

Tests: runtime gtests `./gradlew :kotlin-native:runtime:hostRuntimeTests -Pgtest_filter='ThreadState*:ShadowStack*:TypeInfo*'`
(they link [CompilerGenerated.cpp] instead of real generated code); compiler tests
`./gradlew :native:native.tests:gcTest`, `:native:native.tests:standaloneTest` (`testData/standalone`, including
`runtimeLogging` and `checkers`), and `:native:native.tests:test --tests "...FirNativeCodegenLocalTestGenerated*"` for
`testData/codegen/{boxing,escapeAnalysis,exceptions,initializers,interfaceCallsNCasts,intrinsics}`. Leak detection: no
leak-checker binary option exists; `GC.detectCycles` is a stub returning `null` ([GC.kt]).

## Open questions

- Who consumes `@ExportForCompiler`? No backend code reads `RuntimeNames`/`KonanFqNames` for it; it may only document
  "looked up by name" declarations. Unverified.
- `TF_LEAK_DETECTOR_CANDIDATE` and `TF_HAS_FREEZE_HOOK` in `RTTIGenerator.kt` have no runtime counterpart; are they
  dead, and is reusing bit 16 for `TF_KOTLIN_OBJC_CLASS` intentional?
- The `ShadowStack.hpp` TODO says stack-allocated objects are "currently incompatible"; the codegen stores them into
  root slots, so the GC sees them as ordinary roots. Hypothesis: the TODO is stale; not verified against each GC.
- `ExtendedTypeInfo` is emitted only when not optimizing (`makeExtendedInfo` has a "shall we actually do that?" TODO);
  the exact consumers besides `debug.bc` were not traced.
- `Kotlin_ObjCExport_*` and the ObjC part of the interface ([CompilerObjCInterface.mm]) are not covered here; see
  [export.md](export.md).
- Whether `SlotType.ARENA`/`PARAM_IF_ARENA` lifetimes still produce distinct code (comments mention a removed arena) was
  not verified.

[Memory.h]: ../../kotlin-native/runtime/src/main/cpp/Memory.h
[TypeInfo.h]: ../../kotlin-native/runtime/src/main/cpp/TypeInfo.h
[KObject.hpp]: ../../kotlin-native/runtime/src/main/cpp/KObject.hpp
[Alignment.hpp]: ../../kotlin-native/runtime/src/main/cpp/Alignment.hpp
[KString.h]: ../../kotlin-native/runtime/src/main/cpp/KString.h
[ExtraObjectData.hpp]: ../../kotlin-native/runtime/src/main/cpp/mm/ExtraObjectData.hpp
[mm/Memory.cpp]: ../../kotlin-native/runtime/src/main/cpp/mm/Memory.cpp
[ThreadState.hpp]: ../../kotlin-native/runtime/src/main/cpp/mm/ThreadState.hpp
[ThreadSuspension.hpp]: ../../kotlin-native/runtime/src/main/cpp/mm/ThreadSuspension.hpp
[ShadowStack.hpp]: ../../kotlin-native/runtime/src/main/cpp/mm/ShadowStack.hpp
[GlobalsRegistry.hpp]: ../../kotlin-native/runtime/src/main/cpp/mm/GlobalsRegistry.hpp
[RootSet.hpp]: ../../kotlin-native/runtime/src/main/cpp/mm/RootSet.hpp
[ExceptionObjHolder.cpp]: ../../kotlin-native/runtime/src/main/cpp/mm/ExceptionObjHolder.cpp
[ExternalRCRef.cpp]: ../../kotlin-native/runtime/src/main/cpp/mm/ExternalRCRef.cpp
[ExternalRCRef.hpp]: ../../kotlin-native/runtime/src/main/cpp/ExternalRCRef.hpp
[MarkAndSweepUtils.hpp]: ../../kotlin-native/runtime/src/main/cpp/gc/MarkAndSweepUtils.hpp
[Types.cpp]: ../../kotlin-native/runtime/src/main/cpp/Types.cpp
[Exceptions.cpp]: ../../kotlin-native/runtime/src/main/cpp/Exceptions.cpp
[Runtime.h]: ../../kotlin-native/runtime/src/main/cpp/Runtime.h
[Runtime.cpp]: ../../kotlin-native/runtime/src/main/cpp/Runtime.cpp
[FinalizerHooks.cpp]: ../../kotlin-native/runtime/src/main/cpp/FinalizerHooks.cpp
[CustomAllocator.cpp]: ../../kotlin-native/runtime/src/alloc/custom/cpp/CustomAllocator.cpp
[ObjCExport.mm]: ../../kotlin-native/runtime/src/main/cpp/ObjCExport.mm
[WritableTypeInfo.hpp]: ../../kotlin-native/runtime/src/main/cpp/WritableTypeInfo.hpp
[ObjectTestSupport.hpp]: ../../kotlin-native/runtime/src/main/cpp/ObjectTestSupport.hpp
[CompilerConstants.hpp]: ../../kotlin-native/runtime/src/main/cpp/CompilerConstants.hpp
[CompilerConstants.cpp]: ../../kotlin-native/runtime/src/main/cpp/CompilerConstants.cpp
[Logging.hpp]: ../../kotlin-native/runtime/src/main/cpp/Logging.hpp
[RT Boxing.cpp]: ../../kotlin-native/runtime/src/main/cpp/Boxing.cpp
[CompilerCInterface.cpp]: ../../kotlin-native/runtime/src/compiler_interface/cpp/CompilerCInterface.cpp
[CompilerObjCInterface.mm]: ../../kotlin-native/runtime/src/compiler_interface/cpp/CompilerObjCInterface.mm
[CompilerGenerated.cpp]: ../../kotlin-native/runtime/src/test_support/cpp/CompilerGenerated.cpp
[externalCallsChecker README]: ../../kotlin-native/runtime/src/externalCallsChecker/README.md
[RemoveRedundantSafepoints.cpp]: ../../kotlin-native/libllvmext/src/main/cpp/Passes/RemoveRedundantSafepoints.cpp
[KT internal Annotations.kt]: ../../kotlin-native/runtime/src/main/kotlin/kotlin/native/internal/Annotations.kt
[KT native Annotations.kt]: ../../kotlin-native/runtime/src/main/kotlin/kotlin/native/Annotations.kt
[KT Boxing.kt]: ../../kotlin-native/runtime/src/main/kotlin/kotlin/native/internal/Boxing.kt
[RuntimeUtils.kt]: ../../kotlin-native/runtime/src/main/kotlin/kotlin/native/internal/RuntimeUtils.kt
[Arrays.kt]: ../../kotlin-native/runtime/src/main/kotlin/kotlin/Arrays.kt
[Cleaner.kt]: ../../kotlin-native/runtime/src/main/kotlin/kotlin/native/ref/Cleaner.kt
[GC.kt]: ../../kotlin-native/runtime/src/main/kotlin/kotlin/native/internal/GC.kt
[RTTIGenerator.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/RTTIGenerator.kt
[Runtime.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/Runtime.kt
[ContextUtils.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/ContextUtils.kt
[CodeGenerator.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/CodeGenerator.kt
[IrToBitcode.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/IrToBitcode.kt
[VariableManager.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/VariableManager.kt
[KotlinStaticData.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/KotlinStaticData.kt
[LlvmDeclarations.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/LlvmDeclarations.kt
[LlvmUtils.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/LlvmUtils.kt
[BinaryInterface.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/BinaryInterface.kt
[IntrinsicGenerator.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/IntrinsicGenerator.kt
[NativeRuntimeConstants.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/NativeRuntimeConstants.kt
[NativeRuntimeOverridableConstants.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/NativeRuntimeOverridableConstants.kt
[RuntimeModule.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/runtime/RuntimeModule.kt
[RuntimeModulesConfig.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/runtime/RuntimeModulesConfig.kt
[ClassLayoutBuilder.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/ir/ClassLayoutBuilder.kt
[IrUtils.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/ir/IrUtils.kt
[Boxing.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/Boxing.kt
[EntryPoint.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/EntryPoint.kt
[RuntimeLogging.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/RuntimeLogging.kt
[CompilerOutput.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/CompilerOutput.kt
[NativeGenerationState.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/NativeGenerationState.kt
[NativeSecondStageCompilationConfig.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/NativeSecondStageCompilationConfig.kt
[TopLevelPhases.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/TopLevelPhases.kt
[DataFlowIR.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DataFlowIR.kt
[EscapeAnalysis.kt]: ../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/EscapeAnalysis.kt
[BoxCache.kt]: ../../compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/BoxCache.kt
[Intrinsics.kt]: ../../compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/ir/Intrinsics.kt
[EscapeAnalysisChecker.kt]: ../../compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/checkers/EscapeAnalysisChecker.kt
[RuntimeAssertsMode.kt]: ../../native/binary-options/src/main/kotlin/org/jetbrains/kotlin/config/nativeBinaryOptions/RuntimeAssertsMode.kt
[K2NativeCompilerArguments.kt]: ../../compiler/cli/cli-base/gen/org/jetbrains/kotlin/cli/common/arguments/K2NativeCompilerArguments.kt
[TestCompilation.kt]: ../../native/native.tests/testFixtures/org/jetbrains/kotlin/konan/test/blackbox/support/compilation/TestCompilation.kt
[runtimeLogging/logging.kt]: ../../native/native.tests/testData/standalone/runtimeLogging/logging.kt
