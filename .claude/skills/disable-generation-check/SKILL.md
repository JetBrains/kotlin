---
name: disable-generation-check
description: "Disables the TeamCity generation check (`forbidGenerationOnTeamcity`) for Kotlin version branching. Only invoke manually via /disable-generation-check."
disable-model-invocation: true
---

# Disable Generation Check for Branching

When branching to a new Kotlin version, the CI build fails with "[Re-generation needed!]" because generated version constants are out of date relative to the new language version. Rather than regenerating on every branch, we disable the check by adding `forbidGenerationOnTeamcity = false` to all `GeneratorsFileUtil.writeFileIfContentChanged` calls in the affected generators. This unblocks CI while the version switch stabilizes. These changes get reverted on master after branching is complete.

## Determine the action

Ask the user whether they want to **disable** the generation check (for branching) or **revert** it (re-enable the check after branching is complete). Then follow the corresponding section below.

## Disable (set `forbidGenerationOnTeamcity = false`)

For each `GeneratorsFileUtil.writeFileIfContentChanged(...)` call in the 2 files below, ensure it has `forbidGenerationOnTeamcity = false`. Depending on the current state, either add the parameter or swap `true` to `false`.

Use `replace_text_in_file` with `forbidGenerationOnTeamcity = true` -> `forbidGenerationOnTeamcity = false` as a `replaceAll` replacement in each file. If no match is found (first-time application), add the parameter instead — see the "First-time" examples below.

### Files and call sites

**File 1:** `libraries/tools/gradle/generators/native-cache-kotlin-version/src/main/kotlin/org/jetbrains/kotlin/gradle/generators/native/cache/version/Main.kt` — **1 call** at end of `main()`:
```
// First-time:
GeneratorsFileUtil.writeFileIfContentChanged(genFile.toFile(), content, logNotChanged = false)
// Target state:
GeneratorsFileUtil.writeFileIfContentChanged(genFile.toFile(), content, logNotChanged = false, forbidGenerationOnTeamcity = false)
```

**File 2:** `libraries/tools/gradle/generators/native-cache-kotlin-version/src/main/kotlin/org/jetbrains/kotlin/gradle/generators/native/cache/version/NativeCacheKotlinVersionsFile.kt` — **1 call** inside `updateAndGetAll()`:
```
// First-time:
GeneratorsFileUtil.writeFileIfContentChanged(versionsFilePath.toFile(), sortedVersionNames)
// Target state:
GeneratorsFileUtil.writeFileIfContentChanged(versionsFilePath.toFile(), sortedVersionNames, forbidGenerationOnTeamcity = false)
```

### How to apply

Use JetBrains MCP `replace_text_in_file` for each replacement. All replacements are independent — make them in parallel.

### Commit pattern (disable)
```
[Gradle] Do not forbid generator on teamcity for native cache version
```

---

## Revert (set `forbidGenerationOnTeamcity = true`)

After branching is complete, re-enable the check by swapping `false` to `true` in all call sites across the 2 files.

Use `replace_text_in_file` with `forbidGenerationOnTeamcity = false` -> `forbidGenerationOnTeamcity = true` as a `replaceAll` replacement in each of the 2 files.

### Commit pattern (revert)
```
Revert "[Gradle] Do not forbid generator on teamcity for native cache version"
```

---

## Bump DisableCacheInKotlinVersion in tests (disable only)

The `DisableCacheInKotlinVersion` generator uses a rolling deprecation cycle: N (latest) has no deprecation, N-1 gets WARNING, N-2 gets ERROR. After branching, the version constant used in tests may shift to ERROR level, causing compilation failure.

**File:** `libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/native/KotlinNativeDisableCacheIT.kt`

Find all usages of `DisableCacheInKotlinVersion.\`<old_version>\`` and replace with the next version constant (e.g. `2_3_20` → `2_4_0`). There are typically 3 usages — 2 in string templates and 1 in Kotlin code. The existing `@Suppress("DEPRECATION")` annotations handle the WARNING level on N-1 constants.

Use `replace_text_in_file` with `replaceAll: true` to swap all occurrences at once.

### Commit pattern (bump version in tests)
```
[Gradle] Bump DisableCacheInKotlinVersion in KotlinNativeDisableCacheIT
```

---

## Verification

After all replacements (disable or revert), run JetBrains MCP `get_file_problems` with `errorsOnly: false` on all 2 files. Any errors related to `forbidGenerationOnTeamcity` or the modified lines indicate a problem. Pre-existing warnings (unused imports, whitespace style) can be ignored.

