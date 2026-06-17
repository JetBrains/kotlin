# Migration Notes

Every site flagged by `scan_usages.py --distro-pair g951-to-PAPI-20260609`, with a site-specific
reason. `apply_migrations.py` applied **0** automatic rewrites: this repo has no confirmed
single-match Cat-A setter call sites (its 15 Cat-A hits are all `isEnabled(arg)` name collisions —
see below). Per the **change-minimization principle** in `MIGRATION_RULES.md`, a site is rewritten
only when the old form would fail to compile or change behavior under the Gradle 10 preview.

Task 06 therefore makes **no source rewrites**. Every site below is one of:

- **(FP)** a name-collision false positive — the receiver is a Kotlin-plugin / JDK / third-party
  (Android, Konan) type whose member happens to share a name with a migrated Gradle accessor, not a
  Gradle type listed in `migration-data.json`; or
- **(DSL-survives)** a `prop = value` assignment, `+=` operator, or `mapProp[k]` form that keeps
  compiling via the `org.gradle.kotlin.dsl` overloads (active in `.gradle.kts` and `kotlin-dsl`
  modules), so it is left unchanged; or
- **(→07)** a real Gradle receiver where, *if* the `org.gradle.kotlin.dsl` overload is not active in
  that module, the old form becomes a concrete compile error — deferred to tasks 07/08, which is where
  the rules (Cat-D/Cat-E) direct these to be resolved against compiler output.

---

## Category A — `is*` removed-accessor collisions (15 sites) — all FP

The removed accessors are the zero-arg `org.gradle.caching.configuration.BuildCache.isEnabled()` and
`org.gradle.testing.jacoco.plugins.JacocoTaskExtension.isEnabled()`. Every hit is instead a
**one-argument** `isEnabled(x)` call on a non-Gradle receiver — `org.jetbrains.kotlin.konan.target.HostManager.isEnabled(KonanTarget)`,
`Kapt3GradleSubplugin.isEnabled(Project)`, or an override of `org.gradle.api.logging.Logger.isEnabled(LogLevel)`
(which is not a removed accessor) — so none are the migrated property.

- kotlin-native/build-tools/src/main/kotlin/org/jetbrains/kotlin/dependencies/NativeDependenciesDownloader.kt:61 — RHS `logger.isEnabled(LogLevel.INFO)`, `org.gradle.api.logging.Logger.isEnabled(LogLevel)`, retained API
- libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/abi/AbiValidationKmpIT.kt:205 — `HostManager().isEnabled(KonanTarget)`
- libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/cinterop-MetadataDependencyTransformation-kt-50952/p1/build.gradle.kts:23 — `HostManager().isEnabled(konanTarget)`
- libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/cinterop-MetadataDependencyTransformation-kt-50952/p2/build.gradle.kts:20 — `HostManager().isEnabled(konanTarget)`
- libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/commonize-kt-50847-cinterop-missing-in-supported-target/build.gradle.kts:20 — `HostManager().isEnabled(target.konanTarget)`
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/internal/kapt/Kapt3KotlinGradleSubplugin.kt:531 — `Kapt3GradleSubplugin.isEnabled(project)`
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/statistics/FusMetrics.kt:416 — `HostManager().isEnabled(compilation.target.konanTarget)`
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/android/AndroidProjectHandler.kt:256 — `Kapt3GradleSubplugin.isEnabled(project)`
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/KotlinNativeTarget.kt:48 — `hostManager.isEnabled(konanTarget)`
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/KotlinNativeTargetPreset.kt:135,159 — `HostManager().isEnabled(this)`
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/internal/PlatformLibrariesGenerator.kt:185 — `HostManager().isEnabled(konanTarget)`
- libraries/tools/kotlin-gradle-plugin/src/test/kotlin/org/jetbrains/kotlin/gradle/statistics/TestLogger.kt:59 — `override fun isEnabled(level: LogLevel?)`, overrides `Logger.isEnabled`, not a removed accessor
- libraries/tools/kotlin-gradle-plugin/src/test/kotlin/org/jetbrains/kotlin/testhelpers/StubLogger.kt:287 — `override fun isEnabled(level: LogLevel?)`, overrides `Logger.isEnabled`
- native/kotlin-test-native-xctest/build.gradle.kts:102 — `hostManager.isEnabled(target.konanTarget)`

