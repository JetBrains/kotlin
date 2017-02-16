# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.1-RC

### Reflection
- [`KT-16358`](https://youtrack.jetbrains.com/issue/KT-16358) Incompatibility between kotlin-reflect 1.0 and kotlin-stdlib 1.1 fixed

### Compiler

#### Coroutine support
- [`KT-15938`](https://youtrack.jetbrains.com/issue/KT-15938) Changed error message for calling suspend function outside of suspendable context
- [`KT-16092`](https://youtrack.jetbrains.com/issue/KT-16092) Backend crash fixed: "Don't know how to generate outer expression" for destructuring suspend lambda
- [`KT-16093`](https://youtrack.jetbrains.com/issue/KT-16093) Annotations are retained during reading the binary representation of suspend functions
- [`KT-16122`](https://youtrack.jetbrains.com/issue/KT-16122) java.lang.VerifyError fixed in couroutines: (String, null, suspend () -> String)
- [`KT-16124`](https://youtrack.jetbrains.com/issue/KT-16124) Marked as UNSUPPORTED: suspension points in default parameters
- [`KT-16219`](https://youtrack.jetbrains.com/issue/KT-16219) Marked as UNSUPPORTED: suspend get/set, in/!in operators for
- [`KT-16145`](https://youtrack.jetbrains.com/issue/KT-16145) Beta-2 coroutine regression fixed (wrong code generation)

#### Kapt3
- [`KT-15524`](https://youtrack.jetbrains.com/issue/KT-15524) Fix javac error reporting in Kotlin daemon
- [`KT-15721`](https://youtrack.jetbrains.com/issue/KT-15721) JetBrains nullability annotations are now returned from Element.getAnnotationMirrors()
- [`KT-16146`](https://youtrack.jetbrains.com/issue/KT-16146) Fixed work in verbose mode
- [`KT-16153`](https://youtrack.jetbrains.com/issue/KT-16153) Ignore declarations with illegal Java identifiers
- [`KT-16167`](https://youtrack.jetbrains.com/issue/KT-16167) Fixed compilation error with kapt arguments in build.gradle
- [`KT-16170`](https://youtrack.jetbrains.com/issue/KT-16170) Stub generator now adds imports for corrected error types to stubs
- [`KT-16176`](https://youtrack.jetbrains.com/issue/KT-16176) javac's finalCompiler log is now used to determine annotation processing errors

#### Backward compatibility
- [`KT-16017`](https://youtrack.jetbrains.com/issue/KT-16017) More graceful error message for disabled features
- [`KT-16073`](https://youtrack.jetbrains.com/issue/KT-16073) Improved backward compatibility mode with version 1.0 on JDK dependent built-ins
- [`KT-16094`](https://youtrack.jetbrains.com/issue/KT-16094) Compiler considers API availability when compiling language features requiring runtime support
- [`KT-16171`](https://youtrack.jetbrains.com/issue/KT-16171) Fixed regression "Unexpected container error on Kotlin 1.0 project"
- [`KT-16199`](https://youtrack.jetbrains.com/issue/KT-16199) Do not import "kotlin.comparisons.*" by default in language version 1.0 mode

#### Various issues
- [`KT-16225`](https://youtrack.jetbrains.com/issue/KT-16225) enumValues non-reified stub implementation references nonexistent method no more
- [`KT-16291`](https://youtrack.jetbrains.com/issue/KT-16291) Smart cast works now when getting class of instance
- [`KT-16380`](https://youtrack.jetbrains.com/issue/KT-16380) Show warning when running the compiler under Java 6 or 7

### JavaScript backend
- [`KT-16144`](https://youtrack.jetbrains.com/issue/KT-16144) Fixed inlining of functions called through inheritor ("fake" override) from another module
- [`KT-16158`](https://youtrack.jetbrains.com/issue/KT-16158) Error is not reported now when library path contains JAR file without JS metadata, report warning instead
- [`KT-16160`](https://youtrack.jetbrains.com/issue/KT-16160) Companion object dispatch receiver translation fixed

### Standard library
- [`KT-7858`](https://youtrack.jetbrains.com/issue/KT-7858) Add extension function `takeUnless`
- `javaClass` extension property is deprecated, use `instance::class.java` instead
- Massive deprecations are coming in JS standard library in `kotlin.dom` and `kotlin.dom.build` packages

### IDE

#### Configuration issues
- [`KT-15899`](https://youtrack.jetbrains.com/issue/KT-15899) Kotlin facet: language and api version for submodule setup for 1.0 are filled now as 1.0 too
- [`KT-15914`](https://youtrack.jetbrains.com/issue/KT-15914) Kotlin facet works now with multi-selected modules in Project Settings too
- [`KT-15954`](https://youtrack.jetbrains.com/issue/KT-15954) Does not suggest to configure kotlin for the module after each new kt-file creation
- [`KT-16157`](https://youtrack.jetbrains.com/issue/KT-16157) freeCompilerArgs are now imported from Gradle into IDEA
- [`KT-16206`](https://youtrack.jetbrains.com/issue/KT-16206) Idea no more refuses to compile a kotlin project defined as a maven project
- [`KT-16312`](https://youtrack.jetbrains.com/issue/KT-16312) Kotlin facet: import from gradle: don't import options which are set implicitly already
- [`KT-16325`](https://youtrack.jetbrains.com/issue/KT-16325) Kotlin facet: correct configuration after upgrading the IDE plugin
- [`KT-16345`](https://youtrack.jetbrains.com/issue/KT-16345) Kotlin facet: detect JavaScript if the module has language 1.0 `kotlin-js-library` dependency

#### Coroutine support
- [`KT-16109`](https://youtrack.jetbrains.com/issue/KT-16109) Error fixed: The -Xcoroutines can only have one value 
- [`KT-16251`](https://youtrack.jetbrains.com/issue/KT-16251) Fix detection of suspend calls containing extracted parameters

#### Intention actions, inspections and quick-fixes

##### 2017.1 compatibility
- [`KT-15870`](https://youtrack.jetbrains.com/issue/KT-15870) "Package name does not match containing directory" inspection: fixed throwable "AWT events are not allowed inside write action"
- [`KT-15924`](https://youtrack.jetbrains.com/issue/KT-15924) Create Test action: fixed throwable "AWT events are not allowed inside write action"

##### Bug fixes
- [`KT-14831`](https://youtrack.jetbrains.com/issue/KT-14831) Import statement and FQN are not added on converting lambda to reference for typealias
- [`KT-15545`](https://youtrack.jetbrains.com/issue/KT-15545) Inspection "join with assignment" does not change now execution order for properties
- [`KT-15744`](https://youtrack.jetbrains.com/issue/KT-15744) Fix: intention to import `sleep` wrongly suggests `Thread.sleep`
- [`KT-16000`](https://youtrack.jetbrains.com/issue/KT-16000) Inspection "join with assignment" handles initialization with 'this' correctly
- [`KT-16009`](https://youtrack.jetbrains.com/issue/KT-16009) Auto-import for JDK classes in .kts files
- [`KT-16104`](https://youtrack.jetbrains.com/issue/KT-16104) Don't insert modifiers (e.g. suspend) before visibility

#### Completion
- [`KT-16076`](https://youtrack.jetbrains.com/issue/KT-16076) Completion does not insert more FQN kotlin.text.String
- [`KT-16088`](https://youtrack.jetbrains.com/issue/KT-16088) Completion does not insert more FQN for `kotlin` package
- [`KT-16110`](https://youtrack.jetbrains.com/issue/KT-16110) Keyword 'suspend' completion inside generic arguments
- [`KT-16243`](https://youtrack.jetbrains.com/issue/KT-16243) Performance enhanced after variable of type `ArrayList`

#### Various issues
- [`KT-15291`](https://youtrack.jetbrains.com/issue/KT-15291) 'Find usages' now does not report property access as usage of getter method in Java class with parameter
- [`KT-15647`](https://youtrack.jetbrains.com/issue/KT-15647) Exception fixed: KDoc link to member of class from different package and module
- [`KT-16071`](https://youtrack.jetbrains.com/issue/KT-16071) IDEA deadlock fixed: when typing "parse()" in .kt file
- [`KT-16149`](https://youtrack.jetbrains.com/issue/KT-16149) Intellij Idea 2017.1/Android Studio 2.3 beta3 and Kotlin plugin 1.1-beta2 deadlock fixed

### Coroutine libraries
- [`KT-15716`](https://youtrack.jetbrains.com/issue/KT-15716) Introduced startCoroutineUninterceptedOrReturn coroutine intrinsic
- [`KT-15718`](https://youtrack.jetbrains.com/issue/KT-15718) createCoroutine now returns safe continuation
- [`KT-16155`](https://youtrack.jetbrains.com/issue/KT-16155) Introduced createCoroutineUnchecked intrinsic


### Gradle support
- [`KT-15829`](https://youtrack.jetbrains.com/issue/KT-15829) Gradle Kotlin JS plugin: removed false "Duplicate source root:" warning for kotlin files
- [`KT-15902`](https://youtrack.jetbrains.com/issue/KT-15902) JS: gradle task output is now considered as source set output
- [`KT-16174`](https://youtrack.jetbrains.com/issue/KT-16174) Error fixed during IDEA-Gradle synchronization for Kotlin JS
- [`KT-16267`](https://youtrack.jetbrains.com/issue/KT-16267) JS: fixed regression in 1.1-beta2 for multi-module gradle project
- [`KT-16274`](https://youtrack.jetbrains.com/issue/KT-16274) Kotlin JS Gradle unexpected compiler error / absolute path to output file
- [`KT-16322`](https://youtrack.jetbrains.com/issue/KT-16322) Circlet project Gradle import issue fixed

### REPL
- [`KT-15861`](https://youtrack.jetbrains.com/issue/KT-15861) Use windows line separator in kotlin's JSR implementation
- [`KT-16126`](https://youtrack.jetbrains.com/issue/KT-16126) Proper `jvmTarget` for REPL compilation


### Previous releases

This release also includes the fixes and improvements from the previous
[`1.1-Beta2`](https://github.com/JetBrains/kotlin/blob/1.1-beta2/ChangeLog.md) release.
