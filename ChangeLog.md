## 1.7.11

### Compiler

- [`KT-53739`](https://youtrack.jetbrains.com/issue/KT-53739) Builder inference, extension hides members

#### Fixes
- [`KT-53124`](https://youtrack.jetbrains.com/issue/KT-53124) Receiver type mismatch when combining extension properties, type projections, Java sources, and F-bounded type-variables


## 1.7.10

### Compiler

- [`KT-52702`](https://youtrack.jetbrains.com/issue/KT-52702) Invalid locals information when compiling `kotlinx.collections.immutable` with Kotlin 1.7.0-RC2
- [`KT-52892`](https://youtrack.jetbrains.com/issue/KT-52892) Disappeared specific builder inference resolution ambiguity errors
- [`KT-52782`](https://youtrack.jetbrains.com/issue/KT-52782) Appeared receiver type mismatch error due to ProperTypeInferenceConstraintsProcessing compiler feature
- [`KT-52718`](https://youtrack.jetbrains.com/issue/KT-52718) declaringClass deprecation message mentions the wrong replacement in 1.7

### IDE

#### Fixes

- [`KTIJ-19088`](https://youtrack.jetbrains.com/issue/KTIJ-19088) KotlinUFunctionCallExpression.resolve() returns null for calls to @JvmSynthetic functions
- [`KTIJ-19624`](https://youtrack.jetbrains.com/issue/KTIJ-19624) NoDescriptorForDeclarationException on iosTest.kt.vm
- [`KTIJ-21515`](https://youtrack.jetbrains.com/issue/KTIJ-21515) Load JVM target 1.6 as 1.8 in Maven projects
- [`KTIJ-21735`](https://youtrack.jetbrains.com/issue/KTIJ-21735) Exception when opening a project
- [`KTIJ-17414`](https://youtrack.jetbrains.com/issue/KTIJ-17414) UAST: Synthetic enum methods have null return values
- [`KTIJ-17444`](https://youtrack.jetbrains.com/issue/KTIJ-17444) UAST: Synthetic enum methods are missing nullness annotations
- [`KTIJ-19043`](https://youtrack.jetbrains.com/issue/KTIJ-19043) UElement#comments is empty for a Kotlin property with a getter
- [`KTIJ-10031`](https://youtrack.jetbrains.com/issue/KTIJ-10031) IDE fails to suggest a project declaration import if the name clashes with internal declaration with implicit import from stdlib (ex. @Serializable)
- [`KTIJ-21151`](https://youtrack.jetbrains.com/issue/KTIJ-21151) Exception about wrong read access from "Java overriding methods searcher" with Kotlin overrides
- [`KTIJ-20736`](https://youtrack.jetbrains.com/issue/KTIJ-20736) NoClassDefFoundError: Could not initialize class org.jetbrains.kotlin.idea.roots.KotlinNonJvmOrderEnumerationHandler. Kotlin plugin 1.7 fails to start
- [`KTIJ-21063`](https://youtrack.jetbrains.com/issue/KTIJ-21063) IDE highlighting: False positive error "Context receivers should be enabled explicitly"
- [`KTIJ-20810`](https://youtrack.jetbrains.com/issue/KTIJ-20810) NoClassDefFoundError: org/jetbrains/kotlin/idea/util/SafeAnalyzeKt errors in 1.7.0-master-212 kotlin plugin on project open
- [`KTIJ-17869`](https://youtrack.jetbrains.com/issue/KTIJ-17869) KotlinUFunctionCallExpression.resolve() returns null for instantiations of local classes with default constructors
- [`KTIJ-21061`](https://youtrack.jetbrains.com/issue/KTIJ-21061) UObjectLiteralExpression.getExpressionType() returns the base class type for Kotlin object literals instead of the anonymous class type
- [`KTIJ-20200`](https://youtrack.jetbrains.com/issue/KTIJ-20200) UAST: @Deprecated(level=HIDDEN) constructors are not returning UMethod.isConstructor=true

### IDE. Code Style, Formatting

- [`KTIJ-20554`](https://youtrack.jetbrains.com/issue/KTIJ-20554) Introduce some code style for definitely non-null types

### IDE. Completion

- [`KTIJ-14740`](https://youtrack.jetbrains.com/issue/KTIJ-14740) Multiplatform declaration actualised in an intermediate source set is shown twice in a completion popup called in the source set

### IDE. Debugger

- [`KTIJ-20815`](https://youtrack.jetbrains.com/issue/KTIJ-20815) MPP Debugger: Evaluation of expect function for the project with intermediate source set may fail with java.lang.NoSuchMethodError

### IDE. Decompiler, Indexing, Stubs

- [`KTIJ-21472`](https://youtrack.jetbrains.com/issue/KTIJ-21472) "java.lang.IllegalStateException: Could not read file" exception on indexing invalid class file
- [`KTIJ-20802`](https://youtrack.jetbrains.com/issue/KTIJ-20802) Definitely Not-Null types: "UpToDateStubIndexMismatch: PSI and index do not match" plugin error when trying to use library function with T&Any

### IDE. FIR

- [`KTIJ-20971`](https://youtrack.jetbrains.com/issue/KTIJ-20971) FIR IDE: "Parameter Info" shows parameters of uncallable methods
- [`KTIJ-21021`](https://youtrack.jetbrains.com/issue/KTIJ-21021) FIR IDE: Completion of extension function does not work on nullable receiver
- [`KTIJ-21343`](https://youtrack.jetbrains.com/issue/KTIJ-21343) FIR IDE: Navigation from explicit invoke call does not work
- [`KTIJ-21013`](https://youtrack.jetbrains.com/issue/KTIJ-21013) FIR IDE: Inconsistent smartcasts highlighting
- [`KTIJ-21374`](https://youtrack.jetbrains.com/issue/KTIJ-21374) FIR IDE: Incorrect highlighting for operators
- [`KTIJ-20443`](https://youtrack.jetbrains.com/issue/KTIJ-20443) FIR IDE: Work in Dumb mode
- [`KTIJ-20852`](https://youtrack.jetbrains.com/issue/KTIJ-20852) FIR IDE: Exception when checking `isInheritor` on two classes in different modules
- [`KTIJ-20637`](https://youtrack.jetbrains.com/issue/KTIJ-20637) FIR IDE: Strange exception while commenting-uncommenting FirReferenceResolveHelper.kt

### IDE. Gradle Integration

- [`KTIJ-21807`](https://youtrack.jetbrains.com/issue/KTIJ-21807) Gradle to IDEA import: language and API version settings are not imported for Native facet
- [`KTIJ-21692`](https://youtrack.jetbrains.com/issue/KTIJ-21692) Kotlin Import Test maintenance: 1.7.0-Beta
- [`KTIJ-20567`](https://youtrack.jetbrains.com/issue/KTIJ-20567) Kotlin/JS: Gradle import into IDEA creates no proper sub-modules, source sets, facets

### IDE. Hints. Inlay

- [`KTIJ-20552`](https://youtrack.jetbrains.com/issue/KTIJ-20552) Support definitely non-null types in inlay hints

### IDE. Inspections and Intentions

#### New Features

- [`KTIJ-18979`](https://youtrack.jetbrains.com/issue/KTIJ-18979) Quickfix for INTEGER_OPERATOR_RESOLVE_WILL_CHANGE to add explicit conversion call
- [`KTIJ-19950`](https://youtrack.jetbrains.com/issue/KTIJ-19950) Provide quickfixes for `INVALID_IF_AS_EXPRESSION_WARNING` and `NO_ELSE_IN_WHEN_WARNING`
- [`KTIJ-19866`](https://youtrack.jetbrains.com/issue/KTIJ-19866) Create quick-fix for effective visibility error on private-in-file interface exposing private class
- [`KTIJ-19939`](https://youtrack.jetbrains.com/issue/KTIJ-19939) Provide quickfix for deprecated confusing expressions in when branches

#### Fixes

- [`KTIJ-20705`](https://youtrack.jetbrains.com/issue/KTIJ-20705) Register quickfix for `NO_CONSTRUCTOR_WARNING` diagnostic
- [`KTIJ-21226`](https://youtrack.jetbrains.com/issue/KTIJ-21226) "Remove else branch" quick fix is not suggested
- [`KTIJ-20981`](https://youtrack.jetbrains.com/issue/KTIJ-20981) Definitely non-null types: quick-fixes suggested incorrectly for LV=1.6 when Xenhance-type-parameter-types-to-def-not-null flag is set
- [`KTIJ-20953`](https://youtrack.jetbrains.com/issue/KTIJ-20953) Add quickfix for OVERRIDE_DEPRECATION warning to 1.7 - 1.9 migration
- [`KTIJ-20734`](https://youtrack.jetbrains.com/issue/KTIJ-20734) Replace with [@JvmInline] value quick fix should be appliable on a whole project
- [`KTIJ-21420`](https://youtrack.jetbrains.com/issue/KTIJ-21420) Add 'else' branch quick fix suggestion is displayed twice in case 'if' isn't completed
- [`KTIJ-21192`](https://youtrack.jetbrains.com/issue/KTIJ-21192) "Make protected" intention is redundant for interface properties
- [`KTIJ-18120`](https://youtrack.jetbrains.com/issue/KTIJ-18120) "Make public" intention does not add explicit "public" modifier when using ExplicitApi Strict mode
- [`KTIJ-20493`](https://youtrack.jetbrains.com/issue/KTIJ-20493) "Create expect" quick fix doesn't warn about platform-specific annotations

### IDE. Misc

- [`KTIJ-21582`](https://youtrack.jetbrains.com/issue/KTIJ-21582) Notification for Kotlin EAP survey

### IDE. Native

- [`KTIJ-21602`](https://youtrack.jetbrains.com/issue/KTIJ-21602) With Native Debugging Support plugin Gradle run configurations can't be executed from IDEA: LLDB_NATVIS_RENDERERS_ENABLED

### IDE. Wizards

- [`KTIJ-20919`](https://youtrack.jetbrains.com/issue/KTIJ-20919) Update ktor-html-builder dependency in kotlin wizards
- [`KTIJ-20962`](https://youtrack.jetbrains.com/issue/KTIJ-20962) Wizard: Invalid Ktor imports

### Tools. Gradle

- [`KT-52777`](https://youtrack.jetbrains.com/issue/KT-52777) 'org.jetbrains.kotlinx:atomicfu:1.7.0' Gradle 7.0+ plugin variant was published with missing classes

### Tools. Gradle. JS

- [`KT-52856`](https://youtrack.jetbrains.com/issue/KT-52856) Kotlin/JS: Upgrade NPM dependencies

### Tools. Gradle. Multiplatform

- [`KT-52955`](https://youtrack.jetbrains.com/issue/KT-52955) SourceSetMetadataStorageForIde: Broken 'cleanupStaleEntries' with enabled configuration caching or isolated ClassLoaders
- [`KT-52694`](https://youtrack.jetbrains.com/issue/KT-52694) Kotlin 1.7.0 breaks Configuration Caching in Android projects

### Tools. Incremental Compile

- [`KT-52669`](https://youtrack.jetbrains.com/issue/KT-52669) Full rebuild in IC exception recovery leaves corrupt IC data

### Tools. JPS

- [`KTIJ-17280`](https://youtrack.jetbrains.com/issue/KTIJ-17280) JPS: don't use java.io.File.createTempFile as it is not working sometimes
- [`KTIJ-20954`](https://youtrack.jetbrains.com/issue/KTIJ-20954) NPE at at org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil.readNameResolver on compiling by JPS with LV > 1.7


## 1.7.0

### Analysis API. FIR

- [`KT-50864`](https://youtrack.jetbrains.com/issue/KT-50864) Analysis API: ISE: "KtCallElement should always resolve to a KtCallInfo" is thrown on call resolution inside plusAssign target
- [`KT-50252`](https://youtrack.jetbrains.com/issue/KT-50252) Analysis API: Implement FirModuleResolveStates for libraries
- [`KT-50862`](https://youtrack.jetbrains.com/issue/KT-50862) Analsysis API: do not create use site subsitution override symbols

### Analysis API. FIR Low Level API

- [`KT-50729`](https://youtrack.jetbrains.com/issue/KT-50729) Type bound is not fully resolved
- [`KT-50728`](https://youtrack.jetbrains.com/issue/KT-50728) Lazy resolve of extension function from 'kotlin' package breaks over unresolved type
- [`KT-50271`](https://youtrack.jetbrains.com/issue/KT-50271) Analysis API: get rid of using FirRefWithValidityCheck

### Backend. Native. Debug

- [`KT-50558`](https://youtrack.jetbrains.com/issue/KT-50558) K/N Debugger. Error is not displayed in variables view for catch block

### Compiler

#### New Features

- [`KT-26245`](https://youtrack.jetbrains.com/issue/KT-26245) Add ability to specify generic type parameters as not-null
- [`KT-45165`](https://youtrack.jetbrains.com/issue/KT-45165) Remove JVM target version 1.6
- [`KT-27435`](https://youtrack.jetbrains.com/issue/KT-27435) Allow implementation by delegation to inlined value of inline class
- [`KT-47939`](https://youtrack.jetbrains.com/issue/KT-47939) Support method references to functional interface constructors
- [`KT-50775`](https://youtrack.jetbrains.com/issue/KT-50775) Support IR partial linkage in Kotlin/Native (disabled by default)
- [`KT-51737`](https://youtrack.jetbrains.com/issue/KT-51737) Kotlin/Native: Remove unnecessary safepoints on watchosArm32 and iosArm32 targets
- [`KT-44249`](https://youtrack.jetbrains.com/issue/KT-44249) NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER with type usage in higher order function

#### Performance Improvements

- [`KT-48233`](https://youtrack.jetbrains.com/issue/KT-48233) Switching to JVM IR backend increases compilation time by more than 15%
- [`KT-51699`](https://youtrack.jetbrains.com/issue/KT-51699) Kotlin/Native: runtime has no LTO in debug binaries
- [`KT-34466`](https://youtrack.jetbrains.com/issue/KT-34466) Use optimized switch over enum only when all entries are constant enum entry expressions
- [`KT-50861`](https://youtrack.jetbrains.com/issue/KT-50861) FIR: Combination of array set convention and plusAssign works exponentially
- [`KT-47171`](https://youtrack.jetbrains.com/issue/KT-47171) For loop doesn't avoid boxing with value class iterators (JVM)
- [`KT-29199`](https://youtrack.jetbrains.com/issue/KT-29199) 'next' calls for iterators of merged primitive progressive values are not specialized
- [`KT-50585`](https://youtrack.jetbrains.com/issue/KT-50585) JVM IR: Array constructor loop should use IINC
- [`KT-22429`](https://youtrack.jetbrains.com/issue/KT-22429) Optimize 'for' loop code generation for reversed arrays
- [`KT-50074`](https://youtrack.jetbrains.com/issue/KT-50074) Performance regression in String-based 'when' with single equality clause
- [`KT-22334`](https://youtrack.jetbrains.com/issue/KT-22334) Compiler backend could generate smaller code for loops using range such as integer..array.size -1
- [`KT-35272`](https://youtrack.jetbrains.com/issue/KT-35272) Unnecessary null check on unsafe cast after not-null assertion operator
- [`KT-27427`](https://youtrack.jetbrains.com/issue/KT-27427) Optimize nullable check introduced with 'as' cast

#### Fixes

- [`KT-46762`](https://youtrack.jetbrains.com/issue/KT-46762) Finalize support for jspecify
- [`KT-51499`](https://youtrack.jetbrains.com/issue/KT-51499) @file:OptIn doesn't cover override methods
- [`KT-52037`](https://youtrack.jetbrains.com/issue/KT-52037) FIR: add error in 1.7.0 branch if run with non-compatible plugins
- [`KT-46756`](https://youtrack.jetbrains.com/issue/KT-46756) Release the K2/JVM compiler in Alpha
- [`KT-49715`](https://youtrack.jetbrains.com/issue/KT-49715) IR: "IllegalStateException: Function has no body: FUN name:toString" during IR lowering with shadowed extension inside interface
- [`KT-45508`](https://youtrack.jetbrains.com/issue/KT-45508) False negative ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED on a fake override with an abstract super class member
- [`KT-28078`](https://youtrack.jetbrains.com/issue/KT-28078) Report error "Public property exposes its private type" for primary constructor properties instead of warning
- [`KT-49017`](https://youtrack.jetbrains.com/issue/KT-49017) Forbid usages of super or super<Some> if in fact it accesses an abstract member
- [`KT-38078`](https://youtrack.jetbrains.com/issue/KT-38078) Prohibit calling methods from Any with "super" qualifier once they are overridden as abstract in superclass
- [`KT-52363`](https://youtrack.jetbrains.com/issue/KT-52363) Evaluate impact of qualified `this` behavior change warnings
- [`KT-52561`](https://youtrack.jetbrains.com/issue/KT-52561) JVM: Coroutine state machine loses value after a check-induced smart cast
- [`KT-52311`](https://youtrack.jetbrains.com/issue/KT-52311) java.lang.VerifyError: Bad type on operand stack
- [`KT-41124`](https://youtrack.jetbrains.com/issue/KT-41124) Inconsistency of exceptions at init block for an enum entry with and without a qualifier name
- [`KT-46860`](https://youtrack.jetbrains.com/issue/KT-46860) Make safe calls always nullable
- [`KT-52503`](https://youtrack.jetbrains.com/issue/KT-52503) New green code appeared at the callable reference resolution
- [`KT-51925`](https://youtrack.jetbrains.com/issue/KT-51925) Native: "IllegalStateException: Symbol for kotlinx.cinterop/CStructVar|null[0] is unbound" caused by inline function
- [`KT-49317`](https://youtrack.jetbrains.com/issue/KT-49317) "IllegalStateException: Parent of this declaration is not a class: FUN LOCAL_FUNCTION_FOR_LAMBDA" with parameter of suspend type with the default parameter
- [`KT-51844`](https://youtrack.jetbrains.com/issue/KT-51844) New errors in overload resolution involving vararg extension methods
- [`KT-52006`](https://youtrack.jetbrains.com/issue/KT-52006) "java.lang.Throwable: Unbalanced tree Exception" on indexing kotlin project
- [`KT-51223`](https://youtrack.jetbrains.com/issue/KT-51223) Report warning about conflicting inherited members from deserialized dependencies
- [`KT-51439`](https://youtrack.jetbrains.com/issue/KT-51439) FE 1.0: implement type variance conflict deprecation on qualifier type arguments
- [`KT-51433`](https://youtrack.jetbrains.com/issue/KT-51433) FE 1.0: implement warnings about label resolve changes
- [`KT-51317`](https://youtrack.jetbrains.com/issue/KT-51317) Regression in resolution of lambdas where expected type has an extension receiver parameter
- [`KT-45935`](https://youtrack.jetbrains.com/issue/KT-45935) JVM IR: Add not-null assertion for explicit definitely not-null parameters
- [`KT-51818`](https://youtrack.jetbrains.com/issue/KT-51818) "ClassCastException: class CoroutineSingletons cannot be cast to class" with suspendCoroutineUninterceptedOrReturn and coroutines
- [`KT-51718`](https://youtrack.jetbrains.com/issue/KT-51718) JVM / IR: "VerifyError: Bad type on operand stack" caused by nullable variable inside suspend function
- [`KT-51927`](https://youtrack.jetbrains.com/issue/KT-51927) Native: `The symbol of unexpected type encountered during IR deserialization` error when multiple libraries have non-conflicting declarations with the same name
- [`KT-52394`](https://youtrack.jetbrains.com/issue/KT-52394) JVM: Missing annotation on method with value class return type when a subclass is present in the same file in Kotlin 1.7.0-Beta
- [`KT-51640`](https://youtrack.jetbrains.com/issue/KT-51640) FIR: remove warning about "far from being production ready"
- [`KT-45553`](https://youtrack.jetbrains.com/issue/KT-45553) FIR: support hiding declaration from star import by as import
- [`KT-52404`](https://youtrack.jetbrains.com/issue/KT-52404) Prolong deprecation cycle for errors at contravariant usages of star projected argument from Java
- [`KT-50734`](https://youtrack.jetbrains.com/issue/KT-50734) TYPE_MISMATCH: NonNull parameter with a type of Nullable type argument causes compiler warning
- [`KT-51235`](https://youtrack.jetbrains.com/issue/KT-51235) JVM / IR: "AbstractMethodError: Receiver class does not define or inherit an implementation of the resolved method" when property with inline class type is overridden to return Nothing?
- [`KT-48935`](https://youtrack.jetbrains.com/issue/KT-48935) NI: Multiple generic parameter type constraints are not applied as expected when the parameter is of function type
- [`KT-49661`](https://youtrack.jetbrains.com/issue/KT-49661) NI: No TYPE_INFERENCE_UPPER_BOUND_VIOLATED when argument is inferred by return type
- [`KT-50877`](https://youtrack.jetbrains.com/issue/KT-50877) Inconsistent flexible type
- [`KT-51988`](https://youtrack.jetbrains.com/issue/KT-51988) "NPE: getContainingDeclaration…lDeclarationType.REGULAR) must not be null" when using @BuilderInference with multiple type arguments
- [`KT-48890`](https://youtrack.jetbrains.com/issue/KT-48890) Revert Opt-In restriction "Overriding methods can only have opt-in annotations that are present on their basic declarations."
- [`KT-52035`](https://youtrack.jetbrains.com/issue/KT-52035) FIR: add error in 1.7.0 branch if run on JS / Native configuration
- [`KT-45461`](https://youtrack.jetbrains.com/issue/KT-45461) NI: False negative TYPE_INFERENCE_UPPER_BOUND_VIOLATED when passing an argument to a function with generic constraints
- [`KT-52146`](https://youtrack.jetbrains.com/issue/KT-52146) JVM IR: "AssertionError: Primitive array expected" on vararg of SAM types with self-type and star projection
- [`KT-50730`](https://youtrack.jetbrains.com/issue/KT-50730) Implement error for a super class constructor call on a function interface in supertypes list
- [`KT-52040`](https://youtrack.jetbrains.com/issue/KT-52040) JVM: ClassFormatError Illegal method name "expectFailure$<init>__proxy-0"
- [`KT-50845`](https://youtrack.jetbrains.com/issue/KT-50845) Postpone rxjava errors reporting in the strict mode till 1.8 due to found broken cases
- [`KT-51979`](https://youtrack.jetbrains.com/issue/KT-51979) "AssertionError: No modifier list, but modifier has been found by the analyzer" exception on incorrect Java interface override
- [`KT-51759`](https://youtrack.jetbrains.com/issue/KT-51759) FIR DFA: false positive "Variable must be initialized"
- [`KT-50378`](https://youtrack.jetbrains.com/issue/KT-50378) Unresolved reference for method in Jsoup library in a kts script file
- [`KT-34919`](https://youtrack.jetbrains.com/issue/KT-34919) "Visibility is unknown yet" when named parameter in a function type used in a typealias implemented by an abstract class
- [`KT-51893`](https://youtrack.jetbrains.com/issue/KT-51893) Duplicated [OVERRIDE_DEPRECATION] on overridden properties
- [`KT-41034`](https://youtrack.jetbrains.com/issue/KT-41034) K2: Change evaluation semantics for combination of safe calls and convention operators
- [`KT-51843`](https://youtrack.jetbrains.com/issue/KT-51843) Functional interface constructor references are incorrectly allowed in 1.6.20 without any compiler flags
- [`KT-51914`](https://youtrack.jetbrains.com/issue/KT-51914) False positive RETURN_TYPE_MISMATCH in intellij ultimate
- [`KT-51711`](https://youtrack.jetbrains.com/issue/KT-51711) Compiler warning is displayed in case there is 'if' else branch used with elvis
- [`KT-33517`](https://youtrack.jetbrains.com/issue/KT-33517) Kotlin ScriptEngine does not respect async code when using bindings
- [`KT-51850`](https://youtrack.jetbrains.com/issue/KT-51850) FIR cannot resolve ambiguity with different SinceKotlin/DeprecatedSinceKotlin
- [`KT-44705`](https://youtrack.jetbrains.com/issue/KT-44705) Deprecate using non-exhaustive if's and when's in rhs of elvis
- [`KT-44510`](https://youtrack.jetbrains.com/issue/KT-44510) FIR DFA: smartcast after elvis with escaping lambda
- [`KT-44879`](https://youtrack.jetbrains.com/issue/KT-44879) FIR DFA: Track `inc` and `dec` operator calls in preliminary loop visitor
- [`KT-51758`](https://youtrack.jetbrains.com/issue/KT-51758) FIR: explicit API mode errors should not be reported for effectively internal / private entities
- [`KT-51203`](https://youtrack.jetbrains.com/issue/KT-51203) FIR: Inconsistent RETURN_TYPE_MISMATCH and TYPE_MISMATCH reporting on functions and properties
- [`KT-51624`](https://youtrack.jetbrains.com/issue/KT-51624) FIR: false-positive INAPPLICABLE_LATEINIT_MODIFIER for lateinit properties with unresolved types
- [`KT-51204`](https://youtrack.jetbrains.com/issue/KT-51204) FIR IC: Incremental compilation fails on nested crossinline
- [`KT-51798`](https://youtrack.jetbrains.com/issue/KT-51798) Fix ISE from IR backend when data class inherits equals/hashCode/toString with incompatible signature
- [`KT-46187`](https://youtrack.jetbrains.com/issue/KT-46187) FIR: OVERLOAD_RESOLUTION_AMBIGUITY on SAM-converted callable reference to List::plus
- [`KT-51761`](https://youtrack.jetbrains.com/issue/KT-51761) Incorrect NONE_APPLICABLE in expect class
- [`KT-51756`](https://youtrack.jetbrains.com/issue/KT-51756) FIR: false positive NO_VALUE_FOR_PARAMETER in expect class delegated constructor call
- [`KT-49778`](https://youtrack.jetbrains.com/issue/KT-49778) Support cast to DefinitelyNotNull type in Native
- [`KT-51441`](https://youtrack.jetbrains.com/issue/KT-51441) -Xpartial-linkage option specified in Gradle build script is not passed to Native linker
- [`KT-34515`](https://youtrack.jetbrains.com/issue/KT-34515) NI: "AssertionError: Base expression was not processed: POSTFIX_EXPRESSION" with double not-null assertion to brackets
- [`KT-48546`](https://youtrack.jetbrains.com/issue/KT-48546) PSI2IR: "org.jetbrains.kotlin.psi2ir.generators.ErrorExpressionException: null: KtCallExpression" with recursive property access in lazy block
- [`KT-28109`](https://youtrack.jetbrains.com/issue/KT-28109) "AssertionError: No setter call" for incrementing parenthesized result of indexed access convention operator
- [`KT-46136`](https://youtrack.jetbrains.com/issue/KT-46136) Unsubstituted return type inferred for a function returning anonymous object upcast to supertype
- [`KT-51364`](https://youtrack.jetbrains.com/issue/KT-51364) FIR: ambiguity due to String constructors clash
- [`KT-51621`](https://youtrack.jetbrains.com/issue/KT-51621) FIR: visible VS invisible qualifier conflict
- [`KT-50468`](https://youtrack.jetbrains.com/issue/KT-50468) FIR compilers fails with CCE when meets top-level destruction
- [`KT-51557`](https://youtrack.jetbrains.com/issue/KT-51557) Inline stack frame is not shown for default inline lambda
- [`KT-51358`](https://youtrack.jetbrains.com/issue/KT-51358) OptIn: show default warning/error message in case of empty message argument
- [`KT-44152`](https://youtrack.jetbrains.com/issue/KT-44152) FIR2IR fails on declarations from java stdlib if java classes are loaded from PSI instead of binaries
- [`KT-50949`](https://youtrack.jetbrains.com/issue/KT-50949) PSI2IR: NSEE from `ArgumentsGenerationUtilsKt.createFunctionForSuspendConversion` with providing lambda as argument with suspend type
- [`KT-39256`](https://youtrack.jetbrains.com/issue/KT-39256) ArrayStoreException with list of anonymous objects with inferred types created in reified extension function
- [`KT-39883`](https://youtrack.jetbrains.com/issue/KT-39883) Deprecate computing constant values of complex boolean expressions in when condition branches and conditions of loops
- [`KT-36952`](https://youtrack.jetbrains.com/issue/KT-36952) Exception during codegen: cannot pop operand off an empty stack (reference equality, implicit boxing, type check)
- [`KT-51233`](https://youtrack.jetbrains.com/issue/KT-51233) AssertionError in JavaLikeCounterLoopBuilder with Compose
- [`KT-51254`](https://youtrack.jetbrains.com/issue/KT-51254) Verify Error on passing null to type parameter extending inline class
- [`KT-50996`](https://youtrack.jetbrains.com/issue/KT-50996) [FIR] Support Int -> Long conversion for property initializers
- [`KT-51000`](https://youtrack.jetbrains.com/issue/KT-51000) [FIR] Support Int -> Long? conversion
- [`KT-51003`](https://youtrack.jetbrains.com/issue/KT-51003) [FIR] Consider Int -> Long conversion if expected type is type variable
- [`KT-51018`](https://youtrack.jetbrains.com/issue/KT-51018) [FIR] Wrong type inference if one of constraints is integer literal
- [`KT-51446`](https://youtrack.jetbrains.com/issue/KT-51446) Metadata serialization crashes with IOOBE when deserializing underlying inline class value with type table enabled
- [`KT-50973`](https://youtrack.jetbrains.com/issue/KT-50973) Redundant line number mapping for finally block with JVM IR
- [`KT-51272`](https://youtrack.jetbrains.com/issue/KT-51272) Incompatible types: KClass<Any> and callable reference Collection::class
- [`KT-51274`](https://youtrack.jetbrains.com/issue/KT-51274) "Expected some types" exception on when branch for when expression of erroneous type
- [`KT-51229`](https://youtrack.jetbrains.com/issue/KT-51229) FIR: private constructor of internal data class treated as internal and not private
- [`KT-50750`](https://youtrack.jetbrains.com/issue/KT-50750) [FIR] Report UNSUPPORTED on array literals not from annotation classes
- [`KT-51200`](https://youtrack.jetbrains.com/issue/KT-51200) False EXPOSED_PARAMETER_TYPE for internal type parameter of internal type
- [`KT-49804`](https://youtrack.jetbrains.com/issue/KT-49804) False positive of UPPER_BOUND_VIOLATED and RETURN_TYPE_MISMATCH
- [`KT-51121`](https://youtrack.jetbrains.com/issue/KT-51121) Inconsistent SAM behavior in multiple cases causing AbstractMethodError (Kotlin 1.6.10)
- [`KT-50136`](https://youtrack.jetbrains.com/issue/KT-50136) FIR: syntax error on (T & Any)
- [`KT-49465`](https://youtrack.jetbrains.com/issue/KT-49465) FIR2IR: support definitely not-null types
- [`KT-51357`](https://youtrack.jetbrains.com/issue/KT-51357) FIR: error in inference while using integer literal in expected Long position
- [`KT-49925`](https://youtrack.jetbrains.com/issue/KT-49925) [FIR] Incorrect builder inference (different cases)
- [`KT-50542`](https://youtrack.jetbrains.com/issue/KT-50542) "IllegalStateException: Type parameter descriptor is not initialized: T declared in sort" with definitely non-null type Any & T in generic constraint
- [`KT-51171`](https://youtrack.jetbrains.com/issue/KT-51171) FIR: class `Error` resolution problem
- [`KT-51156`](https://youtrack.jetbrains.com/issue/KT-51156) Multiplatform linkDebugFramework task throws NoSuchElementException when expect class constructors utilize nested enum constant
- [`KT-51017`](https://youtrack.jetbrains.com/issue/KT-51017) [FIR] Ambiguity on callable reference between two functions on generic receiver with different bounds
- [`KT-51007`](https://youtrack.jetbrains.com/issue/KT-51007) [FIR] False positive ILLEGAL_SUSPEND_FUNCTION_CALL if fun interface with suspend function declared in another module
- [`KT-50998`](https://youtrack.jetbrains.com/issue/KT-50998) [FIR] Int.inv() cal does not considered as compile time call
- [`KT-51009`](https://youtrack.jetbrains.com/issue/KT-51009) [FIR] Incorrect inference of lambda in position of return
- [`KT-50997`](https://youtrack.jetbrains.com/issue/KT-50997) [FIR] Incorrect type of typealias for suspend functional type
- [`KT-49714`](https://youtrack.jetbrains.com/issue/KT-49714) Compiler reports "'operator modifier is inapplicable" if expect class with increment operator is provided via type alias
- [`KT-48623`](https://youtrack.jetbrains.com/issue/KT-48623) Type nullability enhancement improvements
- [`KT-44623`](https://youtrack.jetbrains.com/issue/KT-44623) "IllegalStateException: IdSignature is allowed only for PublicApi symbols" when suspending receiver is annotated with something
- [`KT-46000`](https://youtrack.jetbrains.com/issue/KT-46000) JVM / IR: AssertionError on isSubtypeOfClass check in copyValueParametersToStatic with Compose
- [`KT-50211`](https://youtrack.jetbrains.com/issue/KT-50211) Annotation Instantiation with default arguments in Native
- [`KT-49412`](https://youtrack.jetbrains.com/issue/KT-49412) Controversial "type argument is not within its bounds" reported by FIR
- [`KT-48044`](https://youtrack.jetbrains.com/issue/KT-48044) [FIR] Investigate behavior of `UPPER_BOUND_VIOLATED` on complex cases
- [`KT-37975`](https://youtrack.jetbrains.com/issue/KT-37975) Don't show deprecation of enum class itself for its own member
- [`KT-50737`](https://youtrack.jetbrains.com/issue/KT-50737) Inheritance from SuspendFunction leads to compiler crash
- [`KT-50723`](https://youtrack.jetbrains.com/issue/KT-50723) Implement a fix of reporting of uninitialized parameter in default values of parameters
- [`KT-50749`](https://youtrack.jetbrains.com/issue/KT-50749) Implement UNSUPPORTED reporting on array literals inside objects in annotation classes
- [`KT-50753`](https://youtrack.jetbrains.com/issue/KT-50753) Implement reporting errors on cycles in annotation parameter types
- [`KT-50758`](https://youtrack.jetbrains.com/issue/KT-50758) Fix inconsistency of exceptions at init block for an enum entry with and without a qualifier name
- [`KT-50182`](https://youtrack.jetbrains.com/issue/KT-50182) CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT: clarify error message for `const` in object expression
- [`KT-50183`](https://youtrack.jetbrains.com/issue/KT-50183) Fix missing apostrophe escapes in compiler error messages
- [`KT-50788`](https://youtrack.jetbrains.com/issue/KT-50788) FIR: false unsafe call on not-null generic
- [`KT-50785`](https://youtrack.jetbrains.com/issue/KT-50785) FIR: inconsistent smart cast after comparison with true
- [`KT-50858`](https://youtrack.jetbrains.com/issue/KT-50858) [FIR LL] FIR in low level mode creates multiple symbols for same declaration
- [`KT-50822`](https://youtrack.jetbrains.com/issue/KT-50822) Analysis API: make declaration transformers machinery to be a thread safe
- [`KT-50972`](https://youtrack.jetbrains.com/issue/KT-50972) FIR doesn't report VAL_REASSIGNMENT on synthetic properties
- [`KT-50969`](https://youtrack.jetbrains.com/issue/KT-50969) FIR: diamond inheritance with different parameter types depends on a supertype order
- [`KT-50875`](https://youtrack.jetbrains.com/issue/KT-50875) FIR: no smart cast after reassignment with elvis
- [`KT-50835`](https://youtrack.jetbrains.com/issue/KT-50835) Inline functions with suspend lambdas break the tail-call optimization
- [`KT-49485`](https://youtrack.jetbrains.com/issue/KT-49485) JVM / IR: StackOverflowError with long when-expression conditions
- [`KT-35684`](https://youtrack.jetbrains.com/issue/KT-35684) NI: "IllegalStateException: Expected some types" from builder-inference about intersecting empty types on trivial code
- [`KT-50776`](https://youtrack.jetbrains.com/issue/KT-50776) FIR: ambiguity between Sequence.forEach and Iterable.forEach
- [`KT-48908`](https://youtrack.jetbrains.com/issue/KT-48908) Error for annotation on parameter type could have distinct ID and message referring 1.6
- [`KT-48907`](https://youtrack.jetbrains.com/issue/KT-48907) SUPERTYPE_IS_SUSPEND_FUNCTION_TYPE error could have message referring version 1.6
- [`KT-50774`](https://youtrack.jetbrains.com/issue/KT-50774) FIR2IR: NSEE in case of lambda in enum entry constructor call
- [`KT-49016`](https://youtrack.jetbrains.com/issue/KT-49016) Drop QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE diagnostic
- [`KT-34338`](https://youtrack.jetbrains.com/issue/KT-34338) Parameterless main causes duplicate JVM signature error
- [`KT-50577`](https://youtrack.jetbrains.com/issue/KT-50577) JVM_IR: No NPE when casting uninitialized value of non-null type to non-null type
- [`KT-50476`](https://youtrack.jetbrains.com/issue/KT-50476) JVM_IR: NSME when calling 'super.removeAt(Int)' implemented in Java interface as a default method
- [`KT-50257`](https://youtrack.jetbrains.com/issue/KT-50257) JVM_IR: Incorrect bridge delegate signature for renamed remove(I) causes SOE with Kotlin class inherited from fastutils IntArrayList
- [`KT-50470`](https://youtrack.jetbrains.com/issue/KT-50470) FIR: inapplicable candidate in delegate inference due to nullability
- [`KT-32744`](https://youtrack.jetbrains.com/issue/KT-32744) Inefficient compilation of null-safe call (extra null checks, unreachable code)

### Docs & Examples

- [`KT-52032`](https://youtrack.jetbrains.com/issue/KT-52032) Document performance optimizations of the Kotlin/JVM compiler in 1.7.0
- [`KT-49424`](https://youtrack.jetbrains.com/issue/KT-49424) Update KEEP for OptIn

### IDE

#### Fixes

- [`KTIJ-21735`](https://youtrack.jetbrains.com/issue/KTIJ-21735) Exception when opening a project
- [`KTIJ-17414`](https://youtrack.jetbrains.com/issue/KTIJ-17414) UAST: Synthetic enum methods have null return values
- [`KTIJ-17444`](https://youtrack.jetbrains.com/issue/KTIJ-17444) UAST: Synthetic enum methods are missing nullness annotations
- [`KTIJ-19043`](https://youtrack.jetbrains.com/issue/KTIJ-19043) UElement#comments is empty for a Kotlin property with a getter
- [`KTIJ-10031`](https://youtrack.jetbrains.com/issue/KTIJ-10031) IDE fails to suggest a project declaration import if the name clashes with internal declaration with implicit import from stdlib (ex. @Serializable)
- [`KTIJ-21515`](https://youtrack.jetbrains.com/issue/KTIJ-21515) Load JVM target 1.6 as 1.8 in Maven projects
- [`KTIJ-21151`](https://youtrack.jetbrains.com/issue/KTIJ-21151) Exception about wrong read access from "Java overriding methods searcher" with Kotlin overrides
- [`KTIJ-20736`](https://youtrack.jetbrains.com/issue/KTIJ-20736) NoClassDefFoundError: Could not initialize class org.jetbrains.kotlin.idea.roots.KotlinNonJvmOrderEnumerationHandler. Kotlin plugin 1.7 fails to start
- [`KT-50111`](https://youtrack.jetbrains.com/issue/KT-50111) Resolving into KtUltraLightMethod
- [`KTIJ-21063`](https://youtrack.jetbrains.com/issue/KTIJ-21063) IDE highlighting: False positive error "Context receivers should be enabled explicitly"
- [`KTIJ-20810`](https://youtrack.jetbrains.com/issue/KTIJ-20810) NoClassDefFoundError: org/jetbrains/kotlin/idea/util/SafeAnalyzeKt errors in 1.7.0-master-212 kotlin plugin on project open
- [`KTIJ-19088`](https://youtrack.jetbrains.com/issue/KTIJ-19088) KotlinUFunctionCallExpression.resolve() returns null for calls to @JvmSynthetic functions
- [`KTIJ-17869`](https://youtrack.jetbrains.com/issue/KTIJ-17869) KotlinUFunctionCallExpression.resolve() returns null for instantiations of local classes with default constructors
- [`KTIJ-21061`](https://youtrack.jetbrains.com/issue/KTIJ-21061) UObjectLiteralExpression.getExpressionType() returns the base class type for Kotlin object literals instead of the anonymous class type
- [`KTIJ-20200`](https://youtrack.jetbrains.com/issue/KTIJ-20200) UAST: @Deprecated(level=HIDDEN) constructors are not returning UMethod.isConstructor=true
- [`KTIJ-19624`](https://youtrack.jetbrains.com/issue/KTIJ-19624) NoDescriptorForDeclarationException on iosTest.kt.vm

### IDE. Code Style, Formatting

- [`KTIJ-20554`](https://youtrack.jetbrains.com/issue/KTIJ-20554) Introduce some code style for definitely non-null types

### IDE. Completion

- [`KTIJ-14740`](https://youtrack.jetbrains.com/issue/KTIJ-14740) Multiplatform declaration actualised in an intermediate source set is shown twice in a completion popup called in the source set

### IDE. Debugger

- [`KTIJ-20815`](https://youtrack.jetbrains.com/issue/KTIJ-20815) MPP Debugger: Evaluation of expect function for the project with intermediate source set may fail with java.lang.NoSuchMethodError

### IDE. Decompiler, Indexing, Stubs

- [`KTIJ-21472`](https://youtrack.jetbrains.com/issue/KTIJ-21472) "java.lang.IllegalStateException: Could not read file" exception on indexing invalid class file
- [`KTIJ-20802`](https://youtrack.jetbrains.com/issue/KTIJ-20802) Definitely Not-Null types: "UpToDateStubIndexMismatch: PSI and index do not match" plugin error when trying to use library function with T&Any
- [`KT-51248`](https://youtrack.jetbrains.com/issue/KT-51248) Function and parameter names with special symbols have to backticked

### IDE. FIR

- [`KTIJ-20443`](https://youtrack.jetbrains.com/issue/KTIJ-20443) FIR IDE: Work in Dumb mode
- [`KTIJ-21374`](https://youtrack.jetbrains.com/issue/KTIJ-21374) FIR IDE: Incorrect highlighting for operators
- [`KTIJ-21013`](https://youtrack.jetbrains.com/issue/KTIJ-21013) FIR IDE: Inconsistent smartcasts highlighting
- [`KTIJ-21343`](https://youtrack.jetbrains.com/issue/KTIJ-21343) FIR IDE: Navigation from explicit invoke call does not work
- [`KTIJ-20852`](https://youtrack.jetbrains.com/issue/KTIJ-20852) FIR IDE: Exception when checking `isInheritor` on two classes in different modules
- [`KTIJ-21021`](https://youtrack.jetbrains.com/issue/KTIJ-21021) FIR IDE: Completion of extension function does not work on nullable receiver
- [`KTIJ-20637`](https://youtrack.jetbrains.com/issue/KTIJ-20637) FIR IDE: Strange exception while commenting-uncommenting FirReferenceResolveHelper.kt
- [`KTIJ-20971`](https://youtrack.jetbrains.com/issue/KTIJ-20971) FIR IDE: "Parameter Info" shows parameters of uncallable methods

### IDE. Gradle Integration

- [`KTIJ-21807`](https://youtrack.jetbrains.com/issue/KTIJ-21807) Gradle to IDEA import: language and API version settings are not imported for Native facet
- [`KTIJ-21692`](https://youtrack.jetbrains.com/issue/KTIJ-21692) Kotlin Import Test maintenance: 1.7.0-Beta
- [`KTIJ-20567`](https://youtrack.jetbrains.com/issue/KTIJ-20567) Kotlin/JS: Gradle import into IDEA creates no proper sub-modules, source sets, facets

### IDE. Hints. Inlay

- [`KTIJ-20552`](https://youtrack.jetbrains.com/issue/KTIJ-20552) Support definitely non-null types in inlay hints

### IDE. Inspections and Intentions

#### New Features

- [`KTIJ-18979`](https://youtrack.jetbrains.com/issue/KTIJ-18979) Quickfix for INTEGER_OPERATOR_RESOLVE_WILL_CHANGE to add explicit conversion call
- [`KTIJ-19950`](https://youtrack.jetbrains.com/issue/KTIJ-19950) Provide quickfixes for `INVALID_IF_AS_EXPRESSION_WARNING` and `NO_ELSE_IN_WHEN_WARNING`
- [`KTIJ-19866`](https://youtrack.jetbrains.com/issue/KTIJ-19866) Create quick-fix for effective visibility error on private-in-file interface exposing private class
- [`KTIJ-19939`](https://youtrack.jetbrains.com/issue/KTIJ-19939) Provide quickfix for deprecated confusing expressions in when branches

#### Fixes

- [`KTIJ-20705`](https://youtrack.jetbrains.com/issue/KTIJ-20705) Register quickfix for `NO_CONSTRUCTOR_WARNING` diagnostic
- [`KTIJ-21226`](https://youtrack.jetbrains.com/issue/KTIJ-21226) "Remove else branch" quick fix is not suggested
- [`KTIJ-20981`](https://youtrack.jetbrains.com/issue/KTIJ-20981) Definitely non-null types: quick-fixes suggested incorrectly for LV=1.6 when Xenhance-type-parameter-types-to-def-not-null flag is set
- [`KTIJ-20953`](https://youtrack.jetbrains.com/issue/KTIJ-20953) Add quickfix for OVERRIDE_DEPRECATION warning to 1.7 - 1.9 migration
- [`KTIJ-20734`](https://youtrack.jetbrains.com/issue/KTIJ-20734) Replace with [@JvmInline] value quick fix should be appliable on a whole project
- [`KTIJ-21420`](https://youtrack.jetbrains.com/issue/KTIJ-21420) Add 'else' branch quick fix suggestion is displayed twice in case 'if' isn't completed
- [`KTIJ-21192`](https://youtrack.jetbrains.com/issue/KTIJ-21192) "Make protected" intention is redundant for interface properties
- [`KTIJ-18120`](https://youtrack.jetbrains.com/issue/KTIJ-18120) "Make public" intention does not add explicit "public" modifier when using ExplicitApi Strict mode
- [`KTIJ-20493`](https://youtrack.jetbrains.com/issue/KTIJ-20493) "Create expect" quick fix doesn't warn about platform-specific annotations

### IDE. Misc

- [`KTIJ-21582`](https://youtrack.jetbrains.com/issue/KTIJ-21582) Notification for Kotlin EAP survey

### IDE. Multiplatform

- [`KT-49523`](https://youtrack.jetbrains.com/issue/KT-49523) Improve environment setup experience for KMM projects
- [`KT-50952`](https://youtrack.jetbrains.com/issue/KT-50952) MPP: Commonized cinterops doesn't attach/detach to source set on configuration changes

### IDE. Native

- [`KT-44329`](https://youtrack.jetbrains.com/issue/KT-44329) Improve UX of using Native libraries in Kotlin
- [`KTIJ-21602`](https://youtrack.jetbrains.com/issue/KTIJ-21602) With Native Debugging Support plugin Gradle run configurations can't be executed from IDEA: LLDB_NATVIS_RENDERERS_ENABLED

### IDE. Wizards

- [`KTIJ-20919`](https://youtrack.jetbrains.com/issue/KTIJ-20919) Update ktor-html-builder dependency in kotlin wizards
- [`KTIJ-20962`](https://youtrack.jetbrains.com/issue/KTIJ-20962) Wizard: Invalid Ktor imports

### JavaScript

#### New Features

- [`KT-51735`](https://youtrack.jetbrains.com/issue/KT-51735) KJS / IR: Minimize member names in production mode

#### Performance Improvements

- [`KT-51127`](https://youtrack.jetbrains.com/issue/KT-51127) Kotlin/JS - IR generates plenty of useless `Unit_getInstance()`
- [`KT-50212`](https://youtrack.jetbrains.com/issue/KT-50212) KJS IR: Upcast should be a no-op
- [`KT-16974`](https://youtrack.jetbrains.com/issue/KT-16974) JS: Kotlin.charArrayOf is suboptimal due to Rhino bugs

#### Fixes

- [`KT-44319`](https://youtrack.jetbrains.com/issue/KT-44319) JS IR BE: Add an ability to generate separate JS files for each module
- [`KT-52518`](https://youtrack.jetbrains.com/issue/KT-52518) Kotlin/JS IR: project with 1.6.21 fails to consume library built with 1.7.0-RC: ISE "Unexpected IrType kind: KIND_NOT_SET" at IrDeclarationDeserializer.deserializeIrTypeData()
- [`KT-52010`](https://youtrack.jetbrains.com/issue/KT-52010) K/JS IR: both flows execute when using elvis operator
- [`KT-41096`](https://youtrack.jetbrains.com/issue/KT-41096) KJS IR: @JsExport should use original js name for external declarations
- [`KT-52144`](https://youtrack.jetbrains.com/issue/KT-52144) KJS / IR: Missing property definitions for interfaced defined properties
- [`KT-52252`](https://youtrack.jetbrains.com/issue/KT-52252) KJS / IR: overridden properties are undefined/null
- [`KT-51973`](https://youtrack.jetbrains.com/issue/KT-51973) KJS / IR overridden properties of inherited interface missing
- [`KT-51125`](https://youtrack.jetbrains.com/issue/KT-51125) Provide a way to use `import` keyword in `js` expressions
- [`KT-40888`](https://youtrack.jetbrains.com/issue/KT-40888) KJS / IR: Missing methods are no longer generated (polyfills)
- [`KT-50504`](https://youtrack.jetbrains.com/issue/KT-50504) KJS / IR: Transpiled JS incorrectly uses the unscrambled names of internal fields
- [`KT-51853`](https://youtrack.jetbrains.com/issue/KT-51853) JS compilation fails with "Uninitialized fast cache info" error
- [`KT-51205`](https://youtrack.jetbrains.com/issue/KT-51205) K/JS IR: external class is mapped to any
- [`KT-50806`](https://youtrack.jetbrains.com/issue/KT-50806) Typescript definitions contain invalid nested block comments with generic parent and type argument without `@JsExport`
- [`KT-51841`](https://youtrack.jetbrains.com/issue/KT-51841) KJS / IR: No flat hash for FUN FAKE_OVERRIDE with kotlin.incremental.js.ir=true
- [`KT-51081`](https://youtrack.jetbrains.com/issue/KT-51081) KJS / IR + IC: Passing an inline function with default params as a param to a higher-order function crashes the compiler
- [`KT-51084`](https://youtrack.jetbrains.com/issue/KT-51084) KJS / IR + IC: Cache invalidation doesn't check generic inline functions reified qualifier
- [`KT-51211`](https://youtrack.jetbrains.com/issue/KT-51211) K/JS IR: JsExport:  Can't export nested enum
- [`KT-51438`](https://youtrack.jetbrains.com/issue/KT-51438) KJS / IR: Duplicated import names for the same external names
- [`KT-51238`](https://youtrack.jetbrains.com/issue/KT-51238) Kotlin/JS: IR + IC: build fails after clean on `compileTestDevelopmentExecutableKotlinJs` task: "Failed to create MD5 hash for file '.../build/classes/kotlin/main' as it does not exist"
- [`KT-50674`](https://youtrack.jetbrains.com/issue/KT-50674) KJS / IR: JS code cannot modify local variable
- [`KT-50953`](https://youtrack.jetbrains.com/issue/KT-50953) KJS IR: Incorrect nested commenting in d.ts
- [`KT-15223`](https://youtrack.jetbrains.com/issue/KT-15223) JS: function that overrides external function with `vararg` parameter is translated incorrectly
- [`KT-50657`](https://youtrack.jetbrains.com/issue/KT-50657) KJS / IR 1.6.20-M1-39 - Date in Kotlin JS cannot be created from long.

### Language Design

#### New Features

- [`KT-45618`](https://youtrack.jetbrains.com/issue/KT-45618) Stabilize builder inference
- [`KT-30485`](https://youtrack.jetbrains.com/issue/KT-30485) Underscore operator for type arguments
- [`KT-49006`](https://youtrack.jetbrains.com/issue/KT-49006) Support at least three previous versions of language/API
- [`KT-16768`](https://youtrack.jetbrains.com/issue/KT-16768) Context-sensitive resolution prototype (Resolve unqualified enum constants based on expected type)
- [`KT-14663`](https://youtrack.jetbrains.com/issue/KT-14663) Support having a "public" and a "private" type for the same property
- [`KT-50477`](https://youtrack.jetbrains.com/issue/KT-50477) Functional conversion does not work on suspending functions
- [`KT-32162`](https://youtrack.jetbrains.com/issue/KT-32162) Allow generics for inline classes

#### Fixes

- [`KT-12380`](https://youtrack.jetbrains.com/issue/KT-12380) Support sealed (exhaustive) whens
- [`KT-27750`](https://youtrack.jetbrains.com/issue/KT-27750) Reverse reservation of 'yield' as keyword
- [`KT-22956`](https://youtrack.jetbrains.com/issue/KT-22956) Release OptIn annotations
- [`KT-44866`](https://youtrack.jetbrains.com/issue/KT-44866) Change behavior of private constructors of sealed classes
- [`KT-49110`](https://youtrack.jetbrains.com/issue/KT-49110) Prohibit access to members of companion of enum class from initializers of entries of this enum
- [`KT-29405`](https://youtrack.jetbrains.com/issue/KT-29405) Switch default JVM target version to 1.8

### Libraries

#### New Features

- [`KT-50484`](https://youtrack.jetbrains.com/issue/KT-50484) Extensions for java.util.Optional in stdlib
- [`KT-50146`](https://youtrack.jetbrains.com/issue/KT-50146) Reintroduce min/max(By/With) operations on collections with non-nullable return type
- [`KT-46132`](https://youtrack.jetbrains.com/issue/KT-46132) Specialized default time source with non-allocating time marks
- [`KT-41890`](https://youtrack.jetbrains.com/issue/KT-41890) Support named capture groups in Regex on Native
- [`KT-48179`](https://youtrack.jetbrains.com/issue/KT-48179) Introduce API to retrieve the number of CPUs the runtime has

#### Performance Improvements

- [`KT-42178`](https://youtrack.jetbrains.com/issue/KT-42178) Range and Progression should override last()

#### Fixes

- [`KT-42436`](https://youtrack.jetbrains.com/issue/KT-42436) Support `java.nio.Path` extension in the standard library
- [`KT-51470`](https://youtrack.jetbrains.com/issue/KT-51470) Stabilize experimental API for 1.7
- [`KT-51775`](https://youtrack.jetbrains.com/issue/KT-51775) JS: Support named capture groups in Regex
- [`KT-51776`](https://youtrack.jetbrains.com/issue/KT-51776) Native: Support back references to groups with multi-digit index
- [`KT-51082`](https://youtrack.jetbrains.com/issue/KT-51082) Introduce Enum.declaringJavaClass property
- [`KT-51848`](https://youtrack.jetbrains.com/issue/KT-51848) Promote deepRecursiveFunction to stable API
- [`KT-48924`](https://youtrack.jetbrains.com/issue/KT-48924) KJS: `toString` in base 36 produces different results in JS compare to JVM
- [`KT-50742`](https://youtrack.jetbrains.com/issue/KT-50742) Regular expression is fine on jvm but throws PatternSyntaxException for native macosX64 target
- [`KT-50059`](https://youtrack.jetbrains.com/issue/KT-50059) Stop publishing kotlin-stdlib and kotlin-test artifacts under modular classifier
- [`KT-26678`](https://youtrack.jetbrains.com/issue/KT-26678) Rename buildSequence/buildIterator to sequence/iterator

### Native

- [`KT-49406`](https://youtrack.jetbrains.com/issue/KT-49406) Kotlin/Native: generate standalone executable for androidNative targets by default
- [`KT-48595`](https://youtrack.jetbrains.com/issue/KT-48595) Enable Native embeddable compiler jar in Gradle plugin by default
- [`KT-51377`](https://youtrack.jetbrains.com/issue/KT-51377) Native: synthetic forward declarations are preferred over commonized definitions
- [`KT-49145`](https://youtrack.jetbrains.com/issue/KT-49145) Kotlin/Native static library compilation fails for androidNative*
- [`KT-49496`](https://youtrack.jetbrains.com/issue/KT-49496) Gradle (or the KMM plugin) is caching the Xcode Command Line Tools location
- [`KT-49247`](https://youtrack.jetbrains.com/issue/KT-49247) gradle --offline should translate into airplaneMode for kotin-native compiler

### Native. Build Infrastructure

- [`KT-52259`](https://youtrack.jetbrains.com/issue/KT-52259) kotlin-native releases from GitHub don't contain platform libs

### Native. C and ObjC Import

- [`KT-49455`](https://youtrack.jetbrains.com/issue/KT-49455) Methods from Swift extensions are not resolved in Kotlin shared module
- [`KT-50648`](https://youtrack.jetbrains.com/issue/KT-50648) Incorrect KMM cinterop conversion

### Native. ObjC Export

- [`KT-50982`](https://youtrack.jetbrains.com/issue/KT-50982) RuntimeAssertFailedPanic in iOS when Kotlin framework is initialized before loading
- [`KT-49937`](https://youtrack.jetbrains.com/issue/KT-49937) Kotlin/Native 1.5.31: 'runtime assert: Unexpected selector clash' when 'override fun toString(): String' is used

### Native. Platforms

- [`KT-52232`](https://youtrack.jetbrains.com/issue/KT-52232) Kotlin/Native: simplify toolchain dependency override for MinGW

### Native. Runtime

- [`KT-52365`](https://youtrack.jetbrains.com/issue/KT-52365) Kotlin/Native fails to compile projects for 32-bit targets when new memory manager is enabled

### Native. Runtime. Memory

- [`KT-48537`](https://youtrack.jetbrains.com/issue/KT-48537) Kotlin/Native: improve GC triggers in the new MM.
- [`KT-50713`](https://youtrack.jetbrains.com/issue/KT-50713) Kotlin/Native: Enable Concurrent Sweep GC by default

### Native. Stdlib

- [`KT-50312`](https://youtrack.jetbrains.com/issue/KT-50312) enhancement: kotlin native   -- add  alloc<TVarOf<T>>(T)

### Native. Testing

- [`KT-50316`](https://youtrack.jetbrains.com/issue/KT-50316) Kotlin/Native: Produce a list of available tests alongside the final artifact
- [`KT-50139`](https://youtrack.jetbrains.com/issue/KT-50139) Create tests for Enter/Leave frame optimization

### Reflection

- [`KT-27598`](https://youtrack.jetbrains.com/issue/KT-27598) "KotlinReflectionInternalError" when using `callBy` on constructor that has inline class parameters
- [`KT-31141`](https://youtrack.jetbrains.com/issue/KT-31141) IllegalArgumentException when reflectively accessing nullable property of inline class type

### Tools. CLI

- [`KT-52409`](https://youtrack.jetbrains.com/issue/KT-52409) Report error when use-k2 with Multiplatform
- [`KT-51717`](https://youtrack.jetbrains.com/issue/KT-51717) IllegalArgumentException: Unexpected versionNeededToExtract (0) in 1.6.20-RC2 with useFir enabled
- [`KT-52217`](https://youtrack.jetbrains.com/issue/KT-52217) Rename 'useFir' to 'useK2'
- [`KT-29974`](https://youtrack.jetbrains.com/issue/KT-29974) Add a compiler option '-Xjdk-release' similar to javac's '--release' to control the target JDK version
- [`KT-51673`](https://youtrack.jetbrains.com/issue/KT-51673) Make language version description not in capital letters
- [`KT-48833`](https://youtrack.jetbrains.com/issue/KT-48833) -Xsuppress-version-warnings allows to suppress errors about unsupported versions
- [`KT-51627`](https://youtrack.jetbrains.com/issue/KT-51627) kotlinc fails with `java.lang.RuntimeException` if `/tmp/build.txt` file exists on the disk
- [`KT-51306`](https://youtrack.jetbrains.com/issue/KT-51306) Support reading language settings from an environment variable and overriding the current settings by them
- [`KT-51093`](https://youtrack.jetbrains.com/issue/KT-51093) "-Xopt-in=..." command line argument no longer works

### Tools. Commonizer

- [`KT-43309`](https://youtrack.jetbrains.com/issue/KT-43309) Overwrite return type and parameter types of callable member to succeed commonization
- [`KT-52050`](https://youtrack.jetbrains.com/issue/KT-52050) [Commonizer] 'platform.posix.DIR' not implementing 'CPointed' when commonized for 'nativeMain' on linux or windows hosts
- [`KT-51224`](https://youtrack.jetbrains.com/issue/KT-51224) MPP: For optimistically commonized numbers missed kotlinx.cinterop.UnsafeNumber
- [`KT-51215`](https://youtrack.jetbrains.com/issue/KT-51215) MPP: Update Kdoc description for kotlinx.cinterop.UnsafeNumber
- [`KT-51686`](https://youtrack.jetbrains.com/issue/KT-51686) Cinterop: Overload resolution ambiguity in 1.6.20-RC2
- [`KT-46636`](https://youtrack.jetbrains.com/issue/KT-46636) HMPP: missed classes from `platform.posix.*`
- [`KT-51332`](https://youtrack.jetbrains.com/issue/KT-51332) Optimistic number commonization is disabled by default in KGP with enabled HMPP

### Tools. Compiler Plugins

- [`KT-50992`](https://youtrack.jetbrains.com/issue/KT-50992) jvm-abi-gen breaks inline functions in inline classes with private constructors in Kotlin 1.6.20

### Tools. Daemon

- [`KT-32885`](https://youtrack.jetbrains.com/issue/KT-32885) KT. Kotlin daemon compilation process is broken: java.lang.IllegalStateException Service is dying at entities generation by Kotlin.kts script

### Tools. Gradle

#### New Features

- [`KT-49227`](https://youtrack.jetbrains.com/issue/KT-49227) Support Gradle plugins variants
- [`KT-50869`](https://youtrack.jetbrains.com/issue/KT-50869) Provide API that allow AGP to set up Kotlin compilation
- [`KT-48008`](https://youtrack.jetbrains.com/issue/KT-48008) Consider offering a KotlinBasePlugin
- [`KT-52030`](https://youtrack.jetbrains.com/issue/KT-52030) Provide experimental possibility to view internal information about Kotlin Compiler performance

#### Performance Improvements

- [`KT-52141`](https://youtrack.jetbrains.com/issue/KT-52141) Optimize Java class snapshotting for the `kotlin.incremental.useClasspathSnapshot` feature
- [`KT-51978`](https://youtrack.jetbrains.com/issue/KT-51978) Optimize classpath snapshot cache for the `kotlin.incremental.useClasspathSnapshot` feature
- [`KT-51326`](https://youtrack.jetbrains.com/issue/KT-51326) Kotlin-gradle-plugin performance issue with mass java SourceRoots

#### Fixes

- [`KT-52448`](https://youtrack.jetbrains.com/issue/KT-52448) Compilation tasks are missing input/output/internal annotations on includes/excludes properties
- [`KT-52239`](https://youtrack.jetbrains.com/issue/KT-52239) Type based task configuration-blocks for JVM stopped working in Gradle
- [`KT-52313`](https://youtrack.jetbrains.com/issue/KT-52313) No recompilation in Gradle after adding or removing function parameters, removing functions (and maybe more) in dependent modules
- [`KT-51854`](https://youtrack.jetbrains.com/issue/KT-51854) Add Ktor to gradle performance benchmark
- [`KT-52086`](https://youtrack.jetbrains.com/issue/KT-52086) Rename flag 'use-fir' to 'use-k2'
- [`KT-52509`](https://youtrack.jetbrains.com/issue/KT-52509) Main variant published to Gradle plugin portal uses unshadowed artifact
- [`KT-52392`](https://youtrack.jetbrains.com/issue/KT-52392) Gradle: 1.7.0 does not support custom gradle build configuration on Windows OS
- [`KT-32805`](https://youtrack.jetbrains.com/issue/KT-32805) KotlinCompile inherits properties sourceCompatibility and targetCompatibility which breaks Gradle's incremental compilation
- [`KT-52189`](https://youtrack.jetbrains.com/issue/KT-52189) Provide Gradle Kotlin/DSL friendly deprecated classpath property in KotlinCompiler task
- [`KT-51415`](https://youtrack.jetbrains.com/issue/KT-51415) Confusing build failure reason is displayed in case kapt is used and different JDKs are used for compileKotlin and compileJava tasks
- [`KT-52187`](https://youtrack.jetbrains.com/issue/KT-52187) New IC can not be enabled in an Android project using kapt
- [`KT-51898`](https://youtrack.jetbrains.com/issue/KT-51898) Upgrading Kotlin/Kotlin Gradle plugin to 1.5.3 and above breaks 'com.android.asset-pack' plugin
- [`KT-51913`](https://youtrack.jetbrains.com/issue/KT-51913) Gradle plugin should not add attributes to the legacy configurations
- [`KT-34862`](https://youtrack.jetbrains.com/issue/KT-34862) Restoring from build cache breaks Kotlin incremental compilation
- [`KT-45777`](https://youtrack.jetbrains.com/issue/KT-45777) New IC in Gradle
- [`KT-51360`](https://youtrack.jetbrains.com/issue/KT-51360) Show performance difference in percent between releases
- [`KT-51380`](https://youtrack.jetbrains.com/issue/KT-51380) Add open-source project using Kotlin/JS plugin to build regression benchmarks
- [`KT-51937`](https://youtrack.jetbrains.com/issue/KT-51937) Toolchain usage with configuration cache prevents KotlinCompile task to be UP-TO-DATE
- [`KT-48276`](https://youtrack.jetbrains.com/issue/KT-48276) Remove kotlin2js and kotlin-dce-plugin
- [`KT-52138`](https://youtrack.jetbrains.com/issue/KT-52138) KSP could not access internal methods/properties in Kotlin Gradle Plugin
- [`KT-51342`](https://youtrack.jetbrains.com/issue/KT-51342) Set minimal supported Android Gradle plugin version to 3.6.4
- [`KT-50494`](https://youtrack.jetbrains.com/issue/KT-50494) Remove kotlin.experimental.coroutines Gradle DSL option
- [`KT-49733`](https://youtrack.jetbrains.com/issue/KT-49733) Bump minimal supported Gradle version to 6.7.1
- [`KT-48831`](https://youtrack.jetbrains.com/issue/KT-48831) Remove 'KotlinGradleSubplugin'
- [`KT-47924`](https://youtrack.jetbrains.com/issue/KT-47924) Remove annoying cast in toolchain extension method for Kotlin DSL
- [`KT-46541`](https://youtrack.jetbrains.com/issue/KT-46541) Fail Gradle builds when deprecated kotlinOptions.jdkHome is set
- [`KT-51830`](https://youtrack.jetbrains.com/issue/KT-51830) Gradle: deprecate `kotlin.compiler.execution.strategy` system property
- [`KT-47763`](https://youtrack.jetbrains.com/issue/KT-47763) Gradle DSL: Remove deprecated useExperimentalAnnotation and experimentalAnnotationInUse
- [`KT-51374`](https://youtrack.jetbrains.com/issue/KT-51374) NoSuchFileException in getOrCreateSessionFlagFile()
- [`KT-51837`](https://youtrack.jetbrains.com/issue/KT-51837) kotlin-gradle-plugin:1.6.20 fails xray scan on shadowed Gson 2.8.6.
- [`KT-51454`](https://youtrack.jetbrains.com/issue/KT-51454) KotlinJvmTest is not a cacheable task
- [`KT-45745`](https://youtrack.jetbrains.com/issue/KT-45745) Migrate only Kotlin Gradle Plugin tests to new JUnit5 DSL and run them separately on CI
- [`KT-47318`](https://youtrack.jetbrains.com/issue/KT-47318) Remove deprecated 'kotlinPluginVersion' property in `KotlinBasePluginWrapper'
- [`KT-51378`](https://youtrack.jetbrains.com/issue/KT-51378) Gradle 'buildSrc' compilation fails when newer version of Kotlin plugin is added to the build script classpath
- [`KT-46038`](https://youtrack.jetbrains.com/issue/KT-46038) Gradle: `kotlin_module` files are corrupted in the KotlinCompile output, and gets cached
- [`KT-51064`](https://youtrack.jetbrains.com/issue/KT-51064) Kotlin gradle build hangs on MetricsContainer.flush
- [`KT-48779`](https://youtrack.jetbrains.com/issue/KT-48779) Gradle: Could not connect to kotlin daemon

### Tools. Gradle. Cocoapods

- [`KT-50622`](https://youtrack.jetbrains.com/issue/KT-50622) Cocoapods Plugin: cocoapods-generate does not work correctly with ruby 3.0.0 and higher
- [`KT-51861`](https://youtrack.jetbrains.com/issue/KT-51861) Custom binary name in CocoaPods plugin isn't respected by fatFramework task

### Tools. Gradle. JS

- [`KT-52221`](https://youtrack.jetbrains.com/issue/KT-52221) Kotlin/JS: failed Node tests are not reported in a standard way
- [`KT-51895`](https://youtrack.jetbrains.com/issue/KT-51895) K/JS: Redundant technical messages during JS tests
- [`KT-51414`](https://youtrack.jetbrains.com/issue/KT-51414) Allow set up environment variables for JS tests
- [`KT-51623`](https://youtrack.jetbrains.com/issue/KT-51623) Kotlin/JS: Mocha could not failed when external module not found
- [`KT-51503`](https://youtrack.jetbrains.com/issue/KT-51503) Update NPM dependency versions

### Tools. Gradle. Multiplatform

#### New Features

- [`KT-51386`](https://youtrack.jetbrains.com/issue/KT-51386) [KPM] IdeaKotlinProjectModelBuilder: Implement dependencies

#### Fixes

- [`KT-49524`](https://youtrack.jetbrains.com/issue/KT-49524) Improve DSL for managing Kotlin/Native binary output
- [`KT-51765`](https://youtrack.jetbrains.com/issue/KT-51765) com.android.lint in multiplatform project without android target should not trigger warning
- [`KT-38456`](https://youtrack.jetbrains.com/issue/KT-38456) MPP with Android source set: allTests task does not execute Android unit tests
- [`KT-44227`](https://youtrack.jetbrains.com/issue/KT-44227) Common tests are not launched on local JVM for Android via allTests task in a multiplatform project
- [`KT-51946`](https://youtrack.jetbrains.com/issue/KT-51946) Temporarily mark HMPP tasks as notCompatibleWithConfigurationCache for Gradle 7.4
- [`KT-52140`](https://youtrack.jetbrains.com/issue/KT-52140) Support extensibility Kotlin Artifacts DSL by external gradle plugins
- [`KT-51947`](https://youtrack.jetbrains.com/issue/KT-51947) Mark HMPP tasks as notCompatibleWithConfigurationCache for Gradle 7.4 using Reflection
- [`KT-50925`](https://youtrack.jetbrains.com/issue/KT-50925) Could not resolve all files for configuration ':metadataCompileClasspath'
- [`KT-51262`](https://youtrack.jetbrains.com/issue/KT-51262) [KPM] IDEA import: Move model builder to KGP
- [`KT-51220`](https://youtrack.jetbrains.com/issue/KT-51220) [KPM][Android] Implement generic data storage and import pipeline
- [`KT-48649`](https://youtrack.jetbrains.com/issue/KT-48649) No run task generated for macosArm64 target in Gradle plugin

### Tools. Gradle. Native

- [`KT-47746`](https://youtrack.jetbrains.com/issue/KT-47746) Allow customization of the Kotlin/Native compiler download url
- [`KT-51884`](https://youtrack.jetbrains.com/issue/KT-51884) Gradle Native: "A problem occurred starting process 'command 'xcodebuild''" when building `assembleFooXCFramework` task on Linux

### Tools. Incremental Compile

- [`KT-51546`](https://youtrack.jetbrains.com/issue/KT-51546) FIR incremental compilation fails with assertion "Trying to inline an anonymous object which is not part of the public ABI"
- [`KT-49780`](https://youtrack.jetbrains.com/issue/KT-49780) IncrementalCompilerRunner bug: Outputs are deleted after successful rebuild following fallback from an exception
- [`KT-44741`](https://youtrack.jetbrains.com/issue/KT-44741) Incremental compilation: inspectClassesForKotlinIC doesn't determine changes with imported constant

### Tools. JPS

- [`KTIJ-17280`](https://youtrack.jetbrains.com/issue/KTIJ-17280) JPS: don't use java.io.File.createTempFile as it is not working sometimes
- [`KTIJ-20954`](https://youtrack.jetbrains.com/issue/KTIJ-20954) NPE at at org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil.readNameResolver on compiling by JPS with LV > 1.7

### Tools. Kapt

- [`KT-49533`](https://youtrack.jetbrains.com/issue/KT-49533) Make kapt work out of the box with latest JDKs
- [`KT-52284`](https://youtrack.jetbrains.com/issue/KT-52284) FIR: add error in 1.7.0 branch if run with Kapt
- [`KT-51463`](https://youtrack.jetbrains.com/issue/KT-51463) KAPT: Incremental compilation not working when rerunning unit tests
- [`KT-51132`](https://youtrack.jetbrains.com/issue/KT-51132) KAPT: Support reporting the number of generated files by each annotation processor
- [`KT-30172`](https://youtrack.jetbrains.com/issue/KT-30172) Kapt: Shutdown kotlinc gracefully in case of error in annotation processor

### Tools. Scripts

- [`KT-49173`](https://youtrack.jetbrains.com/issue/KT-49173) Add support for nullable types in provided properties and other configuration-defined declarations
- [`KT-52294`](https://youtrack.jetbrains.com/issue/KT-52294) [Scripting] Update oudated org.eclipse.aether dependencies to new org.apache.maven.resolver
- [`KT-51213`](https://youtrack.jetbrains.com/issue/KT-51213) Kotlin JSR223 crashes with "ScriptException: ERROR java.lang.NullPointerException:" if bindings contain one or more null values
- [`KT-48812`](https://youtrack.jetbrains.com/issue/KT-48812) Script: "IllegalStateException: unknown classifier kind SCRIPT" when passing a function reference to a Flow
- [`KT-50902`](https://youtrack.jetbrains.com/issue/KT-50902) Scripts loaded from the compilation cache ignore the `loadDependencies` eval configuration property
- [`KT-52186`](https://youtrack.jetbrains.com/issue/KT-52186) Scripts: Backend Internal error: Exception during IR lowering when using symbol from a dependency inside a function
- [`KT-51731`](https://youtrack.jetbrains.com/issue/KT-51731) Script: jsr223 memory leak in spring-boot Fat Jar
- [`KT-49258`](https://youtrack.jetbrains.com/issue/KT-49258) Scripts: method 'void <init>()' not found with multiple evals using kotlin script JSR223
- [`KT-51346`](https://youtrack.jetbrains.com/issue/KT-51346) Scripts: "BackendException: Exception during IR lowering" with variable of imported script inside class


## 1.7.0-RC2

### Compiler

- [`KT-52311`](https://youtrack.jetbrains.com/issue/KT-52311) java.lang.VerifyError: Bad type on operand stack
- [`KT-52503`](https://youtrack.jetbrains.com/issue/KT-52503) New green code appeared at the callable reference resolution

### JavaScript

- [`KT-52518`](https://youtrack.jetbrains.com/issue/KT-52518) Kotlin/JS IR: project with 1.6.21 fails to consume library built with 1.7.0-RC: ISE "Unexpected IrType kind: KIND_NOT_SET" at IrDeclarationDeserializer.deserializeIrTypeData()

### Tools. CLI

- [`KT-52409`](https://youtrack.jetbrains.com/issue/KT-52409) Report error when use-k2 with Multiplatform

### Tools. Gradle

- [`KT-52509`](https://youtrack.jetbrains.com/issue/KT-52509) Main variant published to Gradle plugin portal uses unshadowed artifact
- [`KT-52392`](https://youtrack.jetbrains.com/issue/KT-52392) Gradle: 1.7.0 does not support custom gradle build configuration on Windows OS

### Tools. Kapt

- [`KT-52284`](https://youtrack.jetbrains.com/issue/KT-52284) FIR: add error in 1.7.0 branch if run with Kapt


## 1.7.0-RC

### Compiler

- [`KT-51640`](https://youtrack.jetbrains.com/issue/KT-51640) FIR: remove warning about "far from being production ready"
- [`KT-52404`](https://youtrack.jetbrains.com/issue/KT-52404) Prolong deprecation cycle for errors at contravariant usages of star projected argument from Java
- [`KT-51844`](https://youtrack.jetbrains.com/issue/KT-51844) New errors in overload resolution involving vararg extension methods
- [`KT-50877`](https://youtrack.jetbrains.com/issue/KT-50877) Inconsistent flexible type
- [`KT-51988`](https://youtrack.jetbrains.com/issue/KT-51988) "NPE: getContainingDeclaration…lDeclarationType.REGULAR) must not be null" when using @BuilderInference with multiple type arguments
- [`KT-51925`](https://youtrack.jetbrains.com/issue/KT-51925) Native: "IllegalStateException: Symbol for kotlinx.cinterop/CStructVar|null[0] is unbound" caused by inline function
- [`KT-52035`](https://youtrack.jetbrains.com/issue/KT-52035) FIR: add error in 1.7.0 branch if run on JS / Native configuration
- [`KT-52037`](https://youtrack.jetbrains.com/issue/KT-52037) FIR: add error in 1.7.0 branch if run with non-compatible plugins

### JavaScript

- [`KT-52144`](https://youtrack.jetbrains.com/issue/KT-52144) KJS / IR: Missing property definitions for interfaced defined properties
- [`KT-51973`](https://youtrack.jetbrains.com/issue/KT-51973) KJS / IR overridden properties of inherited interface missing

### Native. Platforms

- [`KT-52232`](https://youtrack.jetbrains.com/issue/KT-52232) Kotlin/Native: simplify toolchain dependency override for MinGW

### Native. Runtime

- [`KT-52365`](https://youtrack.jetbrains.com/issue/KT-52365) Kotlin/Native fails to compile projects for 32-bit targets when new memory manager is enabled

### Tools. Commonizer

- [`KT-51224`](https://youtrack.jetbrains.com/issue/KT-51224) MPP: For optimistically commonized numbers missed kotlinx.cinterop.UnsafeNumber
- [`KT-51215`](https://youtrack.jetbrains.com/issue/KT-51215) MPP: Update Kdoc description for kotlinx.cinterop.UnsafeNumber

### Tools. Gradle

- [`KT-52187`](https://youtrack.jetbrains.com/issue/KT-52187) New IC can not be enabled in an Android project using kapt
- [`KT-51898`](https://youtrack.jetbrains.com/issue/KT-51898) Upgrading Kotlin/Kotlin Gradle plugin to 1.5.3 and above breaks 'com.android.asset-pack' plugin
- [`KT-51913`](https://youtrack.jetbrains.com/issue/KT-51913) Gradle plugin should not add attributes to the legacy configurations
- [`KT-52313`](https://youtrack.jetbrains.com/issue/KT-52313) No recompilation in Gradle after adding or removing function parameters, removing functions (and maybe more) in dependent modules
- [`KT-52141`](https://youtrack.jetbrains.com/issue/KT-52141) Optimize Java class snapshotting for the `kotlin.incremental.useClasspathSnapshot` feature
- [`KT-51978`](https://youtrack.jetbrains.com/issue/KT-51978) Optimize classpath snapshot cache for the `kotlin.incremental.useClasspathSnapshot` feature
- [`KT-51415`](https://youtrack.jetbrains.com/issue/KT-51415) Confusing build failure reason is displayed in case kapt is used and different JDKs are used for compileKotlin and compileJava tasks

### Tools. Gradle. Cocoapods

- [`KT-51861`](https://youtrack.jetbrains.com/issue/KT-51861) Custom binary name in CocoaPods plugin isn't respected by fatFramework task

### Tools. Gradle. JS

- [`KT-51895`](https://youtrack.jetbrains.com/issue/KT-51895) K/JS: Redundant technical messages during JS tests

### Tools. Gradle. Multiplatform

- [`KT-51947`](https://youtrack.jetbrains.com/issue/KT-51947) Mark HMPP tasks as notCompatibleWithConfigurationCache for Gradle 7.4 using Reflection

### Tools. Kapt

- [`KT-51463`](https://youtrack.jetbrains.com/issue/KT-51463) KAPT: Incremental compilation not working when rerunning unit tests

### Tools. Scripts

- [`KT-49173`](https://youtrack.jetbrains.com/issue/KT-49173) Add support for nullable types in provided properties and other configuration-defined declarations
- [`KT-51213`](https://youtrack.jetbrains.com/issue/KT-51213) Kotlin JSR223 crashes with "ScriptException: ERROR java.lang.NullPointerException:" if bindings contain one or more null values
- [`KT-48812`](https://youtrack.jetbrains.com/issue/KT-48812) Script: "IllegalStateException: unknown classifier kind SCRIPT" when passing a function reference to a Flow
- [`KT-50902`](https://youtrack.jetbrains.com/issue/KT-50902) Scripts loaded from the compilation cache ignore the `loadDependencies` eval configuration property


## 1.7.0-Beta

### Analysis API. FIR

- [`KT-50864`](https://youtrack.jetbrains.com/issue/KT-50864) Analysis API: ISE: "KtCallElement should always resolve to a KtCallInfo" is thrown on call resolution inside plusAssign target
- [`KT-50252`](https://youtrack.jetbrains.com/issue/KT-50252) Analysis API: Implement FirModuleResolveStates for libraries
- [`KT-50862`](https://youtrack.jetbrains.com/issue/KT-50862) Analsysis API: do not create use site subsitution override symbols

### Analysis API. FIR Low Level API

- [`KT-50729`](https://youtrack.jetbrains.com/issue/KT-50729) Type bound is not fully resolved
- [`KT-50728`](https://youtrack.jetbrains.com/issue/KT-50728) Lazy resolve of extension function from 'kotlin' package breaks over unresolved type
- [`KT-50271`](https://youtrack.jetbrains.com/issue/KT-50271) Analysis API: get rid of using FirRefWithValidityCheck

### Backend. Native. Debug

- [`KT-50558`](https://youtrack.jetbrains.com/issue/KT-50558) K/N Debugger. Error is not displayed in variables view for catch block

### Compiler

#### New Features

- [`KT-45165`](https://youtrack.jetbrains.com/issue/KT-45165) Remove JVM target version 1.6
- [`KT-51737`](https://youtrack.jetbrains.com/issue/KT-51737) Kotlin/Native: Remove unnecessary safepoints on watchosArm32 and iosArm32 targets

#### Performance Improvements

- [`KT-51699`](https://youtrack.jetbrains.com/issue/KT-51699) Kotlin/Native: runtime has no LTO in debug binaries
- [`KT-34466`](https://youtrack.jetbrains.com/issue/KT-34466) Use optimized switch over enum only when all entries are constant enum entry expressions
- [`KT-50861`](https://youtrack.jetbrains.com/issue/KT-50861) FIR: Combination of array set convention and plusAssign works exponentially
- [`KT-47171`](https://youtrack.jetbrains.com/issue/KT-47171) For loop doesn't avoid boxing with value class iterators (JVM)
- [`KT-29199`](https://youtrack.jetbrains.com/issue/KT-29199) 'next' calls for iterators of merged primitive progressive values are not specialized
- [`KT-50585`](https://youtrack.jetbrains.com/issue/KT-50585) JVM IR: Array constructor loop should use IINC
- [`KT-22429`](https://youtrack.jetbrains.com/issue/KT-22429) Optimize 'for' loop code generation for reversed arrays
- [`KT-50074`](https://youtrack.jetbrains.com/issue/KT-50074) Performance regression in String-based 'when' with single equality clause
- [`KT-22334`](https://youtrack.jetbrains.com/issue/KT-22334) Compiler backend could generate smaller code for loops using range such as integer..array.size -1
- [`KT-35272`](https://youtrack.jetbrains.com/issue/KT-35272) Unnecessary null check on unsafe cast after not-null assertion operator
- [`KT-27427`](https://youtrack.jetbrains.com/issue/KT-27427) Optimize nullable check introduced with 'as' cast

#### Fixes

- [`KT-51433`](https://youtrack.jetbrains.com/issue/KT-51433) FE 1.0: implement warnings about label resolve changes
- [`KT-52146`](https://youtrack.jetbrains.com/issue/KT-52146) JVM IR: "AssertionError: Primitive array expected" on vararg of SAM types with self-type and star projection
- [`KT-51818`](https://youtrack.jetbrains.com/issue/KT-51818) "ClassCastException: class CoroutineSingletons cannot be cast to class" with suspendCoroutineUninterceptedOrReturn and coroutines
- [`KT-50730`](https://youtrack.jetbrains.com/issue/KT-50730) Implement error for a super class constructor call on a function interface in supertypes list
- [`KT-52040`](https://youtrack.jetbrains.com/issue/KT-52040) JVM: ClassFormatError Illegal method name "expectFailure$<init>__proxy-0"
- [`KT-51927`](https://youtrack.jetbrains.com/issue/KT-51927) Native: `The symbol of unexpected type encountered during IR deserialization` error when multiple libraries have non-conflicting declarations with the same name
- [`KT-50845`](https://youtrack.jetbrains.com/issue/KT-50845) Postpone rxjava errors reporting in the strict mode till 1.8 due to found broken cases
- [`KT-48890`](https://youtrack.jetbrains.com/issue/KT-48890) Revert Opt-In restriction "Overriding methods can only have opt-in annotations that are present on their basic declarations."
- [`KT-51979`](https://youtrack.jetbrains.com/issue/KT-51979) "AssertionError: No modifier list, but modifier has been found by the analyzer" exception on incorrect Java interface override
- [`KT-50378`](https://youtrack.jetbrains.com/issue/KT-50378) Unresolved reference for method in Jsoup library in a kts script file
- [`KT-34919`](https://youtrack.jetbrains.com/issue/KT-34919) "Visibility is unknown yet" when named parameter in a function type used in a typealias implemented by an abstract class
- [`KT-51893`](https://youtrack.jetbrains.com/issue/KT-51893) Duplicated [OVERRIDE_DEPRECATION] on overridden properties
- [`KT-41034`](https://youtrack.jetbrains.com/issue/KT-41034) K2: Change evaluation semantics for combination of safe calls and convention operators
- [`KT-51843`](https://youtrack.jetbrains.com/issue/KT-51843) Functional interface constructor references are incorrectly allowed in 1.6.20 without any compiler flags
- [`KT-51914`](https://youtrack.jetbrains.com/issue/KT-51914) False positive RETURN_TYPE_MISMATCH in intellij ultimate
- [`KT-51711`](https://youtrack.jetbrains.com/issue/KT-51711) Compiler warning is displayed in case there is 'if' else branch used with elvis
- [`KT-49317`](https://youtrack.jetbrains.com/issue/KT-49317) "IllegalStateException: Parent of this declaration is not a class: FUN LOCAL_FUNCTION_FOR_LAMBDA" with parameter of suspend type with the default parameter
- [`KT-33517`](https://youtrack.jetbrains.com/issue/KT-33517) Kotlin ScriptEngine does not respect async code when using bindings
- [`KT-44705`](https://youtrack.jetbrains.com/issue/KT-44705) Deprecate using non-exhaustive if's and when's in rhs of elvis
- [`KT-44510`](https://youtrack.jetbrains.com/issue/KT-44510) FIR DFA: smartcast after elvis with escaping lambda
- [`KT-44879`](https://youtrack.jetbrains.com/issue/KT-44879) FIR DFA: Track `inc` and `dec` operator calls in preliminary loop visitor
- [`KT-51624`](https://youtrack.jetbrains.com/issue/KT-51624) FIR: false-positive INAPPLICABLE_LATEINIT_MODIFIER for lateinit properties with unresolved types
- [`KT-51204`](https://youtrack.jetbrains.com/issue/KT-51204) FIR IC: Incremental compilation fails on nested crossinline
- [`KT-51798`](https://youtrack.jetbrains.com/issue/KT-51798) Fix ISE from IR backend when data class inherits equals/hashCode/toString with incompatible signature
- [`KT-51499`](https://youtrack.jetbrains.com/issue/KT-51499) @file:OptIn doesn't cover override methods
- [`KT-46187`](https://youtrack.jetbrains.com/issue/KT-46187) FIR: OVERLOAD_RESOLUTION_AMBIGUITY on SAM-converted callable reference to List::plus
- [`KT-49778`](https://youtrack.jetbrains.com/issue/KT-49778) Support cast to DefinitelyNotNull type in Native
- [`KT-51718`](https://youtrack.jetbrains.com/issue/KT-51718) JVM / IR: "VerifyError: Bad type on operand stack" caused by nullable variable inside suspend function
- [`KT-34515`](https://youtrack.jetbrains.com/issue/KT-34515) NI: "AssertionError: Base expression was not processed: POSTFIX_EXPRESSION" with double not-null assertion to brackets
- [`KT-48546`](https://youtrack.jetbrains.com/issue/KT-48546) PSI2IR: "org.jetbrains.kotlin.psi2ir.generators.ErrorExpressionException: null: KtCallExpression" with recursive property access in lazy block
- [`KT-28109`](https://youtrack.jetbrains.com/issue/KT-28109) "AssertionError: No setter call" for incrementing parenthesized result of indexed access convention operator
- [`KT-46136`](https://youtrack.jetbrains.com/issue/KT-46136) Unsubstituted return type inferred for a function returning anonymous object upcast to supertype
- [`KT-51621`](https://youtrack.jetbrains.com/issue/KT-51621) FIR: visible VS invisible qualifier conflict
- [`KT-50468`](https://youtrack.jetbrains.com/issue/KT-50468) FIR compilers fails with CCE when meets top-level destruction
- [`KT-51557`](https://youtrack.jetbrains.com/issue/KT-51557) Inline stack frame is not shown for default inline lambda
- [`KT-51358`](https://youtrack.jetbrains.com/issue/KT-51358) OptIn: show default warning/error message in case of empty message argument
- [`KT-44152`](https://youtrack.jetbrains.com/issue/KT-44152) FIR2IR fails on declarations from java stdlib if java classes are loaded from PSI instead of binaries
- [`KT-50949`](https://youtrack.jetbrains.com/issue/KT-50949) PSI2IR: NSEE from `ArgumentsGenerationUtilsKt.createFunctionForSuspendConversion` with providing lambda as argument with suspend type
- [`KT-51439`](https://youtrack.jetbrains.com/issue/KT-51439) FE 1.0: implement type variance conflict deprecation on qualifier type arguments
- [`KT-39256`](https://youtrack.jetbrains.com/issue/KT-39256) ArrayStoreException with list of anonymous objects with inferred types created in reified extension function
- [`KT-39883`](https://youtrack.jetbrains.com/issue/KT-39883) Deprecate computing constant values of complex boolean expressions in when condition branches and conditions of loops
- [`KT-36952`](https://youtrack.jetbrains.com/issue/KT-36952) Exception during codegen: cannot pop operand off an empty stack (reference equality, implicit boxing, type check)
- [`KT-51233`](https://youtrack.jetbrains.com/issue/KT-51233) AssertionError in JavaLikeCounterLoopBuilder with Compose
- [`KT-51254`](https://youtrack.jetbrains.com/issue/KT-51254) Verify Error on passing null to type parameter extending inline class
- [`KT-50996`](https://youtrack.jetbrains.com/issue/KT-50996) [FIR] Support Int -> Long conversion for property initializers
- [`KT-51000`](https://youtrack.jetbrains.com/issue/KT-51000) [FIR] Support Int -> Long? conversion
- [`KT-51003`](https://youtrack.jetbrains.com/issue/KT-51003) [FIR] Consider Int -> Long conversion if expected type is type variable
- [`KT-51018`](https://youtrack.jetbrains.com/issue/KT-51018) [FIR] Wrong type inference if one of constraints is integer literal
- [`KT-51446`](https://youtrack.jetbrains.com/issue/KT-51446) Metadata serialization crashes with IOOBE when deserializing underlying inline class value with type table enabled
- [`KT-50973`](https://youtrack.jetbrains.com/issue/KT-50973) Redundant line number mapping for finally block with JVM IR
- [`KT-51272`](https://youtrack.jetbrains.com/issue/KT-51272) Incompatible types: KClass<Any> and callable reference Collection::class
- [`KT-51229`](https://youtrack.jetbrains.com/issue/KT-51229) FIR: private constructor of internal data class treated as internal and not private
- [`KT-50750`](https://youtrack.jetbrains.com/issue/KT-50750) [FIR] Report UNSUPPORTED on array literals not from annotation classes
- [`KT-49804`](https://youtrack.jetbrains.com/issue/KT-49804) False positive of UPPER_BOUND_VIOLATED and RETURN_TYPE_MISMATCH
- [`KT-51121`](https://youtrack.jetbrains.com/issue/KT-51121) Inconsistent SAM behavior in multiple cases causing AbstractMethodError (Kotlin 1.6.10)
- [`KT-49925`](https://youtrack.jetbrains.com/issue/KT-49925) [FIR] Incorrect builder inference (different cases)
- [`KT-50542`](https://youtrack.jetbrains.com/issue/KT-50542) "IllegalStateException: Type parameter descriptor is not initialized: T declared in sort" with definitely non-null type Any & T in generic constraint
- [`KT-51235`](https://youtrack.jetbrains.com/issue/KT-51235) JVM / IR: "AbstractMethodError: Receiver class does not define or inherit an implementation of the resolved method" when property with inline class type is overridden to return Nothing?
- [`KT-51223`](https://youtrack.jetbrains.com/issue/KT-51223) Report warning about conflicting inherited members from deserialized dependencies
- [`KT-51156`](https://youtrack.jetbrains.com/issue/KT-51156) Multiplatform linkDebugFramework task throws NoSuchElementException when expect class constructors utilize nested enum constant
- [`KT-51017`](https://youtrack.jetbrains.com/issue/KT-51017) [FIR] Ambiguity on callable reference between two functions on generic receiver with different bounds
- [`KT-51007`](https://youtrack.jetbrains.com/issue/KT-51007) [FIR] False positive ILLEGAL_SUSPEND_FUNCTION_CALL if fun interface with suspend function declared in another module
- [`KT-50998`](https://youtrack.jetbrains.com/issue/KT-50998) [FIR] Int.inv() cal does not considered as compile time call
- [`KT-51009`](https://youtrack.jetbrains.com/issue/KT-51009) [FIR] Incorrect inference of lambda in position of return
- [`KT-50997`](https://youtrack.jetbrains.com/issue/KT-50997) [FIR] Incorrect type of typealias for suspend functional type
- [`KT-49714`](https://youtrack.jetbrains.com/issue/KT-49714) Compiler reports "'operator modifier is inapplicable" if expect class with increment operator is provided via type alias
- [`KT-44623`](https://youtrack.jetbrains.com/issue/KT-44623) "IllegalStateException: IdSignature is allowed only for PublicApi symbols" when suspending receiver is annotated with something
- [`KT-46000`](https://youtrack.jetbrains.com/issue/KT-46000) JVM / IR: AssertionError on isSubtypeOfClass check in copyValueParametersToStatic with Compose
- [`KT-50211`](https://youtrack.jetbrains.com/issue/KT-50211) Annotation Instantiation with default arguments in Native
- [`KT-49412`](https://youtrack.jetbrains.com/issue/KT-49412) Controversial "type argument is not within its bounds" reported by FIR
- [`KT-48044`](https://youtrack.jetbrains.com/issue/KT-48044) [FIR] Investigate behavior of `UPPER_BOUND_VIOLATED` on complex cases
- [`KT-37975`](https://youtrack.jetbrains.com/issue/KT-37975) Don't show deprecation of enum class itself for its own member
- [`KT-50737`](https://youtrack.jetbrains.com/issue/KT-50737) Inheritance from SuspendFunction leads to compiler crash
- [`KT-50723`](https://youtrack.jetbrains.com/issue/KT-50723) Implement a fix of reporting of uninitialized parameter in default values of parameters
- [`KT-50749`](https://youtrack.jetbrains.com/issue/KT-50749) Implement UNSUPPORTED reporting on array literals inside objects in annotation classes
- [`KT-50753`](https://youtrack.jetbrains.com/issue/KT-50753) Implement reporting errors on cycles in annotation parameter types
- [`KT-50758`](https://youtrack.jetbrains.com/issue/KT-50758) Fix inconsistency of exceptions at init block for an enum entry with and without a qualifier name
- [`KT-50182`](https://youtrack.jetbrains.com/issue/KT-50182) CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT: clarify error message for `const` in object expression
- [`KT-50183`](https://youtrack.jetbrains.com/issue/KT-50183) Fix missing apostrophe escapes in compiler error messages
- [`KT-50822`](https://youtrack.jetbrains.com/issue/KT-50822) Analysis API: make declaration transformers machinery to be a thread safe
- [`KT-50835`](https://youtrack.jetbrains.com/issue/KT-50835) Inline functions with suspend lambdas break the tail-call optimization
- [`KT-49485`](https://youtrack.jetbrains.com/issue/KT-49485) JVM / IR: StackOverflowError with long when-expression conditions
- [`KT-35684`](https://youtrack.jetbrains.com/issue/KT-35684) NI: "IllegalStateException: Expected some types" from builder-inference about intersecting empty types on trivial code
- [`KT-48908`](https://youtrack.jetbrains.com/issue/KT-48908) Error for annotation on parameter type could have distinct ID and message referring 1.6
- [`KT-48907`](https://youtrack.jetbrains.com/issue/KT-48907) SUPERTYPE_IS_SUSPEND_FUNCTION_TYPE error could have message referring version 1.6
- [`KT-34338`](https://youtrack.jetbrains.com/issue/KT-34338) Parameterless main causes duplicate JVM signature error
- [`KT-50577`](https://youtrack.jetbrains.com/issue/KT-50577) JVM_IR: No NPE when casting uninitialized value of non-null type to non-null type
- [`KT-50476`](https://youtrack.jetbrains.com/issue/KT-50476) JVM_IR: NSME when calling 'super.removeAt(Int)' implemented in Java interface as a default method
- [`KT-50257`](https://youtrack.jetbrains.com/issue/KT-50257) JVM_IR: Incorrect bridge delegate signature for renamed remove(I) causes SOE with Kotlin class inherited from fastutils IntArrayList
- [`KT-50470`](https://youtrack.jetbrains.com/issue/KT-50470) FIR: inapplicable candidate in delegate inference due to nullability
- [`KT-32744`](https://youtrack.jetbrains.com/issue/KT-32744) Inefficient compilation of null-safe call (extra null checks, unreachable code)
- [`KT-36095`](https://youtrack.jetbrains.com/issue/KT-36095) 201: False positive OVERLOAD_RESOLUTION_AMBIGUITY with Java `Enum.valueOf` and `Enum.values()` reference

### IDE

- [`KT-50111`](https://youtrack.jetbrains.com/issue/KT-50111) Resolving into KtUltraLightMethod

### IDE. Decompiler, Indexing, Stubs

- [`KT-51248`](https://youtrack.jetbrains.com/issue/KT-51248) Function and parameter names with special symbols have to backticked

### IDE. Multiplatform

- [`KT-50952`](https://youtrack.jetbrains.com/issue/KT-50952) MPP: Commonized cinterops doesn't attach/detach to source set on configuration changes

### JavaScript

#### New Features

- [`KT-51735`](https://youtrack.jetbrains.com/issue/KT-51735) KJS / IR: Minimize member names in production mode

#### Performance Improvements

- [`KT-51127`](https://youtrack.jetbrains.com/issue/KT-51127) Kotlin/JS - IR generates plenty of useless `Unit_getInstance()`
- [`KT-50212`](https://youtrack.jetbrains.com/issue/KT-50212) KJS IR: Upcast should be a no-op
- [`KT-16974`](https://youtrack.jetbrains.com/issue/KT-16974) JS: Kotlin.charArrayOf is suboptimal due to Rhino bugs

#### Fixes

- [`KT-51125`](https://youtrack.jetbrains.com/issue/KT-51125) Provide a way to use `import` keyword in `js` expressions
- [`KT-50504`](https://youtrack.jetbrains.com/issue/KT-50504) KJS / IR: Transpiled JS incorrectly uses the unscrambled names of internal fields
- [`KT-52010`](https://youtrack.jetbrains.com/issue/KT-52010) K/JS IR: both flows execute when using elvis operator
- [`KT-51853`](https://youtrack.jetbrains.com/issue/KT-51853) JS compilation fails with "Uninitialized fast cache info" error
- [`KT-51205`](https://youtrack.jetbrains.com/issue/KT-51205) K/JS IR: external class is mapped to any
- [`KT-50806`](https://youtrack.jetbrains.com/issue/KT-50806) Typescript definitions contain invalid nested block comments with generic parent and type argument without `@JsExport`
- [`KT-51081`](https://youtrack.jetbrains.com/issue/KT-51081) KJS / IR + IC: Passing an inline function with default params as a param to a higher-order function crashes the compiler
- [`KT-51084`](https://youtrack.jetbrains.com/issue/KT-51084) KJS / IR + IC: Cache invalidation doesn't check generic inline functions reified qualifier
- [`KT-51211`](https://youtrack.jetbrains.com/issue/KT-51211) K/JS IR: JsExport:  Can't export nested enum
- [`KT-51438`](https://youtrack.jetbrains.com/issue/KT-51438) KJS / IR: Duplicated import names for the same external names
- [`KT-50953`](https://youtrack.jetbrains.com/issue/KT-50953) KJS IR: Incorrect nested commenting in d.ts
- [`KT-15223`](https://youtrack.jetbrains.com/issue/KT-15223) JS: function that overrides external function with `vararg` parameter is translated incorrectly
- [`KT-50657`](https://youtrack.jetbrains.com/issue/KT-50657) KJS / IR 1.6.20-M1-39 - Date in Kotlin JS cannot be created from long.

### Language Design

- [`KT-49006`](https://youtrack.jetbrains.com/issue/KT-49006) Support at least three previous versions of language/API
- [`KT-16768`](https://youtrack.jetbrains.com/issue/KT-16768) Context-sensitive resolution prototype (Resolve unqualified enum constants based on expected type)
- [`KT-50477`](https://youtrack.jetbrains.com/issue/KT-50477) Functional conversion does not work on suspending functions
- [`KT-32162`](https://youtrack.jetbrains.com/issue/KT-32162) Allow generics for inline classes
- [`KT-44866`](https://youtrack.jetbrains.com/issue/KT-44866) Change behavior of private constructors of sealed classes
- [`KT-49110`](https://youtrack.jetbrains.com/issue/KT-49110) Prohibit access to members of companion of enum class from initializers of entries of this enum
- [`KT-29405`](https://youtrack.jetbrains.com/issue/KT-29405) Switch default JVM target version to 1.8

### Libraries

#### New Features

- [`KT-50484`](https://youtrack.jetbrains.com/issue/KT-50484) Extensions for java.util.Optional in stdlib
- [`KT-50146`](https://youtrack.jetbrains.com/issue/KT-50146) Reintroduce min/max(By/With) operations on collections with non-nullable return type
- [`KT-46132`](https://youtrack.jetbrains.com/issue/KT-46132) Specialized default time source with non-allocating time marks
- [`KT-41890`](https://youtrack.jetbrains.com/issue/KT-41890) Support named capture groups in Regex on Native
- [`KT-48179`](https://youtrack.jetbrains.com/issue/KT-48179) Introduce API to retrieve the number of CPUs the runtime has

#### Performance Improvements

- [`KT-42178`](https://youtrack.jetbrains.com/issue/KT-42178) Range and Progression should override last()

#### Fixes

- [`KT-51470`](https://youtrack.jetbrains.com/issue/KT-51470) Stabilize experimental API for 1.7
- [`KT-51775`](https://youtrack.jetbrains.com/issue/KT-51775) JS: Support named capture groups in Regex
- [`KT-51776`](https://youtrack.jetbrains.com/issue/KT-51776) Native: Support back references to groups with multi-digit index
- [`KT-51082`](https://youtrack.jetbrains.com/issue/KT-51082) Introduce Enum.declaringJavaClass property
- [`KT-48924`](https://youtrack.jetbrains.com/issue/KT-48924) KJS: `toString` in base 36 produces different results in JS compare to JVM
- [`KT-50742`](https://youtrack.jetbrains.com/issue/KT-50742) Regular expression is fine on jvm but throws PatternSyntaxException for native macosX64 target
- [`KT-50059`](https://youtrack.jetbrains.com/issue/KT-50059) Stop publishing kotlin-stdlib and kotlin-test artifacts under modular classifier
- [`KT-26678`](https://youtrack.jetbrains.com/issue/KT-26678) Rename buildSequence/buildIterator to sequence/iterator

### Native

- [`KT-49406`](https://youtrack.jetbrains.com/issue/KT-49406) Kotlin/Native: generate standalone executable for androidNative targets by default
- [`KT-48595`](https://youtrack.jetbrains.com/issue/KT-48595) Enable Native embeddable compiler jar in Gradle plugin by default
- [`KT-51377`](https://youtrack.jetbrains.com/issue/KT-51377) Native: synthetic forward declarations are preferred over commonized definitions
- [`KT-49145`](https://youtrack.jetbrains.com/issue/KT-49145) Kotlin/Native static library compilation fails for androidNative*
- [`KT-49496`](https://youtrack.jetbrains.com/issue/KT-49496) Gradle (or the KMM plugin) is caching the Xcode Command Line Tools location
- [`KT-49247`](https://youtrack.jetbrains.com/issue/KT-49247) gradle --offline should translate into airplaneMode for kotin-native compiler

### Native. C and ObjC Import

- [`KT-49455`](https://youtrack.jetbrains.com/issue/KT-49455) Methods from Swift extensions are not resolved in Kotlin shared module
- [`KT-50648`](https://youtrack.jetbrains.com/issue/KT-50648) Incorrect KMM cinterop conversion

### Native. ObjC Export

- [`KT-50982`](https://youtrack.jetbrains.com/issue/KT-50982) RuntimeAssertFailedPanic in iOS when Kotlin framework is initialized before loading
- [`KT-49937`](https://youtrack.jetbrains.com/issue/KT-49937) Kotlin/Native 1.5.31: 'runtime assert: Unexpected selector clash' when 'override fun toString(): String' is used

### Native. Runtime. Memory

- [`KT-50713`](https://youtrack.jetbrains.com/issue/KT-50713) Kotlin/Native: Enable Concurrent Sweep GC by default

### Native. Stdlib

- [`KT-50312`](https://youtrack.jetbrains.com/issue/KT-50312) enhancement: kotlin native   -- add  alloc<TVarOf<T>>(T)

### Native. Testing

- [`KT-50316`](https://youtrack.jetbrains.com/issue/KT-50316) Kotlin/Native: Produce a list of available tests alongside the final artifact
- [`KT-50139`](https://youtrack.jetbrains.com/issue/KT-50139) Create tests for Enter/Leave frame optimization

### Reflection

- [`KT-27598`](https://youtrack.jetbrains.com/issue/KT-27598) "KotlinReflectionInternalError" when using `callBy` on constructor that has inline class parameters
- [`KT-31141`](https://youtrack.jetbrains.com/issue/KT-31141) IllegalArgumentException when reflectively accessing nullable property of inline class type

### Tools. CLI

- [`KT-29974`](https://youtrack.jetbrains.com/issue/KT-29974) Add a compiler option '-Xjdk-release' similar to javac's '--release' to control the target JDK version
- [`KT-51717`](https://youtrack.jetbrains.com/issue/KT-51717) IllegalArgumentException: Unexpected versionNeededToExtract (0) in 1.6.20-RC2 with useFir enabled
- [`KT-51673`](https://youtrack.jetbrains.com/issue/KT-51673) Make language version description not in capital letters
- [`KT-48833`](https://youtrack.jetbrains.com/issue/KT-48833) -Xsuppress-version-warnings allows to suppress errors about unsupported versions
- [`KT-51627`](https://youtrack.jetbrains.com/issue/KT-51627) kotlinc fails with `java.lang.RuntimeException` if `/tmp/build.txt` file exists on the disk
- [`KT-51306`](https://youtrack.jetbrains.com/issue/KT-51306) Support reading language settings from an environment variable and overriding the current settings by them

### Tools. Commonizer

- [`KT-52050`](https://youtrack.jetbrains.com/issue/KT-52050) [Commonizer] 'platform.posix.DIR' not implementing 'CPointed' when commonized for 'nativeMain' on linux or windows hosts
- [`KT-51224`](https://youtrack.jetbrains.com/issue/KT-51224) MPP: For optimistically commonized numbers missed kotlinx.cinterop.UnsafeNumber
- [`KT-51215`](https://youtrack.jetbrains.com/issue/KT-51215) MPP: Update Kdoc description for kotlinx.cinterop.UnsafeNumber
- [`KT-51686`](https://youtrack.jetbrains.com/issue/KT-51686) Cinterop: Overload resolution ambiguity in 1.6.20-RC2
- [`KT-46636`](https://youtrack.jetbrains.com/issue/KT-46636) HMPP: missed classes from `platform.posix.*`
- [`KT-51332`](https://youtrack.jetbrains.com/issue/KT-51332) Optimistic number commonization is disabled by default in KGP with enabled HMPP

### Tools. Gradle

#### New Features

- [`KT-50869`](https://youtrack.jetbrains.com/issue/KT-50869) Provide API that allow AGP to set up Kotlin compilation
- [`KT-48008`](https://youtrack.jetbrains.com/issue/KT-48008) Consider offering a KotlinBasePlugin
- [`KT-49227`](https://youtrack.jetbrains.com/issue/KT-49227) Support Gradle plugins variants

#### Fixes

- [`KT-52189`](https://youtrack.jetbrains.com/issue/KT-52189) Provide Gradle Kotlin/DSL friendly deprecated classpath property in KotlinCompiler task
- [`KT-51360`](https://youtrack.jetbrains.com/issue/KT-51360) Show performance difference in percent between releases
- [`KT-51380`](https://youtrack.jetbrains.com/issue/KT-51380) Add open-source project using Kotlin/JS plugin to build regression benchmarks
- [`KT-51937`](https://youtrack.jetbrains.com/issue/KT-51937) Toolchain usage with configuration cache prevents KotlinCompile task to be UP-TO-DATE
- [`KT-48276`](https://youtrack.jetbrains.com/issue/KT-48276) Remove kotlin2js and kotlin-dce-plugin
- [`KT-52138`](https://youtrack.jetbrains.com/issue/KT-52138) KSP could not access internal methods/properties in Kotlin Gradle Plugin
- [`KT-51342`](https://youtrack.jetbrains.com/issue/KT-51342) Set minimal supported Android Gradle plugin version to 3.6.4
- [`KT-50494`](https://youtrack.jetbrains.com/issue/KT-50494) Remove kotlin.experimental.coroutines Gradle DSL option
- [`KT-49733`](https://youtrack.jetbrains.com/issue/KT-49733) Bump minimal supported Gradle version to 6.7.1
- [`KT-48831`](https://youtrack.jetbrains.com/issue/KT-48831) Remove 'KotlinGradleSubplugin'
- [`KT-51830`](https://youtrack.jetbrains.com/issue/KT-51830) Gradle: deprecate `kotlin.compiler.execution.strategy` system property
- [`KT-47763`](https://youtrack.jetbrains.com/issue/KT-47763) Gradle DSL: Remove deprecated useExperimentalAnnotation and experimentalAnnotationInUse
- [`KT-51374`](https://youtrack.jetbrains.com/issue/KT-51374) NoSuchFileException in getOrCreateSessionFlagFile()
- [`KT-51837`](https://youtrack.jetbrains.com/issue/KT-51837) kotlin-gradle-plugin:1.6.20 fails xray scan on shadowed Gson 2.8.6.
- [`KT-45745`](https://youtrack.jetbrains.com/issue/KT-45745) Migrate only Kotlin Gradle Plugin tests to new JUnit5 DSL and run them separately on CI
- [`KT-47318`](https://youtrack.jetbrains.com/issue/KT-47318) Remove deprecated 'kotlinPluginVersion' property in `KotlinBasePluginWrapper'
- [`KT-51378`](https://youtrack.jetbrains.com/issue/KT-51378) Gradle 'buildSrc' compilation fails when newer version of Kotlin plugin is added to the build script classpath
- [`KT-46038`](https://youtrack.jetbrains.com/issue/KT-46038) Gradle: `kotlin_module` files are corrupted in the KotlinCompile output, and gets cached
- [`KT-51064`](https://youtrack.jetbrains.com/issue/KT-51064) Kotlin gradle build hangs on MetricsContainer.flush
- [`KT-31027`](https://youtrack.jetbrains.com/issue/KT-31027) java.lang.NoSuchMethodError: No static method hashCode(Z)I in class Ljava/lang/Boolean; or its super classes (declaration of 'java.lang.Boolean' appears in /system/framework/core-libart.jar)

### Tools. Gradle. JS

- [`KT-51414`](https://youtrack.jetbrains.com/issue/KT-51414) Allow set up environment variables for JS tests
- [`KT-51623`](https://youtrack.jetbrains.com/issue/KT-51623) Kotlin/JS: Mocha could not failed when external module not found
- [`KT-51503`](https://youtrack.jetbrains.com/issue/KT-51503) Update NPM dependency versions

### Tools. Gradle. Multiplatform

- [`KT-51765`](https://youtrack.jetbrains.com/issue/KT-51765) com.android.lint in multiplatform project without android target should not trigger warning
- [`KT-51386`](https://youtrack.jetbrains.com/issue/KT-51386) [KPM] IdeaKotlinProjectModelBuilder: Implement dependencies
- [`KT-51262`](https://youtrack.jetbrains.com/issue/KT-51262) [KPM] IDEA import: Move model builder to KGP
- [`KT-51220`](https://youtrack.jetbrains.com/issue/KT-51220) [KPM][Android] Implement generic data storage and import pipeline
- [`KT-48649`](https://youtrack.jetbrains.com/issue/KT-48649) No run task generated for macosArm64 target in Gradle plugin

### Tools. Gradle. Native

- [`KT-51884`](https://youtrack.jetbrains.com/issue/KT-51884) Gradle Native: "A problem occurred starting process 'command 'xcodebuild''" when building `assembleFooXCFramework` task on Linux

### Tools. Incremental Compile

- [`KT-51546`](https://youtrack.jetbrains.com/issue/KT-51546) FIR incremental compilation fails with assertion "Trying to inline an anonymous object which is not part of the public ABI"
- [`KT-44741`](https://youtrack.jetbrains.com/issue/KT-44741) Incremental compilation: inspectClassesForKotlinIC doesn't determine changes with imported constant

### Tools. Kapt

- [`KT-51132`](https://youtrack.jetbrains.com/issue/KT-51132) KAPT: Support reporting the number of generated files by each annotation processor
- [`KT-30172`](https://youtrack.jetbrains.com/issue/KT-30172) Kapt: Shutdown kotlinc gracefully in case of error in annotation processor

### Tools. Scripts

- [`KT-52186`](https://youtrack.jetbrains.com/issue/KT-52186) Scripts: Backend Internal error: Exception during IR lowering when using symbol from a dependency inside a function
- [`KT-51731`](https://youtrack.jetbrains.com/issue/KT-51731) Script: jsr223 memory leak in spring-boot Fat Jar
- [`KT-49258`](https://youtrack.jetbrains.com/issue/KT-49258) Scripts: method 'void <init>()' not found with multiple evals using kotlin script JSR223
- [`KT-51346`](https://youtrack.jetbrains.com/issue/KT-51346) Scripts: "BackendException: Exception during IR lowering" with variable of imported script inside class


## 1.6.20

### Compiler

#### New Features

- [`KT-48217`](https://youtrack.jetbrains.com/issue/KT-48217) Add an annotation JvmDefaultWithCompatibility to allow generating DefaultImpls classes if -Xjvm-default=all option is used
- [`KT-49929`](https://youtrack.jetbrains.com/issue/KT-49929) [FIR] Support programmatic creation of annotation class instances
- [`KT-49276`](https://youtrack.jetbrains.com/issue/KT-49276) Warn about potential overload resolution change if Range/Progression starts implementing Collection
- [`KT-47902`](https://youtrack.jetbrains.com/issue/KT-47902) Do not propagate method deprecation through overrides
- [`KT-49857`](https://youtrack.jetbrains.com/issue/KT-49857) Require Xcode 13 for building Kotlin/Native compiler
- [`KT-47701`](https://youtrack.jetbrains.com/issue/KT-47701) Support instantiation of annotation classes on Native
- [`KT-46085`](https://youtrack.jetbrains.com/issue/KT-46085) Support experimental parallel compilation of a single module in the JVM backend
- [`KT-46603`](https://youtrack.jetbrains.com/issue/KT-46603) Generate SAM-conversions to Java interfaces extending 'java.io.Serializable' as serializable using java.lang.invoke.LambdaMetafactory

#### Performance Improvements

- [`KT-50156`](https://youtrack.jetbrains.com/issue/KT-50156) HMPP: Slow frontend/ide performance in OKIO (ExpectActualDeclarationChecker)
- [`KT-50073`](https://youtrack.jetbrains.com/issue/KT-50073) Performance regression in adapted function references
- [`KT-50076`](https://youtrack.jetbrains.com/issue/KT-50076) Performance regression in super call to an interface member in $DefaultImpls
- [`KT-50080`](https://youtrack.jetbrains.com/issue/KT-50080) Performance regression in string template with generic property with primitive upper bound
- [`KT-50084`](https://youtrack.jetbrains.com/issue/KT-50084) Performance regression in concatenation with 'String?'
- [`KT-50078`](https://youtrack.jetbrains.com/issue/KT-50078) Performance regression in for-in-array loop
- [`KT-50039`](https://youtrack.jetbrains.com/issue/KT-50039) Performance regression in inner class constructor call with default parameters
- [`KT-48784`](https://youtrack.jetbrains.com/issue/KT-48784) An anonymous class has fields for variables that are only used in the constructor
- [`KT-42010`](https://youtrack.jetbrains.com/issue/KT-42010) Generate IINC instruction for postfix increment in JVM_IR
- [`KT-48433`](https://youtrack.jetbrains.com/issue/KT-48433) JVM_IR don't generate null check on 'this$0' parameter of inner class constructor
- [`KT-48435`](https://youtrack.jetbrains.com/issue/KT-48435) JVM_IR ConstForLoopBenchmark performance regression
- [`KT-48507`](https://youtrack.jetbrains.com/issue/KT-48507) JVM_IR ForLoopBenchmark regressions
- [`KT-48640`](https://youtrack.jetbrains.com/issue/KT-48640) Performance regression in 'longDownToLoop' benchmarks
- [`KT-29822`](https://youtrack.jetbrains.com/issue/KT-29822) Generate specialized bytecode for loops withIndex over unsigned arrays
- [`KT-48669`](https://youtrack.jetbrains.com/issue/KT-48669) Generate optimizable counter loop for loops over indices of unsigned arrays
- [`KT-49444`](https://youtrack.jetbrains.com/issue/KT-49444) Possible performance degradation with UInt downTo loop
- [`KT-48944`](https://youtrack.jetbrains.com/issue/KT-48944) Possible performance regression with comparison of local KFunctions
- [`KT-17111`](https://youtrack.jetbrains.com/issue/KT-17111) Eliminate redundant store/load instructions when the value stored is simple
- [`KT-36837`](https://youtrack.jetbrains.com/issue/KT-36837) Generate more compact code for for-in-range loop in JVM_IR
- [`KT-48947`](https://youtrack.jetbrains.com/issue/KT-48947) JVM / IR Possible performance regression with string templates
- [`KT-48931`](https://youtrack.jetbrains.com/issue/KT-48931) JVM / IR: Performance degradation with string concatenation
- [`KT-36654`](https://youtrack.jetbrains.com/issue/KT-36654) Generate more compact bytecode for safe call in JVM_IR

#### Fixes

- [`KT-24643`](https://youtrack.jetbrains.com/issue/KT-24643) Prohibit using a type parameter declared for an extension property inside delegate
- [`KT-51747`](https://youtrack.jetbrains.com/issue/KT-51747) Make `KtCallableDeclaration.getContextReceivers` default to preserve compatibility
- [`KT-49658`](https://youtrack.jetbrains.com/issue/KT-49658) NI: False negative TYPE_MISMATCH on nullable type with `when`
- [`KT-43493`](https://youtrack.jetbrains.com/issue/KT-43493) NI: @BuilderInference prevents compilation error of "Operator '==' cannot be applied to 'Long' and 'Int'"
- [`KT-51649`](https://youtrack.jetbrains.com/issue/KT-51649) Kotlin/Native: reduce binary size of watchosArm32 and iosArm32 targets by limiting inlining of runtime functions
- [`KT-48626`](https://youtrack.jetbrains.com/issue/KT-48626) JVM IR: incorrect behavior for captured for-loop parameter since 1.6.20-dev-723
- [`KT-51036`](https://youtrack.jetbrains.com/issue/KT-51036) JVM / IR: "NullPointerException: Parameter specified as non-null is null"  with synchronized and companion object
- [`KT-51471`](https://youtrack.jetbrains.com/issue/KT-51471) Native: incorrect debug information when inheriting suspend fun invoke implementation
- [`KT-51352`](https://youtrack.jetbrains.com/issue/KT-51352) "ClassCastException: class ScopeCoroutine cannot be cast to class Iterable" caused by coroutines and context receivers
- [`KT-51271`](https://youtrack.jetbrains.com/issue/KT-51271) "ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 3" with inlining of context function
- [`KT-47084`](https://youtrack.jetbrains.com/issue/KT-47084) JVM IR: "AssertionError: inconsistent parent function for CLASS LAMBDA_IMPL" with tailrec function default parameter nested inline lambda
- [`KT-30616`](https://youtrack.jetbrains.com/issue/KT-30616) Script: "Don't know how to generate outer expression" for top-level variable reference from static context (companion object, enum)
- [`KT-50520`](https://youtrack.jetbrains.com/issue/KT-50520) "NPE: containingDeclaration.ac…lDeclarationType.REGULAR) must not be null" with implicit type on self-referencing lambda in a builder
- [`KT-51353`](https://youtrack.jetbrains.com/issue/KT-51353) IncompatibleClassChangeError: Expected non-static field com.soywiz.korim.color.Colors.BLACK
- [`KT-48945`](https://youtrack.jetbrains.com/issue/KT-48945) JVM IR: special bridge for `get` is not generated in a Map subclass
- [`KT-48499`](https://youtrack.jetbrains.com/issue/KT-48499) Interface call with an inline/value parameter generates AbstractMethodError after rebuilding the module, but NOT the file.
- [`KT-49998`](https://youtrack.jetbrains.com/issue/KT-49998) JVM: missing default value for annotation parameter of an unsigned type
- [`KT-49793`](https://youtrack.jetbrains.com/issue/KT-49793) JVM: `IncompatibleClassChangeError: Expected non-static field`  when property delegation uses receiver of another delegated property
- [`KT-51302`](https://youtrack.jetbrains.com/issue/KT-51302) Kotlin/Native 1.6.20-M1 compiler fails because of assertion in NativeAnnotationImplementationTransformer
- [`KT-51148`](https://youtrack.jetbrains.com/issue/KT-51148) "AssertionError: At this stage there should be no remaining variables with proper constraints" caused by two type parameters
- [`KT-50970`](https://youtrack.jetbrains.com/issue/KT-50970) Kotlin/Native: use arm instruction set instead of thumb-2 for iosArm32 and watchosArm32 targets
- [`KT-50843`](https://youtrack.jetbrains.com/issue/KT-50843) Kotlin/Native: LLVM constant merge pass does not work for Kotlin constants
- [`KT-51157`](https://youtrack.jetbrains.com/issue/KT-51157) JVM / IR: "IndexOutOfBoundsException: Index: 1, Size: 1" caused by interface hierarchy and UInt method parameter
- [`KT-50498`](https://youtrack.jetbrains.com/issue/KT-50498) Exception after analysing an erroneous lambda
- [`KT-50258`](https://youtrack.jetbrains.com/issue/KT-50258) `equals()` returns `false` on the same enum instances if we check it for the second time in `when`
- [`KT-51062`](https://youtrack.jetbrains.com/issue/KT-51062) Progressions resolve changing warning isn't reported for Java methods
- [`KT-48544`](https://youtrack.jetbrains.com/issue/KT-48544) JVM / IR: "UnsupportedOperationException: Unknown structure of ADAPTER_FOR_CALLABLE_REFERENCE" with callable reference `::arrayOf`
- [`KT-50978`](https://youtrack.jetbrains.com/issue/KT-50978) [Native] Error while building static cache: NoSuchElementException at IrTypeInlineClassesSupport.getInlinedClassUnderlyingType(InlineClasses.kt:341)
- [`KT-50977`](https://youtrack.jetbrains.com/issue/KT-50977) [Native] Error while building static cache: IllegalStateException: Class CLASS ENUM_ENTRY is not found at KonanIrlinkerKt.findClass(KonanIrlinker.kt:229)
- [`KT-50976`](https://youtrack.jetbrains.com/issue/KT-50976) [Native] Error while building static cache: IllegalStateException: No descriptor found at DescriptorByIdSignatureFinder.findDescriptorForPublicSignature(DescriptorByIdSignatureFinder.kt:157)
- [`KT-51040`](https://youtrack.jetbrains.com/issue/KT-51040) Type inference fails on 1.6: "Cannot use 'CapturedType(*)' as reified type parameter" with EnumSet and elvis operator
- [`KT-51080`](https://youtrack.jetbrains.com/issue/KT-51080) Line number in mapping for the first instruction is lost
- [`KT-49526`](https://youtrack.jetbrains.com/issue/KT-49526) JVM IR: Function reference with non-denotable intersection type argument is not inlined and is incorrectly approximated
- [`KT-50399`](https://youtrack.jetbrains.com/issue/KT-50399) Error: unexpected variance in super type argument: out @0
- [`KT-50649`](https://youtrack.jetbrains.com/issue/KT-50649) JVM IR: ClassCastException when returning Result as generic type
- [`KT-50617`](https://youtrack.jetbrains.com/issue/KT-50617) JVM IR: java.lang.IndexOutOfBoundsException "Empty list doesn't contain element at index 0" when class and interface have the same name and extension function is used
- [`KT-50856`](https://youtrack.jetbrains.com/issue/KT-50856) SAM conversion generates invalid bytecode for generics
- [`KT-45693`](https://youtrack.jetbrains.com/issue/KT-45693) False negative INCOMPATIBLE_TYPES with `when` with generic subject
- [`KT-49903`](https://youtrack.jetbrains.com/issue/KT-49903) JVM IR: InlineOnly optimization leads to behavior change for println with mutating System.out
- [`KT-51022`](https://youtrack.jetbrains.com/issue/KT-51022) Fix error messages for resolution ambiguity with stub types
- [`KT-51035`](https://youtrack.jetbrains.com/issue/KT-51035) PSI2IR: "org.jetbrains.kotlin.psi2ir.generators.ErrorExpressionException: null: KtCallExpression:" caused by recursive call of java function
- [`KT-50797`](https://youtrack.jetbrains.com/issue/KT-50797) Implement fix for false negative UPPER_BOUND_VIOLATED with generic typealias using not all type parameters as arguments for underlying type
- [`KT-50878`](https://youtrack.jetbrains.com/issue/KT-50878) Usage of contextual declarations from third-party library is allowed without `-Xcontext-receivers`
- [`KT-49829`](https://youtrack.jetbrains.com/issue/KT-49829) Wrong "cast can never succeed" diagnostic with builder inference
- [`KT-49828`](https://youtrack.jetbrains.com/issue/KT-49828) Improve builder inference diagnostics with overload resolution ambiguity
- [`KT-50989`](https://youtrack.jetbrains.com/issue/KT-50989) CCE cause by EmptySubstitutor in ResolutionWithStubTypesChecker
- [`KT-49729`](https://youtrack.jetbrains.com/issue/KT-49729) Implement deprecation warning for private constructors of sealed classes
- [`KT-49349`](https://youtrack.jetbrains.com/issue/KT-49349) Implement deprecation for invalid if as expression in rhs of elvis
- [`KT-46285`](https://youtrack.jetbrains.com/issue/KT-46285) [SEALED_SUPERTYPE_IN_LOCAL_CLASS] Error message isn't adopted to local objects and sealed interfaces
- [`KT-49002`](https://youtrack.jetbrains.com/issue/KT-49002) Allow OptIn marker on override if base class has the same marker
- [`KT-48899`](https://youtrack.jetbrains.com/issue/KT-48899) Report warnings on overrides with wrong types nullability
- [`KT-49461`](https://youtrack.jetbrains.com/issue/KT-49461) Implement prohibitation of access to members of companion of enum class from initializers of entries of this enum
- [`KT-49754`](https://youtrack.jetbrains.com/issue/KT-49754) Kotlin/JS: @JsExport on enum class reports NON_EXPORTABLE_TYPE warning
- [`KT-49598`](https://youtrack.jetbrains.com/issue/KT-49598) Misleading error message "Using @JvmRecord is only allowed with -jvm-target 15 and -Xjvm-enable-preview flag enabled"
- [`KT-44133`](https://youtrack.jetbrains.com/issue/KT-44133) Inline classes: class literal in annotation arguments uses underlying type
- [`KT-47703`](https://youtrack.jetbrains.com/issue/KT-47703) ClassCastException: Programmatically created annotation can't hold Array<KClass<*>>
- [`KT-47549`](https://youtrack.jetbrains.com/issue/KT-47549) JVM / IR: Null argument in ExpressionCodegen for parameter VALUE_PARAMETER CONTINUATION_CLASS caused by suspend function inside "fun interface" in another file
- [`KT-50120`](https://youtrack.jetbrains.com/issue/KT-50120) HMPP: False positive [NO_VALUE_FOR_PARAMETER] for expect function usages
- [`KT-49864`](https://youtrack.jetbrains.com/issue/KT-49864) JVM IR: NoSuchMethodError calling default interface method with inline class return type in -Xjvm-default=all mode
- [`KT-49812`](https://youtrack.jetbrains.com/issue/KT-49812) JVM / IR: "java.lang.VerifyError: Bad return type" when using Result type attribute + extension function with same name
- [`KT-49936`](https://youtrack.jetbrains.com/issue/KT-49936) Extension property in a data class with the same name as the constructor parameter leads to incorrect component function being resolved and generated
- [`KT-48181`](https://youtrack.jetbrains.com/issue/KT-48181) "ISE: Null argument in ExpressionCodegen for parameter VALUE_PARAMETER" on creating instance of kotlin.Metadata
- [`KT-50215`](https://youtrack.jetbrains.com/issue/KT-50215) VerifyError caused by missing cast after is check in when
- [`KT-49977`](https://youtrack.jetbrains.com/issue/KT-49977) "Parameter specified as non-null is null" when inline class implements interface method with default parameters
- [`KT-50385`](https://youtrack.jetbrains.com/issue/KT-50385) DUPLICATE_LABEL_IN_WHEN is reported on incorrect branches
- [`KT-49092`](https://youtrack.jetbrains.com/issue/KT-49092) JVM: ArrayIndexOutOfBoundsException on compiling call with `if` expression and TODO() arguments
- [`KT-48987`](https://youtrack.jetbrains.com/issue/KT-48987) JVM / IR: Smartcast, which never succeed, crashes the compiler
- [`KT-50277`](https://youtrack.jetbrains.com/issue/KT-50277) Invalid bytecode generated for inline lambda in suspend function
- [`KT-50219`](https://youtrack.jetbrains.com/issue/KT-50219) FIR DFA/CFA: no smart cast after null check and assignment
- [`KT-44561`](https://youtrack.jetbrains.com/issue/KT-44561) FIR DFA: extract non-null info from comparison against variable with initial constant value
- [`KT-44560`](https://youtrack.jetbrains.com/issue/KT-44560) FIR DFA: propagate non-null info to original variables in not-null assertion or cast expression
- [`KT-50278`](https://youtrack.jetbrains.com/issue/KT-50278) FIR: accidental resolve to inaccessible value parameter
- [`KT-47483`](https://youtrack.jetbrains.com/issue/KT-47483) JVM IR: "NoSuchElementException: Sequence contains no element matching the predicate" on compiling Array instantiation with TODO
- [`KT-50304`](https://youtrack.jetbrains.com/issue/KT-50304) EXC_BAD_ACCESS at IntrinsicsNative.kt starting coroutine on object with suspend fun as supertype
- [`KT-49765`](https://youtrack.jetbrains.com/issue/KT-49765) JVM: ClassCastException when trying to add object to EmptyList
- [`KT-46879`](https://youtrack.jetbrains.com/issue/KT-46879) "AssertionError: Stack should be spilled before suspension call" with Flow and reified type
- [`KT-50172`](https://youtrack.jetbrains.com/issue/KT-50172) "AssertionError: Not a callable reflection type" on local function reference with the same name as local variable
- [`KT-49443`](https://youtrack.jetbrains.com/issue/KT-49443) JVM IR, Script: "IllegalStateException: No mapping for symbol: VALUE_PARAMETER INSTANCE_RECEIVER" with constructor call of class that has a top-level extension function call
- [`KT-50193`](https://youtrack.jetbrains.com/issue/KT-50193) Garbage collection is not working the same way after jvm-ir-backend change
- [`KT-19424`](https://youtrack.jetbrains.com/issue/KT-19424) Compilation exception for script with property delegate calling operator invoke on an object
- [`KT-43995`](https://youtrack.jetbrains.com/issue/KT-43995) Script: "IllegalStateException: No mapping for symbol: VALUE_PARAMETER INSTANCE_RECEIVER" if companion object initializer calls method on list
- [`KT-47000`](https://youtrack.jetbrains.com/issue/KT-47000) Allow graceful migration to -Xjvm-default=all-compatibility by allowing to inherit from interfaces even in the old (-Xjvm-default=disable) mode
- [`KT-50180`](https://youtrack.jetbrains.com/issue/KT-50180) FIR: not enough information to infer type variable for definitely not null type
- [`KT-50163`](https://youtrack.jetbrains.com/issue/KT-50163) FIR: ISE unsupported compile-time value BLOCK on complex annotations
- [`KT-50171`](https://youtrack.jetbrains.com/issue/KT-50171) JVM IR: "UninitializedPropertyAccessException: Parent not initialized: IrVariableImpl" on SAM-converted property setter reference with Double parameter inside lambda
- [`KT-50140`](https://youtrack.jetbrains.com/issue/KT-50140) Internal error on explicit string concatenation of generic type value with 'String' upper bound
- [`KT-49992`](https://youtrack.jetbrains.com/issue/KT-49992) Anonymous object should not have access to private members from supertypes
- [`KT-49973`](https://youtrack.jetbrains.com/issue/KT-49973) Check existing of default error message for all diagnostics
- [`KT-50019`](https://youtrack.jetbrains.com/issue/KT-50019) Property delegated to callable reference: "ISE: Local class should have its name computed in InventNamesForLocalClasses" with -Xno-optimized-callable-references
- [`KT-49645`](https://youtrack.jetbrains.com/issue/KT-49645) JVM / IR: "IllegalStateException: Local class should have its name computed" caused by default suspend function in interface and value class
- [`KT-50028`](https://youtrack.jetbrains.com/issue/KT-50028) Incorrect implicit casts from Unit
- [`KT-49615`](https://youtrack.jetbrains.com/issue/KT-49615) JVM / IR: "Exception during IR lowering" with list of value classes with non-trivial constructor inside suspend lambda
- [`KT-49127`](https://youtrack.jetbrains.com/issue/KT-49127) FIR: smart cast is not performed after comparison
- [`KT-48708`](https://youtrack.jetbrains.com/issue/KT-48708) Incorrect cast from Unit to Int
- [`KT-48376`](https://youtrack.jetbrains.com/issue/KT-48376) FIR: False positive UNITIALIZED_VARIABLE after try/finally with return from try
- [`KT-48113`](https://youtrack.jetbrains.com/issue/KT-48113) FIR: (false) positive EQUALITY_NOT_APPLICABLE for intersection with platform type
- [`KT-48305`](https://youtrack.jetbrains.com/issue/KT-48305) FIR: incorrect raw type cast
- [`KT-48378`](https://youtrack.jetbrains.com/issue/KT-48378) FIR: synthetic accessor lowering should not attempt to modify other files
- [`KT-48634`](https://youtrack.jetbrains.com/issue/KT-48634) FIR: false property-setter-function resolve cycle
- [`KT-48621`](https://youtrack.jetbrains.com/issue/KT-48621) FIR: SyntheticAccessorLowering should not attempt to modify other files for protected JvmField
- [`KT-48381`](https://youtrack.jetbrains.com/issue/KT-48381) Invalid LLVM module: verification failure of createInlineClassInArgumentPosition.kt
- [`KT-48527`](https://youtrack.jetbrains.com/issue/KT-48527) Native: top-level properties in files with @Test functions are initialized eagerly even if lazy initialization is enabled
- [`KT-48559`](https://youtrack.jetbrains.com/issue/KT-48559) IllegalArgumentException: Unexpected super type argument: * @ 0  during IR lowering
- [`KT-48687`](https://youtrack.jetbrains.com/issue/KT-48687) IR dump mismatch after deep copy with symbols in IR text test
- [`KT-44811`](https://youtrack.jetbrains.com/issue/KT-44811) [FIR] Exception in body resolve of new contracts
- [`KT-48363`](https://youtrack.jetbrains.com/issue/KT-48363) FIR behaves differently in case of resolution between classifier and top-level property
- [`KT-48801`](https://youtrack.jetbrains.com/issue/KT-48801) "AssertionError: Stack should be spilled before suspension call" with Flow and crossinline
- [`KT-46389`](https://youtrack.jetbrains.com/issue/KT-46389) JVM / IR: "ClassCastException: class IrGetValueImpl cannot be cast to class IrConst" with inheritance of supertypes member functions with similar signatures
- [`KT-47797`](https://youtrack.jetbrains.com/issue/KT-47797) Regression during migration to 1.6 in compiler
- [`KT-47987`](https://youtrack.jetbrains.com/issue/KT-47987) Can't infer a postponed type variable based on callable reference receiver type
- [`KT-48446`](https://youtrack.jetbrains.com/issue/KT-48446) "IllegalStateException: IrErrorType (getErasedUpperBound)" caused by suspend function reference
- [`KT-48651`](https://youtrack.jetbrains.com/issue/KT-48651) Collect intermediate annotations during type expanding
- [`KT-48754`](https://youtrack.jetbrains.com/issue/KT-48754) JVM IR: <clinit> in EnclosingMethod leads to IncompatibleClassChangeError on Android 5.0
- [`KT-45034`](https://youtrack.jetbrains.com/issue/KT-45034) Use the new type inference for top-level callable references
- [`KT-49001`](https://youtrack.jetbrains.com/issue/KT-49001) OptIn marker should spread from class to its members, taking into account real dispatch receiver type
- [`KT-49038`](https://youtrack.jetbrains.com/issue/KT-49038) Generics are discriminated during callable references resolution (false negative)
- [`KT-48954`](https://youtrack.jetbrains.com/issue/KT-48954) JVM IR: IllegalAccessError when using Java method reference in constructor
- [`KT-48284`](https://youtrack.jetbrains.com/issue/KT-48284) JVM / IR: "IllegalStateException: Function has no body: FUN STATIC_INLINE_CLASS_REPLACEMENT" caused by inline member toString of value class and string interpolation
- [`KT-49053`](https://youtrack.jetbrains.com/issue/KT-49053) JVM / IR: "AssertionError: Unbound symbols not allowed" on inheriting a protected Java method that returns a package private class from a differently-named file
- [`KT-49106`](https://youtrack.jetbrains.com/issue/KT-49106) JVM: infinite recursion with overridden default suspend interface method where override calls super
- [`KT-45345`](https://youtrack.jetbrains.com/issue/KT-45345) FIR DFA: `FirDataFlowAnalyzer` seems to add wrong type constraints for type parameters
- [`KT-44513`](https://youtrack.jetbrains.com/issue/KT-44513) FIR DFA: extract non-null info from x?.y!!
- [`KT-44559`](https://youtrack.jetbrains.com/issue/KT-44559) FIR DFA: propagate non-null info from not-null assertion (!!)
- [`KT-49073`](https://youtrack.jetbrains.com/issue/KT-49073) FIR: REDUNDANT_MODIFIER and DEPRECATED_MODIFIER_PAIR should be warnings
- [`KT-46371`](https://youtrack.jetbrains.com/issue/KT-46371) FIR: Investigate FunctionType -> ExtensionFunctionType coercion
- [`KT-49078`](https://youtrack.jetbrains.com/issue/KT-49078) FIR: false positive TYPE_VARIANCE_CONFLICT
- [`KT-47135`](https://youtrack.jetbrains.com/issue/KT-47135) FIR: local class references does not compile
- [`KT-48600`](https://youtrack.jetbrains.com/issue/KT-48600) NON_TAIL_RECURSIVE_CALL missing for calls inside lambda
- [`KT-48602`](https://youtrack.jetbrains.com/issue/KT-48602) NON_TAIL_RECURSIVE_CALL missing for calls with explicit dispatch receiver to a singleton
- [`KT-48982`](https://youtrack.jetbrains.com/issue/KT-48982) JVM / IR: KotlinNothingValueException caused by function with local object
- [`KT-49087`](https://youtrack.jetbrains.com/issue/KT-49087) FIR: false positive REPEATED_ANNOTATION
- [`KT-48648`](https://youtrack.jetbrains.com/issue/KT-48648) JVM IR: "AssertionError: Should be primitive or nullable primitive type" with @JvmField generic property with Number upper bound
- [`KT-49069`](https://youtrack.jetbrains.com/issue/KT-49069) FIR: False positive INAPPLICABLE_JVM_NAME on getter
- [`KT-49203`](https://youtrack.jetbrains.com/issue/KT-49203) JVM IR: "AssertionError: Unbound symbols not allowed" with lateinit var and `plusAssign` operator convention call
- [`KT-48993`](https://youtrack.jetbrains.com/issue/KT-48993) JVM / IR: "IllegalStateException: Validation failed in file <multi-file facade ...>" using @JvmMultifileClass and one of the symbols in the file exposes a @JvmInline value class
- [`KT-48938`](https://youtrack.jetbrains.com/issue/KT-48938) FIR: Investigate how priorities should work in case SAM-conversion + type parameters
- [`KT-49129`](https://youtrack.jetbrains.com/issue/KT-49129) FIR: false positive of INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS
- [`KT-49076`](https://youtrack.jetbrains.com/issue/KT-49076) FIR: false positive "An annotation argument must be compile-time constant" on array of imports
- [`KT-49222`](https://youtrack.jetbrains.com/issue/KT-49222) FIR: StackOverflow in MethodSignatureMapper
- [`KT-49083`](https://youtrack.jetbrains.com/issue/KT-49083) FIR erroneously requires default value parameters for override calls when imported from object
- [`KT-49135`](https://youtrack.jetbrains.com/issue/KT-49135) FIR: ambiguity between type alias and function
- [`KT-49134`](https://youtrack.jetbrains.com/issue/KT-49134) FIR makes no difference between lambda with empty parameter list and without explicit parameter list
- [`KT-49301`](https://youtrack.jetbrains.com/issue/KT-49301) FIR: Unresolved reference: <init> for object inherited from inner class
- [`KT-49070`](https://youtrack.jetbrains.com/issue/KT-49070) FIR: ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED with type parameter / class conflict
- [`KT-49133`](https://youtrack.jetbrains.com/issue/KT-49133) FIR: protected java.lang.Throwable constructor is not available
- [`KT-49407`](https://youtrack.jetbrains.com/issue/KT-49407) JVM / IR: "java.lang.VerifyError: Bad local variable type" with "Int.mod" inside "Long.mod" and non-trivial argument
- [`KT-44975`](https://youtrack.jetbrains.com/issue/KT-44975) SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC: confusing error message
- [`KT-38698`](https://youtrack.jetbrains.com/issue/KT-38698) MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED error message: interface is called a "class"
- [`KT-45001`](https://youtrack.jetbrains.com/issue/KT-45001) Confusing warning message "This class can only be used with the compiler argument" for @OptIn annotation
- [`KT-49411`](https://youtrack.jetbrains.com/issue/KT-49411) JVM / IR: NullPointerException during IR lowering with @JvmField property in loop range
- [`KT-49260`](https://youtrack.jetbrains.com/issue/KT-49260) FIR: make FirBasedSymbol hierarchy to correspond FirDeclaration hierarchy
- [`KT-49289`](https://youtrack.jetbrains.com/issue/KT-49289) FIR: false positive "return required" for if/else with inlined return in branch
- [`KT-49316`](https://youtrack.jetbrains.com/issue/KT-49316) JVM / IR: "AssertionError: SyntheticAccessorLowering should not attempt to modify other files!" caused by unreachable code which try to modify `val` from the other package
- [`KT-45915`](https://youtrack.jetbrains.com/issue/KT-45915) NoDescriptorForDeclarationException: Descriptor wasn't found for declaration FUN with circular module dependencies in JPS
- [`KT-49288`](https://youtrack.jetbrains.com/issue/KT-49288) FIR2IR: consider generating all fake override calls properly
- [`KT-42784`](https://youtrack.jetbrains.com/issue/KT-42784) FIR2IR: first create fake overrides, then bind overridden to them
- [`KT-48816`](https://youtrack.jetbrains.com/issue/KT-48816) Kotlin/Native Symbol for public platform.darwin/NSObject|null[100] is unbound
- [`KT-49372`](https://youtrack.jetbrains.com/issue/KT-49372) JVM / IR: Incorrect work of the loop optimization for mutable variable
- [`KT-49356`](https://youtrack.jetbrains.com/issue/KT-49356) Native: UnsupportedOperationException: RETURNABLE_BLOCK caused by nested return with boxing and inline
- [`KT-49659`](https://youtrack.jetbrains.com/issue/KT-49659) JVM IR: Missing value class mangling in SAM wrappers for fun interfaces from different modules
- [`KT-47101`](https://youtrack.jetbrains.com/issue/KT-47101) Incorrect scope for supertypes in companion objects
- [`KT-49360`](https://youtrack.jetbrains.com/issue/KT-49360) Invalid LLVM module: "inlinable function call in a function with debug info must have a !dbg location"
- [`KT-48430`](https://youtrack.jetbrains.com/issue/KT-48430) JVM: ClassCastException with inline class as generic argument for type parameter used in a function type
- [`KT-49575`](https://youtrack.jetbrains.com/issue/KT-49575) IllegalArgumentException: Unhandled intrinsic in ExpressionCodegen with circular module dependencies in JPS
- [`KT-47669`](https://youtrack.jetbrains.com/issue/KT-47669) IR inliner doesn't handle inner class functions referring outer this
- [`KT-48668`](https://youtrack.jetbrains.com/issue/KT-48668) JVM IR: "ISE: Value at CLASS must not be null for CLASS" for modules with a dependency cycle
- [`KT-49370`](https://youtrack.jetbrains.com/issue/KT-49370) JVM / IR: "java.lang.VerifyError: Bad local variable type" with "fun Long.mod" and non-trivial argument
- [`KT-46744`](https://youtrack.jetbrains.com/issue/KT-46744) Memory Leaks in Kotlin daemon
- [`KT-48806`](https://youtrack.jetbrains.com/issue/KT-48806) False-negative USED_AS_EXPRESSION for unreachable catch clauses
- [`KT-45972`](https://youtrack.jetbrains.com/issue/KT-45972) FIR: type is incorrectly resolved to private
- [`KT-46968`](https://youtrack.jetbrains.com/issue/KT-46968) Remove FirCompositeScope from type resolve
- [`KT-49072`](https://youtrack.jetbrains.com/issue/KT-49072) FIR: accidental resolve to private-in-file type
- [`KT-34822`](https://youtrack.jetbrains.com/issue/KT-34822) FIR scopes: deal with nested / inner classes and type parameter priority
- [`KT-49702`](https://youtrack.jetbrains.com/issue/KT-49702) Exception from RENDER_WHEN_MISSING_CASES diagnostic on malformed sealed class inheritor
- [`KT-49860`](https://youtrack.jetbrains.com/issue/KT-49860) [FIR] Add smartcast expression to synthetic `subj` access
- [`KT-49836`](https://youtrack.jetbrains.com/issue/KT-49836) Inference fails on lambda and adjacent function expressions with receiver
- [`KT-49832`](https://youtrack.jetbrains.com/issue/KT-49832) Inference fails on lambda for function types with extension parameter
- [`KT-44022`](https://youtrack.jetbrains.com/issue/KT-44022) Excessive diagnostics range for DECLARATION_CANT_BE_INLINED
- [`KT-48690`](https://youtrack.jetbrains.com/issue/KT-48690) VERSION_REQUIREMENT_DEPRECATION_ERROR message: use current compiler version instead of language version
- [`KT-49609`](https://youtrack.jetbrains.com/issue/KT-49609) Incorrect grammar in DATA_CLASS_NOT_PROPERTY_PARAMETER error message
- [`KT-49600`](https://youtrack.jetbrains.com/issue/KT-49600) Misspelled error message for non-constructor properties with backing fields in @JvmRecord class
- [`KT-49339`](https://youtrack.jetbrains.com/issue/KT-49339) Warn about synchronizing on value classes
- [`KT-49950`](https://youtrack.jetbrains.com/issue/KT-49950) Compilation failed: An operation is not implemented: IrBasedTypeParameterDescriptor
- [`KT-43604`](https://youtrack.jetbrains.com/issue/KT-43604) Problem with initialization order
- [`KT-23890`](https://youtrack.jetbrains.com/issue/KT-23890) Default arguments are not transferred from expect generic member functions
- [`KT-48811`](https://youtrack.jetbrains.com/issue/KT-48811) Expect/actual class with default constructor argument values can not be instantiated from a shared source set without passing arguments (with HMPP enabled)
- [`KT-48106`](https://youtrack.jetbrains.com/issue/KT-48106) FIR: incorrect type inference in provideDelegate receiver
- [`KT-48325`](https://youtrack.jetbrains.com/issue/KT-48325) Safe call operator prevents object from being garbage collected before leaving function
- [`KT-50004`](https://youtrack.jetbrains.com/issue/KT-50004) Linking kotlinx.serialization crashes on Native and JS IR backends
- [`KT-49311`](https://youtrack.jetbrains.com/issue/KT-49311) Missing FIR checker for unresolved references in import statement
- [`KT-48104`](https://youtrack.jetbrains.com/issue/KT-48104) FIR does not see NotNull/Nullable annotations on type arguments
- [`KT-46812`](https://youtrack.jetbrains.com/issue/KT-46812) [FIR] Make FIR diagnostics not related to Diagnostics from FE 1.0
- [`KT-37374`](https://youtrack.jetbrains.com/issue/KT-37374) [FIR] Add `CheckInfixModifier` resolution stage
- [`KT-38351`](https://youtrack.jetbrains.com/issue/KT-38351) FIR: Support `CheckOperatorModifier` resolution stage
- [`KT-39614`](https://youtrack.jetbrains.com/issue/KT-39614) [FIR] Fix building CFG for different candidates of plus assign call
- [`KT-40197`](https://youtrack.jetbrains.com/issue/KT-40197) [FIR] Strange Behaviour of Type Arguments
- [`KT-40362`](https://youtrack.jetbrains.com/issue/KT-40362) [FIR] Match type arguments with type parameters of corresponding qualifier
- [`KT-40375`](https://youtrack.jetbrains.com/issue/KT-40375) FIR: No transformation implicit type -> error type for function value parameters
- [`KT-40585`](https://youtrack.jetbrains.com/issue/KT-40585) [FIR] Incorrect type for 1/1.0
- [`KT-42525`](https://youtrack.jetbrains.com/issue/KT-42525) [FIR] Incorrect IR produced for java.lang.Byte.MAX_VALUE
- [`KT-43359`](https://youtrack.jetbrains.com/issue/KT-43359) FIR: Check applicability type for callable reference with unbound receiver
- [`KT-43378`](https://youtrack.jetbrains.com/issue/KT-43378) FIR: Support or prohibit via call checker callable references to member extensions
- [`KT-43289`](https://youtrack.jetbrains.com/issue/KT-43289) FIR: Correctly load irrelevant override for special built-ins
- [`KT-44558`](https://youtrack.jetbrains.com/issue/KT-44558) Annotation arguments const expr support is missing in FIR
- [`KT-45223`](https://youtrack.jetbrains.com/issue/KT-45223) [FIR] Ambiguity between explicit and synthetic `removeAt`
- [`KT-42215`](https://youtrack.jetbrains.com/issue/KT-42215) FIR: callable reference resolution with type constraints at call-sites
- [`KT-45520`](https://youtrack.jetbrains.com/issue/KT-45520) FIR: NONE_APPLICABLE for unsafe call to function with overloads instead of UNSAFE_CALL
- [`KT-46410`](https://youtrack.jetbrains.com/issue/KT-46410) [FIR] Transform of FirAugmentedArraySetCall leaves erroneous nodes in control flow graph
- [`KT-46421`](https://youtrack.jetbrains.com/issue/KT-46421) FIR: Investigate builder-inference cases
- [`KT-43948`](https://youtrack.jetbrains.com/issue/KT-43948) FIR: hidden unresolved callable reference
- [`KT-46558`](https://youtrack.jetbrains.com/issue/KT-46558) FIR DFA: run once contract is not considered when analyzing lambda
- [`KT-47125`](https://youtrack.jetbrains.com/issue/KT-47125) FIR: Do not avoid trivial constraints if they aren't from upper bounds
- [`KT-43691`](https://youtrack.jetbrains.com/issue/KT-43691) FIR: false positive VARIABLE_INITIALIZER_IS_REDUNDANT with usage in try...finally
- [`KT-37311`](https://youtrack.jetbrains.com/issue/KT-37311) [FIR] Support inference of callable references with type variable as expected type
- [`KT-31972`](https://youtrack.jetbrains.com/issue/KT-31972) Error type encountered: org.jetbrains.kotlin.types.ErrorUtils$UninferredParameterTypeConstructor@1f5b38c2 (ErrorType).
- [`KT-48761`](https://youtrack.jetbrains.com/issue/KT-48761) Report NO_TAIL_CALLS_FOUND on 'tailrec' modifier, not on the whole function header
- [`KT-47647`](https://youtrack.jetbrains.com/issue/KT-47647) NI: Function reference to Java static method can't compile if passed directly as KFunction1 parameter
- [`KT-46995`](https://youtrack.jetbrains.com/issue/KT-46995) Fix setters implicit types only resolved on full body resolve
- [`KT-46359`](https://youtrack.jetbrains.com/issue/KT-46359) Kotlin 1.5 lambda is not Java-serializable by default
- [`KT-49282`](https://youtrack.jetbrains.com/issue/KT-49282) FIR: suspend conversion does not work inside suspend lambda
- [`KT-48953`](https://youtrack.jetbrains.com/issue/KT-48953) FIR: implement diagnostic DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER
- [`KT-38813`](https://youtrack.jetbrains.com/issue/KT-38813) FIR: Report INSTANCE_ACCESS_BEFORE_SUPER_CALL diagnostic on receiver usages in secondary constructors headers
- [`KT-49657`](https://youtrack.jetbrains.com/issue/KT-49657) FIR: accidental override with get:JvmName
- [`KT-49655`](https://youtrack.jetbrains.com/issue/KT-49655) FIR: smart cast is not performed after update of var to not-null value in branch

### Docs & Examples

- [`KT-51086`](https://youtrack.jetbrains.com/issue/KT-51086) [Docs] [Build Tools] Allow to configure additional jvm arguments for 'KaptWithoutKotlincTask`
- [`KT-50563`](https://youtrack.jetbrains.com/issue/KT-50563) [Docs] [Build Tools] Deprecate kotlin.experimental.coroutines Gradle DSL option and kotlin.coroutines property
- [`KT-50580`](https://youtrack.jetbrains.com/issue/KT-50580) [Docs] [Kotlin/Native] Support instantiation of annotation classes on Native
- [`KT-50874`](https://youtrack.jetbrains.com/issue/KT-50874) [Docs] [Language] Add ability to specify generic type parameters as not-null
- [`KT-50564`](https://youtrack.jetbrains.com/issue/KT-50564) [Docs] [K/JS] Ensure that @AfterTest is invoked after the @Test function completes for asynchronous tests

### IDE

#### New Features

- [`KTIJ-20169`](https://youtrack.jetbrains.com/issue/KTIJ-20169) Link to What's new in a notification about new Kotlin version

#### Performance Improvements

- [`KTIJ-20568`](https://youtrack.jetbrains.com/issue/KTIJ-20568) Optimize SubpackagesIndexService#hasSubpackages

#### Fixes

- [`KTIJ-13020`](https://youtrack.jetbrains.com/issue/KTIJ-13020) New compiler settings are applied only after the project is reloaded
- [`KTIJ-21154`](https://youtrack.jetbrains.com/issue/KTIJ-21154) StackOverflowError on Companion.extractPotentiallyFixableTypesForExpectedType that causes "Syntax highlighting has been temporarily turned off"
- [`KTIJ-20129`](https://youtrack.jetbrains.com/issue/KTIJ-20129) Load "@NotNull T" types from libraries as definitely non-nullable if any module in project loads such types as definitely non-nullable
- [`KTIJ-9793`](https://youtrack.jetbrains.com/issue/KTIJ-9793) UAST: KotlinAbstractUElement.equals fails for psi-less elements
- [`KTIJ-16203`](https://youtrack.jetbrains.com/issue/KTIJ-16203) UAST: Annotating assignment expression sometimes leads to UnknownKotlinExpression
- [`KTIJ-18720`](https://youtrack.jetbrains.com/issue/KTIJ-18720) UAST: @Deprecated(level=DeprecationLevel.HIDDEN) makes method visibility be dropped
- [`KTIJ-18039`](https://youtrack.jetbrains.com/issue/KTIJ-18039) @Deprecated(level=HIDDEN) elements return false for isDeprecated()
- [`KTIJ-18716`](https://youtrack.jetbrains.com/issue/KTIJ-18716) KotlinUMethodWithFakeLightDelegate.hasAnnotation() doesn't find annotations
- [`KTIJ-20220`](https://youtrack.jetbrains.com/issue/KTIJ-20220) Kotlin plugin crashes very often
- [`KTIJ-20308`](https://youtrack.jetbrains.com/issue/KTIJ-20308) Syntax highlighting is temporary suspended for file ... due to internal error
- [`KTIJ-6085`](https://youtrack.jetbrains.com/issue/KTIJ-6085) Exception `Incorrect CachedValue...` with KtUltraLightMethodForSourceDeclaration
- [`KTIJ-18977`](https://youtrack.jetbrains.com/issue/KTIJ-18977) Do not show warning `Outdated bundled kotlin compiler` if there are no compatible plugin with newer compiler
- [`KTIJ-20253`](https://youtrack.jetbrains.com/issue/KTIJ-20253) Consider supporting special highlighting for definitely non-null types
- [`KT-42194`](https://youtrack.jetbrains.com/issue/KT-42194) OOME: Java heap space from incremental compilation
- [`KTIJ-13019`](https://youtrack.jetbrains.com/issue/KTIJ-13019) "Add '-Xopt-in=kotlin.io.path.ExperimentalPathApi' to module untitled1 compiler arguments" only works until the project is reloaded

### IDE. Completion

- [`KTIJ-20095`](https://youtrack.jetbrains.com/issue/KTIJ-20095) Optimize FilterOutKotlinSourceFilesScope#contains
- [`KTIJ-16250`](https://youtrack.jetbrains.com/issue/KTIJ-16250) Completion of override with return type annotated with TYPE_USE-targeted annotation suggests two duplicate entries

### IDE. Debugger

- [`KTIJ-20716`](https://youtrack.jetbrains.com/issue/KTIJ-20716) JVM Debugger in common code can't get JVM-specific view on common code and fails
- [`KTIJ-18562`](https://youtrack.jetbrains.com/issue/KTIJ-18562) JVM debugger: coroutineContext.job causes "Failed to generate expression: KtNameReferenceExpression"
- [`KTIJ-20019`](https://youtrack.jetbrains.com/issue/KTIJ-20019) MPP Debugger: NSFE “Field not found” on accessing property with explicit getter from common code in Evaluate expression/Watcher
- [`KTIJ-19990`](https://youtrack.jetbrains.com/issue/KTIJ-19990) MPP Debugger: Evaluate expression for some stdlib in common source set fails with Method threw 'java.lang.ClassNotFoundException' exception.
- [`KTIJ-20929`](https://youtrack.jetbrains.com/issue/KTIJ-20929) MPP Debugger: in a project with single JVM target evaluation of expect function fails with 'NoSuchMethodError' exception in common context
- [`KTIJ-20956`](https://youtrack.jetbrains.com/issue/KTIJ-20956) Debugger: coroutine debugger fails to load sometimes
- [`KTIJ-20775`](https://youtrack.jetbrains.com/issue/KTIJ-20775) MPP Debugger: Evaluate expression for actual typealiases in jvm source set fails with Method threw 'java.lang.ClassNotFoundException' exception.
- [`KTIJ-20712`](https://youtrack.jetbrains.com/issue/KTIJ-20712) MPP Debugger: evaluator fails when evaluating expect function
- [`KTIJ-19344`](https://youtrack.jetbrains.com/issue/KTIJ-19344) K/N debugger shows all types as ObjHeader in variable view.

### IDE. Gradle Integration

- [`KTIJ-20097`](https://youtrack.jetbrains.com/issue/KTIJ-20097) HMPP+Android Project that depends on pure Android Lib fails to import in IDEA
- [`KTIJ-20756`](https://youtrack.jetbrains.com/issue/KTIJ-20756) MPP targeting Android and JVM reports 'The feature "multi platform projects" is experimental and should be enabled explicitly'
- [`KTIJ-20745`](https://youtrack.jetbrains.com/issue/KTIJ-20745) Gradle: NSEE “Key main is missing in the map.” on project import in AS 212 + Kotlin 1.6.20
- [`KT-47570`](https://youtrack.jetbrains.com/issue/KT-47570) MPP, IDE: kotlin-test-common leaks into dependencies of platform-specific source sets
- [`KTIJ-19541`](https://youtrack.jetbrains.com/issue/KTIJ-19541) IDE: Kotlin Facets aren't created for Gradle projects added via `includeBuild`
- [`KT-48882`](https://youtrack.jetbrains.com/issue/KT-48882) MPP IDE import: Failing cinterop Gradle tasks shall not fail import
- [`KTIJ-18135`](https://youtrack.jetbrains.com/issue/KTIJ-18135) MPP, IDE: False positive "No value passed for parameter" in CommonTest when expect declaration has default value and actual does not

### IDE. Inspections and Intentions

#### New Features

- [`KTIJ-12437`](https://youtrack.jetbrains.com/issue/KTIJ-12437) Add inspection to detect redundant (obsolete, unused) @OptIn annotations
- [`KTIJ-15780`](https://youtrack.jetbrains.com/issue/KTIJ-15780) Add quickfix for migration of Experimental -> RequiresOptIn
- [`KTIJ-18865`](https://youtrack.jetbrains.com/issue/KTIJ-18865) Provide quick fixes for OptIn markers on forbidden targets
- [`KTIJ-18439`](https://youtrack.jetbrains.com/issue/KTIJ-18439) Make suggestions for applying opt-in quickfixes more distinct
- [`KTIJ-19985`](https://youtrack.jetbrains.com/issue/KTIJ-19985) Provide quickfix for SAFE_CALL_WILL_CHANGE_NULLABILITY diagnostic

#### Fixes

- [`KTIJ-20550`](https://youtrack.jetbrains.com/issue/KTIJ-20550) False positives in "Unnecessary '@OptIn' annotation" inspection
- [`KTIJ-20993`](https://youtrack.jetbrains.com/issue/KTIJ-20993) Quick-fix for NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS leads to unreachable code
- [`KTIJ-20557`](https://youtrack.jetbrains.com/issue/KTIJ-20557) Quick fix to add `Any` as an upper bound for type parameter to make it non-nullable
- [`KTIJ-20827`](https://youtrack.jetbrains.com/issue/KTIJ-20827) Process type mismatch compiler warnings to provide a corresponding quick fix
- [`KTIJ-12578`](https://youtrack.jetbrains.com/issue/KTIJ-12578) "Make abstract" quick fix for missing abstract members implementations could warn of potentially broken inheritance
- [`KTIJ-20425`](https://youtrack.jetbrains.com/issue/KTIJ-20425) Quick fix to replace `@NotNull` parameter type `T` with a definitely non-nullable type `T & Any`
- [`KTIJ-19997`](https://youtrack.jetbrains.com/issue/KTIJ-19997) Inspection "Possibly blocking call in non-blocking context could lead to thread starvation" suggests "Wrap call in 'withContext'" resulting in red code
- [`KTIJ-18291`](https://youtrack.jetbrains.com/issue/KTIJ-18291) Quickfix "Add @OptIn() annotation" adds the annotation to primary constructor when invoked on primary constructor parameter
- [`KTIJ-19512`](https://youtrack.jetbrains.com/issue/KTIJ-19512) Implement IDE support for new rules of deprecation inheritance
- [`KTIJ-20156`](https://youtrack.jetbrains.com/issue/KTIJ-20156) Exception when applying 'Convert to with'
- [`KTIJ-20290`](https://youtrack.jetbrains.com/issue/KTIJ-20290) Forbid "move to constructor" intention if class contains secondary constructor
- [`KTIJ-20288`](https://youtrack.jetbrains.com/issue/KTIJ-20288) Forbid "move to constructor" intention for actual classes with actual constructor
- [`KT-49736`](https://youtrack.jetbrains.com/issue/KT-49736) Introduce import alias fails when qualifier is unresolved
- [`KTIJ-18743`](https://youtrack.jetbrains.com/issue/KTIJ-18743) "Redundant nullable return type" applied on `actual` method doesn't change the signature of `expect`
- [`KTIJ-12343`](https://youtrack.jetbrains.com/issue/KTIJ-12343) Inspection "Sealed sub-class has no state and no overridden equals" is applied incorrectly to expect/actual declarations of sealed classes with nested subclasses
- [`KTIJ-19406`](https://youtrack.jetbrains.com/issue/KTIJ-19406) The "Add @OptIn(...)" quick fix does not shorten the annotation when adding an argument to the existing annotation
- [`KTIJ-12351`](https://youtrack.jetbrains.com/issue/KTIJ-12351) `generate equals & hashCode() by identity` intention generates incompilable code for multiplatform project modules
- [`KTIJ-13227`](https://youtrack.jetbrains.com/issue/KTIJ-13227) Forbid "move property to constructor" for actual classes with actual constructor having at least one parameter already
- [`KTIJ-11328`](https://youtrack.jetbrains.com/issue/KTIJ-11328) No quick fixes are suggested for annotation from experimental API used with file target
- [`KTIJ-14427`](https://youtrack.jetbrains.com/issue/KTIJ-14427) Meta-annotation value isn't updated for all the corresponding parts of a multiplatform annotation
- [`KTIJ-19735`](https://youtrack.jetbrains.com/issue/KTIJ-19735) NSME org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix on IJ212 + Kotlin master

### IDE. Navigation

- [`KTIJ-18145`](https://youtrack.jetbrains.com/issue/KTIJ-18145) IDE freeze on 'Show Usage'

### IDE. Refactorings. Extract Function

- [`KTIJ-10026`](https://youtrack.jetbrains.com/issue/KTIJ-10026) Refactor / Extract function: Adds unnecessary nullability annotations for parameters coming from `NotNull` or `Nullable` Java methods
- [`KTIJ-15823`](https://youtrack.jetbrains.com/issue/KTIJ-15823) Refactor / Extract Function: resulted declaration gets no @OptIn from original function when necessary

### IDE. Refactorings. Move

- [`KTIJ-5661`](https://youtrack.jetbrains.com/issue/KTIJ-5661) [Tests] MoveRefactoring: Fix ignored incorrect tests of crossmodule declaration moving

### IDE. Wizards

- [`KTIJ-19232`](https://youtrack.jetbrains.com/issue/KTIJ-19232) New project wizard: delete MPP Mobile-Library and Application templates
- [`KTIJ-20878`](https://youtrack.jetbrains.com/issue/KTIJ-20878) Unable to create project with Kotlin Project Wizard when JDK 17 is used.
- [`KTIJ-20244`](https://youtrack.jetbrains.com/issue/KTIJ-20244) Compose MPP project from wizard requires minCompileSdk=31
- [`KTIJ-20781`](https://youtrack.jetbrains.com/issue/KTIJ-20781) Update kotlin-wrappers version in wizard

### JS. Tools

- [`KT-47387`](https://youtrack.jetbrains.com/issue/KT-47387) KJS: Support Apple Silicon for node distrib download

### JavaScript

#### New Features

- [`KT-44494`](https://youtrack.jetbrains.com/issue/KT-44494) KJS / IR: Allow enum classes to be exported
- [`KT-43224`](https://youtrack.jetbrains.com/issue/KT-43224) KJS: Allow using inline classes in external types
- [`KT-35100`](https://youtrack.jetbrains.com/issue/KT-35100) Make Char inline class in K/JS
- [`KT-42936`](https://youtrack.jetbrains.com/issue/KT-42936) KJS IR: Support js-code test directives similar to legacy
- [`KT-47525`](https://youtrack.jetbrains.com/issue/KT-47525) KJS / IR: Support protected members in d.ts generation
- [`KT-50110`](https://youtrack.jetbrains.com/issue/KT-50110) KJS / IR: Enable properties lazy initialization by default

#### Performance Improvements

- [`KT-46443`](https://youtrack.jetbrains.com/issue/KT-46443) KJS / IR: Improve `CharArray` and `Char` performance
- [`KT-45665`](https://youtrack.jetbrains.com/issue/KT-45665) KJS / IR: `equals` on inline value classes is boxed
- [`KT-43644`](https://youtrack.jetbrains.com/issue/KT-43644) KJS / IR: Avoid creating lambda classes

#### Fixes

- [`KT-51685`](https://youtrack.jetbrains.com/issue/KT-51685) KJS / IR: TypeError: collection.iterator_jk1svi_k$ regression in Kotlin 1.6.20-RC2
- [`KT-51523`](https://youtrack.jetbrains.com/issue/KT-51523) KJS IR: "Uncaught TypeError: a._get_length__2347802853_w7ahp7_k$ is not a function"
- [`KT-51700`](https://youtrack.jetbrains.com/issue/KT-51700) KJS / IR: Compiler uses wrong function with List<List>
- [`KT-51222`](https://youtrack.jetbrains.com/issue/KT-51222) KJS / IR: "RangeError: Maximum call stack size exceeded": Default function overloads marked with @JsExport are broken
- [`KT-45054`](https://youtrack.jetbrains.com/issue/KT-45054) KJS: Export secondary constructors as class static methods
- [`KT-37916`](https://youtrack.jetbrains.com/issue/KT-37916) KJS: .d.ts generation not working for enum classes
- [`KT-48199`](https://youtrack.jetbrains.com/issue/KT-48199) KJS / IR: Improve error message for linkage problems
- [`KT-51030`](https://youtrack.jetbrains.com/issue/KT-51030) KJS / IR: internal class that implements public interface is missing getters
- [`KT-45434`](https://youtrack.jetbrains.com/issue/KT-45434) KJS: "WRONG_EXPORTED_DECLARATION" when using `JsExport` on interfaces
- [`KT-50934`](https://youtrack.jetbrains.com/issue/KT-50934) KJS / IR: Re-export all JS-exports from the main module for the multi-module mode
- [`KT-45620`](https://youtrack.jetbrains.com/issue/KT-45620) KJS / IR: Remainder of division of `Int.MIN_VALUE` by -1 is negative zero (-0)
- [`KT-44981`](https://youtrack.jetbrains.com/issue/KT-44981) KJS / IR crashes on `kotlin.js.js` calls with complex constexpr
- [`KT-41964`](https://youtrack.jetbrains.com/issue/KT-41964) KJS IR: Reference to local variable and parameter from js fun could be broken unexpectedly
- [`KT-50682`](https://youtrack.jetbrains.com/issue/KT-50682) Kotlin/JS: IR + IC: TypeError "Cannot read properties of undefined" when properties from different sources refer one another in initializers
- [`KT-50175`](https://youtrack.jetbrains.com/issue/KT-50175) Kotlin/JS, IR: with incremental compilation top level properties initialization is not lazy
- [`KT-40236`](https://youtrack.jetbrains.com/issue/KT-40236) KJS: IR. Invalid override for external field with @JsName
- [`KT-46525`](https://youtrack.jetbrains.com/issue/KT-46525) KJS / IR: Generate context-dependent names for lambdas and object expressions
- [`KT-49779`](https://youtrack.jetbrains.com/issue/KT-49779) KJS / IR: Exported abstract class implementing interface not export interfaces member in d.ts
- [`KT-49773`](https://youtrack.jetbrains.com/issue/KT-49773) KJS / IR: Exported enum implementing interface
- [`KT-41912`](https://youtrack.jetbrains.com/issue/KT-41912) KJS / IR: generates invalid defineProperty methods for class hierarchies
- [`KT-46225`](https://youtrack.jetbrains.com/issue/KT-46225) KJS IR: tailrec function with capturing lambda in default parameter value leads to UninitializedPropertyAccessException at compile time
- [`KT-50528`](https://youtrack.jetbrains.com/issue/KT-50528) Kotlin/JS: IR + IC: TypeError: "combined.get_icpjjy_k$ is not a function" for code calling `GlobalScope.launch {}`
- [`KT-50512`](https://youtrack.jetbrains.com/issue/KT-50512) KJS / IR: IC failed with const val in inline fun
- [`KT-49738`](https://youtrack.jetbrains.com/issue/KT-49738) Ensure that @AfterTest is invoked after the @Test function completes for asynchronous tests
- [`KT-45542`](https://youtrack.jetbrains.com/issue/KT-45542) KJS / IR: "IllegalStateException" for method with default argument in expect class
- [`KT-50464`](https://youtrack.jetbrains.com/issue/KT-50464) KJS IR: Functions with optional parameters and stable names are exported without @JsExport
- [`KT-43374`](https://youtrack.jetbrains.com/issue/KT-43374) KJS / IR: "class org.jetbrains.kotlin.js.backend.ast.JsIf cannot be cast to class org.jetbrains.kotlin.js.backend.ast.JsExpressionStatement" caused by `if-else` expression inside `when`
- [`KT-20791`](https://youtrack.jetbrains.com/issue/KT-20791) ushr Behavior differs in Javascript and Java targets
- [`KT-49844`](https://youtrack.jetbrains.com/issue/KT-49844) KJS / IR: "IllegalStateException: IrSimpleFunctionSymbolImpl is already bound" with callable reference
- [`KT-46961`](https://youtrack.jetbrains.com/issue/KT-46961) KJS / IR: "IllegalStateException: Can't find name for declaration" when external object contains enum class
- [`KT-43191`](https://youtrack.jetbrains.com/issue/KT-43191) KJS / IR: static companion field is not static in d.ts
- [`KT-39891`](https://youtrack.jetbrains.com/issue/KT-39891) [KJS / IR] String interpolation and byte shift round Long value
- [`KT-50040`](https://youtrack.jetbrains.com/issue/KT-50040) JS IR: "Function must be an accessor of corresponding property" exception on private properties with getter in an exported class
- [`KT-49878`](https://youtrack.jetbrains.com/issue/KT-49878) Kotlin/JS, IR: incremental compilation fails with CCE: "class StageController cannot be cast to class WholeWorldStageController"
- [`KT-46202`](https://youtrack.jetbrains.com/issue/KT-46202) KJS / IR: "IllegalStateException" on exported value class
- [`KT-42039`](https://youtrack.jetbrains.com/issue/KT-42039) KJS / IR: JsQualifier annotation can cause conflicts with built-in functions
- [`KT-39364`](https://youtrack.jetbrains.com/issue/KT-39364) KJS: Can't export sealed class/object with subclasses inside the body to typescript definition
- [`KT-47360`](https://youtrack.jetbrains.com/issue/KT-47360) KJS / IR: `js()` function with string val
- [`KT-47376`](https://youtrack.jetbrains.com/issue/KT-47376) KJS / IR: Incorrect `d.ts` generation for sealed classes
- [`KT-47767`](https://youtrack.jetbrains.com/issue/KT-47767) KJS / IR: "IllegalStateException" with inline function with several lambda invocation declared through typealias
- [`KT-47342`](https://youtrack.jetbrains.com/issue/KT-47342) KJS / IR: "StackOverflowError" on `InlinerTypeRemapper.remapTypeArguments`
- [`KT-46218`](https://youtrack.jetbrains.com/issue/KT-46218) KJS / IR: Incorrect result for local `tailrec` function called from capturing inline lambda
- [`KT-45056`](https://youtrack.jetbrains.com/issue/KT-45056) KJS / IR: Inconsistent `ordinal` property value for enum classes
- [`KT-47096`](https://youtrack.jetbrains.com/issue/KT-47096) KJS / IR: `Console.log` introduces spaces between each character
- [`KT-47516`](https://youtrack.jetbrains.com/issue/KT-47516) KJS / IR: Wrong behavior when trying to access property in enum entry
- [`KT-47806`](https://youtrack.jetbrains.com/issue/KT-47806) KJS / IR: "IllegalStateException" with invocation of extension function with generic and lambda parameter on `String.Companion`
- [`KT-49225`](https://youtrack.jetbrains.com/issue/KT-49225) KJS: Default value for value class parameter is not considered
- [`KT-49326`](https://youtrack.jetbrains.com/issue/KT-49326) KJS / IR: Overridden properties should not be exported
- [`KT-49363`](https://youtrack.jetbrains.com/issue/KT-49363) KJS / IR: Nested declarations inside exported object are not exported
- [`KT-49300`](https://youtrack.jetbrains.com/issue/KT-49300) KJS: Source map generator leaks source files descriptors when source embedding is enabled
- [`KT-40525`](https://youtrack.jetbrains.com/issue/KT-40525) KJS IR: investigate issues with using kotlin-wrappers
- [`KT-50197`](https://youtrack.jetbrains.com/issue/KT-50197) KJS IR: using APIs in different packages but with same method name generates clashes
- [`KT-45958`](https://youtrack.jetbrains.com/issue/KT-45958) KJS: Line breaks are missing in `js` files after If-expression
- [`KT-40213`](https://youtrack.jetbrains.com/issue/KT-40213) KJS: fix and "unmute" cases from .../nonReifiedTypeParameters/ "muted" while fixing KT-38771
- [`KT-50152`](https://youtrack.jetbrains.com/issue/KT-50152) Kotlin/JS: with incremental compilation browserRun / nodeRun produce no output
- [`KT-50315`](https://youtrack.jetbrains.com/issue/KT-50315) Kotlin/JS: building project with some dependencies fails: IAE: "Duplicate definition"
- [`KT-50203`](https://youtrack.jetbrains.com/issue/KT-50203) JS IR BE: new IC for lowerings
- [`KT-43250`](https://youtrack.jetbrains.com/issue/KT-43250) KJS / IR: "Can't find name for declaration FUN" with Json#encodeToString as function reference
- [`KT-46992`](https://youtrack.jetbrains.com/issue/KT-46992) KJS / IR: Incorrect boxing of inline classes returned by crossinline suspend lambdas
- [`KT-49776`](https://youtrack.jetbrains.com/issue/KT-49776) KJS / IR: inliner doesn't handle inner class functions referring outer this
- [`KT-49849`](https://youtrack.jetbrains.com/issue/KT-49849) KJS / IR: Access of exported member properties should be by its stable name
- [`KT-49850`](https://youtrack.jetbrains.com/issue/KT-49850) KJS / IR: Member properties in exported class should be getter/setter in d.ts

### Language Design

- [`KT-19423`](https://youtrack.jetbrains.com/issue/KT-19423) Script: reference from class to script top-level member is incompilable
- [`KT-21197`](https://youtrack.jetbrains.com/issue/KT-21197) Support cross platform common implementations
- [`KT-42435`](https://youtrack.jetbrains.com/issue/KT-42435) Prototype multiple receivers
- [`KT-48385`](https://youtrack.jetbrains.com/issue/KT-48385) Deprecate confusing grammar in when-with-subject
- [`KT-49542`](https://youtrack.jetbrains.com/issue/KT-49542) FIR: Smart cast may lead to inconsistent inference result
- [`KT-23727`](https://youtrack.jetbrains.com/issue/KT-23727) Internal declarations from other module in star imports should have lower priority than public declarations from default imports
- [`KT-50251`](https://youtrack.jetbrains.com/issue/KT-50251) Support language version 1.3 in Kotlin 1.6.20
- [`KT-17765`](https://youtrack.jetbrains.com/issue/KT-17765) Ambiguity between SAM adapters when one SAM interface is sub type of another
- [`KT-10926`](https://youtrack.jetbrains.com/issue/KT-10926) False overload resolution ambiguity when both vararg and collection match
- [`KT-41214`](https://youtrack.jetbrains.com/issue/KT-41214) JDK 17: Emit PermittedSubclasses attribute when compiling sealed classes

### Libraries

- [`KT-44089`](https://youtrack.jetbrains.com/issue/KT-44089) Java version checking doesn't work on Android
- [`KT-50033`](https://youtrack.jetbrains.com/issue/KT-50033) Some packages of kotlin-stdlib with public API are not exported in module-info
- [`KT-48367`](https://youtrack.jetbrains.com/issue/KT-48367) Using `synchronized` on captured object leads to slow JVM execution
- [`KT-49721`](https://youtrack.jetbrains.com/issue/KT-49721) KJS: Regex("\\b").findAll yields infinite sequence when a zero length match is found before a surrogate pair (e.g. emoji)

### Native

#### New Features

- [`KT-49463`](https://youtrack.jetbrains.com/issue/KT-49463) --dry-run flag for llvm_builder/package.py

#### Fixes

- [`KT-51359`](https://youtrack.jetbrains.com/issue/KT-51359) Native: the compiler doesn't work on macOS 12.3 Beta
- [`KT-49144`](https://youtrack.jetbrains.com/issue/KT-49144) Kotlin/Native executable early segmentation fault
- [`KT-49348`](https://youtrack.jetbrains.com/issue/KT-49348) KONAN_NO_64BIT_ATOMIC does not guard Kotlin_AtomicLong_addAndGet
- [`KT-42500`](https://youtrack.jetbrains.com/issue/KT-42500) KLIB: K/N compiler cannot link with a library with incorrect symbol in the name or the path
- [`KT-49395`](https://youtrack.jetbrains.com/issue/KT-49395) K/N: After 1.5.20, compiling code including `KSuspendFunction3` to framework leads to "Assertion failed at parametersAssociated"
- [`KT-49967`](https://youtrack.jetbrains.com/issue/KT-49967) Kotlin iOS regex issue
- [`KT-49873`](https://youtrack.jetbrains.com/issue/KT-49873) Native does not sort Strings correctly
- [`KT-49347`](https://youtrack.jetbrains.com/issue/KT-49347) androidNativeArm32 binaries crash when using atomic operations
- [`KT-49597`](https://youtrack.jetbrains.com/issue/KT-49597) Kotlin/Native: Exporting the Arrow library into the framework causes StackOverflowError in the Devirtualization phase
- [`KT-49790`](https://youtrack.jetbrains.com/issue/KT-49790) "Undefined symbols" error when linking project with kotest 5.0.0.RC and Kotlin 1.6

### Native. Build Infrastructure

- [`KT-48625`](https://youtrack.jetbrains.com/issue/KT-48625) Native: distribution doesn't contain sources for kotlin.test

### Native. C Export

- [`KT-47828`](https://youtrack.jetbrains.com/issue/KT-47828) Kotlin/Native: Kotlin exception is not filtered out on interop border when producing a dynamic library with compiler caches enabled

### Native. C and ObjC Import

- [`KT-35059`](https://youtrack.jetbrains.com/issue/KT-35059) Better "could not build module" cinterop report
- [`KT-49768`](https://youtrack.jetbrains.com/issue/KT-49768) Kotlin/Native: Add -Xoverride-konan-properties to cinterop.

### Native. ObjC Export

- [`KT-47399`](https://youtrack.jetbrains.com/issue/KT-47399) Kotlin Native - Objective-C with Swift 5.5 Async Function Needs Returning KotlinUnit
- [`KT-48282`](https://youtrack.jetbrains.com/issue/KT-48282) Kotlin sealed class roots and abstract classes within sealed hierarchies have their constructors exposed to Objective-C/Swift
- [`KT-46866`](https://youtrack.jetbrains.com/issue/KT-46866) Memory consumption / performance of Kotlin classes with String property in KMP project on iOS Swift

### Native. Platform Libraries

- [`KT-50045`](https://youtrack.jetbrains.com/issue/KT-50045) Kotlin/Native: Re-enable disabled Hypervisor framework
- [`KT-47331`](https://youtrack.jetbrains.com/issue/KT-47331) Kotlin/Native: support Xcode 13 SDKs

### Native. Platforms

- [`KT-48078`](https://youtrack.jetbrains.com/issue/KT-48078) Native: Support non-NativeActivity Android executables

### Native. Runtime

- [`KT-48424`](https://youtrack.jetbrains.com/issue/KT-48424) Support resolving source locations using libbacktrace
- [`KT-51586`](https://youtrack.jetbrains.com/issue/KT-51586) SIGSEGV on worker7 test
- [`KT-50491`](https://youtrack.jetbrains.com/issue/KT-50491) Kotlin/Native: Deadlock in the Ktor server tests with the new memory manager

### Native. Runtime. Memory

- [`KT-50879`](https://youtrack.jetbrains.com/issue/KT-50879) Kotlin/Native: Stabilize Concurrent Sweep GC
- [`KT-50948`](https://youtrack.jetbrains.com/issue/KT-50948) Kotlin/Native: Concurrent Sweep GC hangs on Windows in GCStateHolder::waitEpochFinished/waitScheduled
- [`KT-49497`](https://youtrack.jetbrains.com/issue/KT-49497) iOS Swift "runtime assert: Must be positive" and "runtime assert: cycle collector shall only work with single object containers"
- [`KT-50026`](https://youtrack.jetbrains.com/issue/KT-50026) Kotlin/Native: Make AtomicReference behave like FreezableAtomicReference with the new MM
- [`KT-49013`](https://youtrack.jetbrains.com/issue/KT-49013) Kotlin/Native: Correctly switch thread state to native for spin locks

### Native. Testing

- [`KT-48561`](https://youtrack.jetbrains.com/issue/KT-48561) Test sideEffectInTopLevelInitializerMultiModule fails with new MM

### Reflection

- [`KT-50198`](https://youtrack.jetbrains.com/issue/KT-50198) Reflection: NPE from `kotlin.jvm.internal.Intrinsics.areEqual` with Spock

### Tools. Android Extensions

- [`KT-50784`](https://youtrack.jetbrains.com/issue/KT-50784) kotlin-android-extensions produces unbound symbol under `_$_findViewCache`
- [`KT-50627`](https://youtrack.jetbrains.com/issue/KT-50627) NullPointerException when using kotlin-android-extensions synthetic after updating Kotlin to 1.6.10
- [`KT-50887`](https://youtrack.jetbrains.com/issue/KT-50887) kotlin-android-extensions plugin breaks when `package` attribute missing from AndroidManifest.xml

### Tools. CLI

- [`KT-51309`](https://youtrack.jetbrains.com/issue/KT-51309) Add JVM target bytecode version 18
- [`KT-48027`](https://youtrack.jetbrains.com/issue/KT-48027) "Module ... cannot be found in the module graph" with module-info in META-INF/versions
- [`KT-50695`](https://youtrack.jetbrains.com/issue/KT-50695) Compiling into IR backends with language version 1.3 is not rejected
- [`KT-46329`](https://youtrack.jetbrains.com/issue/KT-46329) Deprecated `-Xjvm-default` values are not reported
- [`KT-51025`](https://youtrack.jetbrains.com/issue/KT-51025) JVM CLI compiler takes class file from classpath instead of input java source file
- [`KT-50889`](https://youtrack.jetbrains.com/issue/KT-50889) AnalysisHandlerExtension multiple round execution is broken on Kotlin/MultiPlatform with `expectActualLinker=true` flag
- [`KT-48417`](https://youtrack.jetbrains.com/issue/KT-48417) CLI: boolean -X arguments accept a value after '=' which is ignored
- [`KT-11164`](https://youtrack.jetbrains.com/issue/KT-11164) Allow running class files with '.class' extension in 'kotlin' script
- [`KT-46171`](https://youtrack.jetbrains.com/issue/KT-46171) NoClassDefFoundError produced when running kotlin script

### Tools. Commonizer

- [`KT-48568`](https://youtrack.jetbrains.com/issue/KT-48568) [Commonizer] timespec properties are not commonized in kotlinx.coroutines
- [`KT-46257`](https://youtrack.jetbrains.com/issue/KT-46257) MPP: Stdlib included more than once for an enabled hierarchical commonization
- [`KT-49735`](https://youtrack.jetbrains.com/issue/KT-49735) [Commonizer] :commonizeNativeDistribution  fails for projects with two or more same native targets
- [`KT-48856`](https://youtrack.jetbrains.com/issue/KT-48856) MPP: Unable to resolve c-interop dependency for test compilation in an intermediate source set with the only platform
- [`KT-48288`](https://youtrack.jetbrains.com/issue/KT-48288) [Commonizer] platform.posix.timespec.tv_sec not commonized in OKIO
- [`KT-47574`](https://youtrack.jetbrains.com/issue/KT-47574) [Commonizer] TypeAliasTypeCommonization: Properly substitute underlying type arguments
- [`KT-48221`](https://youtrack.jetbrains.com/issue/KT-48221) MPP: Too few targets specified if platform test source set depends on main
- [`KT-47100`](https://youtrack.jetbrains.com/issue/KT-47100) [Commonizer] Commonize underlying type-alias types

### Tools. Compiler Plugins

- [`KT-50718`](https://youtrack.jetbrains.com/issue/KT-50718) Unable to serialize an object with a generic field
- [`KT-50764`](https://youtrack.jetbrains.com/issue/KT-50764) Kotlin 1.6.10 custom serializers for a generic type receive the unit serializer on jvm
- [`KT-46444`](https://youtrack.jetbrains.com/issue/KT-46444) JVM IR, serialization: "AssertionError: No such value argument slot in IrConstructorCallImpl: 0" with KSerializer of ClosedRange<Float>

### Tools. Daemon

- [`KT-47522`](https://youtrack.jetbrains.com/issue/KT-47522) Provide reasonable resolution strategy for OutOfMemoryError during compilation
- [`KT-51116`](https://youtrack.jetbrains.com/issue/KT-51116) OOM user-friendly message isn't displayed if there is main exception caused by out of memory

### Tools. Gradle

#### New Features

- [`KT-48620`](https://youtrack.jetbrains.com/issue/KT-48620) Add build information into Gradle build scan
- [`KT-41689`](https://youtrack.jetbrains.com/issue/KT-41689) Support statistics for Configuration Cache
- [`KT-49299`](https://youtrack.jetbrains.com/issue/KT-49299) Add more flexible way for defining Kotlin compiler execution strategy
- [`KT-21056`](https://youtrack.jetbrains.com/issue/KT-21056) Kotlin Gradle Plugin tasks execution should be parallelized by default

#### Performance Improvements

- [`KT-50664`](https://youtrack.jetbrains.com/issue/KT-50664) Compile speed regression going from Kotlin 1.5.31 to 1.6.10 for incremental changes when Java class in a dependent child module w/ kapt is modified w/o a method/class signature change
- [`KT-48884`](https://youtrack.jetbrains.com/issue/KT-48884) Configuration performance regression in Kotlin Gradle plugin 1.5.30
- [`KT-49782`](https://youtrack.jetbrains.com/issue/KT-49782) Improve compilation task outputs snapshot performance

#### Fixes

- [`KT-51501`](https://youtrack.jetbrains.com/issue/KT-51501) Gradle: 'java.lang.NoClassDefFoundError: com/gradle/scan/plugin/BuildScanExtension' on 1.6.0-RC when applying Enterprise Plugin from initscript
- [`KT-51588`](https://youtrack.jetbrains.com/issue/KT-51588) Restoring from build cache breaks Kotlin incremental compilation
- [`KT-50620`](https://youtrack.jetbrains.com/issue/KT-50620) Gradle Kotlin Plugin crashes in CI due to hostname resolving issue
- [`KT-49921`](https://youtrack.jetbrains.com/issue/KT-49921) Setup basic release performance regression tests
- [`KT-51177`](https://youtrack.jetbrains.com/issue/KT-51177) After updating from KGP 1.5.30 to 1.6.10, KotlinCompile is non-incremental given an Android resource change
- [`KT-48134`](https://youtrack.jetbrains.com/issue/KT-48134) Debug log level causes build cache miss
- [`KT-50719`](https://youtrack.jetbrains.com/issue/KT-50719) Kotlin Gradle Plugin may hang on writing statistics
- [`KT-48849`](https://youtrack.jetbrains.com/issue/KT-48849) Cache miss due to empty directories in `KotlinCompile` inputs
- [`KT-49014`](https://youtrack.jetbrains.com/issue/KT-49014) Disable Explicit API is not possible
- [`KT-48408`](https://youtrack.jetbrains.com/issue/KT-48408) Build may fail with strict JVM target validation mode when project has no Kotlin sources
- [`KT-49107`](https://youtrack.jetbrains.com/issue/KT-49107) Configuration cache: undeclared kotlin.caching.enabled system property read
- [`KT-50369`](https://youtrack.jetbrains.com/issue/KT-50369) Deprecate kotlin.experimental.coroutines Gradle DSL option and kotlin.coroutines property
- [`KT-48046`](https://youtrack.jetbrains.com/issue/KT-48046) Gradle Throws Exception From Kotlin Plugin: `destinationDir must not be null`
- [`KT-50037`](https://youtrack.jetbrains.com/issue/KT-50037) Kotlin compile task registers more than one task action and their order of execution is counter-intuitive
- [`KT-49772`](https://youtrack.jetbrains.com/issue/KT-49772) Kotlin in-process compilation does not release file handles
- [`KT-47215`](https://youtrack.jetbrains.com/issue/KT-47215) KJS: "UninitializedPropertyAccessException: lateinit property fileHasher has not been initialized" when running `kotlinNpmInstall` or `rootPackageJson` locally
- [`KT-46406`](https://youtrack.jetbrains.com/issue/KT-46406) Remove 'kotlin.parallel.tasks.in.project' build property

### Tools. Gradle. JS

#### Performance Improvements

- [`KT-49037`](https://youtrack.jetbrains.com/issue/KT-49037) KJS / Gradle: Configuration cache usage on large projects lead to high memory consumption by Gradle daemon

#### Fixes

- [`KT-51060`](https://youtrack.jetbrains.com/issue/KT-51060) KJS / IR: Incorrect order of libraries with IC
- [`KT-49061`](https://youtrack.jetbrains.com/issue/KT-49061) KJS / Gradle: Custom package.json handlers break configuration cache
- [`KT-35640`](https://youtrack.jetbrains.com/issue/KT-35640) Kotlin/JS: Gradle: DCE `devMode = true` setting has no effect on incremental build
- [`KT-49095`](https://youtrack.jetbrains.com/issue/KT-49095) KJS / Gradle: KotlinJsTest tasks increase configuration cache state size depending on number of modules in project
- [`KT-49253`](https://youtrack.jetbrains.com/issue/KT-49253) KJS / Gradle: Error while evaluating property 'filteredArgumentsMap' of task ':compileProductionExecutableKotlinJs'
- [`KT-49902`](https://youtrack.jetbrains.com/issue/KT-49902) Kotlin/JS: Gradle: with --debug it still runs yarn without --ignore-scripts
- [`KT-49808`](https://youtrack.jetbrains.com/issue/KT-49808) KJS / Gradle: NPE when running node.js Mocha tests with configuration cache reuse
- [`KT-49530`](https://youtrack.jetbrains.com/issue/KT-49530) KJS: Update Node.JS and Yarn
- [`KT-50930`](https://youtrack.jetbrains.com/issue/KT-50930) KJS / IR: Incremental compilation cache building not consider multiple artifacts
- [`KT-50485`](https://youtrack.jetbrains.com/issue/KT-50485) KJS / IR: Enable per-module by default
- [`KT-49445`](https://youtrack.jetbrains.com/issue/KT-49445) KJS / IR: "AssertionError: Built-in class kotlin.Unit is not found" 1.5.31 fails on consuming artifacts built with 1.6.0
- [`KT-38040`](https://youtrack.jetbrains.com/issue/KT-38040) Make Chrome Headless use "--no-sandbox" (configurable) - for Docker

### Tools. Gradle. Multiplatform

- [`KT-46198`](https://youtrack.jetbrains.com/issue/KT-46198) [Commonizer] c-interop commonization: Support publishing libraries compiled with commonized c-interop libraries
- [`KT-41641`](https://youtrack.jetbrains.com/issue/KT-41641) MPP: NoSuchElementException: "Collection is empty" when android library is added but `android()` source set isn't
- [`KT-50567`](https://youtrack.jetbrains.com/issue/KT-50567) commonizeNativeDistribution fails when enableGranularSourceSetsMetadata is set to true
- [`KT-50592`](https://youtrack.jetbrains.com/issue/KT-50592) [Gradle][MPP] Mitigate isolated KGP classpath issues
- [`KT-51176`](https://youtrack.jetbrains.com/issue/KT-51176) CInteropCommonization: Warn users about disabled cinterop commonization when cinterops are present in hmpp
- [`KT-49089`](https://youtrack.jetbrains.com/issue/KT-49089) An annotation class annotated with @OptionalExpectation can not be used in another module with HMPP enabled
- [`KT-48818`](https://youtrack.jetbrains.com/issue/KT-48818) False positive warning about used `enableDependencyPropagation` flag with enabled hierarchical mpp by default
- [`KT-49596`](https://youtrack.jetbrains.com/issue/KT-49596) Composite Metadata Jar: Read location of cinterops from KotlinProjectStructureMetadata file
- [`KT-41823`](https://youtrack.jetbrains.com/issue/KT-41823) Default arguments not work in iosMain metadata with enableGranularSourceSetsMetadata
- [`KT-50574`](https://youtrack.jetbrains.com/issue/KT-50574) Only enable cinterop metadata transformation when 'kotlin.mpp.enableCInteropCommonization' is set

### Tools. Gradle. Native

#### New Features

- [`KT-47633`](https://youtrack.jetbrains.com/issue/KT-47633) Accept version when configuring cocoapods for kotlin/native
- [`KT-42630`](https://youtrack.jetbrains.com/issue/KT-42630) CocoaPods Gradle plugin: Allow customization of podspec properties
- [`KT-48553`](https://youtrack.jetbrains.com/issue/KT-48553) Kotlin/Native: use Gradle Shared Build Service to read konan.properties
- [`KT-47529`](https://youtrack.jetbrains.com/issue/KT-47529) Read list of opt-in cacheable native targets from konan.properties

#### Fixes

- [`KT-49330`](https://youtrack.jetbrains.com/issue/KT-49330) commonizeNativeDistribution: "ClassCastException: KotlinJvmProjectExtension_Decorated cannot be cast to class org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension"
- [`KT-49484`](https://youtrack.jetbrains.com/issue/KT-49484) Kotlin/Native: XCFramework should include dSYM files for release artifacts
- [`KT-47768`](https://youtrack.jetbrains.com/issue/KT-47768) Gradle MPP plugin: K/N targets break task configuration avoidance for assemble task
- [`KT-49971`](https://youtrack.jetbrains.com/issue/KT-49971) Make 'embedAndSignAppleFrameworkForXcode' task visible for users
- [`KT-43815`](https://youtrack.jetbrains.com/issue/KT-43815) [CocoaPods Plugin] Pod name cannot be changed
- [`KT-50105`](https://youtrack.jetbrains.com/issue/KT-50105) Usage of XCFramework and cocoapods plugin leads to error in prepareKotlinBuildScriptModel task
- [`KT-42105`](https://youtrack.jetbrains.com/issue/KT-42105) Multiplatform Cocoapod kotlin plugin doesn't support repo distribution
- [`KT-48808`](https://youtrack.jetbrains.com/issue/KT-48808) XCFramework task fails when using static debug frameworks
- [`KT-42755`](https://youtrack.jetbrains.com/issue/KT-42755) Cocoapods plugin using backslashes on Windows

### Tools. JPS

- [`KT-51434`](https://youtrack.jetbrains.com/issue/KT-51434) Language version 1.7 is absent in Kotlin Compiler settings
- [`KT-48399`](https://youtrack.jetbrains.com/issue/KT-48399) Internal Error: Unknown version of LookupSymbolKeyDescriptor=-6
- [`KT-49177`](https://youtrack.jetbrains.com/issue/KT-49177) JPS: Kotlin compiler doesn't recompile file that references Java constant in class body
- [`KT-46506`](https://youtrack.jetbrains.com/issue/KT-46506) JPS: Kotlin compiler doesn't recompile file that references Java constant
- [`KT-47909`](https://youtrack.jetbrains.com/issue/KT-47909) UtilsKt.recordPackageLookup is slow
- [`KT-47857`](https://youtrack.jetbrains.com/issue/KT-47857) Class loaders clash when building kotlin project with FIR with JPS without daemon
- [`KT-45773`](https://youtrack.jetbrains.com/issue/KT-45773) Improve quality of JPS incremental compilation

### Tools. Kapt

- [`KT-41456`](https://youtrack.jetbrains.com/issue/KT-41456) Kotlin compilation failure after successful kapt causes invalid incremental builds
- [`KT-48402`](https://youtrack.jetbrains.com/issue/KT-48402) Kapt throws "Fatal Error: Unable to find package java.lang in classpath or bootclasspath" with JDK 16 and Kotlin 1.5.30
- [`KT-48617`](https://youtrack.jetbrains.com/issue/KT-48617) Cache miss due to empty directories in KaptGenerateStubsTask inputs
- [`KT-48450`](https://youtrack.jetbrains.com/issue/KT-48450) Allow to configure additional jvm arguments for 'KaptWithoutKotlincTask`
- [`KT-32596`](https://youtrack.jetbrains.com/issue/KT-32596) kapt replaces class generated by annotation processor with error.NonExistentClass when the class is used as an annotation
- [`KT-48826`](https://youtrack.jetbrains.com/issue/KT-48826) Deprecate 'kapt.use.worker.api' property
- [`KT-47002`](https://youtrack.jetbrains.com/issue/KT-47002) Kapt: warning mentions non-effective property `kapt.includeCompileClasspath`

### Tools. Maven

- [`KT-50306`](https://youtrack.jetbrains.com/issue/KT-50306) Configured plugin dependencies should be available in kotlin-maven-plugin scripts

### Tools. Parcelize

- [`KT-19853`](https://youtrack.jetbrains.com/issue/KT-19853) Parcelize: CREATOR field should be available from code

### Tools. REPL

- [`KT-20488`](https://youtrack.jetbrains.com/issue/KT-20488) REPL: java.lang.InternalError: "Enclosing constructor not found" for class reference on an anonymous object literal

### Tools. Scripts

- [`KT-48758`](https://youtrack.jetbrains.com/issue/KT-48758) Kotlin scripting: introduce a link from evaluation configuration to the compilation one
- [`KT-48414`](https://youtrack.jetbrains.com/issue/KT-48414) Script: get location of the script.main.kts file
- [`KT-40497`](https://youtrack.jetbrains.com/issue/KT-40497) “Cannot inline bytecode built with JVM target 1.8 into bytecode that is being built with JVM target 1.6.” for running script without specified jvm-target
- [`KT-49329`](https://youtrack.jetbrains.com/issue/KT-49329) Kotlin scripts are compiled with jvmTarget 1.8 by default and in many scenarios there is no way to redefine it, which lead to inlining errors e.g. with JDK 17

### Tools. Wasm

- [`KT-49893`](https://youtrack.jetbrains.com/issue/KT-49893) Don't publish wasm targets with `org.jetbrains.kotlin.js.compiler` attribute


## 1.6.10

### Android

- [`KT-49798`](https://youtrack.jetbrains.com/issue/KT-49798) [MPP] [Android] AGP 7.1.0+ android target publications leak 'AgpVersionAttr' attribute

### Compiler

#### Performance Improvements

- [`KT-49821`](https://youtrack.jetbrains.com/issue/KT-49821) Optimize LazyClassMemberScope#getContributedDescriptors: use nameFilter

#### Fixes

- [`KT-49833`](https://youtrack.jetbrains.com/issue/KT-49833) java.lang.NullPointerException caused by accidental newline in package directive
- [`KT-49838`](https://youtrack.jetbrains.com/issue/KT-49838) Type inference fails on 1.6.0: Cannot use 'CapturedType(*)' as reified type parameter
- [`KT-49752`](https://youtrack.jetbrains.com/issue/KT-49752) Regression in method return type inference: "IllegalStateException: Expected some types"
- [`KT-49876`](https://youtrack.jetbrains.com/issue/KT-49876) Kotlin/Native: cross-compilation of Linux static library is broken in Windows in 1.6.0
- [`KT-49792`](https://youtrack.jetbrains.com/issue/KT-49792) Atomicfu: "standalone invocation of kotlinx.atomicfu.AtomicInt::compareAndSet that was not traced to previous field load" with suspend function
- [`KT-49834`](https://youtrack.jetbrains.com/issue/KT-49834) Coroutine method transformer generates invalid locals table.
- [`KT-49441`](https://youtrack.jetbrains.com/issue/KT-49441) Support friend modules in Kotlin Native
- [`KT-49248`](https://youtrack.jetbrains.com/issue/KT-49248) K/N: Symbol with `IrSimpleFunctionSymbolImpl` is unbound after 1.5.30
- [`KT-49651`](https://youtrack.jetbrains.com/issue/KT-49651) Inconsistent compiler APIs for repeatable annotations
- [`KT-49168`](https://youtrack.jetbrains.com/issue/KT-49168) JVM IR: IndexOutOfBoundsException with fun interface + suspend function as SAM method
- [`KT-49573`](https://youtrack.jetbrains.com/issue/KT-49573) No annotated types, compiler emits "Annotated types are not supported in typeOf"
- [`KT-47192`](https://youtrack.jetbrains.com/issue/KT-47192) Build Fake Overrides for internal members of classes from friend module
- [`KT-48673`](https://youtrack.jetbrains.com/issue/KT-48673) IR: IllegalStateException for usage of internal member declared in a superclass in another module

### JavaScript

- [`KT-47811`](https://youtrack.jetbrains.com/issue/KT-47811) KJS / IR: "ClassCastException" when using suspend function in `console.log`

### Language Design

- [`KT-49868`](https://youtrack.jetbrains.com/issue/KT-49868) Support language version 1.3 in Kotlin 1.6.10

### Libraries

- [`KT-50173`](https://youtrack.jetbrains.com/issue/KT-50173) Different behavior of Regex escapeReplacement function in JVM and JS

### Tools. Android Extensions

- [`KT-49799`](https://youtrack.jetbrains.com/issue/KT-49799) NullPointerException when using kotlin-android-extensions synthetic after update to Kotlin 1.6.0

### Tools. Compiler Plugins

- [`KT-50005`](https://youtrack.jetbrains.com/issue/KT-50005) jvm-abi-gen plugin: do not change the declaration order in generated jars
- [`KT-49726`](https://youtrack.jetbrains.com/issue/KT-49726) JVM/IR: "IllegalArgumentException: Null argument in ExpressionCodegen for parameter VALUE_PARAMETER": Serialization with sealed class as type parameter

### Tools. Gradle

- [`KT-49835`](https://youtrack.jetbrains.com/issue/KT-49835) Android consumers can't resolve Android debug variants of published MPP libraries published with Kotlin 1.6.0 & Gradle 7.0+
- [`KT-49910`](https://youtrack.jetbrains.com/issue/KT-49910) Incremental compilation speed regression in 1.6.0 for Android projects

### Tools. Gradle. JS

- [`KT-49109`](https://youtrack.jetbrains.com/issue/KT-49109) KJS / Gradle: Configuration failed: Could not find node-14.17.0-darwin-arm64.tar.gz (org.nodejs:node:14.17.0)
- [`KT-50135`](https://youtrack.jetbrains.com/issue/KT-50135) KJS: Problem with Yarn install with scripts on Windows
- [`KT-34014`](https://youtrack.jetbrains.com/issue/KT-34014) Gradle, JS: Ability to persist / reuse yarn.lock
- [`KT-49505`](https://youtrack.jetbrains.com/issue/KT-49505) KJS / IR: Installation of NPM dependencies should be with ignore-scripts

### Tools. Gradle. Native

- [`KT-49931`](https://youtrack.jetbrains.com/issue/KT-49931) Kotlin Multiplatform Fails in Windows after 1.6.0 upgrade - Cannot run program "pod"
- [`KT-49771`](https://youtrack.jetbrains.com/issue/KT-49771) podInstall task is not executed after adding a pod dependency to the shared module

### Tools. Incremental Compile

- [`KT-49822`](https://youtrack.jetbrains.com/issue/KT-49822) Incremental compilation state is modified when the build fails in Kotlin 1.6
- [`KT-49340`](https://youtrack.jetbrains.com/issue/KT-49340) "IllegalStateException: `@NotNull` method org/jetbrains/kotlin/com/intellij/openapi/application/AsyncExecutionService.getService must not return null" with Anvil plugin and incremental compilation


## 1.6.0

### Android

- [`KT-48019`](https://youtrack.jetbrains.com/issue/KT-48019) Bundle Kotlin Tooling Metadata into apk artifacts
- [`KT-47733`](https://youtrack.jetbrains.com/issue/KT-47733) JVM / IR: Android Synthetic don't generate _findCachedViewById function

### Compiler

#### New Features

- [`KT-47984`](https://youtrack.jetbrains.com/issue/KT-47984) In-place arguments inlining for @InlineOnly functions
- [`KT-12794`](https://youtrack.jetbrains.com/issue/KT-12794) Allow runtime retention repeatable annotations when compiling under Java 8
- [`KT-43714`](https://youtrack.jetbrains.com/issue/KT-43714) Support annotations on class type parameters (AnnotationTarget.TYPE_PARAMETER)
- [`KT-45949`](https://youtrack.jetbrains.com/issue/KT-45949) Kotlin/Native: Improve bound check elimination
- [`KT-43919`](https://youtrack.jetbrains.com/issue/KT-43919) Support loading Java annotations on base classes and implementing interfaces'  type arguments
- [`KT-48194`](https://youtrack.jetbrains.com/issue/KT-48194) Try to resolve calls where we don't have enough type information, using the builder inference despite the presence of the annotation
- [`KT-47736`](https://youtrack.jetbrains.com/issue/KT-47736) Support conversion from regular functional types to suspending ones in JVM IR
- [`KT-39055`](https://youtrack.jetbrains.com/issue/KT-39055) Support property delegate created via synthetic method instead of field

#### Performance Improvements

- [`KT-45185`](https://youtrack.jetbrains.com/issue/KT-45185) FIR2IR: get rid of IrBuiltIns usages
- [`KT-47918`](https://youtrack.jetbrains.com/issue/KT-47918) JVM / IR: Performance degradation with const-bound for-cycles
- [`KT-33835`](https://youtrack.jetbrains.com/issue/KT-33835) Bytecode including unnecessary null checks for safe calls where left-hand side is non-nullable
- [`KT-41510`](https://youtrack.jetbrains.com/issue/KT-41510) Compilation of kotlin html DSL is still too slow
- [`KT-48211`](https://youtrack.jetbrains.com/issue/KT-48211) We spend a lot of time in ExpectActual declaration checker when there is very small amount of actual/expect declaration
- [`KT-39054`](https://youtrack.jetbrains.com/issue/KT-39054) Optimize delegated properties which call get/set on the given KProperty instance on JVM
- [`KT-46615`](https://youtrack.jetbrains.com/issue/KT-46615) Don't generate nullability assertions in methods for directly invoked lambdas

#### Fixes

- [`KT-49613`](https://youtrack.jetbrains.com/issue/KT-49613) JVM / IR: "Exception during IR lowering" with java fun interface and it's non-trivial usage
- [`KT-49548`](https://youtrack.jetbrains.com/issue/KT-49548) "ClassCastException: java.util.ArrayList$Itr cannot be cast to kotlin.collections.IntIterator" with Iterable inside `let`
- [`KT-22562`](https://youtrack.jetbrains.com/issue/KT-22562) Deprecate calls to "suspend" named functions with single dangling lambda argument
- [`KT-47120`](https://youtrack.jetbrains.com/issue/KT-47120) JVM IR: NoClassDefFoundError when there are an extension and a regular function with the same name
- [`KT-49477`](https://youtrack.jetbrains.com/issue/KT-49477) Has ran into recursion problem with two interdependant delegates
- [`KT-49442`](https://youtrack.jetbrains.com/issue/KT-49442) ClassCastException on reporting [EXPOSED_FROM_PRIVATE_IN_FILE] Deprecation: private-in-file class should not expose 'private-in-class'
- [`KT-49371`](https://youtrack.jetbrains.com/issue/KT-49371) JVM / IR: "NoSuchMethodError" with multiple inheritance
- [`KT-44843`](https://youtrack.jetbrains.com/issue/KT-44843) PSI2IR: "org.jetbrains.kotlin.psi2ir.generators.ErrorExpressionException: null: KtCallExpression" with delegate who has name or parameter with the same name as a property
- [`KT-49294`](https://youtrack.jetbrains.com/issue/KT-49294) Turning FlowCollector into 'fun interface' leads to AbstractMethodError
- [`KT-18282`](https://youtrack.jetbrains.com/issue/KT-18282) Companion object referencing it's own method during construction compiles successfully but fails at runtime with VerifyError
- [`KT-25289`](https://youtrack.jetbrains.com/issue/KT-25289) Prohibit access to class members in the super constructor call of its companion and nested object
- [`KT-32753`](https://youtrack.jetbrains.com/issue/KT-32753) Prohibit @JvmField on property in primary constructor that overrides interface property
- [`KT-43433`](https://youtrack.jetbrains.com/issue/KT-43433) `Suspend conversion is disabled` message in cases where it is not supported and quickfix to update language version is suggested
- [`KT-49399`](https://youtrack.jetbrains.com/issue/KT-49399) Building repeatable annotation with Container nested class fails with ISE: "Repeatable annotation class should have a container generated"
- [`KT-49209`](https://youtrack.jetbrains.com/issue/KT-49209) Default upper bound for type variables should be non-null
- [`KT-49335`](https://youtrack.jetbrains.com/issue/KT-49335) NPE in `RepeatedAnnotationLowering.wrapAnnotationEntriesInContainer` when using `@Repeatable` annotation from different file
- [`KT-48876`](https://youtrack.jetbrains.com/issue/KT-48876) java.lang.UnsupportedOperationException: org.jetbrains.kotlin.ir.expressions.impl.IrReturnableBlockImpl@4a729df2
- [`KT-48131`](https://youtrack.jetbrains.com/issue/KT-48131) IAE "Repeatable annotation container value must be a class reference" on using Kotlin-repeatable annotation from dependency
- [`KT-49322`](https://youtrack.jetbrains.com/issue/KT-49322) Postpone promoting warnings to errors for `ProperTypeInferenceConstraintsProcessing` feature
- [`KT-49285`](https://youtrack.jetbrains.com/issue/KT-49285) Exception on nested builder inference calls
- [`KT-49101`](https://youtrack.jetbrains.com/issue/KT-49101) IllegalArgumentException: ClassicTypeSystemContext couldn't handle: Captured(out Number)
- [`KT-41378`](https://youtrack.jetbrains.com/issue/KT-41378) Compilation failed: Deserializer for declaration public kotlinx.coroutines/SingleThreadDispatcher|null[0] is not found
- [`KT-47285`](https://youtrack.jetbrains.com/issue/KT-47285) IR deserialization exception when dependency KLIB has class instead of typealias
- [`KT-46697`](https://youtrack.jetbrains.com/issue/KT-46697) IllegalStateException: IrTypeAliasSymbol expected: Unbound public symbol for public kotlinx.coroutines/CancellationException|null[0] compiling KMM module for Kotlin/Native with Kotlin 1.5
- [`KT-36399`](https://youtrack.jetbrains.com/issue/KT-36399) Gradually support TYPE_USE nullability annotations read from class-files
- [`KT-11454`](https://youtrack.jetbrains.com/issue/KT-11454) Load annotations on TYPE_USE/TYPE_PARAMETER positions from Java class-files
- [`KT-18768`](https://youtrack.jetbrains.com/issue/KT-18768) @Notnull annotation from Java does not work with varargs
- [`KT-24392`](https://youtrack.jetbrains.com/issue/KT-24392) Nullability of Java arrays is read incorrectly if @Nullable annotation has both targets TYPE_USE and VALUE_PARAMETER
- [`KT-48157`](https://youtrack.jetbrains.com/issue/KT-48157) FIR: incorrect resolve with built-in names in use
- [`KT-46409`](https://youtrack.jetbrains.com/issue/KT-46409) FIR: erroneous resolve to qualifier instead of extension
- [`KT-44566`](https://youtrack.jetbrains.com/issue/KT-44566) `FirConflictsChecker` do not check for conflicting overloads across multiple files
- [`KT-37318`](https://youtrack.jetbrains.com/issue/KT-37318) FIR: Discuss treating flexible bounded constraints in inference
- [`KT-45989`](https://youtrack.jetbrains.com/issue/KT-45989) FIR: wrong callable reference type inferred
- [`KT-46058`](https://youtrack.jetbrains.com/issue/KT-46058) [FIR] Remove state from some checkers
- [`KT-45973`](https://youtrack.jetbrains.com/issue/KT-45973) FIR: wrong projection type inferred
- [`KT-43083`](https://youtrack.jetbrains.com/issue/KT-43083) [FIR] False positive 'HIDDEN' on internal
- [`KT-48794`](https://youtrack.jetbrains.com/issue/KT-48794) Breaking change in 1.5.30: Builder inference lambda contains inapplicable calls so {1} cant be inferred
- [`KT-46727`](https://youtrack.jetbrains.com/issue/KT-46727) Report warning on contravariant usages of star projected argument from Java
- [`KT-40668`](https://youtrack.jetbrains.com/issue/KT-40668) FIR: Ambiguity on qualifier when having multiple different same-named objects in near scopes
- [`KT-37081`](https://youtrack.jetbrains.com/issue/KT-37081) [FIR] errors NO_ELSE_IN_WHEN and INCOMPATIBLE_TYPES absence
- [`KT-48162`](https://youtrack.jetbrains.com/issue/KT-48162) NON_VARARG_SPREAD isn't reported on *toTypedArray() call
- [`KT-45118`](https://youtrack.jetbrains.com/issue/KT-45118) ClassCastException caused by parent and child class in if-else
- [`KT-47605`](https://youtrack.jetbrains.com/issue/KT-47605) Kotlin/Native: switch to LLD linker for MinGW targets
- [`KT-48912`](https://youtrack.jetbrains.com/issue/KT-48912) K/N `Symbol with IrSimpleFunctionSymbolImpl is unbound` and `JS Validation failed in file shaders.kt`
- [`KT-44436`](https://youtrack.jetbrains.com/issue/KT-44436) Support default not null annotations to enhance T into T!!
- [`KT-49190`](https://youtrack.jetbrains.com/issue/KT-49190) Increase stub versions
- [`KT-48261`](https://youtrack.jetbrains.com/issue/KT-48261) "overload resolution ambiguity" for JSpecify+jsr305-annotated Java List implementation
- [`KT-48778`](https://youtrack.jetbrains.com/issue/KT-48778) -Xtype-enhancement-improvements-strict-mode not respecting @NonNull annotation for property accesses?
- [`KT-48606`](https://youtrack.jetbrains.com/issue/KT-48606) [1.6] Instantiated annotations do not implement hashCode correctly/consistently
- [`KT-49157`](https://youtrack.jetbrains.com/issue/KT-49157) Tail-call optimization miss with cast to type parameter
- [`KT-46437`](https://youtrack.jetbrains.com/issue/KT-46437) NI: "Throwable: Resolution error of this type shouldn't occur for resolve if as a call" caused by reflectively accessing private property inside "if/else" or "when" expression
- [`KT-48590`](https://youtrack.jetbrains.com/issue/KT-48590) IllegalArgumentException: ClassicTypeSystemContext couldn't handle: Captured(*) reified type class reference
- [`KT-48633`](https://youtrack.jetbrains.com/issue/KT-48633) Can't infer builder inference's type argument across local class
- [`KT-49136`](https://youtrack.jetbrains.com/issue/KT-49136) JVM IR: NPE with safe call chain and property set to null by reflection
- [`KT-48570`](https://youtrack.jetbrains.com/issue/KT-48570) OptIn marker should not spread from class to its members
- [`KT-48928`](https://youtrack.jetbrains.com/issue/KT-48928) Prohibit using old JVM backend with language version >= 1.6
- [`KT-41978`](https://youtrack.jetbrains.com/issue/KT-41978) NI: Kotlin fails  to infer type of function argument
- [`KT-48101`](https://youtrack.jetbrains.com/issue/KT-48101) Smart cast on base class property is impossible if base class is from another module
- [`KT-48732`](https://youtrack.jetbrains.com/issue/KT-48732) JVM / IR: MalformedParameterizedTypeException is thrown when a Spring Bean of suspending function type is registered
- [`KT-47841`](https://youtrack.jetbrains.com/issue/KT-47841) Turning LV to 1.6 breaks some diagnostics based on jspecify annotations
- [`KT-48498`](https://youtrack.jetbrains.com/issue/KT-48498) JVM IR: IllegalAccessError with inline function call and property delegation from different module
- [`KT-48319`](https://youtrack.jetbrains.com/issue/KT-48319) JVM / IR: AssertionError: FUN caused by suspend lambda inside anonymous function
- [`KT-48835`](https://youtrack.jetbrains.com/issue/KT-48835) Psi2ir: vararg parameter value is lost when translating adapted function reference to base class member
- [`KT-46908`](https://youtrack.jetbrains.com/issue/KT-46908) JVM / IR: do not wrap fun interface implementation into another SAM adapter if it inherits from a functional type
- [`KT-48927`](https://youtrack.jetbrains.com/issue/KT-48927) JVM IR: "VerifyError: Bad invokespecial instruction: current class isn't assignable to reference class" when up-casting and read a base class's private property that has a custom getter in the base class's public function
- [`KT-48992`](https://youtrack.jetbrains.com/issue/KT-48992) Postpone migration to new operator resolution scheme for integer literals
- [`KT-48290`](https://youtrack.jetbrains.com/issue/KT-48290) Type bounds warning based on Java annotations not issues with language level 1.6
- [`KT-47920`](https://youtrack.jetbrains.com/issue/KT-47920) There is no warning on violated nullability of type parameter in accordance with java nullability annotation
- [`KT-41664`](https://youtrack.jetbrains.com/issue/KT-41664) Remove the "runtime JAR files in the classpath should have the same version" warning
- [`KT-48851`](https://youtrack.jetbrains.com/issue/KT-48851) Keep using warn mode for jspecify in 1.6
- [`KT-46829`](https://youtrack.jetbrains.com/issue/KT-46829) IR: NullPointerException caused by setting scoped generic extension var
- [`KT-42972`](https://youtrack.jetbrains.com/issue/KT-42972) Forbid protected constructor calls from public inline functions
- [`KT-45378`](https://youtrack.jetbrains.com/issue/KT-45378) Prohibit super calls in public-api inline functions
- [`KT-48515`](https://youtrack.jetbrains.com/issue/KT-48515) JSpecify: If a class has a @Nullable type-parameter bound, Kotlin should still treat unbounded wildcards like platform types
- [`KT-48825`](https://youtrack.jetbrains.com/issue/KT-48825) JVM IR: NPE with delegated property "by this" to base class
- [`KT-48535`](https://youtrack.jetbrains.com/issue/KT-48535) Make EXPERIMENTAL_ANNOTATION_ON_OVERRIDE warning
- [`KT-47928`](https://youtrack.jetbrains.com/issue/KT-47928) Prohibit declarations of repeatable annotation classes whose container annotation violates JLS
- [`KT-47971`](https://youtrack.jetbrains.com/issue/KT-47971) Report error on declaration of a repeatable annotation class with nested class named Container
- [`KT-48478`](https://youtrack.jetbrains.com/issue/KT-48478) JVM IR: Coroutines 1.5.1 + Kotlin 1.5.30 - ClassCastException: CompletedContinuation cannot be cast to DispatchedContinuation
- [`KT-48523`](https://youtrack.jetbrains.com/issue/KT-48523) Kotlin/Native: cross-compilation from Linux to MinGW not working when `platform.posix` is used
- [`KT-48671`](https://youtrack.jetbrains.com/issue/KT-48671) JVM / IR: "AssertionError: Primitive array expected: CLASS IR_EXTERNAL_DECLARATION_STUB CLASS"
- [`KT-46181`](https://youtrack.jetbrains.com/issue/KT-46181) JVM IR: private @JvmStatic function is generated in the outer class instead of companion object, which breaks existing calls via JNI or reflection (e.g. JUnit @MethodSource)
- [`KT-48736`](https://youtrack.jetbrains.com/issue/KT-48736) JVM IR: assert in SyntheticAccessorLowering when inline function attempts to access package-private field from Java
- [`KT-48653`](https://youtrack.jetbrains.com/issue/KT-48653) Warnings on non-exhaustive when statements missing in some cases with 1.6
- [`KT-48394`](https://youtrack.jetbrains.com/issue/KT-48394) JVM: Invalid locals caused by unboxing bytecode optimization
- [`KT-20542`](https://youtrack.jetbrains.com/issue/KT-20542) IllegalAccessError on calling private function with default parameters from internal inline function used in another package
- [`KT-48331`](https://youtrack.jetbrains.com/issue/KT-48331) JVM / IR: "VerifyError: Bad access to protected data in invokevirtual" when a sealed class uses another sealed class in its same hierarchy level as a constructor parameter
- [`KT-48380`](https://youtrack.jetbrains.com/issue/KT-48380) kotlin.RuntimeException: Unexpected receiver type
- [`KT-47855`](https://youtrack.jetbrains.com/issue/KT-47855) Kotlin/Native: compilation fails due to Escape Analysis
- [`KT-48291`](https://youtrack.jetbrains.com/issue/KT-48291) False positive [ACTUAL_MISSING] Declaration must be marked with 'actual' when implementing actual interface
- [`KT-48445`](https://youtrack.jetbrains.com/issue/KT-48445) "IAE: Top level call context should not be null to analyze coroutine-lambda" when compiling Kotlin with language version 1.6
- [`KT-48618`](https://youtrack.jetbrains.com/issue/KT-48618) Enable by default "suspend conversion" feature in 1.6
- [`KT-47638`](https://youtrack.jetbrains.com/issue/KT-47638) Drop EXPERIMENTAL_IS_NOT_ENABLED diagnostic
- [`KT-48589`](https://youtrack.jetbrains.com/issue/KT-48589) KotlinTypeRefiner is lost, leading to TYPE_MISMATCH and OVERLOAD_RESOLUTION_AMBIGUITY issues with MPP projects
- [`KT-48615`](https://youtrack.jetbrains.com/issue/KT-48615) Inconsistent behavior with integer literals overflow (Implementation)
- [`KT-47937`](https://youtrack.jetbrains.com/issue/KT-47937) Implement deprecation of computing constant values of complex boolean expressions in when condition branches and conditions of loops
- [`KT-48391`](https://youtrack.jetbrains.com/issue/KT-48391) JVM / IR: "AssertionError: SyntheticAccessorLowering should not attempt to modify other files!" caused by class which inherits interface which has default function with default argument from companion const val
- [`KT-48552`](https://youtrack.jetbrains.com/issue/KT-48552) Kotlin/Native: iosArm64 debug build fails in 1.6.0-M1-139
- [`KT-46182`](https://youtrack.jetbrains.com/issue/KT-46182) Native: prohibit using dots in identifiers
- [`KT-46230`](https://youtrack.jetbrains.com/issue/KT-46230) JVM IR: "IllegalArgumentException: Null argument in ExpressionCodegen for parameter VALUE_PARAMETER MOVED_DISPATCH_RECEIVER" with value class overriding function with default parameter
- [`KT-48302`](https://youtrack.jetbrains.com/issue/KT-48302) FIR: Investigate not-null assertion on generic Java method
- [`KT-48350`](https://youtrack.jetbrains.com/issue/KT-48350) JVM IR: NPE from LocalDeclarationsLowering on property reference with field from outer class used as receiver (1.6.0-M1 regression)
- [`KT-48500`](https://youtrack.jetbrains.com/issue/KT-48500) AE: "Last parameter type of suspend function must be Continuation, but it is kotlin.coroutines.experimental.Continuation" for `kotlin-stdlib-common` library
- [`KT-48469`](https://youtrack.jetbrains.com/issue/KT-48469) Problem with properties lazy initialization while using kotlinx.serialization plugin
- [`KT-48432`](https://youtrack.jetbrains.com/issue/KT-48432) Regression in IntRange.contains (and probably other ranges too) when used in-place
- [`KT-44855`](https://youtrack.jetbrains.com/issue/KT-44855) "AssertionError: SyntheticAccessorLowering should not attempt to modify other files" on smart cast of protected field owner
- [`KT-47542`](https://youtrack.jetbrains.com/issue/KT-47542) Incorrect ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED after migration to 1.6 on class indirectly extending RuntimeException
- [`KT-48166`](https://youtrack.jetbrains.com/issue/KT-48166) FIR: incorrect common supertype for PSI elements hierarchy
- [`KT-47499`](https://youtrack.jetbrains.com/issue/KT-47499) JVM / IR: java.lang.VerifyError: Bad access to protected data in invokevirtual when trying to clone the result of lambda invocation that is "this"  in an extension function
- [`KT-46451`](https://youtrack.jetbrains.com/issue/KT-46451) JVM Debugging: stepping on finally block end brace line before stepping into finally code
- [`KT-48329`](https://youtrack.jetbrains.com/issue/KT-48329) It's impossible to infer a type variables based on several builder inference lambdas
- [`KT-48193`](https://youtrack.jetbrains.com/issue/KT-48193) Don't use the builder inference for calls which can be resolved without it
- [`KT-46450`](https://youtrack.jetbrains.com/issue/KT-46450) JVM Debugging: some break statements in catch blocks have no line numbers and you cannot set breakpoints on them
- [`KT-48298`](https://youtrack.jetbrains.com/issue/KT-48298) FIR: incorrect deserialized annotations in back-end
- [`KT-48234`](https://youtrack.jetbrains.com/issue/KT-48234) FIR: false positive NON_INTERNAL_PUBLISHED_API for primary constructor property
- [`KT-48174`](https://youtrack.jetbrains.com/issue/KT-48174) IR interpreter: unsupported compile-time call
- [`KT-48158`](https://youtrack.jetbrains.com/issue/KT-48158) FIR: false positive ASSIGNMENT_TYPE_MISMATCH as a result of wrong type inference around callable references
- [`KT-48109`](https://youtrack.jetbrains.com/issue/KT-48109) FIR: incorrect type inference for generic argument of intersection type
- [`KT-48161`](https://youtrack.jetbrains.com/issue/KT-48161) FIR: false positive ARGUMENT_TYPE_MISMATCH for bounded type parameter VS Java not-null
- [`KT-48159`](https://youtrack.jetbrains.com/issue/KT-48159) FIR: erroneous scope order during type resolve of constructor return type
- [`KT-48165`](https://youtrack.jetbrains.com/issue/KT-48165) FIR: false positive "initializer should be a constant value" on String.length
- [`KT-48175`](https://youtrack.jetbrains.com/issue/KT-48175) FIR: exception for JvmField on local class property
- [`KT-48164`](https://youtrack.jetbrains.com/issue/KT-48164) FIR: false positive OVERRIDING_FINAL_MEMBER in enum entry
- [`KT-48116`](https://youtrack.jetbrains.com/issue/KT-48116) FIR: false positive NOT_A_LOOP_LABEL
- [`KT-48102`](https://youtrack.jetbrains.com/issue/KT-48102) FIR: false positive ABSTRACT_MEMBER_NOT_IMPLEMENTED with mapped stdlib functions
- [`KT-47911`](https://youtrack.jetbrains.com/issue/KT-47911) Native compiler on ios_arm64 target generates movi.2d instructions, which are mishandled by Apple hardware
- [`KT-48105`](https://youtrack.jetbrains.com/issue/KT-48105) FIR: generic/specific callable reference ambiguity
- [`KT-14392`](https://youtrack.jetbrains.com/issue/KT-14392) Repeated annotation with use site target is not detected for getter and setter
- [`KT-47493`](https://youtrack.jetbrains.com/issue/KT-47493) Missed frontend diagnostic in try/catch
- [`KT-48058`](https://youtrack.jetbrains.com/issue/KT-48058) "No type for expression" compiler exception on calls with unused lambda
- [`KT-47597`](https://youtrack.jetbrains.com/issue/KT-47597) JVM IR: if statement doesn't eval correctly on 1.5.20 possible nullable type differences.
- [`KT-47922`](https://youtrack.jetbrains.com/issue/KT-47922) False negative type mismatch on empty when as last statement of lambda
- [`KT-34594`](https://youtrack.jetbrains.com/issue/KT-34594) Do not generate fake debugger variables initialization for @InlineOnly functions
- [`KT-47749`](https://youtrack.jetbrains.com/issue/KT-47749) Incorrect scope of a local variable inside the coroutine
- [`KT-47527`](https://youtrack.jetbrains.com/issue/KT-47527) JVM / IR ClassCastException: "kotlin.Unit cannot be cast to java.lang.String"
- [`KT-47840`](https://youtrack.jetbrains.com/issue/KT-47840) JVM / IR: "IllegalStateException: No mapping for symbol: VALUE_PARAMETER name: x" in nested local functions with recursive calls
- [`KT-46448`](https://youtrack.jetbrains.com/issue/KT-46448) JVM Debugging: Locals in finally blocks not always duplicated when the finally block is
- [`KT-47716`](https://youtrack.jetbrains.com/issue/KT-47716) JVM / IR: NoSuchMethodError when trying to get MAX_VALUE from ULong in non-trivial try/finally context
- [`KT-47762`](https://youtrack.jetbrains.com/issue/KT-47762) JVM / IR: Properties with the same signatures in inline class and its companion object crashes the compiler with NullPointerException
- [`KT-47741`](https://youtrack.jetbrains.com/issue/KT-47741) JVM / IR: VerifyError: Bad type on operand stack with iterator and invoking method reference to IntIterator
- [`KT-43696`](https://youtrack.jetbrains.com/issue/KT-43696) ClassFormatError on @JvmStatic external fun in interface companion object
- [`KT-47715`](https://youtrack.jetbrains.com/issue/KT-47715) JVM / IR, R8: External getter cannot be represented in dex format
- [`KT-47684`](https://youtrack.jetbrains.com/issue/KT-47684) Add warning on `is` checks which are always false
- [`KT-47685`](https://youtrack.jetbrains.com/issue/KT-47685) False positive CAST_NEVER_SUCCEEDS on variable of intersection type
- [`KT-32188`](https://youtrack.jetbrains.com/issue/KT-32188) NI: False positive "This cast can never succeed"
- [`KT-35687`](https://youtrack.jetbrains.com/issue/KT-35687) NI: Poor cast can never succeed [CAST_NEVER_SUCCEEDS]
- [`KT-41331`](https://youtrack.jetbrains.com/issue/KT-41331) False negative USELESS_IS_CHECK with null
- [`KT-47609`](https://youtrack.jetbrains.com/issue/KT-47609) JVM IR: "AssertionError: Unexpected number of type arguments" when compiling an extension property with annotation and it extends a value class with a generic parameter
- [`KT-47413`](https://youtrack.jetbrains.com/issue/KT-47413) FIR: Rework FirDelegatedScope
- [`KT-47492`](https://youtrack.jetbrains.com/issue/KT-47492) Illegal use of DUP

### Docs & Examples

- [`KT-48534`](https://youtrack.jetbrains.com/issue/KT-48534) Wrong compiler argument for RequiresOptIn

### IDE

- [`KT-48604`](https://youtrack.jetbrains.com/issue/KT-48604) MISSING_DEPENDENCY_CLASS in test source sets with kotlin.mpp.enableGranularSourceSetsMetadata=true

### IDE. Debugger

- [`KT-47970`](https://youtrack.jetbrains.com/issue/KT-47970) AE: "Either library or explicit name have to be provided <built-ins module>" in IR debugger tests

### IDE. Gradle Integration

- [`KT-46273`](https://youtrack.jetbrains.com/issue/KT-46273) MPP: Don't fail import for case of missed platform in source set structure
- [`KT-48823`](https://youtrack.jetbrains.com/issue/KT-48823) Improve error reporting on import when configuration phase in Gradle failed
- [`KT-48504`](https://youtrack.jetbrains.com/issue/KT-48504) MPP: UninitializedPropertyAccessException on import if new hierarchical mpp flag conflicts with other flags
- [`KT-47463`](https://youtrack.jetbrains.com/issue/KT-47463) MPP: Import fails with `Task 'runCommonizer' not found in root project` if Kotlin configured only in module

### IDE. Multiplatform

- [`KT-47604`](https://youtrack.jetbrains.com/issue/KT-47604) kotlin-stdlib-common leaks into dependencies of Android-specific source sets

### JavaScript

- [`KT-43783`](https://youtrack.jetbrains.com/issue/KT-43783) KJS / IR: companion object and nested objects are not exported
- [`KT-47524`](https://youtrack.jetbrains.com/issue/KT-47524) KJS / IR: Treat protected members as part of exported API
- [`KT-48132`](https://youtrack.jetbrains.com/issue/KT-48132) KJS / IR: "IllegalStateException" when interface methods don't have default implementation
- [`KT-47700`](https://youtrack.jetbrains.com/issue/KT-47700) Support instantiation of annotation classes on JS
- [`KT-48317`](https://youtrack.jetbrains.com/issue/KT-48317) KJS / IR: "TypeError: ... is not a function" on running code with suspend function inheritors
- [`KT-48344`](https://youtrack.jetbrains.com/issue/KT-48344) KJS / IR: incorrect call with vararg argument from suspend function
- [`KT-47751`](https://youtrack.jetbrains.com/issue/KT-47751) Kotlin/JS: IR + IC: "argument has no effect without source map" warnings on build

### Libraries

#### New Features

- [`KT-46423`](https://youtrack.jetbrains.com/issue/KT-46423) infix extension fun Comparable<T>.compareTo
- [`KT-47421`](https://youtrack.jetbrains.com/issue/KT-47421) Stabilize collection builders
- [`KT-48584`](https://youtrack.jetbrains.com/issue/KT-48584) Introduce JVM readln() and readlnOrNull() top-level functions

#### Performance Improvements

- [`KT-45438`](https://youtrack.jetbrains.com/issue/KT-45438) Remove brittle ‘contains’ optimization in minus/removeAll/retainAll

#### Fixes

- [`KT-28378`](https://youtrack.jetbrains.com/issue/KT-28378) Different behavior of Regex replace function in JVM and JS when replacement string contains group reference
- [`KT-46785`](https://youtrack.jetbrains.com/issue/KT-46785) Get rid of !! after readLine() in the standard library
- [`KT-46784`](https://youtrack.jetbrains.com/issue/KT-46784) Stabilize Duration API in the standard library
- [`KT-46229`](https://youtrack.jetbrains.com/issue/KT-46229) Bring back Duration factory extension properties
- [`KT-27738`](https://youtrack.jetbrains.com/issue/KT-27738) Make JS Regex.replace not inline
- [`KT-48607`](https://youtrack.jetbrains.com/issue/KT-48607) Stabilize experimental API for 1.6
- [`KT-47304`](https://youtrack.jetbrains.com/issue/KT-47304) Random#nextLong generates value outside provided range
- [`KT-47706`](https://youtrack.jetbrains.com/issue/KT-47706) System property that controls the brittle `contains` optimization
- [`KT-48999`](https://youtrack.jetbrains.com/issue/KT-48999) Align behavior of some JS functions with their JVM counterpart
- [`KT-46243`](https://youtrack.jetbrains.com/issue/KT-46243) Typography.leftGuillemete and Typography.rightGuillemete are named inconsistent with standard
- [`KT-46101`](https://youtrack.jetbrains.com/issue/KT-46101) Review deprecations in stdlib for 1.6
- [`KT-48456`](https://youtrack.jetbrains.com/issue/KT-48456) Introduce Common (multi-platform) readln() and readlnOrNull() top-level functions
- [`KT-48587`](https://youtrack.jetbrains.com/issue/KT-48587) Deprecate some of JS-only stdlib API
- [`KT-39328`](https://youtrack.jetbrains.com/issue/KT-39328) Make builder collection implementations serializable
- [`KT-47676`](https://youtrack.jetbrains.com/issue/KT-47676) K/JS: MatchResult.next() returns no expected next match if called after `matchEntire`
- [`KT-39166`](https://youtrack.jetbrains.com/issue/KT-39166) Nothing is silently mapped to Void in arguments of the type passed to typeOf
- [`KT-39330`](https://youtrack.jetbrains.com/issue/KT-39330) Migrate declarations from kotlin.dom and kotlin.browser packages to kotlinx.*
- [`KT-28753`](https://youtrack.jetbrains.com/issue/KT-28753) Comparing floating point values in array/list operations 'contains', 'indexOf', 'lastIndexOf': IEEE 754 or total order
- [`KT-38854`](https://youtrack.jetbrains.com/issue/KT-38854) Gradually change the return type of collection min/max functions to non-nullable
- [`KT-38754`](https://youtrack.jetbrains.com/issue/KT-38754) Deprecate appendln in favor of appendLine

### Native

- [`KT-48807`](https://youtrack.jetbrains.com/issue/KT-48807) Cinterop: cannot create bindings for a framework when Xcode 13 RC is installed
- [`KT-49384`](https://youtrack.jetbrains.com/issue/KT-49384) Kotlin/Native: Unexpected variance in super type argument: out @0
- [`KT-47424`](https://youtrack.jetbrains.com/issue/KT-47424) StackOverflowError in IR hashCode() methods compiling KMM module for Kotlin/Native with Kotlin 1.5.0+
- [`KT-49234`](https://youtrack.jetbrains.com/issue/KT-49234) SIGSEGV using the new memory manager in release in Kotlin 1.6.0-RC in MacosX64
- [`KT-48566`](https://youtrack.jetbrains.com/issue/KT-48566) ExceptionInInitializerError when configuring Gradle project with kotlin-multiplatform plugin on a host unsupported by Kotlin/Native
- [`KT-48039`](https://youtrack.jetbrains.com/issue/KT-48039) Native: support shaded (aka embeddable) compiler jar in Gradle plugin
- [`KT-42693`](https://youtrack.jetbrains.com/issue/KT-42693) Remove dependency on ncurses5 library

### Native. C Export

- [`KT-47209`](https://youtrack.jetbrains.com/issue/KT-47209) kotlin-native fails to generate valid C header if a setter takes anonymous parameter (_)

### Native. C and ObjC Import

- [`KT-48074`](https://youtrack.jetbrains.com/issue/KT-48074) Native: cinterop: __flexarr support

### Native. ObjC Export

- [`KT-47809`](https://youtrack.jetbrains.com/issue/KT-47809) Kotlin/Native: ObjC-export module name usage in klib compilation

### Native. Platforms

- [`KT-43024`](https://youtrack.jetbrains.com/issue/KT-43024) Kotlin/Native: Windows as cross-compilation target

### Native. Runtime

- [`KT-48452`](https://youtrack.jetbrains.com/issue/KT-48452) Kotlin/Native: Support thread state switching in termination handlers for the new MM

### Native. Runtime. Memory

- [`KT-48143`](https://youtrack.jetbrains.com/issue/KT-48143) Kotlin/Native: test fails with assert with new MM and state checker
- [`KT-48364`](https://youtrack.jetbrains.com/issue/KT-48364) Uninitialized top-level properties in new MM
- [`KT-44283`](https://youtrack.jetbrains.com/issue/KT-44283) staticCFunction with CValue parameter crashes when invoked off the main thread

### Native. Stdlib

- [`KT-47662`](https://youtrack.jetbrains.com/issue/KT-47662) [Native, All platforms] Incorrect parsing of long strings to Float and Double

### Reflection

- [`KT-45066`](https://youtrack.jetbrains.com/issue/KT-45066) Support flexible types (nullability, mutability, raw) in typeOf
- [`KT-35877`](https://youtrack.jetbrains.com/issue/KT-35877) typeOf<MutableList<*>> cannot be distinguished from typeOf<List<*>> in Kotlin/JVM

### Tools. CLI

- [`KT-49007`](https://youtrack.jetbrains.com/issue/KT-49007) Support three previous API versions
- [`KT-48622`](https://youtrack.jetbrains.com/issue/KT-48622) Introduce compiler X-flag to use the builder inference for all calls by default
- [`KT-32376`](https://youtrack.jetbrains.com/issue/KT-32376) “no main manifest attribute” on running the jar for cli-compiled Kotlin objects with main function
- [`KT-48026`](https://youtrack.jetbrains.com/issue/KT-48026) Add the compiler X-flag to enable self upper bound type inference
- [`KT-47640`](https://youtrack.jetbrains.com/issue/KT-47640) CLI: support -option=value format as for -Xoption=value
- [`KT-47099`](https://youtrack.jetbrains.com/issue/KT-47099) Add a stable compiler argument for opt-in requirements as soon as they are stable

### Tools. Commonizer

#### New Features

- [`KT-48455`](https://youtrack.jetbrains.com/issue/KT-48455) [Commonizer] Optimistic number commonization
- [`KT-48459`](https://youtrack.jetbrains.com/issue/KT-48459) [Commonizer] Add opt-in annotation to optimistically commonized numbers

#### Fixes

- [`KT-47430`](https://youtrack.jetbrains.com/issue/KT-47430) [Commonizer] 'platform.posix.DIR' not implementing 'CPointed' when commonized for "unixMain"
- [`KT-48567`](https://youtrack.jetbrains.com/issue/KT-48567) [Commonizer] pthread_self function is not commonized in atomicfu
- [`KT-48287`](https://youtrack.jetbrains.com/issue/KT-48287) [Commonizer] platform.posix.mkdir not commonized in OKIO
- [`KT-48286`](https://youtrack.jetbrains.com/issue/KT-48286) [Commonizer] platform.posix.ftruncate not commonized in OKIO
- [`KT-47523`](https://youtrack.jetbrains.com/issue/KT-47523) MPP: Unable to resolve c-interop dependency if platform is included in an intermediate source set with the only target
- [`KT-48278`](https://youtrack.jetbrains.com/issue/KT-48278) [Commonizer] platform.posix.usleep not commonized in sqliter
- [`KT-46691`](https://youtrack.jetbrains.com/issue/KT-46691) MPP: Type mismatch for hierarchically commonized typealiases
- [`KT-47221`](https://youtrack.jetbrains.com/issue/KT-47221) C-interop commonization fails if few targets reuse same source set
- [`KT-47775`](https://youtrack.jetbrains.com/issue/KT-47775) Commonizer don't run for shared native code if test source set depends on main
- [`KT-47053`](https://youtrack.jetbrains.com/issue/KT-47053) MPP: Unable to resolve c-interop commonized code from shared test source set
- [`KT-48118`](https://youtrack.jetbrains.com/issue/KT-48118) Commonized c-interop lib is not attached to common main source set
- [`KT-47641`](https://youtrack.jetbrains.com/issue/KT-47641) Enabled cInterop commonization triggers native compilation during Gradle sync in IDE
- [`KT-47056`](https://youtrack.jetbrains.com/issue/KT-47056) MPP: Change naming for folder with commonized c-interop libraries

### Tools. Compiler Plugins

- [`KT-48842`](https://youtrack.jetbrains.com/issue/KT-48842) Compiler crash: Symbol with IrFieldSymbolImpl is unbound
- [`KT-48117`](https://youtrack.jetbrains.com/issue/KT-48117) Kotlin AllOpen Plugin should open private methods
- [`KT-40340`](https://youtrack.jetbrains.com/issue/KT-40340) jvm-abi-gen plugin: failure with Android D8 (Dexer) tool
- [`KT-40133`](https://youtrack.jetbrains.com/issue/KT-40133) jvm-abi-gen plugin: fails for inline function containing apply block with anonymous object
- [`KT-28704`](https://youtrack.jetbrains.com/issue/KT-28704) jvm-abi-gen plugin: avoid calling codegen twice per module
- [`KT-48111`](https://youtrack.jetbrains.com/issue/KT-48111) JVM / IR: "IllegalAccessError: tried to access method" with NoArg plugin and sealed class

### Tools. Gradle

#### Performance Improvements

- [`KT-49159`](https://youtrack.jetbrains.com/issue/KT-49159) KotlinGradleBuildServices leaks Gradle instance when configuration cache is enabled

#### Fixes

- [`KT-45504`](https://youtrack.jetbrains.com/issue/KT-45504) Deprecate Gradle option KotlinJvmOptions.useIR since 1.5
- [`KT-49189`](https://youtrack.jetbrains.com/issue/KT-49189) In Gradle, dependencies on an MPP with Android+JVM fail to resolve in pure-Java projects
- [`KT-48830`](https://youtrack.jetbrains.com/issue/KT-48830) Change deprecation level to 'ERROR' for 'KotlinGradleSubplugin'
- [`KT-48264`](https://youtrack.jetbrains.com/issue/KT-48264) Cannot write Kotlin build report unless directory exists
- [`KT-48745`](https://youtrack.jetbrains.com/issue/KT-48745) JVM target compatibility check should be disabled when Java sources are empty
- [`KT-49066`](https://youtrack.jetbrains.com/issue/KT-49066) Setting kotlinOptions.modulePath in an android project breaks incremental compilation
- [`KT-48847`](https://youtrack.jetbrains.com/issue/KT-48847) Remove deprecated kotlin options marked for removal after 1.5
- [`KT-48245`](https://youtrack.jetbrains.com/issue/KT-48245) KGP makes compileOnly configuration resolvable
- [`KT-38010`](https://youtrack.jetbrains.com/issue/KT-38010) Invalid warning "Runtime JAR files in the classpath should have the same version." with `java-gradle-plugin`
- [`KT-48768`](https://youtrack.jetbrains.com/issue/KT-48768) Misleading 'jdkHome' deprecation message
- [`KT-46719`](https://youtrack.jetbrains.com/issue/KT-46719) Remove 'kotlin.useFallbackCompilerSearch' build option
- [`KT-47792`](https://youtrack.jetbrains.com/issue/KT-47792) KGP should ignore ProjectDependency when customize kotlin Dependencies
- [`KT-47867`](https://youtrack.jetbrains.com/issue/KT-47867) Replace usages of IncrementalTaskInputs with InputChanges
- [`KT-46972`](https://youtrack.jetbrains.com/issue/KT-46972) Migrate Kotlin repo to use Gradle toolchain feature

### Tools. Gradle. JS

- [`KT-49124`](https://youtrack.jetbrains.com/issue/KT-49124) KJS / Gradle: Unable to load '@webpack-cli/serve' command
- [`KT-49201`](https://youtrack.jetbrains.com/issue/KT-49201) KJS / Gradle: NPM dependencies resolution may fail on parallel builds
- [`KT-48241`](https://youtrack.jetbrains.com/issue/KT-48241) KJS / Gradle: NPM test dependency may break Gradle configuration cache
- [`KT-32071`](https://youtrack.jetbrains.com/issue/KT-32071) Possibility to disable downloading of Node.js and Yarn
- [`KT-48332`](https://youtrack.jetbrains.com/issue/KT-48332) Make NodeJsSetupTask and YarnSetupTask not cacheable
- [`KT-37895`](https://youtrack.jetbrains.com/issue/KT-37895) KJS: NPM Post-install Scripts sometimes print "node: not found"
- [`KT-34985`](https://youtrack.jetbrains.com/issue/KT-34985) kotlin-gradle-plugin: Should align ways NodeJs and Yarn are downloaded

### Tools. Gradle. Multiplatform

- [`KT-48709`](https://youtrack.jetbrains.com/issue/KT-48709) MPP: Task compileKotlinMacosX64 fails on matching native variants if ktlint presented
- [`KT-48919`](https://youtrack.jetbrains.com/issue/KT-48919) Gradle multiplatform plugin 1.6.0-M1 does not accept apiVersion = "1.7"
- [`KT-46343`](https://youtrack.jetbrains.com/issue/KT-46343) [Commonizer] Use lockfile for NativeDistributionCommonizationCache
- [`KT-48427`](https://youtrack.jetbrains.com/issue/KT-48427) Execution failed for task ':commonizeNativeDistribution'. > java.io.FileNotFoundException lock (No such file or directory)
- [`KT-48513`](https://youtrack.jetbrains.com/issue/KT-48513) Commonized platform libraries are unresolved in modules for new hierarchical MPP flag
- [`KT-48138`](https://youtrack.jetbrains.com/issue/KT-48138) CInteropCommonizer: Missing commonization request if test source set has different targets than associated main
- [`KT-35832`](https://youtrack.jetbrains.com/issue/KT-35832) Gradle: MPP plugin operates with -Xuse-experimental and not with -Xopt-in

### Tools. Gradle. Native

- [`KT-48729`](https://youtrack.jetbrains.com/issue/KT-48729) Test-source sets receive extra unnecessary granular dependencies to more common source sets when depending on MPP-library
- [`KT-37511`](https://youtrack.jetbrains.com/issue/KT-37511) CocoaPods Gradle plugin: Support incremental task execution when switching between Xcode and terminal
- [`KT-47362`](https://youtrack.jetbrains.com/issue/KT-47362) Cocoapods plugin: add error reporting for case when pod is not installed on user machine
- [`KT-37513`](https://youtrack.jetbrains.com/issue/KT-37513) CocoaPods Gradle plugin: Support building tests from terminal for projects depending on pods

### Tools. Kapt

- [`KT-45545`](https://youtrack.jetbrains.com/issue/KT-45545) Kapt is not compatible with JDK 16+
- [`KT-47853`](https://youtrack.jetbrains.com/issue/KT-47853) `KaptWithoutKotlincTask` eagerly resolves dependencies during construction/configuration and can cause deadlocks
- [`KT-47934`](https://youtrack.jetbrains.com/issue/KT-47934) KaptJavaLog is unable to map stub back to the kotlin source
- [`KT-48195`](https://youtrack.jetbrains.com/issue/KT-48195) Kapt causes dead lock in DefaultFileLockManager

### Tools. Scripts

- [`KT-49400`](https://youtrack.jetbrains.com/issue/KT-49400) Script resolver options can't take values with special symbols (/, \, $, :, .) in them
- [`KT-49012`](https://youtrack.jetbrains.com/issue/KT-49012) Compiling .kts script with inner class declaration fails with Backend Internal Error caused by AE: "Local class constructor can't have dispatch receiver"
- [`KT-47927`](https://youtrack.jetbrains.com/issue/KT-47927) Script: memory leak with new engines
- [`KT-48025`](https://youtrack.jetbrains.com/issue/KT-48025) JVM / IR / Script: IllegalStateException: No mapping for symbol: VALUE_PARAMETER INSTANCE_RECEIVER caused by method tnat returns outer function
- [`KT-48303`](https://youtrack.jetbrains.com/issue/KT-48303) main.kts script fails to detect vanished dependencies if run from the cache
- [`KT-48177`](https://youtrack.jetbrains.com/issue/KT-48177) Scripts: OutOfMemoryException with circular @file:Import
- [`KT-46645`](https://youtrack.jetbrains.com/issue/KT-46645) Scripts: "IllegalStateException: No mapping for symbol: VALUE_PARAMETER INSTANCE_RECEIVER" caused by get accessor


## Recent ChangeLogs:
### [ChangeLog-1.5.X](docs/changelogs/ChangeLog-1.5.X.md)
### [ChangeLog-1.4.X](docs/changelogs/ChangeLog-1.4.X.md)
### [ChangeLog-1.3.X](docs/changelogs/ChangeLog-1.3.X.md)
### [ChangeLog-1.2.X](docs/changelogs/ChangeLog-1.2.X.md)
### [ChangeLog-1.1.X](docs/changelogs/ChangeLog-1.1.X.md)
### [ChangeLog-1.0.X](docs/changelogs/ChangeLog-1.0.X.md)