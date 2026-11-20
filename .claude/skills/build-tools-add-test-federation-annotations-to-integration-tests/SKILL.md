---
name: build-tools-add-test-federation-annotations-to-integration-tests
description: Annotate test classes in `kotlin-gradle-plugin-integration-tests` with Test Federation annotations (`@MustRunOnChangesInXYZ` / `@MustRunAlways`). Use this whenever the user asks to annotate, review, or fix Test Federation annotations on an integration test class, or to assess a test class for cross-domain dependencies under Test Federation (KT-87614).
disable-model-invocation: true
---

# Add Test Federation annotations to `kotlin-gradle-plugin-integration-tests`

Every test class in `libraries/tools/kotlin-gradle-plugin-integration-tests` must be assessed and annotated for
Test Federation (no class should be left unannotated). This skill encodes the per-class analysis procedure and the
conventions agreed on while annotating this module (ticket **KT-87614**).

## Reference material

Read these before assessing any class:

- `repo/TEST-FEDERATION.md` — how domain selection and `@MustRunOnChangesInXYZ`/`@MustRunAlways` work.
- `repo/test-runtime/src/main/kotlin/org/jetbrains/kotlin/test/federation/annotations.kt` — annotation source/KDoc.
- `repo/test-runtime/src/main/generated/annotations.kt` — the generated, concrete `@MustRunOnChangesIn<Domain>` annotations (one per domain).
- `repo/domains.yaml` — domain definitions; confirms which domain a module path belongs to (this module's path already maps to `Gradle`).
- `libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/testTags.kt` - defines common test annotations, some of which are annotated with Test Federation annotations.

## Per-class procedure

Input can be a single test class or a list of test classes; all of them live under
`libraries/tools/kotlin-gradle-plugin-integration-tests`. Apply the following procedure to each one:

1. **Read every test method's logic**, not just its name. Identify which domain(s) it actually exercises — not just
   `Gradle`, but also `Frontend`, `Backend`, `Native`, `Js`, `Wasm`, `CoreLibs`, `CompilerPlugins`, etc. — based on what
   compiler/backend behavior the assertions depend on (e.g. asserting on native linker output ⇒ `Native`; asserting on
   `npm`/`package.json` or klib web resources ⇒ `Js`/`Wasm`; invoking a compiler plugin extension ⇒ `CompilerPlugins`).
2. **A domain-specific annotation requires a genuine dependency on that domain's core logic, not just
   domain-specific configuration/assertions in the test.** A test can configure or assert on a specific target/platform
   (e.g. Swift Export, JS, Native) and still be purely a `Gradle` test if what it actually verifies is Gradle-side
   publication/consumption/task-orchestration behavior (e.g. checking that expected output files exist, that a task
   executed, that no diagnostic was raised) rather than depending on that domain's compiler/backend internals. Example:
   `KotlinArchiveSwiftExportConsumptionIT` configures Swift Export and checks Swift Export artifacts exist, but the
   logic under test is Gradle publication/consumption of the Kotlin Archive format — it only needs
   `@MustRunOnChangesInGradle`, not a Swift-Export/Native-specific annotation.
3. **Always add `@MustRunOnChangesInGradle` at class level.** Even though the module's path already belongs to the
   `Gradle` domain in `domains.yaml` (so every test here already reruns on `Gradle` changes by default), always add an
   explicit class-level `@MustRunOnChangesInGradle`, mirroring the existing precedent in `GradleWarningsDetectorIT.kt`.
   It's technically redundant with the default domain rule, but kept for explicitness/documentation, and is required
   by the "annotate everything" policy. This is the simplest, uniform baseline — apply it to every class regardless of
   whether domain-specific annotations are also needed.
4. **Never leave a class unannotated as a final answer.** The goal is to annotate every test class; at minimum it
   always gets the class-level `@MustRunOnChangesInGradle` from point 3.
5. **Avoid `@MustRunAlways`.** Reserve it only for fundamental "always must pass" smoke tests (e.g.
   `SimpleKotlinGradleIT`/`KotlinGradleIT`). For ordinary feature/behavior tests, pick the most concrete
   `@MustRunOnChangesIn<Domain>` instead — do not use `@MustRunAlways` as a catch-all.
6. **Do not add domain-specific annotations if existing test tags annotations already include them**. Check for  
   test tags annotations on test classes and methods, and look them up in `testTags.kt`: if they already include Test
   Federation annotations, do not add them. Example: tests annotated with `@NativeGradlePluginTests`
   are automatically annotated with `@MustRunOnChangesInNative`, since `@NativeGradlePluginTests` is itself
   annotated with `@MustRunOnChangesInNative` - no need to add it again.
7. **Domain-specific annotations — add at class or method level where necessary, on top of the baseline
   `@MustRunOnChangesInGradle`:** if some or all tests in the class also depend on a domain outside `Gradle` (per
   point 2), add the corresponding `@MustRunOnChangesIn<Domain>` annotation(s) in addition to the class-level
   `@MustRunOnChangesInGradle` from point 3. Do not treat a domain-specific annotation as a substitute for the
   `Gradle` one — both are kept.
8. **Class-level vs. per-method for domain-specific annotations:** place a domain-specific annotation on the class
   when every test in the class shares the same domain dependency. Switch to **per-method** domain-specific
   annotations when tests differ in what they exercise — annotate each test method with only the domain(s) it
   actually depends on, rather than over-annotating the whole class. When asked to convert class-level to per-method,
   re-derive each method's domain(s) from its own assertions rather than reusing the class-level set uniformly.
   **If every method already carries its own domain-specific annotation(s), do not also repeat that domain-specific
   annotation at the class level** — it would be redundant. `@MustRunOnChangesInGradle` is the exception and always
   stays at the class level (see point 3), since it applies uniformly regardless of per-method domain differences.
9. **Imports:** add the corresponding `org.jetbrains.kotlin.testFederation.MustRunOnChangesIn<Domain>` import(s), keeping
   import ordering consistent with the file's existing style.

## Workflow

- If input is a list of test classes, work on every class sequentially, not in parallel.
- Before proceeding to the next class, ask the user to validate the changes and commit them manually.
- Wait for the user's approval before proceeding to the next class.
- Never commit changes automatically.

## Running table

Maintain a running table across the whole session/effort, with exactly these 3 columns, and append one row per class
assessed (keep "Changes made" very brief — a few words, not a sentence):

| Test class | Status | Changes made |
|---|---|---|
| `DifferentClassloadersIT` | Annotated | Added `@MustRunOnChangesInGradle` |
| `KaptCompilerPluginsIT` | Already annotated | None (pre-existing) |
| `PersistentCacheDirIT` | Annotated | Added `@MustRunOnChangesInGradle` |
| `KotlinArchiveConsumptionIT` | Annotated (per-method) | Per-method `Native`/`Js`/`Wasm`; class-level `@MustRunOnChangesInGradle` |
| `KotlinArchiveSwiftExportConsumptionIT` | Annotated | Added `@MustRunOnChangesInGradle` only (Gradle-only logic) |

Use `Annotated` / `Already annotated` / `Annotated (per-method)` etc. for Status.

Print the table at the end of the run in Markdown format, so it can be included in the pull request description.

## Verification

This is an annotation-only change with no behavioral impact, so a build/test run is not required. It's enough to
visually re-review the final file for correct import ordering and annotation placement matching the module's conventions.