---

## Category B — changed-return-type getter reads (231 sites) — all FP

Two root causes, both name collisions on Kotlin-plugin types (none of these receivers appear in
`migration-data.json`):

**B1 — custom `getFile()` extension (128 of the hits, those targeting `PublicationArtifact.file`).**
`libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/utils/fileUtils.kt:123,126`
declares `internal fun Provider<RegularFile>.getFile(): File = get().asFile` and
`internal fun Provider<Directory>.getFile(): File = get().asFile`. Every `.getFile()` hit calls this
Kotlin extension on a `Provider<FileSystemLocation>` (it already resolves the provider internally), not
`org.gradle.api.publish.PublicationArtifact.getFile()`.

**B2 — generic getter overrides/reads on Kotlin domain types (the rest).** `getName()`/`.name`,
`getDisplayName()`, `getPath()`, `getVersion()`, `getModule()`, `getTags()`, `getDistributionType()`,
`getIncludes()`/`getExcludes()`, `getProperties()` are declared/overridden on Kotlin's own types
(e.g. `ModuleVersionIdentifierWithUnspecifiedValue : org.gradle.api.artifacts.ModuleVersionIdentifier`
in `kotlinVariants.kt`, `KotlinTarget`, JS/Native sub-targets, `NativeDistributionTypeProvider`,
attribute value classes) or are JDK/library calls (`System.getProperties()`). The `[CONFIRMED:
MavenPublication]` annotation on `kotlinVariants.kt:34` is import-only (the file imports
`MavenPublication` elsewhere); the receiver there implements `ModuleVersionIdentifier`, whose
`getVersion()` returns a plain `String` and is not migrated.

If any of these receivers turned out to be a genuinely-migrated Gradle getter, the missing `.get()`
would surface as a type mismatch in task 07; none are expected to.

Flagged files (line numbers from the scan):

