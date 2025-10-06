## 2.3.0-Beta1

### Analysis API

- [`KT-80303`](https://youtrack.jetbrains.com/issue/KT-80303) Move `:native:analysis-api-klib-reader` to `:libraries:tools`

### Analysis API. Code Compilation

- [`KT-80227`](https://youtrack.jetbrains.com/issue/KT-80227) Support unnamed context parameters in evaluation
- [`KT-78554`](https://youtrack.jetbrains.com/issue/KT-78554) K2 IDE / Kotlin Debugger: ISE “No override for FUN IR_EXTERNAL_DECLARATION_STUB” on calling toString() for local class instance during evaluation
- [`KT-73201`](https://youtrack.jetbrains.com/issue/KT-73201) K2 IDE: Error while evaluating expressions with local classes

### Analysis API. FIR

- [`KT-46375`](https://youtrack.jetbrains.com/issue/KT-46375) Analysis API: Support cross-file class redeclaration checks using indices
- [`KT-80471`](https://youtrack.jetbrains.com/issue/KT-80471) Analysis API: Deduplicate equivalent call candidates in `resolveToCallCandidates`
- [`KT-79653`](https://youtrack.jetbrains.com/issue/KT-79653) [Analysis API] ContextCollector: BODY context of enum classes doesn't contain enum entries
- [`KT-75858`](https://youtrack.jetbrains.com/issue/KT-75858) K2 AA: False positive 'property must be initialized' on incremental analysis with 'field' usage and semicolon in setter
- [`KT-80231`](https://youtrack.jetbrains.com/issue/KT-80231) AnnotationArgumentsStateKeepers doesn't restore the initial annotation in some cases
- [`KT-80233`](https://youtrack.jetbrains.com/issue/KT-80233) Pull mutation out of AnnotationArgumentsStateKeepers
- [`KT-71466`](https://youtrack.jetbrains.com/issue/KT-71466) `LLFirBuiltinsSessionFactory` uses `createCompositeSymbolProvider`
- [`KT-76432`](https://youtrack.jetbrains.com/issue/KT-76432) JavaClassUseSiteMemberScope: Expected FirResolvedTypeRef with ConeKotlinType but was FirUserTypeRefImpl

### Analysis API. Infrastructure

- [`KT-80717`](https://youtrack.jetbrains.com/issue/KT-80717) Support IntelliJ Bazel build in the Kotlin Coop development mode

### Analysis API. Light Classes

- [`KT-80656`](https://youtrack.jetbrains.com/issue/KT-80656) Duplicate no-args constructor in PSI
- [`KT-60490`](https://youtrack.jetbrains.com/issue/KT-60490) Symbol Light Classes: Property accessors from a delegated interface don't present in the delegating class
- [`KT-79689`](https://youtrack.jetbrains.com/issue/KT-79689) SymbolLightClassForClassLike.toString() causes PSI tree loading
- [`KT-79012`](https://youtrack.jetbrains.com/issue/KT-79012) Add a high-level overview of light classes

### Analysis API. Providers and Caches

- [`KT-78882`](https://youtrack.jetbrains.com/issue/KT-78882) K2 AA: Calling containingSymbol on getProgressionLastElement causes exception
- [`KT-58325`](https://youtrack.jetbrains.com/issue/KT-58325) Analysis API: Combine `LLKotlinStubBasedLibrarySymbolProvider`s in session dependencies (optimization)
- [`KT-77825`](https://youtrack.jetbrains.com/issue/KT-77825) Analysis API: `CheckersComponent` consumes a lot of memory while being unused in LL FIR sessions
- [`KT-76526`](https://youtrack.jetbrains.com/issue/KT-76526) Incorrect built-in module is provided for non-JVM sources in Standalone
- [`KT-62549`](https://youtrack.jetbrains.com/issue/KT-62549) Analysis API: Cache callables in combined Kotlin symbol providers
- [`KT-70721`](https://youtrack.jetbrains.com/issue/KT-70721) LL FIR: investigate possibility of moving `LLFirFirClassByPsiClassProvider . getClassByPsiClass (PsiClass)`  to symbol providers
- [`KT-72998`](https://youtrack.jetbrains.com/issue/KT-72998) Analysis API: Introduce `getClassLikeSymbolByPsi` to LL FIR symbol providers

### Analysis API. Standalone

- [`KT-80573`](https://youtrack.jetbrains.com/issue/KT-80573) Potential performance issue on class ID computation
- [`KT-80559`](https://youtrack.jetbrains.com/issue/KT-80559) Try to optimize KotlinStandaloneDeclarationProviderFactory startup for tests
- [`KT-71706`](https://youtrack.jetbrains.com/issue/KT-71706) Analysis API Standalone: `StandaloneProjectFactory.createSearchScopeByLibraryRoots` creates inefficient file-based search scopes

### Analysis API. Stubs and Decompilation

#### Performance Improvements

- [`KT-77097`](https://youtrack.jetbrains.com/issue/KT-77097) Support `ReplaceWith` deprecation annotation argument via stubs

#### Fixes

- [`KT-77082`](https://youtrack.jetbrains.com/issue/KT-77082) StackOverflowError in CreateFreshTypeVariableSubstitutorStage.shouldBeFlexible
- [`KT-80798`](https://youtrack.jetbrains.com/issue/KT-80798) Improve stubs tests coverage
- [`KT-75318`](https://youtrack.jetbrains.com/issue/KT-75318) Read context parameter fields from metadata in CallableClsStubBuilder
- [`KT-77874`](https://youtrack.jetbrains.com/issue/KT-77874) AA disagrees with the compiler on descriptions of context parameters from binaries in messages for context argument ambiguity errors
- [`KT-80350`](https://youtrack.jetbrains.com/issue/KT-80350) Drop K1 decompiler
- [`KT-80276`](https://youtrack.jetbrains.com/issue/KT-80276) Implement native coping for stubs
- [`KT-79780`](https://youtrack.jetbrains.com/issue/KT-79780) Decompiled MultifileClass has Facade kind
- [`KT-79398`](https://youtrack.jetbrains.com/issue/KT-79398) isClsStubCompiledToJvmDefaultImplementation flag is inconsistent for compiled and decompiled stubs
- [`KT-79798`](https://youtrack.jetbrains.com/issue/KT-79798) Prettify stub usages in LL stub-based deserializer
- [`KT-78949`](https://youtrack.jetbrains.com/issue/KT-78949) AbstractLLStubBasedResolutionTest: tests against real stub-based files
- [`KT-80251`](https://youtrack.jetbrains.com/issue/KT-80251) Inconsistent decompiled and compiled stub for properties with an initializer and a delegate
- [`KT-74547`](https://youtrack.jetbrains.com/issue/KT-74547) Implement decompiler for K2
- [`KT-79555`](https://youtrack.jetbrains.com/issue/KT-79555) Move KotlinFileStubImpl serialization/deserialization to the Analysis API
- [`KT-79487`](https://youtrack.jetbrains.com/issue/KT-79487) "null DefinitelyNotNullType for 'T'" from decompiler
- [`KT-60764`](https://youtrack.jetbrains.com/issue/KT-60764) Stub Builder: fix differences between K1 and K2 stub building on decompiled files
- [`KT-79484`](https://youtrack.jetbrains.com/issue/KT-79484) An empty enum class with a member decompiles with a synthetic error
- [`KT-79730`](https://youtrack.jetbrains.com/issue/KT-79730) Decompiled files have an extra `Kt` suffix
- [`KT-79483`](https://youtrack.jetbrains.com/issue/KT-79483) data modifier is not present on object modifier
- [`KT-75398`](https://youtrack.jetbrains.com/issue/KT-75398) Local classes from scripts have ClassId in stubs
- [`KT-79412`](https://youtrack.jetbrains.com/issue/KT-79412) Context parameters with type annotations cause inconsistency errors while building stubs

### Analysis API. Surface

#### New Features

- [`KT-64340`](https://youtrack.jetbrains.com/issue/KT-64340) Analysis API: no way to get a type of vararg parameter
- [`KT-68387`](https://youtrack.jetbrains.com/issue/KT-68387) AA: provide context for type approximations

#### Performance Improvements

- [`KT-80713`](https://youtrack.jetbrains.com/issue/KT-80713) Optimize KaDeclarationSymbol#visibility for class-like symbols
- [`KT-79097`](https://youtrack.jetbrains.com/issue/KT-79097) KaFirNamedFunctionSymbol#isSuspend shouldn't trigger resolution
- [`KT-79095`](https://youtrack.jetbrains.com/issue/KT-79095) isOverride shouldn't trigger resolution if not compiler plugins present

#### Fixes

- [`KT-78093`](https://youtrack.jetbrains.com/issue/KT-78093) Add bridges for context parameters
- [`KT-79328`](https://youtrack.jetbrains.com/issue/KT-79328) K2 AA, isUsedAsExpression: Unhandled Non-KtExpression parent of KtExpression: class org.jetbrains.kotlin.psi.KtImportDirective
- [`KT-80366`](https://youtrack.jetbrains.com/issue/KT-80366) IllegalStateException from KaFirStopWorldCacheCleaner
- [`KT-80274`](https://youtrack.jetbrains.com/issue/KT-80274) Merge AbstractMultiModuleSymbolByPsiTest to AbstractSymbolByPsiTest
- [`KT-80352`](https://youtrack.jetbrains.com/issue/KT-80352) KaBaseResolutionScope.contains(PsiElement) always returns false for Android light classes (e.g. synthetic R.java classes)
- [`KT-80234`](https://youtrack.jetbrains.com/issue/KT-80234) Incorrect value of `isActual` for the implicitly `actual` constructor of annotation class
- [`KT-80178`](https://youtrack.jetbrains.com/issue/KT-80178) Incorrect modality for an abstract interface function with a redundant `open` modifier
- [`KT-79129`](https://youtrack.jetbrains.com/issue/KT-79129) [Analysis API] `KaFe10TypeCreator.buildClassType` cannot build builtin types by class ids
- [`KT-79143`](https://youtrack.jetbrains.com/issue/KT-79143) AA: `argumentMapping` contains an expression that is not an argument
- [`KT-59857`](https://youtrack.jetbrains.com/issue/KT-59857) KaExpressionTypeProvider#returnType shouldn't throw an exception for class like declarations
- [`KT-79667`](https://youtrack.jetbrains.com/issue/KT-79667) Enable resolve on java record components in standalone mode
- [`KT-73050`](https://youtrack.jetbrains.com/issue/KT-73050) `KaFirSymbolRelationProvider#expectsForActual`: suspicius logic for KaReceiverParameterSymbol
- [`KT-78904`](https://youtrack.jetbrains.com/issue/KT-78904) KaBaseWriteActionStartedChecker throws when no additional WA was done
- [`KT-79281`](https://youtrack.jetbrains.com/issue/KT-79281) Add KDoc to `KaTypePointer#restore`
- [`KT-78597`](https://youtrack.jetbrains.com/issue/KT-78597) KaUseSiteVisibilityChecker returns false for internal functions exposed via implicit receiver
- [`KT-71705`](https://youtrack.jetbrains.com/issue/KT-71705) FIR api impl: Postfix increment expression's `expressionType` is Unit when incrementing array element
- [`KT-75057`](https://youtrack.jetbrains.com/issue/KT-75057) Analysis API: Reference to object through typealias in invoke operator call leads to original type

### Backend. Native. Debug

- [`KT-79848`](https://youtrack.jetbrains.com/issue/KT-79848) Flaky debugger tests in opt.debug/cache.*/GC.CMS/GC.sch.ad/alloc.custom configuration

### Backend. Wasm

#### New Features

- [`KT-59032`](https://youtrack.jetbrains.com/issue/KT-59032) Support instantiation of annotation classes on WASM

#### Fixes

- [`KT-69621`](https://youtrack.jetbrains.com/issue/KT-69621) K/Wasm: Consider enabling support for KClass.qualifiedName by default
- [`KT-80018`](https://youtrack.jetbrains.com/issue/KT-80018) K/Wasm: exceptions don't work properly in JavaScriptCore (vm inside Safari, WebKit)
- [`KT-66072`](https://youtrack.jetbrains.com/issue/KT-66072) K/Wasm: improve how exceptions work in JS interop
- [`KT-80106`](https://youtrack.jetbrains.com/issue/KT-80106) devServer in Kotlin/Wasm overwrites defaults, causing missing static paths
- [`KT-80210`](https://youtrack.jetbrains.com/issue/KT-80210) Wasm: "Unexpected non-external class: kotlin.Nothing" caused by JsExport with JsPromise
- [`KT-80555`](https://youtrack.jetbrains.com/issue/KT-80555) WASM IC: Can't link symbol on kotlinx.coroutines on fresh master
- [`KT-80415`](https://youtrack.jetbrains.com/issue/KT-80415) WasmJs Number Elvis Operator Crash
- [`KT-76509`](https://youtrack.jetbrains.com/issue/KT-76509) WasmJS: ReferenceError: Temporal is not defined caused by "Redundant reference to unused external results"
- [`KT-79317`](https://youtrack.jetbrains.com/issue/KT-79317) [Wasm] Do not throw CCE for ExcludedFromCodegen declarations
- [`KT-78036`](https://youtrack.jetbrains.com/issue/KT-78036) K/Wasm: generate a message with "expected" and "actual" types in case of CCE

### Compiler

#### New Features

- [`KT-77676`](https://youtrack.jetbrains.com/issue/KT-77676) K/N: enable typechecks and the casts optimization pass in debug mode by default
- [`KT-80768`](https://youtrack.jetbrains.com/issue/KT-80768) Warning on overloading by a superset of context parameters in class context
- [`KT-79185`](https://youtrack.jetbrains.com/issue/KT-79185) Support for local type aliases
- [`KT-80461`](https://youtrack.jetbrains.com/issue/KT-80461) K2: false positive NO_ELSE_IN_WHEN for complex sealed hierarchy
- [`KT-79380`](https://youtrack.jetbrains.com/issue/KT-79380) Native: add performance measurement for the rest of backend phases
- [`KT-79381`](https://youtrack.jetbrains.com/issue/KT-79381) Native: add performance measurement of LLVM phases
- [`KT-80222`](https://youtrack.jetbrains.com/issue/KT-80222) Implement the prohibition of always-false `is` checks for definitely incompatible types
- [`KT-79295`](https://youtrack.jetbrains.com/issue/KT-79295) Parse and build raw FIR from new short and full forms of positional destructuring with square brackets
- [`KT-74810`](https://youtrack.jetbrains.com/issue/KT-74810) Support typealiased/mapped Java types in unused return value checker
- [`KT-71244`](https://youtrack.jetbrains.com/issue/KT-71244) Incorporate existing `@CheckReturnValue` annotation(s) into Kotlin's unused return value checker
- [`KT-79922`](https://youtrack.jetbrains.com/issue/KT-79922) Record 'MustUse/ExplicitlyIgnorable' state for overrides even in disabled RVC mode
- [`KT-79920`](https://youtrack.jetbrains.com/issue/KT-79920) Store 'Explicitly ignorable' state of function/property in the metadata
- [`KT-79690`](https://youtrack.jetbrains.com/issue/KT-79690) Implement a USELESS_ELVIS_LEFT_IS_NULL with elvis expression
- [`KT-79296`](https://youtrack.jetbrains.com/issue/KT-79296) Implement/adapt diagnostics for new destructuring
- [`KT-79298`](https://youtrack.jetbrains.com/issue/KT-79298) Report errors on new destructuring syntax in K1

#### Performance Improvements

- [`KT-81340`](https://youtrack.jetbrains.com/issue/KT-81340) K/N: severe compilation time degradation after turning on casts optimization pass
- [`KT-80554`](https://youtrack.jetbrains.com/issue/KT-80554) Kotlin/Native: investigate performance hit from always-on llvm pass profiling
- [`KT-80370`](https://youtrack.jetbrains.com/issue/KT-80370) Add NO_INLINE attribute to some of runtime functions
- [`KT-80167`](https://youtrack.jetbrains.com/issue/KT-80167) K/N: condense the nodes and edges in DevirtualizationAnalysis constraint graph
- [`KT-79535`](https://youtrack.jetbrains.com/issue/KT-79535) Revert incorrect SAM conversion enhancements brought to K2

#### Fixes

- [`KT-81257`](https://youtrack.jetbrains.com/issue/KT-81257) Native: "Unexpected boolean predicate" when generating 'static_cache'
- [`KT-80864`](https://youtrack.jetbrains.com/issue/KT-80864) K2: Missing `Val cannot be reassigned` diagnostic for Java final fields (crashes in runtime with `IllegalAccessError`)
- [`KT-75215`](https://youtrack.jetbrains.com/issue/KT-75215) KDoc: references from `@param` tag are rendered as plain text
- [`KT-79887`](https://youtrack.jetbrains.com/issue/KT-79887) K2 Compiler Internal Error in 'FirFakeOverrideGenerator.checkStatusIsResolved' Method
- [`KT-78125`](https://youtrack.jetbrains.com/issue/KT-78125) false-negative shadowed contextual overload warning on local declarations
- [`KT-79274`](https://youtrack.jetbrains.com/issue/KT-79274) Frontend implementation of name-based destructuring
- [`KT-81057`](https://youtrack.jetbrains.com/issue/KT-81057) Wrong handling of boxing during redundant casts optimization
- [`KT-80285`](https://youtrack.jetbrains.com/issue/KT-80285) IJ monorepo: broken compilation after 2.2.20-RC update
- [`KT-81015`](https://youtrack.jetbrains.com/issue/KT-81015) Mark nested typealiases as stable for 2.3
- [`KT-81064`](https://youtrack.jetbrains.com/issue/KT-81064) Wrong safe call null check handling during redundant casts optimization
- [`KT-80744`](https://youtrack.jetbrains.com/issue/KT-80744) Kotlin failure on lambda with type parameter
- [`KT-78280`](https://youtrack.jetbrains.com/issue/KT-78280) Implement the sourceless `KtDiagnostic`s
- [`KT-78819`](https://youtrack.jetbrains.com/issue/KT-78819) K2: False positive ABSTRACT_MEMBER_NOT_IMPLEMENTED in KJK hierarchy
- [`KT-81018`](https://youtrack.jetbrains.com/issue/KT-81018) ISE "IR class for Foo not found" on missing dependency when lowering SAM constructor
- [`KT-80936`](https://youtrack.jetbrains.com/issue/KT-80936) NON_PUBLIC_CALL_FROM_PUBLIC_INLINE : `@PublishedApi` doesn't work for fun interfaces
- [`KT-75748`](https://youtrack.jetbrains.com/issue/KT-75748) StackOverflowError when reading array from metadata annotations
- [`KT-80400`](https://youtrack.jetbrains.com/issue/KT-80400) K2: AbstractMethodError on fun interface implementation inheriting from an interface compiled with -jvm-default=disable
- [`KT-80940`](https://youtrack.jetbrains.com/issue/KT-80940) K2: Exception in FIR2IR with AnnotationTarget.TYPE with self-annotated non-vararg default argument and usage in child module
- [`KT-80538`](https://youtrack.jetbrains.com/issue/KT-80538) KaFirDiagnostic.EmptyRange doesn't work in most of the cases
- [`KT-80524`](https://youtrack.jetbrains.com/issue/KT-80524) Class is not abstract and does not implement abstract member when compiling with kotlinc-jklib
- [`KT-80597`](https://youtrack.jetbrains.com/issue/KT-80597) Apply fix for CVE-2024-7254 to our fork of protobuf 2.6.1
- [`KT-80849`](https://youtrack.jetbrains.com/issue/KT-80849) K2: `ConstValueProviderImpl` doesn't distinguish files with same name and package
- [`KT-80602`](https://youtrack.jetbrains.com/issue/KT-80602) Exhaustiveness checker improvements for 2.3
- [`KT-80735`](https://youtrack.jetbrains.com/issue/KT-80735) Support || return/throw shortcut in unsed return value checker
- [`KT-79651`](https://youtrack.jetbrains.com/issue/KT-79651) Report a warning about an unused return value only on the function name
- [`KT-80719`](https://youtrack.jetbrains.com/issue/KT-80719) False positive: "Redundant visibility modifier": when overriding protected methods as "public"
- [`KT-80711`](https://youtrack.jetbrains.com/issue/KT-80711) IC Native: NPE during link on ktor
- [`KT-80795`](https://youtrack.jetbrains.com/issue/KT-80795) Wong type cast is added for IMPLICIT_COERCION_TO_UNIT
- [`KT-80434`](https://youtrack.jetbrains.com/issue/KT-80434) K2: DSL marker doesn't work with lambda fields
- [`KT-80383`](https://youtrack.jetbrains.com/issue/KT-80383) Getter without a body is allowed on a property with an explicit backing field
- [`KT-80455`](https://youtrack.jetbrains.com/issue/KT-80455) K2: StackOverflowError in when exhaustiveness checker on red code
- [`KT-72862`](https://youtrack.jetbrains.com/issue/KT-72862) [Native caches] Umbrella for failing codegen/box tests for corner cases in synthetic accessors
- [`KT-20278`](https://youtrack.jetbrains.com/issue/KT-20278) NO_TYPE_ARGUMENTS_ON_RHS: Confusing diagnostic for inner class of generic outer class
- [`KT-80164`](https://youtrack.jetbrains.com/issue/KT-80164) Move name generation for unnamed context parameters to frontend
- [`KT-78112`](https://youtrack.jetbrains.com/issue/KT-78112) RETURN_VALUE_NOT_USED is reported for local function even if it isn't marked with annotation in CHECKER mode
- [`KT-48311`](https://youtrack.jetbrains.com/issue/KT-48311) Incorrect LINENUMBER after if with a suspend call
- [`KT-80688`](https://youtrack.jetbrains.com/issue/KT-80688) Bad SourceDebugExtension caused by enhanced coroutines debugging
- [`KT-73851`](https://youtrack.jetbrains.com/issue/KT-73851) Native: compilation fails with ClassCastException with genericSafeCasts=true
- [`KT-77593`](https://youtrack.jetbrains.com/issue/KT-77593) Add a warning when `@IgnorableReturnValue` is inconsistent between expect/actual functions
- [`KT-78895`](https://youtrack.jetbrains.com/issue/KT-78895) Consider dropping isLocalInFunction and FirClassLikeDeclaration.isLocal
- [`KT-79386`](https://youtrack.jetbrains.com/issue/KT-79386) Confusing error message when named parameters are used for java method calls
- [`KT-80600`](https://youtrack.jetbrains.com/issue/KT-80600) K2: Private and final modifiers are allowed on setter of open delegated property
- [`KT-79783`](https://youtrack.jetbrains.com/issue/KT-79783) KDoc parser: Links aren't rendered if the line has an indent of 4 or more
- [`KT-77101`](https://youtrack.jetbrains.com/issue/KT-77101) Invoke on callable reference is considered ignorable
- [`KT-79923`](https://youtrack.jetbrains.com/issue/KT-79923) Remove lookup of `@IgnorableReturnValue` annotation from FirReturnValueOverrideChecker
- [`KT-80517`](https://youtrack.jetbrains.com/issue/KT-80517) Synthetic kotlin.Any members in data classes are missing `@MustUseReturnValue`
- [`KT-80194`](https://youtrack.jetbrains.com/issue/KT-80194) VAR_TYPE_MISMATCH_ON_OVERRIDE: doesn't mention the inferred type
- [`KT-80484`](https://youtrack.jetbrains.com/issue/KT-80484) K2: ClassCastException due to fake source for implicit lambda parameter (RedundantNullableChecker)
- [`KT-79979`](https://youtrack.jetbrains.com/issue/KT-79979) K2: ClassCastException when overriding extension property with delegation
- [`KT-80592`](https://youtrack.jetbrains.com/issue/KT-80592) `UninitializedPropertyAccessException` when anayzing annotations on members of anonymous classes
- [`KT-80399`](https://youtrack.jetbrains.com/issue/KT-80399) Anonymous Kotlin class incorrectly warns about deprecated java override despite '`@Deprecated`' annotation
- [`KT-79610`](https://youtrack.jetbrains.com/issue/KT-79610) Adding CocoaPod to Kotlin/Native MPP triggers IR serialization failure and commonizer errors
- [`KT-79866`](https://youtrack.jetbrains.com/issue/KT-79866) kotlinc 2.2.0 silently emits 'NonExistentClass' instead of reporting an error
- [`KT-78664`](https://youtrack.jetbrains.com/issue/KT-78664) False positive VARIABLE_NEVER_READ and ASSIGNED_VALUE_IS_NEVER_READ on function type variable with splited declaration and assignment
- [`KT-79496`](https://youtrack.jetbrains.com/issue/KT-79496) False positive "when must be exhaustive" in triangle interface/class hierarchy
- [`KT-79774`](https://youtrack.jetbrains.com/issue/KT-79774) KtDestructuringDeclaration.getLPar & getRPar are broken
- [`KT-79442`](https://youtrack.jetbrains.com/issue/KT-79442) "Multiple annotations of type kotlin.coroutines.jvm.internal.DebugMetadata": 2.2.0-Beta1 generates broken code with JVM default suspend methods in interfaces
- [`KT-80391`](https://youtrack.jetbrains.com/issue/KT-80391) K2: Only one context parameter is mentioned in the [NO_CONTEXT_ARGUMENT] diagnostic
- [`KT-79785`](https://youtrack.jetbrains.com/issue/KT-79785) ktypew:kotlin.collections.List already exists error using Swift Export
- [`KT-78879`](https://youtrack.jetbrains.com/issue/KT-78879) "Sealed types cannot be instantiated": Can't instantiate Java-defined sealed Class from Kotlin
- [`KT-80330`](https://youtrack.jetbrains.com/issue/KT-80330) K2: NPE at org.jetbrains.kotlin.fir.resolve.calls.FirCallResolver.createResolvedNamedReference
- [`KT-21598`](https://youtrack.jetbrains.com/issue/KT-21598) Extension is shadowed by member should not be reported when member is deprecated with HIDDEN level
- [`KT-79622`](https://youtrack.jetbrains.com/issue/KT-79622) FUNCTION_EXPECTED: Misleading 'expression cannot be invoked as a function' when inaccessible with private lambda
- [`KT-80255`](https://youtrack.jetbrains.com/issue/KT-80255) [EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION] can be attached to the receiver type of a functional type
- [`KT-79816`](https://youtrack.jetbrains.com/issue/KT-79816) Java Interfaces implemented by delegation have non-null return checks
- [`KT-80177`](https://youtrack.jetbrains.com/issue/KT-80177) Improve message of RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER in case of member extension
- [`KT-79770`](https://youtrack.jetbrains.com/issue/KT-79770) There is no RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER if the usage of fun is from inside the class
- [`KT-79430`](https://youtrack.jetbrains.com/issue/KT-79430) False positive EXTENSION_SHADOWED_BY_MEMBER on overridden member extension
- [`KT-62934`](https://youtrack.jetbrains.com/issue/KT-62934) Incorrect line mapping inside inline lambda after non-local return
- [`KT-79545`](https://youtrack.jetbrains.com/issue/KT-79545) K2: no error on crossinline lambda usage in anonymous object base constructor call
- [`KT-79643`](https://youtrack.jetbrains.com/issue/KT-79643) HAS_NEXT_FUNCTION_AMBIGUITY and NEXT_AMBIGUITY diagnostics are always ignored in favor of HAS_NEXT_FUNCTION_NONE_APPLICABLE and NEXT_NONE_APPLICABLE
- [`KT-79327`](https://youtrack.jetbrains.com/issue/KT-79327) Modifier 'private' is not applicable to 'value parameter' is reported for context parameters
- [`KT-76453`](https://youtrack.jetbrains.com/issue/KT-76453) K2 IDE: autocomplete freeze
- [`KT-77182`](https://youtrack.jetbrains.com/issue/KT-77182) A function in a file annotated with `@file`:MustUseReturnValue doesn't produce a warning when it is used from compiled code
- [`KT-78541`](https://youtrack.jetbrains.com/issue/KT-78541) Jspecify: Unsound platform type despite `@NullMarked` for an override with a generic-subclass return type
- [`KT-79672`](https://youtrack.jetbrains.com/issue/KT-79672) 'when expression must be exhaustive' even after using 'require()'
- [`KT-71306`](https://youtrack.jetbrains.com/issue/KT-71306) K2 IDE / Kotlin Debugger: “Cannot find local variable 'block' with type kotlin.jvm.functions.Function0” on evaluating lambda arg inside inline function
- [`KT-80003`](https://youtrack.jetbrains.com/issue/KT-80003) Kotlin/Native: deprecate eager GlobalData initialization
- [`KT-76991`](https://youtrack.jetbrains.com/issue/KT-76991) K2 IDE / Kotlin Debugger: ISE “Couldn't find declaration file for” on evaluating local fun when the scope has also inline fun from another file call
- [`KT-79877`](https://youtrack.jetbrains.com/issue/KT-79877) K2 IDE / Kotlin Debugger: failed evaluations of a code fragment capturing local data class
- [`KT-77401`](https://youtrack.jetbrains.com/issue/KT-77401) [FIR] `ParameterNameTypeAttribute.name` doesn't support `@ParameterName` with compile-time constant property argument
- [`KT-79682`](https://youtrack.jetbrains.com/issue/KT-79682) Fix partially uninitialized locals after coroutine spills insertion
- [`KT-79276`](https://youtrack.jetbrains.com/issue/KT-79276) Dexing fails with "Cannot read field X because <local0> is null" with 2.2.0
- [`KT-79562`](https://youtrack.jetbrains.com/issue/KT-79562) NPE when passing non-lambda argument of nullable non-suspend function type into function that accepts nullable suspend function type
- [`KT-79693`](https://youtrack.jetbrains.com/issue/KT-79693) NotImplementedError: An operation is not implemented: Unknown file with KMP separate compilation
- [`KT-79662`](https://youtrack.jetbrains.com/issue/KT-79662) Unused return value checker doesn't work for com.google.errorprone.annotations.CheckReturnValue
- [`KT-79781`](https://youtrack.jetbrains.com/issue/KT-79781) Missing MISSING_DEPENDENCY_CLASS when using type alias with inaccessible RHS
- [`KT-79547`](https://youtrack.jetbrains.com/issue/KT-79547) "UnsupportedOperationException: Not supported" with inlining and value classes
- [`KT-77772`](https://youtrack.jetbrains.com/issue/KT-77772) Only report exposed type on qualifier if it's resolved to an object
- [`KT-79765`](https://youtrack.jetbrains.com/issue/KT-79765) K2. Do not report ignore return value for unresolved reference
- [`KT-76343`](https://youtrack.jetbrains.com/issue/KT-76343) Drop language version 1.8
- [`KT-79017`](https://youtrack.jetbrains.com/issue/KT-79017) False negative REDECLARATION on private nested class
- [`KT-72039`](https://youtrack.jetbrains.com/issue/KT-72039) StackOverflowError on calling keySet on a Kotlin subclass of Java subclass of ConcurrentHashMap
- [`KT-79451`](https://youtrack.jetbrains.com/issue/KT-79451) Rework approach to recursive types approximation
- [`KT-75843`](https://youtrack.jetbrains.com/issue/KT-75843) K2: incorrect line numbers in an if-expression with a super-call
- [`KT-77504`](https://youtrack.jetbrains.com/issue/KT-77504) Add a warning when `@IgnorableReturnValue` is inconsistent on overrides
- [`KT-78389`](https://youtrack.jetbrains.com/issue/KT-78389) Perform version 2.3 boostrapping
- [`KT-79092`](https://youtrack.jetbrains.com/issue/KT-79092) Crash on default argument in function in fun interface
- [`KT-77729`](https://youtrack.jetbrains.com/issue/KT-77729) Package-level `@NullMarked` does not work when kotlinc sees .java *source* files
- [`KT-79013`](https://youtrack.jetbrains.com/issue/KT-79013) False negative `NOT_YET_SUPPORTED_IN_INLINE` on inline local functions inside inline functions
- [`KT-79139`](https://youtrack.jetbrains.com/issue/KT-79139) False positive CONFLICTING_OVERLOADS for context parameters instead of receivers
- [`KT-35305`](https://youtrack.jetbrains.com/issue/KT-35305) Address the overload conflict resolution between unsigned and non-primitive types
- [`KT-42096`](https://youtrack.jetbrains.com/issue/KT-42096) No diagnostic reported on `inline` modifier on an enum entry
- [`KT-79355`](https://youtrack.jetbrains.com/issue/KT-79355) Failed to fix the problem of desugared `inc` with new reverse implies returns contract
- [`KT-79277`](https://youtrack.jetbrains.com/issue/KT-79277) Implies returns contract doesn't affect the return type of the function if it is in the argument position
- [`KT-79271`](https://youtrack.jetbrains.com/issue/KT-79271) Implies returns contract doesn't impact exhaustiveness
- [`KT-79218`](https://youtrack.jetbrains.com/issue/KT-79218) SMARTCAST_IMPOSSIBLE for top‑level extension‑property getter despite returnsNotNull contract
- [`KT-79220`](https://youtrack.jetbrains.com/issue/KT-79220) returnsNotNull contract ignored on extension function with nullable receiver
- [`KT-79354`](https://youtrack.jetbrains.com/issue/KT-79354) IllegalStateException: Debug metadata version mismatch. Expected: 1, got 2 with compiler 2.2.20-Beta1 and stdlib 2.2.0
- [`KT-77986`](https://youtrack.jetbrains.com/issue/KT-77986) K2: False negative: "Local classes are not yet supported in inline functions"
- [`KT-79456`](https://youtrack.jetbrains.com/issue/KT-79456) Redeclaration conflict checks of private top-level classifiers rely on an incorrect containing file
- [`KT-79125`](https://youtrack.jetbrains.com/issue/KT-79125) RVC full mode: delegated interfaces are not checked
- [`KT-63720`](https://youtrack.jetbrains.com/issue/KT-63720) Coroutine debugger: do not optimise out local variables
- [`KT-78595`](https://youtrack.jetbrains.com/issue/KT-78595) type variable leak on a generic property as a call argument given an unstable smart cast
- [`KT-79076`](https://youtrack.jetbrains.com/issue/KT-79076) 'IllegalStateException: Cannot serialize error type: ERROR CLASS: Uninferred type' with Exposed column using recursive generic type
- [`KT-75797`](https://youtrack.jetbrains.com/issue/KT-75797) Native: find a way to handle generates C bridges in inline functions
- [`KT-59807`](https://youtrack.jetbrains.com/issue/KT-59807) K2: Replicate the MUST_BE_LATEINIT logic from K1
- [`KT-76782`](https://youtrack.jetbrains.com/issue/KT-76782) K2: Incorrect resolve into unrelated invoke operator with wrong diagnostic
- [`KT-78066`](https://youtrack.jetbrains.com/issue/KT-78066) TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER error message does not account for context parameters
- [`KT-76136`](https://youtrack.jetbrains.com/issue/KT-76136) Switch latest stable version in Kotlin project to 2.3
- [`KT-78881`](https://youtrack.jetbrains.com/issue/KT-78881) K2: False positive "Assigned value is never read" in composable function
- [`KT-76065`](https://youtrack.jetbrains.com/issue/KT-76065) Drop JavaTypeParameterDefaultRepresentationWithDNN feature
- [`KT-77808`](https://youtrack.jetbrains.com/issue/KT-77808) Inference: recheck the code about DNN-related hacks
- [`KT-24202`](https://youtrack.jetbrains.com/issue/KT-24202) NOTHING_TO_OVERRIDE  if super-class reference misses generic arguments
- [`KT-78909`](https://youtrack.jetbrains.com/issue/KT-78909) K2: Missing diagnostics [CYCLIC_INHERITANCE_HIERARCHY] for recursive class inheritance leads to StackOverflowError
- [`KT-75969`](https://youtrack.jetbrains.com/issue/KT-75969) java.lang.IllegalArgumentException: source must not be null on red code
- [`KT-76902`](https://youtrack.jetbrains.com/issue/KT-76902) Omit type-use annotations from diagnostics
- [`KT-58988`](https://youtrack.jetbrains.com/issue/KT-58988) K2: Deprecate exposing package-private parameter of internal method
- [`KT-17460`](https://youtrack.jetbrains.com/issue/KT-17460) Diagnostics and intention on suspend function that is overriden with non-suspend one.
- [`KT-56665`](https://youtrack.jetbrains.com/issue/KT-56665) K2: false positive RECURSIVE_TYPEALIAS_EXPANSION
- [`KT-78932`](https://youtrack.jetbrains.com/issue/KT-78932) Contracts are allowed for open and overridden property accessors
- [`KT-77203`](https://youtrack.jetbrains.com/issue/KT-77203) FIR: Consider adding destructured type to all COMPONENT_FUNCTION_* diagnostics
- [`KT-76635`](https://youtrack.jetbrains.com/issue/KT-76635) Implement Data-Flow Based Exhaustiveness Support
- [`KT-77685`](https://youtrack.jetbrains.com/issue/KT-77685) "IllegalArgumentException: Sequence contains more than one matching element"
- [`KT-78452`](https://youtrack.jetbrains.com/issue/KT-78452) Drop redundant frontend structures after fir2ir conversion

### Compose compiler
#### New features
- [`98d3907`](https://github.com/JetBrains/kotlin/commit/98d39077a9b19c1e6c112dc59982175095ac9f41) Introduce a compose group analysis module that produces a proguard/R8 mapping from group keys in bytecode.
#### Fixes
- [`b/431025881`](https://issuetracker.google.com/issues/431025881) [Compose] Clean up runtime version checker
- [`b/365922168`](https://issuetracker.google.com/issues/365922168) Add `java.util.Locale` to the list of known stable classes
- [`b/407549020`](https://issuetracker.google.com/issues/407549020) Introduce a registry of known stable markers
- [`b/417989445`](https://issuetracker.google.com/issues/417989445) Added a diagnostic to restrict usages of `runCatching` in `@Composable` functions
- [`KT-80294`](https://youtrack.jetbrains.com/issue/KT-80294) Fix crash with inline `@Composable` function reference
- [`b/430140896`](https://issuetracker.google.com/issues/430140896) Fix IrSourcePrinter output for when branch check and typechecks


### IDE. Gradle Integration

- [`KT-46273`](https://youtrack.jetbrains.com/issue/KT-46273) MPP: Don't fail import for case of missed platform in source set structure
- [`KT-46417`](https://youtrack.jetbrains.com/issue/KT-46417) [UNRESOLVED_REFERENCE] For project to project dependencies of native platform test source sets
- [`KT-44845`](https://youtrack.jetbrains.com/issue/KT-44845) After update to Kotlin 1.4.30 all external dependencies is unresolved in IDE with kotlin.mpp.enableGranularSourceSetsMetadata=true
- [`KT-46142`](https://youtrack.jetbrains.com/issue/KT-46142) K/N distribution is unavailable from IDE with multiplatform hierarchical project structure enabled

### IR. Actualizer

- [`KT-80002`](https://youtrack.jetbrains.com/issue/KT-80002) Investigate the need for map copying in IrCommonToPlatformDependencyExtractor.kt
- [`KT-80131`](https://youtrack.jetbrains.com/issue/KT-80131) KMP Separate Compilation: No override for FUN IR_EXTERNAL_DECLARATION_STUB name:<get-size>
- [`KT-80064`](https://youtrack.jetbrains.com/issue/KT-80064) KMP Separate Compilation: ClassCastException: class org.jetbrains.kotlin.ir.symbols.impl.IrTypeAliasSymbolImpl cannot be cast to class org.jetbrains.kotlin.ir.symbols.IrClassSymbol
- [`KT-80051`](https://youtrack.jetbrains.com/issue/KT-80051) KMP Separate Compilation: Actualization of common dependencies failed on 'PROPERTY FAKE_OVERRIDE name:modCount visibility:protected modality:FINAL [fake_override,var]'
- [`KT-79998`](https://youtrack.jetbrains.com/issue/KT-79998) KMP Separate Compilation: java.lang.IllegalStateException: No override for FUN IR_EXTERNAL_DECLARATION_STUB name:<get-message>
- [`KT-77337`](https://youtrack.jetbrains.com/issue/KT-77337) `IrNoExpectSymbolsHandler` finds expect class reference after enabling annotation traversal in IR

### IR. Inlining

#### New Features

- [`KT-70360`](https://youtrack.jetbrains.com/issue/KT-70360) KLIBs: Uniformly handle`typeOf()` calls at 1st/2nd stages of compilation

#### Performance Improvements

- [`KT-69497`](https://youtrack.jetbrains.com/issue/KT-69497) Crossinline lambda is allocated on K/N & JS

#### Fixes

- [`KT-79334`](https://youtrack.jetbrains.com/issue/KT-79334) Unify intrinsics used on 1st phase of IR inliner in KLIB-based compilers
- [`KT-80610`](https://youtrack.jetbrains.com/issue/KT-80610) KLIB inliner: Always apply cross-module inlining to pre-processed inline functions
- [`KT-80565`](https://youtrack.jetbrains.com/issue/KT-80565) KLIB Inliner: Add a special annotation to prohibit inlining of marked inline functions in stdlib on 1st compilation phase
- [`KT-80883`](https://youtrack.jetbrains.com/issue/KT-80883) [Inliner] Run pre-serialization lowerings in all testrunners
- [`KT-77876`](https://youtrack.jetbrains.com/issue/KT-77876) IrVisibilityChecker: Different set of exceptions for 1st and 2nd compilation stages
- [`KT-80693`](https://youtrack.jetbrains.com/issue/KT-80693) [IC] Split IC invalidation tests for cross-module IR Inliner
- [`KT-79718`](https://youtrack.jetbrains.com/issue/KT-79718) KLIB inliner: Emit warning on generation of `public` synthetic accessor when running in "explicit API mode"
- [`KT-78537`](https://youtrack.jetbrains.com/issue/KT-78537) [Inliner] Incorrect KFunction.name of a reference to inlined local function
- [`KT-80226`](https://youtrack.jetbrains.com/issue/KT-80226) [IR Inliner] Generate constructor accessors as constructors, not static functions
- [`KT-80653`](https://youtrack.jetbrains.com/issue/KT-80653) [IR Inliner] Space: "Local declarations should've been popped out by this point"
- [`KT-80692`](https://youtrack.jetbrains.com/issue/KT-80692) [IC] Split IC invalidation tests for intra-module IR Inliner
- [`KT-77103`](https://youtrack.jetbrains.com/issue/KT-77103) [Inliner] IrLocalDelegatedProperty was not serialized, while its symbol and IrRichPropertyReference were.
- [`KT-78392`](https://youtrack.jetbrains.com/issue/KT-78392) CommonPrefix: Add a way of stopping execution when one of the phases is unsuccessful
- [`KT-78903`](https://youtrack.jetbrains.com/issue/KT-78903) Unify `codegen/boxInline` tests with  `codegen/box`
- [`KT-78989`](https://youtrack.jetbrains.com/issue/KT-78989) Add missing PL tests for inline functions/property accessors
- [`KT-79771`](https://youtrack.jetbrains.com/issue/KT-79771) kotlinx-coroutines-core: Public synthetic accessor generated with enabled KLIB IR inliner
- [`KT-79680`](https://youtrack.jetbrains.com/issue/KT-79680) `IrConstructorSymbolImpl is unbound` in lambdaWithoutNonLocalControlflow.kt
- [`KT-70849`](https://youtrack.jetbrains.com/issue/KT-70849) Ensure correct debug info for intra-module IR inlining on the first compilation phase
- [`KT-79800`](https://youtrack.jetbrains.com/issue/KT-79800) JS BE errors with default values when IR inliner is enabled
- [`KT-79352`](https://youtrack.jetbrains.com/issue/KT-79352) Remove excessive validations from `ValidateAfterAll...` on the first stage
- [`KT-76599`](https://youtrack.jetbrains.com/issue/KT-76599) Migrate `IrValidationAfterInliningAllFunctionsPhase` to the first stage of compilation
- [`KT-78245`](https://youtrack.jetbrains.com/issue/KT-78245) Synthetic Accessors incorrectly copies default values
- [`KT-72594`](https://youtrack.jetbrains.com/issue/KT-72594) [JS][Native] Add IrInliningFacade to test runners

### IR. Interpreter

- [`KT-72881`](https://youtrack.jetbrains.com/issue/KT-72881) K2: incorrect empty array as annotation argument when parameter has default value

### IR. Tree

#### Fixes

- [`KT-77819`](https://youtrack.jetbrains.com/issue/KT-77819) [IR] Fine-tune IrValidator's run after Fir2IR and IR plugins
- [`KT-70160`](https://youtrack.jetbrains.com/issue/KT-70160) Remove IrDeclaration.parents after Anvil update
- [`KT-80454`](https://youtrack.jetbrains.com/issue/KT-80454) LocalDeclarationsLowering: Clean-up the dead code
- [`KT-80819`](https://youtrack.jetbrains.com/issue/KT-80819) Rework IrFileValidator to use Hashmap instead of ClassValue
- [`KT-80516`](https://youtrack.jetbrains.com/issue/KT-80516) Kotlin-like IR dump: Don't render tailrec as lateinit
- [`KT-79439`](https://youtrack.jetbrains.com/issue/KT-79439) KLIB stdlib symbols loading: Split the result of merging of IrBuiltins with BuiltinSymbolsBase hierarchy into two parts (for 1st & 2nd phases)
- [`KT-79437`](https://youtrack.jetbrains.com/issue/KT-79437) KLIB stdlib symbols loading: Drop loading functions from IrBuiltins and migrate usages to SymbolFinder functions and lazy filtering
- [`KT-79569`](https://youtrack.jetbrains.com/issue/KT-79569) Unexpected error during DFG phase in Native due to PL issue with SAM conversion represented by rich reference
- [`KT-79371`](https://youtrack.jetbrains.com/issue/KT-79371) Fix handling of broken SAM conversion in PL with enabled Rich References
- [`KT-76601`](https://youtrack.jetbrains.com/issue/KT-76601) IrValidatorConfig should have all checks disabled by default
- [`KT-69662`](https://youtrack.jetbrains.com/issue/KT-69662) Deduplicate function `createTemporaryVariable`
- [`KT-79440`](https://youtrack.jetbrains.com/issue/KT-79440) KLIB stdlib symbols loading: Drop BuiltinSymbolsBase from plugin API
- [`KT-78960`](https://youtrack.jetbrains.com/issue/KT-78960) [FO] Limit static fake overrides generation for static functions
- [`KT-76813`](https://youtrack.jetbrains.com/issue/KT-76813) IR validator: not all symbols/references are visited

### JVM. Reflection

- [`KT-76521`](https://youtrack.jetbrains.com/issue/KT-76521) Reflection: change KType representation to avoid dependency on K1
- [`KT-74624`](https://youtrack.jetbrains.com/issue/KT-74624) Reflection: KClassifier.createType(...) ignores annotations parameter
- [`KT-80203`](https://youtrack.jetbrains.com/issue/KT-80203) Reflection: provide a way to use legacy K1-based implementation
- [`KT-80236`](https://youtrack.jetbrains.com/issue/KT-80236) Reflection: KType.toString for raw types no longer renders "(raw)"
- [`KT-79020`](https://youtrack.jetbrains.com/issue/KT-79020) Suspend lambdas return type is shown as ??? in reflection
- [`KT-79206`](https://youtrack.jetbrains.com/issue/KT-79206) Reflection: suspend functional type classifier is null
- [`KT-74529`](https://youtrack.jetbrains.com/issue/KT-74529) Context parameters support in reflection

### JavaScript

#### New Features

- [`KT-79284`](https://youtrack.jetbrains.com/issue/KT-79284) Use BigInt64Array for LongArray
- [`KT-79394`](https://youtrack.jetbrains.com/issue/KT-79394) Add the possibility to write common external declarations between JS and WasmJS targets

#### Performance Improvements

- [`KT-57128`](https://youtrack.jetbrains.com/issue/KT-57128) KJS: Use BigInt to represent Long values in ES6 mode

#### Fixes

- [`KT-81424`](https://youtrack.jetbrains.com/issue/KT-81424) Kotlin/JS: Cannot Get / in a simple running application
- [`KT-80873`](https://youtrack.jetbrains.com/issue/KT-80873) KJS: Stdlib requires ES2020-compatible JS engine due to BigInt type literal
- [`KT-56281`](https://youtrack.jetbrains.com/issue/KT-56281) KJS: Can't export suspend functions
- [`KT-72833`](https://youtrack.jetbrains.com/issue/KT-72833) KJS: Source maps have incorrect sources paths in `per-file`
- [`KT-79926`](https://youtrack.jetbrains.com/issue/KT-79926) Wrong export of interfaces with companions with ES Modules
- [`KT-72474`](https://youtrack.jetbrains.com/issue/KT-72474) KJS: `@JsPlainObject` doesn't honour -XXLanguage:+JsAllowInvalidCharsIdentifiersEscaping
- [`KT-79644`](https://youtrack.jetbrains.com/issue/KT-79644) BigInt enabled for ES 2015 despite being an ES 2020 feature
- [`KT-79089`](https://youtrack.jetbrains.com/issue/KT-79089) KJS: Could not load reporter / Cannot find module 'mocha' when running jsNode tests
- [`KT-52771`](https://youtrack.jetbrains.com/issue/KT-52771) KJS: Pair should be exported to JavaScript
- [`KT-79704`](https://youtrack.jetbrains.com/issue/KT-79704) Unify variance rendering between JS and other backends
- [`KT-69297`](https://youtrack.jetbrains.com/issue/KT-69297) Deprecate referencing inlineable lambdas in `js()` calls
- [`KT-80168`](https://youtrack.jetbrains.com/issue/KT-80168) Allow `@JsStatic` inside interface companions
- [`KT-80086`](https://youtrack.jetbrains.com/issue/KT-80086) [k/js] Resolving imported string literals
- [`KT-79066`](https://youtrack.jetbrains.com/issue/KT-79066) [Kotlin/JS] jsNodeTest fails with SyntaxError when a test file has `@file`:JsExport and useEsModules() is enabled
- [`KT-79359`](https://youtrack.jetbrains.com/issue/KT-79359) Kotlin/JS: Suspending function doesn’t return Unit on es2015
- [`KT-77385`](https://youtrack.jetbrains.com/issue/KT-77385) Investigate partial linkage problems for JS HMPP tests
- [`KT-79628`](https://youtrack.jetbrains.com/issue/KT-79628) Remove IR nodes from ExportModel
- [`KT-79916`](https://youtrack.jetbrains.com/issue/KT-79916) K/JS: "Uncaught TypeError" when using 'Xes-long-as-bigint' in compose-html
- [`KT-79050`](https://youtrack.jetbrains.com/issue/KT-79050) KJS / IC: "Unexpected body of primary constructor for processing irClass"
- [`KT-79977`](https://youtrack.jetbrains.com/issue/KT-79977) KJS: Long.rotateLeft returns incorrect result when BigInts are enabled
- [`KT-70222`](https://youtrack.jetbrains.com/issue/KT-70222) Remove legacy JS BE-related CLI flags
- [`KT-78831`](https://youtrack.jetbrains.com/issue/KT-78831) AbstractFunctionReferencesLowering: fragile fake override generation
- [`KT-52230`](https://youtrack.jetbrains.com/issue/KT-52230) KSJ IR: Applying identity equality operator to Longs always returns false
- [`KT-6675`](https://youtrack.jetbrains.com/issue/KT-6675) KotlinJS: toInt() on external Long throws error
- [`KT-79184`](https://youtrack.jetbrains.com/issue/KT-79184) K/JS: Further intrinsify BigInt-backed Long operations

### Klibs

#### Performance Improvements

- [`KT-80861`](https://youtrack.jetbrains.com/issue/KT-80861) [Klib] Deduplicate IrFileEntry.name
- [`KT-80866`](https://youtrack.jetbrains.com/issue/KT-80866) [Klib] Optimize size of IrFileEntry.line_start_offset
- [`KT-80438`](https://youtrack.jetbrains.com/issue/KT-80438) Uncached KlibMetadataClassDataFinder.findClassData

#### Fixes

- [`KT-80298`](https://youtrack.jetbrains.com/issue/KT-80298) K/N: one-stage compilation is broken
- [`KT-80099`](https://youtrack.jetbrains.com/issue/KT-80099) KLIB resolver: Could not find file because of missing `klib` extension in resolved symlink path
- [`KT-79958`](https://youtrack.jetbrains.com/issue/KT-79958) KLIB tool fails to render IR if there is IrErrorType in a lirbrary
- [`KT-75241`](https://youtrack.jetbrains.com/issue/KT-75241) Move ExperimentalLibraryAbiReader to a publishable artifact
- [`KT-76260`](https://youtrack.jetbrains.com/issue/KT-76260) Make `IrRichCallableReferencesInKlibs` lang feature stable in LV=2.3
- [`KT-61552`](https://youtrack.jetbrains.com/issue/KT-61552) [PL] IndexOutOfBoundsException in SAM conversion with substituted function
- [`KT-74417`](https://youtrack.jetbrains.com/issue/KT-74417) Deduce the metadata version based on LV in KLIB-based backends
- [`KT-75980`](https://youtrack.jetbrains.com/issue/KT-75980) [Klib] Reduce serialized size of IrFileEntries for sparse usage of another source files
- [`KT-73826`](https://youtrack.jetbrains.com/issue/KT-73826) Deduplicate `IrFileEntry` that is serialized inside `IrInlinedFunctionBlock`

### Language Design

- [`KT-76926`](https://youtrack.jetbrains.com/issue/KT-76926) Allow return in expression bodies if return type is specified explicitly
- [`KT-23610`](https://youtrack.jetbrains.com/issue/KT-23610) Overload resolution ambiguity for suspend function argument
- [`KT-32619`](https://youtrack.jetbrains.com/issue/KT-32619) JS: return Promise when `continuation` is not provided
- [`KT-78866`](https://youtrack.jetbrains.com/issue/KT-78866) Warn implicit receiver shadowed by context parameter
- [`KT-78976`](https://youtrack.jetbrains.com/issue/KT-78976) Decide if K2 should support local functions inside of local inline functions
- [`KT-79308`](https://youtrack.jetbrains.com/issue/KT-79308) Ability to actualize empty interfaces as Any
- [`KT-48872`](https://youtrack.jetbrains.com/issue/KT-48872) Provide modern and performant replacement for Enum.values()

### Libraries

#### New Features

- [`KT-31400`](https://youtrack.jetbrains.com/issue/KT-31400) Add smartcast for `isArrayOf`
- [`KT-78463`](https://youtrack.jetbrains.com/issue/KT-78463) Annotate wasm and JS targets of kotlin-stdlib with `@IgnorableReturnValue` when appropriate
- [`KT-74444`](https://youtrack.jetbrains.com/issue/KT-74444) EnumEntries type should implement RandomAccess
- [`KT-78462`](https://youtrack.jetbrains.com/issue/KT-78462) Annotate kotlin-stdlib-jvm with `@IgnorableReturnValue` where appropriate

#### Fixes

- [`KT-80778`](https://youtrack.jetbrains.com/issue/KT-80778) Stabilize kotlin.time.Clock and kotlin.time.Instant
- [`KT-80619`](https://youtrack.jetbrains.com/issue/KT-80619) [KLIBs] Enable intra-module inliner in stdlib & kotlin-test
- [`KT-76773`](https://youtrack.jetbrains.com/issue/KT-76773) stdlib: contextOf's type argument can be inferred via contextOf's context argument
- [`KT-71822`](https://youtrack.jetbrains.com/issue/KT-71822) Intersection with (subtraction from) an identity set may produce incorrect results
- [`KT-74411`](https://youtrack.jetbrains.com/issue/KT-74411) Introduce Uuid.generateV4() and generateV7()
- [`KT-80605`](https://youtrack.jetbrains.com/issue/KT-80605) Rename MustUseReturnValue -> MustUseReturnValues
- [`KT-56822`](https://youtrack.jetbrains.com/issue/KT-56822) Deprecate Number.toChar() with error deprecation level
- [`KT-69947`](https://youtrack.jetbrains.com/issue/KT-69947) KLIB stdlib: All intrinsics that can be used in KLIBs with inlined IR must be included in stdlib ABI dump
- [`KT-59044`](https://youtrack.jetbrains.com/issue/KT-59044) Improve various aspects of TimeSource documentation
- [`KT-80544`](https://youtrack.jetbrains.com/issue/KT-80544) Mark controversial path extensions (like .deleteRecursively()) as ignorable
- [`KT-80603`](https://youtrack.jetbrains.com/issue/KT-80603) K/N and K/Wasm: \p{N} category is not supported
- [`KT-79192`](https://youtrack.jetbrains.com/issue/KT-79192) Increase  InputStream.readBytes(Int) deprecation level to HIDDEN
- [`KT-80661`](https://youtrack.jetbrains.com/issue/KT-80661) ArrayDeque.lastIndexOf may return -1 for an element present in the deque
- [`KT-80390`](https://youtrack.jetbrains.com/issue/KT-80390) ArrayDeque.indexOf(null) wrongly returns 0 after removals
- [`KT-79094`](https://youtrack.jetbrains.com/issue/KT-79094) Change signature of assertFailsWith or make lambda excluded otherwise
- [`KT-72028`](https://youtrack.jetbrains.com/issue/KT-72028) Incorrect parameters order in IndexedValue documentation
- [`KT-80130`](https://youtrack.jetbrains.com/issue/KT-80130) [stdlib] Commonize AssociatedObjects in commonNonJvmMain
- [`KT-80107`](https://youtrack.jetbrains.com/issue/KT-80107) [stdlib] Move CancellationException to commonNonJvmMain
- [`KT-80179`](https://youtrack.jetbrains.com/issue/KT-80179) Investigate why StringBuilder.length is not enhanced automatically
- [`KT-80046`](https://youtrack.jetbrains.com/issue/KT-80046) Increase test coverage of Duration.parse[IsoString][OrNull] methods
- [`KT-76459`](https://youtrack.jetbrains.com/issue/KT-76459) Remove comments about sorting stability in unsigned-type arrays
- [`KT-79489`](https://youtrack.jetbrains.com/issue/KT-79489) Generate Stdlib API reference for webMain source set
- [`KT-78243`](https://youtrack.jetbrains.com/issue/KT-78243) Drop JS- and Wasm-specific IrLinkageError classes
- [`KT-79108`](https://youtrack.jetbrains.com/issue/KT-79108) Remove the default argument for `linkageError` from kotlin.js.getPropertyCallableRef
- [`KT-79130`](https://youtrack.jetbrains.com/issue/KT-79130) K/JS: Remove bodies from intrinsified Long methods
- [`KT-79239`](https://youtrack.jetbrains.com/issue/KT-79239) K/Wasm: elementAt extension function of Array/PrimitiveArray/UnsignedArray does not throw IndexOutOfBoundException on incorrect index
- [`KT-79256`](https://youtrack.jetbrains.com/issue/KT-79256) K/Wasm: MatchResult.groups raises a trap on invalid group index
- [`KT-57317`](https://youtrack.jetbrains.com/issue/KT-57317) Repack EnumEntries from stdlib into the compiler

### Native

- [`KT-79384`](https://youtrack.jetbrains.com/issue/KT-79384) K/N: Application Not Responding: Thread Deadlock
- [`KT-80536`](https://youtrack.jetbrains.com/issue/KT-80536) Native: `DependencyDownloader` seems to have no timeout
- [`KT-80624`](https://youtrack.jetbrains.com/issue/KT-80624) Bump minimal watchOS supported versions to 7.0
- [`KT-80620`](https://youtrack.jetbrains.com/issue/KT-80620) Bump minimal iOS and tvOS supported versions to 14.0

### Native. Build Infrastructure

- [`KT-80147`](https://youtrack.jetbrains.com/issue/KT-80147) Set proper LV and AV for `kotlin-native/performance/buildSrc`
- [`KT-79474`](https://youtrack.jetbrains.com/issue/KT-79474) Kotlin/Native: fix breakpad build
- [`KT-79215`](https://youtrack.jetbrains.com/issue/KT-79215) Kotlin/Native: fix distInvalidateStaleCaches on windows

### Native. C and ObjC Import

- [`KT-79571`](https://youtrack.jetbrains.com/issue/KT-79571) Xcode 26 beta 4: CInteropKT39120TestGenerated.testForwardEnum failed
- [`KT-79753`](https://youtrack.jetbrains.com/issue/KT-79753) Native: support CCall.Direct calls in the compiler
- [`KT-80838`](https://youtrack.jetbrains.com/issue/KT-80838) Cinterop fails with an error when Compilation works fine
- [`KT-79752`](https://youtrack.jetbrains.com/issue/KT-79752) Native: make cinterop generate CCall.Direct annotations
- [`KT-49034`](https://youtrack.jetbrains.com/issue/KT-49034) Kotlin/Native: `cnames.structs.Foo` resolves into wrong declaration

### Native. ObjC Export

- [`KT-78810`](https://youtrack.jetbrains.com/issue/KT-78810) [ObjCExport] Enable explicit ObjC block parameter names by default
- [`KT-80271`](https://youtrack.jetbrains.com/issue/KT-80271) ObjC/Swift Export: Remove Native platform `Cloneable` checks
- [`KT-78604`](https://youtrack.jetbrains.com/issue/KT-78604) Consider not inheriting `KlibScope` from `KaScope`
- [`KT-79767`](https://youtrack.jetbrains.com/issue/KT-79767) ObjCExport: private companion must not be exposed
- [`KT-79724`](https://youtrack.jetbrains.com/issue/KT-79724) ObjCExport: extensions order
- [`KT-79548`](https://youtrack.jetbrains.com/issue/KT-79548) ObjCExport: mangling difference between K1 and K2 when translating KotlinDurationCompanion
- [`KT-79475`](https://youtrack.jetbrains.com/issue/KT-79475) ObjCExport: invalid property getter translation
- [`KT-79346`](https://youtrack.jetbrains.com/issue/KT-79346) ObjCExport: Any method overrides
- [`KT-78871`](https://youtrack.jetbrains.com/issue/KT-78871) ObjCExport: translation of keyword `release` with parameter generates invalid header

### Native. Runtime. Memory

- [`KT-75918`](https://youtrack.jetbrains.com/issue/KT-75918) Native: Deprecate -Xallocator=std
- [`KT-80678`](https://youtrack.jetbrains.com/issue/KT-80678) Native: pagedAllocator=false sweep is slow
- [`KT-75916`](https://youtrack.jetbrains.com/issue/KT-75916) Native: Enable sanitizer support with pagedAllocator=false

### Native. Swift Export

- [`KT-80111`](https://youtrack.jetbrains.com/issue/KT-80111) Swift Export Build Fails Due to Errors in KotlinStdlib.swift
- [`KT-80884`](https://youtrack.jetbrains.com/issue/KT-80884) Swift Export: support async in SIR
- [`KT-80185`](https://youtrack.jetbrains.com/issue/KT-80185) Swift Export: IllegalArgumentException – Collection contains more than one matching element
- [`KT-79889`](https://youtrack.jetbrains.com/issue/KT-79889) K/N: swift-export fails under several different conditions
- [`KT-79518`](https://youtrack.jetbrains.com/issue/KT-79518) Swift export: represent kotlin.Any as swift.any
- [`KT-78603`](https://youtrack.jetbrains.com/issue/KT-78603) Do not inherit SirAndKaSession from KaSession
- [`KT-79227`](https://youtrack.jetbrains.com/issue/KT-79227) Swift Export: Fix First Release Issues
- [`KT-79521`](https://youtrack.jetbrains.com/issue/KT-79521) '_CoroutineScope' is inaccessible due to 'internal' protection level
- [`KT-79181`](https://youtrack.jetbrains.com/issue/KT-79181) Swift Export Fails When Using T: Comparable<T> Generic Constraint in Kotlin Classes

### Tools. Ant

- [`KT-75875`](https://youtrack.jetbrains.com/issue/KT-75875) Remove Ant support

### Tools. BCV

- [`KT-80313`](https://youtrack.jetbrains.com/issue/KT-80313) Add ability to generate dump from jar files [ABI Tools]

### Tools. Build Tools API

- [`KT-79409`](https://youtrack.jetbrains.com/issue/KT-79409) BTA: Support removed compiler arguments properly
- [`KT-78196`](https://youtrack.jetbrains.com/issue/KT-78196) BTA: implement API adapter for the prototype implementation
- [`KT-77999`](https://youtrack.jetbrains.com/issue/KT-77999) BTA: Generate BTA options from compiler arguments descriptions
- [`KT-78194`](https://youtrack.jetbrains.com/issue/KT-78194) BTA: port the JVM prototype to the new design
- [`KT-78193`](https://youtrack.jetbrains.com/issue/KT-78193) BTA: Implement core infrastructure according to the new design
- [`KT-78195`](https://youtrack.jetbrains.com/issue/KT-78195) BTA: migrate the test infrastructure from the prototype to the new design

### Tools. CLI

- [`KT-79867`](https://youtrack.jetbrains.com/issue/KT-79867) CompilerConfiguration.configureSourceRoots puts obfuscated file paths instead of ones passed on `classpath` to CLIConfigurationKeys.CONTENT_ROOTS
- [`KT-80348`](https://youtrack.jetbrains.com/issue/KT-80348) Expose 'XXLanguage' compiler argument as a normal argument
- [`KT-80428`](https://youtrack.jetbrains.com/issue/KT-80428) KMP Separate Compilation: Handle friend dependencies
- [`KT-74590`](https://youtrack.jetbrains.com/issue/KT-74590) Deprecate -Xjvm-default in favor of -jvm-default
- [`KT-80349`](https://youtrack.jetbrains.com/issue/KT-80349) KMP Separate Compilation is enabled on non-KMP compilations
- [`KT-79982`](https://youtrack.jetbrains.com/issue/KT-79982) Fix description of -Xjspecify-annotations
- [`KT-79403`](https://youtrack.jetbrains.com/issue/KT-79403) Improve generator for deprecated CLI arguments
- [`KT-75968`](https://youtrack.jetbrains.com/issue/KT-75968) Set proper lifecycle for all existing compiler arguments
- [`KT-79293`](https://youtrack.jetbrains.com/issue/KT-79293) Create Language Features and compiler argument with parameter for new destructuring features

### Tools. Commonizer

- [`KT-49735`](https://youtrack.jetbrains.com/issue/KT-49735) [Commonizer] :commonizeNativeDistribution  fails for projects with two or more same native targets
- [`KT-47523`](https://youtrack.jetbrains.com/issue/KT-47523) MPP: Unable to resolve c-interop dependency if platform is included in an intermediate source set with the only target
- [`KT-48118`](https://youtrack.jetbrains.com/issue/KT-48118) Commonized c-interop lib is not attached to common main source set
- [`KT-46248`](https://youtrack.jetbrains.com/issue/KT-46248) MPP: Compile KotlinMetadata fails with Unresolved reference if only one native platform from shared source set is available

### Tools. Compiler Plugin API

- [`KT-74867`](https://youtrack.jetbrains.com/issue/KT-74867) LLFirIdePredicateBasedProvider matches local classes when it shouldn't
- [`KT-52665`](https://youtrack.jetbrains.com/issue/KT-52665) Deprecate `ComponentRegistrar`
- [`KT-55300`](https://youtrack.jetbrains.com/issue/KT-55300) Provide a mechanism to describe ordering and dependencies for compiler plugins
- [`KT-75865`](https://youtrack.jetbrains.com/issue/KT-75865) Provide an API for setting the file name for the file with top-level declarations generated by a plugin

### Tools. Compiler Plugins

- [`KT-80815`](https://youtrack.jetbrains.com/issue/KT-80815) NoArg compiler plugin: Promote NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS diagnostic from warning to error
- [`KT-80822`](https://youtrack.jetbrains.com/issue/KT-80822) False positive NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS reported for a class with explicit noargs  constructor already present
- [`KT-53122`](https://youtrack.jetbrains.com/issue/KT-53122) Constructors generated with NoArg have no `@Metadata` and are invisible for the frontend
- [`KT-79319`](https://youtrack.jetbrains.com/issue/KT-79319) Lombok: NullPointerException on `mvn compile` when importing Java constants
- [`KT-74687`](https://youtrack.jetbrains.com/issue/KT-74687)  Kotlin Lombok: False positive when calling builder on Java record
- [`KT-80419`](https://youtrack.jetbrains.com/issue/KT-80419) Remove bundled jetbrains annotations from kotlin-dataframe-compiler-plugin
- [`KT-79245`](https://youtrack.jetbrains.com/issue/KT-79245) [AtomicFU] Drop K1/JS- and K1/Native-specific testrunners
- [`KT-79197`](https://youtrack.jetbrains.com/issue/KT-79197) DataFrame: Cannot find local variable 'this`@df`' with type Scope0
- [`KT-73865`](https://youtrack.jetbrains.com/issue/KT-73865) Incorrect type is generated for irPropertyReference during K/N transformation

### Tools. Compiler plugins. Serialization

- [`KT-70345`](https://youtrack.jetbrains.com/issue/KT-70345) Promote COMPANION_OBJECT_IS_SERIALIZABLE_INSIDE_SERIALIZABLE_CLASS diagnostic to error
- [`KT-79695`](https://youtrack.jetbrains.com/issue/KT-79695) Serialization does not exclude field-less properties in 2.2.20-Beta2
- [`KT-79246`](https://youtrack.jetbrains.com/issue/KT-79246) [Serialization] Drop K1-specific testrunners

### Tools. Gradle

#### Fixes

- [`KT-80763`](https://youtrack.jetbrains.com/issue/KT-80763) Add redirect link to error message when 'org.jetbrains.kotlin.android' plugin is used with built-in Kotlin
- [`KT-81038`](https://youtrack.jetbrains.com/issue/KT-81038) Gradle: remove support for properties disabling precise task outputs backup
- [`KT-75449`](https://youtrack.jetbrains.com/issue/KT-75449) Update deprecation of `KotlinJsTestFramework#createTestExecutionSpec`
- [`KT-80875`](https://youtrack.jetbrains.com/issue/KT-80875) Gradle: runToolInSeparateProcess may fail on Windows with too long command line
- [`KT-79851`](https://youtrack.jetbrains.com/issue/KT-79851) Emit an actionable warning/error on unsupported AV/LV configured by `kotlin-dsl`
- [`KT-64273`](https://youtrack.jetbrains.com/issue/KT-64273) Gradle: remove symbols deprecated after KT-54312
- [`KT-77458`](https://youtrack.jetbrains.com/issue/KT-77458) Run Gradle integration tests against Gradle 9.0
- [`KT-74915`](https://youtrack.jetbrains.com/issue/KT-74915) Make ExtrasProperty.kt internal
- [`KT-64992`](https://youtrack.jetbrains.com/issue/KT-64992) Remove KotlinCompilation.source
- [`KT-76720`](https://youtrack.jetbrains.com/issue/KT-76720) Raise deprecation level to error for Kotlin*Options properties
- [`KT-80172`](https://youtrack.jetbrains.com/issue/KT-80172) Error message changes depending on the order of applying 'org.jetbrains.kotlin.android' and 'AGP' 9.0+ with built-in Kotlin plugin
- [`KT-76177`](https://youtrack.jetbrains.com/issue/KT-76177) Remove deprecated classpath snapshot task inputs
- [`KT-79238`](https://youtrack.jetbrains.com/issue/KT-79238) Bump minimal supported AGP version to 8.2.2
- [`KT-79339`](https://youtrack.jetbrains.com/issue/KT-79339) Remove additionalMetadata from compiler options DSL
- [`KT-78741`](https://youtrack.jetbrains.com/issue/KT-78741) Add FUS analytics for klib cross-compilation
- [`KT-73478`](https://youtrack.jetbrains.com/issue/KT-73478) Add module level description
- [`KT-80083`](https://youtrack.jetbrains.com/issue/KT-80083) KGP IT: fix tests on Windows
- [`KT-79034`](https://youtrack.jetbrains.com/issue/KT-79034) Automatically disable cross compilation if it's not supported on the host
- [`KT-79408`](https://youtrack.jetbrains.com/issue/KT-79408) A lot of errors files are created when compile Kotlin
- [`KT-78827`](https://youtrack.jetbrains.com/issue/KT-78827) Rewrite Gradle compiler options DSL generator

### Tools. Gradle. BCV

- [`KT-80621`](https://youtrack.jetbrains.com/issue/KT-80621) Move Gradle tasks into suitable groups [ABI Validation]
- [`KT-78625`](https://youtrack.jetbrains.com/issue/KT-78625) Kotlin's built-in BCV generates empty .api files

### Tools. Gradle. JS

- [`KT-79910`](https://youtrack.jetbrains.com/issue/KT-79910) Wasm, JS: Upgrade NPM versions
- [`KT-79921`](https://youtrack.jetbrains.com/issue/KT-79921) Web Tooling Gradle API does not respect webpack reconfiguration
- [`KT-76996`](https://youtrack.jetbrains.com/issue/KT-76996) Wasm: js tasks triggers wasm subtasks
- [`KT-79237`](https://youtrack.jetbrains.com/issue/KT-79237) Upgrade NPM dependencies versions

### Tools. Gradle. Multiplatform

#### New Features

- [`KT-76446`](https://youtrack.jetbrains.com/issue/KT-76446) Add kotlin-level dependency block to work the same way as commonMain/commonTest dependencies blocks

#### Fixes

- [`KT-80785`](https://youtrack.jetbrains.com/issue/KT-80785) With `android.builtInKotlin=false` and `android.newDsl=true`, using `kotlin-android` plugin will fail with `ClassCastException`
- [`KT-61127`](https://youtrack.jetbrains.com/issue/KT-61127) Remove scoped resolvable and intransitive DependenciesMetadata configurations used in the pre-IdeMultiplatformImport IDE import
- [`KT-62614`](https://youtrack.jetbrains.com/issue/KT-62614) Remove legacy kotlin-gradle-plugin-model
- [`KT-79559`](https://youtrack.jetbrains.com/issue/KT-79559) AGP complains about configurations resolved at configuration time due to KMP partially resolved dependencies diagnostic
- [`KT-78993`](https://youtrack.jetbrains.com/issue/KT-78993) The value for property '*' property 'dependencies' is final and cannot be changed any further
- [`KT-74005`](https://youtrack.jetbrains.com/issue/KT-74005) Implement a prototype of Unified Klib support in Kotlin Gradle Plugin
- [`KT-76200`](https://youtrack.jetbrains.com/issue/KT-76200) TestModuleProperties.productionModuleName for JVM module isn't present with 2.1.20-RC
- [`KT-55312`](https://youtrack.jetbrains.com/issue/KT-55312) Replace "ALL_COMPILE_DEPENDENCIES_METADATA" configuration with set of metadata dependencies configurations associated per set
- [`KT-52216`](https://youtrack.jetbrains.com/issue/KT-52216) HMPP / KTOR: False positive "TYPE_MISMATCH" with Throwable descendant
- [`KT-54312`](https://youtrack.jetbrains.com/issue/KT-54312) TCS: Replace CompilationDetails abstract class hierarchy by composable implementation
- [`KT-55230`](https://youtrack.jetbrains.com/issue/KT-55230) Remove metadata dependencies transformation for runtimeOnly scope

### Tools. Gradle. Native

- [`KT-80675`](https://youtrack.jetbrains.com/issue/KT-80675) Commonized cinterops between "test" compilations produce an import failure
- [`KT-64107`](https://youtrack.jetbrains.com/issue/KT-64107) Kotlin Gradle plugin allows native binaries to have both `debuggable` and `optimized` flags set to `true`
- [`KT-74910`](https://youtrack.jetbrains.com/issue/KT-74910) Bump `destinationDir` in CInteropProcess to hidden
- [`KT-74911`](https://youtrack.jetbrains.com/issue/KT-74911) Promote CInteropProcess.konanVersion to hidden
- [`KT-74864`](https://youtrack.jetbrains.com/issue/KT-74864) Enable exporting KDocs by default to ObjC
- [`KT-72705`](https://youtrack.jetbrains.com/issue/KT-72705) K/N: compile task cache can not be used due to 'artifactVersion' input property

### Tools. Gradle. Swift Export

- [`KT-79524`](https://youtrack.jetbrains.com/issue/KT-79524) NoSuchMethodError: 'java.lang.String org.gradle.api.artifacts.ProjectDependency.getPath() for swift export with dependency export fro gradle < 8.11

### Tools. Incremental Compile

- [`KT-75864`](https://youtrack.jetbrains.com/issue/KT-75864) Implement a conservative mechanism of the IC with compiler plugins generated top-level declarations
- [`KT-79504`](https://youtrack.jetbrains.com/issue/KT-79504) Implement an API to provide IC lookups from backend plugins
- [`KT-55982`](https://youtrack.jetbrains.com/issue/KT-55982) K2: Consider global lookups from plugins in incremental compilation
- [`KT-75657`](https://youtrack.jetbrains.com/issue/KT-75657) Fix difference in incremental compilation scenarios in BTA in-process vs daemon compilation mode
- [`KT-79541`](https://youtrack.jetbrains.com/issue/KT-79541) Refactor tracking of files relation in IC
- [`KT-74628`](https://youtrack.jetbrains.com/issue/KT-74628) Incremental compilation runner does not check compiler exit code before mapping sources to classes

### Tools. JPS

- [`KT-77347`](https://youtrack.jetbrains.com/issue/KT-77347) Support file-less compatible IC approach

### Tools. Kapt

- [`KT-73411`](https://youtrack.jetbrains.com/issue/KT-73411) Remove `kapt.use.k2` property and code which allows to use K2 with K1 kapt
- [`KT-79138`](https://youtrack.jetbrains.com/issue/KT-79138) K2: KAPT Java Stub Gen: `Unresolved reference` with `@kotlin`.Metadata in Java in 2.2.0
- [`KT-79641`](https://youtrack.jetbrains.com/issue/KT-79641) Kapt: too much information is printed in verbose mode
- [`KT-79305`](https://youtrack.jetbrains.com/issue/KT-79305) K2 kapt: ISE "Cannot evaluate IR expression in annotation" on typealias with unresolved expansion
- [`KT-79136`](https://youtrack.jetbrains.com/issue/KT-79136) K2 kapt: unresolved nested class references in annotation arguments are generated without outer class names
- [`KT-79133`](https://youtrack.jetbrains.com/issue/KT-79133) K2 kapt: class literal with typealias is not expanded

### Tools. Performance benchmarks

- [`KT-79709`](https://youtrack.jetbrains.com/issue/KT-79709) Add `-Xdetailed-perf` CLI flag to control verbosity of performance logs
- [`KT-79226`](https://youtrack.jetbrains.com/issue/KT-79226) [K/N] Add performance measurement for native backend lowerings

### Tools. Statistics (FUS)

- [`KT-77407`](https://youtrack.jetbrains.com/issue/KT-77407) Add performance measurement for prefix lowerings
- [`KT-79455`](https://youtrack.jetbrains.com/issue/KT-79455) [FUS] Collect KSP plugin version
- [`KT-79090`](https://youtrack.jetbrains.com/issue/KT-79090) Integrate dynamic stats into `MarkdownReportRenderer`

### Tools. Wasm

- [`KT-80582`](https://youtrack.jetbrains.com/issue/KT-80582) Multiple reloads when using webpack dev server after 2.2.20-Beta2
- [`KT-80896`](https://youtrack.jetbrains.com/issue/KT-80896) K/Wasm: debug tests only once
- [`KT-78921`](https://youtrack.jetbrains.com/issue/KT-78921) K/Wasm: don't generate empty yarn.lock file


## 2.2.20

### Analysis API

- [`KT-78187`](https://youtrack.jetbrains.com/issue/KT-78187) Synthetic properties not to be shown as callables
- [`KT-72525`](https://youtrack.jetbrains.com/issue/KT-72525) K2. red code and KIWA on new-lines in guarded when conditions (with parentheses)
- [`KT-74246`](https://youtrack.jetbrains.com/issue/KT-74246) KaVisibilityChecker.isVisible is inefficient with multiple calls on the same use-site

### Analysis API. Code Compilation

- [`KT-78382`](https://youtrack.jetbrains.com/issue/KT-78382) K2 IR lowering error when interface extends interface
- [`KT-73201`](https://youtrack.jetbrains.com/issue/KT-73201) K2 IDE: Error while evaluating expressions with local classes
- [`KT-78164`](https://youtrack.jetbrains.com/issue/KT-78164) Evaluator: '`@JvmName`' annotations are not recognized in other modules
- [`KT-76457`](https://youtrack.jetbrains.com/issue/KT-76457) K2 IDE / KMP Debugger: KISEWA “Cannot compile a common source without a JVM counterpart” on evaluating inline fun from common module inside jvm
- [`KT-73084`](https://youtrack.jetbrains.com/issue/KT-73084) K2 evaluator cannot resolve local variables standing at the closing brace

### Analysis API. FIR

#### Performance Improvements

- [`KT-76490`](https://youtrack.jetbrains.com/issue/KT-76490) Do not load ast during the contracts phase if no contracts present
- [`KT-78132`](https://youtrack.jetbrains.com/issue/KT-78132) Do not check FirElementBuilder#tryGetFirWithoutBodyResolve optimization for already resolved declarations

#### Fixes

- [`KT-72227`](https://youtrack.jetbrains.com/issue/KT-72227) SOE from recursive value class
- [`KT-68977`](https://youtrack.jetbrains.com/issue/KT-68977) K2 IDE: Reference to companion object through typealias in a function call does not work
- [`KT-72357`](https://youtrack.jetbrains.com/issue/KT-72357) Implement partial body resolution
- [`KT-76932`](https://youtrack.jetbrains.com/issue/KT-76932) Support context parameters on dangling modifier list
- [`KT-72407`](https://youtrack.jetbrains.com/issue/KT-72407) FirImplementationByDelegationWithDifferentGenericSignatureChecker: FirLazyExpression should be calculated before accessing
- [`KT-77602`](https://youtrack.jetbrains.com/issue/KT-77602) K2 / Analysis API: KAEWA “No fir element was found for KtParameter” on incorrect context()-call
- [`KT-77629`](https://youtrack.jetbrains.com/issue/KT-77629) K2: NPE: "org.jetbrains.kotlin.fir.java.declarations.FirJavaTypeParameter.performFirstRoundOfBoundsResolution"
- [`KT-76855`](https://youtrack.jetbrains.com/issue/KT-76855) Analysis API: `KaType.asPsiType` returns `null` for a local inner class in dependent analysis tests
- [`KT-72718`](https://youtrack.jetbrains.com/issue/KT-72718) ImplicitReceiverValue.createSnapshot creates invalid FIR if receiver is smart-casted
- [`KT-76811`](https://youtrack.jetbrains.com/issue/KT-76811) Analysis API: `resolveToFirSymbol` finds a `FirPropertySymbol` for a `KtScript` in dependent analysis
- [`KT-73586`](https://youtrack.jetbrains.com/issue/KT-73586) [Analysis API] Add `lazyResolveToPhase(STATUS)` before accessing modifiers of members
- [`KT-71135`](https://youtrack.jetbrains.com/issue/KT-71135) AA: exception from sealed inheritors checker when `analyzeCopy`
- [`KT-75534`](https://youtrack.jetbrains.com/issue/KT-75534) K2 AA: "Containing declaration should present for nested declaration class KtNamedFunction" with dangling annotation on top-level anonymous function
- [`KT-75687`](https://youtrack.jetbrains.com/issue/KT-75687) K2: local variable doesn't get to the do-while scope
- [`KT-56543`](https://youtrack.jetbrains.com/issue/KT-56543) LL FIR: rework lazy transformers so transformers modify only declarations they suppose to

### Analysis API. Infrastructure

- [`KT-76809`](https://youtrack.jetbrains.com/issue/KT-76809) Analysis API: Dependent analysis tests frequently work with the original element instead of the copied element

### Analysis API. Light Classes

- [`KT-78835`](https://youtrack.jetbrains.com/issue/KT-78835) Find usages of a light constructor from a class with an empty body finds usages of class as well
- [`KT-78878`](https://youtrack.jetbrains.com/issue/KT-78878) K2. Method shown as unavailable in Java when `@JvmExposeBoxed` is applied (redundantly) at both class and method level in Kotlin
- [`KT-78065`](https://youtrack.jetbrains.com/issue/KT-78065) Support "Expose boxed inline value classes" in Light Classes
- [`KT-78076`](https://youtrack.jetbrains.com/issue/KT-78076) DLC: KotlinDeclarationInCompiledFileSearcher missed accessors if types are boxed
- [`KT-77569`](https://youtrack.jetbrains.com/issue/KT-77569) SLC: annotation missing from generated no-args constructor
- [`KT-75182`](https://youtrack.jetbrains.com/issue/KT-75182) K2 AA. False positive red code "Unresolved reference" to a Kotlin method in Java when Kotlin uses a value class with `@JvmOverloads`
- [`KT-77564`](https://youtrack.jetbrains.com/issue/KT-77564) Constructor with JvmOverloads and value class shouldn't mark regular constructors private
- [`KT-77505`](https://youtrack.jetbrains.com/issue/KT-77505) K2: find usages on java accessor methods do not detect kotlin property accessor usages
- [`KT-76789`](https://youtrack.jetbrains.com/issue/KT-76789) Annotation resolve shouldn't search through non-class members
- [`KT-76907`](https://youtrack.jetbrains.com/issue/KT-76907) Wrong equality between repeatable annotation and container

### Analysis API. Providers and Caches

- [`KT-77578`](https://youtrack.jetbrains.com/issue/KT-77578) Analysis API: Performance degradation of `KaBaseResolutionScope.contains` after introduction of library restriction scopes
- [`KT-78640`](https://youtrack.jetbrains.com/issue/KT-78640) Analysis API: Remove "friend builtins provider" from `FirDeclarationForCompiledElementSearcher`
- [`KT-74907`](https://youtrack.jetbrains.com/issue/KT-74907) Analysis API: Apply platform-based library module content restrictions consistently
- [`KT-77605`](https://youtrack.jetbrains.com/issue/KT-77605) AA: Leaking KaDanglingFileModule through IdeKotlinPackageProvider
- [`KT-62474`](https://youtrack.jetbrains.com/issue/KT-62474) Analysis API: Improve mergeability and performance of custom search scopes
- [`KT-77022`](https://youtrack.jetbrains.com/issue/KT-77022) Get rid of ExpectBuiltinPostProcessor workaround
- [`KT-77248`](https://youtrack.jetbrains.com/issue/KT-77248) Delegation of `JavaModuleResolver` is restricted to `CliJavaModuleResolver`
- [`KT-76850`](https://youtrack.jetbrains.com/issue/KT-76850) LLFirLibrarySession cannot be cast to LLFirResolvableModuleSession
- [`KT-76952`](https://youtrack.jetbrains.com/issue/KT-76952) Analysis API: `when` exhaustiveness analysis fails for sealed classes in dangling files
- [`KT-72390`](https://youtrack.jetbrains.com/issue/KT-72390) Kotlin project full of red code

### Analysis API. Standalone

- [`KT-78638`](https://youtrack.jetbrains.com/issue/KT-78638) Analysis API Standalone: Stdlib builtins are not indexed in `STUBS` deserialized declaration origin mode

### Analysis API. Stubs and Decompilation

- [`KT-77496`](https://youtrack.jetbrains.com/issue/KT-77496) Support HAS_MUST_USE_RETURN_VALUE metadata flags in FirStubBasedMemberDeserializer
- [`KT-77778`](https://youtrack.jetbrains.com/issue/KT-77778) Function receivers doesn't have annotations
- [`KT-77777`](https://youtrack.jetbrains.com/issue/KT-77777) Receiver annotations shouldn't be present on types
- [`KT-77538`](https://youtrack.jetbrains.com/issue/KT-77538) Support default property accessors with annotations
- [`KT-77763`](https://youtrack.jetbrains.com/issue/KT-77763) Decompiled stubs miss inline modifier for property accessors
- [`KT-77309`](https://youtrack.jetbrains.com/issue/KT-77309) Decompiled property from annotation constructor with default value should have a constant initializer
- [`KT-77168`](https://youtrack.jetbrains.com/issue/KT-77168) Prefer DataInputOutputUtil for serialization/deserialization
- [`KT-77117`](https://youtrack.jetbrains.com/issue/KT-77117) Flaky WRONG_ANNOTATION_TARGET diagnostic
- [`KT-76791`](https://youtrack.jetbrains.com/issue/KT-76791) Function signature types are deserialized inconsistently
- [`KT-76947`](https://youtrack.jetbrains.com/issue/KT-76947) Support functional types with context parameters

### Analysis API. Surface

#### New Features

- [`KT-73473`](https://youtrack.jetbrains.com/issue/KT-73473) Provide KaExpressionInformationProvider.isUsedAsResultOfLambda
- [`KT-77278`](https://youtrack.jetbrains.com/issue/KT-77278) Implement psi-based `KaFirKotlinPropertyKtPropertyBasedSymbol#hasBackingField`
- [`KT-70770`](https://youtrack.jetbrains.com/issue/KT-70770) KaLocalVariableSymbol: support `isLateInit`

#### Performance Improvements

- [`KT-78526`](https://youtrack.jetbrains.com/issue/KT-78526) Get rid of redundant `checkValidity` from `withPsiValidityAssertion`

#### Fixes

- [`KT-77674`](https://youtrack.jetbrains.com/issue/KT-77674) Analysis API: Redundant smart cast to the original type
- [`KT-76577`](https://youtrack.jetbrains.com/issue/KT-76577) Guard KaFirStopWorldCacheCleaner from deadlocks via threads waiting
- [`KT-78820`](https://youtrack.jetbrains.com/issue/KT-78820) K2: ISE "FIR element class FirErrorExpressionImpl is not supported in constant evaluation" through RedundantValueArgumentInspection
- [`KT-75057`](https://youtrack.jetbrains.com/issue/KT-75057) Analysis API: Reference to object through typealias in invoke operator call leads to original type
- [`KT-79042`](https://youtrack.jetbrains.com/issue/KT-79042) Do not restore KaTypePointer if target kind has changed
- [`KT-72421`](https://youtrack.jetbrains.com/issue/KT-72421) AA: "KtReference.resolveToSymbols" returns empty list when ASSIGN_OPERATOR_AMBGUITY error is present
- [`KT-63464`](https://youtrack.jetbrains.com/issue/KT-63464) AA: KtPsiTypeProvider#asPsiType doesn't substitute kotlin.Unit
- [`KT-75913`](https://youtrack.jetbrains.com/issue/KT-75913) K2: SymbolLightLazyAnnotation evaluates arguments and replaces them with constants
- [`KT-78628`](https://youtrack.jetbrains.com/issue/KT-78628) K2. Setting Receiver=true in Change Signature produces parameter of regular function type receiver instead of extension function type
- [`KT-78278`](https://youtrack.jetbrains.com/issue/KT-78278) ISE: FIR element "class org.jetbrains.kotlin.fir.expressions.impl.FirErrorResolvedQualifierImpl" is not supported in constant evaluation at org.jetbrains.uast.kotlin.internal.FirKotlinUastConstantEvaluator.evaluate
- [`KT-73184`](https://youtrack.jetbrains.com/issue/KT-73184) Analysis API: KaFunctionCall﻿.argumentMapping is unexpectedly deparenthesised
- [`KT-73327`](https://youtrack.jetbrains.com/issue/KT-73327) Cover all psi inputs with scope validity assertions
- [`KT-78613`](https://youtrack.jetbrains.com/issue/KT-78613) PSI: add binary compatibility checks
- [`KT-74013`](https://youtrack.jetbrains.com/issue/KT-74013) Analysis API: Cover the API surface with `@SubclassOptInRequired` annotations
- [`KT-76614`](https://youtrack.jetbrains.com/issue/KT-76614) Move the parser and lexer to a separate module
- [`KT-78552`](https://youtrack.jetbrains.com/issue/KT-78552) `KaFunctionValueParameter` is not marked as `KaLifetimeOwner`
- [`KT-71152`](https://youtrack.jetbrains.com/issue/KT-71152) Add back SubclassOptInRequired to classes in KaModule.kt
- [`KT-71876`](https://youtrack.jetbrains.com/issue/KT-71876) Support storing parameter names in `KaFunctionType`
- [`KT-77738`](https://youtrack.jetbrains.com/issue/KT-77738) AA: inconsistent `KaType.allSupertypes` regarding multiple iterations
- [`KT-75358`](https://youtrack.jetbrains.com/issue/KT-75358) K2 AA, KaFirVisibilityChecker: private member of anonymous object is not visible inside it
- [`KT-73723`](https://youtrack.jetbrains.com/issue/KT-73723) K2 AA, KaFirVisibilityChecker: protected member of superclass is not visible from anonymous object
- [`KT-78057`](https://youtrack.jetbrains.com/issue/KT-78057) [Analysis API, K2] Context parameters are not resolved in KDoc
- [`KT-73758`](https://youtrack.jetbrains.com/issue/KT-73758) K2 Mode: "KaEvaluator.evaluate" does not work for simple arithmetic expressions
- [`KT-72301`](https://youtrack.jetbrains.com/issue/KT-72301) K2 AA. `PSI should present for declaration built by Kotlin code` on property access syntax of generic Java getter through Kotlin subclass
- [`KT-77730`](https://youtrack.jetbrains.com/issue/KT-77730) K2: Unable to get a light PSI for a nested annotation used with fully-qualified name
- [`KT-73216`](https://youtrack.jetbrains.com/issue/KT-73216) K2: unresolvable references in type parameters
- [`KT-71794`](https://youtrack.jetbrains.com/issue/KT-71794) Analysis API: Types with errors have unresolved qualifiers in lambda parameters position
- [`KT-65846`](https://youtrack.jetbrains.com/issue/KT-65846) Support parameter names in functional type rendering
- [`KT-76738`](https://youtrack.jetbrains.com/issue/KT-76738) K2 AA: rendering constructor of sealed class inserts protected modifier
- [`KT-77515`](https://youtrack.jetbrains.com/issue/KT-77515) `KaTypeProvider#receiverType` should be more tolerant to an error code
- [`KT-77333`](https://youtrack.jetbrains.com/issue/KT-77333) K2 AA: KaFirTypeProvider.getType: InvalidFirElementTypeException: For TYPE_REFERENCE with text `I`, unexpected element of type: FirSuperReceiverExpressionImpl found
- [`KT-76044`](https://youtrack.jetbrains.com/issue/KT-76044) K2 AA: isFun is true for restored symbol of Java interface with several methods
- [`KT-77264`](https://youtrack.jetbrains.com/issue/KT-77264) `KaTypeProvider#type` should be more tolerant to an error code
- [`KT-77282`](https://youtrack.jetbrains.com/issue/KT-77282) KaPropertySymbol: support `isDelegatedProperty` for libraries
- [`KT-77254`](https://youtrack.jetbrains.com/issue/KT-77254) K2 AA: expectedType doesn't provide anything for parameter default value
- [`KT-74777`](https://youtrack.jetbrains.com/issue/KT-74777) KaVariableSymbol.hasBackingField returns incorrect result for libraries
- [`KT-77280`](https://youtrack.jetbrains.com/issue/KT-77280) Rename `KaPropertyAccessorSymbol#isCustom` to `isNotDefault`
- [`KT-77210`](https://youtrack.jetbrains.com/issue/KT-77210) Analysis API: `scopeContext` shows implicit receiver with a class instance in the class constructor
- [`KT-77196`](https://youtrack.jetbrains.com/issue/KT-77196) Clarify differences between KaPropertyAccessorSymbol#{isDefault, hasBody}
- [`KT-76580`](https://youtrack.jetbrains.com/issue/KT-76580) K2: No expected type for the second+ vararg argument
- [`KT-76750`](https://youtrack.jetbrains.com/issue/KT-76750) K2. internal exception  'Unable to provide inlay hint' on typo in nested lambdas
- [`KT-73290`](https://youtrack.jetbrains.com/issue/KT-73290) Analysis API: Improve the architecture of content scopes and resolution scopes
- [`KT-73055`](https://youtrack.jetbrains.com/issue/KT-73055) Get rid of the deprecated Analysis API API
- [`KT-70199`](https://youtrack.jetbrains.com/issue/KT-70199) K2: ConcurrentModificationException at FirCallCompleter$LambdaAnalyzerImpl.analyzeAndGetLambdaReturnArguments

### Backend. Wasm

#### New Features

- [`KT-65721`](https://youtrack.jetbrains.com/issue/KT-65721) K/Wasm: stop unconditionally exporting any main function from the root package

#### Performance Improvements

- [`KT-70097`](https://youtrack.jetbrains.com/issue/KT-70097) Optimize shared primitive variables in Native and Wasm

#### Fixes

- [`KT-80106`](https://youtrack.jetbrains.com/issue/KT-80106) devServer in Kotlin/Wasm overwrites defaults, causing missing static paths
- [`KT-80018`](https://youtrack.jetbrains.com/issue/KT-80018) K/Wasm: exceptions don't work properly in JavaScriptCore (vm inside Safari, WebKit)
- [`KT-66072`](https://youtrack.jetbrains.com/issue/KT-66072) K/Wasm: improve how exceptions work in JS interop
- [`KT-77897`](https://youtrack.jetbrains.com/issue/KT-77897) WasmJs: ClassCastException when using star-projection with nullable transformation in generic extension function
- [`KT-71533`](https://youtrack.jetbrains.com/issue/KT-71533) K/Wasm + K2: no error on KClass::qualifiedName usages
- [`KT-73931`](https://youtrack.jetbrains.com/issue/KT-73931) WASM: "RuntimeError: illegal cast" with nullable generic
- [`KT-65403`](https://youtrack.jetbrains.com/issue/KT-65403) [WASM] RuntimeError is thrown instead of ClassCastException
- [`KT-79317`](https://youtrack.jetbrains.com/issue/KT-79317) [Wasm] Do not throw CCE for ExcludedFromCodegen declarations
- [`KT-66085`](https://youtrack.jetbrains.com/issue/KT-66085) K/WASM: Runtime error is uncaught with `catch (e: Throwable)`
- [`KT-78036`](https://youtrack.jetbrains.com/issue/KT-78036) K/Wasm: generate a message with "expected" and "actual" types in case of CCE
- [`KT-78384`](https://youtrack.jetbrains.com/issue/KT-78384) K/Wasm: Incorrect debug info of local declarations in inline function from another file
- [`KT-72220`](https://youtrack.jetbrains.com/issue/KT-72220) Wasm: Unclear exception in case of missed dependency
- [`KT-71691`](https://youtrack.jetbrains.com/issue/KT-71691) No trace on Wasm/JS if an error occurred in initializing global variables in a file with the main function
- [`KT-67554`](https://youtrack.jetbrains.com/issue/KT-67554) [Wasm] Consider to have reference equals or/and equals for function references
- [`KT-71521`](https://youtrack.jetbrains.com/issue/KT-71521) K/Wasm: incorrect results on equality checks for capturing property references
- [`KT-71522`](https://youtrack.jetbrains.com/issue/KT-71522) K/Wasm: incorrect results on equality checks for function references
- [`KT-69570`](https://youtrack.jetbrains.com/issue/KT-69570) K/Wasm: JsExport with default parameter value compiles to invalid Wasm
- [`KT-71517`](https://youtrack.jetbrains.com/issue/KT-71517) K/Wasm: KClass::qualifiedName for local classes and objects returns non-null value
- [`KT-68309`](https://youtrack.jetbrains.com/issue/KT-68309) WASM: Anonymous class simpleName returns "<no name provided>" instead of null
- [`KT-77272`](https://youtrack.jetbrains.com/issue/KT-77272) K/Wasm: Remove kotlin.wasm.internal.ClosureBox* classes from the standard library
- [`KT-66106`](https://youtrack.jetbrains.com/issue/KT-66106) Wasm: lambda was not invoked in test lambda2.kt
- [`KT-77855`](https://youtrack.jetbrains.com/issue/KT-77855) [Wasm] Improve virtual function calls speed for lambdas
- [`KT-77501`](https://youtrack.jetbrains.com/issue/KT-77501) Wasm: unsigned vararg compiles to invalid Wasm
- [`KT-76775`](https://youtrack.jetbrains.com/issue/KT-76775) [Wasm] Inconsistent FP mod operation
- [`KT-77464`](https://youtrack.jetbrains.com/issue/KT-77464) Wasm: KType.toString() has simple names even with -Xwasm-kclass-fqn
- [`KT-77465`](https://youtrack.jetbrains.com/issue/KT-77465) Wasm: KTypeParamter printed without variance information

### Compiler

#### New Features

- [`KT-71768`](https://youtrack.jetbrains.com/issue/KT-71768) Enable -Xjvm-default=all-compatibility by default to generate JVM default interface methods
- [`KT-78374`](https://youtrack.jetbrains.com/issue/KT-78374) Make indy lambda function name generation more consistent
- [`KT-45683`](https://youtrack.jetbrains.com/issue/KT-45683) Allow generics in contract type assertions
- [`KT-27090`](https://youtrack.jetbrains.com/issue/KT-27090) Support contracts in getter and setter for top-level extension properties
- [`KT-76766`](https://youtrack.jetbrains.com/issue/KT-76766) Warning is missing for wrong subclass checking
- [`KT-71244`](https://youtrack.jetbrains.com/issue/KT-71244) Incorporate existing `@CheckReturnValue` annotation(s) into Kotlin's unused return value checker
- [`KT-73256`](https://youtrack.jetbrains.com/issue/KT-73256) Implement `all` meta-target for annotations
- [`KT-78792`](https://youtrack.jetbrains.com/issue/KT-78792) Report warning for redundant return in expression body
- [`KT-32313`](https://youtrack.jetbrains.com/issue/KT-32313) Support contracts for operator functions
- [`KT-70722`](https://youtrack.jetbrains.com/issue/KT-70722) Implement better Kotlin warnings for value classes and JEP 390 (Warnings for Value-Based Classes)
- [`KT-65688`](https://youtrack.jetbrains.com/issue/KT-65688) Generate when-expressions over final classes via invokedynamic typeSwitch + tableswitch on JDK 21+
- [`KT-54344`](https://youtrack.jetbrains.com/issue/KT-54344) Trigger the unused expression warning for interpolated strings, even when the expression may have side effects
- [`KT-74807`](https://youtrack.jetbrains.com/issue/KT-74807) Implement 'full' unused return value checker mode
- [`KT-77653`](https://youtrack.jetbrains.com/issue/KT-77653) K/N: an optimization pass to remove redundant type checks
- [`KT-64477`](https://youtrack.jetbrains.com/issue/KT-64477) Enhance KotlinLightParser to make it able to parse scripts
- [`KT-74809`](https://youtrack.jetbrains.com/issue/KT-74809) Support unnamed local variables
- [`KT-72941`](https://youtrack.jetbrains.com/issue/KT-72941) ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE missing in K2
- [`KT-75061`](https://youtrack.jetbrains.com/issue/KT-75061) Support context-sensitive resolution in type position

#### Performance Improvements

- [`KT-77993`](https://youtrack.jetbrains.com/issue/KT-77993) Optimize old PSI/LightTree Kotlin parser
- [`KT-78672`](https://youtrack.jetbrains.com/issue/KT-78672) Consider having FirCallableSymbol.callableId null for local properties / parameters
- [`KT-77839`](https://youtrack.jetbrains.com/issue/KT-77839) K2: consider not creating CallableId for value parameters / variables / fields
- [`KT-74981`](https://youtrack.jetbrains.com/issue/KT-74981) Kotlin/Native: large binary size for iOS target in 2.1.0(LLVM16)
- [`KT-77838`](https://youtrack.jetbrains.com/issue/KT-77838) K2: consider replacing LinkedHashMap with HashMap inside scopes and scope session
- [`KT-76698`](https://youtrack.jetbrains.com/issue/KT-76698) Android Studio compose preview holds read lock 700ms for KaCompilerFacility API
- [`KT-68677`](https://youtrack.jetbrains.com/issue/KT-68677) Kotlin compilation issue when using EnumMap and Pair

#### Fixes

- [`KT-79979`](https://youtrack.jetbrains.com/issue/KT-79979) K2: ClassCastException when overriding extension property with delegation
- [`KT-67146`](https://youtrack.jetbrains.com/issue/KT-67146) `UPPER_BOUND_VIOLATED` missing on implicit type arguments
- [`KT-76477`](https://youtrack.jetbrains.com/issue/KT-76477) Kotlin/Native: fix compiler performance reporting in sources->klib and klibs->binary
- [`KT-79866`](https://youtrack.jetbrains.com/issue/KT-79866) kotlinc 2.2.0 silently emits 'NonExistentClass' instead of reporting an error
- [`KT-78666`](https://youtrack.jetbrains.com/issue/KT-78666) "Platform declaration clash" caused by indy lambda name generation which generates conflicting names
- [`KT-80285`](https://youtrack.jetbrains.com/issue/KT-80285) IJ monorepo: broken compilation after 2.2.20-RC update
- [`KT-79442`](https://youtrack.jetbrains.com/issue/KT-79442) "Multiple annotations of type kotlin.coroutines.jvm.internal.DebugMetadata": 2.2.0-Beta1 generates broken code with JVM default suspend methods in interfaces
- [`KT-78589`](https://youtrack.jetbrains.com/issue/KT-78589) "Class does not have member field" caused by  delegation from a Java to Kotlin class
- [`KT-79816`](https://youtrack.jetbrains.com/issue/KT-79816) Java Interfaces implemented by delegation have non-null return checks
- [`KT-78097`](https://youtrack.jetbrains.com/issue/KT-78097)  False positive NO_ELSE_IN_WHEN on sealed interface with negative is check
- [`KT-77182`](https://youtrack.jetbrains.com/issue/KT-77182) A function in a file annotated with `@file`:MustUseReturnValue doesn't produce a warning when it is used from compiled code
- [`KT-79085`](https://youtrack.jetbrains.com/issue/KT-79085) Adding `-Xreturn-value-checker=full` to kotlinc causes "error: conflicting overloads"
- [`KT-75268`](https://youtrack.jetbrains.com/issue/KT-75268) K2: Implement the new compilation scheme for MPP (compiler part)
- [`KT-78843`](https://youtrack.jetbrains.com/issue/KT-78843) FIR tree: comments within String concatenation aren't visited in 2.2.0
- [`KT-77401`](https://youtrack.jetbrains.com/issue/KT-77401) [FIR] `ParameterNameTypeAttribute.name` doesn't support `@ParameterName` with compile-time constant property argument
- [`KT-73611`](https://youtrack.jetbrains.com/issue/KT-73611) Remove -Xextended-compiler-checks in favor of a deprecation cycle
- [`KT-79276`](https://youtrack.jetbrains.com/issue/KT-79276) Dexing fails with "Cannot read field X because <local0> is null" with 2.2.0
- [`KT-79781`](https://youtrack.jetbrains.com/issue/KT-79781) Missing MISSING_DEPENDENCY_CLASS when using type alias with inaccessible RHS
- [`KT-78621`](https://youtrack.jetbrains.com/issue/KT-78621) false-positive type mismatch error on value of nullable type as value of platform type
- [`KT-79547`](https://youtrack.jetbrains.com/issue/KT-79547) "UnsupportedOperationException: Not supported" with inlining and value classes
- [`KT-52706`](https://youtrack.jetbrains.com/issue/KT-52706) Bad signature for generic value classes with substituted type parameter
- [`KT-79519`](https://youtrack.jetbrains.com/issue/KT-79519) Nested type alias is unreachable from another module
- [`KT-76839`](https://youtrack.jetbrains.com/issue/KT-76839) False-negative MISSING_DEPENDENCY_CLASS on parameter of data class constructor
- [`KT-78352`](https://youtrack.jetbrains.com/issue/KT-78352) False-positive IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE when comparing with equality operator (==)
- [`KT-78815`](https://youtrack.jetbrains.com/issue/KT-78815) `Symbol not found: __ZNSt3__117bad_function_callD1Ev` error on iOS 15.5 simulator in Xcode 16.3 after update to 2.2.0-Beta2
- [`KT-25341`](https://youtrack.jetbrains.com/issue/KT-25341) NOT_YET_SUPPORTED_IN_INLINE reported over anonymous object border
- [`KT-77099`](https://youtrack.jetbrains.com/issue/KT-77099) 'all' annotation target is not a soft keyword
- [`KT-76478`](https://youtrack.jetbrains.com/issue/KT-76478) FIR: Implement IDE-only checker for types exposed in inline function
- [`KT-79355`](https://youtrack.jetbrains.com/issue/KT-79355) Failed to fix the problem of desugared `inc` with new reverse implies returns contract
- [`KT-79277`](https://youtrack.jetbrains.com/issue/KT-79277) Implies returns contract doesn't affect the return type of the function if it is in the argument position
- [`KT-79271`](https://youtrack.jetbrains.com/issue/KT-79271) Implies returns contract doesn't impact exhaustiveness
- [`KT-79218`](https://youtrack.jetbrains.com/issue/KT-79218) SMARTCAST_IMPOSSIBLE for top‑level extension‑property getter despite returnsNotNull contract
- [`KT-79220`](https://youtrack.jetbrains.com/issue/KT-79220) returnsNotNull contract ignored on extension function with nullable receiver
- [`KT-79354`](https://youtrack.jetbrains.com/issue/KT-79354) IllegalStateException: Debug metadata version mismatch. Expected: 1, got 2 with compiler 2.2.20-Beta1 and stdlib 2.2.0
- [`KT-78479`](https://youtrack.jetbrains.com/issue/KT-78479)  IR lowering failed / Unexpected null argument for composable call
- [`KT-77986`](https://youtrack.jetbrains.com/issue/KT-77986) K2: False negative: "Local classes are not yet supported in inline functions"
- [`KT-79076`](https://youtrack.jetbrains.com/issue/KT-79076) 'IllegalStateException: Cannot serialize error type: ERROR CLASS: Uninferred type' with Exposed column using recursive generic type
- [`KT-78726`](https://youtrack.jetbrains.com/issue/KT-78726) Split runPsiToIr phase into runPsiToIr and runIrLinker
- [`KT-77672`](https://youtrack.jetbrains.com/issue/KT-77672) K/N: come up with a fallback strategy for the casts optimization pass
- [`KT-76365`](https://youtrack.jetbrains.com/issue/KT-76365) K2: Missing ABSTRACT_SUPER_CALL
- [`KT-76585`](https://youtrack.jetbrains.com/issue/KT-76585) K2: RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY is not reported inside initializers of local variables
- [`KT-79099`](https://youtrack.jetbrains.com/issue/KT-79099) K2: Do not inherit inline modifier
- [`KT-76902`](https://youtrack.jetbrains.com/issue/KT-76902) Omit type-use annotations from diagnostics
- [`KT-64499`](https://youtrack.jetbrains.com/issue/KT-64499) Report error on overloading by order of context parameters
- [`KT-58988`](https://youtrack.jetbrains.com/issue/KT-58988) K2: Deprecate exposing package-private parameter of internal method
- [`KT-77199`](https://youtrack.jetbrains.com/issue/KT-77199) OPT_IN_USAGE_ERROR is still absent when calling the enum primary constructor
- [`KT-72800`](https://youtrack.jetbrains.com/issue/KT-72800) K2: java.util.NoSuchElementException when introduce variable
- [`KT-79056`](https://youtrack.jetbrains.com/issue/KT-79056) Add experimental language version 2.5
- [`KT-17460`](https://youtrack.jetbrains.com/issue/KT-17460) Diagnostics and intention on suspend function that is overriden with non-suspend one.
- [`KT-78351`](https://youtrack.jetbrains.com/issue/KT-78351) Plugins: VIRTUAL_MEMBER_HIDDEN caused by FirSupertypeGenerationExtension
- [`KT-78527`](https://youtrack.jetbrains.com/issue/KT-78527) No LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING is reported when a private companion object is accessed via the class name
- [`KT-79045`](https://youtrack.jetbrains.com/issue/KT-79045) FirExpectActualMatcherTransformer should not visit bodies
- [`KT-74570`](https://youtrack.jetbrains.com/issue/KT-74570) K2: Linenumber for annotation on property is present in LVT
- [`KT-74569`](https://youtrack.jetbrains.com/issue/KT-74569) K2: Linenumber of annotation is present in constructor's LVT
- [`KT-64731`](https://youtrack.jetbrains.com/issue/KT-64731) K2: Annotation on inline function or inside inline function is hit by debugger
- [`KT-77756`](https://youtrack.jetbrains.com/issue/KT-77756) Add experimental language version 2.4
- [`KT-78837`](https://youtrack.jetbrains.com/issue/KT-78837) linkReleaseFrameworkIosArm64: Compilation failed: An interface expected but was Any
- [`KT-78945`](https://youtrack.jetbrains.com/issue/KT-78945) CONTRACT_NOT_ALLOWED is not reported for local operator functions
- [`KT-78944`](https://youtrack.jetbrains.com/issue/KT-78944) ANNOTATION_IN_CONTRACT_ERROR is not reported for operators and property accessors with contracts
- [`KT-78943`](https://youtrack.jetbrains.com/issue/KT-78943) ERROR_IN_CONTRACT_DESCRIPTION is not reported for operators and property accessors with contracts
- [`KT-78932`](https://youtrack.jetbrains.com/issue/KT-78932) Contracts are allowed for open and overridden property accessors
- [`KT-77203`](https://youtrack.jetbrains.com/issue/KT-77203) FIR: Consider adding destructured type to all COMPONENT_FUNCTION_* diagnostics
- [`KT-76635`](https://youtrack.jetbrains.com/issue/KT-76635) Implement Data-Flow Based Exhaustiveness Support
- [`KT-78805`](https://youtrack.jetbrains.com/issue/KT-78805) K2: False positive METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE
- [`KT-78651`](https://youtrack.jetbrains.com/issue/KT-78651) No need to report LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING in noinline default value lambda
- [`KT-78849`](https://youtrack.jetbrains.com/issue/KT-78849) K2: [Wasm, Fir2IR] Invalid smartcast on overloaded function call
- [`KT-78793`](https://youtrack.jetbrains.com/issue/KT-78793) Make feature AllowEagerSupertypeAccessibilityChecks experimental
- [`KT-78736`](https://youtrack.jetbrains.com/issue/KT-78736) Missing [NOT_YET_SUPPORTED_IN_INLINE] diagnostics because of incorrect context update
- [`KT-78324`](https://youtrack.jetbrains.com/issue/KT-78324) K2: False negative [INCONSISTENT_TYPE_PARAMETER_VALUES]
- [`KT-69975`](https://youtrack.jetbrains.com/issue/KT-69975) KDoc: cannot reference elements with names in backticks
- [`KT-78229`](https://youtrack.jetbrains.com/issue/KT-78229) KDoc: unable to reference a method with spaces in the name
- [`KT-78047`](https://youtrack.jetbrains.com/issue/KT-78047) Render unnamed context parameters as _ instead of <unused var>
- [`KT-74621`](https://youtrack.jetbrains.com/issue/KT-74621) Debugger: AssertionError on evaluating two suspending calls
- [`KT-78784`](https://youtrack.jetbrains.com/issue/KT-78784) Improve deprecation warnings about KTLC-284
- [`KT-76826`](https://youtrack.jetbrains.com/issue/KT-76826) New inference error [NewConstraintError at Incorporate TypeVariable] caused by recursive generics and nullable expected type
- [`KT-77685`](https://youtrack.jetbrains.com/issue/KT-77685) "IllegalArgumentException: Sequence contains more than one matching element"
- [`KT-78028`](https://youtrack.jetbrains.com/issue/KT-78028) "FirNamedFunctionSymbol" leaks to the error message about missing infix modifier
- [`KT-77245`](https://youtrack.jetbrains.com/issue/KT-77245) Add expression name to RETURN_VALUE_NOT_USED diagnostic
- [`KT-78071`](https://youtrack.jetbrains.com/issue/KT-78071) False-positive NO_ELSE_IN_WHEN after variable reassignment
- [`KT-78068`](https://youtrack.jetbrains.com/issue/KT-78068) False-positive NO_ELSE_IN_WHEN after excluding enum value with inequality check
- [`KT-71134`](https://youtrack.jetbrains.com/issue/KT-71134) Consider to get rid of CapturedTypeMarker.withNotNullProjection()
- [`KT-77131`](https://youtrack.jetbrains.com/issue/KT-77131) getValue/setValue can be declared with more than two/three parameters
- [`KT-78452`](https://youtrack.jetbrains.com/issue/KT-78452) Drop redundant frontend structures after fir2ir conversion
- [`KT-78458`](https://youtrack.jetbrains.com/issue/KT-78458) Don't populate PredicateBasedProvider if no lookup predicates are registered
- [`KT-78440`](https://youtrack.jetbrains.com/issue/KT-78440) Lambda with an implicitly runtime-retained annotation is generated via invokedynamic with `-Xindy-allow-annotated-lambdas=false`
- [`KT-77709`](https://youtrack.jetbrains.com/issue/KT-77709) Missing diagnostics of accessing less visible objects in inline function
- [`KT-77577`](https://youtrack.jetbrains.com/issue/KT-77577) False positive exposed type warnings
- [`KT-77095`](https://youtrack.jetbrains.com/issue/KT-77095) FIR: Report warnings on exposure of references to invisible references in inline functions
- [`KT-76981`](https://youtrack.jetbrains.com/issue/KT-76981) Move exposed type checker to regular checkers
- [`KT-78252`](https://youtrack.jetbrains.com/issue/KT-78252) ClassCastException when `Array<Void>` used for compile-time vararg of `Nothing`
- [`KT-77713`](https://youtrack.jetbrains.com/issue/KT-77713) Context Parameters cause compiler generate r8 incompatible bytecode
- [`KT-71854`](https://youtrack.jetbrains.com/issue/KT-71854) K2 IDE. False positive red code because of external annotation on a generic parameter
- [`KT-67335`](https://youtrack.jetbrains.com/issue/KT-67335) K2: Infers Int instead of Long for an ILT
- [`KT-76629`](https://youtrack.jetbrains.com/issue/KT-76629) K2 Mode: False positive RedundantVisibilityModifier inspection on private constructors in sealed classes
- [`KT-77728`](https://youtrack.jetbrains.com/issue/KT-77728) Drop controversial experimental checkers
- [`KT-78429`](https://youtrack.jetbrains.com/issue/KT-78429) K2: Property callable reference incorrectly smart-casted to intersection of property type and KProperty
- [`KT-78509`](https://youtrack.jetbrains.com/issue/KT-78509) Renamed for override copy functions are cached in scope instead of session
- [`KT-17417`](https://youtrack.jetbrains.com/issue/KT-17417) Loops in delegation: no compilation error on non-abstract class with abstract method that never implemented
- [`KT-75033`](https://youtrack.jetbrains.com/issue/KT-75033) Split JvmBackendPipelinePhase to be able to provide a custom implementation of writeOutputs
- [`KT-75831`](https://youtrack.jetbrains.com/issue/KT-75831) K2: An extra "[VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE] An explicit type is required on a value parameter." for a missing parameter
- [`KT-78370`](https://youtrack.jetbrains.com/issue/KT-78370) All the [something]Assign operators on dynamic return Unit as a type
- [`KT-73950`](https://youtrack.jetbrains.com/issue/KT-73950) K2 IDE / Kotlin Debugger: ISE “Fake override should have at least one overridden descriptor” on evaluation of local calss in presence of bystander
- [`KT-78280`](https://youtrack.jetbrains.com/issue/KT-78280) Implement the sourceless `KtDiagnostic`s
- [`KT-76543`](https://youtrack.jetbrains.com/issue/KT-76543) Migrate psi2ir sources to new IR parameter API
- [`KT-77716`](https://youtrack.jetbrains.com/issue/KT-77716) Kotlin/Native and -Xseparate-kmp-compilation: "Compilation failed: Several functions kotlin/native/immutableBlobOf found"
- [`KT-76400`](https://youtrack.jetbrains.com/issue/KT-76400) Context-sensitive resolution doesn’t work in if-else condition passed as a function argument
- [`KT-76606`](https://youtrack.jetbrains.com/issue/KT-76606) Enable 'Indy: Allow lambdas with annotations' by default
- [`KT-76739`](https://youtrack.jetbrains.com/issue/KT-76739) Dubious argument type mismatch "actual type is 'String', but 'String' was expected" caused by wrong number of type arguments
- [`KT-78121`](https://youtrack.jetbrains.com/issue/KT-78121) Report warning on function type with multiple implicit values that's annotated with DSL marker
- [`KT-76872`](https://youtrack.jetbrains.com/issue/KT-76872) Anonymous context parameters are not visible in debugger
- [`KT-74088`](https://youtrack.jetbrains.com/issue/KT-74088) Kotlin Debugger: CCE on evaluating private suspend function
- [`KT-77301`](https://youtrack.jetbrains.com/issue/KT-77301) False positive Context Parameter resolution when using DslMarker
- [`KT-78230`](https://youtrack.jetbrains.com/issue/KT-78230) Add more test cases to the holdsIn contracts
- [`KT-78111`](https://youtrack.jetbrains.com/issue/KT-78111) K2: Approximation of captured star projection in function type produces `Function1<Nothing?, Unit>` in IR
- [`KT-77273`](https://youtrack.jetbrains.com/issue/KT-77273) K/N: Remove the kotlin.native.internal.Ref class from the standard library
- [`KT-73995`](https://youtrack.jetbrains.com/issue/KT-73995) JVM bytecode: Bad name for value class field
- [`KT-73013`](https://youtrack.jetbrains.com/issue/KT-73013) Kotlin Debugger: ISE “No mapping for symbol: VALUE_PARAMETER” on evaluating callable reference to local function with closure in it
- [`KT-77665`](https://youtrack.jetbrains.com/issue/KT-77665) K2: unresolved annotatation on local context parameter type
- [`KT-77485`](https://youtrack.jetbrains.com/issue/KT-77485) Add constraints logging to inference
- [`KT-76504`](https://youtrack.jetbrains.com/issue/KT-76504) Find and deprecate actively used parts of K1 API
- [`KT-75338`](https://youtrack.jetbrains.com/issue/KT-75338) K2 Mode: False positive "Redundant assignment" diagnostic on variable captured by local function
- [`KT-77648`](https://youtrack.jetbrains.com/issue/KT-77648) K2: False negative DSL_SCOPE_VIOLATION when using named argument for lambda with annotated function type
- [`KT-77355`](https://youtrack.jetbrains.com/issue/KT-77355) Report warning on overloading by a superset of another overload's context parameters
- [`KT-77354`](https://youtrack.jetbrains.com/issue/KT-77354) Report warning on overloading by a subtype of another overload's context parameter
- [`KT-78084`](https://youtrack.jetbrains.com/issue/KT-78084) Unify deprecation warning messages
- [`KT-76776`](https://youtrack.jetbrains.com/issue/KT-76776) `@MustUseReturnValue` doesn't affect nested scopes
- [`KT-77545`](https://youtrack.jetbrains.com/issue/KT-77545) `@NoInfer` on receiver type leads to false positive type mismatch when generic type is specified explicitly and closest implicit receiver is of incorrect type
- [`KT-76772`](https://youtrack.jetbrains.com/issue/KT-76772) `@NoInfer` on a context parameter's type leads to a false-positive context argument ambiguity error regardless of the closest implicit values' types if there are multiple of them at the call site
- [`KT-76771`](https://youtrack.jetbrains.com/issue/KT-76771) `@NoInfer` on context parameter type leads to a false-positive type mismatch when generic type is specified explicitly and closest implicit value at the call site is of a mismatching type
- [`KT-77156`](https://youtrack.jetbrains.com/issue/KT-77156) INITIALIZATION_BEFORE_DECLARATION is not reported in anonymous object
- [`KT-78060`](https://youtrack.jetbrains.com/issue/KT-78060) UNRESOLVED_REFERENCE in fp-space
- [`KT-67555`](https://youtrack.jetbrains.com/issue/KT-67555) Debug metadata: map the Continuation label to the next executable location in file
- [`KT-77723`](https://youtrack.jetbrains.com/issue/KT-77723) Refine the message for ArrayEqualityCanBeReplacedWithEquals checker
- [`KT-75178`](https://youtrack.jetbrains.com/issue/KT-75178) Inline functions in conjunction with `@JvmStatic` may result in bytecode errors
- [`KT-77390`](https://youtrack.jetbrains.com/issue/KT-77390) Prototype lazy loading of stdlib symbols in Native
- [`KT-77921`](https://youtrack.jetbrains.com/issue/KT-77921) False positive EXTENSION_SHADOWED_BY_MEMBER when member has context parameters
- [`KT-77895`](https://youtrack.jetbrains.com/issue/KT-77895) false-negative error on package directives with context parameter lists (even with context parameters disabled)
- [`KT-76767`](https://youtrack.jetbrains.com/issue/KT-76767) AMBIGUOUS_CONTEXT_ARGUMENT should report the name of the context parameter in addition to the type
- [`KT-77444`](https://youtrack.jetbrains.com/issue/KT-77444) K2: False negative "Unchecked cast" with casting from MutableList<out T> to MutableList<T>
- [`KT-63348`](https://youtrack.jetbrains.com/issue/KT-63348) K2: FIR2IR should properly pass expected types
- [`KT-77627`](https://youtrack.jetbrains.com/issue/KT-77627) K2: consider getting rid of NEW_INFERENCE_ERROR
- [`KT-75833`](https://youtrack.jetbrains.com/issue/KT-75833) K2: Extra [ANNOTATION_ARGUMENT_MUST_BE_CONST] when passing regex-like strings as annotation arguments
- [`KT-77547`](https://youtrack.jetbrains.com/issue/KT-77547) Native: add a check that the logic looking for stdlib-related bitcode is not used when compiling sources to a klib
- [`KT-77206`](https://youtrack.jetbrains.com/issue/KT-77206) Remove `PARAMETER_NAME_CHANGED_ON_OVERRIDE` suppression in KMP lexers
- [`KT-77679`](https://youtrack.jetbrains.com/issue/KT-77679) Update syntax-api dependency in KMP Kotlin parser
- [`KT-77705`](https://youtrack.jetbrains.com/issue/KT-77705) K2: Consuming data class compiled with kotlin 1.0.5 breaks the K2 compiler
- [`KT-76583`](https://youtrack.jetbrains.com/issue/KT-76583) CCE: suspend lambda attempts to unbox value class parameter twice after lambda suspended
- [`KT-76663`](https://youtrack.jetbrains.com/issue/KT-76663) KJS: KotlinNothingValueException caused by expression return since 2.1.20
- [`KT-75457`](https://youtrack.jetbrains.com/issue/KT-75457) Native: cache machinery uses stdlib cache with default runtime options even if custom runtime options are supplied when partial linkage is disabled
- [`KT-77563`](https://youtrack.jetbrains.com/issue/KT-77563) False-positive smart cast with captured local in init block causes NPE
- [`KT-77696`](https://youtrack.jetbrains.com/issue/KT-77696) ISE "couldn't find inline method" on kotlin/Result compiled by old Kotlin version
- [`KT-76931`](https://youtrack.jetbrains.com/issue/KT-76931) K2: Annotation on do-while expression captures variables from inside the loop
- [`KT-77183`](https://youtrack.jetbrains.com/issue/KT-77183) Metadata: remove multi-field value class representation
- [`KT-77678`](https://youtrack.jetbrains.com/issue/KT-77678) Apply found optimization to Kotlin KMP parser
- [`KT-60127`](https://youtrack.jetbrains.com/issue/KT-60127) K2: Support scripts with LightTree-based raw FIR building
- [`KT-76615`](https://youtrack.jetbrains.com/issue/KT-76615) K2: "IllegalArgumentException: Inline class types should have the same representation: Lkotlin/UByte; != B" for mixed Java/Kotlin code
- [`KT-77220`](https://youtrack.jetbrains.com/issue/KT-77220) Annotation with EXPRESSION is not allowed on lambdas in Kotlin 2.2.0
- [`KT-77656`](https://youtrack.jetbrains.com/issue/KT-77656) K/N: fix the super type for local delegated properties
- [`KT-75907`](https://youtrack.jetbrains.com/issue/KT-75907) Inference/PCLA: consider storing semi-fixed variables in inference session
- [`KT-77144`](https://youtrack.jetbrains.com/issue/KT-77144) Implement KMP Kotlin parser
- [`KT-77352`](https://youtrack.jetbrains.com/issue/KT-77352) Implement KMP Expression parser
- [`KT-76984`](https://youtrack.jetbrains.com/issue/KT-76984) SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS isn't reported for primitive wrapper classes instantiated within the scope
- [`KT-67471`](https://youtrack.jetbrains.com/issue/KT-67471) K2: "Unresolved reference" on incorrect term of FQ name
- [`KT-77269`](https://youtrack.jetbrains.com/issue/KT-77269) [K/N] external calls checker crashes when used with caches
- [`KT-77205`](https://youtrack.jetbrains.com/issue/KT-77205) Kotlin Debugger / Context Parameters: CCE “class FirPropertySymbol cannot be cast to class FirFunctionSymbol” on evaluating class property
- [`KT-74133`](https://youtrack.jetbrains.com/issue/KT-74133) FIR: use EmptyDeprecationsPerUseSite consistently in symbols
- [`KT-77100`](https://youtrack.jetbrains.com/issue/KT-77100) java.lang.Void type is not ignorable
- [`KT-77491`](https://youtrack.jetbrains.com/issue/KT-77491) K2: No SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE when using typealias
- [`KT-77490`](https://youtrack.jetbrains.com/issue/KT-77490) Report error on contextual function type in supertype
- [`KT-77431`](https://youtrack.jetbrains.com/issue/KT-77431) Functional type with a context is allowed as an upper-bound
- [`KT-77432`](https://youtrack.jetbrains.com/issue/KT-77432) Context isn't passed properly when functional type with a context is used as a type argument
- [`KT-77417`](https://youtrack.jetbrains.com/issue/KT-77417) There is no TYPE_VARIANCE_CONFLICT_ERROR when 'out' type is used in context
- [`KT-62631`](https://youtrack.jetbrains.com/issue/KT-62631) Improve expect-actual "checking" incompatibilities reporting
- [`KT-77481`](https://youtrack.jetbrains.com/issue/KT-77481) Support ExpectRefinement feature in HMPP compilation scheme
- [`KT-77268`](https://youtrack.jetbrains.com/issue/KT-77268) Make sure that -Xreturn-value-checker also enables -XX:UnnamedLocalVariables
- [`KT-65719`](https://youtrack.jetbrains.com/issue/KT-65719) K1/K2: Nullness defaults from subclass unsoundly applied to method in superclass
- [`KT-53836`](https://youtrack.jetbrains.com/issue/KT-53836) In type-parameter declarations, recognize JSpecify annotations only on *bounds*
- [`KT-73658`](https://youtrack.jetbrains.com/issue/KT-73658) JSpecify `@NonNull` annotation on type-parameter bound prevents type-variable usages from being platform types
- [`KT-77000`](https://youtrack.jetbrains.com/issue/KT-77000) Leave ForbidInferOfInvisibleTypeAsReifiedOrVararg as a warning
- [`KT-74084`](https://youtrack.jetbrains.com/issue/KT-74084) K2: False negative [NO_ELSE_IN_WHEN]
- [`KT-77451`](https://youtrack.jetbrains.com/issue/KT-77451) FirLazyResolveContractViolationException for test with overridden delegate
- [`KT-77397`](https://youtrack.jetbrains.com/issue/KT-77397) Report UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL when calling declaration with contextual function type in signature
- [`KT-77137`](https://youtrack.jetbrains.com/issue/KT-77137) K2: Controversial behavior allows resolving annotation arguments on a companion inside it
- [`KT-77257`](https://youtrack.jetbrains.com/issue/KT-77257) Report compilation error when in generated JVM bytecode there is a need for CHECKCAST of the conditional expression to the inaccessible interface
- [`KT-77256`](https://youtrack.jetbrains.com/issue/KT-77256) Report compilation error when in generated JVM bytecode there is a need for CHECKCAST of the functional call result to the inaccessible interface
- [`KT-76356`](https://youtrack.jetbrains.com/issue/KT-76356) K2 evaluation fails on evaluating inline methods if there is an inline with AutoCloseable
- [`KT-73786`](https://youtrack.jetbrains.com/issue/KT-73786) Evaluator: cannot evaluate inline methods with reified parameter
- [`KT-77204`](https://youtrack.jetbrains.com/issue/KT-77204) Native: XCode strip command causes flaky tests
- [`KT-77351`](https://youtrack.jetbrains.com/issue/KT-77351) Implement KMP KDoc parser
- [`KT-76914`](https://youtrack.jetbrains.com/issue/KT-76914) compile-time failure on a type argument placeholder in a callable reference
- [`KT-76597`](https://youtrack.jetbrains.com/issue/KT-76597) False negative opt-in required on delegated constructor call
- [`KT-76667`](https://youtrack.jetbrains.com/issue/KT-76667) Mark the class implementation of interface function with ACC_BRIDGE in the class file
- [`KT-77181`](https://youtrack.jetbrains.com/issue/KT-77181) K2: a nested typealias annotation observes member declarations of the outer class
- [`KT-77180`](https://youtrack.jetbrains.com/issue/KT-77180) K2: Wrong scope for annotation arguments in the constructor header
- [`KT-77287`](https://youtrack.jetbrains.com/issue/KT-77287) Try enforcing `source != null` when `origin == Source`
- [`KT-76135`](https://youtrack.jetbrains.com/issue/KT-76135) K2: drop pre-1.8 language features from compiler code
- [`KT-77231`](https://youtrack.jetbrains.com/issue/KT-77231) Reflection: CCE on resuming coroutine after callSuspend if result is a generic inline class substituted with primitive
- [`KT-77031`](https://youtrack.jetbrains.com/issue/KT-77031) Investigate the actual need of deduplicating provider in HMPP compilation scheme
- [`KT-77050`](https://youtrack.jetbrains.com/issue/KT-77050) Implement KMP KDoc lexer
- [`KT-77048`](https://youtrack.jetbrains.com/issue/KT-77048) Implement KMP Kotlin lexer
- [`KT-77044`](https://youtrack.jetbrains.com/issue/KT-77044) Consolidate, refine and update jFlex dependency
- [`KT-77252`](https://youtrack.jetbrains.com/issue/KT-77252) It is impossible to declare an unnamed variable in a script
- [`KT-58137`](https://youtrack.jetbrains.com/issue/KT-58137) K2: ISE "Usage of default value argument for this annotation is not yet possible" when instantiating Kotlin annotation with default parameter from another module
- [`KT-77140`](https://youtrack.jetbrains.com/issue/KT-77140) Protect ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA with opt-in
- [`KT-76898`](https://youtrack.jetbrains.com/issue/KT-76898) K2: ClassCastException when data class shadows supertype's `componentX` method with wrong type
- [`KT-75695`](https://youtrack.jetbrains.com/issue/KT-75695) Bogus "Assigned value is never read" warning for prefix ++ operator
- [`KT-76805`](https://youtrack.jetbrains.com/issue/KT-76805) Wrong NPE occurs when assigning synthetic properties with platform types in Kotlin 2.1.20
- [`KT-77078`](https://youtrack.jetbrains.com/issue/KT-77078) K2: anonymous object is wrongly allowed to implement interfaces by unsafe Delegation
- [`KT-72722`](https://youtrack.jetbrains.com/issue/KT-72722) Treat 'copy' calls of a data class as explicit constructor usages
- [`KT-77149`](https://youtrack.jetbrains.com/issue/KT-77149) IllegalArgumentException: source must not be null
- [`KT-76806`](https://youtrack.jetbrains.com/issue/KT-76806) K2: AIOOBE in FirEqualityCompatibilityChecker
- [`KT-72391`](https://youtrack.jetbrains.com/issue/KT-72391) KJS: (a * b).toDouble_ygsx0s_k$ is not a function
- [`KT-76950`](https://youtrack.jetbrains.com/issue/KT-76950) K2: "IllegalArgumentException: Inline class types should have the same representation: Lkotlin/UByte; != B" with nullable UByte
- [`KT-76043`](https://youtrack.jetbrains.com/issue/KT-76043) Native: NotImplementedError: Generation of stubs for class org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl is not supported yet
- [`KT-77126`](https://youtrack.jetbrains.com/issue/KT-77126) Transitive dependency mismatch between Kotlin Gradle Plugin and Scripting dependencies
- [`KT-72831`](https://youtrack.jetbrains.com/issue/KT-72831) ANNOTATION_USED_AS_ANNOTATION_ARGUMENT missing in some cases in K2
- [`KT-73707`](https://youtrack.jetbrains.com/issue/KT-73707) Remove dependency on ":compiler:backend.jvm" from Native
- [`KT-75499`](https://youtrack.jetbrains.com/issue/KT-75499) CheckerContext#{containingDeclaration, containingFile} in checkers should return symbols
- [`KT-76548`](https://youtrack.jetbrains.com/issue/KT-76548) False positive TYPE_MISMATCH when resolving an expression with the expected type from the upper bound
- [`KT-76142`](https://youtrack.jetbrains.com/issue/KT-76142) K2: `@RequiresOptIn` warning does not display the custom message when using concatenated strings.
- [`KT-68699`](https://youtrack.jetbrains.com/issue/KT-68699) Kotlin Debugger: UPAE “lateinit property parent has not been initialized” on trying evaluate enumValues<T>(), enumEntries<T>() from inlined function with reified parameter
- [`KT-63267`](https://youtrack.jetbrains.com/issue/KT-63267) K2: incorrect line numbers after smart cast of an extension receiver
- [`KT-71309`](https://youtrack.jetbrains.com/issue/KT-71309) Kotlin Debugger: UnsupportedOperationException on calling method with reified type parameter
- [`KT-74912`](https://youtrack.jetbrains.com/issue/KT-74912) K2: Investigate irrelevant ARGUMENT_TYPE_MISMATCH on top-level lambdas
- [`KT-74657`](https://youtrack.jetbrains.com/issue/KT-74657) K2: Linenumber for annotation on local variable is present in LVT
- [`KT-76749`](https://youtrack.jetbrains.com/issue/KT-76749) NONE_APPLICABLE message is unreadable for stdlib context  function
- [`KT-74932`](https://youtrack.jetbrains.com/issue/KT-74932) Investigate false-negative ARGUMENT_TYPE_MISMATCH on a nested anonymous function
- [`KT-74545`](https://youtrack.jetbrains.com/issue/KT-74545) Redundant TYPE_MISMATCH in variable initializer with call
- [`KT-76774`](https://youtrack.jetbrains.com/issue/KT-76774) K2: Simplify ResolutionMode.WithExpectedType contracts
- [`KT-76689`](https://youtrack.jetbrains.com/issue/KT-76689) Unnamed local variable with type and without initializer is allowed
- [`KT-76746`](https://youtrack.jetbrains.com/issue/KT-76746) ClassCastException: class org.jetbrains.kotlin.fir.types.impl.FirUserTypeRefImpl cannot be cast to class
- [`KT-76754`](https://youtrack.jetbrains.com/issue/KT-76754) K2: Compiler doesn't check annotations on array literals (as annotation arguments)
- [`KT-76674`](https://youtrack.jetbrains.com/issue/KT-76674) The function isn't called from unnamed local variable initializer
- [`KT-75553`](https://youtrack.jetbrains.com/issue/KT-75553) `MISSING_DEPENDENCY_SUPERCLASS` and `MISSING_DEPENDENCY_SUPERCLASS_WARNING` is reported at the same time on the same element
- [`KT-76345`](https://youtrack.jetbrains.com/issue/KT-76345) Enhance variable fixation
- [`KT-73348`](https://youtrack.jetbrains.com/issue/KT-73348) AssertionError from isCompiledToJvmDefault on super call of suspend function with composable function parameter
- [`KT-72305`](https://youtrack.jetbrains.com/issue/KT-72305) K2: Report error when using synthetic properties in case of mapped collections
- [`KT-73527`](https://youtrack.jetbrains.com/issue/KT-73527) Prohibit (via a deprecation warning) accessing nested class through generic outer class
- [`KT-59886`](https://youtrack.jetbrains.com/issue/KT-59886) K2: Disappeared ERROR_IN_CONTRACT_DESCRIPTION
- [`KT-61227`](https://youtrack.jetbrains.com/issue/KT-61227) Definitely non-nullable types cause "Any was expected" for `@Nullable` parameter
- [`KT-57911`](https://youtrack.jetbrains.com/issue/KT-57911) K2: Contracts are not inherited by substitution overrides
- [`KT-47398`](https://youtrack.jetbrains.com/issue/KT-47398) 'null' EnhancedNullability value in String-based 'when' might produce different behavior depending on whether 'when' is "optimized" or not

### Compose compiler

- [`CMP-7505`](https://youtrack.jetbrains.com/issue/CMP-7505) IrLinkageError: Function can not be called: No function found for symbol
- [`b/432262806`](https://issuetracker.google.com/issues/432262806) Fix target description lookup
- [`b/436870733`](https://issuetracker.google.com/issues/436870733) Prevent lambda memoization in local classes inside a composable
- [`b/432485982`](https://issuetracker.google.com/issues/432485982) Fix AbstractMethodError when overriding function with default parameters
- [`b/432262806`](https://issuetracker.google.com/issues/432262806) Use classId as FirApplierInferencer tokens
- [`b/400371006`](https://issuetracker.google.com/issues/400371006) Gate default parameters behind language versions
- [`b/245673006`](https://issuetracker.google.com/issues/245673006) Specify fqName for classes and functions in build metrics
- [`b/254577243`](https://issuetracker.google.com/issues/254577243) Avoid printing complex expressions in compiler metrics
- [`b/394891628`](https://issuetracker.google.com/issues/394891628) Allow specifying target version of Compose runtime
- [`b/424454512`](https://issuetracker.google.com/issues/424454512) Recreate FirApplierInferencer for each check
- [`b/417406922`](https://issuetracker.google.com/issues/417406922) Restrict references to `@Composable` properties
- [`b/282135108`](https://issuetracker.google.com/issues/282135108), [`b/349866442`](https://issuetracker.google.com/issues/349866442) [Compose] Enable applier checking when using FIR
- [`b/307592552`](https://issuetracker.google.com/issues/307592552) Add BigInteger and BigDecimal to the list of known stable classes
- [`b/414547195`](https://issuetracker.google.com/issues/414547195) Unwrap type casts when inferring `@Composable` call arguments

### IR. Inlining

#### New Features

- [`KT-70360`](https://youtrack.jetbrains.com/issue/KT-70360) KLIBs: Uniformly handle`typeOf()` calls at 1st/2nd stages of compilation

#### Fixes

- [`KT-79002`](https://youtrack.jetbrains.com/issue/KT-79002) [Inliner][Native][PL] Native backend fails for inline function that instantiates a class that was compiled implementing two interfaces, which turned into abstract classes
- [`KT-78137`](https://youtrack.jetbrains.com/issue/KT-78137) Review & enable PL tests with enabled IR inliner
- [`KT-72464`](https://youtrack.jetbrains.com/issue/KT-72464) [Native][JS][Wasm] Non-local return through suspend conversion breaks the IR inliner
- [`KT-69941`](https://youtrack.jetbrains.com/issue/KT-69941) Rewrite `DumpSyntheticAccessors` lowering to test handler after moving common Native/JS prefix to KLIB compilation
- [`KT-78245`](https://youtrack.jetbrains.com/issue/KT-78245) Synthetic Accessors incorrectly copies default values
- [`KT-76236`](https://youtrack.jetbrains.com/issue/KT-76236) Include `NativeInliningFacade` and `JsIrInliningFacade` in all Native & JS test runners
- [`KT-76512`](https://youtrack.jetbrains.com/issue/KT-76512) Avoid using `originalFunction` inside `FunctionInlining`
- [`KT-69457`](https://youtrack.jetbrains.com/issue/KT-69457) [references] IR Inliner: References to inline functions are not inlined
- [`KT-47521`](https://youtrack.jetbrains.com/issue/KT-47521) Native & JS: Recursive inline fun calls -> StackOverflowError
- [`KT-76425`](https://youtrack.jetbrains.com/issue/KT-76425) Do not store signatures of preprocessed inline functions in KLIBs
- [`KT-76763`](https://youtrack.jetbrains.com/issue/KT-76763) [Inliner] Don't use attributeOwnerId to pass info from Inliner to non-JVM backends
- [`KT-77102`](https://youtrack.jetbrains.com/issue/KT-77102) [Inliner] Expression uses unlinked type parameter symbol
- [`KT-76145`](https://youtrack.jetbrains.com/issue/KT-76145) Enhance error message about poisoned KLIBs in KLIB-based compilers
- [`KT-77079`](https://youtrack.jetbrains.com/issue/KT-77079) IR: Report warnings on exposure of references to invisible declarations in inline functions
- [`KT-69797`](https://youtrack.jetbrains.com/issue/KT-69797) [references] Accessors for private function/constructor/property references are not generated
- [`KT-76454`](https://youtrack.jetbrains.com/issue/KT-76454) Investigate erasure of class type parameters during inliner
- [`KT-72593`](https://youtrack.jetbrains.com/issue/KT-72593) [K/N] Add NativeIrInliningFacade to CrossCompilationIdentityTest
- [`KT-70969`](https://youtrack.jetbrains.com/issue/KT-70969) IR Inliner: Ensure that common prefix at 1st phase does not affect KLIB signatures
- [`KT-75937`](https://youtrack.jetbrains.com/issue/KT-75937) [IR Inliner] Umbrella for failing tests due to public inliner
- [`KT-77295`](https://youtrack.jetbrains.com/issue/KT-77295) Improve Diagnostic Message Formatting for Private API Exposure in Inline Functions
- [`KT-77047`](https://youtrack.jetbrains.com/issue/KT-77047) Ir Ininler: crash on fake override in private class from more visible class
- [`KT-77336`](https://youtrack.jetbrains.com/issue/KT-77336) [references] Synthetic accessor test for private top-level function accessed via reference fails with `No function found for symbol`
- [`KT-76761`](https://youtrack.jetbrains.com/issue/KT-76761) [Inliner] non-JVM IR Inliner incorrectly uses K/JVM-specific code
- [`KT-76712`](https://youtrack.jetbrains.com/issue/KT-76712) [Inliner] No function found for symbol '/<unknown name>|?'
- [`KT-76711`](https://youtrack.jetbrains.com/issue/KT-76711) [Inliner] Reference to function 'privateMethod' can not be evaluated

### IR. Tree

- [`KT-77508`](https://youtrack.jetbrains.com/issue/KT-77508) K/JS and K/Native CompilationException Wrong number of parameters in wrapper
- [`KT-78978`](https://youtrack.jetbrains.com/issue/KT-78978) PL tests: Drop `adjust*forLazyIr()` hack
- [`KT-76813`](https://youtrack.jetbrains.com/issue/KT-76813) IR validator: not all symbols/references are visited
- [`KT-77596`](https://youtrack.jetbrains.com/issue/KT-77596) Refine `reuseExistingSignaturesForSymbols` setting in IR serializer
- [`KT-76723`](https://youtrack.jetbrains.com/issue/KT-76723) IR validator: Check visibilities of annotations
- [`KT-76405`](https://youtrack.jetbrains.com/issue/KT-76405) Visit annotations in IrTypeVisitor and IrTreeSymbolsVisitor
- [`KT-78033`](https://youtrack.jetbrains.com/issue/KT-78033) [PL] Merge `IrUnimplementedOverridesStrategy` to `PartiallyLinkedIrTreePatcher`

### JVM. Reflection

- [`KT-77882`](https://youtrack.jetbrains.com/issue/KT-77882) kotlin-reflect: KParameter.name returns "<unused var>" instead of null for anonymous context parameters
- [`KT-77879`](https://youtrack.jetbrains.com/issue/KT-77879) kotlin-reflect: toString overrides of KCallable implementations do not render context parameters
- [`KT-74529`](https://youtrack.jetbrains.com/issue/KT-74529) Context parameters support in reflection
- [`KT-52170`](https://youtrack.jetbrains.com/issue/KT-52170) Reflection: typeOf<Array<Long>> gives classifier LongArray
- [`KT-77663`](https://youtrack.jetbrains.com/issue/KT-77663) Reflection: java.util.ServiceConfigurationError: "module kotlin.reflect does not declare `uses`" when using kotlin-reflect in modular mode

### JavaScript

#### New Features

- [`KT-79222`](https://youtrack.jetbrains.com/issue/KT-79222) K/JS: Allow using Long in exported declarations
- [`KT-79394`](https://youtrack.jetbrains.com/issue/KT-79394) Add the possibility to write common external declarations between JS and WasmJS targets
- [`KT-70486`](https://youtrack.jetbrains.com/issue/KT-70486) K/JS: exported exception types should extend Error
- [`KT-19016`](https://youtrack.jetbrains.com/issue/KT-19016) Define accessors as enumerable

#### Performance Improvements

- [`KT-57128`](https://youtrack.jetbrains.com/issue/KT-57128) KJS: Use BigInt to represent Long values in ES6 mode
- [`KT-54689`](https://youtrack.jetbrains.com/issue/KT-54689) KJS: Data class equals less efficient than manually written version

#### Fixes

- [`KT-69297`](https://youtrack.jetbrains.com/issue/KT-69297) Deprecate referencing inlineable lambdas in `js()` calls
- [`KT-77620`](https://youtrack.jetbrains.com/issue/KT-77620) Fix failing IC tests on Windows
- [`KT-77372`](https://youtrack.jetbrains.com/issue/KT-77372) KJS: NullPointerException at JsIntrinsics$JsReflectionSymbols
- [`KT-78316`](https://youtrack.jetbrains.com/issue/KT-78316) KJS: List is not exported to TypeScript declaration if wrapped in Promise
- [`KT-79644`](https://youtrack.jetbrains.com/issue/KT-79644) BigInt enabled for ES 2015 despite being an ES 2020 feature
- [`KT-79089`](https://youtrack.jetbrains.com/issue/KT-79089) KJS: Could not load reporter / Cannot find module 'mocha' when running jsNode tests
- [`KT-79916`](https://youtrack.jetbrains.com/issue/KT-79916) K/JS: "Uncaught TypeError" when using 'Xes-long-as-bigint' in compose-html
- [`KT-79050`](https://youtrack.jetbrains.com/issue/KT-79050) KJS / IC: "Unexpected body of primary constructor for processing irClass"
- [`KT-79977`](https://youtrack.jetbrains.com/issue/KT-79977) KJS: Long.rotateLeft returns incorrect result when BigInts are enabled
- [`KT-76093`](https://youtrack.jetbrains.com/issue/KT-76093) Support new callable reference nodes in partial linkage in Kotlin/JS
- [`KT-78073`](https://youtrack.jetbrains.com/issue/KT-78073) K/JS: KProperty from local delegate changes after another delegate is invoked
- [`KT-52230`](https://youtrack.jetbrains.com/issue/KT-52230) KSJ IR: Applying identity equality operator to Longs always returns false
- [`KT-6675`](https://youtrack.jetbrains.com/issue/KT-6675) KotlinJS: toInt() on external Long throws error
- [`KT-79184`](https://youtrack.jetbrains.com/issue/KT-79184) K/JS: Further intrinsify BigInt-backed Long operations
- [`KT-78701`](https://youtrack.jetbrains.com/issue/KT-78701) Js and Wasm: enumValueOf does not include invalid value into an exception message
- [`KT-55256`](https://youtrack.jetbrains.com/issue/KT-55256) KJS: non-exported subclass with a no-parameter function overload doesn't compile
- [`KT-76034`](https://youtrack.jetbrains.com/issue/KT-76034) passProcessArgvToMainFunction contains the node path and script path
- [`KT-66091`](https://youtrack.jetbrains.com/issue/KT-66091) KJS, WASM: `AssertionError: Illegal value: <T>` in test nonReified_equality.kt
- [`KT-78169`](https://youtrack.jetbrains.com/issue/KT-78169) KJS: [NON_EXPORTABLE_TYPE]  with `@JsExport` class if `@JsStatic` companion method returns an out type
- [`KT-57192`](https://youtrack.jetbrains.com/issue/KT-57192) KJS: "Exported declaration uses non-exportable return type" caused by `@JsExport` Promise with Unit type
- [`KT-61183`](https://youtrack.jetbrains.com/issue/KT-61183) KJS: "AssertionError: Assertion failed" from JsSuspendFunctionsLowering
- [`KT-59326`](https://youtrack.jetbrains.com/issue/KT-59326) KJS / IR: invalid code generated when using constructor parameter named `default`
- [`KT-70295`](https://youtrack.jetbrains.com/issue/KT-70295) KLIB stdlib: Unify intrinsics for boxing captured variables in lambdas across non-JVM backends
- [`KT-77021`](https://youtrack.jetbrains.com/issue/KT-77021) CompilationException: Encountered a local class not previously collected on inner classes inside anonymous objects
- [`KT-77320`](https://youtrack.jetbrains.com/issue/KT-77320) KJS:  Big.js times() is compiled to multiply (*) operator
- [`KT-77430`](https://youtrack.jetbrains.com/issue/KT-77430) K/JS: Remove sharedBox* intrinsics from the standard library
- [`KT-73267`](https://youtrack.jetbrains.com/issue/KT-73267) KJS: IC: "FileNotFoundException": Build failures with Kotlin 2.1-RC and RC2
- [`KT-76912`](https://youtrack.jetbrains.com/issue/KT-76912) KJS: `@JsStatic` can't be used for companion objects implementing external interfaces
- [`KT-77271`](https://youtrack.jetbrains.com/issue/KT-77271) KJS / Serialization: "Cannot set property message of Error which has only a getter"
- [`KT-77242`](https://youtrack.jetbrains.com/issue/KT-77242) Kotlin/JS & Kotlin/Wasm backends: Artificially apply reverse topo-order after IR linkage
- [`KT-77649`](https://youtrack.jetbrains.com/issue/KT-77649) KJS: es-arrow-functions requires explicit opt-in when target is ES2015
- [`KT-76235`](https://youtrack.jetbrains.com/issue/KT-76235) [JS] Extra invalid line `tmp_0.tmp00__1 = Options;` in testSuspendFunction()
- [`KT-76234`](https://youtrack.jetbrains.com/issue/KT-76234) [JS] Extra invalid line `Parent` in testNested()
- [`KT-76233`](https://youtrack.jetbrains.com/issue/KT-76233) [JS] Extra invalid import line in testJsQualifier()
- [`KT-77190`](https://youtrack.jetbrains.com/issue/KT-77190) Migrate JS diagnostic tests to the new CLI-based test facades (1st phase only)
- [`KT-77418`](https://youtrack.jetbrains.com/issue/KT-77418) KJS: cannot debug with whole-program granularity
- [`KT-77371`](https://youtrack.jetbrains.com/issue/KT-77371) [K/N][K/JS][K/Wasm] Unify visibility rules for generated default argument stubs
- [`KT-77148`](https://youtrack.jetbrains.com/issue/KT-77148) KJS: "Uncaught TypeError: (intermediate value).l(...).m is not a function" during production build run
- [`KT-77193`](https://youtrack.jetbrains.com/issue/KT-77193) Migrate JS irText tests to the new CLI-based test facades (1st phase only)
- [`KT-77192`](https://youtrack.jetbrains.com/issue/KT-77192) Migrate JS ABI reader tests to the new CLI-based test facades (1st phase only)
- [`KT-77187`](https://youtrack.jetbrains.com/issue/KT-77187) Migrate JS box tests to the new CLI-based test facades (1st phase only)
- [`KT-77027`](https://youtrack.jetbrains.com/issue/KT-77027) Migrate 1st phase facades to the phased CLI infrastructure in JS tests
- [`KT-69591`](https://youtrack.jetbrains.com/issue/KT-69591) KJS / d.ts: Wrong type of SerializerFactory for abstract classes
- [`KT-76027`](https://youtrack.jetbrains.com/issue/KT-76027) KJS: "ReferenceError: entries is not defined": Accessing entries of an enum arbitrarily fails with println()
- [`KT-76232`](https://youtrack.jetbrains.com/issue/KT-76232) Suspend contextual function with extension receiver results in wrong values at runtime in JS
- [`KT-42305`](https://youtrack.jetbrains.com/issue/KT-42305) KJS / IR: "Class constructor is marked as private" `@JsExport` produces wrong TS code for sealed classes
- [`KT-52563`](https://youtrack.jetbrains.com/issue/KT-52563) KJS / IR: Invalid TypeScript generated for class extending base class with private constructor

### Klibs

#### New Features

- [`KT-78699`](https://youtrack.jetbrains.com/issue/KT-78699) Compiler (JS, Wasm): warn about incompatible kotlin-test/compiler pair
- [`KT-78700`](https://youtrack.jetbrains.com/issue/KT-78700) Compiler (JS, Wasm): Consider making diagnostics for incompatible kotlin-stdlib/compiler and kotlin-test/compiler pairs errors instead of warnings
- [`KT-74815`](https://youtrack.jetbrains.com/issue/KT-74815) KLIB resolver can't consume metadata klibs between source sets when abi_versions diverge
- [`KT-68322`](https://youtrack.jetbrains.com/issue/KT-68322) Compiler (JS, Wasm): warn about incompatible Kotlin stdlib/compiler pair

#### Fixes

- [`KT-78168`](https://youtrack.jetbrains.com/issue/KT-78168) K/N: "IndexOutOfBoundsException: Index 3 out of bounds for length 3" for iOS build with Kotlin 2.2.0-RC2
- [`KT-75766`](https://youtrack.jetbrains.com/issue/KT-75766) PL: Error on building fake override with multiple overridden members with unbound symbols in return type
- [`KT-75757`](https://youtrack.jetbrains.com/issue/KT-75757) PL: Error on building fake overrides with unbound symbols in value parameters
- [`KT-76094`](https://youtrack.jetbrains.com/issue/KT-76094) Support new callable reference nodes in partial linkage in Kotlin/Wasm
- [`KT-78771`](https://youtrack.jetbrains.com/issue/KT-78771) KLIBs: Improve `zipDirAs()` function that is used to produce KLIB (ZIP) archives
- [`KT-75980`](https://youtrack.jetbrains.com/issue/KT-75980) [Klib] Reduce serialized size of IrFileEntries for sparse usage of another source files
- [`KT-78349`](https://youtrack.jetbrains.com/issue/KT-78349) [Tests] Enable Partial Linkage in all tests
- [`KT-76827`](https://youtrack.jetbrains.com/issue/KT-76827) KLIB cross-compilation tests: Don't use IR hashes and metadata hashes in test data
- [`KT-76266`](https://youtrack.jetbrains.com/issue/KT-76266) Move trigger of :tools:binary-compatibility-validator:check to native/native.tests/klib-ir-inliner
- [`KT-76725`](https://youtrack.jetbrains.com/issue/KT-76725) KLIB ABI export in older version: Restore legacy directories
- [`KT-76061`](https://youtrack.jetbrains.com/issue/KT-76061) Add option for suppress warning of missing no-existent transitive klib dependencies
- [`KT-76471`](https://youtrack.jetbrains.com/issue/KT-76471) Partial linkage: add an attribute if a class is invalid
- [`KT-75192`](https://youtrack.jetbrains.com/issue/KT-75192) KLIB reader tends to extract files from the KLIB archive to a temporary directory even when this is not needed

### Language Design

- [`KT-78866`](https://youtrack.jetbrains.com/issue/KT-78866) Warn implicit receiver shadowed by context parameter
- [`KT-54363`](https://youtrack.jetbrains.com/issue/KT-54363) Allow using reified types for catch parameters
- [`KT-32993`](https://youtrack.jetbrains.com/issue/KT-32993) Contract to specify that a function parameter is always true inside lambda
- [`KT-79308`](https://youtrack.jetbrains.com/issue/KT-79308) Ability to actualize empty interfaces as Any
- [`KT-8889`](https://youtrack.jetbrains.com/issue/KT-8889) Contracts: if a given function parameter is not null, the result is not null
- [`KT-22786`](https://youtrack.jetbrains.com/issue/KT-22786) Returns are not allowed for expression-body functions and are allowed when an inline lambda is added
- [`KT-77836`](https://youtrack.jetbrains.com/issue/KT-77836) Support using context parameter of a `@RestrictsSuspension` type as the "restricted coroutine scope"
- [`KT-77823`](https://youtrack.jetbrains.com/issue/KT-77823) Context-sensitive resolution doesn't work for subtypes of sealed types
- [`KT-75977`](https://youtrack.jetbrains.com/issue/KT-75977) False positive unresolved_reference when resolving nested member after a type check
- [`KT-73557`](https://youtrack.jetbrains.com/issue/KT-73557) Allow refining expect declarations for platform groups

### Libraries

#### New Features

- [`KT-76389`](https://youtrack.jetbrains.com/issue/KT-76389) Provide `update` functions for common atomics
- [`KT-78581`](https://youtrack.jetbrains.com/issue/KT-78581) Add the KClass.isInterface property to Kotlin/JS stdlib
- [`KT-34132`](https://youtrack.jetbrains.com/issue/KT-34132) Contract for ClosedRange<T>.contains(T?) operator
- [`KT-73853`](https://youtrack.jetbrains.com/issue/KT-73853) Provide vararg constructors for Atomic Arrays

#### Fixes

- [`KT-71628`](https://youtrack.jetbrains.com/issue/KT-71628) Review deprecations in stdlib for 2.1
- [`KT-76773`](https://youtrack.jetbrains.com/issue/KT-76773) stdlib: contextOf's type argument can be inferred via contextOf's context argument
- [`KT-79489`](https://youtrack.jetbrains.com/issue/KT-79489) Generate Stdlib API reference for webMain source set
- [`KT-79080`](https://youtrack.jetbrains.com/issue/KT-79080) Annotate WasmImport and WasmExport as experimental API
- [`KT-79121`](https://youtrack.jetbrains.com/issue/KT-79121) K/Wasm annotate JS-interop API as experimental
- [`KT-78710`](https://youtrack.jetbrains.com/issue/KT-78710) kotlin.wasm and kotlin.wasm.unsafe packages are missing description
- [`KT-78709`](https://youtrack.jetbrains.com/issue/KT-78709) Wasm: KClass.qualifiedName KDoc should reflect the behavior on the target
- [`KT-78704`](https://youtrack.jetbrains.com/issue/KT-78704) CharSequence.subSequence and String.substring behavior with invalid indices differs between targets
- [`KT-78705`](https://youtrack.jetbrains.com/issue/KT-78705) Float.sign and Double.sign behavior for negative zero is not documented
- [`KT-74543`](https://youtrack.jetbrains.com/issue/KT-74543) Support for context parameters in kotlinx-metadata
- [`KT-78340`](https://youtrack.jetbrains.com/issue/KT-78340) String.startsWith KDoc declares invalid exception condition
- [`KT-78242`](https://youtrack.jetbrains.com/issue/KT-78242) Move IrLinkageError to the common non-JVM part of the standard library
- [`KT-67819`](https://youtrack.jetbrains.com/issue/KT-67819) Document collection interfaces contracts

### Native

- [`KT-79075`](https://youtrack.jetbrains.com/issue/KT-79075) Stuck on Kotlin_getSourceInfo_core_symbolication
- [`KT-76178`](https://youtrack.jetbrains.com/issue/KT-76178) LLVM Update: symbol '__ZnwmSt19__type_descriptor_t' missing
- [`KT-78959`](https://youtrack.jetbrains.com/issue/KT-78959) Xcode 26: fix GC stress tests
- [`KT-78734`](https://youtrack.jetbrains.com/issue/KT-78734) Finish runtime crash dump generation
- [`KT-74662`](https://youtrack.jetbrains.com/issue/KT-74662) Consider providing a way to enable stack canaries for Kotlin/Native binaries
- [`KT-77378`](https://youtrack.jetbrains.com/issue/KT-77378) [macos] Loading libraries with non resolved paths runs XProtectService
- [`KT-61549`](https://youtrack.jetbrains.com/issue/KT-61549) Kotlin/Native: remove kotlin-native/Interop/JsRuntime
- [`KT-76563`](https://youtrack.jetbrains.com/issue/KT-76563) LLVM Update: numerous "was built for newer 'macOS' version" warnings

### Native. Build Infrastructure

- [`KT-77349`](https://youtrack.jetbrains.com/issue/KT-77349) Kotlin/Native: default cache for stdlib is unused

### Native. C and ObjC Import

- [`KT-79571`](https://youtrack.jetbrains.com/issue/KT-79571) Xcode 26 beta 4: CInteropKT39120TestGenerated.testForwardEnum failed
- [`KT-71400`](https://youtrack.jetbrains.com/issue/KT-71400) Fix disabled -fmodules testing for stdarg.h

### Native. ObjC Export

#### New Features

- [`KT-77488`](https://youtrack.jetbrains.com/issue/KT-77488) [ObjCExport] Add explicit ObjCBlock parameter name in objc export
- [`KT-76974`](https://youtrack.jetbrains.com/issue/KT-76974) Include conflicting element in objc export warnings
- [`KT-76338`](https://youtrack.jetbrains.com/issue/KT-76338) Native, ObjCExport: Replace name mangling of special method families

#### Fixes

- [`KT-55648`](https://youtrack.jetbrains.com/issue/KT-55648) Native: produce smaller binaries
- [`KT-78447`](https://youtrack.jetbrains.com/issue/KT-78447) [ObjCExport] Add missing ERROR constructors, align with K1
- [`KT-78034`](https://youtrack.jetbrains.com/issue/KT-78034) ObjCExport: primitive type extension translated as static method
- [`KT-77781`](https://youtrack.jetbrains.com/issue/KT-77781) ObjCExport: support `@ObjCName` for function parameters and receiver parameters
- [`KT-77592`](https://youtrack.jetbrains.com/issue/KT-77592) KMP plugin uses incorrect Swift name from ObjCName annotation
- [`KT-77625`](https://youtrack.jetbrains.com/issue/KT-77625) ObjCExport: ObjCName annotation adds kotlin name swift_name
- [`KT-77484`](https://youtrack.jetbrains.com/issue/KT-77484) KotlinConf app: Invalid identifiers in `ObjCHeader.render`
- [`KT-77500`](https://youtrack.jetbrains.com/issue/KT-77500) `IllegalStateException` during generating ObjC header stubs

### Native. Runtime

- [`KT-79152`](https://youtrack.jetbrains.com/issue/KT-79152) Native: unexpected thread state in kotlin::to_string<KStringConversionMode>

### Native. Runtime. Memory

- [`KT-78925`](https://youtrack.jetbrains.com/issue/KT-78925) Crash SIGABRT on Apple Watch after updating Kotlin to 2.2.0
- [`KT-76851`](https://youtrack.jetbrains.com/issue/KT-76851) Kotlin/Native: GC scheduler MutatorAssists requestAssists and completeEpoch issue
- [`KT-63143`](https://youtrack.jetbrains.com/issue/KT-63143) Kotlin/Native: execute Cleaners on the finalizer thread

### Native. Swift Export

- [`KT-79105`](https://youtrack.jetbrains.com/issue/KT-79105) ConcurrentModificationException During Swift Export Caused by Usage of Array
- [`KT-79227`](https://youtrack.jetbrains.com/issue/KT-79227) Swift Export: Fix First Release Issues
- [`KT-78947`](https://youtrack.jetbrains.com/issue/KT-78947) Implement FUS for Swift export
- [`KT-79521`](https://youtrack.jetbrains.com/issue/KT-79521) '_CoroutineScope' is inaccessible due to 'internal' protection level
- [`KT-79181`](https://youtrack.jetbrains.com/issue/KT-79181) Swift Export Fails When Using T: Comparable<T> Generic Constraint in Kotlin Classes
- [`KT-77650`](https://youtrack.jetbrains.com/issue/KT-77650) Swift export execution tests fail with caches enabled
- [`KT-77634`](https://youtrack.jetbrains.com/issue/KT-77634) K/N: swift export tests started failing after hyper-existentials
- [`KT-77290`](https://youtrack.jetbrains.com/issue/KT-77290) Transitive Export on swift export can duplicate declarations

### Tools. Build Tools API

- [`KT-78415`](https://youtrack.jetbrains.com/issue/KT-78415) Add a tool for performance reports analysing

### Tools. CLI

#### New Features

- [`KT-75812`](https://youtrack.jetbrains.com/issue/KT-75812) Basic DSL for compiler arguments representation

#### Fixes

- [`KT-78318`](https://youtrack.jetbrains.com/issue/KT-78318) Unresolved reference when compiling kotlin/JS project on fresh master
- [`KT-75968`](https://youtrack.jetbrains.com/issue/KT-75968) Set proper lifecycle for all existing compiler arguments
- [`KT-77445`](https://youtrack.jetbrains.com/issue/KT-77445)  UNRESOLVED_REFERENCE when importing classes from kotlin-stdlib
- [`KT-77030`](https://youtrack.jetbrains.com/issue/KT-77030) Implement setup of HMPP sessions for KLib-based compilers
- [`KT-78578`](https://youtrack.jetbrains.com/issue/KT-78578) Support for placeholder (*) and directory in `-Xdump-perf`
- [`KT-78129`](https://youtrack.jetbrains.com/issue/KT-78129) Compiler cannot parse -Xfragment-dependency with a comma in the path
- [`KT-76828`](https://youtrack.jetbrains.com/issue/KT-76828) Warning doesn't exist error with -Xwarning-level when the source file has no code
- [`KT-76957`](https://youtrack.jetbrains.com/issue/KT-76957) Incorrect error message when severity is set with -Xsuppress-warning and -Xwarning-level for the same diagnostic
- [`KT-76829`](https://youtrack.jetbrains.com/issue/KT-76829) UnsupportedOperationException when reenabling a taking place warning with -Xwarning-level
- [`KT-76111`](https://youtrack.jetbrains.com/issue/KT-76111) kotlinc warns about org.fusesource.jansi.internal.JansiLoader call to System.load
- [`KT-76447`](https://youtrack.jetbrains.com/issue/KT-76447) Remove -Xjps compiler argument

### Tools. Compiler Plugin API

- [`KT-78279`](https://youtrack.jetbrains.com/issue/KT-78279) Make the DiagnosticReporter default way for reporting in IR plugins
- [`KT-77157`](https://youtrack.jetbrains.com/issue/KT-77157) Cannot create a symbol pointer for local class generated by FirFunctionCallRefinementExtension

### Tools. Compiler Plugins

#### New Features

- [`KT-78038`](https://youtrack.jetbrains.com/issue/KT-78038) Make jvm-abi-gen compiler plugin output classloader-friendly
- [`KT-77339`](https://youtrack.jetbrains.com/issue/KT-77339) Update kotlin dataframe dependency to 1.0.0-dev-6925

#### Fixes

- [`KT-78969`](https://youtrack.jetbrains.com/issue/KT-78969) [DataFrame] Provide source elements for plugin-generated classes
- [`KT-75265`](https://youtrack.jetbrains.com/issue/KT-75265) PowerAssert: the result of invoke is displayed at the same level as value that can be confusing
- [`KT-78490`](https://youtrack.jetbrains.com/issue/KT-78490) "AssertionError: SyntheticAccessorLowering should not attempt to modify other files" when calling protected open composable with default argument
- [`KT-77626`](https://youtrack.jetbrains.com/issue/KT-77626) K2: AssertionError: FUN LOCAL_FUNCTION_FOR_LAMBDA has no continuation
- [`KT-78671`](https://youtrack.jetbrains.com/issue/KT-78671) [DataFrame] Support type parameter types in DataSchema to fix evaluate expression
- [`KT-78439`](https://youtrack.jetbrains.com/issue/KT-78439) DataFrame compiler plugin: Unresolved reference error in REPL
- [`KT-75876`](https://youtrack.jetbrains.com/issue/KT-75876) PowerAssert: don't display results for assertion operator
- [`KT-75514`](https://youtrack.jetbrains.com/issue/KT-75514) [JS][Native] Add IrPreSerializationLoweringFacade to Atomicfu test runners
- [`KT-77719`](https://youtrack.jetbrains.com/issue/KT-77719) Remove suppress INVISIBLE_REFERENCE from DataFrame plugin
- [`KT-77691`](https://youtrack.jetbrains.com/issue/KT-77691) Kotlin DataFrame plugin: IR and FIR anonymous functions have inconsistent receivers
- [`KT-77455`](https://youtrack.jetbrains.com/issue/KT-77455) kotlin-dataframe plugin throws NoClassDefFoundError in IDE
- [`KT-77437`](https://youtrack.jetbrains.com/issue/KT-77437) Kotlin DataFrame: Add configuration key to disable top level properties generator
- [`KT-74366`](https://youtrack.jetbrains.com/issue/KT-74366) Delete kotlin-android-extensions compiler plugin
- [`KT-73364`](https://youtrack.jetbrains.com/issue/KT-73364) Migrate atomicfu sources to new IR parameter API

### Tools. Compiler plugins. Serialization

- [`KT-79695`](https://youtrack.jetbrains.com/issue/KT-79695) Serialization does not exclude field-less properties in 2.2.20-Beta2
- [`KT-73365`](https://youtrack.jetbrains.com/issue/KT-73365) Migrate kotlinx-serialization sources to new IR parameter API

### Tools. Gradle

#### New Features

- [`KT-76421`](https://youtrack.jetbrains.com/issue/KT-76421) Stabilize klib cross-compilation on different platforms
- [`KT-77107`](https://youtrack.jetbrains.com/issue/KT-77107) Introduce Kotlin ecosystem plugin

#### Fixes

- [`KT-80172`](https://youtrack.jetbrains.com/issue/KT-80172) Error message changes depending on the order of applying 'org.jetbrains.kotlin.android' and 'AGP' 9.0+ with built-in Kotlin plugin
- [`KT-77546`](https://youtrack.jetbrains.com/issue/KT-77546) Implement basic support for HMPP compilation scheme support in KGP
- [`KT-79034`](https://youtrack.jetbrains.com/issue/KT-79034) Automatically disable cross compilation if it's not supported on the host
- [`KT-79408`](https://youtrack.jetbrains.com/issue/KT-79408) A lot of errors files are created when compile Kotlin
- [`KT-77785`](https://youtrack.jetbrains.com/issue/KT-77785) Add -fmodules option to CocoaPod dependency by default
- [`KT-75921`](https://youtrack.jetbrains.com/issue/KT-75921) Make Swift Export available by default
- [`KT-63383`](https://youtrack.jetbrains.com/issue/KT-63383) Add compiler performance metrics to Native build reports
- [`KT-77023`](https://youtrack.jetbrains.com/issue/KT-77023) Support creating KotlinJvmAndroidCompilation in KotlinBaseApiPlugin
- [`KT-74420`](https://youtrack.jetbrains.com/issue/KT-74420) Migrate kotlin-parcelize away from AGP's deprecated Variant API
- [`KT-78233`](https://youtrack.jetbrains.com/issue/KT-78233) Add ExperimentalFeatureWarning unique id
- [`KT-67992`](https://youtrack.jetbrains.com/issue/KT-67992) Cleanup deprecated code required for KSP1
- [`KT-72341`](https://youtrack.jetbrains.com/issue/KT-72341) Remove 'kotlin-android-extensions' plugin
- [`KT-67291`](https://youtrack.jetbrains.com/issue/KT-67291) Enable Project Isolation AND/OR Configuration Cache mode for Gradle Integration tests
- [`KT-78325`](https://youtrack.jetbrains.com/issue/KT-78325) Kotlin ecosystem plugin rejects compatible Gradle patch version when DCL is enabled
- [`KT-76353`](https://youtrack.jetbrains.com/issue/KT-76353) Handle migration to stable -jvm-default in KGP: replace deprecated option and suppress warnings
- [`KT-76797`](https://youtrack.jetbrains.com/issue/KT-76797) KGP: StdlibDependencyManagementKt.configureStdlibVersionAlignment() triggering eager configuration realization
- [`KT-77163`](https://youtrack.jetbrains.com/issue/KT-77163) Migrate Swift Export IT to injections
- [`KT-76282`](https://youtrack.jetbrains.com/issue/KT-76282) Add missing Android Gradle plugin versions in tests
- [`KT-77011`](https://youtrack.jetbrains.com/issue/KT-77011) Update build regression benchmarks for 2.2.0 release
- [`KT-76138`](https://youtrack.jetbrains.com/issue/KT-76138) Compile against Gradle API 8.14
- [`KT-76139`](https://youtrack.jetbrains.com/issue/KT-76139) Run integration tests against Gradle 8.14
- [`KT-77035`](https://youtrack.jetbrains.com/issue/KT-77035) A compiler diagnostic isn't reported when its severity is set to warning with Gradle
- [`KT-76951`](https://youtrack.jetbrains.com/issue/KT-76951) 'distribution-base' plugin is only applied in Gradle 8.13
- [`KT-73142`](https://youtrack.jetbrains.com/issue/KT-73142) Kotlin Gradle plugin: Remove usage of Gradle's internal ExecHandleBuilder
- [`KT-76740`](https://youtrack.jetbrains.com/issue/KT-76740) Use Problems API for warning introduced in KT-75808
- [`KT-65271`](https://youtrack.jetbrains.com/issue/KT-65271) Gradle: "Mutating dependency DefaultExternalModuleDependency after it has been finalized has been deprecated " with gradle 8.6-rc-3

### Tools. Gradle. Cocoapods

- [`KT-76035`](https://youtrack.jetbrains.com/issue/KT-76035) Allow extra command line arguments in PodBuildTask
- [`KT-78387`](https://youtrack.jetbrains.com/issue/KT-78387) Kotlin Cocoapods Gradle Plugin is not compatible with Gradle isolated projects
- [`KT-79429`](https://youtrack.jetbrains.com/issue/KT-79429) K/N: Cocoapods: IllegalArgumentException: "cinterop doesn't support having headers in -fmodules mode" with 2.2.20-Beta1 if explicitly not specify false for 'useClangModules'

### Tools. Gradle. Compiler plugins

- [`KT-66728`](https://youtrack.jetbrains.com/issue/KT-66728) Deprecate `kapt.use.k2` property

### Tools. Gradle. JS

#### New Features

- [`KT-75480`](https://youtrack.jetbrains.com/issue/KT-75480) Add shared source set for js and wasmJs target
- [`KT-77073`](https://youtrack.jetbrains.com/issue/KT-77073) generateTypeScriptDefinitions() does not add generated .d.ts file to package.json automatically

#### Fixes

- [`KT-77319`](https://youtrack.jetbrains.com/issue/KT-77319) KJS / Gradle: generateTypeScriptDefinitions() generates wrong file extension when outputting ES modules
- [`KT-79921`](https://youtrack.jetbrains.com/issue/KT-79921) Web Tooling Gradle API does not respect webpack reconfiguration
- [`KT-76996`](https://youtrack.jetbrains.com/issue/KT-76996) Wasm: js tasks triggers wasm subtasks
- [`KT-79237`](https://youtrack.jetbrains.com/issue/KT-79237) Upgrade NPM dependencies versions
- [`KT-79188`](https://youtrack.jetbrains.com/issue/KT-79188) Pre-generated accessors aren't available for webMain / webTest source sets
- [`KT-78504`](https://youtrack.jetbrains.com/issue/KT-78504) [2.2.0-RC3] NPM Tasks in 2.2 RCs produce broken/unusable build cache entries
- [`KT-77443`](https://youtrack.jetbrains.com/issue/KT-77443) NPE: "NullPointerException: Cannot invoke org.gradle.api.tasks.TaskProvider.flatMap(org.gradle.api.Transformer)": ExecutableWasm.optimizeTask is accessed before initialization
- [`KT-76987`](https://youtrack.jetbrains.com/issue/KT-76987) JS, Wasm: Upgrade NPM dependencies
- [`KT-77119`](https://youtrack.jetbrains.com/issue/KT-77119) KJS: Gradle: Setting custom environment variables in KotlinJsTest tasks no longer works
- [`KT-74735`](https://youtrack.jetbrains.com/issue/KT-74735) KGP uses Gradle internal `CompositeProjectComponentArtifactMetadata`

### Tools. Gradle. Multiplatform

#### New Features

- [`KT-69790`](https://youtrack.jetbrains.com/issue/KT-69790) Report human-readable error when declared dependency doesn't support required target types
- [`KT-76446`](https://youtrack.jetbrains.com/issue/KT-76446) Add kotlin-level dependency block to work the same way as commonMain/commonTest dependencies blocks

#### Fixes

- [`KT-78297`](https://youtrack.jetbrains.com/issue/KT-78297) FileNotFoundException in generateMetadataFile task if non-packed=false
- [`KT-62294`](https://youtrack.jetbrains.com/issue/KT-62294) kotlin-parcelize plugin does not support the new android kotlin multiplatform plugin
- [`KT-77404`](https://youtrack.jetbrains.com/issue/KT-77404) The kotlin-stdlib and annotations are missing from commonTest dependencies with 2.2.0-Beta1
- [`KT-79559`](https://youtrack.jetbrains.com/issue/KT-79559) AGP complains about configurations resolved at configuration time due to KMP partially resolved dependencies diagnostic
- [`KT-78993`](https://youtrack.jetbrains.com/issue/KT-78993) The value for property '*' property 'dependencies' is final and cannot be changed any further
- [`KT-77843`](https://youtrack.jetbrains.com/issue/KT-77843) KGP fails with Gradle 9 on `ProjectDependency.getDependencyProject()`
- [`KT-79315`](https://youtrack.jetbrains.com/issue/KT-79315) Early task materialization with cross-project configuration breaks configuration due to KMP partial resolution checker
- [`KT-77466`](https://youtrack.jetbrains.com/issue/KT-77466) KMP - testFixturesApi and similar configurations do not affect jvmTestFixtures source set
- [`KT-78433`](https://youtrack.jetbrains.com/issue/KT-78433) Gradle: add tracking of the new KMP compilation scheme to FUS
- [`KT-78431`](https://youtrack.jetbrains.com/issue/KT-78431) Gradle: in-process metadata compiler uses deprecated K2MetadataCompiler
- [`KT-77414`](https://youtrack.jetbrains.com/issue/KT-77414) KMP dependencies in detached source sets cause IDE resolution to write error logs: "kotlin-project-structure-metadata.json (No such file or directory)"
- [`KT-76200`](https://youtrack.jetbrains.com/issue/KT-76200) TestModuleProperties.productionModuleName for JVM module isn't present with 2.1.20-RC

### Tools. Gradle. Native

- [`KT-51301`](https://youtrack.jetbrains.com/issue/KT-51301) Remove ability to use Native non-embeddable compiler jar in Gradle plugin
- [`KT-74864`](https://youtrack.jetbrains.com/issue/KT-74864) Enable exporting KDocs by default to ObjC
- [`KT-77977`](https://youtrack.jetbrains.com/issue/KT-77977) "Unknown hardware platform: riscv64" on JVM project build
- [`KT-78838`](https://youtrack.jetbrains.com/issue/KT-78838) Add default 3G max heap size for the commonizer JVM process
- [`KT-68256`](https://youtrack.jetbrains.com/issue/KT-68256) Reduce commonizer max heap size to default 3g and allow users to configure it
- [`KT-77067`](https://youtrack.jetbrains.com/issue/KT-77067) Kotlin Gradle plugin with the configuration cache passes all platform libraries to the compiler when compiling a binary for the first time

### Tools. Gradle. Swift Export

- [`KT-79554`](https://youtrack.jetbrains.com/issue/KT-79554) Swift Export status diagnostic is produced even if swift export is not configured
- [`KT-78385`](https://youtrack.jetbrains.com/issue/KT-78385) Swift Export is not compatible with Gradle isolated projects
- [`KT-79524`](https://youtrack.jetbrains.com/issue/KT-79524) NoSuchMethodError: 'java.lang.String org.gradle.api.artifacts.ProjectDependency.getPath() for swift export with dependency export fro gradle < 8.11

### Tools. Incremental Compile

- [`KT-60653`](https://youtrack.jetbrains.com/issue/KT-60653) IC does not handle changes in inline functions objects/lambdas correctly
- [`KT-78807`](https://youtrack.jetbrains.com/issue/KT-78807) Changing ABI fingerprint on non-ABI changes when lambda passed to inlined function
- [`KT-69075`](https://youtrack.jetbrains.com/issue/KT-69075) Incremental compilation: smartcast is impossible on field with `@JvmName`

### Tools. JPS

- [`KT-77347`](https://youtrack.jetbrains.com/issue/KT-77347) Support file-less compatible IC approach
- [`KT-78444`](https://youtrack.jetbrains.com/issue/KT-78444) Clean up JPS code base
- [`KT-75460`](https://youtrack.jetbrains.com/issue/KT-75460)  Adding `@PurelyImplements` annotation to a List does *not* cause incremental recompile of affected files
- [`KT-50594`](https://youtrack.jetbrains.com/issue/KT-50594) Fix org.jetbrains.kotlin.arguments.CompilerArgumentsContentProspectorTest

### Tools. Kapt

- [`KT-79138`](https://youtrack.jetbrains.com/issue/KT-79138) K2: KAPT Java Stub Gen: `Unresolved reference` with `@kotlin`.Metadata in Java in 2.2.0
- [`KT-79641`](https://youtrack.jetbrains.com/issue/KT-79641) Kapt: too much information is printed in verbose mode
- [`KT-79136`](https://youtrack.jetbrains.com/issue/KT-79136) K2 kapt: unresolved nested class references in annotation arguments are generated without outer class names
- [`KT-79133`](https://youtrack.jetbrains.com/issue/KT-79133) K2 kapt: class literal with typealias is not expanded
- [`KT-77853`](https://youtrack.jetbrains.com/issue/KT-77853) K2 KAPT: backend internal error: exception during IR fake override builder
- [`KT-73322`](https://youtrack.jetbrains.com/issue/KT-73322) Migrate `FirKaptAnalysisHandlerExtension` compilation pipeline to the phased structure

### Tools. Maven

- [`KT-77587`](https://youtrack.jetbrains.com/issue/KT-77587) Maven: Introduce Kotlin daemon support and make it enabled by default
- [`KT-63688`](https://youtrack.jetbrains.com/issue/KT-63688) Remove JS-related stuff from kotlin-maven-plugin

### Tools. Maven. Compiler plugins

- [`KT-77511`](https://youtrack.jetbrains.com/issue/KT-77511) Add maven plugin for Kotlin DataFrame plugin

### Tools. REPL

- [`KT-78755`](https://youtrack.jetbrains.com/issue/KT-78755) [K2 Repl] Redeclaring variables does not work
- [`KT-75632`](https://youtrack.jetbrains.com/issue/KT-75632) Contunue deprecation of the REPL built into `kotlinc`
- [`KT-77470`](https://youtrack.jetbrains.com/issue/KT-77470) [K2 Repl] Lazy Properties crash code generation
- [`KT-76507`](https://youtrack.jetbrains.com/issue/KT-76507) [K2 Repl] Delegated properties are not visible in the next snippet
- [`KT-76508`](https://youtrack.jetbrains.com/issue/KT-76508) [K2 Repl] Annotations on property accessors are not resolved
- [`KT-75672`](https://youtrack.jetbrains.com/issue/KT-75672) [K2 Repl] Serialization plugin crashes compiler backend

### Tools. Scripts

- [`KT-78378`](https://youtrack.jetbrains.com/issue/KT-78378) "Explain" feature of the kotlin script fails on hidden variables

### Tools. Statistics (FUS)

- [`KT-79455`](https://youtrack.jetbrains.com/issue/KT-79455) [FUS] Collect KSP plugin version
- [`KT-77755`](https://youtrack.jetbrains.com/issue/KT-77755) [FUS Pipeline] Fus file format
- [`KT-77995`](https://youtrack.jetbrains.com/issue/KT-77995) Do not collect FUS metrics on TeamCity

### Tools. Wasm

- [`KT-76842`](https://youtrack.jetbrains.com/issue/KT-76842) K/Wasm: serve project sources in *DevRun tasks by default
- [`KT-78921`](https://youtrack.jetbrains.com/issue/KT-78921) K/Wasm: don't generate empty yarn.lock file
- [`KT-75714`](https://youtrack.jetbrains.com/issue/KT-75714) Wasm: Move tooling NPM dependencies outside user project
- [`KT-70013`](https://youtrack.jetbrains.com/issue/KT-70013) .gradle/yarn and .gradle/node are part of Gradle configuration cache
- [`KT-76838`](https://youtrack.jetbrains.com/issue/KT-76838) K/Wasm: No possible to set downloadBaseUrl to null for D8 and Binaryen
- [`KT-76948`](https://youtrack.jetbrains.com/issue/KT-76948) Wasm: Rename kotlinBinaryenSetup and kotlinD8Setup


## 2.2.10

### Compiler

- [`KT-79276`](https://youtrack.jetbrains.com/issue/KT-79276) Dexing fails with "Cannot read field X because <local0> is null" with 2.2.0
- [`KT-79442`](https://youtrack.jetbrains.com/issue/KT-79442) "Multiple annotations of type kotlin.coroutines.jvm.internal.DebugMetadata": 2.2.0-Beta1 generates broken code with JVM default suspend methods in interfaces
- [`KT-78815`](https://youtrack.jetbrains.com/issue/KT-78815) `Symbol not found: __ZNSt3__117bad_function_callD1Ev` error on iOS 15.5 simulator in Xcode 16.3 after update to 2.2.0-Beta2
- [`KT-78501`](https://youtrack.jetbrains.com/issue/KT-78501) K2: Missing [ABSTRACT_SUPER_CALL] diagnostics for delegated interface method leads to AssertionError: isCompiledToJvmDefault during IR lowering
- [`KT-78479`](https://youtrack.jetbrains.com/issue/KT-78479)  IR lowering failed / Unexpected null argument for composable call
- [`KT-76477`](https://youtrack.jetbrains.com/issue/KT-76477) Kotlin/Native: fix compiler performance reporting in sources->klib and klibs->binary
- [`KT-78736`](https://youtrack.jetbrains.com/issue/KT-78736) Missing [NOT_YET_SUPPORTED_IN_INLINE] diagnostics because of incorrect context update
- [`KT-77685`](https://youtrack.jetbrains.com/issue/KT-77685) "IllegalArgumentException: Sequence contains more than one matching element"
- [`KT-76365`](https://youtrack.jetbrains.com/issue/KT-76365) K2: Missing ABSTRACT_SUPER_CALL
- [`KT-78352`](https://youtrack.jetbrains.com/issue/KT-78352) False-positive IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE when comparing with equality operator (==)

### Compose compiler

- [`KT-78479`](https://youtrack.jetbrains.com/issue/KT-78479) Ensure that default transform affects functions entered through a call
- [`KT-78490`](https://youtrack.jetbrains.com/issue/KT-78490) Fix visibility for default wrappers of protected methods
- [`b/408492167`](https://issuetracker.google.com/issues/408492167) Emit parameter names in Compose source information

### JavaScript

- [`KT-79050`](https://youtrack.jetbrains.com/issue/KT-79050) KJS / IC: "Unexpected body of primary constructor for processing irClass"
- [`KT-79089`](https://youtrack.jetbrains.com/issue/KT-79089) KJS: Could not load reporter / Cannot find module 'mocha' when running jsNode tests

### Native

- [`KT-79075`](https://youtrack.jetbrains.com/issue/KT-79075) Stuck on Kotlin_getSourceInfo_core_symbolication
- [`KT-76178`](https://youtrack.jetbrains.com/issue/KT-76178) LLVM Update: symbol '__ZnwmSt19__type_descriptor_t' missing

### Native. Runtime. Memory

- [`KT-78925`](https://youtrack.jetbrains.com/issue/KT-78925) Crash SIGABRT on Apple Watch after updating Kotlin to 2.2.0

### Tools. CLI

- [`KT-77445`](https://youtrack.jetbrains.com/issue/KT-77445)  UNRESOLVED_REFERENCE when importing classes from kotlin-stdlib
- [`KT-78263`](https://youtrack.jetbrains.com/issue/KT-78263) java.lang.NoClassDefFoundError: Could not initialize class com.intellij.psi.impl.PsiSubstitutorImpl
- [`KT-78318`](https://youtrack.jetbrains.com/issue/KT-78318) Unresolved reference when compiling kotlin/JS project on fresh master

### Tools. Compiler Plugins

- [`KT-78490`](https://youtrack.jetbrains.com/issue/KT-78490) "AssertionError: SyntheticAccessorLowering should not attempt to modify other files" when calling protected open composable with default argument
- [`KT-78038`](https://youtrack.jetbrains.com/issue/KT-78038) Make jvm-abi-gen compiler plugin output classloader-friendly

### Tools. Gradle

- [`KT-77023`](https://youtrack.jetbrains.com/issue/KT-77023) Support creating KotlinJvmAndroidCompilation in KotlinBaseApiPlugin

### Tools. Gradle. JS

- [`KT-78504`](https://youtrack.jetbrains.com/issue/KT-78504) [2.2.0-RC3] NPM Tasks in 2.2 RCs produce broken/unusable build cache entries

### Tools. Gradle. Multiplatform

- [`KT-77466`](https://youtrack.jetbrains.com/issue/KT-77466) KMP - testFixturesApi and similar configurations do not affect jvmTestFixtures source set
- [`KT-68646`](https://youtrack.jetbrains.com/issue/KT-68646) Compose extension's metrics/reports dir should use subdirs based on target

### Tools. Gradle. Native

- [`KT-77977`](https://youtrack.jetbrains.com/issue/KT-77977) "Unknown hardware platform: riscv64" on JVM project build

### Tools. Incremental Compile

- [`KT-78807`](https://youtrack.jetbrains.com/issue/KT-78807) Changing ABI fingerprint on non-ABI changes when lambda passed to inlined function

### Tools. Kapt

- [`KT-77853`](https://youtrack.jetbrains.com/issue/KT-77853) K2 KAPT: backend internal error: exception during IR fake override builder
- [`KT-79138`](https://youtrack.jetbrains.com/issue/KT-79138) K2: KAPT Java Stub Gen: `Unresolved reference` with `@kotlin`.Metadata in Java in 2.2.0


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