# Design for `KotlinNativeHost`, a build tool independent description of the Kotlin/Native host

## Glossary

* **Host.** The machine the build runs on, described by its operating system and CPU architecture.
* **Supported host.** A host for which a Kotlin/Native distribution exists: `macos-aarch64`, `linux-x86_64` and `windows-x86_64`.
  See [Kotlin/Native target support](https://kotlinlang.org/docs/native-target-support.html#hosts).
* **Host target.** The Kotlin/Native target whose binaries run on the host itself, for example `macos_arm64` on an Apple Silicon Mac.
* **Target name.** The Kotlin/Native name of a target, as used in `konan.properties` and in the `konanTargetAttribute` of KGP:
  `macos_arm64`, `linux_x64`, `mingw_x64` and so on.
* **`HostManager`.** `org.jetbrains.kotlin.konan.target.HostManager` from `kotlin-native-utils`, an internal Kotlin/Native class
  that build tools and build scripts use today to answer questions about the host.

## References

* [KT-91830](https://youtrack.jetbrains.com/issue/KT-91830) Add a Kotlin Build Tools library describing the Kotlin/Native host
* [KT-39994](https://youtrack.jetbrains.com/issue/KT-39994) Multiplatform Gradle plugin: Public API for getting host info
* [KT-86717](https://youtrack.jetbrains.com/issue/KT-86717) `HostManager.host` throws at configuration time on linux-aarch64
* [KT-64512](https://youtrack.jetbrains.com/issue/KT-64512) Rework `HostManager.enabled`
* [KT-66699](https://youtrack.jetbrains.com/issue/KT-66699), [KT-27114](https://youtrack.jetbrains.com/issue/KT-27114) `HostManager` ABI breaks
  that hit Gradle plugins
* [KT-70247](https://youtrack.jetbrains.com/issue/KT-70247) Remove compiler modules from KGP runtime
* [KT-84955](https://youtrack.jetbrains.com/issue/KT-84955) Deprecate Apple x86_64 targets as Error, which also dropped the Intel Mac host

---

## TL;DR

* We add `KotlinNativeHost`, a small immutable value that describes the build host as Kotlin/Native sees it, in a new Kotlin Build
  Tools library: `build-tools/libraries/native-host`, artifact `org.jetbrains.kotlin:build-tools-native-host`.
* It answers the questions build tools ask `HostManager` today: which OS, which architecture, whether Kotlin/Native supports the
  host, which target is the host target, which targets the host can link or run, and what the distribution classifier is.
* It never throws, depends only on the stdlib and names targets by their Kotlin/Native names, so KGP, the Maven plugin, JPS,
  kotlin-toolchain and Bazel rules can all use it without `kotlin-native-utils`.
* Every member is backed by a use case we found in KGP sources or in a GitHub survey of about 500 user build scripts and plugins.
* Using the library in KGP, with a public DSL for build scripts, is a separate change (KT-39994).

## Problem

### What is the current state?

Build tools that work with Kotlin/Native need to know the host. KGP asks `HostManager`, an internal Kotlin/Native class in
`kotlin-native-utils`: its main sources call it in 61 places across 34 files. Build scripts and third-party plugins use it too,
because `kotlin-gradle-plugin-api` exposes `kotlin-native-utils` as an `api` dependency. A GitHub code search finds about 430 Kotlin
build scripts, 70 Groovy scripts and 770 plugin or `buildSrc` sources that reference it, among them ktor, sqldelight, androidx,
aws-crt-kotlin, smithy-kotlin, kotlinx-io, kotlinx-benchmark, KSP, binary-compatibility-validator, SKIE, sentry-kmp, multipaz and
intellij-community.

### What is the problem?

* It throws. `host`, `hostName`, `hostOs()`, `hostArch()` and `HostManager().isEnabled()` throw `TargetSupportException` on any host
  that has no Kotlin/Native distribution: linux-aarch64, FreeBSD, Windows on ARM. In Gradle, one eager call in any subproject fails
  the whole build, even for JVM-only tasks (KT-86717). Users work around it with reflection (CompleteKotlin), with
  `try/catch TargetSupportException` (binary-compatibility-validator) or by parsing `os.name` themselves (boringssl-kmp,
  filament-kmp).
* Its answers do not match the questions. `isEnabled` is one boolean that means "can link binaries", so users rebuild "can run
  tests" and "can cross-link mingw from macOS" by hand, and often get it wrong (KT-64512).
* Its ABI is not stable. Constructor changes broke published plugins twice (KT-27114, KT-66699), and the Kotlin/Native team wants
  to keep the freedom to change it.
* It is tied to the compiler. `kotlin-native-utils` is a compiler module, which KT-70247 removes from the KGP runtime. Other build
  tools would have to pull it in only to learn the host.

### What is this proposal solving?

A stable, documented description of the host that every build tool integration can use instead of `HostManager`.

### Why is it important?

KT-91830 and KT-39994 are planned for 2.5.20. Every new host that appears in CI (linux-aarch64 runners are common now) produces
another wave of failures, and today our only answer is "stop using `HostManager`" with nothing to point to instead.

## Anti-Goals

This proposal does not try to:

* Be a general OS detection library. Uses unrelated to Kotlin/Native that we saw in the wild (Compose Desktop package formats, JNI
  library paths, `where` versus `command -v`) are served well enough by `System.getProperty("os.name")`.
* Provision toolchains. Downloading `kotlin-native-prebuilt` stays with the build tools, and the toolchain work in KT-70247 owns its
  future.
* Change `HostManager` or `kotlin-native-utils`. Kotlin/Native keeps its class as is.
* Answer project-level questions. Whether a klib can be compiled for a target depends on project settings and cinterops, so it
  belongs to the build tool, not to a description of the host.

## Solution (overview)

```kotlin
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost

val host = KotlinNativeHost.current()

if (host.isMacOs) {
    // Run Xcode tooling
}

when (host.targetName) {
    "macos_arm64" -> { /* ... */ }
    null -> logger.warn("Kotlin/Native does not support ${host.name}")
}

host.canLinkBinaries("ios_arm64")   // true only on a Mac
host.canRunBinaries("linux_x64")    // true only on Linux x86_64
```

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

Three rules apply to every member. Nothing throws: an unknown host is a value (`UNKNOWN`, `null`, `false`), which is what KT-86717
asks for. The object is a plain value computed from `os.name` and `os.arch`, so a build tool can keep it in task state or a
cache. And the library depends only on the stdlib, so targets are Kotlin/Native target names. A build tool that already has
`kotlin-native-utils` can map them to `KonanTarget` itself; KGP does that in `kotlin-gradle-plugin-api` (KT-39994).

The host question does not belong to any one build tool. The Kotlin Gradle plugin, the Maven plugin, JPS, kotlin-toolchain and
Bazel rules all need the same answer, and `build-tools/libraries` exists for that kind of code. Keeping the type free of
`kotlin-native-utils` also means it survives KT-70247.

#### `os`

Replaces `hostIsMac`, `hostIsLinux`, `hostIsMingw`, `hostOs()`, `simpleOsName()` and `host.family`.

OS checks are the most common use. `hostIsMac` alone accounts for 23 of the 61 call sites in KGP and is the top member in the
GitHub survey. Users declare Apple targets only on macOS (algolia, multipaz, kotlinxtras), skip Swift and Xcode tasks elsewhere
(spm4Kmp, kotlin-docc, moko-mvvm, and in KGP `FatFrameworkTask`, CocoaPods and Swift Export), skip `cinterop` on Windows
(smithy-kotlin), and choose `.zip` or `.tar.gz` for the distribution (`NativeCompilerDownloader`).

An enum allows exhaustive `when` and avoids the `"osx"` versus `"macos"` confusion of `hostOs()`: xmlutil compares `hostOs()` with
`"macos"`, which never matches. `UNKNOWN` covers FreeBSD and anything else, and the three convenience getters are false there.
`isMacOs` describes the operating system only, so it is true on an Intel Mac even though Kotlin/Native no longer supports that host
(KT-84955). A build tool that needs a working Kotlin/Native toolchain on macOS checks `isMacOs && isSupported`.

#### `architecture`

Replaces `hostArch()`, `hostArchOrNull()` and `host.architecture`.

Build tools need it to pick the right Apple or Linux target on the host (`macosArm64` or `macosX64`, `linuxArm64` or `linuxX64`)
and to decide whether a simulator test can run on this machine (`KotlinNativeTestRunFactories` in KGP, and the test run line marker
in intellij-community). xmlutil's `ideaPreset(host)`, KGP's `DisabledNativeCacheTest` and cklib's `hostArch` do the same with
strings today. `UNKNOWN` covers riscv64, x86 and others; KT-77977 was a crash on that path.

#### `isSupported`

Replaces `hostIsSupported`, `hostOrNull != null` and `try { host } catch (TargetSupportException)`.

"Is there a Kotlin/Native distribution for this machine" is asked eight times inside KGP (publishing, commonizer, platform
dependencies, target creation, checkers) and by users before they touch anything native (KSP, binary-compatibility-validator,
CrossPaste, convention plugins that skip native targets on linux-aarch64 CI).

#### `targetName`

Replaces `host`, `hostOrNull`, `hostName`, `host.name`, `host.platformName` and `host.fullArchName`.

"Which target runs on this machine" is the second most common question. The answer is the Kotlin/Native target name
(`macos_arm64`), which is what `konanTargetAttribute` values and `konan.properties` keys use. It is `null` on unsupported hosts,
which is what `hostOrNull` returns today; there is no throwing variant. Users need it to declare the host target
(hfhbd, kotlinxtras, parsus, lemcoder, kotlinx-io), to resolve host klibs through `konanTargetAttribute` (KMP-NativeCoroutines,
atomicfu, serialization) and for cross-compilation checks (cryptography-kotlin, the intellij run configuration producer). KGP uses it
for `cacheableTargets` property keys in `KonanPropertiesBuildService` and to find the current host's target preset.

#### `name`

Replaces `platformName()`, the `simpleOsName()` plus `hostArch()` concatenations, and `hostName` where it is only displayed.

A stable host id in the form `macos-aarch64`, `linux-x86_64`, `windows-x86_64`. Each part falls back on its own to the lowercased
system property, so FreeBSD on amd64 is `freebsd-x86_64` and Windows on riscv64 is `windows-riscv64`. `platformName()` gives the same
strings today through `runCatching`, so messages that print the host keep their text. KGP prints it in its unsupported host
diagnostics, and the `kotlin-native-prebuilt-<version>-<name>` classifier uses it (`NativeCompilerDownloader`, SKIE,
intellij-community, and CompleteKotlin, which currently gets it through reflection).

#### `canLinkBinaries(targetName)`

Replaces `HostManager().isEnabled(target)`, `HostManager().enabled` and `enabledByHost`.

This is the one piece of real state in `HostManager`, the host-to-targets table, and the question users most often rebuild by hand
(aws-crt-kotlin, CrossPaste). The name says what is answered: final linking, not klib compilation. It returns `false` on unsupported
hosts and for unknown target names. Call sites: `cinterop` and CMake gates (aws-crt-kotlin, kotlin-sqlite-bindings, powersync),
per-target ABI dumps in binary-compatibility-validator, KSP's host check, and in KGP `PlatformLibrariesGenerator`,
`CinteropCrossCompilationChecker`, FUS metrics and link task enablement.

The table starts as a copy of `HostManager.enabledByHost` and lives in the library, so Kotlin/Native can change its own table for
compiler purposes without changing build tool behaviour. While `kotlin-native-utils` is still on the KGP classpath, a KGP test
checks that both tables agree (KT-39994).

#### `canRunBinaries(targetName)`

Replaces `target == HostManager.host` for host targets and `hostIsMac && host.architecture == target.architecture` for simulator
targets (`KotlinNativeTestRunFactories` in KGP).

"Can the test executable run here" is a different question from "can it be linked here". Users currently disable `linuxX64Test`
and `linkDebugTestLinuxX64` by comparing OS booleans (sqldelight, SQLiter, FileKit, kzmq). One method holds the rules: the host
target itself, and on macOS the simulator targets with the host's architecture. intellij-community uses the same logic for run line
markers.

#### `current()`

Replaces the `HostManager` companion and `HostManager()` instantiation.

`current()` is a pure function of `os.name` and `os.arch` with no static cache. Reading two system properties is cheap, Gradle
records the reads as configuration cache inputs, and a companion without state matters because the Gradle daemon reuses plugin
classloaders between builds and because tests change system properties to simulate hosts.

#### `of(os, architecture)`

New. Plugin authors today thread a `hostIsMac: Boolean = HostManager.hostIsMac` parameter through their code to test both branches
(sentry-kmp, SKIE's `KgpShim`). A public factory lets them build any host in tests without changing system properties.

#### `supportedHosts()`

Replaces the internal `HostManager.supportedHosts` extension and `enabledByHost.keys`.

The typed list of hosts Kotlin/Native ships a distribution for, kept in one place. KGP derives the "supported hosts are ..." line
of its diagnostics and the set of targets linkable on every host from it. It is a list of `KotlinNativeHost` values, not of
strings, so the `name` format does not become a contract.

#### What is not included

`targetManager()`, `resolveAlias()`, `listAliases()`, `jniHostPlatformIncludeDir`, `defaultJvmArgs`. These serve the compiler CLI.
We found no build tool usage.

### Migration from `HostManager`

| Legacy API | Replacement | Notes |
|---|---|---|
| `hostIsMac`, `hostIsLinux`, `hostIsMingw` | `isMacOs`, `isLinux`, `isWindows`, or `os` | `isMacOs` is also true on an Intel Mac |
| `hostIsSupported`, `hostOrNull != null` | `isSupported` | |
| `host`, `hostOrNull` | `targetName` | callers must handle `null` |
| `hostName`, `host.name` | `targetName` | |
| `hostOs()`, `simpleOsName()` | `os` | enum instead of `"osx"` strings |
| `hostArch()`, `hostArchOrNull()` | `architecture` | |
| `platformName()` | `name` | same format |
| `HostManager().isEnabled(t)`, `.enabled`, `.enabledByHost` | `canLinkBinaries(t)` | |
| `HostManager.supportedHosts`, `enabledByHost.keys` | `supportedHosts()` | |

### Design properties

* Values, not services. `KotlinNativeHost` is `Serializable`, holds two enums and the two raw system property values, and has no
  references to build tool objects, so a Gradle task can capture it and the configuration cache can store it.
* No static state. `current()` reads the system properties on every call.
* Java and Groovy friendly. `current()`, `of()` and `supportedHosts()` are `@JvmStatic`, and the boolean getters follow bean naming,
  so Groovy reads `host.macOs` and `host.supported`.
* Room to grow. Users are not expected to implement the interface; `of()` and `current()` are the only factories, which leaves us
  free to add members. The library needs no opt-in, and its ABI dump guards the public surface.

### Rollout

* 2.5.20: the library ships as `org.jetbrains.kotlin:build-tools-native-host`. KGP starts using it and exposes it to build scripts
  in the same release (KT-39994).
* Later: other build tool integrations move their host checks to the library.

## Testing

`KotlinNativeHostTest` in the library is parameterised over `os.name` and `os.arch` and runs without any build tool:

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

It also checks that:

* `supportedHosts()` lists exactly the hosts with a target, and `isSupported` agrees with it for every OS and architecture.
* Linux and Windows hosts link every non-Apple target, a Mac links every target, and unsupported hosts link nothing.
* `canRunBinaries` is true for the host target and, on a Mac, for the simulator targets with the host's architecture, and false
  everywhere on unsupported hosts.
* `of()` produces equal values that survive Java serialization, and `current()` equals `of(detected os, detected arch)`. Equality is
  defined on `os` and `architecture`; `name` is informational.
* No member throws for any input in the table.

`:build-tools:libraries:native-host:checkKotlinAbi` guards the public surface.

## Related work

* [KT-39994](https://youtrack.jetbrains.com/issue/KT-39994) moves KGP from `HostManager` to this library and adds a build script DSL
  on top of it.
* [kotlin-native-toolchain](https://github.com/adamko-dev/kotlin-native-toolchain) by Adam Semenenko provisions distributions outside
  KGP and generates dependency data. It does not model the host, so the two complement each other. Its `knt-dependency-data` could
  later replace the classifier logic in `NativeCompilerDownloader` that `name` feeds.

# Prior art

_What would be the alternatives to the proposed solution? What would happen if we don't solve the problem? Why should this proposal be
preferred?_

**Possible alternatives:**

* Gradle's `org.gradle.nativeplatform.platform.OperatingSystem`. It belongs to the Gradle native plugins, which Gradle no longer
  develops, it has no notion of Kotlin/Native targets or of which targets a host can link, it covers about a third of the surveyed
  call sites, and it does not exist outside Gradle.
* Fix `HostManager` itself: add the missing hosts, stop throwing. Kotlin/Native does not want the class to become a public contract,
  and every change to it has to be checked against all existing usages. It also does not solve the ABI problem, and the class stays in
  a compiler module.
* Adopt `knt-dependency-data` from kotlin-native-toolchain. It solves a different problem (provisioning and dependency metadata) and
  does not describe the host.

If we do nothing, each new CI host produces another KT-86717, and each `HostManager` change another KT-66699.

## FAQ

_Answers to questions asked while discussing this proposal that do not belong in the proposal text itself._

**Q:** Why target names and not `KonanTarget`?

**A:** `KonanTarget` lives in `kotlin-native-utils`, a compiler module. Target names keep the library free of it, so any build tool
can use the library without pulling the compiler in. Build tools that already have `KonanTarget` can map names to it; KGP adds such
extensions in `kotlin-gradle-plugin-api`.

**Q:** Why copy the table of linkable targets instead of delegating to `HostManager`?

**A:** So that build tools can drop `kotlin-native-utils` without a behaviour change, and so that Kotlin/Native can change
`enabledByHost` for compiler purposes without changing build tool behaviour by accident. A KGP test keeps the two aligned while both
exist.

**Q:** Why is `supportedHosts()` a list of hosts and not of strings?

**A:** Strings would make the `name` format a contract. Typed values let callers derive their own text and the "linkable everywhere"
target set without a second copy of the host list.

**Q:** Why is there no cache in `current()`?

**A:** Reading two system properties costs nothing, and a cache would outlive a build in the Gradle daemon and get in the way of tests
that simulate hosts.
