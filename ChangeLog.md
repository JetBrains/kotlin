## 2.2.0

### Analysis API

- [`KT-73337`](https://youtrack.jetbrains.com/issue/KT-73337) Migrate analysis sources to new IR parameter API
- [`KT-75880`](https://youtrack.jetbrains.com/issue/KT-75880) K2 Mode: Typealias reference resolves to the underlying class in KMP project
- [`KT-74246`](https://youtrack.jetbrains.com/issue/KT-74246) KaVisibilityChecker.isVisible is inefficient with multiple calls on the same use-site
- [`KT-57733`](https://youtrack.jetbrains.com/issue/KT-57733) Analysis API: Use optimized `ModuleWithDependenciesScope`s in combined symbol providers
- [`KT-69535`](https://youtrack.jetbrains.com/issue/KT-69535) Redesign 'containingSymbol'
- [`KT-69950`](https://youtrack.jetbrains.com/issue/KT-69950) Analysis API: Introduce `isSubtypeOf(ClassId)`
- [`KT-68393`](https://youtrack.jetbrains.com/issue/KT-68393) Analysis API: Rename `KaClassLikeSymbol. classIdIfNonLocal` to `classId`
- [`KT-62924`](https://youtrack.jetbrains.com/issue/KT-62924) Analysis API: rename KtCallableSymbol.callableIdIfNonLocal -> callableId

### Analysis API. Code Compilation

- [`KT-75502`](https://youtrack.jetbrains.com/issue/KT-75502) K2: IDEA hangs when evaluating inside kotlin-stdlib modules in the Kotlin project
- [`KT-73077`](https://youtrack.jetbrains.com/issue/KT-73077) Evaluation of inline functions is broken inside Kotlin project and Amper module in Idea sources
- [`KT-73936`](https://youtrack.jetbrains.com/issue/KT-73936) K2: CyclicInlineDependencyException: Inline functions have a cyclic dependency in evaluator
- [`KT-74582`](https://youtrack.jetbrains.com/issue/KT-74582) InterpreterMethodNotFoundError when trying to evaluate simple expressions after recent fixes
- [`KT-74524`](https://youtrack.jetbrains.com/issue/KT-74524) Compilation exception with incorrect JvmName annotation arguments
- [`KT-74443`](https://youtrack.jetbrains.com/issue/KT-74443) Compilation peer collector ignores inline property accessors

### Analysis API. FIR

#### New Features

- [`KT-73493`](https://youtrack.jetbrains.com/issue/KT-73493) Support context parameters

#### Performance Improvements

- [`KT-75790`](https://youtrack.jetbrains.com/issue/KT-75790) Experiment with increasing DEFAULT_LOCKING_INTERVAL time
- [`KT-72159`](https://youtrack.jetbrains.com/issue/KT-72159) LLFirCompilerRequiredAnnotationsTargetResolver: consider rewriting it to use honest jumping locks

#### Fixes

- [`KT-76331`](https://youtrack.jetbrains.com/issue/KT-76331) Cleanup FileStructureElement for classes
- [`KT-73117`](https://youtrack.jetbrains.com/issue/KT-73117) K2 AA: Exception "Setter is not found" when val has a setter without body
- [`KT-76540`](https://youtrack.jetbrains.com/issue/KT-76540) K2: Missing library dependency on Android SDK from androidx.activity-1.8.2 causes LiveEdit failures
- [`KT-73266`](https://youtrack.jetbrains.com/issue/KT-73266) K2. "Declaration should have non-local container" with unclosed annotation on top-level function
- [`KT-76432`](https://youtrack.jetbrains.com/issue/KT-76432) JavaClassUseSiteMemberScope: Expected FirResolvedTypeRef with ConeKotlinType but was FirUserTypeRefImpl
- [`KT-76217`](https://youtrack.jetbrains.com/issue/KT-76217) K2 AA: "No fir element was found for KtParameter" with multiple context parameter lists
- [`KT-74740`](https://youtrack.jetbrains.com/issue/KT-74740) Highlighting is broken after the built-in serialization refactoring
- [`KT-76366`](https://youtrack.jetbrains.com/issue/KT-76366) ContextCollector: annotations on class members don't have the class as implicit receiver
- [`KT-76352`](https://youtrack.jetbrains.com/issue/KT-76352) ContextCollector: wrong class annotation context in BODY mode
- [`KT-76341`](https://youtrack.jetbrains.com/issue/KT-76341) ContextCollector: support dangling modifiers
- [`KT-76332`](https://youtrack.jetbrains.com/issue/KT-76332) "Declaration should have non-local container" for declaration inside file annotation
- [`KT-76115`](https://youtrack.jetbrains.com/issue/KT-76115) Disable `FirElementBuilder#getFirForElementInsideAnnotations` optimization for files, classes and scripts
- [`KT-76347`](https://youtrack.jetbrains.com/issue/KT-76347) ContextCollector: avoid resolution for enum entry annotations
- [`KT-76272`](https://youtrack.jetbrains.com/issue/KT-76272) Cleanup AbstractFileStructureTest
- [`KT-75542`](https://youtrack.jetbrains.com/issue/KT-75542) K2 AA: "FirDeclaration was not found for class KtNamedFunction, fir is class FirErrorExpressionImpl" for unclosed annotation on member function
- [`KT-73719`](https://youtrack.jetbrains.com/issue/KT-73719) K2. "FirDeclaration was not found for class KtDestructuringDeclaration, fir is class FirBlockImpl" on incorrect chain call
- [`KT-72908`](https://youtrack.jetbrains.com/issue/KT-72908) K2 Analysis API: "FirDeclaration was not found for class org.jetbrains.kotlin.psi.KtFunctionLiteral" with non-local destructuring declaration without initializer before `init` block
- [`KT-75532`](https://youtrack.jetbrains.com/issue/KT-75532) ContextCollector: scope for an anonymous function type parameter contains regular parameters
- [`KT-74508`](https://youtrack.jetbrains.com/issue/KT-74508) `FirElementBuilder#findElementInside` should reuse logic from `KtToFirMapping#getFir`
- [`KT-73066`](https://youtrack.jetbrains.com/issue/KT-73066) [LL] Enable low-level-api-fir-native even with the disabled native part
- [`KT-75132`](https://youtrack.jetbrains.com/issue/KT-75132) Investigate failures of sandbox diagnostic test
- [`KT-75130`](https://youtrack.jetbrains.com/issue/KT-75130) Set up LL FIR tests for sandbox test data
- [`KT-73386`](https://youtrack.jetbrains.com/issue/KT-73386) Standardize LL FIR test for compiler test data
- [`KT-75125`](https://youtrack.jetbrains.com/issue/KT-75125) ISE “Value classes cannot have 0 fields” on instantiating inline class without fields
- [`KT-75179`](https://youtrack.jetbrains.com/issue/KT-75179) ContextCollector: support error properties
- [`KT-74632`](https://youtrack.jetbrains.com/issue/KT-74632) K2: ISE FirLazyDelegatedConstructorCall should be calculated before accessing
- [`KT-74818`](https://youtrack.jetbrains.com/issue/KT-74818) K2 AA: "FirDeclaration was not found for class KtTypeParameter, fir is null" with TYPE_PARAMETERS_NOT_ALLOWED on anonymous function
- [`KT-73183`](https://youtrack.jetbrains.com/issue/KT-73183) Support context parameters in ContextCollectorVisitor
- [`KT-60350`](https://youtrack.jetbrains.com/issue/KT-60350) K2 IDE: top level destructuring RHS should be resolvable
- [`KT-74794`](https://youtrack.jetbrains.com/issue/KT-74794) K2: FirLazyExpression should be calculated before accessing with context parameter and implicit return type
- [`KT-72938`](https://youtrack.jetbrains.com/issue/KT-72938) Get rid of KaFirAnnotationListForReceiverParameter
- [`KT-73727`](https://youtrack.jetbrains.com/issue/KT-73727) Exception in implicit type resolution

### Analysis API. Infrastructure

- [`KT-74917`](https://youtrack.jetbrains.com/issue/KT-74917) [Analysis API, Test Framework] Introduce a way to acquire `PsiFile` for a given `TestFile` in `KtTestModule`

### Analysis API. Light Classes

- [`KT-73405`](https://youtrack.jetbrains.com/issue/KT-73405) Get rid of KtElement#{symbolPointer, symbolPointerOfType} API usages
- [`KT-75391`](https://youtrack.jetbrains.com/issue/KT-75391) Reduce the amount of psi-based logic in light classes
- [`KT-70001`](https://youtrack.jetbrains.com/issue/KT-70001) SLC adds `@Override` with zero text offset on `override` member
- [`KT-75755`](https://youtrack.jetbrains.com/issue/KT-75755) K2. False positive red code on vararg parameters in Kotlin class with `@JvmOverloads` when called from Java
- [`KT-75397`](https://youtrack.jetbrains.com/issue/KT-75397) Constructors and functions with non-last vararg parameters are treated as varargs
- [`KT-74868`](https://youtrack.jetbrains.com/issue/KT-74868) Support context parameters
- [`KT-74733`](https://youtrack.jetbrains.com/issue/KT-74733) SymbolPsiLiteral.text == value for Java constant
- [`KT-74620`](https://youtrack.jetbrains.com/issue/KT-74620) Delegated functions with value classes are present in light classes
- [`KT-74595`](https://youtrack.jetbrains.com/issue/KT-74595) Static functions with value classes are present in light classes
- [`KT-74284`](https://youtrack.jetbrains.com/issue/KT-74284) Synthetic data class methods using value class types present in LC

### Analysis API. Providers and Caches

#### Performance Improvements

- [`KT-62115`](https://youtrack.jetbrains.com/issue/KT-62115) Analysis API: Package providers are not cached per search scope
- [`KT-74463`](https://youtrack.jetbrains.com/issue/KT-74463) Analysis API: `LLNativeForwardDeclarationsSymbolProvider` queries its cache even when the `ClassId` cannot represent a native forward declaration

#### Fixes

- [`KT-74541`](https://youtrack.jetbrains.com/issue/KT-74541) Analysis API: Include files generated by resolve extensions in `KaModule` content scopes
- [`KT-64236`](https://youtrack.jetbrains.com/issue/KT-64236) Analysis API: Introduce a separate module for fallback dependencies of library source modules
- [`KT-74090`](https://youtrack.jetbrains.com/issue/KT-74090) Analysis API: Support dumb mode (restricted analysis)
- [`KT-63780`](https://youtrack.jetbrains.com/issue/KT-63780) Analysis API: Invalidate resolvable library sessions when binary library modules are modified
- [`KT-72388`](https://youtrack.jetbrains.com/issue/KT-72388) KaFirStopWorldCacheCleaner: Control-flow exceptions
- [`KT-74943`](https://youtrack.jetbrains.com/issue/KT-74943) Analysis API: Replace `KotlinGlobalModificationService` with simpler global modification event publishing and listener-based modification trackers
- [`KT-70518`](https://youtrack.jetbrains.com/issue/KT-70518) K2: Analysis API: Access indices outside of `ConcurrentMap` computation in symbol providers
- [`KT-74302`](https://youtrack.jetbrains.com/issue/KT-74302) Analysis API: `LLFirProvider` should disregard self-declarations in `getFirClassifierBy*`
- [`KT-67868`](https://youtrack.jetbrains.com/issue/KT-67868) Analysis API: Improve the architecture of `LLFirKotlinSymbolProvider`s

### Analysis API. Standalone

- [`KT-72810`](https://youtrack.jetbrains.com/issue/KT-72810) withMultiplatformLightClassSupport is inconvenient in Standalone

### Analysis API. Stubs and Decompilation

- [`KT-71787`](https://youtrack.jetbrains.com/issue/KT-71787) `PsiRawFirBuilder.Visitor#visitStringTemplateExpression` forces AST loading
- [`KT-68484`](https://youtrack.jetbrains.com/issue/KT-68484) K2 IDE, Analysis API: "We should be able to find a symbol for function" for getting KaType of `Iterable<T>.map(transform: (T) -> R)` parameter in J2K

### Analysis API. Surface

#### New Features

- [`KT-74475`](https://youtrack.jetbrains.com/issue/KT-74475) Add `isInline` for `KaPropertySymbol`
- [`KT-75063`](https://youtrack.jetbrains.com/issue/KT-75063) KaScopeContext: support context parameters

#### Performance Improvements

- [`KT-73669`](https://youtrack.jetbrains.com/issue/KT-73669) Support psi-based symbol pointer for implicit primary constructors
- [`KT-76008`](https://youtrack.jetbrains.com/issue/KT-76008) Provide PSI-based implementation for `KaFirNamedClassSymbol#companionObject`
- [`KT-70165`](https://youtrack.jetbrains.com/issue/KT-70165) Introduce PSI-based `KaSymbol`s for K2

#### Fixes

- [`KT-72730`](https://youtrack.jetbrains.com/issue/KT-72730) K2: "Unexpected owner function: KtNamedFunction" on vararg val parameter in function
- [`KT-75123`](https://youtrack.jetbrains.com/issue/KT-75123) K2. KaFirNamedFunctionSymbol should contain a receiver
- [`KT-75894`](https://youtrack.jetbrains.com/issue/KT-75894) Cannot build KaFirJavaFieldSymbol for FirFieldImpl
- [`KT-75115`](https://youtrack.jetbrains.com/issue/KT-75115) Analysis API: The `JavaModuleResolver` compiler class is leaked to Analysis API platform implementations
- [`KT-76018`](https://youtrack.jetbrains.com/issue/KT-76018) K2: Stop the wold leads to deadlock/freeze
- [`KT-76011`](https://youtrack.jetbrains.com/issue/KT-76011) `KaFirNamedClassSymbol#companionObject` doesn't provide generated objects generated by compiled plugins
- [`KT-72482`](https://youtrack.jetbrains.com/issue/KT-72482) "KotlinIllegalArgumentExceptionWithAttachments: Expected all candidates to have same callableId but some of them but was different" on trying to add the import
- [`KT-75586`](https://youtrack.jetbrains.com/issue/KT-75586) `KaFirPropertyGetterSymbol#isInline` and `KaFirPropertySetterSymbol#isInline` is incorrect for accessors with explicit modifier
- [`KT-58572`](https://youtrack.jetbrains.com/issue/KT-58572) Analysis API: Enforcing STATUS resolve in 'KtFirNamedClassOrObjectSymbol.visibility' may cause lazy resolve contract violation
- [`KT-75574`](https://youtrack.jetbrains.com/issue/KT-75574) Recognize injected code fragment copies
- [`KT-75573`](https://youtrack.jetbrains.com/issue/KT-75573) Recognize physical file copies as dangling files
- [`KT-74801`](https://youtrack.jetbrains.com/issue/KT-74801) Analysis API: Publish/subscribe to modification events with a single message bus topic
- [`KT-73290`](https://youtrack.jetbrains.com/issue/KT-73290) Analysis API: Improve the architecture of content scopes and resolution scopes
- [`KT-68901`](https://youtrack.jetbrains.com/issue/KT-68901) Constructor delegation call receiver missing in fir implementation
- [`KT-72639`](https://youtrack.jetbrains.com/issue/KT-72639) Support context parameter API
- [`KT-73112`](https://youtrack.jetbrains.com/issue/KT-73112) AA: FirExpression.toKtReceiverValue should handle context receivers properly
- [`KT-74905`](https://youtrack.jetbrains.com/issue/KT-74905) Cannot find context receiver in FIR declaration
- [`KT-74563`](https://youtrack.jetbrains.com/issue/KT-74563) `createPointer` is overloaded not for all implementations
- [`KT-73722`](https://youtrack.jetbrains.com/issue/KT-73722) Analysis API: Automatically check that the API surface is fully documented
- [`KT-65065`](https://youtrack.jetbrains.com/issue/KT-65065) Provide `KtTypeReference#getShortTypeText()`

### Backend. Native. Debug

- [`KT-75991`](https://youtrack.jetbrains.com/issue/KT-75991) Xcode 16.3: Fix lldb stepping test over an inline function

### Backend. Wasm

#### New Features

- [`KT-59032`](https://youtrack.jetbrains.com/issue/KT-59032) Support instantiation of annotation classes on WASM

#### Fixes

- [`KT-77622`](https://youtrack.jetbrains.com/issue/KT-77622) K/Wasm: investigate CMP crash on mobile Safari
- [`KT-76747`](https://youtrack.jetbrains.com/issue/KT-76747) [Wasm] Wasm name section absent for wasm structs
- [`KT-76701`](https://youtrack.jetbrains.com/issue/KT-76701) K/Wasm: custom formatters are not loaded when a project is built with incremental compilation
- [`KT-66081`](https://youtrack.jetbrains.com/issue/KT-66081) K/WASM: `0/0`, `5/0` and `5%0`throw not ArithmeticException, but RuntimeError
- [`KT-76287`](https://youtrack.jetbrains.com/issue/KT-76287) [Wasm] Enable stdlib and kotlin.test tests after compiler bootstrap
- [`KT-75871`](https://youtrack.jetbrains.com/issue/KT-75871) [Wasm] Implement new RTTI approach
- [`KT-75872`](https://youtrack.jetbrains.com/issue/KT-75872) Wasm / IC: IllegalStateException: IC internal error: can not find library
- [`KT-74441`](https://youtrack.jetbrains.com/issue/KT-74441) K/Wasm: incorrect 1e-45.toString()
- [`KT-59118`](https://youtrack.jetbrains.com/issue/KT-59118) WASM: floating point toString inconsistencies
- [`KT-68948`](https://youtrack.jetbrains.com/issue/KT-68948) Wasm: float from variable is printed with many decimal points
- [`KT-69107`](https://youtrack.jetbrains.com/issue/KT-69107) [wasm] Seemingly incorrect rounding
- [`KT-73362`](https://youtrack.jetbrains.com/issue/KT-73362) Migrate K/Wasm sources to new IR parameter API

### Compiler

#### New Features

- [`KT-70722`](https://youtrack.jetbrains.com/issue/KT-70722) Implement better Kotlin warnings for value classes and JEP 390 (Warnings for Value-Based Classes)
- [`KT-71768`](https://youtrack.jetbrains.com/issue/KT-71768) Enable -Xjvm-default=all-compatibility by default to generate JVM default interface methods
- [`KT-54205`](https://youtrack.jetbrains.com/issue/KT-54205) Support jakarta Nullability annotations
- [`KT-57919`](https://youtrack.jetbrains.com/issue/KT-57919) Store all annotations in Kotlin metadata on JVM under a flag
- [`KT-73255`](https://youtrack.jetbrains.com/issue/KT-73255) Change defaulting rule for annotations
- [`KT-74382`](https://youtrack.jetbrains.com/issue/KT-74382) Annotating Java record components for `@JvmRecord` data class
- [`KT-74811`](https://youtrack.jetbrains.com/issue/KT-74811) Prohibit usages of `@MustUseValue` / `@IgnorableValue` if RV checker is not enabled
- [`KT-74806`](https://youtrack.jetbrains.com/issue/KT-74806) Implement feature flag for improved unused return value checker
- [`KT-74809`](https://youtrack.jetbrains.com/issue/KT-74809) Support unnamed local variables
- [`KT-73508`](https://youtrack.jetbrains.com/issue/KT-73508) Add a warning diagnostic for using kotlin.concurrent.AtomicRef<Int>
- [`KT-72941`](https://youtrack.jetbrains.com/issue/KT-72941) ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE missing in K2
- [`KT-74497`](https://youtrack.jetbrains.com/issue/KT-74497) Warn about incompatible Kotlin and Java targets in annotations
- [`KT-75061`](https://youtrack.jetbrains.com/issue/KT-75061) Support context-sensitive resolution in type position
- [`KT-75315`](https://youtrack.jetbrains.com/issue/KT-75315) Support context-sensitive resolution in the call-argument position
- [`KT-75316`](https://youtrack.jetbrains.com/issue/KT-75316) Support context-sensitive resolution for expression-position with expected type
- [`KT-76088`](https://youtrack.jetbrains.com/issue/KT-76088) Support context-sensitive resolution for annotation arguments
- [`KT-74049`](https://youtrack.jetbrains.com/issue/KT-74049) Introduce special override rule to allow overriding T! with T & Any

#### Performance Improvements

- [`KT-76395`](https://youtrack.jetbrains.com/issue/KT-76395) Performance degradation on 28.03.2025
- [`KT-76422`](https://youtrack.jetbrains.com/issue/KT-76422) FirJavaFacade#createFirJavaClass: do not compute super type references right away
- [`KT-75957`](https://youtrack.jetbrains.com/issue/KT-75957) K2: PsiRawFirBuilder.Visitor#toFirExpression forces AST loading via getSpreadElement
- [`KT-74824`](https://youtrack.jetbrains.com/issue/KT-74824) Exponential performance caused by nested flexible types
- [`KT-62855`](https://youtrack.jetbrains.com/issue/KT-62855) K2: extra allocation for SAM conversion compared to K1
- [`KT-74977`](https://youtrack.jetbrains.com/issue/KT-74977) K/N: support stack array for Array(size) call
- [`KT-74369`](https://youtrack.jetbrains.com/issue/KT-74369) Exponential compiler memory usage in specific situations with type inference

#### Fixes

- [`KT-76606`](https://youtrack.jetbrains.com/issue/KT-76606) Enable 'Indy: Allow lambdas with annotations' by default
- [`KT-77301`](https://youtrack.jetbrains.com/issue/KT-77301) False positive Context Parameter resolution when using DslMarker
- [`KT-74389`](https://youtrack.jetbrains.com/issue/KT-74389) K2: False positive NON_EXPORTABLE_TYPE on non-Unit `Promise<...>` in K/JS
- [`KT-77219`](https://youtrack.jetbrains.com/issue/KT-77219) "`@Composable` annotation is not applicable" on vararg `@Composable` () -> Unit in Kotlin 2.2.0
- [`KT-76357`](https://youtrack.jetbrains.com/issue/KT-76357) K2: a nested class annotation observes member declarations of the outer class
- [`KT-72734`](https://youtrack.jetbrains.com/issue/KT-72734) Support new callable reference nodes in Kotlin Native
- [`KT-74421`](https://youtrack.jetbrains.com/issue/KT-74421) K2: Missing "val cannot be reassigned" when trying to assign a value to parent's "val"
- [`KT-63720`](https://youtrack.jetbrains.com/issue/KT-63720) Coroutine debugger: do not optimise out local variables
- [`KT-74470`](https://youtrack.jetbrains.com/issue/KT-74470) NSME on calling in runtime internal constructor of value class with default arg from tests
- [`KT-77640`](https://youtrack.jetbrains.com/issue/KT-77640) Context parameters: using 'contextOf()' function leads to [NO_CONTEXT_ARGUMENT]
- [`KT-73909`](https://youtrack.jetbrains.com/issue/KT-73909) Add an inspection discouraging usage of kotlin.concurrent Native atomics in favor of the new atomics
- [`KT-76583`](https://youtrack.jetbrains.com/issue/KT-76583) CCE: suspend lambda attempts to unbox value class parameter twice after lambda suspended
- [`KT-76663`](https://youtrack.jetbrains.com/issue/KT-76663) KJS: KotlinNothingValueException caused by expression return since 2.1.20
- [`KT-75457`](https://youtrack.jetbrains.com/issue/KT-75457) Native: cache machinery uses stdlib cache with default runtime options even if custom runtime options are supplied when partial linkage is disabled
- [`KT-76615`](https://youtrack.jetbrains.com/issue/KT-76615) K2: "IllegalArgumentException: Inline class types should have the same representation: Lkotlin/UByte; != B" for mixed Java/Kotlin code
- [`KT-77220`](https://youtrack.jetbrains.com/issue/KT-77220) Annotation with EXPRESSION is not allowed on lambdas in Kotlin 2.2.0
- [`KT-76381`](https://youtrack.jetbrains.com/issue/KT-76381) K2: Expected expression 'FirPropertyAccessExpressionImpl' to be resolved
- [`KT-74739`](https://youtrack.jetbrains.com/issue/KT-74739) Native: "IllegalArgumentException: All constructors should've been lowered: FUNCTION_REFERENCE"
- [`KT-74325`](https://youtrack.jetbrains.com/issue/KT-74325) Explicit API mode does not enforce explicit return types for extension properties
- [`KT-77259`](https://youtrack.jetbrains.com/issue/KT-77259) Confusing message for `ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD`
- [`KT-73771`](https://youtrack.jetbrains.com/issue/KT-73771) K2: Infinite compilation caused by buildList without type
- [`KT-61258`](https://youtrack.jetbrains.com/issue/KT-61258) Kotlin/Native: CLASS CLASS name:<no name provided> modality:FINAL visibility:local superTypes:[<root>.Base]
- [`KT-75317`](https://youtrack.jetbrains.com/issue/KT-75317) Kotlin/Native: segfault in kotlin::gc::Mark<kotlin::gc::mark::ConcurrentMark::MarkTraits>
- [`KT-75965`](https://youtrack.jetbrains.com/issue/KT-75965) The iOS app did not run successfully in Release mode
- [`KT-77397`](https://youtrack.jetbrains.com/issue/KT-77397) Report UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL when calling declaration with contextual function type in signature
- [`KT-77137`](https://youtrack.jetbrains.com/issue/KT-77137) K2: Controversial behavior allows resolving annotation arguments on a companion inside it
- [`KT-77150`](https://youtrack.jetbrains.com/issue/KT-77150) Native: compilation fails with an assertion error
- [`KT-51960`](https://youtrack.jetbrains.com/issue/KT-51960) ClassCastException: Inline function with both context and extension receiver produces this when invoked
- [`KT-73611`](https://youtrack.jetbrains.com/issue/KT-73611) Remove -Xextended-compiler-checks in favor of a deprecation cycle
- [`KT-74649`](https://youtrack.jetbrains.com/issue/KT-74649) Deprecate language versions 1.8 and 1.9
- [`KT-77283`](https://youtrack.jetbrains.com/issue/KT-77283) Binary compatibility of FirDeclarationChecker
- [`KT-73445`](https://youtrack.jetbrains.com/issue/KT-73445) K2: do not report "cannot infer visibility" when inheriting multiple implementations
- [`KT-75945`](https://youtrack.jetbrains.com/issue/KT-75945) Indy: Allow lambdas with annotations
- [`KT-76898`](https://youtrack.jetbrains.com/issue/KT-76898) K2: ClassCastException when data class shadows supertype's `componentX` method with wrong type
- [`KT-75992`](https://youtrack.jetbrains.com/issue/KT-75992) Xcode 16.3: stacktraces on simulators are not symbolicated
- [`KT-76805`](https://youtrack.jetbrains.com/issue/KT-76805) Wrong NPE occurs when assigning synthetic properties with platform types in Kotlin 2.1.20
- [`KT-76171`](https://youtrack.jetbrains.com/issue/KT-76171) "KotlinIllegalArgumentExceptionWithAttachments: Expected expression 'FirSingleExpressionBlock' to be resolved"
- [`KT-77078`](https://youtrack.jetbrains.com/issue/KT-77078) K2: anonymous object is wrongly allowed to implement interfaces by unsafe Delegation
- [`KT-72722`](https://youtrack.jetbrains.com/issue/KT-72722) Treat 'copy' calls of a data class as explicit constructor usages
- [`KT-77001`](https://youtrack.jetbrains.com/issue/KT-77001) Leave ForbidParenthesizedLhsInAssignments as a warning
- [`KT-75828`](https://youtrack.jetbrains.com/issue/KT-75828) Store backing field/delegate annotations and extension receiver annotations in metadata
- [`KT-58369`](https://youtrack.jetbrains.com/issue/KT-58369) K2: enable DFA warnings
- [`KT-51258`](https://youtrack.jetbrains.com/issue/KT-51258) Annotations should go before context receivers
- [`KT-76253`](https://youtrack.jetbrains.com/issue/KT-76253) K2 Compiler: Less precise diagnostic COMPONENT_FUNCTION_AMBIGUITY for flexible type
- [`KT-59526`](https://youtrack.jetbrains.com/issue/KT-59526) Store annotation default values in metadata on JVM
- [`KT-63850`](https://youtrack.jetbrains.com/issue/KT-63850) K2: setter with an annotated parameter has `isNotDefault == false` flag in metadata
- [`KT-75712`](https://youtrack.jetbrains.com/issue/KT-75712) -Wextra: false positive UNUSED_LAMBDA_EXPRESSION on functional type variable assignment with inferred type
- [`KT-4779`](https://youtrack.jetbrains.com/issue/KT-4779) Generate default methods for implementations in interfaces
- [`KT-69624`](https://youtrack.jetbrains.com/issue/KT-69624) Debugger: Missing local variable in Variables view (inline function)
- [`KT-75518`](https://youtrack.jetbrains.com/issue/KT-75518) NO_CONTEXT_ARGUMENT should report the name of the context parameter in addition to the type
- [`KT-76199`](https://youtrack.jetbrains.com/issue/KT-76199) Introduce -Xcontext-sensitive-resolution compiler flag
- [`KT-75553`](https://youtrack.jetbrains.com/issue/KT-75553) `MISSING_DEPENDENCY_SUPERCLASS` and `MISSING_DEPENDENCY_SUPERCLASS_WARNING` is reported at the same time on the same element
- [`KT-76159`](https://youtrack.jetbrains.com/issue/KT-76159) Obsolete error "'`@JvmDefaultWithCompatibility`' annotation is only allowed on interfaces" should be removed
- [`KT-76660`](https://youtrack.jetbrains.com/issue/KT-76660) False negative RETURN_NOT_ALLOWED in lambda in default argument leads to NoClassDefFoundError: $$$$$NON_LOCAL_RETURN$$$$$
- [`KT-76301`](https://youtrack.jetbrains.com/issue/KT-76301) Fail to infer types after syntactical change
- [`KT-74999`](https://youtrack.jetbrains.com/issue/KT-74999) K2: KotlinNothingValueException within Extension Function
- [`KT-76675`](https://youtrack.jetbrains.com/issue/KT-76675) KIAEWA exception at KaFirDataFlowProvider with non-local return from nested inline call
- [`KT-75756`](https://youtrack.jetbrains.com/issue/KT-75756) Backend Internal error: Exception during IR lowering when trying to access variable from providedProperties in class within kotlin custom script
- [`KT-76345`](https://youtrack.jetbrains.com/issue/KT-76345) Enhance variable fixation
- [`KT-76578`](https://youtrack.jetbrains.com/issue/KT-76578) [FIR, K1/K2 Regression] `lateinit` is allowed on loop parameters
- [`KT-76448`](https://youtrack.jetbrains.com/issue/KT-76448) FirOverrideChecker: class ClsMethodImpl is not a subtype of class KtNamedDeclaration for factory VIRTUAL_MEMBER_HIDDEN
- [`KT-73360`](https://youtrack.jetbrains.com/issue/KT-73360) Migrate K/JVM sources to new IR parameter API
- [`KT-74852`](https://youtrack.jetbrains.com/issue/KT-74852) Kotlin/Native: allow caches for thread state checker and sanitizers
- [`KT-76130`](https://youtrack.jetbrains.com/issue/KT-76130) IR evaluator does not support array literals in annotation parameter default values
- [`KT-76436`](https://youtrack.jetbrains.com/issue/KT-76436) Missing K2 checker: non-local return through lambda passed to inline f/o
- [`KT-74326`](https://youtrack.jetbrains.com/issue/KT-74326) False negative: no variable must be initialized error though code doesn't compile
- [`KT-76572`](https://youtrack.jetbrains.com/issue/KT-76572) FIR_NON_SUPPRESSIBLE_ERROR_NAMES does not contain deprecation errors
- [`KT-75704`](https://youtrack.jetbrains.com/issue/KT-75704) Refactor `FirWhenSubjectExpression`
- [`KT-76284`](https://youtrack.jetbrains.com/issue/KT-76284) Flexible captured type is not approximated in receiver position
- [`KT-76192`](https://youtrack.jetbrains.com/issue/KT-76192) RETURN_TYPE_MISMATCH with same expected and actual type: nullability of actual type is omitted
- [`KT-75944`](https://youtrack.jetbrains.com/issue/KT-75944) Allow using invokedynamic for lambdas with no 'Runtime' level retention annotations
- [`KT-76396`](https://youtrack.jetbrains.com/issue/KT-76396) FirIntegerConstantOperatorScope: NoSuchElementException: Collection contains no element matching the predicate
- [`KT-76209`](https://youtrack.jetbrains.com/issue/KT-76209) CONFLICTING_UPPER_BOUNDS on `Nothing` bound
- [`KT-59506`](https://youtrack.jetbrains.com/issue/KT-59506) Context receivers: Unable to use trailing comma in receiver list
- [`KT-46119`](https://youtrack.jetbrains.com/issue/KT-46119) NONE_APPLICABLE instead of NAMED_ARGUMENTS_NOT_ALLOWED with overloaded Java constructor call
- [`KT-75503`](https://youtrack.jetbrains.com/issue/KT-75503) Run lazy resolution in CallableCopyTypeCalculator and use withForcedTypeCalculator everywhere in checkers
- [`KT-76485`](https://youtrack.jetbrains.com/issue/KT-76485) Don't report EXTENSION_SHADOWED_BY_MEMBER if extension can be called with named arguments
- [`KT-76154`](https://youtrack.jetbrains.com/issue/KT-76154) False positive "EXTENSION_SHADOWED_BY_MEMBER" when extension adds default values to parameters
- [`KT-76527`](https://youtrack.jetbrains.com/issue/KT-76527) False positive UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL with -Xcontext-receivers and implicit invoke
- [`KT-63246`](https://youtrack.jetbrains.com/issue/KT-63246) K2: False positive NOTHING_TO_OVERRIDE in generic property with context receiver in non generic class extending generic class
- [`KT-58534`](https://youtrack.jetbrains.com/issue/KT-58534) K2: "Argument type mismatch" with typealias to context receiver functional type
- [`KT-71792`](https://youtrack.jetbrains.com/issue/KT-71792) Switch latest stable version in Kotlin project to 2.2
- [`KT-74827`](https://youtrack.jetbrains.com/issue/KT-74827) CompilationErrorException : Could not load module in an attempt to find deserializer when trying to evaluate an expression
- [`KT-70352`](https://youtrack.jetbrains.com/issue/KT-70352) K2: False-negative CONFLICTING_UPPER_BOUNDS on `Nothing` bound
- [`KT-71481`](https://youtrack.jetbrains.com/issue/KT-71481) K2: drop pre-1.6 language features from compiler code
- [`KT-74454`](https://youtrack.jetbrains.com/issue/KT-74454) Support trailing comma in context parameters
- [`KT-74069`](https://youtrack.jetbrains.com/issue/KT-74069) False positive UNUSED_EXPRESSION due to Long/Int conversion
- [`KT-74337`](https://youtrack.jetbrains.com/issue/KT-74337) Local Delegated properties don't preserve their annotations and don't show up in reflection
- [`KT-55187`](https://youtrack.jetbrains.com/issue/KT-55187) Context receivers in function types can have labels
- [`KT-58498`](https://youtrack.jetbrains.com/issue/KT-58498) Context receivers: ClassCastException with object and extension function in interface
- [`KT-58165`](https://youtrack.jetbrains.com/issue/KT-58165) K2: "IllegalArgumentException: No argument for parameter VALUE_PARAMETER" on overridden contextual property
- [`KT-75234`](https://youtrack.jetbrains.com/issue/KT-75234) Add error for callsInPlace contracts on context parameters
- [`KT-73805`](https://youtrack.jetbrains.com/issue/KT-73805) K2: Investigate missing diagnostic in implicit invoke call on context function type with receiver from module with disabled context parameters
- [`KT-41934`](https://youtrack.jetbrains.com/issue/KT-41934) NI: a type variable for lambda parameter has been inferred to nullable type instead of not null one
- [`KT-75983`](https://youtrack.jetbrains.com/issue/KT-75983) Backend Internal error: Exception during IR lowering 'IllegalStateException: Internal error: cannot convert Any to Int'
- [`KT-75535`](https://youtrack.jetbrains.com/issue/KT-75535) Compilation of typealias does not check for clashes
- [`KT-72313`](https://youtrack.jetbrains.com/issue/KT-72313) K2 IDE / KMP Debugger: Evaluation of inline functions declared in a common source set causes a crash
- [`KT-76290`](https://youtrack.jetbrains.com/issue/KT-76290) False positive UNUSED_EXPRESSION while returning Unit in the when branches
- [`KT-32358`](https://youtrack.jetbrains.com/issue/KT-32358) NI: Smart cast doesn't work with inline function after elvis operator
- [`KT-76316`](https://youtrack.jetbrains.com/issue/KT-76316) K2: Missing NON_PUBLIC_CALL_FROM_PUBLIC_INLINE on object extending private class in public inline function
- [`KT-76324`](https://youtrack.jetbrains.com/issue/KT-76324) Frontend diagnostic says "... this will be an error in Kotlin N.M" but N.M is already released
- [`KT-76058`](https://youtrack.jetbrains.com/issue/KT-76058) PCLA: compile-time failure on calling a higher-order function from another module inside a lambda assigned to a variable of a type with a postponed type variable
- [`KT-75571`](https://youtrack.jetbrains.com/issue/KT-75571) K2: type mismatch error provides unsubstituted types
- [`KT-31391`](https://youtrack.jetbrains.com/issue/KT-31391) 'Recursive call is not a tail call' with elvis operator in tailrec function
- [`KT-73420`](https://youtrack.jetbrains.com/issue/KT-73420) False-positive `NON_TAIL_RECURSIVE_CALL` on tailrec function with elvis in the return statement
- [`KT-75815`](https://youtrack.jetbrains.com/issue/KT-75815) Disable warnings about different context parameter names in overrides
- [`KT-75169`](https://youtrack.jetbrains.com/issue/KT-75169) Unnecessary EXTENSION_SHADOWED_BY_MEMBER on generic declarations
- [`KT-75483`](https://youtrack.jetbrains.com/issue/KT-75483) Native: redundant unboxing generated with smart cast
- [`KT-76339`](https://youtrack.jetbrains.com/issue/KT-76339) K2: Dangling modifier list is missed for enum entries in PSI mode
- [`KT-75513`](https://youtrack.jetbrains.com/issue/KT-75513) Avoid overrides traversal without preinitialization
- [`KT-74587`](https://youtrack.jetbrains.com/issue/KT-74587) Report an error when JvmDefaultWithoutCompatibility is used with -Xjvm-default=all
- [`KT-76257`](https://youtrack.jetbrains.com/issue/KT-76257) Annotations with class references are not supported when marking IR declarations as visible to metadata
- [`KT-71793`](https://youtrack.jetbrains.com/issue/KT-71793) Drop language versions 1.6 and 1.7
- [`KT-59272`](https://youtrack.jetbrains.com/issue/KT-59272) Incorrect bytecode generated: wrong line number table after condition
- [`KT-69248`](https://youtrack.jetbrains.com/issue/KT-69248) K2: IAE “class KtDotQualifiedExpression is not a subtype of class KtCallExpression for factory ENUM_CLASS_CONSTRUCTOR_CALL” with qualified enum constructor call
- [`KT-73778`](https://youtrack.jetbrains.com/issue/KT-73778) Kotlin Debugger: NSFE on accessing private property from dependencies during evaluation
- [`KT-74131`](https://youtrack.jetbrains.com/issue/KT-74131) Incorrect line numbers for static initializer with delegated local variable
- [`KT-76320`](https://youtrack.jetbrains.com/issue/KT-76320) K2: PsiRawFirBuilder: import alias triggers ast loading
- [`KT-63851`](https://youtrack.jetbrains.com/issue/KT-63851) K2: No `setterValueParameter` in metadata for property setter with an annotated parameter
- [`KT-55083`](https://youtrack.jetbrains.com/issue/KT-55083) JVM: AbstractMethodError caused by lambda with sealed base interface and fun sub interface and overridden method
- [`KT-16727`](https://youtrack.jetbrains.com/issue/KT-16727) Names for anonymous classes in interfaces are malformed on JDK 8
- [`KT-12466`](https://youtrack.jetbrains.com/issue/KT-12466) NoClassDefFoundError: B$DefaultImpls on super interface call through K-J-K inheritance
- [`KT-71002`](https://youtrack.jetbrains.com/issue/KT-71002) Possible inheritance from nullable type through typealias
- [`KT-75293`](https://youtrack.jetbrains.com/issue/KT-75293) K2: Missing [HAS_NEXT_FUNCTION_TYPE_MISMATCH] diagnostics
- [`KT-75498`](https://youtrack.jetbrains.com/issue/KT-75498) Forbid .declarations access from checkers
- [`KT-72335`](https://youtrack.jetbrains.com/issue/KT-72335) KotlinIllegalArgumentExceptionWithAttachments when using illegal selector
- [`KT-68375`](https://youtrack.jetbrains.com/issue/KT-68375) K2: FirPrimaryConstructorSuperTypeChecker fails on generated superclasses
- [`KT-71718`](https://youtrack.jetbrains.com/issue/KT-71718) K2: drop TypePreservingVisibilityWrtHack
- [`KT-75112`](https://youtrack.jetbrains.com/issue/KT-75112) FE resolves wrong receivers order for property passed to delegate
- [`KT-75924`](https://youtrack.jetbrains.com/issue/KT-75924) K2. Incorrect generic type Inference "R? & Any" appears for "Add explicit type arguments"
- [`KT-75969`](https://youtrack.jetbrains.com/issue/KT-75969) java.lang.IllegalArgumentException: source must not be null on red code
- [`KT-75322`](https://youtrack.jetbrains.com/issue/KT-75322) ConeDiagnosticToFirDiagnosticKt: source must not be null
- [`KT-73800`](https://youtrack.jetbrains.com/issue/KT-73800) Wrong method executed on super call in -Xjvm-default=all/all-compatibility with an extraneous super-interface
- [`KT-38029`](https://youtrack.jetbrains.com/issue/KT-38029) Wrong method executed on super call in diamond hierarchy with covariant override
- [`KT-75242`](https://youtrack.jetbrains.com/issue/KT-75242) Any use-site target can be applied to a lambda and an expression
- [`KT-73051`](https://youtrack.jetbrains.com/issue/KT-73051) incorrect direction of subtyping violation in type mismatch error's message for A<X<C>> </: A<Y<Tv>> given a Tv <: Rv == C constraint from a lambda return position
- [`KT-75090`](https://youtrack.jetbrains.com/issue/KT-75090) Argument type mismatch: actual type is 'SuspendFunction0<Unit>', but 'SuspendFunction0<Unit>' was expected when anonymous function is passed to function expecting suspend function type
- [`KT-74956`](https://youtrack.jetbrains.com/issue/KT-74956) K2: No USAGE_IS_NOT_INLINABLE with compiling an inlined function call
- [`KT-76049`](https://youtrack.jetbrains.com/issue/KT-76049) K2: drop explicitTypeArgumentIfMadeFlexibleSynthetically creation when DontMakeExplicitJavaTypeArgumentsFlexible is enabled
- [`KT-76055`](https://youtrack.jetbrains.com/issue/KT-76055) K2: drop prepareCustomReturnTypeSubstitutorForFunctionCall logic when DontMakeExplicitJavaTypeArgumentsFlexible is enabled
- [`KT-76057`](https://youtrack.jetbrains.com/issue/KT-76057) K2: don't do reverse Java overridability checks when DontMakeExplicitJavaTypeArgumentsFlexible is enabled
- [`KT-75197`](https://youtrack.jetbrains.com/issue/KT-75197) K2: Missing [COMPARE_TO_TYPE_MISMATCH] diagnostics
- [`KT-75639`](https://youtrack.jetbrains.com/issue/KT-75639) Inline `context` function leads to `ClassCastException`
- [`KT-75677`](https://youtrack.jetbrains.com/issue/KT-75677) K2: change runtime behavior of KT-75649 case in 2.2
- [`KT-75961`](https://youtrack.jetbrains.com/issue/KT-75961) K2: `PsiRawFirBuilder.Visitor#visitSimpleNameExpression`forces AST loading via `getReferencedNameElement().node.text`
- [`KT-67869`](https://youtrack.jetbrains.com/issue/KT-67869) Make inference for lambda working consistently inside and outside of the call
- [`KT-74885`](https://youtrack.jetbrains.com/issue/KT-74885) K2: IAE "source must not be null" in FirCyclicTypeBoundsChecker
- [`KT-75578`](https://youtrack.jetbrains.com/issue/KT-75578) K2: False negative [SUPER_CALL_WITH_DEFAULT_PARAMETERS] when calling the upper-class implementation of a method with the default value argument
- [`KT-73954`](https://youtrack.jetbrains.com/issue/KT-73954) Generate implementations in classes for inherited non-abstract methods in -Xjvm-default=all-compatibility
- [`KT-75173`](https://youtrack.jetbrains.com/issue/KT-75173) Context parameters: KotlinIllegalArgumentExceptionWithAttachments if you override function with value/extension parameter by fun with context
- [`KT-75742`](https://youtrack.jetbrains.com/issue/KT-75742) Native: "IllegalArgumentException: unknown pass name '' " when specifying an empty list of LLVM passes
- [`KT-74819`](https://youtrack.jetbrains.com/issue/KT-74819) K2: False-positive overload resolution ambiguity for flatMap inside PCLA
- [`KT-75093`](https://youtrack.jetbrains.com/issue/KT-75093) K2 IDE: "Unreachable code" highlighting range is confusing
- [`KT-74572`](https://youtrack.jetbrains.com/issue/KT-74572) Context parameters: contracts don't work with context parameters
- [`KT-74765`](https://youtrack.jetbrains.com/issue/KT-74765) Move K1 lazy IR implementation from 'ir.tree' to 'psi2ir'
- [`KT-71425`](https://youtrack.jetbrains.com/issue/KT-71425) IR Inliner: investigate return type of an inlined block
- [`KT-74764`](https://youtrack.jetbrains.com/issue/KT-74764) Native: merge init nodes generated within the same LLVM module for the same klib
- [`KT-75561`](https://youtrack.jetbrains.com/issue/KT-75561) K/N: place InteropLowering after UpgradeCallableReferences phase
- [`KT-73369`](https://youtrack.jetbrains.com/issue/KT-73369) K/N: move interop lowering up the pipeline
- [`KT-75517`](https://youtrack.jetbrains.com/issue/KT-75517) K2: Refactor FirCallableSymbol.resolvedContextParameters to return symbols
- [`KT-75821`](https://youtrack.jetbrains.com/issue/KT-75821) K2: REPL resolution doesn't take into account the property type when processing its initializer
- [`KT-75705`](https://youtrack.jetbrains.com/issue/KT-75705) IllegalArgumentException when isInitialized is used with java field
- [`KT-75334`](https://youtrack.jetbrains.com/issue/KT-75334) Java target shouldn't be specified if Kotlin target isn't specified
- [`KT-75157`](https://youtrack.jetbrains.com/issue/KT-75157) Missing PARAMETER_NAME_CHANGED_ON_OVERRIDE and DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES for context parameters
- [`KT-75160`](https://youtrack.jetbrains.com/issue/KT-75160) Check usages of value parameters in checkers and adapt to context parameters
- [`KT-75729`](https://youtrack.jetbrains.com/issue/KT-75729) KtPsiFactory: no type-safe way to create triple-quoted KtStringTemplateExpression
- [`KT-75040`](https://youtrack.jetbrains.com/issue/KT-75040) Unify `subject` and `subjectVariable` in `FirWhenExpression`
- [`KT-75323`](https://youtrack.jetbrains.com/issue/KT-75323) FirSyntheticProperty: Unexpected status. Expected is FirResolvedDeclarationStatus, but was FirDeclarationStatusImpl
- [`KT-75602`](https://youtrack.jetbrains.com/issue/KT-75602) Introduce concept of shared library session in Fir sessions
- [`KT-75509`](https://youtrack.jetbrains.com/issue/KT-75509) PARAMETER_NAME_CHANGED_ON_OVERRIDE is reported randomly
- [`KT-75124`](https://youtrack.jetbrains.com/issue/KT-75124) IAE “class org.jetbrains.kotlin.psi.KtContextReceiver is not a subtype of class org.jetbrains.kotlin.psi.KtParameter for factory EXPOSED_PARAMETER_TYPE” on private context receiver
- [`KT-73585`](https://youtrack.jetbrains.com/issue/KT-73585) K2: ABSTRACT_SUPER_CALL is not reported
- [`KT-75531`](https://youtrack.jetbrains.com/issue/KT-75531) K2 REPL: local name doesn't shadow one from implicit receiver
- [`KT-73359`](https://youtrack.jetbrains.com/issue/KT-73359) Migrate frontend sources to new IR parameter API
- [`KT-75380`](https://youtrack.jetbrains.com/issue/KT-75380) K2: Modality is configured incorrectly for some FirDefaultPropertyAccessor
- [`KT-75526`](https://youtrack.jetbrains.com/issue/KT-75526) Regression in K2 scripting: local name doesn't shadow one from the implicit receiver
- [`KT-59379`](https://youtrack.jetbrains.com/issue/KT-59379) K2: Missing MIXING_NAMED_AND_POSITIONED_ARGUMENTS
- [`KT-75106`](https://youtrack.jetbrains.com/issue/KT-75106) K2: type parameters of anonymous functions are unresolved
- [`KT-73387`](https://youtrack.jetbrains.com/issue/KT-73387) Unexpected implicit type during enhancement
- [`KT-72618`](https://youtrack.jetbrains.com/issue/KT-72618) Cannot define operator inc/dec in class context
- [`KT-74546`](https://youtrack.jetbrains.com/issue/KT-74546) Serialize context parameters to metadata
- [`KT-68768`](https://youtrack.jetbrains.com/issue/KT-68768) K2: unsuccessful inference fork with jspecify annotations
- [`KT-75345`](https://youtrack.jetbrains.com/issue/KT-75345) Add a test for KT-42271
- [`KT-75012`](https://youtrack.jetbrains.com/issue/KT-75012) K2: Compiler crash on `dynamic == null`
- [`KT-75195`](https://youtrack.jetbrains.com/issue/KT-75195) IllegalStateException: No value for annotation parameter when `@all` meta-target is used with annotation with constructor
- [`KT-75163`](https://youtrack.jetbrains.com/issue/KT-75163) WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET for `@all` meta-target although there are applicable targets
- [`KT-75198`](https://youtrack.jetbrains.com/issue/KT-75198) `@all` meta-target should be forbidden for delegated properties
- [`KT-74958`](https://youtrack.jetbrains.com/issue/KT-74958) K2: UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE isn't reported on accidental trailing closure
- [`KT-74982`](https://youtrack.jetbrains.com/issue/KT-74982) Improve UNSUPPORTED message handling
- [`KT-75111`](https://youtrack.jetbrains.com/issue/KT-75111) False negative "This declaration needs opt-in" for usage of enum entry with OptIn marker in another module
- [`KT-74924`](https://youtrack.jetbrains.com/issue/KT-74924) Infinite recursion in substitution of captured type with recursive supertype
- [`KT-75289`](https://youtrack.jetbrains.com/issue/KT-75289) NPE: getParent(...) must not be null
- [`KT-75275`](https://youtrack.jetbrains.com/issue/KT-75275) Inline class member inherited from interface is not mangled in '-Xjvm-default=all-compatibility'
- [`KT-74340`](https://youtrack.jetbrains.com/issue/KT-74340) FIR: folding binary expression chains for psi parser
- [`KT-73831`](https://youtrack.jetbrains.com/issue/KT-73831) Do not choose `field` target in annotation classes
- [`KT-73494`](https://youtrack.jetbrains.com/issue/KT-73494) Enable first-only-warn annotation defaulting mode
- [`KT-75174`](https://youtrack.jetbrains.com/issue/KT-75174) K2: incorrect influence of return type nullability on required receiver type in KJK hierarchy with property
- [`KT-74920`](https://youtrack.jetbrains.com/issue/KT-74920) Overriding T! with T & Any is not allowed to the extension property receiver type
- [`KT-75150`](https://youtrack.jetbrains.com/issue/KT-75150) False ambiguous context parameter reported because context is not chosen via generic parameter
- [`KT-74965`](https://youtrack.jetbrains.com/issue/KT-74965) CLI compiler doesn't report syntax errors for JS, Metadata backends if light-tree mode is disabled
- [`KT-74303`](https://youtrack.jetbrains.com/issue/KT-74303) K2 IDE / Kotlin Debugger: AE “Trying to inline an anonymous object which is not part of the public ABI” on evaluating private inline function with object inside
- [`KT-75177`](https://youtrack.jetbrains.com/issue/KT-75177) NoSuchMethodError on suspend default interface method fake override returning inline class in -Xjvm-default=all-compatibility
- [`KT-74718`](https://youtrack.jetbrains.com/issue/KT-74718) K/N: Move TestProcessor phase to the top of the pipeline
- [`KT-75015`](https://youtrack.jetbrains.com/issue/KT-75015) Context parameters: it is possible to declare anonymous function with modifiers but they don't have any effect
- [`KT-75092`](https://youtrack.jetbrains.com/issue/KT-75092) K2: Missing errors for modifiers on anonymous function in statement position
- [`KT-75009`](https://youtrack.jetbrains.com/issue/KT-75009) Context parameters: context is unresolved inside anonymous function if passed as an argument
- [`KT-75017`](https://youtrack.jetbrains.com/issue/KT-75017) Context parameters: "IllegalStateException: Cannot find variable a: R|kotlin/String| in local storage " when context from another local function is called
- [`KT-75154`](https://youtrack.jetbrains.com/issue/KT-75154) Context receiver deprecation warning should depend on langauge version, not on LATEST_STABLE
- [`KT-74979`](https://youtrack.jetbrains.com/issue/KT-74979) Context parameters: anonymous functions with a context aren't parsed in complex cases
- [`KT-74673`](https://youtrack.jetbrains.com/issue/KT-74673) K2: ClassCastException when passing suspending functional interface with generic
- [`KT-74469`](https://youtrack.jetbrains.com/issue/KT-74469) K2: False positive: "Argument type mismatch" during Java interop
- [`KT-75105`](https://youtrack.jetbrains.com/issue/KT-75105) K2: False negative NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER for type constraint of anonymous function
- [`KT-74929`](https://youtrack.jetbrains.com/issue/KT-74929) False positive TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER if it is used with T&Any
- [`KT-74227`](https://youtrack.jetbrains.com/issue/KT-74227) K2: "Cannot infer type for this parameter. Please specify it explicitly" caused by lambda in another lambda with a parameterized function type
- [`KT-64558`](https://youtrack.jetbrains.com/issue/KT-64558) K2 compiler does not report UNNECESSARY_SAFE_CALL, UNNECESSARY_NOT_NULL_ASSERTION, USELESS_ELVIS, while K2 IDEA does
- [`KT-74728`](https://youtrack.jetbrains.com/issue/KT-74728) K2: Java method overriding Kotlin method with receiver loses vararg modifier
- [`KT-70789`](https://youtrack.jetbrains.com/issue/KT-70789) CLI error "mixing legacy and modern plugin arguments is prohibited" on using -Xcompiler-plugin unless default scripting plugin is disabled
- [`KT-72829`](https://youtrack.jetbrains.com/issue/KT-72829) Forbid 'entries' name of enum entry, and deprioritize it in resolve
- [`KT-75037`](https://youtrack.jetbrains.com/issue/KT-75037) K2: IrGeneratedDeclarationsRegistrar.registerFunctionAsMetadataVisible doesn't handle extension receivers and context parameters
- [`KT-73149`](https://youtrack.jetbrains.com/issue/KT-73149) Annotations support for context parameters
- [`KT-74798`](https://youtrack.jetbrains.com/issue/KT-74798) Report error on local contextual properties
- [`KT-74092`](https://youtrack.jetbrains.com/issue/KT-74092) Context parameters: it is not possible to declare an anonymous function with a context
- [`KT-52152`](https://youtrack.jetbrains.com/issue/KT-52152) K2: Investigate suspicious code at SAM conversions
- [`KT-75016`](https://youtrack.jetbrains.com/issue/KT-75016) K2: BackendException when context var property is declared in interface
- [`KT-74474`](https://youtrack.jetbrains.com/issue/KT-74474) K2: Report more precise diagnostic when last expression of non-unit lambda is a statement
- [`KT-74478`](https://youtrack.jetbrains.com/issue/KT-74478) K2: False negative RETURN TYPE_MISMATCH if the last statement of a lambda is indexed assignment
- [`KT-73685`](https://youtrack.jetbrains.com/issue/KT-73685) K2 IDE / Kotlin Debugger: NSME “Method not found” on evaluating function with constant value in `@JvmName`
- [`KT-74449`](https://youtrack.jetbrains.com/issue/KT-74449) Report RETURN_TYPE_MISMATCH instead of ARGUMENT_TYPE_MISMATCH for return expressions in lambdas
- [`KT-74918`](https://youtrack.jetbrains.com/issue/KT-74918) FIR: account for K/Wasm diagnostics in generateNonSuppressibleErrorNamesFile
- [`KT-74897`](https://youtrack.jetbrains.com/issue/KT-74897) K2: Report UNSUPPORTED_FEATURE instead of TOPLEVEL_TYPEALIASES_ONLY for nested type aliases
- [`KT-74963`](https://youtrack.jetbrains.com/issue/KT-74963) K2: Fir2Ir: Avoid a situation when startOffset > endOffset in generated IrBranch
- [`KT-74697`](https://youtrack.jetbrains.com/issue/KT-74697) Overriding a method that's both deprecated and non-deprecated should not cause warnings
- [`KT-74928`](https://youtrack.jetbrains.com/issue/KT-74928) K2: "IllegalStateException: Cannot find cached type parameter by FIR symbol"  in KJK hierarchy with extension property
- [`KT-74630`](https://youtrack.jetbrains.com/issue/KT-74630) K2: local class arguments in annotations on types and type parameters are not serialized
- [`KT-74445`](https://youtrack.jetbrains.com/issue/KT-74445) Commonize Native Function/Property reference lowerings
- [`KT-74670`](https://youtrack.jetbrains.com/issue/KT-74670) Warning message CONTEXT_CLASS_OR_CONSTRUCTOR isn't reported for context receiver on the constructor
- [`KT-74617`](https://youtrack.jetbrains.com/issue/KT-74617) Trivial SMAP optimization leads to missing debug info after inline
- [`KT-74812`](https://youtrack.jetbrains.com/issue/KT-74812) compile-time failure on a callable reference with an input type inferred to an inaccessible generic type
- [`KT-66195`](https://youtrack.jetbrains.com/issue/KT-66195) K2: Java method is not enhanced from overridden's context receivers
- [`KT-74501`](https://youtrack.jetbrains.com/issue/KT-74501) Context parameters: ABSTRACT_MEMBER_NOT_IMPLEMENTED if fun with context is implemented in Java in KJK hierarchy
- [`KT-74385`](https://youtrack.jetbrains.com/issue/KT-74385) Missing diagnostic on repeated suspend modifier in function type
- [`KT-74749`](https://youtrack.jetbrains.com/issue/KT-74749) Provide explanation IR before script compilation
- [`KT-74751`](https://youtrack.jetbrains.com/issue/KT-74751) K2: IllegalStateException: Can't apply receivers of FirPropertyAccessExpressionImpl to IrTypeOperatorCallImpl
- [`KT-74729`](https://youtrack.jetbrains.com/issue/KT-74729) NPE when suspend lambda has inline class parameter
- [`KT-74336`](https://youtrack.jetbrains.com/issue/KT-74336) Not supported: class org.jetbrains.kotlin.fir.types.ConeIntersectionType
- [`KT-74203`](https://youtrack.jetbrains.com/issue/KT-74203) K2: False negative NO_ELSE_IN_WHEN of a generic type with star projection <*> bounded by a sealed hierarchy
- [`KT-48085`](https://youtrack.jetbrains.com/issue/KT-48085) Kotlin/Native: LLD removes live code with `--gc-sections` when producing DLL
- [`KT-69164`](https://youtrack.jetbrains.com/issue/KT-69164) Native: use lld from bundled LLVM distribution when compiling on Windows for a MinGW target
- [`KT-74081`](https://youtrack.jetbrains.com/issue/KT-74081) Context parameters: implicit call resolves to extension when there is a context
- [`KT-74682`](https://youtrack.jetbrains.com/issue/KT-74682) Implement internal type exposure via parameter bounds deprecation postponement
- [`KT-74556`](https://youtrack.jetbrains.com/issue/KT-74556) K2: "IAE: class KtDestructuringDeclaration is not a subtype of class KtNamedDeclaration for factory REDECLARATION" with two non-local destructuring declarations
- [`KT-73146`](https://youtrack.jetbrains.com/issue/KT-73146) Context parameters CLI & diagnostics
- [`KT-72104`](https://youtrack.jetbrains.com/issue/KT-72104) Consider enabling check for unbound symbols in JVM before lowerings
- [`KT-74568`](https://youtrack.jetbrains.com/issue/KT-74568) Synthetic nested classes missing JVM attributes
- [`KT-73703`](https://youtrack.jetbrains.com/issue/KT-73703) [Native] Move KonanIrLinker to `serialization.native` module
- [`KT-61175`](https://youtrack.jetbrains.com/issue/KT-61175) K2: FirReceiverParameter does not extend FirDeclaration
- [`KT-73961`](https://youtrack.jetbrains.com/issue/KT-73961) 'lateinit is unnecessary' on transient properties should not be reported for serializable classes
- [`KT-73858`](https://youtrack.jetbrains.com/issue/KT-73858) Compose  / iOS: NullPointerException on building
- [`KT-62953`](https://youtrack.jetbrains.com/issue/KT-62953) JVM IR: Use `SimpleNamedCompilerPhase` instead of `NamedCompilerPhase`
- [`KT-72929`](https://youtrack.jetbrains.com/issue/KT-72929) Consider caching typealiased constructor symbols created by TypeAliasConstructorsSubstitutingScope
- [`KT-74459`](https://youtrack.jetbrains.com/issue/KT-74459) K2: false positive MISSING_DEPENDENCY_CLASS for types inside default argument
- [`KT-73705`](https://youtrack.jetbrains.com/issue/KT-73705) [Native] Decouple native caches support from KonanIrLinker and KonanPartialModuleDeserializer
- [`KT-74091`](https://youtrack.jetbrains.com/issue/KT-74091) K2: `@JvmOverloads`-produced overloads have generated line number table
- [`KT-69754`](https://youtrack.jetbrains.com/issue/KT-69754) Drop -Xuse-k2 compiler flag
- [`KT-73352`](https://youtrack.jetbrains.com/issue/KT-73352) K2:  false negative ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS
- [`KT-72962`](https://youtrack.jetbrains.com/issue/KT-72962) Consider enabling ConsiderForkPointsWhenCheckingContradictions LF earlier
- [`KT-73027`](https://youtrack.jetbrains.com/issue/KT-73027) IllegalStateException: Annotation argument value cannot be null: since
- [`KT-74242`](https://youtrack.jetbrains.com/issue/KT-74242) Freeze on `runCatching` call in `finally` block inside SAM conversion
- [`KT-29222`](https://youtrack.jetbrains.com/issue/KT-29222) FIR: consider folding binary expression chains
- [`KT-73760`](https://youtrack.jetbrains.com/issue/KT-73760) Cannot implement two Java interfaces with `@NotNull`-annotated type argument and Kotlin's plain (nullable) type parameter
- [`KT-58933`](https://youtrack.jetbrains.com/issue/KT-58933) Applying suggested signature from WRONG_NULLABILITY_FOR_JAVA_OVERRIDE leads to red code
- [`KT-70507`](https://youtrack.jetbrains.com/issue/KT-70507) Should parentheses prevent from plus/set operator desugaring?
- [`KT-67520`](https://youtrack.jetbrains.com/issue/KT-67520) Change of behaviour of inline function with safe cast on value type
- [`KT-67518`](https://youtrack.jetbrains.com/issue/KT-67518) Value classes leak their carrier type implementation details via inlining
- [`KT-72305`](https://youtrack.jetbrains.com/issue/KT-72305) K2: Report error when using synthetic properties in case of mapped collections
- [`KT-71226`](https://youtrack.jetbrains.com/issue/KT-71226) K2 Evaluator: Code fragment compilation with unresolved classes does not fail with exception
- [`KT-70233`](https://youtrack.jetbrains.com/issue/KT-70233) Implement a deprecation error for FIELD-targeted annotations on annotation properties
- [`KT-67517`](https://youtrack.jetbrains.com/issue/KT-67517) Value class upcast to Any leaks carrier type interfaces
- [`KT-72814`](https://youtrack.jetbrains.com/issue/KT-72814) FIR: don't use function references in FirThisReference
- [`KT-73153`](https://youtrack.jetbrains.com/issue/KT-73153) K2: Standalone diagnostics on type arguments are not reported
- [`KT-73011`](https://youtrack.jetbrains.com/issue/KT-73011) K2: Allow overloads resolution for callable references based on expected type variable with constraints
- [`KT-70139`](https://youtrack.jetbrains.com/issue/KT-70139) Remove dependencies of debugger on K1 and old JVM backend
- [`KT-69223`](https://youtrack.jetbrains.com/issue/KT-69223) Drop parallel lowering mode in JVM backend
- [`KT-7461`](https://youtrack.jetbrains.com/issue/KT-7461) Forbid using projection modifiers inside top-level Array in annotation's value parameter
- [`KT-53804`](https://youtrack.jetbrains.com/issue/KT-53804) Restore old and incorrect logic of generating InnerClasses attributes for kotlin-stdlib
- [`KT-52774`](https://youtrack.jetbrains.com/issue/KT-52774) Resolve unqualified enum constants based on expected type

### Compose compiler

#### New features
- [`5f7e5d1`](https://github.com/JetBrains/kotlin/commit/5f7e5d1518e839c1f8514a5c145aad52b1ce1739) Enabled PausableComposition feature flag by default
- [`e49ba7a`](https://github.com/JetBrains/kotlin/commit/e49ba7a4e60da8c4bd5020376a69749484d39f6c) Enabled OptimizeNonSkippingGroups feature flag by default
- 
#### Fixes
- [`b/420729503`](https://issuetracker.google.com/issues/420729503) Avoid copying `@Deprecated` annotations on Compose compiler stubs
- [`b/417412949`](https://issuetracker.google.com/issues/417412949) Emit fake line number for `skipToGroupEnd` branch
- [`b/412584977`](https://issuetracker.google.com/issues/412584977) Fix false positive for overriding open functions from older dependencies
- [`b/409238521`](https://issuetracker.google.com/issues/409238521) Fix crash when searching for ComposableLambda::invoke function on JS
- [`b/408752831`](https://issuetracker.google.com/issues/408752831) Fix early return with value from `key` groups
- [`b/388505454`](https://issuetracker.google.com/issues/388505454) Treat context parameters the same way as extension receiver
- [`b/408013789`](https://issuetracker.google.com/issues/408013789) Add missing return for the default function wrappers
- [`b/405541364`](https://issuetracker.google.com/issues/405541364) Realize coalescable children in the body of `key` call
- [`b/305035807`](https://issuetracker.google.com/issues/305035807) Add support for `@Composable` function references with K2
- [`b/401484249`](https://issuetracker.google.com/issues/401484249) Generate a group around `Array` constructor call
- [`b/400380396`](https://issuetracker.google.com/issues/400380396) Fix missing `endMovableGroup` call with early return in `key` function
- [`b/397855145`](https://issuetracker.google.com/issues/397855145) Fix "Unknown file" error in target annotation inference
- [`b/274898109`](https://issuetracker.google.com/issues/274898109) Fix off-by-one error when calculating changed arg count for lambdas
- [`b/377499888`](https://issuetracker.google.com/issues/377499888) Allow restarting overridden functions in a final class
- [`b/393400768`](https://issuetracker.google.com/issues/393400768) Use -1 for `.changed` call if nullable enum parameter is `null`
- [`b/390151896`](https://issuetracker.google.com/issues/390151896) Fix default arguments with varargs in `@Composable` functions
- [`b/388030459`](https://issuetracker.google.com/issues/388030459), [`b/377737816`](https://issuetracker.google.com/issues/377737816) Prevent usage of transitive captures in lambda memoization
- [`b/310004740`](https://issuetracker.google.com/issues/310004740) Check vararg parameter length in skipping logic
- [`b/367066334`](https://issuetracker.google.com/issues/367066334) Add diagnostic to restrict `@Composable` annotation to function types only
- [`b/388505454`](https://issuetracker.google.com/issues/388505454) Change order of $changed bits with context parameters
- [`CMP-7873`](https://youtrack.jetbrains.com/issue/CMP-7873) Native build fails with "e: Compilation failed: Exception during generating code for following declaration"

### IDE

- [`KT-54804`](https://youtrack.jetbrains.com/issue/KT-54804) Generate synthetic functions for annotations on properties in light classes

### IR. Actualizer

- [`KT-70907`](https://youtrack.jetbrains.com/issue/KT-70907) Actualize fake override symbols in Ir Actualizer

### IR. Inlining

#### Fixes

- [`KT-76145`](https://youtrack.jetbrains.com/issue/KT-76145) Enhance error message about poisoned KLIBs in KLIB-based compilers
- [`KT-70916`](https://youtrack.jetbrains.com/issue/KT-70916) IR: Report errors on exposure of private types in non-private inline functions
- [`KT-73155`](https://youtrack.jetbrains.com/issue/KT-73155) Move `Mapping` from `LoweringContext` back to `CommonBackendContext`
- [`KT-76186`](https://youtrack.jetbrains.com/issue/KT-76186) [IR] Sanitize deserialized IR dump of anonymous classes
- [`KT-75788`](https://youtrack.jetbrains.com/issue/KT-75788) IR inliner: Serialize preprocessed inline functions in a separate place inside KLIBs
- [`KT-71416`](https://youtrack.jetbrains.com/issue/KT-71416) Perform IR-level visibility diagnostics for inline functions after the first phase of inlining
- [`KT-76224`](https://youtrack.jetbrains.com/issue/KT-76224) [IR][Inliner] Dumb file is unsuported in IrSymbolBase.getDescriptor()
- [`KT-75793`](https://youtrack.jetbrains.com/issue/KT-75793) IR inliner: Stop injecting the deserialized function body to LazyIR inline function
- [`KT-75791`](https://youtrack.jetbrains.com/issue/KT-75791) IR inliner:  `NonLinkingIrInlineFunctionDeserializer` should load inline functions from a separate location in a KLIB
- [`KT-73708`](https://youtrack.jetbrains.com/issue/KT-73708) Use some marker in KLIBs produced with IR inliner
- [`KT-76024`](https://youtrack.jetbrains.com/issue/KT-76024) [JS][IR Inliner] Partial linkage: No function found for symbol in `kotlin` package
- [`KT-75733`](https://youtrack.jetbrains.com/issue/KT-75733) Reorganize execution of the common prefix at 1st phase of compilation
- [`KT-75986`](https://youtrack.jetbrains.com/issue/KT-75986) Add an option to the `DumpIrTreeOptions` to dump IR signature if available
- [`KT-75951`](https://youtrack.jetbrains.com/issue/KT-75951) [IR Inliner] Illegal non-local return reported by the partial linkage engine
- [`KT-75932`](https://youtrack.jetbrains.com/issue/KT-75932) Fix a problem with already bound symbol with public IR inline enabled
- [`KT-64812`](https://youtrack.jetbrains.com/issue/KT-64812) Investigate and fix remaining owner usages in inliner
- [`KT-74732`](https://youtrack.jetbrains.com/issue/KT-74732) IR: Exposure of private type is reported at wrong source location
- [`KT-76007`](https://youtrack.jetbrains.com/issue/KT-76007) IR deserialization tests w/ enabled IR inliner: undefined offsets in IrInlinedFunctionBlock.inlinedFunctionSymbol
- [`KT-74734`](https://youtrack.jetbrains.com/issue/KT-74734) [Native] Use NativeInliningFacade in new subclass of AbstractFirNativeSerializationTest
- [`KT-73985`](https://youtrack.jetbrains.com/issue/KT-73985) KLIB Synthetic Accessors Dump Format: Include local declarations
- [`KT-72594`](https://youtrack.jetbrains.com/issue/KT-72594) [JS][Native] Add IrInliningFacade to test runners
- [`KT-74456`](https://youtrack.jetbrains.com/issue/KT-74456) SerializedIrDumpHandler: Compare IR dumps with source offsets
- [`KT-73624`](https://youtrack.jetbrains.com/issue/KT-73624) [Native] Implement inlining facade
- [`KT-70370`](https://youtrack.jetbrains.com/issue/KT-70370) SyntheticAccessorLowering: Turn on mode with narrowing visibility on 1st phase of compilation
- [`KT-70452`](https://youtrack.jetbrains.com/issue/KT-70452) OuterThisInInlineFunctionsSpecialAccessorLowering - run it after inlining of private functions
- [`KT-70451`](https://youtrack.jetbrains.com/issue/KT-70451) Enable double-inlining in Native & JS backends unconditionally
- [`KT-69681`](https://youtrack.jetbrains.com/issue/KT-69681) IR: Report warnings on exposure of private types in non-private inline functions

### IR. Interpreter

- [`KT-74581`](https://youtrack.jetbrains.com/issue/KT-74581) Support `IrRich*Reference` in IR interpreter

### IR. Tree

#### Fixes

- [`KT-73189`](https://youtrack.jetbrains.com/issue/KT-73189) Migrate compiler sources to new IR parameter API
- [`KT-77508`](https://youtrack.jetbrains.com/issue/KT-77508) K/JS and K/Native CompilationException Wrong number of parameters in wrapper
- [`KT-74331`](https://youtrack.jetbrains.com/issue/KT-74331) Implement IrElement.copyAttributes as a true attribute map copy
- [`KT-76600`](https://youtrack.jetbrains.com/issue/KT-76600) Use a language feature to check error on cross-file IrGetField operations generated by compiler plugins
- [`KT-75628`](https://youtrack.jetbrains.com/issue/KT-75628) IR validator: Forbid IrExpressionBody for IrFunction
- [`KT-75679`](https://youtrack.jetbrains.com/issue/KT-75679) Extract common `invokeFunction` in IrRichCallableReference
- [`KT-74799`](https://youtrack.jetbrains.com/issue/KT-74799) [Native][IR] Excessive FUNCTION_INTERFACE_CLASS after deserialization
- [`KT-71138`](https://youtrack.jetbrains.com/issue/KT-71138) Report error on cross-file IrGetField operations generated by compiler plugins
- [`KT-75196`](https://youtrack.jetbrains.com/issue/KT-75196) [IR] Make startOffset and endOffset mutable
- [`KT-73206`](https://youtrack.jetbrains.com/issue/KT-73206) Extract common parts from new IrRichFunctionReference/IrRichPropertyReference nodes
- [`KT-73190`](https://youtrack.jetbrains.com/issue/KT-73190) Migrate common backend sources to new IR parameter API
- [`KT-73225`](https://youtrack.jetbrains.com/issue/KT-73225) Migrate `compiler.ir.serialization.common` to new IR parameter API
- [`KT-73220`](https://youtrack.jetbrains.com/issue/KT-73220) Migrate `compiler.ir.tree` to new IR parameter API
- [`KT-75189`](https://youtrack.jetbrains.com/issue/KT-75189) Add an IR validation for the correspondingPropertySymbol
- [`KT-74275`](https://youtrack.jetbrains.com/issue/KT-74275) Adjust IR dump format for context parameters & new parameter API
- [`KT-74269`](https://youtrack.jetbrains.com/issue/KT-74269) Drop IrElementVisitor, IrElementVisitorVoid and IrElementTransformer interfaces
- [`KT-72739`](https://youtrack.jetbrains.com/issue/KT-72739) Create a lowering to replace old callable reference nodes with new ones
- [`KT-73120`](https://youtrack.jetbrains.com/issue/KT-73120) Get rid of `Ir` class
- [`KT-73045`](https://youtrack.jetbrains.com/issue/KT-73045) Fix inconsistency between shapes of IR calls vs callee
- [`KT-69714`](https://youtrack.jetbrains.com/issue/KT-69714) [IR] Remove IrErrorDeclaration
- [`KT-73609`](https://youtrack.jetbrains.com/issue/KT-73609) Tests: Implement DeserializerFacade for Kotlin/Native
- [`KT-73813`](https://youtrack.jetbrains.com/issue/KT-73813) Implement tests for all IR validator checkers
- [`KT-73171`](https://youtrack.jetbrains.com/issue/KT-73171) Choose the approach for testing IR serialization/deserialization wrt IR inliner
- [`KT-73430`](https://youtrack.jetbrains.com/issue/KT-73430) [IR] Get rid of SymbolFinder usages outside of  `Symbols` hierarchy
- [`KT-74455`](https://youtrack.jetbrains.com/issue/KT-74455) IR dump: Support dumping source offsets
- [`KT-73433`](https://youtrack.jetbrains.com/issue/KT-73433) [IR] Get rid of some symbols lookup methods

### JVM. Reflection

- [`KT-75505`](https://youtrack.jetbrains.com/issue/KT-75505) Reflection: use kotlin-metadata-jvm to implement class visibility, modality, modifiers, etc
- [`KT-77663`](https://youtrack.jetbrains.com/issue/KT-77663) Reflection: java.util.ServiceConfigurationError: "module kotlin.reflect does not declare `uses`" when using kotlin-reflect in modular mode
- [`KT-75464`](https://youtrack.jetbrains.com/issue/KT-75464) Bundle kotlin-metadata-jvm into kotlin-reflect
- [`KT-71832`](https://youtrack.jetbrains.com/issue/KT-71832) kotlin.jvm.internal.ClassReference static overhead is 11,060 bytes

### JavaScript

#### New Features

- [`KT-31493`](https://youtrack.jetbrains.com/issue/KT-31493) [Kotlin/JS] Can't put typealias in file marked with JsModule annotation

#### Performance Improvements

- [`KT-74533`](https://youtrack.jetbrains.com/issue/KT-74533) K/JS: avoid number to char calls in charSequenceGet intrinsic

#### Fixes

- [`KT-77021`](https://youtrack.jetbrains.com/issue/KT-77021) CompilationException: Encountered a local class not previously collected on inner classes inside anonymous objects
- [`KT-78073`](https://youtrack.jetbrains.com/issue/KT-78073) K/JS: KProperty from local delegate changes after another delegate is invoked
- [`KT-77271`](https://youtrack.jetbrains.com/issue/KT-77271) KJS / Serialization: "Cannot set property message of Error which has only a getter"
- [`KT-76235`](https://youtrack.jetbrains.com/issue/KT-76235) [JS] Extra invalid line `tmp_0.tmp00__1 = Options;` in testSuspendFunction()
- [`KT-76234`](https://youtrack.jetbrains.com/issue/KT-76234) [JS] Extra invalid line `Parent` in testNested()
- [`KT-76233`](https://youtrack.jetbrains.com/issue/KT-76233) [JS] Extra invalid import line in testJsQualifier()
- [`KT-74839`](https://youtrack.jetbrains.com/issue/KT-74839) AssociatedObjectKey metadata doesn't survive incremental compilation
- [`KT-77418`](https://youtrack.jetbrains.com/issue/KT-77418) KJS: cannot debug with whole-program granularity
- [`KT-76463`](https://youtrack.jetbrains.com/issue/KT-76463) KJS: `@JsPlainObject` fails in parent count is 8+
- [`KT-75606`](https://youtrack.jetbrains.com/issue/KT-75606) KJS: java.lang.AssertionError: Different declarations with the same signatures were detected
- [`KT-69591`](https://youtrack.jetbrains.com/issue/KT-69591) KJS / d.ts: Wrong type of SerializerFactory for abstract classes
- [`KT-72437`](https://youtrack.jetbrains.com/issue/KT-72437) KJS. Invalid `copy` method for inherited JSO with type parameters
- [`KT-57192`](https://youtrack.jetbrains.com/issue/KT-57192) KJS: "Exported declaration uses non-exportable return type" caused by `@JsExport` Promise with Unit type
- [`KT-73226`](https://youtrack.jetbrains.com/issue/KT-73226) Migrate K/JS to new IR parameter API
- [`KT-75254`](https://youtrack.jetbrains.com/issue/KT-75254) KJS: Merge AbstractSuspendFunctionsLowering from Common and JS backends
- [`KT-76440`](https://youtrack.jetbrains.com/issue/KT-76440) `@JsPlainObject` compiles broken code when inlining suspend function and non suspend function
- [`KT-71169`](https://youtrack.jetbrains.com/issue/KT-71169) `@JsPlainObject` copy produces the wrong type when copied property is nullable in parent interface
- [`KT-75772`](https://youtrack.jetbrains.com/issue/KT-75772) KJS: NullPointerException caused by reference of private class with `@JsExport`
- [`KT-73363`](https://youtrack.jetbrains.com/issue/KT-73363) Migrate `js-plain-objects` plugin to new IR parameter API
- [`KT-64927`](https://youtrack.jetbrains.com/issue/KT-64927) [JS] TypeError when abstract class with override var property extends an exported abstract class with val property
- [`KT-70623`](https://youtrack.jetbrains.com/issue/KT-70623) Kotlin/JS: Incremental compilation fails when granularity is changed
- [`KT-74384`](https://youtrack.jetbrains.com/issue/KT-74384) Support new callable reference nodes in JS backend
- [`KT-70652`](https://youtrack.jetbrains.com/issue/KT-70652) Kotlin/JS:  `@JsExport` doesn't work with granularity per-file
- [`KT-71365`](https://youtrack.jetbrains.com/issue/KT-71365) KJS. File-level export support
- [`KT-68775`](https://youtrack.jetbrains.com/issue/KT-68775) Kotlin/JS infinite loop for exception message override that calls super.message
- [`KT-42271`](https://youtrack.jetbrains.com/issue/KT-42271) K/JS: isInitialized for not-lateinit property isn't marked as error as in JVM project
- [`KT-70664`](https://youtrack.jetbrains.com/issue/KT-70664) Extending a `@JsPlainObject` interface with a generic type parameter fails with a compile error
- [`KT-71656`](https://youtrack.jetbrains.com/issue/KT-71656) K2 JS: "IllegalStateException: Class has no primary constructor: kotlin.ULong"
- [`KT-42305`](https://youtrack.jetbrains.com/issue/KT-42305) KJS / IR: "Class constructor is marked as private" `@JsExport` produces wrong TS code for sealed classes
- [`KT-52563`](https://youtrack.jetbrains.com/issue/KT-52563) KJS / IR: Invalid TypeScript generated for class extending base class with private constructor

### Klibs

#### New Features

- [`KT-72296`](https://youtrack.jetbrains.com/issue/KT-72296) Use specialized signatures for serialized local fake overrides

#### Fixes

- [`KT-78168`](https://youtrack.jetbrains.com/issue/KT-78168) K/N: "IndexOutOfBoundsException: Index 3 out of bounds for length 3" for iOS build with Kotlin 2.2.0-RC2
- [`KT-74635`](https://youtrack.jetbrains.com/issue/KT-74635) KLIBs: Change call serialization scheme to store all arguments in a single list
- [`KT-76061`](https://youtrack.jetbrains.com/issue/KT-76061) Add option for suppress warning of missing no-existent transitive klib dependencies
- [`KT-70146`](https://youtrack.jetbrains.com/issue/KT-70146) [KLIB Resolve] Don't fail on nonexistent transitive dependency
- [`KT-75624`](https://youtrack.jetbrains.com/issue/KT-75624) Don't fail on an attempt to deserialize "unknown" IrStatementOrigin and IrDeclarationOrigin
- [`KT-55808`](https://youtrack.jetbrains.com/issue/KT-55808) Support metadata version checks for klibs in the compiler
- [`KT-56062`](https://youtrack.jetbrains.com/issue/KT-56062) Support `-Xmetadata-version` for KLIB-based compilers
- [`KT-76158`](https://youtrack.jetbrains.com/issue/KT-76158) Drop "description" from local signatures
- [`KT-75749`](https://youtrack.jetbrains.com/issue/KT-75749) KLIB: Fail with error on attempt to serialize/deserialize SpecialFakeOverrideSignature
- [`KT-75941`](https://youtrack.jetbrains.com/issue/KT-75941) [IR Inliner] Abstract function  is not implemented in non-abstract anonymous object
- [`KT-75867`](https://youtrack.jetbrains.com/issue/KT-75867) The CLI argument -Xabi-version allows versions with multiple 0 and -0
- [`KT-75192`](https://youtrack.jetbrains.com/issue/KT-75192) KLIB reader tends to extract files from the KLIB archive to a temporary directory even when this is not needed
- [`KT-75013`](https://youtrack.jetbrains.com/issue/KT-75013) Make klib reader more flexible: allow empty directories to be omitted
- [`KT-75680`](https://youtrack.jetbrains.com/issue/KT-75680) KLIB: Drop obsolete IrPerFileLibraryImpl & IrPerFileWriterImpl
- [`KT-73779`](https://youtrack.jetbrains.com/issue/KT-73779) [Native] Context parameters: extension receiver is preferred over context parameters
- [`KT-65375`](https://youtrack.jetbrains.com/issue/KT-65375) Clean-up the logic for serialization of error types in metadata and in IR
- [`KT-73826`](https://youtrack.jetbrains.com/issue/KT-73826) Deduplicate `IrFileEntry` that is serialized inside `IrInlinedFunctionBlock`
- [`KT-75091`](https://youtrack.jetbrains.com/issue/KT-75091) Drop `targets/$target_name/kotlin` directory from klibs
- [`KT-74352`](https://youtrack.jetbrains.com/issue/KT-74352) API4ABI: Fix representation of context parameters
- [`KT-71007`](https://youtrack.jetbrains.com/issue/KT-71007) Align KLIB ABI version with the language version
- [`KT-73672`](https://youtrack.jetbrains.com/issue/KT-73672) Bump KLIB ABI version in 2.2.0
- [`KT-74080`](https://youtrack.jetbrains.com/issue/KT-74080) API4ABI: Adapt API for value parameter kinds
- [`KT-74396`](https://youtrack.jetbrains.com/issue/KT-74396) Support context parameters in klibs
- [`KT-72931`](https://youtrack.jetbrains.com/issue/KT-72931) Support new callable reference nodes in KLIB [de]serializer

### Language Design

#### New Features

- [`KT-2425`](https://youtrack.jetbrains.com/issue/KT-2425) Multidollar interpolation: improve handling of $ in string literals
- [`KT-1436`](https://youtrack.jetbrains.com/issue/KT-1436) Support non-local break and continue
- [`KT-13626`](https://youtrack.jetbrains.com/issue/KT-13626) Guard conditions in when-with-subject
- [`KT-54206`](https://youtrack.jetbrains.com/issue/KT-54206) Support local contextual functions

#### Fixes

- [`KT-53673`](https://youtrack.jetbrains.com/issue/KT-53673) Support `@DslMarker` annotations on contextual receivers
- [`KT-73557`](https://youtrack.jetbrains.com/issue/KT-73557) Allow refining expect declarations for platform groups
- [`KT-72417`](https://youtrack.jetbrains.com/issue/KT-72417) Annotation with target RECORD_COMPONENT cannot be used on `@JvmRecord` data class components
- [`KT-67977`](https://youtrack.jetbrains.com/issue/KT-67977) Compile results of annotations assigned to JvmRecord properties as in Java
- [`KT-73502`](https://youtrack.jetbrains.com/issue/KT-73502) Context parameters: it is not possible to declare local function with a context
- [`KT-73632`](https://youtrack.jetbrains.com/issue/KT-73632) Expect class redeclaration is allowed
- [`KT-70002`](https://youtrack.jetbrains.com/issue/KT-70002) [LC] Forbid using projection modifiers inside top-level Array in annotation's value parameter

### Libraries

#### New Features

- [`KT-76163`](https://youtrack.jetbrains.com/issue/KT-76163) K/N: Hide or remove CreateNSStringFromKString/CreateKStringFromNSString
- [`KT-70456`](https://youtrack.jetbrains.com/issue/KT-70456) Base64: Support lineLength parameter for Mime
- [`KT-76394`](https://youtrack.jetbrains.com/issue/KT-76394) kotlin.time.TimeSource.asClock missing
- [`KT-31857`](https://youtrack.jetbrains.com/issue/KT-31857) Provide easy way to retrieve annotations for kotlinx-metadata
- [`KT-76528`](https://youtrack.jetbrains.com/issue/KT-76528) Instant.parseOrNull
- [`KT-74804`](https://youtrack.jetbrains.com/issue/KT-74804) Add `@MustUseValue` and `@IgnorableValue` / `@Discardable` to kotlin-stdlib
- [`KT-74422`](https://youtrack.jetbrains.com/issue/KT-74422) KotlinWebsiteSampleRewriter should filter individual imports from samples package

#### Performance Improvements

- [`KT-68860`](https://youtrack.jetbrains.com/issue/KT-68860) K/JS: optimize listOf(element)
- [`KT-75647`](https://youtrack.jetbrains.com/issue/KT-75647) Optimized sequenceOf(T) overload is missing

#### Fixes

- [`KT-72138`](https://youtrack.jetbrains.com/issue/KT-72138) Stabilize experimental API for 2.2
- [`KT-76831`](https://youtrack.jetbrains.com/issue/KT-76831) Atomic types: inconsistent behavior on JS and Wasm targets
- [`KT-76795`](https://youtrack.jetbrains.com/issue/KT-76795) RecursiveDeletionTest.deleteRelativeSymbolicLink fails on Windows for unprivileged users
- [`KT-75290`](https://youtrack.jetbrains.com/issue/KT-75290) kotlin-metadata: deprecate hasAnnotations flag, add JVM-only hasAnnotationsInBytecode instead
- [`KT-76193`](https://youtrack.jetbrains.com/issue/KT-76193) Common Atomics: 'AtomicArray.compareAndSetAt' and  'compareAndExchangeAt' docs incorrectly suggest they use `==` when actually they use `===`
- [`KT-54077`](https://youtrack.jetbrains.com/issue/KT-54077) Consider using SecureDirectoryStream in deleteRecursively even when Path.parent is null
- [`KT-72866`](https://youtrack.jetbrains.com/issue/KT-72866) Standard library functions to work with context parameters
- [`KT-76743`](https://youtrack.jetbrains.com/issue/KT-76743) Add kotlin-scripting-jvm to projectsUsedInIntelliJKotlinPlugin list
- [`KT-72483`](https://youtrack.jetbrains.com/issue/KT-72483) Clean up redundant stdlib code for Kotlin 2.2
- [`KT-76385`](https://youtrack.jetbrains.com/issue/KT-76385) Remove suppression from functions to work with context parameters
- [`KT-75337`](https://youtrack.jetbrains.com/issue/KT-75337) Remove suppress annotations from `@IgnorableReturnValue`
- [`KT-72137`](https://youtrack.jetbrains.com/issue/KT-72137) Review deprecations in stdlib for 2.2
- [`KT-75491`](https://youtrack.jetbrains.com/issue/KT-75491) Non intuitive work of 'in' (contains) with String range
- [`KT-75933`](https://youtrack.jetbrains.com/issue/KT-75933) Update readLine's KDoc to suggest alternative functions
- [`KT-46360`](https://youtrack.jetbrains.com/issue/KT-46360) Type inference fails to infer type for sumOf call with integer literal: "Overload resolution ambiguity TypeVariable(T)) -> Int / Long"
- [`KT-73590`](https://youtrack.jetbrains.com/issue/KT-73590) Samplify string.split
- [`KT-75759`](https://youtrack.jetbrains.com/issue/KT-75759) Add the serializer for kotlin.time.Instant to the list of standard serializers
- [`KT-71628`](https://youtrack.jetbrains.com/issue/KT-71628) Review deprecations in stdlib for 2.1
- [`KT-73726`](https://youtrack.jetbrains.com/issue/KT-73726) A link from shuffle's KDoc is not rendered properly
- [`KT-74173`](https://youtrack.jetbrains.com/issue/KT-74173) The sample code of `lazy` on stdlib can not run on playground due to "samples" package import
- [`KT-50081`](https://youtrack.jetbrains.com/issue/KT-50081) AbstractList sublist leads to StackOverflow

### Native

- [`KT-76992`](https://youtrack.jetbrains.com/issue/KT-76992) Native: update llvm for windows targets
- [`KT-56107`](https://youtrack.jetbrains.com/issue/KT-56107) Support Enum.entries for C/ObjC interop enums
- [`KT-76552`](https://youtrack.jetbrains.com/issue/KT-76552) LLVM Update: rebase the LLVM branch
- [`KT-76662`](https://youtrack.jetbrains.com/issue/KT-76662) LLVM 19 update: documentation
- [`KT-76560`](https://youtrack.jetbrains.com/issue/KT-76560) LLVM Update: investigate changes in filterStdargH test
- [`KT-76283`](https://youtrack.jetbrains.com/issue/KT-76283) LLVM Update: pass all tests
- [`KT-74377`](https://youtrack.jetbrains.com/issue/KT-74377) Kotlin Native: release executable crashes with error 139
- [`KT-75829`](https://youtrack.jetbrains.com/issue/KT-75829) LLVM Update: port K/N on LLVM 19
- [`KT-76280`](https://youtrack.jetbrains.com/issue/KT-76280) LLVM Update: benchmarksAnalyzer build failed
- [`KT-70202`](https://youtrack.jetbrains.com/issue/KT-70202) Xcode 16 Linker fails with SIGBUS

### Native. Build Infrastructure

- [`KT-77349`](https://youtrack.jetbrains.com/issue/KT-77349) Kotlin/Native: default cache for stdlib is unused

### Native. C and ObjC Import

- [`KT-75598`](https://youtrack.jetbrains.com/issue/KT-75598) Native: fix samples/objc test
- [`KT-76551`](https://youtrack.jetbrains.com/issue/KT-76551) LLVM Update: investigate CXFile equality problem further
- [`KT-75781`](https://youtrack.jetbrains.com/issue/KT-75781) Xcode 16.3: Fix cinterop tests failing with fatal error: could not build module '_stdint'
- [`KT-74549`](https://youtrack.jetbrains.com/issue/KT-74549) Native: replace clang_Type_getNumProtocols/clang_Type_getProtocol with standard libclang functions

### Native. Platforms

- [`KT-74702`](https://youtrack.jetbrains.com/issue/KT-74702) Deprecate Windows 7 support

### Native. Runtime

- [`KT-71534`](https://youtrack.jetbrains.com/issue/KT-71534) Native: Support Latin-1 encoded strings at runtime
- [`KT-67741`](https://youtrack.jetbrains.com/issue/KT-67741) Kotlin/Native: Unify SpecialRef handling

### Native. Runtime. Memory

- [`KT-74831`](https://youtrack.jetbrains.com/issue/KT-74831) Kotlin/Native: investigate mmap usage in custom allocator
- [`KT-74432`](https://youtrack.jetbrains.com/issue/KT-74432) Native: add an option to allocate everything in SingleObjectPage
- [`KT-74975`](https://youtrack.jetbrains.com/issue/KT-74975) Enable CMS by default in Swift export
- [`KT-60928`](https://youtrack.jetbrains.com/issue/KT-60928) Kotlin/Native: refactor allocator code
- [`KT-50291`](https://youtrack.jetbrains.com/issue/KT-50291) Kotlin/Native: remove dependency of mm on gc implementation

### Native. Swift Export

- [`KT-75166`](https://youtrack.jetbrains.com/issue/KT-75166) Support export of platform libraries types in Swift export
- [`KT-75079`](https://youtrack.jetbrains.com/issue/KT-75079) Swift export: add dependency from sir-compiler-bridge to Analysis API
- [`KT-72413`](https://youtrack.jetbrains.com/issue/KT-72413) Swift Export: potential memory leak when best-fitting class is different from the formal type
- [`KT-72107`](https://youtrack.jetbrains.com/issue/KT-72107) Remove IntoSingleModule stratagy

### Tools. Ant

- [`KT-73116`](https://youtrack.jetbrains.com/issue/KT-73116) Deprecate Ant support

### Tools. BCV

- [`KT-75686`](https://youtrack.jetbrains.com/issue/KT-75686) Improve DSL for BCV in KGP
- [`KT-76129`](https://youtrack.jetbrains.com/issue/KT-76129) Abi validation filtering functionality for included classes doesn't work
- [`KT-75999`](https://youtrack.jetbrains.com/issue/KT-75999) ABI validation filter doesn't apply excluded kotlin files
- [`KT-75981`](https://youtrack.jetbrains.com/issue/KT-75981) ABI validation filter not applying excluded classes without package names
- [`KT-71168`](https://youtrack.jetbrains.com/issue/KT-71168) Implement a prototype of ABI Validation in Kotlin Gradle Plugin

### Tools. Build Tools API

- [`KT-76455`](https://youtrack.jetbrains.com/issue/KT-76455) BTA: Compilation is always non-incremental if BTA API >= 2.2.0 is used together with BTA impl < 2.2.0
- [`KT-76060`](https://youtrack.jetbrains.com/issue/KT-76060) BTA: Java sources passed for IC may fail compilation in non-incremental mode
- [`KT-74041`](https://youtrack.jetbrains.com/issue/KT-74041) Build Tools API: Lower level or remove compiler arguments log

### Tools. CLI

#### New Features

- [`KT-73606`](https://youtrack.jetbrains.com/issue/KT-73606) Provide a unified interface for managing the reporting of compiler warnings
- [`KT-24746`](https://youtrack.jetbrains.com/issue/KT-24746) Provide ability to exclude specific warnings from compiler option Werror (all warnings as errors)
- [`KT-18783`](https://youtrack.jetbrains.com/issue/KT-18783) Option to treat a specific compiler warning as an error
- [`KT-76095`](https://youtrack.jetbrains.com/issue/KT-76095) Add JVM target bytecode version 24
- [`KT-73007`](https://youtrack.jetbrains.com/issue/KT-73007) Add stable compiler argument -jvm-default instead of -Xjvm-default

#### Performance Improvements

- [`KT-75641`](https://youtrack.jetbrains.com/issue/KT-75641) kotlinc -help spends almost 1 second on Usage.render()

#### Fixes

- [`KT-77445`](https://youtrack.jetbrains.com/issue/KT-77445)  UNRESOLVED_REFERENCE when importing classes from kotlin-stdlib
- [`KT-75300`](https://youtrack.jetbrains.com/issue/KT-75300) Lenient compiler mode which generates stubs for missing actuals
- [`KT-76829`](https://youtrack.jetbrains.com/issue/KT-76829) UnsupportedOperationException when reenabling a taking place warning with -Xwarning-level
- [`KT-75588`](https://youtrack.jetbrains.com/issue/KT-75588) [2.1.20-RC] "was compiled by a pre-release version of Kotlin and cannot be loaded by this version of the compiler" warnings despite using the same compiler version
- [`KT-74663`](https://youtrack.jetbrains.com/issue/KT-74663) kotlinc-js CLI: not providing -ir-output-dir results in NullPointerException
- [`KT-75967`](https://youtrack.jetbrains.com/issue/KT-75967) Implement generation of CLI arguments in compiler using new single representation
- [`KT-75966`](https://youtrack.jetbrains.com/issue/KT-75966) Declare all existing CLI arguments using the new DSL
- [`KT-76498`](https://youtrack.jetbrains.com/issue/KT-76498) Implement JSON dumper for performance stats
- [`KT-75970`](https://youtrack.jetbrains.com/issue/KT-75970) Extract all non-trivial logic from `CommonCompilerArguments` and its inheritors
- [`KT-73595`](https://youtrack.jetbrains.com/issue/KT-73595) Kapt.use.k2=true is ignored silently for language-version 1.9 or less
- [`KT-75043`](https://youtrack.jetbrains.com/issue/KT-75043) Migrate Metadata compilation pipeline to the phased structure
- [`KT-75113`](https://youtrack.jetbrains.com/issue/KT-75113) TEST_ONLY LanguageFeature doesn't abort the compilation

### Tools. CLI. Native

- [`KT-69485`](https://youtrack.jetbrains.com/issue/KT-69485) Native: remove adding $llvmDir\bin to PATH on Windows

### Tools. Commonizer

- [`KT-74623`](https://youtrack.jetbrains.com/issue/KT-74623) Drop metadata version check from KLIB commonizer

### Tools. Compiler Plugin API

- [`KT-74640`](https://youtrack.jetbrains.com/issue/KT-74640) [FIR] Support setting `source` in declaration generators

### Tools. Compiler Plugins

#### Fixes

- [`KT-76162`](https://youtrack.jetbrains.com/issue/KT-76162) "IllegalStateException: No mapping for symbol: VALUE_PARAMETER INSTANCE_RECEIVER" after updating to 2.1.20
- [`KT-61584`](https://youtrack.jetbrains.com/issue/KT-61584) [atomicfu]: prohibit declaration of AtomicReference to the value class in the compiler plugin
- [`KT-70982`](https://youtrack.jetbrains.com/issue/KT-70982) Deprecate declaration of atomic properties marked with `@PublishedApi` with error
- [`KT-73367`](https://youtrack.jetbrains.com/issue/KT-73367) Migrate compose plugin to new IR parameter API
- [`KT-76429`](https://youtrack.jetbrains.com/issue/KT-76429) Migrate kotlin-dataframe plugin to new IR parameter API
- [`KT-75263`](https://youtrack.jetbrains.com/issue/KT-75263) PowerAssert: no additional info is displayed for 'when' with subject
- [`KT-75614`](https://youtrack.jetbrains.com/issue/KT-75614) PowerAssert: handling of exceptions doesn't work inside assert function
- [`KT-75264`](https://youtrack.jetbrains.com/issue/KT-75264) PowerAssert: the diagram for try-catch with boolean expressions isn't clear
- [`KT-75663`](https://youtrack.jetbrains.com/issue/KT-75663) PowerAssert: 'contains' result for strings is displayed under the first parameter instead of 'in'
- [`KT-73897`](https://youtrack.jetbrains.com/issue/KT-73897) PowerAssert: Implicit argument detection is brittle in a number of cases
- [`KT-74315`](https://youtrack.jetbrains.com/issue/KT-74315) Kotlin Lombok: "Unresolved reference" on generating `@Builder` for static inner class where outer class is also using `@Builder`
- [`KT-72172`](https://youtrack.jetbrains.com/issue/KT-72172) File Leak occurring in Kotlin Daemon
- [`KT-75159`](https://youtrack.jetbrains.com/issue/KT-75159) Compose: Missing 'FunctionKeyMeta' annotation on lamdas declared in non-composable function
- [`KT-72877`](https://youtrack.jetbrains.com/issue/KT-72877) Power-Assert should provide IrExpression transformation API
- [`KT-73871`](https://youtrack.jetbrains.com/issue/KT-73871) PowerAssert: Comparison via operator overload results in confusing diagram
- [`KT-73898`](https://youtrack.jetbrains.com/issue/KT-73898) PowerAssert: Operator calls with multiple receivers incorrectly aligned
- [`KT-73870`](https://youtrack.jetbrains.com/issue/KT-73870) PowerAssert: Object should not be displayed

### Tools. Compiler plugins. Serialization

- [`KT-49632`](https://youtrack.jetbrains.com/issue/KT-49632) Provide diagnostic when custom serializer for generic type does not have required constructor signature ( ISE Null argument in ExpressionCodegen for parameter VALUE_PARAMETER)

### Tools. Gradle

#### New Features

- [`KT-75823`](https://youtrack.jetbrains.com/issue/KT-75823) Resources bundle with XCFrameworks for iOS
- [`KT-73418`](https://youtrack.jetbrains.com/issue/KT-73418) Gradle '--warning-mode' value should affect Gradle plugin diagnostics
- [`KT-73906`](https://youtrack.jetbrains.com/issue/KT-73906) Improve ToolingDiagnostic CLI rendering
- [`KT-73285`](https://youtrack.jetbrains.com/issue/KT-73285) Integrate Gradle Problem API with KGP diagnostics
- [`KT-61649`](https://youtrack.jetbrains.com/issue/KT-61649) Add Gradle compiler option for jvm-default
- [`KT-68659`](https://youtrack.jetbrains.com/issue/KT-68659) Collect reported Kotlin Gradle Plugin diagnostics into one HTML/Text file report instead of writing it to log

#### Fixes

- [`KT-75188`](https://youtrack.jetbrains.com/issue/KT-75188) Groovy plugin breaks access to internal members of test friendPaths classes in kotlin compilation
- [`KT-54110`](https://youtrack.jetbrains.com/issue/KT-54110) Change deprecation level to ERROR for kotlinOptions DSL
- [`KT-74277`](https://youtrack.jetbrains.com/issue/KT-74277) KGP / FreeBSD: "TargetSupportException: Unknown operating system: FreeBSD" during the build
- [`KT-75820`](https://youtrack.jetbrains.com/issue/KT-75820) Gradle: "ClassNotFoundException: intellij.util.containers.Stack" while parsing compilation warnings with IN_PROCESS execution strategy
- [`KT-64991`](https://youtrack.jetbrains.com/issue/KT-64991) Change deprecation level to error for KotlinCompilation.source
- [`KT-75107`](https://youtrack.jetbrains.com/issue/KT-75107) Add Gradle property to use new FIR IC runner
- [`KT-66133`](https://youtrack.jetbrains.com/issue/KT-66133) Finalize resolution strategy for resources and remove the one that is unused
- [`KT-59632`](https://youtrack.jetbrains.com/issue/KT-59632) KotlinCompileTool.setSource() should replace existing sources
- [`KT-62963`](https://youtrack.jetbrains.com/issue/KT-62963) Remove "kotlin.incremental.useClasspathSnapshot" property
- [`KT-76137`](https://youtrack.jetbrains.com/issue/KT-76137) Compatibility with Gradle 8.14 release
- [`KT-76797`](https://youtrack.jetbrains.com/issue/KT-76797) KGP: StdlibDependencyManagementKt.configureStdlibVersionAlignment() triggering eager configuration realization
- [`KT-76282`](https://youtrack.jetbrains.com/issue/KT-76282) Add missing Android Gradle plugin versions in tests
- [`KT-77011`](https://youtrack.jetbrains.com/issue/KT-77011) Update build regression benchmarks for 2.2.0 release
- [`KT-76138`](https://youtrack.jetbrains.com/issue/KT-76138) Compile against Gradle API 8.14
- [`KT-76139`](https://youtrack.jetbrains.com/issue/KT-76139) Run integration tests against Gradle 8.14
- [`KT-74007`](https://youtrack.jetbrains.com/issue/KT-74007) Not all the DSL features related to kotlinOptions are deprecated
- [`KT-77288`](https://youtrack.jetbrains.com/issue/KT-77288) Using 'KotlinJvmOptions' is an error - Gradle sync issue when using 2.2.0-Beta2 with Android Gradle Plugin
- [`KT-74887`](https://youtrack.jetbrains.com/issue/KT-74887) Compatibility with Gradle 8.13 release
- [`KT-73682`](https://youtrack.jetbrains.com/issue/KT-73682) Compatibility with Gradle 8.12 release
- [`KT-77035`](https://youtrack.jetbrains.com/issue/KT-77035) A compiler diagnostic isn't reported when its severity is set to warning with Gradle
- [`KT-70620`](https://youtrack.jetbrains.com/issue/KT-70620) Raise to error deprecation for KotlinCompilationOutput#resourcesDirProvider
- [`KT-68597`](https://youtrack.jetbrains.com/issue/KT-68597) Update KGP deprecations before 2.2
- [`KT-68325`](https://youtrack.jetbrains.com/issue/KT-68325) Add to Compiler Types DSL exceptions message possible ways of a solution
- [`KT-76951`](https://youtrack.jetbrains.com/issue/KT-76951) 'distribution-base' plugin is only applied in Gradle 8.13
- [`KT-73142`](https://youtrack.jetbrains.com/issue/KT-73142) Kotlin Gradle plugin: Remove usage of Gradle's internal ExecHandleBuilder
- [`KT-73968`](https://youtrack.jetbrains.com/issue/KT-73968) KotlinDependencyManagement tries to mutate configuration after it was resolved
- [`KT-74890`](https://youtrack.jetbrains.com/issue/KT-74890) Run Gradle integrations test against Gradle 8.13 release
- [`KT-74889`](https://youtrack.jetbrains.com/issue/KT-74889) Compile against Gradle 8.13 API
- [`KT-76052`](https://youtrack.jetbrains.com/issue/KT-76052) Support Gradle 8.13 for Problems API
- [`KT-73684`](https://youtrack.jetbrains.com/issue/KT-73684) Run integration tests against Gradle 8.12
- [`KT-76377`](https://youtrack.jetbrains.com/issue/KT-76377) Add integration tests for Problems API
- [`KT-74551`](https://youtrack.jetbrains.com/issue/KT-74551) Improve KGP-IT withDebug for tests with environment variables
- [`KT-74717`](https://youtrack.jetbrains.com/issue/KT-74717) Test publication with dependency constraints
- [`KT-75164`](https://youtrack.jetbrains.com/issue/KT-75164) Run Gradle incremental compilation tests with FIR runner
- [`KT-72694`](https://youtrack.jetbrains.com/issue/KT-72694) Accessing Task.project during execution is being deprecated in Gradle 8.12
- [`KT-76374`](https://youtrack.jetbrains.com/issue/KT-76374) Investigate and fix failing tests with configuration cache in KotlinDaemonIT: testDaemonMultiproject and testMultipleCompilations
- [`KT-76379`](https://youtrack.jetbrains.com/issue/KT-76379) Gradle: KotlinGradleFinishBuildHandler does not perform cleanup on configuration cache reuse
- [`KT-61911`](https://youtrack.jetbrains.com/issue/KT-61911) Gradle: make KGP to depend on fixated version of stdlib
- [`KT-76026`](https://youtrack.jetbrains.com/issue/KT-76026) [ToolingDiagnostic] Gradle warning mode=fail: no emoji replacement, title color unchanged
- [`KT-76025`](https://youtrack.jetbrains.com/issue/KT-76025) ToolingDiagnostic with FATAL Severity: ANSI escape codes are not rendered properly in the Build tool window in IntelliJ IDEA
- [`KT-70252`](https://youtrack.jetbrains.com/issue/KT-70252) Gradle: remove Intellij dependencies from KGP runtime
- [`KT-74333`](https://youtrack.jetbrains.com/issue/KT-74333) improve ToolingDiagnosticBuilder
- [`KT-73683`](https://youtrack.jetbrains.com/issue/KT-73683) Compile against Gradle API 8.12
- [`KT-75187`](https://youtrack.jetbrains.com/issue/KT-75187) Make KotlinToolingDiagnostics internal
- [`KT-75568`](https://youtrack.jetbrains.com/issue/KT-75568) Do not use env variables registered as CC inputs
- [`KT-73842`](https://youtrack.jetbrains.com/issue/KT-73842) Gradle: AGP failing tests with "Failed to calculate the value of property 'generalConfigurationMetrics'" using KGP
- [`KT-75262`](https://youtrack.jetbrains.com/issue/KT-75262) Gradle test-fixtures plugin apply order breaks the project
- [`KT-75277`](https://youtrack.jetbrains.com/issue/KT-75277) FUS statistics: 'java.lang.IllegalStateException: The value for this property cannot be changed any further' exception is thrown during project import
- [`KT-75026`](https://youtrack.jetbrains.com/issue/KT-75026) Corrupted NonSynchronizedMetricsContainer in parallel Gradle build
- [`KT-73849`](https://youtrack.jetbrains.com/issue/KT-73849) Categorize ToolingDiagnostics
- [`KT-74462`](https://youtrack.jetbrains.com/issue/KT-74462) Flaky Kotlin Gradle Plugin Tests: IsInIdeaEnvironmentValueSource$Inject not found
- [`KT-72329`](https://youtrack.jetbrains.com/issue/KT-72329) Consider bumping apiVersion for projects with compatibility setup
- [`KT-74772`](https://youtrack.jetbrains.com/issue/KT-74772) ToolingDiagnostic: title is not displayed on Windows
- [`KT-74485`](https://youtrack.jetbrains.com/issue/KT-74485) BuildFinishedListenerService is not thread-safe
- [`KT-74639`](https://youtrack.jetbrains.com/issue/KT-74639) Executable binaries for JVM test cannot be created unless an additional suffix is set in Groovy
- [`KT-72187`](https://youtrack.jetbrains.com/issue/KT-72187) Gradle tests are using incorrect Kotlin/Native distribution
- [`KT-57653`](https://youtrack.jetbrains.com/issue/KT-57653) Explicit API mode is not enabled when free compiler arguments are specified in Gradle project
- [`KT-51378`](https://youtrack.jetbrains.com/issue/KT-51378) Gradle 'buildSrc' compilation fails when newer version of Kotlin plugin is added to the build script classpath

### Tools. Gradle. Compiler plugins

- [`KT-58009`](https://youtrack.jetbrains.com/issue/KT-58009) `BaseKapt.annotationProcessorOptionProviders` should be a `List<CommandLineArgumentProvider>` instead of `List<Any>`
- [`KT-61928`](https://youtrack.jetbrains.com/issue/KT-61928) Clarify parameter types in KaptArguments and KaptJavacOption

### Tools. Gradle. JS

- [`KT-74859`](https://youtrack.jetbrains.com/issue/KT-74859) Gradle configuration cache issues related to RootPackageJsonTask
- [`KT-70357`](https://youtrack.jetbrains.com/issue/KT-70357) Remove JS/Dce deprecated Gradle DSL
- [`KT-71217`](https://youtrack.jetbrains.com/issue/KT-71217) KJS: 'Per-file' "Module not found: Error: Can't resolve void.mjs"
- [`KT-77119`](https://youtrack.jetbrains.com/issue/KT-77119) KJS: Gradle: Setting custom environment variables in KotlinJsTest tasks no longer works
- [`KT-74735`](https://youtrack.jetbrains.com/issue/KT-74735) KGP uses Gradle internal `CompositeProjectComponentArtifactMetadata`
- [`KT-71879`](https://youtrack.jetbrains.com/issue/KT-71879) Notice of upcoming deprecation for Boolean 'is-' properties in Gradle Groovy scripts
- [`KT-75863`](https://youtrack.jetbrains.com/issue/KT-75863) Wasm/JS: Deprecate phantom-js for Karma
- [`KT-75485`](https://youtrack.jetbrains.com/issue/KT-75485) KJS: "Module not found: Error: Can't resolve 'style-loader' and 'css-loader'" in 2.1.20-RC
- [`KT-74869`](https://youtrack.jetbrains.com/issue/KT-74869) KJS: `jsBrowserProductionWebpack` does not minify output with 2.1.20-Beta2

### Tools. Gradle. Multiplatform

#### New Features

- [`KT-60623`](https://youtrack.jetbrains.com/issue/KT-60623) Deprecate `publishAllLibraryVariants` in kotlin-android

#### Fixes

- [`KT-75161`](https://youtrack.jetbrains.com/issue/KT-75161) Deprecate commonization parameters in KGP with an error
- [`KT-74005`](https://youtrack.jetbrains.com/issue/KT-74005) Implement a prototype of Unified Klib support in Kotlin Gradle Plugin
- [`KT-61817`](https://youtrack.jetbrains.com/issue/KT-61817) Remove support for Legacy Metadata Compilation with support of Compatibility Metadata Variant
- [`KT-71634`](https://youtrack.jetbrains.com/issue/KT-71634) KGP: Remove KotlinTarget.useDisambiguationClassifierAsSourceSetNamePrefix and overrideDisambiguationClassifierOnIdeImport
- [`KT-77404`](https://youtrack.jetbrains.com/issue/KT-77404) The kotlin-stdlib and annotations are missing from commonTest dependencies with 2.2.0-Beta1
- [`KT-62643`](https://youtrack.jetbrains.com/issue/KT-62643) Increase DeprecationLevel to 'Error' on deprecated 'ExtrasProperty.kt' (Kotlin 2.2)
- [`KT-75605`](https://youtrack.jetbrains.com/issue/KT-75605) Dependency resolution fails in commonTest/nativeTest source sets for KMP module when depending on another project due to missing PSM
- [`KT-71698`](https://youtrack.jetbrains.com/issue/KT-71698) Remove preset APIs
- [`KT-68015`](https://youtrack.jetbrains.com/issue/KT-68015) Remove legacy KMP flags
- [`KT-74727`](https://youtrack.jetbrains.com/issue/KT-74727) Dependency resolve from a single target KMP module to another kmp module fails on non-found PSM
- [`KT-75808`](https://youtrack.jetbrains.com/issue/KT-75808) KGP: MPP with jvm target and Gradle java-test-fixtures is broken
- [`KT-59315`](https://youtrack.jetbrains.com/issue/KT-59315) Improve the readability of KGP diagnostics in CLI build output
- [`KT-58231`](https://youtrack.jetbrains.com/issue/KT-58231) Kotlin Gradle Plugin: set deprecation level to Error for KotlinTarget.useDisambiguationClassifierAsSourceSetNamePrefix and overrideDisambiguationClassifierOnIdeImport
- [`KT-66423`](https://youtrack.jetbrains.com/issue/KT-66423) Configuration cache false recalculation because of Kotlin Native downloading during the execution phase
- [`KT-74888`](https://youtrack.jetbrains.com/issue/KT-74888) Use 'distribution-base' plugin in KMP/JVM
- [`KT-76659`](https://youtrack.jetbrains.com/issue/KT-76659) Write proper diagnostics for Uklib checks
- [`KT-70493`](https://youtrack.jetbrains.com/issue/KT-70493) Improve gray-box testing experience in KGP-IT
- [`KT-71608`](https://youtrack.jetbrains.com/issue/KT-71608) Remove 'android()' target
- [`KT-75512`](https://youtrack.jetbrains.com/issue/KT-75512) Maven-publish: ArtifactId is not correct  in`pom` file with customized `withXml`
- [`KT-72203`](https://youtrack.jetbrains.com/issue/KT-72203) Swift Export: Unclear failure for invalid module name
- [`KT-74278`](https://youtrack.jetbrains.com/issue/KT-74278) KSP tasks don't trigger a K/N distribution downloading
- [`KT-74669`](https://youtrack.jetbrains.com/issue/KT-74669) Executable binaries for JVM: a jar generated by jvmJar task isn't added to the build/install/testAppName/lib directory
- [`KT-69200`](https://youtrack.jetbrains.com/issue/KT-69200) Module 'intellij.kotlin.gradle.multiplatformTests' transitively depends on K1/K2 implementation
- [`KT-71454`](https://youtrack.jetbrains.com/issue/KT-71454) Remove not compatible with Project Isolation PomDependenciesRewriter
- [`KT-73536`](https://youtrack.jetbrains.com/issue/KT-73536) Enable kmp isolated projects support for kotlin-test and patch PSM.json

### Tools. Gradle. Native

- [`KT-77067`](https://youtrack.jetbrains.com/issue/KT-77067) Kotlin Gradle plugin with the configuration cache passes all platform libraries to the compiler when compiling a binary for the first time
- [`KT-74953`](https://youtrack.jetbrains.com/issue/KT-74953) Deprecate kotlinArtifacts with a warning
- [`KT-71069`](https://youtrack.jetbrains.com/issue/KT-71069) Remove `konanVersion ` from CInteropProcess
- [`KT-75171`](https://youtrack.jetbrains.com/issue/KT-75171) Provide custom freeCompilerArgs to Swift Export's link task
- [`KT-74591`](https://youtrack.jetbrains.com/issue/KT-74591) HostManager.isMingw isLinux and isMac are not accessible in groovy scripts
- [`KT-65692`](https://youtrack.jetbrains.com/issue/KT-65692) Remove Kotlin Native Performance plugin
- [`KT-74403`](https://youtrack.jetbrains.com/issue/KT-74403) :commonizeNativeDistribution fails when configured native targets cannot be built on machine

### Tools. Gradle. Xcode

- [`KT-66262`](https://youtrack.jetbrains.com/issue/KT-66262) Deprecate and remove support for bitcode embedding from the Kotlin Gradle plugin

### Tools. Incremental Compile

- [`KT-62555`](https://youtrack.jetbrains.com/issue/KT-62555) Wrong ABI fingerprint for inline function containing a lambda
- [`KT-75883`](https://youtrack.jetbrains.com/issue/KT-75883) Follow-up: switch from INSTANCE heuristic to outerClass chain
- [`KT-75276`](https://youtrack.jetbrains.com/issue/KT-75276) Test IC issues with first-round failures that might be fixed by Fir runner
- [`KT-76041`](https://youtrack.jetbrains.com/issue/KT-76041) Make lenient mode work with IC
- [`KT-75155`](https://youtrack.jetbrains.com/issue/KT-75155) Split HistoryFileJvmIncrementalCompilerRunner and the current one
- [`KT-74628`](https://youtrack.jetbrains.com/issue/KT-74628) Incremental compilation runner does not check compiler exit code before mapping sources to classes

### Tools. JPS

- [`KT-60914`](https://youtrack.jetbrains.com/issue/KT-60914) IC misses dependency to recompile when named kt file with JvmField instructed property was replaced with an object with the same name
- [`KT-76495`](https://youtrack.jetbrains.com/issue/KT-76495) JPS: delegated Maven builds use embeddable version of kotlin-serialization compiler plugin with non-embeddable Kotlin compiler
- [`KT-76461`](https://youtrack.jetbrains.com/issue/KT-76461) Fix "compilation of typealias does not check for clashes" for JPS
- [`KT-75917`](https://youtrack.jetbrains.com/issue/KT-75917) Unused imports may lead to inc compilation failure
- [`KT-73379`](https://youtrack.jetbrains.com/issue/KT-73379) Review the usage of JavaBuilder.IS_ENABLED inside the KotlinBuilder JSP builder
- [`KT-63707`](https://youtrack.jetbrains.com/issue/KT-63707) JPS: "Multiple values are not allowed for" caused by Compose

### Tools. Kapt

- [`KT-75936`](https://youtrack.jetbrains.com/issue/KT-75936) K2 KAPT: unsupported FIR element kinds in constant evaluation
- [`KT-64385`](https://youtrack.jetbrains.com/issue/KT-64385) K2: Enable K2 KAPT by default
- [`KT-76546`](https://youtrack.jetbrains.com/issue/KT-76546) Kapt / CLI: ""compile" mode is not supported in Kotlin 2.x" with -version flag
- [`KT-75942`](https://youtrack.jetbrains.com/issue/KT-75942) K2 KAPT: underscore not allowed here
- [`KT-40485`](https://youtrack.jetbrains.com/issue/KT-40485) -Xjvm-default=all causes private interface methods to be generated in JVM target < 9 which is not supported in annotation processing
- [`KT-75202`](https://youtrack.jetbrains.com/issue/KT-75202) K2 kapt: mapped type class literal is converted incorrectly
- [`KT-70797`](https://youtrack.jetbrains.com/issue/KT-70797) Remove obsolete K2 kapt implementation based on Analysis API

### Tools. Maven

- [`KT-73012`](https://youtrack.jetbrains.com/issue/KT-73012) Migrate Kotlin Maven plugin to the Build Tools API
- [`KT-77036`](https://youtrack.jetbrains.com/issue/KT-77036) Kotlin Maven plugin: ClassNotFoundException com.google.common.base.Joiner with compiler plugins in debug mode
- [`KT-61285`](https://youtrack.jetbrains.com/issue/KT-61285) Remove multiplatform "support" from Maven Plugin

### Tools. Performance benchmarks

- [`KT-75563`](https://youtrack.jetbrains.com/issue/KT-75563) Fix crash on kotlin compiler server user project related to performance measurements

### Tools. REPL

#### Fixes

- [`KT-75632`](https://youtrack.jetbrains.com/issue/KT-75632) Contunue deprecation of the REPL built into `kotlinc`
- [`KT-76507`](https://youtrack.jetbrains.com/issue/KT-76507) [K2 Repl] Delegated properties are not visible in the next snippet
- [`KT-76508`](https://youtrack.jetbrains.com/issue/KT-76508) [K2 Repl] Annotations on property accessors are not resolved
- [`KT-75672`](https://youtrack.jetbrains.com/issue/KT-75672) [K2 Repl] Serialization plugin crashes compiler backend
- [`KT-76009`](https://youtrack.jetbrains.com/issue/KT-76009) [K2 Repl] Kotlin-specific imports does not work if dependency is added to the classpath after 1st snippet
- [`KT-75580`](https://youtrack.jetbrains.com/issue/KT-75580) [K2 Repl] Cannot access snippet properties using Kotlin reflection
- [`KT-75616`](https://youtrack.jetbrains.com/issue/KT-75616) [K2 Repl] Sealed hierarchies causes a FileAnalysisException
- [`KT-75593`](https://youtrack.jetbrains.com/issue/KT-75593) [K2 Repl] Custom Delegates crash code gen
- [`KT-75607`](https://youtrack.jetbrains.com/issue/KT-75607) [K2 Repl] ScriptingConfiguration.jvm.jvmTarget is not respected
- [`KT-74607`](https://youtrack.jetbrains.com/issue/KT-74607) [K2 Repl] Lambda statement crashes code generation
- [`KT-74615`](https://youtrack.jetbrains.com/issue/KT-74615) [K2 REPL] Anonymous objects crash code generation
- [`KT-74856`](https://youtrack.jetbrains.com/issue/KT-74856) [K2 Repl] Snippet class files are missing Kotlin metadata
- [`KT-74768`](https://youtrack.jetbrains.com/issue/KT-74768) [K2 Repl] refineConfiguration does not update the classpath correctly
- [`KT-74593`](https://youtrack.jetbrains.com/issue/KT-74593) [K2 Repl] defaultImports does not work in ScriptCompilationConfiguration

### Tools. Scripts

- [`KT-75589`](https://youtrack.jetbrains.com/issue/KT-75589) Scripts: "IndexOutOfBoundsException in jdk.internal.util.Preconditions.outOfBounds" when trying to extend a class which uses global variable
- [`KT-76424`](https://youtrack.jetbrains.com/issue/KT-76424) Dependencies in main.kts not working with 2.1.20
- [`KT-76296`](https://youtrack.jetbrains.com/issue/KT-76296) Kotlin script compiler crashes when secondary constructor calls a function
- [`KT-76430`](https://youtrack.jetbrains.com/issue/KT-76430) Migrate scripting plugin to new IR parameter API
- [`KT-74004`](https://youtrack.jetbrains.com/issue/KT-74004) "Evaluate expression" fails in scripts

### Tools. Wasm

- [`KT-76161`](https://youtrack.jetbrains.com/issue/KT-76161) Wasm: "export startUnitTests was not found" after updating to Kotlin 2.1.20
- [`KT-73398`](https://youtrack.jetbrains.com/issue/KT-73398) K/Wasm: Separate from JS NPM infrastructure
- [`KT-76948`](https://youtrack.jetbrains.com/issue/KT-76948) Wasm: Rename kotlinBinaryenSetup and kotlinD8Setup
- [`KT-74840`](https://youtrack.jetbrains.com/issue/KT-74840) Wasm: Binaryen setup per project
- [`KT-76657`](https://youtrack.jetbrains.com/issue/KT-76657) K/Wasm: Composite build does not work with wasm tasks
- [`KT-76656`](https://youtrack.jetbrains.com/issue/KT-76656) K/Wasm: Change NPM project name of wasm projects
- [`KT-76587`](https://youtrack.jetbrains.com/issue/KT-76587) Wasm lock check failure says to run the JS lock upgrade
- [`KT-76330`](https://youtrack.jetbrains.com/issue/KT-76330) K/Wasm: update binaryen to 123 or newer
- [`KT-74480`](https://youtrack.jetbrains.com/issue/KT-74480) Projects using Compose don't run in Android Studio started from 2.1.20- Kotlin version

## Previous ChangeLogs:
### [ChangeLog-2.1.X](docs/changelogs/ChangeLog-2.1.X.md)
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