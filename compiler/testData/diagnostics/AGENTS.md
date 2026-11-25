# Diagnostic Tests

## Test File Format (`.kt` files)

### Header Directives

Comment lines at the top of the file control test behavior:

| Directive                                                 | Description                                                                                                                                                      |
|-----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `// RUN_PIPELINE_TILL: FRONTEND`, `CODEGEN` or `FIR2IR`   | How far the compiler pipeline runs (FRONTEND = FIR resolution only, CODEGEN = through codegen, FIR2IR = rarely once IR has been created, but failing at backend) |
| `// ISSUE: KT-XXXXX`                                      | References a YouTrack issue                                                                                                                                      |
| `// WITH_STDLIB`                                          | Include stdlib in test classpath                                                                                                                                 |
| `// LANGUAGE: +FeatureName` / `// LANGUAGE: -FeatureName` | Enable/disable language features                                                                                                                                 |
| `// DIAGNOSTICS: -DIAGNOSTIC_NAME`                        | Suppress specific diagnostics                                                                                                                                    |
| `// RENDER_DIAGNOSTICS_FULL_TEXT`                         | Produces `.diag.txt` with human-readable error messages                                                                                                          |
| `// RENDER_DIAGNOSTIC_ARGUMENTS`                          | Renders arguments inside markers, e.g. `<!TYPE_MISMATCH("A; B")!>`                                                                                               |
| `// DUMP_CFG` / `// DUMP_CFG: FLOW`                       | Generates `.dot` file with control flow graph                                                                                                                    |
| `// DUMP_INFERENCE_LOGS: option1, option2`                | Possible options: FIXATION, MARKDOWN, MERMAID                                                                                                                    |
| `// CHECK_TYPE`                                           | Enables `checkType { _<Type>() }` pattern for type assertions                                                                                                    |
| `// FILE: Name.kt` / `// FILE: Name.java`                 | Multi-file test (splits single `.kt` file into virtual files)                                                                                                    |
| `// LANGUAGE_FEATURE_TOGGLED: FeatureName`                | The feature is enabled; `FirLightTreeDiagnosticsWithLanguageFeatureDisabledTestGenerated` runs the test with it disabled (see `.disabled.kt` below) |
| `// LL_FIR_DIVERGENCE`                                    | Opens and closes the comment at the top of an `.ll.kt` (`.ll.wasmJs.kt`, …) explaining why LL FIR reports other diagnostics than the compiler                     |

### Inline Diagnostic Markers

Diagnostic assertions are placed inline around the code that should produce them:

- `<!DIAGNOSTIC_NAME!>code<!>` — asserts `code` produces that diagnostic
- `<!DIAGNOSTIC_NAME("arg1; arg2")!>code<!>` — with diagnostic arguments (needs `RENDER_DIAGNOSTIC_ARGUMENTS`)
- `<!DIAG1, DIAG2!>code<!>` — multiple diagnostics on same code span

### Debug Info Markers

- `<!DEBUG_INFO_CALL("fqName: ...; typeCall: ...")!>call<!>` — asserts call resolution target
- `<!DEBUG_INFO_EXPRESSION_TYPE("type")!>expr<!>` — asserts expression type

### Footer

- `/* GENERATED_FIR_TAGS: tag1, tag2, ... */` — auto-generated tags listing FIR constructs present in the file; do not hand-edit

## Associated Files (auto-generated, not hand-edited)

| Extension        | Content                                                             | Generated when                                    |
|------------------|---------------------------------------------------------------------|---------------------------------------------------|
| `.fir.txt`       | FIR tree dump (resolved FIR with `R\|...\|` references)             | Only when `FIR_DUMP` directive is present         |
| `.diag.txt`      | Diagnostics in text format (`/file.kt:(offset): severity: message`) | `RENDER_DIAGNOSTICS_FULL_TEXT` directive present  |
| `.dot`           | Control flow graph in Graphviz format                               | `DUMP_CFG` directive present                      |
| `.fixation.txt`  | Type variable fixation process                                      | `DUMP_INFERENCE_LOGS: FIXATION` directive present |
| `.inference.md`  | General type inference logs                                         | `DUMP_INFERENCE_LOGS: MARKDOWN` directive present |
| `.inference.mmd` | General type inference logs via Mermaid format                      | `DUMP_INFERENCE_LOGS: MERMAID` directive present  |
| `.latestLV.kt`   | Test expectations for latest language version when they differ from stable | Created by `update` when they differ     |
| `.disabled.kt`   | Test expectations with the toggled language feature disabled        | Created by `update` when they differ              |

