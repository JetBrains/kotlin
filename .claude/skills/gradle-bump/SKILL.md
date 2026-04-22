---
name: gradle-bump
description: "Bump the supported Gradle version in the Kotlin Gradle Plugin (KGP). Use this skill whenever the user wants to add Gradle X.Y support, update Gradle compatibility, bump Gradle to a new version in KGP, or work on 'Compatibility with Gradle X.Y release' tasks. TRIGGER on: 'bump Gradle to 9.5', 'add Gradle 9.5 support', 'update KGP Gradle compatibility', 'KGP Gradle version bump', 'Gradle compatibility issue KT-XXXXX'. Do NOT trigger for general Gradle build fixes, Kotlin version updates, or non-KGP Gradle tasks."
---

# Gradle Version Bump for KGP

This skill guides you through updating KGP's maximum supported Gradle version. Each bump follows a predictable structure: YouTrack tracking issues, four file edits, and fixing any new deprecations introduced by the new Gradle release.

## Phase 1: Gather Information

**Ask the user:**
1. What is the target Gradle version? (e.g., `9.5.0` or `9.5.1`)
2. Does a YouTrack parent issue already exist? If yes, what is the ID (e.g., `KT-XXXXX`)?

**Then, before touching any files:**

Fetch the Gradle upgrading guide to identify new deprecations. The guide URL uses the **major version** in the filename (e.g., Gradle 9.x → `upgrading_version_9.html`, Gradle 10.x → `upgrading_version_10.html`):

```bash
# Replace VERSION with the full version (e.g., 9.5.0) and MAJOR with the major version (e.g., 9)
curl -s "https://docs.gradle.org/<VERSION>/userguide/upgrading_version_<MAJOR>.html" | python3 -c "
import sys, re
html = sys.stdin.read()
html = re.sub(r'<script[^>]*>.*?</script>', '', html, flags=re.DOTALL)
html = re.sub(r'<style[^>]*>.*?</style>', '', html, flags=re.DOTALL)
text = re.sub(r'<[^>]+>', '\n', html)
for e in [('&amp;','&'),('&lt;','<'),('&gt;','>'),('&nbsp;',' ')]:
    text = re.sub(e[0], e[1], text)
text = re.sub(r'\n\s*\n', '\n', text)
start = text.find('Upgrading from')
if start == -1: start = text.find('Deprecations')
print(text[start:start+8000] if start != -1 else text[:8000])
"
```

If that URL returns empty or CSS-only content (the page may be JS-rendered), try the release notes page instead:
```bash
curl -s "https://docs.gradle.org/<VERSION>/release-notes.html" | python3 -c "
import sys, re
html = sys.stdin.read()
html = re.sub(r'<script[^>]*>.*?</script>', '', html, flags=re.DOTALL)
html = re.sub(r'<style[^>]*>.*?</style>', '', html, flags=re.DOTALL)
text = re.sub(r'<[^>]+>', '\n', html)
for e in [('&amp;','&'),('&lt;','<'),('&gt;','>'),('&nbsp;',' ')]:
    text = re.sub(e[0], e[1], text)
text = re.sub(r'\n\s*\n', '\n', text)
for kw in ['Deprecat', 'Breaking', 'Removed', 'Incompatible']:
    idx = text.find(kw)
    if idx > 0:
        print(f'--- {kw} ---')
        print(text[max(0,idx-50):idx+2000])
"
```

Read the four key files to understand the current state:
- `repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradlePluginVariant.kt`
- `libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/TestVersions.kt`
- `libraries/tools/kotlin-gradle-plugin-integration-tests/build.gradle.kts` (look for the `gradleVersions` list)
- `gradle/wrapper/gradle-wrapper.properties`

Use `mcp__jetbrains__get_file_text_by_path` for all project file reads.

## Phase 2: YouTrack Issues

**Always use YouTrack MCP (`mcp__youtrack__*`) for all YouTrack operations. Never fetch YouTrack URLs directly.**

Every Gradle bump is tracked by a parent issue with two sub-tasks:
- **Parent**: "Compatibility with Gradle X.Y.Z release"
- **Sub-task 1**: "Compile against Gradle X.Y API"
- **Sub-task 2**: "Run tests against Gradle X.Y"

### If the user provided a parent issue ID:
1. Fetch it: `mcp__youtrack__get_issue` with the given ID
2. Check for existing sub-tasks: `mcp__youtrack__search_issues` with query `subtask of: KT-XXXXX`
3. Report what exists and what still needs to be created

