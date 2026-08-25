---
name: build-bump-gradle-version
description: >
  Bumps the Gradle wrapper/distribution version that BUILDS the Kotlin project itself — the
  version behind `./gradlew`, defined by `gradle/wrapper/gradle-wrapper.properties`. Covers the
  wrapper regeneration and the version-specific fallout that follows it: removed/deprecated Gradle
  DSL, dependency-verification metadata, the Gradle Module Metadata fixtures, and the KGP
  functional tests. This is DISTINCT from the two sibling skills — it is NOT
  `build-tools-bump-gradle-api` (which bumps GRADLE_COMMON_COMPILE_API_VERSION, the API that
  plugins compile against) and NOT `build-tools-bump-gradle-in-tests` (which extends the KGP
  integration-test version matrix). Manual invocation only: a full run downloads a distribution,
  rewrites `gradle/verification-metadata.xml`, and drives `publish`, `testClasses` and
  `functionalTest` — hours of
  wall clock and a heavily mutated checkout — so it starts only when the user explicitly asks for
  `/build-bump-gradle-version`.
disable-model-invocation: true
---

# Bump the Gradle wrapper/distribution version

This skill upgrades the Gradle version that the Kotlin repository builds with — the wrapper
distribution referenced by `gradle/wrapper/gradle-wrapper.properties` and downloaded by
`./gradlew`. It handles the always-needed wrapper regeneration and then drives `help`,
`publish`, `testClasses`, and `functionalTest` to surface and fix the version-specific fallout
(deprecated DSL, dependency-verification metadata, embedded-version test fixtures, test sources
calling removed Gradle APIs).

It is one of three Gradle-version skills; make sure you're in the right one:

| If the user wants to bump… | Use |
|---|---|
| the version `./gradlew` runs (the wrapper) | **this skill** |
| `GRADLE_COMMON_COMPILE_API_VERSION` (plugin compile API) | `build-tools-bump-gradle-api` |
| the KGP integration-test version matrix (`MAX_SUPPORTED`) | `build-tools-bump-gradle-in-tests` |

**Tooling:** per the project `CLAUDE.md`, use JetBrains IDE MCP tools for every read and write on
project files — `mcp__idea__read_file`, `mcp__idea__search_text` / `mcp__idea__search_regex`,
`mcp__idea__get_symbol_info`, and `mcp__idea__apply_patch` for edits — not `Read`/`Edit`/`Grep`/`Glob`,
which bypass the IDE's view of open buffers. Use the default `Bash` tool for Gradle commands (never
`execute_terminal_command`). After editing any file, run `mcp__idea__get_file_problems` with
`errorsOnly: false` and fix warnings attributable to your change.

---

## Keep the existing build logic intact

A wrapper bump is a *forced-change* task, not a modernization task. The build logic in this repo is
load-bearing for hundreds of modules and for TeamCity configurations that nobody reviews alongside
this diff, so every line you touch is risk the user has to carry. Treat the current build logic as
the specification and change only what the new Gradle version refuses to run.

- **Every edit must trace to a specific line of build output** — an error or a warning you can
  quote. If you cannot name the failure that forced an edit, revert it.
- **Not part of this task, even when tempting:** changing where a version comes from, changing how a
  plugin is applied, adding or removing unrelated configuration, reformatting, renaming, moving
  blocks around, blank-line churn, or any "while I'm here" cleanup.
- **Prefer the narrowest fix that works,** in this order:
  1. a CLI escape hatch (next section) — costs nothing and touches no file;
  2. a `@Suppress` or a migration at the single call site that broke;
  3. a version-keyed branch in the compatibility files that already do this
     (`repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/common-configuration.gradle.kts`,
     `repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradleCommon.kt`),
     which preserves the old-Gradle behaviour instead of replacing it;
  4. a structural change — and when you reach this rung, say out loud why nothing narrower worked.
