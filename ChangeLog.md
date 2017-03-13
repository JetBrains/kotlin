# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.1.1

### IDE
- [`KT-16714`](https://youtrack.jetbrains.com/issue/KT-16714) J2K: Write access is allowed from event dispatch thread only

### Compiler
- [`KT-16801`](https://youtrack.jetbrains.com/issue/KT-16801) Accessors of `@PublishedApi` property gets mangled
- [`KT-16673`](https://youtrack.jetbrains.com/issue/KT-16673) Potentially problematic code causes exception when work with SAM adapters

### Libraries
- [`KT-16557`](https://youtrack.jetbrains.com/issue/KT-16557) Correct `SinceKotlin(1.1)` for all declarations in `kotlin.reflect.full`

## 1.1.1-RC

### IDE
- [`KT-15200`](https://youtrack.jetbrains.com/issue/KT-15200) Show implementation should show inherited classes if a typealias to base class/interface is used
- [`KT-16481`](https://youtrack.jetbrains.com/issue/KT-16481) Kotlin debugger & bytecode fail on select statement blocks (IllegalStateException: More than one package fragment)

### Gradle support
- [`KT-15783`](https://youtrack.jetbrains.com/issue/KT-15783) Gradle builds don't use incremental compilation due to an error: "Could not connect to kotlin daemon"
- [`KT-16434`](https://youtrack.jetbrains.com/issue/KT-16434) Gradle plugin does not compile androidTest sources when Jack is enabled
- [`KT-16546`](https://youtrack.jetbrains.com/issue/KT-16546) Enable incremental compilation in gradle by default

### Compiler
- [`KT-16184`](https://youtrack.jetbrains.com/issue/KT-16184) AbstractMethodError in Kapt3ComponentRegistrar while compiling from IntelliJ using Kotlin 1.1.0
- [`KT-16578`](https://youtrack.jetbrains.com/issue/KT-16578) Fix substitutor for synthetic SAM adapters
- [`KT-16581`](https://youtrack.jetbrains.com/issue/KT-16581) VerifyError when calling default value parameter with jvm-target 1.8
- [`KT-16583`](https://youtrack.jetbrains.com/issue/KT-16583) Cannot access private file-level variables inside a class init within the same file if a secondary constructor is present
- [`KT-16587`](https://youtrack.jetbrains.com/issue/KT-16587) AbstractMethodError: Delegates not generated correctly for private interfaces
- [`KT-16598`](https://youtrack.jetbrains.com/issue/KT-16598) Incorrect error: The feature "bound callable references" is only available since language version 1.1
- [`KT-16621`](https://youtrack.jetbrains.com/issue/KT-16621) Kotlin compiler doesn't report an error if a class implements Annotation interface but doesn't implement annotationType method
- [`KT-16441`](https://youtrack.jetbrains.com/issue/KT-16441) `NoSuchFieldError: $$delegatedProperties` when delegating through `provideDelegate` in companion object

### Previous releases

This release also includes the fixes and improvements from the previous
[`1.1`](https://github.com/JetBrains/kotlin/blob/1.1.0/ChangeLog.md) release.