# CHANGELOG

## 1.4.30-M1

### Android

- [`KT-42383`](https://youtrack.jetbrains.com/issue/KT-42383) HMPP: Bad IDEA dependencies: Missing dependency from p1:jvmAndAndroid to p2:jvmAndAndroid

### Backend. Native

- [`KT-38772`](https://youtrack.jetbrains.com/issue/KT-38772) Native: support non-reified type parameters in typeOf
- [`KT-42234`](https://youtrack.jetbrains.com/issue/KT-42234) Move LLVM optimization parameters into konan.properties
- [`KT-42649`](https://youtrack.jetbrains.com/issue/KT-42649) IndexOutOfBoundsException during InlineClassTransformer lowering
- [`KT-42942`](https://youtrack.jetbrains.com/issue/KT-42942) Native: optimize peak backend memory by clearing BindingContext after psi2ir
- [`KT-43198`](https://youtrack.jetbrains.com/issue/KT-43198) init blocks inside inline classes

### Compiler

#### New Features

- [`KT-28055`](https://youtrack.jetbrains.com/issue/KT-28055) Consider supporting `init` blocks inside inline classes
- [`KT-28056`](https://youtrack.jetbrains.com/issue/KT-28056) Consider supporting non-public primary constructors for inline classes
- [`KT-42094`](https://youtrack.jetbrains.com/issue/KT-42094) Allow open callable members in expect interfaces
- [`KT-43129`](https://youtrack.jetbrains.com/issue/KT-43129) FIR: Support OverloadResolutionByLambdaReturnType

#### Performance Improvements

- [`KT-41352`](https://youtrack.jetbrains.com/issue/KT-41352) JVM IR: reduce bytecode size in for loops and range checks with 'until' by not using inclusive end
- [`KT-41644`](https://youtrack.jetbrains.com/issue/KT-41644) NI: Infinite compilation
- [`KT-42791`](https://youtrack.jetbrains.com/issue/KT-42791) OutOfMemoryError on compilation using kotlin 1.4 on a class with a lot of type inference
- [`KT-42920`](https://youtrack.jetbrains.com/issue/KT-42920) NI: Improve performance around adding constraints

#### Fixes

- [`KT-22465`](https://youtrack.jetbrains.com/issue/KT-22465) Excessive synthetic method for private setter from superclass
- [`KT-26229`](https://youtrack.jetbrains.com/issue/KT-26229) Lambda/anonymous function argument in parentheses is not supported for callsInPlace effect
- [`KT-32228`](https://youtrack.jetbrains.com/issue/KT-32228) Inconsistent boxing/unboxing for inline classes when interface is specialized by object expression
- [`KT-32450`](https://youtrack.jetbrains.com/issue/KT-32450) Inline class incorrectly gets re-wrapped when provided to a function
- [`KT-35849`](https://youtrack.jetbrains.com/issue/KT-35849) Missing nullability assertion on lambda return value if expected type has generic return value type
- [`KT-35902`](https://youtrack.jetbrains.com/issue/KT-35902) Kotlin generates a private parameterless constructor for constructors taking inline class arguments with default values
- [`KT-36769`](https://youtrack.jetbrains.com/issue/KT-36769) JVM IR: Missing LVT entries for inline function (default) parameters at call site
- [`KT-36982`](https://youtrack.jetbrains.com/issue/KT-36982) JVM IR: SAM adapter classes are generated as synthetic
- [`KT-37007`](https://youtrack.jetbrains.com/issue/KT-37007) JVM IR: extraneous property accessors are generated in multifile facade for InlineOnly property
- [`KT-37317`](https://youtrack.jetbrains.com/issue/KT-37317) [FIR] Add support of extension functions in postponed lambda completion
- [`KT-38400`](https://youtrack.jetbrains.com/issue/KT-38400) FIR: interface abstract is preferred to Any method in super resolve
- [`KT-38536`](https://youtrack.jetbrains.com/issue/KT-38536) JVM IR: bound adapted function references are not inlined
- [`KT-38656`](https://youtrack.jetbrains.com/issue/KT-38656) FIR: determine overridden member visibility properly
- [`KT-38901`](https://youtrack.jetbrains.com/issue/KT-38901) FIR: Make behavior of integer literals overflow consistent with FE 1.0
- [`KT-39709`](https://youtrack.jetbrains.com/issue/KT-39709) [FIR] False positive UNINITIALIZED_VARIABLE in presence of complex graph with jumps
- [`KT-39923`](https://youtrack.jetbrains.com/issue/KT-39923) Result.Failure will get wrapped with Success when using with RxJava
- [`KT-40198`](https://youtrack.jetbrains.com/issue/KT-40198) '$default' methods in 'kotlin/test/AssertionsKt' generated as non-synthetic by JVM_IR
- [`KT-40262`](https://youtrack.jetbrains.com/issue/KT-40262) ACC_DEPRECATED flag not generated for property getter delegate in multifile class facade in JVM_IR
- [`KT-40282`](https://youtrack.jetbrains.com/issue/KT-40282) Inline class wrapping Any gets double boxed
- [`KT-40464`](https://youtrack.jetbrains.com/issue/KT-40464) JVM_IR does not generate LINENUMBER at closing brace of (suspend) lambda
- [`KT-40948`](https://youtrack.jetbrains.com/issue/KT-40948) IllegalAccessError while initializing val property in EXACTLY_ONCE lambda that is passed to another function
- [`KT-41468`](https://youtrack.jetbrains.com/issue/KT-41468) JVM IR: IllegalAccessError on access to abstract base member from another package, from anonymous object inside abstract class
- [`KT-41493`](https://youtrack.jetbrains.com/issue/KT-41493) JVM IR: names of classes for local delegated variables contain the variable name twice
- [`KT-41792`](https://youtrack.jetbrains.com/issue/KT-41792) [FIR] Introduce & use ConeAttribute.UnsafeVariance
- [`KT-41793`](https://youtrack.jetbrains.com/issue/KT-41793) [FIR] Make captured types accessible at the end of resolve
- [`KT-41809`](https://youtrack.jetbrains.com/issue/KT-41809) JVM IR: name for internal $default method doesn't include module name
- [`KT-41810`](https://youtrack.jetbrains.com/issue/KT-41810) JVM IR: Deprecated(HIDDEN) class is incorrectly generated as synthetic
- [`KT-41841`](https://youtrack.jetbrains.com/issue/KT-41841) JVM IR: delegates for private functions with default arguments are generated in multifile classes
- [`KT-41857`](https://youtrack.jetbrains.com/issue/KT-41857) Flaky 'ConcurrentModificationException' through `kotlin.serialization.DescriptorSerializer`
- [`KT-41903`](https://youtrack.jetbrains.com/issue/KT-41903) JVM IR: do not generate LineNumberTable in auto-generated members of data classes
- [`KT-41957`](https://youtrack.jetbrains.com/issue/KT-41957) JVM IR: step into suspend function goes to the first line of the file
- [`KT-41960`](https://youtrack.jetbrains.com/issue/KT-41960) JVM IR: smart step into members implemented with delegation to interface doesn't work
- [`KT-41961`](https://youtrack.jetbrains.com/issue/KT-41961) JVM IR: line numbers are not generated in JvmMultifileClass facade declarations
- [`KT-41962`](https://youtrack.jetbrains.com/issue/KT-41962) JVM IR: intermittent -1 line numbers in the state machine cause double stepping in the debugger
- [`KT-42002`](https://youtrack.jetbrains.com/issue/KT-42002) JVM / IR: IllegalStateException: "No mapping for symbol: VAR IR_TEMPORARY_VARIABLE" caused by named arguments
- [`KT-42021`](https://youtrack.jetbrains.com/issue/KT-42021) JVM / IR: "IndexOutOfBoundsException: Index 0 out of bounds for length 0" during IR lowering with suspend conversion
- [`KT-42033`](https://youtrack.jetbrains.com/issue/KT-42033) JVM IR: accidental override in Map subclass with custom implementations of some members
- [`KT-42043`](https://youtrack.jetbrains.com/issue/KT-42043) JVM IR: Don't generate collection stubs when implementing methods with more specific return types
- [`KT-42044`](https://youtrack.jetbrains.com/issue/KT-42044) Compiler error when lambda with contract surrounded with parentheses
- [`KT-42114`](https://youtrack.jetbrains.com/issue/KT-42114) JVM_IR generates stub for 'removeIf' in abstract classes implementing 'List' and 'Set'
- [`KT-42115`](https://youtrack.jetbrains.com/issue/KT-42115) JVM_IR doesn't generate 'next' and 'hasNext' method in an abstract class implementing 'ListIterator'
- [`KT-42116`](https://youtrack.jetbrains.com/issue/KT-42116) FIR: Java accessor function should not exist in scope together with relevant property
- [`KT-42117`](https://youtrack.jetbrains.com/issue/KT-42117) IR-based evaluator cannot handle Java static final fields
- [`KT-42118`](https://youtrack.jetbrains.com/issue/KT-42118) FIR2IR: field-targeted annotation is placed on a property, not on a field
- [`KT-42130`](https://youtrack.jetbrains.com/issue/KT-42130) FIR: type variable is observed after when condition analysis
- [`KT-42132`](https://youtrack.jetbrains.com/issue/KT-42132) FIR2IR: companion function reference has no dispatch receiver
- [`KT-42137`](https://youtrack.jetbrains.com/issue/KT-42137) JVM IR: AbstractMethodError on complex hierarchy where implementation comes from another supertype and has a more specific type
- [`KT-42186`](https://youtrack.jetbrains.com/issue/KT-42186) JVM / IR: Infinite cycle in for expression when unsigned bytes are used in decreasing loop range
- [`KT-42251`](https://youtrack.jetbrains.com/issue/KT-42251) JVM / IR: "IllegalStateException: Descriptor can be left only if it is last" when comparing the i-th element of the container of Int? and `i` with change
- [`KT-42253`](https://youtrack.jetbrains.com/issue/KT-42253) JVM IR: NoSuchFieldError on local delegated property in inline function whose call site happens before declaration in the source
- [`KT-42281`](https://youtrack.jetbrains.com/issue/KT-42281) JVM / IR: AnalyzerException when comparing Int and array that cast to Any in if condition
- [`KT-42340`](https://youtrack.jetbrains.com/issue/KT-42340) FIR2IR: duplicating fake overrides
- [`KT-42344`](https://youtrack.jetbrains.com/issue/KT-42344) IR-based evaluator doesn't support "annotation in annotation"
- [`KT-42346`](https://youtrack.jetbrains.com/issue/KT-42346) FIR: double-vararg in IR while resolving collection literal as Java annotation argument
- [`KT-42348`](https://youtrack.jetbrains.com/issue/KT-42348) FIR: false UNINITIALIZED_VARIABLE in local class
- [`KT-42350`](https://youtrack.jetbrains.com/issue/KT-42350) FIR: false UNINITIALIZED_VARIABLE after initialization in try block
- [`KT-42351`](https://youtrack.jetbrains.com/issue/KT-42351) FIR: false HIDDEN in enum entry member call
- [`KT-42354`](https://youtrack.jetbrains.com/issue/KT-42354) JVM / IR: "AssertionError: Unexpected IR element found during code generation" with KProperty `get` invocation
- [`KT-42359`](https://youtrack.jetbrains.com/issue/KT-42359) FIR2IR: cannot mangle type parameter
- [`KT-42373`](https://youtrack.jetbrains.com/issue/KT-42373) FIR2IR: local object nested class has no parent if forward-referenced by nested class supertype
- [`KT-42384`](https://youtrack.jetbrains.com/issue/KT-42384) FIR (BE): top-level field has no parent class in BE
- [`KT-42496`](https://youtrack.jetbrains.com/issue/KT-42496) FIR resolve: synthetic property is written but has no setter
- [`KT-42517`](https://youtrack.jetbrains.com/issue/KT-42517) FIR: exception in BE for recursive inline call
- [`KT-42601`](https://youtrack.jetbrains.com/issue/KT-42601) [FIR] Inherited declaration clash for stdlib inheritors
- [`KT-42642`](https://youtrack.jetbrains.com/issue/KT-42642) ISE: No `getProgressionLastElement` for progression type IntProgressionType
- [`KT-42650`](https://youtrack.jetbrains.com/issue/KT-42650) JVM IR: extraneous nullability annotation on a generic function of a flexible type
- [`KT-42656`](https://youtrack.jetbrains.com/issue/KT-42656) FIR2IR: unsupported callable reference for Java field
- [`KT-42725`](https://youtrack.jetbrains.com/issue/KT-42725) Debugger steps into core library inline functions in chained calls
- [`KT-42758`](https://youtrack.jetbrains.com/issue/KT-42758) JVM / IR: Deserialized object that overrides readResolve() is not reference equal to the singleton instance
- [`KT-42770`](https://youtrack.jetbrains.com/issue/KT-42770) FIR: duplicating signatures in mangler (typealias for functional type)
- [`KT-42771`](https://youtrack.jetbrains.com/issue/KT-42771) FIR: duplicating signature in mangler (data class with delegate)
- [`KT-42814`](https://youtrack.jetbrains.com/issue/KT-42814) FIR: false UNINITIALIZED_VARIABLE in local function after if...else
- [`KT-42844`](https://youtrack.jetbrains.com/issue/KT-42844) FIR: Property write in init block resolved to parameter write
- [`KT-42846`](https://youtrack.jetbrains.com/issue/KT-42846) JVM_IR: NPE on function reference to @JvmStatic method in a different file
- [`KT-42933`](https://youtrack.jetbrains.com/issue/KT-42933) JVM / IR: "AnalyzerException: Expected an object reference, but found I" with local delegate in inline class
- [`KT-43006`](https://youtrack.jetbrains.com/issue/KT-43006) JVM/JVM_IR: do not generate no-arg constructor for constructor with default arguments if there are inline class types in the signature
- [`KT-43017`](https://youtrack.jetbrains.com/issue/KT-43017) JVM / IR: AssertionError when callable reference passed into a function requiring a suspendable function
- [`KT-43068`](https://youtrack.jetbrains.com/issue/KT-43068) JVM IR: no generic signatures for explicitly written methods in a List subclass, whose signature coincides with MutableList methods
- [`KT-43132`](https://youtrack.jetbrains.com/issue/KT-43132) JVM / IR: Method name '<get-...>' in class '...$screenTexts$1$1' cannot be represented in dex format.
- [`KT-43145`](https://youtrack.jetbrains.com/issue/KT-43145) JVM IR: $default methods in multi-file facades are generated as non-synthetic final
- [`KT-43156`](https://youtrack.jetbrains.com/issue/KT-43156) FIR: false UNINITIALIZED_VARIABLE after initialization in `synchronized` block
- [`KT-43196`](https://youtrack.jetbrains.com/issue/KT-43196) JVM: extra non-static member is generated for extension property in inline class
- [`KT-43199`](https://youtrack.jetbrains.com/issue/KT-43199) JVM IR: synthetic flag for deprecated-hidden is not generated for DeprecatedSinceKotlin and deprecation from override
- [`KT-43207`](https://youtrack.jetbrains.com/issue/KT-43207) JVM IR: no collection stub for `iterator` is generated on extending AbstractCollection
- [`KT-43217`](https://youtrack.jetbrains.com/issue/KT-43217) JVM_IR: Multiple FAKE_OVERRIDES for java methods using @NonNull Double and java double
- [`KT-43226`](https://youtrack.jetbrains.com/issue/KT-43226) "Incompatible stack heights" with non-local return to outer lambda inside suspend lambda
- [`KT-43242`](https://youtrack.jetbrains.com/issue/KT-43242) JVM / IR: "AnalyzerException: Expected I, but found R" caused by `when` inside object with @Nullable Integer subject
- [`KT-43249`](https://youtrack.jetbrains.com/issue/KT-43249) Wrong code generated for suspend lambdas with inline class parameters
- [`KT-43286`](https://youtrack.jetbrains.com/issue/KT-43286) JVM IR: IAE "Inline class types should have the same representation: Lkotlin/UInt; != I" on smart cast of unsigned type value with JVM target 1.8
- [`KT-43326`](https://youtrack.jetbrains.com/issue/KT-43326) JVM_IR: No deprecated flag for getter of deprecated interface property copied to DefaultImpls
- [`KT-43327`](https://youtrack.jetbrains.com/issue/KT-43327) JVM_IR: No deprecated or synthetic flag for accessors of deprecated-hidden property of unsigned type
- [`KT-43332`](https://youtrack.jetbrains.com/issue/KT-43332) FIR: Smart casts lead to false-positive ambiguity
- [`KT-43370`](https://youtrack.jetbrains.com/issue/KT-43370) JVM IR: No deprecated flag for getter of deprecated property copied via delegation by interface
- [`KT-43525`](https://youtrack.jetbrains.com/issue/KT-43525) Prohibit JvmOverloads on declarations with inline class types in parameters
- [`KT-43562`](https://youtrack.jetbrains.com/issue/KT-43562) JVM IR: incorrect mangling for Collection.size in unsigned arrays
- [`KT-43584`](https://youtrack.jetbrains.com/issue/KT-43584) [FIR] Java annotations with named arguments aren't loaded correctly

### IDE

- [`KT-31553`](https://youtrack.jetbrains.com/issue/KT-31553) Complete Statement: Wrong auto-insertion of closing curly brace for a code block
- [`KT-33466`](https://youtrack.jetbrains.com/issue/KT-33466) IDE generates incorrect `external override` with body for overriding `open external` method
- [`KT-39458`](https://youtrack.jetbrains.com/issue/KT-39458) Add CLI support for UL classes
- [`KT-40403`](https://youtrack.jetbrains.com/issue/KT-40403) UAST: PsiMethod for invoked extension function/property misses `@receiver:` annotations
- [`KT-41406`](https://youtrack.jetbrains.com/issue/KT-41406) Kotlin doesn't report annotations for type arguments (no way to add `@Nls`, `@NonNls` annotations to String collections in Kotlin)
- [`KT-41420`](https://youtrack.jetbrains.com/issue/KT-41420) UAST does not return information about type annotations
- [`KT-42754`](https://youtrack.jetbrains.com/issue/KT-42754) MPP: no smart cast for Common nullable property used in platform module
- [`KT-42821`](https://youtrack.jetbrains.com/issue/KT-42821) MPP, IDE: Platform-specific errors are reported even when build doesn't target that platform

### IDE. Android

- [`KT-42381`](https://youtrack.jetbrains.com/issue/KT-42381) MPP: Bad IDEA dependencies: JVM module depending on built artifact instead of sources of module with Android Plugin applied

### IDE. Inspections and Intentions

#### New Features

- [`KT-22666`](https://youtrack.jetbrains.com/issue/KT-22666) "Create enum constant" quick fix could be provided
- [`KT-24556`](https://youtrack.jetbrains.com/issue/KT-24556) Add Remove quick fix for "Expression under 'when' is never equal to null"
- [`KT-34121`](https://youtrack.jetbrains.com/issue/KT-34121) Report unused result of data class `copy` method
- [`KT-34533`](https://youtrack.jetbrains.com/issue/KT-34533) INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER: Add quickfix "Add val to parameter"
- [`KT-35215`](https://youtrack.jetbrains.com/issue/KT-35215) Quickfix for CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT to remove `const` modifier
- [`KT-40251`](https://youtrack.jetbrains.com/issue/KT-40251) Intention action to evaluate compile time expression

#### Fixes

- [`KT-20420`](https://youtrack.jetbrains.com/issue/KT-20420) Intention "Put arguments/parameters on separate lines" doesn't respect the "Place ')' on new line" Kotlin code style setting
- [`KT-21799`](https://youtrack.jetbrains.com/issue/KT-21799) Quickfix "Change function signature" for receiver type doesn't change it
- [`KT-22665`](https://youtrack.jetbrains.com/issue/KT-22665) "Create object" quick fix produce wrong code for enum
- [`KT-23934`](https://youtrack.jetbrains.com/issue/KT-23934) IntelliJ suggest "merge map to joinToString" even when such action is impossible due to suspending actions in map
- [`KT-30894`](https://youtrack.jetbrains.com/issue/KT-30894) Wrong results of intention "Add names to call arguments" when backticked argument starts from digit
- [`KT-31523`](https://youtrack.jetbrains.com/issue/KT-31523) ReplaceWith introduces additional argument name for lambda when named argument is used on call-site
- [`KT-31833`](https://youtrack.jetbrains.com/issue/KT-31833) JavaMapForEachInspection should report for expression with implicit receiver
- [`KT-33096`](https://youtrack.jetbrains.com/issue/KT-33096) Turn 'MapGetWithNotNullAssertionOperator' into an intention
- [`KT-33212`](https://youtrack.jetbrains.com/issue/KT-33212) False positive "map.put() should be converted to assignment" inspection when class inherited from MutableMap has "set" method
- [`KT-34270`](https://youtrack.jetbrains.com/issue/KT-34270) False negative "Join declaration and assignment" with constructor call
- [`KT-34859`](https://youtrack.jetbrains.com/issue/KT-34859) False positive "Should be replaced with Kotlin function" inspection for Character.toString(int) function
- [`KT-34959`](https://youtrack.jetbrains.com/issue/KT-34959) False positive "Redundant overriding method" with different implemented/overridden signatures
- [`KT-35051`](https://youtrack.jetbrains.com/issue/KT-35051) False positive "Remove redundant backticks" if variable inside the string and isn't followed by space
- [`KT-35097`](https://youtrack.jetbrains.com/issue/KT-35097) False positive "Call replaceable with binary operator" on explicit 'equals' call on a platform type value
- [`KT-35165`](https://youtrack.jetbrains.com/issue/KT-35165) "Replace 'if' with elvis operator": don't suggest if val initializer is a complex expression
- [`KT-35346`](https://youtrack.jetbrains.com/issue/KT-35346) False positive 'Make internal' suggestion for function inside interface
- [`KT-35357`](https://youtrack.jetbrains.com/issue/KT-35357) "Move lambda argument out of parentheses" does not preserve block comments
- [`KT-38349`](https://youtrack.jetbrains.com/issue/KT-38349) Invalid suggestion to fold to elvis when having a var-variable
- [`KT-40704`](https://youtrack.jetbrains.com/issue/KT-40704) False negative "Redundant semicolon" at start of line
- [`KT-40861`](https://youtrack.jetbrains.com/issue/KT-40861) "Convert to secondary constructor" intention expected on class name
- [`KT-40879`](https://youtrack.jetbrains.com/issue/KT-40879) False positive "Redundant 'inner' modifier" when calling another inner class with empty constructor
- [`KT-40985`](https://youtrack.jetbrains.com/issue/KT-40985) "Remove explicit type arguments" is suggested when type has an annotation
- [`KT-41223`](https://youtrack.jetbrains.com/issue/KT-41223) False positive "Redundant inner modifier" inspection ignores constructor arguments of object expressions
- [`KT-41246`](https://youtrack.jetbrains.com/issue/KT-41246) False positive "Receiver parameter is never used" with anonymous function expression
- [`KT-41298`](https://youtrack.jetbrains.com/issue/KT-41298) "Remove redundant 'with' call" intention works incorrectly with non-local returns and single-expression functions
- [`KT-41311`](https://youtrack.jetbrains.com/issue/KT-41311) False positive "Redundant inner modifier" when deriving from a nested Java class
- [`KT-41499`](https://youtrack.jetbrains.com/issue/KT-41499) "Convert receiver to parameter" produces code with incorrect order of generic type and function invocation  in case of generic function with lambda as a parameter
- [`KT-41680`](https://youtrack.jetbrains.com/issue/KT-41680) False positive "Redundant inner modifier" when deriving from class with non-empty constructor and value passed to it from enclosing class
- [`KT-42201`](https://youtrack.jetbrains.com/issue/KT-42201) Add Opt-In action doesn't work if there is already OptIn annotation
- [`KT-42255`](https://youtrack.jetbrains.com/issue/KT-42255) "Replace elvis expression with 'if' expression" intention shouldn't introduce unnecessary variable if 'error' expression is used

### IDE. JS

- [`KT-43760`](https://youtrack.jetbrains.com/issue/KT-43760) KJS: Debugging Kotlin code for Node.js runtime doesn't work

### IDE. Scratch

- [`KT-43415`](https://youtrack.jetbrains.com/issue/KT-43415) Kotlin scratch file could not be run and could lead to dead lock

### JavaScript

#### Fixes

- [`KT-31072`](https://youtrack.jetbrains.com/issue/KT-31072) Don't use non-reified arguments to specialize type operations in IR inliner
- [`KT-39964`](https://youtrack.jetbrains.com/issue/KT-39964) Throwable incorrectly implements constructor for (null, cause) args in K/JS-IR
- [`KT-40090`](https://youtrack.jetbrains.com/issue/KT-40090) KJS: IR. Invalid behaviour for optional parameters (redundant tail undefined parameters)
- [`KT-40686`](https://youtrack.jetbrains.com/issue/KT-40686) KJS: Uncaught ReferenceError caused by external class as type inside eventListener in init block
- [`KT-40771`](https://youtrack.jetbrains.com/issue/KT-40771) KJS / IR: "ReferenceError: Metadata is not defined" caused by default parameter value in inner class constructor
- [`KT-41032`](https://youtrack.jetbrains.com/issue/KT-41032) KJS / IR: "AssertionError: Assertion failed" caused by class that is delegated to inherited interface
- [`KT-41771`](https://youtrack.jetbrains.com/issue/KT-41771) KJS / IR: IndexOutOfBoundsException "Index 0 out of bounds for length 0" caused by inline class with List in primary constructor and vararg in secondary
- [`KT-42025`](https://youtrack.jetbrains.com/issue/KT-42025) KJS / IR:  IrConstructorCallImpl: No such type argument slot: 0
- [`KT-42112`](https://youtrack.jetbrains.com/issue/KT-42112) KJS: StackOverflowError on @JsExport in case of name clash with function with Enum parameter with star-projection
- [`KT-42262`](https://youtrack.jetbrains.com/issue/KT-42262) KJS: `break`-statements without label are ignored in a `when`
- [`KT-42364`](https://youtrack.jetbrains.com/issue/KT-42364) KJS: Properties of interface delegate are non-configurable
- [`KT-43222`](https://youtrack.jetbrains.com/issue/KT-43222) KJS IR: prototype lazy initialization for top-level properties like in JVM
- [`KT-43313`](https://youtrack.jetbrains.com/issue/KT-43313) KJS / IR: "Can't find name for declaration FUN" for secondary constructor

### KMM Plugin

- [`KT-41677`](https://youtrack.jetbrains.com/issue/KT-41677) Could not launch iOS project with custom display name
- [`KT-42463`](https://youtrack.jetbrains.com/issue/KT-42463) Launch common tests on local JVM via run gutter
- [`KT-43188`](https://youtrack.jetbrains.com/issue/KT-43188) NoSuchMethodError in New Module Wizard of KMM Project

### Libraries

- [`KT-41112`](https://youtrack.jetbrains.com/issue/KT-41112) Docs: add more details about bit shift operations
- [`KT-41356`](https://youtrack.jetbrains.com/issue/KT-41356) Incorrect documentation for `rangeTo` function

### Middle-end. IR

- [`KT-41765`](https://youtrack.jetbrains.com/issue/KT-41765) [Native/IR]  Could not resolveFakeOverride()
- [`KT-42054`](https://youtrack.jetbrains.com/issue/KT-42054) Psi2ir: "RuntimeException: IrSimpleFunctionSymbolImpl is already bound" when using result of function with overload resolution by lambda return type

### Native. C and ObjC Import

- [`KT-42412`](https://youtrack.jetbrains.com/issue/KT-42412) [C-interop] Modality of generated property accessors is always FINAL

### Native. ObjC Export

- [`KT-38530`](https://youtrack.jetbrains.com/issue/KT-38530) Native: values() method of enum classes is not exposed to Objective-C/Swift
- [`KT-43599`](https://youtrack.jetbrains.com/issue/KT-43599) K/N: Unbound symbols not allowed

### Native. Platform libraries

- [`KT-43597`](https://youtrack.jetbrains.com/issue/KT-43597) Support for Xcode 12.2 SDK

### Native. Platforms

- [`KT-43276`](https://youtrack.jetbrains.com/issue/KT-43276) Support watchos_x64 target

### Native. Runtime

- [`KT-42822`](https://youtrack.jetbrains.com/issue/KT-42822) Kotlin/Native Worker leaks ObjC/Swift autorelease references (and indirectly bridged K/N references) on Darwin targets

### Native. Stdlib

- [`KT-42428`](https://youtrack.jetbrains.com/issue/KT-42428) Inconsistent behavior of map.entries on Kotlin.Native

### Reflection

- [`KT-34024`](https://youtrack.jetbrains.com/issue/KT-34024) "KotlinReflectionInternalError: Inconsistent number of parameters" with `javaMethod` on suspending functions with inline class in function signature or inside the function

### Tools. CLI

- [`KT-43406`](https://youtrack.jetbrains.com/issue/KT-43406) JVM: produce deterministic jar files if -d option value is a .jar file

### Tools. CLI. Native

- [`KT-40670`](https://youtrack.jetbrains.com/issue/KT-40670) Allow to override konan.properties via CLI

### Tools. Compiler Plugins

- [`KT-41764`](https://youtrack.jetbrains.com/issue/KT-41764) KJS /IR IllegalStateException: "Symbol for public kotlin/arrayOf is unbound" with serialization plugin
- [`KT-42976`](https://youtrack.jetbrains.com/issue/KT-42976) kotlinx.serialization + JVM IR: NPE on annotation with @SerialInfo

### Tools. Gradle

- [`KT-38692`](https://youtrack.jetbrains.com/issue/KT-38692) KaptGenerateStubs Gradle task will not clean up outputs when sources are empty and not an incremental build
- [`KT-40140`](https://youtrack.jetbrains.com/issue/KT-40140) kotlin-android plugin eagerly creates several Gradle tasks
- [`KT-41295`](https://youtrack.jetbrains.com/issue/KT-41295) Kotlin Gradle Plugin 1.4.20 Configuration Caching bug due to friendPath provider
- [`KT-42058`](https://youtrack.jetbrains.com/issue/KT-42058) Support moduleName option in Kotlin Gradle plugin for JVM
- [`KT-43054`](https://youtrack.jetbrains.com/issue/KT-43054) Implementation of `AbstractKotlinTarget#buildAdhocComponentsFromKotlinVariants` breaks configuration caching
- [`KT-43489`](https://youtrack.jetbrains.com/issue/KT-43489) Incremental compilation - unable to find history files causing full recompilation

### Tools. Gradle. JS

- [`KT-42400`](https://youtrack.jetbrains.com/issue/KT-42400) Kotlin/JS: Gradle DSL: customField() is rejected in Groovy build.gradle
- [`KT-42462`](https://youtrack.jetbrains.com/issue/KT-42462) NPM dependency declaration with Groovy interpolated string
- [`KT-42954`](https://youtrack.jetbrains.com/issue/KT-42954) Kotlin/JS: IDE import after changing `kotlin.js.externals.output.format` does not re-generate externals
- [`KT-43535`](https://youtrack.jetbrains.com/issue/KT-43535) Common webpack configuration breaks on lambda serialization in some cases

### Tools. Gradle. Multiplatform

- [`KT-42269`](https://youtrack.jetbrains.com/issue/KT-42269) Setup default dependsOn edges for Android source sets
- [`KT-42413`](https://youtrack.jetbrains.com/issue/KT-42413) [MPP/gradle] `withJava` breaks build on 1.4.20-M1
- [`KT-43141`](https://youtrack.jetbrains.com/issue/KT-43141) Gradle / Configuration cache: NPE from org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon.getKotlinOptions() on reusing configuration cache for task compileCommonMainKotlinMetadata
- [`KT-43329`](https://youtrack.jetbrains.com/issue/KT-43329) Gradle / Configuration cache: IAE “Parameter specified as non-null is null: method KotlinMetadataTargetConfiguratorKt.isCompatibilityMetadataVariantEnabled, parameter $this$isCompatibilityMetadataVariantEnabled” on reusing configuration cache for task compileKotlinMetadata

### Tools. Gradle. Native

- [`KT-39564`](https://youtrack.jetbrains.com/issue/KT-39564) Make kotlin-native Gradle tasks Cacheable
- [`KT-42485`](https://youtrack.jetbrains.com/issue/KT-42485) Fail on cinterop: clang_indexTranslationUnit returned 1
- [`KT-42550`](https://youtrack.jetbrains.com/issue/KT-42550) Adding subspec dependency with git location failed
- [`KT-42849`](https://youtrack.jetbrains.com/issue/KT-42849) Gradle / Configuration cache: tasks nativeMetadataJar, runReleaseExecutableNative, runDebugExecutableNative are unsupported and fails on reusing configuration cache
- [`KT-42938`](https://youtrack.jetbrains.com/issue/KT-42938) CocoaPods Gradle plugin: podBuildDependencies doesn't properly report xcodebuild failures
- [`KT-43151`](https://youtrack.jetbrains.com/issue/KT-43151) Gradle / Configuration cache: UPAE “lateinit property binary has not been initialized” on reusing configuration cache for linkDebugExecutableNative, linkDebugTestNative, linkReleaseExecutableNative tasks


### Tools. Parcelize

- [`KT-41553`](https://youtrack.jetbrains.com/issue/KT-41553) JVM IR, Parcelize: IrStarProjectionImpl cannot be cast to class IrTypeProjection

### Tools. kapt

- [`KT-34340`](https://youtrack.jetbrains.com/issue/KT-34340) Incremental annotation processor recompile all files (only if KAPT enabled).
- [`KT-36667`](https://youtrack.jetbrains.com/issue/KT-36667) Kapt: Add a flag to strip kotlin.Metadata() annotations from stubs
- [`KT-40493`](https://youtrack.jetbrains.com/issue/KT-40493) KAPT does not support aggregating annotations processors in incremental mode
- [`KT-40882`](https://youtrack.jetbrains.com/issue/KT-40882) Kapt stub generation is non-deterministic for incremental compilation
- [`KT-41788`](https://youtrack.jetbrains.com/issue/KT-41788) NullPointerException: Random crashes of build using gradle and kapt because of not calling Processor.init()
- [`KT-42182`](https://youtrack.jetbrains.com/issue/KT-42182) KAPT: Does not consider generated sources for incremental compilation.

## Recent ChangeLogs:
### [ChangeLog-1.3.X](ChangeLog-1.3.X.md)
### [ChangeLog-1.2.X](ChangeLog-1.2.X.md)
### [ChangeLog-1.1.X](ChangeLog-1.1.X.md)
### [ChangeLog-1.0.X](ChangeLog-1.0.X.md)