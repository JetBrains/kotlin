# Design for `build-tools-apple`, a library for running Apple developer tools

## Glossary

* **Developer dir.** The `Contents/Developer` directory of an Xcode bundle, or `/Library/Developer/CommandLineTools` for a
  Command Line Tools install. `xcode-select -p` prints it, `/var/db/xcode_select_link` points to it, and `DEVELOPER_DIR` overrides it.
* **Selected Xcode.** The developer dir that `/usr/bin` shims such as `xcrun`, `xcodebuild` and `swift` use when nothing overrides it:
  `DEVELOPER_DIR` if set, otherwise the `xcode-select` choice.
* **Toolchain tool.** A tool shipped inside the developer dir and reached through `xcrun`: `xcodebuild`, `lipo`, `libtool`,
  `install_name_tool`, `swift`, `clang`, `simctl`.
* **OS tool.** A tool that ships with macOS and does not depend on the selected Xcode: `codesign`, `plutil`, `PlistBuddy`, `xattr`.
* **Command Line Tools (CLT).** Apple's standalone toolchain without Xcode.app. It has `lipo`, `libtool`, `clang` and `swift`, but no
  `xcodebuild` or `simctl`.
* **xcodes.** A popular CLI that installs Xcode versions side by side as `/Applications/Xcode-<version>.app`, or into
  `XCODES_DIRECTORY`.
* **Invocation.** One fully resolved command: executable, arguments, working directory, environment changes, standard input and how
  to treat its output.

## References

