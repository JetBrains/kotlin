# CHANGELOG

## 1.2.71

### IDE

- [`KT-25177`](https://youtrack.jetbrains.com/issue/KT-25177) Report asDynamic on dynamic type as a warning
- [`KT-25454`](https://youtrack.jetbrains.com/issue/KT-25454) Extract function: make default visibility private
- [`KT-26794`](https://youtrack.jetbrains.com/issue/KT-26794) Bad version detection during migration in Android Studio 3.2
- [`KT-26889`](https://youtrack.jetbrains.com/issue/KT-26889) Don't show migration dialog if no actual migrations are available

### JavaScript

- [`KT-26466`](https://youtrack.jetbrains.com/issue/KT-26466) Uncaught ReferenceError: println is not defined

### Tools. Gradle

- [`KT-26208`](https://youtrack.jetbrains.com/issue/KT-26208) inspectClassesForKotlinIC slows down continuous mode in Gradle

### Libraries

- [`KT-26929`](https://youtrack.jetbrains.com/issue/KT-26929) Kotlin Reflect and Proguard: can’t find referenced class kotlin.annotations.jvm.ReadOnly/Mutable

## 1.2.70

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
- [`KT-20772`](https://youtrack.jetbrains.com/issue/KT-20772) Incorrect smart cast on enum members
- [`KT-24657`](https://youtrack.jetbrains.com/issue/KT-24657) Compiler performance issues with big files
- [`KT-25745`](https://youtrack.jetbrains.com/issue/KT-25745) Do not report warning about annotations on non-existing accessors for JvmStatic properties
- [`KT-25746`](https://youtrack.jetbrains.com/issue/KT-25746) Improve message for warning about annotations that have target to non-existing accessors
- [`KT-25810`](https://youtrack.jetbrains.com/issue/KT-25810) New Inference: Overload resolution ambiguity on method 'provideDelegate(Nothing?, KProperty<*>)' when there's more than one `provideDelegate` operator in scope
- [`KT-25973`](https://youtrack.jetbrains.com/issue/KT-25973) Report metadata version mismatch upon discovering a .kotlin_module file in the dependencies with an incompatible metadata version
- [`KT-22281`](https://youtrack.jetbrains.com/issue/KT-22281) JVM: Incorrect comparison of Double and Float when types are derived from smart-casts
- [`KT-22649`](https://youtrack.jetbrains.com/issue/KT-22649) Compiler: wrong code generated / Couldn't transform method node - using inline extension property inside lambda

### IDE

- [`KT-18301`](https://youtrack.jetbrains.com/issue/KT-18301) kotlin needs crazy amount of memory
- [`KT-23668`](https://youtrack.jetbrains.com/issue/KT-23668) Methods with internal visibility have different mangling names in IDE and in compiler
- [`KT-24892`](https://youtrack.jetbrains.com/issue/KT-24892) please remove usages of com.intellij.util.containers.ConcurrentFactoryMap#ConcurrentFactoryMap deprecated long ago
- [`KT-25144`](https://youtrack.jetbrains.com/issue/KT-25144) Quick fix “Change signature” changes class of argument when applied for descendant classes with enabled -Xnew-inference option
- [`KT-25356`](https://youtrack.jetbrains.com/issue/KT-25356) Update Gradle Kotlin-DSL icon according to new IDEA 2018.2 icons style
- [`KT-20056`](https://youtrack.jetbrains.com/issue/KT-20056) TCE on creating object of an anonymous class in Kotlin script
- [`KT-25092`](https://youtrack.jetbrains.com/issue/KT-25092) SourcePsi should be physical leaf element but got OPERATION_REFERENCE
- [`KT-25249`](https://youtrack.jetbrains.com/issue/KT-25249) Uast operates "Unit" type instead of "void"
- [`KT-25255`](https://youtrack.jetbrains.com/issue/KT-25255) Preferences | Languages & Frameworks | Kotlin Updates: show currently installed version
- [`KT-25297`](https://youtrack.jetbrains.com/issue/KT-25297) Inconsistency in `KotlinULambdaExpression` and `KotlinLocalFunctionULambdaExpression`
- [`KT-25515`](https://youtrack.jetbrains.com/issue/KT-25515) Add/remove analysis-related compiler setting does not update IDE project model immediately
- [`KT-25524`](https://youtrack.jetbrains.com/issue/KT-25524) UAST: proper resolve for function variable call
- [`KT-25640`](https://youtrack.jetbrains.com/issue/KT-25640) "Configure Kotlin" action changes values of language and API version in project settings

### IDE. Debugger

- [`KT-25147`](https://youtrack.jetbrains.com/issue/KT-25147) Conditional breakpoints doesn't work in `common` code of MPP
- [`KT-25152`](https://youtrack.jetbrains.com/issue/KT-25152) MPP debug doesn't navigate to `common` code if there are same named files in `common` and `platform` parts

### IDE. Gradle

- [`KT-22732`](https://youtrack.jetbrains.com/issue/KT-22732) TestNG runner is always used for TestNG tests even when Use Gradle runner is selected
- [`KT-25913`](https://youtrack.jetbrains.com/issue/KT-25913) Honor 'store generated project files externally option' for Kotlin facets imported from Gradle
- [`KT-25955`](https://youtrack.jetbrains.com/issue/KT-25955) Support expect/actual in new MPP imported into IDEA

### IDE. Inspections and Intentions

#### New Features

- [`KT-6633`](https://youtrack.jetbrains.com/issue/KT-6633) Inspection to detect unnecessary "with" calls
- [`KT-25146`](https://youtrack.jetbrains.com/issue/KT-25146) Add quick-fix for default parameter value removal
- [`KT-7675`](https://youtrack.jetbrains.com/issue/KT-7675) Create inspection to replace if with let
- [`KT-13515`](https://youtrack.jetbrains.com/issue/KT-13515) Add intention to replace '?.let' with null check
- [`KT-13854`](https://youtrack.jetbrains.com/issue/KT-13854) Need intention actions: to convert property with getter to initializer
- [`KT-15476`](https://youtrack.jetbrains.com/issue/KT-15476) Inspection to convert non-lazy chains of collection functions into sequences
- [`KT-22068`](https://youtrack.jetbrains.com/issue/KT-22068) Force usage of “it” in .forEach{} calls 
- [`KT-23445`](https://youtrack.jetbrains.com/issue/KT-23445) Inspection and quickfix to replace `assertTrue(a == b)` with `assertEquals(a, b)`
- [`KT-25270`](https://youtrack.jetbrains.com/issue/KT-25270) "return@foo" outside of lambda should have quickfix to remove "@foo" label

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
- [`KT-16422`](https://youtrack.jetbrains.com/issue/KT-16422) Replace lambda with method reference inspections fails
- [`KT-21999`](https://youtrack.jetbrains.com/issue/KT-21999) Convert lambda to reference adds this with incorrect label
- [`KT-23467`](https://youtrack.jetbrains.com/issue/KT-23467) False positive `suspicious callable reference` on scoping function called on another lambda
- [`KT-25044`](https://youtrack.jetbrains.com/issue/KT-25044) "Implement member" quick-fix should not generate 'actual' modifier with expect declaration in interface only
- [`KT-25579`](https://youtrack.jetbrains.com/issue/KT-25579) Redundant semicolon erroneously reported during local var modifier ambiguity
- [`KT-25633`](https://youtrack.jetbrains.com/issue/KT-25633) “Add kotlin-XXX.jar to the classpath” quick fix adds dependency with invalid version in Gradle-based projects
- [`KT-25739`](https://youtrack.jetbrains.com/issue/KT-25739) "Convert to run" / "Convert to with" intentions incorrectly process references to Java static members
- [`KT-25928`](https://youtrack.jetbrains.com/issue/KT-25928) "Let extend" quick fix is suggested in case of nullable/non-null TYPE_MISMATCH collision
- [`KT-26042`](https://youtrack.jetbrains.com/issue/KT-26042) False positive "Remove redundant '.let' call" for lambda with destructured arguments

### IDE. KDoc

- [`KT-22815`](https://youtrack.jetbrains.com/issue/KT-22815) Update quick documentation
- [`KT-22648`](https://youtrack.jetbrains.com/issue/KT-22648) Quick Doc popup: break (long?) declarations into several lines

### IDE. Libraries

- [`KT-25129`](https://youtrack.jetbrains.com/issue/KT-25129) Idea freezes when Kotlin plugin tries to determine if jar is js lib in jvm module

### IDE. Navigation

- [`KT-25317`](https://youtrack.jetbrains.com/issue/KT-25317) `Go to actual declaration` keyboard shortcut doesn't work for `expect object`, showing "No implementations found" message
- [`KT-25492`](https://youtrack.jetbrains.com/issue/KT-25492) Find usages: keep `Expected functions` option state while searching for usages of a regular function
- [`KT-25498`](https://youtrack.jetbrains.com/issue/KT-25498) `Find Usages` doesn't show `Supertype` usages of `actual` declarations with constructor

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
- [`KT-25857`](https://youtrack.jetbrains.com/issue/KT-25857) Refactoring → Move moves whole file in case of moving class from Kotlin script
- [`KT-25858`](https://youtrack.jetbrains.com/issue/KT-25858) Refactoring → Move can be called only for class declarations in Kotlin script

### IDE. Script

- [`KT-25814`](https://youtrack.jetbrains.com/issue/KT-25814) IDE scripting console -> kotlin (JSR-223) - compilation errors - unresolved IDEA classes
- [`KT-25822`](https://youtrack.jetbrains.com/issue/KT-25822) jvmTarget from the script compiler options is ignored in the IDE

### IDE. Multiplatform

- [`KT-23368`](https://youtrack.jetbrains.com/issue/KT-23368) IDE: Build: JPS errors are reported for valid non-multiplatform module depending on multiplatform one

### IDE. Ultimate

- [`KT-25595`](https://youtrack.jetbrains.com/issue/KT-25595) Rename Kotlin-specific "Protractor" run configuration to distinguish it from the one provided by NodeJS plugin
- [`KT-19309`](https://youtrack.jetbrains.com/issue/KT-19309) Spring JPA Repository IntelliJ tooling with Kotlin

### IDE. Tests Support

- [`KT-26228`](https://youtrack.jetbrains.com/issue/KT-26228) NoClassDefFoundError: org/jetbrains/kotlin/idea/run/KotlinTestNgConfigurationProducer on running a JUnit test with TestNG plugin disabled

### Reflection

- [`KT-25541`](https://youtrack.jetbrains.com/issue/KT-25541) Incorrect parameter names in reflection for inner class constructor from Java class compiled with "-parameters"

### Tools. CLI

- [`KT-21910`](https://youtrack.jetbrains.com/issue/KT-21910) Add `-Xfriend-paths` compiler argument to support internal visibility checks in production/test sources from external build systems
- [`KT-25554`](https://youtrack.jetbrains.com/issue/KT-25554) Do not report warnings when `-XXLanguage` was used to turn on deprecation
- [`KT-25196`](https://youtrack.jetbrains.com/issue/KT-25196) Optional expected annotation is visible in platforms where it doesn't have actual

### Tools. JPS

- [`KT-25540`](https://youtrack.jetbrains.com/issue/KT-25540) JPS JS IC does not recompile usages from other modules when package is different

### Tools. kapt

- [`KT-25396`](https://youtrack.jetbrains.com/issue/KT-25396) KAPT Error: Unknown option: infoAsWarnings
- [`KT-26211`](https://youtrack.jetbrains.com/issue/KT-26211) Kotlin plugin 1.2.60+ breaks IDEA source/resource/test roots in a Maven project with Kapt

### Tools. Gradle

- [`KT-25025`](https://youtrack.jetbrains.com/issue/KT-25025) Inter-project IC for JS in Gradle
- [`KT-25455`](https://youtrack.jetbrains.com/issue/KT-25455) Gradle IC: when class signature is changed its indirect subclasses in different module are not recompiled

### Tools. JPS

- [`KT-25998`](https://youtrack.jetbrains.com/issue/KT-25998) Build process starts compiling w/o any changes (on release version)
- [`KT-25977`](https://youtrack.jetbrains.com/issue/KT-25977) Can not run a Kotlin test
- [`KT-26072`](https://youtrack.jetbrains.com/issue/KT-26072) MPP compilation issue
- [`KT-26113`](https://youtrack.jetbrains.com/issue/KT-26113) Build takes around 20 seconds in already fully built IDEA project

### Tools. Scripts

- [`KT-26142`](https://youtrack.jetbrains.com/issue/KT-26142) update maven-central remote repository url

### Tools. Incremental Compile

- [`KT-26528`](https://youtrack.jetbrains.com/issue/KT-26528) ISE “To save disabled cache status [delete] should be called (this behavior is kept for compatibility)” on compiling project with enabled IC in Maven

### JavaScript

- [`KT-22053`](https://youtrack.jetbrains.com/issue/KT-22053) JS: Secondary constructor of Throwable inheritor doesn't call to primary one
- [`KT-26064`](https://youtrack.jetbrains.com/issue/KT-26064) JS inliner calls wrong constructor in incremental build
- [`KT-26117`](https://youtrack.jetbrains.com/issue/KT-26117) JS runtime error: ArrayList_init instead of ArrayList_init_0

### Libraries

- [`KT-18067`](https://youtrack.jetbrains.com/issue/KT-18067) KotlinJS - String.compareTo(other: String, ignoreCase: Boolean = false): Int
- [`KT-19507`](https://youtrack.jetbrains.com/issue/KT-19507) Using @JvmName from stdlib-common fails to compile in JS module.
- [`KT-19508`](https://youtrack.jetbrains.com/issue/KT-19508) Add @JsName to stdlib-common for controlling JS implementation
- [`KT-24478`](https://youtrack.jetbrains.com/issue/KT-24478) Annotate relevant standard library annotations with @OptionalExpectation
- [`KT-25980`](https://youtrack.jetbrains.com/issue/KT-25980) JvmSynthetic annotation has no description in the docs

## Previous releases

This release also includes the fixes and improvements from the previous [`1.2.61`](https://github.com/JetBrains/kotlin/blob/1.2.60/ChangeLog.md) release.