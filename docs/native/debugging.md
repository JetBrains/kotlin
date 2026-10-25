# Investigating Kotlin/Native bugs: from symptom to subsystem

A routing guide. Each row sends you to the subsystem page whose "Investigating" section has the detailed steps, and names the
flags and tests that usually settle the question fastest. Test mechanics (properties, tags, directives) are canonical in
[HACKING.md](../../kotlin-native/HACKING.md#testing); flags are only listed here when they are the first thing to reach for.

## Step 0: reproduce outside Gradle

Every failing test in `native/native.tests` logs the exact compiler invocation it made
(`COMPILER ARGUMENTS:` followed by `kotlin-native/dist/bin/kotlinc-native …`, from
[LoggedData.kt](../../native/native.tests/testFixtures/org/jetbrains/kotlin/konan/test/blackbox/support/LoggedData.kt)), and
likewise `CINTEROP INVOCATION ARGUMENTS:` and `SWIFTC INVOCATION ARGUMENTS:`. Copy it; add the flags below. The test infrastructure
compiles with `-Xverify-ir=error -Xphases-to-validate-after=all`, so an IR validation failure in a test may not reproduce without
those flags.

Other things to know before you start:
- Tests run the compiler from `kotlin-native/dist`, not the IDE classpath. A compiler change means a dist rebuild (minutes).
  `-Pkn.nativeHome=<dist>` skips the rebuild when you only changed tests.
- The default test mode is `cacheMode=STATIC_ONLY_DIST`, `optimizationMode=DEBUG`. In `OPT` runs the stdlib cache disables all
  whole-program optimizations unless you also pass `-Pkn.cacheMode=NO` ([optimizations.md](optimizations.md)).
- `IGNORE_NATIVE` means "expected to fail": a test that starts passing fails the run. See
  [HACKING.md](../../kotlin-native/HACKING.md#compiler-blackbox-tests).

## Symptom → where to look

### The compiler fails

| Symptom | First suspects | First moves |
|---------|----------------|-------------|
| Exception with a lowering class name in the stack trace, "Internal error in file lowering" | that lowering ([lowerings.md](lowerings.md)) | `-Xphases-to-dump-before=<ClassSimpleName> -Xdump-directory=<dir>`; compare with `-after`. Check `@PhasePrerequisites` and the position in `NativeLoweringPhases.kt`. |
| IR validation error (visibility, types, unbound symbols) at a `IrValidation*` phase | the lowering just before the synchronization point; inlining ([lowerings.md](lowerings.md)) | `-Xphases-to-validate-after=all -Xverify-ir=error` to find the first offending phase; `-Xlist-phases` for names. |
| "unbound symbol", `IrLinker` error, "symbol not found" | IR deserialization, deserialization strategy, klib ABI ([klib-linking.md](klib-linking.md)) | Check which klib and strategy (`ALL` / `EXPLICITLY_EXPORTED` / headers only); check whether a stage-1 lowering changed the klib ABI. |
| Klib rejected: ABI / compiler version, unknown `ir_provider` | `KlibLoader`, `KonanLibrarySpecialCompatibilityChecker` ([klib-linking.md](klib-linking.md)) | `klib` tool (`dist/bin/klib info`) to read the manifest. |
| Crash in `optimizations/` (DFG, devirtualization, EA, DCE) or only with `-opt` | [optimizations.md](optimizations.md) | Bisect with `-Xdisable-phases=EscapeAnalysis`, `Devirtualization`, `DevirtualizationAnalysis`, `DCEPhase`, `GHAPhase`, `PreCodegenInliner`; `-Xverbose-phases` dumps the DFG. |
| LLVM verifier failure, crash inside `IrToBitcode` / `CodeGenerator` | [llvm-codegen.md](llvm-codegen.md) | `-Xverify-bitcode`, `-Xsave-llvm-ir-after=Codegen -Xsave-llvm-ir-directory=<dir>`, `-Xprint-bitcode`; `-Xtemporary-files-dir` keeps the `.bc`/`.o`. |
| Linker error: undefined symbol | which module should define it: runtime (`Kotlin_*`), generated code (`kfun:`), a cache, a cinterop klib's `linkerOpts` | [llvm-codegen.md](llvm-codegen.md) (symbol naming, linker flag sources), [caches.md](caches.md) if only with caches, [cinterop.md](cinterop.md) for `.def` `staticLibraries` / `linkerOpts`. |
| Error only with caches, or different behaviour between `cacheMode=NO` and `STATIC_EVERYWHERE` | cache ABI, additional cache info, stale cache ([caches.md](caches.md)) | Reproduce with `-p static_cache` manually; delete the auto-cache directory; compare `-Pkn.cacheMode=NO`. |
| Stale binary after an incremental Gradle build; link error after IC; "Incremental cache for … rebuilding it" | dirty-file analysis, missing `dependenciesTracker.add`, option outside the cache flavor ([incremental-compilation.md](incremental-compilation.md)) | Read the `IC analysis results` log (Gradle `--info`); delete `build/kotlin-native-ic-cache`; reproduce with `-Xic-cache-dir` on the CLI; remember `-opt` silently disables IC. |
| Build fails or misbehaves after a compiler, Xcode, LLVM or KGP upgrade | a compatibility surface ([compatibility-and-migration.md](compatibility-and-migration.md)); platform libs ([platform-libraries.md](platform-libraries.md)) | Check the klib ABI window, `compiler.fingerprint`, the cache flavor, `minimalXcodeVersion`; the page has a "before changing X, check Y" checklist. |
| Gradle passes a flag the compiler ignores, or the IDE sees a different API than the compiler | KGP argument builders, klib metadata contract ([tooling-integration.md](tooling-integration.md)) | Compare the task's compiler arguments (`--info`) with `-Xprint-configuration`; toggle `kotlin.native.disableCompilerDaemon=true` to rule out in-process classloader issues. |
| Export header missing a declaration, wrong ObjC/Swift name, C header missing a symbol | `ObjCExportMapper.shouldBeExposed`, namer, C export model ([export.md](export.md)) | Header generator tests; `-Xomit-framework-binary` to iterate on the header only. |
| Unresolved reference to a C/ObjC symbol | indexing or `headerFilter` in the `.def`, stub generation ([cinterop.md](cinterop.md)) | Run `dist/bin/cinterop` manually with the logged `CINTEROP INVOCATION ARGUMENTS:`; inspect the klib with `dist/bin/klib`; remember ObjC forward declarations are always "included". |
| Slow compilation | phase timing | `-Xprofile-phases`, `-Xreport-perf`; `-Xbackend-threads`. |

### The program misbehaves or crashes

| Symptom | First suspects | First moves |
|---------|----------------|-------------|
| Wrong result in debug *and* `-opt` | a lowering ([lowerings.md](lowerings.md)) or codegen ([llvm-codegen.md](llvm-codegen.md)) | Minimize to a box test under `native/native.tests/testData/codegen`; dump IR after the suspected lowering; read the LLVM IR with `-Xsave-llvm-ir-after=Codegen`. |
| Wrong result or crash only with `-opt` | devirtualization, DCE, escape analysis ([optimizations.md](optimizations.md)) | `-Xdisable-phases=…` one at a time. Stack-allocated object escaping → memory corruption; `-Xsave-llvm-ir-after=Codegen` shows `alloca %"kclassbody:…"`. |
| Segfault on object access, "TypeInfo" mismatch, wrong `is` / cast result | object layout / TypeInfo contract ([compiler-runtime-contract.md](compiler-runtime-contract.md)); GHA ids ([optimizations.md](optimizations.md)) | `-Xbinary=runtimeAssertionsMode=panic` with the debug runtime; `-Xdisable-phases=GHAPhase`. |
| Runtime assertion (`RuntimeAssert`, `KAssert`) or "wrong thread state" | compiler–runtime contract, thread state switching ([compiler-runtime-contract.md](compiler-runtime-contract.md)) | Build the runtime with asserts: `-Xbinary=runtimeAssertionsMode=panic`; `-Xbinary=checkStateAtExternalCalls=true` (debug, no caches; test property `-Pkn.useThreadStateChecker=true`). |
| GC crash, heap corruption, hang in stop-the-world | GC / scheduler / allocator ([runtime.md](runtime.md); algorithms in [gc.md](gc.md), [allocator.md](allocator.md)) | Bisect with `-Xbinary=gc=stwms` (then `pmcs`, `noop`), `-Xbinary=gcSchedulerType=aggressive` (stress), `-Xbinary=pagedAllocator=false` plus `-Xbinary=sanitizer=thread`; `-Xruntime-logs=gc=debug` (tags in `RuntimeLogging.kt`); `Debugging.dumpMemory` + kdumputil. Test knobs: `-Pkn.gcType`, `-Pkn.gcScheduler`, `-Pkn.alloc`, `gcTest`, gc-fuzzing-tests, litmus-tests. |
| Object never collected / finalizer or Cleaner not run | roots: ExternalRCRef, globals, ExtraObjectData ([runtime.md](runtime.md)); ObjC wrappers ([export.md](export.md)) | `-Xruntime-logs=gc=debug`; memory dump. There is no leak checker binary option (`GC.detectCycles` is a stub). |
| Crash calling a C function; wrong value across the boundary | bridge ABI, C stubs, `.def` type mapping, call mode ([cinterop.md](cinterop.md)) | `-verbose` keeps the generated `cstubs.c`; `-Xphases-to-dump-after=InteropLowering`; switch `-Xbinary=cCallMode=indirect` vs `direct`; FileCheck tests with `FILECHECK_STAGE: CStubs`. |
| Crash calling an exported method from ObjC/Swift; leak or over-release across the boundary; no `NSError` for a thrown exception | ObjC export bridges and the ObjC runtime side ([export.md](export.md)) | `objcexport` / `framework` tests (macOS); `-Xruntime-logs`; check `@Throws` on the Kotlin declaration. |
| `IrLinkageError` thrown at run time | partial linkage replaced a declaration with a stub ([klib-linking.md](klib-linking.md)) | `-Xpartial-linkage-loglevel=warning` (default is silent); if caches are involved, the issue is replayed from the cache. |
| Static initializer not run / run in the wrong order / deadlock during initialization | the initialization state machine, `RemoveRedundantCallsToStaticInitializersPhase`, file order ([static-initializers.md](static-initializers.md)) | `-Xdisable-phases=RemoveRedundantCallsToStaticInitializersPhase` (`-Xverbose-phases=…` prints its call graph); check `_Konan_init_<lib>` in LLVM IR; a cross-thread initialization cycle deadlocks undetected. |
| Null function pointer call in `-opt`; `is`/itable result wrong only in `-opt` | DCE root-set gap; GHA ids ([dce-and-gha.md](dce-and-gha.md)) | `-Xdisable-phases=DCEPhase` / `GHAPhase`; check whether the function reaches the outside through a path not in `computeRootSet`. |
| Call target wrong only in `-opt`, "Unexpected receiver type" exception | devirtualization ([devirtualization.md](devirtualization.md)) | `-Xdisable-phases=Devirtualization`; `-Xverbose-phases=DevirtualizationAnalysis`; check for `Type.Virtual` (ObjC) or an unrooted entry point. |
| `ClassCastException` only in debug, or memory read through the wrong type only in `-opt` | generic safe casts ([casts-optimization.md](casts-optimization.md)) | Debug is usually right: `-Xbinary=genericSafeCasts=true` in `-opt` to confirm; `-Xdisable-phases=CastsOptimization`. |
| Wrong line numbers or variables in the debugger | debug info emission ([llvm-codegen.md](llvm-codegen.md)) | `debuggerTest` tag (`NativeLldbTestGenerated`, `testData/lldb`); `-Xadd-light-debug`, `-Xdebug-info-version`. |
| Crash inside the C++ runtime with no symbols | strip settings | Build with `kotlin.native.isNativeRuntimeDebugInfoEnabled=true` and compile with `-Xbinary=stripDebugInfoFromNativeLibs=false` ([HACKING.md](../../kotlin-native/HACKING.md#kotlinnative-runtime-specifics)). |

## Choosing the test to run

| What you changed | Run |
|------------------|-----|
| A lowering or codegen | `./gradlew :native:native.tests:codegen-box:test --tests "*FirNativeCodegenBoxTestGenerated*"` (shared box tests) and `./gradlew :native:native.tests:test --tests "*FirNativeCodegenLocalTestGenerated*"` (Native test data). Add `-Pkn.optimizationMode=OPT -Pkn.cacheMode=NO` to cover optimizations. |
| Lowering order / prerequisites | `./gradlew :kotlin-native:backend.native:test --tests "*NativeLoweringPrerequisitesTest"` (no dist needed) |
| First-stage lowerings, klib IR | `./gradlew :native:native.tests:klib-ir-inliner:test`, `:native:native.tests:klibTest` |
| Runtime C++ | `./gradlew :kotlin-native:runtime:hostRuntimeTests -Pgtest_filter='*Name*'`, then `:native:native.tests:gcTest` and `:native:native.tests:standaloneTest` |
| Compiler–runtime contract (layout, entry points) | runtime gtests **and** codegen tests; both sides must stay in sync |
| Caches | `./gradlew :native:native.tests:cachesTest`; codegen-box with `-Pkn.cacheMode=STATIC_EVERYWHERE` / `STATIC_PER_FILE_EVERYWHERE` |
| Partial linkage, klib resolution | `:native:native.tests:partialLinkageTest`, `:native:native.tests:klibTest`, `:native:native.tests:klib-compatibility:test` |
| cinterop | `./gradlew :native:native.tests:cinteropTest` |
| ObjC/C export (macOS) | `objcexport` and `cexport` tags; header generator tests in `native/objcexport-header-generator` |
| Debug info | `./gradlew :native:native.tests:debuggerTest` |
| LLVM-level codegen patterns | FileCheck tests in `native/native.tests/testData/codegen/fileCheck` (`// FILECHECK_STAGE:`, `CHECK-OPT` / `CHECK-DEBUG`) |

Tags, per-tag tasks, test properties and directives (`IGNORE_NATIVE`, `DISABLE_NATIVE`, `FREE_COMPILER_ARGS`,
`FILECHECK_STAGE`, …) are documented in [HACKING.md](../../kotlin-native/HACKING.md#testing) and defined in
[TestDirectives.kt](../../native/native.tests/testFixtures/org/jetbrains/kotlin/konan/test/blackbox/support/TestDirectives.kt).

## Flags worth memorizing

| Flag | Effect | Page |
|------|--------|------|
| `-Xlist-phases`, `-Xdisable-phases=A,B` | see / skip phases by name | [second-stage-pipeline.md](second-stage-pipeline.md) |
| `-Xphases-to-dump-before/after=X -Xdump-directory=D` | IR dumps (`-Xdump-fqname`; system property `org.jetbrains.kotlin.compiler.ir.dump.strategy=KotlinLike`) | [lowerings.md](lowerings.md) |
| `-Xverify-ir=error -Xphases-to-validate-after=all` | IR validation after every phase (what the tests do) | [lowerings.md](lowerings.md) |
| `-Xsave-llvm-ir-after=Codegen -Xsave-llvm-ir-directory=D`, `-Xprint-bitcode`, `-Xverify-bitcode` | LLVM IR | [llvm-codegen.md](llvm-codegen.md) |
| `-Xtemporary-files-dir=D` | keep `.bc`, `.o`, generated C stubs and adapters | [second-stage-pipeline.md](second-stage-pipeline.md) |
| `-Xbinary=runtimeAssertionsMode=panic` | runtime asserts (`ignore` default, `log`, `panic`) | [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| `-Xbinary=checkStateAtExternalCalls=true` | thread-state checker around external calls | [compiler-runtime-contract.md](compiler-runtime-contract.md) |
| `-Xruntime-logs=tag=level,…` | runtime logging | [runtime.md](runtime.md) |
| `-Xbinary=gc=…`, `gcSchedulerType=…`, `pagedAllocator=…`, `sanitizer=thread` | runtime variant selection for bisecting | [runtime.md](runtime.md) |
| `-Xprofile-phases`, `-Xreport-perf` | timing | [second-stage-pipeline.md](second-stage-pipeline.md) |
| `-Xpartial-linkage-loglevel=warning` | surface partial-linkage stubs | [klib-linking.md](klib-linking.md) |

Binary option names are the property names in
[BinaryOptions.kt](../../native/binary-options/src/main/kotlin/org/jetbrains/kotlin/config/nativeBinaryOptions/BinaryOptions.kt);
defaults are in `NativeSecondStageCompilationConfig.kt`.
