---
name: bump-kotlin-native-version
description: >
  Bumps the Kotlin/Native version that the Kotlin Gradle plugin is baked against in the
  Kotlin compiler repository — the `kotlin.native.version.default` property consumed by
  `Project.kotlinNativeVersion`. Resolves the newest K/N published from master, verifies it
  on TeamCity and in the dev Maven repository, edits the property, and commits on an `rr/`
  branch. Use this skill whenever the user asks to bump, upgrade, update, or advance the
  Kotlin/Native version, the K/N version in KGP, `kotlin.native.version.default`, or the old
  `versions.kotlin-native` property. Also use it for phrasings like "bump K/N to the latest
  dev", "update the native version KGP ships with", "we need a newer kotlin-native-prebuilt",
  or "pin K/N to 2.5.0-dev-XXXX".
---

# Bump the Kotlin/Native version

The Kotlin repository pins one K/N distribution version that gets baked into KGP's
`project.properties` and downloaded by `NativeCompilerDownloader` on developer machines and in
KGP integration tests. Bumping it is a one-line change — but the line has moved before, and
picking the wrong version fails days later on one host platform only. So the work is mostly
*picking and verifying*, not editing.

Follow project conventions from `CLAUDE.md`: use the JetBrains MCP tools (`get_file_text_by_path`,
`replace_text_in_file`, `search_in_files_by_regex`) for file operations, and `Bash` for git and
scripts.

## Step 1 — Locate the knob, don't assume it

This property has already moved once: it used to be `extra["versions.kotlin-native"]` set inside
a conditional block in the root `build.gradle.kts`, and commit `76fe96a6ff19` moved it into
`gradle.properties` behind a new name. Anyone reproducing an old bump commit from memory ends up
editing a line that no longer exists, so start by finding where the version actually lives today:

```bash
rg -n 'kotlinNativeVersion' repo/gradle-build-conventions/utilities/src/main/kotlin/repoDependencies.kt
```

As of this writing that resolves to:

```kotlin
val Project.kotlinNativeVersion: String
    get() = providers.gradleProperty("kotlin.native.version")          // 1. explicit -P override
        .orElse(
            if (kotlinBuildProperties.alignKotlinNativeVersionInTCBuilds) {
                kotlinBuildProperties.kotlinVersion.get()               // 2. TC: align with build number
            } else if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
                kotlinBuildProperties.defaultSnapshotVersion.get()      // 3. locally built K/N
            } else {
                providers.gradleProperty("kotlin.native.version.default").get()   // 4. ← the one to bump
            }
        ).get()
```

Only branch 4 is a checked-in constant; the other three are computed. So the fallback property
name in that last branch — today `kotlin.native.version.default` in `gradle.properties` — is what
you edit. If the code no longer looks like this, follow the fallback branch to whatever property
it reads now and tell the user the shape changed.

## Step 2 — Resolve and verify the version

Run the bundled resolver, `scripts/resolve_kn_version.py` in this skill's own directory. With no
arguments it picks the newest K/N published from `master`; pass `--version` to check a specific one
the user named. Add `--repo-root` if the repository you're editing isn't the working directory.

```bash
python3 "$SKILL_DIR/scripts/resolve_kn_version.py"
python3 "$SKILL_DIR/scripts/resolve_kn_version.py" --version 2.5.0-dev-4055
```

In a normal checkout `$SKILL_DIR` is `.claude/skills/bump-kotlin-native-version`.

It exits non-zero and prints a `PROBLEMS` list if anything fails. It checks:

- **Published from master.** The version is the build number of a SUCCESS build of TeamCity
  `Kotlin_KotlinDev_KotlinNativePublishMaven` on the default branch. Feature-branch builds also
  publish into the dev repository, and pinning KGP to one means KGP is baked against a K/N nothing
  else in the ecosystem tests against. This is the check Svyatoslav Scherbina called out as the one
  thing to confirm beyond taking a version from the dev repo.
- **All platform artifacts exist** in `packages.jetbrains.team/maven/p/kt/dev` — macOS aarch64 and
  x86_64, Linux x86_64, Windows x86_64, plus the pom. A version missing one tarball breaks only
  that host, and only once someone builds on it.
- **Same release line** as the repo's own `defaultSnapshotVersion` (e.g. `2.5.255-SNAPSHOT` →
  `2.5.0-dev-*`). Crossing lines pins KGP to a different release train.
- **Not a downgrade or a no-op.**