- kotlin-native/build-tools/src/main/kotlin/org/jetbrains/kotlin/bitcode/CompileToBitcodePlugin.kt:183,433
- kotlin-native/build-tools/src/main/kotlin/org/jetbrains/kotlin/gradle/plugin/konan/tasks/KonanCompileTask.kt:52
- kotlin-native/build-tools/src/main/kotlin/org/jetbrains/kotlin/konan/target/TargetWithSanitizer.kt:24
- kotlin-native/performance/buildSrc/src/main/kotlin/benchmark/SwiftBenchmarkingPlugin.kt:63
- libraries/reflect/build.gradle.kts:115
- libraries/tools/kotlin-compose-compiler/src/common/kotlin/org/jetbrains/kotlin/compose/compiler/gradle/ComposeFeatureFlags.kt:27,38
- libraries/tools/kotlin-gradle-plugin-api/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/KotlinCompilation.kt:318
- libraries/tools/kotlin-gradle-plugin-api/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/KotlinPlatformType.kt:69
- libraries/tools/kotlin-gradle-plugin-api/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/KotlinTarget.kt:163
- libraries/tools/kotlin-gradle-plugin-api/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/NativeBinaryTypes.kt:45
- libraries/tools/kotlin-gradle-plugin-api/src/common/kotlin/org/jetbrains/kotlin/gradle/tasks/KotlinTaskConfigs.kt:76,84
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/dsl/KotlinNativeBinaryContainer.kt:54,149,152,164
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/internal/testing/TCServiceMessagesClient.kt:450
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/internal/CustomPropertiesFileValueSource.kt:39
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/KotlinLLDBScript.kt:51,52
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/KotlinSoftwareComponent.kt:46,191
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/KotlinTargetSoftwareComponentImpl.kt:172
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/AppleXcodeTasks.kt:70,599,602,606,607,608
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/CheckXcodeTargetsConfigurationTask.kt:200,220,236,260
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/XCFrameworkTask.kt:149,216,290,294,461,495
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/XcodeVersionTask.kt:50,103
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftexport/SwiftExport.kt:53,114,191,321
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftexport/internal/SwiftExportAction.kt:60,78
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftexport/internal/SwiftExportedDependency.kt:35,45
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftexport/tasks/BuildSPMSwiftExportPackage.kt:95,106,108,129,165
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftexport/tasks/GenerateSPMPackageFromSwiftExport.kt:67,79,88,100,132,145
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftexport/tasks/MergeStaticLibrariesTask.kt:42
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftexport/tasks/SwiftExportTask.kt:81
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftimport/ComputeLocalPackageDependencyInputFiles.kt:57
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftimport/SwiftImportSetupAction.kt:809
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftimport/XcodebuildDefFileWorkAction.kt:56,57,91,111,117,134
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftimport/xcodeIntegrations.kt:127,229
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/external/ExternalKotlinTargetComponent.kt:51
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/external/ExternalKotlinTargetSoftwareComponent.kt:59
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/kotlinVariants.kt:33,34,36,38,80,116
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/uklibs/publication/ArchiveUklibTask.kt:72,73,79,80
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/sources/DefaultKotlinSourceSet.kt:71
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/report/BuildReportsService.kt:242
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/AbstractSetupTask.kt:122,123
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/KotlinJsCompilerAttribute.kt:19,23
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/KotlinWasmCompilerAttribute.kt:18,22
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/ir/KotlinJsBrowserTestImpl.kt:35
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/ir/KotlinJsIrSubTarget.kt:42
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/nodejs/NodeJsExec.kt:89
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/nodejs/NodeJsSetupTask.kt:38
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/npm/Npm.kt:28,138
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/npm/NpmDependency.kt:36,38
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/npm/NpmProject.kt:129
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/npm/PublicPackageJsonTask.kt:79
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/npm/RequiresNpmDependenciesTask.kt:28
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/npm/resolver/KotlinCompilationNpmResolution.kt:150
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/npm/tasks/KotlinToolingSetupTask.kt:65,73
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/swc/SwcEnvSpec.kt:33
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/swc/SwcSetupTask.kt:40
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/testing/KotlinWasmD8.kt:80
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/testing/KotlinWasmNode.kt:58,94
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/testing/WebpackBundleKotlinJsTests.kt:101
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/testing/karma/KotlinKarma.kt:107,389,396,400,409,422,423,429
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/testing/mocha/KotlinMocha.kt:71,97,101
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/typescript/TypeScriptValidationTask.kt:99
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/webpack/KotlinWebpack.kt:290
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/webpack/KotlinWebpackRule.kt:66,148,154,156
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/yarn/YarnSetupTask.kt:37
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/yarn/YarnWorkspaces.kt:28,97
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/jvm/tasks/KotlinJvmTest.kt:44
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/DefaultCInteropSettings.kt:66,97
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/NativeBinaries.kt:151
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/NativeCompilerDownloader.kt:90,278
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/cocoapods/CocoapodsExtension.kt:131,301,368
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/cocoapods/KotlinCocoapodsPlugin.kt:68,362,610,634,644,655,656,657,700
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/cocoapods/tasks/DefFileTask.kt:37,41
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/cocoapods/tasks/DummyFrameworkTask.kt:69,100,109,126,150
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/cocoapods/tasks/PodBuildTask.kt:92
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/cocoapods/tasks/PodSetupBuildTask.kt:57
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/cocoapods/tasks/PodspecTask.kt:130
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/tasks/FatFrameworkTask.kt:183
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/tasks/KotlinNativeTasks.kt:1004
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/toolchain/KotlinNativeBundleBuildService.kt:154
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/wasm/binaryen/BinaryenEnvSpec.kt:36
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/wasm/binaryen/BinaryenExec.kt:128
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/wasm/binaryen/BinaryenSetupTask.kt:45
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/wasm/binaryen/BinaryenWorkAction.kt:33,35,37
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/wasm/d8/D8EnvSpec.kt:57
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/wasm/d8/D8SetupTask.kt:42
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/wasm/internal/WasmBinaryTransform.kt:200
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/wasm/nodejs/WasmNodeJsRootPlugin.kt:129
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/wasm/nodejs/WasmNpmTooling.kt:47,49
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/web/nodejs/BaseNodeJsEnvSpec.kt:44
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/web/nodejs/NodeJsRootPluginApplier.kt:196
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/web/yarn/BaseYarnRootEnvSpec.kt:72
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/tasks/AbstractKotlinCompileTool.kt:76,79
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/tasks/Kotlin2JsCompile.kt:211,351
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/tasks/KotlinCompile.kt:419
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/tasks/TasksOutputsBackup.kt:99
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/testing/KotlinAggregateExecutionSource.kt:33,65
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/utils/fileUtils.kt:123,126 — the extension declarations themselves
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/internal/compilerRunner/native/KotlinNativeToolRunner.kt:75
- libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/unitTests/EmbedAndSignTaskTests.kt:53,82,86,90,91,113,121,154,158,234,259
- libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/unitTests/InvokeWhenCreatedTest.kt:27
- libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/unitTests/ReportDataTest.kt:47,48,49,152,154,155
- libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/unitTests/SwiftExportUnitTests.kt:211
- libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/unitTests/utils/ApplyEmbedAndSignEnvironment.kt:16,17
- libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/util/buildProject.kt:192
- libraries/tools/kotlin-gradle-plugin/src/gradle811/kotlin/org/jetbrains/kotlin/gradle/plugin/diagnostics/ProblemsReporterG811.kt:70,71,78,85
- libraries/tools/kotlin-gradle-plugin/src/gradle88/kotlin/org/jetbrains/kotlin/gradle/plugin/diagnostics/ProblemsReporterG88.kt:70,71,78,85
- repo/gradle-build-conventions/binary-compatibility-extended/src/main/kotlin/targets/BcvTarget.kt:73
- repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/KotlinModuleMetadataVersionBasedSkippingTransformer.kt:43
- repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/plugins/CustomVariantPublishingDsl.kt:184,192,209
- repo/kotlin-build-helpers/src/localProperties.kt:26