- **Review your own diff before handover:**

  ```bash
  git diff --stat
  ```

  Outside `gradle/wrapper/*`, `gradle/verification-metadata.xml`, and the `*.module` fixtures, every
  touched file should have a one-sentence justification. A diff that fans out across all build-logic
  modules is the signal that a symptom got fixed too broadly — back it out and narrow it.

---

## Inputs

Ask the user for the target Gradle version if they haven't given one. Accept
`MAJOR.MINOR.PATCH` (e.g. `9.7.0`) and pre-release qualifiers `-rc-N`, `-milestone-N`,
`-nightly` (e.g. `9.7.0-milestone-3`) — the wrapper resolves those like any release. Use the
**full version string verbatim** wherever a version is written.

---

## Setup / teardown (wraps the whole run)

The build must resolve the full dependency graph (including Kotlin/Native) for verification
and publication to work, so Kotlin/Native must be enabled while the skill runs.

**At the start:** check `local.properties`:

```bash
grep -n "kotlin.native.enabled" local.properties 2>/dev/null || echo "not found"
```

If it contains `kotlin.native.enabled=false`, comment that line out (prefix with `#`) — and
**remember the exact original content of the file**.

**At the end — whether the run succeeds or fails — restore `local.properties` to its original
state.** Do this even if you abort early. Leaving it modified is a silent, surprising side
effect on the user's checkout.

---

## Escape hatches for the two expected failure walls

Two failures are guaranteed on any minor/major bump and say nothing about whether the bump is going
well. Both have a command-line switch, so use it and keep moving rather than editing files to make
the error go away.

**1. Dependency verification** — the new distribution pulls artifacts that aren't recorded in
`gradle/verification-metadata.xml`, and the build stops with "you will need to update the
gradle/verification-metadata.xml file". Add:

```
-Foff
```

(`-F=off` / `--dependency-verification=off`; `strict` is the default.) The real fix is regenerating
the metadata in Step 2 — `-Foff` only stops verification from blocking the steps that come before
it.

**2. New warnings turned into errors** — the new Gradle API deprecates something the repo still
calls, and `-Werror` / `allWarningsAsErrors` turns the warning into a build failure. Add:

```
-Pkotlin.build.disable.werror=true
```

Know its reach before you rely on it — otherwise you will re-run the same failing command
expecting a different result. It feeds `KotlinBuildProperties.disableWerror`
(`repo/gradle-build-conventions/utilities/src/main/kotlin/BuildPropertiesExt.kt`) and is read by
the **main build only**, in `common-configuration.gradle.kts` (`-Werror` for `JavaCompile`,
`allWarningsAsErrors` for `KotlinJvmCompile`). If the failing compilation is elsewhere:

- **`.gradle.kts` script compilation** is governed by `org.gradle.kotlin.dsl.allWarningsAsErrors`
  (set in `gradle.properties`, `repo/gradle-build-conventions/gradle.properties`, and
  `repo/gradle-settings-conventions/gradle.properties`). Override it on the CLI with
  `-Porg.gradle.kotlin.dsl.allWarningsAsErrors=false`.
- **Build-logic modules under `repo/gradle-build-conventions/*` and
  `repo/gradle-settings-conventions/*`** set `allWarningsAsErrors.set(true)` in their own
  `build.gradle.kts`, so no property overrides them. There is no hatch here — fix the warning at its
  call site (narrowest fix per the section above).

**Rules for both hatches:**

- **CLI only.** Never write them into `gradle.properties` or `local.properties`, and never commit
  them. They are diagnostics for your iteration loop, not a change to the build.
- **They buy progress, not a result.** Once past the blocking step, fix the underlying warnings one
  at a time and re-run the same command **without** the flags until it is clean. Verification
  metadata gets regenerated (Step 2); each new warning gets its own narrow fix.
- **The bump is not done while a flag is still required.** If one still is, stop and tell the user
  exactly which warning needs it and what fixing it would involve — don't quietly leave it on.

