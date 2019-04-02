# CocoaPods integration

Starting with 1.3.30, an experimental integration with [CocoaPods](https://cocoapods.org/) is added
to Kotlin/Native. This feature allows you to represent a Kotlin/Native Gradle-project as a
CocoaPods dependency. Such a representation provides the following advantages:

 - Such a dependency can be included in a Podfile of an Xcode project and automatically built (and rebuilt)
 along with this project. As a result, importing to Xcode is simplified since there is no need to
 write corresponding Gradle tasks and Xcode build steps manually.
 
 - When building from Xcode, you can use CocoaPods libraries without writing
 .def files manually and setting cinterop tool parameters. In this case, all required parameters can be
 obtained from the Xcode project configured by CocoaPods.

For an example of CocoaPods integration, refer to the
[`cocoapods`](https://github.com/JetBrains/kotlin-native/tree/master/samples/cocoapods) sample.

## CocoaPods Gradle plugin

The CocoaPods support is implemented in a separate Gradle plugin: `org.jetbrains.kotlin.native.cocoapods`.

> __Note:__ The plugin is based on the multiplatform project model and requires applying the
`org.jetbrains.kotlin.multiplatform` plugin. See details about the multiplatform plugin at
the [corresponding page](https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html).

When applied, the CocoaPods plugin does the following:

1. Adds both `debug` and `release` frameworks as output binaries for all iOS and macOS targets.
2. Creates a `podspec` task which generates a [podspec](https://guides.cocoapods.org/syntax/podspec.html)
file for the given project.

The podspec generated includes a path to an output framework and script phases which automate building
this framework during a build process of an Xcode project. Some fields of the podspec file can be
configured using the `kotlin.cocoapods { ... }` code block.

<div class="sample" markdown="1" theme="idea" mode="kotlin">

```kotlin
// Apply plugins.
plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.3.30"
    id("org.jetbrains.kotlin.native.cocoapods") version "1.3.30"
}

// CocoaPods requires the podspec to have a version.
version = "1.0"

kotlin {
    cocoapods {
        // Configure fields required by CocoaPods.
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"
    }
}
```

</div>

The following podspec fields are required by CocoaPods:
  - `version`
  - `summary`
  - `homepage`

A version of the Gradle project is used as a value for the `version` field.
Fields`summary` and `homepage` can be configured using the `cocoapods` code block.

This podspec file can be referenced from a [Podfile](https://guides.cocoapods.org/using/the-podfile.html)
of an Xcode project. After that the framework built from the Kotlin/Native module can be used from
this Xcode project. If necessary, this framework is automatically rebuilt during Xcode build process.

## Workflow

To import a Kotlin/Native module in an existing Xcode project:

0. Make sure that you have CocoaPods [installed](https://guides.cocoapods.org/using/getting-started.html#installation).
We recommend using CocoaPods 1.6.1 or later.

1. Configure a Gradle project: apply the `org.jetbrains.kotlin.native.cocoapods` plugin, add
and configure the targets, and specify the required podspec fields.

2. Run the `podspec` task. The podspec file described above will be generated.

    > In order to avoid compatibility issues during an Xcode build, the plugin requires using
    a [Gradle wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html). 
    To generate the wrapper automatically during execution of the `podspec` task,
    run it with the parameter `-Pkotlin.native.cocoapods.generate.wrapper=true`.

3. Add a reference to the generated podspec in a Podfile of the Xcode project.

    <div class="sample" markdown="1" theme="idea" mode="ruby">

    ```ruby
    target 'my-ios-app' do
        pod 'my_kotlin_library', :path => 'path/to/my-kotlin-library'
    end
    ```

    </div>

4. Run `pod install` for the Xcode project.
    
After completing these steps, you can
open the generated workspace (see [CocoaPods documentation](https://guides.cocoapods.org/using/using-cocoapods.html#installation))
and run an Xcode build.

## Interoperability

The CocoaPods plugin also allows using CocoaPods libraries without manual configuring cinterop
parameters (see the [corresponding section](https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#cinterop-support)
of the multiplatform plugin documentation). The `cocoapods { ... }` code block allows you to
add dependencies on CocoaPods libraries.

<div class="sample" markdown="1" theme="idea" mode="kotlin">

```kotlin
kotlin {
    cocoapods {
        // Configure a dependency on AFNetworking.
        // The CocoaPods version notation is supported.
        // The dependency will be added to all macOS and iOS targets.
        pod("AFNetworking", "~> 3.2.0")
    }
}
```

</div>

To use these dependencies from a Kotlin code, import a package `cocoapods.<library-name>`.
In the example above, it's `cocoapods.AFNetworking`.

The dependencies declared in this way are added in the podspec file and downloaded during
the execution of `pod install`.

> __Important:__ To correctly import the dependencies into the Kotlin/Native module, the
Podfile must contain either [`use_modular_headers!`](https://guides.cocoapods.org/syntax/podfile.html#use_modular_headers_bang)
or [`use_frameworks!`](https://guides.cocoapods.org/syntax/podfile.html#use_frameworks_bang)
directive.

Search paths for libraries added in the Kotlin/Native module in this way are obtained
from properties of the Xcode projects configured by CocoaPods. Thus if the module uses
CocoaPods libraries, it can be build __only__ __from__ __Xcode__.

## Current Limitations

 - If a Kotlin/Native module uses a CocoaPods library, you can built this module only from an Xcode project.
 Otherwise the CocoaPods library cannot be resolved by the Kotlin/Native infrastructure.
 - [Subspecs](https://guides.cocoapods.org/syntax/podspec.html#group_subspecs) are not supported.
