# Kotlin/Native: compatibility surfaces and migration

What has to stay compatible with what in Kotlin/Native, which mechanism enforces each contract (a version number, a check,
a test) or merely assumes it, and how defaults and formats have been migrated so far. Read this before deciding whether a
change is safe across compiler versions, klibs, caches, platform libraries, targets, toolchains and user builds. The
mechanics of each subsystem are on their own pages and are only linked here: [klib-linking.md](klib-linking.md),
[caches.md](caches.md), [compiler-runtime-contract.md](compiler-runtime-contract.md), [cinterop.md](cinterop.md),
[export.md](export.md), [lowerings.md](lowerings.md); the user-facing model is in [compilation-model.md](compilation-model.md).

Paths are relative to this file. `BACKEND` means
`../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan`, `UTIL_KLIB` means
`../../compiler/util-klib/src/org/jetbrains/kotlin/library`, `KGP` means
`../../libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle`.

## The surfaces at a glance

| Surface | Must stay compatible | Enforced by | Strength |
|---------|----------------------|-------------|----------|
| Klib ABI | klibs produced by compiler A, consumed by compiler B | `abi_version` window `[FIRST_SUPPORTED, CURRENT]` in `KlibLoader`; partial linkage for declaration-level drift | version check + runtime stubs |
| Stdlib vs compiler | distribution stdlib and the compiler that loads it | `KonanLibrarySpecialCompatibilityChecker` (stdlib only) | hard error |
| Platform libs vs compiler | cinterop klibs in the distribution and the compiler | nothing beyond the ABI window and `native_targets`; same-build assumption | assumed |
| Compiler caches | cached machine code and the consumer compilation | flavor directory name; `compiler.fingerprint` for IC caches only | partial |
| Runtime / binary options | option names and values across releases | `BinaryOptions` registry; enum `deprecatedWithReplacement`; ad-hoc checks in `SetupConfiguration.kt` | warnings, some errors |
| Targets and toolchains | target names, OS minimums, LLVM/Xcode versions | `KonanTarget.deprecatedTargets`, `softDeprecatedTargets`, `konan.properties`, Xcode version checks | mixed |
| Exported API | ObjC/C headers and Swift USRs generated for the same Kotlin code | golden header tests; name-collision options; no cross-version test | assumed |
| CLI and KGP | argument names, KGP ↔ compiler version | `compiler/arguments` lifecycle metadata and stable-release tests; KGP downloads its own version | metadata + convention |

## 1. Klib ABI

### Versions carried by a klib manifest

The manifest keys are constants in [`KotlinLibrary.kt`](../../compiler/util-klib/src/org/jetbrains/kotlin/library/KotlinLibrary.kt)
and are written/read by `writeKonanLibraryVersioning`/`readKonanLibraryVersioning`
([`KotlinLibraryVersioning.kt`](../../compiler/util-klib/src/org/jetbrains/kotlin/library/KotlinLibraryVersioning.kt)).

