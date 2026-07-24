---
name: analysis-api-mark-internal-apis
description: Drive the per-module internal-API codebase test, then refine the suggested annotations down to `internal` (or up to `@KaImplementationDetail`) based on actual external usage
user-invocable: true
disable-model-invocation: true
argument-hint: "[--intellij=<path>]"
---

# Mark Internal APIs in Analysis API Implementation Modules

Public declarations in Analysis API implementation modules should not be exposed to users. They should be `internal` or
annotated with a visibility annotation. The codebase test
[`AbstractAnalysisApiInternalApiTest`](../../../compiler/psi/psi-api/testFixtures/org/jetbrains/kotlin/AbstractAnalysisApiInternalApiTest.kt)
already finds unmarked declarations and chooses a default annotation per module/package. This skill runs that test in
auto-apply mode, then refines each freshly marked declaration: downgrade to `internal` when it has no callers outside its
module, upgrade `@LLFirInternals` to `@KaImplementationDetail` when its callers reach outside `analysis/low-level-api-fir/`,
and keep the rest. Finally, fix any "internal exposed through public API" build errors by re-promoting `internal` declarations
to `@KaImplementationDetail`.

**Reference:** Read [Guard API Endpoints with Annotations](/analysis/docs/contribution-guide/api-development.md#guard-api-endpoints-with-annotations)
for the full annotation guide and placement rules.

## Inputs

### Module selection (required)

Use `AskUserQuestion` to present a selection of modules. The user must pick one. Only modules that have a per-module
codebase test are listed here:

| Choice                   | Module path (project-relative)    | Gradle codebase test task                       |
|--------------------------|-----------------------------------|-------------------------------------------------|
| `analysis-api-fir`       | `analysis/analysis-api-fir`       | `:analysis:analysis-api-fir:testCodebase`       |
| `analysis-api-impl-base` | `analysis/analysis-api-impl-base` | `:analysis:analysis-api-impl-base:testCodebase` |
| `low-level-api-fir`      | `analysis/low-level-api-fir`      | `:analysis:low-level-api-fir:testCodebase`      |

Throughout this skill, `<module>` and `<gradle-task>` refer to the user's selection.

### Optional flags

| Input         | Flag                | Default                                  | Description                                                          |
|---------------|---------------------|------------------------------------------|----------------------------------------------------------------------|
| IntelliJ repo | `--intellij=<path>` | `../ultimate` (relative to project root) | Path to the IntelliJ repository for additional usage search          |

---

## Annotations Reference

The codebase test owns the **initial** choice of marker for each unmarked declaration. The test's per-module logic lives in
the `suggestedAnnotation` overrides:

- `analysis-api-fir`, `analysis-api-impl-base` → always `@KaImplementationDetail`.
- `low-level-api-fir` → `@KaImplementationDetail` for declarations in `org.jetbrains.kotlin.analysis.low.level.api.fir.api`
  (and subpackages); `@LLFirInternals` for everything else.

The set of annotations the test treats as already-marked (so they're never flagged) lives in
[`AnalysisApiNonPublicMarkers`](../../../compiler/psi/psi-api/testFixtures/org/jetbrains/kotlin/AnalysisApiNonPublicMarkers.kt):
`@KaImplementationDetail`, `@KaExperimentalApi`, `@KaPlatformInterface`, `@KaNonPublicApi`, `@KaIdeApi`, `@LLFirInternals`.
The test also skips `internal`/`private` declarations, `annotation class` declarations annotated with `@RequiresOptIn`, and
declarations carrying `override`. None of those reach this skill.

The skill then refines the test's suggestion based on actual external usage. The rules differ slightly per module because of how
sub-modules see each other:

In every implementation module, `src/` and the test source sets are separate Kotlin modules — an `internal` declaration in `src/` is
**not** visible from `tests/` or `testFixtures/`. That's why "used in own tests" is enough to disqualify a declaration from being
`internal`. Only `low-level-api-fir` has its own opt-in marker (`@LLFirInternals`) for "test-accessible LL-FIR-internal" declarations;
the other modules don't have a similar marker because their implementations don't go that deep, and `@KaImplementationDetail` is the
catch-all for cross-sub-module visibility there.

| Test's suggestion         | Where the declaration is referenced (besides own `src/`)                                | Action                                   |
|---------------------------|-----------------------------------------------------------------------------------------|------------------------------------------|
| `@KaImplementationDetail` | Nowhere (only own `src/`)                                                               | **Downgrade** to `internal`              |
| `@KaImplementationDetail` | Anywhere else (own tests, outside the module, IntelliJ)                                 | Keep                                     |
| `@LLFirInternals`         | Nowhere (only own `src/`)                                                               | **Downgrade** to `internal`              |
| `@LLFirInternals`         | Only inside `analysis/low-level-api-fir/` (own tests/testFixtures, no usages outside)   | Keep                                     |
| `@LLFirInternals`         | Anywhere outside `analysis/low-level-api-fir/`                                          | **Upgrade** to `@KaImplementationDetail` |

The skill never *adds* annotations to declarations the codebase test didn't already touch — Phase 5 just builds and reports any
remaining errors for manual review.

---

## Phase 1: Run the codebase test in auto-apply mode

Replace any manual scan. The test discovers and annotates one violating file per run.

### Step 1: Invoke the test with auto-apply enabled

```bash
./gradlew <gradle-task> \
    -Pkotlin.analysis.codebaseTest.internalApi.updateSourceCode=true \
    -Pkotlin.test.instrumentation.disable.inputs.check=true
```

- `kotlin.analysis.codebaseTest.internalApi.updateSourceCode=true` flips the abstract test into write-back mode: it writes
  the suggested fix to the violating source file, then throws to stop iteration.
- `kotlin.test.instrumentation.disable.inputs.check=true` disables the test-input security manager so the test JVM is
  allowed to write to source files.

### Step 2: Interpret the outcome

- **Test passes (BUILD SUCCESSFUL):** every public declaration in `<module>/src/` is already marked correctly. Skip to
  **Phase 5** to perform a final build check.
- **Test fails with `"Auto-applied N marker annotation(s) to <File>.kt. ..."`:** exactly one source file under
  `<module>/src/` was modified on disk. The next phases work on that file.

If the test fails with a different message (compile error, security exception, etc.), **stop** and report the error to the
user — the skill cannot make sense of unrelated failures.

### Step 3: Identify the modified file

Use `git diff --name-only` (filtered to files under `<module>/src/`):

```bash
git diff --name-only -- '<module>/src/'
```

If multiple files are reported, prefer the one the test message named (the message contains the file's basename). Other
files in the diff are likely the user's pre-existing edits — don't touch them.

---

## Phase 2: Diff, downgrade, upgrade

For the modified file from Phase 1.

### Step 1: Extract the newly added annotations

The test only inserts lines of the form `<indent>@<MarkerName>` directly above a declaration. Use `git diff <file>` (or
`mcp__idea__read_file` on `<file>` and on the HEAD version via `git show HEAD:<file>`) to extract, for each insertion:

- the **declaration** the annotation precedes (kind, name, FQN);
- the **inserted annotation** — `@KaImplementationDetail` or `@LLFirInternals`.

### Step 2: Classify each declaration's usages

For each newly marked declaration, determine where it is referenced **besides its own `src/`**:

- `<module>/tests/`, `<module>/testFixtures/`, `<module>/test/` count as **own tests**.
- Ignore `<module>/testData/`: those are data files, not Kotlin sources, and can mention internal class names without it
  being a real usage.
- Anything else (other modules' `src/`/`tests/`, the IntelliJ repository) counts as **outside the module**.

Tools to run the search:

- **Within the Kotlin project:** use `mcp__idea__search_text` (preferred for exact names) or `mcp__idea__search_regex`,
  with the `paths` parameter to scope the search. To distinguish own-tests from outside-the-module hits, run two queries
  (or read each result's path). For example, for a declaration in `analysis/low-level-api-fir/`, an "outside" search is:
  ```
  paths: ["!analysis/low-level-api-fir/**"]
  ```
  …and an "own tests" search is:
  ```
  paths: ["analysis/low-level-api-fir/tests/**", "analysis/low-level-api-fir/testFixtures/**"]
  ```
- **Within the IntelliJ repository:** the IntelliJ repo is a separate project, so JetBrains MCP cannot search it. Use the
  standard `Grep` tool against `--intellij=<path>`. Anything that hits in the IntelliJ repo counts as **outside the module**.
  If the path doesn't exist, notify the user but don't abort — proceed with Kotlin-repo results only.

**Reducing false positives:**
- For declarations with common names (`create`, `get`, `resolve`), search for the qualified name or a distinctive usage
  pattern (e.g., `ClassName.methodName`, `import ...ClassName`).
- When a hit is ambiguous, read the usage site to confirm it references this specific declaration.
- Classifier names (classes/interfaces/objects) are usually distinctive enough on their own.

### Step 3: Apply the per-declaration decision

| Test's suggestion         | Where the declaration is referenced (besides own `src/`)                                | Action                                                                                                                              |
|---------------------------|-----------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `@KaImplementationDetail` | Nowhere (only own `src/`)                                                               | **Downgrade.** Remove the inserted annotation line. Add `internal` before the declaration keyword.                                  |
| `@KaImplementationDetail` | Anywhere else (own tests, outside the module, IntelliJ)                                 | **Keep.** No change.                                                                                                                |
| `@LLFirInternals`         | Nowhere (only own `src/`)                                                               | **Downgrade.** Remove the inserted annotation line. Add `internal` before the declaration keyword.                                  |
| `@LLFirInternals`         | Only inside `analysis/low-level-api-fir/` (own tests/testFixtures, no usages outside)   | **Keep.** No change. This is precisely what `@LLFirInternals` is for: making a `src/` declaration accessible to LL FIR's own tests. |
| `@LLFirInternals`         | Anywhere outside `analysis/low-level-api-fir/`                                          | **Upgrade.** Replace the `@LLFirInternals` line with `@KaImplementationDetail`. Swap imports accordingly.                           |

Apply edits with `mcp__idea__replace_text_in_file`. **Import housekeeping:** when removing or replacing an annotation,
also remove the corresponding import if no other declaration in the file still uses it. When adding `@KaImplementationDetail`,
ensure `import org.jetbrains.kotlin.analysis.api.KaImplementationDetail` is present.

**Companion-object exception.** A `companion object`'s marker must match its outer classifier's final marker. The companion
is reached through the outer class (`Outer.member` → `Outer.Companion.member`), so callers who can opt into the outer must
also reach the companion. Skip the usage analysis for companion objects in the diff entirely; instead, decide the outer
first using the table above, then sync the companion:

- Outer kept as `@KaImplementationDetail` → companion stays `@KaImplementationDetail`.
- Outer kept as `@LLFirInternals` → companion stays `@LLFirInternals`.
- Outer upgraded to `@KaImplementationDetail` → upgrade the companion the same way (replace `@LLFirInternals` with
  `@KaImplementationDetail`, swap imports).
- Outer downgraded to `internal` → drop the companion's annotation (Step 4 below).

### Step 4: Downgrading nested classifiers

If the same diff includes a top-level classifier **and** nested classifiers within it, and the top-level was downgraded to
`internal`, the nested classifiers don't need their own marker — Kotlin's `internal` visibility transitively covers them.
For each nested classifier in the diff under that newly-`internal` parent: drop the inserted annotation (and any redundant
import), without adding `internal`.

The skill does **not** need to handle `override` declarations specially: the codebase test's `isViolation` filters them
out before they reach the diff (see `AbstractAnalysisApiInternalApiTest.isViolation`).

---

## Phase 3: Verify

Run JetBrains MCP `get_file_problems` with `errorsOnly=false` on the modified file. Fix any warnings or errors related to
the changes (missing imports, opt-in requirements introduced by the new annotations, etc.).

---

## Phase 4: Loop

Re-run **Phase 1 Step 1** with the same flags. There are three outcomes:

- **Test passes** → proceed to Phase 5.
- **Test fails on the same file you just processed** → something went wrong (the downgrade/upgrade may have re-introduced a
  violation, or the file still has unmarked declarations). Stop and report to the user.
- **Test fails on a new file** → return to Phase 2 with that file.

---

## Phase 5: Build and report

Run a normal compilation of the module:

```bash
./gradlew :analysis:<module>:compileKotlin -q
```

If the build **succeeds**, proceed to Phase 6.

If the build **fails**, stop and present the errors verbatim to the user for manual review. The skill does **not** auto-fix
build errors. Common failure modes the user will need to resolve:

- **"internal declaration exposed through public API"** — a declaration the skill downgraded to `internal` is referenced
  in the signature of a public declaration in the same module. The user typically resolves this by switching the exposed
  declaration from `internal` to `@KaImplementationDetail`.
- **`OPT_IN_USAGE_ERROR`** at use sites — a callsite of a newly annotated declaration needs an `@OptIn` annotation, a
  module-level `optIn` compiler option, or its own marker annotation. These can also be triggered by previously unannotated
  declarations the test just marked.
- **Unresolved imports** — rare, but possible if the skill's import housekeeping in Phase 2 missed a case.

Auto-fixing any of these from the diff alone risks masking real problems (e.g., upgrading `internal` to
`@KaImplementationDetail` everywhere on exposure would defeat the point of distinguishing the two), so this is left for
manual review.

---

## Phase 6: Final summary

Present a per-classification breakdown:

- Declarations marked `internal` (Phase 2 downgrades).
- Declarations kept as `@KaImplementationDetail` (test's suggestion confirmed).
- Declarations kept as `@LLFirInternals` (own-tests-only access; no usages outside `analysis/low-level-api-fir/`).
- Declarations upgraded from `@LLFirInternals` to `@KaImplementationDetail` (Phase 2 upgrades).
- Any compilation errors flagged in Phase 5 for the user to resolve manually.
