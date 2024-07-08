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

- [cdfe659](https://github.com/JetBrains/kotlin/commit/cdfe65911490eef21892098494986af1af14fa64) Changed how compiler features being rolled out are enabled and disabled in compiler plugin CLI. Features, such as strong skipping and non-skipping group optimizations are now enabled through the  "featureFlag" option instead of their own option.

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