## 2.0.0-Beta3

### Analysis. API

#### Fixes

- [`KT-62895`](https://youtrack.jetbrains.com/issue/KT-62895) K2 IDE. FP `'when' expression must be exhaustive` with sealed interface from library
- [`KT-64805`](https://youtrack.jetbrains.com/issue/KT-64805) Analysis API: introduce common entry point for multi-file test cases
- [`KT-64714`](https://youtrack.jetbrains.com/issue/KT-64714) K2: Analysis API: CollectionsKt.map doesn't resolves from Java in kotlin repo
- [`KT-64647`](https://youtrack.jetbrains.com/issue/KT-64647) K2: Allow to calculate decompiled inheritors for sealed classes in tests
- [`KT-64595`](https://youtrack.jetbrains.com/issue/KT-64595) AA: stackoverflow while simplifying a type with a recursive type parameter
- [`KT-64825`](https://youtrack.jetbrains.com/issue/KT-64825) Analysis API. Cannot compute containing PSI for unknown source kind 'org.jetbrains.kotlin.KtFakeSourceElementKind$DefaultAccessor' exception on getContainingSymbol call for default setter parameter
- [`KT-64080`](https://youtrack.jetbrains.com/issue/KT-64080) K2: Analysis API: On-air resolve does not trigger resolution of delegated super call arguments
- [`KT-64243`](https://youtrack.jetbrains.com/issue/KT-64243) K2: proper lazy resolution for fake overrides
- [`KT-62891`](https://youtrack.jetbrains.com/issue/KT-62891) K2 IDE.  FP [EXPOSED_FUNCTION_RETURN_TYPE] on overriding library method which returns protected type
- [`KT-61890`](https://youtrack.jetbrains.com/issue/KT-61890) Analysis API: Migrate KtFirScopeProvider to ContextCollector instead of onAirResolve
- [`KT-64197`](https://youtrack.jetbrains.com/issue/KT-64197) K2: Code fragments are only supported in JVM
- [`KT-64604`](https://youtrack.jetbrains.com/issue/KT-64604) K2: IDE K2: "Modules are inconsistent during performance tests"
- [`KT-62357`](https://youtrack.jetbrains.com/issue/KT-62357) K2 IDE. False positive on generated component methods and false negative on getter of `@JvmRecord` classes in Java
- [`KT-62892`](https://youtrack.jetbrains.com/issue/KT-62892) K2 IDE. Java outer class from other module is not resolved when nested class is accessed with fq name in a type position
- [`KT-62888`](https://youtrack.jetbrains.com/issue/KT-62888) K2 IDE. IDE infers reference to `KMutableProperty` as reference to just `KProperty`
- [`KT-64584`](https://youtrack.jetbrains.com/issue/KT-64584) K2: StubBasedFirDeserializedSymbolProvider: support deserialization of delegated declarations
- [`KT-60324`](https://youtrack.jetbrains.com/issue/KT-60324) K2 IDE: "NoSuchElementException: List is empty at JavaOverrideChecker#buildErasure"
- [`KT-62896`](https://youtrack.jetbrains.com/issue/KT-62896) K2 IDE. FP ABSTRACT_MEMBER_NOT_IMPLEMENTED on inheriting class from library which implements interface by delegation
- [`KT-62947`](https://youtrack.jetbrains.com/issue/KT-62947) Analysis API: Error while resolving FirPropertyImpl
- [`KT-64468`](https://youtrack.jetbrains.com/issue/KT-64468) Analysis API: Implement mixed multi-module tests which support different kinds of `KtModule`s
- [`KT-63547`](https://youtrack.jetbrains.com/issue/KT-63547) K2 IDE. False Positive AMBIGUOUS_ANNOTATION_ARGUMENT
- [`KT-62832`](https://youtrack.jetbrains.com/issue/KT-62832) K2: ClassCastException: FirDeclarationStatusImpl cannot be cast to FirResolvedDeclarationStatus
- [`KT-64205`](https://youtrack.jetbrains.com/issue/KT-64205) Analysis API: Do not import non-top-level callables by default
- [`KT-63056`](https://youtrack.jetbrains.com/issue/KT-63056) K2: Cannot mutate an immutable ImplicitReceiverValue on FirCodeFragment analysis
- [`KT-64108`](https://youtrack.jetbrains.com/issue/KT-64108) K2: KtFirSymbolDeclarationOverridesProvider shouldn't provide fake overrides
- [`KT-63752`](https://youtrack.jetbrains.com/issue/KT-63752) K2: java.lang.StackOverflowError FirFieldSymbol.getHasInitializer
- [`KT-63718`](https://youtrack.jetbrains.com/issue/KT-63718) Analysis API: Stub-based dependency symbol providers of library source sessions compute the wrong package name sets
- [`KT-64186`](https://youtrack.jetbrains.com/issue/KT-64186) Analysis API: ContextCollector provides incorrect scopes for anonymous objects
- [`KT-63979`](https://youtrack.jetbrains.com/issue/KT-63979) K2 IDE: presentation of types in completion is too verbose
- [`KT-63681`](https://youtrack.jetbrains.com/issue/KT-63681) K2: LL FIR: Improve isResolved check coverage of after lazy resolution

### Analysis. Light Classes

- [`KT-63087`](https://youtrack.jetbrains.com/issue/KT-63087) K2 IDE: in .java source reference to JvmName names on unsigned type / value class are unresolved
- [`KT-64605`](https://youtrack.jetbrains.com/issue/KT-64605) K2 IDE: usage of `@Repeatable` annotation in Java: false positive "Duplicate annotation"
- [`KT-64795`](https://youtrack.jetbrains.com/issue/KT-64795) SLC: distinguish last v.s. non-last `vararg` value parameter type during binary resolution
- [`KT-61605`](https://youtrack.jetbrains.com/issue/KT-61605) K2 IDE: Light elements do not obey platform contracts
- [`KT-57536`](https://youtrack.jetbrains.com/issue/KT-57536) SLC: no need to populate members with `expect` modifier
- [`KT-63949`](https://youtrack.jetbrains.com/issue/KT-63949) K2 IDE. Analyze hang on `@Autowired` constructor analysis
- [`KT-64320`](https://youtrack.jetbrains.com/issue/KT-64320) Decouple kotlin psi from java PSI
- [`KT-64282`](https://youtrack.jetbrains.com/issue/KT-64282) Decouple KotlinIconProviderService from java PSI

### Apple Ecosystem

- [`KT-63821`](https://youtrack.jetbrains.com/issue/KT-63821) Copy framework to BUILT_PRODUCTS_DIR in the embedAndSign task

### Backend. Wasm

- [`KT-58852`](https://youtrack.jetbrains.com/issue/KT-58852) WASM: two methods with different varargs: Class korlibs.template.dynamic.DynamicShape has 2 methods with the same signature [register(kotlin.Array<T of kotlin.Array>)

### Compiler

#### New Features

- [`KT-4113`](https://youtrack.jetbrains.com/issue/KT-4113) Smart casts for properties to not-null functional types at `invoke` calls

#### Fixes

- [`KT-64261`](https://youtrack.jetbrains.com/issue/KT-64261) K2 / WASM: Extension function with star projection throws "RuntimeError: unreachable"
- [`KT-64877`](https://youtrack.jetbrains.com/issue/KT-64877) K2: PCLA doesn't allow infer types from value parameter having TV type
- [`KT-63932`](https://youtrack.jetbrains.com/issue/KT-63932) K2/Native codegen test failures around builder inference
- [`KT-64222`](https://youtrack.jetbrains.com/issue/KT-64222) K2: "return type is not a subtype of the return type of the overridden member"
- [`KT-57094`](https://youtrack.jetbrains.com/issue/KT-57094) K1: wrong type inferred for an instance of a local class inside a generic property
- [`KT-62069`](https://youtrack.jetbrains.com/issue/KT-62069) K2: ASSIGNMENT_TYPE_MISMATCH is reported in addition to NO_ELSE_IN_WHEN
- [`KT-62776`](https://youtrack.jetbrains.com/issue/KT-62776) FirLazyResolveContractViolationException: "lazyResolveToPhase(STATUS) cannot be called from a transformer with a phase TYPES" on Java annotation usage
- [`KT-60056`](https://youtrack.jetbrains.com/issue/KT-60056) K2: Introduced UNRESOLVED_REFERENCE
- [`KT-59791`](https://youtrack.jetbrains.com/issue/KT-59791) K2: Implement partially constrained lambda analysis
- [`KT-42020`](https://youtrack.jetbrains.com/issue/KT-42020) Psi2ir: IllegalStateException: "IrSimpleFunctionPublicSymbolImpl for public [...] is already bound" on generic function whose substitution leads to IdSignature clash
- [`KT-64771`](https://youtrack.jetbrains.com/issue/KT-64771) Investigate subtle FIR_DUMP difference for reversed order analysis
- [`KT-62584`](https://youtrack.jetbrains.com/issue/KT-62584) K2: different signature in subclass of local class declared in extension value getter
- [`KT-64615`](https://youtrack.jetbrains.com/issue/KT-64615) Inconsistent error messages for platform type nullability assertions
- [`KT-59938`](https://youtrack.jetbrains.com/issue/KT-59938) K2: Disappeared AMBIGUOUS_ACTUALS
- [`KT-64501`](https://youtrack.jetbrains.com/issue/KT-64501) K2: False-positive WRONG_INVOCATION_KIND when using default arguments
- [`KT-64640`](https://youtrack.jetbrains.com/issue/KT-64640) Prevent mutating SequenceCollection methods from JDK 21 be available on read-only collections
- [`KT-63644`](https://youtrack.jetbrains.com/issue/KT-63644) K2: Create special IR symbols for fake-overrides in fir2ir in mode with IR f/o generator
- [`KT-62476`](https://youtrack.jetbrains.com/issue/KT-62476) K2: Enable building fake overrides by ir on non-JVM targets
- [`KT-63638`](https://youtrack.jetbrains.com/issue/KT-63638) K2: Compiler crashes with "Inline class types should have the same representation"
- [`KT-36220`](https://youtrack.jetbrains.com/issue/KT-36220) NI: false positive NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE if one use cannot resolve
- [`KT-64121`](https://youtrack.jetbrains.com/issue/KT-64121) K2: Actual modifier is missed on `override fun toString()` fro value class in native
- [`KT-63703`](https://youtrack.jetbrains.com/issue/KT-63703) K2: Eliminate call to Candidate.usesSAM and samResolver.getFunctionTypeForPossibleSamType in AbstractConeCallConflictResolver.toTypeWithConversion
- [`KT-64435`](https://youtrack.jetbrains.com/issue/KT-64435) K2: FIR2IR: Source offsets for data class members are inconsistent with PSI2IR
- [`KT-64090`](https://youtrack.jetbrains.com/issue/KT-64090) K2: false-positive new inference error on invoking from another module a generic function on Java list type with wildcard type argument bounded by raw-typed Java inner class
- [`KT-64044`](https://youtrack.jetbrains.com/issue/KT-64044) K2: Java mapped method should have a source from Java method, not from mapped Kotlin source class
- [`KT-46674`](https://youtrack.jetbrains.com/issue/KT-46674) ClassCastException with smartcast if `plus` operator returns a different type
- [`KT-59369`](https://youtrack.jetbrains.com/issue/KT-59369) K2: Missing BUILDER_INFERENCE_STUB_RECEIVER
- [`KT-64644`](https://youtrack.jetbrains.com/issue/KT-64644) K2: Compiler crash in FirTypeParameterBoundsChecker
- [`KT-64312`](https://youtrack.jetbrains.com/issue/KT-64312) K2: FirPropertySymbol.hasBackingField() always returns true for properties from other modules
- [`KT-64420`](https://youtrack.jetbrains.com/issue/KT-64420) K2: Wrong module descriptor for builtin classes
- [`KT-64127`](https://youtrack.jetbrains.com/issue/KT-64127) K2: incorrect resolution of inherited members on Java classes inheriting classes from different packages in the presence of identically named classes in the same packages
- [`KT-63446`](https://youtrack.jetbrains.com/issue/KT-63446) IrFakeOverrideBuilder: AbstractMethodError due to missing bridge for generic method in a Java superclass
- [`KT-63441`](https://youtrack.jetbrains.com/issue/KT-63441) IrFakeOverrideBuilder: "accidental override" when implementing a Java function taking an array parameter
- [`KT-63867`](https://youtrack.jetbrains.com/issue/KT-63867) K2: Smartcast is allowed inside changing lambda with cycles
- [`KT-63414`](https://youtrack.jetbrains.com/issue/KT-63414) K2 / Contracts: false positive "Result has wrong invocation kind" when invoking a function returning a value with contract InvocationKind.EXACTLY_ONCE and try/finally
- [`KT-63777`](https://youtrack.jetbrains.com/issue/KT-63777) K2: Smartcast is allowed inside changing lambda with bounds
- [`KT-64059`](https://youtrack.jetbrains.com/issue/KT-64059) K2: CYCLIC_INHERITANCE_HIERARCHY while using nested annotation in an outer class declaration
- [`KT-63528`](https://youtrack.jetbrains.com/issue/KT-63528) K2: Missing UNNECESSARY_SAFE_CALL for warning level annotated java declarations
- [`KT-64607`](https://youtrack.jetbrains.com/issue/KT-64607) K2: extension functions on UInt and Number lead to JVM ClassCastException
- [`KT-62816`](https://youtrack.jetbrains.com/issue/KT-62816) K2: Annotation use site targets printing could be improved in diagnostics' messages
- [`KT-62815`](https://youtrack.jetbrains.com/issue/KT-62815) K2: FIR renderings leak through some diagnostics' message
- [`KT-35289`](https://youtrack.jetbrains.com/issue/KT-35289) Confusing warning message "Duplicate label in when"
- [`KT-49084`](https://youtrack.jetbrains.com/issue/KT-49084) Contracts: error message is unclear
- [`KT-63228`](https://youtrack.jetbrains.com/issue/KT-63228) K2: Upper bound violation diagnostic renders compiler internals about SourceAttribute
- [`KT-62386`](https://youtrack.jetbrains.com/issue/KT-62386) K2: Proofread quotes in diagnostic messages
- [`KT-64081`](https://youtrack.jetbrains.com/issue/KT-64081) K2: Incorrect smartcast candidate calculation in MemberScopeTowerLevel
- [`KT-63994`](https://youtrack.jetbrains.com/issue/KT-63994) K2: Investigate K2 failures in IntelliJ-Rust plugin
- [`KT-58767`](https://youtrack.jetbrains.com/issue/KT-58767) Inheritance opt-in enforcement via `@SubclassOptInRequired` can be avoided with type aliases
- [`KT-63941`](https://youtrack.jetbrains.com/issue/KT-63941) K2: "IllegalStateException: Unsupported compile-time value STRING_CONCATENATION" caused by class reference in string expression as annotation parameter
- [`KT-59818`](https://youtrack.jetbrains.com/issue/KT-59818) K2: Explore the TODO about suspend functions overridden in Java in FirHelpers
- [`KT-63233`](https://youtrack.jetbrains.com/issue/KT-63233) K2 : false negative `Class is not abstract and does not implement abstract member` with abstract suspend function
- [`KT-63379`](https://youtrack.jetbrains.com/issue/KT-63379) K2. Argument type mismatch on creating functional interface instance with function literal as an argument with `in` type projection
- [`KT-64308`](https://youtrack.jetbrains.com/issue/KT-64308) K2: prefer call with Unit conversion at lower level to one without Unit conversion at upper level
- [`KT-64307`](https://youtrack.jetbrains.com/issue/KT-64307) K2: prefer function with default arguments at lower level to one without them at upper level during callable reference resolve
- [`KT-64306`](https://youtrack.jetbrains.com/issue/KT-64306) K2: prefer SAM at lower level to a functional type at upper level
- [`KT-63827`](https://youtrack.jetbrains.com/issue/KT-63827) K2: Array += desugaring doesn't have origin
- [`KT-64341`](https://youtrack.jetbrains.com/issue/KT-64341) Kotlin/JVM: Missing line number generation for intrinsic comparisons
- [`KT-64238`](https://youtrack.jetbrains.com/issue/KT-64238) Add proper documentation to the `IdeCodegenSettings` class
- [`KT-63667`](https://youtrack.jetbrains.com/issue/KT-63667) K2/KMP: exception when expect property matched to java field
- [`KT-63563`](https://youtrack.jetbrains.com/issue/KT-63563) K2: False negative RETURN_TYPE_MISMATCH with empty return
- [`KT-62525`](https://youtrack.jetbrains.com/issue/KT-62525) K2: IllegalStateException: Can't find KotlinType in IrErrorType: IrErrorType(null)
- [`KT-57427`](https://youtrack.jetbrains.com/issue/KT-57427) Fix inconsistencies in name manglers that use different declaration representations
- [`KT-57755`](https://youtrack.jetbrains.com/issue/KT-57755) K2/JVM: Fix computing a "signature" mangled name for the `main` function
- [`KT-63645`](https://youtrack.jetbrains.com/issue/KT-63645) K2: Replace special f/o symbols with normal ones after actualization
- [`KT-63076`](https://youtrack.jetbrains.com/issue/KT-63076) K2: change in behavior for synthetic properties in Kotlin-Java hierarchy
- [`KT-63723`](https://youtrack.jetbrains.com/issue/KT-63723) Frontend manglers improperly handle error type
- [`KT-63738`](https://youtrack.jetbrains.com/issue/KT-63738) K2: Some declarations are missing in the hierarchy of overridden symbols
- [`KT-62242`](https://youtrack.jetbrains.com/issue/KT-62242) K2: Uniformly treat enum entries as anonymous objects
- [`KT-62281`](https://youtrack.jetbrains.com/issue/KT-62281) K2: build DuckDuckGo Android user project and pass it to CI
- [`KT-60266`](https://youtrack.jetbrains.com/issue/KT-60266) K2: origin is not set for FOR_LOOP_ITERATOR
- [`KT-59875`](https://youtrack.jetbrains.com/issue/KT-59875) K2: Disappeared UNRESOLVED_REFERENCE_WRONG_RECEIVER
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
- [`KT-63530`](https://youtrack.jetbrains.com/issue/KT-63530) K2: Disable passing data flow info from in-place lambdas
- [`KT-60958`](https://youtrack.jetbrains.com/issue/KT-60958) K2: smart cast does not work with definite return from if block
- [`KT-60090`](https://youtrack.jetbrains.com/issue/KT-60090) K2: Introduced DEPRECATED_PARCELER
- [`KT-59949`](https://youtrack.jetbrains.com/issue/KT-59949) K2: Disappeared DEPRECATED_PARCELER
- [`KT-61768`](https://youtrack.jetbrains.com/issue/KT-61768) Wrong bytecode index in LineNumberTable when there is an incremental operation
- [`KT-64045`](https://youtrack.jetbrains.com/issue/KT-64045) K2: "Expect declaration * is incompatible with actual" when function parameter names are different
- [`KT-62018`](https://youtrack.jetbrains.com/issue/KT-62018) K2: prohibit suspend-marked anonymous function declarations in statement positions
- [`KT-63973`](https://youtrack.jetbrains.com/issue/KT-63973) K2: "NoSuchElementException: Array is empty" with vararg used within tail recursive function
- [`KT-63612`](https://youtrack.jetbrains.com/issue/KT-63612) K2: Class is not abstract and does not implement abstract member
- [`KT-61792`](https://youtrack.jetbrains.com/issue/KT-61792) KMP: Backend error on `@Deprecated` usage with DeprecationLevel.HIDDEN in K2
- [`KT-63709`](https://youtrack.jetbrains.com/issue/KT-63709) K2: Argument smartcasting impacting receiver and call resolution for implicit invoke
- [`KT-57788`](https://youtrack.jetbrains.com/issue/KT-57788) Fix computing mangled names of types with `@EnhancedNullability` from IR-based declaration descriptors
- [`KT-63249`](https://youtrack.jetbrains.com/issue/KT-63249) K2: change in annotation resolve when ambiguous
- [`KT-63514`](https://youtrack.jetbrains.com/issue/KT-63514) ISE “Inline class types should have the same representation: [I != I” during compilation on submitting UIntArray to vararg
- [`KT-62553`](https://youtrack.jetbrains.com/issue/KT-62553) K2: Add `topLevelClassifierPackageNames` to symbol name providers
- [`KT-64148`](https://youtrack.jetbrains.com/issue/KT-64148) K2: class cast exception org.jetbrains.kotlin.fir.types.ConeStarProjection
- [`KT-63665`](https://youtrack.jetbrains.com/issue/KT-63665) K2: "NullPointerException" caused by class with the companion object and extra curly brace
- [`KT-59715`](https://youtrack.jetbrains.com/issue/KT-59715) K2: Check behaviour of property + operator in operator position
- [`KT-62347`](https://youtrack.jetbrains.com/issue/KT-62347) Prohibit using property+invoke convention for delegated properties
- [`KT-59421`](https://youtrack.jetbrains.com/issue/KT-59421) K2: Missing CONTEXT_RECEIVERS_WITH_BACKING_FIELD
- [`KT-59903`](https://youtrack.jetbrains.com/issue/KT-59903) K2: Disappeared DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE
- [`KT-62926`](https://youtrack.jetbrains.com/issue/KT-62926) K2: IR has missing receivers during expect-actual matching
- [`KT-62565`](https://youtrack.jetbrains.com/issue/KT-62565) K2 cannot infer type parameters in case of expected functional type
- [`KT-63328`](https://youtrack.jetbrains.com/issue/KT-63328) K2: Top-level properties in scripts can be used while uninitialized
- [`KT-59683`](https://youtrack.jetbrains.com/issue/KT-59683) K2: Add control flow graph to FirScript
- [`KT-63524`](https://youtrack.jetbrains.com/issue/KT-63524) K2: "Not enough information to infer type argument"
- [`KT-63835`](https://youtrack.jetbrains.com/issue/KT-63835) K2: metadata compilation with constants is falling for Native
- [`KT-60251`](https://youtrack.jetbrains.com/issue/KT-60251) K2: delegated method are delegating to different methods in hierarchy compared to K1
- [`KT-63695`](https://youtrack.jetbrains.com/issue/KT-63695) JVM: Don't use plugin extensions when compiling code fragment
- [`KT-63574`](https://youtrack.jetbrains.com/issue/KT-63574) K2: "IllegalStateException: IrFieldPublicSymbolImpl for java.nio/ByteOrder.LITTLE_ENDIAN"
- [`KT-60504`](https://youtrack.jetbrains.com/issue/KT-60504) K2: difference between LL FIR and FIR in enhanced return type with annotation
- [`KT-64147`](https://youtrack.jetbrains.com/issue/KT-64147) K2: Generate FIR diagnostics with explicit types
- [`KT-63042`](https://youtrack.jetbrains.com/issue/KT-63042) K2: proper processing of propagated annotations
- [`KT-59368`](https://youtrack.jetbrains.com/issue/KT-59368) K2: Missing SUBTYPING_BETWEEN_CONTEXT_RECEIVERS
- [`KT-64083`](https://youtrack.jetbrains.com/issue/KT-64083) K2: "KotlinIllegalArgumentExceptionWithAttachments: Unexpected returnTypeRef. Expected is FirResolvedTypeRef, but was FirJavaTypeRef"
- [`KT-37308`](https://youtrack.jetbrains.com/issue/KT-37308) No smart cast when the null check is performed on a child property through a function with a contract
- [`KT-59894`](https://youtrack.jetbrains.com/issue/KT-59894) K2: Disappeared ANNOTATION_ARGUMENT_MUST_BE_CONST
- [`KT-63329`](https://youtrack.jetbrains.com/issue/KT-63329) K2: difference in SAM-conversion casts generation
- [`KT-64062`](https://youtrack.jetbrains.com/issue/KT-64062) K2 IDE. NPE on typing nullable parameter in return
- [`KT-58579`](https://youtrack.jetbrains.com/issue/KT-58579) K2: false-positive new inference error on invoking a generic function on Java wildcard type bounded by raw-typed Java inner class
- [`KT-64031`](https://youtrack.jetbrains.com/issue/KT-64031) K2: Revise naming in FirBuilderInferenceSession
- [`KT-50453`](https://youtrack.jetbrains.com/issue/KT-50453) Improve builder inference diagnostics with type mismatch due to chosen inapplicable overload
- [`KT-56949`](https://youtrack.jetbrains.com/issue/KT-56949) K2: Builder inference violates upper bound
- [`KT-63648`](https://youtrack.jetbrains.com/issue/KT-63648) K2: values of postponed type variable don't introduce type constraints in extension receiver positions during builder-style type inference
- [`KT-64028`](https://youtrack.jetbrains.com/issue/KT-64028) K2: Investigate questionable condition in FirBuilderInfernceSession
- [`KT-63848`](https://youtrack.jetbrains.com/issue/KT-63848) ReflectiveAccessLowering does not count arguments of super-calls
- [`KT-61920`](https://youtrack.jetbrains.com/issue/KT-61920) K2: False negative CONST_VAL_WITH_NON_CONST_INITIALIZER when initializer is Java field
- [`KT-63508`](https://youtrack.jetbrains.com/issue/KT-63508) K2: "IllegalArgumentException: Not FirResolvedTypeRef (String) in storeResult" caused by `@Deprecated` Java function and typo
- [`KT-63522`](https://youtrack.jetbrains.com/issue/KT-63522) K2: wrong context for delegated field type
- [`KT-53308`](https://youtrack.jetbrains.com/issue/KT-53308) TYPE_MISMATCH: Contracts on boolean expression has no effect on referential equality to `null`
- [`KT-51160`](https://youtrack.jetbrains.com/issue/KT-51160) Type mismatch with contracts on narrowing sealed hierarchy fail to smart cast
- [`KT-49696`](https://youtrack.jetbrains.com/issue/KT-49696) Smart cast to non-null with inline non-modifying closures sometimes doesn't work
- [`KT-46586`](https://youtrack.jetbrains.com/issue/KT-46586) SMARTCAST_IMPOSSIBLE when assigning value inside lambda instead of if expression
- [`KT-41728`](https://youtrack.jetbrains.com/issue/KT-41728) False positive no smart cast with unreachable code after return in if expression
- [`KT-22904`](https://youtrack.jetbrains.com/issue/KT-22904) Incorrect bytecode generated for withIndex iteration on Array<Int>

### IR. Actualizer

- [`KT-58861`](https://youtrack.jetbrains.com/issue/KT-58861) K2: Improve the new pipeline of FIR2IR conversion, IR actualization and fake-override generation
- [`KT-63347`](https://youtrack.jetbrains.com/issue/KT-63347) K2: Fix overridden symbols inside LazyDeclarations
- [`KT-62535`](https://youtrack.jetbrains.com/issue/KT-62535) K2: FakeOverrideRebuilder can't handle f/o without overridden symbols

### JavaScript

- [`KT-61929`](https://youtrack.jetbrains.com/issue/KT-61929) KJS: "IllegalStateException: No dispatch receiver parameter for FUN LOCAL_FUNCTION_FOR_LAMBDA" caused by `run` function in init block
- [`KT-64366`](https://youtrack.jetbrains.com/issue/KT-64366) KJS / K2: Exported declaration uses non-exportable return type: 'kotlin.<X>?'
- [`KT-64426`](https://youtrack.jetbrains.com/issue/KT-64426) K2: Implement JsIrLineNumberTestGenerated for K2
- [`KT-64422`](https://youtrack.jetbrains.com/issue/KT-64422) K2: Implement IrJsSteppingTestGenerated for K2
- [`KT-64364`](https://youtrack.jetbrains.com/issue/KT-64364) K2 / KJS: `@JSExports` generates clashing declarations for companion objects that extends its own class
- [`KT-63038`](https://youtrack.jetbrains.com/issue/KT-63038) Compilation of suspend functions into ES2015 generators

### Klibs

- [`KT-64085`](https://youtrack.jetbrains.com/issue/KT-64085) Different klib signatures for K1/K2 for overridden properties assigned in init block
- [`KT-64395`](https://youtrack.jetbrains.com/issue/KT-64395) API for ABI: Add a check for the file's existence to KLIB ABI Reader
- [`KT-63573`](https://youtrack.jetbrains.com/issue/KT-63573) K2: Dependency problems with dependencies with same artifact id
- [`KT-64082`](https://youtrack.jetbrains.com/issue/KT-64082) Different klib signatures in K1/K2 for the same locally used constant declaration
- [`KT-63931`](https://youtrack.jetbrains.com/issue/KT-63931) [K/N] Relative path to klib option of cinterop tool doesn't work
- [`KT-60390`](https://youtrack.jetbrains.com/issue/KT-60390) KLIBs: Wrong IrSymbol is used for deserialized `expect` property's backing field & accessors

### Libraries

- [`KT-61969`](https://youtrack.jetbrains.com/issue/KT-61969) Migrate kotlin-test to the current Kotlin Multiplatform Plugin
- [`KT-64361`](https://youtrack.jetbrains.com/issue/KT-64361) Optimization opportunity in Int.sign
- [`KT-63157`](https://youtrack.jetbrains.com/issue/KT-63157) Make sure that all deprecation levels are raised to ERROR for declarations intended for removal from kotlinx-metadata
- [`KT-64230`](https://youtrack.jetbrains.com/issue/KT-64230) Prohibit writing versions of metadata that are too high

### Native

- [`KT-61695`](https://youtrack.jetbrains.com/issue/KT-61695) [K/N] Empty list error in FakeOverridesActualizer with K2
- [`KT-64508`](https://youtrack.jetbrains.com/issue/KT-64508) IndexOutOfBoundsException in  Konan StaticInitializersOptimization

### Native. C and ObjC Import

- [`KT-63049`](https://youtrack.jetbrains.com/issue/KT-63049) NPE in BackendChecker.visitDelegatingConstructorCall compiling ObjC-interop class
- [`KT-59597`](https://youtrack.jetbrains.com/issue/KT-59597) [K\N] Usage of instancetype in block return type crashes

### Native. ObjC Export

- [`KT-62091`](https://youtrack.jetbrains.com/issue/KT-62091) KMP for iOS framework with private api : __NSCFBoolean
- [`KT-64076`](https://youtrack.jetbrains.com/issue/KT-64076) ObjCExport: Do not retain descriptors in stubs
- [`KT-64168`](https://youtrack.jetbrains.com/issue/KT-64168) ObjCExport: Split header generator module into K1 and Analysis Api
- [`KT-64227`](https://youtrack.jetbrains.com/issue/KT-64227) ObjCExport: Extract Header Generation to base module

### Native. Runtime. Memory

- [`KT-62689`](https://youtrack.jetbrains.com/issue/KT-62689) Native: generate signposts for GC performance debugging

### Native. Testing

- [`KT-64256`](https://youtrack.jetbrains.com/issue/KT-64256) IR_DUMP directive doesn't enforce FIR_IDENTICAL when it is possible
- [`KT-62157`](https://youtrack.jetbrains.com/issue/KT-62157) Native: Migrate FileCheck tests to new native test infra

### Tools. CLI

- [`KT-64013`](https://youtrack.jetbrains.com/issue/KT-64013) CLI REPL: "com.sun.jna.LastErrorException: [14] Bad address" on invoking kotlinc from CLI on ARM Mac

### Tools. Compiler Plugin API

- [`KT-64444`](https://youtrack.jetbrains.com/issue/KT-64444) K2: IrGeneratedDeclarationsRegistrar.addMetadataVisibleAnnotationsToElement doesn't work for declarations in common module

### Tools. Compiler Plugins

- [`KT-33020`](https://youtrack.jetbrains.com/issue/KT-33020) Support stripping debug information in the jvm-abi-gen plugin
- [`KT-64707`](https://youtrack.jetbrains.com/issue/KT-64707) K2: Parcelize ignores `@TypeParceler` set for typealias

### Tools. Compiler plugins. Serialization

- [`KT-64447`](https://youtrack.jetbrains.com/issue/KT-64447) K2: Implement Serialization...IrBoxTestGenerated for K2
- [`KT-64124`](https://youtrack.jetbrains.com/issue/KT-64124) Different klib signatures in K1/K2 for a serializable class

### Tools. Gradle

- [`KT-64653`](https://youtrack.jetbrains.com/issue/KT-64653) Add Kotlin DslMarker into Gradle plugin DSL
- [`KT-64251`](https://youtrack.jetbrains.com/issue/KT-64251) KGP: Cannot re-use tooling model cache with Project Isolation due to "~/.gradle/kotlin-profile" changing
- [`KT-58768`](https://youtrack.jetbrains.com/issue/KT-58768) Support configuration cache and project isolation for FUS statistics
- [`KT-64379`](https://youtrack.jetbrains.com/issue/KT-64379) Remove `kotlin.useK2` gradle property
- [`KT-62527`](https://youtrack.jetbrains.com/issue/KT-62527) Gradle: get rid of the `Project.buildDir` usages
- [`KT-55322`](https://youtrack.jetbrains.com/issue/KT-55322) Kotlin daemon: Cannot perform operation, requested state: Alive > actual: LastSession

### Tools. Gradle. Cocoapods

- [`KT-57650`](https://youtrack.jetbrains.com/issue/KT-57650) Gradle Cocoapods: use pod install --repo-update instead of pod install

### Tools. Gradle. JS

- [`KT-64561`](https://youtrack.jetbrains.com/issue/KT-64561) K/JS tests are not executed after upgrade to 1.9.22
- [`KT-63435`](https://youtrack.jetbrains.com/issue/KT-63435) KJS: Get rid of deprecated outputFileProperty of Kotlin2JsCompile

### Tools. Gradle. Multiplatform

- [`KT-56440`](https://youtrack.jetbrains.com/issue/KT-56440) TCS: Gradle Sync: Add API to populate extras only during sync
- [`KT-63226`](https://youtrack.jetbrains.com/issue/KT-63226) KGP Multiplatform Ide Dependency Resolution: Use gradle variants instead/in addition of ArtifactResolutionQuery

### Tools. Gradle. Native

- [`KT-62745`](https://youtrack.jetbrains.com/issue/KT-62745) iOS application build is failing if script sandboxing option is enabled in Xcode
- [`KT-62800`](https://youtrack.jetbrains.com/issue/KT-62800) CInteropProcess should not require .def file to exist
- [`KT-62795`](https://youtrack.jetbrains.com/issue/KT-62795) CInteropProcess task resolves cinterop def file eagerly, breaking Gradle task dependencies

### Tools. Incremental Compile

- [`KT-63837`](https://youtrack.jetbrains.com/issue/KT-63837) Implement baseline fix for common sources getting access to platform declarations
- [`KT-64513`](https://youtrack.jetbrains.com/issue/KT-64513) Simplify adding configuration properties to incremental compilation
- [`KT-21534`](https://youtrack.jetbrains.com/issue/KT-21534) IC doesn't recompile file with potential SAM-adapter usage
- [`KT-63839`](https://youtrack.jetbrains.com/issue/KT-63839) Measure impact of rebuilding common sources, using nightly IC benchmarks
- [`KT-64228`](https://youtrack.jetbrains.com/issue/KT-64228) K2: After switching to LV20 branch incremental tests are not running on PSI anymore
- [`KT-46743`](https://youtrack.jetbrains.com/issue/KT-46743) Incremental compilation doesn't process usages of Java property in Kotlin code if getter is removed
- [`KT-60522`](https://youtrack.jetbrains.com/issue/KT-60522) Incremental compilation doesn't process usages of Java property in Kotlin code if return type of getter changes

### Tools. JPS

- [`KT-55393`](https://youtrack.jetbrains.com/issue/KT-55393) JPS: Java synthetic properties incremental compilation is broken

### Tools. Kapt

- [`KT-64719`](https://youtrack.jetbrains.com/issue/KT-64719) K2 KAPT Stub genertaion doesn't fail on files with syntax errors
- [`KT-64680`](https://youtrack.jetbrains.com/issue/KT-64680) Kapt: remove the flag to enable old JVM backend
- [`KT-64639`](https://youtrack.jetbrains.com/issue/KT-64639) KAPT+JVM_IR: erased error types in JvmStatic and JvmOverloads
- [`KT-64389`](https://youtrack.jetbrains.com/issue/KT-64389) K2 KAPT generates invalid code for multiple generic constraints
- [`KT-61776`](https://youtrack.jetbrains.com/issue/KT-61776) K2: KAPT tasks fail with parallel gradle
- [`KT-64021`](https://youtrack.jetbrains.com/issue/KT-64021) Kapt3 + Kapt4. NullPointerException: processingEnv must not be null
- [`KT-64303`](https://youtrack.jetbrains.com/issue/KT-64303) K2 KAPT: Kapt doesn't dispose resources allocated by standalone analysis API
- [`KT-64301`](https://youtrack.jetbrains.com/issue/KT-64301) K2 KAPT: Kapt doesn't report invalid enum value names to log
- [`KT-64297`](https://youtrack.jetbrains.com/issue/KT-64297) K2 KAPT: Deprecated members are not marked with `@java`.lang.Deprecated

### Tools. REPL

- [`KT-18355`](https://youtrack.jetbrains.com/issue/KT-18355) REPL doesn't quit on the first line after pressing Ctrl+D or typing :quit


## 2.0.0-Beta2

### Analysis. API

#### Fixes

- [`KT-62982`](https://youtrack.jetbrains.com/issue/KT-62982) K2: Cannot get a PSI element for 'Enum.values'
- [`KT-59732`](https://youtrack.jetbrains.com/issue/KT-59732) FirLazyResolveContractViolationException: `lazyResolveToPhase(IMPORTS)` cannot be called from a transformer with a phase IMPORTS from serialisation plugin
- [`KT-61757`](https://youtrack.jetbrains.com/issue/KT-61757) K2 IDE: resolution to buitlins does not work for from common module
- [`KT-62676`](https://youtrack.jetbrains.com/issue/KT-62676) K2 IDE: Reference shortener does not recoginize redundant this references
- [`KT-63627`](https://youtrack.jetbrains.com/issue/KT-63627) K2 IDE: shorten reference shortens required qualifier
- [`KT-62675`](https://youtrack.jetbrains.com/issue/KT-62675) K2 IDE: Reference shortener does not recoginize redundant labels
- [`KT-63771`](https://youtrack.jetbrains.com/issue/KT-63771) fe10: KtNamedClassOrObjectSymbol#isInline does not cover value classes
- [`KT-62947`](https://youtrack.jetbrains.com/issue/KT-62947) Analysis API: Error while resolving FirPropertyImpl
- [`KT-60327`](https://youtrack.jetbrains.com/issue/KT-60327) K2 IDE. "IllegalArgumentException: source must not be null" during delegation declaration
- [`KT-63700`](https://youtrack.jetbrains.com/issue/KT-63700) K2: "FirLazyExpression should be calculated before accessing" in the case of secondary constructor
- [`KT-62111`](https://youtrack.jetbrains.com/issue/KT-62111) K2 IDE. IllegalArgumentException on for loop with iterator declaration attempt
- [`KT-63538`](https://youtrack.jetbrains.com/issue/KT-63538) Analysis API: Removing a contract statement via `PsiElement.delete()` does not trigger an out-of-block modification
- [`KT-63694`](https://youtrack.jetbrains.com/issue/KT-63694) K1/K2 IDE. "RuntimeException: Broken stub format, most likely version of kotlin.FILE (kotlin.FILE) was not updated after serialization changes" exception on incorrect class name
- [`KT-63560`](https://youtrack.jetbrains.com/issue/KT-63560) Analysis API: Modifiable PSI tests cannot rely on the cached application environment to allow write access
- [`KT-62980`](https://youtrack.jetbrains.com/issue/KT-62980) Implement `KtFirSimpleNameReference#getImportAlias`
- [`KT-63130`](https://youtrack.jetbrains.com/issue/KT-63130) Analysis API: No receiver found for broken code during commit document
- [`KT-60170`](https://youtrack.jetbrains.com/issue/KT-60170) K2 IDE: CCE from KtFirCallResolver on invalid code with wrong implicit invoke
- [`KT-61783`](https://youtrack.jetbrains.com/issue/KT-61783) K2: Analyze 'KtCodeFragment' in a separate session
- [`KT-62010`](https://youtrack.jetbrains.com/issue/KT-62010) K2: IDE K2: "ConeClassLikeTypeImpl is not resolved to symbol for on-error type"
- [`KT-62957`](https://youtrack.jetbrains.com/issue/KT-62957) Analysis API: NullPointerException on call resolution when builtins are not available
- [`KT-62899`](https://youtrack.jetbrains.com/issue/KT-62899) K2 IDE. IDE ignores `@Suppress` annotation for errors
- [`KT-62935`](https://youtrack.jetbrains.com/issue/KT-62935) Analysis API: `kotlin.Cloneable` should not be available in Kotlin/Native sources
- [`KT-62910`](https://youtrack.jetbrains.com/issue/KT-62910) Analysis API: create AbstractFirPsiNativeDiagnosticsTest for LL FIR
- [`KT-63096`](https://youtrack.jetbrains.com/issue/KT-63096) K2: Analysis API: KotlinAnnotationsResolver for IDE is created with incorrect scope
- [`KT-63282`](https://youtrack.jetbrains.com/issue/KT-63282) K2 Script: annotation arguments phase should resolve propagated annotations
- [`KT-63223`](https://youtrack.jetbrains.com/issue/KT-63223) Analysis API: reference to declarations with kotlin* package are not resolved
- [`KT-63195`](https://youtrack.jetbrains.com/issue/KT-63195) AA: incorrect results from `KtTypeProvider#getReceiverTypeForDoubleColonExpression`

### Analysis. Light Classes

- [`KT-63552`](https://youtrack.jetbrains.com/issue/KT-63552) Symbol Light Classes don't support arrayOf and similar without parameters in property initializers and default parameter values
- [`KT-63486`](https://youtrack.jetbrains.com/issue/KT-63486) SLC: a lot of RAM is allocated in `org.jetbrains.kotlin.asJava.LightClassUtil.isMangled`

### Backend. Wasm

- [`KT-62863`](https://youtrack.jetbrains.com/issue/KT-62863) Execution failed for task ':kotlinx-serialization-properties:wasmJsD8Test' in serialization in the K2 QG

### Compiler

#### New Features

- [`KT-22004`](https://youtrack.jetbrains.com/issue/KT-22004) Allow to resolve CONFLICTING_OVERLOADS with Deprecated(HIDDEN)
- [`KT-61955`](https://youtrack.jetbrains.com/issue/KT-61955) Support more wider actual member visibility, if the expect member is effectively final

#### Fixes

- [`KT-63695`](https://youtrack.jetbrains.com/issue/KT-63695) JVM: Don't use plugin extensions when compiling code fragment
- [`KT-59903`](https://youtrack.jetbrains.com/issue/KT-59903) K2: Disappeared DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE
- [`KT-62961`](https://youtrack.jetbrains.com/issue/KT-62961) K2 / KMP: NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS with expect enum class and typealias
- [`KT-59369`](https://youtrack.jetbrains.com/issue/KT-59369) K2: Missing BUILDER_INFERENCE_STUB_RECEIVER
- [`KT-53749`](https://youtrack.jetbrains.com/issue/KT-53749) Support builder inference restriction in FIR
- [`KT-59390`](https://youtrack.jetbrains.com/issue/KT-59390) K2: Missing BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION
- [`KT-59887`](https://youtrack.jetbrains.com/issue/KT-59887) K2: Disappeared ACTUAL_MISSING
- [`KT-62885`](https://youtrack.jetbrains.com/issue/KT-62885) Introduce a language feature entry for expect actual classes for easier configuration of MPP projects
- [`KT-62589`](https://youtrack.jetbrains.com/issue/KT-62589) K2: Investigate need of non-nullable IdSignature in Fir2IrLazyDeclarations
- [`KT-63329`](https://youtrack.jetbrains.com/issue/KT-63329) K2: difference in SAM-conversion casts generation
- [`KT-64062`](https://youtrack.jetbrains.com/issue/KT-64062) K2 IDE. NPE on typing nullable parameter in return
- [`KT-63761`](https://youtrack.jetbrains.com/issue/KT-63761) K2: False positive "Unresolved reference" caused by object's parameter in enum class which is passed as annotation parameter
- [`KT-55252`](https://youtrack.jetbrains.com/issue/KT-55252) Backend Internal error during psi2ir in native compile tasks (NPE in getKlibModuleOrigin)
- [`KT-50453`](https://youtrack.jetbrains.com/issue/KT-50453) Improve builder inference diagnostics with type mismatch due to chosen inapplicable overload
- [`KT-56949`](https://youtrack.jetbrains.com/issue/KT-56949) K2: Builder inference violates upper bound
- [`KT-64028`](https://youtrack.jetbrains.com/issue/KT-64028) K2: Investigate questionable condition in FirBuilderInfernceSession
- [`KT-60031`](https://youtrack.jetbrains.com/issue/KT-60031) K2: Introduced NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS
- [`KT-63646`](https://youtrack.jetbrains.com/issue/KT-63646) K2: "IllegalStateException: Return type of provideDelegate is expected to be one of the type variables of a candidate, but D was found"
- [`KT-59881`](https://youtrack.jetbrains.com/issue/KT-59881) K2: Disappeared UNSUPPORTED
- [`KT-64136`](https://youtrack.jetbrains.com/issue/KT-64136) K2: NSME with Anvil compiler plugin
- [`KT-63448`](https://youtrack.jetbrains.com/issue/KT-63448) K2: CONFLICTING_INHERITED_JVM_DECLARATIONS with `@JvmField`
- [`KT-63865`](https://youtrack.jetbrains.com/issue/KT-63865) K2: "IllegalArgumentException: Failed requirement." caused by lambda parameters with different type in init block
- [`KT-62609`](https://youtrack.jetbrains.com/issue/KT-62609) K2. Type argument inference changed for object of Java class with several common parents
- [`KT-63081`](https://youtrack.jetbrains.com/issue/KT-63081) Optimize new native caches: CachedLibraries.computeVersionedCacheDirectory()
- [`KT-63580`](https://youtrack.jetbrains.com/issue/KT-63580) "AssertionError: access of const val: GET_FIELD" caused by const value and variable with delegation
- [`KT-63567`](https://youtrack.jetbrains.com/issue/KT-63567) "NoSuchMethodError" on getting value of lazily initialized property by companion's const value
- [`KT-63540`](https://youtrack.jetbrains.com/issue/KT-63540) Restrict the CONFLICTING_OVERLOADS + DeprecatedLevel.HIDDEN ignore to final callables
- [`KT-30369`](https://youtrack.jetbrains.com/issue/KT-30369) Smartcasts from safe call + null check don't work if explicit true/false check is used
- [`KT-30376`](https://youtrack.jetbrains.com/issue/KT-30376) Smartcasts don't propagate to the original variable when use not-null assertion or cast expression
- [`KT-30868`](https://youtrack.jetbrains.com/issue/KT-30868) Unsound smartcast if smartcast source and break is placed inside square brackets (indexing expression)
- [`KT-31053`](https://youtrack.jetbrains.com/issue/KT-31053) Nothing? type check isn't equivalent to null check is some places
- [`KT-29935`](https://youtrack.jetbrains.com/issue/KT-29935) Smartcasts don't work if explicit annotated true/false check is used
- [`KT-30903`](https://youtrack.jetbrains.com/issue/KT-30903) Smartcast to null doesn't affect computing of exhaustiveness
- [`KT-62847`](https://youtrack.jetbrains.com/issue/KT-62847) K2: Introduce FIR node for SAM conversion
- [`KT-63564`](https://youtrack.jetbrains.com/issue/KT-63564) K/Wasm: CompilationException with 2.0.0-Beta1
- [`KT-63345`](https://youtrack.jetbrains.com/issue/KT-63345) K2: FIR2IR chooses an incorrect type for smartcast in case of SAM conversion
- [`KT-63848`](https://youtrack.jetbrains.com/issue/KT-63848) ReflectiveAccessLowering does not count arguments of super-calls
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
- [`KT-63377`](https://youtrack.jetbrains.com/issue/KT-63377) K2: conflict between type parameter and nested class
- [`KT-62913`](https://youtrack.jetbrains.com/issue/KT-62913) Convert DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE to checking incompatibility
- [`KT-63550`](https://youtrack.jetbrains.com/issue/KT-63550) K2: fake-override in expect covariant override in actual. Move diagnostics from backend to frontend
- [`KT-63443`](https://youtrack.jetbrains.com/issue/KT-63443) IrFakeOverrideBuilder: ISE "No new fake override recorded" when Java superclass declares abstract toString
- [`KT-58933`](https://youtrack.jetbrains.com/issue/KT-58933) Applying suggested signature from WRONG_NULLABILITY_FOR_JAVA_OVERRIDE leads to red code
- [`KT-63600`](https://youtrack.jetbrains.com/issue/KT-63600) K2: Duplicate WRONG_NULLABILITY_FOR_JAVA_OVERRIDE
- [`KT-63508`](https://youtrack.jetbrains.com/issue/KT-63508) K2: "IllegalArgumentException: Not FirResolvedTypeRef (String) in storeResult" caused by `@Deprecated` Java function and typo
- [`KT-63656`](https://youtrack.jetbrains.com/issue/KT-63656) K2: "IllegalArgumentException: Local com/example/<anonymous> should never be used to find its corresponding classifier"
- [`KT-63459`](https://youtrack.jetbrains.com/issue/KT-63459) K2: OPT_IN_USAGE_ERROR is absent when calling the enum primary constructor
- [`KT-59582`](https://youtrack.jetbrains.com/issue/KT-59582) OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN on an annotation import
- [`KT-63732`](https://youtrack.jetbrains.com/issue/KT-63732) K1: False positive OUTER_CLASS_ARGUMENTS_REQUIRED inside anonymous object
- [`KT-60614`](https://youtrack.jetbrains.com/issue/KT-60614) K2: Conflicting INVISIBLE_REFERENCE and UNRESOLVED_REFERENCE reported depending on FIR test for transitive friend module dependencies
- [`KT-59983`](https://youtrack.jetbrains.com/issue/KT-59983) K2: Disappeared IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS
- [`KT-57044`](https://youtrack.jetbrains.com/issue/KT-57044) K2 LL Tests: false-positive 'Overload resolution ambiguity between candidates: [`@Override`() fun test(): Unit , fun test(): Unit]'
- [`KT-58028`](https://youtrack.jetbrains.com/issue/KT-58028) K2: False-positive TYPE_PARAMETER_IS_NOT_AN_EXPRESSION
- [`KT-62560`](https://youtrack.jetbrains.com/issue/KT-62560) K2: KAPT4: annotation `@ReplaceWith` is missing a default value for the element 'imports'
- [`KT-63068`](https://youtrack.jetbrains.com/issue/KT-63068) K2 supports typeRef-name labels
- [`KT-63642`](https://youtrack.jetbrains.com/issue/KT-63642) JVM_IR: don't generate reflective access to getter/setter without property
- [`KT-62212`](https://youtrack.jetbrains.com/issue/KT-62212) K2: require matching of suspend status for override check
- [`KT-63597`](https://youtrack.jetbrains.com/issue/KT-63597) JVM_IR: Properly handle type parameters of outer declaration in code fragment
- [`KT-61282`](https://youtrack.jetbrains.com/issue/KT-61282) K2: Incorrect overridden function for `java.nio.CharBuffer.get`
- [`KT-63317`](https://youtrack.jetbrains.com/issue/KT-63317) K2: Disallow generic types in contract type assertions
- [`KT-59922`](https://youtrack.jetbrains.com/issue/KT-59922) K2: Disappeared CANNOT_CHECK_FOR_ERASED
- [`KT-63241`](https://youtrack.jetbrains.com/issue/KT-63241) IJ monorepo K2 QG: backward-incompatible compiler ABI change leads to run-time failures of Fleet's kotlinc plugins
- [`KT-59988`](https://youtrack.jetbrains.com/issue/KT-59988) K2: Disappeared TYPE_ARGUMENTS_NOT_ALLOWED
- [`KT-59936`](https://youtrack.jetbrains.com/issue/KT-59936) K2: Disappeared ARGUMENT_PASSED_TWICE
- [`KT-63522`](https://youtrack.jetbrains.com/issue/KT-63522) K2: wrong context for delegated field type
- [`KT-63454`](https://youtrack.jetbrains.com/issue/KT-63454) Properly check that inline fun is in the same module as callee in `IrSourceCompilerForInline`
- [`KT-59951`](https://youtrack.jetbrains.com/issue/KT-59951) K2: Disappeared NO_TYPE_ARGUMENTS_ON_RHS
- [`KT-63535`](https://youtrack.jetbrains.com/issue/KT-63535) K2: Apply DFA implications for nullable Nothing to both sides
- [`KT-62727`](https://youtrack.jetbrains.com/issue/KT-62727) K2: Missing JSCODE_UNSUPPORTED_FUNCTION_KIND
- [`KT-62726`](https://youtrack.jetbrains.com/issue/KT-62726) K2: Missing JSCODE_WRONG_CONTEXT
- [`KT-62725`](https://youtrack.jetbrains.com/issue/KT-62725) K2: Missing JSCODE_INVALID_PARAMETER_NAME
- [`KT-62314`](https://youtrack.jetbrains.com/issue/KT-62314) Make usages of JavaTypeParameterStack safe
- [`KT-60924`](https://youtrack.jetbrains.com/issue/KT-60924) FIR2IR: Get rid of all unsafe usages of IrSymbol.owner
- [`KT-57949`](https://youtrack.jetbrains.com/issue/KT-57949) FIR: SignatureEnhancement: mutation of java enum entry
- [`KT-59908`](https://youtrack.jetbrains.com/issue/KT-59908) K2: Disappeared RECURSIVE_TYPEALIAS_EXPANSION
- [`KT-62724`](https://youtrack.jetbrains.com/issue/KT-62724) K2: Missing WRONG_JS_FUN_TARGET
- [`KT-62856`](https://youtrack.jetbrains.com/issue/KT-62856) K2: Don't create IR declaration when its symbol is accessed in fir2ir
- [`KT-61329`](https://youtrack.jetbrains.com/issue/KT-61329) K2: Review for diagnostic messages reported by CLI arguments processing
- [`KT-60604`](https://youtrack.jetbrains.com/issue/KT-60604) K2: introduced NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, `@PublishedApi` needed for constants in annotations
- [`KT-63286`](https://youtrack.jetbrains.com/issue/KT-63286) K2: Top-level properties in scripts are missing initialization checks
- [`KT-62711`](https://youtrack.jetbrains.com/issue/KT-62711) Incorrect ParsedCodeMetaInfo instances
- [`KT-63122`](https://youtrack.jetbrains.com/issue/KT-63122) K2: Improve 'EVALUATION_ERROR' messages
- [`KT-63164`](https://youtrack.jetbrains.com/issue/KT-63164) K2/JVM: compiler codegen crash on invisible property IllegalStateException: Fake override should have at least one overridden descriptor
- [`KT-62352`](https://youtrack.jetbrains.com/issue/KT-62352) jspecify NonNull annotation seems not supported
- [`KT-56614`](https://youtrack.jetbrains.com/issue/KT-56614) K2: Incorrect overload resolution with SAM types
- [`KT-62783`](https://youtrack.jetbrains.com/issue/KT-62783) K2: False positive CAST_NEVER_SUCCEEDS when casting nullable expression to it's non-nullable generic base class
- [`KT-47931`](https://youtrack.jetbrains.com/issue/KT-47931) FIR DFA: smartcast not working for `if (x!=null || x!=null && x!=null) {}`
- [`KT-62735`](https://youtrack.jetbrains.com/issue/KT-62735) K2: Disappeared EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER
- [`KT-62733`](https://youtrack.jetbrains.com/issue/KT-62733) K2: Disappeared WRONG_EXTERNAL_DECLARATION
- [`KT-62734`](https://youtrack.jetbrains.com/issue/KT-62734) K2: Disappeared INLINE_EXTERNAL_DECLARATION
- [`KT-62618`](https://youtrack.jetbrains.com/issue/KT-62618) K2: Fix the `ensureAllMessagesPresent` test
- [`KT-60312`](https://youtrack.jetbrains.com/issue/KT-60312) K2: CCE “class [I cannot be cast to class java.lang.Number ([I and java.lang.Number are in module java.base of loader 'bootstrap')” on using IntArray as vararg
- [`KT-61362`](https://youtrack.jetbrains.com/issue/KT-61362) K2: Properties/fields are missing from system libraries
- [`KT-52432`](https://youtrack.jetbrains.com/issue/KT-52432) Using the IDE compiled with K2 (useFir) throws VerifyError exception
- [`KT-59825`](https://youtrack.jetbrains.com/issue/KT-59825) K2: Fix the TODO about `wasExperimentalMarkerClasses` in `FirSinceKotlinHelpers`
- [`KT-26045`](https://youtrack.jetbrains.com/issue/KT-26045) False positive DUPLICATE_LABEL_IN_WHEN for safe calls
- [`KT-63094`](https://youtrack.jetbrains.com/issue/KT-63094) K2: Exception from fir2ir during conversion data class with property of dynamic type
- [`KT-59822`](https://youtrack.jetbrains.com/issue/KT-59822) K2: Fix the TODO in FirConstChecks
- [`KT-59493`](https://youtrack.jetbrains.com/issue/KT-59493) Definitely non-nullable types have type inference issues with extension functions
- [`KT-63396`](https://youtrack.jetbrains.com/issue/KT-63396) K2: property from companion object are unresolved as an annotation argument in type parameter
- [`KT-62925`](https://youtrack.jetbrains.com/issue/KT-62925) K2: Disappeared EXPOSED_FUNCTION_RETURN_TYPE for package-private and type args
- [`KT-63430`](https://youtrack.jetbrains.com/issue/KT-63430) IrFakeOverrideBuilder: VerifyError on calling a function with a context receiver from a superclass
- [`KT-62420`](https://youtrack.jetbrains.com/issue/KT-62420) K2: Remove ConeClassifierLookupTag from ConeTypeVariableTypeConstructor
- [`KT-59998`](https://youtrack.jetbrains.com/issue/KT-59998) K2: Disappeared OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN
- [`KT-53308`](https://youtrack.jetbrains.com/issue/KT-53308) TYPE_MISMATCH: Contracts on boolean expression has no effect on referential equality to `null`
- [`KT-51160`](https://youtrack.jetbrains.com/issue/KT-51160) Type mismatch with contracts on narrowing sealed hierarchy fail to smart cast
- [`KT-49696`](https://youtrack.jetbrains.com/issue/KT-49696) Smart cast to non-null with inline non-modifying closures sometimes doesn't work
- [`KT-46586`](https://youtrack.jetbrains.com/issue/KT-46586) SMARTCAST_IMPOSSIBLE when assigning value inside lambda instead of if expression
- [`KT-41728`](https://youtrack.jetbrains.com/issue/KT-41728) False positive no smart cast with unreachable code after return in if expression
- [`KT-57529`](https://youtrack.jetbrains.com/issue/KT-57529) K1/K2: "IllegalStateException: not identifier: <no name provided>" with hard keywords in angle brackets
- [`KT-59401`](https://youtrack.jetbrains.com/issue/KT-59401) K2: Missing ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE
- [`KT-63147`](https://youtrack.jetbrains.com/issue/KT-63147) K2: False negative DSL_SCOPE_VIOLATION when member is annotated with `@LowPriorityInOverloadResolution`
- [`KT-63172`](https://youtrack.jetbrains.com/issue/KT-63172) K2: Java vararg setter should not be used as property accessor
- [`KT-62306`](https://youtrack.jetbrains.com/issue/KT-62306) K2: Compiler internal error for incorrect call on ILT
- [`KT-61592`](https://youtrack.jetbrains.com/issue/KT-61592) kt57320.kt weird diagnostic range for NO_ACTUAL_FOR_EXPECT
- [`KT-60294`](https://youtrack.jetbrains.com/issue/KT-60294) K2: lambda inside object capturing this, when not in K1
- [`KT-62590`](https://youtrack.jetbrains.com/issue/KT-62590) Split expect/actual matcher-checker machinery in two separate components: matcher and checker
- [`KT-62120`](https://youtrack.jetbrains.com/issue/KT-62120) K2: "NoSuchMethodError: java.lang.String" at runtime on class delegating to Java type
- [`KT-36976`](https://youtrack.jetbrains.com/issue/KT-36976) FIR: Provide exact smart casting type
- [`KT-62628`](https://youtrack.jetbrains.com/issue/KT-62628) K2: FirErrorTypeRefImpl doesn't have annotations
- [`KT-62447`](https://youtrack.jetbrains.com/issue/KT-62447) K2. "Replacing annotations in FirErrorTypeRefImpl is not supported" compiler error when annotation is used as variable type or return type
- [`KT-62541`](https://youtrack.jetbrains.com/issue/KT-62541) K2: Missed type mismatch error
- [`KT-37591`](https://youtrack.jetbrains.com/issue/KT-37591) Deprecate cases in FE 1.0 when companion property is prioritized against enum entry

### Docs & Examples

- [`KT-58295`](https://youtrack.jetbrains.com/issue/KT-58295) Create a separate page for https://kotl.in/wasm_help
- [`KT-6259`](https://youtrack.jetbrains.com/issue/KT-6259) Docs: add information about default constructor for class

### IDE

- [`KT-55788`](https://youtrack.jetbrains.com/issue/KT-55788) [SLC] Declarations with value classes are leaked into light classes

### IDE. Gradle. Script

- [`KT-60813`](https://youtrack.jetbrains.com/issue/KT-60813) Scripts: NoSuchMethodError: 'void org.slf4j.Logger.error(java.lang.String, java.lang.Object)' when dependency uses Slf4j API

### IDE. JS

- [`KT-61257`](https://youtrack.jetbrains.com/issue/KT-61257) Analysis API:"KotlinIllegalArgumentExceptionWithAttachments: Invalid FirDeclarationOrigin DynamicScope" exception on unsupported JS dynamic usage in scope

### IDE. Multiplatform

- [`KT-45513`](https://youtrack.jetbrains.com/issue/KT-45513) Run c-interop generation in parallel during project import
- [`KT-63126`](https://youtrack.jetbrains.com/issue/KT-63126) K2: Analysis API Standalone: IllegalStateException from Kotlin/Native klib

### IDE. Script

- [`KT-61267`](https://youtrack.jetbrains.com/issue/KT-61267) K2 Scripts: dependency issues

### IR. Actualizer

- [`KT-62292`](https://youtrack.jetbrains.com/issue/KT-62292) K2: Extract IrActualizer into separate module
- [`KT-63442`](https://youtrack.jetbrains.com/issue/KT-63442) IrFakeOverrideBuilder: ISE "Multiple overrides" error when function signatures differ only in the type parameter upper bound

### IR. Interpreter

- [`KT-62683`](https://youtrack.jetbrains.com/issue/KT-62683) K2: FIR2IR: IrConst*Transformer doesn't evaluate an expression for const val initializer

### IR. Tree

- [`KT-61970`](https://youtrack.jetbrains.com/issue/KT-61970) Refactor IR and FIR tree generators to reuse common logic
- [`KT-61703`](https://youtrack.jetbrains.com/issue/KT-61703) Drop the dependency on kotlinpoet for IR tree generation
- [`KT-63437`](https://youtrack.jetbrains.com/issue/KT-63437) IrFakeOverrideBuilder: ISE "Captured Type does not have a classifier" on complex Java hierarchy

### JavaScript

#### Fixes

- [`KT-61117`](https://youtrack.jetbrains.com/issue/KT-61117) Migrate remaining legacy IC tests to IR
- [`KT-63808`](https://youtrack.jetbrains.com/issue/KT-63808) compileTestDevelopmentExecutableKotlinJs failed in JsIntrinsicTransformers
- [`KT-61523`](https://youtrack.jetbrains.com/issue/KT-61523) KJS: Call main function in per-file mode
- [`KT-63543`](https://youtrack.jetbrains.com/issue/KT-63543) KJS / K2: Exported declaration uses non-exportable return type type: 'kotlin.Unit'
- [`KT-63089`](https://youtrack.jetbrains.com/issue/KT-63089) KJS / K2 : "IllegalArgumentException: source must not be null " for inner class and interface as type
- [`KT-62077`](https://youtrack.jetbrains.com/issue/KT-62077) KJS: TypeError: str.charCodeAt is not a function
- [`KT-63436`](https://youtrack.jetbrains.com/issue/KT-63436) K/JS: Eliminate names for synthetic classes in setMetadataFor()
- [`KT-61929`](https://youtrack.jetbrains.com/issue/KT-61929) KJS: "IllegalStateException: No dispatch receiver parameter for FUN LOCAL_FUNCTION_FOR_LAMBDA" caused by `run` function in init block
- [`KT-63013`](https://youtrack.jetbrains.com/issue/KT-63013) KJS: `requireNotNull` not working correctly in JS tests with Kotlin 1.9.20
- [`KT-63207`](https://youtrack.jetbrains.com/issue/KT-63207) KMP / JS: "TypeError: <mangled_name> is not a function" with 1.9.20
- [`KT-16981`](https://youtrack.jetbrains.com/issue/KT-16981) js: Command line arguments passed to `main()` are always empty
- [`KT-61525`](https://youtrack.jetbrains.com/issue/KT-61525) KJS: Test functions are not invoked in per-file mode

### Klibs

- [`KT-62259`](https://youtrack.jetbrains.com/issue/KT-62259) KLIB ABI reader: add information about a backing field to AbiProperty
- [`KT-62515`](https://youtrack.jetbrains.com/issue/KT-62515) Interop klib of concurrent version is not accepted when building dependent project: "The library versions don't match"

### Language Design

- [`KT-62138`](https://youtrack.jetbrains.com/issue/KT-62138) K1: false positive (?) NO_SET_METHOD for += resolved as a combination of Map.get and plus
- [`KT-61573`](https://youtrack.jetbrains.com/issue/KT-61573) Emit the compilation warning on expect/actual classes. The warning must mention that expect/actual classes are in Beta

### Libraries

- [`KT-62346`](https://youtrack.jetbrains.com/issue/KT-62346) Sublists of ListBuilder does not correctly detect ConcurrentModification
- [`KT-59441`](https://youtrack.jetbrains.com/issue/KT-59441) Design reading and writing future versions of Kotlin metadata
- [`KT-57922`](https://youtrack.jetbrains.com/issue/KT-57922) kotlinx-metadata-jvm does not take into account strict semantics flag
- [`KT-63341`](https://youtrack.jetbrains.com/issue/KT-63341) K2: JVM StringBuilder has no corresponding members for expected class members
- [`KT-51058`](https://youtrack.jetbrains.com/issue/KT-51058) Avoid byte array allocation in File.writeText when possible
- [`KT-63447`](https://youtrack.jetbrains.com/issue/KT-63447) K2: stdlib buildscript error: file included in two modules

### Native

- [`KT-63789`](https://youtrack.jetbrains.com/issue/KT-63789) Native: Incremental compilation problem with compose
- [`KT-50547`](https://youtrack.jetbrains.com/issue/KT-50547) [Commonizer] K/N echoServer sample fails with multiple "Unresolved reference" errors on Windows

### Native. Build Infrastructure

- [`KT-63905`](https://youtrack.jetbrains.com/issue/KT-63905) Extract ObjC Export Header generation from K/N backend
- [`KT-63220`](https://youtrack.jetbrains.com/issue/KT-63220) [K/N] Unable to specify custom LLVM distribution

### Native. C and ObjC Import

- [`KT-63287`](https://youtrack.jetbrains.com/issue/KT-63287) [K/N] Create test model for building/executing C-Interop tests
- [`KT-63048`](https://youtrack.jetbrains.com/issue/KT-63048) K2 ObjC interop: Fields are not supported for Companion of subclass of ObjC type

### Native. ObjC Export

- [`KT-63153`](https://youtrack.jetbrains.com/issue/KT-63153) Native: implement a flag to emit compiler warnings on ObjCExport name collisions

### Reflection

- [`KT-60708`](https://youtrack.jetbrains.com/issue/KT-60708) Reflection: Not supported `)` (parentheses in backticks)

### Tools. Compiler Plugins

- [`KT-53861`](https://youtrack.jetbrains.com/issue/KT-53861) K2. Report SERIALIZER_TYPE_INCOMPATIBLE on specific type argument in kotlinx.serialization
- [`KT-63086`](https://youtrack.jetbrains.com/issue/KT-63086) K2: "Parcelable should be a class"
- [`KT-61432`](https://youtrack.jetbrains.com/issue/KT-61432) K2 Parcelize. RawValue is not recognized if parameter is annotated via typealias

### Tools. Compiler plugins. Serialization

- [`KT-63591`](https://youtrack.jetbrains.com/issue/KT-63591) K2: "KotlinReflectionInternalError: Could not compute caller for function" on generated internal constructor
- [`KT-63570`](https://youtrack.jetbrains.com/issue/KT-63570) K2 / Serialization: "Class * which is serializer for type * is applied here to type *. This may lead to errors or incorrect behavior."
- [`KT-63402`](https://youtrack.jetbrains.com/issue/KT-63402) K2 / Serialization: "SyntheticAccessorLowering should not attempt to modify other files!" caused by sealed base with generic derived class in separate files

### Tools. Gradle

#### New Features

- [`KT-61975`](https://youtrack.jetbrains.com/issue/KT-61975) Re-purpose kotlin.experimental.tryK2

#### Performance Improvements

- [`KT-63005`](https://youtrack.jetbrains.com/issue/KT-63005) Avoid registering KMP related compatibility/disambiguration rules for pure JVM/Android projects

#### Fixes

- [`KT-60733`](https://youtrack.jetbrains.com/issue/KT-60733) Allow specify log level for compiler arguments used to compile sources
- [`KT-63697`](https://youtrack.jetbrains.com/issue/KT-63697) The warning is still presented in terminal after suppressing it with -Xexpect-actual-classes flag
- [`KT-63491`](https://youtrack.jetbrains.com/issue/KT-63491) Restore access to top-level DSL to configure compiler options in MPP
- [`KT-55322`](https://youtrack.jetbrains.com/issue/KT-55322) Kotlin daemon: Cannot perform operation, requested state: Alive > actual: LastSession
- [`KT-63369`](https://youtrack.jetbrains.com/issue/KT-63369) Fix: "The org.gradle.api.plugins.BasePluginConvention type has been deprecated."
- [`KT-63368`](https://youtrack.jetbrains.com/issue/KT-63368) Fix "The automatic loading of test framework implementation dependencies has been deprecated. "
- [`KT-63601`](https://youtrack.jetbrains.com/issue/KT-63601) Fetching Gradle compiler DSL objects using raw strings is inconvenient in the Groovy DSL
- [`KT-62758`](https://youtrack.jetbrains.com/issue/KT-62758) Gradle: make precise task outputs backup enabled by default
- [`KT-62955`](https://youtrack.jetbrains.com/issue/KT-62955) Missing static accessors for Wasm targets in Kotlin Gradle plugin DSL:
- [`KT-62962`](https://youtrack.jetbrains.com/issue/KT-62962) Remove COMPILE_INCREMENTAL_WITH_ARTIFACT_TRANSFORM system property
- [`KT-63499`](https://youtrack.jetbrains.com/issue/KT-63499) Gradle: Source sets conventions are still registered
- [`KT-52976`](https://youtrack.jetbrains.com/issue/KT-52976) Remove deprecated Gradle conventions usages
- [`KT-62939`](https://youtrack.jetbrains.com/issue/KT-62939) Bump minimal supported AGP version to 7.1
- [`KT-58223`](https://youtrack.jetbrains.com/issue/KT-58223) Kotlin Gradle plugin shouldn't store data in project cache directory
- [`KT-62131`](https://youtrack.jetbrains.com/issue/KT-62131) Could not isolate value org.jetbrains.kotlin.gradle.plugin.statistics.BuildFlowService$Parameters_Decorated`@63fddc4b` of type BuildFlowService.Parameters
- [`KT-62264`](https://youtrack.jetbrains.com/issue/KT-62264) Send build type report metric to FUS
- [`KT-62617`](https://youtrack.jetbrains.com/issue/KT-62617) Update report configuration project FUS metrics

### Tools. Gradle. Cocoapods

- [`KT-63331`](https://youtrack.jetbrains.com/issue/KT-63331) CocoaPods plugin noPodspec() causes "property * specifies file * which doesn't exist."

### Tools. Gradle. JS

- [`KT-62780`](https://youtrack.jetbrains.com/issue/KT-62780) K/JS: Deprecate node-specific properties in NodeJsRootExtension
- [`KT-63544`](https://youtrack.jetbrains.com/issue/KT-63544) KGP: JS - KotlinJsIrLink is not compatible with Gradle CC starting 8.4
- [`KT-63312`](https://youtrack.jetbrains.com/issue/KT-63312) KJS: Apply IR flags for JS compilations unconditionally
- [`KT-62633`](https://youtrack.jetbrains.com/issue/KT-62633) wasmWasi/JsNodeTest tasks are always not up-to-date
- [`KT-63040`](https://youtrack.jetbrains.com/issue/KT-63040) K/JS: Rework outputs of webpack and distribution task

### Tools. Gradle. Multiplatform

- [`KT-63315`](https://youtrack.jetbrains.com/issue/KT-63315) Wasm gradle plugin DSL is invalid for parameterless wasmWasi method
- [`KT-63338`](https://youtrack.jetbrains.com/issue/KT-63338) [KMP] metadata task fails to find cinterop classes from dependency projects
- [`KT-63197`](https://youtrack.jetbrains.com/issue/KT-63197) After using Kotlin 1.9.20 on Windows 11, the gradle sync failed
- [`KT-63044`](https://youtrack.jetbrains.com/issue/KT-63044) KGP: Multiplatform - 8.4 configuration cache support
- [`KT-63011`](https://youtrack.jetbrains.com/issue/KT-63011) Apple Framework Artifacts is not connected to KotlinNativeTask

### Tools. Gradle. Native

- [`KT-56455`](https://youtrack.jetbrains.com/issue/KT-56455) Gradle: remove `enableEndorsedLibs` from codebase

### Tools. Incremental Compile

- [`KT-56963`](https://youtrack.jetbrains.com/issue/KT-56963) Add MPP/Jvm incremental compilation tests for both K1 and K2 modes
- [`KT-63876`](https://youtrack.jetbrains.com/issue/KT-63876) Move useful utilities from KmpIncrementalITBase.kt to KGPBaseTest and/or common utils
- [`KT-63010`](https://youtrack.jetbrains.com/issue/KT-63010) Build reports may contain incorrect measurements for "Total size of the cache directory"
- [`KT-59178`](https://youtrack.jetbrains.com/issue/KT-59178) With language version = 2.0 incremental compilation of JVM, JS fails on matching expect and actual declarations

### Tools. JPS

- [`KT-63549`](https://youtrack.jetbrains.com/issue/KT-63549) Add compiler performance metrics to JPS build reports
- [`KT-64026`](https://youtrack.jetbrains.com/issue/KT-64026) Maven. JVM target is imported as 1.6(deprecated) if an invalid parameter value specified in pom.xml
- [`KT-63594`](https://youtrack.jetbrains.com/issue/KT-63594) ClassCastException in JPS statistics
- [`KT-63799`](https://youtrack.jetbrains.com/issue/KT-63799) Make plugin classpath serialization path agnostic

### Tools. Kapt

- [`KT-60821`](https://youtrack.jetbrains.com/issue/KT-60821) [KAPT4] Make sure that KAPT produces correct JCTree; if that's not possible, investigate using JavaPoet as an alternative
- [`KT-57389`](https://youtrack.jetbrains.com/issue/KT-57389) KAPT3 uses a Javac API for JCImport which will break in JDK 21

### Tools. Maven

- [`KT-63322`](https://youtrack.jetbrains.com/issue/KT-63322) Add tests for KTIJ-21742

### Tools. Scripts

- [`KT-58367`](https://youtrack.jetbrains.com/issue/KT-58367) Remove script-util from the repo
- [`KT-54819`](https://youtrack.jetbrains.com/issue/KT-54819) Scripts: Not able to use slf4j in .main.kts
- [`KT-63352`](https://youtrack.jetbrains.com/issue/KT-63352) Scripting dependencies resolver logs "file not found" even if the artefact is retrieved

### Tools. Wasm

- [`KT-63417`](https://youtrack.jetbrains.com/issue/KT-63417) KMP hierarchy DSL. Split withWasm() into withWasmJs() and withWasmWasi()


## 2.0.0-Beta1

### Analysis. API

#### Performance Improvements

- [`KT-61789`](https://youtrack.jetbrains.com/issue/KT-61789) K2: optimize getFirForNonKtFileElement for references inside super type reference
- [`KT-59498`](https://youtrack.jetbrains.com/issue/KT-59498) K2: getOnAirGetTowerContextProvider took too much time due to on air resolve
- [`KT-61728`](https://youtrack.jetbrains.com/issue/KT-61728) Analysis API: optimize AllCandidatesResolver.getAllCandidates

#### Fixes

- [`KT-61252`](https://youtrack.jetbrains.com/issue/KT-61252) K2: IDE K2: "By now the annotations argument mapping should have been resolved"
- [`KT-62310`](https://youtrack.jetbrains.com/issue/KT-62310) K2 IDE. False positives errors with external annotations
- [`KT-62397`](https://youtrack.jetbrains.com/issue/KT-62397) K2 IDE. FP Error in the editor on `RequiresOptIn` annotation from the lib despite the warning level
- [`KT-62705`](https://youtrack.jetbrains.com/issue/KT-62705) K2: "lazyResolveToPhase(IMPORTS) cannot be called..." from light classes
- [`KT-62626`](https://youtrack.jetbrains.com/issue/KT-62626) IllegalStateException: Cannot build symbol for class org.jetbrains.kotlin.psi.KtScriptInitializer
- [`KT-62693`](https://youtrack.jetbrains.com/issue/KT-62693) K2: IDE K2: "PSI should present for declaration built by Kotlin code"
- [`KT-62674`](https://youtrack.jetbrains.com/issue/KT-62674) K2: "Scope for type ConeClassLikeTypeImpl" is null from transitive dependencies
- [`KT-61889`](https://youtrack.jetbrains.com/issue/KT-61889) Analysis API: Migrate KtFirReferenceShortener to ContextCollector instead of FirResolveContextCollector
- [`KT-62772`](https://youtrack.jetbrains.com/issue/KT-62772)  Analysis API: No 'org.jetbrains.kotlin.fir.java.FirSyntheticPropertiesStorage'(31) in array owner: LLFirSourcesSession when analysing builtins in a context of common code
- [`KT-61296`](https://youtrack.jetbrains.com/issue/KT-61296) K2: do not resolve the entire file on lazyResolve call if FirFile is passed
- [`KT-60319`](https://youtrack.jetbrains.com/issue/KT-60319) K2 IDE: "Stability for initialized variable always should be computable"
- [`KT-62859`](https://youtrack.jetbrains.com/issue/KT-62859) K2 IDE: "Evaluate expression" throws exception when calling "Any?.toString()"
- [`KT-62421`](https://youtrack.jetbrains.com/issue/KT-62421) K2: IDE K2: "`lazyResolveToPhase(BODY_RESOLVE)` cannot be called from a transformer with a phase BODY_RESOLVE."
- [`KT-63058`](https://youtrack.jetbrains.com/issue/KT-63058) K2 IDE: Code completion unexpectedly imports static/companion object method
- [`KT-62588`](https://youtrack.jetbrains.com/issue/KT-62588) getExpectedType should not calculate type of the expression
- [`KT-61990`](https://youtrack.jetbrains.com/issue/KT-61990) K2: Unexpected returnTypeRef for FirSyntheticProperty
- [`KT-62625`](https://youtrack.jetbrains.com/issue/KT-62625) K2: 'FirLazyExpression should be calculated before accessing' for unresolved super type
- [`KT-62071`](https://youtrack.jetbrains.com/issue/KT-62071) Analysis API: KtFirScopeProvider.getScopeContextForPosition throws exception when ImplicitReceiverValue.implicitScope is null
- [`KT-62691`](https://youtrack.jetbrains.com/issue/KT-62691) K2: optimize getFirForNonKtFileElement for references inside 'where'
- [`KT-62587`](https://youtrack.jetbrains.com/issue/KT-62587) K2 IDE. FP unresolved reference on accessing nested class in annotation argument
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
- [`KT-62302`](https://youtrack.jetbrains.com/issue/KT-62302) Support PsiType -> KtType conversion
- [`KT-60325`](https://youtrack.jetbrains.com/issue/KT-60325) K2 IDE. "IllegalArgumentException: source must not be null" on `throw` usage attempt
- [`KT-61431`](https://youtrack.jetbrains.com/issue/KT-61431) K2: KtPropertyAccessorSymbolPointer pointer already disposed for $$result script property
- [`KT-60957`](https://youtrack.jetbrains.com/issue/KT-60957) K2: Analysis API: Reference shortener does not work correctly with invoke function calls on properties
- [`KT-58490`](https://youtrack.jetbrains.com/issue/KT-58490) K2: LLFirTypeLazyResolver problems
- [`KT-58494`](https://youtrack.jetbrains.com/issue/KT-58494) K2: LLFirAnnotationArgumentsLazyResolver problems
- [`KT-58492`](https://youtrack.jetbrains.com/issue/KT-58492) K2: LLFirBodyLazyResolver problems
- [`KT-58769`](https://youtrack.jetbrains.com/issue/KT-58769) K2: LL FIR: implement platform-dependent session factories
- [`KT-60343`](https://youtrack.jetbrains.com/issue/KT-60343) K2 IDE. IllegalArgumentException on passing incorrect type parameter to function
- [`KT-61383`](https://youtrack.jetbrains.com/issue/KT-61383) K2: 'KtCompilerFacility' fails on code fragment compilation in library sources with duplicated dependencies
- [`KT-61842`](https://youtrack.jetbrains.com/issue/KT-61842) K2: reduce number of "in-block modification" events
- [`KT-62012`](https://youtrack.jetbrains.com/issue/KT-62012) K2: "KtReadActionConfinementLifetimeToken is inaccessible: Called outside analyse method"
- [`KT-61371`](https://youtrack.jetbrains.com/issue/KT-61371) K2: Analysis API standalone: register compiler symbol provider for libraries in standalone mode
- [`KT-61422`](https://youtrack.jetbrains.com/issue/KT-61422) K2 IDE: "No array element type for vararg value parameter: org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl"
- [`KT-60611`](https://youtrack.jetbrains.com/issue/KT-60611) K2: reduce number of "in-block modification" events
- [`KT-61425`](https://youtrack.jetbrains.com/issue/KT-61425) Analysis API: Provide a way to get a declared member scope for an enum entry's initializing anonymous object
- [`KT-61405`](https://youtrack.jetbrains.com/issue/KT-61405) Analysis API: An enum entry should not be a `KtSymbolWithMembers`
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

### Backend. Native. Debug

- [`KT-57365`](https://youtrack.jetbrains.com/issue/KT-57365) [Native] Incorrect debug info on inline function call site

### Backend. Wasm

- [`KT-62147`](https://youtrack.jetbrains.com/issue/KT-62147) [Kotlin/Wasm] Nothing typed when expression cause a backend error
- [`KT-61958`](https://youtrack.jetbrains.com/issue/KT-61958) Update SpiderMonkey and return its usage in box tests when they switch to the final opcodes for GC and FTR proposals
- [`KT-60828`](https://youtrack.jetbrains.com/issue/KT-60828) K/Wasm: return br_on_cast_fail usages
- [`KT-59720`](https://youtrack.jetbrains.com/issue/KT-59720) K/Wasm: update to final opcodes
- [`KT-59084`](https://youtrack.jetbrains.com/issue/KT-59084) WASM: "RuntimeError: illegal cast" caused by inline class and JsAny
- [`KT-60700`](https://youtrack.jetbrains.com/issue/KT-60700) [WASM] test FirWasmCodegenBoxTestGenerated.testSuspendUnitConversion failed after KT-60259

### Compiler

#### New Features

- [`KT-62239`](https://youtrack.jetbrains.com/issue/KT-62239) Allow enum entries without parentheses uniformly
- [`KT-22004`](https://youtrack.jetbrains.com/issue/KT-22004) Allow to resolve CONFLICTING_OVERLOADS with Deprecated(HIDDEN)
- [`KT-11712`](https://youtrack.jetbrains.com/issue/KT-11712) Smart cast is not applied for invisible setter
- [`KT-61077`](https://youtrack.jetbrains.com/issue/KT-61077) Support provideDelegate inference from var property type
- [`KT-59504`](https://youtrack.jetbrains.com/issue/KT-59504) K2 compiler does not require resolved 'componentX' functions for the placeholder ('_') variables in the destructuring declarations

#### Performance Improvements

- [`KT-62619`](https://youtrack.jetbrains.com/issue/KT-62619) FIR: Checker performance regression due to MISSING_DEPENDENCY checkers
- [`KT-62044`](https://youtrack.jetbrains.com/issue/KT-62044) Do not add nullability annotations to the methods of anonymous class
- [`KT-62706`](https://youtrack.jetbrains.com/issue/KT-62706) Optimize KtSourceElement.findChild()
- [`KT-62513`](https://youtrack.jetbrains.com/issue/KT-62513) Do not add nullability annotations to the methods of local classes
- [`KT-61991`](https://youtrack.jetbrains.com/issue/KT-61991) K2: avoid redundant full body resolution for properties during implicit type phase
- [`KT-61604`](https://youtrack.jetbrains.com/issue/KT-61604) [K/N] Bitcode dependency linking is slow for large compilations
- [`KT-39054`](https://youtrack.jetbrains.com/issue/KT-39054) Optimize delegated properties which call get/set on the given KProperty instance on JVM
- [`KT-61635`](https://youtrack.jetbrains.com/issue/KT-61635) K2: `getConstructorKeyword` call in `PsiRawFirBuilder.toFirConstructor` forces AST load
- [`KT-57616`](https://youtrack.jetbrains.com/issue/KT-57616) K2: Consider optimizing reversed versions of persistent lists in FirTowerDataContext

#### Fixes

- [`KT-63257`](https://youtrack.jetbrains.com/issue/KT-63257) K2: FIR2IR inserts incorrect implicit cast for smartcasted variable
- [`KT-61459`](https://youtrack.jetbrains.com/issue/KT-61459) K2: type parameters cannot be parameterized with type arguments
- [`KT-61959`](https://youtrack.jetbrains.com/issue/KT-61959) K2: Type parameters from outer class leak to nested class
- [`KT-61384`](https://youtrack.jetbrains.com/issue/KT-61384) IrFakeOverrideBuilder incorrectly checks visibility for friend modules
- [`KT-62032`](https://youtrack.jetbrains.com/issue/KT-62032) K2: Render flexible types as A..B instead of cryptic ft<A, B> in diagnostic messages
- [`KT-59940`](https://youtrack.jetbrains.com/issue/KT-59940) K2: Disappeared ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE
- [`KT-61243`](https://youtrack.jetbrains.com/issue/KT-61243) K2: Always use declaredMemberScope-s in `FirConflictsHelpers` instead of `declarations`
- [`KT-59430`](https://youtrack.jetbrains.com/issue/KT-59430) K2: Missing CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY
- [`KT-56683`](https://youtrack.jetbrains.com/issue/KT-56683) K2: No control flow analysis for top-level properties
- [`KT-62334`](https://youtrack.jetbrains.com/issue/KT-62334) K2: FIR should not generate delegated functions for methods from java interface with default implementation
- [`KT-59590`](https://youtrack.jetbrains.com/issue/KT-59590) JVM IR: NotImplementedError during rendering of conflicting JVM signatures diagnostic
- [`KT-62607`](https://youtrack.jetbrains.com/issue/KT-62607) K2: "Overload resolution ambiguity between candidates"
- [`KT-55096`](https://youtrack.jetbrains.com/issue/KT-55096) K2: false-positive smartcast after equals check with reassignment in RHS of ==
- [`KT-62590`](https://youtrack.jetbrains.com/issue/KT-62590) Split expect/actual matcher-checker machinery in two separate components: matcher and checker
- [`KT-62120`](https://youtrack.jetbrains.com/issue/KT-62120) K2: "NoSuchMethodError: java.lang.String" at runtime on class delegating to Java type
- [`KT-62916`](https://youtrack.jetbrains.com/issue/KT-62916) K2: False positive INCOMPATIBLE_MATCHING
- [`KT-62752`](https://youtrack.jetbrains.com/issue/KT-62752) expect-actual matcher/checker: return type must be "checking" incompatibility
- [`KT-62137`](https://youtrack.jetbrains.com/issue/KT-62137) Compiler fails on null tracking (inference) for safe call
- [`KT-59744`](https://youtrack.jetbrains.com/issue/KT-59744) K2:  false negative VAL_REASSIGNMENT  in case of reassignment inside custom setter
- [`KT-58531`](https://youtrack.jetbrains.com/issue/KT-58531) K2: "Property must be initialized" compile error
- [`KT-62404`](https://youtrack.jetbrains.com/issue/KT-62404) K2 Scripting for gradle: unresolved name errors on implicit imports
- [`KT-62305`](https://youtrack.jetbrains.com/issue/KT-62305) K2: Missing Fir metadata serialization support for scripts
- [`KT-62197`](https://youtrack.jetbrains.com/issue/KT-62197) K2 and Apache Commons's MutableLong: Overload resolution ambiguity between candidates
- [`KT-53551`](https://youtrack.jetbrains.com/issue/KT-53551) suspend functional type with context receiver causes ClassCastException
- [`KT-61491`](https://youtrack.jetbrains.com/issue/KT-61491) K2 AA: Multiple FIR declarations for the same delegated property
- [`KT-55965`](https://youtrack.jetbrains.com/issue/KT-55965) K2: NPE via usage of functions that return Nothing but have no return expressions
- [`KT-60942`](https://youtrack.jetbrains.com/issue/KT-60942) K2: Transitive dependency IR is not deserialized correctly
- [`KT-55319`](https://youtrack.jetbrains.com/issue/KT-55319) K2: False negative NON_LOCAL_RETURN_NOT_ALLOWED for non-local returns example
- [`KT-62151`](https://youtrack.jetbrains.com/issue/KT-62151) K2. overload resolution ambiguity for calls of Java record compact constructors
- [`KT-62944`](https://youtrack.jetbrains.com/issue/KT-62944) K2: Symbols with context receiver shouldn't be rendered with line break
- [`KT-62394`](https://youtrack.jetbrains.com/issue/KT-62394) K2: Synthetic property scope doesn't consider java classes in the hierarchy
- [`KT-60117`](https://youtrack.jetbrains.com/issue/KT-60117) K2: ISE “Cannot serialize error type: ERROR CLASS: Cannot infer variable type without initializer / getter / delegate” on compiling lateinit property without initialization
- [`KT-61039`](https://youtrack.jetbrains.com/issue/KT-61039) False positive ABSTRACT_MEMBER_NOT_IMPLEMENTED in K1 when expect actual super types scopes don't match
- [`KT-60042`](https://youtrack.jetbrains.com/issue/KT-60042) K2: Introduced PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS
- [`KT-59402`](https://youtrack.jetbrains.com/issue/KT-59402) K2: Missing EXPANSIVE_INHERITANCE and EXPANSIVE_INHERITANCE_IN_JAVA
- [`KT-62467`](https://youtrack.jetbrains.com/issue/KT-62467) K2: Result type of elvis operator should be flexible if rhs is flexible
- [`KT-62126`](https://youtrack.jetbrains.com/issue/KT-62126) KJS / K2: "InterpreterError: VALUE_PARAMETER" caused by reflection, delegation and languageVersion = 1.9
- [`KT-62679`](https://youtrack.jetbrains.com/issue/KT-62679) K2: drop ARGUMENTS_OF_ANNOTATIONS phase
- [`KT-56615`](https://youtrack.jetbrains.com/issue/KT-56615) K2: False-negative USELESS_CAST after double smartcast
- [`KT-59820`](https://youtrack.jetbrains.com/issue/KT-59820) K2: Investigate the TODO in FirCastDiagnosticsHelpers
- [`KT-61100`](https://youtrack.jetbrains.com/issue/KT-61100) K2: wrong type for "value" parameter of java annotation constructor
- [`KT-62491`](https://youtrack.jetbrains.com/issue/KT-62491) K2. No `'when' expression must be exhaustive` error when Java sealed class inheritors are not listed in `permits` clause
- [`KT-60095`](https://youtrack.jetbrains.com/issue/KT-60095) K2: Introduced INCOMPATIBLE_TYPES
- [`KT-61598`](https://youtrack.jetbrains.com/issue/KT-61598) K2: report IR_WITH_UNSTABLE_ABI_COMPILED_CLASS
- [`KT-59561`](https://youtrack.jetbrains.com/issue/KT-59561) K2/MPP reports INCOMPATIBLE_MATCHING when an actual annotation declaration with vararg property is typealias with `@Suppress`
- [`KT-60123`](https://youtrack.jetbrains.com/issue/KT-60123) K2: PROPERTY_WITH_NO_TYPE_NO_INITIALIZER isn't working in IDE for lateinit property without a type
- [`KT-59935`](https://youtrack.jetbrains.com/issue/KT-59935) K2: Disappeared PROPERTY_WITH_NO_TYPE_NO_INITIALIZER
- [`KT-58455`](https://youtrack.jetbrains.com/issue/KT-58455) K2(LT). Internal compiler error "UninitializedPropertyAccessException: lateinit property identifier has not been initialized" on missing type parameter in "where" constraint
- [`KT-60714`](https://youtrack.jetbrains.com/issue/KT-60714) K2: Implement resolve to private members from Evaluator in K2
- [`KT-59577`](https://youtrack.jetbrains.com/issue/KT-59577) K2. Enum constant name is not specified in error text
- [`KT-60003`](https://youtrack.jetbrains.com/issue/KT-60003) K2: Disappeared INVALID_CHARACTERS_NATIVE_ERROR
- [`KT-62099`](https://youtrack.jetbrains.com/issue/KT-62099) K2: "Type arguments should be specified for an outer class" error about typealias
- [`KT-60983`](https://youtrack.jetbrains.com/issue/KT-60983) K2: "Argument type mismatch: actual type is android/view/View.OnApplyWindowInsetsListener but androidx/core/view/OnApplyWindowInsetsListener? was expected"
- [`KT-60111`](https://youtrack.jetbrains.com/issue/KT-60111) K2: Location regressions for operators
- [`KT-59399`](https://youtrack.jetbrains.com/issue/KT-59399) K2: Missing JSCODE_NO_JAVASCRIPT_PRODUCED
- [`KT-59388`](https://youtrack.jetbrains.com/issue/KT-59388) K2: Missing JSCODE_ERROR
- [`KT-59435`](https://youtrack.jetbrains.com/issue/KT-59435) K2: Missing JSCODE_ARGUMENT_SHOULD_BE_CONSTANT
- [`KT-60601`](https://youtrack.jetbrains.com/issue/KT-60601) K2 / Maven: Overload resolution ambiguity between candidates inline method
- [`KT-60778`](https://youtrack.jetbrains.com/issue/KT-60778) K2: implement MISSING_DEPENDENCY_CLASS(_SUPERCLASS) errors
- [`KT-62581`](https://youtrack.jetbrains.com/issue/KT-62581) K2: Difference in `kind` flag in metadata
- [`KT-59956`](https://youtrack.jetbrains.com/issue/KT-59956) K2: Disappeared INAPPLICABLE_OPERATOR_MODIFIER
- [`KT-35913`](https://youtrack.jetbrains.com/issue/KT-35913) Diagnostic error VAL_REASSIGNMENT is not reported multiple times
- [`KT-60059`](https://youtrack.jetbrains.com/issue/KT-60059) K2: Introduced VAL_REASSIGNMENT
- [`KT-59945`](https://youtrack.jetbrains.com/issue/KT-59945) K2: Disappeared ANONYMOUS_FUNCTION_WITH_NAME
- [`KT-62573`](https://youtrack.jetbrains.com/issue/KT-62573) K2: incorrect parsing behavior with named functions as expressions
- [`KT-56629`](https://youtrack.jetbrains.com/issue/KT-56629) K2: an instance of USELESS_CAST was not moved under EnableDfaWarningsInK2 language feature
- [`KT-58034`](https://youtrack.jetbrains.com/issue/KT-58034) Inconsistent resolve for nested objects in presence of a companion object property with the same name
- [`KT-59864`](https://youtrack.jetbrains.com/issue/KT-59864) K2: Bad locations with delegates
- [`KT-59584`](https://youtrack.jetbrains.com/issue/KT-59584) K2: Bad startOffset for 'this'
- [`KT-61388`](https://youtrack.jetbrains.com/issue/KT-61388) K2: ISE "Annotations are resolved twice" from CompilerRequiredAnnotationsComputationSession on nested annotation
- [`KT-62027`](https://youtrack.jetbrains.com/issue/KT-62027) "java.lang.IndexOutOfBoundsException: Empty list doesn't contain element at index 0" caused by ClassicExpectActualMatchingContext.kt when annotation `@AllowDifferentMembersInActual` used
- [`KT-61055`](https://youtrack.jetbrains.com/issue/KT-61055) K2: Investigate if usage of `toResolvedCallableSymbol` is correct at FirDataFlowAnalyzer#processConditionalContract
- [`KT-61688`](https://youtrack.jetbrains.com/issue/KT-61688) K2: FIR renderings of type annotations leak through the diagnostics' messages
- [`KT-61794`](https://youtrack.jetbrains.com/issue/KT-61794) FIR: MergePostponedLambdaExitsNode.flow remains uninitialized after resolve
- [`KT-61068`](https://youtrack.jetbrains.com/issue/KT-61068) Bounds of type parameters are not enforced during inheritance of inner classes with generic outer classes
- [`KT-61065`](https://youtrack.jetbrains.com/issue/KT-61065) K2: `@Suppress` annotation is ignored inside preconditions of when-clauses
- [`KT-61937`](https://youtrack.jetbrains.com/issue/KT-61937) K2: implicit script receiver from ScriptDefinition are not visible for invoke
- [`KT-61076`](https://youtrack.jetbrains.com/issue/KT-61076) K2: false-positive conflicting overloads error on suspending function and private Java method from a supertype
- [`KT-61075`](https://youtrack.jetbrains.com/issue/KT-61075) K2: type inference for delegate expressions with complexly bounded type variables fails on properties with annotated accessors
- [`KT-58579`](https://youtrack.jetbrains.com/issue/KT-58579) K2: false-positive new inference error on invoking a generic function on Java wildcard type bounded by raw-typed Java inner class
- [`KT-62671`](https://youtrack.jetbrains.com/issue/KT-62671) K2: fir2ir generates a duplicate of delegated function for class from a common module
- [`KT-60682`](https://youtrack.jetbrains.com/issue/KT-60682) K2: Disappeared DEPRECATION
- [`KT-62143`](https://youtrack.jetbrains.com/issue/KT-62143) Error: Identity equality for arguments of types 'kotlin/Int?' and 'kotlin/Nothing?' is prohibited
- [`KT-61517`](https://youtrack.jetbrains.com/issue/KT-61517) K2: FirModuleDescriptor should correctly provide dependencies from FirModuleData
- [`KT-62578`](https://youtrack.jetbrains.com/issue/KT-62578) K2: `@NoInfer` annotation doesn't work for deserialized functions
- [`KT-59916`](https://youtrack.jetbrains.com/issue/KT-59916) K2: Disappeared REPEATED_ANNOTATION
- [`KT-62450`](https://youtrack.jetbrains.com/issue/KT-62450) K2: Disappeared OPT_IN_USAGE_ERROR for a data class property during the destructuring declaration
- [`KT-59997`](https://youtrack.jetbrains.com/issue/KT-59997) K2: Disappeared OPT_IN_USAGE_ERROR
- [`KT-62393`](https://youtrack.jetbrains.com/issue/KT-62393) K2: FIR doesn't count visibility when creating synthetic property override
- [`KT-61208`](https://youtrack.jetbrains.com/issue/KT-61208) EnumEntries mappings are generated incorrectly in the face of incremental compilation
- [`KT-61786`](https://youtrack.jetbrains.com/issue/KT-61786) K2: Remove type enhancement on java final fields
- [`KT-57811`](https://youtrack.jetbrains.com/issue/KT-57811) K2: make java static string and int fields not null
- [`KT-62531`](https://youtrack.jetbrains.com/issue/KT-62531) InvalidProtocolBufferException on reading module metadata compiled by K2 from compilers earlier than 1.8.20 with -Xskip-metadata-version-check
- [`KT-59371`](https://youtrack.jetbrains.com/issue/KT-59371) K2: Missing MISSING_DEPENDENCY_CLASS
- [`KT-61511`](https://youtrack.jetbrains.com/issue/KT-61511) IrFakeOverride builder: objc overridability condition is not supported
- [`KT-62316`](https://youtrack.jetbrains.com/issue/KT-62316) K2: CONFLICTING_INHERITED_JVM_DECLARATIONS on List subclass inheriting remove/removeAt from Java superclass
- [`KT-60671`](https://youtrack.jetbrains.com/issue/KT-60671) KMP: check other annotation targets in expect and actual annotations compatibility checker
- [`KT-62451`](https://youtrack.jetbrains.com/issue/KT-62451) K2: Disappeared OPT_IN_USAGE_ERROR for typealias
- [`KT-62452`](https://youtrack.jetbrains.com/issue/KT-62452) K2: Violation of OPT_IN_USAGE_ERROR non-propagating opt-in rules for typealias
- [`KT-59927`](https://youtrack.jetbrains.com/issue/KT-59927) K2: Disappeared INVISIBLE_REFERENCE
- [`KT-60104`](https://youtrack.jetbrains.com/issue/KT-60104) K2: Introduced FUNCTION_CALL_EXPECTED
- [`KT-57513`](https://youtrack.jetbrains.com/issue/KT-57513) K2: Bound smart casts don't work with Strings
- [`KT-62146`](https://youtrack.jetbrains.com/issue/KT-62146) K2: `@Suppress` does not work with named argument
- [`KT-62475`](https://youtrack.jetbrains.com/issue/KT-62475) K2: IrExternalModuleFragments contains incorrect data in Fir2Ir
- [`KT-61983`](https://youtrack.jetbrains.com/issue/KT-61983) K2: *fir.kt.txt dump uses different naming approach for local vars
- [`KT-59970`](https://youtrack.jetbrains.com/issue/KT-59970) K2: Disappeared NULLABLE_TYPE_IN_CLASS_LITERAL_LHS
- [`KT-58216`](https://youtrack.jetbrains.com/issue/KT-58216) K2 (2.0): when is not checked for exhaustiveness with Java sealed class
- [`KT-62036`](https://youtrack.jetbrains.com/issue/KT-62036) KMP: consider prohibiting `actual fake-override` when the corresponding `expect class` has default arguments
- [`KT-61205`](https://youtrack.jetbrains.com/issue/KT-61205) Compose Compiler K2/ios: No file for /App|App(){}[0] when running linkPodDebugFrameworkIosX64
- [`KT-58240`](https://youtrack.jetbrains.com/issue/KT-58240) Support running irText compiler tests against the Native backend
- [`KT-59565`](https://youtrack.jetbrains.com/issue/KT-59565) K2. Internal error "IndexOutOfBoundsException: Index -1 out of bounds for length 0" on incorrect usage of annotation in type parameter
- [`KT-59393`](https://youtrack.jetbrains.com/issue/KT-59393) K2: Missing TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED
- [`KT-59954`](https://youtrack.jetbrains.com/issue/KT-59954) K2: Disappeared REPEATED_MODIFIER
- [`KT-62127`](https://youtrack.jetbrains.com/issue/KT-62127) "NoSuchFieldError: TRUE$delegate" on referencing companion's variable in submodule
- [`KT-57100`](https://youtrack.jetbrains.com/issue/KT-57100) K2 does not report Conflicting overloads and backend crashes with Exception during IR lowering on conflict overloading with suspend function
- [`KT-62129`](https://youtrack.jetbrains.com/issue/KT-62129) K2: Verification error on calling an extension from an env with 2+ context receivers
- [`KT-59955`](https://youtrack.jetbrains.com/issue/KT-59955) K2: Disappeared INCOMPATIBLE_MODIFIERS
- [`KT-61572`](https://youtrack.jetbrains.com/issue/KT-61572) [K2/N] Missing diagnostic SUPER_CALL_WITH_DEFAULT_PARAMETERS in test for MPP supercall with default params
- [`KT-59514`](https://youtrack.jetbrains.com/issue/KT-59514) K2: New inference error with jspecify and Java interop
- [`KT-62263`](https://youtrack.jetbrains.com/issue/KT-62263) Turn "different expect/actual members" error into a warning
- [`KT-62262`](https://youtrack.jetbrains.com/issue/KT-62262) [K2/N] tests/samples/uikit compilation fails with NPE in checkCanGenerateOverrideInit
- [`KT-52213`](https://youtrack.jetbrains.com/issue/KT-52213) Context receivers: "No mapping for symbol: VALUE_PARAMETER"  caused by contextual suspending function type with receiver
- [`KT-62114`](https://youtrack.jetbrains.com/issue/KT-62114) K2: Unresolved reference for smart cast inside `when` (but not `if`)
- [`KT-59373`](https://youtrack.jetbrains.com/issue/KT-59373) K2: Missing INVISIBLE_MEMBER
- [`KT-61844`](https://youtrack.jetbrains.com/issue/KT-61844) K2: "Expression * of type * cannot be invoked as a function" caused by private property
- [`KT-60581`](https://youtrack.jetbrains.com/issue/KT-60581) K2 fails with New inference error for assertThat under strange circumstances
- [`KT-61735`](https://youtrack.jetbrains.com/issue/KT-61735) [FIR] Assignment to val with flexible type dispatch receiver causes crash
- [`KT-59942`](https://youtrack.jetbrains.com/issue/KT-59942) K2: Disappeared ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT
- [`KT-62058`](https://youtrack.jetbrains.com/issue/KT-62058) K2: use PRE_RELEASE flag until 2.0-RC
- [`KT-59931`](https://youtrack.jetbrains.com/issue/KT-59931) K2: Disappeared CLASS_LITERAL_LHS_NOT_A_CLASS
- [`KT-59377`](https://youtrack.jetbrains.com/issue/KT-59377) K2: Missing CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM
- [`KT-61645`](https://youtrack.jetbrains.com/issue/KT-61645) K2/KMP: Set stdlib-native before stdlib-commonMain in dependencies for shared native metadata compilation
- [`KT-61974`](https://youtrack.jetbrains.com/issue/KT-61974) K2: "ClassCastException: class cannot be cast to class java.lang.Void" in test
- [`KT-61637`](https://youtrack.jetbrains.com/issue/KT-61637) K2: Store all IR declarations inside Fir2IrDeclarationStorage
- [`KT-61924`](https://youtrack.jetbrains.com/issue/KT-61924) Native: problem with abstract fake override from Any
- [`KT-60921`](https://youtrack.jetbrains.com/issue/KT-60921) K2: IndexOutOfBoundsException on attempt to cast an element to inner class with type parameter
- [`KT-61933`](https://youtrack.jetbrains.com/issue/KT-61933) K2: "`Argument type mismatch: actual type is 'Foo<kotlin/Function0<kotlin/Unit>>' but 'Foo<kotlin/coroutines/SuspendFunction0<kotlin/Unit>>' was expected`"
- [`KT-59429`](https://youtrack.jetbrains.com/issue/KT-59429) K2: Missing ABBREVIATED_NOTHING_RETURN_TYPE
- [`KT-59420`](https://youtrack.jetbrains.com/issue/KT-59420) K2: Missing ABBREVIATED_NOTHING_PROPERTY_TYPE
- [`KT-59965`](https://youtrack.jetbrains.com/issue/KT-59965) K2: Disappeared CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON
- [`KT-61732`](https://youtrack.jetbrains.com/issue/KT-61732) K2: Analysis API: resolve ambiguities in kotlin project
- [`KT-60499`](https://youtrack.jetbrains.com/issue/KT-60499) K2: Order of synthetic fields is different from K1's order
- [`KT-61773`](https://youtrack.jetbrains.com/issue/KT-61773) K2 Native: support reporting PRE_RELEASE_CLASS
- [`KT-61578`](https://youtrack.jetbrains.com/issue/KT-61578) [FIR] Resolution to private companion objects does not produce `INVISIBLE_REFERENCE` diagnostic
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
- [`KT-61972`](https://youtrack.jetbrains.com/issue/KT-61972) K2: FIR2IR crashes on converting data classes in MPP setup
- [`KT-60105`](https://youtrack.jetbrains.com/issue/KT-60105) K2: Introduced UNDERSCORE_USAGE_WITHOUT_BACKTICKS
- [`KT-61443`](https://youtrack.jetbrains.com/issue/KT-61443) K2: Return typeId -1 during JS compilation
- [`KT-60075`](https://youtrack.jetbrains.com/issue/KT-60075) K2: Introduced ACTUAL_WITHOUT_EXPECT
- [`KT-61668`](https://youtrack.jetbrains.com/issue/KT-61668) Put expect/actual diagnostics introduced in 1.9.20 release under 1.9 Language Version
- [`KT-61751`](https://youtrack.jetbrains.com/issue/KT-61751) IrFakeOverrideBuilder: keep flexible type annotations when remapping/substituting types
- [`KT-61778`](https://youtrack.jetbrains.com/issue/KT-61778) K2: Overload resolution ambiguity between expect and non-expect in native build
- [`KT-61367`](https://youtrack.jetbrains.com/issue/KT-61367) K2: Introduce OptIn for FirExpression.coneTypeOrNull
- [`KT-61802`](https://youtrack.jetbrains.com/issue/KT-61802) K2: infinite recursion in constant evaluator causing StackOverflowError
- [`KT-60043`](https://youtrack.jetbrains.com/issue/KT-60043) K2: Introduced PROPERTY_AS_OPERATOR
- [`KT-61691`](https://youtrack.jetbrains.com/issue/KT-61691) K2: This annotation is not applicable to target 'local variable'
- [`KT-59915`](https://youtrack.jetbrains.com/issue/KT-59915) K2: Disappeared TOO_MANY_ARGUMENTS
- [`KT-59925`](https://youtrack.jetbrains.com/issue/KT-59925) K2: Disappeared VIRTUAL_MEMBER_HIDDEN
- [`KT-61173`](https://youtrack.jetbrains.com/issue/KT-61173) K2: FirProperty.hasBackingField is true for an expect val
- [`KT-61696`](https://youtrack.jetbrains.com/issue/KT-61696) K2: Cannot override method of interface if superclass has package-protected method with same signature
- [`KT-59370`](https://youtrack.jetbrains.com/issue/KT-59370) K2: Missing JS_NAME_CLASH
- [`KT-36056`](https://youtrack.jetbrains.com/issue/KT-36056) [FIR] Fix implementation of try/catch/finally in DFA
- [`KT-61719`](https://youtrack.jetbrains.com/issue/KT-61719) K2. Invisible reference is shown for whole type reference instead of single name reference
- [`KT-60248`](https://youtrack.jetbrains.com/issue/KT-60248) K2: Type abbreviations are not stored in IR
- [`KT-59251`](https://youtrack.jetbrains.com/issue/KT-59251) KMP/JS: forbid matching actual callable with dynamic return type to expect callable with non-dynamic return type
- [`KT-61510`](https://youtrack.jetbrains.com/issue/KT-61510) K2: internal declarations are invisible in cyclically dependent modules
- [`KT-60048`](https://youtrack.jetbrains.com/issue/KT-60048) K2: Introduced MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND
- [`KT-59425`](https://youtrack.jetbrains.com/issue/KT-59425) K2: Missing JS_FAKE_NAME_CLASH
- [`KT-61060`](https://youtrack.jetbrains.com/issue/KT-61060) K2: Rewrite delegate inference
- [`KT-59529`](https://youtrack.jetbrains.com/issue/KT-59529) K2: "property delegate must have" caused by class hierarchy
- [`KT-55471`](https://youtrack.jetbrains.com/issue/KT-55471) K2. Unresolved reference for nested type is shown instead of outer class
- [`KT-58896`](https://youtrack.jetbrains.com/issue/KT-58896) K2: Higher priority expect overload candidates in common code lose in overload resolution to non-expects
- [`KT-58476`](https://youtrack.jetbrains.com/issue/KT-58476) Context receivers: "No mapping for symbol: VALUE_PARAMETER" with context-receiver inside suspended lambda calling another suspended function
- [`KT-60780`](https://youtrack.jetbrains.com/issue/KT-60780) K2: missing PRE_RELEASE_CLASS
- [`KT-59855`](https://youtrack.jetbrains.com/issue/KT-59855) K2: Replace FirExpression.typeRef with coneType
- [`KT-59391`](https://youtrack.jetbrains.com/issue/KT-59391) K2: Missing JS_BUILTIN_NAME_CLASH
- [`KT-59392`](https://youtrack.jetbrains.com/issue/KT-59392) K2: Missing NAME_CONTAINS_ILLEGAL_CHARS
- [`KT-59110`](https://youtrack.jetbrains.com/issue/KT-59110) K2. "NotImplementedError: An operation is not implemented." error on incorrect `@Target` annotation
- [`KT-53565`](https://youtrack.jetbrains.com/issue/KT-53565) K2: no WRONG_ANNOTATION_TARGET on when subject
- [`KT-54568`](https://youtrack.jetbrains.com/issue/KT-54568) K2: Type variables leak into implicit `it` parameter of lambdas
- [`KT-60892`](https://youtrack.jetbrains.com/issue/KT-60892) K2: Implement diagnostics around `@OptionalExpectation`
- [`KT-61029`](https://youtrack.jetbrains.com/issue/KT-61029) K2: Duplicates when processing direct overridden callables
- [`KT-60917`](https://youtrack.jetbrains.com/issue/KT-60917) K2: "Unresolved reference" for operator for array value
- [`KT-59367`](https://youtrack.jetbrains.com/issue/KT-59367) K2: Missing MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES
- [`KT-60268`](https://youtrack.jetbrains.com/issue/KT-60268) K2: lazy annotation classes have wrong modality
- [`KT-61129`](https://youtrack.jetbrains.com/issue/KT-61129) K2: Implement error suppression warning
- [`KT-60536`](https://youtrack.jetbrains.com/issue/KT-60536) K2: FIR2IR Crash when resolving to companion of internal class with Suppress("INVISIBLE_REFERENCE")
- [`KT-55196`](https://youtrack.jetbrains.com/issue/KT-55196) K2: False-negative CONST_VAL_WITH_NON_CONST_INITIALIZER on boolean .not() call
- [`KT-60292`](https://youtrack.jetbrains.com/issue/KT-60292) K2: annotations on local delegated properties are lost
- [`KT-59418`](https://youtrack.jetbrains.com/issue/KT-59418) K2: Missing DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE
- [`KT-59422`](https://youtrack.jetbrains.com/issue/KT-59422) K2: Missing NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION
- [`KT-57963`](https://youtrack.jetbrains.com/issue/KT-57963) K2: MPP: Annotation calls should be actualized
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
- [`KT-60531`](https://youtrack.jetbrains.com/issue/KT-60531) K2/JS: Report diagnostics before running FIR2IR
- [`KT-59900`](https://youtrack.jetbrains.com/issue/KT-59900) K2: Disappeared NESTED_CLASS_NOT_ALLOWED
- [`KT-59344`](https://youtrack.jetbrains.com/issue/KT-59344) K2: implement deprecation warnings from KT-53153
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
- [`KT-61249`](https://youtrack.jetbrains.com/issue/KT-61249) Move fir-related code from backend.native module
- [`KT-59478`](https://youtrack.jetbrains.com/issue/KT-59478) K2: StackOverflowError on invalid code with nullable unresolved
- [`KT-59893`](https://youtrack.jetbrains.com/issue/KT-59893) K2: Disappeared WRONG_NUMBER_OF_TYPE_ARGUMENTS
- [`KT-60450`](https://youtrack.jetbrains.com/issue/KT-60450) K2: IOOBE from analyzeAndGetLambdaReturnArguments
- [`KT-61442`](https://youtrack.jetbrains.com/issue/KT-61442) K2: Consider stricter filtering on implicit integer coercion
- [`KT-61441`](https://youtrack.jetbrains.com/issue/KT-61441) K2: Wrong overload is chosen with ImplicitIntegerCoercion enabled
- [`KT-57076`](https://youtrack.jetbrains.com/issue/KT-57076) K2 does not report 'More than one overridden descriptor declares a default value'
- [`KT-55672`](https://youtrack.jetbrains.com/issue/KT-55672) K2. Operator name message instead of "Unresolved reference" when operator isn't defined for type
- [`KT-60252`](https://youtrack.jetbrains.com/issue/KT-60252) K2: Supertype argument is not substituted in fake override receivers and value parameters
- [`KT-60687`](https://youtrack.jetbrains.com/issue/KT-60687) K2: Introduced UNEXPECTED_SAFE_CALL
- [`KT-59664`](https://youtrack.jetbrains.com/issue/KT-59664) Inline modifier can be added to a constructor parameter, but it does not have any effect
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
- [`KT-57738`](https://youtrack.jetbrains.com/issue/KT-57738) K2: unresolved class fields and methods in kotlin scripts
- [`KT-60885`](https://youtrack.jetbrains.com/issue/KT-60885) K2: Fix `testSelfUpperBoundInference` test in LV 2.0 branch
- [`KT-59957`](https://youtrack.jetbrains.com/issue/KT-59957) K2: Missing UNSUPPORTED_SEALED_FUN_INTERFACE
- [`KT-60000`](https://youtrack.jetbrains.com/issue/KT-60000) K2: Missing UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION
- [`KT-60886`](https://youtrack.jetbrains.com/issue/KT-60886) K2: Fix `testDirectoryWithRelativePath` in LV 2.0 branch
- [`KT-60002`](https://youtrack.jetbrains.com/issue/KT-60002) K2: Missing UNSUPPORTED_SUSPEND_TEST
- [`KT-59419`](https://youtrack.jetbrains.com/issue/KT-59419) K2: Missing MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE
- [`KT-60297`](https://youtrack.jetbrains.com/issue/KT-60297) K2: finally block is not coerced to unit
- [`KT-59416`](https://youtrack.jetbrains.com/issue/KT-59416) K2: Missing EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT
- [`KT-59417`](https://youtrack.jetbrains.com/issue/KT-59417) K2: Missing CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE
- [`KT-59381`](https://youtrack.jetbrains.com/issue/KT-59381) K2: Missing CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM
- [`KT-59384`](https://youtrack.jetbrains.com/issue/KT-59384) K2: Missing DYNAMIC_NOT_ALLOWED
- [`KT-59406`](https://youtrack.jetbrains.com/issue/KT-59406) K2: Missing PROPERTY_DELEGATION_BY_DYNAMIC
- [`KT-57223`](https://youtrack.jetbrains.com/issue/KT-57223) K2: false-negative INAPPLICABLE_JVM_NAME on non-final properties outside interfaces
- [`KT-59413`](https://youtrack.jetbrains.com/issue/KT-59413) K2: Missing VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS
- [`KT-59387`](https://youtrack.jetbrains.com/issue/KT-59387) K2: Missing NO_CONSTRUCTOR
- [`KT-57803`](https://youtrack.jetbrains.com/issue/KT-57803) K2. "Kotlin: Only the Kotlin standard library is allowed to use the 'kotlin' package" error missing in 2.0

### IDE

#### Fixes

- [`KT-62777`](https://youtrack.jetbrains.com/issue/KT-62777) K2 IDE: False positive MISSING_DEPENDENCY_SUPERCLASS for built-ins in non-JVM modules
- [`KT-61195`](https://youtrack.jetbrains.com/issue/KT-61195) UAST modeling of implicit `it` is inconsistent for `Enum.entries`
- [`KT-62757`](https://youtrack.jetbrains.com/issue/KT-62757) SLC: incorrect nullability annotation on aliased type
- [`KT-60318`](https://youtrack.jetbrains.com/issue/KT-60318) K2: disable SLC for non-JVM platforms
- [`KT-62440`](https://youtrack.jetbrains.com/issue/KT-62440) On the fly resolve with light method context doesn't resolve method type parameters
- [`KT-50241`](https://youtrack.jetbrains.com/issue/KT-50241) Make Symbol Light Classes consistent with Ultra Light Classes
- [`KT-56546`](https://youtrack.jetbrains.com/issue/KT-56546) LL FIR: fix lazy resolve contract violation in Symbol Light Classes
- [`KT-57550`](https://youtrack.jetbrains.com/issue/KT-57550) K2: AA: incorrect constant value in file-level annotation
- [`KT-61460`](https://youtrack.jetbrains.com/issue/KT-61460) SLC: unnecessary upper bound wildcards (w/ type alias)
- [`KT-61377`](https://youtrack.jetbrains.com/issue/KT-61377) K2: SLC: wrong retention counterpart for AnnotationRetention.BINARY
- [`KT-60603`](https://youtrack.jetbrains.com/issue/KT-60603) K2: Investigate intellij tests failures in branch 2.0
- [`KT-60590`](https://youtrack.jetbrains.com/issue/KT-60590) Fix light classes related tests in branch 2.0

### IDE. Gradle Integration

- [`KT-45775`](https://youtrack.jetbrains.com/issue/KT-45775) Improve quality of Import

### IDE. Multiplatform

- [`KT-63007`](https://youtrack.jetbrains.com/issue/KT-63007) K2: Analysis API Standalone: klibs are not resovled from common code
- [`KT-61520`](https://youtrack.jetbrains.com/issue/KT-61520) Sources.jar is not imported for common and intermediate source-sets from the MPP library

### IDE. Script

- [`KT-60418`](https://youtrack.jetbrains.com/issue/KT-60418) K2 scripting: highlighting sometimes fails
- [`KT-60987`](https://youtrack.jetbrains.com/issue/KT-60987) K2: Analysis API: make build.gradle.kts resolution work on build scripts from kotlin projects

### IR. Actualizer

- [`KT-62623`](https://youtrack.jetbrains.com/issue/KT-62623) K2: Ir actualizer leaves inconsistent module links from files

### IR. Tree

- [`KT-61934`](https://youtrack.jetbrains.com/issue/KT-61934) Decouple building fake overrides from symbol table and build scheduling
- [`KT-60923`](https://youtrack.jetbrains.com/issue/KT-60923) IR: Mark IrSymbol.owner with OptIn

### JavaScript

- [`KT-61795`](https://youtrack.jetbrains.com/issue/KT-61795) KJS: Incremental Cache is not invalidated if `useEsClasses` compiler argument was changed
- [`KT-62425`](https://youtrack.jetbrains.com/issue/KT-62425) K/JS: Implement K2 and K1 diagnostics for checking argument passing to js()
- [`KT-58685`](https://youtrack.jetbrains.com/issue/KT-58685) KJS: "IllegalStateException: Not locked" cused by "unlock" called twice
- [`KT-56818`](https://youtrack.jetbrains.com/issue/KT-56818) KJS: "TypeError: Class constructor * cannot be invoked without 'new'" when extending external class
- [`KT-61710`](https://youtrack.jetbrains.com/issue/KT-61710) K/JS: Implement JS_NAME_CLASH check for top level declarations
- [`KT-61886`](https://youtrack.jetbrains.com/issue/KT-61886) K/JS: Prepare K/JS tests for JS IR BE diagnostics
- [`KT-60829`](https://youtrack.jetbrains.com/issue/KT-60829) Fix JS Incremental tests in 2.0 branch
- [`KT-60785`](https://youtrack.jetbrains.com/issue/KT-60785) KJS: Destructured value class in suspend function fails with Uncaught TypeError: can't convert to primitive type error
- [`KT-60635`](https://youtrack.jetbrains.com/issue/KT-60635) K/JS: Class internal methods may clash with child methods from other module that have the same name
- [`KT-60846`](https://youtrack.jetbrains.com/issue/KT-60846) Fix `IncrementalJsKlibCompilerWithScopeExpansionRunnerTestGenerated` test in 2.0 branch

### KMM Plugin

- [`KT-60169`](https://youtrack.jetbrains.com/issue/KT-60169) Generate gradle version catalog in KMM AS plugin
- [`KT-59269`](https://youtrack.jetbrains.com/issue/KT-59269) Update wizards in KMM AS plugin after 1.9.0 release

### Klibs

- [`KT-61767`](https://youtrack.jetbrains.com/issue/KT-61767) [K/N] Header klibs should keep private underlying properties of value classes
- [`KT-60807`](https://youtrack.jetbrains.com/issue/KT-60807) [klib] Add an option to write out header klibs
- [`KT-61097`](https://youtrack.jetbrains.com/issue/KT-61097) [PL] Don't create an executable if there were errors in PL

### Language Design

- [`KT-58921`](https://youtrack.jetbrains.com/issue/KT-58921) K1/K2: difference in Enum.values resolve priority
- [`KT-61573`](https://youtrack.jetbrains.com/issue/KT-61573) Emit the compilation warning on expect/actual classes. The warning must mention that expect/actual classes are in Beta
- [`KT-62138`](https://youtrack.jetbrains.com/issue/KT-62138) K1: false positive (?) NO_SET_METHOD for += resolved as a combination of Map.get and plus
- [`KT-22841`](https://youtrack.jetbrains.com/issue/KT-22841) Prohibit different member scopes for non-final expect and its actual

### Libraries

- [`KT-62785`](https://youtrack.jetbrains.com/issue/KT-62785) Drop unnecessary suppresses in stdlib after bootstrap update
- [`KT-58588`](https://youtrack.jetbrains.com/issue/KT-58588) Optimizations for sequence functions distinct, flatten
- [`KT-62004`](https://youtrack.jetbrains.com/issue/KT-62004) Drop legacy JS compilations of stdlib and kotlin-test
- [`KT-61614`](https://youtrack.jetbrains.com/issue/KT-61614) WASM: Enum hashCode is not final

### Native

- [`KT-61642`](https://youtrack.jetbrains.com/issue/KT-61642) [K/N] Serialize full IdSignatures to caches
- [`KT-62803`](https://youtrack.jetbrains.com/issue/KT-62803) Konanc has print statement "Produced library API in..." that should be deleted or properly logged at INFO level
- [`KT-61248`](https://youtrack.jetbrains.com/issue/KT-61248) [K/N] Extract native manglers out of `backend.native` module

### Native. Runtime. Memory

- [`KT-57773`](https://youtrack.jetbrains.com/issue/KT-57773) Kotlin/Native: track memory in big chunks in the GC scheduler
- [`KT-61093`](https://youtrack.jetbrains.com/issue/KT-61093) Kotlin/Native: enable concurrent weak processing by default

### Native. Stdlib

- [`KT-60514`](https://youtrack.jetbrains.com/issue/KT-60514) Add llvm filecheck tests for atomic intrinsics

### Native. Testing

- [`KT-62157`](https://youtrack.jetbrains.com/issue/KT-62157) Native: Migrate FileCheck tests to new native test infra

### Reflection

- [`KT-60984`](https://youtrack.jetbrains.com/issue/KT-60984) K2: java.lang.ClassNotFoundException: kotlin.Array in runtime with Spring Boot test
- [`KT-60709`](https://youtrack.jetbrains.com/issue/KT-60709) Reflection: Not recognized bound receiver in case of 'equals' always returning true
- [`KT-61304`](https://youtrack.jetbrains.com/issue/KT-61304) Reflection: Calling data class `copy` method via reflection (callBy) fails when the data class has exactly 64 fields

### Tools. CLI

- [`KT-62644`](https://youtrack.jetbrains.com/issue/KT-62644) Don't enable in progressive mode bug-fix features without target version
- [`KT-62350`](https://youtrack.jetbrains.com/issue/KT-62350) CLI: no color output on Apple silicon Macs
- [`KT-61156`](https://youtrack.jetbrains.com/issue/KT-61156) K2: do not try to run compilation if there were errors during calculation of Java module graph
- [`KT-48026`](https://youtrack.jetbrains.com/issue/KT-48026) Add the compiler X-flag to enable self upper bound type inference

### Tools. Compiler Plugin API

- [`KT-61872`](https://youtrack.jetbrains.com/issue/KT-61872) K2: Adding annotations to metadata from backend plugin doesn't work in the presence of comments on annotated declaration

### Tools. Compiler Plugins

- [`KT-60849`](https://youtrack.jetbrains.com/issue/KT-60849) jvm-abi-gen: do not treat hasConstant property flag as a part of ABI for non-const properties
- [`KT-53926`](https://youtrack.jetbrains.com/issue/KT-53926) K2. Don't check serializable properties from supertypes

### Tools. Compiler plugins. Serialization

- [`KT-62215`](https://youtrack.jetbrains.com/issue/KT-62215) Serialization / Native: "IllegalArgumentException: No container found for type parameter" caused by serializing generic classes with a field that uses generics
- [`KT-62522`](https://youtrack.jetbrains.com/issue/KT-62522) K2 + kotlinx.serialization + Native: NPE when generic base class has inheritor in other module

### Tools. Gradle

#### New Features

- [`KT-59627`](https://youtrack.jetbrains.com/issue/KT-59627) FUS base plugin
- [`KT-62025`](https://youtrack.jetbrains.com/issue/KT-62025) K/Wasm: Support binaryen for wasi

#### Performance Improvements

- [`KT-62318`](https://youtrack.jetbrains.com/issue/KT-62318) Android Studio sync memory leak in 1.9.20-Beta

#### Fixes

- [`KT-62650`](https://youtrack.jetbrains.com/issue/KT-62650) Gradle: Return the usage of `kotlin-compiler-embeddable` back
- [`KT-61295`](https://youtrack.jetbrains.com/issue/KT-61295) `KotlinTestReport` captures `Project.buildDir` too early
- [`KT-62987`](https://youtrack.jetbrains.com/issue/KT-62987) Add tests for statistics plugin in Aggregate build
- [`KT-62964`](https://youtrack.jetbrains.com/issue/KT-62964) Build Gradle plugin against Gradle 8.4 API
- [`KT-61896`](https://youtrack.jetbrains.com/issue/KT-61896) Gradle: compilation via build tools API doesn't perform Gradle side output backups
- [`KT-61918`](https://youtrack.jetbrains.com/issue/KT-61918) Removal of an associated compilation from a build script doesn't lead to full recompilation
- [`KT-59826`](https://youtrack.jetbrains.com/issue/KT-59826) Update SimpleKotlinGradleIT#testProjectIsolation to run on Gradle 8
- [`KT-61401`](https://youtrack.jetbrains.com/issue/KT-61401) The reported language version value for KotlinNativeLink tasks in build reports and build scans is inaccurate
- [`KT-62024`](https://youtrack.jetbrains.com/issue/KT-62024) K/Wasm: Binaryen modifying compiler output
- [`KT-61950`](https://youtrack.jetbrains.com/issue/KT-61950) K/Wasm: Add warning about changed sourceSets
- [`KT-61895`](https://youtrack.jetbrains.com/issue/KT-61895) KotlinTopLevelExtension.useCompilerVersion is not marked as experimental
- [`KT-56574`](https://youtrack.jetbrains.com/issue/KT-56574) Implement a prototype of Kotlin JVM compilation pipeline via the build tools API
- [`KT-61206`](https://youtrack.jetbrains.com/issue/KT-61206) Build system classes may leak into the Build Tools API classloader
- [`KT-61737`](https://youtrack.jetbrains.com/issue/KT-61737) GradleStyleMessageRenderer.render misses a space between the file and the message when `location` is (line:column = 0:0)
- [`KT-61457`](https://youtrack.jetbrains.com/issue/KT-61457) Kotlin Gradle Plugin should not use internal deprecated StartParameterInternal.isConfigurationCache

### Tools. Gradle. JS

- [`KT-41382`](https://youtrack.jetbrains.com/issue/KT-41382) NI / KJS / Gradle: TYPE_MISMATCH caused by compilations.getting delegate
- [`KT-53077`](https://youtrack.jetbrains.com/issue/KT-53077) KJS / Gradle: Remove redundant gradle js log on kotlin build
- [`KT-61992`](https://youtrack.jetbrains.com/issue/KT-61992) KJS / Gradle: KotlinJsTest using KotlinMocha should not show output, and should not run a dry-run every time.
- [`KT-56300`](https://youtrack.jetbrains.com/issue/KT-56300) KJS / Gradle: plugin should not add repositories unconditionally
- [`KT-55620`](https://youtrack.jetbrains.com/issue/KT-55620) KJS / Gradle: plugin doesn't support repositoriesMode
- [`KT-56465`](https://youtrack.jetbrains.com/issue/KT-56465) MPP: Import with npm dependency fails with "UninitializedPropertyAccessException: lateinit property fileHasher has not been initialized" if there is no selected JavaScript environment for JS target
- [`KT-41578`](https://youtrack.jetbrains.com/issue/KT-41578) Kotlin/JS: contiuous mode: changes in static resources do not reload browser page

### Tools. Gradle. Kapt

- [`KT-22261`](https://youtrack.jetbrains.com/issue/KT-22261) Annotation Processor - in gradle, kapt configuration is missing extendsFrom
- [`KT-62518`](https://youtrack.jetbrains.com/issue/KT-62518) kapt processing is skipped when all annotation processors are indirect dependencies

### Tools. Gradle. Multiplatform

- [`KT-62601`](https://youtrack.jetbrains.com/issue/KT-62601) AS/IntelliJ exception after updating a KMP project with a macos target to Kotlin 1.9.20-RC
- [`KT-60734`](https://youtrack.jetbrains.com/issue/KT-60734) Handle the migration from ios shortcut and source set with `getting`
- [`KT-59042`](https://youtrack.jetbrains.com/issue/KT-59042) "Cannot build 'KotlinProjectStructureMetadata' during project configuration phase" when configuration cache enabled
- [`KT-62029`](https://youtrack.jetbrains.com/issue/KT-62029) Kotlin 1.9.20-Beta fails to detect some transitive dependency references in JVM+Android source set
- [`KT-61652`](https://youtrack.jetbrains.com/issue/KT-61652) MPP ConcurrentModificationException on transformCommonMainDependenciesMetadata
- [`KT-61622`](https://youtrack.jetbrains.com/issue/KT-61622) Upgrading to Kotlin 1.9 prevents commonMain sourceset classes from being processed by kapt/ksp (dagger/Hilt)
- [`KT-61540`](https://youtrack.jetbrains.com/issue/KT-61540) K2: KMP/K2: Metadata compilations: Discriminate expect over actual by sorting compile path using refines edges
- [`KT-59020`](https://youtrack.jetbrains.com/issue/KT-59020) 1.9.0 Beta Kotlin plugin Gradle sync fails with intermediate JVM + Android source set
- [`KT-60860`](https://youtrack.jetbrains.com/issue/KT-60860) K2: Fix `KotlinNativeCompileArgumentsTest` in 2.0 branch
- [`KT-61463`](https://youtrack.jetbrains.com/issue/KT-61463) KMP: Remove unused 'kpm' code

### Tools. Gradle. Native

- [`KT-51553`](https://youtrack.jetbrains.com/issue/KT-51553) Migrate all Kotlin Gradle plugin/Native tests to new test DSL and add CI configuration to run them
- [`KT-61657`](https://youtrack.jetbrains.com/issue/KT-61657) KonanTarget should implement equals or custom serialization
- [`KT-62907`](https://youtrack.jetbrains.com/issue/KT-62907) Turn on downloading Kotlin Native from maven by default
- [`KT-61700`](https://youtrack.jetbrains.com/issue/KT-61700) Native: linkDebugExecutableNative has duplicated freeCompilerArgs

### Tools. Incremental Compile

- [`KT-61865`](https://youtrack.jetbrains.com/issue/KT-61865) Add support for incremental compilation within the in-process execution strategy in the build tools api
- [`KT-61590`](https://youtrack.jetbrains.com/issue/KT-61590) K2/KMP: Expect actual matching is breaking on the incremental compilation
- [`KT-60831`](https://youtrack.jetbrains.com/issue/KT-60831) Fix IncrementalMultiplatformJvmCompilerRunnerTestGenerated in 2.0 branch

### Tools. JPS

- [`KT-60737`](https://youtrack.jetbrains.com/issue/KT-60737) Investigate/fix JPS-related tests in 2.0 migration branch

### Tools. Kapt

- [`KT-60507`](https://youtrack.jetbrains.com/issue/KT-60507) Kapt: "IllegalAccessError: superclass access check failed" using java 21 toolchain
- [`KT-62438`](https://youtrack.jetbrains.com/issue/KT-62438) Change experimental K2 kapt diagnostic message
- [`KT-61916`](https://youtrack.jetbrains.com/issue/KT-61916) K2 KAPT. Kapt doesn't generate fully qualified names for annotations used as arguments to other annotations
- [`KT-61879`](https://youtrack.jetbrains.com/issue/KT-61879) K2 Kapt: java.lang.NoSuchMethodError during stub generation
- [`KT-61729`](https://youtrack.jetbrains.com/issue/KT-61729) K2: KAPT 4: Compiler crash during compilation of Sphinx for Android
- [`KT-61333`](https://youtrack.jetbrains.com/issue/KT-61333) K2 Kapt: support REPORT_OUTPUT_FILES compiler mode
- [`KT-61761`](https://youtrack.jetbrains.com/issue/KT-61761) Kapt4ToolIntegrationTestGenerated should not use Kapt3ComponentRegistrar

### Tools. Maven

- [`KT-54868`](https://youtrack.jetbrains.com/issue/KT-54868) Stop publishing `kotlin-archetype-js`
- [`KT-26156`](https://youtrack.jetbrains.com/issue/KT-26156) Maven Kotlin Plugin should not WARN when no sources found
- [`KT-60859`](https://youtrack.jetbrains.com/issue/KT-60859) K2: Fix maven `IncrementalCompilationIT` tests in 2.0 branch

### Tools. Parcelize

- [`KT-57685`](https://youtrack.jetbrains.com/issue/KT-57685) Support ImmutableCollections in Parcelize plugin

### Tools. Scripts

- [`KT-62400`](https://youtrack.jetbrains.com/issue/KT-62400) K2: Missing annotation resolving for scripts
- [`KT-61727`](https://youtrack.jetbrains.com/issue/KT-61727) Scripts: Maven artifacts resolution is slow

### Tools. Wasm

- [`KT-61973`](https://youtrack.jetbrains.com/issue/KT-61973) K/Wasm: wasmWasiNodeRun is missed
- [`KT-61971`](https://youtrack.jetbrains.com/issue/KT-61971) K/Wasm: wasmWasiTest should depends on kotlinNodeJsSetup


## 2.0.0-Beta1

### Analysis. API

#### Performance Improvements

- [`KT-61789`](https://youtrack.jetbrains.com/issue/KT-61789) K2: optimize getFirForNonKtFileElement for references inside super type reference
- [`KT-59498`](https://youtrack.jetbrains.com/issue/KT-59498) K2: getOnAirGetTowerContextProvider took too much time due to on air resolve
- [`KT-61728`](https://youtrack.jetbrains.com/issue/KT-61728) Analysis API: optimize AllCandidatesResolver.getAllCandidates

#### Fixes

- [`KT-61252`](https://youtrack.jetbrains.com/issue/KT-61252) K2: IDE K2: "By now the annotations argument mapping should have been resolved"
- [`KT-62310`](https://youtrack.jetbrains.com/issue/KT-62310) K2 IDE. False positives errors with external annotations
- [`KT-62397`](https://youtrack.jetbrains.com/issue/KT-62397) K2 IDE. FP Error in the editor on `RequiresOptIn` annotation from the lib despite the warning level
- [`KT-62705`](https://youtrack.jetbrains.com/issue/KT-62705) K2: "lazyResolveToPhase(IMPORTS) cannot be called..." from light classes
- [`KT-62626`](https://youtrack.jetbrains.com/issue/KT-62626) IllegalStateException: Cannot build symbol for class org.jetbrains.kotlin.psi.KtScriptInitializer
- [`KT-62693`](https://youtrack.jetbrains.com/issue/KT-62693) K2: IDE K2: "PSI should present for declaration built by Kotlin code"
- [`KT-62674`](https://youtrack.jetbrains.com/issue/KT-62674) K2: "Scope for type ConeClassLikeTypeImpl" is null from transitive dependencies
- [`KT-61889`](https://youtrack.jetbrains.com/issue/KT-61889) Analysis API: Migrate KtFirReferenceShortener to ContextCollector instead of FirResolveContextCollector
- [`KT-62772`](https://youtrack.jetbrains.com/issue/KT-62772)  Analysis API: No 'org.jetbrains.kotlin.fir.java.FirSyntheticPropertiesStorage'(31) in array owner: LLFirSourcesSession when analysing builtins in a context of common code
- [`KT-61296`](https://youtrack.jetbrains.com/issue/KT-61296) K2: do not resolve the entire file on lazyResolve call if FirFile is passed
- [`KT-60319`](https://youtrack.jetbrains.com/issue/KT-60319) K2 IDE: "Stability for initialized variable always should be computable"
- [`KT-62859`](https://youtrack.jetbrains.com/issue/KT-62859) K2 IDE: "Evaluate expression" throws exception when calling "Any?.toString()"
- [`KT-62421`](https://youtrack.jetbrains.com/issue/KT-62421) K2: IDE K2: "`lazyResolveToPhase(BODY_RESOLVE)` cannot be called from a transformer with a phase BODY_RESOLVE."
- [`KT-63058`](https://youtrack.jetbrains.com/issue/KT-63058) K2 IDE: Code completion unexpectedly imports static/companion object method
- [`KT-62588`](https://youtrack.jetbrains.com/issue/KT-62588) getExpectedType should not calculate type of the expression
- [`KT-61990`](https://youtrack.jetbrains.com/issue/KT-61990) K2: Unexpected returnTypeRef for FirSyntheticProperty
- [`KT-62625`](https://youtrack.jetbrains.com/issue/KT-62625) K2: 'FirLazyExpression should be calculated before accessing' for unresolved super type
- [`KT-62071`](https://youtrack.jetbrains.com/issue/KT-62071) Analysis API: KtFirScopeProvider.getScopeContextForPosition throws exception when ImplicitReceiverValue.implicitScope is null
- [`KT-62691`](https://youtrack.jetbrains.com/issue/KT-62691) K2: optimize getFirForNonKtFileElement for references inside 'where'
- [`KT-62587`](https://youtrack.jetbrains.com/issue/KT-62587) K2 IDE. FP unresolved reference on accessing nested class in annotation argument
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
- [`KT-62302`](https://youtrack.jetbrains.com/issue/KT-62302) Support PsiType -> KtType conversion
- [`KT-60325`](https://youtrack.jetbrains.com/issue/KT-60325) K2 IDE. "IllegalArgumentException: source must not be null" on `throw` usage attempt
- [`KT-61431`](https://youtrack.jetbrains.com/issue/KT-61431) K2: KtPropertyAccessorSymbolPointer pointer already disposed for $$result script property
- [`KT-60957`](https://youtrack.jetbrains.com/issue/KT-60957) K2: Analysis API: Reference shortener does not work correctly with invoke function calls on properties
- [`KT-58490`](https://youtrack.jetbrains.com/issue/KT-58490) K2: LLFirTypeLazyResolver problems
- [`KT-58494`](https://youtrack.jetbrains.com/issue/KT-58494) K2: LLFirAnnotationArgumentsLazyResolver problems
- [`KT-58492`](https://youtrack.jetbrains.com/issue/KT-58492) K2: LLFirBodyLazyResolver problems
- [`KT-58769`](https://youtrack.jetbrains.com/issue/KT-58769) K2: LL FIR: implement platform-dependent session factories
- [`KT-60343`](https://youtrack.jetbrains.com/issue/KT-60343) K2 IDE. IllegalArgumentException on passing incorrect type parameter to function
- [`KT-61383`](https://youtrack.jetbrains.com/issue/KT-61383) K2: 'KtCompilerFacility' fails on code fragment compilation in library sources with duplicated dependencies
- [`KT-61842`](https://youtrack.jetbrains.com/issue/KT-61842) K2: reduce number of "in-block modification" events
- [`KT-62012`](https://youtrack.jetbrains.com/issue/KT-62012) K2: "KtReadActionConfinementLifetimeToken is inaccessible: Called outside analyse method"
- [`KT-61371`](https://youtrack.jetbrains.com/issue/KT-61371) K2: Analysis API standalone: register compiler symbol provider for libraries in standalone mode
- [`KT-61422`](https://youtrack.jetbrains.com/issue/KT-61422) K2 IDE: "No array element type for vararg value parameter: org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl"
- [`KT-60611`](https://youtrack.jetbrains.com/issue/KT-60611) K2: reduce number of "in-block modification" events
- [`KT-61425`](https://youtrack.jetbrains.com/issue/KT-61425) Analysis API: Provide a way to get a declared member scope for an enum entry's initializing anonymous object
- [`KT-61405`](https://youtrack.jetbrains.com/issue/KT-61405) Analysis API: An enum entry should not be a `KtSymbolWithMembers`
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

### Backend. Native. Debug

- [`KT-57365`](https://youtrack.jetbrains.com/issue/KT-57365) [Native] Incorrect debug info on inline function call site

### Backend. Wasm

- [`KT-62147`](https://youtrack.jetbrains.com/issue/KT-62147) [Kotlin/Wasm] Nothing typed when expression cause a backend error
- [`KT-61958`](https://youtrack.jetbrains.com/issue/KT-61958) Update SpiderMonkey and return its usage in box tests when they switch to the final opcodes for GC and FTR proposals
- [`KT-60828`](https://youtrack.jetbrains.com/issue/KT-60828) K/Wasm: return br_on_cast_fail usages
- [`KT-59720`](https://youtrack.jetbrains.com/issue/KT-59720) K/Wasm: update to final opcodes
- [`KT-59084`](https://youtrack.jetbrains.com/issue/KT-59084) WASM: "RuntimeError: illegal cast" caused by inline class and JsAny
- [`KT-60700`](https://youtrack.jetbrains.com/issue/KT-60700) [WASM] test FirWasmCodegenBoxTestGenerated.testSuspendUnitConversion failed after KT-60259

### Compiler

#### New Features

- [`KT-62239`](https://youtrack.jetbrains.com/issue/KT-62239) Allow enum entries without parentheses uniformly
- [`KT-22004`](https://youtrack.jetbrains.com/issue/KT-22004) Allow to resolve CONFLICTING_OVERLOADS with Deprecated(HIDDEN)
- [`KT-11712`](https://youtrack.jetbrains.com/issue/KT-11712) Smart cast is not applied for invisible setter
- [`KT-61077`](https://youtrack.jetbrains.com/issue/KT-61077) Support provideDelegate inference from var property type
- [`KT-59504`](https://youtrack.jetbrains.com/issue/KT-59504) K2 compiler does not require resolved 'componentX' functions for the placeholder ('_') variables in the destructuring declarations

#### Performance Improvements

- [`KT-62619`](https://youtrack.jetbrains.com/issue/KT-62619) FIR: Checker performance regression due to MISSING_DEPENDENCY checkers
- [`KT-62044`](https://youtrack.jetbrains.com/issue/KT-62044) Do not add nullability annotations to the methods of anonymous class
- [`KT-62706`](https://youtrack.jetbrains.com/issue/KT-62706) Optimize KtSourceElement.findChild()
- [`KT-62513`](https://youtrack.jetbrains.com/issue/KT-62513) Do not add nullability annotations to the methods of local classes
- [`KT-61991`](https://youtrack.jetbrains.com/issue/KT-61991) K2: avoid redundant full body resolution for properties during implicit type phase
- [`KT-61604`](https://youtrack.jetbrains.com/issue/KT-61604) [K/N] Bitcode dependency linking is slow for large compilations
- [`KT-39054`](https://youtrack.jetbrains.com/issue/KT-39054) Optimize delegated properties which call get/set on the given KProperty instance on JVM
- [`KT-61635`](https://youtrack.jetbrains.com/issue/KT-61635) K2: `getConstructorKeyword` call in `PsiRawFirBuilder.toFirConstructor` forces AST load
- [`KT-57616`](https://youtrack.jetbrains.com/issue/KT-57616) K2: Consider optimizing reversed versions of persistent lists in FirTowerDataContext

#### Fixes

- [`KT-63257`](https://youtrack.jetbrains.com/issue/KT-63257) K2: FIR2IR inserts incorrect implicit cast for smartcasted variable
- [`KT-61459`](https://youtrack.jetbrains.com/issue/KT-61459) K2: type parameters cannot be parameterized with type arguments
- [`KT-61959`](https://youtrack.jetbrains.com/issue/KT-61959) K2: Type parameters from outer class leak to nested class
- [`KT-61384`](https://youtrack.jetbrains.com/issue/KT-61384) IrFakeOverrideBuilder incorrectly checks visibility for friend modules
- [`KT-62032`](https://youtrack.jetbrains.com/issue/KT-62032) K2: Render flexible types as A..B instead of cryptic ft<A, B> in diagnostic messages
- [`KT-59940`](https://youtrack.jetbrains.com/issue/KT-59940) K2: Disappeared ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE
- [`KT-61243`](https://youtrack.jetbrains.com/issue/KT-61243) K2: Always use declaredMemberScope-s in `FirConflictsHelpers` instead of `declarations`
- [`KT-59430`](https://youtrack.jetbrains.com/issue/KT-59430) K2: Missing CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY
- [`KT-56683`](https://youtrack.jetbrains.com/issue/KT-56683) K2: No control flow analysis for top-level properties
- [`KT-62334`](https://youtrack.jetbrains.com/issue/KT-62334) K2: FIR should not generate delegated functions for methods from java interface with default implementation
- [`KT-59590`](https://youtrack.jetbrains.com/issue/KT-59590) JVM IR: NotImplementedError during rendering of conflicting JVM signatures diagnostic
- [`KT-62607`](https://youtrack.jetbrains.com/issue/KT-62607) K2: "Overload resolution ambiguity between candidates"
- [`KT-55096`](https://youtrack.jetbrains.com/issue/KT-55096) K2: false-positive smartcast after equals check with reassignment in RHS of ==
- [`KT-62590`](https://youtrack.jetbrains.com/issue/KT-62590) Split expect/actual matcher-checker machinery in two separate components: matcher and checker
- [`KT-62120`](https://youtrack.jetbrains.com/issue/KT-62120) K2: "NoSuchMethodError: java.lang.String" at runtime on class delegating to Java type
- [`KT-62916`](https://youtrack.jetbrains.com/issue/KT-62916) K2: False positive INCOMPATIBLE_MATCHING
- [`KT-62752`](https://youtrack.jetbrains.com/issue/KT-62752) expect-actual matcher/checker: return type must be "checking" incompatibility
- [`KT-62137`](https://youtrack.jetbrains.com/issue/KT-62137) Compiler fails on null tracking (inference) for safe call
- [`KT-59744`](https://youtrack.jetbrains.com/issue/KT-59744) K2:  false negative VAL_REASSIGNMENT  in case of reassignment inside custom setter
- [`KT-58531`](https://youtrack.jetbrains.com/issue/KT-58531) K2: "Property must be initialized" compile error
- [`KT-62404`](https://youtrack.jetbrains.com/issue/KT-62404) K2 Scripting for gradle: unresolved name errors on implicit imports
- [`KT-62305`](https://youtrack.jetbrains.com/issue/KT-62305) K2: Missing Fir metadata serialization support for scripts
- [`KT-62197`](https://youtrack.jetbrains.com/issue/KT-62197) K2 and Apache Commons's MutableLong: Overload resolution ambiguity between candidates
- [`KT-53551`](https://youtrack.jetbrains.com/issue/KT-53551) suspend functional type with context receiver causes ClassCastException
- [`KT-61491`](https://youtrack.jetbrains.com/issue/KT-61491) K2 AA: Multiple FIR declarations for the same delegated property
- [`KT-55965`](https://youtrack.jetbrains.com/issue/KT-55965) K2: NPE via usage of functions that return Nothing but have no return expressions
- [`KT-60942`](https://youtrack.jetbrains.com/issue/KT-60942) K2: Transitive dependency IR is not deserialized correctly
- [`KT-55319`](https://youtrack.jetbrains.com/issue/KT-55319) K2: False negative NON_LOCAL_RETURN_NOT_ALLOWED for non-local returns example
- [`KT-62151`](https://youtrack.jetbrains.com/issue/KT-62151) K2. overload resolution ambiguity for calls of Java record compact constructors
- [`KT-62944`](https://youtrack.jetbrains.com/issue/KT-62944) K2: Symbols with context receiver shouldn't be rendered with line break
- [`KT-62394`](https://youtrack.jetbrains.com/issue/KT-62394) K2: Synthetic property scope doesn't consider java classes in the hierarchy
- [`KT-60117`](https://youtrack.jetbrains.com/issue/KT-60117) K2: ISE “Cannot serialize error type: ERROR CLASS: Cannot infer variable type without initializer / getter / delegate” on compiling lateinit property without initialization
- [`KT-61039`](https://youtrack.jetbrains.com/issue/KT-61039) False positive ABSTRACT_MEMBER_NOT_IMPLEMENTED in K1 when expect actual super types scopes don't match
- [`KT-60042`](https://youtrack.jetbrains.com/issue/KT-60042) K2: Introduced PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS
- [`KT-59402`](https://youtrack.jetbrains.com/issue/KT-59402) K2: Missing EXPANSIVE_INHERITANCE and EXPANSIVE_INHERITANCE_IN_JAVA
- [`KT-62467`](https://youtrack.jetbrains.com/issue/KT-62467) K2: Result type of elvis operator should be flexible if rhs is flexible
- [`KT-62126`](https://youtrack.jetbrains.com/issue/KT-62126) KJS / K2: "InterpreterError: VALUE_PARAMETER" caused by reflection, delegation and languageVersion = 1.9
- [`KT-62679`](https://youtrack.jetbrains.com/issue/KT-62679) K2: drop ARGUMENTS_OF_ANNOTATIONS phase
- [`KT-56615`](https://youtrack.jetbrains.com/issue/KT-56615) K2: False-negative USELESS_CAST after double smartcast
- [`KT-59820`](https://youtrack.jetbrains.com/issue/KT-59820) K2: Investigate the TODO in FirCastDiagnosticsHelpers
- [`KT-61100`](https://youtrack.jetbrains.com/issue/KT-61100) K2: wrong type for "value" parameter of java annotation constructor
- [`KT-62491`](https://youtrack.jetbrains.com/issue/KT-62491) K2. No `'when' expression must be exhaustive` error when Java sealed class inheritors are not listed in `permits` clause
- [`KT-60095`](https://youtrack.jetbrains.com/issue/KT-60095) K2: Introduced INCOMPATIBLE_TYPES
- [`KT-61598`](https://youtrack.jetbrains.com/issue/KT-61598) K2: report IR_WITH_UNSTABLE_ABI_COMPILED_CLASS
- [`KT-59561`](https://youtrack.jetbrains.com/issue/KT-59561) K2/MPP reports INCOMPATIBLE_MATCHING when an actual annotation declaration with vararg property is typealias with `@Suppress`
- [`KT-60123`](https://youtrack.jetbrains.com/issue/KT-60123) K2: PROPERTY_WITH_NO_TYPE_NO_INITIALIZER isn't working in IDE for lateinit property without a type
- [`KT-59935`](https://youtrack.jetbrains.com/issue/KT-59935) K2: Disappeared PROPERTY_WITH_NO_TYPE_NO_INITIALIZER
- [`KT-58455`](https://youtrack.jetbrains.com/issue/KT-58455) K2(LT). Internal compiler error "UninitializedPropertyAccessException: lateinit property identifier has not been initialized" on missing type parameter in "where" constraint
- [`KT-60714`](https://youtrack.jetbrains.com/issue/KT-60714) K2: Implement resolve to private members from Evaluator in K2
- [`KT-59577`](https://youtrack.jetbrains.com/issue/KT-59577) K2. Enum constant name is not specified in error text
- [`KT-60003`](https://youtrack.jetbrains.com/issue/KT-60003) K2: Disappeared INVALID_CHARACTERS_NATIVE_ERROR
- [`KT-62099`](https://youtrack.jetbrains.com/issue/KT-62099) K2: "Type arguments should be specified for an outer class" error about typealias
- [`KT-60983`](https://youtrack.jetbrains.com/issue/KT-60983) K2: "Argument type mismatch: actual type is android/view/View.OnApplyWindowInsetsListener but androidx/core/view/OnApplyWindowInsetsListener? was expected"
- [`KT-60111`](https://youtrack.jetbrains.com/issue/KT-60111) K2: Location regressions for operators
- [`KT-59399`](https://youtrack.jetbrains.com/issue/KT-59399) K2: Missing JSCODE_NO_JAVASCRIPT_PRODUCED
- [`KT-59388`](https://youtrack.jetbrains.com/issue/KT-59388) K2: Missing JSCODE_ERROR
- [`KT-59435`](https://youtrack.jetbrains.com/issue/KT-59435) K2: Missing JSCODE_ARGUMENT_SHOULD_BE_CONSTANT
- [`KT-60601`](https://youtrack.jetbrains.com/issue/KT-60601) K2 / Maven: Overload resolution ambiguity between candidates inline method
- [`KT-60778`](https://youtrack.jetbrains.com/issue/KT-60778) K2: implement MISSING_DEPENDENCY_CLASS(_SUPERCLASS) errors
- [`KT-62581`](https://youtrack.jetbrains.com/issue/KT-62581) K2: Difference in `kind` flag in metadata
- [`KT-59956`](https://youtrack.jetbrains.com/issue/KT-59956) K2: Disappeared INAPPLICABLE_OPERATOR_MODIFIER
- [`KT-35913`](https://youtrack.jetbrains.com/issue/KT-35913) Diagnostic error VAL_REASSIGNMENT is not reported multiple times
- [`KT-60059`](https://youtrack.jetbrains.com/issue/KT-60059) K2: Introduced VAL_REASSIGNMENT
- [`KT-59945`](https://youtrack.jetbrains.com/issue/KT-59945) K2: Disappeared ANONYMOUS_FUNCTION_WITH_NAME
- [`KT-62573`](https://youtrack.jetbrains.com/issue/KT-62573) K2: incorrect parsing behavior with named functions as expressions
- [`KT-56629`](https://youtrack.jetbrains.com/issue/KT-56629) K2: an instance of USELESS_CAST was not moved under EnableDfaWarningsInK2 language feature
- [`KT-58034`](https://youtrack.jetbrains.com/issue/KT-58034) Inconsistent resolve for nested objects in presence of a companion object property with the same name
- [`KT-59864`](https://youtrack.jetbrains.com/issue/KT-59864) K2: Bad locations with delegates
- [`KT-59584`](https://youtrack.jetbrains.com/issue/KT-59584) K2: Bad startOffset for 'this'
- [`KT-61388`](https://youtrack.jetbrains.com/issue/KT-61388) K2: ISE "Annotations are resolved twice" from CompilerRequiredAnnotationsComputationSession on nested annotation
- [`KT-62027`](https://youtrack.jetbrains.com/issue/KT-62027) "java.lang.IndexOutOfBoundsException: Empty list doesn't contain element at index 0" caused by ClassicExpectActualMatchingContext.kt when annotation `@AllowDifferentMembersInActual` used
- [`KT-61055`](https://youtrack.jetbrains.com/issue/KT-61055) K2: Investigate if usage of `toResolvedCallableSymbol` is correct at FirDataFlowAnalyzer#processConditionalContract
- [`KT-61688`](https://youtrack.jetbrains.com/issue/KT-61688) K2: FIR renderings of type annotations leak through the diagnostics' messages
- [`KT-61794`](https://youtrack.jetbrains.com/issue/KT-61794) FIR: MergePostponedLambdaExitsNode.flow remains uninitialized after resolve
- [`KT-61068`](https://youtrack.jetbrains.com/issue/KT-61068) Bounds of type parameters are not enforced during inheritance of inner classes with generic outer classes
- [`KT-61065`](https://youtrack.jetbrains.com/issue/KT-61065) K2: `@Suppress` annotation is ignored inside preconditions of when-clauses
- [`KT-61937`](https://youtrack.jetbrains.com/issue/KT-61937) K2: implicit script receiver from ScriptDefinition are not visible for invoke
- [`KT-61076`](https://youtrack.jetbrains.com/issue/KT-61076) K2: false-positive conflicting overloads error on suspending function and private Java method from a supertype
- [`KT-61075`](https://youtrack.jetbrains.com/issue/KT-61075) K2: type inference for delegate expressions with complexly bounded type variables fails on properties with annotated accessors
- [`KT-58579`](https://youtrack.jetbrains.com/issue/KT-58579) K2: false-positive new inference error on invoking a generic function on Java wildcard type bounded by raw-typed Java inner class
- [`KT-62671`](https://youtrack.jetbrains.com/issue/KT-62671) K2: fir2ir generates a duplicate of delegated function for class from a common module
- [`KT-60682`](https://youtrack.jetbrains.com/issue/KT-60682) K2: Disappeared DEPRECATION
- [`KT-62143`](https://youtrack.jetbrains.com/issue/KT-62143) Error: Identity equality for arguments of types 'kotlin/Int?' and 'kotlin/Nothing?' is prohibited
- [`KT-61517`](https://youtrack.jetbrains.com/issue/KT-61517) K2: FirModuleDescriptor should correctly provide dependencies from FirModuleData
- [`KT-62578`](https://youtrack.jetbrains.com/issue/KT-62578) K2: `@NoInfer` annotation doesn't work for deserialized functions
- [`KT-59916`](https://youtrack.jetbrains.com/issue/KT-59916) K2: Disappeared REPEATED_ANNOTATION
- [`KT-62450`](https://youtrack.jetbrains.com/issue/KT-62450) K2: Disappeared OPT_IN_USAGE_ERROR for a data class property during the destructuring declaration
- [`KT-59997`](https://youtrack.jetbrains.com/issue/KT-59997) K2: Disappeared OPT_IN_USAGE_ERROR
- [`KT-62393`](https://youtrack.jetbrains.com/issue/KT-62393) K2: FIR doesn't count visibility when creating synthetic property override
- [`KT-61208`](https://youtrack.jetbrains.com/issue/KT-61208) EnumEntries mappings are generated incorrectly in the face of incremental compilation
- [`KT-61786`](https://youtrack.jetbrains.com/issue/KT-61786) K2: Remove type enhancement on java final fields
- [`KT-57811`](https://youtrack.jetbrains.com/issue/KT-57811) K2: make java static string and int fields not null
- [`KT-62531`](https://youtrack.jetbrains.com/issue/KT-62531) InvalidProtocolBufferException on reading module metadata compiled by K2 from compilers earlier than 1.8.20 with -Xskip-metadata-version-check
- [`KT-59371`](https://youtrack.jetbrains.com/issue/KT-59371) K2: Missing MISSING_DEPENDENCY_CLASS
- [`KT-61511`](https://youtrack.jetbrains.com/issue/KT-61511) IrFakeOverride builder: objc overridability condition is not supported
- [`KT-62316`](https://youtrack.jetbrains.com/issue/KT-62316) K2: CONFLICTING_INHERITED_JVM_DECLARATIONS on List subclass inheriting remove/removeAt from Java superclass
- [`KT-60671`](https://youtrack.jetbrains.com/issue/KT-60671) KMP: check other annotation targets in expect and actual annotations compatibility checker
- [`KT-62451`](https://youtrack.jetbrains.com/issue/KT-62451) K2: Disappeared OPT_IN_USAGE_ERROR for typealias
- [`KT-62452`](https://youtrack.jetbrains.com/issue/KT-62452) K2: Violation of OPT_IN_USAGE_ERROR non-propagating opt-in rules for typealias
- [`KT-59927`](https://youtrack.jetbrains.com/issue/KT-59927) K2: Disappeared INVISIBLE_REFERENCE
- [`KT-60104`](https://youtrack.jetbrains.com/issue/KT-60104) K2: Introduced FUNCTION_CALL_EXPECTED
- [`KT-57513`](https://youtrack.jetbrains.com/issue/KT-57513) K2: Bound smart casts don't work with Strings
- [`KT-62146`](https://youtrack.jetbrains.com/issue/KT-62146) K2: `@Suppress` does not work with named argument
- [`KT-62475`](https://youtrack.jetbrains.com/issue/KT-62475) K2: IrExternalModuleFragments contains incorrect data in Fir2Ir
- [`KT-61983`](https://youtrack.jetbrains.com/issue/KT-61983) K2: *fir.kt.txt dump uses different naming approach for local vars
- [`KT-59970`](https://youtrack.jetbrains.com/issue/KT-59970) K2: Disappeared NULLABLE_TYPE_IN_CLASS_LITERAL_LHS
- [`KT-58216`](https://youtrack.jetbrains.com/issue/KT-58216) K2 (2.0): when is not checked for exhaustiveness with Java sealed class
- [`KT-62036`](https://youtrack.jetbrains.com/issue/KT-62036) KMP: consider prohibiting `actual fake-override` when the corresponding `expect class` has default arguments
- [`KT-61205`](https://youtrack.jetbrains.com/issue/KT-61205) Compose Compiler K2/ios: No file for /App|App(){}[0] when running linkPodDebugFrameworkIosX64
- [`KT-58240`](https://youtrack.jetbrains.com/issue/KT-58240) Support running irText compiler tests against the Native backend
- [`KT-59565`](https://youtrack.jetbrains.com/issue/KT-59565) K2. Internal error "IndexOutOfBoundsException: Index -1 out of bounds for length 0" on incorrect usage of annotation in type parameter
- [`KT-59393`](https://youtrack.jetbrains.com/issue/KT-59393) K2: Missing TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED
- [`KT-59954`](https://youtrack.jetbrains.com/issue/KT-59954) K2: Disappeared REPEATED_MODIFIER
- [`KT-62127`](https://youtrack.jetbrains.com/issue/KT-62127) "NoSuchFieldError: TRUE$delegate" on referencing companion's variable in submodule
- [`KT-57100`](https://youtrack.jetbrains.com/issue/KT-57100) K2 does not report Conflicting overloads and backend crashes with Exception during IR lowering on conflict overloading with suspend function
- [`KT-62129`](https://youtrack.jetbrains.com/issue/KT-62129) K2: Verification error on calling an extension from an env with 2+ context receivers
- [`KT-59955`](https://youtrack.jetbrains.com/issue/KT-59955) K2: Disappeared INCOMPATIBLE_MODIFIERS
- [`KT-61572`](https://youtrack.jetbrains.com/issue/KT-61572) [K2/N] Missing diagnostic SUPER_CALL_WITH_DEFAULT_PARAMETERS in test for MPP supercall with default params
- [`KT-59514`](https://youtrack.jetbrains.com/issue/KT-59514) K2: New inference error with jspecify and Java interop
- [`KT-62263`](https://youtrack.jetbrains.com/issue/KT-62263) Turn "different expect/actual members" error into a warning
- [`KT-62262`](https://youtrack.jetbrains.com/issue/KT-62262) [K2/N] tests/samples/uikit compilation fails with NPE in checkCanGenerateOverrideInit
- [`KT-52213`](https://youtrack.jetbrains.com/issue/KT-52213) Context receivers: "No mapping for symbol: VALUE_PARAMETER"  caused by contextual suspending function type with receiver
- [`KT-62114`](https://youtrack.jetbrains.com/issue/KT-62114) K2: Unresolved reference for smart cast inside `when` (but not `if`)
- [`KT-59373`](https://youtrack.jetbrains.com/issue/KT-59373) K2: Missing INVISIBLE_MEMBER
- [`KT-61844`](https://youtrack.jetbrains.com/issue/KT-61844) K2: "Expression * of type * cannot be invoked as a function" caused by private property
- [`KT-60581`](https://youtrack.jetbrains.com/issue/KT-60581) K2 fails with New inference error for assertThat under strange circumstances
- [`KT-61735`](https://youtrack.jetbrains.com/issue/KT-61735) [FIR] Assignment to val with flexible type dispatch receiver causes crash
- [`KT-59942`](https://youtrack.jetbrains.com/issue/KT-59942) K2: Disappeared ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT
- [`KT-62058`](https://youtrack.jetbrains.com/issue/KT-62058) K2: use PRE_RELEASE flag until 2.0-RC
- [`KT-59931`](https://youtrack.jetbrains.com/issue/KT-59931) K2: Disappeared CLASS_LITERAL_LHS_NOT_A_CLASS
- [`KT-59377`](https://youtrack.jetbrains.com/issue/KT-59377) K2: Missing CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM
- [`KT-61645`](https://youtrack.jetbrains.com/issue/KT-61645) K2/KMP: Set stdlib-native before stdlib-commonMain in dependencies for shared native metadata compilation
- [`KT-61974`](https://youtrack.jetbrains.com/issue/KT-61974) K2: "ClassCastException: class cannot be cast to class java.lang.Void" in test
- [`KT-61637`](https://youtrack.jetbrains.com/issue/KT-61637) K2: Store all IR declarations inside Fir2IrDeclarationStorage
- [`KT-61924`](https://youtrack.jetbrains.com/issue/KT-61924) Native: problem with abstract fake override from Any
- [`KT-60921`](https://youtrack.jetbrains.com/issue/KT-60921) K2: IndexOutOfBoundsException on attempt to cast an element to inner class with type parameter
- [`KT-61933`](https://youtrack.jetbrains.com/issue/KT-61933) K2: "`Argument type mismatch: actual type is 'Foo<kotlin/Function0<kotlin/Unit>>' but 'Foo<kotlin/coroutines/SuspendFunction0<kotlin/Unit>>' was expected`"
- [`KT-59429`](https://youtrack.jetbrains.com/issue/KT-59429) K2: Missing ABBREVIATED_NOTHING_RETURN_TYPE
- [`KT-59420`](https://youtrack.jetbrains.com/issue/KT-59420) K2: Missing ABBREVIATED_NOTHING_PROPERTY_TYPE
- [`KT-59965`](https://youtrack.jetbrains.com/issue/KT-59965) K2: Disappeared CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON
- [`KT-61732`](https://youtrack.jetbrains.com/issue/KT-61732) K2: Analysis API: resolve ambiguities in kotlin project
- [`KT-60499`](https://youtrack.jetbrains.com/issue/KT-60499) K2: Order of synthetic fields is different from K1's order
- [`KT-61773`](https://youtrack.jetbrains.com/issue/KT-61773) K2 Native: support reporting PRE_RELEASE_CLASS
- [`KT-61578`](https://youtrack.jetbrains.com/issue/KT-61578) [FIR] Resolution to private companion objects does not produce `INVISIBLE_REFERENCE` diagnostic
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
- [`KT-61972`](https://youtrack.jetbrains.com/issue/KT-61972) K2: FIR2IR crashes on converting data classes in MPP setup
- [`KT-60105`](https://youtrack.jetbrains.com/issue/KT-60105) K2: Introduced UNDERSCORE_USAGE_WITHOUT_BACKTICKS
- [`KT-61443`](https://youtrack.jetbrains.com/issue/KT-61443) K2: Return typeId -1 during JS compilation
- [`KT-60075`](https://youtrack.jetbrains.com/issue/KT-60075) K2: Introduced ACTUAL_WITHOUT_EXPECT
- [`KT-61668`](https://youtrack.jetbrains.com/issue/KT-61668) Put expect/actual diagnostics introduced in 1.9.20 release under 1.9 Language Version
- [`KT-61751`](https://youtrack.jetbrains.com/issue/KT-61751) IrFakeOverrideBuilder: keep flexible type annotations when remapping/substituting types
- [`KT-61778`](https://youtrack.jetbrains.com/issue/KT-61778) K2: Overload resolution ambiguity between expect and non-expect in native build
- [`KT-61367`](https://youtrack.jetbrains.com/issue/KT-61367) K2: Introduce OptIn for FirExpression.coneTypeOrNull
- [`KT-61802`](https://youtrack.jetbrains.com/issue/KT-61802) K2: infinite recursion in constant evaluator causing StackOverflowError
- [`KT-60043`](https://youtrack.jetbrains.com/issue/KT-60043) K2: Introduced PROPERTY_AS_OPERATOR
- [`KT-61691`](https://youtrack.jetbrains.com/issue/KT-61691) K2: This annotation is not applicable to target 'local variable'
- [`KT-59915`](https://youtrack.jetbrains.com/issue/KT-59915) K2: Disappeared TOO_MANY_ARGUMENTS
- [`KT-59925`](https://youtrack.jetbrains.com/issue/KT-59925) K2: Disappeared VIRTUAL_MEMBER_HIDDEN
- [`KT-61173`](https://youtrack.jetbrains.com/issue/KT-61173) K2: FirProperty.hasBackingField is true for an expect val
- [`KT-61696`](https://youtrack.jetbrains.com/issue/KT-61696) K2: Cannot override method of interface if superclass has package-protected method with same signature
- [`KT-59370`](https://youtrack.jetbrains.com/issue/KT-59370) K2: Missing JS_NAME_CLASH
- [`KT-36056`](https://youtrack.jetbrains.com/issue/KT-36056) [FIR] Fix implementation of try/catch/finally in DFA
- [`KT-61719`](https://youtrack.jetbrains.com/issue/KT-61719) K2. Invisible reference is shown for whole type reference instead of single name reference
- [`KT-60248`](https://youtrack.jetbrains.com/issue/KT-60248) K2: Type abbreviations are not stored in IR
- [`KT-59251`](https://youtrack.jetbrains.com/issue/KT-59251) KMP/JS: forbid matching actual callable with dynamic return type to expect callable with non-dynamic return type
- [`KT-61510`](https://youtrack.jetbrains.com/issue/KT-61510) K2: internal declarations are invisible in cyclically dependent modules
- [`KT-60048`](https://youtrack.jetbrains.com/issue/KT-60048) K2: Introduced MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND
- [`KT-59425`](https://youtrack.jetbrains.com/issue/KT-59425) K2: Missing JS_FAKE_NAME_CLASH
- [`KT-61060`](https://youtrack.jetbrains.com/issue/KT-61060) K2: Rewrite delegate inference
- [`KT-59529`](https://youtrack.jetbrains.com/issue/KT-59529) K2: "property delegate must have" caused by class hierarchy
- [`KT-55471`](https://youtrack.jetbrains.com/issue/KT-55471) K2. Unresolved reference for nested type is shown instead of outer class
- [`KT-58896`](https://youtrack.jetbrains.com/issue/KT-58896) K2: Higher priority expect overload candidates in common code lose in overload resolution to non-expects
- [`KT-58476`](https://youtrack.jetbrains.com/issue/KT-58476) Context receivers: "No mapping for symbol: VALUE_PARAMETER" with context-receiver inside suspended lambda calling another suspended function
- [`KT-60780`](https://youtrack.jetbrains.com/issue/KT-60780) K2: missing PRE_RELEASE_CLASS
- [`KT-59855`](https://youtrack.jetbrains.com/issue/KT-59855) K2: Replace FirExpression.typeRef with coneType
- [`KT-59391`](https://youtrack.jetbrains.com/issue/KT-59391) K2: Missing JS_BUILTIN_NAME_CLASH
- [`KT-59392`](https://youtrack.jetbrains.com/issue/KT-59392) K2: Missing NAME_CONTAINS_ILLEGAL_CHARS
- [`KT-59110`](https://youtrack.jetbrains.com/issue/KT-59110) K2. "NotImplementedError: An operation is not implemented." error on incorrect `@Target` annotation
- [`KT-53565`](https://youtrack.jetbrains.com/issue/KT-53565) K2: no WRONG_ANNOTATION_TARGET on when subject
- [`KT-54568`](https://youtrack.jetbrains.com/issue/KT-54568) K2: Type variables leak into implicit `it` parameter of lambdas
- [`KT-60892`](https://youtrack.jetbrains.com/issue/KT-60892) K2: Implement diagnostics around `@OptionalExpectation`
- [`KT-61029`](https://youtrack.jetbrains.com/issue/KT-61029) K2: Duplicates when processing direct overridden callables
- [`KT-60917`](https://youtrack.jetbrains.com/issue/KT-60917) K2: "Unresolved reference" for operator for array value
- [`KT-59367`](https://youtrack.jetbrains.com/issue/KT-59367) K2: Missing MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES
- [`KT-60268`](https://youtrack.jetbrains.com/issue/KT-60268) K2: lazy annotation classes have wrong modality
- [`KT-61129`](https://youtrack.jetbrains.com/issue/KT-61129) K2: Implement error suppression warning
- [`KT-60536`](https://youtrack.jetbrains.com/issue/KT-60536) K2: FIR2IR Crash when resolving to companion of internal class with Suppress("INVISIBLE_REFERENCE")
- [`KT-55196`](https://youtrack.jetbrains.com/issue/KT-55196) K2: False-negative CONST_VAL_WITH_NON_CONST_INITIALIZER on boolean .not() call
- [`KT-60292`](https://youtrack.jetbrains.com/issue/KT-60292) K2: annotations on local delegated properties are lost
- [`KT-59418`](https://youtrack.jetbrains.com/issue/KT-59418) K2: Missing DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE
- [`KT-59422`](https://youtrack.jetbrains.com/issue/KT-59422) K2: Missing NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION
- [`KT-57963`](https://youtrack.jetbrains.com/issue/KT-57963) K2: MPP: Annotation calls should be actualized
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
- [`KT-60531`](https://youtrack.jetbrains.com/issue/KT-60531) K2/JS: Report diagnostics before running FIR2IR
- [`KT-59900`](https://youtrack.jetbrains.com/issue/KT-59900) K2: Disappeared NESTED_CLASS_NOT_ALLOWED
- [`KT-59344`](https://youtrack.jetbrains.com/issue/KT-59344) K2: implement deprecation warnings from KT-53153
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
- [`KT-61249`](https://youtrack.jetbrains.com/issue/KT-61249) Move fir-related code from backend.native module
- [`KT-59478`](https://youtrack.jetbrains.com/issue/KT-59478) K2: StackOverflowError on invalid code with nullable unresolved
- [`KT-59893`](https://youtrack.jetbrains.com/issue/KT-59893) K2: Disappeared WRONG_NUMBER_OF_TYPE_ARGUMENTS
- [`KT-60450`](https://youtrack.jetbrains.com/issue/KT-60450) K2: IOOBE from analyzeAndGetLambdaReturnArguments
- [`KT-61442`](https://youtrack.jetbrains.com/issue/KT-61442) K2: Consider stricter filtering on implicit integer coercion
- [`KT-61441`](https://youtrack.jetbrains.com/issue/KT-61441) K2: Wrong overload is chosen with ImplicitIntegerCoercion enabled
- [`KT-57076`](https://youtrack.jetbrains.com/issue/KT-57076) K2 does not report 'More than one overridden descriptor declares a default value'
- [`KT-55672`](https://youtrack.jetbrains.com/issue/KT-55672) K2. Operator name message instead of "Unresolved reference" when operator isn't defined for type
- [`KT-60252`](https://youtrack.jetbrains.com/issue/KT-60252) K2: Supertype argument is not substituted in fake override receivers and value parameters
- [`KT-60687`](https://youtrack.jetbrains.com/issue/KT-60687) K2: Introduced UNEXPECTED_SAFE_CALL
- [`KT-59664`](https://youtrack.jetbrains.com/issue/KT-59664) Inline modifier can be added to a constructor parameter, but it does not have any effect
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
- [`KT-57738`](https://youtrack.jetbrains.com/issue/KT-57738) K2: unresolved class fields and methods in kotlin scripts
- [`KT-60885`](https://youtrack.jetbrains.com/issue/KT-60885) K2: Fix `testSelfUpperBoundInference` test in LV 2.0 branch
- [`KT-59957`](https://youtrack.jetbrains.com/issue/KT-59957) K2: Missing UNSUPPORTED_SEALED_FUN_INTERFACE
- [`KT-60000`](https://youtrack.jetbrains.com/issue/KT-60000) K2: Missing UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION
- [`KT-60886`](https://youtrack.jetbrains.com/issue/KT-60886) K2: Fix `testDirectoryWithRelativePath` in LV 2.0 branch
- [`KT-60002`](https://youtrack.jetbrains.com/issue/KT-60002) K2: Missing UNSUPPORTED_SUSPEND_TEST
- [`KT-59419`](https://youtrack.jetbrains.com/issue/KT-59419) K2: Missing MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE
- [`KT-60297`](https://youtrack.jetbrains.com/issue/KT-60297) K2: finally block is not coerced to unit
- [`KT-59416`](https://youtrack.jetbrains.com/issue/KT-59416) K2: Missing EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT
- [`KT-59417`](https://youtrack.jetbrains.com/issue/KT-59417) K2: Missing CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE
- [`KT-59381`](https://youtrack.jetbrains.com/issue/KT-59381) K2: Missing CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM
- [`KT-59384`](https://youtrack.jetbrains.com/issue/KT-59384) K2: Missing DYNAMIC_NOT_ALLOWED
- [`KT-59406`](https://youtrack.jetbrains.com/issue/KT-59406) K2: Missing PROPERTY_DELEGATION_BY_DYNAMIC
- [`KT-57223`](https://youtrack.jetbrains.com/issue/KT-57223) K2: false-negative INAPPLICABLE_JVM_NAME on non-final properties outside interfaces
- [`KT-59413`](https://youtrack.jetbrains.com/issue/KT-59413) K2: Missing VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS
- [`KT-59387`](https://youtrack.jetbrains.com/issue/KT-59387) K2: Missing NO_CONSTRUCTOR
- [`KT-57803`](https://youtrack.jetbrains.com/issue/KT-57803) K2. "Kotlin: Only the Kotlin standard library is allowed to use the 'kotlin' package" error missing in 2.0

### IDE

#### Fixes

- [`KT-62777`](https://youtrack.jetbrains.com/issue/KT-62777) K2 IDE: False positive MISSING_DEPENDENCY_SUPERCLASS for built-ins in non-JVM modules
- [`KT-61195`](https://youtrack.jetbrains.com/issue/KT-61195) UAST modeling of implicit `it` is inconsistent for `Enum.entries`
- [`KT-62757`](https://youtrack.jetbrains.com/issue/KT-62757) SLC: incorrect nullability annotation on aliased type
- [`KT-60318`](https://youtrack.jetbrains.com/issue/KT-60318) K2: disable SLC for non-JVM platforms
- [`KT-62440`](https://youtrack.jetbrains.com/issue/KT-62440) On the fly resolve with light method context doesn't resolve method type parameters
- [`KT-50241`](https://youtrack.jetbrains.com/issue/KT-50241) Make Symbol Light Classes consistent with Ultra Light Classes
- [`KT-56546`](https://youtrack.jetbrains.com/issue/KT-56546) LL FIR: fix lazy resolve contract violation in Symbol Light Classes
- [`KT-57550`](https://youtrack.jetbrains.com/issue/KT-57550) K2: AA: incorrect constant value in file-level annotation
- [`KT-61460`](https://youtrack.jetbrains.com/issue/KT-61460) SLC: unnecessary upper bound wildcards (w/ type alias)
- [`KT-61377`](https://youtrack.jetbrains.com/issue/KT-61377) K2: SLC: wrong retention counterpart for AnnotationRetention.BINARY
- [`KT-60603`](https://youtrack.jetbrains.com/issue/KT-60603) K2: Investigate intellij tests failures in branch 2.0
- [`KT-60590`](https://youtrack.jetbrains.com/issue/KT-60590) Fix light classes related tests in branch 2.0

### IDE. Gradle Integration

- [`KT-45775`](https://youtrack.jetbrains.com/issue/KT-45775) Improve quality of Import

### IDE. Multiplatform

- [`KT-63007`](https://youtrack.jetbrains.com/issue/KT-63007) K2: Analysis API Standalone: klibs are not resovled from common code
- [`KT-61520`](https://youtrack.jetbrains.com/issue/KT-61520) Sources.jar is not imported for common and intermediate source-sets from the MPP library

### IDE. Script

- [`KT-60418`](https://youtrack.jetbrains.com/issue/KT-60418) K2 scripting: highlighting sometimes fails
- [`KT-60987`](https://youtrack.jetbrains.com/issue/KT-60987) K2: Analysis API: make build.gradle.kts resolution work on build scripts from kotlin projects

### IR. Actualizer

- [`KT-62623`](https://youtrack.jetbrains.com/issue/KT-62623) K2: Ir actualizer leaves inconsistent module links from files

### IR. Tree

- [`KT-61934`](https://youtrack.jetbrains.com/issue/KT-61934) Decouple building fake overrides from symbol table and build scheduling
- [`KT-60923`](https://youtrack.jetbrains.com/issue/KT-60923) IR: Mark IrSymbol.owner with OptIn

### JavaScript

- [`KT-61795`](https://youtrack.jetbrains.com/issue/KT-61795) KJS: Incremental Cache is not invalidated if `useEsClasses` compiler argument was changed
- [`KT-62425`](https://youtrack.jetbrains.com/issue/KT-62425) K/JS: Implement K2 and K1 diagnostics for checking argument passing to js()
- [`KT-58685`](https://youtrack.jetbrains.com/issue/KT-58685) KJS: "IllegalStateException: Not locked" cused by "unlock" called twice
- [`KT-56818`](https://youtrack.jetbrains.com/issue/KT-56818) KJS: "TypeError: Class constructor * cannot be invoked without 'new'" when extending external class
- [`KT-61710`](https://youtrack.jetbrains.com/issue/KT-61710) K/JS: Implement JS_NAME_CLASH check for top level declarations
- [`KT-61886`](https://youtrack.jetbrains.com/issue/KT-61886) K/JS: Prepare K/JS tests for JS IR BE diagnostics
- [`KT-60829`](https://youtrack.jetbrains.com/issue/KT-60829) Fix JS Incremental tests in 2.0 branch
- [`KT-60785`](https://youtrack.jetbrains.com/issue/KT-60785) KJS: Destructured value class in suspend function fails with Uncaught TypeError: can't convert to primitive type error
- [`KT-60635`](https://youtrack.jetbrains.com/issue/KT-60635) K/JS: Class internal methods may clash with child methods from other module that have the same name
- [`KT-60846`](https://youtrack.jetbrains.com/issue/KT-60846) Fix `IncrementalJsKlibCompilerWithScopeExpansionRunnerTestGenerated` test in 2.0 branch

### KMM Plugin

- [`KT-60169`](https://youtrack.jetbrains.com/issue/KT-60169) Generate gradle version catalog in KMM AS plugin
- [`KT-59269`](https://youtrack.jetbrains.com/issue/KT-59269) Update wizards in KMM AS plugin after 1.9.0 release

### Klibs

- [`KT-61767`](https://youtrack.jetbrains.com/issue/KT-61767) [K/N] Header klibs should keep private underlying properties of value classes
- [`KT-60807`](https://youtrack.jetbrains.com/issue/KT-60807) [klib] Add an option to write out header klibs
- [`KT-61097`](https://youtrack.jetbrains.com/issue/KT-61097) [PL] Don't create an executable if there were errors in PL

### Language Design

- [`KT-58921`](https://youtrack.jetbrains.com/issue/KT-58921) K1/K2: difference in Enum.values resolve priority
- [`KT-61573`](https://youtrack.jetbrains.com/issue/KT-61573) Emit the compilation warning on expect/actual classes. The warning must mention that expect/actual classes are in Beta
- [`KT-62138`](https://youtrack.jetbrains.com/issue/KT-62138) K1: false positive (?) NO_SET_METHOD for += resolved as a combination of Map.get and plus
- [`KT-22841`](https://youtrack.jetbrains.com/issue/KT-22841) Prohibit different member scopes for non-final expect and its actual

### Libraries

- [`KT-62785`](https://youtrack.jetbrains.com/issue/KT-62785) Drop unnecessary suppresses in stdlib after bootstrap update
- [`KT-58588`](https://youtrack.jetbrains.com/issue/KT-58588) Optimizations for sequence functions distinct, flatten
- [`KT-62004`](https://youtrack.jetbrains.com/issue/KT-62004) Drop legacy JS compilations of stdlib and kotlin-test
- [`KT-61614`](https://youtrack.jetbrains.com/issue/KT-61614) WASM: Enum hashCode is not final

### Native

- [`KT-61642`](https://youtrack.jetbrains.com/issue/KT-61642) [K/N] Serialize full IdSignatures to caches
- [`KT-62803`](https://youtrack.jetbrains.com/issue/KT-62803) Konanc has print statement "Produced library API in..." that should be deleted or properly logged at INFO level
- [`KT-61248`](https://youtrack.jetbrains.com/issue/KT-61248) [K/N] Extract native manglers out of `backend.native` module

### Native. Runtime. Memory

- [`KT-57773`](https://youtrack.jetbrains.com/issue/KT-57773) Kotlin/Native: track memory in big chunks in the GC scheduler
- [`KT-61093`](https://youtrack.jetbrains.com/issue/KT-61093) Kotlin/Native: enable concurrent weak processing by default

### Native. Stdlib

- [`KT-60514`](https://youtrack.jetbrains.com/issue/KT-60514) Add llvm filecheck tests for atomic intrinsics

### Native. Testing

- [`KT-62157`](https://youtrack.jetbrains.com/issue/KT-62157) Native: Migrate FileCheck tests to new native test infra

### Reflection

- [`KT-60984`](https://youtrack.jetbrains.com/issue/KT-60984) K2: java.lang.ClassNotFoundException: kotlin.Array in runtime with Spring Boot test
- [`KT-60709`](https://youtrack.jetbrains.com/issue/KT-60709) Reflection: Not recognized bound receiver in case of 'equals' always returning true
- [`KT-61304`](https://youtrack.jetbrains.com/issue/KT-61304) Reflection: Calling data class `copy` method via reflection (callBy) fails when the data class has exactly 64 fields

### Tools. CLI

- [`KT-62644`](https://youtrack.jetbrains.com/issue/KT-62644) Don't enable in progressive mode bug-fix features without target version
- [`KT-62350`](https://youtrack.jetbrains.com/issue/KT-62350) CLI: no color output on Apple silicon Macs
- [`KT-61156`](https://youtrack.jetbrains.com/issue/KT-61156) K2: do not try to run compilation if there were errors during calculation of Java module graph
- [`KT-48026`](https://youtrack.jetbrains.com/issue/KT-48026) Add the compiler X-flag to enable self upper bound type inference

### Tools. Compiler Plugin API

- [`KT-61872`](https://youtrack.jetbrains.com/issue/KT-61872) K2: Adding annotations to metadata from backend plugin doesn't work in the presence of comments on annotated declaration

### Tools. Compiler Plugins

- [`KT-60849`](https://youtrack.jetbrains.com/issue/KT-60849) jvm-abi-gen: do not treat hasConstant property flag as a part of ABI for non-const properties
- [`KT-53926`](https://youtrack.jetbrains.com/issue/KT-53926) K2. Don't check serializable properties from supertypes

### Tools. Compiler plugins. Serialization

- [`KT-62215`](https://youtrack.jetbrains.com/issue/KT-62215) Serialization / Native: "IllegalArgumentException: No container found for type parameter" caused by serializing generic classes with a field that uses generics
- [`KT-62522`](https://youtrack.jetbrains.com/issue/KT-62522) K2 + kotlinx.serialization + Native: NPE when generic base class has inheritor in other module

### Tools. Gradle

#### New Features

- [`KT-59627`](https://youtrack.jetbrains.com/issue/KT-59627) FUS base plugin
- [`KT-62025`](https://youtrack.jetbrains.com/issue/KT-62025) K/Wasm: Support binaryen for wasi

#### Performance Improvements

- [`KT-62318`](https://youtrack.jetbrains.com/issue/KT-62318) Android Studio sync memory leak in 1.9.20-Beta

#### Fixes

- [`KT-62650`](https://youtrack.jetbrains.com/issue/KT-62650) Gradle: Return the usage of `kotlin-compiler-embeddable` back
- [`KT-61295`](https://youtrack.jetbrains.com/issue/KT-61295) `KotlinTestReport` captures `Project.buildDir` too early
- [`KT-62987`](https://youtrack.jetbrains.com/issue/KT-62987) Add tests for statistics plugin in Aggregate build
- [`KT-62964`](https://youtrack.jetbrains.com/issue/KT-62964) Build Gradle plugin against Gradle 8.4 API
- [`KT-61896`](https://youtrack.jetbrains.com/issue/KT-61896) Gradle: compilation via build tools API doesn't perform Gradle side output backups
- [`KT-61918`](https://youtrack.jetbrains.com/issue/KT-61918) Removal of an associated compilation from a build script doesn't lead to full recompilation
- [`KT-59826`](https://youtrack.jetbrains.com/issue/KT-59826) Update SimpleKotlinGradleIT#testProjectIsolation to run on Gradle 8
- [`KT-61401`](https://youtrack.jetbrains.com/issue/KT-61401) The reported language version value for KotlinNativeLink tasks in build reports and build scans is inaccurate
- [`KT-62024`](https://youtrack.jetbrains.com/issue/KT-62024) K/Wasm: Binaryen modifying compiler output
- [`KT-61950`](https://youtrack.jetbrains.com/issue/KT-61950) K/Wasm: Add warning about changed sourceSets
- [`KT-61895`](https://youtrack.jetbrains.com/issue/KT-61895) KotlinTopLevelExtension.useCompilerVersion is not marked as experimental
- [`KT-56574`](https://youtrack.jetbrains.com/issue/KT-56574) Implement a prototype of Kotlin JVM compilation pipeline via the build tools API
- [`KT-61206`](https://youtrack.jetbrains.com/issue/KT-61206) Build system classes may leak into the Build Tools API classloader
- [`KT-61737`](https://youtrack.jetbrains.com/issue/KT-61737) GradleStyleMessageRenderer.render misses a space between the file and the message when `location` is (line:column = 0:0)
- [`KT-61457`](https://youtrack.jetbrains.com/issue/KT-61457) Kotlin Gradle Plugin should not use internal deprecated StartParameterInternal.isConfigurationCache

### Tools. Gradle. JS

- [`KT-41382`](https://youtrack.jetbrains.com/issue/KT-41382) NI / KJS / Gradle: TYPE_MISMATCH caused by compilations.getting delegate
- [`KT-53077`](https://youtrack.jetbrains.com/issue/KT-53077) KJS / Gradle: Remove redundant gradle js log on kotlin build
- [`KT-61992`](https://youtrack.jetbrains.com/issue/KT-61992) KJS / Gradle: KotlinJsTest using KotlinMocha should not show output, and should not run a dry-run every time.
- [`KT-56300`](https://youtrack.jetbrains.com/issue/KT-56300) KJS / Gradle: plugin should not add repositories unconditionally
- [`KT-55620`](https://youtrack.jetbrains.com/issue/KT-55620) KJS / Gradle: plugin doesn't support repositoriesMode
- [`KT-56465`](https://youtrack.jetbrains.com/issue/KT-56465) MPP: Import with npm dependency fails with "UninitializedPropertyAccessException: lateinit property fileHasher has not been initialized" if there is no selected JavaScript environment for JS target
- [`KT-41578`](https://youtrack.jetbrains.com/issue/KT-41578) Kotlin/JS: contiuous mode: changes in static resources do not reload browser page

### Tools. Gradle. Kapt

- [`KT-22261`](https://youtrack.jetbrains.com/issue/KT-22261) Annotation Processor - in gradle, kapt configuration is missing extendsFrom
- [`KT-62518`](https://youtrack.jetbrains.com/issue/KT-62518) kapt processing is skipped when all annotation processors are indirect dependencies

### Tools. Gradle. Multiplatform

- [`KT-62601`](https://youtrack.jetbrains.com/issue/KT-62601) AS/IntelliJ exception after updating a KMP project with a macos target to Kotlin 1.9.20-RC
- [`KT-60734`](https://youtrack.jetbrains.com/issue/KT-60734) Handle the migration from ios shortcut and source set with `getting`
- [`KT-59042`](https://youtrack.jetbrains.com/issue/KT-59042) "Cannot build 'KotlinProjectStructureMetadata' during project configuration phase" when configuration cache enabled
- [`KT-62029`](https://youtrack.jetbrains.com/issue/KT-62029) Kotlin 1.9.20-Beta fails to detect some transitive dependency references in JVM+Android source set
- [`KT-61652`](https://youtrack.jetbrains.com/issue/KT-61652) MPP ConcurrentModificationException on transformCommonMainDependenciesMetadata
- [`KT-61622`](https://youtrack.jetbrains.com/issue/KT-61622) Upgrading to Kotlin 1.9 prevents commonMain sourceset classes from being processed by kapt/ksp (dagger/Hilt)
- [`KT-61540`](https://youtrack.jetbrains.com/issue/KT-61540) K2: KMP/K2: Metadata compilations: Discriminate expect over actual by sorting compile path using refines edges
- [`KT-59020`](https://youtrack.jetbrains.com/issue/KT-59020) 1.9.0 Beta Kotlin plugin Gradle sync fails with intermediate JVM + Android source set
- [`KT-60860`](https://youtrack.jetbrains.com/issue/KT-60860) K2: Fix `KotlinNativeCompileArgumentsTest` in 2.0 branch
- [`KT-61463`](https://youtrack.jetbrains.com/issue/KT-61463) KMP: Remove unused 'kpm' code

### Tools. Gradle. Native

- [`KT-51553`](https://youtrack.jetbrains.com/issue/KT-51553) Migrate all Kotlin Gradle plugin/Native tests to new test DSL and add CI configuration to run them
- [`KT-61657`](https://youtrack.jetbrains.com/issue/KT-61657) KonanTarget should implement equals or custom serialization
- [`KT-62907`](https://youtrack.jetbrains.com/issue/KT-62907) Turn on downloading Kotlin Native from maven by default
- [`KT-61700`](https://youtrack.jetbrains.com/issue/KT-61700) Native: linkDebugExecutableNative has duplicated freeCompilerArgs

### Tools. Incremental Compile

- [`KT-61865`](https://youtrack.jetbrains.com/issue/KT-61865) Add support for incremental compilation within the in-process execution strategy in the build tools api
- [`KT-61590`](https://youtrack.jetbrains.com/issue/KT-61590) K2/KMP: Expect actual matching is breaking on the incremental compilation
- [`KT-60831`](https://youtrack.jetbrains.com/issue/KT-60831) Fix IncrementalMultiplatformJvmCompilerRunnerTestGenerated in 2.0 branch

### Tools. JPS

- [`KT-60737`](https://youtrack.jetbrains.com/issue/KT-60737) Investigate/fix JPS-related tests in 2.0 migration branch

### Tools. Kapt

- [`KT-60507`](https://youtrack.jetbrains.com/issue/KT-60507) Kapt: "IllegalAccessError: superclass access check failed" using java 21 toolchain
- [`KT-62438`](https://youtrack.jetbrains.com/issue/KT-62438) Change experimental K2 kapt diagnostic message
- [`KT-61916`](https://youtrack.jetbrains.com/issue/KT-61916) K2 KAPT. Kapt doesn't generate fully qualified names for annotations used as arguments to other annotations
- [`KT-61879`](https://youtrack.jetbrains.com/issue/KT-61879) K2 Kapt: java.lang.NoSuchMethodError during stub generation
- [`KT-61729`](https://youtrack.jetbrains.com/issue/KT-61729) K2: KAPT 4: Compiler crash during compilation of Sphinx for Android
- [`KT-61333`](https://youtrack.jetbrains.com/issue/KT-61333) K2 Kapt: support REPORT_OUTPUT_FILES compiler mode
- [`KT-61761`](https://youtrack.jetbrains.com/issue/KT-61761) Kapt4ToolIntegrationTestGenerated should not use Kapt3ComponentRegistrar

### Tools. Maven

- [`KT-54868`](https://youtrack.jetbrains.com/issue/KT-54868) Stop publishing `kotlin-archetype-js`
- [`KT-26156`](https://youtrack.jetbrains.com/issue/KT-26156) Maven Kotlin Plugin should not WARN when no sources found
- [`KT-60859`](https://youtrack.jetbrains.com/issue/KT-60859) K2: Fix maven `IncrementalCompilationIT` tests in 2.0 branch

### Tools. Parcelize

- [`KT-57685`](https://youtrack.jetbrains.com/issue/KT-57685) Support ImmutableCollections in Parcelize plugin

### Tools. Scripts

- [`KT-62400`](https://youtrack.jetbrains.com/issue/KT-62400) K2: Missing annotation resolving for scripts
- [`KT-61727`](https://youtrack.jetbrains.com/issue/KT-61727) Scripts: Maven artifacts resolution is slow

### Tools. Wasm

- [`KT-61973`](https://youtrack.jetbrains.com/issue/KT-61973) K/Wasm: wasmWasiNodeRun is missed
- [`KT-61971`](https://youtrack.jetbrains.com/issue/KT-61971) K/Wasm: wasmWasiTest should depends on kotlinNodeJsSetup


## Recent ChangeLogs:
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