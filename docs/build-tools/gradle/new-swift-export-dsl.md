Design for Swift Export DSL
===

# Glossary

* **Fully exported to Swift Export.** When a module is fully exported to Swift Export, its entire API becomes visible to calling Swift 
  code.
* **Transitively exported to Swift Export.** When a module is transitively exported, only its API that leaks into the public API of the 
  exported module becomes visible to calling Swift code.
* **Package flattening.** In Swift, members don’t usually have fully-qualified names that include package names, member names are usually 
  shorter and namespaced by modules. When Kotlin code is exported to Swift, members need to be referenced using fully-qualified names in 
  Swift code, which is tedious. With package flattening, Swift Export generates typealises that allow Swift code to reference Kotlin 
  members using their simple names.

# References

* [KT-68572](https://youtrack.jetbrains.com/issue/KT-68572) Simple Swift Export DSL
* [KT-75171](https://youtrack.jetbrains.com/issue/KT-75171) Provide custom freeCompilerArgs to Swift Export's link task
* [OSIP-667](https://youtrack.jetbrains.com/issue/OSIP-667) Fix inconsistencies and formalize the scoping behavior for KMP targets

---

# **TL;DR**

* Currently, Swift Export is configured in a single `swiftExport { ... }` block that both describes the exported module and lists exported 
  project modules and third-party dependencies via `export(...)`.
* This proposal introduces an incremental improvement to the existing Swift Export model:
    * each project uses optional `export.swift { ... }` block for the Swift module name and package flattening;
    * dependencies that are declared via existing `api` scope will be fully exported to Swift automatically
* Libraries can publish Swift Export metadata (their preferred Swift module name and root package (ex `flattenPackage`)).
* Explicit `SwiftExportVisibility.EXPOSED` and `SwiftExportVisibility.HIDE`  which marks dependency as fully exported to Swift Export and 
  removes from export accordingly.
* For more complex setups there is an additional, optional `configure(...) { ... }` DSL to fine-tune how a particular dependency appears in 
  Swift.

With this model, we follow the Gradle’s dependency semantics: all api dependencies will be automatically fully-exported to Swift Export, 
which is predictable by consumers.

# Problem

### What is the current state?

Swift Export is currently configured through a single `swiftExport { ... }` block that performs multiple functions simultaneously: it 
names the root module, sets flattening rules, wires in compiler flags, and lists which projects should be exported via `export(...)`.

```kotlin
kotlin {
    swiftExport {
        moduleName = "Shared"
        flattenPackage = "com.github.example"

        export(projects.foo)
        export(projects.bar) {
            moduleName = "Bar"
            flattenPackage = "com.github.library"
        }

        configure {
            settings.put("SWIFT_EXPORT_CUSTOM_SETTING", "CUSTOM_VALUE")
            freeCompilerArgs.add("-opt-in=some.value")
        }
    }

    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    sourceSets.commonMain {
        dependencies {
            implementation(libs.kotlinx.coroutines.core)
            api(projects.foo)
            api(projects.bar)
        }
    }
}
```

### What is the problem?

* From a build script it’s hard to answer basic questions like “what is the Swift API of this project?” or “how does this particular library 
  show up in Swift?”.
* The Swift surface is described via `export(projectX)` calls, separate from the normal Gradle dependency scopes, so it’s easy for the two 
  to drift out of sync (for example, using `api(projectX)` in `dependencies` but forgetting to export it, or vice versa).
* Libraries can’t publish their own Swift view (module name, flattening, filters), so every consumer re-configures the same things by hand.
* There is no structured place to override how a given dependency is exported, and no way to catch conflicting configurations between 
  modules early.
* `embedSwiftExport` task is registered unconditionally, when we have declared apple targets. This behavior is not consistent with the 
  `embedAndSign` task.
* In ObjC export we could have one umbrella project exporting multiple frameworks/xcframeworks. With Swift Export it’s not possible to do 
  that.

## What is this proposal solving?

The proposal improves the existing model to better integrate with existing Gradle concepts and the Kotlin Gradle Plugin DSL. The 
improvements aim to make implementing simple use cases trivial, while still providing enough flexibility to support more advanced use cases.

### Why is it important?

Swift Export is becoming a core part of our Apple-target story. If it’s not obvious what exactly gets exported, it’s easy to make accidental 
breaking changes and hard for us to write reliable tooling around it. Making the exported surface explicit and predictable gives both users 
and the build tools something simple and stable to rely on.

## Anti-Goals

This proposal is deliberately **not** trying to:

* Redesign Gradle’s dependency resolution or transitivity model. All inclusion/exclusion rules stay in normal Gradle configuration 
  (`implementation`, `api`, `compileOnly`, `runtimeOnly { exclude(...) }`, etc.).
* Introduce new dependency scopes.
* Publish Swift Export as an SPM or XCFramework.
* Change how Swift Export works.
* Migrate Objective-C export API to the new DSL.

# Solution (overview)

At a high level, the proposal introduces a minimal, explicit model for Swift Export:

* Each project has a top-level `export { ... }` block, which wraps exporting capabilities. For Swift Export it would be 
  `export.swift { ... }` block, but its role is narrowed to **describing the Swift module itself**: module name and optional package 
  flattening (with `flattenPackage` renamed to `rootPackage`). The block is easy to extend to support any potential future use cases.
* The **Swift-visible part of the dependency graph** is described in the normal Gradle `dependencies { ... }` block. To mark a dependency as 
  fully-exported to Swift Export it’s just enough to declare it as `api`.
* Libraries can **publish Swift Export metadata** (their preferred Swift module name, root package (flattening), versioned). For most 
  consumers, using a library in Swift means just depending on it and marking it as exported; the metadata does the rest.
* For non-trivial setups there is an **optional override hook** that lets a consumer adjust how a specific dependency appears in Swift (e.g. 
  rename the module, change root package), with a clear precedence order between library metadata and consumer overrides and explicit errors 
  on conflicting configurations.
* When declaring dependencies as `api` is undesirable for some reason, then exporting dependencies could be adjusted with 
  `SwiftExportVisibility` enum.
* Swift Export wiring tasks for the umbrella project will now need to be enabled explicitly, they won’t be registered unconditionally 
  anymore.

# Solution

This proposal keeps Swift Export configuration in the Gradle Kotlin DSL, but rearranges it into a small set of clear responsibilities.

### Project-level `export` block

Introduce top-level `export { ... }` block, which will contain all export-related settings. For now, it will contain only `swift { ... }` 
but in future it could be extended to support other exporting capabilities (e.g. js/wasm).

```kotlin
kotlin {
    export {
        // ...
    }
}
```

### Export-level `swift` block

Introduce `export.swift { ... }` block on a project, but narrow its scope:

* **Module identity** – the Swift module name produced by this project (`moduleName`).
* **Package flattening** – a rule that collapses a Kotlin package subtree into the Swift module namespace (`rootPackage`).

This block defines the Swift module's identity and package mapping, excluding unrelated configuration.

```kotlin
kotlin {
    export {
        swift {
             moduleName = "KotlinxCoroutines"
             rootPackage = "kotlinx.coroutines"
        }
    }
}
```

### Module name precedence

* By default, the name for the swift module is derived from the corresponding Gradle project, by de-dashing and making it CamelCase (e.g. 
  `my-foo-bar` becomes `MyFooBar`)
* In case a project has an explicitly defined `moduleName`, then this name will be used
* If we redefine the export settings in the umbrella project, then this setting will take priority among others

### Library-provided Swift Export metadata

Libraries can attach small pieces of metadata to their published artifacts, such as:

* the Swift module name they want to use;
* their default flattening rule or the root package;

```kotlin
export.swift {
    moduleName = "Foo"
    rootPackage = "org.bar.foo"
}
```

A consumer that depends on that library and exports it transitively has no way to know the producer picked `Foo` instead of the name Gradle 
would derive from the dependency's coordinates, or that it wants `org.bar.foo` collapsed into the module root. Metadata publishing closes 
that gap: the producer serializes those two properties into a small JSON artifact and attaches it to its root Gradle component, and the 
consumer's Swift Export run reads it back and applies it when building the exported module list.

The serializable model looks as follows:

```kotlin
@Serializable
internal data class SwiftExportMetadata(
    val schemaVersion: Int = SWIFT_EXPORT_METADATA_SCHEMA_VERSION,
    val moduleName: String?,
    val rootPackage: String?,
) : Serializable
```

Both fields are nullable and independent, so a producer can publish just one of them. `schemaVersion` is bumped whenever the shape changes. 
Creates the `swiftExportMetadataElements` consumable configuration: `Usage = "swiftExportMetadata"`, `Category = LIBRARY`, and one artifact, 
the task's output file with `classifier = "swift-export-metadata"` and `extension = "json"`.

### Consuming library with Swift Export metadata

It takes the same configuration whose artifacts Swift Export is about to export (the target's `exportedSwiftExportApiConfiguration`) and 
re-resolves it as an artifact view with variant reselection targeting `Usage=swiftExportMetadata/Category=LIBRARY`, `lenient(true)` since 
most dependencies won't publish anything. Reusing the same configuration, rather than resolving a separate one, guarantees the metadata 
lookup and the artifact export walk the exact same dependency graph. There's no way for the two to disagree on which dependencies are even 
present.

### Declaring which dependencies are exported

Instead of `export(projects.foo)` calls inside `export.swift`, we describe the Swift-visible part of the dependency graph in the 
source set level `dependencies { ... }` block.

Conceptually:

* dependencies that should be part of the Swift API are marked explicitly as `api` in `dependencies`;
* everything else remains implementation-only and will be exported as transitive dependencies.

This makes the exported surface part of the same place where you already reason about dependencies, and avoids having a second, parallel 
list inside `swiftExport`.

### Enabling Swift Export for consumers

In order to enable Swift Export for consumers (in our case currently only Xcode) it’s now required to declare the `xcodeIntegration` inside 
`swift` block. Calling `xcodeIntegration()` activates the consumer pipeline and registers `embedSwiftExportForXcode` together with its 
supporting configurations and tasks. Configuring producer metadata alone does not activate this pipeline.

```kotlin
kotlin {
    export.swift {
        xcodeIntegration()
    }
}
```

| Configuration                             | Publishes metadata              | Registers Xcode pipeline |
|-------------------------------------------|---------------------------------|--------------------------|
| Nothing                                   | No                              | No                       |
| `export.swift { moduleName/rootPackage }` | Yes, when explicitly configured | No                       |
| `export.swift { xcodeIntegration() }`     | No metadata by itself           | Yes                      |
| Both                                      | Yes                             | Yes                      |

This would make the separation immediately obvious without introducing separate DSLs.

### Optional consumer-side overrides

Some builds need more control over how a particular dependency shows up in Swift (for example, renaming a third-party module for 
consistency, or narrowing the exported surface to a subset of packages).

For this, the proposal introduces an **optional** override hook under `export.swift.xcodeIntegration`:

```kotlin
export.swift {
    xcodeIntegration {
        // exported dependency setting override
        configure(projects.foo) { 
            moduleName = "MyModule"
            rootPackage = "mymodule"
        }    
    }
}
```

This hook is not required for normal usage; it is intended only for advanced or transitional setups.

Note that `configure(...)` will not need to perform dependency resolution, we’ll just save the configuration metadata and throw a diagnostic 
at execution time if dependency is not part of Swift export.

### Precedence and conflicts

The proposal also defines how different sources of configuration interact:

1. Consumer overrides (i.e. `configure(...)`) for a given dependency take precedence.
2. If there is no override, library metadata is used as-is.
3. If neither exists, simple defaults apply.

If two distinct resolved components produce the same final Swift module name within one Swift Export run, fail with a clear diagnostic. This 
makes it possible to keep multi-module setups consistent and to detect accidental divergence early. For example imagine two distinct 
libraries declaring the same `moduleName`, this will break Swift Export, but with custom override we can adjust the names.

### Adjusting export scopes

Swift Export uses two dedicated dependency scopes to say which libraries become part of the Swift API: `api` and `implementation`. In some 
cases it might not be enough, and we should provide additional optional settings to fine-tune this behavior.

Swift Export runner accepts Klibs with the following settings:

* Fully exported - the Klib public API will be fully exported to Swift Export.
* Transitively exported - the Klibs API will be exported only if it leaks to exported public API.
* Not exported - the Klibs API won’t be exported. In case of a leak the types will be replaced with `Never` type.

In order to adjust the dependency visibility use the same `configure(...)` block by setting the `visibility` property:

```kotlin
kotlin {
    export.swift.xcodeIntegration {
        configure(libs.foo) {
		    moduleName = "FooBar"
            visibility = SwiftExportVisibility.EXPOSED
        }
    }
    sourceSets.commonMain.dependencies {
        implementation(libs.foo)
    }
}
```

Where `SwiftExportVisibility` is a simple enum:

```kotlin
enum class SwiftExportVisibility { EXPOSED, HIDDEN }
```

We are intentionally not introducing `SwiftExportVisibility.TRANSITIVE`, because transitivity is a complex concept and we don’t want to 
expose this to the user DSL.

Visibility could be configured also with a more simple DSL:

```kotlin
kotlin {
    export.swift.xcodeIntegration {
        configure(libs.foo, SwiftExportVisibility.EXPOSED)
        configure(libs.bar, SwiftExportVisibility.HIDDEN)
    }
}
```

### Rules precedence

Since we have multiple ways to export our dependencies, it’s important to understand the priority rules.

* Explicit visibility overrides always take precedence over the ones derived from dependency scopes:
    * `SwiftExportVisibility.EXPOSED` and `api` - they are equivalent, so the precedence between them doesn't matter
    * `SwiftExportVisibility.HIDDEN` takes precedence over `api`. This covers the case where the build author wants a dependency to be added 
      to the build but excluded from Swift Export.
* When the visibility was overridden explicitly multiple times, the latest override wins.

Both `SwiftExportVisibility.HIDDEN` and `SwiftExportVisibility.EXPOSED` are not transitive. This means that the visibility will be changed 
only for that library, but not for its dependencies.

### Complete example

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.ktor)
        api(libs.bar)
        implementation(libs.foo)
    }

    export.swift {
        moduleName = "Shared"           // optional
        rootPackage = "org.jb.kotlin"   // optional

        xcodeIntegration {
            // Marks the dependency as fully-exported to Swift Export
            // but still hides from other Gradle consumers.
 	        configure(libs.foo, SwiftExportVisibility.EXPOSED)

            // Exclude the dependency from export.
            configure(libs.bar, SwiftExportVisibility.HIDDEN)

            // Overrides the configuration of a module that's already
            // being exported via the dependency graph, either fully
            // or transitively, without changing its export mode.
            configure(libs.ktor) {
                moduleName = "MyKtor"
                rootPackage = "myktor"
            }

            settings.put("SWIFT_EXPORT_CUSTOM_SETTING", "CUSTOM_VALUE")
        }
    }
}
```

Here:

* `libs.ktor` will be fully exported to Swift Export.
* `configure(libs.foo, SwiftExportVisibility.EXPOSED)` says “Foo exports to the Swift surface”, but at the same time Foo is hidden from 
  other Gradle consumers.
* The `configure(libs.bar, SwiftExportVisibility.HIDDEN)` excludes `libs.bar` from Swift Export, but remaining dependencies are still 
  transitive.
* Note, we are also removing `freeCompilerArgs,` as it is a super-advanced case, and we don’t know about any use case of it.

### Rollout

We will not be introducing any feature flags to gate the new DSL, and we’ll reuse the existing `ExperimentalSwiftExportDsl` annotation for 
the `swift` entry point:

```kotlin
export {
    @OptIn(ExperimentalSwiftExportDsl::class)
    swift {
        // ...
    }
}
```

## Related work

[KT-86833](https://youtrack.jetbrains.com/issue/KT-86833) [KGP] Migrate Apple/Swift-export/Native Gson usages to kotlinx-serialization; 
introduce FileSerializer (Done)

# Prior art

_What would be the alternatives to the proposed solution? What would happen if we don’t solve the problem? Why should this proposal be 
preferred?_

**Possible Alternatives:**

* Introduce a new `exported(...)` dependency scope to both add a dependency as `api` and mark it as exported to Swift.
    * This approach was rejected due to how complex and confusing introducing a new dependency scope is.

```kotlin
kotlin.dependencies {
    exported(libs.ktor) {
        name = "MyKtor"
        flattenPackageName = "myktor"
    }
    exportedOnly(libs.bar)
}
```

* Instead of introducing a new dependency configuration (`exported`), add an `exported` parameter to relevant existing configurations. That 
  way the semantics of the existing configurations are preserved and there’s no confusion about whether `exported` behaves like `api` or 
  `implementation`. This approach also helps avoid having to redeclare the dependency just to modify its Swift export configuration.

```kotlin
kotlin.dependencies {
    api(libs.ktor) {
        exported = true
    }
}
```

* Do not introduce `expose` and `hide`, instead adjust visibility in `configure`

```kotlin
kotlin {
    export {
        swift {
            configure(libs.ktor, SwiftExportVisibility.HIDDEN)
            configure(libs.foo, SwiftExportVisibility.EXPOSED)
            configure(libs.bar) {
                moduleName = "BarBar"
                rootPackage = "org.github.bar"
                visibility = SwiftExportVisibility.EXPOSED
            }
        }
    }
}
```

* ObjC Export, just for the reference:

```kotlin
kotlin {
    dependencies {
        implementation(libs.ktor.core)
    }

    iosTarget.binaries.framework {
        baseName = "Shared"
        isStatic = false

        export(libs.kermit)
        export(libs.ktor.core)
        export(libs.multiplatformSettings)
    }
}
```

# FAQ

_Answers to questions you’ve commonly been asked after requesting comments for this proposal, but it is not worth to add in the proposal 
text itself._

**Q:** Do we want to support  `configure.settings` and `configure.freeCompilerArgs` in `export.swift` block and then collect them across 
dependency modules in the exported module?

* Use case: supposedly a dependency module specifies a compiler arg (e.g. “supports coroutines export”) that the exported module doesn’t 
  specify. Should the exported module discover the setting in the dependency module and apply it to the export globally?

**A:** The setting will be top-level property along with `moduleName and rootPackage`.

**Q:** Should the contents of the `configure` block be included in published Gradle metadata?

**A:** No

**Q:** Should we go through the official deprecation process for the current version of the DSL, or is it okay to break it since it’s 
experimental?

**A:** Deprecate old DSL, but don’t break existing projects. Point to documentation

**Q**: Could we use camelCase for `SwiftExportVisibility` enum?

**A**: According to docs we could use camelCase [https://kotlinlang.org/docs/coding-conventions.html\#property-names](https://kotlinlang.org/docs/coding-conventions.html#property-names)

So the enum will look like:

```kotlin
enum class SwiftExportVisibility { exposed, hidden }
```
