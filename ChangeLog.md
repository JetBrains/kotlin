# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.1-RC

### Language related changes
- [`KT-7897`](https://youtrack.jetbrains.com/issue/KT-7897) Do not require to call enum constructor for each entry if all parameters have default values
- [`KT-8985`](https://youtrack.jetbrains.com/issue/KT-8985) Support T::class.java for T with no non-null upper bound
- [`KT-10711`](https://youtrack.jetbrains.com/issue/KT-10711) Type inference works now on generics for callable references
- [`KT-13130`](https://youtrack.jetbrains.com/issue/KT-13130) Support exhaustive when for sealed trees
- [`KT-15898`](https://youtrack.jetbrains.com/issue/KT-15898) Cannot use type alias to qualify enum entry
- [`KT-16061`](https://youtrack.jetbrains.com/issue/KT-16061) Smart type inference on callable references in 1.1 mode only

### Reflection
- [`KT-8384`](https://youtrack.jetbrains.com/issue/KT-8384) Access to the delegate object for a KProperty

### Compiler

#### Coroutine support
- [`KT-15016`](https://youtrack.jetbrains.com/issue/KT-15016) VerifyError with coroutine: fix processing of uninitialized instances
- [`KT-15527`](https://youtrack.jetbrains.com/issue/KT-15527) Coroutine compile error: wrong code generated for safe qualified suspension points
- [`KT-15552`](https://youtrack.jetbrains.com/issue/KT-15552) Accessor implementation of suspended function produces AbstractMethodError
- [`KT-15715`](https://youtrack.jetbrains.com/issue/KT-15715) Coroutine generate invalid invoke
- [`KT-15820`](https://youtrack.jetbrains.com/issue/KT-15820) Coroutine Internal Error regression with dispatcher + this@
- [`KT-15821`](https://youtrack.jetbrains.com/issue/KT-15821) Coroutine internal error regression: Could not inline method call apply
- [`KT-15824`](https://youtrack.jetbrains.com/issue/KT-15824) Coroutine iterator regression: Object cannot be cast to java.lang.Boolean
- [`KT-15827`](https://youtrack.jetbrains.com/issue/KT-15827) Show Kotlin Bytecode shows wrong bytecode for suspending functions
- [`KT-15907`](https://youtrack.jetbrains.com/issue/KT-15907) Bogus error about platform declaration clash with private suspend functions
- [`KT-15933`](https://youtrack.jetbrains.com/issue/KT-15933) Suspend getValue/setValue/provideDelegate do not work properly
- [`KT-15935`](https://youtrack.jetbrains.com/issue/KT-15935) Private suspend function in file causes UnsupportedOperationException: Context does not have a "this"
- [`KT-15963`](https://youtrack.jetbrains.com/issue/KT-15963) Coroutine: runtime error if returned object "equals" does not like comparison to SUSPENDED_MARKER
- [`KT-16068`](https://youtrack.jetbrains.com/issue/KT-16068) Prohibit inline lambda parameters of suspend function type

#### Diagnostics
- [`KT-1560`](https://youtrack.jetbrains.com/issue/KT-1560) Report diagnostic for a declaration of extension function which will be always shadowed by member function
- [`KT-12846`](https://youtrack.jetbrains.com/issue/KT-12846) Forbid vararg of Nothing
- [`KT-13227`](https://youtrack.jetbrains.com/issue/KT-13227) NO_ELSE_IN_WHEN in when by sealed class instance if is-check for base sealed class is used
- [`KT-13355`](https://youtrack.jetbrains.com/issue/KT-13355) Type mismatch on inheritance is not reported on abstract class
- [`KT-15010`](https://youtrack.jetbrains.com/issue/KT-15010) Missing error on an usage of non-constant property in annotation default argument 
- [`KT-15201`](https://youtrack.jetbrains.com/issue/KT-15201) Compiler is complaining about when statement without null condition even if null is checked before.
- [`KT-15736`](https://youtrack.jetbrains.com/issue/KT-15736) Report an error on type alias expanded to a nullable type on LHS of a class literal
- [`KT-15740`](https://youtrack.jetbrains.com/issue/KT-15740) Report error on expression of a nullable type on LHS of a class literal
- [`KT-15844`](https://youtrack.jetbrains.com/issue/KT-15844) Do not allow to access primary constructor parameters from property with custom getter
- [`KT-15878`](https://youtrack.jetbrains.com/issue/KT-15878) Extension shadowed by member should not be reported for infix/operator extensions when member is non-infix/operator
- [`KT-16010`](https://youtrack.jetbrains.com/issue/KT-16010) Do not highlight lambda parameters as unused in 1.0 compatibility mode

#### Kapt
- [`KT-15675`](https://youtrack.jetbrains.com/issue/KT-15675) Kapt3 does not generate classes annotated with AutoValue
- [`KT-15803`](https://youtrack.jetbrains.com/issue/KT-15803) Kotlin 1.0.6 broke Dagger
- [`KT-15814`](https://youtrack.jetbrains.com/issue/KT-15814) Regression: Kapt is not working in 1.0.6 / 1.1-M04 / 1.1-Beta
- [`KT-15838`](https://youtrack.jetbrains.com/issue/KT-15838) kapt3 1.1-beta: KaptError: Java file parsing error
- [`KT-15841`](https://youtrack.jetbrains.com/issue/KT-15841) 1.1-Beta + kapt3 fails to build the project with StackOverflowError
- [`KT-15915`](https://youtrack.jetbrains.com/issue/KT-15915) Kapt: Kotlin class target directory is cleared before compilation (and after kapt task)

#### Exceptions / Errors
- [`KT-8264`](https://youtrack.jetbrains.com/issue/KT-8264) Internal compiler error: java.lang.ArithmeticException: BigInteger: modulus not positive
- [`KT-14547`](https://youtrack.jetbrains.com/issue/KT-14547) NoSuchElementException when compiling callable reference without stdlib in the classpath
- [`KT-14966`](https://youtrack.jetbrains.com/issue/KT-14966) Regression: VerifyError on access super implementation from delegate 
- [`KT-15017`](https://youtrack.jetbrains.com/issue/KT-15017) Throwing exception in the end of inline suspend-functions lead to internal compiler error
- [`KT-15439`](https://youtrack.jetbrains.com/issue/KT-15439) Resolved call is not completed for generic callable reference in if-expression
- [`KT-15500`](https://youtrack.jetbrains.com/issue/KT-15500) Exception passing freeCompilerArgs to gradle plugin
- [`KT-15646`](https://youtrack.jetbrains.com/issue/KT-15646) InconsistentDebugInfoException when stepping over `throw`
- [`KT-15726`](https://youtrack.jetbrains.com/issue/KT-15726) Kotlin compiles invalid bytecode for nested try-catch with return
- [`KT-15743`](https://youtrack.jetbrains.com/issue/KT-15743) Overloaded Kotlin extensions annotates wrong parameters in java
- [`KT-15995`](https://youtrack.jetbrains.com/issue/KT-15995) Can't build project with DataBinding using Kotlin 1.1: incompatible language version
- [`KT-16047`](https://youtrack.jetbrains.com/issue/KT-16047) Internal Error: org.jetbrains.kotlin.util.KotlinFrontEndException while analyzing expression

#### Type inference issues
- [`KT-10268`](https://youtrack.jetbrains.com/issue/KT-10268) Wrong type inference related to captured types
- [`KT-11259`](https://youtrack.jetbrains.com/issue/KT-11259) Wrong type inference for Java 8 Stream.collect.
- [`KT-12802`](https://youtrack.jetbrains.com/issue/KT-12802) Type inference failed when irrelevant method reference is used
- [`KT-12964`](https://youtrack.jetbrains.com/issue/KT-12964) Support type inference for callable references from parameter types of an expected function type

#### Smart cast issues
- [`KT-13468`](https://youtrack.jetbrains.com/issue/KT-13468) Smart cast is broken after assignment of 'if' expression
- [`KT-14350`](https://youtrack.jetbrains.com/issue/KT-14350) Make smart-cast work as it does in 1.0 when -language-version 1.0 is used
- [`KT-14597`](https://youtrack.jetbrains.com/issue/KT-14597) When over smartcast enum is broken and breaks all other "when"
- [`KT-15792`](https://youtrack.jetbrains.com/issue/KT-15792) Wrong smart cast after y = x, x = null, y != null sequence

#### Various issues
- [`KT-15236`](https://youtrack.jetbrains.com/issue/KT-15236) False positive: Null can not be a value of a non-null type
- [`KT-15677`](https://youtrack.jetbrains.com/issue/KT-15677) Modifiers and annotations are lost on a (nullable) parenthesized type
- [`KT-15707`](https://youtrack.jetbrains.com/issue/KT-15707) IDEA unable to parallel compile different projects
- [`KT-15734`](https://youtrack.jetbrains.com/issue/KT-15734) Nullability is lost during expansion of a type alias
- [`KT-15748`](https://youtrack.jetbrains.com/issue/KT-15748) Type alias constructor return type should have a corresponding abbreviation
- [`KT-15775`](https://youtrack.jetbrains.com/issue/KT-15775) Annotations are lost on value parameter types of a function type
- [`KT-15780`](https://youtrack.jetbrains.com/issue/KT-15780) Treat Map.getOrDefault overrides in Java the same way as in 1.0.x compiler with language version 1.0
- [`KT-15794`](https://youtrack.jetbrains.com/issue/KT-15794) Refine backward compatibility mode for additional built-ins members from JDK
- [`KT-15848`](https://youtrack.jetbrains.com/issue/KT-15848) Implement additional annotation processing in the `KotlinScriptDefinitionFromAnnotatedTemplate` for SamWithReceiver plugin
- [`KT-15875`](https://youtrack.jetbrains.com/issue/KT-15875) Operation has lead to overflow for 'mod' with negative first operand
- [`KT-15945`](https://youtrack.jetbrains.com/issue/KT-15945) Feature Request: Andrey Breslav to grow a beard.

### JavaScript backend

#### Coroutine support
- [`KT-15834`](https://youtrack.jetbrains.com/issue/KT-15834) JS: Local delegate in suspend function
- [`KT-15892`](https://youtrack.jetbrains.com/issue/KT-15892) JS: safe call of suspend functions causes compiler to crash

#### Diagnostics
- [`KT-14668`](https://youtrack.jetbrains.com/issue/KT-14668) Do not allow declarations in 'kotlin' package or subpackages in JS
- [`KT-15184`](https://youtrack.jetbrains.com/issue/KT-15184) JS: prohibit `..` operation with `dynamic` on left-hand side
- [`KT-15253`](https://youtrack.jetbrains.com/issue/KT-15253) JS: no error when use class external class with JsModule in type context when compiling with plain module kind
- [`KT-15283`](https://youtrack.jetbrains.com/issue/KT-15283) JS: additional restrictions on dynamic
- [`KT-15961`](https://youtrack.jetbrains.com/issue/KT-15961) Could not implement external open class with function with optional parameter

#### Language feature support
- [`KT-14035`](https://youtrack.jetbrains.com/issue/KT-14035) JS: support implementing CharSequence
- [`KT-14036`](https://youtrack.jetbrains.com/issue/KT-14036) JS: use Int16 for Char when it possible and box to our Char otherwise
- [`KT-14097`](https://youtrack.jetbrains.com/issue/KT-14097) Wrong code generated for enum entry initialization using non-primary no-argument constructor
- [`KT-15312`](https://youtrack.jetbrains.com/issue/KT-15312) JS: map kotlin.Throwable to JS Error
- [`KT-15765`](https://youtrack.jetbrains.com/issue/KT-15765) JS: support callable references on built-in and intrinsic functions and properties
- [`KT-15900`](https://youtrack.jetbrains.com/issue/KT-15900) JS: Support enum entry with empty initializer with vararg constructor

#### Standard library support
- [`KT-4141`](https://youtrack.jetbrains.com/issue/KT-4141) JS: wrong return type for Date::getTime
- [`KT-4497`](https://youtrack.jetbrains.com/issue/KT-4497) JS: add String.toInt, String.toDouble etc extension functions, `parseInt` and `parseFloat` are deprecated in favor of these new ones
- [`KT-15940`](https://youtrack.jetbrains.com/issue/KT-15940) JS: rename all js standard library artifacts (both in maven and in compiler distribution) to `kotlin-stdlib-js.jar`
- Add `Promise<T>` external declaration to the standard library
- Types like `Date`, `Math`, `Console`, `Promise`, `RegExp`, `Json` require explicit import from `kotlin.js` package

#### External declarations
- [`KT-15144`](https://youtrack.jetbrains.com/issue/KT-15144) JS: rename `noImpl` to `definedExternally`
- [`KT-15306`](https://youtrack.jetbrains.com/issue/KT-15306) JS: allow to use `definedExternally` only inside a body of external declarations
- [`KT-15336`](https://youtrack.jetbrains.com/issue/KT-15336) JS: allow to inherit external classes from kotlin.Throwable
- [`KT-15905`](https://youtrack.jetbrains.com/issue/KT-15905) JS: add a way to control qualifier for external declarations inside file
- Deprecate `@native` annotation, to be removed in 1.1 release.

#### Exceptions / Errors
- [`KT-10894`](https://youtrack.jetbrains.com/issue/KT-10894) Infinite indexing at projects with JS modules
- [`KT-14124`](https://youtrack.jetbrains.com/issue/KT-14124) AssertionError: strings file not found on K2JS serialized data
 
#### Various issues
- [`KT-8211`](https://youtrack.jetbrains.com/issue/KT-8211) JS: generate dummy init for properties w/o initializer to avoid to have different hidden classes for different instances
- [`KT-12712`](https://youtrack.jetbrains.com/issue/KT-12712) JS: Json should not be a class
- [`KT-13312`](https://youtrack.jetbrains.com/issue/KT-13312) JS: can't use extension lambda where expected lambda and vice versa
- [`KT-13632`](https://youtrack.jetbrains.com/issue/KT-13632) Add template kotlin js project under gradle in "New Project" window
- [`KT-15278`](https://youtrack.jetbrains.com/issue/KT-15278) JS: don't treat property access through dynamic as side effect free
- [`KT-15285`](https://youtrack.jetbrains.com/issue/KT-15285) JS: take into account as many characteristics from the signature as possible when mangling
- [`KT-15678`](https://youtrack.jetbrains.com/issue/KT-15678) JS: Generated local variable named 'element' clashes with actual local variable named 'element'
- [`KT-15755`](https://youtrack.jetbrains.com/issue/KT-15755) JS compiler produces a lot of empty kotlin_file_table files for irrelevant packages
- [`KT-15770`](https://youtrack.jetbrains.com/issue/KT-15770) Name clash between recursive local functions with same name
- [`KT-15797`](https://youtrack.jetbrains.com/issue/KT-15797) JS: wrong code for accessing nested class inside js module
- [`KT-15863`](https://youtrack.jetbrains.com/issue/KT-15863) JS: Extension function reference shifts parameters loosing the receiver
- [`KT-16049`](https://youtrack.jetbrains.com/issue/KT-16049) JS: drop "-kjsm" command line option, merge the logic into "-meta-info"
- [`KT-16083`](https://youtrack.jetbrains.com/issue/KT-16083) JS: rename "-library-files" argument to "-libraries" and change separator from comma to system file separator

### Standard Library
- [`KT-13353`](https://youtrack.jetbrains.com/issue/KT-13353) Add Map.minus(key) and Map.minus(keys) 
- [`KT-13826`](https://youtrack.jetbrains.com/issue/KT-13826) Add parameter names in function types used in the standard library
- [`KT-14279`](https://youtrack.jetbrains.com/issue/KT-14279) Make String.matches(Regex) and Regex.matches(String) infix
- [`KT-15399`](https://youtrack.jetbrains.com/issue/KT-15399) Iterable.average() now returns NaN for an empty collection
- [`KT-15975`](https://youtrack.jetbrains.com/issue/KT-15975) Move coroutine-related runtime parts to `kotlin.coroutines.experimental` package
- [`KT-16030`](https://youtrack.jetbrains.com/issue/KT-16030) Move bitwise operations on Byte and Short to `kotlin.experimental` package
- [`KT-16026`](https://youtrack.jetbrains.com/issue/KT-16026) Classes compiled in 1.1 in 1.0-compatibility mode may contain references to CloseableKt class from 1.1

### IDE

#### Configuration issues
- [`KT-15621`](https://youtrack.jetbrains.com/issue/KT-15621) Copy compiler options values from project settings on creating a kotlin facet for Kotlin (JVM) project
- [`KT-15623`](https://youtrack.jetbrains.com/issue/KT-15623) Copy compiler options values from project settings on creating a kotlin facet for Kotlin (JavaScript) project
- [`KT-15624`](https://youtrack.jetbrains.com/issue/KT-15624) Set option "Use project settings" in newly created Kotlin facet
- [`KT-15712`](https://youtrack.jetbrains.com/issue/KT-15712) Configuring a project with Maven or Gradle should automatically use stdlib-jre7 or stdlib-jre8 instead of standard stdlib
- [`KT-15772`](https://youtrack.jetbrains.com/issue/KT-15772) Facet does not pick up api version from maven
- [`KT-15819`](https://youtrack.jetbrains.com/issue/KT-15819) It would be nice if compileKotlin options are imported into Kotlin facet from gradle/maven
- [`KT-16015`](https://youtrack.jetbrains.com/issue/KT-16015) Prohibit api-version > language-version in Facet and Project Settings

#### Coroutine support
- [`KT-14704`](https://youtrack.jetbrains.com/issue/KT-14704) Extract Method should work in coroutines
- [`KT-15955`](https://youtrack.jetbrains.com/issue/KT-15955) Quick-fix to enable coroutines through Gradle project configuration
- [`KT-16018`](https://youtrack.jetbrains.com/issue/KT-16018) Hide coroutines intrinsics from import and completion
- [`KT-16075`](https://youtrack.jetbrains.com/issue/KT-16075) Error:Kotlin: The -Xcoroutines can only have one value

#### Backward compatibility issues
- [`KT-15134`](https://youtrack.jetbrains.com/issue/KT-15134) Do not suggest using destructuring lambda if this will result in "available since 1.1" error
- [`KT-15918`](https://youtrack.jetbrains.com/issue/KT-15918) Quick fix "Set module language level to 1.1" should also set API version to 1.1
- [`KT-15969`](https://youtrack.jetbrains.com/issue/KT-15969) Replace operator with function should use either rem or mod for % depending on language version
- [`KT-15978`](https://youtrack.jetbrains.com/issue/KT-15978) Type alias from Kotlin 1.1 are suggested in completion even if language level is set to 1.0 in settings
- [`KT-15979`](https://youtrack.jetbrains.com/issue/KT-15979) Usages of type aliases are not shown as errors in editor if language version is set to 1.0
- [`KT-16019`](https://youtrack.jetbrains.com/issue/KT-16019) Do not suggest renaming to underscore in 1.0 compatibility mode
- [`KT-16036`](https://youtrack.jetbrains.com/issue/KT-16036) "Create type alias from usage" quick-fix should not be suggested at language level 1.0

#### Intention actions, inspections and quick-fixes

##### New features
- [`KT-9912`](https://youtrack.jetbrains.com/issue/KT-9912) Merge ifs intention
- [`KT-13427`](https://youtrack.jetbrains.com/issue/KT-13427) "Specify type explicitly" should support type aliases
- [`KT-15066`](https://youtrack.jetbrains.com/issue/KT-15066) "Make private/.." intention on type aliases
- [`KT-15709`](https://youtrack.jetbrains.com/issue/KT-15709) Add inspection for private primary constructors in data classes as they are accessible via the copy method
- [`KT-15738`](https://youtrack.jetbrains.com/issue/KT-15738) Intention to add `suspend` modifier to functional type
- [`KT-15800`](https://youtrack.jetbrains.com/issue/KT-15800) Quick-fix to convert a function to suspending on error when calling suspension inside
- [`KT-15874`](https://youtrack.jetbrains.com/issue/KT-15874) Replace operator with function call replaces % with deprecated mod
- [`KT-16072`](https://youtrack.jetbrains.com/issue/KT-16072) Intentions to convert suspend lambdas to callable references should not be shown

##### Bug fixes
- [`KT-13710`](https://youtrack.jetbrains.com/issue/KT-13710) Import intention action should not appear in import list
- [`KT-14680`](https://youtrack.jetbrains.com/issue/KT-14680) import statement to type alias reported as unused when using only TA constructor
- [`KT-14856`](https://youtrack.jetbrains.com/issue/KT-14856) TextView internationalisation intention does not report the problem
- [`KT-14993`](https://youtrack.jetbrains.com/issue/KT-14993) Keep destructuring declaration parameter on inspection "Remove explicit lambda parameter types"
- [`KT-14994`](https://youtrack.jetbrains.com/issue/KT-14994) PsiInvalidElementAccessException and incorrect generation on inspection "Specify type explicitly" on destructuring parameter
- [`KT-15162`](https://youtrack.jetbrains.com/issue/KT-15162) "Remove explicit lambda parameter types" intentions fails with destructuring declaration with KNPE at KtPsiFactory.createLambdaParameterList()
- [`KT-15311`](https://youtrack.jetbrains.com/issue/KT-15311) "Add Import" intention generates incorrect code
- [`KT-15406`](https://youtrack.jetbrains.com/issue/KT-15406) Convert to secondary constructor for enum class should put new members after enum values
- [`KT-15553`](https://youtrack.jetbrains.com/issue/KT-15553) Copy concatenation text to clipboard with Kotlin and string interpolation does not work
- [`KT-15670`](https://youtrack.jetbrains.com/issue/KT-15670) 'Convert to lambda' quick fix in IDEA leaves single-line comment and } gets commented out
- [`KT-15873`](https://youtrack.jetbrains.com/issue/KT-15873) Alt+Enter menu isn't shown for deprecated mod function
- [`KT-15884`](https://youtrack.jetbrains.com/issue/KT-15884) False positive "Redundant .let call"

#### Android support
- [`KT-13275`](https://youtrack.jetbrains.com/issue/KT-13275) Kotlin Gradle plugin for Android does not work when jackOptions enabled
- [`KT-15150`](https://youtrack.jetbrains.com/issue/KT-15150) Android: Add quick-fix to generate View constructor convention
- [`KT-15282`](https://youtrack.jetbrains.com/issue/KT-15282) Issues debugging crossinline Android code

#### KDoc
- [`KT-14710`](https://youtrack.jetbrains.com/issue/KT-14710) Sample references are not resolved in IDE
- [`KT-15796`](https://youtrack.jetbrains.com/issue/KT-15796) Import of class referenced only in KDoc not preserved after copy-paste

#### Various issues
- [`KT-9011`](https://youtrack.jetbrains.com/issue/KT-9011) Shift+Enter should insert curly braces when invoked after class declaration
- [`KT-11308`](https://youtrack.jetbrains.com/issue/KT-11308) Hide kotlin.jvm.internal package contents from completion and auto-import
- [`KT-14252`](https://youtrack.jetbrains.com/issue/KT-14252) Completion could suggest constructors available via type aliases
- [`KT-14722`](https://youtrack.jetbrains.com/issue/KT-14722) Completion list isn't filled up for type alias to object
- [`KT-14767`](https://youtrack.jetbrains.com/issue/KT-14767) Type alias to annotation class should appear in the completion list
- [`KT-14859`](https://youtrack.jetbrains.com/issue/KT-14859) "Parameter Info" sometimes does not work properly inside lambda
- [`KT-15032`](https://youtrack.jetbrains.com/issue/KT-15032) Injected fragment: descriptor was not found for declaration: FUN
- [`KT-15153`](https://youtrack.jetbrains.com/issue/KT-15153) Support typeAlias extensions in completion and add import
- [`KT-15786`](https://youtrack.jetbrains.com/issue/KT-15786) NoSuchMethodError: com.intellij.util.containers.UtilKt.isNullOrEmpty
- [`KT-15883`](https://youtrack.jetbrains.com/issue/KT-15883) Generating equals() and hashCode(): hashCode does not correctly honor variable names with back ticks
- [`KT-15911`](https://youtrack.jetbrains.com/issue/KT-15911) Kotlin REPL will not launch: "Neither main class nor JAR path is specified"

### J2K
- [`KT-15789`](https://youtrack.jetbrains.com/issue/KT-15789) Kotlin plugin incorrectly converts for-loops from Java to Kotlin

### Gradle support
- [`KT-14830`](https://youtrack.jetbrains.com/issue/KT-14830) Kotlin Gradle plugin configuration should not add 'kotlin' source directory by default
- [`KT-15279`](https://youtrack.jetbrains.com/issue/KT-15279) 'Kotlin not configured message' should not be displayed while gradle sync is in progress
- [`KT-15812`](https://youtrack.jetbrains.com/issue/KT-15812) Create Kotlin facet on importing gradle project with unchecked option Create separate module per source set
- [`KT-15837`](https://youtrack.jetbrains.com/issue/KT-15837) Gradle compiler attempts to connect to daemon on address derived from DNS lookup
- [`KT-15909`](https://youtrack.jetbrains.com/issue/KT-15909) Copy Gradle compiler options to facets in Intellij/AS
- [`KT-15929`](https://youtrack.jetbrains.com/issue/KT-15929) Gradle project imported with wrong 'target platform'

### Other issues
- [`KT-15450`](https://youtrack.jetbrains.com/issue/KT-15450) JSR 223 - support eval with bindings

### Previous releases

This release also includes the fixes and improvements from the previous
[`1.1-Beta`](https://github.com/JetBrains/kotlin/blob/1.1-Beta/ChangeLog.md) release.
