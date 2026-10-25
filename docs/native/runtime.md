# Kotlin/Native runtime: internal architecture

The C++ runtime in `kotlin-native/runtime/src` is what a compiled Kotlin/Native binary runs on: thread registration, the object
heap, garbage collection, finalization, and the glue that lets ObjC/C/Swift hold Kotlin objects. It is compiled to LLVM bitcode
per target and linked into every binary by the second stage (see [architecture.md](architecture.md#runtime)); the module list
and build commands are in the [runtime README](../../kotlin-native/runtime/README.md). This page explains how the pieces work
together and what the code assumes. The compiler side of the contract (object layout, `TypeInfo`, which entry points codegen
emits, safepoint insertion) is in [compiler-runtime-contract.md](compiler-runtime-contract.md) and is not repeated here.

## Key concepts

| Term | Meaning | Where |
|------|---------|-------|
| Mutator | A thread registered with the runtime that runs Kotlin code. The GC thread, finalizer thread and timer thread are not mutators. | [ThreadRegistry.hpp](../../kotlin-native/runtime/src/main/cpp/mm/ThreadRegistry.hpp) |
| `ThreadData` | Per-mutator state: shadow stack, TLS, thread-local queues, allocator/GC/scheduler thread parts, suspension state. Pinned, lives in the registry's list. | [ThreadData.hpp](../../kotlin-native/runtime/src/main/cpp/mm/ThreadData.hpp) |
| `GlobalData` | The single process-wide root object: thread registry, globals registry, `ExternalRCRefRegistry`, scheduler, allocator, GC. Created once, never destroyed. | [GlobalData.hpp](../../kotlin-native/runtime/src/main/cpp/mm/GlobalData.hpp) |
| Thread state | `kRunnable` (may touch the heap; must reach safepoints) or `kNative` (must not touch the heap; GC may inspect the thread's data). | [ThreadState.hpp](../../kotlin-native/runtime/src/main/cpp/mm/ThreadState.hpp) |
| Safepoint | A call to `mm::safePoint()`: a load of one global function pointer, slow path only when some "safepoint action" is active. | [SafePoint.hpp](../../kotlin-native/runtime/src/main/cpp/mm/SafePoint.hpp) |
| STW | Stop-the-world: all mutators are suspended at a safepoint or are in native state. | [ThreadSuspension.hpp](../../kotlin-native/runtime/src/main/cpp/mm/ThreadSuspension.hpp) |
| Epoch | One GC cycle, numbered by `GCStateHolder` (scheduled / started / finished / finalized). | [GCState.hpp](../../kotlin-native/runtime/src/main/cpp/gc/GCState.hpp) |
| Root set | Per thread: shadow stack + TLS. Global: registered globals + `ExternalRCRef`s with `rc > 0`. | [RootSet.hpp](../../kotlin-native/runtime/src/main/cpp/mm/RootSet.hpp) |
| `ExtraObjectData` ("meta object") | Lazily attached side record of an object: flags, ObjC associated object, weak-reference back pointer. | [ExtraObjectData.hpp](../../kotlin-native/runtime/src/main/cpp/mm/ExtraObjectData.hpp) |
| `ExternalRCRef` | A reference-counted handle to a Kotlin object held from outside the heap (ObjC/Swift/C, Cleaner). `rc > 0` makes it a root. | [ExternalRCRef.hpp](../../kotlin-native/runtime/src/main/cpp/ExternalRCRef.hpp) |
| Parallel vs concurrent | *Parallel*: several threads mark while mutators are stopped. *Concurrent*: GC work runs while mutators run (needs barriers). | [README](../../kotlin-native/runtime/README.md#modules) |
| Mutator assists | Scheduler policy: when the heap exceeds `targetHeapBytes`, mutators pause at safepoints until the current epoch finishes. | [MutatorAssists.hpp](../../kotlin-native/runtime/src/main/cpp/gcScheduler/MutatorAssists.hpp) |

## How it works

### Runtime lifecycle

Initialization is per thread and lazy. `Kotlin_initRuntimeIfNeeded` in
[Runtime.cpp](../../kotlin-native/runtime/src/main/cpp/Runtime.cpp) checks a thread-local `runtimeState` and runs `initRuntime`:

1. `initializeGlobalRuntimeIfNeeded`: a CAS on `globalRuntimeStatus` makes exactly one thread run the process-wide init:
   console, `logging::OnRuntimeInit`, `crashHandlerInit`, `mm::GlobalData::init()`, ObjC export init. Other threads block in
   `mm::waitGlobalDataInitialized()` ([GlobalData.cpp](../../kotlin-native/runtime/src/main/cpp/mm/GlobalData.cpp)) before
   touching `GlobalData::Instance()`; with asserts on, `Instance()` checks you did.
2. `InitMemory` ([Memory.cpp](../../kotlin-native/runtime/src/main/cpp/mm/Memory.cpp)) registers the thread:
   `ThreadRegistry::RegisterCurrentThread` creates a `ThreadData` in state `kNative` under the registry lock and calls
   `gc().onThreadRegistration()` (CMS/PMCS use it to pick up barrier state if a GC is in progress).
3. Under a `ThreadStateGuard(kRunnable)`: `WorkerInit`, then the compiler-generated initializer chain
   (`AppendToInitializersTail` nodes) runs with `ALLOC_THREAD_LOCAL_GLOBALS`, `CommitTLSStorage`, `INIT_GLOBALS` (first runtime
   only) and `INIT_THREAD_LOCAL_GLOBALS`. Lazy file initializers use the `FILE_*` state machine in `CallInitGlobalPossiblyLock`;
   a thread that waits for another thread's initializer waits in `kNative` to avoid deadlocking the GC.
4. `konan::onThreadExit(Kotlin_deinitRuntimeCallback)` is registered. The thread is left in `kNative`.

Entry points that create runtimes: the executable launcher `Init_and_run_start`
([launcher.cpp](../../kotlin-native/runtime/src/launcher/cpp/launcher.cpp)), `workerRoutine` in
[Worker.cpp](../../kotlin-native/runtime/src/main/cpp/Worker.cpp), and `CalledFromNativeGuard`
([Memory.h](../../kotlin-native/runtime/src/main/cpp/Memory.h)), which every ObjC/C/Swift-export bridge uses: it initializes the
runtime if needed and switches to `kRunnable` for the scope. The XCTest bundle launcher
([TestLauncher.mm](../../kotlin-native/runtime/src/xctest_launcher/cpp/TestLauncher.mm)) enters through
`Konan_create_testSuite`.

Per-thread teardown (`deinitRuntime`): `ClearTLS`, switch to `kNative`, `WorkerDeinit`, `DeinitMemory` →
`ThreadRegistry::Unregister`. `DeinitMemory` asserts `kNative`: the GC may hold the registry lock while waiting for this thread
to suspend, so unregistering in `kRunnable` would deadlock (comment in `Memory.cpp`).

Process shutdown is deliberately minimal. `Konan_main` calls `Init_and_run_start(argc, argv, memoryDeInit = 1)`, which after
`Konan_start` returns calls `Kotlin_shutdownRuntime`: CAS `globalRuntimeStatus` to shutdown and switch the main thread to
`kNative` "so the GC thread does not wait on it". Nothing is freed; `GlobalData` has a deleted destructor. Unverified: no
caller of `Init_and_run_start` with `memoryDeInit = 0` was found, and there is no "destroy runtime mode" binary option in
[BinaryOptions.kt](../../native/binary-options/src/main/kotlin/org/jetbrains/kotlin/config/nativeBinaryOptions/BinaryOptions.kt).

### Thread states and safepoints

The state lives in `ThreadSuspensionData::state_`
([ThreadSuspension.cpp](../../kotlin-native/runtime/src/main/cpp/mm/ThreadSuspension.cpp)). `SwitchThreadState` asserts the switch is a real change unless `reentrant` is passed. The important asymmetry:
**the `kNative → kRunnable` transition is itself a safepoint** (`setState` calls `safePoint(threadData, seq_cst)`), so a thread
returning from native code blocks there if a suspension is pending. This is why native-state threads never need to be signalled:
the GC treats them as already stopped, and they cannot re-enter the heap until the world resumes.

`mm::safePoint()` ([SafePoint.cpp](../../kotlin-native/runtime/src/main/cpp/mm/SafePoint.cpp)) loads `safePointAction`; it is
non-null only while at least one `SafePointActivator` exists. The slow path `safePointActionImpl` runs, in order:
`gcScheduler().safePoint()` (aggressive scheduler), `gc().safePoint()` (CMS: flush the thread's mark queue if asked), and
`suspensionData().suspendIfRequested()`. `ExtraSafePointActionActivator` lets a subsystem hook a one-off action into this path
(CMS's `FlushActionActivator` is the example). Codegen emits `Kotlin_mm_safePointFunctionPrologue` / `..WhileLoopBody`
calls, which are just `mm::safePoint()`.

Mutexes that can be contended from `kRunnable` should be `ThreadStateAware<T>`
([concurrent/Mutex.hpp](../../kotlin-native/runtime/src/main/cpp/concurrent/Mutex.hpp)): `try_lock` first, then a full switch to
`kNative` while blocking. Blocking in `kRunnable` stalls every STW.

### Stop-the-world protocol

Requester side (`stopTheWorld` in [MarkAndSweepUtils.cpp](../../kotlin-native/runtime/src/main/cpp/gc/MarkAndSweepUtils.cpp),
implemented in `ThreadSuspension.cpp`):

```mermaid
sequenceDiagram
    participant GC as GC thread (unregistered)
    participant M as mutator (kRunnable)
    participant N as mutator (kNative)
    GC->>GC: TryRequestThreadsSuspension: SafePointActivator, then gSuspensionRequestReason = reason
    M->>M: next safePoint(): slow path -> suspendIfRequested()
    M->>M: state = kNative (pauseMutationInScope), gc().OnSuspendForGC(), wait on condvar
    GC->>GC: WaitForThreadsSuspension: lock ThreadRegistry, spin until every thread is kNative
    Note over N: never notified; blocks on its next kNative -> kRunnable switch
    GC->>GC: ... STW work ...
    GC->>GC: ResumeThreads: drop activator, reason = nullptr under mutex, notify_all
    M->>M: state = kRunnable under the mutex, continue
    N->>N: safePoint in setState passes, continue
```

Details that matter: the activator is installed *before* the reason, so mutators start taking the slow path but keep running
until the reason appears; `RequestThreadsSuspension` asserts the caller is not a registered thread (a registered requester, for
example the memory dump, must use `ThreadSuspensionData::requestThreadsSuspension`, which keeps hitting safepoints while
another suspension is active); only one suspension can be active (`gSuspensionRequestReason` is a single global); the registry
stays locked for the whole STW, so no thread can register or unregister during it.

### Roots, side data and external references

- **Shadow stack** ([ShadowStack.hpp](../../kotlin-native/runtime/src/main/cpp/mm/ShadowStack.hpp)): codegen calls
  `EnterFrame`/`LeaveFrame` with zeroed slots; the GC walks `FrameOverlay` frames with no locking, so it must happen while the
  owner is stopped or by the owner itself. The header notes it does not support stack-allocated objects being scanned.
- **Globals** ([GlobalsRegistry.hpp](../../kotlin-native/runtime/src/main/cpp/mm/GlobalsRegistry.hpp)): `RegisterGlobal`
  appends to a per-thread producer queue; `ThreadData::Publish()` moves it to the global list. Root collection publishes each
  thread first (`collectRootSet` in [MarkAndSweepUtils.hpp](../../kotlin-native/runtime/src/main/cpp/gc/MarkAndSweepUtils.hpp)),
  which is why globals must be collected after all threads were published.
- **TLS** ([ThreadLocalStorage.hpp](../../kotlin-native/runtime/src/main/cpp/mm/ThreadLocalStorage.hpp)): records are added
  before `Commit`, then looked up by `(key, index)`; cleared at thread deinit.
- **`ExternalRCRefRegistry`** ([ExternalRCRefRegistry.hpp](../../kotlin-native/runtime/src/main/cpp/mm/ExternalRCRefRegistry.hpp)):
  the header lists the invariants. Each `ExternalRCRefImpl` has `obj_` and `rc_`; `rc_ > 0` ⇒ root, `rc_ == 0` ⇒ weak,
  `disposedMarker` ⇒ garbage. Refs live in thread-local lists until published; a lock-free intrusive "roots" list is what root
  scanning walks; only the GC thread removes from either list. `rc_` can change from any thread in any state, which is what
  makes ObjC `retain`/`release` on a Kotlin object safe. Used by C-export stable pointers (`CreateStablePointer`), ObjC back
  references ([ObjCBackRef.hpp](../../kotlin-native/runtime/src/main/cpp/ObjCBackRef.hpp)), Cleaners, and the stdlib's
  `kotlin.native.internal.ref.ExternalRCRef`. Permanent objects are encoded specially and never registered.
- **`ExtraObjectData`** is installed on demand with a CAS on `typeInfoOrMeta_`
  ([ExtraObjectData.cpp](../../kotlin-native/runtime/src/main/cpp/mm/ExtraObjectData.cpp)); its storage comes from the
  allocator (`allocateExtraObjectData`). Triggers found in code: creating a regular weak reference
  ([Weak.cpp](../../kotlin-native/runtime/src/main/cpp/mm/Weak.cpp)), setting an ObjC associated object
  (`ObjHeader::SetAssociatedObject` in `Memory.cpp`, which also sets `FLAGS_RELEASE_ON_MAIN_QUEUE` when done on the main
  thread). Identity hash is *not* stored there: `Kotlin_Any_hashCode` in
  [Natives.cpp](../../kotlin-native/runtime/src/main/cpp/Natives.cpp) returns the address.
- **Weak references**: a heap `RegularWeakReferenceImpl` object is linked from the referent's `ExtraObjectData` (tagged pointer)
  and registered as an `rc == 0` `ExternalRCRef`. `processWeaks` nulls the registry entries whose `obj_` is unmarked; reads go
  through `gc::weakRefReadBarrier` so concurrent GCs can refuse to resurrect an object being cleared.

### Allocation and accounting

`AllocInstance`/`AllocArrayInstance` → `mm::AllocateObject`
([ObjectOps.cpp](../../kotlin-native/runtime/src/main/cpp/mm/ObjectOps.cpp)): assert `kRunnable`, `allocator().allocateObject`, `gc().onAllocation(object)` (CMS/PMCS allocate black while marking), then a
release fence so another thread that later reads the reference sees an initialized header (KT-58995). Reference stores from
Kotlin go through `mm::RefAccessor` ([ReferenceOps.hpp](../../kotlin-native/runtime/src/main/cpp/ReferenceOps.hpp)): stack slots
have no barrier; heap stores call `gc::beforeHeapRefUpdate`, which is empty for STMS, PMCS and noop and a real barrier for CMS.

The default allocator is `custom` ([alloc/custom/README.md](../../kotlin-native/runtime/src/alloc/custom/README.md); page
types, per-thread pages, sweep-on-demand). GC accounting is coarse: `AllocatedSizeTracker::onPageOverflow`
([AllocatedSizeTracker.cpp](../../kotlin-native/runtime/src/alloc/custom/cpp/AllocatedSizeTracker.cpp)) calls
`OnMemoryAllocation` → `gcScheduler().setAllocatedBytes` when a page fills, not per object. The legacy allocator
([alloc/legacy](../../kotlin-native/runtime/src/alloc/legacy/cpp/ObjectAlloc.cpp), `-Xallocator=std`) reports every allocation.
`pagedAllocator=false` makes the custom allocator put each object in its own page (useful under sanitizers; it is the default
when a sanitizer is on, `NativeSecondStageCompilationConfig.defaultPagedAllocator`).

### A GC cycle

Every tracing GC is a `GC::Impl` holding a `GCStateHolder` and a `MainGCThread<Traits>`
([MainGCThread.hpp](../../kotlin-native/runtime/src/main/cpp/gc/MainGCThread.hpp)), a `UtilityThread` named "Main GC thread"
that is *not* registered as a mutator. `PerformFullGC(epoch)`:

1. take `gc().gcLock()` (also taken by the memory dump), `mark_.setupBeforeSTW` (CMS/PMCS: lock the thread registry for the
   whole mark, construct the `ParallelProcessor`)
2. `stopTheWorld`, `gcScheduler_.onGCStart()`, `state_.start(epoch)`
3. `mark_.markInSTW()` — the only part that differs per GC (table below)
4. every thread's `allocator().prepareForGC()` and the global one (custom allocator: mark pages unswept, take threads' pages)
5. if `Traits::kConcurrentSweep`: `resumeTheWorld`; `allocator_.sweep(gcHandle)` returns the finalizer queue;
   `gcScheduler_.onGCFinish(epoch, keptBytes)`; otherwise resume after sweep
6. `state_.finish(epoch)`, `gcHandle.finished()` (publishes `GC.lastGCInfo` through
   [GCStatistics.hpp](../../kotlin-native/runtime/src/main/cpp/gc/GCStatistics.hpp)), `allocator_.scheduleFinalization`

Marking is generic over a `MarkTraits` type (`tryEnqueue`, `tryDequeue`, `tryMark`, `processInMark`) in
`MarkAndSweepUtils.hpp`. `processInMark` calls the per-type `processObjectInMark` function from `TypeInfo`, which codegen
emits and which calls back `Kotlin_processObjectInMark` / `Kotlin_processArrayInMark`. The mark bit and mark-queue link are
one field, `GC::ObjectData::next_`, placed in a header before each object
([ObjectData.hpp](../../kotlin-native/runtime/src/gc/cms/cpp/ObjectData.hpp)); `tryMark` is a CAS from null to a sentinel. Work distribution uses `ParallelProcessor`
([concurrent/ParallelProcessor.hpp](../../kotlin-native/runtime/src/main/cpp/concurrent/ParallelProcessor.hpp)) with per-worker
local batches and a shared pool.

| GC (`-Xbinary=gc=`) | Module | Mark | Weak processing | Sweep | Barriers |
|---|---|---|---|---|---|
| `cms` (default) | [gc/cms](../../kotlin-native/runtime/src/gc/cms/cpp/ConcurrentMark.cpp) | Roots in STW, closure concurrent | Concurrent | Concurrent | Deletion barrier, weak read, allocation |
| `pmcs` | [gc/pmcs](../../kotlin-native/runtime/src/gc/pmcs/cpp/ParallelMark.cpp) | Parallel, in STW | Concurrent if `concurrentWeakSweep` (default) | Concurrent | Weak read, allocation |
| `stwms` | [gc/stms](../../kotlin-native/runtime/src/gc/stms/cpp/SingleThreadMark.hpp) | Single thread, in STW | In STW | In STW | None |
| `noop` | [gc/noop](../../kotlin-native/runtime/src/gc/noop/cpp/GCImpl.cpp) | — | — | — | None |

- **CMS** (`ConcurrentMark::markInSTW`): in STW, the GC thread collects each mutator's roots (`tryLockRootSet` guards against
  double collection), enables barriers (`kMarkClosure`) and resumes the world. Then it collects globals and builds the closure
  concurrently. Termination runs up to `concurrentMarkMaxIterations` rounds of: take `markTerminationMutex_`, force every
  mutator to flush its local mark queue (`FlushActionActivator`, executed at the mutator's next safepoint, or by the GC thread
  for threads in `kNative`), and check that `ParallelProcessor::batchesEverShared` did not grow; if it never stabilizes, a
  fallback STW finishes the mark. Weak processing runs concurrently under `kWeakProcessing` barriers; a final STW
  ("prepare to sweep") disables barriers and prepares the allocator. The barriers
  ([Barriers.cpp](../../kotlin-native/runtime/src/gc/cms/cpp/Barriers.cpp)): `beforeHeapRefUpdate` enqueues the *overwritten* heap reference during `kMarkClosure` (a deletion barrier); new objects are
  allocated marked; weak reads mark the referee in `kMarkClosure` and return null for unmarked referees in `kWeakProcessing`;
  `ExternalRCRefReleaseGuard` keeps an external release from racing with the mark phase. CMS asserts `!mutatorsCooperate` and
  `auxGCThreads == 0` ([GCImpl.hpp](../../kotlin-native/runtime/src/gc/cms/cpp/GCImpl.hpp)).
- **PMCS** (`ParallelMark::markInSTW`): everything up to the end of marking happens in STW. Workers are the main GC thread,
  `auxGCThreads` ([AuxiliaryGCThreads.hpp](../../kotlin-native/runtime/src/gc/pmcs/cpp/AuxiliaryGCThreads.hpp)) and, with
  `gcMutatorsCooperate`, the suspended mutators themselves (`OnSuspendForGC` → `markOnMutator`); the `MarkPacer` phases
  (`kReady` → `kRootSet` → `kParallelMark`) sequence them. `gcMarkSingleThreaded` forces local-only queues. With
  `concurrentWeakSweep` the GC enables weak-read barriers, resumes the world, processes weaks, and stops the world again
  ([Barriers.cpp](../../kotlin-native/runtime/src/gc/pmcs/cpp/Barriers.cpp) explains the re-read race).

Defaults are decided in `NativeSecondStageCompilationConfig` (`defaultGC = CONCURRENT_MARK_AND_SWEEP`, `gcMarkSingleThreaded`
defaults to true only for MinGW with PMCS). The compiler links the matching `.bc` in
[CompilerOutput.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/CompilerOutput.kt),
file names in
[RuntimeModule.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/runtime/RuntimeModule.kt).

### GC scheduler

`GCScheduler` ([GCScheduler.hpp](../../kotlin-native/runtime/src/main/cpp/gcScheduler/GCScheduler.hpp)) owns a
`GCSchedulerConfig` ([GCSchedulerConfig.hpp](../../kotlin-native/runtime/src/main/cpp/gcScheduler/GCSchedulerConfig.hpp)) that the
stdlib's `kotlin.native.runtime.GC` object reads and writes (`Kotlin_native_internal_GC_*` in `Memory.cpp`). The actual
decision to start an epoch goes through `EpochScheduler`
([EpochScheduler.hpp](../../kotlin-native/runtime/src/main/cpp/gcScheduler/EpochScheduler.hpp)) into `GCStateHolder::schedule`.

- **adaptive** (default; `-Xbinary=gcSchedulerType=`): `HeapGrowthController` with `targetHeapBytes` (initially 10 MiB) and a
  trigger at `0.9 * target`. After each epoch, if `autoTune`, `target = alive / targetHeapUtilization (0.5)` clamped to
  `[minHeapBytes = 5 MiB, maxHeapBytes]`. A "GC Timer thread" schedules an epoch every `regularGcInterval` (10 s) of idleness,
  skipped while the app is in background if `appStateTracking` is enabled
  ([AppStateTrackingUIKit.mm](../../kotlin-native/runtime/src/main/cpp/mm/AppStateTrackingUIKit.mm)). Crossing `targetHeapBytes` requests **mutator assists**: every mutator parks at its next safepoint until the epoch is done; the
  GC thread's `onGCFinish` waits for all of them. Assists are on by default only when `autoTune` is on and `maxHeapBytes` is
  unlimited (reasoning in `GCSchedulerConfig::mutatorAssists`).
- **aggressive**: a stress tool. It holds a `SafePointActivator` permanently, so every safepoint takes the slow path, and
  schedules a GC the first time each distinct safepoint stack trace is seen (`SafePointTracker`), plus the heap triggers.
- **manual**: only `GC.collect()` / `GC.schedule()`. Default for `gc=noop`.

### Finalizers and Cleaners

`HasFinalizers(obj)` is "has a meta object or `TF_HAS_FINALIZER`"
([FinalizerHooks.cpp](../../kotlin-native/runtime/src/main/cpp/FinalizerHooks.cpp)). The custom allocator's sweep
([GCApi.cpp](../../kotlin-native/runtime/src/alloc/custom/cpp/GCApi.cpp)) keeps such objects alive one more cycle and links
their `ExtraObjectCell`s into a `SegregatedFinalizerQueue` (regular vs main-thread). After the cycle,
[SegregatedFinalizerProcessor.hpp](../../kotlin-native/runtime/src/main/cpp/alloc/SegregatedFinalizerProcessor.hpp) hands the
regular queue to the "GC finalizer processor" thread
([FinalizerProcessor.hpp](../../kotlin-native/runtime/src/main/cpp/alloc/FinalizerProcessor.hpp)), which runs `RunFinalizers` in `kRunnable`: `DisposeCleaner` → `Kotlin_internal_executeAndRelease` for `CleanerImpl`,
`DisposeRegularWeakReferenceImpl` for weak refs, then destroys the meta object (which releases the ObjC associated object).
Objects flagged `FLAGS_RELEASE_ON_MAIN_QUEUE` go to `MainThreadFinalizerProcessor` when `objcDisposeOnMain` is set and the
main run loop is being processed. `GC.collect()` waits for the finalized epoch (`scheduleAndWaitFinalized`).

### Runtime variants, constants and diagnostics

- Build: every module in [runtime/build.gradle.kts](../../kotlin-native/runtime/build.gradle.kts) is one `module("…")` block
  compiled by the `compile-to-bitcode` plugin from [kotlin-native/build-tools](../../kotlin-native/build-tools) into
  `dist/konan/targets/<target>/native/*.bc`. `testsGroup` blocks pair a tested module with the support modules it needs.
- Compile-time constants: the compiler emits globals such as `Kotlin_runtimeAssertsMode`, `Kotlin_gcMarkSingleThreaded`,
  `Kotlin_runtimeLogsEnabled` that [CompilerConstants.hpp](../../kotlin-native/runtime/src/main/cpp/CompilerConstants.hpp) wraps.
  The only runtime environment variable found is `KOTLIN_NATIVE_AVAILABLE_PROCESSORS` (`Runtime.cpp`).
- Logging ([Logging.hpp](../../kotlin-native/runtime/src/main/cpp/Logging.hpp)): `RuntimeLogDebug({kTagGC}, fmt, …)` and
  friends; tags `logging`, `rt`, `gc`, `mm`, `tls`, `pause`, `alloc`, `balancing`, `barriers`, `gcMark`, `gcScheduler`,
  `memoryDump`; levels error < warning < info < debug. `-Xruntime-logs=gc=info,pause=debug` is parsed in
  `NativeSecondStageCompilationConfig.runtimeLogs` against the enums in
  [RuntimeLogging.kt](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/RuntimeLogging.kt)
  (must match `Logging.hpp`) and emitted as the `runtimeLogs()` table. Output goes to stderr.
- Assertions ([KAssert.h](../../kotlin-native/runtime/src/main/cpp/KAssert.h)): `RuntimeAssert` obeys
  `-Xbinary=runtimeAssertionsMode=ignore|log|panic` (default ignore); `RuntimeCheck`/`RuntimeFail` are always on.
  `AssertThreadState` is compiled out in `ignore` mode, so thread-state bugs only surface with asserts enabled.
- Calls checker ([externalCallsChecker/README.md](../../kotlin-native/runtime/src/externalCallsChecker/README.md),
  `-Xbinary=checkStateAtExternalCalls=true`): instruments external calls made in `kRunnable`. Runtime code that knowingly calls
  out in `kRunnable` wraps the call in `CallsCheckerIgnoreGuard` or marks the function `NO_EXTERNAL_CALLS_CHECK`.
- Stack traces and symbolication: [StackTrace.hpp](../../kotlin-native/runtime/src/main/cpp/StackTrace.hpp) captures frames;
  `compiler::getSourceInfo` is provided by `source_info_core_symbolication` (Apple) or `source_info_libbacktrace`
  (`-Xbinary=sourceInfoType=`). Crash handler: Breakpad minidumps, macOS only, when `minidumpLocation` is set
  ([CrashHandler.hpp](../../kotlin-native/runtime/src/main/cpp/CrashHandler.hpp)). `MemoryUsageInfo.hpp` gives peak RSS.
- Memory dump: `kotlin.native.runtime.Debugging.dumpMemory(fd)` → `Kotlin_native_runtime_Debugging_dumpMemoryWithOptions`
  (`Memory.cpp`): takes `gcLock`, suspends all other threads from a mutator, publishes every thread, writes roots and walks
  the allocator's objects ([MemoryDump.cpp](../../kotlin-native/runtime/src/main/cpp/mm/MemoryDump.cpp)). Format:
  [MemoryDump.md](../../kotlin-native/runtime/src/main/cpp/mm/MemoryDump.md).

## Invariants and ordering rules

- **Heap access requires `kRunnable`.** `RefAccessor`, `AllocateObject`, `EnterFrame`, `RegisterGlobal`, `LookupTLS`,
  `createRetainedExternalRCRef` all `AssertThreadState(kRunnable)`. Conversely, anything that may block on another thread
  (locks, I/O, ObjC calls) must run in `kNative`, or the next STW hangs (calls-checker README, "Background").
- **`kNative` means "the GC may read my `ThreadData` now".** Do not touch the shadow stack, TLS or mark queues in `kNative`.
- **Exceptions**: `ExternalRCRef` `rc_` and `dispose` may be used in any state; `weakRefReadBarrier`'s
  `ExternalRCRefReleaseGuard` path and `ObjCBackRef::release` are written to be callable from unregistered threads (TLS
  destructors). Read the comments in [cms/Barriers.cpp](../../kotlin-native/runtime/src/gc/cms/cpp/Barriers.cpp) before changing them.
- **Thread registration/unregistration happens in `kNative`** (asserted in `ThreadRegistry.cpp`), and the registry lock is
  held across the whole STW, so a thread stuck in `kRunnable` blocks thread creation too.
- **Lock order observed in code** (not documented as a rule): `GC::gcLock_` → `ThreadRegistry` list lock → per-registry locks
  (`GlobalsRegistry` spin lock and `ExternalRCRefRegistry` spin lock are taken together only in `GlobalRootSet`, per its
  comment). `GCStateHolder::mutex_` and `EpochScheduler::scheduledEpochMutex_` are leaf locks. Hypothesis: the GC thread is
  intentionally unregistered so it can take these locks without participating in suspension.
- **Only one suspension at a time**, and the requester must not itself be a suspendable registered thread unless it uses the
  looping `requestThreadsSuspension`.
- **Safepoint code is reentrancy-checked** (`RuntimeAssert(!recursion)` in `safePointActionImpl`): a safepoint action must
  not call `mm::safePoint()`.
- **Root publication order**: per-thread queues are published before globals are scanned; the
  `ExternalRCRefRegistry` comment "during roots list traversal all refs to the left are either marked or inserted into the
  mark queue" is what allows concurrent `retain` during CMS root scanning.
- **Finalizers run after the object's fields may be freed** (`FinalizerHooks.hpp`); a finalizer must only use
  `ExtraObjectData` and the `ExternalRCRef` it holds.
- **`GC::ObjectData` must be trivially destructible** and is never deinitialized (`GC.hpp`).
- **`GlobalData` is single-shot and immortal**; tests assert a clean global state after each suite instead.

## Extension points and typical changes

| Change | Where | Cross-subsystem impact |
|---|---|---|
| New GC | New `src/gc/<name>/cpp` implementing `GC::Impl`, `GC::ThreadData::Impl`, `beforeHeapRefUpdate`, `weakRefReadBarrier`, `ExternalRCRefReleaseGuard::Impl`, plus a `GCTraits` for `MainGCThread`; a `module()`/`testsGroup()` in `build.gradle.kts`; a `RuntimeModule` entry and `CompilerOutput.kt` branch; a `GC` enum value in `GC.kt` | Allocator (`objectDataForObject` layout), compiler option parsing, test matrix (`-Pkn.gcType`) |
| New scheduler policy | `src/gcScheduler/<name>/cpp/GCSchedulerImpl.{hpp,cpp}` implementing `GCScheduler::Impl` and `ThreadData::Impl`; same build/compiler plumbing | `GCSchedulerConfig` is shared with the stdlib `GC` object; keep `GC.kt` docs in sync (comment in `GCSchedulerConfig.hpp`) |
| New safepoint action | `ExtraSafePointActionActivator<Impl>` + `doIfActive` in the slow path | Must be cheap and non-blocking; see CMS `FlushActionActivator` |
| New kind of external reference / root | `ExternalRCRefRegistry` (`rc` semantics) or a new `Source` in `RootSet.hpp` | Every `MarkTraits::collectRoot` caller, `MemoryDump`, `GCStatistics` root counters |
| New runtime log tag or level | `Logging.hpp` **and** `RuntimeLogging.kt` (ordinals must match) | Compiler emits the per-tag table |
| New compile-time switch | `BinaryOptions.kt` → `NativeSecondStageCompilationConfig` → emitted `Kotlin_*` constant → `CompilerConstants.hpp` | Add to the cache-name suffix in `NativeSecondStageCompilationConfig` if it changes runtime behaviour |
| New finalizable object type | `RunFinalizerHooksImpl` dispatch on `TypeInfo` + `TF_HAS_FINALIZER` set by the compiler | Sweep keeps such objects alive one extra cycle |
| New runtime entry point for Kotlin | `extern "C"` with `Kotlin_<module>_` prefix; stdlib side `@GCUnsafeCall` / `@ExportForCppRuntime` | See [compiler-runtime-contract.md](compiler-runtime-contract.md) |

Style and conventions: [codestyle/cpp/README.md](../../kotlin-native/runtime/codestyle/cpp/README.md); utilities in
`std_support/` (backports of newer stdlib pieces), `concurrent/` (spin locks, `ThreadStateAware`, `ScopedThread`,
`UtilityThread`, `ParallelProcessor`), `objc_support/`.

## Investigating bugs

**Build configurations that change what you see**

| Knob | Effect |
|---|---|
| `-Xbinary=runtimeAssertionsMode=panic` | Turns on `RuntimeAssert`, including every thread-state and barrier-phase check |
| `-Xbinary=checkStateAtExternalCalls=true` | Aborts on external calls made in `kRunnable` (needs debug + no caches in tests: `-Pkn.useThreadStateChecker=true`) |
| `-Xbinary=gc=stwms` / `pmcs` / `cms`, `gcSchedulerType=aggressive` / `manual` | Bisect: if a crash disappears with `stwms`, suspect barriers or termination; `aggressive` finds missing roots fast |
| `-Xbinary=gcMarkSingleThreaded=true`, `auxGCThreads=0`, `gcMutatorsCooperate=false` | Remove parallelism from PMCS |
| `-Xbinary=pagedAllocator=false`, `-Xbinary=sanitizer=thread` | Per-object pages so ASan/TSan can see overflows; TSan enables `STRICT_ATOMICS_IN_HEAP` in `ReferenceOps.hpp` |
| `-Xruntime-logs=gc=debug,pause=info,gcScheduler=info` | Epoch timeline, pause durations with reasons, scheduling reasons |
| `kotlin.native.isNativeRuntimeDebugInfoEnabled=true` + `-Xbinary=stripDebugInfoFromNativeLibs=false` | Runtime with DWARF, debuggable in CLion/lldb ([HACKING.md](../../kotlin-native/HACKING.md#kotlinnative-runtime-specifics)) |

**Symptom → where to look**

- *Crash in `Mark`, `processObjectInMark`, or sweep; bad `TypeInfo` pointer.* Usually a missing root or a reference stored
  without the runtime seeing it. Check: `EnterFrame`/slot usage in the generated code, direct writes through `DirectRefAccessor`
  instead of `RefAccessor`, `ExternalRCRef` released while `rc == 0`. Reproduce with `gcSchedulerType=aggressive`; compare
  `stwms` vs `cms` to isolate barrier bugs; run `:native:native.tests:gcTest`.
- *Hang with all threads idle.* Someone blocks in `kRunnable`: look for a thread in `pthread_cond_wait`/syscall whose
  `ThreadData` state is `kRunnable`, while the GC thread spins in `WaitForThreadsSuspension` or `MutatorAssists::completeEpoch`.
  `-Xruntime-logs=pause=debug` shows who requested suspension. Enable the calls checker.
- *"Illegal thread state switch" / "Unexpected thread state".* A guard was skipped or doubled; the assert message names old and
  new states. Guards are `ThreadStateGuard`, `CalledFromNativeGuard`, `Kotlin_mm_switchThreadState*` from codegen.
- *Finalizer or Cleaner never runs / runs late.* Check the epoch reached `finalized` (`GC.lastGCInfo`), whether the object
  landed in the main-thread queue (`objcDisposeOnMain`, `FLAGS_RELEASE_ON_MAIN_QUEUE`) and whether the main run loop spins.
- *Memory grows.* Compare `GC.lastGCInfo` kept bytes with `targetHeapBytes`; dump the heap with `Debugging.dumpMemory` and
  inspect roots (`GLOBAL_ROOT` source 2 = stable ref means an `ExternalRCRef` with `rc > 0`, typically an ObjC/Swift side
  still retaining). Mutator-assist warnings ("Pausing the mutators until epoch") mean the GC is behind the allocation rate.
- *Windows-only weirdness around condition variables and process exit.* See the winpthreads notes in `GlobalData.cpp`,
  `GCState.hpp` and `MutatorAssists.hpp` (KT-50948, KT-85897).

**Tests**

- Unit tests are gtest files next to the code (`*Test.cpp`), launched by
  [test_support/cpp/TestLauncher.cpp](../../kotlin-native/runtime/src/test_support/cpp/TestLauncher.cpp), which initializes
  the global runtime once and asserts a clean global state after every suite; `test_support/cpp/CompilerGenerated.cpp`
  supplies symbols codegen would emit. Helpers: `ScopedMemoryInit`, `RunInNewThread`
  ([TestSupport.hpp](../../kotlin-native/runtime/src/main/cpp/TestSupport.hpp)), `GCTestSupport.hpp`, `TracingGCTest.hpp`
  (shared tests instantiated per GC). Run: `./gradlew :kotlin-native:runtime:hostRuntimeTests -Pgtest_filter='*Suspension*'`
  (`-Pgtest_timeout=`); the task reruns under the sanitizers supported on the host. Which modules a group links is in the
  `testsGroup` blocks (for example `runtime_test` runs with `noop_gc` + `manual_gcScheduler`, `cms_gc_test` with CMS).
- Integration: `./gradlew :native:native.tests:gcTest` runs the `gc` tag
  ([testData/gc](../../native/native.tests/testData/gc)); any test task accepts `-Pkn.gcType=CMS|PMCS|STWMS|NOOP`,
  `-Pkn.gcScheduler=ADAPTIVE|AGGRESSIVE|MANUAL`, `-Pkn.alloc=STD|CUSTOM`, `-Pkn.pagedAllocator=`, `-Pkn.sanitizer=THREAD`,
  `-Pkn.useThreadStateChecker=true`, `-Pkn.binaryOptions=k=v,…`
  ([ConfigurationProperties.kt](../../native/native.tests/testFixtures/org/jetbrains/kotlin/konan/test/blackbox/support/ConfigurationProperties.kt),
  table in [HACKING.md](../../kotlin-native/HACKING.md#testing)). Stress tests: [native.tests/stress](../../native/native.tests/stress);
  GC fuzzing: [gc-fuzzing-tests/README.md](../../native/native.tests/gc-fuzzing-tests/README.md); memory-model litmus tests:
  [litmus-tests/guide.md](../../native/native.tests/litmus-tests/guide.md).

## Open questions

- Is there any caller of `Init_and_run_start` with `memoryDeInit = 0`, or any remaining binary option that controls whether
  the runtime is torn down at exit? None was found; the launcher always calls `Kotlin_shutdownRuntime`.
- `Platform.isMemoryLeakCheckerActive` exists in the stdlib, but no runtime-side leak checker implementation was found.
  Whether leak reporting at exit still exists for the current memory manager is unverified.
- Lock ordering is inferred from call paths, not documented. Whether `ExternalRCRefRegistry::mutex_` may be taken while
  holding `GlobalsRegistry`'s lock in places other than `GlobalRootSet` was not checked exhaustively.
- `GCScheduler::schedule()` in the manual scheduler and `GC::Schedule()` for `noop` were not traced to confirm what
  `GC.collect()` does with `gc=noop`.
- `AppStateTracking` beyond the adaptive timer: no other consumer was found; whether backgrounding affects allocation-triggered
  GCs is unverified.
- How `Konan_create_testSuite` (XCTest) initializes the runtime on the XCTest thread was not traced (presumably via
  `CalledFromNativeGuard` in generated code).
- The `debug` runtime module (`src/debug`, linked for `-g` builds) was not examined; it serves the LLDB plugin, not asserts.
