# Working with the KotlinToolingDiagnostics framework

This document is a practical guide for internal and external Kotlin Gradle Plugin (KGP) contributors on 
how to work with the tooling diagnostics infrastructure located in the
`org.jetbrains.kotlin.gradle.plugin.diagnostics` package (module `kotlin-gradle-plugin`, with the severity
enum living in `kotlin-gradle-plugin-api`).

Diagnostics are the standard way for KGP to report warnings and errors to users
instead of throwing a raw `GradleException`. They support severities, suppression, structured
content (title/description/solutions/documentation link), deduplication, and IDE-aware rendering.

## Anatomy of a diagnostic

A diagnostic consists of:
- **id** — a stable, unique string used for suppression and for finding the diagnostic in tests
  (defaults to the factory's simple class name).
- **group** — a `DiagnosticGroup` used for categorization (see `ToolingDiagnosticGroup.kt`).
  Predefined KGP groups are `DiagnosticGroup.Kgp.Default`, `.Deprecation`, `.Misconfiguration`,
  `.Experimental`.
- **severity** — one of `KotlinToolingDiagnosticsSeverity`:
  - `WARNING` — non-critical, doesn't fail anything.
  - `STRONG_WARNING` — displayed like an error but doesn't fail the build.
  - `ERROR` — fails the build (via a dedicated task), but IDE sync/non-compiling tasks still succeed.
  - `FATAL` — aborts the current process (configuration/sync) immediately. Use *extremely* sparingly.
- **title**, **description**, **solutions** (one or more actionable suggestions), and an optional
  **documentation link**.

## Adding a new diagnostic

All diagnostics live as nested objects/classes inside the
`KotlinToolingDiagnostics` object in `KotlinToolingDiagnostics.kt`. Each one extends
`ToolingDiagnosticFactory(severity, group)` and exposes an `operator fun invoke(...)` (or a plain
method) that builds the diagnostic using the `title().description().solution(s)()` DSL
(see `ToolingDiagnosticFactory.kt` / `ToolingDiagnostics` builder).

Minimal example, a `WARNING` with a single solution:

```kotlin
internal object KotlinToolingDiagnostics {
    // ...

    object MyNewFeatureMisusedWarning : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(offendingTargetName: String) = build {
            title("Incorrect usage of myNewFeature()")
                .description(
                    "Target '$offendingTargetName' declares myNewFeature(), but this is only " +
                            "supported for JVM targets."
                )
                .solution("Remove myNewFeature() from the target or convert it to a JVM target.")
                .documentationLink(URI("https://kotl.in/my-new-feature")) { url ->
                    "See $url for more details."
                }
        }
    }
}
```

Notes:

- If severity should be configurable by the caller (e.g., warning vs. error depending on context),
  make it a constructor parameter of the factory, as done for
  `UklibPublicationWithoutCrossCompilation(severity)` or `KotlinTargetAlreadyDeclared(severity)`.
- Use `buildDiagnostic(title, description, solution/solutions, ...)` instead of `build { ... }` when
  you don't need the full builder DSL (e.g., no computed/multipart description).
- Prefer a single-sentence, actionable `solution`; the builder enforces solutions to be single-line
  (`checkSolutionIsSingleLine`).
- If the same factory needs to produce several distinct diagnostics (e.g., parameterized by a target
  or property name), pass an `idSuffix` to `build(idSuffix = ...)` so each variant gets a unique,
  stable id (`"${id}_$idSuffix"`) — this keeps suppression per-variant.
- Choose the severity conservatively: prefer `WARNING`/`ERROR` over `FATAL`. `FATAL` should be
  reserved for cases where continuing configuration is impossible or unsafe (see the doc comment on
  `KotlinToolingDiagnosticsSeverity.FATAL`).

## Reporting a diagnostic

Diagnostics are reported through the `KotlinToolingDiagnosticsCollector` Gradle `BuildService`,
never by throwing directly (except implicitly for `FATAL`, see below).

The most common way, from a `Project`:

```kotlin
project.reportDiagnostic(KotlinToolingDiagnostics.MyNewFeatureMisusedWarning(target.name))
```

Deduplication helpers avoid flooding the log when many source sets/targets can trigger the same
diagnostic:

```kotlin
// once per Gradle project (module)
project.reportDiagnosticOncePerProject(diagnostic)

// once per the whole Gradle build (root + all subprojects)
project.reportDiagnosticOncePerBuild(diagnostic)
```

These are convenience wrappers around
`KotlinToolingDiagnosticsCollector.reportOncePerGradleProject/reportOncePerGradleBuild`, which key
deduplication by the diagnostic id (or an explicit `key` you provide).

If you're inside a *checker* (see below) that already has access to a
`KotlinGradleProjectCheckerContext`, report through the context's collector/project the same way.

Where to report from matters:

- **Most diagnostics should be reported from a "project checker"** — see
  `KotlinGradleProjectChecker`/`KotlinGradleProjectCheckersRunner`. Checkers registered in
  `ALL_CHECKERS` run once the whole build script/DSL has been applied
  (`KotlinPluginLifecycle.Stage.ReadyForExecution`), so they observe the *final* configuration
  instead of a partial one. This is the reason the framework exists instead of eagerly
  throwing during `apply {}` — see the class-level KDoc on `KotlinGradleProjectChecker`.
- If a check must prevent an exception thrown *during* configuration (e.g., an inevitable
  `NullPointerException` a bit later), report as early as possible in the `apply`-block instead of
  waiting for `ReadyForExecution`.
- Diagnostics reported at execution time (from a task action) use
  `KotlinToolingDiagnosticsCollector.report(from: UsesKotlinToolingDiagnosticsParameters, ...)`,
  which renders immediately instead of buffering.

Only `FATAL` diagnostics throw immediately, from inside
`KotlinToolingDiagnosticsCollector.handleDiagnostic`. `ERROR`/`STRONG_WARNING` are collected and
later aggregated by the `checkKotlinGradlePluginConfigurationErrors` task
(`CheckKotlinGradlePluginConfigurationErrors.kt`), which Kotlin compile tasks depend on — so a
build that doesn't need to compile Kotlin can still succeed, and IDE sync always gets a full
project model. `WARNING` never fails anything.

## Writing a new checker

If your diagnostic should run once per project after the whole configuration is known, add it as a
`KotlinGradleProjectChecker`:

```kotlin
internal object MyNewFeatureChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks() {
        val target = multiplatformExtension?.targets?.findByName("myTarget") ?: return
        if (target.usesMyNewFeatureIncorrectly()) {
            project.reportDiagnosticOncePerProject(
                KotlinToolingDiagnostics.MyNewFeatureMisusedWarning(target.name)
            )
        }
    }
}
```

...and register it in `KotlinGradleProjectChecker.ALL_CHECKERS`.

## Testing diagnostics

Diagnostics are tested with plain unit/functional tests in
`kotlin-gradle-plugin/src/functionalTest`, using helpers from
`org.jetbrains.kotlin.gradle.util` (`diagnosticUtils.kt`). Build a project with
`buildProjectWithMPP { ... }` (or a similar helper), configure it, `evaluate()` it, then assert.

### Asserting a diagnostic was (or wasn't) reported

```kotlin
class MyNewFeatureCheckerTest {

    @Test
    fun `reports warning when myNewFeature is misused`() {
        val project = buildProjectWithMPP {
            project.multiplatformExtension.jvm()
            // ... configure to trigger the misuse ...
        }
        project.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.MyNewFeatureMisusedWarning)
    }

    @Test
    fun `does not report when configured correctly`() {
        val project = buildProjectWithMPP {
            // ... correct configuration ...
        }
        project.evaluate()

        project.assertNoDiagnostics()
        // or, to only check one factory while other diagnostics may be present:
        // project.assertNoDiagnostics(KotlinToolingDiagnostics.MyNewFeatureMisusedWarning)
    }
}
```

Useful assertions (all in `diagnosticUtils.kt`):

- `Project.assertContainsDiagnostic(factory, idSuffix = "")` — checks a diagnostic with the given
  id (ignoring its parameters/message) was reported; returns the found `ToolingDiagnostic` so you
  can inspect it further (e.g. `found.message`).
- `Project.assertContainsDiagnostic(diagnostic, ignoreThrowable = false)` — checks an *exact*
  diagnostic instance (id + message + severity, and optionally the throwable) was reported; build
  the expected instance the same way production code does, e.g.
  `KotlinToolingDiagnostics.MyNewFeatureMisusedWarning("jvm")`.
- `Project.assertNoDiagnostics()` / `Project.assertNoDiagnostics(factory)` — asserts no diagnostics
  (or none with the given factory's id) were reported. `assertNoDiagnostics()` filters out a few
  environment-specific diagnostics by default (`defaultFilteredDiagnostics`, e.g., outdated Kotlin/Native
  version warnings that are noise in the test environment).
- `Collection<ToolingDiagnostic>.assertContainsSingleDiagnostic(factory)` — asserts *exactly one*
  diagnostic with that id was reported and returns it.
- `Collection<ToolingDiagnostic>.assertDiagnostics(vararg diagnostics)` — asserts the collection
  is exactly the given set of diagnostics (useful when checking several diagnostics at once).

If your diagnostic is `FATAL` and expected to abort evaluation, wrap `project.evaluate()` in
`assertFails { ... }` first, then assert on what was collected — see
`DuplicateSourceSetCheckerTest`:

```kotlin
@Test
fun `target with custom name duplicates default name fails build`() {
    val project = buildProjectWithMPP {
        project.multiplatformExtension.applyDefaultHierarchyTemplate()
        project.multiplatformExtension.linuxX64("linUX")
    }
    assertFails { project.evaluate() }
    project.checkDiagnostics("DuplicateSourceSetChecker")
}
```

### Golden file-based testing

For diagnostics with a non-trivial rendered message (multi-line description, several solutions), it
is often more convenient to snapshot-test the exact rendered text via
`Project.checkDiagnostics(testDataName)`. It renders all diagnostics collected for the project (or
for all subprojects, if there's more than one with diagnostics) and compares the result against a
golden file at
`kotlin-gradle-plugin/src/functionalTest/resources/expectedDiagnostics/<testDataName>.txt`.

Example golden file (`expectedDiagnostics/DuplicateSourceSetChecker.txt`):

```
[DuplicateSourceSetsError | FATAL] Duplicate Kotlin Source Sets Detected
Duplicate Kotlin source sets have been detected: [linUXMain, linuxMain], [linUXTest, linuxTest]. Keep in mind that source set names are case-insensitive, which means that `srcMain` and `sRcMain` are considered the same source set.
Please rename the duplicated source sets.
```

If the file doesn't exist yet, run the test once — `TestDataAssertions.assertEqualsToFile` will
report a diff/create-file hint you can use to populate the initially expected content (make sure to
review it for correctness before committing). For multi-project setups, use
`checkDiagnosticsWithMppProject(expectedDiagnosticsFile) { ... }` which builds and configures the
project for you.

By default, `checkDiagnostics` filters out a couple of environment-noise diagnostics
(`OldNativeVersionDiagnostic`, `DisabledNativeTargetTaskWarning`) — pass a custom
`filterDiagnosticIds` list if you need different filtering.

### Integration tests

For diagnostics that depend on the exact Gradle interaction (rather than KGP's own model), add
an integration test under `kotlin-gradle-plugin-integration-tests` (a `@GradleTest` running against a
real Gradle build, e.g., extending `KGPBaseTest`) and assert on the build output (the `w:`/`e:`
prefixed lines, or the IDE build-log `warning:`/`error:` prefixed lines — see
`renderReportedDiagnostics.kt`) instead of/in addition to a functional test.

Example, adapted from `MppDiagnosticsIt.kt`, which reports a diagnostic via `buildScriptInjection`
and checks it end-to-end (including that it fails the relevant task):

```kotlin
internal object StrongWarningDiagnostic : ToolingDiagnosticFactory(STRONG_WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
    operator fun invoke() = build {
        title("Foo")
            .description("bar")
            .solution("baz")
    }
}

@GradleTest
fun testStrongWarningDiagnostic(gradleVersion: GradleVersion) {
    project("empty", gradleVersion) {
        plugins {
            kotlin("multiplatform")
        }
        buildScriptInjection {
            project.applyMultiplatform {
                jvm()
            }
            project.reportDiagnostic(StrongWarningDiagnostic())
        }
        build(":checkKotlinGradlePluginConfigurationErrors") {
            assertHasDiagnostic(StrongWarningDiagnostic)
            assertTasksExecuted(":checkKotlinGradlePluginConfigurationErrors")
        }
    }
}
```

Useful assertions here (in `testbase/diagnosticsAssertions.kt`, available on both `BuildResult` and
raw `String` output):

- `BuildResult.assertHasDiagnostic(factory, withSubstring = null, expectedSeverity = null)` — asserts
  a diagnostic with the factory's id was rendered in the output; optionally also asserts the rendered
  message contains `withSubstring` and/or was reported with specific severity.
- `BuildResult.assertNoDiagnostic(factory, withSubstring = null)` — the negative counterpart; asserts
  no diagnostic with that id (or none containing `withSubstring`) was rendered.
- `BuildResult.extractProjectsAndTheirDiagnostics()` / `extractProjectsAndTheirDiagnosticsInBlocks()`
  — extract just the diagnostics blocks from the full build log (grouped per subproject), useful for
  golden-file comparisons via `assertEqualsToFile(expectedOutputFile(), extractProjectsAndTheirDiagnostics())`
  when you want to pin the *exact* rendered text, similarly to `checkDiagnostics` in functional tests.
- Combine with regular integration-test assertions as needed, e.g. `assertTasksExecuted`/
  `assertTasksSkipped` (to confirm `checkKotlinGradlePluginConfigurationErrors` did or didn't run) and
  `buildAndFail(...)` (to confirm an `ERROR`/`FATAL` diagnostic actually fails the build).

As with functional tests, cover both the positive case (diagnostic reported, correct severity/message)
and the negative case (diagnostics absent when the project is configured correctly).

## Checklist for a new diagnostic

1. Pick the right severity (default to `WARNING`/`ERROR`; avoid `FATAL` unless truly unavoidable).
2. Add the diagnostic factory to `KotlinToolingDiagnostics.kt` with a clear title, description, and
   at least one actionable solution; add a documentation link if applicable.
3. Report it through `reportDiagnostic`/`reportDiagnosticOncePerProject`/`reportDiagnosticOncePerBuild`,
   preferably from a `KotlinGradleProjectChecker` registered in `ALL_CHECKERS`.
4. Add a test (functional test with `assertContainsDiagnostic`/`assertNoDiagnostics`, and/or a
   golden-file test via `checkDiagnostics`) covering both the "diagnostic reported" and "diagnostic
   not reported when configured correctly" cases. If the diagnostic depends on the exact Gradle
   interaction, add an integration test too (with `assertHasDiagnostic`/`assertNoDiagnostic`),
   covering the same two cases.
5. If the diagnostic can produce false positives, make sure it's suppressible (it is, by default,
   unless it's `FATAL`) and consider documenting the suppression property in user-facing docs.
