# Changelog 1.2.X

## 1.2.71

### Compiler

- [`KT-26806`](https://youtrack.jetbrains.com/issue/KT-26806) Defining constants using kotlin.math is broken in 1.2.70

### IDE

- [`KT-26399`](https://youtrack.jetbrains.com/issue/KT-26399) Kotlin Migration: NPE at KotlinMigrationProjectComponent$onImportFinished$1.run()
- [`KT-26794`](https://youtrack.jetbrains.com/issue/KT-26794) Bad version detection during migration in Android Studio 3.2
- [`KT-26823`](https://youtrack.jetbrains.com/issue/KT-26823) Fix deadlock in databinding with AndroidX which led to Android Studio hanging
- [`KT-26889`](https://youtrack.jetbrains.com/issue/KT-26889) Don't show migration dialog if no actual migrations are available
- [`KT-25177`](https://youtrack.jetbrains.com/issue/KT-25177) Report asDynamic on dynamic type as a warning
- [`KT-25454`](https://youtrack.jetbrains.com/issue/KT-25454) Extract function: make default visibility private

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

## 1.2.60

### Compiler

- [`KT-13762`](https://youtrack.jetbrains.com/issue/KT-13762) Prohibit annotations with target 'EXPRESSION' and retention 'BINARY' or 'RUNTIME'
- [`KT-18882`](https://youtrack.jetbrains.com/issue/KT-18882) Allow code to have platform specific annotations when compiled for different platforms
- [`KT-20356`](https://youtrack.jetbrains.com/issue/KT-20356) Internal compiler error - This method shouldn't be invoked for INVISIBLE_FAKE visibility
- [`KT-22517`](https://youtrack.jetbrains.com/issue/KT-22517) Deprecate smartcasts for local delegated properties
- [`KT-23153`](https://youtrack.jetbrains.com/issue/KT-23153) Compiler allows to set non constant value as annotation parameter
- [`KT-23413`](https://youtrack.jetbrains.com/issue/KT-23413) IndexOutOfBoundsException on local delegated properties from `provideDelegate` if there's at least one non-local delegated property
- [`KT-23742`](https://youtrack.jetbrains.com/issue/KT-23742) Optimise inline class redundant boxing on return from inlined lambda
- [`KT-24513`](https://youtrack.jetbrains.com/issue/KT-24513) High memory usage in Kotlin and 2018.1
- [`KT-24617`](https://youtrack.jetbrains.com/issue/KT-24617) Optional expected annotation is unresolved in a dependent platform module
- [`KT-24679`](https://youtrack.jetbrains.com/issue/KT-24679) KotlinUCallExpression doesn't resolve callee if it is an inline method
- [`KT-24808`](https://youtrack.jetbrains.com/issue/KT-24808) NI: nested `withContext` call is reported with `Suspension functions can be called only within coroutine body` error
- [`KT-24825`](https://youtrack.jetbrains.com/issue/KT-24825) NoClassDefFoundError on SAM adapter in a nested call in inlined lambda since 1.2.40
- [`KT-24859`](https://youtrack.jetbrains.com/issue/KT-24859) Disallow calls of functions annotated with receiver annotated with @RestrictsSuspension in foreign suspension context
- [`KT-24911`](https://youtrack.jetbrains.com/issue/KT-24911) Kotlin 1.2.50: UI for @RecentlyNonNull looks strange in the editor
- [`KT-25333`](https://youtrack.jetbrains.com/issue/KT-25333) Restrict visibility of Java static members from supertypes of companion object

### IDE

#### Performance Improvements

- [`KT-20924`](https://youtrack.jetbrains.com/issue/KT-20924) Slow KtLightAbstractAnnotation.getClsDelegate() lightAnnotations.kt 
- [`KT-23844`](https://youtrack.jetbrains.com/issue/KT-23844) Kotlin property accessor searcher consumes CPU when invoked on a scope consisting only of Java files

#### Fixes

- [`KT-4311`](https://youtrack.jetbrains.com/issue/KT-4311) "Override members" works wrong when function is extension
- [`KT-13948`](https://youtrack.jetbrains.com/issue/KT-13948) IDE plugins: improve description
- [`KT-15300`](https://youtrack.jetbrains.com/issue/KT-15300) "INFO - project.TargetPlatformDetector - Using default platform" flood in log
- [`KT-17350`](https://youtrack.jetbrains.com/issue/KT-17350) Implement members from interface fails when one of the generic types is unresolved
- [`KT-17668`](https://youtrack.jetbrains.com/issue/KT-17668) Edit Configuration dialog doesn't have a button for choosing the "Main class" field
- [`KT-19102`](https://youtrack.jetbrains.com/issue/KT-19102) Wrong equals() and hashCode() code generated for arrays of arrays
- [`KT-20056`](https://youtrack.jetbrains.com/issue/KT-20056) TCE on creating object of an anonymous class in Kotlin script
- [`KT-21863`](https://youtrack.jetbrains.com/issue/KT-21863) Imported typealias to object declared as "Unused import directive" when only referring to methods
- [`KT-23272`](https://youtrack.jetbrains.com/issue/KT-23272) Git commit not working
- [`KT-23407`](https://youtrack.jetbrains.com/issue/KT-23407) Pasting callable reference from different package suggests imports, but inserts incompilable FQN
- [`KT-23456`](https://youtrack.jetbrains.com/issue/KT-23456) UAST: Enum constant constructor call arguments missing from Kotlin enums
- [`KT-23942`](https://youtrack.jetbrains.com/issue/KT-23942) Fix building light-classes for MPP project containing multi-file facades
- [`KT-24072`](https://youtrack.jetbrains.com/issue/KT-24072) Kotlin SDK appears as many times as there are modules in the project
- [`KT-24412`](https://youtrack.jetbrains.com/issue/KT-24412) Kotlin create project wizard: Kotlin/JS no SDK
- [`KT-24933`](https://youtrack.jetbrains.com/issue/KT-24933) please remove usages of com.intellij.psi.search.searches.DirectClassInheritorsSearch#search(com.intellij.psi.PsiClass, com.intellij.psi.search.SearchScope, boolean, boolean) deprecated long ago
- [`KT-24943`](https://youtrack.jetbrains.com/issue/KT-24943) Project leak via LibraryEffectiveKindProviderImpl
- [`KT-24979`](https://youtrack.jetbrains.com/issue/KT-24979) IndexNotReadyException in KtLightClassForSourceDeclaration#isInheritor
- [`KT-24958`](https://youtrack.jetbrains.com/issue/KT-24958) Escaping goes insane when editing interpolated string in injected fragment editor
- [`KT-25024`](https://youtrack.jetbrains.com/issue/KT-25024) Wrong resolve scope while resolving java.lang.String PsiClassReferenceType
- [`KT-25092`](https://youtrack.jetbrains.com/issue/KT-25092) SourcePsi should be physical leaf element but got OPERATION_REFERENCE
- [`KT-25242`](https://youtrack.jetbrains.com/issue/KT-25242) 'Resolved to error element' highlighting is confusingly similar to an active live template
- [`KT-25249`](https://youtrack.jetbrains.com/issue/KT-25249) Uast operates "Unit" type instead of "void"
- [`KT-25255`](https://youtrack.jetbrains.com/issue/KT-25255) Preferences | Languages & Frameworks | Kotlin Updates: show currently installed version
- [`KT-25297`](https://youtrack.jetbrains.com/issue/KT-25297) Inconsistency in `KotlinULambdaExpression` and `KotlinLocalFunctionULambdaExpression`
- [`KT-25414`](https://youtrack.jetbrains.com/issue/KT-25414) Support checking eap-1.3 channel for updates
- [`KT-25524`](https://youtrack.jetbrains.com/issue/KT-25524) UAST: proper resolve for function variable call
- [`KT-25546`](https://youtrack.jetbrains.com/issue/KT-25546) Create popup in 1.2.x plugin if user upgrade version in gradle or maven to kotlin 1.3

### IDE. Android

- [`KT-17946`](https://youtrack.jetbrains.com/issue/KT-17946) Android Studio: remove Gradle configurator on configuring Kotlin
- [`KT-23040`](https://youtrack.jetbrains.com/issue/KT-23040) Wrong run configuration classpath in a mixed Java/Android project
- [`KT-24321`](https://youtrack.jetbrains.com/issue/KT-24321) Actual implementations from Android platform module are wrongly reported with `no corresponding expected declaration` in IDE
- [`KT-25018`](https://youtrack.jetbrains.com/issue/KT-25018) Exception `Dependencies for org.jetbrains.kotlin.resolve.calls.* cannot be satisfied` on a simple project in AS 3.2 Canary

### IDE. Code Style, Formatting

- [`KT-14066`](https://youtrack.jetbrains.com/issue/KT-14066) Comments on when branches are misplaced
- [`KT-25008`](https://youtrack.jetbrains.com/issue/KT-25008) Formatter: Use single indent for multiline elvis operator

### IDE. Completion

- [`KT-23627`](https://youtrack.jetbrains.com/issue/KT-23627) Autocompletion inserts FQN of stdlib functions inside of scoping lambda called on explicit `this`
- [`KT-25239`](https://youtrack.jetbrains.com/issue/KT-25239) Add postfix template for listOf/setOf/etc

### IDE. Debugger

- [`KT-23162`](https://youtrack.jetbrains.com/issue/KT-23162) Evaluate expression in multiplatform common test fails with JvmName missing when run in JVM
- [`KT-24903`](https://youtrack.jetbrains.com/issue/KT-24903) Descriptors leak from `KotlinMethodSmartStepTarget`

### IDE. Decompiler

- [`KT-23981`](https://youtrack.jetbrains.com/issue/KT-23981) Kotlin bytecode decompiler works in AWT thread

### IDE. Gradle

- [`KT-24614`](https://youtrack.jetbrains.com/issue/KT-24614) Gradle can't get published versions until commenting repositories in settings.gradle

### IDE. Gradle. Script

- [`KT-24588`](https://youtrack.jetbrains.com/issue/KT-24588) Multiple Gradle Kotlin DSL script files dependencies lifecycle is flawed

### IDE. Hints

- [`KT-22432`](https://youtrack.jetbrains.com/issue/KT-22432) Type hints: Don't include ".Companion" in the names of types defined inside companion object
- [`KT-22653`](https://youtrack.jetbrains.com/issue/KT-22653) Lambda return hint is duplicated for increment/decrement expressions
- [`KT-24828`](https://youtrack.jetbrains.com/issue/KT-24828) Double return hints on labeled expressions

### IDE. Inspections and Intentions

#### New Features

- [`KT-7710`](https://youtrack.jetbrains.com/issue/KT-7710) Intention to convert lambda to anonymous function
- [`KT-11850`](https://youtrack.jetbrains.com/issue/KT-11850) Add `nested lambdas with implicit parameters` warning
- [`KT-13688`](https://youtrack.jetbrains.com/issue/KT-13688) Add 'Change to val' quickfix for delegates without setValue
- [`KT-13782`](https://youtrack.jetbrains.com/issue/KT-13782) Intention (and may be inspection) to convert toString() call to string template
- [`KT-14779`](https://youtrack.jetbrains.com/issue/KT-14779) Inspection to replace String.format with string templates
- [`KT-15666`](https://youtrack.jetbrains.com/issue/KT-15666) Unused symbol: delete header & its implementations together
- [`KT-18810`](https://youtrack.jetbrains.com/issue/KT-18810) Quick-fix for 'is' absence in when
- [`KT-22871`](https://youtrack.jetbrains.com/issue/KT-22871) Add quickfix to move const val into companion object
- [`KT-23082`](https://youtrack.jetbrains.com/issue/KT-23082) Add quick-fix for type variance conflict
- [`KT-23306`](https://youtrack.jetbrains.com/issue/KT-23306) Add intention of putting remaining when-values even in end, and even if there is "else"
- [`KT-23897`](https://youtrack.jetbrains.com/issue/KT-23897) Inspections: report extension functions declared in same class 
- [`KT-24295`](https://youtrack.jetbrains.com/issue/KT-24295) Add "Remove 'lateinit'" quickfix
- [`KT-24509`](https://youtrack.jetbrains.com/issue/KT-24509) Inspection "JUnit tests should return Unit" 
- [`KT-24815`](https://youtrack.jetbrains.com/issue/KT-24815) Add Quick fix to remove illegal "const" modifier
- [`KT-25238`](https://youtrack.jetbrains.com/issue/KT-25238) Add quickfix wrapping expression into listOf/setOf/etc in case of type mismatch

#### Fixes

- [`KT-12298`](https://youtrack.jetbrains.com/issue/KT-12298) Fix override signature doesn't remove bogus reciever
- [`KT-20523`](https://youtrack.jetbrains.com/issue/KT-20523) Don't mark as unused functions with `@kotlin.test.*` annotations  and classes with such members
- [`KT-20583`](https://youtrack.jetbrains.com/issue/KT-20583) Report "redundant let" even for `it` in argument position
- [`KT-21556`](https://youtrack.jetbrains.com/issue/KT-21556) "Call chain on collection type may be simplified" generates uncompiled code on IntArray
- [`KT-22030`](https://youtrack.jetbrains.com/issue/KT-22030) Invalid Function can be private inspection
- [`KT-22041`](https://youtrack.jetbrains.com/issue/KT-22041) "Convert lambda to reference" suggested incorrectly
- [`KT-22089`](https://youtrack.jetbrains.com/issue/KT-22089) Explict This inspection false negative with synthetic Java property
- [`KT-22094`](https://youtrack.jetbrains.com/issue/KT-22094) Can be private false positive with function called from lambda inside inline function
- [`KT-22162`](https://youtrack.jetbrains.com/issue/KT-22162) Add indices to loop fails on destructing declarator
- [`KT-22180`](https://youtrack.jetbrains.com/issue/KT-22180) "Can be private" false positive when function is called by inline function inside property initializer
- [`KT-22371`](https://youtrack.jetbrains.com/issue/KT-22371) "Create secondary constructor" quick fix is not suggested for supertype constructor reference
- [`KT-22758`](https://youtrack.jetbrains.com/issue/KT-22758) "Create ..." and "Import" quick fixes are not available on unresolved class name in primary constructor
- [`KT-23105`](https://youtrack.jetbrains.com/issue/KT-23105) Create actual implementation shouldn't generate default parameter values
- [`KT-23106`](https://youtrack.jetbrains.com/issue/KT-23106) Implement methods should respect actual modifier as well
- [`KT-23326`](https://youtrack.jetbrains.com/issue/KT-23326) "Add missing actual members" quick fix fails with AE at KtPsiFactory.createDeclaration() with _wrong_ expect code
- [`KT-23452`](https://youtrack.jetbrains.com/issue/KT-23452) "Remove unnecessary parentheses" reports parens of returned function
- [`KT-23686`](https://youtrack.jetbrains.com/issue/KT-23686) "Add missing actual members" should not add primary actual constructor if it's present as secondary one
- [`KT-23697`](https://youtrack.jetbrains.com/issue/KT-23697) Android project with 'org.jetbrains.kotlin.platform.android' plugin: all multiplatform IDE features are absent
- [`KT-23752`](https://youtrack.jetbrains.com/issue/KT-23752) False positive "Remove variable" quick fix on property has lambda or anonymous function initializer
- [`KT-23762`](https://youtrack.jetbrains.com/issue/KT-23762) Add missing actual members quick fix adds actual declaration for val/var again if it was in the primary constructor
- [`KT-23788`](https://youtrack.jetbrains.com/issue/KT-23788) Can't convert long char literal to string if it starts with backslash
- [`KT-23860`](https://youtrack.jetbrains.com/issue/KT-23860) Import quick fix is not available in class constructor containing transitive dependency parameters
- [`KT-24349`](https://youtrack.jetbrains.com/issue/KT-24349) False positive "Call on collection type may be reduced"
- [`KT-24374`](https://youtrack.jetbrains.com/issue/KT-24374) "Class member can have private visibility" inspection reports `expect` members
- [`KT-24422`](https://youtrack.jetbrains.com/issue/KT-24422) Android Studio erroneously reporting that `@Inject lateinit var` can be made private
- [`KT-24423`](https://youtrack.jetbrains.com/issue/KT-24423) False inspection warning "redundant type checks for object"
- [`KT-24425`](https://youtrack.jetbrains.com/issue/KT-24425) wrong hint remove redundant Companion
- [`KT-24537`](https://youtrack.jetbrains.com/issue/KT-24537) False positive `property can be private` on actual properties in a multiplatform project 
- [`KT-24557`](https://youtrack.jetbrains.com/issue/KT-24557) False warning "Remove redundant call" for nullable.toString
- [`KT-24562`](https://youtrack.jetbrains.com/issue/KT-24562) actual extension function implementation warns Receiver type unused
- [`KT-24632`](https://youtrack.jetbrains.com/issue/KT-24632) Quick fix to add getter and setter shouldn't use `field` when it is not allowed
- [`KT-24816`](https://youtrack.jetbrains.com/issue/KT-24816) Inspection: Sealed subclass can be object shouldn't be reported on classes with state 

### IDE. JS

- [`KT-5948`](https://youtrack.jetbrains.com/issue/KT-5948) JS: project shouldn't have "Java file" in new item menu

### IDE. Multiplatform

- [`KT-23722`](https://youtrack.jetbrains.com/issue/KT-23722) MPP: Run tests from common modules should recompile correspond JVM implementation module
- [`KT-24159`](https://youtrack.jetbrains.com/issue/KT-24159) MPP: Show Kotlin Bytecode does not work for common code
- [`KT-24839`](https://youtrack.jetbrains.com/issue/KT-24839) freeCompilerArgs are not imported into Kotlin facet of Android module in IDEA

### IDE. Navigation

- [`KT-11477`](https://youtrack.jetbrains.com/issue/KT-11477) Kotlin searchers consume CPU in a project without any Kotlin files
- [`KT-17512`](https://youtrack.jetbrains.com/issue/KT-17512) Finding usages of actual declarations in common modules
- [`KT-20825`](https://youtrack.jetbrains.com/issue/KT-20825) Header icon on actual class is lost on new line adding
- [`KT-21011`](https://youtrack.jetbrains.com/issue/KT-21011) Difference in information shown for "Is subclassed by" gutter on mouse hovering and clicking
- [`KT-21113`](https://youtrack.jetbrains.com/issue/KT-21113) Expected gutter icon on companion object is unstable
- [`KT-21710`](https://youtrack.jetbrains.com/issue/KT-21710) Override gutter markers are missing for types in sources jar
- [`KT-22177`](https://youtrack.jetbrains.com/issue/KT-22177) Double "A" icon for an expect class with constructor
- [`KT-23685`](https://youtrack.jetbrains.com/issue/KT-23685) Navigation from expect part to actual with ctrl+alt+B shortcut should provide a choice to what actual part to go
- [`KT-24812`](https://youtrack.jetbrains.com/issue/KT-24812) Search suggestion text overlaps for long names

### IDE. Refactorings

- [`KT-15159`](https://youtrack.jetbrains.com/issue/KT-15159) Introduce typealias: Incorrect applying of a typealias in constructor calls in val/var and AssertionError
- [`KT-15351`](https://youtrack.jetbrains.com/issue/KT-15351) Extract Superclass/Interface: existent target file name is rejected; TCE: "null cannot be cast to non-null type org.jetbrains.kotlin.psi.KtFile" at ExtractSuperRefactoring.createClass()
- [`KT-16281`](https://youtrack.jetbrains.com/issue/KT-16281) Extract Interface: private member with Make Abstract = Yes produces incompilable code
- [`KT-16284`](https://youtrack.jetbrains.com/issue/KT-16284) Extract Interface/Superclass: reference to private member turns incompilable, when referring element is made abstract
- [`KT-17235`](https://youtrack.jetbrains.com/issue/KT-17235) Introduce Parameter leaks listener if refactoring is cancelled while in progress
- [`KT-17742`](https://youtrack.jetbrains.com/issue/KT-17742) Refactor / Rename Java getter to `get()` does not update Kotlin references
- [`KT-18555`](https://youtrack.jetbrains.com/issue/KT-18555) Refactor / Extract Interface, Superclass: Throwable: "Refactorings should be invoked inside transaction" at RefactoringDialog.show()
- [`KT-18736`](https://youtrack.jetbrains.com/issue/KT-18736) Extract interface: import for property type is omitted
- [`KT-20260`](https://youtrack.jetbrains.com/issue/KT-20260) AE “Unexpected container” on calling Refactor → Move for class in Kotlin script
- [`KT-20465`](https://youtrack.jetbrains.com/issue/KT-20465) "Introduce variable" in build.gradle.kts creates a variable with no template to change its name
- [`KT-20467`](https://youtrack.jetbrains.com/issue/KT-20467) Refactor → Extract Function: CCE “KtNamedFunction cannot be cast to KtClassOrObject” on calling refactoring for constructor
- [`KT-20469`](https://youtrack.jetbrains.com/issue/KT-20469) NDFDE “Descriptor wasn't found for declaration VALUE_PARAMETER” on calling Refactor → Extract Function on constructor argument
- [`KT-22931`](https://youtrack.jetbrains.com/issue/KT-22931) Converting a scoping function with receiver into one with parameter may change the semantics
- [`KT-23983`](https://youtrack.jetbrains.com/issue/KT-23983) Extract function: Reified type parameters are not extracted properly
- [`KT-24460`](https://youtrack.jetbrains.com/issue/KT-24460) Rename refactoring does not update super call
- [`KT-24574`](https://youtrack.jetbrains.com/issue/KT-24574) Changing Java constructor signature from Kotlin usage is totally broken
- [`KT-24712`](https://youtrack.jetbrains.com/issue/KT-24712) Extract Function Parameter misses 'suspend' for lambda type
- [`KT-24763`](https://youtrack.jetbrains.com/issue/KT-24763) "Change signature" refactoring breaks Kotlin code
- [`KT-24968`](https://youtrack.jetbrains.com/issue/KT-24968) Type hints disappear after "Copy" refactoring
- [`KT-24992`](https://youtrack.jetbrains.com/issue/KT-24992) The IDE got stuck showing a modal dialog (kotlin refactoring) and doesn’t react to any actions

### IDE. Script

- [`KT-25373`](https://youtrack.jetbrains.com/issue/KT-25373) Deadlock in idea plugin

### IDE. Tests Support

- [`KT-18319`](https://youtrack.jetbrains.com/issue/KT-18319) Gradle: Run tests action does not work when test name contains spaces
- [`KT-22306`](https://youtrack.jetbrains.com/issue/KT-22306) Empty gutter menu for main() and test methods in Kotlin/JS project
- [`KT-23672`](https://youtrack.jetbrains.com/issue/KT-23672) JUnit test runner is unaware of @kotlin.test.Test tests when used in common multiplatform module, even if looked from JVM multiplatform module
- [`KT-25253`](https://youtrack.jetbrains.com/issue/KT-25253) No “run” gutter icons for tests in Kotlin/JS project

### JavaScript

- [`KT-22376`](https://youtrack.jetbrains.com/issue/KT-22376) JS: TranslationRuntimeException on 'for (x in ("a"))'
- [`KT-23458`](https://youtrack.jetbrains.com/issue/KT-23458) ClassCastException when compiling when statements to JS

### Libraries

- [`KT-24204`](https://youtrack.jetbrains.com/issue/KT-24204) Empty progression last value overflows resulting in progression being non-empty
- [`KT-25351`](https://youtrack.jetbrains.com/issue/KT-25351) `TestNGAsserter` needs to swap expected/actual

### Reflection

- [`KT-16616`](https://youtrack.jetbrains.com/issue/KT-16616) KotlinReflectionInternalError: Reflection on built-in Kotlin types is not yet fully supported in getMembersOfStandardJavaClasses.kt
- [`KT-17542`](https://youtrack.jetbrains.com/issue/KT-17542) KotlinReflectionInternalError on ::values of enum class 
- [`KT-20442`](https://youtrack.jetbrains.com/issue/KT-20442) ReflectJvmMapping.getJavaConstructor() fails with Call is not yet supported for anonymous class
- [`KT-21973`](https://youtrack.jetbrains.com/issue/KT-21973) Method.kotlinFunction for top level extension function returns null when app is started from test sources
- [`KT-22048`](https://youtrack.jetbrains.com/issue/KT-22048) Reflection explodes when attempting to get constructors of an enum with overridden method

### Tools. Android Extensions

- [`KT-22576`](https://youtrack.jetbrains.com/issue/KT-22576) Parcelable: Allow Parcelize to work with object and enum types
- [`KT-24459`](https://youtrack.jetbrains.com/issue/KT-24459) @IgnoredOnParcel annotation doesn't work for @Parcelize
- [`KT-24720`](https://youtrack.jetbrains.com/issue/KT-24720) Parcelable: java.lang.LinkageError

### Tools. Compiler Plugins

- [`KT-23808`](https://youtrack.jetbrains.com/issue/KT-23808) Array<Int> in @Parcelize class generates an java.lang.VerifyError

### Tools. Gradle

- [`KT-18621`](https://youtrack.jetbrains.com/issue/KT-18621) org.jetbrains.kotlin.incremental.fileUtils.kt conflicts when compiler and gradle plugin in classpath
- [`KT-24497`](https://youtrack.jetbrains.com/issue/KT-24497) Externalized all-open plugin is not applied to a project
- [`KT-24559`](https://youtrack.jetbrains.com/issue/KT-24559) Multiple Kotlin daemon instances are started when building MPP with Gradle
- [`KT-24560`](https://youtrack.jetbrains.com/issue/KT-24560) Multiple Kotlin daemon instances are started when Gradle parallel build is used
- [`KT-24653`](https://youtrack.jetbrains.com/issue/KT-24653) Kotlin plugins don't work when classpath dependency is not declared in current or root project
- [`KT-24675`](https://youtrack.jetbrains.com/issue/KT-24675) Use Gradle dependency resolution to get compiler classpath
- [`KT-24676`](https://youtrack.jetbrains.com/issue/KT-24676) Use Gradle dependency resolution to form compiler plugin classpath
- [`KT-24946`](https://youtrack.jetbrains.com/issue/KT-24946) ISE: "The provided plugin org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar is not compatible with this version of compiler" when build simple Gradle with Zulu JDK  

### Tools. Incremental Compile

- [`KT-25051`](https://youtrack.jetbrains.com/issue/KT-25051) Change in "kotlin-android" project w/o package parts causes non-incremental compilation of dependent modules

### Tools. J2K

- [`KT-9945`](https://youtrack.jetbrains.com/issue/KT-9945) converting java to kotlin confuses git

### Tools. JPS

- [`KT-19957`](https://youtrack.jetbrains.com/issue/KT-19957) Support incremental compilation to JS in JPS
- [`KT-22611`](https://youtrack.jetbrains.com/issue/KT-22611) Support compiling scripts in JPS
- [`KT-23558`](https://youtrack.jetbrains.com/issue/KT-23558) JPS: Support multiplatform projects
- [`KT-23757`](https://youtrack.jetbrains.com/issue/KT-23757) JPS: Incremental multiplatform projects compilation 
- [`KT-24936`](https://youtrack.jetbrains.com/issue/KT-24936) Don't use internal terms in compiler progress messages
- [`KT-25218`](https://youtrack.jetbrains.com/issue/KT-25218) Build fails as Javac doesn't see Kotlin classes

### Tools. Scripts

- [`KT-24926`](https://youtrack.jetbrains.com/issue/KT-24926) NoSuchElementException in TemplateAnnotationVisitor when upgrading the Gradle Kotlin DSL to Kotlin 1.2.50

### Tools. kapt

- [`KT-24313`](https://youtrack.jetbrains.com/issue/KT-24313) Unable to use KAPT when dependency to it is added to buildSrc
- [`KT-24449`](https://youtrack.jetbrains.com/issue/KT-24449) 'kapt.kotlin.generated' is not marked as source root in Android Studio 3.1 and 3.2
- [`KT-24538`](https://youtrack.jetbrains.com/issue/KT-24538) Kapt performs Kotlin compilation when annotation processors are not configured
- [`KT-24919`](https://youtrack.jetbrains.com/issue/KT-24919) Caused by: org.gradle.api.InvalidUserDataException: 'projectDir' is not a file
- [`KT-24963`](https://youtrack.jetbrains.com/issue/KT-24963) gradle kapt plugin's assumption on build dir causing duplicate class error
- [`KT-24985`](https://youtrack.jetbrains.com/issue/KT-24985) Kapt: Allow to disable info->warning mapping in logger
- [`KT-25071`](https://youtrack.jetbrains.com/issue/KT-25071) kapt sometimes emits java stubs with imports that should be static imports
- [`KT-25131`](https://youtrack.jetbrains.com/issue/KT-25131) Kapt should not load annotation processors when generating stubs

## 1.2.51

### Backend. JVM

- [`KT-23943`](https://youtrack.jetbrains.com/issue/KT-23943) Wrong autoboxing for non-null inline class inside elvis with `null` constant
- [`KT-24952`](https://youtrack.jetbrains.com/issue/KT-24952) EnumConstantNotPresentExceptionProxy from Java reflection on annotation class with target TYPE on JVM < 8
- [`KT-24986`](https://youtrack.jetbrains.com/issue/KT-24986) Android project release build with ProGuard enabled crashes with IllegalAccessError: Final field cannot be written to by method

### Binary Metadata

- [`KT-24944`](https://youtrack.jetbrains.com/issue/KT-24944) Exception from stubs: "Unknown type parameter with id = 1" (EA-120997)

### Reflection

- [`KT-23962`](https://youtrack.jetbrains.com/issue/KT-23962) MalformedParameterizedTypeException when reflecting GeneratedMessageLite.ExtendableMessage

### Tools. Gradle

- [`KT-24956`](https://youtrack.jetbrains.com/issue/KT-24956) Kotlin Gradle plugin's inspectClassesForKotlinIC task for the new 1.2.50 release takes incredibly long
- [`KT-23866`](https://youtrack.jetbrains.com/issue/KT-23866) Kapt plugin should pass arguments from compiler argument providers to annotation processors
- [`KT-24716`](https://youtrack.jetbrains.com/issue/KT-24716) 1.2.50 emits warning "Classpath entry points to a non-existent location:"
- [`KT-24832`](https://youtrack.jetbrains.com/issue/KT-24832) Inter-project IC does not work when "kotlin-android" project depends on "kotlin" project
- [`KT-24938`](https://youtrack.jetbrains.com/issue/KT-24938) Gradle parallel execution fails on multi-module Gradle Project
- [`KT-25027`](https://youtrack.jetbrains.com/issue/KT-25027) Kapt plugin: Kapt and KaptGenerateStubs tasks have some incorrect inputs

### Tools. Scripts

- [`KT-24926`](https://youtrack.jetbrains.com/issue/KT-24926) NoSuchElementException in TemplateAnnotationVisitor when upgrading the Gradle Kotlin DSL to Kotlin 1.2.50

## 1.2.50

### Compiler

- [`KT-23360`](https://youtrack.jetbrains.com/issue/KT-23360) Do not serialize annotations with retention SOURCE to metadata
- [`KT-24278`](https://youtrack.jetbrains.com/issue/KT-24278) Hard-code to kotlin compiler annotation for android library migration
- [`KT-24472`](https://youtrack.jetbrains.com/issue/KT-24472) Support argfiles in kotlinc with -Xargfile
- [`KT-24593`](https://youtrack.jetbrains.com/issue/KT-24593) Support -XXLanguage:{+|-}LanguageFeature compiler arguments to enable/disable specific features
- [`KT-24637`](https://youtrack.jetbrains.com/issue/KT-24637) Introduce "progressive" mode of compiler

#### Backend. JS

- [`KT-23094`](https://youtrack.jetbrains.com/issue/KT-23094) JS compiler: Delegation fails to pass the continuation parameter to child suspend function
- [`KT-23582`](https://youtrack.jetbrains.com/issue/KT-23582) JS: Fails to inline, produces bad code
- [`KT-24335`](https://youtrack.jetbrains.com/issue/KT-24335) JS: Invalid implement of external interface

#### Backend. JVM

- [`KT-12330`](https://youtrack.jetbrains.com/issue/KT-12330) Slightly improve generated bytecode for data class equals/hashCode methods
- [`KT-18576`](https://youtrack.jetbrains.com/issue/KT-18576) Debugger fails to show decomposed suspend lambda parameters
- [`KT-22063`](https://youtrack.jetbrains.com/issue/KT-22063) Add intrinsics for javaObjectType and javaPrimitiveType
- [`KT-23402`](https://youtrack.jetbrains.com/issue/KT-23402) Internal error: Couldn't inline method call because the compiler couldn't obtain compiled body for inline function with reified type parameter
- [`KT-23704`](https://youtrack.jetbrains.com/issue/KT-23704) Unstable `checkExpressionValueIsNotNull()` generation in bytecode
- [`KT-23707`](https://youtrack.jetbrains.com/issue/KT-23707) Unstable bridge generation order
- [`KT-23857`](https://youtrack.jetbrains.com/issue/KT-23857) Annotation with target TYPE is not applicable to TYPE_USE in Java sources
- [`KT-23910`](https://youtrack.jetbrains.com/issue/KT-23910) @JvmOverloads doesn't work with default arguments in common code
- [`KT-24427`](https://youtrack.jetbrains.com/issue/KT-24427) Protected function having toArray-like signature from collection becomes public in bytecode
- [`KT-24661`](https://youtrack.jetbrains.com/issue/KT-24661) Support binary compatibility mode for @JvmDefault

#### Frontend

- [`KT-21129`](https://youtrack.jetbrains.com/issue/KT-21129) Unused parameter in property setter is not reported
- [`KT-21157`](https://youtrack.jetbrains.com/issue/KT-21157) Kotlin script: engine can take forever to eval certain code after several times
- [`KT-22740`](https://youtrack.jetbrains.com/issue/KT-22740) REPL slows down during extensions compiling
- [`KT-23124`](https://youtrack.jetbrains.com/issue/KT-23124) Kotlin multiplatform project causes IntelliJ build errors
- [`KT-23209`](https://youtrack.jetbrains.com/issue/KT-23209) Compiler throwing frontend exception
- [`KT-23589`](https://youtrack.jetbrains.com/issue/KT-23589) Report a warning on local annotation classes
- [`KT-23760`](https://youtrack.jetbrains.com/issue/KT-23760) Unable to implement common interface with fun member function with typealiased parameter

### Android

- [`KT-23244`](https://youtrack.jetbrains.com/issue/KT-23244) Option to Disable View Binding generation in Kotlin Android Extensions Plugin

### IDE

- [`KT-8407`](https://youtrack.jetbrains.com/issue/KT-8407) TestNG: running tests from context creates new run configuration every time
- [`KT-9218`](https://youtrack.jetbrains.com/issue/KT-9218) Searching for compilable files takes too long
- [`KT-15019`](https://youtrack.jetbrains.com/issue/KT-15019) Editor: `args` reference in .kts file is red
- [`KT-18769`](https://youtrack.jetbrains.com/issue/KT-18769) Expand Selection on opening curly brace should select the entire block right away
- [`KT-19055`](https://youtrack.jetbrains.com/issue/KT-19055) Idea hangs on copy-paste big Kotlin files
- [`KT-20605`](https://youtrack.jetbrains.com/issue/KT-20605) Unresolved reference on instance from common module function
- [`KT-20824`](https://youtrack.jetbrains.com/issue/KT-20824) Type mismatch for common function taking a non-mapped Kotlin's expected class from stdlib-common, with actual typealias on JVM
- [`KT-20897`](https://youtrack.jetbrains.com/issue/KT-20897) Can't navigate to declaration after PsiInvalidElementAccessException exception
- [`KT-22527`](https://youtrack.jetbrains.com/issue/KT-22527) Kotlin UAST does not evaluate values inside delegation expressions
- [`KT-22868`](https://youtrack.jetbrains.com/issue/KT-22868) Implementing an `expected class` declaration using `actual typealias` produces "good code that is red"
- [`KT-22922`](https://youtrack.jetbrains.com/issue/KT-22922) Override Members should add experimental annotation when required
- [`KT-23384`](https://youtrack.jetbrains.com/issue/KT-23384) Hotspot in org.jetbrains.kotlin.idea.caches.resolve.IDELightClassGenerationSupport.getKotlinInternalClasses(FqName, GlobalSearchScope) IDELightClassGenerationSupport.kt ?
- [`KT-23408`](https://youtrack.jetbrains.com/issue/KT-23408) Don't render @NonNull and @Nullable annotations in parameter info for Java methods
- [`KT-23557`](https://youtrack.jetbrains.com/issue/KT-23557) Expression Bodies should have implicit `return` in Uast
- [`KT-23745`](https://youtrack.jetbrains.com/issue/KT-23745) Unable to implement common interface
- [`KT-23746`](https://youtrack.jetbrains.com/issue/KT-23746) Logger$EmptyThrowable "[kts] cannot find a valid script definition annotation on the class class ScriptTemplateWithArgs" with LivePlugin enabled
- [`KT-23975`](https://youtrack.jetbrains.com/issue/KT-23975) Move Kotlin internal actions under Idea Internal actions menu
- [`KT-24268`](https://youtrack.jetbrains.com/issue/KT-24268) Other main menu item
- [`KT-24438`](https://youtrack.jetbrains.com/issue/KT-24438) ISE “The provided plugin org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar is not compatible with this version of compiler” after rebuilding simple Gradle-based project via JPS.

#### IDE. Configuration

- [`KT-10935`](https://youtrack.jetbrains.com/issue/KT-10935) Add menu entry to create new kotlin .kts scripts
- [`KT-20511`](https://youtrack.jetbrains.com/issue/KT-20511) Library added from maven (using IDEA UI) is not detected as Kotlin/JS library (since type="repository")
- [`KT-20665`](https://youtrack.jetbrains.com/issue/KT-20665) Kotlin Gradle script created by New Project/Module wizard fails with Gradle 4.1+
- [`KT-21844`](https://youtrack.jetbrains.com/issue/KT-21844) Create Kotlin class dialog: make class abstract automatically
- [`KT-22305`](https://youtrack.jetbrains.com/issue/KT-22305) Language and API versions of Kotlin compiler are “Latest” by default in some ways of creating new project
- [`KT-23261`](https://youtrack.jetbrains.com/issue/KT-23261) New MPP design: please show popup with error message if module name is not set
- [`KT-23638`](https://youtrack.jetbrains.com/issue/KT-23638) Kotlin plugin breaks project opening for PhpStorm/WebStorm
- [`KT-23658`](https://youtrack.jetbrains.com/issue/KT-23658) Unclear options “Gradle” and “Gradle (Javascript)” on configuring Kotlin in Gradle- and Maven-based projects
- [`KT-23845`](https://youtrack.jetbrains.com/issue/KT-23845) IntelliJ Maven Plugin does not pass javaParameters option to Kotlin facet
- [`KT-23980`](https://youtrack.jetbrains.com/issue/KT-23980) Move "Update Channel" from "Configure Kotlin Plugin Updates" to settings
- [`KT-24504`](https://youtrack.jetbrains.com/issue/KT-24504) Existent JPS-based Kotlin/JS module is converted to new format, while New Project wizard and facet manipulations still create old format

#### IDE. Debugger

- [`KT-23886`](https://youtrack.jetbrains.com/issue/KT-23886) Both java and kotlin breakpoints in kotlin files
- [`KT-24136`](https://youtrack.jetbrains.com/issue/KT-24136) Debugger: update drop-down menu for the line with lambdas

#### IDE. Editing

- [`KT-2582`](https://youtrack.jetbrains.com/issue/KT-2582) When user inputs triple quote, add matching triple quote automatically
- [`KT-5206`](https://youtrack.jetbrains.com/issue/KT-5206) Long lists of arguments are not foldable
- [`KT-23457`](https://youtrack.jetbrains.com/issue/KT-23457) Auto-import and Import quick fix do not suggest classes from common module [Common test can't find class with word `Abstract` in name.]
- [`KT-23235`](https://youtrack.jetbrains.com/issue/KT-23235) Super slow editing with auto imports enabled

#### IDE. Gradle

- [`KT-23234`](https://youtrack.jetbrains.com/issue/KT-23234) Test names for tests containing inner classes are sporadically reported to teamcity runs.
- [`KT-23383`](https://youtrack.jetbrains.com/issue/KT-23383) Optional plugin dependency for kotlin gradle plugin 'java' subsystem dependent features
- [`KT-22588`](https://youtrack.jetbrains.com/issue/KT-22588) Resolver for 'project source roots and libraries for platform JVM' does not know how to resolve on Gradle Kotlin DSL project without Java and Kotlin
- [`KT-23616`](https://youtrack.jetbrains.com/issue/KT-23616) Synchronize script dependencies not at Gradle Sync
- [`KT-24444`](https://youtrack.jetbrains.com/issue/KT-24444) Do not store proxy objects from Gradle importer in the project model
- [`KT-24586`](https://youtrack.jetbrains.com/issue/KT-24586) MVNFE “Cannot resolve external dependency org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.2.41 because no repositories are defined.” on creating Gradle project with Kotlin only (probably due to lack of repositories block)
- [`KT-24671`](https://youtrack.jetbrains.com/issue/KT-24671) dependencies missed in buildscript block after creating new Gradle-based project in 173 IDEA

#### IDE. Inspections and Intentions

##### New Features

- [`KT-7822`](https://youtrack.jetbrains.com/issue/KT-7822) Convert foreach to for loop should place caret on the variable declaration
- [`KT-9943`](https://youtrack.jetbrains.com/issue/KT-9943) Quick fix/Intention to indent a raw string
- [`KT-15063`](https://youtrack.jetbrains.com/issue/KT-15063) Inspection for coroutine: unused Deferred result
- [`KT-16085`](https://youtrack.jetbrains.com/issue/KT-16085) Inspection "main should return Unit"
- [`KT-20305`](https://youtrack.jetbrains.com/issue/KT-20305) Inspection: Refactor sealed sub-class to object
- [`KT-21413`](https://youtrack.jetbrains.com/issue/KT-21413) Missing inspection: parentheses can be deleted when the only constructor parameter is a function not existing
- [`KT-23137`](https://youtrack.jetbrains.com/issue/KT-23137) Intention for converting to block comment and vise versa
- [`KT-23266`](https://youtrack.jetbrains.com/issue/KT-23266) Add intention(s) to put arguments / parameters on one line
- [`KT-23419`](https://youtrack.jetbrains.com/issue/KT-23419) Intention to replace vararg with array and vice versa
- [`KT-23617`](https://youtrack.jetbrains.com/issue/KT-23617) Add inspection: redundant internal in local anonymous object / class
- [`KT-23775`](https://youtrack.jetbrains.com/issue/KT-23775) IntelliJ plugin: improve "accessor call that can be replaced with property"
- [`KT-24235`](https://youtrack.jetbrains.com/issue/KT-24235) Inspection to replace async.await with withContext
- [`KT-24263`](https://youtrack.jetbrains.com/issue/KT-24263) Add `Make variable immutable` quickfix for const
- [`KT-24433`](https://youtrack.jetbrains.com/issue/KT-24433) Inspection for coroutines: unused async result

##### Performance Improvements

- [`KT-23566`](https://youtrack.jetbrains.com/issue/KT-23566) "Can be private" works on ResolutionResultsCache.kt (from Kotlin project) enormously slow

##### Fixes

- [`KT-6364`](https://youtrack.jetbrains.com/issue/KT-6364) Incorrect quick-fixes are suggested for nullable extension function call
- [`KT-11156`](https://youtrack.jetbrains.com/issue/KT-11156) Incorrect highlighting for nested class in "Redundant SAM-constructor" inspection
- [`KT-11427`](https://youtrack.jetbrains.com/issue/KT-11427) "Replace if with when" does not take break / continue into account
- [`KT-11740`](https://youtrack.jetbrains.com/issue/KT-11740) Invert if condition intention should not remove line breaks
- [`KT-12042`](https://youtrack.jetbrains.com/issue/KT-12042) "Merge with next when" is not applicable when the statements delimited by semicolon or comment
- [`KT-12168`](https://youtrack.jetbrains.com/issue/KT-12168) "Remove explicit type specification" intention produce incompilable code in case of function type
- [`KT-14391`](https://youtrack.jetbrains.com/issue/KT-14391) RemoveUnnecessaryParenthesesIntention lost comment on closing parenthesis
- [`KT-14556`](https://youtrack.jetbrains.com/issue/KT-14556) Quickfix to suggest use of spread operator does not work with mapOf
- [`KT-15195`](https://youtrack.jetbrains.com/issue/KT-15195) Redundant parentheses shouldn't be reported if lambda is not on the same line
- [`KT-16770`](https://youtrack.jetbrains.com/issue/KT-16770) Change type of function quickfix does not propose most idiomatic solutions
- [`KT-19629`](https://youtrack.jetbrains.com/issue/KT-19629) "Convert to primary constructor" quick fix should not move  `init{...}` section down
- [`KT-20123`](https://youtrack.jetbrains.com/issue/KT-20123) Kotlin Gradle script: “Refactoring cannot be performed. Cannot modify build.gradle.kts” for some refactorings and intentions
- [`KT-20332`](https://youtrack.jetbrains.com/issue/KT-20332) Unused property declaration suppression by annotation doesn't work if annotation is targeted to getter
- [`KT-21878`](https://youtrack.jetbrains.com/issue/KT-21878) "arrayOf() call can be replaced by array litteral [...]" quick fix inserts extra parentheses
- [`KT-22092`](https://youtrack.jetbrains.com/issue/KT-22092) Intention "Specify return type explicitly": Propose types from overriden declarations
- [`KT-22615`](https://youtrack.jetbrains.com/issue/KT-22615) "Replace with" intention does not work for equal names
- [`KT-22632`](https://youtrack.jetbrains.com/issue/KT-22632) Gutter icon "go to actual declaration" is absent for enum values on actual side 
- [`KT-22741`](https://youtrack.jetbrains.com/issue/KT-22741) Wrong suggestion for `Replace 'if' expression with elvis expression`
- [`KT-22831`](https://youtrack.jetbrains.com/issue/KT-22831) Inspection for converting to elvis operator does not work for local vars
- [`KT-22860`](https://youtrack.jetbrains.com/issue/KT-22860) "Add annotation target" quick fix does not take into account existent annotations in Java source
- [`KT-22918`](https://youtrack.jetbrains.com/issue/KT-22918) Create interface quickfix is missing 'current class' container
- [`KT-23133`](https://youtrack.jetbrains.com/issue/KT-23133) "Remove redundant calls of the conversion method" wrongly shown for Boolan to Int conversion
- [`KT-23167`](https://youtrack.jetbrains.com/issue/KT-23167) Report "use expression body" also on left brace
- [`KT-23194`](https://youtrack.jetbrains.com/issue/KT-23194) Inspection "map.put() should be converted to assignment" leads to red code in case of labled return
- [`KT-23303`](https://youtrack.jetbrains.com/issue/KT-23303) "Might be const" inspection does not check explicit type specification
- [`KT-23320`](https://youtrack.jetbrains.com/issue/KT-23320) Quick fix to add constructor invocation doesn't work for sealed classes
- [`KT-23321`](https://youtrack.jetbrains.com/issue/KT-23321) Intention to move type to separate file shouldn't be available for sealed classes
- [`KT-23346`](https://youtrack.jetbrains.com/issue/KT-23346) Lift Assignment quick fix incorrectly processes block assignments
- [`KT-23377`](https://youtrack.jetbrains.com/issue/KT-23377) Simplify boolean expression produces incorrect results when mixing nullable and non-nullable variables
- [`KT-23465`](https://youtrack.jetbrains.com/issue/KT-23465) False positive `suspicious callable reference` on lambda invoke with parameters
- [`KT-23511`](https://youtrack.jetbrains.com/issue/KT-23511) "Remove parameter" quick fix makes generic function call incompilable when type could be inferred from removed parameter only
- [`KT-23513`](https://youtrack.jetbrains.com/issue/KT-23513) "Remove parameter" quick fix makes caret jump to the top of the editor
- [`KT-23559`](https://youtrack.jetbrains.com/issue/KT-23559) Wrong hint text for "assignment can be replaced with operator assignment"
- [`KT-23608`](https://youtrack.jetbrains.com/issue/KT-23608) AE “Failed to create expression from text” after applying quick fix “Convert too long character literal to string”
- [`KT-23620`](https://youtrack.jetbrains.com/issue/KT-23620)  False positive `Redundant Companion reference` on calling object from companion
- [`KT-23634`](https://youtrack.jetbrains.com/issue/KT-23634) 'Add use-site target' intention drops annotation arguments
- [`KT-23753`](https://youtrack.jetbrains.com/issue/KT-23753) "Remove variable" quick fix should not remove comment
- [`KT-23756`](https://youtrack.jetbrains.com/issue/KT-23756) Bogus "Might be const" warning in object expression
- [`KT-23778`](https://youtrack.jetbrains.com/issue/KT-23778) "Convert function to property" intention shows broken warning
- [`KT-23796`](https://youtrack.jetbrains.com/issue/KT-23796) "Create extension function/property" quick fix suggests one for nullable type while creates for not-null
- [`KT-23801`](https://youtrack.jetbrains.com/issue/KT-23801) "Convert to constructor" (IntelliJ) quick fix uses wrong use-site target for annotating properties
- [`KT-23977`](https://youtrack.jetbrains.com/issue/KT-23977) wrong hint Unit redundant
- [`KT-24066`](https://youtrack.jetbrains.com/issue/KT-24066) 'Remove redundant Unit' false positive when Unit is returned as Any
- [`KT-24165`](https://youtrack.jetbrains.com/issue/KT-24165) @Deprecated ReplaceWith Constant gets replaced with nothing
- [`KT-24207`](https://youtrack.jetbrains.com/issue/KT-24207) Add parameter intent/red bulb should use auto casted type.
- [`KT-24215`](https://youtrack.jetbrains.com/issue/KT-24215) ReplaceWith produces broken code for lambda following default parameter

#### IDE. Multiplatform

- [`KT-20406`](https://youtrack.jetbrains.com/issue/KT-20406) Overload resolution ambiguity in IDE on expect class / actual typealias from kotlin-stdlib-common / kotlin-stdlib
- [`KT-24316`](https://youtrack.jetbrains.com/issue/KT-24316) Missing dependencies in Kotlin MPP when using gradle composite builds

#### IDE. Navigation

- [`KT-7622`](https://youtrack.jetbrains.com/issue/KT-7622) Searching usages of a field/constructor parameter in a private class seems to scan through the whole project
- [`KT-23182`](https://youtrack.jetbrains.com/issue/KT-23182) Find Usages checks whether there are unused variables in functions which contain search result candidates
- [`KT-23223`](https://youtrack.jetbrains.com/issue/KT-23223) Navigate to actual declaration from actual usage

#### IDE. Refactorings

- [`KT-12078`](https://youtrack.jetbrains.com/issue/KT-12078) Introduce Variable adds explicit type when invoked on anonymous object
- [`KT-15517`](https://youtrack.jetbrains.com/issue/KT-15517) Change signature refactoring shows confusing warning dialog
- [`KT-22387`](https://youtrack.jetbrains.com/issue/KT-22387) Change signature reports "Type cannot be resolved" for class from different package
- [`KT-22669`](https://youtrack.jetbrains.com/issue/KT-22669) Refactor / Copy Kotlin source to plain text causes CCE: "PsiPlainTextFileImpl cannot be cast to KtFile" at CopyKotlinDeclarationsHandler$doCopy$2$1$1.invoke()
- [`KT-22888`](https://youtrack.jetbrains.com/issue/KT-22888) Rename completion cuts off all characters except letters from existent name
- [`KT-23298`](https://youtrack.jetbrains.com/issue/KT-23298) AE: "2 declarations in null..." on rename of a field to `object` or `class`
- [`KT-23563`](https://youtrack.jetbrains.com/issue/KT-23563) null by org.jetbrains.kotlin.idea.refactoring.rename.KotlinMemberInplaceRenameHandler$RenamerImpl exception on trying in-place Rename of non-scratch functions
- [`KT-23613`](https://youtrack.jetbrains.com/issue/KT-23613) Kotlin safe delete processor handles java code when it should not
- [`KT-23644`](https://youtrack.jetbrains.com/issue/KT-23644) Named parameters in generated Kotlin Annotations
- [`KT-23714`](https://youtrack.jetbrains.com/issue/KT-23714) Add Parameter quickfix not working when the called method is in java. 
- [`KT-23838`](https://youtrack.jetbrains.com/issue/KT-23838) Do not search for usages in other files when renaming local variable
- [`KT-24069`](https://youtrack.jetbrains.com/issue/KT-24069) 'Create from usage' doesn't use type info with smart casts

#### IDE. Scratch

- [`KT-6928`](https://youtrack.jetbrains.com/issue/KT-6928) Support Kotlin scratch files
- [`KT-23441`](https://youtrack.jetbrains.com/issue/KT-23441) Scratch options reset on IDE restart
- [`KT-23480`](https://youtrack.jetbrains.com/issue/KT-23480) java.util.NoSuchElementException: "Collection contains no element matching the predicate" on run of a scratch file with unresolved function parameter
- [`KT-23587`](https://youtrack.jetbrains.com/issue/KT-23587) Scratch: references from scratch file aren't taken into account
- [`KT-24016`](https://youtrack.jetbrains.com/issue/KT-24016) Make long scratch output lines readable
- [`KT-24315`](https://youtrack.jetbrains.com/issue/KT-24315) Checkbox labels aren't aligned in scratch panel
- [`KT-24636`](https://youtrack.jetbrains.com/issue/KT-24636) Run Scratch when there are compilation errors in module

#### Tools. J2K

- [`KT-22989`](https://youtrack.jetbrains.com/issue/KT-22989) Exception "Assertion failed: Refactorings should be invoked inside transaction"  on creating UI Component/Notification

### Libraries

- [`KT-10456`](https://youtrack.jetbrains.com/issue/KT-10456) Common Int.toString(radix: Int) method
- [`KT-22298`](https://youtrack.jetbrains.com/issue/KT-22298) Improve docs for Array.copyOf(newSize: Int)
- [`KT-22400`](https://youtrack.jetbrains.com/issue/KT-22400) coroutineContext shall be in kotlin.coroutines.experimental package
- [`KT-23356`](https://youtrack.jetbrains.com/issue/KT-23356) Cross-platform function to convert CharArray slice to String
- [`KT-23920`](https://youtrack.jetbrains.com/issue/KT-23920) CharSequence.trimEnd calls substring instead of subSequence
- [`KT-24353`](https://youtrack.jetbrains.com/issue/KT-24353) Add support for junit 5 in kotlin.test
- [`KT-24371`](https://youtrack.jetbrains.com/issue/KT-24371) Invalid @returns tag does not display in Android Studio popup properly

### Gradle plugin

- [`KT-20214`](https://youtrack.jetbrains.com/issue/KT-20214) NoClassDefFound from Gradle (should report missing tools.jar)
- [`KT-20608`](https://youtrack.jetbrains.com/issue/KT-20608) Cannot reference operator overloads across submodules (.kotlin_module not loaded when a module name has a slash)
- [`KT-22431`](https://youtrack.jetbrains.com/issue/KT-22431) Inter-project incremental compilation does not work with Android plugin 2.3+
- [`KT-22510`](https://youtrack.jetbrains.com/issue/KT-22510) Common sources aren't added when compiling custom source set with Gradle multiplatform plugin
- [`KT-22623`](https://youtrack.jetbrains.com/issue/KT-22623) Kotlin JVM tasks in independent projects are not executed in parallel with Gradle 4.2+ and Kotlin 1.2.20+
- [`KT-23092`](https://youtrack.jetbrains.com/issue/KT-23092) Gradle plugin for MPP common modules should not remove the 'compileJava' task from `project.tasks`
- [`KT-23574`](https://youtrack.jetbrains.com/issue/KT-23574) 'archivesBaseName' does not affect module name in common modules
- [`KT-23719`](https://youtrack.jetbrains.com/issue/KT-23719) Incorrect Gradle Warning for expectedBy in kotlin-platform-android module
- [`KT-23878`](https://youtrack.jetbrains.com/issue/KT-23878) Kapt: Annotation processors are run when formatting is changed
- [`KT-24420`](https://youtrack.jetbrains.com/issue/KT-24420) Kapt plugin: Kapt task has overlapping outputs (and inputs) with Gradle's JavaCompile task
- [`KT-24440`](https://youtrack.jetbrains.com/issue/KT-24440) Gradle daemon OOM due to function descriptors stuck forever

### Tools. kapt

- [`KT-23286`](https://youtrack.jetbrains.com/issue/KT-23286) kapt + nonascii = weird pathes
- [`KT-23427`](https://youtrack.jetbrains.com/issue/KT-23427) kapt: for element with multiple annotations,  annotation values erroneously use default when first annotation uses default
- [`KT-23721`](https://youtrack.jetbrains.com/issue/KT-23721) Warning informing user that 'tools.jar' is absent in the plugin classpath is not show when there is also an error
- [`KT-23898`](https://youtrack.jetbrains.com/issue/KT-23898) Kapt: Do now show a warning for APs from 'annotationProcessor' configuration also declared in 'kapt' configuration
- [`KT-23964`](https://youtrack.jetbrains.com/issue/KT-23964) Kotlin Gradle plugin does not define inputs and outputs of annotation processors

## 1.2.41

### Compiler – Fixes
- [`KT-23901`](https://youtrack.jetbrains.com/issue/KT-23901) Incremental compilation fails on Java 9
- [`KT-23931`](https://youtrack.jetbrains.com/issue/KT-23931) Exception on optimizing eternal loops
- [`KT-23900`](https://youtrack.jetbrains.com/issue/KT-23900) Exception on some cases with nested arrays
- [`KT-23809`](https://youtrack.jetbrains.com/issue/KT-23809) Exception on processing complex hierarchies with `suspend` functions when `-Xdump-declarations-to` is active

### Other
- [`KT-23973`](https://youtrack.jetbrains.com/issue/KT-23973) New compiler behavior lead to ambiguous mappings in Spring Boot temporarily reverted

## 1.2.40

### Compiler

#### New Features

- [`KT-22703`](https://youtrack.jetbrains.com/issue/KT-22703) Allow expect/actual annotation constructors to have default values
- [`KT-19159`](https://youtrack.jetbrains.com/issue/KT-19159) Support `crossinline` lambda parameters of `suspend` function type
- [`KT-21913`](https://youtrack.jetbrains.com/issue/KT-21913) Support default arguments for expected declarations
- [`KT-19120`](https://youtrack.jetbrains.com/issue/KT-19120) Provide extra compiler arguments in `ScriptTemplateDefinition`
- [`KT-19415`](https://youtrack.jetbrains.com/issue/KT-19415) Introduce `@JvmDefault` annotation
- [`KT-21515`](https://youtrack.jetbrains.com/issue/KT-21515) Restrict visibility of classifiers inside `companion object`s

#### Performance Improvements

- [`KT-10057`](https://youtrack.jetbrains.com/issue/KT-10057) Use `lcmp` instruction instead of `kotlin/jvm/internal/Intrinsics.compare`
- [`KT-14258`](https://youtrack.jetbrains.com/issue/KT-14258) Suboptimal codegen for private fieldaccess to private field in companion object
- [`KT-18731`](https://youtrack.jetbrains.com/issue/KT-18731) `==` between enums should use reference equality, not `Intrinsics.areEqual()`.
- [`KT-22714`](https://youtrack.jetbrains.com/issue/KT-22714) Unnecessary checkcast to array of object from an array of specific type
- [`KT-5177`](https://youtrack.jetbrains.com/issue/KT-5177) Optimize code generation for `for` loop with `withIndex()`
- [`KT-19477`](https://youtrack.jetbrains.com/issue/KT-19477) Allow to implement several common modules with a single platform module
- [`KT-21347`](https://youtrack.jetbrains.com/issue/KT-21347) Add compiler warning about using kotlin-stdlib-jre7 or kotlin-stdlib-jre8 artifacts

#### Fixes

- [`KT-16424`](https://youtrack.jetbrains.com/issue/KT-16424) Broken bytecode for nullable generic methods
- [`KT-17171`](https://youtrack.jetbrains.com/issue/KT-17171) `ClassCastException` in case of SAM conversion with `out` variance
- [`KT-19399`](https://youtrack.jetbrains.com/issue/KT-19399) Incorrect bytecode generated for inline functions in some complex cases
- [`KT-21696`](https://youtrack.jetbrains.com/issue/KT-21696) Incorrect warning for use-site target on extension function
- [`KT-22031`](https://youtrack.jetbrains.com/issue/KT-22031) Non-`abstract` expect classes should not have `abstract` members
- [`KT-22260`](https://youtrack.jetbrains.com/issue/KT-22260) Never flag `inline suspend fun` with `NOTHING_TO_INLINE`
- [`KT-22352`](https://youtrack.jetbrains.com/issue/KT-22352) Expect/actual checker can't handle properties and functions with the same name
- [`KT-22652`](https://youtrack.jetbrains.com/issue/KT-22652) Interface with default overrides is not perceived as a SAM
- [`KT-22904`](https://youtrack.jetbrains.com/issue/KT-22904) Incorrect bytecode generated for withIndex iteration on `Array<Int>`
- [`KT-22906`](https://youtrack.jetbrains.com/issue/KT-22906) Invalid class name generated for lambda created from method reference in anonymous object
- [`KT-23044`](https://youtrack.jetbrains.com/issue/KT-23044) Overriden public property with internal setter cannot be found in runtime
- [`KT-23104`](https://youtrack.jetbrains.com/issue/KT-23104) Incorrect code generated for LHS of an intrinsified `in` operator in case of generic type substituted with `Character`
- [`KT-23309`](https://youtrack.jetbrains.com/issue/KT-23309) Minor spelling errors in JVM internal error messages
- [`KT-22001`](https://youtrack.jetbrains.com/issue/KT-22001) JS: compiler crashes on += with "complex" receiver
- [`KT-23239`](https://youtrack.jetbrains.com/issue/KT-23239) JS: Default arguments for non-final member function support is missing for MPP
- [`KT-17091`](https://youtrack.jetbrains.com/issue/KT-17091) Converting to SAM Java type appends non-deterministic hash to class name
- [`KT-21521`](https://youtrack.jetbrains.com/issue/KT-21521) Compilation exception when trying to compile a `suspend` function with `tailrec` keyword
- [`KT-21605`](https://youtrack.jetbrains.com/issue/KT-21605) Cross-inlined coroutine with captured outer receiver creates unverifiable code
- [`KT-21864`](https://youtrack.jetbrains.com/issue/KT-21864) Expect-actual matcher doesn't consider generic upper bounds
- [`KT-21906`](https://youtrack.jetbrains.com/issue/KT-21906) `ACTUAL_MISSING` is reported for actual constructor of non-actual class
- [`KT-21939`](https://youtrack.jetbrains.com/issue/KT-21939) Improve `ACTUAL_MISSING` diagnostics message
- [`KT-22513`](https://youtrack.jetbrains.com/issue/KT-22513) Flaky "JarURLConnection.getUseCaches" NPE during compilation when using compiler plugins

### Libraries

- [`KT-11208`](https://youtrack.jetbrains.com/issue/KT-11208) `readLine()` shouldn't use buffered reader

### IDE

#### New Features

- [`KT-10368`](https://youtrack.jetbrains.com/issue/KT-10368) Run Action for Kotlin Scratch Files
- [`KT-16892`](https://youtrack.jetbrains.com/issue/KT-16892) Shortcut to navigate between header and impl
- [`KT-23005`](https://youtrack.jetbrains.com/issue/KT-23005) Support `prefix`/`suffix` attributes for language injection in Kotlin with annotations and comments

#### Performance Improvements

- [`KT-19484`](https://youtrack.jetbrains.com/issue/KT-19484) KotlinBinaryClassCache retains a lot of memory
- [`KT-23183`](https://youtrack.jetbrains.com/issue/KT-23183) `ConfigureKotlinNotification.getNotificationString()` scans modules with Kotlin files twice
- [`KT-23380`](https://youtrack.jetbrains.com/issue/KT-23380) Improve IDE performance when working with Spring projects

#### Fixes

- [`KT-15482`](https://youtrack.jetbrains.com/issue/KT-15482) `KotlinNullPointerException` in IDE from expected class with nested class
- [`KT-15739`](https://youtrack.jetbrains.com/issue/KT-15739) Internal visibility across common and platform-dependent modules
- [`KT-19025`](https://youtrack.jetbrains.com/issue/KT-19025) Not imported `build.gradle.kts` is all red
- [`KT-19165`](https://youtrack.jetbrains.com/issue/KT-19165) IntelliJ should suggest to reload Gradle projects when `build.gradle.kts` changes
- [`KT-20282`](https://youtrack.jetbrains.com/issue/KT-20282) 'Move statement up' works incorrectly for statement after `finally` block if `try` block contains closure
- [`KT-20521`](https://youtrack.jetbrains.com/issue/KT-20521) Kotlin Gradle script: valid `build.gradle.kts` is red and becomes normal only after reopening the project
- [`KT-20592`](https://youtrack.jetbrains.com/issue/KT-20592) `KotlinNullPointerException`: nested class inside expect / actual interface
- [`KT-21013`](https://youtrack.jetbrains.com/issue/KT-21013) "Move statement up/down" fails for multiline declarations
- [`KT-21420`](https://youtrack.jetbrains.com/issue/KT-21420) `.gradle.kts` editor should do no semantic highlighting until the first successful dependency resolver response
- [`KT-21683`](https://youtrack.jetbrains.com/issue/KT-21683) Language injection: JPAQL. Injection should be present for "query" parameter of `@NamedNativeQueries`
- [`KT-21745`](https://youtrack.jetbrains.com/issue/KT-21745) Warning and quickfix about kotlin-stdlib-jre7/8 -> kotlin-stdlib-jdk7/8 in Maven
- [`KT-21746`](https://youtrack.jetbrains.com/issue/KT-21746) Warning and quickfix about kotlin-stdlib-jre7/8 -> kotlin-stdlib-jdk7/8 in Gradle
- [`KT-21753`](https://youtrack.jetbrains.com/issue/KT-21753) Language injection: SpEL. Not injected for key in `@Caching`
- [`KT-21771`](https://youtrack.jetbrains.com/issue/KT-21771) All annotations in `Annotations.kt` from kotlin-test-js module wrongly have ACTUAL_MISSING
- [`KT-21831`](https://youtrack.jetbrains.com/issue/KT-21831) Opening class from `kotlin-stdlib-jdk8.jar` fails with EE: "Stub list in ... length differs from PSI"
- [`KT-22229`](https://youtrack.jetbrains.com/issue/KT-22229) Kotlin local delegated property Import auto-removed with "Java: Optimize imports on the fly"
- [`KT-22724`](https://youtrack.jetbrains.com/issue/KT-22724) ISE: "psiFile must not be null" at `KotlinNodeJsRunConfigurationProducer.setupConfigurationFromContext()`
- [`KT-22817`](https://youtrack.jetbrains.com/issue/KT-22817) Hitting 'Propagate Parameters' in Change Signature throws `UnsupportedOperationException` 
- [`KT-22851`](https://youtrack.jetbrains.com/issue/KT-22851) Apply button is always active on Kotlin compiler settings tab
- [`KT-22858`](https://youtrack.jetbrains.com/issue/KT-22858) Multiplatform: String constructor parameter is reported in Java file of jvm module on creation of a new instance of a class from common module
- [`KT-22865`](https://youtrack.jetbrains.com/issue/KT-22865) Support multiple expectedBy dependencies when importing project from Gradle or Maven
- [`KT-22873`](https://youtrack.jetbrains.com/issue/KT-22873) Common module-based light classes do not see JDK
- [`KT-22874`](https://youtrack.jetbrains.com/issue/KT-22874) Exception on surround with "if else" when resulting if should be wrapped with `()`
- [`KT-22925`](https://youtrack.jetbrains.com/issue/KT-22925) Unable to view Type Hierarchy from constructor call in expression
- [`KT-22926`](https://youtrack.jetbrains.com/issue/KT-22926) Confusing behavior of Type Hierarchy depending on the caret position at superclass constructor
- [`KT-23097`](https://youtrack.jetbrains.com/issue/KT-23097) Enhance multiplatform project wizard
- [`KT-23271`](https://youtrack.jetbrains.com/issue/KT-23271) Warn about using kotlin-stdlib-jre* libs in `dependencyManagement` section in Maven with `eap` and `dev` Kotlin versions
- [`KT-20672`](https://youtrack.jetbrains.com/issue/KT-20672) IDE can't resolve references to elements from files with `@JvmPackageName`
- [`KT-23546`](https://youtrack.jetbrains.com/issue/KT-23546) Variable name auto-completion popup gets in the way
- [`KT-23546`](https://youtrack.jetbrains.com/issue/KT-23546) Do not show duplicated names in variables completion list
- [`KT-19120`](https://youtrack.jetbrains.com/issue/KT-19120) Use script compiler options on script dependencies in the IDE as well

### IDE. Gradle. Script

- [`KT-23228`](https://youtrack.jetbrains.com/issue/KT-23228) Do not highlight `.gradle.kts` files in non-Gradle projects

### IDE. Inspections and Intentions

#### New Features

- [`KT-16382`](https://youtrack.jetbrains.com/issue/KT-16382) Intention to convert `expr.unsafeCast<Type>()` to `expr as Type` and vice versa
- [`KT-20439`](https://youtrack.jetbrains.com/issue/KT-20439) Intentions to add/remove labeled return to last expression in a lambda
- [`KT-22011`](https://youtrack.jetbrains.com/issue/KT-22011) Inspection to report the usage of Java Collections methods on immutable Kotlin Collections 
- [`KT-22933`](https://youtrack.jetbrains.com/issue/KT-22933) Intention/inspection to convert Pair constructor to `to` function
- [`KT-19871`](https://youtrack.jetbrains.com/issue/KT-19871) Intentions for specifying use-site targets for an annotation
- [`KT-22971`](https://youtrack.jetbrains.com/issue/KT-22971) Inspection to highlight and remove unnecessary explicit companion object references

#### Fixes

- [`KT-12226`](https://youtrack.jetbrains.com/issue/KT-12226) "Convert concatenation to template" does not process `$` sign as a Char
- [`KT-15858`](https://youtrack.jetbrains.com/issue/KT-15858) "Replace with a `foreach` function call" intention breaks code
- [`KT-16332`](https://youtrack.jetbrains.com/issue/KT-16332) Add braces to 'if' statement intention does not put end-of-line comment properly into braces
- [`KT-17058`](https://youtrack.jetbrains.com/issue/KT-17058) "Create implementations from headers": each implementation gets own file
- [`KT-17306`](https://youtrack.jetbrains.com/issue/KT-17306) Don't report package name mismatch if there's no Java code in the module
- [`KT-19730`](https://youtrack.jetbrains.com/issue/KT-19730) Quickfix for delegated properties boilerplate generation doesn't work on locals
- [`KT-21005`](https://youtrack.jetbrains.com/issue/KT-21005) "Missing KDoc inspection" is broken
- [`KT-21082`](https://youtrack.jetbrains.com/issue/KT-21082) "Create actual declaration" of top-level subclass of expected `sealed class` in the same file as actual declaration of sealed class present
- [`KT-22110`](https://youtrack.jetbrains.com/issue/KT-22110) "Can be joined with assignment" inspection underlining extends into comment
- [`KT-22329`](https://youtrack.jetbrains.com/issue/KT-22329) "Create class" quickfix is not suggested in `when` branch
- [`KT-22428`](https://youtrack.jetbrains.com/issue/KT-22428) Create member function from usage shouldn't present type parameters as options
- [`KT-22492`](https://youtrack.jetbrains.com/issue/KT-22492) "Specify explicit lambda signature" intention is available only on lambda braces
- [`KT-22719`](https://youtrack.jetbrains.com/issue/KT-22719) Incorrect warning 'Redundant semicolon' when having method call before lambda expression
- [`KT-22861`](https://youtrack.jetbrains.com/issue/KT-22861) "Add annotation target" quickfix is not available on annotation with use site target
- [`KT-22862`](https://youtrack.jetbrains.com/issue/KT-22862) "Add annotation target" quickfix does not process existent annotations with use site target
- [`KT-22917`](https://youtrack.jetbrains.com/issue/KT-22917) Update order of containers for `create class` quickfix 
- [`KT-22949`](https://youtrack.jetbrains.com/issue/KT-22949) NPE on conversion of `run`/`apply` with explicit lambda signature to `let`/`also`
- [`KT-22950`](https://youtrack.jetbrains.com/issue/KT-22950) Convert stdlib extension function to scoping function works incorrectly in case of explicit lambda signature
- [`KT-22954`](https://youtrack.jetbrains.com/issue/KT-22954) "Sort modifiers" quickfix works incorrectly when method is annotated
- [`KT-22970`](https://youtrack.jetbrains.com/issue/KT-22970) Add explicit this intention/inspection missing for lambda invocation
- [`KT-23109`](https://youtrack.jetbrains.com/issue/KT-23109) "Remove redundant 'if' statement" inspection breaks code with labeled return 
- [`KT-23215`](https://youtrack.jetbrains.com/issue/KT-23215) "Add function to supertype" quickfix works incorrectly
- [`KT-14270`](https://youtrack.jetbrains.com/issue/KT-14270) Intentions "Add/Remove braces" should be applied to the statement where caret is if there several nested statements one into another
- [`KT-21743`](https://youtrack.jetbrains.com/issue/KT-21743) Method reference not correctly moved into parentheses
- [`KT-23045`](https://youtrack.jetbrains.com/issue/KT-23045) AE “Failed to create expression from text” on concatenating string with broken quote mark char literal
- [`KT-23046`](https://youtrack.jetbrains.com/issue/KT-23046) CCE ”KtBinaryExpression cannot be cast to KtStringTemplateExpression” on concatenating broken quote mark char literal with string
- [`KT-23227`](https://youtrack.jetbrains.com/issue/KT-23227) "Add annotation target" quickfix is not suggested for `field:` use-site target

### IDE. Refactorings

#### Fixes

- [`KT-13255`](https://youtrack.jetbrains.com/issue/KT-13255) Refactor / Rename: renaming local variable or class to existing name gives no warning
- [`KT-13284`](https://youtrack.jetbrains.com/issue/KT-13284) Refactor / Rename: superfluous imports and FQNs in Java using `@JvmOverloads` functions
- [`KT-13907`](https://youtrack.jetbrains.com/issue/KT-13907) Rename refactoring warns about name conflict if there is function with different signature but the same name
- [`KT-13986`](https://youtrack.jetbrains.com/issue/KT-13986) Full qualified names of classes in comments should be changed after class Move, if comment contains backquotes
- [`KT-14671`](https://youtrack.jetbrains.com/issue/KT-14671) `typealias`: refactor/rename should propose to rename occurrences in comments
- [`KT-15039`](https://youtrack.jetbrains.com/issue/KT-15039) Extra usage is found for a parameter in data class in destructuring construction
- [`KT-15228`](https://youtrack.jetbrains.com/issue/KT-15228) Extract function from inline function should create public function
- [`KT-15302`](https://youtrack.jetbrains.com/issue/KT-15302) Reference to typealias in SAM conversion is not found
- [`KT-16510`](https://youtrack.jetbrains.com/issue/KT-16510) Can't rename quoted identifier `is`
- [`KT-17827`](https://youtrack.jetbrains.com/issue/KT-17827) Refactor / Move corrupts bound references when containing class of member element is changed
- [`KT-19561`](https://youtrack.jetbrains.com/issue/KT-19561) Name conflict warning when renaming method to a name matching an extension method with the same name exists
- [`KT-20178`](https://youtrack.jetbrains.com/issue/KT-20178) Refactor → Rename can't make companion object name empty
- [`KT-22282`](https://youtrack.jetbrains.com/issue/KT-22282) Moving a Kotlin file to another package does not change imports in itself
- [`KT-22482`](https://youtrack.jetbrains.com/issue/KT-22482) Rename refactoring insert qualifier for non related property call
- [`KT-22661`](https://youtrack.jetbrains.com/issue/KT-22661) Refactor/Move: top level field reference is not imported automatically after move to the source root
- [`KT-22678`](https://youtrack.jetbrains.com/issue/KT-22678) Refactor / Copy: "Class uses constructor which will be inaccessible after move" when derived class has a protected constructor
- [`KT-22692`](https://youtrack.jetbrains.com/issue/KT-22692) Refactor/Move: unnecessary curly braces added on moving to a separate file a top level function with a top level field usage
- [`KT-22745`](https://youtrack.jetbrains.com/issue/KT-22745) Refactor/Move inserts FQ function name at the call site if there is a field same named as the function
- [`KT-22747`](https://youtrack.jetbrains.com/issue/KT-22747) Moving top-level function to a different (existing) file doesn't update references from Java
- [`KT-22751`](https://youtrack.jetbrains.com/issue/KT-22751) Refactor/Rename: type alias name clash is not reported
- [`KT-22769`](https://youtrack.jetbrains.com/issue/KT-22769) Refactor/Move: there is no warning on moving sealed class or its inheritors to another file
- [`KT-22771`](https://youtrack.jetbrains.com/issue/KT-22771) Refactor/Move: there is no warning on moving nested class to another class with stricter visibility
- [`KT-22812`](https://youtrack.jetbrains.com/issue/KT-22812) Refactor/Rename extension functions incorrectly conflicts with other extension functions
- [`KT-23065`](https://youtrack.jetbrains.com/issue/KT-23065) Refactor/Move: Specify the warning message on moving sealed class inheritors without moving the sealed class itself

### IDE. Script

- [`KT-22647`](https://youtrack.jetbrains.com/issue/KT-22647) Run script Action in IDE should use Kotlin compiler from the IDE plugin
- [`KT-18930`](https://youtrack.jetbrains.com/issue/KT-18930) IDEA is unstable With Gradle Kotlin DSL
- [`KT-21042`](https://youtrack.jetbrains.com/issue/KT-21042) Gradle Script Kotlin project is full-red
- [`KT-11618`](https://youtrack.jetbrains.com/issue/KT-11618) Running .kts file from IntelliJ IDEA doesn't allow to import classes in other files which are also part of the project


### IDE. Debugger

- [`KT-22205`](https://youtrack.jetbrains.com/issue/KT-22205) Breakpoints won't work for Kotlin testing with JUnit

### JavaScript

- [`KT-22019`](https://youtrack.jetbrains.com/issue/KT-22019) Fix wrong list sorting order

### Tools. CLI

- [`KT-22777`](https://youtrack.jetbrains.com/issue/KT-22777) Unstable language version setting has no effect when attached runtime has lower version

### Tools. Gradle

- [`KT-22824`](https://youtrack.jetbrains.com/issue/KT-22824) `expectedBy` dependency should be expressed as `compile` dependency in POM
- [`KT-15371`](https://youtrack.jetbrains.com/issue/KT-15371) Multiplatform: setting free compiler args can break build
- [`KT-22864`](https://youtrack.jetbrains.com/issue/KT-22864) Allow multiple expectedBy configuration dependencies in Gradle
- [`KT-22895`](https://youtrack.jetbrains.com/issue/KT-22895) 'kotlin-runtime' library is missing in the compiler classpath sometimes
- [`KT-23085`](https://youtrack.jetbrains.com/issue/KT-23085) Use proper names for the Gradle task inputs/outputs added at runtime
- [`KT-23694`](https://youtrack.jetbrains.com/issue/KT-23694) Fix parallel build in Kotlin IC – invalid KotlinCoreEnvironment disposal

### Tools. Android
- Android Extensions: Support fragments from kotlinx package;

### Tools. Incremental Compile

- [`KT-20516`](https://youtrack.jetbrains.com/issue/KT-20516) "Unresolved reference" when project declares same class as its dependency
- [`KT-22542`](https://youtrack.jetbrains.com/issue/KT-22542) "Source file or directory not found" for incremental compilation with Kobalt
- [`KT-23165`](https://youtrack.jetbrains.com/issue/KT-23165) Incremental compilation is sometimes broken after moving one class

### Tools. JPS

- [`KT-16091`](https://youtrack.jetbrains.com/issue/KT-16091) Incremental compilation ignores changes in Java static field
- [`KT-22995`](https://youtrack.jetbrains.com/issue/KT-22995) EA-91869 - NA: `LookupStorage.<init>`

### Tools. kapt

- [`KT-21735`](https://youtrack.jetbrains.com/issue/KT-21735) Kapt cache was not cleared sometimes

### Tools. REPL

- [`KT-21611`](https://youtrack.jetbrains.com/issue/KT-21611) REPL: Empty lines should be ignored

## 1.2.30

### Android

- [`KT-19300`](https://youtrack.jetbrains.com/issue/KT-19300) [AS3.0] Android extensions, Parcelable: editor shows warning about incomplete implementation on a class with Parcelize annotation
- [`KT-22168`](https://youtrack.jetbrains.com/issue/KT-22168) "Kotlin Android | Illegal Android Identifier" inspection reports non-instrumentation unit tests
- [`KT-22700`](https://youtrack.jetbrains.com/issue/KT-22700) Android Extensions bind views with dot in ID

### Compiler

#### New Features

- [`KT-17336`](https://youtrack.jetbrains.com/issue/KT-17336) Introduce suspendCoroutineUninterceptedOrReturn coroutine intrinsic function
- [`KT-22766`](https://youtrack.jetbrains.com/issue/KT-22766) Imitate "suspend" modifier in 1.2.x by stdlib function

#### Performance Improvements

- [`KT-16880`](https://youtrack.jetbrains.com/issue/KT-16880) Smarter detection of tail-suspending unit invocations
#### Fixes

- [`KT-10494`](https://youtrack.jetbrains.com/issue/KT-10494) IAE in CheckMethodAdapter.checkInternalName when declaring classes inside method with non-standard name
- [`KT-16079`](https://youtrack.jetbrains.com/issue/KT-16079) Internal error when using suspend operator plus
- [`KT-18522`](https://youtrack.jetbrains.com/issue/KT-18522) Internal compiler error with IndexOutOfBoundsException, "Exception while analyzing expression"
- [`KT-18578`](https://youtrack.jetbrains.com/issue/KT-18578) Compilation failure with @JsonInclude and default interface method
- [`KT-19786`](https://youtrack.jetbrains.com/issue/KT-19786) Kotlin — unable to override a Java function with @Nullable vararg argument
- [`KT-20466`](https://youtrack.jetbrains.com/issue/KT-20466) JSR305 false positive for elvis operator
- [`KT-20705`](https://youtrack.jetbrains.com/issue/KT-20705) Tail suspend call optimization doesn't work in when block
- [`KT-20708`](https://youtrack.jetbrains.com/issue/KT-20708) Tail suspend call optiomization doesn't work in some branches
- [`KT-20855`](https://youtrack.jetbrains.com/issue/KT-20855) Unnecessary safe-call reported on nullable type
- [`KT-21165`](https://youtrack.jetbrains.com/issue/KT-21165) Exception from suspending function is not caught
- [`KT-21238`](https://youtrack.jetbrains.com/issue/KT-21238) Nonsensical warning "Expected type does not accept nulls in Java, but the value may be null in Kotlin"
- [`KT-21258`](https://youtrack.jetbrains.com/issue/KT-21258) Raw backing field value exposed via accessors?
- [`KT-21303`](https://youtrack.jetbrains.com/issue/KT-21303) Running on JDK-10-ea-31 leads to ArrayIndexOutOfBoundsException
- [`KT-21642`](https://youtrack.jetbrains.com/issue/KT-21642) Back-end (JVM) Internal error: Couldn't transform method node on using `open` keyword with `suspend` for a top-level function
- [`KT-21759`](https://youtrack.jetbrains.com/issue/KT-21759) Compiler crashes on two subsequent return statements in suspend function
- [`KT-22029`](https://youtrack.jetbrains.com/issue/KT-22029) Fold list to pair with destructuring assignment and inner when results in Exception
- [`KT-22345`](https://youtrack.jetbrains.com/issue/KT-22345) OOM in ReturnUnitMethodReplacer
- [`KT-22410`](https://youtrack.jetbrains.com/issue/KT-22410) invalid compiler optimization for nullable cast to reified type
- [`KT-22577`](https://youtrack.jetbrains.com/issue/KT-22577) Compiler crashes when coroutineContext is used inside of inlined lambda

### IDE

#### New Features

- [`KT-8352`](https://youtrack.jetbrains.com/issue/KT-8352) Pasting Kotlin code into package could create .kt file
- [`KT-16710`](https://youtrack.jetbrains.com/issue/KT-16710) Run configuration to run main() as a Node CLI app
- [`KT-16833`](https://youtrack.jetbrains.com/issue/KT-16833) Allow mixing Java and Kotlin code in "Analyze Data Flow..."
- [`KT-21531`](https://youtrack.jetbrains.com/issue/KT-21531) JS: add support for running specific test from the gutter icon with Jest testing framework
#### Performance Improvements

- [`KT-21450`](https://youtrack.jetbrains.com/issue/KT-21450) Add caching for Module.languageVersionSettings
- [`KT-21517`](https://youtrack.jetbrains.com/issue/KT-21517) OOME during find usages
#### Fixes

- [`KT-7316`](https://youtrack.jetbrains.com/issue/KT-7316) Go to declaration in Kotlin JavaScript project navigates to JDK source in some cases
- [`KT-8563`](https://youtrack.jetbrains.com/issue/KT-8563) Refactor / Rename inserts line breaks without reason
- [`KT-11467`](https://youtrack.jetbrains.com/issue/KT-11467) Editor: `var` property in primary constructor is shown not underscored, same as `val`
- [`KT-13509`](https://youtrack.jetbrains.com/issue/KT-13509) Don't show run line markers for top-level functions annotated with @Test
- [`KT-13971`](https://youtrack.jetbrains.com/issue/KT-13971) Kotlin Bytecode tool window: Decompile is available for incompilable code, CE at MemberCodegen.genFunctionOrProperty()
- [`KT-15000`](https://youtrack.jetbrains.com/issue/KT-15000) Do not spell check overridden declaration names
- [`KT-15331`](https://youtrack.jetbrains.com/issue/KT-15331) "Kotlin not configured" notification always shown for common module in multiplatform project
- [`KT-16333`](https://youtrack.jetbrains.com/issue/KT-16333) Cannot navigate to super declaration via shortcut
- [`KT-16976`](https://youtrack.jetbrains.com/issue/KT-16976) Introduce special SDK for Kotlin JS projects to avoid using JDK
- [`KT-18445`](https://youtrack.jetbrains.com/issue/KT-18445) multiplatform project: provide more comfortable way to process cases when there are missed method implemenation in the implementation class
- [`KT-19194`](https://youtrack.jetbrains.com/issue/KT-19194) Some Live Templates should probably be enabled also for "expressions" not only "statements"
- [`KT-20281`](https://youtrack.jetbrains.com/issue/KT-20281) multiplatform:Unresolved service JavaDescriptorResolver on a file with several header declarations and gutters not shown
- [`KT-20470`](https://youtrack.jetbrains.com/issue/KT-20470) IntelliJ indent guide/invisible brace matching hint tooltip doesn't show context
- [`KT-20522`](https://youtrack.jetbrains.com/issue/KT-20522) Add "Build" action in "Before launch" block when create new JS run configuration (for test)
- [`KT-20915`](https://youtrack.jetbrains.com/issue/KT-20915) Add quickfix for ‘Implicit (unsafe) cast from dynamic type’
- [`KT-20971`](https://youtrack.jetbrains.com/issue/KT-20971) Cannot navigate to sources of compiled common dependency
- [`KT-21115`](https://youtrack.jetbrains.com/issue/KT-21115) Incomplete actual class should still have navigation icon to expect class
- [`KT-21688`](https://youtrack.jetbrains.com/issue/KT-21688) UIdentifier violates JvmDeclarationElement contract
- [`KT-21874`](https://youtrack.jetbrains.com/issue/KT-21874) Unexpected IDE error "Unknown type [typealias ...]"
- [`KT-21958`](https://youtrack.jetbrains.com/issue/KT-21958) Support "Alternative source available" for Kotlin files
- [`KT-21994`](https://youtrack.jetbrains.com/issue/KT-21994) Collapsed comments containing `*` get removed in the summary line.
- [`KT-22179`](https://youtrack.jetbrains.com/issue/KT-22179) For properties overridden in object literals, navigation to inherited properties is missing indication of a type they are overridden
- [`KT-22214`](https://youtrack.jetbrains.com/issue/KT-22214) Front-end Internal error: Failed to analyze declaration
- [`KT-22230`](https://youtrack.jetbrains.com/issue/KT-22230) Reformatting code to Kotlin style indents top-level typealiases with comments
- [`KT-22242`](https://youtrack.jetbrains.com/issue/KT-22242) Semantic highlighting uses different colors for the same 'it' variable and same color for different 'it's
- [`KT-22301`](https://youtrack.jetbrains.com/issue/KT-22301) Don't require space after label for lambda
- [`KT-22346`](https://youtrack.jetbrains.com/issue/KT-22346) Incorrect indentation for chained context extension functions (lambdas) when using Kotlin style guide
- [`KT-22356`](https://youtrack.jetbrains.com/issue/KT-22356) Update status of inspection "Kotlin JVM compiler configured but no stdlib dependency" after pom file update, not on re-import
- [`KT-22360`](https://youtrack.jetbrains.com/issue/KT-22360) MPP: with "Create separate module per source set" = No `expectedBy` dependency is imported not transitively
- [`KT-22374`](https://youtrack.jetbrains.com/issue/KT-22374) "Join lines" works incorrectly in case of line containing more than one string literal
- [`KT-22473`](https://youtrack.jetbrains.com/issue/KT-22473) Regression in IntelliJ Kotlin Plugin 1.2.20, settings.gradle.kts script template is wrong
- [`KT-22508`](https://youtrack.jetbrains.com/issue/KT-22508) Auto-formatting should insert an indentation for default parameter values
- [`KT-22514`](https://youtrack.jetbrains.com/issue/KT-22514) IDE Freeze related to IdeAllOpenDeclarationAttributeAltererExtension.getAnnotationFqNames()
- [`KT-22557`](https://youtrack.jetbrains.com/issue/KT-22557) Dead 'Apply' button, when setting code style
- [`KT-22565`](https://youtrack.jetbrains.com/issue/KT-22565) Cant do `PsiAnchor.create` on annotation in annotation
- [`KT-22570`](https://youtrack.jetbrains.com/issue/KT-22570) Can't add import in "Packages to Use Import with '*'" section on "Import" tab in Code Style -> Kotlin
- [`KT-22593`](https://youtrack.jetbrains.com/issue/KT-22593) AE when invoking find usages on constructor in decompiled java file
- [`KT-22641`](https://youtrack.jetbrains.com/issue/KT-22641) Auto-formatting adds extra indent to a closing square bracket on a separate line
- [`KT-22734`](https://youtrack.jetbrains.com/issue/KT-22734) LinkageError: "loader constraint violation: when resolving method PsiTreeUtilKt.parentOfType()" at KotlinConverter.convertPsiElement$uast_kotlin()

### IDE. Debugger

- [`KT-20351`](https://youtrack.jetbrains.com/issue/KT-20351) Stepping over a line with two inline stdlib functions steps into the second function
- [`KT-21312`](https://youtrack.jetbrains.com/issue/KT-21312) Confusing Kotlin (JavaScript) run configuration
- [`KT-21945`](https://youtrack.jetbrains.com/issue/KT-21945) Double stop on same line during step over if inline call is present
- [`KT-22967`](https://youtrack.jetbrains.com/issue/KT-22967) Debugger: Evaluator fails on evaluating huge lambdas on Android

### IDE. Inspections and Intentions

#### New Features

- [`KT-18124`](https://youtrack.jetbrains.com/issue/KT-18124) Inspection to get rid of unnecessary ticks in references
- [`KT-22038`](https://youtrack.jetbrains.com/issue/KT-22038) Inspection to replace the usage of Java Collections methods on subtypes of MutableList with the methods from Kotlin stdlib
- [`KT-22152`](https://youtrack.jetbrains.com/issue/KT-22152) "Create Class" quickfix should support creating the class in a new file and selecting the package for that file
- [`KT-22171`](https://youtrack.jetbrains.com/issue/KT-22171) Add Intention for single character substring
- [`KT-22303`](https://youtrack.jetbrains.com/issue/KT-22303) Inspection to detect `Type!.inlineWithNotNullReceiver()` calls
- [`KT-22409`](https://youtrack.jetbrains.com/issue/KT-22409) Intention for changing property setter accessibility
#### Performance Improvements

- [`KT-21137`](https://youtrack.jetbrains.com/issue/KT-21137) Kotlin instantiates something expensive via reflection when highlighting Java file
#### Fixes

- [`KT-15176`](https://youtrack.jetbrains.com/issue/KT-15176) Remove "Create type alias" intention when called on java class
- [`KT-18007`](https://youtrack.jetbrains.com/issue/KT-18007) Inspection doesn't suggest Maven Plugin for kotlin-stdlib-jre8
- [`KT-18308`](https://youtrack.jetbrains.com/issue/KT-18308) 'Remove braces from else statement' intention breaks code
- [`KT-18912`](https://youtrack.jetbrains.com/issue/KT-18912) multiplatform project: Convert to enum class: header sealed class cannot convert nested objects to enum values
- [`KT-21114`](https://youtrack.jetbrains.com/issue/KT-21114) IOE: create actual members for expected with companion
- [`KT-21600`](https://youtrack.jetbrains.com/issue/KT-21600) `suspend` modifier should go after `override` in overridden suspend functions
- [`KT-21881`](https://youtrack.jetbrains.com/issue/KT-21881) Replace "If" with safe access intention false positive
- [`KT-22054`](https://youtrack.jetbrains.com/issue/KT-22054) Replace '!=' with 'contentEquals' should be replace '==' with 'contentEquals'
- [`KT-22097`](https://youtrack.jetbrains.com/issue/KT-22097) Redundant Unit inspection false positive for single expression function
- [`KT-22159`](https://youtrack.jetbrains.com/issue/KT-22159) "Replace return with 'if' expression" should not place return before expressions of type Nothing
- [`KT-22167`](https://youtrack.jetbrains.com/issue/KT-22167) "Add annotation target" quick fix does nothing and disappears from menu
- [`KT-22221`](https://youtrack.jetbrains.com/issue/KT-22221) QuickFix to remove unused constructor parameters shouldn't delete parenthesis
- [`KT-22335`](https://youtrack.jetbrains.com/issue/KT-22335) IOE from KotlinUnusedImportInspection.scheduleOptimizeImportsOnTheFly
- [`KT-22339`](https://youtrack.jetbrains.com/issue/KT-22339) Remove setter parameter type: error while creating problem descriptor
- [`KT-22364`](https://youtrack.jetbrains.com/issue/KT-22364) Redundant setter is not reported for overridden fields
- [`KT-22484`](https://youtrack.jetbrains.com/issue/KT-22484) The warning highlight for redundant `!is`check for object types isn't extended to the full operator
- [`KT-22538`](https://youtrack.jetbrains.com/issue/KT-22538) "Redundant type checks for object" inspection application breaks smart cast for an object's field or function

### IDE. Refactorings

#### New Features

- [`KT-17047`](https://youtrack.jetbrains.com/issue/KT-17047) Refactorings for related standard "scoping functions" conversion: 'let' <-> 'run', 'apply' <-> 'also'
#### Fixes

- [`KT-12365`](https://youtrack.jetbrains.com/issue/KT-12365) Renaming `invoke` function should remove `operator` modifier and insert function call for implicit usages
- [`KT-17977`](https://youtrack.jetbrains.com/issue/KT-17977) Move class to upper level creates file with wrong file name
- [`KT-21719`](https://youtrack.jetbrains.com/issue/KT-21719) Actual typealias not renamed on expected declaration rename
- [`KT-22200`](https://youtrack.jetbrains.com/issue/KT-22200) Overriden function generated from completion is missing suspend modifier
- [`KT-22359`](https://youtrack.jetbrains.com/issue/KT-22359) Refactor / Rename file: Throwable at RenameProcessor.performRefactoring()
- [`KT-22461`](https://youtrack.jetbrains.com/issue/KT-22461) Rename doesn't work on private top-level members of multi-file parts
- [`KT-22476`](https://youtrack.jetbrains.com/issue/KT-22476) Rename `it` parameter fails after replacing for-each with mapNotNull
- [`KT-22564`](https://youtrack.jetbrains.com/issue/KT-22564) Rename doesn't warn for conflicts
- [`KT-22705`](https://youtrack.jetbrains.com/issue/KT-22705) Refactor/Rename: rename of `invoke` function with lambda parameter to `get` breaks an implicit call
- [`KT-22708`](https://youtrack.jetbrains.com/issue/KT-22708) Refactor/Rename function using some stdlib name leads to incompilable code

### JavaScript

- [`KT-20735`](https://youtrack.jetbrains.com/issue/KT-20735) JS: kotlin.test-js integration tests terminate build on failure
- [`KT-22638`](https://youtrack.jetbrains.com/issue/KT-22638) Function reference not working in js from extension
- [`KT-22963`](https://youtrack.jetbrains.com/issue/KT-22963) KotlinJS - When statement can cause illegal break

### Libraries

- [`KT-22620`](https://youtrack.jetbrains.com/issue/KT-22620) Add support for TestNG in kotlin.test
- [`KT-16661`](https://youtrack.jetbrains.com/issue/KT-16661) Performance overhead in string splitting in Kotlin versus Java?
- [`KT-22042`](https://youtrack.jetbrains.com/issue/KT-22042) Suboptimal `Strings#findAnyOf`
- [`KT-21154`](https://youtrack.jetbrains.com/issue/KT-21154) kotlin-test-junit doesn't provide JUnitAsserter when test body is run in another thread

### Tools

- [`KT-22196`](https://youtrack.jetbrains.com/issue/KT-22196) kotlin-compiler-embeddable bundles outdated kotlinx.coroutines since 1.1.60
- [`KT-22549`](https://youtrack.jetbrains.com/issue/KT-22549) Service is dying during compilation

### Tools. CLI

- [`KT-19051`](https://youtrack.jetbrains.com/issue/KT-19051) Suppress Java 9 illegal access warnings

### Tools. Gradle

- [`KT-18462`](https://youtrack.jetbrains.com/issue/KT-18462) Add 'org.jetbrains.kotlin.platform.android' plugin.
- [`KT-18821`](https://youtrack.jetbrains.com/issue/KT-18821) Gradle plugin should not resolve dependencies at configuration time

### Tools. Maven

- [`KT-21581`](https://youtrack.jetbrains.com/issue/KT-21581) kotlin.compiler.incremental not copying resources

### Tools. Incremental Compile

- [`KT-22192`](https://youtrack.jetbrains.com/issue/KT-22192) Make precise java classes tracking in Gradle enabled by default

### Tools. J2K

- [`KT-21635`](https://youtrack.jetbrains.com/issue/KT-21635) J2K: create "inspection based post-processing"

### Tools. REPL

- [`KT-12037`](https://youtrack.jetbrains.com/issue/KT-12037) REPL crashes when trying to :load with incorrect filename

### Tools. kapt

- [`KT-22350`](https://youtrack.jetbrains.com/issue/KT-22350) kdoc comment preceding enum method causes compilation failure
- [`KT-22386`](https://youtrack.jetbrains.com/issue/KT-22386) kapt3 fails when project has class named System
- [`KT-22468`](https://youtrack.jetbrains.com/issue/KT-22468) Kapt fails to convert array type to anonymous array element type
- [`KT-22469`](https://youtrack.jetbrains.com/issue/KT-22469) Kapt 1.2.20+ may fail to process classes with KDoc
- [`KT-22493`](https://youtrack.jetbrains.com/issue/KT-22493) Kapt: NoSuchElementException in KotlinCliJavaFileManagerImpl if class first character is dollar sign
- [`KT-22582`](https://youtrack.jetbrains.com/issue/KT-22582) Kapt: Enums inside enum values should be forbidden
- [`KT-22711`](https://youtrack.jetbrains.com/issue/KT-22711) Deprecate original kapt (aka kapt1)

## 1.2.21

### Fixes

- [`KT-22349`](https://youtrack.jetbrains.com/issue/KT-22349) Android: creating new Basic activity fails with Throwable: "Inconsistent FILE tree in SingleRootFileViewProvider" at SingleRootFileViewProvider.checkLengthConsistency()
- [`KT-22459`](https://youtrack.jetbrains.com/issue/KT-22459) Remove .proto files from kotlin-reflect.jar

## 1.2.20

### Android

- [`KT-20085`](https://youtrack.jetbrains.com/issue/KT-20085) Android Extensions: ClassCastException after changing type of view in layout XML
- [`KT-20235`](https://youtrack.jetbrains.com/issue/KT-20235) Error, can't use plugin kotlin-android-extensions
- [`KT-20269`](https://youtrack.jetbrains.com/issue/KT-20269) Mark 'kapt.kotlin.generated' as a source root automatically in Android projects
- [`KT-20545`](https://youtrack.jetbrains.com/issue/KT-20545) Parcelable: Migrate to canonical NEW-DUP-INVOKESPECIAL form
- [`KT-20742`](https://youtrack.jetbrains.com/issue/KT-20742) @Serializable and @Parcelize do not work together
- [`KT-20928`](https://youtrack.jetbrains.com/issue/KT-20928) @Parcelize. Verify Error for Android Api 19

### Binary Metadata

- [`KT-11586`](https://youtrack.jetbrains.com/issue/KT-11586) Support class literal annotation arguments in AnnotationSerializer

### Compiler

#### New Features

- [`KT-17944`](https://youtrack.jetbrains.com/issue/KT-17944) Allow 'expect' final member be implemented by 'actual' open member
- [`KT-21982`](https://youtrack.jetbrains.com/issue/KT-21982) Recognize Checker Framework *declaration* annotations
- [`KT-17609`](https://youtrack.jetbrains.com/issue/KT-17609) Intrinsic suspend val coroutineContext
#### Performance Improvements

- [`KT-21322`](https://youtrack.jetbrains.com/issue/KT-21322) for-in-char-sequence loop improvements
- [`KT-21323`](https://youtrack.jetbrains.com/issue/KT-21323) Decreasing range loop improvements
#### Fixes

- [`KT-4174`](https://youtrack.jetbrains.com/issue/KT-4174) Verify error on lambda with closure in local class super call
- [`KT-10473`](https://youtrack.jetbrains.com/issue/KT-10473) Inapplicable diagnostics for mixed JS / JVM projects
- [`KT-12541`](https://youtrack.jetbrains.com/issue/KT-12541) VerifyError: Bad type on operand stack for local variable captured in local class
- [`KT-13454`](https://youtrack.jetbrains.com/issue/KT-13454) VerifyError on capture of outer class properties in closure inside inner class constructor
- [`KT-14148`](https://youtrack.jetbrains.com/issue/KT-14148) `VerifyError: Bad type on operand stack`  for anonymous type inheriting inner class
- [`KT-18254`](https://youtrack.jetbrains.com/issue/KT-18254) enumValueOf and enumValues throw UnsupportedOperationException when used within a non-inline function block
- [`KT-18514`](https://youtrack.jetbrains.com/issue/KT-18514) IllegalStateException on compile object that inherits its inner interface or class
- [`KT-18639`](https://youtrack.jetbrains.com/issue/KT-18639) VerifyError: Bad type on operand stack
- [`KT-19188`](https://youtrack.jetbrains.com/issue/KT-19188) Nondeterministic method order in class files using DefaultImpls
- [`KT-19827`](https://youtrack.jetbrains.com/issue/KT-19827) Strange VerifyError in simple Example
- [`KT-19928`](https://youtrack.jetbrains.com/issue/KT-19928) Analyze / Inspect Code: ISE "Concrete fake override public final fun <get-allowedTargets>()" at BridgesKt.findConcreteSuperDeclaration()
- [`KT-20433`](https://youtrack.jetbrains.com/issue/KT-20433) NPE during JVM code generation
- [`KT-20639`](https://youtrack.jetbrains.com/issue/KT-20639) Obsolete term "native" used in error message
- [`KT-20802`](https://youtrack.jetbrains.com/issue/KT-20802) USELESS_CAST diagnostic in functions with expression body
- [`KT-20873`](https://youtrack.jetbrains.com/issue/KT-20873) False CAST_NEVER_SUCCEEDS when upcasting Nothing
- [`KT-20903`](https://youtrack.jetbrains.com/issue/KT-20903) Method reference to expect function results in bogus resolution ambiguity
- [`KT-21105`](https://youtrack.jetbrains.com/issue/KT-21105) Compiler incorrectly optimize the operator `in`  with a floating point type range with NaN bound.
- [`KT-21146`](https://youtrack.jetbrains.com/issue/KT-21146) ArrayIndexOutOfBoundsException at org.jetbrains.kotlin.codegen.MemberCodegen.generateMethodCallTo(MemberCodegen.java:841)
- [`KT-21267`](https://youtrack.jetbrains.com/issue/KT-21267) Report pre-release errors if pre-release compiler is run with a release language version
- [`KT-21321`](https://youtrack.jetbrains.com/issue/KT-21321) for-in-array loop improvements
- [`KT-21343`](https://youtrack.jetbrains.com/issue/KT-21343) Compound assignment operator compiles incorrectly when LHS is a property imported from object
- [`KT-21354`](https://youtrack.jetbrains.com/issue/KT-21354) Inconsistent behavior of 'for-in-range' loop if range is an array variable modified in loop body
- [`KT-21532`](https://youtrack.jetbrains.com/issue/KT-21532) Enum constructor not found
- [`KT-21535`](https://youtrack.jetbrains.com/issue/KT-21535) SAM wrapper is not created for a value of functional type in delegating or super constructor call in secondary constructor
- [`KT-21671`](https://youtrack.jetbrains.com/issue/KT-21671) Inline sam wrapper during inline in another module
- [`KT-21919`](https://youtrack.jetbrains.com/issue/KT-21919) Invalid MethodParameters attribute generated for "$DefaultImpls" synthetic class with javaParameters=true
- [`KT-20429`](https://youtrack.jetbrains.com/issue/KT-20429) False-positive 'Unused return value of a function with lambda expression body' in enum constant constructor
- [`KT-21827`](https://youtrack.jetbrains.com/issue/KT-21827) SMAP problem during default lambda parameter inline

### IDE

#### New Features

- [`KT-4001`](https://youtrack.jetbrains.com/issue/KT-4001) Allow to set arguments indent to 1 tab (currently two and not customized)
- [`KT-13378`](https://youtrack.jetbrains.com/issue/KT-13378) Provide ability to configure highlighting for !! in expressions and ? in types
- [`KT-17928`](https://youtrack.jetbrains.com/issue/KT-17928) Support code folding for primary constructors
- [`KT-20591`](https://youtrack.jetbrains.com/issue/KT-20591) Show @StringRes/@IntegerRes annotations in parameter info
- [`KT-20952`](https://youtrack.jetbrains.com/issue/KT-20952) "Navigate | Related symbol" should support expect/actual navigation
- [`KT-21229`](https://youtrack.jetbrains.com/issue/KT-21229) Make it possible to explicitly select "latest" language/API version
- [`KT-21469`](https://youtrack.jetbrains.com/issue/KT-21469) Wrap property initializers after equals sign
- [`KT-14670`](https://youtrack.jetbrains.com/issue/KT-14670) Support kotlinPackageName() macro in live templates
- [`KT-14951`](https://youtrack.jetbrains.com/issue/KT-14951) Editor: navigate actions could be available in intention menu (as done in Java)
- [`KT-15320`](https://youtrack.jetbrains.com/issue/KT-15320) Live templates: Add function which returns the "outer" class name
- [`KT-20067`](https://youtrack.jetbrains.com/issue/KT-20067) Return label hints
- [`KT-20533`](https://youtrack.jetbrains.com/issue/KT-20533) Show "this" and "it" type hints in lambdas.
- [`KT-20614`](https://youtrack.jetbrains.com/issue/KT-20614) Change location of initial parameter type hint when parameters are on multiple lines
- [`KT-21949`](https://youtrack.jetbrains.com/issue/KT-21949) Please add a separate Color Scheme settings for properties synthesized from Java accessors
- [`KT-21974`](https://youtrack.jetbrains.com/issue/KT-21974) Editor color scheme option for Kotlin typealias names
#### Performance Improvements

- [`KT-17367`](https://youtrack.jetbrains.com/issue/KT-17367) Rebuild requested for index KotlinJavaScriptMetaFileIndex 
- [`KT-21632`](https://youtrack.jetbrains.com/issue/KT-21632) Freezing on typing
- [`KT-21701`](https://youtrack.jetbrains.com/issue/KT-21701) IDEA 2017.3 high CPU usage
#### Fixes

- [`KT-9562`](https://youtrack.jetbrains.com/issue/KT-9562) Wrong indent after Enter after an annotation
- [`KT-12176`](https://youtrack.jetbrains.com/issue/KT-12176) Formatter could reformat long primary constructors
- [`KT-12862`](https://youtrack.jetbrains.com/issue/KT-12862) Formatting: Weird wrapping setting for long ?: operator
- [`KT-15099`](https://youtrack.jetbrains.com/issue/KT-15099) Odd code formatting when chaining lambdas and splitting lines on operators
- [`KT-15254`](https://youtrack.jetbrains.com/issue/KT-15254) Use Platform icons for "Run" icon in gutter
- [`KT-17254`](https://youtrack.jetbrains.com/issue/KT-17254) Remove obsolete unfold-icons in structure view 
- [`KT-17838`](https://youtrack.jetbrains.com/issue/KT-17838) Can't report exceptions from the Kotlin plugin 1.1.4-dev-119 in IDEA #IU-171.4424.37
- [`KT-17843`](https://youtrack.jetbrains.com/issue/KT-17843) Don't show parameter name hints when calling Java methods with unknown parameter names
- [`KT-17964`](https://youtrack.jetbrains.com/issue/KT-17964) Local variable type hints in editor for anonymous object
- [`KT-17965`](https://youtrack.jetbrains.com/issue/KT-17965) Do not shown argument name hints for assert
- [`KT-18829`](https://youtrack.jetbrains.com/issue/KT-18829) Do not show parameter name hints for mapOf
- [`KT-18839`](https://youtrack.jetbrains.com/issue/KT-18839) Semantic highlighting not work for local variables in init
- [`KT-19012`](https://youtrack.jetbrains.com/issue/KT-19012) Data Flow from here: doesn't find template usages
- [`KT-19017`](https://youtrack.jetbrains.com/issue/KT-19017) Data Flow from here doesn't find usage in range position of for cycle
- [`KT-19018`](https://youtrack.jetbrains.com/issue/KT-19018) Data Flow from here doesn't find any usages of for-variable
- [`KT-19036`](https://youtrack.jetbrains.com/issue/KT-19036) Data Flow from here: please find calls of extension too
- [`KT-19039`](https://youtrack.jetbrains.com/issue/KT-19039) Data Flow from here: please find cases when an investigated variable is transferred as a parameter into a library function
- [`KT-19087`](https://youtrack.jetbrains.com/issue/KT-19087) Data flow to here: usages with explicit receiver are not found
- [`KT-19089`](https://youtrack.jetbrains.com/issue/KT-19089) Data Flow to here: assigned values are not found if an investigated property is a delegated one
- [`KT-19104`](https://youtrack.jetbrains.com/issue/KT-19104) Data Flow from here: usage of parameter or variable not found when used as lambda receiver/parameter
- [`KT-19106`](https://youtrack.jetbrains.com/issue/KT-19106) Data Flow from here: show point of call of a function used as a parameter investigated parameter/variable
- [`KT-19112`](https://youtrack.jetbrains.com/issue/KT-19112) Data Flow to here for a function (or its return value) doesn't find shorten forms of assignments
- [`KT-19519`](https://youtrack.jetbrains.com/issue/KT-19519) Structure view is not updated properly for function classes
- [`KT-19727`](https://youtrack.jetbrains.com/issue/KT-19727) Code style: New line after '(' with anonymous object or multi-line lambda unexpected behavior
- [`KT-19820`](https://youtrack.jetbrains.com/issue/KT-19820) Strange highlightning for enum constructor
- [`KT-19823`](https://youtrack.jetbrains.com/issue/KT-19823) Kotlin Gradle project import into IntelliJ: import kapt generated classes into classpath
- [`KT-19824`](https://youtrack.jetbrains.com/issue/KT-19824) Please provide a separate icon for a common library
- [`KT-19915`](https://youtrack.jetbrains.com/issue/KT-19915) TODO calls not blue highlighted in lambdas/DSLs
- [`KT-20096`](https://youtrack.jetbrains.com/issue/KT-20096) Kotlin Gradle script: SOE after beginning of Pair definition before some script section
- [`KT-20314`](https://youtrack.jetbrains.com/issue/KT-20314) Kotlin formatter does not respect annotations code style settings
- [`KT-20329`](https://youtrack.jetbrains.com/issue/KT-20329) Multiplatform: gutter "Is subclassed by" should show expect subclass from the common module
- [`KT-20380`](https://youtrack.jetbrains.com/issue/KT-20380) Configure Kotlin plugin updates dialog does not open without opened project
- [`KT-20521`](https://youtrack.jetbrains.com/issue/KT-20521) Kotlin Gradle script: valid build.gradle.kts is red and becomes normal only after reopening the project
- [`KT-20603`](https://youtrack.jetbrains.com/issue/KT-20603) Facet import: when API version > language version, set API version = language version, not to 1.0
- [`KT-20782`](https://youtrack.jetbrains.com/issue/KT-20782) Non-atomic trees update
- [`KT-20813`](https://youtrack.jetbrains.com/issue/KT-20813) SAM with receiver: call with SAM usage is compiled with Gradle, but not with JPS
- [`KT-20880`](https://youtrack.jetbrains.com/issue/KT-20880) Add documentation quick fix should create multiline comment and place caret in right place
- [`KT-20883`](https://youtrack.jetbrains.com/issue/KT-20883) Provide more information in "Missing documentation" inspection message
- [`KT-20884`](https://youtrack.jetbrains.com/issue/KT-20884) Functions with receivers should allow [this] in KDoc
- [`KT-20937`](https://youtrack.jetbrains.com/issue/KT-20937) Exception thrown on RMB click on folder in Kotlin project
- [`KT-20938`](https://youtrack.jetbrains.com/issue/KT-20938) IDE: kotlinc.xml with KotlinCommonCompilerArguments/freeArgs: XSE: "Cannot deserialize class CommonCompilerArguments$DummyImpl" at BaseKotlinCompilerSettings.loadState()
- [`KT-20953`](https://youtrack.jetbrains.com/issue/KT-20953) "Choose actual" popup shows redundant information
- [`KT-20985`](https://youtrack.jetbrains.com/issue/KT-20985) Additional reimport is required in 2017.3/2018.1 idea after creating or importing mp project
- [`KT-20987`](https://youtrack.jetbrains.com/issue/KT-20987) (PerModulePackageCache miss) ISE: diagnoseMissingPackageFragment
- [`KT-21002`](https://youtrack.jetbrains.com/issue/KT-21002) "Highlight usages of identifier under caret" should work for "it"
- [`KT-21076`](https://youtrack.jetbrains.com/issue/KT-21076) Recursive Companion.ivoke() call should be marked with according icon
- [`KT-21132`](https://youtrack.jetbrains.com/issue/KT-21132) containsKey() in SoftValueMap considered pointless
- [`KT-21150`](https://youtrack.jetbrains.com/issue/KT-21150) Do not infer compiler version from build.txt
- [`KT-21200`](https://youtrack.jetbrains.com/issue/KT-21200) Improve Structure-view for Kotlin files
- [`KT-21214`](https://youtrack.jetbrains.com/issue/KT-21214) Fix funcion selection in kotlin
- [`KT-21275`](https://youtrack.jetbrains.com/issue/KT-21275) Don't show argument name hints in calls of methods on 'dynamic' type
- [`KT-21318`](https://youtrack.jetbrains.com/issue/KT-21318) Highlighting of function exit points does not work if the function is a getter for property
- [`KT-21363`](https://youtrack.jetbrains.com/issue/KT-21363) IDE: kotlinc.xml with KotlinCommonCompilerArguments: build fails with UOE: "Operation is not supported for read-only collection" at EmptyList.clear()
- [`KT-21409`](https://youtrack.jetbrains.com/issue/KT-21409) UAST: Super-call arguments are not modeled/visited
- [`KT-21418`](https://youtrack.jetbrains.com/issue/KT-21418) Gradle based project in IDEA 181: Kotlin facets are not created
- [`KT-21441`](https://youtrack.jetbrains.com/issue/KT-21441) Folding multiline strings adds a space at the start if there is not one.
- [`KT-21546`](https://youtrack.jetbrains.com/issue/KT-21546) java.lang.IllegalArgumentException: Unexpected container fatal IDE error
- [`KT-21575`](https://youtrack.jetbrains.com/issue/KT-21575) Secondary constructor call body is missing
- [`KT-21645`](https://youtrack.jetbrains.com/issue/KT-21645) Weird parameter hint position
- [`KT-21733`](https://youtrack.jetbrains.com/issue/KT-21733) Structure view is not updated
- [`KT-21756`](https://youtrack.jetbrains.com/issue/KT-21756) Find Usages for "type" in ts2kt provokes exception
- [`KT-21770`](https://youtrack.jetbrains.com/issue/KT-21770) Pasting $this into an interpolated string shouldn't escape $
- [`KT-21833`](https://youtrack.jetbrains.com/issue/KT-21833) Type hints shown when destructing triple with type parameters
- [`KT-21852`](https://youtrack.jetbrains.com/issue/KT-21852) Custom API version is lost when settings are reopen after restarting IDE
- [`KT-11503`](https://youtrack.jetbrains.com/issue/KT-11503) cmd+shift+enter action in .kt files does not work on empty lines
- [`KT-17217`](https://youtrack.jetbrains.com/issue/KT-17217) Navigate to symbol: hard to choose between a lot of extension overloads
- [`KT-18674`](https://youtrack.jetbrains.com/issue/KT-18674) Join Lines should join strings
- [`KT-19524`](https://youtrack.jetbrains.com/issue/KT-19524) "Local variable type hints" should respect static imports
- [`KT-21010`](https://youtrack.jetbrains.com/issue/KT-21010) Gutter "Is subclassed by" should show actual subclass from the all platform modules in IDEA 2017.3/2018.1
- [`KT-21036`](https://youtrack.jetbrains.com/issue/KT-21036) Throwable “Access is allowed from event dispatch thread only.” after creating nine similar classes with functions.
- [`KT-21213`](https://youtrack.jetbrains.com/issue/KT-21213) Multiline kdoc - intellij joins lines together without space
- [`KT-21592`](https://youtrack.jetbrains.com/issue/KT-21592) <args>-Xjsr305=strict</args> not taken into account during the kotlin files compilation in Idea (maven)
- [`KT-22050`](https://youtrack.jetbrains.com/issue/KT-22050) Redundant parameter type hint on SAM
- [`KT-22071`](https://youtrack.jetbrains.com/issue/KT-22071) Formatter insists on increasing indentation in forEach lambda
- [`KT-22093`](https://youtrack.jetbrains.com/issue/KT-22093) Unnecessary line wrap with new Kotlin code style
- [`KT-22111`](https://youtrack.jetbrains.com/issue/KT-22111) Error while indexing PsiPlainTextFileImpl cannot be cast to KtFile
- [`KT-22121`](https://youtrack.jetbrains.com/issue/KT-22121) Enter in empty argument list should apply normal indent if "Continuation indent for argument list" is off
- [`KT-21702`](https://youtrack.jetbrains.com/issue/KT-21702) `KtLightAnnotation` can't be converted to UAST
- [`KT-19900`](https://youtrack.jetbrains.com/issue/KT-19900) IntelliJ does not recognise no-arg "invokeInitializers" set in pom.xml
### IDE. Completion

- [`KT-13220`](https://youtrack.jetbrains.com/issue/KT-13220) Completion for non-primary-constructor properties should suggest names with types instead of types
- [`KT-12797`](https://youtrack.jetbrains.com/issue/KT-12797) Code completion does not work for inner in base class
- [`KT-16402`](https://youtrack.jetbrains.com/issue/KT-16402) AssertionError on completing expression after template in string literal
- [`KT-20166`](https://youtrack.jetbrains.com/issue/KT-20166) Completion: property declaration completion should be greedy if `tab` pressed
- [`KT-20506`](https://youtrack.jetbrains.com/issue/KT-20506) Second smart completion suggests the same value recursively

### IDE. Debugger

- [`KT-17514`](https://youtrack.jetbrains.com/issue/KT-17514) Debugger, evaluate value: cannot find local variable error on attempt to evaluate outer variable
- [`KT-20962`](https://youtrack.jetbrains.com/issue/KT-20962) NullPointerException because of nullable location in debugger
- [`KT-21538`](https://youtrack.jetbrains.com/issue/KT-21538) "Step into" method doesn't work after adding lambda parameter to the call
- [`KT-21820`](https://youtrack.jetbrains.com/issue/KT-21820) Debugger: Evaluation fails for instance properties (older Android SDKs)

### IDE. Inspections and Intentions

#### New Features

- [`KT-4580`](https://youtrack.jetbrains.com/issue/KT-4580) Intention + inspection to convert between explicit and implicit 'this'
- [`KT-11023`](https://youtrack.jetbrains.com/issue/KT-11023) Inspection to highlight usages of Collections.sort() and replace them with .sort() method from Kotlin stdlib
- [`KT-13702`](https://youtrack.jetbrains.com/issue/KT-13702) Issue a warning when equals is called recursively within itself
- [`KT-18449`](https://youtrack.jetbrains.com/issue/KT-18449) Multiplatform project: provide a quick fix "Implement methods" for a impl class
- [`KT-18828`](https://youtrack.jetbrains.com/issue/KT-18828) Provide an intention action to move a companion object member to top level
- [`KT-19103`](https://youtrack.jetbrains.com/issue/KT-19103) Inspection to remove unnecessary suspend modifier
- [`KT-20484`](https://youtrack.jetbrains.com/issue/KT-20484) Add quick fix to add required target to annotation used on a type
- [`KT-20492`](https://youtrack.jetbrains.com/issue/KT-20492) Offer "Simplify" intention for 'when' expression where only one branch is known to be true
- [`KT-20615`](https://youtrack.jetbrains.com/issue/KT-20615) Inspection to detect usages of values incorrectly marked by Kotlin as const from Java code
- [`KT-20631`](https://youtrack.jetbrains.com/issue/KT-20631) Inspection to detect use of Unit as a standalone expression
- [`KT-20644`](https://youtrack.jetbrains.com/issue/KT-20644) Warning for missing const paired with val modifier for primitives and strings
- [`KT-20714`](https://youtrack.jetbrains.com/issue/KT-20714) Inspection for self-assigment of properties
- [`KT-21023`](https://youtrack.jetbrains.com/issue/KT-21023) Inspection to highlight variables / functions with implicit `Nothing?` type
- [`KT-21510`](https://youtrack.jetbrains.com/issue/KT-21510) Add inspection to add/remove this to/from bound callable
- [`KT-21560`](https://youtrack.jetbrains.com/issue/KT-21560) Inspection to sort modifiers
- [`KT-21573`](https://youtrack.jetbrains.com/issue/KT-21573) Code Style Inspection: `to -> Pair` function used not in infix form 
- [`KT-16260`](https://youtrack.jetbrains.com/issue/KT-16260) Add intention to specify all types explicitly in destructuring assignment
- [`KT-21547`](https://youtrack.jetbrains.com/issue/KT-21547) Allow separate regex for test class and function names in IDE inspection
- [`KT-21741`](https://youtrack.jetbrains.com/issue/KT-21741) Inspection to detect is checks for object types
- [`KT-21950`](https://youtrack.jetbrains.com/issue/KT-21950) Enable quick-fixes for annotator-reported problems in "Inspect Code"
- [`KT-22103`](https://youtrack.jetbrains.com/issue/KT-22103) SortModifiersInspection should report annotations after modifiers
#### Fixes

- [`KT-15941`](https://youtrack.jetbrains.com/issue/KT-15941) "Convert to secondary constructor" produces invalid code for generic property with default value
- [`KT-16340`](https://youtrack.jetbrains.com/issue/KT-16340) "Unused receiver parameter" for invoke operator on companion object
- [`KT-17161`](https://youtrack.jetbrains.com/issue/KT-17161) IDE suggest to replace a for loop with `forEach` to aggresively
- [`KT-17332`](https://youtrack.jetbrains.com/issue/KT-17332) Intention to replace forEach with a 'for' loop should convert return@forEach to continue
- [`KT-17730`](https://youtrack.jetbrains.com/issue/KT-17730) Incorrect suggestion to replace loop with negation to `any{}`
- [`KT-18816`](https://youtrack.jetbrains.com/issue/KT-18816) IDEA suggests replacing for-in-range with stdlib operations
- [`KT-18881`](https://youtrack.jetbrains.com/issue/KT-18881) Invalid "Loop can be replaced with stdlib operations" warning when class has `add()` function
- [`KT-19560`](https://youtrack.jetbrains.com/issue/KT-19560) Do not warn about receiver parameter not used for companion object
- [`KT-19977`](https://youtrack.jetbrains.com/issue/KT-19977) Convert Lambda to reference produces red code when wrong implicit receiver is in scope
- [`KT-20091`](https://youtrack.jetbrains.com/issue/KT-20091) "Convert object literal to class" should create inner class if necessary
- [`KT-20300`](https://youtrack.jetbrains.com/issue/KT-20300) "Variable can be inlined" should not be suggested if there's a variable with the same name in nested scope
- [`KT-20349`](https://youtrack.jetbrains.com/issue/KT-20349) Convert lambda to reference for trailing lambda inserts parameter names for all arguments if at least one named argument was passed
- [`KT-20435`](https://youtrack.jetbrains.com/issue/KT-20435) False "function is never used" warning
- [`KT-20622`](https://youtrack.jetbrains.com/issue/KT-20622) Don't propose “Remove explicit type specification” when it can change semantic?
- [`KT-20763`](https://youtrack.jetbrains.com/issue/KT-20763) Wrong resulting code for "add star projection" quick-fix for inner class with generic outer one
- [`KT-20887`](https://youtrack.jetbrains.com/issue/KT-20887) Missing documentation warning shouldn't be triggered for a member of a private class
- [`KT-20888`](https://youtrack.jetbrains.com/issue/KT-20888) Documentation should be inherited from Map.Entry type
- [`KT-20889`](https://youtrack.jetbrains.com/issue/KT-20889) Members of anonymous objects should be treated as private and not trigger "Missing documentation" warning
- [`KT-20894`](https://youtrack.jetbrains.com/issue/KT-20894) "Add type" quick fix does not take into account `vararg` modifier
- [`KT-20901`](https://youtrack.jetbrains.com/issue/KT-20901) IntelliJ autocorrect to add parameter to data class constructor should make the parameter a val
- [`KT-20981`](https://youtrack.jetbrains.com/issue/KT-20981) False positive for "redundant super" in data class
- [`KT-21025`](https://youtrack.jetbrains.com/issue/KT-21025) Kotlin UAST violates `JvmDeclarationUElement` contract by employing `JavaUAnnotation`
- [`KT-21061`](https://youtrack.jetbrains.com/issue/KT-21061) Cant work with UElement.kt in IDEA with 1.2.0-rc-39: "Stub index points to a file without PSI"
- [`KT-21104`](https://youtrack.jetbrains.com/issue/KT-21104) Do not propose to make local lateinit var immutable
- [`KT-21122`](https://youtrack.jetbrains.com/issue/KT-21122) QuickFix to create member for expect class shouldn't add body
- [`KT-21159`](https://youtrack.jetbrains.com/issue/KT-21159) Fix signature invoked from Java breaks Kotlin code
- [`KT-21179`](https://youtrack.jetbrains.com/issue/KT-21179) Remove empty class body on companion object breaks code
- [`KT-21192`](https://youtrack.jetbrains.com/issue/KT-21192) Confusing "unused expression"
- [`KT-21237`](https://youtrack.jetbrains.com/issue/KT-21237) ReplaceWith incorrectly removes property assignment
- [`KT-21332`](https://youtrack.jetbrains.com/issue/KT-21332) Create from usage: do not propose to create abstract function in non-abstract class
- [`KT-21373`](https://youtrack.jetbrains.com/issue/KT-21373) 'Remove redundant let' quickfix does not work with `in`
- [`KT-21497`](https://youtrack.jetbrains.com/issue/KT-21497) Inspection considers if block to be a lambda
- [`KT-21544`](https://youtrack.jetbrains.com/issue/KT-21544) "Add type" quick fix incorrectly processes `vararg` modifier with primitive type array initializer
- [`KT-21603`](https://youtrack.jetbrains.com/issue/KT-21603) "Join declaration and assignment" should remove 'lateinit' for 'var'
- [`KT-21612`](https://youtrack.jetbrains.com/issue/KT-21612) The "Remove redundant getter" inspection removes the type specifier
- [`KT-21698`](https://youtrack.jetbrains.com/issue/KT-21698) `Create interface` shouldn't suggest to declare it inside a class which implements it
- [`KT-21726`](https://youtrack.jetbrains.com/issue/KT-21726) "arrayOf can be replaced with literal" inspection quick fix produces incompilable result in presence of spread operator
- [`KT-21727`](https://youtrack.jetbrains.com/issue/KT-21727) "Redundant spread operator" inspection does not report array literal
- [`KT-12814`](https://youtrack.jetbrains.com/issue/KT-12814) Specify type explicitly produces erroneous code when platform type overrides not-null type
- [`KT-15180`](https://youtrack.jetbrains.com/issue/KT-15180) Incorrect quickfix 'Specify type explicitly'
- [`KT-17816`](https://youtrack.jetbrains.com/issue/KT-17816) "Replace elvis with if" produce nasty code when safe casts are involved
- [`KT-18396`](https://youtrack.jetbrains.com/issue/KT-18396) Bad quickfix for wrong nested classes in inner class
- [`KT-19073`](https://youtrack.jetbrains.com/issue/KT-19073) No-op quick fix for "Convert lambda to reference" IDE suggestion 
- [`KT-19283`](https://youtrack.jetbrains.com/issue/KT-19283) Kotlin KProperty reference cannot be converted to lambda
- [`KT-19736`](https://youtrack.jetbrains.com/issue/KT-19736) Rephrase text in the unconventional property name inspection
- [`KT-19771`](https://youtrack.jetbrains.com/issue/KT-19771) Preserve old "Convert to expression body" range
- [`KT-20437`](https://youtrack.jetbrains.com/issue/KT-20437) Naming convetions inspection: Add separate inspection for top-level and object properties
- [`KT-20620`](https://youtrack.jetbrains.com/issue/KT-20620) Replace operator with function call breaks code
- [`KT-21414`](https://youtrack.jetbrains.com/issue/KT-21414) OverridersSearch attempts to create nameless fake light method
- [`KT-21780`](https://youtrack.jetbrains.com/issue/KT-21780) Wrong redundant setter inspection
- [`KT-21837`](https://youtrack.jetbrains.com/issue/KT-21837) Don't require documentation on tests and test classes
- [`KT-21929`](https://youtrack.jetbrains.com/issue/KT-21929) Inappropriate quick fix for a sealed class instantiation 
- [`KT-21983`](https://youtrack.jetbrains.com/issue/KT-21983) Do not suggest to remove explicit Unit type for expression body
- [`KT-16619`](https://youtrack.jetbrains.com/issue/KT-16619) Incorrect 'accessing non-final property in constructor' warning

### IDE. Refactorings

#### New Features

- [`KT-20095`](https://youtrack.jetbrains.com/issue/KT-20095) Allow conversion of selected companion methods to methods with @JvmStatic
#### Fixes

- [`KT-15840`](https://youtrack.jetbrains.com/issue/KT-15840) Introduce type alias: don't change not-nullable type with nullable typealias
- [`KT-17212`](https://youtrack.jetbrains.com/issue/KT-17212) Refactor / Inline Function: with 1 occurrence both "Inline all" and "Inline this only" are suggested
- [`KT-18594`](https://youtrack.jetbrains.com/issue/KT-18594) Refactor / Extract (Functional) Parameter are available for annotation arguments, but fail with AE: "Body element is not found"
- [`KT-20146`](https://youtrack.jetbrains.com/issue/KT-20146) IAE “parameter 'name' of NameUtil.splitNameIntoWords must not be null” at renaming class
- [`KT-20335`](https://youtrack.jetbrains.com/issue/KT-20335) Refactor → Extract Type Parameter: “AWT events are not allowed inside write action” after processing duplicates
- [`KT-20402`](https://youtrack.jetbrains.com/issue/KT-20402) Throwable “PsiElement(IDENTIFIER) by KotlinInplaceParameterIntroducer” on calling Refactor → Extract Parameter for default values
- [`KT-20403`](https://youtrack.jetbrains.com/issue/KT-20403) AE “Body element is not found” on calling Refactor → Extract Parameter for default values in constructor of class without body
- [`KT-20790`](https://youtrack.jetbrains.com/issue/KT-20790) Refactoring extension function/property overagressive
- [`KT-20766`](https://youtrack.jetbrains.com/issue/KT-20766) Typealias end-of-line is removed when moving function and typealias to new file
- [`KT-21071`](https://youtrack.jetbrains.com/issue/KT-21071) Cannot invoke move refactoring on a typealias
- [`KT-21162`](https://youtrack.jetbrains.com/issue/KT-21162) Adding parameters to kotlin data class leads to compilation error
- [`KT-21288`](https://youtrack.jetbrains.com/issue/KT-21288) Change Signature refactoring fails to change signature of overriders
- [`KT-21334`](https://youtrack.jetbrains.com/issue/KT-21334) Extract variable doesn't take into account the receiver of a bound callable reference
- [`KT-21371`](https://youtrack.jetbrains.com/issue/KT-21371) Rename refactoring sometimes erases identifier being renamed when popping up name proposals
- [`KT-21530`](https://youtrack.jetbrains.com/issue/KT-21530) KNPE in introduce variable
- [`KT-21508`](https://youtrack.jetbrains.com/issue/KT-21508) `java.lang.AssertionError: PsiLiteralExpression` on property safe delete in Idea 173 
- [`KT-21536`](https://youtrack.jetbrains.com/issue/KT-21536) Rename refactoring sometimes doesn't quite work
- [`KT-21604`](https://youtrack.jetbrains.com/issue/KT-21604) Rename package missing title
- [`KT-21963`](https://youtrack.jetbrains.com/issue/KT-21963) Refactor / Inline Property: "null" in place of number of occurrences of local variable references
- [`KT-21964`](https://youtrack.jetbrains.com/issue/KT-21964) Refactor / Inline: on declaration of element with one usage "Inline and keep" choice is not suggested
- [`KT-21965`](https://youtrack.jetbrains.com/issue/KT-21965) Refactor / Inline: wording in dialog could be unified
### JavaScript

#### New Features

- [`KT-20210`](https://youtrack.jetbrains.com/issue/KT-20210) [JS] Ultra-fast builds for development
#### Performance Improvements

- [`KT-2218`](https://youtrack.jetbrains.com/issue/KT-2218) JS: Optimise in checks for number ranges
- [`KT-20932`](https://youtrack.jetbrains.com/issue/KT-20932) JS: Make withIndex() on arrays intrinsic
- [`KT-21160`](https://youtrack.jetbrains.com/issue/KT-21160) JS: generate switch statement for when statement when possible
#### Fixes

- [`KT-7653`](https://youtrack.jetbrains.com/issue/KT-7653) JS: TypeError when try to access to "simple" property (w/o backing field at runtime)
- [`KT-18963`](https://youtrack.jetbrains.com/issue/KT-18963) javascript project: No output directory found for Module 'xxx_test' production on JPS compiling
- [`KT-19290`](https://youtrack.jetbrains.com/issue/KT-19290) JS integer overflow for unaryMinus
- [`KT-19826`](https://youtrack.jetbrains.com/issue/KT-19826) JS: don't remove debugger statement from suspend functions
- [`KT-20580`](https://youtrack.jetbrains.com/issue/KT-20580) JS: JSON.stringify could improve 'replacer' argument handling
- [`KT-20694`](https://youtrack.jetbrains.com/issue/KT-20694) JS: add missed parts to JS Date
- [`KT-20737`](https://youtrack.jetbrains.com/issue/KT-20737) JS: cache KProperty instances that used to access to delegated property
- [`KT-20738`](https://youtrack.jetbrains.com/issue/KT-20738) JS: remove useless calls to constructor of KProperty* (PropertyMetadata) when it generated for access to delegated property
- [`KT-20854`](https://youtrack.jetbrains.com/issue/KT-20854) `val` parameters of type `kotlin.Char` aren't boxed
- [`KT-20898`](https://youtrack.jetbrains.com/issue/KT-20898) JS: inline js with `for` without initializer causes compiiler to crash
- [`KT-20905`](https://youtrack.jetbrains.com/issue/KT-20905) JS: compiler crashes on invalid inline JavaScript code instead of reporting error
- [`KT-20908`](https://youtrack.jetbrains.com/issue/KT-20908) JS frontend crashes on uncompleted call to function with reified parameters
- [`KT-20978`](https://youtrack.jetbrains.com/issue/KT-20978) JS: inline doesn't work for Array's constructor when it called through typealias
- [`KT-20994`](https://youtrack.jetbrains.com/issue/KT-20994) JS extension property in interface problem
- [`KT-21004`](https://youtrack.jetbrains.com/issue/KT-21004) JS: don't use short-circuit operators when translating Boolean.and/or(Boolean)
- [`KT-21026`](https://youtrack.jetbrains.com/issue/KT-21026) JS: wrong code generated for suspend fun that calls inline suspend fun as a tail call.
- [`KT-21041`](https://youtrack.jetbrains.com/issue/KT-21041) 'TypeError: ... is not a function' for lambda with closure passed as an argument to super type constructor
- [`KT-21043`](https://youtrack.jetbrains.com/issue/KT-21043) JS: inlining coroutine from other module sometimes causes incorrect code generated
- [`KT-21093`](https://youtrack.jetbrains.com/issue/KT-21093) Kotlin.JS doesnt escape ‘in’ identifier and conflicts with in keyword
- [`KT-21245`](https://youtrack.jetbrains.com/issue/KT-21245) JS: interface function with default parameter, overridden by other interface indirectly cannot be found at runtime
- [`KT-21307`](https://youtrack.jetbrains.com/issue/KT-21307) JS DCE does not remap paths to sources
- [`KT-21309`](https://youtrack.jetbrains.com/issue/KT-21309) JS: incorrect source map generated for inline lambda when it's last expression is a statement-like expression (e.g. when or try/catch)
- [`KT-21317`](https://youtrack.jetbrains.com/issue/KT-21317) JS: safe call to suspend function returning Unit causes incorrect 
- [`KT-21421`](https://youtrack.jetbrains.com/issue/KT-21421) JS: accesors of overridden char properties with backing fields aren't boxed
- [`KT-21468`](https://youtrack.jetbrains.com/issue/KT-21468) JS: don't use enum entry's name for when over external enums
- [`KT-21850`](https://youtrack.jetbrains.com/issue/KT-21850) JS: support nested tests
### Language design

- [`KT-10532`](https://youtrack.jetbrains.com/issue/KT-10532) ISE by ThrowingLexicalScope at compile time with specific override chain

### Libraries

- [`KT-20864`](https://youtrack.jetbrains.com/issue/KT-20864) Provide `ReadOnly` and `Mutable` annotations to control java collection mutability in kotlin
- [`KT-18789`](https://youtrack.jetbrains.com/issue/KT-18789) Delegating val to out-projected `MutableMap` resulted in NPE due to cast to `Nothing`
- [`KT-21828`](https://youtrack.jetbrains.com/issue/KT-21828) JS: The List produced by the `IntArray.asList` function caused weird results
- [`KT-21868`](https://youtrack.jetbrains.com/issue/KT-21868) Eliminate potential data race in `SafePublicationLazyImpl`
- [`KT-21918`](https://youtrack.jetbrains.com/issue/KT-21918) Make `toTypedArray()` implementation more efficient and thread-safe
- [`KT-22003`](https://youtrack.jetbrains.com/issue/KT-22003) JS: Replace `Regex` constructor-like functions with secondary constructors
- JS: `Volatile` and `Synchornized` annotations are moved to `kotlin.jvm` package with the migration type aliases provided
- [`KT-16348`](https://youtrack.jetbrains.com/issue/KT-16348) Provide `String.toBoolean()` conversion in JS and common platforms
- Add missing declarations to kotlin-stdlib-common, those that are already supported in both platforms
  - [`KT-21191`](https://youtrack.jetbrains.com/issue/KT-21191) Add missing exception constructors to common and JS declarations
  - [`KT-21861`](https://youtrack.jetbrains.com/issue/KT-21861) Provide `NumberFormatException` in common projects and make it inherit `IllegalArgumentException` in all platforms
  - Add missing `pattern` and `options` properties to common `Regex`
- [`KT-20968`](https://youtrack.jetbrains.com/issue/KT-20968) Improve docs for String.format and String.Companion.format

### Reflection

- [`KT-20875`](https://youtrack.jetbrains.com/issue/KT-20875) Support Void.TYPE as underlying Class object for KClass
- [`KT-21453`](https://youtrack.jetbrains.com/issue/KT-21453) NPE in TypeSignatureMappingKt#computeInternalName

### Tools

- [`KT-20298`](https://youtrack.jetbrains.com/issue/KT-20298) Lint warning when using @Parcelize with delegated properties
- [`KT-20299`](https://youtrack.jetbrains.com/issue/KT-20299) Android non-ASCII TextView Id Unresolved Reference Bug
- [`KT-20717`](https://youtrack.jetbrains.com/issue/KT-20717) @Parcelize Creator.newArray method is generated incorrectly
- [`KT-20751`](https://youtrack.jetbrains.com/issue/KT-20751) kotlin-spring compiler plugin does not open @Validated classes
- [`KT-21171`](https://youtrack.jetbrains.com/issue/KT-21171) _$_findViewCache and _$_findCachedViewById are created in Activity subclass without Kotlin Android Extensions.
- [`KT-21628`](https://youtrack.jetbrains.com/issue/KT-21628) Can't find referenced class kotlin.internal.annotations.AvoidUninitializedObjectCopyingCheck
- [`KT-21777`](https://youtrack.jetbrains.com/issue/KT-21777) RMI "Connection refused" errors with daemon
- [`KT-21992`](https://youtrack.jetbrains.com/issue/KT-21992) @Transient warning for lazy property

### Tools. Gradle

- [`KT-20892`](https://youtrack.jetbrains.com/issue/KT-20892) Support module name option in K2MetadataCompilerArguments
- [`KT-17621`](https://youtrack.jetbrains.com/issue/KT-17621) Incremental compilation is very slow when Java file is modified
- [`KT-14125`](https://youtrack.jetbrains.com/issue/KT-14125) Android-extensions don't track xml changes well
- [`KT-20233`](https://youtrack.jetbrains.com/issue/KT-20233) Kapt: using compiler in-process w/ gradle leads to classloader conflict
- [`KT-21009`](https://youtrack.jetbrains.com/issue/KT-21009) Running Gradle build with `clean` prevents `KotlinCompile` tasks from loading from cache
- [`KT-21596`](https://youtrack.jetbrains.com/issue/KT-21596) Improve Kapt Gradle Plugin to be more friendly for Kotlin-DSL
- [`KT-15753`](https://youtrack.jetbrains.com/issue/KT-15753) Support cacheable tasks
- [`KT-17656`](https://youtrack.jetbrains.com/issue/KT-17656) Kotlin and Kotlin Android plugin not using available build caches
- [`KT-20017`](https://youtrack.jetbrains.com/issue/KT-20017) Support local (non-relocatable) Gradle build cache
- [`KT-20598`](https://youtrack.jetbrains.com/issue/KT-20598) Missing input annotations on AbstractKotlinCompileTool
- [`KT-20604`](https://youtrack.jetbrains.com/issue/KT-20604) Kotlin plugin breaks relocatability and compile avoidance for Java compile tasks
- [`KT-21203`](https://youtrack.jetbrains.com/issue/KT-21203) Kotlin gradle plugin does not create proper Ivy metadata for dependencies
- [`KT-21261`](https://youtrack.jetbrains.com/issue/KT-21261) Gradle plugin 1.1.60 creates "build-history.bin" outside project.buildDir
- [`KT-21805`](https://youtrack.jetbrains.com/issue/KT-21805) Gradle plugin does not work with JDK 1.7 (KaptGradleModel)
- [`KT-21806`](https://youtrack.jetbrains.com/issue/KT-21806) Gradle Plugin: Using automatic dependency versions with 'maven-publish' plugin does not include dependency version in generated publication POMs
### Tools. Incremental Compile

- [`KT-20840`](https://youtrack.jetbrains.com/issue/KT-20840) Multiplatform IC fails if expected or actual file is modified separately
- [`KT-21622`](https://youtrack.jetbrains.com/issue/KT-21622) Make IC work more accurately with changes of Android layouts xml files
- [`KT-21699`](https://youtrack.jetbrains.com/issue/KT-21699) JS IC produces different source maps when enum usage is compiled separately
- [`KT-20633`](https://youtrack.jetbrains.com/issue/KT-20633) Class is not recompiled

### Tools. J2K

- [`KT-21502`](https://youtrack.jetbrains.com/issue/KT-21502) Inspection to convert map.put(k, v) into map[k] = v
- [`KT-19390`](https://youtrack.jetbrains.com/issue/KT-19390) Character and string concatenation in Java is converted to code with multiple type errors in Kotlin
- [`KT-19943`](https://youtrack.jetbrains.com/issue/KT-19943) Redundant 'toInt' after converting explicit Integer#intValue call

### Tools. JPS

- [`KT-21574`](https://youtrack.jetbrains.com/issue/KT-21574) JPS build: API version in project settings is ignored
- [`KT-21841`](https://youtrack.jetbrains.com/issue/KT-21841) JPS throws exception creating temporary file for module
- [`KT-21962`](https://youtrack.jetbrains.com/issue/KT-21962) Source file dependencies (lookups) are not tracked in JPS when Kotlin daemon is used

### Tools. Maven

- [`KT-20816`](https://youtrack.jetbrains.com/issue/KT-20816) Repeated Maven Compiles With Kapt Fail

### Tools. REPL

- [`KT-17561`](https://youtrack.jetbrains.com/issue/KT-17561) Embedding kotlin-script-utils may cause version conflicts e.g. with guava
- [`KT-17921`](https://youtrack.jetbrains.com/issue/KT-17921) The JSR 223 scripting engine fails to eval anything after encountering an unresolved reference
- [`KT-21075`](https://youtrack.jetbrains.com/issue/KT-21075) KotlinJsr223JvmLocalScriptEngineFactory does not well with kotlin-compiler-embeddable
- [`KT-21141`](https://youtrack.jetbrains.com/issue/KT-21141) Kotlin script: KotlinJsr223JvmLocalScriptEngine.state.history.reset() seems not clearing the compiler cache

### Tools. kapt

#### Fixes

- [`KT-18791`](https://youtrack.jetbrains.com/issue/KT-18791) Kapt: Constants from R class should not be inlined
- [`KT-19203`](https://youtrack.jetbrains.com/issue/KT-19203) Kapt3 generator doesn't seem to print log level lower to Mandatory Warning
- [`KT-19402`](https://youtrack.jetbrains.com/issue/KT-19402) `kapt.correctErrorTypes` makes typealias not work.
- [`KT-19505`](https://youtrack.jetbrains.com/issue/KT-19505) Kapt doesn't always stub classes about to be generated.
- [`KT-19518`](https://youtrack.jetbrains.com/issue/KT-19518) Kapt: Support 'correctErrorTypes' option in annotations
- [`KT-20257`](https://youtrack.jetbrains.com/issue/KT-20257) Kapt is incompatible with compiler plugins
- [`KT-20749`](https://youtrack.jetbrains.com/issue/KT-20749) Kapt: Support Java 9
- [`KT-21144`](https://youtrack.jetbrains.com/issue/KT-21144) Kapt: Compilation error with maven plugin (Java 9 compatibility)
- [`KT-21205`](https://youtrack.jetbrains.com/issue/KT-21205) KDoc unavailable via javax.lang.model.util.Elements#getDocComment(Element e)
- [`KT-21262`](https://youtrack.jetbrains.com/issue/KT-21262) Kapt: Remove artificial KaptError exception on errors from annotation processor
- [`KT-21264`](https://youtrack.jetbrains.com/issue/KT-21264) Kapt: -Xmaxerrs javac option is not propagated properly
- [`KT-21358`](https://youtrack.jetbrains.com/issue/KT-21358) Kapt: Support import directive with aliases (correctErrorTypes)
- [`KT-21359`](https://youtrack.jetbrains.com/issue/KT-21359) Kapt: Filter out non-package imports whenever possible (correctErrorTypes)
- [`KT-21425`](https://youtrack.jetbrains.com/issue/KT-21425) kapt warning when assembling unit tests
- [`KT-21433`](https://youtrack.jetbrains.com/issue/KT-21433) Annotations on enum constants are not kept on the generated stub
- [`KT-21483`](https://youtrack.jetbrains.com/issue/KT-21483) Kapt: Loading resources doesn't work without restarting the gradle daemon
- [`KT-21542`](https://youtrack.jetbrains.com/issue/KT-21542) Kapt: Report additional info about time spent in each annotation processor
- [`KT-21565`](https://youtrack.jetbrains.com/issue/KT-21565) Kapt, Maven: Support passing arguments for annotation processors
- [`KT-21566`](https://youtrack.jetbrains.com/issue/KT-21566) Kapt, Maven: Support passing Javac options
- [`KT-21729`](https://youtrack.jetbrains.com/issue/KT-21729) Error message says "androidProcessor" should be "annotationProcessor"
- [`KT-21936`](https://youtrack.jetbrains.com/issue/KT-21936) Kapt 1.2.20-eap:  cannot find symbol @KaptSignature
- [`KT-21735`](https://youtrack.jetbrains.com/issue/KT-21735) Kapt cache not cleared
- [`KT-22056`](https://youtrack.jetbrains.com/issue/KT-22056) Applying Kapt plugin causes RuntimeException on Gradle import: "Kapt importer for generated source roots failed, source root name: debug" at KaptProjectResolverExtension.populateAndroidModuleModelIfNeeded()
- [`KT-22189`](https://youtrack.jetbrains.com/issue/KT-22189) ISE from com.sun.tools.javac.util.Context.checkState when switching from 1.2.10 to 1.2.20-eap-33

## 1.2.10

### Compiler

- [`KT-20821`](https://youtrack.jetbrains.com/issue/KT-20821) Error while inlining function reference implicitly applied to this
- [`KT-21299`](https://youtrack.jetbrains.com/issue/KT-21299) Restore adding JDK roots to the beginning of the classpath list

### IDE

- [`KT-21180`](https://youtrack.jetbrains.com/issue/KT-21180) Project level api/language version settings are erroneously used as default during Gradle import
- [`KT-21335`](https://youtrack.jetbrains.com/issue/KT-21335) Fix exception on Project Structure view open
- [`KT-21610`](https://youtrack.jetbrains.com/issue/KT-21610) Fix "Could not determine the class-path for interface KotlinGradleModel" on Gradle sync
- Optimize dependency handling during import of Gradle project

### JavaScript

- [`KT-21493`](https://youtrack.jetbrains.com/issue/KT-21493) Losing lambda defined in inline function after incremental recompilation

### Tools. CLI

- [`KT-21495`](https://youtrack.jetbrains.com/issue/KT-21537) Bash scripts in Kotlin v1.2 compiler have Windows line terminators 
- [`KT-21537`](https://youtrack.jetbrains.com/issue/KT-21537) javac 7 do nothing when kotlin-compiler(-embeddable) is in classpath

### Libraries

- Unify docs wording of 'trim*' functions 
- Improve cover documentation page of kotlin.test library 
- Provide summary for kotlin.math package 
- Fix unresolved references in the api docs 

## 1.2

### Android

- [`KT-20974`](https://youtrack.jetbrains.com/issue/KT-20974) NSME "AndroidModuleModel.getMainArtifact" on Gradle refresh
- [`KT-20975`](https://youtrack.jetbrains.com/issue/KT-20975) IAE "Missing extension point" on Gradle refresh

### Compiler

- [`KT-6359`](https://youtrack.jetbrains.com/issue/KT-6359) Provide the way to share code with different targets(JVM, JS)

### IDE

- [`KT-21300`](https://youtrack.jetbrains.com/issue/KT-21300) IDEA slow down in Kotlin + Spring project 
- [`KT-20450`](https://youtrack.jetbrains.com/issue/KT-20450) Exception in UAST during function inlining
- [`KT-20789`](https://youtrack.jetbrains.com/issue/KT-20789) Can't navigate to inline call/inline use site when runner is delegated to Gradle
- [`KT-21236`](https://youtrack.jetbrains.com/issue/KT-21236) New project button doesn't work with Kotlin plugin enabled and Gradle plugin disabled
- [`KT-21263`](https://youtrack.jetbrains.com/issue/KT-21263) "Configure Kotlin Plugin Updates" suggests incompatible plugin for AS 3.0

### Tools. JPS

- [`KT-20757`](https://youtrack.jetbrains.com/issue/KT-20757) Rebuild when language/api version is changed

## 1.2-RC2

### Compiler

- [`KT-20844`](https://youtrack.jetbrains.com/issue/KT-20844) VerifyError on Android after upgrading to 1.2.0-beta-88
- [`KT-20895`](https://youtrack.jetbrains.com/issue/KT-20895) NPE in Kotlin 1.2-beta88 PseudocodeVariablesData.kt:337 
- [`KT-21377`](https://youtrack.jetbrains.com/issue/KT-21377) Create fallback flag for "Illegal smart cast is allowed after assignment in try block"

### IDE

- [`KT-18719`](https://youtrack.jetbrains.com/issue/KT-18719) Configure Kotlin in Gradle project to 1.2-Mx: add repository mavenCentral() to buildscript
- [`KT-20782`](https://youtrack.jetbrains.com/issue/KT-20782) Exception when working with routing in ktor (non-atomic trees update)
- [`KT-20966`](https://youtrack.jetbrains.com/issue/KT-20966) ISE: Facade class not found from Kotlin test files
- [`KT-20967`](https://youtrack.jetbrains.com/issue/KT-20967) Kotlin plugin upgrade breaks Gradle refresh
- [`KT-20990`](https://youtrack.jetbrains.com/issue/KT-20990) String literal in string template causes ISE
- [`KT-21028`](https://youtrack.jetbrains.com/issue/KT-21028) Add kotlin-stdlib-jre7/8 instead of kotlin-stdlib-jdk7/8 for Kotlin versions below 1.2
- [`KT-21383`](https://youtrack.jetbrains.com/issue/KT-21383) `Unsupported method: Library.getProject()` when importing Anko project
- Downgrade "use expression body" inspection to INFORMATION default level

### IDE. Debugger

- [`KT-20962`](https://youtrack.jetbrains.com/issue/KT-20962) NullPointerException because of nullable location in debugger

### IDE. Inspections and Intentions

- [`KT-20803`](https://youtrack.jetbrains.com/issue/KT-20803) Create actual declaration in the same source root as expect declaration

### IDE. Refactorings

- [`KT-20979`](https://youtrack.jetbrains.com/issue/KT-20979) Move class refactoring doesn't work anymore

### Libraries

- Remove deprecated `pairwise` function

### Tools. Gradle

- [`KT-21395`](https://youtrack.jetbrains.com/issue/KT-21395) “Unable to load class 'kotlin.collections.CollectionsKt'” on creating gradle project in IDEA 2016.3.7

### Tools. kapt

- Add `kotlin-annotation-processing-embeddable` artifact (compatible with `kotlin-compiler-embeddable`)
- Return `kotlin-annotation-processing` artifact back (compatible with CLI Kotlin compiler)

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

- [`KT-4900`](https://youtrack.jetbrains.com/issue/KT-4900) Finalize math operation parameter names

### Tools. JPS

- [`KT-20852`](https://youtrack.jetbrains.com/issue/KT-20852) IllegalArgumentException: URI has an authority component on attempt to jps compile the gradle project with javascript module

### Tools. kapt

- [`KT-20877`](https://youtrack.jetbrains.com/issue/KT-20877) Butterknife: UninitializedPropertyAccessException: "lateinit property has not been initialized" for field annotated with `@BindView`.

## 1.2-Beta2

### Multiplatform projects

#### New Features

- [`KT-20616`](https://youtrack.jetbrains.com/issue/KT-20616) Compiler options for `KotlinCompileCommon` task
- [`KT-15522`](https://youtrack.jetbrains.com/issue/KT-15522) Treat expect classes without explicit constructors as not having constructors at all
- [`KT-16099`](https://youtrack.jetbrains.com/issue/KT-16099) Do not require obvious override of super-interface methods in non-abstract expect class
- [`KT-20618`](https://youtrack.jetbrains.com/issue/KT-20618) Rename `implement` to `expectedBy` in gradle module dependency

#### Fixes

- [`KT-16926`](https://youtrack.jetbrains.com/issue/KT-16926) 'implement' dependency is not transitive when importing gradle project to IDEA
- [`KT-20634`](https://youtrack.jetbrains.com/issue/KT-20634) False error about platform project implementing non-common project
- [`KT-19170`](https://youtrack.jetbrains.com/issue/KT-19170) Forbid private expected declarations
- [`KT-20431`](https://youtrack.jetbrains.com/issue/KT-20431) Prohibit inheritance by delegation in 'expect' classes
- [`KT-20540`](https://youtrack.jetbrains.com/issue/KT-20540) Report errors about incompatible constructors of actual class
- [`KT-20398`](https://youtrack.jetbrains.com/issue/KT-20398) Do not highlight declarations with not implemented implementations with red during typing
- [`KT-19937`](https://youtrack.jetbrains.com/issue/KT-19937) Support "implement expect class" quickfix for nested classes
- [`KT-20657`](https://youtrack.jetbrains.com/issue/KT-20657) Actual annotation with all parameters that have default values doesn't match expected annotation with no-arg constructor
- [`KT-20680`](https://youtrack.jetbrains.com/issue/KT-20680) No actual class member: inconsistent modality check
- [`KT-18756`](https://youtrack.jetbrains.com/issue/KT-18756) multiplatform project: compilation error on implementation of extension property in javascript client module
- [`KT-17374`](https://youtrack.jetbrains.com/issue/KT-17374) Too many "expect declaration has no implementation" inspection in IDE in a multi-platform project
- [`KT-18455`](https://youtrack.jetbrains.com/issue/KT-18455) Multiplatform project: show gutter Navigate to implementation on expect side of method in the expect class
- [`KT-19222`](https://youtrack.jetbrains.com/issue/KT-19222) Useless tooltip on a gutter icon for expect declaration
- [`KT-20043`](https://youtrack.jetbrains.com/issue/KT-20043) multiplatform: No H gutter if a class has nested/inner classes inherited from it
- [`KT-20164`](https://youtrack.jetbrains.com/issue/KT-20164) expect/actual navigation does not work when actual is a typealias
- [`KT-20254`](https://youtrack.jetbrains.com/issue/KT-20254) multiplatform: there is no link between expect and actual classes, if implementation has a constructor when expect doesn't
- [`KT-20309`](https://youtrack.jetbrains.com/issue/KT-20309) multiplatform: ClassCastException on mouse hovering on the H gutter of the actual secondary constructor
- [`KT-20638`](https://youtrack.jetbrains.com/issue/KT-20638) Context menu in common module: NSEE: "Collection contains no element matching the predicate." at KotlinRunConfigurationProducerKt.findJvmImplementationModule()
- [`KT-18919`](https://youtrack.jetbrains.com/issue/KT-18919) multiplatform project: expect keyword is lost on converting to object
- [`KT-20008`](https://youtrack.jetbrains.com/issue/KT-20008) multiplatform: Create expect class implementation should add actual keyword at secondary constructors
- [`KT-20044`](https://youtrack.jetbrains.com/issue/KT-20044) multiplatform: Create expect class implementation should add actual constructor at primary constructor
- [`KT-20135`](https://youtrack.jetbrains.com/issue/KT-20135) "Create expect class implementation" should open created class in editor
- [`KT-20163`](https://youtrack.jetbrains.com/issue/KT-20163) multiplatform: it should be possible to create an implementation for overloaded method if for one method implementation is present already
- [`KT-20243`](https://youtrack.jetbrains.com/issue/KT-20243) multiplatform: quick fix Create expect interface implementation should add actual keyword at interface members
- [`KT-20325`](https://youtrack.jetbrains.com/issue/KT-20325) multiplatform: Quick fix Create actual ... should specify correct classifier name for object, enum class and annotation class

### Compiler

#### New Features

- [`KT-16028`](https://youtrack.jetbrains.com/issue/KT-16028) Allow to have different bodies of inline functions inlined depending on apiVersion

#### Performance Improvements

- [`KT-20462`](https://youtrack.jetbrains.com/issue/KT-20462) Don't create an array copy for '*<array-constructor-fun>(...)'

#### Fixes

- [`KT-13644`](https://youtrack.jetbrains.com/issue/KT-13644) Information from explicit cast should be used for type inference
- [`KT-14697`](https://youtrack.jetbrains.com/issue/KT-14697) Use-site targeted annotation is not correctly loaded from class file
- [`KT-17981`](https://youtrack.jetbrains.com/issue/KT-17981) Type parameter for catch parameter possible when exception is nested in generic, but fails in runtime
- [`KT-19251`](https://youtrack.jetbrains.com/issue/KT-19251) Stack spilling in constructor arguments breaks Quasar
- [`KT-20387`](https://youtrack.jetbrains.com/issue/KT-20387) Wrong argument generated for accessor call of a protected generic 'operator fun get/set' from base class with primitive type as type parameter
- [`KT-20491`](https://youtrack.jetbrains.com/issue/KT-20491) Incorrect synthetic accessor generated for a generic base class function specialized with primitive type
- [`KT-20651`](https://youtrack.jetbrains.com/issue/KT-20651) "Don't know how to generate outer expression" for enum-values with non-trivial self-closures
- [`KT-20752`](https://youtrack.jetbrains.com/issue/KT-20752) Do not register new kinds of smart casts for unstable values

### IDE

#### New Features

- [`KT-19146`](https://youtrack.jetbrains.com/issue/KT-19146) Parameter hints could be shown for annotation

#### Fixes

- [`KT-19207`](https://youtrack.jetbrains.com/issue/KT-19207) "Configure Kotlin in project" should add "requires kotlin.stdlib" to module-info for Java 9 modules
- [`KT-19213`](https://youtrack.jetbrains.com/issue/KT-19213) Formatter/Code Style: space between type parameters and `where` is not inserted
- [`KT-19216`](https://youtrack.jetbrains.com/issue/KT-19216) Parameter name hints should not be shown for functional type invocation
- [`KT-20448`](https://youtrack.jetbrains.com/issue/KT-20448) Exception in UAST during reference search in J2K
- [`KT-20543`](https://youtrack.jetbrains.com/issue/KT-20543) java.lang.ClassCastException on usage of array literals in Spring annotation
- [`KT-20709`](https://youtrack.jetbrains.com/issue/KT-20709) Loop in parent structure when converting a LITERAL_STRING_TEMPLATE_ENTRY

### IDE. Completion

- [`KT-17165`](https://youtrack.jetbrains.com/issue/KT-17165) Support array literals in annotations in completion

### IDE. Debugger

- [`KT-18775`](https://youtrack.jetbrains.com/issue/KT-18775) Evaluate expression doesn't allow access to properties of private nested objects, including companion

### IDE. Inspections and Intentions

#### New Features

- [`KT-20108`](https://youtrack.jetbrains.com/issue/KT-20108) Support "add requires directive to module-info.java" quick fix on usages of non-required modules in Kotlin sources
- [`KT-20410`](https://youtrack.jetbrains.com/issue/KT-20410) Add inspection for listOf().filterNotNull() to replace it with listOfNotNull()

#### Fixes

- [`KT-16636`](https://youtrack.jetbrains.com/issue/KT-16636) Remove parentheses after deleting the last unused constructor parameter
- [`KT-18549`](https://youtrack.jetbrains.com/issue/KT-18549) "Add type" quick fix adds non-primitive Array type for annotation parameters
- [`KT-18631`](https://youtrack.jetbrains.com/issue/KT-18631) Inspection to convert emptyArray() to empty literal does not work
- [`KT-18773`](https://youtrack.jetbrains.com/issue/KT-18773) Disable "Replace camel-case name with spaces" intention for JS and common projects
- [`KT-20183`](https://youtrack.jetbrains.com/issue/KT-20183) AE “Classifier descriptor of a type should be of type ClassDescriptor” on adding element to generic collection in function
- [`KT-20315`](https://youtrack.jetbrains.com/issue/KT-20315) "call chain on collection type may be simplified" generates code that does not compile

### JavaScript

#### Fixes

- [`KT-8285`](https://youtrack.jetbrains.com/issue/KT-8285) JS: don't generate tmp when only need one component
- [`KT-8374`](https://youtrack.jetbrains.com/issue/KT-8374) JS: some Double values converts to Int differently on JS and JVM
- [`KT-14549`](https://youtrack.jetbrains.com/issue/KT-14549) JS: Non-local returns from secondary constructors don't work
- [`KT-15294`](https://youtrack.jetbrains.com/issue/KT-15294) JS: parse error in `js()` function
- [`KT-17629`](https://youtrack.jetbrains.com/issue/KT-17629) JS: Equals function (==) returns true for all primitive numeric types
- [`KT-17760`](https://youtrack.jetbrains.com/issue/KT-17760) JS: Nothing::class throws error
- [`KT-17933`](https://youtrack.jetbrains.com/issue/KT-17933) JS: toString, hashCode method and simplename property of KClass return senseless results for some classes
- [`KT-18010`](https://youtrack.jetbrains.com/issue/KT-18010) JS: JsName annotation in interfaces can cause runtime exception
- [`KT-18063`](https://youtrack.jetbrains.com/issue/KT-18063) Inlining does not work properly in JS for suspend functions from another module
- [`KT-18548`](https://youtrack.jetbrains.com/issue/KT-18548) JS: wrong string interpolation with generic or Any parameters
- [`KT-19772`](https://youtrack.jetbrains.com/issue/KT-19772) JS: wrong boxing behavior for open val and final fun inside open class
- [`KT-19794`](https://youtrack.jetbrains.com/issue/KT-19794) runtime crash with empty object (Javascript)
- [`KT-19818`](https://youtrack.jetbrains.com/issue/KT-19818) JS: generate paths relative to .map file by default (unless "-source-map-prefix" is used)
- [`KT-19906`](https://youtrack.jetbrains.com/issue/KT-19906) JS: rename compiler option "-source-map-source-roots" to avoid misleading since sourcemaps have field called "sourceRoot"
- [`KT-20287`](https://youtrack.jetbrains.com/issue/KT-20287) Functions don't actually return Unit in Kotlin-JS -> unexpected null problems vs JDK version
- [`KT-20451`](https://youtrack.jetbrains.com/issue/KT-20451) KotlinJs - interface function with default parameter, overridden by implementor, can't be found at runtime
- [`KT-20527`](https://youtrack.jetbrains.com/issue/KT-20527) JS: use prototype chain to check that object implements kotlin interface
- [`KT-20650`](https://youtrack.jetbrains.com/issue/KT-20650) JS: compiler crashes in Java 9 with NoClassDefFoundError
- [`KT-20653`](https://youtrack.jetbrains.com/issue/KT-20653) JS: compiler crashes in Java 9 with TranslationRuntimeException

### Language design

- [`KT-20171`](https://youtrack.jetbrains.com/issue/KT-20171) Deprecate assigning single elements to varargs in named form

### Libraries

- [`KT-19696`](https://youtrack.jetbrains.com/issue/KT-19696) Provide a way to write multiplatform tests
- [`KT-18961`](https://youtrack.jetbrains.com/issue/KT-18961) Closeable.use should call addSuppressed
- [`KT-2460`](https://youtrack.jetbrains.com/issue/KT-2460) [`PR-1300`](https://github.com/JetBrains/kotlin/pull/1300) `shuffle` and `fill` extensions for MutableList now also available in JS
- [`PR-1230`](https://github.com/JetBrains/kotlin/pull/1230) Add assertSame and assertNotSame methods to kotlin-test

### Tools. Gradle

- [`KT-20553`](https://youtrack.jetbrains.com/issue/KT-20553) Rename `warningsAsErrors` compiler option to `allWarningsAsErrors`
- [`KT-20217`](https://youtrack.jetbrains.com/issue/KT-20217) `src/main/java` and `src/test/java` source directories are no longer included by default in Kotlin/JS and Kotlin/Common projects 

### Tools. Incremental Compile

- [`KT-20654`](https://youtrack.jetbrains.com/issue/KT-20654) AndroidStudio: NSME “PsiJavaModule.getName()Ljava/lang/String” on calling simple Kotlin functions like println(), listOf()

### Binary Metadata

- [`KT-20547`](https://youtrack.jetbrains.com/issue/KT-20547) Write pre-release flag into class files if language version > LATEST_STABLE

## 1.2-Beta

### Android

#### New Features

- [`KT-20051`](https://youtrack.jetbrains.com/issue/KT-20051) Quickfixes to support @Parcelize
#### Fixes

- [`KT-19747`](https://youtrack.jetbrains.com/issue/KT-19747) Android extensions + Parcelable: VerifyError in case of RawValue annotation on a type when it's unknown how to parcel it
- [`KT-19899`](https://youtrack.jetbrains.com/issue/KT-19899) Parcelize: Building with ProGuard enabled
- [`KT-19988`](https://youtrack.jetbrains.com/issue/KT-19988) [Android Extensions] inner class LayoutContainer causes NoSuchMethodError
- [`KT-20002`](https://youtrack.jetbrains.com/issue/KT-20002) Parcelize explodes on LongArray
- [`KT-20019`](https://youtrack.jetbrains.com/issue/KT-20019) Parcelize does not propogate flags argument when writing nested Parcelable
- [`KT-20020`](https://youtrack.jetbrains.com/issue/KT-20020) Parcelize does not use primitive array read/write methods on Parcel
- [`KT-20021`](https://youtrack.jetbrains.com/issue/KT-20021) Parcelize does not serialize Parcelable enum as Parcelable
- [`KT-20022`](https://youtrack.jetbrains.com/issue/KT-20022) Parcelize should dispatch directly to java.lang.Enum when writing an enum.
- [`KT-20034`](https://youtrack.jetbrains.com/issue/KT-20034) Application installation failed (INSTALL_FAILED_DEXOPT) in Android 4.3 devices if I use Parcelize
- [`KT-20057`](https://youtrack.jetbrains.com/issue/KT-20057) Parcelize should use specialized write/create methods where available.
- [`KT-20062`](https://youtrack.jetbrains.com/issue/KT-20062) Parceler should allow otherwise un-parcelable property types in enclosing class.
- [`KT-20170`](https://youtrack.jetbrains.com/issue/KT-20170) UAST: Getting the location of a UIdentifier is tricky

### Compiler

- [`KT-4565`](https://youtrack.jetbrains.com/issue/KT-4565) Support smart casting of safe cast's subject (and also safe call's receiver)
- [`KT-8492`](https://youtrack.jetbrains.com/issue/KT-8492) Null check should work after save call with elvis in condition
- [`KT-9327`](https://youtrack.jetbrains.com/issue/KT-9327) Need a way to check whether a lateinit property was assigned
- [`KT-14138`](https://youtrack.jetbrains.com/issue/KT-14138) Allow lateinit local variables
- [`KT-15461`](https://youtrack.jetbrains.com/issue/KT-15461) Allow lateinit top level properties
- [`KT-7257`](https://youtrack.jetbrains.com/issue/KT-7257) NPE when accessing properties of enum from inner lambda on initialization
- [`KT-9580`](https://youtrack.jetbrains.com/issue/KT-9580) Report an error if 'setparam' target does not make sense for a parameter declaration
- [`KT-16310`](https://youtrack.jetbrains.com/issue/KT-16310) Nested classes inside enum entries capturing outer members
- [`KT-20155`](https://youtrack.jetbrains.com/issue/KT-20155) Confusing diagnostics on a nested interface in inner class

### IDE

- [`KT-14175`](https://youtrack.jetbrains.com/issue/KT-14175) Surround with try ... catch (... finally) doesn't work for expressions
- [`KT-20308`](https://youtrack.jetbrains.com/issue/KT-20308) New Gradle with Kotlin DSL project wizard
- [`KT-18353`](https://youtrack.jetbrains.com/issue/KT-18353) Support UAST for .kts files
- [`KT-19823`](https://youtrack.jetbrains.com/issue/KT-19823) Kotlin Gradle project import into IntelliJ: import kapt generated classes into classpath
- [`KT-20185`](https://youtrack.jetbrains.com/issue/KT-20185) Stub and PSI element type mismatch for "var nullableSuspend: (suspend (P) -> Unit)? = null"

### Language design

- [`KT-14486`](https://youtrack.jetbrains.com/issue/KT-14486) Allow smart cast in closure if a local variable is modified only before it (and not after or inside)
- [`KT-15667`](https://youtrack.jetbrains.com/issue/KT-15667) Support "::foo" as a short-hand syntax for bound callable reference to "this::foo"
- [`KT-16681`](https://youtrack.jetbrains.com/issue/KT-16681) kotlin allows mutating the field of read-only property

### Libraries

- [`KT-19258`](https://youtrack.jetbrains.com/issue/KT-19258) Java 9: module-info.java with `requires kotlin.stdlib` causes compiler to fail: "module reads package from both kotlin.reflect and kotlin.stdlib"

### Tools

- [`KT-19692`](https://youtrack.jetbrains.com/issue/KT-19692) kotlin-jpa plugin doesn't support @MappedSuperclass annotation
- [`KT-20030`](https://youtrack.jetbrains.com/issue/KT-20030) Parcelize can directly reference writeToParcel and CREATOR for final, non-Parcelize Parcelable types in same compilation unit.
- [`KT-19742`](https://youtrack.jetbrains.com/issue/KT-19742) [Android extensions] Calling clearFindViewByIdCache causes NPE
- [`KT-19749`](https://youtrack.jetbrains.com/issue/KT-19749) Android extensions + Parcelable: NoSuchMethodError on attempt to pack into parcel a serializable object
- [`KT-20026`](https://youtrack.jetbrains.com/issue/KT-20026) Parcelize overrides describeContents despite being already implemented.
- [`KT-20027`](https://youtrack.jetbrains.com/issue/KT-20027) Parcelize uses wrong classloader when reading parcelable type.
- [`KT-20029`](https://youtrack.jetbrains.com/issue/KT-20029) Parcelize should not directly reference parcel methods on types outside compilation unit
- [`KT-20032`](https://youtrack.jetbrains.com/issue/KT-20032) Parcelize does not respect type nullability in case of Parcelize parcelables

### Tools. CLI

- [`KT-10563`](https://youtrack.jetbrains.com/issue/KT-10563) Support a command line argument -Werror to treat warnings as errors

### Tools. Gradle

- [`KT-20212`](https://youtrack.jetbrains.com/issue/KT-20212) Cannot access internal components from test code

### Tools. kapt

- [`KT-17923`](https://youtrack.jetbrains.com/issue/KT-17923) Reference to Dagger generated class is highlighted red
- [`KT-18923`](https://youtrack.jetbrains.com/issue/KT-18923) Kapt: Do not use the Kotlin error message collector to issue errors from kapt
- [`KT-19097`](https://youtrack.jetbrains.com/issue/KT-19097) Request: Decent support of `kapt.kotlin.generated` on Intellij/Android Studio
- [`KT-20001`](https://youtrack.jetbrains.com/issue/KT-20001) kapt generate stubs Gradle task does not depend on the compilation of sub-project kapt dependencies