### If no parent issue ID was provided:
Ask the user: *"Should I create the YouTrack tracking issues, or do they already exist?"*

- If they already exist, ask for the IDs
- If they need to be created, create:
  1. Parent issue in project `KT`, summary "Compatibility with Gradle X.Y.Z release", description with link to release notes `https://docs.gradle.org/X.Y.Z/release-notes.html`, Subsystem: "Tools. Gradle"
  2. Sub-task "Compile against Gradle X.Y API" linked as child of the parent
  3. Sub-task "Run tests against Gradle X.Y" linked as child of the parent

Use `mcp__youtrack__create_issue` and `mcp__youtrack__link_issues` (with link type `parent for` / `subtask of`).

## Phase 3: Code Changes

Use `mcp__jetbrains__replace_text_in_file` for all edits.

### Step 1 — Compile against Gradle X.Y API

**File: `repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradlePluginVariant.kt`**

Two changes needed:

**a) Add new enum entry** after the current last entry.

Pattern (copy from last entry, update values):
```kotlin
GRADLE_<XY>("gradle<XY>", "<X.Y>", "<X.Y.Z>", "https://docs.gradle.org/current/javadoc/", "<bundled_kotlin>"),
```
Where:
- `sourceSetName` = `"gradle<MAJOR><MINOR>"` (e.g., `"gradle95"`)
- `minimalSupportedGradleVersion` = `"X.Y"` (e.g., `"9.5"`)
- `gradleApiVersion` = `"X.Y.Z"` (the full release version, e.g., `"9.5.0"`)
- `gradleApiJavadocUrl` = `"https://docs.gradle.org/current/javadoc/"` (always `current` for the latest)
- `bundledKotlinVersion` = Kotlin version bundled in this Gradle release (find in release notes under "Upgrade to Kotlin X.Y.Z", e.g., `"2.1"`)

**Also update the previous last entry**: change its `gradleApiJavadocUrl` from `"https://docs.gradle.org/current/javadoc/"` to `"https://docs.gradle.org/<its_version>/javadoc/"` (pin the old entry to its exact version).

**b) Update `GRADLE_COMMON_COMPILE_API_VERSION`** in the companion object:
```kotlin
const val GRADLE_COMMON_COMPILE_API_VERSION = "X.Y.Z"
```

> **Note**: A new enum entry is only needed when the new Gradle version introduces API-level changes that KGP needs to target (new APIs to call, or previously available APIs that now require code changes). If the bump is purely about running tests against the new version without any API usage changes, you may skip adding the enum entry and only update `GRADLE_COMMON_COMPILE_API_VERSION`.

**d) Update `gradle/verification-metadata.xml`**

This file contains checksums for all dependencies. Every "compile against" change requires updating it because the `gradle-public-api` dependency version changes. Run:
```bash
./gradlew --write-verification-metadata sha256 help
```
This regenerates checksums. Alternatively, manually update the `gradle-public-api` entry from the old version to the new one. Bundled dependency versions (Groovy, Kotlin, ASM) may also change — the release notes will list these upgrades.

**e) Fix compilation errors from new/changed Gradle APIs**

After updating the API version, compile the plugin:
```bash
./gradlew :kotlin-gradle-plugin:compileKotlin -q
```
If it fails, the new Gradle version likely changed or added APIs that KGP uses. Common patterns from past bumps:
- **New abstract methods on interfaces** KGP implements (e.g., 9.3 added `AttributeContainer.named()` requiring `ObjectFactory` parameter in `HierarchyAttributeContainer`)
- **Deprecated API suppression** — add `@Suppress("DEPRECATION")` temporarily or create version-guarded helper methods

When creating helper methods for deprecation compatibility, prefer the pattern:
```kotlin
// In utils/configurations.kt or similar
internal fun Configuration.setInvisibleIfSupported() {
    if (GradleVersion.current() < GradleVersion.version("9.0")) {
        @Suppress("DEPRECATION")
        isVisible = false
    }
}
```

If runtime source code changes are needed, update the **API signature file** too:
```bash
./gradlew :kotlin-gradle-plugin:apiCheck
# If it fails, regenerate:
./gradlew :kotlin-gradle-plugin:apiDump
```
This updates `libraries/tools/kotlin-gradle-plugin/api/` signature files.

**c) Add the new variant to `GradleCommon.kt`'s explicit variant list.**

File: `repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradleCommon.kt`

The `createGradlePluginVariants()` function at the bottom of this file contains a **hardcoded `listOf(...)` of all variants**. New enum entries are NOT picked up automatically — add the new `GradlePluginVariant.GRADLE_<XY>` entry there:

