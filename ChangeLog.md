## 2.2.0-Beta1

### Analysis API

- [`KT-75880`](https://youtrack.jetbrains.com/issue/KT-75880) K2 Mode: Typealias reference resolves to the underlying class in KMP project
- [`KT-74246`](https://youtrack.jetbrains.com/issue/KT-74246) KaVisibilityChecker.isVisible is inefficient with multiple calls on the same use-site
- [`KT-57733`](https://youtrack.jetbrains.com/issue/KT-57733) Analysis API: Use optimized `ModuleWithDependenciesScope`s in combined symbol providers

### Analysis API. Code Compilation

- [`KT-75502`](https://youtrack.jetbrains.com/issue/KT-75502) K2: IDEA hangs when evaluating inside kotlin-stdlib modules in the Kotlin project
- [`KT-73077`](https://youtrack.jetbrains.com/issue/KT-73077) Evaluation of inline functions is broken inside Kotlin project and Amper module in Idea sources
- [`KT-73936`](https://youtrack.jetbrains.com/issue/KT-73936) K2: CyclicInlineDependencyException: Inline functions have a cyclic dependency in evaluator
- [`KT-74582`](https://youtrack.jetbrains.com/issue/KT-74582) InterpreterMethodNotFoundError when trying to evaluate simple expressions after recent fixes
- [`KT-74524`](https://youtrack.jetbrains.com/issue/KT-74524) Compilation exception with incorrect JvmName annotation arguments
- [`KT-74443`](https://youtrack.jetbrains.com/issue/KT-74443) Compilation peer collector ignores inline property accessors

### Analysis API. FIR

#### Performance Improvements

- [`KT-75790`](https://youtrack.jetbrains.com/issue/KT-75790) Experiment with increasing DEFAULT_LOCKING_INTERVAL time
- [`KT-72159`](https://youtrack.jetbrains.com/issue/KT-72159) LLFirCompilerRequiredAnnotationsTargetResolver: consider rewriting it to use honest jumping locks

#### Fixes

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
- [`KT-73493`](https://youtrack.jetbrains.com/issue/KT-73493) Support context parameters
- [`KT-73183`](https://youtrack.jetbrains.com/issue/KT-73183) Support context parameters in ContextCollectorVisitor
- [`KT-60350`](https://youtrack.jetbrains.com/issue/KT-60350) K2 IDE: top level destructuring RHS should be resolvable
- [`KT-74794`](https://youtrack.jetbrains.com/issue/KT-74794) K2: FirLazyExpression should be calculated before accessing with context parameter and implicit return type
- [`KT-72938`](https://youtrack.jetbrains.com/issue/KT-72938) Get rid of KaFirAnnotationListForReceiverParameter
- [`KT-73727`](https://youtrack.jetbrains.com/issue/KT-73727) Exception in implicit type resolution

### Analysis API. Infrastructure

- [`KT-74917`](https://youtrack.jetbrains.com/issue/KT-74917) [Analysis API, Test Framework] Introduce a way to acquire `PsiFile` for a given `TestFile` in `KtTestModule`

### Analysis API. Light Classes

- [`KT-75391`](https://youtrack.jetbrains.com/issue/KT-75391) Reduce the amount of psi-based logic in light classes
- [`KT-70001`](https://youtrack.jetbrains.com/issue/KT-70001) SLC adds `@Override` with zero text offset on `override` member
- [`KT-75755`](https://youtrack.jetbrains.com/issue/KT-75755) K2. False positive red code on vararg parameters in Kotlin class with `@JvmOverloads` when called from Java
- [`KT-75397`](https://youtrack.jetbrains.com/issue/KT-75397) Constructors and functions with non-last vararg parameters are treated as varargs
- [`KT-73405`](https://youtrack.jetbrains.com/issue/KT-73405) Get rid of KtElement#{symbolPointer, symbolPointerOfType} API usages
- [`KT-74868`](https://youtrack.jetbrains.com/issue/KT-74868) Support context parameters
- [`KT-74733`](https://youtrack.jetbrains.com/issue/KT-74733) SymbolPsiLiteral.text == value for Java constant
- [`KT-74620`](https://youtrack.jetbrains.com/issue/KT-74620) Delegated functions with value classes are present in light classes
- [`KT-74595`](https://youtrack.jetbrains.com/issue/KT-74595) Static functions with value classes are present in light classes
- [`KT-74284`](https://youtrack.jetbrains.com/issue/KT-74284) Synthetic data class methods using value class types present in LC

### Analysis API. Providers and Caches

- [`KT-74090`](https://youtrack.jetbrains.com/issue/KT-74090) Analysis API: Support dumb mode (restricted analysis)
- [`KT-74943`](https://youtrack.jetbrains.com/issue/KT-74943) Analysis API: Replace `KotlinGlobalModificationService` with simpler global modification event publishing and listener-based modification trackers
- [`KT-70518`](https://youtrack.jetbrains.com/issue/KT-70518) K2: Analysis API: Access indices outside of `ConcurrentMap` computation in symbol providers
- [`KT-62115`](https://youtrack.jetbrains.com/issue/KT-62115) Analysis API: Package providers are not cached per search scope
- [`KT-74302`](https://youtrack.jetbrains.com/issue/KT-74302) Analysis API: `LLFirProvider` should disregard self-declarations in `getFirClassifierBy*`
- [`KT-74463`](https://youtrack.jetbrains.com/issue/KT-74463) Analysis API: `LLNativeForwardDeclarationsSymbolProvider` queries its cache even when the `ClassId` cannot represent a native forward declaration
- [`KT-67868`](https://youtrack.jetbrains.com/issue/KT-67868) Analysis API: Improve the architecture of `LLFirKotlinSymbolProvider`s

### Analysis API. Standalone

- [`KT-72810`](https://youtrack.jetbrains.com/issue/KT-72810) withMultiplatformLightClassSupport is inconvenient in Standalone

### Analysis API. Stubs and Decompilation

- [`KT-68484`](https://youtrack.jetbrains.com/issue/KT-68484) K2 IDE, Analysis API: "We should be able to find a symbol for function" for getting KaType of `Iterable<T>.map(transform: (T) -> R)` parameter in J2K

### Analysis API. Surface

#### New Features

- [`KT-74475`](https://youtrack.jetbrains.com/issue/KT-74475) Add `isInline` for `KaPropertySymbol`
- [`KT-75063`](https://youtrack.jetbrains.com/issue/KT-75063) KaScopeContext: support context parameters

#### Performance Improvements

- [`KT-70165`](https://youtrack.jetbrains.com/issue/KT-70165) Introduce PSI-based `KaSymbol`s for K2

#### Fixes

- [`KT-72482`](https://youtrack.jetbrains.com/issue/KT-72482) "KotlinIllegalArgumentExceptionWithAttachments: Expected all candidates to have same callableId but some of them but was different" on trying to add the import
- [`KT-75894`](https://youtrack.jetbrains.com/issue/KT-75894) Cannot build KaFirJavaFieldSymbol for FirFieldImpl
- [`KT-75586`](https://youtrack.jetbrains.com/issue/KT-75586) `KaFirPropertyGetterSymbol#isInline` and `KaFirPropertySetterSymbol#isInline` is incorrect for accessors with explicit modifier
- [`KT-58572`](https://youtrack.jetbrains.com/issue/KT-58572) Analysis API: Enforcing STATUS resolve in 'KtFirNamedClassOrObjectSymbol.visibility' may cause lazy resolve contract violation
- [`KT-72730`](https://youtrack.jetbrains.com/issue/KT-72730) K2: "Unexpected owner function: KtNamedFunction" on vararg val parameter in function
- [`KT-75574`](https://youtrack.jetbrains.com/issue/KT-75574) Recognize injected code fragment copies
- [`KT-75573`](https://youtrack.jetbrains.com/issue/KT-75573) Recognize physical file copies as dangling files
- [`KT-74801`](https://youtrack.jetbrains.com/issue/KT-74801) Analysis API: Publish/subscribe to modification events with a single message bus topic
- [`KT-73290`](https://youtrack.jetbrains.com/issue/KT-73290) Analysis API: Improve the architecture of content scopes and resolution scopes
- [`KT-68901`](https://youtrack.jetbrains.com/issue/KT-68901) Constructor delegation call receiver missing in fir implementation
- [`KT-75115`](https://youtrack.jetbrains.com/issue/KT-75115) Analysis API: The `JavaModuleResolver` compiler class is leaked to Analysis API platform implementations
- [`KT-75123`](https://youtrack.jetbrains.com/issue/KT-75123) K2. KaFirNamedFunctionSymbol should contain a receiver
- [`KT-72639`](https://youtrack.jetbrains.com/issue/KT-72639) Support context parameter API
- [`KT-73112`](https://youtrack.jetbrains.com/issue/KT-73112) AA: FirExpression.toKtReceiverValue should handle context receivers properly
- [`KT-74905`](https://youtrack.jetbrains.com/issue/KT-74905) Cannot find context receiver in FIR declaration
- [`KT-74563`](https://youtrack.jetbrains.com/issue/KT-74563) `createPointer` is overloaded not for all implementations
- [`KT-73722`](https://youtrack.jetbrains.com/issue/KT-73722) Analysis API: Automatically check that the API surface is fully documented
- [`KT-65065`](https://youtrack.jetbrains.com/issue/KT-65065) Provide `KtTypeReference#getShortTypeText()`

### Backend. Wasm

- [`KT-59032`](https://youtrack.jetbrains.com/issue/KT-59032) Support instantiation of annotation classes on WASM
- [`KT-74441`](https://youtrack.jetbrains.com/issue/KT-74441) K/Wasm: incorrect 1e-45.toString()
- [`KT-59118`](https://youtrack.jetbrains.com/issue/KT-59118) WASM: floating point toString inconsistencies
- [`KT-68948`](https://youtrack.jetbrains.com/issue/KT-68948) Wasm: float from variable is printed with many decimal points
- [`KT-69107`](https://youtrack.jetbrains.com/issue/KT-69107) [wasm] Seemingly incorrect rounding

### Compiler

#### New Features

- [`KT-75315`](https://youtrack.jetbrains.com/issue/KT-75315) Support context-sensitive resolution in the call-argument position
- [`KT-75316`](https://youtrack.jetbrains.com/issue/KT-75316) Support context-sensitive resolution for expression-position with expected type
- [`KT-71768`](https://youtrack.jetbrains.com/issue/KT-71768) Enable -Xjvm-default=all-compatibility by default to generate JVM default interface methods
- [`KT-76088`](https://youtrack.jetbrains.com/issue/KT-76088) Support context-sensitive resolution for annotation arguments
- [`KT-74049`](https://youtrack.jetbrains.com/issue/KT-74049) Introduce special override rule to allow overriding T! with T & Any
- [`KT-74809`](https://youtrack.jetbrains.com/issue/KT-74809) Support unnamed local variables
- [`KT-75061`](https://youtrack.jetbrains.com/issue/KT-75061) Support context-sensitive resolution in type position
- [`KT-74811`](https://youtrack.jetbrains.com/issue/KT-74811) Prohibit usages of `@MustUseValue` / `@IgnorableValue` if RV checker is not enabled
- [`KT-74806`](https://youtrack.jetbrains.com/issue/KT-74806) Implement feature flag for improved unused return value checker
- [`KT-73508`](https://youtrack.jetbrains.com/issue/KT-73508) Add a warning diagnostic for using kotlin.concurrent.AtomicRef<Int>
- [`KT-72941`](https://youtrack.jetbrains.com/issue/KT-72941) ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE missing in K2
- [`KT-74497`](https://youtrack.jetbrains.com/issue/KT-74497) Warn about incompatible Kotlin and Java targets in annotations
- [`KT-74382`](https://youtrack.jetbrains.com/issue/KT-74382) Annotating Java record components for `@JvmRecord` data class
- [`KT-73255`](https://youtrack.jetbrains.com/issue/KT-73255) Change defaulting rule for annotations

#### Performance Improvements

- [`KT-75957`](https://youtrack.jetbrains.com/issue/KT-75957) K2: PsiRawFirBuilder.Visitor#toFirExpression forces AST loading via getSpreadElement
- [`KT-74824`](https://youtrack.jetbrains.com/issue/KT-74824) Exponential performance caused by nested flexible types
- [`KT-62855`](https://youtrack.jetbrains.com/issue/KT-62855) K2: extra allocation for SAM conversion compared to K1
- [`KT-74977`](https://youtrack.jetbrains.com/issue/KT-74977) K/N: support stack array for Array(size) call
- [`KT-74369`](https://youtrack.jetbrains.com/issue/KT-74369) Exponential compiler memory usage in specific situations with type inference

#### Fixes

- [`KT-75945`](https://youtrack.jetbrains.com/issue/KT-75945) Indy: Allow lambdas with annotations
- [`KT-59506`](https://youtrack.jetbrains.com/issue/KT-59506) Context receivers: Unable to use trailing comma in receiver list
- [`KT-4779`](https://youtrack.jetbrains.com/issue/KT-4779) Generate default methods for implementations in interfaces
- [`KT-71792`](https://youtrack.jetbrains.com/issue/KT-71792) Switch latest stable version in Kotlin project to 2.2
- [`KT-74827`](https://youtrack.jetbrains.com/issue/KT-74827) CompilationErrorException : Could not load module in an attempt to find deserializer when trying to evaluate an expression
- [`KT-74454`](https://youtrack.jetbrains.com/issue/KT-74454) Support trailing comma in context parameters
- [`KT-74337`](https://youtrack.jetbrains.com/issue/KT-74337) Local Delegated properties don't preserve their annotations and don't show up in reflection
- [`KT-55187`](https://youtrack.jetbrains.com/issue/KT-55187) Context receivers in function types can have labels
- [`KT-58498`](https://youtrack.jetbrains.com/issue/KT-58498) Context receivers: ClassCastException with object and extension function in interface
- [`KT-58165`](https://youtrack.jetbrains.com/issue/KT-58165) K2: "IllegalArgumentException: No argument for parameter VALUE_PARAMETER" on overridden contextual property
- [`KT-75535`](https://youtrack.jetbrains.com/issue/KT-75535) Compilation of typealias does not check for clashes
- [`KT-72313`](https://youtrack.jetbrains.com/issue/KT-72313) K2 IDE / KMP Debugger: Evaluation of inline functions declared in a common source set causes a crash
- [`KT-75815`](https://youtrack.jetbrains.com/issue/KT-75815) Disable warnings about different context parameter names in overrides
- [`KT-75483`](https://youtrack.jetbrains.com/issue/KT-75483) Native: redundant unboxing generated with smart cast
- [`KT-74421`](https://youtrack.jetbrains.com/issue/KT-74421) K2: Missing "val can not be assigned" when trying to assign a value to parent's "val"
- [`KT-55083`](https://youtrack.jetbrains.com/issue/KT-55083) JVM: AbstractMethodError caused by lambda with sealed base interface and fun sub interface and overridden method
- [`KT-16727`](https://youtrack.jetbrains.com/issue/KT-16727) Names for anonymous classes in interfaces are malformed on JDK 8
- [`KT-12466`](https://youtrack.jetbrains.com/issue/KT-12466) NoClassDefFoundError: B$DefaultImpls on super interface call through K-J-K inheritance
- [`KT-75293`](https://youtrack.jetbrains.com/issue/KT-75293) K2: Missing [HAS_NEXT_FUNCTION_TYPE_MISMATCH] diagnostics
- [`KT-72734`](https://youtrack.jetbrains.com/issue/KT-72734) Support new callable reference nodes in Kotlin Native
- [`KT-75965`](https://youtrack.jetbrains.com/issue/KT-75965) The iOS app did not run successfully in Release mode
- [`KT-72335`](https://youtrack.jetbrains.com/issue/KT-72335) KotlinIllegalArgumentExceptionWithAttachments when using illegal selector
- [`KT-71718`](https://youtrack.jetbrains.com/issue/KT-71718) K2: drop TypePreservingVisibilityWrtHack
- [`KT-75969`](https://youtrack.jetbrains.com/issue/KT-75969) java.lang.IllegalArgumentException: source must not be null on red code
- [`KT-75322`](https://youtrack.jetbrains.com/issue/KT-75322) ConeDiagnosticToFirDiagnosticKt: source must not be null
- [`KT-73800`](https://youtrack.jetbrains.com/issue/KT-73800) Wrong method executed on super call in -Xjvm-default=all/all-compatibility with an extraneous super-interface
- [`KT-38029`](https://youtrack.jetbrains.com/issue/KT-38029) Wrong method executed on super call in diamond hierarchy with covariant override
- [`KT-76049`](https://youtrack.jetbrains.com/issue/KT-76049) K2: drop explicitTypeArgumentIfMadeFlexibleSynthetically creation when DontMakeExplicitJavaTypeArgumentsFlexible is enabled
- [`KT-76055`](https://youtrack.jetbrains.com/issue/KT-76055) K2: drop prepareCustomReturnTypeSubstitutorForFunctionCall logic when DontMakeExplicitJavaTypeArgumentsFlexible is enabled
- [`KT-76057`](https://youtrack.jetbrains.com/issue/KT-76057) K2: don't do reverse Java overridability checks when DontMakeExplicitJavaTypeArgumentsFlexible is enabled
- [`KT-75197`](https://youtrack.jetbrains.com/issue/KT-75197) K2: Missing [COMPARE_TO_TYPE_MISMATCH] diagnostics
- [`KT-75639`](https://youtrack.jetbrains.com/issue/KT-75639) Inline `context` function leads to `ClassCastException`
- [`KT-75677`](https://youtrack.jetbrains.com/issue/KT-75677) K2: change runtime behavior of KT-75649 case in 2.2
- [`KT-75961`](https://youtrack.jetbrains.com/issue/KT-75961) K2: `PsiRawFirBuilder.Visitor#visitSimpleNameExpression`forces AST loading via `getReferencedNameElement().node.text`
- [`KT-73611`](https://youtrack.jetbrains.com/issue/KT-73611) Remove -Xextended-compiler-checks in favor of a deprecation cycle
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
- [`KT-75040`](https://youtrack.jetbrains.com/issue/KT-75040) Unify `subject` and `subjectVariable` in `FirWhenExpression`
- [`KT-75323`](https://youtrack.jetbrains.com/issue/KT-75323) FirSyntheticProperty: Unexpected status. Expected is FirResolvedDeclarationStatus, but was FirDeclarationStatusImpl
- [`KT-75602`](https://youtrack.jetbrains.com/issue/KT-75602) Introduce concept of shared library session in Fir sessions
- [`KT-75509`](https://youtrack.jetbrains.com/issue/KT-75509) PARAMETER_NAME_CHANGED_ON_OVERRIDE is reported randomly
- [`KT-75124`](https://youtrack.jetbrains.com/issue/KT-75124) IAE “class org.jetbrains.kotlin.psi.KtContextReceiver is not a subtype of class org.jetbrains.kotlin.psi.KtParameter for factory EXPOSED_PARAMETER_TYPE” on private context receiver
- [`KT-73909`](https://youtrack.jetbrains.com/issue/KT-73909) Add an inspection discouraging usage of kotlin.concurrent Native atomics in favor of the new atomics
- [`KT-73585`](https://youtrack.jetbrains.com/issue/KT-73585) K2: ABSTRACT_SUPER_CALL is not reported
- [`KT-75531`](https://youtrack.jetbrains.com/issue/KT-75531) K2 REPL: local name doesn't shadow one from implicit receiver
- [`KT-58369`](https://youtrack.jetbrains.com/issue/KT-58369) K2: enable DFA warnings
- [`KT-73359`](https://youtrack.jetbrains.com/issue/KT-73359) Migrate frontend sources to new IR parameter API
- [`KT-75380`](https://youtrack.jetbrains.com/issue/KT-75380) K2: Modality is configured incorrectly for some FirDefaultPropertyAccessor
- [`KT-75526`](https://youtrack.jetbrains.com/issue/KT-75526) Regression in K2 scripting: local name doesn't shadow one from the implicit receiver
- [`KT-59379`](https://youtrack.jetbrains.com/issue/KT-59379) K2: Missing MIXING_NAMED_AND_POSITIONED_ARGUMENTS
- [`KT-74649`](https://youtrack.jetbrains.com/issue/KT-74649) Deprecate language versions 1.8 and 1.9
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
- [`KT-51258`](https://youtrack.jetbrains.com/issue/KT-51258) Annotations should go before context receivers
- [`KT-74336`](https://youtrack.jetbrains.com/issue/KT-74336) Not supported: class org.jetbrains.kotlin.fir.types.ConeIntersectionType
- [`KT-74203`](https://youtrack.jetbrains.com/issue/KT-74203) K2: False negative NO_ELSE_IN_WHEN of a generic type with star projection <*> bounded by a sealed hierarchy
- [`KT-63720`](https://youtrack.jetbrains.com/issue/KT-63720) Coroutine debugger: do not optimise out local variables
- [`KT-48085`](https://youtrack.jetbrains.com/issue/KT-48085) Kotlin/Native: LLD removes live code with `--gc-sections` when producing DLL
- [`KT-69164`](https://youtrack.jetbrains.com/issue/KT-69164) Native: use lld from bundled LLVM distribution when compiling on Windows for a MinGW target
- [`KT-74081`](https://youtrack.jetbrains.com/issue/KT-74081) Context parameters: implicit call resolves to extension when there is a context
- [`KT-74682`](https://youtrack.jetbrains.com/issue/KT-74682) Implement internal type exposure via parameter bounds deprecation postponement
- [`KT-74556`](https://youtrack.jetbrains.com/issue/KT-74556) K2: "IAE: class KtDestructuringDeclaration is not a subtype of class KtNamedDeclaration for factory REDECLARATION" with two non-local destructuring declarations
- [`KT-73146`](https://youtrack.jetbrains.com/issue/KT-73146) Context parameters CLI & diagnostics
- [`KT-72722`](https://youtrack.jetbrains.com/issue/KT-72722) Treat 'copy' calls of a data class as explicit constructor usages
- [`KT-74389`](https://youtrack.jetbrains.com/issue/KT-74389) K2: False positive NON_EXPORTABLE_TYPE on non-Unit `Promise<...>` in K/JS
- [`KT-72104`](https://youtrack.jetbrains.com/issue/KT-72104) Consider enabling check for unbound symbols in JVM before lowerings
- [`KT-74568`](https://youtrack.jetbrains.com/issue/KT-74568) Synthetic nested classes missing JVM attributes
- [`KT-73703`](https://youtrack.jetbrains.com/issue/KT-73703) [Native] Move KonanIrLinker to `serialization.native` module
- [`KT-61175`](https://youtrack.jetbrains.com/issue/KT-61175) K2: FirReceiverParameter does not extend FirDeclaration
- [`KT-73961`](https://youtrack.jetbrains.com/issue/KT-73961) 'lateinit is unnecessary' on transient properties should not be reported for serializable classes
- [`KT-73858`](https://youtrack.jetbrains.com/issue/KT-73858) Compose  / iOS: NullPointerException on building
- [`KT-62953`](https://youtrack.jetbrains.com/issue/KT-62953) JVM IR: Use `SimpleNamedCompilerPhase` instead of `NamedCompilerPhase`
- [`KT-72929`](https://youtrack.jetbrains.com/issue/KT-72929) Consider caching typealiased constructor symbols created by TypeAliasConstructorsSubstitutingScope
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
- [`KT-73771`](https://youtrack.jetbrains.com/issue/KT-73771) K2: Infinite compilation caused by buildList without type
- [`KT-67520`](https://youtrack.jetbrains.com/issue/KT-67520) Change of behaviour of inline function with safe cast on value type
- [`KT-67518`](https://youtrack.jetbrains.com/issue/KT-67518) Value classes leak their carrier type implementation details via inlining
- [`KT-72305`](https://youtrack.jetbrains.com/issue/KT-72305) K2: Report error when using synthetic properties in case of mapped collections
- [`KT-70233`](https://youtrack.jetbrains.com/issue/KT-70233) Implement a deprecation error for FIELD-targeted annotations on annotation properties
- [`KT-67517`](https://youtrack.jetbrains.com/issue/KT-67517) Value class upcast to Any leaks carrier type interfaces
- [`KT-72814`](https://youtrack.jetbrains.com/issue/KT-72814) FIR: don't use function references in FirThisReference
- [`KT-73153`](https://youtrack.jetbrains.com/issue/KT-73153) K2: Standalone diagnostics on type arguments are not reported
- [`KT-73011`](https://youtrack.jetbrains.com/issue/KT-73011) K2: Allow overloads resolution for callable references based on expected type variable with constraints
- [`KT-69223`](https://youtrack.jetbrains.com/issue/KT-69223) Drop parallel lowering mode in JVM backend
- [`KT-7461`](https://youtrack.jetbrains.com/issue/KT-7461) Forbid using projection modifiers inside top-level Array in annotation's value parameter
- [`KT-53804`](https://youtrack.jetbrains.com/issue/KT-53804) Restore old and incorrect logic of generating InnerClasses attributes for kotlin-stdlib
- [`KT-52774`](https://youtrack.jetbrains.com/issue/KT-52774) Resolve unqualified enum constants based on expected type

### Compose compiler

- [`b/401484249`](https://issuetracker.google.com/issues/401484249) Generate a group around `Array` constructor call
- [`b/400380396`](https://issuetracker.google.com/issues/400380396) Fix missing `endMovableGroup` call with early return in `key` function
- [`b/400495890`](https://issuetracker.google.com/issues/400495890) Replace function metrics global with an IR attribute
- [`b/397855145`](https://issuetracker.google.com/issues/397855145) Fix "Unknown file" error in target annotation inference
- [`b/274898109`](https://issuetracker.google.com/issues/274898109) Fix off-by-one error when calculating changed arg count for lambdas
- [`b/367066334`](https://issuetracker.google.com/issues/367066334) Add diagnostic to restrict `@Composable` annotation to function types only

### IDE

- [`KT-54804`](https://youtrack.jetbrains.com/issue/KT-54804) Generate synthetic functions for annotations on properties in light classes

### IR. Inlining

#### Fixes

- [`KT-75986`](https://youtrack.jetbrains.com/issue/KT-75986) Add an option to the `DumpIrTreeOptions` to dump IR signature if available
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

### IR. Interpreter

- [`KT-74581`](https://youtrack.jetbrains.com/issue/KT-74581) Support `IrRich*Reference` in IR interpreter

### IR. Tree

#### Fixes

- [`KT-75679`](https://youtrack.jetbrains.com/issue/KT-75679) Extract common `invokeFunction` in IrRichCallableReference
- [`KT-74799`](https://youtrack.jetbrains.com/issue/KT-74799) [Native][IR] Excessive FUNCTION_INTERFACE_CLASS after deserialization
- [`KT-71138`](https://youtrack.jetbrains.com/issue/KT-71138) Report error on cross-file IrGetField operations generated by compiler plugins
- [`KT-74331`](https://youtrack.jetbrains.com/issue/KT-74331) Implement IrElement.copyAttributes as a true attribute map copy
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

### JavaScript

#### Performance Improvements

- [`KT-74533`](https://youtrack.jetbrains.com/issue/KT-74533) K/JS: avoid number to char calls in charSequenceGet intrinsic

#### Fixes

- [`KT-57192`](https://youtrack.jetbrains.com/issue/KT-57192) KJS: "Exported declaration uses non-exportable return type" caused by `@JsExport` Promise with Unit type
- [`KT-73363`](https://youtrack.jetbrains.com/issue/KT-73363) Migrate `js-plain-objects` plugin to new IR parameter API
- [`KT-64927`](https://youtrack.jetbrains.com/issue/KT-64927) [JS] TypeError when abstract class with override var property extends an exported abstract class with val property
- [`KT-75606`](https://youtrack.jetbrains.com/issue/KT-75606) KJS: java.lang.AssertionError: Different declarations with the same signatures were detected
- [`KT-70623`](https://youtrack.jetbrains.com/issue/KT-70623) Kotlin/JS: Incremental compilation fails when granularity is changed
- [`KT-70652`](https://youtrack.jetbrains.com/issue/KT-70652) Kotlin/JS:  `@JsExport` doesn't work with granularity per-file
- [`KT-71365`](https://youtrack.jetbrains.com/issue/KT-71365) KJS. File-level export support
- [`KT-68775`](https://youtrack.jetbrains.com/issue/KT-68775) Kotlin/JS infinite loop for exception message override that calls super.message
- [`KT-42271`](https://youtrack.jetbrains.com/issue/KT-42271) K/JS: isInitialized for not-lateinit property isn't marked as error as in JVM project
- [`KT-72437`](https://youtrack.jetbrains.com/issue/KT-72437) KJS. Invalid `copy` method for inherited JSO with type parameters
- [`KT-70664`](https://youtrack.jetbrains.com/issue/KT-70664) Extending a `@JsPlainObject` interface with a generic type parameter fails with a compile error
- [`KT-74839`](https://youtrack.jetbrains.com/issue/KT-74839) AssociatedObjectKey metadata doesn't survive incremental compilation
- [`KT-71169`](https://youtrack.jetbrains.com/issue/KT-71169) `@JsPlainObject` copy produces the wrong type when copied property is nullable in parent interface
- [`KT-71656`](https://youtrack.jetbrains.com/issue/KT-71656) K2 JS: "IllegalStateException: Class has no primary constructor: kotlin.ULong"

### Klibs

#### Fixes

- [`KT-70146`](https://youtrack.jetbrains.com/issue/KT-70146) [KLIB Resolve] Don't fail on nonexistent transitive dependency
- [`KT-75624`](https://youtrack.jetbrains.com/issue/KT-75624) Don't fail on an attempt to deserialize "unknown" IrStatementOrigin and IrDeclarationOrigin
- [`KT-55808`](https://youtrack.jetbrains.com/issue/KT-55808) Support metadata version checks for klibs in the compiler
- [`KT-56062`](https://youtrack.jetbrains.com/issue/KT-56062) Support `-Xmetadata-version` for KLIB-based compilers
- [`KT-75192`](https://youtrack.jetbrains.com/issue/KT-75192) KLIB reader tends to extract files from the KLIB archive to a temporary directory even when this is not needed
- [`KT-75013`](https://youtrack.jetbrains.com/issue/KT-75013) Make klib reader more flexible: allow empty directories to be omitted
- [`KT-75680`](https://youtrack.jetbrains.com/issue/KT-75680) KLIB: Drop obsolete IrPerFileLibraryImpl & IrPerFileWriterImpl
- [`KT-73779`](https://youtrack.jetbrains.com/issue/KT-73779) [Native] Context parameters: extension receiver is preferred over context parameters
- [`KT-65375`](https://youtrack.jetbrains.com/issue/KT-65375) Clean-up the logic for serialization of error types in metadata and in IR
- [`KT-73826`](https://youtrack.jetbrains.com/issue/KT-73826) Deduplicate `IrFileEntry` that is serialized inside `IrInlinedFunctionBlock`
- [`KT-75091`](https://youtrack.jetbrains.com/issue/KT-75091) Drop `targets/$target_name/kotlin` directory from klibs
- [`KT-74635`](https://youtrack.jetbrains.com/issue/KT-74635) KLIBs: Change call serialization scheme to store all arguments in a single list
- [`KT-74352`](https://youtrack.jetbrains.com/issue/KT-74352) API4ABI: Fix representation of context parameters
- [`KT-71007`](https://youtrack.jetbrains.com/issue/KT-71007) Align KLIB ABI version with the language version
- [`KT-73672`](https://youtrack.jetbrains.com/issue/KT-73672) Bump KLIB ABI version in 2.2.0
- [`KT-74080`](https://youtrack.jetbrains.com/issue/KT-74080) API4ABI: Adapt API for value parameter kinds
- [`KT-74396`](https://youtrack.jetbrains.com/issue/KT-74396) Support context parameters in klibs
- [`KT-72931`](https://youtrack.jetbrains.com/issue/KT-72931) Support new callable reference nodes in KLIB [de]serializer

### Language Design

- [`KT-72417`](https://youtrack.jetbrains.com/issue/KT-72417) Annotation with target RECORD_COMPONENT cannot be used on `@JvmRecord` data class components
- [`KT-73557`](https://youtrack.jetbrains.com/issue/KT-73557) Allow refining expect declarations for platform groups
- [`KT-54206`](https://youtrack.jetbrains.com/issue/KT-54206) Support local contextual functions
- [`KT-67977`](https://youtrack.jetbrains.com/issue/KT-67977) Compile results of annotations assigned to JvmRecord properties as in Java
- [`KT-73502`](https://youtrack.jetbrains.com/issue/KT-73502) Context parameters: it is not possible to declare local function with a context
- [`KT-73632`](https://youtrack.jetbrains.com/issue/KT-73632) Expect class redeclaration is allowed
- [`KT-70002`](https://youtrack.jetbrains.com/issue/KT-70002) [LC] Forbid using projection modifiers inside top-level Array in annotation's value parameter

### Libraries

- [`KT-75933`](https://youtrack.jetbrains.com/issue/KT-75933) Update readLine's KDoc to suggest alternative functions
- [`KT-46360`](https://youtrack.jetbrains.com/issue/KT-46360) Type inference fails to infer type for sumOf call with integer literal: "Overload resolution ambiguity TypeVariable(T)) -> Int / Long"
- [`KT-74804`](https://youtrack.jetbrains.com/issue/KT-74804) Add `@MustUseValue` and `@IgnorableValue` / `@Discardable` to kotlin-stdlib
- [`KT-73590`](https://youtrack.jetbrains.com/issue/KT-73590) Samplify string.split
- [`KT-75759`](https://youtrack.jetbrains.com/issue/KT-75759) Add the serializer for kotlin.time.Instant to the list of standard serializers
- [`KT-71628`](https://youtrack.jetbrains.com/issue/KT-71628) Review deprecations in stdlib for 2.1
- [`KT-73726`](https://youtrack.jetbrains.com/issue/KT-73726) A link from shuffle's KDoc is not rendered properly
- [`KT-74173`](https://youtrack.jetbrains.com/issue/KT-74173) The sample code of `lazy` on stdlib can not run on playground due to "samples" package import
- [`KT-50081`](https://youtrack.jetbrains.com/issue/KT-50081) AbstractList sublist leads to StackOverflow
- [`KT-74422`](https://youtrack.jetbrains.com/issue/KT-74422) KotlinWebsiteSampleRewriter should filter individual imports from samples package

### Native

- [`KT-74377`](https://youtrack.jetbrains.com/issue/KT-74377) Kotlin Native: release executable crashes with error 139
- [`KT-70202`](https://youtrack.jetbrains.com/issue/KT-70202) Xcode 16 Linker fails with SIGBUS

### Native. C and ObjC Import

- [`KT-75781`](https://youtrack.jetbrains.com/issue/KT-75781) Xcode 16.3: Fix cinterop tests failing with fatal error: could not build module '_stdint'
- [`KT-75598`](https://youtrack.jetbrains.com/issue/KT-75598) Native: fix samples/objc test
- [`KT-74549`](https://youtrack.jetbrains.com/issue/KT-74549) Native: replace clang_Type_getNumProtocols/clang_Type_getProtocol with standard libclang functions

### Native. Platforms

- [`KT-74702`](https://youtrack.jetbrains.com/issue/KT-74702) Deprecate Windows 7 support

### Native. Runtime

- [`KT-71534`](https://youtrack.jetbrains.com/issue/KT-71534) Native: Support Latin-1 encoded strings at runtime
- [`KT-67741`](https://youtrack.jetbrains.com/issue/KT-67741) Kotlin/Native: Unify SpecialRef handling

### Native. Runtime. Memory

- [`KT-74432`](https://youtrack.jetbrains.com/issue/KT-74432) Native: add an option to allocate everything in SingleObjectPage
- [`KT-74975`](https://youtrack.jetbrains.com/issue/KT-74975) Enable CMS by default in Swift export
- [`KT-74831`](https://youtrack.jetbrains.com/issue/KT-74831) Kotlin/Native: investigate mmap usage in custom allocator
- [`KT-60928`](https://youtrack.jetbrains.com/issue/KT-60928) Kotlin/Native: refactor allocator code
- [`KT-50291`](https://youtrack.jetbrains.com/issue/KT-50291) Kotlin/Native: remove dependency of mm on gc implementation

### Native. Swift Export

- [`KT-75166`](https://youtrack.jetbrains.com/issue/KT-75166) Support export of platform libraries types in Swift export
- [`KT-75079`](https://youtrack.jetbrains.com/issue/KT-75079) Swift export: add dependency from sir-compiler-bridge to Analysis API
- [`KT-72413`](https://youtrack.jetbrains.com/issue/KT-72413) Swift Export: potential memory leak when best-fitting class is different from the formal type
- [`KT-72107`](https://youtrack.jetbrains.com/issue/KT-72107) Remove IntoSingleModule stratagy

### Reflection

- [`KT-75464`](https://youtrack.jetbrains.com/issue/KT-75464) Bundle kotlin-metadata-jvm into kotlin-reflect
- [`KT-71832`](https://youtrack.jetbrains.com/issue/KT-71832) kotlin.jvm.internal.ClassReference static overhead is 11,060 bytes

### Test Infrastructure

- [`KT-75152`](https://youtrack.jetbrains.com/issue/KT-75152) Introduce phase checks to `PerformanceManager`
- [`KT-75987`](https://youtrack.jetbrains.com/issue/KT-75987) Refine names for jps tests that are shown after running test data helper plugin
- [`KT-74254`](https://youtrack.jetbrains.com/issue/KT-74254) Perform version 2.2 boostrapping and language version update
- [`KT-75194`](https://youtrack.jetbrains.com/issue/KT-75194) Fix remaining problems of PerformanceManager
- [`KT-75309`](https://youtrack.jetbrains.com/issue/KT-75309) Add kotlin-compiler-server as K2 user project
- [`KT-75094`](https://youtrack.jetbrains.com/issue/KT-75094) Introduce NumberAgnosticSanitizer and use it for checking performance dumps
- [`KT-74987`](https://youtrack.jetbrains.com/issue/KT-74987) Get rid of using PerformanceCounter in CommonCompilerPerformanceManager
- [`KT-74705`](https://youtrack.jetbrains.com/issue/KT-74705) Remove org/jetbrains/kotlin/utils/intellijUtil.kt file with now obsolete Pair.companion functions

### Tools. Ant

- [`KT-73116`](https://youtrack.jetbrains.com/issue/KT-73116) Deprecate Ant support

### Tools. BCV

- [`KT-71168`](https://youtrack.jetbrains.com/issue/KT-71168) Implement a prototype of ABI Validation in Kotlin Gradle Plugin

### Tools. Build Tools API

- [`KT-76060`](https://youtrack.jetbrains.com/issue/KT-76060) BTA: Java sources passed for IC may fail compilation in non-incremental mode
- [`KT-74041`](https://youtrack.jetbrains.com/issue/KT-74041) Build Tools API: Lower level or remove compiler arguments log

### Tools. CLI

- [`KT-74663`](https://youtrack.jetbrains.com/issue/KT-74663) kotlinc-js CLI: not providing -ir-output-dir results in NullPointerException
- [`KT-73606`](https://youtrack.jetbrains.com/issue/KT-73606) Provide a unified interface for managing the reporting of compiler warnings
- [`KT-73007`](https://youtrack.jetbrains.com/issue/KT-73007) Add stable compiler argument -jvm-default instead of -Xjvm-default
- [`KT-73595`](https://youtrack.jetbrains.com/issue/KT-73595) Kapt.use.k2=true is ignored silently for language-version 1.9 or less
- [`KT-18783`](https://youtrack.jetbrains.com/issue/KT-18783) Option to treat a specific compiler warning as an error
- [`KT-24746`](https://youtrack.jetbrains.com/issue/KT-24746) Provide ability to exclude specific warnings from compiler option Werror (all warnings as errors)
- [`KT-75641`](https://youtrack.jetbrains.com/issue/KT-75641) kotlinc -help spends almost 1 second on Usage.render()
- [`KT-75043`](https://youtrack.jetbrains.com/issue/KT-75043) Migrate Metadata compilation pipeline to the phased structure
- [`KT-75113`](https://youtrack.jetbrains.com/issue/KT-75113) TEST_ONLY LanguageFeature doesn't abort the compilation
- [`KT-73324`](https://youtrack.jetbrains.com/issue/KT-73324) Use phased CLI infrastructure in JVM tests

### Tools. Commonizer

- [`KT-74623`](https://youtrack.jetbrains.com/issue/KT-74623) Drop metadata version check from KLIB commonizer

### Tools. Compiler Plugin API

- [`KT-74640`](https://youtrack.jetbrains.com/issue/KT-74640) [FIR] Support setting `source` in declaration generators

### Tools. Compiler Plugins

#### Fixes

- [`KT-73367`](https://youtrack.jetbrains.com/issue/KT-73367) Migrate compose plugin to new IR parameter API
- [`KT-75614`](https://youtrack.jetbrains.com/issue/KT-75614) PowerAssert: handling of exceptions doesn't work inside assert function
- [`KT-75264`](https://youtrack.jetbrains.com/issue/KT-75264) PowerAssert: the diagram for try-catch with boolean expressions isn't clear
- [`KT-75663`](https://youtrack.jetbrains.com/issue/KT-75663) PowerAssert: 'contains' result for strings is displayed under the first parameter instead of 'in'
- [`KT-73897`](https://youtrack.jetbrains.com/issue/KT-73897) PowerAssert: Implicit argument detection is brittle in a number of cases
- [`KT-74315`](https://youtrack.jetbrains.com/issue/KT-74315) Kotlin Lombok: "Unresolved reference" on generating `@Builder` for static inner class where outer class is also using `@Builder`
- [`KT-72172`](https://youtrack.jetbrains.com/issue/KT-72172) File Leak occurring in Kotlin Daemon
- [`KT-75159`](https://youtrack.jetbrains.com/issue/KT-75159) Compose: Missing 'FunctionKeyMeta' annotation on lamdas declared in non-composable function
- [`KT-72877`](https://youtrack.jetbrains.com/issue/KT-72877) Power-Assert should provide IrExpression transformation API
- [`KT-61584`](https://youtrack.jetbrains.com/issue/KT-61584) [atomicfu]: prohibit declaration of AtomicReference to the value class in the compiler plugin
- [`KT-73871`](https://youtrack.jetbrains.com/issue/KT-73871) PowerAssert: Comparison via operator overload results in confusing diagram
- [`KT-73898`](https://youtrack.jetbrains.com/issue/KT-73898) PowerAssert: Operator calls with multiple receivers incorrectly aligned
- [`KT-73870`](https://youtrack.jetbrains.com/issue/KT-73870) PowerAssert: Object should not be displayed

### Tools. Gradle

#### New Features

- [`KT-73418`](https://youtrack.jetbrains.com/issue/KT-73418) Gradle '--warning-mode' value should affect Gradle plugin diagnostics
- [`KT-73285`](https://youtrack.jetbrains.com/issue/KT-73285) Integrate Gradle Problem API with KGP diagnostics
- [`KT-61649`](https://youtrack.jetbrains.com/issue/KT-61649) Add Gradle compiler option for jvm-default
- [`KT-68659`](https://youtrack.jetbrains.com/issue/KT-68659) Collect reported Kotlin Gradle Plugin diagnostics into one HTML/Text file report instead of writing it to log
- [`KT-73906`](https://youtrack.jetbrains.com/issue/KT-73906) Improve ToolingDiagnostic CLI rendering

#### Fixes

- [`KT-75107`](https://youtrack.jetbrains.com/issue/KT-75107) Add Gradle property to use new FIR IC runner
- [`KT-75188`](https://youtrack.jetbrains.com/issue/KT-75188) Groovy plugin breaks access to internal members of test friendPaths classes in kotlin compilation
- [`KT-70252`](https://youtrack.jetbrains.com/issue/KT-70252) Gradle: remove Intellij dependencies from KGP runtime
- [`KT-75921`](https://youtrack.jetbrains.com/issue/KT-75921) Make Swift Export available by default
- [`KT-74333`](https://youtrack.jetbrains.com/issue/KT-74333) improve ToolingDiagnosticBuilder
- [`KT-73683`](https://youtrack.jetbrains.com/issue/KT-73683) Compile against Gradle API 8.12
- [`KT-75187`](https://youtrack.jetbrains.com/issue/KT-75187) Make KotlinToolingDiagnostics internal
- [`KT-75568`](https://youtrack.jetbrains.com/issue/KT-75568) Do not use env variables registered as CC inputs
- [`KT-74277`](https://youtrack.jetbrains.com/issue/KT-74277) KGP / FreeBSD: "TargetSupportException: Unknown operating system: FreeBSD" during the build
- [`KT-73842`](https://youtrack.jetbrains.com/issue/KT-73842) Gradle: AGP failing tests with "Failed to calculate the value of property 'generalConfigurationMetrics'" using KGP
- [`KT-75262`](https://youtrack.jetbrains.com/issue/KT-75262) Gradle test-fixtures plugin apply order breaks the project
- [`KT-75277`](https://youtrack.jetbrains.com/issue/KT-75277) FUS statistics: 'java.lang.IllegalStateException: The value for this property cannot be changed any further' exception is thrown during project import
- [`KT-75164`](https://youtrack.jetbrains.com/issue/KT-75164) Run Gradle incremental compilation tests with FIR runner
- [`KT-59632`](https://youtrack.jetbrains.com/issue/KT-59632) KotlinCompileTool.setSource() should replace existing sources
- [`KT-72694`](https://youtrack.jetbrains.com/issue/KT-72694) Accessing Task.project during execution is being deprecated in Gradle 8.12
- [`KT-75026`](https://youtrack.jetbrains.com/issue/KT-75026) Corrupted NonSynchronizedMetricsContainer in parallel Gradle build
- [`KT-66133`](https://youtrack.jetbrains.com/issue/KT-66133) Finalize resolution strategy for resources and remove the one that is unused
- [`KT-64991`](https://youtrack.jetbrains.com/issue/KT-64991) Change deprecation level to error for KotlinCompilation.source
- [`KT-70620`](https://youtrack.jetbrains.com/issue/KT-70620) Raise to error deprecation for KotlinCompilationOutput#resourcesDirProvider
- [`KT-73849`](https://youtrack.jetbrains.com/issue/KT-73849) Categorize ToolingDiagnostics
- [`KT-74462`](https://youtrack.jetbrains.com/issue/KT-74462) Flaky Kotlin Gradle Plugin Tests: IsInIdeaEnvironmentValueSource$Inject not found
- [`KT-72329`](https://youtrack.jetbrains.com/issue/KT-72329) Consider bumping apiVersion for projects with compatibility setup
- [`KT-74415`](https://youtrack.jetbrains.com/issue/KT-74415) Make composeCompiler.includeSourceInformation true by default
- [`KT-74772`](https://youtrack.jetbrains.com/issue/KT-74772) ToolingDiagnostic: title is not displayed on Windows
- [`KT-74485`](https://youtrack.jetbrains.com/issue/KT-74485) BuildFinishedListenerService is not thread-safe
- [`KT-74717`](https://youtrack.jetbrains.com/issue/KT-74717) Test publication with dependency constraints
- [`KT-74551`](https://youtrack.jetbrains.com/issue/KT-74551) Improve KGP-IT withDebug for tests with environment variables
- [`KT-74639`](https://youtrack.jetbrains.com/issue/KT-74639) Executable binaries for JVM test cannot be created unless an additional suffix is set in Groovy
- [`KT-72187`](https://youtrack.jetbrains.com/issue/KT-72187) Gradle tests are using incorrect Kotlin/Native distribution
- [`KT-57653`](https://youtrack.jetbrains.com/issue/KT-57653) Explicit API mode is not enabled when free compiler arguments are specified in Gradle project
- [`KT-51378`](https://youtrack.jetbrains.com/issue/KT-51378) Gradle 'buildSrc' compilation fails when newer version of Kotlin plugin is added to the build script classpath

### Tools. Gradle. Compiler plugins

- [`KT-58009`](https://youtrack.jetbrains.com/issue/KT-58009) `BaseKapt.annotationProcessorOptionProviders` should be a `List<CommandLineArgumentProvider>` instead of `List<Any>`
- [`KT-61928`](https://youtrack.jetbrains.com/issue/KT-61928) Clarify parameter types in KaptArguments and KaptJavacOption

### Tools. Gradle. JS

- [`KT-75863`](https://youtrack.jetbrains.com/issue/KT-75863) Wasm/JS: Deprecate phantom-js for Karma
- [`KT-75485`](https://youtrack.jetbrains.com/issue/KT-75485) KJS: "Module not found: Error: Can't resolve 'style-loader' and 'css-loader'" in 2.1.20-RC
- [`KT-74869`](https://youtrack.jetbrains.com/issue/KT-74869) KJS: `jsBrowserProductionWebpack` does not minify output with 2.1.20-Beta2
- [`KT-74859`](https://youtrack.jetbrains.com/issue/KT-74859) Gradle configuration cache issues related to RootPackageJsonTask
- [`KT-70357`](https://youtrack.jetbrains.com/issue/KT-70357) Remove JS/Dce deprecated Gradle DSL

### Tools. Gradle. Multiplatform

#### New Features

- [`KT-60623`](https://youtrack.jetbrains.com/issue/KT-60623) Deprecate `publishAllLibraryVariants` in kotlin-android

#### Fixes

- [`KT-75605`](https://youtrack.jetbrains.com/issue/KT-75605) Dependency resolution fails in commonTest/nativeTest source sets for KMP module when depending on another project due to missing PSM
- [`KT-74727`](https://youtrack.jetbrains.com/issue/KT-74727) Dependency resolve from a single target KMP module to another kmp module fails on non-found PSM
- [`KT-75512`](https://youtrack.jetbrains.com/issue/KT-75512) Maven-publish: ArtifactId is not correct  in`pom` file with customized `withXml`
- [`KT-68015`](https://youtrack.jetbrains.com/issue/KT-68015) Remove legacy KMP flags
- [`KT-71634`](https://youtrack.jetbrains.com/issue/KT-71634) KGP: Remove KotlinTarget.useDisambiguationClassifierAsSourceSetNamePrefix and overrideDisambiguationClassifierOnIdeImport
- [`KT-72203`](https://youtrack.jetbrains.com/issue/KT-72203) Swift Export: Unclear failure for invalid module name
- [`KT-66542`](https://youtrack.jetbrains.com/issue/KT-66542) Gradle: JVM target with `withJava()` produces a deprecation warning
- [`KT-74278`](https://youtrack.jetbrains.com/issue/KT-74278) KSP tasks don't trigger a K/N distribution downloading
- [`KT-75161`](https://youtrack.jetbrains.com/issue/KT-75161) Deprecate commonization parameters in KGP with an error
- [`KT-71608`](https://youtrack.jetbrains.com/issue/KT-71608) Remove 'android()' target
- [`KT-74669`](https://youtrack.jetbrains.com/issue/KT-74669) Executable binaries for JVM: a jar generated by jvmJar task isn't added to the build/install/testAppName/lib directory
- [`KT-61817`](https://youtrack.jetbrains.com/issue/KT-61817) Remove support for Legacy Metadata Compilation with support of Compatibility Metadata Variant
- [`KT-69200`](https://youtrack.jetbrains.com/issue/KT-69200) Module 'intellij.kotlin.gradle.multiplatformTests' transitively depends on K1/K2 implementation
- [`KT-62643`](https://youtrack.jetbrains.com/issue/KT-62643) Increase DeprecationLevel to 'Error' on deprecated 'ExtrasProperty.kt' (Kotlin 2.2)
- [`KT-71454`](https://youtrack.jetbrains.com/issue/KT-71454) Remove not compatible with Project Isolation PomDependenciesRewriter
- [`KT-70493`](https://youtrack.jetbrains.com/issue/KT-70493) Improve gray-box testing experience in KGP-IT
- [`KT-73536`](https://youtrack.jetbrains.com/issue/KT-73536) Enable kmp isolated projects support for kotlin-test and patch PSM.json
- [`KT-58231`](https://youtrack.jetbrains.com/issue/KT-58231) Kotlin Gradle Plugin: set deprecation level to Error for KotlinTarget.useDisambiguationClassifierAsSourceSetNamePrefix and overrideDisambiguationClassifierOnIdeImport

### Tools. Gradle. Native

- [`KT-75171`](https://youtrack.jetbrains.com/issue/KT-75171) Provide custom freeCompilerArgs to Swift Export's link task
- [`KT-74591`](https://youtrack.jetbrains.com/issue/KT-74591) HostManager.isMingw isLinux and isMac are not accessible in groovy scripts
- [`KT-71069`](https://youtrack.jetbrains.com/issue/KT-71069) Remove `konanVersion ` from CInteropProcess
- [`KT-65692`](https://youtrack.jetbrains.com/issue/KT-65692) Remove Kotlin Native Performance plugin
- [`KT-74403`](https://youtrack.jetbrains.com/issue/KT-74403) :commonizeNativeDistribution fails when configured native targets cannot be built on machine

### Tools. Gradle. Xcode

- [`KT-66262`](https://youtrack.jetbrains.com/issue/KT-66262) Deprecate and remove support for bitcode embedding from the Kotlin Gradle plugin

### Tools. Incremental Compile

- [`KT-62555`](https://youtrack.jetbrains.com/issue/KT-62555) Wrong ABI fingerprint for inline function containing a lambda
- [`KT-75155`](https://youtrack.jetbrains.com/issue/KT-75155) Split HistoryFileJvmIncrementalCompilerRunner and the current one
- [`KT-74628`](https://youtrack.jetbrains.com/issue/KT-74628) Incremental compilation runner does not check compiler exit code before mapping sources to classes
- [`KT-75276`](https://youtrack.jetbrains.com/issue/KT-75276) Test IC issues with first-round failures that might be fixed by Fir runner

### Tools. JPS

- [`KT-76461`](https://youtrack.jetbrains.com/issue/KT-76461) Fix "compilation of typealias does not check for clashes" for JPS
- [`KT-73379`](https://youtrack.jetbrains.com/issue/KT-73379) Review the usage of JavaBuilder.IS_ENABLED inside the KotlinBuilder JSP builder

### Tools. Kapt

- [`KT-75202`](https://youtrack.jetbrains.com/issue/KT-75202) K2 kapt: mapped type class literal is converted incorrectly
- [`KT-70797`](https://youtrack.jetbrains.com/issue/KT-70797) Remove obsolete K2 kapt implementation based on Analysis API
- [`KT-64385`](https://youtrack.jetbrains.com/issue/KT-64385) K2: Enable K2 KAPT by default

### Tools. Maven

- [`KT-73012`](https://youtrack.jetbrains.com/issue/KT-73012) Migrate Kotlin Maven plugin to the Build Tools API
- [`KT-43894`](https://youtrack.jetbrains.com/issue/KT-43894) Maven, Windows: error "RuntimeException: Could not find installation home path"
- [`KT-61285`](https://youtrack.jetbrains.com/issue/KT-61285) Remove multiplatform "support" from Maven Plugin

### Tools. Performance benchmarks

- [`KT-75563`](https://youtrack.jetbrains.com/issue/KT-75563) Fix crash on kotlin compiler server user project related to performance measurements

### Tools. REPL

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
- [`KT-74004`](https://youtrack.jetbrains.com/issue/KT-74004) "Evaluate expression" fails in scripts

### Tools. Wasm

- [`KT-74840`](https://youtrack.jetbrains.com/issue/KT-74840) Wasm: Binaryen setup per project
- [`KT-73398`](https://youtrack.jetbrains.com/issue/KT-73398) K/Wasm: Separate from JS NPM infrastructure
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