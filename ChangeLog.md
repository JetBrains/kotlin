# CHANGELOG

## 1.3-M2

### Compiler

#### New Features

- [`KT-6592`](https://youtrack.jetbrains.com/issue/KT-6592) Support local variable initialization in lambda arguments to some functions
- [`KT-19323`](https://youtrack.jetbrains.com/issue/KT-19323) Allow generic type parameter to have mixed constraints for @InlineOnly functions
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

### IDE. Libraries

- [`KT-25962`](https://youtrack.jetbrains.com/issue/KT-25962) Add contract for 'synchronized'

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
- [`KT-18986`](https://youtrack.jetbrains.com/issue/KT-18986) Debug-friendly toString implementation for CoroutineImpl
- [`KT-19532`](https://youtrack.jetbrains.com/issue/KT-19532) Evaluation order for constructor call
- [`KT-22274`](https://youtrack.jetbrains.com/issue/KT-22274) Restrict statement labels applicability

### Libraries

#### New Features

- [`KT-13814`](https://youtrack.jetbrains.com/issue/KT-13814) keys.associateWith { k -> v } function
- [`KT-15695`](https://youtrack.jetbrains.com/issue/KT-15695) String/Collection/Map/Array/Sequence.ifEmpty { null }
- [`KT-25659`](https://youtrack.jetbrains.com/issue/KT-25659) Consider adding SuccessOrFailure.getOrDefault function

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
- [`KT-21972`](https://youtrack.jetbrains.com/issue/KT-21972) Reflection: Implement suspend functions `KCallable.callSuspend`,  `callSuspendBy`, and `isSuspend`

### Tools. Gradle

- [`KT-26301`](https://youtrack.jetbrains.com/issue/KT-26301) In new MPP, a project with no `java` or `java-base` plugin applied cannot depend on a published MPP lib or run tests

### Tools. Scripts

- [`KT-26142`](https://youtrack.jetbrains.com/issue/KT-26142) update maven-central remote repository url
