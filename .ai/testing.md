# Test Infrastructure

## Test Data Files

Tests use `.kt` files in `testData/` directories with special directive comments:

```kotlin
// FILE: fileName.kt           - Split test into multiple files
// MODULE: moduleName          - Define module boundaries
// MODULE: name(dep1, dep2)    - Module with dependencies

// Common directives for language features and compiler options
// LANGUAGE: +Feature          - Enable language feature
// API_VERSION: 1.9            - Set API version
// WITH_STDLIB                 - Include stdlib in compilation
```

## Test Generation

Tests are generated from abstract test runners. After adding test data:
1. Add test data file to appropriate `testData/` directory
2. Run `./gradlew generateTests`
3. New test methods appear in `*Generated.java`/`*Generated.kt` runner files

### Where generated runners land — READ BEFORE SEARCHING

Each module's `testGenerator(...)` call in its `build.gradle.kts` decides the output directory:

| Config                                 | Output location             | Tracked by Git? | Found by repo-search tools (grep/glob over Git content)? |
|-----------------------------------------|------------------------------|------------------|-------------------------------------------------------------|
| default                                 | `<module>/tests-gen/`        | Yes              | Yes                                                          |
| `generateTestsInBuildDirectory = true`  | `<module>/build/tests-gen/`  | **No** (`build/` is gitignored) | **No**                                        |

Many areas use `generateTestsInBuildDirectory = true` (JS, FIR, Native, Wasm, analysis compiler-tests, etc.), so their generated runners and HTML test reports (`<module>/build/reports/tests/test/...`) are build output, not source, and are invisible to any search that only sees Git-tracked files.

**A file-search/glob tool reporting "no matches" for an exact, correctly-spelled `build/tests-gen/...` path is NOT proof the file is missing.** Many agent tool integrations (including IDE-integrated AI assistants) filter out gitignored paths at the index level — they will report zero matches for `build/tests-gen/**` even when you type the exact real path, and some also refuse to open/reference such a file directly by path (not just via search). This is a hard limitation of that tool, not evidence about the file's existence.

**If a generated runner can't be found via repository search, do NOT assume it wasn't generated.** Instead, prefer a real terminal/shell command (which reads the filesystem directly and ignores VCS-ignore/index filtering) over any search or "open by path" tool for anything under `build/`:
1. Run `./gradlew generateTests -q` (or the module-specific generator task) to make sure it's up to date.
2. Search the filesystem directly via a shell command, not a search/glob tool and not Git-tracked content, e.g. `find <module> -name '*Generated.java' -o -name '*Generated.kt'`, or `ls -R <module>/build/tests-gen/` and `<module>/tests-gen/` directly.
3. For large generated files, check `<module>/build/reports/tests/test/<fully.qualified.RunnerName>/` for the HTML report instead of scrolling the raw file.
4. If no terminal/shell access is available at all, ask the user to run one of the commands above and paste the result — do not report the runner as "not found" based on a search or open-by-path tool alone.

This applies to any module — grep for `generateTestsInBuildDirectory` in the relevant `build.gradle.kts` to confirm which location a specific module uses.

## Finding the real test task vs. decoy tasks

Not every Gradle task whose name looks like a test runner actually runs tests with `--tests` filtering support. Some modules register **extra**, CI-only task names alongside the normal `test` task, via `testTask(...)`/`jsTestTask(...)` with `skipInLocalBuild = true`. Examples:

| Module                    | Decoy task name(s)              | Real filterable task    |
|---------------------------|----------------------------------|--------------------------|
| `js/js.tests`              | `jsTest`, `jsES6Test`            | `:js:js.tests:test`      |
| `compiler`                 | `fastJarFSLongTests`             | `:compiler:test`         |
| `compiler/fir/fir2ir`      | `aggregateTests`, `nightlyTests` | `:compiler:fir:fir2ir:test` |

**Why this happens:** `skipInLocalBuild = true` tasks are meant to run only on TeamCity. Locally, `ProjectTestsExtension.testTask(...)` short-circuits and does `project.tasks.register(taskName)` — a plain `Task`, not a `Test` task — while still being exposed under a `TaskProvider<Test>`-shaped API. Passing `--tests` to this plain task doesn't fail loudly; it's simply ignored, since the task has no test-filtering behavior to apply.

**How to find the real task instead of guessing:**
1. Check the [Areas](guidelines.md#areas) table first — the area's own docs may already name the correct task.
2. Look at the module's `build.gradle.kts` for `testTask(...)`/`jsTestTask(...)` calls: the one **without** `skipInLocalBuild = true` (often just named `test`, i.e. the default `taskName`) is the one that runs locally and supports `--tests`.
3. When in doubt, prefer `:<module path>:test` (e.g. `:js:js.tests:test`) as the first thing to try for JUnit-filtered runs.
4. If a `--tests`-filtered run finishes suspiciously fast with `NO-SOURCE`/`UP-TO-DATE` and no test output, that's a sign the task was a decoy — re-check for a `skipInLocalBuild = true` sibling task with the same purpose.