```kotlin
fun Project.createGradlePluginVariants(commonSourceSet: SourceSet, publishShadowedJar: Boolean) {
    listOf(
        ...
        GradlePluginVariant.GRADLE_813,
        GradlePluginVariant.GRADLE_<XY>,  // add here
    ).forEach { variant -> ...
```

> **Gradle API dependency workaround**: The private `createGradlePluginVariant()` function uses `dev.gradleplugins:gradle-api:<version>` for each variant. However, `dev.gradleplugins` sometimes lags behind official Gradle releases and may not have published the new version yet. If that's the case, add a workaround similar to the existing `GRADLE_813` special case:
> ```kotlin
> if (variant == GradlePluginVariant.GRADLE_<XY>) {
>     // Workaround until 'dev.gradleplugins:gradle-api:X.Y.Z' will be published
>     variantSourceSet.compileOnlyConfigurationName("org.jetbrains.intellij.deps:gradle-api:${variant.gradleApiVersion}")
>     variantSourceSet.compileOnlyConfigurationName("javax.inject:javax.inject:1")
> }
> ```
> Check whether `https://repo.gradle.org/gradle/libs-releases/dev/gradleplugins/gradle-api/` has the new version before deciding.

### Step 2 — Run tests against Gradle X.Y

**File: `libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/TestVersions.kt`**

Inside the `Gradle` object:
1. Add: `const val G_<X>_<Y> = "X.Y.Z"` (after the previous last entry)
2. Update: `const val MAX_SUPPORTED = G_<X>_<Y>`

Also in `TestVersions.kt`, check the `AGP` object:
```kotlin
const val MAX_SUPPORTED = AGP_813 // Update once the Gradle MAX_SUPPORTED version is bumped
```
This constant tracks the highest AGP version whose max-supported Gradle version has been validated. If the new Gradle version has been confirmed to work with a newer AGP, update `AGP.MAX_SUPPORTED` accordingly.

Also review the `AgpCompatibilityMatrix` enum entries — when a new Gradle version is added, you may need to update the `maxSupportedGradleVersion` on recent AGP entries that have been validated against it:
```kotlin
AGP_91(AGP.AGP_91, GradleVersion.version(Gradle.G_9_3), GradleVersion.version(Gradle.G_9_4), JavaVersion.VERSION_17),
// ↑ maxSupportedGradleVersion may need bumping to G_9_5 once validated
```
Only update these if you've verified the AGP version actually works with the new Gradle.

**File: `libraries/tools/kotlin-gradle-plugin-integration-tests/build.gradle.kts`**

In the `gradleVersions` list (marked with comment `// Must be in sync with TestVersions.kt KTI-1612`), add `"X.Y.Z"` as the last entry.

**File: `gradle/wrapper/gradle-wrapper.properties`**

Update both:
- `distributionUrl=https\://cache-redirector.jetbrains.com/services.gradle.org/distributions/gradle-X.Y.Z-bin.zip`
- `distributionSha256Sum=<sha256>`

To get the SHA256, fetch it from the Gradle distributions page:
```bash
curl -s "https://services.gradle.org/distributions/gradle-X.Y.Z-bin.zip.sha256"
```

### Step 3 — Update GradleCompatibilityIT

**File: `libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/GradleCompatibilityIT.kt`**

The `properPluginVariantIsUsed` test maps every Gradle version to its expected KGP variant name. Add a new entry at the top of the `when` block:

```kotlin
val expectedVariant = when (gradleVersion) {
    GradleVersion.version(TestVersions.Gradle.G_<X>_<Y>) -> "gradle<VARIANT>"  // add here
    GradleVersion.version(TestVersions.Gradle.G_9_4) -> "gradle813"
    ...
```

The variant name should match the `sourceSetName` of the `GradlePluginVariant` entry that covers this Gradle version. If no new variant was added, it will be the same as the previous last entry (e.g., `"gradle813"`).

### Step 4 — Fix Deprecation Warnings

Search for usages of deprecated APIs in KGP sources:
```
mcp__jetbrains__search_in_files_by_text with appropriate search terms
```