* [Kotlin Build Tools libraries](../../build-tools/libraries/README.md) and their [rules](../../build-tools/libraries/AGENTS.md)
* Bazel [`xcode_locator.m`](https://github.com/bazelbuild/bazel/blob/master/tools/osx/xcode_locator.m) and
  [`xcode_configure.bzl`](https://bazel.googlesource.com/bazel/+/HEAD/tools/osx/xcode_configure.bzl)
* [xcodes](https://github.com/XcodesOrg/xcodes), [xcenv](https://github.com/xcenv/xcenv), [chxcode](https://github.com/klaaspieter/chxcode)
* [`xcode-select(1)`](https://keith.github.io/xcode-man-pages/xcode-select.1.html),
  [Configuring command line tools](https://developer.apple.com/documentation/xcode/configuring-command-line-tools-settings)

---

## TL;DR

* `build-tools/libraries/apple` (artifact `org.jetbrains.kotlin:build-tools-apple`, package `org.jetbrains.kotlin.buildtools.apple`)
  finds the Xcode to use and runs Apple developer tools against it. It depends on the stdlib and the JDK only.
* `XcodeLocator` handles side-by-side installs made by xcodes or by hand, symlinked and broken `Xcode.app` entries, Command Line Tools,
  `DEVELOPER_DIR` and version requests such as `16.4` or a `.xcode-version` file. Spotlight lookup is opt-in.
* `AppleTools` binds the tools to one installation. Every invocation sets `DEVELOPER_DIR` to it. Toolchain tools run through
  `/usr/bin/xcrun`, OS tools by absolute path, so nothing resolves through `PATH`.
* Each tool has a pure method that returns the command line and a method that runs it through a pluggable `AppleToolRunner`.
* Tests live in the library. Most run on any host against fakes and fixture bundles; one class runs the real tools on macOS.

## Problem

Building for Apple platforms means starting a dozen command line tools, and each one raises the same questions.

* Which Xcode? A machine often has several. A tool started by bare name resolves through `PATH`, where a `swift` from swiftly or
  Homebrew can replace the one from Xcode. A tool started through a `/usr/bin` shim follows `DEVELOPER_DIR` and `xcode-select`, which
  can change between two commands of one build.
* Where is it? xcodes installs `Xcode-<version>.app`, CI images keep copies under arbitrary names, and `/Applications/Xcode.app` may be
  a symlink to a version that was removed. There is no API on the JVM that lists installations or reads their versions.
* What leaks in from the environment? When a build runs from an Xcode build phase, Xcode exports its build settings as variables.
  `SDKROOT`, `OTHER_LDFLAGS` and others change what a nested `xcodebuild` or `swift` does.
* What happened when it failed? A raw non-zero exit code says little. Some failures have a known cause and a known fix.
* Is the command line right? Argument order, optional flags and the quoting rules of `PlistBuddy` are easy to get wrong and cannot be
  checked without running the tool, unless building the command is separate from running it.

The library answers these once, in code that has unit tests and no dependency on any build tool.

## Anti-goals

The library does not:

* Install, download or switch Xcode. It reads what is on the machine and never changes the system selection.
* Parse JSON. `simctl`, `swift package describe` and `plutil -convert json` return text. JSON support would need
  kotlinx-serialization and with it an API/implementation split.
* Wrap CocoaPods. `pod` is a Ruby gem, not an Apple tool.
* Manage simulators. `simctl` support is limited to listing devices and runtimes and building a `spawn` command.
* Decide policy. Where a version request comes from and how output is logged are up to the caller.

## Overview

```kotlin
val runner = ProcessAppleToolRunner()

// Which Xcode: an explicit request, or the one DEVELOPER_DIR / xcode-select points to.
val locator = XcodeLocator(runner, environment = System.getenv())
val xcode: XcodeInstallation = locator.resolve(XcodeRequest.Version("16.4"))

val tools = AppleTools(xcode, runner)
tools.lipo.create(inputs = listOf(arm64Lib, x64Lib), output = fatLib)
tools.xcodebuild.createXCFramework(
    slices = listOf(XCFrameworkSlice.Library(iosLib), XCFrameworkSlice.Library(simulatorLib)),
    output = xcframework,
)

// Callers that start the process themselves only need the command line.
val spawn: AppleToolInvocation = tools.simctl.spawnInvocation(device = deviceId, executable = testBinary, arguments = testArgs)
```

The library has three parts:

* `XcodeLocator` finds Xcode installations and resolves an `XcodeRequest` to one `XcodeInstallation`.
* `AppleToolInvocation`, `AppleToolRunner` and `AppleToolException` describe a command, run it and report failures in one format.
* One class per tool (`Xcodebuild`, `Lipo`, `Simctl`, ...), reached through `AppleTools`, which binds them to an installation and a runner.

## Finding Xcode

### What machines look like

A developer machine with xcodes installed, taken from a real setup:

```
/Applications/Xcode-27.2.0.app                              installed by xcodes
/Applications/Xcode.app -> /Applications/Xcode-26.3.0.app   symlink left over after 26.3 was removed
/var/db/xcode_select_link -> /Applications/Xcode-27.2.0.app/Contents/Developer
Xcode-27.2.0.app/Contents/Info.plist      binary plist
Xcode-27.2.0.app/Contents/version.plist   XML: CFBundleShortVersionString = 27.2, ProductBuildVersion = 27B52
```

Hardcoding `/Applications/Xcode.app` finds a broken symlink here. `Info.plist` is binary, so reading it needs `plutil` or `PlistBuddy`.
`version.plist` is XML and holds both the marketing version and the build number, so the JDK XML parser reads it without starting a
process, and tests can build fixture bundles on Linux.

### Prior art

Bazel's `xcode-locator` asks Launch Services for every bundle with the identifier `com.apple.dt.Xcode`. It reads
`CFBundleShortVersionString` (padded to three components) and `ProductBuildVersion`, and registers shorter versions as aliases, so `16`
and `16.4` both resolve. When two bundles claim the same version, the one under `/Applications/` wins. It ignores `DEVELOPER_DIR` and
Command Line Tools.

xcodes, xcenv and chxcode agree on precedence: `DEVELOPER_DIR` first, then a `.xcode-version` file, then the `xcode-select` choice.
`DEVELOPER_DIR` may name the `.app` or its `Contents/Developer`; the `/usr/bin` shims accept both.

The JVM has no Launch Services API. `mdfind "kMDItemCFBundleIdentifier == 'com.apple.dt.Xcode'"` reads the same Spotlight index, but
Spotlight is often disabled on CI images and adds a process launch to every scan.

### `XcodeLocator`

```kotlin
public class XcodeLocator(
    runner: AppleToolRunner,                 // runs xcode-select and mdfind
    environment: Map<String, String>,        // read for DEVELOPER_DIR, HOME and XCODES_DIRECTORY
    searchDirectories: List<Path> = defaultSearchDirectories(environment),
    useSpotlight: Boolean = false,
) {
    public fun resolve(request: XcodeRequest = XcodeRequest.Selected): XcodeInstallation
    public fun installations(): List<XcodeInstallation>

    public companion object {
        public fun installationAt(path: Path): XcodeInstallation?
        public fun defaultSearchDirectories(environment: Map<String, String>): List<Path>
    }
}
```

The locator caches nothing. Resolve once per build and pass the `XcodeInstallation` on: that keeps one build on one Xcode and lets a
long-lived process notice a new selection in the next build.

#### Resolution order

`resolve(request)` takes the first rule that applies:

1. `XcodeRequest.DeveloperDir(path)`. An explicit path, either the `.app`, its `Contents/Developer`, or a Command Line Tools root.
2. `XcodeRequest.Version(spec)`. The spec is a version prefix (`16`, `16.4`, `16.4.1`) or a build number (`16F6`). It is matched
   against `installations()`: a build number must match exactly, a prefix picks the highest matching version, and on a tie the install
   from the earlier search directory wins. `/Applications` is the first one by default, so it wins as in Bazel. Any other spec, such
   as `16.4-beta`, is rejected with `IllegalArgumentException` when the request is created, before anything is scanned.
3. `DEVELOPER_DIR` from the environment map passed to the locator.
4. `xcode-select -p`.

`XcodeRequest.Selected`, the default, skips the first two rules. If no rule finds a valid installation, `XcodeNotFoundException`
says what was looked at: the bad path, what `xcode-select` printed to standard error, or every installation the scan found.

#### `.xcode-version`

The locator does not look for a `.xcode-version` file, because the right starting directory depends on the caller.
`XcodeVersionFile` does the reading:

* `parse(text)` returns the first line that is neither blank nor a `#` comment as an `XcodeRequest.Version`, or `null`.
* `find(startDirectory, stopAt = null)` returns the nearest file in the directory or its parents.
* `read(file)` parses a file and names it in the error when the spec is invalid.

#### Listing installations

`installations()` scans:

* `/Applications/*.app` and `~/Applications/*.app`;
* `$XCODES_DIRECTORY/*.app` when the variable is set;
* the currently selected developer dir;
* Spotlight, only when the locator is created with `useSpotlight = true`. A failing `mdfind` is ignored and the directory scan
  result stands.

The result lists Xcode.app bundles only, highest version first. The search directories are a constructor parameter, so tests point
them at a temporary directory.

A bundle counts as Xcode when `Contents/version.plist` parses and `Contents/Developer/usr/bin/xcodebuild` exists. The bundle name does
not matter, so copies named `xcode_16.4_16F6` are recognised. The parser has external DTD loading turned off: `version.plist` declares
Apple's DTD by URL, and loading it would reach the network. Paths are deduplicated by `toRealPath()`, so `Xcode.app` and the
`Xcode-27.2.0.app` it points to count once. A symlink whose target is gone is skipped. An explicit path or `DEVELOPER_DIR` is resolved
the same way, so a link such as `/opt/xcode/current` to a `Contents/Developer` directory is described by the bundle it points into.

#### `XcodeInstallation`

```kotlin
public data class XcodeInstallation(
    public val developerDir: Path,       // …/Contents/Developer, or the CLT root
    public val appBundle: Path?,         // null for Command Line Tools
    public val kind: XcodeKind,          // XCODE or COMMAND_LINE_TOOLS
    public val version: XcodeVersion?,   // major.minor.patch from version.plist; null for CLT
    public val buildVersion: String?,    // "27B52"
)
```

A developer dir at `/Library/Developer/CommandLineTools`, or any developer dir outside an `.app`, gives an installation of kind
`COMMAND_LINE_TOOLS`. `XcodeVersion` is comparable, treats a missing component as zero (`16` equals `16.0.0`) and prints without a
zero patch (`16.4`).

## Running tools

### `AppleToolInvocation`

```kotlin
public class AppleToolInvocation(
    public val executable: Path,                         // absolute
    public val arguments: List<String>,
    public val workingDirectory: Path? = null,
    public val environment: Map<String, String>,         // set or override, applied after the cleanup
    public val environmentCleanup: EnvironmentCleanup,   // inherited variables to remove
    public val output: OutputMode,                       // Capture, Forward(consumer, errorConsumer), Discard
    public val acceptedExitCodes: Set<Int> = setOf(0),
    public val input: String? = null,                    // written to standard input as UTF-8
) {
    public fun commandLine(): List<String>
    public fun applyEnvironment(inherited: MutableMap<String, in String>)
    override fun toString(): String                      // DEVELOPER_DIR=… /usr/bin/xcrun lipo …, shell-quoted
}
```

An invocation is plain data. A caller that must start the process itself, such as a test executor, takes `commandLine()`,
`workingDirectory` and `applyEnvironment` from it. `applyEnvironment` accepts any map that can hold strings, so it also works on
environment maps typed `Map<String, Any>`. `toString()` gives the command as a shell would take it, with the variables it sets in
front, so a line from a log or an error message can be pasted into a terminal.

`OutputMode` describes standard output:

* `Capture` keeps it in `AppleToolResult.stdout`. This is the default.
* `Forward(consumer, errorConsumer)` passes each line on while the process runs. With one consumer, lines of both streams go to it.
  SwiftPM prints its progress to standard error, which is why `Forward` covers both.
* `Discard` drops it.

Standard error is always kept in `AppleToolResult.stderr`, whatever the mode, so error messages have it.

### `AppleToolRunner`

```kotlin
public fun interface AppleToolRunner {
    public fun run(invocation: AppleToolInvocation): AppleToolResult   // exitCode, stdout, stderr
}
```

A runner starts the process and returns the result whatever the exit code. The tools compare the code with `acceptedExitCodes`.
The interface exists so that a build tool integration can start processes through its own API; the library never chooses a runner on
its own.

`ProcessAppleToolRunner` is the implementation on top of `ProcessBuilder`. The process inherits the environment of the JVM. Both pipes
are read on their own threads, so neither blocks the other and the calling thread stays interruptible. The process is killed when the
calling thread is interrupted or a `Forward` consumer throws. Only the process the runner started is killed, not that process's
children. A missing executable gives `AppleToolUnavailableException`.

### Errors

| Exception                       | When                                                                                               |
|---------------------------------|----------------------------------------------------------------------------------------------------|
| `AppleToolException`            | A tool exits with a code outside `acceptedExitCodes`. Holds the `invocation`, the `result` and an optional `hint`. |
| `AppleToolUnavailableException` | The executable cannot be started, or a tool needs Xcode.app and only Command Line Tools are selected. |
| `XcodeNotFoundException`        | No installation matches a request, or a requested path is not an installation.                     |

The message of `AppleToolException` shows the command with the variables it sets, the exit code, the hint, and the last 50 lines of standard
error, or of standard output when standard error is empty. A tool adds a hint when it recognises the failure: `Xcodebuild.build`
recognises "platform is not installed" and says how to install the platform.

### `AppleTools`

```kotlin
public class AppleTools(
    public val xcode: XcodeInstallation,
    public val runner: AppleToolRunner,
    public val toolchain: String? = null,    // passed as TOOLCHAINS, for example "swift" for a swift.org toolchain
)
```

`AppleTools` has one property per tool: `xcodebuild`, `xcrun`, `lipo`, `libtool`, `installNameTool`, `codesign`, `plistBuddy`,
`plutil`, `simctl`, `swiftPackage` and `xattr`.

Every invocation sets `DEVELOPER_DIR` to `xcode.developerDir`, also when it already equals the system selection. A child process then
uses the resolved Xcode whatever the parent environment or a later `xcode-select --switch` says.

| Kind      | Tools                                                                                | Executable                |
|-----------|--------------------------------------------------------------------------------------|---------------------------|
| Toolchain | `xcodebuild`, `lipo`, `libtool`, `install_name_tool`, `swift`, `simctl`, `Xcrun.runTool` | `/usr/bin/xcrun <tool>` |
| OS        | `codesign`, `plutil`, `xattr`                                                        | `/usr/bin/<tool>`         |
| OS        | `PlistBuddy`                                                                         | `/usr/libexec/PlistBuddy` |

Nothing resolves through `PATH`. Without an explicit `toolchain`, `TOOLCHAINS` is removed from every `xcrun` invocation, because an
inherited value would redirect `xcrun` to another toolchain.

`xcodebuild` and `simctl` need Xcode.app. With a `COMMAND_LINE_TOOLS` installation they throw `AppleToolUnavailableException` before
starting a process, with a message that names the installation and says to select a full Xcode.

### `EnvironmentCleanup`

```kotlin
public class EnvironmentCleanup(
    public val names: Set<String> = emptySet(),       // removed by exact name
    public val prefixes: Set<String> = emptySet(),    // every variable starting with one of these
) {
    public operator fun plus(other: EnvironmentCleanup): EnvironmentCleanup
}
```

Which inherited variables to remove depends on the operation, so `xcodebuild` and `swift` operations take a cleanup as a parameter.
The presets name the sets that matter when a build runs from an Xcode build phase:

| Preset               | Removes                                                                                                   |
|----------------------|-----------------------------------------------------------------------------------------------------------|
| `NONE`               | Nothing.                                                                                                  |
| `SDKROOT`            | `SDKROOT`, which selects one SDK for every tool and so breaks builds for any other platform.              |
| `SDK_VARIABLES`      | Every `SDK*` variable. With them set, SwiftPM compiles package manifests for the app's platform.          |
| `DEBUG_DYLIB`        | `ENABLE_DEBUG_DYLIB` and the three `EXECUTABLE_*DYLIB*` variables, which make `xcodebuild` link the debug dylib of the surrounding app. |
| `APP_BUILD_SETTINGS` | `DEBUG_DYLIB`, `EMBED_PACKAGE_RESOURCE_BUNDLE_NAMES`, and the per-target `OTHER_*` and `ASSETCATALOG_*` settings. |

## Tools

Each tool class has an `xxxInvocation(...)` method, which is pure and returns the command, and an `xxx(...)` method that runs it and
throws `AppleToolException` on failure. Arguments are plain values: `Path`, `String`, collections and `ApplePlatform`.

### `Xcodebuild`

| Operation | Command | Notes |
|-----------|---------|-------|
| `createXCFramework(slices, output)` | `-create-xcframework {-framework … | -library … [-headers …]} [-debug-symbols …]… -output …` | A slice is `XCFrameworkSlice.Framework(path, debugSymbols)` or `XCFrameworkSlice.Library(path, headers, debugSymbols)`. Debug symbols are a list of `.dSYM` or `.bcsymbolmap` paths; they are made absolute, because `xcodebuild` rejects relative ones. `xcodebuild` fails when the output exists. |
| `build(workingDirectory, scheme, destination, …)` | `[actions] [-project …] -scheme … -destination … [-configuration …] [-derivedDataPath …] [-quiet] KEY=VALUE… [additional arguments]` | Returns the result. Takes `environment` to set, a `cleanup` (default `APP_BUILD_SETTINGS`) and an `OutputMode`. |
| `showBuildSettings(workingDirectory, project, scheme, sdk, configuration)` | `-showBuildSettings [-project …] [-scheme …] [-sdk …] [-configuration …]` | Returns the output. `cleanup` defaults to `NONE`. |
| `version()` | `-version` | Returns the `XcodeVersion` from the first line, or `null`. |

`build` output can run to megabytes. `quiet = true` limits it to warnings and errors, and `OutputMode.Forward` streams it instead of
holding it in memory.

### `Xcrun`

| Operation | Command | Notes |
|-----------|---------|-------|
| `find(tool)` | `xcrun --find <tool>` | The absolute path, for callers that must pass one, such as `ALTERNATE_LINKER=`. |
| `showSdkPath(platform)` | `xcrun --sdk <sdk> --show-sdk-path` | The SDK directory. |
| `runTool(tool, arguments, workingDirectory, input, output)` | `xcrun <tool> <arguments>` | For toolchain tools without a wrapper, such as `xcprojformatter` or `otool`. `input` is written to standard input. |

### `Lipo`, `Libtool`, `InstallNameTool`

| Operation | Command |
|-----------|---------|
| `lipo.create(inputs, output)` | `lipo -create <inputs> -output <output>` |
| `libtool.createStatic(inputs, output)` | `libtool -static -o <output> <inputs>` |
| `installNameTool.setId(binary, id)` | `install_name_tool -id <id> <binary>` |

`libtool` runs through `xcrun`, so a GNU `libtool` earlier on `PATH` is never picked up. Empty inputs are rejected before a process starts.

### `Codesign`

`sign(file, identity, force = true)` runs `codesign [--force] --sign <identity> -- <file>`. The identity is a name, a hash, or `-`
for ad hoc signing. The `--` keeps a file name that starts with a dash from being read as an option.

### `PlistBuddy`

`run(plist, commands, ignoreFailures = false)` runs `PlistBuddy -c <command>… <plist>` and returns the output. Commands are typed:

```kotlin
PlistBuddyCommand.Add(listOf("CFBundleSupportedPlatforms"), PlistValueType.ARRAY)
PlistBuddyCommand.Add(listOf("CFBundleSupportedPlatforms", "0"), PlistValueType.STRING, "iPhoneOS")
PlistBuddyCommand.Set(listOf("MinimumOSVersion"), "15.0")
PlistBuddyCommand.Delete(listOf("UIRequiredDeviceCapabilities"))
PlistBuddyCommand.Print(listOf("CFBundleVersion"))
```

An entry is addressed by a key path, one list element per level. Every key path and value is quoted. Inside a quoted token a
backslash escapes `\`, `"` and `'`, and colons in keys are escaped as well, since PlistBuddy uses them to separate key path components.
Quotes, backslashes, colons and non-ASCII text therefore reach the plist unchanged.

PlistBuddy runs every command even after one fails and then exits with 1. `ignoreFailures = true` accepts that exit code, which is
useful to delete entries that may not exist. A missing plist is created by the first command that writes.

### `Plutil`

`convertToJson(input)` runs `plutil -convert json -o - <input>` and returns the JSON. `convertToJson(input, output)` writes it to a
file. It reads any property list format, including Xcode's `project.pbxproj`.

### `Simctl`

| Operation | Command |
|-----------|---------|
| `listDevices(availableOnly = true, json = false)` | `simctl list [-j] devices [available]` |
| `listRuntimes()` | `simctl list -j runtimes` |
| `spawnInvocation(device, executable, arguments, waitForDebugger, standalone)` | `simctl spawn [--wait-for-debugger] [--standalone] <device> <executable> <arguments>` |

`spawn` has no method that runs it: the process is a test or an app whose output and lifetime belong to the caller.

### `SwiftPackage`

Every operation runs `swift` in the package directory and takes a `cleanup` that defaults to `SDKROOT`.

| Operation | Command |
|-----------|---------|
| `resolve(packageDirectory, scratchPath, additionalArguments)` | `swift package [--scratch-path …] resolve …` |
| `describe(packageDirectory)` | `swift package describe --type json` |
| `build(packageDirectory, additionalArguments)` | `swift build …` |
| `showBinPath(packageDirectory, additionalArguments)` | `swift build --show-bin-path …` |

`resolve` and `build` return the result, so warnings that SwiftPM prints to standard error on success are not lost. `describe` returns
the JSON text and `showBinPath` a `Path`.

### `Xattr`

`markCreatedByBuildSystem(directory)` runs `xattr -w com.apple.xcode.CreatedByBuildSystem true <directory>`. Xcode's "Clean Build
Folder" only deletes directories that carry this attribute.

### `ApplePlatform`

An enum of SDK platforms with the names different tools use for them:

| Entry | `sdkName` | `destinationPlatform` | `bundlePlatformName` |
|-------|-----------|-----------------------|----------------------|
| `MACOS` | `macosx` | `macOS` | `MacOSX` |
| `IOS` / `IOS_SIMULATOR` | `iphoneos` / `iphonesimulator` | `iOS` / `iOS Simulator` | `iPhoneOS` / `iPhoneSimulator` |
| `TVOS` / `TVOS_SIMULATOR` | `appletvos` / `appletvsimulator` | `tvOS` / `tvOS Simulator` | `AppleTVOS` / `AppleTVSimulator` |
| `WATCHOS` / `WATCHOS_SIMULATOR` | `watchos` / `watchsimulator` | `watchOS` / `watchOS Simulator` | `WatchOS` / `WatchSimulator` |
| `VISIONOS` / `VISIONOS_SIMULATOR` | `xros` / `xrsimulator` | `visionOS` / `visionOS Simulator` | `XROS` / `XRSimulator` |

`genericDestination` gives `generic/platform=<destinationPlatform>` for `xcodebuild -destination`, `isSimulator` tells device and
simulator platforms apart, and `fromSdkName` maps an SDK name back to an entry.

## Module layout

```
build-tools/libraries/apple/
├── build.gradle.kts
├── code-rules.md                  @../library-rules.md
├── api/apple.api
└── src/
    ├── main/kotlin/org/jetbrains/kotlin/buildtools/apple/
    │   ├── XcodeLocator.kt
    │   ├── XcodeRequest.kt
    │   ├── XcodeInstallation.kt       with XcodeKind
    │   ├── XcodeVersion.kt
    │   ├── XcodeVersionFile.kt
    │   ├── VersionPlist.kt            internal XML plist reader
    │   ├── AppleToolInvocation.kt     with OutputMode
    │   ├── AppleToolRunner.kt         with AppleToolResult and ProcessAppleToolRunner
    │   ├── AppleToolException.kt      with AppleToolUnavailableException, XcodeNotFoundException
    │   ├── EnvironmentCleanup.kt
    │   ├── ApplePlatform.kt
    │   ├── AppleTools.kt
    │   └── Xcodebuild.kt, Xcrun.kt, Lipo.kt, Libtool.kt, InstallNameTool.kt, Codesign.kt,
    │       PlistBuddy.kt, Plutil.kt, Simctl.kt, SwiftPackage.kt, Xattr.kt
    └── test/kotlin/org/jetbrains/kotlin/buildtools/apple/
        ├── <Subject>Test.kt
        ├── RecordingAppleToolRunner.kt
        └── AppleToolsMacosTest.kt
```

The library rules allow one package per library, so the tools are files in one package and not subpackages. The public API is
recorded in `api/apple.api`.

## Testing

All tests live in the library's own `test` task.

### Tests that run on every host

* `XcodeLocatorTest` builds fake `.app` bundles in `@TempDir` from a copy of a real `version.plist`. It covers xcodes-style names, a
  symlink to an install that is also scanned directly, a broken symlink, a symlinked developer dir, the same version in two search
  directories, a bundle with an unreadable version, a CLT-only machine, `DEVELOPER_DIR` given as the `.app` and as
  `Contents/Developer`, the resolution order, prefix and build-number matching, invalid specs, Spotlight on and off through the fake
  runner, and the message when nothing matches.
* `XcodeVersionFileTest` and `XcodeVersionTest` cover parsing, comparison and malformed input.
* `VersionPlistTest` checks that a DOCTYPE does not reach the network, and covers malformed files and missing keys.
* Tool tests (`XcodebuildTest`, `PlistBuddyTest`, `SimctlTest`, `SwiftPackageTest`, and `ToolCommandLinesTest` for the
  single-operation tools) assert the exact command line through `RecordingAppleToolRunner`: paths with spaces and non-ASCII
  characters, empty inputs, `DEVELOPER_DIR` and `TOOLCHAINS`, removed variables, optional flags such as `-debug-symbols` and `-quiet`,
  and the Xcode.app check for CLT installations. `PlistBuddyTest` covers values with quotes.
* `ProcessAppleToolRunnerTest` runs a script written to `@TempDir` and checks the exit code, stderr, working directory, environment
  removal, standard input, each `OutputMode`, large output on both pipes, and that the process is killed on interruption or when a
  consumer throws.

### Tests that need a Mac

`AppleToolsMacosTest` is annotated with `@EnabledOnOs(OS.MAC)` and runs the real tools on files in `@TempDir`:

* resolve the selected Xcode and compare it with `xcodebuild -version`;
* check that `xcrun --find` answers from the installation's developer dir, also for Command Line Tools next to a selected Xcode;
* compile objects for two architectures with `clang -c`, archive them with `libtool -static`, merge them with `lipo -create`,
  and pack the result with `xcodebuild -create-xcframework`;
* set the install name of a dylib with `install_name_tool` and sign it ad hoc with `codesign`;
* round-trip values with quotes, backslashes, colons and non-ASCII text through `PlistBuddy`, and convert a plist with `plutil`;
* mark a directory with `xattr`, find tools and SDKs with `xcrun`, and list simulator runtimes and devices;
* describe, resolve and build a Swift package with `swift`, and build it with `xcodebuild`.

The locator in these tests has no search directories, so they do not scan the user's home. They are skipped when no developer dir is
selected.

`test-inputs-check` allows reading files outside the repository, so `/Applications` and `/usr/bin` are fine. The test task declares
`/var/db/xcode_select_link/usr/bin/xcodebuild` (absolute path sensitivity) and `DEVELOPER_DIR` as inputs. Without them, the build
cache would reuse a result from a Linux run, where these tests are skipped, or from a machine with another Xcode.

## FAQ

### Why run toolchain tools through `xcrun` instead of `/usr/bin/<tool>`?

The `/usr/bin` shims also honour `DEVELOPER_DIR`, but only some tools have a shim, and `simctl` has none. `xcrun` reaches every tool in
the developer dir the same way and supports `TOOLCHAINS`. The cost is one extra exec per call, which is small next to the tools
themselves.

### Why set `DEVELOPER_DIR` when it equals the current selection?

The library cannot know what the selection will be when the process starts. Setting it always makes the invocation self-contained, and
since error messages print the variables an invocation sets, the command in a message reproduces the failure.

### Why read `version.plist` and not `Info.plist`?

`Info.plist` in current Xcode releases is a binary plist, which the JDK cannot read without a hand-written parser or a process.
`version.plist` is XML and contains both `CFBundleShortVersionString` and `ProductBuildVersion`.

### Why is Spotlight opt-in?

The directory scan finds xcodes installs and the usual manual ones. Spotlight adds installs in arbitrary locations, but it costs a
process launch per scan and is often disabled on CI images, where its absence would make results differ between machines.

### Why not return typed models for JSON output?

Typed models need kotlinx-serialization. Under the library rules that means an API/implementation split, and consumers would have to
load the implementation in an isolated class loader. On the stdlib alone, the library can sit on any classpath.

### Why doesn't the locator read `.xcode-version` on its own?

Where to look depends on the caller: a project root, a module directory or a workspace. `XcodeVersionFile` parses the file and can
search upwards from a directory, and the caller chooses that directory.

### Why is the environment cleanup a parameter and not fixed per tool?

The right set depends on what the nested command builds. A package build must not see the app's `OTHER_LDFLAGS`, while a build of
the app's own dependencies may rely on them. Fixed sets would be wrong for one of the two, so each operation has a default and takes
another set when the caller knows better.

### Why does every operation have an `xxxInvocation` twin?

Some callers need the command without running it, and command-line tests need it too. Returning an object with a `run()` method from
a single function would halve the API, but a forgotten `.run()` would then silently do nothing, which is a bad failure for `codesign`
or `lipo`. Two plain functions cost some repetition and cannot be misused that way.
