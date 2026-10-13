Configured authentication: Managed by agent

Now I'm examining the NativeCompilerArguments component in the compiler arguments description area to understand how native compiler options are defined and structured. This will help me grasp the current setup before making any adjustments related to native compilation.

I located the configuration mapping where Native compiler arguments are parsed and routed into compiler keys. I will inspect `SetupConfiguration.kt` and related setup classes to see how arguments are separated between KLIB generation and binary linking.

I identified `NativeConfigurationPhase.kt` where KLIB configuration updater is defined. Now I will open this file to see which arguments are specifically processed during the KLIB compilation phase.

I confirmed that Kotlin/Native explicitly distinguishes the stages via `isNativeSecondStage` based on the `-produce` flag. Now I will inspect `K2NativeCompilerArgumentsConfigurator.kt` to understand how argument configuration is split across phases.

I compared how Gradle configures arguments for KLIB compilation versus native linking tasks. Now I will trace how the Kotlin/Native compiler entrypoint invokes `setupFromArguments` and executes its backend pipeline phases.

I located the KLIB compilation pipeline in `NativeKlibCliPipeline.kt`. Now I will search for where this pipeline is invoked to see how the compiler driver alternates between KLIB compilation and linking.

I inspected `KonanDriver.kt` which drives the second stage (binary generation and cache building). Now I will locate the entry point that invokes `KonanDriver` to see how the overall CLI pipeline routes KLIB vs linking requests.

I confirmed how the Native compiler executes KLIB compilation versus linking. Now I will inspect the complete set of arguments defined in `NativeCompilerArguments.kt` to categorize each one accurately.

I started verifying each compiler argument against the KLIB compilation pipeline and backend linking driver. Now I will search if binary options or any related flags affect the KLIB compilation stage.

I identified phase usage for several groups of native arguments. Now I will search where incremental cache directory and caching keys are used across the compiler to ensure accurate categorization.

I verified the phase assignment for another batch of options including library packing and optimization flags. Now I will search where `overrideKonanProperties` is used to see if property overrides apply to KLIB compilation, linking, or both.

I analyzed options related to runtime logging, LLVM IR dumping, linker options, and target platform. Now I will check the usage of `temporaryFilesDir`, `testDumpOutputPath`, `verifyBitCode`, `verifyCompiler`, and `writeDependenciesOfProducedKlibTo`.

**Kotlin/Native Two-Stage Compilation Architecture**

In Kotlin/Native (`K2Native`), compilation is split into two distinct stages:
1. **KLIB Compilation Phase (First Stage):** Compiles Kotlin source files into an intermediate Kotlin Library (`.klib`) artifact containing Kotlin FIR/IR, metadata, and optional embedded native bitcode libraries. This is orchestrated by `NativeKlibCliPipeline` (`NativeConfigurationPhase`, `NativeKlibWritingPipelinePhase`).
2. **Linking / Binary Compilation Phase (Second Stage):** Consumes compiled `.klib` libraries, performs whole-program / monomorphization lowerings, generates machine code via LLVM, and links the final target executable, framework, dynamic/static library, or cache. This is orchestrated by `KonanDriver` and `SetupConfiguration.kt`.

The phase is primarily determined by `-produce` (`produce`):
- `-produce library` executes the **KLIB compilation phase**.
- `-produce program`, `dynamic`, `static`, `framework`, or `*_cache` executes the **linking phase** (or two-stage compilation where an intermediate KLIB is generated first and immediately fed into the linking phase).

---

**Classification of `K2NativeCompilerArguments`**

Below is the complete classification of all arguments defined directly in `K2NativeCompilerArguments`.

---

**1. Arguments Used Exclusively in the KLIB Compilation Phase**

These options specifically control `.klib` artifact structure, metadata generation, and manifest properties:

