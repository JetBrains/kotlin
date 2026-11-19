# Design for `KotlinNativeHost` in KGP, the public replacement for `HostManager` in build scripts

## Glossary

* **`KotlinNativeHost`.** The description of the build host from the native-host Kotlin Build Tools library. Its design is in
  [kotlin-native-host.md](kotlin-native-host.md).
* **`HostManager`.** `org.jetbrains.kotlin.konan.target.HostManager` from `kotlin-native-utils`. An internal Kotlin/Native class that
  reaches every build script because `kotlin-gradle-plugin-api` has an `api` dependency on `kotlin-native-utils`.
* **Enabled target.** A `KonanTarget` the host can link final binaries for. Linux and Windows hosts can link only non-Apple targets.
* **Klib cross-compilation.** Compiling a `.klib` for a target the host cannot link. Enabled with
  `kotlin.native.enableKlibsCrossCompilation`.

## References

* [KT-39994](https://youtrack.jetbrains.com/issue/KT-39994) Multiplatform Gradle plugin: Public API for getting host info
* [KT-91830](https://youtrack.jetbrains.com/issue/KT-91830) Add a Kotlin Build Tools library describing the Kotlin/Native host
* [KT-52980](https://youtrack.jetbrains.com/issue/KT-52980) Gradle: Hostmanager: Add target to KonanTarget
* [KT-74608](https://youtrack.jetbrains.com/issue/KT-74608) Deprecate HostManager in KGP
* [KT-86717](https://youtrack.jetbrains.com/issue/KT-86717) `HostManager.host` throws at configuration time on linux-aarch64
* [KT-70247](https://youtrack.jetbrains.com/issue/KT-70247) Remove compiler modules from KGP runtime
* [KT-70251](https://youtrack.jetbrains.com/issue/KT-70251) Hide compiler symbols in KGP (the ASM deprecation transformer)
* [KT-84955](https://youtrack.jetbrains.com/issue/KT-84955) Deprecate Apple x86_64 targets as Error, which also dropped the Intel Mac host

---

## TL;DR

* `kotlin-gradle-plugin-api` depends on the native-host library, so build scripts see `KotlinNativeHost`, and adds a thin
  `KonanTarget` bridge for Gradle build logic that already works with `KonanTarget`.
* Build scripts get the host as `kotlin.nativeHost`, and declare the target for this machine with `kotlin.hostNativeTarget()`
  instead of the `when (HostManager.host)` block users copy between projects.
* `KotlinNativeTarget.isCompilableOnCurrentHost` answers the project-level question the library cannot: can this target be
  compiled here.
* KGP itself moves from `HostManager` to the library. After that KT-74608 deprecates `HostManager` for KGP consumers only, with the
  ASM transformer from KT-70251, so Kotlin/Native keeps the class as is.

## Problem

### What is the current state?

Every Gradle build script can see `HostManager` because `kotlin-gradle-plugin-api` declares `api(":native:kotlin-native-utils")`,
and because `KonanTarget` appears in public KGP signatures. Users and third-party plugins treat it as public API. Inside KGP, 34 main
source files import it. [kotlin-native-host.md](kotlin-native-host.md) describes the usage survey and the problems of `HostManager`.

### What is the problem?

* Build scripts have no supported way to ask about the host, so they keep using `HostManager`, which throws on hosts without a
  Kotlin/Native distribution and fails whole builds at configuration time (KT-86717).
* The most common pattern, declaring the native target for the current machine, is copied between projects and often wrong.
* We cannot deprecate `HostManager` in place. Kotlin/Native is its legitimate owner and user. KGP needs a deprecation that its
  consumers see and other modules do not.

### What is this proposal solving?

A public, stable way to use `KotlinNativeHost` from Gradle build scripts and plugins, a DSL shortcut for the host target, KGP itself
off `HostManager`, and a plan to deprecate `HostManager` for KGP consumers.

### Why is it important?

KT-39994 and KT-74608 are planned for 2.5.20. Every new host that appears in CI (linux-aarch64 runners are common now) produces
another wave of configuration-time failures, and today our only answer is "stop using `HostManager`" with nothing to point to instead.

## Anti-Goals

This proposal does not try to:

* Hide `KonanTarget`. It is already public through `KotlinNativeTarget.konanTarget` and `konanTargetAttribute`, and two thirds of the
  surveyed call sites need it. Removing it belongs to KT-70247.
* Change `HostManager` or `kotlin-native-utils`.

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

The proposal has four parts:

* `kotlin-gradle-plugin-api` depends on the native-host library and adds `KonanTarget`-typed extensions on top.
* Two ways to get the host: `KotlinNativeHost.current()` for plugins and projects without the multiplatform extension, and
  `KotlinMultiplatformExtension.nativeHost` for build scripts.
* `KotlinMultiplatformExtension.hostNativeTarget(...)`, which declares the target that matches the host.
* `KotlinNativeTarget.isCompilableOnCurrentHost`, for the question that depends on the project.

## Solution

### The `KonanTarget` bridge

```kotlin
// kotlin-gradle-plugin-api, package org.jetbrains.kotlin.gradle.dsl
@ExperimentalKotlinGradlePluginApi val KotlinNativeHost.konanTarget: KonanTarget?
@ExperimentalKotlinGradlePluginApi fun KotlinNativeHost.canLinkBinaries(target: KonanTarget): Boolean
@ExperimentalKotlinGradlePluginApi fun KotlinNativeHost.canRunBinaries(target: KonanTarget): Boolean
```

The library names targets by their Kotlin/Native names so that it does not depend on `kotlin-native-utils`. Gradle build logic
already works with `KonanTarget` (`KotlinNativeTarget.konanTarget`, `when` matching, `Family` checks), so the API module, which
already has `kotlin-native-utils`, maps names to `KonanTarget` and back. `konanTarget` is `null` on unsupported hosts, like
`targetName`.

### `KotlinMultiplatformExtension.nativeHost`

```kotlin
val KotlinMultiplatformExtension.nativeHost: KotlinNativeHost
```

The same value as `KotlinNativeHost.current()`, exposed where about 60% of the surveyed usages live: inside `kotlin { }`.

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

The `name` parameter is the Gradle target name, the same parameter as in `macosArm64(name)`, and it drives the names of the derived
source sets, compilations and tasks (`<name>Main`, `compileKotlin<Name>`, `linkDebugExecutable<Name>`).

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

### KGP internals

KGP main sources stop using `HostManager`: `isMacOs` and `isWindows` for OS checks, `isSupported` for host support, `name` for the
distribution classifier and diagnostics, `canLinkBinaries` for target enablement and `canRunBinaries` for simulator tests.

`HostManager.hostIsMac` was false on an Intel Mac since 2.5.0 dropped that host (KT-84955), and KGP's Apple tooling tasks relied on
that to stay skipped. `isMacOs` is true there, so those gates use an internal `isMacOs && isSupported` check. An Intel Mac still gets
skipped tasks and the unsupported host diagnostic instead of failing `xcodebuild` and SwiftPM runs on missing binaries.

### Migration from `HostManager` in build scripts

The [library doc](kotlin-native-host.md#migration-from-hostmanager) maps each `HostManager` member to `KotlinNativeHost`. With the
bridge, build scripts that work with `KonanTarget` have two more options:

| Legacy API | Replacement |
|---|---|
| `host`, `hostOrNull` | `konanTarget`, which is `null` on unsupported hosts |
| `host.family`, `host.architecture` | `konanTarget?.family`, `architecture` |
| `HostManager().isEnabled(target)` | `canLinkBinaries(target)` |
| `HostManager().targets`, `.targetValues` | `KonanTarget.predefinedTargets`, already public |
| `when (HostManager.host) { ... }` that declares the host target | `hostNativeTarget()` |

### Deprecating `HostManager` for KGP consumers only

`HostManager` stays untouched for Kotlin/Native. The mechanism for a consumer-side deprecation already exists: the
`asm-deprecating-transformer` convention from KT-70251 adds `@kotlin.Deprecated` with a message to classes bundled into a jar, and a
checked `.deprecations` dump guards the set. It only works on `embedded` artifacts, and `kotlin-native-utils` used to be a published
`api` dependency of `kotlin-gradle-plugin-api`. So `kotlin-gradle-plugin-api` now bundles `kotlin-native-utils` without relocation, as
it already bundles `kotlin-gradle-compiler-types`, keeps `kotlin-util-io` (the module's own dependency) as a regular dependency, and
deprecates `HostManager` and its companion with a message that points to `KotlinNativeHost`. `KonanTarget`, `Family` and the rest of
the module stay undeprecated because they are public API. So does `TargetSupportException`: only `HostManager` throws it, and a
second warning on every `catch` would not help anyone migrate.

Dropping the `api` dependency is not enough on its own. `kotlin-klib-commonizer-api`, a runtime dependency of KGP, also depends on
`kotlin-native-utils`, and its plain copy would keep an undeprecated `HostManager` on every buildscript classpath. KGP excludes it
there, so the bundled copy is the only one. Projects in this repository compile against the classes of `kotlin-gradle-plugin-api`,
which do not contain bundled jars, so the ones that use `KonanTarget` declare `compileOnly` on `kotlin-native-utils`, as they already
do for `kotlin-gradle-compiler-types`.

The BOM keeps its `kotlin-native-utils` entry. A build that still adds the module itself, directly or through another plugin, then gets
the same version as the bundled copy, so the duplicate classes are identical. An integration test puts a plain `kotlin-native-utils` on
the buildscript classpath next to KGP and checks that the build still links. Removing the classes from the KGP API altogether stays
with KT-70247, and the deprecation needs agreement with its owners.

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

* Configuration cache inputs. `KotlinNativeHost.current()` reads only `os.name` and `os.arch`. The user guide says direct system
  property reads at configuration time are recorded as configuration inputs, so a `ValueSource` is not needed. We keep `ValueSource`
  for cases like the Xcode device lookup, where several sources or an external process are involved.
* Task state. `KotlinNativeHost` is `Serializable` and has no `Project` reference and no lambdas. `KonanTarget` instances are the
  public singletons KGP tasks already serialize today.
* Reading values at the right time. All examples read the host into a local inside the task configuration block before `onlyIf` or
  `doLast`, because a task action that reaches `Task.project` or an extension at execution time breaks the configuration cache. The
  local must not be a top-level script `val`: that is a script object property, and capturing it makes the task unserializable. The
  integration test for the DSL runs with the configuration cache and `isolated-projects` on and caught this mistake. `onlyIf` uses the
  overload with a reason, so a skipped task explains itself in `--info` output and in build scans.
* Task inputs. A task whose output depends on the host declares it with `inputs.property("kotlin.native.host", host.name)`. KGP's native
  tasks already key on `konanTarget`; the documentation page will show the pattern for user tasks.
* Lazy where the value is lazy. `isCompilableOnCurrentHost` is a `Provider<Boolean>` because it depends on finalised compilations. Host
  facts are plain values because they are constants for the build JVM, and wrapping constants in providers only adds indirection.
* Isolated projects. There is no cross-project access, no `rootProject`, and no shared mutable service. `nativeHost` is a read-only view
  over process-level constants, and `hostNativeTarget` only calls the project's own preset functions.
* Groovy and Java. `hostNativeTarget` has `Action<T>` overloads, and the library's boolean getters follow bean naming, so Groovy reads
  `nativeHost.macOs` and `host.supported`.
* Managed types. We do not use them for `KotlinNativeHost` because it is a value, not configurable state, and `objects.newInstance` would
  require a `Project`. The extension property is a `val`, so build scripts cannot reassign it.
* Diagnostics. Unsupported hosts are reported through `KotlinToolingDiagnostics`, never with `println` or with an exception at
  configuration time.
* API evolution. The KGP declarations are `@ExperimentalKotlinGradlePluginApi` for one release and carry `@since 2.5.20`. The ABI dump
  of `kotlin-gradle-plugin-api` guards the surface, and the legacy deprecation follows KT-A-1174.

## Testing

Before this change only the unsupported host diagnostics had tests. Everything below comes with the implementation. The library has
its own tests, described in [kotlin-native-host.md](kotlin-native-host.md#testing).

### Unit tests in `kotlin-gradle-plugin/src/test`

* `KotlinNativeHostBridgeTest` compares the library's table of linkable targets with `HostManager().enabledByHost` for every
  supported host while that class is still on the classpath, and checks the `KonanTarget` extensions: `konanTarget` maps each
  supported host to its target and is `null` on linux-aarch64, and `canRunBinaries` is true for `MACOS_ARM64` and
  `IOS_SIMULATOR_ARM64` on an Apple Silicon Mac and false for `IOS_X64` and `LINUX_X64` there.

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
* A build script that calls `HostManager.hostIsMac` still gets the right value and compiles with a deprecation warning that names
  the replacement. The assertion checks the message of the warning lines only.
* A build script with a plain `kotlin-native-utils` on its buildscript classpath, next to the bundled copy, still links the host
  target.

### Repository guards

* `CodeConformanceTest.testNoThrowingHostManagerCallsInBuildLogic`, from KT-86717, fails on `HostManager.host` and
  `HostManager.hostName` in the build scripts and build logic of this repository.
* `CodeConformanceTest.testNoHostManagerInKgpMainSources` fails on any `HostManager` reference in the main sources of
  `kotlin-gradle-plugin` and `kotlin-gradle-plugin-api`. Tests may still use it to compare the two tables.
* `:kotlin-gradle-plugin-api:apiCheck` guards the public surface.
* `checkDeprecationsForJar` and `checkDeprecationsForGradle96Jar` in `kotlin-gradle-plugin-api` fail when the set of deprecated
  packages in its jars changes, so a pattern that starts deprecating more than `HostManager` cannot slip in.

## Related work

* [KT-91830](https://youtrack.jetbrains.com/issue/KT-91830) adds the native-host library this design builds on.
* [KT-86717](https://youtrack.jetbrains.com/issue/KT-86717) fixed the eager `HostManager.host` calls in our own build scripts and added
  `hostKonanTargetAttribute()`, which still reads `HostManager.hostOrNull` and can move to `konanTarget` later.

# Prior art

_What would be the alternatives to the proposed solution? What would happen if we don't solve the problem? Why should this proposal be
preferred?_

**Possible alternatives:**

* Point users at Gradle's `org.gradle.nativeplatform.platform.OperatingSystem`, with a few extension functions in
  `kotlin-gradle-plugin-api`. This was the first suggestion in the team discussion. It belongs to the Gradle native plugins, which Gradle
  no longer develops, it has no notion of Kotlin/Native targets or of which targets a host can link, and it would cover about a third of
  the surveyed call sites. We would still need everything in this proposal for the rest.
* Only deprecate, with no replacement. Users would keep the `when` block and the `os.name` parsing, and we would keep answering the same
  issues.

If we do nothing, each new CI host produces another KT-86717.

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

**A:** Only in the `kotlin-gradle-plugin-api` bridge. It is already public there through `KotlinNativeTarget.konanTarget` and
`konanTargetAttribute`, and Gradle users need it for `when` matching and `Family` checks. The library itself uses target names, so it
has no dependency on `kotlin-native-utils` and other build tools can use it without pulling the compiler module in.
