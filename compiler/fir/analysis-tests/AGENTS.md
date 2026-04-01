# FIR Analysis Tests

Location: `compiler/fir/analysis-tests/testData/resolve/`

## Test File Format (`.kt` files)

### Header Directives

Comment lines at the top of the file control test behavior:

| Directive                                                 | Description                                                                                                                                                      |
|-----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `// RUN_PIPELINE_TILL: FRONTEND`, `BACKEND` or `FIR2IR`   | How far the compiler pipeline runs (FRONTEND = FIR resolution only, BACKEND = through codegen, FIR2IR = rarely once IR has been created, but failing at backend) |
| `// ISSUE: KT-XXXXX`                                      | References a YouTrack issue                                                                                                                                      |
| `// WITH_STDLIB`                                          | Include stdlib in test classpath                                                                                                                                 |
| `// LANGUAGE: +FeatureName` / `// LANGUAGE: -FeatureName` | Enable/disable language features                                                                                                                                 |
| `// DIAGNOSTICS: -DIAGNOSTIC_NAME`                        | Suppress specific diagnostics                                                                                                                                    |
| `// RENDER_DIAGNOSTICS_FULL_TEXT`                         | Produces `.fir.diag.txt` with human-readable error messages                                                                                                      |
| `// RENDER_DIAGNOSTIC_ARGUMENTS`                          | Renders arguments inside markers, e.g. `<!TYPE_MISMATCH("A; B")!>`                                                                                               |
| `// DUMP_CFG` / `// DUMP_CFG: FLOW`                       | Generates `.dot` file with control flow graph                                                                                                                    |
| `// DUMP_INFERENCE_LOGS: option1, option2`                | Possible options: FIXATION, MARKDOWN, MERMAID                                                                                                                    |
| `// CHECK_TYPE`                                           | Enables `checkType { _<Type>() }` pattern for type assertions                                                                                                    |
| `// FILE: Name.kt` / `// FILE: Name.java`                 | Multi-file test (splits single `.kt` file into virtual files)                                                                                                    |
| `// LATEST_LV_DIFFERENCE`                                 | Indicates test expectations differ between stable and latest language version (see `.latestLV.kt` below)                                                         |

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
| `.fir.diag.txt`  | Diagnostics in text format (`/file.kt:(offset): severity: message`) | `RENDER_DIAGNOSTICS_FULL_TEXT` directive present  |
| `.dot`           | Control flow graph in Graphviz format                               | `DUMP_CFG` directive present                      |
| `.fixation.txt`  | Type variable fixation process                                      | `DUMP_INFERENCE_LOGS: FIXATION` directive present |
| `.inference.md`  | General type inference logs                                         | `DUMP_INFERENCE_LOGS: MARKDOWN` directive present |
| `.inference.mmd` | General type inference logs via Mermaid format                      | `DUMP_INFERENCE_LOGS: MERMAID` directive present  |
| `.latestLV.kt`   | Test expectations for latest language version when they differ from stable | `LATEST_LV_DIFFERENCE` directive present     |

## Latest Language Version Differences

When a language feature has `sinceVersion` set to a future Kotlin version (e.g., `KOTLIN_2_5`), it is disabled at the stable language version but enabled at the latest language version. This causes different compiler behavior depending on which LV the test runs with.

To handle this:
1. Add `// LATEST_LV_DIFFERENCE` directive to the `.kt` test file
2. Create a `.latestLV.kt` file (for diagnostics tests: `.fir.latestLV.kt` if only FIR behavior differs) containing the full test with diagnostic expectations for the latest LV
3. The base `.kt` file keeps expectations for the stable (default) language version

The latest-LV test runners (`FirLightTreeDiagnosticsWithLatestLanguageVersionTestGenerated`, `FirLightTreeOldFrontendDiagnosticsWithLatestLanguageVersionTestGenerated`) use the `.latestLV.kt` file instead of the base `.kt` file.

Run with `-Pkotlin.test.update.test.data=true` to auto-generate/update `.latestLV.kt` content.

## Creating a New Test

1. **Create the `.kt` test file** with appropriate directives and test code.
2. **Regenerate test runners**: `./gradlew generateTests` — updates `*Generated.java` files to include the new test method.
3. **Run the test once** (it will fail):
   ```bash
   ./gradlew :compiler:fir:analysis-tests:test --tests "org.jetbrains.kotlin.test.runners.PhasedJvmDiagnosticLightTreeTestGenerated\$Resolve\$Problems.testMyTest"
   ```
   This first run auto-generates the `GENERATED_FIR_TAGS` footer in the `.kt` file. No special flags are needed — tags are written automatically on first run.
4. **Run the test again** — it should now pass.

### Important notes

- **`RUN_PIPELINE_TILL` must match actual test needs.** If the test has no expected diagnostics/errors, the framework requires `BACKEND`. Using `FRONTEND` when `BACKEND` is possible causes a "Phase FRONTEND could be promoted to BACKEND" failure.
- **`GENERATED_FIR_TAGS` are written automatically** on the first test run when absent. Do not add them manually. Just run the test, let it fail and write the tags, then run again.
- **`.fir.txt` is NOT generated by default.** It requires a `// FIR_DUMP` directive. Most tests (especially simple regression tests) do not need it.
- **stdlib is not available by default.** Functions like `println` will produce `UNRESOLVED_REFERENCE` without the `// WITH_STDLIB` directive.
- **`-Pkotlin.test.update.test.data=true`** updates handler-generated files (like `.fir.txt`) but does NOT update the `.kt` source file itself (tags, diagnostic markers). The `.kt` file is updated by the test framework's own `TagsGeneratorChecker` on a normal test run.

## Directory Structure at `testData/resolve/`

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
