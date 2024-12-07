## 2.0.21

### Apple Ecosystem

- [`KT-69093`](https://youtrack.jetbrains.com/issue/KT-69093) Xcode 16 support in Kotlin

### Backend. Native. Debug

- [`KT-71374`](https://youtrack.jetbrains.com/issue/KT-71374) lldb: step out breaks breaking in Xcode 16

### Compiler

#### Fixes

- [`KT-69735`](https://youtrack.jetbrains.com/issue/KT-69735) K2: Static fields are missing from the declaration list of corresponding IrClass for java class
- [`KT-71122`](https://youtrack.jetbrains.com/issue/KT-71122) Regression in Kotlin Compiler 2.0 causing NPE in the runtime
- [`KT-70931`](https://youtrack.jetbrains.com/issue/KT-70931) K2 / Scripts: "cannot convert IrExpression to ConstantValue" when using function annotation
- [`KT-70584`](https://youtrack.jetbrains.com/issue/KT-70584) K2: "IllegalStateException: flow for PostponedLambdaExitNode not initialized - traversing nodes in wrong order?"
- [`KT-70808`](https://youtrack.jetbrains.com/issue/KT-70808) K2: "node has already been visited" with anonymous object in dead code
- [`KT-69985`](https://youtrack.jetbrains.com/issue/KT-69985) K2: Classifier declarations from root package are resolved without imports in non-root packages
- [`KT-70683`](https://youtrack.jetbrains.com/issue/KT-70683) K2: Internal compiler error in IrFakeOverrideSymbolBase.getOwner
- [`KT-70901`](https://youtrack.jetbrains.com/issue/KT-70901) False positive Public-API inline function cannot access non-public-API property accessor
- [`KT-70930`](https://youtrack.jetbrains.com/issue/KT-70930) K2: Java annotations not present on ENUM_ENTRY IR elements
- [`KT-70194`](https://youtrack.jetbrains.com/issue/KT-70194) K2 IDE: exception on a very red file
- [`KT-69399`](https://youtrack.jetbrains.com/issue/KT-69399) Native: IllegalStateException: "Failed to build cache"

### Compose compiler

#### Fixes
- [`b/329477544`](https://issuetracker.google.com/issues/329477544) Force open / overridden Composable functions to be non-restartable.
- [`b/361652128`](https://issuetracker.google.com/issues/361652128) Disable live literal transform if the corresponding flag is disabled

### IR. Actualizer

- [`KT-70894`](https://youtrack.jetbrains.com/issue/KT-70894) IR crash. Unprocessed `IrFunctionFakeOverrideSymbol` when actualize to Java

### IR. Tree

- [`KT-71191`](https://youtrack.jetbrains.com/issue/KT-71191) SymbolTable: Check if the provided signature is public before adding a symbol to the SymbolTable

### Native. Build Infrastructure

- [`KT-71485`](https://youtrack.jetbrains.com/issue/KT-71485) K/N runtime parts don't build due to _Float16 issues on x86_64 macOS

### Native. C Export

- [`KT-69507`](https://youtrack.jetbrains.com/issue/KT-69507) LLVM 11 clang with Xcode 16 headers: standard c++ headers

### Native. C and ObjC Import

- [`KT-71029`](https://youtrack.jetbrains.com/issue/KT-71029) Investigate why stdarg.h declarations leak into testModuleA

### Native. Platform Libraries

- [`KT-70566`](https://youtrack.jetbrains.com/issue/KT-70566) LLVM 11 clang with Xcode 16 headers: 'sys/cdefs.h' file not found
- [`KT-71624`](https://youtrack.jetbrains.com/issue/KT-71624) Eliminate remaining UIKit/AppKit removed signatures
- [`KT-70031`](https://youtrack.jetbrains.com/issue/KT-70031) Rebuild platform libraries in 2.0.21 with Xcode 16

### Native. Testing

- [`KT-70603`](https://youtrack.jetbrains.com/issue/KT-70603) C++ with -fmodules: cyclic dependency in module 'std': std -> _wctype -> __wctype -> std in dev llvm toolchains

### Tools. CLI. Native

- [`KT-71262`](https://youtrack.jetbrains.com/issue/KT-71262) KotlinNativeLink tasks fetching from network despite -Xoverride-konan-properties=dependenciesUrl= being set

### Tools. Compiler Plugins

- [`KT-71038`](https://youtrack.jetbrains.com/issue/KT-71038) PowerAssert: Constant on RHS of elvis operator leads to compiler crash

### Tools. Daemon

- [`KT-35381`](https://youtrack.jetbrains.com/issue/KT-35381) Get rid of the native-platform usage in kotlin compiler

### Tools. Gradle. Multiplatform

- [`KT-71444`](https://youtrack.jetbrains.com/issue/KT-71444) Certain POMs produced by Kotlin 2.0.20 cannot be consumed by KMP projects with Android targets
- [`KT-70700`](https://youtrack.jetbrains.com/issue/KT-70700) Gradle 8.10: The value for task ':commonizeNativeDistribution' property 'kotlinNativeBundleBuildService' cannot be changed any further
- [`KT-71396`](https://youtrack.jetbrains.com/issue/KT-71396) Gradle client side JVM explodes with OOM due to xcodebuild logs

### Tools. Gradle. Native

- [`KT-71419`](https://youtrack.jetbrains.com/issue/KT-71419) Light bundle KGP IT run against a stable K/N version

### Tools. JPS

- [`KT-71450`](https://youtrack.jetbrains.com/issue/KT-71450) Constant build failure: com.intellij.util.io.ClosedStorageException: storage is already closed


## 2.0.20

### Analysis. API

#### New Features

- [`KT-68143`](https://youtrack.jetbrains.com/issue/KT-68143) Analysis API: support KtWhenConditionInRange call resolution

#### Performance Improvements

- [`KT-67195`](https://youtrack.jetbrains.com/issue/KT-67195) K2: do not call redundant resolve on body resolution phase for classes

#### Fixes

- [`KT-67360`](https://youtrack.jetbrains.com/issue/KT-67360) Analysis API: KtDestructuringDeclarationSymbol#entries shouldn't be KtLocalVariableSymbol
- [`KT-67748`](https://youtrack.jetbrains.com/issue/KT-67748) K2: AllCandidatesResolver modifies the original FirDelegatedConstructorCall
- [`KT-68198`](https://youtrack.jetbrains.com/issue/KT-68198) Analysis API: Support application service registration in plugin XMLs
- [`KT-62936`](https://youtrack.jetbrains.com/issue/KT-62936) Analysis API: NativeForwardDeclarationsSymbolProvider is not supported for Kotlin/Native
- [`KT-68689`](https://youtrack.jetbrains.com/issue/KT-68689) LL API: support analysis from builtins module
- [`KT-69630`](https://youtrack.jetbrains.com/issue/KT-69630) KAPT User project builds with KAPT4 enabled fail with Metaspace overflow
- [`KT-65417`](https://youtrack.jetbrains.com/issue/KT-65417) K2 IDE: KTOR false positive expect-actual matching error on enum class because of implicit clone() in non-JVM source sets
- [`KT-68882`](https://youtrack.jetbrains.com/issue/KT-68882) Analysis API: Refactor `KaSymbol`s
- [`KT-65413`](https://youtrack.jetbrains.com/issue/KT-65413) K2 IDE: KTOR unresolved serializer() call for `@Serializable` class in common code
- [`KT-67996`](https://youtrack.jetbrains.com/issue/KT-67996) Analysis API: rename Kt prefix to Ka
- [`KT-67775`](https://youtrack.jetbrains.com/issue/KT-67775) Analysis API: expose only interfaces/abstract classes for the user surface
- [`KT-68009`](https://youtrack.jetbrains.com/issue/KT-68009) K2: lowering transformers of Compose compiler plugin access AbstractFir2IrLazyFunction modality, which results in null point exception
- [`KT-68918`](https://youtrack.jetbrains.com/issue/KT-68918) collectCallCandidates works incorrectly for parenthesis invoke
- [`KT-68462`](https://youtrack.jetbrains.com/issue/KT-68462) Analysis API: Integrate `project-structure` module into `analysis-api` and `analysis-api-platform-interface`
- [`KT-69131`](https://youtrack.jetbrains.com/issue/KT-69131) AA: "provideDelegate" operator is not resolved from the delegation reference in FIR implementation
- [`KT-69055`](https://youtrack.jetbrains.com/issue/KT-69055) Analysis API: Stabilize `KaScope`s
- [`KT-66216`](https://youtrack.jetbrains.com/issue/KT-66216) K2 IDE. "FirDeclaration was not found for class org.jetbrains.kotlin.psi.KtProperty, fir is null" on incorrect string template
- [`KT-68959`](https://youtrack.jetbrains.com/issue/KT-68959) Introduce KaSeverity
- [`KT-53669`](https://youtrack.jetbrains.com/issue/KT-53669) Analysis API: redesign KtSymbolOrigin to distinguish kotlin/java source/library declarations
- [`KT-68846`](https://youtrack.jetbrains.com/issue/KT-68846) Mark KaFirReference and all implementations with internal modifier
- [`KT-68845`](https://youtrack.jetbrains.com/issue/KT-68845) Move KaSymbolBasedReference to resolution package
- [`KT-68844`](https://youtrack.jetbrains.com/issue/KT-68844) Move KaTypeProjection to types package
- [`KT-65849`](https://youtrack.jetbrains.com/issue/KT-65849) K2: Rename 'high-level-api' family of JARs to 'analysis-api'
- [`KT-62540`](https://youtrack.jetbrains.com/issue/KT-62540) Remove uses of TypeInfo.fromString and TypeInfo.createTypeText from Kotlin plugin
- [`KT-62889`](https://youtrack.jetbrains.com/issue/KT-62889) K2 IDE. FP `MISSING_DEPENDENCY_CLASS` on not available type alias with available underlying type
- [`KT-68155`](https://youtrack.jetbrains.com/issue/KT-68155) Analysis API: Add PSI validity check to `analyze`
- [`KT-62343`](https://youtrack.jetbrains.com/issue/KT-62343) Analysis API: fix binary incopatibility problems cause by `KtAnalysisSessionProvider.analyze` being inline
- [`KT-68498`](https://youtrack.jetbrains.com/issue/KT-68498) To get reference symbol the one should be KtSymbolBasedReference
- [`KT-68393`](https://youtrack.jetbrains.com/issue/KT-68393) Analysis API: Rename `KaClassLikeSymbol. classIdIfNonLocal` to `classId`
- [`KT-62924`](https://youtrack.jetbrains.com/issue/KT-62924) Analysis API: rename KtCallableSymbol.callableIdIfNonLocal -> callableId
- [`KT-66712`](https://youtrack.jetbrains.com/issue/KT-66712) K2 IDE. SOE on settings string template for string variable with the same name
- [`KT-65892`](https://youtrack.jetbrains.com/issue/KT-65892) K2: "We should be able to find a symbol" for findNonLocalFunction
- [`KT-68273`](https://youtrack.jetbrains.com/issue/KT-68273) AA: support `KtFirKDocReference#isReferenceToImportAlias`
- [`KT-68272`](https://youtrack.jetbrains.com/issue/KT-68272) AA: KtFirReference.isReferenceToImportAlias doesn't work for references on constructor
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
- [`KT-67743`](https://youtrack.jetbrains.com/issue/KT-67743) K2: Stubs & AbbreviatedTypeAttribute
- [`KT-67706`](https://youtrack.jetbrains.com/issue/KT-67706) K2: "KtDotQualifiedExpression is not a subtype of class KtNamedDeclaration" from UnusedChecker
- [`KT-68021`](https://youtrack.jetbrains.com/issue/KT-68021) Analysis API: do not break the diagnostic collection in a case of exception from some collector
- [`KT-67973`](https://youtrack.jetbrains.com/issue/KT-67973) AA FIR: wrong KtCall modeling for == from type bound
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
- [`KT-66713`](https://youtrack.jetbrains.com/issue/KT-66713) K2 FIR: Expose a way to get the module name used for name mangling
- [`KT-61892`](https://youtrack.jetbrains.com/issue/KT-61892) KtType#asPsiType could provide nullability annotations
- [`KT-66122`](https://youtrack.jetbrains.com/issue/KT-66122) Analysis API: Pass `KtTestModule` instead of `TestModule` to tests based on `AbstractAnalysisApiBasedTest`

### Analysis. Light Classes

- [`KT-65714`](https://youtrack.jetbrains.com/issue/KT-65714) K2: IDE K2: "org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirClassLikeSymbolPointer pointer already disposed"
- [`KT-65835`](https://youtrack.jetbrains.com/issue/KT-65835) `SymbolLightClassForClassLike.getName` returns `null` for a companion object instead of `Companion`
- [`KT-68261`](https://youtrack.jetbrains.com/issue/KT-68261) SLC: Constructors of sealed classes should be private
- [`KT-68696`](https://youtrack.jetbrains.com/issue/KT-68696) Drop `DecompiledPsiDeclarationProvider`-related stuff
- [`KT-68404`](https://youtrack.jetbrains.com/issue/KT-68404) SLC: wrong binary resolution to declaration with `@JvmName`
- [`KT-68275`](https://youtrack.jetbrains.com/issue/KT-68275) LC: no arg constructor is not visible in light classes
- [`KT-66687`](https://youtrack.jetbrains.com/issue/KT-66687) Symbol Light Classes: Duplicate field names for classes with companion objects
- [`KT-66804`](https://youtrack.jetbrains.com/issue/KT-66804) Symbol Light Classes: Fields from the parent interface's companion are added to DefaultImpls

### Apple Ecosystem

- [`KT-65542`](https://youtrack.jetbrains.com/issue/KT-65542) Cinterop tasks fails if Xcode 15.3 is used

### Backend. Native. Debug

- [`KT-67567`](https://youtrack.jetbrains.com/issue/KT-67567) Native: after updating to LLVM 16 lldb hangs when smooth stepping

### Backend. Wasm

- [`KT-70591`](https://youtrack.jetbrains.com/issue/KT-70591) To much sources that don't exist inside SourceMap file
- [`KT-69529`](https://youtrack.jetbrains.com/issue/KT-69529) compileProductionExecutableKotlinWasmJs FAILED: No such value argument slot in IrConstructorCallImpl: 1 (total=1)
- [`KT-68088`](https://youtrack.jetbrains.com/issue/KT-68088) Wasm: "UNREACHABLE executed at Precompute.cpp:838" running gradle task wasmJsBrowserDistribution for compose multiplatform on Windows
- [`KT-65798`](https://youtrack.jetbrains.com/issue/KT-65798) K/Wasm: make an error on default export usage
- [`KT-68828`](https://youtrack.jetbrains.com/issue/KT-68828) Wasm test failure. expect-actual. private constructor in expect
- [`KT-68453`](https://youtrack.jetbrains.com/issue/KT-68453) K/Wasm: "Supported JS engine not detected" in Web Worker
- [`KT-64565`](https://youtrack.jetbrains.com/issue/KT-64565) Kotlin/wasm removeEventListener function did not remove the event listener
- [`KT-65322`](https://youtrack.jetbrains.com/issue/KT-65322) [Wasm] Clean-up bootstrap code
- [`KT-66099`](https://youtrack.jetbrains.com/issue/KT-66099) Wasm: local.get of type f64 has to be in the same reference type hierarchy as (ref 686) @+237036
- [`KT-63230`](https://youtrack.jetbrains.com/issue/KT-63230) [WASM] `println(null)` prints 'ul'

### Compiler

#### New Features

- [`KT-58310`](https://youtrack.jetbrains.com/issue/KT-58310) Consider non-functional type constraints for type variable which is an expected type for lambda argument
- [`KT-68969`](https://youtrack.jetbrains.com/issue/KT-68969) Consider implementing general "redundant interpolation prefix" warning
- [`KT-57872`](https://youtrack.jetbrains.com/issue/KT-57872) Improve "Public-API inline function cannot access non-public-API" check
- [`KT-68165`](https://youtrack.jetbrains.com/issue/KT-68165) Native: type checks on generic types boundary
- [`KT-67611`](https://youtrack.jetbrains.com/issue/KT-67611) Implement improved handling of $ in literals
- [`KT-67787`](https://youtrack.jetbrains.com/issue/KT-67787) Implement guard conditions for when-with-subject
- [`KT-39868`](https://youtrack.jetbrains.com/issue/KT-39868) Allow access to protected consts and fields from a super companion object
- [`KT-66169`](https://youtrack.jetbrains.com/issue/KT-66169) `useContents` lacks a `contract`
- [`KT-67767`](https://youtrack.jetbrains.com/issue/KT-67767) Introduce an ability to enforce explicit return types for public declarations without enabling Explicit API mode
- [`KT-65841`](https://youtrack.jetbrains.com/issue/KT-65841) Allow to actualize expect types in kotlin stdlib to builtins in JVM
- [`KT-53834`](https://youtrack.jetbrains.com/issue/KT-53834) Support for JSpecify `@NullUnmarked`

#### Performance Improvements

- [`KT-69995`](https://youtrack.jetbrains.com/issue/KT-69995) K2: Slow compilation when star projecting mutually recursive bounds from java
- [`KT-69723`](https://youtrack.jetbrains.com/issue/KT-69723) K2: code analysis taking too long
- [`KT-69898`](https://youtrack.jetbrains.com/issue/KT-69898) K2: Performance degradation in fir2ir caused by changes around intersection types
- [`KT-68034`](https://youtrack.jetbrains.com/issue/KT-68034) Devirtualization analysis fails to devirtualize string.get

#### Fixes

- [`KT-67102`](https://youtrack.jetbrains.com/issue/KT-67102) IR Evaluator: NoSuchFieldException when accessing a private delegated property
- [`KT-35305`](https://youtrack.jetbrains.com/issue/KT-35305) "Overload resolution ambiguity" on function for unsigned types (UByte, UShort, UInt, ULong)
- [`KT-69211`](https://youtrack.jetbrains.com/issue/KT-69211) K2: java.lang.IllegalArgumentException: Failed requirement
- [`KT-68874`](https://youtrack.jetbrains.com/issue/KT-68874) Types with different captured types as type arguments are rendered incorrectly
- [`KT-66086`](https://youtrack.jetbrains.com/issue/KT-66086) K/N: Unchecked illegal cast is not thrown
- [`KT-70186`](https://youtrack.jetbrains.com/issue/KT-70186) Kotlin 2.0.20-Beta2: Unexpected number of type arguments: 0
- [`KT-68889`](https://youtrack.jetbrains.com/issue/KT-68889) K2: type variable should not be fixed
- [`KT-69835`](https://youtrack.jetbrains.com/issue/KT-69835) K2 / Native: kotlin.native.binary.gc=cms throws library cached but its dependency isn't error
- [`KT-70417`](https://youtrack.jetbrains.com/issue/KT-70417) DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE leads to NPE in BE
- [`KT-69964`](https://youtrack.jetbrains.com/issue/KT-69964) K2: Returning from an in-place lambda doesn't compile
- [`KT-69773`](https://youtrack.jetbrains.com/issue/KT-69773) K2: "Overload resolution ambiguity between candidate" with arrays
- [`KT-60261`](https://youtrack.jetbrains.com/issue/KT-60261) K2: No origin is set for composite assignment operators
- [`KT-15388`](https://youtrack.jetbrains.com/issue/KT-15388) Forbid delegated property to have external getter/setter
- [`KT-70238`](https://youtrack.jetbrains.com/issue/KT-70238) K2: false negative VOLATILE_ON_VALUE for constructor properties
- [`KT-68669`](https://youtrack.jetbrains.com/issue/KT-68669) K2: Generate inherited delegated members after actualization
- [`KT-63828`](https://youtrack.jetbrains.com/issue/KT-63828) K2: Missing `signature` metadata for accessors of properties inherited from delegate
- [`KT-63871`](https://youtrack.jetbrains.com/issue/KT-63871) K2: different value of `isNotDefault ` flag for property inherited from delegate
- [`KT-67119`](https://youtrack.jetbrains.com/issue/KT-67119) Migration warning from context receivers to context parameters
- [`KT-68997`](https://youtrack.jetbrains.com/issue/KT-68997) K2: "No accessor found" for an inline value class when query the value of a delegated class by reflection
- [`KT-64106`](https://youtrack.jetbrains.com/issue/KT-64106) Native: the compiler allows using `-opt` and `-g` at the same time
- [`KT-69766`](https://youtrack.jetbrains.com/issue/KT-69766) K2: False negative: Internal setter of generic class is accessible from another module
- [`KT-68364`](https://youtrack.jetbrains.com/issue/KT-68364) JVM: ISE "Bad exception handler end" on a non-local break/continue inside try with finally
- [`KT-69494`](https://youtrack.jetbrains.com/issue/KT-69494) StackOverflowError in CfgTraverserKt.getPreviousCfgNodes
- [`KT-56880`](https://youtrack.jetbrains.com/issue/KT-56880) K2. Conflicting overloads for main() isn't shown when language version is set to 2.0
- [`KT-69282`](https://youtrack.jetbrains.com/issue/KT-69282) K2: equality of unsigned types with nullability works incorrectly
- [`KT-68492`](https://youtrack.jetbrains.com/issue/KT-68492) JVM IR backend: IDE / Kotlin Debugger: AE “Non-reified type parameter under ::class should be rejected by type checker” on evaluating private generic function
- [`KT-70039`](https://youtrack.jetbrains.com/issue/KT-70039) K2: inconsistent stability of vals of captured receivers
- [`KT-44139`](https://youtrack.jetbrains.com/issue/KT-44139) Don't report overload resolution ambiguities if arguments contain an error type
- [`KT-68996`](https://youtrack.jetbrains.com/issue/KT-68996) K2: "Not enough information to infer type argument" caused by typealias annotation with fixed generic argument
- [`KT-55851`](https://youtrack.jetbrains.com/issue/KT-55851) K2: reference to a field from package private class crashes in runtime
- [`KT-65038`](https://youtrack.jetbrains.com/issue/KT-65038) K2: Type alias from indirect dependency causes `MISSING_DEPENDENCY_CLASS` error
- [`KT-61875`](https://youtrack.jetbrains.com/issue/KT-61875) Native: remove support for bitcode embedding
- [`KT-67693`](https://youtrack.jetbrains.com/issue/KT-67693) Implement checkers for K1 compiler which will check the usage of K2 new features and report that they are not supported in K1 compiler
- [`KT-68556`](https://youtrack.jetbrains.com/issue/KT-68556) K2: false negative PROPERTY_WITH_NO_TYPE_NO_INITIALIZER on uninitialized property without type
- [`KT-60445`](https://youtrack.jetbrains.com/issue/KT-60445) K2/Java: investigate possible symbol clash while enhancing Java class type parameter bounds
- [`KT-64193`](https://youtrack.jetbrains.com/issue/KT-64193) K2: No smartcast with two boolean expressions in a row
- [`KT-65546`](https://youtrack.jetbrains.com/issue/KT-65546) K2. implement extended checker for unused anonymous parameter in lambda
- [`KT-68358`](https://youtrack.jetbrains.com/issue/KT-68358) `@EnhancedNullability` is missing on value parameter type after inheritance by delegation with strict JSpecify enabled
- [`KT-67791`](https://youtrack.jetbrains.com/issue/KT-67791) False negative "Synchronizing by Meters is forbidden" with inline value classes
- [`KT-69495`](https://youtrack.jetbrains.com/issue/KT-69495) k2: inconsistent output of unsigned number in string templates
- [`KT-69619`](https://youtrack.jetbrains.com/issue/KT-69619) K2. JAVA_TYPE_MISMATCH when Kotlin out generic type used in Java
- [`KT-69563`](https://youtrack.jetbrains.com/issue/KT-69563) trying to call `.source` on `FirPackageFragmentDescriptor` results in exception
- [`KT-69611`](https://youtrack.jetbrains.com/issue/KT-69611) Internal annotation FlexibleArrayElementVariance is written to output jar
- [`KT-69463`](https://youtrack.jetbrains.com/issue/KT-69463) K2: false negative SUPER_CALL_WITH_DEFAULT_PARAMETERS  with expect/actual declarations
- [`KT-68724`](https://youtrack.jetbrains.com/issue/KT-68724) K2: "ABSTRACT_MEMBER_NOT_IMPLEMENTED" caused by open modifier on interface
- [`KT-69182`](https://youtrack.jetbrains.com/issue/KT-69182) K2: OptIn on enum companion blocks enum constants
- [`KT-69191`](https://youtrack.jetbrains.com/issue/KT-69191) K2: "Unresolved reference" caused by nested data objects
- [`KT-69569`](https://youtrack.jetbrains.com/issue/KT-69569) Wrong paths when one type has multiple annotated arguments
- [`KT-55128`](https://youtrack.jetbrains.com/issue/KT-55128) Wrong type path in type annotations when type arguments are compiled to wildcards
- [`KT-67692`](https://youtrack.jetbrains.com/issue/KT-67692) Native: support LLVM opaque pointers in the compiler
- [`KT-69402`](https://youtrack.jetbrains.com/issue/KT-69402) FirSupertypeResolverVisitor: ConcurrentModificationException
- [`KT-69062`](https://youtrack.jetbrains.com/issue/KT-69062) K1: false-negative "unsupported feature" error on multi-dollar interpolation
- [`KT-68967`](https://youtrack.jetbrains.com/issue/KT-68967) Consider demoting warnings about multi-dollar interpolation to IJ inspections
- [`KT-68957`](https://youtrack.jetbrains.com/issue/KT-68957) False-negative diagnostics about multi-dollar interpolation on string literals without interpolation
- [`KT-69476`](https://youtrack.jetbrains.com/issue/KT-69476) False negative NO_ELSE_IN_WHEN on when over intersection type with expect enum/sealed class
- [`KT-67069`](https://youtrack.jetbrains.com/issue/KT-67069) K2: Delegated member calls interface method instead of fake override
- [`KT-63864`](https://youtrack.jetbrains.com/issue/KT-63864) K2: Missing abbreviated type in metadata
- [`KT-59833`](https://youtrack.jetbrains.com/issue/KT-59833) K2: Stop modifying values of enum entries
- [`KT-69421`](https://youtrack.jetbrains.com/issue/KT-69421) K2: Resolve changed from delegated function to java default function
- [`KT-69392`](https://youtrack.jetbrains.com/issue/KT-69392) K2: "UNSAFE_CALL": when with some variable subjects does not smartcast the variable
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
- [`KT-68469`](https://youtrack.jetbrains.com/issue/KT-68469) [K2] MISSING_DEPENDENCY_CLASS caused by redundant `@file`:JvmName
- [`KT-68999`](https://youtrack.jetbrains.com/issue/KT-68999) K2: Unify the style of FIR generator with IR and SIR tree-generators
- [`KT-66061`](https://youtrack.jetbrains.com/issue/KT-66061) Kotlin/Native - building shared module for iOS - Argument list too long
- [`KT-49420`](https://youtrack.jetbrains.com/issue/KT-49420) Suspicious behaviour of frontend in case of DefinitelyNotNull type overload
- [`KT-59752`](https://youtrack.jetbrains.com/issue/KT-59752) K2: "Conflicting overloads" if function with same signature added to different contexts
- [`KT-68618`](https://youtrack.jetbrains.com/issue/KT-68618) K1: Unresolved reference for qualified this in implicit type
- [`KT-25341`](https://youtrack.jetbrains.com/issue/KT-25341) NOT_YET_SUPPORTED_IN_INLINE reported over anonymous object border
- [`KT-69215`](https://youtrack.jetbrains.com/issue/KT-69215) K2: IllegalArgumentException for delegated function in anonymous object with captured type parameters
- [`KT-69044`](https://youtrack.jetbrains.com/issue/KT-69044) Destructuring declaration shouldn't be possible in declaration in when
- [`KT-69028`](https://youtrack.jetbrains.com/issue/KT-69028) K2: `FirJvmActualizingBuiltinSymbolProvider` returns `null` on builtins declarations if common source-set is not presented
- [`KT-15704`](https://youtrack.jetbrains.com/issue/KT-15704) Rethink usage of term "type annotation" in error messages
- [`KT-68970`](https://youtrack.jetbrains.com/issue/KT-68970) K2. Argument type mismatch caused by out projection in inferred type from if - else
- [`KT-68800`](https://youtrack.jetbrains.com/issue/KT-68800) K2: Delete `ConeAttributes.plus` method
- [`KT-59389`](https://youtrack.jetbrains.com/issue/KT-59389) K2: Missing AMBIGUOUS_LABEL
- [`KT-68803`](https://youtrack.jetbrains.com/issue/KT-68803) K2: Smart cast fails with "Unresolved reference" when `@Suppress`("UNCHECKED_CAST") used in statement
- [`KT-68968`](https://youtrack.jetbrains.com/issue/KT-68968) K2: Missing ILLEGAL_SUSPEND_FUNCTION_CALL diagnostic in initialization code of a local class inside suspend function
- [`KT-68336`](https://youtrack.jetbrains.com/issue/KT-68336) K2 does not seem to pass the right constructor arguments to custom scripts
- [`KT-68517`](https://youtrack.jetbrains.com/issue/KT-68517) "IrSimpleFunctionSymbolImpl is unbound" for actual class containing non-actual functions
- [`KT-59678`](https://youtrack.jetbrains.com/issue/KT-59678) K2: Investigate `ConeKotlinType.unCapture()`
- [`KT-69027`](https://youtrack.jetbrains.com/issue/KT-69027) K2: Initialize `FirStdlibBuiltinSyntheticFunctionInterfaceProvider` in library session
- [`KT-62818`](https://youtrack.jetbrains.com/issue/KT-62818) K2: improve VAR_OVERRIDDEN_BY_VAL diagnostic message
- [`KT-68214`](https://youtrack.jetbrains.com/issue/KT-68214) Rename TypeApproximatorConfiguration properties for clarity
- [`KT-68093`](https://youtrack.jetbrains.com/issue/KT-68093) Implement deprecation of smartcasts on class-delegated properties
- [`KT-67270`](https://youtrack.jetbrains.com/issue/KT-67270) Native: report more performance metrics from the compiler
- [`KT-68621`](https://youtrack.jetbrains.com/issue/KT-68621) DATA_CLASS_INVISIBLE_COPY_USAGE false negative for inline fun
- [`KT-68568`](https://youtrack.jetbrains.com/issue/KT-68568) K2: False-positive ACCIDENTAL_OVERRIDE caused by missing dependency class
- [`KT-66723`](https://youtrack.jetbrains.com/issue/KT-66723) K2: NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS for actual typealias that extends to Java class with complicated hierarchy that includes default method
- [`KT-69000`](https://youtrack.jetbrains.com/issue/KT-69000) Can't render constructor of intersection type
- [`KT-68849`](https://youtrack.jetbrains.com/issue/KT-68849) K2: "ClassCastException:  cannot be cast to kotlin.jvm.functions.Function2" caused by passing lambda to SAM constructor results
- [`KT-61744`](https://youtrack.jetbrains.com/issue/KT-61744) Native: -Xsave-llvm-ir-after fails to check errors from LLVMPrintModuleToFile
- [`KT-67103`](https://youtrack.jetbrains.com/issue/KT-67103) Support AbbreviatedTypeAttribute for aliased types from the source code
- [`KT-63921`](https://youtrack.jetbrains.com/issue/KT-63921) K2: different representation of recursive type aliases
- [`KT-68679`](https://youtrack.jetbrains.com/issue/KT-68679) K2: "Override has incorrect nullability in its signature compared to the overridden declaration" caused by subclass of Android HashMap
- [`KT-64335`](https://youtrack.jetbrains.com/issue/KT-64335) K2: improve rendering of captured types in diagnostic messages
- [`KT-68820`](https://youtrack.jetbrains.com/issue/KT-68820) K2: "Unresolved reference" on calling function with "contract" name
- [`KT-67933`](https://youtrack.jetbrains.com/issue/KT-67933) K2: no conversion between fun interfaces if target has `suspend`
- [`KT-68230`](https://youtrack.jetbrains.com/issue/KT-68230) K2: FirMissingDependencyClassChecker: Not supported: ConeFlexibleType
- [`KT-68531`](https://youtrack.jetbrains.com/issue/KT-68531) K2: False-negative error on assignment to enum entry
- [`KT-68446`](https://youtrack.jetbrains.com/issue/KT-68446) K2: compile-time failure on smart-casted generic value used as a when-subject in a contains-check with range
- [`KT-68678`](https://youtrack.jetbrains.com/issue/KT-68678) K2: Drop using `FirBuiltinSymbolProvider` while compiling JVM stdlib
- [`KT-68382`](https://youtrack.jetbrains.com/issue/KT-68382) Get rid of context receivers in FirScript implementation
- [`KT-68585`](https://youtrack.jetbrains.com/issue/KT-68585) Implement new rules for CFA about enum entries
- [`KT-68110`](https://youtrack.jetbrains.com/issue/KT-68110) K2: "Java type mismatch" caused by spring.Nullable
- [`KT-68613`](https://youtrack.jetbrains.com/issue/KT-68613) K2: False positive `CONFLICTING_PROJECTION` after fixing KT-67764
- [`KT-67764`](https://youtrack.jetbrains.com/issue/KT-67764) K2: False negative: Projection problem is not reported in `is` expression
- [`KT-67887`](https://youtrack.jetbrains.com/issue/KT-67887) Expection on  assigning to private field of value type
- [`KT-67801`](https://youtrack.jetbrains.com/issue/KT-67801) NSME on evaluating private member function with value class parameter
- [`KT-67800`](https://youtrack.jetbrains.com/issue/KT-67800) NSME on evaluating private top-level function with value class parameter
- [`KT-68542`](https://youtrack.jetbrains.com/issue/KT-68542) K2: Fix referecing to `@ExtensionFunctionType` if it's declared in source
- [`KT-68188`](https://youtrack.jetbrains.com/issue/KT-68188) K2: Properly support FunctionN creation for stdlib compilation
- [`KT-67946`](https://youtrack.jetbrains.com/issue/KT-67946) K2: Crash on red code: `Instead use FirErrorTypeRef for ERROR CLASS: Cannot infer argument for type parameter T`
- [`KT-68526`](https://youtrack.jetbrains.com/issue/KT-68526) K2: false-negative inconsistent data class copy visibility warning on call to generic data class copy function
- [`KT-68528`](https://youtrack.jetbrains.com/issue/KT-68528) K2: false-positive inconsistent data class copy visibility warning on call to function from another module with identical value parameter types and return type
- [`KT-68525`](https://youtrack.jetbrains.com/issue/KT-68525) K2: false-negative inconsistent data class copy visibility warning on callable reference to data class copy function
- [`KT-68617`](https://youtrack.jetbrains.com/issue/KT-68617) K2: Secondary constructors in a sealed class have private visibility instead of protected in the generated IR
- [`KT-63920`](https://youtrack.jetbrains.com/issue/KT-63920) K2: Private secondary sealed class constructor is private in metadata, but protected in K1
- [`KT-57996`](https://youtrack.jetbrains.com/issue/KT-57996) Usages of `Foo `@Nullable` []` produce only warnings even with `-Xtype-enhancement-improvements-strict-mode -Xjspecify-annotations=strict`
- [`KT-68207`](https://youtrack.jetbrains.com/issue/KT-68207) K2: Investigate if losing ConeIntersectionType.upperBoundForApproximation during approximation leads to any issues
- [`KT-64990`](https://youtrack.jetbrains.com/issue/KT-64990) K2: Remove usages of SymbolTable from FIR2IR
- [`KT-67798`](https://youtrack.jetbrains.com/issue/KT-67798) NSME on assigning to private delegated property of value class
- [`KT-68264`](https://youtrack.jetbrains.com/issue/KT-68264) K2: confusing INVISIBLE_* error when typealias is involved
- [`KT-68529`](https://youtrack.jetbrains.com/issue/KT-68529) K2: false-negative redundant annotation warning on `@ExposedCopyVisibility` on data class with public constructor
- [`KT-67943`](https://youtrack.jetbrains.com/issue/KT-67943) Approximation should not generate types with UPPER_BOUND_VIOLATION errors
- [`KT-67503`](https://youtrack.jetbrains.com/issue/KT-67503) K2: False negative "Type Expected" when attempting to annotate a wildcard type argument
- [`KT-68187`](https://youtrack.jetbrains.com/issue/KT-68187) K2: Create IrBuiltins in fir2ir only after IR actualization
- [`KT-66443`](https://youtrack.jetbrains.com/issue/KT-66443) K2: ArrayIterationHandler doesn't work if UIntArray declared in sources
- [`KT-68291`](https://youtrack.jetbrains.com/issue/KT-68291) K2 / Contracts: Non-existent invocation kind is suggested as a fix
- [`KT-68209`](https://youtrack.jetbrains.com/issue/KT-68209) K2: Strange import suggestion when lambda body contains invalid code
- [`KT-67368`](https://youtrack.jetbrains.com/issue/KT-67368) "NullPointerException: Parameter specified as non-null is null" local lambda creates new not-null checks with 2.0.0-Beta5
- [`KT-51433`](https://youtrack.jetbrains.com/issue/KT-51433) FE 1.0: implement warnings about label resolve changes
- [`KT-66554`](https://youtrack.jetbrains.com/issue/KT-66554) K2. Drop FIR based fake-override generator from fir2ir
- [`KT-64202`](https://youtrack.jetbrains.com/issue/KT-64202) K2: Drop old methods for calculation of overridden symbols for lazy declarations
- [`KT-67895`](https://youtrack.jetbrains.com/issue/KT-67895) K2: Properly implement generation of fake-overrides for fields
- [`KT-54496`](https://youtrack.jetbrains.com/issue/KT-54496) K2: `REDUNDANT_MODALITY_MODIFIER` diagnostic disregards compiler plugins
- [`KT-63745`](https://youtrack.jetbrains.com/issue/KT-63745) K2: Approximation of DNN with nullability warning attribute leads to attribute incorrectly becoming not-null
- [`KT-63362`](https://youtrack.jetbrains.com/issue/KT-63362) AbstractTypeApproximator fixes only first local type in hierarchy
- [`KT-67769`](https://youtrack.jetbrains.com/issue/KT-67769) K2: "variable must be initialized" on unreachable access in constructor
- [`KT-51195`](https://youtrack.jetbrains.com/issue/KT-51195) FIR IC: Incremental compilation fails with `@PublishedApi` property
- [`KT-67966`](https://youtrack.jetbrains.com/issue/KT-67966) No JVM type annotation is generated on a class supertype
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
- [`KT-64456`](https://youtrack.jetbrains.com/issue/KT-64456) K2: Port *VersionRequirementTest to K2
- [`KT-67136`](https://youtrack.jetbrains.com/issue/KT-67136) Put $this parameter to LVT for suspend lambdas
- [`KT-62538`](https://youtrack.jetbrains.com/issue/KT-62538) K2: Declarations inside external classes should be implicitly external
- [`KT-67627`](https://youtrack.jetbrains.com/issue/KT-67627) K2: External interface companion isn't external in IR
- [`KT-60290`](https://youtrack.jetbrains.com/issue/KT-60290) K2: origin is not set for !in operator
- [`KT-67512`](https://youtrack.jetbrains.com/issue/KT-67512) K2: false positive WRONG_GETTER_RETURN_TYPE when getter return type is annotated
- [`KT-67635`](https://youtrack.jetbrains.com/issue/KT-67635) K2: No warning TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES for SAM constructor with inferred type
- [`KT-60501`](https://youtrack.jetbrains.com/issue/KT-60501) K2 Scripting: investigate metadata difference between K1 and K2
- [`KT-67598`](https://youtrack.jetbrains.com/issue/KT-67598) K2: Fix incorrect casting `UByte` to `Number` in `FirToConstantValueTransformer`
- [`KT-56564`](https://youtrack.jetbrains.com/issue/KT-56564) False positive "non-exhaustive when" in case of intersection type
- [`KT-63969`](https://youtrack.jetbrains.com/issue/KT-63969) K2: extra property in metadata
- [`KT-63968`](https://youtrack.jetbrains.com/issue/KT-63968) K2: extra property in metadata for anonymous variable in script
- [`KT-67547`](https://youtrack.jetbrains.com/issue/KT-67547) K/N can't build caches, fails with "clang++: error=2, No such file or directory"
- [`KT-67469`](https://youtrack.jetbrains.com/issue/KT-67469) K2: Failing module in FP-intellij
- [`KT-64033`](https://youtrack.jetbrains.com/issue/KT-64033) K2: Investigate ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound
- [`KT-64457`](https://youtrack.jetbrains.com/issue/KT-64457) K2: Fix DecompiledKnmStubConsistencyK2TestGenerated
- [`KT-66377`](https://youtrack.jetbrains.com/issue/KT-66377) IR Evaluator: "no container found for type parameter" when evaluating nested generics
- [`KT-66378`](https://youtrack.jetbrains.com/issue/KT-66378) IR Evaluator: Symbol is unbound
- [`KT-64506`](https://youtrack.jetbrains.com/issue/KT-64506) IDE, IR Evaluator: NPE in ReflectiveAccessLowering.fieldLocationAndReceiver when evaluating private static properties
- [`KT-67380`](https://youtrack.jetbrains.com/issue/KT-67380) K2: Don't check for `equals` overriding for class `Any`
- [`KT-67038`](https://youtrack.jetbrains.com/issue/KT-67038) K2: Missing type of FirLiteralExpression causes an exception for property initializer type resolution
- [`KT-59813`](https://youtrack.jetbrains.com/issue/KT-59813) K2: Fix the TODO about `firEffect.source` in `FirReturnsImpliesAnalyzer`
- [`KT-59834`](https://youtrack.jetbrains.com/issue/KT-59834) K2: Fix the TODO about `merge(other)` in `UnusedChecker`
- [`KT-59188`](https://youtrack.jetbrains.com/issue/KT-59188) K2: Change positioning strategy for `WRONG_NUMBER_OF_TYPE_ARGUMENTS` error
- [`KT-59108`](https://youtrack.jetbrains.com/issue/KT-59108) K2. SMARTCAST_IMPOSSIBLE instead of UNSAFE_IMPLICIT_INVOKE_CALL
- [`KT-65503`](https://youtrack.jetbrains.com/issue/KT-65503) The inline processor cannot handle objects inside the lambda correctly when calling an inline function from another module
- [`KT-30696`](https://youtrack.jetbrains.com/issue/KT-30696) NoSuchMethodError if nested anonymous objects are used with propagation reified type parameter
- [`KT-58966`](https://youtrack.jetbrains.com/issue/KT-58966) Incorrect type inference for parameters with omitted type of anonymous function that is being analyzed as value of function type with receiver
- [`KT-67458`](https://youtrack.jetbrains.com/issue/KT-67458) Use `@PhaseDescription` for JVM backend lowering phases
- [`KT-65647`](https://youtrack.jetbrains.com/issue/KT-65647) K2 ignores diagnostics on sourceless `FirTypeRef`s
- [`KT-64489`](https://youtrack.jetbrains.com/issue/KT-64489) K2: Rename FirAugmentedArraySet
- [`KT-67394`](https://youtrack.jetbrains.com/issue/KT-67394) FIR: Make FIR repr of For from PSI and LightTree the same
- [`KT-66724`](https://youtrack.jetbrains.com/issue/KT-66724) K2 IDE. False positive errors because of wrong type inference in complex case of delegated property and type arguments
- [`KT-40248`](https://youtrack.jetbrains.com/issue/KT-40248) Confusing error message NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY
- [`KT-66947`](https://youtrack.jetbrains.com/issue/KT-66947) K2: false-positive JSpecify nullability enhancement warning on Java wildcard type argument with same base type but different nullabilities as upper and lower bounds
- [`KT-66974`](https://youtrack.jetbrains.com/issue/KT-66974) K2: false-negative JSpecify nullability enhancement warning on nullable projection of Java wildcard type argument with non-null bounds in out-position
- [`KT-66946`](https://youtrack.jetbrains.com/issue/KT-66946) K2: false-negative JSpecify nullability enhancement warning on Java wildcard type argument with nullable upper bound in out-position
- [`KT-66442`](https://youtrack.jetbrains.com/issue/KT-66442) K2: No visibility error on importing private aliases
- [`KT-66598`](https://youtrack.jetbrains.com/issue/KT-66598) K2: Allow comparisons, `is`-checks and casts between Kotlin and platform types
- [`KT-55966`](https://youtrack.jetbrains.com/issue/KT-55966) K2: Not enough information to infer type variable K if smartcast is used
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
- [`b/351858979`](https://issuetracker.google.com/issues/351858979) Fix stability inferencing of interfaces on incremental compilation
- [`b/346821372`](https://issuetracker.google.com/issues/346821372) [Compose] Fix code generation for group optimization
- [`b/357878245`](https://issuetracker.google.com/issues/357878245) Disallow open @Composable functions with default params to fix binary compatibility issues.

### IR. Actualizer

- [`KT-68830`](https://youtrack.jetbrains.com/issue/KT-68830) Compiler crash on missing actual class
- [`KT-69024`](https://youtrack.jetbrains.com/issue/KT-69024) K2: Children of expect annotation with `@OptionalExpectation` should be actualized
- [`KT-68742`](https://youtrack.jetbrains.com/issue/KT-68742) Allow expect protected to Java protected actualization
- [`KT-66436`](https://youtrack.jetbrains.com/issue/KT-66436) K2. Actualizing modCount property with a field in AbstractMutableList
- [`KT-68741`](https://youtrack.jetbrains.com/issue/KT-68741) Support actualization of AbstractMutableList.modCount
- [`KT-68801`](https://youtrack.jetbrains.com/issue/KT-68801) Crash on access of fake override of function actualized by fake override
- [`KT-66307`](https://youtrack.jetbrains.com/issue/KT-66307) K2: property fake override isn't generated for protected field

### IR. Inlining

- [`KT-67208`](https://youtrack.jetbrains.com/issue/KT-67208) KJS: put ReplaceSuspendIntrinsicLowering after IR inliner
- [`KT-68100`](https://youtrack.jetbrains.com/issue/KT-68100) Run IR validation in the beginning and the end of the common prefix
- [`KT-69171`](https://youtrack.jetbrains.com/issue/KT-69171) Introduce a temporary `-X` CLI parameter that enables double-inlining
- [`KT-69006`](https://youtrack.jetbrains.com/issue/KT-69006) Enable IR visibility checks after IR inlining
- [`KT-69183`](https://youtrack.jetbrains.com/issue/KT-69183) IR inlining: properly handle defaults that depends on previous value parameters
- [`KT-67660`](https://youtrack.jetbrains.com/issue/KT-67660) Suspicious package part FQN calculation in InventNamesForLocalClasses
- [`KT-68558`](https://youtrack.jetbrains.com/issue/KT-68558) Move `InlineCallableReferenceToLambdaPhase` into `ir.inline` module
- [`KT-56466`](https://youtrack.jetbrains.com/issue/KT-56466) Support non-local break/continue in IR inliner
- [`KT-64958`](https://youtrack.jetbrains.com/issue/KT-64958) KJS: Put as many as possible lowerings after the inliner
- [`KT-67297`](https://youtrack.jetbrains.com/issue/KT-67297) Implement IR deserializer with unbound symbols

### IR. Interpreter

- [`KT-66938`](https://youtrack.jetbrains.com/issue/KT-66938) Internal error in file lowering: java.lang.AssertionError: Error occurred while optimizing an expression: CALL 'public final fun less (arg0: kotlin.Int, arg1: kotlin.Int): kotlin.Boolean declared in kotlin.internal.ir'

### IR. Tree

#### Performance Improvements

- [`KT-67695`](https://youtrack.jetbrains.com/issue/KT-67695) ForLoopsLowering fails to handle a loop over an imprecise typed iterable

#### Fixes

- [`KT-68784`](https://youtrack.jetbrains.com/issue/KT-68784) Support validating visibility of referenced declarations in IrValidator
- [`KT-68174`](https://youtrack.jetbrains.com/issue/KT-68174) Delete the IrMessageLogger interface
- [`KT-67082`](https://youtrack.jetbrains.com/issue/KT-67082) Introduce attributes on IrElement
- [`KT-68716`](https://youtrack.jetbrains.com/issue/KT-68716) `DeepCopyIrTreeWithSymbols.visitConst` should remap const type
- [`KT-67650`](https://youtrack.jetbrains.com/issue/KT-67650) Add default implementations to methods for non-leaf IrSymbol subclasses from SymbolRemapper
- [`KT-67649`](https://youtrack.jetbrains.com/issue/KT-67649) Autogenerate IrSymbol interface hierarchy
- [`KT-44721`](https://youtrack.jetbrains.com/issue/KT-44721) IR: merge IrPrivateSymbolBase and IrPublicSymbolBase hierarchies
- [`KT-67580`](https://youtrack.jetbrains.com/issue/KT-67580) Autogenerate SymbolRemapper
- [`KT-67457`](https://youtrack.jetbrains.com/issue/KT-67457) Introduce a way to simplify IR lowering phase creation
- [`KT-67060`](https://youtrack.jetbrains.com/issue/KT-67060) NoSuchMethodError for org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl in the Flysto

### JavaScript

#### New Features

- [`KT-69133`](https://youtrack.jetbrains.com/issue/KT-69133) Kotlin/JS: Add support for collection instantiation in JavaScript
- [`KT-18891`](https://youtrack.jetbrains.com/issue/KT-18891) JS: provide a way to declare static members (JsStatic?)

#### Fixes

- [`KT-68943`](https://youtrack.jetbrains.com/issue/KT-68943) `@JsPlainObject` breaks when interface has type parameters
- [`KT-70592`](https://youtrack.jetbrains.com/issue/KT-70592) "Error: HttpClientCall expected" on HTTP request when targeting es2015 with KTOR
- [`KT-67273`](https://youtrack.jetbrains.com/issue/KT-67273) Creating Kotlin Collections from JS collections
- [`KT-65018`](https://youtrack.jetbrains.com/issue/KT-65018) JS: Deprecate error tolerance
- [`KT-67355`](https://youtrack.jetbrains.com/issue/KT-67355) KJS / ES6: 'super' in lambda with enabled `-Xir-generate-inline-anonymous-functions` leads to JS errors
- [`KT-69353`](https://youtrack.jetbrains.com/issue/KT-69353) KJS / d.ts: Kotlin does not export base collection classes along with their mutable collection counterparts
- [`KT-66898`](https://youtrack.jetbrains.com/issue/KT-66898) KJS: Reserved keywords not escaped when `-Xir-generate-inline-anonymous-functions` is enabled
- [`KT-69710`](https://youtrack.jetbrains.com/issue/KT-69710) JS IR generates bad code for inner param default referring to outer members
- [`KT-68632`](https://youtrack.jetbrains.com/issue/KT-68632) K2: allow JS_NAME_CLASH suppression
- [`KT-69400`](https://youtrack.jetbrains.com/issue/KT-69400) Use correct type for references on local functions when transforming them into lambda
- [`KT-68554`](https://youtrack.jetbrains.com/issue/KT-68554) Legalize marker interface as parent for JSO (interface marked with `@JsPlainObject`)
- [`KT-68740`](https://youtrack.jetbrains.com/issue/KT-68740) Kotlin/JS 2.0.0 IrLinkageError with dynamic function parameters inside data classes
- [`KT-68944`](https://youtrack.jetbrains.com/issue/KT-68944) `@JsPlainObject` require properties, when type - nullable alias
- [`KT-68891`](https://youtrack.jetbrains.com/issue/KT-68891) `@JsPlainObject` fails to compile when encountering reserved keywords as interface properties
- [`KT-69023`](https://youtrack.jetbrains.com/issue/KT-69023) KJS / IR: `globalThis` is mandatory, breaking older browsers support
- [`KT-68641`](https://youtrack.jetbrains.com/issue/KT-68641) KJS: 'export was not found' with per-file mode on case-insensitive filesystem
- [`KT-68053`](https://youtrack.jetbrains.com/issue/KT-68053) K2: NON_EXPORTABLE_TYPE on a typealias of primitive type
- [`KT-62304`](https://youtrack.jetbrains.com/issue/KT-62304) K/JS: Investigate the compiler assertion crash in JS FIR with backend tests
- [`KT-68620`](https://youtrack.jetbrains.com/issue/KT-68620) [wasm][js] Default param in inner class method fails if we are referring generic extension property
- [`KT-64801`](https://youtrack.jetbrains.com/issue/KT-64801) K2 + JS and WASM: Inner with default inner doesn't work properly
- [`KT-67248`](https://youtrack.jetbrains.com/issue/KT-67248) ModuleDescriptor in JS Linker contains incorrect friend dependecies
- [`KT-64424`](https://youtrack.jetbrains.com/issue/KT-64424) K2: Migrate JsProtoComparisonTestGenerated to K2
- [`KT-52602`](https://youtrack.jetbrains.com/issue/KT-52602) Kotlin/JS + IR: incompatible ABI version is not reported when no declarations are actually used by a Gradle compilation
- [`KT-66092`](https://youtrack.jetbrains.com/issue/KT-66092) K/JS & Wasm: .isReified for reified upper bound is wrongly false
- [`KT-67112`](https://youtrack.jetbrains.com/issue/KT-67112) Unable to apply `@JsStatic` for common sources: [NO_CONSTRUCTOR]
- [`KT-62329`](https://youtrack.jetbrains.com/issue/KT-62329) KJS: "UnsupportedOperationException: Empty collection can't be reduced" caused by external enum with "`@JsExport`"
- [`KT-67018`](https://youtrack.jetbrains.com/issue/KT-67018) K/JS: Executable js file for module-kind=umd contains top level this instead of globalThis
- [`KT-64776`](https://youtrack.jetbrains.com/issue/KT-64776) Test infra for JS can't process dependency in mpp module
- [`KT-65076`](https://youtrack.jetbrains.com/issue/KT-65076) Use the same instance when a fun interface doesn't capture or capture only singletons

### Klibs

- [`KT-66557`](https://youtrack.jetbrains.com/issue/KT-66557) Check, that no bad metadata in klib is produced, when we failed to compute constant value
- [`KT-66968`](https://youtrack.jetbrains.com/issue/KT-66968) Provide K/N platforms libs for all available targets
- [`KT-66967`](https://youtrack.jetbrains.com/issue/KT-66967) Provide K/N stdlib for all available targets in all distributions
- [`KT-66605`](https://youtrack.jetbrains.com/issue/KT-66605) [KLIB] Excessive creation of `BaseKotlinLibrary` during resolving libs
- [`KT-68824`](https://youtrack.jetbrains.com/issue/KT-68824) API 4 ABI: Don't show sealed class constructors
- [`KT-68202`](https://youtrack.jetbrains.com/issue/KT-68202) KLIB metadata: nested classes are sometimes inside a different 'knm' chunk
- [`KT-65834`](https://youtrack.jetbrains.com/issue/KT-65834) [KLIB Resolve] Drop library versions in KLIB manifests
- [`KT-67446`](https://youtrack.jetbrains.com/issue/KT-67446) [KLIB Tool] Drop "-repository <path>" CLI parameter
- [`KT-67445`](https://youtrack.jetbrains.com/issue/KT-67445) [KLIB Tool] Drop "install" and "remove" commands

### Language Design

- [`KT-58920`](https://youtrack.jetbrains.com/issue/KT-58920) K2: Prioritize Enum.entries resolve
- [`KT-11914`](https://youtrack.jetbrains.com/issue/KT-11914) Confusing data class copy with private constructor
- [`KT-68636`](https://youtrack.jetbrains.com/issue/KT-68636) Incorrect private_to_this visibility for data class with a private constructor

### Libraries

#### New Features

- [`KT-31880`](https://youtrack.jetbrains.com/issue/KT-31880) UUID functionality to fix Java bugs as well as extend it
- [`KT-57998`](https://youtrack.jetbrains.com/issue/KT-57998) implement Base64.withoutPadding

#### Performance Improvements

- [`KT-67023`](https://youtrack.jetbrains.com/issue/KT-67023) Optimize Int.sign and Long.sign for js and wasmJs targets

#### Fixes

- [`KT-70196`](https://youtrack.jetbrains.com/issue/KT-70196) Introduce ExperimentalUuidApi annotation for marking Uuid API
- [`KT-60787`](https://youtrack.jetbrains.com/issue/KT-60787) Cannot ignore alpha when formatting with HexFormat
- [`KT-68025`](https://youtrack.jetbrains.com/issue/KT-68025) Improve documentation for Hex
- [`KT-66129`](https://youtrack.jetbrains.com/issue/KT-66129) Minor issues with HexFormat
- [`KT-67511`](https://youtrack.jetbrains.com/issue/KT-67511) provide equals() and hashCode() implementations for kotlinx.metadata.KmType
- [`KT-68240`](https://youtrack.jetbrains.com/issue/KT-68240) stdlib: proper expects for internal API used in intermediate shared source sets
- [`KT-68840`](https://youtrack.jetbrains.com/issue/KT-68840) atomicfu-runtime: annotate some internal functions with `@PublishedApi`
- [`KT-68839`](https://youtrack.jetbrains.com/issue/KT-68839) Annotate `kotlin.js.VOID` property with `@PublishedApi`
- [`KT-68023`](https://youtrack.jetbrains.com/issue/KT-68023) Improve documentation for Base64
- [`KT-51483`](https://youtrack.jetbrains.com/issue/KT-51483) Documentation of trimMargin is (partly) difficult to understand
- [`KT-64649`](https://youtrack.jetbrains.com/issue/KT-64649) Add explanation to "A compileOnly dependency is used in the Kotlin/Native target" warning message
- [`KT-67807`](https://youtrack.jetbrains.com/issue/KT-67807) JS/Wasm: ByteArray.decodeToString incorrectly handles ill-formed 4-byte sequences with a 2nd byte not being continuation byte
- [`KT-67768`](https://youtrack.jetbrains.com/issue/KT-67768) Wasm: ByteArray.decodeToString throws out-of-bounds exception if the last byte is a start of a 4-byte sequence
- [`KT-66896`](https://youtrack.jetbrains.com/issue/KT-66896) Improve Array contentEquals and contentDeepEquals documentation

### Native

- [`KT-70166`](https://youtrack.jetbrains.com/issue/KT-70166) Native: EXC_BAD_ACCESS on watchOS when using Dispatchers
- [`KT-69246`](https://youtrack.jetbrains.com/issue/KT-69246) K2: linkPodDebugFrameworkIosArm64 takes  15 (!!) times longer
- [`KT-67694`](https://youtrack.jetbrains.com/issue/KT-67694) Native: WeakRefBenchmark degradation due to nonoptimized IntProgression iteration
- [`KT-69206`](https://youtrack.jetbrains.com/issue/KT-69206) Native: updating to LLVM 16 breaks debugging in lldb on Linux
- [`KT-68640`](https://youtrack.jetbrains.com/issue/KT-68640) Native: updating to LLVM 16 changes behavior of `used` attribute in C/C++ code
- [`KT-58097`](https://youtrack.jetbrains.com/issue/KT-58097) Kotlin/Native: improve the error message if Xcode is not properly configured
- [`KT-67583`](https://youtrack.jetbrains.com/issue/KT-67583) compileKotlin-task unexpectedly downloads K/N dependencies on Linux (but doesn't on Mac)

### Native. Build Infrastructure

- [`KT-69781`](https://youtrack.jetbrains.com/issue/KT-69781) Kotlin/Native performance tests fail to compile with bitcode

### Native. C and ObjC Import

- [`KT-69094`](https://youtrack.jetbrains.com/issue/KT-69094) LLVM 11 clang: cinterops fail with "_Float16 is not supported on this target"
- [`KT-68254`](https://youtrack.jetbrains.com/issue/KT-68254) Native: flaky testForwardEnum test in Kotlin/Native on MacOS
- [`KT-65260`](https://youtrack.jetbrains.com/issue/KT-65260) Native: compiler crashes when casting to an Obj-C class companion

### Native. ObjC Export

- [`KT-65666`](https://youtrack.jetbrains.com/issue/KT-65666) Native: enable objcExportSuspendFunctionLaunchThreadRestriction=none by default
- [`KT-57496`](https://youtrack.jetbrains.com/issue/KT-57496) linkReleaseFrameworkIosArm64: e: Compilation failed: An operation is not implemented

### Native. Platform Libraries

- [`KT-69382`](https://youtrack.jetbrains.com/issue/KT-69382) LLVM 11 clang: symbol not found when running the linker

### Native. Runtime

- [`KT-70043`](https://youtrack.jetbrains.com/issue/KT-70043) Native: EXC_BAD_ACCESS on watchOS when using Random
- [`KT-68928`](https://youtrack.jetbrains.com/issue/KT-68928) EXC_BREAKPOINT: BUG IN CLIENT OF LIBPLATFORM: Trying to recursively lock an os_unfair_lock

### Native. Runtime. Memory

- [`KT-66644`](https://youtrack.jetbrains.com/issue/KT-66644) Native: threads are too often paused to assist GC (with concurrent mark)
- [`KT-68871`](https://youtrack.jetbrains.com/issue/KT-68871) Native: Unexpected barriers phase during STW: weak-processing
- [`KT-67779`](https://youtrack.jetbrains.com/issue/KT-67779) Native: SpecialRefRegistry::ThradData publication prolongs the pause in CMS
- [`KT-66918`](https://youtrack.jetbrains.com/issue/KT-66918) Native: scan global root set concurrently

### Native. Swift Export

- [`KT-69469`](https://youtrack.jetbrains.com/issue/KT-69469) Exporting object twice causing crash
- [`KT-69251`](https://youtrack.jetbrains.com/issue/KT-69251) Get rid of context receivers from ./native/.../lazyWithSessions.kt
- [`KT-68865`](https://youtrack.jetbrains.com/issue/KT-68865) Move config into test-directives
- [`KT-68259`](https://youtrack.jetbrains.com/issue/KT-68259) Swift export: secondary constructs lead to compilation errors
- [`KT-67095`](https://youtrack.jetbrains.com/issue/KT-67095) Native: fix testNativeRefs export test
- [`KT-67099`](https://youtrack.jetbrains.com/issue/KT-67099) Remove SirVisitor and SirTransformer from code
- [`KT-67003`](https://youtrack.jetbrains.com/issue/KT-67003) Abandon PackageInflator implementation in favour of PackageProvider component

### Native. Testing

- [`KT-69235`](https://youtrack.jetbrains.com/issue/KT-69235) Incorrect handling of friend dependencies in Native test infra
- [`KT-67436`](https://youtrack.jetbrains.com/issue/KT-67436) Native: support CLI tests
- [`KT-68416`](https://youtrack.jetbrains.com/issue/KT-68416) Native: when using test grouping, a whole group gets ignored on non-Mac hosts if it has Objective-C tests
- [`KT-68500`](https://youtrack.jetbrains.com/issue/KT-68500) Native: Drop custom logic in ExtTestCaseGroupProvider, mute codegen/box tests explicitly

### Reflection

- [`KT-69433`](https://youtrack.jetbrains.com/issue/KT-69433) KotlinReflectionInternalError on non-reified type parameter in typeOf inside an inline lambda
- [`KT-68675`](https://youtrack.jetbrains.com/issue/KT-68675) K2: KotlinReflectionInternalError on non-reified type parameter in typeOf inside a lambda

### Tools. Build Tools API

- [`KT-68555`](https://youtrack.jetbrains.com/issue/KT-68555) BTA test infra: top level declarations are invisible across modules

### Tools. CLI

- [`KT-69792`](https://youtrack.jetbrains.com/issue/KT-69792) Add the possibility to disable fast jar fs in K2
- [`KT-68838`](https://youtrack.jetbrains.com/issue/KT-68838) OutOfMemory when compiling in CLI
- [`KT-67939`](https://youtrack.jetbrains.com/issue/KT-67939) Add CLI argument to enable when guards feature
- [`KT-68743`](https://youtrack.jetbrains.com/issue/KT-68743) Extract common CLI arguments for all KLIB-based backends
- [`KT-68450`](https://youtrack.jetbrains.com/issue/KT-68450) CLI: errors related to module-info are reported even if there are no Kotlin source files
- [`KT-68060`](https://youtrack.jetbrains.com/issue/KT-68060) FastJarFS fails on empty jars

### Tools. CLI. Native

- [`KT-66952`](https://youtrack.jetbrains.com/issue/KT-66952) Native: konanc fails when KONAN_HOME is under path with spaces
- [`KT-64524`](https://youtrack.jetbrains.com/issue/KT-64524) Introduce a CLI argument to override native_targets field in klib manifest

### Tools. Commonizer

- [`KT-68835`](https://youtrack.jetbrains.com/issue/KT-68835) Command line length overflow on Linux/Windows while invoking commonizer via :commonizeDistribution

### Tools. Compiler Plugin API

- [`KT-68020`](https://youtrack.jetbrains.com/issue/KT-68020) K2: run FirSupertypeGenerationExtension over generated declarations

### Tools. Compiler Plugins

- [`KT-64425`](https://youtrack.jetbrains.com/issue/KT-64425) K2: Implement Atomicfu*IrTestGenerated for K2
- [`KT-69401`](https://youtrack.jetbrains.com/issue/KT-69401) Kotlin power assert plugin doesn't work correctly with safe cast operator
- [`KT-69290`](https://youtrack.jetbrains.com/issue/KT-69290) PowerAssert: implicit receivers included in power-assert generated diagram
- [`KT-68511`](https://youtrack.jetbrains.com/issue/KT-68511) Power Assert kotlinx.assertEquals message display problem
- [`KT-68807`](https://youtrack.jetbrains.com/issue/KT-68807) Power-Assert crashes the Kotlin compiler when if expression used as assertion parameter
- [`KT-68162`](https://youtrack.jetbrains.com/issue/KT-68162) K2 Parcelize implementation breaks compiler phase contracts
- [`KT-67605`](https://youtrack.jetbrains.com/issue/KT-67605) K2 parcelize: false positive NOTHING_TO_OVERRIDE in one test
- [`KT-64455`](https://youtrack.jetbrains.com/issue/KT-64455) K2: Implement ParcelizeIrBoxTestWithSerializableLikeExtension for K2

### Tools. Compiler plugins. Serialization

- [`KT-70516`](https://youtrack.jetbrains.com/issue/KT-70516) KxSerialization: `@KeepGeneratedSerializer` and sealed class cause initialization error
- [`KT-68752`](https://youtrack.jetbrains.com/issue/KT-68752) Serializable annotation on Java class is not taken into account in K2 checker
- [`KT-68931`](https://youtrack.jetbrains.com/issue/KT-68931) JS/Native + serialization: partial linkage error
- [`KT-69039`](https://youtrack.jetbrains.com/issue/KT-69039) FIR: Implement IDE-only checker for kotlinx.serialization compiler plugin to report IDE-only diagnostics

### Tools. Fleet. ObjC Export

- [`KT-68826`](https://youtrack.jetbrains.com/issue/KT-68826) ObjCExport: SerializersModuleBuilder
- [`KT-68841`](https://youtrack.jetbrains.com/issue/KT-68841) ObjCExport: `@Deprecated` support
- [`KT-68887`](https://youtrack.jetbrains.com/issue/KT-68887) ObjCExport: K1 text fixture `@Deprecated` support
- [`KT-68051`](https://youtrack.jetbrains.com/issue/KT-68051) [ObjCExport] Support reserved method names

### Tools. Gradle

#### New Features

- [`KT-68651`](https://youtrack.jetbrains.com/issue/KT-68651) Compose: provide a single place in extension to configure all compose flags

#### Performance Improvements

- [`KT-61861`](https://youtrack.jetbrains.com/issue/KT-61861) Gradle: Kotlin compilations depend on packed artifacts

#### Fixes

- [`KT-69809`](https://youtrack.jetbrains.com/issue/KT-69809) Compose Gradle Plugin: AGP doesn't override configuration properties like traceMarkersEnabled
- [`KT-65820`](https://youtrack.jetbrains.com/issue/KT-65820) Compatibility with Gradle 8.7 release
- [`KT-69444`](https://youtrack.jetbrains.com/issue/KT-69444) Don't warn about missing Compose Compiler Gradle plugin in some cases
- [`KT-67888`](https://youtrack.jetbrains.com/issue/KT-67888) Remove usages of deprecated Configuration.fileCollection() method
- [`KT-68843`](https://youtrack.jetbrains.com/issue/KT-68843) Gradle: Kotlin plugin changes source set 'main' to 'null/main'
- [`KT-67395`](https://youtrack.jetbrains.com/issue/KT-67395) Add new plugins to collector kotlin gradle performance
- [`KT-68661`](https://youtrack.jetbrains.com/issue/KT-68661) Move ExperimentalWasmDsl to kotlin-gradle-plugin-annotations
- [`KT-69837`](https://youtrack.jetbrains.com/issue/KT-69837) Deprecation warning for file-based IC is issued when the property is set to true, altering the intended meaning of the message
- [`KT-69291`](https://youtrack.jetbrains.com/issue/KT-69291) Compose Gradle plugin: Enable strong skipping by default
- [`KT-67766`](https://youtrack.jetbrains.com/issue/KT-67766) Build against Gradle API 8.7
- [`KT-67889`](https://youtrack.jetbrains.com/issue/KT-67889) Run tests against Gradle 8.8 release
- [`KT-65271`](https://youtrack.jetbrains.com/issue/KT-65271) Gradle: "Mutating dependency DefaultExternalModuleDependency after it has been finalized has been deprecated " with gradle 8.6-rc-3
- [`KT-67822`](https://youtrack.jetbrains.com/issue/KT-67822) Deprecate JVM history files based incremental compilation
- [`KT-64378`](https://youtrack.jetbrains.com/issue/KT-64378) Compatibility with Gradle 8.6 release
- [`KT-69414`](https://youtrack.jetbrains.com/issue/KT-69414) Compose: featureFlags override values of the deprecated compose options
- [`KT-67771`](https://youtrack.jetbrains.com/issue/KT-67771) Compatibility with Gradle 8.8 release
- [`KT-65528`](https://youtrack.jetbrains.com/issue/KT-65528) Migrate rest of Gradle integration tests to new Test DSL
- [`KT-68306`](https://youtrack.jetbrains.com/issue/KT-68306) Project isolation for FUS statistics: Cannot access project ':' from project ':app' at org.jetbrains.kotlin.gradle.report.BuildMetricsService$ Companion.initBuildScanExtensionHolder
- [`KT-67890`](https://youtrack.jetbrains.com/issue/KT-67890) Compile against Gradle 8.8 API artifact
- [`KT-68773`](https://youtrack.jetbrains.com/issue/KT-68773) Kotlin 2.0.0 with Gradle 8.8: ConcurrentModificationException on BuildFusService configurationMetrics
- [`KT-68308`](https://youtrack.jetbrains.com/issue/KT-68308) Project isolation for FUS statistics: An error is thrown at org.gradle.configurationcache.ProblemReportingCrossProjectModelAccess$ProblemReportingProject.getLayout
- [`KT-61574`](https://youtrack.jetbrains.com/issue/KT-61574) Add project-isolation test for Kotlin/Android plugin
- [`KT-65936`](https://youtrack.jetbrains.com/issue/KT-65936) Provide a detailed error for changing kotlin native version dependency.
- [`KT-62684`](https://youtrack.jetbrains.com/issue/KT-62684) PropertiesBuildService should load extraProperties only once
- [`KT-67288`](https://youtrack.jetbrains.com/issue/KT-67288) Test DSL should not fail the test if build scan publishing has failed
- [`KT-58280`](https://youtrack.jetbrains.com/issue/KT-58280) org.jetbrains.kotlin.jvm Gradle plugin contributes build directories to the test compile classpath

### Tools. Gradle. JS

- [`KT-70077`](https://youtrack.jetbrains.com/issue/KT-70077) [2.0.20-Beta2] KGP reports confusing warnings about js/wasmJS source sets
- [`KT-69805`](https://youtrack.jetbrains.com/issue/KT-69805) YarnSetupTask  does not work for custom downloadBaseUrl
- [`KT-67444`](https://youtrack.jetbrains.com/issue/KT-67444) Gradle / JS: wrong type commonWebpackConfig.devServer.proxy
- [`KT-42923`](https://youtrack.jetbrains.com/issue/KT-42923) KJS: Resources are not available when running Karma tests
- [`KT-68482`](https://youtrack.jetbrains.com/issue/KT-68482) KotlinNpmInstallTask is not compatible with configuration cache
- [`KT-68072`](https://youtrack.jetbrains.com/issue/KT-68072) K/JS, K/Wasm: Module not found in transitive case
- [`KT-68103`](https://youtrack.jetbrains.com/issue/KT-68103) K/JS, K/Wasm: Generation of test compilation's package.json requires main compilation
- [`KT-67924`](https://youtrack.jetbrains.com/issue/KT-67924) K/JS, K/Wasm: kotlinNpmInstall can rewrite root package.json

### Tools. Gradle. Kapt

- [`KT-64627`](https://youtrack.jetbrains.com/issue/KT-64627) Kapt3KotlinGradleSubplugin uses property lookup that breaks project isolation
- [`KT-61928`](https://youtrack.jetbrains.com/issue/KT-61928) Clarify parameter types in KaptArguments and KaptJavacOption

### Tools. Gradle. Multiplatform

#### New Features

- [`KT-56566`](https://youtrack.jetbrains.com/issue/KT-56566) Consider pre-generating DSL accessors for source sets with names corresponding to the default target hierarchy

#### Fixes

- [`KT-66568`](https://youtrack.jetbrains.com/issue/KT-66568) w: KLIB resolver: The same 'unique_name=...' found in more than one library
- [`KT-65754`](https://youtrack.jetbrains.com/issue/KT-65754) Gradle: Commonized cinterop dependency configuration changes project description
- [`KT-69406`](https://youtrack.jetbrains.com/issue/KT-69406) Deprecate combinations of KMP plugin with some Gradle Java plugins
- [`KT-64109`](https://youtrack.jetbrains.com/issue/KT-64109) Using compileOnly/runtimeOnly dependencies in K/N-related configurations leads to odd behaviour
- [`KT-69311`](https://youtrack.jetbrains.com/issue/KT-69311) runDebugExecutable task fails with "this.compilation" is null with enabled configuration cache
- [`KT-69310`](https://youtrack.jetbrains.com/issue/KT-69310)  w: KLIB resolver: The same 'unique_name=...' found in more than one library for diamond source set structures
- [`KT-61793`](https://youtrack.jetbrains.com/issue/KT-61793) KMP/AGP compatibility: Update the maximum tested AGP version to 8.3
- [`KT-66209`](https://youtrack.jetbrains.com/issue/KT-66209) Accessing the source sets by name is confusing
- [`KT-62368`](https://youtrack.jetbrains.com/issue/KT-62368) Kotlin 1.9.X fails to detect kotlin.test.Test annotation reference on commonTest source set when targeting JVM+Android
- [`KT-67110`](https://youtrack.jetbrains.com/issue/KT-67110) Usage of BuildOperationExecutor.getCurrentOpeartion internal Gradle API
- [`KT-58319`](https://youtrack.jetbrains.com/issue/KT-58319) kotlin.git: ProjectMetadataProviderImpl "Unexpected source set 'commonMain'"

### Tools. Gradle. Native

- [`KT-69918`](https://youtrack.jetbrains.com/issue/KT-69918) java.lang.NullPointerException: Cannot invoke "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation.getTarget()" because "this.compilation" is null
- [`KT-67935`](https://youtrack.jetbrains.com/issue/KT-67935) OverriddenKotlinNativeHomeChecker does not work well with relative paths
- [`KT-64430`](https://youtrack.jetbrains.com/issue/KT-64430) Remove deprecated KotlinToolRunner(project) constructor
- [`KT-64427`](https://youtrack.jetbrains.com/issue/KT-64427) Stop using deprecated KotlinToolRunner(project) constructor call

### Tools. Incremental Compile

- [`KT-69042`](https://youtrack.jetbrains.com/issue/KT-69042) K2: changing a Java constant won't cause Kotlin usages to recompile
- [`KT-63476`](https://youtrack.jetbrains.com/issue/KT-63476) Investigate the debug output of JVM compilation in KMP IC smoke tests

### Tools. JPS

- [`KT-63707`](https://youtrack.jetbrains.com/issue/KT-63707) JPS: "Multiple values are not allowed for" caused by Compose

### Tools. Kapt

- [`KT-68145`](https://youtrack.jetbrains.com/issue/KT-68145) K2 KAPT: missing $annotations methods for const properties and private properties without accessors
- [`KT-67495`](https://youtrack.jetbrains.com/issue/KT-67495) File leak in when building with kapt
- [`KT-66780`](https://youtrack.jetbrains.com/issue/KT-66780) K2 KAPT Kotlinc should exit with an exit code 1 (compilation error) if a Kapt task fails
- [`KT-66998`](https://youtrack.jetbrains.com/issue/KT-66998) K2 KAPT: Reimplement support for DefaultImpls

### Tools. Scripts

- [`KT-69296`](https://youtrack.jetbrains.com/issue/KT-69296) scripting dependency resolution does not authenticate towards maven mirrors
- [`KT-67575`](https://youtrack.jetbrains.com/issue/KT-67575) FromConfigurationsBase script definition unexpected behaviour with regex from gradle templates
- [`KT-67066`](https://youtrack.jetbrains.com/issue/KT-67066) DeepCopyIrTreeWithSymbols does not copy IrScript nodes correctly
- [`KT-67071`](https://youtrack.jetbrains.com/issue/KT-67071) K2: ScriptCompilationConfigurationFromDefinition is not serializable
- [`KT-67063`](https://youtrack.jetbrains.com/issue/KT-67063) LauncherReplTest flaky on Windows

### Tools. Wasm

- [`KT-70220`](https://youtrack.jetbrains.com/issue/KT-70220) K/Wasm: Upgrade NPM dependencies
- [`KT-69245`](https://youtrack.jetbrains.com/issue/KT-69245) K/Wasm: Remove warning of working-in-progress
- [`KT-69154`](https://youtrack.jetbrains.com/issue/KT-69154) K/Wasm: wasmJsBrowserProductionRun flaky crash with "WebAssembly.instantiate(): Import ...  function import requires a callable"
- [`KT-68930`](https://youtrack.jetbrains.com/issue/KT-68930) K/Wasm: Production run doesn not use optimize task result
- [`KT-67901`](https://youtrack.jetbrains.com/issue/KT-67901) K/Wasm: Add kotlin-wasm-examples to CI
- [`KT-67468`](https://youtrack.jetbrains.com/issue/KT-67468) Gradle task build (allTests) fails on default web project
- [`KT-67980`](https://youtrack.jetbrains.com/issue/KT-67980) Wasm: Incorrect "Please choose a JavaScript environment to build distributions and run tests" when WASM is not configured
- [`KT-67862`](https://youtrack.jetbrains.com/issue/KT-67862) K/Wasm: Make usage of ChromeWasmGc an error


## 2.0.10

### Apple Ecosystem

- [`KT-68257`](https://youtrack.jetbrains.com/issue/KT-68257) Xcode incorrectly reuses embedAndSign framework when moving to and from 2.0.0

### Compiler

#### Fixes

- [`KT-69876`](https://youtrack.jetbrains.com/issue/KT-69876) K2 Compile exception: Only IrBlockBody together with kotlinx serialization
- [`KT-68521`](https://youtrack.jetbrains.com/issue/KT-68521) K2: Property's private setters can be bypassed when using plusAssign and minusAssign operators
- [`KT-68667`](https://youtrack.jetbrains.com/issue/KT-68667) K2:  Compiler hangs on mapNotNull and elvis inside lambda
- [`KT-68747`](https://youtrack.jetbrains.com/issue/KT-68747) K2: Long compilation time because of constraint solving when using typealias in different modules
- [`KT-68940`](https://youtrack.jetbrains.com/issue/KT-68940) K2: "IllegalArgumentException: All variables should be fixed to something"
- [`KT-68797`](https://youtrack.jetbrains.com/issue/KT-68797) K2 / Native: "java.lang.IllegalStateException: FIELD" caused by enabled caching
- [`KT-68362`](https://youtrack.jetbrains.com/issue/KT-68362) False-positive ABSTRACT_MEMBER_NOT_IMPLEMENTED for inheritor of java class which directly implements java.util.Map
- [`KT-68449`](https://youtrack.jetbrains.com/issue/KT-68449) K2: "when" expression returns Unit
- [`KT-67072`](https://youtrack.jetbrains.com/issue/KT-67072) K2: inconsistent stability of open vals on receivers of final type
- [`KT-68570`](https://youtrack.jetbrains.com/issue/KT-68570) K2: "Unresolved reference" in call with lambda argument and nested lambda argument
- [`KT-69159`](https://youtrack.jetbrains.com/issue/KT-69159) K2: KotlinNothingValueException in Exposed
- [`KT-68623`](https://youtrack.jetbrains.com/issue/KT-68623) K2: "Only safe or null-asserted calls are allowed" on safe call
- [`KT-68193`](https://youtrack.jetbrains.com/issue/KT-68193) JDK 21: new MutableList.addFirst/addLast  methods allow adding nullable value for non-null types
- [`KT-67804`](https://youtrack.jetbrains.com/issue/KT-67804) removeFirst and removeLast return type with Java 21
- [`KT-68727`](https://youtrack.jetbrains.com/issue/KT-68727) K2: "Null argument in ExpressionCodegen for parameter VALUE_PARAMETER" caused by an enum class with default parameter in a different module
- [`KT-68383`](https://youtrack.jetbrains.com/issue/KT-68383) K2: "Argument type mismatch: actual type is 'kotlin.String', but 'T & Any' was expected." with intersection types
- [`KT-68546`](https://youtrack.jetbrains.com/issue/KT-68546) K2: false-positive conflicting overloads error on inheriting generic type with inherited generic and non-generic member overloads
- [`KT-68626`](https://youtrack.jetbrains.com/issue/KT-68626) K2: "Conflicting Overloads" for function if inherited from generic type
- [`KT-68351`](https://youtrack.jetbrains.com/issue/KT-68351) K2: "Suspension functions can only be called within coroutine body"
- [`KT-68489`](https://youtrack.jetbrains.com/issue/KT-68489) K2: WRONG_ANNOTATION_TARGET with Java and Kotlin `@Target` annotation positions
- [`KT-69058`](https://youtrack.jetbrains.com/issue/KT-69058) K2: Java-defined property annotations not persisted
- [`KT-64515`](https://youtrack.jetbrains.com/issue/KT-64515) K2 IDE: [NEW_INFERENCE_ERROR] in a build.gradle.kts script while applying "jvm-test-suite" plugin and then configuring targets for test suites
- [`KT-68016`](https://youtrack.jetbrains.com/issue/KT-68016) K2: Gradle repo test `should compile correctly with Kotlin explicit api mode` fails on K2
- [`KT-68575`](https://youtrack.jetbrains.com/issue/KT-68575) K2: `@ParameterName` annotation is not erased when inferring the type of `it` in lambdas
- [`KT-67999`](https://youtrack.jetbrains.com/issue/KT-67999) K2: lost flexibility on parameters of Java SAM
- [`KT-59679`](https://youtrack.jetbrains.com/issue/KT-59679) K2: Investigate extracting uncompleted candidates from blocks
- [`KT-68401`](https://youtrack.jetbrains.com/issue/KT-68401) K2: "IllegalAccessError: failed to access class" caused by package private super Java type, when inferencing a common super type of if or when branches on JVM
- [`KT-68806`](https://youtrack.jetbrains.com/issue/KT-68806) K/Wasm RuntimeError: unreachable on Sequence::toList
- [`KT-68455`](https://youtrack.jetbrains.com/issue/KT-68455) K2: False negative UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS
- [`KT-68538`](https://youtrack.jetbrains.com/issue/KT-68538) KJS/K2: using `while` with `break` inside inline lambdas leads to an endless cycle
- [`KT-68798`](https://youtrack.jetbrains.com/issue/KT-68798) JVM compiler crashes on calling private expect constructor with a default parameter
- [`KT-68734`](https://youtrack.jetbrains.com/issue/KT-68734) K2: enum class in KMP: Expect declaration `MMKVLogLevel` is incompatible with actual `MMKVLogLevel` because modality is different
- [`KT-68674`](https://youtrack.jetbrains.com/issue/KT-68674) False positive ACTUAL_WITHOUT_EXPECT in K2
- [`KT-68350`](https://youtrack.jetbrains.com/issue/KT-68350) K2: "Inapplicable candidate(s)" caused by parameter reference of local class with type parameters from function
- [`KT-68571`](https://youtrack.jetbrains.com/issue/KT-68571) K2: "IllegalStateException: Fake override should have at least one overridden descriptor" caused by exceptions and when statement
- [`KT-68523`](https://youtrack.jetbrains.com/issue/KT-68523) K2: FileAnalysisException when using Definitely non-nullable types
- [`KT-68339`](https://youtrack.jetbrains.com/issue/KT-68339) K2: "Enum entry * is uninitialized here" caused by lazy property with enum in `when` expression
- [`KT-66688`](https://youtrack.jetbrains.com/issue/KT-66688) K2: false-negative "upper bound violated" error in extension receiver
- [`KT-68630`](https://youtrack.jetbrains.com/issue/KT-68630) DiagnosticsSuppressor is not invoked with Kotlin 2.0
- [`KT-68222`](https://youtrack.jetbrains.com/issue/KT-68222) K2. KMP. False negative `Expected declaration must not have a body` for expected top-level property with getter/setter
- [`KT-64103`](https://youtrack.jetbrains.com/issue/KT-64103) FirExpectActualDeclarationChecker reports diagnostic error for KtPsiSimpleDiagnostic with KtFakeSourceElement
- [`KT-68191`](https://youtrack.jetbrains.com/issue/KT-68191) K2. Static fake-overrides are not generated for kotlin Fir2IrLazyClass
- [`KT-68024`](https://youtrack.jetbrains.com/issue/KT-68024) K2: Gradle repo test `accessors to kotlin internal task types...` fails on K2
- [`KT-64957`](https://youtrack.jetbrains.com/issue/KT-64957) K1: drop ModuleAnnotationResolver

### Compose compiler

- [`0c5a858`](https://github.com/JetBrains/kotlin/commit/0c5a858604da726792d5b3c16374bb6cba5baf2f) Fix memoization of captureless lambdas when K2 compiler is used [b/340582180](https://issuetracker.google.com/issue/340582180)
- [`a8249d6`](https://github.com/JetBrains/kotlin/commit/a8249d60c7cd14a459469fe0ef2099721d3dd699) Allow memoizing lambdas in composable inline functions [b/340606661](https://issuetracker.google.com/issue/340606661)

### Native

- [`KT-68094`](https://youtrack.jetbrains.com/issue/KT-68094) K2/Native: Member inherits different '`@Throws`' when inheriting from generic type

### Tools. Compiler Plugins

- [`KT-69187`](https://youtrack.jetbrains.com/issue/KT-69187) Compose compiler for web doesn't support rememberComposableLambda
- [`KT-68557`](https://youtrack.jetbrains.com/issue/KT-68557) K2. Supertypes resolution of KJK hierarchy fails in presence of allopen plugin

### Tools. Compiler plugins. Serialization

- [`KT-68850`](https://youtrack.jetbrains.com/issue/KT-68850) Compose lambda type not transformed with KGP 2 + new Compose plugin

### Tools. Daemon

- [`KT-68297`](https://youtrack.jetbrains.com/issue/KT-68297) KGP 2.0 regression: JAVA_TOOL_OPTIONS is not considered in Kotlin daemon creation

### Tools. Gradle

- [`KT-69330`](https://youtrack.jetbrains.com/issue/KT-69330) KotlinCompile friendPathsSet property is racy due causing build cache invalidation
- [`KT-69026`](https://youtrack.jetbrains.com/issue/KT-69026) Mark AGP 8.5.0 as compatible with KGP
- [`KT-68447`](https://youtrack.jetbrains.com/issue/KT-68447) ill-added intentionally-broken dependency source configurations
- [`KT-69078`](https://youtrack.jetbrains.com/issue/KT-69078) Gradle: Add option to disable FUS Service
- [`KT-68278`](https://youtrack.jetbrains.com/issue/KT-68278) Spring resource loading in combination with `java-test-fixtures` plugin broken
- [`KT-66452`](https://youtrack.jetbrains.com/issue/KT-66452) Gradle produces false positive configuration cache problem for Project usage at execution time
- [`KT-68242`](https://youtrack.jetbrains.com/issue/KT-68242) Run tests against AGP 8.4.0

### Tools. Gradle. Multiplatform

- [`KT-68805`](https://youtrack.jetbrains.com/issue/KT-68805) KMP project (re-)import took a long time for downloading platform libs
- [`KT-68248`](https://youtrack.jetbrains.com/issue/KT-68248) kotlin multiplatform project fail to build on Fedora with corretto

### Tools. Gradle. Native

- [`KT-68638`](https://youtrack.jetbrains.com/issue/KT-68638) KGP 2.0 breaks native test with api dependencies and configuration cache
- [`KT-65761`](https://youtrack.jetbrains.com/issue/KT-65761) Missing JDK Platform ClassLoader when compiling Kotlin native in daemon

### Tools. JPS

- [`KT-69204`](https://youtrack.jetbrains.com/issue/KT-69204) Generate lookups in dumb mode for compatibility with ref index

### Tools. Kapt

- [`KT-68171`](https://youtrack.jetbrains.com/issue/KT-68171) K2KAPT: boxed return types in overridden methods changed to primitives

### Tools. Scripts

- [`KT-68681`](https://youtrack.jetbrains.com/issue/KT-68681) K2 / CLI / Script: "NullPointerException: getService(...) must not be null" caused by `@DependsOn`
- [`KT-67747`](https://youtrack.jetbrains.com/issue/KT-67747) K2: regression in Spring unit tests using `javax.script.ScriptEngine`


## 2.0.0

### Analysis. API

#### New Features

- [`KT-65327`](https://youtrack.jetbrains.com/issue/KT-65327) Support reading klib contents in Analysis API

#### Performance Improvements

- [`KT-65560`](https://youtrack.jetbrains.com/issue/KT-65560) K2: Anaysis API: ContextCollector triggers redundant resolution in the case of file elements
- [`KT-64987`](https://youtrack.jetbrains.com/issue/KT-64987) Analysis API: 50GB memory allocation on creating empty kotlinx.collections.immutable.persistentMapOf
- [`KT-61789`](https://youtrack.jetbrains.com/issue/KT-61789) K2: optimize getFirForNonKtFileElement for references inside super type reference
- [`KT-59498`](https://youtrack.jetbrains.com/issue/KT-59498) K2: getOnAirGetTowerContextProvider took too much time due to on air resolve
- [`KT-61728`](https://youtrack.jetbrains.com/issue/KT-61728) Analysis API: optimize AllCandidatesResolver.getAllCandidates

#### Fixes

- [`KT-65561`](https://youtrack.jetbrains.com/issue/KT-65561) Analysis API: dummy.kt is not a physical file
- [`KT-65616`](https://youtrack.jetbrains.com/issue/KT-65616) K2: FirDeclarationStatusImpl cannot be cast to FirResolvedDeclarationStatus from STATUS
- [`KT-65600`](https://youtrack.jetbrains.com/issue/KT-65600) Analysis Api: FirFile for KtCodeFragments are created and not updated on changes
- [`KT-64919`](https://youtrack.jetbrains.com/issue/KT-64919) K2 IDE: Implement KMP support for sealed class inheritors
- [`KT-64241`](https://youtrack.jetbrains.com/issue/KT-64241) K2: Unresolved calls to functions in scripts depending on included projects
- [`KT-65813`](https://youtrack.jetbrains.com/issue/KT-65813) Analysis API Standalone: `FirDeclarationForCompiledElementSearcher` does not find compiled elements
- [`KT-66052`](https://youtrack.jetbrains.com/issue/KT-66052) AA: render expect/actual modifier
- [`KT-66795`](https://youtrack.jetbrains.com/issue/KT-66795) KtCodeFragment.clone() is broken
- [`KT-66532`](https://youtrack.jetbrains.com/issue/KT-66532) K2 CodeGen AA: missing annotation setup for function in source module but not in a compile target file
- [`KT-64833`](https://youtrack.jetbrains.com/issue/KT-64833) Analysis API: Members implemented by delegation have no overridden symbols
- [`KT-62405`](https://youtrack.jetbrains.com/issue/KT-62405) Analysis API:  Symbols `SUBSTITUTION_OVERRIDE` have no overridden symbols
- [`KT-66749`](https://youtrack.jetbrains.com/issue/KT-66749) K2: "Collection contains no element matching the predicate" on an unresolved call
- [`KT-62832`](https://youtrack.jetbrains.com/issue/KT-62832) K2: ClassCastException: FirDeclarationStatusImpl cannot be cast to FirResolvedDeclarationStatus
- [`KT-66719`](https://youtrack.jetbrains.com/issue/KT-66719) AbstractGetKlibSourceFileNameTest: The dependency to ":native:analysis-api-klib-reader" breaks JPS compilation
- [`KT-66603`](https://youtrack.jetbrains.com/issue/KT-66603) Analysis API: support type annotations in KtPsiTypeProviderMixIn#asPsiType
- [`KT-64505`](https://youtrack.jetbrains.com/issue/KT-64505) Analysis API Standalone: Remove test-specific calculation of sealed class inheritors
- [`KT-66013`](https://youtrack.jetbrains.com/issue/KT-66013) Analysis API Standalone: Sealed inheritors aren't correctly calculated for source classes
- [`KT-62880`](https://youtrack.jetbrains.com/issue/KT-62880) K2 IDE: Unresolved java annotation methods in KDoc
- [`KT-66530`](https://youtrack.jetbrains.com/issue/KT-66530) K2: Analysis API: KtPsiTypeProvider#asKtType crashes on PsiClassType for Java type parameter with wrong use site
- [`KT-65571`](https://youtrack.jetbrains.com/issue/KT-65571) Support VirtualFile inputs to Analysis API modules
- [`KT-66485`](https://youtrack.jetbrains.com/issue/KT-66485) Substituted types are not provided for callable references
- [`KT-66498`](https://youtrack.jetbrains.com/issue/KT-66498) Analysis API: 'KtFe10SymbolDeclarationOverridesProvider' considers a class to be a subclass of itself
- [`KT-64579`](https://youtrack.jetbrains.com/issue/KT-64579) K2 IDE: "Expected FirResolvedArgumentList for FirAnnotationCallImpl of FirValueParameterImpl(Source) but FirArgumentListImpl found"
- [`KT-65978`](https://youtrack.jetbrains.com/issue/KT-65978) Analysis API: Use soft references in `FileStructureCache`
- [`KT-64051`](https://youtrack.jetbrains.com/issue/KT-64051) K2 IDE: Analysis API: Unresolved links to typealias in KDoc
- [`KT-66189`](https://youtrack.jetbrains.com/issue/KT-66189) K2 / IDE: KtFirExpressionTypeProvider bugs
- [`KT-61422`](https://youtrack.jetbrains.com/issue/KT-61422) K2 IDE: "No array element type for vararg value parameter: org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl"
- [`KT-66276`](https://youtrack.jetbrains.com/issue/KT-66276) K2: Analysis API: `TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM` false positive for script parameter
- [`KT-66232`](https://youtrack.jetbrains.com/issue/KT-66232) K2: Analysis API: cover ScriptWithCustomDefDiagnosticsTestBaseGenerated by LL FIR tests
- [`KT-60996`](https://youtrack.jetbrains.com/issue/KT-60996) K2: Stub Based Deserializer: Set versionRequirements to enable VERSION_REQUIREMENT_DEPRECATION diagnostics
- [`KT-66306`](https://youtrack.jetbrains.com/issue/KT-66306) K2: Analysis API: drop ability to enable global phase resolve lock
- [`KT-55750`](https://youtrack.jetbrains.com/issue/KT-55750) LL FIR: Implement multi-threaded resolve
- [`KT-65563`](https://youtrack.jetbrains.com/issue/KT-65563) Analysis API: Missing session component `FirExpectActualMatchingContextFactory` in `LLFirLibrarySession`
- [`KT-66173`](https://youtrack.jetbrains.com/issue/KT-66173) K2: No 'org.jetbrains.kotlin.fir.scopes.impl.FirDelegatedMembersFilter' in array owner: LLFirLibrarySession
- [`KT-66238`](https://youtrack.jetbrains.com/issue/KT-66238) Gradle kotlin build scripts - a lot of unresolved symbols after latest changes in kotlin master
- [`KT-65099`](https://youtrack.jetbrains.com/issue/KT-65099) K2: Recursive local storage cache check for Fir2IrDeclarationStorage::createAndCacheIrPropertySymbols()
- [`KT-65265`](https://youtrack.jetbrains.com/issue/KT-65265) Analysis API: Add library session invalidation tests
- [`KT-56288`](https://youtrack.jetbrains.com/issue/KT-56288) Analysis API: Add tests for session invalidation on the Analysis API side
- [`KT-64000`](https://youtrack.jetbrains.com/issue/KT-64000) K2: make AnnotationArgumentsStateKeepers more accurate
- [`KT-63606`](https://youtrack.jetbrains.com/issue/KT-63606) K2: Analysis API: rewrite FirLazyAnnotationTransformer to avoid redundant transformations
- [`KT-65191`](https://youtrack.jetbrains.com/issue/KT-65191) KtFirMultiplatformInfoProvider#getExpectForActual doesn't return expect function for slightly broken code
- [`KT-62136`](https://youtrack.jetbrains.com/issue/KT-62136) Analysis API: Add concurrent tests for `CleanableSoftValueCache`
- [`KT-61222`](https://youtrack.jetbrains.com/issue/KT-61222) K2: Add lifecycle management for `KtResolveExtension`
- [`KT-65960`](https://youtrack.jetbrains.com/issue/KT-65960) Analysis API: Test infrastructure indexes binary libraries from decompiled files instead of stubs during IDE mode tests
- [`KT-65240`](https://youtrack.jetbrains.com/issue/KT-65240) K2: CodeGen API fails to resolve Annotation parameter type when it runs FIR2IR for a class with a parent class from other module if the parent class has an annotation from another module
- [`KT-65344`](https://youtrack.jetbrains.com/issue/KT-65344) K2: make FirScript statements (declarations) independent
- [`KT-65930`](https://youtrack.jetbrains.com/issue/KT-65930) AA: receiver type for `Int?::foo` misses nullability
- [`KT-65914`](https://youtrack.jetbrains.com/issue/KT-65914) AA: receiver type for `this::foo` returns return type of the target callable
- [`KT-62071`](https://youtrack.jetbrains.com/issue/KT-62071) Analysis API: KtFirScopeProvider.getScopeContextForPosition throws exception when ImplicitReceiverValue.implicitScope is null
- [`KT-65780`](https://youtrack.jetbrains.com/issue/KT-65780) K2: polish FileStructure implementation for FirFile
- [`KT-62840`](https://youtrack.jetbrains.com/issue/KT-62840) K2 Script: everything around destructuring declaration on top level of scripts are broken
- [`KT-64528`](https://youtrack.jetbrains.com/issue/KT-64528) K2 IDE: MPP: unregistered component 'org.jetbrains.kotlin.fir.scopes.impl.FirDelegatedMembersFilter'
- [`KT-64921`](https://youtrack.jetbrains.com/issue/KT-64921) K2 IDE: references in platform code resolve to expect classifier instead of actual
- [`KT-61296`](https://youtrack.jetbrains.com/issue/KT-61296) K2: do not resolve the entire file on lazyResolve call if FirFile is passed
- [`KT-65683`](https://youtrack.jetbrains.com/issue/KT-65683) Analysis API: Dangling file session creation causes a `computeIfAbsent` contract violation
- [`KT-64884`](https://youtrack.jetbrains.com/issue/KT-64884) K2 IDE. FP [NAMED_PARAMETER_NOT_FOUND] for copy method of library data class when class has not parameter-properties
- [`KT-65763`](https://youtrack.jetbrains.com/issue/KT-65763) K2: value parameter from library data class copy have RAW_FIR phase
- [`KT-65665`](https://youtrack.jetbrains.com/issue/KT-65665) Analysis API: support `KtDelegatedSuperTypeEntry` in `KtFirExpressionInfoProvider.isUsedAsExpression`
- [`KT-62899`](https://youtrack.jetbrains.com/issue/KT-62899) K2 IDE. IDE ignores `@Suppress` annotation for errors
- [`KT-65655`](https://youtrack.jetbrains.com/issue/KT-65655) Analysis API: `KtCodeCompilationException` should not strongly reference FIR sessions
- [`KT-62302`](https://youtrack.jetbrains.com/issue/KT-62302) Support PsiType -> KtType conversion
- [`KT-64604`](https://youtrack.jetbrains.com/issue/KT-64604) K2: IDE K2: "Modules are inconsistent during performance tests"
- [`KT-65345`](https://youtrack.jetbrains.com/issue/KT-65345) K2: unify FirDesignation and LLFirResolveTarget
- [`KT-61757`](https://youtrack.jetbrains.com/issue/KT-61757) K2 IDE: resolution to buitlins does not work for from common module
- [`KT-65268`](https://youtrack.jetbrains.com/issue/KT-65268) K2: Checking the presence of the delegated constructor call forces AST loading
- [`KT-63330`](https://youtrack.jetbrains.com/issue/KT-63330) Analysis API: Stub-based deserialized symbol providers provide unresolved enum entry annotation arguments
- [`KT-65418`](https://youtrack.jetbrains.com/issue/KT-65418) Analysis API:  `LLFirAbstractSessionFactory` loads anchor module sessions eagerly
- [`KT-64718`](https://youtrack.jetbrains.com/issue/KT-64718) Analysis API: do not expose SealedClassInheritorsProvider and FirRegularClass to IDE Plugin
- [`KT-65075`](https://youtrack.jetbrains.com/issue/KT-65075) K2: getContainingDeclaration() is broken for declarations inside code fragments
- [`KT-61332`](https://youtrack.jetbrains.com/issue/KT-61332) Support `KtTypeCodeFragment` in `PsiRawFirBuilder`
- [`KT-65150`](https://youtrack.jetbrains.com/issue/KT-65150) AA: incorrect result from `KtTypeProvider#getReceiverTypeForDoubleColonExpression` for Java static method
- [`KT-56551`](https://youtrack.jetbrains.com/issue/KT-56551) LL FIR: implement parallel resolve for jumping phases
- [`KT-65223`](https://youtrack.jetbrains.com/issue/KT-65223) Psi: avoid KtFile usages
- [`KT-65307`](https://youtrack.jetbrains.com/issue/KT-65307) Analysis API FE10: support KtFe10AnalysisSessionProvider.getAnalysisSessionByUseSiteKtModule
- [`KT-62695`](https://youtrack.jetbrains.com/issue/KT-62695) K2 IDE: Unresolved extension functions in KDoc
- [`KT-65152`](https://youtrack.jetbrains.com/issue/KT-65152) Analysis API: KDoc references to packages are not fully resolved
- [`KT-64988`](https://youtrack.jetbrains.com/issue/KT-64988) K2 IDE: Navigation from the named argument in safe call does not work
- [`KT-63195`](https://youtrack.jetbrains.com/issue/KT-63195) AA: incorrect results from `KtTypeProvider#getReceiverTypeForDoubleColonExpression`
- [`KT-64074`](https://youtrack.jetbrains.com/issue/KT-64074) K2: Investigate LL divergence for Script.testTopLevelPropertyInitialization
- [`KT-62441`](https://youtrack.jetbrains.com/issue/KT-62441) K2: IDE K2: "No dangling modifier found"
- [`KT-62895`](https://youtrack.jetbrains.com/issue/KT-62895) K2 IDE. FP `'when' expression must be exhaustive` with sealed interface from library
- [`KT-64993`](https://youtrack.jetbrains.com/issue/KT-64993) Analysis API: KtExpressionTypeProvider.getExpectedType works incorrectly for arguments of safe calls
- [`KT-64883`](https://youtrack.jetbrains.com/issue/KT-64883) Allow direct creation of KtCommonFile
- [`KT-64646`](https://youtrack.jetbrains.com/issue/KT-64646) K2: properly forbid ast loading during raw fir phase in tests
- [`KT-64862`](https://youtrack.jetbrains.com/issue/KT-64862) Psi: missed parenthesis in type reference presentation
- [`KT-62893`](https://youtrack.jetbrains.com/issue/KT-62893) K2 IDE. FP 'when' expression must be exhaustive with Java sealed interface from library
- [`KT-63795`](https://youtrack.jetbrains.com/issue/KT-63795) K2: `lazyResolveToPhase(BODY_RESOLVE)` cannot be called from a transformer with a phase BODY_RESOLVE from SealedClassInheritorsProviderIdeImpl
- [`KT-64805`](https://youtrack.jetbrains.com/issue/KT-64805) Analysis API: introduce common entry point for multi-file test cases
- [`KT-64714`](https://youtrack.jetbrains.com/issue/KT-64714) K2: Analysis API: CollectionsKt.map doesn't resolves from Java in kotlin repo
- [`KT-64647`](https://youtrack.jetbrains.com/issue/KT-64647) K2: Allow to calculate decompiled inheritors for sealed classes in tests
- [`KT-64595`](https://youtrack.jetbrains.com/issue/KT-64595) AA: stackoverflow while simplifying a type with a recursive type parameter
- [`KT-64825`](https://youtrack.jetbrains.com/issue/KT-64825) Analysis API. Cannot compute containing PSI for unknown source kind 'org.jetbrains.kotlin.KtFakeSourceElementKind$DefaultAccessor' exception on getContainingSymbol call for default setter parameter
- [`KT-64080`](https://youtrack.jetbrains.com/issue/KT-64080) K2: Analysis API: On-air resolve does not trigger resolution of delegated super call arguments
- [`KT-64243`](https://youtrack.jetbrains.com/issue/KT-64243) K2: proper lazy resolution for fake overrides
- [`KT-62891`](https://youtrack.jetbrains.com/issue/KT-62891) K2 IDE.  FP [EXPOSED_FUNCTION_RETURN_TYPE] on overriding library method which returns protected type
- [`KT-62667`](https://youtrack.jetbrains.com/issue/KT-62667) K2: Cannot find enclosing declaration for KtNameReferenceExpression (on-air, imports)
- [`KT-61890`](https://youtrack.jetbrains.com/issue/KT-61890) Analysis API: Migrate KtFirScopeProvider to ContextCollector instead of onAirResolve
- [`KT-64197`](https://youtrack.jetbrains.com/issue/KT-64197) K2: Code fragments are only supported in JVM
- [`KT-62357`](https://youtrack.jetbrains.com/issue/KT-62357) K2 IDE. False positive on generated component methods and false negative on getter of `@JvmRecord` classes in Java
- [`KT-62892`](https://youtrack.jetbrains.com/issue/KT-62892) K2 IDE. Java outer class from other module is not resolved when nested class is accessed with fq name in a type position
- [`KT-62888`](https://youtrack.jetbrains.com/issue/KT-62888) K2 IDE. IDE infers reference to `KMutableProperty` as reference to just `KProperty`
- [`KT-64584`](https://youtrack.jetbrains.com/issue/KT-64584) K2: StubBasedFirDeserializedSymbolProvider: support deserialization of delegated declarations
- [`KT-60324`](https://youtrack.jetbrains.com/issue/KT-60324) K2 IDE: "NoSuchElementException: List is empty at JavaOverrideChecker#buildErasure"
- [`KT-62896`](https://youtrack.jetbrains.com/issue/KT-62896) K2 IDE. FP ABSTRACT_MEMBER_NOT_IMPLEMENTED on inheriting class from library which implements interface by delegation
- [`KT-62947`](https://youtrack.jetbrains.com/issue/KT-62947) Analysis API: Error while resolving FirPropertyImpl
- [`KT-64468`](https://youtrack.jetbrains.com/issue/KT-64468) Analysis API: Implement mixed multi-module tests which support different kinds of `KtModule`s
- [`KT-56541`](https://youtrack.jetbrains.com/issue/KT-56541) Symbol Light Classes: No `@NotNull` annotations are generated for accessors of lateinit properties of unresolved types
- [`KT-63547`](https://youtrack.jetbrains.com/issue/KT-63547) K2 IDE. False Positive AMBIGUOUS_ANNOTATION_ARGUMENT
- [`KT-64205`](https://youtrack.jetbrains.com/issue/KT-64205) Analysis API: Do not import non-top-level callables by default
- [`KT-63056`](https://youtrack.jetbrains.com/issue/KT-63056) K2: Cannot mutate an immutable ImplicitReceiverValue on FirCodeFragment analysis
- [`KT-64108`](https://youtrack.jetbrains.com/issue/KT-64108) K2: KtFirSymbolDeclarationOverridesProvider shouldn't provide fake overrides
- [`KT-63752`](https://youtrack.jetbrains.com/issue/KT-63752) K2: java.lang.StackOverflowError FirFieldSymbol.getHasInitializer
- [`KT-63718`](https://youtrack.jetbrains.com/issue/KT-63718) Analysis API: Stub-based dependency symbol providers of library source sessions compute the wrong package name sets
- [`KT-64225`](https://youtrack.jetbrains.com/issue/KT-64225) K2: IDE K2: "FirLazyBlock should be calculated before accessing" in evaluate debuger completion
- [`KT-64186`](https://youtrack.jetbrains.com/issue/KT-64186) Analysis API: ContextCollector provides incorrect scopes for anonymous objects
- [`KT-63979`](https://youtrack.jetbrains.com/issue/KT-63979) K2 IDE: presentation of types in completion is too verbose
- [`KT-63681`](https://youtrack.jetbrains.com/issue/KT-63681) K2: LL FIR: Improve isResolved check coverage of after lazy resolution
- [`KT-62982`](https://youtrack.jetbrains.com/issue/KT-62982) K2: Cannot get a PSI element for 'Enum.values'
- [`KT-59732`](https://youtrack.jetbrains.com/issue/KT-59732) FirLazyResolveContractViolationException: `lazyResolveToPhase(IMPORTS)` cannot be called from a transformer with a phase IMPORTS from serialisation plugin
- [`KT-62676`](https://youtrack.jetbrains.com/issue/KT-62676) K2 IDE: Reference shortener does not recoginize redundant this references
- [`KT-63627`](https://youtrack.jetbrains.com/issue/KT-63627) K2 IDE: shorten reference shortens required qualifier
- [`KT-62675`](https://youtrack.jetbrains.com/issue/KT-62675) K2 IDE: Reference shortener does not recoginize redundant labels
- [`KT-60957`](https://youtrack.jetbrains.com/issue/KT-60957) K2: Analysis API: Reference shortener does not work correctly with invoke function calls on properties
- [`KT-63771`](https://youtrack.jetbrains.com/issue/KT-63771) fe10: KtNamedClassOrObjectSymbol#isInline does not cover value classes
- [`KT-60327`](https://youtrack.jetbrains.com/issue/KT-60327) K2 IDE. "IllegalArgumentException: source must not be null" during delegation declaration
- [`KT-62421`](https://youtrack.jetbrains.com/issue/KT-62421) K2: IDE K2: "`lazyResolveToPhase(BODY_RESOLVE)` cannot be called from a transformer with a phase BODY_RESOLVE."
- [`KT-62587`](https://youtrack.jetbrains.com/issue/KT-62587) K2 IDE. FP unresolved reference on accessing nested class in annotation argument
- [`KT-63700`](https://youtrack.jetbrains.com/issue/KT-63700) K2: "FirLazyExpression should be calculated before accessing" in the case of secondary constructor
- [`KT-61383`](https://youtrack.jetbrains.com/issue/KT-61383) K2: 'KtCompilerFacility' fails on code fragment compilation in library sources with duplicated dependencies
- [`KT-62111`](https://youtrack.jetbrains.com/issue/KT-62111) K2 IDE. IllegalArgumentException on for loop with iterator declaration attempt
- [`KT-63538`](https://youtrack.jetbrains.com/issue/KT-63538) Analysis API: Removing a contract statement via `PsiElement.delete()` does not trigger an out-of-block modification
- [`KT-63694`](https://youtrack.jetbrains.com/issue/KT-63694) K1/K2 IDE. "RuntimeException: Broken stub format, most likely version of kotlin.FILE (kotlin.FILE) was not updated after serialization changes" exception on incorrect class name
- [`KT-63660`](https://youtrack.jetbrains.com/issue/KT-63660) K2: expect-actual gutter icons must be shown when declarations are matched but incompatible
- [`KT-63560`](https://youtrack.jetbrains.com/issue/KT-63560) Analysis API: Modifiable PSI tests cannot rely on the cached application environment to allow write access
- [`KT-62980`](https://youtrack.jetbrains.com/issue/KT-62980) Implement `KtFirSimpleNameReference#getImportAlias`
- [`KT-63130`](https://youtrack.jetbrains.com/issue/KT-63130) Analysis API: No receiver found for broken code during commit document
- [`KT-62705`](https://youtrack.jetbrains.com/issue/KT-62705) K2: "lazyResolveToPhase(IMPORTS) cannot be called..." from light classes
- [`KT-60170`](https://youtrack.jetbrains.com/issue/KT-60170) K2 IDE: CCE from KtFirCallResolver on invalid code with wrong implicit invoke
- [`KT-61783`](https://youtrack.jetbrains.com/issue/KT-61783) K2: Analyze 'KtCodeFragment' in a separate session
- [`KT-62010`](https://youtrack.jetbrains.com/issue/KT-62010) K2: IDE K2: "ConeClassLikeTypeImpl is not resolved to symbol for on-error type"
- [`KT-62957`](https://youtrack.jetbrains.com/issue/KT-62957) Analysis API: NullPointerException on call resolution when builtins are not available
- [`KT-61252`](https://youtrack.jetbrains.com/issue/KT-61252) K2: IDE K2: "By now the annotations argument mapping should have been resolved"
- [`KT-62935`](https://youtrack.jetbrains.com/issue/KT-62935) Analysis API: `kotlin.Cloneable` should not be available in Kotlin/Native sources
- [`KT-62910`](https://youtrack.jetbrains.com/issue/KT-62910) Analysis API: create AbstractFirPsiNativeDiagnosticsTest for LL FIR
- [`KT-63096`](https://youtrack.jetbrains.com/issue/KT-63096) K2: Analysis API: KotlinAnnotationsResolver for IDE is created with incorrect scope
- [`KT-62310`](https://youtrack.jetbrains.com/issue/KT-62310) K2 IDE. False positives errors with external annotations
- [`KT-63282`](https://youtrack.jetbrains.com/issue/KT-63282) K2 Script: annotation arguments phase should resolve propagated annotations
- [`KT-62397`](https://youtrack.jetbrains.com/issue/KT-62397) K2 IDE. FP Error in the editor on `RequiresOptIn` annotation from the lib despite the warning level
- [`KT-63223`](https://youtrack.jetbrains.com/issue/KT-63223) Analysis API: reference to declarations with kotlin* package are not resolved
- [`KT-62626`](https://youtrack.jetbrains.com/issue/KT-62626) IllegalStateException: Cannot build symbol for class org.jetbrains.kotlin.psi.KtScriptInitializer
- [`KT-62693`](https://youtrack.jetbrains.com/issue/KT-62693) K2: IDE K2: "PSI should present for declaration built by Kotlin code"
- [`KT-62674`](https://youtrack.jetbrains.com/issue/KT-62674) K2: "Scope for type ConeClassLikeTypeImpl" is null from transitive dependencies
- [`KT-61889`](https://youtrack.jetbrains.com/issue/KT-61889) Analysis API: Migrate KtFirReferenceShortener to ContextCollector instead of FirResolveContextCollector
- [`KT-62772`](https://youtrack.jetbrains.com/issue/KT-62772)  Analysis API: No 'org.jetbrains.kotlin.fir.java.FirSyntheticPropertiesStorage'(31) in array owner: LLFirSourcesSession when analysing builtins in a context of common code
- [`KT-60319`](https://youtrack.jetbrains.com/issue/KT-60319) K2 IDE: "Stability for initialized variable always should be computable"
- [`KT-62859`](https://youtrack.jetbrains.com/issue/KT-62859) K2 IDE: "Evaluate expression" throws exception when calling "Any?.toString()"
- [`KT-63058`](https://youtrack.jetbrains.com/issue/KT-63058) K2 IDE: Code completion unexpectedly imports static/companion object method
- [`KT-62588`](https://youtrack.jetbrains.com/issue/KT-62588) getExpectedType should not calculate type of the expression
- [`KT-61990`](https://youtrack.jetbrains.com/issue/KT-61990) K2: Unexpected returnTypeRef for FirSyntheticProperty
- [`KT-62625`](https://youtrack.jetbrains.com/issue/KT-62625) K2: 'FirLazyExpression should be calculated before accessing' for unresolved super type
- [`KT-62691`](https://youtrack.jetbrains.com/issue/KT-62691) K2: optimize getFirForNonKtFileElement for references inside 'where'
- [`KT-62834`](https://youtrack.jetbrains.com/issue/KT-62834) K2: missing file node level in control flow builder
- [`KT-62768`](https://youtrack.jetbrains.com/issue/KT-62768) Analysis API: No 'org.jetbrains.kotlin.fir.scopes.impl.FirDelegatedMembersFilter'(44) in array owner: LLFirSourcesSession exception on analysing common code
- [`KT-62874`](https://youtrack.jetbrains.com/issue/KT-62874) K2: FirLazyExpression should be calculated before accessing
- [`KT-62407`](https://youtrack.jetbrains.com/issue/KT-62407) Analysis API: resolve `[this]` in KDoc to extension receiver
- [`KT-61204`](https://youtrack.jetbrains.com/issue/KT-61204) K2: "FirLazyExpression should be calculated before accessing in ktor HttpBinApplication"
- [`KT-61901`](https://youtrack.jetbrains.com/issue/KT-61901) Analysis API: Declared member scopes for Java classes are missing static members
- [`KT-61800`](https://youtrack.jetbrains.com/issue/KT-61800) Analysis API: Provide separate declared member scopes for non-static and static callables
- [`KT-61255`](https://youtrack.jetbrains.com/issue/KT-61255) Analysis API: Get rid of `valueOf`, `values` and `entries` from a declared member scope
- [`KT-62466`](https://youtrack.jetbrains.com/issue/KT-62466) Expected type for functional expression should include inferred types
- [`KT-61203`](https://youtrack.jetbrains.com/issue/KT-61203) IDE K2: "Expected FirResolvedArgumentList for FirAnnotationCallImpl of FirRegularClassImpl(Source) but FirArgumentListImpl found"
- [`KT-61791`](https://youtrack.jetbrains.com/issue/KT-61791) Analysis API: Implement combined `getPackage` for combined Kotlin symbol providers
- [`KT-62437`](https://youtrack.jetbrains.com/issue/KT-62437) K2 IDE. Resolution does not work inside lambda expression in constructor argument in supertypes
- [`KT-62244`](https://youtrack.jetbrains.com/issue/KT-62244)  K2: Analysis API Standalone:  Resolving klib dependencies from binary roots terminates application
- [`KT-62897`](https://youtrack.jetbrains.com/issue/KT-62897) K2 IDE. Unresolved declarations from libraries which are doubled in `intellij` project libraries
- [`KT-61615`](https://youtrack.jetbrains.com/issue/KT-61615) K2: No 'org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsModuleKind' in array owner: LLFirSourcesSession
- [`KT-59334`](https://youtrack.jetbrains.com/issue/KT-59334) K2: LLFirImplicitTypesLazyResolver problems
- [`KT-62038`](https://youtrack.jetbrains.com/issue/KT-62038) K2: Nested classes are missing in symbol light class structure tests for libraries
- [`KT-61788`](https://youtrack.jetbrains.com/issue/KT-61788) Analysis API: Symbol for `FirAnonymousInitializer` cannot be null
- [`KT-62139`](https://youtrack.jetbrains.com/issue/KT-62139) Analysis API: KtFe10AnalysisSession.createContextDependentCopy does not need validity check
- [`KT-62090`](https://youtrack.jetbrains.com/issue/KT-62090) Analysis API: introduce an API to get a substitution formed by class inheritance
- [`KT-62268`](https://youtrack.jetbrains.com/issue/KT-62268) K2 IDE. No autocompletion and IllegalStateException for Pair
- [`KT-60325`](https://youtrack.jetbrains.com/issue/KT-60325) K2 IDE. "IllegalArgumentException: source must not be null" on `throw` usage attempt
- [`KT-61431`](https://youtrack.jetbrains.com/issue/KT-61431) K2: KtPropertyAccessorSymbolPointer pointer already disposed for $$result script property
- [`KT-58490`](https://youtrack.jetbrains.com/issue/KT-58490) K2: LLFirTypeLazyResolver problems
- [`KT-58494`](https://youtrack.jetbrains.com/issue/KT-58494) K2: LLFirAnnotationArgumentsLazyResolver problems
- [`KT-58492`](https://youtrack.jetbrains.com/issue/KT-58492) K2: LLFirBodyLazyResolver problems
- [`KT-58769`](https://youtrack.jetbrains.com/issue/KT-58769) K2: LL FIR: implement platform-dependent session factories
- [`KT-60343`](https://youtrack.jetbrains.com/issue/KT-60343) K2 IDE. IllegalArgumentException on passing incorrect type parameter to function
- [`KT-61842`](https://youtrack.jetbrains.com/issue/KT-61842) K2: reduce number of "in-block modification" events
- [`KT-62012`](https://youtrack.jetbrains.com/issue/KT-62012) K2: "KtReadActionConfinementLifetimeToken is inaccessible: Called outside analyse method"
- [`KT-61371`](https://youtrack.jetbrains.com/issue/KT-61371) K2: Analysis API standalone: register compiler symbol provider for libraries in standalone mode
- [`KT-60611`](https://youtrack.jetbrains.com/issue/KT-60611) K2: reduce number of "in-block modification" events
- [`KT-61425`](https://youtrack.jetbrains.com/issue/KT-61425) Analysis API: Provide a way to get a declared member scope for an enum entry's initializing anonymous object
- [`KT-61405`](https://youtrack.jetbrains.com/issue/KT-61405) Analysis API: An enum entry should not be a `KtSymbolWithMembers`
- [`KT-55504`](https://youtrack.jetbrains.com/issue/KT-55504) AA: remove dependency on :compiler:cli from standalone AA
- [`KT-60904`](https://youtrack.jetbrains.com/issue/KT-60904) K2: IDE K2: "For DESTRUCTURING_DECLARATION_ENTRY with text `_`, one of element types expected, but FirValueParameterSymbol found"
- [`KT-61260`](https://youtrack.jetbrains.com/issue/KT-61260) K2 Scripts: Containing function should be not null for KtParameter
- [`KT-61568`](https://youtrack.jetbrains.com/issue/KT-61568) FIR Analysis API: `collectCallCandidates` gives presence to the top level functions in the presence of more suitable overrides
- [`KT-60610`](https://youtrack.jetbrains.com/issue/KT-60610) K2 IDE: move "out of block" processing logic into LL FIR
- [`KT-61597`](https://youtrack.jetbrains.com/issue/KT-61597) Analysis API: KotlinIllegalStateExceptionWithAttachments: expected as maximum one `expect` for the actual on errorneous code with multiple expects
- [`KT-59793`](https://youtrack.jetbrains.com/issue/KT-59793) K2: class org.jetbrains.kotlin.fir.declarations.impl.FirErrorImportImpl cannot be cast to class org.jetbrains.kotlin.fir.declarations.FirResolvedImport
- [`KT-61599`](https://youtrack.jetbrains.com/issue/KT-61599) K2: ContextCollector: Support smart cast collection
- [`KT-61689`](https://youtrack.jetbrains.com/issue/KT-61689) Analysis API: ContextCollector provides incorrect context in scripts
- [`KT-61683`](https://youtrack.jetbrains.com/issue/KT-61683) Analysis API: resolve ambiguities in kotlin project
- [`KT-61245`](https://youtrack.jetbrains.com/issue/KT-61245) Analysis API: ContextCollector provides incorrect context for supertype constructor calls
- [`KT-60384`](https://youtrack.jetbrains.com/issue/KT-60384) K2: Opening `@JvmName` source in IDEA: NPE at PsiRawFirBuilder$Visitor.toFirConstructor()
- [`KT-60918`](https://youtrack.jetbrains.com/issue/KT-60918) K2 IDE: "FirDeclaration was not found for class org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry, fir is null"
- [`KT-61013`](https://youtrack.jetbrains.com/issue/KT-61013) K2 Scripts: LLFirReturnTypeCalculatorWithJump: No designation of local declaration
- [`KT-59517`](https://youtrack.jetbrains.com/issue/KT-59517) K2 IDE: KotlinExceptionWithAttachments: Modules are inconsistent
- [`KT-61331`](https://youtrack.jetbrains.com/issue/KT-61331) K2: add cache restoring in case of existing context
- [`KT-61408`](https://youtrack.jetbrains.com/issue/KT-61408) K2: IDE K2: "Inconsistency in the cache. Someone without context put a null value in the cache"

### Analysis. Light Classes

#### Performance Improvements

- [`KT-63486`](https://youtrack.jetbrains.com/issue/KT-63486) SLC: a lot of RAM is allocated in `org.jetbrains.kotlin.asJava.LightClassUtil.isMangled`

#### Fixes

- [`KT-66692`](https://youtrack.jetbrains.com/issue/KT-66692) SLC: `findAttributeValue` for attribute w/ default value in Java returns `null`
- [`KT-61734`](https://youtrack.jetbrains.com/issue/KT-61734) SLC: wildcard suppression not honored
- [`KT-65112`](https://youtrack.jetbrains.com/issue/KT-65112) Symbol Light Classes don't support annotations on type parameters
- [`KT-65843`](https://youtrack.jetbrains.com/issue/KT-65843) K2: Light method returns `kotlin.Unit` type for `TestResult` return type
- [`KT-65653`](https://youtrack.jetbrains.com/issue/KT-65653) SLC: wrong binary resolution to function with value class
- [`KT-65393`](https://youtrack.jetbrains.com/issue/KT-65393) SLC: missing deprecated-hidden property
- [`KT-64772`](https://youtrack.jetbrains.com/issue/KT-64772) SLC: presence of source PSI for compiler-generated declaration
- [`KT-65425`](https://youtrack.jetbrains.com/issue/KT-65425) K2 IDE: Seeing a reference to the class generated by compiler plugin exposed from Java code caused NPE from light classes
- [`KT-64937`](https://youtrack.jetbrains.com/issue/KT-64937) SLC: internal setters are not mangled
- [`KT-63949`](https://youtrack.jetbrains.com/issue/KT-63949) K2 IDE. Analyze hang on `@Autowired` constructor analysis
- [`KT-63087`](https://youtrack.jetbrains.com/issue/KT-63087) K2 IDE: in .java source reference to JvmName names on unsigned type / value class are unresolved
- [`KT-64605`](https://youtrack.jetbrains.com/issue/KT-64605) K2 IDE: usage of `@Repeatable` annotation in Java: false positive "Duplicate annotation"
- [`KT-64795`](https://youtrack.jetbrains.com/issue/KT-64795) SLC: distinguish last v.s. non-last `vararg` value parameter type during binary resolution
- [`KT-61605`](https://youtrack.jetbrains.com/issue/KT-61605) K2 IDE: Light elements do not obey platform contracts
- [`KT-57536`](https://youtrack.jetbrains.com/issue/KT-57536) SLC: no need to populate members with `expect` modifier
- [`KT-64320`](https://youtrack.jetbrains.com/issue/KT-64320) Decouple kotlin psi from java PSI
- [`KT-64282`](https://youtrack.jetbrains.com/issue/KT-64282) Decouple KotlinIconProviderService from java PSI
- [`KT-63552`](https://youtrack.jetbrains.com/issue/KT-63552) Symbol Light Classes don't support arrayOf and similar without parameters in property initializers and default parameter values

### Apple Ecosystem

- [`KT-64096`](https://youtrack.jetbrains.com/issue/KT-64096) Diagnostic when embedAndSign used for framework with cocoapods-dependencies
- [`KT-63821`](https://youtrack.jetbrains.com/issue/KT-63821) Copy framework to BUILT_PRODUCTS_DIR in the embedAndSign task
- [`KT-67892`](https://youtrack.jetbrains.com/issue/KT-67892) KotlinNativeLink task instantiates with a fixed list of apiFiles
- [`KT-66446`](https://youtrack.jetbrains.com/issue/KT-66446) Diagnostic never showed, and build fails when CocoaPods dependency is used with embedAndSign task and linking type is dynamic
- [`KT-66445`](https://youtrack.jetbrains.com/issue/KT-66445) Diagnostic never showed when CocoaPods dependency is used with embedAndSign task and linking type is static
- [`KT-62373`](https://youtrack.jetbrains.com/issue/KT-62373) "Xcode higher than tested" diagnostic
- [`KT-63212`](https://youtrack.jetbrains.com/issue/KT-63212) podInstall task fails without a proper diagnostic when xcodeproj gem is outdated

### Backend. Native. Debug

- [`KT-65553`](https://youtrack.jetbrains.com/issue/KT-65553) K2: Native: kt42208WithPassingLambdaToAnotherFunction test fails with K2
- [`KT-57365`](https://youtrack.jetbrains.com/issue/KT-57365) [Native] Incorrect debug info on inline function call site

### Backend. Wasm

#### New Features

- [`KT-65009`](https://youtrack.jetbrains.com/issue/KT-65009) Generate TypeScript definitions for the `@JsExport` declarations in K/Wasm
- [`KT-58088`](https://youtrack.jetbrains.com/issue/KT-58088) [PL] Support & enable partial linkage for Wasm
- [`KT-66327`](https://youtrack.jetbrains.com/issue/KT-66327) Include information about particular Wasm target into KLib manifest

#### Fixes

- [`KT-66465`](https://youtrack.jetbrains.com/issue/KT-66465) WASM support doesn't appear to be able to see some common declarations
- [`KT-66905`](https://youtrack.jetbrains.com/issue/KT-66905) K/Wasm: support new version of exception handling proposal
- [`KT-66515`](https://youtrack.jetbrains.com/issue/KT-66515) Wasm: "call param types must match" during the build
- [`KT-67435`](https://youtrack.jetbrains.com/issue/KT-67435) K/Wasm: import.meta.url transforming into absolute local path in webpack
- [`KT-65777`](https://youtrack.jetbrains.com/issue/KT-65777) Implement named export for Kotlin/Wasm
- [`KT-65660`](https://youtrack.jetbrains.com/issue/KT-65660) [WasmJs] Support catching JS exceptions
- [`KT-65824`](https://youtrack.jetbrains.com/issue/KT-65824) Wasm: Allow unsigned primitives to be used inside functions annotated with `@JsExport`
- [`KT-66103`](https://youtrack.jetbrains.com/issue/KT-66103) Wasm: companion object is not initialized in test initializers1.kt
- [`KT-66471`](https://youtrack.jetbrains.com/issue/KT-66471) Null method reference with Kotlin/Wasm on 2.0.0-Beta4
- [`KT-65210`](https://youtrack.jetbrains.com/issue/KT-65210) K/Wasm `::class` operator produces Number KClass for Short expression
- [`KT-66065`](https://youtrack.jetbrains.com/issue/KT-66065) [Wasm] Make specialisations for closured primitive values
- [`KT-64890`](https://youtrack.jetbrains.com/issue/KT-64890) K/Wasm compiler crash with external class and Kodein
- [`KT-66104`](https://youtrack.jetbrains.com/issue/KT-66104) Wasm: compiler crash: NoSuchElementException: Sequence contains no element matching the predicate
- [`KT-65778`](https://youtrack.jetbrains.com/issue/KT-65778) Create the same TypeScript tests infrastructure for Kotlin/Wasm that we have now for Kotlin/JS
- [`KT-65411`](https://youtrack.jetbrains.com/issue/KT-65411) Kotlin/Wasm: Boolean boxed instances are not the same
- [`KT-65713`](https://youtrack.jetbrains.com/issue/KT-65713) Kotlin/Wasm generates a wrapper that cannot run in Deno
- [`KT-63939`](https://youtrack.jetbrains.com/issue/KT-63939) Kotlin/Wasm Support lazy associated object initialisation
- [`KT-61888`](https://youtrack.jetbrains.com/issue/KT-61888) [Kotlin/wasm] in kotlin.test support for `@AfterTest` for async tests
- [`KT-64803`](https://youtrack.jetbrains.com/issue/KT-64803) K/Wasm: non-capturing lambdas are not singleton unlike same lambdas in jvm
- [`KT-64449`](https://youtrack.jetbrains.com/issue/KT-64449) K2: Implement K1WasmWasiCodegenBoxTestGenerated for K2
- [`KT-64829`](https://youtrack.jetbrains.com/issue/KT-64829) K/Wasm: division remainder has a wrong sign
- [`KT-58852`](https://youtrack.jetbrains.com/issue/KT-58852) WASM: two methods with different varargs: Class korlibs.template.dynamic.DynamicShape has 2 methods with the same signature [register(kotlin.Array<T of kotlin.Array>)
- [`KT-61263`](https://youtrack.jetbrains.com/issue/KT-61263) K/Wasm: add a way to turn on k2 in wasm examples using Compose
- [`KT-62863`](https://youtrack.jetbrains.com/issue/KT-62863) Execution failed for task ':kotlinx-serialization-properties:wasmJsD8Test' in serialization in the K2 QG
- [`KT-62657`](https://youtrack.jetbrains.com/issue/KT-62657) K/Wasm: switch to json repots for Kotlin Wasm Benchmarks
- [`KT-62147`](https://youtrack.jetbrains.com/issue/KT-62147) [Kotlin/Wasm] Nothing typed when expression cause a backend error
- [`KT-61958`](https://youtrack.jetbrains.com/issue/KT-61958) Update SpiderMonkey and return its usage in box tests when they switch to the final opcodes for GC and FTR proposals
- [`KT-60828`](https://youtrack.jetbrains.com/issue/KT-60828) K/Wasm: return br_on_cast_fail usages
- [`KT-59084`](https://youtrack.jetbrains.com/issue/KT-59084) WASM: "RuntimeError: illegal cast" caused by inline class and JsAny
- [`KT-60700`](https://youtrack.jetbrains.com/issue/KT-60700) [WASM] test FirWasmCodegenBoxTestGenerated.testSuspendUnitConversion failed after KT-60259

### Compiler

#### New Features

- [`KT-24664`](https://youtrack.jetbrains.com/issue/KT-24664) No smartcast on stable property if receiver had non-null assertion
- [`KT-45375`](https://youtrack.jetbrains.com/issue/KT-45375) Generate all Kotlin lambdas via invokedynamic + LambdaMetafactory by default
- [`KT-23915`](https://youtrack.jetbrains.com/issue/KT-23915) Add smart cast to non-nullable type after elvis operator
- [`KT-61077`](https://youtrack.jetbrains.com/issue/KT-61077) Support provideDelegate inference from var property type
- [`KT-59688`](https://youtrack.jetbrains.com/issue/KT-59688) K2: consider removing smartcasts only from the only visibile property with specific name, not from all of them
- [`KT-7389`](https://youtrack.jetbrains.com/issue/KT-7389) Intersection type for type parameter with multiple upper bounds in star projection
- [`KT-63477`](https://youtrack.jetbrains.com/issue/KT-63477) Consider supporting builder-style type inference from Unit coercion of last statements in lambdas
- [`KT-61907`](https://youtrack.jetbrains.com/issue/KT-61907) K2: builder inference works with assignments to member properties
- [`KT-61909`](https://youtrack.jetbrains.com/issue/KT-61909) K2: builder inference infers correct types from assignments to extension properties
- [`KT-59551`](https://youtrack.jetbrains.com/issue/KT-59551) K2: builder inference works with anonymous functions if builder parameter has a receiver with a postponed type variable
- [`KT-65443`](https://youtrack.jetbrains.com/issue/KT-65443) [K/N] Implement header caches
- [`KT-4113`](https://youtrack.jetbrains.com/issue/KT-4113) Smart casts for properties to not-null functional types at `invoke` calls
- [`KT-65681`](https://youtrack.jetbrains.com/issue/KT-65681) K2: Improve error message of UPPER_BOUND_VIOLATED when upper bound is a captured type or other non-denotable type
- [`KT-32754`](https://youtrack.jetbrains.com/issue/KT-32754) Choose existing extensions over additional built-ins members from JDK except overrides
- [`KT-57800`](https://youtrack.jetbrains.com/issue/KT-57800) Support synthetic properties on `super` receiver
- [`KT-64350`](https://youtrack.jetbrains.com/issue/KT-64350) K2: deprecate using typealias as a callable qualifier in imports
- [`KT-26565`](https://youtrack.jetbrains.com/issue/KT-26565) Choose existing extensions over additional built-ins members from JDK
- [`KT-65478`](https://youtrack.jetbrains.com/issue/KT-65478) JVM: Change inlined variable naming format
- [`KT-64702`](https://youtrack.jetbrains.com/issue/KT-64702) Upper bound of type parameter is ignored when capturing of in-projection appears in out position
- [`KT-60274`](https://youtrack.jetbrains.com/issue/KT-60274) K2: builder inference works through a delegated local variable inside builder argument
- [`KT-65859`](https://youtrack.jetbrains.com/issue/KT-65859) Calls refinement extension point
- [`KT-15220`](https://youtrack.jetbrains.com/issue/KT-15220) Reuse resolution results of common code for platform modules in multiplatform projects
- [`KT-60476`](https://youtrack.jetbrains.com/issue/KT-60476) K2: False positive NO_VALUE_FOR_PARAMETER in platform code for value class with default parameter in common declaration
- [`KT-65153`](https://youtrack.jetbrains.com/issue/KT-65153) K/N: extract liveness analysis to a separate phase
- [`KT-59098`](https://youtrack.jetbrains.com/issue/KT-59098) Support -Xjdk-release=1.6/1.7 with -jvm-target 1.8
- [`KT-63670`](https://youtrack.jetbrains.com/issue/KT-63670) Implement platform specific declaration clash diagnostics across all backends
- [`KT-62547`](https://youtrack.jetbrains.com/issue/KT-62547) Introduce a language feature flag for smartcasts based on "memory" variables
- [`KT-60820`](https://youtrack.jetbrains.com/issue/KT-60820) K1: Empty vararg value is inserted in serialized annotation call with expect default vararg value
- [`KT-58172`](https://youtrack.jetbrains.com/issue/KT-58172) Forbid `expect class A actual constructor`
- [`KT-54443`](https://youtrack.jetbrains.com/issue/KT-54443) Smart cast to non-null after safe-call in require
- [`KT-25747`](https://youtrack.jetbrains.com/issue/KT-25747) DFA variables: propagate smart cast results from local variables
- [`KT-22997`](https://youtrack.jetbrains.com/issue/KT-22997) Smart-cast should merge is-check for non-nullable type and a null check to a nullable type
- [`KT-22996`](https://youtrack.jetbrains.com/issue/KT-22996) Smart casts should observe nullability after is-check with a nullable subject type
- [`KT-22004`](https://youtrack.jetbrains.com/issue/KT-22004) Allow to resolve CONFLICTING_OVERLOADS with Deprecated(HIDDEN)
- [`KT-61955`](https://youtrack.jetbrains.com/issue/KT-61955) Support more wider actual member visibility, if the expect member is effectively final
- [`KT-59504`](https://youtrack.jetbrains.com/issue/KT-59504) K2 compiler does not require resolved 'componentX' functions for the placeholder ('_') variables in the destructuring declarations
- [`KT-62239`](https://youtrack.jetbrains.com/issue/KT-62239) Allow enum entries without parentheses uniformly
- [`KT-11712`](https://youtrack.jetbrains.com/issue/KT-11712) Smart cast is not applied for invisible setter

#### Performance Improvements

- [`KT-47545`](https://youtrack.jetbrains.com/issue/KT-47545) NI: Slow type inference involving large when-expression (ConstraintInjector.processConstraints)
- [`KT-62714`](https://youtrack.jetbrains.com/issue/KT-62714) Do not add nullability annotations to the methods of inner classes in enum entries
- [`KT-62903`](https://youtrack.jetbrains.com/issue/KT-62903) Unoptimzied `when` compilation
- [`KT-67388`](https://youtrack.jetbrains.com/issue/KT-67388) FP intellij: performance degradation in build 611
- [`KT-67507`](https://youtrack.jetbrains.com/issue/KT-67507) K2: Slow compilation times when a class has a lot of possibly conflicting declarations
- [`KT-65005`](https://youtrack.jetbrains.com/issue/KT-65005) K2: Investigate testCommonSuperTypeContravariant performance
- [`KT-65996`](https://youtrack.jetbrains.com/issue/KT-65996) Compiler enters endless loop
- [`KT-66341`](https://youtrack.jetbrains.com/issue/KT-66341) K2: Don't build IdSignatures in FIR2IR with IR f/o builder
- [`KT-66172`](https://youtrack.jetbrains.com/issue/KT-66172) K2: Improve memory consumption of `KtPsiSourceElement`
- [`KT-50860`](https://youtrack.jetbrains.com/issue/KT-50860) Combination of array set convention and plusAssign works exponentially
- [`KT-62798`](https://youtrack.jetbrains.com/issue/KT-62798) 'in' range checks are not intrinsified in kotlin-stdlib
- [`KT-65579`](https://youtrack.jetbrains.com/issue/KT-65579) K2: performance regression in FP Space
- [`KT-61635`](https://youtrack.jetbrains.com/issue/KT-61635) K2: `getConstructorKeyword` call in `PsiRawFirBuilder.toFirConstructor` forces AST load
- [`KT-62619`](https://youtrack.jetbrains.com/issue/KT-62619) FIR: Checker performance regression due to MISSING_DEPENDENCY checkers
- [`KT-62044`](https://youtrack.jetbrains.com/issue/KT-62044) Do not add nullability annotations to the methods of anonymous class
- [`KT-62706`](https://youtrack.jetbrains.com/issue/KT-62706) Optimize KtSourceElement.findChild()
- [`KT-62513`](https://youtrack.jetbrains.com/issue/KT-62513) Do not add nullability annotations to the methods of local classes
- [`KT-61991`](https://youtrack.jetbrains.com/issue/KT-61991) K2: avoid redundant full body resolution for properties during implicit type phase
- [`KT-61604`](https://youtrack.jetbrains.com/issue/KT-61604) [K/N] Bitcode dependency linking is slow for large compilations
- [`KT-61121`](https://youtrack.jetbrains.com/issue/KT-61121) [K/N] Kotlin Native compiler performance is slow when generating large frameworks
- [`KT-57616`](https://youtrack.jetbrains.com/issue/KT-57616) K2: Consider optimizing reversed versions of persistent lists in FirTowerDataContext

#### Fixes

- [`KT-67486`](https://youtrack.jetbrains.com/issue/KT-67486) K2: Calling method from a Java (implementing a Kotlin class) with named parameters is no longer possible if Java method has different parameter names
- [`KT-64615`](https://youtrack.jetbrains.com/issue/KT-64615) Inconsistent error messages for platform type nullability assertions
- [`KT-65062`](https://youtrack.jetbrains.com/issue/KT-65062) K2: build kotlinx.collections.immutable and pass to CI
- [`KT-68164`](https://youtrack.jetbrains.com/issue/KT-68164) Smart cast fails for KT-49404
- [`KT-56545`](https://youtrack.jetbrains.com/issue/KT-56545) Fix incorrect functions mangling in JVM backend in case of accidental clashing overload in a Java subclass
- [`KT-49404`](https://youtrack.jetbrains.com/issue/KT-49404) Fix type unsoundness for contravariant captured type based on Java class
- [`KT-64598`](https://youtrack.jetbrains.com/issue/KT-64598) K2: build Arrow with k2 user project
- [`KT-61039`](https://youtrack.jetbrains.com/issue/KT-61039) False positive ABSTRACT_MEMBER_NOT_IMPLEMENTED in K1 when expect actual super types scopes don't match
- [`KT-56408`](https://youtrack.jetbrains.com/issue/KT-56408) Inconsistent rules of CFA in class initialization block between K1 and K2
- [`KT-63580`](https://youtrack.jetbrains.com/issue/KT-63580) "AssertionError: access of const val: GET_FIELD" caused by const value and variable with delegation
- [`KT-67993`](https://youtrack.jetbrains.com/issue/KT-67993) K2: PCLA Inference throws exception with local objects
- [`KT-61768`](https://youtrack.jetbrains.com/issue/KT-61768) Wrong bytecode index in LineNumberTable when there is an incremental operation
- [`KT-63567`](https://youtrack.jetbrains.com/issue/KT-63567) "NoSuchMethodError" on getting value of lazily initialized property by companion's const value
- [`KT-56078`](https://youtrack.jetbrains.com/issue/KT-56078) K2: build kotlinx.coroutines
- [`KT-67609`](https://youtrack.jetbrains.com/issue/KT-67609) K2: False negative INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_ERROR
- [`KT-57750`](https://youtrack.jetbrains.com/issue/KT-57750) Report ambiguity error when resolving types and having the same-named classes star imported
- [`KT-65603`](https://youtrack.jetbrains.com/issue/KT-65603) K2: No approximation is done on public, but effectively private property
- [`KT-59932`](https://youtrack.jetbrains.com/issue/KT-59932) K2: Disappeared AMBIGUOUS_ANONYMOUS_TYPE_INFERRED
- [`KT-59906`](https://youtrack.jetbrains.com/issue/KT-59906) K2: Disappeared CAPTURED_VAL_INITIALIZATION
- [`KT-53886`](https://youtrack.jetbrains.com/issue/KT-53886) NoSuchMethodError exception in Kotlin/Native compiler
- [`KT-57678`](https://youtrack.jetbrains.com/issue/KT-57678) K2: Inconsistency in how K2 analyzes unresolved code for loops and changing closures
- [`KT-57871`](https://youtrack.jetbrains.com/issue/KT-57871) K1/K2 inconsistency on if-conditional without else-branch in parenthesis
- [`KT-56384`](https://youtrack.jetbrains.com/issue/KT-56384) K2: build IntelliJ monorepo master branch
- [`KT-49191`](https://youtrack.jetbrains.com/issue/KT-49191) Leaked integer literals from lambda with flexible return type
- [`KT-65812`](https://youtrack.jetbrains.com/issue/KT-65812) K2: "OutOfMemoryError: Java heap space" in kotlin.utils.SmartList.add
- [`KT-67224`](https://youtrack.jetbrains.com/issue/KT-67224) K2/Native: Member overrides different '`@Throws`' filter from separate module
- [`KT-65623`](https://youtrack.jetbrains.com/issue/KT-65623) K2: Unresolved reference in connection with casts
- [`KT-64136`](https://youtrack.jetbrains.com/issue/KT-64136) K2: NSME with Anvil compiler plugin
- [`KT-51241`](https://youtrack.jetbrains.com/issue/KT-51241) Provide a error when override method has different set of context receivers
- [`KT-52920`](https://youtrack.jetbrains.com/issue/KT-52920) Confusing "Multiple arguments applicable for context receiver" error message
- [`KT-67912`](https://youtrack.jetbrains.com/issue/KT-67912) K2: Cannot inference type properly from inline function with Type parameter
- [`KT-68056`](https://youtrack.jetbrains.com/issue/KT-68056) Prohibit referencing java field in case of conflict with property from companion object of the derived class
- [`KT-61129`](https://youtrack.jetbrains.com/issue/KT-61129) K2: Implement error suppression warning
- [`KT-67367`](https://youtrack.jetbrains.com/issue/KT-67367) K2: Incorrect resolution to top-level function with less specific signature in presence of SAM constructor on the same tower level
- [`KT-50179`](https://youtrack.jetbrains.com/issue/KT-50179) Fix DUPLICATE_LABEL_IN_WHEN warning with new rules of complex boolean constants
- [`KT-45334`](https://youtrack.jetbrains.com/issue/KT-45334) Prohibit referencing constructors of sealed classes by its inner members
- [`KT-59943`](https://youtrack.jetbrains.com/issue/KT-59943) K2: Disappeared OPERATOR_MODIFIER_REQUIRED
- [`KT-67875`](https://youtrack.jetbrains.com/issue/KT-67875) K2: Resolution ambiguity between Iterable and varargs
- [`KT-67699`](https://youtrack.jetbrains.com/issue/KT-67699) Not enough information to infer type argument for 'Error' using Arrow's Raise context receiver since Kotlin 2.0.0-Beta3
- [`KT-66527`](https://youtrack.jetbrains.com/issue/KT-66527) K2: type mismatch on override  for <anonymous> type
- [`KT-59897`](https://youtrack.jetbrains.com/issue/KT-59897) K2: Disappeared PACKAGE_OR_CLASSIFIER_REDECLARATION
- [`KT-50020`](https://youtrack.jetbrains.com/issue/KT-50020) K2: False-negative USAGE_IS_NOT_INLINEABLE when lambda in receiver position
- [`KT-44557`](https://youtrack.jetbrains.com/issue/KT-44557) Implement main function detection to FIR
- [`KT-67810`](https://youtrack.jetbrains.com/issue/KT-67810) K2: public-API inline function cannot access non-public-API annotation enum
- [`KT-66447`](https://youtrack.jetbrains.com/issue/KT-66447) Implement KT-59138 under a language feature
- [`KT-54862`](https://youtrack.jetbrains.com/issue/KT-54862) Anonymous type can be exposed from private inline function from type argument
- [`KT-37592`](https://youtrack.jetbrains.com/issue/KT-37592) Property invoke of a functional type with receiver is preferred over extension function invoke
- [`KT-51194`](https://youtrack.jetbrains.com/issue/KT-51194) False negative CONFLICTING_INHERITED_MEMBERS when dependency class contained in two different versions of the same dependency
- [`KT-67221`](https://youtrack.jetbrains.com/issue/KT-67221) K2: "new inference error [NewConstraintError at Incorporate TypeVariable" for captured type
- [`KT-66701`](https://youtrack.jetbrains.com/issue/KT-66701) K2: Java interface method override via Kotlin class rejected
- [`KT-60604`](https://youtrack.jetbrains.com/issue/KT-60604) K2: introduced NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, `@PublishedApi` needed for constants in annotations
- [`KT-64309`](https://youtrack.jetbrains.com/issue/KT-64309) Generate a variable mapping for continuation parameter in suspend methods just from the start
- [`KT-65438`](https://youtrack.jetbrains.com/issue/KT-65438) K2: Introduce WEAKLY_HIDDEN concept to built-in-JDK content mapping
- [`KT-65235`](https://youtrack.jetbrains.com/issue/KT-65235) JDK 21 might lead to change in overloads resolution
- [`KT-66768`](https://youtrack.jetbrains.com/issue/KT-66768) K1: False positive UNRESOLVED_REFERENCE in super.getFirst/getLast call
- [`KT-67106`](https://youtrack.jetbrains.com/issue/KT-67106) Platforms libs-dependant autotests for ObjC checkers
- [`KT-65440`](https://youtrack.jetbrains.com/issue/KT-65440) K2: Mark all potential implementations of List.getFirst()/getLast() as deprecated independently of JDK
- [`KT-65594`](https://youtrack.jetbrains.com/issue/KT-65594) K2: Type inference fails on NullMarked object with star type
- [`KT-62849`](https://youtrack.jetbrains.com/issue/KT-62849) Unoptimised bytecode for Java synthetic property references
- [`KT-60174`](https://youtrack.jetbrains.com/issue/KT-60174) JVM IR inline: accidental reification in various cases
- [`KT-57609`](https://youtrack.jetbrains.com/issue/KT-57609) K2: Stop relying on the presence of `@UnsafeVariance` using for contravariant parameters
- [`KT-54316`](https://youtrack.jetbrains.com/issue/KT-54316) Out-of-call reference to companion object's member has invalid signature
- [`KT-66976`](https://youtrack.jetbrains.com/issue/KT-66976) Some value class diagnostics are missed
- [`KT-57426`](https://youtrack.jetbrains.com/issue/KT-57426) Incorrect error message on inapplicable smartcast from alien property
- [`KT-55111`](https://youtrack.jetbrains.com/issue/KT-55111) OptIn: forbid constructor calls with default arguments under marker
- [`KT-49856`](https://youtrack.jetbrains.com/issue/KT-49856) Incorrect smartcast on var assigned in try-catch block
- [`KT-41237`](https://youtrack.jetbrains.com/issue/KT-41237) ReturnsImplies contract for receiver of member function does not work (no smartcast)
- [`KT-37878`](https://youtrack.jetbrains.com/issue/KT-37878) No Smart cast for class literal reference of nullable generic type
- [`KT-35846`](https://youtrack.jetbrains.com/issue/KT-35846) Smart cast with unchecked cast leads to unresolved call that was resolved before (both old and new inference)
- [`KT-30867`](https://youtrack.jetbrains.com/issue/KT-30867) Unsound smartcast if smartcast source and break is placed in for-in header as function arguments
- [`KT-30267`](https://youtrack.jetbrains.com/issue/KT-30267) Inconsistent smart casts in while (true)
- [`KT-33917`](https://youtrack.jetbrains.com/issue/KT-33917) Prohibit to expose anonymous types from private inline functions
- [`KT-28889`](https://youtrack.jetbrains.com/issue/KT-28889) Smart cast does not work with boolean `and` infix function
- [`KT-54790`](https://youtrack.jetbrains.com/issue/KT-54790) False positive NO_ELSE_IN_WHEN when all interfaces are sealed
- [`KT-54920`](https://youtrack.jetbrains.com/issue/KT-54920) K2: `when` with a single branch stops being exhaustive the second time it's done
- [`KT-53364`](https://youtrack.jetbrains.com/issue/KT-53364) False positive UNUSED_VARIABLE warning for variable that is used across multiple blocks
- [`KT-43234`](https://youtrack.jetbrains.com/issue/KT-43234) False positive INVALID_IF_AS_EXPRESSION caused by `if` without `else` inside `else` inside  synchronized()
- [`KT-38490`](https://youtrack.jetbrains.com/issue/KT-38490) False negative INVALID_IF_AS_EXPRESSION with unreachable code and coercion to Unit
- [`KT-35510`](https://youtrack.jetbrains.com/issue/KT-35510) No INVALID_IF_AS_EXPRESSION ("'if' must have both main and 'else' branches if used as an expression") diagnostic for if-expression with only one branch and Nothing type condition
- [`KT-34016`](https://youtrack.jetbrains.com/issue/KT-34016) Contracts - variable cannot be initialized before declaration
- [`KT-33829`](https://youtrack.jetbrains.com/issue/KT-33829) False positive SENSELESS_COMPARISON with assignment in catch block
- [`KT-30717`](https://youtrack.jetbrains.com/issue/KT-30717) False positive UNUSED_VARIABLE with local var used in inline lambda block with loop, return and other lambda
- [`KT-28232`](https://youtrack.jetbrains.com/issue/KT-28232) RETURN_NOT_ALLOWED in inline lambda argument of '[... ]' operator convention
- [`KT-26116`](https://youtrack.jetbrains.com/issue/KT-26116) No error when class member val is referenced in inline function before it is assigned later on
- [`KT-25311`](https://youtrack.jetbrains.com/issue/KT-25311) Calls on error type values lead to false-positive unreachable code
- [`KT-24372`](https://youtrack.jetbrains.com/issue/KT-24372) Misleading warning on unused setter parameter in some cases
- [`KT-23680`](https://youtrack.jetbrains.com/issue/KT-23680) False positive UNREACHABLE_CODE on `throw` with a `return` inside `finally` clause
- [`KT-23502`](https://youtrack.jetbrains.com/issue/KT-23502) When exhaustiveness is not checked for unreachable code, resulting in JVM back-end error
- [`KT-22621`](https://youtrack.jetbrains.com/issue/KT-22621) "throw throw Exception()": False negative UNREACHABLE_CODE warning
- [`KT-22317`](https://youtrack.jetbrains.com/issue/KT-22317) No INITIALIZATION_BEFORE_DECLARATION without primary constructor
- [`KT-67307`](https://youtrack.jetbrains.com/issue/KT-67307) K2: "Cannot find cached type parameter by FIR symbol" in JpaRepository.saveAll
- [`KT-67185`](https://youtrack.jetbrains.com/issue/KT-67185) K2: Incorrect coercion-to-Unit leading to CCE at runtime
- [`KT-64891`](https://youtrack.jetbrains.com/issue/KT-64891) K2: consider supporting/forbidding foo.(bar)() syntax
- [`KT-59480`](https://youtrack.jetbrains.com/issue/KT-59480) K2: build moko-resources
- [`KT-65771`](https://youtrack.jetbrains.com/issue/KT-65771) K2: "IndexOutOfBoundsException: Cannot pop operand off an empty stack" when calling method imported using typealias as callable qualifier
- [`KT-67502`](https://youtrack.jetbrains.com/issue/KT-67502) K2: "property must be initialized or be abstract" with try-finally in secondary constructor
- [`KT-67456`](https://youtrack.jetbrains.com/issue/KT-67456) K2: "property must be initialized or be abstract" depending on constructor declaration order
- [`KT-63524`](https://youtrack.jetbrains.com/issue/KT-63524) K2: "Not enough information to infer type argument"
- [`KT-67628`](https://youtrack.jetbrains.com/issue/KT-67628) K2: "IllegalArgumentException: Expected nullable type" — alias of nullable type analyzed as non-nullable in type parameter
- [`KT-67625`](https://youtrack.jetbrains.com/issue/KT-67625) K2: Array aliases can't be used as vararg values
- [`KT-67624`](https://youtrack.jetbrains.com/issue/KT-67624) K2: False negative "The feature "break continue in inline lambdas" is experimental and should be enabled explicitly" in elvis operator
- [`KT-61787`](https://youtrack.jetbrains.com/issue/KT-61787) K2 doesn't report warnings for some Gradle tasks
- [`KT-62550`](https://youtrack.jetbrains.com/issue/KT-62550) K2: Different JVM signature of lambda with `Unit` return type
- [`KT-65120`](https://youtrack.jetbrains.com/issue/KT-65120) K2 Consider turn into platform checkers ones which checks for objC
- [`KT-60271`](https://youtrack.jetbrains.com/issue/KT-60271) K2: origins are not set on compare operators
- [`KT-28695`](https://youtrack.jetbrains.com/issue/KT-28695) Compiler does not detect uninitialized property in lambda
- [`KT-67593`](https://youtrack.jetbrains.com/issue/KT-67593) K2: false negative SUPER_CALL_WITH_DEFAULT_PARAMETERS
- [`KT-67484`](https://youtrack.jetbrains.com/issue/KT-67484) K2: FIR2IR generates incorrect access to f/o of lateinit internal var
- [`KT-47382`](https://youtrack.jetbrains.com/issue/KT-47382) JVM / IR: "AssertionError: Unbound private symbol IrFieldSymbolImpl" caused by string template in constructor and extension property
- [`KT-67581`](https://youtrack.jetbrains.com/issue/KT-67581) K2: Compiler fails on actualizing abstract class with sealed Java class via type alias
- [`KT-22379`](https://youtrack.jetbrains.com/issue/KT-22379) Condition of while-loop with break can produce unsound smartcast
- [`KT-67021`](https://youtrack.jetbrains.com/issue/KT-67021) K2: Cannot find cached type parameter by FIR symbol: E of the owner: FirRegularClassSymbol Function
- [`KT-67014`](https://youtrack.jetbrains.com/issue/KT-67014) K1/K2 handle when expression as annotation target differently
- [`KT-67254`](https://youtrack.jetbrains.com/issue/KT-67254) K1/K2 both allow annotations on loops, assignments, array sets
- [`KT-66960`](https://youtrack.jetbrains.com/issue/KT-66960) K2. KMP. False negative ` 'when' expression must be exhaustive` without sealed class inheritor from common source-set
- [`KT-65578`](https://youtrack.jetbrains.com/issue/KT-65578) K2: implement a deprecation warning for KT-57014 (wrong nullability returned from JDK SAM constructor lambda)
- [`KT-63466`](https://youtrack.jetbrains.com/issue/KT-63466) `@NonNull` on a type-variable usage doesn't take precedence over a wildcard type argument
- [`KT-56134`](https://youtrack.jetbrains.com/issue/KT-56134) K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER diagnostic is reported for the wrong symbol
- [`KT-66196`](https://youtrack.jetbrains.com/issue/KT-66196) Convert INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_ERROR to warning
- [`KT-66793`](https://youtrack.jetbrains.com/issue/KT-66793) K2: "assigning single elements to varargs in named form is prohibited." caused by varargs supplied from java with elvis operator
- [`KT-59872`](https://youtrack.jetbrains.com/issue/KT-59872) K2: Disappeared TYPE_MISMATCH
- [`KT-67192`](https://youtrack.jetbrains.com/issue/KT-67192) K2: Disappeared TYPE_MISMATCH [3]
- [`KT-63319`](https://youtrack.jetbrains.com/issue/KT-63319) K1/K2: inconsistent behavior around NullMarked and type parameter based types
- [`KT-59882`](https://youtrack.jetbrains.com/issue/KT-59882) K2: Disappeared CANNOT_INFER_PARAMETER_TYPE
- [`KT-67191`](https://youtrack.jetbrains.com/issue/KT-67191) K2: Disappeared TYPE_MISMATCH [4]
- [`KT-53752`](https://youtrack.jetbrains.com/issue/KT-53752) Missed subtyping check for an intersection type
- [`KT-52628`](https://youtrack.jetbrains.com/issue/KT-52628) Deprecate SAM constructor usages which require OptIn without annotation
- [`KT-54066`](https://youtrack.jetbrains.com/issue/KT-54066) Deprecate upper bound violation in typealias constructors
- [`KT-64860`](https://youtrack.jetbrains.com/issue/KT-64860) K2: Consider using different ConstraintPosition when fixing variables for PCLA
- [`KT-67189`](https://youtrack.jetbrains.com/issue/KT-67189) K2: Disappeared TYPE_MISMATCH [5]
- [`KT-67551`](https://youtrack.jetbrains.com/issue/KT-67551) K2: No wrong annotation target error for `for` statement
- [`KT-67374`](https://youtrack.jetbrains.com/issue/KT-67374) K2: Object is not smartcasted to type parameter type
- [`KT-67264`](https://youtrack.jetbrains.com/issue/KT-67264) K2: "argument type mismatch" with suspend lambda and java wildcard
- [`KT-63257`](https://youtrack.jetbrains.com/issue/KT-63257) K2: FIR2IR inserts incorrect implicit cast for smartcasted variable
- [`KT-66902`](https://youtrack.jetbrains.com/issue/KT-66902) K2: "Named arguments are prohibited for non-Kotlin functions" with Java interop
- [`KT-67311`](https://youtrack.jetbrains.com/issue/KT-67311) K2: "Argument type mismatch" caused by lambda type when using named arguments
- [`KT-57011`](https://youtrack.jetbrains.com/issue/KT-57011) Make real type of a destructuring variable consistent with explicit type when specified
- [`KT-62043`](https://youtrack.jetbrains.com/issue/KT-62043) K2: Fix FirCompileKotlinAgainstCustomBinariesTest.testRawTypes
- [`KT-66256`](https://youtrack.jetbrains.com/issue/KT-66256) K2: compiler FIR2IR crash on SAM-conversion to value parameter of in-projected type
- [`KT-67124`](https://youtrack.jetbrains.com/issue/KT-67124) "Unstable inference behaviour with multiple generic lambdas" compilation error
- [`KT-59791`](https://youtrack.jetbrains.com/issue/KT-59791) K2: Implement partially constrained lambda analysis
- [`KT-66743`](https://youtrack.jetbrains.com/issue/KT-66743) Lambda receivers and anonymous function parameters of inaccessible types are allowed
- [`KT-67315`](https://youtrack.jetbrains.com/issue/KT-67315) K2: Some default imports are not excluded
- [`KT-56126`](https://youtrack.jetbrains.com/issue/KT-56126) Avoid using descriptors at JvmPlatformAnalyzerServices::computePlatformSpecificDefaultImports
- [`KT-66513`](https://youtrack.jetbrains.com/issue/KT-66513) K2: Suppressing OPT_IN_USAGE_ERROR is now a warning in K2, preventing safe code gen compatible with -Werror
- [`KT-67233`](https://youtrack.jetbrains.com/issue/KT-67233) False negative UNSAFE_CALL with type check after null coalescing with 'OR'
- [`KT-52802`](https://youtrack.jetbrains.com/issue/KT-52802) Report ambiguity resolving between property/field and enum entry
- [`KT-64920`](https://youtrack.jetbrains.com/issue/KT-64920) Json.encodeToString yields different results depending on whether typealias is used
- [`KT-58260`](https://youtrack.jetbrains.com/issue/KT-58260) Make invoke convention work consistently with expected desugaring
- [`KT-67314`](https://youtrack.jetbrains.com/issue/KT-67314) PCLA works inconsistently with smart-cast related CS forks
- [`KT-66797`](https://youtrack.jetbrains.com/issue/KT-66797) K2 JS: Primary constructor property annotation with target VALUE_PARAMETER is put on property instead of parameter
- [`KT-55179`](https://youtrack.jetbrains.com/issue/KT-55179) False negative PRIVATE_CLASS_MEMBER_FROM_INLINE on calling private class companion object member from internal inline function
- [`KT-54663`](https://youtrack.jetbrains.com/issue/KT-54663) Projected types don't take into account in-place not null types
- [`KT-58191`](https://youtrack.jetbrains.com/issue/KT-58191) K2: capturing closures successors that are already resolved (thanks to backward edges) must be taken into account for allowing smart casts
- [`KT-67144`](https://youtrack.jetbrains.com/issue/KT-67144) K2: potential NPE when assigning to unstable vars
- [`KT-66971`](https://youtrack.jetbrains.com/issue/KT-66971) K2: missing SMARTCAST_IMPOSSIBLE on open val declared in another module
- [`KT-66904`](https://youtrack.jetbrains.com/issue/KT-66904) K2: possible NPE when reassigning captured variables
- [`KT-57031`](https://youtrack.jetbrains.com/issue/KT-57031) operator assignment, increment/decrement should be considered as variable reassigning in terms of DFA. green in K1 -> red in K2 for unsound code
- [`KT-67212`](https://youtrack.jetbrains.com/issue/KT-67212) K2: "Failed to find functional supertype for class org.jetbrains.kotlin.fir.types.ConeCapturedType"
- [`KT-67283`](https://youtrack.jetbrains.com/issue/KT-67283) K2: No SAM conversion for fun interface with abstract toString
- [`KT-67318`](https://youtrack.jetbrains.com/issue/KT-67318) Compiler fails with OutOfMemoryError on combination of PCLA+smart cast
- [`KT-66956`](https://youtrack.jetbrains.com/issue/KT-66956) K2: false negative CONST_VAL_WITH_NON_CONST_INITIALIZER for inc/dec operators
- [`KT-64233`](https://youtrack.jetbrains.com/issue/KT-64233) K2: K1/K2: ensure JVM ABI consistency for quality gates projects
- [`KT-63535`](https://youtrack.jetbrains.com/issue/KT-63535) K2: Apply DFA implications for nullable Nothing to both sides
- [`KT-63413`](https://youtrack.jetbrains.com/issue/KT-63413) K2 / kotlinx-atomicfu: "IllegalStateException: Expected some types"
- [`KT-62931`](https://youtrack.jetbrains.com/issue/KT-62931) K2: extra class files for `@OptionalExpectation` marked annotations
- [`KT-34307`](https://youtrack.jetbrains.com/issue/KT-34307) Confusing error message on lambda return type mismatch
- [`KT-62151`](https://youtrack.jetbrains.com/issue/KT-62151) K2. overload resolution ambiguity for calls of Java record compact constructors
- [`KT-60732`](https://youtrack.jetbrains.com/issue/KT-60732) K2 Scripting: TeamCity DSL test
- [`KT-59467`](https://youtrack.jetbrains.com/issue/KT-59467) K2: build toolbox-enterprise
- [`KT-67205`](https://youtrack.jetbrains.com/issue/KT-67205) K2: can't deserialize annotation with local class as argument
- [`KT-52175`](https://youtrack.jetbrains.com/issue/KT-52175) K2: WRONG_ANNOTATION_TARGET for annotation that used inside if
- [`KT-65449`](https://youtrack.jetbrains.com/issue/KT-65449) K2: build KAPT user project and pass it to CI
- [`KT-61384`](https://youtrack.jetbrains.com/issue/KT-61384) IrFakeOverrideBuilder incorrectly checks visibility for friend modules
- [`KT-67142`](https://youtrack.jetbrains.com/issue/KT-67142) K2: IrFakeOverrideBuilder: AbstractMethodError on raw type argument in a Java superclass
- [`KT-65105`](https://youtrack.jetbrains.com/issue/KT-65105) K2 / Native: Member overrides different '`@Throws`' filter
- [`KT-62570`](https://youtrack.jetbrains.com/issue/KT-62570) IncompatibleClassChangeError due to overriding final method
- [`KT-57812`](https://youtrack.jetbrains.com/issue/KT-57812) K2: support serialization of type annotation's arguments
- [`KT-67190`](https://youtrack.jetbrains.com/issue/KT-67190) K2: Disappeared TYPE_MISMATCH [2]
- [`KT-56683`](https://youtrack.jetbrains.com/issue/KT-56683) K2: No control flow analysis for top-level properties
- [`KT-67188`](https://youtrack.jetbrains.com/issue/KT-67188) K2: Disappeared TYPE_MISMATCH [6]
- [`KT-62063`](https://youtrack.jetbrains.com/issue/KT-62063) K2: drop pre-release flag in 2.0-RC
- [`KT-67187`](https://youtrack.jetbrains.com/issue/KT-67187) K2: Disappeared TYPE_MISMATCH [1]
- [`KT-66909`](https://youtrack.jetbrains.com/issue/KT-66909) K2: Implement a diagnostic for returning null from a lambda with expected return type Unit!
- [`KT-66534`](https://youtrack.jetbrains.com/issue/KT-66534) False positive ASSIGNMENT_TYPE_MISMATCH in lambdas with expected return type Unit!
- [`KT-63381`](https://youtrack.jetbrains.com/issue/KT-63381) IrFakeOverrideBuilder: PublishedApi affects overridability of internal members
- [`KT-63836`](https://youtrack.jetbrains.com/issue/KT-63836) K2: No deprecation error message in common metadata compilation
- [`KT-57618`](https://youtrack.jetbrains.com/issue/KT-57618) K2: complex deprecation messages are not printed in the error
- [`KT-59856`](https://youtrack.jetbrains.com/issue/KT-59856) K2: Check ConeDiagnostics that are not mapped to KtDiagnostics
- [`KT-57502`](https://youtrack.jetbrains.com/issue/KT-57502) K2: Smart casts should be forbidden if variable that remembers the smart cast is declared by delegation
- [`KT-63967`](https://youtrack.jetbrains.com/issue/KT-63967) K2: Missing getterSignature in metadata for script variables
- [`KT-59372`](https://youtrack.jetbrains.com/issue/KT-59372) K2: Missing SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR
- [`KT-60526`](https://youtrack.jetbrains.com/issue/KT-60526) K2: Fix the TODO in `convertToIr.kt`
- [`KT-67090`](https://youtrack.jetbrains.com/issue/KT-67090) K2: Exception from metadata compilation when compiling class with annotations from dependencies
- [`KT-59479`](https://youtrack.jetbrains.com/issue/KT-59479) K2: build KorGE
- [`KT-64502`](https://youtrack.jetbrains.com/issue/KT-64502) K2: Internal error on calling function before declaration
- [`KT-62560`](https://youtrack.jetbrains.com/issue/KT-62560) K2: KAPT4: annotation `@ReplaceWith` is missing a default value for the element 'imports'
- [`KT-67027`](https://youtrack.jetbrains.com/issue/KT-67027) K2: Review all use-sites of annotation arguments utilities
- [`KT-65012`](https://youtrack.jetbrains.com/issue/KT-65012) IR Evaluator: `NoSuchFieldException` when evaluating protected/private fields of superclasses
- [`KT-66953`](https://youtrack.jetbrains.com/issue/KT-66953) K2: toByte() call on Char leads to ClassCastException for klib backends
- [`KT-60096`](https://youtrack.jetbrains.com/issue/KT-60096) K2: Introduced API_NOT_AVAILABLE
- [`KT-59484`](https://youtrack.jetbrains.com/issue/KT-59484) K2: build trustwallet sample
- [`KT-64151`](https://youtrack.jetbrains.com/issue/KT-64151) K2: consider implementing FIR-level constant evaluation
- [`KT-65787`](https://youtrack.jetbrains.com/issue/KT-65787) K2: "KotlinIllegalArgumentExceptionWithAttachments: Expected FirResolvedTypeRef with ConeKotlinType" caused by passing lambda expression with multiple labels to function
- [`KT-53629`](https://youtrack.jetbrains.com/issue/KT-53629) K2: forbid multiple labels per statement
- [`KT-65255`](https://youtrack.jetbrains.com/issue/KT-65255) K2 / KJS: "IllegalArgumentException: Candidate is not successful, but system has no contradiction"
- [`KT-65195`](https://youtrack.jetbrains.com/issue/KT-65195) K2: Unexpected exception when executing dynamic array element inc/dec
- [`KT-63416`](https://youtrack.jetbrains.com/issue/KT-63416) K2 / Contracts: False positive "Leaked in-place lambda" warning caused by suspend lambda with callsInPlace contract
- [`KT-66717`](https://youtrack.jetbrains.com/issue/KT-66717) Incorrect diagnostics around intersection property overrides
- [`KT-63540`](https://youtrack.jetbrains.com/issue/KT-63540) Restrict the CONFLICTING_OVERLOADS + DeprecatedLevel.HIDDEN ignore to final callables
- [`KT-56587`](https://youtrack.jetbrains.com/issue/KT-56587) There are no warnings in some cases when Enum.entries is shadowed
- [`KT-65111`](https://youtrack.jetbrains.com/issue/KT-65111) K2: Java star imports don't work in KJK interdependencies
- [`KT-63709`](https://youtrack.jetbrains.com/issue/KT-63709) K2: Argument smartcasting impacting receiver and call resolution for implicit invoke
- [`KT-63530`](https://youtrack.jetbrains.com/issue/KT-63530) K2: Disable passing data flow info from in-place lambdas
- [`KT-65377`](https://youtrack.jetbrains.com/issue/KT-65377) K2: "Argument type mismatch" caused by approximated captured type argument of generic type
- [`KT-59400`](https://youtrack.jetbrains.com/issue/KT-59400) K2: Missing CANNOT_INFER_VISIBILITY
- [`KT-62305`](https://youtrack.jetbrains.com/issue/KT-62305) K2: Missing Fir metadata serialization support for scripts
- [`KT-64534`](https://youtrack.jetbrains.com/issue/KT-64534) K2: org.jetbrains.kotlin.util.FileAnalysisException: Somewhere in file
- [`KT-57555`](https://youtrack.jetbrains.com/issue/KT-57555) [LC] Forbid deferred initialization of open properties with backing field
- [`KT-65776`](https://youtrack.jetbrains.com/issue/KT-65776) [LC] K2 breaks `false && ...` and `false || ...`
- [`KT-64641`](https://youtrack.jetbrains.com/issue/KT-64641) K2: Change in inference of supertype of function types with receiver
- [`KT-65649`](https://youtrack.jetbrains.com/issue/KT-65649) K2: IR has incorrect origins for some inplace updating operators
- [`KT-64295`](https://youtrack.jetbrains.com/issue/KT-64295) Forbid recursive resolve in case of potential ambiguity on upper tower level
- [`KT-62866`](https://youtrack.jetbrains.com/issue/KT-62866) K2: Change qualifier resolution behavior when companion object is preferred against static scope
- [`KT-55446`](https://youtrack.jetbrains.com/issue/KT-55446) Change impact of _private-to-this_ visibility to resolution
- [`KT-64255`](https://youtrack.jetbrains.com/issue/KT-64255) Forbid accessing internal setter from a derived class in another module
- [`KT-64966`](https://youtrack.jetbrains.com/issue/KT-64966) Forbid generic delegating constructor calls with wrong type for generic parameter
- [`KT-63389`](https://youtrack.jetbrains.com/issue/KT-63389) K2: `WRONG_ANNOTATION_TARGET` is reported on incompatible annotations of a type wrapped into `()?`
- [`KT-66748`](https://youtrack.jetbrains.com/issue/KT-66748) K2: False-positive AMBIGUOUS_SUPER in toString
- [`KT-67013`](https://youtrack.jetbrains.com/issue/KT-67013) K2: ClassCastException: class FirConstructorSymbol cannot be cast to class FirNamedFunctionSymbol
- [`KT-64872`](https://youtrack.jetbrains.com/issue/KT-64872) K2: do-while condition able to access uninitialized variable
- [`KT-66350`](https://youtrack.jetbrains.com/issue/KT-66350) K2: "IllegalStateException: Unsupported compile-time value STRING_CONCATENATION" when evaluating an annotation argument string
- [`KT-61798`](https://youtrack.jetbrains.com/issue/KT-61798) K2 incorrectly calculates modality of property accessors
- [`KT-65035`](https://youtrack.jetbrains.com/issue/KT-65035) IrFakeOverrideBuilder: AbstractMethodError on inheritance from Java subclass of CharSequence with inherited implementations
- [`KT-61579`](https://youtrack.jetbrains.com/issue/KT-61579) K2: Inconsistent reporting `UNINITIALIZED_VARIABLE` for top-level properties
- [`KT-66730`](https://youtrack.jetbrains.com/issue/KT-66730) K2: False positive RETURN_TYPE_MISMATCH in return statement in SAM constructor
- [`KT-66570`](https://youtrack.jetbrains.com/issue/KT-66570) Generic wildcard upper bound inference error
- [`KT-65272`](https://youtrack.jetbrains.com/issue/KT-65272) K2: invoke operator applies "restricted suspending call" error differently than K1
- [`KT-66148`](https://youtrack.jetbrains.com/issue/KT-66148) K2. Sources of receivers updated twice because of PCLA
- [`KT-62525`](https://youtrack.jetbrains.com/issue/KT-62525) K2: IllegalStateException: Can't find KotlinType in IrErrorType: IrErrorType(null)
- [`KT-64266`](https://youtrack.jetbrains.com/issue/KT-64266) K2: don't report MISSING_DEPENDENCY_CLASS on lambda parameter for non-generic types
- [`KT-65300`](https://youtrack.jetbrains.com/issue/KT-65300) K2: this-expressions in initializers and local declarations don't introduce type information to either BI or PCLA
- [`KT-66463`](https://youtrack.jetbrains.com/issue/KT-66463) K2: false positive ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE
- [`KT-62356`](https://youtrack.jetbrains.com/issue/KT-62356) Prohibit using property+invoke for iterator
- [`KT-63631`](https://youtrack.jetbrains.com/issue/KT-63631) K2: constant value UByte.MAX_VALUE is incorrectly deserialized from metadata
- [`KT-65386`](https://youtrack.jetbrains.com/issue/KT-65386) K2: Different signature of invoke for Unit lambda
- [`KT-60574`](https://youtrack.jetbrains.com/issue/KT-60574) K2: generated IR for `suspendCoroutineUninterceptedOrReturn` is different from K1 (K2 uses Any? instead of Unit)
- [`KT-66512`](https://youtrack.jetbrains.com/issue/KT-66512) K2: Incorrect diagnostic in lambda whose expected type is a type alias to Unit
- [`KT-66279`](https://youtrack.jetbrains.com/issue/KT-66279) K2: False positive INITIALIZER_TYPE_MISMATCH with `return Unit` in a lambda with the expected type `() -> Unit`
- [`KT-66277`](https://youtrack.jetbrains.com/issue/KT-66277) K2: False negative RETURN_TYPE_MISMATCH with empty return in lambda assigned to a property
- [`KT-66654`](https://youtrack.jetbrains.com/issue/KT-66654) K2 FIR resolution: Mismatch between actual type and expected type for a value parameter when the parameter type is a function type with special function kind
- [`KT-66638`](https://youtrack.jetbrains.com/issue/KT-66638) Cannot access properties of a generic type with wildcards
- [`KT-66690`](https://youtrack.jetbrains.com/issue/KT-66690) K2: don't report MISSING_DEPENDENCY_CLASS on expression without errors for generic type arguments
- [`KT-66767`](https://youtrack.jetbrains.com/issue/KT-66767) K2: Destructuring declaration inside initializer failure
- [`KT-63695`](https://youtrack.jetbrains.com/issue/KT-63695) JVM: Don't use plugin extensions when compiling code fragment
- [`KT-65727`](https://youtrack.jetbrains.com/issue/KT-65727) K2: add proper package for properties generated from destructuring declarations
- [`KT-64854`](https://youtrack.jetbrains.com/issue/KT-64854) K2: Trying to access private field on runtime with contracts
- [`KT-65388`](https://youtrack.jetbrains.com/issue/KT-65388) IrFakeOverrideBuilder - custom annotation is available in fake getter/setter
- [`KT-66595`](https://youtrack.jetbrains.com/issue/KT-66595) K2: compiler FIR checking crash on destructuring declarations calling hidden componentN declarations
- [`KT-62129`](https://youtrack.jetbrains.com/issue/KT-62129) K2: Verification error on calling an extension from an env with 2+ context receivers
- [`KT-41607`](https://youtrack.jetbrains.com/issue/KT-41607) NI: UNSAFE_CALL caused by try catch block assigning to a nullable variable
- [`KT-63932`](https://youtrack.jetbrains.com/issue/KT-63932) K2/Native codegen test failures around builder inference
- [`KT-66352`](https://youtrack.jetbrains.com/issue/KT-66352) K2: difference between LL FIR and FIR for componentN functions
- [`KT-66686`](https://youtrack.jetbrains.com/issue/KT-66686) K2 Script: Unresolved reference of script-specific entities on out-of-order resolve
- [`KT-65523`](https://youtrack.jetbrains.com/issue/KT-65523) K2: add proper package for result$$ property
- [`KT-66699`](https://youtrack.jetbrains.com/issue/KT-66699) Restore HostManager ABI
- [`KT-60533`](https://youtrack.jetbrains.com/issue/KT-60533) Inliner incorrectly captures non-null value as null in coroutines
- [`KT-57925`](https://youtrack.jetbrains.com/issue/KT-57925) K2: Consider removing FirEmptyContractDescription
- [`KT-61893`](https://youtrack.jetbrains.com/issue/KT-61893) K2: should not resolve to Java function with Kotlin hidden-level deprecation
- [`KT-59669`](https://youtrack.jetbrains.com/issue/KT-59669) K2: Explore assignments in in-place lambdas
- [`KT-66271`](https://youtrack.jetbrains.com/issue/KT-66271) Fir: Deserialize classFile, functionFile and propertyFile from KlibMetadataProtoBuf
- [`KT-57957`](https://youtrack.jetbrains.com/issue/KT-57957) K2: Symbol providers are frequently queried with error-named class IDs
- [`KT-66046`](https://youtrack.jetbrains.com/issue/KT-66046) K2: false negative CANNOT_WEAKEN_ACCESS_PRIVILEGE on property
- [`KT-66677`](https://youtrack.jetbrains.com/issue/KT-66677) K2: OVERRIDE_DEPRECATION isn't reported for WEAKLY_HIDDEN method toArray()
- [`KT-62793`](https://youtrack.jetbrains.com/issue/KT-62793) K2: slightly different bytecode of suspend conversions
- [`KT-57244`](https://youtrack.jetbrains.com/issue/KT-57244) K2: slightly different naming scheme for suspend conversion adapters
- [`KT-60256`](https://youtrack.jetbrains.com/issue/KT-60256) K2: types are not substituted in suspend conversion
- [`KT-66673`](https://youtrack.jetbrains.com/issue/KT-66673) K2/JS: FirJsInheritanceClassChecker doesn't expand type aliases to supertypes
- [`KT-66475`](https://youtrack.jetbrains.com/issue/KT-66475) K2/KMP/Wasm: report WRONG_JS_INTEROP_TYPE from a platform checker
- [`KT-66474`](https://youtrack.jetbrains.com/issue/KT-66474) K2/KMP/JS: report EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE from a platform checker
- [`KT-66473`](https://youtrack.jetbrains.com/issue/KT-66473) K2/Wasm: FirWasmExternalInheritanceChecker doesn't expand type aliases
- [`KT-64407`](https://youtrack.jetbrains.com/issue/KT-64407) Implement WriteSignatureTestGenerated for K2
- [`KT-64438`](https://youtrack.jetbrains.com/issue/KT-64438) K2: Port CodegenTestCase to K2
- [`KT-64404`](https://youtrack.jetbrains.com/issue/KT-64404) Implement WriteFlagsTestGenerated for K2
- [`KT-66491`](https://youtrack.jetbrains.com/issue/KT-66491) K2 / KJS: "Name contains illegal characters." caused by backticks in import
- [`KT-66275`](https://youtrack.jetbrains.com/issue/KT-66275) K2: false-positive "Java module does not depend on module" error on access to inherited member from twice-transitive dependency via class from transitive dependency
- [`KT-65801`](https://youtrack.jetbrains.com/issue/KT-65801) IrFakeOverrideBuilder - visibility is lost for setter in KJK hierarchy
- [`KT-65576`](https://youtrack.jetbrains.com/issue/KT-65576) K2: Incorrect resolution of variable+invoke when the property type is not computed
- [`KT-58575`](https://youtrack.jetbrains.com/issue/KT-58575) Private Kotlin property prevents use of Java get- and set-methods from Java-Kotlin-Java hierarchy
- [`KT-61282`](https://youtrack.jetbrains.com/issue/KT-61282) K2: Incorrect overridden function for `java.nio.CharBuffer.get`
- [`KT-65464`](https://youtrack.jetbrains.com/issue/KT-65464) K2: False positive UNRESOLVED_REFERENCE on extension property call defined in KJK hierarchy
- [`KT-59470`](https://youtrack.jetbrains.com/issue/KT-59470) K2: build KaMPKit
- [`KT-60510`](https://youtrack.jetbrains.com/issue/KT-60510) Smartcast to functional type does not work in when exprssion
- [`KT-59677`](https://youtrack.jetbrains.com/issue/KT-59677) K2: Report diagnostics about missing receiver for delegated constructor call to inner class
- [`KT-65183`](https://youtrack.jetbrains.com/issue/KT-65183) K2: Remove workaround for `@OnlyInputTypes` and captured types with recursive supertypes from inference
- [`KT-66120`](https://youtrack.jetbrains.com/issue/KT-66120) IrFakeOverrideBuilder: wrong return type in intersection with 3 classes
- [`KT-65939`](https://youtrack.jetbrains.com/issue/KT-65939) IrFakeOverrideBuilder - nullability annotation is lost in intersection without annotation
- [`KT-59473`](https://youtrack.jetbrains.com/issue/KT-59473) K2: build firebase-kotlin-sdk
- [`KT-66356`](https://youtrack.jetbrains.com/issue/KT-66356) K2: type mismatch error when generic type with inaccessible generic type as type argument is produced and consumed by declarations from dependencies
- [`KT-65193`](https://youtrack.jetbrains.com/issue/KT-65193) K2: "JAVA_TYPE_MISMATCH" caused by MutableList
- [`KT-66636`](https://youtrack.jetbrains.com/issue/KT-66636) NoSuchMethodError: 'void org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl.<init> in the FLysto K2 QG
- [`KT-63941`](https://youtrack.jetbrains.com/issue/KT-63941) K2: "IllegalStateException: Unsupported compile-time value STRING_CONCATENATION" caused by class reference in string expression as annotation parameter
- [`KT-65704`](https://youtrack.jetbrains.com/issue/KT-65704) K2: `computeCommonSuperType` of flexible type with recursive captured type argument produces giant multi-level-deep type
- [`KT-65410`](https://youtrack.jetbrains.com/issue/KT-65410) K2: ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED  for 'removeAt' in KJK hierarchy
- [`KT-65184`](https://youtrack.jetbrains.com/issue/KT-65184) K2: disappeared TYPE_MISMATCH for java collections
- [`KT-66392`](https://youtrack.jetbrains.com/issue/KT-66392) K2: Exception in KJK hierarchy with implicit types
- [`KT-66551`](https://youtrack.jetbrains.com/issue/KT-66551) Revert temporary commits after KT-62063 and bootstrapping
- [`KT-65218`](https://youtrack.jetbrains.com/issue/KT-65218) FIR LL and DiagnosticFE10 tests start to fail in case of adding any new declaration into stdlib commonMain
- [`KT-66552`](https://youtrack.jetbrains.com/issue/KT-66552) K2: build of intellij crashes the compiler
- [`KT-63746`](https://youtrack.jetbrains.com/issue/KT-63746) K2: JSpecify: If a class has a `@Nullable` type-parameter bound, Kotlin should still treat unbounded wildcards like platform types
- [`KT-66504`](https://youtrack.jetbrains.com/issue/KT-66504) K2: plusAssign operator call is resolved differently than function call
- [`KT-48515`](https://youtrack.jetbrains.com/issue/KT-48515) JSpecify: If a class has a `@Nullable` type-parameter bound, Kotlin should still treat unbounded wildcards like platform types
- [`KT-57588`](https://youtrack.jetbrains.com/issue/KT-57588) K2/Native: False positive '"CONFLICTING_OVERLOADS", "PARAMETER_NAME_CHANGED_ON_OVERRIDE" on overriding objc methods
- [`KT-58892`](https://youtrack.jetbrains.com/issue/KT-58892) K2: Parcelize doesn't work in common code when expect annotation is actualized with typealias to `@Parcelize`
- [`KT-65882`](https://youtrack.jetbrains.com/issue/KT-65882) K2: "KotlinNothingValueException" caused by unsafe cast and Nothing::class
- [`KT-66124`](https://youtrack.jetbrains.com/issue/KT-66124) K2: Remove FirLambdaArgumentExpression and FirNamedArgumentExpression after resolution
- [`KT-65959`](https://youtrack.jetbrains.com/issue/KT-65959) K2: Incorrect warnings about inline function impact
- [`KT-64994`](https://youtrack.jetbrains.com/issue/KT-64994) K2: `@Composable` lambda type is not resolved from other modules
- [`KT-66048`](https://youtrack.jetbrains.com/issue/KT-66048) K2: property becomes nullable in KJK hierarchy if base declaration has implicit return type
- [`KT-47843`](https://youtrack.jetbrains.com/issue/KT-47843) No error reported on assigning "continue" to a companion object
- [`KT-47530`](https://youtrack.jetbrains.com/issue/KT-47530) NI: Unexpected TYPE_MISMATCH when combining nested conditional and contravariant type argument
- [`KT-49583`](https://youtrack.jetbrains.com/issue/KT-49583) NI: NullPointerException on compiling anonymous function returning a method reference
- [`KT-42782`](https://youtrack.jetbrains.com/issue/KT-42782) NI: Smart casting for generic type doesn't work if the variable is already smart cast
- [`KT-38031`](https://youtrack.jetbrains.com/issue/KT-38031) FIR: Discrepancy in call resolution for qualifiers with old FE
- [`KT-65789`](https://youtrack.jetbrains.com/issue/KT-65789) K1/K2: Resolve change in constructor/top-level function ambiguity
- [`KT-66150`](https://youtrack.jetbrains.com/issue/KT-66150) K2: expects type argument in super qualifier
- [`KT-60971`](https://youtrack.jetbrains.com/issue/KT-60971) Incorrect "cannot inline bytecode built with JVM target ..." on property setter if only getter is inline
- [`KT-61514`](https://youtrack.jetbrains.com/issue/KT-61514) K2: Build fake overrides using IR during Fir2IR
- [`KT-65584`](https://youtrack.jetbrains.com/issue/KT-65584) K2: "Duplicate parameter name in a function type"
- [`KT-50008`](https://youtrack.jetbrains.com/issue/KT-50008) JSpecify `@Nullable` annotation on type-parameter bound prevents type-variable usages from being platform types
- [`KT-37000`](https://youtrack.jetbrains.com/issue/KT-37000) IndexOutOfBoundsException from TypeResolver on typealias with cyclic references
- [`KT-56988`](https://youtrack.jetbrains.com/issue/KT-56988) CFG, smart casts: red in K1 -> green in K2 for invalid code
- [`KT-62118`](https://youtrack.jetbrains.com/issue/KT-62118) FIR: "HashMap.entry" has invalid enhanced type
- [`KT-64840`](https://youtrack.jetbrains.com/issue/KT-64840) K2: Bare type are not allowed for TV based values during PCLA
- [`KT-65415`](https://youtrack.jetbrains.com/issue/KT-65415) K2:  Stdlib K2 build error: IrConstructorSymbolImpl is already bound
- [`KT-66449`](https://youtrack.jetbrains.com/issue/KT-66449) Make DiagnosticSuppressor a project-level extension
- [`KT-66411`](https://youtrack.jetbrains.com/issue/KT-66411) FIR: Real source on fake block around assignment expression in the "when" branch affects resolve in K2 Analysis API and IDE
- [`KT-65249`](https://youtrack.jetbrains.com/issue/KT-65249) K2: False positive modality is different for native compilation
- [`KT-65982`](https://youtrack.jetbrains.com/issue/KT-65982) K2 Scripts cannot disambiguate declarations imported from default and explicit imports
- [`KT-65677`](https://youtrack.jetbrains.com/issue/KT-65677) K2: Unable to resolve parent class from companion object
- [`KT-47310`](https://youtrack.jetbrains.com/issue/KT-47310) Change qualifier resolution behavior when companion property is preferred against enum entry
- [`KT-41034`](https://youtrack.jetbrains.com/issue/KT-41034) K2: Change evaluation semantics for combination of safe calls and convention operators
- [`KT-63529`](https://youtrack.jetbrains.com/issue/KT-63529) K2: Compiler does not detect tailrec call with nullable type
- [`KT-66441`](https://youtrack.jetbrains.com/issue/KT-66441) Remove symbol table from IR fake override builder in Fir2Ir
- [`KT-64846`](https://youtrack.jetbrains.com/issue/KT-64846) K2: false negative CONFLICTING_JVM_DECLARATIONS on inheritance from Java collection subclass with a conflicting override
- [`KT-62312`](https://youtrack.jetbrains.com/issue/KT-62312) [K2/N] revert putting stdlib to the beginning of libraries list in the compiler
- [`KT-58203`](https://youtrack.jetbrains.com/issue/KT-58203) K2: false-negative incompatible types error on is-check with unrelated type
- [`KT-65722`](https://youtrack.jetbrains.com/issue/KT-65722) K2: Property reference refers to non-existent functions
- [`KT-65878`](https://youtrack.jetbrains.com/issue/KT-65878) K2: "ClassCastException" when passing nun-suspend lambda to SAM constructor with named argument
- [`KT-66379`](https://youtrack.jetbrains.com/issue/KT-66379) K2: No extra message in UPPER_BOUND_VIOLATED  for cases with CapturedType
- [`KT-59475`](https://youtrack.jetbrains.com/issue/KT-59475) K2: build nowinandroid
- [`KT-65926`](https://youtrack.jetbrains.com/issue/KT-65926) K2: add tests for all fixed-in-k2 / not-reproducible-in-k2 unresolved issues
- [`KT-59481`](https://youtrack.jetbrains.com/issue/KT-59481) K2: build aws-sdk-kotlin + smithy-kotlin
- [`KT-65022`](https://youtrack.jetbrains.com/issue/KT-65022) K2: Compiler crashes when array literal is used in delegate expression
- [`KT-62836`](https://youtrack.jetbrains.com/issue/KT-62836) K2: Expected FirResolvedTypeRef with ConeKotlinType but was FirImplicitTypeRefImplWithoutSource
- [`KT-64727`](https://youtrack.jetbrains.com/issue/KT-64727) K1: Closing bracket of object inside crossinline lambda or inside lambda in inline function is not hit on step-over
- [`KT-64726`](https://youtrack.jetbrains.com/issue/KT-64726) K1: Cannot stop on closing bracket of crossinline lambda inside of another crossinline lambda
- [`KT-64725`](https://youtrack.jetbrains.com/issue/KT-64725) K1: Cannot stop on closing bracket of lambda of inline-only function
- [`KT-66272`](https://youtrack.jetbrains.com/issue/KT-66272) Could not load module <Error module> with a combination of type parameters
- [`KT-66243`](https://youtrack.jetbrains.com/issue/KT-66243) Could not load module <Error module> in a builder inference with lambda with typed parameter
- [`KT-66229`](https://youtrack.jetbrains.com/issue/KT-66229) Could not load module <Error module> in a builder inference with Map.Entry
- [`KT-66313`](https://youtrack.jetbrains.com/issue/KT-66313) K2: declaration-order-dependent false-positive "recursive problem in type checker" error on `getX` declaration with implicit return type that calls `x` declaration via intermediate declaration in `getX`'s expression body
- [`KT-61041`](https://youtrack.jetbrains.com/issue/KT-61041) K2: Consider getting rid of confusing shouldRunCompletion and shouldAvoidFullCompletion function in FirInferenceSession
- [`KT-66267`](https://youtrack.jetbrains.com/issue/KT-66267) K2: generic function's type parameter is erased if present as type argument in type of callable reference to member of generic function's local class
- [`KT-61448`](https://youtrack.jetbrains.com/issue/KT-61448) K2: Disappeared DEPRECATION in testWithModifiedMockJdk
- [`KT-60106`](https://youtrack.jetbrains.com/issue/KT-60106) K2: Introduced REIFIED_TYPE_FORBIDDEN_SUBSTITUTION
- [`KT-58279`](https://youtrack.jetbrains.com/issue/KT-58279) K2. False-negative `Smart cast to is impossible, because is a public API property declared in different module` for Java static field
- [`KT-61626`](https://youtrack.jetbrains.com/issue/KT-61626) K2: Module "com.soywiz.korlibs.kmem:kmem" has a reference to symbol korlibs.memory/Buffer|null[1]
- [`KT-57427`](https://youtrack.jetbrains.com/issue/KT-57427) Fix inconsistencies in name manglers that use different declaration representations
- [`KT-66258`](https://youtrack.jetbrains.com/issue/KT-66258) K2: accessor-targeted `@Suppress` annotation is ignored on primary constructor property
- [`KT-29559`](https://youtrack.jetbrains.com/issue/KT-29559) Smart Cast functionality doesn't behave in an expected way in all cases
- [`KT-60777`](https://youtrack.jetbrains.com/issue/KT-60777) K2: missing INLINE_FROM_HIGHER_PLATFORM
- [`KT-66260`](https://youtrack.jetbrains.com/issue/KT-66260) K2: false-positive "abstract function in non-abstract class" error on abstract member function of open interface
- [`KT-66067`](https://youtrack.jetbrains.com/issue/KT-66067) K2: different overrides are created in a complex hierarchy with raw types and upper-bounded type parameters
- [`KT-65821`](https://youtrack.jetbrains.com/issue/KT-65821) K2: [NONE_APPLICABLE] None of the following functions is applicable: [constructor(message: String?): Throwable, constructor(cause: Throwable?): Throwable, constructor(): Throwable, ...]
- [`KT-66268`](https://youtrack.jetbrains.com/issue/KT-66268) K2: Expected FirResolvedTypeRef with ConeKotlinType but was FirUserTypeRefImpl
- [`KT-63563`](https://youtrack.jetbrains.com/issue/KT-63563) K2: False negative RETURN_TYPE_MISMATCH with empty return
- [`KT-60797`](https://youtrack.jetbrains.com/issue/KT-60797) K2: implement JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE
- [`KT-28159`](https://youtrack.jetbrains.com/issue/KT-28159) Smartcasts don't work with Nothing? values (Nothing? considered a null constant => an unstable value)
- [`KT-28262`](https://youtrack.jetbrains.com/issue/KT-28262) Smartcasts for reference equality don't work if explicit true check is used
- [`KT-66000`](https://youtrack.jetbrains.com/issue/KT-66000) K2: inherited inline getter has not been inlined
- [`KT-66158`](https://youtrack.jetbrains.com/issue/KT-66158) K2: not nullable return type for upper-bounded kotlin type parameter in KJK hierarchy
- [`KT-57268`](https://youtrack.jetbrains.com/issue/KT-57268) K2: extra methods `remove` and/or `getOrDefault` are generated for Map subclasses with JDK 1.6 in dependencies
- [`KT-63577`](https://youtrack.jetbrains.com/issue/KT-63577) K2: false-positive "wrong number of type arguments" error on callable reference to member of generic function's local class
- [`KT-62352`](https://youtrack.jetbrains.com/issue/KT-62352) jspecify NonNull annotation seems not supported
- [`KT-65636`](https://youtrack.jetbrains.com/issue/KT-65636) PowerAssert: Negative contains operator not aligned correctly in K2
- [`KT-64271`](https://youtrack.jetbrains.com/issue/KT-64271) K2: Wrong overriddenSymbols for toString of data class
- [`KT-62779`](https://youtrack.jetbrains.com/issue/KT-62779) K2: Difference in fake override generation
- [`KT-61941`](https://youtrack.jetbrains.com/issue/KT-61941) K2: FIR2IR incorrectly generates f/o structure for complex java/kotlin hierarchies with remapped jvm declarations
- [`KT-60283`](https://youtrack.jetbrains.com/issue/KT-60283) K2: fake override for java static method is not generated
- [`KT-65095`](https://youtrack.jetbrains.com/issue/KT-65095) K2: no bridge generated for getOrDefault when inheriting from Java Map implementation
- [`KT-57301`](https://youtrack.jetbrains.com/issue/KT-57301) K2: `getOrDefault` and bridges are not generated for certain Map subclasses
- [`KT-50916`](https://youtrack.jetbrains.com/issue/KT-50916) K2: store resolved type inside ConeStubType after builder inference
- [`KT-65857`](https://youtrack.jetbrains.com/issue/KT-65857) K2: java.lang.IllegalArgumentException: Unknown visibility: unknown
- [`KT-66174`](https://youtrack.jetbrains.com/issue/KT-66174) -Xjdk-release 6 and 7 have a misleading error message
- [`KT-66175`](https://youtrack.jetbrains.com/issue/KT-66175) Wrong supported options list for -jvm-target compiler option
- [`KT-58814`](https://youtrack.jetbrains.com/issue/KT-58814) Too eager subtype inference in when expression
- [`KT-65408`](https://youtrack.jetbrains.com/issue/KT-65408) K1: "There are still 2 unbound symbols after generation of IR module" caused by data object's `copy` function usage
- [`KT-65844`](https://youtrack.jetbrains.com/issue/KT-65844) False Positive "This class can only be used as an annotation or as an argument to `@OptIn`" when passing as an array
- [`KT-58697`](https://youtrack.jetbrains.com/issue/KT-58697) K2: Tests: Assert no dump files exist when dump directive isn't present
- [`KT-63258`](https://youtrack.jetbrains.com/issue/KT-63258) NPE with function reference from within lambda during init
- [`KT-60597`](https://youtrack.jetbrains.com/issue/KT-60597) K1: IllegalArgumentException: fromIndex(0) > toIndex(-1) when wrapping receiver with backticks
- [`KT-33108`](https://youtrack.jetbrains.com/issue/KT-33108) USELESS_CAST false positive for cast inside lambda
- [`KT-58458`](https://youtrack.jetbrains.com/issue/KT-58458) K1: "java.lang.NullPointerException" with 'var equals' or 'val equals' as argument in when
- [`KT-58447`](https://youtrack.jetbrains.com/issue/KT-58447) K1: "AssertionError: Recursion detected on input" with `@ParameterName` and extension
- [`KT-41013`](https://youtrack.jetbrains.com/issue/KT-41013) OVERLOAD_RESOLUTION_AMBIGUITY for functions  takes lambda: can not resolve it, but only named lambda parameter
- [`KT-56032`](https://youtrack.jetbrains.com/issue/KT-56032) [LC issue] Incorrect wrapping when passing java vararg method to inline function
- [`KT-65588`](https://youtrack.jetbrains.com/issue/KT-65588) K2: typealias of primitive type in vararg causes ABI incompatibility
- [`KT-23873`](https://youtrack.jetbrains.com/issue/KT-23873) Indexed access operator can cause false USELESS_CAST warning
- [`KT-31191`](https://youtrack.jetbrains.com/issue/KT-31191) Contract not smartcasting for extension functions in if-statement with multiple conditions
- [`KT-28725`](https://youtrack.jetbrains.com/issue/KT-28725) ReenteringLazyValueComputationException during resolution & inference
- [`KT-35429`](https://youtrack.jetbrains.com/issue/KT-35429) ReenteringLazyValueComputationException when accessing property with same name
- [`KT-63826`](https://youtrack.jetbrains.com/issue/KT-63826) K2: expect for expect crashes the compiler
- [`KT-25668`](https://youtrack.jetbrains.com/issue/KT-25668) False-positive error on restricted suspending function call with callable reference
- [`KT-18055`](https://youtrack.jetbrains.com/issue/KT-18055) SMARTCAST_IMPOSSIBLE on mutable data class variable with a read-only property
- [`KT-15904`](https://youtrack.jetbrains.com/issue/KT-15904) Improve error message when type of generic extension call is inferred from receiver
- [`KT-66186`](https://youtrack.jetbrains.com/issue/KT-66186) K1 diagnostics miss some reporting messages
- [`KT-65101`](https://youtrack.jetbrains.com/issue/KT-65101) Generics behaving different when parenthesized
- [`KT-63444`](https://youtrack.jetbrains.com/issue/KT-63444) TYPE_MISMATCH caused by Inner class with nullable type and star projection
- [`KT-62022`](https://youtrack.jetbrains.com/issue/KT-62022) K1 False positive EXPOSED_FUNCTION_RETURN_TYPE on generics with anonymous object types
- [`KT-58751`](https://youtrack.jetbrains.com/issue/KT-58751) Definitely non-nullable type gets lost with star projection
- [`KT-56624`](https://youtrack.jetbrains.com/issue/KT-56624) "Unresolved reference" with import alias and enum constructor call
- [`KT-54726`](https://youtrack.jetbrains.com/issue/KT-54726) K1: StackOverflowError on mutually recursive typealiases
- [`KT-35134`](https://youtrack.jetbrains.com/issue/KT-35134) False negative INCOMPATIBLE_TYPES, EQUALITY_NOT_APPLICABLE when comparing smartcast value to Boolean
- [`KT-20617`](https://youtrack.jetbrains.com/issue/KT-20617) Qualified this`@property` does not work in extension properties with body expression
- [`KT-10879`](https://youtrack.jetbrains.com/issue/KT-10879) OVERLOAD_RESOLUTION_AMBIGUITY for synthetic property accessor with smartcasted receiver
- [`KT-26768`](https://youtrack.jetbrains.com/issue/KT-26768) K1 IDE: False positive "Smart cast to '$CLASS$' is impossible", on local variable in run closure
- [`KT-63525`](https://youtrack.jetbrains.com/issue/KT-63525) K2: "IllegalStateException: Fake override should have at least one overridden descriptor" caused by unreachable code
- [`KT-65333`](https://youtrack.jetbrains.com/issue/KT-65333) K2: UNRESOLVED_REFERENCE for java inner class in intersection scope
- [`KT-61060`](https://youtrack.jetbrains.com/issue/KT-61060) K2: Rewrite delegate inference
- [`KT-63712`](https://youtrack.jetbrains.com/issue/KT-63712) Make it possible to add new stdlib API with SinceKotlin(2.0)
- [`KT-63741`](https://youtrack.jetbrains.com/issue/KT-63741) K2: fix visibility inference with overridden + inherited member
- [`KT-64488`](https://youtrack.jetbrains.com/issue/KT-64488) K2: False positive DEPRECATED_ACCESS_TO_ENTRY_PROPERTY_FROM_ENUM with context receivers
- [`KT-62283`](https://youtrack.jetbrains.com/issue/KT-62283) K2: build Dokka with K2 user project and pass it to CI
- [`KT-57585`](https://youtrack.jetbrains.com/issue/KT-57585) K2/MPP: false-negative errors on expect/actual modifiers mismatch
- [`KT-66077`](https://youtrack.jetbrains.com/issue/KT-66077) IrFakeOverrideBuilder: NPE from IrJavaIncompatibilityRulesOverridabilityCondition.doesJavaOverrideHaveIncompatibleValueParameterKinds
- [`KT-57044`](https://youtrack.jetbrains.com/issue/KT-57044) K2 LL Tests: false-positive 'Overload resolution ambiguity between candidates: [`@Override`() fun test(): Unit , fun test(): Unit]'
- [`KT-66020`](https://youtrack.jetbrains.com/issue/KT-66020) K2: ISE "IrPropertySymbolImpl is unbound. Signature: null" on a property with getter with `@JvmName`
- [`KT-62135`](https://youtrack.jetbrains.com/issue/KT-62135) K2, KLIB: Classes are still sorted before serializing them to metadata
- [`KT-65866`](https://youtrack.jetbrains.com/issue/KT-65866) [K/N] Fix java.lang.IllegalArgumentException: Unknown visibility: unknown
- [`KT-66005`](https://youtrack.jetbrains.com/issue/KT-66005) K2: "Should not be here: class org.jetbrains.kotlin.fir.expressions.impl.FirResolvedReifiedParameterReferenceImpl" on incorrect comparison of reified type parameter
- [`KT-65840`](https://youtrack.jetbrains.com/issue/KT-65840) [K2] Initializer type mismatch: expected 'Type', actual 'Type'
- [`KT-65002`](https://youtrack.jetbrains.com/issue/KT-65002) K2: Incorrect suspend conversion if argument is an aliased functional type
- [`KT-65984`](https://youtrack.jetbrains.com/issue/KT-65984) K2 scripting: failure on processing SUPPRESS annotation in the last script statement
- [`KT-65680`](https://youtrack.jetbrains.com/issue/KT-65680) K2: Class redeclaration leads to BackendException during IR fake override builder
- [`KT-66028`](https://youtrack.jetbrains.com/issue/KT-66028) K2: Convert FirExpectActualDeclarationChecker to platform checker
- [`KT-65592`](https://youtrack.jetbrains.com/issue/KT-65592) K2: IrFakeOverrideBuilder: ISE "should not be called" on diamond hierarchy with explicit dependency on annotations.jar
- [`KT-65277`](https://youtrack.jetbrains.com/issue/KT-65277) IrFakeOverrideBuilder: NPE from IrJavaIncompatibilityRulesOverridabilityCondition.doesJavaOverrideHaveIncompatibleValueParameterKinds
- [`KT-65983`](https://youtrack.jetbrains.com/issue/KT-65983) K2 gradle scripting: "'val' cannot be reassigned" errors
- [`KT-60452`](https://youtrack.jetbrains.com/issue/KT-60452) K2 Scripting: implement overriding of the script params
- [`KT-65975`](https://youtrack.jetbrains.com/issue/KT-65975) K2: Implicit receivers resolution order in K2 scripting
- [`KT-60249`](https://youtrack.jetbrains.com/issue/KT-60249) K2: No unit coercion generated for loops body
- [`KT-65937`](https://youtrack.jetbrains.com/issue/KT-65937) K2: order of enum entries changed
- [`KT-65933`](https://youtrack.jetbrains.com/issue/KT-65933) K2: Type missmatch in arrays in annotations
- [`KT-65343`](https://youtrack.jetbrains.com/issue/KT-65343) JVM IR: Source parameter is lost when copying with DeepCopyIrTreeWithSymbols
- [`KT-65103`](https://youtrack.jetbrains.com/issue/KT-65103) K2: IllegalArgumentException: IrErrorCallExpressionImpl(5388, 5392, "Unresolved reference: R?C|<local>/cont|") found but error code is not allowed
- [`KT-62788`](https://youtrack.jetbrains.com/issue/KT-62788) K2: difference in annotation inheritance in overriddings
- [`KT-65669`](https://youtrack.jetbrains.com/issue/KT-65669) K2: ClassCastException class FirDeclarationStatusImpl cannot be cast to class FirResolvedDeclarationStatus
- [`KT-65493`](https://youtrack.jetbrains.com/issue/KT-65493) IrFakeOverrideBuilder: difference in return type for intersection with raw type
- [`KT-65207`](https://youtrack.jetbrains.com/issue/KT-65207) IrFakeOverrideBuilder - nullable return type for intersection override
- [`KT-65972`](https://youtrack.jetbrains.com/issue/KT-65972) Fix problems related to Unknown visibility in [FP] intellij
- [`KT-65246`](https://youtrack.jetbrains.com/issue/KT-65246) K2: Overiding java method that takes vararg parameter causes WRONG_NULLABILITY_FOR_JAVA_OVERRIDE warning
- [`KT-59883`](https://youtrack.jetbrains.com/issue/KT-59883) K2: Disappeared INVALID_IF_AS_EXPRESSION
- [`KT-57300`](https://youtrack.jetbrains.com/issue/KT-57300) K2: subclass of MutableCollection with primitive element type has methods with boxed type
- [`KT-58476`](https://youtrack.jetbrains.com/issue/KT-58476) Context receivers: "No mapping for symbol: VALUE_PARAMETER" with context-receiver inside suspended lambda calling another suspended function
- [`KT-52213`](https://youtrack.jetbrains.com/issue/KT-52213) Context receivers: "No mapping for symbol: VALUE_PARAMETER"  caused by contextual suspending function type with receiver
- [`KT-13650`](https://youtrack.jetbrains.com/issue/KT-13650) Right-hand side of a safe assignment is not always evaluated, which can fool smart-casts
- [`KT-61823`](https://youtrack.jetbrains.com/issue/KT-61823) K2: Render list of declarations in diagnostic messages with linebreak as separator
- [`KT-65302`](https://youtrack.jetbrains.com/issue/KT-65302) IrFakeOverrideBuilder - missing `@EnhancedNullability`
- [`KT-65241`](https://youtrack.jetbrains.com/issue/KT-65241) K2: [LT] Compiler crash on assignment expression with incorrect lvalue
- [`KT-60006`](https://youtrack.jetbrains.com/issue/KT-60006) K2: Disappeared EXPRESSION_EXPECTED
- [`KT-65817`](https://youtrack.jetbrains.com/issue/KT-65817) K2: Check if callable reference vararg adaption can be affected by primitive type aliases
- [`KT-62847`](https://youtrack.jetbrains.com/issue/KT-62847) K2: Introduce FIR node for SAM conversion
- [`KT-65920`](https://youtrack.jetbrains.com/issue/KT-65920) K2: no field for delegation is created
- [`KT-65487`](https://youtrack.jetbrains.com/issue/KT-65487) K2: Different fake overrides and false positive NOTHING_TO_OVERRIDE for intersection/override with Collection.remove
- [`KT-65460`](https://youtrack.jetbrains.com/issue/KT-65460) Don't compare order of functions in IR dump
- [`KT-64276`](https://youtrack.jetbrains.com/issue/KT-64276) [K/N][K2] K2 behaviorial difference with inconsistent inheritance of ObjCName
- [`KT-65572`](https://youtrack.jetbrains.com/issue/KT-65572) [K/N][K2] INCOMPATIBLE_OBJC_NAME_OVERRIDE error message changed from K1
- [`KT-63420`](https://youtrack.jetbrains.com/issue/KT-63420) Prevent weakening visibility in implicit overrides
- [`KT-64635`](https://youtrack.jetbrains.com/issue/KT-64635) K2: "KotlinIllegalArgumentExceptionWithAttachments: Expected expression 'FirAnonymousFunctionExpressionImpl' to be resolved" when provideDelegate is extension of function with receiver
- [`KT-63879`](https://youtrack.jetbrains.com/issue/KT-63879) K2: Redundant flag `declaresDefaultValue` for parameter of function inherited from delegate
- [`KT-56744`](https://youtrack.jetbrains.com/issue/KT-56744) Prepare language committee ticket about DFA/Smart-cast related changes in K2
- [`KT-65790`](https://youtrack.jetbrains.com/issue/KT-65790) K2: Move check for _private-to-this_ visibility into checker
- [`KT-65551`](https://youtrack.jetbrains.com/issue/KT-65551) K2: Property redeclaration on native compilation leads to NotImplementedError
- [`KT-65770`](https://youtrack.jetbrains.com/issue/KT-65770) K2: Diagnostic rendering of `vararg Foo` parameter produces `vararg Array<Foo>`
- [`KT-65555`](https://youtrack.jetbrains.com/issue/KT-65555) K2:   must override 'spliterator' because it inherits multiple implementations for it
- [`KT-59921`](https://youtrack.jetbrains.com/issue/KT-59921) K2: Disappeared NULL_FOR_NONNULL_TYPE
- [`KT-65290`](https://youtrack.jetbrains.com/issue/KT-65290) K2:  No override for FUN DEFAULT_PROPERTY_ACCESSOR
- [`KT-19446`](https://youtrack.jetbrains.com/issue/KT-19446) False positive "Smart cast to 'Foo' is impossible" due to same variable names in different closures
- [`KT-65337`](https://youtrack.jetbrains.com/issue/KT-65337) K2: False positive UNRESOLVED_REFERENCE when lambda labeled by illegal label and operator-invoked
- [`KT-65448`](https://youtrack.jetbrains.com/issue/KT-65448) K2: fake overrides are not generated for 'containsAll', 'removeAll', 'retainAll' if inherited from raw type
- [`KT-65298`](https://youtrack.jetbrains.com/issue/KT-65298) K2: not nullable return type and parameter for raw types
- [`KT-63377`](https://youtrack.jetbrains.com/issue/KT-63377) K2: conflict between type parameter and nested class
- [`KT-63286`](https://youtrack.jetbrains.com/issue/KT-63286) K2: Top-level properties in scripts are missing initialization checks
- [`KT-59744`](https://youtrack.jetbrains.com/issue/KT-59744) K2:  false negative VAL_REASSIGNMENT  in case of reassignment inside custom setter
- [`KT-58579`](https://youtrack.jetbrains.com/issue/KT-58579) K2: false-positive new inference error on invoking a generic function on Java wildcard type bounded by raw-typed Java inner class
- [`KT-60258`](https://youtrack.jetbrains.com/issue/KT-60258) Support java-kotlin interop for `@SubclassOptInRequired`
- [`KT-60262`](https://youtrack.jetbrains.com/issue/KT-60262) Support for inter-module interaction for `@SubclassOptInRequired`
- [`KT-62878`](https://youtrack.jetbrains.com/issue/KT-62878) K2: missing implicit coercion to unit
- [`KT-59715`](https://youtrack.jetbrains.com/issue/KT-59715) K2: Check behaviour of property + operator in operator position
- [`KT-63441`](https://youtrack.jetbrains.com/issue/KT-63441) IrFakeOverrideBuilder: "accidental override" when implementing a Java function taking an array parameter
- [`KT-65706`](https://youtrack.jetbrains.com/issue/KT-65706) K2: IrFakeOverrideBuilder: ISE "Fake override should have at least one overridden descriptor" on J-K-J-K hierarchy with interface delegation
- [`KT-61362`](https://youtrack.jetbrains.com/issue/KT-61362) K2: Properties/fields are missing from system libraries
- [`KT-63344`](https://youtrack.jetbrains.com/issue/KT-63344) K2: False positive ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED
- [`KT-58845`](https://youtrack.jetbrains.com/issue/KT-58845) K2: SAM checker can run incorrectly in presence of an expect supertype
- [`KT-61843`](https://youtrack.jetbrains.com/issue/KT-61843) K2: Missing TYPE_MISMATCH for nested array literals
- [`KT-62752`](https://youtrack.jetbrains.com/issue/KT-62752) expect-actual matcher/checker: return type must be "checking" incompatibility
- [`KT-59887`](https://youtrack.jetbrains.com/issue/KT-59887) K2: Disappeared ACTUAL_MISSING
- [`KT-65604`](https://youtrack.jetbrains.com/issue/KT-65604) K2: INAPPLICABLE_JVM_NAME: effective modality
- [`KT-65637`](https://youtrack.jetbrains.com/issue/KT-65637) Prepare documentation for PCLA implementation
- [`KT-65341`](https://youtrack.jetbrains.com/issue/KT-65341) K2: "Cannot find cached type parameter by FIR symbol" caused by not-null assertion operator inside string in throw
- [`KT-49283`](https://youtrack.jetbrains.com/issue/KT-49283) Support contribution type info from a nested builder inference call
- [`KT-64077`](https://youtrack.jetbrains.com/issue/KT-64077) K2: Builder inference ignores constraints from nested builder inference
- [`KT-49160`](https://youtrack.jetbrains.com/issue/KT-49160) Couldn't infer a type argument through several builder inference calls broken by a local class
- [`KT-63827`](https://youtrack.jetbrains.com/issue/KT-63827) K2: Array += desugaring doesn't have origin
- [`KT-65057`](https://youtrack.jetbrains.com/issue/KT-65057) K2: Wrong type inferred in code with heavy use of generics
- [`KT-63514`](https://youtrack.jetbrains.com/issue/KT-63514) ISE “Inline class types should have the same representation: [I != I” during compilation on submitting UIntArray to vararg
- [`KT-61088`](https://youtrack.jetbrains.com/issue/KT-61088) K2: return types of non-last-expression calls to `@PolymorphicSignature` methods inside try-expressions don't resolve to void when required
- [`KT-62476`](https://youtrack.jetbrains.com/issue/KT-62476) K2: Enable building fake overrides by ir on non-JVM targets
- [`KT-59839`](https://youtrack.jetbrains.com/issue/KT-59839) Prohibit `header` and `impl` in MPP
- [`KT-61310`](https://youtrack.jetbrains.com/issue/KT-61310) K2: "Not enough information to infer type variable R" for transformLatest
- [`KT-63733`](https://youtrack.jetbrains.com/issue/KT-63733) Builder-style type inference can't resolve to extension overloads when they're more applicable than member ones
- [`KT-57707`](https://youtrack.jetbrains.com/issue/KT-57707) K1: inconsistent TYPE_MISMATCH in builder inference
- [`KT-55057`](https://youtrack.jetbrains.com/issue/KT-55057) Builder inference changes behaviour sporadically based on BI annotation on unrelated call
- [`KT-60663`](https://youtrack.jetbrains.com/issue/KT-60663) Builder inference does not work inside a nested unrelated builder inference lambda
- [`KT-53639`](https://youtrack.jetbrains.com/issue/KT-53639) TYPE_MISMATCH: compiler can't infer the list's type when using `buildList {}` builder or `Collection#isNotEmpty`
- [`KT-60291`](https://youtrack.jetbrains.com/issue/KT-60291) K2: "IllegalStateException: Cannot serialize error type: ERROR CLASS: Cannot infer argument for type parameter T" during FIR serialization
- [`KT-65033`](https://youtrack.jetbrains.com/issue/KT-65033) K2: Fir2LazyIr: Lazy type aliases not supported
- [`KT-57709`](https://youtrack.jetbrains.com/issue/KT-57709) Inconsistent extension function call resolution in builder inference
- [`KT-53740`](https://youtrack.jetbrains.com/issue/KT-53740) Builder inference with multiple lambdas leads to unsound type
- [`KT-60877`](https://youtrack.jetbrains.com/issue/KT-60877) Builder inference from the null literal results in Nothing instead of Nothing? for producing positions of the postponed type variable
- [`KT-53553`](https://youtrack.jetbrains.com/issue/KT-53553) Builder inference: inconsistent types in different lambda scopes
- [`KT-54400`](https://youtrack.jetbrains.com/issue/KT-54400) K2: builder inference does not work with assignments of literals to member properties
- [`KT-63840`](https://youtrack.jetbrains.com/issue/KT-63840) Builder inference fails on calls to identity-shaped functions with postponed type variables inside select-constructions
- [`KT-65262`](https://youtrack.jetbrains.com/issue/KT-65262) K2: Exception in DFA for combination of try-finally + PCLA + DI
- [`KT-58169`](https://youtrack.jetbrains.com/issue/KT-58169) K2: make equals bounded smart casts work the same as in K1
- [`KT-64967`](https://youtrack.jetbrains.com/issue/KT-64967) K2: false positive TYPE_MISMATCH with generic type parameters
- [`KT-64102`](https://youtrack.jetbrains.com/issue/KT-64102) K2: Missing (disappeared in this case) DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER_ERROR
- [`KT-63988`](https://youtrack.jetbrains.com/issue/KT-63988) K2: Reflection cannot find type of local class of local class
- [`KT-63901`](https://youtrack.jetbrains.com/issue/KT-63901) K2: Different naming of inner class in metadata
- [`KT-63655`](https://youtrack.jetbrains.com/issue/KT-63655) K2: incorrect short class name in metadata for anonymous object inside a local class
- [`KT-59664`](https://youtrack.jetbrains.com/issue/KT-59664) Inline modifier can be added to a constructor parameter, but it does not have any effect
- [`KT-59418`](https://youtrack.jetbrains.com/issue/KT-59418) K2: Missing DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE
- [`KT-63612`](https://youtrack.jetbrains.com/issue/KT-63612) K2: Class is not abstract and does not implement abstract member
- [`KT-63737`](https://youtrack.jetbrains.com/issue/KT-63737) Wasm: revise external declaration FE checker for WASI mode
- [`KT-59782`](https://youtrack.jetbrains.com/issue/KT-59782) K2: Forbid local delegated properties with private accessors in public inline functions
- [`KT-65482`](https://youtrack.jetbrains.com/issue/KT-65482) K2: NoSuchFieldError due to using unboxed type
- [`KT-61182`](https://youtrack.jetbrains.com/issue/KT-61182) Unit conversion is accidentally allowed to be used for expressions on variables + invoke resolution
- [`KT-62998`](https://youtrack.jetbrains.com/issue/KT-62998) Forbid assignment of a nullable to a not-null Java field as a selector of unsafe assignment
- [`KT-63208`](https://youtrack.jetbrains.com/issue/KT-63208) K2: Implement deprecation cycle and fix missing errors for error-level nullable arguments of warning-level Java types
- [`KT-57600`](https://youtrack.jetbrains.com/issue/KT-57600) Forbid overriding of Java method with raw-typed parameter with generic typed parameter
- [`KT-63147`](https://youtrack.jetbrains.com/issue/KT-63147) K2: False negative DSL_SCOPE_VIOLATION when member is annotated with `@LowPriorityInOverloadResolution`
- [`KT-62134`](https://youtrack.jetbrains.com/issue/KT-62134) K2: handle non-simple types during FirStatusResolver.isPrivateToThis check
- [`KT-42020`](https://youtrack.jetbrains.com/issue/KT-42020) Psi2ir: IllegalStateException: "IrSimpleFunctionPublicSymbolImpl for public [...] is already bound" on generic function whose substitution leads to IdSignature clash
- [`KT-59012`](https://youtrack.jetbrains.com/issue/KT-59012) K2: Support inferring types based on self upper bounds
- [`KT-65373`](https://youtrack.jetbrains.com/issue/KT-65373) K2: there is a crash in KJK hierarchy with an extension member property
- [`KT-65456`](https://youtrack.jetbrains.com/issue/KT-65456) K1: ISE "Property has no getter" with -Xsam-conversions=class when Java SAM interface contains a field
- [`KT-62884`](https://youtrack.jetbrains.com/issue/KT-62884) K2: different signature of delegate object for generic extension property
- [`KT-60581`](https://youtrack.jetbrains.com/issue/KT-60581) K2 fails with New inference error for assertThat under strange circumstances
- [`KT-59630`](https://youtrack.jetbrains.com/issue/KT-59630) K2: Implement running FIR Blackbox tests on different JDKs
- [`KT-64944`](https://youtrack.jetbrains.com/issue/KT-64944) Can't assign null after early return smart cast with typed destructive assignment
- [`KT-64910`](https://youtrack.jetbrains.com/issue/KT-64910) K2: AA FIR: KtCall's argument mapping misses SAM conversion argument
- [`KT-65165`](https://youtrack.jetbrains.com/issue/KT-65165) K2: "ClassCastException: class java.lang.String cannot be cast to class SampleClass"
- [`KT-64982`](https://youtrack.jetbrains.com/issue/KT-64982) K2: false negative FUNCTION_CALL_EXPECTED
- [`KT-65318`](https://youtrack.jetbrains.com/issue/KT-65318) K2: Substitution stackoverflow on jspecify `@NullMarked` superclass
- [`KT-65010`](https://youtrack.jetbrains.com/issue/KT-65010) Kotlin/Native: code generation for a static field is failing
- [`KT-57299`](https://youtrack.jetbrains.com/issue/KT-57299) K2: VerifyError due to overriding final method `size` on a subclass of Collection and Set
- [`KT-64706`](https://youtrack.jetbrains.com/issue/KT-64706) K2: Type inference cannot resolve nullable `@Composable` lambda
- [`KT-65058`](https://youtrack.jetbrains.com/issue/KT-65058) K2: Protected function call from public-API inline function is prohibited in anonymous object
- [`KT-65316`](https://youtrack.jetbrains.com/issue/KT-65316) K2: False positive USAGE_IS_NOT_INLINABLE for expression labeled with illegal label
- [`KT-60958`](https://youtrack.jetbrains.com/issue/KT-60958) K2: smart cast does not work with definite return from if block
- [`KT-63151`](https://youtrack.jetbrains.com/issue/KT-63151) K2: Assignment within function lambda should invalidate contract DFA implications
- [`KT-63351`](https://youtrack.jetbrains.com/issue/KT-63351) K2. No smart cast with not-null assertion operator after a safe call
- [`KT-65324`](https://youtrack.jetbrains.com/issue/KT-65324) atomicfu-plugin: top-level delegated properties cause NPE
- [`KT-60246`](https://youtrack.jetbrains.com/issue/KT-60246) K2: origin is not set for getting array element operator
- [`KT-64387`](https://youtrack.jetbrains.com/issue/KT-64387) K2: Missing POSTFIX_INC/DEC origin for array element inc/dec
- [`KT-61891`](https://youtrack.jetbrains.com/issue/KT-61891) K2: POSTFIX_{INCR|DECR} of global misses an origin
- [`KT-65019`](https://youtrack.jetbrains.com/issue/KT-65019) K2: unexpected exception when executing inc/dec in finally block on WASM
- [`KT-64392`](https://youtrack.jetbrains.com/issue/KT-64392) Factor out KLIB serialization logic from the `backend.native` module
- [`KT-65270`](https://youtrack.jetbrains.com/issue/KT-65270) K2: Missing ACTUAL_WITHOUT_EXPECT when expect is fake-override
- [`KT-60367`](https://youtrack.jetbrains.com/issue/KT-60367) K2: Support EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE diagnostics
- [`KT-62704`](https://youtrack.jetbrains.com/issue/KT-62704) Absent testrunner FirLightTreeDiagnosticTestSpecGenerated
- [`KT-65044`](https://youtrack.jetbrains.com/issue/KT-65044) K2 compiler crash on unresolved delegated extention receiver
- [`KT-65021`](https://youtrack.jetbrains.com/issue/KT-65021) K2: Missing error and miscompilation in destructuring declaration delegation
- [`KT-63899`](https://youtrack.jetbrains.com/issue/KT-63899) K2: Vararg parameter misses annotation in metadata
- [`KT-60175`](https://youtrack.jetbrains.com/issue/KT-60175) JVM IR inline: accidental reification of typeOf type argument
- [`KT-65336`](https://youtrack.jetbrains.com/issue/KT-65336) K2: Space build fails
- [`KT-59683`](https://youtrack.jetbrains.com/issue/KT-59683) K2: Add control flow graph to FirScript
- [`KT-63434`](https://youtrack.jetbrains.com/issue/KT-63434) K2. False positive `Cannot access` with protected nested classifiers references inside anonymous object inherited from containing class
- [`KT-64222`](https://youtrack.jetbrains.com/issue/KT-64222) K2: "return type is not a subtype of the return type of the overridden member"
- [`KT-64314`](https://youtrack.jetbrains.com/issue/KT-64314) K2: Rename FirConstExpression to FirLiteralExpression
- [`KT-64975`](https://youtrack.jetbrains.com/issue/KT-64975) FIR: Deserialize enum entry annotation arguments from binary libraries with lookup tags instead of symbols
- [`KT-63646`](https://youtrack.jetbrains.com/issue/KT-63646) K2: "IllegalStateException: Return type of provideDelegate is expected to be one of the type variables of a candidate, but D was found"
- [`KT-65024`](https://youtrack.jetbrains.com/issue/KT-65024) K2: kotlin.NotImplementedError: An operation is not implemented in the K2 QGs
- [`KT-63994`](https://youtrack.jetbrains.com/issue/KT-63994) K2: Investigate K2 failures in IntelliJ-Rust plugin
- [`KT-64268`](https://youtrack.jetbrains.com/issue/KT-64268) K2: Data-flow from nested lambda not passed to outer lambda
- [`KT-59729`](https://youtrack.jetbrains.com/issue/KT-59729) K2: Investigate CFG buildings for inner lambdas in case of double-lambda builder inference
- [`KT-63042`](https://youtrack.jetbrains.com/issue/KT-63042) K2: proper processing of propagated annotations
- [`KT-64841`](https://youtrack.jetbrains.com/issue/KT-64841) K2: argument type mismatch with type parameter with recursive bound
- [`KT-62554`](https://youtrack.jetbrains.com/issue/KT-62554) K2: incorrect "inherits multiple implementations" error when base Java method takes a parameter of primitive wrapper type
- [`KT-65093`](https://youtrack.jetbrains.com/issue/KT-65093) K2: Super constructor call able to access uninitialized object fields
- [`KT-56489`](https://youtrack.jetbrains.com/issue/KT-56489) K2 allows reading uninitialized variable in object declaration
- [`KT-59987`](https://youtrack.jetbrains.com/issue/KT-59987) K2: Disappeared REIFIED_TYPE_FORBIDDEN_SUBSTITUTION
- [`KT-36786`](https://youtrack.jetbrains.com/issue/KT-36786) Smartcast doesn't work in case of property infix call
- [`KT-65027`](https://youtrack.jetbrains.com/issue/KT-65027) K2: java.lang.NoSuchMethodError: void org.jetbrains.kotlin.name.CallableId in the K2 QG
- [`KT-65056`](https://youtrack.jetbrains.com/issue/KT-65056) IrFakeOverrideBuilder: ISE "No override for FUN" on package-private Java method in K-J-K hierarchy
- [`KT-63414`](https://youtrack.jetbrains.com/issue/KT-63414) K2 / Contracts: false positive "Result has wrong invocation kind" when invoking a function returning a value with contract InvocationKind.EXACTLY_ONCE and try/finally
- [`KT-64809`](https://youtrack.jetbrains.com/issue/KT-64809) K2: Remove the LINK_VIA_SIGNATURES flag from FIR2IR configuration
- [`KT-62045`](https://youtrack.jetbrains.com/issue/KT-62045) IrFakeOverrideBuilder: incorrectly merged fake overrides for Java methods accepting wrapper Double and primitive double
- [`KT-57640`](https://youtrack.jetbrains.com/issue/KT-57640) [K2/N] Investigate behaviour for intersection overrides for properties that have incompatible types
- [`KT-59371`](https://youtrack.jetbrains.com/issue/KT-59371) K2: Missing MISSING_DEPENDENCY_CLASS
- [`KT-59682`](https://youtrack.jetbrains.com/issue/KT-59682) K2: Use proper source for vararg arguments
- [`KT-64261`](https://youtrack.jetbrains.com/issue/KT-64261) K2 / WASM: Extension function with star projection throws "RuntimeError: unreachable"
- [`KT-64257`](https://youtrack.jetbrains.com/issue/KT-64257) K2 QG: kotlin.NotImplementedError: Generation of stubs for class org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterPublicSymbolImpl is not supported yet
- [`KT-64844`](https://youtrack.jetbrains.com/issue/KT-64844) [K/N] Filecheck test `redundant_safepoints.kt` fails under linux_x64
- [`KT-64877`](https://youtrack.jetbrains.com/issue/KT-64877) K2: PCLA doesn't allow infer types from value parameter having TV type
- [`KT-63794`](https://youtrack.jetbrains.com/issue/KT-63794) K2: False positive `NONE_APPLICABLE` on `Throws::class`
- [`KT-63781`](https://youtrack.jetbrains.com/issue/KT-63781) K2: Generated blocks appear in the IR
- [`KT-63779`](https://youtrack.jetbrains.com/issue/KT-63779) K2: Regression for locations of 'if' statements
- [`KT-63624`](https://youtrack.jetbrains.com/issue/KT-63624) K2: incompatible declaration because of different visibility
- [`KT-64400`](https://youtrack.jetbrains.com/issue/KT-64400) K2: allow to use simple boolean expressions as constants
- [`KT-65050`](https://youtrack.jetbrains.com/issue/KT-65050) K2: IllegalStateException: Captured type for incorporation shouldn't escape from incorporation: CapturedType(out org/jetbrains/plugins/gitlab/mergerequest/api/dto/GitLabMergeRequestShortRestDTO)
- [`KT-59972`](https://youtrack.jetbrains.com/issue/KT-59972) K2: Disappeared EXPRESSION_EXPECTED_PACKAGE_FOUND
- [`KT-63256`](https://youtrack.jetbrains.com/issue/KT-63256) K2: NOT_IDENTITY operator call is illegal in contract description
- [`KT-61717`](https://youtrack.jetbrains.com/issue/KT-61717) K1: Unsound green code with self upper bounds and captured types
- [`KT-64871`](https://youtrack.jetbrains.com/issue/KT-64871) IrFakeOverrideBuilder: ISE "no override for <get-size>" on HashMap subclass
- [`KT-58739`](https://youtrack.jetbrains.com/issue/KT-58739) K2: Rewrite `CallableId.classId` to be thread-safe
- [`KT-64979`](https://youtrack.jetbrains.com/issue/KT-64979) K2: Missing REDUNDANT_TYPE_PARCELER when using type alias
- [`KT-60019`](https://youtrack.jetbrains.com/issue/KT-60019) K2: Introduced PARCELER_TYPE_INCOMPATIBLE
- [`KT-60682`](https://youtrack.jetbrains.com/issue/KT-60682) K2: Disappeared DEPRECATION
- [`KT-62500`](https://youtrack.jetbrains.com/issue/KT-62500) K2: origin=GET_PROPERTY is wrongly set to GET_FIELD of backing field inside property's own getter
- [`KT-64743`](https://youtrack.jetbrains.com/issue/KT-64743) K2: Non-expanded type serialized in metadata
- [`KT-64405`](https://youtrack.jetbrains.com/issue/KT-64405) K2: Implement CompileJavaAgainstKotlinTestGenerated for K2
- [`KT-57094`](https://youtrack.jetbrains.com/issue/KT-57094) K1: wrong type inferred for an instance of a local class inside a generic property
- [`KT-62069`](https://youtrack.jetbrains.com/issue/KT-62069) K2: ASSIGNMENT_TYPE_MISMATCH is reported in addition to NO_ELSE_IN_WHEN
- [`KT-62776`](https://youtrack.jetbrains.com/issue/KT-62776) FirLazyResolveContractViolationException: "lazyResolveToPhase(STATUS) cannot be called from a transformer with a phase TYPES" on Java annotation usage
- [`KT-47313`](https://youtrack.jetbrains.com/issue/KT-47313) Change (V)::foo reference resolution when V has a companion
- [`KT-64837`](https://youtrack.jetbrains.com/issue/KT-64837) K2: NPE in fir2ir when generic transitive dependency class is missing
- [`KT-60260`](https://youtrack.jetbrains.com/issue/KT-60260) K2: Implicit coercion to unit is not generated in adapted function reference
- [`KT-60858`](https://youtrack.jetbrains.com/issue/KT-60858) Remove redundant `createDeprecatedAnnotation` necessary to workaround kotlinx-serialization compilation with native
- [`KT-64432`](https://youtrack.jetbrains.com/issue/KT-64432) Unbound symbol access in Fir2Ir fake override builder
- [`KT-64466`](https://youtrack.jetbrains.com/issue/KT-64466) K2: Delegated method annotations are not copied in IR
- [`KT-63589`](https://youtrack.jetbrains.com/issue/KT-63589) K1: Unsound type inference for unbound callable reference to star-projected class's generic mutable property
- [`KT-56141`](https://youtrack.jetbrains.com/issue/KT-56141) K2: Consider removing skipping diagnostics for DelegatedPropertyConstraintPosition
- [`KT-60056`](https://youtrack.jetbrains.com/issue/KT-60056) K2: Introduced UNRESOLVED_REFERENCE
- [`KT-61032`](https://youtrack.jetbrains.com/issue/KT-61032) K2: False positive “Unused variable” for function callable reference
- [`KT-64832`](https://youtrack.jetbrains.com/issue/KT-64832) K2: False positive "Unused variable" checker report on suspend functional types, on overloaded functional types and on custom invoke operator types
- [`KT-64771`](https://youtrack.jetbrains.com/issue/KT-64771) Investigate subtle FIR_DUMP difference for reversed order analysis
- [`KT-62584`](https://youtrack.jetbrains.com/issue/KT-62584) K2: different signature in subclass of local class declared in extension value getter
- [`KT-63806`](https://youtrack.jetbrains.com/issue/KT-63806) Native / KJS / Wasm: "NullPointerException: accept(...) must not be null"
- [`KT-59938`](https://youtrack.jetbrains.com/issue/KT-59938) K2: Disappeared AMBIGUOUS_ACTUALS
- [`KT-43713`](https://youtrack.jetbrains.com/issue/KT-43713) callsInPlace InvocationKind.EXACTLY_ONCE causes CAPTURED_VAL_INITIALIZATION in constructor
- [`KT-64645`](https://youtrack.jetbrains.com/issue/KT-64645) K2: Missing smartcast caused by typealias that expands to nullable type in upper bound
- [`KT-64501`](https://youtrack.jetbrains.com/issue/KT-64501) K2: False-positive WRONG_INVOCATION_KIND when using default arguments
- [`KT-63962`](https://youtrack.jetbrains.com/issue/KT-63962) K2: "java.lang.IllegalStateException: !"
- [`KT-63644`](https://youtrack.jetbrains.com/issue/KT-63644) K2: Create special IR symbols for fake-overrides in fir2ir in mode with IR f/o generator
- [`KT-63638`](https://youtrack.jetbrains.com/issue/KT-63638) K2: Compiler crashes with "Inline class types should have the same representation"
- [`KT-36220`](https://youtrack.jetbrains.com/issue/KT-36220) NI: false positive NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE if one use cannot resolve
- [`KT-64121`](https://youtrack.jetbrains.com/issue/KT-64121) K2: Actual modifier is missed on `override fun toString()` fro value class in native
- [`KT-63703`](https://youtrack.jetbrains.com/issue/KT-63703) K2: Eliminate call to Candidate.usesSAM and samResolver.getFunctionTypeForPossibleSamType in AbstractConeCallConflictResolver.toTypeWithConversion
- [`KT-61443`](https://youtrack.jetbrains.com/issue/KT-61443) K2: Return typeId -1 during JS compilation
- [`KT-64090`](https://youtrack.jetbrains.com/issue/KT-64090) K2: false-positive new inference error on invoking from another module a generic function on Java list type with wildcard type argument bounded by raw-typed Java inner class
- [`KT-64044`](https://youtrack.jetbrains.com/issue/KT-64044) K2: Java mapped method should have a source from Java method, not from mapped Kotlin source class
- [`KT-39137`](https://youtrack.jetbrains.com/issue/KT-39137) Smartcast to wrong nullability with generic type parameter upper bound
- [`KT-46674`](https://youtrack.jetbrains.com/issue/KT-46674) ClassCastException with smartcast if `plus` operator returns a different type
- [`KT-64625`](https://youtrack.jetbrains.com/issue/KT-64625) [FIR] Infinite recursion in `TypeUnificationKt.doUnify()` building subset of native stdlib
- [`KT-59369`](https://youtrack.jetbrains.com/issue/KT-59369) K2: Missing BUILDER_INFERENCE_STUB_RECEIVER
- [`KT-62590`](https://youtrack.jetbrains.com/issue/KT-62590) Split expect/actual matcher-checker machinery in two separate components: matcher and checker
- [`KT-63732`](https://youtrack.jetbrains.com/issue/KT-63732) K1: False positive OUTER_CLASS_ARGUMENTS_REQUIRED inside anonymous object
- [`KT-64644`](https://youtrack.jetbrains.com/issue/KT-64644) K2: Compiler crash in FirTypeParameterBoundsChecker
- [`KT-64312`](https://youtrack.jetbrains.com/issue/KT-64312) K2: FirPropertySymbol.hasBackingField() always returns true for properties from other modules
- [`KT-64420`](https://youtrack.jetbrains.com/issue/KT-64420) K2: Wrong module descriptor for builtin classes
- [`KT-64127`](https://youtrack.jetbrains.com/issue/KT-64127) K2: incorrect resolution of inherited members on Java classes inheriting classes from different packages in the presence of identically named classes in the same packages
- [`KT-63446`](https://youtrack.jetbrains.com/issue/KT-63446) IrFakeOverrideBuilder: AbstractMethodError due to missing bridge for generic method in a Java superclass
- [`KT-63867`](https://youtrack.jetbrains.com/issue/KT-63867) K2: Smartcast is allowed inside changing lambda with cycles
- [`KT-64609`](https://youtrack.jetbrains.com/issue/KT-64609) K2: INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE diagnostic is missed for primary constructor properties
- [`KT-63777`](https://youtrack.jetbrains.com/issue/KT-63777) K2: Smartcast is allowed inside changing lambda with bounds
- [`KT-64059`](https://youtrack.jetbrains.com/issue/KT-64059) K2: CYCLIC_INHERITANCE_HIERARCHY while using nested annotation in an outer class declaration
- [`KT-63528`](https://youtrack.jetbrains.com/issue/KT-63528) K2: Missing UNNECESSARY_SAFE_CALL for warning level annotated java declarations
- [`KT-64607`](https://youtrack.jetbrains.com/issue/KT-64607) K2: extension functions on UInt and Number lead to JVM ClassCastException
- [`KT-63761`](https://youtrack.jetbrains.com/issue/KT-63761) K2: False positive "Unresolved reference" caused by object's parameter in enum class which is passed as annotation parameter
- [`KT-62816`](https://youtrack.jetbrains.com/issue/KT-62816) K2: Annotation use site targets printing could be improved in diagnostics' messages
- [`KT-62815`](https://youtrack.jetbrains.com/issue/KT-62815) K2: FIR renderings leak through some diagnostics' message
- [`KT-35289`](https://youtrack.jetbrains.com/issue/KT-35289) Confusing warning message "Duplicate label in when"
- [`KT-49084`](https://youtrack.jetbrains.com/issue/KT-49084) Contracts: error message is unclear
- [`KT-63228`](https://youtrack.jetbrains.com/issue/KT-63228) K2: Upper bound violation diagnostic renders compiler internals about SourceAttribute
- [`KT-62386`](https://youtrack.jetbrains.com/issue/KT-62386) K2: Proofread quotes in diagnostic messages
- [`KT-64081`](https://youtrack.jetbrains.com/issue/KT-64081) K2: Incorrect smartcast candidate calculation in MemberScopeTowerLevel
- [`KT-32420`](https://youtrack.jetbrains.com/issue/KT-32420) Confusing error message "Contracts are allowed only for top-level functions" when `contract` block is not first expression
- [`KT-61937`](https://youtrack.jetbrains.com/issue/KT-61937) K2: implicit script receiver from ScriptDefinition are not visible for invoke
- [`KT-58767`](https://youtrack.jetbrains.com/issue/KT-58767) Inheritance opt-in enforcement via `@SubclassOptInRequired` can be avoided with type aliases
- [`KT-59818`](https://youtrack.jetbrains.com/issue/KT-59818) K2: Explore the TODO about suspend functions overridden in Java in FirHelpers
- [`KT-63233`](https://youtrack.jetbrains.com/issue/KT-63233) K2 : false negative `Class is not abstract and does not implement abstract member` with abstract suspend function
- [`KT-59344`](https://youtrack.jetbrains.com/issue/KT-59344) K2: implement deprecation warnings from KT-53153
- [`KT-63379`](https://youtrack.jetbrains.com/issue/KT-63379) K2. Argument type mismatch on creating functional interface instance with function literal as an argument with `in` type projection
- [`KT-64308`](https://youtrack.jetbrains.com/issue/KT-64308) K2: prefer call with Unit conversion at lower level to one without Unit conversion at upper level
- [`KT-64307`](https://youtrack.jetbrains.com/issue/KT-64307) K2: prefer function with default arguments at lower level to one without them at upper level during callable reference resolve
- [`KT-64306`](https://youtrack.jetbrains.com/issue/KT-64306) K2: prefer SAM at lower level to a functional type at upper level
- [`KT-64341`](https://youtrack.jetbrains.com/issue/KT-64341) Kotlin/JVM: Missing line number generation for intrinsic comparisons
- [`KT-64238`](https://youtrack.jetbrains.com/issue/KT-64238) Add proper documentation to the `IdeCodegenSettings` class
- [`KT-63667`](https://youtrack.jetbrains.com/issue/KT-63667) K2/KMP: exception when expect property matched to java field
- [`KT-59915`](https://youtrack.jetbrains.com/issue/KT-59915) K2: Disappeared TOO_MANY_ARGUMENTS
- [`KT-57755`](https://youtrack.jetbrains.com/issue/KT-57755) K2/JVM: Fix computing a "signature" mangled name for the `main` function
- [`KT-63645`](https://youtrack.jetbrains.com/issue/KT-63645) K2: Replace special f/o symbols with normal ones after actualization
- [`KT-63076`](https://youtrack.jetbrains.com/issue/KT-63076) K2: change in behavior for synthetic properties in Kotlin-Java hierarchy
- [`KT-63723`](https://youtrack.jetbrains.com/issue/KT-63723) Frontend manglers improperly handle error type
- [`KT-56491`](https://youtrack.jetbrains.com/issue/KT-56491) K2: Fix reporting AMBIGUOUS_ANONYMOUS_TYPE_INFERRED if anonymous object is leaked in type argument
- [`KT-63738`](https://youtrack.jetbrains.com/issue/KT-63738) K2: Some declarations are missing in the hierarchy of overridden symbols
- [`KT-62242`](https://youtrack.jetbrains.com/issue/KT-62242) K2: Uniformly treat enum entries as anonymous objects
- [`KT-62281`](https://youtrack.jetbrains.com/issue/KT-62281) K2: build DuckDuckGo Android user project and pass it to CI
- [`KT-60266`](https://youtrack.jetbrains.com/issue/KT-60266) K2: origin is not set for FOR_LOOP_ITERATOR
- [`KT-59875`](https://youtrack.jetbrains.com/issue/KT-59875) K2: Disappeared UNRESOLVED_REFERENCE_WRONG_RECEIVER
- [`KT-62394`](https://youtrack.jetbrains.com/issue/KT-62394) K2: Synthetic property scope doesn't consider java classes in the hierarchy
- [`KT-62715`](https://youtrack.jetbrains.com/issue/KT-62715) K2: Missing WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE
- [`KT-62723`](https://youtrack.jetbrains.com/issue/KT-62723) K2: Missing WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION
- [`KT-62722`](https://youtrack.jetbrains.com/issue/KT-62722) K2: Missing NESTED_WASM_IMPORT
- [`KT-62721`](https://youtrack.jetbrains.com/issue/KT-62721) K2: Missing WASM_EXPORT_ON_EXTERNAL_DECLARATION
- [`KT-62720`](https://youtrack.jetbrains.com/issue/KT-62720) K2: Missing JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION
- [`KT-62719`](https://youtrack.jetbrains.com/issue/KT-62719) K2: Missing NESTED_WASM_EXPORT
- [`KT-62718`](https://youtrack.jetbrains.com/issue/KT-62718) K2: Missing WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE
- [`KT-62717`](https://youtrack.jetbrains.com/issue/KT-62717) K2: Missing WASM_IMPORT_EXPORT_PARAMETER_DEFAULT_VALUE
- [`KT-62716`](https://youtrack.jetbrains.com/issue/KT-62716) K2: Missing WASM_IMPORT_EXPORT_VARARG_PARAMETER
- [`KT-60225`](https://youtrack.jetbrains.com/issue/KT-60225) K2: compiler FIR symbol resolution crash on a call to an extension function whose receiver contains a type parameter with a recursive upper bound
- [`KT-60090`](https://youtrack.jetbrains.com/issue/KT-60090) K2: Introduced DEPRECATED_PARCELER
- [`KT-59949`](https://youtrack.jetbrains.com/issue/KT-59949) K2: Disappeared DEPRECATED_PARCELER
- [`KT-64045`](https://youtrack.jetbrains.com/issue/KT-64045) K2: "Expect declaration * is incompatible with actual" when function parameter names are different
- [`KT-62018`](https://youtrack.jetbrains.com/issue/KT-62018) K2: prohibit suspend-marked anonymous function declarations in statement positions
- [`KT-63973`](https://youtrack.jetbrains.com/issue/KT-63973) K2: "NoSuchElementException: Array is empty" with vararg used within tail recursive function
- [`KT-61792`](https://youtrack.jetbrains.com/issue/KT-61792) KMP: Backend error on `@Deprecated` usage with DeprecationLevel.HIDDEN in K2
- [`KT-57788`](https://youtrack.jetbrains.com/issue/KT-57788) Fix computing mangled names of types with `@EnhancedNullability` from IR-based declaration descriptors
- [`KT-63249`](https://youtrack.jetbrains.com/issue/KT-63249) K2: change in annotation resolve when ambiguous
- [`KT-62553`](https://youtrack.jetbrains.com/issue/KT-62553) K2: Add `topLevelClassifierPackageNames` to symbol name providers
- [`KT-64148`](https://youtrack.jetbrains.com/issue/KT-64148) K2: class cast exception org.jetbrains.kotlin.fir.types.ConeStarProjection
- [`KT-63665`](https://youtrack.jetbrains.com/issue/KT-63665) K2: "NullPointerException" caused by class with the companion object and extra curly brace
- [`KT-62736`](https://youtrack.jetbrains.com/issue/KT-62736) K2: Disappeared NESTED_JS_EXPORT
- [`KT-62347`](https://youtrack.jetbrains.com/issue/KT-62347) Prohibit using property+invoke convention for delegated properties
- [`KT-59421`](https://youtrack.jetbrains.com/issue/KT-59421) K2: Missing CONTEXT_RECEIVERS_WITH_BACKING_FIELD
- [`KT-59903`](https://youtrack.jetbrains.com/issue/KT-59903) K2: Disappeared DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE
- [`KT-54997`](https://youtrack.jetbrains.com/issue/KT-54997) Forbid implicit non-public-API accesses from public-API inline function
- [`KT-34372`](https://youtrack.jetbrains.com/issue/KT-34372) Report missed error for virtual inline method in enum classes
- [`KT-62926`](https://youtrack.jetbrains.com/issue/KT-62926) K2: IR has missing receivers during expect-actual matching
- [`KT-62565`](https://youtrack.jetbrains.com/issue/KT-62565) K2 cannot infer type parameters in case of expected functional type
- [`KT-63328`](https://youtrack.jetbrains.com/issue/KT-63328) K2: Top-level properties in scripts can be used while uninitialized
- [`KT-62120`](https://youtrack.jetbrains.com/issue/KT-62120) K2: "NoSuchMethodError: java.lang.String" at runtime on class delegating to Java type
- [`KT-36876`](https://youtrack.jetbrains.com/issue/KT-36876) Smartcast doesn't work when class has  property available through the invoke
- [`KT-63835`](https://youtrack.jetbrains.com/issue/KT-63835) K2: metadata compilation with constants is falling for Native
- [`KT-60251`](https://youtrack.jetbrains.com/issue/KT-60251) K2: delegated method are delegating to different methods in hierarchy compared to K1
- [`KT-63574`](https://youtrack.jetbrains.com/issue/KT-63574) K2: "IllegalStateException: IrFieldPublicSymbolImpl for java.nio/ByteOrder.LITTLE_ENDIAN"
- [`KT-61068`](https://youtrack.jetbrains.com/issue/KT-61068) Bounds of type parameters are not enforced during inheritance of inner classes with generic outer classes
- [`KT-60504`](https://youtrack.jetbrains.com/issue/KT-60504) K2: difference between LL FIR and FIR in enhanced return type with annotation
- [`KT-64147`](https://youtrack.jetbrains.com/issue/KT-64147) K2: Generate FIR diagnostics with explicit types
- [`KT-62961`](https://youtrack.jetbrains.com/issue/KT-62961) K2 / KMP: NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS with expect enum class and typealias
- [`KT-53749`](https://youtrack.jetbrains.com/issue/KT-53749) Support builder inference restriction in FIR
- [`KT-59390`](https://youtrack.jetbrains.com/issue/KT-59390) K2: Missing BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION
- [`KT-61065`](https://youtrack.jetbrains.com/issue/KT-61065) K2: `@Suppress` annotation is ignored inside preconditions of when-clauses
- [`KT-59368`](https://youtrack.jetbrains.com/issue/KT-59368) K2: Missing SUBTYPING_BETWEEN_CONTEXT_RECEIVERS
- [`KT-64083`](https://youtrack.jetbrains.com/issue/KT-64083) K2: "KotlinIllegalArgumentExceptionWithAttachments: Unexpected returnTypeRef. Expected is FirResolvedTypeRef, but was FirJavaTypeRef"
- [`KT-37308`](https://youtrack.jetbrains.com/issue/KT-37308) No smart cast when the null check is performed on a child property through a function with a contract
- [`KT-62589`](https://youtrack.jetbrains.com/issue/KT-62589) K2: Investigate need of non-nullable IdSignature in Fir2IrLazyDeclarations
- [`KT-59894`](https://youtrack.jetbrains.com/issue/KT-59894) K2: Disappeared ANNOTATION_ARGUMENT_MUST_BE_CONST
- [`KT-63329`](https://youtrack.jetbrains.com/issue/KT-63329) K2: difference in SAM-conversion casts generation
- [`KT-64062`](https://youtrack.jetbrains.com/issue/KT-64062) K2 IDE. NPE on typing nullable parameter in return
- [`KT-61427`](https://youtrack.jetbrains.com/issue/KT-61427) K2/MPP/JS does not report Expecting a top level declaration and FIR2IR crashes
- [`KT-64031`](https://youtrack.jetbrains.com/issue/KT-64031) K2: Revise naming in FirBuilderInferenceSession
- [`KT-55252`](https://youtrack.jetbrains.com/issue/KT-55252) Backend Internal error during psi2ir in native compile tasks (NPE in getKlibModuleOrigin)
- [`KT-50453`](https://youtrack.jetbrains.com/issue/KT-50453) Improve builder inference diagnostics with type mismatch due to chosen inapplicable overload
- [`KT-56949`](https://youtrack.jetbrains.com/issue/KT-56949) K2: Builder inference violates upper bound
- [`KT-63648`](https://youtrack.jetbrains.com/issue/KT-63648) K2: values of postponed type variable don't introduce type constraints in extension receiver positions during builder-style type inference
- [`KT-64028`](https://youtrack.jetbrains.com/issue/KT-64028) K2: Investigate questionable condition in FirBuilderInfernceSession
- [`KT-60031`](https://youtrack.jetbrains.com/issue/KT-60031) K2: Introduced NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS
- [`KT-55809`](https://youtrack.jetbrains.com/issue/KT-55809) K2: Support pre-release checks for klibs
- [`KT-59881`](https://youtrack.jetbrains.com/issue/KT-59881) K2: Disappeared UNSUPPORTED
- [`KT-63448`](https://youtrack.jetbrains.com/issue/KT-63448) K2: CONFLICTING_INHERITED_JVM_DECLARATIONS with `@JvmField`
- [`KT-63705`](https://youtrack.jetbrains.com/issue/KT-63705) False positive UNSAFE_IMPLICIT_INVOKE_CALL after explicit null check of the constructor val property
- [`KT-63865`](https://youtrack.jetbrains.com/issue/KT-63865) K2: "IllegalArgumentException: Failed requirement." caused by lambda parameters with different type in init block
- [`KT-62036`](https://youtrack.jetbrains.com/issue/KT-62036) KMP: consider prohibiting `actual fake-override` when the corresponding `expect class` has default arguments
- [`KT-62609`](https://youtrack.jetbrains.com/issue/KT-62609) K2. Type argument inference changed for object of Java class with several common parents
- [`KT-30369`](https://youtrack.jetbrains.com/issue/KT-30369) Smartcasts from safe call + null check don't work if explicit true/false check is used
- [`KT-30376`](https://youtrack.jetbrains.com/issue/KT-30376) Smartcasts don't propagate to the original variable when use not-null assertion or cast expression
- [`KT-30868`](https://youtrack.jetbrains.com/issue/KT-30868) Unsound smartcast if smartcast source and break is placed inside square brackets (indexing expression)
- [`KT-31053`](https://youtrack.jetbrains.com/issue/KT-31053) Nothing? type check isn't equivalent to null check is some places
- [`KT-29935`](https://youtrack.jetbrains.com/issue/KT-29935) Smartcasts don't work if explicit annotated true/false check is used
- [`KT-30903`](https://youtrack.jetbrains.com/issue/KT-30903) Smartcast to null doesn't affect computing of exhaustiveness
- [`KT-63564`](https://youtrack.jetbrains.com/issue/KT-63564) K/Wasm: CompilationException with 2.0.0-Beta1
- [`KT-63345`](https://youtrack.jetbrains.com/issue/KT-63345) K2: FIR2IR chooses an incorrect type for smartcast in case of SAM conversion
- [`KT-63848`](https://youtrack.jetbrains.com/issue/KT-63848) ReflectiveAccessLowering does not count arguments of super-calls
- [`KT-62544`](https://youtrack.jetbrains.com/issue/KT-62544) K2: IllegalAccessError when functional type argument is inferred to package-private type
- [`KT-61920`](https://youtrack.jetbrains.com/issue/KT-61920) K2: False negative CONST_VAL_WITH_NON_CONST_INITIALIZER when initializer is Java field
- [`KT-63649`](https://youtrack.jetbrains.com/issue/KT-63649) K2: Wild card in superclass confuses EXPANSIVE_INHERITANCE checker
- [`KT-63569`](https://youtrack.jetbrains.com/issue/KT-63569) K2: "IllegalStateException: ?!id:1" caused by private function call
- [`KT-63842`](https://youtrack.jetbrains.com/issue/KT-63842) K2: some arguments of annotations on local declarations are unresolved
- [`KT-63832`](https://youtrack.jetbrains.com/issue/KT-63832) K2: missed context during annotation argument resolution for a type alias, init and property receiver
- [`KT-62559`](https://youtrack.jetbrains.com/issue/KT-62559) KMP, K2: prevent reporting ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT twice in CLI
- [`KT-24652`](https://youtrack.jetbrains.com/issue/KT-24652) Elvis with 'break' can produce unsound smartcasts in while-true loop
- [`KT-28508`](https://youtrack.jetbrains.com/issue/KT-28508) Possible unsound smartcast in class initializer
- [`KT-28759`](https://youtrack.jetbrains.com/issue/KT-28759) No not-null smartcast from direct assignment if it's split into declaration and value assignment
- [`KT-28760`](https://youtrack.jetbrains.com/issue/KT-28760) No not-null smartcast from direct assignment of `this`
- [`KT-29878`](https://youtrack.jetbrains.com/issue/KT-29878) Smartcasts from type check or null check don't work if explicit true check as reference equality is used
- [`KT-29936`](https://youtrack.jetbrains.com/issue/KT-29936) Smartcasts don't work if comparing with return value of some function and explicit true/false check is used
- [`KT-30317`](https://youtrack.jetbrains.com/issue/KT-30317) Smartcast doesn't work if smartcast source is used as an operand of the reference equality
- [`KT-63071`](https://youtrack.jetbrains.com/issue/KT-63071) K2 supports calling functions with the dynamic receiver over `Nothing?`
- [`KT-59896`](https://youtrack.jetbrains.com/issue/KT-59896) K2: Disappeared WRONG_ANNOTATION_TARGET
- [`KT-56849`](https://youtrack.jetbrains.com/issue/KT-56849) Implement K/Wasm K1 diagnostics in K2
- [`KT-31636`](https://youtrack.jetbrains.com/issue/KT-31636) Expect-actual matching doesn't work for inner/nested classes with explicit constructor using typealiases
- [`KT-63361`](https://youtrack.jetbrains.com/issue/KT-63361) K2: Expected FirResolvedTypeRef for return type of FirDefaultPropertyGetter(SubstitutionOverride(DeclarationSite)) but FirImplicitTypeRefImplWithoutSource found
- [`KT-62913`](https://youtrack.jetbrains.com/issue/KT-62913) Convert DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE to checking incompatibility
- [`KT-63550`](https://youtrack.jetbrains.com/issue/KT-63550) K2: fake-override in expect covariant override in actual. Move diagnostics from backend to frontend
- [`KT-62491`](https://youtrack.jetbrains.com/issue/KT-62491) K2. No `'when' expression must be exhaustive` error when Java sealed class inheritors are not listed in `permits` clause
- [`KT-63443`](https://youtrack.jetbrains.com/issue/KT-63443) IrFakeOverrideBuilder: ISE "No new fake override recorded" when Java superclass declares abstract toString
- [`KT-62679`](https://youtrack.jetbrains.com/issue/KT-62679) K2: drop ARGUMENTS_OF_ANNOTATIONS phase
- [`KT-63600`](https://youtrack.jetbrains.com/issue/KT-63600) K2: Duplicate WRONG_NULLABILITY_FOR_JAVA_OVERRIDE
- [`KT-63508`](https://youtrack.jetbrains.com/issue/KT-63508) K2: "IllegalArgumentException: Not FirResolvedTypeRef (String) in storeResult" caused by `@Deprecated` Java function and typo
- [`KT-63656`](https://youtrack.jetbrains.com/issue/KT-63656) K2: "IllegalArgumentException: Local com/example/<anonymous> should never be used to find its corresponding classifier"
- [`KT-63459`](https://youtrack.jetbrains.com/issue/KT-63459) K2: OPT_IN_USAGE_ERROR is absent when calling the enum primary constructor
- [`KT-59582`](https://youtrack.jetbrains.com/issue/KT-59582) OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN on an annotation import
- [`KT-60614`](https://youtrack.jetbrains.com/issue/KT-60614) K2: Conflicting INVISIBLE_REFERENCE and UNRESOLVED_REFERENCE reported depending on FIR test for transitive friend module dependencies
- [`KT-59983`](https://youtrack.jetbrains.com/issue/KT-59983) K2: Disappeared IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS
- [`KT-63068`](https://youtrack.jetbrains.com/issue/KT-63068) K2 supports typeRef-name labels
- [`KT-63642`](https://youtrack.jetbrains.com/issue/KT-63642) JVM_IR: don't generate reflective access to getter/setter without property
- [`KT-62212`](https://youtrack.jetbrains.com/issue/KT-62212) K2: require matching of suspend status for override check
- [`KT-60983`](https://youtrack.jetbrains.com/issue/KT-60983) K2: "Argument type mismatch: actual type is android/view/View.OnApplyWindowInsetsListener but androidx/core/view/OnApplyWindowInsetsListener? was expected"
- [`KT-63597`](https://youtrack.jetbrains.com/issue/KT-63597) JVM_IR: Properly handle type parameters of outer declaration in code fragment
- [`KT-59913`](https://youtrack.jetbrains.com/issue/KT-59913) K2: Disappeared UNSUPPORTED_FEATURE
- [`KT-63593`](https://youtrack.jetbrains.com/issue/KT-63593) K2: FIR2IR converts arguments of array set call for dynamic receiver twice
- [`KT-63317`](https://youtrack.jetbrains.com/issue/KT-63317) K2: Disallow generic types in contract type assertions
- [`KT-59922`](https://youtrack.jetbrains.com/issue/KT-59922) K2: Disappeared CANNOT_CHECK_FOR_ERASED
- [`KT-59561`](https://youtrack.jetbrains.com/issue/KT-59561) K2/MPP reports INCOMPATIBLE_MATCHING when an actual annotation declaration with vararg property is typealias with `@Suppress`
- [`KT-63241`](https://youtrack.jetbrains.com/issue/KT-63241) IJ monorepo K2 QG: backward-incompatible compiler ABI change leads to run-time failures of Fleet's kotlinc plugins
- [`KT-55318`](https://youtrack.jetbrains.com/issue/KT-55318) Redundant variance projection causes wrong signature in klib
- [`KT-57513`](https://youtrack.jetbrains.com/issue/KT-57513) K2: Bound smart casts don't work with Strings
- [`KT-59988`](https://youtrack.jetbrains.com/issue/KT-59988) K2: Disappeared TYPE_ARGUMENTS_NOT_ALLOWED
- [`KT-59936`](https://youtrack.jetbrains.com/issue/KT-59936) K2: Disappeared ARGUMENT_PASSED_TWICE
- [`KT-61959`](https://youtrack.jetbrains.com/issue/KT-61959) K2: Type parameters from outer class leak to nested class
- [`KT-58094`](https://youtrack.jetbrains.com/issue/KT-58094) K2: Review IrBuiltinsOverFir
- [`KT-63522`](https://youtrack.jetbrains.com/issue/KT-63522) K2: wrong context for delegated field type
- [`KT-63454`](https://youtrack.jetbrains.com/issue/KT-63454) Properly check that inline fun is in the same module as callee in `IrSourceCompilerForInline`
- [`KT-59951`](https://youtrack.jetbrains.com/issue/KT-59951) K2: Disappeared NO_TYPE_ARGUMENTS_ON_RHS
- [`KT-62727`](https://youtrack.jetbrains.com/issue/KT-62727) K2: Missing JSCODE_UNSUPPORTED_FUNCTION_KIND
- [`KT-62726`](https://youtrack.jetbrains.com/issue/KT-62726) K2: Missing JSCODE_WRONG_CONTEXT
- [`KT-62725`](https://youtrack.jetbrains.com/issue/KT-62725) K2: Missing JSCODE_INVALID_PARAMETER_NAME
- [`KT-62314`](https://youtrack.jetbrains.com/issue/KT-62314) Make usages of JavaTypeParameterStack safe
- [`KT-60924`](https://youtrack.jetbrains.com/issue/KT-60924) FIR2IR: Get rid of all unsafe usages of IrSymbol.owner
- [`KT-59402`](https://youtrack.jetbrains.com/issue/KT-59402) K2: Missing EXPANSIVE_INHERITANCE and EXPANSIVE_INHERITANCE_IN_JAVA
- [`KT-57949`](https://youtrack.jetbrains.com/issue/KT-57949) FIR: SignatureEnhancement: mutation of java enum entry
- [`KT-62724`](https://youtrack.jetbrains.com/issue/KT-62724) K2: Missing WRONG_JS_FUN_TARGET
- [`KT-62856`](https://youtrack.jetbrains.com/issue/KT-62856) K2: Don't create IR declaration when its symbol is accessed in fir2ir
- [`KT-61329`](https://youtrack.jetbrains.com/issue/KT-61329) K2: Review for diagnostic messages reported by CLI arguments processing
- [`KT-58953`](https://youtrack.jetbrains.com/issue/KT-58953) K2 doesn't work with Compose Multiplatform
- [`KT-63599`](https://youtrack.jetbrains.com/issue/KT-63599) False negative WRONG_NULLABILITY_FOR_JAVA_OVERRIDE when Java parameter is warning-severity not-null and override isn't a DNN
- [`KT-62711`](https://youtrack.jetbrains.com/issue/KT-62711) Incorrect ParsedCodeMetaInfo instances
- [`KT-63122`](https://youtrack.jetbrains.com/issue/KT-63122) K2: Improve 'EVALUATION_ERROR' messages
- [`KT-63164`](https://youtrack.jetbrains.com/issue/KT-63164) K2/JVM: compiler codegen crash on invisible property IllegalStateException: Fake override should have at least one overridden descriptor
- [`KT-56614`](https://youtrack.jetbrains.com/issue/KT-56614) K2: Incorrect overload resolution with SAM types
- [`KT-62783`](https://youtrack.jetbrains.com/issue/KT-62783) K2: False positive CAST_NEVER_SUCCEEDS when casting nullable expression to it's non-nullable generic base class
- [`KT-47931`](https://youtrack.jetbrains.com/issue/KT-47931) FIR DFA: smartcast not working for `if (x!=null || x!=null && x!=null) {}`
- [`KT-62735`](https://youtrack.jetbrains.com/issue/KT-62735) K2: Disappeared EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER
- [`KT-62733`](https://youtrack.jetbrains.com/issue/KT-62733) K2: Disappeared WRONG_EXTERNAL_DECLARATION
- [`KT-62734`](https://youtrack.jetbrains.com/issue/KT-62734) K2: Disappeared INLINE_EXTERNAL_DECLARATION
- [`KT-62618`](https://youtrack.jetbrains.com/issue/KT-62618) K2: Fix the `ensureAllMessagesPresent` test
- [`KT-60312`](https://youtrack.jetbrains.com/issue/KT-60312) K2: CCE “class [I cannot be cast to class java.lang.Number ([I and java.lang.Number are in module java.base of loader 'bootstrap')” on using IntArray as vararg
- [`KT-58531`](https://youtrack.jetbrains.com/issue/KT-58531) K2: "Property must be initialized" compile error
- [`KT-54064`](https://youtrack.jetbrains.com/issue/KT-54064) K2. Conflicting declarations error differs for k1 and k2
- [`KT-52432`](https://youtrack.jetbrains.com/issue/KT-52432) Using the IDE compiled with K2 (useFir) throws VerifyError exception
- [`KT-59825`](https://youtrack.jetbrains.com/issue/KT-59825) K2: Fix the TODO about `wasExperimentalMarkerClasses` in `FirSinceKotlinHelpers`
- [`KT-26045`](https://youtrack.jetbrains.com/issue/KT-26045) False positive DUPLICATE_LABEL_IN_WHEN for safe calls
- [`KT-59514`](https://youtrack.jetbrains.com/issue/KT-59514) K2: New inference error with jspecify and Java interop
- [`KT-63094`](https://youtrack.jetbrains.com/issue/KT-63094) K2: Exception from fir2ir during conversion data class with property of dynamic type
- [`KT-59822`](https://youtrack.jetbrains.com/issue/KT-59822) K2: Fix the TODO in FirConstChecks
- [`KT-59493`](https://youtrack.jetbrains.com/issue/KT-59493) Definitely non-nullable types have type inference issues with extension functions
- [`KT-63396`](https://youtrack.jetbrains.com/issue/KT-63396) K2: property from companion object are unresolved as an annotation argument in type parameter
- [`KT-62925`](https://youtrack.jetbrains.com/issue/KT-62925) K2: Disappeared EXPOSED_FUNCTION_RETURN_TYPE for package-private and type args
- [`KT-63430`](https://youtrack.jetbrains.com/issue/KT-63430) IrFakeOverrideBuilder: VerifyError on calling a function with a context receiver from a superclass
- [`KT-58754`](https://youtrack.jetbrains.com/issue/KT-58754) "Not enough information to infer type variable for subcalls of if expression" when adding curly braces to a conditional inside a `lazy` property
- [`KT-54067`](https://youtrack.jetbrains.com/issue/KT-54067) K1 with NI: false positive UPPER_BOUND_VIOLATED in typealias constructor
- [`KT-62420`](https://youtrack.jetbrains.com/issue/KT-62420) K2: Remove ConeClassifierLookupTag from ConeTypeVariableTypeConstructor
- [`KT-63431`](https://youtrack.jetbrains.com/issue/KT-63431) K1: Incorrect resolution of call to Java class that extends CharSequence and inherits a `get(int): Char` method
- [`KT-55288`](https://youtrack.jetbrains.com/issue/KT-55288) False negative WRONG_ANNOTATION_TARGET on type under a nullability qualifier
- [`KT-61459`](https://youtrack.jetbrains.com/issue/KT-61459) K2: type parameters cannot be parameterized with type arguments
- [`KT-59998`](https://youtrack.jetbrains.com/issue/KT-59998) K2: Disappeared OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN
- [`KT-53308`](https://youtrack.jetbrains.com/issue/KT-53308) TYPE_MISMATCH: Contracts on boolean expression has no effect on referential equality to `null`
- [`KT-51160`](https://youtrack.jetbrains.com/issue/KT-51160) Type mismatch with contracts on narrowing sealed hierarchy fail to smart cast
- [`KT-49696`](https://youtrack.jetbrains.com/issue/KT-49696) Smart cast to non-null with inline non-modifying closures sometimes doesn't work
- [`KT-46586`](https://youtrack.jetbrains.com/issue/KT-46586) SMARTCAST_IMPOSSIBLE when assigning value inside lambda instead of if expression
- [`KT-41728`](https://youtrack.jetbrains.com/issue/KT-41728) False positive no smart cast with unreachable code after return in if expression
- [`KT-59482`](https://youtrack.jetbrains.com/issue/KT-59482) K2: build kmm-production-sample
- [`KT-57529`](https://youtrack.jetbrains.com/issue/KT-57529) K1/K2: "IllegalStateException: not identifier: <no name provided>" with hard keywords in angle brackets
- [`KT-62032`](https://youtrack.jetbrains.com/issue/KT-62032) K2: Render flexible types as A..B instead of cryptic ft<A, B> in diagnostic messages
- [`KT-59940`](https://youtrack.jetbrains.com/issue/KT-59940) K2: Disappeared ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE
- [`KT-59401`](https://youtrack.jetbrains.com/issue/KT-59401) K2: Missing ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE
- [`KT-56081`](https://youtrack.jetbrains.com/issue/KT-56081) K2: build kotlinx.serialization
- [`KT-63172`](https://youtrack.jetbrains.com/issue/KT-63172) K2: Java vararg setter should not be used as property accessor
- [`KT-61243`](https://youtrack.jetbrains.com/issue/KT-61243) K2: Always use declaredMemberScope-s in `FirConflictsHelpers` instead of `declarations`
- [`KT-59430`](https://youtrack.jetbrains.com/issue/KT-59430) K2: Missing CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY
- [`KT-62306`](https://youtrack.jetbrains.com/issue/KT-62306) K2: Compiler internal error for incorrect call on ILT
- [`KT-61592`](https://youtrack.jetbrains.com/issue/KT-61592) kt57320.kt weird diagnostic range for NO_ACTUAL_FOR_EXPECT
- [`KT-62334`](https://youtrack.jetbrains.com/issue/KT-62334) K2: FIR should not generate delegated functions for methods from java interface with default implementation
- [`KT-60294`](https://youtrack.jetbrains.com/issue/KT-60294) K2: lambda inside object capturing this, when not in K1
- [`KT-59590`](https://youtrack.jetbrains.com/issue/KT-59590) JVM IR: NotImplementedError during rendering of conflicting JVM signatures diagnostic
- [`KT-62607`](https://youtrack.jetbrains.com/issue/KT-62607) K2: "Overload resolution ambiguity between candidates"
- [`KT-55096`](https://youtrack.jetbrains.com/issue/KT-55096) K2: false-positive smartcast after equals check with reassignment in RHS of ==
- [`KT-63002`](https://youtrack.jetbrains.com/issue/KT-63002) K2: Fix flaky FirPsiOldFrontendDiagnosticsTestGenerated.Tests.Annotations#testAnnotatedErrorTypeRef
- [`KT-62916`](https://youtrack.jetbrains.com/issue/KT-62916) K2: False positive INCOMPATIBLE_MATCHING
- [`KT-45687`](https://youtrack.jetbrains.com/issue/KT-45687) Contract doesn't allow smart cast when implicit receiver and inference target is `this`
- [`KT-62137`](https://youtrack.jetbrains.com/issue/KT-62137) Compiler fails on null tracking (inference) for safe call
- [`KT-36976`](https://youtrack.jetbrains.com/issue/KT-36976) FIR: Provide exact smart casting type
- [`KT-60004`](https://youtrack.jetbrains.com/issue/KT-60004) K2: Disappeared CONTRACT_NOT_ALLOWED
- [`KT-62404`](https://youtrack.jetbrains.com/issue/KT-62404) K2 Scripting for gradle: unresolved name errors on implicit imports
- [`KT-62197`](https://youtrack.jetbrains.com/issue/KT-62197) K2 and Apache Commons's MutableLong: Overload resolution ambiguity between candidates
- [`KT-59890`](https://youtrack.jetbrains.com/issue/KT-59890) K2: Disappeared CONST_VAL_WITH_NON_CONST_INITIALIZER
- [`KT-53551`](https://youtrack.jetbrains.com/issue/KT-53551) suspend functional type with context receiver causes ClassCastException
- [`KT-61491`](https://youtrack.jetbrains.com/issue/KT-61491) K2 AA: Multiple FIR declarations for the same delegated property
- [`KT-55965`](https://youtrack.jetbrains.com/issue/KT-55965) K2: NPE via usage of functions that return Nothing but have no return expressions
- [`KT-60942`](https://youtrack.jetbrains.com/issue/KT-60942) K2: Transitive dependency IR is not deserialized correctly
- [`KT-55319`](https://youtrack.jetbrains.com/issue/KT-55319) K2: False negative NON_LOCAL_RETURN_NOT_ALLOWED for non-local returns example
- [`KT-59884`](https://youtrack.jetbrains.com/issue/KT-59884) K2: Disappeared NON_LOCAL_RETURN_NOT_ALLOWED
- [`KT-61942`](https://youtrack.jetbrains.com/issue/KT-61942) K2 + kotlinx.serialization: Incorrect 'Conflicting declarations' on only one declaration
- [`KT-62944`](https://youtrack.jetbrains.com/issue/KT-62944) K2: Symbols with context receiver shouldn't be rendered with line break
- [`KT-59977`](https://youtrack.jetbrains.com/issue/KT-59977) K2: Disappeared NO_ACTUAL_FOR_EXPECT
- [`KT-60117`](https://youtrack.jetbrains.com/issue/KT-60117) K2: ISE “Cannot serialize error type: ERROR CLASS: Cannot infer variable type without initializer / getter / delegate” on compiling lateinit property without initialization
- [`KT-60042`](https://youtrack.jetbrains.com/issue/KT-60042) K2: Introduced PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS
- [`KT-62467`](https://youtrack.jetbrains.com/issue/KT-62467) K2: Result type of elvis operator should be flexible if rhs is flexible
- [`KT-62126`](https://youtrack.jetbrains.com/issue/KT-62126) KJS / K2: "InterpreterError: VALUE_PARAMETER" caused by reflection, delegation and languageVersion = 1.9
- [`KT-56615`](https://youtrack.jetbrains.com/issue/KT-56615) K2: False-negative USELESS_CAST after double smartcast
- [`KT-59820`](https://youtrack.jetbrains.com/issue/KT-59820) K2: Investigate the TODO in FirCastDiagnosticsHelpers
- [`KT-61100`](https://youtrack.jetbrains.com/issue/KT-61100) K2: wrong type for "value" parameter of java annotation constructor
- [`KT-59996`](https://youtrack.jetbrains.com/issue/KT-59996) K2: Disappeared INVALID_CHARACTERS
- [`KT-62598`](https://youtrack.jetbrains.com/issue/KT-62598) K2: SOE through JvmBinaryAnnotationDeserializer with nested annotation with value parameter in other module
- [`KT-59070`](https://youtrack.jetbrains.com/issue/KT-59070) K1: Unbound private symbol with mixed Java/Kotlin hierarchy
- [`KT-60095`](https://youtrack.jetbrains.com/issue/KT-60095) K2: Introduced INCOMPATIBLE_TYPES
- [`KT-61598`](https://youtrack.jetbrains.com/issue/KT-61598) K2: report IR_WITH_UNSTABLE_ABI_COMPILED_CLASS
- [`KT-42625`](https://youtrack.jetbrains.com/issue/KT-42625) "Unresolved reference" when star import packages with conflicting entries
- [`KT-60123`](https://youtrack.jetbrains.com/issue/KT-60123) K2: PROPERTY_WITH_NO_TYPE_NO_INITIALIZER isn't working in IDE for lateinit property without a type
- [`KT-59935`](https://youtrack.jetbrains.com/issue/KT-59935) K2: Disappeared PROPERTY_WITH_NO_TYPE_NO_INITIALIZER
- [`KT-57931`](https://youtrack.jetbrains.com/issue/KT-57931) K1: unsafe assignment of nullable values to not-null Java fields via safe access operator
- [`KT-59992`](https://youtrack.jetbrains.com/issue/KT-59992) K2: Disappeared KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE
- [`KT-58455`](https://youtrack.jetbrains.com/issue/KT-58455) K2(LT). Internal compiler error "UninitializedPropertyAccessException: lateinit property identifier has not been initialized" on missing type parameter in "where" constraint
- [`KT-60714`](https://youtrack.jetbrains.com/issue/KT-60714) K2: Implement resolve to private members from Evaluator in K2
- [`KT-59577`](https://youtrack.jetbrains.com/issue/KT-59577) K2. Enum constant name is not specified in error text
- [`KT-60003`](https://youtrack.jetbrains.com/issue/KT-60003) K2: Disappeared INVALID_CHARACTERS_NATIVE_ERROR
- [`KT-62099`](https://youtrack.jetbrains.com/issue/KT-62099) K2: "Type arguments should be specified for an outer class" error about typealias
- [`KT-60111`](https://youtrack.jetbrains.com/issue/KT-60111) K2: Location regressions for operators
- [`KT-59974`](https://youtrack.jetbrains.com/issue/KT-59974) K2: Disappeared INAPPLICABLE_INFIX_MODIFIER
- [`KT-59399`](https://youtrack.jetbrains.com/issue/KT-59399) K2: Missing JSCODE_NO_JAVASCRIPT_PRODUCED
- [`KT-59388`](https://youtrack.jetbrains.com/issue/KT-59388) K2: Missing JSCODE_ERROR
- [`KT-59435`](https://youtrack.jetbrains.com/issue/KT-59435) K2: Missing JSCODE_ARGUMENT_SHOULD_BE_CONSTANT
- [`KT-59991`](https://youtrack.jetbrains.com/issue/KT-59991) K2: Disappeared FORBIDDEN_VARARG_PARAMETER_TYPE
- [`KT-60601`](https://youtrack.jetbrains.com/issue/KT-60601) K2 / Maven: Overload resolution ambiguity between candidates inline method
- [`KT-59973`](https://youtrack.jetbrains.com/issue/KT-59973) K2: Disappeared INAPPLICABLE_LATEINIT_MODIFIER
- [`KT-59933`](https://youtrack.jetbrains.com/issue/KT-59933) K2: Disappeared USAGE_IS_NOT_INLINABLE
- [`KT-60778`](https://youtrack.jetbrains.com/issue/KT-60778) K2: implement MISSING_DEPENDENCY_CLASS(_SUPERCLASS) errors
- [`KT-62581`](https://youtrack.jetbrains.com/issue/KT-62581) K2: Difference in `kind` flag in metadata
- [`KT-59967`](https://youtrack.jetbrains.com/issue/KT-59967) K2: Disappeared UNINITIALIZED_ENUM_ENTRY
- [`KT-59956`](https://youtrack.jetbrains.com/issue/KT-59956) K2: Disappeared INAPPLICABLE_OPERATOR_MODIFIER
- [`KT-35913`](https://youtrack.jetbrains.com/issue/KT-35913) Diagnostic error VAL_REASSIGNMENT is not reported multiple times
- [`KT-60059`](https://youtrack.jetbrains.com/issue/KT-60059) K2: Introduced VAL_REASSIGNMENT
- [`KT-59945`](https://youtrack.jetbrains.com/issue/KT-59945) K2: Disappeared ANONYMOUS_FUNCTION_WITH_NAME
- [`KT-62573`](https://youtrack.jetbrains.com/issue/KT-62573) K2: incorrect parsing behavior with named functions as expressions
- [`KT-55484`](https://youtrack.jetbrains.com/issue/KT-55484) K2: `@OptIn` false negative OPT_IN_USAGE_ERROR on equals operator call
- [`KT-56629`](https://youtrack.jetbrains.com/issue/KT-56629) K2: an instance of USELESS_CAST was not moved under EnableDfaWarningsInK2 language feature
- [`KT-58034`](https://youtrack.jetbrains.com/issue/KT-58034) Inconsistent resolve for nested objects in presence of a companion object property with the same name
- [`KT-59864`](https://youtrack.jetbrains.com/issue/KT-59864) K2: Bad locations with delegates
- [`KT-59584`](https://youtrack.jetbrains.com/issue/KT-59584) K2: Bad startOffset for 'this'
- [`KT-61388`](https://youtrack.jetbrains.com/issue/KT-61388) K2: ISE "Annotations are resolved twice" from CompilerRequiredAnnotationsComputationSession on nested annotation
- [`KT-62628`](https://youtrack.jetbrains.com/issue/KT-62628) K2: FirErrorTypeRefImpl doesn't have annotations
- [`KT-62447`](https://youtrack.jetbrains.com/issue/KT-62447) K2. "Replacing annotations in FirErrorTypeRefImpl is not supported" compiler error when annotation is used as variable type or return type
- [`KT-61055`](https://youtrack.jetbrains.com/issue/KT-61055) K2: Investigate if usage of `toResolvedCallableSymbol` is correct at FirDataFlowAnalyzer#processConditionalContract
- [`KT-61518`](https://youtrack.jetbrains.com/issue/KT-61518) K2: IAE: "Expected type to be resolved" at FirTypeUtilsKt.getResolvedType() on usage of Java annotation with default value for enum array parameter
- [`KT-61688`](https://youtrack.jetbrains.com/issue/KT-61688) K2: FIR renderings of type annotations leak through the diagnostics' messages
- [`KT-61794`](https://youtrack.jetbrains.com/issue/KT-61794) FIR: MergePostponedLambdaExitsNode.flow remains uninitialized after resolve
- [`KT-59986`](https://youtrack.jetbrains.com/issue/KT-59986) K2: Disappeared ITERATOR_MISSING
- [`KT-57802`](https://youtrack.jetbrains.com/issue/KT-57802) K2: Backend Internal error: RecordEnclosingMethodsLowering.kt
- [`KT-59941`](https://youtrack.jetbrains.com/issue/KT-59941) K2: Disappeared COMPONENT_FUNCTION_MISSING
- [`KT-61076`](https://youtrack.jetbrains.com/issue/KT-61076) K2: false-positive conflicting overloads error on suspending function and private Java method from a supertype
- [`KT-61075`](https://youtrack.jetbrains.com/issue/KT-61075) K2: type inference for delegate expressions with complexly bounded type variables fails on properties with annotated accessors
- [`KT-62671`](https://youtrack.jetbrains.com/issue/KT-62671) K2: fir2ir generates a duplicate of delegated function for class from a common module
- [`KT-62541`](https://youtrack.jetbrains.com/issue/KT-62541) K2: Missed type mismatch error
- [`KT-62585`](https://youtrack.jetbrains.com/issue/KT-62585) KMP, K2: fix ugly reporting of annotation arguments in ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT diagnostic
- [`KT-62143`](https://youtrack.jetbrains.com/issue/KT-62143) Error: Identity equality for arguments of types 'kotlin/Int?' and 'kotlin/Nothing?' is prohibited
- [`KT-62620`](https://youtrack.jetbrains.com/issue/KT-62620) Warn about `@OptIn`/`@Deprecated` for overrides of Any
- [`KT-59689`](https://youtrack.jetbrains.com/issue/KT-59689) K2: Fix complex smartcasts with safe calls
- [`KT-61517`](https://youtrack.jetbrains.com/issue/KT-61517) K2: FirModuleDescriptor should correctly provide dependencies from FirModuleData
- [`KT-62578`](https://youtrack.jetbrains.com/issue/KT-62578) K2: `@NoInfer` annotation doesn't work for deserialized functions
- [`KT-59916`](https://youtrack.jetbrains.com/issue/KT-59916) K2: Disappeared REPEATED_ANNOTATION
- [`KT-36844`](https://youtrack.jetbrains.com/issue/KT-36844) DELEGATE_SPECIAL_FUNCTION_MISSING highlight is missed when Delegate class has getValue property available through the invoke convention
- [`KT-62450`](https://youtrack.jetbrains.com/issue/KT-62450) K2: Disappeared OPT_IN_USAGE_ERROR for a data class property during the destructuring declaration
- [`KT-59997`](https://youtrack.jetbrains.com/issue/KT-59997) K2: Disappeared OPT_IN_USAGE_ERROR
- [`KT-60026`](https://youtrack.jetbrains.com/issue/KT-60026) K2: Introduced EXPOSED_TYPEALIAS_EXPANDED_TYPE
- [`KT-62393`](https://youtrack.jetbrains.com/issue/KT-62393) K2: FIR doesn't count visibility when creating synthetic property override
- [`KT-61191`](https://youtrack.jetbrains.com/issue/KT-61191) K2: Problem with `@OptionalExpectation`
- [`KT-61208`](https://youtrack.jetbrains.com/issue/KT-61208) EnumEntries mappings are generated incorrectly in the face of incremental compilation
- [`KT-57811`](https://youtrack.jetbrains.com/issue/KT-57811) K2: make java static string and int fields not null
- [`KT-53982`](https://youtrack.jetbrains.com/issue/KT-53982) Keep nullability when approximating local types in public signatures
- [`KT-62531`](https://youtrack.jetbrains.com/issue/KT-62531) InvalidProtocolBufferException on reading module metadata compiled by K2 from compilers earlier than 1.8.20 with -Xskip-metadata-version-check
- [`KT-61511`](https://youtrack.jetbrains.com/issue/KT-61511) IrFakeOverride builder: objc overridability condition is not supported
- [`KT-62316`](https://youtrack.jetbrains.com/issue/KT-62316) K2: CONFLICTING_INHERITED_JVM_DECLARATIONS on List subclass inheriting remove/removeAt from Java superclass
- [`KT-60671`](https://youtrack.jetbrains.com/issue/KT-60671) KMP: check other annotation targets in expect and actual annotations compatibility checker
- [`KT-62473`](https://youtrack.jetbrains.com/issue/KT-62473) K2: `@Suppress`("UNCHECKED_CAST")` doesn't work on rhs of augmented assignment call
- [`KT-59433`](https://youtrack.jetbrains.com/issue/KT-59433) K2: Missing NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE
- [`KT-62451`](https://youtrack.jetbrains.com/issue/KT-62451) K2: Disappeared OPT_IN_USAGE_ERROR for typealias
- [`KT-62452`](https://youtrack.jetbrains.com/issue/KT-62452) K2: Violation of OPT_IN_USAGE_ERROR non-propagating opt-in rules for typealias
- [`KT-59927`](https://youtrack.jetbrains.com/issue/KT-59927) K2: Disappeared INVISIBLE_REFERENCE
- [`KT-60080`](https://youtrack.jetbrains.com/issue/KT-60080) K2: Introduced INVISIBLE_SETTER
- [`KT-60104`](https://youtrack.jetbrains.com/issue/KT-60104) K2: Introduced FUNCTION_CALL_EXPECTED
- [`KT-59979`](https://youtrack.jetbrains.com/issue/KT-59979) K2: Disappeared SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS
- [`KT-62146`](https://youtrack.jetbrains.com/issue/KT-62146) K2: `@Suppress` does not work with named argument
- [`KT-62475`](https://youtrack.jetbrains.com/issue/KT-62475) K2: IrExternalModuleFragments contains incorrect data in Fir2Ir
- [`KT-59978`](https://youtrack.jetbrains.com/issue/KT-59978) K2: Disappeared EXPECTED_ENUM_ENTRY_WITH_BODY
- [`KT-59015`](https://youtrack.jetbrains.com/issue/KT-59015) K1+NI: "Type mismatch: inferred type is CapturedType(*) but Xy was expected" with star projection callable reference to extension function
- [`KT-61983`](https://youtrack.jetbrains.com/issue/KT-61983) K2: *fir.kt.txt dump uses different naming approach for local vars
- [`KT-59970`](https://youtrack.jetbrains.com/issue/KT-59970) K2: Disappeared NULLABLE_TYPE_IN_CLASS_LITERAL_LHS
- [`KT-58216`](https://youtrack.jetbrains.com/issue/KT-58216) K2 (2.0): when is not checked for exhaustiveness with Java sealed class
- [`KT-61205`](https://youtrack.jetbrains.com/issue/KT-61205) Compose Compiler K2/ios: No file for /App|App(){}[0] when running linkPodDebugFrameworkIosX64
- [`KT-58087`](https://youtrack.jetbrains.com/issue/KT-58087) Unexpected type mismatch after nullable captured type approximation
- [`KT-58240`](https://youtrack.jetbrains.com/issue/KT-58240) Support running irText compiler tests against the Native backend
- [`KT-59565`](https://youtrack.jetbrains.com/issue/KT-59565) K2. Internal error "IndexOutOfBoundsException: Index -1 out of bounds for length 0" on incorrect usage of annotation in type parameter
- [`KT-59954`](https://youtrack.jetbrains.com/issue/KT-59954) K2: Disappeared REPEATED_MODIFIER
- [`KT-57100`](https://youtrack.jetbrains.com/issue/KT-57100) K2 does not report Conflicting overloads and backend crashes with Exception during IR lowering on conflict overloading with suspend function
- [`KT-59955`](https://youtrack.jetbrains.com/issue/KT-59955) K2: Disappeared INCOMPATIBLE_MODIFIERS
- [`KT-61572`](https://youtrack.jetbrains.com/issue/KT-61572) [K2/N] Missing diagnostic SUPER_CALL_WITH_DEFAULT_PARAMETERS in test for MPP supercall with default params
- [`KT-62262`](https://youtrack.jetbrains.com/issue/KT-62262) [K2/N] tests/samples/uikit compilation fails with NPE in checkCanGenerateOverrideInit
- [`KT-62114`](https://youtrack.jetbrains.com/issue/KT-62114) K2: Unresolved reference for smart cast inside `when` (but not `if`)
- [`KT-59373`](https://youtrack.jetbrains.com/issue/KT-59373) K2: Missing INVISIBLE_MEMBER
- [`KT-61844`](https://youtrack.jetbrains.com/issue/KT-61844) K2: "Expression * of type * cannot be invoked as a function" caused by private property
- [`KT-61735`](https://youtrack.jetbrains.com/issue/KT-61735) [FIR] Assignment to val with flexible type dispatch receiver causes crash
- [`KT-59942`](https://youtrack.jetbrains.com/issue/KT-59942) K2: Disappeared ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT
- [`KT-62058`](https://youtrack.jetbrains.com/issue/KT-62058) K2: use PRE_RELEASE flag until 2.0-RC
- [`KT-59931`](https://youtrack.jetbrains.com/issue/KT-59931) K2: Disappeared CLASS_LITERAL_LHS_NOT_A_CLASS
- [`KT-62104`](https://youtrack.jetbrains.com/issue/KT-62104) K2: fix failing tests caused by KT-59940
- [`KT-61974`](https://youtrack.jetbrains.com/issue/KT-61974) K2: "ClassCastException: class cannot be cast to class java.lang.Void" in test
- [`KT-61637`](https://youtrack.jetbrains.com/issue/KT-61637) K2: Store all IR declarations inside Fir2IrDeclarationStorage
- [`KT-60921`](https://youtrack.jetbrains.com/issue/KT-60921) K2: IndexOutOfBoundsException on attempt to cast an element to inner class with type parameter
- [`KT-59429`](https://youtrack.jetbrains.com/issue/KT-59429) K2: Missing ABBREVIATED_NOTHING_RETURN_TYPE
- [`KT-59420`](https://youtrack.jetbrains.com/issue/KT-59420) K2: Missing ABBREVIATED_NOTHING_PROPERTY_TYPE
- [`KT-59965`](https://youtrack.jetbrains.com/issue/KT-59965) K2: Disappeared CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON
- [`KT-59952`](https://youtrack.jetbrains.com/issue/KT-59952) K2: Disappeared EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR
- [`KT-61732`](https://youtrack.jetbrains.com/issue/KT-61732) K2: Analysis API: resolve ambiguities in kotlin project
- [`KT-60499`](https://youtrack.jetbrains.com/issue/KT-60499) K2: Order of synthetic fields is different from K1's order
- [`KT-61773`](https://youtrack.jetbrains.com/issue/KT-61773) K2 Native: support reporting PRE_RELEASE_CLASS
- [`KT-61578`](https://youtrack.jetbrains.com/issue/KT-61578) [FIR] Resolution to private companion objects does not produce `INVISIBLE_REFERENCE` diagnostic
- [`KT-59985`](https://youtrack.jetbrains.com/issue/KT-59985) K2: Disappeared UNDERSCORE_USAGE_WITHOUT_BACKTICKS
- [`KT-62031`](https://youtrack.jetbrains.com/issue/KT-62031) K2: Render k2-specific flexible types in a more compact way in diagnostic messages
- [`KT-62030`](https://youtrack.jetbrains.com/issue/KT-62030) K2: Render dot-separated FQNs instead of slash-separated ones in diagnostics
- [`KT-59950`](https://youtrack.jetbrains.com/issue/KT-59950) K2: Disappeared ILLEGAL_ESCAPE
- [`KT-61827`](https://youtrack.jetbrains.com/issue/KT-61827) K2: Fix rendering of `NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS` message
- [`KT-61386`](https://youtrack.jetbrains.com/issue/KT-61386) IrFakeOverrideBuilder: wrong dispatch receiver type
- [`KT-59907`](https://youtrack.jetbrains.com/issue/KT-59907) K2: Disappeared RETURN_TYPE_MISMATCH
- [`KT-62056`](https://youtrack.jetbrains.com/issue/KT-62056) K2: Drop FIR_COMPILED_CLASS error in K1
- [`KT-61824`](https://youtrack.jetbrains.com/issue/KT-61824) K2: Don't render internal compiler type annotations in diagnostic messages
- [`KT-61826`](https://youtrack.jetbrains.com/issue/KT-61826) K2: Fix rendering of SUSPENSION_POINT_INSIDE_CRITICAL_SECTION message
- [`KT-57858`](https://youtrack.jetbrains.com/issue/KT-57858) `@PlatformDependent` annotation should be considered in JS and Native
- [`KT-61876`](https://youtrack.jetbrains.com/issue/KT-61876) K2: FirCommonSessionFactory does not register visibility checker for a library session
- [`KT-60264`](https://youtrack.jetbrains.com/issue/KT-60264) K2: while loop body block sometimes replaced with single expression
- [`KT-58542`](https://youtrack.jetbrains.com/issue/KT-58542) K2: Store abbreviated types in deserialized declarations as attributes for rendering
- [`KT-62008`](https://youtrack.jetbrains.com/issue/KT-62008) K2: Java getter function may be enhanced twice
- [`KT-61921`](https://youtrack.jetbrains.com/issue/KT-61921) K2: Check for false positive/negative diagnostics caused by wrong handling of typealiases
- [`KT-41997`](https://youtrack.jetbrains.com/issue/KT-41997) False positive "Value class cannot have properties with backing fields" inside expect class
- [`KT-62017`](https://youtrack.jetbrains.com/issue/KT-62017) K2: ISE "No real overrides for FUN FAKE_OVERRIDE" on calling package-private Java method through anonymous object
- [`KT-58247`](https://youtrack.jetbrains.com/issue/KT-58247) Incorrect inference of nullable types inside Optional
- [`KT-61309`](https://youtrack.jetbrains.com/issue/KT-61309) K2: Only named arguments are available for Java annotations
- [`KT-61366`](https://youtrack.jetbrains.com/issue/KT-61366) IrFakeOverrideBuilder ignores package-private visibility
- [`KT-59899`](https://youtrack.jetbrains.com/issue/KT-59899) K2: Disappeared EXPECTED_DECLARATION_WITH_BODY
- [`KT-59980`](https://youtrack.jetbrains.com/issue/KT-59980) K2: Disappeared EXPECTED_ENUM_CONSTRUCTOR
- [`KT-59982`](https://youtrack.jetbrains.com/issue/KT-59982) K2: Disappeared EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER
- [`KT-61499`](https://youtrack.jetbrains.com/issue/KT-61499) K2: False positive "Const 'val' initializer should be a constant value" when using typealias
- [`KT-62005`](https://youtrack.jetbrains.com/issue/KT-62005) K2: No conflicting declarations error for constructors of nested classes and member functions
- [`KT-60092`](https://youtrack.jetbrains.com/issue/KT-60092) K2: Introduced EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR
- [`KT-61972`](https://youtrack.jetbrains.com/issue/KT-61972) K2: FIR2IR crashes on converting data classes in MPP setup
- [`KT-60105`](https://youtrack.jetbrains.com/issue/KT-60105) K2: Introduced UNDERSCORE_USAGE_WITHOUT_BACKTICKS
- [`KT-60075`](https://youtrack.jetbrains.com/issue/KT-60075) K2: Introduced ACTUAL_WITHOUT_EXPECT
- [`KT-29316`](https://youtrack.jetbrains.com/issue/KT-29316) Change diagnostics strategy for equality-operators applicability
- [`KT-61751`](https://youtrack.jetbrains.com/issue/KT-61751) IrFakeOverrideBuilder: keep flexible type annotations when remapping/substituting types
- [`KT-61778`](https://youtrack.jetbrains.com/issue/KT-61778) K2: Overload resolution ambiguity between expect and non-expect in native build
- [`KT-57703`](https://youtrack.jetbrains.com/issue/KT-57703) K1/K2: unprecise constraint system behavior around integer literals and comparable arrays
- [`KT-61367`](https://youtrack.jetbrains.com/issue/KT-61367) K2: Introduce OptIn for FirExpression.coneTypeOrNull
- [`KT-61802`](https://youtrack.jetbrains.com/issue/KT-61802) K2: infinite recursion in constant evaluator causing StackOverflowError
- [`KT-60043`](https://youtrack.jetbrains.com/issue/KT-60043) K2: Introduced PROPERTY_AS_OPERATOR
- [`KT-61829`](https://youtrack.jetbrains.com/issue/KT-61829) K2. Internal error, FileAnalysisException when type argument doesn't conform expected type
- [`KT-61691`](https://youtrack.jetbrains.com/issue/KT-61691) K2: This annotation is not applicable to target 'local variable'
- [`KT-59925`](https://youtrack.jetbrains.com/issue/KT-59925) K2: Disappeared VIRTUAL_MEMBER_HIDDEN
- [`KT-61173`](https://youtrack.jetbrains.com/issue/KT-61173) K2: FirProperty.hasBackingField is true for an expect val
- [`KT-61696`](https://youtrack.jetbrains.com/issue/KT-61696) K2: Cannot override method of interface if superclass has package-protected method with same signature
- [`KT-59370`](https://youtrack.jetbrains.com/issue/KT-59370) K2: Missing JS_NAME_CLASH
- [`KT-36056`](https://youtrack.jetbrains.com/issue/KT-36056) [FIR] Fix implementation of try/catch/finally in DFA
- [`KT-61719`](https://youtrack.jetbrains.com/issue/KT-61719) K2. Invisible reference is shown for whole type reference instead of single name reference
- [`KT-35566`](https://youtrack.jetbrains.com/issue/KT-35566) False negative UPPER_BOUND_VIOLATED in a supertype of an inner class
- [`KT-60248`](https://youtrack.jetbrains.com/issue/KT-60248) K2: Type abbreviations are not stored in IR
- [`KT-61720`](https://youtrack.jetbrains.com/issue/KT-61720) K2: Delegates: Property type not specialised in property reference of setter
- [`KT-59251`](https://youtrack.jetbrains.com/issue/KT-59251) KMP/JS: forbid matching actual callable with dynamic return type to expect callable with non-dynamic return type
- [`KT-61510`](https://youtrack.jetbrains.com/issue/KT-61510) K2: internal declarations are invisible in cyclically dependent modules
- [`KT-54890`](https://youtrack.jetbrains.com/issue/KT-54890) FIR: fix resolve contract violations in FIR
- [`KT-60048`](https://youtrack.jetbrains.com/issue/KT-60048) K2: Introduced MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND
- [`KT-59425`](https://youtrack.jetbrains.com/issue/KT-59425) K2: Missing JS_FAKE_NAME_CLASH
- [`KT-59529`](https://youtrack.jetbrains.com/issue/KT-59529) K2: "property delegate must have" caused by class hierarchy
- [`KT-55471`](https://youtrack.jetbrains.com/issue/KT-55471) K2. Unresolved reference for nested type is shown instead of outer class
- [`KT-58896`](https://youtrack.jetbrains.com/issue/KT-58896) K2: Higher priority expect overload candidates in common code lose in overload resolution to non-expects
- [`KT-60780`](https://youtrack.jetbrains.com/issue/KT-60780) K2: missing PRE_RELEASE_CLASS
- [`KT-59855`](https://youtrack.jetbrains.com/issue/KT-59855) K2: Replace FirExpression.typeRef with coneType
- [`KT-53565`](https://youtrack.jetbrains.com/issue/KT-53565) K2: no WRONG_ANNOTATION_TARGET on when subject
- [`KT-54568`](https://youtrack.jetbrains.com/issue/KT-54568) K2: Type variables leak into implicit `it` parameter of lambdas
- [`KT-60892`](https://youtrack.jetbrains.com/issue/KT-60892) K2: Implement diagnostics around `@OptionalExpectation`
- [`KT-60917`](https://youtrack.jetbrains.com/issue/KT-60917) K2: "Unresolved reference" for operator for array value
- [`KT-59367`](https://youtrack.jetbrains.com/issue/KT-59367) K2: Missing MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES
- [`KT-60268`](https://youtrack.jetbrains.com/issue/KT-60268) K2: lazy annotation classes have wrong modality
- [`KT-60536`](https://youtrack.jetbrains.com/issue/KT-60536) K2: FIR2IR Crash when resolving to companion of internal class with Suppress("INVISIBLE_REFERENCE")
- [`KT-60292`](https://youtrack.jetbrains.com/issue/KT-60292) K2: annotations on local delegated properties are lost
- [`KT-59422`](https://youtrack.jetbrains.com/issue/KT-59422) K2: Missing NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION
- [`KT-61407`](https://youtrack.jetbrains.com/issue/KT-61407) K2: java.lang.IllegalArgumentException: Stability for initialized variable always should be computable
- [`KT-59186`](https://youtrack.jetbrains.com/issue/KT-59186) K2: False negative CONFLICTING_OVERLOADS in nested functions
- [`KT-54390`](https://youtrack.jetbrains.com/issue/KT-54390) K2: ClassId for local classes do not match with specification
- [`KT-61277`](https://youtrack.jetbrains.com/issue/KT-61277) K2: Expand the MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES check to other function kinds
- [`KT-61548`](https://youtrack.jetbrains.com/issue/KT-61548) Compiler crashes with StackOverflowError when mapping types
- [`KT-56757`](https://youtrack.jetbrains.com/issue/KT-56757) Drop `IGNORE_BACKEND_K2_LIGHT_TREE` directive
- [`KT-61330`](https://youtrack.jetbrains.com/issue/KT-61330) K2: No BinarySourceElement for system libraries
- [`KT-61166`](https://youtrack.jetbrains.com/issue/KT-61166) Inherited platform declaration clash & accidental override
- [`KT-58764`](https://youtrack.jetbrains.com/issue/KT-58764) [K2] Make `FirResolvedDeclarationStatus.modality` not nullable
- [`KT-61576`](https://youtrack.jetbrains.com/issue/KT-61576) [FIR] Private type alias for public class constructor is always visible
- [`KT-46031`](https://youtrack.jetbrains.com/issue/KT-46031) False negative SEALED_INHERITOR_IN_DIFFERENT_MODULE in bamboo HMPP hierarchy
- [`KT-59804`](https://youtrack.jetbrains.com/issue/KT-59804) K2: Repeat the `SealedInheritorInSameModuleChecker` HMPP logic
- [`KT-59900`](https://youtrack.jetbrains.com/issue/KT-59900) K2: Disappeared NESTED_CLASS_NOT_ALLOWED
- [`KT-61067`](https://youtrack.jetbrains.com/issue/KT-61067) K2. No `Assignments are not expressions`
- [`KT-61144`](https://youtrack.jetbrains.com/issue/KT-61144) FIR2IR: Fix field access for class context receiver from debugger evaluator in K2
- [`KT-59914`](https://youtrack.jetbrains.com/issue/KT-59914) K2: Disappeared RETURN_NOT_ALLOWED
- [`KT-60136`](https://youtrack.jetbrains.com/issue/KT-60136) Wrong IR is generated for spread call in annotation call when annotation has a vararg parameter
- [`KT-56872`](https://youtrack.jetbrains.com/issue/KT-56872) K2: not all reassignments, operator assignments, increments, decrements are tracked in DFA for try/catch expressions
- [`KT-60397`](https://youtrack.jetbrains.com/issue/KT-60397) K2/MPP: don't perform enhancement twice when Java method is called from different modules
- [`KT-61640`](https://youtrack.jetbrains.com/issue/KT-61640) K2: Share declarations from JvmMappedScope between sessions in MPP scenario
- [`KT-59051`](https://youtrack.jetbrains.com/issue/KT-59051) "ISE: IrSimpleFunctionSymbolImpl is already bound" when implementing multiple interfaces by delegation where one of them overrides equals/hashCode
- [`KT-60380`](https://youtrack.jetbrains.com/issue/KT-60380) K2: IAE: class org.jetbrains.kotlin.psi.KtLambdaArgument is not a subtype of class org.jetbrains.kotlin.psi.KtExpression for factory TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM
- [`KT-60795`](https://youtrack.jetbrains.com/issue/KT-60795) K2: missing INCOMPATIBLE_CLASS and corresponding CLI error
- [`KT-59650`](https://youtrack.jetbrains.com/issue/KT-59650) K2: Get rid of `FirNoReceiverExpression`
- [`KT-60555`](https://youtrack.jetbrains.com/issue/KT-60555) K2. FirJavaClass source field is null
- [`KT-61045`](https://youtrack.jetbrains.com/issue/KT-61045) K2: Missing return from DELEGATED_PROPERTY_ACCESSOR setter
- [`KT-60636`](https://youtrack.jetbrains.com/issue/KT-60636) KMP: K2 handling of actual typealiases to nullable types
- [`KT-59815`](https://youtrack.jetbrains.com/issue/KT-59815) K2: Avoid recomputing `argumentVariables`
- [`KT-61409`](https://youtrack.jetbrains.com/issue/KT-61409) Kotlin/Native: crash in kmm-production-sample (compose-app) with escape analysis enabled
- [`KT-61348`](https://youtrack.jetbrains.com/issue/KT-61348) K2: Refactor FIR2IR declaration storages
- [`KT-54905`](https://youtrack.jetbrains.com/issue/KT-54905) KLIB check on compiled with pre-release version
- [`KT-61249`](https://youtrack.jetbrains.com/issue/KT-61249) Move fir-related code from backend.native module
- [`KT-59478`](https://youtrack.jetbrains.com/issue/KT-59478) K2: StackOverflowError on invalid code with nullable unresolved
- [`KT-59893`](https://youtrack.jetbrains.com/issue/KT-59893) K2: Disappeared WRONG_NUMBER_OF_TYPE_ARGUMENTS
- [`KT-60450`](https://youtrack.jetbrains.com/issue/KT-60450) K2: IOOBE from analyzeAndGetLambdaReturnArguments
- [`KT-57076`](https://youtrack.jetbrains.com/issue/KT-57076) K2 does not report 'More than one overridden descriptor declares a default value'
- [`KT-55672`](https://youtrack.jetbrains.com/issue/KT-55672) K2. Operator name message instead of "Unresolved reference" when operator isn't defined for type
- [`KT-61454`](https://youtrack.jetbrains.com/issue/KT-61454) K1: False positive WRONG_NUMBER_OF_TYPE_ARGUMENTS when typealias is LHS of class literal
- [`KT-60252`](https://youtrack.jetbrains.com/issue/KT-60252) K2: Supertype argument is not substituted in fake override receivers and value parameters
- [`KT-60687`](https://youtrack.jetbrains.com/issue/KT-60687) K2: Introduced UNEXPECTED_SAFE_CALL
- [`KT-61312`](https://youtrack.jetbrains.com/issue/KT-61312) K2: Remove FirExpression.typeRef completely when Compose was migrated
- [`KT-60602`](https://youtrack.jetbrains.com/issue/KT-60602) Fix scripting tests in 2.0 branch
- [`KT-60771`](https://youtrack.jetbrains.com/issue/KT-60771) K2: "Conflicting declarations". Unable to re-declare variable if the first one comes from a destructured element
- [`KT-60760`](https://youtrack.jetbrains.com/issue/KT-60760) K2: Every FirFunctionCall has an implicit type reference which points to the return type declaration
- [`KT-59944`](https://youtrack.jetbrains.com/issue/KT-59944) K2: Disappeared NON_MEMBER_FUNCTION_NO_BODY
- [`KT-60936`](https://youtrack.jetbrains.com/issue/KT-60936) KMP: check annotations compatibility on members inside expect and actual class scopes
- [`KT-60668`](https://youtrack.jetbrains.com/issue/KT-60668) KMP: check expect and actual annotations match when actual method is fake override
- [`KT-60250`](https://youtrack.jetbrains.com/issue/KT-60250) K2: origin is set too many times for elvis operator
- [`KT-60254`](https://youtrack.jetbrains.com/issue/KT-60254) K2: Extra unset type argument on Java field reference
- [`KT-60245`](https://youtrack.jetbrains.com/issue/KT-60245) K2: Extra return is generated in always throwing function
- [`KT-59407`](https://youtrack.jetbrains.com/issue/KT-59407) K2: Missing MISSING_CONSTRUCTOR_KEYWORD
- [`KT-57681`](https://youtrack.jetbrains.com/issue/KT-57681) Request review for all FIR diagnostic messages
- [`KT-60885`](https://youtrack.jetbrains.com/issue/KT-60885) K2: Fix `testSelfUpperBoundInference` test in LV 2.0 branch
- [`KT-59957`](https://youtrack.jetbrains.com/issue/KT-59957) K2: Missing UNSUPPORTED_SEALED_FUN_INTERFACE
- [`KT-60000`](https://youtrack.jetbrains.com/issue/KT-60000) K2: Missing UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION
- [`KT-60886`](https://youtrack.jetbrains.com/issue/KT-60886) K2: Fix `testDirectoryWithRelativePath` in LV 2.0 branch
- [`KT-59419`](https://youtrack.jetbrains.com/issue/KT-59419) K2: Missing MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE
- [`KT-59748`](https://youtrack.jetbrains.com/issue/KT-59748) K2: Return type mismatch: expected Unit, actual Any? for when with an assignment in branch
- [`KT-60297`](https://youtrack.jetbrains.com/issue/KT-60297) K2: finally block is not coerced to unit
- [`KT-59860`](https://youtrack.jetbrains.com/issue/KT-59860) [FIR] False-positive `UNEXPECTED_SAFE_CALL`
- [`KT-46794`](https://youtrack.jetbrains.com/issue/KT-46794) Contract not working with extension function in class
- [`KT-59101`](https://youtrack.jetbrains.com/issue/KT-59101) Contract not smartcasting for private extension functions inside class
- [`KT-59387`](https://youtrack.jetbrains.com/issue/KT-59387) K2: Missing NO_CONSTRUCTOR
- [`KT-22499`](https://youtrack.jetbrains.com/issue/KT-22499) Missing error on 'x == y' for different numeric types inferred from smart casts
- [`KT-56867`](https://youtrack.jetbrains.com/issue/KT-56867) Green in K1 -> red in K2 for unsound code. `catch_end` to `good_finally` data flow
- [`KT-57526`](https://youtrack.jetbrains.com/issue/KT-57526) K1: "NullPointerException: Cannot invoke "com.intellij.psi.PsiElement.getParent()" because "current" is null" with label
- [`KT-46383`](https://youtrack.jetbrains.com/issue/KT-46383) EQUALITY_NOT_APPLICABLE is not taking smart cast into consideration in `if` block
- [`KT-32575`](https://youtrack.jetbrains.com/issue/KT-32575) Bound smartcasts in contracts
- [`KT-58331`](https://youtrack.jetbrains.com/issue/KT-58331) Erroneous suspend conversion on anonymous function should not affect call resolution
- [`KT-37591`](https://youtrack.jetbrains.com/issue/KT-37591) Deprecate cases in FE 1.0 when companion property is prioritized against enum entry
- [`KT-53210`](https://youtrack.jetbrains.com/issue/KT-53210) OVERLOAD_RESOLUTION_AMBIGUITY when lambda with single argument `it` is involved
- [`KT-51796`](https://youtrack.jetbrains.com/issue/KT-51796) False positive smart cast after safe call to contract function with nullable receiver
- [`KT-52782`](https://youtrack.jetbrains.com/issue/KT-52782) Receiver type mismatch error due to ProperTypeInferenceConstraintsProcessing compiler feature
- [`KT-57308`](https://youtrack.jetbrains.com/issue/KT-57308) Incorrect property type inference after contracted smart cast of generic type
- [`KT-18130`](https://youtrack.jetbrains.com/issue/KT-18130) Smart cast can be broken by expression in string template
- [`KT-21915`](https://youtrack.jetbrains.com/issue/KT-21915) Generic parameter of a reference gets wrongly smart-casted after a cast
- [`KT-22454`](https://youtrack.jetbrains.com/issue/KT-22454) Unsound smartcast in nested loops with labeled break from while-true
- [`KT-17694`](https://youtrack.jetbrains.com/issue/KT-17694) Smart cast impossible on var declared in init block with a secondary constructor
- [`KT-47895`](https://youtrack.jetbrains.com/issue/KT-47895) NullPointerException in `PSICallResolver.resolveToDeprecatedMod` with incorrect loop range
- [`KT-47378`](https://youtrack.jetbrains.com/issue/KT-47378) Missed FUNCTION_CALL_EXPECTED diagnostic on wrong code with callable reference
- [`KT-43408`](https://youtrack.jetbrains.com/issue/KT-43408) False positive CAPTURED_VAL_INITIALIZATION on crossinline val property initialization with EXACTLY_ONCE lambda call from the init block
- [`KT-35565`](https://youtrack.jetbrains.com/issue/KT-35565) False negative UNINITIALIZED_VARIABLE, VAL_REASSIGNMENT, and INVISIBLE_SETTER errors in unreachable code block
- [`KT-10420`](https://youtrack.jetbrains.com/issue/KT-10420) Shadowed variable declaration in inner function makes compiler behave strange
- [`KT-49881`](https://youtrack.jetbrains.com/issue/KT-49881) "AssertionError: Base expression was not processed: POSTFIX_EXPRESSION" when analyzing dangling [bracketed] expression with postfix
- [`KT-53847`](https://youtrack.jetbrains.com/issue/KT-53847) Missed USAGE_IS_NOT_INLINABLE when using runCatching with the inline function's functional argument as a receiver
- [`KT-53802`](https://youtrack.jetbrains.com/issue/KT-53802) No smartcast after a while (true) infinite loop with break
- [`KT-27754`](https://youtrack.jetbrains.com/issue/KT-27754) Stack Overflow Error  in pseudocode analysis
- [`KT-41131`](https://youtrack.jetbrains.com/issue/KT-41131) Error: java.lang.AssertionError: Rewrite at slice LEAKING_THIS when invoking non final constructor property in init block
- [`KT-42962`](https://youtrack.jetbrains.com/issue/KT-42962) False positive "ACCIDENTAL_OVERRIDE" when field name annotated with `@JvmField` conflicts with getter/setter from Java
- [`KT-49507`](https://youtrack.jetbrains.com/issue/KT-49507) JVM: "IllegalAccessError: class X tried to access private field" with same-named Kotlin property and Java base class field
- [`KT-35752`](https://youtrack.jetbrains.com/issue/KT-35752) "AE: Recursion detected in a lazy value" with type alias and inner class from another module
- [`KT-28333`](https://youtrack.jetbrains.com/issue/KT-28333) Smartcast is wrong if while(true) and break as a part of expression is used (possible NPE)
- [`KT-28489`](https://youtrack.jetbrains.com/issue/KT-28489) Smartcast is wrong if not-null assertion in while condition + break to the parent while is used (produces NPE)
- [`KT-28369`](https://youtrack.jetbrains.com/issue/KT-28369) Var not-null smartcasts are wrong if reassignments are used inside another expressions
- [`KT-26612`](https://youtrack.jetbrains.com/issue/KT-26612) Smartcast don't work in not-null checks + NotNull contract
- [`KT-7676`](https://youtrack.jetbrains.com/issue/KT-7676) Redundant cast of var is not redundant?
- [`KT-51984`](https://youtrack.jetbrains.com/issue/KT-51984) Cannot use `x == null` when Java class X declares equals(`@NonNull`)
- [`KT-56249`](https://youtrack.jetbrains.com/issue/KT-56249) No method equals for HttpMethod in Spring Boot 3
- [`KT-56264`](https://youtrack.jetbrains.com/issue/KT-56264) incorrect type inference/smart cast for exhaustive try catch
- [`KT-24565`](https://youtrack.jetbrains.com/issue/KT-24565) Incorrect floating point comparisons in constant expressions
- [`KT-54333`](https://youtrack.jetbrains.com/issue/KT-54333) False positive CONST_VAL_WITH_NON_CONST_INITIALIZER on negative literals in const vals
- [`KT-53447`](https://youtrack.jetbrains.com/issue/KT-53447) Leaking/unrefined types from main source set when main/test use different library versions
- [`KT-35981`](https://youtrack.jetbrains.com/issue/KT-35981) No smart cast and UNSAFE_CALL error when using not() function instead of inverse operator
- [`KT-33132`](https://youtrack.jetbrains.com/issue/KT-33132) Cannot override the equals operator twice (in a class and its subclass) unless omitting the operator keyword in the subclass
- [`KT-55335`](https://youtrack.jetbrains.com/issue/KT-55335) Don't report SUPERTYPE_NOT_INITIALIZED for annotation supertype, because FINAL_SUPERTYPE is already reported
- [`KT-27936`](https://youtrack.jetbrains.com/issue/KT-27936) Write InnerClasses attribute for all class names used in a class file
- [`KT-53261`](https://youtrack.jetbrains.com/issue/KT-53261) Evaluate effect from <T-unbox> inline for primitive types
- [`KT-31367`](https://youtrack.jetbrains.com/issue/KT-31367) IllegalStateException: Concrete fake override public open fun (...)  defined in TheIssue[PropertyGetterDescriptorImpl`@1a03c376`] should have exactly one concrete super-declaration: []

### Compose Compiler

#### New features
- [13b27eb](https://github.com/JetBrains/kotlin/commit/13b27eb8120c67bc0f52bccd451103ea6fed36b6) Strong skipping is no longer considered experimental and is safe for use in production. It will become the default behavior in an upcoming release.

#### Bug fixes
- [868d0ac](https://github.com/JetBrains/kotlin/commit/868d0acf6bd5e550ae84428a6b62f9f6e1c2c633) Ensure that inline body is realized when source information is off [b/338179884](https://issuetracker.google.com/issue/338179884)
- [8a6f64a](https://github.com/JetBrains/kotlin/commit/8a6f64aad528983fc937df9075a3a120c770a67b) Generate binary compat stubs for nullable value classes [b/335384193](https://issuetracker.google.com/issue/335384193)
- [154d479](https://github.com/JetBrains/kotlin/commit/154d47964f1d9b569eb38f9458d890dc9cabd04f) Make sure a composable call does not escape composable lambda [b/331365999](https://issuetracker.google.com/issue/331365999)
- [53f4f37](https://github.com/JetBrains/kotlin/commit/53f4f37287b5845ffb440b41b833386440482258) Make parameter types for inline classes nullable when underlying type is not primitive [b/330655412](https://issuetracker.google.com/issue/330655412)

### Docs & Examples

#### New Features

- [`KT-66958`](https://youtrack.jetbrains.com/issue/KT-66958) [Docs][JVM] Add info about generating lambda functions like the Java compiler by default

#### Fixes

- [`KT-63618`](https://youtrack.jetbrains.com/issue/KT-63618) [Docs] Create documentation for Kotlin power-assert compiler plugin
- [`KT-67902`](https://youtrack.jetbrains.com/issue/KT-67902) [Docs][Wasm] K/Wasm: support new version of exception handling proposal
- [`KT-67944`](https://youtrack.jetbrains.com/issue/KT-67944) [Docs][K2][IDE] Update IDE support description for K2
- [`KT-67865`](https://youtrack.jetbrains.com/issue/KT-67865) [Docs][K2] update Kotlin Release Page
- [`KT-66957`](https://youtrack.jetbrains.com/issue/KT-66957) [Docs] [Gradle] Build reports are Stable
- [`KT-67936`](https://youtrack.jetbrains.com/issue/KT-67936) [Docs][Build tools] Update KGP variants
- [`KT-67508`](https://youtrack.jetbrains.com/issue/KT-67508) [Docs] Talk about the new Compose Gradle plugin
- [`KT-67347`](https://youtrack.jetbrains.com/issue/KT-67347) Remove docs on dropped K/JS feature "Ignoring compilation errors"
- [`KT-64710`](https://youtrack.jetbrains.com/issue/KT-64710) [Docs] Update What's new for 2.0.0-BetaX
- [`KT-63001`](https://youtrack.jetbrains.com/issue/KT-63001) K2: Organize team-wide talks about new FIR2IR & PCLA
- [`KT-6259`](https://youtrack.jetbrains.com/issue/KT-6259) Docs: add information about default constructor for class

### IDE

- [`KT-50241`](https://youtrack.jetbrains.com/issue/KT-50241) Make Symbol Light Classes consistent with Ultra Light Classes
- [`KT-60318`](https://youtrack.jetbrains.com/issue/KT-60318) K2: disable SLC for non-JVM platforms
- [`KT-56546`](https://youtrack.jetbrains.com/issue/KT-56546) LL FIR: fix lazy resolve contract violation in Symbol Light Classes
- [`KT-55788`](https://youtrack.jetbrains.com/issue/KT-55788) [SLC] Declarations with value classes are leaked into light classes
- [`KT-61195`](https://youtrack.jetbrains.com/issue/KT-61195) UAST modeling of implicit `it` is inconsistent for `Enum.entries`
- [`KT-62757`](https://youtrack.jetbrains.com/issue/KT-62757) SLC: incorrect nullability annotation on aliased type
- [`KT-62440`](https://youtrack.jetbrains.com/issue/KT-62440) On the fly resolve with light method context doesn't resolve method type parameters
- [`KT-57550`](https://youtrack.jetbrains.com/issue/KT-57550) K2: AA: incorrect constant value in file-level annotation
- [`KT-61460`](https://youtrack.jetbrains.com/issue/KT-61460) SLC: unnecessary upper bound wildcards (w/ type alias)
- [`KT-61377`](https://youtrack.jetbrains.com/issue/KT-61377) K2: SLC: wrong retention counterpart for AnnotationRetention.BINARY

### IDE. Gradle Integration

- [`KT-65617`](https://youtrack.jetbrains.com/issue/KT-65617) K/N project import fails if ~/.konan dir is empty
- [`KT-45775`](https://youtrack.jetbrains.com/issue/KT-45775) Improve quality of Import

### IDE. JS

- [`KT-61257`](https://youtrack.jetbrains.com/issue/KT-61257) Analysis API:"KotlinIllegalArgumentExceptionWithAttachments: Invalid FirDeclarationOrigin DynamicScope" exception on unsupported JS dynamic usage in scope

### IDE. Multiplatform

- [`KT-45513`](https://youtrack.jetbrains.com/issue/KT-45513) Run c-interop generation in parallel during project import
- [`KT-63007`](https://youtrack.jetbrains.com/issue/KT-63007) K2: Analysis API Standalone: klibs are not resovled from common code
- [`KT-63126`](https://youtrack.jetbrains.com/issue/KT-63126) K2: Analysis API Standalone: IllegalStateException from Kotlin/Native klib
- [`KT-61520`](https://youtrack.jetbrains.com/issue/KT-61520) Sources.jar is not imported for common and intermediate source-sets from the MPP library

### IDE. Script

- [`KT-61267`](https://youtrack.jetbrains.com/issue/KT-61267) K2 Scripts: dependency issues
- [`KT-60418`](https://youtrack.jetbrains.com/issue/KT-60418) K2 scripting: highlighting sometimes fails
- [`KT-60987`](https://youtrack.jetbrains.com/issue/KT-60987) K2: Analysis API: make build.gradle.kts resolution work on build scripts from kotlin projects

### IR. Actualizer

#### Fixes

- [`KT-67488`](https://youtrack.jetbrains.com/issue/KT-67488) K2: AssertionError No such value argument slot in IrConstructorCallImpl: 0 (total=0
- [`KT-60847`](https://youtrack.jetbrains.com/issue/KT-60847) K2: Fake overrides are incorrect after actualization
- [`KT-65274`](https://youtrack.jetbrains.com/issue/KT-65274) IrFakeOverrideBuilder: ISE: "IrFieldPublicSymbolImpl is already bound"
- [`KT-63756`](https://youtrack.jetbrains.com/issue/KT-63756) K2: "AssertionError: No such value argument slot in IrConstructorCallImpl" caused by actual typealias for annotation with default parameter
- [`KT-65236`](https://youtrack.jetbrains.com/issue/KT-65236) IrFakeOverrideBuilder: ISE: "should not be called"
- [`KT-65116`](https://youtrack.jetbrains.com/issue/KT-65116) K2: IrFakeOverrideBuilder: "No override for FUN"  if the function has already been overridden by another class in K <- J<- K <- J hierarchy
- [`KT-65499`](https://youtrack.jetbrains.com/issue/KT-65499) IrFakeOverrideBuilder: ISE  IrSimpleFunctionPublicSymbolImpl is already bound for irrelevant 'remove' clashing with a function from Java collection subclass
- [`KT-64150`](https://youtrack.jetbrains.com/issue/KT-64150) IrFakeOverrideBuilder: Fake overrides for static java functions are not generated
- [`KT-65432`](https://youtrack.jetbrains.com/issue/KT-65432) IrFakeOverrideBuilder -  No override for FUN IR_EXTERNAL_JAVA_DECLARATION_STUB name:elementData
- [`KT-64895`](https://youtrack.jetbrains.com/issue/KT-64895) K2:IrActualizer corrupts attributeOwnerId value
- [`KT-58861`](https://youtrack.jetbrains.com/issue/KT-58861) K2: Improve the new pipeline of FIR2IR conversion, IR actualization and fake-override generation
- [`KT-64835`](https://youtrack.jetbrains.com/issue/KT-64835) K2: K/JS: Expect declaration is incompatible errors in the K2 QG
- [`KT-63347`](https://youtrack.jetbrains.com/issue/KT-63347) K2: Fix overridden symbols inside LazyDeclarations
- [`KT-62535`](https://youtrack.jetbrains.com/issue/KT-62535) K2: FakeOverrideRebuilder can't handle f/o without overridden symbols
- [`KT-62292`](https://youtrack.jetbrains.com/issue/KT-62292) K2: Extract IrActualizer into separate module
- [`KT-63442`](https://youtrack.jetbrains.com/issue/KT-63442) IrFakeOverrideBuilder: ISE "Multiple overrides" error when function signatures differ only in the type parameter upper bound
- [`KT-62623`](https://youtrack.jetbrains.com/issue/KT-62623) K2: Ir actualizer leaves inconsistent module links from files

### IR. Inlining

- [`KT-66017`](https://youtrack.jetbrains.com/issue/KT-66017) K2 / Native: "NoSuchElementException: Sequence contains no element matching the predicate" on building native release binaries
- [`KT-64868`](https://youtrack.jetbrains.com/issue/KT-64868) [K/N] Inlined assert is later not removed, even without `-ea`
- [`KT-64807`](https://youtrack.jetbrains.com/issue/KT-64807) Refactor InlineFunctionResolver
- [`KT-64806`](https://youtrack.jetbrains.com/issue/KT-64806) Move FunctionInlining to separate module

### IR. Interpreter

- [`KT-64079`](https://youtrack.jetbrains.com/issue/KT-64079) Native library evolution behaviour for constants
- [`KT-62683`](https://youtrack.jetbrains.com/issue/KT-62683) K2: FIR2IR: IrConst*Transformer doesn't evaluate an expression for const val initializer

### IR. Tree

- [`KT-66152`](https://youtrack.jetbrains.com/issue/KT-66152) IrFakeOverrideBuilder: AssertionError "different length of type parameter lists"
- [`KT-65971`](https://youtrack.jetbrains.com/issue/KT-65971) K2: Investigate diagnostic test failures with IrFakeOverrideBuilder
- [`KT-64974`](https://youtrack.jetbrains.com/issue/KT-64974) Consolidate visibility checks in IrFakeOverrideBuilder
- [`KT-61360`](https://youtrack.jetbrains.com/issue/KT-61360) Fix essential problems in IrFakeOverrideBuilder
- [`KT-61970`](https://youtrack.jetbrains.com/issue/KT-61970) Refactor IR and FIR tree generators to reuse common logic
- [`KT-61703`](https://youtrack.jetbrains.com/issue/KT-61703) Drop the dependency on kotlinpoet for IR tree generation
- [`KT-63437`](https://youtrack.jetbrains.com/issue/KT-63437) IrFakeOverrideBuilder: ISE "Captured Type does not have a classifier" on complex Java hierarchy
- [`KT-61934`](https://youtrack.jetbrains.com/issue/KT-61934) Decouple building fake overrides from symbol table and build scheduling
- [`KT-60923`](https://youtrack.jetbrains.com/issue/KT-60923) IR: Mark IrSymbol.owner with OptIn

### JavaScript

#### New Features

- [`KT-56206`](https://youtrack.jetbrains.com/issue/KT-56206) KJS / Reflection: add KClass.createInstance
- [`KT-44871`](https://youtrack.jetbrains.com/issue/KT-44871) Add `@JsExport` and `@JsName` annotations to stdlib classes (especially collections) to avoid method name mangling and improve Kotlin usability from JS
- [`KT-8373`](https://youtrack.jetbrains.com/issue/KT-8373) JS: support ES6 as compilation target
- [`KT-65168`](https://youtrack.jetbrains.com/issue/KT-65168) Introduce an ability to create type-safe JS objects
- [`KT-45604`](https://youtrack.jetbrains.com/issue/KT-45604) KJS / IR: Use `globalThis` instead of top level `this`

#### Fixes

- [`KT-66922`](https://youtrack.jetbrains.com/issue/KT-66922) K2 JS: Intrinsic Float/Double toString producing wrong numbers
- [`KT-64135`](https://youtrack.jetbrains.com/issue/KT-64135) K2 / KJS: Incorrect value class support when used with inline fun
- [`KT-67978`](https://youtrack.jetbrains.com/issue/KT-67978) K2: Declaration of such kind (expect) cannot be exported to JavaScript
- [`KT-64951`](https://youtrack.jetbrains.com/issue/KT-64951) Kotlin-Multiplatform does not allow JSExport of expect
- [`KT-63038`](https://youtrack.jetbrains.com/issue/KT-63038) Compilation of suspend functions into ES2015 generators
- [`KT-16981`](https://youtrack.jetbrains.com/issue/KT-16981) js: Command line arguments passed to `main()` are always empty
- [`KT-34995`](https://youtrack.jetbrains.com/issue/KT-34995) JS: List, Map, and Set types are hard to use from JS because of mangled member names
- [`KT-51225`](https://youtrack.jetbrains.com/issue/KT-51225) JS IR & Wasm: using nested expect enum entry in a default argument fails
- [`KT-63907`](https://youtrack.jetbrains.com/issue/KT-63907) KJS: default parameters in interfaces are lost in implementations
- [`KT-64708`](https://youtrack.jetbrains.com/issue/KT-64708) KJS: exported interfaces missing __doNotUseOrImplementIt when extending from external types
- [`KT-62806`](https://youtrack.jetbrains.com/issue/KT-62806) KJS: Type mismatch on inferred return type with Nothing
- [`KT-64421`](https://youtrack.jetbrains.com/issue/KT-64421) K2: Implement IrJsTypeScriptExportTestGenerated for K2
- [`KT-61526`](https://youtrack.jetbrains.com/issue/KT-61526) KJS: Compiled files clash with the new per-file granularity
- [`KT-63359`](https://youtrack.jetbrains.com/issue/KT-63359) K2: support new ways to declare TestResult in JS TestGenerator lowering
- [`KT-61929`](https://youtrack.jetbrains.com/issue/KT-61929) KJS: "IllegalStateException: No dispatch receiver parameter for FUN LOCAL_FUNCTION_FOR_LAMBDA" caused by `run` function in init block
- [`KT-65216`](https://youtrack.jetbrains.com/issue/KT-65216) K2 JS: False positive JS_NAME_CLASH diagnostic on generic interface
- [`KT-64548`](https://youtrack.jetbrains.com/issue/KT-64548) KJS / K2: "Cannot find delegated constructor call" caused by external classes constructors
- [`KT-64867`](https://youtrack.jetbrains.com/issue/KT-64867) K2 JS: Name clash between constructors with same JsName but in different classes
- [`KT-64463`](https://youtrack.jetbrains.com/issue/KT-64463) KJS / K2: "Name contains illegal chars that cannot appear in JavaScript identifier" caused by non-ASCII character
- [`KT-64451`](https://youtrack.jetbrains.com/issue/KT-64451) K2: Implement MultiModuleOrderTestGenerated for K2
- [`KT-64450`](https://youtrack.jetbrains.com/issue/KT-64450) K2: Implement SourceMapGenerationSmokeTestGenerated for K2
- [`KT-64366`](https://youtrack.jetbrains.com/issue/KT-64366) KJS / K2: Exported declaration uses non-exportable return type: 'kotlin.<X>?'
- [`KT-64426`](https://youtrack.jetbrains.com/issue/KT-64426) K2: Implement JsIrLineNumberTestGenerated for K2
- [`KT-64422`](https://youtrack.jetbrains.com/issue/KT-64422) K2: Implement IrJsSteppingTestGenerated for K2
- [`KT-64364`](https://youtrack.jetbrains.com/issue/KT-64364) K2 / KJS: `@JSExports` generates clashing declarations for companion objects that extends its own class
- [`KT-64445`](https://youtrack.jetbrains.com/issue/KT-64445) K2: Implement **VersionChangedTestGenerated for K2
- [`KT-64446`](https://youtrack.jetbrains.com/issue/KT-64446) K2: Implement JsIrInvalidationPerFileWithPLTestGenerated for K2
- [`KT-64423`](https://youtrack.jetbrains.com/issue/KT-64423) K2: Implement JsIrES6InvalidationPerFileTestGenerated for K2
- [`KT-63543`](https://youtrack.jetbrains.com/issue/KT-63543) KJS / K2: Exported declaration uses non-exportable return type type: 'kotlin.Unit'
- [`KT-61596`](https://youtrack.jetbrains.com/issue/KT-61596) K2 JS: support reporting PRE_RELEASE_CLASS
- [`KT-61117`](https://youtrack.jetbrains.com/issue/KT-61117) Migrate remaining legacy IC tests to IR
- [`KT-61523`](https://youtrack.jetbrains.com/issue/KT-61523) KJS: Call main function in per-file mode
- [`KT-63089`](https://youtrack.jetbrains.com/issue/KT-63089) KJS / K2 : "IllegalArgumentException: source must not be null " for inner class and interface as type
- [`KT-56818`](https://youtrack.jetbrains.com/issue/KT-56818) KJS: "TypeError: Class constructor * cannot be invoked without 'new'" when extending external class
- [`KT-62077`](https://youtrack.jetbrains.com/issue/KT-62077) KJS: TypeError: str.charCodeAt is not a function
- [`KT-63436`](https://youtrack.jetbrains.com/issue/KT-63436) K/JS: Eliminate names for synthetic classes in setMetadataFor()
- [`KT-63013`](https://youtrack.jetbrains.com/issue/KT-63013) KJS: `requireNotNull` not working correctly in JS tests with Kotlin 1.9.20
- [`KT-61525`](https://youtrack.jetbrains.com/issue/KT-61525) KJS: Test functions are not invoked in per-file mode
- [`KT-62425`](https://youtrack.jetbrains.com/issue/KT-62425) K/JS: Implement K2 and K1 diagnostics for checking argument passing to js()
- [`KT-61524`](https://youtrack.jetbrains.com/issue/KT-61524) KJS: Eager initialization doesn't work in per-file mode
- [`KT-61862`](https://youtrack.jetbrains.com/issue/KT-61862) KJS: Can't create kotlin.js.Promise inheritor
- [`KT-61710`](https://youtrack.jetbrains.com/issue/KT-61710) K/JS: Implement JS_NAME_CLASH check for top level declarations
- [`KT-61886`](https://youtrack.jetbrains.com/issue/KT-61886) K/JS: Prepare K/JS tests for JS IR BE diagnostics
- [`KT-60829`](https://youtrack.jetbrains.com/issue/KT-60829) Fix JS Incremental tests in 2.0 branch
- [`KT-60635`](https://youtrack.jetbrains.com/issue/KT-60635) K/JS: Class internal methods may clash with child methods from other module that have the same name
- [`KT-60846`](https://youtrack.jetbrains.com/issue/KT-60846) Fix `IncrementalJsKlibCompilerWithScopeExpansionRunnerTestGenerated` test in 2.0 branch

### KMM Plugin

- [`KT-59270`](https://youtrack.jetbrains.com/issue/KT-59270) Update wizards in KMM AS plugin after 1.9.20 release
- [`KT-60169`](https://youtrack.jetbrains.com/issue/KT-60169) Generate gradle version catalog in KMM AS plugin
- [`KT-59269`](https://youtrack.jetbrains.com/issue/KT-59269) Update wizards in KMM AS plugin after 1.9.0 release

### Klibs

#### New Features

- [`KT-66367`](https://youtrack.jetbrains.com/issue/KT-66367) KLib ABI dump: support wasm_target manifest attribute
- [`KT-65442`](https://youtrack.jetbrains.com/issue/KT-65442) [klibs] header klibs: keep internal declarations and declarations inside inlines
- [`KT-62213`](https://youtrack.jetbrains.com/issue/KT-62213) [klibs] header klibs should keep private interfaces
- [`KT-62259`](https://youtrack.jetbrains.com/issue/KT-62259) KLIB ABI reader: add information about a backing field to AbiProperty
- [`KT-62341`](https://youtrack.jetbrains.com/issue/KT-62341) [KLIB tool] Dump declared & imported signatures by IR (not metadata)
- [`KT-60807`](https://youtrack.jetbrains.com/issue/KT-60807) [klib] Add an option to write out header klibs

#### Fixes

- [`KT-67401`](https://youtrack.jetbrains.com/issue/KT-67401) KLib ABI dump: write plain targets in the manifest
- [`KT-66970`](https://youtrack.jetbrains.com/issue/KT-66970) K2: "IrLinkageError: Function * can not be called" when calling `@JvmStatic` functions in Native test
- [`KT-64440`](https://youtrack.jetbrains.com/issue/KT-64440) K2: Port KotlinKlibSerializerTest to K2
- [`KT-66921`](https://youtrack.jetbrains.com/issue/KT-66921) K/JS backend doesn't report "/ by zero" and fails with const val property must have a const initializer
- [`KT-66611`](https://youtrack.jetbrains.com/issue/KT-66611) Check, that no bad IR is produced, when we failed to compute constant default value in constant context
- [`KT-33411`](https://youtrack.jetbrains.com/issue/KT-33411) Kotlin/Native crashes if several libraries have declarations with the same FQ name
- [`KT-44626`](https://youtrack.jetbrains.com/issue/KT-44626) Umbrella issue: different kinds of klib IR linker error messages
- [`KT-64452`](https://youtrack.jetbrains.com/issue/KT-64452) K2: Port FilePathsInKlibTest to K2
- [`KT-64395`](https://youtrack.jetbrains.com/issue/KT-64395) API for ABI: Add a check for the file's existence to KLIB ABI Reader
- [`KT-61143`](https://youtrack.jetbrains.com/issue/KT-61143) [klib tool] Dump IR with unbound symbols
- [`KT-65723`](https://youtrack.jetbrains.com/issue/KT-65723) K2: Signature clash diagnostic fails for parametrized function with Unsupported pair of descriptors
- [`KT-65063`](https://youtrack.jetbrains.com/issue/KT-65063) Clashing KLIB signatures from different modules result in an exception
- [`KT-64085`](https://youtrack.jetbrains.com/issue/KT-64085) Different klib signatures for K1/K2 for overridden properties assigned in init block
- [`KT-63573`](https://youtrack.jetbrains.com/issue/KT-63573) K2: Dependency problems with dependencies with same artifact id
- [`KT-64082`](https://youtrack.jetbrains.com/issue/KT-64082) Different klib signatures in K1/K2 for the same locally used constant declaration
- [`KT-63931`](https://youtrack.jetbrains.com/issue/KT-63931) [K/N] Relative path to klib option of cinterop tool doesn't work
- [`KT-60390`](https://youtrack.jetbrains.com/issue/KT-60390) KLIBs: Wrong IrSymbol is used for deserialized `expect` property's backing field & accessors
- [`KT-61136`](https://youtrack.jetbrains.com/issue/KT-61136) Drop ExpectActualTable + clean-up the relevant code
- [`KT-61767`](https://youtrack.jetbrains.com/issue/KT-61767) [K/N] Header klibs should keep private underlying properties of value classes
- [`KT-61097`](https://youtrack.jetbrains.com/issue/KT-61097) [PL] Don't create an executable if there were errors in PL

### Language Design

#### New Features

- [`KT-64510`](https://youtrack.jetbrains.com/issue/KT-64510) Proceed to next tower level if property setter is invisible in assignment
- [`KT-59553`](https://youtrack.jetbrains.com/issue/KT-59553) K2: Simplify rules for upper bound violated checks for qualifier in LHS of class literal
- [`KT-11272`](https://youtrack.jetbrains.com/issue/KT-11272) Resolve combined index-accessed get and set operators
- [`KT-65682`](https://youtrack.jetbrains.com/issue/KT-65682) Deprecate `header`/`impl` keywords
- [`KT-65965`](https://youtrack.jetbrains.com/issue/KT-65965) KMP: Parameter properties in constructor of external class
- [`KT-57274`](https://youtrack.jetbrains.com/issue/KT-57274) Allow generic argument to have explicit `Nothing` upper bound
- [`KT-1982`](https://youtrack.jetbrains.com/issue/KT-1982) Smart cast to a common supertype of subject types after `||` (OR operator)
- [`KT-65964`](https://youtrack.jetbrains.com/issue/KT-65964) KMP: Private constructor in external classes
- [`KT-37316`](https://youtrack.jetbrains.com/issue/KT-37316) Allow actual classifier to have more permissive visibility than visibility of expect classifier
- [`KT-58616`](https://youtrack.jetbrains.com/issue/KT-58616) KMP: consider relaxing the classifier visibility matching rules
- [`KT-37115`](https://youtrack.jetbrains.com/issue/KT-37115) Smart cast with boolean expressions and early return / throw statements
- [`KT-7186`](https://youtrack.jetbrains.com/issue/KT-7186)  Smart cast for captured variables inside changing closures of inline functions
- [`KT-62138`](https://youtrack.jetbrains.com/issue/KT-62138) K1: false positive (?) NO_SET_METHOD for += resolved as a combination of Map.get and plus

#### Performance Improvements

- [`KT-38101`](https://youtrack.jetbrains.com/issue/KT-38101) Exponential analysis of += calls

#### Fixes

- [`KT-64187`](https://youtrack.jetbrains.com/issue/KT-64187) K2: False positive ABSTRACT_NOT_IMPLEMENTED caused by the fact that common code sees platform code of its dependencies
- [`KT-57290`](https://youtrack.jetbrains.com/issue/KT-57290) Deprecate smart cast on base class property from invisible derived class if base class is from another module
- [`KT-54309`](https://youtrack.jetbrains.com/issue/KT-54309) Deprecate use of a synthetic setter on a projected receiver
- [`KT-61718`](https://youtrack.jetbrains.com/issue/KT-61718) Forbid unsound code with self upper bounds and captured types
- [`KT-54607`](https://youtrack.jetbrains.com/issue/KT-54607) Can't use same function if having multiple instances of same subtype in same `when`-statement
- [`KT-27252`](https://youtrack.jetbrains.com/issue/KT-27252) Smart cast in when on a sealed class depends on the order of "is" checks
- [`KT-57178`](https://youtrack.jetbrains.com/issue/KT-57178) Change inferred type of prefix increment to return type of getter instead of return type of inc() operator
- [`KT-61749`](https://youtrack.jetbrains.com/issue/KT-61749) Forbid unsound bound violation in generic inner class of generic outer class
- [`KT-64342`](https://youtrack.jetbrains.com/issue/KT-64342) SAM conversion of parameter types of callable references leads to CCE
- [`KT-64299`](https://youtrack.jetbrains.com/issue/KT-64299) Companion scope is ignored for resolution of annotations on companion object
- [`KT-66453`](https://youtrack.jetbrains.com/issue/KT-66453) Consistently resolve operator/infix calls like function calls in presence of classifier candidate for receiver
- [`KT-62923`](https://youtrack.jetbrains.com/issue/KT-62923) K2: Introduce PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE for projections of outer super types of inner class
- [`KT-65724`](https://youtrack.jetbrains.com/issue/KT-65724) Propagate data flow information from try block to catch and finally blocks
- [`KT-65750`](https://youtrack.jetbrains.com/issue/KT-65750) Increment and plus operators that change return type must affect smart casts
- [`KT-58881`](https://youtrack.jetbrains.com/issue/KT-58881) K2: Run checkers in common code against platform session
- [`KT-62646`](https://youtrack.jetbrains.com/issue/KT-62646) Decide on the equality compatibility
- [`KT-65775`](https://youtrack.jetbrains.com/issue/KT-65775) K2: Consider prohibiting actual typealias to superclass
- [`KT-65881`](https://youtrack.jetbrains.com/issue/KT-65881) K2: Missing `ITERATOR_MISSING` in `for` loop on object
- [`KT-61340`](https://youtrack.jetbrains.com/issue/KT-61340) K2: Allowed smart cast in common which should be prohibited in platform
- [`KT-51827`](https://youtrack.jetbrains.com/issue/KT-51827) Inconsistent behavior with smartcast and protected members
- [`KT-58589`](https://youtrack.jetbrains.com/issue/KT-58589) Deprecate missed MUST_BE_INITIALIZED when no primary constructor is presented or when class is local
- [`KT-26983`](https://youtrack.jetbrains.com/issue/KT-26983) Gradle buildscript (kotlin-dsl): "Smart cast to 'Foo' is impossible" due to same variable names
- [`KT-62959`](https://youtrack.jetbrains.com/issue/KT-62959) Value of captured type is not a subtype of the same captured type
- [`KT-64828`](https://youtrack.jetbrains.com/issue/KT-64828) Update KEEP for SubclassOptInRequired
- [`KT-64739`](https://youtrack.jetbrains.com/issue/KT-64739) Mark `@SubclassOptInRequired` as an experimental
- [`KT-26044`](https://youtrack.jetbrains.com/issue/KT-26044) When expression is not considered to be exhaustive for empty nullable sealed and enum classes
- [`KT-57422`](https://youtrack.jetbrains.com/issue/KT-57422) K2: Prohibit use-site 'get' targeted annotations on property getters
- [`KT-58921`](https://youtrack.jetbrains.com/issue/KT-58921) K1/K2: difference in Enum.values resolve priority

### Libraries

#### New Features

- [`KT-65532`](https://youtrack.jetbrains.com/issue/KT-65532) Stabilize experimental API for 2.0
- [`KT-60657`](https://youtrack.jetbrains.com/issue/KT-60657) Introduce Common String.toCharArray(destination) in stdlib
- [`KT-57150`](https://youtrack.jetbrains.com/issue/KT-57150) Introduce common protected property AbstractMutableList.modCount
- [`KT-57151`](https://youtrack.jetbrains.com/issue/KT-57151) Introduce common protected function AbstractMutableList.removeRange
- [`KT-66102`](https://youtrack.jetbrains.com/issue/KT-66102) Constructor-like function for creating AutoCloseable instances
- [`KT-59441`](https://youtrack.jetbrains.com/issue/KT-59441) Design reading and writing future versions of Kotlin metadata

#### Performance Improvements

- [`KT-64361`](https://youtrack.jetbrains.com/issue/KT-64361) Optimization opportunity in Int.sign
- [`KT-65590`](https://youtrack.jetbrains.com/issue/KT-65590) Make CharSequence.isBlank idiomatic and improve its performance
- [`KT-61488`](https://youtrack.jetbrains.com/issue/KT-61488) Kotlin/Native stdlib: simplify ArrayList implementation
- [`KT-51058`](https://youtrack.jetbrains.com/issue/KT-51058) Avoid byte array allocation in File.writeText when possible
- [`KT-58588`](https://youtrack.jetbrains.com/issue/KT-58588) Optimizations for sequence functions distinct, flatten

#### Fixes

- [`KT-67397`](https://youtrack.jetbrains.com/issue/KT-67397) Switch remaining org.jetbrains.kotlin libs to K2
- [`KT-61969`](https://youtrack.jetbrains.com/issue/KT-61969) Migrate kotlin-test to the current Kotlin Multiplatform Plugin
- [`KT-60803`](https://youtrack.jetbrains.com/issue/KT-60803) Experimental AutoCloseable 'use' method is not resolved in Java
- [`KT-63156`](https://youtrack.jetbrains.com/issue/KT-63156) Remove all deprecated declarations in kotlinx-metadata-jvm
- [`KT-54879`](https://youtrack.jetbrains.com/issue/KT-54879) Add callsInPlace contract for more functions in stdlib
- [`KT-55777`](https://youtrack.jetbrains.com/issue/KT-55777) Unresolved kotlin.AutoCloseable in JVM
- [`KT-63219`](https://youtrack.jetbrains.com/issue/KT-63219) Change root package and coordinates of kotlinx-metadata-jvm to kotlin.*
- [`KT-65518`](https://youtrack.jetbrains.com/issue/KT-65518) Memory leak in buildMap and in Wasm/Js/Native (Linked)HashMap
- [`KT-65525`](https://youtrack.jetbrains.com/issue/KT-65525) JS: Wrong return value of HashMap.keys.remove
- [`KT-63397`](https://youtrack.jetbrains.com/issue/KT-63397) kotlin-test should declare runtime dependency on "org.junit.platform:junit-platform-launcher"
- [`KT-65242`](https://youtrack.jetbrains.com/issue/KT-65242) Update transitive dependencies of JVM test frameworks in kotlin-test
- [`KT-63355`](https://youtrack.jetbrains.com/issue/KT-63355) Detect concurrent modifications in ArrayDeque
- [`KT-64956`](https://youtrack.jetbrains.com/issue/KT-64956) Implement optimized removeRange for ArrayDeque
- [`KT-58039`](https://youtrack.jetbrains.com/issue/KT-58039) Wasm: Implement unsigned numbers using wasm builtin capabilities
- [`KT-63341`](https://youtrack.jetbrains.com/issue/KT-63341) K2: JVM StringBuilder has no corresponding members for expected class members
- [`KT-63714`](https://youtrack.jetbrains.com/issue/KT-63714) K2: kotlinx-benchmarks fails with "Unable to find method ''org.gradle.api.tasks.TaskProvider" with register("js")
- [`KT-63157`](https://youtrack.jetbrains.com/issue/KT-63157) Make sure that all deprecation levels are raised to ERROR for declarations intended for removal from kotlinx-metadata
- [`KT-60870`](https://youtrack.jetbrains.com/issue/KT-60870) kotlinx.metadata.InconsistentKotlinMetadataException: No VersionRequirement with the given id in the table In kotlinx-metadata-jvm
- [`KT-64230`](https://youtrack.jetbrains.com/issue/KT-64230) Prohibit writing versions of metadata that are too high
- [`KT-62346`](https://youtrack.jetbrains.com/issue/KT-62346) Sublists of ListBuilder does not correctly detect ConcurrentModification
- [`KT-57922`](https://youtrack.jetbrains.com/issue/KT-57922) kotlinx-metadata-jvm does not take into account strict semantics flag
- [`KT-63447`](https://youtrack.jetbrains.com/issue/KT-63447) K2: stdlib buildscript error: file included in two modules
- [`KT-62785`](https://youtrack.jetbrains.com/issue/KT-62785) Drop unnecessary suppresses in stdlib after bootstrap update
- [`KT-62004`](https://youtrack.jetbrains.com/issue/KT-62004) Drop legacy JS compilations of stdlib and kotlin-test
- [`KT-61614`](https://youtrack.jetbrains.com/issue/KT-61614) WASM: Enum hashCode is not final

### Multiplatform Wizard

- [`KT-66188`](https://youtrack.jetbrains.com/issue/KT-66188) Update Compose for Desktop version to 1.6.0

### Native

#### New Features

- [`KT-61642`](https://youtrack.jetbrains.com/issue/KT-61642) [K/N] Serialize full IdSignatures to caches

#### Performance Improvements

- [`KT-63749`](https://youtrack.jetbrains.com/issue/KT-63749) konan_lldb.py: is_string_or_array inefficient

#### Fixes

- [`KT-67218`](https://youtrack.jetbrains.com/issue/KT-67218) Native: nested classes in kx.serialization ProtoBuf produce empty array for release binary
- [`KT-66390`](https://youtrack.jetbrains.com/issue/KT-66390) Universal binary in included binaries produces universal archive as output
- [`KT-60817`](https://youtrack.jetbrains.com/issue/KT-60817) K2/N: Fix remaining tests
- [`KT-65659`](https://youtrack.jetbrains.com/issue/KT-65659) [K/N][K2] Typealiased kotlin.Throws isn't translated to NSError out param
- [`KT-64249`](https://youtrack.jetbrains.com/issue/KT-64249) Native: Implicit cache directory search is O(n^2)
- [`KT-61695`](https://youtrack.jetbrains.com/issue/KT-61695) [K/N] Empty list error in FakeOverridesActualizer with K2
- [`KT-57870`](https://youtrack.jetbrains.com/issue/KT-57870) compileKotlinNative fails on windows if PATH contains invalid entry
- [`KT-64508`](https://youtrack.jetbrains.com/issue/KT-64508) IndexOutOfBoundsException in  Konan StaticInitializersOptimization
- [`KT-50547`](https://youtrack.jetbrains.com/issue/KT-50547) [Commonizer] K/N echoServer sample fails with multiple "Unresolved reference" errors on Windows
- [`KT-62803`](https://youtrack.jetbrains.com/issue/KT-62803) Konanc has print statement "Produced library API in..." that should be deleted or properly logged at INFO level
- [`KT-61248`](https://youtrack.jetbrains.com/issue/KT-61248) [K/N] Extract native manglers out of `backend.native` module

### Native. Build Infrastructure

- [`KT-63905`](https://youtrack.jetbrains.com/issue/KT-63905) Extract ObjC Export Header generation from K/N backend
- [`KT-63220`](https://youtrack.jetbrains.com/issue/KT-63220) [K/N] Unable to specify custom LLVM distribution

### Native. C and ObjC Import

- [`KT-63049`](https://youtrack.jetbrains.com/issue/KT-63049) NPE in BackendChecker.visitDelegatingConstructorCall compiling ObjC-interop class
- [`KT-49558`](https://youtrack.jetbrains.com/issue/KT-49558) Kotlin/Native: "Backend Internal error: Exception during IR lowering" while compiling "val ldap = memScoped<LDAP> { alloc() }"
- [`KT-64105`](https://youtrack.jetbrains.com/issue/KT-64105) [K2/N] cannot access Objective-C forward declared class used only in a dependent lib
- [`KT-59597`](https://youtrack.jetbrains.com/issue/KT-59597) [K\N] Usage of instancetype in block return type crashes
- [`KT-63287`](https://youtrack.jetbrains.com/issue/KT-63287) [K/N] Create test model for building/executing C-Interop tests
- [`KT-63048`](https://youtrack.jetbrains.com/issue/KT-63048) K2 ObjC interop: Fields are not supported for Companion of subclass of ObjC type

### Native. ObjC Export

- [`KT-66565`](https://youtrack.jetbrains.com/issue/KT-66565) Exporting framework "umbrella" produces an unimportable framework
- [`KT-65863`](https://youtrack.jetbrains.com/issue/KT-65863) Native: implement a flag to emit compiler errors on ObjCExport name collisions
- [`KT-63153`](https://youtrack.jetbrains.com/issue/KT-63153) Native: implement a flag to emit compiler warnings on ObjCExport name collisions
- [`KT-62091`](https://youtrack.jetbrains.com/issue/KT-62091) KMP for iOS framework with private api : __NSCFBoolean

### Native. Runtime

- [`KT-65170`](https://youtrack.jetbrains.com/issue/KT-65170) Kotlin/Native: deprecate -Xworker-exception-handling=legacy with error

### Native. Runtime. Memory

- [`KT-62689`](https://youtrack.jetbrains.com/issue/KT-62689) Native: generate signposts for GC performance debugging
- [`KT-63423`](https://youtrack.jetbrains.com/issue/KT-63423) Kotlin/Native: huge dispose-on-main overhead
- [`KT-66371`](https://youtrack.jetbrains.com/issue/KT-66371) Native: nullptr access during concurrent weak processing in CMS GC
- [`KT-64313`](https://youtrack.jetbrains.com/issue/KT-64313) Kotlin Native: Seg Fault during Garbage Collection on 1.9.21 (observed on iOS)
- [`KT-61093`](https://youtrack.jetbrains.com/issue/KT-61093) Kotlin/Native: enable concurrent weak processing by default

### Native. Stdlib

- [`KT-60514`](https://youtrack.jetbrains.com/issue/KT-60514) Add llvm filecheck tests for atomic intrinsics

### Native. Testing

- [`KT-67501`](https://youtrack.jetbrains.com/issue/KT-67501) Mute flaky driver tests on macOS agents
- [`KT-64755`](https://youtrack.jetbrains.com/issue/KT-64755) Setup test for CMS GC
- [`KT-66014`](https://youtrack.jetbrains.com/issue/KT-66014) [K/N][Tests] Some testsuites don't test two-stage compilation and lose -language-version flag
- [`KT-64393`](https://youtrack.jetbrains.com/issue/KT-64393) Use Compiler Core test infrastructure for testing serialization diagnostics on Native
- [`KT-61871`](https://youtrack.jetbrains.com/issue/KT-61871) Native CompilerOutput tests should be runned for K2
- [`KT-65117`](https://youtrack.jetbrains.com/issue/KT-65117) Implement `IrBackendFacade`s for Kotlin/Native backend
- [`KT-65979`](https://youtrack.jetbrains.com/issue/KT-65979) Improve test coverage on K/JS and K/JVM with existing tests
- [`KT-64408`](https://youtrack.jetbrains.com/issue/KT-64408) [K/N] No tests have been found for `eagerInitializationGlobal1` test with per-file-caches
- [`KT-64256`](https://youtrack.jetbrains.com/issue/KT-64256) IR_DUMP directive doesn't enforce FIR_IDENTICAL when it is possible
- [`KT-62157`](https://youtrack.jetbrains.com/issue/KT-62157) Native: Migrate FileCheck tests to new native test infra

### Reflection

- [`KT-65156`](https://youtrack.jetbrains.com/issue/KT-65156) Calls to `callBy` that use default arguments fail with `KotlineReflectionInternalError` when the argument size is a multiple of 32 in a constructor that contains `value class` as a parameter
- [`KT-57972`](https://youtrack.jetbrains.com/issue/KT-57972) Reflection: "KotlinReflectionInternalError" when using `callBy` with overridden function in inline class
- [`KT-60708`](https://youtrack.jetbrains.com/issue/KT-60708) Reflection: Not supported `)` (parentheses in backticks)
- [`KT-60984`](https://youtrack.jetbrains.com/issue/KT-60984) K2: java.lang.ClassNotFoundException: kotlin.Array in runtime with Spring Boot test
- [`KT-60709`](https://youtrack.jetbrains.com/issue/KT-60709) Reflection: Not recognized bound receiver in case of 'equals' always returning true

### Specification

- [`KT-65651`](https://youtrack.jetbrains.com/issue/KT-65651) Add Vladimir Reshetnikov to the specification "Acknowledgments" section
- [`KT-54499`](https://youtrack.jetbrains.com/issue/KT-54499) Update kotlin specification for non-local break and continue

### Tools. Build Tools API

- [`KT-61860`](https://youtrack.jetbrains.com/issue/KT-61860) Add infrastructure for BTA tests
- [`KT-65048`](https://youtrack.jetbrains.com/issue/KT-65048) "Can't get connection" (to daemon) when classpath has spaces

### Tools. CLI

#### New Features

- [`KT-66703`](https://youtrack.jetbrains.com/issue/KT-66703) Add JVM target bytecode version 22
- [`KT-64989`](https://youtrack.jetbrains.com/issue/KT-64989) Mark the whole diagnostic position range instead of only start position

#### Fixes

- [`KT-65094`](https://youtrack.jetbrains.com/issue/KT-65094) K2: Revise PerformanceManager reporting
- [`KT-67417`](https://youtrack.jetbrains.com/issue/KT-67417) CLI: Remove option -Xrepeat
- [`KT-65451`](https://youtrack.jetbrains.com/issue/KT-65451) K2: CLI: false positive warning "scripts are not yet supported with K2 in LightTree mode" on irrelevant files in source directory
- [`KT-65842`](https://youtrack.jetbrains.com/issue/KT-65842) K2 / CLI: "kotlinc -version" creates META-INF/main.kotlin_module
- [`KT-66926`](https://youtrack.jetbrains.com/issue/KT-66926) Add a flag to report warnings when errors are found
- [`KT-64384`](https://youtrack.jetbrains.com/issue/KT-64384) Until the REPL in K2 is not supported, display an appropriate warning
- [`KT-64608`](https://youtrack.jetbrains.com/issue/KT-64608) K2: Wrong end position of compiler diagnostics
- [`KT-64013`](https://youtrack.jetbrains.com/issue/KT-64013) CLI REPL: "com.sun.jna.LastErrorException: [14] Bad address" on invoking kotlinc from CLI on ARM Mac
- [`KT-62644`](https://youtrack.jetbrains.com/issue/KT-62644) Don't enable in progressive mode bug-fix features without target version
- [`KT-62350`](https://youtrack.jetbrains.com/issue/KT-62350) CLI: no color output on Apple silicon Macs
- [`KT-61156`](https://youtrack.jetbrains.com/issue/KT-61156) K2: do not try to run compilation if there were errors during calculation of Java module graph
- [`KT-48026`](https://youtrack.jetbrains.com/issue/KT-48026) Add the compiler X-flag to enable self upper bound type inference

### Tools. CLI. Native

- [`KT-64517`](https://youtrack.jetbrains.com/issue/KT-64517) Drop deprecated KonanTargets

### Tools. Commonizer

- [`KT-64376`](https://youtrack.jetbrains.com/issue/KT-64376) Commonizer incorrectly retains UnsafeNumber annotation in target sets where it shouldn't

### Tools. Compiler Plugin API

- [`KT-59555`](https://youtrack.jetbrains.com/issue/KT-59555) Expose resource closing extension point in `CompilerPluginRegistrar`
- [`KT-64444`](https://youtrack.jetbrains.com/issue/KT-64444) K2: IrGeneratedDeclarationsRegistrar.addMetadataVisibleAnnotationsToElement doesn't work for declarations in common module

### Tools. Compiler Plugins

#### New Features

- [`KT-63617`](https://youtrack.jetbrains.com/issue/KT-63617) Add kotlin-power-assert to Kotlin repository
- [`KT-33020`](https://youtrack.jetbrains.com/issue/KT-33020) Support stripping debug information in the jvm-abi-gen plugin
- [`KT-64591`](https://youtrack.jetbrains.com/issue/KT-64591) Data class' copy method is never stripped from ABI
- [`KT-65690`](https://youtrack.jetbrains.com/issue/KT-65690) jvm-abi-gen: Remove internal declarations from ABI
- [`KT-64590`](https://youtrack.jetbrains.com/issue/KT-64590) jvm-abi-gen: Effectively private classes are not being removed from ABI

#### Fixes

- [`KT-64707`](https://youtrack.jetbrains.com/issue/KT-64707) K2: Parcelize ignores `@TypeParceler` set for typealias
- [`KT-67523`](https://youtrack.jetbrains.com/issue/KT-67523) [K2] Actualizer cannot reconcile mismatched parameter names from java supertypes
- [`KT-67489`](https://youtrack.jetbrains.com/issue/KT-67489) JsPlainObjects Plugin: Method not found when consuming
- [`KT-63607`](https://youtrack.jetbrains.com/issue/KT-63607) Migrate kotlin-power-assert into Kotlin repository
- [`KT-67354`](https://youtrack.jetbrains.com/issue/KT-67354) K2 Parcelize: support efficient Parcel serializer for parcelables in the same module
- [`KT-64454`](https://youtrack.jetbrains.com/issue/KT-64454) K2: Implement ParcelizeIrBytecodeListingTestGenerated for K2
- [`KT-67353`](https://youtrack.jetbrains.com/issue/KT-67353) K2 Parcelize: support parcelableCreator intrinsic
- [`KT-66526`](https://youtrack.jetbrains.com/issue/KT-66526) K2: Special function kind setup does not work for value parameter whose type is function with a receiver
- [`KT-63507`](https://youtrack.jetbrains.com/issue/KT-63507) K2 / All-open plugin: "'open' has no effect on a final class" warning
- [`KT-66208`](https://youtrack.jetbrains.com/issue/KT-66208) PowerAssert: some built-in operators are not aligned correctly for some values
- [`KT-65810`](https://youtrack.jetbrains.com/issue/KT-65810) PowerAssert: Infix transformation doesn't capture full context
- [`KT-65640`](https://youtrack.jetbrains.com/issue/KT-65640) PowerAssert: Infix function not aligned correctly
- [`KT-61993`](https://youtrack.jetbrains.com/issue/KT-61993) K2: Synthetic file classes are generated with start offset of 0, causing errors during compilation
- [`KT-64971`](https://youtrack.jetbrains.com/issue/KT-64971) Exception is thrown when compiling kotlinx.coroutines to Native because of the new signature clash diagnostics
- [`KT-59074`](https://youtrack.jetbrains.com/issue/KT-59074) K2: false-positive MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT if allOpen plugin is used and a val is defined with init {} block
- [`KT-64589`](https://youtrack.jetbrains.com/issue/KT-64589) jvm-abi-gen: Order of class members affects ABI jar
- [`KT-65072`](https://youtrack.jetbrains.com/issue/KT-65072) jvm-abi-gen: SourceDebugExtension annotation isn't stripped along with corresponding attribute
- [`KT-54025`](https://youtrack.jetbrains.com/issue/KT-54025) [K2] [NONE_APPLICABLE] compiler error in case @ AllArgConstructor annotation is used together with a static field
- [`KT-54054`](https://youtrack.jetbrains.com/issue/KT-54054) [Lombok] An extra unneeded constructor parameter is expected by compiler if java class annotated with @ AllArgsConstructor and has private final initialized field
- [`KT-61432`](https://youtrack.jetbrains.com/issue/KT-61432) K2 Parcelize. RawValue is not recognized if parameter is annotated via typealias
- [`KT-64656`](https://youtrack.jetbrains.com/issue/KT-64656) K2: realm-kotlin: compilation errors in IR plugin
- [`KT-53861`](https://youtrack.jetbrains.com/issue/KT-53861) K2. Report SERIALIZER_TYPE_INCOMPATIBLE on specific type argument in kotlinx.serialization
- [`KT-63086`](https://youtrack.jetbrains.com/issue/KT-63086) K2: "Parcelable should be a class"
- [`KT-60849`](https://youtrack.jetbrains.com/issue/KT-60849) jvm-abi-gen: do not treat hasConstant property flag as a part of ABI for non-const properties
- [`KT-53926`](https://youtrack.jetbrains.com/issue/KT-53926) K2. Don't check serializable properties from supertypes

### Tools. Compiler plugins. Serialization

- [`KT-65757`](https://youtrack.jetbrains.com/issue/KT-65757) K2: Missing `@Deprecated` annotation on synthesized declarations
- [`KT-63539`](https://youtrack.jetbrains.com/issue/KT-63539) K2: Missing "Serializable class has duplicate serial name of property"
- [`KT-63570`](https://youtrack.jetbrains.com/issue/KT-63570) K2 / Serialization: "Class * which is serializer for type * is applied here to type *. This may lead to errors or incorrect behavior."
- [`KT-64447`](https://youtrack.jetbrains.com/issue/KT-64447) K2: Implement Serialization...IrBoxTestGenerated for K2
- [`KT-63591`](https://youtrack.jetbrains.com/issue/KT-63591) K2: "KotlinReflectionInternalError: Could not compute caller for function" on generated internal constructor
- [`KT-64124`](https://youtrack.jetbrains.com/issue/KT-64124) Different klib signatures in K1/K2 for a serializable class
- [`KT-63402`](https://youtrack.jetbrains.com/issue/KT-63402) K2 / Serialization: "SyntheticAccessorLowering should not attempt to modify other files!" caused by sealed base with generic derived class in separate files
- [`KT-62215`](https://youtrack.jetbrains.com/issue/KT-62215) Serialization / Native: "IllegalArgumentException: No container found for type parameter" caused by serializing generic classes with a field that uses generics
- [`KT-62522`](https://youtrack.jetbrains.com/issue/KT-62522) K2 + kotlinx.serialization + Native: NPE when generic base class has inheritor in other module

### Tools. Daemon

- [`KT-64283`](https://youtrack.jetbrains.com/issue/KT-64283) Configure correct JVM arguments when starting the daemon

### Tools. Fleet. ObjC Export

#### Fixes

- [`KT-66695`](https://youtrack.jetbrains.com/issue/KT-66695) Move `analysis-api-klib-reader` package into 'o.j.k.native.analysis.api`
- [`KT-65384`](https://youtrack.jetbrains.com/issue/KT-65384) ObjCExport: class super name special case
- [`KT-66380`](https://youtrack.jetbrains.com/issue/KT-66380) ObjCExport: support interface implementation
- [`KT-65670`](https://youtrack.jetbrains.com/issue/KT-65670) ObjCExport: Naming: Support additional module based prefix
- [`KT-64953`](https://youtrack.jetbrains.com/issue/KT-64953) ObjCExport: Analysis-Api: enum
- [`KT-65348`](https://youtrack.jetbrains.com/issue/KT-65348) ObjCExport: Char as function return type
- [`KT-65738`](https://youtrack.jetbrains.com/issue/KT-65738) ObjCExport: Analysis-Api: Generate base declarations
- [`KT-65204`](https://youtrack.jetbrains.com/issue/KT-65204) ObjCExport: Analysis Api: Support nested classes
- [`KT-65225`](https://youtrack.jetbrains.com/issue/KT-65225) ObjCExport: implement KtCallableSymbol.isArray
- [`KT-65108`](https://youtrack.jetbrains.com/issue/KT-65108) ObjCExport: Tests: Check if 'requirePlatformLibs' is necessary
- [`KT-65281`](https://youtrack.jetbrains.com/issue/KT-65281) ObjCExport: AA: Run already passing Unit Tests on CI
- [`KT-65080`](https://youtrack.jetbrains.com/issue/KT-65080) ObjCExport: Analysis-Api: error handling
- [`KT-64952`](https://youtrack.jetbrains.com/issue/KT-64952) ObjCExport: Analysis-Api: object
- [`KT-64076`](https://youtrack.jetbrains.com/issue/KT-64076) ObjCExport: Do not retain descriptors in stubs
- [`KT-64227`](https://youtrack.jetbrains.com/issue/KT-64227) ObjCExport: Extract Header Generation to base module
- [`KT-64168`](https://youtrack.jetbrains.com/issue/KT-64168) ObjCExport: Split header generator module into K1 and Analysis Api
- [`KT-64869`](https://youtrack.jetbrains.com/issue/KT-64869) ObjCExport: Analysis-Api: Support 'MustBeDocumented' annotations
- [`KT-64839`](https://youtrack.jetbrains.com/issue/KT-64839) ObjCExport: Enable tests on CI for aggregate
- [`KT-64888`](https://youtrack.jetbrains.com/issue/KT-64888) ObjCExport: Analysis Api: Support exporting KDoc

### Tools. Gradle

#### New Features

- [`KT-67253`](https://youtrack.jetbrains.com/issue/KT-67253) Support per-target configuration in compose-compiler-gradle-plugin
- [`KT-67006`](https://youtrack.jetbrains.com/issue/KT-67006) Create new compose compiler Gradle plugin
- [`KT-62921`](https://youtrack.jetbrains.com/issue/KT-62921) Add API to allow getting the version of the kotlinc compiler
- [`KT-61975`](https://youtrack.jetbrains.com/issue/KT-61975) Re-purpose kotlin.experimental.tryK2
- [`KT-64653`](https://youtrack.jetbrains.com/issue/KT-64653) Add Kotlin DslMarker into Gradle plugin DSL
- [`KT-59627`](https://youtrack.jetbrains.com/issue/KT-59627) FUS base plugin
- [`KT-62025`](https://youtrack.jetbrains.com/issue/KT-62025) K/Wasm: Support binaryen for wasi

#### Performance Improvements

- [`KT-60664`](https://youtrack.jetbrains.com/issue/KT-60664) Gradle 8.3: KGP eagerly creates compile task
- [`KT-64353`](https://youtrack.jetbrains.com/issue/KT-64353) Improve reuse of Build Tools Api's classloader
- [`KT-66912`](https://youtrack.jetbrains.com/issue/KT-66912) Parallel compilation slowdown due to synchronization
- [`KT-63005`](https://youtrack.jetbrains.com/issue/KT-63005) Avoid registering KMP related compatibility/disambiguration rules for pure JVM/Android projects

#### Fixes

- [`KT-58768`](https://youtrack.jetbrains.com/issue/KT-58768) Support configuration cache and project isolation for FUS statistics
- [`KT-65143`](https://youtrack.jetbrains.com/issue/KT-65143) Use the new ConfigurationContainer dependencyScope method to create dependency declaration configurations
- [`KT-62640`](https://youtrack.jetbrains.com/issue/KT-62640) Compatibility with Gradle 8.5 release
- [`KT-62639`](https://youtrack.jetbrains.com/issue/KT-62639) Compatibility with Gradle 8.4 release
- [`KT-59024`](https://youtrack.jetbrains.com/issue/KT-59024) Compatibility with Gradle 8.3 release
- [`KT-58064`](https://youtrack.jetbrains.com/issue/KT-58064) Compatibility with Gradle 8.2 release
- [`KT-64355`](https://youtrack.jetbrains.com/issue/KT-64355) Add plugin variant for gradle 8.5
- [`KT-67746`](https://youtrack.jetbrains.com/issue/KT-67746) Indicate for users they need to apply the new Kotlin Compose Gradle plugin
- [`KT-67387`](https://youtrack.jetbrains.com/issue/KT-67387) Enable intrinsic remember by default in compose compiler gradle plugin
- [`KT-64115`](https://youtrack.jetbrains.com/issue/KT-64115) KGP + JVM/JS/WASM: The same library can be passed twice to the compiler
- [`KT-67762`](https://youtrack.jetbrains.com/issue/KT-67762) Rename Kotlin Compose Compiler plugin on Gradle portal
- [`KT-64504`](https://youtrack.jetbrains.com/issue/KT-64504) Remove ownModuleName from AbstractKotlinCompile
- [`KT-67778`](https://youtrack.jetbrains.com/issue/KT-67778) Clarify documentation for compose metricsDestination property
- [`KT-67139`](https://youtrack.jetbrains.com/issue/KT-67139) Build reports can be overridden
- [`KT-67138`](https://youtrack.jetbrains.com/issue/KT-67138) Json report is empty for incremental compilation
- [`KT-67685`](https://youtrack.jetbrains.com/issue/KT-67685) KotlinBaseApiPlugin regression with Gradle's Configuration Cache in 2.0.0-RC1
- [`KT-64567`](https://youtrack.jetbrains.com/issue/KT-64567) [FUS] Add boolean flag into kotlin.gradle.performance collector
- [`KT-67515`](https://youtrack.jetbrains.com/issue/KT-67515) Remove 'experimental' from compose strong skipping mode
- [`KT-67441`](https://youtrack.jetbrains.com/issue/KT-67441) Gradle remote cache misses in the compose plugin
- [`KT-67602`](https://youtrack.jetbrains.com/issue/KT-67602) Compose gradle plugin: a deprecated plugin option 'experimentalStrongSkipping' is added by default that causes a warning
- [`KT-67200`](https://youtrack.jetbrains.com/issue/KT-67200) Compose gradle plugin: 'suppressKotlinVersionCompatibilityCheck' option is duplicated if added as a kotlin option for the KotlinCompile task and kapt is used
- [`KT-67216`](https://youtrack.jetbrains.com/issue/KT-67216) Compose compiler plugin: false-positive versions incompatibility is reported
- [`KT-64379`](https://youtrack.jetbrains.com/issue/KT-64379) Remove `kotlin.useK2` gradle property
- [`KT-62939`](https://youtrack.jetbrains.com/issue/KT-62939) Bump minimal supported AGP version to 7.1
- [`KT-63491`](https://youtrack.jetbrains.com/issue/KT-63491) Restore access to top-level DSL to configure compiler options in MPP
- [`KT-65935`](https://youtrack.jetbrains.com/issue/KT-65935) Track project isolation Gradle feature
- [`KT-65934`](https://youtrack.jetbrains.com/issue/KT-65934) Track if Gradle configuration cache is enabled in the user builds
- [`KT-66459`](https://youtrack.jetbrains.com/issue/KT-66459) PowerAssert: Improve design of excludedSourceSets extension property
- [`KT-64203`](https://youtrack.jetbrains.com/issue/KT-64203) Throw exception when old build report properties are used
- [`KT-62758`](https://youtrack.jetbrains.com/issue/KT-62758) Gradle: make precise task outputs backup enabled by default
- [`KT-65568`](https://youtrack.jetbrains.com/issue/KT-65568) Deprecate the ability to configure compiler options in KotlinCompilation
- [`KT-63419`](https://youtrack.jetbrains.com/issue/KT-63419) Deprecate 'kotlinOptions' DSL
- [`KT-64848`](https://youtrack.jetbrains.com/issue/KT-64848) Log K/Native compiler arguments with log level specified for compiler arguments
- [`KT-58223`](https://youtrack.jetbrains.com/issue/KT-58223) Kotlin Gradle plugin shouldn't store data in project cache directory
- [`KT-61913`](https://youtrack.jetbrains.com/issue/KT-61913) Validate LanguageSettings KDoc
- [`KT-61171`](https://youtrack.jetbrains.com/issue/KT-61171) CompilerPluginsIncrementalIT.afterChangeInPluginBuildDoesIncrementalProcessing doesn't provide a compiler plugin for K2 leading to the test failure
- [`KT-62131`](https://youtrack.jetbrains.com/issue/KT-62131) Could not isolate value org.jetbrains.kotlin.gradle.plugin.statistics.BuildFlowService$Parameters_Decorated`@63fddc4b` of type BuildFlowService.Parameters
- [`KT-66961`](https://youtrack.jetbrains.com/issue/KT-66961) Early access to gradle.rootProject leads to an exception
- [`KT-61918`](https://youtrack.jetbrains.com/issue/KT-61918) Removal of an associated compilation from a build script doesn't lead to full recompilation
- [`KT-63619`](https://youtrack.jetbrains.com/issue/KT-63619) Add Kotlin power-assert compiler plugin to feature usage statistics gathering
- [`KT-62108`](https://youtrack.jetbrains.com/issue/KT-62108) Wrong scope of compiler options is used while configuring options for all targets and all compilations
- [`KT-55322`](https://youtrack.jetbrains.com/issue/KT-55322) Kotlin daemon: Cannot perform operation, requested state: Alive > actual: LastSession
- [`KT-66429`](https://youtrack.jetbrains.com/issue/KT-66429) Move WASM stability warning to KGP Tooling Diagnostics and report it once per build
- [`KT-63165`](https://youtrack.jetbrains.com/issue/KT-63165) Gradle: checkKotlinGradlePluginConfigurationErrors uses deprecated Gradle behavior
- [`KT-66374`](https://youtrack.jetbrains.com/issue/KT-66374) Diagnostic for deprecated properties: false-positive warning is reported for `kapt.use.k2`property
- [`KT-64117`](https://youtrack.jetbrains.com/issue/KT-64117) K2: "'when' expression must be exhaustive" state does not fail compilation
- [`KT-58443`](https://youtrack.jetbrains.com/issue/KT-58443) Change deprecation level to WARNING for KotlinOptions
- [`KT-65768`](https://youtrack.jetbrains.com/issue/KT-65768) Don't pass -Xfragment-sources for non-mpp compilations
- [`KT-62398`](https://youtrack.jetbrains.com/issue/KT-62398) KMP: Compose breaks resolution of stdlib declarations in common source set
- [`KT-64046`](https://youtrack.jetbrains.com/issue/KT-64046) Provide K/N version to KGP when -Pkotlin.native.enabled=true
- [`KT-66154`](https://youtrack.jetbrains.com/issue/KT-66154) Cannot access 'org.slf4j.spi.LoggingEventAware' in the Space K2 QG
- [`KT-65952`](https://youtrack.jetbrains.com/issue/KT-65952) PowerAssert: Update Gradle extension to be more idiomatic
- [`KT-65951`](https://youtrack.jetbrains.com/issue/KT-65951) PowerAssert: Add Gradle integration tests to compiler plugin
- [`KT-66373`](https://youtrack.jetbrains.com/issue/KT-66373) [Wasm, KGP] Npm is not configured for JS usagе for wasmWasi project
- [`KT-66314`](https://youtrack.jetbrains.com/issue/KT-66314) Build reports in JSON: property 'kotlin.build.report.json.directory' without value causes NPE
- [`KT-64380`](https://youtrack.jetbrains.com/issue/KT-64380) Add project diagnostics for deprecated properties
- [`KT-65986`](https://youtrack.jetbrains.com/issue/KT-65986) `GradleDeprecatedOption.removeAfter` does not actually remove arguments from the compilerOptions/kotlinOptions DSLs
- [`KT-65989`](https://youtrack.jetbrains.com/issue/KT-65989) Compile against Gradle API 8.6
- [`KT-65819`](https://youtrack.jetbrains.com/issue/KT-65819) Build Gradle Plugins against Gradle 8.5 API
- [`KT-65701`](https://youtrack.jetbrains.com/issue/KT-65701) Limit Gradle daemon max memory in integration tests
- [`KT-65708`](https://youtrack.jetbrains.com/issue/KT-65708) Flaky tests because of ivy repos in Integration Tests
- [`KT-56904`](https://youtrack.jetbrains.com/issue/KT-56904) Enable warnings-as-error for Kotlin Gradle plugins compilation
- [`KT-65606`](https://youtrack.jetbrains.com/issue/KT-65606) Out of memory in Anki Android in the K2 QG
- [`KT-65347`](https://youtrack.jetbrains.com/issue/KT-65347) K/N has not been dowloaded before :commonizeNativeDistribution
- [`KT-65213`](https://youtrack.jetbrains.com/issue/KT-65213) Collect logic for FUS metrics calculation in one place
- [`KT-61698`](https://youtrack.jetbrains.com/issue/KT-61698) Compiler options configured inside metadata {} target set up all targets in a project
- [`KT-64824`](https://youtrack.jetbrains.com/issue/KT-64824) Move validateParameters from CInteropProcess to diagnostics
- [`KT-60879`](https://youtrack.jetbrains.com/issue/KT-60879) Deprecation warning on trying to configure Configuration multiple times
- [`KT-64251`](https://youtrack.jetbrains.com/issue/KT-64251) KGP: Cannot re-use tooling model cache with Project Isolation due to "~/.gradle/kotlin-profile" changing
- [`KT-64655`](https://youtrack.jetbrains.com/issue/KT-64655) K2: PeopleInSpace: K2 build fails during Gradle config
- [`KT-63697`](https://youtrack.jetbrains.com/issue/KT-63697) The warning is still presented in terminal after suppressing it with -Xexpect-actual-classes flag
- [`KT-62527`](https://youtrack.jetbrains.com/issue/KT-62527) Gradle: get rid of the `Project.buildDir` usages
- [`KT-60733`](https://youtrack.jetbrains.com/issue/KT-60733) Allow specify log level for compiler arguments used to compile sources
- [`KT-63369`](https://youtrack.jetbrains.com/issue/KT-63369) Fix: "The org.gradle.api.plugins.BasePluginConvention type has been deprecated."
- [`KT-63368`](https://youtrack.jetbrains.com/issue/KT-63368) Fix "The automatic loading of test framework implementation dependencies has been deprecated. "
- [`KT-63601`](https://youtrack.jetbrains.com/issue/KT-63601) Fetching Gradle compiler DSL objects using raw strings is inconvenient in the Groovy DSL
- [`KT-62955`](https://youtrack.jetbrains.com/issue/KT-62955) Missing static accessors for Wasm targets in Kotlin Gradle plugin DSL:
- [`KT-62962`](https://youtrack.jetbrains.com/issue/KT-62962) Remove COMPILE_INCREMENTAL_WITH_ARTIFACT_TRANSFORM system property
- [`KT-62264`](https://youtrack.jetbrains.com/issue/KT-62264) Send build type report metric to FUS
- [`KT-62650`](https://youtrack.jetbrains.com/issue/KT-62650) Gradle: Return the usage of `kotlin-compiler-embeddable` back
- [`KT-61295`](https://youtrack.jetbrains.com/issue/KT-61295) `KotlinTestReport` captures `Project.buildDir` too early
- [`KT-62987`](https://youtrack.jetbrains.com/issue/KT-62987) Add tests for statistics plugin in Aggregate build
- [`KT-62964`](https://youtrack.jetbrains.com/issue/KT-62964) Build Gradle plugin against Gradle 8.4 API
- [`KT-62617`](https://youtrack.jetbrains.com/issue/KT-62617) Update report configuration project FUS metrics
- [`KT-61896`](https://youtrack.jetbrains.com/issue/KT-61896) Gradle: compilation via build tools API doesn't perform Gradle side output backups
- [`KT-62016`](https://youtrack.jetbrains.com/issue/KT-62016) ClassNotFoundException on org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer  in the K2 QG
- [`KT-56574`](https://youtrack.jetbrains.com/issue/KT-56574) Implement a prototype of Kotlin JVM compilation pipeline via the build tools API
- [`KT-61206`](https://youtrack.jetbrains.com/issue/KT-61206) Build system classes may leak into the Build Tools API classloader
- [`KT-61737`](https://youtrack.jetbrains.com/issue/KT-61737) GradleStyleMessageRenderer.render misses a space between the file and the message when `location` is (line:column = 0:0)

### Tools. Gradle. Cocoapods

- [`KT-57650`](https://youtrack.jetbrains.com/issue/KT-57650) Gradle Cocoapods: use pod install --repo-update instead of pod install
- [`KT-63331`](https://youtrack.jetbrains.com/issue/KT-63331) CocoaPods plugin noPodspec() causes "property * specifies file * which doesn't exist."

### Tools. Gradle. JS

#### Fixes

- [`KT-55620`](https://youtrack.jetbrains.com/issue/KT-55620) KJS / Gradle: plugin doesn't support repositoriesMode
- [`KT-65870`](https://youtrack.jetbrains.com/issue/KT-65870) KJS / Gradle: kotlinUpgradePackageLock fails making Yarn unusable
- [`KT-66917`](https://youtrack.jetbrains.com/issue/KT-66917) JS/Wasm: Upgrade NPM dependencies
- [`KT-63040`](https://youtrack.jetbrains.com/issue/KT-63040) K/JS: Rework outputs of webpack and distribution task
- [`KT-61992`](https://youtrack.jetbrains.com/issue/KT-61992) KJS / Gradle: KotlinJsTest using KotlinMocha should not show output, and should not run a dry-run every time.
- [`KT-65295`](https://youtrack.jetbrains.com/issue/KT-65295) Gradle: K/N and K/JS tests may produce unrequested TeamCity service messages
- [`KT-63435`](https://youtrack.jetbrains.com/issue/KT-63435) KJS: Get rid of deprecated outputFileProperty of Kotlin2JsCompile
- [`KT-61294`](https://youtrack.jetbrains.com/issue/KT-61294) `NodeJsRootExtension` captures `Project.buildDir` too early
- [`KT-59282`](https://youtrack.jetbrains.com/issue/KT-59282) K/JS: KotlinJsIrLinkConfig is not compatible with Configuration Cache in Gradle 8.1.1
- [`KT-62780`](https://youtrack.jetbrains.com/issue/KT-62780) K/JS: Deprecate node-specific properties in NodeJsRootExtension
- [`KT-63544`](https://youtrack.jetbrains.com/issue/KT-63544) KGP: JS - KotlinJsIrLink is not compatible with Gradle CC starting 8.4
- [`KT-63312`](https://youtrack.jetbrains.com/issue/KT-63312) KJS: Apply IR flags for JS compilations unconditionally
- [`KT-62633`](https://youtrack.jetbrains.com/issue/KT-62633) wasmWasi/JsNodeTest tasks are always not up-to-date
- [`KT-63225`](https://youtrack.jetbrains.com/issue/KT-63225) java.lang.ClassNotFoundException: org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation in the K2 QG
- [`KT-41382`](https://youtrack.jetbrains.com/issue/KT-41382) NI / KJS / Gradle: TYPE_MISMATCH caused by compilations.getting delegate
- [`KT-53077`](https://youtrack.jetbrains.com/issue/KT-53077) KJS / Gradle: Remove redundant gradle js log on kotlin build
- [`KT-56300`](https://youtrack.jetbrains.com/issue/KT-56300) KJS / Gradle: plugin should not add repositories unconditionally
- [`KT-60694`](https://youtrack.jetbrains.com/issue/KT-60694) KJS: Remove K/JS legacy support from Gradle plugin
- [`KT-56465`](https://youtrack.jetbrains.com/issue/KT-56465) MPP: Import with npm dependency fails with "UninitializedPropertyAccessException: lateinit property fileHasher has not been initialized" if there is no selected JavaScript environment for JS target
- [`KT-41578`](https://youtrack.jetbrains.com/issue/KT-41578) Kotlin/JS: contiuous mode: changes in static resources do not reload browser page

### Tools. Gradle. Kapt

- [`KT-62518`](https://youtrack.jetbrains.com/issue/KT-62518) kapt processing is skipped when all annotation processors are indirect dependencies
- [`KT-27404`](https://youtrack.jetbrains.com/issue/KT-27404) Kapt does not call annotation processors on custom (e.g., androidTest) source sets if all dependencies are inherited from the main kapt configuration
- [`KT-22261`](https://youtrack.jetbrains.com/issue/KT-22261) Annotation Processor - in gradle, kapt configuration is missing extendsFrom

### Tools. Gradle. Multiplatform

#### New Features

- [`KT-66047`](https://youtrack.jetbrains.com/issue/KT-66047) KMP: Isolate dependencies graph between main and test source sets
- [`KT-61559`](https://youtrack.jetbrains.com/issue/KT-61559) Include stdlib and platform dependencies to KotlinNativeCompilation.compileDependencyFiles API
- [`KT-65196`](https://youtrack.jetbrains.com/issue/KT-65196) Add high-level DSL to configure compiler options in the multiplatform project

#### Performance Improvements

- [`KT-57141`](https://youtrack.jetbrains.com/issue/KT-57141) K2: KotlinCompile task input named 'multiplatformStructure.fragments.$0.sources' is tracked in a pure JVM kotlin project together with changes of sources

#### Fixes

- [`KT-65315`](https://youtrack.jetbrains.com/issue/KT-65315) KMP Composite compileIosMainKotlinMetadata fails with "Could not find <included iOS dependency>"
- [`KT-67042`](https://youtrack.jetbrains.com/issue/KT-67042) K2: Unresolved reference 'convertRadiusToSigma'
- [`KT-66983`](https://youtrack.jetbrains.com/issue/KT-66983) MPP Configuration Cache IT fails with Gradle 8.7 on windows
- [`KT-60489`](https://youtrack.jetbrains.com/issue/KT-60489) Android-java only consumers (no KGP applied) choose Java-variant instead of Android-variant when depending on MPP library
- [`KT-67806`](https://youtrack.jetbrains.com/issue/KT-67806) KMP import fails if android target has flavors
- [`KT-67636`](https://youtrack.jetbrains.com/issue/KT-67636) Gradle configuration error when use withJava()
- [`KT-63536`](https://youtrack.jetbrains.com/issue/KT-63536) KMP: MetadataDependencyTransformationTask is not Thread Safe
- [`KT-67127`](https://youtrack.jetbrains.com/issue/KT-67127) KMP: IDE Dependency Resolver for CInterops reports errors on linux and windows machines
- [`KT-66514`](https://youtrack.jetbrains.com/issue/KT-66514) Don't get output file from Cinterop task for IDE Import if host os doesn't support it
- [`KT-65426`](https://youtrack.jetbrains.com/issue/KT-65426) K2: Debug compilation fails because code from main source set included in two K2 fragments
- [`KT-65480`](https://youtrack.jetbrains.com/issue/KT-65480) MissingNativeStdlibChecker checks existence of konanDistribution.stdlib during configuration phase
- [`KT-61945`](https://youtrack.jetbrains.com/issue/KT-61945) Report redundant dependsOn-edges
- [`KT-65187`](https://youtrack.jetbrains.com/issue/KT-65187) Remove deprecated platform plugins ids
- [`KT-49919`](https://youtrack.jetbrains.com/issue/KT-49919) Introduce the `org.gradle.jvm.environment` attribute on JVM and Android published variants (both for MPP and non-MPP libraries)
- [`KT-66419`](https://youtrack.jetbrains.com/issue/KT-66419) Remove useless API: Kotlin compilation level compiler options DSL
- [`KT-64913`](https://youtrack.jetbrains.com/issue/KT-64913) Report warning if user has multiple source set roots for a certain compilation
- [`KT-66563`](https://youtrack.jetbrains.com/issue/KT-66563) Stop including resources to metadata klib
- [`KT-61078`](https://youtrack.jetbrains.com/issue/KT-61078) K2: Compilation fails in FirSerializer trying to serialize nested class
- [`KT-66372`](https://youtrack.jetbrains.com/issue/KT-66372) KMP: JVM dependency can be downgraded by metadata dependency
- [`KT-66431`](https://youtrack.jetbrains.com/issue/KT-66431) KMP: External Target Compilation friendArtifactResolver throws ClassCastException
- [`KT-64995`](https://youtrack.jetbrains.com/issue/KT-64995) KonanPropertiesBuildService is not compatible with Project Isolation
- [`KT-61430`](https://youtrack.jetbrains.com/issue/KT-61430) K2/KMP: metadata compilation fails with Unresolved reference for property in actual class
- [`KT-63753`](https://youtrack.jetbrains.com/issue/KT-63753) K2: File "does not belong to any module" when it is generated by `registerJavaGeneratingTask` in AGP
- [`KT-62508`](https://youtrack.jetbrains.com/issue/KT-62508) Merge Android Source Sets into one K2 Fragment
- [`KT-61943`](https://youtrack.jetbrains.com/issue/KT-61943) Mark the `checkKotlinGradlePluginConfigurationErrors` as UP-TO-DATE when possible
- [`KT-63206`](https://youtrack.jetbrains.com/issue/KT-63206) Deprecate  eager CInteropProcess.outputFile in favor to lazy outputFileProvider
- [`KT-65248`](https://youtrack.jetbrains.com/issue/KT-65248) Native compile task fail with ClassNotFoundException: org.jetbrains.kotlin.cli.utilities.MainKt
- [`KT-56440`](https://youtrack.jetbrains.com/issue/KT-56440) TCS: Gradle Sync: Add API to populate extras only during sync
- [`KT-64629`](https://youtrack.jetbrains.com/issue/KT-64629) Gradle configuration fails: 'fun jvmToolchain(jdkVersion: Int): Unit' can't be called in this context by implicit receiver
- [`KT-63226`](https://youtrack.jetbrains.com/issue/KT-63226) KGP Multiplatform Ide Dependency Resolution: Use gradle variants instead/in addition of ArtifactResolutionQuery
- [`KT-60734`](https://youtrack.jetbrains.com/issue/KT-60734) Handle the migration from ios shortcut and source set with `getting`
- [`KT-63197`](https://youtrack.jetbrains.com/issue/KT-63197) After using Kotlin 1.9.20 on Windows 11, the gradle sync failed
- [`KT-61540`](https://youtrack.jetbrains.com/issue/KT-61540) K2: KMP/K2: Metadata compilations: Discriminate expect over actual by sorting compile path using refines edges
- [`KT-60860`](https://youtrack.jetbrains.com/issue/KT-60860) K2: Fix `KotlinNativeCompileArgumentsTest` in 2.0 branch
- [`KT-61463`](https://youtrack.jetbrains.com/issue/KT-61463) KMP: Remove unused 'kpm' code
- [`KT-40309`](https://youtrack.jetbrains.com/issue/KT-40309) A call of a declaration with actual typealiases is incorrectly successfully compiled in commonTest using the type from actual part

### Tools. Gradle. Native

#### New Features

- [`KT-49268`](https://youtrack.jetbrains.com/issue/KT-49268) Only download Kotlin/Native Compiler when there are valid targets

#### Performance Improvements

- [`KT-58303`](https://youtrack.jetbrains.com/issue/KT-58303) Kotlin multiplatform Gradle plugin downloads Kotlin/Native compiler during configuration

#### Fixes

- [`KT-67522`](https://youtrack.jetbrains.com/issue/KT-67522) K/N toolchain: unclear compilation error if path specified as a value for the kotlin.native.home doesn't provide the kotlin native compiler downloaded
- [`KT-67521`](https://youtrack.jetbrains.com/issue/KT-67521) K/N warning checking existence of the standard library isn't displayed when the native toolchain enabled and the kotlin native home dir doesn't contain stdlib
- [`KT-65624`](https://youtrack.jetbrains.com/issue/KT-65624) K/N warning: "The Kotlin/Native distribution used in this build does not provide the standard library." is displayed during configuration phase
- [`KT-66694`](https://youtrack.jetbrains.com/issue/KT-66694) Disable Kotlin Native Toolchain when custom konan home passed
- [`KT-66309`](https://youtrack.jetbrains.com/issue/KT-66309) K/N compiler can't be downloaded if project import is stopped while 'commonizeNativeDistribution' task is being executed and rerun again
- [`KT-65641`](https://youtrack.jetbrains.com/issue/KT-65641) Invalid replacements for deprecated properties 'konanHome' and 'konanDataDir' are suggested as quick fixes
- [`KT-65823`](https://youtrack.jetbrains.com/issue/KT-65823) Add downloading k/n dependencies to KotlinNativeProvider
- [`KT-62907`](https://youtrack.jetbrains.com/issue/KT-62907) Turn on downloading Kotlin Native from maven by default
- [`KT-62795`](https://youtrack.jetbrains.com/issue/KT-62795) CInteropProcess task resolves cinterop def file eagerly, breaking Gradle task dependencies
- [`KT-66982`](https://youtrack.jetbrains.com/issue/KT-66982) Gradle plugin corrupts Native compiler dependencies
- [`KT-66750`](https://youtrack.jetbrains.com/issue/KT-66750) Cannot query the value of task ':commonizeNativeDistribution' property 'kotlinNativeBundleBuildService' because it has no value available
- [`KT-64903`](https://youtrack.jetbrains.com/issue/KT-64903) Add maven repo with dev versions into IT
- [`KT-66422`](https://youtrack.jetbrains.com/issue/KT-66422) Configuration cache breaks during Kotlin Native dependencies downloading
- [`KT-65985`](https://youtrack.jetbrains.com/issue/KT-65985) Race condition during simultaneous execution of several native tasks
- [`KT-51379`](https://youtrack.jetbrains.com/issue/KT-51379) Build fails when using `RepositoriesMode.FAIL_ON_PROJECT_REPOS` with kotlin multiplatform projects
- [`KT-52567`](https://youtrack.jetbrains.com/issue/KT-52567) Use Gradle dependency management for downloading Kotlin/Native compiler when compiling with Gradle
- [`KT-65222`](https://youtrack.jetbrains.com/issue/KT-65222) Native compile task fails after clean reimport
- [`KT-52483`](https://youtrack.jetbrains.com/issue/KT-52483) Sign native prebuilt tars
- [`KT-62800`](https://youtrack.jetbrains.com/issue/KT-62800) CInteropProcess should not require .def file to exist
- [`KT-51255`](https://youtrack.jetbrains.com/issue/KT-51255) Kotlin/Native should not download compiler artifacts when not necessary
- [`KT-62745`](https://youtrack.jetbrains.com/issue/KT-62745) iOS application build is failing if script sandboxing option is enabled in Xcode
- [`KT-61657`](https://youtrack.jetbrains.com/issue/KT-61657) KonanTarget should implement equals or custom serialization
- [`KT-62232`](https://youtrack.jetbrains.com/issue/KT-62232) embedAndSignAppleFrameworkForXcode task is broken with 1.9.20-Beta2
- [`KT-56455`](https://youtrack.jetbrains.com/issue/KT-56455) Gradle: remove `enableEndorsedLibs` from codebase
- [`KT-51553`](https://youtrack.jetbrains.com/issue/KT-51553) Migrate all Kotlin Gradle plugin/Native tests to new test DSL and add CI configuration to run them

### Tools. Incremental Compile

#### New Features

- [`KT-61865`](https://youtrack.jetbrains.com/issue/KT-61865) Add support for incremental compilation within the in-process execution strategy in the build tools api

#### Fixes

- [`KT-61137`](https://youtrack.jetbrains.com/issue/KT-61137) Incremental scripting compilation fails with 2.0
- [`KT-65943`](https://youtrack.jetbrains.com/issue/KT-65943) Incorrect scopeFqName recorded in LookupTracker
- [`KT-56423`](https://youtrack.jetbrains.com/issue/KT-56423) IC: "Cannot access class 'xxx.Foo'. Check your module classpath for missing or conflicting dependencies" in tests and KSP
- [`KT-62101`](https://youtrack.jetbrains.com/issue/KT-62101) IC: Execution failed for ClasspathEntrySnapshotTransform: when using tools.jar as dependency
- [`KT-62686`](https://youtrack.jetbrains.com/issue/KT-62686) K2: Common module sees platform declarations in case of MPP project incremental compilation
- [`KT-63837`](https://youtrack.jetbrains.com/issue/KT-63837) Implement baseline fix for common sources getting access to platform declarations
- [`KT-64513`](https://youtrack.jetbrains.com/issue/KT-64513) Simplify adding configuration properties to incremental compilation
- [`KT-21534`](https://youtrack.jetbrains.com/issue/KT-21534) IC doesn't recompile file with potential SAM-adapter usage
- [`KT-63839`](https://youtrack.jetbrains.com/issue/KT-63839) Measure impact of rebuilding common sources, using nightly IC benchmarks
- [`KT-64228`](https://youtrack.jetbrains.com/issue/KT-64228) K2: After switching to LV20 branch incremental tests are not running on PSI anymore
- [`KT-46743`](https://youtrack.jetbrains.com/issue/KT-46743) Incremental compilation doesn't process usages of Java property in Kotlin code if getter is removed
- [`KT-60522`](https://youtrack.jetbrains.com/issue/KT-60522) Incremental compilation doesn't process usages of Java property in Kotlin code if return type of getter changes
- [`KT-56963`](https://youtrack.jetbrains.com/issue/KT-56963) Add MPP/Jvm incremental compilation tests for both K1 and K2 modes
- [`KT-63876`](https://youtrack.jetbrains.com/issue/KT-63876) Move useful utilities from KmpIncrementalITBase.kt to KGPBaseTest and/or common utils
- [`KT-63010`](https://youtrack.jetbrains.com/issue/KT-63010) Build reports may contain incorrect measurements for "Total size of the cache directory"
- [`KT-59178`](https://youtrack.jetbrains.com/issue/KT-59178) With language version = 2.0 incremental compilation of JVM, JS fails on matching expect and actual declarations
- [`KT-60831`](https://youtrack.jetbrains.com/issue/KT-60831) Fix IncrementalMultiplatformJvmCompilerRunnerTestGenerated in 2.0 branch

### Tools. JPS

- [`KT-65043`](https://youtrack.jetbrains.com/issue/KT-65043) JPS dumb mode should respect maps needed for the compiler
- [`KT-55393`](https://youtrack.jetbrains.com/issue/KT-55393) JPS: Java synthetic properties incremental compilation is broken
- [`KT-63549`](https://youtrack.jetbrains.com/issue/KT-63549) Add compiler performance metrics to JPS build reports
- [`KT-63484`](https://youtrack.jetbrains.com/issue/KT-63484) JPS Kotlin Incremental Compilation Overcaching
- [`KT-62486`](https://youtrack.jetbrains.com/issue/KT-62486) K2 Intellij build: Execution timeout after changes in IC in the K2 QG
- [`KT-60737`](https://youtrack.jetbrains.com/issue/KT-60737) Investigate/fix JPS-related tests in 2.0 migration branch

### Tools. Kapt

#### Fixes

- [`KT-66541`](https://youtrack.jetbrains.com/issue/KT-66541) K2 KAPT: KotlinIllegalArgumentExceptionWithAttachments: Expected expression 'FirPropertyAccessExpressionImpl' to be resolved
- [`KT-64303`](https://youtrack.jetbrains.com/issue/KT-64303) K2 KAPT: Kapt doesn't dispose resources allocated by standalone analysis API
- [`KT-66773`](https://youtrack.jetbrains.com/issue/KT-66773) KAPT: Generated stubs cannot access annotations from other module
- [`KT-65399`](https://youtrack.jetbrains.com/issue/KT-65399) K2 QG: Kapt3 with K2 produces incorrect code
- [`KT-65684`](https://youtrack.jetbrains.com/issue/KT-65684) KAPT: (Re)enable fallback to K1 KAPT and make it default
- [`KT-44706`](https://youtrack.jetbrains.com/issue/KT-44706) KAPT: `@JvmRecord` causes "Record is an API that is part of a preview feature"
- [`KT-59488`](https://youtrack.jetbrains.com/issue/KT-59488) K2: build sphinx-kotlin
- [`KT-64391`](https://youtrack.jetbrains.com/issue/KT-64391) Some K2 Kapt integration tests are being executed with K1
- [`KT-65404`](https://youtrack.jetbrains.com/issue/KT-65404) KAPT should print a warning if stub generation is triggered for an interface with method bodies but without -Xjvm-default=all or -Xjvm-default=all-compatibility
- [`KT-65453`](https://youtrack.jetbrains.com/issue/KT-65453) Kapt4:  error "annotation `@ParameterName` is missing a default value for the element 'name'" for a composable lambda fun without parameters
- [`KT-61080`](https://youtrack.jetbrains.com/issue/KT-61080) Kapt: investigate suspicious check for KMutableMap.Entry in KaptTreeMaker
- [`KT-65006`](https://youtrack.jetbrains.com/issue/KT-65006) [kapt] org.jetbrains.kotlin.utils.exceptions.KotlinIllegalArgumentExceptionWithAttachments: Error while resolving org.jetbrains.kotlin.fir.declarations.impl.FirRegularClassImpl in the K2 QG
- [`KT-64479`](https://youtrack.jetbrains.com/issue/KT-64479) Kapt4 + Compose. Error: scoping construct cannot be annotated with type-use annotation: `@androidx`.compose.runtime.Composable
- [`KT-64719`](https://youtrack.jetbrains.com/issue/KT-64719) K2 KAPT Stub genertaion doesn't fail on files with syntax errors
- [`KT-64680`](https://youtrack.jetbrains.com/issue/KT-64680) Kapt: remove the flag to enable old JVM backend
- [`KT-64639`](https://youtrack.jetbrains.com/issue/KT-64639) KAPT+JVM_IR: erased error types in JvmStatic and JvmOverloads
- [`KT-64389`](https://youtrack.jetbrains.com/issue/KT-64389) K2 KAPT generates invalid code for multiple generic constraints
- [`KT-61776`](https://youtrack.jetbrains.com/issue/KT-61776) K2: KAPT tasks fail with parallel gradle
- [`KT-64021`](https://youtrack.jetbrains.com/issue/KT-64021) Kapt3 + Kapt4. NullPointerException: processingEnv must not be null
- [`KT-64301`](https://youtrack.jetbrains.com/issue/KT-64301) K2 KAPT: Kapt doesn't report invalid enum value names to log
- [`KT-64297`](https://youtrack.jetbrains.com/issue/KT-64297) K2 KAPT: Deprecated members are not marked with `@java`.lang.Deprecated
- [`KT-60821`](https://youtrack.jetbrains.com/issue/KT-60821) [KAPT4] Make sure that KAPT produces correct JCTree; if that's not possible, investigate using JavaPoet as an alternative
- [`KT-62059`](https://youtrack.jetbrains.com/issue/KT-62059) Kapt4IT.kt18799 test fails - cannot find symbol Factory
- [`KT-62097`](https://youtrack.jetbrains.com/issue/KT-62097) K2: [KAPT4]  Keep import statements for unresolved annotation classes
- [`KT-61628`](https://youtrack.jetbrains.com/issue/KT-61628) K2: testAndroidDaggerIC doesn't work with Kapt4
- [`KT-61916`](https://youtrack.jetbrains.com/issue/KT-61916) K2 KAPT. Kapt doesn't generate fully qualified names for annotations used as arguments to other annotations
- [`KT-61729`](https://youtrack.jetbrains.com/issue/KT-61729) K2: KAPT 4: Compiler crash during compilation of Sphinx for Android
- [`KT-61333`](https://youtrack.jetbrains.com/issue/KT-61333) K2 Kapt: support REPORT_OUTPUT_FILES compiler mode
- [`KT-61761`](https://youtrack.jetbrains.com/issue/KT-61761) Kapt4ToolIntegrationTestGenerated should not use Kapt3ComponentRegistrar
- [`KT-59702`](https://youtrack.jetbrains.com/issue/KT-59702) KAPT4: Build sphinx-kotlin using KAPT4

### Tools. Maven

- [`KT-63322`](https://youtrack.jetbrains.com/issue/KT-63322) Add tests for KTIJ-21742
- [`KT-54868`](https://youtrack.jetbrains.com/issue/KT-54868) Stop publishing `kotlin-archetype-js`
- [`KT-60859`](https://youtrack.jetbrains.com/issue/KT-60859) K2: Fix maven `IncrementalCompilationIT` tests in 2.0 branch

### Tools. Parcelize

- [`KT-57685`](https://youtrack.jetbrains.com/issue/KT-57685) Support ImmutableCollections in Parcelize plugin

### Tools. REPL

- [`KT-18355`](https://youtrack.jetbrains.com/issue/KT-18355) REPL doesn't quit on the first line after pressing Ctrl+D or typing :quit

### Tools. Scripts

- [`KT-67727`](https://youtrack.jetbrains.com/issue/KT-67727) Kotlin Scripting with language version 2.0 fails during IR lowering on empty scripts
- [`KT-66395`](https://youtrack.jetbrains.com/issue/KT-66395) K2: Scripting test testHelloSerialization fails on K2
- [`KT-63352`](https://youtrack.jetbrains.com/issue/KT-63352) Scripting dependencies resolver logs "file not found" even if the artefact is retrieved
- [`KT-62400`](https://youtrack.jetbrains.com/issue/KT-62400) K2: Missing annotation resolving for scripts
- [`KT-65865`](https://youtrack.jetbrains.com/issue/KT-65865) K2: Compile scripts in a separate session
- [`KT-65967`](https://youtrack.jetbrains.com/issue/KT-65967) Scripts in common source roots should be forbidden for now
- [`KT-58367`](https://youtrack.jetbrains.com/issue/KT-58367) Remove script-util from the repo

### Tools. Wasm

#### New Features

- [`KT-63417`](https://youtrack.jetbrains.com/issue/KT-63417) KMP hierarchy DSL. Split withWasm() into withWasmJs() and withWasmWasi()
- [`KT-64553`](https://youtrack.jetbrains.com/issue/KT-64553) K/Wasm: enable binaryen by default in production builds

#### Fixes

- [`KT-65864`](https://youtrack.jetbrains.com/issue/KT-65864) K/Wasm: update Node.js to 22.x
- [`KT-67785`](https://youtrack.jetbrains.com/issue/KT-67785) Kotlin/Wasm: Node.JS 22 does not need experimental-wasm-gc flag anymore
- [`KT-66228`](https://youtrack.jetbrains.com/issue/KT-66228) K/Wasm 2.0.0-Beta4 distribution doesn't contain all files
- [`KT-66159`](https://youtrack.jetbrains.com/issue/KT-66159) K/Wasm: applyBinaryen somehow affects skiko.mjs
- [`KT-67086`](https://youtrack.jetbrains.com/issue/KT-67086) K/Wasm: wasi with binaries.library fails on import and build
- [`KT-65889`](https://youtrack.jetbrains.com/issue/KT-65889) wasmJsBrowserDistribution doesn't copy wasm binaries to dist folder
- [`KT-66733`](https://youtrack.jetbrains.com/issue/KT-66733) wasmWasiTest is not compatible with Gradle Configuration Cache
- [`KT-64851`](https://youtrack.jetbrains.com/issue/KT-64851) Wasm. Support Gradle configuration cache
- [`KT-64601`](https://youtrack.jetbrains.com/issue/KT-64601) Indicate that wasmJsBrowserDevelopmentRun has finished bundling
- [`KT-65686`](https://youtrack.jetbrains.com/issue/KT-65686) K/Wasm: Binaryen and d8 have to be downloaded via the same mechanism as Node.js and Yarn
- [`KT-58291`](https://youtrack.jetbrains.com/issue/KT-58291) Wasm:  --tests argument is ignored when running wasmBrowserTest