Apply fixes based on the [Known Deprecation Patterns](#known-deprecation-patterns) section below. Focus on deprecations **newly introduced** in the target version — earlier ones should already be fixed.

## Phase 4: Verify Locally (must compile)

The goal is to ensure all changes compile without errors. Full test verification happens later in CI.

### 4a. Check modified files for IDE problems
```
mcp__jetbrains__get_file_problems on each modified file (errorsOnly=false)
```
Fix any warnings related to your changes.

### 4b. Compile the plugin
```bash
./gradlew :kotlin-gradle-plugin:compileKotlin -q
```
This verifies `GradlePluginVariant.kt` and `GradleCommon.kt` changes compile.

### 4c. Compile the integration tests
```bash
./gradlew :kotlin-gradle-plugin-integration-tests:compileTestKotlin -q
```
This verifies `TestVersions.kt`, `GradleCompatibilityIT.kt`, and any test fix changes compile. This is the key step — if this passes, the code is ready for CI.

### 4d. (Optional) Run a quick smoke test
```bash
./gradlew :kotlin-gradle-plugin:functionalTest -q
```
Verifies the plugin itself works with the new variant.

### 4e. CI verification (done by the developer after pushing)
Full integration test verification runs on TeamCity. Tests are grouped by Gradle version:
- Task pattern: `kgpJvmTestsForGradle_<X>_<Y>_<Z>` (e.g., `kgpJvmTestsForGradle_9_5_0`)
- This is configured automatically from the `gradleVersions` list in `build.gradle.kts`
- The developer runs the CI job after pushing and fixes any test failures

**Common test failure patterns** (found during past bumps — expect to fix these after CI runs):

- **XML test result fixtures**: Test output format changes between Gradle versions. Create version-specific XML files in `src/test/resources/testProject/` (e.g., `Gradle95-TEST-all.xml`) and branch the test code:
  ```kotlin
  assertTestResults(
      projectPath.resolve(
          if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_5)) "TEST-all.xml"
          else "Gradle95-TEST-all.xml"
      ),
      "jsNodeTest"
  )
  ```
- **Problems API format changes**: Gradle changes the problems report structure periodically (major change in 9.4). Check `problemsApiTestUtils.kt` and `ProblemsApiIT.kt`
- **Build statistics / FUS service changes**: Service initialization behavior may change. Check `BuildFusStatisticsIT.kt`
- **Test report path/directory changes**: Gradle 9.3 changed report directory structure, Gradle 9.4 introduced path hashing. File existence assertions may need updating
- **Publishing behavior**: New Gradle versions may require explicitly applying the `maven-publish` plugin where it was implicit before
- **Skip incompatible tests**: Use `@GradleTestVersions(maxVersion = TestVersions.Gradle.G_<PREV>)` to exclude tests that cannot work with the new Gradle version yet
- **Configuration cache invalidation**: If tests use custom Gradle properties for CC invalidation, ensure the property format is correct for the new Gradle version (e.g., `-P` vs `-D` prefix, `=` separator)

## Phase 5: Commit

**Before committing, read `docs/code_authoring_and_core_review.md`** — it contains the mandatory commit message rules.

Key rules (not exhaustive):
- Subject line ≤ 72 characters, imperative mood
- Reference the YouTrack issue: `KT-XXXXX` in the subject or body
- Use `^KT-XXXXX Fixed` in the body to auto-close sub-tasks
- Commit tests together with the corresponding code change
- Non-functional changes (deprecation fixes, reformats) go in separate commits from the wrapper/version bumps

Typical commit structure for a Gradle bump:

```
# Commit 1: compile-against change
KT-XXXXX Compile KGP against Gradle X.Y API

Update GradlePluginVariant to add GRADLE_XY entry and update
GRADLE_COMMON_COMPILE_API_VERSION.

^KT-YYYYY Fixed

# Commit 2: run-tests change
KT-XXXXX Run KGP integration tests against Gradle X.Y

Add Gradle X.Y to TestVersions.kt and gradleVersions list.
Update gradle-wrapper.properties to use Gradle X.Y.Z.

^KT-ZZZZZ Fixed

# Commit 3 (if needed): deprecation fixes
KT-XXXXX Fix deprecation warnings with Gradle X.Y

[describe what was deprecated and how it was fixed]
```

---

## Known Deprecation Patterns

Reference for common deprecations encountered in Gradle 9.x bumps. **Always check the upgrading guide for newly introduced ones** — this table only covers known history up to 9.4. For Gradle 10+, the upgrade guide URL will be `upgrading_version_10.html` and the patterns may differ significantly.

### Gradle 9.1 (KT-78763)

| Deprecated API | Fix |
|---------------|-----|
| `archives` configuration for artifact declaration (KT-78620) | Add artifacts as direct task dependencies of `assemble`: `tasks.named("assemble") { dependsOn(myJarTask) }` |
| `Configuration.isVisible` / `setVisible()` — no effect since 9.0 (KT-78754) | Remove calls. If backward compat with <9.0 needed, guard with `if (GradleVersion.current() < GradleVersion.version("9.0"))` |
| Multi-string dependency notation: `implementation(group="org", name="foo", version="1.0")` (KT-82715) | Replace with `implementation("org:foo:1.0")`. For dynamic coords, use `project.dependencyFactory.create(group, name, version)` |
| Toolchain project property via `-P` flag (e.g. `-Porg.gradle.java.installations.auto-detect=false`) (KT-82717) | Change `-P` to `-D` |
| `ReportingExtension.file(String)` | Use `getBaseDirectory().file(String)` |
| `JavaForkOptions.setAllJvmArgs()` | Use `setJvmArgs()` and manually clear other fork options as needed |

### Gradle 9.2 (KT-80356)

| Deprecated/Removed API | Fix |
|----------------------|-----|
| `Project.container(Class)` (KT-80186) | Use `ObjectFactory.domainObjectContainer(Class)` — ensure the class has an `@Inject` constructor |
| `Project.container(Class, NamedDomainObjectFactory)` (KT-80186) | Use `ObjectFactory.domainObjectContainer(Class, factory)` — drop-in replacement |
| `ObjectFactory#dependencyCollector()` **removed** (was incubating) | Use `DependencyCollectors` within Gradle managed types |
| Consumable configurations now lazy | Don't rely on `configure {}` side effects at configuration time; the block may not run until the config is realized |
| RuleSource-based APIs: `ComponentMetadataHandler.all(Object)`, `ComponentSelectionRules.all(Object)` | Use overloads accepting `ComponentMetadataRule` class or `Action` |

### Gradle 9.3 (KT-82883)

| Deprecated API | Fix |
|---------------|-----|
| `Wrapper.getAvailableDistributionTypes()` | Use `Wrapper.DistributionType.values()` |
| Publishing deps on unpublished projects | Ensure all project dependencies of published projects are also published |
| `Project` referential equality (breaking) | Use `Project.equals()` instead of `===` |

### Gradle 9.4 (KT-83858)

| Deprecated/Changed API | Fix |
|-----------------------|-----|
| `KotlinJvmTarget.sourceSets` wrongly created a new container (KT-84874) | Removed `KotlinTarget.sourceSets` property (see KT-85509) |
| `java-gradle-plugin` moves `gradleApi()` from `api` to `compileOnlyApi` | Explicitly add `gradleApi()` to any test source sets that need it at runtime |
| `ProjectBuilder` anchors `layout.settingsDirectory` to project dir | Adjust functional tests that relied on `settingsDirectory` pointing outside the project dir |
| `DomainObjectCollection.findAll(Closure)` | Use `matching(Spec)` instead |
| `AbstractTestTask.onOutput(Closure)` and other closure-based test APIs | Use `addTestOutputListener(TestOutputListener)` / `addTestListener(TestListener)` |
| `apply false` in precompiled script plugins | Remove `apply false` if you want the plugin active; delete the line if you don't need it |

---

## Quick Reference: File Locations

```
=== "Compile against" changes ===

repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradlePluginVariant.kt
  → enum entries + GRADLE_COMMON_COMPILE_API_VERSION

repo/gradle-build-conventions/gradle-plugins-common/src/main/kotlin/gradle/GradleCommon.kt
  → createGradlePluginVariants() explicit variant list (bottom of file)
  → createGradlePluginVariant() private function (Gradle API dependency workaround)

gradle/verification-metadata.xml
  → dependency checksums for gradle-public-api (updated every compile-against bump)

libraries/tools/kotlin-gradle-plugin/api/
  → API signature files (update with ./gradlew :kotlin-gradle-plugin:apiDump if runtime code changes)

=== "Run tests" changes ===

libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/TestVersions.kt
  → Gradle version constants + MAX_SUPPORTED
  → AGP.MAX_SUPPORTED + AgpCompatibilityMatrix (check if update needed)

libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/GradleCompatibilityIT.kt
  → properPluginVariantIsUsed: new version → variant mapping

libraries/tools/kotlin-gradle-plugin-integration-tests/build.gradle.kts
  → gradleVersions list (keep in sync with TestVersions.kt, KTI-1612)

gradle/wrapper/gradle-wrapper.properties
  → distributionUrl + distributionSha256Sum

=== References ===

docs/code_authoring_and_core_review.md
  → mandatory commit message rules (read before committing)
```
