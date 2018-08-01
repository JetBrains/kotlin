# CHANGELOG

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

#### New Features

- [`KT-24827`](https://youtrack.jetbrains.com/issue/KT-24827) Allow to do an additional check for Kotlin plugin update before an actual install

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