---

## Category C — `+=` operator mutations (19 sites) — DSL-survives / FP

All 19 hits are `+=` (there are no `-=` or `<<` hits, which would require rewriting). By kind:

- **`.gradle.kts` / kotlin-dsl `.kt` on a real list/set property** — `+=` keeps compiling via the
  `org.gradle.kotlin.dsl` `plusAssign(HasMultipleValues<T>, …)` extension, so it is left unchanged per
  change-minimization:
  - build.gradle.kts:849 — `args += listOf(...)` inside an `exec`/`JavaExec` spec block
  - libraries/kotlin.test/build.gradle.kts:331 — `filter.excludePatterns += "*ContributorTest"` (`TestFilter.excludePatterns` SetProperty)
  - wasm/wasm.tests/build.gradle.kts:343,355,365,376 — `jvmArgumentProviders += objects.newInstance<...>()`
  - repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/share-foreign-java-nullability-annotations.gradle.kts:29 — `jvmArgumentProviders += CommandLineArgumentProvider { ... }`
  - repo/gradle-build-conventions/java-flight-recorder/src/main/kotlin/java-flight-recorder.gradle.kts:19 — `testTask.jvmArgumentProviders += objects.newInstance<...>()`
  - repo/gradle-build-conventions/test-data-manager-convention/src/main/kotlin/test-data-manager.gradle.kts:96 — `jvmArgumentProviders += testTask.jvmArgumentProviders`
  - repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/setupIrJsBoxTests.kt:50 — `jvmArgumentProviders += ...` in a `kotlin-dsl` convention module (auto-imports `org.gradle.kotlin.dsl`)
  - repo/gradle-build-conventions/project-tests-convention/src/main/kotlin/jsTest.kt:39 — `jvmArgumentProviders += ...` in a `kotlin-dsl` convention module
