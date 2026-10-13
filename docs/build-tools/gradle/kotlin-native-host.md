# Design for `KotlinNativeHost`, a public replacement for `HostManager` in KGP

## Glossary

* **Host.** The machine the Gradle build runs on, described by its operating system and CPU architecture.
* **Supported host.** A host for which a Kotlin/Native distribution exists: `macos-aarch64`, `linux-x86_64` and `windows-x86_64`.
  See [Kotlin/Native target support](https://kotlinlang.org/docs/native-target-support.html#hosts).
* **Host target.** The `KonanTarget` whose binaries run on the host itself, for example `MACOS_ARM64` on an Apple Silicon Mac.
* **Enabled target.** A `KonanTarget` the host can link final binaries for. Linux and Windows hosts can link only non-Apple targets.
* **Klib cross-compilation.** Compiling a `.klib` for a target the host cannot link. Enabled with
  `kotlin.native.enableKlibsCrossCompilation`.
* **`HostManager`.** `org.jetbrains.kotlin.konan.target.HostManager` from `kotlin-native-utils`. An internal Kotlin/Native class that
  reaches every build script because `kotlin-gradle-plugin-api` has an `api` dependency on `kotlin-native-utils`.

## References

* [KT-74608](https://youtrack.jetbrains.com/issue/KT-74608) Deprecate HostManager in KGP
* [KT-39994](https://youtrack.jetbrains.com/issue/KT-39994) Multiplatform Gradle plugin: Public API for getting host info
* [KT-52980](https://youtrack.jetbrains.com/issue/KT-52980) Gradle: Hostmanager: Add target to KonanTarget
* [KT-64512](https://youtrack.jetbrains.com/issue/KT-64512) Rework `HostManager.enabled`
* [KT-86717](https://youtrack.jetbrains.com/issue/KT-86717) `HostManager.host` throws at configuration time on linux-aarch64
* [KT-66699](https://youtrack.jetbrains.com/issue/KT-66699), [KT-27114](https://youtrack.jetbrains.com/issue/KT-27114) `HostManager` ABI breaks
  that hit Gradle plugins
* [KT-70247](https://youtrack.jetbrains.com/issue/KT-70247) Remove compiler modules from KGP runtime
* [KT-70251](https://youtrack.jetbrains.com/issue/KT-70251) Hide compiler symbols in KGP (the ASM deprecation transformer)

---

## TL;DR

* We add a small, immutable `KotlinNativeHost` value to `kotlin-gradle-plugin-api`. It never throws, and it answers the questions
  users and KGP ask `HostManager` today: which OS, which architecture, whether the host is supported, which `KonanTarget` the host is,
  whether a given target can be linked or run here, and what the distribution classifier is.
* We add a `hostNativeTarget()` shortcut to `KotlinMultiplatformExtension` that replaces the `when (HostManager.host)` block users copy
  between projects.
* Every member is backed by a use case we found either in KGP sources or in a GitHub survey of roughly 500 user build scripts and plugins.
* KGP internals move to the new API in the same change. After that we deprecate `HostManager` for KGP consumers only, with the ASM
  transformer from KT-70251, so Kotlin/Native keeps the class as is.

## Problem

### What is the current state?

`HostManager` is an internal Kotlin/Native helper. Every Gradle build script can see it because `kotlin-gradle-plugin-api` declares
`api(":native:kotlin-native-utils")`, and because `KonanTarget` appears in public KGP signatures. Users and third-party plugins treat it
as public API.

A GitHub code search finds about 430 Kotlin build scripts, 70 Groovy scripts and 770 plugin or `buildSrc` sources that reference it.
The list includes ktor, sqldelight, androidx, aws-crt-kotlin, smithy-kotlin, kotlinx-io, kotlinx-benchmark, KSP,
binary-compatibility-validator, SKIE, sentry-kmp, multipaz and intellij-community. Inside KGP, 34 main source files import it.

### What is the problem?

* It throws at configuration time. `host`, `hostName`, `hostOs()`, `hostArch()` and `HostManager().isEnabled()` throw
  `TargetSupportException` on any host Kotlin/Native ships no distribution for: linux-aarch64, FreeBSD, Windows on ARM. One eager call
  in any subproject fails the whole build, even for JVM-only tasks (KT-86717). Users work around it with reflection (CompleteKotlin),
  with `try/catch TargetSupportException` (binary-compatibility-validator) or by parsing `os.name` themselves (boringssl-kmp,
  filament-kmp).
* Its answers do not match the questions. `isEnabled` is one boolean that means "can link binaries", so users reimplement "can compile a
  klib", "can run tests" and "can cross-link mingw from macOS" by hand, and often get it wrong (KT-64512).
* Its ABI is not stable. Constructor changes broke published plugins twice (KT-27114, KT-66699), and the Kotlin/Native team wants to keep
  the freedom to change it.
* We cannot deprecate it in place. Kotlin/Native is the legitimate owner and user of `HostManager`. KGP needs a deprecation that its
  consumers see and other modules do not.

### What is this proposal solving?

A stable, documented replacement in `kotlin-gradle-plugin-api` that covers the use cases we observed, plus a migration and deprecation
plan for `HostManager`.

### Why is it important?

KT-74608 is planned for 2.5.20. Every new host that appears in CI (linux-aarch64 runners are common now) produces another wave of
configuration-time failures, and today our only answer is "stop using `HostManager`" with nothing to point to instead.

## Anti-Goals

This proposal is deliberately **not** trying to:

* Be a general OS detection library. Uses unrelated to Kotlin/Native that we saw in the wild (Compose Desktop package formats, JNI library
  paths, `where` versus `command -v`) are served well enough by `System.getProperty("os.name")`.
* Provision toolchains. Downloading `kotlin-native-prebuilt` stays inside KGP, and the toolchain work in KT-70247 owns its future.
* Change `HostManager` or `kotlin-native-utils`.
* Hide `KonanTarget`. It is already public through `KotlinNativeTarget.konanTarget` and `konanTargetAttribute`, and two thirds of the
  surveyed call sites need it. Removing it belongs to KT-70247.

## Solution (overview)

```kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeHost

kotlin {
    // Declares the target for this machine under its usual name (macosArm64, linuxX64, ...).
    hostNativeTarget {
        binaries.executable()
    }

    if (nativeHost.isMacOs) {
        iosArm64()
        iosSimulatorArm64()
    }
}

// In tasks and plugins: read the value at configuration time, use it at execution time.
val host = KotlinNativeHost.current()
tasks.withType<CInteropProcess>().configureEach {
    onlyIf("host cannot link ${konanTarget.name}") { host.canLinkBinaries(konanTarget) }
}

// For dependency attributes in compiler plugin runtime projects and other JVM-only modules.
attributes {
    host.konanTarget?.let { attribute(KotlinNativeTarget.konanTargetAttribute, it.name) }
}
```

```groovy
def host = KotlinNativeHost.current()
kotlin {
    hostNativeTarget { binaries.executable() }
    if (host.macOs) { iosArm64() }
}
```

The proposal has three parts:

* `KotlinNativeHost`, an immutable value describing the current host. It lives in `kotlin-gradle-plugin-api`, depends on nothing from
  Gradle except `Serializable`, and never throws.
* Two ways to get it: `KotlinNativeHost.current()` for plugins and projects without the multiplatform extension, and
  `KotlinMultiplatformExtension.nativeHost` for build scripts.
* `KotlinMultiplatformExtension.hostNativeTarget(...)`, which declares the target that matches the host.

## Solution

### `KotlinNativeHost`

```kotlin
package org.jetbrains.kotlin.gradle.dsl

@ExperimentalKotlinGradlePluginApi   // 2.5.20; stable in 2.6.0
interface KotlinNativeHost : java.io.Serializable {
    val os: KotlinNativeHostOs
    val architecture: KotlinNativeHostArchitecture
    val isSupported: Boolean
    val konanTarget: KonanTarget?
    val name: String

    val isMacOs: Boolean   get() = os == KotlinNativeHostOs.MACOS
    val isLinux: Boolean   get() = os == KotlinNativeHostOs.LINUX
    val isWindows: Boolean get() = os == KotlinNativeHostOs.WINDOWS

    fun canLinkBinaries(target: KonanTarget): Boolean
    fun canRunBinaries(target: KonanTarget): Boolean

    companion object {
        @JvmStatic fun current(): KotlinNativeHost
        @JvmStatic fun of(os: KotlinNativeHostOs, architecture: KotlinNativeHostArchitecture): KotlinNativeHost
    }
}

enum class KotlinNativeHostOs { MACOS, LINUX, WINDOWS, UNKNOWN }
enum class KotlinNativeHostArchitecture { X64, ARM64, UNKNOWN }
```

Three rules apply to every member. Nothing throws: an unknown host is a value (`UNKNOWN`, `null`, `false`), not an exception, which is
what KT-86717 asks for. The object is a plain value computed from `os.name` and `os.arch`, so it can be captured in task state and in
the configuration cache without a `Project` or a provider. And host-level members do not use `Family` or `Architecture` from
`kotlin-native-utils`; `KonanTarget` shows up only where the question is about a target.

#### `os`

Replaces `hostIsMac`, `hostIsLinux`, `hostIsMingw`, `hostOs()`, `simpleOsName()` and `host.family`.

OS checks are by far the most common use. `hostIsMac` alone accounts for 21 of the 55 call sites in KGP and is the top member in the
GitHub survey. Users declare Apple targets only on macOS (algolia, multipaz, kotlinxtras), put `onlyIf { hostIsMac }` on Swift and Xcode
tasks (spm4Kmp, kotlin-docc, moko-mvvm, and in KGP `FatFrameworkTask`, CocoaPods and Swift Export), skip `cinterop` on Windows
(smithy-kotlin), and choose `.zip` or `.tar.gz` for the distribution (`NativeCompilerDownloader`).

An enum allows exhaustive `when` and avoids the `"osx"` versus `"macos"` confusion of `hostOs()`. xmlutil compares `hostOs()` with
`"macos"`, which never matches. `UNKNOWN` covers FreeBSD and anything else; the three convenience getters are false there.

#### `architecture`

Replaces `hostArch()`, `hostArchOrNull()` and `host.architecture`.

We need it to pick the right Apple or Linux target on the host (`macosArm64` or `macosX64`, `linuxArm64` or `linuxX64`) and to decide
whether a simulator test can run on this machine (`KotlinNativeTestRunFactories`, and the test run line marker in intellij-community).
xmlutil's `ideaPreset(host)`, our `DisabledNativeCacheTest` and cklib's `hostArch` do the same with strings today. `UNKNOWN` covers
riscv64, x86 and others; KT-77977 was a crash on exactly that path.

#### `isSupported`

Replaces `hostIsSupported`, `hostOrNull != null` and `try { host } catch (TargetSupportException)`.

"Is there a Kotlin/Native distribution for this machine" is asked eight times inside KGP (publishing, commonizer, platform dependencies,
target creation, checkers) and by users before they touch anything native (KSP, binary-compatibility-validator, CrossPaste, convention
plugins that skip native targets on linux-aarch64 CI).

#### `konanTarget`

Replaces `host`, `hostOrNull`, `hostName`, `host.name`, `host.platformName` and `host.fullArchName`.

"Which target runs on this machine" is the second most common question. The answer is nullable because that is the truth on
unsupported hosts; it is what `hostOrNull` already returns, and the throwing `host` goes away. Users need it for
`when (host) { MACOS_ARM64 -> macosArm64() ... }` (hfhbd, kotlinxtras, parsus, lemcoder, kotlinx-io), for `konanTargetAttribute` values
when resolving host klibs (KMP-NativeCoroutines, atomicfu, serialization, our `hostKonanTargetAttribute()` from KT-86717), and for
`target.konanTarget == host` cross-compilation checks (cryptography-kotlin, the intellij run configuration producer). KGP uses it for
`cacheableTargets` property keys in `KonanPropertiesBuildService` and for `isCurrentHost` in `KotlinNativeTargetPreset`.

#### `name`

Replaces `platformName()`, the `simpleOsName()` plus `hostArch()` concatenations, and `hostName` where it is only displayed.

A stable host id in the form `macos-aarch64`, `linux-x86_64`, `windows-x86_64`. On unknown hosts it falls back to a lowercased
`"$os.name-$os.arch"`, which is what `platformName()` does today through `runCatching`, so diagnostics keep working everywhere. Every
KGP diagnostic that prints the host uses it (`NativeHostNotSupportedError`, `PublishingDisabledOnUnsupportedHost`,
`DisabledNativeTargetTaskWarning`), and so does the `kotlin-native-prebuilt-<version>-<name>` classifier (`NativeCompilerDownloader`,
SKIE, intellij-community, and CompleteKotlin, which currently gets it through reflection).

#### `canLinkBinaries(target)`

Replaces `HostManager().isEnabled(target)`, `HostManager().enabled`, `enabledByHost` and KGP's
`enabledOnCurrentHostForBinariesCompilation`.

This is the one piece of real state in `HostManager`, the host-to-targets table, and the question users most often rebuild by hand
(aws-crt-kotlin, CrossPaste). The name says what is answered: final linking, not klib compilation. It returns `false` on unsupported
hosts instead of throwing. Call sites: `cinterop` and CMake `onlyIf` gates (aws-crt-kotlin, kotlin-sqlite-bindings, powersync),
per-target ABI dumps in binary-compatibility-validator, KSP's host check, and in KGP `PlatformLibrariesGenerator`,
`CinteropCrossCompilationChecker`, FUS metrics and link task enablement in `KotlinNativeConfigureBinariesSideEffect`.

The table starts as a copy of `HostManager.enabledByHost` and lives in KGP, so Kotlin/Native can change its own table without changing
Gradle behaviour. A test asserts both tables agree for as long as `kotlin-native-utils` is on the classpath, so they cannot drift
quietly.

#### `canRunBinaries(target)`

Replaces `target == HostManager.host` for host targets and `hostIsMac && host.architecture == target.architecture` for simulator targets
(`KotlinNativeTestRunFactories`).

"Can the test executable run here" is a different question from "can it be linked here". Users currently disable `linuxX64Test` and
`linkDebugTestLinuxX64` by comparing OS booleans (sqldelight, SQLiter, FileKit, kzmq). One method holds the rules: the host target
itself, and on macOS the simulator targets with the host's architecture. KGP uses it in the host and simulator test run factories;
intellij uses the same logic for run line markers.

#### `current()`

Replaces the `HostManager` companion and `HostManager()` instantiation.

Plugins and projects without the multiplatform extension need the host too: compiler plugin runtime modules that resolve host klibs,
convention plugins applied before the Kotlin plugin. `current()` is a pure function of `os.name` and `os.arch` with no static cache.
Reading two system properties is cheap, Gradle records the reads as configuration cache inputs, and keeping the companion free of state
matters because the daemon reuses plugin classloaders between builds and because our functional tests change system properties to
simulate hosts.

#### `of(os, architecture)`

New. Plugin authors today thread a `hostIsMac: Boolean = HostManager.hostIsMac` parameter through their code to test both branches
(sentry-kmp, SKIE's `KgpShim`). A public factory lets them build hosts in tests, and lets our own tests cover every OS and architecture
combination without `withModifiedSystemProperties`.

#### What is not included

`supportedHosts`. The internal `HostManager.supportedHosts` extension only feeds the "supported hosts are ..." line of two diagnostics.
No surveyed user code enumerates hosts; they ask about the current host or about a target. A public list of strings would make the
`name` format a contract by accident, so the list stays internal. If a need shows up it can be added later as `Set<KotlinNativeHost>`
without breaking anything.

`targetManager()`, `resolveAlias()`, `listAliases()`, `jniHostPlatformIncludeDir`, `defaultJvmArgs`. These serve the compiler CLI. We
found no Gradle usage.

### `KotlinMultiplatformExtension.nativeHost`

```kotlin
val KotlinMultiplatformExtension.nativeHost: KotlinNativeHost
```

The same value as `current()`, exposed where about 60% of the surveyed usages live: inside `kotlin { }`.

### `KotlinMultiplatformExtension.hostNativeTarget(...)`

```kotlin
// Kotlin DSL. Without a name the target gets the canonical preset name of the host (macosArm64, linuxX64, mingwX64, ...).
fun hostNativeTarget(
    name: String? = null,
    configure: KotlinNativeTargetWithHostTests.() -> Unit = {},
): KotlinNativeTargetWithHostTests?

// Groovy and Java callers, mirroring the overloads of macosArm64() and linuxX64().
fun hostNativeTarget(): KotlinNativeTargetWithHostTests?
fun hostNativeTarget(name: String): KotlinNativeTargetWithHostTests?
fun hostNativeTarget(configure: Action<KotlinNativeTargetWithHostTests>): KotlinNativeTargetWithHostTests?
fun hostNativeTarget(name: String, configure: Action<KotlinNativeTargetWithHostTests>): KotlinNativeTargetWithHostTests?
```

Replaces the `when (HostManager.host) { ... }` and `when { hostIsMac -> macosX64("native") ... }` blocks that projects copy from each
other and from the Kotlin/Native template, including seven of our own integration test projects. This resolves KT-52980.

It is the most common user pattern and the one most often wrong: `macosX64` hard-coded on Apple Silicon, `linuxArm64` missing, or an
`else -> error(...)` that fails JVM-only builds on hosts without a distribution. The typical project is a native CLI tool, a benchmark,
a sample, or a test harness for a compiler plugin: it wants exactly one native target, the one for this machine.

The function maps `nativeHost.konanTarget` to the matching preset function. On an unsupported host it returns `null` and reports a new
`HostNativeTargetUnavailable` warning instead of failing configuration. Calling it twice with the same name returns the same target,
like the preset functions do.

About the `name` parameter. It is the Gradle target name, the same parameter as in `macosArm64(name)`, and it drives the names of the
derived source sets, compilations and tasks (`<name>Main`, `compileKotlin<Name>`, `linkDebugExecutable<Name>`).

* Without a name the target gets its canonical preset name, so `hostNativeTarget()` on an Apple Silicon Mac is the same as
  `macosArm64()`. This works with the default hierarchy template and with projects that declare other targets next to it.
* With a custom name, usually `"native"`, task and source set names are the same on every CI runner (`linkDebugExecutableNative`
  everywhere). This is why the surveyed `when` blocks pass it, and it is the right choice for single-target samples, benchmarks and CLI
  tools. We do not make it the default because a target called `native` creates `nativeMain`, which clashes with the shared
  `nativeMain` of the hierarchy template; KGP then skips the template and reports `KotlinDefaultHierarchyFallbackIllegalTargetNames`.
  That existing warning already covers the custom name case.

### The target-level question: can this klib be compiled here?

`canCompileKlib(target)` is deliberately not on `KotlinNativeHost`. Whether a klib can be produced depends on the project
(`kotlin.native.enableKlibsCrossCompilation`) and on the compilation (whether it has cinterops). KGP already models this as
`KotlinNativeTarget.crossCompilationOnCurrentHostSupported: Future<Boolean>`, and we make it public:

```kotlin
@ExperimentalKotlinGradlePluginApi
val KotlinNativeTarget.isCompilableOnCurrentHost: Provider<Boolean>
```

That gives KSP-style checks (`hostOrNull != null && klibCrossCompilationEnabled && !hasCinterops`) and kmp-tor's property toggling a
supported answer. It is a `Provider` because the value is only known after compilations are finalised.

### Migration from `HostManager`

| Legacy API | Replacement | Notes |
|---|---|---|
| `hostIsMac`, `hostIsLinux`, `hostIsMingw` | `isMacOs`, `isLinux`, `isWindows`, or `os` | |
| `hostIsSupported`, `hostOrNull != null` | `isSupported` | |
| `host` | `konanTarget` | callers must handle `null` |
| `hostOrNull` | `konanTarget` | |
| `hostName`, `host.name` | `konanTarget?.name` | |
| `hostOs()`, `simpleOsName()` | `os` | enum instead of `"osx"` strings |
| `hostArch()`, `hostArchOrNull()` | `architecture` | |
| `platformName()` | `name` | same format |
| `HostManager().isEnabled(t)`, `.enabled`, `.enabledByHost` | `canLinkBinaries(t)` | |
| `HostManager().targets`, `.targetValues` | `KonanTarget.predefinedTargets` | already public |
| `host.family`, `host.architecture` | `konanTarget?.family`, `architecture` | |

### Deprecating `HostManager` for KGP consumers only

`HostManager` stays untouched for Kotlin/Native. The mechanism for a consumer-side deprecation already exists: the
`asm-deprecating-transformer` convention from KT-70251 adds `@kotlin.Deprecated` with a message to classes bundled into KGP jars, and a
checked `.deprecations` dump guards the set. It only works on `embedded` artifacts, and `kotlin-native-utils` is currently a published
`api` dependency of `kotlin-gradle-plugin-api`. So we embed `kotlin-native-utils` into `kotlin-gradle-plugin-api` without relocation,
as `kotlin-gradle-compiler-types` already is, drop the `api` dependency and the BOM entry, and deprecate
`org.jetbrains.kotlin.konan.target.HostManager**` and `TargetSupportException` with a message that points to `KotlinNativeHost`.
`KonanTarget` and its neighbours stay undeprecated because they are public API.

The risk is duplicate classes when `kotlin-native-utils` also arrives on the classpath another way, for example through
`kotlin-compiler-embeddable`. We cover that with a smoke integration test that puts both on the buildscript classpath. This step is
sequenced after `KotlinNativeHost` ships and needs agreement with the owners of KT-70247.

### Rollout

* 2.5.20: `KotlinNativeHost`, `nativeHost`, `hostNativeTarget` and `isCompilableOnCurrentHost` ship under
  `@ExperimentalKotlinGradlePluginApi`. KGP internals are migrated, so no `HostManager` import remains outside the implementation file.
  Our own build scripts and integration test projects are migrated. Documentation page and a "What's new" entry.
* 2.6.0: the API is stabilised. `HostManager` gets a deprecation warning for KGP consumers through the ASM transformer. KT-39994,
  KT-52980 and KT-86717 are closed.
* 2.7.0: the deprecation becomes an error, following the KGP deprecation process (KT-A-1174).
* 2.8.0: the class is no longer reachable from KGP, and `kotlin-native-utils` leaves the KGP API surface together with KT-70247.

### Gradle conventions we checked against

We checked the design against the current Gradle user guide (configuration cache requirements, isolated projects, plugin development,
Kotlin DSL) and against the existing conventions of `kotlin-gradle-plugin-api`.

* Configuration cache inputs. `current()` reads only `os.name` and `os.arch`. The user guide says direct system property reads at
  configuration time are recorded as configuration inputs, so a `ValueSource` is not needed. We keep `ValueSource` for cases like the
  Xcode device lookup, where several sources or an external process are involved.
* Task state. `KotlinNativeHost` is `Serializable`, holds two enums and derived values, and has no `Project` reference and no lambdas.
  `KonanTarget` instances are the public singletons KGP tasks already serialize today.
* Reading values at the right time. All examples read the host into a local before `onlyIf` or `doLast`, which is what the user guide
  asks for since Gradle 9 deprecated reading extensions inside task actions. `onlyIf` uses the overload with a reason, so a skipped task
  explains itself in `--info` output and in build scans.
* Task inputs. A task whose output depends on the host declares it with `inputs.property("kotlin.native.host", host.name)`. KGP's native
  tasks already key on `konanTarget`; the documentation page will show the pattern for user tasks.
* Lazy where the value is lazy. `isCompilableOnCurrentHost` is a `Provider<Boolean>` because it depends on finalised compilations. Host
  facts are plain values because they are constants for the build JVM, and wrapping constants in providers only adds indirection.
* No static state. `current()` is a pure function with no companion-level cache.
* Isolated projects. There is no cross-project access, no `rootProject`, and no shared mutable service. `nativeHost` is a read-only view
  over process-level constants, and `hostNativeTarget` only calls the project's own preset functions.
* Groovy and Java. `current()` and `of()` are `@JvmStatic`, `hostNativeTarget` has `Action<T>` overloads, and the boolean getters follow
  bean naming so Groovy reads `host.macOs` and `host.supported`.
* Managed types. We do not use them for `KotlinNativeHost` because it is a value, not configurable state, and `objects.newInstance` would
  require a `Project`. The extension property is a `val`, so build scripts cannot reassign it.
* Diagnostics. Unsupported hosts are reported through `KotlinToolingDiagnostics`, never with `println` or with an exception at
  configuration time.
* API evolution. `@ExperimentalKotlinGradlePluginApi` for one release, `@since 2.5.20` on every declaration, `apiDump` guards the
  surface, and the legacy deprecation follows KT-A-1174. Users are not expected to implement the interface; `of()` is the only factory,
  which leaves us free to add members.

## Testing

None of the behaviour below is covered today except the unsupported-host diagnostics. All tests are added together with the
implementation.

### Unit tests in `kotlin-gradle-plugin/src/test`

`KotlinNativeHostTest` is parameterised over `os.name` and `os.arch`:

| Input | Expected |
|---|---|
| `Mac OS X` / `aarch64` | `MACOS`, `ARM64`, supported, `MACOS_ARM64`, `macos-aarch64` |
| `Mac OS X` / `x86_64` | `MACOS`, `X64`, not supported, `null`, `macos-x86_64` (the macos_x64 host was dropped in KT-84955) |
| `Linux` / `amd64` | `LINUX`, `X64`, supported, `LINUX_X64`, `linux-x86_64` |
| `Linux` / `aarch64` | `LINUX`, `ARM64`, not supported, `null`, `linux-aarch64` (KT-86717) |
| `Windows 11` / `amd64` | `WINDOWS`, `X64`, supported, `MINGW_X64`, `windows-x86_64` |
| `Windows 11` / `arm64` | `WINDOWS`, `ARM64`, not supported, `null` |
| `FreeBSD` / `amd64` | `UNKNOWN`, `X64`, not supported, `null`, `freebsd-amd64` (KT-74277, KT-75741) |
| `Linux` / `riscv64` | `LINUX`, `UNKNOWN`, not supported (KT-77977) |

Also:

* `canLinkBinaries` for every `KonanTarget.predefinedTargets` value on each supported host, compared with `HostManager().enabledByHost`
  while that class is still on the test classpath, and `false` for every target on an unsupported host.
* `canRunBinaries`: the host target is true; `IOS_SIMULATOR_ARM64` is true only on `macos-aarch64`; `IOS_X64` is false there;
  `LINUX_X64` is false on macOS; everything is false on unsupported hosts.
* `of()` produces equal, serializable values, and `current()` equals `of(detected os, detected arch)`.
* No member throws for any input in the table.

### Functional tests in `kotlin-gradle-plugin/src/functionalTest`

These check configuration state only, following the KGP test layering.

* `HostNativeTargetTests`. On each simulated supported host, `hostNativeTarget()` creates the target the matching preset function would,
  under its canonical name, and runs `configure`. `hostNativeTarget("native")` creates it under that name. A second call returns the same
  instance. Together with `jvm()` and the default hierarchy template, the canonical name produces no
  `KotlinDefaultHierarchyFallbackIllegalTargetNames` warning and `"native"` produces the existing one. On `Linux/aarch64` it returns
  `null`, reports `HostNativeTargetUnavailable` once, and the project still evaluates with a `jvm()` target.
* `KotlinNativeHostDslTests`. `kotlin.nativeHost` equals `KotlinNativeHost.current()`. `isCompilableOnCurrentHost` is true for an
  enabled target, true for a disabled target with `enableKlibsCrossCompilation=true` and no cinterops, and false once a cinterop is
  added.
* The existing `UnsupportedKotlinNativeHostTest`, `DisabledNativeCacheTest`, `CrossCompilationWithCinteropTests` and
  `KotlinNativeToolchainTest` move from `HostManager` to `KotlinNativeHost.of(...)` and must keep passing. That is the evidence that the
  internal migration kept behaviour.
* The diagnostics tests for `NativeHostNotSupportedError` and `PublishingDisabledOnUnsupportedHost` get their golden text updated once
  for the new `name` format.

### Integration tests in `kotlin-gradle-plugin-integration-tests`

* A new `native-host-dsl` test project uses `hostNativeTarget("native")` (so the asserted task name is the same on every host),
  `nativeHost.isMacOs` to gate an Apple target, and a captured `host.canLinkBinaries(konanTarget)` in `onlyIf` on a cinterop task. We
  assert that `linkDebugExecutableNative` runs on all three CI hosts, that Apple tasks are absent off macOS, and that the configuration
  cache is reused on the second run.
* The seven existing test projects that import `HostManager` (`nativeCompilerPluginTransitiveClassPath`, `cinterop-failing` and the
  others) switch to the new DSL.
* A Groovy smoke test calls `KotlinNativeHost.current().macOs` and `kotlin.hostNativeTarget { }` from `build.gradle`.
* For the 2.6.0 step: a build script that calls `HostManager.hostIsMac` compiles with a deprecation warning that names the replacement.
  The assertion checks the warning line only.

### Repository guards

* `CodeConformanceTest`, added in the KT-86717 branch, fails on any `HostManager` import under
  `libraries/tools/kotlin-gradle-plugin/src/common` except the implementation file.
* `:kotlin-gradle-plugin-api:apiDump` is updated and `apiCheck` guards the surface.
* The `checkDeprecationsFor*` dumps are updated when the ASM step lands.

## Related work

* [KT-86717](https://youtrack.jetbrains.com/issue/KT-86717) fixes the eager `HostManager.host` calls in our own build scripts and adds
  `hostKonanTargetAttribute()`; that helper becomes a one-liner over `konanTarget` once this API exists.
* [kotlin-native-toolchain](https://github.com/adamko-dev/kotlin-native-toolchain) by Adam Semenenko provisions distributions outside
  KGP and generates dependency data. It does not model the host, so the two complement each other. Its `knt-dependency-data` could later
  replace the classifier logic in `NativeCompilerDownloader` that `name` feeds.

# Prior art

_What would be the alternatives to the proposed solution? What would happen if we don't solve the problem? Why should this proposal be
preferred?_

**Possible alternatives:**

* Point users at Gradle's `org.gradle.nativeplatform.platform.OperatingSystem`, with a few extension functions in
  `kotlin-gradle-plugin-api`. This was the first suggestion in the team discussion. It belongs to the Gradle native plugins, which Gradle
  no longer develops, it has no notion of Kotlin/Native targets or of which targets a host can link, and it would cover about a third of
  the surveyed call sites. We would still need everything in this proposal for the rest.
* Fix `HostManager` itself: add the missing hosts to `hostMapping`, stop throwing. Kotlin/Native does not want the class to become a
  public contract, and every change to it has to be checked against all existing usages. It also does not solve the ABI problem.
* Only deprecate, with no replacement. Users would keep the `when` block and the `os.name` parsing, and we would keep answering the same
  issues.
* Adopt `knt-dependency-data` from kotlin-native-toolchain. It solves a different problem (provisioning and dependency metadata) and does
  not describe the host.

If we do nothing, each new CI host produces another KT-86717, and each `HostManager` change another KT-66699.

## FAQ

_Answers to questions asked while discussing this proposal that do not belong in the proposal text itself._

**Q:** Why a value and not a `Provider<KotlinNativeHost>`?

**A:** The host cannot change during a build, the inputs are two system properties the configuration cache already tracks, and every
surveyed call site wants a plain boolean inside `if`, `when` or `onlyIf`. A provider would make the common case worse without making
anything more correct. `isCompilableOnCurrentHost` is a provider because it depends on project state.

**Q:** Why not a `ValueSource`?

**A:** A `ValueSource` is for inputs Gradle cannot track by itself: processes, files, combinations of several properties. Two direct
`System.getProperty` reads are tracked on their own and cost nothing. Wrapping them would hide them from the configuration cache
report and force every call site through `Provider.get()`.

**Q:** Why does `hostNativeTarget()` create the target eagerly instead of returning a `NamedDomainObjectProvider`?

**A:** Every preset function creates its target eagerly today; `macosArm64()` returns the target. Matching that keeps the shortcut a
direct replacement for the `when` block. Making only this entry point lazy would be inconsistent and gain nothing, because target
creation is cheap and the target is needed for source set wiring anyway.

**Q:** Why return `null` from `hostNativeTarget()` instead of throwing?

**A:** Throwing recreates KT-86717: a JVM-only task on a linux-aarch64 machine fails because one module declared a host target. A
warning plus `null` keeps the build configurable, and the null is easy to handle with `?.apply { }`.

**Q:** Why keep `KonanTarget` in the API?

**A:** It is already public through `KotlinNativeTarget.konanTarget` and `konanTargetAttribute`, and users need it for attributes,
`when` matching and `Family` checks. Hiding it is a separate and larger project, KT-70247.

**Q:** Why copy the enablement table instead of delegating to `HostManager`?

**A:** So that KGP can drop the dependency later without a behaviour change, and so that Kotlin/Native can change `enabledByHost` for
compiler purposes without changing Gradle behaviour by accident. The comparison test keeps them aligned while both exist.

**Q:** Why not `supportedHosts` as a list of strings?

**A:** Nobody outside KGP's diagnostics needs the list, and strings would make the `name` format a contract. It stays internal; a
`Set<KotlinNativeHost>` can be added later if a need appears.
