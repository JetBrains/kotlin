# CHANGELOG

## 1.3.60 EAP 2

### Compiler

- [`KT-18541`](https://youtrack.jetbrains.com/issue/KT-18541) Prohibit "tailrec" modifier on open functions
- [`KT-19844`](https://youtrack.jetbrains.com/issue/KT-19844) Do not render type annotations on symbols rendered in diagnostic messages
- [`KT-24913`](https://youtrack.jetbrains.com/issue/KT-24913) KotlinFrontEndException with local class in init of generic class
- [`KT-28940`](https://youtrack.jetbrains.com/issue/KT-28940) Concurrency issue for lazy values with the post-computation phase
- [`KT-31540`](https://youtrack.jetbrains.com/issue/KT-31540) Change initialization order of default values for tail recursive optimized functions

### IDE

- [`KT-21153`](https://youtrack.jetbrains.com/issue/KT-21153) IDE: string template + annotation usage: ISE: "Couldn't get delegate" at LightClassDataHolderKt.findDelegate()
- [`KT-33352`](https://youtrack.jetbrains.com/issue/KT-33352) "KotlinExceptionWithAttachments: Couldn't get delegate for class" on nested class/object
- [`KT-34042`](https://youtrack.jetbrains.com/issue/KT-34042) "Error loading Kotlin facets. Kotlin facets are not allowed in Kotlin/Native Module" in 192 IDEA
- [`KT-34237`](https://youtrack.jetbrains.com/issue/KT-34237) MPP with Android target: `common*` source sets are not shown as source roots in IDE

### IDE. Debugger

- [`KT-11395`](https://youtrack.jetbrains.com/issue/KT-11395) Breakpoint inside lambda argument of InlineOnly function doesn't work

### IDE. Gradle

- [`KT-32960`](https://youtrack.jetbrains.com/issue/KT-32960) KotlinMPPGradleModelBuilder takes a long time to process when syncing non-MPP project with IDE
- [`KT-34424`](https://youtrack.jetbrains.com/issue/KT-34424) With Kotlin plugin in Gradle project without Native the IDE fails to start Gradle task: "Kotlin/Native properties file is absent at null/konan/konan.properties"

### IDE. Inspections and Intentions

- [`KT-32965`](https://youtrack.jetbrains.com/issue/KT-32965) False positive "Redundant qualifier name" with nested enum member call
- [`KT-33597`](https://youtrack.jetbrains.com/issue/KT-33597) False positive "Redundant qualifier name" with class property initialized with same-named object property
- [`KT-33991`](https://youtrack.jetbrains.com/issue/KT-33991) False positive "Redundant qualifier name" with enum member function call
- [`KT-34113`](https://youtrack.jetbrains.com/issue/KT-34113) False positive "Redundant qualifier name" with Enum.values() from a different Enum

### IDE. Run Configurations

- [`KT-34366`](https://youtrack.jetbrains.com/issue/KT-34366) Implement gutters for running tests (multi-platform projects)

### Libraries

- [`KT-33864`](https://youtrack.jetbrains.com/issue/KT-33864) Read from pseudo-file system is empty

### Tools. CLI

- [`KT-24966`](https://youtrack.jetbrains.com/issue/KT-24966) Classloader problems when running basic kafka example with `kotlin` and `kotlinc`

### Tools. Gradle

- [`KT-33980`](https://youtrack.jetbrains.com/issue/KT-33980) Read the granular source sets metadata flag value once and cache it for the current Gradle build
- [`KT-34312`](https://youtrack.jetbrains.com/issue/KT-34312) UnsupportedOperationException on `requiresVisibilityOf` in the Kotlin Gradle plugin

### Tools. Gradle. JS

- [`KT-32319`](https://youtrack.jetbrains.com/issue/KT-32319) Gradle, js, webpack: source-map-loader failed to load source contents from relative urls
- [`KT-33417`](https://youtrack.jetbrains.com/issue/KT-33417) NodeTest failed with error "Failed to create MD5 hash" after NodeRun is executed
- [`KT-33747`](https://youtrack.jetbrains.com/issue/KT-33747) Exception doesn't fail the test in kotlin js node runner

### Tools. J2K

- [`KT-19355`](https://youtrack.jetbrains.com/issue/KT-19355) "Variable expected" error after J2K for increment/decrement of an object field
- [`KT-19569`](https://youtrack.jetbrains.com/issue/KT-19569) Java wrappers for primitives are converted to nullable types with nullability errors in Kotlin
- [`KT-30643`](https://youtrack.jetbrains.com/issue/KT-30643) J2K: wrong position of TYPE_USE annotation
- [`KT-32518`](https://youtrack.jetbrains.com/issue/KT-32518) Nullability information is lost after J2K convertion of constructor with null parameter
- [`KT-33941`](https://youtrack.jetbrains.com/issue/KT-33941) J2K: Overload resolution ambiguity with assertThat and `StackOverflowError` in IDEA
- [`KT-33942`](https://youtrack.jetbrains.com/issue/KT-33942) New J2K: `StackOverflowError` from `org.jetbrains.kotlin.nj2k.inference.common.BoundTypeCalculatorImpl.boundTypeUnenhanced`
- [`KT-34164`](https://youtrack.jetbrains.com/issue/KT-34164) J2K: on converting static method references in other .java sources are not corrected
- [`KT-34165`](https://youtrack.jetbrains.com/issue/KT-34165) J2K: imports are lost in conversion, references resolve to different same-named classes
- [`KT-34266`](https://youtrack.jetbrains.com/issue/KT-34266) Multiple errors after converting Java class implementing an interface from another file

### Tools. Scripts

- [`KT-33892`](https://youtrack.jetbrains.com/issue/KT-33892) REPL/Script: Implement mechanism for resolve top-level functions and properties from classloader
- [`KT-34294`](https://youtrack.jetbrains.com/issue/KT-34294) SamWithReceiver cannot be used with new scripting API

### Tools. kapt

- [`KT-33503`](https://youtrack.jetbrains.com/issue/KT-33503) Kapt, Spring Boot: "Could not resolve all files for configuration ':_classStructurekaptKotlin'"

### Kotlin Native
Related [Kotlin Native changelog](https://github.com/JetBrains/kotlin-native/blob/master/CHANGELOG.md#v1360-oct-2019) can be found separately.

## Previous releases
This release also includes the fixes and improvements from the [previous EAP release](https://github.com/JetBrains/kotlin/releases/tag/v1.3.60-eap-23).