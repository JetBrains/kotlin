## 2.1.0

### Analysis API

#### New Features

- [`KT-68603`](https://youtrack.jetbrains.com/issue/KT-68603) KotlinDirectInheritorsProvider: add an option to ignore non-kotlin results

#### Performance Improvements

- [`KT-70757`](https://youtrack.jetbrains.com/issue/KT-70757) Performance problem in KaFirVisibilityChecker for KaFirPsiJavaClassSymbol

#### Fixes

- [`KT-70437`](https://youtrack.jetbrains.com/issue/KT-70437) Class reference is not resolvable
- [`KT-57733`](https://youtrack.jetbrains.com/issue/KT-57733) Analysis API: Use optimized `ModuleWithDependenciesScope`s in combined symbol providers
- [`KT-72389`](https://youtrack.jetbrains.com/issue/KT-72389) K2: False positive "Redundant 'protected' modifier" for protected property inside protected constructor from private or internal class
- [`KT-69190`](https://youtrack.jetbrains.com/issue/KT-69190) K2: False-positive "redundant private modifier"
- [`KT-64984`](https://youtrack.jetbrains.com/issue/KT-64984) Analysis API: Support Wasm target
- [`KT-70375`](https://youtrack.jetbrains.com/issue/KT-70375) K2: NPE at org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedClassSymbolBase.createPointer
- [`KT-71259`](https://youtrack.jetbrains.com/issue/KT-71259) K2 evaluator: Invalid smart cast info collecting for Code Fragments
- [`KT-69360`](https://youtrack.jetbrains.com/issue/KT-69360) Lack of implicit receiver for the last statement under lambda in scripts
- [`KT-70890`](https://youtrack.jetbrains.com/issue/KT-70890) Analysis API: Experiment with weak references to LL FIR/analysis sessions in session caches
- [`KT-70657`](https://youtrack.jetbrains.com/issue/KT-70657) Analysis API: Inner types from classes with generics are incorrectly represented by the compiled jars
- [`KT-71055`](https://youtrack.jetbrains.com/issue/KT-71055) Suspend calls inside 'analyze()' break the block guarantees
- [`KT-70815`](https://youtrack.jetbrains.com/issue/KT-70815) Analysis API: Implement stop-the-world session invalidation
- [`KT-69819`](https://youtrack.jetbrains.com/issue/KT-69819) K2 IDE: LHS type in callable references is unresolved when it has type arguments and is qualified
- [`KT-68761`](https://youtrack.jetbrains.com/issue/KT-68761) Analysis API: Experiment with limited-size cache in `KaFirSessionProvider`
- [`KT-70384`](https://youtrack.jetbrains.com/issue/KT-70384) Analysis API Standalone: The same class in the same two renamed jars is unresolved
- [`KT-71067`](https://youtrack.jetbrains.com/issue/KT-71067) Exceptions from references cancel Find Usages
- [`KT-69535`](https://youtrack.jetbrains.com/issue/KT-69535) Redesign 'containingSymbol'
- [`KT-71025`](https://youtrack.jetbrains.com/issue/KT-71025) K2 IDE: Scopes in "importingScopeContext" have reversed ordering and "indexInTower" values
- [`KT-67483`](https://youtrack.jetbrains.com/issue/KT-67483) K2 IDE: Serializable plugin causes infinite resolve recursion when there is a star import from a class with annotation call
- [`KT-69416`](https://youtrack.jetbrains.com/issue/KT-69416) K2 IDE / Completion: “No classifier found” on simple value creating
- [`KT-70257`](https://youtrack.jetbrains.com/issue/KT-70257) CCE: class kotlin.UInt cannot be cast to class java.lang.Number
- [`KT-70376`](https://youtrack.jetbrains.com/issue/KT-70376) K2 IDE / Kotlin Debugger: IAE “Only componentN functions should be cached this way, but got: toString” on evaluating toString() method for value class
- [`KT-70264`](https://youtrack.jetbrains.com/issue/KT-70264) AA: service registration via XML fails with AbstractMethodError in Lint CLI
- [`KT-69950`](https://youtrack.jetbrains.com/issue/KT-69950) Analysis API: Introduce `isSubtypeOf(ClassId)`
- [`KT-68625`](https://youtrack.jetbrains.com/issue/KT-68625) K2: “`lazyResolveToPhase(STATUS)` cannot be called from a transformer with a phase STATUS.”
- [`KT-67665`](https://youtrack.jetbrains.com/issue/KT-67665) K2: contract violation for value class with a constructor parameter with an implicit type
- [`KT-67009`](https://youtrack.jetbrains.com/issue/KT-67009) Analysis API: Add abbreviated type tests for type aliases from source modules
- [`KT-69977`](https://youtrack.jetbrains.com/issue/KT-69977) KaFirFunctionalType#getAbbreviation is always null
- [`KT-68341`](https://youtrack.jetbrains.com/issue/KT-68341) Analysis API: Expanded function types from libraries don't have an abbreviated type
- [`KT-68857`](https://youtrack.jetbrains.com/issue/KT-68857) Analysis API: Refactor annotations
- [`KT-70386`](https://youtrack.jetbrains.com/issue/KT-70386) Do not filter out overloads from different libraries in dangling files
- [`KT-65552`](https://youtrack.jetbrains.com/issue/KT-65552) K2: CANNOT_CHECK_FOR_ERASED in KtTypeCodeFragment
- [`KT-65803`](https://youtrack.jetbrains.com/issue/KT-65803) K2: Analysis API: KtFirTypeProvider#getSubstitutedSuperTypes throws an exception in the case of "Wrong number of type arguments"
- [`KT-68896`](https://youtrack.jetbrains.com/issue/KT-68896) Support VirtualFile binary dependency inputs to Analysis API modules
- [`KT-69395`](https://youtrack.jetbrains.com/issue/KT-69395) K2 IDE: incorrect overload selection from binary dependencies in a shared native source set
- [`KT-68573`](https://youtrack.jetbrains.com/issue/KT-68573) ISE: "Unexpected constant value (kotlin/annotation/AnnotationTarget, CLASS)" at Kt1DescUtilsKt.toKtConstantValue()
- [`KT-69576`](https://youtrack.jetbrains.com/issue/KT-69576) Analysis API: FIR implementation of "isImplicitReferenceToCompanion" returns false for companion references in implicit invoke operator calls
- [`KT-69568`](https://youtrack.jetbrains.com/issue/KT-69568) Analysis API: FIR implementation of "isImplicitReferenceToCompanion" returns true for non-companion references in qualified calls
- [`KT-69436`](https://youtrack.jetbrains.com/issue/KT-69436) Analysis API Platform: Encapsulate `LLFirDeclarationModificationService` as an engine service
- [`KT-63004`](https://youtrack.jetbrains.com/issue/KT-63004) K2: Analysis API: Design API for querying declarations generated by compiler plugins (similar to indices)
- [`KT-69452`](https://youtrack.jetbrains.com/issue/KT-69452) AA FIR: wrong source PSI after compile-time evaluation
- [`KT-69598`](https://youtrack.jetbrains.com/issue/KT-69598) AA: definitely not-null type at receiver position should be wrapped in parenthesis
- [`KT-60484`](https://youtrack.jetbrains.com/issue/KT-60484) Analysis API: add support for KtType pointers similar to KtSymbolPointer
- [`KT-68884`](https://youtrack.jetbrains.com/issue/KT-68884) Analysis API: Rename/deprecate/remove declarations as part of Stabilization
- [`KT-69453`](https://youtrack.jetbrains.com/issue/KT-69453) AA FIR: miss to handle expected type of lambda with explicit label
- [`KT-69533`](https://youtrack.jetbrains.com/issue/KT-69533) Protect implementation parts of Analysis API with opt-in annotations

### Analysis API. FIR

#### Performance Improvements

- [`KT-71566`](https://youtrack.jetbrains.com/issue/KT-71566) FirElementBuilder#getFirForNonKtFileElement should iterate a Psi file over and over
- [`KT-71224`](https://youtrack.jetbrains.com/issue/KT-71224) Analysis API: `FirElementFinder.collectDesignationPath` relies on naive iteration through FIR files

#### Fixes

- [`KT-70327`](https://youtrack.jetbrains.com/issue/KT-70327) Analysis API: Batch inspection causes deadlock in `ValueWithPostCompute`
- [`KT-69070`](https://youtrack.jetbrains.com/issue/KT-69070) Analysis API: Querying declared member scope for Java symbols results in exception in some use cases
- [`KT-68268`](https://youtrack.jetbrains.com/issue/KT-68268) LLSealedInheritorsProvider: reduce scope to kotlin files
- [`KT-69671`](https://youtrack.jetbrains.com/issue/KT-69671) TYPES phase contract violation through JavaSymbolProvider
- [`KT-70624`](https://youtrack.jetbrains.com/issue/KT-70624) Declaration symbols from code fragments are treated as not local
- [`KT-70662`](https://youtrack.jetbrains.com/issue/KT-70662) NPE: FirLazyBodiesCalculatorKt.calculateLazyBodyForProperty
- [`KT-70859`](https://youtrack.jetbrains.com/issue/KT-70859) Do not fail highlighting due to resolution problems
- [`KT-70474`](https://youtrack.jetbrains.com/issue/KT-70474) FirLazyResolveContractViolationException from JavaSymbolProvider
- [`KT-70323`](https://youtrack.jetbrains.com/issue/KT-70323) FirLazyResolveContractViolationException: `lazyResolveToPhase(TYPES)` cannot be called from a transformer with a phase TYPES
- [`KT-71567`](https://youtrack.jetbrains.com/issue/KT-71567) LLFirCompilerRequiredAnnotationsTargetResolver should calculate annotation arguments on demand
- [`KT-71584`](https://youtrack.jetbrains.com/issue/KT-71584) `getNonLocalContainingOrThisDeclaration` treats KtParameter from functional type as non-local

### Analysis API. Light Classes

#### Performance Improvements

- [`KT-69998`](https://youtrack.jetbrains.com/issue/KT-69998) Drop redundant cache from ClassInnerStuffCache

#### Fixes

- [`KT-69833`](https://youtrack.jetbrains.com/issue/KT-69833) Support value classes
- [`KT-71693`](https://youtrack.jetbrains.com/issue/KT-71693) Wrong name mangling for JvmField class property and companion property clash
- [`KT-71469`](https://youtrack.jetbrains.com/issue/KT-71469) KtLightClassForDecompiledDeclaration: missed kotlinOrigin
- [`KT-70710`](https://youtrack.jetbrains.com/issue/KT-70710) Provide light classes for KMP modules in Android Lint
- [`KT-70548`](https://youtrack.jetbrains.com/issue/KT-70548) SLC: text of class object access expression is not the same as raw text
- [`KT-70572`](https://youtrack.jetbrains.com/issue/KT-70572) SLC: missing `isInheritor` implementation for type parameter
- [`KT-70491`](https://youtrack.jetbrains.com/issue/KT-70491) SLC: inconsistent source PSI of no-arg constructor for all default values
- [`KT-70458`](https://youtrack.jetbrains.com/issue/KT-70458) SLC: missed `auxiliaryOriginalElement` for delegated property
- [`KT-70232`](https://youtrack.jetbrains.com/issue/KT-70232) Support a companion object inside value classes
- [`KT-70349`](https://youtrack.jetbrains.com/issue/KT-70349) `@delegate`:` annotations are missed for light class fields
- [`KT-68328`](https://youtrack.jetbrains.com/issue/KT-68328) Move KtLightClassBase to ULC

### Analysis API. Providers and Caches

- [`KT-65618`](https://youtrack.jetbrains.com/issue/KT-65618) K2: resulted FirClass.psi != requested PsiClass from completion
- [`KT-69292`](https://youtrack.jetbrains.com/issue/KT-69292) K2: Analysis API: A property's `MUST_BE_INITIALIZED` diagnostic is not updated after changing `field` usage in an accessor
- [`KT-71468`](https://youtrack.jetbrains.com/issue/KT-71468) Drop redundant logic from LLFirJavaFacadeForBinaries
- [`KT-71700`](https://youtrack.jetbrains.com/issue/KT-71700) Cache result of resolveToCall
- [`KT-71520`](https://youtrack.jetbrains.com/issue/KT-71520) Analysis API: `LLFirNativeForwardDeclarationsSymbolProvider` spends a lot of time in indices

### Analysis API. Standalone

- [`KT-65110`](https://youtrack.jetbrains.com/issue/KT-65110) Analysis API: In Standalone mode the order of symbols is unstable

### Analysis API. Stubs and Decompilation

- [`KT-71565`](https://youtrack.jetbrains.com/issue/KT-71565) KtClassOrObject should use isLocal from greenStub

### Analysis API. Surface

#### New Features

- [`KT-69960`](https://youtrack.jetbrains.com/issue/KT-69960) `resolveToCallCandidates` should support operators
- [`KT-69961`](https://youtrack.jetbrains.com/issue/KT-69961) `resolveToCallCandidates` should support properties

#### Performance Improvements

- [`KT-70529`](https://youtrack.jetbrains.com/issue/KT-70529) KaSymbol: reduce the number of `cached` usages
- [`KT-70165`](https://youtrack.jetbrains.com/issue/KT-70165) Introduce PSI-based `KaSymbol`s for K2

#### Fixes

- [`KT-69371`](https://youtrack.jetbrains.com/issue/KT-69371) Analysis API: expose only interfaces/abstract classes for the resolution API
- [`KT-69696`](https://youtrack.jetbrains.com/issue/KT-69696) KaSymbolByFirBuilder should filter call-site substitutions
- [`KT-69679`](https://youtrack.jetbrains.com/issue/KT-69679) KaDelegatedConstructorCall should have substituted signature
- [`KT-70206`](https://youtrack.jetbrains.com/issue/KT-70206) `anonymousSymbol` API throws an exception for regular functions
- [`KT-69699`](https://youtrack.jetbrains.com/issue/KT-69699) Receiver type is not substituted in the case of conflict declarations
- [`KT-69381`](https://youtrack.jetbrains.com/issue/KT-69381) Analysis API: Investigate the viability of current `KaSymbol` caches
- [`KT-70199`](https://youtrack.jetbrains.com/issue/KT-70199) K2: ConcurrentModificationException at FirCallCompleter$LambdaAnalyzerImpl.analyzeAndGetLambdaReturnArguments
- [`KT-70661`](https://youtrack.jetbrains.com/issue/KT-70661) Invalid FirDeclarationOrigin ScriptTopLevelDestructuringDeclarationContainer
- [`KT-70663`](https://youtrack.jetbrains.com/issue/KT-70663) KaFirDestructuringDeclarationSymbol: Failed requirement
- [`KT-63490`](https://youtrack.jetbrains.com/issue/KT-63490) Analysis API: Accessing the Analysis API should be prohibited during dumb mode
- [`KT-63390`](https://youtrack.jetbrains.com/issue/KT-63390) K2: Analysis API: add annotations to KtClassInitializerSymbol
- [`KT-55124`](https://youtrack.jetbrains.com/issue/KT-55124) Design common ancestor for KtValueParameter and KtReceiverParameterSymbol
- [`KT-71731`](https://youtrack.jetbrains.com/issue/KT-71731) directlyOverridenSymbols/allOverridenSymbols works incorrectly for intersection overrides

### Apple Ecosystem

- [`KT-66262`](https://youtrack.jetbrains.com/issue/KT-66262) Deprecate and remove support for bitcode embedding from the Kotlin Gradle plugin
- [`KT-66894`](https://youtrack.jetbrains.com/issue/KT-66894) XCFramework task fails when name passed to xcframework DSL is different from framework's name
- [`KT-65675`](https://youtrack.jetbrains.com/issue/KT-65675) XCFrameworkTask produces an xcframework with mismatched casing in embedded frameworks
- [`KT-69119`](https://youtrack.jetbrains.com/issue/KT-69119) xcodeVersion task fails if Xcode isn't installed and apple-specific native targets aren't declared

### Backend. Wasm

#### New Features

- [`KT-70786`](https://youtrack.jetbrains.com/issue/KT-70786) Improve DX of the variable view during debugging in Chrome/Firefox for Kotlin/Wasm
- [`KT-70331`](https://youtrack.jetbrains.com/issue/KT-70331) Support incremental compilation for the Wasm backend
- [`KT-71686`](https://youtrack.jetbrains.com/issue/KT-71686) K/Wasm: Add functions to convert between Kotlin and JS array types
- [`KT-68185`](https://youtrack.jetbrains.com/issue/KT-68185) [WasmJs] Attach js exception object to JsException

#### Fixes

- [`KT-71294`](https://youtrack.jetbrains.com/issue/KT-71294) Wasm Artifacts/Resource are being loaded relatively instead of absolutely
- [`KT-71473`](https://youtrack.jetbrains.com/issue/KT-71473) K/Wasm: Use `--closed-world` and related options for Binaryen
- [`KT-72297`](https://youtrack.jetbrains.com/issue/KT-72297) [Wasm] Unused associated object class lead to compiler fail
- [`KT-72156`](https://youtrack.jetbrains.com/issue/KT-72156) custom-formatters.js exists in JAR after publishToMavenLocal but not in the published artifact in Maven public
- [`KT-65799`](https://youtrack.jetbrains.com/issue/KT-65799) K/Wasm: remove default exports from wasm exports
- [`KT-71800`](https://youtrack.jetbrains.com/issue/KT-71800) Wasm compiler: Fix member generation for data classes with an array-type property
- [`KT-71580`](https://youtrack.jetbrains.com/issue/KT-71580) String::toFloat on wasm behaves differently compared to other targets
- [`KT-71523`](https://youtrack.jetbrains.com/issue/KT-71523) K/Wasm: cleanup after fix for KT-71474
- [`KT-71475`](https://youtrack.jetbrains.com/issue/KT-71475) K/Wasm: KClass::qualifiedName returns incorrect result for nested or companion objects
- [`KT-71474`](https://youtrack.jetbrains.com/issue/KT-71474) K/Wasm: KProperty*Impl equals work incorrectly for clabbale reference created in different files or modules
- [`KT-61130`](https://youtrack.jetbrains.com/issue/KT-61130) K/Wasm: Function signatures may clash with base class internal methods from a friend module
- [`KT-70820`](https://youtrack.jetbrains.com/issue/KT-70820) [Kotlin QG] wasm-validator fails when running compile[...]KotlinWasmJsOptimize
- [`KT-70819`](https://youtrack.jetbrains.com/issue/KT-70819) [Kotlin QG] node.js fails when running wasmJs[...]Test KGP tasks
- [`KT-70394`](https://youtrack.jetbrains.com/issue/KT-70394) Investigate increased wasm binary size after switching stdlib compilation to K2
- [`KT-69627`](https://youtrack.jetbrains.com/issue/KT-69627) Remove `create###Array` functions from WASM stdlib
- [`KT-68509`](https://youtrack.jetbrains.com/issue/KT-68509) Fatal: error validating input in compileProductionExecutableKotlinWasmJsOptimize

### Compiler

#### New Features

- [`KT-71094`](https://youtrack.jetbrains.com/issue/KT-71094) Kotlin/Native incremental compilation: fail compilation if cache build failed
- [`KT-21908`](https://youtrack.jetbrains.com/issue/KT-21908) Support 'when' exhaustiveness checking for generic type parameter with sealed class upper bound
- [`KT-70679`](https://youtrack.jetbrains.com/issue/KT-70679) Kotlin/Native: fill WritableTypeInfo from Swift Export type mapping
- [`KT-59798`](https://youtrack.jetbrains.com/issue/KT-59798) Builder inference is not working when combined with `let` expression
- [`KT-54227`](https://youtrack.jetbrains.com/issue/KT-54227) Cannot use nullable Nothing as reified type parameter
- [`KT-71430`](https://youtrack.jetbrains.com/issue/KT-71430) Kotlin-to-Java direct actualization implementation
- [`KT-68163`](https://youtrack.jetbrains.com/issue/KT-68163) Expose supplementary compiler warnings via CLI
- [`KT-69321`](https://youtrack.jetbrains.com/issue/KT-69321) Swift export: enable auto-linkage of binary dependencies
- [`KT-11526`](https://youtrack.jetbrains.com/issue/KT-11526) Improve diagnostics for "X overrides nothing"
- [`KT-49710`](https://youtrack.jetbrains.com/issue/KT-49710) False positive NO_ELSE_IN_WHEN with nullable type as receiver
- [`KT-69729`](https://youtrack.jetbrains.com/issue/KT-69729) Support calling super interface Java methods from Kotlin interface
- [`KT-69508`](https://youtrack.jetbrains.com/issue/KT-69508) Improve "Public-API inline function cannot access non-public-API" check for the inline property accessors

#### Performance Improvements

- [`KT-71353`](https://youtrack.jetbrains.com/issue/KT-71353) FP Kotlin performance degradation (around Cone types hierarchy changes)
- [`KT-71159`](https://youtrack.jetbrains.com/issue/KT-71159) [K2] OOM on large enum classes with fields
- [`KT-69718`](https://youtrack.jetbrains.com/issue/KT-69718) K2: Check for jvm nullability annotations in fir2ir is slow
- [`KT-68417`](https://youtrack.jetbrains.com/issue/KT-68417) Native: LLVM 16 inliner is slow on K/N-produced modules
- [`KT-63971`](https://youtrack.jetbrains.com/issue/KT-63971) K2: Redundant `@ParameterName` in abbreviated type in metadata

#### Fixes

- [`KT-71550`](https://youtrack.jetbrains.com/issue/KT-71550) JVM IR: NPE on identity equals of boolean true with null
- [`KT-72214`](https://youtrack.jetbrains.com/issue/KT-72214) -fpass-plugin (clangFlags) is not applied since Kotlin 2.0.20
- [`KT-68933`](https://youtrack.jetbrains.com/issue/KT-68933) CompilationException: Back-end: Could not get inlined class
- [`KT-72255`](https://youtrack.jetbrains.com/issue/KT-72255) Promote jspecify from warning to error
- [`KT-73065`](https://youtrack.jetbrains.com/issue/KT-73065) CCE with context receivers
- [`KT-61033`](https://youtrack.jetbrains.com/issue/KT-61033) K2: implement a diagnostic corresponding to K1's MISSING_BUILT_IN_DECLARATION
- [`KT-72345`](https://youtrack.jetbrains.com/issue/KT-72345) K2: Method 'get' without `@Override` annotation not called
- [`KT-71260`](https://youtrack.jetbrains.com/issue/KT-71260) K2: Internal compiler error in IrFakeOverrideSymbolBase.getOwner when there is no actual for expect
- [`KT-72996`](https://youtrack.jetbrains.com/issue/KT-72996) false-positive unresolved reference error on an overloaded callable reference in a lambda return position on the left-hand size of an elvis operator
- [`KT-72552`](https://youtrack.jetbrains.com/issue/KT-72552) AutoboxingTransformer fails on during linkage on nested lambdas with cinteroped types
- [`KT-71751`](https://youtrack.jetbrains.com/issue/KT-71751) K2: Skipping code in last statement of lambda
- [`KT-71121`](https://youtrack.jetbrains.com/issue/KT-71121) Kotlin/JS incremental compilation fails with KotlinIllegalArgumentExceptionWithAttachments
- [`KT-60521`](https://youtrack.jetbrains.com/issue/KT-60521) Drop language versions 1.4 and 1.5
- [`KT-70461`](https://youtrack.jetbrains.com/issue/KT-70461) K2: "Inline class types should have the same representation" caused by value class and smart check
- [`KT-72238`](https://youtrack.jetbrains.com/issue/KT-72238)  Argument type mismatch in builder inside extension function after ?:
- [`KT-70306`](https://youtrack.jetbrains.com/issue/KT-70306) K2: Lambdas are unserializable: inferred from Java param `? super I`
- [`KT-67383`](https://youtrack.jetbrains.com/issue/KT-67383) Incorrect optimisation when optimising for loop with UByte
- [`KT-68653`](https://youtrack.jetbrains.com/issue/KT-68653) Switch latest stable language version in Kotlin project to 2.1
- [`KT-71708`](https://youtrack.jetbrains.com/issue/KT-71708) False negative UNSUPPORTED for collection literals as trailing return value
- [`KT-72281`](https://youtrack.jetbrains.com/issue/KT-72281) K/N: "Failed to wait for cache to be built"
- [`KT-72017`](https://youtrack.jetbrains.com/issue/KT-72017) Enum property reflection returning null KClassifier property for Enum classes defined inside Kotlin Scripts
- [`KT-69040`](https://youtrack.jetbrains.com/issue/KT-69040) PCLA: deal with "deep" calls that can be fully analyzed properly
- [`KT-69920`](https://youtrack.jetbrains.com/issue/KT-69920) K2: java.lang.IllegalArgumentException: FirNamedArgumentExpressionImpl.replaceConeTypeOrNull() during Space project compilation
- [`KT-69549`](https://youtrack.jetbrains.com/issue/KT-69549) Try to move callable reference transformation earlier in pipeline
- [`KT-63944`](https://youtrack.jetbrains.com/issue/KT-63944) Kotlin/Native: Cache flavor selection doesn't respect GC kind
- [`KT-71649`](https://youtrack.jetbrains.com/issue/KT-71649) K2: Put operator on  mutableMap<T?, V>() causes crashes on null key
- [`KT-70667`](https://youtrack.jetbrains.com/issue/KT-70667) K2: "Type parameter * has inconsistent bounds" caused by wildcard and where-clause
- [`KT-70562`](https://youtrack.jetbrains.com/issue/KT-70562) `@SubclassOptInRequired` cannot accept multiple experimental marker
- [`KT-69407`](https://youtrack.jetbrains.com/issue/KT-69407) K2: Compiler crash (Shouldn't be here) due to unresolved reference in FirProjectionRelationChecker
- [`KT-71508`](https://youtrack.jetbrains.com/issue/KT-71508) JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS isn't reported when java class is inherited from an effectively private class
- [`KT-72178`](https://youtrack.jetbrains.com/issue/KT-72178) K2: "Unexpected FirPlaceholderProjectionImpl" exception when using "_" as key type in EnumMap
- [`KT-70407`](https://youtrack.jetbrains.com/issue/KT-70407) Error/warning message for `@SubclassOptInRequired`-annotated class should provide more context
- [`KT-72302`](https://youtrack.jetbrains.com/issue/KT-72302) K2: no error on type operator in annotation parameter default value
- [`KT-58820`](https://youtrack.jetbrains.com/issue/KT-58820) OPT_IN_USAGE_ERROR's message text does not account for SubclassOptInRequired
- [`KT-71662`](https://youtrack.jetbrains.com/issue/KT-71662) PCLA: a type variable is not fixed on demand to a type containing a not-fixed type variable
- [`KT-69739`](https://youtrack.jetbrains.com/issue/KT-69739) K2: "KotlinIllegalArgumentExceptionWithAttachments: Unexpected FirPlaceholderProjectionImpl" caused by unresolved references
- [`KT-72154`](https://youtrack.jetbrains.com/issue/KT-72154) Dokka fails with `not array: KClass<out Annotation>` on Kotlin 2.1.20-dev with `@SubclassOptInRequired`
- [`KT-70756`](https://youtrack.jetbrains.com/issue/KT-70756) K2. Compiler crash with FileAnalysisException on incorrect symbol in nesting lambda
- [`KT-72173`](https://youtrack.jetbrains.com/issue/KT-72173) K2: simple object names from root package are resolved without imports in non-root packages when used as values
- [`KT-71480`](https://youtrack.jetbrains.com/issue/KT-71480) JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS isn't reported while java object isn't created
- [`KT-71034`](https://youtrack.jetbrains.com/issue/KT-71034) Failing compiler/testData/codegen/box/inlineClasses/kt70461.kt
- [`KT-71016`](https://youtrack.jetbrains.com/issue/KT-71016) K/Wasm: Failing compiler/testData/codegen/box/inlineClasses/kt70461.kt
- [`KT-52469`](https://youtrack.jetbrains.com/issue/KT-52469) Deprecate reified type parameter instantiating into intersection types
- [`KT-71753`](https://youtrack.jetbrains.com/issue/KT-71753) PCLA: false-negative operator ambiguity error on fixing a type variable on demand for an operator assignment
- [`KT-59871`](https://youtrack.jetbrains.com/issue/KT-59871) K2: Fix introduced diagnostics
- [`KT-71563`](https://youtrack.jetbrains.com/issue/KT-71563) 'llegalStateException: Source classes should be created separately before referencing' when actualized through typealias and java direct actualization
- [`KT-64741`](https://youtrack.jetbrains.com/issue/KT-64741) Avoid leaking ConeTypeVariable types in diagnostics from PCLA
- [`KT-60447`](https://youtrack.jetbrains.com/issue/KT-60447) Builder inference fails to infer generic type argument from local class
- [`KT-69170`](https://youtrack.jetbrains.com/issue/KT-69170) K2: "Unresolved reference" caused by generics and fun interfaces
- [`KT-71756`](https://youtrack.jetbrains.com/issue/KT-71756) K2 evaluator: broken resolve of private members during debug of Kotlin project itself
- [`KT-68893`](https://youtrack.jetbrains.com/issue/KT-68893) Invalid annotation in contract crashes with K2
- [`KT-71490`](https://youtrack.jetbrains.com/issue/KT-71490) K2: missing REDUNDANT_ELSE_IN_WHEN
- [`KT-64403`](https://youtrack.jetbrains.com/issue/KT-64403) Implement BlackBoxCodegenTestSpecGenerated for K2
- [`KT-71551`](https://youtrack.jetbrains.com/issue/KT-71551) JVM IR K1: NPE on generating a function imported from an object from another module
- [`KT-71210`](https://youtrack.jetbrains.com/issue/KT-71210) K2: false negative FUNCTION_CALL_EXPECTED / NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE with companion objects
- [`KT-71528`](https://youtrack.jetbrains.com/issue/KT-71528) K2/JVM: ClassCastException around Array<Nothing?>
- [`KT-71228`](https://youtrack.jetbrains.com/issue/KT-71228) K2: "IllegalArgumentException: Failed requirement" caused by lambda parameter and class type
- [`KT-71738`](https://youtrack.jetbrains.com/issue/KT-71738) K2: False negative REDECLARATION inside object expression
- [`KT-71701`](https://youtrack.jetbrains.com/issue/KT-71701) K2: false positive CAN_BE_VAL with lateinit and non-in-place lambda
- [`KT-68694`](https://youtrack.jetbrains.com/issue/KT-68694) K2 IDE / Kotlin Debugger: AE “Unresolved reference: <HIDDEN: samples/gen/classes/enum class/EnumClass.lam is invisible” on evaluating private lambda inside enum entry
- [`KT-70970`](https://youtrack.jetbrains.com/issue/KT-70970) K2 IDE / Kotlin Debugger: AE “Only assignable IrValues can be set” on calling overloaded inc() operator on interface
- [`KT-70824`](https://youtrack.jetbrains.com/issue/KT-70824) K2: NoSuchFieldException when evaluating private extension property
- [`KT-70390`](https://youtrack.jetbrains.com/issue/KT-70390) K2 IDE / Kotlin Debugger: can't invoke lambda from private class during evaluation
- [`KT-68701`](https://youtrack.jetbrains.com/issue/KT-68701) K2 IDE / Kotlin Debugger: AE “ERROR_CALL 'Unresolved reference: <HIDDEN: /privateLambda is invisible>#' type=IrErrorType(null)” on evaluating private top-level lambda
- [`KT-68695`](https://youtrack.jetbrains.com/issue/KT-68695) K2 IDE / Kotlin Debugger: AE “Unsupported callable reference” on evaluating ::lateinitStr on private lateinit property
- [`KT-70861`](https://youtrack.jetbrains.com/issue/KT-70861) K2 IDE / Kotlin Debugger: can't evaluate Clazz::class call for private class
- [`KT-34911`](https://youtrack.jetbrains.com/issue/KT-34911) Improve error message for WRONG_ANNOTATION_TARGET: list applicable targets
- [`KT-71601`](https://youtrack.jetbrains.com/issue/KT-71601) K2: When with a subject of type dynamic always considered exhaustive
- [`KT-33091`](https://youtrack.jetbrains.com/issue/KT-33091) Kotlin/Native: Compiler crashes if an external class is declared
- [`KT-59651`](https://youtrack.jetbrains.com/issue/KT-59651) K1/K2: Assertion error on external enum usage attempt
- [`KT-69939`](https://youtrack.jetbrains.com/issue/KT-69939) Extract a category of internal FIR checkers from supplementary FIR checkers
- [`KT-70850`](https://youtrack.jetbrains.com/issue/KT-70850) Pull down typeArguments from ConeKotlinType to ConeClassLikeType
- [`KT-71117`](https://youtrack.jetbrains.com/issue/KT-71117) K2: "IllegalArgumentException: No type for StarProjection" with star projection and function type
- [`KT-71251`](https://youtrack.jetbrains.com/issue/KT-71251) Native & JS K2: Missing check for calling `isInitialized` inside inline fun
- [`KT-70161`](https://youtrack.jetbrains.com/issue/KT-70161) Native: extracting LLVM 16 on Linux makes the compiler print many "Ignoring unknown extended header keyword 'LIBARCHIVE.creationtime'" messages
- [`KT-71215`](https://youtrack.jetbrains.com/issue/KT-71215) K2: UB due to the erroneous greening of the red code with multiple delegation with java
- [`KT-59386`](https://youtrack.jetbrains.com/issue/KT-59386) K2: Missing CONSTANT_EXPECTED_TYPE_MISMATCH
- [`KT-69564`](https://youtrack.jetbrains.com/issue/KT-69564) Make using -Xuse-k2 compiler flag an error
- [`KT-69756`](https://youtrack.jetbrains.com/issue/KT-69756) TypeOfLowering: don't create constant object nodes before inlining
- [`KT-66328`](https://youtrack.jetbrains.com/issue/KT-66328) K2: implement an error for KT-66324
- [`KT-71046`](https://youtrack.jetbrains.com/issue/KT-71046) K/N: a separate lowering to convert function reference to IrConstantObject
- [`KT-69223`](https://youtrack.jetbrains.com/issue/KT-69223) Drop parallel lowering mode in JVM backend
- [`KT-70260`](https://youtrack.jetbrains.com/issue/KT-70260) `@JsPlainObject`: improve compiler error if a method is present
- [`KT-67739`](https://youtrack.jetbrains.com/issue/KT-67739) Improve error message when JDK used in -Xjdk-release has corrupted class files
- [`KT-63964`](https://youtrack.jetbrains.com/issue/KT-63964) K2: different naming of classes defined in script in metadata
- [`KT-70014`](https://youtrack.jetbrains.com/issue/KT-70014) Common inference: introduce rigidTypeMarker
- [`KT-71352`](https://youtrack.jetbrains.com/issue/KT-71352) Cannot load script definition class org.gradle.kotlin.dsl.KotlinProjectScriptTemplate
- [`KT-63502`](https://youtrack.jetbrains.com/issue/KT-63502) Getting java.lang.ClassNotFoundException: javaslang.λ  during  compilation
- [`KT-66316`](https://youtrack.jetbrains.com/issue/KT-66316) Kotlin/Native: make `@Escapes` annotation required for all external functions
- [`KT-69653`](https://youtrack.jetbrains.com/issue/KT-69653) Prohibit exposing types via type parameters' bounds
- [`KT-68451`](https://youtrack.jetbrains.com/issue/KT-68451) Inconsistent rules of CFA in enum initialization block
- [`KT-70893`](https://youtrack.jetbrains.com/issue/KT-70893) K2: Bogus NO_COMPANION_OBJECT on resolve to private qualifier
- [`KT-70965`](https://youtrack.jetbrains.com/issue/KT-70965) FIR/AA: Initializers for Java annotation arguments mapping capture use-site sessions
- [`KT-63945`](https://youtrack.jetbrains.com/issue/KT-63945) K2: Prevent possible diagnostic loss
- [`KT-64453`](https://youtrack.jetbrains.com/issue/KT-64453) K2: Implement ComposeLikeIr...TestGenerated for K2
- [`KT-30424`](https://youtrack.jetbrains.com/issue/KT-30424) Confusing error message "modality is different"
- [`KT-70846`](https://youtrack.jetbrains.com/issue/KT-70846) Replace `ConeKotlinType.nullability` with `isMarkedNullable` on specific types
- [`KT-56720`](https://youtrack.jetbrains.com/issue/KT-56720) K2: false positive MANY_IMPL_MEMBER_NOT_IMPLEMENTED in case of delegation in diamond inheritance
- [`KT-69937`](https://youtrack.jetbrains.com/issue/KT-69937) Define & enforce user-friendly terminology for extended checkers
- [`KT-64406`](https://youtrack.jetbrains.com/issue/KT-64406) K2: Implement CompileKotlinAgainstJavaTestGenerated for K2
- [`KT-69938`](https://youtrack.jetbrains.com/issue/KT-69938) Validate sets of default compiler warnings and supplementary compiler warnings
- [`KT-68971`](https://youtrack.jetbrains.com/issue/KT-68971) Investigate suspicious fragmentation of FIR trees for string literals with interpolation
- [`KT-71073`](https://youtrack.jetbrains.com/issue/KT-71073) Multi-dollar strings: parser grabs too much if backticks follow a short sequence of '$'
- [`KT-71213`](https://youtrack.jetbrains.com/issue/KT-71213) Rethrow exceptions in checkers with some useful attachments
- [`KT-70395`](https://youtrack.jetbrains.com/issue/KT-70395) K2: "Captured Type does not have a classifier" caused by `out` type and interface hierarchy
- [`KT-70133`](https://youtrack.jetbrains.com/issue/KT-70133) K2: false negative UNINITIALIZED_VARIABLE when postponed lambda is created before initialization
- [`KT-70625`](https://youtrack.jetbrains.com/issue/KT-70625) K2: ClassCastException caused by function reference, star projection and invariant type parameter
- [`KT-70835`](https://youtrack.jetbrains.com/issue/KT-70835) K2: "TYPE_MISMATCH" caused by operator assignment
- [`KT-70366`](https://youtrack.jetbrains.com/issue/KT-70366) K2: "KotlinIllegalArgumentExceptionWithAttachments: Failed to find functional supertype for class "
- [`KT-68834`](https://youtrack.jetbrains.com/issue/KT-68834) Parentheses don't influence calls of any convention operators (except invoke operator) after safe navigation operator
- [`KT-70358`](https://youtrack.jetbrains.com/issue/KT-70358) K2: "java.lang.IllegalArgumentException: No type for StarProjection" when using a star projection on a function type
- [`KT-69298`](https://youtrack.jetbrains.com/issue/KT-69298) K2: "Initializer type mismatch" caused by elvis operator type inference for nullable typealias
- [`KT-71189`](https://youtrack.jetbrains.com/issue/KT-71189) K2: emit 'DELEGATE_SPECIAL_FUNCTION_MISSING' and 'DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE' on 'by' keyword
- [`KT-71178`](https://youtrack.jetbrains.com/issue/KT-71178) K2: False negative NO_ELSE_IN_WHEN in when over nullable type with `!is Nothing?` check
- [`KT-70812`](https://youtrack.jetbrains.com/issue/KT-70812) False positive NO_ELSE_IN_WHEN with nullable type argument as subject
- [`KT-70947`](https://youtrack.jetbrains.com/issue/KT-70947) False positive NO_ELSE_IN_WHEN with DNN subject and nullable sealed class upper bound
- [`KT-70752`](https://youtrack.jetbrains.com/issue/KT-70752) Review diagnostics with whole declaration as range
- [`KT-71160`](https://youtrack.jetbrains.com/issue/KT-71160) K2: Rendering of flexible collection types and arrays is too verbose
- [`KT-61227`](https://youtrack.jetbrains.com/issue/KT-61227) Definitely non-nullable types cause "Any was expected" for `@Nullable` parameter
- [`KT-69389`](https://youtrack.jetbrains.com/issue/KT-69389) K2: NONE_APPLICABLE instead of more useful "type mismatch" error with overloads and parameter nullability mismatch
- [`KT-69829`](https://youtrack.jetbrains.com/issue/KT-69829) Missed UNRESOLVED_LABEL for label in returns and loops
- [`KT-61223`](https://youtrack.jetbrains.com/issue/KT-61223) JDK 21: new addFirst/addLast and putFirst/putLast methods allow adding nullable value for non-null types
- [`KT-66742`](https://youtrack.jetbrains.com/issue/KT-66742) Supertypes with inaccessible type arguments are allowed
- [`KT-62906`](https://youtrack.jetbrains.com/issue/KT-62906) Type system: consider changing simple type & DNN type relation
- [`KT-70104`](https://youtrack.jetbrains.com/issue/KT-70104) Update the error message for calling super Java interface methods case
- [`KT-69794`](https://youtrack.jetbrains.com/issue/KT-69794) K2: Wrong target is reported for EXPOSED_SUPER_INTERFACE diagnostic
- [`KT-70724`](https://youtrack.jetbrains.com/issue/KT-70724) False-positive UNINITIALIZED_VARIABLE for inline constructor with late-initialized variables
- [`KT-70749`](https://youtrack.jetbrains.com/issue/KT-70749) False-positive UNINITIALIZED_VARIABLE for inline fun with crossinline modifier
- [`KT-65805`](https://youtrack.jetbrains.com/issue/KT-65805) Migrate builtins serializer to K2
- [`KT-71004`](https://youtrack.jetbrains.com/issue/KT-71004) FirSignatureEnhancement#enhance mutates attributes on the original function
- [`KT-70813`](https://youtrack.jetbrains.com/issue/KT-70813) Questionable behavior for calls on ILT receivers
- [`KT-70208`](https://youtrack.jetbrains.com/issue/KT-70208) 'when' is not exhaustive for expect Boolean
- [`KT-69210`](https://youtrack.jetbrains.com/issue/KT-69210) Native: tune LLVM optimization pipeline
- [`KT-70753`](https://youtrack.jetbrains.com/issue/KT-70753) K2: Missing non-null assertion on the return value of try-catch block
- [`KT-70012`](https://youtrack.jetbrains.com/issue/KT-70012) EXTENSION_SHADOWED_BY_MEMBER shouldn't be reported for actual declarations
- [`KT-70837`](https://youtrack.jetbrains.com/issue/KT-70837) K2. "Expected FirResolvedTypeRef with ConeKotlinType but was FirImplicitTypeRefImplWithoutSource" on incorrect call with extension fun
- [`KT-66751`](https://youtrack.jetbrains.com/issue/KT-66751) Implement a general deprecation of types with inaccessible type arguments
- [`KT-68748`](https://youtrack.jetbrains.com/issue/KT-68748) K2: Remove `irFactory` from `Fir2IrComponents`
- [`KT-61659`](https://youtrack.jetbrains.com/issue/KT-61659) K2: Implement the `EXTENSION_SHADOWED_BY_MEMBER` warning
- [`KT-70709`](https://youtrack.jetbrains.com/issue/KT-70709) Range for MUST_BE_INITIALIZED shouldn't include property annotations
- [`KT-63294`](https://youtrack.jetbrains.com/issue/KT-63294) Do not use duplicated compiler argument names across the codebase
- [`KT-70673`](https://youtrack.jetbrains.com/issue/KT-70673) False positive NO_ELSE_IN_WHEN with nullable Boolean as subject
- [`KT-70672`](https://youtrack.jetbrains.com/issue/KT-70672) False positive NO_ELSE_IN_WHEN with nullable Enum as subject
- [`KT-69207`](https://youtrack.jetbrains.com/issue/KT-69207) Native: use lld when the compiler produces binaries for a Linux target
- [`KT-67696`](https://youtrack.jetbrains.com/issue/KT-67696) Native: compiler crashes when loading an LLVM bitcode file of unsupported version
- [`KT-69767`](https://youtrack.jetbrains.com/issue/KT-69767) K2: Investigate differences in tests without alias behavior for cyclic expansion
- [`KT-70617`](https://youtrack.jetbrains.com/issue/KT-70617) K2: ClassCastException caused by Java enum with overridden `name` property
- [`KT-68796`](https://youtrack.jetbrains.com/issue/KT-68796) Non-first invoke operator calls break chained calls of convention operators after safe navigation operator
- [`KT-67772`](https://youtrack.jetbrains.com/issue/KT-67772) K2: Metadata misses NoInfer annotation for unsafeCast result
- [`KT-70304`](https://youtrack.jetbrains.com/issue/KT-70304) [FIR2IR] Missing `@NoInfer`
- [`KT-65085`](https://youtrack.jetbrains.com/issue/KT-65085) K2: Get rid of special check for unresolved array literals on argument mapping phase
- [`KT-65066`](https://youtrack.jetbrains.com/issue/KT-65066) K1 crashes, K2 doesn't report type mismatch on array literal inside nested annotation call
- [`KT-49235`](https://youtrack.jetbrains.com/issue/KT-49235) Kotlin interface limited to 1000 super types
- [`KT-69991`](https://youtrack.jetbrains.com/issue/KT-69991) K2/JVM: Backend crash with functional types and KFunctions
- [`KT-7461`](https://youtrack.jetbrains.com/issue/KT-7461) Forbid using projection modifiers inside top-level Array in annotation's value parameter
- [`KT-52315`](https://youtrack.jetbrains.com/issue/KT-52315) Legacy keywords (header, impl) break enum definitions
- [`KT-69499`](https://youtrack.jetbrains.com/issue/KT-69499) Native: aggressive inline of runtime procedures causes compiler crash in debug builds
- [`KT-69737`](https://youtrack.jetbrains.com/issue/KT-69737) Native: incompatible target-cpu attributes between runtime and Kotlin code
- [`KT-69911`](https://youtrack.jetbrains.com/issue/KT-69911) Unexpected line numbers in default setter
- [`KT-61529`](https://youtrack.jetbrains.com/issue/KT-61529) K2: Unexpected FirClassLikeSymbol null with -no-jdk
- [`KT-69475`](https://youtrack.jetbrains.com/issue/KT-69475) K2: No "Name contains illegal characters" for package name with dots inside
- [`KT-69484`](https://youtrack.jetbrains.com/issue/KT-69484) Native: remove default values for `isObjectType`
- [`KT-70352`](https://youtrack.jetbrains.com/issue/KT-70352) K2: False-negative CONFLICTING_UPPER_BOUNDS on `Nothing` bound
- [`KT-59781`](https://youtrack.jetbrains.com/issue/KT-59781) K2: investigate implicit cast generation in fir2ir vs psi2ir
- [`KT-70036`](https://youtrack.jetbrains.com/issue/KT-70036) [FIR2IR] Fix param name in overridden setter
- [`KT-68718`](https://youtrack.jetbrains.com/issue/KT-68718) [JVM] Generic function is instantiated with wrong type argument
- [`KT-67983`](https://youtrack.jetbrains.com/issue/KT-67983) K2: False negative "Recursive type alias in expansion" at recursive typealiases
- [`KT-70328`](https://youtrack.jetbrains.com/issue/KT-70328) K2: `@UnsafeVariance` stored in the metadata despite the Source retention
- [`KT-70313`](https://youtrack.jetbrains.com/issue/KT-70313) K2: Don't add `Any` supertype to `kotlin.Nothing` compiled from sources
- [`KT-69982`](https://youtrack.jetbrains.com/issue/KT-69982) K2: New errors when executing `:kotlin-stdlib:jvmJar`
- [`KT-70169`](https://youtrack.jetbrains.com/issue/KT-70169) K2: implement a deprecation error for Synchronized, Throws, JvmField on annotation parameters
- [`KT-67651`](https://youtrack.jetbrains.com/issue/KT-67651) K2: inconsistency in behavior for SAM constructor with flexible type
- [`KT-63857`](https://youtrack.jetbrains.com/issue/KT-63857) K2: Extra `operator` modifier in metadata
- [`KT-70182`](https://youtrack.jetbrains.com/issue/KT-70182) K2: Set up `isOperator` flag according to operator naming conventions during building synthetic overrides for Java methods
- [`KT-20798`](https://youtrack.jetbrains.com/issue/KT-20798) Implement a deprecation warning for reified modifier on type parameters of type alias
- [`KT-68697`](https://youtrack.jetbrains.com/issue/KT-68697) K2 IDE / Kotlin Debugger: NSEE “List is empty.” when method reference is used in some place in code
- [`KT-70157`](https://youtrack.jetbrains.com/issue/KT-70157) K2: false positive JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS for a Java private class
- [`KT-68702`](https://youtrack.jetbrains.com/issue/KT-68702) K2 IDE: AE “SyntheticAccessorLowering should not attempt to modify other files!” on evaluating of supermethods toString() and hashCode()
- [`KT-69509`](https://youtrack.jetbrains.com/issue/KT-69509) K2 IDE / Kotlin Debugger: exception in lowering ReplaceKFunctionInvokeWithFunctionInvoke when compiling code fragment
- [`KT-66323`](https://youtrack.jetbrains.com/issue/KT-66323) K2: Clarify contracts of `ConeSubstitutorByMap`
- [`KT-69652`](https://youtrack.jetbrains.com/issue/KT-69652) K2: False positive "Redundant visibility modifier" with explicitApi()
- [`KT-65815`](https://youtrack.jetbrains.com/issue/KT-65815) K2: False-positive NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY with inline function returning Nothing?
- [`KT-60508`](https://youtrack.jetbrains.com/issue/KT-60508) K2/stdlib: compilation of common code fails if built-in types are provided as platform sources
- [`KT-70037`](https://youtrack.jetbrains.com/issue/KT-70037) K2: Generate IR body for `Any` constructor despite that fact it's empty
- [`KT-69870`](https://youtrack.jetbrains.com/issue/KT-69870) K2: False positive NO_VALUE_FOR_PARAMETER for override without default but base with default and with enabled KMP
- [`KT-69599`](https://youtrack.jetbrains.com/issue/KT-69599) K2: Investiage and fix lots of `UNRESOLVED_REFERENCE` during building stdlib native with K2
- [`KT-68375`](https://youtrack.jetbrains.com/issue/KT-68375) K2: FirPrimaryConstructorSuperTypeChecker fails on generated superclasses
- [`KT-58309`](https://youtrack.jetbrains.com/issue/KT-58309) Deal with test failures inside FirTypeEnhancementTestGenerated
- [`KT-27112`](https://youtrack.jetbrains.com/issue/KT-27112) Implement prohibition of exposing types via type parameters' bounds
- [`KT-69831`](https://youtrack.jetbrains.com/issue/KT-69831) Add long FastJarFS tests to the `nightlyFirCompilerTest` configuration
- [`KT-69537`](https://youtrack.jetbrains.com/issue/KT-69537) K2: Unintentional behavior caused by InferMoreImplicationsFromBooleanExpressions
- [`KT-59814`](https://youtrack.jetbrains.com/issue/KT-59814) K2: Explore why `FirDataFlowAnalyzer` strips away value parameters of non top-level-functions
- [`KT-69069`](https://youtrack.jetbrains.com/issue/KT-69069) K2: expect overloads are deprioritized in common code
- [`KT-69511`](https://youtrack.jetbrains.com/issue/KT-69511) KJS / K2: False positive IMPLICIT_BOXING_IN_IDENTITY_EQUALS when comparing dynamic with primitive
- [`KT-69500`](https://youtrack.jetbrains.com/issue/KT-69500) Native: introduce an option to inline less "ALWAYS_INLINE" runtime procedures
- [`KT-69717`](https://youtrack.jetbrains.com/issue/KT-69717) K2: Don't call `coneType`/`coneTypeOrNull` extensions on `FirResolvedTypeRef`
- [`KT-60440`](https://youtrack.jetbrains.com/issue/KT-60440) K2/Java: investigate constructor own type parameters enhancement
- [`KT-69871`](https://youtrack.jetbrains.com/issue/KT-69871) K2 allows modifier keywords on `package` declaration
- [`KT-61271`](https://youtrack.jetbrains.com/issue/KT-61271) Frontend: "The label does not denote a loop." error message is used even if the label does denote a loop
- [`KT-69768`](https://youtrack.jetbrains.com/issue/KT-69768) K2: Investigate differences in tests without alias behavior with typealias to enum entry
- [`KT-63846`](https://youtrack.jetbrains.com/issue/KT-63846) K2: incorrect type argument inferred for smart cast value of a generic type
- [`KT-69774`](https://youtrack.jetbrains.com/issue/KT-69774) Don't report overload resolution ambiguity if extension receiver contains error type
- [`KT-61316`](https://youtrack.jetbrains.com/issue/KT-61316) K2: Consider throwing exception when replaceType is called on special FirExpressions with immutable types
- [`KT-69201`](https://youtrack.jetbrains.com/issue/KT-69201) Discard expect candidate in overload conflict resolver if there is no actual
- [`KT-69557`](https://youtrack.jetbrains.com/issue/KT-69557) K2: Investigate failures with enabled assertion in `ConeResolvedAtom` constructor
- [`KT-69783`](https://youtrack.jetbrains.com/issue/KT-69783) K2: Make FirTypeProjection sealed
- [`KT-68000`](https://youtrack.jetbrains.com/issue/KT-68000) Investigate getting container functions in checkers
- [`KT-69649`](https://youtrack.jetbrains.com/issue/KT-69649) K2: Cleanup various utilities about `toSymbol` conversion
- [`KT-69185`](https://youtrack.jetbrains.com/issue/KT-69185) K2: Prepare a test runner for diagnostic tests with type aliases non-expanded automatically
- [`KT-69390`](https://youtrack.jetbrains.com/issue/KT-69390) UNRESOLVED_REFERENCE on call with lambda argument turns whole call red
- [`KT-68794`](https://youtrack.jetbrains.com/issue/KT-68794) K2 IDE / Kotlin Debugger: ISE “No real overrides for FUN FAKE_OVERRIDE name:privateFun visibility:private modality:FINAL” on calling private function from superclass in debugger
- [`KT-69315`](https://youtrack.jetbrains.com/issue/KT-69315) FirJavaGenericVarianceViolationTypeChecker: StackOverflowError
- [`KT-49962`](https://youtrack.jetbrains.com/issue/KT-49962) "Visibility inherited is not allowed in forVisibility" when analyzing broken file
- [`KT-24212`](https://youtrack.jetbrains.com/issue/KT-24212) Report "This class shouldn't be used in Kotlin" on calling constructor of Java class with Kotlin analog
- [`KT-64195`](https://youtrack.jetbrains.com/issue/KT-64195) K2: Consider make `FirAnonymousInitializer. containingDeclarationSymbol` not null
- [`KT-64254`](https://youtrack.jetbrains.com/issue/KT-64254) "Projections are not allowed on type arguments of functions and properties": Type-project type arguments of properties
- [`KT-40533`](https://youtrack.jetbrains.com/issue/KT-40533) Error message PROPERTY_WITH_NO_TYPE_NO_INITIALIZER for interface property is not fully correct
- [`KT-20014`](https://youtrack.jetbrains.com/issue/KT-20014) Improve diagnostics for lateinit property without initializer and type annotation
- [`KT-51366`](https://youtrack.jetbrains.com/issue/KT-51366) False positive error "Value class cannot extend classes" when extending generic interface with wrong number of type arguments
- [`KT-68277`](https://youtrack.jetbrains.com/issue/KT-68277) K2: false positive UNREACHABLE_CODE for non-local `return`/`break`
- [`KT-69544`](https://youtrack.jetbrains.com/issue/KT-69544) K2: Mapped Java `@Target` annotation's vararg argument has swapped type and elementType
- [`KT-68998`](https://youtrack.jetbrains.com/issue/KT-68998) K2: Refactor postponed atoms
- [`KT-69288`](https://youtrack.jetbrains.com/issue/KT-69288) Native: Apple LLVM 16 fork can't read bitcode with memory attribute produced by upstream LLVM 16
- [`KT-67808`](https://youtrack.jetbrains.com/issue/KT-67808) K2: Inconsistent properties initialization analysis in init blocks in presence of smartcast on this
- [`KT-69035`](https://youtrack.jetbrains.com/issue/KT-69035) K2: Investigate potential removal of FirMangler
- [`KT-69473`](https://youtrack.jetbrains.com/issue/KT-69473) Missing suspend-conversion for lambda in the last statement of when with expected type
- [`KT-64640`](https://youtrack.jetbrains.com/issue/KT-64640) Prevent mutating SequenceCollection methods from JDK 21 be available on read-only collections
- [`KT-65441`](https://youtrack.jetbrains.com/issue/KT-65441) K1: Remove JDK 21 getFirst()/getLast() in (Mutable)List interfaces
- [`KT-54792`](https://youtrack.jetbrains.com/issue/KT-54792) Store program order of properties inside `@kotlin`.Metadata
- [`KT-59832`](https://youtrack.jetbrains.com/issue/KT-59832) K2: Fix the TODO about merging values for labels in UnusedChecker

### Compose compiler

#### New features

- [`b/328817808`](https://issuetracker.google.com/issues/328817808) Added the PausableComposition feature flags
- [`83c48a0`](https://github.com/JetBrains/kotlin/commit/83c48a0dc54c8b48bd46805c84c5df215558134d) Decoy support for JS target is removed from Compose compiler

#### Fixes
- [`CMP-6926`](https://youtrack.jetbrains.com/issue/CMP-6926) iOS compilation failure: Unresolved reference 'copy'
- [`CMP-6842`](https://youtrack.jetbrains.com/issue/CMP-6842) FAKE_OVERRIDE declarations are not preserved in metadata and should not be marked with annotations
- [`CMP-6788`](https://youtrack.jetbrains.com/issue/CMP-6788) non-private field compilation warnings (stableprop & ComposableSingletons)
- [`CMP-6685`](https://youtrack.jetbrains.com/issue/CMP-6685) Native/WASM compilation failure on Composable function with value-type arg + return
- [`b/376058538`](https://issuetracker.google.com/issues/376058538) Fix stack overflow when inferring stability of indirect generic loop
- [`b/339322843`](https://issuetracker.google.com/issues/339322843) Transform @Composable property delegate references
- [`b/366040842`](https://issuetracker.google.com/issues/366040842), [`b/365066530`](https://issuetracker.google.com/issues/365066530) Replace deep copy in Compose plugin with in-place type mutation
- [`b/329477544`](https://issuetracker.google.com/issues/329477544) Force open / overridden Composable functions to be non-restartable.
- [`b/361652128`](https://issuetracker.google.com/issues/361652128) Disable live literal transform if the corresponding flag is disabled
- [`b/325004814`](https://issuetracker.google.com/issues/325004814) [Compose] Fix infinite recursion in target analysis
- [`b/357878245`](https://issuetracker.google.com/issues/357878245) Disallow open @Composable functions with default params to fix binary compatibility issues.
- [`b/338597078`](https://issuetracker.google.com/issues/338597078) [Compose] Fix target warning message
- [`b/351858979`](https://issuetracker.google.com/issues/351858979) Fix stability inferencing of interfaces on incremental compilation
- [`b/346821372`](https://issuetracker.google.com/issues/346821372) [Compose] Fix code generation for group optimization
- [`b/339311821`](https://issuetracker.google.com/issues/339311821) Give warning when stability configuration file is not found
- [`b/346821372`](https://issuetracker.google.com/issues/346821372) Fixes group generation for if statements when nonSkippingGroupOptimization is enabled.

### IDE. Gradle Integration

- [`KT-48554`](https://youtrack.jetbrains.com/issue/KT-48554) [Multiplatform Import] Ensure consistency between `GradleImportProperties` and `PropertiesProvider`

### IR. Actualizer

- [`KT-71631`](https://youtrack.jetbrains.com/issue/KT-71631) Kotlin-to-Java direct actualization: java annotation element isn't actualized
- [`KT-71597`](https://youtrack.jetbrains.com/issue/KT-71597) Kotlin-to-Java direct actualization: it is possible to actualize a function with default parameters
- [`KT-71592`](https://youtrack.jetbrains.com/issue/KT-71592) Kotlin-to-Java direct actualization: constructor of nested class can't be actualized
- [`KT-71577`](https://youtrack.jetbrains.com/issue/KT-71577) Kotlin-to-Java direct actualization: method can be actualized by java static method
- [`KT-69632`](https://youtrack.jetbrains.com/issue/KT-69632) K2: Expect actual mismatch on actualization with alias to expect class
- [`KT-71817`](https://youtrack.jetbrains.com/issue/KT-71817) Actualization of static members is broken for non-JVM platforms

### IR. Inlining

#### New Features

- [`KT-69527`](https://youtrack.jetbrains.com/issue/KT-69527) Set the right visibility for synthetic accessors in SyntheticAccessorLowering

#### Fixes

- [`KT-71232`](https://youtrack.jetbrains.com/issue/KT-71232) Implement an IR validation check that ensures that all IrFields are private on non-JVM backends
- [`KT-69307`](https://youtrack.jetbrains.com/issue/KT-69307) Source offsets seem incorrect after IR inlining
- [`KT-72884`](https://youtrack.jetbrains.com/issue/KT-72884) Internal error in body lowering: IllegalStateException: Can't inline given reference, it should've been lowered
- [`KT-71659`](https://youtrack.jetbrains.com/issue/KT-71659) IR Inliner fails to inline function expressions due to implicit cast from the 1st phase of inlining
- [`KT-69681`](https://youtrack.jetbrains.com/issue/KT-69681) IR: Report warnings on exposure of private types in non-private inline functions
- [`KT-72521`](https://youtrack.jetbrains.com/issue/KT-72521) Kotlin/Native: java.lang.AssertionError: kfun:androidx.compose.runtime#access$<get-androidx_compose_runtime_ProvidedValue$stable>$p$tComposerKt(){}kotlin.Int
- [`KT-72623`](https://youtrack.jetbrains.com/issue/KT-72623) Don't generate synthetic accessors in files other than the one being lowered
- [`KT-70420`](https://youtrack.jetbrains.com/issue/KT-70420) Enable double-inlining in Native & JS backends by default
- [`KT-67292`](https://youtrack.jetbrains.com/issue/KT-67292) Handling assertions before the IR inliner
- [`KT-70423`](https://youtrack.jetbrains.com/issue/KT-70423) KLIB: SyntheticAccessorLowering - generate static factory functions instead of synthetic constructors
- [`KT-69565`](https://youtrack.jetbrains.com/issue/KT-69565) Don't generate synthetic accessors for private symbols inside local classes
- [`KT-69787`](https://youtrack.jetbrains.com/issue/KT-69787) Handle clashes of synthetic accessors generated for top-level callables
- [`KT-71137`](https://youtrack.jetbrains.com/issue/KT-71137) Generate synthetic accessors for backing fields
- [`KT-67172`](https://youtrack.jetbrains.com/issue/KT-67172) Native & JS: Introduce OuterThisInInlineFunctionsSpecialAccessorLowering
- [`KT-64865`](https://youtrack.jetbrains.com/issue/KT-64865) Explicitly generate accessors for private declarations in inline functions
- [`KT-71657`](https://youtrack.jetbrains.com/issue/KT-71657) K/JS: Double-inlining causes failures in IC with top-level synthetic accessors
- [`KT-71078`](https://youtrack.jetbrains.com/issue/KT-71078) Inline all functions in local classes at the 1st stage of inlining
- [`KT-69802`](https://youtrack.jetbrains.com/issue/KT-69802) Don't extract local classes from inline functions in double inlining mode
- [`KT-66508`](https://youtrack.jetbrains.com/issue/KT-66508) IR inliner: Add implicit cast for initializer of temporary variables
- [`KT-66507`](https://youtrack.jetbrains.com/issue/KT-66507) IR inliner: Enable implicit casts in all KLib backends
- [`KT-69466`](https://youtrack.jetbrains.com/issue/KT-69466) IrInlinedFunctionBlock: Refactor it to make it possible to serialize in KLIBs
- [`KT-69317`](https://youtrack.jetbrains.com/issue/KT-69317) IR Inlining. Try to place inlined arguments outside `IrInlinedFunctionBlock`
- [`KT-67149`](https://youtrack.jetbrains.com/issue/KT-67149) Common Native/JS lowering prefix at the 2nd phase of compilation
- [`KT-69172`](https://youtrack.jetbrains.com/issue/KT-69172) Implement double-inlining for Native
- [`KT-67304`](https://youtrack.jetbrains.com/issue/KT-67304) Keep in common prefix: Shared variables + local classes in inline lambdas
- [`KT-67170`](https://youtrack.jetbrains.com/issue/KT-67170) ArrayConstructorReferenceLowering is missing in Native
- [`KT-70583`](https://youtrack.jetbrains.com/issue/KT-70583) Internal error in body lowering: java.lang.IllegalStateException: An attempt to generate an accessor after all accessors have been already added to their containers
- [`KT-69700`](https://youtrack.jetbrains.com/issue/KT-69700) Inline `stub_for_inlining` use sites survive after the inliner
- [`KT-69462`](https://youtrack.jetbrains.com/issue/KT-69462) Support dumping IR after inlining in compiler tests
- [`KT-70693`](https://youtrack.jetbrains.com/issue/KT-70693) IR: replace IrReturnableBlock.inlineFucntion with IrInlinedFunctionBlock.inlineFucntion
- [`KT-70763`](https://youtrack.jetbrains.com/issue/KT-70763) IR inline: consider storing stub_for_inline as an inlined function for callable reference
- [`KT-69168`](https://youtrack.jetbrains.com/issue/KT-69168) Wrap assertion calls before IR inliner
- [`KT-69167`](https://youtrack.jetbrains.com/issue/KT-69167) Create intrinsics in stdlib for handling assertions in KLIB-based backends
- [`KT-69169`](https://youtrack.jetbrains.com/issue/KT-69169) Expand assertion intrinsics in backend based on CLI parameters
- [`KT-69174`](https://youtrack.jetbrains.com/issue/KT-69174) Implement the basic Synthetic Accessors Lowering for KLIB-based backends

### IR. Interpreter

- [`KT-70388`](https://youtrack.jetbrains.com/issue/KT-70388) K2 IDE / Kotlin Debugger: InterpreterError “Unsupported number of arguments for invocation as builtin function: INT_MAX_POWER_OF_TWO” during evaluation

### IR. Tree

#### Fixes

- [`KT-69644`](https://youtrack.jetbrains.com/issue/KT-69644) Report warning on cross-file IrGetField operations generated by compiler plugins
- [`KT-68789`](https://youtrack.jetbrains.com/issue/KT-68789) Prepare tests for testing visibility (non-)violation in inlined IR
- [`KT-71826`](https://youtrack.jetbrains.com/issue/KT-71826) stdlib fails to compile with `-Xserialize-ir=all`
- [`KT-70333`](https://youtrack.jetbrains.com/issue/KT-70333) IR: remove ability to apply compiler plugins during KAPT stub generation phase
- [`KT-67752`](https://youtrack.jetbrains.com/issue/KT-67752) Make copyRemappedTypeArgumentsFrom and transformValueArguments methods in DeepCopyIrTreeWithSymbols protected instead of private
- [`KT-68151`](https://youtrack.jetbrains.com/issue/KT-68151) Setup testing visibility of referenced declarations in IR
- [`KT-68988`](https://youtrack.jetbrains.com/issue/KT-68988) [Tests] Streamline the order of irFiles in IR- and Kotlin-like dumps
- [`KT-65773`](https://youtrack.jetbrains.com/issue/KT-65773) Auto generate IR implementation classes
- [`KT-70330`](https://youtrack.jetbrains.com/issue/KT-70330) Automatically keep track of IrValueParameter.index
- [`KT-68495`](https://youtrack.jetbrains.com/issue/KT-68495) Compile-time failure on bounded generic value used in a contains-check with range
- [`KT-68974`](https://youtrack.jetbrains.com/issue/KT-68974) Validate scopes of IrValueParameters in IrValidator

### JavaScript

#### New Features

- [`KT-70254`](https://youtrack.jetbrains.com/issue/KT-70254) K/JS: Generate arrows in ES6 mode instead of anonymous functions
- [`KT-70283`](https://youtrack.jetbrains.com/issue/KT-70283) KJS / ES6: Don't generate bind(this) calls for anonymous functions that capture `this`

#### Fixes

- [`KT-43567`](https://youtrack.jetbrains.com/issue/KT-43567) KJS: toString() method and string interpolation of variable produce different code
- [`KT-70533`](https://youtrack.jetbrains.com/issue/KT-70533) KJS: changed string concatenation behavior in 2.0
- [`KT-14013`](https://youtrack.jetbrains.com/issue/KT-14013) JS toString produces different result for nullable/non-nullable ref to the same array
- [`KT-72732`](https://youtrack.jetbrains.com/issue/KT-72732) KJS / ES6: "SyntaxError: 'super' keyword unexpected here" with enabled `-Xir-generate-inline-anonymous-functions` and disabled arrow functions
- [`KT-69408`](https://youtrack.jetbrains.com/issue/KT-69408) [JS] Enable insertAdditionalImplicitCasts=true (as in other KLIB-based backends)
- [`KT-71821`](https://youtrack.jetbrains.com/issue/KT-71821) K/JS tests are failing with coroutines flow and turbine on timeout
- [`KT-31799`](https://youtrack.jetbrains.com/issue/KT-31799) Allow non-identifier characters in Kotlin/JS (backquoted properties, `@JsName`)
- [`KT-55869`](https://youtrack.jetbrains.com/issue/KT-55869) Coroutine is not intercepted, when the coroutine is started calling `startCoroutineUninterceptedOrReturn` using callable reference
- [`KT-70117`](https://youtrack.jetbrains.com/issue/KT-70117) Generate debug info for code from `js` call
- [`KT-69642`](https://youtrack.jetbrains.com/issue/KT-69642) ES generator-based coroutines rely on eval
- [`KT-67452`](https://youtrack.jetbrains.com/issue/KT-67452) K2: Consider hiding dynamic type creation under FlexibleTypeFactory for JS only
- [`KT-70226`](https://youtrack.jetbrains.com/issue/KT-70226) Delete JS tests that were only run with the legacy JS backend
- [`KT-71338`](https://youtrack.jetbrains.com/issue/KT-71338) K/JS: Add a flag for switching generating arrow functions on & off
- [`KT-69173`](https://youtrack.jetbrains.com/issue/KT-69173) Implement double-inlining for JS
- [`KT-67327`](https://youtrack.jetbrains.com/issue/KT-67327) JS: Remove error tolerance
- [`KT-69892`](https://youtrack.jetbrains.com/issue/KT-69892) Array.isArray() returns false for an instance returned by KtList.asReadonlyArrayView()
- [`KT-70231`](https://youtrack.jetbrains.com/issue/KT-70231) Delete the org.jetbrains.kotlin.cli.js.dce.K2JSDce class
- [`KT-69928`](https://youtrack.jetbrains.com/issue/KT-69928) KJS: keys() and values() of KtMap's JS view don't behave as expected
- [`KT-70707`](https://youtrack.jetbrains.com/issue/KT-70707) KJS: asJsReadonlyMapView does not implement ReadonlyMap correctly
- [`KT-71220`](https://youtrack.jetbrains.com/issue/KT-71220) Fix invalid IrFunctionReference creation in InnerClassConstructorCallsLowering
- [`KT-70393`](https://youtrack.jetbrains.com/issue/KT-70393) Investigate failing JS test after switch stdlib compilation to K2
- [`KT-64429`](https://youtrack.jetbrains.com/issue/KT-64429) K2: Implement KlibJsIrTextTestCaseGenerated for K2
- [`KT-69587`](https://youtrack.jetbrains.com/issue/KT-69587) [Tests] Fix multi-module deserialization in JS irText tests
- [`KT-70219`](https://youtrack.jetbrains.com/issue/KT-70219) Delete the org.jetbrains.kotlin.cli.js.K2JSCompiler class
- [`KT-70221`](https://youtrack.jetbrains.com/issue/KT-70221) Rename org.jetbrains.kotlin.cli.js.K2JsIrCompiler to K2JSCompiler
- [`KT-70229`](https://youtrack.jetbrains.com/issue/KT-70229) Remove test classes related to the legacy JS backend
- [`KT-70359`](https://youtrack.jetbrains.com/issue/KT-70359) Remove legacy backend-related test directives from Kotlin/JS tests
- [`KT-70362`](https://youtrack.jetbrains.com/issue/KT-70362) Clean up Gradle tasks for running JS tests against the legacy JS backend
- [`KT-66181`](https://youtrack.jetbrains.com/issue/KT-66181) Reorganize JsCodeOutliningLowering and keep it before the IR inliner
- [`KT-30016`](https://youtrack.jetbrains.com/issue/KT-30016) JS BE does not generate special bridge methods
- [`KT-68975`](https://youtrack.jetbrains.com/issue/KT-68975) KJS: Investigate calling `js(...)` from inline functions

### KMM Plugin

- [`KT-71011`](https://youtrack.jetbrains.com/issue/KT-71011) AS KMP plugin: ios application can't start for 2024.2.1

### Klibs

#### New Features

- [`KT-64169`](https://youtrack.jetbrains.com/issue/KT-64169) [KLIB Resolve] Don't skip libraries that happen to have the same `unique_name`
- [`KT-68322`](https://youtrack.jetbrains.com/issue/KT-68322) Compiler (JS, Wasm): warn about incompatible Kotlin stdlib/compiler pair

#### Fixes

- [`KT-61098`](https://youtrack.jetbrains.com/issue/KT-61098) [KLIB Resolve] Don't allow working with KLIB "repositories"
- [`KT-72965`](https://youtrack.jetbrains.com/issue/KT-72965) Ignore subclassOptInRequired constructor warning
- [`KT-68792`](https://youtrack.jetbrains.com/issue/KT-68792) Bump KLIB ABI version in 2.1
- [`KT-67474`](https://youtrack.jetbrains.com/issue/KT-67474) K2: Missing `@ExtensionFunctionType` in metadata in KLIBs
- [`KT-71633`](https://youtrack.jetbrains.com/issue/KT-71633) [2.1.0] Suspicious "Argument type mismatch" error
- [`KT-70146`](https://youtrack.jetbrains.com/issue/KT-70146) [KLIB Resolve] Don't fail on nonexistent transitive dependency
- [`KT-71455`](https://youtrack.jetbrains.com/issue/KT-71455) [KLIB Resolve] Forbid passing KLIB unique names via CLI
- [`KT-67448`](https://youtrack.jetbrains.com/issue/KT-67448) [KLIB Resolve] Deprecate passing KLIB unique names via CLI
- [`KT-67450`](https://youtrack.jetbrains.com/issue/KT-67450) [KLIB Resolve] Kotlin/Native: Only one implicit repository should remain for the compiler ("dist")
- [`KT-70285`](https://youtrack.jetbrains.com/issue/KT-70285) Warning about incompatible stdlib (JS/Wasm) is not reported if stdlib is unpacked
- [`KT-66218`](https://youtrack.jetbrains.com/issue/KT-66218) Clean-up the code for serialization & deserialization of DFGs to & from KLIBs
- [`KT-71414`](https://youtrack.jetbrains.com/issue/KT-71414) KotlinLibraryResolver.resolveWithDependencies was evolved in binary incompatible way
- [`KT-68195`](https://youtrack.jetbrains.com/issue/KT-68195) move KlibMetadataProtoBuf to frondend-independent module

### Language Design

- [`KT-54617`](https://youtrack.jetbrains.com/issue/KT-54617) Stabilize `@SubclassOptInRequired`: ability to require opt-in for interface implementation
- [`KT-54458`](https://youtrack.jetbrains.com/issue/KT-54458) Preview of non-local break and continue
- [`KT-69924`](https://youtrack.jetbrains.com/issue/KT-69924) Mention 'if' guard when '&&' is used incorrectly
- [`KT-71222`](https://youtrack.jetbrains.com/issue/KT-71222) Remove `@ExperimentalSubclassOptIn` from SubclassOptInRequired
- [`KT-67675`](https://youtrack.jetbrains.com/issue/KT-67675) Allow usage of Array<Nothing?>
- [`KT-70754`](https://youtrack.jetbrains.com/issue/KT-70754) Changes in typeOf behaviour for Kotlin/Native
- [`KT-58659`](https://youtrack.jetbrains.com/issue/KT-58659) Prohibit implementing a var property with an inherited val property

### Libraries

#### Performance Improvements

- [`KT-66715`](https://youtrack.jetbrains.com/issue/KT-66715) Performance: faster alternative to String.lines()

#### Fixes

- [`KT-71628`](https://youtrack.jetbrains.com/issue/KT-71628) Review deprecations in stdlib for 2.1
- [`KT-69545`](https://youtrack.jetbrains.com/issue/KT-69545) Kotlin/Native: Deprecate API marked with FreezingIsDeprecated to error
- [`KT-56076`](https://youtrack.jetbrains.com/issue/KT-56076) K2: build Kotlin standard library
- [`KT-71660`](https://youtrack.jetbrains.com/issue/KT-71660) Stabilize experimental API for 2.1
- [`KT-54299`](https://youtrack.jetbrains.com/issue/KT-54299) Extract org.w3c declarations to separate library from K/Wasm Stdlib
- [`KT-68027`](https://youtrack.jetbrains.com/issue/KT-68027) Document caveats and deincentivise usage of measureTimeMillis
- [`KT-71581`](https://youtrack.jetbrains.com/issue/KT-71581) Update outdated documentation to common lazy and provide samples
- [`KT-71796`](https://youtrack.jetbrains.com/issue/KT-71796) Improve documentation for Path.walk and Path.visitFileTree functions
- [`KT-68019`](https://youtrack.jetbrains.com/issue/KT-68019) Fill in missing package descriptions for standard library documentation
- [`KT-52181`](https://youtrack.jetbrains.com/issue/KT-52181) Native: Inconsistent behaviour of LinkedHashMap#entries on JVM and Native
- [`KT-71570`](https://youtrack.jetbrains.com/issue/KT-71570) Document suspend lambda builder
- [`KT-65526`](https://youtrack.jetbrains.com/issue/KT-65526) Rewrite builtins as expect-actual
- [`KT-68502`](https://youtrack.jetbrains.com/issue/KT-68502) K2: Fix or suppress stdlib K2 warnings
- [`KT-68731`](https://youtrack.jetbrains.com/issue/KT-68731) K2: Handle some formally incompatible expect/actual classes in JVM stdlib
- [`KT-70378`](https://youtrack.jetbrains.com/issue/KT-70378) Implement custom serialization for Uuid
- [`KT-70005`](https://youtrack.jetbrains.com/issue/KT-70005) K/Wasm and K/Native: IntArray.sort - array element access out of bounds
- [`KT-66764`](https://youtrack.jetbrains.com/issue/KT-66764) kotlinx-benchmark: rework on kotlin-compiler-embeddable
- [`KT-69817`](https://youtrack.jetbrains.com/issue/KT-69817) Set up klib binary API validation for stdlib
- [`KT-68396`](https://youtrack.jetbrains.com/issue/KT-68396) Handle some formally incompatible top-level expects/actuals callables
- [`KT-69524`](https://youtrack.jetbrains.com/issue/KT-69524) kotlin.uuid.Uuid: checkHyphenAt - error message always specified index 8
- [`KT-69327`](https://youtrack.jetbrains.com/issue/KT-69327) [native] FloatingPointParser.initialParse works incorrectly for some inputs
- [`KT-46785`](https://youtrack.jetbrains.com/issue/KT-46785) Get rid of !! after readLine() in the standard library

### Native

- [`KT-71435`](https://youtrack.jetbrains.com/issue/KT-71435) Native: cannot access class 'objcnames.classes.Protocol'
- [`KT-49279`](https://youtrack.jetbrains.com/issue/KT-49279) Kotlin/Native: update LLVM from 11.1.0 to 16.0.0
- [`KT-61299`](https://youtrack.jetbrains.com/issue/KT-61299) Native: patch LLVM to prevent it from using signal handlers incompatibly with JVM
- [`KT-69637`](https://youtrack.jetbrains.com/issue/KT-69637) Native: our LLVM shouldn't advise submitting bugs to the upstream
- [`KT-64636`](https://youtrack.jetbrains.com/issue/KT-64636) kotlin.incremental.native=true causes IrLinkageError
- [`KT-69142`](https://youtrack.jetbrains.com/issue/KT-69142) ObsoleteWorkersApi and FreezingIsDeprecated is not displayed on targets in webdocs

### Native. Build Infrastructure

- [`KT-71820`](https://youtrack.jetbrains.com/issue/KT-71820) Update the coroutines version used in kotlin-native build infrastructure
- [`KT-69479`](https://youtrack.jetbrains.com/issue/KT-69479) Native: remove custom python version building from the LLVM builder container image
- [`KT-63214`](https://youtrack.jetbrains.com/issue/KT-63214) [K/N] llvm build script fails with MacOSX14.0.sdk sysroot

### Native. ObjC Export

- [`KT-62997`](https://youtrack.jetbrains.com/issue/KT-62997) IllegalStateException for hashCode(): KClass for Objective-C classes is not supported yet
- [`KT-59497`](https://youtrack.jetbrains.com/issue/KT-59497) KClass.simpleName returns null in ObjC-inherited class

### Native. Platform Libraries

- [`KT-70032`](https://youtrack.jetbrains.com/issue/KT-70032) Rebuild platform libraries in 2.1.0 with Xcode 16
- [`KT-69448`](https://youtrack.jetbrains.com/issue/KT-69448) LLVM 16 clang with Xcode 16 headers: 'sys/cdefs.h' file not found

### Native. Runtime

- [`KT-70680`](https://youtrack.jetbrains.com/issue/KT-70680) Kotlin/Native: Use WritableTypeInfo when creating Swift wrapper from the runtime
- [`KT-70568`](https://youtrack.jetbrains.com/issue/KT-70568) Native: revert workaround for debug with LLVM 16
- [`KT-67730`](https://youtrack.jetbrains.com/issue/KT-67730) Native: fix runtime compilation warnings after update to LLVM 16

### Native. Runtime. Memory

- [`KT-72624`](https://youtrack.jetbrains.com/issue/KT-72624) Native: testRelease_on_unattached_thread sometimes fails with Releasing StableRef with rc 0
- [`KT-71401`](https://youtrack.jetbrains.com/issue/KT-71401) K/N: CMS barrier can be executed on an unregisterred thread
- [`KT-70364`](https://youtrack.jetbrains.com/issue/KT-70364) Kotlin/Native: data race during GC initialization
- [`KT-68544`](https://youtrack.jetbrains.com/issue/KT-68544) [Native] Implement heap dump tool
- [`KT-70365`](https://youtrack.jetbrains.com/issue/KT-70365) Kotlin/Native: make thread id be pointer size

### Native. Swift Export

#### New Features

- [`KT-71539`](https://youtrack.jetbrains.com/issue/KT-71539) Swift Export: export class member overrides
- [`KT-70442`](https://youtrack.jetbrains.com/issue/KT-70442) Swift Export: export class inheritance
- [`KT-68864`](https://youtrack.jetbrains.com/issue/KT-68864) Refactor internal details of swift-export-standalone

#### Fixes

- [`KT-70678`](https://youtrack.jetbrains.com/issue/KT-70678) Swift Export: generate Kotlin<->Swift type mapping
- [`KT-70920`](https://youtrack.jetbrains.com/issue/KT-70920) Swift Export Nullability: primitive type
- [`KT-71087`](https://youtrack.jetbrains.com/issue/KT-71087) Swift Export: Nullability: Never
- [`KT-71086`](https://youtrack.jetbrains.com/issue/KT-71086) Swift Export: Nullability: Strings
- [`KT-70919`](https://youtrack.jetbrains.com/issue/KT-70919) Swift Export Nullability: reference type
- [`KT-71026`](https://youtrack.jetbrains.com/issue/KT-71026) Swift Export: function overloading with ref types does not work
- [`KT-70960`](https://youtrack.jetbrains.com/issue/KT-70960) Swift Export nullability: add nullability to sir and printer
- [`KT-70063`](https://youtrack.jetbrains.com/issue/KT-70063) Swift export generates invalid Swift code for class and function with the same name
- [`KT-70069`](https://youtrack.jetbrains.com/issue/KT-70069) Swift export: filter out extension properties
- [`KT-70068`](https://youtrack.jetbrains.com/issue/KT-70068) Swift export: nullable types are not marked as unsupported
- [`KT-69287`](https://youtrack.jetbrains.com/issue/KT-69287) Swift Export: support leaking dependencies
- [`KT-69633`](https://youtrack.jetbrains.com/issue/KT-69633) Provide interface for multiple module translation
- [`KT-69286`](https://youtrack.jetbrains.com/issue/KT-69286) [Swift Export][TestInfra] Support translating multiple roots
- [`KT-69376`](https://youtrack.jetbrains.com/issue/KT-69376) Property with Any type does not force addition of import

### Reflection

- [`KT-71378`](https://youtrack.jetbrains.com/issue/KT-71378) KotlinReflectionInternalError: Inconsistent number of parameters in the descriptor and Java reflection object

### Specification

- [`KT-53427`](https://youtrack.jetbrains.com/issue/KT-53427) Specify `@SubclassOptInRequired`

### Tools. CLI

#### New Features

- [`KT-8087`](https://youtrack.jetbrains.com/issue/KT-8087) Make it possible to suppress warnings globally in compiler (via command-line option)
- [`KT-71537`](https://youtrack.jetbrains.com/issue/KT-71537) Add JVM target bytecode version 23

#### Fixes

- [`KT-70991`](https://youtrack.jetbrains.com/issue/KT-70991) K2: Compilation fails if project version has a comma
- [`KT-70179`](https://youtrack.jetbrains.com/issue/KT-70179) K2: Building a file with kotlin-test-junit without junit does not include annotations
- [`KT-72311`](https://youtrack.jetbrains.com/issue/KT-72311) KotlinCliJavaFileManagerImpl caches empty result and broke repeated analyses
- [`KT-61745`](https://youtrack.jetbrains.com/issue/KT-61745) K2: support light tree in multi-module chunk mode
- [`KT-70885`](https://youtrack.jetbrains.com/issue/KT-70885) Errors are not reported for wrong arguments in -Xsuppress-warning flag for non-jvm backends
- [`KT-69541`](https://youtrack.jetbrains.com/issue/KT-69541) K2: "IllegalArgumentException: Unexpected versionNeededToExtract" on using JAR packaged as ZIP64
- [`KT-69434`](https://youtrack.jetbrains.com/issue/KT-69434) K2: Kotlin compiler JarFS can't handle large dependencies (>2GB)
- [`KT-70959`](https://youtrack.jetbrains.com/issue/KT-70959) K2: Support legacy metadata jar format in K2 compiler
- [`KT-70337`](https://youtrack.jetbrains.com/issue/KT-70337) Obsolete code is not removed after refactoring - `JvmEnvironmentConfigurator.registerModuleDependencies`
- [`KT-70322`](https://youtrack.jetbrains.com/issue/KT-70322) Merge CLITool and CLICompiler classes

### Tools. CLI. Native

- [`KT-68673`](https://youtrack.jetbrains.com/issue/KT-68673) Kotlin/Native "You have not specified any compilation arguments. No output has been produced" when no source nor `-Xinclude` is passed

### Tools. Compiler Plugins

#### Fixes

- [`KT-72804`](https://youtrack.jetbrains.com/issue/KT-72804) Regression in Kotlin 2.1.0: compilation fails when building iOS
- [`KT-72824`](https://youtrack.jetbrains.com/issue/KT-72824) Kotlin power-assert plugin StringIndexOutOfBoundsException
- [`KT-71658`](https://youtrack.jetbrains.com/issue/KT-71658) Transform top-level atomic properties to Java boxed atomics
- [`KT-65645`](https://youtrack.jetbrains.com/issue/KT-65645) Atomicfu-plugin: compilation hangs on a long string concatenation
- [`KT-69038`](https://youtrack.jetbrains.com/issue/KT-69038) Power-Assert does not display const vals
- [`KT-71525`](https://youtrack.jetbrains.com/issue/KT-71525) Setting JvmAbiConfigurationKeys.REMOVE_PRIVATE_CLASSES = true triggers java.util.ConcurrentModificationException
- [`KT-41888`](https://youtrack.jetbrains.com/issue/KT-41888) IrExpression startOffset and endOffset are inconsistent with raw file text
- [`KT-69856`](https://youtrack.jetbrains.com/issue/KT-69856) Compose Plugin: IrType.erasedUpperBound throws NullPointerException when evaluating IrScript nodes due to missing targetClass
- [`KT-69410`](https://youtrack.jetbrains.com/issue/KT-69410) PowerAssert: Cannot find overload of requireNotNull without existing message
- [`KT-66293`](https://youtrack.jetbrains.com/issue/KT-66293) Atomicfu-plugin: wrong return types for lowered extension functions
- [`KT-69646`](https://youtrack.jetbrains.com/issue/KT-69646) PowerAssert: result of array access operator is unaligned
- [`KT-70112`](https://youtrack.jetbrains.com/issue/KT-70112) Power Assert: multiline assertion support
- [`KT-70504`](https://youtrack.jetbrains.com/issue/KT-70504) [atomicfu-plugin] Incremental compilation fails for atomic extensions on JVM
- [`KT-70351`](https://youtrack.jetbrains.com/issue/KT-70351) K2 CodeGen API exception triggered by a compose compiler plugin lowering transformer for data class example
- [`KT-70113`](https://youtrack.jetbrains.com/issue/KT-70113) Power Assert: tab support
- [`KT-69806`](https://youtrack.jetbrains.com/issue/KT-69806) K2: SOE on nested plugin-like annotation in class annotated with itself
- [`KT-69538`](https://youtrack.jetbrains.com/issue/KT-69538) jvm-abi-gen: Remove copy$default if data class constructor is private and ConsistentCopyVisibility is used

### Tools. Compiler plugins. Serialization

- [`KT-70110`](https://youtrack.jetbrains.com/issue/KT-70110) Prohibit `@Serializable` on companion object of another `@Serializable` class
- [`KT-69388`](https://youtrack.jetbrains.com/issue/KT-69388) Serialization: "You should use ConeClassLookupTagWithFixedSymbol" caused by `@Serializable` on local generic class

### Tools. Daemon

- [`KT-69929`](https://youtrack.jetbrains.com/issue/KT-69929) compileKotlin task reports that daemon has terminated unexpectedly
- [`KT-72530`](https://youtrack.jetbrains.com/issue/KT-72530) The daemon has terminated unexpectedly on startup attempt #1 with error code: Unknown

### Tools. Fleet. ObjC Export

#### Fixes

- [`KT-71162`](https://youtrack.jetbrains.com/issue/KT-71162) ObjCExport: nullable functional type with reference arguments
- [`KT-71022`](https://youtrack.jetbrains.com/issue/KT-71022) ObjCExport: enum c keywords translation
- [`KT-71082`](https://youtrack.jetbrains.com/issue/KT-71082) ObjCExport: KotlinUnit translated as Function1
- [`KT-70781`](https://youtrack.jetbrains.com/issue/KT-70781) ObjCExport: classifiers and callables type parameters translation
- [`KT-70943`](https://youtrack.jetbrains.com/issue/KT-70943) ObjCExport: extension order
- [`KT-70840`](https://youtrack.jetbrains.com/issue/KT-70840) ObjCExport: duplicated interfaces
- [`KT-70642`](https://youtrack.jetbrains.com/issue/KT-70642) ObjCExport: translate collection type arguments as id
- [`KT-70546`](https://youtrack.jetbrains.com/issue/KT-70546) ObjCExport: method generic parameter is lost and translated as id
- [`KT-70329`](https://youtrack.jetbrains.com/issue/KT-70329) ObjCExport: translation and forward of super generic types
- [`KT-70263`](https://youtrack.jetbrains.com/issue/KT-70263) ObjCExport: generic extension support
- [`KT-69685`](https://youtrack.jetbrains.com/issue/KT-69685) ObjCExport: extension translated as not extension
- [`KT-70318`](https://youtrack.jetbrains.com/issue/KT-70318) ObjCExport: translate companion type
- [`KT-69252`](https://youtrack.jetbrains.com/issue/KT-69252) ObjCExport: Get rid of context receivers from ./native/objcexport-header-generator

### Tools. Gradle

#### New Features

- [`KT-69940`](https://youtrack.jetbrains.com/issue/KT-69940) Expose supplementary compiler warnings via KGP
- [`KT-71603`](https://youtrack.jetbrains.com/issue/KT-71603) Introduce KotlinJvmExtension and KotlinAndroidExtension
- [`KT-70383`](https://youtrack.jetbrains.com/issue/KT-70383) KotlinJvmFactory registerKaptGenerateStubsTask() function should also request compilation task provider
- [`KT-65125`](https://youtrack.jetbrains.com/issue/KT-65125) Provide basic support for Swift Export in Kotlin Gradle Plugin
- [`KT-71602`](https://youtrack.jetbrains.com/issue/KT-71602) Introduce KotlinTopLevelExtension
- [`KT-69927`](https://youtrack.jetbrains.com/issue/KT-69927) Need ability to pass KotlinJvmCompilerOptions to registerKotlinJvmCompileTask()
- [`KT-71227`](https://youtrack.jetbrains.com/issue/KT-71227) [Compose] Add PausableComposition feature flag to the Compose Gradle Plugin
- [`KT-68345`](https://youtrack.jetbrains.com/issue/KT-68345) 'composeCompiler#stabilityConfigurationFile' doesn't allow setting multiple stability configuration files

#### Performance Improvements

- [`KT-65285`](https://youtrack.jetbrains.com/issue/KT-65285) Use uncompressed Klibs

#### Fixes

- [`KT-71411`](https://youtrack.jetbrains.com/issue/KT-71411) Add FUS statistics for new Dokka tasks
- [`KT-72495`](https://youtrack.jetbrains.com/issue/KT-72495) Warn about kotlin-compiler-embeddable loaded along KGP
- [`KT-70543`](https://youtrack.jetbrains.com/issue/KT-70543) Gradle: create migration guide for those who are using Kotlin compiler classes indirectly available in buildscripts
- [`KT-69329`](https://youtrack.jetbrains.com/issue/KT-69329) Compatibility with Gradle 8.9 release
- [`KT-71291`](https://youtrack.jetbrains.com/issue/KT-71291) Log plugins from the list as Gradle plugins
- [`KT-69255`](https://youtrack.jetbrains.com/issue/KT-69255) Deprecate KotlinCompilationOutput#resourcesDirProvider
- [`KT-61706`](https://youtrack.jetbrains.com/issue/KT-61706) Gradle: remove kotlin-compiler-embeddable from build runtime dependencies
- [`KT-73128`](https://youtrack.jetbrains.com/issue/KT-73128) Apply Kotlinlang template for partial HTMLs
- [`KT-47897`](https://youtrack.jetbrains.com/issue/KT-47897) Official Kotlin Gradle plugin api
- [`KT-58858`](https://youtrack.jetbrains.com/issue/KT-58858) Add KDoc documentation for Kotlin Gradle plugin API
- [`KT-73076`](https://youtrack.jetbrains.com/issue/KT-73076) Kotlin Gradle Plugin API Reference: adjust settings
- [`KT-72387`](https://youtrack.jetbrains.com/issue/KT-72387) KGP 2.1.0-RC-227 changes cause KSP to crash calling produceUnpackedKlib
- [`KT-53280`](https://youtrack.jetbrains.com/issue/KT-53280) Gradle plugin leaks some compiler related extensions into API
- [`KT-69851`](https://youtrack.jetbrains.com/issue/KT-69851) Compatibility with Gradle 8.10 release
- [`KT-65565`](https://youtrack.jetbrains.com/issue/KT-65565) Remove deprecated common platform plugin id
- [`KT-69719`](https://youtrack.jetbrains.com/issue/KT-69719) Bump minimal supported Gradle version to 7.6.3
- [`KT-69721`](https://youtrack.jetbrains.com/issue/KT-69721) Bump minimal supported Android Gradle plugin version to 7.3.1
- [`KT-66944`](https://youtrack.jetbrains.com/issue/KT-66944) Relax host requirements on Kotlin klib compilation
- [`KT-72651`](https://youtrack.jetbrains.com/issue/KT-72651) Unable to use `target` for KotlinBaseApiPlugin.createKotlin(Jvm/Android)Extension()
- [`KT-72467`](https://youtrack.jetbrains.com/issue/KT-72467) kotlin.sourceSets extension not added for KotlinBaseApiPlugin.createKotlinAndroidExtension()
- [`KT-72303`](https://youtrack.jetbrains.com/issue/KT-72303) KGP 2.1.0-Beta2 broke compatibility with KSP
- [`KT-68596`](https://youtrack.jetbrains.com/issue/KT-68596) Update KGP deprecations before 2.1
- [`KT-67951`](https://youtrack.jetbrains.com/issue/KT-67951) Update Compose extension KDoc
- [`KT-66049`](https://youtrack.jetbrains.com/issue/KT-66049) KGP JVM: Publishing isn't compatible with isolated projects and project dependencies
- [`KT-71405`](https://youtrack.jetbrains.com/issue/KT-71405) Compose compiler gradle plugin: project.layout.file can't be used as a value of the 'stabilityConfigurationFiles' option
- [`KT-71948`](https://youtrack.jetbrains.com/issue/KT-71948) KotlinJvmFactory : get rid of replaces with TODO()
- [`KT-72092`](https://youtrack.jetbrains.com/issue/KT-72092) Gradle: use packed klib variant as the default when no packaging attribute is present
- [`KT-58956`](https://youtrack.jetbrains.com/issue/KT-58956) Offer a shared interface for JVM and Android compilerOptions in Project extension
- [`KT-70251`](https://youtrack.jetbrains.com/issue/KT-70251) Gradle: hide compiler symbols in KGP
- [`KT-70430`](https://youtrack.jetbrains.com/issue/KT-70430) Clean-up obsolete Gradle plugin variants for Gradle versions <7.6
- [`KT-69853`](https://youtrack.jetbrains.com/issue/KT-69853) Compile against Gradle API 8.10
- [`KT-69852`](https://youtrack.jetbrains.com/issue/KT-69852) Run Gradle integration tests against Gradle 8.10 release
- [`KT-65990`](https://youtrack.jetbrains.com/issue/KT-65990) Update `GradleDeprecatedOption.level` values for arguments removed from the DSL after 2.1
- [`KT-69331`](https://youtrack.jetbrains.com/issue/KT-69331) Run tests against Gradle 8.9 release
- [`KT-69332`](https://youtrack.jetbrains.com/issue/KT-69332) Compile against Gradle 8.9 API
- [`KT-67174`](https://youtrack.jetbrains.com/issue/KT-67174) Cleanup old Test DSL
- [`KT-71071`](https://youtrack.jetbrains.com/issue/KT-71071) BuildFusStatisticsIT.testInvalidFusReportDir test failes on Windows
- [`KT-69585`](https://youtrack.jetbrains.com/issue/KT-69585) KGP / Composite Build: "Could not apply withXml() to generated POM" during publishing
- [`KT-59769`](https://youtrack.jetbrains.com/issue/KT-59769) Many "Unexpected exception happened" warnings during build without internet connection

### Tools. Gradle. Cocoapods

- [`KT-63811`](https://youtrack.jetbrains.com/issue/KT-63811) cinterop fails to build klib for iosArm64 target when iOS simulator SDK isn't installed
- [`KT-70500`](https://youtrack.jetbrains.com/issue/KT-70500) Remove useLibraries from CocoaPods plugin
- [`KT-56947`](https://youtrack.jetbrains.com/issue/KT-56947) Replace AFNetworking with a smaller library in tests

### Tools. Gradle. JS

- [`KT-69628`](https://youtrack.jetbrains.com/issue/KT-69628) K/Wasm: Node.js version per project
- [`KT-71578`](https://youtrack.jetbrains.com/issue/KT-71578) KotlinJS. Webpack does not recompile on changes with `per-file`
- [`KT-71536`](https://youtrack.jetbrains.com/issue/KT-71536) [JS, Wasm] Stop collecting information about KLIB IC in Kotlin2JsCompile
- [`KT-70621`](https://youtrack.jetbrains.com/issue/KT-70621) Move kotlin-test-js-runner out of Kotlin repository
- [`KT-67442`](https://youtrack.jetbrains.com/issue/KT-67442) KJS / Gradle: `kotlinStorePackageLock` fails due to OS-dependent lockfile with npm package manager

### Tools. Gradle. Multiplatform

#### New Features

- [`KT-70469`](https://youtrack.jetbrains.com/issue/KT-70469) Add feature flag for Project Isolation and Kotlin Multiplatform
- [`KT-70897`](https://youtrack.jetbrains.com/issue/KT-70897) Add KotlinBaseApiPlugin.kotlinAndroidExtension

#### Fixes

- [`KT-71206`](https://youtrack.jetbrains.com/issue/KT-71206) KGP: Test source set may get duplicated KLIBs of different versions
- [`KT-71209`](https://youtrack.jetbrains.com/issue/KT-71209) Drop Hierarchy Template diagnostic about used shortcuts
- [`KT-69412`](https://youtrack.jetbrains.com/issue/KT-69412) Change `KotlinTargetAlreadyDeclaredChecker`'s severity from warning to error
- [`KT-70060`](https://youtrack.jetbrains.com/issue/KT-70060) KGP: handleHierarchicalStructureFlagsMigration doesn't support project isolation
- [`KT-57280`](https://youtrack.jetbrains.com/issue/KT-57280) Expose Kotlin Project Structure metadata via consumable configurations instead of accessing all gradle projects directly
- [`KT-64999`](https://youtrack.jetbrains.com/issue/KT-64999) Support Project Isolation with Kotlin Native tasks (XCode integration, Cocoapods etc)
- [`KT-64998`](https://youtrack.jetbrains.com/issue/KT-64998) Granular Metadata Dependencies Transformation is not compatible with Project Isolation
- [`KT-70650`](https://youtrack.jetbrains.com/issue/KT-70650) GenerateProjectStructureMetadata is not compatible with Project Isolation
- [`KT-71675`](https://youtrack.jetbrains.com/issue/KT-71675) checkSandboxAndWriteProtection collides with Compose's syncComposeResources
- [`KT-66461`](https://youtrack.jetbrains.com/issue/KT-66461) Promote compiler options DSL for multiplatform projects to stable
- [`KT-69323`](https://youtrack.jetbrains.com/issue/KT-69323) Don't pass platform dependencies to metadata compilation
- [`KT-72454`](https://youtrack.jetbrains.com/issue/KT-72454) Revert changes made in KT-69899 i.e. make kotlin.android.buildTypeAttribute.keep = false by default again
- [`KT-70380`](https://youtrack.jetbrains.com/issue/KT-70380) KMM App failed to consume android binary lib
- [`KT-71423`](https://youtrack.jetbrains.com/issue/KT-71423) Xcode archive missing dSYM files since Kotlin 2.0.20
- [`KT-69899`](https://youtrack.jetbrains.com/issue/KT-69899) KMP: Publish BuildType by default for android publications with multiple variants
- [`KT-71428`](https://youtrack.jetbrains.com/issue/KT-71428) Change deprecation message for KMP target shorcuts
- [`KT-58231`](https://youtrack.jetbrains.com/issue/KT-58231) Kotlin Gradle Plugin: set deprecation level to Error for KotlinTarget.useDisambiguationClassifierAsSourceSetNamePrefix and overrideDisambiguationClassifierOnIdeImport
- [`KT-72068`](https://youtrack.jetbrains.com/issue/KT-72068) Distribution for klib cross-compilation is not downloaded during compile tasks
- [`KT-70612`](https://youtrack.jetbrains.com/issue/KT-70612) Report incompatibility warning when Project Isolation enabled and Included builds are used
- [`KT-71529`](https://youtrack.jetbrains.com/issue/KT-71529) Deprecate targetFromPreset API with an error
- [`KT-69614`](https://youtrack.jetbrains.com/issue/KT-69614) Deprecate with error ios/tvos/watchos presets
- [`KT-69974`](https://youtrack.jetbrains.com/issue/KT-69974) KMP: POM dependency rewriter doesn't work with Included Builds OR dependencySubstitution
- [`KT-69472`](https://youtrack.jetbrains.com/issue/KT-69472) Remove IncompatibleAgpVersionTooHighWarning diagnostic
- [`KT-64996`](https://youtrack.jetbrains.com/issue/KT-64996) Commonize Native Distribution task is not compatible with Project Isolation
- [`KT-62911`](https://youtrack.jetbrains.com/issue/KT-62911) Export Kotlin Multipaltform Project Coordinates as a secondary variant of apiMetadataElements
- [`KT-70888`](https://youtrack.jetbrains.com/issue/KT-70888) Project isolation: Project cannot dynamically look up a property in the parent project at PropertiesProvider.propertiesWithPrefix
- [`KT-70688`](https://youtrack.jetbrains.com/issue/KT-70688) Move ExperimentalSwiftExportDsl to another package
- [`KT-58298`](https://youtrack.jetbrains.com/issue/KT-58298) AndroidAndJavaConsumeMppLibIT maintenance: Convert to new infrastructure and add test for newer AGP versions
- [`KT-68976`](https://youtrack.jetbrains.com/issue/KT-68976) K2 IDE: Unresolved FileSystem.SYSTEM from OKIO in shared source sets

### Tools. Gradle. Native

- [`KT-67162`](https://youtrack.jetbrains.com/issue/KT-67162) KGP: Kotlin/Native with Isolated Projects: kotlinNativeBundleBuildService cannot be changed any futher
- [`KT-72366`](https://youtrack.jetbrains.com/issue/KT-72366) KGP 2.1.0-Beta2 doesn't download `kotlin-native-prebuilt` when running Dokka
- [`KT-45559`](https://youtrack.jetbrains.com/issue/KT-45559) CInteropProcess: Changes to header files are not recognized; Task is still UP-TO-DATE
- [`KT-71051`](https://youtrack.jetbrains.com/issue/KT-71051) K/N dependencies are re-downloaded multiple times on Windows
- [`KT-71398`](https://youtrack.jetbrains.com/issue/KT-71398) kotlinNativeBundleConfiguration should not contain dependencies on unsupported platforms
- [`KT-71722`](https://youtrack.jetbrains.com/issue/KT-71722) kotlinNativeBundleConfiguration present in JVM-only Gradle project
- [`KT-55832`](https://youtrack.jetbrains.com/issue/KT-55832) Support passing errors to Xcode when configuration cache is enabled
- [`KT-70690`](https://youtrack.jetbrains.com/issue/KT-70690) not possible to build iOS app with Swift Export and Xcode 16
- [`KT-65838`](https://youtrack.jetbrains.com/issue/KT-65838) Remove project usage from PlatformLibrariesGenerator
- [`KT-70875`](https://youtrack.jetbrains.com/issue/KT-70875) KSP1 native tasks fail on configuration phase

### Tools. Incremental Compile

- [`KT-69123`](https://youtrack.jetbrains.com/issue/KT-69123) IC: "NoSuchFieldError: No instance field". Not tracking changes to Android ViewBinding class

### Tools. JPS

- [`KT-68565`](https://youtrack.jetbrains.com/issue/KT-68565) K2: IllegalStateException: Source classes should be created separately before referencing
- [`KT-71042`](https://youtrack.jetbrains.com/issue/KT-71042) `JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE` when compiling IntelliJ

### Tools. Kapt

- [`KT-72249`](https://youtrack.jetbrains.com/issue/KT-72249) K2 KAPT Not picking up use site annontation like K1 Kapt
- [`KT-69860`](https://youtrack.jetbrains.com/issue/KT-69860) K2 kapt: use compiler directly instead of Analysis API
- [`KT-71776`](https://youtrack.jetbrains.com/issue/KT-71776) K2 Kapt in 2.1.0-Beta1 fails with `e: java.lang.IllegalStateException: FIR symbol "class org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol" is not supported in constant evaluation`
- [`KT-70879`](https://youtrack.jetbrains.com/issue/KT-70879) Kapt: check that Kotlin 2.1 language features are ignored correctly by K1 kapt
- [`KT-71431`](https://youtrack.jetbrains.com/issue/KT-71431) K2KAPT fails on modules without any annotation processors
- [`KT-70600`](https://youtrack.jetbrains.com/issue/KT-70600) K2 KAPT: inline reified function has a null signature
- [`KT-70718`](https://youtrack.jetbrains.com/issue/KT-70718) Kapt: "error: could not load module <Error module>" on error type in data class component
- [`KT-69861`](https://youtrack.jetbrains.com/issue/KT-69861) Kapt: use IR to obtain line information instead of PSI

### Tools. REPL

- [`KT-71109`](https://youtrack.jetbrains.com/issue/KT-71109) Kotlin Scripting REPL doesn't support keyboard shortcuts

### Tools. Scripts

- [`KT-68685`](https://youtrack.jetbrains.com/issue/KT-68685) K2 / Script: "KotlinReflectionInternalError: Unresolved class:" caused by main.kts script with nested classes and reflection
- [`KT-68545`](https://youtrack.jetbrains.com/issue/KT-68545) Using labeled `this` access to implicit receivers fails in scripts

### Tools. Wasm

- [`KT-67797`](https://youtrack.jetbrains.com/issue/KT-67797) Improve the variable view during debugging in Fleet for Kotlin/Wasm
- [`KT-71506`](https://youtrack.jetbrains.com/issue/KT-71506) [Wasm, IC] FUS report for builds with incremental compilation
- [`KT-70100`](https://youtrack.jetbrains.com/issue/KT-70100) wasmJs Target Fails to Compile on ARM64 Linux
- [`KT-70367`](https://youtrack.jetbrains.com/issue/KT-70367) Update binaryen once we get a release with PR 6793
- [`KT-67863`](https://youtrack.jetbrains.com/issue/KT-67863) K/Wasm: Remove ChromeWasmGc
- [`KT-71360`](https://youtrack.jetbrains.com/issue/KT-71360) K/JS & K/Wasm: Upgrade NPM dependencies
- [`KT-70297`](https://youtrack.jetbrains.com/issue/KT-70297) Wasm: Incorrect kotlinJsTestRunner version set in Multi-Project Builds with mixed kotlin-stdlibs

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