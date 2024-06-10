# Swift export

> ❗️ Swift export is in very early stages of development.
> Things change very quickly, and documentation might not catch up at times.

Swift export is a tool that generates Swift bindings for public Kotlin declarations, making it possible to share business logic between
Kotlin and Swift applications.

## How to try

> ❗️ (Yep, again) Swift export is far even from Alpha state at the moment.
> We are working hard to make it feature-complete and stable.

### Sample project

[swift-export-sample](https://github.com/Kotlin/swift-export-sample) provides an example of using Swift export in a typical KMP project.

### Kotlin Playground

You can [play](https://pl.kotl.in/mT89eWpvD) with Swift export at the Kotlin playground to get familiar with the Swift API it generates.

A few notes:
* Playground implementation inserts `stub()` in functions instead of compiler bridges to make the generated API a bit cleaner.
* It takes a bit of time to include the latest changes from Swift export on Playground. 

### Separate artifacts

You can play with Swift export without KGP. Note that it might be a bit tricky at the moment:

* Swift export requires a Kotlin/Native distribution to resolve references to stdlib.
* Swift export depends on the not-yet-stable Analysis API.
* Swift export API surface is not stable by any means.
* Versions of Swift export and Analysis API artifacts might not be the same.

1. Add Maven repositories:

```kotlin
maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/swift-export-experimental")
```

2. Add dependencies:

```kotlin
// Make sure to use the latest versions of artifacts.
val kotlinVersion = "2.0.20-dev-+"

implementation("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")
implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
implementation("org.jetbrains.kotlin:swift-export-embeddable:$kotlinVersion")
```

3. Call Swift export APIs:

```kotlin
val inputModule = InputModule.Binary("ModuleName", Path("path-to-input-klib"))
val config = SwiftExportConfig(
    outputPath = Path("path-to-output-artifacts"),
    distribution = Distribution(
        konanHome = "$HOME/.konan/some-kotlin-native-distribution"
    ),
)
val result = runSwiftExport(inputModule, config)
// explore the result!
```

## More info

* [Architecture overview](architecture.md)
* [Kotlin to Swift mapping](language-mapping.md)
* [Swift wrappers to Kotlin bridges](compiler-bridges.md)

## Slack channel

We use the `#swift-export` Slack channel as the place to share our progress, and we encourage you to join us there!

