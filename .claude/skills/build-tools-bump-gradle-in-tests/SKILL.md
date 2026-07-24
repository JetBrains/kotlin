---
name: build-tools-bump-gradle-in-tests
description: Bump the maximum supported Gradle version in the `kotlin-gradle-plugin-integration-tests` project. Use this whenever the user asks to bump Gradle in integration tests, add a new Gradle X.Y.Z to the test matrix, update `MAX_SUPPORTED` Gradle, "run tests against the new Gradle release", or anything else that sounds like extending the integration test matrix to a newer Gradle. The procedure spans four touch points across three files that must stay in lock-step (`TestVersions.kt`, `build.gradle.kts`, `GradleCompatibilityIT.kt`); reach for this skill even when the user mentions only one of those files, because editing by hand routinely desyncs them.
disable-model-invocation: true
---

# Bump the maximum supported Gradle version in integration tests

The `kotlin-gradle-plugin-integration-tests` module parameterises its tests
across a fixed list of Gradle versions. Bumping the maximum requires
synchronised edits to a versions table, a Gradle build list, an AGP
compatibility matrix, and a compatibility test's `when` block — all of which
must agree. This skill encodes the procedure.

## When to use

Trigger on phrasings such as:

- "bump Gradle in tests" / "bump max Gradle"
- "add Gradle X.Y.Z to integration tests"
- "test against the new Gradle X.Y.Z release"
- "update MAX_SUPPORTED Gradle"
- "support Gradle X.Y in KGP integration tests"

Users frequently mention only one of the affected files; the procedure always
touches all of them, so use this skill even if the request looks narrower.

## Inputs

Ask the user for the new Gradle version string (e.g. `9.6.0`, `9.6.1`,
`9.6.0-rc-1`, `10.0.0`). Gradle ships pre-releases with a `-rc-N`,
`-milestone-N`, or `-nightly` qualifier; accept those too — the wrapper
resolves them like any release. If the user has not provided a version,
ask before editing anything.

From `MAJOR.MINOR.PATCH[-qualifier]` derive:

- `constantName = G_<MAJOR>_<MINOR>` (always major.minor only — drop the
  patch *and* any qualifier from the name)
- `constantValue = "<full input string>"` (keep patch and qualifier
  verbatim, since this is the literal string the Gradle wrapper consumes)

If a constant with that name already exists, update its value and the
matching `gradleVersions` list entry in place — do **not** create a
duplicate. Two common shapes for this:

- Patch-only bump (`9.5.0` → `9.5.1` on the existing `G_9_5`)
- RC → stable promotion (`9.6.0-rc-1` → `9.6.0` on the existing `G_9_6`
  once the stable release ships)

Steps 4 and 7 still apply — `MAX_SUPPORTED` and the `when` branch may need
refreshing if the surrounding entries shifted.

## Files involved

All edits are inside `libraries/tools/kotlin-gradle-plugin-integration-tests/`:

| File | Edits |
|---|---|
| `src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/TestVersions.kt` | 3 — new constant, `MAX_SUPPORTED`, last `AgpCompatibilityMatrix` entry |
| `build.gradle.kts` | 1 — append to `gradleVersions` list |
| `src/test/kotlin/org/jetbrains/kotlin/gradle/GradleCompatibilityIT.kt` | 1 — new branch in `properPluginVariantIsUsed`'s `when` |

Plus one read-only reference (open it but do not edit it):

- `repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradlePluginVariant.kt`
  — drives the `sourceSetName` chosen for the new version.

## Tooling

Per the project `CLAUDE.md`, use JetBrains IDE MCP tools for every read and
write on these project files. Specifically:

- Read with `mcp__idea__read_file` or `mcp__idea__get_symbol_info`.
- Edit with `mcp__idea__replace_text_in_file`.
- After each edit, run `mcp__idea__get_file_problems` with `errorsOnly: false`
  and fix any warning attributable to the change.

Do not use `Read`, `Edit`, `Write`, `Grep`, or `Glob` on these project files —
they bypass the IDE's view of open buffers and can desync.

## Step-by-step

Carry these out in order. After each step, briefly state what changed so the
user can follow along.

### 1. Collect inputs

Confirm the target version with the user. Accept `MAJOR.MINOR.PATCH` and
`MAJOR.MINOR.PATCH-<qualifier>` (Gradle uses `-rc-N`, `-milestone-N`,
`-nightly`). Reject obviously wrong input (no recognisable
`MAJOR.MINOR.PATCH` core, or strictly less than the current
`MAX_SUPPORTED`). Compute `constantName` and `constantValue` per the
"Inputs" section.

### 2. Pick the Gradle plugin variant