| Argument | CLI Flag | Description |
|---|---|---|
| `headerKlibPath` | `-Xheader-klib-path` | Path where a header KLIB containing only the public ABI should be produced. |
| `includeBinaries` | `-include-binary` / `-ib` | Packs external native static/dynamic binaries into the produced `.klib` file. |
| `manifestFile` | `-manifest` | Provides an additional manifest file whose properties are merged into the `.klib` manifest. |
| `manifestNativeTargets` | `-Xmanifest-native-targets` | Specifies the comma-separated target list written as the `native_targets` property in the `.klib` manifest. |
| `nopack` | `-nopack` | Leaves the generated KLIB unpacked as a directory instead of compressing it into a `.klib` zip archive. |
| `refinesPaths` | `-Xrefines-paths` | Specifies output directories for refined modules (modules whose `expect` declarations this module can actualize). |
| `writeDependenciesOfProducedKlibTo` | `-Xwrite-dependencies-of-produced-klib-to` | Writes a file containing the file paths of all KLIB dependencies resolved during KLIB creation. |

---

**2. Arguments That Apply to Both Phases**

These options configure common platform settings, module identity, dependency resolution, or diagnostics across both KLIB creation and binary linking:

| Argument | CLI Flag | Role in KLIB Phase vs. Linking Phase |
|---|---|---|
| `produce` | `-produce` / `-p` | Selects the compilation phase (`library` for KLIB creation; `program`, `framework`, etc., for linking). |
| `target` | `-target` | Sets the hardware target (e.g. `ios_arm64`, `linux_x64`). Determines platform headers/libraries for KLIB compilation and target machine codegen/linking for binaries. |
| `moduleName` | `-module-name` | Sets the module name recorded in the `.klib` manifest as well as the default base name of output binaries and exported symbols. |
| `outputName` | `-output` / `-o` | Sets the destination path for the produced `.klib` file or the destination binary (executable, framework, library). |
| `libraries` | `-library` / `-l` | Provides dependency `.klib` libraries for symbol resolution during frontend/KLIB compilation and for linking into the final binary. |
| `friendModules` | `-friend-modules` | Grants access to `internal` declarations; checked during frontend analysis in KLIB compilation and during backend IR inlining. |
| `nativeLibraries` | `-native-library` / `-nl` | Bitcode libraries (`.bc`); can be bundled into the KLIB artifact or linked directly into the binary during backend bitcode linking. |
| `nodefaultlibs` | `-no-default-libs` | Disables automatic resolution of standard platform libraries during both KLIB dependency resolution and binary linking. |
| `nostdlib` | `-nostdlib` | Suppresses linking against the Kotlin/Native standard library in both phases. |
| `konanDataDir` | `-Xkonan-data-dir` | Overrides the Kotlin/Native distribution directory (containing sysroots, LLVM, and platform dependencies) used in both phases. |
| `exportKDoc` | `-Xexport-kdoc` | Enables `LanguageFeature.ExportKDocDocumentationToKlib` to serialize KDoc into the KLIB metadata, and exports KDoc comments into generated Objective-C framework headers during framework linking. |
| `shortModuleName` | `-Xshort-module-name` | Written to the `.klib` manifest as `short_name` during KLIB compilation and read during linking when generating Objective-C framework headers. |
| `printIr` | `-Xprint-ir` | Prints Kotlin IR after IR phases (operates in both IR generation for KLIB and backend IR lowerings). |
| `printFiles` | `-Xprint-files` | Prints input files processed during KLIB or binary compilation. |

---

**3. Arguments Used Exclusively in the Linking / Binary Compilation Phase**

These options affect native code generation, LLVM optimization passes, garbage collection, memory management, Objective-C/C export, caching, and system linker invocations:

