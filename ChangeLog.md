# CHANGELOG

## 1.3.30

### Compiler

#### New Features

- [`KT-19664`](https://youtrack.jetbrains.com/issue/KT-19664) Allow more permissive visibility for non-virtual actual declarations
- [`KT-29586`](https://youtrack.jetbrains.com/issue/KT-29586) Add support for Android platform annotations
- [`KT-29604`](https://youtrack.jetbrains.com/issue/KT-29604) Do not implicitly propagate deprecations originated in Java

#### Performance Improvements

- [`KT-24876`](https://youtrack.jetbrains.com/issue/KT-24876) Emit calls to java.lang.Long.divideUnsigned for unsigned types when target version is 1.8
- [`KT-25974`](https://youtrack.jetbrains.com/issue/KT-25974) 'when' by unsigned integers is not translated to tableswitch/lookupswitch
- [`KT-28015`](https://youtrack.jetbrains.com/issue/KT-28015) Coroutine state-machine shall use Result.throwOnFailure
- [`KT-29229`](https://youtrack.jetbrains.com/issue/KT-29229) Intrinsify 'in' operator for unsigned integer ranges
- [`KT-29230`](https://youtrack.jetbrains.com/issue/KT-29230) Specialize 'next' method call for unsigned integer range and progression iterators

#### Fixes

- [`KT-7185`](https://youtrack.jetbrains.com/issue/KT-7185) Parse import directives in the middle of the file, report a diagnostic instead
- [`KT-7237`](https://youtrack.jetbrains.com/issue/KT-7237) Parser recovery (angle bracket mismatch)
- [`KT-11656`](https://youtrack.jetbrains.com/issue/KT-11656) Could not generate LightClass because of ISE from bridge generation on invalid code
- [`KT-13497`](https://youtrack.jetbrains.com/issue/KT-13497) Better recovery in comma-separated lists in case of missing comma
- [`KT-13703`](https://youtrack.jetbrains.com/issue/KT-13703) Restore parser better when `class` is missing from `enum` declaration
- [`KT-13731`](https://youtrack.jetbrains.com/issue/KT-13731) Recover parser on value parameter without a type
- [`KT-14227`](https://youtrack.jetbrains.com/issue/KT-14227) Incorrect code is generated when using MutableMap.set with plusAssign operator
- [`KT-19389`](https://youtrack.jetbrains.com/issue/KT-19389) Couldn't inline method call 'with'
- [`KT-20065`](https://youtrack.jetbrains.com/issue/KT-20065) "Cannot serialize error type: [ERROR : Unknown type parameter 0]" with generic typealias
- [`KT-20322`](https://youtrack.jetbrains.com/issue/KT-20322) Debug: member value returned from suspending function is not updated immediately 
- [`KT-20780`](https://youtrack.jetbrains.com/issue/KT-20780) "Cannot serialize error type: [ERROR : Unknown type parameter 0]" with parameterized inner type alias
- [`KT-21405`](https://youtrack.jetbrains.com/issue/KT-21405) Throwable “Rewrite at slice LEXICAL_SCOPE key: VALUE_PARAMETER_LIST” on editing string literal in kotlin-js module
- [`KT-21775`](https://youtrack.jetbrains.com/issue/KT-21775) "Cannot serialize error type: [ERROR : Unknown type parameter 0]" with typealias used from a different module
- [`KT-22818`](https://youtrack.jetbrains.com/issue/KT-22818) "UnsupportedOperationException: Don't know how to generate outer expression" on using non-trivial expression in default argument of `expect` function
- [`KT-23117`](https://youtrack.jetbrains.com/issue/KT-23117) Local delegate + local object = NoSuchMethodError
- [`KT-23701`](https://youtrack.jetbrains.com/issue/KT-23701) Report error when -Xmultifile-parts-inherit is used and relevant JvmMultifileClass parts have any state
- [`KT-23992`](https://youtrack.jetbrains.com/issue/KT-23992) Target prefixes for annotations on supertype list elements are not checked
- [`KT-24490`](https://youtrack.jetbrains.com/issue/KT-24490) Wrong type is inferred when last expression in lambda has functional type
- [`KT-24871`](https://youtrack.jetbrains.com/issue/KT-24871) Optimize iteration and contains for UIntRange/ULongRange
- [`KT-24964`](https://youtrack.jetbrains.com/issue/KT-24964) "Cannot serialize error type: [ERROR : Unknown type parameter 0]" with `Validated` typealias from Arrow
- [`KT-25383`](https://youtrack.jetbrains.com/issue/KT-25383) Named function as last statement in lambda doesn't coerce to Unit
- [`KT-25431`](https://youtrack.jetbrains.com/issue/KT-25431) Type mismatch when trying to bind mutable property with complex common system
- [`KT-25435`](https://youtrack.jetbrains.com/issue/KT-25435) Try/catch as the last expression of lambda cause type mismatch
- [`KT-25437`](https://youtrack.jetbrains.com/issue/KT-25437) Type variable fixation of postponed arguments and type variables with Nothing constraint
- [`KT-25446`](https://youtrack.jetbrains.com/issue/KT-25446) Empty labeled return doesn't force coercion to Unit
- [`KT-26069`](https://youtrack.jetbrains.com/issue/KT-26069) NoSuchMethodError on calling remove/getOrDefault on a Kotlin subclass of Java subclass of Map
- [`KT-26638`](https://youtrack.jetbrains.com/issue/KT-26638) Check for repeatablilty of annotations doesn't take into account annotations with use-site target
- [`KT-26816`](https://youtrack.jetbrains.com/issue/KT-26816) Lambdas to Nothing is inferred if multilevel collections is used (listOf, mapOf, etc)
- [`KT-27190`](https://youtrack.jetbrains.com/issue/KT-27190) State machine elimination after inlining stopped working (regression)
- [`KT-27241`](https://youtrack.jetbrains.com/issue/KT-27241) Contracts: smartcasts don't work correctly if type checking for contract function is used
- [`KT-27565`](https://youtrack.jetbrains.com/issue/KT-27565) Lack of fallback resolution for SAM conversions for Kotlin functions in new inference
- [`KT-27799`](https://youtrack.jetbrains.com/issue/KT-27799) Prohibit references to reified type parameters in annotation arguments in local classes / anonymous objects
- [`KT-28182`](https://youtrack.jetbrains.com/issue/KT-28182) Kotlin Bytecode tool window shows incorrect output on annotated property with backing field
- [`KT-28236`](https://youtrack.jetbrains.com/issue/KT-28236) "Cannot serialize error type: [ERROR : Unknown type parameter 2]" with inferred type arguments in generic extension function from Arrow
- [`KT-28309`](https://youtrack.jetbrains.com/issue/KT-28309) Do not generate LVT entries with different types pointing to the same slot, but have different types
- [`KT-28317`](https://youtrack.jetbrains.com/issue/KT-28317) Strange behavior in testJvmAssertInlineFunctionAssertionsEnabled on Jdk 6 and exception on JDK 8
- [`KT-28453`](https://youtrack.jetbrains.com/issue/KT-28453) Mark anonymous classes for callable references as synthetic
- [`KT-28598`](https://youtrack.jetbrains.com/issue/KT-28598) Type is inferred incorrectly to Any on a deep generic type with out projection
- [`KT-28654`](https://youtrack.jetbrains.com/issue/KT-28654) No report about type mismatch inside a lambda in generic functions with a type parameter as a return type
- [`KT-28670`](https://youtrack.jetbrains.com/issue/KT-28670) Not null smartcasts on an intersection of nullable types don't work
- [`KT-28718`](https://youtrack.jetbrains.com/issue/KT-28718) progressive mode plus new inference result in different floating-point number comparisons
- [`KT-28810`](https://youtrack.jetbrains.com/issue/KT-28810) Suspend function's continuation parameter is missing from LVT
- [`KT-28855`](https://youtrack.jetbrains.com/issue/KT-28855) NoSuchMethodError with vararg of unsigned Int in generic class constructor
- [`KT-28984`](https://youtrack.jetbrains.com/issue/KT-28984) Exception when subtype of kotlin.Function is used as an expected one for lambda or callable reference
- [`KT-28993`](https://youtrack.jetbrains.com/issue/KT-28993) Incorrect behavior when two lambdas are passed outside a parenthesized argument list
- [`KT-29144`](https://youtrack.jetbrains.com/issue/KT-29144) Interface with companion object generates invalid bytecode in progressive mode
- [`KT-29228`](https://youtrack.jetbrains.com/issue/KT-29228) Intrinsify 'for' loop for unsigned integer ranges and progressions
- [`KT-29324`](https://youtrack.jetbrains.com/issue/KT-29324) Warnings indexing jdk 11 classes
- [`KT-29367`](https://youtrack.jetbrains.com/issue/KT-29367) New inference doesn't wrap annotated type from java to TypeWithEnhancement
- [`KT-29507`](https://youtrack.jetbrains.com/issue/KT-29507) @field-targeted annotation on property with both getter and setter is absent from bytecode
- [`KT-29705`](https://youtrack.jetbrains.com/issue/KT-29705) 'Rewrite at slice CONSTRUCTOR` of JS class while editing another JVM-class
- [`KT-29792`](https://youtrack.jetbrains.com/issue/KT-29792) UnsupportedOperationException: Unsupported annotation argument type when using Java annotation with infinity or NaN as a default value
- [`KT-29891`](https://youtrack.jetbrains.com/issue/KT-29891) Kotlin doesn't allow to use local class literals as annotation arguments
- [`KT-29912`](https://youtrack.jetbrains.com/issue/KT-29912) Crossinline nonsuspend lambda leads to KNPE during inlining
- [`KT-29965`](https://youtrack.jetbrains.com/issue/KT-29965) Don't generate annotation on $default  method
- [`KT-30030`](https://youtrack.jetbrains.com/issue/KT-30030) Extensive 'Rewrite at slice'-exception with contracts in JS module of multiplatform project
- [`KT-22043`](https://youtrack.jetbrains.com/issue/KT-22043) Report an error when comparing enum (==/!=/when) to any other incompatible type since 1.4
- [`KT-26150`](https://youtrack.jetbrains.com/issue/KT-26150) KotlinFrontendException is thrown when callsInPlace called twice with different InvocationKind in functions with contracts
- [`KT-26153`](https://youtrack.jetbrains.com/issue/KT-26153) Contract is allowed when it's at the beginning in control flow terms, but not in tokens order terms (contract doesn't work)
- [`KT-26191`](https://youtrack.jetbrains.com/issue/KT-26191) Contract may not be the first statement if it's part of the expression
- [`KT-29178`](https://youtrack.jetbrains.com/issue/KT-29178) Prohibit arrays of reified type parameters in annotation arguments in local classes / anonymous objects
- [`KT-20507`](https://youtrack.jetbrains.com/issue/KT-20507) PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL not reported for generic base class constructor call, IAE at run-time
- [`KT-20849`](https://youtrack.jetbrains.com/issue/KT-20849) Inference results in Nothing type argument in case of passing 'out T' to 'in T1'
- [`KT-28285`](https://youtrack.jetbrains.com/issue/KT-28285) NullPointerException on calling Array constructor compiled via Excelsior JET
- [`KT-29376`](https://youtrack.jetbrains.com/issue/KT-29376) Report a deprecation warning when comparing enum to any other incompatible type
- [`KT-29884`](https://youtrack.jetbrains.com/issue/KT-29884) Report warning on @Synchronized on inline method
- [`KT-30073`](https://youtrack.jetbrains.com/issue/KT-30073) ClassCastException on coroutine start with crossinline lambda
- [`KT-30597`](https://youtrack.jetbrains.com/issue/KT-30597) "Extend selection" throws exception in empty class body case
- [`KT-29492`](https://youtrack.jetbrains.com/issue/KT-29492) Double cross-inline of suspending functions produces incorrect code
- [`KT-30508`](https://youtrack.jetbrains.com/issue/KT-30508) Wrong file name in metadata of suspend function capturing crossinline lambda
- [`KT-30679`](https://youtrack.jetbrains.com/issue/KT-30679) "KotlinFrontEndException: Front-end Internal error: Failed to analyze declaration" exception during a compilation of a multiplatform project containing Kotlin Script File

### IDE

#### New Features

- [`KT-26950`](https://youtrack.jetbrains.com/issue/KT-26950) Support Multiline TODO comments
- [`KT-29034`](https://youtrack.jetbrains.com/issue/KT-29034) Make JvmDeclarationSearch find private fields in kotlin classes

#### Performance Improvements

- [`KT-29457`](https://youtrack.jetbrains.com/issue/KT-29457) FindImplicitNothingAction#update freezes UI for 30 secs
- [`KT-29551`](https://youtrack.jetbrains.com/issue/KT-29551) CreateKotlinSdkActivity runs on UI thread

#### Fixes

- [`KT-11143`](https://youtrack.jetbrains.com/issue/KT-11143) Do not insert closing brace for string template between open brace and identifier
- [`KT-18503`](https://youtrack.jetbrains.com/issue/KT-18503) Optimize imports produces red code
- [`KT-27283`](https://youtrack.jetbrains.com/issue/KT-27283) KotlinULiteralExpression and PsiLanguageInjectionHost mismatch
- [`KT-27794`](https://youtrack.jetbrains.com/issue/KT-27794) KotlinAnnotatedElementsSearcher doesn't process method parameters
- [`KT-28272`](https://youtrack.jetbrains.com/issue/KT-28272) UAST: Need to be able to identify SAM conversions
- [`KT-28360`](https://youtrack.jetbrains.com/issue/KT-28360) Getting tons of "There are 2 classes with same fqName" logs in IntelliJ
- [`KT-28739`](https://youtrack.jetbrains.com/issue/KT-28739) Bad caret position after `Insert curly braces around variable` inspection
- [`KT-29013`](https://youtrack.jetbrains.com/issue/KT-29013) Injection with interpolation loses suffix
- [`KT-29025`](https://youtrack.jetbrains.com/issue/KT-29025) Implement `UReferenceExpression.referenceNameElement` for Kotlin
- [`KT-29287`](https://youtrack.jetbrains.com/issue/KT-29287) Exception in ultra-light classes on method annotated with @Throws
- [`KT-29381`](https://youtrack.jetbrains.com/issue/KT-29381) Highlight return lambda expressions when cursor is one the call with lambda argument
- [`KT-29434`](https://youtrack.jetbrains.com/issue/KT-29434) Can not detect injection host in string passed as argument into arrayOf() function
- [`KT-29464`](https://youtrack.jetbrains.com/issue/KT-29464) Project reopening does not create missing Kotlin SDK for Native modules (like it does for other non-JVM ones)
- [`KT-29467`](https://youtrack.jetbrains.com/issue/KT-29467) Maven/Gradle re-import does not add missing Kotlin SDK for kotlin2js modules (non-MPP JavaScript)
- [`KT-29804`](https://youtrack.jetbrains.com/issue/KT-29804) Probable error in the "Kotlin (Mobile Android/iOS)" new project template in IntelliJ
- [`KT-30033`](https://youtrack.jetbrains.com/issue/KT-30033) UAST: Delegation expression missing from parse tree
- [`KT-30388`](https://youtrack.jetbrains.com/issue/KT-30388) Disable constant exception reporting from release versions
- [`KT-30524`](https://youtrack.jetbrains.com/issue/KT-30524) "java.lang.IllegalStateException: This method shouldn't be invoked for LOCAL visibility" on add import
- [`KT-30534`](https://youtrack.jetbrains.com/issue/KT-30534) KotlinUObjectLiteralExpression returns classReference whose referenceNameElement is null
- [`KT-30546`](https://youtrack.jetbrains.com/issue/KT-30546) Kotlin UImportStatement's children references always resolve to null
- [`KT-5435`](https://youtrack.jetbrains.com/issue/KT-5435) Surround with try/catch should generate more Kotlin-style code

### IDE. Android

- [`KT-29847`](https://youtrack.jetbrains.com/issue/KT-29847) Many IDEA plugins are not loaded in presence of Kotlin plugin: "Plugins should not have cyclic dependencies"

### IDE. Code Style, Formatting

- [`KT-23295`](https://youtrack.jetbrains.com/issue/KT-23295) One-line comment indentation in functions with expression body 
- [`KT-28905`](https://youtrack.jetbrains.com/issue/KT-28905) When is "... if long" hitting?
- [`KT-29304`](https://youtrack.jetbrains.com/issue/KT-29304) Settings / Code Style / Kotlin mentions "methods" instead of functions
- [`KT-26954`](https://youtrack.jetbrains.com/issue/KT-26954) Bad indentation for single function with expression body in new code style

### IDE. Completion

- [`KT-18663`](https://youtrack.jetbrains.com/issue/KT-18663) Support "smart enter/complete statement" completion for method calls
- [`KT-28394`](https://youtrack.jetbrains.com/issue/KT-28394) Improve code completion for top level class/interface to incorporate filename
- [`KT-29435`](https://youtrack.jetbrains.com/issue/KT-29435) org.jetbrains.kotlin.types.TypeUtils.contains hanging forever and freezing IntelliJ
- [`KT-27915`](https://youtrack.jetbrains.com/issue/KT-27915) Stop auto-completing braces for companion objects

### IDE. Debugger

- [`KT-22250`](https://youtrack.jetbrains.com/issue/KT-22250) Evaluate: 'this' shows different values when evaluated as a variable/watch
- [`KT-24829`](https://youtrack.jetbrains.com/issue/KT-24829) Access to coroutineContext in 'Evaluate expression'
- [`KT-25220`](https://youtrack.jetbrains.com/issue/KT-25220) Evaluator: a instance of Pair returned instead of String ("Extract function" failed)
- [`KT-25222`](https://youtrack.jetbrains.com/issue/KT-25222) Evaluate: ClassCastException: ObjectValue cannot be cast to IntValue ("Extract function" failed)
- [`KT-26913`](https://youtrack.jetbrains.com/issue/KT-26913) Change local variable name mangling ($receiver -> this_<label>)
- [`KT-28087`](https://youtrack.jetbrains.com/issue/KT-28087) [Kotlin/JVM view] Inconsistent debugging data inside forEachIndexed
- [`KT-28134`](https://youtrack.jetbrains.com/issue/KT-28134) Separate JVM/Kotlin views in "Variables" tool window
- [`KT-28192`](https://youtrack.jetbrains.com/issue/KT-28192) Exception from KotlinEvaluator: cannot find local variable
- [`KT-28680`](https://youtrack.jetbrains.com/issue/KT-28680) Missing `this` word completion in "Evaluate expression" window
- [`KT-28728`](https://youtrack.jetbrains.com/issue/KT-28728) Async stack trace support for Kotlin coroutines
- [`KT-21650`](https://youtrack.jetbrains.com/issue/KT-21650) Debugger: Can't evaluate value, resolution error
- [`KT-23828`](https://youtrack.jetbrains.com/issue/KT-23828) Debugger: "Smart cast is impossible" when evaluating expression
- [`KT-29661`](https://youtrack.jetbrains.com/issue/KT-29661) Evaluate expression: "Cannot find local variable" for variable name escaped with backticks
- [`KT-29814`](https://youtrack.jetbrains.com/issue/KT-29814) Can't evaluate a property on star-projected type
- [`KT-29871`](https://youtrack.jetbrains.com/issue/KT-29871) Debugger in IDE does not handle correctly extensions.
- [`KT-30182`](https://youtrack.jetbrains.com/issue/KT-30182) Incorrect KT elvis expression debugger evaluation
- [`KT-29189`](https://youtrack.jetbrains.com/issue/KT-29189) [BE] 'Step Over' falls through 'return when' (and 'return if') instead of executing individual branches
- [`KT-29234`](https://youtrack.jetbrains.com/issue/KT-29234) ISE “@NotNull method org/jetbrains/kotlin/codegen/binding/CodegenBinding.anonymousClassForCallable must not return null” on debugging with breakpoints in Kotlin script file
- [`KT-29423`](https://youtrack.jetbrains.com/issue/KT-29423) Unable to evaluate lambdas on jdk 9-11
- [`KT-30220`](https://youtrack.jetbrains.com/issue/KT-30220) Empty variables view when breakpoint inside an lambda inside class
- [`KT-30318`](https://youtrack.jetbrains.com/issue/KT-30318) KotlinCoroutinesAsyncStackTraceProvider slows down java debugging
- [`KT-17811`](https://youtrack.jetbrains.com/issue/KT-17811) Couldn't inline method error for inline method with anonymous object initialization and reified type parameter
- [`KT-30611`](https://youtrack.jetbrains.com/issue/KT-30611) Debugger: in projects with stdlib of 1.2.n version Frames view can't complete loading, EvaluateException: "Method threw 'java.lang.ClassNotFoundException' exception." at EvaluateExceptionUtil.createEvaluateException()

### IDE. Decompiler

- [`KT-9618`](https://youtrack.jetbrains.com/issue/KT-9618) Exception in ClassClsStubBuilder.createNestedClassStub() while opening recent project
- [`KT-29427`](https://youtrack.jetbrains.com/issue/KT-29427) Exception in ClassClsStubBuilder.createNestedClassStub() for obfuscated library

### IDE. Gradle

- [`KT-26865`](https://youtrack.jetbrains.com/issue/KT-26865) Gradle build in IDE: error messages in Native sources are not hyperlinks
- [`KT-28515`](https://youtrack.jetbrains.com/issue/KT-28515) Failed to import Kotlin project with gradle 5.0
- [`KT-29564`](https://youtrack.jetbrains.com/issue/KT-29564) kotlin.parallel.tasks.in.project=true causes idea to create kotlin modules with target JVM 1.6
- [`KT-30076`](https://youtrack.jetbrains.com/issue/KT-30076) Memory leaks in Kotlin import
- [`KT-30379`](https://youtrack.jetbrains.com/issue/KT-30379) Gradle 5.3 publishes an MPP with broken Maven scope mapping

### IDE. Gradle. Script

- [`KT-27684`](https://youtrack.jetbrains.com/issue/KT-27684) Gradle Kotlin DSL: the `rootProject` field is unresolved in IDEA for a common module
- [`KT-29465`](https://youtrack.jetbrains.com/issue/KT-29465) IndexNotReadyException on context menu invocation for build.gradle.kts file
- [`KT-29707`](https://youtrack.jetbrains.com/issue/KT-29707) "Navigate declaration" navigates to compiled class in gradle cache folder instead of classes defined in gradle buildSrc folder
- [`KT-29832`](https://youtrack.jetbrains.com/issue/KT-29832) Multiple Script Definitions for settings.gradle.kts
- [`KT-30623`](https://youtrack.jetbrains.com/issue/KT-30623) Errors in build.gradle.kts after applying new script dependencies
- [`KT-29474`](https://youtrack.jetbrains.com/issue/KT-29474) Regression in 1.3.20: Kotlin IDE plugin parses all *.gradle.kts files when any class in buildSrc is opened
- [`KT-30130`](https://youtrack.jetbrains.com/issue/KT-30130) “Access is allowed from event dispatch thread only.” from ScriptNewDependenciesNotificationKt.removeScriptDependenciesNotificationPanel() on creating foo.gradle.kts files in IJ from master

### IDE. Hints

- [`KT-29196`](https://youtrack.jetbrains.com/issue/KT-29196) Variable type hints are redundant for constructor calls of nested classes
- [`KT-30058`](https://youtrack.jetbrains.com/issue/KT-30058) IndexNotReadyException from quick documentation when popup is active

### IDE. Hints. Inlay

- [`KT-19558`](https://youtrack.jetbrains.com/issue/KT-19558) Wrong position of type hint while renaming Kotlin variable
- [`KT-27438`](https://youtrack.jetbrains.com/issue/KT-27438) "Show lambda return expression hints" breaks code indentation
- [`KT-28870`](https://youtrack.jetbrains.com/issue/KT-28870) Rework "Lambda return expression" hint as between_lines_hint of disable it by default

### IDE. Hints. Parameter Info

- [`KT-29574`](https://youtrack.jetbrains.com/issue/KT-29574) Incorrect parameter info popup for lambda nested in object

### IDE. Inspections and Intentions

#### New Features

- [`KT-16118`](https://youtrack.jetbrains.com/issue/KT-16118) "Introduce import alias" intention
- [`KT-17119`](https://youtrack.jetbrains.com/issue/KT-17119) Inspection for (Scala-like) `= { ... }` syntax without expected type in function definition
- [`KT-26128`](https://youtrack.jetbrains.com/issue/KT-26128) Inspection for suspension inside synchronized and withLock functions
- [`KT-27556`](https://youtrack.jetbrains.com/issue/KT-27556) Add intention for collections, !collection.isEmpty() -> collection.isNotEmpty()
- [`KT-27670`](https://youtrack.jetbrains.com/issue/KT-27670) Add quick fix: wrap expression in a lambda if compatible functional type is required
- [`KT-28803`](https://youtrack.jetbrains.com/issue/KT-28803) Inspection: result of enum entries comparison is always false / true
- [`KT-28953`](https://youtrack.jetbrains.com/issue/KT-28953) Add intention to add underscores to decimal numerical literal
- [`KT-29001`](https://youtrack.jetbrains.com/issue/KT-29001) Add intention to move variable declaration before when-expression into when's subject
- [`KT-29113`](https://youtrack.jetbrains.com/issue/KT-29113) Warn about  redundant requireNotNull and checkNotNull usages
- [`KT-29321`](https://youtrack.jetbrains.com/issue/KT-29321) "Remove empty primary constructor": apply for enum entries
- [`KT-12134`](https://youtrack.jetbrains.com/issue/KT-12134) Suggest to remove qualifier in FQN name
- [`KT-17278`](https://youtrack.jetbrains.com/issue/KT-17278) Inspection to replace Java 8 Map.forEach with Kotlin's forEach
- [`KT-26965`](https://youtrack.jetbrains.com/issue/KT-26965) Add inspection + quickfix for replacing Collection<T>.count() with .size
- [`KT-30123`](https://youtrack.jetbrains.com/issue/KT-30123) Add intention to replace isEmpty/isNotEmpty method negation
- [`KT-25272`](https://youtrack.jetbrains.com/issue/KT-25272) Unused expression as last expression of normal function should have quickfix to add "return"
- [`KT-30456`](https://youtrack.jetbrains.com/issue/KT-30456) Improve: intention "Introduce Import Alias" should suggest new names for the new alias.

#### Fixes

- [`KT-7593`](https://youtrack.jetbrains.com/issue/KT-7593) On splitting property declaration for functional expression additional bracket added
- [`KT-12273`](https://youtrack.jetbrains.com/issue/KT-12273) "Replace with operator" intention is suggested for some non-operator functions and produces invalid code
- [`KT-18715`](https://youtrack.jetbrains.com/issue/KT-18715) Replace if with elvis swallows comments
- [`KT-19254`](https://youtrack.jetbrains.com/issue/KT-19254) Intention to convert object literal to class always creates a class named "O"
- [`KT-25501`](https://youtrack.jetbrains.com/issue/KT-25501) "Replace overloaded operator with function call" changes semantics of increment and decrement operators
- [`KT-26979`](https://youtrack.jetbrains.com/issue/KT-26979) "Lambda argument inside parentheses" inspection is not reported, if function type is actual type argument, but not formal parameter type
- [`KT-27143`](https://youtrack.jetbrains.com/issue/KT-27143) Intention "Replace camel-case name with spaces" is suggested for snake_case names in test functions and renames them incorrectly
- [`KT-28081`](https://youtrack.jetbrains.com/issue/KT-28081) "Convert to lambda" changes expression type for interface with multiple supertypes
- [`KT-28131`](https://youtrack.jetbrains.com/issue/KT-28131) False positive "Redundant lambda arrow" with a functional type argument
- [`KT-28224`](https://youtrack.jetbrains.com/issue/KT-28224) "Add braces to 'else' statement" moves comment outside braces when 'if-else' is inside 'if / when' branch
- [`KT-28592`](https://youtrack.jetbrains.com/issue/KT-28592) False positive "Remove redundant backticks" for underscore variable name
- [`KT-28596`](https://youtrack.jetbrains.com/issue/KT-28596) "Can be replaced with binary operator" shouldn't be suggested when receiver or argument is floating point type
- [`KT-28641`](https://youtrack.jetbrains.com/issue/KT-28641) "Remove useless cast" produces a dangling lambda ("Too many arguments" error)
- [`KT-28698`](https://youtrack.jetbrains.com/issue/KT-28698) "Convert to apply" intention: include function calls with `this` passed as an argument
- [`KT-28773`](https://youtrack.jetbrains.com/issue/KT-28773) Kotlin/JS: Wrong inspection to replace .equals() with == on dynamic values
- [`KT-28851`](https://youtrack.jetbrains.com/issue/KT-28851) 'Convert parameter to receiver' adds `Array<out T>` wrapper to `vararg` parameter and drops `override` modifier in implementations
- [`KT-28969`](https://youtrack.jetbrains.com/issue/KT-28969) TYPE_MISMATCH in array vs non-array case: two quick fixes exist for annotation and none of them adds array literal
- [`KT-28995`](https://youtrack.jetbrains.com/issue/KT-28995) "Add parameter to constructor" quickfix for first enum member changes arguments for all members
- [`KT-29051`](https://youtrack.jetbrains.com/issue/KT-29051) "Add parameter to constructor" quickfix for not-first enum member: "PsiInvalidElementAccessException: Element: class org.jetbrains.kotlin.psi.KtStringTemplateExpression #kotlin  because: different providers"
- [`KT-29052`](https://youtrack.jetbrains.com/issue/KT-29052) "Add parameter to constructor" quickfix for not-first enum member inserts FQN type for parameter
- [`KT-29056`](https://youtrack.jetbrains.com/issue/KT-29056) KNPE in ConvertPrimaryConstructorToSecondary with missing property identifier
- [`KT-29085`](https://youtrack.jetbrains.com/issue/KT-29085) False positive "Class member can have 'private' visibility" for a `const val` used in a public inline function
- [`KT-29093`](https://youtrack.jetbrains.com/issue/KT-29093) False positive inspection "Redundant lambda arrow" with nested lambdas
- [`KT-29099`](https://youtrack.jetbrains.com/issue/KT-29099) "Convert to apply" intention is not available for a single function call
- [`KT-29128`](https://youtrack.jetbrains.com/issue/KT-29128) False positive 'Explicitly given type is redundant here' when typealias is used
- [`KT-29153`](https://youtrack.jetbrains.com/issue/KT-29153) False negative "'rangeTo' or the '..' call should be replaced with 'until'" with bracketed expressions
- [`KT-29193`](https://youtrack.jetbrains.com/issue/KT-29193) Quick fix "Create extension function" `List<Int>.set` should not be suggested for read-only collections
- [`KT-29238`](https://youtrack.jetbrains.com/issue/KT-29238) Non-canonical modifiers order inspection incorrectly includes annotations into range
- [`KT-29248`](https://youtrack.jetbrains.com/issue/KT-29248) "Convert member to extension" doesn't preserve visibility
- [`KT-29416`](https://youtrack.jetbrains.com/issue/KT-29416) False positive "Redundant property getter" for `external` getter
- [`KT-29469`](https://youtrack.jetbrains.com/issue/KT-29469) False positive in "Boolean literal argument without parameter name" inspection for varargs parameters
- [`KT-29549`](https://youtrack.jetbrains.com/issue/KT-29549) Make package name convention inspection global
- [`KT-29567`](https://youtrack.jetbrains.com/issue/KT-29567) "Remove empty class body" is a poor name for inspection text
- [`KT-29606`](https://youtrack.jetbrains.com/issue/KT-29606) Do not propose to remove unused parameter of property setter
- [`KT-29763`](https://youtrack.jetbrains.com/issue/KT-29763) False negative "Object literal can be converted to lambda" for block body function with explicit return
- [`KT-30007`](https://youtrack.jetbrains.com/issue/KT-30007) False negative "Add import for '...'" in UserType
- [`KT-19944`](https://youtrack.jetbrains.com/issue/KT-19944) multiplatform: Convert expect/actual function to property should keep the caret on the converted function
- [`KT-27289`](https://youtrack.jetbrains.com/issue/KT-27289) "Create" quick fix on FQN does nothing with KNPE at KotlinRefactoringUtilKt$chooseContainerElement$1.renderText()
- [`KT-29312`](https://youtrack.jetbrains.com/issue/KT-29312) "Make constructor parameter a property" produces wrong modifier order + exception "Invalid range specified"
- [`KT-29414`](https://youtrack.jetbrains.com/issue/KT-29414) "Main parameter is not necessary" inspection reports parameter of `main()` in object
- [`KT-29499`](https://youtrack.jetbrains.com/issue/KT-29499) "Unsafe call of inline function with nullable extension receiver" inspection ignores inferred nullability
- [`KT-29927`](https://youtrack.jetbrains.com/issue/KT-29927) Missing "Import members from" intention with type check operator in `when` branch
- [`KT-30010`](https://youtrack.jetbrains.com/issue/KT-30010) Introduce alternative quick-fixes for `map[key]!!`
- [`KT-30166`](https://youtrack.jetbrains.com/issue/KT-30166) False positive "Redundant companion reference" on companion with the outer class name
- [`KT-14886`](https://youtrack.jetbrains.com/issue/KT-14886) Create Property from Usage should place generated property next to other properties
- [`KT-16139`](https://youtrack.jetbrains.com/issue/KT-16139) Adding explicit type argument leads to type mismatch
- [`KT-19462`](https://youtrack.jetbrains.com/issue/KT-19462) False positive inspection "Redundant lambda arrow" for overloaded functions
- [`KT-22137`](https://youtrack.jetbrains.com/issue/KT-22137) Create class quickfix is not suggested in return statement
- [`KT-23259`](https://youtrack.jetbrains.com/issue/KT-23259) False positive unchecked cast warning/quickfix result in good code turning red
- [`KT-27641`](https://youtrack.jetbrains.com/issue/KT-27641) "Specify type explicitly" suggests too general type even when type hint shows specific generic type
- [`KT-29124`](https://youtrack.jetbrains.com/issue/KT-29124) False positive inspection 'Redundant lambda arrow' with generic function/constructor with lambda argument
- [`KT-29590`](https://youtrack.jetbrains.com/issue/KT-29590) False positive inspection "Redundant lambda arrow" with vararg lambda arguments passed via spread operator
- [`KT-29977`](https://youtrack.jetbrains.com/issue/KT-29977) False positive "Unused import directive" for typealias of an enum imported as static
- [`KT-30233`](https://youtrack.jetbrains.com/issue/KT-30233) Change order of the quick fixes when method does not accept nullable types
- [`KT-30341`](https://youtrack.jetbrains.com/issue/KT-30341) False positive 'Use withIndex() instead of manual index increment' inspection with destructive declaration in 'for' loop
- [`KT-30414`](https://youtrack.jetbrains.com/issue/KT-30414) "Replace return with 'if' expression" drops return label
- [`KT-30426`](https://youtrack.jetbrains.com/issue/KT-30426) Don't preserve extra line when adding remaining branches for when
- [`KT-30433`](https://youtrack.jetbrains.com/issue/KT-30433) "Convert member to extension" doesn't update external Kotlin calls
- [`KT-30117`](https://youtrack.jetbrains.com/issue/KT-30117) Kotlin unused import analysis accesses file editor manager model outside UI thread
- [`KT-29143`](https://youtrack.jetbrains.com/issue/KT-29143) Unnecessary primary `constructor` keyword inspection
- [`KT-29444`](https://youtrack.jetbrains.com/issue/KT-29444) "Make public" intention does not remove additional white-space to conform to proper style
- [`KT-30337`](https://youtrack.jetbrains.com/issue/KT-30337) Do not propose to move variable declaration into "when" if it's not used inside the when-expression

### IDE. Multiplatform

- [`KT-29918`](https://youtrack.jetbrains.com/issue/KT-29918) Outdated Ktor version in Kotlin (JS Client/JVM Server) multiplatform project generated via New Project Wizard

### IDE. Navigation

- [`KT-26924`](https://youtrack.jetbrains.com/issue/KT-26924) Overriding Methods list has more values than it should be in case of inline class
- [`KT-28661`](https://youtrack.jetbrains.com/issue/KT-28661) "Is implemented in" gutter icon shows duplicate function implementations in inline classes
- [`KT-28838`](https://youtrack.jetbrains.com/issue/KT-28838) Group by file structure doesn't work for text search in Kotlin

### IDE. Refactorings

- [`KT-27602`](https://youtrack.jetbrains.com/issue/KT-27602) Kotlin property renaming change target name several times  during rename making it hard to process it by reference handlers
- [`KT-29062`](https://youtrack.jetbrains.com/issue/KT-29062) Extract Superclass refactoring throws Exception if sourceRoots.size() <= 1
- [`KT-29796`](https://youtrack.jetbrains.com/issue/KT-29796) Label rename refactoring does not work on label usage

### IDE. Scratch

- [`KT-23985`](https://youtrack.jetbrains.com/issue/KT-23985) Allow to run Kotlin Worksheet without module classpath
- [`KT-27955`](https://youtrack.jetbrains.com/issue/KT-27955) Interactive mode for Kotlin Scratch files
- [`KT-28958`](https://youtrack.jetbrains.com/issue/KT-28958) Exception "Read access is allowed from event dispatch thread or inside read-action only" when running a scratch file with "Use REPL" and "Make before Run" enabled
- [`KT-30200`](https://youtrack.jetbrains.com/issue/KT-30200) "java.lang.Throwable: Couldn't find expression with start line ..." on edition of a scratch file during its execution with interactive mode enabled

### IDE. Script

- [`KT-29770`](https://youtrack.jetbrains.com/issue/KT-29770) IntelliJ IDEA makes too many requests for the classpath of a Gradle Kotlin build script
- [`KT-29893`](https://youtrack.jetbrains.com/issue/KT-29893) IDE is frozen during project configuration because of `ScriptTemplatesFromDependenciesProvider`
- [`KT-30146`](https://youtrack.jetbrains.com/issue/KT-30146) Preferences from Kotlin scripting section reset to default after project reopening

### IDE. Tests Support

- [`KT-25956`](https://youtrack.jetbrains.com/issue/KT-25956) With failed test function class gutter icon is "failure", but function icon is "success"

### IDE. Wizards

- [`KT-17829`](https://youtrack.jetbrains.com/issue/KT-17829) Please unify naming of Kotlin projects and frameworks for JVM
- [`KT-28941`](https://youtrack.jetbrains.com/issue/KT-28941) Tip of the day: obsolete project types from "New project wizard"

### Libraries

- [`KT-27108`](https://youtrack.jetbrains.com/issue/KT-27108) `.toDouble()` and `.toFloat()` conversions for unsigned types
- [`KT-29520`](https://youtrack.jetbrains.com/issue/KT-29520) Random.Default cannot be used asJavaRandom
- [`KT-30109`](https://youtrack.jetbrains.com/issue/KT-30109) Documentation for Result.onSuccess and Result.onFailure are flipped around
- [`KT-26378`](https://youtrack.jetbrains.com/issue/KT-26378) 'contains' overloads for unsigned integer ranges with other unsigned integer types
- [`KT-26410`](https://youtrack.jetbrains.com/issue/KT-26410) High-order function overloads for unsigned arrays
- [`KT-27262`](https://youtrack.jetbrains.com/issue/KT-27262) Binary search for specialized arrays of unsigned integers
- [`KT-28339`](https://youtrack.jetbrains.com/issue/KT-28339) Add `fill` extension function for unsigned primitive arrays
- [`KT-28397`](https://youtrack.jetbrains.com/issue/KT-28397) UByteArray plus UByteArray = List<UByte>
- [`KT-28779`](https://youtrack.jetbrains.com/issue/KT-28779) Implement method sum() for arrays of unsigned primitives
- [`KT-29151`](https://youtrack.jetbrains.com/issue/KT-29151) Documentation for CharSequence.take() & String.take() shows examples of Iterable<T>.take()
- [`KT-30035`](https://youtrack.jetbrains.com/issue/KT-30035) add max/maxOf/min/minOf for unsigned types
- [`KT-30051`](https://youtrack.jetbrains.com/issue/KT-30051) elementAt extension function of Array/PrimitiveAray/UnsignedArray does not throw IndexOutOfBoundException on incorrect index (JS only)
- [`KT-30141`](https://youtrack.jetbrains.com/issue/KT-30141) JS: document Array.get behavior
- [`KT-30704`](https://youtrack.jetbrains.com/issue/KT-30704) Documentation of Random function not quite correct

### Tools. CLI

- [`KT-26240`](https://youtrack.jetbrains.com/issue/KT-26240) Support JVM bytecode targets 9, 10, 11, 12

### Tools. Gradle

- [`KT-12295`](https://youtrack.jetbrains.com/issue/KT-12295) Gradle IC: Compile error leads to non-incremental build
- [`KT-12700`](https://youtrack.jetbrains.com/issue/KT-12700) Add a way to diagnose IC problems
- [`KT-26275`](https://youtrack.jetbrains.com/issue/KT-26275) Check new MPP IC
- [`KT-27885`](https://youtrack.jetbrains.com/issue/KT-27885) Drop support for Gradle 3.x and earlier
- [`KT-27886`](https://youtrack.jetbrains.com/issue/KT-27886) Drop support for Android Gradle plugin 2.x
- [`KT-28552`](https://youtrack.jetbrains.com/issue/KT-28552) Gradle 4.7 import fails on Kotlin/mpp projects with Java11
- [`KT-29275`](https://youtrack.jetbrains.com/issue/KT-29275) Drop support for Gradle 4.0
- [`KT-29758`](https://youtrack.jetbrains.com/issue/KT-29758) Gradle build failed with exception on publication of a multiplatform library with Gradle metadata enabled: org.jetbrains.kotlin.gradle.plugin.mpp.HierarchyAttributeContainer cannot be cast to org.gradle.api.internal.attributes.AttributeContainerInternal
- [`KT-29966`](https://youtrack.jetbrains.com/issue/KT-29966) Fix inter-project IC with new MPP for JS/JVM targets
- [`KT-27059`](https://youtrack.jetbrains.com/issue/KT-27059) Ensure a dependency on the multiplatform project in the POM when publishing a single-platform module with the `maven` plugin
- [`KT-29971`](https://youtrack.jetbrains.com/issue/KT-29971) ConcurrentModificationException in Kotlin Gradle plugin (GradleCompilerRunner.buildModulesInfo)
- [`KT-21030`](https://youtrack.jetbrains.com/issue/KT-21030) Automatically detect java 1.8 sources in kotlin-android gradle plugin
- [`KT-27675`](https://youtrack.jetbrains.com/issue/KT-27675) Enable Kapt build cache by default
- [`KT-27714`](https://youtrack.jetbrains.com/issue/KT-27714) Kotlin MPP Android targets don't have their attributes copied to the configurations of the compilations
- [`KT-29761`](https://youtrack.jetbrains.com/issue/KT-29761) Inter-project IC does not work for kaptGenerateStubs* tasks on Android
- [`KT-29823`](https://youtrack.jetbrains.com/issue/KT-29823) Update 'org.gradle.usage' attribute rules to support the 'JAVA_API_JARS' value
- [`KT-29964`](https://youtrack.jetbrains.com/issue/KT-29964) A universal Gradle DSL way of configuring all compilations of all targets doesn't work for Android target of a multiplatform project
- [`KT-30276`](https://youtrack.jetbrains.com/issue/KT-30276) Warn if the Kotlin Gradle plugin is loaded multiple times
- [`KT-30322`](https://youtrack.jetbrains.com/issue/KT-30322) Memory leak in CompilationSourceSetUtil
- [`KT-30492`](https://youtrack.jetbrains.com/issue/KT-30492) Classes not removed for out/in process compilation

### Tools. J2K

- [`KT-29713`](https://youtrack.jetbrains.com/issue/KT-29713) java.lang.IllegalStateException at converting  @RestController java file to Kotlin file

### Tools. JPS

- [`KT-30137`](https://youtrack.jetbrains.com/issue/KT-30137) Deadlock during concurrent classloading

### Tools. Maven

- [`KT-29251`](https://youtrack.jetbrains.com/issue/KT-29251) NSME: MavenProjectsManager.scheduleArtifactsDownloading() at KotlinMavenImporter.scheduleDownloadStdlibSources()

### Tools. REPL

- [`KT-19276`](https://youtrack.jetbrains.com/issue/KT-19276) Console spam when opening idea-community project in debug IDEA

### Tools. Scripts

- [`KT-29296`](https://youtrack.jetbrains.com/issue/KT-29296) Script evaluation - impossible to set base classloader to null
- [`KT-27051`](https://youtrack.jetbrains.com/issue/KT-27051) Support dynamic versions in @file:DependsOn
- [`KT-27815`](https://youtrack.jetbrains.com/issue/KT-27815) Compiler options in the scripting compilation configuration are ignored on compilation/evaluation
- [`KT-28593`](https://youtrack.jetbrains.com/issue/KT-28593) Idea tries to associate file type with the script definition discovery file
- [`KT-29319`](https://youtrack.jetbrains.com/issue/KT-29319) scripts default jvmTarget causes inlining problems - default should be 1.8
- [`KT-29741`](https://youtrack.jetbrains.com/issue/KT-29741) KJvmCompiledScript can not be deserialized KJvmCompiledModule if it's null
- [`KT-30210`](https://youtrack.jetbrains.com/issue/KT-30210) Coroutines in main.kts crash with NoSuchMethodError because kotlin-main-kts.jar has embedded coroutines

### Tools. kapt

- [`KT-26977`](https://youtrack.jetbrains.com/issue/KT-26977) kapt plugin applied in platform.jvm module preventing visibility of common code
- [`KT-27506`](https://youtrack.jetbrains.com/issue/KT-27506) Kapt error "no interface expected here" in class implementing interface with secondary constructor
- [`KT-28220`](https://youtrack.jetbrains.com/issue/KT-28220) kapt can generate invalid stub files for imports of enum constants
- [`KT-28306`](https://youtrack.jetbrains.com/issue/KT-28306) Cannot extend an generic interface with function body while using kapt and correctErrorTypes in Kotlin 1.3
- [`KT-23880`](https://youtrack.jetbrains.com/issue/KT-23880) Kapt: Support incremental annotation processors
- [`KT-29302`](https://youtrack.jetbrains.com/issue/KT-29302) Java classes doesn't resolve Kotlin classes when kapt.use.worker.api = true
- [`KT-30163`](https://youtrack.jetbrains.com/issue/KT-30163) Kapt: Javadoc in Java source model mangled (leading asterisks are preserved)

### Docs & Examples

- [`KT-30091`](https://youtrack.jetbrains.com/issue/KT-30091) KClass documentation incorrectly shows all members available on all platforms
- [`KT-30100`](https://youtrack.jetbrains.com/issue/KT-30100) Clarify Map.toSortedMap docs
- [`KT-30418`](https://youtrack.jetbrains.com/issue/KT-30418) Documentation for floor() and ceil() functions is misleading
- [`KT-29373`](https://youtrack.jetbrains.com/issue/KT-29373) MutableSet.add documentation is confusing

## Previous releases
This release also includes the fixes and improvements from the [previous releases](https://github.com/JetBrains/kotlin/releases/tag/v1.3.21).