Read `repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradlePluginVariant.kt`.

Each enum entry has a `sourceSetName` and a `gradleVersion`. Pick the variant
whose `gradleVersion` is the **highest value ≤ the new Gradle `MAJOR.MINOR`**.

Worked example (today's enum): the last entry is
`GRADLE_813(sourceSetName = "gradle813", gradleVersion = "8.13", …)`, so
anything in the 8.13+ range — including a bump to 9.6.0 — maps to
`"gradle813"`. If a future enum gains a newer entry (e.g. `GRADLE_900`
covering 9.0+), pick that instead. Always reread the enum at the start of
the run; do not rely on a memorised value.

Remember the chosen `sourceSetName` — step 7 uses it.

### 3. Add the new constant to `TestVersions.Gradle`

File: `libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/TestVersions.kt`

Inside `object Gradle`, after the last `const val G_…` line and before
`MIN_UNSUPPORTED_VERSION_TO_CHECK` / `MIN_SUPPORTED` / `MAX_SUPPORTED`,
insert:

```kotlin
const val G_X_Y = "X.Y.Z"
```

Preserve numerical order with the existing constants.

### 4. Update `MAX_SUPPORTED`

In the same file, change:

```kotlin
const val MAX_SUPPORTED = G_<previous>
```

to:

```kotlin
const val MAX_SUPPORTED = G_X_Y
```

### 5. Update the last `AgpCompatibilityMatrix` entry

In the same file, find the **last** entry of `enum class AgpCompatibilityMatrix`.
Its third constructor argument is `maxSupportedGradleVersion: GradleVersion`.
Replace its existing constant reference with `G_X_Y`. Do not change the
preceding entries or the `minSupportedGradleVersion` argument.

Worked example — before, for a 9.6.0 bump:

```kotlin
AGP_91(AGP.AGP_91,   G_9_3,  G_9_5, JavaVersion.VERSION_17),
```

After:

```kotlin
AGP_91(AGP.AGP_91,   G_9_3,  G_9_6, JavaVersion.VERSION_17),
```

### 6. Add the version to `gradleVersions` in `build.gradle.kts`

File: `libraries/tools/kotlin-gradle-plugin-integration-tests/build.gradle.kts`

Find `val gradleVersions = listOf(...)` (the comment
`// Must be in sync with TestVersions.kt KTI-1612` sits immediately above it).
Append the new version at the end, preserving the trailing-comma style:

```kotlin
val gradleVersions = listOf(
    …
    "9.5.0",
    "X.Y.Z",
)
```

The list order must mirror `TestVersions.Gradle`.

### 7. Add the new branch in `gradlePluginVariantIsUsed`

File: `libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/GradleCompatibilityIT.kt`

Inside `fun properPluginVariantIsUsed(...)`'s `when (gradleVersion)` block,
insert a new branch as the **topmost** case (most recent Gradle version
first):

```kotlin
GradleVersion.version(TestVersions.Gradle.G_X_Y) -> "<sourceSetName>"
```

Use the `sourceSetName` selected in step 2.

## Verification

1. Run `mcp__idea__get_file_problems` (with `errorsOnly: false`) on each
   edited file and confirm no new problems attributable to the change.
2. Optionally exercise the compatibility test against the new Gradle:

   ```bash
   ./gradlew :kotlin-gradle-plugin-integration-tests:<prefix>TestsForGradle_X_Y_Z \
       --tests "*GradleCompatibilityIT.properPluginVariantIsUsed*" -q
   ```

   The task name is generated by the loop in `build.gradle.kts`
   (`tasks.register<Test>("${taskPrefix}TestsForGradle_${gradleVersion.replace(".", "_")}")`);
   convert dots in the version to underscores. The `<prefix>` varies by
   configuration — discover it with `./gradlew :kotlin-gradle-plugin-integration-tests:tasks --all | grep TestsForGradle_X_Y_Z`.

## Commit conventions

Read `docs/code_authoring_and_core_review.md` before committing.

A single commit covers all three files. Use the subject style established by
prior bumps (see `e6f6dba6e8d1` for 9.5.0 and `9cf530c41d41` for 9.4.1):

```
[Gradle] Run tests against Gradle X.Y.Z release
```

If a YouTrack issue is associated, add `^KT-XXXXX Fixed` on a body line so
YouTrack auto-closes it.

## Out of scope

- `AGP.MAX_SUPPORTED` (note its "Update once the Gradle MAX_SUPPORTED version
  is bumped" comment) — governed by AGP releases, not Gradle, and handled by
  a separate task.
- Any files outside the three listed in "Files involved" — other modules
  consume Gradle versions transitively through `TestVersions`, so the bump
  propagates without further edits.
