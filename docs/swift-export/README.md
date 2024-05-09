# Swift export

> ❗️ Swift export is in very early stages of development.
> Things change very quickly, and documentation might not catch up at times.

Swift export is a tool that generates Swift bindings for public Kotlin declarations, making it possible to share business logic between
Kotlin and Swift applications.

## How to try

> ❗️ (Yep, again) Swift export is far even from Alpha state at the moment.
> We are working hard to make it feature-complete and stable.

### Separate artifacts

You can play with Swift export without KGP. Note that it might be a bit tricky at the moment:

* Swift export requires a Kotlin/Native distribution to resolve references to stdlib.
* Swift export depends on the not-yet-stable Analysis API.
* Swift export API surface is not stable by any means.
* Versions of Swift export and Analysis API artifacts might not be the same.

1. Add Maven repositories:

```kotlin
maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/swift-export-experimental")
```

2. Add dependencies:

```kotlin
// Make sure to use the latest versions of artifacts.
val swiftExportVersion = "2.0.20-dev-3855"
val kotlinVersion = "2.0.20-dev-3805"

implementation("org.jetbrains.kotlin:kotlin-native-utils:2.0.0")

implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
// Analysis API components which are required for the Swift export
implementation("org.jetbrains.kotlin:analysis-api-standalone-for-ide:$kotlinVersion") { isTransitive = false }
implementation("org.jetbrains.kotlin:analysis-api-providers-for-ide:$kotlinVersion") { isTransitive = false }
implementation("org.jetbrains.kotlin:high-level-api-for-ide:$kotlinVersion") { isTransitive = false }
implementation("org.jetbrains.kotlin:high-level-api-fir-for-ide:$kotlinVersion") { isTransitive = false }
implementation("org.jetbrains.kotlin:high-level-api-impl-base-for-ide:$kotlinVersion") { isTransitive = false }
implementation("org.jetbrains.kotlin:low-level-api-fir-for-ide:$kotlinVersion") { isTransitive = false }
implementation("org.jetbrains.kotlin:symbol-light-classes-for-ide:$kotlinVersion") { isTransitive = false }

// Swift IR declarations
implementation("org.jetbrains.kotlin:sir:$swiftExportVersion") { isTransitive = false }
// Analysis API -> Swift IR translation machinery
implementation("org.jetbrains.kotlin:sir-providers:$swiftExportVersion") { isTransitive = false }
// Swift IR -> Kotlin bridges generator
implementation("org.jetbrains.kotlin:sir-compiler-bridge:$swiftExportVersion") { isTransitive = false }
// Lazy implementation of Swift IR wrappers over Analysis API KtSymbols
implementation("org.jetbrains.kotlin:sir-light-classes:$swiftExportVersion") { isTransitive = false }
// Swift IR -> Swift sources translator
implementation("org.jetbrains.kotlin:sir-printer:$swiftExportVersion") { isTransitive = false }
// High-level API for Swift export
implementation("org.jetbrains.kotlin:swift-export-standalone:$swiftExportVersion") { isTransitive = false }
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