---

## Step 1 — Core wrapper bump (always)

### 1.1 Idempotency check

Read `gradle/wrapper/gradle-wrapper.properties` and find the `distributionUrl` line. If the
version it already references equals the requested version, **stop here and tell the user
there is nothing to do** (do not run any further steps; still perform teardown).

### 1.2 Fetch the distribution checksum

Fetch the SHA-256 of the distribution zip (WebFetch or `curl`):

```
https://services.gradle.org/distributions/gradle-<VER>-bin.zip.sha256
```

Keep the returned hex string; it goes into the wrapper.

### 1.3 Regenerate the wrapper

Run the `wrapper` task **twice with identical parameters**:

```bash
./gradlew wrapper --gradle-version <VER> \
  --gradle-distribution-url https://cache-redirector.jetbrains.com/services.gradle.org/distributions/gradle-<VER>-bin.zip \
  --gradle-distribution-sha256-sum <SHA256> \
  --distribution-type BIN \
  -Foff
```

Why twice: the **first** run executes under the *current* Gradle and rewrites
`gradle-wrapper.properties`. The **second** run executes under the *newly written* wrapper
(`./gradlew` re-reads the properties and downloads `<VER>`), which is what regenerates
`gradle-wrapper.jar`, `gradlew`, and `gradlew.bat` into the new version's format. After the
first run those three files typically show **no diff** — they only change once the task runs
under the new Gradle.

Why `-Foff`: the second run is the first thing to configure the build under `<VER>`, so it trips
dependency verification on the artifacts `<VER>` introduces. That is expected fallout, and with the
hatch in place both runs go back-to-back; the metadata is regenerated properly in Step 2. A patch
bump (e.g. `9.6.0` → `9.6.1`) usually introduces no new artifacts, so it would pass either way.

**Then verify** `gradle/wrapper/gradle-wrapper.properties`:
- `distributionUrl` still uses the `cache-redirector.jetbrains.com/services.gradle.org/...`
  prefix (the repo standard). Gradle writes it with an escaped colon (`https\://…`) — that is
  normal and equivalent to `https://…`; do **not** "fix" the backslash. Only if the task
  dropped the JetBrains prefix and wrote plain `services.gradle.org`, restore the
  cache-redirector prefix with the IDE edit tool.
- `distributionSha256Sum` equals the value from step 1.2.

By the end of the bump the changed wrapper files should be:
`gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`,
`gradlew.bat`.

---

## Step 2 — Run `help` and fix all build failures

```bash
./gradlew help -q --console=plain
```

This downloads and validates the new distribution + checksum and evaluates every project's
configuration. Fix **every** failure it reports, then re-run until clean:

- **Dependency-verification failures** (`gradle/verification-metadata.xml`): regenerate it with
  the existing helper — do not hand-edit checksums:

  ```bash
  ./scripts/update-verification-metadata.sh
  ```

  (The script drops the `<components>` block and re-writes it via
  `--write-verification-metadata sha256`, which bypasses verification on its own — it needs no
  `-Foff`. `kotlin.native.enabled` is already handled by the run-wide setup above, so you don't need
  to toggle it again here.) If the script is missing, fall back to the manual steps in `ReadMe.md`.
  Afterwards re-run `help` with verification **on** (no `-Foff`) and confirm it passes.
- **Removed / deprecated Gradle DSL** in `build.gradle.kts` / `settings.gradle.kts` /
  convention plugins. Migrate to the replacement using the build output and the Gradle release
  notes for `<VER>`. This is version-specific — there is no fixed script; read the error, find
  the new API (`mcp__idea__get_symbol_info` helps), and update the call site — that call site only.
- **New warnings as errors:** unblock with the werror hatch if it's in your way, then fix them one
  by one and finish with a flag-free run.