| Argument | CLI Flag | Description |
|---|---|---|
| `allocator` | `-Xallocator` | Selects runtime memory allocator (`std`, `mimalloc`, `custom`). |
| `autoCacheDir` | `-Xauto-cache-dir` | Path to directory where caches for auto-cacheable dependencies are placed. |
| `autoCacheableFrom` | `-Xauto-cache-from` | Root directories from which dependencies are cached automatically. |
| `backendThreads` | `-Xbackend-threads` | Number of worker threads for parallel file-level code generation in the backend. |
| `binaryOptions` | `-Xbinary` | Key-value options tuning native backend and runtime behavior. |
| `cacheDirectories` | `-Xcache-directory` | Paths to directories containing precompiled native binary caches. |
| `cachedLibraries` | `-Xcached-library` | Explicit mappings between a library and its precompiled cache archive. |
| `checkDependencies` | `-Xcheck-dependencies` | Checks and downloads missing target sysroots and toolchain dependencies. |
| `checkExternalCalls` | `-Xcheck-state-at-external-calls` | Injects thread state transition checks around external native C calls. |
| `clangOptions` | `-Xoverride-clang-options` | Flags passed directly to Clang when compiling native stubs and invoking the linker. |
| `compileFromBitcode` | `-Xcompile-from-bitcode` | Bypasses Kotlin frontend/backend and continues linking directly from an LLVM bitcode file. |
| `debug` | `-g` | Instructs the backend to emit full DWARF debug symbols. |
| `debugInfoFormatVersion` | `-Xdebug-info-version` | Sets DWARF debug format version (1 or 2). |
| `debugPrefixMap` | `-Xdebug-prefix-map` | Remaps source paths in DWARF debug symbols. |
| `dumpBuiltCachesTo` | `-Xdump-built-caches-to` | Path to dump the list of built cache archives. |
| `emitLazyObjCHeader` | `-Xemit-lazy-objc-header` | Path to emit a lazy Objective-C framework header. |
| `enableAssertions` | `-enable-assertions` / `-ea` | Controls whether runtime assertions (`assert(...)`) are generated into native code. |
| `exportedLibraries` | `-Xexport-library` | Specifies which dependency libraries should be included in the public C/Objective-C framework header. |
| `externalDependencies` | `-Xexternal-dependencies` | Path to the dependency graph file used by the cache/build system. |
| `filesToCache` | `-Xfile-to-cache` | Paths of specific files or libraries to compile into cache. |
| `frameworkImportHeaders` | `-Xframework-import-header` | Additional header `#import` statements added to the produced framework header. |
| `gc` | `-Xgc` | Selects the native runtime garbage collector (`noop`, `stms`, `cms`). |
| `generateDebugTrampolineString` | `-Xg-generate-debug-trampoline` | Emits trampolines to enhance breakpoint resolution in inlined code. |
| `generateTestRunner` | `-generate-test-runner` / `-tr` | Generates a synthetic test runner `main` entry point for unit test binaries. |
| `generateNoExitTestRunner` | `-generate-no-exit-test-runner` / `-trn` | Generates a unit test runner that returns instead of exiting the process. |
| `generateWorkerTestRunner` | `-generate-worker-test-runner` / `-trw` | Generates a worker test runner for unit tests. |
| `includes` | `-Xinclude` | Includes an intermediate KLIB directly into the binary compilation as root source modules. |
| `incrementalCacheDir` | `-Xic-cache-dir` | Directory where backend incremental compilation caches are stored. |
| `libraryToAddToCache` | `-Xadd-cache` | Library to add to a pre-built cache. |
| `lightDebugString` | `-Xadd-light-debug` | Adds light debug info for optimized builds (e.g. `.dSYM` on Apple targets). |
| `linkerArguments` | `-linker-options` / `-linkerOpts` | Passes space-separated flags to the platform linker. |
| `singleLinkerArguments` | `-linker-option` | Passes a single flag to the platform linker. |
| `listTargets` | `-list-targets` | Compiler driver action to list supported hardware targets. |
| `llvmLTOPasses` | `-Xllvm-lto-passes` | Custom LLVM pass pipeline for link-time optimization (LTO). |
| `llvmModulePasses` | `-Xllvm-module-passes` | Custom LLVM pass pipeline for module-level optimization. |
| `llvmVariant` | `-Xllvm-variant` | Selects LLVM distribution (`dev`, `user`, or absolute path). |
| `mainPackage` | `-entry` / `-e` | Custom qualified entry point function name for executable binaries. |
| `makePerFileCache` | `-Xmake-per-file-cache` | Produces per-file binary caches instead of monolithic library caches. |
| `memoryModel` | `-memory-model` | Runtime memory model configuration (`experimental`, `strict`). |
| `noObjcGenerics` | `-Xno-objc-generics` | Suppresses Objective-C generics in generated framework headers. |
| `nomain` | `-nomain` | Informs the linker that the `main` entry point is provided by external native libraries. |
| `omitFrameworkBinary` | `-Xomit-framework-binary` | Produces only the Objective-C framework headers and manifest without compiling the binary. |
| `optimization` | `-opt` | Enables LLVM code generation optimizations and native inlining. |
| `overrideKonanProperties` | `-Xoverride-konan-properties` | Overrides key-value pairs from `konan.properties` (e.g., sysroot paths, toolchain flags). |
| `preLinkCaches` | `-Xpre-link-caches` | Controls pre-linking of cache archives for faster link steps. |
| `printBitCode` | `-Xprint-bitcode` | Prints LLVM bitcode generated by the backend. |
| `propertyLazyInitialization` | `-Xir-property-lazy-initialization` | Enables lazy per-file initialization of top-level properties. |
| `runtimeFile` | `-Xruntime` | Overrides the path to the standard native `runtime.bc` bitcode library. |
| `runtimeLogs` | `-Xruntime-logs` | Configures internal native runtime logging tags and levels. |
| `saveDependenciesPath` | `-Xwrite-dependencies-to` | Writes backend dependency graph to the specified file. |
| `saveLlvmIrAfter` | `-Xsave-llvm-ir-after` | Saves LLVM IR after specified optimization phases. |
| `saveLlvmIrDirectory` | `-Xsave-llvm-ir-directory` | Directory to save LLVM IR dumps to. |
| `serializedDependencies` | `-Xread-dependencies-from` | Reads pre-serialized dependency graph for linking. |
| `staticFramework` | `-Xstatic-framework` | Produces a static framework archive (`.a`) instead of dynamic (`.dylib`). |
| `temporaryFilesDir` | `-Xtemporary-files-dir` | Directory for intermediate object files and compiler artifacts. |
| `testDumpOutputPath` | `-Xdump-tests-to` | Dumps the list of all available unit tests found during linking. |
| `verifyBitCode` | `-Xverify-bitcode` | Invokes LLVM bitcode verification after each function and module. |
| `verifyCompiler` | `-Xverify-compiler` | Runs backend compiler invariant verification. |

