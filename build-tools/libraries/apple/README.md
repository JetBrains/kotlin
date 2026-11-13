# Apple developer tools

`org.jetbrains.kotlin:build-tools-apple` finds the Xcode a build should use and runs Apple developer tools against it.
It depends on the Kotlin stdlib and the JDK only.

The design, including how Xcode is found and why each tool is run the way it is, is in
[docs/build-tools/build-tools-apple.md](../../../docs/build-tools/build-tools-apple.md).

## Usage

```kotlin
val runner = ProcessAppleToolRunner()

// DEVELOPER_DIR if set, otherwise the xcode-select choice
val locator = XcodeLocator(runner, System.getenv())
val xcode = locator.resolve()

val tools = AppleTools(xcode, runner)
tools.lipo.create(inputs = listOf(arm64Lib, x64Lib), output = fatLib)
tools.xcodebuild.createXCFramework(
    slices = listOf(XCFrameworkSlice.Library(iosLib), XCFrameworkSlice.Library(simulatorLib)),
    output = xcframework,
)
```

With several Xcodes installed, ask for one:

```kotlin
locator.resolve(XcodeRequest.Version("16.4"))    // highest 16.4.x
locator.resolve(XcodeRequest.Version("16F6"))    // exact build number
locator.resolve(XcodeRequest.DeveloperDir(Paths.get("/Applications/Xcode-beta.app")))

// From a .xcode-version file
val request = XcodeVersionFile.find(projectDir)?.let(XcodeVersionFile::read) ?: XcodeRequest.Selected
```

A caller that starts processes itself takes only the command line:

```kotlin
val spawn = tools.simctl.spawnInvocation(device = "iPhone 16", executable = testBinary)
spawn.commandLine()   // [/usr/bin/xcrun, simctl, spawn, iPhone 16, …]
spawn.environment     // {DEVELOPER_DIR=…}
```

## What is in it

| Class                                                         | What it does                                                                     |
|---------------------------------------------------------------|----------------------------------------------------------------------------------|
| `XcodeLocator`, `XcodeRequest`, `XcodeInstallation`           | Lists Xcode installations and picks one                                          |
| `XcodeVersionFile`                                            | Reads `.xcode-version`                                                           |
| `AppleTools`                                                  | Binds the tools to one installation and one runner                               |
| `AppleToolInvocation`, `AppleToolRunner`, `AppleToolException` | A command, the thing that starts it, and the failure report                      |
| `EnvironmentCleanup`                                          | Inherited variables to remove, with presets for builds started from Xcode        |
| `Xcodebuild`                                                  | `-create-xcframework`, builds, `-showBuildSettings`, `-version`                  |
| `Xcrun`                                                       | Tool and SDK paths, and any toolchain tool without a wrapper                     |
| `Lipo`, `Libtool`, `InstallNameTool`, `Codesign`              | Universal binaries, static libraries, install names, signing                     |
| `PlistBuddy`, `Plutil`                                        | Editing property lists, converting them to JSON                                  |
| `Simctl`                                                      | Listing simulator devices and runtimes, the `spawn` command line                 |
| `SwiftPackage`                                                | `swift package resolve` and `describe`, `swift build`                            |
| `Xattr`                                                       | Marking a directory as created by the build system                               |

## Building and testing

```bash
./gradlew :build-tools:libraries:apple:check -q            # compile, tests, API dump check
./gradlew :build-tools:libraries:apple:updateKotlinAbi     # after changing public declarations
```

Most tests run on any host. `AppleToolsMacosTest` runs the real tools and needs macOS with Xcode or Command Line
Tools selected; it is skipped elsewhere.
