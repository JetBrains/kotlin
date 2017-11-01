# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.2-RC

### Compiler

#### Fixes

- [`KT-20774`](https://youtrack.jetbrains.com/issue/KT-20774) "::foo.isInitialized" for lateinit member properties produces incorrect bytecode
- [`KT-20826`](https://youtrack.jetbrains.com/issue/KT-20826) Can't compile Ultimate Idea with Kotlin 1.2
- [`KT-20879`](https://youtrack.jetbrains.com/issue/KT-20879) Compiler problem in when-expressions
- [`KT-20959`](https://youtrack.jetbrains.com/issue/KT-20959) Regression: Unexpected diagnostic NINITIALIZED_ENUM_COMPANION reported in 1.1.60 & 1.2.0-rc
- [`KT-20651`](https://youtrack.jetbrains.com/issue/KT-20651) Don't know how to generate outer expression" for enum-values with non-trivial self-closures

### IDE

#### New Features

- [`KT-20286`](https://youtrack.jetbrains.com/issue/KT-20286) "Configure Kotlin in project" should add kotlin-stdlib-jdk7/8 instead of kotlin-stdlib-jre7/8 starting from Kotlin 1.2

#### Fixes

- [`KT-19599`](https://youtrack.jetbrains.com/issue/KT-19599) No indentation for multiline collection literal
- [`KT-20346`](https://youtrack.jetbrains.com/issue/KT-20346) Can't build tests in common code due to missing org.jetbrains.kotlin:kotlin-test-js testCompile dependency in JS
- [`KT-20550`](https://youtrack.jetbrains.com/issue/KT-20550) Spring: "Navigate to autowired candidates" gutter action is missed (IDEA 2017.3)
- [`KT-20566`](https://youtrack.jetbrains.com/issue/KT-20566) Spring: "Navigate to the spring beans declaration" gutter action for `@ComponentScan` is missed (IDEA 2017.3)
- [`KT-20843`](https://youtrack.jetbrains.com/issue/KT-20843) Kotlin TypeDeclarationProvider may stop other declarations providers execution
- [`KT-20906`](https://youtrack.jetbrains.com/issue/KT-20906) Find symbol by name doesn't work
- [`KT-20920`](https://youtrack.jetbrains.com/issue/KT-20920) UAST: SOE Thrown in JavaColorProvider
- [`KT-20922`](https://youtrack.jetbrains.com/issue/KT-20922) Couldn't match ClsMethodImpl from Kotlin test files
- [`KT-20929`](https://youtrack.jetbrains.com/issue/KT-20929) Import Project from Gradle wizard: the same page is shown twice
- [`KT-20833`](https://youtrack.jetbrains.com/issue/KT-20833) MP project: add dependency to kotlin-test-annotation-common to common module

### IDE. Completion

- [`KT-18458`](https://youtrack.jetbrains.com/issue/KT-18458) Spring: code completion does not suggest bean names inside `@Qualifier` before function parameter

### IDE. Inspections and Intentions

- [`KT-20899`](https://youtrack.jetbrains.com/issue/KT-20899) Code Cleanup fails to convert Circlet codebase to 1.2
- [`KT-20949`](https://youtrack.jetbrains.com/issue/KT-20949) CCE from UAST (File breadcrumbs don't update when file tree does)

### IDE. Refactorings

- [`KT-20251`](https://youtrack.jetbrains.com/issue/KT-20251) Kotlin Gradle script: Refactor → Inline works incorrect when you need to inline all function occurrences

### JavaScript

- [`KT-2976`](https://youtrack.jetbrains.com/issue/KT-2976) Suggestion for cleaner style to implement !! operator
- [`KT-5259`](https://youtrack.jetbrains.com/issue/KT-5259) JS: RTTI may be break by overwriting constructor field
- [`KT-17475`](https://youtrack.jetbrains.com/issue/KT-17475) JS: object and companion object named "prototype" cause exceptions
- [`KT-18095`](https://youtrack.jetbrains.com/issue/KT-18095) JS: Wrong behavior of fun named "constructor"
- [`KT-18105`](https://youtrack.jetbrains.com/issue/KT-18105) JS: inner class "length" cause runtime exception
- [`KT-20625`](https://youtrack.jetbrains.com/issue/KT-20625) JS: Interface function with default parameter, overridden by other interface cannot be found at runtime
- [`KT-20820`](https://youtrack.jetbrains.com/issue/KT-20820) JS: IDEA project doesn't generate paths relative to .map

### Libraries

- [`KT-4900`](https://youtrack.jetbrains.com/issue/KT-4900) Support math operations in stdlib

### Tools. JPS

- [`KT-20852`](https://youtrack.jetbrains.com/issue/KT-20852) IllegalArgumentException: URI has an authority component on attempt to jps compile the gradle project with javascript module

### Tools. kapt

- [`KT-20877`](https://youtrack.jetbrains.com/issue/KT-20877) Butterknife: UninitializedPropertyAccessException: "lateinit property has not been initialized" for field annotated with `@BindView`.

## Previous releases

This release also includes the fixes and improvements from the previous
[`1.2-Beta2`](https://github.com/JetBrains/kotlin/blob/1.2-Beta2/ChangeLog.md) release.