- **Sanity-check the version-keyed compatibility branches** — these switch behavior on the
  running Gradle version and may need a new branch for `<VER>`:
  - `repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/common-configuration.gradle.kts`
  - `repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradleCommon.kt`

A patch bump (e.g. `9.6.0` → `9.6.1`) usually needs none of the above; a minor/major bump
usually needs DSL migration and verification-metadata regeneration.

---

## Step 3 — Run `publish` and fix all build failures

```bash
./gradlew publish --console=plain
```

(If that task/aggregate isn't resolvable as-is, discover the right one with
`./gradlew tasks --all | grep -i publish`.) This exercises publication and Gradle Module
Metadata. Fix **every** failure, then re-run until clean — the escape hatches are fine while you
iterate, but the step counts as done only on a flag-free run. The characteristic failure is the
**embedded Gradle version in the Module Metadata test fixtures**:

The `*.module` files under `repo/artifacts-tests/src/test/resources/org/jetbrains/kotlin/**/`
embed the producing Gradle version, e.g.:

```
"createdBy": { "gradle": { "version": "9.6.1" } }
```

They must track the wrapper version or `:repo:artifacts-tests` fails (≈60 files changed in the
9.6.1 bump). To update them:

1. Find the currently embedded version:
   ```
   mcp__idea__search_text  "createdBy"  paths: ["repo/artifacts-tests/src/test/resources/"]
   ```
   or search the old version string directly.
2. **Confirm the exact string Gradle stamps for `<VER>`** before mass-replacing — for a
   `-milestone`/`-rc`/`-nightly` qualifier, Gradle may write something other than the literal
   input, so inspect one freshly produced `.module` (from the `publish` output) rather than
   assuming. Prefer regenerating the fixtures via the test's update mode if one exists;
   otherwise do a scoped replace of the old version string → the confirmed new string across
   that resource tree only.

A new Gradle version may also change the *shape* of the metadata, not just the stamped version. When
that happens the schema check needs a matching touch —
`repo/artifacts-tests/src/test/kotlin/org/jetbrains/kotlin/code/GradleMetadataSchema.kt` and its
test (see `fb7004508e92` for how the 9.7.0 bump handled it). Keep that edit as narrow as the new
metadata requires.

---

## Step 4 — Run `testClasses` and fix all compilation failures

```bash
./gradlew testClasses --console=plain
```

Unqualified, this runs `testClasses` in every project that has it, so it compiles the whole repo's
test sources — including test fixtures, which no earlier step reaches. Test code is where a wrapper
bump breaks most widely: tests and fixtures call `gradleApi()` / `ProjectBuilder` / Gradle internals
directly, so a removed or changed API shows up here in dozens of modules at once. Doing this before
the test *runs* is deliberate — one compile pass surfaces the whole set of breakages, which is much
cheaper than discovering them one failing test task at a time.

Fix **every** compilation error, then re-run until clean, flag-free for the final run. Root-cause
each one and keep the fix at the broken call site — this step is the one most likely to tempt a
sweeping refactor across modules, and the 9.7.0 bump needed only narrow per-module fixes
(`220377fe67a0`, `50e715f3eaa1`, `c6a8fa7cde72`).

Iterate on a single module rather than the whole repo while you work:

```bash
./gradlew :kotlin-gradle-plugin:testClasses --console=plain
```

---

## Step 5 — Run `functionalTest` and fix all failures (last)

```bash
./gradlew :kotlin-gradle-plugin:functionalTest --console=plain
```

These use Gradle's `ProjectBuilder` API and are sensitive to Gradle version changes. Fix
**every** compilation error and test failure, then re-run until green — again, flag-free for the
final run. Iterate faster on a single class:

```bash
./gradlew :kotlin-gradle-plugin:functionalTest --tests "org.jetbrains.kotlin.gradle.SomeTestClass" --console=plain
```

Root-cause each failure:
- Test setup uses a changed/removed Gradle API → update the test or its utility.
- Production behavior changed due to the bump → update the expectation and/or the production
  code. Test sources live in `libraries/tools/kotlin-gradle-plugin/src/functionalTest/`.