| Property | Written as | Read and checked by |
|----------|-----------|---------------------|
| `abi_version` | `klibAbiVersionForManifest()` ([`klibAbiVersionUtils.kt`](../../compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/klibAbiVersionUtils.kt)): `-Xklib-abi-version` (tests only; the flag's own description says it does not change the ABI) or `KlibAbiCompatibilityLevel.toAbiVersionForManifest()` | `KlibLoader` window, see below; `LibrarySpecialCompatibilityChecker` for stdlib |
| `compiler_version` | `KotlinCompilerVersion` of the producer | nothing in the compiler (informational) |
| `metadata_version` | `languageVersion.toMetadataVersion()` ([`serializeModuleIntoKlib.kt`](../../compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/serializeModuleIntoKlib.kt)) | `getIncompatibility(configuration.metadataVersion)` attached to every package fragment in [`K1KlibMetadataModuleDescriptorFactoryImpl.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/K1KlibMetadataModuleDescriptorFactoryImpl.kt). Unverified: how the incompatibility surfaces to the user in the second stage. |
| `ir_signature_versions` | `KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS` = `{1, 2}` ([`KotlinIrSignatureVersion.kt`](../../compiler/util-klib/src/org/jetbrains/kotlin/library/KotlinIrSignatureVersion.kt)); absent → `V1` | `AllKnownIrSignatureVersionsAreSupported` in `compiler/util-klib-abi`. V2 adds a human-readable description; linkage uses only the numeric id, so V2 is compatible with V1 |
| `native_targets` | the `-target` of the producer | [`KlibPlatformChecker.Native`](../../compiler/util-klib/src/org/jetbrains/kotlin/library/loader/KlibPlatformChecker.kt): `builtins_platform` must be NATIVE; `commonizer_native_targets` or `native_targets` must contain the target; an empty list passes (metadata klibs) |
| `depends` | used-library computation in the first stage ([klib-linking.md](klib-linking.md)) | no reader found in the compiler (`KlibDAG` uses signature indices); only [`NativeKlibWriterUtils.kt`](../../native/utils/src/org/jetbrains/kotlin/konan/library/writer/NativeKlibWriterUtils.kt) merges it. Hypothesis: consumed by KGP/commonizer only |
| `interop`, `ir_provider`, `package` | cinterop tool | `checkForUnknownIrProviders` in `loadNativeKlibs` accepts only none or `kotlin.native.cinterop`; `KonanInteropModuleDeserializer` |
| `header` | `-Xheader-klib-path` | deserialization strategy `WITH_INLINE_BODIES` ([klib-linking.md](klib-linking.md)) |
| `new_companion_initialization` | first stage | read per library by `ObjectClassLowering`/`StaticInitializersLowering` and stored in IC cache metadata (`staleCacheReason`, [caches.md](caches.md)): the model for a per-klib behaviour switch |
| `language_features`, `poisoning_language_features`, `metadata_flags` | `writeKlib` | not traced on this page |

The ABI check in [`KlibLoader.kt`](../../compiler/util-klib/src/org/jetbrains/kotlin/library/loader/KlibLoader.kt) runs
only when `library.hasAbi` (`ir != null || irProviderName != null`), so metadata-only klibs bypass it entirely.

### The acceptance window and what it means for users

[`KotlinAbiVersion`](../../compiler/util-klib/src/org/jetbrains/kotlin/library/KotlinAbiVersion.kt) defines
`CURRENT = 2.5.0` ("since 2.2.0 aligned with the Kotlin version"), `FIRST_SUPPORTED = 1.6.0` and
`FIRST_SUPPORTED_COMPILER_VERSION = "1.6.0"`, the oldest compiler whose klibs are accepted. `loadNativeKlibs`
([`loadNativeKlibs.kt`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/loadNativeKlibs.kt))
passes both to the loader (`minPermittedAbiVersion`, `maxPermittedAbiVersion`), so a user klib compiled by any 1.6+ compiler
is *accepted*; a klib from a newer compiler is rejected with "Incompatible ABI version". The window is therefore
backward-only by default. Forward export exists as an opt-in: `LanguageFeature.ExportKlibToOlderAbiVersion` (KT-76131,
`forcesPreReleaseBinaries`) makes `setupKlibAbiCompatibilityLevel`
([`klibArguments.kt`](../../compiler/cli/src/org/jetbrains/kotlin/cli/common/klibArguments.kt)) map the language version to
[`KlibAbiCompatibilityLevel`](../../compiler/config/src/org/jetbrains/kotlin/config/KlibAbiCompatibilityLevel.kt)
(`2.4 → ABI_LEVEL_2_4`, `2.5 → ABI_LEVEL_2_5`; `check(size == entries.size)` forces the map to be updated on every new level).
Without the feature the level is always `LATEST_STABLE`.

Accepted does not mean linkable: inside the window the compiler relies on the IR reader tolerating older protos and on
partial linkage for everything declaration-level (missing symbols, changed classifiers), see
[klib-linking.md](klib-linking.md#partial-linkage). PL cannot be disabled from the CLI (only its log level), which makes it
the de-facto compatibility safety net.

### Native-specific checks

[`KonanLibrarySpecialCompatibilityChecker`](../../compiler/ir/serialization.native/src/org/jetbrains/kotlin/backend/konan/serialization/KonanLibrarySpecialCompatibilityChecker.kt)
specialises the common
[`LibrarySpecialCompatibilityChecker`](../../compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/diagnostics/LibrarySpecialCompatibilityChecker.kt)
(KT-68322). `toCheckedLibrary` returns non-null **only for `isNativeStdlib`**; platform libraries and user klibs are not
checked. For the stdlib it reports `KLIB_LOADING_ERROR` when (a) `abi_version.major.minor` differs from the compiler's ABI
level (a dev compiler also accepts the previous level, because stdlib is built by the bootstrap compiler), or (b) at
`LATEST_STABLE` level the library version is older than the compiler. Since the K/N stdlib has no `META-INF/MANIFEST.MF`,
the version is read from `<dist>/konan/konan.properties` `compilerVersion`, a token `@compilerVersion@` replaced at build
time by [`PrepareKonanProperties.kt`](../../kotlin-native/build-tools/src/main/kotlin/org/jetbrains/kotlin/nativeDistribution/PrepareKonanProperties.kt).
`-Xskip-library-special-compatibility-checks` disables the check.
[`KlibNativeManifestTransformer`](../../native/utils/src/org/jetbrains/kotlin/konan/library/KlibNativeManifestTransformer.kt)
is not a check: it substitutes `<prop>.<target>` manifest entries for the current target before the manifest is read.

### What the ABI actually is

Three things make up what a consumer must understand, and they are versioned differently:

1. **The IR protobuf and (de)serialization logic** (`compiler/ir/serialization.common`, `KotlinIr.proto`). The KDoc of
   `KotlinAbiVersion` says it "must be bumped when incompatible changes are made in `KotlinIr.proto`" or in the
   serialization logic, following `BinaryVersion` rules. Since 2.2.0 it is bumped once per release together with the
   language version, so a format change needs no separate bump but must land before the release that bumps.
2. **Signatures** (`IdSignature`, `ir_signature_versions`): the mangler output. A mangling change is a silent ABI break
   (consumers see unbound symbols → PL stubs) unless `ir_signature_versions` changes.
3. **Pre-serialization lowerings**: a klib stores IR *after* `nativeLoweringsOfTheFirstPhase`
   ([`NativeFirstPhaseLoweringPhases.kt`](../../compiler/ir/ir.inline/src/org/jetbrains/kotlin/ir/inline/konan/NativeFirstPhaseLoweringPhases.kt)).
   Each lowering whose output changes the klib shape is gated by a `LanguageFeature`, so old klibs keep the old shape and
   the second stage must accept both ([lowerings.md](lowerings.md)):

| Gate (defined in [`LanguageVersionSettings.kt`](../../core/language.version-settings/src/org/jetbrains/kotlin/config/LanguageVersionSettings.kt)) | Since | Effect in the first stage |
|------|-------|---------------------------|
| `IrRichCallableReferencesInKlibs` (KT-72734) | 2.3 | `UpgradeCallableReferences`: rich function/property references in klib IR (one cause of the 1.201.0 → 2.2.0 ABI bump) |
| `IrIntraModuleInlinerBeforeKlibSerialization` (KT-79717) | 2.4 | `NativeAssertionWrapperLowering` plus the common pre-serialization inliner |
| `IrCrossModuleInlinerBeforeKlibSerialization` (KT-71896, `forcesPreReleaseBinaries`) | 2.5 | cross-module inlining before serialization |
| `NativeTestProcessorBeforeSerialization` (KT-83807) | 2.4 | `TestProcessor` runs in the first stage ([`PreSerializingLowerings.kt`](../../compiler/cli/cli-native-klib/src/main/kotlin/org/jetbrains/kotlin/native/PreSerializingLowerings.kt)) |
| `ExportKDocDocumentationToKlib` (KT-83921) | none; enabled by `-Xexport-kdoc` through `@Enables` → generated [`ConfigureNativeLanguageFeatures.kt`](../../compiler/cli/cli-base/gen/org/jetbrains/kotlin/cli/common/arguments/ConfigureNativeLanguageFeatures.kt) | KDoc serialized into the klib; read by `shouldExportKDoc` in [`ConfigChecks.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/ConfigChecks.kt) |

The common `EnumEntries` feature is the precedent for what happens when the two stages disagree:
`UnexpectedUnboundIrSymbols` ([`KotlinIrLinkerIssues.kt`](../../compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/linkage/issues/KotlinIrLinkerIssues.kt))
special-cases signatures that look like `entries` to hint at the mismatch.

### Bump history (evidence for how migrations were done)

[`KotlinAbiVersionBumpHistory.md`](../../compiler/util-klib/KotlinAbiVersionBumpHistory.md) records every bump with the
commit. The instructive ones: 1.4.1 → 1.4.2 (fake overrides no longer serialized; only a patch bump because readers already
tolerated their absence), 1.7.0 → 1.8.0 (an incompatible proto change shipped in 1.8.0 but bumped only in 1.9.0, KT-55808;
it was survivable because the error was human-readable, KT-53620), 1.8.0 → 1.201.0 (an **LLVM** upgrade, KT-49279: cinterop
klibs carry bitcode that the new LLVM could not read, so a toolchain change became a klib ABI change), 1.201.0 → 2.2.0 (new
IR nodes `IrRichFunctionReference`, `IrReturnableBlock`, deduplicated `IrFileEntry`). Since 2.2.0 the bump is part of the
language-version bump commit.

### Tests

- **klib-compatibility** ([`build.gradle.kts`](../../native/native.tests/klib-compatibility/build.gradle.kts)): downloads
  `org.jetbrains.kotlin:kotlin-native-prebuilt:<version>:<host>@tar.gz` into `~/.konan/kotlin-native-prebuilt-releases` and
  runs one stage with it. `testCustomFirstStage_<v>` (v = 1.9.20, 2.0.0, …, 2.4.0): the *old* compiler produces the klib, the
  current one links it — backward compatibility. `testCustomSecondStage_<v>` (2.4.0, "previous and latest"): the current
  first stage with `+ExportKlibToOlderAbiVersion` produces the klib, the old compiler links it — forward compatibility
  (`setupCustomLVForKlibForwardCompatibilityTest` in [`utils.kt`](../../compiler/tests-common-new/testFixtures/org/jetbrains/kotlin/test/klib/utils.kt)
  aborts if the gap is more than one language version). The two directions are separate Gradle tasks because the
  cinterop/LLVM shared libraries of the two distributions cannot be mixed in one JVM. Test data: `compiler/testData/codegen/box`,
  `boxInline`, `native/native.tests/testData/codegen` ([`GenerateNativeKlibCompatibilityTests.kt`](../../native/native.tests/klib-compatibility/testFixtures/org/jetbrains/kotlin/generators/tests/GenerateNativeKlibCompatibilityTests.kt)).
  Expected breakages are muted per version with `IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE` /
  `IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_STAGE` ([`CustomKlibCompilerTestDirectives.kt`](../../compiler/tests-common-new/testFixtures/org/jetbrains/kotlin/test/klib/CustomKlibCompilerTestDirectives.kt));
  the sanity suites ([`CustomNativeCompilerSecondStageSanity.kt`](../../native/native.tests/klib-compatibility/tests/org/jetbrains/kotlin/konan/test/klib/CustomNativeCompilerSecondStageSanity.kt))
  fail when a mute is no longer needed. Skipped on Windows.
- **ABI dumps**: `compiler/util-klib-abi` ([`ReadMe.md`](../../compiler/util-klib-abi/ReadMe.md)) renders the public ABI
  with signatures; the `DUMP_KLIB_ABI` directive ([`KlibAbiDumpDirectives.kt`](../../compiler/tests-common-new/testFixtures/org/jetbrains/kotlin/test/directives/KlibAbiDumpDirectives.kt),
  handler [`KlibAbiDumpHandler.kt`](../../compiler/tests-common-new/testFixtures/org/jetbrains/kotlin/test/backend/handlers/KlibAbiDumpHandler.kt))
  and `CHECK_SAME_ABI_AFTER_INLINING` keep golden dumps under [`compiler/testData/klib/dump-abi`](../../compiler/testData/klib/dump-abi);
  Native runs them in `:native:native.tests:klib-ir-inliner:test`. A changed dump is the earliest signal of an ABI change.
- Partial linkage and signature-clash tests: see [klib-linking.md](klib-linking.md#tests).

## 2. Stdlib and platform libraries vs the compiler

The stdlib is checked for the same version (section 1). Platform libraries are built in the same Gradle build by
`KonanInteropTask` with the distribution's compiler ([`platformLibs/build.gradle.kts`](../../kotlin-native/platformLibs/build.gradle.kts)),
so same-version is a build invariant, not a runtime check: a platform klib passes only the ABI window and
`native_targets`. Their consumer contract is the cinterop stub shape: `CInteropKlibMetadata2IRTransformer` in
`native/cinterop.deserialization` rebuilds IR from metadata by matching `kotlinx.cinterop.internal.*` annotations by FQ
name, so a change in `StubIrBuilder` output needs a matching change there ([cinterop.md](cinterop.md), "Invariants").
`CCallMode` must be satisfiable on both sides: platform libs are built with `-Xccall-mode indirect`, hence
`-Xbinary=cCallMode=Direct` fails on them, and `cCallMode` is part of the cache flavor. Prebuilt platform-library caches ship
inside the distribution (`cacheableTargets.<host>` in `konan.properties`), so they are replaced together with the compiler.

## 3. Compiler caches

A cache is machine code plus side info that stands in for IR ([caches.md](caches.md)). Its compatibility key is the flavor
string built in [`NativeSecondStageCompilationConfig.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/NativeSecondStageCompilationConfig.kt):

| Part | Contents (appended only when different from the default) |
|------|-----------------------------------------------------------|
| `appendCommonCacheFlavor` | target, `-g`, `-opt`, `STATIC`, `perFileCacheForStdlib`, `propertyLazyInitialization`, `sanitizer`, `checkStateAtExternalCalls`, `stackProtector`, `genericSafeCasts`, `macabi`, `cCallMode`, `latin1Strings`, `assertsEnabled` |
| `systemCacheFlavorString` (distribution libs) | common + `-system`, `runtime_debug`, `allocator`, `gc`, `gcSchedulerType`, `runtimeAssertsMode`, `disableMmap`, `gcMarkSingleThreaded`, `fixedBlockPageSize`, `pagedAllocator`, `concurrentWeakSweep`, `with_crash_dumps`, `runtime_logs_enabled` |
| `userCacheFlavorString` (auto and IC caches) | common + `-user-pl`, `swift_export` |

Two consequences. First, **user caches do not encode GC, scheduler or allocator**: that is sound only because generated
code reaches the memory manager through runtime calls (`UpdateHeapRef` and friends, [llvm-codegen.md](llvm-codegen.md)) and
`gc`/`gcSchedulerType` only select runtime bitcode modules ([`CompilerOutput.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/CompilerOutput.kt)).
Any codegen that starts depending on the GC must be added to the user flavor. Second, the flavor does not contain the
compiler version. System caches live inside the distribution, so a new compiler brings new caches; IC caches store
`compiler.fingerprint` (`<dist>/konan/compiler.fingerprint`, [`DistributionUtils.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/util/DistributionUtils.kt))
and are rebuilt by `staleCacheReason` in [`CacheBuilder.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/CacheBuilder.kt);
auto caches are keyed by library fingerprints only. What invalidates a cache semantically: any change in runtime constants or
object layout ([compiler-runtime-contract.md](compiler-runtime-contract.md)), in `ClassLayoutBuilder` field rules
(`class_fields`), in the klib IR protobuf (`inline_bodies` are indices into it), or in what the mangler considers exported.
The comment above `systemCacheFlavorString` states the rule: every new flavor entry must also be forwarded in
`setupCommonOptionsForCaches` ([`SetupConfiguration.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/SetupConfiguration.kt)),
and the test infrastructure duplicates the flavor in `CacheMode.computeCacheDirName`.

Migration example — **release binary caches**. Caches were a debug-only feature because whole-program optimizations
(devirtualization, GHA) are unsound against opaque machine code. Commit `7829d2f682d0` (2026-06-23, "Support compiler
caches in -opt mode", KT-80373; listed under 2.4.21 in [`ChangeLog.md`](../../ChangeLog.md)) added the opt-in binary option
`enableReleaseBinaryCache`: `ignoreCacheReason` is "with global optimizations" unless it is set, and it is forwarded to
spawned cache builds. KGP exposes it as the internal property `kotlin.internal.native.enableReleaseBinaryCache`
([caches.md](caches.md)). The pattern — new behaviour behind a binary option that is off by default, test infrastructure
turns it on, KGP exposes it as internal — is how cache-affecting defaults are migrated.

## 4. Runtime and binary options

[`BinaryOptions`](../../native/binary-options/src/main/kotlin/org/jetbrains/kotlin/config/nativeBinaryOptions/BinaryOptions.kt)
is a registry whose property *names are the user interface* (header comment): `-Xbinary=name=value` and the KGP DSL use them
verbatim. [`parsing.kt`](../../native/binary-options/src/main/kotlin/org/jetbrains/kotlin/config/nativeBinaryOptions/parsing.kt)
reports an unknown name or value as a **warning** and drops it, so a typo or an option removed in a newer compiler never
fails a build. Binary options are not part of `compiler/arguments` and have no lifecycle metadata (section 7).

Defaults follow one pattern in `NativeSecondStageCompilationConfig`: `private val defaultX get() = …` and
`val x get() = configuration.get(BinaryOptions.x) ?: defaultX`; the flavor appends `x` when it differs from the default,
so changing a default silently re-keys every system cache directory. Deprecations are expressed in three ways:

| Option | Mechanism | Evidence |
|--------|-----------|----------|
| `gcSchedulerType=DISABLED/WITH_TIMER/ON_SAFE_POINTS` | enum entries carry `deprecatedWithReplacement` (MANUAL/ADAPTIVE/ADAPTIVE) in [`GCSchedulerType.kt`](../../native/binary-options/src/main/kotlin/org/jetbrains/kotlin/config/nativeBinaryOptions/GCSchedulerType.kt); hidden from the help text via `hideValue`; `gcSchedulerType` in the config warns and substitutes; `CompilerOutput.kt` throws if a deprecated value survives | `7815eec7c10e` 2023-06-09 "Rework GC switches with better naming" |
| `memoryModel`, `-memory-model` | `SetupConfiguration.kt`: any value but `experimental` → `KONAN_ARGUMENT_ERROR` "Legacy MM is deprecated and no longer works"; `experimental` → strong warning | the option still exists only to produce the message |
| `freezing` | same file: any value but `Disabled` → error pointing to the migration guide; `Disabled` → strong warning | since 1.7.20 per the message |
| `gc` | default is `GC.CONCURRENT_MARK_AND_SWEEP` (`defaultGC`); the four values keep short aliases (`cms`, `pmcs`, `stwms`, `noop`) | `0851ea86878b` 2025-12-16 "Experiment with enabling CMS GC by default" (KT-82635; message: opt-in since 2.0.20, Swift export default earlier: `dcc255caf100`, reverted `0113ed4171b0`, fixed `ab350bfdbbc1`) |
| allocator | not a binary option: `-Xallocator=std|mimalloc|custom` (CLI), default `AllocationMode.CUSTOM`; enters the system flavor as `-allocator<mode>` | `da1fde24777e` 2023-05-11 "Enable custom allocator by default" (KT-55364) |

How a default is changed, concretely: edit `defaultX` (one place), check that the runtime module selection in
`CompilerOutput.kt` and the mirrored constants in the runtime still agree ([compiler-runtime-contract.md](compiler-runtime-contract.md)),
run the test matrix with `-Pkn.gcType=`/`-Pkn.gcScheduler=`, and expect the distribution cache directory names to change.

KGP passes options through three doors that end in the same `-Xbinary=name=value`
([`KotlinNativeCompilerArgBuilder.kt`](../../libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/tasks/KotlinNativeCompilerArgBuilder.kt)):
the `binaryOption(name, value)` DSL on a binary ([`NativeBinaries.kt`](../../libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/NativeBinaries.kt),
with a `TODO: report if $name is unknown?`), project-wide `kotlin.native.binary.<name>=<value>` Gradle properties
(`nativeBinaryOptions` in [`PropertiesProvider.kt`](../../libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/PropertiesProvider.kt),
merged first so the DSL wins in [`KotlinNativeLink.kt`](../../libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/tasks/KotlinNativeLink.kt)),
and raw `freeCompilerArgs`. The runtime also has one environment contract that is documented API:
`KOTLIN_NATIVE_AVAILABLE_PROCESSORS` overrides `Platform.getAvailableProcessors()`
([`Platform.kt`](../../kotlin-native/runtime/src/main/kotlin/kotlin/native/Platform.kt),
`Konan_Platform_getAvailableProcessorsEnv` in [`Runtime.cpp`](../../kotlin-native/runtime/src/main/cpp/Runtime.cpp)); a
non-positive value throws `IllegalStateException`.

## 5. Targets and toolchains

**Target names** are the objects of the sealed class [`KonanTarget`](../../native/utils/src/org/jetbrains/kotlin/konan/target/KonanTarget.kt)
(`predefinedTargets`). Deprecation has two lists there: `deprecatedTargets` (linux_arm32_hfp, watchos_x64, tvos_x64,
macos_x64, watchos_arm32, all android) and `toleratedDeprecatedTargets` (linux_arm32_hfp, android). KGP turns
`deprecatedTargets − toleratedDeprecatedTargets` into an **error** (`DeprecatedKotlinNativeTargetsChecker`,
[`KotlinToolingDiagnostics.kt`](../../libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/diagnostics/KotlinToolingDiagnostics.kt)
"removed Kotlin/Native targets") and a deprecated *host* into a warning. The compiler keeps its own, smaller
`softDeprecatedTargets` (linux_arm32_hfp, macos_x64, tvos_x64, watchos_x64) in
[`KonanDriver.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/KonanDriver.kt)
and emits a strong warning pointing to `https://kotl.in/native-targets-tiers` for every non-`LIBRARY` output. The two lists are
not derived from each other. Which targets a host may build is `HostManager.enabledByHost`
([`HostManager.kt`](../../native/utils/src/org/jetbrains/kotlin/konan/target/HostManager.kt)).

**Per-target toolchain** is [`konan.properties`](../../kotlin-native/konan/konan.properties), read through
[`Configurables.kt`](../../native/utils/src/org/jetbrains/kotlin/konan/target/Configurables.kt) /
[`KonanProperties.kt`](../../native/utils/src/org/jetbrains/kotlin/konan/target/KonanProperties.kt): `llvmHome.<host>`,
`targetToolchain.<host>-<target>`, `targetSysRoot.<target>`, `dependencies.<target>` (what to download), `targetTriple`,
`clang*Flags`, `osVersionMin.<target> = $minVersion.<family>`, `cacheableTargets.<host>`, and the global
`dependenciesUrl`, `airplaneMode`, `minimalXcodeVersion`, `xcodeVersion`/`xcodeBuild` (provisioned Xcode). Any key can be
overridden per invocation with `-Xoverride-konan-properties=k=v;…`. Dependencies are downloaded by
[`DependencyProcessor.kt`](../../native/utils/src/org/jetbrains/kotlin/konan/util/DependencyProcessor.kt) into
`$KONAN_DATA_DIR/dependencies` (`KONAN_DATA_DIR` env or `~/.konan`, [`DependencyDirectories.kt`](../../native/utils/src/org/jetbrains/kotlin/konan/util/DependencyDirectories.kt);
`-Xkonan-data-dir` for the compiler, `konan.data.dir` properties for Gradle).

**Minimum OS versions** are the four `minVersion.*` values (ios 15.0, macos 12.0, tvos 15.0, watchos 8.0) consumed by
`ClangArgs` (the triple), `Linker` (`-*_version_min`), `BitcodeCompiler` and `InfoPListBuilder` (`MinimumOSVersion`). Raising
one changes every produced binary and framework plist; history: `9daa40d2ed21` moved to `platform_version`, and
`c8a07ae1565a` deliberately kept `osVersionMinFlagLd` "for compatibility purposes".

**Xcode**: at compile time `Apple.kt` (`currentXcodeCheckingVersion`) fails below `minimalXcodeVersion=15.4` unless
`ignoreXcodeVersionCheck=true` ([`Apple.kt`](../../native/utils/src/org/jetbrains/kotlin/konan/target/Apple.kt)). At build
time `xcodeMajorVersion=27` in [`gradle.properties`](../../gradle.properties) is enforced by `PlatformInfo.checkXcodeVersion`
([`PlatformInfo.kt`](../../kotlin-native/build-tools/src/main/kotlin/org/jetbrains/kotlin/PlatformInfo.kt)) only when
`-PcheckXcodeVersion=true` is passed and the internal server is unavailable.

**LLVM**: the version is `kotlin.native.llvm.default.<host>.version=21` in `gradle.properties`, with `kotlin.native.llvm`
selecting `default`, `next` (the migration slot) or a local path ([BUILDING_LLVM.md](../../kotlin-native/BUILDING_LLVM.md)).
The compiler selects a distribution at run time with `-Xllvm-variant=user|dev|<path>` (`LLVM_VARIANT` in
`SetupConfiguration.kt`, applied as a `konan.properties` override in `NativeSecondStageCompilationConfig`). An LLVM upgrade is a
compatibility event on three surfaces: cinterop klib bitcode (the 1.201.0 ABI bump), runtime bitcode, and the
`libllvmext` passes ([llvm-codegen.md](llvm-codegen.md)).

## 6. Exported API stability

ObjC/Swift names are computed by `ObjCExportNamerImpl` from the whole exported world; collisions are resolved by appending
`_` and surfaced only on request: `-Xbinary=objcExportReportNameCollisions=true` (warning) or
`objcExportErrorOnNameCollisions=true` (error), wired in [`ObjCExport.kt`](../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/objcexport/ObjCExport.kt)
as `ObjCExportNameCollisionMode`. Adding a declaration can therefore rename an unrelated one in the header; nothing
compares headers across compiler versions, only golden headers for fixed inputs (`testData/headers`,
[export.md](export.md)). `-Xexport-kdoc` is a `LanguageFeature`-backed switch (section 1) and changes header content, not
names. C export names come from `scopeUniqueName` mangling with the same caveats. Swift export USRs are a Swift-toolchain
artifact; [testing-usr-stability.md](../swift-export/testing-usr-stability.md) explains why a USR change is not
automatically a regression and how the tests cope.

## 7. CLI and KGP

Compiler arguments have one source of truth, [`compiler/arguments`](../../compiler/arguments/Readme.md): every argument
has `lifecycle(introducedVersion, stabilizedVersion?, deprecatedVersion?, removedVersion?)` over `KotlinReleaseVersion`
([`KotlinReleaseVersionLifecycle.kt`](../../compiler/arguments/src/org/jetbrains/kotlin/arguments/dsl/base/KotlinReleaseVersionLifecycle.kt))
and an optional `deprecatedMessage`. The rule in [`code-rules.md`](../../compiler/arguments/code-rules.md) is "set
`removedVersion` instead of removing": removed Native flags move to
[`removedNativeCompilerArguments.kt`](../../compiler/arguments/src/org/jetbrains/kotlin/arguments/description/removed/removedNativeCompilerArguments.kt)
(e.g. `-Xg0`, deprecated 1.5.20, removed 2.4.20). A current example is `-trw` in
[`NativeCompilerArguments.kt`](../../compiler/arguments/src/org/jetbrains/kotlin/arguments/description/NativeCompilerArguments.kt)
(`deprecatedVersion = v2_5_0`, message "Use '-generate-test-runner'"). `stableReleaseTests`
(`CurrentArgumentsConsistencyTest`, `DeprecatedArgumentsConsistencyTest`) compare the description against published
releases, so a renamed or retyped argument fails the build. `-Xbinary=` is a single argument from this model's point of
view; the option names inside it have no lifecycle and no stability test (section 4), which is the trade-off behind "add a
binary option, not a flag". Native-specific plumbing lives in
[`K2NativeCompilerArgumentsConfigurator`](../../compiler/cli/cli-base/src/org/jetbrains/kotlin/cli/common/arguments/K2NativeCompilerArgumentsConfigurator.kt)
(analysis flags, `configureNativeLanguageFeatures`, `isSecondStage`).

KGP and the compiler are coupled by version: `NativeCompilerDownloader`
([`NativeCompilerDownloader.kt`](../../libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/NativeCompilerDownloader.kt))
resolves `org.jetbrains.kotlin:kotlin-native[-prebuilt]:<version>:<os>` where `<version>` is `kotlin.native.version`
([`NativeProperties.kt`](../../libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/internal/properties/NativeProperties.kt);
`org.jetbrains.kotlin.native.version` is the deprecated spelling) defaulting to KGP's own version from `project.properties`;
`kotlin.native.home` points at a local distribution instead, and `kotlin.native.distribution.type`/`baseDownloadUrl`
tune the download. KGP never reads compiler metadata back; it builds the argument list from its own
`K2NativeCompilerArguments`. Hypothesis: with a mismatched `kotlin.native.version` the failure mode is an unknown-argument
error or a silently ignored option from the newer side; not tested in this tree.

## 8. The pattern for a language-version-gated Native change

1. Add a `LanguageFeature` in `LanguageVersionSettings.kt` with the target `sinceVersion` and the KT issue; use
   `sinceVersion = null` plus an `@Enables` annotation on a CLI flag when the behaviour is opt-in
   (`ExportKDocDocumentationToKlib`), and `forcesPreReleaseBinaries = true` when consumers of the klib must be at least
   as new (`IrCrossModuleInlinerBeforeKlibSerialization`).
2. Gate the producer side on `languageVersionSettings.supportsFeature(...)`: a pre-serialization lowering in
   `nativeLoweringsOfTheFirstPhase`, or a first-stage transformation like `TestProcessor`.
3. Keep the consumer side (second stage) able to read klibs without the feature; the klib-compatibility suite exercises
   both directions with the previous release.
4. If the result is a new IR shape that an old reader cannot parse at all, land it before the release's ABI bump and record
   it in `KotlinAbiVersionBumpHistory.md`.
5. Once the feature becomes default for the latest LV, the old shape is still accepted for `FIRST_SUPPORTED` releases.

## 9. Checklist: before changing X, check Y

| Changing… | Check / update | Run |
|-----------|----------------|-----|
| `KotlinIr.proto`, IR (de)serialization | `KotlinAbiVersion.CURRENT` bump before release, `KotlinAbiVersionBumpHistory.md`; old klibs still readable | `klib-compatibility` first-stage tasks, `klib-ir-inliner:test` (dump-abi), `partialLinkageTest` |
| Mangler / `IdSignature` | `ir_signature_versions`; signature-clash checks | `:compiler:ir.serialization.native:test`, dump-abi goldens |
| A pre-serialization lowering | `LanguageFeature` gate; second stage accepts both shapes; `PhasePrerequisites` | `klib-compatibility` both directions, `lowerings.md` tests |
| cinterop stub shape (`StubIrBuilder`) | `CInteropKlibMetadata2IRTransformer`; platform libs rebuilt; `CCallMode` both sides | `cinteropTest`, `klibTest` |
| Runtime layout, `TypeInfo`, entry points, `CompilerConstants.hpp` | matching `llvm/` change in the same commit; distribution caches rebuilt | `:kotlin-native:runtime:hostRuntimeTests`, `cachesTest`, codegen-box in `STATIC_EVERYWHERE` |
| A binary option or its default | `defaultX`, flavor append (system or user), `setupCommonOptionsForCaches`, `CacheMode.computeCacheDirName`, runtime module selection; deprecate values with `deprecatedWithReplacement` | `cachesTest`, test matrix with `-Pkn.gcType`/`-Pkn.gcScheduler`/`-Pkn.cacheMode` |
| GC-dependent codegen | add to `userCacheFlavorString` (today it ignores GC) | `cachesTest` with `-Pkn.gcType` |
| `konan.properties` (LLVM, sysroot, `minVersion.*`) | downloaded dependency names; `osVersionMin` consumers; cinterop bitcode readability (ABI bump precedent) | full `nativeCompilerTest`, platform libs build |
| A target's status | `KonanTarget.deprecatedTargets`/`toleratedDeprecatedTargets`, `softDeprecatedTargets`, `enabledByHost`, `cacheableTargets`, KGP diagnostics | KGP functional tests, `klibTest` cross-compilation |
| A CLI argument | `compiler/arguments` lifecycle; never delete, set `removedVersion`; KGP argument builders | `:compiler:arguments` stable-release tests, KGP functional tests |
| ObjC/C export naming | collision behaviour under the two `objcExport*NameCollisions` options; golden headers | `native/objcexport-header-generator` tests, `export.md` suites |
| KGP ↔ compiler coupling | `project.properties` version, `NativeProperties`, argument builders | `kgpNativeTests` |

## Open questions

- Who consumes the `depends` manifest property today? No reader was found in the compiler; KGP and the commonizer were not
  searched.
- How a `metadata_version` incompatibility attached by `getIncompatibility` in the second stage reaches the user (error,
  warning, or silent) was not traced end to end.
- Platform libraries are not covered by `KonanLibrarySpecialCompatibilityChecker`; is the same-build invariant enough once
  `kotlin.native.home` points at a mixed distribution, or should platform libs be checked like the stdlib?
- The compiler's `softDeprecatedTargets` and `KonanTarget.deprecatedTargets` are maintained separately and disagree
  (android, watchos_arm32); is that intentional?
- What exactly happens when KGP's version and the downloaded compiler's version differ (`kotlin.native.version` override):
  the failure mode for arguments unknown to the older side was not reproduced.
- Does any test assert ObjC header stability across compiler versions for the same input (as the klib-compatibility suite
  does for klibs)? None was found.
- `userCacheFlavorString` ignores `gc`, `allocator` and `gcSchedulerType`; the GC-agnostic codegen assumption that makes
  this sound is not stated in code or checked by a test.