The `.kt` file has variants for other test configurations (`.ll.kt`, `.reversed.kt`, …), see [Variant files](#variant-files).

Diagnostic messages rendered into `.diag.txt` must follow the [Diagnostic Message Style Guide](../../../docs/diagnostic_message_style_guide.md).

## Latest Language Version Differences

When a language feature has `sinceVersion` set to a future Kotlin version (e.g., `KOTLIN_2_5`), it is disabled at the stable language version but enabled at the latest language version. This causes different compiler behavior depending on which LV the test runs with.

The latest-LV test runners in `:compiler:jvm.tests` (`FirLightTreeDiagnosticsWithLatestLanguageVersionTestGenerated`, and
`FirOldFrontendMPPDiagnosticsWithLightTreeWithLatestLanguageVersionTestGenerated` for `multiplatform/`) are managed tests with the
`latestLV` variant: the `.kt` file keeps the expectations for the stable (default) language version, and `update` creates `.latestLV.kt`
with the expectations for the latest one when they differ, and deletes it when they don't.

## Creating a New Test

1. **Create the `.kt` test file** with appropriate directives and test code.
2. **Regenerating diagnostic test runners is not required**: They are regenerated on demand when running tests.
3. **Update the test data** of the compiler and LL tests (see [Updating Test Data](#updating-test-data)):
   ```bash
   python3 analysis/test-data-manager/test-data-manager.py update --path compiler/testData/diagnostics/tests/resolve/problems/myTest.kt
   ```
   It writes the diagnostic markers, the `GENERATED_FIR_TAGS` footer and the `RUN_PIPELINE_TILL` directive (a missing one, or a promoted
   one) into the `.kt` file, and creates the variants whose output differs from it.
4. **Review the result** with `git diff`: a passed `update` needs no `check`. A created `.ll.kt` fails the run (a `TEST ERROR`) until its
   `LL_FIR_DIVERGENCE` directive is added; then `update --rerun <runId>`.
5. **Run the unmanaged tests** whose test data the test is in, see [Unmanaged tests](#unmanaged-tests).

### Important notes

- **`RUN_PIPELINE_TILL` must match actual test needs.** If the test has no expected diagnostics/errors, the framework requires `CODEGEN`. Using `FRONTEND` when `CODEGEN` is possible causes a "Phase FRONTEND could be promoted to CODEGEN" failure in a plain test run; `update` promotes the directive.
- **`GENERATED_FIR_TAGS` are written by `update`** (step 3) when absent. Do not add them manually. A plain test run only fails on missing tags.
- **`.fir.txt` is NOT generated by default.** It requires a `// FIR_DUMP` directive. Most tests (especially simple regression tests) do not need it.
- **stdlib is not available by default.** Functions like `println` will produce `UNRESOLVED_REFERENCE` without the `// WITH_STDLIB` directive.
- **`-Pkotlin.test.update.test.data=true`** updates the files of the [unmanaged tests](#unmanaged-tests), but not the files `updateTestData` manages (the `.kt` file of the managed tests, its variants and their dumps).

## Updating Test Data

### Managed tests

These tests are managed by the [test data manager](../../../analysis/test-data-manager/README.md):

- `PhasedJvmDiagnosticLightTreeTestGenerated` (`:compiler:jvm.tests`) over `tests/`, `testsWithAnyBackend/` and `testsWithStdLib/`: the
  golden test of their `.kt` files; `PhasedJvmDiagnosticLightTreeWithContextSensitiveEnabledTestGenerated` over
  `tests/contextSensitiveResolutionUsingExpectedType/ideHint/`; the latest-LV runners (see
  [Latest Language Version Differences](#latest-language-version-differences)); `FirLightTreeDiagnosticsWithLanguageFeatureDisabledTestGenerated`
  over the tests with `LANGUAGE_FEATURE_TOGGLED`, which runs them with the feature disabled;
  `FirLightTreeDiagnosticsWithoutAliasExpansionTestGenerated` over the tests with type aliases, which runs them without type alias
  expansion.
- The LL diagnostic tests (`:analysis:low-level-api-fir:low-level-api-fir-compiler-tests`): `LLDiagnosticsTestGenerated`,
  `LLReversedDiagnosticsTestGenerated`, `LLPartialDiagnosticsTestGenerated`, and their JS and Wasm suites over `testsWithJsStdLib/`,
  `wasmTests/` and `wasmWasiTests/` (`LLJsDiagnosticsFe10TestGenerated`, `LLWasmJsDiagnosticsFe10TestGenerated`, …).
- The spec diagnostic tests over `compiler/tests-spec/testData/diagnostics`: `FirLightTreeDiagnosticTestSpecGenerated` (golden),
  `LLSpecTestGenerated`, `LLReversedSpecTestGenerated`.
- The foreign annotations tests over `foreignAnnotationsTests/`: `FirPsiOldFrontendForeignAnnotationsSourceJavaTestGenerated` (golden),
  `FirPsiOldFrontendForeignAnnotationsCompiledJavaTestGenerated` and
  `FirPsiOldFrontendForeignAnnotationsCompiledJavaWithPsiClassReadingTestGenerated`.
- The diagnostic tests of the compiler plugins with their LL counterparts.

They are updated and checked only with the [wrapper script](../../../analysis/AGENTS.md#working-with-test-data): `update` updates their test
data, `check` checks it without writing anything. `--path` picks the modules, so one run covers the compiler and the LL tests of a file.

### Variant files

The `.kt` file is the golden test data; a test whose output differs from it uses a variant file with the variant before the extension:

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

The Wasm WASI tests use `wasmWasi` instead of `wasmJs`, and the LL JS tests use the plain LL chains. A script gets the variant before its
whole extension: `foo.ll.kts`, `foo.ll.test.kts`, `foo.ll.repl.kts`. A test reads the first existing file and writes only the first one
(its own variant); `updateTestData` creates it when the output differs from the file it falls back to, and deletes it when they are
identical, or when an `.ll.kt` differs only in the `LL_FIR_DIVERGENCE` directive or in `METADATA` tags. A new `.ll.kt` or `.ll.wasmJs.kt`
fails until its `LL_FIR_DIVERGENCE` directive is added by hand: the divergence of LL from the compiler needs an explanation. Muted tests
(`MUTE_LL_FIR`, `IGNORE_REVERSED_RESOLVE`, a `.fail` file, …) and tests whose analysis crashed are only checked.

The `GENERATED_FIR_TAGS` edits of the compiler test go into the `.kt` file and all its existing variants except `.disabled.kt`. Its
`RUN_PIPELINE_TILL` edits also skip `.contextSensitive.kt` and `.noAliasExpansion.kt`: a test with such a variant may stop at another
phase, e.g., with the feature disabled, so `update` writes its `RUN_PIPELINE_TILL` into its own variant, creating or deleting the variant
as for the diagnostics.

The compiler tests run first, as the LL tests fall back to their `.kt` files. Don't pass `--incremental`: it doesn't see the changes made by
another module's run; narrow the run with `--path` instead.

### Dumps

The dumps of a managed test (`.fir.txt`, `.dot`, `.overrides.txt`, inference logs, `.cli.out`, …) follow the same chains as its `.kt` file,
with the variant before the dump extension: the reversed LL test reads `foo.reversed.fir.txt` → `foo.ll.fir.txt` → `foo.fir.txt` and writes
only `foo.reversed.fir.txt`. `update` writes them like the `.kt` file, and not after a crash. An LL test dumps the FIR without `FIR_DUMP` only
if its own dump exists, e.g., `foo.ll.fir.txt`. The LL tests don't compare `.diag.txt`, and the test without type alias expansion, a
sanity check of the diagnostics, compares neither `.fir.txt` nor `.diag.txt`.

### Unmanaged tests

Other diagnostic tests read the same test data but are not managed: they compare with the file they read, and a Gradle run with
`-Pkotlin.test.update.test.data=true` writes that file.

- The JS, Wasm and Native compiler diagnostic tests (`js/js.tests`, `wasm/wasm.tests`, `native/native.tests`). They also cover
  `testsWithAnyBackend/`, and the Wasm ones `tests/defaultArguments/`: run them after `update` changes a `.kt` file there.
- The metadata diagnostic tests (`AbstractMetadataDiagnosticTest` in `:compiler:tests-common-new`) over `metadataDiagnosticTests/`, and
  the JS diagnostic tests of the js-plain-objects plugin (`AbstractFirJsPlainObjectsPluginDiagnosticTest`).
- The native LL tests (`LLNativeDiagnosticsTestGenerated`, `LLReversedNativeDiagnosticsTestGenerated`).

The `.kt` files of `testsWithJsStdLib/`, `wasmTests/`, `wasmWasiTests/`, `metadataDiagnosticTests/` and
`plugins/js-plain-objects/compiler-plugin/testData/diagnostics/` are written only by these compiler tests: update them first, then run
`update` for the LL variants, which are created against the `.kt` they fall back to.

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