---

## Step 6 — Handover (do not commit)

The user reviews and commits this change themselves; a wrapper bump usually gets split into a core
commit plus per-area fix commits, and that split is theirs to make. **Leave everything in the
working tree — do not `git add`, `git commit`, or `git stash` anything.**

Finish by reporting:

1. **What changed, grouped by cause:** wrapper files; `gradle/verification-metadata.xml`;
   `.module` fixtures; each forced source/build-logic fix with the failure that forced it.
2. **`git diff --stat`**, reviewed as described in "Keep the existing build logic intact".
3. **Anything still not clean** — in particular any warning that still needs
   `-Pkotlin.build.disable.werror=true` or any step you could only get through with a flag.
4. **`local.properties` restored** to its original content (confirm this explicitly).
5. **A suggested commit message** for the user to apply if they want it — the style prior bumps
   used (`91b45e1ee110`):

   ```
   [Repo] Update Gradle version to <VER>

   Release notes: https://docs.gradle.org/<VER>/release-notes.html

   ^KT-XXXXX
   ```

   `.ai/commit-guidelines.md` has the repo's rules if the user asks you to draft more.

---

## Out of scope

- **The other four `gradle-wrapper.properties` files** —
  `kotlin-native/performance/`, `kotlin-native/backend.native/tests/samples/`,
  `libraries/tools/kotlin-stdlib-docs/`, `libraries/tools/kotlin-stdlib-docs-legacy/` — are
  independent standalone sub-builds, intentionally pinned to their own Gradle versions. Do
  **not** sync them with the root wrapper.
- **`GRADLE_COMMON_COMPILE_API_VERSION`** → `build-tools-bump-gradle-api`.
- **The KGP integration-test version matrix / `MAX_SUPPORTED`** → `build-tools-bump-gradle-in-tests`.
- **Committing** — see Step 6.
- **Build-logic modernization** that the new Gradle version does not force — see "Keep the existing
  build logic intact".

---

## Key files

| Path | Role |
|------|------|
| `gradle/wrapper/gradle-wrapper.properties` | The authoritative build Gradle version (`distributionUrl` + `distributionSha256Sum`) — edited in Step 1 |
| `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat` | Regenerated by the `wrapper` task in Step 1 |
| `scripts/update-verification-metadata.sh` | Regenerates `gradle/verification-metadata.xml` (Step 2) |
| `gradle/verification-metadata.xml` | Dependency checksum registry — regenerated, not hand-edited |
| `repo/artifacts-tests/src/test/resources/org/jetbrains/kotlin/**/*.module` | Fixtures embedding the producing Gradle version (Step 3) |
| `repo/artifacts-tests/src/test/kotlin/org/jetbrains/kotlin/code/GradleMetadataSchema.kt` | Module Metadata schema check — may need a narrow update if the metadata shape changed (Step 3) |
| `repo/gradle-build-conventions/.../common-configuration.gradle.kts`, `.../gradle/GradleCommon.kt` | `GradleVersion.current()` compatibility branches to sanity-check (Step 2); the first also holds the `-Werror` / `allWarningsAsErrors` switches |
| `repo/gradle-build-conventions/utilities/src/main/kotlin/BuildPropertiesExt.kt` | `KotlinBuildProperties.disableWerror` — what `-Pkotlin.build.disable.werror=true` actually feeds |
| `gradle.properties`, `repo/gradle-build-conventions/gradle.properties`, `repo/gradle-settings-conventions/gradle.properties` | `org.gradle.kotlin.dsl.allWarningsAsErrors=true` — the `.gradle.kts` werror switch |
| `libraries/tools/kotlin-gradle-plugin/src/functionalTest/` | Functional test sources (Step 5) |
| `local.properties` | May set `kotlin.native.enabled=false` — toggle at start, restore at end |