- **Third-party Android `packagingOptions.resources.excludes`** — `com.android.build` DSL, not Gradle
  core; out of scope:
  - libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/AndroidSimpleComposeApp/build.gradle.kts:44 — `excludes += "META-INF/..."`
  - libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/JBComposeApp/composeApp/build.gradle.kts:56 — `excludes += "/META-INF/{AL2.0,LGPL2.1}"`
  - libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/kapt2/android-databinding/app/build.gradle:34 — `excludes += "META-INF/..."` (AGP)
  - libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/kapt2/android-databinding/library/build.gradle:31 — `excludes += "META-INF/..."` (AGP)
- **Standalone integration-test fixture project** — run by KGP integration tests against their own
  pinned Gradle, not built/configured by this repo's `help`/`assemble`; left as-is:
  - libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/kapt2/jpms-module/build.gradle:34 — fixture `options.compilerArgs += [...]`
- **String-literal test data, not executed code** — `args += [...]` embedded inside a Kotlin triple-quoted
  test source string (`|    args += [...]`):
  - libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/Kotlin2JsGradlePluginIT.kt:98,371
  - libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/android/KaptAndroidIT.kt:101 — `arguments += [...]` inside a test-fixture build-script string

---

## Category D — `prop = value` assignments (32 sites) — DSL-survives / →07

Per the Cat-D rule, `prop = value` is detect-only: it keeps compiling wherever Gradle's lazy
assignment overload is active (`.gradle.kts`, `kotlin-dsl` convention modules), and is rewritten to
`.set(...)`/`.setFrom(...)` only if a module without the overload fails to compile in task 07. The
receivers below are real Gradle types (exec specs, `MavenPublication`, `Test`/`AbstractCompile`
`classpath`, `UrlArtifactRepository.url`), so each is left unchanged now and re-checked against task 07
compiler output.

- kotlin-native/build-tools/src/main/kotlin/org/jetbrains/kotlin/ExecLlvm.kt:19 — `executable = ...` on an exec spec
- kotlin-native/performance/buildSrc/src/main/kotlin/RunKotlinNativeTask.kt:31,145 — `standardOutput = output`, `executable = "cset"`
- kotlin-native/performance/buildSrc/src/main/kotlin/SwiftBuildTask.kt:81 — `standardOutput = this@apply`
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/internal/exec.kt:39,40 — `exec.standardOutput`/`exec.errorOutput` on an `ExecOperations.exec { exec -> }` `ExecSpec` lambda param
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/publishing/Publishing.kt:152 — `artifactId = ...` on `MavenPublication`
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/webpack/KotlinWebpackRunner.kt:107,113,131 — `execSpec.standardOutput`/`errorOutput`/`args` on an `ExecSpec`
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/jvm/KotlinJvmBinariesDsl.kt:342,345,347,348 — `CreateStartScripts` `classpath`/`applicationName`/`executableDir`/`defaultJvmOpts`
- libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/dependencyResolutionTests/MavenRepositories.kt:26 — `it.url = ...` inside `maven { }` (`UrlArtifactRepository.url`)
- libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/unitTests/diagnosticsTests/AndroidPublicationNotConfiguredTest.kt:57,58,59 — `groupId`/`artifactId`/`version` on `MavenPublication`
- libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/utils/processes/ExecAsyncHandleTest.kt:64,65,91,92,146,250 — `standardOutput`/`errorOutput`/`standardInput`/`executable` on exec specs
- repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/LibrariesCommon.kt:57 — `classpath = objects.fileCollection().from()` (`AbstractCompile.classpath`; convention module applies `kotlin-dsl`)
- repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/localDependencies.kt:57 — `url = baseDir.toURI()` (`UrlArtifactRepository.url`)
- repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradleCommon.kt:893 — `classpath = classpath.filter { ... }` (`Test`/`AbstractCompile.classpath`)
- repo/gradle-build-conventions/project-tests-convention/src/main/kotlin/ProjectTestsExtension.kt:261 — `this.args = buildList { ... }` (`JavaExecSpec.args`)
- repo/gradle-build-conventions/project-tests-convention/src/main/kotlin/generalTestTask.kt:113,114,277 — `Test` `classpath`/`testClassesDirs`/`maxParallelForks`
- repo/gradle-build-conventions/project-tests-convention/src/main/kotlin/testCodebaseTask.kt:52 — `Test.testClassesDirs`

