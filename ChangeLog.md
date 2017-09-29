# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.1.51

- [`KT-19901`](https://youtrack.jetbrains.com/issue/KT-19901) KotlinLanguageInjector#getLanguagesToInject can cancel any progress in which it was invoked
- [`KT-20411`](https://youtrack.jetbrains.com/issue/KT-20411) JSR 305 meta-annotations support is broken with Kotlin 1.1.50

## 1.1.50

### Android

- [`KT-14800`](https://youtrack.jetbrains.com/issue/KT-14800) Kotlin Lint: `@SuppressLint` annotation on local variable is ignored
- [`KT-16600`](https://youtrack.jetbrains.com/issue/KT-16600) False positive "For methods, permission annotation should specify one of `value`, `anyOf` or `allOf`"
- [`KT-16834`](https://youtrack.jetbrains.com/issue/KT-16834) Android Lint: Bogus warning on @setparam:StringRes
- [`KT-17785`](https://youtrack.jetbrains.com/issue/KT-17785) Kotlin Lint: "Incorrect support annotation usage" does not pick the value of const val
- [`KT-18837`](https://youtrack.jetbrains.com/issue/KT-18837) Android Lint: Collection.removeIf is not flagged when used on RealmList
- [`KT-18893`](https://youtrack.jetbrains.com/issue/KT-18893) Android support annotations (ColorInt, etc) cannot be used on properties: "does not apply for type void"
- [`KT-18997`](https://youtrack.jetbrains.com/issue/KT-18997) KLint: False positive "Could not find property setter method setLevel on java.lang.Object" if using elvis with return on RHS
- [`KT-19671`](https://youtrack.jetbrains.com/issue/KT-19671) UAST: Parameter annotations not provided for val parameters

### Compiler

#### Performance Improvements

- [`KT-17963`](https://youtrack.jetbrains.com/issue/KT-17963) Unnecessary boxing in case of primitive comparison to object
- [`KT-18589`](https://youtrack.jetbrains.com/issue/KT-18589) 'Equality check can be used instead of elvis' produces code that causes boxing
- [`KT-18693`](https://youtrack.jetbrains.com/issue/KT-18693) Optimize in-expression with optimizable range in RHS
- [`KT-18721`](https://youtrack.jetbrains.com/issue/KT-18721) Improve code generation for if-in-primitive-literal expression ('if (expr in low .. high)') 
- [`KT-18818`](https://youtrack.jetbrains.com/issue/KT-18818) Optimize null cases in `when` statement to avoid Intrinsics usage
- [`KT-18834`](https://youtrack.jetbrains.com/issue/KT-18834) Do not create ranges for 'x in low..high' where type of x doesn't match range element type
- [`KT-19029`](https://youtrack.jetbrains.com/issue/KT-19029) Use specialized equality implementations for 'when'
- [`KT-19149`](https://youtrack.jetbrains.com/issue/KT-19149) Use 'for-in-until' loop in intrinsic array constructors
- [`KT-19252`](https://youtrack.jetbrains.com/issue/KT-19252) Use 'for-in-until' loop for 'for-in-rangeTo' loops with constant upper bounds when possible
- [`KT-19256`](https://youtrack.jetbrains.com/issue/KT-19256) Destructuring assignment generates redundant code for temporary variable nullification
- [`KT-19457`](https://youtrack.jetbrains.com/issue/KT-19457) Extremely slow analysis for file with deeply nested lambdas
#### Fixes

- [`KT-10754`](https://youtrack.jetbrains.com/issue/KT-10754) Bogus unresolved extension function
- [`KT-11739`](https://youtrack.jetbrains.com/issue/KT-11739) Incorrect error message on getValue operator with KProperty<Something> parameter
- [`KT-11834`](https://youtrack.jetbrains.com/issue/KT-11834) INAPPLICABLE_LATEINIT_MODIFIER is confusing for a generic type parameter with nullable (default) upper bound
- [`KT-11963`](https://youtrack.jetbrains.com/issue/KT-11963) Exception: recursive call in a lazy value under LockBasedStorageManager
- [`KT-12737`](https://youtrack.jetbrains.com/issue/KT-12737) Confusing error message when calling extension function with an implicit receiver, passing value parameter of wrong type
- [`KT-12767`](https://youtrack.jetbrains.com/issue/KT-12767) Too much unnecessary information in "N type arguments expected" error message
- [`KT-12796`](https://youtrack.jetbrains.com/issue/KT-12796) IllegalArgumentException on referencing inner class constructor on an outer class instance
- [`KT-12899`](https://youtrack.jetbrains.com/issue/KT-12899) Platform null escapes if passed as an extension receiver to an inline function
- [`KT-13665`](https://youtrack.jetbrains.com/issue/KT-13665) Generic componentN() functions should provide better diagnostics when type cannot be inferred
- [`KT-16223`](https://youtrack.jetbrains.com/issue/KT-16223) Confusing diagnostic for local inline functions
- [`KT-16246`](https://youtrack.jetbrains.com/issue/KT-16246) CompilationException caused by intersection type overload and wrong type parameter
- [`KT-16746`](https://youtrack.jetbrains.com/issue/KT-16746) DslMarker doesn't work with typealiases
- [`KT-17444`](https://youtrack.jetbrains.com/issue/KT-17444) Accessors generated for private file functions should respect @JvmName
- [`KT-17464`](https://youtrack.jetbrains.com/issue/KT-17464) Calling super constructor with generic function call in arguments fails at runtime
- [`KT-17725`](https://youtrack.jetbrains.com/issue/KT-17725) java.lang.VerifyError when both dispatch receiver and extension receiver have smart casts
- [`KT-17745`](https://youtrack.jetbrains.com/issue/KT-17745) Unfriendly error message on creating an instance of interface via typealias
- [`KT-17748`](https://youtrack.jetbrains.com/issue/KT-17748) Equality for class literals of primitive types is not preserved by reification
- [`KT-17879`](https://youtrack.jetbrains.com/issue/KT-17879) Comparing T::class from a reified generic with a Class<*> and KClass<*> variable in when statement is broken
- [`KT-18356`](https://youtrack.jetbrains.com/issue/KT-18356) Argument reordering in super class constructor call for anonymous object fails with VerifyError
- [`KT-18819`](https://youtrack.jetbrains.com/issue/KT-18819) JVM BE treats 'if (a in low .. high)' as 'if (a >= low && a <= high)', so 'high' can be non-evaluated
- [`KT-18855`](https://youtrack.jetbrains.com/issue/KT-18855) Convert "Remove at from annotation argument" inspection into compiler error & quick-fix
- [`KT-18858`](https://youtrack.jetbrains.com/issue/KT-18858) Exception within typealias expansion with dynamic used as one of type arguments
- [`KT-18902`](https://youtrack.jetbrains.com/issue/KT-18902) NullPointerException when using provideDelegate with properties of the base class at runtime
- [`KT-18940`](https://youtrack.jetbrains.com/issue/KT-18940) REPEATED_ANNOTATION is reported on wrong location for typealias arguments
- [`KT-18944`](https://youtrack.jetbrains.com/issue/KT-18944) Type annotations are lost for dynamic type
- [`KT-18966`](https://youtrack.jetbrains.com/issue/KT-18966) Report full package FQ name in compilation errors related to visibility
- [`KT-18971`](https://youtrack.jetbrains.com/issue/KT-18971) Missing non-null assertion for platform type passed as a receiver to the member extension function
- [`KT-18982`](https://youtrack.jetbrains.com/issue/KT-18982) NoSuchFieldError on access to imported object property from the declaring object itself
- [`KT-18985`](https://youtrack.jetbrains.com/issue/KT-18985) Too large highlighting range for UNCHECKED_CAST
- [`KT-19058`](https://youtrack.jetbrains.com/issue/KT-19058) VerifyError: no CHECKAST on dispatch receiver of the synthetic property defined in Java interface
- [`KT-19100`](https://youtrack.jetbrains.com/issue/KT-19100) VerifyError: missing CHECKCAST on extension receiver of the extension property
- [`KT-19115`](https://youtrack.jetbrains.com/issue/KT-19115) Report warnings on usages of JSR 305-annotated declarations which rely on incorrect or missing nullability information
- [`KT-19128`](https://youtrack.jetbrains.com/issue/KT-19128) java.lang.VerifyError with smart cast to String from Any
- [`KT-19180`](https://youtrack.jetbrains.com/issue/KT-19180) Bad SAM conversion of Java interface causing ClassCastException: [...] cannot be cast to kotlin.jvm.functions.Function1
- [`KT-19205`](https://youtrack.jetbrains.com/issue/KT-19205) Poor diagnostic message for deprecated class referenced through typealias
- [`KT-19367`](https://youtrack.jetbrains.com/issue/KT-19367) NSFE if property with name matching companion object property name is referenced within lambda
- [`KT-19434`](https://youtrack.jetbrains.com/issue/KT-19434) Object inheriting generic class with a reified type parameter looses method annotations
- [`KT-19475`](https://youtrack.jetbrains.com/issue/KT-19475) AnalyserException in case of combination of `while (true)` + stack-spilling (coroutines/try-catch expressions)
- [`KT-19528`](https://youtrack.jetbrains.com/issue/KT-19528) Compiler exception on inline suspend function inside a generic class
- [`KT-19575`](https://youtrack.jetbrains.com/issue/KT-19575) Deprecated typealias is not marked as such in access to companion object
- [`KT-19601`](https://youtrack.jetbrains.com/issue/KT-19601) UPPER_BOUND_VIOLATED reported on type alias expansion in a recursive upper bound on a type parameter
- [`KT-19814`](https://youtrack.jetbrains.com/issue/KT-19814) Runtime annotations for open suspend function are not generated correctly
- [`KT-19892`](https://youtrack.jetbrains.com/issue/KT-19892) Overriding remove method on inheritance from TreeSet<Int>
- [`KT-19910`](https://youtrack.jetbrains.com/issue/KT-19910) Nullability assertions removed when inlining an anonymous object in crossinline lambda
- [`KT-19985`](https://youtrack.jetbrains.com/issue/KT-19985) JSR 305: nullability qualifier of Java function return type detected incorrectly in case of using annotation nickname

### IDE

#### New Features

- [`KT-6676`](https://youtrack.jetbrains.com/issue/KT-6676) Show enum constant ordinal in quick doc like in Java
- [`KT-12246`](https://youtrack.jetbrains.com/issue/KT-12246) Kotlin source files are not highlighted in Gradle build output in IntelliJ
#### Performance Improvements

- [`KT-19670`](https://youtrack.jetbrains.com/issue/KT-19670) When computing argument hints, don't resolve call if none of the arguments are unclear expressions
#### Fixes

- [`KT-9288`](https://youtrack.jetbrains.com/issue/KT-9288) Call hierarchy ends on function call inside local val initializer expression
- [`KT-9669`](https://youtrack.jetbrains.com/issue/KT-9669) Join Lines should add semicolon when joining statements into the same line
- [`KT-14346`](https://youtrack.jetbrains.com/issue/KT-14346) IllegalArgumentException on attempt to call Show Hierarchy view on lambda
- [`KT-14428`](https://youtrack.jetbrains.com/issue/KT-14428) AssertionError in KotlinCallerMethodsTreeStructure.<init> on attempt to call Hierarchy view
- [`KT-19466`](https://youtrack.jetbrains.com/issue/KT-19466) Kotlin based Gradle build not recognized when added as a module
- [`KT-18083`](https://youtrack.jetbrains.com/issue/KT-18083) IDEA: Support extension main function
- [`KT-18863`](https://youtrack.jetbrains.com/issue/KT-18863) Formatter should add space after opening brace in a single-line enum declaration
- [`KT-19024`](https://youtrack.jetbrains.com/issue/KT-19024) build.gradle.kts is not supported as project
- [`KT-19124`](https://youtrack.jetbrains.com/issue/KT-19124) Creating source file with directory/package throws AE: "Write access is allowed inside write-action only" at NewKotlinFileAction$Companion.findOrCreateTarget()
- [`KT-19154`](https://youtrack.jetbrains.com/issue/KT-19154) Completion and auto-import does not suggest companion object members when inside an extension function
- [`KT-19202`](https://youtrack.jetbrains.com/issue/KT-19202) Applying 'ReplaceWith' fix in type alias can change program behaviour
- [`KT-19209`](https://youtrack.jetbrains.com/issue/KT-19209) "Stub and PSI element type mismatch" in when receiver type is annotated with @receiver
- [`KT-19277`](https://youtrack.jetbrains.com/issue/KT-19277) Optimize imports on the fly should not work in test data files
- [`KT-19278`](https://youtrack.jetbrains.com/issue/KT-19278) Optimize imports on the fly should not remove incomplete import while it's being typed
- [`KT-19322`](https://youtrack.jetbrains.com/issue/KT-19322) Script editor: Move Statement Down/Up can't move one out of top level lambda
- [`KT-19451`](https://youtrack.jetbrains.com/issue/KT-19451) "Unresolved reference" with Kotlin Android Extensions when layout defines the Android namespace as something other than "android"
- [`KT-19492`](https://youtrack.jetbrains.com/issue/KT-19492) Java 9: references from unnamed module to not exported classes of named module are compiled, but red in the editor
- [`KT-19493`](https://youtrack.jetbrains.com/issue/KT-19493) Java 9: references from named module to classes of unnamed module are not compiled, but green in the editor
- [`KT-19843`](https://youtrack.jetbrains.com/issue/KT-19843) Performance warning: LineMarker is supposed to be registered for leaf elements only
- [`KT-19889`](https://youtrack.jetbrains.com/issue/KT-19889) KotlinGradleModel : Unsupported major.minor version 52.0
- [`KT-19885`](https://youtrack.jetbrains.com/issue/KT-19885) 200% CPU for some time on Kotlin sources (PackagePartClassUtils.hasTopLevelCallables())
- [`KT-19901`](https://youtrack.jetbrains.com/issue/KT-19901) KotlinLanguageInjector#getLanguagesToInject can cancel any progress in which it was invoked
- [`KT-19903`](https://youtrack.jetbrains.com/issue/KT-19903) Copy Reference works incorrectly for const val
- [`KT-20153`](https://youtrack.jetbrains.com/issue/KT-20153) Kotlin facet: Java 9 `-Xadd-modules` setting produces more and more identical sub-elements of `<additionalJavaModules>` in .iml file

### IDE. Completion

- [`KT-8848`](https://youtrack.jetbrains.com/issue/KT-8848) Code completion does not support import aliases
- [`KT-18040`](https://youtrack.jetbrains.com/issue/KT-18040) There is no auto-popup competion after typing "$x." anymore
- [`KT-19015`](https://youtrack.jetbrains.com/issue/KT-19015) Smart completion: parameter list completion is not available when some of parameters are already written

### IDE. Debugger

- [`KT-19429`](https://youtrack.jetbrains.com/issue/KT-19429) Breakpoint appears in random place during debug

### IDE. Inspections and Intentions

#### New Features

- [`KT-4748`](https://youtrack.jetbrains.com/issue/KT-4748) Remove double negation for boolean expressions intention + inspection
- [`KT-5878`](https://youtrack.jetbrains.com/issue/KT-5878) Quickfix for "variable initializer is redundant" (VARIABLE_WITH_REDUNDANT_INITIALIZER)
- [`KT-11991`](https://youtrack.jetbrains.com/issue/KT-11991) Kotlin should have an inspection to suggest the simplified format for a no argument lambda
- [`KT-12195`](https://youtrack.jetbrains.com/issue/KT-12195) Quickfix @JvmStatic on main() method in an object
- [`KT-12233`](https://youtrack.jetbrains.com/issue/KT-12233) "Package naming convention" inspection could show warning in .kt sources
- [`KT-12504`](https://youtrack.jetbrains.com/issue/KT-12504) Intention to make open class with only private constructors sealed
- [`KT-12523`](https://youtrack.jetbrains.com/issue/KT-12523) Quick-fix to remove `when` with only `else`
- [`KT-12613`](https://youtrack.jetbrains.com/issue/KT-12613) "Make abstract" on member of open or final class should make abstract both member and class
- [`KT-16033`](https://youtrack.jetbrains.com/issue/KT-16033) Automatically static import the enum value name when "Add remaining branches" on an enum from another class/file
- [`KT-16404`](https://youtrack.jetbrains.com/issue/KT-16404) Create from usage should allow generating nested classes
- [`KT-17322`](https://youtrack.jetbrains.com/issue/KT-17322) Intentions to generate a getter and a setter for a property
- [`KT-17888`](https://youtrack.jetbrains.com/issue/KT-17888) Inspection to warn about suspicious combination of == and ===
- [`KT-18826`](https://youtrack.jetbrains.com/issue/KT-18826) INAPPLICABLE_LATEINIT_MODIFIER should have a quickfix to remove initializer
- [`KT-18965`](https://youtrack.jetbrains.com/issue/KT-18965) Add quick-fix for USELESS_IS_CHECK
- [`KT-19126`](https://youtrack.jetbrains.com/issue/KT-19126) Add quickfix for 'Property initializes are not allowed in interfaces'
- [`KT-19282`](https://youtrack.jetbrains.com/issue/KT-19282) Support "flip equals" intention for String.equals extension from stdlib
- [`KT-19428`](https://youtrack.jetbrains.com/issue/KT-19428) Add inspection for redundant overrides that only call the super method
- [`KT-19514`](https://youtrack.jetbrains.com/issue/KT-19514) Redundant getter / setter inspection
#### Fixes

- [`KT-13985`](https://youtrack.jetbrains.com/issue/KT-13985) "Add remaining branches" action does not use back-ticks correctly
- [`KT-15422`](https://youtrack.jetbrains.com/issue/KT-15422) Reduce irrelevant reporting of Destructure inspection
- [`KT-17480`](https://youtrack.jetbrains.com/issue/KT-17480) Create from usage in expression body of override function should take base type into account
- [`KT-18482`](https://youtrack.jetbrains.com/issue/KT-18482) "Move lambda argument to parenthesis" action generate uncompilable code
- [`KT-18665`](https://youtrack.jetbrains.com/issue/KT-18665) "Use destructuring declaration" must not be suggested for invisible properties
- [`KT-18666`](https://youtrack.jetbrains.com/issue/KT-18666) "Use destructuring declaration" should not be reported on a variable used in destructuring declaration only
- [`KT-18978`](https://youtrack.jetbrains.com/issue/KT-18978) Intention Move to class body generates incorrect code for vararg val/var
- [`KT-19006`](https://youtrack.jetbrains.com/issue/KT-19006) Inspection message "Equality check can be used instead of elvis" is slightly confusing
- [`KT-19011`](https://youtrack.jetbrains.com/issue/KT-19011) Unnecessary import for companion object property with extension function type is automatically inserted
- [`KT-19299`](https://youtrack.jetbrains.com/issue/KT-19299) Quickfix to correct overriding function signature keeps java NotNull annotations
- [`KT-19614`](https://youtrack.jetbrains.com/issue/KT-19614) Quickfix for INVISIBLE_MEMBER doesn't offer to make member protected if referenced from subclass
- [`KT-19666`](https://youtrack.jetbrains.com/issue/KT-19666) ClassCastException in IfThenToElvisIntention
- [`KT-19704`](https://youtrack.jetbrains.com/issue/KT-19704) Don't remove braces in redundant cascade if
- [`KT-19811`](https://youtrack.jetbrains.com/issue/KT-19811) Internal member incorrectly highlighted as unused
- [`KT-19926`](https://youtrack.jetbrains.com/issue/KT-19926) Naming convention inspections: pattern is validated while edited, PSE at Pattern.error()
- [`KT-19927`](https://youtrack.jetbrains.com/issue/KT-19927) "Package naming convention" inspection checks FQN, but default pattern looks like for simple name

### IDE. Refactorings

- [`KT-17266`](https://youtrack.jetbrains.com/issue/KT-17266) Refactor / Inline Function: reference to member of class containing extension function is inlined wrong
- [`KT-17776`](https://youtrack.jetbrains.com/issue/KT-17776) Inline method of inner class adds 'this' for methods from enclosing class
- [`KT-19161`](https://youtrack.jetbrains.com/issue/KT-19161) Safe delete conflicts are shown incorrectly for local declarations

### JavaScript

#### Performance Improvements

- [`KT-18329`](https://youtrack.jetbrains.com/issue/KT-18329) JS: for loop implementation depends on parentheses
#### Fixes

- [`KT-12970`](https://youtrack.jetbrains.com/issue/KT-12970) Empty block expression result is 'undefined' (expected: 'kotlin.Unit')
- [`KT-13930`](https://youtrack.jetbrains.com/issue/KT-13930) Safe call for a function returning 'Unit' result is 'undefined' or 'null' (instead of 'kotlin.Unit' or 'null')
- [`KT-13932`](https://youtrack.jetbrains.com/issue/KT-13932) 'kotlin.Unit' is not materialized in some functions returning supertype of 'Unit' ('undefined' returned instead)
- [`KT-16408`](https://youtrack.jetbrains.com/issue/KT-16408) JS: Inliner loses imported values when extending a class from another module
- [`KT-17014`](https://youtrack.jetbrains.com/issue/KT-17014) Different results in JVM and JavaScript on Unit-returning functions
- [`KT-17915`](https://youtrack.jetbrains.com/issue/KT-17915) JS: 'kotlin.Unit' is not materialized as result of try-catch block expression with empty catch
- [`KT-18166`](https://youtrack.jetbrains.com/issue/KT-18166) JS: Delegated property named with non-identifier symbols can crash in runtime.
- [`KT-18176`](https://youtrack.jetbrains.com/issue/KT-18176) JS: dynamic type should not allow methods and properties with incorrect identifier symbols
- [`KT-18216`](https://youtrack.jetbrains.com/issue/KT-18216) JS: Unit-returning expression used in loop can cause wrong behavior
- [`KT-18793`](https://youtrack.jetbrains.com/issue/KT-18793) Kotlin Javascript compiler null handling generates if-else block where else is always taken
- [`KT-19108`](https://youtrack.jetbrains.com/issue/KT-19108) JS: Inconsistent behaviour from JVM code when modifying variable whilst calling run on it
- [`KT-19495`](https://youtrack.jetbrains.com/issue/KT-19495) JS: Wrong compilation of nested conditions with if- and when-clauses
- [`KT-19540`](https://youtrack.jetbrains.com/issue/KT-19540) JS: prohibit to use illegal symbols on call site
- [`KT-19542`](https://youtrack.jetbrains.com/issue/KT-19542) JS: delegate field should have unique name otherwise it can be accidentally overwritten 
- [`KT-19712`](https://youtrack.jetbrains.com/issue/KT-19712) KotlinJS - providing default value of lambda-argument produces invalid js-code
- [`KT-19793`](https://youtrack.jetbrains.com/issue/KT-19793) build-crash with external varargs (Javascript)
- [`KT-19821`](https://youtrack.jetbrains.com/issue/KT-19821) JS remap sourcemaps in DCE
- [`KT-19891`](https://youtrack.jetbrains.com/issue/KT-19891) Runtime crash with inline function with reified type parameter and object expression: "T_0 is not defined" (JavaScript)
- [`KT-20005`](https://youtrack.jetbrains.com/issue/KT-20005) Invalid source map with option sourceMapEmbedSources = "always" 

### Libraries

- [`KT-19133`](https://youtrack.jetbrains.com/issue/KT-19133) Specialize `any` and `none` for Collection
- [`KT-18267`](https://youtrack.jetbrains.com/issue/KT-18267) Deprecate CharSequence.size extension function on the JS side
- [`KT-18992`](https://youtrack.jetbrains.com/issue/KT-18992) JS: Missing MutableMap.iterator()
- [`KT-19881`](https://youtrack.jetbrains.com/issue/KT-19881) Expand doc comment of @PublishedApi

### Tools. CLI

- [`KT-18859`](https://youtrack.jetbrains.com/issue/KT-18859) Strange error message when kotlin-embeddable-compiler is run without explicit -kotlin-home
- [`KT-19287`](https://youtrack.jetbrains.com/issue/KT-19287) Common module compilation: K2MetadataCompiler ignores coroutines state

### Tools. Gradle

- [`KT-17150`](https://youtrack.jetbrains.com/issue/KT-17150) Support 'packagePrefix' option in Gradle plugin
- [`KT-19956`](https://youtrack.jetbrains.com/issue/KT-19956) Support incremental compilation to JS in Gradle
- [`KT-13918`](https://youtrack.jetbrains.com/issue/KT-13918) Cannot access internal classes/methods in androidTest source set in an Android library module
- [`KT-17355`](https://youtrack.jetbrains.com/issue/KT-17355) Use `archivesBaseName` instead of `project.name` for module names, get rid of `_main` for `main` source set
- [`KT-18183`](https://youtrack.jetbrains.com/issue/KT-18183) Kotlin gradle plugin uses compile task output as "friends directory"
- [`KT-19248`](https://youtrack.jetbrains.com/issue/KT-19248) Documentation suggested way to enable coroutines (gradle) doesn't work
- [`KT-19397`](https://youtrack.jetbrains.com/issue/KT-19397) local.properties file not closed by KotlinProperties.kt

### Tools. Incremental Compile

- [`KT-19580`](https://youtrack.jetbrains.com/issue/KT-19580) IC does not detect  non-nested sealed class addition

### Tools. J2K

- [`KT-10375`](https://youtrack.jetbrains.com/issue/KT-10375) 0xFFFFFFFFFFFFFFFFL conversion issue
- [`KT-13552`](https://youtrack.jetbrains.com/issue/KT-13552) switch-to-when conversion creates broken code
- [`KT-17379`](https://youtrack.jetbrains.com/issue/KT-17379) Converting multiline expressions creates dangling operations
- [`KT-18232`](https://youtrack.jetbrains.com/issue/KT-18232) Kotlin code converter misses annotations
- [`KT-18786`](https://youtrack.jetbrains.com/issue/KT-18786) Convert Kotlin to Java generates error: Variable cannot be initialized before declaration
- [`KT-19523`](https://youtrack.jetbrains.com/issue/KT-19523) J2K produce invalid code when convert some numbers

### Tools. JPS

- [`KT-17397`](https://youtrack.jetbrains.com/issue/KT-17397) Kotlin JPS Builder can mark dirty files already compiled in round
- [`KT-19176`](https://youtrack.jetbrains.com/issue/KT-19176) Java 9: JPS build fails for Kotlin source referring exported Kotlin class from another module: "unresolved supertypes: kotlin.Any"
- [`KT-19833`](https://youtrack.jetbrains.com/issue/KT-19833) Cannot access class/superclass from SDK on compilation of JDK 9 module together with non-9 module

### Tools. REPL

- [`KT-11369`](https://youtrack.jetbrains.com/issue/KT-11369) REPL: Ctrl-C should interrupt the input, Ctrl-D should quit

### Tools. kapt

- [`KT-19996`](https://youtrack.jetbrains.com/issue/KT-19996) Error with 'kotlin-kapt' plugin and dagger2, clean project required

## Previous releases

This release also includes the fixes and improvements from the previous
[`1.1.4`](https://github.com/JetBrains/kotlin/blob/1.1.4/ChangeLog.md) release.
