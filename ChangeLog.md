## 2.0.20-RC

### Analysis. API

- [`KT-69630`](https://youtrack.jetbrains.com/issue/KT-69630) KAPT User project builds with KAPT4 enabled fail with Metaspace overflow

### Backend. Wasm

- [`KT-69876`](https://youtrack.jetbrains.com/issue/KT-69876) K2 Compile exception: Only IrBlockBody together with kotlinx serialization
- [`KT-69529`](https://youtrack.jetbrains.com/issue/KT-69529) compileProductionExecutableKotlinWasmJs FAILED: No such value argument slot in IrConstructorCallImpl: 1 (total=1)
- [`KT-68088`](https://youtrack.jetbrains.com/issue/KT-68088) Wasm: "UNREACHABLE executed at Precompute.cpp:838" running gradle task wasmJsBrowserDistribution for compose multiplatform on Windows

### Compiler

- [`KT-69494`](https://youtrack.jetbrains.com/issue/KT-69494) StackOverflowError in CfgTraverserKt.getPreviousCfgNodes
- [`KT-69723`](https://youtrack.jetbrains.com/issue/KT-69723) K2: code analysis taking too long
- [`KT-56880`](https://youtrack.jetbrains.com/issue/KT-56880) K2. Conflicting overloads for main() isn't shown when language version is set to 2.0
- [`KT-69170`](https://youtrack.jetbrains.com/issue/KT-69170) K2: "Unresolved reference" caused by generics and fun interfaces
- [`KT-70039`](https://youtrack.jetbrains.com/issue/KT-70039) K2: inconsistent stability of vals of captured receivers
- [`KT-68996`](https://youtrack.jetbrains.com/issue/KT-68996) K2: "Not enough information to infer type argument" caused by typealias annotation with fixed generic argument
- [`KT-68889`](https://youtrack.jetbrains.com/issue/KT-68889) K2: type variable should not be fixed
- [`KT-15388`](https://youtrack.jetbrains.com/issue/KT-15388) Forbid delegated property to have external getter/setter

### Compose compiler

- [`b/351858979`](https://issuetracker.google.com/issues/351858979) Fix stability inferencing of interfaces on incremental compilation
- [`b/346821372`](https://issuetracker.google.com/issues/346821372) [Compose] Fix code generation for group optimization

### JavaScript

- [`KT-69353`](https://youtrack.jetbrains.com/issue/KT-69353) KJS / d.ts: Kotlin does not export base collection classes along with their mutable collection counterparts

### Libraries

- [`KT-68025`](https://youtrack.jetbrains.com/issue/KT-68025) Improve documentation for Hex

### Native. Build Infrastructure

- [`KT-69781`](https://youtrack.jetbrains.com/issue/KT-69781) Kotlin/Native performance tests fail to compile with bitcode

### Native. C and ObjC Import

- [`KT-69094`](https://youtrack.jetbrains.com/issue/KT-69094) LLVM 11 clang: cinterops fail with "_Float16 is not supported on this target"

### Native. Platform Libraries

- [`KT-69382`](https://youtrack.jetbrains.com/issue/KT-69382) LLVM 11 clang: symbol not found when running the linker

### Native. Runtime

- [`KT-68928`](https://youtrack.jetbrains.com/issue/KT-68928) EXC_BREAKPOINT: BUG IN CLIENT OF LIBPLATFORM: Trying to recursively lock an os_unfair_lock

### Tools. CLI

- [`KT-69792`](https://youtrack.jetbrains.com/issue/KT-69792) Add the possibility to disable fast jar fs in K2

### Tools. Gradle

- [`KT-69809`](https://youtrack.jetbrains.com/issue/KT-69809) Compose Gradle Plugin: AGP doesn't override configuration properties like traceMarkersEnabled
- [`KT-68843`](https://youtrack.jetbrains.com/issue/KT-68843) Gradle: Kotlin plugin changes source set 'main' to 'null/main'
- [`KT-69837`](https://youtrack.jetbrains.com/issue/KT-69837) Deprecation warning for file-based IC is issued when the property is set to true, altering the intended meaning of the message

### Tools. Gradle. JS

- [`KT-69805`](https://youtrack.jetbrains.com/issue/KT-69805) YarnSetupTask  does not work for custom downloadBaseUrl

### Tools. Gradle. Multiplatform

- [`KT-69311`](https://youtrack.jetbrains.com/issue/KT-69311) runDebugExecutable task fails with "this.compilation" is null with enabled configuration cache

### Tools. Gradle. Native

- [`KT-69918`](https://youtrack.jetbrains.com/issue/KT-69918) java.lang.NullPointerException: Cannot invoke "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation.getTarget()" because "this.compilation" is null

### Tools. Incremental Compile

- [`KT-69042`](https://youtrack.jetbrains.com/issue/KT-69042) K2: changing a Java constant won't cause Kotlin usages to recompile

### Tools. JPS

- [`KT-69204`](https://youtrack.jetbrains.com/issue/KT-69204) Generate lookups in dumb mode for compatibility with ref index


## 2.0.20-Beta2

### Analysis. API

#### Fixes

- [`KT-65417`](https://youtrack.jetbrains.com/issue/KT-65417) K2 IDE: KTOR false positive expect-actual matching error on enum class because of implicit clone() in non-JVM source sets
- [`KT-68882`](https://youtrack.jetbrains.com/issue/KT-68882) Analysis API: Refactor `KaSymbol`s
- [`KT-68689`](https://youtrack.jetbrains.com/issue/KT-68689) LL API: support analysis from builtins module
- [`KT-67775`](https://youtrack.jetbrains.com/issue/KT-67775) Analysis API: expose only interfaces/abstract classes for the user surface
- [`KT-68009`](https://youtrack.jetbrains.com/issue/KT-68009) K2: lowering transformers of Compose compiler plugin access AbstractFir2IrLazyFunction modality, which results in null point exception
- [`KT-68918`](https://youtrack.jetbrains.com/issue/KT-68918) collectCallCandidates works incorrectly for parenthesis invoke
- [`KT-68462`](https://youtrack.jetbrains.com/issue/KT-68462) Analysis API: Integrate `project-structure` module into `analysis-api` and `analysis-api-platform-interface`
- [`KT-69131`](https://youtrack.jetbrains.com/issue/KT-69131) AA: "provideDelegate" operator is not resolved from the delegation reference in FIR implementation
- [`KT-69055`](https://youtrack.jetbrains.com/issue/KT-69055) Analysis API: Stabilize `KaScope`s
- [`KT-68959`](https://youtrack.jetbrains.com/issue/KT-68959) Introduce KaSeverity
- [`KT-68846`](https://youtrack.jetbrains.com/issue/KT-68846) Mark KaFirReference and all implementations with internal modifier
- [`KT-68845`](https://youtrack.jetbrains.com/issue/KT-68845) Move KaSymbolBasedReference to resolution package
- [`KT-68844`](https://youtrack.jetbrains.com/issue/KT-68844) Move KaTypeProjection to types package
- [`KT-65849`](https://youtrack.jetbrains.com/issue/KT-65849) K2: Rename 'high-level-api' family of JARs to 'analysis-api'
- [`KT-62540`](https://youtrack.jetbrains.com/issue/KT-62540) Remove uses of TypeInfo.fromString and TypeInfo.createTypeText from Kotlin plugin
- [`KT-68155`](https://youtrack.jetbrains.com/issue/KT-68155) Analysis API: Add PSI validity check to `analyze`
- [`KT-62936`](https://youtrack.jetbrains.com/issue/KT-62936) Analysis API: NativeForwardDeclarationsSymbolProvider is not supported for Kotlin/Native

### Analysis. Light Classes

- [`KT-68261`](https://youtrack.jetbrains.com/issue/KT-68261) SLC: Constructors of sealed classes should be private
- [`KT-68696`](https://youtrack.jetbrains.com/issue/KT-68696) Drop `DecompiledPsiDeclarationProvider`-related stuff
- [`KT-68404`](https://youtrack.jetbrains.com/issue/KT-68404) SLC: wrong binary resolution to declaration with `@JvmName`

### Backend. Native. Debug

- [`KT-67567`](https://youtrack.jetbrains.com/issue/KT-67567) Native: after updating to LLVM 16 lldb hangs when smooth stepping

### Backend. Wasm

- [`KT-68828`](https://youtrack.jetbrains.com/issue/KT-68828) Wasm test failure. expect-actual. private constructor in expect

### Compiler

#### New Features

- [`KT-58310`](https://youtrack.jetbrains.com/issue/KT-58310) Consider non-functional type constraints for type variable which is an expected type for lambda argument
- [`KT-57872`](https://youtrack.jetbrains.com/issue/KT-57872) Improve "Public-API inline function cannot access non-public-API" check

#### Fixes

- [`KT-65546`](https://youtrack.jetbrains.com/issue/KT-65546) K2. implement extended checker for unused anonymous parameter in lambda
- [`KT-60445`](https://youtrack.jetbrains.com/issue/KT-60445) K2/Java: investigate possible symbol clash while enhancing Java class type parameter bounds
- [`KT-68358`](https://youtrack.jetbrains.com/issue/KT-68358) `@EnhancedNullability` is missing on value parameter type after inheritance by delegation with strict JSpecify enabled
- [`KT-67791`](https://youtrack.jetbrains.com/issue/KT-67791) False negative "Synchronizing by Meters is forbidden" with inline value classes
- [`KT-69495`](https://youtrack.jetbrains.com/issue/KT-69495) k2: inconsistent output of unsigned number in string templates
- [`KT-67693`](https://youtrack.jetbrains.com/issue/KT-67693) Implement checkers for K1 compiler which will check the usage of K2 new features and report that they are not supported in K1 compiler
- [`KT-44139`](https://youtrack.jetbrains.com/issue/KT-44139) Don't report overload resolution ambiguities if arguments contain an error type
- [`KT-69282`](https://youtrack.jetbrains.com/issue/KT-69282) K2: equality of unsigned types with nullability works incorrectly
- [`KT-69619`](https://youtrack.jetbrains.com/issue/KT-69619) K2. JAVA_TYPE_MISMATCH when Kotlin out generic type used in Java
- [`KT-68996`](https://youtrack.jetbrains.com/issue/KT-68996) K2: "Not enough information to infer type argument" caused by typealias annotation with fixed generic argument
- [`KT-69563`](https://youtrack.jetbrains.com/issue/KT-69563) trying to call `.source` on `FirPackageFragmentDescriptor` results in exception
- [`KT-69611`](https://youtrack.jetbrains.com/issue/KT-69611) Internal annotation FlexibleArrayElementVariance is written to output jar
- [`KT-69463`](https://youtrack.jetbrains.com/issue/KT-69463) K2: false negative SUPER_CALL_WITH_DEFAULT_PARAMETERS  with expect/actual declarations
- [`KT-68556`](https://youtrack.jetbrains.com/issue/KT-68556) K2: false negative PROPERTY_WITH_NO_TYPE_NO_INITIALIZER on uninitialized property without type
- [`KT-68997`](https://youtrack.jetbrains.com/issue/KT-68997) K2: "No accessor found" for an inline value class when query the value of a delegated class by reflection
- [`KT-68724`](https://youtrack.jetbrains.com/issue/KT-68724) K2: "ABSTRACT_MEMBER_NOT_IMPLEMENTED" caused by open modifier on interface
- [`KT-68667`](https://youtrack.jetbrains.com/issue/KT-68667) K2:  Compiler hangs on mapNotNull and elvis inside lambda
- [`KT-68747`](https://youtrack.jetbrains.com/issue/KT-68747) K2: Long compilation time because of constraint solving when using typealias in different modules
- [`KT-68940`](https://youtrack.jetbrains.com/issue/KT-68940) K2: "IllegalArgumentException: All variables should be fixed to something"
- [`KT-69182`](https://youtrack.jetbrains.com/issue/KT-69182) K2: OptIn on enum companion blocks enum constants
- [`KT-69191`](https://youtrack.jetbrains.com/issue/KT-69191) K2: "Unresolved reference" caused by nested data objects
- [`KT-68797`](https://youtrack.jetbrains.com/issue/KT-68797) K2 / Native: "java.lang.IllegalStateException: FIELD" caused by enabled caching
- [`KT-69569`](https://youtrack.jetbrains.com/issue/KT-69569) Wrong paths when one type has multiple annotated arguments
- [`KT-63871`](https://youtrack.jetbrains.com/issue/KT-63871) K2: different value of `isNotDefault ` flag for property inherited from delegate
- [`KT-63828`](https://youtrack.jetbrains.com/issue/KT-63828) K2: Missing `signature` metadata for accessors of properties inherited from delegate
- [`KT-68669`](https://youtrack.jetbrains.com/issue/KT-68669) K2: Generate inherited delegated members after actualization
- [`KT-69402`](https://youtrack.jetbrains.com/issue/KT-69402) FirSupertypeResolverVisitor: ConcurrentModificationException
- [`KT-68449`](https://youtrack.jetbrains.com/issue/KT-68449) K2: "when" expression returns Unit
- [`KT-67072`](https://youtrack.jetbrains.com/issue/KT-67072) K2: inconsistent stability of open vals on receivers of final type
- [`KT-68570`](https://youtrack.jetbrains.com/issue/KT-68570) K2: "Unresolved reference" in call with lambda argument and nested lambda argument
- [`KT-69476`](https://youtrack.jetbrains.com/issue/KT-69476) False negative NO_ELSE_IN_WHEN on when over intersection type with expect enum/sealed class
- [`KT-67069`](https://youtrack.jetbrains.com/issue/KT-67069) K2: Delegated member calls interface method instead of fake override
- [`KT-63864`](https://youtrack.jetbrains.com/issue/KT-63864) K2: Missing abbreviated type in metadata
- [`KT-69421`](https://youtrack.jetbrains.com/issue/KT-69421) K2: Resolve changed from delegated function to java default function
- [`KT-69392`](https://youtrack.jetbrains.com/issue/KT-69392) K2: "UNSAFE_CALL": when with some variable subjects does not smartcast the variable
- [`KT-69159`](https://youtrack.jetbrains.com/issue/KT-69159) K2: KotlinNothingValueException in Exposed
- [`KT-69053`](https://youtrack.jetbrains.com/issue/KT-69053) K2: Unsupported intersection overrides for fields
- [`KT-69227`](https://youtrack.jetbrains.com/issue/KT-69227) K2: "Argument type mismatch" caused by generic typealias and upper bound
- [`KT-31371`](https://youtrack.jetbrains.com/issue/KT-31371) NOT_YET_SUPPORTED_IN_INLINE: incorrect error message for local inline function
- [`KT-49473`](https://youtrack.jetbrains.com/issue/KT-49473) PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR: specialize error message for 'inline' property
- [`KT-49474`](https://youtrack.jetbrains.com/issue/KT-49474) NON_PUBLIC_CALL_FROM_PUBLIC_INLINE: specialize error message for 'inline' property
- [`KT-49503`](https://youtrack.jetbrains.com/issue/KT-49503) SUPER_CALL_FROM_PUBLIC_INLINE_ERROR: specialize error message for 'inline' property
- [`KT-11302`](https://youtrack.jetbrains.com/issue/KT-11302) On inapplicable '`@JvmStatic`' annotation, highlight only the annotation, not the function signature
- [`KT-59510`](https://youtrack.jetbrains.com/issue/KT-59510) K2: do not render annotations in the deprecation diagnostic
- [`KT-68532`](https://youtrack.jetbrains.com/issue/KT-68532) "This code uses error suppression for 'INAPPLICABLE_JVM_NAME'. While it might compile and work, the compiler behavior is UNSPECIFIED and WON'T BE PRESERVED"
- [`KT-68859`](https://youtrack.jetbrains.com/issue/KT-68859) K2: unable to suppress only "JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE"
- [`KT-68623`](https://youtrack.jetbrains.com/issue/KT-68623) K2: "Only safe or null-asserted calls are allowed" on safe call
- [`KT-68364`](https://youtrack.jetbrains.com/issue/KT-68364) JVM: ISE "Bad exception handler end" on a non-local break/continue inside try with finally
- [`KT-68469`](https://youtrack.jetbrains.com/issue/KT-68469) [K2] MISSING_DEPENDENCY_CLASS caused by redundant `@file`:JvmName
- [`KT-68999`](https://youtrack.jetbrains.com/issue/KT-68999) K2: Unify the style of FIR generator with IR and SIR tree-generators
- [`KT-66061`](https://youtrack.jetbrains.com/issue/KT-66061) Kotlin/Native - building shared module for iOS - Argument list too long
- [`KT-67804`](https://youtrack.jetbrains.com/issue/KT-67804) removeFirst and removeLast return type with Java 21
- [`KT-49420`](https://youtrack.jetbrains.com/issue/KT-49420) Suspicious behaviour of frontend in case of DefinitelyNotNull type overload
- [`KT-59752`](https://youtrack.jetbrains.com/issue/KT-59752) K2: "Conflicting overloads" if function with same signature added to different contexts
- [`KT-68618`](https://youtrack.jetbrains.com/issue/KT-68618) K1: Unresolved reference for qualified this in implicit type
- [`KT-25341`](https://youtrack.jetbrains.com/issue/KT-25341) NOT_YET_SUPPORTED_IN_INLINE reported over anonymous object border
- [`KT-69044`](https://youtrack.jetbrains.com/issue/KT-69044) Destructuring declaration shouldn't be possible in declaration in when
- [`KT-69028`](https://youtrack.jetbrains.com/issue/KT-69028) K2: `FirJvmActualizingBuiltinSymbolProvider` returns `null` on builtins declarations if common source-set is not presented
- [`KT-67119`](https://youtrack.jetbrains.com/issue/KT-67119) Migration warning from context receivers to context parameters
- [`KT-15704`](https://youtrack.jetbrains.com/issue/KT-15704) Rethink usage of term "type annotation" in error messages
- [`KT-68970`](https://youtrack.jetbrains.com/issue/KT-68970) K2. Argument type mismatch caused by out projection in inferred type from if - else
- [`KT-68727`](https://youtrack.jetbrains.com/issue/KT-68727) K2: "Null argument in ExpressionCodegen for parameter VALUE_PARAMETER" caused by an enum class with default parameter in a different module
- [`KT-68626`](https://youtrack.jetbrains.com/issue/KT-68626) K2: "Conflicting Overloads" for function if inherited from generic type
- [`KT-68800`](https://youtrack.jetbrains.com/issue/KT-68800) K2: Delete `ConeAttributes.plus` method
- [`KT-59389`](https://youtrack.jetbrains.com/issue/KT-59389) K2: Missing AMBIGUOUS_LABEL
- [`KT-68803`](https://youtrack.jetbrains.com/issue/KT-68803) K2: Smart cast fails with "Unresolved reference" when `@Suppress`("UNCHECKED_CAST") used in statement
- [`KT-68968`](https://youtrack.jetbrains.com/issue/KT-68968) K2: Missing ILLEGAL_SUSPEND_FUNCTION_CALL diagnostic in initialization code of a local class inside suspend function
- [`KT-68489`](https://youtrack.jetbrains.com/issue/KT-68489) K2: WRONG_ANNOTATION_TARGET with Java and Kotlin `@Target` annotation positions
- [`KT-68517`](https://youtrack.jetbrains.com/issue/KT-68517) "IrSimpleFunctionSymbolImpl is unbound" for actual class containing non-actual functions
- [`KT-59678`](https://youtrack.jetbrains.com/issue/KT-59678) K2: Investigate `ConeKotlinType.unCapture()`
- [`KT-64193`](https://youtrack.jetbrains.com/issue/KT-64193) K2: No smartcast with two boolean expressions in a row
- [`KT-69058`](https://youtrack.jetbrains.com/issue/KT-69058) K2: Java-defined property annotations not persisted
- [`KT-69027`](https://youtrack.jetbrains.com/issue/KT-69027) K2: Initialize `FirStdlibBuiltinSyntheticFunctionInterfaceProvider` in library session
- [`KT-62818`](https://youtrack.jetbrains.com/issue/KT-62818) K2: improve VAR_OVERRIDDEN_BY_VAL diagnostic message
- [`KT-68214`](https://youtrack.jetbrains.com/issue/KT-68214) Rename TypeApproximatorConfiguration properties for clarity
- [`KT-64515`](https://youtrack.jetbrains.com/issue/KT-64515) K2 IDE: [NEW_INFERENCE_ERROR] in a build.gradle.kts script while applying "jvm-test-suite" plugin and then configuring targets for test suites
- [`KT-68093`](https://youtrack.jetbrains.com/issue/KT-68093) Implement deprecation of smartcasts on class-delegated properties
- [`KT-67270`](https://youtrack.jetbrains.com/issue/KT-67270) Native: report more performance metrics from the compiler
- [`KT-68621`](https://youtrack.jetbrains.com/issue/KT-68621) DATA_CLASS_INVISIBLE_COPY_USAGE false negative for inline fun
- [`KT-68575`](https://youtrack.jetbrains.com/issue/KT-68575) K2: `@ParameterName` annotation is not erased when inferring the type of `it` in lambdas
- [`KT-69000`](https://youtrack.jetbrains.com/issue/KT-69000) Can't render constructor of intersection type
- [`KT-68401`](https://youtrack.jetbrains.com/issue/KT-68401) K2: "IllegalAccessError: failed to access class" caused by package private super Java type, when inferencing a common super type of if or when branches on JVM
- [`KT-68806`](https://youtrack.jetbrains.com/issue/KT-68806) K/Wasm RuntimeError: unreachable on Sequence::toList
- [`KT-68849`](https://youtrack.jetbrains.com/issue/KT-68849) K2: "ClassCastException:  cannot be cast to kotlin.jvm.functions.Function2" caused by passing lambda to SAM constructor results
- [`KT-68455`](https://youtrack.jetbrains.com/issue/KT-68455) K2: False negative UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS
- [`KT-61744`](https://youtrack.jetbrains.com/issue/KT-61744) Native: -Xsave-llvm-ir-after fails to check errors from LLVMPrintModuleToFile
- [`KT-68874`](https://youtrack.jetbrains.com/issue/KT-68874) Types with different captured types as type arguments are rendered incorrectly
- [`KT-67103`](https://youtrack.jetbrains.com/issue/KT-67103) Support AbbreviatedTypeAttribute for aliased types from the source code
- [`KT-65038`](https://youtrack.jetbrains.com/issue/KT-65038) K2: Type alias from indirect dependency causes `MISSING_DEPENDENCY_CLASS` error
- [`KT-63921`](https://youtrack.jetbrains.com/issue/KT-63921) K2: different representation of recursive type aliases
- [`KT-68538`](https://youtrack.jetbrains.com/issue/KT-68538) KJS/K2: using `while` with `break` inside inline lambdas leads to an endless cycle
- [`KT-68679`](https://youtrack.jetbrains.com/issue/KT-68679) K2: "Override has incorrect nullability in its signature compared to the overridden declaration" caused by subclass of Android HashMap
- [`KT-68734`](https://youtrack.jetbrains.com/issue/KT-68734) K2: enum class in KMP: Expect declaration `MMKVLogLevel` is incompatible with actual `MMKVLogLevel` because modality is different
- [`KT-68820`](https://youtrack.jetbrains.com/issue/KT-68820) K2: "Unresolved reference" on calling function with "contract" name
- [`KT-68230`](https://youtrack.jetbrains.com/issue/KT-68230) K2: FirMissingDependencyClassChecker: Not supported: ConeFlexibleType
- [`KT-68531`](https://youtrack.jetbrains.com/issue/KT-68531) K2: False-negative error on assignment to enum entry
- [`KT-68571`](https://youtrack.jetbrains.com/issue/KT-68571) K2: "IllegalStateException: Fake override should have at least one overridden descriptor" caused by exceptions and when statement
- [`KT-68523`](https://youtrack.jetbrains.com/issue/KT-68523) K2: FileAnalysisException when using Definitely non-nullable types
- [`KT-68339`](https://youtrack.jetbrains.com/issue/KT-68339) K2: "Enum entry * is uninitialized here" caused by lazy property with enum in `when` expression
- [`KT-68678`](https://youtrack.jetbrains.com/issue/KT-68678) K2: Drop using `FirBuiltinSymbolProvider` while compiling JVM stdlib
- [`KT-68585`](https://youtrack.jetbrains.com/issue/KT-68585) Implement new rules for CFA about enum entries
- [`KT-68110`](https://youtrack.jetbrains.com/issue/KT-68110) K2: "Java type mismatch" caused by spring.Nullable
- [`KT-68613`](https://youtrack.jetbrains.com/issue/KT-68613) K2: False positive `CONFLICTING_PROJECTION` after fixing KT-67764
- [`KT-67764`](https://youtrack.jetbrains.com/issue/KT-67764) K2: False negative: Projection problem is not reported in `is` expression
- [`KT-68542`](https://youtrack.jetbrains.com/issue/KT-68542) K2: Fix referecing to `@ExtensionFunctionType` if it's declared in source
- [`KT-68188`](https://youtrack.jetbrains.com/issue/KT-68188) K2: Properly support FunctionN creation for stdlib compilation
- [`KT-67946`](https://youtrack.jetbrains.com/issue/KT-67946) K2: Crash on red code: `Instead use FirErrorTypeRef for ERROR CLASS: Cannot infer argument for type parameter T`
- [`KT-68617`](https://youtrack.jetbrains.com/issue/KT-68617) K2: Secondary constructors in a sealed class have private visibility instead of protected in the generated IR
- [`KT-63920`](https://youtrack.jetbrains.com/issue/KT-63920) K2: Private secondary sealed class constructor is private in metadata, but protected in K1
- [`KT-68207`](https://youtrack.jetbrains.com/issue/KT-68207) K2: Investigate if losing ConeIntersectionType.upperBoundForApproximation during approximation leads to any issues
- [`KT-51433`](https://youtrack.jetbrains.com/issue/KT-51433) FE 1.0: implement warnings about label resolve changes

### Compose compiler

#### New features
- [`192e556`](https://github.com/JetBrains/kotlin/commit/192e5565f484b399b33ed9e959793922f0aeb3d0) Strong skipping is now enabled by default
- [`842a9e8`](https://github.com/JetBrains/kotlin/commit/842a9e87e3c1e1d219313caedcc9e9fae895e53f) Add support for default parameters in abstract and open @Composable functions [`b/165812010`](https://issuetracker.google.com/issues/165812010)

#### Fixes
- [`e207b05`](https://github.com/JetBrains/kotlin/commit/e207b05f1fcbba38b71030be0fc30b378e9b5308) Fixes group generation for if statements when nonSkippingGroupOptimization is enabled [`b/346821372`](https://issuetracker.google.com/issues/346821372)
- [`f64fc3a`](https://github.com/JetBrains/kotlin/commit/f64fc3ae5f9be6f2a066b3b9350f830bdd4e854c) Fixes `endToMarker` generation in early return from inline lambdas that caused start/end imbalance [`b/346808602`](https://issuetracker.google.com/issues/346808602)
- [`d6ac8a5`](https://github.com/JetBrains/kotlin/commit/d6ac8a50a4eb4ce1c6464cb5103d2d9b04f67019) Stop memoizing lambdas with captured property delegates [`b/342557697`](https://issuetracker.google.com/issues/342557697)
- [`f38d5a3`](https://github.com/JetBrains/kotlin/commit/f38d5a3c047edb3b38eb0eaebcdcedc2aa1c04d8) Stop capturing parameter meta across crossinline boundary [`b/343801379`](https://issuetracker.google.com/issues/343801379)
- [`770fe8d`](https://github.com/JetBrains/kotlin/commit/770fe8dda6a8a801b47cb84f5026f93555c4b452) Propagate annotations from inferred function types when serializing [`b/345261077`](https://issuetracker.google.com/issues/345261077)
- [`3c67cda`](https://github.com/JetBrains/kotlin/commit/3c67cda09099f9acdd10b944183a75958e023141) Fix memoization of captureless lambdas when K2 compiler is used [`b/340582180`](https://issuetracker.google.com/issues/340582180)
- [`3281e53`](https://github.com/JetBrains/kotlin/commit/3281e53a1bb15af932157d42178184aed55e6d71) Allow memoizing lambdas in composable inline functions [`b/340606661`](https://issuetracker.google.com/issues/340606661)

### IR. Actualizer

- [`KT-68830`](https://youtrack.jetbrains.com/issue/KT-68830) Compiler crash on missing actual class
- [`KT-69024`](https://youtrack.jetbrains.com/issue/KT-69024) K2: Children of expect annotation with `@OptionalExpectation` should be actualized
- [`KT-68742`](https://youtrack.jetbrains.com/issue/KT-68742) Allow expect protected to Java protected actualization
- [`KT-66436`](https://youtrack.jetbrains.com/issue/KT-66436) K2. Actualizing modCount property with a field in AbstractMutableList
- [`KT-68741`](https://youtrack.jetbrains.com/issue/KT-68741) Support actualization of AbstractMutableList.modCount
- [`KT-68801`](https://youtrack.jetbrains.com/issue/KT-68801) Crash on access of fake override of function actualized by fake override

### IR. Inlining

- [`KT-68100`](https://youtrack.jetbrains.com/issue/KT-68100) Run IR validation in the beginning and the end of the common prefix
- [`KT-69171`](https://youtrack.jetbrains.com/issue/KT-69171) Introduce a temporary `-X` CLI parameter that enables double-inlining
- [`KT-69006`](https://youtrack.jetbrains.com/issue/KT-69006) Enable IR visibility checks after IR inlining
- [`KT-69183`](https://youtrack.jetbrains.com/issue/KT-69183) IR inlining: properly handle defaults that depends on previous value parameters
- [`KT-68558`](https://youtrack.jetbrains.com/issue/KT-68558) Move `InlineCallableReferenceToLambdaPhase` into `ir.inline` module

### IR. Tree

- [`KT-68784`](https://youtrack.jetbrains.com/issue/KT-68784) Support validating visibility of referenced declarations in IrValidator
- [`KT-68174`](https://youtrack.jetbrains.com/issue/KT-68174) Delete the IrMessageLogger interface
- [`KT-67082`](https://youtrack.jetbrains.com/issue/KT-67082) Introduce attributes on IrElement
- [`KT-67695`](https://youtrack.jetbrains.com/issue/KT-67695) ForLoopsLowering fails to handle a loop over an imprecise typed iterable
- [`KT-68716`](https://youtrack.jetbrains.com/issue/KT-68716) `DeepCopyIrTreeWithSymbols.visitConst` should remap const type

### JavaScript

- [`KT-66898`](https://youtrack.jetbrains.com/issue/KT-66898) KJS: Reserved keywords not escaped when `-Xir-generate-inline-anonymous-functions` is enabled
- [`KT-69400`](https://youtrack.jetbrains.com/issue/KT-69400) Use correct type for references on local functions when transforming them into lambda
- [`KT-68554`](https://youtrack.jetbrains.com/issue/KT-68554) Legalize marker interface as parent for JSO (interface marked with `@JsPlainObject`)
- [`KT-68891`](https://youtrack.jetbrains.com/issue/KT-68891) `@JsPlainObject` fails to compile when encountering reserved keywords as interface properties
- [`KT-69023`](https://youtrack.jetbrains.com/issue/KT-69023) KJS / IR: `globalThis` is mandatory, breaking older browsers support
- [`KT-68641`](https://youtrack.jetbrains.com/issue/KT-68641) KJS: 'export was not found' with per-file mode on case-insensitive filesystem
- [`KT-68632`](https://youtrack.jetbrains.com/issue/KT-68632) K2: allow JS_NAME_CLASH suppression
- [`KT-68620`](https://youtrack.jetbrains.com/issue/KT-68620) `[wasm] [js]` Default param in inner class method fails if we are referring generic extension property

### Klibs

- [`KT-66605`](https://youtrack.jetbrains.com/issue/KT-66605) [KLIB] Excessive creation of `BaseKotlinLibrary` during resolving libs
- [`KT-68824`](https://youtrack.jetbrains.com/issue/KT-68824) API 4 ABI: Don't show sealed class constructors

### Language Design

- [`KT-68636`](https://youtrack.jetbrains.com/issue/KT-68636) Incorrect private_to_this visibility for data class with a private constructor

### Libraries

- [`KT-31880`](https://youtrack.jetbrains.com/issue/KT-31880) UUID functionality to fix Java bugs as well as extend it
- [`KT-60787`](https://youtrack.jetbrains.com/issue/KT-60787) Cannot ignore alpha when formatting with HexFormat
- [`KT-66129`](https://youtrack.jetbrains.com/issue/KT-66129) Minor issues with HexFormat
- [`KT-57998`](https://youtrack.jetbrains.com/issue/KT-57998) implement Base64.withoutPadding
- [`KT-67511`](https://youtrack.jetbrains.com/issue/KT-67511) provide equals() and hashCode() implementations for kotlinx.metadata.KmType
- [`KT-68240`](https://youtrack.jetbrains.com/issue/KT-68240) stdlib: proper expects for internal API used in intermediate shared source sets
- [`KT-68840`](https://youtrack.jetbrains.com/issue/KT-68840) atomicfu-runtime: annotate some internal functions with `@PublishedApi`
- [`KT-68839`](https://youtrack.jetbrains.com/issue/KT-68839) Annotate `kotlin.js.VOID` property with `@PublishedApi`

### Native

- [`KT-69206`](https://youtrack.jetbrains.com/issue/KT-69206) Native: updating to LLVM 16 breaks debugging in lldb on Linux
- [`KT-68640`](https://youtrack.jetbrains.com/issue/KT-68640) Native: updating to LLVM 16 changes behavior of `used` attribute in C/C++ code
- [`KT-58097`](https://youtrack.jetbrains.com/issue/KT-58097) Kotlin/Native: improve the error message if Xcode is not properly configured

### Native. ObjC Export

- [`KT-57496`](https://youtrack.jetbrains.com/issue/KT-57496) linkReleaseFrameworkIosArm64: e: Compilation failed: An operation is not implemented

### Native. Runtime. Memory

- [`KT-68871`](https://youtrack.jetbrains.com/issue/KT-68871) Native: Unexpected barriers phase during STW: weak-processing

### Native. Swift Export

- [`KT-69469`](https://youtrack.jetbrains.com/issue/KT-69469) Exporting object twice causing crash
- [`KT-69251`](https://youtrack.jetbrains.com/issue/KT-69251) Get rid of context receivers from ./native/.../lazyWithSessions.kt
- [`KT-68865`](https://youtrack.jetbrains.com/issue/KT-68865) Move config into test-directives

### Native. Testing

- [`KT-69235`](https://youtrack.jetbrains.com/issue/KT-69235) Incorrect handling of friend dependencies in Native test infra
- [`KT-67436`](https://youtrack.jetbrains.com/issue/KT-67436) Native: support CLI tests

### Reflection

- [`KT-69433`](https://youtrack.jetbrains.com/issue/KT-69433) KotlinReflectionInternalError on non-reified type parameter in typeOf inside an inline lambda
- [`KT-68675`](https://youtrack.jetbrains.com/issue/KT-68675) K2: KotlinReflectionInternalError on non-reified type parameter in typeOf inside a lambda

### Tools. Build Tools API

- [`KT-68555`](https://youtrack.jetbrains.com/issue/KT-68555) BTA test infra: top level declarations are invisible across modules

### Tools. CLI

- [`KT-68838`](https://youtrack.jetbrains.com/issue/KT-68838) OutOfMemory when compiling in CLI
- [`KT-68743`](https://youtrack.jetbrains.com/issue/KT-68743) Extract common CLI arguments for all KLIB-based backends
- [`KT-68450`](https://youtrack.jetbrains.com/issue/KT-68450) CLI: errors related to module-info are reported even if there are no Kotlin source files

### Tools. CLI. Native

- [`KT-66952`](https://youtrack.jetbrains.com/issue/KT-66952) Native: konanc fails when KONAN_HOME is under path with spaces

### Tools. Commonizer

- [`KT-68835`](https://youtrack.jetbrains.com/issue/KT-68835) Command line length overflow on Linux/Windows while invoking commonizer via :commonizeDistribution

### Tools. Compiler Plugins

- [`KT-69401`](https://youtrack.jetbrains.com/issue/KT-69401) Kotlin power assert plugin doesn't work correctly with safe cast operator
- [`KT-69290`](https://youtrack.jetbrains.com/issue/KT-69290) PowerAssert: implicit receivers included in power-assert generated diagram
- [`KT-68511`](https://youtrack.jetbrains.com/issue/KT-68511) Power Assert kotlinx.assertEquals message display problem
- [`KT-68807`](https://youtrack.jetbrains.com/issue/KT-68807) Power-Assert crashes the Kotlin compiler when if expression used as assertion parameter
- [`KT-68557`](https://youtrack.jetbrains.com/issue/KT-68557) K2. Supertypes resolution of KJK hierarchy fails in presence of allopen plugin

### Tools. Compiler plugins. Serialization

- [`KT-68931`](https://youtrack.jetbrains.com/issue/KT-68931) JS/Native + serialization: partial linkage error
- [`KT-69039`](https://youtrack.jetbrains.com/issue/KT-69039) FIR: Implement IDE-only checker for kotlinx.serialization compiler plugin to report IDE-only diagnostics
- [`KT-68752`](https://youtrack.jetbrains.com/issue/KT-68752) Serializable annotation on Java class is not taken into account in K2 checker

### Tools. Daemon

- [`KT-68297`](https://youtrack.jetbrains.com/issue/KT-68297) KGP 2.0 regression: JAVA_TOOL_OPTIONS is not considered in Kotlin daemon creation

### Tools. Fleet. ObjC Export

- [`KT-68887`](https://youtrack.jetbrains.com/issue/KT-68887) ObjCExport: K1 text fixture `@Deprecated` support
- [`KT-68841`](https://youtrack.jetbrains.com/issue/KT-68841) ObjCExport: `@Deprecated` support
- [`KT-68826`](https://youtrack.jetbrains.com/issue/KT-68826) ObjCExport: SerializersModuleBuilder

### Tools. Gradle

#### New Features

- [`KT-68651`](https://youtrack.jetbrains.com/issue/KT-68651) Compose: provide a single place in extension to configure all compose flags

#### Performance Improvements

- [`KT-61861`](https://youtrack.jetbrains.com/issue/KT-61861) Gradle: Kotlin compilations depend on packed artifacts

#### Fixes

- [`KT-69330`](https://youtrack.jetbrains.com/issue/KT-69330) KotlinCompile friendPathsSet property is racy due causing build cache invalidation
- [`KT-69444`](https://youtrack.jetbrains.com/issue/KT-69444) Don't warn about missing Compose Compiler Gradle plugin in some cases
- [`KT-65271`](https://youtrack.jetbrains.com/issue/KT-65271) Gradle: "Mutating dependency DefaultExternalModuleDependency after it has been finalized has been deprecated " with gradle 8.6-rc-3
- [`KT-69026`](https://youtrack.jetbrains.com/issue/KT-69026) Mark AGP 8.5.0 as compatible with KGP
- [`KT-67822`](https://youtrack.jetbrains.com/issue/KT-67822) Deprecate JVM history files based incremental compilation
- [`KT-67771`](https://youtrack.jetbrains.com/issue/KT-67771) Compatibility with Gradle 8.8 release
- [`KT-65820`](https://youtrack.jetbrains.com/issue/KT-65820) Compatibility with Gradle 8.7 release
- [`KT-64378`](https://youtrack.jetbrains.com/issue/KT-64378) Compatibility with Gradle 8.6 release
- [`KT-68661`](https://youtrack.jetbrains.com/issue/KT-68661) Move ExperimentalWasmDsl to kotlin-gradle-plugin-annotations
- [`KT-69291`](https://youtrack.jetbrains.com/issue/KT-69291) Compose Gradle plugin: Enable strong skipping by default
- [`KT-65528`](https://youtrack.jetbrains.com/issue/KT-65528) Migrate rest of Gradle integration tests to new Test DSL
- [`KT-68306`](https://youtrack.jetbrains.com/issue/KT-68306) Project isolation for FUS statistics: Cannot access project ':' from project ':app' at org.jetbrains.kotlin.gradle.report.BuildMetricsService$ Companion.initBuildScanExtensionHolder
- [`KT-67395`](https://youtrack.jetbrains.com/issue/KT-67395) Add new plugins to collector kotlin gradle performance
- [`KT-67766`](https://youtrack.jetbrains.com/issue/KT-67766) Build against Gradle API 8.7
- [`KT-67890`](https://youtrack.jetbrains.com/issue/KT-67890) Compile against Gradle 8.8 API artifact
- [`KT-68773`](https://youtrack.jetbrains.com/issue/KT-68773) Kotlin 2.0.0 with Gradle 8.8: ConcurrentModificationException on BuildFusService configurationMetrics
- [`KT-67889`](https://youtrack.jetbrains.com/issue/KT-67889) Run tests against Gradle 8.8 release
- [`KT-69078`](https://youtrack.jetbrains.com/issue/KT-69078) Gradle: Add option to disable FUS Service
- [`KT-68308`](https://youtrack.jetbrains.com/issue/KT-68308) Project isolation for FUS statistics: An error is thrown at org.gradle.configurationcache.ProblemReportingCrossProjectModelAccess$ProblemReportingProject.getLayout
- [`KT-58280`](https://youtrack.jetbrains.com/issue/KT-58280) org.jetbrains.kotlin.jvm Gradle plugin contributes build directories to the test compile classpath

### Tools. Gradle. Kapt

- [`KT-61928`](https://youtrack.jetbrains.com/issue/KT-61928) Clarify parameter types in KaptArguments and KaptJavacOption

### Tools. Gradle. Multiplatform

- [`KT-69310`](https://youtrack.jetbrains.com/issue/KT-69310)  w: KLIB resolver: The same 'unique_name=...' found in more than one library for diamond source set structures
- [`KT-66568`](https://youtrack.jetbrains.com/issue/KT-66568) w: KLIB resolver: The same 'unique_name=...' found in more than one library
- [`KT-69406`](https://youtrack.jetbrains.com/issue/KT-69406) Deprecate combinations of KMP plugin with some Gradle Java plugins
- [`KT-66209`](https://youtrack.jetbrains.com/issue/KT-66209) Accessing the source sets by name is confusing
- [`KT-56566`](https://youtrack.jetbrains.com/issue/KT-56566) Consider pre-generating DSL accessors for source sets with names corresponding to the default target hierarchy
- [`KT-69129`](https://youtrack.jetbrains.com/issue/KT-69129) KGP: stdlib version alignment for JS and Wasm
- [`KT-62368`](https://youtrack.jetbrains.com/issue/KT-62368) Kotlin 1.9.X fails to detect kotlin.test.Test annotation reference on commonTest source set when targeting JVM+Android
- [`KT-67110`](https://youtrack.jetbrains.com/issue/KT-67110) Usage of BuildOperationExecutor.getCurrentOpeartion internal Gradle API
- [`KT-68248`](https://youtrack.jetbrains.com/issue/KT-68248) kotlin multiplatform project fail to build on Fedora with corretto

### Tools. Gradle. Native

- [`KT-68638`](https://youtrack.jetbrains.com/issue/KT-68638) KGP 2.0 breaks native test with api dependencies and configuration cache

### Tools. Kapt

- [`KT-68171`](https://youtrack.jetbrains.com/issue/KT-68171) K2KAPT: boxed return types in overridden methods changed to primitives
- [`KT-68145`](https://youtrack.jetbrains.com/issue/KT-68145) K2 KAPT: missing $annotations methods for const properties and private properties without accessors

### Tools. Scripts

- [`KT-69296`](https://youtrack.jetbrains.com/issue/KT-69296) scripting dependency resolution does not authenticate towards maven mirrors
- [`KT-68681`](https://youtrack.jetbrains.com/issue/KT-68681) K2 / CLI / Script: "NullPointerException: getService(...) must not be null" caused by `@DependsOn`

### Tools. Wasm

- [`KT-69245`](https://youtrack.jetbrains.com/issue/KT-69245) K/Wasm: Remove warning of working-in-progress
- [`KT-69154`](https://youtrack.jetbrains.com/issue/KT-69154) K/Wasm: wasmJsBrowserProductionRun flaky crash with "WebAssembly.instantiate(): Import ...  function import requires a callable"
- [`KT-67980`](https://youtrack.jetbrains.com/issue/KT-67980) Wasm: Incorrect "Please choose a JavaScript environment to build distributions and run tests" when WASM is not configured

## 2.0.20-Beta1

### Analysis. API

#### New Features

- [`KT-68143`](https://youtrack.jetbrains.com/issue/KT-68143) Analysis API: support KtWhenConditionInRange call resolution

#### Performance Improvements

- [`KT-67195`](https://youtrack.jetbrains.com/issue/KT-67195) K2: do not call redundant resolve on body resolution phase for classes

#### Fixes

- [`KT-66216`](https://youtrack.jetbrains.com/issue/KT-66216) K2 IDE. "FirDeclaration was not found for class org.jetbrains.kotlin.psi.KtProperty, fir is null" on incorrect string template
- [`KT-53669`](https://youtrack.jetbrains.com/issue/KT-53669) Analysis API: redesign KtSymbolOrigin to distinguish kotlin/java source/library declarations
- [`KT-62889`](https://youtrack.jetbrains.com/issue/KT-62889) K2 IDE. FP `MISSING_DEPENDENCY_CLASS` on not available type alias with available underlying type
- [`KT-62343`](https://youtrack.jetbrains.com/issue/KT-62343) Analysis API: fix binary incopatibility problems cause by `KtAnalysisSessionProvider.analyze` being inline
- [`KT-68498`](https://youtrack.jetbrains.com/issue/KT-68498) To get reference symbol the one should be KtSymbolBasedReference
- [`KT-68393`](https://youtrack.jetbrains.com/issue/KT-68393) Analysis API: Rename `KaClassLikeSymbol. classIdIfNonLocal` to `classId`
- [`KT-62924`](https://youtrack.jetbrains.com/issue/KT-62924) Analysis API: rename KtCallableSymbol.callableIdIfNonLocal -> callableId
- [`KT-66712`](https://youtrack.jetbrains.com/issue/KT-66712) K2 IDE. SOE on settings string template for string variable with the same name
- [`KT-65892`](https://youtrack.jetbrains.com/issue/KT-65892) K2: "We should be able to find a symbol" for findNonLocalFunction
- [`KT-67360`](https://youtrack.jetbrains.com/issue/KT-67360) Analysis API: KtDestructuringDeclarationSymbol#entries shouldn't be KtLocalVariableSymbol
- [`KT-68198`](https://youtrack.jetbrains.com/issue/KT-68198) Analysis API: Support application service registration in plugin XMLs
- [`KT-68273`](https://youtrack.jetbrains.com/issue/KT-68273) AA: support `KtFirKDocReference#isReferenceToImportAlias`
- [`KT-68272`](https://youtrack.jetbrains.com/issue/KT-68272) AA: KtFirReference.isReferenceToImportAlias doesn't work for references on constructor
- [`KT-67996`](https://youtrack.jetbrains.com/issue/KT-67996) Analysis API: rename Kt prefix to Ka
- [`KT-66996`](https://youtrack.jetbrains.com/issue/KT-66996) Analysis API: Expose the abbreviated type of an expanded `KtType`
- [`KT-66646`](https://youtrack.jetbrains.com/issue/KT-66646) K2: Expected FirResolvedTypeRef with ConeKotlinType but was FirUserTypeRefImpl from FirJsHelpersKt.isExportedObject
- [`KT-68203`](https://youtrack.jetbrains.com/issue/KT-68203) K2: Analysis API: wrong type of receiver value in case of imported object member
- [`KT-68031`](https://youtrack.jetbrains.com/issue/KT-68031) LL resolve crash in case of PCLA inference with local object
- [`KT-67851`](https://youtrack.jetbrains.com/issue/KT-67851) K2: `PsiReference#isReferenceTo` always returns false for references to Java getters
- [`KT-68076`](https://youtrack.jetbrains.com/issue/KT-68076) AA: use type code fragments for import alias detection
- [`KT-65915`](https://youtrack.jetbrains.com/issue/KT-65915) K2: Analysis API: extract services registration into xml file
- [`KT-68049`](https://youtrack.jetbrains.com/issue/KT-68049) Analysis API: do not expose imported symbols
- [`KT-68075`](https://youtrack.jetbrains.com/issue/KT-68075) K2: Analysis API: Type arguments for delegation constructor to java constructor with type parameters not supported
- [`KT-65190`](https://youtrack.jetbrains.com/issue/KT-65190) AA: reference to the super type is not resolved
- [`KT-68070`](https://youtrack.jetbrains.com/issue/KT-68070) AA: KtExpressionInfoProvider#isUsedAsExpression doesn't work for KtPropertyDelegate
- [`KT-67748`](https://youtrack.jetbrains.com/issue/KT-67748) K2: AllCandidatesResolver modifies the original FirDelegatedConstructorCall
- [`KT-67743`](https://youtrack.jetbrains.com/issue/KT-67743) K2: Stubs & AbbreviatedTypeAttribute
- [`KT-67706`](https://youtrack.jetbrains.com/issue/KT-67706) K2: "KtDotQualifiedExpression is not a subtype of class KtNamedDeclaration" from UnusedChecker
- [`KT-68021`](https://youtrack.jetbrains.com/issue/KT-68021) Analysis API: do not break the diagnostic collection in a case of exception from some collector
- [`KT-67949`](https://youtrack.jetbrains.com/issue/KT-67949) AA: Type arguments of Java methods' calls are not reported as used by KtFirImportOptimizer
- [`KT-67988`](https://youtrack.jetbrains.com/issue/KT-67988) AA: functional type at receiver position should be wrapped in parenthesis
- [`KT-66536`](https://youtrack.jetbrains.com/issue/KT-66536) Analysis API: ContextCollector doesn't provide implicit receivers from FirExpressionResolutionExtension
- [`KT-67321`](https://youtrack.jetbrains.com/issue/KT-67321) AA: Type arguments of Java methods' calls are not resolved
- [`KT-64158`](https://youtrack.jetbrains.com/issue/KT-64158) K2: "KotlinIllegalArgumentExceptionWithAttachments: No fir element was found for KtParameter"
- [`KT-60344`](https://youtrack.jetbrains.com/issue/KT-60344) K2 IDE. "KotlinExceptionWithAttachments: expect `createKtCall` to succeed for resolvable case with callable symbol" on attempt to assign value to param named getParam
- [`KT-64599`](https://youtrack.jetbrains.com/issue/KT-64599) K2: "expect `createKtCall` to succeed for resolvable case with callable" for unfinished if statement
- [`KT-60330`](https://youtrack.jetbrains.com/issue/KT-60330) K2 IDE. ".KotlinExceptionWithAttachments: expect `createKtCall` to succeed for resolvable case with callable symbol" on attempt to assign or compare true with something
- [`KT-66672`](https://youtrack.jetbrains.com/issue/KT-66672) K2 IDE. False positive INVISIBLE_REFERENCE on accessing private subclass as type argument in parent class declaration
- [`KT-67750`](https://youtrack.jetbrains.com/issue/KT-67750) Analysis API: Remove `infix` modifiers from type equality and subtyping functions
- [`KT-67655`](https://youtrack.jetbrains.com/issue/KT-67655) Analysis API: declare a rule how to deal with parameters in KtLifetimeOwner
- [`KT-61775`](https://youtrack.jetbrains.com/issue/KT-61775) Analysis API: KtKClassAnnotationValue lacks complete type information
- [`KT-67168`](https://youtrack.jetbrains.com/issue/KT-67168) K2: Analysis API: Rendering is broken for JSR-305 enhanced Java types
- [`KT-66689`](https://youtrack.jetbrains.com/issue/KT-66689) Analysis API: KtFirPackageScope shouldn't rely on KotlinDeclarationProvider for binary dependencies in standalone mode
- [`KT-60483`](https://youtrack.jetbrains.com/issue/KT-60483) Analysis API: add isTailrec property to KtFunctionSymbol
- [`KT-67472`](https://youtrack.jetbrains.com/issue/KT-67472) K2: Analysis API FIR: KtFunctionCall misses argument with desugared expressions
- [`KT-65759`](https://youtrack.jetbrains.com/issue/KT-65759) Analysis API: Avoid hard references to `LLFirSession` in session validity trackers
- [`KT-60272`](https://youtrack.jetbrains.com/issue/KT-60272) K2: Implement active invalidation of `KtAnalysisSession`s
- [`KT-66765`](https://youtrack.jetbrains.com/issue/KT-66765) K2: Analysis API: support classpath substitution with library dependencies in super type transformer
- [`KT-67265`](https://youtrack.jetbrains.com/issue/KT-67265) K2: status phase should resolve original declarations in the case of classpath subsitution
- [`KT-67244`](https://youtrack.jetbrains.com/issue/KT-67244) K2: StackOverflowError in the case of cyclic type hierarchy and library classpath substitution
- [`KT-67080`](https://youtrack.jetbrains.com/issue/KT-67080) K2: clearer contract for lazyResolveToPhaseWithCallableMembers
- [`KT-65413`](https://youtrack.jetbrains.com/issue/KT-65413) K2 IDE: KTOR unresolved serializer() call for `@Serializable` class in common code
- [`KT-66713`](https://youtrack.jetbrains.com/issue/KT-66713) K2 FIR: Expose a way to get the module name used for name mangling
- [`KT-61892`](https://youtrack.jetbrains.com/issue/KT-61892) KtType#asPsiType could provide nullability annotations
- [`KT-66122`](https://youtrack.jetbrains.com/issue/KT-66122) Analysis API: Pass `KtTestModule` instead of `TestModule` to tests based on `AbstractAnalysisApiBasedTest`

### Analysis. Light Classes

- [`KT-68275`](https://youtrack.jetbrains.com/issue/KT-68275) LC: no arg constructor is not visible in light classes
- [`KT-66687`](https://youtrack.jetbrains.com/issue/KT-66687) Symbol Light Classes: Duplicate field names for classes with companion objects
- [`KT-66804`](https://youtrack.jetbrains.com/issue/KT-66804) Symbol Light Classes: Fields from the parent interface's companion are added to DefaultImpls

### Apple Ecosystem

- [`KT-68257`](https://youtrack.jetbrains.com/issue/KT-68257) Xcode incorrectly reuses embedAndSign framework when moving to and from 2.0.0
- [`KT-65542`](https://youtrack.jetbrains.com/issue/KT-65542) Cinterop tasks fails if Xcode 15.3 is used

### Backend. Wasm

- [`KT-65798`](https://youtrack.jetbrains.com/issue/KT-65798) K/Wasm: make an error on default export usage
- [`KT-68453`](https://youtrack.jetbrains.com/issue/KT-68453) K/Wasm: "Supported JS engine not detected" in Web Worker
- [`KT-64565`](https://youtrack.jetbrains.com/issue/KT-64565) Kotlin/wasm removeEventListener function did not remove the event listener
- [`KT-66099`](https://youtrack.jetbrains.com/issue/KT-66099) Wasm: local.get of type f64 has to be in the same reference type hierarchy as (ref 686) @+237036

### Compiler

#### New Features

- [`KT-67611`](https://youtrack.jetbrains.com/issue/KT-67611) Implement improved handling of $ in literals
- [`KT-39868`](https://youtrack.jetbrains.com/issue/KT-39868) Allow access to protected consts and fields from a super companion object
- [`KT-67787`](https://youtrack.jetbrains.com/issue/KT-67787) Implement guard conditions for when-with-subject
- [`KT-68165`](https://youtrack.jetbrains.com/issue/KT-68165) Native: type checks on generic types boundary
- [`KT-66169`](https://youtrack.jetbrains.com/issue/KT-66169) `useContents` lacks a `contract`
- [`KT-67767`](https://youtrack.jetbrains.com/issue/KT-67767) Introduce an ability to enforce explicit return types for public declarations without enabling Explicit API mode
- [`KT-65841`](https://youtrack.jetbrains.com/issue/KT-65841) Allow to actualize expect types in kotlin stdlib to builtins in JVM
- [`KT-53834`](https://youtrack.jetbrains.com/issue/KT-53834) Support for JSpecify `@NullUnmarked`

#### Performance Improvements

- [`KT-68034`](https://youtrack.jetbrains.com/issue/KT-68034) Devirtualization analysis fails to devirtualize string.get

#### Fixes

- [`KT-68568`](https://youtrack.jetbrains.com/issue/KT-68568) K2: False-positive ACCIDENTAL_OVERRIDE caused by missing dependency class
- [`KT-66723`](https://youtrack.jetbrains.com/issue/KT-66723) K2: NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS for actual typealias that extends to Java class with complicated hierarchy that includes default method
- [`KT-68492`](https://youtrack.jetbrains.com/issue/KT-68492) JVM IR backend: IDE / Kotlin Debugger: AE “Non-reified type parameter under ::class should be rejected by type checker” on evaluating private generic function
- [`KT-61875`](https://youtrack.jetbrains.com/issue/KT-61875) Native: remove support for bitcode embedding
- [`KT-35305`](https://youtrack.jetbrains.com/issue/KT-35305) "Overload resolution ambiguity" on function for unsigned types (UByte, UShort, UInt, ULong)
- [`KT-59679`](https://youtrack.jetbrains.com/issue/KT-59679) K2: Investigate extracting uncompleted candidates from blocks
- [`KT-68193`](https://youtrack.jetbrains.com/issue/KT-68193) JDK 21: new MutableList.addFirst/addLast  methods allow adding nullable value for non-null types
- [`KT-68383`](https://youtrack.jetbrains.com/issue/KT-68383) K2: "Argument type mismatch: actual type is 'kotlin.String', but 'T & Any' was expected." with intersection types
- [`KT-68351`](https://youtrack.jetbrains.com/issue/KT-68351) K2: "Suspension functions can only be called within coroutine body"
- [`KT-68674`](https://youtrack.jetbrains.com/issue/KT-68674) False positive ACTUAL_WITHOUT_EXPECT in K2
- [`KT-64335`](https://youtrack.jetbrains.com/issue/KT-64335) K2: improve rendering of captured types in diagnostic messages
- [`KT-67933`](https://youtrack.jetbrains.com/issue/KT-67933) K2: no conversion between fun interfaces if target has `suspend`
- [`KT-68350`](https://youtrack.jetbrains.com/issue/KT-68350) K2: "Inapplicable candidate(s)" caused by parameter reference of local class with type parameters from function
- [`KT-68362`](https://youtrack.jetbrains.com/issue/KT-68362) False-positive ABSTRACT_MEMBER_NOT_IMPLEMENTED for inheritor of java class which directly implements java.util.Map
- [`KT-68446`](https://youtrack.jetbrains.com/issue/KT-68446) K2: compile-time failure on smart-casted generic value used as a when-subject in a contains-check with range
- [`KT-68571`](https://youtrack.jetbrains.com/issue/KT-68571) K2: "IllegalStateException: Fake override should have at least one overridden descriptor" caused by exceptions and when statement
- [`KT-68339`](https://youtrack.jetbrains.com/issue/KT-68339) K2: "Enum entry * is uninitialized here" caused by lazy property with enum in `when` expression
- [`KT-66688`](https://youtrack.jetbrains.com/issue/KT-66688) K2: false-negative "upper bound violated" error in extension receiver
- [`KT-64106`](https://youtrack.jetbrains.com/issue/KT-64106) Native: the compiler allows using `-opt` and `-g` at the same time
- [`KT-67887`](https://youtrack.jetbrains.com/issue/KT-67887) Expection on  assigning to private field of value type
- [`KT-67801`](https://youtrack.jetbrains.com/issue/KT-67801) NSME on evaluating private member function with value class parameter
- [`KT-67800`](https://youtrack.jetbrains.com/issue/KT-67800) NSME on evaluating private top-level function with value class parameter
- [`KT-57996`](https://youtrack.jetbrains.com/issue/KT-57996) Usages of `Foo `@Nullable` []` produce only warnings even with `-Xtype-enhancement-improvements-strict-mode -Xjspecify-annotations=strict`
- [`KT-68630`](https://youtrack.jetbrains.com/issue/KT-68630) DiagnosticsSuppressor is not invoked with Kotlin 2.0
- [`KT-68222`](https://youtrack.jetbrains.com/issue/KT-68222) K2. KMP. False negative `Expected declaration must not have a body` for expected top-level property with getter/setter
- [`KT-64103`](https://youtrack.jetbrains.com/issue/KT-64103) FirExpectActualDeclarationChecker reports diagnostic error for KtPsiSimpleDiagnostic with KtFakeSourceElement
- [`KT-68191`](https://youtrack.jetbrains.com/issue/KT-68191) K2. Static fake-overrides are not generated for kotlin Fir2IrLazyClass
- [`KT-64990`](https://youtrack.jetbrains.com/issue/KT-64990) K2: Remove usages of SymbolTable from FIR2IR
- [`KT-67798`](https://youtrack.jetbrains.com/issue/KT-67798) NSME on assigning to private delegated property of value class
- [`KT-68264`](https://youtrack.jetbrains.com/issue/KT-68264) K2: confusing INVISIBLE_* error when typealias is involved
- [`KT-68024`](https://youtrack.jetbrains.com/issue/KT-68024) K2: Gradle repo test `accessors to kotlin internal task types...` fails on K2
- [`KT-67943`](https://youtrack.jetbrains.com/issue/KT-67943) Approximation should not generate types with UPPER_BOUND_VIOLATION errors
- [`KT-67503`](https://youtrack.jetbrains.com/issue/KT-67503) K2: False negative "Type Expected" when attempting to annotate a wildcard type argument
- [`KT-68187`](https://youtrack.jetbrains.com/issue/KT-68187) K2: Create IrBuiltins in fir2ir only after IR actualization
- [`KT-66443`](https://youtrack.jetbrains.com/issue/KT-66443) K2: ArrayIterationHandler doesn't work if UIntArray declared in sources
- [`KT-68291`](https://youtrack.jetbrains.com/issue/KT-68291) K2 / Contracts: Non-existent invocation kind is suggested as a fix
- [`KT-67692`](https://youtrack.jetbrains.com/issue/KT-67692) Native: support LLVM opaque pointers in the compiler
- [`KT-68209`](https://youtrack.jetbrains.com/issue/KT-68209) K2: Strange import suggestion when lambda body contains invalid code
- [`KT-67368`](https://youtrack.jetbrains.com/issue/KT-67368) "NullPointerException: Parameter specified as non-null is null" local lambda creates new not-null checks with 2.0.0-Beta5
- [`KT-66554`](https://youtrack.jetbrains.com/issue/KT-66554) K2. Drop FIR based fake-override generator from fir2ir
- [`KT-64202`](https://youtrack.jetbrains.com/issue/KT-64202) K2: Drop old methods for calculation of overridden symbols for lazy declarations
- [`KT-55851`](https://youtrack.jetbrains.com/issue/KT-55851) K2: reference to a field from package private class crashes in runtime
- [`KT-67895`](https://youtrack.jetbrains.com/issue/KT-67895) K2: Properly implement generation of fake-overrides for fields
- [`KT-54496`](https://youtrack.jetbrains.com/issue/KT-54496) K2: `REDUNDANT_MODALITY_MODIFIER` diagnostic disregards compiler plugins
- [`KT-63745`](https://youtrack.jetbrains.com/issue/KT-63745) K2: Approximation of DNN with nullability warning attribute leads to attribute incorrectly becoming not-null
- [`KT-63362`](https://youtrack.jetbrains.com/issue/KT-63362) AbstractTypeApproximator fixes only first local type in hierarchy
- [`KT-67769`](https://youtrack.jetbrains.com/issue/KT-67769) K2: "variable must be initialized" on unreachable access in constructor
- [`KT-51195`](https://youtrack.jetbrains.com/issue/KT-51195) FIR IC: Incremental compilation fails with `@PublishedApi` property
- [`KT-67966`](https://youtrack.jetbrains.com/issue/KT-67966) No JVM type annotation is generated on a class supertype
- [`KT-55128`](https://youtrack.jetbrains.com/issue/KT-55128) Wrong type path in type annotations when type arguments are compiled to wildcards
- [`KT-46640`](https://youtrack.jetbrains.com/issue/KT-46640) Generate JVM type annotations on wildcard bounds
- [`KT-67952`](https://youtrack.jetbrains.com/issue/KT-67952) Annotations on type parameters are not generated for parameters other than the first
- [`KT-68012`](https://youtrack.jetbrains.com/issue/KT-68012) K2. No `'operator' modifier is required on 'component'` error in K2
- [`KT-61835`](https://youtrack.jetbrains.com/issue/KT-61835) K2: FirStubTypeTransformer receives unresolved expressions in builder inference session
- [`KT-63596`](https://youtrack.jetbrains.com/issue/KT-63596) K1/K2: Different behavior for lambda with different return type
- [`KT-67688`](https://youtrack.jetbrains.com/issue/KT-67688) K2: False positive CANNOT_INFER_PARAMETER_TYPE for Unit constraint type variable
- [`KT-62080`](https://youtrack.jetbrains.com/issue/KT-62080) False positive UNUSED_VARIABLE for variable that is used in lambda and in further code with several conditions
- [`KT-60726`](https://youtrack.jetbrains.com/issue/KT-60726) K2: Missed TYPE_MISMATCH error: inferred type non-suspend function but suspend function was expected
- [`KT-41835`](https://youtrack.jetbrains.com/issue/KT-41835) [FIR] Green code turns to red in presence of smartcasts and redundant type arguments
- [`KT-67579`](https://youtrack.jetbrains.com/issue/KT-67579) K1/JVM: false-negative annotation-based diagnostics on usages of ABI compiled with non-trivially configured generation of default methods
- [`KT-67493`](https://youtrack.jetbrains.com/issue/KT-67493) K2: argument type mismatch: actual type is 'T', but 'T' was expected
- [`KT-64900`](https://youtrack.jetbrains.com/issue/KT-64900) K2: `getConstructorKeyword` call in `PsiRawFirBuilder.toFirConstructor` forces AST load
- [`KT-67648`](https://youtrack.jetbrains.com/issue/KT-67648) K2: wrong exposed visibility errors with WRONG_MODIFIER_CONTAINING_DECLARATION on top-level enum class
- [`KT-58686`](https://youtrack.jetbrains.com/issue/KT-58686) FIR2IR: Don't use global counters
- [`KT-67592`](https://youtrack.jetbrains.com/issue/KT-67592) K2: Success execution of `:kotlin-stdlib:compileKotlinMetadata`
- [`KT-60398`](https://youtrack.jetbrains.com/issue/KT-60398) K2: consider forbidding FirBasedSymbol rebind
- [`KT-54918`](https://youtrack.jetbrains.com/issue/KT-54918) Refactor transformAnonymousFunctionWithExpectedType
- [`KT-63360`](https://youtrack.jetbrains.com/issue/KT-63360) K2: Malformed type mismatch error with functional type
- [`KT-67266`](https://youtrack.jetbrains.com/issue/KT-67266) K2: disappeared INLINE_CLASS_DEPRECATED
- [`KT-67569`](https://youtrack.jetbrains.com/issue/KT-67569) K2: Fix default value parameters of Enum's constructor if it's declared in source code
- [`KT-67378`](https://youtrack.jetbrains.com/issue/KT-67378) K2: Don't use `wrapScopeWithJvmMapped` for common source sets
- [`KT-67738`](https://youtrack.jetbrains.com/issue/KT-67738) K2: Introduce `kotlin.internal.ActualizeByJvmBuiltinProvider` annotation
- [`KT-67136`](https://youtrack.jetbrains.com/issue/KT-67136) Put $this parameter to LVT for suspend lambdas
- [`KT-62538`](https://youtrack.jetbrains.com/issue/KT-62538) K2: Declarations inside external classes should be implicitly external
- [`KT-67627`](https://youtrack.jetbrains.com/issue/KT-67627) K2: External interface companion isn't external in IR
- [`KT-60290`](https://youtrack.jetbrains.com/issue/KT-60290) K2: origin is not set for !in operator
- [`KT-67512`](https://youtrack.jetbrains.com/issue/KT-67512) K2: false positive WRONG_GETTER_RETURN_TYPE when getter return type is annotated
- [`KT-67635`](https://youtrack.jetbrains.com/issue/KT-67635) K2: No warning TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES for SAM constructor with inferred type
- [`KT-67598`](https://youtrack.jetbrains.com/issue/KT-67598) K2: Fix incorrect casting `UByte` to `Number` in `FirToConstantValueTransformer`
- [`KT-56564`](https://youtrack.jetbrains.com/issue/KT-56564) False positive "non-exhaustive when" in case of intersection type
- [`KT-63969`](https://youtrack.jetbrains.com/issue/KT-63969) K2: extra property in metadata
- [`KT-63968`](https://youtrack.jetbrains.com/issue/KT-63968) K2: extra property in metadata for anonymous variable in script
- [`KT-67547`](https://youtrack.jetbrains.com/issue/KT-67547) K/N can't build caches, fails with "clang++: error=2, No such file or directory"
- [`KT-64457`](https://youtrack.jetbrains.com/issue/KT-64457) K2: Fix DecompiledKnmStubConsistencyK2TestGenerated
- [`KT-67102`](https://youtrack.jetbrains.com/issue/KT-67102) IR Evaluator: NoSuchFieldException when accessing a private delegated property
- [`KT-66377`](https://youtrack.jetbrains.com/issue/KT-66377) IR Evaluator: "no container found for type parameter" when evaluating nested generics
- [`KT-66378`](https://youtrack.jetbrains.com/issue/KT-66378) IR Evaluator: Symbol is unbound
- [`KT-64506`](https://youtrack.jetbrains.com/issue/KT-64506) IDE, IR Evaluator: NPE in ReflectiveAccessLowering.fieldLocationAndReceiver when evaluating private static properties
- [`KT-67380`](https://youtrack.jetbrains.com/issue/KT-67380) K2: Don't check for `equals` overriding for class `Any`
- [`KT-67038`](https://youtrack.jetbrains.com/issue/KT-67038) K2: Missing type of FirLiteralExpression causes an exception for property initializer type resolution
- [`KT-59813`](https://youtrack.jetbrains.com/issue/KT-59813) K2: Fix the TODO about `firEffect.source` in `FirReturnsImpliesAnalyzer`
- [`KT-59834`](https://youtrack.jetbrains.com/issue/KT-59834) K2: Fix the TODO about `merge(other)` in `UnusedChecker`
- [`KT-59833`](https://youtrack.jetbrains.com/issue/KT-59833) K2: Stop modifying values of enum entries
- [`KT-59188`](https://youtrack.jetbrains.com/issue/KT-59188) K2: Change positioning strategy for `WRONG_NUMBER_OF_TYPE_ARGUMENTS` error
- [`KT-59108`](https://youtrack.jetbrains.com/issue/KT-59108) K2. SMARTCAST_IMPOSSIBLE instead of UNSAFE_IMPLICIT_INVOKE_CALL
- [`KT-65503`](https://youtrack.jetbrains.com/issue/KT-65503) The inline processor cannot handle objects inside the lambda correctly when calling an inline function from another module
- [`KT-30696`](https://youtrack.jetbrains.com/issue/KT-30696) NoSuchMethodError if nested anonymous objects are used with propagation reified type parameter
- [`KT-58966`](https://youtrack.jetbrains.com/issue/KT-58966) Incorrect type inference for parameters with omitted type of anonymous function that is being analyzed as value of function type with receiver
- [`KT-67458`](https://youtrack.jetbrains.com/issue/KT-67458) Use `@PhaseDescription` for JVM backend lowering phases
- [`KT-65647`](https://youtrack.jetbrains.com/issue/KT-65647) K2 ignores diagnostics on sourceless `FirTypeRef`s
- [`KT-64489`](https://youtrack.jetbrains.com/issue/KT-64489) K2: Rename FirAugmentedArraySet
- [`KT-67394`](https://youtrack.jetbrains.com/issue/KT-67394) FIR: Make FIR repr of For from PSI and LightTree the same
- [`KT-60261`](https://youtrack.jetbrains.com/issue/KT-60261) K2: No origin is set for composite assignment operators
- [`KT-66724`](https://youtrack.jetbrains.com/issue/KT-66724) K2 IDE. False positive errors because of wrong type inference in complex case of delegated property and type arguments
- [`KT-40248`](https://youtrack.jetbrains.com/issue/KT-40248) Confusing error message NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY
- [`KT-66947`](https://youtrack.jetbrains.com/issue/KT-66947) K2: false-positive JSpecify nullability enhancement warning on Java wildcard type argument with same base type but different nullabilities as upper and lower bounds
- [`KT-66974`](https://youtrack.jetbrains.com/issue/KT-66974) K2: false-negative JSpecify nullability enhancement warning on nullable projection of Java wildcard type argument with non-null bounds in out-position
- [`KT-66946`](https://youtrack.jetbrains.com/issue/KT-66946) K2: false-negative JSpecify nullability enhancement warning on Java wildcard type argument with nullable upper bound in out-position
- [`KT-66442`](https://youtrack.jetbrains.com/issue/KT-66442) K2: No visibility error on importing private aliases
- [`KT-66598`](https://youtrack.jetbrains.com/issue/KT-66598) K2: Allow comparisons, `is`-checks and casts between Kotlin and platform types
- [`KT-55966`](https://youtrack.jetbrains.com/issue/KT-55966) K2: Not enough information to infer type variable K if smartcast is used
- [`KT-64957`](https://youtrack.jetbrains.com/issue/KT-64957) K1: drop ModuleAnnotationResolver
- [`KT-64894`](https://youtrack.jetbrains.com/issue/KT-64894) OPT_IN_ARGUMENT_IS_NOT_MARKER diagnostic message is unclear
- [`KT-67019`](https://youtrack.jetbrains.com/issue/KT-67019) K2: IR has incorrect EQ origins for some inplace updating operators
- [`KT-59810`](https://youtrack.jetbrains.com/issue/KT-59810) K2: Support other ConstraintPosition-s
- [`KT-55383`](https://youtrack.jetbrains.com/issue/KT-55383) K1/K2: isClassTypeConstructor behaves differently for stub types
- [`KT-60089`](https://youtrack.jetbrains.com/issue/KT-60089) K2: Introduced ERROR_IN_CONTRACT_DESCRIPTION
- [`KT-60382`](https://youtrack.jetbrains.com/issue/KT-60382) K2: Refactor ExpectActualCollector
- [`KT-62929`](https://youtrack.jetbrains.com/issue/KT-62929) K2: investigate if guessArrayTypeIfNeeded is necessary in annotation loader
- [`KT-65642`](https://youtrack.jetbrains.com/issue/KT-65642) K2: IR: Array access desugaring doesn't have origins
- [`KT-24807`](https://youtrack.jetbrains.com/issue/KT-24807) No smartcast to Boolean in subject of when-expression when subject type is non-nullable
- [`KT-66057`](https://youtrack.jetbrains.com/issue/KT-66057) K2: incorrect supertype leads to class declaration being highlighted red
- [`KT-63958`](https://youtrack.jetbrains.com/issue/KT-63958) K2: drop support of UseBuilderInferenceOnlyIfNeeded=false
- [`KT-63959`](https://youtrack.jetbrains.com/issue/KT-63959) K2: treat stub types as non-nullable for isReceiverNullable check
- [`KT-65100`](https://youtrack.jetbrains.com/issue/KT-65100) IrFakeOverrideBuilder: support custom 'remove(Int)' handling logic in MutableCollection subclasses

### Compose compiler

#### New features

- [`cdfe659`](https://github.com/JetBrains/kotlin/commit/cdfe65911490eef21892098494986af1af14fa64) Changed how compiler features being rolled out are enabled and disabled in compiler plugin CLI. Features, such as strong skipping and non-skipping group optimizations are now enabled through the  "featureFlag" option instead of their own option.

### IR. Actualizer

- [`KT-66307`](https://youtrack.jetbrains.com/issue/KT-66307) K2: property fake override isn't generated for protected field

### IR. Inlining

- [`KT-67660`](https://youtrack.jetbrains.com/issue/KT-67660) Suspicious package part FQN calculation in InventNamesForLocalClasses
- [`KT-67208`](https://youtrack.jetbrains.com/issue/KT-67208) KJS: put ReplaceSuspendIntrinsicLowering after IR inliner
- [`KT-64958`](https://youtrack.jetbrains.com/issue/KT-64958) KJS: Put as many as possible lowerings after the inliner
- [`KT-67297`](https://youtrack.jetbrains.com/issue/KT-67297) Implement IR deserializer with unbound symbols

### IR. Tree

- [`KT-67650`](https://youtrack.jetbrains.com/issue/KT-67650) Add default implementations to methods for non-leaf IrSymbol subclasses from SymbolRemapper
- [`KT-67649`](https://youtrack.jetbrains.com/issue/KT-67649) Autogenerate IrSymbol interface hierarchy
- [`KT-44721`](https://youtrack.jetbrains.com/issue/KT-44721) IR: merge IrPrivateSymbolBase and IrPublicSymbolBase hierarchies
- [`KT-67580`](https://youtrack.jetbrains.com/issue/KT-67580) Autogenerate SymbolRemapper
- [`KT-67457`](https://youtrack.jetbrains.com/issue/KT-67457) Introduce a way to simplify IR lowering phase creation

### JavaScript

#### New Features

- [`KT-18891`](https://youtrack.jetbrains.com/issue/KT-18891) JS: provide a way to declare static members (JsStatic?)

#### Fixes

- [`KT-68053`](https://youtrack.jetbrains.com/issue/KT-68053) K2: NON_EXPORTABLE_TYPE on a typealias of primitive type
- [`KT-68740`](https://youtrack.jetbrains.com/issue/KT-68740) Kotlin/JS 2.0.0 IrLinkageError with dynamic function parameters inside data classes
- [`KT-62304`](https://youtrack.jetbrains.com/issue/KT-62304) K/JS: Investigate the compiler assertion crash in JS FIR with backend tests
- [`KT-65018`](https://youtrack.jetbrains.com/issue/KT-65018) JS: Deprecate error tolerance
- [`KT-64801`](https://youtrack.jetbrains.com/issue/KT-64801) K2 + JS and WASM: Inner with default inner doesn't work properly
- [`KT-67248`](https://youtrack.jetbrains.com/issue/KT-67248) ModuleDescriptor in JS Linker contains incorrect friend dependecies
- [`KT-67273`](https://youtrack.jetbrains.com/issue/KT-67273) Creating Kotlin Collections from JS collections
- [`KT-64424`](https://youtrack.jetbrains.com/issue/KT-64424) K2: Migrate JsProtoComparisonTestGenerated to K2
- [`KT-52602`](https://youtrack.jetbrains.com/issue/KT-52602) Kotlin/JS + IR: incompatible ABI version is not reported when no declarations are actually used by a Gradle compilation
- [`KT-66092`](https://youtrack.jetbrains.com/issue/KT-66092) K/JS & Wasm: .isReified for reified upper bound is wrongly false
- [`KT-67112`](https://youtrack.jetbrains.com/issue/KT-67112) Unable to apply `@JsStatic` for common sources: [NO_CONSTRUCTOR]
- [`KT-62329`](https://youtrack.jetbrains.com/issue/KT-62329) KJS: "UnsupportedOperationException: Empty collection can't be reduced" caused by external enum with "`@JsExport`"
- [`KT-67018`](https://youtrack.jetbrains.com/issue/KT-67018) K/JS: Executable js file for module-kind=umd contains top level this instead of globalThis
- [`KT-64776`](https://youtrack.jetbrains.com/issue/KT-64776) Test infra for JS can't process dependency in mpp module
- [`KT-65076`](https://youtrack.jetbrains.com/issue/KT-65076) Use the same instance when a fun interface doesn't capture or capture only singletons

### Klibs

- [`KT-68202`](https://youtrack.jetbrains.com/issue/KT-68202) KLIB metadata: nested classes are sometimes inside a different 'knm' chunk
- [`KT-66968`](https://youtrack.jetbrains.com/issue/KT-66968) Provide K/N platforms libs for all available targets
- [`KT-66967`](https://youtrack.jetbrains.com/issue/KT-66967) Provide K/N stdlib for all available targets in all distributions
- [`KT-65834`](https://youtrack.jetbrains.com/issue/KT-65834) [KLIB Resolve] Drop library versions in KLIB manifests
- [`KT-67446`](https://youtrack.jetbrains.com/issue/KT-67446) [KLIB Tool] Drop "-repository <path>" CLI parameter
- [`KT-67445`](https://youtrack.jetbrains.com/issue/KT-67445) [KLIB Tool] Drop "install" and "remove" commands
- [`KT-66557`](https://youtrack.jetbrains.com/issue/KT-66557) Check, that no bad metadata in klib is produced, when we failed to compute constant value

### Language Design

- [`KT-11914`](https://youtrack.jetbrains.com/issue/KT-11914) Confusing data class copy with private constructor

### Libraries

- [`KT-51483`](https://youtrack.jetbrains.com/issue/KT-51483) Documentation of trimMargin is (partly) difficult to understand
- [`KT-64649`](https://youtrack.jetbrains.com/issue/KT-64649) Add explanation to "A compileOnly dependency is used in the Kotlin/Native target" warning message
- [`KT-67807`](https://youtrack.jetbrains.com/issue/KT-67807) JS/Wasm: ByteArray.decodeToString incorrectly handles ill-formed 4-byte sequences with a 2nd byte not being continuation byte
- [`KT-67768`](https://youtrack.jetbrains.com/issue/KT-67768) Wasm: ByteArray.decodeToString throws out-of-bounds exception if the last byte is a start of a 4-byte sequence
- [`KT-66896`](https://youtrack.jetbrains.com/issue/KT-66896) Improve Array contentEquals and contentDeepEquals documentation

### Native

- [`KT-68094`](https://youtrack.jetbrains.com/issue/KT-68094) K2/Native: Member inherits different '`@Throws`' when inheriting from generic type
- [`KT-67583`](https://youtrack.jetbrains.com/issue/KT-67583) compileKotlin-task unexpectedly downloads K/N dependencies on Linux (but doesn't on Mac)

### Native. C and ObjC Import

- [`KT-65260`](https://youtrack.jetbrains.com/issue/KT-65260) Native: compiler crashes when casting to an Obj-C class companion

### Native. ObjC Export

- [`KT-65666`](https://youtrack.jetbrains.com/issue/KT-65666) Native: enable objcExportSuspendFunctionLaunchThreadRestriction=none by default

### Native. Runtime. Memory

- [`KT-67779`](https://youtrack.jetbrains.com/issue/KT-67779) Native: SpecialRefRegistry::ThradData publication prolongs the pause in CMS
- [`KT-66644`](https://youtrack.jetbrains.com/issue/KT-66644) Native: threads are too often paused to assist GC (with concurrent mark)
- [`KT-66918`](https://youtrack.jetbrains.com/issue/KT-66918) Native: scan global root set concurrently

### Native. Swift Export

- [`KT-68259`](https://youtrack.jetbrains.com/issue/KT-68259) Swift export: secondary constructs lead to compilation errors
- [`KT-67095`](https://youtrack.jetbrains.com/issue/KT-67095) Native: fix testNativeRefs export test
- [`KT-67099`](https://youtrack.jetbrains.com/issue/KT-67099) Remove SirVisitor and SirTransformer from code
- [`KT-67003`](https://youtrack.jetbrains.com/issue/KT-67003) Abandon PackageInflator implementation in favour of PackageProvider component

### Native. Testing

- [`KT-68500`](https://youtrack.jetbrains.com/issue/KT-68500) Native: Drop custom logic in ExtTestCaseGroupProvider, mute codegen/box tests explicitly

### Tools. CLI

- [`KT-67939`](https://youtrack.jetbrains.com/issue/KT-67939) Add CLI argument to enable when guards feature
- [`KT-68060`](https://youtrack.jetbrains.com/issue/KT-68060) FastJarFS fails on empty jars

### Tools. CLI. Native

- [`KT-64524`](https://youtrack.jetbrains.com/issue/KT-64524) Introduce a CLI argument to override native_targets field in klib manifest

### Tools. Compiler Plugin API

- [`KT-68020`](https://youtrack.jetbrains.com/issue/KT-68020) K2: run FirSupertypeGenerationExtension over generated declarations

### Tools. Compiler Plugins

- [`KT-67605`](https://youtrack.jetbrains.com/issue/KT-67605) K2 parcelize: false positive NOTHING_TO_OVERRIDE in one test
- [`KT-64455`](https://youtrack.jetbrains.com/issue/KT-64455) K2: Implement ParcelizeIrBoxTestWithSerializableLikeExtension for K2

### Tools. Fleet. ObjC Export

- [`KT-68051`](https://youtrack.jetbrains.com/issue/KT-68051) [ObjCExport] Support reserved method names

### Tools. Gradle

- [`KT-68447`](https://youtrack.jetbrains.com/issue/KT-68447) ill-added intentionally-broken dependency source configurations
- [`KT-68278`](https://youtrack.jetbrains.com/issue/KT-68278) Spring resource loading in combination with `java-test-fixtures` plugin broken
- [`KT-66452`](https://youtrack.jetbrains.com/issue/KT-66452) Gradle produces false positive configuration cache problem for Project usage at execution time
- [`KT-68242`](https://youtrack.jetbrains.com/issue/KT-68242) Run tests against AGP 8.4.0
- [`KT-61574`](https://youtrack.jetbrains.com/issue/KT-61574) Add project-isolation test for Kotlin/Android plugin
- [`KT-65936`](https://youtrack.jetbrains.com/issue/KT-65936) Provide a detailed error for changing kotlin native version dependency.
- [`KT-67888`](https://youtrack.jetbrains.com/issue/KT-67888) Remove usages of deprecated Configuration.fileCollection() method
- [`KT-62684`](https://youtrack.jetbrains.com/issue/KT-62684) PropertiesBuildService should load extraProperties only once
- [`KT-67288`](https://youtrack.jetbrains.com/issue/KT-67288) Test DSL should not fail the test if build scan publishing has failed

### Tools. Gradle. JS

- [`KT-68482`](https://youtrack.jetbrains.com/issue/KT-68482) KotlinNpmInstallTask is not compatible with configuration cache
- [`KT-68072`](https://youtrack.jetbrains.com/issue/KT-68072) K/JS, K/Wasm: Module not found in transitive case
- [`KT-68103`](https://youtrack.jetbrains.com/issue/KT-68103) K/JS, K/Wasm: Generation of test compilation's package.json requires main compilation
- [`KT-67924`](https://youtrack.jetbrains.com/issue/KT-67924) K/JS, K/Wasm: kotlinNpmInstall can rewrite root package.json

### Tools. Gradle. Kapt

- [`KT-64627`](https://youtrack.jetbrains.com/issue/KT-64627) Kapt3KotlinGradleSubplugin uses property lookup that breaks project isolation

### Tools. Gradle. Multiplatform

- [`KT-64109`](https://youtrack.jetbrains.com/issue/KT-64109) Using compileOnly/runtimeOnly dependencies in K/N-related configurations leads to odd behaviour
- [`KT-58319`](https://youtrack.jetbrains.com/issue/KT-58319) kotlin.git: ProjectMetadataProviderImpl "Unexpected source set 'commonMain'"

### Tools. Gradle. Native

- [`KT-65761`](https://youtrack.jetbrains.com/issue/KT-65761) Missing JDK Platform ClassLoader when compiling Kotlin native in daemon
- [`KT-67935`](https://youtrack.jetbrains.com/issue/KT-67935) OverriddenKotlinNativeHomeChecker does not work well with relative paths
- [`KT-64430`](https://youtrack.jetbrains.com/issue/KT-64430) Remove deprecated KotlinToolRunner(project) constructor
- [`KT-64427`](https://youtrack.jetbrains.com/issue/KT-64427) Stop using deprecated KotlinToolRunner(project) constructor call

### Tools. Incremental Compile

- [`KT-63476`](https://youtrack.jetbrains.com/issue/KT-63476) Investigate the debug output of JVM compilation in KMP IC smoke tests

### Tools. JPS

- [`KT-63707`](https://youtrack.jetbrains.com/issue/KT-63707) JPS: "Multiple values are not allowed for" caused by Compose

### Tools. Kapt

- [`KT-67495`](https://youtrack.jetbrains.com/issue/KT-67495) File leak in when building with kapt
- [`KT-66780`](https://youtrack.jetbrains.com/issue/KT-66780) K2 KAPT Kotlinc should exit with an exit code 1 (compilation error) if a Kapt task fails
- [`KT-66998`](https://youtrack.jetbrains.com/issue/KT-66998) K2 KAPT: Reimplement support for DefaultImpls

### Tools. Scripts

- [`KT-67575`](https://youtrack.jetbrains.com/issue/KT-67575) FromConfigurationsBase script definition unexpected behaviour with regex from gradle templates
- [`KT-67066`](https://youtrack.jetbrains.com/issue/KT-67066) DeepCopyIrTreeWithSymbols does not copy IrScript nodes correctly
- [`KT-67071`](https://youtrack.jetbrains.com/issue/KT-67071) K2: ScriptCompilationConfigurationFromDefinition is not serializable
- [`KT-67063`](https://youtrack.jetbrains.com/issue/KT-67063) LauncherReplTest flaky on Windows

### Tools. Wasm

- [`KT-67468`](https://youtrack.jetbrains.com/issue/KT-67468) Gradle task build (allTests) fails on default web project
- [`KT-67862`](https://youtrack.jetbrains.com/issue/KT-67862) K/Wasm: Make usage of ChromeWasmGc an error

## Previous ChangeLogs:
### [ChangeLog-2.0.X](docs/changelogs/ChangeLog-2.0.X.md)
### [ChangeLog-1.9.X](docs/changelogs/ChangeLog-1.9.X.md)
### [ChangeLog-1.8.X](docs/changelogs/ChangeLog-1.8.X.md)
### [ChangeLog-1.7.X](docs/changelogs/ChangeLog-1.7.X.md)
### [ChangeLog-1.6.X](docs/changelogs/ChangeLog-1.6.X.md)
### [ChangeLog-1.5.X](docs/changelogs/ChangeLog-1.5.X.md)
### [ChangeLog-1.4.X](docs/changelogs/ChangeLog-1.4.X.md)
### [ChangeLog-1.3.X](docs/changelogs/ChangeLog-1.3.X.md)
### [ChangeLog-1.2.X](docs/changelogs/ChangeLog-1.2.X.md)
### [ChangeLog-1.1.X](docs/changelogs/ChangeLog-1.1.X.md)
### [ChangeLog-1.0.X](docs/changelogs/ChangeLog-1.0.X.md)