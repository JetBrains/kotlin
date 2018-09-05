# CHANGELOG

## 1.3-M2

### Compiler

#### New Features

- [`KT-6592`](https://youtrack.jetbrains.com/issue/KT-6592) Support local variable initialization in lambda arguments to some functions
- [`KT-19323`](https://youtrack.jetbrains.com/issue/KT-19323) Allow generic type parameter to have mixed constraints for @InlineOnly functions
- [`KT-24857`](https://youtrack.jetbrains.com/issue/KT-24857) Support compatibility wrappers for coroutines in compiler
- [`KT-25320`](https://youtrack.jetbrains.com/issue/KT-25320) Support limited conversions for constants to simplify interop for Kotlin/Native
- [`KT-25372`](https://youtrack.jetbrains.com/issue/KT-25372) Support JvmName on annotation property getters
- [`KT-25495`](https://youtrack.jetbrains.com/issue/KT-25495) Release contract DSL as experimental
- [`KT-25557`](https://youtrack.jetbrains.com/issue/KT-25557) Metadata that maps coroutine label to the file name and line number
- [`KT-25559`](https://youtrack.jetbrains.com/issue/KT-25559) Metadata that maps coroutine spilled state to local variables
- [`KT-25655`](https://youtrack.jetbrains.com/issue/KT-25655) Safe or non-null calls required on x following check(x != null)
- [`KT-25972`](https://youtrack.jetbrains.com/issue/KT-25972) Support reading binary metadata of the next major release

#### Performance Improvements

- [`KT-24657`](https://youtrack.jetbrains.com/issue/KT-24657) Compiler performance issues with big files
- [`KT-26243`](https://youtrack.jetbrains.com/issue/KT-26243) Avoid redundant "specialized" method for suspend lambdas.

#### Fixes

- [`KT-19628`](https://youtrack.jetbrains.com/issue/KT-19628) Unresolved reference not reported on data class constructor `@get` annotation
- [`KT-20830`](https://youtrack.jetbrains.com/issue/KT-20830) Nulls can propagate through "enhanced nullability" types on module boundaries
- [`KT-21240`](https://youtrack.jetbrains.com/issue/KT-21240) Remove suspendCoroutineOrReturn intrinsic from compiler
- [`KT-22379`](https://youtrack.jetbrains.com/issue/KT-22379) Condition of while-loop with break can produce unsound smartcast
- [`KT-23438`](https://youtrack.jetbrains.com/issue/KT-23438) Back-end (JVM) Internal error: Failed to generate function suspendCoroutineOrReturn
- [`KT-23819`](https://youtrack.jetbrains.com/issue/KT-23819) Inline classes: mapping of fully generic classes 
- [`KT-23857`](https://youtrack.jetbrains.com/issue/KT-23857) Annotation with target TYPE is not applicable to TYPE_USE in Java sources
- [`KT-24717`](https://youtrack.jetbrains.com/issue/KT-24717) Allow number literals to be used as unsigned ones with unsigned expected typed
- [`KT-24860`](https://youtrack.jetbrains.com/issue/KT-24860) Forbid usage function expression as suspend function expression
- [`KT-24872`](https://youtrack.jetbrains.com/issue/KT-24872) Do not generate user-defined methods inside box class of inline class
- [`KT-24873`](https://youtrack.jetbrains.com/issue/KT-24873) Generate equals/hashCode/toString methods for inline classes same as for data classes
- [`KT-25246`](https://youtrack.jetbrains.com/issue/KT-25246) Incorrect bytecode generated for secondary constructor in inline class + primitive array
- [`KT-25278`](https://youtrack.jetbrains.com/issue/KT-25278) No smart cast for "returns() implies" contract when default argument is omitted
- [`KT-25287`](https://youtrack.jetbrains.com/issue/KT-25287) Getter-targeted annotations on annotation constructor parameters are lost
- [`KT-25293`](https://youtrack.jetbrains.com/issue/KT-25293) “Couldn't transform method node” error on compiling inline class with hashCode() method call when underlying value type is basic (number, char, boolean)
- [`KT-25299`](https://youtrack.jetbrains.com/issue/KT-25299) NoSuchMethodError Foo$Erased.hashCode(Ljava/lang/Object;) for hashCode(), toString() and equals() methods in inline classes
- [`KT-25328`](https://youtrack.jetbrains.com/issue/KT-25328) “Couldn't transform method node” error on compiling inline class which is wrapping Unit type
- [`KT-25330`](https://youtrack.jetbrains.com/issue/KT-25330) CCE “[Ljava.lang.Integer; cannot be cast to Foo” for inline class which is wrapping Array<T>
- [`KT-25521`](https://youtrack.jetbrains.com/issue/KT-25521) Coroutines state machine in Kotlin 1.3 should not have getLabel/setLabel
- [`KT-25558`](https://youtrack.jetbrains.com/issue/KT-25558) Stabilize field naming and mangling for suspending lambda classes
- [`KT-25580`](https://youtrack.jetbrains.com/issue/KT-25580) No warning about experimental API when unsigned types are inferred
- [`KT-25599`](https://youtrack.jetbrains.com/issue/KT-25599) “Exception during code generation” on compiling code with public constructor of unsigned numbers array
- [`KT-25614`](https://youtrack.jetbrains.com/issue/KT-25614) Support secondary constructors for inline classes
- [`KT-25683`](https://youtrack.jetbrains.com/issue/KT-25683) Compiler support for calling experimental suspend functions and function with experimental suspend function type as parameter
- [`KT-25688`](https://youtrack.jetbrains.com/issue/KT-25688) Add $continuation to LVT
- [`KT-25750`](https://youtrack.jetbrains.com/issue/KT-25750) CCE “Foo cannot be cast to java.lang.String” with inline class
- [`KT-25760`](https://youtrack.jetbrains.com/issue/KT-25760) Inline data class throws java.lang.VerifyError when trying toString() it
- [`KT-25794`](https://youtrack.jetbrains.com/issue/KT-25794) Incorrect code generated for accessing elements of Array<C> where C is inline class
- [`KT-25824`](https://youtrack.jetbrains.com/issue/KT-25824) Move SuspendFunctionN fictitious interfaces to kotlin.coroutines package
- [`KT-25825`](https://youtrack.jetbrains.com/issue/KT-25825) Allow to distinguish instances of function types from instances of suspend function types via kotlin.coroutines.jvm.internal.SuspendFunction marker interface
- [`KT-25912`](https://youtrack.jetbrains.com/issue/KT-25912) Calling groupingBy+reduce from suspend function causes IncompatibleClassChangeError
- [`KT-25914`](https://youtrack.jetbrains.com/issue/KT-25914) '==' for inline class with custom 'equals' uses underlying primitive type comparison instead
- [`KT-25973`](https://youtrack.jetbrains.com/issue/KT-25973) Report metadata version mismatch upon discovering a .kotlin_module file in the dependencies with an incompatible metadata version
- [`KT-25981`](https://youtrack.jetbrains.com/issue/KT-25981) Incorrect code generated for unboxed to boxed inline class equality
- [`KT-25983`](https://youtrack.jetbrains.com/issue/KT-25983) Inline class equality uses IEEE 754 instead of total order
- [`KT-26029`](https://youtrack.jetbrains.com/issue/KT-26029) Prohibit delegated properties inside inline classes
- [`KT-26030`](https://youtrack.jetbrains.com/issue/KT-26030) Prohibit implementation by delegation for inline classes
- [`KT-26052`](https://youtrack.jetbrains.com/issue/KT-26052) Inline Classes: IllegalArgumentException when underlying type is non-null but declared type is nullable
- [`KT-26101`](https://youtrack.jetbrains.com/issue/KT-26101) Prohibit inline classes with recursive underlying types
- [`KT-26103`](https://youtrack.jetbrains.com/issue/KT-26103) Inline class with type parameters is inconsistently mapped to JVM type if underlying type is a primitive
- [`KT-26120`](https://youtrack.jetbrains.com/issue/KT-26120) Inline Classes: Class inheritance is allowed but fails when referencing a superclass member

### IDE

- [`KT-25316`](https://youtrack.jetbrains.com/issue/KT-25316) PARTIAL resolve mode doesn't work when effects system is enabled
- [`KT-25611`](https://youtrack.jetbrains.com/issue/KT-25611) With Language / API version = "Latest stable" installing 1.3-M1 plugin upgrades actual values to 1.3
- [`KT-25681`](https://youtrack.jetbrains.com/issue/KT-25681) Remove "Coroutines (experimental)" settings from IDE and do not pass `-Xcoroutines` to JPS compiler (since 1.3)
- [`KT-25714`](https://youtrack.jetbrains.com/issue/KT-25714) Kotlin plugin updater suggests plugins incompatible with current Studio build platform
- [`KT-26239`](https://youtrack.jetbrains.com/issue/KT-26239) New MPP template: add `kotlin-test` libraries to the modules dependencies by default
- [`KT-26290`](https://youtrack.jetbrains.com/issue/KT-26290) Gradle Import: When all modules have the same language/API version use it for project-level settings as well

### IDE. Completion

- [`KT-25275`](https://youtrack.jetbrains.com/issue/KT-25275) Code completion does not take into account smart casts gotten from "returns implies" contract

### IDE. Inspections and Intentions

- [`KT-22330`](https://youtrack.jetbrains.com/issue/KT-22330) "Add remaining branch" quickfix doesn't properly import enum class
- [`KT-22354`](https://youtrack.jetbrains.com/issue/KT-22354) "Add remaining branches with import" quick fix causes KNPE at ImportAllMembersIntention$Companion.importReceiverMembers()
- [`KT-26158`](https://youtrack.jetbrains.com/issue/KT-26158) KNPE in "Create local variable"

### IDE. Multiplatform

- [`KT-26217`](https://youtrack.jetbrains.com/issue/KT-26217) "org.jetbrains.kotlin.resolve.MultiTargetPlatform$Common cannot be cast to org.jetbrains.kotlin.resolve.MultiTargetPlatform$Specific" on splitted actuals

### IDE. Script

- [`KT-25814`](https://youtrack.jetbrains.com/issue/KT-25814) IDE scripting console -> kotlin (JSR-223) - compilation errors - unresolved IDEA classes
- [`KT-25822`](https://youtrack.jetbrains.com/issue/KT-25822) jvmTarget from the script compiler options is ignored in the IDE

### JavaScript

- [`KT-22053`](https://youtrack.jetbrains.com/issue/KT-22053) JS: Secondary constructor of Throwable inheritor doesn't call to primary one
- [`KT-25014`](https://youtrack.jetbrains.com/issue/KT-25014) Support 'when' with subject variable in JS back-end
- [`KT-26064`](https://youtrack.jetbrains.com/issue/KT-26064) JS inliner calls wrong constructor in incremental build
- [`KT-26117`](https://youtrack.jetbrains.com/issue/KT-26117) JS runtime error: ArrayList_init instead of ArrayList_init_0
- [`KT-26138`](https://youtrack.jetbrains.com/issue/KT-26138) JS: prohibit external inline class
- [`KT-26171`](https://youtrack.jetbrains.com/issue/KT-26171) Prohibit inline classes as parameter and return type of external declaration

### Language design

- [`KT-7566`](https://youtrack.jetbrains.com/issue/KT-7566) Annotate kotlin.test.Test.assertNotNull() so that safe dereference isn't required after it
- [`KT-14397`](https://youtrack.jetbrains.com/issue/KT-14397) Make "smart cast" to non-null string working after isNullOrEmpty() check.
- [`KT-19532`](https://youtrack.jetbrains.com/issue/KT-19532) Evaluation order for constructor call
- [`KT-22274`](https://youtrack.jetbrains.com/issue/KT-22274) Restrict statement labels applicability

### Libraries

#### New Features

- [`KT-13814`](https://youtrack.jetbrains.com/issue/KT-13814) keys.associateWith { k -> v } function
- [`KT-15539`](https://youtrack.jetbrains.com/issue/KT-15539) Random Convenience Method in Ranges (LongRange, IntRange, etc)
- [`KT-15695`](https://youtrack.jetbrains.com/issue/KT-15695) String/Collection/Map/Array/Sequence.ifEmpty { null }
- [`KT-18986`](https://youtrack.jetbrains.com/issue/KT-18986) Debug-friendly toString implementation for CoroutineImpl
- [`KT-25570`](https://youtrack.jetbrains.com/issue/KT-25570) Random extensions to generate unsigned random numbers
- [`KT-25659`](https://youtrack.jetbrains.com/issue/KT-25659) Consider adding SuccessOrFailure.getOrDefault function
- [`KT-25874`](https://youtrack.jetbrains.com/issue/KT-25874) Support array copying between two existing arrays
- [`KT-25875`](https://youtrack.jetbrains.com/issue/KT-25875) Need more access to raw underlying array in unsigned arrays
- [`KT-25962`](https://youtrack.jetbrains.com/issue/KT-25962) Add contract for 'synchronized'
- [`KT-26339`](https://youtrack.jetbrains.com/issue/KT-26339) Introduce CoroutineStackFrame interface for coroutine stack reconstruction in debugger

#### Fixes

- [`KT-16097`](https://youtrack.jetbrains.com/issue/KT-16097) Index overflow when sequence has more than Int.MAX_VALUE elements
- [`KT-17176`](https://youtrack.jetbrains.com/issue/KT-17176) Long/Int progressions with Long.MIN_VALUE and Int.MIN_VALUE step are weird
- [`KT-19305`](https://youtrack.jetbrains.com/issue/KT-19305) IOStreams#readBytes is badly named
- [`KT-19489`](https://youtrack.jetbrains.com/issue/KT-19489) Array.copyOfRange returns value violating declared type when bounds are out of range
- [`KT-21049`](https://youtrack.jetbrains.com/issue/KT-21049) Different behavior in split by regex at JVM and JS
- [`KT-23799`](https://youtrack.jetbrains.com/issue/KT-23799) Discontinue deprecated artifacts distribution: kotlin-runtime, kotlin-jslib, kotlin-stdlib-jre7/8
- [`KT-25274`](https://youtrack.jetbrains.com/issue/KT-25274) contract() function has internal visibility
- [`KT-25303`](https://youtrack.jetbrains.com/issue/KT-25303) checkNotNull(T?) has no contract in contrast to checkNotNull(T?, () -> Any)
- [`KT-25771`](https://youtrack.jetbrains.com/issue/KT-25771) SuccessOrFailure.isFailure always returns false when boxed
- [`KT-25961`](https://youtrack.jetbrains.com/issue/KT-25961) Provide a way to create default-initialized 'U*Array'
- [`KT-26161`](https://youtrack.jetbrains.com/issue/KT-26161) String-to-number and number-to-string conversions for unsigned integers

### Reflection

- [`KT-14657`](https://youtrack.jetbrains.com/issue/KT-14657) Reflection: Provide ability to enumerate all cases of a sealed class
- [`KT-16795`](https://youtrack.jetbrains.com/issue/KT-16795) KType does not implement KAnnotatedElement
- [`KT-21972`](https://youtrack.jetbrains.com/issue/KT-21972) Reflection: Implement suspend functions `KCallable.callSuspend`,  `callSuspendBy`, and `isSuspend`

### Tools. CLI

- [`KT-25823`](https://youtrack.jetbrains.com/issue/KT-25823) Report a warning or error if an old language version or API version is used

### Tools. Gradle

- [`KT-26301`](https://youtrack.jetbrains.com/issue/KT-26301) In new MPP, a project with no `java` or `java-base` plugin applied cannot depend on a published MPP lib or run tests

### Tools. Scripts

- [`KT-26142`](https://youtrack.jetbrains.com/issue/KT-26142) update maven-central remote repository url



## 1.3-M1 IDE plugins update

### IDE

- Kotlin plugin for Android Studio 3.3 Canary 4 is ready
- [`KT-25713`](https://youtrack.jetbrains.com/issue/KT-25713) Android Studio on 182 platform: Gradle project re-import fails 
- [`KT-25733`](https://youtrack.jetbrains.com/issue/KT-25733) "Convert Java File to Kotlin File" action was always disabled



## 1.3-M1

### Language design

- [`KEEP-104`](https://github.com/Kotlin/KEEP/issues/104) Inline classes
- [`KEEP-135`](https://github.com/Kotlin/KEEP/issues/135) Unsigned integer types
- [`KEEP-95`](https://github.com/Kotlin/KEEP/issues/95) Experimental API annotations
- [`KT-4895`](https://youtrack.jetbrains.com/issue/KT-4895) Support assignment of "when" subject to a variable
- [`KT-13762`](https://youtrack.jetbrains.com/issue/KT-13762) Prohibit annotations with target 'EXPRESSION' and retention 'BINARY' or 'RUNTIME'
- [`KT-16681`](https://youtrack.jetbrains.com/issue/KT-16681) kotlin allows mutating the field of read-only property
- [`KT-21515`](https://youtrack.jetbrains.com/issue/KT-21515) Restrict visibility of classifiers, nested in companion objects

### Compiler

- [`KT-24848`](https://youtrack.jetbrains.com/issue/KT-24848) Refine loading Java overrides of Kotlin suspend functions
- [`KT-9580`](https://youtrack.jetbrains.com/issue/KT-9580) Report an error if 'setparam' target does not make sense for a parameter declaration
- [`KT-16310`](https://youtrack.jetbrains.com/issue/KT-16310) Nested classes inside enum entries capturing outer members
- [`KT-17981`](https://youtrack.jetbrains.com/issue/KT-17981) Type parameter for catch parameter possible when exception is nested in generic, but fails in runtime
- [`KT-21354`](https://youtrack.jetbrains.com/issue/KT-21354) Inconsistent behavior of 'for-in-range' loop if range is an array variable modified in loop body
- [`KT-25333`](https://youtrack.jetbrains.com/issue/KT-25333) Restrict visibility of Java static members from supertypes of companion object
- [`KT-25623`](https://youtrack.jetbrains.com/issue/KT-25623) Do not load experimental coroutines as non-suspend function with additional Continuation parameter

#### Backend. JVM

- [`KT-6301`](https://youtrack.jetbrains.com/issue/KT-6301) Support JvmStatic annotation on interface companion object members
- [`KT-25508`](https://youtrack.jetbrains.com/issue/KT-25508) Inject probeCoroutineSuspended to coroutines body
- [`KT-18987`](https://youtrack.jetbrains.com/issue/KT-18987) Unroll recursion in CoroutineImpl.resume
- [`KT-11567`](https://youtrack.jetbrains.com/issue/KT-11567) Companion object INSTANCE field more visible than companion object class itself
- [`KT-13764`](https://youtrack.jetbrains.com/issue/KT-13764) Support lambdas and function references for arities bigger than 22
- [`KT-16615`](https://youtrack.jetbrains.com/issue/KT-16615) Do not generate ConstantValue attribute for non-const vals
- [`KT-25193`](https://youtrack.jetbrains.com/issue/KT-25193) Names of parameters from Java interface methods implemented by delegation are lost  
- [`KT-25324`](https://youtrack.jetbrains.com/issue/KT-25324) VerifyError “Bad type on operand stack” on running code with call of array iterator for array of inline classes
- [`KT-25325`](https://youtrack.jetbrains.com/issue/KT-25325) CCE “Foo cannot be cast to java.lang.String” for iterating over the list of inline classes
- [`KT-25626`](https://youtrack.jetbrains.com/issue/KT-25626) Inline class values inside string literals don't use their own toString implementation

#### Backend. JVM. Coroutines

- [`KT-20219`](https://youtrack.jetbrains.com/issue/KT-20219) Inline suspend function can't be used as non-inline function
- [`KT-24863`](https://youtrack.jetbrains.com/issue/KT-24863) Support new Continuation API in JVM BE
- [`KT-24864`](https://youtrack.jetbrains.com/issue/KT-24864) Create new CoroutineImpl and other internal coroutines interfaces

#### Backend. JVM. Inline

- [`KT-25511`](https://youtrack.jetbrains.com/issue/KT-25511) Inline classes fail with cross-inline functions

#### Binary Metadata

- [`KT-24617`](https://youtrack.jetbrains.com/issue/KT-24617) Optional expected annotation is unresolved in a dependent platform module
- [`KT-25120`](https://youtrack.jetbrains.com/issue/KT-25120) RequireKotlin on nested class and its members is not loaded correctly
- [`KT-25273`](https://youtrack.jetbrains.com/issue/KT-25273) java.lang.UnsupportedOperationException from incremental JS compilation
- [`KT-25310`](https://youtrack.jetbrains.com/issue/KT-25310) Write isUnsigned flag into metadata for unsigned types


#### Frontend

- [`KT-15807`](https://youtrack.jetbrains.com/issue/KT-15807) @JvmField is not applicable to interface companion properties
- [`KT-16962`](https://youtrack.jetbrains.com/issue/KT-16962) Annotation classes cannot contain types or static fields because they cannot have a body
- [`KT-23153`](https://youtrack.jetbrains.com/issue/KT-23153) Compiler allows to set non constant value as annotation parameter
- [`KT-23362`](https://youtrack.jetbrains.com/issue/KT-23362) Move coroutines to package kolin.coroutines for 1.3
- [`KT-24861`](https://youtrack.jetbrains.com/issue/KT-24861) Fix loading kotlin.suspend compiled with LV=1.2 when release coroutines package is used
- [`KT-25241`](https://youtrack.jetbrains.com/issue/KT-25241) Kotlin compiler doesn't warn about usage of lambda/reference with more than 22 parameters
- [`KT-25600`](https://youtrack.jetbrains.com/issue/KT-25600) NSEE “Collection is empty” after trying to call default constructor of unsigned number class

#### Frontend. Data-flow analysis

- [`KT-22517`](https://youtrack.jetbrains.com/issue/KT-22517) Deprecate smartcasts for local delegated properties

#### Frontend. Declarations

- [`KT-19618`](https://youtrack.jetbrains.com/issue/KT-19618) Data class `copy()` call with optional parameters leads to broken code when the class implements an interface with `copy()`
- [`KT-23277`](https://youtrack.jetbrains.com/issue/KT-23277) Prohibit local annotation classes
- [`KT-24197`](https://youtrack.jetbrains.com/issue/KT-24197) Make 'mod' operator error in 1.3

#### Frontend. Lexer & Parser

- [`KT-24663`](https://youtrack.jetbrains.com/issue/KT-24663) Add 'UL' suffix to represent number literals of unsigned Long type

#### Frontend. Resolution and Inference

- [`KT-16908`](https://youtrack.jetbrains.com/issue/KT-16908) Support callable references to suspending functions
- [`KT-20588`](https://youtrack.jetbrains.com/issue/KT-20588) Report error on single element assignment to varargs in named form in annotations
- [`KT-20589`](https://youtrack.jetbrains.com/issue/KT-20589) Report error on single element assignment to varargs in named form in functions
- [`KT-24859`](https://youtrack.jetbrains.com/issue/KT-24859) Disallow calls of functions annotated with receiver annotated with @RestrictsSuspension in foreign suspension context

### IDE

- [`KT-25466`](https://youtrack.jetbrains.com/issue/KT-25466) Make coroutines resolve to be independent of language and API versions set in `Kotlin Compiler` settings

#### IDE. Inspections and Intentions

- [`KT-24243`](https://youtrack.jetbrains.com/issue/KT-24243) Support quick fix to enable usages of Experimental/UseExperimental
- [`KT-11154`](https://youtrack.jetbrains.com/issue/KT-11154) Spell checking inspection is not suppressable
- [`KT-25169`](https://youtrack.jetbrains.com/issue/KT-25169) Impossible to suppress UAST/JVM inspections

#### IDE. Libraries

- [`KT-25129`](https://youtrack.jetbrains.com/issue/KT-25129) Idea freezes when Kotlin plugin tries to determine if jar is js lib in jvm module


### Libraries

#### New Features

- [`KEEP-131`](https://github.com/Kotlin/KEEP/issues/131), [`KT-17261`](https://youtrack.jetbrains.com/issue/KT-17261) Add random number generator to stdlib and related collection extension functions
- [`KT-7922`](https://youtrack.jetbrains.com/issue/KT-7922) Companion object for Boolean
- [`KT-8247`](https://youtrack.jetbrains.com/issue/KT-8247) Byte size for primitives
- [`KT-16552`](https://youtrack.jetbrains.com/issue/KT-16552) Add Sequence.orEmpty()
- [`KT-18559`](https://youtrack.jetbrains.com/issue/KT-18559) Make SafeContinuation and context impl classes serializable
- [`KT-18910`](https://youtrack.jetbrains.com/issue/KT-18910) StringBuilder#setLength(0) in non-JVM Kotlin
- [`KT-21763`](https://youtrack.jetbrains.com/issue/KT-21763) Provide Char MIN_VALUE/MAX_VALUE constants
- [`KT-23279`](https://youtrack.jetbrains.com/issue/KT-23279) isNullOrEmpty() for collections, maps, and arrays
- [`KT-23602`](https://youtrack.jetbrains.com/issue/KT-23602) Make kotlin.Metadata public

#### Fixes

- [`KT-23564`](https://youtrack.jetbrains.com/issue/KT-23564) KotlinJS: Math is deprecated yet random is not a global function
- [`KT-24856`](https://youtrack.jetbrains.com/issue/KT-24856) Create compatibility wrappers for migration from 1.2 to 1.3 coroutines
- [`KT-24862`](https://youtrack.jetbrains.com/issue/KT-24862) Create new Continuation API for coroutines 

### Reflection

- [`KT-25541`](https://youtrack.jetbrains.com/issue/KT-25541) Incorrect parameter names in reflection for inner class constructor from Java class compiled with "-parameters"



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

## 1.1.60

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

### Compiler

#### Performance Improvements

- [`KT-20462`](https://youtrack.jetbrains.com/issue/KT-20462) Don't create an array copy for '*<array-constructor-fun>(...)'
#### Fixes

- [`KT-14697`](https://youtrack.jetbrains.com/issue/KT-14697) Use-site targeted annotation is not correctly loaded from class file
- [`KT-17680`](https://youtrack.jetbrains.com/issue/KT-17680) Android Studio and multiple tests in single file
- [`KT-19251`](https://youtrack.jetbrains.com/issue/KT-19251) Stack spilling in constructor arguments breaks Quasar
- [`KT-19592`](https://youtrack.jetbrains.com/issue/KT-19592) Apply JSR 305 default nullability qualifiers with to generic type arguments if they're applicable for TYPE_USE
- [`KT-20016`](https://youtrack.jetbrains.com/issue/KT-20016) JSR 305: default nullability qualifiers are ignored in TYPE_USE and PARAMETER positions
- [`KT-20131`](https://youtrack.jetbrains.com/issue/KT-20131) Support @NonNull(when = NEVER) nullability annotation
- [`KT-20158`](https://youtrack.jetbrains.com/issue/KT-20158) Preserve flexibility for Java types annotated with @NonNull(when = UNKNOWN)
- [`KT-20337`](https://youtrack.jetbrains.com/issue/KT-20337) No multifile class facade is generated for files with type aliases only
- [`KT-20387`](https://youtrack.jetbrains.com/issue/KT-20387) Wrong argument generated for accessor call of a protected generic 'operator fun get/set' from base class with primitive type as type parameter
- [`KT-20418`](https://youtrack.jetbrains.com/issue/KT-20418) Wrong code generated for literal long range with mixed integer literal ends
- [`KT-20491`](https://youtrack.jetbrains.com/issue/KT-20491) Incorrect synthetic accessor generated for a generic base class function specialized with primitive type
- [`KT-20651`](https://youtrack.jetbrains.com/issue/KT-20651) "Don't know how to generate outer expression" for enum-values with non-trivial self-closures
- [`KT-20707`](https://youtrack.jetbrains.com/issue/KT-20707) Support when by enum in kotlin scripts
- [`KT-20879`](https://youtrack.jetbrains.com/issue/KT-20879) Compiler problem in when-expressions

### IDE

#### New Features

- [`KT-14175`](https://youtrack.jetbrains.com/issue/KT-14175) Surround with try ... catch (... finally) doesn't work for expressions
- [`KT-15769`](https://youtrack.jetbrains.com/issue/KT-15769) Join lines could "convert to expression body"
- [`KT-19134`](https://youtrack.jetbrains.com/issue/KT-19134) IntelliJ Color Scheme editor - allow changing color of colons and double colons
- [`KT-20308`](https://youtrack.jetbrains.com/issue/KT-20308) New Gradle with Kotlin DSL project wizard
#### Fixes

- [`KT-15932`](https://youtrack.jetbrains.com/issue/KT-15932) Attempt to rename private property finds unrelated usages
- [`KT-18996`](https://youtrack.jetbrains.com/issue/KT-18996) After Kotlin compiler settings change: 'Apply' button doesn't work
- [`KT-19458`](https://youtrack.jetbrains.com/issue/KT-19458) Resolver for 'completion/highlighting in ScriptModuleInfo for build.gradle.kts / JVM' does not know how to resolve LibraryInfo
- [`KT-19474`](https://youtrack.jetbrains.com/issue/KT-19474) Kotlin Gradle Script: highlighting fails on unresolved references
- [`KT-19823`](https://youtrack.jetbrains.com/issue/KT-19823) Kotlin Gradle project import into IntelliJ: import kapt generated classes into classpath
- [`KT-19958`](https://youtrack.jetbrains.com/issue/KT-19958) Android: kotlinOptions from build.gradle are not imported into facet
- [`KT-19972`](https://youtrack.jetbrains.com/issue/KT-19972) AssertionError “Resolver for 'completion/highlighting in ModuleProductionSourceInfo(module=Module: 'kotlin-pure_main') for files dummy.kt for platform JVM' does not know how to resolve SdkInfo“ on copying Kotlin file with kotlin.* imports from other project
- [`KT-20112`](https://youtrack.jetbrains.com/issue/KT-20112) maven dependency type test-jar with scope compile not 
- [`KT-20185`](https://youtrack.jetbrains.com/issue/KT-20185) Stub and PSI element type mismatch for "var nullableSuspend: (suspend (P) -> Unit)? = null"
- [`KT-20199`](https://youtrack.jetbrains.com/issue/KT-20199) Cut action is not available during indexing
- [`KT-20331`](https://youtrack.jetbrains.com/issue/KT-20331) Wrong EAP repository
- [`KT-20346`](https://youtrack.jetbrains.com/issue/KT-20346) Can't build tests in common code due to missing org.jetbrains.kotlin:kotlin-test-js testCompile dependency in JS
- [`KT-20419`](https://youtrack.jetbrains.com/issue/KT-20419) Android Studio plugin 1.1.50 show multiple gutter icon for the same item
- [`KT-20519`](https://youtrack.jetbrains.com/issue/KT-20519) Error “Parameter specified as non-null is null: method ModuleGrouperKt.isQualifiedModuleNamesEnabled” on creating Gradle (Kotlin DSL) project from scratch
- [`KT-20550`](https://youtrack.jetbrains.com/issue/KT-20550) Spring: "Navigate to autowired candidates" gutter action is missed (IDEA 2017.3)
- [`KT-20566`](https://youtrack.jetbrains.com/issue/KT-20566) Spring: "Navigate to the spring beans declaration" gutter action for `@ComponentScan` is missed (IDEA 2017.3)
- [`KT-20621`](https://youtrack.jetbrains.com/issue/KT-20621) Provide automatic migration from JetRunConfigurationType to KotlinRunConfigurationType
- [`KT-20648`](https://youtrack.jetbrains.com/issue/KT-20648) Do we need a separate ProjectImportProvider for gradle kotlin dsl projects?
- [`KT-20782`](https://youtrack.jetbrains.com/issue/KT-20782) Non-atomic trees update
- [`KT-20789`](https://youtrack.jetbrains.com/issue/KT-20789) Can't navigate to inline call/inline use site when runner is delegated to Gradle
- [`KT-20843`](https://youtrack.jetbrains.com/issue/KT-20843) Kotlin TypeDeclarationProvider may stop other declarations providers execution
- [`KT-20929`](https://youtrack.jetbrains.com/issue/KT-20929) Import Project from Gradle wizard: the same page is shown twice

### IDE. Completion

- [`KT-16383`](https://youtrack.jetbrains.com/issue/KT-16383) IllegalStateException: Failed to create expression from text: '<init>' on choosing ByteArray from completion list
- [`KT-18458`](https://youtrack.jetbrains.com/issue/KT-18458) Spring: code completion does not suggest bean names inside `@Qualifier` before function parameter
- [`KT-20256`](https://youtrack.jetbrains.com/issue/KT-20256) java.lang.Throwable “Invalid range specified” on editing template inside string literal

### IDE. Inspections and Intentions

#### New Features

- [`KT-14695`](https://youtrack.jetbrains.com/issue/KT-14695) Simplify comparison intention produces meaningless statement for assert()
- [`KT-17204`](https://youtrack.jetbrains.com/issue/KT-17204) Add `Assign to property quickfix`
- [`KT-18220`](https://youtrack.jetbrains.com/issue/KT-18220) Add data modifier to a class quickfix
- [`KT-18742`](https://youtrack.jetbrains.com/issue/KT-18742) Add quick-fix for CANNOT_CHECK_FOR_ERASED
- [`KT-19735`](https://youtrack.jetbrains.com/issue/KT-19735) Add quickfix for type mismatch that converts Sequence/Array/List
- [`KT-20259`](https://youtrack.jetbrains.com/issue/KT-20259) Show warning if arrays are compared by '!='
#### Fixes

- [`KT-10546`](https://youtrack.jetbrains.com/issue/KT-10546) Wrong "Unused property" warning on using inline object syntax
- [`KT-16394`](https://youtrack.jetbrains.com/issue/KT-16394) "Convert reference to lambda" generates wrong code
- [`KT-16808`](https://youtrack.jetbrains.com/issue/KT-16808) Intention "Remove unnecessary parantheses" is erroneously proposed for elvis operator on LHS of `in` operator if RHS of elvis is return with value
- [`KT-17437`](https://youtrack.jetbrains.com/issue/KT-17437) Class highlighted as unused even if Companion methods/fields really used
- [`KT-19377`](https://youtrack.jetbrains.com/issue/KT-19377) Inspections are run for Kotlin Gradle DSL sources
- [`KT-19420`](https://youtrack.jetbrains.com/issue/KT-19420) Kotlin Gradle script editor: suggestion to import required class from stdlib fails with AE: ResolverForProjectImpl.descriptorForModule()
- [`KT-19626`](https://youtrack.jetbrains.com/issue/KT-19626) (Specify type explicitly) Descriptor was not found for VALUE_PARAMETER
- [`KT-19674`](https://youtrack.jetbrains.com/issue/KT-19674) 'Convert property initializer to getter' intention fails on incompilable initializer with AssertionError at SpecifyTypeExplicitlyIntention$Companion.addTypeAnnotationWithTemplate() 
- [`KT-19782`](https://youtrack.jetbrains.com/issue/KT-19782) Surround with if else doesn't work for expressions
- [`KT-20010`](https://youtrack.jetbrains.com/issue/KT-20010) 'Replace safe access expression with 'if' expression' IDEA Kotlin plugin intention may failed
- [`KT-20104`](https://youtrack.jetbrains.com/issue/KT-20104) "Recursive property accessor" reports false positive when property reference is used in the assignment
- [`KT-20218`](https://youtrack.jetbrains.com/issue/KT-20218) AE on calling intention “Convert to secondary constructor” for already referred argument 
- [`KT-20231`](https://youtrack.jetbrains.com/issue/KT-20231) False positive 'Redundant override' when delegated member hides super type override
- [`KT-20261`](https://youtrack.jetbrains.com/issue/KT-20261) Incorrect "Redundant Unit return type" inspection for Nothing-typed expression
- [`KT-20315`](https://youtrack.jetbrains.com/issue/KT-20315) "call chain on collection type may be simplified" generates code that does not compile
- [`KT-20333`](https://youtrack.jetbrains.com/issue/KT-20333) Assignment can be lifted out of try is applied too broadly
- [`KT-20366`](https://youtrack.jetbrains.com/issue/KT-20366) Code cleanup: some inspections are broken 
- [`KT-20369`](https://youtrack.jetbrains.com/issue/KT-20369) Inspection messages with INFORMATION highlight type are shown in Code Inspect
- [`KT-20409`](https://youtrack.jetbrains.com/issue/KT-20409) useless warning "Remove curly braces" for Chinese character
- [`KT-20417`](https://youtrack.jetbrains.com/issue/KT-20417) Converting property getter to block body doesn't insert explicit return type

### IDE. Refactorings

#### Performance Improvements

- [`KT-18823`](https://youtrack.jetbrains.com/issue/KT-18823) Move class to a separate file is very slow in 'kotlin' project
- [`KT-20205`](https://youtrack.jetbrains.com/issue/KT-20205) Invoke MoveKotlinDeclarationsProcessor.findUsages() under progress
#### Fixes

- [`KT-15840`](https://youtrack.jetbrains.com/issue/KT-15840) Introduce type alias: don't change not-nullable type with nullable typealias
- [`KT-17949`](https://youtrack.jetbrains.com/issue/KT-17949) Rename private fun should not search it out of scope
- [`KT-18196`](https://youtrack.jetbrains.com/issue/KT-18196) Refactor / Copy: the copy is formatted
- [`KT-18594`](https://youtrack.jetbrains.com/issue/KT-18594) Refactor / Extract (Functional) Parameter are available for annotation arguments, but fail with AE: "Body element is not found"
- [`KT-19439`](https://youtrack.jetbrains.com/issue/KT-19439) Kotlin introduce parameter causes exception
- [`KT-19909`](https://youtrack.jetbrains.com/issue/KT-19909) copy a kotlin class removes imports and other modifications
- [`KT-19949`](https://youtrack.jetbrains.com/issue/KT-19949) AssertionError „Resolver for 'project source roots and libraries for platform JVM' does not know how to resolve ModuleProductionSourceInfo“ through MoveConflictChecker.getModuleDescriptor() on copying Kotlin file from other project
- [`KT-20092`](https://youtrack.jetbrains.com/issue/KT-20092) Refactor / Copy: copy of .kt file removes all the blank lines and 'hanging' comments
- [`KT-20335`](https://youtrack.jetbrains.com/issue/KT-20335) Refactor → Extract Type Parameter: “AWT events are not allowed inside write action” after processing duplicates
- [`KT-20402`](https://youtrack.jetbrains.com/issue/KT-20402) Throwable “PsiElement(IDENTIFIER) by KotlinInplaceParameterIntroducer” on calling Refactor → Extract Parameter for default values
- [`KT-20403`](https://youtrack.jetbrains.com/issue/KT-20403) AE “Body element is not found” on calling Refactor → Extract Parameter for default values in constructor of class without body

### JavaScript

#### Fixes

- [`KT-8285`](https://youtrack.jetbrains.com/issue/KT-8285) JS: don't generate tmp when only need one component
- [`KT-14549`](https://youtrack.jetbrains.com/issue/KT-14549) JS: Non-local returns from secondary constructors don't work
- [`KT-15294`](https://youtrack.jetbrains.com/issue/KT-15294) JS: parse error in `js()` function
- [`KT-17450`](https://youtrack.jetbrains.com/issue/KT-17450) PlatformDependent members of collections are compiled in JS
- [`KT-18010`](https://youtrack.jetbrains.com/issue/KT-18010) JS: JsName annotation in interfaces can cause runtime exception
- [`KT-18063`](https://youtrack.jetbrains.com/issue/KT-18063) Inlining does not work properly in JS for suspend functions from another module
- [`KT-18548`](https://youtrack.jetbrains.com/issue/KT-18548) JS: wrong string interpolation with generic or Any parameters
- [`KT-19794`](https://youtrack.jetbrains.com/issue/KT-19794) runtime crash with empty object (Javascript) 
- [`KT-19818`](https://youtrack.jetbrains.com/issue/KT-19818) JS: generate paths relative to .map file by default (unless "-source-map-prefix" is used)
- [`KT-19906`](https://youtrack.jetbrains.com/issue/KT-19906) JS: rename compiler option "-source-map-source-roots" to avoid misleading since sourcemaps have field called "sourceRoot"
- [`KT-20287`](https://youtrack.jetbrains.com/issue/KT-20287) Functions don't actually return Unit in Kotlin-JS -> unexpected null problems vs JDK version
- [`KT-20451`](https://youtrack.jetbrains.com/issue/KT-20451) KotlinJs - interface function with default parameter, overridden by implementor, can't be found at runtime
- [`KT-20650`](https://youtrack.jetbrains.com/issue/KT-20650) JS: compiler crashes in Java 9 with NoClassDefFoundError
- [`KT-20653`](https://youtrack.jetbrains.com/issue/KT-20653) JS: compiler crashes in Java 9 with TranslationRuntimeException
- [`KT-20820`](https://youtrack.jetbrains.com/issue/KT-20820) JS: IDEA project doesn't generate paths relative to .map

### Libraries

- [`KT-20596`](https://youtrack.jetbrains.com/issue/KT-20596) 'synchronized' does not allow non-local return in Kotlin JS
- [`KT-20600`](https://youtrack.jetbrains.com/issue/KT-20600) Typo in POMs for kotlin-runtime

### Tools

- [`KT-19692`](https://youtrack.jetbrains.com/issue/KT-19692) kotlin-jpa plugin doesn't support @MappedSuperclass annotation
- [`KT-20030`](https://youtrack.jetbrains.com/issue/KT-20030) Parcelize can directly reference writeToParcel and CREATOR for final, non-Parcelize Parcelable types in same compilation unit.
- [`KT-19742`](https://youtrack.jetbrains.com/issue/KT-19742) [Android extensions] Calling clearFindViewByIdCache causes NPE
- [`KT-19749`](https://youtrack.jetbrains.com/issue/KT-19749) Android extensions + Parcelable: NoSuchMethodError on attempt to pack into parcel a serializable object
- [`KT-20026`](https://youtrack.jetbrains.com/issue/KT-20026) Parcelize overrides describeContents despite being already implemented.
- [`KT-20027`](https://youtrack.jetbrains.com/issue/KT-20027) Parcelize uses wrong classloader when reading parcelable type.
- [`KT-20029`](https://youtrack.jetbrains.com/issue/KT-20029) Parcelize should not directly reference parcel methods on types outside compilation unit
- [`KT-20032`](https://youtrack.jetbrains.com/issue/KT-20032) Parcelize does not respect type nullability in case of Parcelize parcelables

### Tools. Gradle

- [`KT-3463`](https://youtrack.jetbrains.com/issue/KT-3463) Gradle plugin ignores kotlin compile options changes
- [`KT-16299`](https://youtrack.jetbrains.com/issue/KT-16299) Gradle build does not recompile annotated classes on changing compiler's plugins configuration
- [`KT-16764`](https://youtrack.jetbrains.com/issue/KT-16764) Kotlin Gradle plugin should replicate task dependencies of Java source directories
- [`KT-17564`](https://youtrack.jetbrains.com/issue/KT-17564) Applying Kotlin's Gradle plugin results in src/main/java being listed twice in sourceSets.main.allSource
- [`KT-17674`](https://youtrack.jetbrains.com/issue/KT-17674) Test code is not compiled incrementally when main is changed
- [`KT-18765`](https://youtrack.jetbrains.com/issue/KT-18765) Move incremental compilation message from Gradle's warning to info logging level
- [`KT-20036`](https://youtrack.jetbrains.com/issue/KT-20036) Gradle tasks up-to-date-ness

### Tools. J2K

- [`KT-19565`](https://youtrack.jetbrains.com/issue/KT-19565) Java code using Iterator#remove converted to red code
- [`KT-19651`](https://youtrack.jetbrains.com/issue/KT-19651) Java class with static-only methods can contain 'protected' members

### Tools. JPS

- [`KT-20082`](https://youtrack.jetbrains.com/issue/KT-20082) Java 9: incremental build reports bogus error for reference to Kotlin source
- [`KT-20671`](https://youtrack.jetbrains.com/issue/KT-20671) Kotlin plugin compiler exception when compiling under JDK9

### Tools. Maven

- [`KT-20064`](https://youtrack.jetbrains.com/issue/KT-20064) Maven + Java 9: compile task warns about module-info in the output path
- [`KT-20400`](https://youtrack.jetbrains.com/issue/KT-20400) Do not output module name, version and related information by default in Maven builds

### Tools. REPL

- [`KT-20167`](https://youtrack.jetbrains.com/issue/KT-20167) JDK 9 `unresolved supertypes: Object` when working with Kotlin Scripting API

### Tools. kapt

- [`KT-17923`](https://youtrack.jetbrains.com/issue/KT-17923) Reference to Dagger generated class is highlighted red
- [`KT-18923`](https://youtrack.jetbrains.com/issue/KT-18923) Kapt: Do not use the Kotlin error message collector to issue errors from kapt
- [`KT-19097`](https://youtrack.jetbrains.com/issue/KT-19097) Request: Decent support of `kapt.kotlin.generated` on Intellij/Android Studio
- [`KT-20877`](https://youtrack.jetbrains.com/issue/KT-20877) Butterknife: UninitializedPropertyAccessException: "lateinit property has not been initialized" for field annotated with `@BindView`.

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

## 1.1.4-3

- [`KT-18062`](https://youtrack.jetbrains.com/issue/KT-18062) SamWithReceiver compiler plugin not used by IntelliJ for .kt files 
- [`KT-18497`](https://youtrack.jetbrains.com/issue/KT-18497) Gradle Kotlin Plugin does not work with the gradle java-library plugin 
- [`KT-19276`](https://youtrack.jetbrains.com/issue/KT-19276) Console spam when opening idea-community project in debug IDEA 
- [`KT-19433`](https://youtrack.jetbrains.com/issue/KT-19433) [Coroutines + Kapt3] Assertion failed in ClassClsStubBuilder.createNestedClassStub 
- [`KT-19680`](https://youtrack.jetbrains.com/issue/KT-19680) kapt3 & Parcelize: Compilation error 
- [`KT-19687`](https://youtrack.jetbrains.com/issue/KT-19687) Kotlin 1.1.4 noarg plugin breaks with sealed classes
- [`KT-19700`](https://youtrack.jetbrains.com/issue/KT-19700) Kapt error after updating to 1.1.4 - stub adds type parameters where there are none
- [`KT-19713`](https://youtrack.jetbrains.com/issue/KT-19713) Mocking of final named suspend methods with mockito fails
- [`KT-19729`](https://youtrack.jetbrains.com/issue/KT-19729) kapt3: not always including argument to @javax.inject.Named in generated stubs
- [`KT-19759`](https://youtrack.jetbrains.com/issue/KT-19759) "Convert to expression body" is not shown in 162 / AS23 branches for multi-liners 
- [`KT-19767`](https://youtrack.jetbrains.com/issue/KT-19767) NPE caused by Map<String, Boolean>?.get 
- [`KT-19769`](https://youtrack.jetbrains.com/issue/KT-19769) PerModulePackageCacheService calls getOrderEntriesForFile() for every file, even those that can't affect Kotlin resolve 
- [`KT-19774`](https://youtrack.jetbrains.com/issue/KT-19774) Provide an opt-out flag for separate classes directories (Gradle 4.0+) 
- [`KT-19847`](https://youtrack.jetbrains.com/issue/KT-19847) if an imported library already exists it should be redetected during gradle import 

## 1.1.4-2

- [`KT-19679`](https://youtrack.jetbrains.com/issue/KT-19679) CompilationException: Couldn't inline method call 'methodName' into... 
- [`KT-19690`](https://youtrack.jetbrains.com/issue/KT-19690) Lazy field in interface default method leads to ClassFormatError 
- [`KT-19716`](https://youtrack.jetbrains.com/issue/KT-19716) Quickdoc `Ctrl+Q` broken while browsing code completion list `Ctrl-Space`
- [`KT-19717`](https://youtrack.jetbrains.com/issue/KT-19717) Library kind incorrectly detected for vertx-web in Kotlin project 
- [`KT-19723`](https://youtrack.jetbrains.com/issue/KT-19723) "Insufficient maximum stack size" during compilation 

## 1.1.4

### Android

#### New Features

- [`KT-11048`](https://youtrack.jetbrains.com/issue/KT-11048) Android Extensions: cannot evaluate expression containing generated properties
#### Performance Improvements

- [`KT-10542`](https://youtrack.jetbrains.com/issue/KT-10542) Android Extensions: No cache for Views
- [`KT-18250`](https://youtrack.jetbrains.com/issue/KT-18250) Android Extensions: Allow to use SparseArray as a View cache
#### Fixes

- [`KT-11051`](https://youtrack.jetbrains.com/issue/KT-11051) Android Extensions: completion of generated properties is unclear for ambiguous ids
- [`KT-14086`](https://youtrack.jetbrains.com/issue/KT-14086) Android-extensions not generated using flavors dimension
- [`KT-14912`](https://youtrack.jetbrains.com/issue/KT-14912) Lint: "Code contains STOPSHIP marker" ignores suppress annotation
- [`KT-15164`](https://youtrack.jetbrains.com/issue/KT-15164) Kotlin Lint: problems in delegate expression are not reported
- [`KT-16934`](https://youtrack.jetbrains.com/issue/KT-16934) Android Extensions fails to compile when importing synthetic properties for layouts in other modules
- [`KT-17641`](https://youtrack.jetbrains.com/issue/KT-17641) Problem with Kotlin Android Extensions and Gradle syntax
- [`KT-17783`](https://youtrack.jetbrains.com/issue/KT-17783) Kotlin Lint: quick fixes to add inapplicable @RequiresApi and @SuppressLint make code incompilable
- [`KT-17786`](https://youtrack.jetbrains.com/issue/KT-17786) Kotlin Lint: "Surround with if()" quick fix is not suggested for single expression `get()`
- [`KT-17787`](https://youtrack.jetbrains.com/issue/KT-17787) Kotlin Lint: "Add @TargetApi" quick fix is not suggested for top level property accessor
- [`KT-17788`](https://youtrack.jetbrains.com/issue/KT-17788) Kotlin Lint: "Surround with if()" quick fix corrupts code in case of destructuring declaration
- [`KT-17890`](https://youtrack.jetbrains.com/issue/KT-17890) [kotlin-android-extensions] Renaming layout file does not rename import
- [`KT-18012`](https://youtrack.jetbrains.com/issue/KT-18012) Kotlin Android Extensions generates `@NotNull` properties for views present in a configuration and potentially missing in another
- [`KT-18545`](https://youtrack.jetbrains.com/issue/KT-18545) Accessing to synthetic properties on smart casted Android components crashed compiler

### Compiler

#### New Features

- [`KT-10942`](https://youtrack.jetbrains.com/issue/KT-10942) Support meta-annotations from JSR 305 for nullability qualifiers
- [`KT-14187`](https://youtrack.jetbrains.com/issue/KT-14187) Redundant "is" check is not detected
- [`KT-16603`](https://youtrack.jetbrains.com/issue/KT-16603) Support `inline suspend` function
- [`KT-17585`](https://youtrack.jetbrains.com/issue/KT-17585) Generate state machine for named functions in their bodies
#### Performance Improvements

- [`KT-3098`](https://youtrack.jetbrains.com/issue/KT-3098) Generate efficient comparisons
- [`KT-6247`](https://youtrack.jetbrains.com/issue/KT-6247) Optimization for 'in' and '..'
- [`KT-7571`](https://youtrack.jetbrains.com/issue/KT-7571) Don't box Double instance to call hashCode on Java 8
- [`KT-9900`](https://youtrack.jetbrains.com/issue/KT-9900) Optimize range operations for 'until' extension from stdlib
- [`KT-11959`](https://youtrack.jetbrains.com/issue/KT-11959) Unnceessary boxing/unboxing due to Comparable.compareTo
- [`KT-12158`](https://youtrack.jetbrains.com/issue/KT-12158) Optimize away boxing when comparing nullable primitive type value to primitive value
- [`KT-13682`](https://youtrack.jetbrains.com/issue/KT-13682) Reuse StringBuilder for concatenation and string interpolation
- [`KT-14323`](https://youtrack.jetbrains.com/issue/KT-14323) IntelliJ lockup when using Apache Spark UDF
- [`KT-14375`](https://youtrack.jetbrains.com/issue/KT-14375) Kotlin compiler failure with spark when creating a flexible type for scala.Function22
- [`KT-15235`](https://youtrack.jetbrains.com/issue/KT-15235) Escaped characters in template strings are generating inefficient implementations
- [`KT-17280`](https://youtrack.jetbrains.com/issue/KT-17280) Inline constant expressions in string templates
- [`KT-17903`](https://youtrack.jetbrains.com/issue/KT-17903) Generate 'for-in-indices' as a precondition loop
- [`KT-18157`](https://youtrack.jetbrains.com/issue/KT-18157) Optimize out trivial INSTANCEOF checks
- [`KT-18162`](https://youtrack.jetbrains.com/issue/KT-18162) Do not check nullability assertions twice for effectively same value
- [`KT-18164`](https://youtrack.jetbrains.com/issue/KT-18164) Do not check nullability for values that have been already checked with !!
- [`KT-18478`](https://youtrack.jetbrains.com/issue/KT-18478) Unnecessary nullification of bound variables
- [`KT-18558`](https://youtrack.jetbrains.com/issue/KT-18558) Flatten nested string concatenation
- [`KT-18777`](https://youtrack.jetbrains.com/issue/KT-18777) Unnecessary boolean negation generated for 'if (expr !in range)'
#### Fixes

- [`KT-1809`](https://youtrack.jetbrains.com/issue/KT-1809) Confusing diagnostics when wrong number of type arguments are specified and there are several callee candiates
- [`KT-2007`](https://youtrack.jetbrains.com/issue/KT-2007) Improve diagnostics when + in not resolved on a pair of nullable ints
- [`KT-5066`](https://youtrack.jetbrains.com/issue/KT-5066) Bad diagnostic message for ABSTRACT_MEMBER_NOT_IMPLEMENTED for (companion) object
- [`KT-5511`](https://youtrack.jetbrains.com/issue/KT-5511) Inconsistent handling of inner enum
- [`KT-7773`](https://youtrack.jetbrains.com/issue/KT-7773) Disallow to explicitly extend Enum<E> class
- [`KT-7975`](https://youtrack.jetbrains.com/issue/KT-7975) Unclear error message when redundant type arguments supplied
- [`KT-8340`](https://youtrack.jetbrains.com/issue/KT-8340) vararg in a property setter must be an error
- [`KT-8612`](https://youtrack.jetbrains.com/issue/KT-8612) Incorrect error message for var extension property without getter or setter
- [`KT-8829`](https://youtrack.jetbrains.com/issue/KT-8829) Type parameter of a class is not resolved in the constructor parameter's default value
- [`KT-8845`](https://youtrack.jetbrains.com/issue/KT-8845) Bogus diagnostic on infix operation "in"
- [`KT-9282`](https://youtrack.jetbrains.com/issue/KT-9282) Improve diagnostic on overload resolution ambiguity when a nullable argument is passed to non-null parameter
- [`KT-10045`](https://youtrack.jetbrains.com/issue/KT-10045) Not specific enough compiler error message in case of trying to call overloaded private methods
- [`KT-10164`](https://youtrack.jetbrains.com/issue/KT-10164) Incorrect error message for external inline method
- [`KT-10248`](https://youtrack.jetbrains.com/issue/KT-10248) Smart casts: Misleading error on overloaded function call
- [`KT-10657`](https://youtrack.jetbrains.com/issue/KT-10657) Confusing diagnostic when trying to invoke value as a function
- [`KT-10839`](https://youtrack.jetbrains.com/issue/KT-10839) Weird diagnostics on callable reference of unresolved class
- [`KT-11119`](https://youtrack.jetbrains.com/issue/KT-11119) Confusing error message when overloaded method is called on nullable receiver
- [`KT-12408`](https://youtrack.jetbrains.com/issue/KT-12408) Generic information lost for override values
- [`KT-12551`](https://youtrack.jetbrains.com/issue/KT-12551) Report "unused expression" on unused bound double colon expressions
- [`KT-13749`](https://youtrack.jetbrains.com/issue/KT-13749) Error highlighting range for no 'override' modifier is bigger than needed
- [`KT-14598`](https://youtrack.jetbrains.com/issue/KT-14598) Do not report "member is final and cannot be overridden" when overriding something from final class
- [`KT-14633`](https://youtrack.jetbrains.com/issue/KT-14633) "If must have both main and else branches" diagnostic range is too high
- [`KT-14647`](https://youtrack.jetbrains.com/issue/KT-14647) Confusing error message "'@receiver:' annotations could be applied only to extension function or extension property declarations"
- [`KT-14927`](https://youtrack.jetbrains.com/issue/KT-14927) TCE in QualifiedExpressionResolver
- [`KT-15243`](https://youtrack.jetbrains.com/issue/KT-15243) Report deprecation on usages of type alias expanded to a deprecated class
- [`KT-15804`](https://youtrack.jetbrains.com/issue/KT-15804) Prohibit having duplicate parameter names in functional types
- [`KT-15810`](https://youtrack.jetbrains.com/issue/KT-15810) destructuring declarations don't work in scripts on the top level
- [`KT-15931`](https://youtrack.jetbrains.com/issue/KT-15931) IllegalStateException: ClassDescriptor of superType should not be null: T by a
- [`KT-16016`](https://youtrack.jetbrains.com/issue/KT-16016) Compiler failure with NO_EXPECTED_TYPE
- [`KT-16448`](https://youtrack.jetbrains.com/issue/KT-16448) Inline suspend functions with inlined suspend invocations are miscompiled (VerifyError, ClassNotFound)
- [`KT-16576`](https://youtrack.jetbrains.com/issue/KT-16576) Wrong code generated with skynet benchmark
- [`KT-17007`](https://youtrack.jetbrains.com/issue/KT-17007) Kotlin is not optimizing away unreachable code based on const vals
- [`KT-17188`](https://youtrack.jetbrains.com/issue/KT-17188) Do not propose to specify constructor invocation for classes without an accessible constructor
- [`KT-17611`](https://youtrack.jetbrains.com/issue/KT-17611) Unnecessary "Name shadowed" warning on parameter of local function or local class member
- [`KT-17692`](https://youtrack.jetbrains.com/issue/KT-17692) NPE in compiler when calling KClass.java on function result of type Unit
- [`KT-17820`](https://youtrack.jetbrains.com/issue/KT-17820) False "useless cast" when target type is flexible
- [`KT-17972`](https://youtrack.jetbrains.com/issue/KT-17972) Anonymous class generated from lambda captures its outer and tries to set nonexistent this$0 field.
- [`KT-18029`](https://youtrack.jetbrains.com/issue/KT-18029) typealias not working in .kts files
- [`KT-18085`](https://youtrack.jetbrains.com/issue/KT-18085) Compilation Error:Kotlin: [Internal Error] kotlin.TypeCastException: null cannot be cast to non-null type com.intellij.psi.PsiElement
- [`KT-18115`](https://youtrack.jetbrains.com/issue/KT-18115) Generic inherited classes in different packages with coroutine causes java.lang.VerifyError: Bad local variable type
- [`KT-18189`](https://youtrack.jetbrains.com/issue/KT-18189) Incorrect generic signature generated for implementation methods overriding special built-ins
- [`KT-18234`](https://youtrack.jetbrains.com/issue/KT-18234) Top-level variables in script aren't local variables
- [`KT-18413`](https://youtrack.jetbrains.com/issue/KT-18413) Strange compiler error - probably incremental compiler
- [`KT-18486`](https://youtrack.jetbrains.com/issue/KT-18486) Superfluos generation of suspend function state-machine because of inner suspension of different coroutine
- [`KT-18598`](https://youtrack.jetbrains.com/issue/KT-18598) Report error on access to declarations from non-exported packages and from inaccessible modules on Java 9
- [`KT-18698`](https://youtrack.jetbrains.com/issue/KT-18698) java.lang.IllegalStateException: resolveToInstruction: incorrect index -1 for label L12 in subroutine
- [`KT-18702`](https://youtrack.jetbrains.com/issue/KT-18702) Proguard warning with Kotlin 1.2-M1
- [`KT-18728`](https://youtrack.jetbrains.com/issue/KT-18728) Integer method reference application fails with CompilationException: Back-end (JVM) Internal error
- [`KT-18845`](https://youtrack.jetbrains.com/issue/KT-18845) Exception on building gradle project with collection literals
- [`KT-18867`](https://youtrack.jetbrains.com/issue/KT-18867) Getting constant "VerifyError: Operand stack underflow" from Kotlin plugin
- [`KT-18916`](https://youtrack.jetbrains.com/issue/KT-18916) Strange bytecode generated for 'null' passed as SAM adapter for Java interface
- [`KT-18983`](https://youtrack.jetbrains.com/issue/KT-18983) Coroutines: miscompiled suspend for loop (local variables are not spilled around suspension points)
- [`KT-19175`](https://youtrack.jetbrains.com/issue/KT-19175) Compiler generates different bytecode when classes are compiled separately or together
- [`KT-19246`](https://youtrack.jetbrains.com/issue/KT-19246) Using generic inline function inside inline extension function throws java.lang.VerifyError: Bad return type
- [`KT-19419`](https://youtrack.jetbrains.com/issue/KT-19419) Support JSR 305 meta-annotations in libraries even when JSR 305 JAR is not on the classpath

### IDE

#### New Features

- [`KT-2638`](https://youtrack.jetbrains.com/issue/KT-2638) Inline property (with accessors) refactoring
- [`KT-7107`](https://youtrack.jetbrains.com/issue/KT-7107) Rename refactoring for labels
- [`KT-9818`](https://youtrack.jetbrains.com/issue/KT-9818) Code style for method expression bodies
- [`KT-11994`](https://youtrack.jetbrains.com/issue/KT-11994) Data flow analysis support for Kotlin in IntelliJ
- [`KT-14126`](https://youtrack.jetbrains.com/issue/KT-14126) Code style wrapping options for enum constants
- [`KT-14929`](https://youtrack.jetbrains.com/issue/KT-14929) Deprecated ReplaceWith for type aliases
- [`KT-14950`](https://youtrack.jetbrains.com/issue/KT-14950) Code Style: Wrapping and Braces / "Local variable annotations" setting could be supported
- [`KT-14965`](https://youtrack.jetbrains.com/issue/KT-14965) "Configure Kotlin in project" should support build.gradle.kts
- [`KT-15504`](https://youtrack.jetbrains.com/issue/KT-15504) Add code style options to limit number of blank lines
- [`KT-16558`](https://youtrack.jetbrains.com/issue/KT-16558) Code Style: Add Options for "Spaces Before Parentheses"
- [`KT-18113`](https://youtrack.jetbrains.com/issue/KT-18113) Add new line options to code style for method parameters
- [`KT-18605`](https://youtrack.jetbrains.com/issue/KT-18605) Option to not use continuation indent in chained calls
- [`KT-18607`](https://youtrack.jetbrains.com/issue/KT-18607) Options to put blank lines between 'when' branches
#### Performance Improvements

- [`KT-14606`](https://youtrack.jetbrains.com/issue/KT-14606) Code completion calculates decompiled text when building lookup elements for PSI from compiled classes
- [`KT-17751`](https://youtrack.jetbrains.com/issue/KT-17751) Kotlin slows down java inspections big time
- [`KT-17835`](https://youtrack.jetbrains.com/issue/KT-17835) 10s hang on IDEA project open
- [`KT-18842`](https://youtrack.jetbrains.com/issue/KT-18842) Very slow typing in certain files of Kotlin project
- [`KT-18921`](https://youtrack.jetbrains.com/issue/KT-18921) Configure library kind explicitly
#### Fixes

- [`KT-6610`](https://youtrack.jetbrains.com/issue/KT-6610) Language injection doesn't work with String Interpolation
- [`KT-8893`](https://youtrack.jetbrains.com/issue/KT-8893) Quick documentation shows type for top-level object-type elements, but "no name provided" for local ones
- [`KT-9359`](https://youtrack.jetbrains.com/issue/KT-9359) "Accidental override" error message does not mention class (type) names
- [`KT-10736`](https://youtrack.jetbrains.com/issue/KT-10736) Highlighting usages doesn't work for synthetic properties created by the Android Extensions
- [`KT-11980`](https://youtrack.jetbrains.com/issue/KT-11980) Spring: Generate Constructor, Setter Dependency in XML for Kotlin class: IOE at LightElement.add()
- [`KT-12123`](https://youtrack.jetbrains.com/issue/KT-12123) Formatter: always indent after newline in variable initialization
- [`KT-12910`](https://youtrack.jetbrains.com/issue/KT-12910) spring: create init-method/destroy-method from usage results in IOE
- [`KT-13072`](https://youtrack.jetbrains.com/issue/KT-13072) Kotlin struggles to index JDK 9 classes
- [`KT-13099`](https://youtrack.jetbrains.com/issue/KT-13099) formatting in angle brackets ignored and not fixed
- [`KT-14083`](https://youtrack.jetbrains.com/issue/KT-14083) Formatting of where clasuses
- [`KT-14271`](https://youtrack.jetbrains.com/issue/KT-14271) Value captured in closure doesn't always get highlighted
- [`KT-14561`](https://youtrack.jetbrains.com/issue/KT-14561) Use regular indent for the primary constructor parameters
- [`KT-14974`](https://youtrack.jetbrains.com/issue/KT-14974) "Find Usages" hangs in ExpressionsOfTypeProcessor
- [`KT-15093`](https://youtrack.jetbrains.com/issue/KT-15093) Navigation to library may not work if there's another module in same project that references same jar via a different library
- [`KT-15270`](https://youtrack.jetbrains.com/issue/KT-15270) Quickfix to migrate from @native***
- [`KT-16352`](https://youtrack.jetbrains.com/issue/KT-16352) Create from usage inserts extra space in first step
- [`KT-16725`](https://youtrack.jetbrains.com/issue/KT-16725) Formatter does not fix spaces before square brackets
- [`KT-16999`](https://youtrack.jetbrains.com/issue/KT-16999) "Parameter info" shows duplicates on toString
- [`KT-17357`](https://youtrack.jetbrains.com/issue/KT-17357) BuiltIns for module build with project LV settings, not with facet module settings
- [`KT-17394`](https://youtrack.jetbrains.com/issue/KT-17394) Core formatting is wrong for expression body properties
- [`KT-17759`](https://youtrack.jetbrains.com/issue/KT-17759) Breakpoints not working in JS
- [`KT-17771`](https://youtrack.jetbrains.com/issue/KT-17771) Kotlin IntelliJ plugin should resolve Gradle script classpath asynchronously
- [`KT-17818`](https://youtrack.jetbrains.com/issue/KT-17818) Formatting of long constructors is inconsistent with Kotlin code conventions
- [`KT-17849`](https://youtrack.jetbrains.com/issue/KT-17849) Automatically insert trimMargin() or trimIndent() on enter in multi-line strings
- [`KT-17855`](https://youtrack.jetbrains.com/issue/KT-17855) Main function is shown as unused
- [`KT-17894`](https://youtrack.jetbrains.com/issue/KT-17894) String `trimIndent` support inserts wrong indent in some cases
- [`KT-17942`](https://youtrack.jetbrains.com/issue/KT-17942) Enter in multiline string with injection doesn't add a proper indent
- [`KT-17956`](https://youtrack.jetbrains.com/issue/KT-17956) Type hints for properties that only consist of constructor calls don't add much value
- [`KT-18006`](https://youtrack.jetbrains.com/issue/KT-18006) Copying part of string literal with escape sequences converts this sequences to special characters
- [`KT-18030`](https://youtrack.jetbrains.com/issue/KT-18030) Parameters hints: `kotlin.arrayOf(elements)` should be on the blacklist by default
- [`KT-18059`](https://youtrack.jetbrains.com/issue/KT-18059) Kotlin Lint: False positive error "requires api level 24" for interface method with body
- [`KT-18149`](https://youtrack.jetbrains.com/issue/KT-18149) PIEAE "Element class CompositeElement of type REFERENCE_EXPRESSION (class KtNameReferenceExpressionElementType)" at PsiInvalidElementAccessException.createByNode()
- [`KT-18151`](https://youtrack.jetbrains.com/issue/KT-18151) Do not import jdkHome from Gradle/Maven model
- [`KT-18158`](https://youtrack.jetbrains.com/issue/KT-18158) Expand selection should select the comment after expression getter on the same line
- [`KT-18186`](https://youtrack.jetbrains.com/issue/KT-18186) Create function from usage should infer expected return type
- [`KT-18221`](https://youtrack.jetbrains.com/issue/KT-18221) AE at org.jetbrains.kotlin.analyzer.ResolverForProjectImpl.descriptorForModule
- [`KT-18269`](https://youtrack.jetbrains.com/issue/KT-18269) Find Usages fails to find operator-style usages of `invoke()` defined as extension
- [`KT-18298`](https://youtrack.jetbrains.com/issue/KT-18298) spring: strange menu at "Navige to the spring bean" gutter
- [`KT-18309`](https://youtrack.jetbrains.com/issue/KT-18309) Join lines breaks code
- [`KT-18373`](https://youtrack.jetbrains.com/issue/KT-18373) Facet: can't change target platform between JVM versions
- [`KT-18376`](https://youtrack.jetbrains.com/issue/KT-18376) Maven import fails with NPE at ArgumentUtils.convertArgumentsToStringList() if `jvmTarget` setting is absent
- [`KT-18418`](https://youtrack.jetbrains.com/issue/KT-18418) Generate equals and hashCode should be available for classes without properties
- [`KT-18429`](https://youtrack.jetbrains.com/issue/KT-18429) Android strings resources folding false positives
- [`KT-18444`](https://youtrack.jetbrains.com/issue/KT-18444) Type hints don't work for destructuring declarations
- [`KT-18475`](https://youtrack.jetbrains.com/issue/KT-18475) Gradle/IntelliJ sync can result in IntelliJ modules getting gradle artifacts added to the classpath, breaking compilation
- [`KT-18479`](https://youtrack.jetbrains.com/issue/KT-18479) Can't find usages of invoke operator with vararg parameter
- [`KT-18501`](https://youtrack.jetbrains.com/issue/KT-18501) Quick Documentation doesn't show when @Supress("unused") is above the javadoc
- [`KT-18566`](https://youtrack.jetbrains.com/issue/KT-18566) Long find usages for operators when there are several operators for the same type
- [`KT-18596`](https://youtrack.jetbrains.com/issue/KT-18596) "Generate hashCode" produces poorly formatted code
- [`KT-18725`](https://youtrack.jetbrains.com/issue/KT-18725) Android: `kotlin-language` facet disappears on reopening the project
- [`KT-18974`](https://youtrack.jetbrains.com/issue/KT-18974) Type hints shouldn't appear for negative literals
- [`KT-19054`](https://youtrack.jetbrains.com/issue/KT-19054) Lags in typing in string literal
- [`KT-19062`](https://youtrack.jetbrains.com/issue/KT-19062) Member navigation doesn't work in expression bodies of getters with inferred property type
- [`KT-19210`](https://youtrack.jetbrains.com/issue/KT-19210) Command line flags like -Xload-jsr305-annotations have no effect in IDE
- [`KT-19303`](https://youtrack.jetbrains.com/issue/KT-19303) Project language version settings are used to analyze libraries, disabling module-specific analysis flags like -Xjsr305-annotations

### IDE. Completion

- [`KT-8208`](https://youtrack.jetbrains.com/issue/KT-8208) Support static member completion with not-imported-yet classes
- [`KT-12104`](https://youtrack.jetbrains.com/issue/KT-12104) Smart completion does not work with "invoke" when receiver is expression
- [`KT-17074`](https://youtrack.jetbrains.com/issue/KT-17074) Incorrect autocomplete suggestions for contexts affected by @DslMarker
- [`KT-18443`](https://youtrack.jetbrains.com/issue/KT-18443) IntelliJ not handling default constructor argument from companion object well
- [`KT-19191`](https://youtrack.jetbrains.com/issue/KT-19191) Disable completion binding context caching by default

### IDE. Debugger

- [`KT-14845`](https://youtrack.jetbrains.com/issue/KT-14845) Evaluate expression freezes debugger while evaluating filter, for time proportional to number of elements in collection.
- [`KT-17120`](https://youtrack.jetbrains.com/issue/KT-17120) Evaluate expression: cannot find local variable
- [`KT-18453`](https://youtrack.jetbrains.com/issue/KT-18453) Support 'Step over' and 'Force step over' action for suspended calls
- [`KT-18577`](https://youtrack.jetbrains.com/issue/KT-18577) Debug: Smart Step Into does not enter functions passed as variable or parameter: "Method invoke() has not been called"
- [`KT-18632`](https://youtrack.jetbrains.com/issue/KT-18632) Debug: Smart Step Into does not enter functions passed as variable or parameter when signature of lambda and parameter doesn't match
- [`KT-18949`](https://youtrack.jetbrains.com/issue/KT-18949) Can't stop on breakpoint after call to inline in Android Studio
- [`KT-19403`](https://youtrack.jetbrains.com/issue/KT-19403) 30s complete hangs of application on breakpoints stop attempt

### IDE. Inspections and Intentions

#### New Features

- [`KT-12119`](https://youtrack.jetbrains.com/issue/KT-12119) Intention to replace .addAll() on a mutable collection with +=
- [`KT-13436`](https://youtrack.jetbrains.com/issue/KT-13436) Replace 'when' with return: handle case when all branches jump out (return Nothing)
- [`KT-13458`](https://youtrack.jetbrains.com/issue/KT-13458) Cascade "replace with return" for if/when expressions
- [`KT-13676`](https://youtrack.jetbrains.com/issue/KT-13676) Add better quickfix for 'let' and 'error  'only not null or asserted calls are allowed'
- [`KT-14648`](https://youtrack.jetbrains.com/issue/KT-14648) Add quickfix for @receiver annotation being applied to extension member instead of extension type
- [`KT-14799`](https://youtrack.jetbrains.com/issue/KT-14799) Add inspection to simplify successive null checks into safe-call and null check
- [`KT-14900`](https://youtrack.jetbrains.com/issue/KT-14900) "Lift return out of when/if" should work with control flow expressions
- [`KT-15257`](https://youtrack.jetbrains.com/issue/KT-15257) JS: quickfix to migrate from @native to external
- [`KT-15368`](https://youtrack.jetbrains.com/issue/KT-15368) Add intention to convert Boolean? == true to ?: false and vice versa
- [`KT-15893`](https://youtrack.jetbrains.com/issue/KT-15893) "Array property in data class" inspection could have a quick fix to generate `equals()` and `hashcode()`
- [`KT-15958`](https://youtrack.jetbrains.com/issue/KT-15958) Inspection to inline "unnecessary" variables
- [`KT-16063`](https://youtrack.jetbrains.com/issue/KT-16063) Inspection to suggest converting block body to expression body
- [`KT-17198`](https://youtrack.jetbrains.com/issue/KT-17198) Inspection to replace filter calls followed by functions with a predicate variant
- [`KT-17580`](https://youtrack.jetbrains.com/issue/KT-17580) Add remaning branches intention should be available for sealed classes
- [`KT-17583`](https://youtrack.jetbrains.com/issue/KT-17583) Support "Declaration access can be weaker"  inspection for kotlin properties
- [`KT-17815`](https://youtrack.jetbrains.com/issue/KT-17815) Quick-fix "Replace with safe call & elvis"
- [`KT-17842`](https://youtrack.jetbrains.com/issue/KT-17842) Add quick-fix for NO_CONSTRUCTOR error
- [`KT-17895`](https://youtrack.jetbrains.com/issue/KT-17895) Inspection to replace 'a .. b-1' with 'a until b'
- [`KT-17919`](https://youtrack.jetbrains.com/issue/KT-17919) Add "Simplify if" intention/inspection
- [`KT-17920`](https://youtrack.jetbrains.com/issue/KT-17920) Add intention/inspection removing redundant spread operator for arrayOf call
- [`KT-17970`](https://youtrack.jetbrains.com/issue/KT-17970) Intention actions to format parameter/argument list placing each on separate line
- [`KT-18236`](https://youtrack.jetbrains.com/issue/KT-18236) Add inspection for potentially wrongly placed unary operators
- [`KT-18274`](https://youtrack.jetbrains.com/issue/KT-18274) Add inspection to replace map+joinTo with joinTo(transform)
- [`KT-18386`](https://youtrack.jetbrains.com/issue/KT-18386) Inspection to detect safe calls of orEmpty()
- [`KT-18438`](https://youtrack.jetbrains.com/issue/KT-18438) Add inspection for empty ranges with start > endInclusive
- [`KT-18460`](https://youtrack.jetbrains.com/issue/KT-18460) Add intentions to apply De Morgan's laws to conditions
- [`KT-18516`](https://youtrack.jetbrains.com/issue/KT-18516) Add inspection to detect & remove redundant Unit
- [`KT-18517`](https://youtrack.jetbrains.com/issue/KT-18517) Provide "Remove explicit type" inspection for some obvious cases
- [`KT-18534`](https://youtrack.jetbrains.com/issue/KT-18534) Quick-fix to add empty brackets after primary constructor
- [`KT-18540`](https://youtrack.jetbrains.com/issue/KT-18540) Add quickfix to create data class property from usage in destructuring declaration
- [`KT-18615`](https://youtrack.jetbrains.com/issue/KT-18615) Inspection to replace if with three or more options with when
- [`KT-18749`](https://youtrack.jetbrains.com/issue/KT-18749) Inspection for useless operations on collection with not-null elements
- [`KT-18830`](https://youtrack.jetbrains.com/issue/KT-18830) "Lift return out of try"
#### Fixes

- [`KT-11906`](https://youtrack.jetbrains.com/issue/KT-11906) Spring: "Create getter / setter" quick fixes cause IOE at LightElement.add()
- [`KT-12524`](https://youtrack.jetbrains.com/issue/KT-12524) Wrong "redundant semicolon" for semicolon inside an enum class before the companion object declaration
- [`KT-13870`](https://youtrack.jetbrains.com/issue/KT-13870) Wrong caption "Change to property access" for Quick Fix to convert class instantiation to object reference
- [`KT-13886`](https://youtrack.jetbrains.com/issue/KT-13886) Unused variable intention should remove constant initializer
- [`KT-14092`](https://youtrack.jetbrains.com/issue/KT-14092) "Make <modifier>" intention inserts modifier between annotation and class keywords
- [`KT-14093`](https://youtrack.jetbrains.com/issue/KT-14093) "Make <modifier>" intention available only on modifier when declaration already have a visibility modifier
- [`KT-14643`](https://youtrack.jetbrains.com/issue/KT-14643) "Add non-null asserted call" quickfix should not be offered on literal null constants
- [`KT-15242`](https://youtrack.jetbrains.com/issue/KT-15242) Create type from usage should include constraints into base types
- [`KT-16046`](https://youtrack.jetbrains.com/issue/KT-16046) Globally unused typealias is not marked as such
- [`KT-16069`](https://youtrack.jetbrains.com/issue/KT-16069) "Simplify if statement" doesn't work in specific case
- [`KT-17026`](https://youtrack.jetbrains.com/issue/KT-17026) "Replace explicit parameter" should not be shown on destructuring declaration
- [`KT-17092`](https://youtrack.jetbrains.com/issue/KT-17092) Create function from usage works incorrectly with ::class expression
- [`KT-17353`](https://youtrack.jetbrains.com/issue/KT-17353) "Create type parameter from usage" should not be offered for unresolved annotations
- [`KT-17537`](https://youtrack.jetbrains.com/issue/KT-17537) Create from Usage should suggest Boolean return type if function is used in if condition
- [`KT-17623`](https://youtrack.jetbrains.com/issue/KT-17623) "Remove explicit type arguments" is too conservative sometimes
- [`KT-17651`](https://youtrack.jetbrains.com/issue/KT-17651) Create property from usage should make lateinit var
- [`KT-17726`](https://youtrack.jetbrains.com/issue/KT-17726) Nullability quick-fixes operate incorrectly with implicit nullable receiver
- [`KT-17740`](https://youtrack.jetbrains.com/issue/KT-17740) CME at MakeOverriddenMemberOpenFix.getText()
- [`KT-18506`](https://youtrack.jetbrains.com/issue/KT-18506) Inspection on final Kotlin spring components is false positive
- [`KT-17823`](https://youtrack.jetbrains.com/issue/KT-17823) Intention "Make private" and friends should respect modifier order
- [`KT-17917`](https://youtrack.jetbrains.com/issue/KT-17917) Superfluos suggestion to add replaceWith for DeprecationLevel.HIDDEN
- [`KT-17954`](https://youtrack.jetbrains.com/issue/KT-17954) Setting error severity on "Kotlin | Function or property has platform type" does not show up as error in IDE
- [`KT-17996`](https://youtrack.jetbrains.com/issue/KT-17996) Android Studio Default Constructor Command Removes Custom Setter
- [`KT-18033`](https://youtrack.jetbrains.com/issue/KT-18033) Do not suggest to cast expression to non-nullable type when it's the same as !!
- [`KT-18035`](https://youtrack.jetbrains.com/issue/KT-18035) Quickfix for "CanBePrimaryConstructorProperty" does not work correctly with vararg constructor properties
- [`KT-18044`](https://youtrack.jetbrains.com/issue/KT-18044) "Move to class body" intention: better placement in the body
- [`KT-18074`](https://youtrack.jetbrains.com/issue/KT-18074) Suggestion in Intention 'Specify return type explicitly' doesn't support generic type parameter
- [`KT-18120`](https://youtrack.jetbrains.com/issue/KT-18120) Recursive property accessor gives false positives
- [`KT-18148`](https://youtrack.jetbrains.com/issue/KT-18148) Incorrect, not working quickfix - final and can't be overridden
- [`KT-18160`](https://youtrack.jetbrains.com/issue/KT-18160) Circular autofix actions between redundant modality and non-final variable with allopen plugin
- [`KT-18194`](https://youtrack.jetbrains.com/issue/KT-18194) "Protected in final" inspection works incorrectly with all-open
- [`KT-18195`](https://youtrack.jetbrains.com/issue/KT-18195) "Redundant modality" is not reported with all-open
- [`KT-18197`](https://youtrack.jetbrains.com/issue/KT-18197) Redundant "make open" for abstract class member with all-open
- [`KT-18253`](https://youtrack.jetbrains.com/issue/KT-18253) Wrong location of "Redundant 'toString()' call in string template" quickfix
- [`KT-18347`](https://youtrack.jetbrains.com/issue/KT-18347) Nullability quickfixes are not helpful when using invoke operator
- [`KT-18368`](https://youtrack.jetbrains.com/issue/KT-18368) "Cast expression x to Type" fails for expression inside argument list
- [`KT-18375`](https://youtrack.jetbrains.com/issue/KT-18375) Backticked function name is suggested to be renamed to the same name
- [`KT-18385`](https://youtrack.jetbrains.com/issue/KT-18385) Spring: Generate Dependency causes Throwable "AWT events are not allowed inside write action"
- [`KT-18407`](https://youtrack.jetbrains.com/issue/KT-18407) "Move property to constructor" action should not appear on properties declared in interfaces
- [`KT-18425`](https://youtrack.jetbrains.com/issue/KT-18425) Make <modifier> intention inserts modifier at wrong position for sealed class
- [`KT-18529`](https://youtrack.jetbrains.com/issue/KT-18529) Add '!!' quick fix applies to wrong expression on operation 'in'
- [`KT-18642`](https://youtrack.jetbrains.com/issue/KT-18642) Remove unused parameter intention transfers default value to another parameter
- [`KT-18683`](https://youtrack.jetbrains.com/issue/KT-18683) Wrong 'equals' is generated for Kotlin JS project
- [`KT-18709`](https://youtrack.jetbrains.com/issue/KT-18709) "Lift assignment out of if" changes semantics
- [`KT-18711`](https://youtrack.jetbrains.com/issue/KT-18711) "Lift return out of when" changes semantics for functional type
- [`KT-18717`](https://youtrack.jetbrains.com/issue/KT-18717) Report MemberVisibilityCanBePrivate on visibility modifier if present
- [`KT-18722`](https://youtrack.jetbrains.com/issue/KT-18722) Correct "before" sample in description for intention Convert to enum class
- [`KT-18723`](https://youtrack.jetbrains.com/issue/KT-18723) Correct "after" sample for intention Convert to apply
- [`KT-18852`](https://youtrack.jetbrains.com/issue/KT-18852) "Lift return out of when" does not work for exhaustive when without else
- [`KT-18928`](https://youtrack.jetbrains.com/issue/KT-18928) In IDE, "Replace 'if' expression with safe access expression incorrectly replace expression when using property
- [`KT-18954`](https://youtrack.jetbrains.com/issue/KT-18954) Kotlin plugin updater activates in headless mode
- [`KT-18970`](https://youtrack.jetbrains.com/issue/KT-18970) Do not report "property can be private" on JvmField properties
- [`KT-19232`](https://youtrack.jetbrains.com/issue/KT-19232) Replace Math.min with coerceAtMost intention is broken
- [`KT-19272`](https://youtrack.jetbrains.com/issue/KT-19272) Do not report "function can be private" on JUnit 3 test methods

### IDE. Refactorings

#### New Features

- [`KT-4379`](https://youtrack.jetbrains.com/issue/KT-4379) Support renaming import alias
- [`KT-8180`](https://youtrack.jetbrains.com/issue/KT-8180) Copy Class
- [`KT-17547`](https://youtrack.jetbrains.com/issue/KT-17547) Refactor / Move: Problems Detected / Conflicts in View: only referencing file is mentioned
#### Fixes

- [`KT-9054`](https://youtrack.jetbrains.com/issue/KT-9054) Copy / pasting a Kotlin file should bring up the Copy Class dialog
- [`KT-13437`](https://youtrack.jetbrains.com/issue/KT-13437) Change signature replaces return type with Unit when it's not requested
- [`KT-15859`](https://youtrack.jetbrains.com/issue/KT-15859) Renaming variables or functions with backticks removes the backticks
- [`KT-16180`](https://youtrack.jetbrains.com/issue/KT-16180) Opened decompiled editor blocks refactoring of involved element
- [`KT-17062`](https://youtrack.jetbrains.com/issue/KT-17062) Field/property inline refactoring works incorrectly with Kotlin & Java usages
- [`KT-17128`](https://youtrack.jetbrains.com/issue/KT-17128) Refactor / Rename in the last position of label name throws Throwable "PsiElement(IDENTIFIER) by com.intellij.refactoring.rename.inplace.MemberInplaceRenamer" at InplaceRefactoring.buildTemplateAndStart()
- [`KT-17489`](https://youtrack.jetbrains.com/issue/KT-17489) Refactor / Inline Property: cannot inline val with the following plusAssign
- [`KT-17571`](https://youtrack.jetbrains.com/issue/KT-17571) Refactor / Move warns about using private/internal class from Java, but this is not related to the move
- [`KT-17622`](https://youtrack.jetbrains.com/issue/KT-17622) Refactor / Inline Function loses type arguments
- [`KT-18034`](https://youtrack.jetbrains.com/issue/KT-18034) Copy Class refactoring replaces all usages of the class with the new one!
- [`KT-18076`](https://youtrack.jetbrains.com/issue/KT-18076) Refactor / Rename on alias of Java class suggests to select between refactoring handlers
- [`KT-18096`](https://youtrack.jetbrains.com/issue/KT-18096) Refactor / Rename on import alias usage of a class member element tries to rename the element itself
- [`KT-18098`](https://youtrack.jetbrains.com/issue/KT-18098) Refactor / Copy can't generate proper import if original code uses import alias of java member
- [`KT-18135`](https://youtrack.jetbrains.com/issue/KT-18135) Refactor: no Problems Detected for Copy/Move source using platform type to another platform's module
- [`KT-18200`](https://youtrack.jetbrains.com/issue/KT-18200) Refactor / Copy is enabled for Java source selected with Kotlin file, but not for Java source selected with Kotlin class
- [`KT-18241`](https://youtrack.jetbrains.com/issue/KT-18241) Refactor / Copy (and Move) fails for chain of lambdas and invoke()'s with IllegalStateException: "No selector for PARENTHESIZED" at KtSimpleNameReference.changeQualifiedName()
- [`KT-18325`](https://youtrack.jetbrains.com/issue/KT-18325) Renaming a parameter name in one implementation silently rename it in all implementations
- [`KT-18390`](https://youtrack.jetbrains.com/issue/KT-18390) Refactor / Copy called for Java class opens only Copy File dialog
- [`KT-18699`](https://youtrack.jetbrains.com/issue/KT-18699) Refactor / Copy, Move loses necessary parentheses
- [`KT-18738`](https://youtrack.jetbrains.com/issue/KT-18738) Misleading quick fix message for an 'open' modifier on an interface member
- [`KT-19130`](https://youtrack.jetbrains.com/issue/KT-19130) Refactor / Inline val: "Show inline dialog for local variables" setting is ignored

### JavaScript

#### Performance Improvements

- [`KT-18331`](https://youtrack.jetbrains.com/issue/KT-18331) JS: compilation performance degrades fast when inlined nested labels are used
#### Fixes

- [`KT-4078`](https://youtrack.jetbrains.com/issue/KT-4078) JS sourcemaps should contain relative path. The relative base & prefix should be set from project/module preferences
- [`KT-8020`](https://youtrack.jetbrains.com/issue/KT-8020) JS: String? plus operator crashes on runtime
- [`KT-13919`](https://youtrack.jetbrains.com/issue/KT-13919) JS: Source map weirdness
- [`KT-15456`](https://youtrack.jetbrains.com/issue/KT-15456) JS: inlining doesn't work for array constructor with size and lambda
- [`KT-16984`](https://youtrack.jetbrains.com/issue/KT-16984) KotlinJS - 1 > 2 > false causes unhandled javascript exception
- [`KT-17285`](https://youtrack.jetbrains.com/issue/KT-17285) JS: wrong result when call function with default parameter overridden by delegation by function from another interface
- [`KT-17445`](https://youtrack.jetbrains.com/issue/KT-17445) JS: minifier for Kotlin JS apps
- [`KT-17476`](https://youtrack.jetbrains.com/issue/KT-17476) JS: Some symbols in identifiers compile, but are not legal
- [`KT-17871`](https://youtrack.jetbrains.com/issue/KT-17871) JS: spread vararg call doesn't work on functions imported with @JsModule
- [`KT-18027`](https://youtrack.jetbrains.com/issue/KT-18027) JS: Illegal symbols are possible in backticked labels, but cause crash in runtime and malformed js code
- [`KT-18032`](https://youtrack.jetbrains.com/issue/KT-18032) JS: Illegal symbols are possible in backticked package names, but cause crash in runtime and malformed js code
- [`KT-18169`](https://youtrack.jetbrains.com/issue/KT-18169) JS: reified generic backticked type name containing non-identifier symbols causes malformed JS and runtime crash
- [`KT-18187`](https://youtrack.jetbrains.com/issue/KT-18187) JS backend does not copy non-abstract method of interface to implementing class in some cases
- [`KT-18201`](https://youtrack.jetbrains.com/issue/KT-18201) JS backend generates wrong code for inline function which calls non-inline function from another module
- [`KT-18652`](https://youtrack.jetbrains.com/issue/KT-18652) JS: Objects from same package but from different libraries are incorrectly accessed

### Libraries

- [`KT-18526`](https://youtrack.jetbrains.com/issue/KT-18526) Small typo in documentation for kotlin-stdlib / kotlin.collections / retainAll
- [`KT-18624`](https://youtrack.jetbrains.com/issue/KT-18624) JS: Bad return type for Promise.all
- [`KT-18670`](https://youtrack.jetbrains.com/issue/KT-18670) Incorrect documentation of MutableMap.values
- [`KT-18671`](https://youtrack.jetbrains.com/issue/KT-18671) Provide implementation for CoroutineContext.Element functions.

### Reflection

- [`KT-15222`](https://youtrack.jetbrains.com/issue/KT-15222) Support reflection for local delegated properties
- [`KT-14094`](https://youtrack.jetbrains.com/issue/KT-14094) IllegalAccessException when try to get members annotated by private annotation with parameter
- [`KT-16399`](https://youtrack.jetbrains.com/issue/KT-16399) Embedded Tomcat fails to load Class-Path: kotlin-runtime.jar from kotlin-reflect-1.0.6.jar
- [`KT-16810`](https://youtrack.jetbrains.com/issue/KT-16810) Do not include incorrect ExternalOverridabilityCondition service file into kotlin-reflect.jar
- [`KT-18404`](https://youtrack.jetbrains.com/issue/KT-18404) “KotlinReflectionInternalError: This callable does not support a default call” when function or constructor has more than 32 parameters
- [`KT-18476`](https://youtrack.jetbrains.com/issue/KT-18476) KClass<*>.superclasses does not contain Any::class
- [`KT-18480`](https://youtrack.jetbrains.com/issue/KT-18480) Kotlin Reflection unable to call getter of protected read-only val with custom getter from parent class

### Tools

- [`KT-18245`](https://youtrack.jetbrains.com/issue/KT-18245) NoArg: IllegalAccessError on instantiating sealed class child via Java reflection
- [`KT-18874`](https://youtrack.jetbrains.com/issue/KT-18874) Crash during compilation after switching to 1.1.3-release-IJ2017.2-2
- [`KT-19047`](https://youtrack.jetbrains.com/issue/KT-19047) Private methods are final event if used with the all-open-plugin.

### Tools. CLI

- [`KT-17297`](https://youtrack.jetbrains.com/issue/KT-17297) Report error when CLI compiler is not being run under Java 8+
- [`KT-18599`](https://youtrack.jetbrains.com/issue/KT-18599) Support -Xmodule-path and -Xadd-modules arguments for modular compilation on Java 9
- [`KT-18794`](https://youtrack.jetbrains.com/issue/KT-18794) kotlinc-jvm prints an irrelevant error message when a JVM Home directory does not exist
- [`KT-3045`](https://youtrack.jetbrains.com/issue/KT-3045) Report error instead of failing with exception on "kotlinc -script foo.kt"
- [`KT-18754`](https://youtrack.jetbrains.com/issue/KT-18754) Rename CLI argument "-module" to "-Xbuild-file"
- [`KT-18927`](https://youtrack.jetbrains.com/issue/KT-18927) run kotlin app crashes eclipse

### Tools. Gradle

- [`KT-10537`](https://youtrack.jetbrains.com/issue/KT-10537) Gradle plugin doesn't pick up changed project.buildDir
- [`KT-17031`](https://youtrack.jetbrains.com/issue/KT-17031) JVM crash on in-process compilation in Gradle with debug
- [`KT-17035`](https://youtrack.jetbrains.com/issue/KT-17035) Gradle Kotlin Plugin can not compile tests calling source internal fields/variables if compileJava dumps classes to a different directory and then copied classes are moved to sourceSets.main.output.classesDir by a different task
- [`KT-17197`](https://youtrack.jetbrains.com/issue/KT-17197) Gradle Kotlin plugin does not wire task dependencies correctly, causing compilation failures
- [`KT-17618`](https://youtrack.jetbrains.com/issue/KT-17618) Pass freeCompilerArgs to compiler unchanged
- [`KT-18262`](https://youtrack.jetbrains.com/issue/KT-18262) kotlin-spring should also open @SpringBootTest classes
- [`KT-18647`](https://youtrack.jetbrains.com/issue/KT-18647) Kotlin incremental compile cannot be disabled.
- [`KT-18832`](https://youtrack.jetbrains.com/issue/KT-18832) Java version parsing error with Gradle Kotlin plugin + JDK 9

### Tools. J2K

- [`KT-10762`](https://youtrack.jetbrains.com/issue/KT-10762) J2K removes empty lines from Doc-comments
- [`KT-13146`](https://youtrack.jetbrains.com/issue/KT-13146) J2K goes into infinite loop with anonymous inner class that references itself
- [`KT-15761`](https://youtrack.jetbrains.com/issue/KT-15761) Converting Java to Kotlin corrupts string which includes escaped backslash
- [`KT-16133`](https://youtrack.jetbrains.com/issue/KT-16133) Converting switch statement inserts dead code (possibly as a false positive for fall-through)
- [`KT-16142`](https://youtrack.jetbrains.com/issue/KT-16142) Kotlin Konverter produces empty line in Kdoc
- [`KT-18038`](https://youtrack.jetbrains.com/issue/KT-18038) Java to Kotlin converter messes up empty lines while converting from JavaDoc to KDoc
- [`KT-18051`](https://youtrack.jetbrains.com/issue/KT-18051) Doesn't work the auto-convert Java to Kotlin in Android Studio 3.0
- [`KT-18141`](https://youtrack.jetbrains.com/issue/KT-18141) J2K changes semantic when while does not have a body
- [`KT-18142`](https://youtrack.jetbrains.com/issue/KT-18142) J2K changes semantics when `if` does not have a body
- [`KT-18512`](https://youtrack.jetbrains.com/issue/KT-18512) J2K Incorrect null parameter conversion

### Tools. JPS

- [`KT-14848`](https://youtrack.jetbrains.com/issue/KT-14848) JPS: invalid compiler argument causes exception (see also EA-92062)
- [`KT-16057`](https://youtrack.jetbrains.com/issue/KT-16057) Provide better error message when the same compiler argument is set twice
- [`KT-19155`](https://youtrack.jetbrains.com/issue/KT-19155) IllegalArgumentException: Unsupported kind: PACKAGE_LOCAL_VARIABLE_LIST in incremental compilation

### Tools. Maven

- [`KT-18022`](https://youtrack.jetbrains.com/issue/KT-18022) kotlin maven plugin - adding dependencies overwrites arguments.pluginClassPath preventing kapt goal from running
- [`KT-18224`](https://youtrack.jetbrains.com/issue/KT-18224) Maven compilation with JDK 9 fails with InaccessibleObjectException

### Tools. REPL

- [`KT-5620`](https://youtrack.jetbrains.com/issue/KT-5620) REPL: Support destructuring declarations
- [`KT-12564`](https://youtrack.jetbrains.com/issue/KT-12564) Kotlin REPL Doesn't Perform Many Checks
- [`KT-15172`](https://youtrack.jetbrains.com/issue/KT-15172) REPL: function declarations that contain empty lines throw error
- [`KT-18181`](https://youtrack.jetbrains.com/issue/KT-18181) REPL: support non-headless execution for Swing code
- [`KT-18349`](https://youtrack.jetbrains.com/issue/KT-18349) REPL: do not show warnings when there are errors

### Tools. kapt

- [`KT-18682`](https://youtrack.jetbrains.com/issue/KT-18682) Kapt: Anonymous class types are not rendered properly in stubs
- [`KT-18758`](https://youtrack.jetbrains.com/issue/KT-18758) Kotlin 1.1.3  / Kapt fails with gradle
- [`KT-18799`](https://youtrack.jetbrains.com/issue/KT-18799) Kapt3, IC: Kapt does not generate annotation value for constant values in documented types
- [`KT-19178`](https://youtrack.jetbrains.com/issue/KT-19178) Kapt: Build dependencies from 'kapt' configuration should go into the 'kaptCompile' task dependencies
- [`KT-19179`](https://youtrack.jetbrains.com/issue/KT-19179) Kapt: Gradle silently skips 'kotlinKapt' task sometimes
- [`KT-19211`](https://youtrack.jetbrains.com/issue/KT-19211) Kapt3: Generated classes output is not synchronized with Java classes output in pure Java projects (Gradle 4+)

## 1.1.3-2

#### Fixes

-   Noarg compiler plugin reverted to 1.1.2 behavior: by default, it will not run any initialization code
    from the generated default constructor. If you want to run initializers, you need to enable
    the corresponding option as described in the documentation.
    Note that if a @noarg class has initializers that depend on constructor parameters, you will get incorrect
    compiler behavior, so you shouldn't enable this option if you have such classes in your project.
    This resolves [`KT-18667`](https://youtrack.jetbrains.com/issue/KT-18667) and [`KT-18668`](https://youtrack.jetbrains.com/issue/KT-18668).
- [`KT-18689`](https://youtrack.jetbrains.com/issue/KT-18689) Incorrect bytecode generated when passing a bound member reference to an inline function with default argument values
- [`KT-18377`](https://youtrack.jetbrains.com/issue/KT-18377) Syntax error while generating kapt stubs
- [`KT-18411`](https://youtrack.jetbrains.com/issue/KT-18411) Slow debugger step-in into inlined function
- [`KT-18687`](https://youtrack.jetbrains.com/issue/KT-18687) Deadlock in resolve with Kotlin 1.1.3
- [`KT-18726`](https://youtrack.jetbrains.com/issue/KT-18726) Frequent UI hangs in 2017.2 EAPs

## 1.1.3

### Android

#### New Features

- [`KT-12049`](https://youtrack.jetbrains.com/issue/KT-12049) Kotlin Lint: "Missing Parcelable CREATOR field" could suggest "Add implementation" quick fix
- [`KT-16712`](https://youtrack.jetbrains.com/issue/KT-16712) Show warning in IDEA when using Java 1.8 api in Android
- [`KT-16843`](https://youtrack.jetbrains.com/issue/KT-16843) Android: provide gutter icons for resources like colors and drawables
- [`KT-17389`](https://youtrack.jetbrains.com/issue/KT-17389) Implement Intention "Add Activity / BroadcastReceiver / Service to manifest" 
- [`KT-17465`](https://youtrack.jetbrains.com/issue/KT-17465) Add intentions Add/Remove/Redo parcelable implementation
#### Fixes

- [`KT-14970`](https://youtrack.jetbrains.com/issue/KT-14970) ClassCastException: butterknife.lint.LintRegistry cannot be cast to com.android.tools.klint.client.api.IssueRegistry
- [`KT-17287`](https://youtrack.jetbrains.com/issue/KT-17287) Configure kotlin in Android Studio: don't show menu Choose Configurator with single choice
- [`KT-17288`](https://youtrack.jetbrains.com/issue/KT-17288) Android Studio: please remove menu item Configure Kotlin (JavaScript) in Project
- [`KT-17289`](https://youtrack.jetbrains.com/issue/KT-17289) Android Studio: Hide "Configure Kotlin" balloon if Kotlin is configured from a kt file banner
- [`KT-17291`](https://youtrack.jetbrains.com/issue/KT-17291) Android Studio 2.4: Cannot get property 'metaClass' on null object error after Kotlin configuring
- [`KT-17610`](https://youtrack.jetbrains.com/issue/KT-17610) "Unknown reference: kotlinx"

### Compiler

#### New Features

- [`KT-11167`](https://youtrack.jetbrains.com/issue/KT-11167) Support compilation against JRE 9
- [`KT-17497`](https://youtrack.jetbrains.com/issue/KT-17497) Warn about redundant else branch in exhaustive when
#### Performance Improvements

- [`KT-7931`](https://youtrack.jetbrains.com/issue/KT-7931) Optimize iteration over strings/charsequences on JVM
- [`KT-10848`](https://youtrack.jetbrains.com/issue/KT-10848) Optimize substitution of inline function with default parameters
- [`KT-12497`](https://youtrack.jetbrains.com/issue/KT-12497) Optimize inlined bytecode for functions with default parameters
- [`KT-17342`](https://youtrack.jetbrains.com/issue/KT-17342) Optimize control-flow for case of many variables
- [`KT-17562`](https://youtrack.jetbrains.com/issue/KT-17562) Optimize KtFile::isScript
#### Fixes

- [`KT-4960`](https://youtrack.jetbrains.com/issue/KT-4960) Redeclaration is not reported for type parameters of interfaces
- [`KT-5160`](https://youtrack.jetbrains.com/issue/KT-5160) No warning when a lambda parameter hides a variable
- [`KT-5246`](https://youtrack.jetbrains.com/issue/KT-5246) is check fails on cyclic type parameter bounds
- [`KT-5354`](https://youtrack.jetbrains.com/issue/KT-5354) Wrong label resolution when label name clash with fun name
- [`KT-7645`](https://youtrack.jetbrains.com/issue/KT-7645) Prohibit default value for `catch`-block parameter
- [`KT-7724`](https://youtrack.jetbrains.com/issue/KT-7724) Can never import private member 
- [`KT-7796`](https://youtrack.jetbrains.com/issue/KT-7796) Wrong scope for default parameter value resolution
- [`KT-7984`](https://youtrack.jetbrains.com/issue/KT-7984) Unexpected "Unresolved reference" in a default value expression in a local function
- [`KT-7985`](https://youtrack.jetbrains.com/issue/KT-7985) Unexpected "Unresolved reference to a type parameter" in a default value expression in a local function
- [`KT-8320`](https://youtrack.jetbrains.com/issue/KT-8320) It should not be possible to catch a type parameter type
- [`KT-8877`](https://youtrack.jetbrains.com/issue/KT-8877) Automatic labeling doesn't work for infix calls
- [`KT-9251`](https://youtrack.jetbrains.com/issue/KT-9251) Qualified this does not work with labeled function literals
- [`KT-9551`](https://youtrack.jetbrains.com/issue/KT-9551) False warning "No cast needed"
- [`KT-9645`](https://youtrack.jetbrains.com/issue/KT-9645) Incorrect inspection: No cast Needed
- [`KT-9986`](https://youtrack.jetbrains.com/issue/KT-9986) 'null as T' should be unchecked cast
- [`KT-10397`](https://youtrack.jetbrains.com/issue/KT-10397) java.lang.reflect.GenericSignatureFormatError when generic inner class is mentioned in function signature
- [`KT-11474`](https://youtrack.jetbrains.com/issue/KT-11474) ISE: Requested A, got foo.A in JavaClassFinderImpl on Java file with package not matching directory
- [`KT-11622`](https://youtrack.jetbrains.com/issue/KT-11622) False "No cast needed" when ambiguous call because of smart cast
- [`KT-12245`](https://youtrack.jetbrains.com/issue/KT-12245) Code with annotation that has an unresolved identifier as a parameter compiles successfully
- [`KT-12269`](https://youtrack.jetbrains.com/issue/KT-12269) False "Non-null type is checked for instance of nullable type"
- [`KT-12683`](https://youtrack.jetbrains.com/issue/KT-12683) A problem with `is` operator and non-reified type-parameters
- [`KT-12690`](https://youtrack.jetbrains.com/issue/KT-12690) USELESS_CAST compiler warning may break code when fix is applied
- [`KT-13348`](https://youtrack.jetbrains.com/issue/KT-13348) Report useless cast on safe cast from nullable type to the same not null type
- [`KT-13597`](https://youtrack.jetbrains.com/issue/KT-13597) No check for accessing final field in local object in constructor
- [`KT-13997`](https://youtrack.jetbrains.com/issue/KT-13997) Incorrect "Property must be initialized or be abstract" error for property with external accessors
- [`KT-14381`](https://youtrack.jetbrains.com/issue/KT-14381) Possible val reassignment not detected inside init block
- [`KT-14564`](https://youtrack.jetbrains.com/issue/KT-14564) java.lang.VerifyError: Bad local variable type
- [`KT-14801`](https://youtrack.jetbrains.com/issue/KT-14801) Invoke error message if nested class has the same name as a function from base class
- [`KT-14977`](https://youtrack.jetbrains.com/issue/KT-14977) IDE doesn't warn about checking null value of variable that cannot be null
- [`KT-15085`](https://youtrack.jetbrains.com/issue/KT-15085) Label and function naming conflict is resolved in unintuitive way
- [`KT-15161`](https://youtrack.jetbrains.com/issue/KT-15161) False warning "no cast needed" for array creation
- [`KT-15480`](https://youtrack.jetbrains.com/issue/KT-15480) Cannot destruct a list when "if" has an "else" branch
- [`KT-15495`](https://youtrack.jetbrains.com/issue/KT-15495) Internal typealiases in the same module are inaccessible on incremental compilation
- [`KT-15566`](https://youtrack.jetbrains.com/issue/KT-15566) Object member imported in file scope used in delegation expression in object declaration should be a compiler error
- [`KT-16016`](https://youtrack.jetbrains.com/issue/KT-16016) Compiler failure with NO_EXPECTED_TYPE
- [`KT-16426`](https://youtrack.jetbrains.com/issue/KT-16426) Return statement resolved to function instead of property getter
- [`KT-16813`](https://youtrack.jetbrains.com/issue/KT-16813) Anonymous objects returned from private-in-file members should behave as for private class members
- [`KT-16864`](https://youtrack.jetbrains.com/issue/KT-16864) Local delegate + ad-hoc object leads to CCE
- [`KT-17144`](https://youtrack.jetbrains.com/issue/KT-17144) Breakpoint inside `when`
- [`KT-17149`](https://youtrack.jetbrains.com/issue/KT-17149) Incorrect warning "Kotlin: This operation has led to an overflow"
- [`KT-17156`](https://youtrack.jetbrains.com/issue/KT-17156) No re-parse after lambda was converted to block
- [`KT-17318`](https://youtrack.jetbrains.com/issue/KT-17318) Typo in DSL Marker message `cant`
- [`KT-17384`](https://youtrack.jetbrains.com/issue/KT-17384) break/continue expression in inlined function parameter argument causes compilation exception
- [`KT-17457`](https://youtrack.jetbrains.com/issue/KT-17457) Suspend + LongRange couldn't transform method node issue in Kotlin 1.1.1
- [`KT-17479`](https://youtrack.jetbrains.com/issue/KT-17479) val reassign is allowed via explicit this receiver
- [`KT-17560`](https://youtrack.jetbrains.com/issue/KT-17560) Overload resolution ambiguity on semi-valid class-files generated by Scala
- [`KT-17572`](https://youtrack.jetbrains.com/issue/KT-17572) try-catch expression in inlined function parameter argument causes compilation exception
- [`KT-17573`](https://youtrack.jetbrains.com/issue/KT-17573) try-finally expression in inlined function parameter argument fails with VerifyError
- [`KT-17588`](https://youtrack.jetbrains.com/issue/KT-17588) Compiler error while optimizer tries to get rid of captured variable
- [`KT-17590`](https://youtrack.jetbrains.com/issue/KT-17590) conditional return in inline function parameter argument causes compilation exception
- [`KT-17591`](https://youtrack.jetbrains.com/issue/KT-17591) non-conditional return in inline function parameter argument causes compilation exception
- [`KT-17613`](https://youtrack.jetbrains.com/issue/KT-17613) 'this' expression referring to deprecated class instance is highlighted as deprecated in IDE
- [`KT-18358`](https://youtrack.jetbrains.com/issue/KT-18358) Keep smart pointers instead of PSI elements in JavaElementImpl and its descendants

### IDE

#### New Features

- [`KT-7810`](https://youtrack.jetbrains.com/issue/KT-7810) Separate icon for abstract class
- [`KT-8617`](https://youtrack.jetbrains.com/issue/KT-8617) Recognize TODO method usages and highlight them same as TODO-comment
- [`KT-12629`](https://youtrack.jetbrains.com/issue/KT-12629) Add rainbow/semantic-highlighting for local variables
- [`KT-14109`](https://youtrack.jetbrains.com/issue/KT-14109) support parameter hints in idea plugin
- [`KT-16645`](https://youtrack.jetbrains.com/issue/KT-16645) Support inlay type hints for implicitly typed vals, properties, and functions
- [`KT-17807`](https://youtrack.jetbrains.com/issue/KT-17807) Add Smart Enter processor for object expessions
#### Performance Improvements

- [`KT-16995`](https://youtrack.jetbrains.com/issue/KT-16995) Typing during in-place refactorings is impossibly laggy
- [`KT-17331`](https://youtrack.jetbrains.com/issue/KT-17331) Frequent long editor freezes
- [`KT-17383`](https://youtrack.jetbrains.com/issue/KT-17383) Slow editing in Kotlin files If breadcrumbs are enabled in module with many dependencies
- [`KT-17495`](https://youtrack.jetbrains.com/issue/KT-17495) Much time spent in LibraryDependenciesCache.getLibrariesAndSdksUsedWith
#### Fixes

- [`KT-7848`](https://youtrack.jetbrains.com/issue/KT-7848) When you paste text into a string literal special symbols should be escaped
- [`KT-7954`](https://youtrack.jetbrains.com/issue/KT-7954) 'Go to symbol' doesn't show containing declaration for local symbols
- [`KT-9091`](https://youtrack.jetbrains.com/issue/KT-9091) Sometimes backticks of the method name with spaces are highlighted with rose background
- [`KT-10577`](https://youtrack.jetbrains.com/issue/KT-10577) Refactor / Move Kotlin + Java files adds wrong import in very specific case
- [`KT-12856`](https://youtrack.jetbrains.com/issue/KT-12856) Import fold region is not updated to include imports added while editing file
- [`KT-14161`](https://youtrack.jetbrains.com/issue/KT-14161) Navigate to symbol doesn't see local named functions
- [`KT-14601`](https://youtrack.jetbrains.com/issue/KT-14601) Formatter inserts unnecessary indent before 'else'
- [`KT-14639`](https://youtrack.jetbrains.com/issue/KT-14639) Incorrect name of code style setting: Align in columns 'case' branches
- [`KT-15029`](https://youtrack.jetbrains.com/issue/KT-15029) "Go to symbol" action doesn't find properties declared in primary constructors
- [`KT-15255`](https://youtrack.jetbrains.com/issue/KT-15255) Move cursor to a better place when creating a new Kotlin file
- [`KT-15273`](https://youtrack.jetbrains.com/issue/KT-15273) Kotlin IDE plugin adds `import java.lang.String` with "Optimize Imports", making project broken
- [`KT-16159`](https://youtrack.jetbrains.com/issue/KT-16159) Wrong "Constructor call" highlighting if operator is called on newly created object
- [`KT-16392`](https://youtrack.jetbrains.com/issue/KT-16392) Gradle/Maven java module: Add framework support/ Kotlin (Java or JavaScript) adds nothing
- [`KT-16423`](https://youtrack.jetbrains.com/issue/KT-16423) Show expression type doesn't work when selecting from the middle of expression with "Expand Selection"
- [`KT-16635`](https://youtrack.jetbrains.com/issue/KT-16635) Do not show kotlin-specific live templates macros for all context types
- [`KT-16755`](https://youtrack.jetbrains.com/issue/KT-16755) No "Is sublassed by" icon for sealed class
- [`KT-16775`](https://youtrack.jetbrains.com/issue/KT-16775) Rewrite at slice CLASS key: OBJECT_DECLARATION while writing code in IDE
- [`KT-16803`](https://youtrack.jetbrains.com/issue/KT-16803) Suspending iteration is not marked in the gutter by IDEA as suspending invocation
- [`KT-17037`](https://youtrack.jetbrains.com/issue/KT-17037) Editor suggests to import `EmptyCoroutineContext.plus` for any unresolved `+`
- [`KT-17046`](https://youtrack.jetbrains.com/issue/KT-17046) Kotlin facet, Compiler plugins: last line is shown empty when not selected
- [`KT-17088`](https://youtrack.jetbrains.com/issue/KT-17088) Settings: Kotlin Compiler: "Destination directory" should be enabled if "Copy library runtime files" is on on the dialog opening 
- [`KT-17094`](https://youtrack.jetbrains.com/issue/KT-17094) Kotlin facet, additional command line parameters dialog: please provide a title
- [`KT-17138`](https://youtrack.jetbrains.com/issue/KT-17138) Configure Kotlin in Project: Choose Configurator popup: names could be unified
- [`KT-17145`](https://youtrack.jetbrains.com/issue/KT-17145) Kotlin facet: IllegalArgumentException on attempt to show settings common for several javascript kotlin facets with different moduleKind
- [`KT-17223`](https://youtrack.jetbrains.com/issue/KT-17223) Absolute path to Kotlin compiler plugin in IML
- [`KT-17293`](https://youtrack.jetbrains.com/issue/KT-17293) Project Structure dialog is opened too slow for a project with a lot of empty gradle modules
- [`KT-17304`](https://youtrack.jetbrains.com/issue/KT-17304) IDEA shows wrong type for expressions
- [`KT-17439`](https://youtrack.jetbrains.com/issue/KT-17439) Kotlin: 'autoscroll from source' doesn't work in Structure view
- [`KT-17448`](https://youtrack.jetbrains.com/issue/KT-17448) Regression: Sample Resolve 
- [`KT-17482`](https://youtrack.jetbrains.com/issue/KT-17482) Set jvmTarget to 1.8 by default when configuring a project with JDK 1.8
- [`KT-17492`](https://youtrack.jetbrains.com/issue/KT-17492) -jvm-target is ignored by IntelliJ
- [`KT-17505`](https://youtrack.jetbrains.com/issue/KT-17505) LazyLightClassMemberMatchingError from collection implementation
- [`KT-17517`](https://youtrack.jetbrains.com/issue/KT-17517) Compiler options specified as properties are not handled by Maven importer
- [`KT-17521`](https://youtrack.jetbrains.com/issue/KT-17521) Quickfix to enable coroutines should work for Maven projects
- [`KT-17525`](https://youtrack.jetbrains.com/issue/KT-17525) IDE: KNPE at KotlinAddImportActionKt.createSingleImportActionForConstructor() on invalid reference to inner class constructor
- [`KT-17578`](https://youtrack.jetbrains.com/issue/KT-17578) Throwable: "Reported element PsiIdentifier:AnnotationConfiguration is not from the file 'PsiFile:InSource.kt' the inspection 'ImplicitSubclassInspection'"
- [`KT-17638`](https://youtrack.jetbrains.com/issue/KT-17638) ISE in KotlinElementDescriptionProvider.renderShort
- [`KT-17698`](https://youtrack.jetbrains.com/issue/KT-17698) Unknown library format - prevents IDEA from configuring Kotlin JS
- [`KT-17714`](https://youtrack.jetbrains.com/issue/KT-17714) UAST inspection on non-physical element
- [`KT-17722`](https://youtrack.jetbrains.com/issue/KT-17722) IntelliJ plugin uses wrong JVM target when Kotlin Facet is not configured
- [`KT-17770`](https://youtrack.jetbrains.com/issue/KT-17770) Kotlin IntelliJ plugin fails to re-index Gradle script classpath after change to the `plugins` block
- [`KT-17777`](https://youtrack.jetbrains.com/issue/KT-17777) Logger$EmptyThrowable: "Facet Kotlin (app) [kotlin-language] not found" at FacetModelImpl.removeFacet()
- [`KT-17810`](https://youtrack.jetbrains.com/issue/KT-17810) Exception from unused import inspection leads to code analysis hangs
- [`KT-17821`](https://youtrack.jetbrains.com/issue/KT-17821) In Kotlin's plugin KotlinJsMetadataVersionIndex loads file with VfsUtilCore.loadText
- [`KT-17840`](https://youtrack.jetbrains.com/issue/KT-17840) Show expression type on `this` shows bogus disambiguation
- [`KT-17845`](https://youtrack.jetbrains.com/issue/KT-17845) Searching for usages of override property in primary constructor doesn't suggest base property search
- [`KT-17847`](https://youtrack.jetbrains.com/issue/KT-17847) Kotlin facet: strange warning if API version = 1.2
- [`KT-17857`](https://youtrack.jetbrains.com/issue/KT-17857) Java should see classes affected by "allopen" plugin as open
- [`KT-17861`](https://youtrack.jetbrains.com/issue/KT-17861) Setting 'kotlin.experimental.coroutines "enable"' doesn't work for Android projects
- [`KT-17875`](https://youtrack.jetbrains.com/issue/KT-17875) New Project/Module with Kotlin: on attempt to use libraries from plugin IDE suggests to rewrite them
- [`KT-17876`](https://youtrack.jetbrains.com/issue/KT-17876) New Project/Module with Kotlin: with "Copy to" option only part of jars are copied
- [`KT-17899`](https://youtrack.jetbrains.com/issue/KT-17899) Navigate to symbol: vararg signatures are indistinguishable from non-vararg ones
- [`KT-18070`](https://youtrack.jetbrains.com/issue/KT-18070) KtLightModifierList.hasExplicitModifier("default") is true for interface method with body

### IDE. Completion

#### New Features

- [`KT-11250`](https://youtrack.jetbrains.com/issue/KT-11250) Auto-completion for convention function names in 'operator fun' definitions
- [`KT-12293`](https://youtrack.jetbrains.com/issue/KT-12293) Autocompletion should propose `lateinit var` in addition to `lateinit`
- [`KT-13673`](https://youtrack.jetbrains.com/issue/KT-13673) Add 'companion { ... }'  code completion opsion
#### Performance Improvements

- [`KT-10978`](https://youtrack.jetbrains.com/issue/KT-10978) Kotlin + JOOQ + Intellij performance is unusable 
- [`KT-16715`](https://youtrack.jetbrains.com/issue/KT-16715) Typing is very slow since 1.1
- [`KT-16850`](https://youtrack.jetbrains.com/issue/KT-16850) UI freeze for several seconds during inserting selected completion variant
#### Fixes

- [`KT-13524`](https://youtrack.jetbrains.com/issue/KT-13524) Completing the keyword 'constructor' before a primary constructor wrongly inserts parentheses
- [`KT-14665`](https://youtrack.jetbrains.com/issue/KT-14665) No completion for "else" keyword
- [`KT-15603`](https://youtrack.jetbrains.com/issue/KT-15603) Annoying completion when making a primary constructor private
- [`KT-16161`](https://youtrack.jetbrains.com/issue/KT-16161) Completion of 'onEach' inserts unneeded angular brackets
- [`KT-16856`](https://youtrack.jetbrains.com/issue/KT-16856) Code completion optimization

### IDE. Debugger

- [`KT-15823`](https://youtrack.jetbrains.com/issue/KT-15823) Breakpoints not work inside crossinline from init of object passed into collection
- [`KT-15854`](https://youtrack.jetbrains.com/issue/KT-15854) Debugger not able to evaluate internal member functions
- [`KT-16025`](https://youtrack.jetbrains.com/issue/KT-16025) Step into suspend functions stops at the function end
- [`KT-17295`](https://youtrack.jetbrains.com/issue/KT-17295) Can't stop in kotlin.concurrent.timer lambda parameter

### IDE. Inspections and Intentions

#### New Features

- [`KT-10981`](https://youtrack.jetbrains.com/issue/KT-10981) Quickfix for INAPPLICABLE_JVM_FIELD to replace with 'const' when possible
- [`KT-14046`](https://youtrack.jetbrains.com/issue/KT-14046) Add intention to add inline keyword if a function has parameter with noinline and/or crossinline modifier
- [`KT-14137`](https://youtrack.jetbrains.com/issue/KT-14137) Add intention to convert top level val with object expression to object
- [`KT-15903`](https://youtrack.jetbrains.com/issue/KT-15903) QuickFix to add/remove suspend in hierarchies
- [`KT-16786`](https://youtrack.jetbrains.com/issue/KT-16786) Intention to add "open" modifier to a non-private method or property in an open class
- [`KT-16851`](https://youtrack.jetbrains.com/issue/KT-16851) Quickfix adding qualifier `@call` to unallowed 'return' in closures
- [`KT-17053`](https://youtrack.jetbrains.com/issue/KT-17053) Inspection to detect use of callable reference as a lambda body
- [`KT-17054`](https://youtrack.jetbrains.com/issue/KT-17054) Intention/ inspection to convert 'if' with 'is' check to 'as?' with safe call 
- [`KT-17191`](https://youtrack.jetbrains.com/issue/KT-17191) Intention to name anonymous (_) parameter
- [`KT-17221`](https://youtrack.jetbrains.com/issue/KT-17221) Inspection for recursive calls in property accessors
- [`KT-17520`](https://youtrack.jetbrains.com/issue/KT-17520) Quickfix to update language/API version should work for Maven projects
- [`KT-17650`](https://youtrack.jetbrains.com/issue/KT-17650) Add quickfix inserting 'lateinit' modifier for not-initialized property
- [`KT-17660`](https://youtrack.jetbrains.com/issue/KT-17660) Inspection: data class copy without named argument(s)
#### Fixes

- [`KT-10211`](https://youtrack.jetbrains.com/issue/KT-10211) "Replace infix call with ordinary call" appears both as a quickfix and as an intention in the pop-up
- [`KT-11003`](https://youtrack.jetbrains.com/issue/KT-11003) Invalid quickfix in companion object for open properties
- [`KT-12805`](https://youtrack.jetbrains.com/issue/KT-12805) False positive redundant semicolon after while without block expression
- [`KT-14335`](https://youtrack.jetbrains.com/issue/KT-14335) Unexpected range of "convert lambda to reference" intention
- [`KT-14435`](https://youtrack.jetbrains.com/issue/KT-14435) "Use destructuring declaration" should be available as intention even without usages
- [`KT-14443`](https://youtrack.jetbrains.com/issue/KT-14443) IDEA intention suggest to make a method in an interface final
- [`KT-14820`](https://youtrack.jetbrains.com/issue/KT-14820) Convert function to property shouldn't insert explicit type if it was inferred previously
- [`KT-15076`](https://youtrack.jetbrains.com/issue/KT-15076) Replace if with elvis inspection should not be reported in some complex cases
- [`KT-15543`](https://youtrack.jetbrains.com/issue/KT-15543) "Convert receiver to parameter" refactoring breaks code
- [`KT-15942`](https://youtrack.jetbrains.com/issue/KT-15942) "Convert to secondary constructor" intention is available for data class
- [`KT-16136`](https://youtrack.jetbrains.com/issue/KT-16136) Wrong type parameter variance suggested if type parameter is used in nested anonymous object
- [`KT-16339`](https://youtrack.jetbrains.com/issue/KT-16339) Incorrect warning: 'protected' visibility is effectively 'private' in a final class
- [`KT-16577`](https://youtrack.jetbrains.com/issue/KT-16577) "Redundant semicolon" is not reported for semicolon after package statement in file with no imports
- [`KT-17079`](https://youtrack.jetbrains.com/issue/KT-17079) Kotlin: Bad conversion of double comparison to range check if bounds have mixed types
- [`KT-17372`](https://youtrack.jetbrains.com/issue/KT-17372) Specify explicit lambda signature handles anonymous parameters incorrectly
- [`KT-17404`](https://youtrack.jetbrains.com/issue/KT-17404) Editor: attempt to pass type parameter as reified argument causes AE "Classifier descriptor of a type should be of type ClassDescriptor" at DescriptorUtils.getClassDescriptorForTypeConstructor()
- [`KT-17408`](https://youtrack.jetbrains.com/issue/KT-17408) "Convert to secondary constructor" intention is available for annotation parameters
- [`KT-17503`](https://youtrack.jetbrains.com/issue/KT-17503) Intention "To raw string literal" should handle string concatenations
- [`KT-17599`](https://youtrack.jetbrains.com/issue/KT-17599) "Make primary constructor internal" intention is available for annotation class 
- [`KT-17600`](https://youtrack.jetbrains.com/issue/KT-17600) "Make primary constructor private" intention is available for annotation class
- [`KT-17707`](https://youtrack.jetbrains.com/issue/KT-17707) "Final declaration can't be overridden at runtime" inspection reports Kotlin classes non final due to compiler plugin
- [`KT-17708`](https://youtrack.jetbrains.com/issue/KT-17708) "Move to class body" intention is available for annotation parameters
- [`KT-17762`](https://youtrack.jetbrains.com/issue/KT-17762) 'Convert to range' intention generates inequivalent code for doubles

### IDE. Refactorings

#### Performance Improvements

- [`KT-17234`](https://youtrack.jetbrains.com/issue/KT-17234) Refactor / Inline on library property is rejected after GUI freeze for a while
- [`KT-17333`](https://youtrack.jetbrains.com/issue/KT-17333) KotlinChangeInfo retains 132MB of the heap
#### Fixes

- [`KT-8370`](https://youtrack.jetbrains.com/issue/KT-8370) "Can't move to original file" should not be an error
- [`KT-8930`](https://youtrack.jetbrains.com/issue/KT-8930) Refactor / Move preivew: moved element is shown as reference, and its file as subject
- [`KT-9158`](https://youtrack.jetbrains.com/issue/KT-9158) Refactor / Move preview mentions the package statement of moved class as a usage
- [`KT-13192`](https://youtrack.jetbrains.com/issue/KT-13192) Refactor / Move: to another class: "To" field code completion suggests facade and Java classes
- [`KT-13466`](https://youtrack.jetbrains.com/issue/KT-13466) Refactor / Move: class to upper level: the package statement is not updated
- [`KT-15519`](https://youtrack.jetbrains.com/issue/KT-15519) KDoc comments for data class values get removed by Change Signature
- [`KT-17211`](https://youtrack.jetbrains.com/issue/KT-17211) Refactor / Move several files: superfluous FQN is inserted into reference to same file's element
- [`KT-17213`](https://youtrack.jetbrains.com/issue/KT-17213) Refactor / Inline Function: parameters of lambda as call argument turn incompilable
- [`KT-17272`](https://youtrack.jetbrains.com/issue/KT-17272) Refactor / Inline Function: unused String literal in parameters is kept (while unsed Int is not)
- [`KT-17273`](https://youtrack.jetbrains.com/issue/KT-17273) Refactor / Inline Function: PIEAE: "Element: class org.jetbrains.kotlin.psi.KtCallExpression because: different providers" at PsiUtilCore.ensureValid()
- [`KT-17296`](https://youtrack.jetbrains.com/issue/KT-17296) Refactor / Inline Function: UOE at ExpressionReplacementPerformer.findOrCreateBlockToInsertStatement() for call of multi-statement function in declaration
- [`KT-17330`](https://youtrack.jetbrains.com/issue/KT-17330) Inline kotlin function causes an infinite loop
- [`KT-17395`](https://youtrack.jetbrains.com/issue/KT-17395) Refactor / Inline Function: arguments passed to lambda turns code to incompilable
- [`KT-17496`](https://youtrack.jetbrains.com/issue/KT-17496) Refactor / Move: calls to moved extension function type properties are updated (incorrectly)
- [`KT-17515`](https://youtrack.jetbrains.com/issue/KT-17515) Refactor / Move inner class to another class, Move companion object: disabled in editor, but available in Move dialog
- [`KT-17526`](https://youtrack.jetbrains.com/issue/KT-17526) Refactor / Move: reference to companion member gets superfluous companion name in certain cases
- [`KT-17538`](https://youtrack.jetbrains.com/issue/KT-17538) Refactor / Move: moving file with import alias removes alias usage from code
- [`KT-17545`](https://youtrack.jetbrains.com/issue/KT-17545) Refactor / Move: false Problems Detected on moving class using parent's protected class, object
- [`KT-18018`](https://youtrack.jetbrains.com/issue/KT-18018) F5 (for Copy) does not work for Kotlin files anymore
- [`KT-18205`](https://youtrack.jetbrains.com/issue/KT-18205) Moving multiple classes causes imports to be converted to fully qualified class names

### Infrastructure

- [`KT-14988`](https://youtrack.jetbrains.com/issue/KT-14988) Support running the Kotlin compiler on Java 9
- [`KT-17112`](https://youtrack.jetbrains.com/issue/KT-17112) IncompatibleClassChangeError on invoking Kotlin compiler daemon on JDK 9

### JavaScript

#### Fixes

- [`KT-12926`](https://youtrack.jetbrains.com/issue/KT-12926) JS: use # instead of @ when linking to sourcemap from generated code
- [`KT-13577`](https://youtrack.jetbrains.com/issue/KT-13577) Double.hashCode is zero for big numbers
- [`KT-15135`](https://youtrack.jetbrains.com/issue/KT-15135) JS: support friend modules
- [`KT-15484`](https://youtrack.jetbrains.com/issue/KT-15484) JS: (node): println with object /number argument leads to "TypeError: Invalid data, chunk must be a string or buffer, not object/number"
- [`KT-16658`](https://youtrack.jetbrains.com/issue/KT-16658) JS: Suspend function with default param value in interface
- [`KT-16717`](https://youtrack.jetbrains.com/issue/KT-16717) KotlinJs - copy() on data class doesn't work with when there is a secondary constructor
- [`KT-16745`](https://youtrack.jetbrains.com/issue/KT-16745) JS: initialize enum fields before calling companion objects's initializer
- [`KT-16951`](https://youtrack.jetbrains.com/issue/KT-16951) JS: coroutine suspension point is not inserted when inlining suspend function with tail call to another suspend function
- [`KT-16979`](https://youtrack.jetbrains.com/issue/KT-16979) Kotlin.js : Intellij test and productions sources produce a AMD module with the same name
- [`KT-17067`](https://youtrack.jetbrains.com/issue/KT-17067) JS: suspendCoroutine not working as expected
- [`KT-17219`](https://youtrack.jetbrains.com/issue/KT-17219) Hexadecimal literals in js(...) argument are replaced by wrong decimal constants
- [`KT-17281`](https://youtrack.jetbrains.com/issue/KT-17281) JS: wrong code generated for a recursive call in suspend function
- [`KT-17446`](https://youtrack.jetbrains.com/issue/KT-17446) JS: incorrect code generated for call to `suspendCoroutineOrReturn` when the same function calls another suspend function
- [`KT-17540`](https://youtrack.jetbrains.com/issue/KT-17540) Incorrect inlining optimization of `also`/`apply` function
- [`KT-17700`](https://youtrack.jetbrains.com/issue/KT-17700) Wrong code generated for 'str += (nullableChar ?: break)'
- [`KT-17966`](https://youtrack.jetbrains.com/issue/KT-17966) JS: Char literal inside of string template

### Libraries

- [`KT-17453`](https://youtrack.jetbrains.com/issue/KT-17453) Array iterators throw IndexOutOfBoundsException instead of NoSuchElementException
- [`KT-17635`](https://youtrack.jetbrains.com/issue/KT-17635) Document String#toIntOfNull may throw an exception
- [`KT-17686`](https://youtrack.jetbrains.com/issue/KT-17686) takeLast(n) incorrectly performs drop(n) for Lists without random access
- [`KT-17704`](https://youtrack.jetbrains.com/issue/KT-17704) Update JavaDoc for ReentrantReadWriteLock.write to put more stress on the fact that to upgrade to write lock, read lock is first released.
- [`KT-17853`](https://youtrack.jetbrains.com/issue/KT-17853) JS: Confusing parameter names in 'Math.atan2`
- [`KT-18092`](https://youtrack.jetbrains.com/issue/KT-18092) Issue using kotlin-reflect with proguard: missing annotations Mutable and ReadOnly
- [`KT-18210`](https://youtrack.jetbrains.com/issue/KT-18210) JS String::match(regex) should have nullable return type

### Reflection

- [`KT-17055`](https://youtrack.jetbrains.com/issue/KT-17055) NPE in hashCode and equals of kotlin.jvm.internal.FunctionReference (on local functions)
- [`KT-17594`](https://youtrack.jetbrains.com/issue/KT-17594) Cache the result of val Class<T>.kotlin: KClass<T>
- [`KT-18494`](https://youtrack.jetbrains.com/issue/KT-18494) KNPE from Kotlin reflection (sometimes) in UtilKt.toJavaClass

### Tools

- [`KT-16692`](https://youtrack.jetbrains.com/issue/KT-16692) No-Arg-Constructor plugin should generate code to initialize delegates

### Tools. CLI

- [`KT-17696`](https://youtrack.jetbrains.com/issue/KT-17696) Allow kotlinc to take friend modules as .jar files
- [`KT-17697`](https://youtrack.jetbrains.com/issue/KT-17697) Allow kotlinc to take .java files as arguments
- [`KT-9370`](https://youtrack.jetbrains.com/issue/KT-9370) not possible to pass an argument that starts with "-" to a script using kotlinc
- [`KT-17100`](https://youtrack.jetbrains.com/issue/KT-17100) "kotlin" launcher script: do not add current working directory to classpath if explicit "-classpath" is specified
- [`KT-17140`](https://youtrack.jetbrains.com/issue/KT-17140) Warning "classpath entry points to a file that is not a jar file" could just be disabled
- [`KT-17264`](https://youtrack.jetbrains.com/issue/KT-17264) Change the format of advanced CLI arguments ("-X...") to require value after "=", not a whitespace
- [`KT-18180`](https://youtrack.jetbrains.com/issue/KT-18180) Modules not exported by java.se are not readable when compiling against JRE 9

### Tools. Gradle

- [`KT-15151`](https://youtrack.jetbrains.com/issue/KT-15151) Kapt3: Support incremental compilation of Java stubs
- [`KT-16298`](https://youtrack.jetbrains.com/issue/KT-16298) Gradle: IOException "Parent file doesn't exist:/.../artifact-difference.tab.len" on non-incremental clean after incremental build
- [`KT-17681`](https://youtrack.jetbrains.com/issue/KT-17681) Support the new API of Android Gradle plugin (2.4.0+)
- [`KT-17936`](https://youtrack.jetbrains.com/issue/KT-17936) Circular dependency between gradle tasks dataBindingExportBuildInfoDebug and compileDebugKotlin
- [`KT-17960`](https://youtrack.jetbrains.com/issue/KT-17960) Improve test of memory leak with Gradle daemon
- [`KT-18047`](https://youtrack.jetbrains.com/issue/KT-18047) Gradle kotlin options should use unset value as default for languageVersion and apiVersion

### Tools. J2K

- [`KT-16754`](https://youtrack.jetbrains.com/issue/KT-16754) J2K: Apply quick-fixes from EDT thread only
- [`KT-16816`](https://youtrack.jetbrains.com/issue/KT-16816) Java To Kotlin bug: if + chained assignment doesn't include brackets
- [`KT-17230`](https://youtrack.jetbrains.com/issue/KT-17230) J2K Deadlock
- [`KT-17712`](https://youtrack.jetbrains.com/issue/KT-17712) Exception in J2K during InlineCodegen convertion: com.intellij.psi.impl.source.JavaDummyHolder cannot be cast to com.intellij.psi.PsiJavaFile

### Tools. JPS

- [`KT-16568`](https://youtrack.jetbrains.com/issue/KT-16568) modulesWhoseInternalsAreVisible in ModuleDependencies are not filled in for JS projects
- [`KT-17387`](https://youtrack.jetbrains.com/issue/KT-17387) When compiling in the IDE, progress tracker says "configuring the compilation environment" when it clearly isn't
- [`KT-17665`](https://youtrack.jetbrains.com/issue/KT-17665) JPS: Kotlin: The '-d' option with a directory destination is ignored because '-module' is specified
- [`KT-17801`](https://youtrack.jetbrains.com/issue/KT-17801) Unresolved supertypes from JRE on JDK 9 in JPS

### Tools. Maven

- [`KT-17093`](https://youtrack.jetbrains.com/issue/KT-17093) Import from maven: please provide a special tag for coroutine option
- [`KT-10028`](https://youtrack.jetbrains.com/issue/KT-10028) Support parallel builds in maven
- [`KT-15050`](https://youtrack.jetbrains.com/issue/KT-15050) Random build failures using maven 3 (multi-thread) + bamboo
- [`KT-15318`](https://youtrack.jetbrains.com/issue/KT-15318) Intermitent Kotlin compilation errors
- [`KT-16283`](https://youtrack.jetbrains.com/issue/KT-16283) Maven compiler plugin warns, "Source root doesn't exist"
- [`KT-16743`](https://youtrack.jetbrains.com/issue/KT-16743) Update configuration options in Kotlin Maven plugin
- [`KT-16762`](https://youtrack.jetbrains.com/issue/KT-16762) Maven: JS compiler option main is missing

### Tools. REPL

- [`KT-5822`](https://youtrack.jetbrains.com/issue/KT-5822) Exception on package directive in REPL
- [`KT-10060`](https://youtrack.jetbrains.com/issue/KT-10060) REPL: Cannot execute more than 255 lines
- [`KT-17365`](https://youtrack.jetbrains.com/issue/KT-17365) REPL crash when referencing a variable whose definition threw an exception

### Tools. kapt

- [`KT-17245`](https://youtrack.jetbrains.com/issue/KT-17245) Kapt: Javac compiler arguments can't be specified in Gradle
- [`KT-17418`](https://youtrack.jetbrains.com/issue/KT-17418) "The following options were not recognized by any processor: '[kapt.kotlin.generated]'" warning from Javac shouldn't be shown even if no processor supports the generated annotation
- [`KT-17456`](https://youtrack.jetbrains.com/issue/KT-17456) kapt3: NoClassDefFound com/sun/tools/javac/util/Context
- [`KT-17567`](https://youtrack.jetbrains.com/issue/KT-17567) Kapt (1.1.2-eap-77) generates invalid Java stub for internal class
- [`KT-17620`](https://youtrack.jetbrains.com/issue/KT-17620) Kapt3 IC: avoid running AP when API is not changed
- [`KT-17959`](https://youtrack.jetbrains.com/issue/KT-17959) Kapt3 doesn't preserve method parameter names for abstract methods
- [`KT-17999`](https://youtrack.jetbrains.com/issue/KT-17999) Cannot use KAPT3 1.1.2-4 in Android Studio java libs (null TypeCastException to WrappedVariantData<*> on Gradle Sync)

## 1.1.2

### Compiler

#### Front-end

- [`KT-16113`](https://youtrack.jetbrains.com/issue/KT-16113) Support destructuring parameters of suspend lambda with suspend componentX
- [`KT-3805`](https://youtrack.jetbrains.com/issue/KT-3805) Report error on double constants out of range
- [`KT-6014`](https://youtrack.jetbrains.com/issue/KT-6014) Wrong ABSTRACT_MEMBER_NOT_IMPLEMENTED for toString implemented by delegation
- [`KT-8959`](https://youtrack.jetbrains.com/issue/KT-8959) Missing diagnostic when trying to call inner class constructor qualificated with outer class name
- [`KT-12477`](https://youtrack.jetbrains.com/issue/KT-12477) Do not report 'const' inapplicability on property of error type
- [`KT-11010`](https://youtrack.jetbrains.com/issue/KT-11010) NDFDE for local object with type parameters
- [`KT-12881`](https://youtrack.jetbrains.com/issue/KT-12881) Descriptor wasn't found for declaration TYPE_PARAMETER
- [`KT-13342`](https://youtrack.jetbrains.com/issue/KT-13342) Unqualified super call should not resolve to a method of supertype overriden in another supertype
- [`KT-14236`](https://youtrack.jetbrains.com/issue/KT-14236) Allow to use emptyArray in annotation
- [`KT-14536`](https://youtrack.jetbrains.com/issue/KT-14536) IllegalStateException: Type parameter T not found for lazy class Companion at LazyDeclarationResolver visitTypeParameter
- [`KT-14865`](https://youtrack.jetbrains.com/issue/KT-14865) Throwable exception at KotlinParser parseLambdaExpression on typing { inside a string inside a lambda
- [`KT-15516`](https://youtrack.jetbrains.com/issue/KT-15516) Compiler error when passing suspending extension-functions as parameter and casting stuff to Any
- [`KT-15802`](https://youtrack.jetbrains.com/issue/KT-15802) Java constant referenced using subclass is not considered a constant expression
- [`KT-15872`](https://youtrack.jetbrains.com/issue/KT-15872) Constant folding is mistakenly triggered for user function
- [`KT-15901`](https://youtrack.jetbrains.com/issue/KT-15901) Unstable smart cast target after type check
- [`KT-15951`](https://youtrack.jetbrains.com/issue/KT-15951) Callable reference to class constructor from object is not resolved
- [`KT-16232`](https://youtrack.jetbrains.com/issue/KT-16232) Prohibit objects inside inner classes
- [`KT-16233`](https://youtrack.jetbrains.com/issue/KT-16233) Prohibit inner sealed classes
- [`KT-16250`](https://youtrack.jetbrains.com/issue/KT-16250) Import methods from typealias to object throws compiler exception  "Should be class or package: typealias"
- [`KT-16272`](https://youtrack.jetbrains.com/issue/KT-16272) Missing deprecation and SinceKotlin-related diagnostic for variable as function call
- [`KT-16278`](https://youtrack.jetbrains.com/issue/KT-16278) Public member method can't be used for callable reference because of private static with the same name
- [`KT-16372`](https://youtrack.jetbrains.com/issue/KT-16372) 'mod is deprecated' warning should not be shown when language version is 1.0
- [`KT-16484`](https://youtrack.jetbrains.com/issue/KT-16484) SimpleTypeImpl should not be created for error type: ErrorScope
- [`KT-16528`](https://youtrack.jetbrains.com/issue/KT-16528) Error: Loop in supertypes when using Java classes with type parameters having raw interdependent supertypes
- [`KT-16538`](https://youtrack.jetbrains.com/issue/KT-16538) No smart cast when equals is present
- [`KT-16782`](https://youtrack.jetbrains.com/issue/KT-16782) Enum entry is incorrectly forbidden on LHS of '::' with language version 1.0
- [`KT-16815`](https://youtrack.jetbrains.com/issue/KT-16815) Assertion error from compiler: unexpected classifier: class DeserializedTypeAliasDescriptor
- [`KT-16931`](https://youtrack.jetbrains.com/issue/KT-16931) Compiler cannot see inner class when for outer class exist folder with same name
- [`KT-16956`](https://youtrack.jetbrains.com/issue/KT-16956) Prohibit using function calls inside default parameter values of annotations
- [`KT-8187`](https://youtrack.jetbrains.com/issue/KT-8187) IAE on anonymous object in the delegation specifier list
- [`KT-8813`](https://youtrack.jetbrains.com/issue/KT-8813) Do not report unused parameters for anonymous functions
- [`KT-12112`](https://youtrack.jetbrains.com/issue/KT-12112) Do not consider nullability of error functions and properties for smart casts
- [`KT-12276`](https://youtrack.jetbrains.com/issue/KT-12276) No warning for unnecessary non-null assertion after method call with generic return type
- [`KT-13648`](https://youtrack.jetbrains.com/issue/KT-13648) Spurious warning: "Elvis operator (?:) always returns the left operand of non-nullable type (???..???)"
- [`KT-16264`](https://youtrack.jetbrains.com/issue/KT-16264) Forbid usage of _ without backticks
- [`KT-16875`](https://youtrack.jetbrains.com/issue/KT-16875) Decrease severity of unused parameter in lambda to weak warning
- [`KT-17136`](https://youtrack.jetbrains.com/issue/KT-17136) ModuleDescriptorImpl.allImplementingModules should be evaluated lazily
- [`KT-17214`](https://youtrack.jetbrains.com/issue/KT-17214) Do not show warning about useless elvis for error function types
- [`KT-13740`](https://youtrack.jetbrains.com/issue/KT-13740) Plugin crashes at accidentally wrong annotation argument type
- [`KT-17597`](https://youtrack.jetbrains.com/issue/KT-17597) Pattern::compile resolves to private instance method in 1.1.2

#### Back-end

- [`KT-8689`](https://youtrack.jetbrains.com/issue/KT-8689) NoSuchMethodError on local functions inside inlined lambda with variables captured from outer context
- [`KT-11314`](https://youtrack.jetbrains.com/issue/KT-11314) Abstract generic class with Array<Array<T>> parameter compiles fine but fails at runtime with "Bad type on operand stack" VerifyError
- [`KT-12839`](https://youtrack.jetbrains.com/issue/KT-12839) Two null checks are generated when manually null checking platform type
- [`KT-14565`](https://youtrack.jetbrains.com/issue/KT-14565) Cannot pop operand off empty stack when compiling enum class
- [`KT-14566`](https://youtrack.jetbrains.com/issue/KT-14566) Make kotlin.jvm.internal.Ref$...Ref classes serializable
- [`KT-14567`](https://youtrack.jetbrains.com/issue/KT-14567) VerifyError: Bad type on operand stack (generics with operator methods)
- [`KT-14607`](https://youtrack.jetbrains.com/issue/KT-14607) Incorrect class name "ava/lang/Void from AsyncTask extension function
- [`KT-14811`](https://youtrack.jetbrains.com/issue/KT-14811) Unecessary checkcast generated in parameterized functions.
- [`KT-14963`](https://youtrack.jetbrains.com/issue/KT-14963) unnecessary checkcast java/lang/Object
- [`KT-15105`](https://youtrack.jetbrains.com/issue/KT-15105) Comparing Chars in a Pair results in ClassCastException
- [`KT-15109`](https://youtrack.jetbrains.com/issue/KT-15109) Subclass from a type alias with named parameter in constructor will produce compiler exception
- [`KT-15192`](https://youtrack.jetbrains.com/issue/KT-15192) Compiler crashes on certain companion objects: "Error generating constructors of class Companion with kind IMPLEMENTATION"
- [`KT-15424`](https://youtrack.jetbrains.com/issue/KT-15424) javac crash when calling Kotlin function having generic varargs with default and @JvmOverloads
- [`KT-15574`](https://youtrack.jetbrains.com/issue/KT-15574) Can't instantiate Array through Type Alias
- [`KT-15594`](https://youtrack.jetbrains.com/issue/KT-15594) java.lang.VerifyError when referencing normal getter in @JvmStatic getters inside an object
- [`KT-15759`](https://youtrack.jetbrains.com/issue/KT-15759) tailrec suspend function fails to compile
- [`KT-15862`](https://youtrack.jetbrains.com/issue/KT-15862) Inline generic functions can unexpectedly box primitives
- [`KT-15871`](https://youtrack.jetbrains.com/issue/KT-15871) Unnecessary boxing for equality operator on inlined primitive values
- [`KT-15993`](https://youtrack.jetbrains.com/issue/KT-15993) Property annotations are stored in private fields and killed by obfuscators
- [`KT-15997`](https://youtrack.jetbrains.com/issue/KT-15997) Reified generics don't work properly with crossinline functions
- [`KT-16077`](https://youtrack.jetbrains.com/issue/KT-16077) Redundant private getter for private var in a class within a JvmMultifileClass annotated file
- [`KT-16194`](https://youtrack.jetbrains.com/issue/KT-16194) Code with unnecessary safe call contains redundant boxing/unboxing for primitive values
- [`KT-16245`](https://youtrack.jetbrains.com/issue/KT-16245) Redundant null-check generated for a cast of already non-nullable value
- [`KT-16532`](https://youtrack.jetbrains.com/issue/KT-16532) Kotlin 1.1 RC - Android cross-inline synchronized won't run
- [`KT-16555`](https://youtrack.jetbrains.com/issue/KT-16555) VerifyError: Bad type on operand stack
- [`KT-16713`](https://youtrack.jetbrains.com/issue/KT-16713) Insufficient maximum stack size
- [`KT-16720`](https://youtrack.jetbrains.com/issue/KT-16720) ClassCastException during compilation
- [`KT-16732`](https://youtrack.jetbrains.com/issue/KT-16732) Type 'java/lang/Number' (current frame, stack[0]) is not assignable to 'java/lang/Character
- [`KT-16929`](https://youtrack.jetbrains.com/issue/KT-16929) `VerifyError` when using bound method reference on generic property
- [`KT-16412`](https://youtrack.jetbrains.com/issue/KT-16412) Exception from compiler when try call SAM constructor where argument is callable reference to nested class inside object
- [`KT-17210`](https://youtrack.jetbrains.com/issue/KT-17210) Smartcast failure results in "Bad type operand on stack" runtime error

### Tools

- [`KT-15420`](https://youtrack.jetbrains.com/issue/KT-15420) Maven, all-open plugin: in console the settings of all-open are always reported as empty
- [`KT-11916`](https://youtrack.jetbrains.com/issue/KT-11916) Provide incremental compilation for Maven
- [`KT-15946`](https://youtrack.jetbrains.com/issue/KT-15946) Kotlin-JPA plugin support for @Embeddable
- [`KT-16627`](https://youtrack.jetbrains.com/issue/KT-16627) Do not make private members open in all-open plugin
- [`KT-16699`](https://youtrack.jetbrains.com/issue/KT-16699) Script resolving doesn't work with custom templates located in an external jar
- [`KT-16812`](https://youtrack.jetbrains.com/issue/KT-16812) import in .kts file does not works
- [`KT-16927`](https://youtrack.jetbrains.com/issue/KT-16927) Using `KotlinJsr223JvmLocalScriptEngineFactory` causes multiple warnings
- [`KT-15562`](https://youtrack.jetbrains.com/issue/KT-15562) Service is dying
- [`KT-17125`](https://youtrack.jetbrains.com/issue/KT-17125) > Failed to apply plugin [id 'kotlin'] > For input string: “”

#### Kapt

- [`KT-12432`](https://youtrack.jetbrains.com/issue/KT-12432) Dagger 2 does not generate Component which was referenced from Kotlin file.
- [`KT-8558`](https://youtrack.jetbrains.com/issue/KT-8558) KAPT only works with service-declared annotation processors
- [`KT-16753`](https://youtrack.jetbrains.com/issue/KT-16753) kapt3 generates invalid stubs when IC is enabled
- [`KT-16458`](https://youtrack.jetbrains.com/issue/KT-16458) kotlin-kapt / kapt3:  "cannot find symbol" error for companion object with same name as enclosing class
- [`KT-14478`](https://youtrack.jetbrains.com/issue/KT-14478) Add APT / Kapt support to the maven plugin
- [`KT-14070`](https://youtrack.jetbrains.com/issue/KT-14070) Kapt3: kapt doesn't compile generated Kotlin files and doesn't use the "kapt.kotlin.generated" folder anymore
- [`KT-16990`](https://youtrack.jetbrains.com/issue/KT-16990) Kapt3: java.io.File cannot be cast to java.lang.String
- [`KT-16965`](https://youtrack.jetbrains.com/issue/KT-16965) Error:Kotlin: Multiple values are not allowed for plugin option org.jetbrains.kotlin.kapt:output
- [`KT-16184`](https://youtrack.jetbrains.com/issue/KT-16184) AbstractMethodError in Kapt3ComponentRegistrar while compiling from IntelliJ  2016.3.4 using Kotlin 1.1.0-beta-38

#### Gradle

- [`KT-15084`](https://youtrack.jetbrains.com/issue/KT-15084) Navigation into sources of gradle-script-kotlin doesn't work
- [`KT-16003`](https://youtrack.jetbrains.com/issue/KT-16003) Gradle Plugin Fails When Run From Jenkins On Multiple Nodes
- [`KT-16585`](https://youtrack.jetbrains.com/issue/KT-16585) Kotlin Gradle Plugin makes using Gradle Java incremental compiler not work
- [`KT-16902`](https://youtrack.jetbrains.com/issue/KT-16902) Gradle plugin compilation on daemon fails on Linux ARM
- [`KT-14619`](https://youtrack.jetbrains.com/issue/KT-14619) Gradle: The '-d' option with a directory destination is ignored because '-module' is specified
- [`KT-12792`](https://youtrack.jetbrains.com/issue/KT-12792) Automatically configure standard library dependency and set its version equal to compiler version if not specified
- [`KT-15994`](https://youtrack.jetbrains.com/issue/KT-15994) Compiler arguments are not copied from the main compile task to kapt task
- [`KT-16820`](https://youtrack.jetbrains.com/issue/KT-16820) Changing compileKotlin.destinationDir leads to failure in :copyMainKotlinClasses task due to an NPE
- [`KT-16917`](https://youtrack.jetbrains.com/issue/KT-16917) First connection to daemon after start timeouts when DNS is slow
- [`KT-16580`](https://youtrack.jetbrains.com/issue/KT-16580) Kotlin gradle plugin cannot resolve the kotlin compiler 

### Android support

- [`KT-16624`](https://youtrack.jetbrains.com/issue/KT-16624) Implement quickfix "Add TargetApi/RequiresApi annotation" for Android api issues
- [`KT-16625`](https://youtrack.jetbrains.com/issue/KT-16625) Implement quickfix "Surround with if (VERSION.SDK_INT >= VERSION_CODES.SOME_VERSION) { ... }" for Android api issues
- [`KT-16840`](https://youtrack.jetbrains.com/issue/KT-16840) Kotlin Gradle plugin fails with Android Gradle plugin 2.4.0-alpha1
- [`KT-16897`](https://youtrack.jetbrains.com/issue/KT-16897) Gradle plugin 1.1.1 duplicates all main classes into Android instrumentation test APK
- [`KT-16957`](https://youtrack.jetbrains.com/issue/KT-16957) Android Extensions: Support Dialog class
- [`KT-15023`](https://youtrack.jetbrains.com/issue/KT-15023) Android `gradle installDebugAndroidTest` fails unless you first call `gradle assembleDebugAndroidTest`
- [`KT-12769`](https://youtrack.jetbrains.com/issue/KT-12769) "Name for method must be provided" error occurs on trying to use spaces in method name in integration tests in Android
- [`KT-12819`](https://youtrack.jetbrains.com/issue/KT-12819) Kotlin Lint: False positive for "Unconditional layout inflation" when using elvis operator
- [`KT-15116`](https://youtrack.jetbrains.com/issue/KT-15116) Kotlin Lint: problems in property accessors are not reported
- [`KT-15156`](https://youtrack.jetbrains.com/issue/KT-15156) Kotlin Lint: problems in annotation parameters are not reported
- [`KT-15179`](https://youtrack.jetbrains.com/issue/KT-15179) Kotlin Lint: problems inside local function are not reported
- [`KT-14870`](https://youtrack.jetbrains.com/issue/KT-14870) Kotlin Lint: problems inside local class are not reported
- [`KT-14920`](https://youtrack.jetbrains.com/issue/KT-14920) Kotlin Lint: "Android Lint for Kotlin | Incorrect support annotation usage" inspection does not report problems
- [`KT-14947`](https://youtrack.jetbrains.com/issue/KT-14947) Kotlin Lint: "Calling new methods on older versions" could suggest specific quick fixes
- [`KT-12741`](https://youtrack.jetbrains.com/issue/KT-12741) Android Extensions: Enable IDE plugin only if it is enabled in the build.gradle file
- [`KT-13122`](https://youtrack.jetbrains.com/issue/KT-13122) Implement '@RequiresApi' intention for android and don't report warning on annotated classes
- [`KT-16680`](https://youtrack.jetbrains.com/issue/KT-16680) Stack overflow in UAST containsLocalTypes()
- [`KT-15451`](https://youtrack.jetbrains.com/issue/KT-15451) Support "Android String Reference" folding in Kotlin files
- [`KT-16132`](https://youtrack.jetbrains.com/issue/KT-16132) Renaming property provided by kotlinx leads to renaming another members
- [`KT-17200`](https://youtrack.jetbrains.com/issue/KT-17200) Unable to build an android project
- [`KT-13104`](https://youtrack.jetbrains.com/issue/KT-13104) Incorrect resource name in Activity after renaming ID attribute value in layout file
- [`KT-17436`](https://youtrack.jetbrains.com/issue/KT-17436) Refactor | Rename android:id corrupts R.id references in kotlin code
- [`KT-17255`](https://youtrack.jetbrains.com/issue/KT-17255) Kotlin 1.1.2 EAP is broken with 2.4.0-alpha3
- [`KT-17610`](https://youtrack.jetbrains.com/issue/KT-17610) "Unknown reference: kotlinx"

### IDE

- [`KT-6159`](https://youtrack.jetbrains.com/issue/KT-6159) Inline Method refactoring
- [`KT-4578`](https://youtrack.jetbrains.com/issue/KT-4578) Intention to move property between class body and constructor parameter
- [`KT-8568`](https://youtrack.jetbrains.com/issue/KT-8568) Provide a QuickFix to replace type `Array<Int>` in annotation with `IntArray`
- [`KT-10393`](https://youtrack.jetbrains.com/issue/KT-10393) Detect calls to functions returning a lambda from expression body which ignore the return value
- [`KT-11393`](https://youtrack.jetbrains.com/issue/KT-11393) Inspection to highlight and warn on usage of internal members in other module from Java
- [`KT-12004`](https://youtrack.jetbrains.com/issue/KT-12004) IDE inspection that destructuring variable name matches the other name in data class
- [`KT-12183`](https://youtrack.jetbrains.com/issue/KT-12183) Intention converting several calls with same receiver to 'with'/`apply`/`run`
- [`KT-13111`](https://youtrack.jetbrains.com/issue/KT-13111) Support bound references in lambda-to-reference intention / inspection
- [`KT-15966`](https://youtrack.jetbrains.com/issue/KT-15966) Create quickfix for DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE
- [`KT-16074`](https://youtrack.jetbrains.com/issue/KT-16074) Introduce a quick-fix adding noinline modifier for a value parameter of suspend function type 
- [`KT-16131`](https://youtrack.jetbrains.com/issue/KT-16131) Add quickfix for: Cannot access member: it is invisible (private in supertype)
- [`KT-16188`](https://youtrack.jetbrains.com/issue/KT-16188) Add create class quickfix
- [`KT-16258`](https://youtrack.jetbrains.com/issue/KT-16258) Add intention to add missing components to destructuring assignment
- [`KT-16292`](https://youtrack.jetbrains.com/issue/KT-16292) Support "Reference to lambda" for bound references
- [`KT-11234`](https://youtrack.jetbrains.com/issue/KT-11234) Debugger won't hit breakpoint in nested lamba
- [`KT-12002`](https://youtrack.jetbrains.com/issue/KT-12002) Improve completion for closure parameters to work in more places
- [`KT-15768`](https://youtrack.jetbrains.com/issue/KT-15768) It would be nice to show in Kotlin facet what compiler plugins are on and their options
- [`KT-16022`](https://youtrack.jetbrains.com/issue/KT-16022) Kotlin facet: provide UI to navigate to project settings
- [`KT-16214`](https://youtrack.jetbrains.com/issue/KT-16214) Do not hide package kotlin.reflect.jvm.internal from auto-import and completion, inside package "kotlin.reflect"
- [`KT-16647`](https://youtrack.jetbrains.com/issue/KT-16647) Don't create kotlinc.xml if the settings don't differ from the defaults
- [`KT-16649`](https://youtrack.jetbrains.com/issue/KT-16649) All Gradle related classes should be moved to optional dependency section of plugin.xml
- [`KT-16800`](https://youtrack.jetbrains.com/issue/KT-16800) Autocomplete for closure with single arguments

#### Bug fixes

- [`KT-16316`](https://youtrack.jetbrains.com/issue/KT-16316) IDE: don't show Kotlin Scripting section when target platform is JavaScript
- [`KT-16317`](https://youtrack.jetbrains.com/issue/KT-16317) IDE: some fields stay enabled in an facet when use project settings was chosen
- [`KT-16596`](https://youtrack.jetbrains.com/issue/KT-16596) Hang in IntelliJ while scanning zips
- [`KT-16646`](https://youtrack.jetbrains.com/issue/KT-16646) The flag to enable coroutines does not sync from gradle file in Android Studio
- [`KT-16788`](https://youtrack.jetbrains.com/issue/KT-16788) Importing Kotlin Maven projects results in invalid .iml
- [`KT-16827`](https://youtrack.jetbrains.com/issue/KT-16827) kotlin javascript module not recognized by gradle sync when an android module is present
- [`KT-16848`](https://youtrack.jetbrains.com/issue/KT-16848) Regression: completion after dot in string interpolation expression doesn't work if there are no curly braces
- [`KT-16888`](https://youtrack.jetbrains.com/issue/KT-16888) "Multiple values are not allowed for plugin option org.jetbrains.kotlin.android:package" when rebuilding project
- [`KT-16980`](https://youtrack.jetbrains.com/issue/KT-16980) Accessing language version settings for a module performs runtime version detection on every access with no caching
- [`KT-16991`](https://youtrack.jetbrains.com/issue/KT-16991) Navigate to receiver from this in extension function
- [`KT-16992`](https://youtrack.jetbrains.com/issue/KT-16992) Navigate to lambda start from auto-generated 'it' parameter 
- [`KT-12264`](https://youtrack.jetbrains.com/issue/KT-12264) AssertionError: Resolver for 'completion/highlighting in LibrarySourceInfo for platform JVM' does not know how to resolve ModuleProductionSourceInfo
- [`KT-13734`](https://youtrack.jetbrains.com/issue/KT-13734) Annotated element search is slow
- [`KT-14710`](https://youtrack.jetbrains.com/issue/KT-14710) Sample references aren't resolved in IDE
- [`KT-16415`](https://youtrack.jetbrains.com/issue/KT-16415) Dependency leakage with Kotlin IntelliJ plugin, using gradle-script-kotlin, and the gradle-intellij-plugin
- [`KT-16837`](https://youtrack.jetbrains.com/issue/KT-16837) Slow typing in Kotlin file because of ImportFixBase
- [`KT-16926`](https://youtrack.jetbrains.com/issue/KT-16926) 'implement' dependency is not transitive when importing gradle project to IDEA
- [`KT-17141`](https://youtrack.jetbrains.com/issue/KT-17141) Running test from gutter icon fails in AS 2.4 Preview 3
- [`KT-17162`](https://youtrack.jetbrains.com/issue/KT-17162) Plain-text Java copy-paste to Kotlin file results in exception
- [`KT-16714`](https://youtrack.jetbrains.com/issue/KT-16714) J2K: Write access is allowed from event dispatch thread only
- [`KT-14058`](https://youtrack.jetbrains.com/issue/KT-14058) Unexpected error MISSING_DEPENDENCY_CLASS
- [`KT-9275`](https://youtrack.jetbrains.com/issue/KT-9275) Unhelpful IDE warning "Configure Kotlin"
- [`KT-15279`](https://youtrack.jetbrains.com/issue/KT-15279) 'Kotlin not configured message' should not be displayed while gradle sync is in progress
- [`KT-11828`](https://youtrack.jetbrains.com/issue/KT-11828) Configure Kotlin in Project: failure for Gradle modules without build.gradle (IDEA creates them)
- [`KT-16571`](https://youtrack.jetbrains.com/issue/KT-16571) Configure Kotlin in Project does not suggest just published version
- [`KT-16590`](https://youtrack.jetbrains.com/issue/KT-16590) Configure kotlin warning popup after each sync gradle
- [`KT-16353`](https://youtrack.jetbrains.com/issue/KT-16353) Configure Kotlin in Project: configurators are not suggested for Gradle module in non-Gradle project with separate sub-modules for source sets
- [`KT-16381`](https://youtrack.jetbrains.com/issue/KT-16381) Configure Kotlin dialog suggests modules already configured with other platforms
- [`KT-16401`](https://youtrack.jetbrains.com/issue/KT-16401) Configure Kotlin in the project adds incorrect dependency kotlin-stdlib-jre8 to 1.0.x language
- [`KT-12261`](https://youtrack.jetbrains.com/issue/KT-12261) Partial body resolve doesn't resolve anything in object literal used as an expression body of a method
- [`KT-13013`](https://youtrack.jetbrains.com/issue/KT-13013) "Go to Type Declaration" doesn't work for extension receiver and implict lambda parameter
- [`KT-13135`](https://youtrack.jetbrains.com/issue/KT-13135) IDE goes in an infinite indexing loop if a .kotlin_module file is corrupted
- [`KT-14129`](https://youtrack.jetbrains.com/issue/KT-14129) for/iter postfix templates should be applied for string, ranges and mutable collections
- [`KT-14134`](https://youtrack.jetbrains.com/issue/KT-14134) Allow to apply for/iter postfix template to map
- [`KT-14871`](https://youtrack.jetbrains.com/issue/KT-14871) Idea and Maven is not in sync with ModuleKind for Kotlin projects
- [`KT-14986`](https://youtrack.jetbrains.com/issue/KT-14986) Disable postfix completion when typing package statements
- [`KT-15200`](https://youtrack.jetbrains.com/issue/KT-15200) Show implementation should show inherited classes if a typealias to base class/interface is used
- [`KT-15398`](https://youtrack.jetbrains.com/issue/KT-15398) Annotations find usages in annotation instance site
- [`KT-15536`](https://youtrack.jetbrains.com/issue/KT-15536) Highlight usages: Class with primary constructor isn't highlighted when caret is on constructor invocation
- [`KT-15628`](https://youtrack.jetbrains.com/issue/KT-15628) Change error message if both KotlinJavaRuntime and KotlinJavaScript libraries are present in the module dependencies
- [`KT-15947`](https://youtrack.jetbrains.com/issue/KT-15947) Kotlin facet: Target platform on importing from a maven project should be filled the same way for different artifacts
- [`KT-16023`](https://youtrack.jetbrains.com/issue/KT-16023) Kotlin facet: When "Use project settings" is enabled, respective fields should show values from the project settings
- [`KT-16698`](https://youtrack.jetbrains.com/issue/KT-16698) Kotlin facet: modules created from different gradle sourcesets have the same module options
- [`KT-16700`](https://youtrack.jetbrains.com/issue/KT-16700) Kotlin facet: jdkHome path containing spaces splits into several additional args after import
- [`KT-16776`](https://youtrack.jetbrains.com/issue/KT-16776) Kotlin facet, import from maven: free arguments from submodule doesn't override arguments from parent module
- [`KT-16550`](https://youtrack.jetbrains.com/issue/KT-16550) Kotlin facet from Maven: provide error messages if additional command line parameters are set several times
- [`KT-16313`](https://youtrack.jetbrains.com/issue/KT-16313) Kotlin facet: unify filling up information about included AllOpen/NoArg plugins on importing from Maven and Gradle
- [`KT-16342`](https://youtrack.jetbrains.com/issue/KT-16342) Kotlin facet: JavaScript platform is not detected if there are 2 versions of stdlib in dependencies
- [`KT-16032`](https://youtrack.jetbrains.com/issue/KT-16032) Kotlin code formatter merges comment line with non-comment line
- [`KT-16038`](https://youtrack.jetbrains.com/issue/KT-16038) UI blocked on pasting java code into a kotlin file
- [`KT-16062`](https://youtrack.jetbrains.com/issue/KT-16062) Kotlin breakpoint doesn't work in some lambda in Rider project.
- [`KT-15855`](https://youtrack.jetbrains.com/issue/KT-15855) Can't evaluate expression in @JvmStatic method
- [`KT-16667`](https://youtrack.jetbrains.com/issue/KT-16667) Kotlin debugger "smart step into" fail on method defined in the middle of class hierarchy
- [`KT-16078`](https://youtrack.jetbrains.com/issue/KT-16078) Formatter puts empty body braces on different lines when KDoc is present
- [`KT-16265`](https://youtrack.jetbrains.com/issue/KT-16265) Parameter info doesn't work with type alias constructor
- [`KT-14727`](https://youtrack.jetbrains.com/issue/KT-14727) Wrong samples for some postfix templates

##### Inspections / Quickfixes

- [`KT-17002`](https://youtrack.jetbrains.com/issue/KT-17002) Make "Lambda to Reference" inspection off by default
- [`KT-14402`](https://youtrack.jetbrains.com/issue/KT-14402) Inspection "Use destructuring declaration" for lambdas doesn't work when parameter is of type Pair
- [`KT-16857`](https://youtrack.jetbrains.com/issue/KT-16857) False "Remove redundant 'let'" suggestion
- [`KT-16928`](https://youtrack.jetbrains.com/issue/KT-16928) Surround with null check quickfix works badly in case of qualifier
- [`KT-15870`](https://youtrack.jetbrains.com/issue/KT-15870) Move quick fix of "Package name does not match containing directory" inspection: Throwable "AWT events are not allowed inside write action"
- [`KT-16128`](https://youtrack.jetbrains.com/issue/KT-16128) 'Add label to loop' QF proposed when there's already a label
- [`KT-16828`](https://youtrack.jetbrains.com/issue/KT-16828) Don't suggest destructing declarations if not all components are used
- [`KT-17022`](https://youtrack.jetbrains.com/issue/KT-17022) Replace deprecated in the whole project may miss some usages in expression body

##### Refactorings, Intentions

- [`KT-7516`](https://youtrack.jetbrains.com/issue/KT-7516) Rename refactoring doesn't rename related labels
- [`KT-7520`](https://youtrack.jetbrains.com/issue/KT-7520) Exception when try rename label from usage
- [`KT-8955`](https://youtrack.jetbrains.com/issue/KT-8955) Refactor / Move package: KNPE at KotlinMoveDirectoryWithClassesHelper.postProcessUsages() with not matching package statement
- [`KT-11863`](https://youtrack.jetbrains.com/issue/KT-11863) Refactor / Move: moving referred file level elements to another package keeps reference to old FQN
- [`KT-13190`](https://youtrack.jetbrains.com/issue/KT-13190) Refactor / Move: no warning on moving class containing internal member to different module
- [`KT-13341`](https://youtrack.jetbrains.com/issue/KT-13341) Convert lambda to function reference intention is not available for object member calls
- [`KT-13755`](https://youtrack.jetbrains.com/issue/KT-13755) When (java?) class is moved redundant imports are not removed
- [`KT-13911`](https://youtrack.jetbrains.com/issue/KT-13911) Refactor / Move: "Problems Detected" dialog is not shown on moving whole .kt file
- [`KT-14401`](https://youtrack.jetbrains.com/issue/KT-14401) Can't rename implicit lambda parameter 'it' when caret is placed right after the last character
- [`KT-14483`](https://youtrack.jetbrains.com/issue/KT-14483) "Argument of NotNull parameter must be not null" in KotlinTryCatchSurrounder when using "try" postfix template
- [`KT-15075`](https://youtrack.jetbrains.com/issue/KT-15075) KNPE in "Specify explicit lambda signature"
- [`KT-15190`](https://youtrack.jetbrains.com/issue/KT-15190) Refactor / Move: false Problems Detected on moving class using parent's protected member
- [`KT-15250`](https://youtrack.jetbrains.com/issue/KT-15250) Convert anonymous object to lambda is shown when conversion not possible due implicit calls on this
- [`KT-15339`](https://youtrack.jetbrains.com/issue/KT-15339) Extract Superclass is enabled for any element: CommonRefactoringUtil$RefactoringErrorHintException: "Superclass cannot be extracted from interface" at ExtractSuperRefactoring.performRefactoring()
- [`KT-15559`](https://youtrack.jetbrains.com/issue/KT-15559) Kotlin: Moving classes to different packages breaks references to companion object's properties
- [`KT-15556`](https://youtrack.jetbrains.com/issue/KT-15556) Convert lambda to reference isn't proposed for parameterless constructor
- [`KT-15586`](https://youtrack.jetbrains.com/issue/KT-15586) ISE during "Move to a separate file"
- [`KT-15822`](https://youtrack.jetbrains.com/issue/KT-15822) Move class refactoring leaves unused imports
- [`KT-16108`](https://youtrack.jetbrains.com/issue/KT-16108) Cannot rename class on the companion object reference
- [`KT-16198`](https://youtrack.jetbrains.com/issue/KT-16198) Extract method refactoring should order parameters by first usage
- [`KT-17006`](https://youtrack.jetbrains.com/issue/KT-17006) Refactor / Move: usage of library function is reported as problem on move between modules with different library versions
- [`KT-17032`](https://youtrack.jetbrains.com/issue/KT-17032) Refactor / Move updates references to not moved class from the same file
- [`KT-11907`](https://youtrack.jetbrains.com/issue/KT-11907) Move to package renames file to temp.kt
- [`KT-16468`](https://youtrack.jetbrains.com/issue/KT-16468) Destructure declaration intention should be applicable for Pair
- [`KT-16162`](https://youtrack.jetbrains.com/issue/KT-16162) IAE for destructuring declaration entry from KotlinFinalClassOrFunSpringInspection
- [`KT-16556`](https://youtrack.jetbrains.com/issue/KT-16556) Move refactoring shows Refactoring cannot be performed warning.
- [`KT-16605`](https://youtrack.jetbrains.com/issue/KT-16605) NPE caused by Rename Refactoring of backing field when caret is after the last character
- [`KT-16809`](https://youtrack.jetbrains.com/issue/KT-16809) Move refactoring fails badly
- [`KT-16903`](https://youtrack.jetbrains.com/issue/KT-16903) "Convert to primary constructor" doesn't update supertype constructor call in supertypes list in case of implicit superclass constructor call

### JS

- [`KT-6627`](https://youtrack.jetbrains.com/issue/KT-6627) JS: test sources doesn't compile from IDE
- [`KT-13610`](https://youtrack.jetbrains.com/issue/KT-13610) JS: boxed Double.NaN is not equal to itself
- [`KT-16012`](https://youtrack.jetbrains.com/issue/KT-16012) JS: prohibit nested declarations, except interfaces inside external interface
- [`KT-16043`](https://youtrack.jetbrains.com/issue/KT-16043) IDL: mark inline helper function as InlineOnly
- [`KT-16058`](https://youtrack.jetbrains.com/issue/KT-16058) JS: getValue/setValue don't work if they are declared as suspend
- [`KT-16164`](https://youtrack.jetbrains.com/issue/KT-16164) JS: Bad getCallableRef in suspend function
- [`KT-16350`](https://youtrack.jetbrains.com/issue/KT-16350) KotlinJS - wrong code generated when temporary variables generated for RHS of `&&` operation
- [`KT-16377`](https://youtrack.jetbrains.com/issue/KT-16377) JS: losing declarations of temporary variables in secondary constructors
- [`KT-16545`](https://youtrack.jetbrains.com/issue/KT-16545) JS: ::class crashes at runtime for primitive types (e.g. Int::class, or Double::class)
- [`KT-16144`](https://youtrack.jetbrains.com/issue/KT-16144) JS: inliner can't find function called through inheritor ("fake" override) from another module

### Reflection

- [`KT-9453`](https://youtrack.jetbrains.com/issue/KT-9453) ClassCastException: java.lang.Class cannot be cast to kotlin.reflect.KClass
- [`KT-11254`](https://youtrack.jetbrains.com/issue/KT-11254) Make callable references Serializable on JVM
- [`KT-11316`](https://youtrack.jetbrains.com/issue/KT-11316) NPE in hashCode of KProperty object created for delegated property
- [`KT-12630`](https://youtrack.jetbrains.com/issue/KT-12630) KotlinReflectionInternalError on referencing some functions from stdlib
- [`KT-14731`](https://youtrack.jetbrains.com/issue/KT-14731) When starting application from test source root, kotlin function reflection fails in objects defined in sources

### Libraries

- [`KT-16922`](https://youtrack.jetbrains.com/issue/KT-16922) buildSequence/Iterator: Infinite sequence terminates prematurely
- [`KT-16923`](https://youtrack.jetbrains.com/issue/KT-16923) Progression iterator doesn't throw after completion
- [`KT-16994`](https://youtrack.jetbrains.com/issue/KT-16994) Classify sequence operations as stateful/stateless and intermediate/terminal
- [`KT-9786`](https://youtrack.jetbrains.com/issue/KT-9786) String.trimIndent doc is misleading
- [`KT-16572`](https://youtrack.jetbrains.com/issue/KT-16572) Add links to Mozilla Developer Network to kdocs of classes that we generate from IDL
- [`KT-16252`](https://youtrack.jetbrains.com/issue/KT-16252) IDL2K: Add ItemArrayLike interface implementation to collection-like classes

## 1.1.1

### IDE
- [`KT-16714`](https://youtrack.jetbrains.com/issue/KT-16714) J2K: Write access is allowed from event dispatch thread only

### Compiler
- [`KT-16801`](https://youtrack.jetbrains.com/issue/KT-16801) Accessors of `@PublishedApi` property gets mangled
- [`KT-16673`](https://youtrack.jetbrains.com/issue/KT-16673) Potentially problematic code causes exception when work with SAM adapters

### Libraries
- [`KT-16557`](https://youtrack.jetbrains.com/issue/KT-16557) Correct `SinceKotlin(1.1)` for all declarations in `kotlin.reflect.full`

## 1.1.1-RC

### IDE
- [`KT-16481`](https://youtrack.jetbrains.com/issue/KT-16481) Kotlin debugger & bytecode fail on select statement blocks (IllegalStateException: More than one package fragment)

### Gradle support
- [`KT-15783`](https://youtrack.jetbrains.com/issue/KT-15783) Gradle builds don't use incremental compilation due to an error: "Could not connect to kotlin daemon"
- [`KT-16434`](https://youtrack.jetbrains.com/issue/KT-16434) Gradle plugin does not compile androidTest sources when Jack is enabled
- [`KT-16546`](https://youtrack.jetbrains.com/issue/KT-16546) Enable incremental compilation in gradle by default

### Compiler
- [`KT-16184`](https://youtrack.jetbrains.com/issue/KT-16184) AbstractMethodError in Kapt3ComponentRegistrar while compiling from IntelliJ using Kotlin 1.1.0
- [`KT-16578`](https://youtrack.jetbrains.com/issue/KT-16578) Fix substitutor for synthetic SAM adapters
- [`KT-16581`](https://youtrack.jetbrains.com/issue/KT-16581) VerifyError when calling default value parameter with jvm-target 1.8
- [`KT-16583`](https://youtrack.jetbrains.com/issue/KT-16583) Cannot access private file-level variables inside a class init within the same file if a secondary constructor is present
- [`KT-16587`](https://youtrack.jetbrains.com/issue/KT-16587) AbstractMethodError: Delegates not generated correctly for private interfaces
- [`KT-16598`](https://youtrack.jetbrains.com/issue/KT-16598) Incorrect error: The feature "bound callable references" is only available since language version 1.1
- [`KT-16621`](https://youtrack.jetbrains.com/issue/KT-16621) Kotlin compiler doesn't report an error if a class implements Annotation interface but doesn't implement annotationType method
- [`KT-16441`](https://youtrack.jetbrains.com/issue/KT-16441) `NoSuchFieldError: $$delegatedProperties` when delegating through `provideDelegate` in companion object

### JavaScript support
- Prohibit function types with receiver as parameter types of external declarations
- Remove extension receiver for function parameters in `jQuery` declarations

## 1.1

### Compiler exceptions
- [`KT-16411`](https://youtrack.jetbrains.com/issue/KT-16411) Exception from compiler when try to inline callable reference to class constructor inside object
- [`KT-16412`](https://youtrack.jetbrains.com/issue/KT-16412) Exception from compiler when try call SAM constructor where argument is callable reference to nested class inside object
- [`KT-16413`](https://youtrack.jetbrains.com/issue/KT-16413) When we create sam adapter for java.util.function.Function we add incorrect null-check for argument

### Standard library
- [`KT-6561`](https://youtrack.jetbrains.com/issue/KT-6561) Drop java.util.Collections package from js stdlib
- `javaClass` extension property is no more deprecated due to migration problems

### IDE
- [`KT-16329`](https://youtrack.jetbrains.com/issue/KT-16329) Inspection "Calls to staic methods in Java interfaces..." always reports warning undependent of jvm-target


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


## 1.1-Beta2

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
- [`KT-15697`](https://youtrack.jetbrains.com/issue/KT-15697) If an annotation with AnnotationTarget.PROPERTY is tagged on a Kotlin property, it breaks annotation processing
- [`KT-15803`](https://youtrack.jetbrains.com/issue/KT-15803) Kotlin 1.0.6 broke Dagger
- [`KT-15814`](https://youtrack.jetbrains.com/issue/KT-15814) Regression: Kapt is not working in 1.0.6 / 1.1-M04 / 1.1-Beta
- [`KT-15838`](https://youtrack.jetbrains.com/issue/KT-15838) kapt3 1.1-beta: KaptError: Java file parsing error
- [`KT-15841`](https://youtrack.jetbrains.com/issue/KT-15841) 1.1-Beta + kapt3 fails to build the project with StackOverflowError
- [`KT-15915`](https://youtrack.jetbrains.com/issue/KT-15915) Kapt: Kotlin class target directory is cleared before compilation (and after kapt task)
- [`KT-16006`](https://youtrack.jetbrains.com/issue/KT-16006) Cannot determine if type is an error type during annotation processing

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
- [`KT-15868`](https://youtrack.jetbrains.com/issue/KT-15868) NPE when comparing nullable doubles for equality
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
- [`KT-15874`](https://youtrack.jetbrains.com/issue/KT-15874) Replace operator with function call replaces % with deprecated mod
- [`KT-15884`](https://youtrack.jetbrains.com/issue/KT-15884) False positive "Redundant .let call"
- [`KT-16072`](https://youtrack.jetbrains.com/issue/KT-16072) Intentions to convert suspend lambdas to callable references should not be shown

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


## 1.1.0-Beta

### Reflection
- [`KT-15540`](https://youtrack.jetbrains.com/issue/KT-15540) findAnnotation returns T?, but it throws NoSuchElementException when there is no matching annotation
- Reflection API in `kotlin-reflect` library is moved to `kotlin.reflect.full` package, declarations in the package `kotlin.reflect` are left deprecated. Please migrate according to the hints provided.

### Compiler

#### Coroutine support
- [`KT-15379`](https://youtrack.jetbrains.com/issue/KT-15379) Allow invoke on instances of suspend function type inside suspend function
- [`KT-15380`](https://youtrack.jetbrains.com/issue/KT-15380) Support suspend function type with value parameters
- [`KT-15391`](https://youtrack.jetbrains.com/issue/KT-15391) Prohibit suspend function type in supertype list
- [`KT-15392`](https://youtrack.jetbrains.com/issue/KT-15392) Prohibit local suspending function
- [`KT-15413`](https://youtrack.jetbrains.com/issue/KT-15413) Override regular functions with suspending ones and vice versa
- [`KT-15657`](https://youtrack.jetbrains.com/issue/KT-15657) Refine dispatchResume convention
- [`KT-15662`](https://youtrack.jetbrains.com/issue/KT-15662) Prohibit callable references to suspend functions

#### Diagnostics
- [`KT-9630`](https://youtrack.jetbrains.com/issue/KT-9630) Cannot create extension function on intersection of types
- [`KT-11398`](https://youtrack.jetbrains.com/issue/KT-11398) Possible false positive for INACCESSIBLE_TYPE
- [`KT-13593`](https://youtrack.jetbrains.com/issue/KT-13593) Do not report USELESS_ELVIS_RIGHT_IS_NULL for left argument with platform type
- [`KT-13859`](https://youtrack.jetbrains.com/issue/KT-13859) Wrong error about using unrepeatable annotation when mix implicit and explicit targets
- [`KT-14179`](https://youtrack.jetbrains.com/issue/KT-14179) Prohibit to use enum entry as type parameter
- [`KT-15097`](https://youtrack.jetbrains.com/issue/KT-15097) Inherited platform declarations clash: regression under 1.1 when indirectly inheriting from java.util.Map
- [`KT-15287`](https://youtrack.jetbrains.com/issue/KT-15287) Kotlin runtime 1.1 and runtime 1.0.x:  Overload resolution ambiguity
- [`KT-15334`](https://youtrack.jetbrains.com/issue/KT-15334) Incorrect "val cannot be reassigned" inside do-while
- [`KT-15410`](https://youtrack.jetbrains.com/issue/KT-15410) "Protected function call from public-API inline function" for protected constructor call

#### Kapt3
- [`KT-15145`](https://youtrack.jetbrains.com/issue/KT-15145) Kapt3: Doesn't compile with multiple errors 
- [`KT-15232`](https://youtrack.jetbrains.com/issue/KT-15232) Kapt3 crash due to java codepage
- [`KT-15359`](https://youtrack.jetbrains.com/issue/KT-15359) Kapt3 exception while annotation processing (DataBindings AS2.3-beta1)
- [`KT-15375`](https://youtrack.jetbrains.com/issue/KT-15375) Kapt3 can't find ${env.JDK_18}/lib/tools.jar
- [`KT-15381`](https://youtrack.jetbrains.com/issue/KT-15381) Unresolved references: R with Kapt3
- [`KT-15397`](https://youtrack.jetbrains.com/issue/KT-15397) Kapt3 doesn't work with databinding
- [`KT-15409`](https://youtrack.jetbrains.com/issue/KT-15409) Kapt3 Cannot find the getter for attribute 'android:text' with value type java.lang.String on android.widget.EditText.
- [`KT-15421`](https://youtrack.jetbrains.com/issue/KT-15421) Kapt3: Substitute types from Psi instead of writing NonExistentClass for generated type names
- [`KT-15459`](https://youtrack.jetbrains.com/issue/KT-15459) Kapt3 doesn't generate code in test module
- [`KT-15524`](https://youtrack.jetbrains.com/issue/KT-15524) Kapt3 - Error messages should display associated element information (if available)
- [`KT-15713`](https://youtrack.jetbrains.com/issue/KT-15713) Kapt3: circular dependencies between Gradke tasks

#### Exceptions / Errors
- [`KT-11401`](https://youtrack.jetbrains.com/issue/KT-11401) Error type encountered for implicit invoke with function literal argument
- [`KT-12044`](https://youtrack.jetbrains.com/issue/KT-12044) Assertion "Rewrite at slice LEXICAL_SCOPE" for 'if' with property references 
- [`KT-14011`](https://youtrack.jetbrains.com/issue/KT-14011) Compiler crash when inlining: lateinit property allRecapturedParameters has not been initialized
- [`KT-14868`](https://youtrack.jetbrains.com/issue/KT-14868) CCE in runtime while converting Number to Char
- [`KT-15364`](https://youtrack.jetbrains.com/issue/KT-15364) VerifyError: Bad type on operand stack on ObserverIterator.hasNext
- [`KT-15373`](https://youtrack.jetbrains.com/issue/KT-15373) Internal error when running TestNG test
- [`KT-15437`](https://youtrack.jetbrains.com/issue/KT-15437) VerifyError: Bad local variable type on simplest provideDelegate
- [`KT-15446`](https://youtrack.jetbrains.com/issue/KT-15446) Property reference on an instance of subclass causes java.lang.VerifyError
- [`KT-15447`](https://youtrack.jetbrains.com/issue/KT-15447) Compiler backend error: "Don't know how to generate outer expression for class"
- [`KT-15449`](https://youtrack.jetbrains.com/issue/KT-15449) Back-end (JVM) Internal error: Couldn't inline method call
- [`KT-15464`](https://youtrack.jetbrains.com/issue/KT-15464) Regression: "Supertypes of the following classes cannot be resolved. Please make sure you have the required dependencies in the classpath:"
- [`KT-15575`](https://youtrack.jetbrains.com/issue/KT-15575) VerifyError: Bad type on operand stack

#### Various issues
- [`KT-11962`](https://youtrack.jetbrains.com/issue/KT-11962) Super call with default parameters check is generated for top-level function
- [`KT-11969`](https://youtrack.jetbrains.com/issue/KT-11969) ProGuard issue with private interface methods
- [`KT-12795`](https://youtrack.jetbrains.com/issue/KT-12795) Write information about sealed class inheritors to metadata
- [`KT-13718`](https://youtrack.jetbrains.com/issue/KT-13718) ClassFormatError on aspectj instrumentation
- [`KT-14162`](https://youtrack.jetbrains.com/issue/KT-14162) Support @InlineOnly on inline properties
- [`KT-14705`](https://youtrack.jetbrains.com/issue/KT-14705) Inconsistent smart casts on when enum subject
- [`KT-14917`](https://youtrack.jetbrains.com/issue/KT-14917) No way to pass additional java command line options to kontlinc on Windows
- [`KT-15112`](https://youtrack.jetbrains.com/issue/KT-15112) Compiler hangs on nested lock compilation
- [`KT-15225`](https://youtrack.jetbrains.com/issue/KT-15225) Scripts: generate classes with names that are valid Java identifiers
- [`KT-15411`](https://youtrack.jetbrains.com/issue/KT-15411) Unnecessary CHECKCAST bytecode when dealing with null
- [`KT-15473`](https://youtrack.jetbrains.com/issue/KT-15473) Invalid KFunction byte code signature for callable references
- [`KT-15582`](https://youtrack.jetbrains.com/issue/KT-15582) Generated bytecode is sometimes incompatible with Java 9
- [`KT-15584`](https://youtrack.jetbrains.com/issue/KT-15584) Do not mark class files compiled with a release language version as pre-release
- [`KT-15589`](https://youtrack.jetbrains.com/issue/KT-15589) Upper bound for T in KClass<T> can be implicitly violated using generic function
- [`KT-15631`](https://youtrack.jetbrains.com/issue/KT-15631) Compiler hang in MethodAnalyzer.analyze() fixed 

### JavaScript backend

#### Coroutine support
- [`KT-15362`](https://youtrack.jetbrains.com/issue/KT-15362) JS: Regex doesn't work (properly) in coroutine
- [`KT-15366`](https://youtrack.jetbrains.com/issue/KT-15366) JS: error when calling inline function with optional parameters from another module inside coroutine lambda
- [`KT-15367`](https://youtrack.jetbrains.com/issue/KT-15367) JS: `for` against iterator with suspend `next` and `hasNext` functions does not work
- [`KT-15400`](https://youtrack.jetbrains.com/issue/KT-15400) suspendCoroutine is missing in JS BE
- [`KT-15597`](https://youtrack.jetbrains.com/issue/KT-15597) Support non-tail suspend calls inside named suspend functions 
- [`KT-15625`](https://youtrack.jetbrains.com/issue/KT-15625) JS: return statement without value surrounded by `try..finally` in suspend lambda causes compiler error
- [`KT-15698`](https://youtrack.jetbrains.com/issue/KT-15698) Move coroutine intrinsics to kotlin.coroutine.intrinsics package

#### Diagnostics
- [`KT-14577`](https://youtrack.jetbrains.com/issue/KT-14577) JS: do not report declaration clash when common redeclaration diagnostic applies
- [`KT-15136`](https://youtrack.jetbrains.com/issue/KT-15136) JS: prohibit inheritance from kotlin Function{N} interfaces

#### Language features support
- [`KT-12194`](https://youtrack.jetbrains.com/issue/KT-12194) Exhaustiveness check isn't generated for when expressions in JS at all
- [`KT-15590`](https://youtrack.jetbrains.com/issue/KT-15590) Support increment on inlined properties
 
#### Native / external
- [`KT-8081`](https://youtrack.jetbrains.com/issue/KT-8081) JS: native inherited class shouldn't require super or primary constructor call
- [`KT-13892`](https://youtrack.jetbrains.com/issue/KT-13892) JS: restrictions for native (external) functions and properties
- [`KT-15307`](https://youtrack.jetbrains.com/issue/KT-15307) JS: prohibit inline members inside external declarations
- [`KT-15308`](https://youtrack.jetbrains.com/issue/KT-15308) JS: prohibit non-abstract members inside external interfaces except nullable properties (with accessors)

#### Exceptions / Errors
- [`KT-7302`](https://youtrack.jetbrains.com/issue/KT-7302) KotlinJS - Trait with optional parameter causes compilation error
- [`KT-15325`](https://youtrack.jetbrains.com/issue/KT-15325) JS: ReferenceError: $receiver is not defined
- [`KT-15357`](https://youtrack.jetbrains.com/issue/KT-15357) JS: `when` expression in primary-from-secondary constructor call
- [`KT-15435`](https://youtrack.jetbrains.com/issue/KT-15435) Call to 'synchronize' crashes JS backend
- [`KT-15513`](https://youtrack.jetbrains.com/issue/KT-15513) JS: empty do..while loop crashes compiler

#### Various issues
- [`KT-4160`](https://youtrack.jetbrains.com/issue/KT-4160) JS: compiler produces wrong code for escaped variable names with characters which Illegal in JS (e.g. spaces)
- [`KT-7004`](https://youtrack.jetbrains.com/issue/KT-7004) JS: functions named `call` not inlined
- [`KT-7588`](https://youtrack.jetbrains.com/issue/KT-7588) JS: operators are not inlined
- [`KT-7733`](https://youtrack.jetbrains.com/issue/KT-7733) JS: Provide overflow behavior for integer arithmetic operations
- [`KT-8413`](https://youtrack.jetbrains.com/issue/KT-8413) JS: generated wrong code for some float constants
- [`KT-12598`](https://youtrack.jetbrains.com/issue/KT-12598) JS: comparisons for Enums always translates using strong operator
- [`KT-13523`](https://youtrack.jetbrains.com/issue/KT-13523) Augmented assignment with array access in LHS is translated incorrectly
- [`KT-13888`](https://youtrack.jetbrains.com/issue/KT-13888) JS: change how functions optional parameters get translated
- [`KT-15260`](https://youtrack.jetbrains.com/issue/KT-15260) JS: don't import module more than once
- [`KT-15475`](https://youtrack.jetbrains.com/issue/KT-15475) JS compiler deletes internal function name in js("") text block
- [`KT-15506`](https://youtrack.jetbrains.com/issue/KT-15506) JS: invalid evaluation order when passing arguments to function by name
- [`KT-15512`](https://youtrack.jetbrains.com/issue/KT-15512) JS: wrong result when use break/throw/return in || and && operators
- [`KT-15569`](https://youtrack.jetbrains.com/issue/KT-15569) js: Wrong code generated when calling an overloaded operator function on an inherited property

### Standard Library
- [`KEEP-23`](https://github.com/Kotlin/KEEP/blob/master/proposals/stdlib/group-and-fold.md) Operation to group by key and fold each group simultaneously
- [`KT-15774`](https://youtrack.jetbrains.com/issue/KT-15774) `buildSequence` and `buildIterator` functions with `yield` and `yieldAll` based on coroutines
- [`KT-6903`](https://youtrack.jetbrains.com/issue/KT-6903) Add `also` extension, which is like `apply`, but with `it` instead of `this` inside lambda.
- [`KT-7858`](https://youtrack.jetbrains.com/issue/KT-7858) Add extension function `takeIf` to match a value against predicate and return null when it does not match
- [`KT-11851`](https://youtrack.jetbrains.com/issue/KT-11851) Provide extension `Map.getValue(key: K): V` which throws or returns default when key is not found
- [`KT-7417`](https://youtrack.jetbrains.com/issue/KT-7417) Add min, max on two numbers to standard library
- [`KT-13898`](https://youtrack.jetbrains.com/issue/KT-13898) Allow to implement `toArray` in collections as protected and provide protected toArray in AbstractCollection.
- [`KT-14935`](https://youtrack.jetbrains.com/issue/KT-14935) Array-like list instantiation functions: `List(count) { init }` and `MutableList(count) { init }` 
- [`KT-15630`](https://youtrack.jetbrains.com/issue/KT-15630) Overloads of mutableListOf, mutableSetOf, mutableMapOf without parameters
- [`KT-15557`](https://youtrack.jetbrains.com/issue/KT-15557) Iterable<T>.joinTo loses information about each element by calling toString on them by default
- [`KT-15477`](https://youtrack.jetbrains.com/issue/KT-15477) Introduce Throwable.addSuppressed extension
- [`KT-15310`](https://youtrack.jetbrains.com/issue/KT-15310) Add dynamic.unsafeCast
- [`KT-15436`](https://youtrack.jetbrains.com/issue/KT-15436) JS stdlib: org.w3c.fetch.RequestInit has 12 parameters, all required
- [`KT-15458`](https://youtrack.jetbrains.com/issue/KT-15458) Add print and println to common stdlib

### IDE
- Project View: Fix presentation of Kotlin files and their members when @JvmName having the same name as the file itself

#### no-arg / all-open
- [`KT-15419`](https://youtrack.jetbrains.com/issue/KT-15419) IDE build doesn't pick settings of all-open plugin
- [`KT-15686`](https://youtrack.jetbrains.com/issue/KT-15686) IDE build doesn't pick settings of no-arg plugin
- [`KT-15735`](https://youtrack.jetbrains.com/issue/KT-15735) Facet loses compiler plugin settings on reopening project, when "Use project settings" = Yes

#### Formatter
- [`KT-15542`](https://youtrack.jetbrains.com/issue/KT-15542) Formatter doesn't handle spaces around 'by' keyword
- [`KT-15544`](https://youtrack.jetbrains.com/issue/KT-15544) Formatter doesn't remove spaces around function reference operator

#### Intention actions, inspections and quick-fixes

##### New features
- Implement quickfix which enables/disables coroutine support in module or project
- [`KT-5045`](https://youtrack.jetbrains.com/issue/KT-5045) Intention to convert between two comparisons and range check and vice versa
- [`KT-5629`](https://youtrack.jetbrains.com/issue/KT-5629) Quick-fix to import extension method when arguments of non-extension method do not match
- [`KT-6217`](https://youtrack.jetbrains.com/issue/KT-6217) Add warning for unused equals expression
- [`KT-6824`](https://youtrack.jetbrains.com/issue/KT-6824) Quick-fix for applying spread operator where vararg is expected
- [`KT-8855`](https://youtrack.jetbrains.com/issue/KT-8855) Implement "Create label" quick-fix
- [`KT-15056`](https://youtrack.jetbrains.com/issue/KT-15056) Implement intention which converts object literal to class
- [`KT-15068`](https://youtrack.jetbrains.com/issue/KT-15068) Implement intention which rename file according to the top-level class name
- [`KT-15564`](https://youtrack.jetbrains.com/issue/KT-15564) Add quick-fix for changing primitive cast to primitive conversion method

##### Bug fixes
- [`KT-14630`](https://youtrack.jetbrains.com/issue/KT-14630) Clearer diagnostic message for platform type inspection
- [`KT-14745`](https://youtrack.jetbrains.com/issue/KT-14745) KNPE in convert primary constructor to secondary
- [`KT-14889`](https://youtrack.jetbrains.com/issue/KT-14889) Replace 'if' with elvis operator produces red code if result is referenced in 'if'
- [`KT-14907`](https://youtrack.jetbrains.com/issue/KT-14907) Quick-fix for missing operator adds infix modifier to created function
- [`KT-15092`](https://youtrack.jetbrains.com/issue/KT-15092) Suppress inspection "use property access syntax" for some getters and fix completion for them
- [`KT-15227`](https://youtrack.jetbrains.com/issue/KT-15227) "Replace if with elvis" silently changes semantics
- [`KT-15412`](https://youtrack.jetbrains.com/issue/KT-15412) "Join declaration and assignment" can break code with smart casts
- [`KT-15501`](https://youtrack.jetbrains.com/issue/KT-15501) Intention "Add names to call arguments" shouldn't appear when the only argument is a trailing lambda

#### Refactorings (Extract / Pull)
- [`KT-15611`](https://youtrack.jetbrains.com/issue/KT-15611) Extract Interface/Superclass: Disable const-properties
- Pull Up: Fix pull-up from object to superclass
- [`KT-15602`](https://youtrack.jetbrains.com/issue/KT-15602) Extract Interface/Superclass: Disable "Make abstract" for inline/external/lateinit members
- Extract Interface: Disable inline/external/lateinit members
- [`KT-12704`](https://youtrack.jetbrains.com/issue/KT-12704), [`KT-15583`](https://youtrack.jetbrains.com/issue/KT-15583) Override/Implement Members: Support all nullability annotations respected by the Kotlin compiler
- [`KT-15563`](https://youtrack.jetbrains.com/issue/KT-15563) Override Members: Allow overriding virtual synthetic members (e.g. equals(), hashCode(), toString(), etc.) in data classes
- [`KT-15355`](https://youtrack.jetbrains.com/issue/KT-15355) Extract Interface: Disable "Make abstract" and assume it to be true for abstract members of an interface
- [`KT-15353`](https://youtrack.jetbrains.com/issue/KT-15353) Extract Superclass/Interface: Allow extracting class with special name (and quotes)
- [`KT-15643`](https://youtrack.jetbrains.com/issue/KT-15643) Extract Interface/Pull Up: Disable "Make abstract" and assume it to be true for primary constructor parameter when moving to an interface
- [`KT-15607`](https://youtrack.jetbrains.com/issue/KT-15607) Extract Interface/Pull Up: Disable internal/protected members when moving to an interface
- [`KT-15640`](https://youtrack.jetbrains.com/issue/KT-15640) Extract Interface/Pull Up: Drop 'final' modifier when moving to an interface
- [`KT-15639`](https://youtrack.jetbrains.com/issue/KT-15639) Extract Superclass/Interface/Pull Up: Add spaces between 'abstract' modifier and annotations
- [`KT-15606`](https://youtrack.jetbrains.com/issue/KT-15606) Extract Interface/Pull Up: Warn about private members with usages in the original class
- [`KT-15635`](https://youtrack.jetbrains.com/issue/KT-15635) Extract Superclass/Interface: Fix bogus visibility warning inside a member when it's being moved as abstract
- [`KT-15598`](https://youtrack.jetbrains.com/issue/KT-15598) Extract Interface: Red-highlight members inherited from a super-interface when that interface reference itself is not extracted
- [`KT-15674`](https://youtrack.jetbrains.com/issue/KT-15674) Extract Superclass: Drop inapplicable modifiers when converting property-parameter to ordinary parameter

#### Multi-platform project support
- [`KT-14908`](https://youtrack.jetbrains.com/issue/KT-14908) Actions (quick-fixes) to create implementations of header elements
- [`KT-15305`](https://youtrack.jetbrains.com/issue/KT-15305) Do not report UNUSED for header declarations with implementations and vice versa
- [`KT-15601`](https://youtrack.jetbrains.com/issue/KT-15601) Cannot suppress HEADER_WITHOUT_IMPLEMENTATION
- [`KT-15641`](https://youtrack.jetbrains.com/issue/KT-15641) Quick-fix "Create header interface implementation" does nothing

#### Android support

- [`KT-12884`](https://youtrack.jetbrains.com/issue/KT-12884) Android Extensions: Refactor / Rename of activity name does not change import extension statement
- [`KT-14308`](https://youtrack.jetbrains.com/issue/KT-14308) Android Studio randomly hangs due to Java static member import quick-fix lags
- [`KT-14358`](https://youtrack.jetbrains.com/issue/KT-14358) Kotlin extensions: rename layout file: Throwable: "PSI and index do not match" through KotlinFullClassNameIndex.get()
- [`KT-15483`](https://youtrack.jetbrains.com/issue/KT-15483) Kotlin lint throws unexpected exceptions in IDE

#### Various issues
- [`KT-12872`](https://youtrack.jetbrains.com/issue/KT-12872) Don't show "defined in <very long qualifier here>" in quick doc for local variables
- [`KT-13001`](https://youtrack.jetbrains.com/issue/KT-13001) "Go to Type Declaration" is broken for stdlib types
- [`KT-13067`](https://youtrack.jetbrains.com/issue/KT-13067) Syntax colouring doesn't work for KDoc tags
- [`KT-14815`](https://youtrack.jetbrains.com/issue/KT-14815) alt + enter -> "import" over a constructor reference is not working
- [`KT-14819`](https://youtrack.jetbrains.com/issue/KT-14819) Quick documentation for special Enum functions doesn't work
- [`KT-15141`](https://youtrack.jetbrains.com/issue/KT-15141) Bogus import popup for when function call cannot be resolved fully
- [`KT-15154`](https://youtrack.jetbrains.com/issue/KT-15154) IllegalStateException on attempt to convert import statement to * if last added import is to typealias
- [`KT-15329`](https://youtrack.jetbrains.com/issue/KT-15329) Regex not inspected properly for javaJavaIdentifierStart and javaJavaIdentifierPart
- [`KT-15383`](https://youtrack.jetbrains.com/issue/KT-15383) Kotlin Scripts can only resolve stdlib functions/classes if they are in a source directory
- [`KT-15440`](https://youtrack.jetbrains.com/issue/KT-15440) Improve extensions detection in IDEA
- [`KT-15548`](https://youtrack.jetbrains.com/issue/KT-15548) Kotlin plugin: @Language injections specified in another module are ignored
- Invoke `StorageComponentContainerContributor` extension for module dependencies container as well (needed for "sam-with-receiver" plugin to work with scripts)

### J2K
- [`KT-6790`](https://youtrack.jetbrains.com/issue/KT-6790) J2K: Static import of Map.Entry is lost during conversion
- [`KT-14736`](https://youtrack.jetbrains.com/issue/KT-14736) J2K: Incorrect conversion of back ticks in javadoc {@code} tag
- [`KT-15027`](https://youtrack.jetbrains.com/issue/KT-15027) J2K: Annotations are set on functions, but not on property accessors

### Gradle support

- [`KT-15376`](https://youtrack.jetbrains.com/issue/KT-15376) Kotlin incremental=true: fixed compatibility with AS 2.3
- [`KT-15433`](https://youtrack.jetbrains.com/issue/KT-15433) Kotlin daemon swallows exceptions: fixed stack trace reporting
- [`KT-15682`](https://youtrack.jetbrains.com/issue/KT-15682) Uncheck "Use project settings" option on import Kotlin project from gradle


## 1.1-M04 (EAP-4)

### Language related changes

- [`KT-4481`](https://youtrack.jetbrains.com/issue/KT-4481) compareTo on primitive floats/doubles should behave naturally
- [`KT-11016`](https://youtrack.jetbrains.com/issue/KT-11016) Allow to annotate internal API to be used inside public inline functions
- [`KT-11128`](https://youtrack.jetbrains.com/issue/KT-11128) Member vs SAM conversion with more specific signature
- [`KT-12215`](https://youtrack.jetbrains.com/issue/KT-12215) Allowing to access protected members in public inline members creates potential binary compatibility problem
- [`KT-12531`](https://youtrack.jetbrains.com/issue/KT-12531) Report error when delegated member hides a supertype member
- [`KT-14650`](https://youtrack.jetbrains.com/issue/KT-14650) mod function on integral types is inconsistent with BigInteger.mod
- [`KT-14651`](https://youtrack.jetbrains.com/issue/KT-14651) Floating point comparisons shall operate according to IEEE754
- [`KT-14852`](https://youtrack.jetbrains.com/issue/KT-14852) It should not be possible to use typealias that abbreviates a generic projection as a constructor
- [`KT-15226`](https://youtrack.jetbrains.com/issue/KT-15226) Restrict delegation to java 8 default methods

### Reflection

- [`KT-12250`](https://youtrack.jetbrains.com/issue/KT-12250) Provide API for getting a single annotation by its class
- [`KT-14939`](https://youtrack.jetbrains.com/issue/KT-14939) VerifyError in accessors for bound property reference with receiver 'null'

### Compiler

#### Coroutines

- Major coroutines redesign - see [`KEEP`](https://github.com/Kotlin/kotlin-coroutines/blob/master/kotlin-coroutines-informal.md) for details

#### Optimizations

- [`KT-11734`](https://youtrack.jetbrains.com/issue/KT-11734) Optimize const vals by inlining them at call site
- [`KT-13570`](https://youtrack.jetbrains.com/issue/KT-13570) Generate TABLE/LOOKUPSWITCH if all when branches are const integer values
- [`KT-14746`](https://youtrack.jetbrains.com/issue/KT-14746) Captured Refs should not be volatile

#### Various issues

- [`KT-10982`](https://youtrack.jetbrains.com/issue/KT-10982) java.util.Map::compute* poor usability
- [`KT-12144`](https://youtrack.jetbrains.com/issue/KT-12144) Type inference incorporation error on SAM adapter call
- [`KT-14196`](https://youtrack.jetbrains.com/issue/KT-14196) Do not allow class literal with expression in annotation arguments
- [`KT-14453`](https://youtrack.jetbrains.com/issue/KT-14453) Regression: Type inference failed: inferred type is T but T was expected
- [`KT-14774`](https://youtrack.jetbrains.com/issue/KT-14774) Incorrect inner class modifier generated for sealed inner classes
- [`KT-14839`](https://youtrack.jetbrains.com/issue/KT-14839) CompilationException when calling inline fun with first arg of 2 (w/defaults) within catch block of Java exception type
- [`KT-14855`](https://youtrack.jetbrains.com/issue/KT-14855) Projection in type aliases should be allowed in supertypes and constructor invocations if they expand to non-toplevel projections
- [`KT-14887`](https://youtrack.jetbrains.com/issue/KT-14887) Unhelpful error "public-API inline function cannot access non-public-API" for unresolved call inside inline function
- [`KT-14930`](https://youtrack.jetbrains.com/issue/KT-14930) Android: creating Kotlin activity: UOE at EmptyList.removeAll()
- [`KT-15146`](https://youtrack.jetbrains.com/issue/KT-15146) Kapt3 no source files on unittest
- [`KT-15272`](https://youtrack.jetbrains.com/issue/KT-15272) Exception when building 2 projects at the same time

### JavaScript backend

#### dynamic type

- [`KT-8207`](https://youtrack.jetbrains.com/issue/KT-8207) Extension function on dynamic resolves on any type
- [`KT-6579`](https://youtrack.jetbrains.com/issue/KT-6579) JS: prohibit to use `in` and `!in` on dynamic
- [`KT-6580`](https://youtrack.jetbrains.com/issue/KT-6580) JS: prohibit to use more than one argument in indexed access on dynamic
- [`KT-13615`](https://youtrack.jetbrains.com/issue/KT-13615) JS: don't generate guard for catch with dynamic type

#### @native/external

- [`KT-13893`](https://youtrack.jetbrains.com/issue/KT-13893) JS: Replace @native annotation with external modifier
- [`KT-12877`](https://youtrack.jetbrains.com/issue/KT-12877) Allow to specify module for native JS declarations
- [`KT-14806`](https://youtrack.jetbrains.com/issue/KT-14806) JS: name of a local variable clashes with native declaration from global scope

#### Diagnostics

- [`KT-13889`](https://youtrack.jetbrains.com/issue/KT-13889) JS: prohibit overriding native functions with default values assigned to parameters
- [`KT-13894`](https://youtrack.jetbrains.com/issue/KT-13894) JS: prohibit native declaration inside non-native
- [`KT-13895`](https://youtrack.jetbrains.com/issue/KT-13895) JS: RUNTIME annotations
- [`KT-13896`](https://youtrack.jetbrains.com/issue/KT-13896) JS: prohibit external(native) extension functions and properties
- [`KT-13897`](https://youtrack.jetbrains.com/issue/KT-13897) JS: prohibit native(external) files and typealiases
- [`KT-13910`](https://youtrack.jetbrains.com/issue/KT-13910) JS: prohibit override members of native declaration with overloads
- [`KT-14027`](https://youtrack.jetbrains.com/issue/KT-14027) JS: prohibit native inner classes
- [`KT-14029`](https://youtrack.jetbrains.com/issue/KT-14029) JS: prohibit private members inside native declarations
- [`KT-14037`](https://youtrack.jetbrains.com/issue/KT-14037) JS: prohibit using native interfaces in RHS of IS
- [`KT-14038`](https://youtrack.jetbrains.com/issue/KT-14038) JS: warn when using native interface in RHS of AS
- [`KT-15130`](https://youtrack.jetbrains.com/issue/KT-15130) JS: prohibit inheritance native from non-native
- [`KT-12600`](https://youtrack.jetbrains.com/issue/KT-12600) JS: type check with a native interface compiles but crash at runtime
- [`KT-13307`](https://youtrack.jetbrains.com/issue/KT-13307) KotlinJS cannot cast to a marker interface.

#### Language features support

- [`KT-13573`](https://youtrack.jetbrains.com/issue/KT-13573) JS: support bound callable reference
- [`KT-14634`](https://youtrack.jetbrains.com/issue/KT-14634) JS: support enumValues / enumValueOf
- [`KT-15058`](https://youtrack.jetbrains.com/issue/KT-15058) JS: replace suspend function convention

#### Issues related to kotlin.Any

- [`KT-7664`](https://youtrack.jetbrains.com/issue/KT-7664) JS: "x is Any" is always false
- [`KT-7665`](https://youtrack.jetbrains.com/issue/KT-7665) JS: creating Any instance crashes on runtime
- [`KT-15131`](https://youtrack.jetbrains.com/issue/KT-15131) JS: don't mangle Any.equals

#### Various issues

- [`KT-14033`](https://youtrack.jetbrains.com/issue/KT-14033) JS: don't optimize (based on type information) by default expressions with any of "as, is, !is, as?, ?., !!"
- [`KT-13616`](https://youtrack.jetbrains.com/issue/KT-13616) JS: don't omit guard for catch with Throwable type
- [`KT-12976`](https://youtrack.jetbrains.com/issue/KT-12976) JS: human-friendly error message on wrong modules order
- [`KT-15212`](https://youtrack.jetbrains.com/issue/KT-15212) JS: link unqualified names in `js(...)` function to local functions in outer Kotlin function by name
- [`KT-14750`](https://youtrack.jetbrains.com/issue/KT-14750) JS: remove unnecessary functions from kotlin.js

#### Bugfixes

- [`KT-12566`](https://youtrack.jetbrains.com/issue/KT-12566) JS: inner local class should refer to captured variables via its outer class
- [`KT-12527`](https://youtrack.jetbrains.com/issue/KT-12527) Reified is-check works wrongly for chained calls
- [`KT-12586`](https://youtrack.jetbrains.com/issue/KT-12586) JS: compiler crashes when call inline function inside string templeate
- [`KT-13164`](https://youtrack.jetbrains.com/issue/KT-13164) Ecma TypeError on extending local class from inner one
- [`KT-14888`](https://youtrack.jetbrains.com/issue/KT-14888) JS: Compiler error: Cannot get FQ name of local class: lazy class <no name provided>
- [`KT-14748`](https://youtrack.jetbrains.com/issue/KT-14748) JS: eliminate unused functions
- [`KT-14999`](https://youtrack.jetbrains.com/issue/KT-14999) JS: Operator set + labeled lambdas
- [`KT-15007`](https://youtrack.jetbrains.com/issue/KT-15007) JS: Dies when checking if exception implements interface. TypeError: Cannot read property 'baseClasses' of undefined
- [`KT-15073`](https://youtrack.jetbrains.com/issue/KT-15073) KT to JS losing extension function's receiver
- [`KT-15169`](https://youtrack.jetbrains.com/issue/KT-15169) JS: compiler fails on annotated expression with TRE at Translation.doTranslateExpression()
- [`KT-13522`](https://youtrack.jetbrains.com/issue/KT-13522) JS: can't use captured reified type paramter in jsClass
- [`KT-13784`](https://youtrack.jetbrains.com/issue/KT-13784) JS: lambda was not inlined for function with reified parameter declared in another module
- [`KT-13792`](https://youtrack.jetbrains.com/issue/KT-13792) JS: inner class of local class does not capture enclosing class properly
- [`KT-15327`](https://youtrack.jetbrains.com/issue/KT-15327) JS: Enum `valueOf` should throw IllegalArgumentException

### Standard library

- [`KT-7930`](https://youtrack.jetbrains.com/issue/KT-7930) Make String.toInt(), toLong(), etc. nullable instead of throwing exception
- [`KT-8220`](https://youtrack.jetbrains.com/issue/KT-8220) Add #peek method to Sequence similar to Stream.peek
- [`KT-8286`](https://youtrack.jetbrains.com/issue/KT-8286) Int.toString and String.toInt with base as parameter
- [`KT-14034`](https://youtrack.jetbrains.com/issue/KT-14034) JS: unsafeCast function
- [`KT-15181`](https://youtrack.jetbrains.com/issue/KT-15181) Some source files are missing from published sources on Bintray

### IDE

- [`KT-15205`](https://youtrack.jetbrains.com/issue/KT-15205) Implement quick-fix for increasing module language level to enable unsupported language features

###### Issues fixed
- [`KT-14693`](https://youtrack.jetbrains.com/issue/KT-14693) Introduce Type Alias: Do not suggest type qualifiers
- [`KT-14696`](https://youtrack.jetbrains.com/issue/KT-14696) Introduce Type Alias: Fix NPE during dialog repaint
- [`KT-14685`](https://youtrack.jetbrains.com/issue/KT-14685) Introduce Type Alias: Replace type usages in constructor calls
- [`KT-14861`](https://youtrack.jetbrains.com/issue/KT-14861) Introduce Type Alias: Support callable references/class literals
- [`KT-15204`](https://youtrack.jetbrains.com/issue/KT-15204) Implement navigation from header to its implementation and vice versa
- [`KT-15269`](https://youtrack.jetbrains.com/issue/KT-15269) Quickfix for external (native) extension declarations
- [`KT-15293`](https://youtrack.jetbrains.com/issue/KT-15293) Add 1.1 EAP repository when creating a new Gradle project with 1.1 EAP

### Scripting

- [`KT-14538`](https://youtrack.jetbrains.com/issue/KT-14538) Kotlin gradle script files appear totally unresolved
- [`KT-14706`](https://youtrack.jetbrains.com/issue/KT-14706) Support package declaration in scripting
- [`KT-14707`](https://youtrack.jetbrains.com/issue/KT-14707) Support javax.script.Invocable on the JSR 223 ScriptEngine
- [`KT-14708`](https://youtrack.jetbrains.com/issue/KT-14708) kotlin-script-runtime is not published
- [`KT-14713`](https://youtrack.jetbrains.com/issue/KT-14713) Make it possible to use JSR 223 support without specifying compiler JAR absolute path
- [`KT-15064`](https://youtrack.jetbrains.com/issue/KT-15064) Gradle build with script .kts file: NPE at ScriptCodegen.genConstructor()

### Gradle support

- [`KT-15080`](https://youtrack.jetbrains.com/issue/KT-15080) Gradle build fails with Gradle 3.2 (master)
- [`KT-15120`](https://youtrack.jetbrains.com/issue/KT-15120) Gradle JS test compile task doesn't pick up production code
- [`KT-15127`](https://youtrack.jetbrains.com/issue/KT-15127) JS "compiler jar not found" with Gradle 3.2
- [`KT-15133`](https://youtrack.jetbrains.com/issue/KT-15133) Recent gradle-script-kotlin 3.3 distributions are unusable
- [`KT-15218`](https://youtrack.jetbrains.com/issue/KT-15218) Isolate Gradle Kotlin compiler process


## 1.1-M03 (EAP-3)

### New language features

- [`KT-2964`](https://youtrack.jetbrains.com/issue/KT-2964) Underscores in integer literals
    (see [KEEP](https://github.com/Kotlin/KEEP/blob/master/proposals/underscores-in-numeric-literals.md))
- [`KT-3824`](https://youtrack.jetbrains.com/issue/KT-3824) Underscore in lambda for unused parameters
    (see [KEEP](https://github.com/Kotlin/KEEP/blob/master/proposals/underscore-for-unused-parameters.md))
- [`KT-2783`](https://youtrack.jetbrains.com/issue/KT-2783) Allow to skip some components in a multi-declaration
    (see the same [KEEP](https://github.com/Kotlin/KEEP/blob/master/proposals/underscore-for-unused-parameters.md))
- [`KT-11551`](https://youtrack.jetbrains.com/issue/KT-11551) limited scope for dsl writers 
    (see [KEEP](https://github.com/Kotlin/KEEP/blob/master/proposals/scope-control-for-implicit-receivers.md))

### Compiler

#### Coroutines related issues
- Make fields for storing lambda parameters non-final (as they get assigned within `invoke` call)
- [`KT-14719`](https://youtrack.jetbrains.com/issue/KT-14719) Make initial continuation able to be resumed with exception 
- [`KT-14636`](https://youtrack.jetbrains.com/issue/KT-14636) Coroutine fields should not be volatile
- [`KT-14718`](https://youtrack.jetbrains.com/issue/KT-14718) Validate label value of coroutine in case of no suspension points

#### Typealises related issues
- [`KT-13514`](https://youtrack.jetbrains.com/issue/KT-13514) Type inference doesn't work with generic typealiases
- [`KT-13837`](https://youtrack.jetbrains.com/issue/KT-13837) Error "Type alias expands to T, which is not a class, an interface, or an object" 
    should also appear for local type aliases
- [`KT-14307`](https://youtrack.jetbrains.com/issue/KT-14307) Local recursive type alias should be an error
- [`KT-14400`](https://youtrack.jetbrains.com/issue/KT-14400) Compiler Error IllegalStateException: kotlin.NotImplementedError when anonymous 
    object inherits from typealias
- [`KT-14377`](https://youtrack.jetbrains.com/issue/KT-14377) Expected error: Modifier 'companion' is not applicable to 'typealias'
- [`KT-14498`](https://youtrack.jetbrains.com/issue/KT-14498) typealias allows to circumvent variance annotations
- [`KT-14641`](https://youtrack.jetbrains.com/issue/KT-14641) An exception while processing a nested type alias access after a dot

#### Various issues
- [`KT-550`](https://youtrack.jetbrains.com/issue/KT-550) Properties without initializer but with get must infer type from getter
- [`KT-8816`](https://youtrack.jetbrains.com/issue/KT-8816) Generate Kotlin parameter names in the same form as expected for Java 8 reflection
- [`KT-10569`](https://youtrack.jetbrains.com/issue/KT-10569) Cannot iterate over values of an enum class when it is used as a generic parameter
    (see [KEEP](https://github.com/Kotlin/KEEP/blob/master/proposals/generic-values-and-valueof-for-enums.md))
- [`KT-13557`](https://youtrack.jetbrains.com/issue/KT-13557) VerifyError with delegated local variable used in object expression
- [`KT-13890`](https://youtrack.jetbrains.com/issue/KT-13890) IllegalAccessError when invoking protected method with default arguments
- [`KT-14012`](https://youtrack.jetbrains.com/issue/KT-14012) Back-end (JVM) Internal error every first compilation after the source code change
- [`KT-14201`](https://youtrack.jetbrains.com/issue/KT-14201) UnsupportedOperationException: Don't know how to generate outer expression for anonymous 
    object with invoke and non-trivial closure
- [`KT-14318`](https://youtrack.jetbrains.com/issue/KT-14318) Repeated annotations resulting from type alias expansion should be reported
- [`KT-14347`](https://youtrack.jetbrains.com/issue/KT-14347) Report UNUSED_PARAMETER/VARIABLE on named unused lambda parameters/destructuring entries
- [`KT-14352`](https://youtrack.jetbrains.com/issue/KT-14352) @SinceKotlin is not taken into account for companion object member referenced via 
    type alias
- [`KT-14357`](https://youtrack.jetbrains.com/issue/KT-14357) Try-catch used in false condition generates CompilationException
- [`KT-14502`](https://youtrack.jetbrains.com/issue/KT-14502) Prohibit irrelevant modifiers and annotations on destructured parameters in lambda
- [`KT-14692`](https://youtrack.jetbrains.com/issue/KT-14692) Change resolution scope for componentX in lambda parameters
- [`KT-14824`](https://youtrack.jetbrains.com/issue/KT-14824) Back-end (JVM) Internal error: Couldn't inline method call 'get' into local final fun 
    StorageComponentContainer.<anonymous>(): kotlin.Unit
- [`KT-14798`](https://youtrack.jetbrains.com/issue/KT-14798) Gradle 3.2 AssertionError: Built-in class kotlin.ParameterName is not found

### JS

#### Feature support
- [`KT-6985`](https://youtrack.jetbrains.com/issue/KT-6985) Support Exceptions in JS
- [`KT-13574`](https://youtrack.jetbrains.com/issue/KT-13574) JS: support coroutines
- [`KT-14422`](https://youtrack.jetbrains.com/issue/KT-14422) JS: Support destructuring in lambda parameters
- [`KT-14507`](https://youtrack.jetbrains.com/issue/KT-14507) JS: allow to skip some components in a multi-declaration

#### Library updates
- [`KT-14637`](https://youtrack.jetbrains.com/issue/KT-14637) JS: Missing ArrayList.ensureCapacity
    
#### Other issues
- [`KT-2328`](https://youtrack.jetbrains.com/issue/KT-2328) js: kotlin exceptions must inherit Error
- [`KT-5537`](https://youtrack.jetbrains.com/issue/KT-5537) Drop Cloneable in JS
- [`KT-7014`](https://youtrack.jetbrains.com/issue/KT-7014) JS: generate code which more friendly to js tools (minifier, optimizer, linter etc)
- [`KT-8019`](https://youtrack.jetbrains.com/issue/KT-8019) JS: no stackTrace in exception subclasses
- [`KT-10911`](https://youtrack.jetbrains.com/issue/KT-10911) JS: Throwable properties aren't supported well
- [`KT-13912`](https://youtrack.jetbrains.com/issue/KT-13912) JS: Compiler NPE at JsSourceGenerationVisitor. Lambda with empty [if] block passed 
    to inline function
- [`KT-14535`](https://youtrack.jetbrains.com/issue/KT-14535) JS: Broken modification of captured variables defined by a destructuring declaration

### Standard Library
- [`KT-2084`](https://youtrack.jetbrains.com/issue/KT-2084) Common API should be available without referring to java.* packages

    Now those common types, which are supported on all platforms, are available in `kotlin.*` packages, and are imported by default. These include:
    - `ArrayList`, `HashSet`, `LinkedHashSet`, `HashMap`, `LinkedHashMap` in `kotlin.collections`
    - `Appendable` and `StringBuilder` in `kotlin.text`
    - `Comparator` in `kotlin.comparisons`
    On JVM these are just typealiases of the good old types from `java.util` and `java.lang`
- [`KT-13554`](https://youtrack.jetbrains.com/issue/KT-13554) Introduce bitwise operations `and`/`or`/`xor`/`inv` for Byte and Short
- [`KT-13582`](https://youtrack.jetbrains.com/issue/KT-13582)  New platform-agnostic extensions for arrays: `contentEquals` to compare arrays' 
    content for equality, `contentHashCode` to get hashcode of array's content, and `contentToString` to get the string representation of array elements.
- [`KT-14510`](https://youtrack.jetbrains.com/issue/KT-14510) Generic constraints of `Array.flatten` signature were relaxed a bit to make it just usable.
- [`KT-14789`](https://youtrack.jetbrains.com/issue/KT-14789) Provide `KotlinVersion` class, which allows to get the current version of the standard 
    library and compare it with some other `KotlinVersion` value.

### IDE
- [`KT-14409`](https://youtrack.jetbrains.com/issue/KT-14409) Incorrect "Variable can be declared immutable" inspection for local delegated variable
- [`KT-14431`](https://youtrack.jetbrains.com/issue/KT-14431) Create quick-fix on UNUSED_PARAMETER/VARIABLE when it can be replaced with one underscore
- [`KT-14794`](https://youtrack.jetbrains.com/issue/KT-14794) Add /Specify type/Remove explicit type intentions for property with getters if type 
    can be inferred
- [`KT-14752`](https://youtrack.jetbrains.com/issue/KT-14752) Exception while typing @JsName annotation in editor

## 1.1-M02 (EAP-2)

### Language features

+ **Destructuring for lambdas** ([proposal](https://github.com/Kotlin/KEEP/issues/32))

    Current limitations:

    - Nested destructuring is not supported
    - Destructuring in named functions/constructors is not supported
    - Is not supported for JS target
        
### Compiler

#### Smart cast enhancements
- [`KT-2127`](https://youtrack.jetbrains.com/issue/KT-2127) Smart cast receiver to not null after a not null safe call
- [`KT-6840`](https://youtrack.jetbrains.com/issue/KT-6840) Make data flow information the same for assigned and assignee
- [`KT-13426`](https://youtrack.jetbrains.com/issue/KT-13426) Fix exception when smartcast on both dispatch & extension receiver

#### Bound references related issues
- [`KT-12995`](https://youtrack.jetbrains.com/issue/KT-12995) Do not skip generation of the left-hand side for intrinsic bound references and class literals
- [`KT-13075`](https://youtrack.jetbrains.com/issue/KT-13075) Fix codegen for bound class reference
- [`KT-13110`](https://youtrack.jetbrains.com/issue/KT-13110) Fix type mismatch error on class literal with integer receiver expression
- [`KT-13172`](https://youtrack.jetbrains.com/issue/KT-13172) Report error on "this::class" in super constructor call
- [`KT-13271`](https://youtrack.jetbrains.com/issue/KT-13271) Fix incorrect unsupported error on synthetic extension call on LHS of ::
- [`KT-13367`](https://youtrack.jetbrains.com/issue/KT-13367) Inline bound callable reference if it's used only as a lambda

#### Coroutines related issues
- [`KT-13156`](https://youtrack.jetbrains.com/issue/KT-13156) Do not execute last Unit-typed coroutine statement twice
- [`KT-13246`](https://youtrack.jetbrains.com/issue/KT-13246) Fix VerifyError with coroutines on Dalvik
- [`KT-13289`](https://youtrack.jetbrains.com/issue/KT-13289) Fix VerifyError with coroutines: Bad type on operand stack
- [`KT-13409`](https://youtrack.jetbrains.com/issue/KT-13409) Fix generic variable spilling with coroutines
- [`KT-13531`](https://youtrack.jetbrains.com/issue/KT-13531) Fix ClassCastException when coercion to Unit interacts with generic await() and coroutines
- Prohibit `Continuation<*>` as a last parameter of suspend functions
- [`KT-13560`](https://youtrack.jetbrains.com/issue/KT-13560) Prohibit non-Unit suspend functions

#### Typealises related issues
- [`KT-13200`](https://youtrack.jetbrains.com/issue/KT-13200) Fix incorrect number of required type arguments reported on typealias
- [`KT-13181`](https://youtrack.jetbrains.com/issue/KT-13181) Fix unresolved reference for a type alias from a different module
- [`KT-13161`](https://youtrack.jetbrains.com/issue/KT-13161) Support java static methods calls with typealiases
- [`KT-13835`](https://youtrack.jetbrains.com/issue/KT-13835) Do not lose nullability information  while expanding type alias in projection position
- [`KT-13422`](https://youtrack.jetbrains.com/issue/KT-13422) Prohibit usage of type alias to exception class as an object in 'throw' expression 
- [`KT-13735`](https://youtrack.jetbrains.com/issue/KT-13735) Fix NoSuchMethodError for generic typealias access
- [`KT-13513`](https://youtrack.jetbrains.com/issue/KT-13513) Support SAM constructors for aliased java functional types
- [`KT-13822`](https://youtrack.jetbrains.com/issue/KT-13822) Fix exception for start-projection of a type alias
- [`KT-14071`](https://youtrack.jetbrains.com/issue/KT-14071) Prohibit using type alias as a qualifier for super
- [`KT-14282`](https://youtrack.jetbrains.com/issue/KT-14282) Report error on unused type alias with -language-version 1.0
- [`KT-14274`](https://youtrack.jetbrains.com/issue/KT-14274) Fix type alias resolution when it's used for supertype constructor call

#### JDK dependent built-in classes related issues
- [`KT-13209`](https://youtrack.jetbrains.com/issue/KT-13209) Change first parameter's type of Map.getOrDefault to K instead of Any
- [`KT-13069`](https://youtrack.jetbrains.com/issue/KT-13069) Do not emit invalid DefaultImpls delegation when interface extends MutableMap with JDK8

#### `data` classes and inheritance
- [`KT-11306`](https://youtrack.jetbrains.com/issue/KT-11306) Allow data classes to implement equals/hashCode/toString from base classes

#### Various JVM code generation issues
- [`KT-13182`](https://youtrack.jetbrains.com/issue/KT-13182) Fix compiler internal error at inline
- [`KT-13757`](https://youtrack.jetbrains.com/issue/KT-13757) Prohibit referencing nested classes by name with $
- [`KT-12985`](https://youtrack.jetbrains.com/issue/KT-12985) Do not create range instances for 'for' loop in CharSequence.indices
- [`KT-13931`](https://youtrack.jetbrains.com/issue/KT-13931) Optimize generated code for IntRange#contains

#### Various analysis & diagnostic issues
- [`KT-435`](https://youtrack.jetbrains.com/issue/KT-435) Use parameter names in error messages when calling a function-valued expression
- [`KT-10001`](https://youtrack.jetbrains.com/issue/KT-10001) Fix false unnecessary non-null assertion on a pair element
- [`KT-12811`](https://youtrack.jetbrains.com/issue/KT-12811) Treat function declaration as final if it is a member of a final class
- [`KT-13961`](https://youtrack.jetbrains.com/issue/KT-13961) Report REDECLARATION on private-in-file 'foo' vs public 'foo' in different file

### JS

#### Feature support
- [`KT-13544`](https://youtrack.jetbrains.com/issue/KT-13544) Support type aliases in JS
- [`KT-13345`](https://youtrack.jetbrains.com/issue/KT-13345) Support class literals in JS

#### Library updates
- [`KT-18`](https://youtrack.jetbrains.com/issue/KT-18) Move exceptions from `java.lang` to `kotlin` package
- [`KT-12386`](https://youtrack.jetbrains.com/issue/KT-12386) Rewrite JS collections in Kotlin, move them to `kotlin.collections` package
- [`KT-7809`](https://youtrack.jetbrains.com/issue/KT-7809) Make Collection implementations conform to their declared interfaces
- [`KT-7473`](https://youtrack.jetbrains.com/issue/KT-7473) Make AbstractCollection.equals check object type
- [`KT-13429`](https://youtrack.jetbrains.com/issue/KT-13429) Make 'remove' on fresh iterator throw exception  instead of removing last element
- [`KT-13459`](https://youtrack.jetbrains.com/issue/KT-13459) Make JS implementation of ArrayList::add(index, element) check the index is in valid range
- [`KT-8724`](https://youtrack.jetbrains.com/issue/KT-8724) Fix MutableIterator.remove() for HashMap
- [`KT-10786`](https://youtrack.jetbrains.com/issue/KT-10786) Make Map.keys return view of map keys instead of snapshot
- [`KT-14194`](https://youtrack.jetbrains.com/issue/KT-14194) Make HashMap.putAll implementation not to call getKey/getValue

### Standard Library

#### Backward compatibility
- [`KT-14297`](https://youtrack.jetbrains.com/issue/KT-14297) Add @SinceKotlin annotation to support compatibility with compilation against older standard library
- [`KT-14213`](https://youtrack.jetbrains.com/issue/KT-14213) Ensure printStackTrace can be called with -language-version 1.0

#### Enhancements
- [`KEEP-53`](https://github.com/Kotlin/KEEP/blob/master/proposals/stdlib/abstract-collections.md) Provide two distinct hierarchies of abstract collections: one for implementing read-only/immutable collections, and other for implementing mutable collections
- [`KEEP-13`](https://github.com/Kotlin/KEEP/blob/master/proposals/stdlib/map-copying.md) Provide extension functions to copy maps
- [`KT-18`](https://youtrack.jetbrains.com/issue/KT-18) Introduce type aliases for common exceptions from `java.lang` in `kotlin` package
- [`KT-12762`](https://youtrack.jetbrains.com/issue/KT-12762) Make `kotlin.ranges.until` return an empty range for "illegal" 'to' parameter
- [`KT-12894`](https://youtrack.jetbrains.com/issue/KT-12894) Allow nullable receiver for `use` extension

### Reflection

#### New features
- [`KT-8998`](https://youtrack.jetbrains.com/issue/KT-8998) Introduce comprehensive API to work with KType instances 
- [`KT-10447`](https://youtrack.jetbrains.com/issue/KT-10447) Provide a way to check if a KClass is a data class
- [`KT-11284`](https://youtrack.jetbrains.com/issue/KT-11284) Add KClass<T>.cast extension
- [`KT-13106`](https://youtrack.jetbrains.com/issue/KT-13106) Support annotation constructors in reflection

#### Optimizations
- [`KT-10651`](https://youtrack.jetbrains.com/issue/KT-10651) Optimize KClass.simpleName

### IDE

###### New features
- [`KT-12903`](https://youtrack.jetbrains.com/issue/KT-12903) Implement "Inline type alias" refactoring
- [`KT-12902`](https://youtrack.jetbrains.com/issue/KT-12902) Implement "Introduce type alias" refactoring
- [`KT-12904`](https://youtrack.jetbrains.com/issue/KT-12904) Implement "Create type alias from usage" quick fix
- [`KT-9016`](https://youtrack.jetbrains.com/issue/KT-9016) Make use of named higher order function parameters
- [`KT-12205`](https://youtrack.jetbrains.com/issue/KT-12205) Suggest import of Kotlin static members in editor with Java source
- [`KT-13941`](https://youtrack.jetbrains.com/issue/KT-13941) Implement intention for introducing destructured lambda parameters when it's possible
- [`KT-13943`](https://youtrack.jetbrains.com/issue/KT-13943) Implement inspection and quickfix for to detect a manual destructuring of for / lambda parameter

###### Issues fixed
- [`KT-13004`](https://youtrack.jetbrains.com/issue/KT-13004) Support bound method references in completion
- [`KT-13242`](https://youtrack.jetbrains.com/issue/KT-13242) Suggest 'typealias' keyword in completion
- [`KT-13244`](https://youtrack.jetbrains.com/issue/KT-13244) Override/Implement Members: Do not expand type aliases in the generated members
- [`KT-13611`](https://youtrack.jetbrains.com/issue/KT-13611) Go to Class: Fix presentation of type aliases
- [`KT-13759`](https://youtrack.jetbrains.com/issue/KT-13759) Rename: Process object-wrapping alias references
- [`KT-13955`](https://youtrack.jetbrains.com/issue/KT-13955) Find Usages: Add special type for usages inside of type aliases
- [`KT-13479`](https://youtrack.jetbrains.com/issue/KT-13479) Support navigation to type aliases from binaries
- [`KT-13766`](https://youtrack.jetbrains.com/issue/KT-13766) Fix optimize imports not to add wrong and unnecessary import because of type alias
- [`KT-12949`](https://youtrack.jetbrains.com/issue/KT-12949) Consider type aliases as candidates for import
- [`KT-13266`](https://youtrack.jetbrains.com/issue/KT-13266) Suggest non-imported type aliases in completion
- [`KT-13689`](https://youtrack.jetbrains.com/issue/KT-13689) Do not treat type alias constructor usage as original type usage for optimize imports

### Scripting

- A new library `kotlin-script-util` containing utilities for implementing kotlin script support  
- [`KT-7880`](https://youtrack.jetbrains.com/issue/KT-7880) Experimental support for JSR 223 Scripting API
- [`KT-13975`](https://youtrack.jetbrains.com/issue/KT-13975), [`KT-14264`](https://youtrack.jetbrains.com/issue/KT-14264) Convert error on retrieving gradle plugin settings to warning
- Implement support for custom template-based scripts in command-line compiler, maven and gradle plugins

## 1.1-M01 (EAP-1)

### Language features

+ **Coroutines (async/await, generators)** ([proposal](https://github.com/Kotlin/kotlin-coroutines))

    Current limitations:

    - for some cases type inference is not supported yet
    - limited IDE support
    - allowed only one `handleResult` function: [design](https://github.com/Kotlin/kotlin-coroutines/blob/master/kotlin-coroutines-informal.md#result-handlers)
    - handling `finally` blocks is not supported: [issue](https://github.com/Kotlin/kotlin-coroutines/issues/1)

+ **Bound callable references** ([proposal](https://github.com/Kotlin/KEEP/issues/5))

+ **Type aliases** ([proposal](https://github.com/Kotlin/KEEP/issues/4))

    Current limitations:
    - type alias constructors for inner classes are not supported yet
    - annotations on type alias are not supported yet
    - limited IDE support

+ **Local delegated properties** ([proposal](https://github.com/Kotlin/KEEP/issues/25))

+ **JDK dependent built-in classes** ([proposal](https://github.com/Kotlin/KEEP/issues/30))

+ **Sealed class inheritors in the same file** ([proposal](https://github.com/Kotlin/KEEP/issues/29))
+ **Allow base classes for data classes** ([proposal](https://github.com/Kotlin/KEEP/issues/31))


### Scripting

- Implement support for [Script Definition Template](https://github.com/Kotlin/KEEP/blob/da9f3ec5f78429e7560bfc284cb7f52e02282b1f/proposals/script-definition-template.md)
and related functionality, except the following parts:
  - automatic script templates discovery is not implemented
  - `@file:ScriptTemplate` annotation is not supported
  - the parameters `javaHome` and `scripts` from `KotlinScriptExternalDependencies` are not used yet
- Implement support for custom template-based scripts in IDEA: resolving, completion and navigation to symbols from script classpath and sources
- Implement GradleScriptTemplatesProvider extension that supplies a script template if gradle with
[kotlin script support](https://github.com/gradle/gradle-script-kotlin) is used in the project


### Compiler

###### Issues fixed
- [`KT-4779`](https://youtrack.jetbrains.com/issue/KT-4779) Generate default methods for implementations in interfaces
- [`KT-11780`](https://youtrack.jetbrains.com/issue/KT-11780) Fixed incorrect "No cast needed" warning
- [`KT-12156`](https://youtrack.jetbrains.com/issue/KT-12156) Fixed incorrect error on `inline` modifier inside final class
- [`KT-12358`](https://youtrack.jetbrains.com/issue/KT-12358) Report missing error "Abstract member not implemented" when a fake method of 'Any' is inherited from an interface
- [`KT-6206`](https://youtrack.jetbrains.com/issue/KT-6206) Generate equals/hashCode/toString in data class always unless it'll cause a JVM signature clash error
- [`KT-8990`](https://youtrack.jetbrains.com/issue/KT-8990) Fixed incorrect error "virtual member hidden" for a private method of an inner class
- [`KT-12429`](https://youtrack.jetbrains.com/issue/KT-12429) Fixed visibility checks for annotation usage on top-level declarations
- [`KT-5068`](https://youtrack.jetbrains.com/issue/KT-5068) Introduced a special diagnostic message for "type mismatch" errors such as `fun f(): Int = { 1 }`.

### Standard Library

- [`KT-8254`](https://youtrack.jetbrains.com/issue/KT-8254) Provide standard library supplement artifacts for using with JDK 7 and 8.
These artifacts include extensions for the types available in the latter JDKs, such as `AutoCloseable.use` ([`KT-5899`](https://youtrack.jetbrains.com/issue/KT-5899)) or `Stream.toList`.
- [`KT-12753`](https://youtrack.jetbrains.com/issue/KT-12753) Provide an access to named group matches of `Regex` match result (for JDK 8 only).
- Add `assertFails` overload with message to kotlin-test.


### IDE

###### New features

+ [`KT-12019`](https://youtrack.jetbrains.com/issue/KT-12019) Introduce "redundant `if`" inspection

###### Issues fixed

+ [`KT-12389`](https://youtrack.jetbrains.com/issue/KT-12389) Do not exit from REPL when toString() of user class throws an exception
+ [`KT-12129`](https://youtrack.jetbrains.com/issue/KT-12129) Fixed link on api reference page in KDoc

## 1.0.7

### IDE

- Project View: Fix presentation of Kotlin files and their members when @JvmName having the same name as the file itself
- [`KT-15611`](https://youtrack.jetbrains.com/issue/KT-15611) Extract Interface/Superclass: Disable const-properties
- Pull Up: Fix pull-up from object to superclass
- [`KT-15602`](https://youtrack.jetbrains.com/issue/KT-15602) Extract Interface/Superclass: Disable "Make abstract" for inline/external/lateinit members
- Extract Interface: Disable inline/external/lateinit members
- [`KT-12704`](https://youtrack.jetbrains.com/issue/KT-12704), [`KT-15583`](https://youtrack.jetbrains.com/issue/KT-15583) Override/Implement Members: Support all nullability annotations respected by the Kotlin compiler
- [`KT-15563`](https://youtrack.jetbrains.com/issue/KT-15563) Override Members: Allow overriding virtual synthetic members (e.g. equals(), hashCode(), toString(), etc.) in data classes
- [`KT-15355`](https://youtrack.jetbrains.com/issue/KT-15355) Extract Interface: Disable "Make abstract" and assume it to be true for abstract members of an interface
- [`KT-15353`](https://youtrack.jetbrains.com/issue/KT-15353) Extract Superclass/Interface: Allow extracting class with special name (and quotes)
- [`KT-15643`](https://youtrack.jetbrains.com/issue/KT-15643) Extract Interface/Pull Up: Disable "Make abstract" and assume it to be true for primary constructor parameter when moving to an interface
- [`KT-15607`](https://youtrack.jetbrains.com/issue/KT-15607) Extract Interface/Pull Up: Disable internal/protected members when moving to an interface
- [`KT-15640`](https://youtrack.jetbrains.com/issue/KT-15640) Extract Interface/Pull Up: Drop 'final' modifier when moving to an interface
- [`KT-15639`](https://youtrack.jetbrains.com/issue/KT-15639) Extract Superclass/Interface/Pull Up: Add spaces between 'abstract' modifier and annotations
- [`KT-15606`](https://youtrack.jetbrains.com/issue/KT-15606) Extract Interface/Pull Up: Warn about private members with usages in the original class
- [`KT-15635`](https://youtrack.jetbrains.com/issue/KT-15635) Extract Superclass/Interface: Fix bogus visibility warning inside a member when it's being moved as abstract
- [`KT-15598`](https://youtrack.jetbrains.com/issue/KT-15598) Extract Interface: Red-highlight members inherited from a super-interface when that interface reference itself is not extracted
- [`KT-15674`](https://youtrack.jetbrains.com/issue/KT-15674) Extract Superclass: Drop inapplicable modifiers when converting property-parameter to ordinary parameter
- [`KT-15444`](https://youtrack.jetbrains.com/issue/KT-15444) Spring Support: Consider declaration open if it's supplemented with a preconfigured annotation in corresponding compiler plugin

#### Intention actions, inspections and quickfixes

##### New features

- [`KT-15068`](https://youtrack.jetbrains.com/issue/KT-15068) Implement intention which rename file according to the top-level class name
- Implement quickfix which enables/disables coroutine support in module or project
- [`KT-15056`](https://youtrack.jetbrains.com/issue/KT-15056) Implement intention which converts object literal to class
- [`KT-8855`](https://youtrack.jetbrains.com/issue/KT-8855) Implement "Create label" quick fix
- [`KT-15627`](https://youtrack.jetbrains.com/issue/KT-15627) Support "Change parameter type" for parameters with type-mismatched default value

## 1.0.6

### IDE

- [`KT-13811`](https://youtrack.jetbrains.com/issue/KT-13811) Expose JVM target setting in IntelliJ IDEA plugin compiler configuration UI
- [`KT-12410`](https://youtrack.jetbrains.com/issue/KT-12410) Expose language version setting in IntelliJ IDEA plugin compiler configuration UI

#### Intention actions, inspections and quickfixes

- [`KT-14569`](https://youtrack.jetbrains.com/issue/KT-14569) Convert Property to Function Intention: Search occurrences using progress dialog
- [`KT-14501`](https://youtrack.jetbrains.com/issue/KT-14501) Create from Usage: Support array access expressions/binary expressions with type mismatch errors
- [`KT-14500`](https://youtrack.jetbrains.com/issue/KT-14500) Create from Usage: Suggest functional type based on the call with lambda argument and unresolved invoke()
- [`KT-14459`](https://youtrack.jetbrains.com/issue/KT-14459) Initialize with Constructor Parameter: Fix IDE freeze on properties in generic class
- [`KT-14044`](https://youtrack.jetbrains.com/issue/KT-14044) Fix exception on deleting unused declaration in IDEA 2016.3
- [`KT-14019`](https://youtrack.jetbrains.com/issue/KT-14019) Create from Usage: Support generation of abstract members for superclasses
- [`KT-14246`](https://youtrack.jetbrains.com/issue/KT-14246) Intentions: Convert function type parameter to receiver
- [`KT-14246`](https://youtrack.jetbrains.com/issue/KT-14246) Intentions: Convert function type receiver to parameter

##### New features

- [`KT-14729`](https://youtrack.jetbrains.com/issue/KT-14729) Implement "Add names to call arguments" intention
- [`KT-11760`](https://youtrack.jetbrains.com/issue/KT-11760) Create from Usage: Support adding type parameters to the referenced type

#### Refactorings

- [`KT-14583`](https://youtrack.jetbrains.com/issue/KT-14583) Change Signature: Use new signature when looking for redeclaration conflicts
- [`KT-14854`](https://youtrack.jetbrains.com/issue/KT-14854) Extract Interface: Fix NPE on dialog opening
- [`KT-14814`](https://youtrack.jetbrains.com/issue/KT-14814) Rename: Fix renaming of .kts file to .kt and vice versa
- [`KT-14361`](https://youtrack.jetbrains.com/issue/KT-14361) Rename: Do not report redeclaration conflict for private top-level declarations located in different files
- [`KT-14596`](https://youtrack.jetbrains.com/issue/KT-14596) Safe Delete: Fix exception on deleting Java class used in Kotlin import directive(s)
- [`KT-14325`](https://youtrack.jetbrains.com/issue/KT-14325) Rename: Fix exceptions on moving file with facade class to another package
- [`KT-14197`](https://youtrack.jetbrains.com/issue/KT-14197) Move: Fix callable reference processing when moving to another package
- [`KT-13781`](https://youtrack.jetbrains.com/issue/KT-13781) Extract Function: Do not wrap companion member references inside of the `with` call

##### New features
- [`KT-14792`](https://youtrack.jetbrains.com/issue/KT-14792) Rename: Suggest respective parameter name for the local variable passed to function

## 1.0.5

### IDE

- [`KT-9125`](https://youtrack.jetbrains.com/issue/KT-9125) Support Type Hierarchy on references inside of super type call entries
- [`KT-13542`](https://youtrack.jetbrains.com/issue/KT-13542) Rename: Do not search parameter text occurrences outside of its containing declaration
- [`KT-8672`](https://youtrack.jetbrains.com/issue/KT-8672) Rename: Optimize search of parameter references in calls with named arguments
- [`KT-9285`](https://youtrack.jetbrains.com/issue/KT-9285) Rename: Optimize search of private class members
- [`KT-13589`](https://youtrack.jetbrains.com/issue/KT-13589) Use TODO() consistently in implementation stubs
- [`KT-13630`](https://youtrack.jetbrains.com/issue/KT-13630) Do not show Change Signature dialog when applying "Remove parameter" quick-fix
- Re-highlight only single function after local modifications
- [`KT-13474`](https://youtrack.jetbrains.com/issue/KT-13474) Fix performance of typing super call lambda
- Show "Variables and values captured in a closure" highlighting only for usages
- [`KT-13838`](https://youtrack.jetbrains.com/issue/KT-13838) Add file name to the presentation of private top-level declaration (Go to symbol, etc.)
- [`KT-14096`](https://youtrack.jetbrains.com/issue/KT-14096) Rename: When renaming Kotlin file outside of source root do not rename its namesake in a source root
- [`KT-13928`](https://youtrack.jetbrains.com/issue/KT-13928) Move Inner Class to Upper Level: Fix replacement of outer class instances used in inner class constructor calls
- [`KT-12556`](https://youtrack.jetbrains.com/issue/KT-12556) Allow using whitespaces and other symbols in "Generate -> Test Function" dialog
- [`KT-14122`](https://youtrack.jetbrains.com/issue/KT-14122) Generate 'toString()': Permit for data classes
- [`KT-12398`](https://youtrack.jetbrains.com/issue/KT-12398) Call Hierarchy: Show Kotlin usages of Java methods
- [`KT-13976`](https://youtrack.jetbrains.com/issue/KT-13976) Search Everywhere: Render function parameter types
- [`KT-13977`](https://youtrack.jetbrains.com/issue/KT-13977) Search Everywhere: Render extension type in prefix position
- Implement Kotlin facet

#### Intention actions, inspections and quickfixes

- [`KT-9490`](https://youtrack.jetbrains.com/issue/KT-9490) Convert receiver to parameter: use template instead of the dialog
- [`KT-11483`](https://youtrack.jetbrains.com/issue/KT-11483) Move to Companion: Do not use qualified names as labels
- [`KT-13874`](https://youtrack.jetbrains.com/issue/KT-13874) Move to Companion: Fix AssertionError on running refactoring from Conflicts View
- [`KT-13883`](https://youtrack.jetbrains.com/issue/KT-13883) Move to Companion Object: Fix exception when applied to class
- [`KT-13876`](https://youtrack.jetbrains.com/issue/KT-13876) Move to Companion Object: Forbid for functions/properties referencing type parameters of the containing class
- [`KT-13877`](https://youtrack.jetbrains.com/issue/KT-13877) Move to Companion Object: Warn if companion object already contains function with the same signature
- [`KT-13933`](https://youtrack.jetbrains.com/issue/KT-13933) Convert Parameter to Receiver: Do not qualify companion members with labeled 'this'
- [`KT-13942`](https://youtrack.jetbrains.com/issue/KT-13942) Redundant 'toString()' in String Template: Disable for qualified expressions with 'super' receiver
- [`KT-13878`](https://youtrack.jetbrains.com/issue/KT-13878) Remove Redundant Receiver Parameter: Fix exception receiver removal
- [`KT-14143`](https://youtrack.jetbrains.com/issue/KT-14143) Create from Usages: Do not suggest on type-mismatched expressions which are not call arguments
- [`KT-13882`](https://youtrack.jetbrains.com/issue/KT-13882) Convert Receiver to Parameter: Fix AssertionError
- [`KT-14199`](https://youtrack.jetbrains.com/issue/KT-14199) Add Library: Fix exception due to resolution being run in the "dumb mode"
- Convert Receiver to Parameter: Fix this replacement

##### New features

- [`KT-11525`](https://youtrack.jetbrains.com/issue/KT-11525) Implement "Create type parameter" quickfix
- [`KT-9931`](https://youtrack.jetbrains.com/issue/KT-9931) Implement "Remove unused assignment" quickfix
- [`KT-14245`](https://youtrack.jetbrains.com/issue/KT-14245) Implement "Convert enum to sealed class" intention
- [`KT-14245`](https://youtrack.jetbrains.com/issue/KT-14245) Implement "Convert sealed class to enum" intention

#### Refactorings

- [`KT-13535`](https://youtrack.jetbrains.com/issue/KT-13535) Pull Up: Remove visibility modifiers on adding 'override'
- [`KT-13216`](https://youtrack.jetbrains.com/issue/KT-13216) Move: Report separate conflicts for each property accessor
- [`KT-13216`](https://youtrack.jetbrains.com/issue/KT-13216) Move: Forbid moving of enum entries
- [`KT-13553`](https://youtrack.jetbrains.com/issue/KT-13553) Move: Do not show directory selection dialog if target directory is already specified by drag-and-drop
- [`KT-8867`](https://youtrack.jetbrains.com/issue/KT-8867) Rename: Rename all overridden members if user chooses to refactor base declaration(s)
- Pull Up: Drop 'override' modifier if moved member doesn't override anything
- [`KT-13660`](https://youtrack.jetbrains.com/issue/KT-13660) Move: Do not drop object receivers when calling variable of extension functional type
- [`KT-13903`](https://youtrack.jetbrains.com/issue/KT-13903) Move: Remove companion object which becomes empty after the move
- [`KT-13916`](https://youtrack.jetbrains.com/issue/KT-13916) Move: Report visibility conflicts in import directives
- [`KT-13906`](https://youtrack.jetbrains.com/issue/KT-13906) Move Nested Class to Upper Level: Do not show directory selection dialog twice
- [`KT-13901`](https://youtrack.jetbrains.com/issue/KT-13901) Move: Do not ignore target directory selected in the dialog (DnD mode)
- [`KT-13904`](https://youtrack.jetbrains.com/issue/KT-13904) Move Nested Class to Upper Level: Preserve state of "Search in comments"/"Search for text occurrences" checkboxes
- [`KT-13909`](https://youtrack.jetbrains.com/issue/KT-13909) Move Files/Directories: Fix behavior of "Open moved files in editor" checkbox
- [`KT-14004`](https://youtrack.jetbrains.com/issue/KT-14004) Introduce Variable: Fix exception on trying to extract variable of functional type
- [`KT-13726`](https://youtrack.jetbrains.com/issue/KT-13726) Move: Fix bogus conflicts due to references resolving to wrong library version
- [`KT-14114`](https://youtrack.jetbrains.com/issue/KT-14114) Move: Fix exception on moving Kotlin file without declarations
- [`KT-14157`](https://youtrack.jetbrains.com/issue/KT-14157) Rename: Rename do-while loop variables in the loop condition
- [`KT-14128`](https://youtrack.jetbrains.com/issue/KT-14128), [`KT-13862`](https://youtrack.jetbrains.com/issue/KT-13862) Rename: Use qualified class name when looking for occurrences in non-code files
- [`KT-6199`](https://youtrack.jetbrains.com/issue/KT-6199) Rename: Replace non-code class occurrences with new qualified name
- [`KT-14182`](https://youtrack.jetbrains.com/issue/KT-14182) Move: Show error message on applying to enum entries
- Extract Function: Support implicit abnormal exits via Nothing-typed expressions
- [`KT-14285`](https://youtrack.jetbrains.com/issue/KT-14285) Rename: Forbid on backing field reference
- [`KT-14240`](https://youtrack.jetbrains.com/issue/KT-14240) Introduce Variable: Do not replace assignment left-hand sides
- [`KT-14234`](https://youtrack.jetbrains.com/issue/KT-14234) Rename: Do not suggest type-based names for functions with primitive return types

##### New features

- [`KT-13155`](https://youtrack.jetbrains.com/issue/KT-13155) Implement "Introduce Type Parameter" refactoring
- [`KT-11017`](https://youtrack.jetbrains.com/issue/KT-11017) Implement "Extract Superclass" refactoring
- [`KT-11017`](https://youtrack.jetbrains.com/issue/KT-11017) Implement "Extract Interface" refactoring
Pull Up: Support properties declared in the primary constructor
Pull Up: Support members declared in the companion object of the original class
Pull Up: Show member dependencies in the refactoring dialog
- [`KT-9485`](https://youtrack.jetbrains.com/issue/KT-9485) Push Down: Support moving members from Java to Kotlin class
- [`KT-13963`](https://youtrack.jetbrains.com/issue/KT-13963) Rename: Implement popup chooser for overriding members

#### Android Lint

###### Issues fixed

- [`KT-12022`](https://youtrack.jetbrains.com/issue/KT-12022) Report lint warnings even when file contains errors

## 1.0.4

### Compiler

#### Analysis & diagnostics

- [`KT-10968`](https://youtrack.jetbrains.com/issue/KT-10968), [`KT-11075`](https://youtrack.jetbrains.com/issue/KT-11075), [`KT-12286`](https://youtrack.jetbrains.com/issue/KT-12286) Type inference of callable references
- [`KT-11892`](https://youtrack.jetbrains.com/issue/KT-11892) Report error on qualified super call to a supertype extended by a different supertype
- [`KT-12875`](https://youtrack.jetbrains.com/issue/KT-12875) Report error on incorrect call of member extension invoke
- [`KT-12847`](https://youtrack.jetbrains.com/issue/KT-12847) Report error on accessing protected property setter from super class' companion
- [`KT-12322`](https://youtrack.jetbrains.com/issue/KT-12322) Overload resolution ambiguity with constructor reference when class has a companion object
- [`KT-11440`](https://youtrack.jetbrains.com/issue/KT-11440) Overload resolution ambiguity on specialized Map.put implementation from Java
- [`KT-11389`](https://youtrack.jetbrains.com/issue/KT-11389) Runtime exception when calling Java primitive overloadings
- [`KT-8200`](https://youtrack.jetbrains.com/issue/KT-8200) Exception when using non-generic interface with generic arguments
- [`KT-10237`](https://youtrack.jetbrains.com/issue/KT-10237) Exception on an unresolved symbol in a type parameter bound in the 'where' clause
- [`KT-11821`](https://youtrack.jetbrains.com/issue/KT-11821) Exception on incorrect number of generic arguments in a type parameter bound in the 'where' clause
- [`KT-12482`](https://youtrack.jetbrains.com/issue/KT-12482) Exception: Implementation doesn't have the most specific type, but none of the other overridden methods does either
- [`KT-12687`](https://youtrack.jetbrains.com/issue/KT-12687) Exception when 'data' modifier is applied to object
- [`KT-9620`](https://youtrack.jetbrains.com/issue/KT-9620) AssertionError in DescriptorResolver#checkBounds
- [`KT-3689`](https://youtrack.jetbrains.com/issue/KT-3689) IllegalAccess on a property with private setter of the subclass
- [`KT-6391`](https://youtrack.jetbrains.com/issue/KT-6391) Wrong warning for array casting (Array<Any?> to Array<Any>)
- [`KT-8596`](https://youtrack.jetbrains.com/issue/KT-8596) Exception when analyzing nested class constructor reference in an argument position
- [`KT-12982`](https://youtrack.jetbrains.com/issue/KT-12982) Incorrect type inference when accessing mutable protected property via reflection
- [`KT-13206`](https://youtrack.jetbrains.com/issue/KT-13206) Report "Cast never succeeds" if and only if ClassCastException can be predicted
- [`KT-12467`](https://youtrack.jetbrains.com/issue/KT-12467) IllegalStateException: Concrete fake override should have exactly one concrete super-declaration: []
- [`KT-13340`](https://youtrack.jetbrains.com/issue/KT-13340) Report "return is not allowed here" only on the return keyword, not the whole expression
- [`KT-2349`](https://youtrack.jetbrains.com/issue/KT-2349), [`KT-6054`](https://youtrack.jetbrains.com/issue/KT-6054) Report "uninitialized enum entry" if enum entry is referenced before its declaration
- [`KT-12809`](https://youtrack.jetbrains.com/issue/KT-12809) Report "uninitialized variable" if property is referenced before its declaration
- [`KT-260`](https://youtrack.jetbrains.com/issue/KT-260) Do not report "cast never succeeds" when casting nullable to nullable
- [`KT-11769`](https://youtrack.jetbrains.com/issue/KT-11769) Prohibit access from enum instance initialization code to members of enum's companion object
- [`KT-13371`](https://youtrack.jetbrains.com/issue/KT-13371) Fix CompilationException: Rewrite at slice LEAKING_THIS key: REFERENCE_EXPRESSION
- [`KT-13401`](https://youtrack.jetbrains.com/issue/KT-13401) Fix StackOverflowError when checking variance
- [`KT-13330`](https://youtrack.jetbrains.com/issue/KT-13330), [`KT-13349`](https://youtrack.jetbrains.com/issue/KT-13349) Fix AssertionError: Illegal resolved call to variable with invoke
- [`KT-13421`](https://youtrack.jetbrains.com/issue/KT-13421) Fix AssertionError: Only integer constants should be checked for overflow
- [`KT-13555`](https://youtrack.jetbrains.com/issue/KT-13555) Fix internal error "resolveToInstruction"
- [`KT-8989`](https://youtrack.jetbrains.com/issue/KT-8989) Change error messages: Replace "invisible_fake" with "invisible (private in a supertype)"
- [`KT-13612`](https://youtrack.jetbrains.com/issue/KT-13612) Val reassignment in try / catch
- [`KT-5469`](https://youtrack.jetbrains.com/issue/KT-5469) Incorrect "is never used" warning for value used in catch block
- [`KT-13510`](https://youtrack.jetbrains.com/issue/KT-13510) Missing "Nested class not allowed" error for anonymous object inside val initializer
- [`KT-13685`](https://youtrack.jetbrains.com/issue/KT-13685) Fix NPE when resolving callable references on incomplete code
- Change error messages: Fix quotes around keywords in diagnostic messages
- Change error messages: Remove quotes around visibilities

#### Parser

- [`KT-7118`](https://youtrack.jetbrains.com/issue/KT-7118) Improve error message after trailing dot in floating point literal
- [`KT-4948`](https://youtrack.jetbrains.com/issue/KT-4948) Recover by following keyword
- [`KT-7915`](https://youtrack.jetbrains.com/issue/KT-7915) Recover after val with no subsequent name
- [`KT-12987`](https://youtrack.jetbrains.com/issue/KT-12987) Recover after val with no name before declaration starting with soft keyword

#### JVM code generation

- [`KT-12909`](https://youtrack.jetbrains.com/issue/KT-12909) Do not generate redundant bridge for special built-in override
- [`KT-11915`](https://youtrack.jetbrains.com/issue/KT-11915) Exception in entrySet when Map implementation in Kotlin extends another one
- [`KT-12755`](https://youtrack.jetbrains.com/issue/KT-12755) Exception on property generation in multi-file classes
- [`KT-12983`](https://youtrack.jetbrains.com/issue/KT-12983) VerifyError: Bad type on operand stack in arraylength
- [`KT-12908`](https://youtrack.jetbrains.com/issue/KT-12908) Variable initialization in loop causes VerifyError: Bad local variable type
- [`KT-13040`](https://youtrack.jetbrains.com/issue/KT-13040) Invalid bytecode generated for extension lambda invocation with safe call
- [`KT-13023`](https://youtrack.jetbrains.com/issue/KT-13023) Char operations throw ClassCastException for boxed Chars
- [`KT-11634`](https://youtrack.jetbrains.com/issue/KT-11634) Exception for super call in delegation
- [`KT-12359`](https://youtrack.jetbrains.com/issue/KT-12359) Redundant stubs are generated on inheriting from java.util.Collection
- [`KT-11833`](https://youtrack.jetbrains.com/issue/KT-11833) Error generating constructors of class on anonymous object inheriting from nested class of super class
- [`KT-13133`](https://youtrack.jetbrains.com/issue/KT-13133) Incorrect InnerClasses attribute value for anonymous object copied from an inline function
- [`KT-13241`](https://youtrack.jetbrains.com/issue/KT-13241) Indices optimization leads to VerifyError with smart cast receiver
- [`KT-13374`](https://youtrack.jetbrains.com/issue/KT-13374) Fix compiler exception when inline function contains anonymous object implementing an interface by delegation

##### Generated code performance

- [`KT-11964`](https://youtrack.jetbrains.com/issue/KT-11964) No TABLESWITCH in when on enum bytecode if enum constant is imported
- [`KT-6916`](https://youtrack.jetbrains.com/issue/KT-6916) Optimize 'for' over 'downTo'
- [`KT-12733`](https://youtrack.jetbrains.com/issue/KT-12733) Optimize 'for' over 'rangeTo' as a non-qualified call

### Standard Library

- [`KT-13115`](https://youtrack.jetbrains.com/issue/KT-13115), [`KT-13297`](https://youtrack.jetbrains.com/issue/KT-13297) Improve documentation formatting, clarify documentation for `FileTreeWalk`, `Sequence` and `generateSequence`.
- [`KT-12894`](https://youtrack.jetbrains.com/issue/KT-12894) Do not fail in `Closeable.use` if the resource is `null`.

### Reflection

- [`KT-12915`](https://youtrack.jetbrains.com/issue/KT-12915) Runtime exception on callBy of JvmStatic function with default arguments
- [`KT-12967`](https://youtrack.jetbrains.com/issue/KT-12967) Runtime exception on reference to generic property
- [`KT-13370`](https://youtrack.jetbrains.com/issue/KT-13370) NullPointerException on companionObjectInstance of a built-in class
- [`KT-13462`](https://youtrack.jetbrains.com/issue/KT-13462) Make KClass for primitive type equal to the corresponding KClass for wrapper type

### IDE

- [`KT-12655`](https://youtrack.jetbrains.com/issue/KT-12655) New Kotlin file: extra error message for already existing file
- [`KT-12760`](https://youtrack.jetbrains.com/issue/KT-12760) Prohibit running non-Unit returning main function
- [`KT-12893`](https://youtrack.jetbrains.com/issue/KT-12893) Impossible to open Kotlin compiler settings
- [`KT-10433`](https://youtrack.jetbrains.com/issue/KT-10433) Copy-pasting reference to companion object member causes import dialog
- [`KT-12803`](https://youtrack.jetbrains.com/issue/KT-12803) Class is marked as unused when it is only used is in method reference
- [`KT-13084`](https://youtrack.jetbrains.com/issue/KT-13084) Run test method action executes all tests from same kotlin file
- [`KT-12718`](https://youtrack.jetbrains.com/issue/KT-12718) Deadlock due to index reentering
- [`KT-13114`](https://youtrack.jetbrains.com/issue/KT-13114) 'Unused declaration' option 'JUnit static methods' is always enabled
- [`KT-12997`](https://youtrack.jetbrains.com/issue/KT-12997) Override/Implement Members: Support "Copy JavaDoc" options for library classes
- [`KT-12887`](https://youtrack.jetbrains.com/issue/KT-12887) "Extend selection" should select call's invoked expression
- [`KT-13383`](https://youtrack.jetbrains.com/issue/KT-13383), [`KT-13379`](https://youtrack.jetbrains.com/issue/KT-13379) Override/Implement Members: Do not make return type non-nullable if base return type is explicitly nullable
- [`KT-13218`](https://youtrack.jetbrains.com/issue/KT-13218) Extract Function: Fix AssertionError on callable references
- [`KT-6520`](https://youtrack.jetbrains.com/issue/KT-6520) Introduce 'maino' and 'psvmo' templates for generating main in object
- [`KT-13455`](https://youtrack.jetbrains.com/issue/KT-13455) Override/Implement: Make return type non-nullable (platform collection case) when overriding Java method
- [`KT-10209`](https://youtrack.jetbrains.com/issue/KT-10209) Find Usages: Do not duplicate containing declaration in super member warning dialog
- [`KT-12977`](https://youtrack.jetbrains.com/issue/KT-12977) Hybrid dependency causes "outdated binary" warning to appear in non-js project
- [`KT-13057`](https://youtrack.jetbrains.com/issue/KT-13057) Go to inheritors on Enum should navigate to all enum classes
- Fix exception when choose Gradle configurer after project is synced
- Allow configuring Kotlin in Gradle module without Kotlin sources
- Show all Kotlin annotations when browsing hierarchy of "java.lang.Annotation"

#### Completion

- [`KT-12793`](https://youtrack.jetbrains.com/issue/KT-12793) Suggest abstract protected extension methods

#### Performance

- [`KT-12645`](https://youtrack.jetbrains.com/issue/KT-12645) Lazily calculate FQ name for local classes
- [`KT-13071`](https://youtrack.jetbrains.com/issue/KT-13071) Fix severe freezes because of long lint checks on large files

#### Highlighting

- [`KT-12937`](https://youtrack.jetbrains.com/issue/KT-12937) Java synthetic accessors highlighting does not differ from local variables

#### KDoc

- [`KT-12998`](https://youtrack.jetbrains.com/issue/KT-12998) Backslash is not rendered
- [`KT-12999`](https://youtrack.jetbrains.com/issue/KT-12999) Backtick inside inline code block is not rendered
- [`KT-13000`](https://youtrack.jetbrains.com/issue/KT-13000) Exclamation mark is not rendered
- [`KT-10398`](https://youtrack.jetbrains.com/issue/KT-10398) Fully qualified link is not resolved in editor
- [`KT-12932`](https://youtrack.jetbrains.com/issue/KT-12932) Link to library element is not clickable
- [`KT-10654`](https://youtrack.jetbrains.com/issue/KT-10654) Quick Doc can't follow KDoc link in referenced function description
- [`KT-9271`](https://youtrack.jetbrains.com/issue/KT-9271) Show Quick Doc for implicit lambda parameter 'it'

#### Formatter

- [`KT-12830`](https://youtrack.jetbrains.com/issue/KT-12830) Remove spaces before *?* in nullable types
- [`KT-13314`](https://youtrack.jetbrains.com/issue/KT-13314) Format spaces around !is and !in

#### Intention actions, inspections and quickfixes

##### New features

- [`KT-12152`](https://youtrack.jetbrains.com/issue/KT-12152) "Leaking this" inspection reports dangerous operations inside constructors including:

   * Accessing non-final property in constructor
   * Calling non-final function in constructor
   * Using 'this' as function argument in constructor of non-final class

- [`KT-13187`](https://youtrack.jetbrains.com/issue/KT-13187) "Make constructor parameter a val" should make the val private or public depending on its option
- [`KT-5771`](https://youtrack.jetbrains.com/issue/KT-5771) Mark setter parameter type as redundant and provide quickfix to remove it
- [`KT-9228`](https://youtrack.jetbrains.com/issue/KT-9228) Add quickfix to remove '@' from annotation used as argument of another annotation
- [`KT-12251`](https://youtrack.jetbrains.com/issue/KT-12251) Add quickfix to fix type mismatch for primitive literals
- [`KT-12838`](https://youtrack.jetbrains.com/issue/KT-12838) Add quickfix for "Illegal usage of inline parameter" that adds `noinline`
- [`KT-13134`](https://youtrack.jetbrains.com/issue/KT-13134) Add quickfix for wrong Long suffix (Use `L` instead of `l`)
- [`KT-10903`](https://youtrack.jetbrains.com/issue/KT-10903) Add intention to convert lambda to function reference
- [`KT-7492`](https://youtrack.jetbrains.com/issue/KT-7492) Support "Create abstract function/property" inside an abstract class
- [`KT-10668`](https://youtrack.jetbrains.com/issue/KT-10668) Support "Create member/extension" corresponding to the extension receiver of enclosing function
- [`KT-12553`](https://youtrack.jetbrains.com/issue/KT-12553) Show versions in inspection about different version of Kotlin plugin in Maven and IDE plugin
- [`KT-12489`](https://youtrack.jetbrains.com/issue/KT-12489) Implement intention to replace camel-case test function name with a space-separated one
- [`KT-12730`](https://youtrack.jetbrains.com/issue/KT-12730) Warn about using different versions of Kotlin Gradle plugin and bundled compiler
- [`KT-13173`](https://youtrack.jetbrains.com/issue/KT-13173) Handle more cases in "Add Const Modifier" Intention
- [`KT-12628`](https://youtrack.jetbrains.com/issue/KT-12628) Quickfix for `invoke` operator unsafe calls
- [`KT-11425`](https://youtrack.jetbrains.com/issue/KT-11425) Inspection and quickfix to replace usages of `equals()` and `compareTo()` with operators
- [`KT-13113`](https://youtrack.jetbrains.com/issue/KT-13113) Inspection to detect redundant string templates
- [`KT-13011`](https://youtrack.jetbrains.com/issue/KT-13011) Inspection and quickfix for unnecessary lateinit
- [`KT-10731`](https://youtrack.jetbrains.com/issue/KT-10731) Inspection and quickfix for unnecessary use of toString() inside string interpolation
- [`KT-12043`](https://youtrack.jetbrains.com/issue/KT-12043) Intention to add / remove braces for when entry/entries
- [`KT-13483`](https://youtrack.jetbrains.com/issue/KT-13483) Intention to replace `a..b-1` with `a until b` and vice versa
- [`KT-6975`](https://youtrack.jetbrains.com/issue/KT-6975) Quickfix for adding 'inline' to a function with reified generic

##### Bugfixes

- Show receiver type in the text of "Create extension" quick fix
- Show target class name in the text of "Create member" quick fix
- [`KT-12869`](https://youtrack.jetbrains.com/issue/KT-12869) Usages of overridden Java method through synthetic accessors are not found
- [`KT-12813`](https://youtrack.jetbrains.com/issue/KT-12813) "Find Usages" for property returns function calls
- [`KT-7722`](https://youtrack.jetbrains.com/issue/KT-7722) Approximate unresolvable types in "Create from Usage" quickfixes
- [`KT-11115`](https://youtrack.jetbrains.com/issue/KT-11115) Implement Members: Fix base member detection when abstract and non-abstract members with matching signatures are inherited from an interface
- [`KT-12876`](https://youtrack.jetbrains.com/issue/KT-12876) Bogus suggestion to move property to constructor
- [`KT-13055`](https://youtrack.jetbrains.com/issue/KT-13055) Exception in "Specify Type Explicitly" intention
- [`KT-12942`](https://youtrack.jetbrains.com/issue/KT-12942) "Replace 'when' with 'if'" intention changes semantics when 'if' statements are used
- [`KT-12646`](https://youtrack.jetbrains.com/issue/KT-12646) 'Convert to block body' should use partial body resolve
- [`KT-12919`](https://youtrack.jetbrains.com/issue/KT-12919) Use simple class name in "Change function return type" quickfix
- [`KT-13151`](https://youtrack.jetbrains.com/issue/KT-13151) Incorrect warning "Make variable immutable"
- [`KT-13170`](https://youtrack.jetbrains.com/issue/KT-13170) "Declaration has platform type" inspection: by default should not be reported for platform type arguments
- [`KT-13262`](https://youtrack.jetbrains.com/issue/KT-13262) "Wrap with safe let call" quickfix produces wrong result for qualified function
- [`KT-13364`](https://youtrack.jetbrains.com/issue/KT-13364) Do not suggest creating annotations/enum classes for unresolved type parameter bounds
- [`KT-12627`](https://youtrack.jetbrains.com/issue/KT-12627) Allow warnings suppression for secondary constructor
- [`KT-13365`](https://youtrack.jetbrains.com/issue/KT-13365) Disable "Create property" (non-abstract) in interfaces. Make "Create function" (non-abstract) generate function body in interfaces
- [`KT-8903`](https://youtrack.jetbrains.com/issue/KT-8903) Remove Unused Receiver: update function/property usages
- [`KT-11799`](https://youtrack.jetbrains.com/issue/KT-11799) Create from Usage: Make extension functions/properties 'private' by default
- [`KT-11795`](https://youtrack.jetbrains.com/issue/KT-11795) Create from Usage: Place extension properties after the usage and generate stub getter
- [`KT-12951`](https://youtrack.jetbrains.com/issue/KT-12951) Prohibit "Convert to expression body" when function body is 'if' without 'else' or 'when' is non-exhaustive
- [`KT-13430`](https://youtrack.jetbrains.com/issue/KT-13430) "Add non-null asserted (!!) call" quickfix can't process unary operators
- [`KT-13336`](https://youtrack.jetbrains.com/issue/KT-13336) "Convert concatenation to template" intention appends literal to variable omitting braces
- [`KT-13328`](https://youtrack.jetbrains.com/issue/KT-13328) Do not suggest "Replace infix with safe call" inside conditions or binary / unary expressions
- [`KT-13452`](https://youtrack.jetbrains.com/issue/KT-13452) "Replace if expression with assignment" doesn't work for cascade if-else if-else
- [`KT-13184`](https://youtrack.jetbrains.com/issue/KT-13184) "Different Kotlin Version" inspection: false positive caused by verbose plugin version name
- [`KT-13480`](https://youtrack.jetbrains.com/issue/KT-13480) "Can be replaced with comparison" inspection: false positive if extension method called 'equals' is used
- [`KT-13288`](https://youtrack.jetbrains.com/issue/KT-13288) "Unused property" inspection: false positive when extending abstract class and implementing interface
- [`KT-13432`](https://youtrack.jetbrains.com/issue/KT-13432) "Replace with safe call" quickfix does not work with `compareTo()` usage
- [`KT-13444`](https://youtrack.jetbrains.com/issue/KT-13444) "Invert if" intention changes semantics for nested if with return
- [`KT-13536`](https://youtrack.jetbrains.com/issue/KT-13536) Fix StackOverflowError from "Unused Symbol" inspection after importing enum's values()
- [`KT-12820`](https://youtrack.jetbrains.com/issue/KT-12820) Platform Type Inspection: !! quickfix shouldn't be available when any generic parameter has platform type
- [`KT-9825`](https://youtrack.jetbrains.com/issue/KT-9825) Incorrect "unused variable" warning when used in finally block
- [`KT-13715`](https://youtrack.jetbrains.com/issue/KT-13715) Prohibit applying "Change to star projection" to functional types

#### Refactorings

##### New features

- [`KT-12017`](https://youtrack.jetbrains.com/issue/KT-12017) Inline Property: Support "Do not show this dialog" and "Inline this occurrence" options

##### Bugfixes

- [`KT-11176`](https://youtrack.jetbrains.com/issue/KT-11176) Add a space before '{' in functions generated "Generate hashCode/equals/toString"
- [`KT-12294`](https://youtrack.jetbrains.com/issue/KT-12294) Introduce Property: Fix extraction of expressions referring to primary constructor parameters
- [`KT-12413`](https://youtrack.jetbrains.com/issue/KT-12413) Change Signature: Fix bogus warning about unresolved type parameters/invalid functional type replacement
- [`KT-12084`](https://youtrack.jetbrains.com/issue/KT-12084) Introduce Property: Do not skip outer classes if extractable expression is contained in object literal
- [`KT-13082`](https://youtrack.jetbrains.com/issue/KT-13082) Rename: Fix exception on property rename preview
- [`KT-13207`](https://youtrack.jetbrains.com/issue/KT-13207) Safe delete: Fix exception when removing any function in 2016.2
- [`KT-12945`](https://youtrack.jetbrains.com/issue/KT-12945) Rename: Fix function description in super method warning dialog
- [`KT-12922`](https://youtrack.jetbrains.com/issue/KT-12922) Introduce Variable: Do not suggest expressions without type
- [`KT-12943`](https://youtrack.jetbrains.com/issue/KT-12943) Rename: Show function signatures in "Rename Overloads" dialog
- [`KT-13157`](https://youtrack.jetbrains.com/issue/KT-13157) Extract Function: Automatically quote function name if necessary
- [`KT-13010`](https://youtrack.jetbrains.com/issue/KT-13010) Extract Function: Fix generation of destructuring declarations
- [`KT-13128`](https://youtrack.jetbrains.com/issue/KT-13128) Introduce Variable: Retain entered name after changing "Specify type explicitly" option
- [`KT-13054`](https://youtrack.jetbrains.com/issue/KT-13054) Introduce Variable: Skip leading/trailing comments inside selection
- [`KT-13385`](https://youtrack.jetbrains.com/issue/KT-13385) Move: Quote package name (if necessary) when moving declarations to new file
- [`KT-13395`](https://youtrack.jetbrains.com/issue/KT-13395) Introduce Property: Fix duplicate count in popup window
- [`KT-13277`](https://youtrack.jetbrains.com/issue/KT-13277) Change Signature: Fix usage processing to prevent interfering with Python support plugin
- [`KT-13254`](https://youtrack.jetbrains.com/issue/KT-13254) Rename: Conflict detection for type parameters
- [`KT-13282`](https://youtrack.jetbrains.com/issue/KT-13282), [`KT-13283`](https://youtrack.jetbrains.com/issue/KT-13283) Rename: Fix name quoting for automatic renamers
- [`KT-13239`](https://youtrack.jetbrains.com/issue/KT-13239) Rename: Warn about function name conflicts
- [`KT-13174`](https://youtrack.jetbrains.com/issue/KT-13174) Move: Warn about accessibility conflicts due to moving to unrelated module
- [`KT-13175`](https://youtrack.jetbrains.com/issue/KT-13175) Move: Warn about accessibility conflicts when moving entire file
- [`KT-13240`](https://youtrack.jetbrains.com/issue/KT-13240) Rename: Do not report shadowing conflict if redeclaration is detected
- [`KT-13253`](https://youtrack.jetbrains.com/issue/KT-13253) Rename: Report conflicts for constructor parameters
- [`KT-12971`](https://youtrack.jetbrains.com/issue/KT-12971) Push Down: Do not specifiy visibility on generated overriding members
- [`KT-13124`](https://youtrack.jetbrains.com/issue/KT-13124) Pull Up: Skip super members without explicit declarations
- [`KT-13032`](https://youtrack.jetbrains.com/issue/KT-13032) Rename: Support accessors with non-conventional names
- [`KT-13463`](https://youtrack.jetbrains.com/issue/KT-13463) Rename: Quote parameter name when necessary
- [`KT-13476`](https://youtrack.jetbrains.com/issue/KT-13476) Rename: Fix parameter rename when new name matches call selector
- [`KT-9381`](https://youtrack.jetbrains.com/issue/KT-9381) Rename: Do not search for component convention usages
- [`KT-13488`](https://youtrack.jetbrains.com/issue/KT-13488) Rename: Support rename of packages with non-standard quoted names

#### Debugger

##### New features

- [`KT-7549`](https://youtrack.jetbrains.com/issue/KT-7549) Provide an option to use the Kotlin syntax when evaluating watches and expressions in Java files

##### Bugfixes

- [`KT-13059`](https://youtrack.jetbrains.com/issue/KT-13059) Fix error stepping on *Step Over* action in the end of while block
- [`KT-13037`](https://youtrack.jetbrains.com/issue/KT-13037) Fix possible deadlock in debugger in 2016.1 and exception in 2016.2
- [`KT-12651`](https://youtrack.jetbrains.com/issue/KT-12651) Fix exception in evaluate expression when bad identifier is used for marking object
- [`KT-12896`](https://youtrack.jetbrains.com/issue/KT-12896) Fix "Step In" to inline functions for Android
- [`KT-13269`](https://youtrack.jetbrains.com/issue/KT-13269) Make quick evaluate work on receiver in qualified expressions
- [`KT-12641`](https://youtrack.jetbrains.com/issue/KT-12641) Unknown error on evaluate expression containing inline functions with complicated environment
- [`KT-13163`](https://youtrack.jetbrains.com/issue/KT-13163) Fix exception when evaluating expression: Access is allowed from event dispatch thread only.

### JS

#### New features

- [`KT-3008`](https://youtrack.jetbrains.com/issue/KT-3008) Option to generate require.js and AMD compatible modules
- [`KT-5987`](https://youtrack.jetbrains.com/issue/KT-5987) Add ability to refer to class
- [`KT-4115`](https://youtrack.jetbrains.com/issue/KT-4115) Provide method to get Kotlin type name

#### Bugfixes

- [`KT-8003`](https://youtrack.jetbrains.com/issue/KT-8003) Compiler exception on 'throw throw'
- [`KT-8318`](https://youtrack.jetbrains.com/issue/KT-8318) Wrong result for 'when' containing only 'else' block
- [`KT-12157`](https://youtrack.jetbrains.com/issue/KT-12157) Compiler exception on `when` condition containing `return`, `break` or `continue`
- [`KT-12275`](https://youtrack.jetbrains.com/issue/KT-12275) Fix code generation with inline function call in condition of `while`/`do..while`
- [`KT-13160`](https://youtrack.jetbrains.com/issue/KT-13160) Fix compiler exception when left-hand side of assignment is array access and right-hand side is inline function
- [`KT-12864`](https://youtrack.jetbrains.com/issue/KT-12864) Make enums comparable
- [`KT-12865`](https://youtrack.jetbrains.com/issue/KT-12865) Implementing Comparable breaks inheritance
- [`KT-12928`](https://youtrack.jetbrains.com/issue/KT-12928) Nested inline causes undefined reference access
- [`KT-12929`](https://youtrack.jetbrains.com/issue/KT-12929) Code with callable reference crashed at runtime (in some JS VMs)
- [`KT-13043`](https://youtrack.jetbrains.com/issue/KT-13043) Invalid invocation generated for secondary constructor that calls constructor from base class with default parameters
- [`KT-13025`](https://youtrack.jetbrains.com/issue/KT-13025) 'function?.invoke' does not work properly with extension functions
- [`KT-12807`](https://youtrack.jetbrains.com/issue/KT-12807) Lambda was lost in generated code
- [`KT-12808`](https://youtrack.jetbrains.com/issue/KT-12808) Compiler duplicates arguments and the body of lambda when lambda is in RHS of assignment operator
- [`KT-12873`](https://youtrack.jetbrains.com/issue/KT-12873) Fix ReferenceError when class delegates to complex expression
- [`KT-13658`](https://youtrack.jetbrains.com/issue/KT-13658) Wrong code when capturing object


### Tools

#### Gradle

- Gradle versions < 2.0 are not supported
- [`KT-13234`](https://youtrack.jetbrains.com/issue/KT-13234) Setting kotlinOptions.destination and kotlinOptions.classpath is deprecated
- [`KT-9392`](https://youtrack.jetbrains.com/issue/KT-9392) Kotlin classes are missing after converting Java class to Kotlin
- [`KT-12736`](https://youtrack.jetbrains.com/issue/KT-12736) Kotlin classes are deleted when generated Java source is changed
- [`KT-12658`](https://youtrack.jetbrains.com/issue/KT-12658) Build fails after android resources are edited
- [`KT-12750`](https://youtrack.jetbrains.com/issue/KT-12750) Non clean compilation fails with gradle 2.14
- [`KT-12912`](https://youtrack.jetbrains.com/issue/KT-12912) New class from subproject is unresolved with subsequent build with Gradle Daemon
- [`KT-12962`](https://youtrack.jetbrains.com/issue/KT-12962) Incremental compilation: Track changes in generated files
- [`KT-12923`](https://youtrack.jetbrains.com/issue/KT-12923) Incremental compilation: Compile error when code using internal class is modified
- [`KT-13528`](https://youtrack.jetbrains.com/issue/KT-13528) Incremental compilation: support multi-project incremental compilation
- [`KT-13732`](https://youtrack.jetbrains.com/issue/KT-13732) Android Build folder littered with `copyFlavourTypeXXX`

#### KAPT

##### New features

- [`KT-13499`](https://youtrack.jetbrains.com/issue/KT-13499) Implement Annotation Processing API (JSR 269) natively in Kotlin

##### Bugfixes

- [`KT-12776`](https://youtrack.jetbrains.com/issue/KT-12776) Android build fails with KAPT and generateStubs depending on library module names
- [`KT-13179`](https://youtrack.jetbrains.com/issue/KT-13179) Java is recompiled every time with Gradle 2.14 and KAPT
- [`KT-12303`](https://youtrack.jetbrains.com/issue/KT-12303), [`KT-12113`](https://youtrack.jetbrains.com/issue/KT-12113) Do not pass non-relevant annotations to processors

#### REPL

- [`KT-12389`](https://youtrack.jetbrains.com/issue/KT-12389) REPL just quits when toString() of user class throws an exception

#### CLI & Ant

- [`KT-13237`](https://youtrack.jetbrains.com/issue/KT-13237) Include kotlin-reflect.jar to classpath by default, add '-no-reflect' key to suppress this behavior

#### CLI

- [`KT-13491`](https://youtrack.jetbrains.com/issue/KT-13491) Support '-no-reflect' in 'kotlin' command

#### Maven

- [`KT-13211`](https://youtrack.jetbrains.com/issue/KT-13211) Provide better compilation failure info for TeamCity builds

#### Compiler daemon

- Fix exception "java.lang.NoClassDefFoundError: Could not initialize class kotlin.Unit"

## 1.0.3

### Compiler

#### Analysis & diagnostics

- Combination of `open` and `override` is no longer a warning
- [`KT-4829`](https://youtrack.jetbrains.com/issue/KT-4829) Equal conditions in `when` is now a warning
- [`KT-6611`](https://youtrack.jetbrains.com/issue/KT-6611) "This cast can never succeed" warning is no longer reported for `Foo<T> as Foo<Any>`
- [`KT-7174`](https://youtrack.jetbrains.com/issue/KT-7174) Declaring members with the same signature as non-overridable methods from Java classes (like Object.wait/notify) is now an error (when targeting JVM)
- [`KT-12302`](https://youtrack.jetbrains.com/issue/KT-12302) `abstract` modifier for a member of interface is no longer a warning
- [`KT-12452`](https://youtrack.jetbrains.com/issue/KT-12452) `open` modifier for a member of interface without implementation is now a warning
- [`KT-11111`](https://youtrack.jetbrains.com/issue/KT-11111) Overriding by inline function is now a warning, overriding by a function with reified type parameter is an error
- [`KT-12337`](https://youtrack.jetbrains.com/issue/KT-12337) Reference to a property with invisible setter now has KProperty type (as opposed to KMutableProperty)

###### Issues fixed

- [`KT-4285`](https://youtrack.jetbrains.com/issue/KT-4285) No warning for a non-tail call when the method inherits default arguments from superclass
- [`KT-4764`](https://youtrack.jetbrains.com/issue/KT-4764) Spurious "Variable must be initialized" in try/catch/finally
- [`KT-6665`](https://youtrack.jetbrains.com/issue/KT-6665) Unresolved reference leads to marking subsequent code unreachable
- [`KT-11750`](https://youtrack.jetbrains.com/issue/KT-11750) Exceptions when creating various entries with the name "name" in enums
- [`KT-11998`](https://youtrack.jetbrains.com/issue/KT-11998) Smart cast to not-null is not performed on a boolean property in `if` condition
- [`KT-10648`](https://youtrack.jetbrains.com/issue/KT-10648) Exhaustiveness check does not work when sealed class hierarchy contains intermediate sealed classes
- [`KT-10717`](https://youtrack.jetbrains.com/issue/KT-10717) Type inference for lambda with local return
- [`KT-11266`](https://youtrack.jetbrains.com/issue/KT-11266) Fixed "Empty intersection of types" internal compiler error for some cases
- [`KT-11857`](https://youtrack.jetbrains.com/issue/KT-11857) Fix visibility check for dynamic members within protected method (when targeting JS)
- [`KT-12589`](https://youtrack.jetbrains.com/issue/KT-12589) Improved "`infix` modifier is inapplicable" diagnostic message
- [`KT-11679`](https://youtrack.jetbrains.com/issue/KT-11679) Erroneous call with argument causes Throwable at ResolvedCallImpl.getArgumentMapping()
- [`KT-12623`](https://youtrack.jetbrains.com/issue/KT-12623) Fix ISE on malformed code

#### JVM code generation

- [`KT-5075`](https://youtrack.jetbrains.com/issue/KT-5075) Optimize array/collection indices usage in `for` loop
- [`KT-11116`](https://youtrack.jetbrains.com/issue/KT-11116) Optimize coercion to Unit, POP operations are backward-propagated

###### Issues fixed
- [`KT-11499`](https://youtrack.jetbrains.com/issue/KT-11499) Compiler crashes with "Incompatible stack heights"
- [`KT-11943`](https://youtrack.jetbrains.com/issue/KT-11943) CompilationException with extension property of KClass
- [`KT-12125`](https://youtrack.jetbrains.com/issue/KT-12125) Wrong increment/decrement on Byte/Char/Short.MAX_VALUE/MIN_VALUE
- [`KT-12192`](https://youtrack.jetbrains.com/issue/KT-12192) Exhaustiveness check isn't generated for when expression returning Unit
- [`KT-12200`](https://youtrack.jetbrains.com/issue/KT-12200) Erroneously optimized away assignment to a property initialized to zero
- [`KT-12582`](https://youtrack.jetbrains.com/issue/KT-12582) "VerifyError: Bad local variable type" caused by explicit loop variable type
- [`KT-12708`](https://youtrack.jetbrains.com/issue/KT-12708) Bridge method not generated when data class implements interface with copy() method
- [`KT-12106`](https://youtrack.jetbrains.com/issue/KT-12106) import static of reified companion object method throws IllegalAccessError

#### Performance

- Reduced number of IO operation when loading kotlin compiled classes

#### Сompiler options

- Allow to specify version of Kotlin language for source compatibility with older releases.
    - CLI: `-language-version` command line option
    - Maven: `languageVersion` configuration parameter, linked with `kotlin.compiler.languageVersion` property
    - Gradle: `kotlinOptions.languageVersion` property in task configuration
- Allow to specify which java runtime target version to generate bytecode for.
    - CLI: `-jvm-target` command line option
    - Maven: `jvmTarget` configuration parameter, linked with `kotlin.compiler.jvmTarget` property
    - Gradle: `kotlinOptions.jvmTarget` property in task configuration
- Allow to specify path to JDK to resolve classes from.
    - CLI: `-jdk-home` command line option
    - Maven: `jdkHome` configuration parameter, linked with `kotlin.compiler.jdkHome` property
    - Gradle: `kotlinOptions.jdkHome` property in task configuration

### Standard Library

- Improve documentation (including [`KT-11632`](https://youtrack.jetbrains.com/issue/KT-11632))
- List iteration used in collection operations is performed with an indexed loop when the list supports `RandomAccess` and the operation isn't inlined

### IDE

#### Completion

###### New features

- Smart completion after `by` and `in`
- Improved completion in bodies of overridden members (when no type is specified)
- Improved presentation of completion items for property accessors
- Fixed keyword completion after `try` in assignment expression
- [`KT-8527`](https://youtrack.jetbrains.com/issue/KT-8527) Include non-imported declarations on the first completion
- [`KT-12068`](https://youtrack.jetbrains.com/issue/KT-12068) Special completion item for "[]" get-operator access
- [`KT-12080`](https://youtrack.jetbrains.com/issue/KT-12080) Parameter names are now higher up in completion list

###### Issues fixed
- Fixed enum members being present in completion as static members
- Fixed QuickDoc not working for properties generated for java classes
- [`KT-9166`](https://youtrack.jetbrains.com/issue/KT-9166) Code completion does not work for synthetic java properties on typing "g"
- [`KT-11609`](https://youtrack.jetbrains.com/issue/KT-11609) No named arguments completion should be after dot
- [`KT-11633`](https://youtrack.jetbrains.com/issue/KT-11633) Wrong indentation after completing a statement in data class
- [`KT-11680`](https://youtrack.jetbrains.com/issue/KT-11680) Code completion of label for existing return with value inserts redundant whitespace
- [`KT-11784`](https://youtrack.jetbrains.com/issue/KT-11784) Completion for `if` statement should add parentheses automatically
- [`KT-11890`](https://youtrack.jetbrains.com/issue/KT-11890) Completion for callable references does not propose static Java members
- [`KT-11912`](https://youtrack.jetbrains.com/issue/KT-11912) String interpolation is not converted to ${} form when accessing this.property
- [`KT-11957`](https://youtrack.jetbrains.com/issue/KT-11957) No `catch` and `finally` keywords in completion
- [`KT-12103`](https://youtrack.jetbrains.com/issue/KT-12103) Smart completion for nested SAM-adapter produces short unresolved name
- [`KT-12138`](https://youtrack.jetbrains.com/issue/KT-12138) Do not show "::error" in smart completion when any function type accepting one argument is expected
- [`KT-12150`](https://youtrack.jetbrains.com/issue/KT-12150) Smart completion suggests to compare non-nullable with null
- [`KT-12124`](https://youtrack.jetbrains.com/issue/KT-12124) No code completion for a java property in a specific position
- [`KT-12299`](https://youtrack.jetbrains.com/issue/KT-12299) Completion: incorrect priority of property foo over method getFoo in Kotlin-only code
- [`KT-12328`](https://youtrack.jetbrains.com/issue/KT-12328) Qualified function name inserted when typing before `if`
- [`KT-12427`](https://youtrack.jetbrains.com/issue/KT-12427) Completion doesn't work for "@receiver:" annotation target
- [`KT-12447`](https://youtrack.jetbrains.com/issue/KT-12447) Don't use CompletionProgressIndicator in Kotlin plugin
- [`KT-12669`](https://youtrack.jetbrains.com/issue/KT-12669) Completion should show variant with `()` when there is default lambda
- [`KT-12369`](https://youtrack.jetbrains.com/issue/KT-12369) Pressing dot after class name should not cause insertion of constructor call

#### Spring support

###### New features

- [`KT-11692`](https://youtrack.jetbrains.com/issue/KT-11692) Support Spring model diagrams for Kotlin classes
- [`KT-12079`](https://youtrack.jetbrains.com/issue/KT-12079) Support "Autowired members defined in invalid Spring bean" inspection on Kotlin declarations
- [`KT-12092`](https://youtrack.jetbrains.com/issue/KT-12092) Implement bean references in @Qualifier annotations
- [`KT-12135`](https://youtrack.jetbrains.com/issue/KT-12135) Automatically configure components based on `basePackageClasses` attribute of @ComponentScan
- [`KT-12136`](https://youtrack.jetbrains.com/issue/KT-12136) Implement package references inside of string literals
- [`KT-12139`](https://youtrack.jetbrains.com/issue/KT-12139) Support Spring configurations linked via @Import annotation
- [`KT-12278`](https://youtrack.jetbrains.com/issue/KT-12278) Implement Spring @Autowired inspection
- [`KT-12465`](https://youtrack.jetbrains.com/issue/KT-12465) Implement Spring @ComponentScan inspection

###### Issues fixed

- [`KT-12091`](https://youtrack.jetbrains.com/issue/KT-12091) Fixed unstable behavior of Spring line markers
- [`KT-12096`](https://youtrack.jetbrains.com/issue/KT-12096) Fixed rename of custom-named beans specified with Kotlin annotation
- [`KT-12117`](https://youtrack.jetbrains.com/issue/KT-12117) Group Kotlin classes from the same file in the Choose Bean dialog
- [`KT-12120`](https://youtrack.jetbrains.com/issue/KT-12120) Show autowiring candidates line markers for @Autowired-annotated constructors and constructor parameters
- [`KT-12122`](https://youtrack.jetbrains.com/issue/KT-12122) Fixed line marker popup on functions with @Qualifier-annotated parameters
- [`KT-12143`](https://youtrack.jetbrains.com/issue/KT-12143) Fixed "Spring Facet Code Configuration (Kotlin)" inspection description
- [`KT-12147`](https://youtrack.jetbrains.com/issue/KT-12147) Fixed exception on analyzing object declaration with @Component annotation
- [`KT-12148`](https://youtrack.jetbrains.com/issue/KT-12148) Warn about object declarations annotated with Spring `@Configuration`/`@Component`/etc.
- [`KT-12363`](https://youtrack.jetbrains.com/issue/KT-12363) Fixed "Autowired members defined in invalid Spring bean (Kotlin)" inspection description
- [`KT-12366`](https://youtrack.jetbrains.com/issue/KT-12366) Fixed exception on analyzing class declaration upon annotation typing
- [`KT-12384`](https://youtrack.jetbrains.com/issue/KT-12384) Fixed bean references in factory method calls

#### Intention actions, inspections and quickfixes

###### New features

- New icon for "New -> Kotlin Activity" action
- "Change visibility on exposure" and "Make visible" fixes now support all possible visibilities
- [`KT-8477`](https://youtrack.jetbrains.com/issue/KT-8477) New inspection "Can be primary constructor property" with quick-fix
- [`KT-5010`](https://youtrack.jetbrains.com/issue/KT-5010) "Redundant semicolon" inspection with quickfix
- [`KT-9757`](https://youtrack.jetbrains.com/issue/KT-9757) Quickfix for "Unused lambda expression" warning
- [`KT-10844`](https://youtrack.jetbrains.com/issue/KT-10844) Quick fix to add crossinline modifier
- [`KT-11090`](https://youtrack.jetbrains.com/issue/KT-11090) "Add variance modifiers to type parameters" inspection
- [`KT-11255`](https://youtrack.jetbrains.com/issue/KT-11255) Move Element Left/Right actions
- [`KT-11450`](https://youtrack.jetbrains.com/issue/KT-11450) "Modality is redundant" inspection
- [`KT-11523`](https://youtrack.jetbrains.com/issue/KT-11523) "Add @JvmOverloads annotation" intention
- [`KT-11768`](https://youtrack.jetbrains.com/issue/KT-11768) "Introduce local variable" intention
- [`KT-11806`](https://youtrack.jetbrains.com/issue/KT-11806) Quick-fix to increase visibility for invisible member
- [`KT-11807`](https://youtrack.jetbrains.com/issue/KT-11807) Use function body template when generating overriding functions with default body
- [`KT-11864`](https://youtrack.jetbrains.com/issue/KT-11864) Suggest "Create function/secondary constructor" quick fix on argument type mismatch
- [`KT-11876`](https://youtrack.jetbrains.com/issue/KT-11876) Quickfix for "Extension function type is not allowed as supertype" error
- [`KT-11920`](https://youtrack.jetbrains.com/issue/KT-11920) "Increase visibility" and "Decrease visibility" quickfixes for exposed visibility errors
- [`KT-12089`](https://youtrack.jetbrains.com/issue/KT-12089) Quickfix "Make primary constructor parameter a property"
- [`KT-12121`](https://youtrack.jetbrains.com/issue/KT-12121) "Add `toString()` call" quickfix
- [`KT-11104`](https://youtrack.jetbrains.com/issue/KT-11104) New quickfixes for nullability problems: "Surround with null check" and "Wrap with safe let call"
- [`KT-12310`](https://youtrack.jetbrains.com/issue/KT-12310) New inspection "Member has platform type" with quickfix

###### Issues fixed

- Fixed "Convert property initializer getter" intention being available inside lambda initializer
- Improved message for "Can be declared as `val`" inspection
- [`KT-3797`](https://youtrack.jetbrains.com/issue/KT-3797) Quickfix to make a function abstract should not be offered for object members
- [`KT-11866`](https://youtrack.jetbrains.com/issue/KT-11866) Suggest "Create secondary constructor" when constructors exist but are not applicable
- [`KT-11482`](https://youtrack.jetbrains.com/issue/KT-11482) Fixed exception in "Move to companion object" intention
- [`KT-11483`](https://youtrack.jetbrains.com/issue/KT-11483) Pass implicit receiver as argument when moving member function to companion object
- [`KT-11512`](https://youtrack.jetbrains.com/issue/KT-11512) Allow choosing any source root in "Move file to directory" intention
- [`KT-10950`](https://youtrack.jetbrains.com/issue/KT-10950) Keep original file package name when moving top-level declarations to separate file (provided it's not ambiguous)
- [`KT-10174`](https://youtrack.jetbrains.com/issue/KT-10174) Optimize imports after applying "Move declaration to separate file" intention
- [`KT-11764`](https://youtrack.jetbrains.com/issue/KT-11764) Intention "Replace with a `forEach` function call should replace `continue` with `return@forEach`
- [`KT-11724`](https://youtrack.jetbrains.com/issue/KT-11724) False suggestion to replace with compound assignment
- [`KT-11805`](https://youtrack.jetbrains.com/issue/KT-11805) Invert if-condition intention breaks code in case of end of line comment
- [`KT-11811`](https://youtrack.jetbrains.com/issue/KT-11811) "Make protected" intention for a val declared in parameters of constructor
- [`KT-11710`](https://youtrack.jetbrains.com/issue/KT-11710) "Replace `if` with elvis operator": incorrect code generated for `if` expression
- [`KT-11849`](https://youtrack.jetbrains.com/issue/KT-11849) Replace explicit parameter with `it` changes the meaning of code because of the shadowing
- [`KT-11870`](https://youtrack.jetbrains.com/issue/KT-11870) "Replace with Elvis" refactoring doesn't change the variable type from T? to T
- [`KT-12069`](https://youtrack.jetbrains.com/issue/KT-12069) Specify language for all Kotlin code inspections
- [`KT-11366`](https://youtrack.jetbrains.com/issue/KT-11366) "object `Companion` is never used" warning in intellij
- [`KT-11275`](https://youtrack.jetbrains.com/issue/KT-11275) Inconsistent behaviour of "move lambda argument out of parentheses" intention action when using lambda calls with function arguments without parentheses
- [`KT-11594`](https://youtrack.jetbrains.com/issue/KT-11594) "Add non-null asserted (!!) call" applied to unsafe cast to nullable type causes AE at KtPsiFactory.createExpression()
- [`KT-11982`](https://youtrack.jetbrains.com/issue/KT-11982) False "Redundant final modifier" reported
- [`KT-12040`](https://youtrack.jetbrains.com/issue/KT-12040) "Replace when with if" produce invalid code for first entry which has comment
- [`KT-12204`](https://youtrack.jetbrains.com/issue/KT-12204) "Use classpath of module" option in existing Kotlin run configuration may be changed when a new run configuration is created
- [`KT-10635`](https://youtrack.jetbrains.com/issue/KT-10635) Don't mark private writeObject and readObject methods of Serializable classes as unused
- [`KT-11466`](https://youtrack.jetbrains.com/issue/KT-11466) "Make abstract" quick fix applies to outer class of object with accidentally abstract function
- [`KT-11120`](https://youtrack.jetbrains.com/issue/KT-11120) Constructor parameter/field reported as unused symbol even if it have `used` annotation
- [`KT-11974`](https://youtrack.jetbrains.com/issue/KT-11974) Invert if-condition intention loses comments
- [`KT-10812`](https://youtrack.jetbrains.com/issue/KT-10812) Globally unused constructors are not marked as such
- [`KT-11320`](https://youtrack.jetbrains.com/issue/KT-11320) Don't mark @BeforeClass (JUnit4) annotated functions as unused
- [`KT-12267`](https://youtrack.jetbrains.com/issue/KT-12267) "Change type" quick fix converts to Int for Long literal
- [`KT-11949`](https://youtrack.jetbrains.com/issue/KT-11949) Various problems fixed with "Constructor parameter is never used as a property" inspection
- [`KT-11716`](https://youtrack.jetbrains.com/issue/KT-11716) "Simply `for` using destructuring declaration" intention: incorrect behavior for data classes
- [`KT-12145`](https://youtrack.jetbrains.com/issue/KT-12145) "Simplify `for` using destructuring declaration" should work even when no variables declared inside loop
- [`KT-11933`](https://youtrack.jetbrains.com/issue/KT-11933) Entities used only by alias are marked as unused
- [`KT-12193`](https://youtrack.jetbrains.com/issue/KT-12193) Convert to block body isn't equivalent for when expressions returning Unit
- [`KT-10779`](https://youtrack.jetbrains.com/issue/KT-10779) Simplify `for` using destructing declaration: intention / inspection quick fix is available only when all variables are used
- [`KT-11281`](https://youtrack.jetbrains.com/issue/KT-11281) Fix exception on applying "Convert to class" intention to Java interface with Kotlin inheritor(s)
- [`KT-12285`](https://youtrack.jetbrains.com/issue/KT-12285) Fix exception on test class generation
- [`KT-12502`](https://youtrack.jetbrains.com/issue/KT-12502) Convert to expression body should be forbidden for non-exhaustive when returning Unit
- [`KT-12260`](https://youtrack.jetbrains.com/issue/KT-12260) ISE while replacing an operator with safe call
- [`KT-12649`](https://youtrack.jetbrains.com/issue/KT-12649) "Convert if to when" intention incorrectly deletes code
- [`KT-12671`](https://youtrack.jetbrains.com/issue/KT-12671) "Shot type" action: "Type is unknown" error on an invoked expression
- [`KT-12284`](https://youtrack.jetbrains.com/issue/KT-12284) Too wide applicability range for "Add braces to else" intention
- [`KT-11975`](https://youtrack.jetbrains.com/issue/KT-11975) "Invert if-condition" intention does not simplify `is` expression
- [`KT-12437`](https://youtrack.jetbrains.com/issue/KT-12437) "Replace explicit parameter" intention is suggested for parameter of inner lambda in presence of `it` from outer lambda
- [`KT-12290`](https://youtrack.jetbrains.com/issue/KT-12290) Navigate to the generated declaration when using "Implement abstract member" intention
- [`KT-12376`](https://youtrack.jetbrains.com/issue/KT-12376) Don't show "Package directive doesn't match file location" in injected code
- [`KT-12777`](https://youtrack.jetbrains.com/issue/KT-12777) Fix exception in "Create class" quickfix applied to unresolved references in type arguments

#### Language injection

- Apply injection for the literals in property initializer through property usages
- Enable injection from Java or Kotlin function declaration by annotating parameter with @Language annotation
- [`KT-2428`](https://youtrack.jetbrains.com/issue/KT-2428) Support basic use-cases of language injection for expressions marked with @Language annotation
- [`KT-11574`](https://youtrack.jetbrains.com/issue/KT-11574) Support predefined Java positions for language injection
- [`KT-11472`](https://youtrack.jetbrains.com/issue/KT-11472) Add comment or @Language annotation after "Inject language or reference" intention automatically

#### Refactorings

###### New features
- [`KT-6372`](https://youtrack.jetbrains.com/issue/KT-6372) Add name suggestions to Rename dialog
- [`KT-7851`](https://youtrack.jetbrains.com/issue/KT-7851) Respect naming conventions in automatic variable rename
- [`KT-8044`](https://youtrack.jetbrains.com/issue/KT-8044), [`KT-9432`](https://youtrack.jetbrains.com/issue/KT-9432) Support @JvmName annotation in rename refactoring
- [`KT-8512`](https://youtrack.jetbrains.com/issue/KT-8512) Support "Rename tests" options in Rename dialog
- [`KT-9168`](https://youtrack.jetbrains.com/issue/KT-9168) Support rename of synthetic properties
- [`KT-10578`](https://youtrack.jetbrains.com/issue/KT-10578) Support automatic test renaming for facade files
- [`KT-12657`](https://youtrack.jetbrains.com/issue/KT-12657) Rename implicit usages of annotation method `value`
- [`KT-12759`](https://youtrack.jetbrains.com/issue/KT-12759) Suggest renaming both property accessors with matching @JvmName when renaming one of them from Java

###### Issues fixed
- [`KT-4791`](https://youtrack.jetbrains.com/issue/KT-4791) Rename overridden property and all its accessors on attempt to rename overriding accessor in Java code
- [`KT-6363`](https://youtrack.jetbrains.com/issue/KT-6363) Do not rename ambiguous references in import directives
- [`KT-6663`](https://youtrack.jetbrains.com/issue/KT-6663) Fixed rename of ambiguous import reference to class/function when some referenced declarations are not changed
- [`KT-8510`](https://youtrack.jetbrains.com/issue/KT-8510) Preserve "Search in comments and strings" and "Search for text occurrences" settings in Rename dialog
- [`KT-8541`](https://youtrack.jetbrains.com/issue/KT-8541), [`KT-8786`](https://youtrack.jetbrains.com/issue/KT-8786) Do now show 'Rename overloads' options if target function has no overloads
- [`KT-8544`](https://youtrack.jetbrains.com/issue/KT-8544) Show more detailed description in Rename dialog
- [`KT-8562`](https://youtrack.jetbrains.com/issue/KT-8562) Show conflicts dialog on attempt of redeclaration
- [`KT-8611`](https://youtrack.jetbrains.com/issue/KT-8732) Qualify class references to resolve rename conflicts when possible
- [`KT-8732`](https://youtrack.jetbrains.com/issue/KT-8732) Implement Rename conflict analysis and fixes for properties/parameters
- [`KT-8860`](https://youtrack.jetbrains.com/issue/KT-8860) Allow renaming class by constructor delegation call referencing primary constructor
- [`KT-8892`](https://youtrack.jetbrains.com/issue/KT-8892) Suggest renaming base declarations on overriding members in object literals
- [`KT-9156`](https://youtrack.jetbrains.com/issue/KT-9156) Quote non-identifier names in Kotlin references
- [`KT-9157`](https://youtrack.jetbrains.com/issue/KT-9157) Fixed in-place rename of Kotlin expression referring Java declaration
- [`KT-9241`](https://youtrack.jetbrains.com/issue/KT-9241) Do not replace Java references to synthetic component functions when renaming constructor parameter
- [`KT-9435`](https://youtrack.jetbrains.com/issue/KT-9435) Process property accessor usages (Java) in comments and string literals
- [`KT-9444`](https://youtrack.jetbrains.com/issue/KT-9444) Rename dialog: Allow typing any identifier without backquotes
- [`KT-9446`](https://youtrack.jetbrains.com/issue/KT-9446) Copy default parameter values to overriding function which is renamed while its base function is not
- [`KT-9649`](https://youtrack.jetbrains.com/issue/KT-9649) Constraint search scope of parameter declared in a private member
- [`KT-10033`](https://youtrack.jetbrains.com/issue/KT-10033) Qualify references to members of enum companions in case of conflict with enum entries
- [`KT-10713`](https://youtrack.jetbrains.com/issue/KT-10713) Skip read-only declarations when renaming parameters
- [`KT-10687`](https://youtrack.jetbrains.com/issue/KT-10687) Qualify property references to avoid shadowing by parameters
- [`KT-11903`](https://youtrack.jetbrains.com/issue/KT-11903) Update references to facade class when renaming file via matching top-level class
- [`KT-12411`](https://youtrack.jetbrains.com/issue/KT-12411) Fix package name quotation in Move refactoring
- [`KT-12543`](https://youtrack.jetbrains.com/issue/KT-12543) Qualify property references with `this` to avoid renaming conflicts
- [`KT-12732`](https://youtrack.jetbrains.com/issue/KT-12732) Copy default parameter values to overriding function which is renamed by Java reference while its base function is unchanged
- [`KT-12747`](https://youtrack.jetbrains.com/issue/KT-12747) Fix exception on file copy

#### Java to Kotlin converter

###### New features

- [`KT-4727`](https://youtrack.jetbrains.com/issue/KT-4727) Convert Java code copied from browser or other sources

###### Issues fixed

- [`KT-11952`](https://youtrack.jetbrains.com/issue/KT-11952) Assertion failed in PropertyDetectionCache.get on conversion of access to Java constant of anonymous type
- [`KT-12046`](https://youtrack.jetbrains.com/issue/KT-12046) Recursive property setter
- [`KT-12039`](https://youtrack.jetbrains.com/issue/KT-12039) Static imports converted missing ".Companion"
- [`KT-12054`](https://youtrack.jetbrains.com/issue/KT-12054) Wrong conversion of `instanceof` checks with raw types
- [`KT-12045`](https://youtrack.jetbrains.com/issue/KT-12045) Convert `Object()` to `Any()`

#### Android Lint

###### Issues fixed

- [`KT-12015`](https://youtrack.jetbrains.com/issue/KT-12015) False positive for Bundle.getInt()
- [`KT-12023`](https://youtrack.jetbrains.com/issue/KT-12023) "minSdk" lint check doesn't work for `as`/`is`
- [`KT-12674`](https://youtrack.jetbrains.com/issue/KT-12674) "Calling new methods on older versions" errors for inlined constants
- [`KT-12681`](https://youtrack.jetbrains.com/issue/KT-12681) Running lint from main menu: diagnostics reported for java source files only
- [`KT-12173`](https://youtrack.jetbrains.com/issue/KT-12173) False positive for "Toast created but not shown" inside SAM adapter
- [`KT-12895`](https://youtrack.jetbrains.com/issue/KT-12895) NoSuchMethodError thrown when saving a Kotlin file

#### KDoc

###### New features
- Support for @receiver tag

###### Issues fixed
- Rendering of `_` and `*` standalone characters
- Rendering of code blocks
- [`KT-9933`](https://youtrack.jetbrains.com/issue/KT-9933) Indentation in code fragments is not preserved
- [`KT-10998`](https://youtrack.jetbrains.com/issue/KT-10998) Spaces around links are missing in return block
- [`KT-11791`](https://youtrack.jetbrains.com/issue/KT-11791) Markdown links rendering
- [`KT-12001`](https://youtrack.jetbrains.com/issue/KT-12001) Allow use of `@param` to document type parameter

#### Maven support

###### New features
- Inspections that check that kotlin IDEA plugin, kotlin Maven plugin and kotlin stdlib are of the same version
- [`KT-11643`](https://youtrack.jetbrains.com/issue/KT-11643) Inspections and intentions to fix erroneously configured Maven pom file
- [`KT-11701`](https://youtrack.jetbrains.com/issue/KT-11701) "Add Maven Dependency quick fix" in Kotlin source files
- [`KT-11743`](https://youtrack.jetbrains.com/issue/KT-11743) Intention to replace kotlin-test with kotlin-test-junit

###### Issues fixed
- [`KT-9492`](https://youtrack.jetbrains.com/issue/KT-9492) Configuring multiple Maven Modules
- [`KT-11642`](https://youtrack.jetbrains.com/issue/KT-11642) Kotlin Maven configurator tags order
- [`KT-11436`](https://youtrack.jetbrains.com/issue/KT-11436) "Choose Configurator" control opens dialogs with inconsistent modality (linux)
- [`KT-11731`](https://youtrack.jetbrains.com/issue/KT-11731) Default maven integration doesn't include documentation
- [`KT-12568`](https://youtrack.jetbrains.com/issue/KT-12568) Execution configuration: file path completion works only in some sub-elements of <sourceDirs>
- [`KT-12558`](https://youtrack.jetbrains.com/issue/KT-12558) Configure Kotlin in Project: "Undo" should revert changes in all poms
- [`KT-12512`](https://youtrack.jetbrains.com/issue/KT-12512) "Different IDE and Maven plugin version" inspection is being invoked for non-tracked pom.xml files

#### Debugger

###### New features
- [`KT-11438`](https://youtrack.jetbrains.com/issue/KT-11438) Support navigation from stacktrace to inline function call site

###### Issues fixed
- Do not step into inline lambda argument during step over inside inline function body
- Fix step over for inline argument with non-local return
- [`KT-12067`](https://youtrack.jetbrains.com/issue/KT-12067) Deadlock in Kotlin debugger is fixed
- [`KT-12232`](https://youtrack.jetbrains.com/issue/KT-12232) No code completion in Evaluate Expression and Throwable at CodeCompletionHandlerBase.invokeCompletion()
- [`KT-12137`](https://youtrack.jetbrains.com/issue/KT-12137) Evaluate expression: code completion/intention actions allows to use symbols from modules that are not referenced
- [`KT-12206`](https://youtrack.jetbrains.com/issue/KT-12206) NoSuchFieldError in Evaluate Expression on a property of a derived class
- [`KT-12678`](https://youtrack.jetbrains.com/issue/KT-12678) NoSuchFieldError in Evaluate Expression on accessing delegated property defined in other module
- [`KT-12773`](https://youtrack.jetbrains.com/issue/KT-12773) Fix debugging for Kotlin JS projects

#### Formatter

###### Issues fixed

- [`KT-12035`](https://youtrack.jetbrains.com/issue/KT-12035) Spaces around `as`
- [`KT-12018`](https://youtrack.jetbrains.com/issue/KT-12018) Spaces between function name and arguments in infix calls
- [`KT-11961`](https://youtrack.jetbrains.com/issue/KT-11961) Spaces before angle bracket in method definition
- [`KT-12175`](https://youtrack.jetbrains.com/issue/KT-12175) Don't enforce empty line between secondary constructors without body
- [`KT-12548`](https://youtrack.jetbrains.com/issue/KT-12548) Spaces around `is` keyword
- [`KT-12446`](https://youtrack.jetbrains.com/issue/KT-12446) Spaces before class type parameters
- [`KT-12634`](https://youtrack.jetbrains.com/issue/KT-12634) Spaces between method name and parenthesis in method call
- [`KT-10680`](https://youtrack.jetbrains.com/issue/KT-10680) Spaces around `in` keyword
- [`KT-12791`](https://youtrack.jetbrains.com/issue/KT-12791) Spaces between curly brace and expression inside string template
- [`KT-12781`](https://youtrack.jetbrains.com/issue/KT-12781) Spaces between annotation and expression
- [`KT-12689`](https://youtrack.jetbrains.com/issue/KT-12689) Spaces around semicolons
- [`KT-12714`](https://youtrack.jetbrains.com/issue/KT-12714) Spaces around parentheses in enum elements

#### Other

###### New features

- Added "Decompile" button to Kotlin bytecode toolwindow
- Added Kotlin "Tips of the day"
- Added "Kotlin 1.1 EAP" to "Configure Kotlin Plugin updates"
- [`KT-2919`](https://youtrack.jetbrains.com/issue/KT-2919) Constructor calls are no longer highlighted as classes
- [`KT-6540`](https://youtrack.jetbrains.com/issue/KT-6540) Infix function calls are now highlighted as regular function calls
- [`KT-9410`](https://youtrack.jetbrains.com/issue/KT-9410) Annotations in Kotlin are now highlighted with the same color as in Java by default
- [`KT-11465`](https://youtrack.jetbrains.com/issue/KT-11465) Type parameters in Kotlin are now highlighted with the same color as in Java by default
- [`KT-11657`](https://youtrack.jetbrains.com/issue/KT-11657) Allow viewing decompiled Java source code for Kotlin-compiled classes
- [`KT-11704`](https://youtrack.jetbrains.com/issue/KT-11704) Support file path references inside of Kotlin string literals
- [`KT-12076`](https://youtrack.jetbrains.com/issue/KT-12076) Kotlin Plugin update check: always display installed version number
- [`KT-11814`](https://youtrack.jetbrains.com/issue/KT-11814) New icon for kotlin annotation classes
- [`KT-12735`](https://youtrack.jetbrains.com/issue/KT-12735) Convert JavaDoc to KDoc when overriding Java class member in Kotlin

###### Issues fixed

- [`KT-5960`](https://youtrack.jetbrains.com/issue/KT-5960) Can't find usages for Java methods used from Kotlin by call convention
- [`KT-8362`](https://youtrack.jetbrains.com/issue/KT-8362) "New Kotlin file":  Keywords should be escaped in package name
- [`KT-8682`](https://youtrack.jetbrains.com/issue/KT-8682) Respect "Copy JavaDoc" option in the "Override/Implement Members..." dialog
- [`KT-8817`](https://youtrack.jetbrains.com/issue/KT-8817) Fixed rename of Java getters/setters through synthetic property references in Kotlin
- [`KT-9399`](https://youtrack.jetbrains.com/issue/KT-9399) Find Usages omits Kotlin annotation parameter usage in Java source
- [`KT-9797`](https://youtrack.jetbrains.com/issue/KT-9797) "Kotlin Bytecode" toolwindow breaks after closing
- [`KT-11145`](https://youtrack.jetbrains.com/issue/KT-11145) Use progress indicator when searching usages in Introduce Parameter
- [`KT-11155`](https://youtrack.jetbrains.com/issue/KT-11155) Allow running multiple Kotlin classes as well as running mixtures of Kotlin and Java classes
- [`KT-11495`](https://youtrack.jetbrains.com/issue/KT-11495) Show recursion line markers for extension function calls with different receiver
- [`KT-11659`](https://youtrack.jetbrains.com/issue/KT-11659) Generate abstract overrides for Any members inside of Kotlin interfaces
- [`KT-12070`](https://youtrack.jetbrains.com/issue/KT-12070) Add empty line in error message of Maven and Gradle configuration
- [`KT-11908`](https://youtrack.jetbrains.com/issue/KT-11908) Allow properties with custom setters to be used in generated equals/hashCode/toString
- [`KT-11617`](https://youtrack.jetbrains.com/issue/KT-11617) Fixed title of Introduce Parameter declaration chooser
- [`KT-11817`](https://youtrack.jetbrains.com/issue/KT-11817) Fixed rename of Kotlin enum constants through Java references
- [`KT-11816`](https://youtrack.jetbrains.com/issue/KT-11816) Fixed usages search for Safe Delete on simple enum entries
- [`KT-11282`](https://youtrack.jetbrains.com/issue/KT-11282) Delete interface reference from super-type list when applying Safe Delete to Java interface
- [`KT-11967`](https://youtrack.jetbrains.com/issue/KT-11967) Fix Find Usages/Rename for parameter references in XML files
- [`KT-10770`](https://youtrack.jetbrains.com/issue/KT-10770) "Optimize imports" will not keep import if a type is only referenced by kdoc
- [`KT-11955`](https://youtrack.jetbrains.com/issue/KT-11955) Copy/Paste inserts fully qualified name when copying function with overloads
- [`KT-12436`](https://youtrack.jetbrains.com/issue/KT-12436) "Replace explicit parameter with it": java.lang.Exception at BaseRefactoringProcessor.run()
- [`KT-12440`](https://youtrack.jetbrains.com/issue/KT-12440) Removing unused parameter results in Exception "Refactorings should not be started inside write action"
- [`KT-12006`](https://youtrack.jetbrains.com/issue/KT-12006) getLanguageLevel is slow for Kotlin light classes
- [`KT-12026`](https://youtrack.jetbrains.com/issue/KT-12026) "Constant expression required" in Java for const Kotlin values
- [`KT-12259`](https://youtrack.jetbrains.com/issue/KT-12259) ClassCastException in light classes while trying to create generic property
- [`KT-12289`](https://youtrack.jetbrains.com/issue/KT-12289) Remove unnecessary `?` from `serr` live template
- [`KT-12110`](https://youtrack.jetbrains.com/issue/KT-12110) Map help button of the Compiler - Kotlin page
- [`KT-12075`](https://youtrack.jetbrains.com/issue/KT-12075) Kotlin Plugin update check: make dumbaware
- [`KT-10255`](https://youtrack.jetbrains.com/issue/KT-10255) call BuildManager.clearState(project) in apply() method of Kotlin Compiler Settings configurable
- [`KT-11841`](https://youtrack.jetbrains.com/issue/KT-11841) New Project / Module wizard, Gradle: pure Kotlin module is created without `repositories` call in build.gradle
- [`KT-11095`](https://youtrack.jetbrains.com/issue/KT-11095) Java cannot infer generic return type of Kotlin function (with java 8 language level)
- [`KT-12090`](https://youtrack.jetbrains.com/issue/KT-12090) Intellij/Kotlin plugin does not handle generic return type of static method defined in Kotlin, called from Java
- [`KT-12206`](https://youtrack.jetbrains.com/issue/KT-12206) Fix NoSuchFieldError on accessing base property without backing field in evaluate expression
- [`KT-12516`](https://youtrack.jetbrains.com/issue/KT-12516) File Structure: Kotlin annotation classes have Java annotation icons
- [`KT-11328`](https://youtrack.jetbrains.com/issue/KT-11328) "New Kotlin class": generates packages when fully qualified name is specified
- [`KT-11778`](https://youtrack.jetbrains.com/issue/KT-11778) Exception in Lombok plugin: Rewrite at slice FUNCTION
- [`KT-11708`](https://youtrack.jetbrains.com/issue/KT-11708) "Go to declaration" doesn't work on a call to function with SAM conversion on a derived type
- [`KT-12381`](https://youtrack.jetbrains.com/issue/KT-12381) Prefer not-nullable return type when overriding Java method without nullability annotation
- [`KT-12647`](https://youtrack.jetbrains.com/issue/KT-12647) Performance improvement for test-related line markers
- [`KT-12526`](https://youtrack.jetbrains.com/issue/KT-12526) Kotlin intentions increase PSI modification counts from isAvailable, even in daemon threads

### Reflection

###### Issues fixed
- [`KT-11531`](https://youtrack.jetbrains.com/issue/KT-11531) Optimize "KCallable.name"
- [`KT-10771`](https://youtrack.jetbrains.com/issue/KT-10771) Reflection on Function objects does not support lambdas with generic return type
- [`KT-11824`](https://youtrack.jetbrains.com/issue/KT-11824) Reflection inconsistency between member property and accessor

### JS

- Improve performance of maps and sets

###### Issues fixed
- [`KT-6942`](https://youtrack.jetbrains.com/issue/KT-6942) Generate structural equality check (i.e. `Any.equals`) instead of referential check (===) value equality patterns in `when`
- [`KT-7228`](https://youtrack.jetbrains.com/issue/KT-7228) Wrong AbstractList signature
- [`KT-8299`](https://youtrack.jetbrains.com/issue/KT-8299) Wrong access to private member in autogenerated code in data class
- [`KT-11346`](https://youtrack.jetbrains.com/issue/KT-11346) Reified functions like `filterIsInstance` are now available in JS Standard Library
- [`KT-12305`](https://youtrack.jetbrains.com/issue/KT-12305) Incorrect translation of `vararg` in `@native` functions
- [`KT-12254`](https://youtrack.jetbrains.com/issue/KT-12254) JsEmptyExpression in initializer when compiling code like `val x = throw Exception()`
- [`KT-11960`](https://youtrack.jetbrains.com/issue/KT-11960) Wrong code generated when a method of a local class calls constructor of the class
- [`KT-10931`](https://youtrack.jetbrains.com/issue/KT-10931) Incorrect inlining of library method with optional parameters
- [`KT-12417`](https://youtrack.jetbrains.com/issue/KT-12417) Wrong check cast generated for KMutableProperty

### Tools

###### New features

- [`KT-11839`](https://youtrack.jetbrains.com/issue/KT-11839) Maven goal to execute kotlin script

###### Issues fixed

- KAPT: fix error when using enum constructors with parameters
- Various problems with gradle 2.2 fixed: [`KT-12478`](https://youtrack.jetbrains.com/issue/KT-12478), [`KT-12406`](https://youtrack.jetbrains.com/issue/KT-12406), [`KT-12478`](https://youtrack.jetbrains.com/issue/KT-12478)
- [`KT-12595`](https://youtrack.jetbrains.com/issue/KT-12595) JPS: Fixed com.intellij.util.io.MappingFailedException: Cannot map buffer
- [`KT-11166`](https://youtrack.jetbrains.com/issue/KT-11166) Gradle: Unable to access internal classes from test code within the same module
- [`KT-12352`](https://youtrack.jetbrains.com/issue/KT-12352) KAPT: Fix "Classpath entry points to a non-existent location" warnings
- [`KT-12074`](https://youtrack.jetbrains.com/issue/KT-12074) Building Kotlin maven projects using a parent pom will silently fail
- [`KT-11770`](https://youtrack.jetbrains.com/issue/KT-11770) Warning "RuntimeException: Could not find installation home path" when using Gradle Incremental Compilation
- [`KT-10969`](https://youtrack.jetbrains.com/issue/KT-10969) Android extensions: NullPointerException when finding view in Fragment
- [`KT-11885`](https://youtrack.jetbrains.com/issue/KT-11885) Gradle/Android: Unresolved reference "kotlinx" when classpath dependency is defined in root build.gradle
- [`KT-12786`](https://youtrack.jetbrains.com/issue/KT-12786) Deprecation warning with Gradle 2.14

## 1.0.2-1

- [KT-12159](https://youtrack.jetbrains.com/issue/KT-12159), [KT-12406](https://youtrack.jetbrains.com/issue/KT-12406), [KT-12431](https://youtrack.jetbrains.com/issue/KT-12431), [KT-12478](https://youtrack.jetbrains.com/issue/KT-12478) Support Android Studio 2.2
- [KT-11770](https://youtrack.jetbrains.com/issue/KT-11770) Fix warning "RuntimeException: Could not find installation home path" when using incremental compilation in Gradle
- [KT-12436](https://youtrack.jetbrains.com/issue/KT-12436), [KT-12440](https://youtrack.jetbrains.com/issue/KT-12440) Fix multiple exceptions during refactorings in IDEA 2016.2 EAP
- [KT-12015](https://youtrack.jetbrains.com/issue/KT-12015), [KT-12047](https://youtrack.jetbrains.com/issue/KT-12047), [KT-12387](https://youtrack.jetbrains.com/issue/KT-12387) Fix multiple issues in Kotlin Lint checks

## 1.0.2

### Compiler

#### Analysis & diagnostics

- [KT-7437](https://youtrack.jetbrains.com/issue/KT-7437), [KT-7971](https://youtrack.jetbrains.com/issue/KT-7971), [KT-7051](https://youtrack.jetbrains.com/issue/KT-7051), [KT-6125](https://youtrack.jetbrains.com/issue/KT-6125), [KT-6186](https://youtrack.jetbrains.com/issue/KT-6186), [KT-11649](https://youtrack.jetbrains.com/issue/KT-11649) Implement missing checks for protected visibility
- [KT-11666](https://youtrack.jetbrains.com/issue/KT-11666) Report "Implicit nothing return type" on non-override member functions
- [KT-4328](https://youtrack.jetbrains.com/issue/KT-4328), [KT-11497](https://youtrack.jetbrains.com/issue/KT-11497), [KT-10493](https://youtrack.jetbrains.com/issue/KT-10493), [KT-10820](https://youtrack.jetbrains.com/issue/KT-10820), [KT-11368](https://youtrack.jetbrains.com/issue/KT-11368) Report error if some classes were not found due to missing or conflicting dependencies
- [KT-11280](https://youtrack.jetbrains.com/issue/KT-11280) Do not perform smart casts for values with custom `equals` compared with `==`
- [KT-3856](https://youtrack.jetbrains.com/issue/KT-3856) Fix wrong "inner class inaccessible" diagnostic for extension to outer class
- [KT-3896](https://youtrack.jetbrains.com/issue/KT-3896), [KT-3883](https://youtrack.jetbrains.com/issue/KT-3883), [KT-4986](https://youtrack.jetbrains.com/issue/KT-4986) `do...while (true)` is now considered an infinite loop
- [KT-10445](https://youtrack.jetbrains.com/issue/KT-10445) Prohibit initialization of captured `val` in lambda or in local function
- [KT-10042](https://youtrack.jetbrains.com/issue/KT-10042) Correctly handle local classes and anonymous objects in control flow analysis
- [KT-11043](https://youtrack.jetbrains.com/issue/KT-11043) Prohibit complex expressions with class literals in annotation arguments
- [KT-10992](https://youtrack.jetbrains.com/issue/KT-10992), [KT-11007](https://youtrack.jetbrains.com/issue/KT-11007) Fix multiple problems related to smart casts
- [KT-11490](https://youtrack.jetbrains.com/issue/KT-11490) Prohibit nested intersection types in return position
- [KT-11411](https://youtrack.jetbrains.com/issue/KT-11411) Report "illegal noinline/crossinline" on parameter of subtype of function type
- [KT-3083](https://youtrack.jetbrains.com/issue/KT-3083) Report "conflicting overloads" for functions with parameter of type parameter type
- [KT-7265](https://youtrack.jetbrains.com/issue/KT-7265) Parse anonymous functions in blocks as expressions
- [KT-8246](https://youtrack.jetbrains.com/issue/KT-8246) Handle break/continue for outer loop correctly in case of try/finally in between
- [KT-11300](https://youtrack.jetbrains.com/issue/KT-11300) Report error on increment or augmented assignment when `get` is an operator but `set` is not
- Report warning about unused anonymous functions
- Improve callable reference type in some ambiguous cases
- Improve multiple diagnostic messages: [KT-10761](https://youtrack.jetbrains.com/issue/KT-10761), [KT-9760](https://youtrack.jetbrains.com/issue/KT-9760), [KT-10949](https://youtrack.jetbrains.com/issue/KT-10949), [KT-9887](https://youtrack.jetbrains.com/issue/KT-9887), [KT-9550](https://youtrack.jetbrains.com/issue/KT-9550), [KT-11239](https://youtrack.jetbrains.com/issue/KT-11239), [KT-11819](https://youtrack.jetbrains.com/issue/KT-11819)
- Fix several compiler bugs leading to exceptions: [KT-9820](https://youtrack.jetbrains.com/issue/KT-9820), [KT-11597](https://youtrack.jetbrains.com/issue/KT-11597), [KT-10983](https://youtrack.jetbrains.com/issue/KT-10983), [KT-10972](https://youtrack.jetbrains.com/issue/KT-10972), [KT-11287](https://youtrack.jetbrains.com/issue/KT-11287), [KT-11492](https://youtrack.jetbrains.com/issue/KT-11492), [KT-11765](https://youtrack.jetbrains.com/issue/KT-11765), [KT-11869](https://youtrack.jetbrains.com/issue/KT-11869)

#### JVM code generation

- [KT-8269](https://youtrack.jetbrains.com/issue/KT-8269), [KT-9246](https://youtrack.jetbrains.com/issue/KT-9246), [KT-10143](https://youtrack.jetbrains.com/issue/KT-10143) Fix visibility of protected classes in bytecode
- [KT-11363](https://youtrack.jetbrains.com/issue/KT-11363) Fix potential binary compatibility breakage on using `when` over enums in inline functions
- [KT-11762](https://youtrack.jetbrains.com/issue/KT-11762) Fix VerifyError caused by explicit loop variable type
- [KT-11645](https://youtrack.jetbrains.com/issue/KT-11645) Fix NoSuchFieldError on private const property in multi-file class
- [KT-9670](https://youtrack.jetbrains.com/issue/KT-9670) Optimize Class <-> KClass wrapping/unwrapping when getting values from annotation
- [KT-6842](https://youtrack.jetbrains.com/issue/KT-6842) Optimize unnecessary boxing and interface calls on iterating over ranges
- [KT-11025](https://youtrack.jetbrains.com/issue/KT-11025) Don't inline const val properties in non-annotation contexts
- [KT-5429](https://youtrack.jetbrains.com/issue/KT-5429) Write nullability annotations on extension receiver parameters
- [KT-11347](https://youtrack.jetbrains.com/issue/KT-11347) Preserve source file and line number of call site when inlining certain standard library functions
- [KT-11677](https://youtrack.jetbrains.com/issue/KT-11677) Write correct generic signatures for local classes in inlined lambdas
- [KT-12127](https://youtrack.jetbrains.com/issue/KT-12127) Do not write unnecessary generic signature for property delegate backing field
- Fix multiple issues leading to exceptions or bad bytecode being generated: [KT-11034](https://youtrack.jetbrains.com/issue/KT-11034), [KT-11519](https://youtrack.jetbrains.com/issue/KT-11519), [KT-11117](https://youtrack.jetbrains.com/issue/KT-11117), [KT-11479](https://youtrack.jetbrains.com/issue/KT-11479)

#### Java interoperability

- [KT-3068](https://youtrack.jetbrains.com/issue/KT-3068) Load contravariantly projected collections in Java (`List<? super T>`) as mutable collections in Kotlin (`MutableList<in T>`)
- [KT-11322](https://youtrack.jetbrains.com/issue/KT-11322) Do not lose type nullability information in SAM constructors
- [KT-11721](https://youtrack.jetbrains.com/issue/KT-11721) Fix wrong "Typechecker has run into recursive problem" error on calling Kotlin get function as synthetic Java property
- [KT-10691](https://youtrack.jetbrains.com/issue/KT-10691) Fix wrong "Inherited platform declarations clash" error on inheritance from generic Java class with overloaded methods

#### Command line compiler

- [KT-9546](https://youtrack.jetbrains.com/issue/KT-9546) Flush stdout and stderr before shutdown when executing scripts
- [KT-10605](https://youtrack.jetbrains.com/issue/KT-10605) Disable colored output on certain platforms to prevent crashes
- Report warning instead of error on unknown "-X" flags
- Remove the compiler option "Xmultifile-facades-open"

#### Compiler daemon

- Reduce read disk activity
- Fix compiler daemon JAR cache clearing on IDEA Ultimate

### Standard library

- [KT-11410](https://youtrack.jetbrains.com/issue/KT-11410) Reduce method count of the standard library by ~2k
- [KT-9990](https://youtrack.jetbrains.com/issue/KT-9990) Optimize snapshot operations to return special collection implementations when result is empty or has single element
- [KT-10794](https://youtrack.jetbrains.com/issue/KT-10794) EmptyList now implements RandomAccess
- [KT-10821](https://youtrack.jetbrains.com/issue/KT-10821) Create at most one wrapper sequence for adjacent drop/take operations on sequences
- [KT-11301](https://youtrack.jetbrains.com/issue/KT-11301) Make Map.plus accept Map out-projected by key type as either operand (receiver or parameter)
- [KT-11485](https://youtrack.jetbrains.com/issue/KT-11485) Remove implementations of some internal intrinsic functions
- [KT-11648](https://youtrack.jetbrains.com/issue/KT-11648) Add deprecated extension MutableList.remove to redirect to valid function removeAt
- [KT-11348](https://youtrack.jetbrains.com/issue/KT-11348) kotlin.test: Make inline methods `todo` and `currentStackTrace` `@InlineOnly` not to lose stack trace
- [KT-11745](https://youtrack.jetbrains.com/issue/KT-11745) Rename parameters of `String.subSequence` to match those of `CharSequence.subSequence`
- [KT-10953](https://youtrack.jetbrains.com/issue/KT-10953) Clarify parameter order of lambda function parameter of `*Indexed` functions
- [KT-10198](https://youtrack.jetbrains.com/issue/KT-10198) Improve docs for `binarySearch` functions
- [KT-9786](https://youtrack.jetbrains.com/issue/KT-9786) Improve docs for `trimIndent`/`trimMargin`

### Reflection

- [KT-9952](https://youtrack.jetbrains.com/issue/KT-9952) Improve `toString()` for lambdas and function expressions when kotlin-reflect.jar is available
- [KT-11433](https://youtrack.jetbrains.com/issue/KT-11433) Fix multiple resource leaks by closing InputStream instances
- [KT-8131](https://youtrack.jetbrains.com/issue/KT-8131) Fix exception from calling `KProperty.javaField` on a subclass
- [KT-10690](https://youtrack.jetbrains.com/issue/KT-10690) Support `javaMethod` and `kotlinFunction` for top level functions in a different file
- [KT-11447](https://youtrack.jetbrains.com/issue/KT-11447) Support reflection calls to multifile class members
- [KT-10892](https://youtrack.jetbrains.com/issue/KT-10892) Load annotations of const properties from multifile classes
- [KT-11258](https://youtrack.jetbrains.com/issue/KT-11258) Don't crash on requesting members of Java collection classes
- [KT-11502](https://youtrack.jetbrains.com/issue/KT-11502) Clarify KClass equality

### JS

- [KT-4124](https://youtrack.jetbrains.com/issue/KT-4124) Support nested classes
- [KT-11030](https://youtrack.jetbrains.com/issue/KT-11030) Support local classes
- [KT-7819](https://youtrack.jetbrains.com/issue/KT-7819) Support non-local returns in local lambdas
- [KT-6912](https://youtrack.jetbrains.com/issue/KT-6912) Safe calls (`x?.let { it }`) are now inlined
- [KT-2670](https://youtrack.jetbrains.com/issue/KT-2670) Support unsafe casts (`as`)
- [KT-7016](https://youtrack.jetbrains.com/issue/KT-7016), [KT-8012](https://youtrack.jetbrains.com/issue/KT-8012) Fix `is`-checks for reified type parameters
- [KT-7038](https://youtrack.jetbrains.com/issue/KT-7038) Avoid unwanted side effects on `is`-checks for nullable types
- [KT-10614](https://youtrack.jetbrains.com/issue/KT-10614) Copy array on vararg call with spread operator
- [KT-10785](https://youtrack.jetbrains.com/issue/KT-10785) Correctly translate property names and receiver instances in assignment operations
- [KT-11611](https://youtrack.jetbrains.com/issue/KT-11611) Fix translation of default value of secondary constructor's functional parameter
- [KT-11100](https://youtrack.jetbrains.com/issue/KT-11100) Fix generation of `invoke` on objects and companion objects
- [KT-11823](https://youtrack.jetbrains.com/issue/KT-11823) Fix capturing of outer class' `this` in inner's lambdas
- [KT-11996](https://youtrack.jetbrains.com/issue/KT-11996) Fix translation of a call to a private member of an outer class from an inner class which is a subtype of the outer class
- [KT-10667](https://youtrack.jetbrains.com/issue/KT-10667) Support inheritance from nested built-in types such as Map.Entry
- [KT-7480](https://youtrack.jetbrains.com/issue/KT-7480) Remove declarations of LinkedList, SortedSet, TreeSet, Enumeration
- [KT-3064](https://youtrack.jetbrains.com/issue/KT-3064) Implement `CharSequence.repeat`

### IDE

New features:

- Spring Support
  - [KT-11098](https://youtrack.jetbrains.com/issue/KT-11098) Inspection on final classes/functions annotated with Spring `@Configuration`/`@Component`/`@Bean`
  - [KT-11405](https://youtrack.jetbrains.com/issue/KT-11405) Navigation and Find Usages for Spring beans referenced in annotation arguments and BeanFactory method calls
  - [KT-3741](https://youtrack.jetbrains.com/issue/KT-3741) Show Spring-specific line markers on Kotlin classes
  - [KT-11406](https://youtrack.jetbrains.com/issue/KT-11406) Support Spring EL injections inside of Kotlin string literals
  - [KT-11604](https://youtrack.jetbrains.com/issue/KT-11604) Support "Configure Spring facet" inspection on Kotlin classes
  - [KT-11407](https://youtrack.jetbrains.com/issue/KT-11407) Implement "Generate Spring Dependency..." actions
  - [KT-11408](https://youtrack.jetbrains.com/issue/KT-11408) Implement "Generate `@Autowired` Dependency..." action
  - [KT-11652](https://youtrack.jetbrains.com/issue/KT-11652) Rename bean attributes mentioned in Spring XML config together with corresponding Kotlin declarations
- Enable precise incremental compilation by default in non-Maven/Gradle projects
- [KT-11612](https://youtrack.jetbrains.com/issue/KT-11612) Highlight named arguments
- [KT-7715](https://youtrack.jetbrains.com/issue/KT-7715) Highlight `var`s that can be replaced by `val`s
- [KT-5208](https://youtrack.jetbrains.com/issue/KT-5208) Intention action to convert string to raw string and back
- [KT-11078](https://youtrack.jetbrains.com/issue/KT-11078) Quick fix to remove `.java` when KClass is expected
- [KT-1494](https://youtrack.jetbrains.com/issue/KT-1494) Inspection to highlight public members with no documentation
- [KT-8473](https://youtrack.jetbrains.com/issue/KT-8473) Intention action to implement interface or abstract class
- [KT-10299](https://youtrack.jetbrains.com/issue/KT-10299) Inspection to warn on array properties in data classes
- [KT-6674](https://youtrack.jetbrains.com/issue/KT-6674) Inspection to warn on protected symbols in effectively final classes
- [KT-11576](https://youtrack.jetbrains.com/issue/KT-11576) Quick fix to suppress "Unused symbol" warning based on annotations on the declaration
- [KT-10063](https://youtrack.jetbrains.com/issue/KT-10063) Quick fix for adding `arrayOf` wrapper for annotation parameters
- [KT-10476](https://youtrack.jetbrains.com/issue/KT-10476) Quick fix for converting primitive types
- [KT-10859](https://youtrack.jetbrains.com/issue/KT-10859) Quick fix to make `var` with private setter final
- [KT-9498](https://youtrack.jetbrains.com/issue/KT-9498) Quick fix to specify property type
- [KT-10509](https://youtrack.jetbrains.com/issue/KT-10509) Quick fix to simplify condition with senseless comparison
- [KT-11404](https://youtrack.jetbrains.com/issue/KT-11404) Quick fix to let type implement missing interface
- [KT-6785](https://youtrack.jetbrains.com/issue/KT-6785), [KT-10013](https://youtrack.jetbrains.com/issue/KT-10013), [KT-9996](https://youtrack.jetbrains.com/issue/KT-9996), [KT-11675](https://youtrack.jetbrains.com/issue/KT-11675) Support Smart Enter for trailing lambda argument, try/catch/finally, property setter, init block
- Add `kotlinClassName()` and `kotlinFunctionName()` macros for use in live templates
- Auto-configure EAP-repository during Kotlin Maven and Gradle project set up

Issues fixed:

- [KT-11678](https://youtrack.jetbrains.com/issue/KT-11678), [KT-4768](https://youtrack.jetbrains.com/issue/KT-4768) Support navigation to Kotlin libraries from Java sources
- [KT-9401](https://youtrack.jetbrains.com/issue/KT-9401) Support Change Signature quick fix for Java -> Kotlin case
- [KT-8592](https://youtrack.jetbrains.com/issue/KT-8592) Fix "Choose sources" for Kotlin files
- [KT-11256](https://youtrack.jetbrains.com/issue/KT-11256) Fix Navigate to declaration for Java constructor with `@NotNull` parameter
- [KT-11018](https://youtrack.jetbrains.com/issue/KT-11018) Fix `var`s shown in Ctrl + Mouse Hover as `val`s
- [KT-5105](https://youtrack.jetbrains.com/issue/KT-5105), [KT-11024](https://youtrack.jetbrains.com/issue/KT-11024) Improve incompatible ABI versions editor strap, show the hint on how to resolve the problem
- [KT-11638](https://youtrack.jetbrains.com/issue/KT-11638) Fixed `hashCode()` implementation in "Generate equals/hashCode" action
- [KT-10971](https://youtrack.jetbrains.com/issue/KT-10971) Pull Members Up: Always insert spaces between keywords
- [KT-11476](https://youtrack.jetbrains.com/issue/KT-11476), [KT-4175](https://youtrack.jetbrains.com/issue/KT-4175), [KT-10965](https://youtrack.jetbrains.com/issue/KT-10965), [KT-11076](https://youtrack.jetbrains.com/issue/KT-11076) Formatter: fix multiple issues regarding space handling
- [KT-9025](https://youtrack.jetbrains.com/issue/KT-9025) Improve "Create Kotlin Java runtime library" dialog usability
- [KT-11481](https://youtrack.jetbrains.com/issue/KT-11481) Fix "Add import" intention not being available for `is` branches in when
- [KT-10619](https://youtrack.jetbrains.com/issue/KT-10619) Fix completion after package name in annotation
- [KT-10621](https://youtrack.jetbrains.com/issue/KT-10621) Do not show non-top level packages after `@` in completion
- [KT-11295](https://youtrack.jetbrains.com/issue/KT-11295) "Convert string to template" intention: fix exception on certain code
- [KT-10750](https://youtrack.jetbrains.com/issue/KT-10750), [KT-11424](https://youtrack.jetbrains.com/issue/KT-11424) "Convert if to when" intention now detects effectively else branches in subsequent code and performs more accurate comment handling
- Configure Kotlin: show only changed files in the notification "Kotlin not configured", restore all changed files in undo action
- [KT-11556](https://youtrack.jetbrains.com/issue/KT-11556) Do not show "Kotlin not configured" for Kotlin JS projects
- [KT-11593](https://youtrack.jetbrains.com/issue/KT-11593) Fix "Configure Kotlin" action for Gradle projects in IDEA 2016
- [KT-11077](https://youtrack.jetbrains.com/issue/KT-11077) Use new built-in definition file format (`.kotlin_builtins` files)
- [KT-5728](https://youtrack.jetbrains.com/issue/KT-5728) Remove closing curly brace in a string template when opening one is deleted
- [KT-10883](https://youtrack.jetbrains.com/issue/KT-10883) "Explicit get or set call" quick fix: do not move caret too far away
- [KT-5717](https://youtrack.jetbrains.com/issue/KT-5717) "Replace `when` with `if`": do not lose comments
- [KT-10797](https://youtrack.jetbrains.com/issue/KT-10797) "Replace with operator" intention is not available anymore for non-`operator` functions
- [KT-11529](https://youtrack.jetbrains.com/issue/KT-11529) Highlighting range for unresolved annotation name does not include `@` now
- [KT-11178](https://youtrack.jetbrains.com/issue/KT-11178) Don't show "Change type arguments" fix when there's nothing to change
- [KT-11789](https://youtrack.jetbrains.com/issue/KT-11789) Don't interpret annotations inside Markdown code blocks as KDoc tags
- [KT-11702](https://youtrack.jetbrains.com/issue/KT-11702) Fixed resolution of Kotlin beans with custom name
- [KT-11689](https://youtrack.jetbrains.com/issue/KT-11689) Fixed exception on attempt to navigate to Kotlin file from Spring notification balloon
- [KT-11725](https://youtrack.jetbrains.com/issue/KT-11725) Fixed renaming of injected SpEL references
- [KT-11720](https://youtrack.jetbrains.com/issue/KT-11720) Fixed renaming of Kotlin beans through SpEL references
- [KT-11719](https://youtrack.jetbrains.com/issue/KT-11719) Fixed renaming of Kotlin parameters references in XML files
- [KT-11736](https://youtrack.jetbrains.com/issue/KT-11736) Fixed searching of Java usages for @JvmStatic properties and @JvmStatic @JvmOverloads functions
- [KT-11862](https://youtrack.jetbrains.com/issue/KT-11862) Fixed bogus warnings about unresolved types in the Change Signature dialog
- Fix several issues leading to exceptions: [KT-11579](https://youtrack.jetbrains.com/issue/KT-11579), [KT-11580](https://youtrack.jetbrains.com/issue/KT-11580), [KT-11777](https://youtrack.jetbrains.com/issue/KT-11777), [KT-11868](https://youtrack.jetbrains.com/issue/KT-11868), [KT-11845](https://youtrack.jetbrains.com/issue/KT-11845), [KT-11486](https://youtrack.jetbrains.com/issue/KT-11486)
- Fixed NoSuchFieldException in Kotlin module settings on IDEA Ultimate

#### Debugger

- [KT-11705](https://youtrack.jetbrains.com/issue/KT-11705) "Smart step into" no longer skips methods from subclasses
- Debugger can now distinguish nested inline arguments
- [KT-11326](https://youtrack.jetbrains.com/issue/KT-11326) Support private classes in Evaluate Expression
- [KT-11455](https://youtrack.jetbrains.com/issue/KT-11455) Fix Evaluate Expression behavior for files with errors in sources
- [KT-10670](https://youtrack.jetbrains.com/issue/KT-10670) Fix Evaluate Expression behavior for inline functions with default parameters
- [KT-11380](https://youtrack.jetbrains.com/issue/KT-11380) Evaluate Expression now handles smart casts correctly
- [KT-10148](https://youtrack.jetbrains.com/issue/KT-10148) Do not suggest methods from outer context in "Smart step into"
- Fix Evaluate Expression for expression created for array element
- Complete private members from libraries in Evaluate Expression
- [KT-11578](https://youtrack.jetbrains.com/issue/KT-11578) Evaluate Expression: do not highlight completion variants from nullable receiver with grey
- [KT-6805](https://youtrack.jetbrains.com/issue/KT-6805) Convert Java expression to Kotlin when opening Evaluate Expression from Variables view
- [KT-11927](https://youtrack.jetbrains.com/issue/KT-11927) Fix "ambiguous import" error when invoking Evaluate Expression from Variables view for some field
- [KT-11831](https://youtrack.jetbrains.com/issue/KT-11831) Fix Evaluate Expression for values of raw types
- Show error message when debug info for some local variable is corrupted
- Avoid 1s delay in completion in debugger fields if session is not stopped on a breakpoint
- Avoid cast to runtime type unavailable in current scope
- Fix text with line breaks in popup with line breakpoint variants
- Fix breakpoints inside inline functions in libraries sources
- Allow breakpoints at catch clause declaration
- [KT-11848](https://youtrack.jetbrains.com/issue/KT-11848) Fix breakpoints inside generic crossinline lambda argument body
- [KT-11932](https://youtrack.jetbrains.com/issue/KT-11932) Fix Step Over for `while` loop condition

### Java to Kotlin converter

- Protected members used outside of inheritors are converted as public
- Support conversion for annotation constructor calls
- Place comments from the middle of the call to the end
- Drop line breaks between operator arguments (except `+`, `-`, `&&` and `||`)
- Add non-null assertions on call site for non-null parameters
- Specify type for variables with anonymous type if they have write accesses
- [KT-11587](https://youtrack.jetbrains.com/issue/KT-11587) Fix conversion of static field accesses from other Java class
- [KT-6800](https://youtrack.jetbrains.com/issue/KT-6800) Quote `$` symbols in converted strings
- [KT-11126](https://youtrack.jetbrains.com/issue/KT-11126) Convert annotations in annotations parameters correctly
- [KT-11600](https://youtrack.jetbrains.com/issue/KT-11600) Do not produce unresolved `toArray` calls for Java `Collection#toArray(T[])`
- [KT-11544](https://youtrack.jetbrains.com/issue/KT-11544) Fix conversion of uninitialized non-final field
- [KT-10604](https://youtrack.jetbrains.com/issue/KT-10604) Fix conversion of scratch files
- [KT-11543](https://youtrack.jetbrains.com/issue/KT-11543) Do not produce unnecessary casts of non-nullable expression to nullable type
- [KT-11160](https://youtrack.jetbrains.com/issue/KT-11160) Fix IDE freeze

### Android

- [KT-7729](https://youtrack.jetbrains.com/issue/KT-7729) Add Android Lint checks for Kotlin (from Android Studio 1.5)
- [KT-11487](https://youtrack.jetbrains.com/issue/KT-11487) Fixed sequential build with kapt and stubs enabled when Kotlin source file was modified and no Java source files were modified
- [KT-11264](https://youtrack.jetbrains.com/issue/KT-11264) Action to create new activity in Kotlin
- [KT-11201](https://youtrack.jetbrains.com/issue/KT-11201) Do not ignore items with similar names in kapt
- [KT-11944](https://youtrack.jetbrains.com/issue/KT-11944) Rename Android Extensions imports when the layout file is renamed/deleted/added
- [KT-10321](https://youtrack.jetbrains.com/issue/KT-10321) Do not upcast ViewStub to View
- [KT-10841](https://youtrack.jetbrains.com/issue/KT-10841) Support `@android:id/*` IDs in Android Extensions

### Maven

- [KT-2917](https://youtrack.jetbrains.com/issue/KT-2917), [KT-11261](https://youtrack.jetbrains.com/issue/KT-11261) Maven archetype for new Kotlin projects

### Gradle

- [KT-8487](https://youtrack.jetbrains.com/issue/KT-8487) Experimental support for incremental compilation with project property `kotlin.incremental`
- [KT-11350](https://youtrack.jetbrains.com/issue/KT-11350) Fixed a bug causing Java rebuild when both Java and Kotlin are up-to-date
- [KT-10507](https://youtrack.jetbrains.com/issue/KT-10507) Fix IllegalArgumentException "Missing extension point" on parallel builds
- [KT-10932](https://youtrack.jetbrains.com/issue/KT-10932) Prevent compile tasks from running when nothing changes
- [KT-11993](https://youtrack.jetbrains.com/issue/KT-11993) Fix NoSuchMethodError on access to internal members in production from tests (IDEA 2016+)

## 1.0.1-2

### Compiler

- [KT-11584](https://youtrack.jetbrains.com/issue/KT-11584), [KT-11514](https://youtrack.jetbrains.com/issue/KT-11514) Correct comparison of Long! / Double! with integer constant
- [KT-11590](https://youtrack.jetbrains.com/issue/KT-11590) SAM adapter for inline function corrected

## 1.0.1-1

### Compiler

- [KT-11468](https://youtrack.jetbrains.com/issue/KT-11468) More correct use-site / declaration-site variance combination handling
- [KT-11478](https://youtrack.jetbrains.com/issue/KT-11478) "Couldn't inline method call" internal compiler error fixed

## 1.0.1

### Compiler

Analysis & diagnostics issues fixed:

- [KT-2277](https://youtrack.jetbrains.com/issue/KT-2277) Local function declarations are now checked for overload conflicts
- [KT-3602](https://youtrack.jetbrains.com/issue/KT-3602)  Special diagnostic is reported now on nullable ‘for’ range
- [KT-10775](https://youtrack.jetbrains.com/issue/KT-10775) No compilation exception for empty when
- [KT-10952](https://youtrack.jetbrains.com/issue/KT-10952) False deprecation warnings removed
- [KT-10934](https://youtrack.jetbrains.com/issue/KT-10934) Type inference improved for whens
- [KT-10902](https://youtrack.jetbrains.com/issue/KT-10902) Redeclaration is reported for top-level property vs classifier conflict
- [KT-9985](https://youtrack.jetbrains.com/issue/KT-9985)  Correct handling of safe call arguments in generic functions
- [KT-10856](https://youtrack.jetbrains.com/issue/KT-10856) Diagnostic about projected out member is reported correctly on calls with smart cast receiver
- [KT-5190](https://youtrack.jetbrains.com/issue/KT-5190)  Calls of Java 8 Stream.collect
- [KT-11109](https://youtrack.jetbrains.com/issue/KT-11109) Warning is reported on Strictfp annotation on a class because it's not supported yet
- [KT-10686](https://youtrack.jetbrains.com/issue/KT-10686) Support generic constructors defined in Java
- [KT-6958](https://youtrack.jetbrains.com/issue/KT-6958)  Fixed resolution for overloaded functions with extension lambdas
- [KT-10765](https://youtrack.jetbrains.com/issue/KT-10765) Correct handling of overload conflict between constructor and function in JPS
- [KT-10752](https://youtrack.jetbrains.com/issue/KT-10752) If inferred type for an expression refers to a non-accessible Java class, it's a compiler error to prevent IAE in runtime
- [KT-7415](https://youtrack.jetbrains.com/issue/KT-7415) Approximation of captured types in signatures
- [KT-10913](https://youtrack.jetbrains.com/issue/KT-10913), [KT-10186](https://youtrack.jetbrains.com/issue/KT-10186), [KT-5198](https://youtrack.jetbrains.com/issue/KT-5198) False “unreachable code” fixed for various situations
- Minor: [KT-3680](https://youtrack.jetbrains.com/issue/KT-3680), [KT-9702](https://youtrack.jetbrains.com/issue/KT-9702), [KT-8776](https://youtrack.jetbrains.com/issue/KT-8776), [KT-6745](https://youtrack.jetbrains.com/issue/KT-6745), [KT-10919](https://youtrack.jetbrains.com/issue/KT-10919), [KT-9548](https://youtrack.jetbrains.com/issue/KT-9548)

JVM code generation issues fixed:

- [KT-11153](https://youtrack.jetbrains.com/issue/KT-11153) NoClassDefFoundError is fixed on primitive iterators during boxing optimization
- [KT-7319](https://youtrack.jetbrains.com/issue/KT-7319)  Correct parameter names for @JvmOverloads-generated methods
- [KT-10425](https://youtrack.jetbrains.com/issue/KT-10425) Non-const values of member properties are not inlined now
- [KT-11163](https://youtrack.jetbrains.com/issue/KT-11163) Correct calls of custom compareTo on primitives
- [KT-11081](https://youtrack.jetbrains.com/issue/KT-11081) Reified type parameters are correctly stored in anonymous objects
- [KT-11121](https://youtrack.jetbrains.com/issue/KT-11121) Generic properties generation is fixed for interfaces
- [KT-11285](https://youtrack.jetbrains.com/issue/KT-11285), [KT-10958](https://youtrack.jetbrains.com/issue/KT-10958) Special bridge generation refined
- [KT-10313](https://youtrack.jetbrains.com/issue/KT-10313), [KT-11190](https://youtrack.jetbrains.com/issue/KT-11190), [KT-11192](https://youtrack.jetbrains.com/issue/KT-11192), [KT-11130](https://youtrack.jetbrains.com/issue/KT-11130) Diagnostics and bytecode fixed for various operations with Long
- [KT-11203](https://youtrack.jetbrains.com/issue/KT-11203), [KT-11191](https://youtrack.jetbrains.com/issue/KT-11191), [KT-11206](https://youtrack.jetbrains.com/issue/KT-11206), [KT-8505](https://youtrack.jetbrains.com/issue/KT-8505), [KT-11203](https://youtrack.jetbrains.com/issue/KT-11203) Handling of increment / decrement for collection elements with user-defined get / set fixed
- [KT-9739](https://youtrack.jetbrains.com/issue/KT-9739)  Backticked names with spaces are generated correctly

JS translator issues fixed:

- [KT-7683](https://youtrack.jetbrains.com/issue/KT-7683), [KT-11027](https://youtrack.jetbrains.com/issue/KT-11027) correct handling of in / !in inside when expressions

### Standard library

- [KT-10579](https://youtrack.jetbrains.com/issue/KT-10579) Improved performance of sum() and average() for arrays
- [KT-10821](https://youtrack.jetbrains.com/issue/KT-10821) Improved performance of drop() / take() for sequences

### Reflection

- [KT-10840](https://youtrack.jetbrains.com/issue/KT-10840) Fix annotations on Java elements in reflection

### IDE

New features:

- Compatibility with IDEA 2016
- Kotlin Education Plugin (for IDEA 2016)
- [KT-9752](https://youtrack.jetbrains.com/issue/KT-9752)  More usable file chooser for "Move declaration to another file"
- [KT-9697](https://youtrack.jetbrains.com/issue/KT-9697)  Move method to companion object and back
- [KT-7443](https://youtrack.jetbrains.com/issue/KT-7443) Inspection + intention to replace assert (x != null) with "!!" or elvis

General issues fixed:

- [KT-11277](https://youtrack.jetbrains.com/issue/KT-11277) Correct moving of Java classes from project view
- [KT-11256](https://youtrack.jetbrains.com/issue/KT-11256) Navigate Declaration fixed for Java classes with @NotNull parameter in constructor
- [KT-10553](https://youtrack.jetbrains.com/issue/KT-10553) A warning provided when Refactor / Move result is not compilable due to visibility problems
- [KT-11039](https://youtrack.jetbrains.com/issue/KT-11039) Parameter names are now not missing in parameter info and completion for compiled java code used from kotlin
- [KT-10204](https://youtrack.jetbrains.com/issue/KT-10204) Highlight usages in file is working now for function parameter
- [KT-10954](https://youtrack.jetbrains.com/issue/KT-10954) Introduce Parameter (Ctrl+Alt+P) fixed when default value is a simple name reference
- [KT-10776](https://youtrack.jetbrains.com/issue/KT-10776) Intentions: "Convert to lambda expression" works now for empty function body
- [KT-10815](https://youtrack.jetbrains.com/issue/KT-10815) Generate equals() and hashCode() is no more suggested for interfaces
- [KT-10818](https://youtrack.jetbrains.com/issue/KT-10818) "Initialize with constructor parameter" fixed
- [KT-8876](https://youtrack.jetbrains.com/issue/KT-8876) "Convert member to extension" now removes modality modifiers (open / final)
- [KT-10800](https://youtrack.jetbrains.com/issue/KT-10800) Create enum entry now adds comma after a new entry
- [KT-10552](https://youtrack.jetbrains.com/issue/KT-10552) Pull Members Up now takes visibility conflicts into account
- [KT-10978](https://youtrack.jetbrains.com/issue/KT-10978) Partially fixed, completion for JOOQ became ~ 10 times faster
- [KT-10940](https://youtrack.jetbrains.com/issue/KT-10940) Reference search optimized for convention functions
- [KT-9026](https://youtrack.jetbrains.com/issue/KT-9026)  Editor no more locks up during scala file viewing
- [KT-11142](https://youtrack.jetbrains.com/issue/KT-11142), [KT-11276](https://youtrack.jetbrains.com/issue/KT-11276) Darkula scheme appearance corrected for Kotlin
- Minor: [KT-10778](https://youtrack.jetbrains.com/issue/KT-10778), [KT-10763](https://youtrack.jetbrains.com/issue/KT-10763), [KT-10908](https://youtrack.jetbrains.com/issue/KT-10908), [KT-10345](https://youtrack.jetbrains.com/issue/KT-10345), [KT-10696](https://youtrack.jetbrains.com/issue/KT-10696), [KT-11041](https://youtrack.jetbrains.com/issue/KT-11041), [KT-9434](https://youtrack.jetbrains.com/issue/KT-9434), [KT-8744](https://youtrack.jetbrains.com/issue/KT-8744), [KT-9738](https://youtrack.jetbrains.com/issue/KT-9738), [KT-10912](https://youtrack.jetbrains.com/issue/KT-10912)

Configuration issues fixed:

- [KT-11213](https://youtrack.jetbrains.com/issue/KT-11213) Kotlin plugin version corrected in build.gradle
- [KT-10918](https://youtrack.jetbrains.com/issue/KT-10918) "Update Kotlin runtime" action does not try to update the runtime coming in from Gradle
- [KT-11072](https://youtrack.jetbrains.com/issue/KT-11072) Libraries in maven, gradle and ide systems are never more detected as runtime libraries
- [KT-10489](https://youtrack.jetbrains.com/issue/KT-10489) Configuration messages are aggregated into one notification
- [KT-10831](https://youtrack.jetbrains.com/issue/KT-10831) Configure Kotlin in Project: "All modules containing Kotlin files" does not list modules not containing Kotlin files
- [KT-10366](https://youtrack.jetbrains.com/issue/KT-10366) Gradle import: no fake "Configure Kotlin" notification on project creating

Debugger issues fixed:

- [KT-10827](https://youtrack.jetbrains.com/issue/KT-10827) Fixed debugger stepping for inline calls
- [KT-10780](https://youtrack.jetbrains.com/issue/KT-10780) Breakpoints in a lazy property work correctly
- [KT-10634](https://youtrack.jetbrains.com/issue/KT-10634) Watches can now use private overloaded functions
- [KT-10611](https://youtrack.jetbrains.com/issue/KT-10611) Line breakpoints now can be created inside lambda in init block
- [KT-10673](https://youtrack.jetbrains.com/issue/KT-10673) Breakpoints inside lambda are no more ignored in presence of crossinline function parameter
- [KT-11318](https://youtrack.jetbrains.com/issue/KT-11318) Stepping inside for each is optimized
- [KT-3873](https://youtrack.jetbrains.com/issue/KT-3873)  Editing code while standing on breakpoint is optimized
- [KT-7261](https://youtrack.jetbrains.com/issue/KT-7261), [KT-7266](https://youtrack.jetbrains.com/issue/KT-7266), [KT-10672](https://youtrack.jetbrains.com/issue/KT-10672) Evaluate expression applicability corrected

### Tools

- [KT-7943](https://youtrack.jetbrains.com/issue/KT-7943), [KT-10127](https://youtrack.jetbrains.com/issue/KT-10127) Overhead removed in Kotlin Gradle Plugin
- [KT-11351](https://youtrack.jetbrains.com/issue/KT-11351) Fixed NoSuchMethodError with Gradle 2.12
