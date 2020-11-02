# CHANGELOG

## 1.4.20-RC

### Compiler

#### New Features

- [`KT-31567`](https://youtrack.jetbrains.com/issue/KT-31567) Support special semantics for underscore-named catch block parameters

#### Performance Improvements

- [`KT-41741`](https://youtrack.jetbrains.com/issue/KT-41741) NI: "AssertionError: Empty intersection for types" with generic Java collection
- [`KT-42195`](https://youtrack.jetbrains.com/issue/KT-42195) NI: prohibitively long compilation time for values of nested data structures with type inference
- [`KT-42221`](https://youtrack.jetbrains.com/issue/KT-42221) Native compiler never finishes frontend phase after migrating to Kotlin 1.4.10

#### Fixes

- [`KT-17691`](https://youtrack.jetbrains.com/issue/KT-17691) Wrong argument order in resolved call with varargs
- [`KT-25114`](https://youtrack.jetbrains.com/issue/KT-25114) Prohibit @JvmStatic on functions in private companions
- [`KT-33917`](https://youtrack.jetbrains.com/issue/KT-33917) Prohibit to expose anonymous types from private inline functions
- [`KT-35870`](https://youtrack.jetbrains.com/issue/KT-35870) Forbid secondary enum class constructors which do not delegate to the primary constructor
- [`KT-39098`](https://youtrack.jetbrains.com/issue/KT-39098) NI: parameter of anonymous function can be inferred to Any? if another parameter's type is specified
- [`KT-41176`](https://youtrack.jetbrains.com/issue/KT-41176) NI with Gson: "ClassCastException: java.util.ArrayList cannot be cast to java.lang.Void"
- [`KT-41194`](https://youtrack.jetbrains.com/issue/KT-41194) ClassCastException on returning Result.failure from lambda within suspend function
- [`KT-42438`](https://youtrack.jetbrains.com/issue/KT-42438) NI: ClassCastException: cannot be cast to java.lang.Void caused by when statement in `run` function
- [`KT-42699`](https://youtrack.jetbrains.com/issue/KT-42699) False positive NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT diagnostic in new jvm-default modes
- [`KT-42706`](https://youtrack.jetbrains.com/issue/KT-42706) Kotlin 1.4 infers generic is Nothing instead of actual Foo class (Android project)

### IDE

- [`KT-42883`](https://youtrack.jetbrains.com/issue/KT-42883) No highlighting for elements marked as @Deprecated in stdlib

### IDE. Decompiler, Indexing, Stubs

- [`KT-41646`](https://youtrack.jetbrains.com/issue/KT-41646) "AssertionError: ContentElementType: FILE"; Code analysis never finishes on some files from my project

### IDE. Gradle Integration

- [`KT-38830`](https://youtrack.jetbrains.com/issue/KT-38830) addTransitiveDependenciesOnImplementedModules performance is slowing down Android Studio Gradle Sync

### IDE. Inspections and Intentions

- [`KT-43037`](https://youtrack.jetbrains.com/issue/KT-43037) Disable "Incomplete destructuring declaration" in 1.4.20

### JavaScript

- [`KT-37829`](https://youtrack.jetbrains.com/issue/KT-37829) Kotlin JS IR: "Properties without fields are not supported" for companion objects
- [`KT-39740`](https://youtrack.jetbrains.com/issue/KT-39740) KJS / IR: Can't use Serializable and JsExport annotations at the same time

### Libraries

- [`KT-19192`](https://youtrack.jetbrains.com/issue/KT-19192) Provide file system extensions/APIs based on java.nio.file.Path
- [`KT-41837`](https://youtrack.jetbrains.com/issue/KT-41837) Remove @ExperimentalStdlibApi from CancellationException

### Tools. CLI

- [`KT-41916`](https://youtrack.jetbrains.com/issue/KT-41916) Add JVM target bytecode version 15

### Tools. Gradle. JS

- [`KT-42494`](https://youtrack.jetbrains.com/issue/KT-42494) KJS / Gradle: "Configuration cache state could not be cached" caused by Gradle configuration cache

### Tools. Gradle. Native

- [`KT-42531`](https://youtrack.jetbrains.com/issue/KT-42531) Gradle task "podGenIos" fails if a Pod with a static library is added.
