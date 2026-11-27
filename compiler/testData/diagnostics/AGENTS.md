# Diagnostic Tests

## Test File Format (`.kt` files)

### Header Directives

Comment lines at the top of the file control test behavior:

| Directive                                                 | Description                                                                                                                  |
|-----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `// RUN_PIPELINE_TILL: <phase>`                           | The phase the test is expected to reach: `FRONTEND`, `FIR2IR`, `LOWERINGS` or `CODEGEN`, see [Phases](#phases)               |
| `// ISSUE: KT-XXXXX`                                      | References a YouTrack issue                                                                                                  |
| `// WITH_STDLIB`                                          | Include stdlib in test classpath                                                                                             |
| `// LANGUAGE: +FeatureName` / `// LANGUAGE: -FeatureName` | Enable/disable language features                                                                                             |
| `// DIAGNOSTICS: -DIAGNOSTIC_NAME`                        | Suppress specific diagnostics                                                                                                |
| `// RENDER_DIAGNOSTICS_FULL_TEXT`                         | Produces `.diag.txt` with human-readable error messages, see [Dumps](#dumps)                                                 |
| `// RENDER_DIAGNOSTIC_ARGUMENTS`                          | Renders arguments inside markers, e.g. `<!TYPE_MISMATCH("A; B")!>`                                                           |
| `// FIR_DUMP`                                             | Produces `.fir.txt`, the resolved FIR tree; most tests don't need it                                                         |
| `// DUMP_CFG` / `// DUMP_CFG: FLOW`                       | Produces `.dot` with the control flow graph                                                                                  |
| `// DUMP_INFERENCE_LOGS: option1, option2`                | Possible options: FIXATION, MARKDOWN, MERMAID                                                                                |
| `// CHECK_TYPE`                                           | Enables `checkType { _<Type>() }` pattern for type assertions                                                                |
| `// FILE: Name.kt` / `// FILE: Name.java`                 | Multi-file test (splits single `.kt` file into virtual files)                                                                |
| `// LANGUAGE_FEATURE_TOGGLED: FeatureName`                | Enables the feature, and the test also runs with it disabled, see [Variant runners](#variant-runners)                        |
| `// LL_FIR_DIVERGENCE`                                    | Opens and closes the comment at the top of an `.ll.kt` (`.ll.wasmJs.kt`, …) explaining why LL FIR reports other diagnostics  |

stdlib is not available by default: functions like `println` produce `UNRESOLVED_REFERENCE` without `// WITH_STDLIB`.

### Inline Diagnostic Markers

Diagnostic assertions are placed inline around the code that should produce them:

- `<!DIAGNOSTIC_NAME!>code<!>` — asserts `code` produces that diagnostic
- `<!DIAGNOSTIC_NAME("arg1; arg2")!>code<!>` — with diagnostic arguments (needs `RENDER_DIAGNOSTIC_ARGUMENTS`)
- `<!DIAG1, DIAG2!>code<!>` — multiple diagnostics on same code span

### Debug Info Markers

- `<!DEBUG_INFO_CALL("fqName: ...; typeCall: ...")!>call<!>` — asserts call resolution target
- `<!DEBUG_INFO_EXPRESSION_TYPE("type")!>expr<!>` — asserts expression type

### Footer

- `/* GENERATED_FIR_TAGS: tag1, tag2, ... */` — tags listing the FIR constructs of the file, written by `update`; don't write or edit them

## Which Tests Read a Test Data File

Find this out first: it decides how the test data is updated.

**Managed tests** are updated and checked only with the [wrapper script](../../../analysis/AGENTS.md#working-with-test-data) of the
[test data manager](../../../analysis/test-data-manager/README.md): `update` writes their test data, `check` only compares it. `--path`
picks the modules, so one run covers the compiler and the LL tests of a file:

- `:compiler:jvm.tests` over `tests/`, `testsWithAnyBackend/` and `testsWithStdLib/`: `PhasedJvmDiagnosticLightTreeTestGenerated`, the
  golden test of the `.kt` files, and the [variant runners](#variant-runners).
- `:compiler:jvm.tests` over `foreignAnnotationsTests/`: `FirPsiOldFrontendForeignAnnotationsSourceJavaTestGenerated` (golden),
  `FirPsiOldFrontendForeignAnnotationsCompiledJavaTestGenerated` and
  `FirPsiOldFrontendForeignAnnotationsCompiledJavaWithPsiClassReadingTestGenerated`.
- The spec tests over `compiler/tests-spec/testData/diagnostics`: `FirLightTreeDiagnosticTestSpecGenerated` (golden), `LLSpecTestGenerated`,
  `LLReversedSpecTestGenerated`.
- The LL tests (`:analysis:low-level-api-fir:low-level-api-fir-compiler-tests`): `LLDiagnosticsTestGenerated`,
  `LLReversedDiagnosticsTestGenerated`, `LLPartialDiagnosticsTestGenerated` over the directories of the golden test, minus a few subtrees
  (`headerMode/`, most of `jvm/duplicateJvmSignature/`, `jvm/exposedPrivate/`, `testsWithStdLib/native/`: see the LL generator,
  `TestGenerator.kt` of that module), and their JS and Wasm suites over `testsWithJsStdLib/`, `wasmTests/` and `wasmWasiTests/`
  (`LLJsDiagnosticsFe10TestGenerated`, …); both Wasm suites also cover `tests/defaultArguments/`.
- The diagnostic tests of the compiler plugins and their LL counterparts, except the JS and Native ones below.

**Unmanaged tests** compare with the file they read, and only a Gradle run of the suite with `-Pkotlin.test.update.test.data=true` writes
it. The flag applies to the suites of that run only. It leaves the files of the managed tests in the run alone, but an unmanaged suite
writes the file it read even where a managed test reads it too, e.g., a `.kt` file of `testsWithAnyBackend/`.

- The JS, Wasm and Native compiler diagnostic tests (`:js:js.tests`, `:wasm:wasm.tests`,
  `:native:native.tests:klib-ir-inliner:withPlatformLibs`) over `testsWithJsStdLib/`, `wasmTests/`, `wasmWasiTests/`, `nativeTests/`, and
  over `testsWithAnyBackend/` and `tests/defaultArguments/`, whose `.kt` files the golden test writes too.
- The metadata diagnostic tests (`:compiler:tests-common-new`, `AbstractMetadataDiagnosticTest`) over `metadataDiagnosticTests/`.
- The JS diagnostic tests of the plugins: js-plain-objects (`:plugins:js-plain-objects:compiler-plugin`) over its `testData/diagnostics/`,
  and kotlinx-serialization (`:kotlinx-serialization-compiler-plugin`) over its `testData/diagnosticsJs/`.
- The Native diagnostic tests of atomicfu (`:kotlin-atomicfu-compiler:nativeTest`) over its `testData/backendDiagnostic/`.
- The scripting tests with custom script definitions and the REPL diagnostic tests (`:plugins:scripting:scripting-tests`):
  `AbstractScriptWithCustomDefDiagnosticsTestBase`, `AbstractReplWithTestExtensionsDiagnosticsTest`, `AbstractReplViaApiDiagnosticsTest`.
- The native LL tests (`:analysis:low-level-api-fir:low-level-api-fir-native-compiler-tests:llFirNativeTests`) over `nativeTests/`.

**The order of the runs**: the suite that writes the `.kt` file runs first.

- Where a managed golden test writes it (`tests/`, `testsWithAnyBackend/`, `testsWithStdLib/`, the spec tests, most plugins): run
  `update`, then run the unmanaged suites that read the file, without the flag.
- Where an unmanaged suite writes it: run that Gradle suite with the flag, review the result, and rerun it without the flag. Then, if
  managed LL tests read the file (`testsWithJsStdLib/`, `wasmTests/`, `wasmWasiTests/`, `metadataDiagnosticTests/`, js-plain-objects,
  scripting's `scriptsWithCustomDefinitions/`), run `update`; elsewhere (e.g., `nativeTests/`, serialization's `diagnosticsJs/`, atomicfu's
  `backendDiagnostic/`) there is nothing managed to run.

## Workflows

### Creating a New Test

1. **Create the `.kt` test file** with the directives and the code. Leave out the diagnostic markers, `GENERATED_FIR_TAGS` and
   `RUN_PIPELINE_TILL`, but write the `DEBUG_INFO_CALL` and `DEBUG_INFO_EXPRESSION_TYPE` markers you want to assert: they are reported
   only where a marker already is, and `update` fills in their arguments.
2. **Generate the runners if the suite commits them**: the suites with `tests-gen` directories (the native LL tests, kotlinx-serialization,
   Compose, atomicfu, scripting, plugin interactions) need `./gradlew generateTests` (or the module's generator), and the regenerated files
   are committed. `:compiler:jvm.tests` and `:analysis:low-level-api-fir:low-level-api-fir-compiler-tests` generate their runners during
   the run.
3. **Run `update`** on the new file, in [the order of the runs](#which-tests-read-a-test-data-file) (if only unmanaged suites read it, run
   them instead and skip to step 6):
   ```bash
   python3 analysis/test-data-manager/test-data-manager.py update --path compiler/testData/diagnostics/tests/resolve/problems/myTest.kt
   ```
   It writes the diagnostic markers into the `.kt` file, and creates the variants and dumps whose output differs. The phased compiler
   tests also write the `GENERATED_FIR_TAGS` footer and `RUN_PIPELINE_TILL`, see [Phases](#phases).
4. **Review everything it wrote**: `git diff` for the changed files, and `git status --short` plus the `CREATED` lines of the output for the
   new ones, the test itself included. A passed `update` needs no `check`.
5. **Review a created `.ll.kt` (or `.ll.wasmJs.kt`, …)**: the run fails with a `TEST ERROR` until it has an `LL_FIR_DIVERGENCE` comment.
   Compare it with the file it falls back to (`.kt`, or `.ll.kt` for `.ll.wasmJs.kt`, see [Variant files](#variant-files)). A difference
   is an LL bug by default: fix it, or explain in the comment why LL may diverge there; a comment the variant inherited from `.ll.kt` has
   to explain this variant's difference too. After fixing a failed run, run `update --rerun <runId>`; after editing a variant of a passed
   run, run `update --path` on the test.
6. **Run the unmanaged suites** that read the file, see [Which Tests Read a Test Data File](#which-tests-read-a-test-data-file).

### Changing a Test or the Compiler

1. **Edit the `.kt` file, and its existing variants the same way.** A variant (`foo.ll.kt`, `foo.latestLV.kt`, …, see
   [Variant files](#variant-files)) repeats the code of the test data, and the tests check that it differs only in the diagnostics and
   `RUN_PIPELINE_TILL`. Keep what belongs to the variant: its `LL_FIR_DIVERGENCE` comment and its own `RUN_PIPELINE_TILL`. Don't edit
   the diagnostic markers or dumps: `update` writes them. Move or delete the variants and dumps together with their test; delete those
   of a configuration the test no longer runs in, e.g., `.disabled.kt` after removing `LANGUAGE_FEATURE_TOGGLED`.
2. **Run `update`** on the changed tests, or on the directories a compiler change affects, in
   [the order of the runs](#which-tests-read-a-test-data-file).
3. **Review the result** as for a new test: an intended change of the compiler shows up in the markers and dumps, anything else is a bug.
4. **Run the unmanaged suites** that read the changed files.

### Fixing a Failed `check`

1. **Run `check --path`** on the test data files or directories (the `.kt` files, not their dumps). The output lists the first problems;
   the module reports linked from the summary have all of them, with `actualFile` and `comparedFile` for a `MISMATCH`.
2. **Decide for each problem whether the new output is intended.** Fix the code or the test for the others first.
3. **Update a test only when all its problems are intended**: `update --path` and `update --rerun <runId>` take whole tests, and write
   every output of the test and its variants, intended or not. `update --rerun <runId>` reruns all the failed tests of a run. Review what
   the update wrote, as for a new test.
4. **A `TEST ERROR` isn't a mismatch**: read its message and stack trace in the module report. A crash, a missing `LL_FIR_DIVERGENCE`
   comment, or a variant whose code differs from the test data it falls back to: fix the cause. An `.ll.kt` that differs from the test
   data only in its comment: run `update` on the test, which deletes the variant.
5. **A mismatch that `update` doesn't fix** comes from a muted test (`MUTE_LL_FIR`, `IGNORE_REVERSED_RESOLVE`, a `.fail` file, …) or from
   an analysis that crashed or stopped early: their output is only compared. Keep a mute while the failure it tracks remains, and remove it
   once that is fixed. For an analysis that stopped early, fix the first failing module.

The exit codes and the other kinds of problems are in [analysis/AGENTS.md](../../../analysis/AGENTS.md#working-with-test-data).

## Reference

### Variant Files

The `.kt` file is the golden test data. A test whose output differs from it has a variant file, with the variant before the extension:

| Test                                   | Reads and writes                                                            |
|----------------------------------------|-----------------------------------------------------------------------------|
| Compiler                               | `.kt`                                                                       |
| Compiler, context-sensitive resolution | `.contextSensitive.kt` → `.kt`                                              |
| Compiler, latest language version      | `.latestLV.kt` → `.kt`                                                      |
| Compiler, toggled language feature off | `.disabled.kt` → `.kt`                                                      |
| Compiler, no type alias expansion      | `.noAliasExpansion.kt` → `.kt`                                              |
| Foreign annotations, compiled Java     | `.compiledJava.kt` → `.kt`                                                  |
| Foreign annotations, compiled as PSI   | `.psiClassReading.kt` → `.compiledJava.kt` → `.kt`                          |
| LL                                     | `.ll.kt` → `.kt`                                                            |
| LL, reversed resolution order          | `.reversed.kt` → `.ll.kt` → `.kt`                                           |
| LL, partial body analysis              | `.partialBody.kt` → `.ll.kt` → `.kt`                                        |
| LL, Wasm                               | `.ll.wasmJs.kt` → `.ll.kt` → `.kt`                                          |
| LL, Wasm, reversed resolution order    | `.reversed.wasmJs.kt` → `.ll.wasmJs.kt` → `.reversed.kt` → `.ll.kt` → `.kt` |

- A test reads the first existing file and writes only the first one, its own variant. `update` creates the variant when the output
  differs from the file it falls back to, and deletes it when they are identical (an `.ll.kt` also when it differs only in its
  `LL_FIR_DIVERGENCE` comment or in `METADATA` tags). Don't create variants by hand; delete them by hand only with their test, or when
  the test no longer runs in their configuration.
- The Wasm WASI tests use `wasmWasi` instead of `wasmJs`, and the LL JS tests use the plain LL chains. A script gets the variant before its
  whole extension: `foo.ll.kts`, `foo.ll.test.kts`.
- Muted tests (`MUTE_LL_FIR`, `IGNORE_REVERSED_RESOLVE`, a `.fail` file, …) and tests whose analysis crashed are only compared.
- The compiler tests run before the LL tests, which fall back to their `.kt` files. Don't pass `--incremental`: it doesn't see the changes
  of another module's run; narrow the run with `--path` instead.

### Variant Runners

The variant runners of `:compiler:jvm.tests` run the tests in another configuration. The latest LV, disabled and no-alias runners cover
`tests/` (except its `jvm/` and `multiplatform/` subtrees) and `testsWithStdLib/`; a test outside these directories isn't run by them:

- `FirLightTreeDiagnosticsWithLatestLanguageVersionTestGenerated`: every test, with the latest language version. A feature whose
  `sinceVersion` is a future version is enabled there, so the expectations can differ: `.latestLV.kt`.
  `FirOldFrontendMPPDiagnosticsWithLightTreeWithLatestLanguageVersionTestGenerated` does the same for `tests/multiplatform/`.
- `FirLightTreeDiagnosticsWithLanguageFeatureDisabledTestGenerated`: the tests with `LANGUAGE_FEATURE_TOGGLED: F`, with `F` disabled; every
  other run enables `F`. The expectations with `F` disabled: `.disabled.kt`.
- `FirLightTreeDiagnosticsWithoutAliasExpansionTestGenerated`: the tests with a type alias, without type alias expansion, a sanity check of
  the diagnostics: `.noAliasExpansion.kt`.
- `PhasedJvmDiagnosticLightTreeWithContextSensitiveEnabledTestGenerated`: `tests/contextSensitiveResolutionUsingExpectedType/ideHint/` with
  context-sensitive resolution: `.contextSensitive.kt`.

Whether a test runs in these configurations is decided by the `.kt` file, not by its variant.

### Phases

In the phased compiler tests (`:compiler:jvm.tests` over `tests/`, `testsWithAnyBackend/`, `testsWithStdLib/`, and most plugin
diagnostic tests), `RUN_PIPELINE_TILL` declares the last phase the test is expected to reach: `FRONTEND` (FIR resolution with errors),
`FIR2IR`, `LOWERINGS` or `CODEGEN`. A test that passes the whole pipeline runs till its runner's last phase, normally `CODEGEN`.

- `update` adds a missing directive and promotes one the test can pass: a plain run fails with "Phase FRONTEND could be promoted to
  CODEGEN". `DISABLE_NEXT_PHASE_SUGGESTION` turns the promotion off.
- `update` doesn't demote: if an intended error makes the test fail at an earlier phase, set the directive to that phase by hand.
- The golden test's edits of `RUN_PIPELINE_TILL` go into the `.kt` file and its variants, except those whose runners check their own
  phase: `.disabled.kt`, `.contextSensitive.kt` and `.noAliasExpansion.kt`. Such a runner may stop at another phase, so `update` writes
  its `RUN_PIPELINE_TILL` into its own variant. `GENERATED_FIR_TAGS` go into all variants except `.disabled.kt`.

### Dumps

| Extension        | Content                                                         | Requested by                     |
|------------------|-----------------------------------------------------------------|----------------------------------|
| `.fir.txt`       | FIR tree dump (resolved FIR with `R\|...\|` references)         | `FIR_DUMP`                       |
| `.diag.txt`      | Diagnostics as text (`/file.kt:line:column: severity: message`) | `RENDER_DIAGNOSTICS_FULL_TEXT`   |
| `.dot`           | Control flow graph in Graphviz format                           | `DUMP_CFG`                       |
| `.fixation.txt`  | Type variable fixation process                                  | `DUMP_INFERENCE_LOGS: FIXATION`  |
| `.inference.md`  | General type inference logs                                     | `DUMP_INFERENCE_LOGS: MARKDOWN`  |
| `.inference.mmd` | General type inference logs via Mermaid format                  | `DUMP_INFERENCE_LOGS: MERMAID`   |

- The dumps of a managed test follow the chain of its `.kt` file, with the variant before the dump extension: the reversed LL test reads
  `foo.reversed.fir.txt` → `foo.ll.fir.txt` → `foo.fir.txt` and writes only `foo.reversed.fir.txt`. `update` writes them like the `.kt`
  file, but not after a crash.
- A test's own existing `.fir.txt` (e.g., `foo.ll.fir.txt` for the LL test) keeps its FIR dump on without `FIR_DUMP`; a dump it falls
  back to doesn't, so keep `FIR_DUMP` while the variants should compare with `foo.fir.txt`. Some runners turn a dump on by default
  (e.g., `FIR_DUMP` for `firMembers/` of kotlinx-serialization). To stop a dump, remove what turns it on (or disable the handler, e.g.,
  with `DISABLE_FIR_DUMP_HANDLER`) and delete the file. `update` deletes a dump the test no longer produces only while the handler runs.
- Not every run produces every dump: the latest LV and no-alias runs produce no `.fir.txt`, and the LL, latest LV, disabled and no-alias
  runs no `.diag.txt` from `RENDER_DIAGNOSTICS_FULL_TEXT`.

Diagnostic messages rendered into `.diag.txt` must follow the [Diagnostic Message Style Guide](../../../docs/diagnostic_message_style_guide.md).

## Directory Structure at `testData/diagnostics/tests`

Root level contains individual `.kt` test files with their `.fir.txt` dumps. Subdirectories group tests by topic:

- `annotations/`, `arguments/`, `arrays/` — basic language constructs
- `builtins/`, `stdlib/` — standard library interactions
- `callResolution/` — call resolution scenarios (overloads, invoke, SAM, operators)
- `cfa/`, `cfg/` — control flow analysis and graphs
- `checkers/`, `extraCheckers/`, `diagnostics/` — diagnostic-focused tests
- `collectionLiterals/`, `constructors/`, `constVal/` — specific constructs
- `contextParameters/`, `contextSensitiveResolutionUsingExpectedType/` — context-dependent resolution
- `contracts/` — Kotlin contracts
- `delegates/`, `destructuring/` — delegation and destructuring
- `exhaustiveness/` — exhaustive when/sealed checks
- `expresssions/` (note: misspelled in repo), `fromBuilder/` — expression-level tests
- `headerMode/` — header/expect declarations
- `inference/` — type inference
- `inlineClasses/`, `innerClasses/`, `localClasses/` — class variants
- `j+k/` — Java-Kotlin interop
- `multifile/` — multi-file tests (using `// FILE:` directive)
- `multiplatform/` — multiplatform expect/actual
- `nestedTypeAliases/`, `typeArguments/`, `typeParameters/`, `types/` — type system
- `overloadResolution/`, `overrides/` — overloading and overriding
- `problems/` — regression/bug scenarios
- `properties/`, `propertyVsField/` — property resolution
- `qualifiers/`, `references/` — qualified access and references
- `returnInExpressionBodies/` — return in expression body functions
- `samConstructors/`, `samConversions/` — SAM conversion tests
- `scopes/`, `visibility/` — scoping and visibility
- `scripts/` — Kotlin scripting
- `smartcasts/` — smart cast tests
- `suppress/` — @Suppress annotation tests
- `unqualifiedEnum/` — unqualified enum access
- `vfir/` — virtual FIR tests
- `withAllowedKotlinPackage/` — tests allowing kotlin package