---

**Inherited Common Arguments**

`K2NativeCompilerArguments` inherits from `CommonKlibBasedCompilerArguments`:

- **KLIB Compilation Phase:**
  - `relativePathBases` (`-Xklib-relative-path-bases`): Normalizes source file paths stored inside the `.klib`.
  - `duplicatedUniqueNameStrategy` (`-Xklib-duplicated-unique-name-strategy`): Controls duplicate library naming validation.
  - `customKlibAbiVersion` (`-Xcustom-klib-abi-version`): Custom ABI version written to the `.klib` manifest.
  - `enableSignatureClashChecks` (`-Xklib-enable-signature-clash-checks`): Validates mangled signature uniqueness during serialization.
  - Frontend arguments (`languageVersion`, `apiVersion`, `optIn`, `pluginClasspaths`, `pluginOptions`): Configure parsing, type checking, and compiler plugins during KLIB creation.
- **Linking Phase:**
  - `partialLinkageMode` (`-Xpartial-linkage`): Configures partial linkage engine when linking binary artifacts against evolved KLIB dependencies.
- **Both Phases:**
  - `klibZipFileAccessorCacheLimit` (`-Xklib-zip-file-accessor-cache-limit`): Limits open zip file handlers when reading `.klib` files.
  - `skipLibrarySpecialCompatibilityChecks` (`-Xklib-skip-library-special-compatibility-checks`): Controls compatibility verification checks for pre-release library versions.
