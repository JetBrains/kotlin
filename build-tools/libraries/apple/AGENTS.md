# Apple developer tools library

Finds the Xcode to use and runs Apple developer tools against it. [README.md](README.md) lists the classes;
the [design doc](../../../docs/build-tools/build-tools-apple.md) explains the decisions.
The rules for every Kotlin Build Tools library are in [../AGENTS.md](../AGENTS.md); this file adds what is specific here.

## Adding a tool or an operation

1. Write a pure `xxxInvocation(...)` that returns the `AppleToolInvocation`, and an `xxx(...)` that runs it with
   `tools.runner.runChecked`. Callers that start processes themselves use the first one.
2. Build the invocation with `tools.toolchainInvocation(tool, ...)` for a tool inside the developer dir, so it runs
   through `/usr/bin/xcrun`, or with `tools.systemInvocation(absolutePath, ...)` for a tool that ships with macOS.
   Both set `DEVELOPER_DIR`; every executable is an absolute path.
3. Call `tools.requireXcodeApp(tool)` first when Command Line Tools lack the tool.
4. Validate arguments with `require` before the invocation is built: empty inputs, blank names.
5. When the tool reads inherited variables that an Xcode build phase exports, take a
   `cleanup: EnvironmentCleanup` parameter with a default, as `Xcodebuild.build` and `SwiftPackage` do.
6. For a new tool class, add a property to `AppleTools`.
7. Return JSON output as text. Typed models need a dependency this library stays without.
8. Add the operation to the Tools section of the design doc and to the README table.

## Testing

- Assert the exact `commandLine()` through `RecordingAppleToolRunner`, with a path that has a space and a non-ASCII
  character. Single-operation tools go in `ToolCommandLinesTest`; a tool with several operations gets its own class.
- Run the real tool once in `AppleToolsMacosTest`, on files in `@TempDir`. Call `assumeXcodeApp()` when the tool
  needs Xcode.app. Keep the locator there on `searchDirectories = emptyList()`, so the tests stay out of the user's home.
- `XcodeLocator` tests build fake bundles with `xcodeBundle(...)` in `@TempDir` and compare against real paths
  (`tempDir.toRealPath()`), since the locator resolves symlinks.
- Check quoting or escaping rules against the real tool before encoding them; `PlistBuddyTest` and the PlistBuddy
  round trip in `AppleToolsMacosTest` show the pattern.
