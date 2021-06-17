# CHANGELOG

## 1.5.10

### Compiler

#### Fixes

- [`KT-41078`](https://youtrack.jetbrains.com/issue/KT-41078) Incorrect type substitution in contracts with type parameters
- [`KT-44770`](https://youtrack.jetbrains.com/issue/KT-44770) JVM / IR: "IllegalArgumentException: Unrecognized Type: [null]" Jackson doesn't recognize type
- [`KT-45084`](https://youtrack.jetbrains.com/issue/KT-45084) JVM IR: "NoSuchElementException: Sequence contains no element matching the predicate" when inline class is passed to lambda with >22 parameters
- [`KT-45779`](https://youtrack.jetbrains.com/issue/KT-45779) JVM / IR: java.lang.NoSuchMethodError: 'int java.lang.Integer.plus(int)' caused by function reference
- [`KT-45941`](https://youtrack.jetbrains.com/issue/KT-45941) JVM IR: local functions use generic type parameters of the outer class in the bytecode, which breaks Bytebuddy and MockK
- [`KT-46149`](https://youtrack.jetbrains.com/issue/KT-46149) Generate synthetic classes for SAM adapters with erased instead of generic supertype
- [`KT-46189`](https://youtrack.jetbrains.com/issue/KT-46189) JVM IR: tailrec function with capturing lambda in default parameter value leads to NoSuchMethodError at runtime
- [`KT-46214`](https://youtrack.jetbrains.com/issue/KT-46214) JVM / IR: "IllegalStateException: No mapping for symbol: VALUE_PARAMETER INSTANCE_RECEIVER" on a suspend function in an inner class
- [`KT-46238`](https://youtrack.jetbrains.com/issue/KT-46238) JVM IR: BootstrapMethodError in JDK 11+ on intersection type passed in arguments of SAM adapter where SAM interface's type parameter has a non-trivial upper bound
- [`KT-46259`](https://youtrack.jetbrains.com/issue/KT-46259) JVM IR: local function for adapted function reference is not declared as ACC_SYNTHETIC
- [`KT-46284`](https://youtrack.jetbrains.com/issue/KT-46284) JVM IR: "Unbound private symbol IrClassSymbol" on class reference to script class
- [`KT-46402`](https://youtrack.jetbrains.com/issue/KT-46402) IllegalAccessError: "CapturedLambdaInterpreter (in unnamed module @0x71b06418) cannot access class jdk.internal.org.objectweb.asm.Type" caused by inline function with a suspend parameter in Maven project
- [`KT-46408`](https://youtrack.jetbrains.com/issue/KT-46408) JVM IR: BootstrapMethodError due to missing bridge for subclass of generic Java interface
- [`KT-46426`](https://youtrack.jetbrains.com/issue/KT-46426) JVM IR: Corrupted .class file when passing Array constructor reference as (inline) lambda
- [`KT-46455`](https://youtrack.jetbrains.com/issue/KT-46455) OOM on parsing invalid code with string interpolation
- [`KT-46503`](https://youtrack.jetbrains.com/issue/KT-46503) JVM IR: "AssertionError: Unexpected variance in super type argument: out @1"
- [`KT-46505`](https://youtrack.jetbrains.com/issue/KT-46505) JVM IR: NullPointerException caused by a callable reference with nullable inline value class parameter
- [`KT-46512`](https://youtrack.jetbrains.com/issue/KT-46512) JVM / IR: NoSuchMethodError on SAM conversion of a function reference
- [`KT-46515`](https://youtrack.jetbrains.com/issue/KT-46515) IndexOutOfBoundsException: "Empty list doesn't contain element at index 0." on bad variable name in 1.5.0
- [`KT-46516`](https://youtrack.jetbrains.com/issue/KT-46516) JVM IR: "AnalyzerException: Expected I, but found R" on subclassing AbstractMutableList<Int>
- [`KT-46524`](https://youtrack.jetbrains.com/issue/KT-46524) Cannot use unsigned literals with api-version < 1.5 even with opt-in
- [`KT-46537`](https://youtrack.jetbrains.com/issue/KT-46537) JVM / IR: "IllegalStateException: No noarg super constructor for CLASS" caused by "No-arg" plugin with annotation on child class
- [`KT-46540`](https://youtrack.jetbrains.com/issue/KT-46540) JVM / IR: AnalyzerException: Expected an object reference, but found J caused by java.function.Supplier
- [`KT-46554`](https://youtrack.jetbrains.com/issue/KT-46554) JVM IR: "IllegalStateException: No mapping for symbol: VAR IR_TEMPORARY_VARIABLE" with value class constructor delegation call
- [`KT-46555`](https://youtrack.jetbrains.com/issue/KT-46555) JVM IR: IllegalAccessError when using Java method reference
- [`KT-46562`](https://youtrack.jetbrains.com/issue/KT-46562) Kotlin 1.5.0 generates non-serializable lambdas when they should be serializable
- [`KT-46568`](https://youtrack.jetbrains.com/issue/KT-46568) JVM IR: "AssertionError: IrCall expected inside JvmStatic wrapper" on compiling protected static function with return type Nothing inside companion object of abstract class
- [`KT-46574`](https://youtrack.jetbrains.com/issue/KT-46574) JVM / IR: ClassCastException caused by runBlocking awaiting call while returning Kotlin.Result type.
- [`KT-46579`](https://youtrack.jetbrains.com/issue/KT-46579) JVM IR: "IllegalArgumentException: Sequence contains more than one matching element" for Java enum with overloaded values() static method
- [`KT-46584`](https://youtrack.jetbrains.com/issue/KT-46584) JVM IR: Intrinsics.needClassReification (UnsupportedOperationException thrown). Property delegate provider crossinline lambda inlining/reification issue
- [`KT-46751`](https://youtrack.jetbrains.com/issue/KT-46751) JVM / IR:"ClassCastException: java.lang.String cannot be cast to java.lang.Void" in extension function in Kotlin 1.5

### IDE

- [`KT-45981`](https://youtrack.jetbrains.com/issue/KT-45981) failed to analyze: java.lang.AssertionError: diagnostic callback has been already registered: Code analysis get stuck in AS 2020.3.1.14 & Kotlin v1.5.0-M2
- [`KT-46622`](https://youtrack.jetbrains.com/issue/KT-46622) 60+ second freezes with Kotlin plugin 1.5.0: GetModuleInfoKt.findJvmStdlibAcrossDependencies

### IDE. Gradle Integration

- [`KT-46417`](https://youtrack.jetbrains.com/issue/KT-46417) [UNRESOLVED_REFERENCE] For project to project dependencies of native platform test source sets

### Libraries

- [`KT-46280`](https://youtrack.jetbrains.com/issue/KT-46280) JvmRecord annotation missing constructor in common

### Middle-end. IR

- [`KT-44013`](https://youtrack.jetbrains.com/issue/KT-44013) NPE: When calling constructor of a function type while inheriting from it, despite it's an interface

### Tools. Android Extensions

- [`KT-46590`](https://youtrack.jetbrains.com/issue/KT-46590) Kotlin Android Extensions 1.5.0 generates bad writeToParcel() method for nullable Array types

### Tools. Gradle

- [`KT-41142`](https://youtrack.jetbrains.com/issue/KT-41142) Kotlin version conflict when using Kotlin Gradle plugins in pre-compiled script plugin
- [`KT-46353`](https://youtrack.jetbrains.com/issue/KT-46353) Optimizations disabled in Gradle 7 for KAPT when generating sources
- [`KT-46368`](https://youtrack.jetbrains.com/issue/KT-46368) Memory leak with 1.5.0-RC when building with Gradle
- [`KT-46689`](https://youtrack.jetbrains.com/issue/KT-46689) Track -Xuse-old-backend flag usage

### Tools. Gradle. JS

- [`KT-46006`](https://youtrack.jetbrains.com/issue/KT-46006) KJS \ Gradle: Task without declaring an explicit or implicit dependency on `jsGenerateExternalsIntegrated` in Gradle 7
- [`KT-46162`](https://youtrack.jetbrains.com/issue/KT-46162) KJS: Exported items unavailable on dev server
- [`KT-46331`](https://youtrack.jetbrains.com/issue/KT-46331) KJS: With `kotlin.js.webpack.major.version=4` browserXRun tasks fail

### Tools. Parcelize

- [`KT-46567`](https://youtrack.jetbrains.com/issue/KT-46567) Kotlin 1.5.0 parcelize compilation fails in new backend when using TypeParceller with nested generics

### Tools. kapt

- [`KT-45532`](https://youtrack.jetbrains.com/issue/KT-45532) Do not create Kapt stubs directory during configuration time


## 1.5.0

### Backend. Native

- [`KT-42053`](https://youtrack.jetbrains.com/issue/KT-42053) Support compiler caches for linux_x64
- [`KT-43690`](https://youtrack.jetbrains.com/issue/KT-43690) Support compiler caches for ios_arm64

### Backend. IR

- [`KT-42684`](https://youtrack.jetbrains.com/issue/KT-42684) StackOverflowError on recursive inline arguments in inline fun

### Compiler

#### New Features

- [`KT-28791`](https://youtrack.jetbrains.com/issue/KT-28791) Kotlin serialization with inline classes
- [`KT-30222`](https://youtrack.jetbrains.com/issue/KT-30222) Support JVM target version selection in Kotlin bytecode tool window
- [`KT-41884`](https://youtrack.jetbrains.com/issue/KT-41884) Support 'file' target for JvmSynthetic annotation
- [`KT-43677`](https://youtrack.jetbrains.com/issue/KT-43677) Support for Java records
- [`KT-43920`](https://youtrack.jetbrains.com/issue/KT-43920) Support loading binary Java annotations on fields
- [`KT-44278`](https://youtrack.jetbrains.com/issue/KT-44278) Generate SAM-converted lambdas and function references using 'invokedynamic' on JDK 1.8+
- [`KT-44650`](https://youtrack.jetbrains.com/issue/KT-44650) Deprecate JVM target version 1.6
- [`KT-44787`](https://youtrack.jetbrains.com/issue/KT-44787) Suspend functions in fun interfaces
- [`KT-44865`](https://youtrack.jetbrains.com/issue/KT-44865) Allow to declare protected constructors in sealed classes
- [`KT-44869`](https://youtrack.jetbrains.com/issue/KT-44869) Compiling sealed interface with version less than 1.5: error message from future could be provided


#### Performance Improvements

- [`KT-6336`](https://youtrack.jetbrains.com/issue/KT-6336) Optimize generation of local functions
- [`KT-7307`](https://youtrack.jetbrains.com/issue/KT-7307) Optimize infix call of String.plus
- [`KT-18692`](https://youtrack.jetbrains.com/issue/KT-18692) Optimize '<optimizable_range> step x' for-in loop
- [`KT-19978`](https://youtrack.jetbrains.com/issue/KT-19978) Inefficient bytecode generated for function references undergoing Java SAM conversion
- [`KT-23565`](https://youtrack.jetbrains.com/issue/KT-23565) OperationsMapGenerated.kt generates unreasonable amount of bytecode
- [`KT-23825`](https://youtrack.jetbrains.com/issue/KT-23825) Tail suspend call utilizing elvis operator does not take advantage of suspend tail call optimization
- [`KT-23826`](https://youtrack.jetbrains.com/issue/KT-23826) A suspend function on the right side of a returned || condition is not tail call optimized
- [`KT-25348`](https://youtrack.jetbrains.com/issue/KT-25348) No compile time unsigned integer conversion when using hex literal
- [`KT-26060`](https://youtrack.jetbrains.com/issue/KT-26060) Support a compiler mode to compile lambda expressions using `invokedynamic` instruction
- [`KT-26590`](https://youtrack.jetbrains.com/issue/KT-26590) Do not generate create method for suspend lambdas if its arity >= 2
- [`KT-27427`](https://youtrack.jetbrains.com/issue/KT-27427) Optimize nullable check introduced with 'as' cast
- [`KT-28246`](https://youtrack.jetbrains.com/issue/KT-28246) Redundant boxing/unboxing isn't eliminated by the compiler in case of inline classes and javaClass intrinsic
- [`KT-30605`](https://youtrack.jetbrains.com/issue/KT-30605) Constant folding doesn't evaluate inv() function
- [`KT-36845`](https://youtrack.jetbrains.com/issue/KT-36845) Generate enum-based TABLESWITCH/LOOKUPSWITCH on a value with smart cast to enum in JVM_IR
- [`KT-39585`](https://youtrack.jetbrains.com/issue/KT-39585) JVM BE generates redundant accessor calls when accessing static final field lifted from companion
- [`KT-40886`](https://youtrack.jetbrains.com/issue/KT-40886) Old JVM BE unspills ACONST_NULL from continuation
- [`KT-42621`](https://youtrack.jetbrains.com/issue/KT-42621) Kotlin binary size considerably larger for code extensively using stream API
- [`KT-44153`](https://youtrack.jetbrains.com/issue/KT-44153) NI: Low Memory and IntelliJ hangs when quotes in split() are missed
- [`KT-45410`](https://youtrack.jetbrains.com/issue/KT-45410) JVM / IR: Extreme performance regression on arithmetic operations inside a loop

#### Fixes

- [`KT-6007`](https://youtrack.jetbrains.com/issue/KT-6007) Support changed return type of inlined generic function when lambda returns anonymous object
- [`KT-6055`](https://youtrack.jetbrains.com/issue/KT-6055) Failed invoke plus assign on array element accessed via several args through local get/set convention extensions
- [`KT-6879`](https://youtrack.jetbrains.com/issue/KT-6879) CompilationException when local classes hierarchy is placed within other local or inner declaration
- [`KT-8120`](https://youtrack.jetbrains.com/issue/KT-8120) NoSuchMethodError on local class constructor call inside a local class
- [`KT-8199`](https://youtrack.jetbrains.com/issue/KT-8199) "Cannot pop operand off an empty stack" for local class using a captured variable as default value for constructor parameter
- [`KT-10835`](https://youtrack.jetbrains.com/issue/KT-10835) "AssertionError: Non-outer parameter incorrectly mapped to outer" when inlining object literal extending inner class
- [`KT-12790`](https://youtrack.jetbrains.com/issue/KT-12790) Don't generate synthetic accessors for private inline function/properties
- [`KT-13213`](https://youtrack.jetbrains.com/issue/KT-13213) IllegalArgumentException in ByteVector.putUTF8 on attempt to compile file with moderately long string literal
- [`KT-14628`](https://youtrack.jetbrains.com/issue/KT-14628) "UnsupportedOperationException: Don't know how to generate outer expression" for nested class inheriting from inner class with a companion object
- [`KT-14833`](https://youtrack.jetbrains.com/issue/KT-14833) JVM internal error: Augment assignment and increment are not supported for local delegated properties and inline properties
- [`KT-15403`](https://youtrack.jetbrains.com/issue/KT-15403) Suspend operator get wrong code generated by BE (NoSuchMethodError)
- [`KT-15404`](https://youtrack.jetbrains.com/issue/KT-15404) Suspend operator set wrong code generated
- [`KT-16084`](https://youtrack.jetbrains.com/issue/KT-16084) Proguard can't find enclosing class of let closure inside apply closure
- [`KT-16151`](https://youtrack.jetbrains.com/issue/KT-16151) Internal compiler error when using plusAssign operator with mutable map
- [`KT-16221`](https://youtrack.jetbrains.com/issue/KT-16221) Support in/!in suspend operators
- [`KT-16282`](https://youtrack.jetbrains.com/issue/KT-16282) "Cannot pop operand off an empty stack" for plusAssign with default parameters in setter operator
- [`KT-16445`](https://youtrack.jetbrains.com/issue/KT-16445) `java.lang.VerifyError: Bad type on operand stack` when delegating an interface through a private reified function inside an object
- [`KT-16520`](https://youtrack.jetbrains.com/issue/KT-16520) Invalid bytecode semantics for set call by convention with default parameters
- [`KT-16567`](https://youtrack.jetbrains.com/issue/KT-16567) Inliner creates redundant objects on source inlining
- [`KT-16752`](https://youtrack.jetbrains.com/issue/KT-16752) Delegating function interface to function reference does not work
- [`KT-17554`](https://youtrack.jetbrains.com/issue/KT-17554) Incorrect cast to Unit generated on annotated when-expression with a single-branch if inside
- [`KT-17738`](https://youtrack.jetbrains.com/issue/KT-17738) Java cannot extend class implementing kotlin.collections.Map
- [`KT-17753`](https://youtrack.jetbrains.com/issue/KT-17753) Strange behavior of if and return statements
- [`KT-18583`](https://youtrack.jetbrains.com/issue/KT-18583) "ISE: Recursive call in a lazy value" for generic sealed class with nested subclass in a `when(this)` with inferred return type
- [`KT-19861`](https://youtrack.jetbrains.com/issue/KT-19861) "IllegalStateException: Label wasn't found during iterating through instructions" for `plusAssign` with safe call
- [`KT-20306`](https://youtrack.jetbrains.com/issue/KT-20306) Make 'when' over an 'expect' enum class non-exhaustive
- [`KT-20869`](https://youtrack.jetbrains.com/issue/KT-20869) kotlin.jvm.internal.DefaultConstructorMarker should be public
- [`KT-20996`](https://youtrack.jetbrains.com/issue/KT-20996) IllegalStateException: Cannot get FQ name of local class: class <no name provided> in metadata serialization for common code
- [`KT-21014`](https://youtrack.jetbrains.com/issue/KT-21014) Incorrect bytecode generated for 'PrimitiveArray::size'
- [`KT-21092`](https://youtrack.jetbrains.com/issue/KT-21092) Reference `javaClass` for generic property: "couldn't transform method node: get()"
- [`KT-21778`](https://youtrack.jetbrains.com/issue/KT-21778) "IllegalStateException: Couldn't build context" for inline function inside an anonymous object
- [`KT-21900`](https://youtrack.jetbrains.com/issue/KT-21900) VerifyError on equals on generic primitive type
- [`KT-22098`](https://youtrack.jetbrains.com/issue/KT-22098) "UnsupportedOperationException: Don't know how to generate outer expression" on extension function call inside lambda in anonymous object super constructor call
- [`KT-22488`](https://youtrack.jetbrains.com/issue/KT-22488) Bad line numbers generated for '&&' expression
- [`KT-22972`](https://youtrack.jetbrains.com/issue/KT-22972) A compiler bug(?) in Number class descendants
- [`KT-23619`](https://youtrack.jetbrains.com/issue/KT-23619) Transform stateless singleton lambda during inline
- [`KT-23881`](https://youtrack.jetbrains.com/issue/KT-23881) Declaration of lambda in inlined apply block holds reference to superfluous references causing leak
- [`KT-24135`](https://youtrack.jetbrains.com/issue/KT-24135) Calling invoke on crossinline suspend lambda leads to no state-machine
- [`KT-24193`](https://youtrack.jetbrains.com/issue/KT-24193) NoClassDefFoundError: java/lang/Cloneable$DefaultImpls on inheritance from Cloneable through an interface
- [`KT-24305`](https://youtrack.jetbrains.com/issue/KT-24305) ClassNotFoundException when using Java reflection on local class in an inlined lambda
- [`KT-24564`](https://youtrack.jetbrains.com/issue/KT-24564) Custom operator fun set on ByteArray resolves properly but is miscompiled
- [`KT-25400`](https://youtrack.jetbrains.com/issue/KT-25400) "NoClassDefFoundError: kotlin/KotlinPackage" with Turkish system locale on macOS
- [`KT-26130`](https://youtrack.jetbrains.com/issue/KT-26130) Incorrect method signature for a generic function with inline class as a type parameter upper bound
- [`KT-26360`](https://youtrack.jetbrains.com/issue/KT-26360) "Method from super interface has a different signature" for Interface that extends both interfaces with and without @JvmDefault
- [`KT-26473`](https://youtrack.jetbrains.com/issue/KT-26473) Error on compiling inline class with calls of super methods equals(), hashCode(), toString()
- [`KT-26474`](https://youtrack.jetbrains.com/issue/KT-26474) VE “Bad type on operand stack” at runtime on calling toString() method of inline class with calls of super methods (toString(), equals(), hashCode()) inside
- [`KT-26592`](https://youtrack.jetbrains.com/issue/KT-26592) Do not generate private suspend functions as synthetic package-private
- [`KT-27449`](https://youtrack.jetbrains.com/issue/KT-27449) NoSuchMethodError for local suspend function with suspend lambda parameter with default value
- [`KT-27469`](https://youtrack.jetbrains.com/issue/KT-27469) "Cannot pop operand off an empty stack" for compound assignment (plusAssign) with a `vararg` operator get
- [`KT-27825`](https://youtrack.jetbrains.com/issue/KT-27825) Gradually prohibit non-abstract classes containing abstract members invisible from that classes (internal/package-private)
- [`KT-27830`](https://youtrack.jetbrains.com/issue/KT-27830) "Incompatible stack heights" with suspend inline function in do while loop that executes suspend lambda
- [`KT-28042`](https://youtrack.jetbrains.com/issue/KT-28042) "Cannot pop operand off an empty stack" for a bound callable reference of lambda inside inline function
- [`KT-28166`](https://youtrack.jetbrains.com/issue/KT-28166) "Argument 1: expected I, but found R" for generic method with generic parameter or receiver with inline class upper bound
- [`KT-28331`](https://youtrack.jetbrains.com/issue/KT-28331) Consider generating accessors for lateinit properties to avoid assertion on each call
- [`KT-28573`](https://youtrack.jetbrains.com/issue/KT-28573) Inliner does not update references to transformed object
- [`KT-29331`](https://youtrack.jetbrains.com/issue/KT-29331) "AnalyzerException: Argument 1: expected R, but found I" with local generic extension property called on `Int` receiver
- [`KT-29595`](https://youtrack.jetbrains.com/issue/KT-29595) NoClassDefFoundError with inline reified function with lambda argument returning anonymous object
- [`KT-29802`](https://youtrack.jetbrains.com/issue/KT-29802) Incorrect reification when the same type parameter name is used for different reified types
- [`KT-30041`](https://youtrack.jetbrains.com/issue/KT-30041) "AnalyzerException: Expected an object reference, but found ." on nested suspend function calls outer suspend function
- [`KT-30066`](https://youtrack.jetbrains.com/issue/KT-30066) Consider adding annotations to ConeKotlinType
- [`KT-30280`](https://youtrack.jetbrains.com/issue/KT-30280) Inline class class literal gets unwrapped in annotation arguments
- [`KT-30402`](https://youtrack.jetbrains.com/issue/KT-30402) Constant folding works incorrectly with unsigned arithmetics
- [`KT-30548`](https://youtrack.jetbrains.com/issue/KT-30548) "java.lang.IndexOutOfBoundsException: Cannot pop operand off an empty stack" while compiling access to a private lateinit companion field
- [`KT-30629`](https://youtrack.jetbrains.com/issue/KT-30629) `java.lang.VerifyError: Bad type on operand stack` when using a function reference to a generic property
- [`KT-30933`](https://youtrack.jetbrains.com/issue/KT-30933) Inline function produces IllegalAccessError on property reference from different package
- [`KT-31136`](https://youtrack.jetbrains.com/issue/KT-31136) "AnalyzerException: Argument 1: expected R, but found I" on x::javaClass when x is inline class object built around primitive type
- [`KT-31227`](https://youtrack.jetbrains.com/issue/KT-31227) Prohibit using array based on non-reified type parameters as reified type arguments on JVM
- [`KT-31592`](https://youtrack.jetbrains.com/issue/KT-31592) NoSuchMethodException when inlining public function accessing a protected static Java class member
- [`KT-31727`](https://youtrack.jetbrains.com/issue/KT-31727) Object expression captures all variables used in constructor
- [`KT-32023`](https://youtrack.jetbrains.com/issue/KT-32023) "AnalyzerException: Expected I, but found R" with inline suspend function used with callable reference
- [`KT-32115`](https://youtrack.jetbrains.com/issue/KT-32115) NPE during initialization of enum class with delegated property
- [`KT-32153`](https://youtrack.jetbrains.com/issue/KT-32153) "AnalyzerException: Expected an object reference, but found ." with recursive suspend local function
- [`KT-32351`](https://youtrack.jetbrains.com/issue/KT-32351) ClassNotFoundException for anonymous object implementing interface inside a lambda with data class and inline methods
- [`KT-32384`](https://youtrack.jetbrains.com/issue/KT-32384) Safe cast to generic type argument with inline class upper-bound throws NPE instead of ClassCastException
- [`KT-32579`](https://youtrack.jetbrains.com/issue/KT-32579) java.lang.VerifyError: Bad type on operand stack on calling inner class of inherited class in super class when casting to inherited class
- [`KT-32749`](https://youtrack.jetbrains.com/issue/KT-32749) "VerifyError: Call to wrong <init> method" with inline function and accessing class field from anonymous object
- [`KT-32793`](https://youtrack.jetbrains.com/issue/KT-32793) Generated code crashes by ClassCastException with local suspend function and inline class
- [`KT-32812`](https://youtrack.jetbrains.com/issue/KT-32812) "AnalyzerException: Argument 1: expected R, but found I" invoking function with default parameter inherited by inline class
- [`KT-32821`](https://youtrack.jetbrains.com/issue/KT-32821) Missing unboxing of inline class for complex hierarchy of suspend calls
- [`KT-33155`](https://youtrack.jetbrains.com/issue/KT-33155) ClassNotFoundException for qualified this in anonymous object and as a result of inline function call
- [`KT-33173`](https://youtrack.jetbrains.com/issue/KT-33173) Internal error: "AnalyzerException: Expected I, but found R" for supercall inside inline lambda from HashSet.remove implementation
- [`KT-33577`](https://youtrack.jetbrains.com/issue/KT-33577) NoSuchFieldError with nested anonymous objects accessing outer instance property
- [`KT-33836`](https://youtrack.jetbrains.com/issue/KT-33836) Wrong code generated for a local tailrec suspend function.
- [`KT-33873`](https://youtrack.jetbrains.com/issue/KT-33873) ClassCastException invoking UByte setter function via reflection
- [`KT-34018`](https://youtrack.jetbrains.com/issue/KT-34018) "Cannot pop operand off an empty stack" with inline lambda with callable reference
- [`KT-34186`](https://youtrack.jetbrains.com/issue/KT-34186) JDK11: class file contains malformed variable arity method for vararg sealed class constructor
- [`KT-34202`](https://youtrack.jetbrains.com/issue/KT-34202) IllegalAccessError on callable reference of function from multifile facade from standard library
- [`KT-34255`](https://youtrack.jetbrains.com/issue/KT-34255) @JvmStatic tailrec function: "Cannot pop operand off an empty stack"
- [`KT-34507`](https://youtrack.jetbrains.com/issue/KT-34507) Incorrect generated code for mutable collection stub methods in case of presence of functions with similar signature
- [`KT-34665`](https://youtrack.jetbrains.com/issue/KT-34665) Possible index overflow in optimized "for" loop over withIndex()
- [`KT-34754`](https://youtrack.jetbrains.com/issue/KT-34754) Flow builder: "AnalyzerException: Expected an object reference, but found ." with recursive suspend local function
- [`KT-34816`](https://youtrack.jetbrains.com/issue/KT-34816) "AnalyzerException: Expected an object reference, but found I" on "this" in inline class member extension suspend function
- [`KT-34841`](https://youtrack.jetbrains.com/issue/KT-34841) ClassNotFoundException when invoke param function inside anonymous object method
- [`KT-35008`](https://youtrack.jetbrains.com/issue/KT-35008) "AnalyzerException: Expected an object reference, but found I" in inline class companion calling private constructor
- [`KT-35166`](https://youtrack.jetbrains.com/issue/KT-35166) `NoSuchMethodError` at runtime with local property delegate on anonymous object referencing another anonymous object
- [`KT-35224`](https://youtrack.jetbrains.com/issue/KT-35224) It's possible to pass non-spread arrays after arguments with SAM-conversion
- [`KT-35301`](https://youtrack.jetbrains.com/issue/KT-35301) MethodInliner fails with "AssertionError: <init> call doesn't correspond to object transformation info" for qualified this in SAM constructor used as parameter of anonymous object inside inline lambda
- [`KT-35419`](https://youtrack.jetbrains.com/issue/KT-35419) `Failed to generate expression: KtNamedFunction` for local suspend tailrec function with receiver
- [`KT-35511`](https://youtrack.jetbrains.com/issue/KT-35511) VerifyError: "Bad type on operand stack" after reification
- [`KT-35553`](https://youtrack.jetbrains.com/issue/KT-35553) Kotlin compiler generates methods that always have line number 1 for Inline Classes
- [`KT-35725`](https://youtrack.jetbrains.com/issue/KT-35725) "AssertionError: Couldn't find a context for a super-call" for `super` member call in property initializer of companion object
- [`KT-36420`](https://youtrack.jetbrains.com/issue/KT-36420) ClassCastException with inline class Foo extending generic Comparable<Foo>
- [`KT-36713`](https://youtrack.jetbrains.com/issue/KT-36713) AnalyzerException: "Incompatible stack heights" with suspend and inline suspend functions
- [`KT-36794`](https://youtrack.jetbrains.com/issue/KT-36794) Move $assertionsDisabled field to the top-level class
- [`KT-36853`](https://youtrack.jetbrains.com/issue/KT-36853) IR: UninitializedPropertyAccessException on tailrec with object expression in default argument
- [`KT-36875`](https://youtrack.jetbrains.com/issue/KT-36875) "RuntimeException: Trying to access skipped parameter" on synthetic local variable access from inline function
- [`KT-36916`](https://youtrack.jetbrains.com/issue/KT-36916) AnalyzerException: Argument 1: expected I, but found R when using inline class with rxjava
- [`KT-36957`](https://youtrack.jetbrains.com/issue/KT-36957) Exception during codegen: cannot pop operand off an empty stack (Nothing variable in string interpolation)
- [`KT-36984`](https://youtrack.jetbrains.com/issue/KT-36984) SAM adapter classes should be generated as anonymous inner classes in JVM_IR
- [`KT-37704`](https://youtrack.jetbrains.com/issue/KT-37704) Incorrect SMAP syntax
- [`KT-37716`](https://youtrack.jetbrains.com/issue/KT-37716) "AssertionError: <init> call doesn't correspond to object transformation info" with inline reified type parameter, anonymous object and lambda in constructor call
- [`KT-37972`](https://youtrack.jetbrains.com/issue/KT-37972) IllegalAccessError on initializing property reference for a property declared in JvmMultifileClass with -Xmultifile-parts-inherit
- [`KT-38100`](https://youtrack.jetbrains.com/issue/KT-38100) Support local delegated properties (not inlined) in new JVM default modes
- [`KT-38833`](https://youtrack.jetbrains.com/issue/KT-38833) JVM: java.lang.ClassCastException when loop variable is nullable in for loop over unsigned progression
- [`KT-38849`](https://youtrack.jetbrains.com/issue/KT-38849) Read-only variable initialized in non-inline lambda using contract callsInPlace EXACTLY_ONCE is not captured correctly in nested lambdas
- [`KT-38869`](https://youtrack.jetbrains.com/issue/KT-38869) JVM BE produces invalid bytecode when inheriting from AbstractList and declaring methods that look like MutableList implementors (but they aren't)
- [`KT-38965`](https://youtrack.jetbrains.com/issue/KT-38965) "UnsupportedOperationException: Don't know how to generate outer expression: Closure" with reference to local variable in block argument of anonymous object `by` delegation
- [`KT-39289`](https://youtrack.jetbrains.com/issue/KT-39289) CCE in if-else inside annotated 'if' statement
- [`KT-39425`](https://youtrack.jetbrains.com/issue/KT-39425) AbstractMethodError: "Receiver class does not define or inherit an implementation of the resolved method" using classes with complex Java and Kotlin inheritance hierarchies.
- [`KT-39434`](https://youtrack.jetbrains.com/issue/KT-39434) IllegalAccessError with local delegated property in lambda in inlined function
- [`KT-39687`](https://youtrack.jetbrains.com/issue/KT-39687) "Couldn't find captured this" when more than 3 inline functions are nested
- [`KT-39784`](https://youtrack.jetbrains.com/issue/KT-39784) "IndexOutOfBoundsException: Cannot pop operand off an empty stack" caused by JvmOverloads annotation inside an inline class
- [`KT-40165`](https://youtrack.jetbrains.com/issue/KT-40165) ClassCastException caused by SAM conversion used on a functional interface with suspended function
- [`KT-40179`](https://youtrack.jetbrains.com/issue/KT-40179) "VerifyError: Bad type on operand stack" with parent class `get` extension function and child class `set` extension function which used inside child class `plusAssign` extension function
- [`KT-40277`](https://youtrack.jetbrains.com/issue/KT-40277) Fix generic types in special bridge methods
- [`KT-40308`](https://youtrack.jetbrains.com/issue/KT-40308) NoSuchFieldError for multiple delegated extension properties with the same name in a companion object
- [`KT-40338`](https://youtrack.jetbrains.com/issue/KT-40338) NoSuchFieldError on property without backing field that is called as function reference
- [`KT-40392`](https://youtrack.jetbrains.com/issue/KT-40392) Deprecate JvmDefault annotation and old -Xjvm-default modes
- [`KT-40396`](https://youtrack.jetbrains.com/issue/KT-40396) NI: Exceptions when ambiguous type argument and generic invoke
- [`KT-40510`](https://youtrack.jetbrains.com/issue/KT-40510) "AssertionError: DELEGATION slice must override something" for ByteBuffer delegation
- [`KT-40601`](https://youtrack.jetbrains.com/issue/KT-40601) VerifyError: "interface method reference is in an indirect superinterface" when calling @JvmDefault suspend method
- [`KT-40809`](https://youtrack.jetbrains.com/issue/KT-40809) "Couldn't find captured field" compiler error with local function with recursive call through method reference
- [`KT-41056`](https://youtrack.jetbrains.com/issue/KT-41056) Increase stub version due to new "contract" keyword
- [`KT-41105`](https://youtrack.jetbrains.com/issue/KT-41105) IllegalStateException: 'Couldn't find declaration file <class name>' with inline delegate declared in another file
- [`KT-41165`](https://youtrack.jetbrains.com/issue/KT-41165) "IllegalStateException: Concrete fake override public final fun" when an enum class inherits an interface with a variable 'name' or 'ordinal'
- [`KT-41222`](https://youtrack.jetbrains.com/issue/KT-41222) "IllegalStateException: Concrete fake override public final fun" when a class property is inherited as merged 'var' from 'val' and 'var' from parent abstract class and interface properties
- [`KT-41255`](https://youtrack.jetbrains.com/issue/KT-41255) JDK 11: "VerifyError: Bad type on operand stack" with long function body with annotated `when` expression
- [`KT-41427`](https://youtrack.jetbrains.com/issue/KT-41427) NoSuchMethodError caused by implementation by delegation to function reference
- [`KT-41508`](https://youtrack.jetbrains.com/issue/KT-41508) ClassNotFoundException caused by object with overridden function inside a lambda with safe cast receiver
- [`KT-41750`](https://youtrack.jetbrains.com/issue/KT-41750) Inline classes: ClassCastExceptionError when calling .withIndex() on Iterator over Array
- [`KT-41758`](https://youtrack.jetbrains.com/issue/KT-41758) Deprecate kotlin.Metadata.bytecodeVersion and avoid using it in the compiler
- [`KT-41770`](https://youtrack.jetbrains.com/issue/KT-41770) AssertionError: "Asm parameter types should be the same length as Kotlin parameter types" cause by fun interface
- [`KT-41874`](https://youtrack.jetbrains.com/issue/KT-41874) "IllegalStateException: Couldn't obtain compiled function body" on extension delegated property with inline operator getValue in a different file
- [`KT-41917`](https://youtrack.jetbrains.com/issue/KT-41917) [FIR] Incorrect calculating property type for override from intersection scope
- [`KT-42012`](https://youtrack.jetbrains.com/issue/KT-42012) IllegalAccess to protected field instead of getter
- [`KT-42017`](https://youtrack.jetbrains.com/issue/KT-42017) "AssertionError: Unsigned type expected: UInt?" during codegen when a variable of nullable unsigned type is checking for presence in the range
- [`KT-42032`](https://youtrack.jetbrains.com/issue/KT-42032) "AnalyzerException: Expected I, but found R" while using Flow.reduce() with suspend function reference
- [`KT-42034`](https://youtrack.jetbrains.com/issue/KT-42034) ArrayIndexOutOfBoundsException in PopBackwardPropagationTransformer on external override of function in inline class
- [`KT-42064`](https://youtrack.jetbrains.com/issue/KT-42064) "Parameter specified as non-null is null" with default value of the parameter in operator fun
- [`KT-42069`](https://youtrack.jetbrains.com/issue/KT-42069) JVM IR: -Xreport-output-files doesn't report any source files for META-INF/*.kotlin_module files
- [`KT-42083`](https://youtrack.jetbrains.com/issue/KT-42083) AbstractMethodError when 'remove' with irrelevant generic parameter but matching JVM signature is present in Kotlin collection class
- [`KT-42092`](https://youtrack.jetbrains.com/issue/KT-42092) JVM / IR: "AnalyzerException: Argument 1: expected R, but found J" when trying to add to ArrayList the result of a function applied to int
- [`KT-42175`](https://youtrack.jetbrains.com/issue/KT-42175) Psi2ir: "AssertionError: Undefined parameter referenced: <this>" on augmented assignment on this in a BuilderInference lambda
- [`KT-42179`](https://youtrack.jetbrains.com/issue/KT-42179) Platform declaration clash when extending abstract Java class implementing 'java.util.Collection' by abstract Kotlin class implementing Kotlin Set or List
- [`KT-42321`](https://youtrack.jetbrains.com/issue/KT-42321) JVM IR: do not cast integer value based on the type of a literal receiver of an operator call
- [`KT-42337`](https://youtrack.jetbrains.com/issue/KT-42337) NoSuchMethodError in JVM backend with inheritance of private functions in the interface
- [`KT-42404`](https://youtrack.jetbrains.com/issue/KT-42404) "Supertypes of the following classes cannot be resolved" in Rider project
- [`KT-42472`](https://youtrack.jetbrains.com/issue/KT-42472) No TYPE_INFERENCE_UPPER_BOUND_VIOLATED for Delegated Properties do not check types (in Kotlin 1.4.10)
- [`KT-42487`](https://youtrack.jetbrains.com/issue/KT-42487) "IndexOutOfBoundsException: Cannot pop operand off an empty stack" caused by USELESS_IS_CHECK of Double type
- [`KT-42533`](https://youtrack.jetbrains.com/issue/KT-42533) `(N until MIN_VALUE).reversed()` should be an empty progression in for loops
- [`KT-42588`](https://youtrack.jetbrains.com/issue/KT-42588) "IllegalStateException: Concrete fake override public open fun" caused by `val` override with `var` with delegation.
- [`KT-42634`](https://youtrack.jetbrains.com/issue/KT-42634) Different bridges and abstract stubs behavior in abstract class implementing Map<K, String> in JVM and JVM_IR
- [`KT-42635`](https://youtrack.jetbrains.com/issue/KT-42635) ClassCastException with inline class in for loop
- [`KT-42662`](https://youtrack.jetbrains.com/issue/KT-42662) AbstractMethodError when using partially specialized generic Map class
- [`KT-42694`](https://youtrack.jetbrains.com/issue/KT-42694) @get:Synchronized causes the JVM getter method not to be generated
- [`KT-42753`](https://youtrack.jetbrains.com/issue/KT-42753) "VerifyError: Bad invokespecial instruction: interface method reference is in an indirect superinterface" with `jvm-default=all`
- [`KT-42879`](https://youtrack.jetbrains.com/issue/KT-42879) JVM: Declaration clash in fun interface implementation returning an inline class
- [`KT-42900`](https://youtrack.jetbrains.com/issue/KT-42900) "VerifyError: Bad return type" incorrect bytecode when a property and an extension property in inline class have the same names
- [`KT-42946`](https://youtrack.jetbrains.com/issue/KT-42946) FIR2IR:  Fix super-calls to Java overrides of special built-in
- [`KT-42971`](https://youtrack.jetbrains.com/issue/KT-42971) JVM: "AssertionError: Unsigned type expected: T" with UInt loop range
- [`KT-42990`](https://youtrack.jetbrains.com/issue/KT-42990) "AssertionError: Next value after NEW should be one generated by DUP" caused by extension properties with accessors annotataed as @JvmStatic
- [`KT-43034`](https://youtrack.jetbrains.com/issue/KT-43034) AssertionError: Compiler fails with complicated tailrec + inline case
- [`KT-43048`](https://youtrack.jetbrains.com/issue/KT-43048) JVM_IR: Implement coroutines state clearing
- [`KT-43050`](https://youtrack.jetbrains.com/issue/KT-43050) JVM IR: incorrect mangling for method with type parameter with inline class bound in the signature
- [`KT-43059`](https://youtrack.jetbrains.com/issue/KT-43059) Different bridges  behavior in class implementing Map<String, String> in JVM and JVM_IR
- [`KT-43063`](https://youtrack.jetbrains.com/issue/KT-43063) Redundant DefaultImpls delegate is generated in old JVM backend on explicit "duplicate" inheritance from interface
- [`KT-43069`](https://youtrack.jetbrains.com/issue/KT-43069) JVM: incorrect generic signature for method with implicit return type Nothing overriding a method from Collection
- [`KT-43099`](https://youtrack.jetbrains.com/issue/KT-43099) Tailrec call in not tail-call position leads to internal compiler error
- [`KT-43106`](https://youtrack.jetbrains.com/issue/KT-43106) JVM: custom `remove` in Iterator subclass results in a synthetic bridge
- [`KT-43120`](https://youtrack.jetbrains.com/issue/KT-43120) JVM: "Expected an object reference, but found ." caused by function which is passed as reference to suspend parameter
- [`KT-43167`](https://youtrack.jetbrains.com/issue/KT-43167) JVM IR, serialization: "No mapping for symbol: VALUE_PARAMETER INSTANCE_RECEIVER" with data class containing property defined in body
- [`KT-43255`](https://youtrack.jetbrains.com/issue/KT-43255) Verify error when inheriting from an abstract class implementing Collection with stub-like method in superclass
- [`KT-43303`](https://youtrack.jetbrains.com/issue/KT-43303) NI: False negative TYPE_INFERENCE_UPPER_BOUND_VIOLATED when inferred type argument is not a subtype of type parameter upper bound
- [`KT-43333`](https://youtrack.jetbrains.com/issue/KT-43333) AbstractMethodError when calling 'toArray' from Java on a Kotlin Collection with custom internal 'toArray'
- [`KT-43334`](https://youtrack.jetbrains.com/issue/KT-43334) AbstractMethodError when calling 'remove' from Java on a Kotlin Collection with custom internal 'remove'
- [`KT-43342`](https://youtrack.jetbrains.com/issue/KT-43342) [FIR2IR] No getter or backing field found for delegated member call
- [`KT-43347`](https://youtrack.jetbrains.com/issue/KT-43347) [FIR] Synthetic setter with unmatched parameter type isn't found
- [`KT-43401`](https://youtrack.jetbrains.com/issue/KT-43401) JVM_IR. Additional `synchronized` flag on JvmOverloads-generated adapter for Synchronized function
- [`KT-43405`](https://youtrack.jetbrains.com/issue/KT-43405) Turkish locale, Linux Mint: "NoSuchMethodError: 'int[] kotlin.jvm.internal.Intrinsics$Kotlin.intArrayOf(int[])'" with `intArrayOf` function call
- [`KT-43460`](https://youtrack.jetbrains.com/issue/KT-43460) JVM: redundant private setter is generated in case of multifile facade
- [`KT-43473`](https://youtrack.jetbrains.com/issue/KT-43473) "VerifyError: Bad type on operand stack" caused by operator `get` with optional argument in superclass when called via square brackets on subclass
- [`KT-43518`](https://youtrack.jetbrains.com/issue/KT-43518) JVM_IR. Additional `strictfp` flag on JvmOverloads-generated adapter for Strictfp function
- [`KT-43569`](https://youtrack.jetbrains.com/issue/KT-43569) FIR: inapplicable candidate(s): kotlin/collections/set
- [`KT-43616`](https://youtrack.jetbrains.com/issue/KT-43616) [FIR] Nullable type parameter-based type after merge in if
- [`KT-43669`](https://youtrack.jetbrains.com/issue/KT-43669) FIR: No real overrides for FUN IR_EXTERNAL_DECLARATION_STUB
- [`KT-43682`](https://youtrack.jetbrains.com/issue/KT-43682) Inline extension method of a multifile library inline class not found
- [`KT-43687`](https://youtrack.jetbrains.com/issue/KT-43687) FIR: UnusedChecker does not take annotation arguments into account
- [`KT-43688`](https://youtrack.jetbrains.com/issue/KT-43688) FIR: unused checker doesn't handle invokes properly
- [`KT-43749`](https://youtrack.jetbrains.com/issue/KT-43749) "UnsupportedOperationException: Don't know how to generate outer expression: Closure" caused by Flow and collect method with function reference as a parameter
- [`KT-43812`](https://youtrack.jetbrains.com/issue/KT-43812) JVM IR: SAM wrapper class with generic supertype mentions missing type parameter in the signature
- [`KT-43832`](https://youtrack.jetbrains.com/issue/KT-43832) JVM IR: missing bridges for inheritance of class from interface in a complex generic diamond hierarchy
- [`KT-43851`](https://youtrack.jetbrains.com/issue/KT-43851) JVM IR: function call returning object instance is removed during constant propagation
- [`KT-43864`](https://youtrack.jetbrains.com/issue/KT-43864) JVM: "Assertion error after mandatory stack transformations: incorrect bytecode" with lateinit property of type T, which has a primitive type upperbound
- [`KT-43887`](https://youtrack.jetbrains.com/issue/KT-43887) Problem with FunctionReferenceLowering$FunctionReferenceBuilder in kotlin native
- [`KT-43912`](https://youtrack.jetbrains.com/issue/KT-43912) JVM internal error: Augment assignment and increment are not supported for local delegated properties and inline properties
- [`KT-43915`](https://youtrack.jetbrains.com/issue/KT-43915) Back-end (JVM) Internal error: wrong bytecode generated for default method
- [`KT-43938`](https://youtrack.jetbrains.com/issue/KT-43938) NSME when calling 'kotlin.Number' methods on instance of Java class extending Kolin abstract class extending 'kotlin.Number'
- [`KT-43942`](https://youtrack.jetbrains.com/issue/KT-43942) org.jetbrains.kotlin.codegen.CompilationException: Back-end (JVM) Internal error: Failed to generate function
- [`KT-43949`](https://youtrack.jetbrains.com/issue/KT-43949) FIR: unresolved callable reference as lambda return
- [`KT-43983`](https://youtrack.jetbrains.com/issue/KT-43983) IllegalStateException: "Couldn't obtain compiled function body for public final suspend inline fun" after moving inline extension function to library
- [`KT-43984`](https://youtrack.jetbrains.com/issue/KT-43984) FIR: recursion in overridden symbols
- [`KT-43984`](https://youtrack.jetbrains.com/issue/KT-43984) FIR: recursion in overridden symbols
- [`KT-44010`](https://youtrack.jetbrains.com/issue/KT-44010) FIR: Inapplicable constructor due to an unresolved reference
- [`KT-44030`](https://youtrack.jetbrains.com/issue/KT-44030) FIR2IR: uncached type parameters in delegated property
- [`KT-44032`](https://youtrack.jetbrains.com/issue/KT-44032) FIR2IR: uncached type parameters in Java field
- [`KT-44050`](https://youtrack.jetbrains.com/issue/KT-44050) FIR: anonymous object as IR parent
- [`KT-44054`](https://youtrack.jetbrains.com/issue/KT-44054) FIR2IR: incorrect IR origin for substituted override function
- [`KT-44058`](https://youtrack.jetbrains.com/issue/KT-44058) CompilationException: open suspend fun with @JvmStatic in open class companion
- [`KT-44069`](https://youtrack.jetbrains.com/issue/KT-44069) please remove deprecated usages
- [`KT-44066`](https://youtrack.jetbrains.com/issue/KT-44066) FIR Java: override ambiguity with vararg value type
- [`KT-44114`](https://youtrack.jetbrains.com/issue/KT-44114) CompilationException when inlining a extension suspend function declared in interface companion with 'this' reference to extension receiver
- [`KT-44131`](https://youtrack.jetbrains.com/issue/KT-44131) "UnsupportedOperationException: Don't know how to generate outer expression: Closure" when using suspend lambda and a function reference
- [`KT-44140`](https://youtrack.jetbrains.com/issue/KT-44140) JVM IR: compilation of kotlin.Result crashes with IOOBE while generating toString-impl
- [`KT-44141`](https://youtrack.jetbrains.com/issue/KT-44141) JVM IR: "ISE: There should be underlying type for inline class type" on usage of type parameter with Result upper bound inside a lambda
- [`KT-44192`](https://youtrack.jetbrains.com/issue/KT-44192) Allow a greater number of constants in an enum class
- [`KT-44202`](https://youtrack.jetbrains.com/issue/KT-44202) "ClassCastException" when getting delegated property with inline class and Any/Any? type
- [`KT-44210`](https://youtrack.jetbrains.com/issue/KT-44210) KJS / IR: "AssertionError: Undefined parameter referenced: <this> defined" caused by plus assign operators in build blocks
- [`KT-44233`](https://youtrack.jetbrains.com/issue/KT-44233) [IR] Collection Stub generation not correctly considering java.util Collection iterators
- [`KT-44234`](https://youtrack.jetbrains.com/issue/KT-44234) Private companion property with explicit setter generates invalid bytecode
- [`KT-44269`](https://youtrack.jetbrains.com/issue/KT-44269) "[TAILREC_ON_VIRTUAL_MEMBER_ERROR] Tailrec is not allowed on open members" with Spring annotation and private tailrec function
- [`KT-44284`](https://youtrack.jetbrains.com/issue/KT-44284) Make Kotlin binaries publicly unavailable (set KotlinCompilerVersion.IS_PRE_RELEASE = true)
- [`KT-44316`](https://youtrack.jetbrains.com/issue/KT-44316) ReenteringLazyValueComputationException when analyzing complex lazy delegate
- [`KT-44347`](https://youtrack.jetbrains.com/issue/KT-44347) Back-end (JVM) Internal error: Couldn't transform method node for suspend function with wrong local for Continuation
- [`KT-44368`](https://youtrack.jetbrains.com/issue/KT-44368) "IllegalStateException: Error type encountered" when inlining 'invoke' operator without enough information on type variable
- [`KT-44412`](https://youtrack.jetbrains.com/issue/KT-44412) JVM IR backend fails to compile break in condition of do while
- [`KT-44420`](https://youtrack.jetbrains.com/issue/KT-44420) False NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATION with 1.4.30-RC
- [`KT-44429`](https://youtrack.jetbrains.com/issue/KT-44429) JVM IR: unnecessary integer unboxing leads to NPE when using mockito-kotlin
- [`KT-44439`](https://youtrack.jetbrains.com/issue/KT-44439) Type inference of generic types failing java interop
- [`KT-44440`](https://youtrack.jetbrains.com/issue/KT-44440) Too many Nothings in inferred type
- [`KT-44471`](https://youtrack.jetbrains.com/issue/KT-44471) Fix failing script tests after switching to 1.5
- [`KT-44474`](https://youtrack.jetbrains.com/issue/KT-44474) Compiler expects sealed type inheritors from platform specific source-sets in when expression in common source-set
- [`KT-44483`](https://youtrack.jetbrains.com/issue/KT-44483) JVM IR: CCE on calling generic vararg function reference with Array expected type
- [`KT-44527`](https://youtrack.jetbrains.com/issue/KT-44527) Suspend function with kotlin.Result: ClassCastException class kotlin.Result cannot be cast to class ...
- [`KT-44529`](https://youtrack.jetbrains.com/issue/KT-44529) Inline class calls wrong iterator method in for loop
- [`KT-44533`](https://youtrack.jetbrains.com/issue/KT-44533) JVM IR: ClassFormatError on synthetic $suspendImpl method generated in interface for a @JvmDefault function
- [`KT-44540`](https://youtrack.jetbrains.com/issue/KT-44540) Regression in 1.4.30 in intellij-community: type mismatch for generic function call with generic Java class
- [`KT-44546`](https://youtrack.jetbrains.com/issue/KT-44546) NI: changed variable fixation order (that can lead to changed resolution)
- [`KT-44550`](https://youtrack.jetbrains.com/issue/KT-44550) KotlinBinaryClassCache leaks Kotlin plugin classloader on plugin unload
- [`KT-44563`](https://youtrack.jetbrains.com/issue/KT-44563) Type Inference loosing type annotations in lambda type expectation for function calls with block parameters
- [`KT-44583`](https://youtrack.jetbrains.com/issue/KT-44583) "Supertypes of the following classes cannot be resolved" error message gives no context
- [`KT-44627`](https://youtrack.jetbrains.com/issue/KT-44627) JVM IR: ACCIDENTAL_OVERRIDE when overriding a generic field where the type parameter has a primitive bound
- [`KT-44631`](https://youtrack.jetbrains.com/issue/KT-44631) "IndexOutOfBoundsException: Cannot pop operand off an empty stack" caused by a default param in inner class constructor which uses method or field from receiver
- [`KT-44647`](https://youtrack.jetbrains.com/issue/KT-44647) "IllegalAccessError: class TestKt tried to access private method" with String Builder `get` and `inc` operator
- [`KT-44651`](https://youtrack.jetbrains.com/issue/KT-44651) JVM / IR: "IllegalStateException: Illegal type substitutor" with if-else inside class constructor argument inside another if-else
- [`KT-44660`](https://youtrack.jetbrains.com/issue/KT-44660) Internal inline functions in companion object with inline class return type fails compilation
- [`KT-44671`](https://youtrack.jetbrains.com/issue/KT-44671) JVM_IR: ClassCastException: Result$Failure cannot be cast to Result with multithreaded coroutines
- [`KT-44703`](https://youtrack.jetbrains.com/issue/KT-44703) JVM / IR: "IllegalStateException: Unhandled special name in mangledNameFor" caused by a reference to inline class inside interface's companion with lazy initialization
- [`KT-44712`](https://youtrack.jetbrains.com/issue/KT-44712) JVM / IR: Behavior change after enabling with Mockito
- [`KT-44714`](https://youtrack.jetbrains.com/issue/KT-44714) Debugger / Coroutines: Local variables are trimmed out too aggressively
- [`KT-44722`](https://youtrack.jetbrains.com/issue/KT-44722) JVM IR: ClassCastException with inline class, let and bound function reference
- [`KT-44726`](https://youtrack.jetbrains.com/issue/KT-44726) JVM IR: Incorrect KType nullability for platform type reified as non-null
- [`KT-44781`](https://youtrack.jetbrains.com/issue/KT-44781) JVM IR: java.lang.NoSuchFieldError: $noName_0 when calling a crossinline lambda within a suspending lambda
- [`KT-44798`](https://youtrack.jetbrains.com/issue/KT-44798) JVM IR: Inherited platform declarations clash for class implementing both List and Set
- [`KT-44801`](https://youtrack.jetbrains.com/issue/KT-44801) 1.4.30 JVM IR: Unbound symbols not allowed with anonymous object
- [`KT-44803`](https://youtrack.jetbrains.com/issue/KT-44803) FIR bootstrap: incorrect nullability is set for type alias-based type
- [`KT-44827`](https://youtrack.jetbrains.com/issue/KT-44827) Non-existing outer class is written in anonymous class for SAM wrapper in inline lambda with capture
- [`KT-44837`](https://youtrack.jetbrains.com/issue/KT-44837) JVM / IR: ClassCastException with Result object when it is used by a generic method in a suspend call
- [`KT-44875`](https://youtrack.jetbrains.com/issue/KT-44875) JVM_IR. `hashCode` call is generated  on interface target in fun interface equality
- [`KT-44878`](https://youtrack.jetbrains.com/issue/KT-44878) JVM_IR: "IllegalStateException: Unexpected types" when checking non-nullable variable is `in` range between nullable ones with smart-cast
- [`KT-44926`](https://youtrack.jetbrains.com/issue/KT-44926) MPP: Actual typealias to compiled inline class incompatible with expect inline class
- [`KT-44947`](https://youtrack.jetbrains.com/issue/KT-44947) Sealed interfaces: Sealed fun interface leads to "NoWhenBranchMatchedException"
- [`KT-44993`](https://youtrack.jetbrains.com/issue/KT-44993) JVM IR: VerifyError on getfield with Kotlin generic field and elvis operator
- [`KT-45008`](https://youtrack.jetbrains.com/issue/KT-45008) JVM IR: hashCode is generated as invokeinterface if smart cast to interface is present
- [`KT-45011`](https://youtrack.jetbrains.com/issue/KT-45011) JVM / IR: "AssertionError: Unbound symbols not allowed"
- [`KT-45022`](https://youtrack.jetbrains.com/issue/KT-45022) IR: "AssertionError: Undefined variable referenced" from psi2ir caused by plusAssign operator of object
- [`KT-45064`](https://youtrack.jetbrains.com/issue/KT-45064) JVM IR: "java.lang.AssertionError: SyntheticAccessorLowering should not attempt to modify other files!" with member reference to property in another file with private setter
- [`KT-45067`](https://youtrack.jetbrains.com/issue/KT-45067) "IllegalArgumentException: Wildcard mast have a bound for annotation of WILDCARD_BOUND position" with BEAM SDK 2.27
- [`KT-45069`](https://youtrack.jetbrains.com/issue/KT-45069) JVM / IR: New SAM conversions mode fails when converting from Unit to Any
- [`KT-45131`](https://youtrack.jetbrains.com/issue/KT-45131) JVM / IR: "RuntimeException: Lambda, SAM or anonymous object should have only one constructor" caused by inline class that type cast to reified type parameter inside lambda in inline function
- [`KT-45139`](https://youtrack.jetbrains.com/issue/KT-45139) Inline class: AssertionError: Expected top level inline class
- [`KT-45166`](https://youtrack.jetbrains.com/issue/KT-45166) JVM / IR: "AbstractMethodError: Receiver class does not define or inherit an implementation of the resolved method of interface" caused by interface with suspend function
- [`KT-45187`](https://youtrack.jetbrains.com/issue/KT-45187) JVM /  IR: ClassCastException caused by substituting generic type of vararg parameter with java.lang.Void
- [`KT-45195`](https://youtrack.jetbrains.com/issue/KT-45195) JVM IR: annotation methods are generated as default interface methods if `allopen` is used
- [`KT-45243`](https://youtrack.jetbrains.com/issue/KT-45243) "IllegalStateException: Lambdas shouldn't be visited by ESExpressionVisitor" caused by lambda inside `kotlin.test.assertNotNull`
- [`KT-45259`](https://youtrack.jetbrains.com/issue/KT-45259) JVM: ClassCastException caused by Result as lambda parameter type
- [`KT-45292`](https://youtrack.jetbrains.com/issue/KT-45292) AssertionError with recursive inline extension property
- [`KT-45300`](https://youtrack.jetbrains.com/issue/KT-45300) Deprecate super calls in public-api inline functions
- [`KT-45409`](https://youtrack.jetbrains.com/issue/KT-45409) Rename jspecify annotations’ package and default not null annotation
- [`KT-45446`](https://youtrack.jetbrains.com/issue/KT-45446) JVM / IR: NullPointerException caused by unreachable code and comparison
- [`KT-45721`](https://youtrack.jetbrains.com/issue/KT-45721) JVM / IR: "Unbound symbols not allowed" caused by class reference in sequence lambda
- [`KT-45853`](https://youtrack.jetbrains.com/issue/KT-45853) JVM / IR: "Accidental override" caused by inheriting Throwable.getCause from Java interface
- [`KT-45861`](https://youtrack.jetbrains.com/issue/KT-45861) Turning warnings into errors for calls with type parameters annotated by @OnlyInputTypes
- [`KT-45865`](https://youtrack.jetbrains.com/issue/KT-45865) JVM IR: "VerifyError: Bad type on operand stack" with `enumValueOf` on a value from a list of strings
- [`KT-45868`](https://youtrack.jetbrains.com/issue/KT-45868) JVM IR: ClassCastException with SAM function in init block when SAM is generated via invokedynamic
- [`KT-45920`](https://youtrack.jetbrains.com/issue/KT-45920) JVM IR: "Accidental override" on redefining `get()` in custom Map class
- [`KT-45934`](https://youtrack.jetbrains.com/issue/KT-45934) JVM IR: "java.lang.IllegalStateException: Function has no body" for class implementing interface by delegation
- [`KT-45945`](https://youtrack.jetbrains.com/issue/KT-45945) JVM / IR: "AssertionError: Unexpected variance in super type argument" with contravariance and intersection types
- [`KT-45963`](https://youtrack.jetbrains.com/issue/KT-45963) JVM / IR: "AbstractMethodError: Receiver class does not define or inherit an implementation of the resolved method" in Dokka tests
- [`KT-45967`](https://youtrack.jetbrains.com/issue/KT-45967) JVM IR: "IllegalAccessError" with invokedynamic to Java SAM over callable reference to private function
- [`KT-45982`](https://youtrack.jetbrains.com/issue/KT-45982) Wrong subtyping result on captured types with postponed type variables
- [`KT-46007`](https://youtrack.jetbrains.com/issue/KT-46007) JVM / IR: "ClassCastException: kotlin.Unit cannot be cast to java.lang.String" caused by default suspend function in interface
- [`KT-46060`](https://youtrack.jetbrains.com/issue/KT-46060) JVM IR: NullPointerException from RangeContainsLowering when `contains` is a @JvmStatic function in object
- [`KT-46069`](https://youtrack.jetbrains.com/issue/KT-46069) JVM IR: unbound type parameter on generic bound adapted function reference
- [`KT-46092`](https://youtrack.jetbrains.com/issue/KT-46092) JVM IR: AssertionError "Array type expected: @[FlexibleNullability] kotlin.CharArray?" on super call to Java constructor with primitive vararg
- [`KT-46104`](https://youtrack.jetbrains.com/issue/KT-46104) The message on inline -> value class migration should not say that inline classes are deprecated
- [`KT-46131`](https://youtrack.jetbrains.com/issue/KT-46131) Kotlin 1.5.0-RC errors when reading class file
- [`KT-46160`](https://youtrack.jetbrains.com/issue/KT-46160) JVM IR: IllegalAccessException at runtime for member reference to JvmMultifileClass member from stdlib
- [`KT-46186`](https://youtrack.jetbrains.com/issue/KT-46186) Type inference regression in Kotlin 1.5 with constrained generic return types

### Docs & Examples

- [`KT-45884`](https://youtrack.jetbrains.com/issue/KT-45884) Incorrect description for JVM `toUpperCase` method

### IDE

- [`KT-33233`](https://youtrack.jetbrains.com/issue/KT-33233) Use dependency of library to build built-ins in IDE, instead of loading them from the current classloader
- [`KT-34023`](https://youtrack.jetbrains.com/issue/KT-34023) kotlin.KotlinNullPointerException at org.jetbrains.kotlin.backend.common.FunctionsFromAnyGenerator.getPrimaryConstructorProperties(FunctionsFromAnyGenerator.kt:66)
- [`KT-35947`](https://youtrack.jetbrains.com/issue/KT-35947) KFunctionN.call is unresolved in IDE in Kotlin/JVM project
- [`KT-37702`](https://youtrack.jetbrains.com/issue/KT-37702) Code analysis speed: on-the-fly analysis diagnostics reporting
- [`KT-41048`](https://youtrack.jetbrains.com/issue/KT-41048) [FIR-IDE] Properly implement methods in KtFirPackageScope
- [`KT-41671`](https://youtrack.jetbrains.com/issue/KT-41671) Missing nullability information in properties using type inference from get()
- [`KT-43824`](https://youtrack.jetbrains.com/issue/KT-43824) KtLightClassForSourceDeclaration#isInheritor works in a different way than java implementation
- [`KT-44128`](https://youtrack.jetbrains.com/issue/KT-44128) IDE: Kotlin JVM record has incorrect property accessors as seen from Java
- [`KT-44487`](https://youtrack.jetbrains.com/issue/KT-44487) MPP, IDE: No error in IDE when sealed class inheritor from common source-set is not used in exhaustive when expression in platform source-set
- [`KT-45254`](https://youtrack.jetbrains.com/issue/KT-45254) Highlighting for files with certain errors appears only on second opening
- [`KT-46097`](https://youtrack.jetbrains.com/issue/KT-46097) Light classes: Incomplete nullability information for a getter method of a kotlin property defined in private constructor

### IDE. Decompiler, Indexing, Stubs

- [`KT-43699`](https://youtrack.jetbrains.com/issue/KT-43699) IDE: Unresolved extension method from Java code for simple class with typealias and generics (IllegalStateException: Unknown type parameter)
- [`KT-44756`](https://youtrack.jetbrains.com/issue/KT-44756) Infinite "UpToDateStubIndexMismatch: PSI and index do not match." with IDEA 2021.1 EAP upon attempt to open "org.gradle.configurationcache" even they seem to be the same

### IDE. Gradle Integration

- [`KT-37127`](https://youtrack.jetbrains.com/issue/KT-37127) Implement precise importing of platforms of root source sets (commonMain/commonTest) when hierarchical multiplatform support is enabled
- [`KT-42048`](https://youtrack.jetbrains.com/issue/KT-42048) KJS / Gradle integration: Could not determine the dependencies of task ':webApp:testPackageJson' in Android Studio 4.2 Canary 11

### IDE. Gradle. Script

- [`KT-46215`](https://youtrack.jetbrains.com/issue/KT-46215) Dead lock on closing project during the import in IJ211 through ScriptDefinitionsManager

### IDE. Inspections and Intentions

- [`KT-23824`](https://youtrack.jetbrains.com/issue/KT-23824) Return lifted out of if condition causes suspend tail call optimization to no longer apply
- [`KT-38155`](https://youtrack.jetbrains.com/issue/KT-38155) Lift assignment out of 'if' produces type mismatch without manually adding a semicolon
- [`KT-44821`](https://youtrack.jetbrains.com/issue/KT-44821) IDE: False positive NO_ELSE_IN_WHEN caused by sealed class and when in another module
- [`KT-46088`](https://youtrack.jetbrains.com/issue/KT-46088) [IDEA] Incorrect behavior of replace inline class with value class intention

### IDE. Misc

- [`KT-44675`](https://youtrack.jetbrains.com/issue/KT-44675) Incorrect reference to resource into 202 plugin

### IDE. Refactorings

- [`KT-44079`](https://youtrack.jetbrains.com/issue/KT-44079) Sealed Interfaces: Move refactoring should warn about violation of hierarchy restrictions
- [`KT-44839`](https://youtrack.jetbrains.com/issue/KT-44839) Sealed interfaces: move refactoring warnings works with "more freedom for sealed classes" rules for language level < 1.5

### IDE. Script

- [`KT-43288`](https://youtrack.jetbrains.com/issue/KT-43288) Allow push notifications about script configuration /dependencies changes via the `ScriptDefinitionsProvider` EP

### JavaScript

- [`KT-39272`](https://youtrack.jetbrains.com/issue/KT-39272) KJS / IR: Can't use javascript keywords as JsName
- [`KT-41650`](https://youtrack.jetbrains.com/issue/KT-41650) JS IR BE: `default` should be a reserved identifier
- [`KT-42176`](https://youtrack.jetbrains.com/issue/KT-42176) KJS / IR: Interface default method in sub-interface not resolved correctly from extension on super-interface
- [`KT-44103`](https://youtrack.jetbrains.com/issue/KT-44103) [JSIR] TypeError when bumping from 1.4.20 to 1.4.30-M1
- [`KT-44180`](https://youtrack.jetbrains.com/issue/KT-44180) KJS / IR: NPE in ConstTransformer of compileDevelopmentExecutableKotlinJs/compileProductionExecutableKotlinJs tasks
- [`KT-44415`](https://youtrack.jetbrains.com/issue/KT-44415) Kotlin/JS with IR and kotlin-react: "too much recursion" error in runtime in browser
- [`KT-44433`](https://youtrack.jetbrains.com/issue/KT-44433) KJS IR: support function interfaces with suspend member
- [`KT-44469`](https://youtrack.jetbrains.com/issue/KT-44469) KJS / IR: Incorrect export functions with bridges
- [`KT-44718`](https://youtrack.jetbrains.com/issue/KT-44718) MPP/ KJS: "IllegalStateException: Unsupported operation" with serialization plugin and incremental compilation
- [`KT-44796`](https://youtrack.jetbrains.com/issue/KT-44796) KJS / IR: default parameter of function with @JsName leads to "RangeError: Maximum call stack size exceeded"
- [`KT-45059`](https://youtrack.jetbrains.com/issue/KT-45059) KJS / IR: Add possibility for runtime diagnostics of DCE result

### Libraries

- [`KT-12109`](https://youtrack.jetbrains.com/issue/KT-12109) Add stdlib method that combines mapNotNull() and first/firstOrNull()
- [`KT-25571`](https://youtrack.jetbrains.com/issue/KT-25571) Make random implementations serializable
- [`KT-26234`](https://youtrack.jetbrains.com/issue/KT-26234) Floored division and remainder function for numeric types
- [`KT-32996`](https://youtrack.jetbrains.com/issue/KT-32996) kotlin.test: add assertContentEquals for comparing content of arrays, iterables, sequences
- [`KT-39177`](https://youtrack.jetbrains.com/issue/KT-39177) Make CharCategory available in common multiplatform code
- [`KT-40225`](https://youtrack.jetbrains.com/issue/KT-40225) Support adding kotlin-test as a single dependency, as it should be with a multiplatform library
- [`KT-42071`](https://youtrack.jetbrains.com/issue/KT-42071) Strict version of String.toBoolean()
- [`KT-42720`](https://youtrack.jetbrains.com/issue/KT-42720) Kotlin ArrayDeque on JVM: provide optimized toArray method
- [`KT-42840`](https://youtrack.jetbrains.com/issue/KT-42840) Commonize and generalize String.contentEquals that is currently JVM-only
- [`KT-43772`](https://youtrack.jetbrains.com/issue/KT-43772) Kotlin/Native unfinished workers detected.
- [`KT-44168`](https://youtrack.jetbrains.com/issue/KT-44168) Prevent storing NaN and negative zero in kotlin.time.Duration
- [`KT-44369`](https://youtrack.jetbrains.com/issue/KT-44369) Commonize Char.titlecaseChar() and Char.titlecase() that are currently JVM-only
- [`KT-44783`](https://youtrack.jetbrains.com/issue/KT-44783) Add IS_VALUE flag for value classes to kotlinx-metadata-jvm
- [`KT-44815`](https://youtrack.jetbrains.com/issue/KT-44815) Remove kotlin-annotations-android and JVM compiler support for @ParameterName/@DefaultValue/@DefaultNull
- [`KT-45213`](https://youtrack.jetbrains.com/issue/KT-45213) Update Unicode version used in K/N for Char and String case conversion functions

### Middle-end. IR

- [`KT-43831`](https://youtrack.jetbrains.com/issue/KT-43831) Compilation failed, IrSimpleFunctionPublicSymbolImpl is already bound
- [`KT-44100`](https://youtrack.jetbrains.com/issue/KT-44100) KJS / IR: Top level declarations added in IR plugin are not referenceable from other modules
- [`KT-45170`](https://youtrack.jetbrains.com/issue/KT-45170) IR: "AssertionError: Single expression value for GET_OBJECT" caused by inc operator of field inside scope function inside object

### Native

- [`KT-42446`](https://youtrack.jetbrains.com/issue/KT-42446) Native: SIGSEGV in Kotlin_Array_get on linuxArm64
- [`KT-43502`](https://youtrack.jetbrains.com/issue/KT-43502) [K/N] relocation R_X86_64_PC32 cannot be used against symbol __environ; recompile with -fPIC
- [`KT-44295`](https://youtrack.jetbrains.com/issue/KT-44295) 1.4.21 Kotlin native ndk compiler crash
- [`KT-44774`](https://youtrack.jetbrains.com/issue/KT-44774) ld fails with CALL16 reloc at 0x48f00 not against global symbol (Linux MIPS)
- [`KT-44746`](https://youtrack.jetbrains.com/issue/KT-44746) Different hashCode() results for Kotlin/Native stdlib

### Native. C and ObjC Import

- [`KT-44824`](https://youtrack.jetbrains.com/issue/KT-44824) cinterop tool no longer appends .klib to produced klibs

### Native. C Export

- [`KT-36639`](https://youtrack.jetbrains.com/issue/KT-36639) MPP: Build ios "staticLib" or "sharedLib" binary failed if interface contains member extension function
- [`KT-41725`](https://youtrack.jetbrains.com/issue/KT-41725) Dynamic library doesn't load on raspberrypi

### Native. ObjC Export

- [`KT-44549`](https://youtrack.jetbrains.com/issue/KT-44549) In the Xcode debug session, call stack is missing a frame when the iOS app fails

### Native. Platforms

- [`KT-45094`](https://youtrack.jetbrains.com/issue/KT-45094) Fail to compile Kotlin Native sources under Oracle Linux 7

### Reflection

- [`KT-44594`](https://youtrack.jetbrains.com/issue/KT-44594) Avoid using unnecessary array types reflection in kotlin-reflect
- [`KT-44782`](https://youtrack.jetbrains.com/issue/KT-44782) Add KClass.isValue to kotlin-reflect

### Tools. Ant

- [`KT-16227`](https://youtrack.jetbrains.com/issue/KT-16227) Ant task: do not include runtime by default if destination is a jar
- [`KT-44293`](https://youtrack.jetbrains.com/issue/KT-44293) Support fork mode in kotlinc Ant task

### Tools. CLI

- [`KT-17344`](https://youtrack.jetbrains.com/issue/KT-17344) Include kotlin-reflect to resulting jar if "-include-runtime" is specified
- [`KT-43220`](https://youtrack.jetbrains.com/issue/KT-43220) -include-runtime should add .kotlin_builtins to the output
- [`KT-43704`](https://youtrack.jetbrains.com/issue/KT-43704) Illegal reflective access by com.intellij.util.ReflectionUtil to method java.util.ResourceBundle.setParent(java.util.ResourceBundle)
- [`KT-44078`](https://youtrack.jetbrains.com/issue/KT-44078) Do not include module-info.class of kotlin-stdlib.jar to the resulting jar with -include-runtime
- [`KT-44232`](https://youtrack.jetbrains.com/issue/KT-44232) CLI: do not pass -noverify to java process starting from JDK 13
- [`KT-45566`](https://youtrack.jetbrains.com/issue/KT-45566) JDK 16 - e: java.lang.NoClassDefFoundError: Could not initialize class org.jetbrains.kotlin.com.intellij.pom.java.LanguageLevel

### Tools. CLI. Native

- [`KT-43874`](https://youtrack.jetbrains.com/issue/KT-43874) Native / CLI: provide a way to show difference between Jvm and Native compilers

### Tools. Compiler Plugins

- [`KT-45783`](https://youtrack.jetbrains.com/issue/KT-45783) Serialization: "AnalyzerException: Expected an object reference, but found I" caused by `JvmInline` and `Serializable` annotations

### Tools. Gradle

- [`KT-31027`](https://youtrack.jetbrains.com/issue/KT-31027) java.lang.NoSuchMethodError: No static method hashCode(Z)I in class Ljava/lang/Boolean; or its super classes (declaration of 'java.lang.Boolean' appears in /system/framework/core-libart.jar)
- [`KT-43605`](https://youtrack.jetbrains.com/issue/KT-43605) Kotlin Gradle Plugin 1.4.20 undeclared system property reads cause problems with Gradle configuration cache enabled
- [`KT-44204`](https://youtrack.jetbrains.com/issue/KT-44204) Kotlin Gradle Plugin 1.4.21 makes impossible to use ANTLR in other plugins
- [`KT-44361`](https://youtrack.jetbrains.com/issue/KT-44361) Gradle: deprecate options includeRuntime, noStdlib, noReflect
- [`KT-44462`](https://youtrack.jetbrains.com/issue/KT-44462) Kotlin Gradle plugin creates `compile` configuration with Gradle 7.0
- [`KT-44834`](https://youtrack.jetbrains.com/issue/KT-44834) Gradle Kotlin DSL: Add `languageSettings` configuration lambda without `apply` call
- [`KT-44949`](https://youtrack.jetbrains.com/issue/KT-44949) Compatibility with Gradle 7.0
- [`KT-44957`](https://youtrack.jetbrains.com/issue/KT-44957) gradle - target.compilations seems to be deprecated
- [`KT-45340`](https://youtrack.jetbrains.com/issue/KT-45340) Update minimal supported version of Kotlin Gradle Plugin to 6.1

### Tools. Gradle. JS

- [`KT-43237`](https://youtrack.jetbrains.com/issue/KT-43237) KJS: `-jsLegacy` Naming Convention is incompatible with NPM
- [`KT-43869`](https://youtrack.jetbrains.com/issue/KT-43869) Error in webpack configuration not displayed
- [`KT-44614`](https://youtrack.jetbrains.com/issue/KT-44614) Update Node.JS and Yarn versions
- [`KT-44616`](https://youtrack.jetbrains.com/issue/KT-44616) Kotlin/JS: IR backend with React: "Uncaught TypeError: _this__0 is undefined" runtime error in browser
- [`KT-45574`](https://youtrack.jetbrains.com/issue/KT-45574) Sync Kotlin/JS compile tasks into one folder (build/js/packages/<package>/kotlin)

### Tools. Gradle. Multiplatform

- [`KT-42098`](https://youtrack.jetbrains.com/issue/KT-42098) Commonizer is re-launched for every included Gradle build
- [`KT-43116`](https://youtrack.jetbrains.com/issue/KT-43116) Merge together MultiplatformHighlightingTest and MultiplatformAnalysisTest
- [`KT-44322`](https://youtrack.jetbrains.com/issue/KT-44322) KotlinTargetComponent maintenance for -sources.jar
- [`KT-44900`](https://youtrack.jetbrains.com/issue/KT-44900) Support gradle configuration cache with kotlin.multiplatform plugin

### Tools. Gradle. Native

- [`KT-46122`](https://youtrack.jetbrains.com/issue/KT-46122) kotlinx-serialization and kotlinx-datetime can't be built with 1.5.0-RC

### Tools. JPS

- [`KT-13631`](https://youtrack.jetbrains.com/issue/KT-13631) Compilation fails on Turkish locale because of locale-sensitive uppercasing
- [`KT-44644`](https://youtrack.jetbrains.com/issue/KT-44644) Mark all `@JvmMultifileClass` parts compiled in the previous round as dirty in the JPS plugin, similarly to how it’s done in the Gradle plugin

### Tools. Scripts

- [`KT-45194`](https://youtrack.jetbrains.com/issue/KT-45194) KT: Generate Kotlin Entities script: it doesn't work
- [`KT-44580`](https://youtrack.jetbrains.com/issue/KT-44580) Scripts: Unable to set new file annotation hooks after first snippet compilation

### Tools. kapt

- [`KT-43686`](https://youtrack.jetbrains.com/issue/KT-43686) KaptWithoutKotlincTask should use `@CompileClasspath` for `kotlinStdlibClasspath` for cache relocateability.
- [`KT-44130`](https://youtrack.jetbrains.com/issue/KT-44130) KAPT changes field order in 1.4.30-M1
- [`KT-44909`](https://youtrack.jetbrains.com/issue/KT-44909) Kapt: ReenteringLazyValueComputationException without stacktrace caused by `when` expression with sealed class function without explicit return type
- [`KT-45168`](https://youtrack.jetbrains.com/issue/KT-45168) KAPT: Java stubs generated for Kotlin files generated by annotation processors


## Recent ChangeLogs:
### [ChangeLog-1.4.X](docs/changelogs/ChangeLog-1.4.X.md)
### [ChangeLog-1.3.X](docs/changelogs/ChangeLog-1.3.X.md)
### [ChangeLog-1.2.X](docs/changelogs/ChangeLog-1.2.X.md)
### [ChangeLog-1.1.X](docs/changelogs/ChangeLog-1.1.X.md)
### [ChangeLog-1.0.X](docs/changelogs/ChangeLog-1.0.X.md)
