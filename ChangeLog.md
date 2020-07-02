# CHANGELOG

## 1.4-M2

### Compiler

#### New Features

- [`KT-37432`](https://youtrack.jetbrains.com/issue/KT-37432) Do not include annotations fields into 'visibility must be explicitly specified' check in api mode

#### Performance Improvements

- [`KT-27362`](https://youtrack.jetbrains.com/issue/KT-27362) Anonymous classes representing function/property references contain rarely used methods
- [`KT-35626`](https://youtrack.jetbrains.com/issue/KT-35626) NI: Performance problem with many type parameters
- [`KT-36047`](https://youtrack.jetbrains.com/issue/KT-36047) Compiler produces if-chain instead of switch when when subject captured as variable
- [`KT-36638`](https://youtrack.jetbrains.com/issue/KT-36638) Use 'java/lang/StringBuilder.append (C)Ljava/lang/StringBuilder;' when appending single character in JVM_IR
- [`KT-37389`](https://youtrack.jetbrains.com/issue/KT-37389) Avoid type approximation during generation constraints with EQUALITY kind
- [`KT-37392`](https://youtrack.jetbrains.com/issue/KT-37392) Avoid substitution and type approximation for simple calls
- [`KT-37546`](https://youtrack.jetbrains.com/issue/KT-37546) NI: high memory and CPU consumption due to creating useless captured types (storing in approximated types cache, unneeded computations)

#### Fixes

- [`KT-11265`](https://youtrack.jetbrains.com/issue/KT-11265) Factory pattern and overload resolution ambiguity
- [`KT-27524`](https://youtrack.jetbrains.com/issue/KT-27524) Inline class is boxed when used with suspend modifier
- [`KT-27586`](https://youtrack.jetbrains.com/issue/KT-27586) ClassCastException occurs if the Result (or any other inline class) is returned from a lambda
- [`KT-30419`](https://youtrack.jetbrains.com/issue/KT-30419) Use boxed version of an inline class in return type position for covariant and generic-specialized overrides
- [`KT-31163`](https://youtrack.jetbrains.com/issue/KT-31163) FIR: consider replacing comparisons with compareTo calls and some additional intrinsics
- [`KT-31585`](https://youtrack.jetbrains.com/issue/KT-31585) ClassCastException with derived class delegated to generic class with inline class type argument
- [`KT-31823`](https://youtrack.jetbrains.com/issue/KT-31823) NI: Type mismatch with a star projection and `UnsafeVariance`
- [`KT-33119`](https://youtrack.jetbrains.com/issue/KT-33119) Pre-increment for inline class wrapping Int compiles to direct increment instead of inc-impl
- [`KT-33715`](https://youtrack.jetbrains.com/issue/KT-33715) Kotlin/Native: metadata compiler
- [`KT-34048`](https://youtrack.jetbrains.com/issue/KT-34048) IllegalAccessError when initializing val property in EXACTLY_ONCE lambda
- [`KT-34433`](https://youtrack.jetbrains.com/issue/KT-34433) NI: Type mismatch with a star projection and `UnsafeVariance`
- [`KT-35133`](https://youtrack.jetbrains.com/issue/KT-35133) FIR Java: don't set 'isOperator' for methods with non-operator names
- [`KT-35234`](https://youtrack.jetbrains.com/issue/KT-35234) ClassCastException with creating an inline class from a function reference of covariant or generic-specialized override
- [`KT-35406`](https://youtrack.jetbrains.com/issue/KT-35406) Generic type implicitly inferred as Nothing with no warning
- [`KT-35587`](https://youtrack.jetbrains.com/issue/KT-35587) Plain namespace strings in JvmNameResolver.PREDEFINED_STRINGS are prone to namespace changes during jar relocation.
- [`KT-36044`](https://youtrack.jetbrains.com/issue/KT-36044) NI: premature fixation a type variable if there were nested lambdas (constraint source was the deepest lambda)
- [`KT-36057`](https://youtrack.jetbrains.com/issue/KT-36057) [FIR] Incorrect smartcast
- [`KT-36069`](https://youtrack.jetbrains.com/issue/KT-36069) NI: TYPE_MISMATCH caused by incorrect inference to Nothing
- [`KT-36125`](https://youtrack.jetbrains.com/issue/KT-36125) Callable reference resolution ambiguity error is not displayed properly in the IDE
- [`KT-36191`](https://youtrack.jetbrains.com/issue/KT-36191) IDE locks loading packages and editing file containing `try` keyword inside string template
- [`KT-36222`](https://youtrack.jetbrains.com/issue/KT-36222) NI: Improve error message about nullability mismatch for a generic call
- [`KT-36249`](https://youtrack.jetbrains.com/issue/KT-36249) NI doesn't use upper bound for T of called function during infer return type and as a result infer it to `Any?` if the resulting type was intersection type
- [`KT-36345`](https://youtrack.jetbrains.com/issue/KT-36345) FIR: record argument mapping for use in back-end
- [`KT-36446`](https://youtrack.jetbrains.com/issue/KT-36446) NI: "UnsupportedOperationException no descriptor for type constructor of IntegerLiteralType[Int,Long,Byte,Short]" with BuilderInference and delegate
- [`KT-36758`](https://youtrack.jetbrains.com/issue/KT-36758) [FIR] Unresolved callable reference to member of local class
- [`KT-36759`](https://youtrack.jetbrains.com/issue/KT-36759) [FIR] Unsupported callable reference resolution for methods with default parameters
- [`KT-36762`](https://youtrack.jetbrains.com/issue/KT-36762) [FIR] Unresolved `array.clone()`
- [`KT-36764`](https://youtrack.jetbrains.com/issue/KT-36764) [FIR] Bug in inference with DefinitelyNotNull types
- [`KT-36816`](https://youtrack.jetbrains.com/issue/KT-36816) NI: definitely not-null (T!!) types in invariant positions don't approximate to T inside inference process
- [`KT-36819`](https://youtrack.jetbrains.com/issue/KT-36819) NI: premature completion of lambdas, which are passed somewhere
- [`KT-36850`](https://youtrack.jetbrains.com/issue/KT-36850) Incorrect private visibility of sealed class constructors
- [`KT-36856`](https://youtrack.jetbrains.com/issue/KT-36856) Throwing exception when there is inheritance in Kotlin from Java class, which contains methods with the same JVM descriptors
- [`KT-36879`](https://youtrack.jetbrains.com/issue/KT-36879) Introduce FIR_IDENTICAL in diagnostic tests
- [`KT-36881`](https://youtrack.jetbrains.com/issue/KT-36881) FIR: completion don't runs for return expressions
- [`KT-36887`](https://youtrack.jetbrains.com/issue/KT-36887) [FIR] Unresolved member in nested lambda in initializer
- [`KT-36905`](https://youtrack.jetbrains.com/issue/KT-36905) [FIR] Unresolved in lambda in default argument position
- [`KT-36953`](https://youtrack.jetbrains.com/issue/KT-36953) AssertionError: "Unsigned type expected: null" when there is a range with an unsigned type
- [`KT-37009`](https://youtrack.jetbrains.com/issue/KT-37009) FIR: Bound smart-cast lost
- [`KT-37027`](https://youtrack.jetbrains.com/issue/KT-37027) FIR: Wrong projection on spread + varargs on non-final types
- [`KT-37038`](https://youtrack.jetbrains.com/issue/KT-37038) NI: redundant lambda's arrow breaks CST calculation for extension lambdas
- [`KT-37043`](https://youtrack.jetbrains.com/issue/KT-37043) NI: inference T to Any? if there was elvis between Java out-type and reified `materialize` for this type without out projection
- [`KT-37066`](https://youtrack.jetbrains.com/issue/KT-37066) [FIR] Wrong type inference for lambdas
- [`KT-37070`](https://youtrack.jetbrains.com/issue/KT-37070) [FIR] Unresolved parameters of outer lambda in scope of inner lambda
- [`KT-37087`](https://youtrack.jetbrains.com/issue/KT-37087) "IllegalStateException: Can't find method 'invoke()'" for mutable property reference in default value of an inline function parameter
- [`KT-37091`](https://youtrack.jetbrains.com/issue/KT-37091) [FIR] Wrong inferred type of when-expression if when-argument  is not-null-asserted and type is not specifies explicitly
- [`KT-37176`](https://youtrack.jetbrains.com/issue/KT-37176) [FIR] Incorrect resolution mode for statements of block
- [`KT-37302`](https://youtrack.jetbrains.com/issue/KT-37302) Unexpected conversion:`Int` constant inferred to `Long` in when expression
- [`KT-37327`](https://youtrack.jetbrains.com/issue/KT-37327) FIR: Smartcast problem
- [`KT-37343`](https://youtrack.jetbrains.com/issue/KT-37343) NI: definitely not null types pre-approximation is inconsistent with OI
- [`KT-37380`](https://youtrack.jetbrains.com/issue/KT-37380) NI: broken some code with def not null types due to skip needed constraints
- [`KT-37419`](https://youtrack.jetbrains.com/issue/KT-37419) NI: UNRESOLVED_REFERENCE_WRONG_RECEIVER is reported in case lambda with receiver is returned from `when` expression
- [`KT-37434`](https://youtrack.jetbrains.com/issue/KT-37434) Kotlin/JS, Kotlin/Native: fun interfaces: SAM conversion to Kotlin interface is not compiled with RESOLUTION_TO_CLASSIFIER
- [`KT-37447`](https://youtrack.jetbrains.com/issue/KT-37447) Expression from annotation entry in value parameter inside value parameter should be marked as USED_AS_EXPRESSION
- [`KT-37453`](https://youtrack.jetbrains.com/issue/KT-37453) Type arguments not checked to be empty for candidates with no declared parameters
- [`KT-37488`](https://youtrack.jetbrains.com/issue/KT-37488) [FIR] Incorrect exhaustiveness checking for branches with equals to object that implements sealed class
- [`KT-37497`](https://youtrack.jetbrains.com/issue/KT-37497) NI:  'super' is not an expression, it can not be used as a receiver for extension functions
- [`KT-37530`](https://youtrack.jetbrains.com/issue/KT-37530) NI: instantiation of abstract class via callable reference argument causes run time InstantiationError
- [`KT-37531`](https://youtrack.jetbrains.com/issue/KT-37531) NI: callable reference argument with left hand side type parameter causes frontend exception
- [`KT-37554`](https://youtrack.jetbrains.com/issue/KT-37554) NI: Nothing is inferred incorrectly with elvis return
- [`KT-37579`](https://youtrack.jetbrains.com/issue/KT-37579) NI: inconsistent behaviour with OI around implicit invoke convention after safe call with additional implicit receiver
- [`KT-37604`](https://youtrack.jetbrains.com/issue/KT-37604) "VerifyError: Call to wrong <init> method" in 'invoke' for adapted callable reference to constructor with coercion to Unit
- [`KT-37621`](https://youtrack.jetbrains.com/issue/KT-37621) NI: type variable is inferred to Nothing if the second branch was Nothing and there was upper bound for a type parameter
- [`KT-37626`](https://youtrack.jetbrains.com/issue/KT-37626) NI: builder inference with expected type breaks class references resolution for a class with parameters
- [`KT-37627`](https://youtrack.jetbrains.com/issue/KT-37627) NI: wrong order of the type variable fixation (Nothing? against a call with lambda)
- [`KT-37628`](https://youtrack.jetbrains.com/issue/KT-37628) NI: wrong approximation of type argument to star projection during common super type calculation
- [`KT-37644`](https://youtrack.jetbrains.com/issue/KT-37644) NI: appeared exception during incorporation of a captured type into a type variable for elvis resolve
- [`KT-37650`](https://youtrack.jetbrains.com/issue/KT-37650) NI: it's impossible to infer a type variable with the participation of a wrapped covariant type
- [`KT-37718`](https://youtrack.jetbrains.com/issue/KT-37718) False positive unused parameter for @JvmStatic main function in object
- [`KT-37779`](https://youtrack.jetbrains.com/issue/KT-37779) ClassCastException: Named argument without spread operator for vararg parameter causes code to crash on runtime
- [`KT-37832`](https://youtrack.jetbrains.com/issue/KT-37832) In MPP, subtypes of types defined in legacy libraries, like stdlib, cannot properly resolve on the consumer side receviing both
- [`KT-37861`](https://youtrack.jetbrains.com/issue/KT-37861) Capturing an outer class instance in a default parameter of inner class constructor causes VerifyError
- [`KT-37986`](https://youtrack.jetbrains.com/issue/KT-37986) Return value of function reference returning inline class mapped to 'java.lang.Object' is not boxed properly
- [`KT-37998`](https://youtrack.jetbrains.com/issue/KT-37998) '!!' operator on safe call of function returning inline class value causes CCE at runtime
- [`KT-38042`](https://youtrack.jetbrains.com/issue/KT-38042) Allow kotlin.Result as a return type only if one enabled inline classes explicitly
- [`KT-38134`](https://youtrack.jetbrains.com/issue/KT-38134) NI: Type mismatch with a star projection and `UnsafeVariance`
- [`KT-38298`](https://youtrack.jetbrains.com/issue/KT-38298) Inconsistent choice of candidate when both expect/actual are available (affects only `enableGranularSourceSetMetadata`)
- [`KT-38661`](https://youtrack.jetbrains.com/issue/KT-38661) NI: "Cannot infer type variable TypeVariable" with lambda with receiver
- [`KT-38668`](https://youtrack.jetbrains.com/issue/KT-38668) Project with module dependency in KN, build fails with Kotlin 1.3.71 and associated libs but passes with 1.3.61.
- [`KT-38857`](https://youtrack.jetbrains.com/issue/KT-38857) Class versions V1_5 or less must use F_NEW frames.
- [`KT-39113`](https://youtrack.jetbrains.com/issue/KT-39113) "AssertionError: Uninitialized value on stack" with EXACTLY_ONCE contract in non-inline function and lambda destructuring
- [`KT-28483`](https://youtrack.jetbrains.com/issue/KT-28483) Override of generic-return-typed function with inline class should lead to a boxing
- [`KT-37963`](https://youtrack.jetbrains.com/issue/KT-37963) ClassCastException: Value of inline class represented as 'java.lang.Object' is not boxed properly on return from lambda

### Docs & Examples

- [`KT-35231`](https://youtrack.jetbrains.com/issue/KT-35231) toMutableList documentation is vague

### IDE

#### Performance Improvements

- [`KT-30541`](https://youtrack.jetbrains.com/issue/KT-30541) EDT Freeze after new Kotlin Script creation
- [`KT-35050`](https://youtrack.jetbrains.com/issue/KT-35050) Significant freezes due to findSdkAcrossDependencies()
- [`KT-37301`](https://youtrack.jetbrains.com/issue/KT-37301) Freeze when "Optimize Imports" in KotlinImportOptimizer
- [`KT-37466`](https://youtrack.jetbrains.com/issue/KT-37466) Invalidate partialBodyResolveCache on OCB
- [`KT-37467`](https://youtrack.jetbrains.com/issue/KT-37467) PerFileAnalysisCache.fetchAnalysisResults
- [`KT-37993`](https://youtrack.jetbrains.com/issue/KT-37993) Do not resolve references if paste code is located in the same origin
- [`KT-38318`](https://youtrack.jetbrains.com/issue/KT-38318) Freezes in IDEA

#### Fixes

- [`KT-27935`](https://youtrack.jetbrains.com/issue/KT-27935) Functional typealias with typealias in type parameters causes UnsupportedOperationException in TypeSignatureMappingKt (IDEA analysis)
- [`KT-31668`](https://youtrack.jetbrains.com/issue/KT-31668) Complete statement for class declaration: add '()' to supertype
- [`KT-33473`](https://youtrack.jetbrains.com/issue/KT-33473) UAST: References to local variable are resolved to UastKotlinPsiVariable
- [`KT-34564`](https://youtrack.jetbrains.com/issue/KT-34564) Kotlin USimpleNameReferenceExpression for annotation parameter resolves to null for compiled Kotlin classes
- [`KT-34973`](https://youtrack.jetbrains.com/issue/KT-34973) Light class incorrectly claiming ambiguous method call from Java when one overload is synthetic
- [`KT-35801`](https://youtrack.jetbrains.com/issue/KT-35801) UAST: UnknownKotlinExpression for valid Kotlin annotated expression
- [`KT-35804`](https://youtrack.jetbrains.com/issue/KT-35804) UAST: Annotations missing from catch clause parameters
- [`KT-35848`](https://youtrack.jetbrains.com/issue/KT-35848) UAST:  ClassCastException when trying to invoke UElement for some wrapped PsiElements
- [`KT-36156`](https://youtrack.jetbrains.com/issue/KT-36156) Kotlin annotation attributes have blue color whereas white in Java
- [`KT-36275`](https://youtrack.jetbrains.com/issue/KT-36275) UAST: UCallExpression::resolve returns null for local function calls
- [`KT-36717`](https://youtrack.jetbrains.com/issue/KT-36717) Fix failing light class tests after switching plugin to language version 1.4
- [`KT-36877`](https://youtrack.jetbrains.com/issue/KT-36877) Message bundles for copy paste are missed in 201
- [`KT-36907`](https://youtrack.jetbrains.com/issue/KT-36907) IDE: `-Xuse-ir` setting on facet level does not affect highlighting
- [`KT-37133`](https://youtrack.jetbrains.com/issue/KT-37133) UAST: Annotating assignment expression sometimes leads to UnknownKotlinExpression
- [`KT-37312`](https://youtrack.jetbrains.com/issue/KT-37312) "Implement members" intention put function in the primary constructor if there are unused brackets in class
- [`KT-37613`](https://youtrack.jetbrains.com/issue/KT-37613) Uast: no parameters in reified method
- [`KT-37933`](https://youtrack.jetbrains.com/issue/KT-37933) Rare NPE in ProjectRootsUtilKt.isKotlinBinary [easy fix]
- [`KT-38081`](https://youtrack.jetbrains.com/issue/KT-38081) Configure kotlin in project produces IDE error "heavy operation and should not be call on AWT thread"
- [`KT-38354`](https://youtrack.jetbrains.com/issue/KT-38354) HMPP. IDE. Dependency leakage from leaf native to shared native module
- [`KT-38634`](https://youtrack.jetbrains.com/issue/KT-38634) IDE: Error on opening MPP project in 1.3.72 after opening it in 1.4-M2

### IDE. Code Style, Formatting

- [`KT-37870`](https://youtrack.jetbrains.com/issue/KT-37870) "Remove trailing comma" action stops working after applying and cancelling it

### IDE. Completion

- [`KT-36808`](https://youtrack.jetbrains.com/issue/KT-36808) Delete Flow.collect from autocompletion list or make it least prioritized
- [`KT-36860`](https://youtrack.jetbrains.com/issue/KT-36860) Provide convenient completion of extension functions from objects
- [`KT-37395`](https://youtrack.jetbrains.com/issue/KT-37395) Invalid callable reference completion of member extension

### IDE. Debugger

- [`KT-34906`](https://youtrack.jetbrains.com/issue/KT-34906) Implement Coroutine Debugger
- [`KT-35392`](https://youtrack.jetbrains.com/issue/KT-35392) Debugger omits meaningful part of the stacktrace even with disabled filter
- [`KT-36215`](https://youtrack.jetbrains.com/issue/KT-36215) Coroutines debugger tab is empty in Android Studio
- [`KT-37238`](https://youtrack.jetbrains.com/issue/KT-37238) Coroutines Debugger: dump creation fails every time
- [`KT-38047`](https://youtrack.jetbrains.com/issue/KT-38047) Coroutines Debugger: Assertion failed: “Should be invoked in manager thread, use DebuggerManagerThreadImpl” on moving to source code from suspended coroutine in project without debugger jar in classpath
- [`KT-38049`](https://youtrack.jetbrains.com/issue/KT-38049) Coroutines Debugger: NPE “null cannot be cast to non-null type com.sun.jdi.ObjectReference” is thrown by calling dumpCoroutines
- [`KT-38487`](https://youtrack.jetbrains.com/issue/KT-38487) Any Field Watch interaction causes a MissingResourceException

### IDE. Decompiler, Indexing, Stubs

- [`KT-37896`](https://youtrack.jetbrains.com/issue/KT-37896) IAE: "Argument for @NotNull parameter 'file' of IndexTodoCacheManagerImpl.getTodoCount must not be null" through KotlinTodoSearcher.processQuery()

### IDE. Gradle Integration

- [`KT-33809`](https://youtrack.jetbrains.com/issue/KT-33809) With `kotlin.mpp.enableGranularSourceSetsMetadata=true`, IDE misses dependsOn-relation between kotlin and android sourceSets, leading to issues with expect/actual matching
- [`KT-36354`](https://youtrack.jetbrains.com/issue/KT-36354) IDE: Gradle import from non-JVM projects: dependency to output artifact is created instead of module dependency
- [`KT-38037`](https://youtrack.jetbrains.com/issue/KT-38037) UnsupportedOperationException on sync gradle Kotlin project with at least two multiplatform modules

### IDE. Gradle. Script

- [`KT-36763`](https://youtrack.jetbrains.com/issue/KT-36763) Drop modification stamp for scripts after project import
- [`KT-37237`](https://youtrack.jetbrains.com/issue/KT-37237) Script configurations should be loaded during project import in case of errors
- [`KT-38041`](https://youtrack.jetbrains.com/issue/KT-38041) Do not request for script configuration after VCS update

### IDE. Inspections and Intentions

#### New Features

- [`KT-3262`](https://youtrack.jetbrains.com/issue/KT-3262) Inspection "Inner class could be nested"
- [`KT-15723`](https://youtrack.jetbrains.com/issue/KT-15723) Add 'Convert to value' quickfix for property containing only getter
- [`KT-34026`](https://youtrack.jetbrains.com/issue/KT-34026) Add "Remove argument" quick fix for redundant argument in constructor call
- [`KT-34332`](https://youtrack.jetbrains.com/issue/KT-34332) Add "Remove argument" quick fix for redundant argument in function call
- [`KT-34450`](https://youtrack.jetbrains.com/issue/KT-34450) `Convert function to property` intention should be also displayed on `fun` keyword
- [`KT-34593`](https://youtrack.jetbrains.com/issue/KT-34593) Invert 'if' condition: Invert `String.isNotEmpty` should be `String.isEmpty`
- [`KT-34819`](https://youtrack.jetbrains.com/issue/KT-34819) Inspection: report useless elvis "?: return null"
- [`KT-37849`](https://youtrack.jetbrains.com/issue/KT-37849) Support `ReplaceWith` for supertypes call

#### Performance Improvements

- [`KT-37515`](https://youtrack.jetbrains.com/issue/KT-37515) Deadlock

#### Fixes

- [`KT-12329`](https://youtrack.jetbrains.com/issue/KT-12329) "invert if" inserts unnecessary 'continue' for statement inside a loop with 'continue'
- [`KT-17615`](https://youtrack.jetbrains.com/issue/KT-17615) "Convert parameter to receiver" changes `this` to `this@ < no name provided >`
- [`KT-20868`](https://youtrack.jetbrains.com/issue/KT-20868) IntelliJ says method from anonymous inner class with inferred interface type is not used even though it is
- [`KT-20907`](https://youtrack.jetbrains.com/issue/KT-20907) Secondary constructor is marked as unused by IDE when called by typealias 
- [`KT-22368`](https://youtrack.jetbrains.com/issue/KT-22368) "Convert to block body" intention incorrectly formats closing brace
- [`KT-23510`](https://youtrack.jetbrains.com/issue/KT-23510) "Remove parameter" quick fix keeps lambda argument when it's out of parentheses
- [`KT-27601`](https://youtrack.jetbrains.com/issue/KT-27601) False positive "Unused import directive" for extension function used in KDoc
- [`KT-28085`](https://youtrack.jetbrains.com/issue/KT-28085) "Convert receiver to parameter" introduces incorrect this@class in lambda
- [`KT-30028`](https://youtrack.jetbrains.com/issue/KT-30028) "Convert parameter to receiver" introduces wrong 'this' qualifier for extension lambda receiver
- [`KT-31601`](https://youtrack.jetbrains.com/issue/KT-31601) "Remove redundant let call" changes semantics by introducing multiple safe calls
- [`KT-31800`](https://youtrack.jetbrains.com/issue/KT-31800) False positive "never used" with function in private val object expression
- [`KT-31912`](https://youtrack.jetbrains.com/issue/KT-31912) QF “Convert to anonymous object” do nothing on SAM-interfaces
- [`KT-32561`](https://youtrack.jetbrains.com/issue/KT-32561) "Property can be declared in constructor" causes another warning
- [`KT-32809`](https://youtrack.jetbrains.com/issue/KT-32809) Convert parameter to receiver inserts wrong qualifiers for this (when nothing needs to be changed)
- [`KT-34371`](https://youtrack.jetbrains.com/issue/KT-34371) "Surround with lambda" quickfix is not available for suspend lambda parameters.
- [`KT-34640`](https://youtrack.jetbrains.com/issue/KT-34640) Replace 'if' with 'when' leads to copy comment line above when from another if
- [`KT-36225`](https://youtrack.jetbrains.com/issue/KT-36225) KNPE: CodeInliner.processTypeParameterUsages with `ReplaceWith` for inline reified generic function
- [`KT-36266`](https://youtrack.jetbrains.com/issue/KT-36266) NPE when invoking Lift return out of if/when after intention becomes inapplicable but still beeing shown
- [`KT-36296`](https://youtrack.jetbrains.com/issue/KT-36296) False negative "Redundant SAM-constructor" with multiple SAM arguments
- [`KT-36367`](https://youtrack.jetbrains.com/issue/KT-36367) False negative "Redundant SAM-constructor" for kotlin functions
- [`KT-36368`](https://youtrack.jetbrains.com/issue/KT-36368) False negative "Redundant SAM-constructor" for fun interfaces in kotlin
- [`KT-36395`](https://youtrack.jetbrains.com/issue/KT-36395) False positive "Redundant SAM-constructor" with two java interfaces extending one another
- [`KT-36411`](https://youtrack.jetbrains.com/issue/KT-36411) "Put parameters on separate lines" and "Put parameters on one line" actions do not respect trailing comma
- [`KT-36482`](https://youtrack.jetbrains.com/issue/KT-36482) "Add JvmOverloads annotation" intention is still suggested for annotation's parameters
- [`KT-36686`](https://youtrack.jetbrains.com/issue/KT-36686) Implement members quickfix puts the implementation *before* the data class if it already has a body
- [`KT-36685`](https://youtrack.jetbrains.com/issue/KT-36685) "Convert to a range check" transform hex range to int if it is compared with "Less" or "Greater"
- [`KT-36707`](https://youtrack.jetbrains.com/issue/KT-36707) False positive redundant companion object on calling companion object members
- [`KT-36735`](https://youtrack.jetbrains.com/issue/KT-36735) Inspection 'Replace 'toString' with string template' miss curly braces and generates wrong code for constructor calls
- [`KT-36834`](https://youtrack.jetbrains.com/issue/KT-36834) Convert use-site targets and usages with convert property to fun intention
- [`KT-37213`](https://youtrack.jetbrains.com/issue/KT-37213) "Move to top level" intention does not update imports for extension functions
- [`KT-37496`](https://youtrack.jetbrains.com/issue/KT-37496) False positive "Remove redundant backticks" for multiple underscores variable name
- [`KT-37502`](https://youtrack.jetbrains.com/issue/KT-37502) False positive "redundant lambda arrow" with inline generic function with reified type in object and anonymous parameter name
- [`KT-37508`](https://youtrack.jetbrains.com/issue/KT-37508) "Convert receiver to parameter" breaks code in anonymous objects (this@ < no name provided >)
- [`KT-37576`](https://youtrack.jetbrains.com/issue/KT-37576) Kotlin InspectionSuppressor not being called for the kotlin's inspections
- [`KT-37749`](https://youtrack.jetbrains.com/issue/KT-37749) "Convert to anonymous object" intention is suggested for Java SAM conversion, but not for Kotlin
- [`KT-37781`](https://youtrack.jetbrains.com/issue/KT-37781) "Add modifier" intention/quickfix works incorrectly with functional interfaces
- [`KT-37893`](https://youtrack.jetbrains.com/issue/KT-37893) i18n: Incorrect quickfix name "Lift return out of '"

### IDE. KDoc

- [`KT-37361`](https://youtrack.jetbrains.com/issue/KT-37361) Support for showing rendered doc comments in editor

### IDE. Libraries

- [`KT-36276`](https://youtrack.jetbrains.com/issue/KT-36276) IDE: references to declarations in JavaScript KLib dependency are unresolved
- [`KT-37562`](https://youtrack.jetbrains.com/issue/KT-37562) IDE: references to JavaScript KLib dependency are unresolved, when project and library are compiled with "both" mode

### IDE. Navigation

- [`KT-18472`](https://youtrack.jetbrains.com/issue/KT-18472) UI lockup on find usages
- [`KT-18619`](https://youtrack.jetbrains.com/issue/KT-18619) Find Usages of element used via import alias does not show actual usage location
- [`KT-34088`](https://youtrack.jetbrains.com/issue/KT-34088) Navigate | Implementations action doesn't show implementations of Java methods in Kotlin files if method has parameters referring to generic type
- [`KT-35006`](https://youtrack.jetbrains.com/issue/KT-35006) IDE: "Navigate to inline function call site" from stack trace for nested inline call navigates to outer inline call
- [`KT-36138`](https://youtrack.jetbrains.com/issue/KT-36138) 628 second freeze when doing Find Usages on data class property
- [`KT-36218`](https://youtrack.jetbrains.com/issue/KT-36218) Show Kotlin file members in navigation bar
- [`KT-37494`](https://youtrack.jetbrains.com/issue/KT-37494) AnnotatedElementsSearch unable to find annotated property accessor

### IDE. Project View

- [`KT-32886`](https://youtrack.jetbrains.com/issue/KT-32886) Project tool window: Show Visibility Icons does nothing for Kotlin classes
- [`KT-37632`](https://youtrack.jetbrains.com/issue/KT-37632) IDE error on project structure opening

### IDE. Refactorings

#### Performance Improvements

- [`KT-37801`](https://youtrack.jetbrains.com/issue/KT-37801) Renaming private property with common name is very slow

#### Fixes

- [`KT-22733`](https://youtrack.jetbrains.com/issue/KT-22733) Refactor / Inline Function: fun with type parameter: KNPE at CodeInliner.processTypeParameterUsages()
- [`KT-27389`](https://youtrack.jetbrains.com/issue/KT-27389) MPP: Refactoring "Move Class" does not change the package declaration
- [`KT-29870`](https://youtrack.jetbrains.com/issue/KT-29870) Inline variable doesn't handle 'when' subject val correctly
- [`KT-33045`](https://youtrack.jetbrains.com/issue/KT-33045) Cover Move Refactoring by statistics (FUS)
- [`KT-36071`](https://youtrack.jetbrains.com/issue/KT-36071) Refactoring: Move top declaration implementation refactoring
- [`KT-36072`](https://youtrack.jetbrains.com/issue/KT-36072) Empty files are removed on Refactor/Move action with turned off "Delete empty source files" option
- [`KT-36114`](https://youtrack.jetbrains.com/issue/KT-36114) java.lang.NoClassDefFoundError exception on Refactor/Move of kotlin function if it is referenced in java
- [`KT-36129`](https://youtrack.jetbrains.com/issue/KT-36129) java.lang.Throwable: Invalid file exception occurs on Refactor/Move of class from kotlin script
- [`KT-36382`](https://youtrack.jetbrains.com/issue/KT-36382) Move file refactoring breaks ktor application config
- [`KT-36504`](https://youtrack.jetbrains.com/issue/KT-36504) "Extract property" suggests potentially invalid name for new property
- [`KT-37637`](https://youtrack.jetbrains.com/issue/KT-37637) KotlinChangeSignatureUsageProcessor broke Change Signature in Python plugin
- [`KT-37797`](https://youtrack.jetbrains.com/issue/KT-37797) Useless "Value for new paramater" step in 'Update usages to reflect signature changes' for method with default parameter value
- [`KT-37822`](https://youtrack.jetbrains.com/issue/KT-37822) Improve message "Inline all references and remove the kind"
- [`KT-38348`](https://youtrack.jetbrains.com/issue/KT-38348) UL methods return signature without generic type parameters
- [`KT-38527`](https://youtrack.jetbrains.com/issue/KT-38527) Move nested class to upper level fails silently: MissingResourceException

### IDE. Script

- [`KT-37765`](https://youtrack.jetbrains.com/issue/KT-37765) NCDFE KJvmCompiledModuleInMemory on running `*.main.kts` script

### IDE. Tests Support

- [`KT-36716`](https://youtrack.jetbrains.com/issue/KT-36716) With `kotlin.gradle.testing.enabled=true`, gradle console output gets extra ijLog messages
- [`KT-36910`](https://youtrack.jetbrains.com/issue/KT-36910) There are no Run/Debug actions in context menu for non-JVM platform-specific test results
- [`KT-37037`](https://youtrack.jetbrains.com/issue/KT-37037) [JS, Debug] Node.JS test debug doesn't stop on breakpoints

### IDE. Wizards

#### New Features

- [`KT-36150`](https://youtrack.jetbrains.com/issue/KT-36150) New Project Wizard: provide a way to connect with "dependsOn" relation the added project modules
- [`KT-36179`](https://youtrack.jetbrains.com/issue/KT-36179) New Project Wizard: it's impossible to make a JVM target friendly to Java code in a multiplatform project

#### Fixes

- [`KT-35583`](https://youtrack.jetbrains.com/issue/KT-35583) New Project wizard: don't suggest build systems which cannot be used
- [`KT-35585`](https://youtrack.jetbrains.com/issue/KT-35585) New Project wizard: remember choices which have sense for many projects
- [`KT-35691`](https://youtrack.jetbrains.com/issue/KT-35691) New Project wizard: artifact and group values are effectively ignored
- [`KT-35693`](https://youtrack.jetbrains.com/issue/KT-35693) New Project wizard creates pom.xml / build.gradle referring to release Kotlin version only
- [`KT-36136`](https://youtrack.jetbrains.com/issue/KT-36136) New Project Wizard: generated projects are missing m2 Gradle repository
- [`KT-36137`](https://youtrack.jetbrains.com/issue/KT-36137) New Project Wizard: "multiplatform" shall be written as a single word, without the capital P in the middle
- [`KT-36155`](https://youtrack.jetbrains.com/issue/KT-36155) New Project Wizard: show warning "Multiplatform project cannot be generated" only for MPP projects
- [`KT-36162`](https://youtrack.jetbrains.com/issue/KT-36162) New Project Wizard: make the error messages in modules editor actionable
- [`KT-36163`](https://youtrack.jetbrains.com/issue/KT-36163) New Project Wizard: remove trailing spaces in Android SDK Path automatically
- [`KT-36166`](https://youtrack.jetbrains.com/issue/KT-36166) New Project Wizard: addition of Android target into a multiplatform project doesn't add a necessary minimal Android configuration
- [`KT-36169`](https://youtrack.jetbrains.com/issue/KT-36169) New Project Wizard: Android-related projects failed to build
- [`KT-36176`](https://youtrack.jetbrains.com/issue/KT-36176) New Project Wizard: module templates doesn't work for multiplatform projects
- [`KT-36177`](https://youtrack.jetbrains.com/issue/KT-36177) New Project Wizard: it's impossible to add more than one target of JVM kind to a multiplatform project
- [`KT-36180`](https://youtrack.jetbrains.com/issue/KT-36180) New Project Wizard: it's impossible to set target JVM version for a JVM module
- [`KT-36226`](https://youtrack.jetbrains.com/issue/KT-36226) New Project Wizard: add Mobile Android/iOS project template
- [`KT-36267`](https://youtrack.jetbrains.com/issue/KT-36267) New Project Wizard: flatten JVM targets list for multiplatform projects
- [`KT-36328`](https://youtrack.jetbrains.com/issue/KT-36328) New Project wizard fails for certain templates with AE: "Wrong line separators" at KotlinFormattingModelBuilder.createModel()
- [`KT-37599`](https://youtrack.jetbrains.com/issue/KT-37599) New Project Wizard: Open Kotlin Wizard via hyperlink
- [`KT-37667`](https://youtrack.jetbrains.com/issue/KT-37667) New project wizard: implement new UI design
- [`KT-37674`](https://youtrack.jetbrains.com/issue/KT-37674) Kotlin version in build files includes the IDEA version
- [`KT-38061`](https://youtrack.jetbrains.com/issue/KT-38061) New Project wizard 1.4: do not allow choosing build system if corresponding IJ plugin is disabled
- [`KT-38567`](https://youtrack.jetbrains.com/issue/KT-38567) New Project wizard 1.4+: Improve processing case when project with required path already exists
- [`KT-38579`](https://youtrack.jetbrains.com/issue/KT-38579) New Project wizard 1.4+: multiplatform mobile application: build fails on lint task: Configuration with name 'compileClasspath' not found
- [`KT-38929`](https://youtrack.jetbrains.com/issue/KT-38929) New project wizard: update libraries in project template according to kotlin IDE plugin version
- [`KT-38417`](https://youtrack.jetbrains.com/issue/KT-38417) Enable new project wizard by-default
- [`KT-38158`](https://youtrack.jetbrains.com/issue/KT-38158) java.lang.NullPointerException when try to create new project via standard wizard on Mac os

### JS. Tools

- [`KT-36484`](https://youtrack.jetbrains.com/issue/KT-36484) KotlinJS, MPP: Compilation throws "TypeError: b is not a function" only in production mode

### JavaScript

- [`KT-31126`](https://youtrack.jetbrains.com/issue/KT-31126) Invalid JS constructor call (primary ordinary -> secondary external)
- [`KT-35966`](https://youtrack.jetbrains.com/issue/KT-35966) Make @JsExport annotation usable in common code
- [`KT-37128`](https://youtrack.jetbrains.com/issue/KT-37128) KJS: StackOverflowException when using reified recursive bound for type parameter
- [`KT-37163`](https://youtrack.jetbrains.com/issue/KT-37163) KJS: NullPointerException on using intersection type as a reified one
- [`KT-37418`](https://youtrack.jetbrains.com/issue/KT-37418) Support `AssociatedObjectKey` and `findAssociatedObject` in JS IR BE

### Libraries

#### New Features

- [`KT-8658`](https://youtrack.jetbrains.com/issue/KT-8658) Add property delegates which call get/set on the given KProperty instance, e.g. a property reference
- [`KT-12448`](https://youtrack.jetbrains.com/issue/KT-12448) Make `@Suppress` applicable for type parameters
- [`KT-22932`](https://youtrack.jetbrains.com/issue/KT-22932) String.format should support null locale
- [`KT-23514`](https://youtrack.jetbrains.com/issue/KT-23514) assertFailsWith should link unexpected exception as cause
- [`KT-23737`](https://youtrack.jetbrains.com/issue/KT-23737) JS & MPP: Support exception cause and addSuppressed
- [`KT-25651`](https://youtrack.jetbrains.com/issue/KT-25651) Add shuffle() to Array<T>, ByteArray, IntArray, etc to match MutableList
- [`KT-26494`](https://youtrack.jetbrains.com/issue/KT-26494) Create an interface with provideDelegate()
- [`KT-27729`](https://youtrack.jetbrains.com/issue/KT-27729) Inherit `ReadWriteProperty` from `ReadOnlyProperty`
- [`KT-28290`](https://youtrack.jetbrains.com/issue/KT-28290) Add the onEach extension function to the Array
- [`KT-29182`](https://youtrack.jetbrains.com/issue/KT-29182) SIZE_BYTES/BITS for Float and Double
- [`KT-30372`](https://youtrack.jetbrains.com/issue/KT-30372) Add associateWith to Array<T>
- [`KT-33906`](https://youtrack.jetbrains.com/issue/KT-33906) Add vararg overloads for maxOf/minOf functions
- [`KT-34161`](https://youtrack.jetbrains.com/issue/KT-34161) Array.contentEquals/contentHashCode/contentToString should allow null array receiver and argument
- [`KT-35851`](https://youtrack.jetbrains.com/issue/KT-35851) Add setOfNotNull function
- [`KT-36866`](https://youtrack.jetbrains.com/issue/KT-36866) reduceIndexedOrNull
- [`KT-36955`](https://youtrack.jetbrains.com/issue/KT-36955) stdlib: Reverse range and sortDescending range
- [`KT-37161`](https://youtrack.jetbrains.com/issue/KT-37161) Add #onEachIndexed similar to #forEachIndexed
- [`KT-37603`](https://youtrack.jetbrains.com/issue/KT-37603) Throwable.stackTraceToString: string with detailed information about exception
- [`KT-37751`](https://youtrack.jetbrains.com/issue/KT-37751) Implement shuffled() method Sequences
- [`KT-37804`](https://youtrack.jetbrains.com/issue/KT-37804) Add 'fail' in kotlin-test that allows to specify cause
- [`KT-37839`](https://youtrack.jetbrains.com/issue/KT-37839) StringBuilder.appendLine in stdlib-common
- [`KT-37910`](https://youtrack.jetbrains.com/issue/KT-37910) Support Media Source Extension (MSE) and Encrypted Media Extensions (EME) in Kotlin/Js
- [`KT-38044`](https://youtrack.jetbrains.com/issue/KT-38044) Common Throwable.printStackTrace

#### Performance Improvements

- [`KT-37416`](https://youtrack.jetbrains.com/issue/KT-37416) readLine() is very slow

#### Fixes

- [`KT-13887`](https://youtrack.jetbrains.com/issue/KT-13887) Double/Float companion values such as NaN should be constants
- [`KT-14119`](https://youtrack.jetbrains.com/issue/KT-14119)  `String.toBoolean()` should be `String?.toBoolean()`
- [`KT-16529`](https://youtrack.jetbrains.com/issue/KT-16529) Names of KProperty's type parameters are inconsistent with ReadOnlyProperty/ReadWriteProperty
- [`KT-36356`](https://youtrack.jetbrains.com/issue/KT-36356) Specify which element Iterable.distinctBy(selector) retains
- [`KT-38060`](https://youtrack.jetbrains.com/issue/KT-38060) runningFold and runningReduce instead of scanReduce
- [`KT-38566`](https://youtrack.jetbrains.com/issue/KT-38566) Kotlin/JS IR: kx.serialization & ktor+JsonFeature: SerializationException: Can't locate argument-less serializer for class

### Reflection

- [`KT-29969`](https://youtrack.jetbrains.com/issue/KT-29969) Support optional vararg parameter in `KCallable.callBy`
- [`KT-37707`](https://youtrack.jetbrains.com/issue/KT-37707) "IllegalStateException: superInterface.classLoader must not be null" on class, which implements "AutoCloaseable" interface, "isAccessible" property changing

### Tools. CLI

- [`KT-37090`](https://youtrack.jetbrains.com/issue/KT-37090) file does not exist: `C:\Users\NK\DOWNLO~1\kotlin-compiler-1.3.61\kotlinc\bin\..\lib\kotlin-compiler.jar" from standalone compiler on Windows`

### Tools. Gradle

- [`KT-35447`](https://youtrack.jetbrains.com/issue/KT-35447) Warnings should be piped to stderr when using allWarningsAsErrors = true
- [`KT-35942`](https://youtrack.jetbrains.com/issue/KT-35942) User test Gradle source set code cannot reach out internal members from the production code
- [`KT-36019`](https://youtrack.jetbrains.com/issue/KT-36019) Implement Gradle DSL for explicit API mode

### Tools. Gradle. JS

#### New Features

- [`KT-32017`](https://youtrack.jetbrains.com/issue/KT-32017) Kotlin/JS in MPP: support changing the generated JS file name in Gradle DSL
- [`KT-32721`](https://youtrack.jetbrains.com/issue/KT-32721) [Gradle, JS] CSS Support for browser
- [`KT-36843`](https://youtrack.jetbrains.com/issue/KT-36843) [Gradle, JS, IR] Configure JS Compiler Type through DSL
- [`KT-37207`](https://youtrack.jetbrains.com/issue/KT-37207) Allow to use npm dependency from a local directory
- [`KT-38056`](https://youtrack.jetbrains.com/issue/KT-38056) [Gradle, JS] Group tasks by browser and node

#### Fixes

- [`KT-32466`](https://youtrack.jetbrains.com/issue/KT-32466) kotlinNpmResolve fails in the case of composite build
- [`KT-34468`](https://youtrack.jetbrains.com/issue/KT-34468) Consider custom versions while parsing yarn.lock
- [`KT-36489`](https://youtrack.jetbrains.com/issue/KT-36489) [Gradle, JS, IR]: Correct naming for both compilers
- [`KT-36784`](https://youtrack.jetbrains.com/issue/KT-36784) Kotlin. JS. MPP – Cannot find project :js when using Gradle composite builds
- [`KT-36864`](https://youtrack.jetbrains.com/issue/KT-36864) KJS. Composite build require JS plugin in root project
- [`KT-37240`](https://youtrack.jetbrains.com/issue/KT-37240) KJS. Nondeterministic execution order for webpack scripts (from folder 'webpack.config.d')
- [`KT-37582`](https://youtrack.jetbrains.com/issue/KT-37582) Kotlin/JS: KotlinWebpack non-nullable properties are shown as nullable in Gradle configuration
- [`KT-37587`](https://youtrack.jetbrains.com/issue/KT-37587) KJS. Karma ignore dynamically created webpack patches
- [`KT-37635`](https://youtrack.jetbrains.com/issue/KT-37635) [Gradle, JS] Webpack devtool provide enum for only 2 variants
- [`KT-37636`](https://youtrack.jetbrains.com/issue/KT-37636) [Gradle, JS] Extract package.json from klib
- [`KT-37762`](https://youtrack.jetbrains.com/issue/KT-37762) [Gradle, JS] Actualize Node and Yarn versions in 1.4
- [`KT-37988`](https://youtrack.jetbrains.com/issue/KT-37988) [Gradle, JS] Bump NPM versions on 1.4-M2
- [`KT-38051`](https://youtrack.jetbrains.com/issue/KT-38051) [Gradle, JS] browserDistribution doesn't provide outputs
- [`KT-38519`](https://youtrack.jetbrains.com/issue/KT-38519) JS Compiler per project without additional import

### Tools. Gradle. Multiplatform

- [`KT-36674`](https://youtrack.jetbrains.com/issue/KT-36674) `allMetadataJar` task fails if there is an empty intermediate source set in a multiplatform project with native targets
- [`KT-38746`](https://youtrack.jetbrains.com/issue/KT-38746) In HMPP, compilation of a shared-native source set could be mistakenly disabled
- [`KT-39094`](https://youtrack.jetbrains.com/issue/KT-39094) Provide a way to pass custom JVM args to commonizer from Gradle

### Tools. Gradle. Native

- [`KT-25887`](https://youtrack.jetbrains.com/issue/KT-25887) Kotlin Native gradle build fail with `endorsed is not supported. Endorsed standards and standalone APIs` on jdk > 8 & CLion
- [`KT-36721`](https://youtrack.jetbrains.com/issue/KT-36721) Deduce a fully qualified unique_name in klib manifest from something like group name
- [`KT-37730`](https://youtrack.jetbrains.com/issue/KT-37730) Native part of multiplatform build fails with "unresolved reference" errors if there is a local and external module with the same name
- [`KT-38174`](https://youtrack.jetbrains.com/issue/KT-38174) Kotlin/Native: Disable platform libraries generation at the user side by default

### Tools. J2K

#### Fixes

- [`KT-34965`](https://youtrack.jetbrains.com/issue/KT-34965) Convert function reference copied from function call
- [`KT-35593`](https://youtrack.jetbrains.com/issue/KT-35593) New J2K: method's names don't change between functions declared in Number.java and Number.kt
- [`KT-35897`](https://youtrack.jetbrains.com/issue/KT-35897) J2K converts private enum constructors to internal constructors and produces NON_PRIVATE_CONSTRUCTOR_IN_ENUM error
- [`KT-36088`](https://youtrack.jetbrains.com/issue/KT-36088) J2K:  StackOverflowError when trying to convert Java class with recursive type bound
- [`KT-36149`](https://youtrack.jetbrains.com/issue/KT-36149) J2K: PsiInvalidElementAccessException: Element class com.intellij.psi.impl.source.tree.CompositeElement of type DOT_QUALIFIED_EXPRESSION
- [`KT-36152`](https://youtrack.jetbrains.com/issue/KT-36152) J2K: RuntimeException: Couldn't get containingKtFile for ktElement
- [`KT-36159`](https://youtrack.jetbrains.com/issue/KT-36159) J2K: ClassCastException if constructor contains a super() call and class extends from Kotlin class
- [`KT-36190`](https://youtrack.jetbrains.com/issue/KT-36190) J2K: Wrong property name generation when getter for non-boolean value starts with 'is'
- [`KT-36891`](https://youtrack.jetbrains.com/issue/KT-36891) j2k: Fail with java.lang.NoClassDefFoundError when converting array
- [`KT-37052`](https://youtrack.jetbrains.com/issue/KT-37052) new J2K: Java private function is converted to `private open` top-level function
- [`KT-37620`](https://youtrack.jetbrains.com/issue/KT-37620) new J2K: IndexOutOfBoundsException (DefaultArgumentsConversion.applyToElement) with overloaded function with vararg parameter
- [`KT-37919`](https://youtrack.jetbrains.com/issue/KT-37919) new J2K: Redundant line feeds when converting function

### Tools. JPS

- [`KT-37159`](https://youtrack.jetbrains.com/issue/KT-37159) A Typo (forgotten space) in build output in Circular dependencies warning description

### Tools. Scripts

- [`KT-30086`](https://youtrack.jetbrains.com/issue/KT-30086) ThreadDeath when running kotlin scripts using jsr223
- [`KT-37558`](https://youtrack.jetbrains.com/issue/KT-37558) Scripts: implicit receivers don't work correctly when using CompiledScriptJarsCache
- [`KT-37823`](https://youtrack.jetbrains.com/issue/KT-37823) Consecutive invocations of main.kts throw a KotlinReflectionNotSupportedError
