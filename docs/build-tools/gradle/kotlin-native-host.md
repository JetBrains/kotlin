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
* [KT-84955](https://youtrack.jetbrains.com/issue/KT-84955) Deprecate Apple x86_64 targets as Error, which also dropped the Intel Mac host

---

## TL;DR

* We add a small, immutable `KotlinNativeHost` value as a Kotlin Build Tools library (`native-host`), free of Gradle and of
  `kotlin-native-utils`, so Maven, JPS and other integrations can use it too. It never throws, and it answers the questions users
  and KGP ask `HostManager` today: which OS, which architecture, whether the host is supported, which Kotlin/Native target the host
  is, whether a given target can be linked or run here, and what the distribution classifier is.
* `kotlin-gradle-plugin-api` depends on the library, so build scripts see the type, and adds a thin `KonanTarget` bridge for Gradle
  build logic that already works with `KonanTarget`.
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

A stable, documented replacement that covers the use cases we observed, usable from every build tool integration, plus a migration
and deprecation plan for `HostManager`.

### Why is it important?

KT-39994 and KT-74608 are planned for 2.5.20. Every new host that appears in CI (linux-aarch64 runners are common now) produces
another wave of configuration-time failures, and today our only answer is "stop using `HostManager`" with nothing to point to instead.

## Anti-Goals

This proposal does not try to:

* Be a general OS detection library. Uses unrelated to Kotlin/Native that we saw in the wild (Compose Desktop package formats, JNI library
  paths, `where` versus `command -v`) are served well enough by `System.getProperty("os.name")`.
* Provision toolchains. Downloading `kotlin-native-prebuilt` stays inside KGP, and the toolchain work in KT-70247 owns its future.
* Change `HostManager` or `kotlin-native-utils`.
* Hide `KonanTarget`. It is already public through `KotlinNativeTarget.konanTarget` and `konanTargetAttribute`, and two thirds of the
  surveyed call sites need it. Removing it belongs to KT-70247.

## Solution (overview)

```kotlin
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost
import org.jetbrains.kotlin.gradle.dsl.canLinkBinaries
import org.jetbrains.kotlin.gradle.dsl.konanTarget

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
tasks.withType<CInteropProcess>().configureEach {
    val host = KotlinNativeHost.current()
    onlyIf("host cannot link ${konanTarget.name}") { host.canLinkBinaries(konanTarget) }
}

// For dependency attributes in compiler plugin runtime projects and other JVM-only modules.
attributes {
    KotlinNativeHost.current().konanTarget?.let { attribute(KotlinNativeTarget.konanTargetAttribute, it.name) }
}
```

In a `.gradle.kts` script the local must live inside the configuration block. A top-level `val` is a property of the script
object, and a lambda that captures it drags the whole script into the task, which the configuration cache refuses to store.

```groovy
kotlin {
    hostNativeTarget { binaries.executable() }
    if (nativeHost.macOs) { iosArm64() }
}
```

The proposal has three parts:

* `KotlinNativeHost`, an immutable value describing the current host. It is a Kotlin Build Tools library
  (`build-tools/libraries/native-host`, artifact `org.jetbrains.kotlin:build-tools-native-host`), depends on nothing
  but the stdlib, names targets by their Kotlin/Native names (`macos_arm64`), and never throws. `kotlin-gradle-plugin-api` depends on
  it and adds `KonanTarget`-typed extensions on top.
* Two ways to get it: `KotlinNativeHost.current()` for plugins and projects without the multiplatform extension, and
  `KotlinMultiplatformExtension.nativeHost` for build scripts.
* `KotlinMultiplatformExtension.hostNativeTarget(...)`, which declares the target that matches the host.

## Solution

### `KotlinNativeHost`

```kotlin
// build-tools/libraries/native-host
package org.jetbrains.kotlin.buildtools.nativehost

public interface KotlinNativeHost : java.io.Serializable {
    public val os: KotlinNativeHostOs
    public val architecture: KotlinNativeHostArchitecture
    public val isSupported: Boolean
    public val targetName: String?        // "macos_arm64", "linux_x64", "mingw_x64" or null
    public val name: String               // "macos-aarch64"

    public val isMacOs: Boolean
    public val isLinux: Boolean
    public val isWindows: Boolean

    public fun canLinkBinaries(targetName: String): Boolean
    public fun canRunBinaries(targetName: String): Boolean

    public companion object {
        @JvmStatic public fun current(): KotlinNativeHost
        @JvmStatic public fun of(os: KotlinNativeHostOs, architecture: KotlinNativeHostArchitecture): KotlinNativeHost
        @JvmStatic public fun supportedHosts(): List<KotlinNativeHost>
    }
}

public enum class KotlinNativeHostOs { MACOS, LINUX, WINDOWS, UNKNOWN }
public enum class KotlinNativeHostArchitecture { X64, ARM64, UNKNOWN }
```

```kotlin
// kotlin-gradle-plugin-api, package org.jetbrains.kotlin.gradle.dsl
@ExperimentalKotlinGradlePluginApi val KotlinNativeHost.konanTarget: KonanTarget?
@ExperimentalKotlinGradlePluginApi fun KotlinNativeHost.canLinkBinaries(target: KonanTarget): Boolean
@ExperimentalKotlinGradlePluginApi fun KotlinNativeHost.canRunBinaries(target: KonanTarget): Boolean
```

Three rules apply to every member. Nothing throws: an unknown host is a value (`UNKNOWN`, `null`, `false`), not an exception, which is
what KT-86717 asks for. The object is a plain value computed from `os.name` and `os.arch`, so it can be captured in task state and in
the configuration cache without a `Project` or a provider. And the library depends only on the stdlib: targets are the Kotlin/Native
target names, and `KonanTarget` appears only in the `kotlin-gradle-plugin-api` bridge, which already has `kotlin-native-utils`.

Why a Build Tools library and not a type in `kotlin-gradle-plugin-api`: the host question is not Gradle's. The Kotlin Maven plugin,
JPS, kotlin-toolchain and Bazel rules all need the same answer, and `build-tools/libraries` exists for that kind
of code. Keeping the type free of `kotlin-native-utils` also means it survives KT-70247, which removes that module from the KGP API.

#### `os`

Replaces `hostIsMac`, `hostIsLinux`, `hostIsMingw`, `hostOs()`, `simpleOsName()` and `host.family`.

OS checks are the most common use. `hostIsMac` alone accounts for 23 of the 61 call sites in KGP and is the top member in the
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
riscv64, x86 and others; KT-77977 was a crash on that path.

#### `isSupported`

Replaces `hostIsSupported`, `hostOrNull != null` and `try { host } catch (TargetSupportException)`.

"Is there a Kotlin/Native distribution for this machine" is asked eight times inside KGP (publishing, commonizer, platform dependencies,
target creation, checkers) and by users before they touch anything native (KSP, binary-compatibility-validator, CrossPaste, convention
plugins that skip native targets on linux-aarch64 CI).

#### `targetName`, and `konanTarget` in the Gradle bridge

Replaces `host`, `hostOrNull`, `hostName`, `host.name`, `host.platformName` and `host.fullArchName`.

The library gives the Kotlin/Native target name (`macos_arm64`), which is what `konanTargetAttribute` and `konan.properties` keys
use. `kotlin-gradle-plugin-api` adds `konanTarget: KonanTarget?` on top for `when` matching and `Family` checks.

"Which target runs on this machine" is the second most common question. The answer is nullable because that is the truth on
unsupported hosts; it is what `hostOrNull` already returns, and the throwing `host` goes away. Users need it for
`when (host) { MACOS_ARM64 -> macosArm64() ... }` (hfhbd, kotlinxtras, parsus, lemcoder, kotlinx-io), for `konanTargetAttribute` values
when resolving host klibs (KMP-NativeCoroutines, atomicfu, serialization, our `hostKonanTargetAttribute()` from KT-86717), and for
`target.konanTarget == host` cross-compilation checks (cryptography-kotlin, the intellij run configuration producer). KGP uses it for
`cacheableTargets` property keys in `KonanPropertiesBuildService` and for `isCurrentHost` in `KotlinNativeTargetPreset`.

#### `name`

Replaces `platformName()`, the `simpleOsName()` plus `hostArch()` concatenations, and `hostName` where it is only displayed.

A stable host id in the form `macos-aarch64`, `linux-x86_64`, `windows-x86_64`. Each part falls back on its own to the lowercased
system property, so FreeBSD on amd64 is `freebsd-x86_64` and Windows on riscv64 is `windows-riscv64`. `platformName()` gives the same
strings today through `runCatching`, so diagnostics keep working everywhere. Every KGP diagnostic that prints the host uses it
(`NativeHostNotSupportedError`, `PublishingDisabledOnUnsupportedHost`, `DisabledNativeTargetTaskWarning`), and so does the
`kotlin-native-prebuilt-<version>-<name>` classifier (`NativeCompilerDownloader`, SKIE, intellij-community, and CompleteKotlin, which
currently gets it through reflection).

#### `canLinkBinaries(targetName)`, with a `KonanTarget` overload in the bridge

Replaces `HostManager().isEnabled(target)`, `HostManager().enabled` and `enabledByHost`, including the call behind KGP's
`enabledOnCurrentHostForBinariesCompilation`.

This is the one piece of real state in `HostManager`, the host-to-targets table, and the question users most often rebuild by hand
(aws-crt-kotlin, CrossPaste). The name says what is answered: final linking, not klib compilation. It returns `false` on unsupported
hosts instead of throwing. Call sites: `cinterop` and CMake `onlyIf` gates (aws-crt-kotlin, kotlin-sqlite-bindings, powersync),
per-target ABI dumps in binary-compatibility-validator, KSP's host check, and in KGP `PlatformLibrariesGenerator`,
`CinteropCrossCompilationChecker`, FUS metrics and link task enablement in `KotlinNativeConfigureBinariesSideEffect`.

The table starts as a copy of `HostManager.enabledByHost` and lives in the `native-host` library, so Kotlin/Native can change its own
table without changing Gradle behaviour. A test in KGP asserts both tables agree for as long as `kotlin-native-utils` is on the
classpath, so they cannot drift unnoticed.

#### `canRunBinaries(targetName)`, with a `KonanTarget` overload in the bridge

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

#### `supportedHosts()`

Replaces the internal `HostManager.supportedHosts` extension and `enabledByHost.keys`.

The typed list of hosts Kotlin/Native ships a distribution for. KGP derives the "supported hosts are ..." line of its diagnostics and
the set of targets linkable on every host from it, so the list is kept in one place. It is a list of `KotlinNativeHost` values, not of
strings, so the `name` format does not become a contract.

#### What is not included

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

`KotlinNativeHost` has no `canCompileKlib(target)`, because whether a klib can be produced depends on the project
(`kotlin.native.enableKlibsCrossCompilation`) and on the compilation (whether it has cinterops). KGP already models this as
the internal `KotlinNativeTarget.crossCompilationOnCurrentHostSupported: Future<Boolean>`, and we expose it as a public `Provider`:

```kotlin
@ExperimentalKotlinGradlePluginApi
val KotlinNativeTarget.isCompilableOnCurrentHost: Provider<Boolean>
```

That gives KSP-style checks (`hostOrNull != null && klibCrossCompilationEnabled && !hasCinterops`) and kmp-tor's property toggling a
supported answer. It is a `Provider` because the value is only known after compilations are finalised. Reading it earlier fails
with a message that says when the value is available and suggests reading it in a task action or wiring it into a task input.

### Migration from `HostManager`

| Legacy API | Replacement | Notes |
|---|---|---|
| `hostIsMac`, `hostIsLinux`, `hostIsMingw` | `isMacOs`, `isLinux`, `isWindows`, or `os` | |
| `hostIsSupported`, `hostOrNull != null` | `isSupported` | |
| `host` | `targetName`, or `konanTarget` from the bridge | callers must handle `null` |
| `hostOrNull` | `targetName`, or `konanTarget` from the bridge | |
| `hostName`, `host.name` | `targetName` | |
| `hostOs()`, `simpleOsName()` | `os` | enum instead of `"osx"` strings |
| `hostArch()`, `hostArchOrNull()` | `architecture` | |
| `platformName()` | `name` | same format |
| `HostManager().isEnabled(t)`, `.enabled`, `.enabledByHost` | `canLinkBinaries(t)` | |
| `HostManager().targets`, `.targetValues` | `KonanTarget.predefinedTargets` | already public |
| `host.family`, `host.architecture` | `konanTarget?.family`, `architecture` | |

### Deprecating `HostManager` for KGP consumers only

`HostManager` stays untouched for Kotlin/Native. KT-74608 deprecates it for KGP consumers with the `asm-deprecating-transformer`
convention from KT-70251, which adds `@kotlin.Deprecated` with a message to classes bundled into a jar, and a checked `.deprecations`
dump guards the set. The transformer only works on `embedded` artifacts, and `kotlin-native-utils` is currently a published `api`
dependency of `kotlin-gradle-plugin-api`. So KT-74608 bundles `kotlin-native-utils` into `kotlin-gradle-plugin-api` without relocation,
as `kotlin-gradle-compiler-types` already is, and deprecates `HostManager` and its companion with a message that points to
`KotlinNativeHost`. `KonanTarget` and the rest of the module stay undeprecated because they are public API. So does
`TargetSupportException`: only `HostManager` throws it, and a second warning on every `catch` would not help anyone migrate.

The risk is a second, undeprecated copy of `kotlin-native-utils` on the buildscript classpath, for example through
`kotlin-klib-commonizer-api` or `kotlin-compiler-embeddable`. An integration test puts a plain copy next to KGP and checks that the
build still works. The deprecation needs agreement with the owners of KT-70247.

### Rollout

* 2.5.20: `nativeHost`, `hostNativeTarget`, `isCompilableOnCurrentHost` and the `KonanTarget` bridge ship under
  `@ExperimentalKotlinGradlePluginApi`. `KotlinNativeHost` itself needs no opt-in, so users can move off `HostManager` without
  experimental API. KGP internals are migrated, so no `HostManager` import remains in KGP main sources. Our own build scripts and
  integration test projects are migrated. `HostManager` gets a deprecation warning for KGP consumers through the ASM transformer
  (KT-74608). Documentation page and a "What's new" entry.
* 2.6.0: the experimental KGP API is stabilised.
* 2.7.0: the deprecation becomes an error, following the KGP deprecation process (KT-A-1174).
* 2.8.0: the class is no longer reachable from KGP, and `kotlin-native-utils` leaves the KGP API surface together with KT-70247.

### Gradle conventions we checked against

We checked the design against the current Gradle user guide (configuration cache requirements, isolated projects, plugin development,
Kotlin DSL) and against the existing conventions of `kotlin-gradle-plugin-api`.

* Configuration cache inputs. `current()` reads only `os.name` and `os.arch`. The user guide says direct system property reads at
  configuration time are recorded as configuration inputs, so a `ValueSource` is not needed. We keep `ValueSource` for cases like the
  Xcode device lookup, where several sources or an external process are involved.
* Task state. `KotlinNativeHost` is `Serializable`, holds two enums and the two raw system property values, and has no `Project`
  reference and no lambdas. `KonanTarget` instances are the public singletons KGP tasks already serialize today.
* Reading values at the right time. All examples read the host into a local inside the task configuration block before `onlyIf` or
  `doLast`, because a task action that reaches `Task.project` or an extension at execution time breaks the configuration cache. The
  local must not be a top-level script `val`: that is a script object property, and capturing it makes the task unserializable. The
  integration test for the DSL runs with the configuration cache and `isolated-projects` on and caught this mistake. `onlyIf` uses the
  overload with a reason, so a skipped task explains itself in `--info` output and in build scans.
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
* API evolution. The KGP declarations are `@ExperimentalKotlinGradlePluginApi` for one release and carry `@since 2.5.20`. ABI dumps
  of the library and of `kotlin-gradle-plugin-api` guard the surface, and the legacy deprecation follows KT-A-1174. Users are not
  expected to implement the interface; `of()` is the only factory, which leaves us free to add members.

## Testing

Before this change only the unsupported host diagnostics had tests. Everything below comes with the implementation.

### Unit tests in the `native-host` library and in `kotlin-gradle-plugin/src/test`

`KotlinNativeHostTest` in the library is parameterised over `os.name` and `os.arch` and runs without Gradle:

| Input | Expected |
|---|---|
| `Mac OS X` / `aarch64` | `MACOS`, `ARM64`, supported, `macos_arm64`, `macos-aarch64` |
| `Mac OS X` / `x86_64` | `MACOS`, `X64`, not supported, `null`, `macos-x86_64` (the Intel Mac host was dropped in KT-84955) |
| `Linux` / `amd64` and `x86_64` | `LINUX`, `X64`, supported, `linux_x64`, `linux-x86_64` |
| `Linux` / `aarch64` | `LINUX`, `ARM64`, not supported, `null`, `linux-aarch64` (KT-86717) |
| `Linux` / `riscv64` | `LINUX`, `UNKNOWN`, not supported, `null`, `linux-riscv64` (KT-77977) |
| `Windows 11` / `amd64` | `WINDOWS`, `X64`, supported, `mingw_x64`, `windows-x86_64` |
| `Windows 11` / `arm64` | `WINDOWS`, `ARM64`, not supported, `null`, `windows-aarch64` |
| `Windows 11` / `riscv64` | `WINDOWS`, `UNKNOWN`, not supported, `null`, `windows-riscv64` |
| `FreeBSD` / `amd64` | `UNKNOWN`, `X64`, not supported, `null`, `freebsd-x86_64` (KT-74277, KT-75741) |
| `SunOS` / `aarch64` | `UNKNOWN`, `ARM64`, not supported, `null`, `sunos-aarch64` |

Also:

* `canLinkBinaries` for every Kotlin/Native target name on each supported host, and `false` for every target on an unsupported host.
* `KotlinNativeHostBridgeTest` in `kotlin-gradle-plugin/src/test` compares the library table with `HostManager().enabledByHost` for
  every supported host while that class is still on the classpath, and checks the `KonanTarget` extensions.
* `canRunBinaries`: the host target is true; `IOS_SIMULATOR_ARM64` is true only on `macos-aarch64`; `IOS_X64` is false there;
  `LINUX_X64` is false on macOS; everything is false on unsupported hosts.
* `of()` produces equal, serializable values, and `current()` equals `of(detected os, detected arch)` on every host in the table.
  Equality is defined on `os` and `architecture`; `name` is informational.
* No member throws for any input in the table.

### Functional tests in `kotlin-gradle-plugin/src/functionalTest`

These check configuration state only, following the KGP test layering.

* `HostNativeTargetTests`. On each simulated supported host, `hostNativeTarget()` creates the target the matching preset function would,
  under its canonical name, and runs `configure`. `hostNativeTarget("native")` creates it under that name. A second call returns the same
  instance. Together with `jvm()` and the default hierarchy template, the canonical name produces no
  `KotlinDefaultHierarchyFallbackIllegalTargetNames` warning and `"native"` produces the existing one. On `Linux/aarch64` it returns
  `null`, reports `HostNativeTargetUnavailable` once, and the project still evaluates with a `jvm()` target. `kotlin.nativeHost`
  equals `KotlinNativeHost.current()`.
* `KotlinNativeHostDslTests`. `isCompilableOnCurrentHost` is true for a linkable target and for a cross-compiled target without
  cinterops, and false once a cinterop is added, when klib cross-compilation is disabled, and on an unsupported host. Reading it before
  the compilations are finalised fails with the message described above.
* The existing `UnsupportedKotlinNativeHostTest`, `DisabledNativeCacheTest`, `CrossCompilationWithCinteropTests` and
  `KotlinNativeToolchainTest` move from `HostManager` to `KotlinNativeHost`, still simulating hosts through `os.name` and `os.arch`,
  and must keep passing. That is the evidence that the internal migration kept behaviour.
* The diagnostics that print the host, `NativeHostNotSupportedError` and `PublishingDisabledOnUnsupportedHost`, keep their text,
  because `name` has the same format as `platformName()`.

### Integration tests in `kotlin-gradle-plugin-integration-tests`

* A new `native-host-dsl` test project uses `hostNativeTarget("native")` (so the asserted task name is the same on every host),
  `nativeHost.isMacOs` to gate an Apple target, and a host captured in the task configuration block for an `onlyIf` on the cinterop
  tasks. We assert that `linkDebugExecutableNative` runs on all three CI hosts, that the `iosArm64` cinterop is skipped off macOS, and
  that the configuration cache is reused on the second run.
* The seven existing test projects that import `HostManager` (`nativeCompilerPluginTransitiveClassPath`, `cinterop-failing` and the
  others) switch to the new DSL.
* A Groovy smoke test reads `KotlinNativeHost.current().supported` and `nativeHost.macOs` and calls
  `kotlin.hostNativeTarget("native") { }` from `build.gradle`.
* For the deprecation in KT-74608: a build script that calls `HostManager.hostIsMac` compiles with a deprecation warning that names
  the replacement. The assertion checks the warning line only.

### Repository guards

* `CodeConformanceTest.testNoThrowingHostManagerCallsInBuildLogic`, from KT-86717, fails on `HostManager.host` and
  `HostManager.hostName` in the build scripts and build logic of this repository.
* `CodeConformanceTest.testNoHostManagerInKgpMainSources` fails on any `HostManager` reference in the main sources of
  `kotlin-gradle-plugin` and `kotlin-gradle-plugin-api`. Tests may still use it to compare the two tables.
* `:build-tools:libraries:native-host:checkKotlinAbi` and `:kotlin-gradle-plugin-api:apiCheck` guard the public surface.
* KT-74608 adds the `checkDeprecationsFor*` dumps of `kotlin-gradle-plugin-api`.

## Related work

* [KT-86717](https://youtrack.jetbrains.com/issue/KT-86717) fixed the eager `HostManager.host` calls in our own build scripts and added
  `hostKonanTargetAttribute()`, which still reads `HostManager.hostOrNull` and can move to `konanTarget` later.
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

**Q:** Is `isMacOs` true on an Intel Mac?

**A:** Yes. It states the operating system, and users gate Xcode or CocoaPods invocations on it. `HostManager.hostIsMac` was
false there since 2.5.0 dropped the Intel Mac host (KT-84955), and KGP's own Apple tooling tasks relied on that to stay
skipped. Inside KGP those gates use an internal `isMacOs && isSupported` check, so an Intel Mac still gets skipped tasks and the
unsupported host diagnostic instead of failing `xcodebuild` and SwiftPM runs on missing binaries.

**Q:** Why keep `KonanTarget` in the API?

**A:** Only in the `kotlin-gradle-plugin-api` bridge. It is already public there through `KotlinNativeTarget.konanTarget` and
`konanTargetAttribute`, and Gradle users need it for `when` matching and `Family` checks. The library itself uses target names, so it
has no dependency on `kotlin-native-utils` and other build tools can use it without pulling the compiler module in.

**Q:** Why copy the enablement table instead of delegating to `HostManager`?

**A:** So that KGP can drop the dependency later without a behaviour change, and so that Kotlin/Native can change `enabledByHost` for
compiler purposes without changing Gradle behaviour by accident. The comparison test keeps them aligned while both exist.

**Q:** Why is `supportedHosts()` a list of hosts and not of strings?

**A:** Strings would make the `name` format a contract. Typed values let KGP derive its diagnostics text and the "linkable
everywhere" target set without keeping a second copy of the host list.
