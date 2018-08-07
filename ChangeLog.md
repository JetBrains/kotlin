# CHANGELOG

## 1.2.70 (Work in progress)

### Compiler

- [`KT-13860`](https://youtrack.jetbrains.com/issue/KT-13860) Avoid creating KtImportDirective PSI elements for default imports in LazyImportScope
- [`KT-22201`](https://youtrack.jetbrains.com/issue/KT-22201) Generate nullability annotations for data class toString and equals methods.
- [`KT-23870`](https://youtrack.jetbrains.com/issue/KT-23870) SAM adapter method returns null-values for "genericParameterTypes"
- [`KT-24597`](https://youtrack.jetbrains.com/issue/KT-24597) IDE doesn't report missing constructor on inheritance of an expected class in common module
- [`KT-25120`](https://youtrack.jetbrains.com/issue/KT-25120) RequireKotlin on nested class and its members is not loaded correctly
- [`KT-25193`](https://youtrack.jetbrains.com/issue/KT-25193) Names of parameters from Java interface methods implemented by delegation are lost  
- [`KT-25405`](https://youtrack.jetbrains.com/issue/KT-25405) Mismatching descriptor type parameters on inner types
- [`KT-25604`](https://youtrack.jetbrains.com/issue/KT-25604) Disable callable references to exprerimental suspend functions
- [`KT-25665`](https://youtrack.jetbrains.com/issue/KT-25665) Add a warning for annotations which target non-existent accessors
- [`KT-25894`](https://youtrack.jetbrains.com/issue/KT-25894) Do not generate body for functions from Any in light class builder mode

### IDE

- [`KT-18301`](https://youtrack.jetbrains.com/issue/KT-18301) kotlin needs crazy amount of memory
- [`KT-23668`](https://youtrack.jetbrains.com/issue/KT-23668) Methods with internal visibility have different mangling names in IDE and in compiler
- [`KT-24892`](https://youtrack.jetbrains.com/issue/KT-24892) please remove usages of com.intellij.util.containers.ConcurrentFactoryMap#ConcurrentFactoryMap deprecated long ago
- [`KT-25144`](https://youtrack.jetbrains.com/issue/KT-25144) Quick fix “Change signature” changes class of argument when applied for descendant classes with enabled -Xnew-inference option
- [`KT-25356`](https://youtrack.jetbrains.com/issue/KT-25356) Update Gradle Kotlin-DSL icon according to new IDEA 2018.2 icons style

### IDE. Debugger

- [`KT-25147`](https://youtrack.jetbrains.com/issue/KT-25147) Conditional breakpoints doesn't work in `common` code of MPP
- [`KT-25152`](https://youtrack.jetbrains.com/issue/KT-25152) MPP debug doesn't navigate to `common` code if there are same named files in `common` and `platform` parts

### IDE. Inspections and Intentions

#### New Features

- [`KT-6633`](https://youtrack.jetbrains.com/issue/KT-6633) Inspection to detect unnecessary "with" calls
- [`KT-25146`](https://youtrack.jetbrains.com/issue/KT-25146) Add quick-fix for default parameter value removal

#### Fixes

- [`KT-11154`](https://youtrack.jetbrains.com/issue/KT-11154) Spell checking inspection is not suppressable
- [`KT-18681`](https://youtrack.jetbrains.com/issue/KT-18681) "Replace 'if' with 'when'" generates unnecessary else block
- [`KT-24001`](https://youtrack.jetbrains.com/issue/KT-24001) "Suspicious combination of == and ===" false positive
- [`KT-24385`](https://youtrack.jetbrains.com/issue/KT-24385) Convert lambda to reference refactor produces red code with companion object
- [`KT-24694`](https://youtrack.jetbrains.com/issue/KT-24694) Move lambda out of parentheses should not be applied for multiple functional parameters
- [`KT-25089`](https://youtrack.jetbrains.com/issue/KT-25089) False-positive "Call chain on collection type can be simplified" for `map` and `joinToString` on a `HashMap`
- [`KT-25169`](https://youtrack.jetbrains.com/issue/KT-25169) Impossible to suppress UAST/JVM inspections
- [`KT-25321`](https://youtrack.jetbrains.com/issue/KT-25321) Safe delete of a class property implementing constructor parameter at the platform side doesn't remove all the related declarations
- [`KT-25539`](https://youtrack.jetbrains.com/issue/KT-25539) `Make class open` quick fix doesn't update all the related implementations of a multiplatform class
- [`KT-25608`](https://youtrack.jetbrains.com/issue/KT-25608) Confusing "Redundant override" inspection message

### IDE. KDoc

- [`KT-22815`](https://youtrack.jetbrains.com/issue/KT-22815) Update quick documentation

### IDE. Libraries

- [`KT-25129`](https://youtrack.jetbrains.com/issue/KT-25129) Idea freezes when Kotlin plugin tries to determine if jar is js lib in jvm module

### IDE. Navigation

- [`KT-25317`](https://youtrack.jetbrains.com/issue/KT-25317) `Go to actual declaration` keyboard shortcut doesn't work for `expect object`, showing "No implementations found" message
- [`KT-25492`](https://youtrack.jetbrains.com/issue/KT-25492) Find usages: keep `Expected functions` option state while searching for usages of a regular function

### IDE. Project View

- [`KT-22823`](https://youtrack.jetbrains.com/issue/KT-22823) Text pasted into package is parsed as Kotlin before Java

### IDE. Refactorings

- [`KT-22072`](https://youtrack.jetbrains.com/issue/KT-22072) "Convert MutableMap.put to assignment" should not be applicable when put is used as expression
- [`KT-23590`](https://youtrack.jetbrains.com/issue/KT-23590) Incorrect conflict warning "Internal function will not be accessible" when moving class from jvm to common module
- [`KT-23594`](https://youtrack.jetbrains.com/issue/KT-23594) Incorrect conflict warning about IllegalStateException when moving class from jvm to common module
- [`KT-23772`](https://youtrack.jetbrains.com/issue/KT-23772) MPP: Refactor / Rename class does not update name of file containing related expect/actual class
- [`KT-23914`](https://youtrack.jetbrains.com/issue/KT-23914) Safe search false positives during moves between common and actual modules
- [`KT-25326`](https://youtrack.jetbrains.com/issue/KT-25326) Refactor/Safe Delete doesn't report `actual object` usages
- [`KT-25438`](https://youtrack.jetbrains.com/issue/KT-25438) Refactor/Safe delete of a multiplatform companion object: usage is not reported

### IDE. Script

- [`KT-25814`](https://youtrack.jetbrains.com/issue/KT-25814) IDE scripting console -> kotlin (JSR-223) - compilation errors - unresolved IDEA classes
- [`KT-25822`](https://youtrack.jetbrains.com/issue/KT-25822) jvmTarget from the script compiler options is ignored in the IDE

### IDE. Ultimate

- [`KT-25595`](https://youtrack.jetbrains.com/issue/KT-25595) Rename Kotlin-specific "Protractor" run configuration to distinguish it from the one provided by NodeJS plugin

### Reflection

- [`KT-25541`](https://youtrack.jetbrains.com/issue/KT-25541) Incorrect parameter names in reflection for inner class constructor from Java class compiled with "-parameters"

### Tools. CLI

- [`KT-21910`](https://youtrack.jetbrains.com/issue/KT-21910) Add `-Xfriend-paths` compiler argument to support internal visibility checks in production/test sources from external build systems
- [`KT-25554`](https://youtrack.jetbrains.com/issue/KT-25554) Do not report warnings when `-XXLanguage` was used to turn on deprecation

### Tools. JPS

- [`KT-25540`](https://youtrack.jetbrains.com/issue/KT-25540) JPS JS IC does not recompile usages from other modules when package is different

### Tools. kapt

- [`KT-25396`](https://youtrack.jetbrains.com/issue/KT-25396) KAPT Error: Unknown option: infoAsWarnings
