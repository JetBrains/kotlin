## 2.5.0-Beta2

### Analysis API

- [`KT-60484`](https://youtrack.jetbrains.com/issue/KT-60484) Analysis API: add support for KtType pointers similar to KtSymbolPointer
- [`KT-67786`](https://youtrack.jetbrains.com/issue/KT-67786) Analysis API: Implement improved `KtType` hierarchy and design
- [`KT-68462`](https://youtrack.jetbrains.com/issue/KT-68462) Analysis API: Integrate `project-structure` module into `analysis-api` and `analysis-api-platform-interface`
- [`KT-68341`](https://youtrack.jetbrains.com/issue/KT-68341) Analysis API: Expanded function types from libraries don't have an abbreviated type
- [`KT-68857`](https://youtrack.jetbrains.com/issue/KT-68857) Analysis API: Refactor annotations
- [`KT-65215`](https://youtrack.jetbrains.com/issue/KT-65215) Analysis API: Distinguish APIs for Analysis API users and platforms
- [`KT-68884`](https://youtrack.jetbrains.com/issue/KT-68884) Analysis API: Rename/deprecate/remove declarations as part of Stabilization
- [`KT-69055`](https://youtrack.jetbrains.com/issue/KT-69055) Analysis API: Stabilize `KaScope`s
- [`KT-61775`](https://youtrack.jetbrains.com/issue/KT-61775) Analysis API: KtKClassAnnotationValue lacks complete type information
- [`KT-60884`](https://youtrack.jetbrains.com/issue/KT-60884) Analysis API: Improve standalone mode project model creation

### Analysis API. FIR

#### Performance Improvements

- [`KT-60240`](https://youtrack.jetbrains.com/issue/KT-60240) K2: optimize FirElementBuilder#getOrBuildFirForNonKtFileElement for `KtTypeReference` case
- [`KT-76375`](https://youtrack.jetbrains.com/issue/KT-76375) ContextCollector: terminate search if no elements were found with a designation
- [`KT-86758`](https://youtrack.jetbrains.com/issue/KT-86758) Don't require body resolution to obtain an explicit backing field
- [`KT-75496`](https://youtrack.jetbrains.com/issue/KT-75496) Doesn't resolve the entire designation path to the body phase for diagnostics

#### Fixes

- [`KT-74654`](https://youtrack.jetbrains.com/issue/KT-74654) K2 Analysis API: InvalidFirElementTypeException with annotated non-local destructuring declaration
- [`KT-81100`](https://youtrack.jetbrains.com/issue/KT-81100) Support jvmTargetProvider in low-level analysis API
- [`KT-75470`](https://youtrack.jetbrains.com/issue/KT-75470) ContextCollector: potentially incorrect scope for super type call
- [`KT-79057`](https://youtrack.jetbrains.com/issue/KT-79057) Fix all `// IGNORE_PHASE_VERIFICATION: invalid code inside annotations` directives
- [`KT-77041`](https://youtrack.jetbrains.com/issue/KT-77041) Expected is FirResolvedDeclarationStatus, but was FirDeclarationStatusImpl
- [`KT-60609`](https://youtrack.jetbrains.com/issue/KT-60609) K2 IDE: Navigation to super class of annotation opens `Any` instead of written super type
- [`KT-80721`](https://youtrack.jetbrains.com/issue/KT-80721) K2 AA.  "FirDeclaration was not found for class KtTypeParameter, fir is null" with generic type parameter in object expression
- [`KT-64899`](https://youtrack.jetbrains.com/issue/KT-64899) K2: PsiRawFirBuilder forces AST loading for scripts
- [`KT-85627`](https://youtrack.jetbrains.com/issue/KT-85627) AA: LLFlightRecorder initialization fails on runtimes without jdk.jfr
- [`KT-86185`](https://youtrack.jetbrains.com/issue/KT-86185) Analysis API: Make `FirDesignation` internal by removing the leak through `UsualClassTypeQualifierBuilder`
- [`KT-85410`](https://youtrack.jetbrains.com/issue/KT-85410) LLFirTypeTargetResolver: add more type guards
- [`KT-88111`](https://youtrack.jetbrains.com/issue/KT-88111) K2: Analysis API: make FileStructure element computation fully lazy

### Analysis API. Infrastructure

- [`KT-89711`](https://youtrack.jetbrains.com/issue/KT-89711) test-data-manager is incompatible with Isolated Projects
- [`KT-87672`](https://youtrack.jetbrains.com/issue/KT-87672) Eliminate class duplication in the Analysis API JARs
- [`KT-84914`](https://youtrack.jetbrains.com/issue/KT-84914) Do not publish analysis-api-test-framework
- [`KT-89119`](https://youtrack.jetbrains.com/issue/KT-89119) Enforce KaInternals prefix name schema

### Analysis API. Light Classes

- [`KT-87399`](https://youtrack.jetbrains.com/issue/KT-87399) LightClassUtil.getLightClassPropertyMethods returns wrong setter when JvmName annotation is used
- [`KT-70427`](https://youtrack.jetbrains.com/issue/KT-70427) SLC: non-abstract interface method generated as default with `-jvm-default=disable` option
- [`KT-88498`](https://youtrack.jetbrains.com/issue/KT-88498) Support multi-field value classes in light classes
- [`KT-89316`](https://youtrack.jetbrains.com/issue/KT-89316) SLC: `DefaultImpls` light class exposes `@JvmStatic` members of the interface companion object
- [`KT-89322`](https://youtrack.jetbrains.com/issue/KT-89322) UAST: NPE accessing containingFile of UParameter for mapped collection method
- [`KT-89073`](https://youtrack.jetbrains.com/issue/KT-89073) SLC: `@JvmOverloads` + `@JvmExposeBoxed`("bar") methods are exposed with the original name
- [`KT-84373`](https://youtrack.jetbrains.com/issue/KT-84373) Implement PsiMethod#isDefaultConstructor
- [`KT-89204`](https://youtrack.jetbrains.com/issue/KT-89204) SLC: False positive "unresolved reference" in Java for interface companion block members

### Analysis API. PSI

- [`KT-80780`](https://youtrack.jetbrains.com/issue/KT-80780) KtPsiUtil#getEnclosingElementForLocalDeclaration should return KtClassBody for enum entries members
- [`KT-89846`](https://youtrack.jetbrains.com/issue/KT-89846) PSI: Kotlin overrides of standard `PsiElement` mutation methods throw IllegalStateException without KtPsiMutationService
- [`KT-85052`](https://youtrack.jetbrains.com/issue/KT-85052) Move mutation methods out of the Kotlin PSI
- [`KT-88443`](https://youtrack.jetbrains.com/issue/KT-88443) Advance DeprecationLevel for PSI declarations deprecated in 2026.2 and earlier

### Analysis API. Providers and Caches

- [`KT-71359`](https://youtrack.jetbrains.com/issue/KT-71359) Perform validity checks before session acquisition
- [`KT-89816`](https://youtrack.jetbrains.com/issue/KT-89816) Analysis API tests: low-memory cache cleanup invalidates sessions in the middle of a test
- [`KT-86402`](https://youtrack.jetbrains.com/issue/KT-86402) False positive MISSING_DEPENDENCY_SUPERCLASS when using Scala case class in Kotlin

### Analysis API. Standalone

- [`KT-84851`](https://youtrack.jetbrains.com/issue/KT-84851) Analysis API Standalone: 75% of CPU time spent in StringsKt.split via trie-based library scopes
- [`KT-88362`](https://youtrack.jetbrains.com/issue/KT-88362) `PsiMethod.findSuperMethods()` always returns an empty array
- [`KT-61404`](https://youtrack.jetbrains.com/issue/KT-61404) Analysis API: implement proper library publishing structure
- [`KT-85112`](https://youtrack.jetbrains.com/issue/KT-85112) AA does not see packages from unpacked klibs

### Analysis API. Stubs and Decompilation

- [`KT-80758`](https://youtrack.jetbrains.com/issue/KT-80758) Consider to drop KotlinClassOrObjectStub#isLocal
- [`KT-81203`](https://youtrack.jetbrains.com/issue/KT-81203) Get rid of deprecated usage at ClsClassFinder.isKotlinInternalCompiledFile
- [`KT-88772`](https://youtrack.jetbrains.com/issue/KT-88772) Refine stubs surface
- [`KT-89376`](https://youtrack.jetbrains.com/issue/KT-89376) Rethink decompiler dependencies

### Analysis API. Surface

#### New Features

- [`KT-85384`](https://youtrack.jetbrains.com/issue/KT-85384) Provide use-site-aware API for Light Classes
- [`KT-88496`](https://youtrack.jetbrains.com/issue/KT-88496) Support multi-field inline classes in Analysis API Surface
- [`KT-78862`](https://youtrack.jetbrains.com/issue/KT-78862) Provide KaSymbol -> PsiElement API

#### Fixes

- [`KT-88129`](https://youtrack.jetbrains.com/issue/KT-88129) Advance `DeprecationLevel` for declarations deprecated in 2026.2 and earlier
- [`KT-73570`](https://youtrack.jetbrains.com/issue/KT-73570) Merge implementation of `KaExpressionTypeProvider.returnType` with `KaCallableSymbol.returnType`
- [`KT-86591`](https://youtrack.jetbrains.com/issue/KT-86591) Rethink how the context parameters feature could be integrated into the Analysis API Surface
- [`KT-82176`](https://youtrack.jetbrains.com/issue/KT-82176) KaFirUsualClassType has incorrect type argument for generic local class in generic function
- [`KT-66192`](https://youtrack.jetbrains.com/issue/KT-66192) Analysis API: Improve render customizability
- [`KT-83199`](https://youtrack.jetbrains.com/issue/KT-83199) Clarify API around extension points
- [`KT-77280`](https://youtrack.jetbrains.com/issue/KT-77280) Rename `KaPropertyAccessorSymbol#isCustom` to `isNotDefault`
- [`KT-60483`](https://youtrack.jetbrains.com/issue/KT-60483) Analysis API: add isTailrec property to KtFunctionSymbol
- [`KT-68882`](https://youtrack.jetbrains.com/issue/KT-68882) Analysis API: Refactor `KaSymbol`s
- [`KT-55124`](https://youtrack.jetbrains.com/issue/KT-55124) Design common ancestor for KtValueParameter and KtReceiverParameterSymbol
- [`KT-53669`](https://youtrack.jetbrains.com/issue/KT-53669) Analysis API: redesign KtSymbolOrigin to distinguish kotlin/java source/library declarations

### Backend. Wasm

#### Performance Improvements

- [`KT-88469`](https://youtrack.jetbrains.com/issue/KT-88469) K/Wasm: consider inlining suspendCoroutineUninterceptedOrReturn and getCoroutineContext intrinsics on 2nd Stage

#### Fixes

- [`KT-89394`](https://youtrack.jetbrains.com/issue/KT-89394) [Wasm] stop creating of CoroutineImplStackSwitching at the call cite of suspendCoroutineUninterceptedOrReturn
- [`KT-90119`](https://youtrack.jetbrains.com/issue/KT-90119) K/Wasm: ArenaLikeAllocator.allocationsToFree is only cleared on freeAllComponentModelReallocAllocatedMemory()
- [`KT-89267`](https://youtrack.jetbrains.com/issue/KT-89267) K/Wasm: calling a (T) -> R lambda with an argument that isn't a T traps instead of throwing ClassCastException
- [`KT-88235`](https://youtrack.jetbrains.com/issue/KT-88235) K/Wasm: Grouping test infra causes d8: error reading module error on Windows (path length problem?)
- [`KT-89657`](https://youtrack.jetbrains.com/issue/KT-89657) [Wasm] Insvestigate performance issues with primitive box caching
- [`KT-89548`](https://youtrack.jetbrains.com/issue/KT-89548) [Wasm] clean up type codegen for stack switching and avoid leaking too many continuation types to non-stdlib modules
- [`KT-89547`](https://youtrack.jetbrains.com/issue/KT-89547) [Wasm] add test config for running stack switching with incremental compilation
- [`KT-87742`](https://youtrack.jetbrains.com/issue/KT-87742) K/Wasm: initialization of properties with default value different from JVM
- [`KT-89315`](https://youtrack.jetbrains.com/issue/KT-89315) K/Wasm Standard Library Tests: Assertions are not enabled
- [`KT-89445`](https://youtrack.jetbrains.com/issue/KT-89445) Wasm: non-thread-safe instruction cache causes ClassCastException during parallel compilation
- [`KT-89345`](https://youtrack.jetbrains.com/issue/KT-89345) [Wasm] look into and unmute some CallableReferences benchmarks for Single Module Mode
- [`KT-86207`](https://youtrack.jetbrains.com/issue/KT-86207) k/wasm: Add box tests/support for multi-value value classes and callable ref merging
- [`KT-88291`](https://youtrack.jetbrains.com/issue/KT-88291) K/Wasm: Unsigned integer division by zero throws RuntimeError instead of ArithmeticException
- [`KT-87710`](https://youtrack.jetbrains.com/issue/KT-87710) [K/Wasm] initialization order of companion objects from super types differs from JVM 
- [`KT-73452`](https://youtrack.jetbrains.com/issue/KT-73452) K/Wasm: turning on range checks by default
- [`KT-86242`](https://youtrack.jetbrains.com/issue/KT-86242) K/Wasm: deprecate and prohibit using `wasmExports`

### Compiler

#### New Features

- [`KT-89201`](https://youtrack.jetbrains.com/issue/KT-89201) Implement "Strict equality for structural classes"
- [`KT-89096`](https://youtrack.jetbrains.com/issue/KT-89096) Rich Errors: Parser & PSI
- [`KT-89098`](https://youtrack.jetbrains.com/issue/KT-89098) Rich Errors: Resolution
- [`KT-89103`](https://youtrack.jetbrains.com/issue/KT-89103) Rich Errors: Checkers
- [`KT-89097`](https://youtrack.jetbrains.com/issue/KT-89097) Rich Errors: FIR Tree
- [`KT-80943`](https://youtrack.jetbrains.com/issue/KT-80943) Context-sensitive resolution fails to look further than a type
- [`KT-2264`](https://youtrack.jetbrains.com/issue/KT-2264) Emit a warning about ignored (unused) binary operation result
- [`KT-86868`](https://youtrack.jetbrains.com/issue/KT-86868) Update checkers to support `lateinit val`

### Compose compiler

#### Fixes
- [`b/485865131`](https://issuetracker.google.com/issues/485865131) Removed deprecated `IntrinsicRemember` and `StrongSkipping` Compose feature flags
- [`b/555304803`](https://issuetracker.google.com/issues/555304803) Fix parameter traversal in `ComposableLambdaTokenizer.captureInsnCount`
- [`b/539644912`](https://issuetracker.google.com/issues/539644912) [Compose] Support rich callable references for `@Composable` functions
- [`b/546101628`](https://issuetracker.google.com/issues/546101628) Fixed a bug that caused miscompilation whenever a `when` expression had a result that was computed by another `when` expression, and the inner `when` expression had a `@Composable` call in one of its conditions.
- [`b/549552317`](https://issuetracker.google.com/issues/549552317) Fixed a bug that, in certain cases, prevented execution of non-local return `statements` inside lambdas called from `when` expressions.
- [`b/481422057`](https://issuetracker.google.com/issues/481422057) Made the applier inferencer support repeated `ComposableTarget` annotations per the semantics described in the documentation of `ComposableTarget`
- [`b/530458587`](https://issuetracker.google.com/issues/530458587) Start initializing the fields of `ComposableSingletons` objects lazily

#### Performance Improvements

- [`KT-89840`](https://youtrack.jetbrains.com/issue/KT-89840) Native: bad incremental clean build performance
- [`KT-87570`](https://youtrack.jetbrains.com/issue/KT-87570) Kotlin/Native: improve RemoveRedundantCallsToStaticInitializersPhase
- [`KT-89224`](https://youtrack.jetbrains.com/issue/KT-89224) Significant performance drop in MT intellij (checkers)

#### Fixes

- [`KT-84904`](https://youtrack.jetbrains.com/issue/KT-84904) Alpha release of Multi-Field Value Classes
- [`KT-90352`](https://youtrack.jetbrains.com/issue/KT-90352) K/N: with -Xcompanion-blocks-and-extensions, a Kotlin subclass of an Objective-C class fails to link (undefined $clinit_trigger of the ObjC supertypes)
- [`KT-90235`](https://youtrack.jetbrains.com/issue/KT-90235) Compiler (JVM): drop requirement of `@JvmInline` for inline value class
- [`KT-90234`](https://youtrack.jetbrains.com/issue/KT-90234) Compiler: don't report inline modifier is deprecated in combination with value
- [`KT-90233`](https://youtrack.jetbrains.com/issue/KT-90233) Compiler: allow inline value modifier combination
- [`KT-89687`](https://youtrack.jetbrains.com/issue/KT-89687) [JVM] SpecialBuiltinsMapping.getDifferentNameForJvmBuiltinFunction computes JVM signature of the sub-declaration instead of the overridden built-in
- [`KT-89980`](https://youtrack.jetbrains.com/issue/KT-89980) K2 loads full value classes from klibs without their value class representation
- [`KT-89982`](https://youtrack.jetbrains.com/issue/KT-89982) No warning for Kotlin/Native WeakReference and createCleaner on value class and primitive instances
- [`KT-89983`](https://youtrack.jetbrains.com/issue/KT-89983) Partial linkage doesn't report a value class whose superclass became an identity class
- [`KT-89390`](https://youtrack.jetbrains.com/issue/KT-89390) K/N IC: per-file caches are not invalidated when toggling -Xcompanion-blocks
- [`KT-81760`](https://youtrack.jetbrains.com/issue/KT-81760) Kotlin/Native: incosistent handling of clashing declarations
- [`KT-90014`](https://youtrack.jetbrains.com/issue/KT-90014) K2 / JVM: `IllegalStateException: Don't know how to compile annotation value ERROR_CALL 'Unresolved reference: <Strange deserialized enum value: ...>'` when delegating to a Java interface annotated with a nested Kotlin enum
- [`KT-89655`](https://youtrack.jetbrains.com/issue/KT-89655) Prohibit lateinit in companion blocks in interfaces
- [`KT-89646`](https://youtrack.jetbrains.com/issue/KT-89646) Add JVM target bytecode version 28
- [`KT-86086`](https://youtrack.jetbrains.com/issue/KT-86086) Make `IntrinsicConstEvaluation` feature stable
- [`KT-87452`](https://youtrack.jetbrains.com/issue/KT-87452) Fix remaining issues in companion blocks and extensions on JVM
- [`KT-90169`](https://youtrack.jetbrains.com/issue/KT-90169) Callable references: Array passed as a single vararg element for generic vararg function since 2.4.0
- [`KT-89915`](https://youtrack.jetbrains.com/issue/KT-89915) JVM: ClassCastException 'kotlin.Result cannot be cast to ...' in an indy SAM-converted lambda whose Java SAM parameter is `@NotNull` 
- [`KT-89698`](https://youtrack.jetbrains.com/issue/KT-89698) JVM: `@Throws` is copied onto a bridge only when the overridden method is compiled from source, so incremental and clean builds produce different class files (JvmEnhancedBridges, 2.4.0)
- [`KT-85853`](https://youtrack.jetbrains.com/issue/KT-85853) CompanionBlocksAndExtensions: companion block val in interface generates illegal private static field (ClassFormatError 0x1A)
- [`KT-89451`](https://youtrack.jetbrains.com/issue/KT-89451) Suspend conversion: a `null` argument arrives non-null
- [`KT-39697`](https://youtrack.jetbrains.com/issue/KT-39697) "Cannot infer type parameter" in map with java static or global method reference
- [`KT-89892`](https://youtrack.jetbrains.com/issue/KT-89892) [K/N] latin1Strings binary option is ignored with caches
- [`KT-86132`](https://youtrack.jetbrains.com/issue/KT-86132) Drop support of language versions 2.0 and 2.1
- [`KT-86793`](https://youtrack.jetbrains.com/issue/KT-86793) setupCommonOptionsForCaches does not forward pagedAllocator (and other runtime constant options) to cache sub-compilations
- [`KT-89461`](https://youtrack.jetbrains.com/issue/KT-89461) KMP separate compilation: Incorrect fake-override is e/a mismatched in case of duplicated supertype in dependency class
- [`KT-82548`](https://youtrack.jetbrains.com/issue/KT-82548) K2: report a warning about annotations which target non-existent accessors
- [`KT-87295`](https://youtrack.jetbrains.com/issue/KT-87295) Enable Return Value Checker by default
- [`KT-89410`](https://youtrack.jetbrains.com/issue/KT-89410) Introduce WillBecomeValue annotation
- [`KT-89232`](https://youtrack.jetbrains.com/issue/KT-89232) Align context-sensitive resolution with how collection literals are resolved 
- [`KT-47891`](https://youtrack.jetbrains.com/issue/KT-47891) JVM: "IllegalStateException: Bad exception handler end" with nested try/run/finally blocks
- [`KT-89933`](https://youtrack.jetbrains.com/issue/KT-89933) Incorrect highlighting of a KDoc with a code block with comments
- [`KT-89576`](https://youtrack.jetbrains.com/issue/KT-89576) PSI path in script compilation still depends on K1
- [`KT-87596`](https://youtrack.jetbrains.com/issue/KT-87596) Kotlin/Native: port calls checker to LLVM pass
- [`KT-89313`](https://youtrack.jetbrains.com/issue/KT-89313) DNN in supertype leads to compiler crash
- [`KT-89416`](https://youtrack.jetbrains.com/issue/KT-89416) Native: top-level properties are not initialized when checked for initialization state from another class
- [`KT-89876`](https://youtrack.jetbrains.com/issue/KT-89876) K/N IC: "File … is not found in cache" after a file is renamed or moved to another module (stale weak dependency)
- [`KT-87856`](https://youtrack.jetbrains.com/issue/KT-87856) Investigate failures in scripting tests after a full switch to K2
- [`KT-87149`](https://youtrack.jetbrains.com/issue/KT-87149) Drop K1-dependent parts of the scripting compiler plugin
- [`KT-89769`](https://youtrack.jetbrains.com/issue/KT-89769) FIR2IR throws "IR class for <X> not found" when a class's supertype isn't exposed via `api`
- [`KT-89751`](https://youtrack.jetbrains.com/issue/KT-89751) K2 REPL: contracts of functions are not resolved inside REPL snippets
- [`KT-89790`](https://youtrack.jetbrains.com/issue/KT-89790) Improve error reporting around new compilation scheme
- [`KT-87904`](https://youtrack.jetbrains.com/issue/KT-87904) Incorrect $default method/constructor signatures when `@JvmInline` value class parameter type is absent from compilation classpath
- [`KT-83498`](https://youtrack.jetbrains.com/issue/KT-83498) Implement LT support in the K2 REPL
- [`KT-86093`](https://youtrack.jetbrains.com/issue/KT-86093) Context sensitive resolution does not apply if simple access is resolved to invisible qualifier
- [`KT-34051`](https://youtrack.jetbrains.com/issue/KT-34051) UnsupportedOperationException when using reified type inside lambda in inline function
- [`KT-89837`](https://youtrack.jetbrains.com/issue/KT-89837) Correctly evaluate `name` for declaration that are imported with different name
- [`KT-89584`](https://youtrack.jetbrains.com/issue/KT-89584) Build rich references for suspend conversion & SAM constructor references
- [`KT-89770`](https://youtrack.jetbrains.com/issue/KT-89770) KMP separate compilation reports EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE for external class extending JsAny
- [`KT-75366`](https://youtrack.jetbrains.com/issue/KT-75366) Provide proper keys for ScopeSession#getOrBuild usages
- [`KT-74304`](https://youtrack.jetbrains.com/issue/KT-74304) K2: WRAPPED_LHS_IN_ASSIGNMENT on annotations
- [`KT-77583`](https://youtrack.jetbrains.com/issue/KT-77583) K2: Support repl snippets with LightTree/KMP-based raw FIR building
- [`KT-89834`](https://youtrack.jetbrains.com/issue/KT-89834) Drop RawFirBuilderTotalKotlinTestCase.testPsiConsistency
- [`KT-86380`](https://youtrack.jetbrains.com/issue/KT-86380) New inline variables scheme: addition of this variable
- [`KT-87777`](https://youtrack.jetbrains.com/issue/KT-87777) K/N IC: "Undefined symbols for architecture" when an inherited open method becomes final  
- [`KT-81075`](https://youtrack.jetbrains.com/issue/KT-81075) False negative MISSING_DEPENDENCY_SUPERCLASS on unresolved supertype of SAM constructor
- [`KT-89168`](https://youtrack.jetbrains.com/issue/KT-89168) Reflection: support getDelegate() for companion block properties in interfaces
- [`KT-74793`](https://youtrack.jetbrains.com/issue/KT-74793) Parser: Property accessor of a non-local destructuring declaration leads to exceptions
- [`KT-83340`](https://youtrack.jetbrains.com/issue/KT-83340) Investigate need of `KtRegisteredDiagnosticFactoriesStorage` component in AA mode
- [`KT-89762`](https://youtrack.jetbrains.com/issue/KT-89762) KMP Separate Compilation: EXPECT_ACTUAL_IR_MISMATCH caused by accessing a java field in a single platform project
- [`KT-89635`](https://youtrack.jetbrains.com/issue/KT-89635) False "Return type mismatch" in interface delegation when the delegated function with an implicit return type is declared in another file
- [`KT-89341`](https://youtrack.jetbrains.com/issue/KT-89341) KMP separate compilation: false-positive EXPECT_ACTUAL_IR_MISMATCH on members of inner class with generics in outer class
- [`KT-89744`](https://youtrack.jetbrains.com/issue/KT-89744) Apostrophe in an escaped identifier corrupts ARGUMENT_TYPE_MISMATCH diagnostic rendering
- [`KT-89121`](https://youtrack.jetbrains.com/issue/KT-89121) [Native] `@BindClassToObjCName` adapters from a cached module never reach Kotlin_ObjCExport_sorted{Class,Protocol}Adapters
- [`KT-83419`](https://youtrack.jetbrains.com/issue/KT-83419) K2: CCE when `inline` lambda changes a `var` involved in an `implies` contract
- [`KT-89063`](https://youtrack.jetbrains.com/issue/KT-89063) JvmExposeBoxed allows to generate invalid Java method names
- [`KT-89068`](https://youtrack.jetbrains.com/issue/KT-89068) JvmExposeBoxed: secondary constructor of inline class is never exposed
- [`KT-89476`](https://youtrack.jetbrains.com/issue/KT-89476) Diagnostic for incorrect `@EagerInitialization` usage
- [`KT-89653`](https://youtrack.jetbrains.com/issue/KT-89653) Native: failing lldb tests with state checker enabled
- [`KT-89407`](https://youtrack.jetbrains.com/issue/KT-89407) When frontend reports diagnostic on null source, report as sourceless diagnostic
- [`KT-82900`](https://youtrack.jetbrains.com/issue/KT-82900) Language Feature EnhancedBridgesGeneration
- [`KT-20245`](https://youtrack.jetbrains.com/issue/KT-20245) NoSuchFieldError on usage of crossinline lambda in anonymous object inside another anonymous object in inline function
- [`KT-89641`](https://youtrack.jetbrains.com/issue/KT-89641) Verify JS export works for full value classes
- [`KT-80383`](https://youtrack.jetbrains.com/issue/KT-80383) Getter without a body is allowed on a property with an explicit backing field
- [`KT-89415`](https://youtrack.jetbrains.com/issue/KT-89415) Refactor logic around FirParser.LightTree/MultiplatformParsing and USE_LIGHT_TREE configuration flag
- [`KT-87569`](https://youtrack.jetbrains.com/issue/KT-87569) Support Valhalla for Kotlin Inline and Full Value Classes
- [`KT-87531`](https://youtrack.jetbrains.com/issue/KT-87531) AbstractMethodError on intersection override with `@JvmField` property when accessing property through interface
- [`KT-89642`](https://youtrack.jetbrains.com/issue/KT-89642) Disable feature typed equals in value classes for full value classes
- [`KT-81558`](https://youtrack.jetbrains.com/issue/KT-81558) Context-sensitive resolution cannot resolve an unqualified enum entry in Gradle `Property<T>`-like cases
- [`KT-83304`](https://youtrack.jetbrains.com/issue/KT-83304) Context sensitive resolution doesn't work on mutable collection operators (no explicit import from enum)
- [`KT-89504`](https://youtrack.jetbrains.com/issue/KT-89504) VerifyError: Bad type on operand stack with startCoroutine on smart-casted nullable suspend function type
- [`KT-84406`](https://youtrack.jetbrains.com/issue/KT-84406) Remove support for IR serialization in JVM
- [`KT-84562`](https://youtrack.jetbrains.com/issue/KT-84562) Get rid of usages of `@K1Deprecation`-annotated declarations in JVM backend
- [`KT-89325`](https://youtrack.jetbrains.com/issue/KT-89325) Java Direct: no supertypes on resolution due to reentrance
- [`KT-86095`](https://youtrack.jetbrains.com/issue/KT-86095) "VerifyError: Bad type on operand stack" when inline+crossinline function returns anonymous object overriding internal suspend fun with yield()
- [`KT-89430`](https://youtrack.jetbrains.com/issue/KT-89430) Don't store raw-fir generated tests in git
- [`KT-88081`](https://youtrack.jetbrains.com/issue/KT-88081) Normalize Double.NaN and Float.NaN after constant evaluation
- [`KT-89433`](https://youtrack.jetbrains.com/issue/KT-89433) Frontend: Metadata of abstract/sealed full value classes declares equals/hashCode/toString that the JVM backend does not generate
- [`KT-89533`](https://youtrack.jetbrains.com/issue/KT-89533) class KtValueArgument is not a subtype of class KtCollectionLiteralExpression for factory UNRESOLVED_COLLECTION_LITERAL
- [`KT-88589`](https://youtrack.jetbrains.com/issue/KT-88589) Full Value Classes: irrelevant SMARTCAST_IMPOSSIBLE for value class implemented in another module 
- [`KT-89206`](https://youtrack.jetbrains.com/issue/KT-89206) Inlining of TODO() as a default value leads to an internal error
- [`KT-89157`](https://youtrack.jetbrains.com/issue/KT-89157) Remove temporary useless casts introduced by KT-79212
- [`KT-73165`](https://youtrack.jetbrains.com/issue/KT-73165) "ANNOTATION_ARGUMENT_MUST_BE_CONST" caused by Enum name as a constant in Annotation
- [`KT-89466`](https://youtrack.jetbrains.com/issue/KT-89466) KMP Separate Compilation: NoSuchElementException in case of classpath substitution
- [`KT-79562`](https://youtrack.jetbrains.com/issue/KT-79562) NPE when passing non-lambda argument of nullable non-suspend function type into function that accepts nullable suspend function type
- [`KT-86282`](https://youtrack.jetbrains.com/issue/KT-86282) Implement FIR builder based on KMP parser
- [`KT-89391`](https://youtrack.jetbrains.com/issue/KT-89391) KMP separate compilation: false-positive `EXPECT_ACTUAL_IR_MISMATCH` on declarations from friend dependencies
- [`KT-89475`](https://youtrack.jetbrains.com/issue/KT-89475) Missed MISSING_DEPENDENCY_SUPERCLASS for parameters of value classes
- [`KT-89259`](https://youtrack.jetbrains.com/issue/KT-89259) Collection literals: enabling `+CollectionLiteralsBasedAnnotationResolution` causes `MISSING_DEPENDENCY_CLASS`
- [`KT-82959`](https://youtrack.jetbrains.com/issue/KT-82959) IllegalArgumentException: Unhandled intrinsic in ExpressionCodegen for generated top-level declarations
- [`KT-85104`](https://youtrack.jetbrains.com/issue/KT-85104) JVM compilation error when having TODO() as a parameter default value of an inline function when adding a call to it
- [`KT-85168`](https://youtrack.jetbrains.com/issue/KT-85168) Generate static initializers as proper IR functions
- [`KT-89457`](https://youtrack.jetbrains.com/issue/KT-89457) companion block on `@JvmRecord` class cannot be compiled
- [`KT-89453`](https://youtrack.jetbrains.com/issue/KT-89453) `INVALID_NON_OPTIONAL_PARAMETER_POSITION` ignores `expect`-chain defaults
- [`KT-70118`](https://youtrack.jetbrains.com/issue/KT-70118) [KLIB Resolve] Stop using depends= property of klib manifest when building caches in Native
- [`KT-86888`](https://youtrack.jetbrains.com/issue/KT-86888) Update PersistentMultimap to follow persistent collection naming conventions
- [`KT-84001`](https://youtrack.jetbrains.com/issue/KT-84001) KIAEWA: Exception in type checkers when classpath contains newer Kotlin-compiled dependencies
- [`KT-88520`](https://youtrack.jetbrains.com/issue/KT-88520) Equality applicability checker should account for custom equals in full value classes
- [`KT-89374`](https://youtrack.jetbrains.com/issue/KT-89374) Companion blocks: false positive PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS for property inside companion block of value class
- [`KT-89112`](https://youtrack.jetbrains.com/issue/KT-89112) Move transformation `arrayOf(...)` -> `[...]` in annotations to constant evaluator
- [`KT-74051`](https://youtrack.jetbrains.com/issue/KT-74051) Add a Continuation for tail-call suspend functions
- [`KT-81874`](https://youtrack.jetbrains.com/issue/KT-81874) NPE related to data race during Application disposal
- [`KT-63741`](https://youtrack.jetbrains.com/issue/KT-63741) K2: fix visibility inference with overridden + inherited member
- [`KT-46419`](https://youtrack.jetbrains.com/issue/KT-46419) FIR: Investigate how integer-literal resolution works
- [`KT-6843`](https://youtrack.jetbrains.com/issue/KT-6843) NPE in REPL
- [`KT-10001`](https://youtrack.jetbrains.com/issue/KT-10001) False unnecessary non-null assertion on a pair element

### IR. Tree

- [`KT-89203`](https://youtrack.jetbrains.com/issue/KT-89203) IR validation: New checker to prohibit use of legacy Ir*Reference nodes on 1st compilation stage
- [`KT-87442`](https://youtrack.jetbrains.com/issue/KT-87442) [IR] Introduce 1st stage warning on failed tailrec optimization
- [`KT-87836`](https://youtrack.jetbrains.com/issue/KT-87836) Make sure that type, value and kind in IrConst are coherent
- [`KT-88082`](https://youtrack.jetbrains.com/issue/KT-88082) Do not allow constant folding if the resulting expression is Double.NaN and Float.NaN
- [`KT-87954`](https://youtrack.jetbrains.com/issue/KT-87954) Use `kotlinLibrary` from IrModuleFragment instead of a library from ModuleDescriptor

### JVM. Reflection

#### New Features

- [`KT-84275`](https://youtrack.jetbrains.com/issue/KT-84275) Support companion blocks & extensions in reflection
- [`KT-42082`](https://youtrack.jetbrains.com/issue/KT-42082) Ability to retrieve a container/owner class of the KProperty/KFunction
- [`KT-87955`](https://youtrack.jetbrains.com/issue/KT-87955) Reflection support for contextualized callable references

#### Fixes

- [`KT-89918`](https://youtrack.jetbrains.com/issue/KT-89918) Reflection: IllegalStateException while processing the times overload during Duration property lookup
- [`KT-80710`](https://youtrack.jetbrains.com/issue/KT-80710) Reflection: change KCallable representation to avoid dependency on K1
- [`KT-89280`](https://youtrack.jetbrains.com/issue/KT-89280) NoSuchElementException in argumentsMakeSenseOnlyForMutableContainer for Java non-static inner class of generic outer class
- [`KT-89956`](https://youtrack.jetbrains.com/issue/KT-89956) KClass.isValue of mapped builtins like kotlin.Number ignores that their Java class is a value class
- [`KT-89952`](https://youtrack.jetbrains.com/issue/KT-89952) Reflection fails with NoSuchElementException when an inline class wraps a full value class
- [`KT-89949`](https://youtrack.jetbrains.com/issue/KT-89949) Reflection treats full value classes and Java value classes as inline classes when calling functions
- [`KT-88503`](https://youtrack.jetbrains.com/issue/KT-88503) KType.isMarkedNullable always returns false for `@Nullable`-annotated Java parameters in K2 reflection implementation
- [`KT-85735`](https://youtrack.jetbrains.com/issue/KT-85735) Reflection: Java wildcard type based on Kotlin type with declaration-site variance is loaded differently in new implementation
- [`KT-89732`](https://youtrack.jetbrains.com/issue/KT-89732) Reflection: IllegalStateException in CovariantOverrideComparator for Java interface inheriting both raw and generic method with same erasure
- [`KT-89019`](https://youtrack.jetbrains.com/issue/KT-89019) Reflection: Java Enum.valueOf(value) has lost its parameter name
- [`KT-89419`](https://youtrack.jetbrains.com/issue/KT-89419) Reflection: parameter annotations on bound constructor reference are loaded incorrectly
- [`KT-89000`](https://youtrack.jetbrains.com/issue/KT-89000) KotlinReflectionInternalError on Array  size.getter.annotations

### JavaScript

#### New Features

- [`KT-87029`](https://youtrack.jetbrains.com/issue/KT-87029) ⁠[Effect Analysis] Leverage effect analysis results in DCE to delete unused properties' initializers
- [`KT-87027`](https://youtrack.jetbrains.com/issue/KT-87027) [Effect Analysis] Propagate effects across the call graph based on function body calls
- [`KT-87026`](https://youtrack.jetbrains.com/issue/KT-87026) ⁠[Effect Analysis] Introduce a lowering for analyzing mutation of inputs and outer state
- [`KT-87024`](https://youtrack.jetbrains.com/issue/KT-87024) [Effect Analysis] Mark up kotlin-stdlib declarations with effect annotations
- [`KT-88024`](https://youtrack.jetbrains.com/issue/KT-88024) KJS: Allow exporting interfaces nested in other interfaces to JavaScript

#### Performance Improvements

- [`KT-89682`](https://youtrack.jetbrains.com/issue/KT-89682) K/JS & K/Wasm: Initialize trivial objects eagerly even in dev mode

#### Fixes

- [`KT-89984`](https://youtrack.jetbrains.com/issue/KT-89984) JS: nullable value class over a nullable type parameter holding null is treated as null
- [`KT-89978`](https://youtrack.jetbrains.com/issue/KT-89978) JS and Native box single-field full value classes and call equals() when comparing them with ==
- [`KT-89920`](https://youtrack.jetbrains.com/issue/KT-89920) K/JS: Adapted `::ulongArrayOf` reference does not unbox ULong arguments, producing a corrupted ULongArray
- [`KT-89919`](https://youtrack.jetbrains.com/issue/KT-89919) KJS: "Unhandled JavaScript exception: array.slice is not a function" when using non-adapted ::uintArrayOf
- [`KT-89449`](https://youtrack.jetbrains.com/issue/KT-89449) Kotlin/JS: Member names no longer minified in production
- [`KT-89290`](https://youtrack.jetbrains.com/issue/KT-89290) Web: companion block members are not initialized when observed from a nested class
- [`KT-49422`](https://youtrack.jetbrains.com/issue/KT-49422) Fix test about support of DefinitelyNotNull type in backends
- [`KT-64821`](https://youtrack.jetbrains.com/issue/KT-64821) KJS: "TypeError: * is not a function" with class which extends suspend lambda
- [`KT-83337`](https://youtrack.jetbrains.com/issue/KT-83337) KJS: Difference in behavior on nested class initialization
- [`KT-88343`](https://youtrack.jetbrains.com/issue/KT-88343) KJS: Report `JS_MODULE_PROHIBITED_ON_VAR` when `@file`:JsModule` is present
- [`KT-89591`](https://youtrack.jetbrains.com/issue/KT-89591) JS plain objects: generated top-level invoke/copy leak visibility for internal interfaces
- [`KT-85540`](https://youtrack.jetbrains.com/issue/KT-85540) Drop longArrayClass definitions from boxedLong.kt and longAsBigInt.kt
- [`KT-72720`](https://youtrack.jetbrains.com/issue/KT-72720) Setp up a nightly test suite for running all JS compiler tests with -Xir-generate-inline-anonymous-functions
- [`KT-89372`](https://youtrack.jetbrains.com/issue/KT-89372) KJS: Run the second DCE pass after the optimization pipeline
- [`KT-85047`](https://youtrack.jetbrains.com/issue/KT-85047) Kotlin/JS: `@JsStatic` on suspend fun of class companion generates incorrect d.ts

### Klibs

- [`KT-86840`](https://youtrack.jetbrains.com/issue/KT-86840) KLIBs: Implement a component inside the compiler to compute DAG of dependencies for a given set of libraries
- [`KT-89276`](https://youtrack.jetbrains.com/issue/KT-89276) KLIBs: Check remaining usages of `KlibMetadataProtoBuf.Header` in compiler's source code
- [`KT-89921`](https://youtrack.jetbrains.com/issue/KT-89921) klibs: failure with two identical static inline functions in the same file in different classes
- [`KT-89498`](https://youtrack.jetbrains.com/issue/KT-89498) IR linker performance: IrFileDeserializerImpl.getAllMatchingSignatures()
- [`KT-89825`](https://youtrack.jetbrains.com/issue/KT-89825) K/N: "IrClassSymbolImpl is already bound" when two cinterop libraries in the same package declare the same C struct
- [`KT-87487`](https://youtrack.jetbrains.com/issue/KT-87487) KLIBs: Don't serialize empty metadata package fragments
- [`KT-89482`](https://youtrack.jetbrains.com/issue/KT-89482) [KLIB] Introduce special signature indices in regular & C-interop Native KLIBs
- [`KT-81624`](https://youtrack.jetbrains.com/issue/KT-81624) [KLIB Resolve] Kotlin/Native: -no-default-libs may lead to the final binary depending on a lot of system frameworks
- [`KT-61096`](https://youtrack.jetbrains.com/issue/KT-61096) [KLIB Resolve] Drop KLIB resolve inside the Kotlin/Native compiler
- [`KT-87387`](https://youtrack.jetbrains.com/issue/KT-87387) Fix K1 API usage in `:kotlin-util-klib-metadata`

### Language Design

- [`KT-12719`](https://youtrack.jetbrains.com/issue/KT-12719) Must-use return values: warn about unused return values

### Libraries

#### New Features

- [`KT-24908`](https://youtrack.jetbrains.com/issue/KT-24908) CodePoint value class
- [`KT-83426`](https://youtrack.jetbrains.com/issue/KT-83426) Add Tau=2*Pi to the stdlib
- [`KT-89662`](https://youtrack.jetbrains.com/issue/KT-89662) Stabilize Uuid.Companion.generateV{4,7}
- [`KT-89629`](https://youtrack.jetbrains.com/issue/KT-89629) Make some of the Timer.schedule overloads ignorable
- [`KT-87021`](https://youtrack.jetbrains.com/issue/KT-87021) ⁠[Effect Analysis] Introduce a new annotation for marking stdlib functions  
- [`KT-89332`](https://youtrack.jetbrains.com/issue/KT-89332) kotlin-metadata: stabilize RVC-related API

#### Performance Improvements

- [`KT-89899`](https://youtrack.jetbrains.com/issue/KT-89899) K/Wasm: Optimize Double.ulp
- [`KT-71987`](https://youtrack.jetbrains.com/issue/KT-71987) Float.ulp can be optimized for Native

#### Fixes

- [`KT-48530`](https://youtrack.jetbrains.com/issue/KT-48530) Asymmetric equality for progressions and ranges
- [`KT-89174`](https://youtrack.jetbrains.com/issue/KT-89174) Incorrect exception type documented in random function for ranges
- [`KT-56363`](https://youtrack.jetbrains.com/issue/KT-56363) Kotlin/Native: NativeRandom does not behave well with multiple threads
- [`KT-88241`](https://youtrack.jetbrains.com/issue/KT-88241) kotlin-metadata-jvm: Consider renaming isStatic to isCompanionBlockMember
- [`KT-89560`](https://youtrack.jetbrains.com/issue/KT-89560) Make kotlin.concurrent.thread's return value ignorable
- [`KT-85126`](https://youtrack.jetbrains.com/issue/KT-85126) Increase StringBuilder.append(obj: Any?) deprecation level to ERROR
- [`KT-89177`](https://youtrack.jetbrains.com/issue/KT-89177) Aggregate functions accepting a destination collection should be annotated with IgnorableReturnValue
- [`KT-82882`](https://youtrack.jetbrains.com/issue/KT-82882) Migrate `kotlinx-benchmarks` Gradle plugin from KLIB resolver to `KlibLoader` API
- [`KT-64377`](https://youtrack.jetbrains.com/issue/KT-64377) Update Dokka Gradle integration tests build configuration
- [`KT-64200`](https://youtrack.jetbrains.com/issue/KT-64200) Dokka Integration tests refactoring
- [`KT-64916`](https://youtrack.jetbrains.com/issue/KT-64916) Update Dokka build config to use JB Gradle Enterprise

### Native

- [`KT-85131`](https://youtrack.jetbrains.com/issue/KT-85131) Native: freeze in `LLVMGetTargetFromTriple` during a parallel test run
- [`KT-89541`](https://youtrack.jetbrains.com/issue/KT-89541) K/N: Enabling `-Xcompanion-blocks` in stdlib breaks some callable reference tests with OPT, NO_CACHE
- [`KT-89355`](https://youtrack.jetbrains.com/issue/KT-89355) Kotlin/Native: some benchmarks are missing
- [`KT-87872`](https://youtrack.jetbrains.com/issue/KT-87872) Native: a few tests in `ObjCToKotlinSteppingInLLDBTest` fail with Xcode 27
- [`KT-87869`](https://youtrack.jetbrains.com/issue/KT-87869) Native: Xcode 27
- [`KT-89637`](https://youtrack.jetbrains.com/issue/KT-89637) K/N: Linking of linux (x64/arm64) binaries with cinterop on Windows is failing
- [`KT-84955`](https://youtrack.jetbrains.com/issue/KT-84955) Deprecate apple x86_64 targets as Error
- [`KT-89258`](https://youtrack.jetbrains.com/issue/KT-89258) Kotlin/Native: mute volatile benchmarks

### Native. C Export

- [`KT-89632`](https://youtrack.jetbrains.com/issue/KT-89632) [K/N][CExport] Header declaration order depends on -Xinclude order

### Native. C and ObjC Import

- [`KT-89463`](https://youtrack.jetbrains.com/issue/KT-89463) Native: 'Unable to compile C bridge' when implementing UIScreenshotServiceDelegateProtocol
- [`KT-89401`](https://youtrack.jetbrains.com/issue/KT-89401) K/N: NPE in IrLazilyBoundAnnotationImpl when a cinterop klib references an annotation from a regular klib

### Native. ObjC Export

- [`KT-89981`](https://youtrack.jetbrains.com/issue/KT-89981) Building an Objective-C framework fails with single-field full value classes in the API
- [`KT-70626`](https://youtrack.jetbrains.com/issue/KT-70626) ObjCExport: init constructors order in Throwable

### Native. Swift Export

#### Fixes

- [`KT-88251`](https://youtrack.jetbrains.com/issue/KT-88251) Swift Export cross-language inheritance example cannot instantiate KotlinBase
- [`KT-89647`](https://youtrack.jetbrains.com/issue/KT-89647) Native: failing test with state checker on
- [`KT-90024`](https://youtrack.jetbrains.com/issue/KT-90024) [Swift Export] improve compilation performance of Swift Export
- [`KT-89702`](https://youtrack.jetbrains.com/issue/KT-89702) Swift Export: reverse bridge calls a member named init without backticks 
- [`KT-89728`](https://youtrack.jetbrains.com/issue/KT-89728) Swift Export: Prohibit exporting objc-imported protocol conformances on kontlin types
- [`KT-89701`](https://youtrack.jetbrains.com/issue/KT-89701) Swift Export: sealed-type switch references a subtype nested in an internal class 
- [`KT-89842`](https://youtrack.jetbrains.com/issue/KT-89842) [Swift Export] extensions of Unavailable type results on conflicts
- [`KT-89839`](https://youtrack.jetbrains.com/issue/KT-89839) [Swift Export] there could be more than one incoming cinterop
- [`KT-89159`](https://youtrack.jetbrains.com/issue/KT-89159) [Swift export] val to var property override is unreadable: "ambiguous use" on the subclass, base value returned via the superclass
- [`KT-88256`](https://youtrack.jetbrains.com/issue/KT-88256) Swift Export: constructor of a Kotlin subclass with renamed params causes 'error: argument labels for initializer do not match'
- [`KT-88259`](https://youtrack.jetbrains.com/issue/KT-88259) Swift Export [Inheritance]: toString/hashCode/equals cause EXC_BAD_ACCESS on Swift subclass of an exported Kotlin class 
- [`KT-88277`](https://youtrack.jetbrains.com/issue/KT-88277) Swift Export [Inheritance]: function override with lambda parameter is ignored

### Tools. Build Tools API

- [`KT-89804`](https://youtrack.jetbrains.com/issue/KT-89804) Protect build-tools-api-jps compatibility with an API dump
- [`KT-89615`](https://youtrack.jetbrains.com/issue/KT-89615) Support daemon mode in the JPS-only BTA API surface for client-managed incremental JVM compilation
- [`KT-89673`](https://youtrack.jetbrains.com/issue/KT-89673) Use Path instead of String for file paths in the JPS BTA tracker API
- [`KT-89513`](https://youtrack.jetbrains.com/issue/KT-89513) Introduce the JPS-only BTA API surface for client-managed incremental JVM compilation
- [`KT-84229`](https://youtrack.jetbrains.com/issue/KT-84229) Build reports missing memory metrics and incorrect in-process strategy indicators with BTA

### Tools. CLI

- [`KT-88174`](https://youtrack.jetbrains.com/issue/KT-88174) Show error when running the compiler on JDK < 17
- [`KT-89800`](https://youtrack.jetbrains.com/issue/KT-89800) KMP Separate Compilation: Single-JVM-target commonTest cannot reference jvmMain with separate compilation
- [`KT-89727`](https://youtrack.jetbrains.com/issue/KT-89727) `EXPECT_ACTUAL_IR_MISMATCH` when using `separateCompilation=true`
- [`KT-88475`](https://youtrack.jetbrains.com/issue/KT-88475) IncrementalCompilationComponents instances are not being called anymore
- [`KT-89211`](https://youtrack.jetbrains.com/issue/KT-89211) Show warning when running the compiler on JDK < 17
- [`KT-89356`](https://youtrack.jetbrains.com/issue/KT-89356) Check that CLI arguments configuring already-deprecated/removed language features are deprecated/removed themselves
- [`KT-73967`](https://youtrack.jetbrains.com/issue/KT-73967) JDK 25: "IllegalArgumentException: 25-ea" with EA builds

### Tools. Compiler Plugin API

- [`KT-68878`](https://youtrack.jetbrains.com/issue/KT-68878) AA: LLFirIdePredicateBasedProvider can return declarations from dependant modules

### Tools. Compiler Plugins

#### New Features

- [`KT-76529`](https://youtrack.jetbrains.com/issue/KT-76529) Support lombok.fieldDefaults.defaultPrivate in Lombok plugin

#### Fixes

- [`KT-89189`](https://youtrack.jetbrains.com/issue/KT-89189) `@EqualsAndHashCode` on a Kotlin class must generate canEqual
- [`KT-83329`](https://youtrack.jetbrains.com/issue/KT-83329) Lombok. The case where the default builder is hidden as an internal and an auxiliary one is created doesn't work from kotlin
- [`KT-89973`](https://youtrack.jetbrains.com/issue/KT-89973) all-open makes value classes open, causing "Value class cannot be open" errors
- [`KT-89972`](https://youtrack.jetbrains.com/issue/KT-89972) no-arg reports a redundant superclass error on inner classes it already rejects
- [`KT-89971`](https://youtrack.jetbrains.com/issue/KT-89971) no-arg plugin silently generates zero-argument constructors for value classes
- [`KT-89970`](https://youtrack.jetbrains.com/issue/KT-89970) Parcelize assigns fields of a value class after super() in Valhalla mode
- [`KT-79199`](https://youtrack.jetbrains.com/issue/KT-79199) Add K/N FIR dump tests to atomicfu compiler plugin
- [`KT-89117`](https://youtrack.jetbrains.com/issue/KT-89117) Lombok: NoSuchMethodError when using Java class annotated with Builder and Singular in Kotlin and with lombok.singular.useGuava=true
- [`KT-89045`](https://youtrack.jetbrains.com/issue/KT-89045) Lombok: some java.util.* collections supported in Java as `@Singular` targets are not supported in Kotlin
- [`KT-89084`](https://youtrack.jetbrains.com/issue/KT-89084) Lombok: lombok.singular.useGuava=true is not working for Kotlin collections
- [`KT-88904`](https://youtrack.jetbrains.com/issue/KT-88904) Lombok: Builder annotation generates invalid IR for generics with type-parametrized upper bound
- [`KT-83273`](https://youtrack.jetbrains.com/issue/KT-83273) Lombok. The inner builder class is unavailable in kotlin
- [`KT-89753`](https://youtrack.jetbrains.com/issue/KT-89753) Lombok: StackOverflowError when a Java class uses its own nested annotation on a member
- [`KT-88941`](https://youtrack.jetbrains.com/issue/KT-88941) Lombok: KotlinIllegalArgumentExceptionWithAttachments in the IDE when `@NoArgsConstructor` or `@Builder` is applied to a Kotlin class
- [`KT-89185`](https://youtrack.jetbrains.com/issue/KT-89185) Lombok: toBuilder() silently drops a builder field with no property to read, where Lombok reports an error
- [`KT-89218`](https://youtrack.jetbrains.com/issue/KT-89218) Lombok: no diagnostic when `@Builder`.Default or `@Singular` is applied to a body-declared property of a Kotlin class
- [`KT-88848`](https://youtrack.jetbrains.com/issue/KT-88848) Lombok: AssertionError when applying Builder annotation to a local class
- [`KT-89024`](https://youtrack.jetbrains.com/issue/KT-89024) Lombok: ClassFormatError when loading class annotated with `@Builder`(builderMethodName="")
- [`KT-88852`](https://youtrack.jetbrains.com/issue/KT-88852) Lombok: RuntimeException if Builder is applied to an inner class
- [`KT-89027`](https://youtrack.jetbrains.com/issue/KT-89027) Lombok: `@Builder`(access = AccessLevel.PRIVATE) in Kotlin produces unusable builder class
- [`KT-89040`](https://youtrack.jetbrains.com/issue/KT-89040) Lombok: no diagnostic and no codegen when `@Builder` applied to top-level functions
- [`KT-88891`](https://youtrack.jetbrains.com/issue/KT-88891) Lombok: Builder annotation generates setters for properties declared in a class body that do not work
- [`KT-88814`](https://youtrack.jetbrains.com/issue/KT-88814) Lombok: InstantiationError when using Builder with abstract or sealed class
- [`KT-71791`](https://youtrack.jetbrains.com/issue/KT-71791) [AFU plugin] Improve illegal use cases handling 
- [`KT-89412`](https://youtrack.jetbrains.com/issue/KT-89412) Add kotlin-atomicfu-compiler-plugin to .zip compiler distribution
- [`KT-88276`](https://youtrack.jetbrains.com/issue/KT-88276) REDECLARATION conflicts with the existing nested 'Companion' 
- [`KT-86652`](https://youtrack.jetbrains.com/issue/KT-86652) Lombok: introduce NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS for `@NoArgsConstructor`
- [`KT-86915`](https://youtrack.jetbrains.com/issue/KT-86915) Support for multiple annotations on the same class
- [`KT-89133`](https://youtrack.jetbrains.com/issue/KT-89133) Lombok: no navigation from a generated `@Builder` member to the property it is generated from
- [`KT-89152`](https://youtrack.jetbrains.com/issue/KT-89152) Lombok: use kotlin.test assertions in the plugin's test data instead of the hand-rolled assertEquals
- [`KT-89162`](https://youtrack.jetbrains.com/issue/KT-89162) Lombok: `@Builder` on a method generates nothing in the IDE because the annotation is read off unresolved FIR
- [`KT-89169`](https://youtrack.jetbrains.com/issue/KT-89169) Lombok: constructor annotations on a Java inner class produce an uncallable constructor
- [`KT-88707`](https://youtrack.jetbrains.com/issue/KT-88707) Lombok: FileAnalysisException when creating an instance of an inner class with NoArgsConstructor
- [`KT-88659`](https://youtrack.jetbrains.com/issue/KT-88659) Lombok: RuntimeException when NoArgsContructor/Builder are applied to a class with non-property parameter in a primary constructor
- [`KT-88729`](https://youtrack.jetbrains.com/issue/KT-88729) Lombok: flagUsage does not report erros/warnings for constructor-related annotations used in Kotlin

### Tools. Compiler plugins. Compose

- [`KT-89681`](https://youtrack.jetbrains.com/issue/KT-89681) [WEB] Expression uses unlinked type parameter symbol
- [`KT-89969`](https://youtrack.jetbrains.com/issue/KT-89969) Compose ignores the stability configuration file for value classes
- [`KT-89968`](https://youtrack.jetbrains.com/issue/KT-89968) Compose infers abstract and sealed value classes as unstable instead of uncertain
- [`KT-89958`](https://youtrack.jetbrains.com/issue/KT-89958) Compose ignores type arguments of generic value classes when inferring stability
- [`KT-89961`](https://youtrack.jetbrains.com/issue/KT-89961) Compose crashes with StackOverflowError on a recursive full value class
- [`KT-89957`](https://youtrack.jetbrains.com/issue/KT-89957) Compose infers full value classes with uncertain properties as stable

### Tools. Compiler plugins. Serialization

- [`KT-81042`](https://youtrack.jetbrains.com/issue/KT-81042) Warn when Proto* annotations are applied to properties of inappropriate type
- [`KT-69488`](https://youtrack.jetbrains.com/issue/KT-69488) Serialization: plugin should print a diagnostic or a warning when misusing `@Serializable`(with=...)
- [`KT-55738`](https://youtrack.jetbrains.com/issue/KT-55738) Report a diagnostic when `@Serializable` class already has a serializer() function in companion with signature that matches plugin-generated
- [`KT-66462`](https://youtrack.jetbrains.com/issue/KT-66462) Report a warning when the serializable class has a private companion
- [`KT-89275`](https://youtrack.jetbrains.com/issue/KT-89275) Kotlin/Wasm: Invalid Wasm is emitted for a `@Serializable` generic class with a class upper bound
- [`KT-89643`](https://youtrack.jetbrains.com/issue/KT-89643) Fix Protobuf tag reuse for full value classes
- [`KT-59088`](https://youtrack.jetbrains.com/issue/KT-59088) KJS / Serialization: Report a compiler error if `@Serializable` class contains a property with dynamic type
- [`KT-69067`](https://youtrack.jetbrains.com/issue/KT-69067) K2 + Serialization: provide a warning when plugin-related annotations are typealiased
- [`KT-73207`](https://youtrack.jetbrains.com/issue/KT-73207) Report a compiler error when `@Serializable`(with) is applied and number of generic parameters does not match
- [`KT-71680`](https://youtrack.jetbrains.com/issue/KT-71680) Report a warning when `@Serializable` public class has private serializer

### Tools. Daemon

- [`KT-83117`](https://youtrack.jetbrains.com/issue/KT-83117) Daemon mode: Clear jar caches at the end of the build

### Tools. Gradle

#### New Features

- [`KT-81782`](https://youtrack.jetbrains.com/issue/KT-81782) Enable CRI by default
- [`KT-74808`](https://youtrack.jetbrains.com/issue/KT-74808) Add Gradle DSL option for enabling unused return value checker
- [`KT-88601`](https://youtrack.jetbrains.com/issue/KT-88601) Warn user on non-publishable targets when Kotlin Archive is enabled
- [`KT-88529`](https://youtrack.jetbrains.com/issue/KT-88529) Support compatibility mode with direct dependencies on platform components in KGP

#### Fixes

- [`KT-90111`](https://youtrack.jetbrains.com/issue/KT-90111) Add missing generated files to 2.5.0 branch
- [`KT-88594`](https://youtrack.jetbrains.com/issue/KT-88594) Compatibility with Gradle 9.8 release
- [`KT-87910`](https://youtrack.jetbrains.com/issue/KT-87910) Implement Kotlin Archive (.kar) publication format
- [`KT-87987`](https://youtrack.jetbrains.com/issue/KT-87987) Consume Swift Export metadata for published external dependencies
- [`KT-87990`](https://youtrack.jetbrains.com/issue/KT-87990) Add consumer dependency overrides to Swift Export Xcode integration
- [`KT-89323`](https://youtrack.jetbrains.com/issue/KT-89323) Consume Swift Export metadata for project dependencies
- [`KT-85434`](https://youtrack.jetbrains.com/issue/KT-85434) Gradle: drop support for non-BTA JVM compiler execution mode
- [`KT-84114`](https://youtrack.jetbrains.com/issue/KT-84114) Bump the minimal supported Gradle version to 8.14.x
- [`KT-89340`](https://youtrack.jetbrains.com/issue/KT-89340) Adopt sources.jars publication to Kotlin Archive
- [`KT-87558`](https://youtrack.jetbrains.com/issue/KT-87558) Gradle: Remove property for disabling usage of non-packed klibs
- [`KT-88785`](https://youtrack.jetbrains.com/issue/KT-88785) Gradle: Remove kotlin.internal.classpathSnapshot.parseInlinedLocalClasses
- [`KT-88619`](https://youtrack.jetbrains.com/issue/KT-88619) Bump minimal supported AGP version to 8.8.2
- [`KT-88609`](https://youtrack.jetbrains.com/issue/KT-88609) Cleanup declarative Gradle prototype from ecosystem plugin
- [`KT-88583`](https://youtrack.jetbrains.com/issue/KT-88583) Kapt may unexpectedly load conflicting dependencies from the Gradle runtime classpath
- [`KT-73968`](https://youtrack.jetbrains.com/issue/KT-73968) KotlinDependencyManagement tries to mutate configuration after it was resolved

### Tools. Gradle. JS

#### New Features

- [`KT-88882`](https://youtrack.jetbrains.com/issue/KT-88882) Reimplement Node.js provisioning in KGP
- [`KT-88868`](https://youtrack.jetbrains.com/issue/KT-88868) Rework declaration and collection of npm dependencies in KGP
- [`KT-87087`](https://youtrack.jetbrains.com/issue/KT-87087) Integrate Analysis API based TypeScript generation into the KMP Gradle plugin as a separate task
- [`KT-89230`](https://youtrack.jetbrains.com/issue/KT-89230) Expose allBrowserTestRunners as gradle named domain collection
- [`KT-88856`](https://youtrack.jetbrains.com/issue/KT-88856) KGP/JS/Playwright: Provide Gradle test filtering arguments in KotlinJsTestsLocation

#### Fixes

- [`KT-89605`](https://youtrack.jetbrains.com/issue/KT-89605) Use nodeJsToolchain in all tasks for nodeExecutable
- [`KT-88431`](https://youtrack.jetbrains.com/issue/KT-88431) Playwright JS tests don't run on Linux/ARM
- [`KT-88592`](https://youtrack.jetbrains.com/issue/KT-88592) Reporter option 'alsoWithHtml' has no effect. Because custom reporterOptions.Base was provided. In node-js tests
- [`KT-85440`](https://youtrack.jetbrains.com/issue/KT-85440) Remove kotlin-js plugin
- [`KT-89734`](https://youtrack.jetbrains.com/issue/KT-89734) Update JS tooling dependencies
- [`KT-86683`](https://youtrack.jetbrains.com/issue/KT-86683) Use npm-installed mocha.js instead HTTP loaded variant in  JS Browser test pipeline
- [`KT-89508`](https://youtrack.jetbrains.com/issue/KT-89508) JS/Wasm: Remove deprecated properties in NodeJsRootExtension
- [`KT-89404`](https://youtrack.jetbrains.com/issue/KT-89404) wasmJs browser test returns timeout and cant load mocha.js and mocha.css because they are missing in the ems bundle

### Tools. Gradle. Multiplatform

- [`KT-89563`](https://youtrack.jetbrains.com/issue/KT-89563) KGP doesn't work with the new xcproj Xcode project format
- [`KT-84736`](https://youtrack.jetbrains.com/issue/KT-84736) SPM import: the package name for the included build is just an underscore
- [`KT-89413`](https://youtrack.jetbrains.com/issue/KT-89413) Multiplatform: jvmTest reports omit JUnit TestReporter.publishEntry entries on Gradle 9.4+
- [`KT-89900`](https://youtrack.jetbrains.com/issue/KT-89900) Disable partial resolution diagnostic in large projects
- [`KT-86666`](https://youtrack.jetbrains.com/issue/KT-86666) Metadata (aka e.g. commonMain) unique_name does not include group prefix
- [`KT-87513`](https://youtrack.jetbrains.com/issue/KT-87513) Error deprecation for watchosArm32 target
- [`KT-89878`](https://youtrack.jetbrains.com/issue/KT-89878) Raise deprecation to error for 'withJava()' API
- [`KT-86611`](https://youtrack.jetbrains.com/issue/KT-86611) Deprecate hidden kotlin.mpp.freeCompilerArgsForSourceSet property

### Tools. Gradle. Native

- [`KT-89265`](https://youtrack.jetbrains.com/issue/KT-89265) K/N: Gradle 9.8 reports "Using the enum type KotlinNativeBundleArtifactsTypes as an attribute value type has been deprecated"
- [`KT-89480`](https://youtrack.jetbrains.com/issue/KT-89480) Changing swift export visibility doesn't affect incremental builds
- [`KT-89622`](https://youtrack.jetbrains.com/issue/KT-89622) cleanSwiftImportFingerprintArtifacts fails to parse a fingerprint left by 2.4.20 after upgrading to 2.5.0-Beta1

### Tools. Gradle. Swift Export

#### New Features

- [`KT-78747`](https://youtrack.jetbrains.com/issue/KT-78747) Swift export for SPM
- [`KT-89803`](https://youtrack.jetbrains.com/issue/KT-89803) Swift package generation for Swift Export
- [`KT-85687`](https://youtrack.jetbrains.com/issue/KT-85687) [Swift Export - Gradle] Add exposed and hidden dependency visibility overrides

#### Fixes

- [`KT-90365`](https://youtrack.jetbrains.com/issue/KT-90365) Swift Export: rename Gradle properties to follow the naming conventions
- [`KT-90020`](https://youtrack.jetbrains.com/issue/KT-90020) Swift Export: OutOfMemoryError in SwiftExportAction because of default 512 MB worker heap
- [`KT-90132`](https://youtrack.jetbrains.com/issue/KT-90132) Swift Export: Swift package export fails when Apple targets export different modules
- [`KT-89393`](https://youtrack.jetbrains.com/issue/KT-89393) Report SwiftExportUnsupportedMetadataSchemaVersion when processing metadata from KotlinProjectSharedDataProvider
- [`KT-89495`](https://youtrack.jetbrains.com/issue/KT-89495) Changes in published metadata not picked up by Swift Export task
- [`KT-89805`](https://youtrack.jetbrains.com/issue/KT-89805) Wire settings from the new Swift Export DSL into the Swift Export run
- [`KT-89720`](https://youtrack.jetbrains.com/issue/KT-89720) Add Swift Export metadata artifact to .kar archive
- [`KT-89812`](https://youtrack.jetbrains.com/issue/KT-89812) Run Swift Export once per target and declare the outputs of its tasks
- [`KT-89680`](https://youtrack.jetbrains.com/issue/KT-89680) [Swift Export] Types from a transitive spm dependency are not exported
- [`KT-89354`](https://youtrack.jetbrains.com/issue/KT-89354) embedSwiftExportForXcode can never be a no-op
- [`KT-89379`](https://youtrack.jetbrains.com/issue/KT-89379) Remove the ExperimentalExportDsl opt-in annotation
- [`KT-89289`](https://youtrack.jetbrains.com/issue/KT-89289) Move SwiftExportConfigurationCompat into the internal package
- [`KT-88987`](https://youtrack.jetbrains.com/issue/KT-88987) Add an example to the documentation for the export.swift.rootPackage DSL property

### Tools. Gradle. Wasm

- [`KT-89269`](https://youtrack.jetbrains.com/issue/KT-89269) KGP/JS/Playwright: "Failed to load Kotlin WASM JS tests entry point TypeError"
- [`KT-87136`](https://youtrack.jetbrains.com/issue/KT-87136) K/Wasm: Add possibility to have a distribution without webpack
- [`KT-89832`](https://youtrack.jetbrains.com/issue/KT-89832) K/Wasm: deprecate node for wasmWasi
- [`KT-89521`](https://youtrack.jetbrains.com/issue/KT-89521) wasmJsTestBundleAsEsm fails instead of being skipped when there are no test sources
- [`KT-84258`](https://youtrack.jetbrains.com/issue/KT-84258) K/Wasm: Remove deprecated properties in BinaryenExec
- [`KT-89856`](https://youtrack.jetbrains.com/issue/KT-89856) K/Wasm: update Binaryen to 133
- [`KT-89813`](https://youtrack.jetbrains.com/issue/KT-89813) K/Wasm: Update wasmtime to 49.0.1
- [`KT-89510`](https://youtrack.jetbrains.com/issue/KT-89510) Wasm: Promote declarations deprecations
- [`KT-87562`](https://youtrack.jetbrains.com/issue/KT-87562) K/Wasm: remove `ExperimentalWasmDsl` from wasm-js target-related declarations KGP

### Tools. Incremental Compile

- [`KT-90060`](https://youtrack.jetbrains.com/issue/KT-90060) Kotlin/JS: Internal compiler error "IrClassSymbolImpl is unbound. Signature: kotlin/Unit|null[0]" with incremental compilation
- [`KT-88539`](https://youtrack.jetbrains.com/issue/KT-88539) Incremental JS Klib compilation tests fail with LV 2.4+
- [`KT-89912`](https://youtrack.jetbrains.com/issue/KT-89912) Kotlin 2.4.20: JS/Wasm IC fails removing FIR-generated hint, then falls back to a full rebuild
- [`KT-90019`](https://youtrack.jetbrains.com/issue/KT-90019) IC: Classpath snapshot shrinking builds the reverse-impact resolver even when no classpath classes are referenced
- [`KT-89336`](https://youtrack.jetbrains.com/issue/KT-89336) IC: inferred interface member return type is not recompiled when its underlying type changes
- [`KT-89044`](https://youtrack.jetbrains.com/issue/KT-89044) K2 KMP JVM IC: Actual typealias overload change ignored by incremental compilation
- [`KT-89300`](https://youtrack.jetbrains.com/issue/KT-89300) K2 KMP JVM IC: Interface delegation causes IllegalStateException
- [`KT-89342`](https://youtrack.jetbrains.com/issue/KT-89342) Investigate potential performance improvement for IC classpath snapshotting

### Tools. JPS

- [`KT-89464`](https://youtrack.jetbrains.com/issue/KT-89464) Compilation fails with NoClassDefFoundError in UtilsKt
- [`KT-88702`](https://youtrack.jetbrains.com/issue/KT-88702) Support incremental dumb-mode compiler execution with providing tracker data output for JPS
- [`KT-89569`](https://youtrack.jetbrains.com/issue/KT-89569) Remove copied APIs from JPS

### Tools. Kapt

- [`KT-70784`](https://youtrack.jetbrains.com/issue/KT-70784) Consider using LightTree instead of PSI in K2 Kapt
- [`KT-89349`](https://youtrack.jetbrains.com/issue/KT-89349) Investigate failures in KAPT tests if one enables `+CLBasedAnnotationResolution`
- [`KT-88608`](https://youtrack.jetbrains.com/issue/KT-88608) KAPT: Do not generate JTree in direct mode

### Tools. Maven

- [`KT-90004`](https://youtrack.jetbrains.com/issue/KT-90004) Maven and Gradle tests fail after scripting runtime migration

### Tools. Performance benchmarks

- [`KT-87324`](https://youtrack.jetbrains.com/issue/KT-87324) K/N: Add runtime benchmarks for release caches

### Tools. REPL

- [`KT-86291`](https://youtrack.jetbrains.com/issue/KT-86291) Drop GenericReplCompiler
- [`KT-89748`](https://youtrack.jetbrains.com/issue/KT-89748) K2 REPL tests: in-process and Analysis API REPL runners keep failed snippets in the history
- [`KT-89747`](https://youtrack.jetbrains.com/issue/KT-89747) K2 REPL: a snippet failing after resolution stays in the history and consumes a snippet number
- [`KT-89750`](https://youtrack.jetbrains.com/issue/KT-89750) K2 REPL: UNINITIALIZED_VARIABLE is not reported for snippet properties
- [`KT-89749`](https://youtrack.jetbrains.com/issue/KT-89749) K2 REPL: UNRESOLVED_REFERENCE to a previous snippet's class in a function with an implicit return type
- [`KT-87432`](https://youtrack.jetbrains.com/issue/KT-87432) Implement JSR-223 variant on K2 compiler that compiles out of process
- [`KT-89459`](https://youtrack.jetbrains.com/issue/KT-89459) Remove `-Xrepl` option from `kotlinc`

### Tools. Scripts

- [`KT-90298`](https://youtrack.jetbrains.com/issue/KT-90298) JSR-223: ENGINE_SCOPE/GLOBAL_SCOPE bindings are lost after the ScriptContext is replaced
- [`KT-90299`](https://youtrack.jetbrains.com/issue/KT-90299) JSR-223: post-GC heap growth per evaluated snippet is ~6x higher in 2.5.20-dev-1225 than in 2.4.10 and ~3x higher than in 2.5.0-Beta1
- [`KT-90244`](https://youtrack.jetbrains.com/issue/KT-90244) Scripting, JSR-223: Each new ScriptEngine or ScriptContext retains its K2 compiler environment, leading to OutOfMemoryError
- [`KT-89627`](https://youtrack.jetbrains.com/issue/KT-89627) K2: a script processed before its definition's package sources causes 'Unresolved reference' in that package
- [`KT-89220`](https://youtrack.jetbrains.com/issue/KT-89220) JSR-223: generic Java bindings break all subsequent `eval()` calls
- [`KT-89247`](https://youtrack.jetbrains.com/issue/KT-89247) Kotlin Script: NoClassDefFoundError on 2nd execution with transitive dependency from imported script
- [`KT-42101`](https://youtrack.jetbrains.com/issue/KT-42101) Scripts: `@file`:Import() in kotlin-main-kts uses a stale cache
- [`KT-90022`](https://youtrack.jetbrains.com/issue/KT-90022) The completion doesn't work in main-kts scripts in IJ
- [`KT-89752`](https://youtrack.jetbrains.com/issue/KT-89752) JSR-223 (K2): the engine's snippet number (repl.currentSnippetNo) is ignored by the compiler
- [`KT-74354`](https://youtrack.jetbrains.com/issue/KT-74354) Implement (and use) expected exceptions processing in K2 REPL tests

### Tools. Statistics (FUS)

- [`KT-88686`](https://youtrack.jetbrains.com/issue/KT-88686) Add FUS metrics to the new Swift Export DSL

## 2.5.0-Beta1

### Analysis API

- [`KT-88945`](https://youtrack.jetbrains.com/issue/KT-88945) Analysis API: annotation resolution drops a class id when a source annotation shadows a library one
- [`KT-87460`](https://youtrack.jetbrains.com/issue/KT-87460) Flaky (Embeddable)SwiftExport tests
- [`KT-68896`](https://youtrack.jetbrains.com/issue/KT-68896) Support VirtualFile binary dependency inputs to Analysis API modules
- [`KT-65417`](https://youtrack.jetbrains.com/issue/KT-65417) K2 IDE: KTOR false positive expect-actual matching error on enum class because of implicit clone() in non-JVM source sets

### Analysis API. FIR

- [`KT-88962`](https://youtrack.jetbrains.com/issue/KT-88962) False negative [AMBIGUOUS_ANNOTATION_ARGUMENT] diagnostic in some corner cases
- [`KT-87651`](https://youtrack.jetbrains.com/issue/KT-87651) [Analysis API] Unexpected FIR status exception on SAM interfaces
- [`KT-87572`](https://youtrack.jetbrains.com/issue/KT-87572) Symbols from companion blocks have dispatchReceiver in stub-based version
- [`KT-69727`](https://youtrack.jetbrains.com/issue/KT-69727) K2 IDE. Wrong error in the editor on calling `clone` function of actual enum instance in non-jvm platform
- [`KT-69726`](https://youtrack.jetbrains.com/issue/KT-69726) FP errors on declaring `fun clone()` in actual enum in not-jvm source-set
- [`KT-72660`](https://youtrack.jetbrains.com/issue/KT-72660) ISE: Recursive update at org.jetbrains.kotlin.analysis.low.level.api.fir.caches.FirCaffeineCache.getValue
- [`KT-62892`](https://youtrack.jetbrains.com/issue/KT-62892) K2 IDE. Java outer class from other module is not resolved when nested class is accessed with fq name in a type position

### Analysis API. Infrastructure

- [`KT-84914`](https://youtrack.jetbrains.com/issue/KT-84914) Do not publish analysis-api-test-framework
- [`KT-87672`](https://youtrack.jetbrains.com/issue/KT-87672) Eliminate class duplication in the Analysis API JARs
- [`KT-87998`](https://youtrack.jetbrains.com/issue/KT-87998) Move out Standalone components from the 'kotlin-analysis-api-surface' and 'kotlin-analysis-api-implementation'
- [`KT-88404`](https://youtrack.jetbrains.com/issue/KT-88404) Merge the decompiler text and stubs into one test case
- [`KT-88371`](https://youtrack.jetbrains.com/issue/KT-88371) Optimize symbol light classes tests
- [`KT-87607`](https://youtrack.jetbrains.com/issue/KT-87607) Test Federation: Review domain Analysis API Smoke & Contract tests
- [`KT-86787`](https://youtrack.jetbrains.com/issue/KT-86787) Stop publish analysis-api-fe10-for-ide
- [`KT-87593`](https://youtrack.jetbrains.com/issue/KT-87593) Migrate Analysis API tests on JDK 11
- [`KT-87529`](https://youtrack.jetbrains.com/issue/KT-87529) Test Data Manager: introduce a separate task for checks

### Analysis API. Light Classes

- [`KT-57537`](https://youtrack.jetbrains.com/issue/KT-57537) SLC: propagate default parameter value from (`@JvmOverloads`) `expect` declarations to `actual` declarations
- [`KT-88879`](https://youtrack.jetbrains.com/issue/KT-88879) Value classes / JvmExposeBoxed: false-negative resolve for overridden exposed functions
- [`KT-88861`](https://youtrack.jetbrains.com/issue/KT-88861) Value classes / JvmExposeBoxed: internal method from value class is shown as valid call on java side but cannot be compiled
- [`KT-88805`](https://youtrack.jetbrains.com/issue/KT-88805) SLC: `@JvmInline` value class` is treated as `PsiClass#isValueClass`
- [`KT-65385`](https://youtrack.jetbrains.com/issue/KT-65385) K2 IDE: Light classes incorrectly expose `Companion` class and the corresponding static field if the companion object is generated by a compiler plugin
- [`KT-67711`](https://youtrack.jetbrains.com/issue/KT-67711) K2: Pointer already disposed from SLC/AI

### Analysis API. PSI

- [`KT-89032`](https://youtrack.jetbrains.com/issue/KT-89032) Missing PSI utilities for companion blocks
- [`KT-88925`](https://youtrack.jetbrains.com/issue/KT-88925) Restrict the `KtElement` hierarchy with `@SubclassOptInRequired`
- [`KT-88788`](https://youtrack.jetbrains.com/issue/KT-88788) Refine the new resolution API (PSI-part)
- [`KT-74070`](https://youtrack.jetbrains.com/issue/KT-74070) Cover the Kotlin PSI with KDocs
- [`KT-87819`](https://youtrack.jetbrains.com/issue/KT-87819) UseScope of private interfaces is incorrect
- [`KT-85052`](https://youtrack.jetbrains.com/issue/KT-85052) Move mutation methods out of the Kotlin PSI

### Analysis API. Providers and Caches

- [`KT-77194`](https://youtrack.jetbrains.com/issue/KT-77194) Analysis API: Move union and intersection scope merge strategies to the Analysis API
- [`KT-87231`](https://youtrack.jetbrains.com/issue/KT-87231) Analysis API: Fallback dependencies must have a binary-only base content scope
- [`KT-86402`](https://youtrack.jetbrains.com/issue/KT-86402) False positive MISSING_DEPENDENCY_SUPERCLASS when using Scala case class in Kotlin
- [`KT-87680`](https://youtrack.jetbrains.com/issue/KT-87680) Analysis API: LL FIR sessions contain redundant `BuiltinTypes` instances

### Analysis API. Standalone

- [`KT-89242`](https://youtrack.jetbrains.com/issue/KT-89242) Analysis API Standalone: KLIB/JAR file handles remain open after session disposal
- [`KT-61404`](https://youtrack.jetbrains.com/issue/KT-61404) Analysis API: implement proper library publishing structure
- [`KT-88362`](https://youtrack.jetbrains.com/issue/KT-88362) `PsiMethod.findSuperMethods()` always returns an empty array
- [`KT-84851`](https://youtrack.jetbrains.com/issue/KT-84851) Analysis API Standalone: 75% of CPU time spent in StringsKt.split via trie-based library scopes

### Analysis API. Stubs and Decompilation

#### New Features

- [`KT-10793`](https://youtrack.jetbrains.com/issue/KT-10793) Improve decompiled text for annotations
- [`KT-87857`](https://youtrack.jetbrains.com/issue/KT-87857) Support strict equals in stubs
- [`KT-85756`](https://youtrack.jetbrains.com/issue/KT-85756) Support annotation arguments in the decompiler

#### Performance Improvements

- [`KT-88067`](https://youtrack.jetbrains.com/issue/KT-88067) Implement stub-based initializer for constant properties
- [`KT-88060`](https://youtrack.jetbrains.com/issue/KT-88060) Make KtParenthesizedExpression stub-based
- [`KT-79396`](https://youtrack.jetbrains.com/issue/KT-79396) Make KtBinaryExpression stub-based
- [`KT-79397`](https://youtrack.jetbrains.com/issue/KT-79397) Make KtUnaryExpression stub-based
- [`KT-71787`](https://youtrack.jetbrains.com/issue/KT-71787) `PsiRawFirBuilder.Visitor#visitStringTemplateExpression` forces AST loading

#### Fixes

- [`KT-88984`](https://youtrack.jetbrains.com/issue/KT-88984) compiler-common and analysis-api-impl-base cyclic depend on each other
- [`KT-78356`](https://youtrack.jetbrains.com/issue/KT-78356) Migrate stubs from stub element type to stub factories
- [`KT-88500`](https://youtrack.jetbrains.com/issue/KT-88500) Support multi-field value classes in Analysis API stubs / decompiler
- [`KT-52806`](https://youtrack.jetbrains.com/issue/KT-52806) Store type in callable stubs when callable have implicit type and trivial initializer
- [`KT-77096`](https://youtrack.jetbrains.com/issue/KT-77096) Implement more stub-based annotation arguments

### Analysis API. Surface

#### New Features

- [`KT-84255`](https://youtrack.jetbrains.com/issue/KT-84255) Expose possible smart casts from ContextCollector in KaScopeContext
- [`KT-88985`](https://youtrack.jetbrains.com/issue/KT-88985) Refine the new resolution API around error handling
- [`KT-88521`](https://youtrack.jetbrains.com/issue/KT-88521) Analysis API surface: support "Strict equals" (equality bound types) language feature
- [`KT-86610`](https://youtrack.jetbrains.com/issue/KT-86610) Rework KaDiagnosticProvider to be more flexible
- [`KT-68881`](https://youtrack.jetbrains.com/issue/KT-68881) Analysis API: Introduce `KaType.classId`
- [`KT-87311`](https://youtrack.jetbrains.com/issue/KT-87311) Propagate psi from generated declarations to KaSymbols

#### Fixes

- [`KT-84186`](https://youtrack.jetbrains.com/issue/KT-84186) Unresolved annotation  `ClassId` returns "<error>"
- [`KT-89175`](https://youtrack.jetbrains.com/issue/KT-89175) Add contracts to the new resolution utilities
- [`KT-88767`](https://youtrack.jetbrains.com/issue/KT-88767) Refine the new resolution API
- [`KT-87557`](https://youtrack.jetbrains.com/issue/KT-87557) 'Analysis API Public API Checks' run configuration should include the artifacts checks
- [`KT-89030`](https://youtrack.jetbrains.com/issue/KT-89030) [Analysis API] Generalize `KaFirNestedInLocalClassFromCompilerPluginSymbolPointer`
- [`KT-73195`](https://youtrack.jetbrains.com/issue/KT-73195) AA FIR: missing annotations on function literal
- [`KT-82397`](https://youtrack.jetbrains.com/issue/KT-82397) `KaFunctionType` should offer context parameter types, not context receivers
- [`KT-88645`](https://youtrack.jetbrains.com/issue/KT-88645) Stabilize KtResolvable(Call)
- [`KT-88489`](https://youtrack.jetbrains.com/issue/KT-88489) resolveToCallCandidates throws ClassCastException for callable reference expressions resolveToCallCandidates
- [`KT-85528`](https://youtrack.jetbrains.com/issue/KT-85528) Move diagnostic classes out of analysis-api-fir
- [`KT-63221`](https://youtrack.jetbrains.com/issue/KT-63221) K2: Analysis API: rework `getDiagnosticsForElement` API
- [`KT-87210`](https://youtrack.jetbrains.com/issue/KT-87210) KaSubstitutor.Empty shouldn't be a class
- [`KT-75549`](https://youtrack.jetbrains.com/issue/KT-75549) Deprecate `KaTypeProvider#collectImplicitReceiverTypes`
- [`KT-54508`](https://youtrack.jetbrains.com/issue/KT-54508) Re-design 'javaGetterName' and 'javaSetterName'
- [`KT-86591`](https://youtrack.jetbrains.com/issue/KT-86591) Rethink how the context parameters feature could be integrated into the Analysis API Surface
- [`KT-87564`](https://youtrack.jetbrains.com/issue/KT-87564) Analysis API: nested type alias qualifier resolves to expanded type owner
- [`KT-87924`](https://youtrack.jetbrains.com/issue/KT-87924) Consider dropping `KaType.is*Type` utilities
- [`KT-87552`](https://youtrack.jetbrains.com/issue/KT-87552) Compiler-internal types leak into the public Analysis API surface
- [`KT-87578`](https://youtrack.jetbrains.com/issue/KT-87578) Explicitly split implementations and APIs in symbols
- [`KT-69959`](https://youtrack.jetbrains.com/issue/KT-69959) `fakeOverrideOriginal` should support value/type/receiver parameters
- [`KT-87338`](https://youtrack.jetbrains.com/issue/KT-87338) Drop KaTypeParameterOwnerSymbol
- [`KT-87343`](https://youtrack.jetbrains.com/issue/KT-87343) AA: hasDefaultValue is incorrect for parameter of actual constructor
- [`KT-73946`](https://youtrack.jetbrains.com/issue/KT-73946) Analysis API: Add a way to navigate from "KaPropertySymbol" to "KaValueParameterSymbol" for primary constructor properties
- [`KT-81264`](https://youtrack.jetbrains.com/issue/KT-81264) K2 AA: `KaValueParameterSymbol.hasDefaultValue` is false for overriding or actual functions

### Backend. Wasm

#### New Features

- [`KT-88027`](https://youtrack.jetbrains.com/issue/KT-88027) K/Wasm: Export `cabi_realloc` function from stdlib to support components

#### Fixes

- [`KT-87125`](https://youtrack.jetbrains.com/issue/KT-87125) K/Wasm: share a common base class for all suspend functions
- [`KT-89009`](https://youtrack.jetbrains.com/issue/KT-89009) [Wasm/MMCW] Compilation fail with exception on simple project
- [`KT-87742`](https://youtrack.jetbrains.com/issue/KT-87742) K/Wasm: initialization of properties with default value different from JVM
- [`KT-87746`](https://youtrack.jetbrains.com/issue/KT-87746) K/Wasm: Investigate if it is possible to enable floating point const folding on Wasm backend
- [`KT-88808`](https://youtrack.jetbrains.com/issue/KT-88808) K\Wasm: separate CoroutineImpl and CoroutineImplStackSwitching
- [`KT-66089`](https://youtrack.jetbrains.com/issue/KT-66089) K/WASM: assertSame failed in test `static_smallIntIdentity.kt`
- [`KT-87862`](https://youtrack.jetbrains.com/issue/KT-87862) K/Wasm: introduce a separate diagnostic ID for companion object inside external interface
- [`KT-88434`](https://youtrack.jetbrains.com/issue/KT-88434) K/Wasm: JS-to-Kotlin closure converter is shared between different external function types with the same JS-erased signature, injecting a wrong cast
- [`KT-88543`](https://youtrack.jetbrains.com/issue/KT-88543) K/Wasm: Automatically clean test output by default
- [`KT-85772`](https://youtrack.jetbrains.com/issue/KT-85772) K/Wasm: implement companion blocks and extensions
- [`KT-83171`](https://youtrack.jetbrains.com/issue/KT-83171) K/Wasm: Investigate import.meta usage in mjs files
- [`KT-79411`](https://youtrack.jetbrains.com/issue/KT-79411) K/WasmJs: "Can't link symbol kotlin.js/js|js(kotlin.String){}[0]"
- [`KT-88387`](https://youtrack.jetbrains.com/issue/KT-88387) K/Wasm: DWARF debuginfo accidentally swaps files and folders
- [`KT-88366`](https://youtrack.jetbrains.com/issue/KT-88366) [Wasm] mute failing coroutines stack switching test via WASM_IGNORE_FOR directive
- [`KT-87583`](https://youtrack.jetbrains.com/issue/KT-87583) Wasm IC cache is not invalidated when kotlin.wasm.compilationMode changes causing IndexOutOfBoundsException in WasmDeserializer
- [`KT-87687`](https://youtrack.jetbrains.com/issue/KT-87687) Investigate whether unboxing needed when accessing suspendResult in transformed doResume for suspend function
- [`KT-87128`](https://youtrack.jetbrains.com/issue/KT-87128) K/Wasm: investigate possible optimization of suspend "helper" functions
- [`KT-87329`](https://youtrack.jetbrains.com/issue/KT-87329) K/Wasm: child companion block static initializer runs before parent initializer in single-module mode
- [`KT-87109`](https://youtrack.jetbrains.com/issue/KT-87109) K/Wasm: Companion block initializer block fails in multi-module scenario

### Compiler

#### New Features

- [`KT-87345`](https://youtrack.jetbrains.com/issue/KT-87345) Swift Export: Support proper exception throwing from sync bridges in cross-language inheritance
- [`KT-87767`](https://youtrack.jetbrains.com/issue/KT-87767) Add JVM target bytecode version 27
- [`KT-80964`](https://youtrack.jetbrains.com/issue/KT-80964) Improve error messages for compile-time constants
- [`KT-87412`](https://youtrack.jetbrains.com/issue/KT-87412) Kotlin/Native: use JVM-like class initialization scheme with CompanionBlocksAndExtensions
- [`KT-88415`](https://youtrack.jetbrains.com/issue/KT-88415) Java type enhancement: support org.chromium.build.annotations nullability annotations as JSpecify equivalents
- [`KT-87419`](https://youtrack.jetbrains.com/issue/KT-87419) (De)serialize attribute set for strict `equals`
- [`KT-86129`](https://youtrack.jetbrains.com/issue/KT-86129) Strict `equals` checks
- [`KT-87418`](https://youtrack.jetbrains.com/issue/KT-87418) Incompatible `==` / `!=` checks based on `@EqualityBound`
- [`KT-86124`](https://youtrack.jetbrains.com/issue/KT-86124) Set attribute `equalityBoundType` to parameters
- [`KT-87498`](https://youtrack.jetbrains.com/issue/KT-87498) Strict equals x interface delegation
- [`KT-87417`](https://youtrack.jetbrains.com/issue/KT-87417) Generate prologue of strict equals in Fir2Ir
- [`KT-87420`](https://youtrack.jetbrains.com/issue/KT-87420) Set `equalityBoundType` attribute for generated equals in data and value classes
- [`KT-86127`](https://youtrack.jetbrains.com/issue/KT-86127) Add smart casts based on strict operator equals
- [`KT-87422`](https://youtrack.jetbrains.com/issue/KT-87422) Smart casts inside the body of strict `equals`
- [`KT-81676`](https://youtrack.jetbrains.com/issue/KT-81676) Improve error reporting for underscores as type parameters
- [`KT-58562`](https://youtrack.jetbrains.com/issue/KT-58562) More concise error for "Incompatible classes were found in dependencies"
- [`KT-80373`](https://youtrack.jetbrains.com/issue/KT-80373) Add initial support for release compiler caches
- [`KT-68819`](https://youtrack.jetbrains.com/issue/KT-68819) Don't report "no corresponding expect/actual declaration" when signature contains error types
- [`KT-85925`](https://youtrack.jetbrains.com/issue/KT-85925) Native: rework virtual trampolines machinery
- [`KT-83040`](https://youtrack.jetbrains.com/issue/KT-83040) Collection literals: ensure normal interaction with CFG
- [`KT-79656`](https://youtrack.jetbrains.com/issue/KT-79656) Use `org.jetbrains.annotations.UnmodifiableView` and/or `org.jetbrains.annotations.Unmodifiable` to infer read-only types for Java entities

#### Performance Improvements

- [`KT-57995`](https://youtrack.jetbrains.com/issue/KT-57995) Extra anonymous class is created for lambda after upgrading from Kotlin 1.6.10 to 1.7.20
- [`KT-89131`](https://youtrack.jetbrains.com/issue/KT-89131) JVM: Hoist ResultKt.throwOnFailure out of SuspendLambda.invokeSuspend switch cases
- [`KT-615`](https://youtrack.jetbrains.com/issue/KT-615) Optimize instanceof check for nullable type
- [`KT-49139`](https://youtrack.jetbrains.com/issue/KT-49139) NI: EnumMap constructor call in Kotlin source causes OutOfMemoryError during compilation and analysis in IntelliJ Idea
- [`KT-52283`](https://youtrack.jetbrains.com/issue/KT-52283) Never ending type inference while compiling Kotlin code with lots of self pointing generics
- [`KT-85899`](https://youtrack.jetbrains.com/issue/KT-85899) Performance problem around the second incorporation kind
- [`KT-86022`](https://youtrack.jetbrains.com/issue/KT-86022) K2 frontend hangs in approximateToSuperTypeWithRecursionPrevention
- [`KT-88383`](https://youtrack.jetbrains.com/issue/KT-88383) K/N CallGraph: Eager check before pushing elements onto the stack
- [`KT-88352`](https://youtrack.jetbrains.com/issue/KT-88352) JVM: redundant `Character` box/unbox and `Ref.IntRef` when a bound callable reference is passed to an inline function from within another inlined lambda
- [`KT-88435`](https://youtrack.jetbrains.com/issue/KT-88435) Kotlin/Native: Avoid redundant filtering in DevirtualizationAnalysis
- [`KT-87033`](https://youtrack.jetbrains.com/issue/KT-87033) [Native] Optimize dumpObjcSelectorToSignatureMapping to execute during frontend phase, bypassing IR linking
- [`KT-87952`](https://youtrack.jetbrains.com/issue/KT-87952) Bound receiver wrapped in a cast is needlessly stored in a `$receiver` field for property reference delegation
- [`KT-87896`](https://youtrack.jetbrains.com/issue/KT-87896) Make check for JvmFieldApplicabilityProblem.NOT_PUBLIC_VAL_WITH_JVMFIELD not quadratic
- [`KT-69758`](https://youtrack.jetbrains.com/issue/KT-69758) FastJarFS - avoid copying data on inflating (JDK 16+)
- [`KT-83068`](https://youtrack.jetbrains.com/issue/KT-83068) Investigate long compilation times with many generics & overloads
- [`KT-66469`](https://youtrack.jetbrains.com/issue/KT-66469) Long compilation with mockk import

#### Fixes

- [`KT-88715`](https://youtrack.jetbrains.com/issue/KT-88715) K/N: "Argument list too long" when linking with a large number of included static libraries
- [`KT-86978`](https://youtrack.jetbrains.com/issue/KT-86978) Type arguments are allowed (and unresolved) in qualifiers in compiler-required annotations
- [`KT-87372`](https://youtrack.jetbrains.com/issue/KT-87372) False positive INVISIBLE_REFERENCE when accessing protected companion block member of superclass
- [`KT-87284`](https://youtrack.jetbrains.com/issue/KT-87284) Implement type conversion from ILT/builtin numeric types into types marked with a special annotation
- [`KT-88953`](https://youtrack.jetbrains.com/issue/KT-88953) CCE caused by suspend conversion of nullable argument to nullable expected type
- [`KT-86453`](https://youtrack.jetbrains.com/issue/KT-86453) K2: Generate IrRich*Reference for all kinds of references
- [`KT-86046`](https://youtrack.jetbrains.com/issue/KT-86046) Put `@K1Deprecation` to all public declarations of K1 modules
- [`KT-89325`](https://youtrack.jetbrains.com/issue/KT-89325) Java Direct: no supertypes on resolution due to reentrance
- [`KT-89184`](https://youtrack.jetbrains.com/issue/KT-89184) JVM: anonymous function SAM-converted via invokedynamic loses its non-null parameter assertion
- [`KT-89183`](https://youtrack.jetbrains.com/issue/KT-89183) JVM: LambdaConversionException on JDK 9+ when a SAM method parameter is a type parameter with two interface upper bounds
- [`KT-87385`](https://youtrack.jetbrains.com/issue/KT-87385) Ensure that KMP mismatches in libraries in the HMPP scheme are reported reasonably
- [`KT-87146`](https://youtrack.jetbrains.com/issue/KT-87146) False negative REPEATED_ANNOTATION with actual typealias
- [`KT-88339`](https://youtrack.jetbrains.com/issue/KT-88339) JvmExposeBoxed: no null check is generated in exposed methods
- [`KT-85879`](https://youtrack.jetbrains.com/issue/KT-85879) Get rid of second incorporation kind in inference
- [`KT-88995`](https://youtrack.jetbrains.com/issue/KT-88995) Inline scopes numbers: wrong call site line numbers when a default argument value contains an inline call
- [`KT-87499`](https://youtrack.jetbrains.com/issue/KT-87499) ClassCastException "Float cannot be cast to Unit" when a suspend function's result is discarded in a when-branch
- [`KT-79212`](https://youtrack.jetbrains.com/issue/KT-79212) Resolve new type for class literal of unbounded reified type parameter
- [`KT-88399`](https://youtrack.jetbrains.com/issue/KT-88399) K2: StackOverflowError in FirPrefixAndSuffixSyntaxChecker.getLeaf for a long string-concatenation chain
- [`KT-89057`](https://youtrack.jetbrains.com/issue/KT-89057) IllegalArgumentException: source must not be null in FirOverrideChecker
- [`KT-89049`](https://youtrack.jetbrains.com/issue/KT-89049) JVM: do not generate DefaultImpls for companion block functions
- [`KT-89043`](https://youtrack.jetbrains.com/issue/KT-89043) Explicit context arguments: align sinceVersion with the 2.6.0 Stable plan
- [`KT-77362`](https://youtrack.jetbrains.com/issue/KT-77362) JVM diagnostics are not properly reported for scripts and REPL
- [`KT-86556`](https://youtrack.jetbrains.com/issue/KT-86556) Account for CSR in compiler required annotations transformer
- [`KT-87020`](https://youtrack.jetbrains.com/issue/KT-87020) Type aliased `AnnotationTarget` used in `@Target`()`
- [`KT-87072`](https://youtrack.jetbrains.com/issue/KT-87072) SINCE_KOTLIN doesn't work with non-literal arguments
- [`KT-87025`](https://youtrack.jetbrains.com/issue/KT-87025) Support aliased imports to compiler required annotations
- [`KT-89123`](https://youtrack.jetbrains.com/issue/KT-89123) Keep the visibility inconsistency about data class copy and primary constructor as a warning
- [`KT-89065`](https://youtrack.jetbrains.com/issue/KT-89065) JVM: companion block functions in interfaces do not compile with `-jvm-default=disable`
- [`KT-89087`](https://youtrack.jetbrains.com/issue/KT-89087) JVM: Initialization order of companion blocks is incorrect
- [`KT-87940`](https://youtrack.jetbrains.com/issue/KT-87940) [DFA] Remove VariableStorage
- [`KT-88388`](https://youtrack.jetbrains.com/issue/KT-88388) JvmExposeBoxed: Exposed constructor leads to NSME from another module
- [`KT-88325`](https://youtrack.jetbrains.com/issue/KT-88325) JvmExposeBoxed: False-positive "no effect" warning on extension properties
- [`KT-88369`](https://youtrack.jetbrains.com/issue/KT-88369) JvmExposeBoxed on overrides makes KAPT generate a standalone method with faulty `@Override` annotation
- [`KT-87540`](https://youtrack.jetbrains.com/issue/KT-87540) Fix potential issues in explicit context arguments implementation
- [`KT-89013`](https://youtrack.jetbrains.com/issue/KT-89013) K/N: auto-built release compiler caches are compiled at O1, not O3
- [`KT-88260`](https://youtrack.jetbrains.com/issue/KT-88260) java-direct: support Java 25 module import declarations (JEP 511)
- [`KT-88304`](https://youtrack.jetbrains.com/issue/KT-88304) java-direct: JavaUsingAst tests silently fall back to the PSI-based finder, hiding java-direct failures
- [`KT-88078`](https://youtrack.jetbrains.com/issue/KT-88078) Java Direct: Refactor binary class finder entanglement
- [`KT-86376`](https://youtrack.jetbrains.com/issue/KT-86376) New inline variables scheme: incorrect line number
- [`KT-87825`](https://youtrack.jetbrains.com/issue/KT-87825) Implement new frontend rules for companion blocks in interfaces
- [`KT-87437`](https://youtrack.jetbrains.com/issue/KT-87437) Restrict interface companion block properties
- [`KT-89008`](https://youtrack.jetbrains.com/issue/KT-89008) Constant condition elimination may break debug information of inline scopes
- [`KT-89016`](https://youtrack.jetbrains.com/issue/KT-89016) Replace early return with assertion in function type conversion
- [`KT-88667`](https://youtrack.jetbrains.com/issue/KT-88667) False negative POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION on `@get`: use-site target and annotation constructor call
- [`KT-87330`](https://youtrack.jetbrains.com/issue/KT-87330) Introduce Static Initialization Control Flow Graph for Classes
- [`KT-88427`](https://youtrack.jetbrains.com/issue/KT-88427) K2: compiler crashes with IllegalArgumentException when escaped class name containing braces is used in diagnostic type rendering
- [`KT-88961`](https://youtrack.jetbrains.com/issue/KT-88961) K2: FirPrefixAndSuffixSyntaxChecker misses literal prefixes/suffixes outside binary expressions with the LightTree parser
- [`KT-88576`](https://youtrack.jetbrains.com/issue/KT-88576) False-negative `OVERLOAD_RESOLUTION_AMBIGUITY` on increment call in recursive initializer
- [`KT-88934`](https://youtrack.jetbrains.com/issue/KT-88934) Error in IrConstExpressionTransformer when lowering constant expression in subject for function reference
- [`KT-88811`](https://youtrack.jetbrains.com/issue/KT-88811) Compiler crashes with "IllegalArgumentException: List has more than one element" when separate KMP compilation is combined with useMetadataOnIncrementalClasspath
- [`KT-88312`](https://youtrack.jetbrains.com/issue/KT-88312) Implement 'returnsParameter' contract
- [`KT-88307`](https://youtrack.jetbrains.com/issue/KT-88307) Support actualization of multiple top-level `expect` functions with the same `actual`
- [`KT-88012`](https://youtrack.jetbrains.com/issue/KT-88012) Abstract diagnostic factories from source types
- [`KT-88580`](https://youtrack.jetbrains.com/issue/KT-88580) Kotlin/Native Release: "Unexpected receiver type: kotlin.collections.ArrayList" in generic collection code
- [`KT-88316`](https://youtrack.jetbrains.com/issue/KT-88316) K/N: StackOverflowError in CastsOptimization when two nullable local variables are assigned to each other across nested loops
- [`KT-88770`](https://youtrack.jetbrains.com/issue/KT-88770) KMP separate compilation: false-positive SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC
- [`KT-88769`](https://youtrack.jetbrains.com/issue/KT-88769) KMP separate compilation: incorrect `EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE` in case of complex inheritance by delegation
- [`KT-88860`](https://youtrack.jetbrains.com/issue/KT-88860) Fir2Ir crash "Expected argument type to be Nothing?" when a non-null value is null-checked and passed to a generic function
- [`KT-64219`](https://youtrack.jetbrains.com/issue/KT-64219) K2, Wasm: Green expect-actual code allows for invalid external type hierarchy
- [`KT-88761`](https://youtrack.jetbrains.com/issue/KT-88761) Native: remove custom validation phases
- [`KT-87788`](https://youtrack.jetbrains.com/issue/KT-87788) Incorrect smart cast stability of implicit `when` subject variables
- [`KT-77718`](https://youtrack.jetbrains.com/issue/KT-77718) K2: Remove workaround for KDF in FIR2IR implicit cast generation for receivers
- [`KT-85368`](https://youtrack.jetbrains.com/issue/KT-85368) ConstraintIncorporator#insideOtherConstraint is order-dependent
- [`KT-78002`](https://youtrack.jetbrains.com/issue/KT-78002) K2: local class function annotation observes the function parameters
- [`KT-88218`](https://youtrack.jetbrains.com/issue/KT-88218) -Xheader-mode: inline function bodies replaced with aconst_null/athrow stubs instead of being preserved
- [`KT-53422`](https://youtrack.jetbrains.com/issue/KT-53422) NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER in flow.collect
- [`KT-71754`](https://youtrack.jetbrains.com/issue/KT-71754) PCLA: compile-time failure on fixing a type variable on demand for an indexing operator assignment
- [`KT-64802`](https://youtrack.jetbrains.com/issue/KT-64802) K2: Investigate behavior change related to PCLA+Captured types+ILT
- [`KT-88228`](https://youtrack.jetbrains.com/issue/KT-88228) JVM: IllegalAccessError at runtime when delegating property to private member of sealed superclass
- [`KT-88455`](https://youtrack.jetbrains.com/issue/KT-88455) AssertionError "Inconsistent target list for lambda annotation" when Java annotation applied to lambda
- [`KT-67462`](https://youtrack.jetbrains.com/issue/KT-67462) Use new lowering phase creation API in Native backend
- [`KT-71600`](https://youtrack.jetbrains.com/issue/KT-71600) K2: missing WHEN_ENUM_CAN_BE_NULL_IN_JAVA
- [`KT-36111`](https://youtrack.jetbrains.com/issue/KT-36111) False negative warning on exhaustive `when` with flexible type of sealed class in subject
- [`KT-88190`](https://youtrack.jetbrains.com/issue/KT-88190) False positive UNEXHAUSTIVE_WHEN_BASED_ON_JAVA_ANNOTATIONS
- [`KT-88766`](https://youtrack.jetbrains.com/issue/KT-88766) K2 KMP JVM IC: "NoSuchElementException: Key Module <commonMain> is missing in the map" when an actual and its expect are recompiled together
- [`KT-87967`](https://youtrack.jetbrains.com/issue/KT-87967) Implement deprecation warning for concurrent maps purely implementation hierarchy
- [`KT-87165`](https://youtrack.jetbrains.com/issue/KT-87165) [K/N] computeTypes phase breaks boxed equality
- [`KT-88721`](https://youtrack.jetbrains.com/issue/KT-88721) KMP Separate Compilation: missing fake-override for internal declaration from a friend module
- [`KT-88099`](https://youtrack.jetbrains.com/issue/KT-88099) "ArithmeticException: / by zero" caused by Int.mod on division by zero in constant evaluation
- [`KT-88332`](https://youtrack.jetbrains.com/issue/KT-88332) IntrinsicConstEvaluation: false positive "const val initializer must be a constant value" on division by non-zero value that overflows to int zero
- [`KT-88326`](https://youtrack.jetbrains.com/issue/KT-88326) Enable IntrinsicConstEvaluation since 2.5
- [`KT-88662`](https://youtrack.jetbrains.com/issue/KT-88662) Render the actual receiver type in `UNRESOLVED_REFERENCE_WRONG_RECEIVER`
- [`KT-88013`](https://youtrack.jetbrains.com/issue/KT-88013) Native: migrate CastsOptimization from Predicate.Empty to Predicate.True and fix Predicates.or
- [`KT-88132`](https://youtrack.jetbrains.com/issue/KT-88132) False positive INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION when typealias for Any used as right component of definitely-not-nullable supertype
- [`KT-87659`](https://youtrack.jetbrains.com/issue/KT-87659) No warning emitted when runtime-retained annotations on lambdas are silently discarded by invokedynamic codegen
- [`KT-84499`](https://youtrack.jetbrains.com/issue/KT-84499)   Kotlin/Java interop: type mismatch when overriding methods from Java class with module import declaration (Java 25)
- [`KT-70450`](https://youtrack.jetbrains.com/issue/KT-70450) OuterThisLowering - consider merging it into InnerClassLowering
- [`KT-87881`](https://youtrack.jetbrains.com/issue/KT-87881) IllegalStateException: Expected some types crash in FIR type inference when buildList contains implicit receiver call and nested when expression with !is smartcast
- [`KT-88340`](https://youtrack.jetbrains.com/issue/KT-88340) 'returnsResultOf' contract works incorrectly with named arguments
- [`KT-88191`](https://youtrack.jetbrains.com/issue/KT-88191) KMP Separate Compilation: Problems with `throwNoWhenBranchMatchedException`  function
- [`KT-87389`](https://youtrack.jetbrains.com/issue/KT-87389) Change companion extension metadata serialization format
- [`KT-88480`](https://youtrack.jetbrains.com/issue/KT-88480) KMP parser: string literal content is remapped to IDENTIFIER if it matches a soft keyword
- [`KT-88408`](https://youtrack.jetbrains.com/issue/KT-88408) [IR] Consider eliminating `ModuleDescriptor` from `IrActualizer` module diagnostics
- [`KT-87983`](https://youtrack.jetbrains.com/issue/KT-87983) CCE on member extension property delegated to bound property reference
- [`KT-88487`](https://youtrack.jetbrains.com/issue/KT-88487) Return value checker: unresolved TODO about Array.contains in stdlib
- [`KT-88102`](https://youtrack.jetbrains.com/issue/KT-88102) Incompatible return type in delegated member not reported if it implements intersection override
- [`KT-85403`](https://youtrack.jetbrains.com/issue/KT-85403) kotlin.NoWhenBranchMatchedException on Kotlin Native with sealed interface
- [`KT-87922`](https://youtrack.jetbrains.com/issue/KT-87922) K1FakeDescriptorsForReferencesKt classes duplicated between kotlin-scripting-compiler-embeddable and kotlin-build-tools-impl
- [`KT-73462`](https://youtrack.jetbrains.com/issue/KT-73462) AssertionError "FUN LOCAL_FUNCTION_FOR_LAMBDA has no continuation" on suspend lambda with parameter of functional type called from another file
- [`KT-87549`](https://youtrack.jetbrains.com/issue/KT-87549) `@IntroducedAt` does not work correctly with inheritance
- [`KT-87887`](https://youtrack.jetbrains.com/issue/KT-87887) KJS: Prefix increment on dynamic array access is lowered into no-op
- [`KT-87129`](https://youtrack.jetbrains.com/issue/KT-87129) Decide what to do with tests in ScriptGenTest
- [`KT-67014`](https://youtrack.jetbrains.com/issue/KT-67014) K1/K2 handle when expression as annotation target differently
- [`KT-80741`](https://youtrack.jetbrains.com/issue/KT-80741) Fix rendering of inner classes with generic outer classes
- [`KT-86119`](https://youtrack.jetbrains.com/issue/KT-86119) JVM builtins dump for JDK21 does not include methods introduced after JDK8
- [`KT-86742`](https://youtrack.jetbrains.com/issue/KT-86742) JVM: missing accidental override error on companion block members
- [`KT-86109`](https://youtrack.jetbrains.com/issue/KT-86109) K2: Compiler crash on mix of static/companion in jkj hierarhy
- [`KT-88103`](https://youtrack.jetbrains.com/issue/KT-88103) Java Direct: use FirLazyJavaAnnotationList to avoid cycles in enums
- [`KT-88101`](https://youtrack.jetbrains.com/issue/KT-88101) Java Direct: Deduplicate repeated type resolution logic
- [`KT-88064`](https://youtrack.jetbrains.com/issue/KT-88064) Java Direct: Use lexer for lightweight "pre-parsing" instead of regexes
- [`KT-88079`](https://youtrack.jetbrains.com/issue/KT-88079) Java Direct: Get rid of VFS and VirtualFile use
- [`KT-88066`](https://youtrack.jetbrains.com/issue/KT-88066) Java Direct: (Re)introduce lazy AST calculations
- [`KT-87368`](https://youtrack.jetbrains.com/issue/KT-87368) Adjust the message of `IMPOSSIBLE_IS_CHECK`
- [`KT-87664`](https://youtrack.jetbrains.com/issue/KT-87664) JvmExposeBoxed: exposing constructor of ordinary class changes behavior
- [`KT-88134`](https://youtrack.jetbrains.com/issue/KT-88134) Improve diagnostics for name-based-destructuring=complete and using obsolete syntax
- [`KT-88149`](https://youtrack.jetbrains.com/issue/KT-88149) Mention operator token in UNRESOLVED_REFERENCE_WRONG_RECEIVER
- [`KT-65832`](https://youtrack.jetbrains.com/issue/KT-65832) K2: Support diagnostic suppression with complex expression argument
- [`KT-81416`](https://youtrack.jetbrains.com/issue/KT-81416) Native: compiler fails in case of cross-module Volatile references
- [`KT-86740`](https://youtrack.jetbrains.com/issue/KT-86740) K/JS: IllegalStateException "Serialization of IrErrorType is not supported" when type variable is inferred into empty intersection
- [`KT-88127`](https://youtrack.jetbrains.com/issue/KT-88127) Remove or replace usages of `psiElement` util
- [`KT-88128`](https://youtrack.jetbrains.com/issue/KT-88128) Rewrite `JvmBackendClassResolver` to get rid of descriptor usages
- [`KT-88141`](https://youtrack.jetbrains.com/issue/KT-88141) False negative MANY_IMPL_MEMBER_NOT_IMPLEMENTED when method is called equals, hashCode or toString
- [`KT-86543`](https://youtrack.jetbrains.com/issue/KT-86543) IllegalStateException: Parent of this declaration is not a class: PROPERTY IR_EXTERNAL_DECLARATION_STUB
- [`KT-88142`](https://youtrack.jetbrains.com/issue/KT-88142) False negative INTERFACE_AS_FUNCTION when Java interface contains function called toString, equals or hashCode with receiver or context parameters
- [`KT-87261`](https://youtrack.jetbrains.com/issue/KT-87261) K/N debug: 'is' check miscompilation — local variable reports wrong type when two smart-cast extractions precede an inline loop
- [`KT-87830`](https://youtrack.jetbrains.com/issue/KT-87830) Complementary value sets should include non-direct inheritors of supertypes
- [`KT-87103`](https://youtrack.jetbrains.com/issue/KT-87103) Drop compiler/fir/dump
- [`KT-86131`](https://youtrack.jetbrains.com/issue/KT-86131) Deprecate language version 2.2
- [`KT-88045`](https://youtrack.jetbrains.com/issue/KT-88045) Delete leftovers of javac integration (javac-wrapper) subsystem
- [`KT-87577`](https://youtrack.jetbrains.com/issue/KT-87577) Compiler crash on red code with ambiguous type parameter used as expression
- [`KT-87931`](https://youtrack.jetbrains.com/issue/KT-87931) CCE caused by suspend conversion in receiver position
- [`KT-87676`](https://youtrack.jetbrains.com/issue/KT-87676) Context Parameters: Cannot find local variable '$context-String' with type java.lang.String when evaluating contextual call that uses lambda context parameter
- [`KT-87061`](https://youtrack.jetbrains.com/issue/KT-87061) Companion blocks: compilation fails when companion function names clash with enum methods
- [`KT-86518`](https://youtrack.jetbrains.com/issue/KT-86518) K2: Enrich NoWhenBranchMatchedException with additional information about the cause
- [`KT-88007`](https://youtrack.jetbrains.com/issue/KT-88007) Don't report `equals(Any?)` in companion blocks of interfaces
- [`KT-87926`](https://youtrack.jetbrains.com/issue/KT-87926) USELESS_JVM_EXPOSE_BOXED confuses in case of argument presence
- [`KT-61582`](https://youtrack.jetbrains.com/issue/KT-61582) Kotlin/Native: deprecate and remove -generate-worker-test-runner
- [`KT-87821`](https://youtrack.jetbrains.com/issue/KT-87821) Do not promote visibility of local lambda classes in private inline scopes to Public
- [`KT-80005`](https://youtrack.jetbrains.com/issue/KT-80005) Kotlin/Native: remove eager GlobalData initialization
- [`KT-87975`](https://youtrack.jetbrains.com/issue/KT-87975) Reenable inline checks in noinline parameter default values
- [`KT-87255`](https://youtrack.jetbrains.com/issue/KT-87255) CompanionBlocksAndExtensions: companion blocks initialized before enum entires
- [`KT-81816`](https://youtrack.jetbrains.com/issue/KT-81816) False positive "Unreachable code" with lambda and elvis
- [`KT-87864`](https://youtrack.jetbrains.com/issue/KT-87864) K2IdeK2CodeKotlinSteppingTestGenerated$StepOver$Uncategorized.testSoSuspendableCallInEndOfFun fails
- [`KT-82676`](https://youtrack.jetbrains.com/issue/KT-82676) Replace reflection-based RuntimeFeatureToFlagMap with code generation
- [`KT-29890`](https://youtrack.jetbrains.com/issue/KT-29890) Inline constructors for arrays could not be inlined
- [`KT-72903`](https://youtrack.jetbrains.com/issue/KT-72903) K/N: combine `Context` and `KonanBackendContext`
- [`KT-82462`](https://youtrack.jetbrains.com/issue/KT-82462) Context sensitive resolution can use extension properties from imports
- [`KT-82465`](https://youtrack.jetbrains.com/issue/KT-82465) Context sensitive resolution does not resolve extension properties in presense of any additional receiver
- [`KT-82461`](https://youtrack.jetbrains.com/issue/KT-82461) Context sensitive resolution can use  extension properties declared on Any receiver
- [`KT-87269`](https://youtrack.jetbrains.com/issue/KT-87269) Kotlin Debugger: ISE “Non-empty unresolved argument list.” on evaluating serializable value class with custom serializer
- [`KT-65195`](https://youtrack.jetbrains.com/issue/KT-65195) K2: Unexpected exception when executing dynamic array element inc/dec
- [`KT-87297`](https://youtrack.jetbrains.com/issue/KT-87297) 'Underscore for unused variable' feature should be stable and enabled by default
- [`KT-87838`](https://youtrack.jetbrains.com/issue/KT-87838) IntroducedAt: Using `@IntroducedAt` in ordinary class constructor with inline class parameters leads to platform declaration clash
- [`KT-87189`](https://youtrack.jetbrains.com/issue/KT-87189) False negative NoWhenBranchMatchedException: when subtype implements multiple sealed interfaces
- [`KT-87466`](https://youtrack.jetbrains.com/issue/KT-87466) JvmExposeBoxed: Cannot generate code for delegating constructor of data class in IJ
- [`KT-87725`](https://youtrack.jetbrains.com/issue/KT-87725) Prohibit lambda implementations of sealed Java interfaces
- [`KT-87436`](https://youtrack.jetbrains.com/issue/KT-87436) Separate LFs and compiler arguments for companion extensions
- [`KT-85143`](https://youtrack.jetbrains.com/issue/KT-85143) Remove the unfinished "compile against klib" feature on JVM
- [`KT-75945`](https://youtrack.jetbrains.com/issue/KT-75945) Indy: Allow lambdas with annotations
- [`KT-84802`](https://youtrack.jetbrains.com/issue/KT-84802) Add new synthetic class flag to JVM metadata
- [`KT-86635`](https://youtrack.jetbrains.com/issue/KT-86635) Analysis API: arguments of applied annotation is sometimes (incorrectly) empty
- [`KT-87553`](https://youtrack.jetbrains.com/issue/KT-87553) K2: Incorrect argument evaluation order with explicit context argument in presence of explicit extension receiver
- [`KT-87546`](https://youtrack.jetbrains.com/issue/KT-87546) Fix slightly confusing test data for explicit context arguments in case of Java override
- [`KT-87696`](https://youtrack.jetbrains.com/issue/KT-87696) NBD doesn't work for smart-casted MFVC
- [`KT-87695`](https://youtrack.jetbrains.com/issue/KT-87695) Compiler crash while compilation of MFVC destructuring with a variable name mismatch
- [`KT-85307`](https://youtrack.jetbrains.com/issue/KT-85307) "Type argument is not within its bounds" for Java classes with recursive generic bounds (F-bounded polymorphism)
- [`KT-87202`](https://youtrack.jetbrains.com/issue/KT-87202) K/N IC: NoSuchFileException: metadata.properties when upgrading Kotlin from <2.2.10 to >2.2.20
- [`KT-87606`](https://youtrack.jetbrains.com/issue/KT-87606) Compiler crash on red code with redeclaration of type parameter which is used as expression
- [`KT-87786`](https://youtrack.jetbrains.com/issue/KT-87786) Make `ConeLookupTagBasedType` into a sealed class
- [`KT-86554`](https://youtrack.jetbrains.com/issue/KT-86554) Check suspicious when over ConeKotlinType in JavaOverrideChecker
- [`KT-87775`](https://youtrack.jetbrains.com/issue/KT-87775) Incorrect RETURN_VALUE_NOT_USED_COERCION where regular 'Return value not used' was expected
- [`KT-87073`](https://youtrack.jetbrains.com/issue/KT-87073) CompanionBlocksAndExtensions: calling companion extension function from project/binary dependency fails with java.lang.IndexOutOfBoundsException
- [`KT-84780`](https://youtrack.jetbrains.com/issue/KT-84780) The INITIALIZER_TYPE_MISMATCH diagnostic is reported on the assignment operator (=) instead of the initializer expression when there is a type mismatch
- [`KT-87335`](https://youtrack.jetbrains.com/issue/KT-87335) Context parameters: wrong LINENUMBER emitted for context argument loads in JVM bytecode
- [`KT-86332`](https://youtrack.jetbrains.com/issue/KT-86332) False positive MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION for implicit lambda parameters with inaccessible type annotations
- [`KT-87273`](https://youtrack.jetbrains.com/issue/KT-87273) K/N IC: cinterop klib caches are not recompiled
- [`KT-84562`](https://youtrack.jetbrains.com/issue/KT-84562) Get rid of usages of `@K1Deprecation`-annotated declarations in JVM backend
- [`KT-87550`](https://youtrack.jetbrains.com/issue/KT-87550) Drop the "too complex expression" error with `@IntroducedAt`
- [`KT-87505`](https://youtrack.jetbrains.com/issue/KT-87505) False positive `INVALID_VERSIONING_ON_VALUE_CLASS_PARAMETER` on value class secondary constructor with `@IntroducedAt`
- [`KT-87504`](https://youtrack.jetbrains.com/issue/KT-87504) Do not make `@IntroducedAt` a compiler-required annotation
- [`KT-87194`](https://youtrack.jetbrains.com/issue/KT-87194) K/N IC: changing an external library to an already-cached version in ~/.konan doesn't rebuild application's cache
- [`KT-82677`](https://youtrack.jetbrains.com/issue/KT-82677) Switch latest stable version in Kotlin project to 2.5
- [`KT-84406`](https://youtrack.jetbrains.com/issue/KT-84406) Remove support for IR serialization in JVM
- [`KT-72706`](https://youtrack.jetbrains.com/issue/KT-72706) Confusing "INVISIBLE_REFERENCE" when calling private constructor
- [`KT-87592`](https://youtrack.jetbrains.com/issue/KT-87592) Kotlin/Native: make remove redundant safepoints phase a proper LLVM pass
- [`KT-87520`](https://youtrack.jetbrains.com/issue/KT-87520) Implicit JvmExposeBoxed breaks delegation by interface
- [`KT-87278`](https://youtrack.jetbrains.com/issue/KT-87278) Smartcast to `Any` in RHS of `==` with nullable type parameter
- [`KT-83575`](https://youtrack.jetbrains.com/issue/KT-83575) Native: perf logging doesn't work correctly for subcompilations (like per-file caches)
- [`KT-73771`](https://youtrack.jetbrains.com/issue/KT-73771) K2: Infinite compilation caused by buildList without type
- [`KT-85417`](https://youtrack.jetbrains.com/issue/KT-85417) K2: False negative SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC diagnostic when superclass is generic
- [`KT-30062`](https://youtrack.jetbrains.com/issue/KT-30062) "AssertionError: Recursion detected on input" with class implementing itself by delegation
- [`KT-87507`](https://youtrack.jetbrains.com/issue/KT-87507) False-positive `ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED` on fake-override with value parameter of type missing in dependencies
- [`KT-87413`](https://youtrack.jetbrains.com/issue/KT-87413) CompanionBlocksAndExtensions: impossible to use field in companion extension properties getters/setters
- [`KT-87351`](https://youtrack.jetbrains.com/issue/KT-87351) False positive WRONG_MODIFIER_CONTAINING_DECLARATION on internal companion block members in interface
- [`KT-87364`](https://youtrack.jetbrains.com/issue/KT-87364) False positive BACKING_FIELD_IN_INTERFACE on companion block member in interface
- [`KT-87360`](https://youtrack.jetbrains.com/issue/KT-87360) False positive DELEGATED_PROPERTY_IN_INTERFACE for delegated companion block member in interface
- [`KT-87347`](https://youtrack.jetbrains.com/issue/KT-87347) False negative NON_ABSTRACT_FUNCTION_WITH_NO_BODY/MUST_BE_INITIALIZED for companion block member in interface
- [`KT-86905`](https://youtrack.jetbrains.com/issue/KT-86905) DFA-based exhaustiveness bug with implicit companion access
- [`KT-85764`](https://youtrack.jetbrains.com/issue/KT-85764) Native: Prohibit compiling sources in 'kotlin' package by default
- [`KT-86464`](https://youtrack.jetbrains.com/issue/KT-86464) Support `isPublicAbi` JVM metadata flag for anonymous and synthetic local classes
- [`KT-86524`](https://youtrack.jetbrains.com/issue/KT-86524) JvmExposeBoxed: Collection does not implement abstract method
- [`KT-80247`](https://youtrack.jetbrains.com/issue/KT-80247) No diagnostic on unresolved type annotation from a dependency in an inferred type
- [`KT-85230`](https://youtrack.jetbrains.com/issue/KT-85230) False negative deprecation diagnostics on import of member of nested class
- [`KT-85188`](https://youtrack.jetbrains.com/issue/KT-85188) Don't poison binaries with companion blocks & extensions for LV >= 2.5
- [`KT-63454`](https://youtrack.jetbrains.com/issue/KT-63454) Properly check that inline fun is in the same module as callee in `IrSourceCompilerForInline`
- [`KT-71854`](https://youtrack.jetbrains.com/issue/KT-71854) K2 IDE. False positive red code because of external annotation on a generic parameter
- [`KT-82132`](https://youtrack.jetbrains.com/issue/KT-82132) False-positive type mismatch with -language-version 2.2
- [`KT-59526`](https://youtrack.jetbrains.com/issue/KT-59526) Store annotation default values in metadata on JVM
- [`KT-57802`](https://youtrack.jetbrains.com/issue/KT-57802) K2: Backend Internal error: RecordEnclosingMethodsLowering.kt
- [`KT-11474`](https://youtrack.jetbrains.com/issue/KT-11474) ISE: Requested A, got foo.A in JavaClassFinderImpl on Java file with package not matching directory

### Compose compiler
- [`b/555304803`](https://issuetracker.google.com/issues/555304803) Fixed a bug that made Compose stack trace mapping collection fail in certain cases in which a boolean variable was captured in a suspend lambda.
- [`b/539644912`](https://issuetracker.google.com/issues/539644912) [Compose] Support rich callable references for `@Composable` functions
- [`b/546101628`](https://issuetracker.google.com/issues/546101628) Fixed a bug that caused miscompilation whenever a `when` expression had a result that was computed by another `when` expression, and the inner `when` expression had a `@Composable` call in one of its conditions.
- [`b/549552317`](https://issuetracker.google.com/issues/549552317) Fixed a bug that, in certain cases, prevented execution of non-local return `statements` inside lambdas called from `when` expressions.
- [`b/481422057`](https://issuetracker.google.com/issues/481422057) Made the applier inferencer support repeated `ComposableTarget` annotations per the semantics described in the documentation of `ComposableTarget`
- [`b/530458587`](https://issuetracker.google.com/issues/530458587) Start initializing the fields of `ComposableSingletons` objects lazily


### IR. Inlining

- [`KT-81022`](https://youtrack.jetbrains.com/issue/KT-81022) KLIB inliner: Don't specify language features in test runners for IR Inliner on 1st stage
- [`KT-87521`](https://youtrack.jetbrains.com/issue/KT-87521) renderCascadingInlining throws NPE when reporting an AFU-transformed function
- [`KT-87829`](https://youtrack.jetbrains.com/issue/KT-87829) Volatile field leaking through internal inline function in friend module
- [`KT-86578`](https://youtrack.jetbrains.com/issue/KT-86578) Leaking of a private entity through a companion extension: cross-module case
- [`KT-88020`](https://youtrack.jetbrains.com/issue/KT-88020) [KLIBs] Enable cross-module inliner in stdlib & kotlin-test
- [`KT-86558`](https://youtrack.jetbrains.com/issue/KT-86558) Forbid accesing volatile references after inlining

### IR. Interpreter

- [`KT-84627`](https://youtrack.jetbrains.com/issue/KT-84627) Disable IR interpreter in the compiler
- [`KT-87043`](https://youtrack.jetbrains.com/issue/KT-87043) Remove IR interpreter sources from the compiler

### IR. Tree

#### Fixes

- [`KT-88864`](https://youtrack.jetbrains.com/issue/KT-88864) Cache deserializers using a stable key inside IR linker
- [`KT-88610`](https://youtrack.jetbrains.com/issue/KT-88610) Type parameter is out of bounds while returning fallback value
- [`KT-88693`](https://youtrack.jetbrains.com/issue/KT-88693) Investigate using the same IR validation settings for all backends
- [`KT-70048`](https://youtrack.jetbrains.com/issue/KT-70048) IR: remove addSimpleDelegatingConstructor
- [`KT-85576`](https://youtrack.jetbrains.com/issue/KT-85576) KLIBs: Add PL tests with companion blocks and companion extensions
- [`KT-87456`](https://youtrack.jetbrains.com/issue/KT-87456) Make sure IR renderer is working correctly with `isStatic` call
- [`KT-69305`](https://youtrack.jetbrains.com/issue/KT-69305) Re-enable reporting out-of-scope type parameter usages during pre-lowering IR validation
- [`KT-88146`](https://youtrack.jetbrains.com/issue/KT-88146) Replace `IrDeclaration.module` with  `IrDeclaration.moduleFragment`
- [`KT-84335`](https://youtrack.jetbrains.com/issue/KT-84335) [Klib] Carefully revise usage of IR validator checkers in testrunners
- [`KT-88177`](https://youtrack.jetbrains.com/issue/KT-88177) IR: Mark `DescriptorVisibility.isVisible` with `@ObsoleteDescriptorBasedAPI` and ensure it is not used outside the old frontend and psi2ir
- [`KT-77049`](https://youtrack.jetbrains.com/issue/KT-77049) Avoid failing to produce an IR dump in tests
- [`KT-87198`](https://youtrack.jetbrains.com/issue/KT-87198) IR: Add `IrModuleFragment` link to `IrExternalPackageFragment`
- [`KT-87300`](https://youtrack.jetbrains.com/issue/KT-87300) IR: Replace module reference in `ir.declarations.createEmptyExternalPackageFragment` from `ModuleDescriptor` to `IrModuleFragment`
- [`KT-87438`](https://youtrack.jetbrains.com/issue/KT-87438) Make sure there are no duplicated names in lowerings on first and second stages
- [`KT-87732`](https://youtrack.jetbrains.com/issue/KT-87732) Don't use `IrBuiltInsOverDescriptors` in KLIB tool's `dump-ir-inlinable-functions` command
- [`KT-87962`](https://youtrack.jetbrains.com/issue/KT-87962) [IR] Add a checker that IrClass.superTypes is not empty
- [`KT-85896`](https://youtrack.jetbrains.com/issue/KT-85896) Type parameter is out of bounds (in setter) for a property with context parameters
- [`KT-85698`](https://youtrack.jetbrains.com/issue/KT-85698) Type parameter is out of bounds (in setter) for an extension property with type parameter
- [`KT-78960`](https://youtrack.jetbrains.com/issue/KT-78960) [FO] Limit static fake overrides generation for static functions
- [`KT-83236`](https://youtrack.jetbrains.com/issue/KT-83236) PeopleInSpace_mpp compilation error: The symbol table has been sealed

### JVM. Reflection

#### New Features

- [`KT-85904`](https://youtrack.jetbrains.com/issue/KT-85904) Reflection: add a way to get companion extension class

#### Performance Improvements

- [`KT-84354`](https://youtrack.jetbrains.com/issue/KT-84354) kotlin-reflect: replace usages of SoftLazy with regular safe-publication lazy
- [`KT-87408`](https://youtrack.jetbrains.com/issue/KT-87408) kotlin-reflect: KCallable.callBy on a bound `::copy` reference is ~55x slower since 2.2.20

#### Fixes

- [`KT-80710`](https://youtrack.jetbrains.com/issue/KT-80710) Reflection: change KCallable representation to avoid dependency on K1
- [`KT-89280`](https://youtrack.jetbrains.com/issue/KT-89280) NoSuchElementException in argumentsMakeSenseOnlyForMutableContainer for Java non-static inner class of generic outer class
- [`KT-87709`](https://youtrack.jetbrains.com/issue/KT-87709) Reflection: "invoke" in classes inheriting from suspend function types is loaded incorrectly
- [`KT-87948`](https://youtrack.jetbrains.com/issue/KT-87948) Reflection: AssertionError "Different tree sizes of bounds" on inheritance from Java raw class with self-recursive bound with method override
- [`KT-87581`](https://youtrack.jetbrains.com/issue/KT-87581) Reflection: extra abstract invoke() method shown for a Java class implementing a functional interface in the new implementation
- [`KT-87464`](https://youtrack.jetbrains.com/issue/KT-87464) Reflection: property accessor parameters are equal to property parameters in the new implementation
- [`KT-87369`](https://youtrack.jetbrains.com/issue/KT-87369) Reflection: KClass.members fails on Java class with self-referential raw type bounds
- [`KT-88145`](https://youtrack.jetbrains.com/issue/KT-88145) Investigate potential bug caused by incorrect statics handling in KClassImpl.Data#getInlineClassUnderlyingType
- [`KT-87367`](https://youtrack.jetbrains.com/issue/KT-87367) Reflection: memberFunctions fails on suspend-implementing classes
- [`KT-87365`](https://youtrack.jetbrains.com/issue/KT-87365) Reflection: java.util.LinkedList::class.supertypes contains a duplicate MutableList<E!> entry in the new implementation
- [`KT-87363`](https://youtrack.jetbrains.com/issue/KT-87363) Reflection: kotlin.Any constructor exposes `@IntrinsicCandidate` annotation in the new implementation
- [`KT-87356`](https://youtrack.jetbrains.com/issue/KT-87356) New Reflection: KotlinReflectionInternalError on .annotations of primitive/array constructors
- [`KT-86926`](https://youtrack.jetbrains.com/issue/KT-86926) Reflection: references to functions inherited in Java are not substituted in the new implementation
- [`KT-87308`](https://youtrack.jetbrains.com/issue/KT-87308) Reflection: KTypeParameter instances not equal for bound/unbound KProperty

### JavaScript

#### New Features

- [`KT-88294`](https://youtrack.jetbrains.com/issue/KT-88294) Add es2020 target for Kotlin/JS and Gradle plugin
- [`KT-88024`](https://youtrack.jetbrains.com/issue/KT-88024) KJS: Allow exporting interfaces nested in other interfaces to JavaScript
- [`KT-49791`](https://youtrack.jetbrains.com/issue/KT-49791) KJS: Provide option to export `dynamic` and `Any` as `unknown`
- [`KT-49795`](https://youtrack.jetbrains.com/issue/KT-49795) KJS IR: Allow typealias of exportable type to be exported
- [`KT-56493`](https://youtrack.jetbrains.com/issue/KT-56493) KJS: Export documentation to generated d.ts files

#### Fixes

- [`KT-87127`](https://youtrack.jetbrains.com/issue/KT-87127) KJS: `@JsPlainObject` factory contains duplicated parameters
- [`KT-87516`](https://youtrack.jetbrains.com/issue/KT-87516) [K/JS, K/Wasm] Companion objects coming from super interfaces are not initialized
- [`KT-86954`](https://youtrack.jetbrains.com/issue/KT-86954) K/JS: hashCode() of Double/Float disagrees with equals() for NaN values
- [`KT-89144`](https://youtrack.jetbrains.com/issue/KT-89144) [JS, Wasm] `static_init_called` field for static initializers is not initialized correctly
- [`KT-88463`](https://youtrack.jetbrains.com/issue/KT-88463) K/JS: Rewrite JsToStringGenerationVisitor to Kotlin
- [`KT-87753`](https://youtrack.jetbrains.com/issue/KT-87753) Add "scopes" decoding to ECMA426BasedSourceMapParser
- [`KT-88571`](https://youtrack.jetbrains.com/issue/KT-88571) K/JS: "llegalStateException: Internal error: cannot find external signature" caused by  `@Serializable` enum in 2.4.20-RC
- [`KT-88698`](https://youtrack.jetbrains.com/issue/KT-88698) [K/JS, tests] Remove obsolete DISABLE_IR_VISIBILITY_CHECKS from kotlinx-serialization test data
- [`KT-87774`](https://youtrack.jetbrains.com/issue/KT-87774) KJS: Refactor org.jetbrains.kotlin.js.testOld.utils.DirectiveTestUtils
- [`KT-87008`](https://youtrack.jetbrains.com/issue/KT-87008) K/JS: Preserve containers' names when lifting static members in StaticMembersLowering
- [`KT-17719`](https://youtrack.jetbrains.com/issue/KT-17719) JS: integer division by zero does not throw exception
- [`KT-86771`](https://youtrack.jetbrains.com/issue/KT-86771) KJS: `@JsExport`.Ignore on a Long member still triggers the -Xes-long-as-bigint compilation error
- [`KT-86273`](https://youtrack.jetbrains.com/issue/KT-86273) `@JsExport`.Ignore does not work properly with data classes properties
- [`KT-87901`](https://youtrack.jetbrains.com/issue/KT-87901) KJS: Perform WhileConditionFolding on IR instead of JS AST
- [`KT-88117`](https://youtrack.jetbrains.com/issue/KT-88117) K/JS: duplicated expression in generated JS comma expression for IrComposite
- [`KT-88015`](https://youtrack.jetbrains.com/issue/KT-88015) KJS: Exported sealed interfaces should not be implementable from TypeScript
- [`KT-87352`](https://youtrack.jetbrains.com/issue/KT-87352) Kotlin/JS: Ignore `typealias` inside `@JsExport`ed class
- [`KT-87793`](https://youtrack.jetbrains.com/issue/KT-87793) [Wasm, JS, IC] Invert IC cache invalidation keys list
- [`KT-86648`](https://youtrack.jetbrains.com/issue/KT-86648) [K/JS] Support companion blocks and extensions in TypeScript export
- [`KT-86934`](https://youtrack.jetbrains.com/issue/KT-86934) K/JS: Stack overflow when implementing interface with suspend fun using anonymous object
- [`KT-87377`](https://youtrack.jetbrains.com/issue/KT-87377) External classes can have field names colliding with minimized subclass field/method names
- [`KT-80188`](https://youtrack.jetbrains.com/issue/KT-80188) Design exporting of suspend lambdas into JS/TS
- [`KT-86437`](https://youtrack.jetbrains.com/issue/KT-86437) Incorrect handling of array destructuring with elision
- [`KT-86433`](https://youtrack.jetbrains.com/issue/KT-86433) JSCODE_ERROR for object destructuring assignment with default values
- [`KT-85990`](https://youtrack.jetbrains.com/issue/KT-85990) K/JS: Default parameter values ignored in `@JsStatic` suspend functions when class is exported
- [`KT-75176`](https://youtrack.jetbrains.com/issue/KT-75176) Add infrastructure for bundle size testing
- [`KT-73067`](https://youtrack.jetbrains.com/issue/KT-73067) Drop support for the legacy JS metadata from the metadata decompiler
- [`KT-13338`](https://youtrack.jetbrains.com/issue/KT-13338) JS: don't generate new class/object for empty enum entry

### Klibs

#### New Features

- [`KT-86081`](https://youtrack.jetbrains.com/issue/KT-86081) KLIBs: Support moving companion fun to parent class

#### Fixes

- [`KT-87965`](https://youtrack.jetbrains.com/issue/KT-87965) [Tests][Klibs] Allow generated version overload wrappers appear during pre-serialization lowerings as legitimate Klib ABI entries.
- [`KT-86687`](https://youtrack.jetbrains.com/issue/KT-86687) Split `KLIB_WRITING` metric, to`KLIB_IR_WRITING` and `KLIB_METADATA_WRITING`
- [`KT-87454`](https://youtrack.jetbrains.com/issue/KT-87454) KLIBs: Add PL tests for references with context parameters
- [`KT-77818`](https://youtrack.jetbrains.com/issue/KT-77818) [KLIB resolve] "The same 'unique_name=kotlin' found in more than one library" warning during metadata compilation
- [`KT-86572`](https://youtrack.jetbrains.com/issue/KT-86572) -Xheader-mode: header klib omits members provided by interface delegation (by)
- [`KT-84838`](https://youtrack.jetbrains.com/issue/KT-84838) Get rid of `ModuleDescriptor` from `IrModuleDeserializer`
- [`KT-54019`](https://youtrack.jetbrains.com/issue/KT-54019) [PL] Unimplemented FO: Adding interface to abstract parent + direct reference to absent member: JS build is successful, Native fails
- [`KT-87625`](https://youtrack.jetbrains.com/issue/KT-87625) Wasm: a callable reference bound to a null receiver equals an unbound reference
- [`KT-87443`](https://youtrack.jetbrains.com/issue/KT-87443) KLIB: Store metadata flags in manifest
- [`KT-87447`](https://youtrack.jetbrains.com/issue/KT-87447) Wasm: Rework internal presentation of KFunction to support references with context parameters
- [`KT-87445`](https://youtrack.jetbrains.com/issue/KT-87445) Native: Rework internal presentation of KFunction to support references with context parameters
- [`KT-87450`](https://youtrack.jetbrains.com/issue/KT-87450) Advance KlibAbiCompatibilityLevel to 2.5
- [`KT-87104`](https://youtrack.jetbrains.com/issue/KT-87104) Make org.jetbrains.kotlin.konan.file.File to be implementation detail of the compiler
- [`KT-86320`](https://youtrack.jetbrains.com/issue/KT-86320) [PL] Replacing unusable classifiers with Any? type may cause a signature clash
- [`KT-81947`](https://youtrack.jetbrains.com/issue/KT-81947) [Wasm] Klib backward and forward compatibility testing
- [`KT-84836`](https://youtrack.jetbrains.com/issue/KT-84836) Minimize usages of IrBuiltIns in the KotlinIrLinker

### Language Design

- [`KT-11968`](https://youtrack.jetbrains.com/issue/KT-11968) Research and prototype namespace-based solution for statics and static extensions
- [`KT-86090`](https://youtrack.jetbrains.com/issue/KT-86090) Stable release of "IntroduceAt": binary compatibility for optional parameters

### Libraries

#### New Features

- [`KT-6938`](https://youtrack.jetbrains.com/issue/KT-6938) Boolean.onTrue/onFalse, ifOrNull
- [`KT-88708`](https://youtrack.jetbrains.com/issue/KT-88708) Consider documenting that MutableMap.put keeps the stored key instance
- [`KT-88037`](https://youtrack.jetbrains.com/issue/KT-88037) kotlin-metadata-jvm: add readLenient()/readStrict() to KotlinModuleMetadata
- [`KT-39661`](https://youtrack.jetbrains.com/issue/KT-39661) Support TypeVariable.getGenericDeclaration for types created in KType.javaType in kotlin-stdlib
- [`KT-31305`](https://youtrack.jetbrains.com/issue/KT-31305) Support JUnit5-like assertions with message suppliers in kotlin-test
- [`KT-78462`](https://youtrack.jetbrains.com/issue/KT-78462) Annotate kotlin-stdlib-jvm with `@IgnorableReturnValue` where appropriate

#### Fixes

- [`KT-86151`](https://youtrack.jetbrains.com/issue/KT-86151) AtomicFU + companion block: compiler plugin generated invalid IR
- [`KT-86049`](https://youtrack.jetbrains.com/issue/KT-86049) Check serialization lib for the Companion Blocks and extensions
- [`KT-78174`](https://youtrack.jetbrains.com/issue/KT-78174) Improve kotlin.AutoCloseable documentation
- [`KT-87407`](https://youtrack.jetbrains.com/issue/KT-87407) Adapt kotlinx.metadata to changes in companion extension metadata format
- [`KT-87780`](https://youtrack.jetbrains.com/issue/KT-87780) K/Wasm: review coroutine-related APIs annotated by PublishedApi
- [`KT-87779`](https://youtrack.jetbrains.com/issue/KT-87779) K/Wasm: remove DoNotInlineOnFirstStage from coroutine-related APIs
- [`KT-78998`](https://youtrack.jetbrains.com/issue/KT-78998) K/Wasm: consider using qualified name instead of simple name for JS Error created for exceptions
- [`KT-78707`](https://youtrack.jetbrains.com/issue/KT-78707) Wasm: Throwable.stackTraceToString misses exception's class if it has no message or cause
- [`KT-86680`](https://youtrack.jetbrains.com/issue/KT-86680) Revert allEqualBy to == comparison once KT-86678 is fixed
- [`KT-57134`](https://youtrack.jetbrains.com/issue/KT-57134) Native: Consider introducing public ExceptionInInitializerError and NoClassDefFound
- [`KT-86760`](https://youtrack.jetbrains.com/issue/KT-86760) Consider deprecating the assertNotNull 3-arg overload accepting a lambda
- [`KT-87613`](https://youtrack.jetbrains.com/issue/KT-87613) Test Federation: Review domain Core Libraries Smoke & Contract tests
- [`KT-87849`](https://youtrack.jetbrains.com/issue/KT-87849) Set `@SinceKotlin`(2.3) for ignorability annotations
- [`KT-87312`](https://youtrack.jetbrains.com/issue/KT-87312) Stabilize RVC and allow `@IgnorableReturnValue` even in disabled checker mode
- [`KT-86956`](https://youtrack.jetbrains.com/issue/KT-86956) Remove the JS guards from allDistinct NaN tests once KT-86954 is fixed
- [`KT-52400`](https://youtrack.jetbrains.com/issue/KT-52400) Deprecate `@BuilderInference`
- [`KT-80605`](https://youtrack.jetbrains.com/issue/KT-80605) Rename MustUseReturnValue -> MustUseReturnValues
- [`KT-65526`](https://youtrack.jetbrains.com/issue/KT-65526) Rewrite builtins as expect-actual

### Native

- [`KT-88857`](https://youtrack.jetbrains.com/issue/KT-88857) Regression in LibraryPathFilter
- [`KT-88965`](https://youtrack.jetbrains.com/issue/KT-88965) [Native] Update minimal Xcode version
- [`KT-86678`](https://youtrack.jetbrains.com/issue/KT-86678) K/N: == on boxed Double/Float disagrees with equals() for NaN values

### Native. Build Infrastructure

- [`KT-87996`](https://youtrack.jetbrains.com/issue/KT-87996) K/N test task fails with "No cache found for library"

### Native. C Export

- [`KT-87538`](https://youtrack.jetbrains.com/issue/KT-87538) Kotlin/Native: hide companion blocks and extensions from C export

### Native. C and ObjC Import

- [`KT-81934`](https://youtrack.jetbrains.com/issue/KT-81934) Swift export should look up `SwiftBridge` and `SwiftName` when translating types from platform libraries
- [`KT-88374`](https://youtrack.jetbrains.com/issue/KT-88374) Kotlin/Native: deprecate CStructVar.Type with error
- [`KT-84721`](https://youtrack.jetbrains.com/issue/KT-84721) Generate C-interop KLIBs in previous ABI version in Kotlin 2.5.0

### Native. ObjC Export

- [`KT-87738`](https://youtrack.jetbrains.com/issue/KT-87738) Kotlin/Native: hide companion blocks and extensions from ObjC export
- [`KT-86199`](https://youtrack.jetbrains.com/issue/KT-86199) Scalability problems with PackageFragmentProviderImpl
- [`KT-88052`](https://youtrack.jetbrains.com/issue/KT-88052) [ObjCExport] AA: Property names are mangled differently and accessors do not match K1's output
- [`KT-87852`](https://youtrack.jetbrains.com/issue/KT-87852) [ObjCExport] AA: Replace name mangling of special method families
- [`KT-88153`](https://youtrack.jetbrains.com/issue/KT-88153) [ObjCExport] AA: documentation specifying annotation is not attached to receiver
- [`KT-87626`](https://youtrack.jetbrains.com/issue/KT-87626) `@ObjCEnum` interpreted as open enum in Swift

### Native. Platforms

- [`KT-78078`](https://youtrack.jetbrains.com/issue/KT-78078) Remove armv7k(watchosArm32) target

### Native. Runtime

- [`KT-88954`](https://youtrack.jetbrains.com/issue/KT-88954) [Native] Add -Wno-unknown-attributes to build of the runtime
- [`KT-88156`](https://youtrack.jetbrains.com/issue/KT-88156) Kotlin/Native: reduce number of runtime modules

### Native. Runtime. Memory

- [`KT-87963`](https://youtrack.jetbrains.com/issue/KT-87963) Kotlin/Native: review atomics usage in GlobalData.cpp

### Native. Swift Export

#### New Features

- [`KT-87556`](https://youtrack.jetbrains.com/issue/KT-87556) Kotlin/Native hide companion blocks and extensions from Swift Export
- [`KT-87802`](https://youtrack.jetbrains.com/issue/KT-87802) Swift Export: cross-language subtyping pt.2
- [`KT-87574`](https://youtrack.jetbrains.com/issue/KT-87574) Swift Export: unify error/exception throwing in sync & asynchronous bridges in cross-language inheritance
- [`KT-80632`](https://youtrack.jetbrains.com/issue/KT-80632) Swift export: symbol not found for cinterop protocol
- [`KT-72556`](https://youtrack.jetbrains.com/issue/KT-72556) Export top-level extension functions and properties from "main" package as Swift extensions
- [`KT-84191`](https://youtrack.jetbrains.com/issue/KT-84191) [Swift Export] export KDoc

#### Fixes

- [`KT-88584`](https://youtrack.jetbrains.com/issue/KT-88584) Swift Export: context parameter is dropped from export when declared in a flattened package: missing argument for parameter #1 in call
- [`KT-88042`](https://youtrack.jetbrains.com/issue/KT-88042) Swift Export [Inheritance]: Kotlin can't call an interface method on a Swift subclass that inherits the interface (ClassCastException)
- [`KT-87947`](https://youtrack.jetbrains.com/issue/KT-87947) Swift Export fails when inheriting Kotlin abstract class
- [`KT-86403`](https://youtrack.jetbrains.com/issue/KT-86403) Swift Export: Lean TypeInfo generation for swift inheritors
- [`KT-89025`](https://youtrack.jetbrains.com/issue/KT-89025) [Swift Export] generic type param with generic upper bound fails to compile
- [`KT-88999`](https://youtrack.jetbrains.com/issue/KT-88999) Swift export: leaf case of sealedType() is unreachable when the implementing class is not exported
- [`KT-86510`](https://youtrack.jetbrains.com/issue/KT-86510) [Swift Export. Tests] Exporting full API surface of kotlinx.coroutines causes runtime crashes
- [`KT-87875`](https://youtrack.jetbrains.com/issue/KT-87875) [Swift Export] Reverse bridges don't work with overloads
- [`KT-88386`](https://youtrack.jetbrains.com/issue/KT-88386) [Swift Export] SwiftExportConfig.errorTypeStrategy with SpecialType generates incompilable code
- [`KT-88809`](https://youtrack.jetbrains.com/issue/KT-88809) [Swift Export] intersection override with final implementation fails to compile
- [`KT-88740`](https://youtrack.jetbrains.com/issue/KT-88740) [Swift Export] declaration with internal OptIn fails to compile
- [`KT-88669`](https://youtrack.jetbrains.com/issue/KT-88669) [Swift Export] abstract class with multiple interfaces with same declaration fails to compile
- [`KT-87398`](https://youtrack.jetbrains.com/issue/KT-87398) Swift Export: issues with updating a user project
- [`KT-88739`](https://youtrack.jetbrains.com/issue/KT-88739) [Swift Export] function returning MutableList fails to export
- [`KT-87457`](https://youtrack.jetbrains.com/issue/KT-87457) Swift Export: NSInvalidArgumentException in _Kotlin_SwiftExport_wrapIntoExistential when Compose renders first iOS frame
- [`KT-88330`](https://youtrack.jetbrains.com/issue/KT-88330) KotlinExistential has no _createClassWrapperForExternalRCRef
- [`KT-88723`](https://youtrack.jetbrains.com/issue/KT-88723) [Swift Export] Support factory fun name based on suffix
- [`KT-88682`](https://youtrack.jetbrains.com/issue/KT-88682) Swift Export: can't export enum class implementing a sealed interface
- [`KT-88495`](https://youtrack.jetbrains.com/issue/KT-88495) [Swift Export] KDoc support: text after the `@see` identifier is dropped
- [`KT-88378`](https://youtrack.jetbrains.com/issue/KT-88378) [Swift Export] Special characters in interface names
- [`KT-88474`](https://youtrack.jetbrains.com/issue/KT-88474) [Swift Export] Typealias for inner interface is internal
- [`KT-88473`](https://youtrack.jetbrains.com/issue/KT-88473) [Swift Export] Can't implement sealedType function for leaf interface
- [`KT-88368`](https://youtrack.jetbrains.com/issue/KT-88368) [Swift Export] OptIn receiver extension in interface fails to compile
- [`KT-87697`](https://youtrack.jetbrains.com/issue/KT-87697) [Swift Export] conflicting ObjC marker names
- [`KT-87391`](https://youtrack.jetbrains.com/issue/KT-87391) [Swift Export] object returned from a function as an interface should be castable to a last public class, if that class conforms to that said interface
- [`KT-88224`](https://youtrack.jetbrains.com/issue/KT-88224) [Swift Export] `@escaping` is emitted on a variadic closure parameter, which Swift rejects
- [`KT-88370`](https://youtrack.jetbrains.com/issue/KT-88370) [Swift Export] Extension property on generic class with type param fails to compile
- [`KT-87791`](https://youtrack.jetbrains.com/issue/KT-87791) [Swift Export] OptIn interface member with default implementation fails to compile
- [`KT-88019`](https://youtrack.jetbrains.com/issue/KT-88019) Swift Export: parameter names in top level functions are not escaped
- [`KT-88171`](https://youtrack.jetbrains.com/issue/KT-88171) [Swift Export] enable KDoc export
- [`KT-87874`](https://youtrack.jetbrains.com/issue/KT-87874) [Swift Export] Generic start project param in closure fails to compile
- [`KT-87798`](https://youtrack.jetbrains.com/issue/KT-87798) [Swift Export] generic sealed interface fails to compile
- [`KT-87808`](https://youtrack.jetbrains.com/issue/KT-87808) [Swift Export] Nested generic fails to compile
- [`KT-87236`](https://youtrack.jetbrains.com/issue/KT-87236) [Swift Export] interface member with extension receiver fails to compile
- [`KT-85545`](https://youtrack.jetbrains.com/issue/KT-85545) [Swift Export] functional typealias with unavailable type
- [`KT-85544`](https://youtrack.jetbrains.com/issue/KT-85544) [Swift Export] property with hidden setter isn't exported

### Tools. BCV

- [`KT-85950`](https://youtrack.jetbrains.com/issue/KT-85950) Add support of "com.android.kotlin.multiplatform.library" plugin [ABI validation]
- [`KT-87551`](https://youtrack.jetbrains.com/issue/KT-87551) Bring kotlin.test API back after migrating to JUnit5

### Tools. Build Tools API

#### New Features

- [`KT-84401`](https://youtrack.jetbrains.com/issue/KT-84401) BTA: implement Kotlin/JS incremental compilation support
- [`KT-88018`](https://youtrack.jetbrains.com/issue/KT-88018) K2 Multiplatform incremental compilation of common code targeting JVM across multiple modules
- [`KT-87086`](https://youtrack.jetbrains.com/issue/KT-87086) Add Build Tools API entry point for Analysis API based TypeScript generation

#### Fixes

- [`KT-88835`](https://youtrack.jetbrains.com/issue/KT-88835) Wasm klib incremental compilation via daemon ignores configuration inputs
- [`KT-85464`](https://youtrack.jetbrains.com/issue/KT-85464) BTA: promote restricted arguments to ERROR
- [`KT-88776`](https://youtrack.jetbrains.com/issue/KT-88776) Remove deprecated JvmSnapshotBasedIncrementalCompilationConfiguration.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION
- [`KT-83937`](https://youtrack.jetbrains.com/issue/KT-83937) Promote shrunk-classpath-snapshot configuration deprecation to error
- [`KT-88299`](https://youtrack.jetbrains.com/issue/KT-88299) JS and Wasm klib compilation and linking operations silently ignore the IR_OUTPUT_DIR and X_INCLUDE compiler arguments
- [`KT-86169`](https://youtrack.jetbrains.com/issue/KT-86169) [BTA] `applyArgumentStrings` silently drops typed arguments and suppresses typed validation when used on the same builder
- [`KT-89050`](https://youtrack.jetbrains.com/issue/KT-89050) LOOKUP_TRACKER is not propagated to JS/Wasm incremental compilation
- [`KT-88298`](https://youtrack.jetbrains.com/issue/KT-88298) Dropped JS/Wasm compiler arguments are still present in the public BTA
- [`KT-88491`](https://youtrack.jetbrains.com/issue/KT-88491) WasmLinkingOperation and WasmKlibCompilationOperation do not check option availability for the compiler version in set()
- [`KT-88838`](https://youtrack.jetbrains.com/issue/KT-88838) JsKlibCompilationOperation.shouldCompileIncrementally() error message mentions the JVM config type
- [`KT-88478`](https://youtrack.jetbrains.com/issue/KT-88478) JsLinkingOperationImpl uses Options(JsKlibCompilationOperation::class) instead of JsLinkingOperation::class
- [`KT-88684`](https://youtrack.jetbrains.com/issue/KT-88684) WasmLinkingOperation cannot target wasm-wasi: no Wasm target option available in linking arguments
- [`KT-88381`](https://youtrack.jetbrains.com/issue/KT-88381) Argument-parsing warnings are lost when compiling via Build Tools API
- [`KT-88763`](https://youtrack.jetbrains.com/issue/KT-88763) Default classpath metadata to the common sources IC property
- [`KT-88777`](https://youtrack.jetbrains.com/issue/KT-88777) Remove deprecated JvmSnapshotBasedIncrementalCompilationConfiguration.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM
- [`KT-87522`](https://youtrack.jetbrains.com/issue/KT-87522) Separate enableUnsafeOptimizationsForMultiplatform for js and jvm
- [`KT-85791`](https://youtrack.jetbrains.com/issue/KT-85791) Remove compatibility wrapper for BTA IMPL 2.4.0 with lower versions
- [`KT-88348`](https://youtrack.jetbrains.com/issue/KT-88348) MessageCollector in IncrementalCompilerRunner drops diagnostics IDs
- [`KT-87525`](https://youtrack.jetbrains.com/issue/KT-87525) Add metrics for JVM metadata operations in Incremental Compilation
- [`KT-87848`](https://youtrack.jetbrains.com/issue/KT-87848) Guard JVM incremental metadata plumbing behind KMP metadata-on-classpath support
- [`KT-87743`](https://youtrack.jetbrains.com/issue/KT-87743) Make `KotlinCoreEnvironment.disposeApplicationEnvironment()` unnecessary for K2 BTA compilation
- [`KT-87822`](https://youtrack.jetbrains.com/issue/KT-87822) Clean up obsolete disk-based common metadata code after switching to IC-cache JVM metadata
- [`KT-87792`](https://youtrack.jetbrains.com/issue/KT-87792) [BTA] Handle removed arguments in BTA tests
- [`KT-87654`](https://youtrack.jetbrains.com/issue/KT-87654) [BTA] KotlinMetadataKlibCompilationOperation can silently produce a non-klib (legacy metadata) artifact
- [`KT-87653`](https://youtrack.jetbrains.com/issue/KT-87653) [BTA] Metadata operation reports a JVM-specific message for the restricted -d argument
- [`KT-87597`](https://youtrack.jetbrains.com/issue/KT-87597) [BTA] KotlinCompilationProcessFailedException causes LinkageError

### Tools. CLI

#### New Features

- [`KT-87671`](https://youtrack.jetbrains.com/issue/KT-87671) Disallow using of any lifecycle version greater than KotlinVersion.CURRENT

#### Performance Improvements

- [`KT-87868`](https://youtrack.jetbrains.com/issue/KT-87868) In-process compilation via CLI/Build Tools API pays a large ThreadMXBean.getCurrentThreadUserTime penalty on Linux: PerformanceManager.measureSideTime measures unconditionally around every class file read

#### Fixes

- [`KT-89211`](https://youtrack.jetbrains.com/issue/KT-89211) Show warning when running the compiler on JDK < 17
- [`KT-75990`](https://youtrack.jetbrains.com/issue/KT-75990) Deprecate all K1-related compiler flags and modes
- [`KT-89094`](https://youtrack.jetbrains.com/issue/KT-89094) Rich Errors: Create LF
- [`KT-88548`](https://youtrack.jetbrains.com/issue/KT-88548) False positive "Member overrides different '`@Throws`' filter" when a nativeMain class overrides a commonMain member carrying the same `@Throws` filter
- [`KT-88475`](https://youtrack.jetbrains.com/issue/KT-88475) IncrementalCompilationComponents instances are not being called anymore
- [`KT-86383`](https://youtrack.jetbrains.com/issue/KT-86383) Use a language feature for new inline variables scheme
- [`KT-87493`](https://youtrack.jetbrains.com/issue/KT-87493) kotlinc.bat crashes with ClassNotFoundException when installed to a path with spaces on ReFS (dev drive)
- [`KT-88222`](https://youtrack.jetbrains.com/issue/KT-88222) kotlinc/kotlin bash scripts fail to handle spaces in KOTLIN_HOME path, producing %20-encoded classpath entries on macOS/Linux
- [`KT-87373`](https://youtrack.jetbrains.com/issue/KT-87373) Warn on the use of the `kotlin` executable and recommend `kotlinr`
- [`KT-88026`](https://youtrack.jetbrains.com/issue/KT-88026) large arithmetic expression causes java.lang.NoClassDefFoundError instead of stackoverflow
- [`KT-85591`](https://youtrack.jetbrains.com/issue/KT-85591) Remove `Xklib-normalize-absolute-path` compiler argument
- [`KT-86626`](https://youtrack.jetbrains.com/issue/KT-86626) Add Compiler Argument for Context-bound Callable References
- [`KT-87826`](https://youtrack.jetbrains.com/issue/KT-87826) Remove -Xcontext-receivers compiler argument
- [`KT-68175`](https://youtrack.jetbrains.com/issue/KT-68175) Remove CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY
- [`KT-80348`](https://youtrack.jetbrains.com/issue/KT-80348) Expose 'XXLanguage' compiler argument as a normal argument
- [`KT-76498`](https://youtrack.jetbrains.com/issue/KT-76498) Implement JSON dumper for performance stats

### Tools. Compiler Plugin API

- [`KT-46917`](https://youtrack.jetbrains.com/issue/KT-46917) Dependencies of native compiler plugins are not resolved
- [`KT-86900`](https://youtrack.jetbrains.com/issue/KT-86900) FirSamResolver ignores plugin-generated abstract members when performing SAM conversion on fun interface

### Tools. Compiler Plugins

#### New Features

- [`KT-87731`](https://youtrack.jetbrains.com/issue/KT-87731) AtomicFU + companion block: gracefully handle delegated companion properties
- [`KT-87984`](https://youtrack.jetbrains.com/issue/KT-87984) AtomicFU: support companion blocks and extensions for JS target
- [`KT-87978`](https://youtrack.jetbrains.com/issue/KT-87978) AtomicFU: support companion blocks and extensions for K/N
- [`KT-87683`](https://youtrack.jetbrains.com/issue/KT-87683) Support functions and constructors with `@Builder` on Kotlin classes
- [`KT-87682`](https://youtrack.jetbrains.com/issue/KT-87682) Support for `@Singular` annotation with `@Builder` on Kotlin classes
- [`KT-88270`](https://youtrack.jetbrains.com/issue/KT-88270) Support for `@Singular` with `@Builder` and Guava collections on Kotlin classes
- [`KT-87886`](https://youtrack.jetbrains.com/issue/KT-87886) Support for`@Builder`.Default` annotations with `@Builder` on Kotlin classes
- [`KT-87685`](https://youtrack.jetbrains.com/issue/KT-87685) Support generics with `@Builder` on Kotlin classes
- [`KT-87686`](https://youtrack.jetbrains.com/issue/KT-87686) Implement minimal viable functionality with `@Builder` on Kotlin classes

#### Fixes

- [`KT-87277`](https://youtrack.jetbrains.com/issue/KT-87277) AtomicFU: allow only private atomic companion extension and member properties
- [`KT-88336`](https://youtrack.jetbrains.com/issue/KT-88336) Change Lombok diagnostics severity from warning to strong warning
- [`KT-84119`](https://youtrack.jetbrains.com/issue/KT-84119) Power-assert: "StringIndexOutOfBoundsException: Range [0, -2) out of bounds for length 884" in a scratch file
- [`KT-88179`](https://youtrack.jetbrains.com/issue/KT-88179) Power-assert: IrGenerationExtensionException (StringIndexOutOfBoundsException) when assert() wraps a context-parameter function call
- [`KT-54072`](https://youtrack.jetbrains.com/issue/KT-54072) [K2] [JAVA_TYPE_MISMATCH] error in case sortedSetOf() is used as a param of a filed annotated with @ Singular which type is raw SortedSet
- [`KT-87871`](https://youtrack.jetbrains.com/issue/KT-87871) ANNOTATION_HAS_NO_EFFECT reported for interfaces/annotation class with `@Log`, although the log property is generated
- [`KT-88505`](https://youtrack.jetbrains.com/issue/KT-88505) Lombok: `@Builder` generates a setter for a Java static field
- [`KT-88322`](https://youtrack.jetbrains.com/issue/KT-88322) Add `linuxArm32Hfp()` to Power Assert runtime
- [`KT-88803`](https://youtrack.jetbrains.com/issue/KT-88803) PowerAssert: Create tests for diagram without runtime
- [`KT-88337`](https://youtrack.jetbrains.com/issue/KT-88337) Handle Lombok AccessLevel.PROTECTED, MODULE, and PACKAGE correctly for Kotlin classes
- [`KT-86651`](https://youtrack.jetbrains.com/issue/KT-86651) Lombok: "Constructor without parameters is already defined" checker for `@NoArgConstructor`
- [`KT-87912`](https://youtrack.jetbrains.com/issue/KT-87912) AtomicFU: compiler plugin can't handle loop function invocation inside an inline function initializing top-level property
- [`KT-87784`](https://youtrack.jetbrains.com/issue/KT-87784) AFU: compiler plugin does not support inline extension properties on atomics
- [`KT-87612`](https://youtrack.jetbrains.com/issue/KT-87612) Test Federation: Review domain Compiler Plugins Smoke & Contract tests
- [`KT-75877`](https://youtrack.jetbrains.com/issue/KT-75877) PowerAssert: don't display value for anonymous object
- [`KT-88124`](https://youtrack.jetbrains.com/issue/KT-88124) DataFrame: failing test when removing workaround in FIR2IR
- [`KT-87889`](https://youtrack.jetbrains.com/issue/KT-87889) Use Lombok implementation to singularize words
- [`KT-85180`](https://youtrack.jetbrains.com/issue/KT-85180) AtomicFU/Native IR validation error
- [`KT-67440`](https://youtrack.jetbrains.com/issue/KT-67440) Atomicfu-compiler-plugin: incorrect initialization order of delegated properties
- [`KT-67839`](https://youtrack.jetbrains.com/issue/KT-67839) Atomicfu plugin: support transformation of minusAssign/plusAssign operators
- [`KT-87563`](https://youtrack.jetbrains.com/issue/KT-87563) Lombok: StackOverflow with `@SuperBuilder` and cyclic inheritance hierarchy

### Tools. Compiler plugins. Compose

- [`KT-88069`](https://youtrack.jetbrains.com/issue/KT-88069) [Compose Compiler] NoClassDefFoundError when invoking `@Composable` lambda directly
- [`KT-88028`](https://youtrack.jetbrains.com/issue/KT-88028) Compose compiler: IndexOutOfBoundsException in ComposableTargetAnnotationsTransformer when overriding a `@Composable` function with default parameter in another module
- [`KT-82808`](https://youtrack.jetbrains.com/issue/KT-82808) [CMP] IrLinkageError: Abstract function is not implemented

### Tools. Compiler plugins. Serialization

- [`KT-88540`](https://youtrack.jetbrains.com/issue/KT-88540) Warn when `@SerialInfo`/`@InheritableSerialInfo`-annotated annotation has inapplicable target
- [`KT-75702`](https://youtrack.jetbrains.com/issue/KT-75702) Support array content equality when deciding whether to encode a default value in kotlinx.serialization plugin

### Tools. Gradle

#### New Features

- [`KT-88526`](https://youtrack.jetbrains.com/issue/KT-88526) Initial implementation of Kotlin Archive in KGP

#### Performance Improvements

- [`KT-88084`](https://youtrack.jetbrains.com/issue/KT-88084) Each build leaves a strong reference to an instance of BuildMetricsService
- [`KT-69613`](https://youtrack.jetbrains.com/issue/KT-69613) Remove usages of `getCanonicalPath` and `getCanonicalFile` in plugins code

#### Fixes

- [`KT-87986`](https://youtrack.jetbrains.com/issue/KT-87986) Publish Swift Export metadata
- [`KT-88622`](https://youtrack.jetbrains.com/issue/KT-88622) Unconditionally deprecate direct integration with AGP
- [`KT-87971`](https://youtrack.jetbrains.com/issue/KT-87971) Make multiple loaded plugins detection compatible with project isolation
- [`KT-88290`](https://youtrack.jetbrains.com/issue/KT-88290) Make kotlin.user.home support relative paths
- [`KT-71968`](https://youtrack.jetbrains.com/issue/KT-71968) Setting custom `kotlin.project.persistent.dir` causes `NoSuchFileException: ./custom-cache-dir/sessions/kotlin-compiler-12437393244673679493.salive`
- [`KT-69336`](https://youtrack.jetbrains.com/issue/KT-69336) Support kotlin.project.persistent.dir that is outside of rootProject
- [`KT-88380`](https://youtrack.jetbrains.com/issue/KT-88380) Raise deprecation level to error for Kotlin source sets in Android extension
- [`KT-88850`](https://youtrack.jetbrains.com/issue/KT-88850) Add explicit xcodeIntegration and opt-in task wiring
- [`KT-84114`](https://youtrack.jetbrains.com/issue/KT-84114) Bump the minimal supported Gradle version to 8.14.x
- [`KT-87989`](https://youtrack.jetbrains.com/issue/KT-87989) Deprecate the legacy Swift Export DSL and provide migration diagnostics
- [`KT-77241`](https://youtrack.jetbrains.com/issue/KT-77241) KGP: Remove CleanData infrastructure
- [`KT-88853`](https://youtrack.jetbrains.com/issue/KT-88853) Derive Swift Export module export modes from KMP api and implementation dependencies
- [`KT-82426`](https://youtrack.jetbrains.com/issue/KT-82426) Improve publishing with cross compilation
- [`KT-87993`](https://youtrack.jetbrains.com/issue/KT-87993) Compile against Gradle API 9.7
- [`KT-87994`](https://youtrack.jetbrains.com/issue/KT-87994) Run Gradle integration tests against Gradle 9.7
- [`KT-87995`](https://youtrack.jetbrains.com/issue/KT-87995) Run Gradle functional tests using Gradle runtime 9.7
- [`KT-87840`](https://youtrack.jetbrains.com/issue/KT-87840) Isolated projects: "Project ... cannot access 'Project.extensions' functionality on another project" when configuring JS/Wasm targets (NodeJsPlugin/D8Plugin)
- [`KT-88192`](https://youtrack.jetbrains.com/issue/KT-88192) KMP Separate Compilation: separate compilation mode is incompatible with configuration cache
- [`KT-87726`](https://youtrack.jetbrains.com/issue/KT-87726) Run functional tests against the Gradle 9.6 release
- [`KT-87256`](https://youtrack.jetbrains.com/issue/KT-87256) KotlincOutputParser fails to parse compilation errors under Gradle when task path prefixes are enabled (Kotlin 2.4.20-Beta1)
- [`KT-87509`](https://youtrack.jetbrains.com/issue/KT-87509) Stop using Gradle Problems API severity field
- [`KT-87508`](https://youtrack.jetbrains.com/issue/KT-87508) Wire buildNeeded/buildDependent only for Gradle <9.5.0
- [`KT-86835`](https://youtrack.jetbrains.com/issue/KT-86835) [KGP] Migrate KotlinSecondaryVariantsDataSharing to kotlinx-serialization
- [`KT-86833`](https://youtrack.jetbrains.com/issue/KT-86833) [KGP] Migrate Apple/Swift-export/Native Gson usages to kotlinx-serialization; introduce FileSerializer
- [`KT-87724`](https://youtrack.jetbrains.com/issue/KT-87724) Fix failing SwiftPM Import Tests
- [`KT-87970`](https://youtrack.jetbrains.com/issue/KT-87970) Extend test coverage for multiple loaded plugins detection
- [`KT-87679`](https://youtrack.jetbrains.com/issue/KT-87679) Bump version of file-leak-detector used in KGP ITs
- [`KT-87911`](https://youtrack.jetbrains.com/issue/KT-87911) Report an actionable diagnostic when .kar files reach compilation classpaths in Kotlin 2.4.20
- [`KT-86435`](https://youtrack.jetbrains.com/issue/KT-86435) Replace `File.ensureParentDirsCreated()` with NIO Path `createDirectories()`
- [`KT-61225`](https://youtrack.jetbrains.com/issue/KT-61225) Turn "Kotlin Gradle plugin was loaded multiple times" warnings into error
- [`KT-87729`](https://youtrack.jetbrains.com/issue/KT-87729) Update KGP test to use jvm-test-suite
- [`KT-86040`](https://youtrack.jetbrains.com/issue/KT-86040) Run tests against the Gradle 9.6.0 release

### Tools. Gradle. JS

#### New Features

- [`KT-86731`](https://youtrack.jetbrains.com/issue/KT-86731) Integrate new Kotlin JS Browser Test pipeline with IDEA in debug mode
- [`KT-86715`](https://youtrack.jetbrains.com/issue/KT-86715) Add ability to configure Dev-server URL (or specification) to run browser tests from

#### Fixes

- [`KT-87641`](https://youtrack.jetbrains.com/issue/KT-87641) Having a few same js targets with configured tests leads to Cannot add task 'kotlinInstallPlaywrightBrowsers'
- [`KT-88709`](https://youtrack.jetbrains.com/issue/KT-88709) Deprecate NpmDependencyExtension
- [`KT-88718`](https://youtrack.jetbrains.com/issue/KT-88718) [TechDebt] Add dependsOnNpmTooling to simplify task dependency on NPM tooling
- [`KT-84740`](https://youtrack.jetbrains.com/issue/KT-84740) Promote deprecation of NpmApiExecution#resolveRootProject to ERROR
- [`KT-89052`](https://youtrack.jetbrains.com/issue/KT-89052) Add a cacheability annotation to EsmBundleKotlinJsTests
- [`KT-86112`](https://youtrack.jetbrains.com/issue/KT-86112) Promote KotlinJsIrTarget constructor deprecation from WARN to ERROR
- [`KT-85179`](https://youtrack.jetbrains.com/issue/KT-85179) Remove NodeJsExec.create
- [`KT-88597`](https://youtrack.jetbrains.com/issue/KT-88597) Fix failing Gradle Integration Tests Gradle Kotlin JS with Browser tests Linux
- [`KT-87502`](https://youtrack.jetbrains.com/issue/KT-87502) Validate customBrowserExecutable path in new JS test DSL
- [`KT-88504`](https://youtrack.jetbrains.com/issue/KT-88504) Gradle Integration Tests Gradle Kotlin JS with Browser tests (Windows) fails after Gradle 9.7 Bump
- [`KT-86708`](https://youtrack.jetbrains.com/issue/KT-86708) Add FUS events for new JS test API
- [`KT-84724`](https://youtrack.jetbrains.com/issue/KT-84724) NPM dependency versions do not invalidate KotlinWebpack task cache key in kotlin multiplatform project
- [`KT-86797`](https://youtrack.jetbrains.com/issue/KT-86797) Failed JS tests don't report errors in the output
- [`KT-86799`](https://youtrack.jetbrains.com/issue/KT-86799) Report browser environment to IJ
- [`KT-87501`](https://youtrack.jetbrains.com/issue/KT-87501) Throw an error if new JS test DSL is used with Wasm
- [`KT-87600`](https://youtrack.jetbrains.com/issue/KT-87600) Playwright install on multiple subprojects cause Gradle build to fail
- [`KT-53687`](https://youtrack.jetbrains.com/issue/KT-53687) Don't trigger npm and yarn related tasks if it not relevant for assemble

### Tools. Gradle. Multiplatform

- [`KT-60724`](https://youtrack.jetbrains.com/issue/KT-60724) Legacy dependencies are not reported if used in tests
- [`KT-80897`](https://youtrack.jetbrains.com/issue/KT-80897) Drop implementationMetadataConfigurationName from KGP
- [`KT-88890`](https://youtrack.jetbrains.com/issue/KT-88890) Stabilize MppIdeDependencyResolutionIT IDE resolution strict mode
- [`KT-88590`](https://youtrack.jetbrains.com/issue/KT-88590) NoSuchElementException thrown with new KMP compilation scheme
- [`KT-87790`](https://youtrack.jetbrains.com/issue/KT-87790) Kotlin 2.4.0 increases Gradle sync times
- [`KT-82417`](https://youtrack.jetbrains.com/issue/KT-82417) KMP Separate Compilation: missing fragment dependencies for custom darwin source set that mirrors appleMain
- [`KT-87946`](https://youtrack.jetbrains.com/issue/KT-87946) Flaky JSBrowserTestsIT

### Tools. Gradle. Native

#### New Features

- [`KT-84852`](https://youtrack.jetbrains.com/issue/KT-84852) Wire linkage package integration check with Swift Export's embedAndSign

#### Fixes

- [`KT-88813`](https://youtrack.jetbrains.com/issue/KT-88813) Delete native tasks API deprecated as ERROR
- [`KT-87632`](https://youtrack.jetbrains.com/issue/KT-87632) Native test: empty unnamed Output tab appears in Test Results when test process exits abnormally
- [`KT-76147`](https://youtrack.jetbrains.com/issue/KT-76147) Gradle Configuration Cache: 'compile*KotlinMetadata' caused invocation of 'Task.project' in 'commonizeCInterop'
- [`KT-87576`](https://youtrack.jetbrains.com/issue/KT-87576) Simplify SwiftPM lock file sync by moving logic from sync tasks into fetch task
- [`KT-87535`](https://youtrack.jetbrains.com/issue/KT-87535) Make SwiftPM fingerprint file structured
- [`KT-53477`](https://youtrack.jetbrains.com/issue/KT-53477) Native: Gradle plugin doesn't add compiler plugin transitive dependencies to compiler plugin classpath
- [`KT-85468`](https://youtrack.jetbrains.com/issue/KT-85468) Don't fail IDE import if the SwiftPM import pipeline has failed somewhere
- [`KT-87394`](https://youtrack.jetbrains.com/issue/KT-87394) Publishing fails with FileNotFoundException: iosArm64 KLIB missing when compilation skipped due to cross-compilation disabled in a project dependency
- [`KT-88008`](https://youtrack.jetbrains.com/issue/KT-88008) SwiftPM-import Gradle IT fails after watchosArm32 removal and platform-condition change
- [`KT-27683`](https://youtrack.jetbrains.com/issue/KT-27683) Most Kotlin compiler plugins cannot be used in Gradle MPP with Kotlin/Native
- [`KT-87873`](https://youtrack.jetbrains.com/issue/KT-87873) Deprecation warning on AndroidNative gradle DSL
- [`KT-87630`](https://youtrack.jetbrains.com/issue/KT-87630) Lenient import doesn't work with awaits introduced in KT-85933

### Tools. Gradle. Swift Export

- [`KT-88672`](https://youtrack.jetbrains.com/issue/KT-88672) Gracefully fail embedSwiftExport task if the minimal target version is less then iOS 18
- [`KT-88523`](https://youtrack.jetbrains.com/issue/KT-88523) Introduce the new export extension with a swift configuration block
- [`KT-89059`](https://youtrack.jetbrains.com/issue/KT-89059) Introduce a bridging layer between the legacy SwiftExportExtension and the new ExportExtension.SwiftExportConfiguration

### Tools. Gradle. Wasm

- [`KT-87082`](https://youtrack.jetbrains.com/issue/KT-87082) Support new playwright-based browser test DSL in wasmjs
- [`KT-88566`](https://youtrack.jetbrains.com/issue/KT-88566) K/Wasm: Webpack configuration generated after user defined chunks from the webpack.config.d directory
- [`KT-88285`](https://youtrack.jetbrains.com/issue/KT-88285) WasmJS tooling version overrides are ignored
- [`KT-88115`](https://youtrack.jetbrains.com/issue/KT-88115) "Your lockfile needs to be updated": Outdated bundled yarn.lock breaks kotlinWasmToolingSetup with --frozen-lockfile
- [`KT-86879`](https://youtrack.jetbrains.com/issue/KT-86879) K/Wasm: Update wasmtime to 47.0.0
- [`KT-87831`](https://youtrack.jetbrains.com/issue/KT-87831) NPM install on Windows fails if project path contains braces
- [`KT-87565`](https://youtrack.jetbrains.com/issue/KT-87565) Add validation for  kotlin.wasm.compilationMode Gradle property values

### Tools. Incremental Compile

- [`KT-88801`](https://youtrack.jetbrains.com/issue/KT-88801) Incremental compilation fails for a serializable class hierarchy in multiple files with private properties
- [`KT-86121`](https://youtrack.jetbrains.com/issue/KT-86121) Incremental compilation fails for a serializable sealed inheritance chain in multiple files
- [`KT-88762`](https://youtrack.jetbrains.com/issue/KT-88762) Make kotlin.internal.jvm.enableUnsafeOptimizationsForMultiplatform a user-facing Gradle property
- [`KT-62686`](https://youtrack.jetbrains.com/issue/KT-62686) K2: Common module sees platform declarations in case of MPP project incremental compilation

### Tools. Kapt

- [`KT-71883`](https://youtrack.jetbrains.com/issue/KT-71883) Kapt: generate Java sources directly instead of using JCTree
- [`KT-88559`](https://youtrack.jetbrains.com/issue/KT-88559) KAPT: 'com.sun.tools.java.util.Context' class can't be found ('tools.jar' is absent in the plugin classpath) on 28-ea JDK
- [`KT-85195`](https://youtrack.jetbrains.com/issue/KT-85195) KAPT: IntroducedAt is not supported in KAPT
- [`KT-80882`](https://youtrack.jetbrains.com/issue/KT-80882) K2 KAPT: Dagger fails to process code annotated with `@Composable`
- [`KT-70804`](https://youtrack.jetbrains.com/issue/KT-70804) Cleanup kapt implementations for K1 and K2

### Tools. Maven

- [`KT-83374`](https://youtrack.jetbrains.com/issue/KT-83374) CRI: it is not clear that CRI generation requires IC being enabled in Maven

### Tools. Scripts

- [`KT-87958`](https://youtrack.jetbrains.com/issue/KT-87958) K2 scripting: FirPropertyImpl transformation crash when using class from `@file`:Import-ed script in 2.4.0+
- [`KT-88453`](https://youtrack.jetbrains.com/issue/KT-88453) `VirtualFileScriptSource.text` leaks an unclosed file stream — script source file handle held until GC, blocks file deletion on Windows
- [`KT-87047`](https://youtrack.jetbrains.com/issue/KT-87047) Scripts: ClassNotFoundException for 'kotlin.script.experimental.dependencies.Repository' when compiling .main.kts with '-Xuse-fir-lt=false'
- [`KT-87041`](https://youtrack.jetbrains.com/issue/KT-87041) Scripting: classpath extraction fails to extract stdlib from kotlinc

### Tools. Statistics (FUS)

- [`KT-88758`](https://youtrack.jetbrains.com/issue/KT-88758) Enable FUS for all Gradle IT tests

### Analysis API

- [`KT-83867`](https://youtrack.jetbrains.com/issue/KT-83867) OVERLOAD_RESOLUTION_AMBIGUITY false positive with assertEquals in IJ repo 
- [`KT-83723`](https://youtrack.jetbrains.com/issue/KT-83723) [Analysis API] Enable experimental KDoc resolver by default
- [`KT-83388`](https://youtrack.jetbrains.com/issue/KT-83388) Analysis API: properly support KMP in KotlinPackageProvider
- [`KT-65683`](https://youtrack.jetbrains.com/issue/KT-65683) Analysis API: Dangling file session creation causes a `computeIfAbsent` contract violation

### Analysis API. Code Compilation

- [`KT-78946`](https://youtrack.jetbrains.com/issue/KT-78946) Evaluation of variable with local class in type parameter leads to InventNamesForLocalClasses exception

### Analysis API. FIR

- [`KT-68260`](https://youtrack.jetbrains.com/issue/KT-68260) K2 AA: InvalidFirElementTypeException “For CALLABLE_REFERENCE_EXPRESSION with text `::lam1`, unexpected element of type: no element found” with illegal callable reference call
- [`KT-70896`](https://youtrack.jetbrains.com/issue/KT-70896) AA: False positive deprecation warning with override of built-in method in JDK mapped class
- [`KT-84711`](https://youtrack.jetbrains.com/issue/KT-84711) K2 IDE sometimes loses FIR plugin-generated declarations after file changes
- [`KT-84625`](https://youtrack.jetbrains.com/issue/KT-84625) Analysis API: collectDesignationPath fails for nested classes inside plugin-generated top-level classes
- [`KT-84596`](https://youtrack.jetbrains.com/issue/KT-84596) Improve K2 Jooq completion performance
- [`KT-84525`](https://youtrack.jetbrains.com/issue/KT-84525) KaValueParameterSymbol#getHasSynthesizedName returns false for FirDeclarationOrigin.SubstitutionOverride.DeclarationSite
- [`KT-83546`](https://youtrack.jetbrains.com/issue/KT-83546) Kotlin analysis reach ClsCustomNavigationPolicy
- [`KT-84259`](https://youtrack.jetbrains.com/issue/KT-84259) Move CommonDefaultImportsProvider to the frontend independent module
- [`KT-82945`](https://youtrack.jetbrains.com/issue/KT-82945) Analysis API: KotlinIllegalArgumentExceptionWithAttachments: Expected FirResolvedTypeRef with ConeKotlinType but was FirUserTypeRefImpl
- [`KT-71135`](https://youtrack.jetbrains.com/issue/KT-71135) AA: exception from sealed inheritors checker when `analyzeCopy`

### Analysis API. Infrastructure

#### Fixes

- [`KT-83905`](https://youtrack.jetbrains.com/issue/KT-83905) Analysis API: Improve UX with test data
- [`KT-84913`](https://youtrack.jetbrains.com/issue/KT-84913) Extract compiler classes used by the PSI & Analysis API to a separate module
- [`KT-64986`](https://youtrack.jetbrains.com/issue/KT-64986) Analysis API: Implement Analysis API tests for different KMP Platforms
- [`KT-84776`](https://youtrack.jetbrains.com/issue/KT-84776) The test data manager misses the redundancy check in the update mode
- [`KT-84962`](https://youtrack.jetbrains.com/issue/KT-84962) The test data manager misses -ea flag
- [`KT-84388`](https://youtrack.jetbrains.com/issue/KT-84388) Preserve the EOF status in the test data manager to avoid extra changes
- [`KT-84362`](https://youtrack.jetbrains.com/issue/KT-84362) Analysis API tests produce many warnings due to "not yet loaded registry"
- [`KT-84279`](https://youtrack.jetbrains.com/issue/KT-84279) Test Data Manager fails on a clean build
- [`KT-83913`](https://youtrack.jetbrains.com/issue/KT-83913) Exclude compiler-based Analysis API tests from Git tracking
- [`KT-80379`](https://youtrack.jetbrains.com/issue/KT-80379) Extract per-module test generators for AA tests
- [`KT-84120`](https://youtrack.jetbrains.com/issue/KT-84120) Move CLI modules out of kotlin-compiler-fe10-for-ide
- [`KT-83200`](https://youtrack.jetbrains.com/issue/KT-83200) Track external dependencies of the Analysis API modules

### Analysis API. Light Classes

- [`KT-82434`](https://youtrack.jetbrains.com/issue/KT-82434) Light classes should prefer enum entries to properties
- [`KT-84200`](https://youtrack.jetbrains.com/issue/KT-84200) SLC: return type is not boxed for delegated methods with generic original method
- [`KT-72451`](https://youtrack.jetbrains.com/issue/KT-72451) "CCE: class PsiPrimitiveType cannot be cast to class PsiClassType" with same-named enum class and typealias

### Analysis API. PSI

- [`KT-84715`](https://youtrack.jetbrains.com/issue/KT-84715) removeModifier doesn't delete whitespaces around the removed modifier
- [`KT-84564`](https://youtrack.jetbrains.com/issue/KT-84564) KtEnumEntry.delete deletes semicolon
- [`KT-84781`](https://youtrack.jetbrains.com/issue/KT-84781) Use computed properties in KotlinElementTypeProviderImpl
- [`KT-83846`](https://youtrack.jetbrains.com/issue/KT-83846) Set up guidelines for PSI
- [`KT-84135`](https://youtrack.jetbrains.com/issue/KT-84135) Deprecate KtSelfType

### Analysis API. Providers and Caches

- [`KT-82731`](https://youtrack.jetbrains.com/issue/KT-82731) Analysis API: Limit granular tree change processing to a few files
- [`KT-79234`](https://youtrack.jetbrains.com/issue/KT-79234) Analysis API: Usage of `asMap()` on Caffeine caches bypasses stats counters
- [`KT-74090`](https://youtrack.jetbrains.com/issue/KT-74090) Analysis API: Support dumb mode (restricted analysis)

### Analysis API. Standalone

- [`KT-83801`](https://youtrack.jetbrains.com/issue/KT-83801) Nested typealiases are not correctly indexed in standalone mode

### Analysis API. Stubs and Decompilation

- [`KT-85371`](https://youtrack.jetbrains.com/issue/KT-85371) StackOverflowError from LLKotlinStubBasedLibrarySymbolProvider and StubBasedClassDeserialization
- [`KT-83935`](https://youtrack.jetbrains.com/issue/KT-83935) Support KDoc loading in decompiled stubs

### Analysis API. Surface

#### New Features

- [`KT-85239`](https://youtrack.jetbrains.com/issue/KT-85239) Streaming version of collectDiagnostics()
- [`KT-82519`](https://youtrack.jetbrains.com/issue/KT-82519) Automatically recognize the appropriate analysis mode for in-memory file copies based on their content
- [`KT-73534`](https://youtrack.jetbrains.com/issue/KT-73534) SAM method API
- [`KT-82993`](https://youtrack.jetbrains.com/issue/KT-82993) Support explicit backing fields in the Analysis API

#### Fixes

- [`KT-79186`](https://youtrack.jetbrains.com/issue/KT-79186) KtCompletionExtensionCandidateChecker does not work for extensions when using callable references of a type
- [`KT-83759`](https://youtrack.jetbrains.com/issue/KT-83759) Analysis API: Mark platform interface APIs with `@KaPlatformInterface`
- [`KT-83921`](https://youtrack.jetbrains.com/issue/KT-83921) Extend KaKDocProvider to read Kdoc from KLIB metadata
- [`KT-77426`](https://youtrack.jetbrains.com/issue/KT-77426) KaFirCompilerFacility uses an arbitrary JVM counterpart for common sources
- [`KT-84737`](https://youtrack.jetbrains.com/issue/KT-84737) KaCallableSymbol#directlyOverriddenSymbols doesn't work for java overrides of kotlin properties
- [`KT-84621`](https://youtrack.jetbrains.com/issue/KT-84621) Migrate symbol tests to ManagedTest properly
- [`KT-80575`](https://youtrack.jetbrains.com/issue/KT-80575) KaFirJavaInteroperabilityComponent#getJavaGetterName should not throw exception on incomplete code
- [`KT-84397`](https://youtrack.jetbrains.com/issue/KT-84397) KtDefaultAnnotationArgumentReference should return only results with value name
- [`KT-84804`](https://youtrack.jetbrains.com/issue/KT-84804) buildSubstitutor does not work correctly with Java type parameters
- [`KT-84389`](https://youtrack.jetbrains.com/issue/KT-84389) Cover references with ABI and documentation checks
- [`KT-57042`](https://youtrack.jetbrains.com/issue/KT-57042) K2, Analysis API: KaJavaInteroperabilityComponent#callableSymbol returns null for a Java getter implementing Kotlin property
- [`KT-80856`](https://youtrack.jetbrains.com/issue/KT-80856) Analysis API: `analysisContextModule` incorrectly determines the module of an original file when used for dangling file context assignment
- [`KT-84363`](https://youtrack.jetbrains.com/issue/KT-84363) AA, isUsedAsExpression: Unhandled Non-KtExpression parent of KtExpression: class org.jetbrains.kotlin.psi.KtContractEffect
- [`KT-70476`](https://youtrack.jetbrains.com/issue/KT-70476) Analysis API: "KtDefaultAnnotationArgumentReference.resolveToSymbols" does not work in FIR implementation
- [`KT-68499`](https://youtrack.jetbrains.com/issue/KT-68499) Split KtDefaultAnnotationArgumentReference on K1 and K2 implementation
- [`KT-70521`](https://youtrack.jetbrains.com/issue/KT-70521) Analysis API: Impossible to distinguish between 'iterator' operator calls dispatched with imports from objects
- [`KT-77669`](https://youtrack.jetbrains.com/issue/KT-77669) Context arguments are missed on implicit invoke calls
- [`KT-77670`](https://youtrack.jetbrains.com/issue/KT-77670) resolveToCall: extensionReceiver is incorrectly chosed due to a conflict with context parameters for an implicit `invoke` call
- [`KT-68633`](https://youtrack.jetbrains.com/issue/KT-68633) K2 AA: IAE "Expected class KaClassSymbol instead of class KaFirEnumEntrySymbol" with enum entry initializer
- [`KT-83777`](https://youtrack.jetbrains.com/issue/KT-83777) Analysis API: The resolution scope of a context module accepts elements from associated dangling files
- [`KT-82571`](https://youtrack.jetbrains.com/issue/KT-82571) No expected type for overridden property without explicit type
- [`KT-83223`](https://youtrack.jetbrains.com/issue/KT-83223) Support "Explicit context arguments" in the Analysis API
- [`KT-65186`](https://youtrack.jetbrains.com/issue/KT-65186) K2: Analysis API: KtExpressionTypeProvider.getExpectedType works incorrectly for the right hand side of assignment expressions
- [`KT-76011`](https://youtrack.jetbrains.com/issue/KT-76011) `KaFirNamedClassSymbol#companionObject` doesn't provide generated objects generated by compiled plugins
- [`KT-73290`](https://youtrack.jetbrains.com/issue/KT-73290) Analysis API: Improve the architecture of content scopes and resolution scopes

### Backend. J2KLIB

- [`KT-86368`](https://youtrack.jetbrains.com/issue/KT-86368) [JKLIB] MetadataJVMModuleDeserializer tries to deserialize all symbols
- [`KT-86367`](https://youtrack.jetbrains.com/issue/KT-86367) [JKLIB] kotlin.Cloneable built-in class not found
- [`KT-84877`](https://youtrack.jetbrains.com/issue/KT-84877) [J2KLIB] Remove withKotlinBuiltinsHack present in JKlibIrLinker.kt
- [`KT-85846`](https://youtrack.jetbrains.com/issue/KT-85846) Tests failing with fake override property missing accessors or backing field
- [`KT-85717`](https://youtrack.jetbrains.com/issue/KT-85717)  IllegalStateException due to already bound symbol 
- [`KT-86204`](https://youtrack.jetbrains.com/issue/KT-86204) [JKlib] Propagate private members from dependecies in the IR tree

### Backend. Native. Debug

- [`KT-83804`](https://youtrack.jetbrains.com/issue/KT-83804) Native: debug information generator converts relative paths to absolute ones

### Backend. Wasm

- [`KT-83162`](https://youtrack.jetbrains.com/issue/KT-83162) K/Wasm: renaming temporary and synthetic variables in the Chrome debugger
- [`KT-85008`](https://youtrack.jetbrains.com/issue/KT-85008) Develop and publish a demo app using an early version of the component model support
- [`KT-76205`](https://youtrack.jetbrains.com/issue/KT-76205) K/Wasm: stabilize and turn on incremental compilation by default
- [`KT-83728`](https://youtrack.jetbrains.com/issue/KT-83728) [Wasm] Invalid Ir type while suspend call with blocked if null comprehansion
- [`KT-81637`](https://youtrack.jetbrains.com/issue/KT-81637) K/JS/Wasm interop: Inconsistent behavior of `is`/`as` operations for `JsReference<C>` and `C`
- [`KT-65030`](https://youtrack.jetbrains.com/issue/KT-65030) K/Wasm: memory allocator for Component Model ABI
- [`KT-83607`](https://youtrack.jetbrains.com/issue/KT-83607) WasmJS: Production build eliminates 'else if' branch when 'else' is not wrapped with curly braces 
- [`KT-82803`](https://youtrack.jetbrains.com/issue/KT-82803) Kotlin/WASM: Failed to compile the doResume function with if inside catch block
- [`KT-83800`](https://youtrack.jetbrains.com/issue/KT-83800) [Wasm] Closed world per-module compilation

### Compiler

#### New Features

- [`KT-84487`](https://youtrack.jetbrains.com/issue/KT-84487) "-Xcollection-literals" compiler flag
- [`KT-84072`](https://youtrack.jetbrains.com/issue/KT-84072) Collection literals: treat visibility of `of` during resolve correctly
- [`KT-83165`](https://youtrack.jetbrains.com/issue/KT-83165) Collection literals: treat Deprecated(HIDDEN) operators `of` reasonably
- [`KT-80500`](https://youtrack.jetbrains.com/issue/KT-80500) Collection literals: Analyze `ConeCollectionLiteralAtom` in cases their expected type is not fully known
- [`KT-80491`](https://youtrack.jetbrains.com/issue/KT-80491) Implement fallback mechanism for collection literals
- [`KT-80490`](https://youtrack.jetbrains.com/issue/KT-80490) Implement overload resolution mechanism for collection literals
- [`KT-84298`](https://youtrack.jetbrains.com/issue/KT-84298) K2: Generate IR for Companion Blocks & Extensions
- [`KT-83765`](https://youtrack.jetbrains.com/issue/KT-83765) Make -Xsuppress-version-warnings have a diagnostic ID
- [`KT-84484`](https://youtrack.jetbrains.com/issue/KT-84484) Companion Extensions Analysis & Resolution 
- [`KT-84199`](https://youtrack.jetbrains.com/issue/KT-84199) Implement DontMakeExplicitNullableJavaTypeArgumentsFlexible feature
- [`KT-84319`](https://youtrack.jetbrains.com/issue/KT-84319) Add JVM target bytecode version 26
- [`KT-84297`](https://youtrack.jetbrains.com/issue/KT-84297) Serialize & Deserialize Companion Block Declarations & Extensions to/from Metadata
- [`KT-84288`](https://youtrack.jetbrains.com/issue/KT-84288) Companion Blocks Analysis & Resolution 
- [`KT-84286`](https://youtrack.jetbrains.com/issue/KT-84286) Parse Companion Blocks & Extensions
- [`KT-84292`](https://youtrack.jetbrains.com/issue/KT-84292) Enforce Companion Blocks & Extensions Language Feature during Resolution
- [`KT-84291`](https://youtrack.jetbrains.com/issue/KT-84291) Companion Blocks & Extensions Checkers
- [`KT-84290`](https://youtrack.jetbrains.com/issue/KT-84290) Callable References to Companion Block Declarations & Extensions
- [`KT-84287`](https://youtrack.jetbrains.com/issue/KT-84287) Build Raw FIR for Companion Blocks & Extensions
- [`KT-73256`](https://youtrack.jetbrains.com/issue/KT-73256) Implement `all` meta-target for annotations
- [`KT-66344`](https://youtrack.jetbrains.com/issue/KT-66344) K1 & K2: False positive WRONG_NUMBER_OF_TYPE_ARGUMENTS in callable reference to inner class member
- [`KT-76766`](https://youtrack.jetbrains.com/issue/KT-76766) Warning is missing for wrong subclass checking 
- [`KT-74049`](https://youtrack.jetbrains.com/issue/KT-74049) Introduce special override rule to allow overriding T! with T & Any

#### Performance Improvements

- [`KT-84941`](https://youtrack.jetbrains.com/issue/KT-84941) Skip linking klibs for generating ObjC headers
- [`KT-80367`](https://youtrack.jetbrains.com/issue/KT-80367) Reduce memory consumption of DevirtualizationAnalysis 
- [`KT-84412`](https://youtrack.jetbrains.com/issue/KT-84412) iOS release build time dramatically increases with 2.3.20-Beta2 compared to 2.3.10
- [`KT-82559`](https://youtrack.jetbrains.com/issue/KT-82559) linkDebugTest*X64 tasks are slower for Kotlin 2.3 than for 2.2
- [`KT-84095`](https://youtrack.jetbrains.com/issue/KT-84095) Improve Unit tail-call optimization to support inline generic functions similar to `suspendCoroutine`

#### Fixes

- [`KT-77726`](https://youtrack.jetbrains.com/issue/KT-77726) Move FirUnusedExpressionChecker to the default checkers list
- [`KT-84841`](https://youtrack.jetbrains.com/issue/KT-84841) Collection literals: Drop special treatment of `when` with expected type
- [`KT-84732`](https://youtrack.jetbrains.com/issue/KT-84732) Collection literals: "Expected `FirCollectionLiteralImpl` to be resolved" in RHS of equality operator
- [`KT-84675`](https://youtrack.jetbrains.com/issue/KT-84675) Collection literals: 'Not singleClassifierType superType: TypeVariable(S)' in PCLA
- [`KT-84559`](https://youtrack.jetbrains.com/issue/KT-84559) `@OptIn` on collection literal and context-sensitive does not work
- [`KT-84547`](https://youtrack.jetbrains.com/issue/KT-84547) Collection literals: "Expected expression 'FirCollectionLiteralImpl' to be resolved" in elvis expression
- [`KT-80489`](https://youtrack.jetbrains.com/issue/KT-80489) Collection literals: experimental version (Frontend)
- [`KT-84768`](https://youtrack.jetbrains.com/issue/KT-84768) [K/N] switch thread state in `org_jetbrains_skia_Canvas__1nDrawPicture` to workaround the deadlock
- [`KT-52673`](https://youtrack.jetbrains.com/issue/KT-52673) Don't report deprecation warning/error on last segments of imports
- [`KT-83988`](https://youtrack.jetbrains.com/issue/KT-83988) Remove extraneous POP+GETSTATIC Unit for calls of Unit-returning suspend functions
- [`KT-77584`](https://youtrack.jetbrains.com/issue/KT-77584) Support scripts built from LT in scripting API
- [`KT-80113`](https://youtrack.jetbrains.com/issue/KT-80113) Consider improving diagnostic messages related to `==`/`===`/`is`/`as`
- [`KT-85392`](https://youtrack.jetbrains.com/issue/KT-85392) Native: concurrency issues in per-file caches
- [`KT-83938`](https://youtrack.jetbrains.com/issue/KT-83938) Missing Tail call optimization in reference classes returning Unit
- [`KT-78432`](https://youtrack.jetbrains.com/issue/KT-78432) No-arg constructor should be generated for regular classes with a value class parameter in case of JvmExposeBoxed
- [`KT-86130`](https://youtrack.jetbrains.com/issue/KT-86130) False positive UNINITIALIZED_ENUM_COMPANION on LV 2.3 and lower
- [`KT-84860`](https://youtrack.jetbrains.com/issue/KT-84860) False positive UNINITIALIZED_ENUM_COMPANION in enum access with explicit receiver in enum initializer when enum class has a companion
- [`KT-86210`](https://youtrack.jetbrains.com/issue/KT-86210) Update -Xannotation-default-target CLI parameter doc
- [`KT-80590`](https://youtrack.jetbrains.com/issue/KT-80590) Drop language version 1.9 for JVM
- [`KT-85948`](https://youtrack.jetbrains.com/issue/KT-85948) Contracts in 2.4 stdlib is not compatible with 2.3 compiler
- [`KT-82123`](https://youtrack.jetbrains.com/issue/KT-82123) KDoc: references that goes after markdown blocks don't have links
- [`KT-85957`](https://youtrack.jetbrains.com/issue/KT-85957) Contract on function is getting discarded if any of effect declarations is unknown
- [`KT-84931`](https://youtrack.jetbrains.com/issue/KT-84931) Incorrect type nullability in SAM super type in anonymous class-based SAM conversion
- [`KT-83766`](https://youtrack.jetbrains.com/issue/KT-83766) K2: Wrong sourcePsi is set for `SymbolPsiLiteral` in SLC for annotation arguments referencing a const val
- [`KT-85062`](https://youtrack.jetbrains.com/issue/KT-85062) Deprecate language version 2.1
- [`KT-79729`](https://youtrack.jetbrains.com/issue/KT-79729) Kotlin should warn about orphaned block expressions, e.g. // if () \n { .... }
- [`KT-83884`](https://youtrack.jetbrains.com/issue/KT-83884) The unused return value checker doesn't report a coercion from reference to Unit
- [`KT-85825`](https://youtrack.jetbrains.com/issue/KT-85825) Context parameter lambda loses context type when wrapped in nested `run` blocks
- [`KT-37179`](https://youtrack.jetbrains.com/issue/KT-37179) false-positive shadowing warning on local and member extension functions in presence of member extension property with invoke operator
- [`KT-84366`](https://youtrack.jetbrains.com/issue/KT-84366) Invalid name for captured `this` in bytecode
- [`KT-84960`](https://youtrack.jetbrains.com/issue/KT-84960) Property contract leaks unsubstituted type parameter in smart cast 
- [`KT-84281`](https://youtrack.jetbrains.com/issue/KT-84281) Standalone typealias-to-object qualifier allows type arguments and has type `Unit` in this case
- [`KT-84280`](https://youtrack.jetbrains.com/issue/KT-84280) Standalone `Unit` qualifier allows type arguments: `Unit<Any>`
- [`KT-84380`](https://youtrack.jetbrains.com/issue/KT-84380) Type alias to non-generic class can have (arbitrary number of) type arguments in LHS of `::class`
- [`KT-84190`](https://youtrack.jetbrains.com/issue/KT-84190) Implement basic functionality for returnsResultOf contract
- [`KT-83920`](https://youtrack.jetbrains.com/issue/KT-83920) False positive "modifier 'value' is not applicable to 'local variable'" with soft keyword in positional destructuring (square bracket) declaration
- [`KT-84991`](https://youtrack.jetbrains.com/issue/KT-84991) Improve `Argument type mismatch` diagnostics
- [`KT-82216`](https://youtrack.jetbrains.com/issue/KT-82216) Sanitize '.kotlin_module' filename
- [`KT-85354`](https://youtrack.jetbrains.com/issue/KT-85354) checkPsiTypeConsistency: add psi text attachments
- [`KT-85479`](https://youtrack.jetbrains.com/issue/KT-85479) Improve diagnostic messages for upper bound violations
- [`KT-84585`](https://youtrack.jetbrains.com/issue/KT-84585) Upper bound violated warning for expansion of type alias in LHS
- [`KT-84924`](https://youtrack.jetbrains.com/issue/KT-84924) Native: stdlib-cache.lock used by mulitple processes
- [`KT-85244`](https://youtrack.jetbrains.com/issue/KT-85244) False positive DUPLICATE_BRANCH_CONDITION_IN_WHEN with guard condition
- [`KT-85487`](https://youtrack.jetbrains.com/issue/KT-85487) Investigate why WrapContinuationForTailCallFunctions does not work in Android Test
- [`KT-59633`](https://youtrack.jetbrains.com/issue/KT-59633) K2: Implement running AndroidRunner tests with FIR
- [`KT-76237`](https://youtrack.jetbrains.com/issue/KT-76237) Store File-level annotations in KLIB metadata separately
- [`KT-84198`](https://youtrack.jetbrains.com/issue/KT-84198) Support multiple embedded .let-like calls with returnsResultOf contract
- [`KT-84196`](https://youtrack.jetbrains.com/issue/KT-84196) Handle multiple entry/exit points for returnsResultOf functions
- [`KT-84195`](https://youtrack.jetbrains.com/issue/KT-84195) Handle function references in returnsResultOf
- [`KT-85162`](https://youtrack.jetbrains.com/issue/KT-85162) Introduce diagnostics to refine numeric types casting
- [`KT-80060`](https://youtrack.jetbrains.com/issue/KT-80060) False positive REDUNDANT_CALL_OF_CONVERSION_METHOD in case of overloads
- [`KT-85289`](https://youtrack.jetbrains.com/issue/KT-85289) False-positive smartcast from == with type parameter based variable
- [`KT-83890`](https://youtrack.jetbrains.com/issue/KT-83890) return-value-checker: false positive "Unused return value of 'context'" on kotlin.context() functions
- [`KT-84106`](https://youtrack.jetbrains.com/issue/KT-84106) False negative "NON_EXHAUSTIVE_WHEN": "NoWhenBranchMatchedException" at runtime with sealed and platform type
- [`KT-84717`](https://youtrack.jetbrains.com/issue/KT-84717) Provide information for qualified expressions that might be replaced with context-sensitive simple names in IDE mode
- [`KT-38983`](https://youtrack.jetbrains.com/issue/KT-38983) Bridges are not generated in interfaces, which leads to counterintuitive behavior of java.lang.reflect.Proxy
- [`KT-85005`](https://youtrack.jetbrains.com/issue/KT-85005) Consider `all:` target in the checker of repeatable annotations
- [`KT-85210`](https://youtrack.jetbrains.com/issue/KT-85210) Enabling -XXLanguage:+IntrinsicConstEvaluation breaks highlighting on some broken code
- [`KT-85217`](https://youtrack.jetbrains.com/issue/KT-85217) Rework implementation supporting simple-to-suspend function conversion
- [`KT-85036`](https://youtrack.jetbrains.com/issue/KT-85036) Introduce a proper handling of optional expectation annotations in platform checkers during metadata compilation
- [`KT-85086`](https://youtrack.jetbrains.com/issue/KT-85086) False-negative JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME
- [`KT-84082`](https://youtrack.jetbrains.com/issue/KT-84082) [OPT_IN_USAGE_ERROR] duplicates for destructuring declaration 
- [`KT-85007`](https://youtrack.jetbrains.com/issue/KT-85007) Properly implement special rules for `kotlin.Result` in `@JvmExposeBoxed` support
- [`KT-74383`](https://youtrack.jetbrains.com/issue/KT-74383) Support new callable reference nodes in JVM backend
- [`KT-84828`](https://youtrack.jetbrains.com/issue/KT-84828) Cleanup JVM backend from the old callable references-related code
- [`KT-85006`](https://youtrack.jetbrains.com/issue/KT-85006) Refine error messages for `INAPPLICABLE_ALL_TARGET` diagnostic
- [`KT-84296`](https://youtrack.jetbrains.com/issue/KT-84296) Support Companion Blocks in CFG
- [`KT-85058`](https://youtrack.jetbrains.com/issue/KT-85058) Remove final field modification in DescriptorRendererOptionsImpl to prevent warnings on JDK 26+
- [`KT-72710`](https://youtrack.jetbrains.com/issue/KT-72710) Incorrect behaviour of tail call suspend functions optimization
- [`KT-84727`](https://youtrack.jetbrains.com/issue/KT-84727) [K/N] Segfault when returning null as generic Int type from dynamic framework
- [`KT-83460`](https://youtrack.jetbrains.com/issue/KT-83460) Deprecation from `@all`:Deprecated is not propagated to property accessors/backing fields
- [`KT-84859`](https://youtrack.jetbrains.com/issue/KT-84859) Skip deprecation phase for generic arguments in qualifier receiver of static call for companion block members and extensions
- [`KT-85050`](https://youtrack.jetbrains.com/issue/KT-85050) [Swift Export] usage of inline classes with ref types crashes at runtime
- [`KT-84983`](https://youtrack.jetbrains.com/issue/KT-84983) Type parameter annotations are lost for local functions
- [`KT-78800`](https://youtrack.jetbrains.com/issue/KT-78800) Investigate FirMissingDependencySupertypeInQualifiedAccessExpressionsChecker
- [`KT-73945`](https://youtrack.jetbrains.com/issue/KT-73945) K2 IDE: Duplicated inspections for redundant 'open' in interface member
- [`KT-84294`](https://youtrack.jetbrains.com/issue/KT-84294) Ensure Context Sensitive Resolution works with Companion Blocks & Extensions
- [`KT-83904`](https://youtrack.jetbrains.com/issue/KT-83904) [Inliner] Inline function overrides an abstract method with a default value in an inheritance chain
- [`KT-81675`](https://youtrack.jetbrains.com/issue/KT-81675) Improve message for CONTEXTUAL_OVERLOAD_SHADOWED
- [`KT-84994`](https://youtrack.jetbrains.com/issue/KT-84994) Rework optimization for companion extension resolution
- [`KT-81598`](https://youtrack.jetbrains.com/issue/KT-81598) incorrect type mismatch error messages for generic calls with explicit type arguments
- [`KT-84185`](https://youtrack.jetbrains.com/issue/KT-84185) Type arguments are wrongly allowed in receivers of static calls
- [`KT-83441`](https://youtrack.jetbrains.com/issue/KT-83441) False positive: REDUNDANT_CALL_OF_CONVERSION_METHOD
- [`KT-83587`](https://youtrack.jetbrains.com/issue/KT-83587) K2: Missing null-check when using == on Short! and Byte! platform types
- [`KT-84405`](https://youtrack.jetbrains.com/issue/KT-84405) ClassCastException with conflicting projection on the LHS of a callable reference
- [`KT-84866`](https://youtrack.jetbrains.com/issue/KT-84866) Reserve CoroutineContext as context parameter for future use
- [`KT-65239`](https://youtrack.jetbrains.com/issue/KT-65239) K2: Render FIR declaration instead of IR-based descriptors in IR signature clash diagnostics
- [`KT-84743`](https://youtrack.jetbrains.com/issue/KT-84743) Type parameter declared as 'in' can be used in 'out' position in DNN & flexible types
- [`KT-84720`](https://youtrack.jetbrains.com/issue/KT-84720) "Unused return value" is not reported inside used if/when multi-statement blocks
- [`KT-84310`](https://youtrack.jetbrains.com/issue/KT-84310) No Warning Emitted For Deprecated Java Enum Value Usage
- [`KT-81871`](https://youtrack.jetbrains.com/issue/KT-81871) Drop context receiver tests
- [`KT-84714`](https://youtrack.jetbrains.com/issue/KT-84714) KJS: Forbid exporting properties with context parameters
- [`KT-80701`](https://youtrack.jetbrains.com/issue/KT-80701) Native: `-Xbinary=cCallMode` is not integrated with compiler caches
- [`KT-84000`](https://youtrack.jetbrains.com/issue/KT-84000) Native: test pre-codegen inliner on CI
- [`KT-57557`](https://youtrack.jetbrains.com/issue/KT-57557) Implement getAndSet for AtomicNativePtr via getAndSetField intrinsic
- [`KT-84352`](https://youtrack.jetbrains.com/issue/KT-84352) `createUninitializedInstance` generates invalid LLVM for value classes
- [`KT-84234`](https://youtrack.jetbrains.com/issue/KT-84234) Rename `CONST_VAL_WITHOUT_INITIALIZER` diagnostic for properties with EBF
- [`KT-84411`](https://youtrack.jetbrains.com/issue/KT-84411) Confusing message for the class reference of the inner class with the type parameter
- [`KT-84594`](https://youtrack.jetbrains.com/issue/KT-84594) EBF is smartcasted in inline function with `@PiblishedApi`
- [`KT-83989`](https://youtrack.jetbrains.com/issue/KT-83989) Update coroutines-codegen.md after changes of Unit tailcall optimization
- [`KT-84452`](https://youtrack.jetbrains.com/issue/KT-84452) EBF is smartcasted in the default argument with noinline modifier
- [`KT-80925`](https://youtrack.jetbrains.com/issue/KT-80925) Replace "useless" in diagnostic messages
- [`KT-83646`](https://youtrack.jetbrains.com/issue/KT-83646) Native: don't use `sun.misc.Unsafe` in `ByteArrayStream` when running on JVM 24+
- [`KT-82122`](https://youtrack.jetbrains.com/issue/KT-82122) Prohibit arbitrary placement of type parameters in callable reference LHS
- [`KT-82574`](https://youtrack.jetbrains.com/issue/KT-82574) Fixation: consider preferring EQUALS constraints to LOWER ones
- [`KT-83564`](https://youtrack.jetbrains.com/issue/KT-83564) Consider dropping `HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT`
- [`KT-84213`](https://youtrack.jetbrains.com/issue/KT-84213) Flaky incremental compilation behaviour with EBF
- [`KT-84133`](https://youtrack.jetbrains.com/issue/KT-84133) Adopt `initInstance` to handle value classes
- [`KT-24840`](https://youtrack.jetbrains.com/issue/KT-24840) Square bracket escaping in KDoc
- [`KT-84167`](https://youtrack.jetbrains.com/issue/KT-84167) Invalid type references with type arguments in package parts compile without diagnostics
- [`KT-84209`](https://youtrack.jetbrains.com/issue/KT-84209) False negative ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT on context parameters of function types
- [`KT-83354`](https://youtrack.jetbrains.com/issue/KT-83354) Wrong position for lambda context type error 
- [`KT-84206`](https://youtrack.jetbrains.com/issue/KT-84206) Remove forcesPreReleaseBinaries = true from ExplicitBackingFields
- [`KT-83524`](https://youtrack.jetbrains.com/issue/KT-83524) An anonymous function with named parameters throws FileAnalysisException
- [`KT-84155`](https://youtrack.jetbrains.com/issue/KT-84155) K2: NO_CONTEXT_ARGUMENT caused by stale value in `NewConstraintSystemImpl.hasContradictionInForkPointsCache`
- [`KT-83829`](https://youtrack.jetbrains.com/issue/KT-83829) False-negative INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE
- [`KT-83842`](https://youtrack.jetbrains.com/issue/KT-83842) KIAEWA: Exception in expression checkers for `@OptIn`(markerClass=[…])
- [`KT-84045`](https://youtrack.jetbrains.com/issue/KT-84045) Evaluate default arguments of annotation's parameters using FIR evaluator
- [`KT-83987`](https://youtrack.jetbrains.com/issue/KT-83987) Refactor/fix CoroutineCodegen.isReadOfInlineLambda() 
- [`KT-83772`](https://youtrack.jetbrains.com/issue/KT-83772) Create a language feature for wrapContinuationForTailCallFunctions
- [`KT-84061`](https://youtrack.jetbrains.com/issue/KT-84061) K2: `IllegalStateException: FirResolvedNamedReference expected` on plusAssign for array element with unresolved initializer inside buildList
- [`KT-83985`](https://youtrack.jetbrains.com/issue/KT-83985) Drop `arrayOf` check from `EscapeAnalysisChecker ` after bootstrap update
- [`KT-78885`](https://youtrack.jetbrains.com/issue/KT-78885) Current frame disappears from stack trace when debugging inline-heavy suspend code
- [`KT-78727`](https://youtrack.jetbrains.com/issue/KT-78727) Split KonanConfig into NativeFrontendConfig and NativeBackendConfig
- [`KT-83755`](https://youtrack.jetbrains.com/issue/KT-83755) Support rendering of evaluated and original arguments in `FirAnnotationRenderer#renderAnnotation`
- [`KT-17763`](https://youtrack.jetbrains.com/issue/KT-17763) Inner class constructor has incorrect generic signature in the bytecode
- [`KT-83625`](https://youtrack.jetbrains.com/issue/KT-83625) Initialize annotations on Java record components
- [`KT-83795`](https://youtrack.jetbrains.com/issue/KT-83795) Compiler crash on suspend lambda as default parameter of inline function
- [`KT-72880`](https://youtrack.jetbrains.com/issue/KT-72880) Calls with incorrect VarHandle method signatures are generated with -Xjdk-release being used
- [`KT-67809`](https://youtrack.jetbrains.com/issue/KT-67809) Native: remove support for non-opaque LLVM pointer types
- [`KT-82148`](https://youtrack.jetbrains.com/issue/KT-82148) Suspend function returns the wrong value and not Unit
- [`KT-55559`](https://youtrack.jetbrains.com/issue/KT-55559) JVM: ClassCastException with Unit returning suspend function and tail-call Non-Unit returning suspend function and callable reference
- [`KT-70995`](https://youtrack.jetbrains.com/issue/KT-70995) Kotlin/Native: Treat all `@HasFinalizer` types as escaping in Escape Analysis
- [`KT-83903`](https://youtrack.jetbrains.com/issue/KT-83903) 'when'  with 'val' does not take previous nullability check into account
- [`KT-83952`](https://youtrack.jetbrains.com/issue/KT-83952) StackEntries for tail-call suspend functions have internal names for classes instead of FQ names
- [`KT-83377`](https://youtrack.jetbrains.com/issue/KT-83377) Investigate usage of `declarationSymbols` in resolve of local user type
- [`KT-83770`](https://youtrack.jetbrains.com/issue/KT-83770) Smartcast doesn't work for an explicit backing field with multiple intersections
- [`KT-83650`](https://youtrack.jetbrains.com/issue/KT-83650) Native: don't use `sun.misc.Unsafe` in `CastsOptimization` when running on JVM 24+
- [`KT-83754`](https://youtrack.jetbrains.com/issue/KT-83754) KotlinIllegalArgumentExceptionWithAttachments for explicit backing field with annotated type
- [`KT-83756`](https://youtrack.jetbrains.com/issue/KT-83756) Error while resolving FirNamedFunctionImpl with explicit backing field and implicit type
- [`KT-83563`](https://youtrack.jetbrains.com/issue/KT-83563) Consider dropping fixation readiness `REIFIED`
- [`KT-83713`](https://youtrack.jetbrains.com/issue/KT-83713) K2: No error with `external` primary constructor parameter
- [`KT-83104`](https://youtrack.jetbrains.com/issue/KT-83104) K2: No error with external enum entry
- [`KT-83696`](https://youtrack.jetbrains.com/issue/KT-83696) Consider dropping HAS_NO_RELATION_TO_ANY_OUTPUT_TYPE readiness
- [`KT-83308`](https://youtrack.jetbrains.com/issue/KT-83308) K/N: "IllegalArgumentException: An interface expected but was Any"
- [`KT-81590`](https://youtrack.jetbrains.com/issue/KT-81590) Switch latest stable version in Kotlin project to 2.4
- [`KT-66701`](https://youtrack.jetbrains.com/issue/KT-66701) K2: Java interface method override via Kotlin class rejected
- [`KT-56563`](https://youtrack.jetbrains.com/issue/KT-56563) Inference within if stops working when changing expected type from Any to a different type
- [`KT-261`](https://youtrack.jetbrains.com/issue/KT-261) Can't specify function return type in a subclass

### Compose compiler

#### New features
- [`c1bbb47`](https://github.com/JetBrains/kotlin/commit/c1bbb479ed4d37b19407917bb7c3bad14f99406c) Started inferring the stability of all interfaces to be
      `Stability.Unknown`, expect for those explicitly marked as known
      stable.

#### Fixes
- [`b/511102714`](https://issuetracker.google.com/issues/511102714) Made the default stability of non-final classes `Unknown`
      instead of `Stable`
- [`b/509945632`](https://issuetracker.google.com/issues/509945632) Do not generate groups in inline lambdas without `@Composable` calls.
- [`b/504284805`](https://issuetracker.google.com/issues/504284805) Fix indentation for generated proguard mappings.
- [`b/422193018`](https://issuetracker.google.com/issues/422193018) Fix applier inference for nested composables of different types.
- [`b/497751457`](https://issuetracker.google.com/issues/497751457) Prevent a `$stable` property from being added to any object.
- [`b/427530633`](https://issuetracker.google.com/issues/427530633) Do not infer a getter call as static across when it is defined in another file.
- [`b/427530633`](https://issuetracker.google.com/issues/427530633) Started using `Stability.Runtime` more broadly. Now, when an
      element depends on the stability of an `internal` or `public` class
      defined in another file, the element will no longer infer the
      stability of that class and will depend on the runtime stability of
      that class instead.

### IR. Actualizer

- [`KT-84293`](https://youtrack.jetbrains.com/issue/KT-84293) Expect Actual Matching for Companion Block Declarations & Extensions

### IR. Inlining

- [`KT-73708`](https://youtrack.jetbrains.com/issue/KT-73708) Use some marker in KLIBs produced with IR inliner
- [`KT-85605`](https://youtrack.jetbrains.com/issue/KT-85605) "Local delegated property has not delegate" exception when calling inline function containing delegated property in a lambda from within an inline lambda
- [`KT-84112`](https://youtrack.jetbrains.com/issue/KT-84112) Intra-module inliner: No container found for type parameter 'T'
- [`KT-84416`](https://youtrack.jetbrains.com/issue/KT-84416) High memory usage for IrFileEntry after enabling inliner
- [`KT-75396`](https://youtrack.jetbrains.com/issue/KT-75396) [IR] Pass LoweringContext to inline and serialization checkers

### IR. Interpreter

- [`KT-86083`](https://youtrack.jetbrains.com/issue/KT-86083) Create a new CLI flag to enable `IntrinsicConstEvaluation` feature
- [`KT-80804`](https://youtrack.jetbrains.com/issue/KT-80804) Enable constant evaluation for more standard library
- [`KT-83514`](https://youtrack.jetbrains.com/issue/KT-83514) Get rid of `EvaluatedConstTracker`
- [`KT-84561`](https://youtrack.jetbrains.com/issue/KT-84561) K2: Convert evaluated constant by default in FIR2IR

### IR. Tree

- [`KT-79663`](https://youtrack.jetbrains.com/issue/KT-79663) KLIB-based compilers: Promote partial linkage to "always on"
- [`KT-76934`](https://youtrack.jetbrains.com/issue/KT-76934) Drop old IR parameter API
- [`KT-72950`](https://youtrack.jetbrains.com/issue/KT-72950) Partial Linkage: Change the semantics of `-Xpartial-linkage-loglevel`
- [`KT-74763`](https://youtrack.jetbrains.com/issue/KT-74763) Build: refactor ':compiler:backend.common' and ':compiler:ir.backend.common' modules
- [`KT-76634`](https://youtrack.jetbrains.com/issue/KT-76634) PL: Don't report warnings in cases that don't lead to runtime errors
- [`KT-79801`](https://youtrack.jetbrains.com/issue/KT-79801) KLIBs: Implement checks for symbols loaded by the compiler on 1st and 2nd phases
- [`KT-72812`](https://youtrack.jetbrains.com/issue/KT-72812) IR serializer: Don't serialize any cinterop fake overrides to Klibs

### JVM. Reflection

#### Fixes

- [`KT-86017`](https://youtrack.jetbrains.com/issue/KT-86017) KClass.constructors returns all java.lang.String constructors for mapped type kotlin.String
- [`KT-85550`](https://youtrack.jetbrains.com/issue/KT-85550) Reflection: KParameter.type.classifier returns boxed KClass for non-nullable primitive types
- [`KT-82659`](https://youtrack.jetbrains.com/issue/KT-82659) Reflection: IAE on a call to a Java inner class constructor
- [`KT-85999`](https://youtrack.jetbrains.com/issue/KT-85999) Reflection: ByteArray KType incorrectly has type arguments in Kotlin 2.4.0
- [`KT-85285`](https://youtrack.jetbrains.com/issue/KT-85285) Reflection: InvocationTargetException (UInt cannot be cast to Integer) when reading UInt annotation property via getter
- [`KT-85091`](https://youtrack.jetbrains.com/issue/KT-85091) Reflection: "KotlinReflectionInternalError: Unsupported parameter owner: null" on attempt to get annotations of annotation constructor parameter
- [`KT-85322`](https://youtrack.jetbrains.com/issue/KT-85322) Reflection: KotlinReflectionInternalError when loading ProGuard-obfuscated code compiled before 2.3.20
- [`KT-84679`](https://youtrack.jetbrains.com/issue/KT-84679) Reflection: confusing "Kotlin reflection is not yet supported for synthetic Java properties" for reference to Java enum's entries property
- [`KT-85025`](https://youtrack.jetbrains.com/issue/KT-85025) `KTypeParameter` instances not equal to each other for the same type parameter in member specialization `KFunction`
- [`KT-84796`](https://youtrack.jetbrains.com/issue/KT-84796) Reflection: mutable flexibility is lost for K1-based types in KClass.allSupertypes
- [`KT-84494`](https://youtrack.jetbrains.com/issue/KT-84494) Reflection: Java Collections have differences in kotlin supertypes from old K1 reflection
- [`KT-84382`](https://youtrack.jetbrains.com/issue/KT-84382) Reflection: raw list in Java type is transformed to List instead of MutableList
- [`KT-84492`](https://youtrack.jetbrains.com/issue/KT-84492) Reflection: supertypes of raw list in Java type are not raw
- [`KT-84076`](https://youtrack.jetbrains.com/issue/KT-84076) Reflection: list in Java type is transformed to flexible instead of mutable list
- [`KT-14990`](https://youtrack.jetbrains.com/issue/KT-14990) 'callBy' for inner class constructor fails at run-time
- [`KT-82881`](https://youtrack.jetbrains.com/issue/KT-82881) Reflection: update KCallable.callBy kdoc to mention vararg parameters
- [`KT-84075`](https://youtrack.jetbrains.com/issue/KT-84075) Reflection: wildcard in Java type is transformed to `out Any!` instead of star projection

### JavaScript

#### New Features

- [`KT-51292`](https://youtrack.jetbrains.com/issue/KT-51292) Proposed behavior of `@JsExport` on interfaces and classes with companion objects
- [`KT-21626`](https://youtrack.jetbrains.com/issue/KT-21626) Support ES2015 syntax in `js` function
- [`KT-83451`](https://youtrack.jetbrains.com/issue/KT-83451) K/JS: Support ES6 object destructuring in js() calls
- [`KT-83452`](https://youtrack.jetbrains.com/issue/KT-83452) K/JS: Support ES6 array destructuring in js() calls
- [`KT-82128`](https://youtrack.jetbrains.com/issue/KT-82128) [K/JS] Allow named companion objects in exported interfaces

#### Performance Improvements

- [`KT-77646`](https://youtrack.jetbrains.com/issue/KT-77646) KJS: optimize Byte/Char/Short/Int/Float/DoubleArray.copyOf(newSize)

#### Fixes

- [`KT-84332`](https://youtrack.jetbrains.com/issue/KT-84332) KJS: Reconsider disallowing nested classes in exported interfaces
- [`KT-72198`](https://youtrack.jetbrains.com/issue/KT-72198) KJS: ES2015 interop with ValueClass
- [`KT-84090`](https://youtrack.jetbrains.com/issue/KT-84090) Save variance in the generated TypeScript
- [`KT-56618`](https://youtrack.jetbrains.com/issue/KT-56618) KJS/IR: Support external interfaces from common code (via annotation?)
- [`KT-85038`](https://youtrack.jetbrains.com/issue/KT-85038) Kotlin/JS: `@JsExport` on sealed external interface with companion object causes NPE
- [`KT-81787`](https://youtrack.jetbrains.com/issue/KT-81787) KJS: Value class type lost when using JsExport on interface
- [`KT-85411`](https://youtrack.jetbrains.com/issue/KT-85411) Fix conversionCombinations.kt tests for the JS target
- [`KT-83701`](https://youtrack.jetbrains.com/issue/KT-83701) Escaped identifier with a quote cause an invalid d.ts file
- [`KT-68281`](https://youtrack.jetbrains.com/issue/KT-68281) K/JS: Order of classes in initMetadataForClass are not deterministic
- [`KT-15101`](https://youtrack.jetbrains.com/issue/KT-15101) js: Same callable references are not equal
- [`KT-84810`](https://youtrack.jetbrains.com/issue/KT-84810) [K/JS] Callable references operator produces duplicates
- [`KT-85323`](https://youtrack.jetbrains.com/issue/KT-85323) JsClass optimization doesn't work well for primitives
- [`KT-60651`](https://youtrack.jetbrains.com/issue/KT-60651) KJS / ES6: init block and constructor are not called
- [`KT-84601`](https://youtrack.jetbrains.com/issue/KT-84601) K/JS: `KClass<>` reference doesn't work in JS counterside as a `new` target in ES6 mode
- [`KT-85099`](https://youtrack.jetbrains.com/issue/KT-85099) KotlinJS: JsPlainObject from the js-plain-objects plugin does not respect overrides
- [`KT-84615`](https://youtrack.jetbrains.com/issue/KT-84615) KJS: Forbid `@JsStatic` on extension functions/properties
- [`KT-84002`](https://youtrack.jetbrains.com/issue/KT-84002) Bump version from 2.3 to 2.4 for JsNoRuntime-related annotations
- [`KT-84474`](https://youtrack.jetbrains.com/issue/KT-84474) Kotlin/JS: Long::class becomes null when passing the value to a generic function with -Xes-long-as-bigint
- [`KT-84647`](https://youtrack.jetbrains.com/issue/KT-84647) K/JS: Class expressions are not supported in js() calls
- [`KT-84458`](https://youtrack.jetbrains.com/issue/KT-84458) KJS: Fully support `@JsStatic` in Analysis API-based TypeScript Export
- [`KT-84454`](https://youtrack.jetbrains.com/issue/KT-84454) KJS: Generate protected overrides for abstract class inheritors in Analysis API-based TypeScript Export
- [`KT-84490`](https://youtrack.jetbrains.com/issue/KT-84490) KJS: Fix mutability of exported top-level variables Analysis API-based TS export with ES modules
- [`KT-84459`](https://youtrack.jetbrains.com/issue/KT-84459) KJS: Support default exportability in Analysis API-based TypeScript Export
- [`KT-84456`](https://youtrack.jetbrains.com/issue/KT-84456) KJS: Support deprecation comments in Analysis API-based TypeScript export
- [`KT-82264`](https://youtrack.jetbrains.com/issue/KT-82264) Implement exporting classes in Analysis API-based TypeScript Export
- [`KT-84233`](https://youtrack.jetbrains.com/issue/KT-84233) K/JS: exported collection views doesn't provide Iterator methods
- [`KT-82127`](https://youtrack.jetbrains.com/issue/KT-82127) Remove generator-based coroutines intrinsics after bootstrap
- [`KT-84003`](https://youtrack.jetbrains.com/issue/KT-84003) Remove `@Suppress` from JsReference after bootstrap
- [`KT-44753`](https://youtrack.jetbrains.com/issue/KT-44753) KJS / IR: `@JsExport` non-public fun exports nothing
- [`KT-83992`](https://youtrack.jetbrains.com/issue/KT-83992) Drop K1 JS entry point and IC code
- [`KT-83722`](https://youtrack.jetbrains.com/issue/KT-83722) K/JS: NON_EXPORTABLE_TYPE in the exported file with name-based destructuring declarations
- [`KT-69353`](https://youtrack.jetbrains.com/issue/KT-69353) KJS / d.ts: Kotlin does not export base collection classes along with their mutable collection counterparts

### Klibs

#### Performance Improvements

- [`KT-84415`](https://youtrack.jetbrains.com/issue/KT-84415) Ineffective hashMap usage in IrSymbolDeserializer
- [`KT-80903`](https://youtrack.jetbrains.com/issue/KT-80903) [Klib] Optimize size of serialized IR element coordinates
- [`KT-84451`](https://youtrack.jetbrains.com/issue/KT-84451) [Klib] Use varint encoding for element sizes in IR tables
- [`KT-84400`](https://youtrack.jetbrains.com/issue/KT-84400) [Klib] Optimize size of serialized IrExpression
- [`KT-79675`](https://youtrack.jetbrains.com/issue/KT-79675) K/N: Uncached ZipFIleSystemAccessor

#### Fixes

- [`KT-78188`](https://youtrack.jetbrains.com/issue/KT-78188) [JS] Klib backward and forward compatibility testing
- [`KT-82471`](https://youtrack.jetbrains.com/issue/KT-82471) [K/N] Klib forward compatibility testing with codegen tests
- [`KT-83929`](https://youtrack.jetbrains.com/issue/KT-83929) Add tests for IR signatures of static properties and functions
- [`KT-81411`](https://youtrack.jetbrains.com/issue/KT-81411) Merge `KonanLibrary` to `KotlinLibrary` to simplify adoption of `KlibLoader` in the Kotlin/Native compiler
- [`KT-82469`](https://youtrack.jetbrains.com/issue/KT-82469) [K/N] Klib backward compatibility testing with codegen tests
- [`KT-84511`](https://youtrack.jetbrains.com/issue/KT-84511) [Native][Tests] Improve descriptor-related logic in NativeCliBasedFacades.kt
- [`KT-85017`](https://youtrack.jetbrains.com/issue/KT-85017) [PL] Add test for added `internal abstract fun`
- [`KT-84488`](https://youtrack.jetbrains.com/issue/KT-84488) Export in previous version: Prohibit using on 2nd stage
- [`KT-85149`](https://youtrack.jetbrains.com/issue/KT-85149) Klib Dump parser: fix parsing of qualified names adjacent to vararg symbol
- [`KT-85129`](https://youtrack.jetbrains.com/issue/KT-85129) Klib Dump parser: fix enum names parsing
- [`KT-84684`](https://youtrack.jetbrains.com/issue/KT-84684) Remove `UserVisibleIrModulesSupport` from IR linker
- [`KT-84820`](https://youtrack.jetbrains.com/issue/KT-84820) [K/N] Load `libcallbacks` and `libllvmstubs` from configured path 
- [`KT-84819`](https://youtrack.jetbrains.com/issue/KT-84819) K/N: Segfault on xctest when using internal abstract on a parent class
- [`KT-83807`](https://youtrack.jetbrains.com/issue/KT-83807) Restore non-nullability of symbols not available in 2.3.0 stdlib
- [`KT-83012`](https://youtrack.jetbrains.com/issue/KT-83012) Export in previous version (Native): add the checker for incompatible Kotlin stdlib/compiler pairs
- [`KT-84341`](https://youtrack.jetbrains.com/issue/KT-84341) Fix detection of box function in forward compatibility tests
- [`KT-83748`](https://youtrack.jetbrains.com/issue/KT-83748) Bump versions in JS Klib compatibility testing
- [`KT-83724`](https://youtrack.jetbrains.com/issue/KT-83724) Fix & unmute stdlib & kotlin-test compatibility tests
- [`KT-83151`](https://youtrack.jetbrains.com/issue/KT-83151) Restore non-nullability of symbols available since 2.3

### Language Design

- [`KT-14663`](https://youtrack.jetbrains.com/issue/KT-14663) Support having a "public" and a "private" type for the same property
- [`KT-80852`](https://youtrack.jetbrains.com/issue/KT-80852) Version overloading: generate overloads corresponding to different versions of a function whose parameters are annotated with `@IntroducedAt`(<version>)
- [`KT-73821`](https://youtrack.jetbrains.com/issue/KT-73821) Decide the future of the ForbidUsingSupertypesWithInaccessibleContentInTypeArguments language feature
- [`KT-85120`](https://youtrack.jetbrains.com/issue/KT-85120) `@IntroducedAt` on expect parameter cannot be properly actualized

### Libraries

#### New Features

- [`KT-84921`](https://youtrack.jetbrains.com/issue/KT-84921) Add 'returnsResultOf' contract to appropriate declarations in the stdlib
- [`KT-73111`](https://youtrack.jetbrains.com/issue/KT-73111) No UInt.toBigInteger() and ULong.toBigInteger() conversion function
- [`KT-78499`](https://youtrack.jetbrains.com/issue/KT-78499) Add isSorted() extension to standard library

#### Performance Improvements

- [`KT-70750`](https://youtrack.jetbrains.com/issue/KT-70750) Kotlin's Regex implementation on Kotlin Native is painfully slow
- [`KT-67731`](https://youtrack.jetbrains.com/issue/KT-67731) Regular expressions works slowly on iOS
- [`KT-80772`](https://youtrack.jetbrains.com/issue/KT-80772) K/N: Regex: improve look behind matching performance for "fixed-length" patterns

#### Fixes

- [`KT-83525`](https://youtrack.jetbrains.com/issue/KT-83525) Introduce support window for Kotlin Stdlib versions
- [`KT-86027`](https://youtrack.jetbrains.com/issue/KT-86027) Hide returnsResultOf under a separate flag and remove its usages from kotlin stdlib
- [`KT-84818`](https://youtrack.jetbrains.com/issue/KT-84818) [Regex] Native and Wasm: Decomposed Unicode character are incorrectly process with CANON_EQ flag
- [`KT-83131`](https://youtrack.jetbrains.com/issue/KT-83131) Testing: assertNull has no contract
- [`KT-85122`](https://youtrack.jetbrains.com/issue/KT-85122) Deprecate kotlin.io.readLine with WARNING
- [`KT-84970`](https://youtrack.jetbrains.com/issue/KT-84970) Deprecate AbstractCoroutineContextKey and associated API
- [`KT-81395`](https://youtrack.jetbrains.com/issue/KT-81395) Stabilize kotlin.uuid.Uuid API
- [`KT-85127`](https://youtrack.jetbrains.com/issue/KT-85127) Remove kotlin.test.assert*NoInline hidden functions
- [`KT-84264`](https://youtrack.jetbrains.com/issue/KT-84264) Add appropiate `@SinceKotlin` to new contracts
- [`KT-84697`](https://youtrack.jetbrains.com/issue/KT-84697) Update the list of JDKs the stdlib is tested with
- [`KT-83956`](https://youtrack.jetbrains.com/issue/KT-83956) Clarify joinToString behavior when the receiver is empty
- [`KT-61180`](https://youtrack.jetbrains.com/issue/KT-61180) kotlin.ArrayIndexOutOfBoundsException on Native with Regex, works on Android/JVM though
- [`KT-84871`](https://youtrack.jetbrains.com/issue/KT-84871) compareValues, nullsFirst, nullsLast return 0 for -0.0 and 0.0 on JS
- [`KT-84691`](https://youtrack.jetbrains.com/issue/KT-84691) Add samples for toBigInteger extension functions
- [`KT-84372`](https://youtrack.jetbrains.com/issue/KT-84372) PathExtensionsTest.copyToRestrictedReadSource fails with JDK22+
- [`KT-84369`](https://youtrack.jetbrains.com/issue/KT-84369) StringJVMTest.formatter fails with JDK13+
- [`KT-84613`](https://youtrack.jetbrains.com/issue/KT-84613) String.toDouble() produces incorrect results on Wasm for large exponent values
- [`KT-76905`](https://youtrack.jetbrains.com/issue/KT-76905) Add samples for kotlin.math functions
- [`KT-84355`](https://youtrack.jetbrains.com/issue/KT-84355) Reduce the number of iterations for the removeHashAtStressTest
- [`KT-83962`](https://youtrack.jetbrains.com/issue/KT-83962) List.listIterator(Int) KDoc's exception condition is incorrect
- [`KT-83958`](https://youtrack.jetbrains.com/issue/KT-83958) Improve enumValueOf documentation
- [`KT-83953`](https://youtrack.jetbrains.com/issue/KT-83953) Add samples for kotlin.time extension functions
- [`KT-83951`](https://youtrack.jetbrains.com/issue/KT-83951) Rewrite stdlib samples to use assertPrints instead of assertEquals

### Native

#### New Features

- [`KT-78686`](https://youtrack.jetbrains.com/issue/KT-78686) LLVM update Q1 2026
- [`KT-84826`](https://youtrack.jetbrains.com/issue/KT-84826) Bump the minimum deployment version of Apple targets

#### Fixes

- [`KT-84686`](https://youtrack.jetbrains.com/issue/KT-84686) Removing x64 in gradle file breaks builds on certain platforms
- [`KT-71278`](https://youtrack.jetbrains.com/issue/KT-71278) Enable Concurrent Mark and Sweep (CMS) GC by default
- [`KT-81748`](https://youtrack.jetbrains.com/issue/KT-81748) Create a phased CLI for Native klib compilation
- [`KT-84733`](https://youtrack.jetbrains.com/issue/KT-84733) LLVM Update: rebase LLVM once the upstream stabilizes
- [`KT-83914`](https://youtrack.jetbrains.com/issue/KT-83914) Native: when loading JNI libraries, java.library.path can contain system directories with libraries with same names
- [`KT-83133`](https://youtrack.jetbrains.com/issue/KT-83133) Native: don't use `sun.misc.Unsafe` in the compiler and cinterop when running on JDK 25+
- [`KT-83648`](https://youtrack.jetbrains.com/issue/KT-83648) Native: don't use `sun.misc.Unsafe` in `NativeMemoryAllocator` when running on JDK 25+
- [`KT-83647`](https://youtrack.jetbrains.com/issue/KT-83647) Native: don't use `sun.misc.Unsafe` in `nativeMemUtils` when running on JDK 25+
- [`KT-82674`](https://youtrack.jetbrains.com/issue/KT-82674) Native: dyld[...]: Symbol not found: _mach_vm_reclaim_update_kernel_accounting_trap on macOS
- [`KT-82879`](https://youtrack.jetbrains.com/issue/KT-82879) Native: DLLs in the Windows distribution are not reproducible
- [`KT-83283`](https://youtrack.jetbrains.com/issue/KT-83283) Test Kotlin/Native performance tests compilation in Gradle 9.0
- [`KT-82872`](https://youtrack.jetbrains.com/issue/KT-82872) Native: make Kotlin/Native distribution compiler cache reproducible for Linux
- [`KT-82871`](https://youtrack.jetbrains.com/issue/KT-82871) Native: cstubs.bc for android_* platform libraries contain absolute paths in string literals
- [`KT-34467`](https://youtrack.jetbrains.com/issue/KT-34467) Cinterop: Clang crashes when -fmodule-map-file is specified (SIGSEGV)

### Native. Build Infrastructure

- [`KT-85803`](https://youtrack.jetbrains.com/issue/KT-85803) K/N: build not reproducible - platformLibs caches
- [`KT-84503`](https://youtrack.jetbrains.com/issue/KT-84503) Duplicate META-INF/serialization.shadow.kotlin_module entry in kotlin-native-compiler-embeddable jar
- [`KT-85191`](https://youtrack.jetbrains.com/issue/KT-85191) K/N: Dependency cycle in libclangInterop
- [`KT-84937`](https://youtrack.jetbrains.com/issue/KT-84937) Kotlin/Native: non-reproducible .bc for mingw_x64
- [`KT-80072`](https://youtrack.jetbrains.com/issue/KT-80072) Make Kotlin/Native distribution reproducible
- [`KT-81771`](https://youtrack.jetbrains.com/issue/KT-81771) konanc failing to load native libraries

### Native. C Export

- [`KT-61748`](https://youtrack.jetbrains.com/issue/KT-61748) KMM- warnings when compiling native targets (Kotlin 1.9.0)

### Native. C and ObjC Import

- [`KT-85705`](https://youtrack.jetbrains.com/issue/KT-85705) Swift-generated headers with external_source_symbol produce duplicate enum declarations 
- [`KT-82379`](https://youtrack.jetbrains.com/issue/KT-82379) Introduce lenient modular cinterop mode
- [`KT-81695`](https://youtrack.jetbrains.com/issue/KT-81695) Repeated typedefs across multiple clang modules break cinterop with -fmodules
- [`KT-82766`](https://youtrack.jetbrains.com/issue/KT-82766) K/N: external_source_symbol clang attribute causes cinterops with -fmodules to downgrade to forward declaration
- [`KT-84023`](https://youtrack.jetbrains.com/issue/KT-84023) Modular import fails with an obscure error when the failing module is not the last one
- [`KT-81433`](https://youtrack.jetbrains.com/issue/KT-81433) Generate C-interop KLIBs in previous ABI version in Kotlin 2.4.0
- [`KT-82402`](https://youtrack.jetbrains.com/issue/KT-82402) Inter-cinterop type reuse with -fmodules uses forward declaration when an actual declaration is available
- [`KT-82377`](https://youtrack.jetbrains.com/issue/KT-82377) Fix ObjC forward declaration handling in modular cinterops
- [`KT-81752`](https://youtrack.jetbrains.com/issue/KT-81752) Native: investigate and remove filtering of `-fmodule-map-file` in cinterop
- [`KT-83814`](https://youtrack.jetbrains.com/issue/KT-83814) Native: includedHeaders= in platform libs manifests is not reproducible when modules= is used

### Native. ObjC Export

- [`KT-86068`](https://youtrack.jetbrains.com/issue/KT-86068) Native: add support for ObjCEnum.EntryName
- [`KT-85171`](https://youtrack.jetbrains.com/issue/KT-85171) Red Swift code in Native UI Multiplatform App project from Template Gallery

### Native. Runtime

- [`KT-84331`](https://youtrack.jetbrains.com/issue/KT-84331) Kotlin/Native: RunLoopFinalizerProcessor needs initialized runtime before it has any jobs

### Native. Runtime. Memory

- [`KT-83670`](https://youtrack.jetbrains.com/issue/KT-83670) K/N: gc concurrent mark phase assert Failed to terminate mark in STW in a single iteration
- [`KT-80770`](https://youtrack.jetbrains.com/issue/KT-80770) Kotlin/Native: revise ObjC refcount methods called in runnable state
- [`KT-84640`](https://youtrack.jetbrains.com/issue/KT-84640) Native: comment for `kotlin.native.runtime.SweepStatistics` misses the word "number"

### Native. Swift Export

#### New Features

- [`KT-84361`](https://youtrack.jetbrains.com/issue/KT-84361) [Swift Export] Preserve TypeInfo on StateFlow
- [`KT-85130`](https://youtrack.jetbrains.com/issue/KT-85130) [Swift Export] Preserve TypeInfo on SharedFlow
- [`KT-82705`](https://youtrack.jetbrains.com/issue/KT-82705) Support convenient export of Flow types in Swift export
- [`KT-66821`](https://youtrack.jetbrains.com/issue/KT-66821) Swift Export: value class
- [`KT-82598`](https://youtrack.jetbrains.com/issue/KT-82598) Swift Export: Custom name translation
- [`KT-69431`](https://youtrack.jetbrains.com/issue/KT-69431) Swift export: inline functions
- [`KT-84263`](https://youtrack.jetbrains.com/issue/KT-84263) [Swift Export] Context Parameters on Functional Types

#### Fixes

- [`KT-80305`](https://youtrack.jetbrains.com/issue/KT-80305) Support coroutines in Swift Export
- [`KT-85380`](https://youtrack.jetbrains.com/issue/KT-85380) [Swift Export] Attempt to bridge unbridgeable type: SirUnsupportedType
- [`KT-84317`](https://youtrack.jetbrains.com/issue/KT-84317) Swift Export: "protocol members can only be marked unavailable in an '`@objc`' protocol" in generated code for kotlinx-coroutines
- [`KT-85704`](https://youtrack.jetbrains.com/issue/KT-85704) [Swift Export] cannot infer generic type of function returning a generic type
- [`KT-85711`](https://youtrack.jetbrains.com/issue/KT-85711) [Swift Export] suspend function returning non-null generic fails to compile
- [`KT-85715`](https://youtrack.jetbrains.com/issue/KT-85715) [Swift Export] generic interface in typealias fails to compile
- [`KT-85714`](https://youtrack.jetbrains.com/issue/KT-85714) [Swift Export] unsupported input type param in functional receiver 
- [`KT-83743`](https://youtrack.jetbrains.com/issue/KT-83743) Swift export: type arguments expected for generic typealias
- [`KT-85458`](https://youtrack.jetbrains.com/issue/KT-85458) [Swift Export] value of a closure returning a closure generates invalid swift code
- [`KT-85521`](https://youtrack.jetbrains.com/issue/KT-85521) [Swift Export] conflicting overloads for generated Kotlin bridges
- [`KT-85293`](https://youtrack.jetbrains.com/issue/KT-85293) SwiftExportCoroutinesWithResultValidationTest.testCoroutines fails after cross-push
- [`KT-84515`](https://youtrack.jetbrains.com/issue/KT-84515) [Swift Export] suspend functional parameter generates invalid Swift code
- [`KT-82282`](https://youtrack.jetbrains.com/issue/KT-82282) Swift Export: suspend function returning Array leads to incompilable code 
- [`KT-81540`](https://youtrack.jetbrains.com/issue/KT-81540) Swift Export: using interface in Set generates incompilable code
- [`KT-66873`](https://youtrack.jetbrains.com/issue/KT-66873) Swift Export: suspendable contravariant functional type
- [`KT-85272`](https://youtrack.jetbrains.com/issue/KT-85272) [Swift Export] conflicting imports for kotlinx-coroutines
- [`KT-85163`](https://youtrack.jetbrains.com/issue/KT-85163) [Swift Export] Flow of Unit values crashes
- [`KT-85159`](https://youtrack.jetbrains.com/issue/KT-85159) [Swift Export] Flow is not properly being cancelled
- [`KT-84226`](https://youtrack.jetbrains.com/issue/KT-84226) [Swift Export] Flow in contrvariant position is not allowed
- [`KT-84485`](https://youtrack.jetbrains.com/issue/KT-84485) [Swift Export] Flow with nullable elements
- [`KT-83730`](https://youtrack.jetbrains.com/issue/KT-83730) Generated Swift switch on bridged Kotlin enum crashes with fatalError
- [`KT-85016`](https://youtrack.jetbrains.com/issue/KT-85016) [Swift Export] it's not OK to expose Flow as AsyncSequence
- [`KT-84979`](https://youtrack.jetbrains.com/issue/KT-84979) Swift Export Nullability: Unit
- [`KT-83821`](https://youtrack.jetbrains.com/issue/KT-83821) Swift Export: suspend function returning Nothing leads to incompilable code
- [`KT-81593`](https://youtrack.jetbrains.com/issue/KT-81593) Swift Export: suspend function returning Unit leads to incompilable code
- [`KT-84359`](https://youtrack.jetbrains.com/issue/KT-84359) [Swift Export] nested functional type with Unit parameter
- [`KT-84358`](https://youtrack.jetbrains.com/issue/KT-84358) [Swift Export] functional type with Unit parameter
- [`KT-84356`](https://youtrack.jetbrains.com/issue/KT-84356) [Swift Export] functional type with single Unit parameter
- [`KT-83567`](https://youtrack.jetbrains.com/issue/KT-83567) Swift Export: "IllegalStateException: Internal compiler error: doesn't correspond to any C type: kotlin.Unit": invalid closure is generated for suspend function which returns Unit
- [`KT-83397`](https://youtrack.jetbrains.com/issue/KT-83397) [Swift Export] Functional return type with Unit parameter is emitted as invalid void parameter list ('void' must be the first and only parameter)
- [`KT-84243`](https://youtrack.jetbrains.com/issue/KT-84243) [Swift Export] Returning value of suspending functional type from suspending function yields invalid code
- [`KT-82568`](https://youtrack.jetbrains.com/issue/KT-82568) Swift Export: Context Parameters
- [`KT-83398`](https://youtrack.jetbrains.com/issue/KT-83398) [Swift export] converting non-escaping parameter to generic parameter may allow it to escape
- [`KT-83389`](https://youtrack.jetbrains.com/issue/KT-83389) Swift Export: "ClassCastException" caused by suspend fun throwing Error
- [`KT-83116`](https://youtrack.jetbrains.com/issue/KT-83116) Swift export generates bridges incompatible with language version 2.4
- [`KT-83749`](https://youtrack.jetbrains.com/issue/KT-83749) [Swift Export] varargs and List uses the same mangling on bridges
- [`KT-83712`](https://youtrack.jetbrains.com/issue/KT-83712) Swift Export ignores internal setter and generates invalid bridge code

### Tools. BCV

- [`KT-83476`](https://youtrack.jetbrains.com/issue/KT-83476) Use Maven publications as dump input [ABI Validation]
- [`KT-78341`](https://youtrack.jetbrains.com/issue/KT-78341) Outer scope's visibility is not considered when dumping const vals [ABI Validation JVM]
- [`KT-78305`](https://youtrack.jetbrains.com/issue/KT-78305) Private constructor is written in ABI dump
- [`KT-82724`](https://youtrack.jetbrains.com/issue/KT-82724) BCV incorrectly reports generated `@JvmOverloads` declarations as public
- [`KT-78367`](https://youtrack.jetbrains.com/issue/KT-78367) Internal constructor infiltrated into a dump
- [`KT-78366`](https://youtrack.jetbrains.com/issue/KT-78366) Protected method of enum should not be included into a dump

### Tools. Build Tools API

#### New Features

- [`KT-83794`](https://youtrack.jetbrains.com/issue/KT-83794) Make BTA JVM Compiler Arguments Type-Safe
- [`KT-80963`](https://youtrack.jetbrains.com/issue/KT-80963) BTA: Add structured information about reported messages to KotlinLogger
- [`KT-73037`](https://youtrack.jetbrains.com/issue/KT-73037) Add input (like compiler arguments) changes tracking
- [`KT-82791`](https://youtrack.jetbrains.com/issue/KT-82791) BTA: introduce an option for `ExecutionPolicy.WithDaemon` to control the daemon log files path
- [`KT-84453`](https://youtrack.jetbrains.com/issue/KT-84453) SSoT: provide a unified way to convert Enums to Strings

#### Fixes

- [`KT-86395`](https://youtrack.jetbrains.com/issue/KT-86395) [BTA] forward-compatibility violation: NoSuchMethodError on JvmSnapshotBasedIncrementalCompilationConfiguration.<init> breaks IC
- [`KT-80679`](https://youtrack.jetbrains.com/issue/KT-80679) Add support  for the Build Tools API [ABI Validation]
- [`KT-85294`](https://youtrack.jetbrains.com/issue/KT-85294) BTA: Replace hardcoded `@since` in KDoc with dynamic versioning
- [`KT-85035`](https://youtrack.jetbrains.com/issue/KT-85035) Don't expose X_COMPILER_PLUGIN_ORDER in CommonCompilerArguments
- [`KT-85391`](https://youtrack.jetbrains.com/issue/KT-85391) [BTA] Hide boilerplate required to load isolated BTA implementation
- [`KT-85447`](https://youtrack.jetbrains.com/issue/KT-85447) BTA: deprecate JvmCompilerArguments.contains (warning)
- [`KT-82390`](https://youtrack.jetbrains.com/issue/KT-82390) [BTA] Remove deprecated non-builder factory functions and classes
- [`KT-84850`](https://youtrack.jetbrains.com/issue/KT-84850) Make kotlin-home type safe
- [`KT-84867`](https://youtrack.jetbrains.com/issue/KT-84867) Make Xphases-to-* arguments type-safe
- [`KT-85094`](https://youtrack.jetbrains.com/issue/KT-85094) Make Xwarning-level type safe
- [`KT-85205`](https://youtrack.jetbrains.com/issue/KT-85205) Make Xdump-perf type safe
- [`KT-85204`](https://youtrack.jetbrains.com/issue/KT-85204) Make Xdump-directory type safe
- [`KT-84906`](https://youtrack.jetbrains.com/issue/KT-84906) Make enum-based common arguments type-safe
- [`KT-84322`](https://youtrack.jetbrains.com/issue/KT-84322) Make X_PROFILE BTA compiler argument type safe
- [`KT-84825`](https://youtrack.jetbrains.com/issue/KT-84825) Make script-templates type safe
- [`KT-84546`](https://youtrack.jetbrains.com/issue/KT-84546) Replace raw string path arguments with type-safe PathListType
- [`KT-84705`](https://youtrack.jetbrains.com/issue/KT-84705) Make Xjdk-release to type-safe
- [`KT-85167`](https://youtrack.jetbrains.com/issue/KT-85167) Make Xjsr305 type safe
- [`KT-85069`](https://youtrack.jetbrains.com/issue/KT-85069) Make Xnullability-annotations type safe
- [`KT-84738`](https://youtrack.jetbrains.com/issue/KT-84738) Make Xscript-resolver-environment type safe
- [`KT-85082`](https://youtrack.jetbrains.com/issue/KT-85082) Make Xignored-annotations-for-bridges type safe
- [`KT-84324`](https://youtrack.jetbrains.com/issue/KT-84324) Make X_ADD_MODULES BTA compiler argument type safe
- [`KT-84338`](https://youtrack.jetbrains.com/issue/KT-84338) Make enum BTA JVM compiler argument type safe
- [`KT-84325`](https://youtrack.jetbrains.com/issue/KT-84325) Make JVM_DEFAULT BTA compiler argument type safe
- [`KT-75837`](https://youtrack.jetbrains.com/issue/KT-75837) IC: Shrunk classpath snapshot name is hardcoded
- [`KT-84228`](https://youtrack.jetbrains.com/issue/KT-84228) BTA: Improving KDoc generation for Enums and Custom Types
- [`KT-85738`](https://youtrack.jetbrains.com/issue/KT-85738) BTA forward compatibility: NoSuchFieldError on X_IGNORED_ANNOTATIONS_FOR_BRIDGES when API 2.3.0 is used with impl 2.4.0
- [`KT-84436`](https://youtrack.jetbrains.com/issue/KT-84436) Сompiler warnings are missing under Gradle -q option with -Werror
- [`KT-85072`](https://youtrack.jetbrains.com/issue/KT-85072) AbstractMethodError when calling discoverScriptExtensionsOperationBuilder with pre-2.4.0 compiler
- [`KT-84523`](https://youtrack.jetbrains.com/issue/KT-84523) Add more forward compatibility tests
- [`KT-84187`](https://youtrack.jetbrains.com/issue/KT-84187) [BTA] Add more build operation immutability tests
- [`KT-84219`](https://youtrack.jetbrains.com/issue/KT-84219) [BTA] Add additional tests on basic metrics collection
- [`KT-83781`](https://youtrack.jetbrains.com/issue/KT-83781) Add additional tests for KT-79975 (BTA ability to cancel build operations)
- [`KT-85092`](https://youtrack.jetbrains.com/issue/KT-85092) [BTA] Update BTA Backward Compatibility Testing: 2.3.20-RC → 2.3.20
- [`KT-85439`](https://youtrack.jetbrains.com/issue/KT-85439) BTA: Warn or error when incompatible compiler arguments are passed via applyArgumentStrings
- [`KT-75540`](https://youtrack.jetbrains.com/issue/KT-75540) Build Tools API Should Reject -Xbuild-file Argument
- [`KT-85333`](https://youtrack.jetbrains.com/issue/KT-85333) Add BTA tests for BACKUP_CLASSES and KEEP_IC_CACHES_IN_MEMORY behavior after compilation error
- [`KT-84770`](https://youtrack.jetbrains.com/issue/KT-84770) BTA: default options cannot be retrieved from many option objects
- [`KT-85224`](https://youtrack.jetbrains.com/issue/KT-85224) Add `@ExperimentalArgumentApi` to compiler argument DSL types
- [`KT-84953`](https://youtrack.jetbrains.com/issue/KT-84953) Fail TC build if generated files change
- [`KT-85189`](https://youtrack.jetbrains.com/issue/KT-85189) Refactor path argument types: flatten hierarchy and improve naming
- [`KT-82335`](https://youtrack.jetbrains.com/issue/KT-82335) Promote the deprecation level for BTA prototype to the ERROR level
- [`KT-84984`](https://youtrack.jetbrains.com/issue/KT-84984) Runtime NPEs caused by null return in CompilerMessageRenderer implementation
- [`KT-84015`](https://youtrack.jetbrains.com/issue/KT-84015) Introduce detection of custom script names to new BTA API
- [`KT-83972`](https://youtrack.jetbrains.com/issue/KT-83972) BTA: use isolated classloader for loading the BTA implementation in integration tests
- [`KT-84181`](https://youtrack.jetbrains.com/issue/KT-84181) More verbose warning when CRI is enabled without using BTA
- [`KT-84449`](https://youtrack.jetbrains.com/issue/KT-84449) Platform-Specific File.pathSeparator Hardcoded During SSOT Generation
- [`KT-84249`](https://youtrack.jetbrains.com/issue/KT-84249) Fix hardcoded path separator in -Xprofile argument to support absolute paths on Windows

### Tools. CLI

#### Fixes

- [`KT-84609`](https://youtrack.jetbrains.com/issue/KT-84609) Remove Nullability from Array-based CLI Compiler Arguments
- [`KT-85414`](https://youtrack.jetbrains.com/issue/KT-85414) Argument DSL: `delimiter = KotlinCompilerArgument.Delimiter.PathSeparator` generates invalid Kotlin code
- [`KT-85004`](https://youtrack.jetbrains.com/issue/KT-85004) Set proper since version for language feature about property annotation targeting
- [`KT-84220`](https://youtrack.jetbrains.com/issue/KT-84220) Enable Context Parameters by default in LV 2.4
- [`KT-84188`](https://youtrack.jetbrains.com/issue/KT-84188) Create CLI argument for explicit context parameters
- [`KT-84999`](https://youtrack.jetbrains.com/issue/KT-84999) Don't poison binaries with `ImplicitJvmExposeBoxed` language feature
- [`KT-85001`](https://youtrack.jetbrains.com/issue/KT-85001) Convert `ImplicitJvmExposeBoxed` language feature to analysis flag
- [`KT-83341`](https://youtrack.jetbrains.com/issue/KT-83341) Don't use the extension point registration mechanism from Intellij for K2 extensions
- [`KT-56850`](https://youtrack.jetbrains.com/issue/KT-56850) Separate K/Wasm CLI entry point from K/JS CLI
- [`KT-84132`](https://youtrack.jetbrains.com/issue/KT-84132) CLI: regression in deduplication of same-value arguments
- [`KT-83261`](https://youtrack.jetbrains.com/issue/KT-83261) No error if pass an arbitrary string to a CLI argument that changes language features 
- [`KT-83172`](https://youtrack.jetbrains.com/issue/KT-83172) Boolean CLI argument for a language feature with explicit false value is allowed but has no effect

### Tools. CLI. Native

- [`KT-82482`](https://youtrack.jetbrains.com/issue/KT-82482) Compiler plugins are not propagated to frontend environment in ONE_STAGE_MULTI_MODULE Native mode

### Tools. Compiler Plugin API

- [`KT-85969`](https://youtrack.jetbrains.com/issue/KT-85969) KtLint incompatible with Kotlin 2.4.0-Beta2 (parsing errors / Extensions storage issue)
- [`KT-85133`](https://youtrack.jetbrains.com/issue/KT-85133) Drop deprecated K1 specific methods from IrPluginContext

### Tools. Compiler Plugins

#### New Features

- [`KT-66808`](https://youtrack.jetbrains.com/issue/KT-66808) PowerAssert: Add support for third-party assertion libraries
- [`KT-69036`](https://youtrack.jetbrains.com/issue/KT-69036) Power-Assert indent multiline values
- [`KT-75656`](https://youtrack.jetbrains.com/issue/KT-75656) PowerAssert: Create runtime library

#### Fixes

- [`KT-83931`](https://youtrack.jetbrains.com/issue/KT-83931) Power Assert: Compilation fails when using the metro plugin
- [`KT-86170`](https://youtrack.jetbrains.com/issue/KT-86170) PowerAssert: Stabilize runtime ABI for initial release
- [`KT-85250`](https://youtrack.jetbrains.com/issue/KT-85250) PowerAssert: Automatically add runtime library dependency
- [`KT-75873`](https://youtrack.jetbrains.com/issue/KT-75873) PowerAssert: display callable reference value under '::'
- [`KT-85151`](https://youtrack.jetbrains.com/issue/KT-85151) PowerAssert: Surround string and character values with quotes
- [`KT-85184`](https://youtrack.jetbrains.com/issue/KT-85184) PowerAssert: Annotation may only be used on expect and non-override functions
- [`KT-85178`](https://youtrack.jetbrains.com/issue/KT-85178) PowerAssert: Crash when transforming IrCall without CallableId
- [`KT-66807`](https://youtrack.jetbrains.com/issue/KT-66807) PowerAssert: Improve output diagram formatting
- [`KT-85089`](https://youtrack.jetbrains.com/issue/KT-85089) PowerAssert: Wasm CompileError when using `PowerAssert.explanation`
- [`KT-75266`](https://youtrack.jetbrains.com/issue/KT-75266) PowerAssert: arrayOf() isn't displayed on the diagram
- [`KT-67332`](https://youtrack.jetbrains.com/issue/KT-67332) "IndexOutOfBoundsException: Cannot pop operand off an empty stack." caused by function reference                                                                             
- [`KT-83330`](https://youtrack.jetbrains.com/issue/KT-83330) Lombok.  An add methods with `@Singular` annotation in Java record doesn't work from kotlin
- [`KT-83204`](https://youtrack.jetbrains.com/issue/KT-83204) Lombok. If `@Data` and `@NoArgsConstructor` are used together, then the constructor from `@Data` shouldn't be available
- [`KT-83336`](https://youtrack.jetbrains.com/issue/KT-83336) Lombok. IllegalAccessError for constructor if `@Value` and `@Builder` are applied and used from another package
- [`KT-83352`](https://youtrack.jetbrains.com/issue/KT-83352) Lombok. FileAnalysisException when `@SuperBuilder` is used with `@Builder`
- [`KT-83325`](https://youtrack.jetbrains.com/issue/KT-83325) Lombok. Constructor with parameters is unavailable for a class with `@Builder`

### Tools. Compiler plugins. Serialization

- [`KT-85963`](https://youtrack.jetbrains.com/issue/KT-85963) `IncompatibleClassChangeError: Expected non-static field $stable` on deserialization of `@Serializable` data class when Compose compiler plugin is applied before Serialization plugin
- [`KT-85554`](https://youtrack.jetbrains.com/issue/KT-85554) Serialization: "IndexOutOfBoundsException" on property generated by Compose plugin

### Tools. Gradle

#### New Features

- [`KT-76197`](https://youtrack.jetbrains.com/issue/KT-76197) Write Kotlin compiler warnings and errors to Problems API

#### Fixes

- [`KT-86346`](https://youtrack.jetbrains.com/issue/KT-86346) Compiler warnings printed twice in console — once as `w:` and again as Gradle "Problem found:" block
- [`KT-69701`](https://youtrack.jetbrains.com/issue/KT-69701) Gradle: module name is passed inconsistently to different types of compilations
- [`KT-85373`](https://youtrack.jetbrains.com/issue/KT-85373) Compile against Gradle API 9.5.0
- [`KT-85374`](https://youtrack.jetbrains.com/issue/KT-85374) Run tests against Gradle 9.5.0
- [`KT-80448`](https://youtrack.jetbrains.com/issue/KT-80448) Remove internal & deprecated API from ExtrasProperty.kt
- [`KT-69830`](https://youtrack.jetbrains.com/issue/KT-69830) Support Gradle `com.gradle.develocity` plugin in KGP
- [`KT-85509`](https://youtrack.jetbrains.com/issue/KT-85509) Remove deprecated API in the 2.4.0 release
- [`KT-83858`](https://youtrack.jetbrains.com/issue/KT-83858) Compatibility with Gradle 9.4.0 release
- [`KT-84144`](https://youtrack.jetbrains.com/issue/KT-84144) Bump the minimal supported AGP version to 8.5.2
- [`KT-85562`](https://youtrack.jetbrains.com/issue/KT-85562) Failed to invoke getCompilation on org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink_Decorated on project import
- [`KT-85590`](https://youtrack.jetbrains.com/issue/KT-85590) Gradle: cannot create task MainKt.main() due to missing defaultSourceSetName
- [`KT-74451`](https://youtrack.jetbrains.com/issue/KT-74451) Deprecate access to Kotlin source sets in Android extension
- [`KT-82960`](https://youtrack.jetbrains.com/issue/KT-82960) Remove deprecated enableKotlinToolingMetadataArtifact in 2.4.0
- [`KT-84377`](https://youtrack.jetbrains.com/issue/KT-84377) Broken package-list file on KGP/CMPG documentation page
- [`KT-85412`](https://youtrack.jetbrains.com/issue/KT-85412) Module name is not sanitized with older Kotlin compiler versions
- [`KT-65566`](https://youtrack.jetbrains.com/issue/KT-65566) Use the new ConfigurationContainer consumable method to create consumable configurations
- [`KT-83775`](https://youtrack.jetbrains.com/issue/KT-83775) Migrate KGP functionalTest to junit5
- [`KT-85433`](https://youtrack.jetbrains.com/issue/KT-85433) Gradle: deprecate non-BTA JVM compiler execution mode
- [`KT-83860`](https://youtrack.jetbrains.com/issue/KT-83860) Run tests against Gradle 9.4.0
- [`KT-75004`](https://youtrack.jetbrains.com/issue/KT-75004) KGP: improve messaging when multiplatform tasks are disabled on incompatible OSes
- [`KT-83859`](https://youtrack.jetbrains.com/issue/KT-83859) Compile against Gradle API 9.4.0
- [`KT-82847`](https://youtrack.jetbrains.com/issue/KT-82847) Raise deprecation to error for LanguageSettings.enableLanguageFeature DSL
- [`KT-84053`](https://youtrack.jetbrains.com/issue/KT-84053) Deprecate support for Gradle 7.6-8.13 versions
- [`KT-78659`](https://youtrack.jetbrains.com/issue/KT-78659) Remove 'kotlin-android-extensions' plugin id
- [`KT-79924`](https://youtrack.jetbrains.com/issue/KT-79924) Make enableKotlinToolingMetadataArtifact deprecated
- [`KT-82933`](https://youtrack.jetbrains.com/issue/KT-82933) Add a tab with results in TC
- [`KT-83130`](https://youtrack.jetbrains.com/issue/KT-83130) [ToolingDiagnostic] incorrect problem ID formatting for acronyms and undefined locations in Gradle8 problems reports
- [`KT-84143`](https://youtrack.jetbrains.com/issue/KT-84143) Reduce usage of Project in Tooling Diagnostics
- [`KT-83126`](https://youtrack.jetbrains.com/issue/KT-83126) Remove out-of-process compilation mode
- [`KT-80466`](https://youtrack.jetbrains.com/issue/KT-80466) Gradle: remove getPluginArtifactForNative()
- [`KT-81834`](https://youtrack.jetbrains.com/issue/KT-81834) Compile against AGP 8.13 API
- [`KT-77498`](https://youtrack.jetbrains.com/issue/KT-77498) Test .swiftmodules more accurate in SwiftExportIT
- [`KT-84141`](https://youtrack.jetbrains.com/issue/KT-84141) Add convenient host check
- [`KT-83592`](https://youtrack.jetbrains.com/issue/KT-83592) Enable AFU in FusStatisticsIT.testKotlinxPlugins test after next AFU release

### Tools. Gradle. BCV

- [`KT-86268`](https://youtrack.jetbrains.com/issue/KT-86268) ABI validation tasks fail with Unsupported platform toolchain type when using kotlin.compilerVersion pointing to an older compiler after BTA migration in 2.4.0-Beta2
- [`KT-80685`](https://youtrack.jetbrains.com/issue/KT-80685) Simplify Gradle DSL [ABI Validation]
- [`KT-83486`](https://youtrack.jetbrains.com/issue/KT-83486) Create tasks only if abiValidation block called explicitly [ABI Validation]
- [`KT-82410`](https://youtrack.jetbrains.com/issue/KT-82410) Remove word `legacy` from  DSL [ABI Validation]
- [`KT-83999`](https://youtrack.jetbrains.com/issue/KT-83999) ABI validation: Groovy DSL doesn’t deprecate included/excluded filters, allowing four filter configs instead of two
- [`KT-84461`](https://youtrack.jetbrains.com/issue/KT-84461) Remove the use of abi-tools-api from KGP [ABI Validation]
- [`KT-84100`](https://youtrack.jetbrains.com/issue/KT-84100) Add Deprecated annotation to legacyDump block and property [ABI Validation]
- [`KT-84365`](https://youtrack.jetbrains.com/issue/KT-84365) Gradle plugin of abi-validation should precisely define output files
- [`KT-83898`](https://youtrack.jetbrains.com/issue/KT-83898) Classes produced by JvmMultifileClass ignore filters

### Tools. Gradle. Compiler plugins

- [`KT-85343`](https://youtrack.jetbrains.com/issue/KT-85343) Update Compose Gradle plugin deprecations before 2.4

### Tools. Gradle. Dokka

- [`KT-82984`](https://youtrack.jetbrains.com/issue/KT-82984) Support AGP9 in Dokka Gradle Plugin

### Tools. Gradle. JS

- [`KT-81037`](https://youtrack.jetbrains.com/issue/KT-81037) K/JS, Wasm: Remove deprecated internal JS functions
- [`KT-81034`](https://youtrack.jetbrains.com/issue/KT-81034) K/JS, Wasm: Remove deprecated public constructors of JS declarations
- [`KT-81030`](https://youtrack.jetbrains.com/issue/KT-81030) K/JS, Wasm: remove deprecated NodeJsExec.create
- [`KT-81036`](https://youtrack.jetbrains.com/issue/KT-81036) K/JS, Wasm: Remove deprecated ExperimentalDceDsl
- [`KT-81040`](https://youtrack.jetbrains.com/issue/KT-81040) Gradle: Remove deprecated Kotlin/JS tasks constructors
- [`KT-64275`](https://youtrack.jetbrains.com/issue/KT-64275) Gradle: remove deprecated symbols related to the legacy JS target
- [`KT-84753`](https://youtrack.jetbrains.com/issue/KT-84753) Deprecate `KotlinJsCompilerType` and `KotlinProjectExtension` methods using it
- [`KT-81033`](https://youtrack.jetbrains.com/issue/KT-81033) K/JS, Wasm: Remove deprecated wasm declarations in "js" package 

### Tools. Gradle. Multiplatform

#### Fixes

- [`KT-84767`](https://youtrack.jetbrains.com/issue/KT-84767) K/N: associateWith triggers warning about friend-modules libs not included in -library argument
- [`KT-82265`](https://youtrack.jetbrains.com/issue/KT-82265) Remove Android source set layout v1
- [`KT-82230`](https://youtrack.jetbrains.com/issue/KT-82230) Cleanup 'org.jetbrains.gradle.apple.applePlugin' plugin usage
- [`KT-81117`](https://youtrack.jetbrains.com/issue/KT-81117) With `android.builtInKotlin=true` (AGP 9.0), using `kotlin-multiplatform` plugin will fail with `Cannot add extension with name 'kotlin'`
- [`KT-86174`](https://youtrack.jetbrains.com/issue/KT-86174) Project sync from cached checkout fails in convertSyntheticImportProjectIntoDefFileIphoneos with 'fatal: Needed a single revision'
- [`KT-85877`](https://youtrack.jetbrains.com/issue/KT-85877) The number of SPM direct dependencies is multiplied on number on targets
- [`KT-85706`](https://youtrack.jetbrains.com/issue/KT-85706) SwiftPM Import: Updating package version in build script updates version in the lock file
- [`KT-84085`](https://youtrack.jetbrains.com/issue/KT-84085) Remove deprecated gradle property kotlin.kmp.isolated-projects.support
- [`KT-83370`](https://youtrack.jetbrains.com/issue/KT-83370) Incorrect metadata transformation for stdlib's webMain source set
- [`KT-69571`](https://youtrack.jetbrains.com/issue/KT-69571) compileNativeMainKotlinMetadata not handling project/prebuilt substitutions
- [`KT-84533`](https://youtrack.jetbrains.com/issue/KT-84533) KMP: compileCommonMainKotlinMetadata: "Unresolved reference" for androidx.savedstate from Maven (works with project() dependency)
- [`KT-84669`](https://youtrack.jetbrains.com/issue/KT-84669) SPM import: If iosApp dir located outside of the project, checkSyntheticImportProjectIsCorrectlyIntegrated will fail
- [`KT-84597`](https://youtrack.jetbrains.com/issue/KT-84597) Remove trailing comma for dependencies blocks settings in Package.swift
- [`KT-82895`](https://youtrack.jetbrains.com/issue/KT-82895) kotlin-stdlib import is flaky in commonTest in 2.1.21
- [`KT-81958`](https://youtrack.jetbrains.com/issue/KT-81958) Redundant “android target already exists” error when migrating to com.android.kotlin.multiplatform.library with androidTarget {}

### Tools. Gradle. Native

#### New Features

- [`KT-83873`](https://youtrack.jetbrains.com/issue/KT-83873) Redo how dynamic library linkage and promotion are handled
- [`KT-83863`](https://youtrack.jetbrains.com/issue/KT-83863) Persist Package.resolved files and reuse them in incremental SwiftPM import

#### Fixes

- [`KT-85984`](https://youtrack.jetbrains.com/issue/KT-85984) linkReleaseFrameworkIosSimulatorArm64 is executed during Debug iOS build since 2.4.0-Beta2
- [`KT-85510`](https://youtrack.jetbrains.com/issue/KT-85510) Cleanup native tasks API
- [`KT-84656`](https://youtrack.jetbrains.com/issue/KT-84656) Concurrent issue in downloadKotlinNativeDistribution
- [`KT-85502`](https://youtrack.jetbrains.com/issue/KT-85502) Swift PM Import: "Library not loaded": KotlinMultiplatformLinkedPackage.framework is not copied next to the executable
- [`KT-85561`](https://youtrack.jetbrains.com/issue/KT-85561) SwiftPM lock-files metadata resolution fails with NoMatchingGraphVariantsException in external KMP projects 
- [`KT-85128`](https://youtrack.jetbrains.com/issue/KT-85128) Refactor SwiftPM import lock tests and test utils
- [`KT-83681`](https://youtrack.jetbrains.com/issue/KT-83681) Parallelize parts of SwiftPM import pipeline that are called during import
- [`KT-83874`](https://youtrack.jetbrains.com/issue/KT-83874) Linker hack doesn't work when clang uses response files
- [`KT-86024`](https://youtrack.jetbrains.com/issue/KT-86024) Empty main compilations cause w: [COMPILER_ARGUMENTS_WARNING] There are libraries in -friend-modules CLI argument that are not included in -library CLI argument:
- [`KT-69896`](https://youtrack.jetbrains.com/issue/KT-69896) Native: output to stderr ends up in the Gradle log
- [`KT-85708`](https://youtrack.jetbrains.com/issue/KT-85708) [KGP] dSYM copy task ignores `isStatic` due to eager read before framework configuration
- [`KT-84262`](https://youtrack.jetbrains.com/issue/KT-84262) integrateEmbedAndSign produces an incorrect Gradle call for the root project
- [`KT-84730`](https://youtrack.jetbrains.com/issue/KT-84730) Add Kdocs to SwiftPM import APIs
- [`KT-84558`](https://youtrack.jetbrains.com/issue/KT-84558) Upstream SwiftPM import work
- [`KT-82824`](https://youtrack.jetbrains.com/issue/KT-82824) Make linker hack path relative
- [`KT-84508`](https://youtrack.jetbrains.com/issue/KT-84508) Add a warning on usage macos_x64 as host
- [`KT-84692`](https://youtrack.jetbrains.com/issue/KT-84692) Misleading error message for disableNativeCache DSL without required Opt-In
- [`KT-83680`](https://youtrack.jetbrains.com/issue/KT-83680) Remove trailing commas from the package manifest to be compatible with pre-16.3 Xcode

### Tools. Gradle. Wasm

- [`KT-86457`](https://youtrack.jetbrains.com/issue/KT-86457) [Wasm, Gradle] BinaryenExec.standardOutput is silently ignored after migration to Gradle Workers
- [`KT-84137`](https://youtrack.jetbrains.com/issue/KT-84137) K/Wasm: Support binaryen run with multiple files
- [`KT-85974`](https://youtrack.jetbrains.com/issue/KT-85974) K/Wasm: Do not set -Xir-per-module for Wasm tasks
- [`KT-83566`](https://youtrack.jetbrains.com/issue/KT-83566) K/Wasm: Support Wasm per module/klib compilation in Gradle plugin
- [`KT-85046`](https://youtrack.jetbrains.com/issue/KT-85046) K/Wasm: Wasm per-module Gradle integration tests on Windows
- [`KT-84230`](https://youtrack.jetbrains.com/issue/KT-84230) Wasm: Fix test WasmYarnGradlePluginIT.testWasmUsePredefinedTooling

### Tools. Incremental Compile

- [`KT-85387`](https://youtrack.jetbrains.com/issue/KT-85387) BTA: switch the default value of `MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION` to `true`
- [`KT-85386`](https://youtrack.jetbrains.com/issue/KT-85386) BTA JVM IC: 'moduleName' is null!
- [`KT-84450`](https://youtrack.jetbrains.com/issue/KT-84450) Star imports are not reported via FirImportTrackerComponent

### Tools. JPS

- [`KT-81579`](https://youtrack.jetbrains.com/issue/KT-81579) JPS: -Xwarning-level=DEPRECATION:warning not supported

### Tools. Kapt

- [`KT-84094`](https://youtrack.jetbrains.com/issue/KT-84094) Kotlin daemon holds file locks for too long
- [`KT-32743`](https://youtrack.jetbrains.com/issue/KT-32743) Kapt, Maven: Do not include compile classpath entries in the annotation processing classpath
- [`KT-41217`](https://youtrack.jetbrains.com/issue/KT-41217) Running kapt with Maven does not seem to include the compilation classpath
- [`KT-80569`](https://youtrack.jetbrains.com/issue/KT-80569) K2 KAPT: Class Literals Missing in Explicit Annotation Value Parameters
- [`KT-18791`](https://youtrack.jetbrains.com/issue/KT-18791) Kapt: Constants from R class should not be inlined

### Tools. Maven

#### New Features

- [`KT-85121`](https://youtrack.jetbrains.com/issue/KT-85121) Maven: enable configuration inputs tracking in BTA
- [`KT-84386`](https://youtrack.jetbrains.com/issue/KT-84386) Support Maven Toolchains in kotlin-maven-plugin
- [`KT-85317`](https://youtrack.jetbrains.com/issue/KT-85317) Auto‑align jvmTarget with the project’s Java level 

#### Fixes

- [`KT-74001`](https://youtrack.jetbrains.com/issue/KT-74001) Support monotonous dirtySet expansion in Maven
- [`KT-76062`](https://youtrack.jetbrains.com/issue/KT-76062) Maven: remove Kotlin script execution support
- [`KT-84101`](https://youtrack.jetbrains.com/issue/KT-84101) Maven: compile and test-compile handle sourceDirs inconsistently
- [`KT-84653`](https://youtrack.jetbrains.com/issue/KT-84653) Add integration test for KAPT with smart defaults in mixed Kotlin+Java projects
- [`KT-84778`](https://youtrack.jetbrains.com/issue/KT-84778) Add integration test for auto-bind execution order in mixed Kotlin+Java projects
- [`KT-85146`](https://youtrack.jetbrains.com/issue/KT-85146) Maven: Adding stdlib as smart-default may break maven dependency resolution for other plugins
- [`KT-84793`](https://youtrack.jetbrains.com/issue/KT-84793) Use kotlin bootstrap to build kotlin-maven-plugin
- [`KT-83109`](https://youtrack.jetbrains.com/issue/KT-83109) Remove beanshell and groovy verification in kotlin-maven-plugin-test
- [`KT-83110`](https://youtrack.jetbrains.com/issue/KT-83110) Remove dependency to intellij platform from kotlin-maven-plugin-test
- [`KT-83113`](https://youtrack.jetbrains.com/issue/KT-83113) Configure kotlin.git/.idea to work nicely with maven-kotlin-plugin-test tests 
- [`KT-83114`](https://youtrack.jetbrains.com/issue/KT-83114) Migrate kotlin-maven-plugin-test from maven.invoker to junit6 + maven-verifier

### Tools. Performance benchmarks

- [`KT-82928`](https://youtrack.jetbrains.com/issue/KT-82928) Support local run for new benchmarks infra
- [`KT-84283`](https://youtrack.jetbrains.com/issue/KT-84283) Add scenario generator for performance tests
- [`KT-83257`](https://youtrack.jetbrains.com/issue/KT-83257) Parse gradle profile report

### Tools. REPL

- [`KT-77816`](https://youtrack.jetbrains.com/issue/KT-77816) REPL: Support for `const` properties
- [`KT-74683`](https://youtrack.jetbrains.com/issue/KT-74683) [K2 Repl] Does not support suspend functions
- [`KT-84483`](https://youtrack.jetbrains.com/issue/KT-84483) [K2 Repl] NullPointerException in Analysis when using custom classes
- [`KT-84803`](https://youtrack.jetbrains.com/issue/KT-84803) [REPL] FirReplSnippet: provide the eval function symbol instead of the name (`evalFunctionName`)
- [`KT-84160`](https://youtrack.jetbrains.com/issue/KT-84160) [REPL] Resolve eval function during implicit body
- [`KT-83689`](https://youtrack.jetbrains.com/issue/KT-83689) [K2 REPL] Create raw FIR tests for repl snippets
- [`KT-82554`](https://youtrack.jetbrains.com/issue/KT-82554) [REPL] Fix unresolved reference when using dataframe compiler-plugin
- [`KT-82578`](https://youtrack.jetbrains.com/issue/KT-82578) [K2 REPL] Split snippet property declaration and initialization
- [`KT-82503`](https://youtrack.jetbrains.com/issue/KT-82503) [K2 Repl] Nested class annotations are not available in the next snippet

### Tools. Wasm

- [`KT-84396`](https://youtrack.jetbrains.com/issue/KT-84396) [Wasm] Support multimodule  in incremental compilation
- [`KT-75086`](https://youtrack.jetbrains.com/issue/KT-75086) Wasm: Deprecate and remove D8 in js packages

## Previous ChangeLogs:
### [ChangeLog-2.4.X](docs/changelogs/ChangeLog-2.4.X.md)
### [ChangeLog-2.3.X](docs/changelogs/ChangeLog-2.3.X.md)
### [ChangeLog-2.2.X](docs/changelogs/ChangeLog-2.2.X.md)
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
