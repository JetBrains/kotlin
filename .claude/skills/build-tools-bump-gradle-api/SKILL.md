---
name: build-tools-bump-gradle-api
description: >
  Bumps the Gradle API version that Kotlin Gradle plugins are compiled against in the
  Kotlin compiler repository. Use this skill whenever the user asks to upgrade, bump,
  or update the Gradle compile API version, Gradle plugin API version, or
  GRADLE_COMMON_COMPILE_API_VERSION. Also use it when the user says something like
  "update KGP to compile against Gradle X.Y.Z" or "bump Gradle API to X.Y" or
  "we need to support Gradle X.Y in the plugin".
---

# Bump Gradle API Version

This skill walks through bumping `GradlePluginVariant.GRADLE_COMMON_COMPILE_API_VERSION`
— the Gradle API version that the Kotlin Gradle plugins' common source set is compiled against.

**IMPORTANT**: This project uses JetBrains MCP tools for all file operations. Use
`mcp__idea__*` tools (read_file, replace_text_in_file, search_in_files_by_text, etc.)
instead of standard Read/Edit/Grep/Glob tools.

---

## Step 1 — Confirm the target version

If the user hasn't specified the target Gradle API version, ask them for it before proceeding.

Read the current value from:
```
repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradlePluginVariant.kt
```

Look for the companion object and confirm the current `GRADLE_COMMON_COMPILE_API_VERSION` value,
then let the user know what you're about to change it to.

---

## Step 2 — Update GRADLE_COMMON_COMPILE_API_VERSION

In `repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradlePluginVariant.kt`,
change the constant inside the `companion object`:

```kotlin
const val GRADLE_COMMON_COMPILE_API_VERSION = "<NEW_VERSION>"
```

Use `mcp__idea__replace_text_in_file` to make this change.

---

## Step 3 — Regenerate verification-metadata.xml

The new Gradle API version introduces new artifacts whose checksums must be recorded.

### 3a. Handle local.properties

Check if `local.properties` contains `kotlin.native.enabled=false`:

```bash
grep -n "kotlin.native.enabled" local.properties 2>/dev/null || echo "not found"
```

If found, temporarily comment it out (prefix the line with `#`). Remember to restore it after step 3c.

### 3b. Run the regeneration script

```bash
./scripts/update-verification-metadata.sh
```

This script:
1. Strips the `<components>` section from `gradle/verification-metadata.xml`
2. Runs `./gradlew --write-verification-metadata sha256 -Pkotlin.native.enabled=true resolveDependencies`

This can take several minutes. If the script itself doesn't exist or fails, fall back to the manual steps from `ReadMe.md`:
```bash
# Linux
sed -i -e '/<components>/,/<\/components>/d' gradle/verification-metadata.xml
./gradlew --write-verification-metadata sha256 -Pkotlin.native.enabled=true resolveDependencies
```

### 3c. Restore local.properties

If you commented out `kotlin.native.enabled=false` in step 3a, restore it now.

---

## Step 4 — Compile kotlin-gradle-plugin and fix errors

Run compilation to surface any API incompatibilities:

```bash
./gradlew :kotlin-gradle-plugin:compileKotlin --quiet --console=plain --warning-mode=none 2>&1 | head -100
```

If that passes cleanly, also verify the common source set compilation (which uses the bumped API):
```bash
./gradlew :kotlin-gradle-plugin:classes --quiet --console=plain --warning-mode=none 2>&1 | head -100
```

### Fixing compilation errors

If errors appear, approach them methodically:

1. **Identify the source set**: Errors in `src/common/` affect all Gradle versions.
   Errors in `src/gradleXXX/` affect only that specific variant.

2. **Understand the API change**: Use `mcp__idea__get_symbol_info` to look up what
   replaced a removed or changed API. Check the Gradle release notes for the target version.

3. **Apply fixes**: Use `mcp__idea__replace_text_in_file` to update code.

4. **Re-run compilation** after each round of fixes until it's clean.

5. **Check for warnings**: After compilation succeeds, run `mcp__idea__get_file_problems`
   on any files you changed (with `errorsOnly=false`). Fix warnings related to your changes.

Common categories of errors when bumping Gradle API:
- Removed internal Gradle classes/methods → find the public replacement
- Changed method signatures → update call sites
- Newly deprecated APIs used in common code → migrate to the replacement

---

## Step 5 — Run functionalTest and fix failures

Run the functional tests (these use Gradle's `ProjectBuilder` API, not full integration tests):

```bash
./gradlew :kotlin-gradle-plugin:functionalTest --quiet --console=plain --warning-mode=none 2>&1 | tail -80
```

### Fixing test failures

If failures occur:

1. **Read the failure output carefully** — determine whether it's a compilation error,
   an assertion failure, or an exception at runtime.

2. **Run just the failing test class** to iterate faster:
   ```bash
   ./gradlew :kotlin-gradle-plugin:functionalTest --tests "org.jetbrains.kotlin.gradle.SomeTestClass" --quiet --console=plain --warning-mode=none
   ```

3. **Fix the root cause**:
   - If the test setup uses a Gradle API that changed → update the test or test utility
   - If production behavior changed due to the API bump → update the expectation and/or production code
   - Test sources live in `libraries/tools/kotlin-gradle-plugin/src/functionalTest/`

4. **Re-run** after each fix until all tests pass.

---

## Key files

| Path | Purpose |
|------|---------|
| `repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradlePluginVariant.kt` | Contains `GRADLE_COMMON_COMPILE_API_VERSION` — edit this |
| `repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradleCommon.kt` | Consumes the constant (3 usages) — read-only reference |
| `scripts/update-verification-metadata.sh` | Script to regenerate `gradle/verification-metadata.xml` |
| `gradle/verification-metadata.xml` | Dependency checksum registry — regenerated in step 3 |
| `libraries/tools/kotlin-gradle-plugin/src/functionalTest/` | Functional test sources |
| `libraries/tools/kotlin-gradle-plugin/AGENTS.md` | Area-specific build/test commands |
| `local.properties` | May override `kotlin.native.enabled` — handle carefully in step 3 |
