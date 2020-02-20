# CHANGELOG

## 1.3.70

### Compiler

- [`KT-31411`](https://youtrack.jetbrains.com/issue/KT-31411) Support mode of compiler where it analyses source-set as platform one, but produces only metadata for that specific source-set
- [`KT-32372`](https://youtrack.jetbrains.com/issue/KT-32372) Type inference errors in IDE
- [`KT-33542`](https://youtrack.jetbrains.com/issue/KT-33542) Compilation failed with "AssertionError: Suspend functions may be called either as suspension points or from another suspend function"
- [`KT-33544`](https://youtrack.jetbrains.com/issue/KT-33544) "UnsupportedOperationException: no descriptor for type constructor of TypeVariable(R)?" with BuilderInference and elvis operator
- [`KT-36297`](https://youtrack.jetbrains.com/issue/KT-36297) NI: ClassNotFoundException: compiler emits reference to nonexisting class for code with nested inline lambdas
- [`KT-36719`](https://youtrack.jetbrains.com/issue/KT-36719) Enable new inference in IDE since 1.3.70

### IDE

- [`KT-33820`](https://youtrack.jetbrains.com/issue/KT-33820) Stop using com.intellij.codeInsight.AnnotationUtil#isJetbrainsAnnotation
- [`KT-33846`](https://youtrack.jetbrains.com/issue/KT-33846) Stop using com.intellij.openapi.vfs.newvfs.BulkFileListener.Adapter

### IDE. Completion

- [`KT-36306`](https://youtrack.jetbrains.com/issue/KT-36306) Code completion inlines content of FQN class if completion called in string

### IDE. Debugger

- [`KT-16277`](https://youtrack.jetbrains.com/issue/KT-16277) Can't set breakpoint for object construction

### IDE. Inspections and Intentions

- [`KT-33771`](https://youtrack.jetbrains.com/issue/KT-33771) False positive "Redundant Companion reference" with Java synthetic property and same-named object property
- [`KT-36307`](https://youtrack.jetbrains.com/issue/KT-36307) False positive "Remove redundant '.let' call" for nested lambda change scope reference

### IDE. Multiplatform

- [`KT-33321`](https://youtrack.jetbrains.com/issue/KT-33321) In IDE, actuals of intermediate test source set are incorrectly matched against parent main source-set (not test one)

### IDE. Refactorings

- [`KT-24122`](https://youtrack.jetbrains.com/issue/KT-24122) Long pauses with "removing redundant imports" dialog on rename refactoring for IDEA Kotlin Plugin
- [`KT-32514`](https://youtrack.jetbrains.com/issue/KT-32514) Moving file in with 'search for references' inlines contents in referred source code
- [`KT-32999`](https://youtrack.jetbrains.com/issue/KT-32999) Renaming parameter does not rename usage in named argument in a different file
- [`KT-33372`](https://youtrack.jetbrains.com/issue/KT-33372) Rename resource cause its content to be replaced
- [`KT-34415`](https://youtrack.jetbrains.com/issue/KT-34415) Refactor > Change signature of an overridden actual function from a platform class: "org.jetbrains.kotlin.descriptors.InvalidModuleException: Accessing invalid module descriptor"
- [`KT-34419`](https://youtrack.jetbrains.com/issue/KT-34419) Refactor > Change signature > add a function parameter: "org.jetbrains.kotlin.descriptors.InvalidModuleException: Accessing invalid module descriptor"
- [`KT-35689`](https://youtrack.jetbrains.com/issue/KT-35689) Change Signature:  "InvalidModuleException: Accessing invalid module descriptor" on attempt to change receiver type of a member abstract function
- [`KT-35903`](https://youtrack.jetbrains.com/issue/KT-35903) Change Signature refactoring crashes by InvalidModuleException on simplest examples

### IDE. Run Configurations

- [`KT-35038`](https://youtrack.jetbrains.com/issue/KT-35038) Running a test in a multi-module MPP project via IntelliJ Idea gutter action produces incorrect Gradle Run configuration

### Libraries

- [`KT-7657`](https://youtrack.jetbrains.com/issue/KT-7657) scan functions for Sequences and Iterable
- [`KT-33761`](https://youtrack.jetbrains.com/issue/KT-33761) reduceOrNull: reduce that doesn't throw on empty input

### Tools. Gradle. JS

- [`KT-36714`](https://youtrack.jetbrains.com/issue/KT-36714) [Gradle, JS] Webpack output doesn't consider Kotlin/JS exports (library mode)

### Tools. Gradle. Multiplatform

- [`KT-36469`](https://youtrack.jetbrains.com/issue/KT-36469) Dependencies with compileOnly scope are not visible in Gradle build of MPP with source set hierarchies support