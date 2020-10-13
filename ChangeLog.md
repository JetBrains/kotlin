# CHANGELOG

## 1.4.20-M2

### Android

- [`KT-42121`](https://youtrack.jetbrains.com/issue/KT-42121) Deprecate Kotlin Android Extensions compiler plugin
- [`KT-42267`](https://youtrack.jetbrains.com/issue/KT-42267) `Platform declaration clash` error in IDE when using `kotlinx.android.parcel.Parcelize`
- [`KT-42406`](https://youtrack.jetbrains.com/issue/KT-42406) Long or infinite code analysis on simple files modification

### Compiler

- [`KT-11713`](https://youtrack.jetbrains.com/issue/KT-11713) Refine visibility check for synthetic property with protected setter
- [`KT-21147`](https://youtrack.jetbrains.com/issue/KT-21147) JEP 280: Indify String Concatenation (StringConcatFactory)
- [`KT-34178`](https://youtrack.jetbrains.com/issue/KT-34178) Scripts should be able to access imports objects
- [`KT-41484`](https://youtrack.jetbrains.com/issue/KT-41484) JVM IR: support -Xemit-jvm-type-annotations
- [`KT-42005`](https://youtrack.jetbrains.com/issue/KT-42005) JVM / IR: "NullPointerException: Parameter specified as non-null is null" when toString is called on inline class with not primitive property
- [`KT-42450`](https://youtrack.jetbrains.com/issue/KT-42450) NI: "IllegalStateException: Error type encountered: NonFixed:" with coroutines
- [`KT-42523`](https://youtrack.jetbrains.com/issue/KT-42523) Missed DefaultImpls for interface in `-jvm-default=all` mode on inheriting it from interface compiled in old scheme
- [`KT-42524`](https://youtrack.jetbrains.com/issue/KT-42524) Wrong specialization diagnostic is reported on inheriting from java interface with default with -Xjvm-default=all-compatibility
- [`KT-42546`](https://youtrack.jetbrains.com/issue/KT-42546) HMPP: bogus overload resolution ambiguity on using and expect-function declaration with nullable expect in a signature

### Docs & Examples

- [`KT-42318`](https://youtrack.jetbrains.com/issue/KT-42318) No documentation for `kotlin.js.js`

### IDE

- [`KT-38959`](https://youtrack.jetbrains.com/issue/KT-38959) IDE: False negative EXPLICIT_DELEGATION_CALL_REQUIRED, "IllegalArgumentException: Range must be inside element being annotated"

### IDE. Debugger

- [`KT-38659`](https://youtrack.jetbrains.com/issue/KT-38659) Evaluate Expression: `toString()` on variable returns error when breakpoint is in `commonTest` sourceset

### IDE. Decompiler, Indexing, Stubs

- [`KT-28732`](https://youtrack.jetbrains.com/issue/KT-28732) Stub file element types should be registered early enough
- [`KT-41346`](https://youtrack.jetbrains.com/issue/KT-41346) IDE: "AssertionError: Stub type mismatch: USER_TYPE!=REFERENCE_EXPRESSION" with `CollapsedDumpParser` class from IDEA SDK
- [`KT-41859`](https://youtrack.jetbrains.com/issue/KT-41859) File analysis never ending with kotlinx.cli (AssertionError: Stub type mismatch: TYPEALIAS!=CLASS)

### IDE. Gradle. Script

- [`KT-41141`](https://youtrack.jetbrains.com/issue/KT-41141) Gradle Kotlin DSL: "cannot access 'java.lang.Comparable'. Check your module classpath" with empty JDK in Project structure

### IDE. Inspections and Intentions

- [`KT-38915`](https://youtrack.jetbrains.com/issue/KT-38915) "Remove explicit type specification" intention should be disabled in explicit API mode
- [`KT-38981`](https://youtrack.jetbrains.com/issue/KT-38981) "Specify return type explicitly" inspection is not reported for declaration annotated with @PublishedApi in Explicit Api mode
- [`KT-39026`](https://youtrack.jetbrains.com/issue/KT-39026) 'Specify return type explicitly' intention duplicates compiler warning in Explicit api mode

### IDE. Script

- [`KT-41622`](https://youtrack.jetbrains.com/issue/KT-41622) IDE: Kotlin scripting support can't find context class from same project
- [`KT-41905`](https://youtrack.jetbrains.com/issue/KT-41905) IDE / Script: FilePathPattern parameter in @KotlinScript annotation is not reflected correctly in Pattern / Extension
- [`KT-42206`](https://youtrack.jetbrains.com/issue/KT-42206) Cannot load script definitions using org.jetbrains.kotlin.jsr223.ScriptDefinitionForExtensionAndIdeConsoleRootsSource

### IDE. Tests Support

- [`KT-37799`](https://youtrack.jetbrains.com/issue/KT-37799) Don't show a target choice in context menu for a test launched on specific platform

### IDE. Wizards

- [`KT-42372`](https://youtrack.jetbrains.com/issue/KT-42372) Rrename test classes in wizard template to avoid name clashing

### JavaScript

- [`KT-38136`](https://youtrack.jetbrains.com/issue/KT-38136) JS IR BE: add an ability to generate separate js files for each module and maybe each library
- [`KT-38868`](https://youtrack.jetbrains.com/issue/KT-38868) [MPP / JS / IR] IllegalStateException: "Serializable class must have single primary constructor" for expect class without primary constructor with @Serializable annotation
- [`KT-41275`](https://youtrack.jetbrains.com/issue/KT-41275) KJS / IR: "IllegalStateException: Can't find name for declaration FUN" caused by default value in constructor parameter
- [`KT-41627`](https://youtrack.jetbrains.com/issue/KT-41627) KJS / IR / Serialization: IllegalStateException: Serializable class must have single primary constructor

### KMM Plugin

- [`KT-41522`](https://youtrack.jetbrains.com/issue/KT-41522) KMM: exceptions for Mobile Multiplatform plugin are suggested to report to Google, not JetBrains
- [`KT-42065`](https://youtrack.jetbrains.com/issue/KT-42065) [KMM plugin] iOS apps fail to launch on iOS simulator with Xcode 12

### Libraries

- [`KT-41799`](https://youtrack.jetbrains.com/issue/KT-41799) String.replace performance improvements

### Native. C and ObjC Import

- [`KT-41250`](https://youtrack.jetbrains.com/issue/KT-41250) [C-interop] Stubs for C functions without parameter names should have non-stable names

### Native. Platform libraries

- [`KT-42191`](https://youtrack.jetbrains.com/issue/KT-42191) Support for Xcode 12

### Native. Runtime. Memory

- [`KT-42275`](https://youtrack.jetbrains.com/issue/KT-42275) "Memory.cpp:1605: runtime assert: Recursive GC is disallowed" sometimes when using Kotlin from Swift deinit

### Native. Stdlib

- [`KT-39145`](https://youtrack.jetbrains.com/issue/KT-39145) MutableData append method

### Tools. Compiler Plugins

- [`KT-36329`](https://youtrack.jetbrains.com/issue/KT-36329) Provide diagnostic in kotlinx.serialization when custom serializer mismatches property type
- [`KT-40030`](https://youtrack.jetbrains.com/issue/KT-40030) Move the Parcelize functionality out of the Android Extensions plugin

### Tools. Gradle. JS

- [`KT-39838`](https://youtrack.jetbrains.com/issue/KT-39838) Kotlin/JS Gradle tooling: NPM dependencies of different kinds with different versions of the same package fail with "Cannot find package@version in yarn.lock"
- [`KT-40202`](https://youtrack.jetbrains.com/issue/KT-40202) Kotlin/JS: Gradle: NPM version range operators are written into package.json as escape sequences
- [`KT-40986`](https://youtrack.jetbrains.com/issue/KT-40986) KJS / Gradle: BuildOperationQueueFailure when two different versions of js library are used as dependencies
- [`KT-42222`](https://youtrack.jetbrains.com/issue/KT-42222) KJS / Gradle: "Cannot find package@version in yarn.lock" when npm dependencies of one package but with different version are used in project
- [`KT-42339`](https://youtrack.jetbrains.com/issue/KT-42339) Support dukat binaries generation

### Tools. Gradle. Native

- [`KT-40999`](https://youtrack.jetbrains.com/issue/KT-40999) CocoaPods Gradle plugin: Support custom cinterop options when declaring a pod dependency.
- [`KT-41844`](https://youtrack.jetbrains.com/issue/KT-41844) Kotlin 1.4.10 gradle configuration error with cocoapods using multiple multiplatform modules

### Tools. Scripts

- [`KT-37987`](https://youtrack.jetbrains.com/issue/KT-37987) Kotlin script: hyphen arguments not forwarded to script
- [`KT-39502`](https://youtrack.jetbrains.com/issue/KT-39502) Scripting: reverse order of Severity enum so that ERROR > INFO
- [`KT-42335`](https://youtrack.jetbrains.com/issue/KT-42335) No "caused by" info about an exception that thrown in Kotlin Script

### Tools. kapt

- [`KT-25960`](https://youtrack.jetbrains.com/issue/KT-25960) Interfaces annotated with JvmDefault has wrong modifiers during annotation processing
- [`KT-37732`](https://youtrack.jetbrains.com/issue/KT-37732) Kapt task is broken after update to 1.3.70/1.3.71
