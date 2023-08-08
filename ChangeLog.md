## 1.9.0

### Analysis API

#### New Features

- [`KT-57930`](https://youtrack.jetbrains.com/issue/KT-57930) Analysis API: provide an API for extending Kotlin resolution
- [`KT-57636`](https://youtrack.jetbrains.com/issue/KT-57636) K2: Add the return type of K2 reference shortener AA `ShortenCommand::invokeShortening()` e.g., `ShorteningResultInfo` to allow callers to access the shortening result PSI

#### Fixes

- [`KT-58249`](https://youtrack.jetbrains.com/issue/KT-58249) Analysis API: Disable error logging for FE10 implementation of resolveCall when resolve is not successful
- [`KT-55626`](https://youtrack.jetbrains.com/issue/KT-55626) Impossible to restore symbol by psi from script file
- [`KT-57314`](https://youtrack.jetbrains.com/issue/KT-57314) LL FIR: Combine `LLFirProvider$SymbolProvider`s in session dependencies (optimization)
- [`KT-55527`](https://youtrack.jetbrains.com/issue/KT-55527) K2 IDE: Rewrite KtScopeContext class to allow to handle each scope separately
- [`KT-55329`](https://youtrack.jetbrains.com/issue/KT-55329) LL FIR: Unexpected ACTUAL_WITHOUT_EXPECT error on constructor and function declaration
- [`KT-50732`](https://youtrack.jetbrains.com/issue/KT-50732) LL API: fix compiler based tests
- [`KT-57850`](https://youtrack.jetbrains.com/issue/KT-57850) K2: contract violation due to SymbolLightAccessorMethod.propertyAccessorSymbol
- [`KT-56543`](https://youtrack.jetbrains.com/issue/KT-56543) LL FIR: rework lazy transformers so transformers modify only declarations they suppose to
- [`KT-56721`](https://youtrack.jetbrains.com/issue/KT-56721) K2: FirExtensionDeclarationsSymbolProvider: java.lang.IllegalStateException: Recursive update
- [`KT-50253`](https://youtrack.jetbrains.com/issue/KT-50253) Analysis API: Solve issues with ProcessCancelledException
- [`KT-56800`](https://youtrack.jetbrains.com/issue/KT-56800) K2 IDE: optimize deprecation calculation for symbols
- [`KT-55006`](https://youtrack.jetbrains.com/issue/KT-55006) Analysis API does not transform Java type refs for callable symbol return types
- [`KT-57256`](https://youtrack.jetbrains.com/issue/KT-57256) AA FIR: Reduce lazy resolve phase for deprecation status
- [`KT-57619`](https://youtrack.jetbrains.com/issue/KT-57619) K2: CFG for class initializer is not correctly built in reversed resolve mode
- [`KT-58141`](https://youtrack.jetbrains.com/issue/KT-58141) K2: AA FIR: impossible to restore symbol for declaration with annotation with argument inside type
- [`KT-57462`](https://youtrack.jetbrains.com/issue/KT-57462) Symbol Light Classes: SymbolLightFieldForProperty should retrieve annotations not from KtPropertySymbol, but from the corresponding backing field
- [`KT-54864`](https://youtrack.jetbrains.com/issue/KT-54864) Analysis API: add function to get expect KtSymbol list by actual KtSymbol
- [`KT-56763`](https://youtrack.jetbrains.com/issue/KT-56763) Analysis API: `.KtSourceModuleImpl is missing in the map.` on symbol restore when symbol cannot be seen from the use-site module
- [`KT-56617`](https://youtrack.jetbrains.com/issue/KT-56617) Analysis API: optimize KtFirSymbolProviderByJavaPsi.getNamedClassSymbol
- [`KT-54430`](https://youtrack.jetbrains.com/issue/KT-54430) K2: .getAllOverriddenSymbols() returns invalid results

### Backend. Native. Debug

- [`KT-55440`](https://youtrack.jetbrains.com/issue/KT-55440) Kotlin/Native debugger: inline function parameters are not visible during debugging

### Backend. Wasm

- [`KT-58293`](https://youtrack.jetbrains.com/issue/KT-58293) Wasm: ReferenceError: e is not defined in kotlin.test.jsThrow
- [`KT-58931`](https://youtrack.jetbrains.com/issue/KT-58931) Wasm tests are failing to start on Kotlin 1.9.0-Beta
- [`KT-58188`](https://youtrack.jetbrains.com/issue/KT-58188) Restore binary compatibility of PlatformDiagnosticSuppressor.shouldReportUnusedParameter
- [`KT-57136`](https://youtrack.jetbrains.com/issue/KT-57136) K/Wasm: Restrict non-external types in JS interop
- [`KT-57060`](https://youtrack.jetbrains.com/issue/KT-57060) Clarify the lack of support for dynamic in Kotlin/Wasm
- [`KT-56955`](https://youtrack.jetbrains.com/issue/KT-56955) K/Wasm: Support restricted version of K/JS `js(code)`
- [`KT-57276`](https://youtrack.jetbrains.com/issue/KT-57276) Wasm: "Body not found for function" error when compiling konform library with Kotlin/Wasm support
- [`KT-56976`](https://youtrack.jetbrains.com/issue/KT-56976) K/Wasm bug with calling override of external function with default parameters

### Compiler

#### New Features

- [`KT-55333`](https://youtrack.jetbrains.com/issue/KT-55333) Allow secondary constructors in value classes with bodies
- [`KT-54944`](https://youtrack.jetbrains.com/issue/KT-54944) `@Volatile` support in native
- [`KT-54746`](https://youtrack.jetbrains.com/issue/KT-54746) Deprecate with ERROR JvmDefault annotation and old -Xjvm-default modes
- [`KT-47902`](https://youtrack.jetbrains.com/issue/KT-47902) Do not propagate method deprecation through overrides
- [`KT-29378`](https://youtrack.jetbrains.com/issue/KT-29378) K2: rework warnings/errors for equality/identity operators on incompatible types
- [`KT-57477`](https://youtrack.jetbrains.com/issue/KT-57477) False-positive overload resolution ambiguity in case of lambda without arguments
- [`KT-57010`](https://youtrack.jetbrains.com/issue/KT-57010) Kotlin/Native: make it possible to compile bitcode in a separate compiler invocation
- [`KT-55691`](https://youtrack.jetbrains.com/issue/KT-55691) K2: Avoid inferring Nothing? in presence of other constraints (beside type parameter bounds)
- [`KT-46288`](https://youtrack.jetbrains.com/issue/KT-46288) Unexpected behavior of extension function on lambda with suspend receiver
- [`KT-24779`](https://youtrack.jetbrains.com/issue/KT-24779) Inconsistent smart cast behavior for bound data flow values

#### Performance Improvements

- [`KT-23397`](https://youtrack.jetbrains.com/issue/KT-23397) Optimize out field for property delegate when it's safe (JVM)
- [`KT-56906`](https://youtrack.jetbrains.com/issue/KT-56906) FIR: Use cached instance of FirImplicitTypeRefImpl in FIR builders
- [`KT-56276`](https://youtrack.jetbrains.com/issue/KT-56276) LanguageVersion.getVersionString() allocates 5k objects on project opening

#### Fixes

- [`KT-57784`](https://youtrack.jetbrains.com/issue/KT-57784) "NullPointerException: Parameter specified as non-null is null:" with enum, companion object, 'entries' and map
- [`KT-55217`](https://youtrack.jetbrains.com/issue/KT-55217) K2: support callable reference conversions on top-level expressions
- [`KT-57232`](https://youtrack.jetbrains.com/issue/KT-57232) K2: build Space JVM (master)
- [`KT-59079`](https://youtrack.jetbrains.com/issue/KT-59079) "AE: SyntheticAccessorLowering should not attempt to modify other files!" with callable reference to constructor with value class parameter
- [`KT-58837`](https://youtrack.jetbrains.com/issue/KT-58837) Partial linkage fails to report any compiler message on Windows when launched through Gradle plugin
- [`KT-57602`](https://youtrack.jetbrains.com/issue/KT-57602) K2: Rework member scope of types having projection arguments for covariant parameters
- [`KT-55171`](https://youtrack.jetbrains.com/issue/KT-55171) Put new contracts syntax under a feature flag
- [`KT-58719`](https://youtrack.jetbrains.com/issue/KT-58719) K2: false-positive INVISIBLE_REFERENCE error in case of importing an internal abstract class
- [`KT-56030`](https://youtrack.jetbrains.com/issue/KT-56030) [K2/N] Support Objective-C overloading by param names only
- [`KT-57510`](https://youtrack.jetbrains.com/issue/KT-57510) K2: Data class equals/hashCode/toString methods are not written to Klib metadata
- [`KT-56331`](https://youtrack.jetbrains.com/issue/KT-56331) K2: compiler backend crash on usage of expected function with default arguments
- [`KT-53846`](https://youtrack.jetbrains.com/issue/KT-53846) K2 / Context receivers: ClassCastException on secondary constructor of class with context receiver
- [`KT-58621`](https://youtrack.jetbrains.com/issue/KT-58621) K2: Private class shadows public function defined in the same package
- [`KT-59102`](https://youtrack.jetbrains.com/issue/KT-59102) K2: constant evaluator does not provide Long type on shl
- [`KT-59066`](https://youtrack.jetbrains.com/issue/KT-59066) [K2] delegation leads to "IllegalStateException: Expected some types"
- [`KT-56074`](https://youtrack.jetbrains.com/issue/KT-56074) K2: build Space JVM (snapshot 2022.3)
- [`KT-58787`](https://youtrack.jetbrains.com/issue/KT-58787) KAPT: "NullPointerException: null cannot be cast to non-null type" with delegate
- [`KT-57022`](https://youtrack.jetbrains.com/issue/KT-57022) K2 IllegalStateException in signature computation
- [`KT-56792`](https://youtrack.jetbrains.com/issue/KT-56792) K2: build kotlinpoet
- [`KT-57373`](https://youtrack.jetbrains.com/issue/KT-57373) K2: FIR properties synthesized when implementing interface by delegation don't have accessors
- [`KT-56583`](https://youtrack.jetbrains.com/issue/KT-56583) K1: Implement opt-in for integer cinterop conversions
- [`KT-59030`](https://youtrack.jetbrains.com/issue/KT-59030) [PL] Workaround for broken `@Deprecated` annotations in c-interop KLIBs
- [`KT-58618`](https://youtrack.jetbrains.com/issue/KT-58618) K2: Local property delegates cannot infer generic return type
- [`KT-36770`](https://youtrack.jetbrains.com/issue/KT-36770) Prohibit unsafe calls with expected `@NotNull` T and given Kotlin generic parameter with nullable bound
- [`KT-56739`](https://youtrack.jetbrains.com/issue/KT-56739) K2: build Space iOS
- [`KT-57131`](https://youtrack.jetbrains.com/issue/KT-57131) K2: stdlib test compilation fails on ListTest.kt in FirJvmMangleComputer
- [`KT-58137`](https://youtrack.jetbrains.com/issue/KT-58137) K2: ISE "Usage of default value argument for this annotation is not yet possible" when instantiating Kotlin annotation with default parameter from another module
- [`KT-58897`](https://youtrack.jetbrains.com/issue/KT-58897) K2: False positive unresolved reference with same-named enum class and its entry
- [`KT-40903`](https://youtrack.jetbrains.com/issue/KT-40903) Forbid actual member in expect class
- [`KT-30905`](https://youtrack.jetbrains.com/issue/KT-30905) Expect var property with default public setter matches with actual var property with private setter
- [`KT-56172`](https://youtrack.jetbrains.com/issue/KT-56172) K2: Fix reporting of PRIVATE_CLASS_MEMBER_FROM_INLINE error
- [`KT-56171`](https://youtrack.jetbrains.com/issue/KT-56171) Implement deprecation warning for missing PRIVATE_CLASS_MEMBER_FROM_INLINE error
- [`KT-27261`](https://youtrack.jetbrains.com/issue/KT-27261) Contracts for infix functions don't work (for receivers and parameters)
- [`KT-56927`](https://youtrack.jetbrains.com/issue/KT-56927) Enum with secondary constructor can't be compiled with K2 using JS/Native backend
- [`KT-53568`](https://youtrack.jetbrains.com/issue/KT-53568) Partial linkage: absent class as type parameter bound causes failure of `compileProductionExecutableKotlinJs`
- [`KT-53608`](https://youtrack.jetbrains.com/issue/KT-53608) Partial linkage: Kotlin/JS fails with IllegalStateException: "Validation failed in file" when overridden declaration was visible, but now private
- [`KT-53663`](https://youtrack.jetbrains.com/issue/KT-53663) Partial linkage: usage of property which becomes abstract: no IrLinkageError, but AssertionError in Native backend instead
- [`KT-56013`](https://youtrack.jetbrains.com/issue/KT-56013) K2. a set of errors about local properties are missing
- [`KT-53939`](https://youtrack.jetbrains.com/issue/KT-53939) Partial linkage: with turning object into class link*Native and js*Test tasks fail
- [`KT-53938`](https://youtrack.jetbrains.com/issue/KT-53938) Partial linkage: with turning interface into class and using as second parent Native build fails
- [`KT-53941`](https://youtrack.jetbrains.com/issue/KT-53941) Partial linkage: with turning class into object accessing member via parameterless constructor does not fail
- [`KT-53970`](https://youtrack.jetbrains.com/issue/KT-53970) Partial linkage: on turning nested class into inner JS tasks are successful, Native build fails
- [`KT-53971`](https://youtrack.jetbrains.com/issue/KT-53971) Partial linkage: turning inner class into nested: without usage in executable Native is successful, JavaScript fails
- [`KT-53972`](https://youtrack.jetbrains.com/issue/KT-53972) Partial linkage: turning inner class into nested: with usage in executable Native fails with NPE in backend
- [`KT-54045`](https://youtrack.jetbrains.com/issue/KT-54045) Partial linkage: turning class into type alias + calculating implicit function type: build fails with UninitializedPropertyAccessException: "lateinit property parent has not been initialized"
- [`KT-54046`](https://youtrack.jetbrains.com/issue/KT-54046) Partial linkage: turning type alias into class + using it as type: build fails with AssertionError: "Expected exactly one delegating constructor call but none encountered"
- [`KT-53887`](https://youtrack.jetbrains.com/issue/KT-53887) Partial linkage: turning from enum to regular class + reference to enum contant causes compileProductionExecutableKotlinJs fail with IllegalStateException
- [`KT-54047`](https://youtrack.jetbrains.com/issue/KT-54047) Partial linkage: reference to removed enum const causes JS fail with "IllegalStateException: Validation failed in file"
- [`KT-54048`](https://youtrack.jetbrains.com/issue/KT-54048) Partial linkage: reference to removed enum const in runtime causes Native fail with IllegalStateException at IrBindablePublicSymbolBase.getOwner()
- [`KT-53995`](https://youtrack.jetbrains.com/issue/KT-53995) Partial linkage: on turning class to abstract and direct constructor call Naive fails, JavaScript is successful
- [`KT-43527`](https://youtrack.jetbrains.com/issue/KT-43527) `@ExtensionFunctionType` is allowed on function types with no parameters and leads to backend exception
- [`KT-55316`](https://youtrack.jetbrains.com/issue/KT-55316) K2. IllegalStateException on incorrect import directive name
- [`KT-57570`](https://youtrack.jetbrains.com/issue/KT-57570) Remove source code excerpts from platform type nullability assertion exceptions
- [`KT-56073`](https://youtrack.jetbrains.com/issue/KT-56073) K2: build Exposed
- [`KT-47932`](https://youtrack.jetbrains.com/issue/KT-47932) Report errors on cycles in annotation parameter types
- [`KT-38871`](https://youtrack.jetbrains.com/issue/KT-38871) Kotlin Gradle DSL, MPP: UNUSED_VARIABLE when configuring a sourceset with delegated property
- [`KT-46344`](https://youtrack.jetbrains.com/issue/KT-46344) No error for a super class constructor call on a function interface in supertypes list
- [`KT-56609`](https://youtrack.jetbrains.com/issue/KT-56609) K2: False positive NULL_FOR_NONNULL_TYPE with -Xjsr305=strict and `@Nullable` annotation Java parameter
- [`KT-56656`](https://youtrack.jetbrains.com/issue/KT-56656) K1/K2: inconsistent NOTHING_TO_OVERRIDE with complex nullable annotations
- [`KT-58332`](https://youtrack.jetbrains.com/issue/KT-58332) K2: local fun with suspend type is not marked as suspend in IR
- [`KT-57991`](https://youtrack.jetbrains.com/issue/KT-57991) K2: Modifier 'suspend' is not applicable to 'anonymous function'
- [`KT-54294`](https://youtrack.jetbrains.com/issue/KT-54294) K2: "Not all type variables found" in builder inference with type parameters inferred through a union of two branches
- [`KT-58564`](https://youtrack.jetbrains.com/issue/KT-58564) [PL] Annotations with unlinked parameters are not removed
- [`KT-52597`](https://youtrack.jetbrains.com/issue/KT-52597) Provide Alpha Support for Multiplatform in the K2 platform
- [`KT-58523`](https://youtrack.jetbrains.com/issue/KT-58523) K2: reference is resolved to imported type-alias instead of identically named top-level property
- [`KT-57098`](https://youtrack.jetbrains.com/issue/KT-57098) Native: avoid object initialization while accessing const val
- [`KT-57973`](https://youtrack.jetbrains.com/issue/KT-57973) 32-th default value in inline classes override function is not used
- [`KT-57714`](https://youtrack.jetbrains.com/issue/KT-57714) "IllegalStateException: <B::!>" using reified generics
- [`KT-57810`](https://youtrack.jetbrains.com/issue/KT-57810) `toString` of object erroneously considered as constant function in string concatenation
- [`KT-58076`](https://youtrack.jetbrains.com/issue/KT-58076) K2: Incorrect inference of type of labeled receiver
- [`KT-57929`](https://youtrack.jetbrains.com/issue/KT-57929) K2: Arguments of annotations  are not calculated in a lot of strange locations
- [`KT-54874`](https://youtrack.jetbrains.com/issue/KT-54874) K2. No compilation error with incorrect Comparator invocation
- [`KT-55388`](https://youtrack.jetbrains.com/issue/KT-55388) Consider enabling ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
- [`KT-53041`](https://youtrack.jetbrains.com/issue/KT-53041) NPE in Kotlin 1.7.0 when using RxJava Maybe.doOnEvent with anonymous parameters
- [`KT-54829`](https://youtrack.jetbrains.com/issue/KT-54829) Cleanup local types approximation logic
- [`KT-58577`](https://youtrack.jetbrains.com/issue/KT-58577) K2: private Kotlin property prevents use of Java set-method from Java-Kotlin-Java hierarchy in another module
- [`KT-58587`](https://youtrack.jetbrains.com/issue/KT-58587) MUST_BE_INITIALIZED must take into account effectivelly final
- [`KT-58524`](https://youtrack.jetbrains.com/issue/KT-58524) K2: false-positive overload resolution ambiguity error on invoking a generic class's member function with id-shaped function-typed parameter on intersection-typed receiver
- [`KT-53929`](https://youtrack.jetbrains.com/issue/KT-53929) Enum.entries: consider changing scope behavior in K1
- [`KT-58520`](https://youtrack.jetbrains.com/issue/KT-58520) K2: FIR2IR: ISE during const evaluation of operator times with exposed
- [`KT-57905`](https://youtrack.jetbrains.com/issue/KT-57905) K1: resolution to base class's Java field instead of derived class's Kotlin property is not deprecated in case of different types
- [`KT-56662`](https://youtrack.jetbrains.com/issue/KT-56662) K1: false negative INVISIBLE_SETTER for a var with internal setter accessed from a derived class
- [`KT-57770`](https://youtrack.jetbrains.com/issue/KT-57770) K2: Support generation of serializer if base class for serializable class declared in different module
- [`KT-58375`](https://youtrack.jetbrains.com/issue/KT-58375) Kapt: "wrong number of type arguments. required 1" when more than 22 type arguments
- [`KT-48870`](https://youtrack.jetbrains.com/issue/KT-48870) [FIR] Different behavior for explicit receiver resolution inside delegated constructors
- [`KT-58013`](https://youtrack.jetbrains.com/issue/KT-58013) K2: "Not enough information to infer type variable T" when using assert non-null (!!) and delegation
- [`KT-58365`](https://youtrack.jetbrains.com/issue/KT-58365) K2: Fix stub types leakage in builder inference caused by implicit receiver type update with partially resolved calls (IGNORE_LEAKED_INTERNAL_TYPES for stub types)
- [`KT-58214`](https://youtrack.jetbrains.com/issue/KT-58214) Continuation parameter only exists in lowered suspend functions, but function origin is LOCAL_FUNCTION_FOR_LAMBDA
- [`KT-58030`](https://youtrack.jetbrains.com/issue/KT-58030) K2/MPP/JVM: compiler backend crash on super-call to indirectly inherited Java method
- [`KT-58135`](https://youtrack.jetbrains.com/issue/KT-58135) K2: Priority of extension property is lower than ordinary property
- [`KT-57181`](https://youtrack.jetbrains.com/issue/KT-57181) [K1/N, K2/N] Expect and Actual funs have different IdSignature.CommonSignature, if Expect has default argument
- [`KT-58219`](https://youtrack.jetbrains.com/issue/KT-58219) K2/MPP/metadata: false-positive invisible reference error in Native-shared source set
- [`KT-58145`](https://youtrack.jetbrains.com/issue/KT-58145) K2/MPP/metadata: compiler FIR crash on inheritance of a generic class with property by actual-class from Native-shared source set
- [`KT-56023`](https://youtrack.jetbrains.com/issue/KT-56023) Constant operations (e.g. division) are not constant in K2 (JS, Native)
- [`KT-57354`](https://youtrack.jetbrains.com/issue/KT-57354) In suspend function default arguments are sometimes not deleted in IR
- [`KT-55242`](https://youtrack.jetbrains.com/issue/KT-55242) K2/MPP: basic build/link functionality
- [`KT-57979`](https://youtrack.jetbrains.com/issue/KT-57979) K2: Unresolved reference error when assigning to Java synthetic property with a different nullability getter
- [`KT-57543`](https://youtrack.jetbrains.com/issue/KT-57543) K2 reports DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE
- [`KT-58142`](https://youtrack.jetbrains.com/issue/KT-58142) K2: val parameter with more specific type is lower priority
- [`KT-48546`](https://youtrack.jetbrains.com/issue/KT-48546) Missed TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM error at plus-assign
- [`KT-57854`](https://youtrack.jetbrains.com/issue/KT-57854) RECEIVER_TYPE_MISMATCH on synthetic property from mutually recursive Java generics with disabled ProperTypeInferenceConstraintsProcessing
- [`KT-54518`](https://youtrack.jetbrains.com/issue/KT-54518) False negative NON_PUBLIC_CALL_FROM_PUBLIC_INLINE when calling internal method of super class
- [`KT-58025`](https://youtrack.jetbrains.com/issue/KT-58025) K2: Argument type mismatch when using Springs HandlerMethodArgumentResolver
- [`KT-58259`](https://youtrack.jetbrains.com/issue/KT-58259) Unexpected unresolved function call with obvious invoke-convention desugaring
- [`KT-57135`](https://youtrack.jetbrains.com/issue/KT-57135) K2: Fir should take into account an annotation's allowed targets as well as the use-site target when deciding whether it applies to a property, a field, or a constructor parameter
- [`KT-57069`](https://youtrack.jetbrains.com/issue/KT-57069) K2: Method kind in metadata is DECLARATION when DELEGATION is used in K1
- [`KT-57958`](https://youtrack.jetbrains.com/issue/KT-57958) K2: Initializer type mismatch when using extension property on type with star projection
- [`KT-58149`](https://youtrack.jetbrains.com/issue/KT-58149) K2: New inference error with buildList
- [`KT-58008`](https://youtrack.jetbrains.com/issue/KT-58008) K2: "Cannot find cached type parameter by FIR symbol: T" on suspend function with generic and nested class
- [`KT-57835`](https://youtrack.jetbrains.com/issue/KT-57835) K2: compiler crash on lambda with dynamic receiver
- [`KT-57601`](https://youtrack.jetbrains.com/issue/KT-57601) K2: Builtin function `extensionToString` can't be accessed
- [`KT-57655`](https://youtrack.jetbrains.com/issue/KT-57655) K2: ImplicitIntegerCoercion is not working for named arguments
- [`KT-58143`](https://youtrack.jetbrains.com/issue/KT-58143) K2: overload resolution ambiguity inside dynamic lambda
- [`KT-58132`](https://youtrack.jetbrains.com/issue/KT-58132) K2: Implicit int constant to long converion crashes FirSerializer
- [`KT-57378`](https://youtrack.jetbrains.com/issue/KT-57378) Partial linkage: Run codegen box tests for Native & JS with enabled PL
- [`KT-58207`](https://youtrack.jetbrains.com/issue/KT-58207) K2: Handle result of completion of synthetic call with callable reference argument
- [`KT-56549`](https://youtrack.jetbrains.com/issue/KT-56549) K2: Reference to Java sealed class fails to compile
- [`KT-57994`](https://youtrack.jetbrains.com/issue/KT-57994) K2: Type inference failed on function reference
- [`KT-58099`](https://youtrack.jetbrains.com/issue/KT-58099) interop0 test fails with error "type kotlin.String?  is not supported here: doesn't correspond to any C type"
- [`KT-57671`](https://youtrack.jetbrains.com/issue/KT-57671) Synthetic $EntriesMappings declaration is public and generated even for enums from current module on IC
- [`KT-56517`](https://youtrack.jetbrains.com/issue/KT-56517) K2: Reference to Java record fails to compile: "unresolved reference", "Overload resolution ambiguity between candidates"
- [`KT-58163`](https://youtrack.jetbrains.com/issue/KT-58163) FIR: deserialized default property setter and getter must have FirResolvePhase.ANALYZED_DEPENDENCIES phase
- [`KT-55646`](https://youtrack.jetbrains.com/issue/KT-55646) K2: Report definitely non-nullable as reified error
- [`KT-58043`](https://youtrack.jetbrains.com/issue/KT-58043) k2: Expect call is not removed from IR  with nullability-based overload
- [`KT-56442`](https://youtrack.jetbrains.com/issue/KT-56442) K2: Make sure K2 has the same behavior for defaults with overrides as K1 has
- [`KT-55904`](https://youtrack.jetbrains.com/issue/KT-55904) Fix tests for volatile annotation on K2
- [`KT-57928`](https://youtrack.jetbrains.com/issue/KT-57928) K2: Arguments of annotations on constructor value parameter are not calculated
- [`KT-57814`](https://youtrack.jetbrains.com/issue/KT-57814) K2: Argument type mismatch with delegating property
- [`KT-56490`](https://youtrack.jetbrains.com/issue/KT-56490) Implement deprecation for an anonymous type exposed from inline functions with type argument
- [`KT-57781`](https://youtrack.jetbrains.com/issue/KT-57781) K2: Generated serializer is invisible in a non-JVM test source set
- [`KT-57807`](https://youtrack.jetbrains.com/issue/KT-57807) K2: Symbol already bound exception for arrayOf function from IrBuiltInsOverFir
- [`KT-57962`](https://youtrack.jetbrains.com/issue/KT-57962) K2: No set method providing array access on dynamic
- [`KT-57353`](https://youtrack.jetbrains.com/issue/KT-57353) K2: unresolved reference when using fully qualified object declaration name as an expression, when a declaration package is from another klib and has at least two name segments
- [`KT-57899`](https://youtrack.jetbrains.com/issue/KT-57899) K2: compiler FIR2IR crash on anonymous object with inheritance by delegation to value of smart-casted type parameter
- [`KT-57988`](https://youtrack.jetbrains.com/issue/KT-57988) K2: compiler exception on get operator on dynamic this
- [`KT-57960`](https://youtrack.jetbrains.com/issue/KT-57960) K2: incorrect type inference in lambda with dynamic receiver
- [`KT-57923`](https://youtrack.jetbrains.com/issue/KT-57923) K2: Optional expectation annotation crashes const evaluator
- [`KT-56511`](https://youtrack.jetbrains.com/issue/KT-56511) K1: false negative SMARTCAST_IMPOSSIBLE when alien constructor property is accessed from a private class
- [`KT-58033`](https://youtrack.jetbrains.com/issue/KT-58033) K2 reports Constructor must be private or protected in sealed class in actual sealed class if its constructor has own actual declaration
- [`KT-58061`](https://youtrack.jetbrains.com/issue/KT-58061) K2: false-positive unsupported feature error on callable references to Java methods from annotation interfaces
- [`KT-55079`](https://youtrack.jetbrains.com/issue/KT-55079) Refactor DiagnosticReporterByTrackingStrategy and fix some "diagnostic into black hole" problems
- [`KT-57889`](https://youtrack.jetbrains.com/issue/KT-57889) K2: false-positive lack of information for inline function's type parameter in case of builder-style inference from caller function's return expression
- [`KT-57961`](https://youtrack.jetbrains.com/issue/KT-57961) K2: Unresolved reference using dynamic lambda parameter
- [`KT-57911`](https://youtrack.jetbrains.com/issue/KT-57911) K2: Contracts are not inherited by substitution overrides
- [`KT-57880`](https://youtrack.jetbrains.com/issue/KT-57880) K2: false-positive argument type mismatch due to lambda receiver shadowing labeled outer lambda receiver when assigning lambda to variable
- [`KT-57986`](https://youtrack.jetbrains.com/issue/KT-57986) K2: NPE on building Space
- [`KT-57873`](https://youtrack.jetbrains.com/issue/KT-57873) K2: compiler FIR serialization crash on builder-style inference from lambda's return type
- [`KT-57941`](https://youtrack.jetbrains.com/issue/KT-57941) K2: Assertion error on loading serializable class with non-serializable property compiled with K1 compiler
- [`KT-57947`](https://youtrack.jetbrains.com/issue/KT-57947) K2: Incorrect resolution results when property type for invokeExtension is not inferred
- [`KT-58002`](https://youtrack.jetbrains.com/issue/KT-58002) K2: compiler FIR serialization crash on platform type with type-targeted Java annotation with Java enum as argument
- [`KT-57263`](https://youtrack.jetbrains.com/issue/KT-57263) K2/MPP/JVM: compiler codegen crash on expect-property as default argument for expect-function's parameter
- [`KT-56942`](https://youtrack.jetbrains.com/issue/KT-56942) K2: False-negative NO_ELSE_IN_WHEN if subject is flexible type
- [`KT-56687`](https://youtrack.jetbrains.com/issue/KT-56687) Unexpected behaviour with enum entries when using outdated stdlib
- [`KT-56398`](https://youtrack.jetbrains.com/issue/KT-56398) K2/MPP: compiler backend crash on inheritance from expected interface
- [`KT-57806`](https://youtrack.jetbrains.com/issue/KT-57806) K2: string interpolation as annotation parameter causes error
- [`KT-57611`](https://youtrack.jetbrains.com/issue/KT-57611) K2: Annotation arguments are not evaluated
- [`KT-56190`](https://youtrack.jetbrains.com/issue/KT-56190) [K2/N] Const initializers are not serialized to klib
- [`KT-57843`](https://youtrack.jetbrains.com/issue/KT-57843) K2: Missing diagnostic when calling constructor through typealias whose expansion has a deprecation
- [`KT-57350`](https://youtrack.jetbrains.com/issue/KT-57350) FIR: deprecation diagnostic is not reported on a super class call
- [`KT-57532`](https://youtrack.jetbrains.com/issue/KT-57532) K2: IrActualizer doesn't handle properties overloaded by extension receiver correctly
- [`KT-57776`](https://youtrack.jetbrains.com/issue/KT-57776) K2: Suppressing "INVISIBLE_REFERENCE" leads to AssertionError: Unexpected IR element found during code generation
- [`KT-57769`](https://youtrack.jetbrains.com/issue/KT-57769) [K2] Load properties in proper order for classes compiled with kotlinx.serialization and LV < 2.0
- [`KT-57879`](https://youtrack.jetbrains.com/issue/KT-57879) K2: compiler FIR serialization crash on passing Java constants as arguments to type-targeted annotations
- [`KT-57893`](https://youtrack.jetbrains.com/issue/KT-57893) K1/K2 inconsistency on smart casts of internally visible properties in friend modules
- [`KT-57876`](https://youtrack.jetbrains.com/issue/KT-57876) K2: stack overflow in compiler FIR deserialization on nested type-target annotation class used in enclosing class
- [`KT-57839`](https://youtrack.jetbrains.com/issue/KT-57839) K2: Compiler crash on lambda returning anonymous object with implemented lambda
- [`KT-57822`](https://youtrack.jetbrains.com/issue/KT-57822) K2: Can't refer to external interface from class literal
- [`KT-57809`](https://youtrack.jetbrains.com/issue/KT-57809) K2: No value passed for parameter of external class
- [`KT-56383`](https://youtrack.jetbrains.com/issue/KT-56383) Build intellij master with LV 1.9
- [`KT-57735`](https://youtrack.jetbrains.com/issue/KT-57735) K2: MPP: K2 reports hides member of supertype and needs 'override' modifier for the function with `@PlatformDependent` annotation when there is an empty linux target in project
- [`KT-55056`](https://youtrack.jetbrains.com/issue/KT-55056) Builder inference causes incorrect type inference result in related call
- [`KT-57689`](https://youtrack.jetbrains.com/issue/KT-57689) K2: Unresolved reference to nested typealias in KLIB
- [`KT-57665`](https://youtrack.jetbrains.com/issue/KT-57665) K2: incorrect resolution of dynamic type
- [`KT-57381`](https://youtrack.jetbrains.com/issue/KT-57381) K2/MPP/Native: impossible to override Any::equals with non-external function
- [`KT-57654`](https://youtrack.jetbrains.com/issue/KT-57654) K2: Lambda with receiver deserialized as lambda without receiver during metadata compilation
- [`KT-57662`](https://youtrack.jetbrains.com/issue/KT-57662) K2: The error message is poorly formatted and not precise in case of NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS error and `@Suppress` is used
- [`KT-57763`](https://youtrack.jetbrains.com/issue/KT-57763) FirExtensionRegistrar extension point broken
- [`KT-57312`](https://youtrack.jetbrains.com/issue/KT-57312) K2: IR interpreter fails on string interpolation with `const val` from a klib involved
- [`KT-57768`](https://youtrack.jetbrains.com/issue/KT-57768) Don't decompile code to search for annotation arguments
- [`KT-55628`](https://youtrack.jetbrains.com/issue/KT-55628) Diagnostics for kotlin.concurrent.Volatile annotation applicability
- [`KT-55860`](https://youtrack.jetbrains.com/issue/KT-55860) K2. [CONFLICTING_INHERITED_MEMBERS] for inheritor of a class with overloaded generic function
- [`KT-53491`](https://youtrack.jetbrains.com/issue/KT-53491) K2: Implement "Operator '==' cannot be applied to 'Long' and 'Int'" error
- [`KT-55804`](https://youtrack.jetbrains.com/issue/KT-55804) K2: UNSAFE_CALL Non-nullable generic marked as nullable even if non-null asserted
- [`KT-57682`](https://youtrack.jetbrains.com/issue/KT-57682) K2: Incorrect composing of signatures for calls on dynamic types
- [`KT-55405`](https://youtrack.jetbrains.com/issue/KT-55405) K2: false-negative INVISIBLE_REFERENCE in import directives
- [`KT-54781`](https://youtrack.jetbrains.com/issue/KT-54781) K2: no error on unresolved import statement with more than one package
- [`KT-57635`](https://youtrack.jetbrains.com/issue/KT-57635) K2/MPP: Expect constructors are not considered as expect during metadata deserialization
- [`KT-57376`](https://youtrack.jetbrains.com/issue/KT-57376) K2/MPP: false-positive K/JS diagnostic in absence of K/JS target when sharing a source set between K/JVM and K/Native
- [`KT-55902`](https://youtrack.jetbrains.com/issue/KT-55902) K2: Support ImplicitIntegerCoercion annotation
- [`KT-56577`](https://youtrack.jetbrains.com/issue/KT-56577) Migrate Native KLIB ABI compatibility tests to K2
- [`KT-56603`](https://youtrack.jetbrains.com/issue/KT-56603) [K2/N] Segfault invoking fun from binary compatible klib
- [`KT-57457`](https://youtrack.jetbrains.com/issue/KT-57457) K2: the error message is not quite informative in case of EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR
- [`KT-57568`](https://youtrack.jetbrains.com/issue/KT-57568) K2: K2, Native reports overload resolution ambiguity
- [`KT-57446`](https://youtrack.jetbrains.com/issue/KT-57446) K2: Adapter function reference is not generated inside when expression
- [`KT-54894`](https://youtrack.jetbrains.com/issue/KT-54894) K2: False positive RETURN_TYPE_MISMATCH on function which returns a functional type with `@UnsafeVariance` argument
- [`KT-57001`](https://youtrack.jetbrains.com/issue/KT-57001) K2 compilation fails due to nullabillity subtyping not working properly
- [`KT-57271`](https://youtrack.jetbrains.com/issue/KT-57271) Delay forbidding inference to an empty intersection to version 2.0
- [`KT-57209`](https://youtrack.jetbrains.com/issue/KT-57209) K2: type parameters are available in companion object scope
- [`KT-50550`](https://youtrack.jetbrains.com/issue/KT-50550) False positive NO_ELSE_IN_WHEN with annotated `when` branch condition
- [`KT-57431`](https://youtrack.jetbrains.com/issue/KT-57431) K2 MPP JS: Compiler crash on transitive common dependencies
- [`KT-57456`](https://youtrack.jetbrains.com/issue/KT-57456) K2 reports uninitializied variable in enum class when variable is used in lambda and defined in companion object
- [`KT-57583`](https://youtrack.jetbrains.com/issue/KT-57583) K2/MPP/JS&Native: FIR2IR compiler crash on reference to Any method inherited by expect-classifier
- [`KT-56336`](https://youtrack.jetbrains.com/issue/KT-56336) [K2/N] Multiplatform test fails with unexpected "actual declaration has no corresponding expected declaration" compiler error
- [`KT-57556`](https://youtrack.jetbrains.com/issue/KT-57556) K2: Rename error 'This API is not available after FIR'
- [`KT-23447`](https://youtrack.jetbrains.com/issue/KT-23447) Integer.toChar compiles to missing method
- [`KT-46465`](https://youtrack.jetbrains.com/issue/KT-46465) Deprecate and make open Number.toChar()
- [`KT-49017`](https://youtrack.jetbrains.com/issue/KT-49017) Forbid usages of super or super<Some> if in fact it accesses an abstract member
- [`KT-56119`](https://youtrack.jetbrains.com/issue/KT-56119) BinaryVersion.isCompatible binary compatibility is broken
- [`KT-57369`](https://youtrack.jetbrains.com/issue/KT-57369) K2/MPP: supertypes established in actual-classifiers from other source sets are not visible
- [`KT-55469`](https://youtrack.jetbrains.com/issue/KT-55469) [K2/N] equals(Double,Double) and equals(Boolean,Boolean) are not found
- [`KT-57250`](https://youtrack.jetbrains.com/issue/KT-57250) K2: the metadata is serialized for an `expect` class even if the `actual` class is present when compiling to klib
- [`KT-56660`](https://youtrack.jetbrains.com/issue/KT-56660) K2/MPP: compiler backend crash on invoking a K/Common constructor in K/JS code
- [`KT-55055`](https://youtrack.jetbrains.com/issue/KT-55055) K1: Builder inference violates upper bound
- [`KT-57316`](https://youtrack.jetbrains.com/issue/KT-57316) Initialize Enum.entries eagerly: avoid using invokedynamics
- [`KT-57491`](https://youtrack.jetbrains.com/issue/KT-57491) Kotlin synthetic parameter looks ordinary
- [`KT-56846`](https://youtrack.jetbrains.com/issue/KT-56846) K2: incorrect line & symbol numbers in exception reporting
- [`KT-56368`](https://youtrack.jetbrains.com/issue/KT-56368) K2/MPP: compiler backend crash on missing actual declaration
- [`KT-57104`](https://youtrack.jetbrains.com/issue/KT-57104) K2: false-positive conflicting inherited JVM declarations error despite use of `@JvmName` in another module
- [`KT-56747`](https://youtrack.jetbrains.com/issue/KT-56747) [K2/N] Return type for `lambda: (Any) -> Any` which returns Unit is different for K1 and K2 and return statement is missing with K2
- [`KT-57211`](https://youtrack.jetbrains.com/issue/KT-57211) K2: incorrect "error: an annotation argument must be a compile-time constant" on unsigned array in annotation argument
- [`KT-57302`](https://youtrack.jetbrains.com/issue/KT-57302) K2 fails with IllegalStateException on reading inherited property of Java enum
- [`KT-57424`](https://youtrack.jetbrains.com/issue/KT-57424) K2 IDE: "By now the annotations argument mapping should have been resolved" exception
- [`KT-57241`](https://youtrack.jetbrains.com/issue/KT-57241) K2 MPP: Actualization doesn't work for actual enum that has primary constructor with arguments
- [`KT-57210`](https://youtrack.jetbrains.com/issue/KT-57210) K2 MPP: Support of arguments with dynamic type
- [`KT-57182`](https://youtrack.jetbrains.com/issue/KT-57182) K2 MPP: Actualization doesn't work for nested objects
- [`KT-56344`](https://youtrack.jetbrains.com/issue/KT-56344) K2: Implement correct errors reporting of IrActualizer
- [`KT-54405`](https://youtrack.jetbrains.com/issue/KT-54405) K2 compiler allows val redeclaration
- [`KT-54531`](https://youtrack.jetbrains.com/issue/KT-54531) [K2] Uncaught Runtime exception is thrown instead of user friendly error messages with details in case -no-jdk option set to true
- [`KT-56926`](https://youtrack.jetbrains.com/issue/KT-56926) K2: incorrect line number generated for class constructor or method with default parameter when comment before
- [`KT-56913`](https://youtrack.jetbrains.com/issue/KT-56913) K2: Incorrect line numbers in overriden field getters and setters
- [`KT-56982`](https://youtrack.jetbrains.com/issue/KT-56982) K2: Incorrect line number start in when expression
- [`KT-56720`](https://youtrack.jetbrains.com/issue/KT-56720) K2: false positive MANY_IMPL_MEMBER_NOT_IMPLEMENTED in case of delegation in diamond inheritance
- [`KT-57175`](https://youtrack.jetbrains.com/issue/KT-57175) K2: false-positive INVALID_TYPE_OF_ANNOTATION_MEMBER on type aliases
- [`KT-25694`](https://youtrack.jetbrains.com/issue/KT-25694) Fix reporting of uninitialized parameter in default values of parameters
- [`KT-57198`](https://youtrack.jetbrains.com/issue/KT-57198) K2: false-positive type mismatch error on inherited raw-typed class with type parameters in upper bounds of other type parameters
- [`KT-15470`](https://youtrack.jetbrains.com/issue/KT-15470) Inconsistency: use-site 'set' target is a compilation error, use-site 'get' target is ok
- [`KT-57179`](https://youtrack.jetbrains.com/issue/KT-57179) FIR: preserve prefix increment behavior like in K1 by calling getter twice
- [`KT-57405`](https://youtrack.jetbrains.com/issue/KT-57405) K2. Function call ambiguity error when nullable String is passed to function with Spring `@Nullable` annotation in signature
- [`KT-57284`](https://youtrack.jetbrains.com/issue/KT-57284) K2: compiler codegen crash at property initialization in constructor after smartcast of dispatch receiver to indirectly derived type
- [`KT-57221`](https://youtrack.jetbrains.com/issue/KT-57221) K2: compiler FIR2IR crash on function's unavailable cached type parameter
- [`KT-57036`](https://youtrack.jetbrains.com/issue/KT-57036) Unresolved reference: with inferred type of class constructor with extension parameter
- [`KT-56177`](https://youtrack.jetbrains.com/issue/KT-56177) K2: FIR should not generate annotation on both property and parameter
- [`KT-54990`](https://youtrack.jetbrains.com/issue/KT-54990) NI: Type mismatch when encountering bounded type parameter and projections
- [`KT-57065`](https://youtrack.jetbrains.com/issue/KT-57065) K2: overload resolution ambiguity between type-aliased constructor and identically named function
- [`KT-49653`](https://youtrack.jetbrains.com/issue/KT-49653) Deprecate and remove Enum.declaringClass synthetic property
- [`KT-57190`](https://youtrack.jetbrains.com/issue/KT-57190) K2: false-positive unsafe call error on safe call on type-aliased nullable receiver in SAM-conversion
- [`KT-57166`](https://youtrack.jetbrains.com/issue/KT-57166) K2: false-positive val reassignment error when synthetic property with implicitly typed overridden getter is called from implicitly typed member that is declared earlier
- [`KT-55828`](https://youtrack.jetbrains.com/issue/KT-55828) [K2/N]: Fix test fails in OPT mode : `Internal compiler error: no implementation found ... when building itable/vtable`
- [`KT-56169`](https://youtrack.jetbrains.com/issue/KT-56169) False negative deprecation warning about future inference error with builder inference
- [`KT-56657`](https://youtrack.jetbrains.com/issue/KT-56657) K1/K2: inconsistent behavior in nullability mismatch (Guava hash set/map)
- [`KT-57105`](https://youtrack.jetbrains.com/issue/KT-57105) K2: compiler codegen crash at property initialization in constructor after smartcast
- [`KT-56379`](https://youtrack.jetbrains.com/issue/KT-56379) K2: build tests for the Kotlin standard library
- [`KT-56079`](https://youtrack.jetbrains.com/issue/KT-56079) K2: build YouTrack 2022.3
- [`KT-57092`](https://youtrack.jetbrains.com/issue/KT-57092) K2: false-positive multiple inherited implementations error
- [`KT-56696`](https://youtrack.jetbrains.com/issue/KT-56696) K2: Allow to access uninitialized member properties in non-inPlace lambdas in class initialization
- [`KT-56354`](https://youtrack.jetbrains.com/issue/KT-56354) K2/MPP: unresolved references to library entities
- [`KT-57095`](https://youtrack.jetbrains.com/issue/KT-57095) K2: false-positive lack of type arguments error on raw cast of Base<*> to Derived<T>: Base<T?>
- [`KT-56630`](https://youtrack.jetbrains.com/issue/KT-56630) FIR: ClassCastException on compilation hierarchy with a raw type
- [`KT-57171`](https://youtrack.jetbrains.com/issue/KT-57171) K2: Implement bytecode tests
- [`KT-57214`](https://youtrack.jetbrains.com/issue/KT-57214) K2: compiler FIR crash on annotation usage before annotation class declaration
- [`KT-57204`](https://youtrack.jetbrains.com/issue/KT-57204) K2: callable reference to mutable property of inherited by delegation superinterface isn't properly resolved
- [`KT-57195`](https://youtrack.jetbrains.com/issue/KT-57195) K2: false-positive VAR_TYPE_MISMATCH_ON_OVERRIDE on changing property's platform type to non-nullable type when overriding
- [`KT-56814`](https://youtrack.jetbrains.com/issue/KT-56814) K2. PsiElement is null inside IrClass. As a result ClassBuilder defineClass gets null as origin
- [`KT-54758`](https://youtrack.jetbrains.com/issue/KT-54758) Deprecate `ClassBuilderInterceptorExtension.interceptClassBuilderFactory` and provide another method without dependency on K1
- [`KT-57253`](https://youtrack.jetbrains.com/issue/KT-57253) K2: clean up callable reference logic in FIR2IR
- [`KT-56225`](https://youtrack.jetbrains.com/issue/KT-56225) K2. "BackendException: Backend Internal error: Exception during IR lowering" error on incorrect constructor in inline class
- [`KT-56769`](https://youtrack.jetbrains.com/issue/KT-56769) K2. Annotation applicability is ignored during compilation when there's use-site `@target`
- [`KT-56616`](https://youtrack.jetbrains.com/issue/KT-56616) K2: cannot infer Java array type properly
- [`KT-57247`](https://youtrack.jetbrains.com/issue/KT-57247) K2: false-positive INVALID_TYPE_OF_ANNOTATION_MEMBER on type-aliased vararg property
- [`KT-57206`](https://youtrack.jetbrains.com/issue/KT-57206) K2: false-positive val reassignment error on synthetic property from generic class with overridden getter but not setter
- [`KT-56519`](https://youtrack.jetbrains.com/issue/KT-56519) K2: Compiler crash on a function reference on companion receiver that inherits from outer class
- [`KT-56506`](https://youtrack.jetbrains.com/issue/KT-56506) K1/K2 inconsistency: VAL_REASSIGNMENT on synthetic setter with different nullability
- [`KT-56877`](https://youtrack.jetbrains.com/issue/KT-56877) K2: false-positive UNRESOLVED_LABEL for labeled this-expression in contract description
- [`KT-56863`](https://youtrack.jetbrains.com/issue/KT-56863) K2: false-positive property initialization analysis errors after smartcast
- [`KT-56864`](https://youtrack.jetbrains.com/issue/KT-56864) K2: Unexpected behavior with default parameter inheritance and function reference
- [`KT-56665`](https://youtrack.jetbrains.com/issue/KT-56665) K2: false positive RECURSIVE_TYPEALIAS_EXPANSION
- [`KT-53966`](https://youtrack.jetbrains.com/issue/KT-53966) K2 does not support SAM conversions with condition into Java/Kotlin functional interfaces
- [`KT-56659`](https://youtrack.jetbrains.com/issue/KT-56659) FIR: Increment operator on object leads to exception from resolve
- [`KT-56771`](https://youtrack.jetbrains.com/issue/KT-56771) FIR: Increment operator on qualified expressions leads to exception from resolve
- [`KT-56759`](https://youtrack.jetbrains.com/issue/KT-56759) K2: False-positive UNRESOLVED_REFERENCE on labeled desctructuring declaration in LT mode
- [`KT-56548`](https://youtrack.jetbrains.com/issue/KT-56548) K2: false positive overload resolution ambiguity for Java record constructor
- [`KT-56476`](https://youtrack.jetbrains.com/issue/KT-56476) K2: false positive NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY at inline fun use-site
- [`KT-56138`](https://youtrack.jetbrains.com/issue/KT-56138) K2: Illegal conversion of lambda with parameters to ExtensionFunction expected type
- [`KT-56448`](https://youtrack.jetbrains.com/issue/KT-56448) K2: False-positive unsafe call due to incorrect inference of smartcasted type
- [`KT-55966`](https://youtrack.jetbrains.com/issue/KT-55966) K2: Not enough information to infer type variable K if smartcast is used
- [`KT-57168`](https://youtrack.jetbrains.com/issue/KT-57168) K2: compiler FIR2IR crash on synthetic property from generic class with overridden getter but not setter
- [`KT-56876`](https://youtrack.jetbrains.com/issue/KT-56876) K2: false-positive UNRESOLVED_REFERENCE for name of nested class in contract description
- [`KT-57194`](https://youtrack.jetbrains.com/issue/KT-57194) K2: overload resolution doesn't prioritize Double over Float
- [`KT-57067`](https://youtrack.jetbrains.com/issue/KT-57067) Warning about expect/aсtual in the same module doesn't take into account absence of 'actual' modifier
- [`KT-56954`](https://youtrack.jetbrains.com/issue/KT-56954) K2: function literals can be passed as arguments to parameters with kotlin.reflect function types
- [`KT-55423`](https://youtrack.jetbrains.com/issue/KT-55423) K2: Implement CONTRACT_NOT_ALLOWED
- [`KT-56923`](https://youtrack.jetbrains.com/issue/KT-56923) K2: no line number in bytecode when ASTORE exception in catch
- [`KT-56829`](https://youtrack.jetbrains.com/issue/KT-56829) K2: compiler FIR2IR crash on passing to function a callable reference to nested class's constructor with default arguments
- [`KT-57029`](https://youtrack.jetbrains.com/issue/KT-57029) Per-file caches fail on local inline function in an inline function
- [`KT-57085`](https://youtrack.jetbrains.com/issue/KT-57085) K2: `@Suppress` is sensitive to its argument's case
- [`KT-57103`](https://youtrack.jetbrains.com/issue/KT-57103) K1: AssertionError: Mismatching type arguments: 0 vs 1 + 0 when calling inline function with callable reference to generic synthetic property
- [`KT-57033`](https://youtrack.jetbrains.com/issue/KT-57033) Make KtClassLiteralExpression stub based
- [`KT-57035`](https://youtrack.jetbrains.com/issue/KT-57035) Make KtCollectionLiteralExpression stub based
- [`KT-40857`](https://youtrack.jetbrains.com/issue/KT-40857) Invalid parameterized types for extension function on parameterized receiver when javaParameters=true
- [`KT-56154`](https://youtrack.jetbrains.com/issue/KT-56154) Compiler backend crash on reference to Java synthetic property from generic class
- [`KT-56692`](https://youtrack.jetbrains.com/issue/KT-56692) StackOverflow in PrivateInlineFunctionsReturningAnonymousObjectsChecker
- [`KT-55879`](https://youtrack.jetbrains.com/issue/KT-55879) Modularized tests: fir.bench.language.version is used as API version, not language version
- [`KT-51821`](https://youtrack.jetbrains.com/issue/KT-51821) ClassCastException on anonymous fun interface implementation when unrelated vararg is used
- [`KT-56820`](https://youtrack.jetbrains.com/issue/KT-56820) K2: compiler FIR crash on Java field access after smartcast
- [`KT-56579`](https://youtrack.jetbrains.com/issue/KT-56579) [K2/N] IR actualizer crashed with K2 on expect annotation marked with `@OptionalExpectation`, without actual.
- [`KT-56750`](https://youtrack.jetbrains.com/issue/KT-56750) K2: "IllegalArgumentException: No argument for parameter VALUE_PARAMETER" when calling typealias method reference
- [`KT-55614`](https://youtrack.jetbrains.com/issue/KT-55614) K2: consider serializing static enum members (values/valueOf/entries) to match K1 behavior
- [`KT-30507`](https://youtrack.jetbrains.com/issue/KT-30507) Unsound smartcast if null assignment inside index place and plusAssign/minusAssign is used
- [`KT-56646`](https://youtrack.jetbrains.com/issue/KT-56646) K2: "IllegalStateException: No single implementation found for: FUN FAKE_OVERRIDE" when compiling a functional interface
- [`KT-56334`](https://youtrack.jetbrains.com/issue/KT-56334) K2: can't call expected function with default arguments
- [`KT-56514`](https://youtrack.jetbrains.com/issue/KT-56514) K2 should report ACTUAL_TYPE_ALIAS_NOT_TO_CLASS
- [`KT-56522`](https://youtrack.jetbrains.com/issue/KT-56522) K2 should report ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS
- [`KT-56910`](https://youtrack.jetbrains.com/issue/KT-56910) Exception during IR lowering: Cannot determine lineNumber of element FUN name:cancelProgress
- [`KT-56542`](https://youtrack.jetbrains.com/issue/KT-56542) K2: false positive TOO_MANY_ARGUMENTS in VarHandle.set call
- [`KT-56861`](https://youtrack.jetbrains.com/issue/KT-56861) FIR: test FirPluginBlackBoxCodegenTestGenerated.testClassWithAllPropertiesConstructor is failing with runtime error
- [`KT-56234`](https://youtrack.jetbrains.com/issue/KT-56234) K2: "ISE: Expected value generated with NEW" with inline property setter and noinline parameter
- [`KT-56722`](https://youtrack.jetbrains.com/issue/KT-56722) K2: cannot resolve component call after smart cast
- [`KT-56875`](https://youtrack.jetbrains.com/issue/KT-56875) K2: isOperator flag is incorrectly set for java methods
- [`KT-56714`](https://youtrack.jetbrains.com/issue/KT-56714) K2: wrong argument mapping in DSL
- [`KT-56723`](https://youtrack.jetbrains.com/issue/KT-56723) K2: lambda accidentally returns Unit? instead of Unit
- [`KT-55877`](https://youtrack.jetbrains.com/issue/KT-55877) K2: Secondary constructor without call to parent: no frontend error, ISE: "Null argument in ExpressionCodegen for parameter VALUE_PARAMETER"
- [`KT-56386`](https://youtrack.jetbrains.com/issue/KT-56386) K2: Make possible to access Java field which is shadowed by Kotlin invisible property`
- [`KT-56862`](https://youtrack.jetbrains.com/issue/KT-56862) Compatibility problem with using Kotlin in Intellij 223 or higher because of missing particular trove4j dependency
- [`KT-55088`](https://youtrack.jetbrains.com/issue/KT-55088) JS, Native compilation fail with internal error on `SomeEnum.entries` reference when `SomeEnum` is from klib compiled with disabled EnumEntries language feature
- [`KT-40904`](https://youtrack.jetbrains.com/issue/KT-40904) No warning when declare actual in the same target (module) as expect
- [`KT-56707`](https://youtrack.jetbrains.com/issue/KT-56707) K2: Unexpected TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM where only getter type specified explicitly
- [`KT-56508`](https://youtrack.jetbrains.com/issue/KT-56508) Context receivers: Internal compiler error when compiling code containing a class with a secondary constructor
- [`KT-56706`](https://youtrack.jetbrains.com/issue/KT-56706) K2: False-positive ARGUMENT_TYPE_MISMATCH for generic nested types from library
- [`KT-56505`](https://youtrack.jetbrains.com/issue/KT-56505) K2: Missing `NO_EXPLICIT_VISIBILITY_IN_API_MODE` errors on various declarations
- [`KT-56682`](https://youtrack.jetbrains.com/issue/KT-56682) K2: False-negative UNINITIALIZED_VARIABLE on access to delegated property
- [`KT-56678`](https://youtrack.jetbrains.com/issue/KT-56678) K2: False-negative UNINITIALIZED_VARIABLE if corresponding variable has initializer
- [`KT-56612`](https://youtrack.jetbrains.com/issue/KT-56612) K2: false positive NO_TYPE_ARGUMENTS_ON_RHS on raw cast with type alias based argument
- [`KT-56445`](https://youtrack.jetbrains.com/issue/KT-56445) K2: False-positive unresolved reference to callable reference to function with default argument
- [`KT-55024`](https://youtrack.jetbrains.com/issue/KT-55024) K2: overload resolution ambiguity/unresolved reference if variable is smart-casted to an invisible internal class
- [`KT-55722`](https://youtrack.jetbrains.com/issue/KT-55722) K2: Incorrect OVERLOAD_RESOLUTION_AMBIGUITY with smart cast on dispatch receiver (simple)
- [`KT-56563`](https://youtrack.jetbrains.com/issue/KT-56563) Inference within if stops working when changing expected type from Any to a different type
- [`KT-55936`](https://youtrack.jetbrains.com/issue/KT-55936) K2: Support proper resolution of callable references as last statements in lambda
- [`KT-45989`](https://youtrack.jetbrains.com/issue/KT-45989) FIR: wrong callable reference type inferred
- [`KT-55169`](https://youtrack.jetbrains.com/issue/KT-55169) K2: False-negative NO_ELSE_IN_WHEN
- [`KT-55932`](https://youtrack.jetbrains.com/issue/KT-55932) K2. No compiler error when elvis operator returns not matched type
- [`KT-53987`](https://youtrack.jetbrains.com/issue/KT-53987) K2: False negative "TYPE_MISMATCH" with if statement return
- [`KT-41038`](https://youtrack.jetbrains.com/issue/KT-41038) NI: TYPE_MISMATCH when passing constructor of nested class
- [`KT-42449`](https://youtrack.jetbrains.com/issue/KT-42449) Can not resolve property for value of type Any even after casting type to a type with star projection
- [`KT-52934`](https://youtrack.jetbrains.com/issue/KT-52934) StackOverflow from `PseudocodeTraverserKt.collectDataFromSubgraph` with `if` inside `finally`
- [`KT-52860`](https://youtrack.jetbrains.com/issue/KT-52860) StackOverflowError when casting involving recursive generics and star projection
- [`KT-52424`](https://youtrack.jetbrains.com/issue/KT-52424) ClassCastException: Wrong smartcast to Nothing? with if-else in nullable lambda parameter
- [`KT-52262`](https://youtrack.jetbrains.com/issue/KT-52262) TYPE_MISMATCH: Nonnull smartcasting fails with non-exhaustive when
- [`KT-52502`](https://youtrack.jetbrains.com/issue/KT-52502) Forbid extension calls on inline functional parameters
- [`KT-51045`](https://youtrack.jetbrains.com/issue/KT-51045) SETTER_PROJECTED_OUT: Star projected nullable property can't be set to null
- [`KT-40480`](https://youtrack.jetbrains.com/issue/KT-40480) [FIR] Support `hasStableParameterName` from metadata
- [`KT-50134`](https://youtrack.jetbrains.com/issue/KT-50134) NI: Type inference regression in java streams groupingBy
- [`KT-50160`](https://youtrack.jetbrains.com/issue/KT-50160) False positive "USELESS_CAST" caused by indexed access operator
- [`KT-42715`](https://youtrack.jetbrains.com/issue/KT-42715) Unable to use implicit lambda param `it` for overloaded methods
- [`KT-49045`](https://youtrack.jetbrains.com/issue/KT-49045) False positive USELESS_CAST in generic type with nullable type parameter
- [`KT-49024`](https://youtrack.jetbrains.com/issue/KT-49024) AssertionError: Variance conflict: type parameter variance 'out' and projection kind 'in' cannot be combined
- [`KT-48975`](https://youtrack.jetbrains.com/issue/KT-48975) Type mismatch: inferred type is X but Nothing! was expected with the AssertJ latest version
- [`KT-47870`](https://youtrack.jetbrains.com/issue/KT-47870) INVISIBLE_MEMBER: Kotlin class can't access protected annotation defined in Java parent class
- [`KT-47495`](https://youtrack.jetbrains.com/issue/KT-47495) ReenteringLazyValueComputationException on invalid code
- [`KT-47490`](https://youtrack.jetbrains.com/issue/KT-47490) Missed diagnostic for incorrect callable reference in finally
- [`KT-47484`](https://youtrack.jetbrains.com/issue/KT-47484) "Recursion detected in a lazy value under LockBasedStorageManager" on invalid code
- [`KT-46301`](https://youtrack.jetbrains.com/issue/KT-46301) Combining branches with sealed interfaces in `when` breaks type inference
- [`KT-44392`](https://youtrack.jetbrains.com/issue/KT-44392) False negative: redundant nullability/not null check on cast with as operator
- [`KT-43936`](https://youtrack.jetbrains.com/issue/KT-43936) Recursion detected on input: ANNOTATION_ENTRY with annotation on star-imported nested class
- [`KT-43846`](https://youtrack.jetbrains.com/issue/KT-43846) No smart cast when returning function closures with captured smart-cast variable
- [`KT-43603`](https://youtrack.jetbrains.com/issue/KT-43603) False positive USELESS_CAST leads to TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM on "when" with smart cast and extension property
- [`KT-43553`](https://youtrack.jetbrains.com/issue/KT-43553) JVM / IR: "AssertionError: Unbound symbols not allowed" caused by annotation class with TYPE target
- [`KT-42169`](https://youtrack.jetbrains.com/issue/KT-42169) False negative CAST_NEVER_SUCCEEDS for incompatible types with generic parameter and star projection
- [`KT-42136`](https://youtrack.jetbrains.com/issue/KT-42136) NI: False positive [USELESS_CAST] when list serves to create a mutable list
- [`KT-41721`](https://youtrack.jetbrains.com/issue/KT-41721) SAM conversion fails on varargs with type approximated to Nothing
- [`KT-38288`](https://youtrack.jetbrains.com/issue/KT-38288) Unresolved reference for type parameter upper bound of nested class when outer class extends it with star projected type argument
- [`KT-37490`](https://youtrack.jetbrains.com/issue/KT-37490) NULL_FOR_NONNULL_TYPE: "Null can not be a value of a non-null type Nothing" when null is passed to nullable argument of type projected method
- [`KT-37365`](https://youtrack.jetbrains.com/issue/KT-37365) NPE from `ReflectionReferencesGenerator.generateCallableReference` with inner class function reference and wrong parenthesis
- [`KT-36958`](https://youtrack.jetbrains.com/issue/KT-36958) NI: missed unresolved on parenthesized callable reference passing through call (back-ends throw an exception)
- [`KT-30756`](https://youtrack.jetbrains.com/issue/KT-30756) No smartcast if elvis operator as a smartcast source in while or do-while is used as the last statement
- [`KT-24737`](https://youtrack.jetbrains.com/issue/KT-24737) Report an error on invalid this-expression with angle brackets on left-hand side of a callable reference
- [`KT-21463`](https://youtrack.jetbrains.com/issue/KT-21463) Compiler doesn't take into accout a type parameter upper bound if a corresponding type argument is in projection
- [`KT-6822`](https://youtrack.jetbrains.com/issue/KT-6822) Smart cast doesn't work inside local returned expression in lambda
- [`KT-55840`](https://youtrack.jetbrains.com/issue/KT-55840) Inconsistency between members of enums in bytecode between FE 1.0 + JVM IR and FIR + JVM IR
- [`KT-47815`](https://youtrack.jetbrains.com/issue/KT-47815) JVM: "Recursion detected in a lazy value under LockBasedStorageManager" when trying to inherit interface from a class with non-trivial function
- [`KT-17817`](https://youtrack.jetbrains.com/issue/KT-17817) No error reported on invalid LHS for class literal
- [`KT-47373`](https://youtrack.jetbrains.com/issue/KT-47373) Missed diagnostics on/after non-null assertion (!!) on generic class class use with class literal
- [`KT-51143`](https://youtrack.jetbrains.com/issue/KT-51143) Wrong Unit-requiring at if/when branch with stub types
- [`KT-53671`](https://youtrack.jetbrains.com/issue/KT-53671) False-positive diagnostic reported on OptIn annotation import from root package
- [`KT-53494`](https://youtrack.jetbrains.com/issue/KT-53494) Mistaken type inference in compound 'if' expression with nullability check and covariant type
- [`KT-28668`](https://youtrack.jetbrains.com/issue/KT-28668) "AssertionError: Unrelated types in SAM conversion for index variable" if lambda argument of '[...]' in LHS of augmented assignment is used as an implementation for different SAM interfaces
- [`KT-55931`](https://youtrack.jetbrains.com/issue/KT-55931) Inference for callable reference inside synthetic calls for if/when/try/etc stops working when brought into lambda for a call
- [`KT-20223`](https://youtrack.jetbrains.com/issue/KT-20223) Inline access check ignores operator calls to `invoke()`
- [`KT-54478`](https://youtrack.jetbrains.com/issue/KT-54478) `@NoInfer` causes CONFLICTING_OVERLOADS
- [`KT-56472`](https://youtrack.jetbrains.com/issue/KT-56472) K2: Add stack of all FIR elements to CheckerContext
- [`KT-41126`](https://youtrack.jetbrains.com/issue/KT-41126) [FIR] Inconsistency of a compiler behaviour at init block for an enum entry with and without a qualifier name
- [`KT-54931`](https://youtrack.jetbrains.com/issue/KT-54931) Annotations defined in nested classes cannot be instantiated directly
- [`KT-52338`](https://youtrack.jetbrains.com/issue/KT-52338) "IncompatibleClassChangeError: Expected non-static field" with Kotlin class with same-named companion object property as base Java class field
- [`KT-24901`](https://youtrack.jetbrains.com/issue/KT-24901) No smart cast for `when` with early return
- [`KT-53086`](https://youtrack.jetbrains.com/issue/KT-53086) "Cannot access '<init>' before superclass constructor has been called" with inner class secondary constructor
- [`KT-55137`](https://youtrack.jetbrains.com/issue/KT-55137) Callable references with conversion are incorrectly allowed to be promoted to KFunction
- [`KT-30497`](https://youtrack.jetbrains.com/issue/KT-30497) EXACTLY_ONCE contract doesn't work in a function with `vararg` parameter
- [`KT-47074`](https://youtrack.jetbrains.com/issue/KT-47074) Front-end Internal error: Failed to analyze declaration State / java.lang.IllegalStateException: Should not be called! when try to add Parcelize
- [`KT-24503`](https://youtrack.jetbrains.com/issue/KT-24503) Return-as-expression is allowed as this/super constructor parameter
- [`KT-55379`](https://youtrack.jetbrains.com/issue/KT-55379) False positive NO_ELSE_IN_WHEN with smartcast to Boolean
- [`KT-47750`](https://youtrack.jetbrains.com/issue/KT-47750) False positive NO_ELSE_IN_WHEN in presence of smartcast to sealed interface
- [`KT-53819`](https://youtrack.jetbrains.com/issue/KT-53819) False positive UNINITIALIZED_VARIABLE with secondary constructor and custom property getter in local class
- [`KT-56457`](https://youtrack.jetbrains.com/issue/KT-56457) JVM: Enum.entries are not annotated with `@NotNull`
- [`KT-56072`](https://youtrack.jetbrains.com/issue/KT-56072) K2. "IllegalStateException: Fir2IrSimpleFunctionSymbol for <paramName> is already bound" when trying to access java synthetic property of inherited class
- [`KT-50082`](https://youtrack.jetbrains.com/issue/KT-50082) Kotlin non-overriding property of subclass doesn't shadow same-named Java field from base class
- [`KT-55822`](https://youtrack.jetbrains.com/issue/KT-55822) False positive ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED with raw types and mixed overridden members
- [`KT-55666`](https://youtrack.jetbrains.com/issue/KT-55666) K2: label on local function is rejected: "Target label does not denote a function"
- [`KT-56283`](https://youtrack.jetbrains.com/issue/KT-56283) False-positive INVISIBLE_MEMBER on overridden member of more specific type after smart cast
- [`KT-51969`](https://youtrack.jetbrains.com/issue/KT-51969) [FIR] Compilation for expect value class fails with "Fir2IrSimpleFunctionSymbol for [declaration] is already bound"
- [`KT-56329`](https://youtrack.jetbrains.com/issue/KT-56329) K2: compiler backend crash on two expected functions with similar signatures
- [`KT-56361`](https://youtrack.jetbrains.com/issue/KT-56361) K2/MPP: receiver isn't available in lambda literals with receiver
- [`KT-55295`](https://youtrack.jetbrains.com/issue/KT-55295) K2/MPP: JS build functionality
- [`KT-55909`](https://youtrack.jetbrains.com/issue/KT-55909) [K2/N] IndexOutOfBoundsException for a reference to a function defined in companion object superclass
- [`KT-55664`](https://youtrack.jetbrains.com/issue/KT-55664) K2: eliminate ClassId.isSame call from FirClass.isSubclassOf
- [`KT-56353`](https://youtrack.jetbrains.com/issue/KT-56353) K2. False negative "Unresolved reference" in default value of secondary constructor's parameter
- [`KT-56381`](https://youtrack.jetbrains.com/issue/KT-56381) K2: Function type kind not extracted from lambda literal in generic call
- [`KT-55747`](https://youtrack.jetbrains.com/issue/KT-55747) K2. "Convention for 'mod' is forbidden. Use 'rem'" error is missing
- [`KT-56104`](https://youtrack.jetbrains.com/issue/KT-56104) Unnecessary inner classes attributes in class files for subclasses
- [`KT-55570`](https://youtrack.jetbrains.com/issue/KT-55570) K2: ACTUAL_WITHOUT_EXPECT error is not reported on a simple actual class
- [`KT-56176`](https://youtrack.jetbrains.com/issue/KT-56176) [K2/N] "IllegalStateException: actual type is kotlin.Int, expected kotlin.Long" when expected type uses typealias
- [`KT-56229`](https://youtrack.jetbrains.com/issue/KT-56229) K2: IllegalStateException (already bound) for triangle-like dependencies scheme with MPP scenario
- [`KT-56199`](https://youtrack.jetbrains.com/issue/KT-56199) K2 + MPP + kotlinx.serialization: java.lang.VerifyError: Bad type on operand stack in aaload
- [`KT-56212`](https://youtrack.jetbrains.com/issue/KT-56212) K2: Exception when compiling extension function declaration with illegally chained type parameter receiver
- [`KT-55503`](https://youtrack.jetbrains.com/issue/KT-55503) K2: "Argument type mismatch" caused by using the wrong "this"
- [`KT-56050`](https://youtrack.jetbrains.com/issue/KT-56050) K2: inconsistency regarding visibility of synthetic properties with protected getter and public setter
- [`KT-49663`](https://youtrack.jetbrains.com/issue/KT-49663) FIR: Support `@kotlin`.jvm.PurelyImplements for java collections
- [`KT-55468`](https://youtrack.jetbrains.com/issue/KT-55468) [K2/N] Crash with debuginfo caused by changed tree using IMPLICIT_COERCION_TO_UNIT
- [`KT-56269`](https://youtrack.jetbrains.com/issue/KT-56269) [K2/N] Don't test "Tailrec is not allowed on open members" in K2
- [`KT-54647`](https://youtrack.jetbrains.com/issue/KT-54647) K2: Function call with Lambda on LHS of assignment leads to KotlinExceptionWithAttachments: FirDeclaration was not found for class org.jetbrains.kotlin.psi.KtFunctionLiteral
- [`KT-54648`](https://youtrack.jetbrains.com/issue/KT-54648) K2: Function call on left side of erroneous assignment isn't resolved
- [`KT-55699`](https://youtrack.jetbrains.com/issue/KT-55699) K2. False Negative "Type parameter T is not an expression"
- [`KT-56132`](https://youtrack.jetbrains.com/issue/KT-56132) Restore 'JvmBackendContext' constructor signature for compatibility
- [`KT-55973`](https://youtrack.jetbrains.com/issue/KT-55973) K2: Exception from UnusedChecker on an unused destructuring
- [`KT-56275`](https://youtrack.jetbrains.com/issue/KT-56275) K2 IDE: Missed error for enum super type
- [`KT-54775`](https://youtrack.jetbrains.com/issue/KT-54775) K2. "IllegalStateException: Expected FirResolvedTypeRef with ConeKotlinType but was FirImplicitTypeRefImpl <implicit>" exception on incorrect code
- [`KT-55528`](https://youtrack.jetbrains.com/issue/KT-55528) K2: CFA for property initialization analysis is not run for class initialization graphs
- [`KT-54410`](https://youtrack.jetbrains.com/issue/KT-54410) K2: Deprecation warning instead of "this declaration is only available since Kotlin X" when language version in project are below required to use language feature
- [`KT-55186`](https://youtrack.jetbrains.com/issue/KT-55186) K2: No compilation error on calling exception without constructor
- [`KT-36776`](https://youtrack.jetbrains.com/issue/KT-36776) Treat special constructions (if, when, try) as a usual calls when there is expected type
- [`KT-50947`](https://youtrack.jetbrains.com/issue/KT-50947) False negative: FE 1.0 doesn't report type variance conflict error on an inner type
- [`KT-39041`](https://youtrack.jetbrains.com/issue/KT-39041) Collection literals should not be allowed inside annotation classes
- [`KT-54694`](https://youtrack.jetbrains.com/issue/KT-54694) Consider enabling BooleanElvisBoundSmartCasts in K1 or K2
- [`KT-54587`](https://youtrack.jetbrains.com/issue/KT-54587) K2. CCE on compilation when some operator fun is needed and it is implemented as an extension function for another class
- [`KT-52774`](https://youtrack.jetbrains.com/issue/KT-52774) Resolve unqualified enum constants based on expected type

### Docs & Examples

- [`KT-53643`](https://youtrack.jetbrains.com/issue/KT-53643) Update coding style conventions to include rangeUntil operator
- [`KT-57902`](https://youtrack.jetbrains.com/issue/KT-57902) Create migration tutorial from kotlin-js to kotlin-multiplatform gradle plugin
- [`KT-58381`](https://youtrack.jetbrains.com/issue/KT-58381) [Docs][Libraries] Document Path.createParentDirectories

### IDE

#### Performance Improvements

- [`KTIJ-23501`](https://youtrack.jetbrains.com/issue/KTIJ-23501) Make main run configuration detection lighter
- [`KT-56613`](https://youtrack.jetbrains.com/issue/KT-56613) Reduce memory consumption of light classes

#### Fixes

- [`KTIJ-25855`](https://youtrack.jetbrains.com/issue/KTIJ-25855) Infinite "Analyzing..." on .kt files from native source-set
- [`KTIJ-25448`](https://youtrack.jetbrains.com/issue/KTIJ-25448) When project JDK is less than one defines in jvmToolchain block run with Idea fails with `has been compiled by a more recent version of the Java Runtime`
- [`KTIJ-25673`](https://youtrack.jetbrains.com/issue/KTIJ-25673) Temporarily switch off adding jvmToolchain to new/converted Kotlin projects
- [`KTIJ-25719`](https://youtrack.jetbrains.com/issue/KTIJ-25719) K/Wasm: Fix import of wasm projects with Kotlin 1.9.0
- [`KTIJ-24136`](https://youtrack.jetbrains.com/issue/KTIJ-24136) Files with actual declarations are not highlighted in kotlinx.coroutines project
- [`KTIJ-24390`](https://youtrack.jetbrains.com/issue/KTIJ-24390) Kotlin assignment plugin: Imports are not recognized in build logic .kt files for Gradle build
- [`KTIJ-24695`](https://youtrack.jetbrains.com/issue/KTIJ-24695) Default Kotlin project can not be synced on Mac M1
- [`KTIJ-25302`](https://youtrack.jetbrains.com/issue/KTIJ-25302) Wrong variant of serialization plugins is passed to JPS build if the project was imported from Gradle
- [`KTIJ-25416`](https://youtrack.jetbrains.com/issue/KTIJ-25416) "Add import" works incorrectly without formatting the code for root package
- [`KT-57849`](https://youtrack.jetbrains.com/issue/KT-57849) K2: contract violation due to implicit java type with annotation
- [`KTIJ-24666`](https://youtrack.jetbrains.com/issue/KTIJ-24666) Store annotation arguments in cls
- [`KTIJ-24667`](https://youtrack.jetbrains.com/issue/KTIJ-24667) Store constant expressions in cls
- [`KTIJ-24665`](https://youtrack.jetbrains.com/issue/KTIJ-24665) Store contracts in cls
- [`KTIJ-24543`](https://youtrack.jetbrains.com/issue/KTIJ-24543) An option to configure kotlin with JavaScript is available at Tools -> Kotlin menu
- [`KT-57857`](https://youtrack.jetbrains.com/issue/KT-57857) LC: FakeFileForLightClass: Read access is allowed from inside read-action
- [`KTIJ-25172`](https://youtrack.jetbrains.com/issue/KTIJ-25172) Store flexible types in cls
- [`KT-57578`](https://youtrack.jetbrains.com/issue/KT-57578) SLC: incorrect upper bound wildcards
- [`KT-57917`](https://youtrack.jetbrains.com/issue/KT-57917) Analysis API: decompiled value parameters are not resolved
- [`KT-56046`](https://youtrack.jetbrains.com/issue/KT-56046) K2 IDE: Avoid redundant resolve from annotations
- [`KT-57569`](https://youtrack.jetbrains.com/issue/KT-57569) SLC: incorrect visibility for lateinit var with private setter
- [`KT-57547`](https://youtrack.jetbrains.com/issue/KT-57547) SLC: non-last `vararg` value parameter type mismatch
- [`KTIJ-25034`](https://youtrack.jetbrains.com/issue/KTIJ-25034) K2 IDE: unresolved import treated as unused
- [`KT-57548`](https://youtrack.jetbrains.com/issue/KT-57548) SLC: incorrect inheritance list for Comparator
- [`KT-56843`](https://youtrack.jetbrains.com/issue/KT-56843) Light classes: certain kinds of constant values in property initializers aren't supported
- [`KTIJ-24676`](https://youtrack.jetbrains.com/issue/KTIJ-24676) Enum.entries is red if it's called on enum class from JDK or module without stdlib in dependencies
- [`KT-56868`](https://youtrack.jetbrains.com/issue/KT-56868) SLC: IncorrectOperationException on enum annotation arguments that are not valid Java identifiers
- [`KTIJ-25048`](https://youtrack.jetbrains.com/issue/KTIJ-25048) K2: IDE K2: "Unexpected scope FirNameAwareCompositeScope"
- [`KTIJ-24895`](https://youtrack.jetbrains.com/issue/KTIJ-24895) K2 IDE: "Invalid FirDeclarationOrigin Synthetic" exception for synthetic "WHEN_CALL" function during highlighting
- [`KT-56833`](https://youtrack.jetbrains.com/issue/KT-56833) Light classes: Accessors to lateinit properties don't have `@NotNull` annotations
- [`KTIJ-24768`](https://youtrack.jetbrains.com/issue/KTIJ-24768) IDE K2: "IllegalArgumentException Failed requirement at FirJvmTypeMapper"
- [`KT-56845`](https://youtrack.jetbrains.com/issue/KT-56845) Light classes: Overridden property accessors don't have `@Override` annotation
- [`KT-56441`](https://youtrack.jetbrains.com/issue/KT-56441) K2 IDE: reference from Java to ObjectName.INSTANCE of private object is red in IDE, but compiled successfully
- [`KT-56891`](https://youtrack.jetbrains.com/issue/KT-56891) Symbol Classes: DefaultImpls classes contain methods without default implementation
- [`KTIJ-24742`](https://youtrack.jetbrains.com/issue/KTIJ-24742) K2 IDE: InvalidFirElementTypeException
- [`KTIJ-24067`](https://youtrack.jetbrains.com/issue/KTIJ-24067) K2 IDE: references from .java source to top-level Kotlin members from dependencies are unresolved
- [`KT-56842`](https://youtrack.jetbrains.com/issue/KT-56842) Light Classes: Primitive-backed context receiver parameters shouldn't be marked with `@NotNull`
- [`KT-56835`](https://youtrack.jetbrains.com/issue/KT-56835) Light classes: Underlying fields for delegated properties should be marked as final and `@NotNull`
- [`KT-56840`](https://youtrack.jetbrains.com/issue/KT-56840) Light Classes: Inline classes backed by Java primitives shouldn't be marked with `@NotNull`
- [`KT-56728`](https://youtrack.jetbrains.com/issue/KT-56728) K2 IDE. False positive `not applicable to` for kotlin annotation with target annotating Java element
- [`KTIJ-24610`](https://youtrack.jetbrains.com/issue/KTIJ-24610) K2: Exception from import optimizer: "Unexpected qualifier '10' of type 'KtConstantExpression`"
- [`KTIJ-24476`](https://youtrack.jetbrains.com/issue/KTIJ-24476) Make application services to be classes instead of objects
- [`KTIJ-24574`](https://youtrack.jetbrains.com/issue/KTIJ-24574) K2 IDE: "No fir element was found for" from inspections
- [`KT-55815`](https://youtrack.jetbrains.com/issue/KT-55815) SLC: Keep annotations on type when converting to `PsiType`
- [`KT-55669`](https://youtrack.jetbrains.com/issue/KT-55669) K2 IDE: INRE from light classes
- [`KTIJ-24530`](https://youtrack.jetbrains.com/issue/KTIJ-24530) K2 IDE: status resolution fails with CCE
- [`KTIJ-23937`](https://youtrack.jetbrains.com/issue/KTIJ-23937) K2: No 'JavaSymbolProvider' in array owner: Source session for module <Non under content root module>
- [`KTIJ-24087`](https://youtrack.jetbrains.com/issue/KTIJ-24087) K2: Type parameter bounds found not analyzed during call inference
- [`KTIJ-24344`](https://youtrack.jetbrains.com/issue/KTIJ-24344) K2 IDE: Object reference incorrectly resolves to 'invoke' operator function
- [`KTIJ-24107`](https://youtrack.jetbrains.com/issue/KTIJ-24107) K2 IDE: Unresolved call for qualified companion invoke()
- [`KTIJ-24385`](https://youtrack.jetbrains.com/issue/KTIJ-24385) K2 IDE:  KtCallExpression(KtCallExpression) should always resolve to a KtCallInfo

### IDE. Code Style, Formatting

- [`KTIJ-24928`](https://youtrack.jetbrains.com/issue/KTIJ-24928) Organize imports puts imports in one line if Formatter option is used

### IDE. Completion

- [`KTIJ-25108`](https://youtrack.jetbrains.com/issue/KTIJ-25108) K2 IDE: Code completion in Java context for Kotlin top level members: "Slow operations are prohibited on EDT" through KtAnalysisScopeProviderImpl.canBeAnalysed()
- [`KTIJ-24989`](https://youtrack.jetbrains.com/issue/KTIJ-24989) K2 IDE: completion should show information about expanded types for type alias parameters
- [`KTIJ-24992`](https://youtrack.jetbrains.com/issue/KTIJ-24992) K2 IDE: completion shows return type with incorrect nullability when type is stub type
- [`KTIJ-24948`](https://youtrack.jetbrains.com/issue/KTIJ-24948) K2 IDE: "ERROR: class org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef cannot be cast to class org.jetbrains.kotlin.fir.types.FirResolvedTypeRef" during completion of inner classes' constructors
- [`KTIJ-24256`](https://youtrack.jetbrains.com/issue/KTIJ-24256) K2 IDE: Angle brackets are missing in completion of function with type parameters
- [`KTIJ-24083`](https://youtrack.jetbrains.com/issue/KTIJ-24083) K2, Completion: Exception on adding a type parameter receiver to a function
- [`KTIJ-23963`](https://youtrack.jetbrains.com/issue/KTIJ-23963) K2 IDE: Completion in Kotlin suggests overriding a Java field; fails with "Unknown member to override"
- [`KTIJ-22359`](https://youtrack.jetbrains.com/issue/KTIJ-22359) K2 IDE: no completion for Java synthetic properties from super class
- [`KTIJ-23880`](https://youtrack.jetbrains.com/issue/KTIJ-23880) Completion doesn't work for Java synthetic property reference

### IDE. Decompiler, Indexing, Stubs

- [`KTIJ-25152`](https://youtrack.jetbrains.com/issue/KTIJ-25152) Kotlin Bytecode tool window: ISE Symbol with IrSimpleFunctionSymbolImpl is unbound on actual callable with argument in mpp project with IR
- [`KTIJ-24475`](https://youtrack.jetbrains.com/issue/KTIJ-24475) Migrate kotlin index extension implementation from object to classes
- [`KTIJ-24335`](https://youtrack.jetbrains.com/issue/KTIJ-24335) Kotlin Bytecode tool window: NoSuchElementException caused by duplicate Boolean type descriptor in JvmSharedVariablesManager.getProvider
- [`KTIJ-24206`](https://youtrack.jetbrains.com/issue/KTIJ-24206) Kotlin Bytecode tool window: "Unhandled intrinsic in ExpressionCodegen" when compiling a source file with an expect function
- [`KTIJ-15764`](https://youtrack.jetbrains.com/issue/KTIJ-15764) IR by default in Kotlin bytecode tool window

### IDE. Gradle Integration

- [`KT-59034`](https://youtrack.jetbrains.com/issue/KT-59034) MPP build failed with "Factory type is not known for plugin variants" in kt-231-*
- [`KT-56671`](https://youtrack.jetbrains.com/issue/KT-56671) KGP import: K/N distribution libraries should'n be resolved for IDE by KGP with package names
- [`KTIJ-24573`](https://youtrack.jetbrains.com/issue/KTIJ-24573) KGP-based import: commonized cinterop libraries don't include source set targets, unsupported by host
- [`KTIJ-25757`](https://youtrack.jetbrains.com/issue/KTIJ-25757) KJS: 1.9.0-Beta fails to run when running in Android Studio
- [`KTIJ-24701`](https://youtrack.jetbrains.com/issue/KTIJ-24701) KGP import: JVM+Android shared source sets don't receive a correct default stdlib dependency
- [`KTIJ-24745`](https://youtrack.jetbrains.com/issue/KTIJ-24745) IDE sync of kotlin("js") projects with js(BOTH) set fails with exception because of the new MPP IDE import
- [`KTIJ-24567`](https://youtrack.jetbrains.com/issue/KTIJ-24567) Enable KGP dependency resolution by default
- [`KTIJ-24729`](https://youtrack.jetbrains.com/issue/KTIJ-24729) KotlinMPPGradleProjectResolverKt must not be requested from main classloader on project import
- [`KTIJ-11978`](https://youtrack.jetbrains.com/issue/KTIJ-11978) IDE does not recognize the sources JAR of a published to mavenLocal MPP library

### IDE. Inspections and Intentions

- [`KTIJ-24684`](https://youtrack.jetbrains.com/issue/KTIJ-24684) K2 IDE: 'Redundant qualifier name' false positive in type specification in extension function
- [`KTIJ-24662`](https://youtrack.jetbrains.com/issue/KTIJ-24662) K2 IDE: False positive "Redundant qualifier name" inspection for nested class from base interface
- [`KTIJ-25232`](https://youtrack.jetbrains.com/issue/KTIJ-25232) K2 IDE: "Redundant qualifier name" inspection false positive with object referenced via property
- [`KTIJ-25447`](https://youtrack.jetbrains.com/issue/KTIJ-25447) Make Enum.entries and RangeUntil inspections don't check for opt-in when APIs become stable
- [`KTIJ-23588`](https://youtrack.jetbrains.com/issue/KTIJ-23588) K2 IDE. False positive unused import directive for extension function of an object
- [`KTIJ-25112`](https://youtrack.jetbrains.com/issue/KTIJ-25112) K2 IDE: False positive "Actual value of parameter 'b' is always 'null'"
- [`KTIJ-24485`](https://youtrack.jetbrains.com/issue/KTIJ-24485) Explicit API mode: false positive "redundant 'public' modifier"
- [`KTIJ-24453`](https://youtrack.jetbrains.com/issue/KTIJ-24453) Unsuccessfull resolve error from OperatorToFunctionIntention on recursive property declaration

### IDE. JS

- [`KT-58427`](https://youtrack.jetbrains.com/issue/KT-58427) Kotlin Gradle Plugin ignores language version value for Kotlin/JS sources

### IDE. Libraries

- [`KTIJ-25096`](https://youtrack.jetbrains.com/issue/KTIJ-25096) K2 IDE: Library sessions are garbage collected between performance test runs with enabled library caches
- [`KTIJ-24413`](https://youtrack.jetbrains.com/issue/KTIJ-24413) Cannot navigate to enum of the Kotlin library via entries call

### IDE. Misc

- [`KT-58763`](https://youtrack.jetbrains.com/issue/KT-58763) K2 IDE: NoSuchMethodError: KtPsiFactory$Companion.contextual
- [`KTIJ-25304`](https://youtrack.jetbrains.com/issue/KTIJ-25304) Move IDE Extension Points from compiler.xml to the IDE repository
- [`KTIJ-24893`](https://youtrack.jetbrains.com/issue/KTIJ-24893) K2 IDE: Serializable plugin causes infinite resolve recursion

### IDE. Multiplatform

- [`KTIJ-25859`](https://youtrack.jetbrains.com/issue/KTIJ-25859) MPP: Library wizard with android target uses deprecated `androidTest` source set
- [`KTIJ-25479`](https://youtrack.jetbrains.com/issue/KTIJ-25479) Compiler options is not imported correctly for js source sets
- [`KTIJ-24011`](https://youtrack.jetbrains.com/issue/KTIJ-24011) MPP: Native tests are missing run gutters

### IDE. Navigation

- [`KTIJ-23073`](https://youtrack.jetbrains.com/issue/KTIJ-23073) K2 IDE: Navigation doesn't work from library sources to another library sources
- [`KTIJ-24819`](https://youtrack.jetbrains.com/issue/KTIJ-24819) K2 IDE: support compiler reference index
- [`KTIJ-24697`](https://youtrack.jetbrains.com/issue/KTIJ-24697) K2 IDE: handling PresistenceMap in LLFirSessionsCache takes a lot of time
- [`KTIJ-24373`](https://youtrack.jetbrains.com/issue/KTIJ-24373) K2 IDE: Qualified generic type is not fully resolved

### IDE. Run Configurations

- [`KTIJ-25046`](https://youtrack.jetbrains.com/issue/KTIJ-25046) K2 IDE. Junit5 test method with internal modifier can't be launched using gutter

### IDE. Wizards

- [`KTIJ-25932`](https://youtrack.jetbrains.com/issue/KTIJ-25932) Turn off the adding new targets in Kotlin Multiplatform Wizard
- [`KTIJ-26022`](https://youtrack.jetbrains.com/issue/KTIJ-26022) Project module not selected in a second page of "New Project" window in KMP wizard
- [`KTIJ-24834`](https://youtrack.jetbrains.com/issue/KTIJ-24834) Set default for "kotlin.mpp.experimental" flag for K/JS features to true

### JavaScript

#### New Features

- [`KT-12784`](https://youtrack.jetbrains.com/issue/KT-12784) JS: generate ES2015 compatible modules
- [`KT-48154`](https://youtrack.jetbrains.com/issue/KT-48154) KJS / IR: Inline members support for external types
- [`KT-51582`](https://youtrack.jetbrains.com/issue/KT-51582) FIR: support basic compile-time evaluation for JS backend

#### Fixes

- [`KT-43490`](https://youtrack.jetbrains.com/issue/KT-43490) KJS / IR: "Cannot set property message of Error which has only a getter" caused by class that is child of Throwable
- [`KT-57690`](https://youtrack.jetbrains.com/issue/KT-57690) K2/MPP: compileProductionLibraryKotlinJs fails with Module has a reference to symbol kotlin/arrayOf|3204918726020768747[0]. Neither the module itself nor its dependencies contain such declaration
- [`KT-56911`](https://youtrack.jetbrains.com/issue/KT-56911) K2/MPP: Compile K/JS fails for `@Serializable` annotation with class IrDeclarationOrigin$GeneratedByPlugin cannot be cast to class IrDeclarationOriginImpl
- [`KT-56950`](https://youtrack.jetbrains.com/issue/KT-56950) Support KLIB IC with K2
- [`KT-58570`](https://youtrack.jetbrains.com/issue/KT-58570) KJS: ES6 classes + PL throw java.lang.NullPointerException
- [`KT-58835`](https://youtrack.jetbrains.com/issue/KT-58835) K2/JS: Fix incremental compilation klib tests
- [`KT-58794`](https://youtrack.jetbrains.com/issue/KT-58794) KJS / K2: Assertion failed with Space build
- [`KT-51706`](https://youtrack.jetbrains.com/issue/KT-51706) Partial linkage: in case of absent symbol referred from declaration Native compiler is successful, JavaScript fails
- [`KT-54452`](https://youtrack.jetbrains.com/issue/KT-54452) Kotlin/JS libraries with "joined" legacy+IR content: publish IR sources for them
- [`KT-53180`](https://youtrack.jetbrains.com/issue/KT-53180) Kotlin/JS: generated TypeScript constructor can have "TS1016: A required parameter cannot follow an optional parameter" error with certain properties order
- [`KT-39650`](https://youtrack.jetbrains.com/issue/KT-39650) KJS IR: provide a way to enable ES2015 class generation
- [`KT-57990`](https://youtrack.jetbrains.com/issue/KT-57990) KJS/IR. Invalid `super` call for final parent methods (ES classes)
- [`KT-58246`](https://youtrack.jetbrains.com/issue/KT-58246) KJS: ES15 classses — duplicated code in class constructor
- [`KT-57479`](https://youtrack.jetbrains.com/issue/KT-57479) KJS: Add an annotation for a function parameter which checks that a passed argument has an external type
- [`KT-58201`](https://youtrack.jetbrains.com/issue/KT-58201) Unknown statement type when building with ES modules
- [`KT-30810`](https://youtrack.jetbrains.com/issue/KT-30810) values and valueOf are miscompiled for external enum classes
- [`KT-57024`](https://youtrack.jetbrains.com/issue/KT-57024) Ugly TypeScript definitions for declarations with both `@JsExport` and `@Serializable`
- [`KT-56237`](https://youtrack.jetbrains.com/issue/KT-56237) KJS + IC: Adding or removing interface default implementation doesn't invalidate children and doesn't update JS code
- [`KT-54638`](https://youtrack.jetbrains.com/issue/KT-54638) K2/JS: Fir2ir - implement and use JS-specific mangler
- [`KT-54028`](https://youtrack.jetbrains.com/issue/KT-54028) Native / JS: Using private object implementing a sealed interface causes a linker error
- [`KT-57423`](https://youtrack.jetbrains.com/issue/KT-57423) KJS: Add an annotation for external interfaces which allows to be inherited only by other external interfaces, classes or objects
- [`KT-57711`](https://youtrack.jetbrains.com/issue/KT-57711) K2: Native & JS fail to compile a KLIB that uses const val from a dependency KLIB
- [`KT-57078`](https://youtrack.jetbrains.com/issue/KT-57078) JS IC: Unbound symbol left in `SymbolTable` in `JsIr[ES6]InvalidationTestGenerated.testBreakKlibBinaryCompatibilityWithVariance` tests
- [`KT-57254`](https://youtrack.jetbrains.com/issue/KT-57254) Deprecate `external enum` declarations
- [`KT-57002`](https://youtrack.jetbrains.com/issue/KT-57002) KJS: "JsParserException: missing name after . operator" when a js(...) block contains an interpolated constant
- [`KT-56961`](https://youtrack.jetbrains.com/issue/KT-56961) JS IR: serializedIrFileFingerprints in klib manifest has a wrong format
- [`KT-56282`](https://youtrack.jetbrains.com/issue/KT-56282) KJS: Invalidate incremental cache in case of compiler internal errors

### KMM Plugin

- [`KT-55402`](https://youtrack.jetbrains.com/issue/KT-55402) "Framework not found SQLCipher": after selection of "Regular framework" as "iOS framework distribution" and installing SqlCihper through CocoaPods
- [`KT-55988`](https://youtrack.jetbrains.com/issue/KT-55988) KN debugger in KMM plugin for Android Studio can't recognize the source code

### Language Design

#### New Features

- [`KT-48872`](https://youtrack.jetbrains.com/issue/KT-48872) Provide modern and performant replacement for Enum.values()
- [`KT-15613`](https://youtrack.jetbrains.com/issue/KT-15613) Introduce special syntax for the until operator
- [`KT-4107`](https://youtrack.jetbrains.com/issue/KT-4107) Design and implement a solution for toString, equals and hashCode on objects (data object)

#### Fixes

- [`KT-28850`](https://youtrack.jetbrains.com/issue/KT-28850) Prohibit protected visibility in final expected classes
- [`KT-39362`](https://youtrack.jetbrains.com/issue/KT-39362) Expect fun interface must have actual fun interface counterpart
- [`KT-48994`](https://youtrack.jetbrains.com/issue/KT-48994) Prohibit type unsound java fields assignments
- [`KT-58791`](https://youtrack.jetbrains.com/issue/KT-58791) Prolongate PROGRESSION_CHANGE_RESOLVE diagnostics to 2.x
- [`KT-53778`](https://youtrack.jetbrains.com/issue/KT-53778) Release stdlib API about "rangeUntil" operator in 1.9
- [`KT-53653`](https://youtrack.jetbrains.com/issue/KT-53653) Export Enum.entries to Objective-C and Swift
- [`KT-55177`](https://youtrack.jetbrains.com/issue/KT-55177) Deprecate declaration of expect and actual counterparts of same class in one module
- [`KT-49110`](https://youtrack.jetbrains.com/issue/KT-49110) Prohibit access to members of companion of enum class from initializers of entries of this enum
- [`KT-47986`](https://youtrack.jetbrains.com/issue/KT-47986) Forbid implicit inferring a type variable into an upper bound in the builder inference context
- [`KT-57395`](https://youtrack.jetbrains.com/issue/KT-57395) Delay ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound feature to LV 2.0
- [`KT-55082`](https://youtrack.jetbrains.com/issue/KT-55082) Bump KLib version for Enum.entries
- [`KT-49264`](https://youtrack.jetbrains.com/issue/KT-49264) Deprecate infix function calls of functions named "suspend" with dangling function literal

### Libraries

#### New Features

- [`KT-58046`](https://youtrack.jetbrains.com/issue/KT-58046) Stabilize remaining kotlin.time API: time sources, time marks, measureTime
- [`KT-58074`](https://youtrack.jetbrains.com/issue/KT-58074) Stabilization of Atomics API in K/N
- [`KT-55268`](https://youtrack.jetbrains.com/issue/KT-55268) Mutiplatform `@Volatile` annotation
- [`KT-51908`](https://youtrack.jetbrains.com/issue/KT-51908) Provide common function for getting regex capture group by name
- [`KT-53263`](https://youtrack.jetbrains.com/issue/KT-53263) Path.createParentDirectories
- [`KT-7637`](https://youtrack.jetbrains.com/issue/KT-7637) Add toString() to standard delegates classes (NotNullVar, LazyVal, BlockingLazyVal, ...)
- [`KT-40728`](https://youtrack.jetbrains.com/issue/KT-40728) Add AssertionError constructor with `cause: Throwable` parameter to common stdlib
- [`KT-57298`](https://youtrack.jetbrains.com/issue/KT-57298) Avoid FileAlreadyExistsException from Path.createParentDirectories in case of parent is symlink

#### Performance Improvements

- [`KT-54739`](https://youtrack.jetbrains.com/issue/KT-54739) `build` method in collection builders (Set, Map, List) should return a single instance for empty collections
- [`KT-42589`](https://youtrack.jetbrains.com/issue/KT-42589) Provide common listOf(value) overload to avoid allocation of the vararg
- [`KT-55091`](https://youtrack.jetbrains.com/issue/KT-55091) Stdlib: Sequence.toSet() and Sequence.toList() may create the collection twice
- [`KT-57617`](https://youtrack.jetbrains.com/issue/KT-57617) Optimize ReversedListReadOnly iterator
- [`KT-57607`](https://youtrack.jetbrains.com/issue/KT-57607) KJS: Bad performance for ArrayList.addAll

#### Fixes

- [`KT-58841`](https://youtrack.jetbrains.com/issue/KT-58841) Serialization: NPE when obtaining a serializer of a sealed base class with a self-referencing property
- [`KT-57728`](https://youtrack.jetbrains.com/issue/KT-57728) Explicitly specify level of stability of programmatically-accessible interoperability API
- [`KT-58985`](https://youtrack.jetbrains.com/issue/KT-58985) Update KClass.isData KDoc
- [`KT-57762`](https://youtrack.jetbrains.com/issue/KT-57762) Introduce HexFormat for formatting and parsing hexadecimals
- [`KT-55612`](https://youtrack.jetbrains.com/issue/KT-55612) Stabilize experimental API for 1.9
- [`KT-58548`](https://youtrack.jetbrains.com/issue/KT-58548) Stabilize standard library API for Enum.entries
- [`KT-56400`](https://youtrack.jetbrains.com/issue/KT-56400) Disable compilation of atomicfu-runtime with legacy JS backend
- [`KT-58276`](https://youtrack.jetbrains.com/issue/KT-58276) Deprecate redundant public declarations in kotlin.native.concurrent
- [`KT-35973`](https://youtrack.jetbrains.com/issue/KT-35973) Extract org.w3c declarations from stdlib-js
- [`KT-58073`](https://youtrack.jetbrains.com/issue/KT-58073) JS/Legacy compiler blocks compilation of kotlinx.atomicfu with K2
- [`KT-57317`](https://youtrack.jetbrains.com/issue/KT-57317) Repack EnumEntries from stdlib into the compiler
- [`KT-54702`](https://youtrack.jetbrains.com/issue/KT-54702) Native: mark Worker and related APIs as obsolete
- [`KT-55610`](https://youtrack.jetbrains.com/issue/KT-55610) Deprecate kotlin.jvm.Volatile annotation in platforms except JVM
- [`KT-57404`](https://youtrack.jetbrains.com/issue/KT-57404) Native: Support AnnotationTarget.TYPE_PARAMETER
- [`KT-57318`](https://youtrack.jetbrains.com/issue/KT-57318) Change EnumEntries stdlib implementation to be eager
- [`KT-57137`](https://youtrack.jetbrains.com/issue/KT-57137) Native: Consider removing ArrayAsList
- [`KT-56661`](https://youtrack.jetbrains.com/issue/KT-56661) Missing EnumEntries-related bytecode in kotlin-stdlib-1.9.255-SNAPSHOT.jar
- [`KT-51579`](https://youtrack.jetbrains.com/issue/KT-51579) PlatformImplementations loading is not compatible with graalvm native-image --no-fallback

### Native

- [`KT-54098`](https://youtrack.jetbrains.com/issue/KT-54098) Decommission and remove 'enableEndorsedLibs' flag from Gradle setup
- [`KT-52594`](https://youtrack.jetbrains.com/issue/KT-52594) Provide Alpha support for Native in the K2 platform
- [`KT-56071`](https://youtrack.jetbrains.com/issue/KT-56071) K2/MPP: Native build functionality
- [`KT-56218`](https://youtrack.jetbrains.com/issue/KT-56218) [K2/N] Receiver annotations for properties are not serialized
- [`KT-56326`](https://youtrack.jetbrains.com/issue/KT-56326) [K2/N] RemoveRedundantCallsToStaticInitializersPhase removes important static initializer
- [`KT-27002`](https://youtrack.jetbrains.com/issue/KT-27002) `lateinit` intrinsics frontend checkers aren't applied on Native

### Native. Build Infrastructure

- [`KT-58160`](https://youtrack.jetbrains.com/issue/KT-58160) Native: performance build configuration fails with NoSuchMethodError: 'boolean kotlinx.coroutines.CompletableDeferredKt.completeWith(kotlinx.coroutines.CompletableDeferred, java.lang.Object)'

### Native. C and ObjC Import

- [`KT-54610`](https://youtrack.jetbrains.com/issue/KT-54610) Kotlin Native can't call `objc_direct` functions
- [`KT-57918`](https://youtrack.jetbrains.com/issue/KT-57918) [K2/N] Support typealiases in FirClassSymbol<*>.selfOrAnySuperClass()
- [`KT-58651`](https://youtrack.jetbrains.com/issue/KT-58651) Native c-interop tool generates broken `@Deprecated` annotations
- [`KT-57541`](https://youtrack.jetbrains.com/issue/KT-57541) Compilation fails without explicit cast on cinterop code
- [`KT-54805`](https://youtrack.jetbrains.com/issue/KT-54805) KMP ios memory leak when using CA Layer
- [`KT-57490`](https://youtrack.jetbrains.com/issue/KT-57490) [K/N] Duplicate package names for cinterop klibs with objc protocols fails to link

### Native. ObjC Export

- [`KT-58839`](https://youtrack.jetbrains.com/issue/KT-58839) K/N: Exception during HiddenFromObjC marked class extension function compiling
- [`KT-56464`](https://youtrack.jetbrains.com/issue/KT-56464) K/N: Allow HiddenFromObjC for classes
- [`KT-57507`](https://youtrack.jetbrains.com/issue/KT-57507) K2: Set of Objc exported declarations is different between K1 and K2

### Native. Runtime

- [`KT-58441`](https://youtrack.jetbrains.com/issue/KT-58441) Kotlin/Native: `@ObjCAction` `@ObjCOutlet` generate bridges without switching state
- [`KT-57091`](https://youtrack.jetbrains.com/issue/KT-57091) Align Native and Java file/class initialization behavior

### Native. Runtime. Memory

- [`KT-56233`](https://youtrack.jetbrains.com/issue/KT-56233) [Kotlin/Native] Crash when enum values are accessed in multiple threads
- [`KT-58130`](https://youtrack.jetbrains.com/issue/KT-58130) Implement preview of custom allocator for Kotlin/Native
- [`KT-56402`](https://youtrack.jetbrains.com/issue/KT-56402) Native: if a Kotlin peer for an Obj-C object is created on the main thread, then Kotlin runtime should run objc_release for it on the main thread

### Native. Stdlib

- [`KT-57344`](https://youtrack.jetbrains.com/issue/KT-57344) Try to remove strange .equals overload on primitive types
- [`KT-57592`](https://youtrack.jetbrains.com/issue/KT-57592) Native: Remove the default parameter value for AtomicLong constructor

### Native. Testing

- [`KT-57349`](https://youtrack.jetbrains.com/issue/KT-57349) Enable more K2 MPP codegen/box tests for Kotlin/Native
- [`KT-57026`](https://youtrack.jetbrains.com/issue/KT-57026) K2: Fix Native test infrastructure for MPP

### Reflection

- [`KT-54833`](https://youtrack.jetbrains.com/issue/KT-54833) Reflection: Incorrect behaviour for Field.kotlinProperty function in companion objects
- [`KT-56650`](https://youtrack.jetbrains.com/issue/KT-56650) ArrayStoreException from InlineClassAwareCaller.call with an array of inline class
- [`KT-56093`](https://youtrack.jetbrains.com/issue/KT-56093) Metaspace leak in a Gradle plugin built with Kotlin 1.8.0
- [`KT-55937`](https://youtrack.jetbrains.com/issue/KT-55937) Optimize implementation of kotlinFunction/kotlinProperty

### Specification

- [`KT-58932`](https://youtrack.jetbrains.com/issue/KT-58932) Specify the priority between candidates with type and value receivers when doing callable reference inference
- [`KT-54254`](https://youtrack.jetbrains.com/issue/KT-54254) Specify that an annotation type cannot have itself as a nested element
- [`KT-53427`](https://youtrack.jetbrains.com/issue/KT-53427) Specify `@SubclassOptInRequired`
- [`KT-53323`](https://youtrack.jetbrains.com/issue/KT-53323) Add Enum.entries to Kotlin specification, KEEP-283
- [`KT-54255`](https://youtrack.jetbrains.com/issue/KT-54255) Specify extension receivers are effectively `noinline`
- [`KT-53646`](https://youtrack.jetbrains.com/issue/KT-53646) Incorporate rangeUntil (..<) operator into specification

### Tools. CLI

- [`KT-57495`](https://youtrack.jetbrains.com/issue/KT-57495) Add JVM target bytecode version 20
- [`KT-57154`](https://youtrack.jetbrains.com/issue/KT-57154) Incorrect version of JDK is provided through CoreJrtFs
- [`KT-56209`](https://youtrack.jetbrains.com/issue/KT-56209) Add CLI support for HMPP in K2
- [`KT-58351`](https://youtrack.jetbrains.com/issue/KT-58351) Confusing error message when using removed -Xjvm-default mode value
- [`KT-57535`](https://youtrack.jetbrains.com/issue/KT-57535) K2: Kotlin command line compiler doesn't see class files on the class path in 2.0
- [`KT-57644`](https://youtrack.jetbrains.com/issue/KT-57644) K2: Prohibit passing HMPP module structure with CLI arguments to metadata compiler
- [`KT-56351`](https://youtrack.jetbrains.com/issue/KT-56351) Reduce memory usage spent on compiler settings

### Tools. Commonizer

- [`KT-57796`](https://youtrack.jetbrains.com/issue/KT-57796) NoSuchFileException in :module-B:commonizeCInterop with Kotlin 1.8.20
- [`KT-56207`](https://youtrack.jetbrains.com/issue/KT-56207) Investigate failing tests in ClassifierCommonizationFromSourcesTest

### Tools. Compiler Plugins

#### Fixes

- [`KT-57821`](https://youtrack.jetbrains.com/issue/KT-57821) K2: Compiler calls declaration generation plugins twice for classes in the common source set
- [`KT-57406`](https://youtrack.jetbrains.com/issue/KT-57406) FIR Compiler plugins: Assignment plugin incorrectly recognizes qualified names of annotations
- [`KT-57626`](https://youtrack.jetbrains.com/issue/KT-57626) K2: SERIALIZER_NOT_FOUND for serializable class from another module
- [`KT-57400`](https://youtrack.jetbrains.com/issue/KT-57400) FIR Compiler Plugins: `annotated` predicate does not work with Java classes
- [`KT-57140`](https://youtrack.jetbrains.com/issue/KT-57140) K2: Implement backwards compatibility for FirFunctionTypeKindExtension
- [`KT-56685`](https://youtrack.jetbrains.com/issue/KT-56685) K2: ArrayIndexOfBound during session creation if compiler plugins are enabled
- [`KT-55375`](https://youtrack.jetbrains.com/issue/KT-55375) Remove "legacy" mode of jvm-abi-gen plugin
- [`KT-53470`](https://youtrack.jetbrains.com/issue/KT-53470) FIR: pass `MemberGenerationContext` to all methods of FirDeclarationGenerationExtension
- [`KT-51092`](https://youtrack.jetbrains.com/issue/KT-51092) Lombok `@Value` causes IllegalAccessError
- [`KT-55885`](https://youtrack.jetbrains.com/issue/KT-55885) K2 plugin API: Backend-only declarations are not visible from other modules
- [`KT-55584`](https://youtrack.jetbrains.com/issue/KT-55584) K2: Improve registration of session components from compiler plugins
- [`KT-55843`](https://youtrack.jetbrains.com/issue/KT-55843) FIR Plugin API: metaAnnotated predicate returns meta-annotation itself as well
- [`KT-53874`](https://youtrack.jetbrains.com/issue/KT-53874) Optimize checking for plugin applicability and redesign DeclarationPredicates

### Tools. Compiler plugins. Serialization

#### Fixes

- [`KT-58954`](https://youtrack.jetbrains.com/issue/KT-58954) Serialization: NPE at run time when accessing a delegating property of a deserialized object
- [`KT-56537`](https://youtrack.jetbrains.com/issue/KT-56537) Serialization: Presence of (transient) delegated field in the serialized class breaks deserialization
- [`KT-58918`](https://youtrack.jetbrains.com/issue/KT-58918) Serialization: NPE at run time obtaining a serializer for a sealed class with a generic self-referencing property
- [`KT-59113`](https://youtrack.jetbrains.com/issue/KT-59113) Serialization: NPE at run time when accessing a delegating property of a deserialized object
- [`KT-57647`](https://youtrack.jetbrains.com/issue/KT-57647) Serialization: "IllegalAccessError: Update to static final field" caused by serializable value class
- [`KT-57704`](https://youtrack.jetbrains.com/issue/KT-57704) K2/serialization: false-positive SERIALIZER_NOT_FOUND when compiling against 1.7.20 binary with enum class
- [`KT-57083`](https://youtrack.jetbrains.com/issue/KT-57083) K2/serialization: can't resolve serializers for classes from other modules
- [`KT-56480`](https://youtrack.jetbrains.com/issue/KT-56480) K2: false-positive warning about incompatible serializer type when using type aliases
- [`KT-56594`](https://youtrack.jetbrains.com/issue/KT-56594) K2/serialization reports SERIALIZER_NOT_FOUND over aliased String or primitive types
- [`KT-56553`](https://youtrack.jetbrains.com/issue/KT-56553) Support 'serialization plugin intrinsics' feature in K2
- [`KT-56244`](https://youtrack.jetbrains.com/issue/KT-56244) kotlinx.serialization compiler intrinsic does not work with encodeToString function in 1.8.0

### Tools. Daemon

- [`KT-50846`](https://youtrack.jetbrains.com/issue/KT-50846) Remove "new" Kotlin daemon from codebase

### Tools. Gradle

#### New Features

- [`KT-56971`](https://youtrack.jetbrains.com/issue/KT-56971) Expose jvmTargetValidationMode property in KotlinCompile Gradle task
- [`KT-57159`](https://youtrack.jetbrains.com/issue/KT-57159) Add project level compiler options for Kotlin/JVM plugin

#### Performance Improvements

- [`KT-57052`](https://youtrack.jetbrains.com/issue/KT-57052) Gradle: Stop using exceptions for flow control
- [`KT-57757`](https://youtrack.jetbrains.com/issue/KT-57757) Reduce classpath snapshotter memory consumption
- [`KT-56052`](https://youtrack.jetbrains.com/issue/KT-56052) Implement an in-memory wrapper for PersistentHashMap to avoid applying changes to IC caches before successful compilation

#### Fixes

- [`KT-55624`](https://youtrack.jetbrains.com/issue/KT-55624) Update KGP integration tests that use removed in Gradle 8 getClassifier method
- [`KT-59589`](https://youtrack.jetbrains.com/issue/KT-59589) Gradle: 'java.lang.NoClassDefFoundError: com/gradle/scan/plugin/BuildScanExtension' on 1.9.0-RC when applying Enterprise Plugin from initscript
- [`KT-59063`](https://youtrack.jetbrains.com/issue/KT-59063) Explicit API mode broken in Kotlin 1.9.0-Beta
- [`KT-57653`](https://youtrack.jetbrains.com/issue/KT-57653) Explicit API mode is not enabled when free compiler arguments are specified in Gradle project
- [`KT-59256`](https://youtrack.jetbrains.com/issue/KT-59256) [1.9.0-Beta] ServiceLoader does not pick up classes defined in the same project
- [`KT-58662`](https://youtrack.jetbrains.com/issue/KT-58662) Gradle 8.1 + Configuration Cache: custom values data is missing from build report
- [`KT-58280`](https://youtrack.jetbrains.com/issue/KT-58280) org.jetbrains.kotlin.jvm Gradle plugin contributes build directories to the test compile classpath
- [`KT-59191`](https://youtrack.jetbrains.com/issue/KT-59191) Actual compilation failure exception might be hidden in the case of a Kotlin daemon crash
- [`KT-56211`](https://youtrack.jetbrains.com/issue/KT-56211) Improve Kotlin build reports
- [`KT-57767`](https://youtrack.jetbrains.com/issue/KT-57767) Gradle: "ZipException: invalid entry size" with 1.8.20
- [`KT-57736`](https://youtrack.jetbrains.com/issue/KT-57736) K2: Introduce an easy way to try K2 compiler in Gradle user projects
- [`KT-59056`](https://youtrack.jetbrains.com/issue/KT-59056) FreeCompilerArgs options added using 'subprojects' extension override module-level freeCompilerArgs options
- [`KT-55740`](https://youtrack.jetbrains.com/issue/KT-55740) Gradle 8: Listener registration using Gradle.addBuildListener() has been deprecated
- [`KT-37652`](https://youtrack.jetbrains.com/issue/KT-37652) Support explicit mode for Android projects
- [`KT-58251`](https://youtrack.jetbrains.com/issue/KT-58251) Build Statistics. Kotlin-specific tags are missing in build scans if projects use Gradle 8+
- [`KT-57224`](https://youtrack.jetbrains.com/issue/KT-57224) Add an indicator into build metrics report to show whether K1 or K2 compiler was used to compile the code
- [`KT-58571`](https://youtrack.jetbrains.com/issue/KT-58571) ExplicitApi mode should not apply for test compilations
- [`KT-42718`](https://youtrack.jetbrains.com/issue/KT-42718) Test and AndroidTest sources should be excluded from explicit API requirements for libraries
- [`KT-58916`](https://youtrack.jetbrains.com/issue/KT-58916) [1.9.0-Beta] PLUGIN_CLASSPATH_CONFIGURATION_NAME and NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME should stay public or offer an alternative API
- [`KT-58869`](https://youtrack.jetbrains.com/issue/KT-58869) K2, Gradle: Improve message "100% (2/2) tasks have compiled with Kotlin 2"
- [`KT-52811`](https://youtrack.jetbrains.com/issue/KT-52811) Kotlin Serialization metadata issue due to incompatibility between Gradle Kotlin embedded version and Kotlin Gradle Plugin version
- [`KT-57330`](https://youtrack.jetbrains.com/issue/KT-57330) Provide collection of usage statistics for the Dokka
- [`KT-57393`](https://youtrack.jetbrains.com/issue/KT-57393) jvm-target value set through 'android.kotlinOptions' is ignored and overwritten with the default 1.8 value
- [`KT-58745`](https://youtrack.jetbrains.com/issue/KT-58745) KaptGenerateStubs task should also be configured with the same compiler plugin options
- [`KT-58682`](https://youtrack.jetbrains.com/issue/KT-58682) Explicit api mode does not apply in MPP projects
- [`KT-52976`](https://youtrack.jetbrains.com/issue/KT-52976) Remove deprecated Gradle conventions usages
- [`KT-58530`](https://youtrack.jetbrains.com/issue/KT-58530) Compiler plugin unbundling changes should be backward compatible with Kotlin plugin
- [`KT-36904`](https://youtrack.jetbrains.com/issue/KT-36904) Adding folders to sourceSets.resources.srcDir() in Gradle script does not work
- [`KT-58313`](https://youtrack.jetbrains.com/issue/KT-58313) An exception in console if no task is executed and file build reports are enabled
- [`KT-58619`](https://youtrack.jetbrains.com/issue/KT-58619) Move all pm20 interfaces into Gradle plugin codebase
- [`KT-58320`](https://youtrack.jetbrains.com/issue/KT-58320) Kotlin daemon OOM help message is missing on OOM in Kotlin Daemon itself
- [`KT-53923`](https://youtrack.jetbrains.com/issue/KT-53923) Add 'progressive' compiler argument to Gradle compiler options
- [`KT-53924`](https://youtrack.jetbrains.com/issue/KT-53924) Add 'optIn' compiler arguments to Gradle compiler options
- [`KT-53748`](https://youtrack.jetbrains.com/issue/KT-53748) Remove KotlinCompile setClasspath/getClasspath methods
- [`KT-56454`](https://youtrack.jetbrains.com/issue/KT-56454) Bump minimal support AGP version to 4.2.2
- [`KT-57397`](https://youtrack.jetbrains.com/issue/KT-57397) Add infrastructure to use the build-tools-api to run compilation from Gradle
- [`KT-56946`](https://youtrack.jetbrains.com/issue/KT-56946) Switch incremental Gradle tests for K2 to use language version 2.0
- [`KT-57782`](https://youtrack.jetbrains.com/issue/KT-57782) Disable daemon fallback strategy for Gradle integration tests by default
- [`KT-57142`](https://youtrack.jetbrains.com/issue/KT-57142) Split org.jetbrains.kotlin.gradle.tasks/Tasks.kt into several source files
- [`KT-54447`](https://youtrack.jetbrains.com/issue/KT-54447) Remove usage of deprecated internal Gradle field in Kotlin Gradle Plugin, replace with equivalent in public API
- [`KT-49785`](https://youtrack.jetbrains.com/issue/KT-49785) Avoid creating task output backups until really needed
- [`KT-56047`](https://youtrack.jetbrains.com/issue/KT-56047) False positive message about full recompilation is displayed while restoring from build cache and then making a syntax error
- [`KT-56421`](https://youtrack.jetbrains.com/issue/KT-56421) Gradle: plugin should not use BasePluginExtension deprecated properties
- [`KT-55241`](https://youtrack.jetbrains.com/issue/KT-55241) Gradle: the VariantImplementationFactories build service state is not persistent making impossible to access factories with configuration cache lazily
- [`KT-56357`](https://youtrack.jetbrains.com/issue/KT-56357) Gradle: "DefaultTaskCollection#configureEach(Action) on task set cannot be executed in the current context" because of VariantImplementationFactories
- [`KT-56352`](https://youtrack.jetbrains.com/issue/KT-56352) Make build scan reports more readable
- [`KT-55972`](https://youtrack.jetbrains.com/issue/KT-55972) Gradle: Add an assertion to all integration tests if `warningMode` is not `FAIL`, but the build doesn't produce any warnings

### Tools. Gradle. Cocoapods

- [`KT-38749`](https://youtrack.jetbrains.com/issue/KT-38749) Support reusing generated C-interop between dependant pods
- [`KT-54161`](https://youtrack.jetbrains.com/issue/KT-54161) Support adding extra code to generated Podfile from the Kotlin gradle plugin
- [`KT-56162`](https://youtrack.jetbrains.com/issue/KT-56162) Provide granular Gradle warnings suppression for CocoaPodsIT

### Tools. Gradle. JS

#### New Features

- [`KT-48791`](https://youtrack.jetbrains.com/issue/KT-48791) KJS: Support for Power(ppc64le) and Z(s390x)
- [`KT-32209`](https://youtrack.jetbrains.com/issue/KT-32209) org.jetbrains.kotlin.js does not respect Gradle's archivesBaseName
- [`KT-52646`](https://youtrack.jetbrains.com/issue/KT-52646) KJS / Gradle: make "KotlinCompilationNpmResolver already closed" a warning
- [`KT-52647`](https://youtrack.jetbrains.com/issue/KT-52647) KJS / Gradle: Make "Projects must be configuring" a warning
- [`KT-56158`](https://youtrack.jetbrains.com/issue/KT-56158) KJS: Support implementation dependencies

#### Fixes

- [`KT-59604`](https://youtrack.jetbrains.com/issue/KT-59604) Unresolved reference: useKarma in convention plugin
- [`KT-57604`](https://youtrack.jetbrains.com/issue/KT-57604) JS, Space: Circular dependency between tasks
- [`KT-59116`](https://youtrack.jetbrains.com/issue/KT-59116) K/JS npm dependcies are not resolved properly on Kotlin 1.9
- [`KT-54731`](https://youtrack.jetbrains.com/issue/KT-54731) KJS / Gradle: "There are multiple versions of "kotlin" used in nodejs build: 1.6.21, 1.7.20." with kotlin-dsl in buildSrc
- [`KT-58970`](https://youtrack.jetbrains.com/issue/KT-58970) browserTest gradle task fails if karma is used and gradle configuration cache is enabled
- [`KT-59004`](https://youtrack.jetbrains.com/issue/KT-59004) Kotlin JS 1.9.0-Beta, yarn.lock is unstable in multi module project
- [`KT-56458`](https://youtrack.jetbrains.com/issue/KT-56458) KJS / Gradle: Unnecessary and confusing "There are multiple versions of "kotlin" used in nodejs build" generated from `YarnImportedPackagesVersionResolver`
- [`KT-57985`](https://youtrack.jetbrains.com/issue/KT-57985) K/JS: `packageJson` Gradle configurations don't inherit unique attributes from JsTarget DSL
- [`KT-57817`](https://youtrack.jetbrains.com/issue/KT-57817) JS: executables for couple of JS targets builds in the same directory
- [`KT-58199`](https://youtrack.jetbrains.com/issue/KT-58199) K/JS: Remove useCoverage method
- [`KT-57116`](https://youtrack.jetbrains.com/issue/KT-57116) KJS / Gradle: `commonWebpackConfig` not applied if called after `binaries.executable()`
- [`KT-58522`](https://youtrack.jetbrains.com/issue/KT-58522) K/JS: Upgrade NPM dependency versions
- [`KT-57629`](https://youtrack.jetbrains.com/issue/KT-57629) K/JS: Change default destination of JS production distribution
- [`KT-57480`](https://youtrack.jetbrains.com/issue/KT-57480) K/JS: Use IR compiler by default without explicit choosing of js compiler
- [`KT-58345`](https://youtrack.jetbrains.com/issue/KT-58345) K/JS: Webpack task skipped with ES modules because files have mjs extension
- [`KT-58071`](https://youtrack.jetbrains.com/issue/KT-58071) KJS / Gradle: `jsNodeTest` task is not incremental
- [`KT-43809`](https://youtrack.jetbrains.com/issue/KT-43809) KJS: browserProductionExecutableDistributeResources tasks deletes distributions directory
- [`KT-56690`](https://youtrack.jetbrains.com/issue/KT-56690) Kotlin2JsCompiler friendDependencies cannot be configured through friendPaths
- [`KT-57920`](https://youtrack.jetbrains.com/issue/KT-57920) K/JS: Make imported NPM package not considering dev dependencies
- [`KT-56025`](https://youtrack.jetbrains.com/issue/KT-56025) KJS / Gradle: Gradle 8.0 jsBrowserProductionWebpack uses the output of another project's jsProductionExecutableCompileSync
- [`KT-57630`](https://youtrack.jetbrains.com/issue/KT-57630) K/JS: webpack updating twice on one change of kt sources
- [`KT-47351`](https://youtrack.jetbrains.com/issue/KT-47351) KJS / IR: `:jsTestPackageJson` is unable to find nested included builds under composite build
- [`KT-44754`](https://youtrack.jetbrains.com/issue/KT-44754) K/JS: `browserRun --continuous` keeps rebuilding without any changes
- [`KT-49774`](https://youtrack.jetbrains.com/issue/KT-49774) KJS / Gradle: Errors during NPM dependencies resolution in parallel build lead to unfriendly error messages like "Projects must be closed"
- [`KT-57387`](https://youtrack.jetbrains.com/issue/KT-57387) Remove support of webpack 4
- [`KT-57386`](https://youtrack.jetbrains.com/issue/KT-57386) Kotlin/JS upgrade npm dependencies
- [`KT-56705`](https://youtrack.jetbrains.com/issue/KT-56705) KJS / Gradle: Module name starting with '@' isn't properly set when FUS is disabled
- [`KT-46428`](https://youtrack.jetbrains.com/issue/KT-46428) KJS / IR: Composing build failed "Failed to create MD5 hash for package.json"
- [`KT-53687`](https://youtrack.jetbrains.com/issue/KT-53687) Don't trigger npm and yarn related tasks if it not relevant for assemble
- [`KT-49915`](https://youtrack.jetbrains.com/issue/KT-49915) KJS / Gradle: Gradle build cache miss because of absolute path in `KotlinJsIrLink.filteredArgumentsMap`
- [`KT-56192`](https://youtrack.jetbrains.com/issue/KT-56192) KJS: In browser testing no original sources in stacktrace
- [`KT-42395`](https://youtrack.jetbrains.com/issue/KT-42395) Kotlin/JS: Gradle DSL: PackageJson.customField() does not accept null
- [`KT-43305`](https://youtrack.jetbrains.com/issue/KT-43305) Support Node.JS downloading for Ubuntu
- [`KT-48631`](https://youtrack.jetbrains.com/issue/KT-48631) KJS: Unconditionally uses linux/x86 binaries
- [`KT-38015`](https://youtrack.jetbrains.com/issue/KT-38015) NodeJS installation does not extract symlinks correctly (npm and npx)

### Tools. Gradle. Multiplatform

#### New Features

- [`KT-34662`](https://youtrack.jetbrains.com/issue/KT-34662) Provide an option for Android targets to compile & run `commonTest` tests as unit tests only, instrumented tests only, or both
- [`KT-55881`](https://youtrack.jetbrains.com/issue/KT-55881) Add possibility to enable/disable sources publication similar to Java Gradle Plugin API

#### Fixes

- [`KT-59446`](https://youtrack.jetbrains.com/issue/KT-59446) MPP: transformCommonMainDependenciesMetadata failing for api java dependency in shared jvm source set with Future was not completed yet
- [`KT-55751`](https://youtrack.jetbrains.com/issue/KT-55751) MPP / Gradle: Consumable configurations must have unique attributes
- [`KT-57688`](https://youtrack.jetbrains.com/issue/KT-57688) MPP: `compileDebugKotlinAndroid` task fails with llegalArgumentException: 'moduleName' is null!
- [`KT-56210`](https://youtrack.jetbrains.com/issue/KT-56210) Pass module structure to CLI of K2 if K2 enabled
- [`KT-57531`](https://youtrack.jetbrains.com/issue/KT-57531) KotlinNativeLink: StackOverflowError when consuming library with dependency cycles (from constraints)
- [`KT-58281`](https://youtrack.jetbrains.com/issue/KT-58281) Kotlin Gradle Plugin: Enable Kotlin/Android SourceSetLayout v2 by default
- [`KT-57903`](https://youtrack.jetbrains.com/issue/KT-57903) Prepare for migration to the pluggable android target plugin
- [`KT-49933`](https://youtrack.jetbrains.com/issue/KT-49933) Support Gradle Configuration caching with HMPP
- [`KTIJ-25644`](https://youtrack.jetbrains.com/issue/KTIJ-25644) KGP import: 'KotlinRunConfiguration' won't be able to infer correct runtime classpath
- [`KT-58661`](https://youtrack.jetbrains.com/issue/KT-58661) KGP: KotlinJvmTarget: Implement 'run' carrier task
- [`KT-59055`](https://youtrack.jetbrains.com/issue/KT-59055) KotlinJvmRun not respecting jvmToolchain setting
- [`KT-57959`](https://youtrack.jetbrains.com/issue/KT-57959) Module-name value can't be changed for the android target of a multiplatform project
- [`KT-55506`](https://youtrack.jetbrains.com/issue/KT-55506) TCS: Gradle Sync: kotlin-stdlib-common is not filtered from JVM + Android source sets
- [`KTIJ-25732`](https://youtrack.jetbrains.com/issue/KTIJ-25732) Flaky: KotlinMultiplatformSmokeTests .testJvmAndNative
- [`KTIJ-25039`](https://youtrack.jetbrains.com/issue/KTIJ-25039) New import can add unresolved dependency as empty
- [`KT-57652`](https://youtrack.jetbrains.com/issue/KT-57652) Don't expose sourcesElements for project2project dependencies
- [`KT-58601`](https://youtrack.jetbrains.com/issue/KT-58601) Finalise "ExternalKotlinTargetApi" shape for initial 1.9 release
- [`KT-58710`](https://youtrack.jetbrains.com/issue/KT-58710) External Target Api: Add API to control SourceSetTree
- [`KT-58488`](https://youtrack.jetbrains.com/issue/KT-58488) Add a diagnostic message to KGP in case of val androidTest by getting usage
- [`KT-57482`](https://youtrack.jetbrains.com/issue/KT-57482) cleanNativeDistributionCommonization is not compatible with configuration cache with gradle 8.0
- [`KT-58062`](https://youtrack.jetbrains.com/issue/KT-58062) Commonizer configuration cache not compatible with Gradle 8.1
- [`KT-58086`](https://youtrack.jetbrains.com/issue/KT-58086) Warn about using MPP libraries published in the legacy mode
- [`KTIJ-24254`](https://youtrack.jetbrains.com/issue/KTIJ-24254) KotlinProjectArtifactDependencyResolver returning too early
- [`KT-56439`](https://youtrack.jetbrains.com/issue/KT-56439) TCS: Gradle Sync: IdeBinaryResolver: Add componentFilter API for compilations/configurations
- [`KT-57023`](https://youtrack.jetbrains.com/issue/KT-57023) Cryptic Gradle task descriptions for compile tasks
- [`KT-58470`](https://youtrack.jetbrains.com/issue/KT-58470) Warning about using MPP libraries published in the legacy mode is not reported if the dependency is declared in an intermediate source set
- [`KT-58466`](https://youtrack.jetbrains.com/issue/KT-58466) K2 Gradle: non *.kt files are passed to -Xfragment-sources
- [`KT-58319`](https://youtrack.jetbrains.com/issue/KT-58319) kotlin.git: ProjectMetadataProviderImpl "Unexpected source set 'commonMain'"
- [`KT-51940`](https://youtrack.jetbrains.com/issue/KT-51940) HMPP resolves configurations during configuration
- [`KT-58261`](https://youtrack.jetbrains.com/issue/KT-58261) Link kotlin native binary framework tasks fails when configuration cache is enabled
- [`KT-41506`](https://youtrack.jetbrains.com/issue/KT-41506) UnknownDomainObjectException: "KotlinSourceSet with name not found" when creating custom compilations after applying withJava to an MPP JVM target
- [`KT-58209`](https://youtrack.jetbrains.com/issue/KT-58209) Do not use the term 'Module' in KotlinTargetHierarchy
- [`KT-56153`](https://youtrack.jetbrains.com/issue/KT-56153) When the dependency is unresolved, import fails and don't import anything instead of degrading gracefully
- [`KT-56571`](https://youtrack.jetbrains.com/issue/KT-56571) New import broke apiVersion for commonMain, commonTest and jvmAndAndroidMain modules
- [`KTIJ-24552`](https://youtrack.jetbrains.com/issue/KTIJ-24552) TCS: KotlinMppGradleProjectResolverExtension: Add 'afterProjectResolved' API
- [`KTIJ-24368`](https://youtrack.jetbrains.com/issue/KTIJ-24368) Multiplatform;Composite Builds: prepareKotlinIdeaImport not executed on included builds
- [`KTIJ-24377`](https://youtrack.jetbrains.com/issue/KTIJ-24377) Multiplatform;Composite Builds: Unresolved symbols from a root project of included build
- [`KT-56712`](https://youtrack.jetbrains.com/issue/KT-56712) Multiplatform;Composite Builds: Classpath isolation: .MppDependencyProjectStructureMetadataExtractorFactory cannot be cast to class *MppDependencyProjectStructureMetadataExtractorFactory
- [`KT-56461`](https://youtrack.jetbrains.com/issue/KT-56461) MPP: resolvableMetadataConfiguration: Ensure consistent resolution across all compile dependencies
- [`KT-56841`](https://youtrack.jetbrains.com/issue/KT-56841) MPP: Module-to-module dependencies don't work inside included build in included build
- [`KT-42748`](https://youtrack.jetbrains.com/issue/KT-42748) Project that transitively depends on composite build of multimodule multiplatform library cannot resolve dependencies properly
- [`KT-52356`](https://youtrack.jetbrains.com/issue/KT-52356) MPP / Gradle: Missing common classes on KMM project integrated via Gradle included build into an Android application
- [`KT-51293`](https://youtrack.jetbrains.com/issue/KT-51293) Unresolved references with hierarchical project structure when building KotlinMetadata from native-common source set
- [`KTIJ-24771`](https://youtrack.jetbrains.com/issue/KTIJ-24771) Multiplatform: Composite builds do not resolve in IDEA when rootProject.name != buildIdentifier.name
- [`KT-56700`](https://youtrack.jetbrains.com/issue/KT-56700) V2 MPP Source Set layout warnings should include link to docs
- [`KT-55926`](https://youtrack.jetbrains.com/issue/KT-55926) TCS: Gradle Sync: Import Extras on KotlinSourceSet and KotlinTarget
- [`KTIJ-24364`](https://youtrack.jetbrains.com/issue/KTIJ-24364) KotlinProjectArtifactDependencyResolver: Extend API to include information about 'requesting source set'
- [`KTIJ-24365`](https://youtrack.jetbrains.com/issue/KTIJ-24365) KotlinMPPGradleProjectResolver: Create extension points for other plugins (Android)
- [`KT-55730`](https://youtrack.jetbrains.com/issue/KT-55730) MPP / Gradle: compileKotlinMetadata fails to resolve symbols in additional source sets

### Tools. Gradle. Native

- [`KT-58838`](https://youtrack.jetbrains.com/issue/KT-58838) KGP/Multiplatform: 1.9.0-Beta with custom cinterops: IllegalStateException: Could not create domain object 'jni' (DefaultCInteropSettings)
- [`KT-57823`](https://youtrack.jetbrains.com/issue/KT-57823) KotlinNativeCompileOptions.moduleName value is ignored and replaced with the default one if to set up using compilations
- [`KT-57815`](https://youtrack.jetbrains.com/issue/KT-57815) KotlinNativeCompileOptions.moduleName isn't accessible if to configure using compilerOptions.configure {}
- [`KT-57944`](https://youtrack.jetbrains.com/issue/KT-57944) K2: K2, MPP, Native: K2 reports "Source does not belong to any module" for native sources
- [`KT-53108`](https://youtrack.jetbrains.com/issue/KT-53108) Expose Kotlin/Native compiler options as Gradle DSL
- [`KT-58063`](https://youtrack.jetbrains.com/issue/KT-58063) Kotlin/Native tasks configuration cache are not compatible with Gradle 8.1
- [`KT-38317`](https://youtrack.jetbrains.com/issue/KT-38317) Kotlin/Native: NSURLConnection HTTPS requests fail in iOS tests due to --standalone simctl flag
- [`KT-56280`](https://youtrack.jetbrains.com/issue/KT-56280) Gradle: freeCompilerArgs are no longer propagated from compilations to Native binaries

### Tools. Incremental Compile

- [`KT-58289`](https://youtrack.jetbrains.com/issue/KT-58289) IC fails to detect a change to class annotations
- [`KT-58986`](https://youtrack.jetbrains.com/issue/KT-58986) New IC: ISE "The following LookupSymbols are not yet converted to ProgramSymbols: LookupSymbol(name=$$delegatedProperties, ...)"
- [`KT-56197`](https://youtrack.jetbrains.com/issue/KT-56197) If use classpathSnapshot, the invoke place of subclass's super function who has default parameters will not recompiled if it is incremental build
- [`KT-56886`](https://youtrack.jetbrains.com/issue/KT-56886) K2: Changes to Java sources used in Kotlin project do not trigger a rebuild if a previous build was successful

### Tools. JPS

- [`KTIJ-25384`](https://youtrack.jetbrains.com/issue/KTIJ-25384) JPS plugin: serialization plugin is not applied to modules
- [`KT-56438`](https://youtrack.jetbrains.com/issue/KT-56438) Add minimal statistic report for JPS build
- [`KT-58314`](https://youtrack.jetbrains.com/issue/KT-58314) kotlin-build-statistics is missing for kotlin-jps-plugin
- [`KT-55696`](https://youtrack.jetbrains.com/issue/KT-55696) JPS IC K2 - fix new failing tests with Java interop
- [`KT-57102`](https://youtrack.jetbrains.com/issue/KT-57102) SCE: DefaultErrorMessages$Extension: DefaultErrorMessagesWasm not a subtype after adding WASM error messages

### Tools. Kapt

- [`KT-54468`](https://youtrack.jetbrains.com/issue/KT-54468) KAPT Gradle plugin causes eager task creation
- [`KT-59521`](https://youtrack.jetbrains.com/issue/KT-59521) Kapt maven plugin require version of annotation processor
- [`KT-58301`](https://youtrack.jetbrains.com/issue/KT-58301) K2: Compile Kotlin task failure for the generated by Kapt sources : 'Source NameOfTheGenerated.kt  does not belong to any module
- [`KT-57598`](https://youtrack.jetbrains.com/issue/KT-57598) K2: Support a fallback mode executing Kapt with K1 even when the compiler is run with languageVersion=2.0
- [`KT-58226`](https://youtrack.jetbrains.com/issue/KT-58226) KAPT: “org.jetbrains.kotlin.psi2ir.generators.ErrorExpressionException: null: KtNameReferenceExpression” with enum with secondary constructor

### Tools. Maven

- [`KTIJ-25445`](https://youtrack.jetbrains.com/issue/KTIJ-25445) Maven. JVM target is imported as 1.6 when no target specified in pom.xml

### Tools. Parcelize

- [`KT-59112`](https://youtrack.jetbrains.com/issue/KT-59112) K2: "IllegalStateException: Function has no body with `@Parcelize`" on nested sealed class hierarchies

### Tools. Scripts

- [`KT-58366`](https://youtrack.jetbrains.com/issue/KT-58366) The obsolete kotlin-script-util jar is still published and contains broken JSR-223 implementation

### Tools. Wasm

- [`KT-56585`](https://youtrack.jetbrains.com/issue/KT-56585) Change wasmBrowserRun Browser Executable to System Default
- [`KT-56159`](https://youtrack.jetbrains.com/issue/KT-56159) Running (karma) tests doesn't work in a project generated by wizard "Browser Application for Kotlin/Wasm"
- [`KT-57203`](https://youtrack.jetbrains.com/issue/KT-57203) Update Kotlin/Wasm to support Gradle 8


## Recent ChangeLogs:
### [ChangeLog-1.8.X](docs/changelogs/ChangeLog-1.8.X.md)
### [ChangeLog-1.7.X](docs/changelogs/ChangeLog-1.7.X.md)
### [ChangeLog-1.6.X](docs/changelogs/ChangeLog-1.6.X.md)
### [ChangeLog-1.5.X](docs/changelogs/ChangeLog-1.5.X.md)
### [ChangeLog-1.4.X](docs/changelogs/ChangeLog-1.4.X.md)
### [ChangeLog-1.3.X](docs/changelogs/ChangeLog-1.3.X.md)
### [ChangeLog-1.2.X](docs/changelogs/ChangeLog-1.2.X.md)
### [ChangeLog-1.1.X](docs/changelogs/ChangeLog-1.1.X.md)
### [ChangeLog-1.0.X](docs/changelogs/ChangeLog-1.0.X.md)