---

## Category E — collection ops on now-lazy props (18 sites) — FP / →07

`.remove`/`.removeAll`/`.filterKeys` have no equivalent on `MapProperty`/`ListProperty`/`SetProperty`.
Where the receiver is a plain Kotlin/JDK collection (FP) nothing changes; where it is a genuine Gradle
lazy property the call becomes a compile error fixed in task 07.

- analysis/analysis-tools/deprecated-k1-frontend-internals-for-ide-generated/build.gradle.kts:14 — `options.compilerArgs.remove("-Werror")` (`CompileOptions.compilerArgs` → 07 if lazy)
- libraries/tools/abi-validation/kgp-integration-tests/build.gradle.kts:10 — `environment.remove("KONAN_DATA_DIR")` (`ProcessForkOptions.environment` → 07)
- libraries/tools/kotlin-gradle-plugin-integration-tests/build.gradle.kts:393 — `environment.remove("KONAN_DATA_DIR")` (→ 07)
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/compilerRunner/CompilerSystemPropertiesService.kt:52 — `properties.remove(it)` on a local `Properties`/map (FP)
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/internal/kapt/Kapt3KotlinGradleSubplugin.kt:371 — `options.compilerArgumentProviders.removeAll(...)` (Android `CompileOptions`; → 07 if lazy)
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftimport/ComputeLocalPackageDependencyInputFiles.kt:85 — `exec.environment.remove(it)` (→ 07)
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftimport/FetchSyntheticImportProjectPackages.kt:132 — `exec.environment.remove(key)` (→ 07)
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftimport/XcodebuildDefFileWorkAction.kt:155,164 — `exec.environment.remove(it)` (→ 07)
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/report/BuildReportsService.kt:376,377 — `tags.remove(...)` on a local `MutableList<StatTag>` (FP)
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/tasks/KotlinNativeTest.kt:97 — `environment.filterKeys(...)` read-only transform (FP, or → 07 if `environment` is the lazy property)
- libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/internal/compilerRunner/native/KotlinNativeToolRunner.kt:121 — `spec.environment.remove(it)` (→ 07)
- native/native.tests/klib-compatibility/build.gradle.kts:139,140 — `systemProperties.remove("...")` (`JavaForkOptions.systemProperties` → 07)
- repo/gradle-build-conventions/project-tests-convention/src/main/kotlin/generalTestTask.kt:258 — `filter.excludePatterns.removeAll(parallelTestsExcludes)` (`TestFilter.excludePatterns` → 07)
- repo/gradle-build-conventions/project-tests-convention/src/main/kotlin/nativeTest.kt:451 — `systemProperties.filterKeys { ... }` read-only transform (FP, or → 07)
- repo/gradle-build-conventions/test-data-manager-convention/src/main/kotlin/test-data-manager.gradle.kts:113 — `testTask.systemProperties.filterKeys { ... }` read-only transform (→ 07 if lazy)