Both endpoints are public reads (TeamCity via `/guestAuth`), so no credentials are needed. If
TeamCity is unreachable and the `teamcity` CLI is authenticated, `teamcity run list --job
Kotlin_KotlinDev_KotlinNativePublishMaven --branch master --status success` gives the same answer.

**If the resolver reports problems, stop and show them to the user.** Don't bump to an unverified
version — the failure surfaces in an aggregate build hours later and is annoying to trace back.

## Step 3 — Confirm with the user

Report the current value, the target, and the TeamCity build the version came from. Then ask for a
YouTrack issue for the `^KT-XXXXX` trailer if the user hasn't given one. Routine "advance to latest"
bumps often have no issue, and that's fine — accept "none" and commit without a trailer rather than
inventing one. When there *is* a driving issue (a K/N fix KGP needs to pick up), the trailer matters,
so it's worth one question.

## Step 4 — Branch

`rr/` is the right prefix, not `rrn/`. The `rrn/` review queue is for changes to Kotlin/Native
itself; this property only affects which prebuilt distribution KGP resolves, so it doesn't need
K/N-team review.

```bash
git fetch origin && git switch -c rr/<username>/<topic> origin/master
```

Derive `<username>` from the user's existing branches rather than guessing from git config — the
short login often differs from the committer name:

```bash
git branch -a --format='%(refname:short)' | grep -oE 'rr/[^/]+/' | sort | uniq -c | sort -rn | head
```

For `<topic>`, use the KT issue if there is one (`rr/ayastrebov/KT-12345`), otherwise something
descriptive like `kn-2.5.0-dev-4190`.

## Step 5 — Edit and commit

Change the single property value in `gradle.properties` with `replace_text_in_file`. Nothing else
needs touching:

- `gradle/verification-metadata.xml` has no entry for the dev distribution — KGP downloads the
  tarball itself, outside Gradle's dependency verification. The `kotlin-native-prebuilt` components
  recorded there are release versions used by specific integration tests, unrelated to this bump.
- KGP's `src/common/resources/project.properties` reads `${kotlinNativeVersion}` and is filtered at
  build time, so it follows automatically.

Commit per `.ai/commit-guidelines.md`. Use the `[Build]` subsystem tag — this is the repository's
own build configuration, not a change to K/N or to KGP behaviour. Subject and body hard-wrapped at
72 columns, imperative mood, and say *why* the bump is happening, since "bump to latest" alone tells
a future reader nothing:

```
[Build] Bump Kotlin/Native version in KGP to 2.5.0-dev-4190

The pinned K/N distribution had drifted a month behind master. Advance
kotlin.native.version.default to the newest version published from
master by Kotlin_KotlinDev_KotlinNativePublishMaven.

^KT-12345
```

Drop the trailer entirely when there is no issue.

## Step 6 — Hand off

Stop before pushing and tell the user what's left:

- Push the branch and open the MR.
- Green means the **aggregate build is green** — there is no targeted check that exercises only
  this property, and nobody verifies KGP integration tests against the new K/N by hand. The
  aggregate is the acceptance criterion.
- Coverage is partial, so treat green as weaker evidence than it looks. TC configurations that
  pass `kotlinNativeVersionForGradleIT` override the K/N version with the snapshot and never
  exercise the bumped value (see `libraries/tools/kotlin-gradle-plugin-integration-tests/build.gradle.kts`);
  only the configurations that leave it unset fall through to the baked-in property. That's the
  accepted practice, not something to work around.

## Reference

| Path | Role |
|------|------|
| `gradle.properties` | Holds `kotlin.native.version.default` — the line you edit |
| `repo/gradle-build-conventions/utilities/src/main/kotlin/repoDependencies.kt` | `Project.kotlinNativeVersion` resolution order |
| `repo/gradle-build-conventions/utilities/src/main/kotlin/BuildPropertiesExt.kt` | `alignKotlinNativeVersionInTCBuilds`, and docs on what this version means |
| `libraries/tools/kotlin-gradle-plugin/build.gradle.kts` | Bakes the value into KGP's `project.properties` |
| `libraries/tools/kotlin-gradle-plugin/src/common/kotlin/.../NativeCompilerDownloader.kt` | Reads it back at KGP runtime |
| `.ai/commit-guidelines.md` | Commit message rules and subsystem tags |

Known drift worth mentioning if you touch it: `scripts/build-kotlin-maven.sh` still passes
`-Pversions.kotlin-native=$KOTLIN_NATIVE_VERSION`, a property nothing reads since `76fe96a6ff19`.
The current override is `-Pkotlin.native.version`. Fixing that is a separate change — mention it,
don't fold it into the bump.
