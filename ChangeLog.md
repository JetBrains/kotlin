## 1.9.0-Beta

### Analysis API

#### New Features

- [`KT-57930`](https://youtrack.jetbrains.com/issue/KT-57930) Analysis API: provide an API for extending Kotlin resolution
- [`KT-57636`](https://youtrack.jetbrains.com/issue/KT-57636) K2: Add the return type of K2 reference shortener AA `ShortenCommand::invokeShortening()` e.g., `ShorteningResultInfo` to allow callers to access the shortening result PSI

#### Fixes

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
- [`KT-58141`](https://youtrack.jetbrains.com/issue/KT-58141) K2: AA FIR: impossible to restore symbol for declaration with annotation with argument inside type
- [`KT-57462`](https://youtrack.jetbrains.com/issue/KT-57462) Symbol Light Classes: SymbolLightFieldForProperty should retrieve annotations not from KtPropertySymbol, but from the corresponding backing field
- [`KT-58249`](https://youtrack.jetbrains.com/issue/KT-58249) Analysis API: Disable error logging for FE10 implementation of resolveCall when resolve is not successful
- [`KT-54864`](https://youtrack.jetbrains.com/issue/KT-54864) Analysis API: add function to get expect KtSymbol list by actual KtSymbol
- [`KT-56763`](https://youtrack.jetbrains.com/issue/KT-56763) Analysis API: `.KtSourceModuleImpl is missing in the map.` on symbol restore when symbol cannot be seen from the use-site module
- [`KT-56617`](https://youtrack.jetbrains.com/issue/KT-56617) Analysis API: optimize KtFirSymbolProviderByJavaPsi.getNamedClassSymbol
- [`KT-54430`](https://youtrack.jetbrains.com/issue/KT-54430) K2: .getAllOverriddenSymbols() returns invalid results

### Backend. Native. Debug

- [`KT-55440`](https://youtrack.jetbrains.com/issue/KT-55440) Kotlin/Native debugger: inline function parameters are not visible during debugging

### Backend. Wasm

- [`KT-57136`](https://youtrack.jetbrains.com/issue/KT-57136) K/Wasm: Restrict non-external types in JS interop
- [`KT-57060`](https://youtrack.jetbrains.com/issue/KT-57060) Clarify the lack of support for dynamic in Kotlin/Wasm
- [`KT-56955`](https://youtrack.jetbrains.com/issue/KT-56955) K/Wasm: Support restricted version of K/JS `js(code)`
- [`KT-57276`](https://youtrack.jetbrains.com/issue/KT-57276) Wasm: "Body not found for function" error when compiling konform library with Kotlin/Wasm support
- [`KT-56976`](https://youtrack.jetbrains.com/issue/KT-56976) K/Wasm bug with calling override of external function with default parameters
- [`KT-56584`](https://youtrack.jetbrains.com/issue/KT-56584) K/Wasm: Can't link symbol class

### Compiler

#### New Features

- [`KT-49276`](https://youtrack.jetbrains.com/issue/KT-49276) Warn about potential overload resolution change if Range/Progression starts implementing Collection
- [`KT-55333`](https://youtrack.jetbrains.com/issue/KT-55333) Allow secondary constructors in value classes with bodies
- [`KT-54944`](https://youtrack.jetbrains.com/issue/KT-54944) @Volatile support in native
- [`KT-54746`](https://youtrack.jetbrains.com/issue/KT-54746) Deprecate with ERROR JvmDefault annotation and old -Xjvm-default modes
- [`KT-29378`](https://youtrack.jetbrains.com/issue/KT-29378) K2: rework warnings/errors for equality/identity operators on incompatible types
- [`KT-57010`](https://youtrack.jetbrains.com/issue/KT-57010) Kotlin/Native: make it possible to compile bitcode in a separate compiler invocation
- [`KT-55691`](https://youtrack.jetbrains.com/issue/KT-55691) K2: Avoid inferring Nothing? in presence of other constraints (beside type parameter bounds)
- [`KT-46288`](https://youtrack.jetbrains.com/issue/KT-46288) Unexpected behavior of extension function on lambda with suspend receiver
- [`KT-24779`](https://youtrack.jetbrains.com/issue/KT-24779) Inconsistent smart cast behavior for bound data flow values

#### Performance Improvements

- [`KT-56906`](https://youtrack.jetbrains.com/issue/KT-56906) FIR: Use cached instance of FirImplicitTypeRefImpl in FIR builders
- [`KT-56276`](https://youtrack.jetbrains.com/issue/KT-56276) LanguageVersion.getVersionString() allocates 5k objects on project opening

#### Fixes

- [`KT-56609`](https://youtrack.jetbrains.com/issue/KT-56609) K2: False positive NULL_FOR_NONNULL_TYPE with -Xjsr305=strict and @Nullable annotation Java parameter
- [`KT-56656`](https://youtrack.jetbrains.com/issue/KT-56656) K1/K2: inconsistent NOTHING_TO_OVERRIDE with complex nullable annotations
- [`KT-58332`](https://youtrack.jetbrains.com/issue/KT-58332) K2: local fun with suspend type is not marked as suspend in IR
- [`KT-57991`](https://youtrack.jetbrains.com/issue/KT-57991) K2: Modifier 'suspend' is not applicable to 'anonymous function'
- [`KT-4113`](https://youtrack.jetbrains.com/issue/KT-4113) No smartcast for nullable lambda property (functional type) with implicit/operator `invoke` call
- [`KT-54294`](https://youtrack.jetbrains.com/issue/KT-54294) K2: "Not all type variables found" in builder inference with type parameters inferred through a union of two branches
- [`KT-52597`](https://youtrack.jetbrains.com/issue/KT-52597) Provide Alpha Support for Multiplatform in the K2 platform
- [`KT-58523`](https://youtrack.jetbrains.com/issue/KT-58523) K2: reference is resolved to imported type-alias instead of identically named top-level property
- [`KT-57098`](https://youtrack.jetbrains.com/issue/KT-57098) Native: avoid object initialization while accessing const val
- [`KT-57973`](https://youtrack.jetbrains.com/issue/KT-57973) 32-th default value in inline classes override function is not used
- [`KT-57714`](https://youtrack.jetbrains.com/issue/KT-57714) "IllegalStateException: <B::!>" using reified generics
- [`KT-57810`](https://youtrack.jetbrains.com/issue/KT-57810) `toString` of object erroneously considered as constant function in string concatenation
- [`KT-58076`](https://youtrack.jetbrains.com/issue/KT-58076) K2: Incorrect inference of type of labeled receiver
- [`KT-57929`](https://youtrack.jetbrains.com/issue/KT-57929) K2: Arguments of annotations  are not calculated in a lot of strange locations
- [`KT-55388`](https://youtrack.jetbrains.com/issue/KT-55388) Consider enabling ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
- [`KT-53041`](https://youtrack.jetbrains.com/issue/KT-53041) NPE in Kotlin 1.7.0 when using RxJava Maybe.doOnEvent with anonymous parameters
- [`KT-54829`](https://youtrack.jetbrains.com/issue/KT-54829) Cleanup local types approximation logic
- [`KT-38871`](https://youtrack.jetbrains.com/issue/KT-38871) Kotlin Gradle DSL, MPP: UNUSED_VARIABLE when configuring a sourceset with delegated property
- [`KT-58587`](https://youtrack.jetbrains.com/issue/KT-58587) MUST_BE_INITIALIZED must take into account effectivelly final
- [`KT-58524`](https://youtrack.jetbrains.com/issue/KT-58524) K2: false-positive overload resolution ambiguity error on invoking a generic class's member function with id-shaped function-typed parameter on intersection-typed receiver
- [`KT-53929`](https://youtrack.jetbrains.com/issue/KT-53929) Enum.entries: consider changing scope behavior in K1
- [`KT-58520`](https://youtrack.jetbrains.com/issue/KT-58520) K2: FIR2IR: ISE during const evaluation of operator times with exposed
- [`KT-56662`](https://youtrack.jetbrains.com/issue/KT-56662) K1: false negative INVISIBLE_SETTER for a var with internal setter accessed from a derived class
- [`KT-57770`](https://youtrack.jetbrains.com/issue/KT-57770) K2: Support generation of serializer if base class for serializable class declared in different module
- [`KT-58375`](https://youtrack.jetbrains.com/issue/KT-58375) Kapt: "wrong number of type arguments. required 1" when more than 22 type arguments
- [`KT-56077`](https://youtrack.jetbrains.com/issue/KT-56077) K2: build atomicfu
- [`KT-56074`](https://youtrack.jetbrains.com/issue/KT-56074) K2: build Space JVM (snapshot 2022.3)
- [`KT-48870`](https://youtrack.jetbrains.com/issue/KT-48870) [FIR] Different behavior for explicit receiver resolution inside delegated constructors
- [`KT-53865`](https://youtrack.jetbrains.com/issue/KT-53865) Partial linkage: overriding function with parameter losing vararg: Native is successful, JS fails in runtime
- [`KT-53568`](https://youtrack.jetbrains.com/issue/KT-53568) Partial linkage: absent class as type parameter bound causes failure of `compileProductionExecutableKotlinJs`
- [`KT-53608`](https://youtrack.jetbrains.com/issue/KT-53608) Partial linkage: Kotlin/JS fails with IllegalStateException: "Validation failed in file" when overridden declaration was visible, but now private
- [`KT-53663`](https://youtrack.jetbrains.com/issue/KT-53663) Partial linkage: usage of property which becomes abstract: no IrLinkageError, but AssertionError in Native backend instead
- [`KT-53938`](https://youtrack.jetbrains.com/issue/KT-53938) Partial linkage: with turning interface into class and using as second parent Native build fails
- [`KT-53939`](https://youtrack.jetbrains.com/issue/KT-53939) Partial linkage: with turning object into class link*Native and js*Test tasks fail
- [`KT-53941`](https://youtrack.jetbrains.com/issue/KT-53941) Partial linkage: with turning class into object accessing member via parameterless constructor does not fail
- [`KT-53970`](https://youtrack.jetbrains.com/issue/KT-53970) Partial linkage: on turning nested class into inner JS tasks are successful, Native build fails
- [`KT-53972`](https://youtrack.jetbrains.com/issue/KT-53972) Partial linkage: turning inner class into nested: with usage in executable Native fails with NPE in backend
- [`KT-53971`](https://youtrack.jetbrains.com/issue/KT-53971) Partial linkage: turning inner class into nested: without usage in executable Native is successful, JavaScript fails
- [`KT-53995`](https://youtrack.jetbrains.com/issue/KT-53995) Partial linkage: on turning class to abstract and direct constructor call Naive fails, JavaScript is successful
- [`KT-58013`](https://youtrack.jetbrains.com/issue/KT-58013) K2: "Not enough information to infer type variable T" when using assert non-null (!!) and delegation
- [`KT-54045`](https://youtrack.jetbrains.com/issue/KT-54045) Partial linkage: turning class into type alias + calculating implicit function type: build fails with UninitializedPropertyAccessException: "lateinit property parent has not been initialized"
- [`KT-54046`](https://youtrack.jetbrains.com/issue/KT-54046) Partial linkage: turning type alias into class + using it as type: build fails with AssertionError: "Expected exactly one delegating constructor call but none encountered"
- [`KT-53887`](https://youtrack.jetbrains.com/issue/KT-53887) Partial linkage: turning from enum to regular class + reference to enum contant causes compileProductionExecutableKotlinJs fail with IllegalStateException
- [`KT-54047`](https://youtrack.jetbrains.com/issue/KT-54047) Partial linkage: reference to removed enum const causes JS fail with "IllegalStateException: Validation failed in file"
- [`KT-54048`](https://youtrack.jetbrains.com/issue/KT-54048) Partial linkage: reference to removed enum const in runtime causes Native fail with IllegalStateException at IrBindablePublicSymbolBase.getOwner()
- [`KT-57784`](https://youtrack.jetbrains.com/issue/KT-57784) "NullPointerException: Parameter specified as non-null is null:" with enum, companion object, 'entries' and map
- [`KT-58365`](https://youtrack.jetbrains.com/issue/KT-58365) K2: Fix stub types leakage in builder inference caused by implicit receiver type update with partially resolved calls (IGNORE_LEAKED_INTERNAL_TYPES for stub types)
- [`KT-58214`](https://youtrack.jetbrains.com/issue/KT-58214) Continuation parameter only exists in lowered suspend functions, but function origin is LOCAL_FUNCTION_FOR_LAMBDA
- [`KT-58135`](https://youtrack.jetbrains.com/issue/KT-58135) K2: Priority of extension property is lower than ordinary property
- [`KT-57181`](https://youtrack.jetbrains.com/issue/KT-57181) [K1/N, K2/N] Expect and Actual funs have different IdSignature.CommonSignature, if Expect has default argument
- [`KT-56023`](https://youtrack.jetbrains.com/issue/KT-56023) Constant operations (e.g. division) are not constant in K2 (JS, Native)
- [`KT-57354`](https://youtrack.jetbrains.com/issue/KT-57354) In suspend function default arguments are sometimes not deleted in IR
- [`KT-55242`](https://youtrack.jetbrains.com/issue/KT-55242) K2/MPP: basic build/link functionality
- [`KT-57979`](https://youtrack.jetbrains.com/issue/KT-57979) K2: Unresolved reference error when assigning to Java synthetic property with a different nullability getter
- [`KT-58142`](https://youtrack.jetbrains.com/issue/KT-58142) K2: val parameter with more specific type is lower priority
- [`KT-54518`](https://youtrack.jetbrains.com/issue/KT-54518) False negative NON_PUBLIC_CALL_FROM_PUBLIC_INLINE when calling internal method of super class
- [`KT-58025`](https://youtrack.jetbrains.com/issue/KT-58025) K2: Argument type mismatch when using Springs HandlerMethodArgumentResolver
- [`KT-57373`](https://youtrack.jetbrains.com/issue/KT-57373) K2: FIR properties synthesized when implementing interface by delegation don't have accessors
- [`KT-58259`](https://youtrack.jetbrains.com/issue/KT-58259) Unexpected unresolved function call with obvious invoke-convention desugaring
- [`KT-57135`](https://youtrack.jetbrains.com/issue/KT-57135) K2: Fir should take into account an annotation's allowed targets as well as the use-site target when deciding whether it applies to a property, a field, or a constructor parameter
- [`KT-57958`](https://youtrack.jetbrains.com/issue/KT-57958) K2: Initializer type mismatch when using extension property on type with star projection
- [`KT-58149`](https://youtrack.jetbrains.com/issue/KT-58149) K2: New inference error with buildList
- [`KT-58008`](https://youtrack.jetbrains.com/issue/KT-58008) K2: "Cannot find cached type parameter by FIR symbol: T" on suspend function with generic and nested class
- [`KT-57835`](https://youtrack.jetbrains.com/issue/KT-57835) K2: compiler crash on lambda with dynamic receiver
- [`KT-56500`](https://youtrack.jetbrains.com/issue/KT-56500) The type parameter TYPE_PARAMETER name:E index:0 variance: superTypes:[kotlin.Any?] reified:false is not defined in the referenced function FUN LOCAL_FUNCTION_FOR_LAMBDA
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
- [`KT-56442`](https://youtrack.jetbrains.com/issue/KT-56442) K2: Make sure K2 has the same behavior for defaults with overrides as K1 has
- [`KT-55904`](https://youtrack.jetbrains.com/issue/KT-55904) Fix tests for volatile annotation on K2
- [`KT-57928`](https://youtrack.jetbrains.com/issue/KT-57928) K2: Arguments of annotations on constructor value parameter are not calculated
- [`KT-57814`](https://youtrack.jetbrains.com/issue/KT-57814) K2: Argument type mismatch with delegating property
- [`KT-56490`](https://youtrack.jetbrains.com/issue/KT-56490) Implement deprecation for an anonymous type exposed from inline functions with type argument
- [`KT-57962`](https://youtrack.jetbrains.com/issue/KT-57962) K2: No set method providing array access on dynamic
- [`KT-57353`](https://youtrack.jetbrains.com/issue/KT-57353) K2: unresolved reference when using fully qualified object declaration name as an expression, when a declaration package is from another klib and has at least two name segments
- [`KT-57988`](https://youtrack.jetbrains.com/issue/KT-57988) K2: compiler exception on get operator on dynamic this
- [`KT-57960`](https://youtrack.jetbrains.com/issue/KT-57960) K2: incorrect type inference in lambda with dynamic receiver
- [`KT-56511`](https://youtrack.jetbrains.com/issue/KT-56511) K1: false negative SMARTCAST_IMPOSSIBLE when alien constructor property is accessed from a private class
- [`KT-55079`](https://youtrack.jetbrains.com/issue/KT-55079) Refactor DiagnosticReporterByTrackingStrategy and fix some "diagnostic into black hole" problems
- [`KT-57961`](https://youtrack.jetbrains.com/issue/KT-57961) K2: Unresolved reference using dynamic lambda parameter
- [`KT-57880`](https://youtrack.jetbrains.com/issue/KT-57880) K2: false-positive argument type mismatch due to lambda receiver shadowing labeled outer lambda receiver when assigning lambda to variable
- [`KT-57947`](https://youtrack.jetbrains.com/issue/KT-57947) K2: Incorrect resolution results when property type for invokeExtension is not inferred
- [`KT-56687`](https://youtrack.jetbrains.com/issue/KT-56687) Unexpected behaviour with enum entries when using outdated stdlib
- [`KT-57806`](https://youtrack.jetbrains.com/issue/KT-57806) K2: string interpolation as annotation parameter causes error
- [`KT-57611`](https://youtrack.jetbrains.com/issue/KT-57611) K2: Annotation arguments are not evaluated
- [`KT-56190`](https://youtrack.jetbrains.com/issue/KT-56190) [K2/N] Const initializers are not serialized to klib
- [`KT-57843`](https://youtrack.jetbrains.com/issue/KT-57843) K2: Missing diagnostic when calling constructor through typealias whose expansion has a deprecation
- [`KT-57350`](https://youtrack.jetbrains.com/issue/KT-57350) FIR: deprecation diagnostic is not reported on a super class call
- [`KT-57769`](https://youtrack.jetbrains.com/issue/KT-57769) [K2] Load properties in proper order for classes compiled with kotlinx.serialization and LV < 2.0
- [`KT-56258`](https://youtrack.jetbrains.com/issue/KT-56258) VerifyError: Bad local variable type when using -Xdebug
- [`KT-57839`](https://youtrack.jetbrains.com/issue/KT-57839) K2: Compiler crash on lambda returning anonymous object with implemented lambda
- [`KT-57822`](https://youtrack.jetbrains.com/issue/KT-57822) K2: Can't refer to external interface from class literal
- [`KT-57809`](https://youtrack.jetbrains.com/issue/KT-57809) K2: No value passed for parameter of external class
- [`KT-56383`](https://youtrack.jetbrains.com/issue/KT-56383) Build intellij master with LV 1.9
- [`KT-55056`](https://youtrack.jetbrains.com/issue/KT-55056) Builder inference causes incorrect type inference result in related call
- [`KT-30905`](https://youtrack.jetbrains.com/issue/KT-30905) Expect var property with default public setter matches with actual var property with private setter
- [`KT-56030`](https://youtrack.jetbrains.com/issue/KT-56030) [K2/N] Support Objective-C overloading by param names only
- [`KT-57665`](https://youtrack.jetbrains.com/issue/KT-57665) K2: incorrect resolution of dynamic type
- [`KT-57763`](https://youtrack.jetbrains.com/issue/KT-57763) FirExtensionRegistrar extension point broken
- [`KT-40903`](https://youtrack.jetbrains.com/issue/KT-40903) Forbid actual member in expect class
- [`KT-56965`](https://youtrack.jetbrains.com/issue/KT-56965) K/N: linkDebugFrameworkIosArm64 tasks failing with UnsupportedOperationException: VAR name:disposables type:com.badoo.reaktive.disposable.CompositeDisposable [val]
- [`KT-57768`](https://youtrack.jetbrains.com/issue/KT-57768) Don't decompile code to search for annotation arguments
- [`KT-55628`](https://youtrack.jetbrains.com/issue/KT-55628) Diagnostics for kotlin.concurrent.Volatile annotation applicability
- [`KT-56815`](https://youtrack.jetbrains.com/issue/KT-56815) compileKotlin task is stuck with while(true) and suspend function
- [`KT-55860`](https://youtrack.jetbrains.com/issue/KT-55860) K2. [CONFLICTING_INHERITED_MEMBERS] for inheritor of a class with overloaded generic function
- [`KT-55316`](https://youtrack.jetbrains.com/issue/KT-55316) K2. IllegalStateException on incorrect import directive name
- [`KT-55804`](https://youtrack.jetbrains.com/issue/KT-55804) K2: UNSAFE_CALL Non-nullable generic marked as nullable even if non-null asserted
- [`KT-55405`](https://youtrack.jetbrains.com/issue/KT-55405) K2: false-negative INVISIBLE_REFERENCE in import directives
- [`KT-54781`](https://youtrack.jetbrains.com/issue/KT-54781) K2: no error on unresolved import statement with more than one package
- [`KT-55902`](https://youtrack.jetbrains.com/issue/KT-55902) K2: Support ImplicitIntegerCoercion annotation
- [`KT-56577`](https://youtrack.jetbrains.com/issue/KT-56577) Migrate Native KLIB ABI compatibility tests to K2
- [`KT-56603`](https://youtrack.jetbrains.com/issue/KT-56603) [K2/N] Segfault invoking fun from binary compatible klib
- [`KT-54894`](https://youtrack.jetbrains.com/issue/KT-54894) K2: False positive RETURN_TYPE_MISMATCH on function which returns a functional type with @UnsafeVariance argument
- [`KT-57602`](https://youtrack.jetbrains.com/issue/KT-57602) K2: Rework member scope of types having projection arguments for covariant parameters
- [`KT-50550`](https://youtrack.jetbrains.com/issue/KT-50550) False positive NO_ELSE_IN_WHEN with annotated `when` branch condition
- [`KT-57431`](https://youtrack.jetbrains.com/issue/KT-57431) K2 MPP JS: Compiler crash on transitive common dependencies
- [`KT-57510`](https://youtrack.jetbrains.com/issue/KT-57510) K2: Data class equals/hashCode/toString methods are not written to Klib metadata
- [`KT-56336`](https://youtrack.jetbrains.com/issue/KT-56336) [K2/N] Multiplatform test fails with unexpected "actual declaration has no corresponding expected declaration" compiler error
- [`KT-57556`](https://youtrack.jetbrains.com/issue/KT-57556) K2: Rename error 'This API is not available after FIR'
- [`KT-56583`](https://youtrack.jetbrains.com/issue/KT-56583) K1: Implement opt-in for integer cinterop conversions
- [`KT-23447`](https://youtrack.jetbrains.com/issue/KT-23447) Integer.toChar compiles to missing method
- [`KT-46465`](https://youtrack.jetbrains.com/issue/KT-46465) Deprecate and make open Number.toChar()
- [`KT-56119`](https://youtrack.jetbrains.com/issue/KT-56119) BinaryVersion.isCompatible binary compatibility is broken
- [`KT-56527`](https://youtrack.jetbrains.com/issue/KT-56527) K2: "AssertionError: Assertion failed" during compilation in SequentialFilePositionFinder
- [`KT-55469`](https://youtrack.jetbrains.com/issue/KT-55469) [K2/N] equals(Double,Double) and equals(Boolean,Boolean) are not found
- [`KT-55055`](https://youtrack.jetbrains.com/issue/KT-55055) K1: Builder inference violates upper bound
- [`KT-57316`](https://youtrack.jetbrains.com/issue/KT-57316) Initialize Enum.entries eagerly: avoid using invokedynamics
- [`KT-57491`](https://youtrack.jetbrains.com/issue/KT-57491) Kotlin synthetic parameter looks ordinary
- [`KT-56747`](https://youtrack.jetbrains.com/issue/KT-56747) [K2/N] Return type for `lambda: (Any) -> Any` which returns Unit is different for K1 and K2 and return statement is missing with K2
- [`KT-57211`](https://youtrack.jetbrains.com/issue/KT-57211) K2: incorrect "error: an annotation argument must be a compile-time constant" on unsigned array in annotation argument
- [`KT-57424`](https://youtrack.jetbrains.com/issue/KT-57424) K2 IDE: "By now the annotations argument mapping should have been resolved" exception
- [`KT-56171`](https://youtrack.jetbrains.com/issue/KT-56171) Implement deprecation warning for missing PRIVATE_CLASS_MEMBER_FROM_INLINE error
- [`KT-57241`](https://youtrack.jetbrains.com/issue/KT-57241) K2 MPP: Actualization doesn't work for actual enum that has primary constructor with arguments
- [`KT-57210`](https://youtrack.jetbrains.com/issue/KT-57210) K2 MPP: Support of arguments with dynamic type
- [`KT-57182`](https://youtrack.jetbrains.com/issue/KT-57182) K2 MPP: Actualization doesn't work for nested objects
- [`KT-56344`](https://youtrack.jetbrains.com/issue/KT-56344) K2: Implement correct errors reporting of IrActualizer
- [`KT-54405`](https://youtrack.jetbrains.com/issue/KT-54405) K2 compiler allows val redeclaration
- [`KT-54531`](https://youtrack.jetbrains.com/issue/KT-54531) [K2] Uncaught Runtime exception is thrown instead of user friendly error messages with details in case -no-jdk option set to true
- [`KT-57131`](https://youtrack.jetbrains.com/issue/KT-57131) K2: stdlib test compilation fails on ListTest.kt in FirJvmMangleComputer
- [`KT-56913`](https://youtrack.jetbrains.com/issue/KT-56913) K2: Incorrect line numbers in overriden field getters and setters
- [`KT-56982`](https://youtrack.jetbrains.com/issue/KT-56982) K2: Incorrect line number start in when expression
- [`KT-56720`](https://youtrack.jetbrains.com/issue/KT-56720) K2: false positive MANY_IMPL_MEMBER_NOT_IMPLEMENTED in case of delegation in diamond inheritance
- [`KT-57198`](https://youtrack.jetbrains.com/issue/KT-57198) K2: false-positive type mismatch error on inherited raw-typed class with type parameters in upper bounds of other type parameters
- [`KT-15470`](https://youtrack.jetbrains.com/issue/KT-15470) Inconsistency: use-site 'set' target is a compilation error, use-site 'get' target is ok
- [`KT-57405`](https://youtrack.jetbrains.com/issue/KT-57405) K2. Function call ambiguity error when nullable String is passed to function with Spring @Nullable annotation in signature
- [`KT-57242`](https://youtrack.jetbrains.com/issue/KT-57242) Equals behaviour for value classes implementing interfaces is different between 1.8.10 and 1.8.20-RC
- [`KT-57261`](https://youtrack.jetbrains.com/issue/KT-57261) "IllegalArgumentException was thrown at: MemoizedInlineClassReplacements.getSpecializedEqualsMethod" when comparing non-inline class instance with an inline class instance
- [`KT-27261`](https://youtrack.jetbrains.com/issue/KT-27261) Contracts for infix functions don't work (for receivers and parameters)
- [`KT-57036`](https://youtrack.jetbrains.com/issue/KT-57036) Unresolved reference: with inferred type of class constructor with extension parameter
- [`KT-56177`](https://youtrack.jetbrains.com/issue/KT-56177) K2: FIR should not generate annotation on both property and parameter
- [`KT-54990`](https://youtrack.jetbrains.com/issue/KT-54990) NI: Type mismatch when encountering bounded type parameter and projections
- [`KT-57107`](https://youtrack.jetbrains.com/issue/KT-57107) Handling of Windows line endings CRLF broken in latest snapshot with K2
- [`KT-57117`](https://youtrack.jetbrains.com/issue/KT-57117) K2: Compiler reports invalid columns in diagnostics in case of crlf line endings
- [`KT-55828`](https://youtrack.jetbrains.com/issue/KT-55828) [K2/N]: Fix test fails in OPT mode : `Internal compiler error: no implementation found ... when building itable/vtable`
- [`KT-56169`](https://youtrack.jetbrains.com/issue/KT-56169) False negative deprecation warning about future inference error with builder inference
- [`KT-56657`](https://youtrack.jetbrains.com/issue/KT-56657) K1/K2: inconsistent behavior in nullability mismatch (Guava hash set/map)
- [`KT-56379`](https://youtrack.jetbrains.com/issue/KT-56379) K2: build tests for the Kotlin standard library
- [`KT-56079`](https://youtrack.jetbrains.com/issue/KT-56079) K2: build YouTrack 2022.3
- [`KT-56696`](https://youtrack.jetbrains.com/issue/KT-56696) K2: Allow to access uninitialized member properties in non-inPlace lambdas in class initialization
- [`KT-56630`](https://youtrack.jetbrains.com/issue/KT-56630) FIR: ClassCastException on compilation hierarchy with a raw type
- [`KT-57171`](https://youtrack.jetbrains.com/issue/KT-57171) K2: Implement bytecode tests
- [`KT-56814`](https://youtrack.jetbrains.com/issue/KT-56814) K2. PsiElement is null inside IrClass. As a result ClassBuilder defineClass gets null as origin
- [`KT-54758`](https://youtrack.jetbrains.com/issue/KT-54758) Deprecate `ClassBuilderInterceptorExtension.interceptClassBuilderFactory` and provide another method without dependency on K1
- [`KT-57253`](https://youtrack.jetbrains.com/issue/KT-57253) K2: clean up callable reference logic in FIR2IR
- [`KT-56225`](https://youtrack.jetbrains.com/issue/KT-56225) K2. "BackendException: Backend Internal error: Exception during IR lowering" error on incorrect constructor in inline class
- [`KT-56769`](https://youtrack.jetbrains.com/issue/KT-56769) K2. Annotation applicability is ignored during compilation when there's use-site @target
- [`KT-56616`](https://youtrack.jetbrains.com/issue/KT-56616) K2: cannot infer Java array type properly
- [`KT-56506`](https://youtrack.jetbrains.com/issue/KT-56506) K1/K2 inconsistency: VAL_REASSIGNMENT on synthetic setter with different nullability
- [`KT-56665`](https://youtrack.jetbrains.com/issue/KT-56665) K2: false positive RECURSIVE_TYPEALIAS_EXPANSION
- [`KT-53966`](https://youtrack.jetbrains.com/issue/KT-53966) K2 does not support SAM conversions with condition into Java/Kotlin functional interfaces
- [`KT-56013`](https://youtrack.jetbrains.com/issue/KT-56013) K2. a set of errors about local properties are missing
- [`KT-56771`](https://youtrack.jetbrains.com/issue/KT-56771) FIR: Increment operator on qualified expressions leads to exception from resolve
- [`KT-56548`](https://youtrack.jetbrains.com/issue/KT-56548) K2: false positive overload resolution ambiguity for Java record constructor
- [`KT-56476`](https://youtrack.jetbrains.com/issue/KT-56476) K2: false positive NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY at inline fun use-site
- [`KT-56138`](https://youtrack.jetbrains.com/issue/KT-56138) K2: Illegal conversion of lambda with parameters to ExtensionFunction expected type
- [`KT-55966`](https://youtrack.jetbrains.com/issue/KT-55966) K2: Not enough information to infer type variable K if smartcast is used
- [`KT-55423`](https://youtrack.jetbrains.com/issue/KT-55423) K2: Implement CONTRACT_NOT_ALLOWED
- [`KT-53846`](https://youtrack.jetbrains.com/issue/KT-53846) K2 / Context receivers: ClassCastException on secondary constructor of class with context receiver
- [`KT-57029`](https://youtrack.jetbrains.com/issue/KT-57029) Per-file caches fail on local inline function in an inline function
- [`KT-57033`](https://youtrack.jetbrains.com/issue/KT-57033) Make KtClassLiteralExpression stub based
- [`KT-57035`](https://youtrack.jetbrains.com/issue/KT-57035) Make KtCollectionLiteralExpression stub based
- [`KT-40857`](https://youtrack.jetbrains.com/issue/KT-40857) Invalid parameterized types for extension function on parameterized receiver when javaParameters=true
- [`KT-56154`](https://youtrack.jetbrains.com/issue/KT-56154) Compiler backend crash on reference to Java synthetic property from generic class
- [`KT-55879`](https://youtrack.jetbrains.com/issue/KT-55879) Modularized tests: fir.bench.language.version is used as API version, not language version
- [`KT-55886`](https://youtrack.jetbrains.com/issue/KT-55886) K2: Wrong code location mapping with Windows line endings
- [`KT-51821`](https://youtrack.jetbrains.com/issue/KT-51821) ClassCastException on anonymous fun interface implementation when unrelated vararg is used
- [`KT-57053`](https://youtrack.jetbrains.com/issue/KT-57053) Problem around anonymous objects in inline functions
- [`KT-56579`](https://youtrack.jetbrains.com/issue/KT-56579) [K2/N] IR actualizer crashed with K2 on expect annotation marked with `@OptionalExpectation`, without actual.
- [`KT-56750`](https://youtrack.jetbrains.com/issue/KT-56750) K2: "IllegalArgumentException: No argument for parameter VALUE_PARAMETER" when calling typealias method reference
- [`KT-55614`](https://youtrack.jetbrains.com/issue/KT-55614) K2: consider serializing static enum members (values/valueOf/entries) to match K1 behavior
- [`KT-30507`](https://youtrack.jetbrains.com/issue/KT-30507) Unsound smartcast if null assignment inside index place and plusAssign/minusAssign is used
- [`KT-56646`](https://youtrack.jetbrains.com/issue/KT-56646) K2: "IllegalStateException: No single implementation found for: FUN FAKE_OVERRIDE" when compiling a functional interface
- [`KT-56514`](https://youtrack.jetbrains.com/issue/KT-56514) K2 should report ACTUAL_TYPE_ALIAS_NOT_TO_CLASS
- [`KT-56522`](https://youtrack.jetbrains.com/issue/KT-56522) K2 should report ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS
- [`KT-56542`](https://youtrack.jetbrains.com/issue/KT-56542) K2: false positive TOO_MANY_ARGUMENTS in VarHandle.set call
- [`KT-56861`](https://youtrack.jetbrains.com/issue/KT-56861) FIR: test FirPluginBlackBoxCodegenTestGenerated.testClassWithAllPropertiesConstructor is failing with runtime error
- [`KT-56234`](https://youtrack.jetbrains.com/issue/KT-56234) K2: "ISE: Expected value generated with NEW" with inline property setter and noinline parameter
- [`KT-56722`](https://youtrack.jetbrains.com/issue/KT-56722) K2: cannot resolve component call after smart cast
- [`KT-56714`](https://youtrack.jetbrains.com/issue/KT-56714) K2: wrong argument mapping in DSL
- [`KT-56723`](https://youtrack.jetbrains.com/issue/KT-56723) K2: lambda accidentally returns Unit? instead of Unit
- [`KT-56847`](https://youtrack.jetbrains.com/issue/KT-56847) Unresolved reference to Java annotation in Kotlin class with the same name packages
- [`KT-55877`](https://youtrack.jetbrains.com/issue/KT-55877) K2: Secondary constructor without call to parent: no frontend error, ISE: "Null argument in ExpressionCodegen for parameter VALUE_PARAMETER"
- [`KT-56386`](https://youtrack.jetbrains.com/issue/KT-56386) K2: Make possible to access Java field which is shadowed by Kotlin invisible property`
- [`KT-56862`](https://youtrack.jetbrains.com/issue/KT-56862) Compatibility problem with using Kotlin in Intellij 223 or higher because of missing particular trove4j dependency
- [`KT-55088`](https://youtrack.jetbrains.com/issue/KT-55088) JS, Native compilation fail with internal error on `SomeEnum.entries` reference when `SomeEnum` is from klib compiled with disabled EnumEntries language feature
- [`KT-40904`](https://youtrack.jetbrains.com/issue/KT-40904) No warning when declare actual in the same target (module) as expect
- [`KT-56707`](https://youtrack.jetbrains.com/issue/KT-56707) K2: Unexpected TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM where only getter type specified explicitly
- [`KT-56508`](https://youtrack.jetbrains.com/issue/KT-56508) Context receivers: Internal compiler error when compiling code containing a class with a secondary constructor
- [`KT-56505`](https://youtrack.jetbrains.com/issue/KT-56505) K2: Missing `NO_EXPLICIT_VISIBILITY_IN_API_MODE` errors on various declarations
- [`KT-56215`](https://youtrack.jetbrains.com/issue/KT-56215) JVM: Object extension function nullable receiver﻿ null check false negative when object is null
- [`KT-56188`](https://youtrack.jetbrains.com/issue/KT-56188) K/N: AssertionError when casting SAM wrapper with generic type parameter
- [`KT-56033`](https://youtrack.jetbrains.com/issue/KT-56033) Restore 'isMostPreciseContravariantArgument' function signature for compatibility
- [`KT-56612`](https://youtrack.jetbrains.com/issue/KT-56612) K2: false positive NO_TYPE_ARGUMENTS_ON_RHS on raw cast with type alias based argument
- [`KT-56701`](https://youtrack.jetbrains.com/issue/KT-56701) K2 (with LightTree) reports syntax errors without additional information
- [`KT-56649`](https://youtrack.jetbrains.com/issue/KT-56649) K2 uses 0-index for line numbers rather than 1-index
- [`KT-56445`](https://youtrack.jetbrains.com/issue/KT-56445) K2: False-positive unresolved reference to callable reference to function with default argument
- [`KT-55024`](https://youtrack.jetbrains.com/issue/KT-55024) K2: overload resolution ambiguity/unresolved reference if variable is smart-casted to an invisible internal class
- [`KT-55722`](https://youtrack.jetbrains.com/issue/KT-55722) K2: Incorrect OVERLOAD_RESOLUTION_AMBIGUITY with smart cast on dispatch receiver (simple)
- [`KT-56563`](https://youtrack.jetbrains.com/issue/KT-56563) Inference within if stops working when changing expected type from Any to a different type
- [`KT-55936`](https://youtrack.jetbrains.com/issue/KT-55936) K2: Support proper resolution of callable references as last statements in lambda
- [`KT-45989`](https://youtrack.jetbrains.com/issue/KT-45989) FIR: wrong callable reference type inferred
- [`KT-55217`](https://youtrack.jetbrains.com/issue/KT-55217) `Appendable::append` reference resolve inconsistencies
- [`KT-55169`](https://youtrack.jetbrains.com/issue/KT-55169) K2: False-negative NO_ELSE_IN_WHEN
- [`KT-55932`](https://youtrack.jetbrains.com/issue/KT-55932) K2. No compiler error when elvis operator returns not matched type
- [`KT-53987`](https://youtrack.jetbrains.com/issue/KT-53987) K2: False negative "TYPE_MISMATCH" with if statement return
- [`KT-55436`](https://youtrack.jetbrains.com/issue/KT-55436) K1: implement warning about shadowing of the derived property by the base class field
- [`KT-56521`](https://youtrack.jetbrains.com/issue/KT-56521) Static scope initializers sometimes not called when first accessed from interop
- [`KT-41038`](https://youtrack.jetbrains.com/issue/KT-41038) NI: TYPE_MISMATCH when passing constructor of nested class
- [`KT-42449`](https://youtrack.jetbrains.com/issue/KT-42449) Can not resolve property for value of type Any even after casting type to a type with star projection
- [`KT-52934`](https://youtrack.jetbrains.com/issue/KT-52934) StackOverflow from `PseudocodeTraverserKt.collectDataFromSubgraph` with `if` inside `finally`
- [`KT-52860`](https://youtrack.jetbrains.com/issue/KT-52860) StackOverflowError when casting involving recursive generics and star projection
- [`KT-52424`](https://youtrack.jetbrains.com/issue/KT-52424) ClassCastException: Wrong smartcast to Nothing? with if-else in nullable lambda parameter
- [`KT-52262`](https://youtrack.jetbrains.com/issue/KT-52262) TYPE_MISMATCH: Nonnull smartcasting fails with non-exhaustive when
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
- [`KT-56073`](https://youtrack.jetbrains.com/issue/KT-56073) K2: build Exposed
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
- [`KT-54478`](https://youtrack.jetbrains.com/issue/KT-54478) @NoInfer causes CONFLICTING_OVERLOADS
- [`KT-56472`](https://youtrack.jetbrains.com/issue/KT-56472) K2: Add stack of all FIR elements to CheckerContext
- [`KT-41126`](https://youtrack.jetbrains.com/issue/KT-41126) [FIR] Inconsistency of a compiler behaviour at init block for an enum entry with and without a qualifier name
- [`KT-54931`](https://youtrack.jetbrains.com/issue/KT-54931) Annotations defined in nested classes cannot be instantiated directly
- [`KT-24901`](https://youtrack.jetbrains.com/issue/KT-24901) No smart cast for `when` with early return
- [`KT-7389`](https://youtrack.jetbrains.com/issue/KT-7389) No intersection type for type parameter with multiple upper bounds in star projection
- [`KT-53086`](https://youtrack.jetbrains.com/issue/KT-53086) "Cannot access '<init>' before superclass constructor has been called" with inner class secondary constructor
- [`KT-55137`](https://youtrack.jetbrains.com/issue/KT-55137) Callable references with conversion are incorrectly allowed to be promoted to KFunction
- [`KT-30497`](https://youtrack.jetbrains.com/issue/KT-30497) EXACTLY_ONCE contract doesn't work in a function with `vararg` parameter
- [`KT-47074`](https://youtrack.jetbrains.com/issue/KT-47074) Front-end Internal error: Failed to analyze declaration State / java.lang.IllegalStateException: Should not be called! when try to add Parcelize
- [`KT-24503`](https://youtrack.jetbrains.com/issue/KT-24503) Return-as-expression is allowed as this/super constructor parameter
- [`KT-55379`](https://youtrack.jetbrains.com/issue/KT-55379) False positive NO_ELSE_IN_WHEN with smartcast to Boolean
- [`KT-47750`](https://youtrack.jetbrains.com/issue/KT-47750) False positive NO_ELSE_IN_WHEN in presence of smartcast to sealed interface
- [`KT-53819`](https://youtrack.jetbrains.com/issue/KT-53819) False positive UNINITIALIZED_VARIABLE with secondary constructor and custom property getter in local class
- [`KT-56457`](https://youtrack.jetbrains.com/issue/KT-56457) JVM: Enum.entries are not annotated with @NotNull
- [`KT-56072`](https://youtrack.jetbrains.com/issue/KT-56072) K2. "IllegalStateException: Fir2IrSimpleFunctionSymbol for <paramName> is already bound" when trying to access java synthetic property of inherited class
- [`KT-50082`](https://youtrack.jetbrains.com/issue/KT-50082) Kotlin non-overriding property of subclass doesn't shadow same-named Java field from base class
- [`KT-55822`](https://youtrack.jetbrains.com/issue/KT-55822) False positive ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED with raw types and mixed overridden members
- [`KT-55666`](https://youtrack.jetbrains.com/issue/KT-55666) K2: label on local function is rejected: "Target label does not denote a function"
- [`KT-56283`](https://youtrack.jetbrains.com/issue/KT-56283) False-positive INVISIBLE_MEMBER on overridden member of more specific type after smart cast
- [`KT-51969`](https://youtrack.jetbrains.com/issue/KT-51969) [FIR] Compilation for expect value class fails with "Fir2IrSimpleFunctionSymbol for [declaration] is already bound"
- [`KT-56061`](https://youtrack.jetbrains.com/issue/KT-56061) K1 does not report error on inconsistent synthetic property assignment
- [`KT-55125`](https://youtrack.jetbrains.com/issue/KT-55125) Difference in generated bytecode for open suspend functions of generic classes
- [`KT-55295`](https://youtrack.jetbrains.com/issue/KT-55295) K2/MPP: JS build functionality
- [`KT-55909`](https://youtrack.jetbrains.com/issue/KT-55909) [K2/N] IndexOutOfBoundsException for a reference to a function defined in companion object superclass
- [`KT-55664`](https://youtrack.jetbrains.com/issue/KT-55664) K2: eliminate ClassId.isSame call from FirClass.isSubclassOf
- [`KT-55747`](https://youtrack.jetbrains.com/issue/KT-55747) K2. "Convention for 'mod' is forbidden. Use 'rem'" error is missing
- [`KT-56104`](https://youtrack.jetbrains.com/issue/KT-56104) Unnecessary inner classes attributes in class files for subclasses
- [`KT-55570`](https://youtrack.jetbrains.com/issue/KT-55570) K2: ACTUAL_WITHOUT_EXPECT error is not reported on a simple actual class
- [`KT-56176`](https://youtrack.jetbrains.com/issue/KT-56176) [K2/N] "IllegalStateException: actual type is kotlin.Int, expected kotlin.Long" when expected type uses typealias
- [`KT-56199`](https://youtrack.jetbrains.com/issue/KT-56199) K2 + MPP + kotlinx.serialization: java.lang.VerifyError: Bad type on operand stack in aaload
- [`KT-56212`](https://youtrack.jetbrains.com/issue/KT-56212) K2: Exception when compiling extension function declaration with illegally chained type parameter receiver
- [`KT-54140`](https://youtrack.jetbrains.com/issue/KT-54140) SOE at `IrBasedDescriptorsKt.makeKotlinType` with mixing recursive definitely not nullable type with nullability
- [`KT-56224`](https://youtrack.jetbrains.com/issue/KT-56224) Clarify message "Secondary constructors with bodies are reserved for for future releases" for secondary constructors in value classes with bodies
- [`KT-55503`](https://youtrack.jetbrains.com/issue/KT-55503) K2: "Argument type mismatch" caused by using the wrong "this"
- [`KT-56050`](https://youtrack.jetbrains.com/issue/KT-56050) K2: inconsistency regarding visibility of synthetic properties with protected getter and public setter
- [`KT-49663`](https://youtrack.jetbrains.com/issue/KT-49663) FIR: Support @kotlin.jvm.PurelyImplements for java collections
- [`KT-54507`](https://youtrack.jetbrains.com/issue/KT-54507) K2: Wrong `implicitModality` for interface in `FirHelpers`
- [`KT-55468`](https://youtrack.jetbrains.com/issue/KT-55468) [K2/N] Crash with debuginfo caused by changed tree using IMPLICIT_COERCION_TO_UNIT
- [`KT-56269`](https://youtrack.jetbrains.com/issue/KT-56269) [K2/N] Don't test "Tailrec is not allowed on open members" in K2
- [`KT-56172`](https://youtrack.jetbrains.com/issue/KT-56172) K2: Fix reporting of PRIVATE_CLASS_MEMBER_FROM_INLINE error
- [`KT-54647`](https://youtrack.jetbrains.com/issue/KT-54647) K2: Function call with Lambda on LHS of assignment leads to KotlinExceptionWithAttachments: FirDeclaration was not found for class org.jetbrains.kotlin.psi.KtFunctionLiteral
- [`KT-54648`](https://youtrack.jetbrains.com/issue/KT-54648) K2: Function call on left side of erroneous assignment isn't resolved
- [`KT-55171`](https://youtrack.jetbrains.com/issue/KT-55171) Put new contracts syntax under a feature flag
- [`KT-55699`](https://youtrack.jetbrains.com/issue/KT-55699) K2. False Negative "Type parameter T is not an expression"
- [`KT-55973`](https://youtrack.jetbrains.com/issue/KT-55973) K2: Exception from UnusedChecker on an unused destructuring
- [`KT-56275`](https://youtrack.jetbrains.com/issue/KT-56275) K2 IDE: Missed error for enum super type
- [`KT-54775`](https://youtrack.jetbrains.com/issue/KT-54775) K2. "IllegalStateException: Expected FirResolvedTypeRef with ConeKotlinType but was FirImplicitTypeRefImpl <implicit>" exception on incorrect code
- [`KT-55528`](https://youtrack.jetbrains.com/issue/KT-55528) K2: CFA for property initialization analysis is not run for class initialization graphs
- [`KT-54410`](https://youtrack.jetbrains.com/issue/KT-54410) K2: Deprecation warning instead of "this declaration is only available since Kotlin X" when language version in project are below required to use language feature
- [`KT-55186`](https://youtrack.jetbrains.com/issue/KT-55186) K2: No compilation error on calling exception without constructor
- [`KT-36776`](https://youtrack.jetbrains.com/issue/KT-36776) Treat special constructions (if, when, try) as a usual calls when there is expected type
- [`KT-54694`](https://youtrack.jetbrains.com/issue/KT-54694) Consider enabling BooleanElvisBoundSmartCasts in K1 or K2
- [`KT-54587`](https://youtrack.jetbrains.com/issue/KT-54587) K2. CCE on compilation when some operator fun is needed and it is implemented as an extension function for another class
- [`KT-54417`](https://youtrack.jetbrains.com/issue/KT-54417) K2: move receiver-targeted annotations to KtReceiverParameterSymbol and remove it from FirProperty receiver type
- [`KT-33917`](https://youtrack.jetbrains.com/issue/KT-33917) Prohibit to expose anonymous types from private inline functions

### IDE

#### Performance Improvements

- [`KT-56613`](https://youtrack.jetbrains.com/issue/KT-56613) Reduce memory consumption of light classes

#### Fixes

- [`KT-57849`](https://youtrack.jetbrains.com/issue/KT-57849) K2: contract violation due to implicit java type with annotation
- [`KT-57857`](https://youtrack.jetbrains.com/issue/KT-57857) LC: FakeFileForLightClass: Read access is allowed from inside read-action
- [`KT-57578`](https://youtrack.jetbrains.com/issue/KT-57578) SLC: incorrect upper bound wildcards
- [`KT-57917`](https://youtrack.jetbrains.com/issue/KT-57917) Analysis API: decompiled value parameters are not resolved
- [`KT-56046`](https://youtrack.jetbrains.com/issue/KT-56046) K2 IDE: Avoid redundant resolve from annotations
- [`KT-57569`](https://youtrack.jetbrains.com/issue/KT-57569) SLC: incorrect visibility for lateinit var with private setter
- [`KT-57547`](https://youtrack.jetbrains.com/issue/KT-57547) SLC: non-last `vararg` value parameter type mismatch
- [`KT-57548`](https://youtrack.jetbrains.com/issue/KT-57548) SLC: incorrect inheritance list for Comparator
- [`KT-57579`](https://youtrack.jetbrains.com/issue/KT-57579) SLC: unboxed type argument as method return type
- [`KT-56843`](https://youtrack.jetbrains.com/issue/KT-56843) Light classes: certain kinds of constant values in property initializers aren't supported
- [`KT-56868`](https://youtrack.jetbrains.com/issue/KT-56868) SLC: IncorrectOperationException on enum annotation arguments that are not valid Java identifiers
- [`KT-56833`](https://youtrack.jetbrains.com/issue/KT-56833) Light classes: Accessors to lateinit properties don't have @NotNull annotations
- [`KT-56845`](https://youtrack.jetbrains.com/issue/KT-56845) Light classes: Overridden property accessors don't have @Override annotation
- [`KT-56441`](https://youtrack.jetbrains.com/issue/KT-56441) K2 IDE: reference from Java to ObjectName.INSTANCE of private object is red in IDE, but compiled successfully
- [`KT-56891`](https://youtrack.jetbrains.com/issue/KT-56891) Symbol Classes: DefaultImpls classes contain methods without default implementation
- [`KT-56842`](https://youtrack.jetbrains.com/issue/KT-56842) Light Classes: Primitive-backed context receiver parameters shouldn't be marked with @NotNull
- [`KT-56835`](https://youtrack.jetbrains.com/issue/KT-56835) Light classes: Underlying fields for delegated properties should be marked as final and @NotNull
- [`KT-56840`](https://youtrack.jetbrains.com/issue/KT-56840) Light Classes: Inline classes backed by Java primitives shouldn't be marked with @NotNull
- [`KT-56728`](https://youtrack.jetbrains.com/issue/KT-56728) K2 IDE. False positive `not applicable to` for kotlin annotation with target annotating Java element
- [`KT-55815`](https://youtrack.jetbrains.com/issue/KT-55815) SLC: Keep annotations on type when converting to `PsiType`
- [`KT-55669`](https://youtrack.jetbrains.com/issue/KT-55669) K2 IDE: INRE from light classes
- [`KT-55150`](https://youtrack.jetbrains.com/issue/KT-55150) Argument for @NotNull parameter 'scope' of org/jetbrains/kotlin/resolve/AnnotationResolverImpl.resolveAnnotationType must not be null

### IDE. Misc

- [`KT-58763`](https://youtrack.jetbrains.com/issue/KT-58763) K2 IDE: NoSuchMethodError: KtPsiFactory$Companion.contextual

### IDE. Script

- [`KT-56632`](https://youtrack.jetbrains.com/issue/KT-56632) Script configuration cannot be loaded for embedded code snippets

### JavaScript

#### New Features

- [`KT-12784`](https://youtrack.jetbrains.com/issue/KT-12784) JS: generate ES2015 compatible modules
- [`KT-48154`](https://youtrack.jetbrains.com/issue/KT-48154) KJS / IR: Inline members support for external types
- [`KT-51582`](https://youtrack.jetbrains.com/issue/KT-51582) FIR: support basic compile-time evaluation for JS backend

#### Fixes

- [`KT-39650`](https://youtrack.jetbrains.com/issue/KT-39650) KJS IR: provide a way to enable ES2015 class generation
- [`KT-57990`](https://youtrack.jetbrains.com/issue/KT-57990) KJS/IR. Invalid `super` call for final parent methods (ES classes)
- [`KT-58246`](https://youtrack.jetbrains.com/issue/KT-58246) KJS: ES15 classses — duplicated code in class constructor
- [`KT-57479`](https://youtrack.jetbrains.com/issue/KT-57479) KJS: Add an annotation for a function parameter which checks that a passed argument has an external type
- [`KT-51706`](https://youtrack.jetbrains.com/issue/KT-51706) Partial linkage: in case of absent symbol referred from declaration Native compiler is successful, JavaScript fails
- [`KT-56237`](https://youtrack.jetbrains.com/issue/KT-56237) KJS + IC: Adding or removing interface default implementation doesn't invalidate children and doesn't update JS code
- [`KT-54638`](https://youtrack.jetbrains.com/issue/KT-54638) K2/JS: Fir2ir - implement and use JS-specific mangler
- [`KT-54028`](https://youtrack.jetbrains.com/issue/KT-54028) Native / JS: Using private object implementing a sealed interface causes a linker error
- [`KT-57423`](https://youtrack.jetbrains.com/issue/KT-57423) KJS: Add an annotation for external interfaces which allows to be inherited only by other external interfaces, classes or objects
- [`KT-57711`](https://youtrack.jetbrains.com/issue/KT-57711) K2: Native & JS fail to compile a KLIB that uses const val from a dependency KLIB
- [`KT-43490`](https://youtrack.jetbrains.com/issue/KT-43490) KJS / IR: "Cannot set property message of Error which has only a getter" caused by class that is child of Throwable
- [`KT-57078`](https://youtrack.jetbrains.com/issue/KT-57078) JS IC: Unbound symbol left in `SymbolTable` in `JsIr[ES6]InvalidationTestGenerated.testBreakKlibBinaryCompatibilityWithVariance` tests
- [`KT-54452`](https://youtrack.jetbrains.com/issue/KT-54452) Kotlin/JS libraries with "joined" legacy+IR content: publish IR sources for them
- [`KT-57254`](https://youtrack.jetbrains.com/issue/KT-57254) Deprecate `external enum` declarations
- [`KT-57002`](https://youtrack.jetbrains.com/issue/KT-57002) KJS: "JsParserException: missing name after . operator" when a js(...) block contains an interpolated constant
- [`KT-53180`](https://youtrack.jetbrains.com/issue/KT-53180) Kotlin/JS: generated TypeScript constructor can have "TS1016: A required parameter cannot follow an optional parameter" error with certain properties order
- [`KT-56961`](https://youtrack.jetbrains.com/issue/KT-56961) JS IR: serializedIrFileFingerprints in klib manifest has a wrong format
- [`KT-56282`](https://youtrack.jetbrains.com/issue/KT-56282) KJS: Invalidate incremental cache in case of compiler internal errors
- [`KT-55720`](https://youtrack.jetbrains.com/issue/KT-55720) KJS: `ReferenceError: SuspendFunction1 is not defined` with 1.8 when importing`kotlin.coroutines.SuspendFunction1`
- [`KT-56469`](https://youtrack.jetbrains.com/issue/KT-56469) KJS: BE Incremental rebuild spoils source map comment

### KMM Plugin

- [`KT-55402`](https://youtrack.jetbrains.com/issue/KT-55402) "Framework not found SQLCipher": after selection of "Regular framework" as "iOS framework distribution" and installing SqlCihper through CocoaPods
- [`KT-55988`](https://youtrack.jetbrains.com/issue/KT-55988) KN debugger in KMM plugin for Android Studio can't recognize the source code

### Language Design

- [`KT-53778`](https://youtrack.jetbrains.com/issue/KT-53778) Release stdlib API about "rangeUntil" operator in 1.9
- [`KT-53653`](https://youtrack.jetbrains.com/issue/KT-53653) Export Enum.entries to Objective-C and Swift
- [`KT-55177`](https://youtrack.jetbrains.com/issue/KT-55177) Deprecate declaration of expect and actual counterparts of same class in one module
- [`KT-28850`](https://youtrack.jetbrains.com/issue/KT-28850) Prohibit protected visibility in final expected classes
- [`KT-39362`](https://youtrack.jetbrains.com/issue/KT-39362) Expect fun interface must have actual fun interface counterpart
- [`KT-57395`](https://youtrack.jetbrains.com/issue/KT-57395) Delay ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound feature to LV 2.0
- [`KT-55082`](https://youtrack.jetbrains.com/issue/KT-55082) Bump KLib version for Enum.entries

### Libraries

#### New Features

- [`KT-58046`](https://youtrack.jetbrains.com/issue/KT-58046) Stabilize remaining kotlin.time API: time sources, time marks, measureTime
- [`KT-58074`](https://youtrack.jetbrains.com/issue/KT-58074) Stabilization of Atomics API in K/N
- [`KT-55268`](https://youtrack.jetbrains.com/issue/KT-55268) Mutiplatform @Volatile annotation
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

- [`KT-55612`](https://youtrack.jetbrains.com/issue/KT-55612) Stabilize experimental API for 1.9
- [`KT-57728`](https://youtrack.jetbrains.com/issue/KT-57728) Explicitly specify level of stability of programmatically-accessible interoperability API
- [`KT-58548`](https://youtrack.jetbrains.com/issue/KT-58548) Stabilize standard library API for Enum.entries
- [`KT-58276`](https://youtrack.jetbrains.com/issue/KT-58276) Deprecate redundant public declarations in kotlin.native.concurrent
- [`KT-57762`](https://youtrack.jetbrains.com/issue/KT-57762) Introduce HexFormat for formatting and parsing hexadecimals
- [`KT-57317`](https://youtrack.jetbrains.com/issue/KT-57317) Repack EnumEntries from stdlib into the compiler
- [`KT-54702`](https://youtrack.jetbrains.com/issue/KT-54702) Native: mark Worker and related APIs as obsolete
- [`KT-55610`](https://youtrack.jetbrains.com/issue/KT-55610) Deprecate kotlin.jvm.Volatile annotation in platforms except JVM
- [`KT-57404`](https://youtrack.jetbrains.com/issue/KT-57404) Native: Support AnnotationTarget.TYPE_PARAMETER
- [`KT-57318`](https://youtrack.jetbrains.com/issue/KT-57318) Change EnumEntries stdlib implementation to be eager
- [`KT-57137`](https://youtrack.jetbrains.com/issue/KT-57137) Native: Consider removing ArrayAsList
- [`KT-55935`](https://youtrack.jetbrains.com/issue/KT-55935) [Kotlin/JVM] Path.copyToRecursively does not work across file systems
- [`KT-51579`](https://youtrack.jetbrains.com/issue/KT-51579) PlatformImplementations loading is not compatible with graalvm native-image --no-fallback

### Native

- [`KT-52594`](https://youtrack.jetbrains.com/issue/KT-52594) Provide Alpha support for Native in the K2 platform
- [`KT-56443`](https://youtrack.jetbrains.com/issue/KT-56443) Native link task reports w: Cached libraries will not be used for optimized compilation
- [`KT-56071`](https://youtrack.jetbrains.com/issue/KT-56071) K2/MPP: Native build functionality
- [`KT-56218`](https://youtrack.jetbrains.com/issue/KT-56218) [K2/N] Receiver annotations for properties are not serialized
- [`KT-56326`](https://youtrack.jetbrains.com/issue/KT-56326) [K2/N] RemoveRedundantCallsToStaticInitializersPhase removes important static initializer
- [`KT-54098`](https://youtrack.jetbrains.com/issue/KT-54098) Decommission and remove 'enableEndorsedLibs' flag from Gradle setup

### Native. Build Infrastructure

- [`KT-58160`](https://youtrack.jetbrains.com/issue/KT-58160) Native: performance build configuration fails with NoSuchMethodError: 'boolean kotlinx.coroutines.CompletableDeferredKt.completeWith(kotlinx.coroutines.CompletableDeferred, java.lang.Object)'

### Native. C and ObjC Import

- [`KT-54610`](https://youtrack.jetbrains.com/issue/KT-54610) Kotlin Native can't call `objc_direct` functions
- [`KT-54805`](https://youtrack.jetbrains.com/issue/KT-54805) KMP ios memory leak when using CA Layer
- [`KT-57490`](https://youtrack.jetbrains.com/issue/KT-57490) [K/N] Duplicate package names for cinterop klibs with objc protocols fails to link
- [`KT-57541`](https://youtrack.jetbrains.com/issue/KT-57541) Compilation fails without explicit cast on cinterop code

### Native. ObjC Export

- [`KT-56464`](https://youtrack.jetbrains.com/issue/KT-56464) K/N: Allow HiddenFromObjC for classes
- [`KT-57507`](https://youtrack.jetbrains.com/issue/KT-57507) K2: Set of Objc exported declarations is different between K1 and K2

### Native. Runtime

- [`KT-58441`](https://youtrack.jetbrains.com/issue/KT-58441) Kotlin/Native: @ObjCAction @ObjCOutlet generate bridges without switching state
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

- [`KT-56650`](https://youtrack.jetbrains.com/issue/KT-56650) ArrayStoreException from InlineClassAwareCaller.call with an array of inline class
- [`KT-56093`](https://youtrack.jetbrains.com/issue/KT-56093) Metaspace leak in a Gradle plugin built with Kotlin 1.8.0
- [`KT-27585`](https://youtrack.jetbrains.com/issue/KT-27585) Flaky IllegalPropertyDelegateAccessException: Cannot obtain the delegate of a non-accessible property. Use "isAccessible = true" to make the property accessible
- [`KT-55937`](https://youtrack.jetbrains.com/issue/KT-55937) Optimize implementation of kotlinFunction/kotlinProperty
- [`KT-54833`](https://youtrack.jetbrains.com/issue/KT-54833) Reflection: Incorrect behaviour for Field.kotlinProperty function in companion objects

### Tools. CLI

- [`KT-56209`](https://youtrack.jetbrains.com/issue/KT-56209) Add CLI support for HMPP in K2
- [`KT-57495`](https://youtrack.jetbrains.com/issue/KT-57495) Add JVM target bytecode version 20
- [`KT-56789`](https://youtrack.jetbrains.com/issue/KT-56789) Metaspace memory leak in CoreJrtFileSystem
- [`KT-57077`](https://youtrack.jetbrains.com/issue/KT-57077) `1.8.20-RC-243` shows Java 19 warnings even if configured with Java 17 toolchain
- [`KT-56992`](https://youtrack.jetbrains.com/issue/KT-56992) Performance test regression in Gradle when switching to Kotlin 1.8.20
- [`KT-57644`](https://youtrack.jetbrains.com/issue/KT-57644) K2: Prohibit passing HMPP module structure with CLI arguments to metadata compiler
- [`KT-56925`](https://youtrack.jetbrains.com/issue/KT-56925) Remove warning about assignment plugin
- [`KT-56351`](https://youtrack.jetbrains.com/issue/KT-56351) Reduce memory usage spent on compiler settings

### Tools. Commonizer

- [`KT-57796`](https://youtrack.jetbrains.com/issue/KT-57796) NoSuchFileException in :module-B:commonizeCInterop with Kotlin 1.8.20
- [`KT-56207`](https://youtrack.jetbrains.com/issue/KT-56207) Investigate failing tests in ClassifierCommonizationFromSourcesTest

### Tools. Compiler Plugins

#### Fixes

- [`KT-57406`](https://youtrack.jetbrains.com/issue/KT-57406) FIR Compiler plugins: Assignment plugin incorrectly recognizes qualified names of annotations
- [`KT-57400`](https://youtrack.jetbrains.com/issue/KT-57400) FIR Compiler Plugins: `annotated` predicate does not work with Java classes
- [`KT-57140`](https://youtrack.jetbrains.com/issue/KT-57140) K2: Implement backwards compatibility for FirFunctionTypeKindExtension
- [`KT-55375`](https://youtrack.jetbrains.com/issue/KT-55375) Remove "legacy" mode of jvm-abi-gen plugin
- [`KT-53470`](https://youtrack.jetbrains.com/issue/KT-53470) FIR: pass `MemberGenerationContext` to all methods of FirDeclarationGenerationExtension
- [`KT-51092`](https://youtrack.jetbrains.com/issue/KT-51092) Lombok @Value causes IllegalAccessError
- [`KT-56487`](https://youtrack.jetbrains.com/issue/KT-56487) Add more methods to DescriptorSerializerPlugin
- [`KT-55885`](https://youtrack.jetbrains.com/issue/KT-55885) K2 plugin API: Backend-only declarations are not visible from other modules
- [`KT-55584`](https://youtrack.jetbrains.com/issue/KT-55584) K2: Improve registration of session components from compiler plugins
- [`KT-55843`](https://youtrack.jetbrains.com/issue/KT-55843) FIR Plugin API: metaAnnotated predicate returns meta-annotation itself as well
- [`KT-53874`](https://youtrack.jetbrains.com/issue/KT-53874) Optimize checking for plugin applicability and redesign DeclarationPredicates

### Tools. Compiler plugins. Serialization

- [`KT-58067`](https://youtrack.jetbrains.com/issue/KT-58067) Serialization: NullPointerException caused by @Contextual property with type with generic
- [`KT-57730`](https://youtrack.jetbrains.com/issue/KT-57730) Serialization: "IllegalStateException: Serializer for element of type <root>.Foo has not been found" caused by serialization of Java type
- [`KT-56594`](https://youtrack.jetbrains.com/issue/KT-56594) K2/serialization reports SERIALIZER_NOT_FOUND over aliased String or primitive types
- [`KT-56990`](https://youtrack.jetbrains.com/issue/KT-56990) "AssertionError: SyntheticAccessorLowering should not attempt to modify other files" in kotlinx-serialization using @Serializer and List argument
- [`KT-56553`](https://youtrack.jetbrains.com/issue/KT-56553) Support 'serialization plugin intrinsics' feature in K2
- [`KT-56244`](https://youtrack.jetbrains.com/issue/KT-56244) kotlinx.serialization compiler intrinsic does not work with encodeToString function in 1.8.0

### Tools. Daemon

- [`KT-50846`](https://youtrack.jetbrains.com/issue/KT-50846) Remove "new" Kotlin daemon from codebase

### Tools. Gradle

#### New Features

- [`KT-57159`](https://youtrack.jetbrains.com/issue/KT-57159) Add project level compiler options for Kotlin/JVM plugin
- [`KT-56971`](https://youtrack.jetbrains.com/issue/KT-56971) Expose jvmTargetValidationMode property in KotlinCompile Gradle task

#### Performance Improvements

- [`KT-57052`](https://youtrack.jetbrains.com/issue/KT-57052) Gradle: Stop using exceptions for flow control
- [`KT-57757`](https://youtrack.jetbrains.com/issue/KT-57757) Reduce classpath snapshotter memory consumption
- [`KT-56052`](https://youtrack.jetbrains.com/issue/KT-56052) Implement an in-memory wrapper for PersistentHashMap to avoid applying changes to IC caches before successful compilation

#### Fixes

- [`KT-58745`](https://youtrack.jetbrains.com/issue/KT-58745) KaptGenerateStubs task should also be configured with the same compiler plugin options
- [`KT-58571`](https://youtrack.jetbrains.com/issue/KT-58571) ExplicitApi mode should not apply for test compilations
- [`KT-52811`](https://youtrack.jetbrains.com/issue/KT-52811) Kotlin Serialization metadata issue due to incompatibility between Gradle Kotlin embedded version and Kotlin Gradle Plugin version
- [`KT-57224`](https://youtrack.jetbrains.com/issue/KT-57224) Add an indicator into build metrics report to show whether K1 or K2 compiler was used to compile the code
- [`KT-58682`](https://youtrack.jetbrains.com/issue/KT-58682) Explicit api mode does not apply in MPP projects
- [`KT-57736`](https://youtrack.jetbrains.com/issue/KT-57736) K2: Introduce an easy way to try K2 compiler in Gradle user projects
- [`KT-52976`](https://youtrack.jetbrains.com/issue/KT-52976) Remove deprecated Gradle conventions usages
- [`KT-58530`](https://youtrack.jetbrains.com/issue/KT-58530) Compiler plugin unbundling changes should be backward compatible with Kotlin plugin
- [`KT-36904`](https://youtrack.jetbrains.com/issue/KT-36904) Adding folders to sourceSets.resources.srcDir() in Gradle script does not work
- [`KT-57330`](https://youtrack.jetbrains.com/issue/KT-57330) Provide collection of usage statistics for the Dokka
- [`KT-58619`](https://youtrack.jetbrains.com/issue/KT-58619) Move all pm20 interfaces into Gradle plugin codebase
- [`KT-58320`](https://youtrack.jetbrains.com/issue/KT-58320) Kotlin daemon OOM help message is missing on OOM in Kotlin Daemon itself
- [`KT-58167`](https://youtrack.jetbrains.com/issue/KT-58167) Applying KAPT plugin to Gradle 8.1 causes configuration cache miss
- [`KT-53923`](https://youtrack.jetbrains.com/issue/KT-53923) Add 'progressive' compiler argument to Gradle compiler options
- [`KT-53924`](https://youtrack.jetbrains.com/issue/KT-53924) Add 'optIn' compiler arguments to Gradle compiler options
- [`KT-53748`](https://youtrack.jetbrains.com/issue/KT-53748) Remove KotlinCompile setClasspath/getClasspath methods
- [`KT-56454`](https://youtrack.jetbrains.com/issue/KT-56454) Bump minimal support AGP version to 4.2.2
- [`KT-57767`](https://youtrack.jetbrains.com/issue/KT-57767) Gradle: "ZipException: invalid entry size" with 1.8.20
- [`KT-57397`](https://youtrack.jetbrains.com/issue/KT-57397) Add infrastructure to use the build-tools-api to run compilation from Gradle
- [`KT-56946`](https://youtrack.jetbrains.com/issue/KT-56946) Switch incremental Gradle tests for K2 to use language version 2.0
- [`KT-55624`](https://youtrack.jetbrains.com/issue/KT-55624) Update KGP integration tests that use removed in Gradle 8 getClassifier method
- [`KT-57782`](https://youtrack.jetbrains.com/issue/KT-57782) Disable daemon fallback strategy for Gradle integration tests by default
- [`KT-56645`](https://youtrack.jetbrains.com/issue/KT-56645) Gradle: KGP reports an incorrect resources processing task name for JVM projects
- [`KT-55824`](https://youtrack.jetbrains.com/issue/KT-55824) Deprecate `commonMain.dependsOn(anything)` in user scripts
- [`KT-57142`](https://youtrack.jetbrains.com/issue/KT-57142) Split org.jetbrains.kotlin.gradle.tasks/Tasks.kt into several source files
- [`KT-56211`](https://youtrack.jetbrains.com/issue/KT-56211) Improve Kotlin build reports
- [`KT-54447`](https://youtrack.jetbrains.com/issue/KT-54447) Remove usage of deprecated internal Gradle field in Kotlin Gradle Plugin, replace with equivalent in public API
- [`KT-49785`](https://youtrack.jetbrains.com/issue/KT-49785) Avoid creating task output backups until really needed
- [`KT-56047`](https://youtrack.jetbrains.com/issue/KT-56047) False positive message about full recompilation is displayed while restoring from build cache and then making a syntax error
- [`KT-56421`](https://youtrack.jetbrains.com/issue/KT-56421) Gradle: plugin should not use BasePluginExtension deprecated properties
- [`KT-55241`](https://youtrack.jetbrains.com/issue/KT-55241) Gradle: the VariantImplementationFactories build service state is not persistent making impossible to access factories with configuration cache lazily
- [`KT-56414`](https://youtrack.jetbrains.com/issue/KT-56414) Dependency locking and failed builds with Kotlin 1.8.10
- [`KT-56357`](https://youtrack.jetbrains.com/issue/KT-56357) Gradle: "DefaultTaskCollection#configureEach(Action) on task set cannot be executed in the current context" because of VariantImplementationFactories
- [`KT-56352`](https://youtrack.jetbrains.com/issue/KT-56352) Make build scan reports more readable
- [`KT-52149`](https://youtrack.jetbrains.com/issue/KT-52149) Gradle: declare shared build services usages with `Task#usesService`
- [`KT-55741`](https://youtrack.jetbrains.com/issue/KT-55741) Gradle 8: Build service '*' is being used by task '*' without the corresponding declaration via 'Task#usesService'.
- [`KT-55174`](https://youtrack.jetbrains.com/issue/KT-55174) KotlinCompile task produces deprecation "Build service 'variant_impl_factories_...' is being used by task"
- [`KT-55972`](https://youtrack.jetbrains.com/issue/KT-55972) Gradle: Add an assertion to all integration tests if `warningMode` is not `FAIL`, but the build doesn't produce any warnings

### Tools. Gradle. Cocoapods

- [`KT-38749`](https://youtrack.jetbrains.com/issue/KT-38749) Support dependencies between pods when using the cocoapods plugin
- [`KT-54161`](https://youtrack.jetbrains.com/issue/KT-54161) Support adding extra code to generated Podfile from the Kotlin gradle plugin
- [`KT-49430`](https://youtrack.jetbrains.com/issue/KT-49430) Stop invalidating iOS framework generated by KMM module on each Gradle Sync
- [`KT-56304`](https://youtrack.jetbrains.com/issue/KT-56304) Podspec generated with new K/N artifact DSL contains wrong artifact names for static and dynamic libraries
- [`KT-56162`](https://youtrack.jetbrains.com/issue/KT-56162) Provide granular Gradle warnings suppression for CocoaPodsIT

### Tools. Gradle. JS

#### New Features

- [`KT-32209`](https://youtrack.jetbrains.com/issue/KT-32209) org.jetbrains.kotlin.js does not respect Gradle's archivesBaseName
- [`KT-56158`](https://youtrack.jetbrains.com/issue/KT-56158) KJS: Support implementation dependencies
- [`KT-48791`](https://youtrack.jetbrains.com/issue/KT-48791) KJS: Support for Power(ppc64le) and Z(s390x)

#### Fixes

- [`KT-57985`](https://youtrack.jetbrains.com/issue/KT-57985) K/JS: `packageJson` Gradle configurations don't inherit unique attributes from JsTarget DSL
- [`KT-57817`](https://youtrack.jetbrains.com/issue/KT-57817) JS: executables for couple of JS targets builds in the same directory
- [`KT-58199`](https://youtrack.jetbrains.com/issue/KT-58199) K/JS: Remove useCoverage method
- [`KT-57116`](https://youtrack.jetbrains.com/issue/KT-57116) KJS / Gradle: `commonWebpackConfig` not applied if called after `binaries.executable()`
- [`KT-58522`](https://youtrack.jetbrains.com/issue/KT-58522) K/JS: Upgrade NPM dependency versions
- [`KT-57629`](https://youtrack.jetbrains.com/issue/KT-57629) K/JS: Change default destination of JS production distribution
- [`KT-57480`](https://youtrack.jetbrains.com/issue/KT-57480) K/JS: Use IR compiler by default without explicit choosing of js compiler
- [`KT-58345`](https://youtrack.jetbrains.com/issue/KT-58345) K/JS: Webpack task skipped with ES modules because files have mjs extension
- [`KT-56458`](https://youtrack.jetbrains.com/issue/KT-56458) KJS / Gradle: Unnecessary and confusing "There are multiple versions of "kotlin" used in nodejs build" generated from `YarnImportedPackagesVersionResolver`
- [`KT-56690`](https://youtrack.jetbrains.com/issue/KT-56690) Kotlin2JsCompiler friendDependencies cannot be configured through friendPaths
- [`KT-57920`](https://youtrack.jetbrains.com/issue/KT-57920) K/JS: Make imported NPM package not considering dev dependencies
- [`KT-56025`](https://youtrack.jetbrains.com/issue/KT-56025) KJS / Gradle: Gradle 8.0 jsBrowserProductionWebpack uses the output of another project's jsProductionExecutableCompileSync
- [`KT-57630`](https://youtrack.jetbrains.com/issue/KT-57630) K/JS: webpack updating twice on one change of kt sources
- [`KT-47351`](https://youtrack.jetbrains.com/issue/KT-47351) KJS / IR: `:jsTestPackageJson` is unable to find nested included builds under composite build
- [`KT-57387`](https://youtrack.jetbrains.com/issue/KT-57387) Remove support of webpack 4
- [`KT-57386`](https://youtrack.jetbrains.com/issue/KT-57386) Kotlin/JS upgrade npm dependencies
- [`KT-56765`](https://youtrack.jetbrains.com/issue/KT-56765) K/JS: Several binaries use same cache directory
- [`KT-56488`](https://youtrack.jetbrains.com/issue/KT-56488) Debugger won't stop on breakpoints of JS browser test
- [`KT-56705`](https://youtrack.jetbrains.com/issue/KT-56705) KJS / Gradle: Module name starting with '@' isn't properly set when FUS is disabled
- [`KT-56719`](https://youtrack.jetbrains.com/issue/KT-56719) KJS / Gradle: Compile sync task has to sync only changed files
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

- [`KT-55881`](https://youtrack.jetbrains.com/issue/KT-55881) Add possibility to enable/disable sources publication similar to Java Gradle Plugin API

#### Fixes

- [`KT-55751`](https://youtrack.jetbrains.com/issue/KT-55751) MPP / Gradle: Consumable configurations must have unique attributes
- [`KT-49933`](https://youtrack.jetbrains.com/issue/KT-49933) Support Gradle Configuration caching with HMPP
- [`KT-58470`](https://youtrack.jetbrains.com/issue/KT-58470) Warning about using MPP libraries published in the legacy mode is not reported if the dependency is declared in an intermediate source set
- [`KT-58086`](https://youtrack.jetbrains.com/issue/KT-58086) Warn about using MPP libraries published in the legacy mode
- [`KT-58466`](https://youtrack.jetbrains.com/issue/KT-58466) K2 Gradle: non *.kt files are passed to -Xfragment-sources
- [`KT-58319`](https://youtrack.jetbrains.com/issue/KT-58319) kotlin.git: ProjectMetadataProviderImpl "Unexpected source set 'commonMain'"
- [`KT-58281`](https://youtrack.jetbrains.com/issue/KT-58281) Kotlin Gradle Plugin: Enable Kotlin/Android SourceSetLayout v2 by default
- [`KT-51940`](https://youtrack.jetbrains.com/issue/KT-51940) HMPP resolves configurations during configuration
- [`KT-41506`](https://youtrack.jetbrains.com/issue/KT-41506) UnknownDomainObjectException: "KotlinSourceSet with name not found" when creating custom compilations after applying withJava to an MPP JVM target
- [`KT-58209`](https://youtrack.jetbrains.com/issue/KT-58209) Do not use the term 'Module' in KotlinTargetHierarchy
- [`KT-56285`](https://youtrack.jetbrains.com/issue/KT-56285) TCS: Gradle Sync: IdeProjectToProjectCInteropDependencyResolver: Ensure lenient resolution
- [`KT-56571`](https://youtrack.jetbrains.com/issue/KT-56571) New import broke apiVersion for commonMain, commonTest and jvmAndAndroidMain modules
- [`KT-56536`](https://youtrack.jetbrains.com/issue/KT-56536) Multiplatform: Composite build fails on included build with rootProject.name != buildIdentifier.name
- [`KT-56841`](https://youtrack.jetbrains.com/issue/KT-56841) MPP: Module-to-module dependencies don't work inside included build in included build
- [`KT-42748`](https://youtrack.jetbrains.com/issue/KT-42748) Project that transitively depends on composite build of multimodule multiplatform library cannot resolve dependencies properly
- [`KT-52356`](https://youtrack.jetbrains.com/issue/KT-52356) MPP / Gradle: Missing common classes on KMM project integrated via Gradle included build into an Android application
- [`KT-51293`](https://youtrack.jetbrains.com/issue/KT-51293) Unresolved references with hierarchical project structure when building KotlinMetadata from native-common source set
- [`KT-56729`](https://youtrack.jetbrains.com/issue/KT-56729) commonizeCInterop: Duplicated libraries: co.touchlab:sqliter-driver-cinterop-sqlite3
- [`KT-56510`](https://youtrack.jetbrains.com/issue/KT-56510) Import with included plugin build may fail with OverlappingFileLockException during commonizeNativeDistribution
- [`KT-56700`](https://youtrack.jetbrains.com/issue/KT-56700) V2 MPP Source Set layout warnings should include link to docs
- [`KT-56115`](https://youtrack.jetbrains.com/issue/KT-56115) Multiplatform;Composite Builds: Support import with cinterop commonization enabled
- [`KT-56429`](https://youtrack.jetbrains.com/issue/KT-56429) Fix flaky: MppIdeDependencyResolutionIT.test cinterops - are stored in root gradle folder
- [`KT-56337`](https://youtrack.jetbrains.com/issue/KT-56337) Unable to import a project with cinterop with enableKgpDependencyResolution
- [`KT-55926`](https://youtrack.jetbrains.com/issue/KT-55926) TCS: Gradle Sync: Import Extras on KotlinSourceSet and KotlinTarget
- [`KT-55891`](https://youtrack.jetbrains.com/issue/KT-55891) Deprecate pre-HMPP flags
- [`KT-56278`](https://youtrack.jetbrains.com/issue/KT-56278) TCS: Gradle Sync: [MISSING_DEPENDENCY_CLASS] on libraries used in shared native source sets
- [`KT-55730`](https://youtrack.jetbrains.com/issue/KT-55730) MPP / Gradle: compileKotlinMetadata fails to resolve symbols in additional source sets
- [`KT-56204`](https://youtrack.jetbrains.com/issue/KT-56204) KotlinTargetHierarchy: Changing naming from 'any' to 'with' prefix
- [`KT-56111`](https://youtrack.jetbrains.com/issue/KT-56111) Multiplatform;Composite Builds: Clean builds fail on when 'hostSpecificMetadata' is required

### Tools. Gradle. Native

- [`KT-53108`](https://youtrack.jetbrains.com/issue/KT-53108) Expose Kotlin/Native compiler options as Gradle DSL
- [`KT-58063`](https://youtrack.jetbrains.com/issue/KT-58063) Kotlin/Native tasks configuration cache are not compatible with Gradle 8.1
- [`KT-38317`](https://youtrack.jetbrains.com/issue/KT-38317) Kotlin/Native: NSURLConnection HTTPS requests fail in iOS tests due to --standalone simctl flag
- [`KT-37051`](https://youtrack.jetbrains.com/issue/KT-37051) MPP Gradle plugin: duplicated cinterop libraries in composite build
- [`KT-56280`](https://youtrack.jetbrains.com/issue/KT-56280) Gradle: freeCompilerArgs are no longer propagated from compilations to Native binaries
- [`KT-56205`](https://youtrack.jetbrains.com/issue/KT-56205) Shared Native Compilation: False positive 'w: Could not find' warnings on metadata klibs

### Tools. Incremental Compile

- [`KT-58289`](https://youtrack.jetbrains.com/issue/KT-58289) IC fails to detect a change to class annotations
- [`KT-56197`](https://youtrack.jetbrains.com/issue/KT-56197) If use classpathSnapshot, the invoke place of subclass's super function who has default parameters will not recompiled if it is incremental build
- [`KT-56886`](https://youtrack.jetbrains.com/issue/KT-56886) K2: Changes to Java sources used in Kotlin project do not trigger a rebuild if a previous build was successful
- [`KT-55021`](https://youtrack.jetbrains.com/issue/KT-55021) New IC: "The following LookupSymbols are not yet converted to programSymbols" when removing/renaming file facades

### Tools. JPS

- [`KT-56438`](https://youtrack.jetbrains.com/issue/KT-56438) Add minimal statistic report for JPS build
- [`KT-58314`](https://youtrack.jetbrains.com/issue/KT-58314) kotlin-build-statistics is missing for kotlin-jps-plugin
- [`KT-55696`](https://youtrack.jetbrains.com/issue/KT-55696) JPS IC K2 - fix new failing tests with Java interop
- [`KT-57102`](https://youtrack.jetbrains.com/issue/KT-57102) SCE: DefaultErrorMessages$Extension: DefaultErrorMessagesWasm not a subtype after adding WASM error messages
- [`KT-56165`](https://youtrack.jetbrains.com/issue/KT-56165) Language version 1.9 and 2.0 is absent in Kotlin Compiler settings

### Tools. Kapt

- [`KT-57598`](https://youtrack.jetbrains.com/issue/KT-57598) K2: Support a fallback mode executing Kapt with K1 even when the compiler is run with languageVersion=2.0
- [`KT-54468`](https://youtrack.jetbrains.com/issue/KT-54468) KAPT Gradle plugin causes eager task creation
- [`KT-58027`](https://youtrack.jetbrains.com/issue/KT-58027) Kotlin 1.8.20 kapt issue "null: KtCallExpression: build()"
- [`KT-58226`](https://youtrack.jetbrains.com/issue/KT-58226) KAPT: “org.jetbrains.kotlin.psi2ir.generators.ErrorExpressionException: null: KtNameReferenceExpression” with enum with secondary constructor
- [`KT-56360`](https://youtrack.jetbrains.com/issue/KT-56360) Kapt with JVM IR changes fields order
- [`KT-54380`](https://youtrack.jetbrains.com/issue/KT-54380) Kapt / IR: Build failed when inheritance by functional interface with suspend modifier

### Tools. Maven

- [`KT-58101`](https://youtrack.jetbrains.com/issue/KT-58101) 'Unable to access class' in kotlin-maven-plugin after updating to Kotlin 1.8.20
- [`KT-58048`](https://youtrack.jetbrains.com/issue/KT-58048) Maven: "Too many source module declarations found" after upgrading to 1.8.20
- [`KT-13995`](https://youtrack.jetbrains.com/issue/KT-13995) Maven: Kotlin compiler plugin should respect model's compile source roots
- [`KT-55709`](https://youtrack.jetbrains.com/issue/KT-55709) Maven: "java.lang.reflect.InaccessibleObjectException: Unable to make field protected java.io.OutputStream java.io.FilterOutputStream.out accessible"

### Tools. Scripts

- [`KT-58366`](https://youtrack.jetbrains.com/issue/KT-58366) The obsolete kotlin-script-util jar is still published and contains broken JSR-223 implementation

### Tools. Wasm

- [`KT-56585`](https://youtrack.jetbrains.com/issue/KT-56585) Change wasmBrowserRun Browser Executable to System Default
- [`KT-56159`](https://youtrack.jetbrains.com/issue/KT-56159) Running (karma) tests doesn't work in a project generated by wizard "Browser Application for Kotlin/Wasm"
- [`KT-57203`](https://youtrack.jetbrains.com/issue/KT-57203) Update Kotlin/Wasm to support Gradle 8


## 1.8.21

### Compiler

- [`KT-57848`](https://youtrack.jetbrains.com/issue/KT-57848) Native: compilation of dynamic/static library fails with Xcode 14.3
- [`KT-57875`](https://youtrack.jetbrains.com/issue/KT-57875) Native compilation failure: Suspend functions should be lowered out at this point, but FUN LOCAL_FUNCTION_FOR_LAMBDA
- [`KT-57946`](https://youtrack.jetbrains.com/issue/KT-57946) KAPT: "RuntimeException: No type for expression" with delegate

### JavaScript

- [`KT-57356`](https://youtrack.jetbrains.com/issue/KT-57356) KJS: StackOverflowException on @JsExport with type parameters referring to one another

### Tools. Compiler plugins. Serialization

- [`KT-58067`](https://youtrack.jetbrains.com/issue/KT-58067) Serialization: NullPointerException caused by @Contextual property with type with generic
- [`KT-57730`](https://youtrack.jetbrains.com/issue/KT-57730) Serialization: "IllegalStateException: Serializer for element of type <root>.Foo has not been found" caused by serialization of Java type

### Tools. Gradle. JS

- [`KT-57766`](https://youtrack.jetbrains.com/issue/KT-57766) KJS / Gradle "Module not found: Error: Can't resolve 'kotlin-kotlin-stdlib-js-ir'" when using "useEsModules"

### Tools. Kapt

- [`KT-58027`](https://youtrack.jetbrains.com/issue/KT-58027) Kotlin 1.8.20 kapt issue "null: KtCallExpression: build()"

### Tools. Maven

- [`KT-58048`](https://youtrack.jetbrains.com/issue/KT-58048) Maven: "Too many source module declarations found" after upgrading to 1.8.20
- [`KT-58101`](https://youtrack.jetbrains.com/issue/KT-58101) 'Unable to access class' in kotlin-maven-plugin after updating to Kotlin 1.8.20


## 1.8.20

### Analysis API

- [`KT-55510`](https://youtrack.jetbrains.com/issue/KT-55510) K2: Lost designation for local classes
- [`KT-55191`](https://youtrack.jetbrains.com/issue/KT-55191) AA: add an API to compare symbol pointers
- [`KT-55487`](https://youtrack.jetbrains.com/issue/KT-55487) K2: symbol pointer restoring doesn't work for static members
- [`KT-55336`](https://youtrack.jetbrains.com/issue/KT-55336) K2 IDE: "java.lang.IllegalStateException: Required value was null." exception while importing a compiled JPS project
- [`KT-55098`](https://youtrack.jetbrains.com/issue/KT-55098) AA: KtDeclarationRenderer should render a context receivers
- [`KT-51181`](https://youtrack.jetbrains.com/issue/KT-51181) LL API: errors for SAM with suspend function from another module
- [`KT-50250`](https://youtrack.jetbrains.com/issue/KT-50250) Analysis API: Implement Analysis API of KtExpression.isUsedAsExpression
- [`KT-54360`](https://youtrack.jetbrains.com/issue/KT-54360) KtPropertySymbol: support JvmField in javaSetterName and javaGetterName

### Analysis API. FE1.0

- [`KT-55825`](https://youtrack.jetbrains.com/issue/KT-55825) AA FE1.0: stackoverflow when resolution to a function with a recursive type parameter

### Analysis API. FIR

- [`KT-54311`](https://youtrack.jetbrains.com/issue/KT-54311) K2: proper implementation of KtSymbolPointer
- [`KT-50238`](https://youtrack.jetbrains.com/issue/KT-50238) Analysis API: Implement KSymbolPointer for KtSymbol

### Analysis API. FIR Low Level API

- [`KT-52160`](https://youtrack.jetbrains.com/issue/KT-52160) FIR: Substitution overrides on FirValueParameter-s are incorrectly unwrapped
- [`KT-55566`](https://youtrack.jetbrains.com/issue/KT-55566) LL FIR: Tests in `compiler/testData/diagnostics/tests/testsWithJava17` fail under LL FIR
- [`KT-55339`](https://youtrack.jetbrains.com/issue/KT-55339) LL FIR: Missing RECURSIVE_TYPEALIAS_EXPANSION error in function type alias
- [`KT-55327`](https://youtrack.jetbrains.com/issue/KT-55327) LL FIR: Diverging UNRESOLVED_REFERENCE errors in recursive local function test
- [`KT-54826`](https://youtrack.jetbrains.com/issue/KT-54826) KtSymbolPointer: migrate from IdSignature to our own solution

### Android

- [`KT-54464`](https://youtrack.jetbrains.com/issue/KT-54464) MPP, Android SSL2: Add a flag for suppressing warning in case of using Android Style folders

### Backend. Wasm

- [`KT-38924`](https://youtrack.jetbrains.com/issue/KT-38924) Wasm support in nodejs
- [`KT-56160`](https://youtrack.jetbrains.com/issue/KT-56160) Getting WebAssembly.CompileError in browsers not supported GC and other required proposals
- [`KT-46773`](https://youtrack.jetbrains.com/issue/KT-46773) Implement an experimental version of the Kotlin/Wasm compiler backend
- [`KT-56584`](https://youtrack.jetbrains.com/issue/KT-56584) K/Wasm: Can't link symbol class
- [`KT-56166`](https://youtrack.jetbrains.com/issue/KT-56166) Fix compatibility with Firefox Nightly
- [`KT-55589`](https://youtrack.jetbrains.com/issue/KT-55589) Basic support of WASI
- [`KT-53790`](https://youtrack.jetbrains.com/issue/KT-53790) Reading from "node:module" is not handled by plugins error with Kotlin/Wasm 1.7.20-Beta

### Compiler

#### New Features

- [`KT-54535`](https://youtrack.jetbrains.com/issue/KT-54535) Implement custom equals and hashCode for value classes in Kotlin/JVM
- [`KT-55949`](https://youtrack.jetbrains.com/issue/KT-55949) Release experimental `@Volatile` support in native
- [`KT-44698`](https://youtrack.jetbrains.com/issue/KT-44698) Frontend (K2): print file name/line on compiler crash/exception
- [`KT-54666`](https://youtrack.jetbrains.com/issue/KT-54666) K2: Allow to skip specifying type arguments for members from raw type scope
- [`KT-54524`](https://youtrack.jetbrains.com/issue/KT-54524) Implement Java synthetic property references in compiler
- [`KT-54024`](https://youtrack.jetbrains.com/issue/KT-54024) K2: support -Xlink-via-signatures mode

#### Performance Improvements

- [`KT-33722`](https://youtrack.jetbrains.com/issue/KT-33722) JVM: Result API causes unnecessary boxing
- [`KT-53330`](https://youtrack.jetbrains.com/issue/KT-53330) Optimize for-loops and contains over open-ended ranges with until operator (`..<`) for all backends
- [`KT-54415`](https://youtrack.jetbrains.com/issue/KT-54415) JVM BE: performance loss related to multi-field inline class lowering
- [`KT-48759`](https://youtrack.jetbrains.com/issue/KT-48759) Infix compareTo boxes inline classes
- [`KT-55033`](https://youtrack.jetbrains.com/issue/KT-55033) Make org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl#runTransaction an inline function
- [`KT-54501`](https://youtrack.jetbrains.com/issue/KT-54501) Improve code generation for inline classes with custom equals

#### Fixes

- [`KT-56965`](https://youtrack.jetbrains.com/issue/KT-56965) K/N: linkDebugFrameworkIosArm64 tasks failing with UnsupportedOperationException: VAR name:disposables type:com.badoo.reaktive.disposable.CompositeDisposable [val]
- [`KT-56611`](https://youtrack.jetbrains.com/issue/KT-56611) Native: new native caches are broken when KONAN_DATA_DIR is defined to a directory inside ~/.gradle
- [`KT-55251`](https://youtrack.jetbrains.com/issue/KT-55251) Enum.entries compilation error should be more specific
- [`KT-56527`](https://youtrack.jetbrains.com/issue/KT-56527) K2: "AssertionError: Assertion failed" during compilation in SequentialFilePositionFinder
- [`KT-56526`](https://youtrack.jetbrains.com/issue/KT-56526) InvalidProtocolBufferException on reading module metadata compiled by K2 in 1.8.20
- [`KT-57388`](https://youtrack.jetbrains.com/issue/KT-57388) Kapt+JVM_IR: "RuntimeException: No type for expression" for delegated property
- [`KT-53153`](https://youtrack.jetbrains.com/issue/KT-53153) Synthetic Enum.entries can be shadowed by user-defined declarations
- [`KT-51290`](https://youtrack.jetbrains.com/issue/KT-51290) "AssertionError: Parameter indices mismatch at context" with context receivers
- [`KT-57242`](https://youtrack.jetbrains.com/issue/KT-57242) Equals behaviour for value classes implementing interfaces is different between 1.8.10 and 1.8.20-RC
- [`KT-57261`](https://youtrack.jetbrains.com/issue/KT-57261) "IllegalArgumentException was thrown at: MemoizedInlineClassReplacements.getSpecializedEqualsMethod" when comparing non-inline class instance with an inline class instance
- [`KT-57107`](https://youtrack.jetbrains.com/issue/KT-57107) Handling of Windows line endings CRLF broken in latest snapshot with K2
- [`KT-57117`](https://youtrack.jetbrains.com/issue/KT-57117) K2: Compiler reports invalid columns in diagnostics in case of crlf line endings
- [`KT-56500`](https://youtrack.jetbrains.com/issue/KT-56500) The type parameter TYPE_PARAMETER name:E index:0 variance: superTypes:[kotlin.Any?] reified:false is not defined in the referenced function FUN LOCAL_FUNCTION_FOR_LAMBDA
- [`KT-56258`](https://youtrack.jetbrains.com/issue/KT-56258) VerifyError: Bad local variable type when using -Xdebug
- [`KT-54455`](https://youtrack.jetbrains.com/issue/KT-54455) Unexpected result of equality comparison of inline class objects
- [`KT-56251`](https://youtrack.jetbrains.com/issue/KT-56251) Generic Java synthetic property references don't work in K2
- [`KT-55886`](https://youtrack.jetbrains.com/issue/KT-55886) K2: Wrong code location mapping with Windows line endings
- [`KT-43296`](https://youtrack.jetbrains.com/issue/KT-43296) FIR: Complicated interaction between smart cast and inference leads to false-positive diagnostic
- [`KT-57053`](https://youtrack.jetbrains.com/issue/KT-57053) Problem around anonymous objects in inline functions
- [`KT-54950`](https://youtrack.jetbrains.com/issue/KT-54950) NoSuchMethodError on calling 'addAll' on inline class implementing mutable list
- [`KT-56815`](https://youtrack.jetbrains.com/issue/KT-56815) compileKotlin task is stuck with while(true) and suspend function
- [`KT-56847`](https://youtrack.jetbrains.com/issue/KT-56847) Unresolved reference to Java annotation in Kotlin class with the same name packages
- [`KT-52459`](https://youtrack.jetbrains.com/issue/KT-52459) Context receivers: AbstractMethodError caused by Interface method with both an extension and a context receiver is overriden incorrectly in subclasses
- [`KT-56215`](https://youtrack.jetbrains.com/issue/KT-56215) JVM: Object extension function nullable receiver﻿ null check false negative when object is null
- [`KT-56188`](https://youtrack.jetbrains.com/issue/KT-56188) K/N: AssertionError when casting SAM wrapper with generic type parameter
- [`KT-56033`](https://youtrack.jetbrains.com/issue/KT-56033) Restore 'isMostPreciseContravariantArgument' function signature for compatibility
- [`KT-56407`](https://youtrack.jetbrains.com/issue/KT-56407) Backend Internal error: Exception during IR lowering during `:daemon-common-new:compileKotlin`
- [`KT-55887`](https://youtrack.jetbrains.com/issue/KT-55887) K2. "IllegalStateException: org.jetbrains.kotlin.ir.expressions.impl.IrErrorCallExpressionImpl is not expected" on adding kotlin.plugin.jpa
- [`KT-56701`](https://youtrack.jetbrains.com/issue/KT-56701) K2 (with LightTree) reports syntax errors without additional information
- [`KT-56649`](https://youtrack.jetbrains.com/issue/KT-56649) K2 uses 0-index for line numbers rather than 1-index
- [`KT-54807`](https://youtrack.jetbrains.com/issue/KT-54807) K2. Support `@OnlyInputTypes` diagnostic checks (`contains` like calls)
- [`KT-51247`](https://youtrack.jetbrains.com/issue/KT-51247) "AssertionError: org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl" caused by context receiver functional types
- [`KT-55436`](https://youtrack.jetbrains.com/issue/KT-55436) K1: implement warning about shadowing of the derived property by the base class field
- [`KT-56521`](https://youtrack.jetbrains.com/issue/KT-56521) Static scope initializers sometimes not called when first accessed from interop
- [`KT-49182`](https://youtrack.jetbrains.com/issue/KT-49182) Strange cast from Unit to String
- [`KT-55288`](https://youtrack.jetbrains.com/issue/KT-55288) False negative WRONG_ANNOTATION_TARGET on type under a nullability qualifier
- [`KT-33132`](https://youtrack.jetbrains.com/issue/KT-33132) Cannot override the equals operator twice (in a class and its subclass) unless omitting the operator keyword in the subclass
- [`KT-56061`](https://youtrack.jetbrains.com/issue/KT-56061) K1 does not report error on inconsistent synthetic property assignment
- [`KT-55483`](https://youtrack.jetbrains.com/issue/KT-55483) K2: Fir is not initialized for FirRegularClassSymbol java/lang/invoke/LambdaMetafactory
- [`KT-55125`](https://youtrack.jetbrains.com/issue/KT-55125) Difference in generated bytecode for open suspend functions of generic classes
- [`KT-54140`](https://youtrack.jetbrains.com/issue/KT-54140) SOE at `IrBasedDescriptorsKt.makeKotlinType` with mixing recursive definitely not nullable type with nullability
- [`KT-56224`](https://youtrack.jetbrains.com/issue/KT-56224) Clarify message "Secondary constructors with bodies are reserved for for future releases" for secondary constructors in value classes with bodies
- [`KT-54662`](https://youtrack.jetbrains.com/issue/KT-54662) K2: Assign operator ambiguity on synthetic property from java
- [`KT-54507`](https://youtrack.jetbrains.com/issue/KT-54507) K2: Wrong `implicitModality` for interface in `FirHelpers`
- [`KT-55912`](https://youtrack.jetbrains.com/issue/KT-55912) "UnsupportedOperationException: Unsupported const element type kotlin.Any" caused by `kotlin` fqn in annotation
- [`KT-56018`](https://youtrack.jetbrains.com/issue/KT-56018) [K2/N] Fir2Ir does not take value parameters annotations from FIR to IR
- [`KT-56091`](https://youtrack.jetbrains.com/issue/KT-56091) [K2/N] Fix various property annotations
- [`KT-54209`](https://youtrack.jetbrains.com/issue/KT-54209) K2: false positive deprecation on a class literal with deprecated companion
- [`KT-55977`](https://youtrack.jetbrains.com/issue/KT-55977) [K2/N] Suspend function reference type is wrongly serialized to klib
- [`KT-55493`](https://youtrack.jetbrains.com/issue/KT-55493) K2: False-negative VAL_REASSIGNMENT
- [`KT-55372`](https://youtrack.jetbrains.com/issue/KT-55372) K2: false-negative INVISIBLE_MEMBER for call of static method of package-private Java grandparent class
- [`KT-55371`](https://youtrack.jetbrains.com/issue/KT-55371) K2: compiled code fails trying to call static method of package-private Java grandparent class
- [`KT-55408`](https://youtrack.jetbrains.com/issue/KT-55408) K2: can't access indirectly inherited from a package-private class Java members through a type alias
- [`KT-55116`](https://youtrack.jetbrains.com/issue/KT-55116) K2: store static qualifiers in dispatch receiver field
- [`KT-55996`](https://youtrack.jetbrains.com/issue/KT-55996) K2: cannot switch the light tree mode off with -Xuse-fir-lt=false
- [`KT-55368`](https://youtrack.jetbrains.com/issue/KT-55368) K2/MPP: Metadata compiler
- [`KT-54305`](https://youtrack.jetbrains.com/issue/KT-54305) K1: implement warning "synthetic setter projected out"
- [`KT-52027`](https://youtrack.jetbrains.com/issue/KT-52027) "NullPointerException" when using context receivers with inline fun
- [`KT-55984`](https://youtrack.jetbrains.com/issue/KT-55984) Stack allocated array is not cleaned between loop iterations
- [`KT-52593`](https://youtrack.jetbrains.com/issue/KT-52593) Provide Alpha support for JS in the K2 platform
- [`KT-54656`](https://youtrack.jetbrains.com/issue/KT-54656) NoSuchMethodError on invoking Java constructor which takes an inline value class as a parameter
- [`KT-56015`](https://youtrack.jetbrains.com/issue/KT-56015) Remove unnecessary stack traces for special checks for ObjC interop
- [`KT-55606`](https://youtrack.jetbrains.com/issue/KT-55606) K2. Infix operator "in" works on ConcurrentHashMap when it's declared through another class
- [`KT-53884`](https://youtrack.jetbrains.com/issue/KT-53884) K2: "IllegalStateException: Fir is not initialized for FirRegularClassSymbol com/appodeal/consent/Consent.a" when importing this class
- [`KT-54502`](https://youtrack.jetbrains.com/issue/KT-54502) Synthetic extensions on raw types work differently from regular getter calls
- [`KT-49351`](https://youtrack.jetbrains.com/issue/KT-49351) FIR: Raw type scopes are unsupported
- [`KT-49345`](https://youtrack.jetbrains.com/issue/KT-49345) FIR: Properly support raw types in type parameter upper bounds
- [`KT-55733`](https://youtrack.jetbrains.com/issue/KT-55733) K2. Reference resolve works incorrectly for classes declared through typealias
- [`KT-46369`](https://youtrack.jetbrains.com/issue/KT-46369) FIR: Investigate raw types for arrays
- [`KT-41794`](https://youtrack.jetbrains.com/issue/KT-41794) [FIR] Implement raw type based scope
- [`KT-55181`](https://youtrack.jetbrains.com/issue/KT-55181) K2. No compilation error on throwing not throwable
- [`KT-55398`](https://youtrack.jetbrains.com/issue/KT-55398) Kotlin inline nested inline lambda's inline variable will inline not correctly
- [`KT-55359`](https://youtrack.jetbrains.com/issue/KT-55359) K2. No error when secondary constructor does not delegate to primary one
- [`KT-55759`](https://youtrack.jetbrains.com/issue/KT-55759) K2: Unresolved reference of `serializer` if library linking is used (with kotlinx.serialization plugin)
- [`KT-54705`](https://youtrack.jetbrains.com/issue/KT-54705) Kotlin scripting doesn't support files with UTF-8 BOM
- [`KT-51753`](https://youtrack.jetbrains.com/issue/KT-51753) FIR: various errors due to expect/actual mapping absence in translator
- [`KT-44515`](https://youtrack.jetbrains.com/issue/KT-44515) FIR DFA: extract non-null info from anonymous object's initialization
- [`KT-55018`](https://youtrack.jetbrains.com/issue/KT-55018) K2 / serialization: FIR2IR fails on local companion
- [`KT-55284`](https://youtrack.jetbrains.com/issue/KT-55284) Refactor org.jetbrains.kotlin.diagnostics.KtDiagnosticReportContextHelpersKt#reportOn(...)
- [`KT-55693`](https://youtrack.jetbrains.com/issue/KT-55693) K2. Type inference changed in k2
- [`KT-54742`](https://youtrack.jetbrains.com/issue/KT-54742) K2: lambda with conditional bare `return` inferred to return Any, not Unit
- [`KT-54332`](https://youtrack.jetbrains.com/issue/KT-54332) Add deprecation warning for false-negative TYPE_MISMATCH for KT-49404
- [`KT-55509`](https://youtrack.jetbrains.com/issue/KT-55509) Invisible fake overrides are listed among lazy IR class members
- [`KT-55597`](https://youtrack.jetbrains.com/issue/KT-55597) K2. `This type has a constructor, and thus must be initialized here` error is missed for anonymous object inherits class with no-arg constructor
- [`KT-54357`](https://youtrack.jetbrains.com/issue/KT-54357) "ClassCastException: class org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver cannot be cast to class org.jetbrains.kotlin.resolve.scopes.receivers.ContextClassReceiver" with anonymous object extending a class with a context receiver
- [`KT-51397`](https://youtrack.jetbrains.com/issue/KT-51397) "VerifyError: Bad type on operand stack" with context receivers
- [`KT-54905`](https://youtrack.jetbrains.com/issue/KT-54905) KLIB check on compiled with pre-release version
- [`KT-55615`](https://youtrack.jetbrains.com/issue/KT-55615) K2 often does not expand type aliases in annotation position
- [`KT-54522`](https://youtrack.jetbrains.com/issue/KT-54522) K2: ambiguity between operator candidates on += (plusAssign) to reassigned var of MutableList type
- [`KT-54300`](https://youtrack.jetbrains.com/issue/KT-54300) K2: No "Projections are not allowed for immediate arguments of a supertype" for projection in supertypes of an anonymous object
- [`KT-55495`](https://youtrack.jetbrains.com/issue/KT-55495) K2: support lateinit intrinsic applicability checker
- [`KT-55494`](https://youtrack.jetbrains.com/issue/KT-55494) MPP. Error when building for native: Compilation failed: Global 'kclass:io.ktor.serialization.$deserializeCOROUTINE$0' already exists
- [`KT-54980`](https://youtrack.jetbrains.com/issue/KT-54980) K2: Explicit type arguments in calls with the wrong number of type arguments are not resolved
- [`KT-54730`](https://youtrack.jetbrains.com/issue/KT-54730) K2: type aliases to generic functional interfaces attempt to re-infer explicitly specified type parameters
- [`KT-55611`](https://youtrack.jetbrains.com/issue/KT-55611) IC / MPP: Optional internal annotations are not visible on incremental builds
- [`KT-55324`](https://youtrack.jetbrains.com/issue/KT-55324) K2: ControlFlowGraphBuilder fails with index out of bounds exception
- [`KT-55656`](https://youtrack.jetbrains.com/issue/KT-55656) K2: PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED being a warning causes a NPE in runtime
- [`KT-51277`](https://youtrack.jetbrains.com/issue/KT-51277) "NoSuchElementException: Collection contains no element matching the predicate" with context receivers and star projection
- [`KT-52791`](https://youtrack.jetbrains.com/issue/KT-52791) Class with multiple context receivers fails -Xvalidate-ir with "Validation failed in file"
- [`KT-55071`](https://youtrack.jetbrains.com/issue/KT-55071) Shared Native Compilation: Calls from intermediate common source set cannot use default parameters declared in expect common functions
- [`KT-52193`](https://youtrack.jetbrains.com/issue/KT-52193) Native: Unable to call primary constructor with default values in an actual class without passing the values, in nativeMain source set
- [`KT-54573`](https://youtrack.jetbrains.com/issue/KT-54573) K2: untouched implicit types in delegated constructor call of data class with `@JvmRecord`
- [`KT-55037`](https://youtrack.jetbrains.com/issue/KT-55037) Support jspecify annotations moved to the new package org.jspecify.annotations in jspecify 0.3
- [`KT-48989`](https://youtrack.jetbrains.com/issue/KT-48989) JVM / IR: "IllegalStateException: Bad exception handler end" when first parameter of inline function is nullable with "try/catch/finally" default value and second parameter tries to call toString() on the first
- [`KT-55231`](https://youtrack.jetbrains.com/issue/KT-55231) K2: Contract declarations are not passed to checkers
- [`KT-54411`](https://youtrack.jetbrains.com/issue/KT-54411) False positive: INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION on kotlinx.coroutines code
- [`KT-55005`](https://youtrack.jetbrains.com/issue/KT-55005) Inconsistent behavior of array set operation in Kotlin 1.7.21
- [`KT-44625`](https://youtrack.jetbrains.com/issue/KT-44625) Property backing/delegate field annotations are not serialized/deserialized for non-JVM targets
- [`KT-42490`](https://youtrack.jetbrains.com/issue/KT-42490) Receiver annotations are not serialized/deserialized for non-JVM targets
- [`KT-53441`](https://youtrack.jetbrains.com/issue/KT-53441) K2: cannot access static method of package-private Java grandparent class
- [`KT-54197`](https://youtrack.jetbrains.com/issue/KT-54197) [K2] Exception from inliner for inline function with context receiver
- [`KT-55246`](https://youtrack.jetbrains.com/issue/KT-55246) Disable 'CustomEqualsInValueClasses' feature in 1.9 language version
- [`KT-55247`](https://youtrack.jetbrains.com/issue/KT-55247) Disable 'InlineLateinit' feature in 1.9 language version
- [`KT-53957`](https://youtrack.jetbrains.com/issue/KT-53957) K2 and -Xlambdas=indy: LambdaConversionException on reference to method with both context and extension receivers
- [`KT-55421`](https://youtrack.jetbrains.com/issue/KT-55421) K2: get rid of potentially redundant call of preCacheBuiltinClassMembers from getIrClassSymbol
- [`KT-52815`](https://youtrack.jetbrains.com/issue/KT-52815) Compiler option -Xjdk-release fails to compile mixed projects
- [`KT-52236`](https://youtrack.jetbrains.com/issue/KT-52236) Different modality in psi and fir
- [`KT-54921`](https://youtrack.jetbrains.com/issue/KT-54921) K2: cannot access static field of package-private Java parent class
- [`KT-53698`](https://youtrack.jetbrains.com/issue/KT-53698) K2: FIR2IR fails on call of inivisble extension function with Suppress
- [`KT-53920`](https://youtrack.jetbrains.com/issue/KT-53920) K2: "NoSuchElementException: Key `org.jetbrains.kotlin.fir.resolve.dfa.cfg.ClassExitNode@ef115ab` is missing in the map" with unreachable code and anonymous object
- [`KT-55358`](https://youtrack.jetbrains.com/issue/KT-55358) INTEGER_OPERATOR_RESOLVE_WILL_CHANGE is not reported in return positions of functions
- [`KT-51475`](https://youtrack.jetbrains.com/issue/KT-51475) "ArrayIndexOutOfBoundsException: Index 4 out of bounds for length 4" with context(Any) on inline function with contract
- [`KT-51951`](https://youtrack.jetbrains.com/issue/KT-51951) "IllegalStateException: No receiver" caused by implicit invoke on typealias context receiver
- [`KT-52373`](https://youtrack.jetbrains.com/issue/KT-52373) Context receivers: ClassCastException: function with dispatch, context, and extension receivers produces this when a parameter's default is included
- [`KT-54220`](https://youtrack.jetbrains.com/issue/KT-54220) K2: compiler fails on compiling plus expression on unsigned int
- [`KT-54692`](https://youtrack.jetbrains.com/issue/KT-54692) K2: compiler fails on compiling unsigned shifts
- [`KT-54824`](https://youtrack.jetbrains.com/issue/KT-54824) K2: missing smartcast after two levels of aliasing and a reassignment
- [`KT-53368`](https://youtrack.jetbrains.com/issue/KT-53368) Out of bounds read in sse version of String::hashCode
- [`KT-54978`](https://youtrack.jetbrains.com/issue/KT-54978) K2: Property accesses with explicit type arguments pass frontend checkers
- [`KT-51863`](https://youtrack.jetbrains.com/issue/KT-51863) ClassCastException when using context receivers with named argument.
- [`KT-55123`](https://youtrack.jetbrains.com/issue/KT-55123) JvmSerializableLambda is not applicable in common code in multiplatform projects
- [`KT-45970`](https://youtrack.jetbrains.com/issue/KT-45970) Missing deprecation warnings for constant operators calls in property initializers
- [`KT-54851`](https://youtrack.jetbrains.com/issue/KT-54851) K2: analysis of as/is contains multiple errors that result in missing diagnostics
- [`KT-54668`](https://youtrack.jetbrains.com/issue/KT-54668) K2: Inference error in body of lazy property with elvis with Nothing in RHS
- [`KT-55269`](https://youtrack.jetbrains.com/issue/KT-55269) FIR2IR: Static functions and nested classes are missing from Fir2IrLazyClass
- [`KT-55026`](https://youtrack.jetbrains.com/issue/KT-55026) K2: Function hides internal constructor from another module
- [`KT-53070`](https://youtrack.jetbrains.com/issue/KT-53070) Update intellij testdata fixes for FIR and merge it to master
- [`KT-53492`](https://youtrack.jetbrains.com/issue/KT-53492) No parameter null check generated for constructor taking an inline class type
- [`KT-50489`](https://youtrack.jetbrains.com/issue/KT-50489) Smart cast may lead to failing inference
- [`KT-55160`](https://youtrack.jetbrains.com/issue/KT-55160) Kotlin's fragment element types must not extend `IStubFileElementType`
- [`KT-55143`](https://youtrack.jetbrains.com/issue/KT-55143) K2: INAPPLICABLE_JVM_NAME in JVM does not work for inline classes
- [`KT-47933`](https://youtrack.jetbrains.com/issue/KT-47933) Report warning if kotlin.annotation.Repeatable is used together with java.lang.annotation.Repeatable
- [`KT-55035`](https://youtrack.jetbrains.com/issue/KT-55035) FIR: do not use FirValueParameter for FirFunctionalTypeRef
- [`KT-55095`](https://youtrack.jetbrains.com/issue/KT-55095) Wrong containingDeclarationSymbol in type parameter from Enhancement
- [`KT-53946`](https://youtrack.jetbrains.com/issue/KT-53946) K2: don't resolve Enum.declaringClass and Enum.getDeclaringClass
- [`KT-54673`](https://youtrack.jetbrains.com/issue/KT-54673) K2. "Superclass is not accessible" from interface error for sealed interfaces
- [`KT-55074`](https://youtrack.jetbrains.com/issue/KT-55074) OptIn false negative: constructor call with default argument value
- [`KT-54260`](https://youtrack.jetbrains.com/issue/KT-54260) K2: "AssertionError: No modifier list, but modifier has been found by the analyzer" when annotated annotation and AllOpen plugin
- [`KT-55034`](https://youtrack.jetbrains.com/issue/KT-55034) FIR: provide information about containing function/constructor to FirValueParameter
- [`KT-54744`](https://youtrack.jetbrains.com/issue/KT-54744) K2: reassigning a var erases smartcast info of a variable derived from the old value
- [`KT-53988`](https://youtrack.jetbrains.com/issue/KT-53988) K2: False negative "The expression cannot be a selector (occur after a dot)"
- [`KT-53983`](https://youtrack.jetbrains.com/issue/KT-53983) K2 crashes with NPE when 'this' is used inside enum class constructor
- [`KT-54910`](https://youtrack.jetbrains.com/issue/KT-54910) Can not declare typed equals operator in inline class with "Nothing" return type
- [`KT-54909`](https://youtrack.jetbrains.com/issue/KT-54909) Usage of custom typed equals operator in generic inline class is type-unsafe
- [`KT-53371`](https://youtrack.jetbrains.com/issue/KT-53371) Properly resolve FIR to get fully resolved annotations
- [`KT-53519`](https://youtrack.jetbrains.com/issue/KT-53519) FIR: argument mapping for annotations on value parameter is not properly built
- [`KT-54827`](https://youtrack.jetbrains.com/issue/KT-54827) MPP: "java.lang.IndexOutOfBoundsException: Index: 0" during compilation of `androidMain` target
- [`KT-54417`](https://youtrack.jetbrains.com/issue/KT-54417) K2: move receiver-targeted annotations to KtReceiverParameterSymbol and remove it from FirProperty receiver type
- [`KT-54972`](https://youtrack.jetbrains.com/issue/KT-54972) K2: Local functions with multiple type arguments are broken
- [`KT-54762`](https://youtrack.jetbrains.com/issue/KT-54762) Private constructor is accessible from a public inline function via `@PublishedAPI` annotation
- [`KT-54832`](https://youtrack.jetbrains.com/issue/KT-54832) Deprecate incorrect callable references resolution behavior for KT-54316
- [`KT-54732`](https://youtrack.jetbrains.com/issue/KT-54732) DirectedGraphCondensationBuilder.paint fails with StackOverflowError during linkReleaseFrameworkIos64
- [`KT-54897`](https://youtrack.jetbrains.com/issue/KT-54897) K2: value class with private constructor stripped by jvm-abi-gen cannot be used in another module
- [`KT-54784`](https://youtrack.jetbrains.com/issue/KT-54784) NPE from IrSourceCompilerForInlineKt.nonLocalReturnLabel on non-local break and continue in anonymous initializers and in scripts
- [`KT-54840`](https://youtrack.jetbrains.com/issue/KT-54840) Field for const property on interface companion object loses deprecated status when copied to interface
- [`KT-53825`](https://youtrack.jetbrains.com/issue/KT-53825) class files are generated when compilation fails with platform declaration clash
- [`KT-54526`](https://youtrack.jetbrains.com/issue/KT-54526) K2: Raw type scope is lost after exiting from elvis
- [`KT-54570`](https://youtrack.jetbrains.com/issue/KT-54570) K2: False-positive OVERLOAD_RESOLUTION_AMBIGUITY in case of combination of raw types
- [`KT-52157`](https://youtrack.jetbrains.com/issue/KT-52157) Annotation on type parameter isn't present in the symbol loaded from the library
- [`KT-54318`](https://youtrack.jetbrains.com/issue/KT-54318) VerifyError on `{ null }` in catch block
- [`KT-54654`](https://youtrack.jetbrains.com/issue/KT-54654) K2: Implicit types leaks into delegated member
- [`KT-54645`](https://youtrack.jetbrains.com/issue/KT-54645) K2: Clash of two inherited classes with the same name
- [`KT-53255`](https://youtrack.jetbrains.com/issue/KT-53255) [FIR2IR] StackOverflowError with long when-expression conditions
- [`KT-48861`](https://youtrack.jetbrains.com/issue/KT-48861) No warning on incorrect usage of array type annotated as Nullable in Java
- [`KT-54539`](https://youtrack.jetbrains.com/issue/KT-54539) `@Deprecated` on members of private companion object is no longer needed
- [`KT-54403`](https://youtrack.jetbrains.com/issue/KT-54403) Unexpected behaviour on overridden typed equals in inline class
- [`KT-54536`](https://youtrack.jetbrains.com/issue/KT-54536) Unexpected result of comparison of inline class instances
- [`KT-54603`](https://youtrack.jetbrains.com/issue/KT-54603) ClassCastException on comparison of inline classes with custom equals
- [`KT-54401`](https://youtrack.jetbrains.com/issue/KT-54401) Unhandled exception on compilation inline class with 'equals' from 'Any' returning 'Nothing'
- [`KT-54378`](https://youtrack.jetbrains.com/issue/KT-54378) K2: smart cast breaks subtyping in case with complex projections
- [`KT-53761`](https://youtrack.jetbrains.com/issue/KT-53761) Reified type not propagated to supertype token through two inline functions
- [`KT-53876`](https://youtrack.jetbrains.com/issue/KT-53876) Manually instantiated annotations with unsigned arrays are not equal
- [`KT-51740`](https://youtrack.jetbrains.com/issue/KT-51740) NO_VALUE_FOR_PARAMETER: Consider increasing error highlighting range
- [`KT-54084`](https://youtrack.jetbrains.com/issue/KT-54084) ClassCastException when trying to call a context receiver's method
- [`KT-51282`](https://youtrack.jetbrains.com/issue/KT-51282) IllegalAccessError: Compiler for JVM 1.8+ makes lambdas access unaccessible classes when using `@JvmMultifileClasses`
- [`KT-53479`](https://youtrack.jetbrains.com/issue/KT-53479) False positive "Cannot access 'runCatching' before superclass constructor has been called"
- [`KT-50950`](https://youtrack.jetbrains.com/issue/KT-50950) JVM IR: "AssertionError: FUN SYNTHETIC_GENERATED_SAM_IMPLEMENTATION" when using bound reference to suspend SAM function
- [`KT-49364`](https://youtrack.jetbrains.com/issue/KT-49364) "VerifyError: Bad type on operand stack" on cast which "can never succeed" from ULong to Int
- [`KT-51478`](https://youtrack.jetbrains.com/issue/KT-51478) Inapplicable receiver diagnostic expected when there are two context receiver candidates

### Docs & Examples

- [`KT-32469`](https://youtrack.jetbrains.com/issue/KT-32469) `@Synchronized` on extension method doesn't generate instance lock

### IDE

#### New Features

- [`KTIJ-24378`](https://youtrack.jetbrains.com/issue/KTIJ-24378) Update Kotlin plugin to 1.8.0 in IDEA 223.2

#### Performance Improvements

- [`KT-55445`](https://youtrack.jetbrains.com/issue/KT-55445) KtUltraLightClassModifierList.hasModifierProperty requires resolve for PsiModifier.PRIVATE

#### Fixes

- [`KTIJ-24657`](https://youtrack.jetbrains.com/issue/KTIJ-24657) Disable pre-release and other metadata checks in IDE
- [`KT-55929`](https://youtrack.jetbrains.com/issue/KT-55929) Unresolved dependencies for intermediate multiplatform SourceSets
- [`KTIJ-24179`](https://youtrack.jetbrains.com/issue/KTIJ-24179) Bundle Kotlin 1.8.0 with Intellij IDEA 2022.3.2
- [`KTIJ-23547`](https://youtrack.jetbrains.com/issue/KTIJ-23547) K2 IDE: Functional type: explicit parameter name VS ParameterName annotation
- [`KTIJ-23347`](https://youtrack.jetbrains.com/issue/KTIJ-23347) K2 IDE. False positive "Symbol fun intFun(): Unit is invisible" in tests
- [`KT-55862`](https://youtrack.jetbrains.com/issue/KT-55862) Can't resolve kotlin-stdlib-js sources in IDE
- [`KTIJ-23587`](https://youtrack.jetbrains.com/issue/KTIJ-23587) K2: SOE in delegate field resolution
- [`KT-55782`](https://youtrack.jetbrains.com/issue/KT-55782) [SLC] Typealiases are not exapnded in arguments of annotations
- [`KT-55778`](https://youtrack.jetbrains.com/issue/KT-55778) [SLC] Incorrect determination of useSitePostion for types of local declarations
- [`KT-55780`](https://youtrack.jetbrains.com/issue/KT-55780) [SLC] No approximation of anonymous and local types in members
- [`KT-55743`](https://youtrack.jetbrains.com/issue/KT-55743) K2 SLC: SymbolLightClassForClassOrObject must have a name
- [`KT-55604`](https://youtrack.jetbrains.com/issue/KT-55604) Descriptor leak
- [`KT-55502`](https://youtrack.jetbrains.com/issue/KT-55502) SLC: drop redundant 'final' modifier from synthetic static enum members
- [`KT-55497`](https://youtrack.jetbrains.com/issue/KT-55497) LC: drop `@NotNull` annotation from parameter from synthetic Enum.valueOf
- [`KT-55496`](https://youtrack.jetbrains.com/issue/KT-55496) SLC: generated synthetic enum methods by symbols instead of manual creation
- [`KT-55481`](https://youtrack.jetbrains.com/issue/KT-55481) SLC: implement correct java annotations for annotation classes (Retention, Target, etc.)
- [`KT-55470`](https://youtrack.jetbrains.com/issue/KT-55470) SLC: implement light class for RepeatableContainer
- [`KT-55442`](https://youtrack.jetbrains.com/issue/KT-55442) SLC: 'isInheritor' for 'DefaultImpls' should work correctly
- [`KTIJ-23449`](https://youtrack.jetbrains.com/issue/KTIJ-23449) K2: "parent must not be null" from SymbolLightClassBase.getContext()
- [`KT-40609`](https://youtrack.jetbrains.com/issue/KT-40609) IDE: False positive "Exception is never thrown..." in Java when Kotlin getter is annotated with Throws
- [`KT-54051`](https://youtrack.jetbrains.com/issue/KT-54051) Migrate symbol light classes from KtSymbol to KtElement

### IDE. Completion

- [`KTIJ-22503`](https://youtrack.jetbrains.com/issue/KTIJ-22503) Support code completion for data objects
- [`KTIJ-22361`](https://youtrack.jetbrains.com/issue/KTIJ-22361) ISE “java.lang.IllegalStateException: Expected FirResolvedTypeRef with ConeKotlinType but was FirImplicitTypeRefImpl <implicit>” on K2

### IDE. Debugger

- [`KTIJ-24259`](https://youtrack.jetbrains.com/issue/KTIJ-24259) Debugger is stuck in an infinite loop in an Android project
- [`KTIJ-24003`](https://youtrack.jetbrains.com/issue/KTIJ-24003) Smart step into doesn't work for Java synthetic properties references
- [`KTIJ-24039`](https://youtrack.jetbrains.com/issue/KTIJ-24039) Support smart step into for property setters

### IDE. Decompiler, Indexing, Stubs

- [`KTIJ-24351`](https://youtrack.jetbrains.com/issue/KTIJ-24351) Kotlin Bytecode tool window: NullPointerException during inlining of inline function with object literal

### IDE. Gradle Integration

- [`KTIJ-24616`](https://youtrack.jetbrains.com/issue/KTIJ-24616) Gradle Integration: "NoSuchMethodError: 'java.util.Collection org.jetbrains.kotlin.idea.projectModel.KotlinCompilation.getDeclaredSourceSets" during sync fail after updating Kotlin IJ Plugin to 1.8.20-Beta
- [`KT-55347`](https://youtrack.jetbrains.com/issue/KT-55347) Broken IDE sync for js: java.lang.IllegalStateException: Accessing Compile Dependencies Transformations is not yet initialised
- [`KTIJ-23781`](https://youtrack.jetbrains.com/issue/KTIJ-23781) TCS: Gradle Sync: Support friend&dependsOn via IdeaKotlinSourceDependency

### IDE. Gradle. Script

- [`KT-56941`](https://youtrack.jetbrains.com/issue/KT-56941) Gradle KTS / Navigation: Go to declaration for Java types doesn't work

### IDE. Inspections and Intentions

- [`KTIJ-23404`](https://youtrack.jetbrains.com/issue/KTIJ-23404) K2 IDE. Platform type is inserted as type parameter for "Change return type" intention
- [`KTIJ-24319`](https://youtrack.jetbrains.com/issue/KTIJ-24319) "Set module version to *" quickfix isn't working
- [`KTIJ-23225`](https://youtrack.jetbrains.com/issue/KTIJ-23225) "Change package" intention unintentionally and intractably replaces text inside of critical strings and comments
- [`KTIJ-23892`](https://youtrack.jetbrains.com/issue/KTIJ-23892) UsePropertyAccessSyntaxInspection should also suggest replacing getter method references with method synthetic properties referencies after Kotlin 1.9
- [`KTIJ-22087`](https://youtrack.jetbrains.com/issue/KTIJ-22087) Support IDE inspections for upcoming data objects
- [`KTIJ-24286`](https://youtrack.jetbrains.com/issue/KTIJ-24286) Constant conditions: false positive "Cast will always fail" with cast of java.lang.String to kotlin.String
- [`KTIJ-23859`](https://youtrack.jetbrains.com/issue/KTIJ-23859) ConvertObjectToDataObjectInspection support more hashCode and toString cases
- [`KTIJ-23760`](https://youtrack.jetbrains.com/issue/KTIJ-23760) Get rid of `readResolve` logic in ConvertObjectToDataObjectInspection

### IDE. KDoc

- [`KTIJ-24342`](https://youtrack.jetbrains.com/issue/KTIJ-24342) KDoc: First line break character is swallowed when pasted

### IDE. Misc

- [`KTIJ-24370`](https://youtrack.jetbrains.com/issue/KTIJ-24370) Remove link to k2.xml from plugin.xml in kt-223 branches
- [`KTIJ-24210`](https://youtrack.jetbrains.com/issue/KTIJ-24210) Compatibility issue with the CUBA plugin

### IDE. Multiplatform

- [`KTIJ-21205`](https://youtrack.jetbrains.com/issue/KTIJ-21205) MPP: Kotlin not configured error is shown for K/N sources if Android target is presented
- [`KT-52172`](https://youtrack.jetbrains.com/issue/KT-52172) Multiplatform: Support composite builds
- [`KT-56198`](https://youtrack.jetbrains.com/issue/KT-56198) Multiplatform;Composite Builds: import fails if single jvm target multiplatform project consume included jvm build
- [`KTIJ-24147`](https://youtrack.jetbrains.com/issue/KTIJ-24147) MPP: NullPointerException: versionString must not be null

### IDE. Refactorings. Move

- [`KTIJ-24243`](https://youtrack.jetbrains.com/issue/KTIJ-24243) Move declarations: "Search in comments and strings" and "Search for text occurrences" options are always enabled when files are moved

### IDE. Script

- [`KT-56632`](https://youtrack.jetbrains.com/issue/KT-56632) Script configuration cannot be loaded for embedded code snippets

### IDE. Wizards

- [`KTIJ-24562`](https://youtrack.jetbrains.com/issue/KTIJ-24562) Android target created by wizard contains AGP higher than supported
- [`KTIJ-24402`](https://youtrack.jetbrains.com/issue/KTIJ-24402) Changes "Browser Application for Kotlin/Wasm" wizard template
- [`KTIJ-23525`](https://youtrack.jetbrains.com/issue/KTIJ-23525) Wizard: Compose multiplatform: project won't build and require higher compileSdkVersion

### JavaScript

#### New Features

- [`KT-54118`](https://youtrack.jetbrains.com/issue/KT-54118) Kotlin/JS IR: keep declarations with non-minified names
- [`KT-35655`](https://youtrack.jetbrains.com/issue/KT-35655) Investigate could we use "names" field in SourceMaps to improve debug experience

#### Fixes

- [`KT-55971`](https://youtrack.jetbrains.com/issue/KT-55971) KJS: Result of suspend function cannot be assigned to property of dynamic value
- [`KT-52374`](https://youtrack.jetbrains.com/issue/KT-52374) KJS / IR: caling suspend function as dynamic ignores the rest of the expression
- [`KT-56884`](https://youtrack.jetbrains.com/issue/KT-56884) KJS: "Top-level declarations in .d.ts files must start with either a 'declare' or 'export' modifier." caused by enum and array inside the companion object
- [`KT-51122`](https://youtrack.jetbrains.com/issue/KT-51122) Provide fully-qualified method name in Kotlin/JS source maps
- [`KT-56602`](https://youtrack.jetbrains.com/issue/KT-56602) KJS / Serialization: polymorphicDefaultDeserializer unbound on Kotlin 1.8.20-Beta
- [`KT-56580`](https://youtrack.jetbrains.com/issue/KT-56580) KJS: languageVersionSettings string is unstable
- [`KT-56581`](https://youtrack.jetbrains.com/issue/KT-56581) KJS: Lock file for incremental cache
- [`KT-56582`](https://youtrack.jetbrains.com/issue/KT-56582) KJS: Function type interface reflection crashes the compiler in incremental build
- [`KT-55720`](https://youtrack.jetbrains.com/issue/KT-55720) KJS: `ReferenceError: SuspendFunction1 is not defined` with 1.8 when importing`kotlin.coroutines.SuspendFunction1`
- [`KT-56469`](https://youtrack.jetbrains.com/issue/KT-56469) KJS: BE Incremental rebuild spoils source map comment
- [`KT-55930`](https://youtrack.jetbrains.com/issue/KT-55930) KJS: A recursive callable reference of the inline function leads broken cross module references
- [`KT-31888`](https://youtrack.jetbrains.com/issue/KT-31888) Kotlin/JS: make possible to call `main()` in main run tasks, but not in test tasks
- [`KT-51581`](https://youtrack.jetbrains.com/issue/KT-51581) FIR: support JS backend
- [`KT-55786`](https://youtrack.jetbrains.com/issue/KT-55786) KJS: Rewriting of secondary constructors if they are protected
- [`KT-52563`](https://youtrack.jetbrains.com/issue/KT-52563) KJS / IR: Invalid TypeScript generated for class extending base class with private constructor
- [`KT-55367`](https://youtrack.jetbrains.com/issue/KT-55367) KJS / IR + IC: Moving an external declaration between different JsModules() doesn't rebuild the JS code
- [`KT-55240`](https://youtrack.jetbrains.com/issue/KT-55240) KJS: "NoSuchElementException: No element of given type found" caused by `@JsExport` and `Throwable's` child class
- [`KT-54398`](https://youtrack.jetbrains.com/issue/KT-54398) KJS / IR + IC: Support *.d.ts generation
- [`KT-55144`](https://youtrack.jetbrains.com/issue/KT-55144) KJS / IR + IC: Modifying an inline function which is used as a default param in another inline function doesn't invalidate a caller
- [`KT-54134`](https://youtrack.jetbrains.com/issue/KT-54134) KJS / IR: "TypeError: Cannot read properties of undefined" in js block wrapped with suspend functions around
- [`KT-54911`](https://youtrack.jetbrains.com/issue/KT-54911) KJS / IR + IC: invalidate all klib dependencies after removing it
- [`KT-54912`](https://youtrack.jetbrains.com/issue/KT-54912) KJS / IR + IC: Commit cache header only in the end (after lowering)
- [`KT-52677`](https://youtrack.jetbrains.com/issue/KT-52677) Native: StackOverFlow during "kotlin.ir.util.RenderIrElementVisitor$renderTypeAnnotations$1.invoke"
- [`KT-54480`](https://youtrack.jetbrains.com/issue/KT-54480) KJS: "Exported declaration contains non-consumable identifier" warning when exporting modules as default
- [`KT-41294`](https://youtrack.jetbrains.com/issue/KT-41294) KJS: Weird behaviour of j2v8 in test infra
- [`KT-54173`](https://youtrack.jetbrains.com/issue/KT-54173) Kotlin/JS + IR: failed to provide `keep` setting to avoid DCE remove of default interface function from implementing object

### Language Design

- [`KT-55451`](https://youtrack.jetbrains.com/issue/KT-55451) Preview of lifting restriction on secondary constructor bodies for value classes
- [`KT-54621`](https://youtrack.jetbrains.com/issue/KT-54621) Preview of Enum.entries: modern and performant replacement for Enum.values()
- [`KT-54525`](https://youtrack.jetbrains.com/issue/KT-54525) Preview of Java synthetic property references
- [`KT-55337`](https://youtrack.jetbrains.com/issue/KT-55337) Preview of data objects
- [`KT-55344`](https://youtrack.jetbrains.com/issue/KT-55344) Deprecate `@Synchronized` in platforms except JVM

### Libraries

- [`KT-35508`](https://youtrack.jetbrains.com/issue/KT-35508) EXC_BAD_ACCESS(code=2, address=0x16d8dbff0) crashes on iOS when using a sequence (from map() etc.)
- [`KT-56794`](https://youtrack.jetbrains.com/issue/KT-56794) Libraries: "Recursively copying a directory into its subdirectory is prohibited" Path.copyToRecursively fails on copying from one ZipFileSystem to another ZipFileSystem
- [`KT-55935`](https://youtrack.jetbrains.com/issue/KT-55935) [Kotlin/JVM] Path.copyToRecursively does not work across file systems
- [`KT-55978`](https://youtrack.jetbrains.com/issue/KT-55978) Provide Common Base64 encoding in stdlib
- [`KT-46211`](https://youtrack.jetbrains.com/issue/KT-46211) [Kotlin/Native] Stack overflow crash in Regex classes with simple pattern and very large input
- [`KT-31066`](https://youtrack.jetbrains.com/issue/KT-31066) Add Closeable & use to common stdlib
- [`KT-55609`](https://youtrack.jetbrains.com/issue/KT-55609) Introduce experimental kotlin.concurrent.Volatile annotation
- [`KT-39789`](https://youtrack.jetbrains.com/issue/KT-39789) Segfault in Kotlin/Native regex interpreter
- [`KT-53310`](https://youtrack.jetbrains.com/issue/KT-53310) Native: HashMap/HashSet doesn't reclaim storage after removing elements

### Native

- [`KT-56443`](https://youtrack.jetbrains.com/issue/KT-56443) Native link task reports w: Cached libraries will not be used for optimized compilation
- [`KT-55938`](https://youtrack.jetbrains.com/issue/KT-55938) [Kotlin/Native] Inline functions accessing ObjC class companion cause compiler to crash when building static caches in 1.8.20 dev build

### Native. C and ObjC Import

- [`KT-55303`](https://youtrack.jetbrains.com/issue/KT-55303) Objective-C import: improve `-fmodules` flag discoverability.
- [`KT-39120`](https://youtrack.jetbrains.com/issue/KT-39120) Cinterop tool doesn't support the -fmodules compiler argument
- [`KT-40426`](https://youtrack.jetbrains.com/issue/KT-40426) Incorrect Objective-C extensions importing that prevents UIKit usage
- [`KT-55653`](https://youtrack.jetbrains.com/issue/KT-55653) Since Kotlin 1.8.0 NSView.resetCursorRects doesn't exist anymore and cannot override it
- [`KT-54284`](https://youtrack.jetbrains.com/issue/KT-54284) Kotlin/Native: cinterop produces non-deterministic metadata

### Native. ObjC Export

- [`KT-56350`](https://youtrack.jetbrains.com/issue/KT-56350) Kotlin/Native: restore "use Foundation" in generated Objective-C frameworks
- [`KT-55736`](https://youtrack.jetbrains.com/issue/KT-55736) Native: exporting suspend function from a cached dependency to Objective-C crashes with "Suspend functions should be lowered out at this point"
- [`KT-53638`](https://youtrack.jetbrains.com/issue/KT-53638) Native: support disabling mangling globally for Swift names in generated Objective-C header
- [`KT-53069`](https://youtrack.jetbrains.com/issue/KT-53069) SOE on K/N framework build for Arm64
- [`KT-53317`](https://youtrack.jetbrains.com/issue/KT-53317) ObjCName annotation is not applied to an extension receiver in Objective-C export

### Native. Stdlib

- [`KT-53064`](https://youtrack.jetbrains.com/issue/KT-53064) Native: provide stdlib API to obtain memory management statistics

### Reflection

- [`KT-27585`](https://youtrack.jetbrains.com/issue/KT-27585) Flaky IllegalPropertyDelegateAccessException: Cannot obtain the delegate of a non-accessible property. Use "isAccessible = true" to make the property accessible
- [`KT-55178`](https://youtrack.jetbrains.com/issue/KT-55178) Improve performance of KCallable.callBy
- [`KT-53279`](https://youtrack.jetbrains.com/issue/KT-53279) Reflection: "KotlinReflectionInternalError: Method is not supported" caused by `@Repeatable` annotation deserialization at runtime if it's repeated and contains arrays
- [`KT-44977`](https://youtrack.jetbrains.com/issue/KT-44977) Reflection: ClassCastException caused by annotations with "AnnotationTarget.TYPE" usage on array attributes access

### Tools. CLI

- [`KT-57077`](https://youtrack.jetbrains.com/issue/KT-57077) `1.8.20-RC-243` shows Java 19 warnings even if configured with Java 17 toolchain
- [`KT-56992`](https://youtrack.jetbrains.com/issue/KT-56992) Performance test regression in Gradle when switching to Kotlin 1.8.20
- [`KT-56789`](https://youtrack.jetbrains.com/issue/KT-56789) Metaspace memory leak in CoreJrtFileSystem
- [`KT-56925`](https://youtrack.jetbrains.com/issue/KT-56925) Remove warning about assignment plugin
- [`KT-54652`](https://youtrack.jetbrains.com/issue/KT-54652) Enable -Xuse-fir-lt by default when -Xuse-k2 is turned on, provide way to disable
- [`KT-55784`](https://youtrack.jetbrains.com/issue/KT-55784) Unable to format compilation errors with ansi colors in compilation server
- [`KT-54718`](https://youtrack.jetbrains.com/issue/KT-54718) K2: Compiler crashes with "IllegalArgumentException: newPosition > limit"
- [`KT-54337`](https://youtrack.jetbrains.com/issue/KT-54337) CLI: compiling module-info.java without explicitly specified JDK home leads to a weird error

### Tools. Commonizer

- [`KT-47429`](https://youtrack.jetbrains.com/issue/KT-47429) [Commonizer] OKIO support
- [`KT-51517`](https://youtrack.jetbrains.com/issue/KT-51517) C Interop Commonizer Fails On Classifier That Doesn't Exist

### Tools. Compiler Plugins

#### Fixes

- [`KT-53590`](https://youtrack.jetbrains.com/issue/KT-53590) K2 Allopen does not look for transitive meta-annotations
- [`KT-56487`](https://youtrack.jetbrains.com/issue/KT-56487) Add more methods to DescriptorSerializerPlugin
- [`KT-54020`](https://youtrack.jetbrains.com/issue/KT-54020) [K2] [NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER] error in case 'static Name' param was added to `@AllArgsConstructor` annotation and an empty list is set as a constructor param value
- [`KT-53096`](https://youtrack.jetbrains.com/issue/KT-53096) Create a pack of compiler utilities for generating declarations from plugins
- [`KT-55248`](https://youtrack.jetbrains.com/issue/KT-55248) K2/PluginAPI: getCallableNamesForClass/generateClassLikeDeclaration are not called for synthetic companions of local classes
- [`KT-54756`](https://youtrack.jetbrains.com/issue/KT-54756) Deprecate "legacy" mode of jvm-abi-gen plugin
- [`KT-55233`](https://youtrack.jetbrains.com/issue/KT-55233) jvm-abi-gen strips out InnerClass attributes
- [`KT-54994`](https://youtrack.jetbrains.com/issue/KT-54994) K2 plugin API: Compile-time constants are not evaluated before IR
- [`KT-55023`](https://youtrack.jetbrains.com/issue/KT-55023) K2 plugin API: Compilation with Kotlin daemon fails after certain number of tries
- [`KT-55286`](https://youtrack.jetbrains.com/issue/KT-55286) K2: Parcelize plugin sometimes can't find nested objects in current class
- [`KT-54500`](https://youtrack.jetbrains.com/issue/KT-54500) Private type aliases can be referenced from public declarations, but are stripped by jvm-abi-gen

### Tools. Compiler plugins. Serialization

- [`KT-56738`](https://youtrack.jetbrains.com/issue/KT-56738) Unexpected SERIALIZER_NOT_FOUND when compiling against binary with enum
- [`KT-56990`](https://youtrack.jetbrains.com/issue/KT-56990) "AssertionError: SyntheticAccessorLowering should not attempt to modify other files" in kotlinx-serialization using `@Serializer` and List argument
- [`KT-54441`](https://youtrack.jetbrains.com/issue/KT-54441) Prohibit implicit serializer customization via companion object
- [`KT-49983`](https://youtrack.jetbrains.com/issue/KT-49983) Implement prototype of kotlinx.serialization for K2 compiler
- [`KT-48733`](https://youtrack.jetbrains.com/issue/KT-48733) "AssertionError: Unexpected IR element found during code generation" caused by Serialization and annotation with default parameter
- [`KT-54297`](https://youtrack.jetbrains.com/issue/KT-54297) Regression in serializable classes with star projections

### Tools. Gradle

#### New Features

- [`KT-54691`](https://youtrack.jetbrains.com/issue/KT-54691) Kotlin Gradle Plugin libraries alignment platform
- [`KT-54492`](https://youtrack.jetbrains.com/issue/KT-54492) Send gradle build errors from idea
- [`KT-55540`](https://youtrack.jetbrains.com/issue/KT-55540) Add compilation speed metric in build reports
- [`KT-55541`](https://youtrack.jetbrains.com/issue/KT-55541) Validate FUS metrics values on Gradle side

#### Performance Improvements

- [`KT-54836`](https://youtrack.jetbrains.com/issue/KT-54836) Kotlin/JVM Gradle plugin creates task eagerly on Gradle 7.3+
- [`KT-55995`](https://youtrack.jetbrains.com/issue/KT-55995) Add ability to perform precise compilation task outputs backup
- [`KT-54579`](https://youtrack.jetbrains.com/issue/KT-54579) Kapt tasks slow down significantly on Windows when running with JDK 17 compared to JDK 11
- [`KT-54588`](https://youtrack.jetbrains.com/issue/KT-54588) KotlinCompile: Avoid calling `FileCollection.getFiles()` multiple times

#### Fixes

- [`KT-57296`](https://youtrack.jetbrains.com/issue/KT-57296) Build statistics sending errors in case of buildSrc directory usage with kotlin-dsl plugin applied
- [`KT-56645`](https://youtrack.jetbrains.com/issue/KT-56645) Gradle: KGP reports an incorrect resources processing task name for JVM projects
- [`KT-55824`](https://youtrack.jetbrains.com/issue/KT-55824) Deprecate `commonMain.dependsOn(anything)` in user scripts
- [`KT-56221`](https://youtrack.jetbrains.com/issue/KT-56221) Gradle KTS: False positive `Val cannot be reassigned` when using an extension and its property with an implicit `set` operator
- [`KT-55452`](https://youtrack.jetbrains.com/issue/KT-55452) Values of the compiler arguments set via KotlinCompile task configuration are duplicated by the KaptGenerateStubs task
- [`KT-55565`](https://youtrack.jetbrains.com/issue/KT-55565) Consider de-duping or blocking standard addition of freeCompilerArgs to KaptGenerateStubsTask
- [`KT-55632`](https://youtrack.jetbrains.com/issue/KT-55632) 'The configuration :kotlinCompilerClasspath is both consumable and declarable' messages are displayed in logs for different types of projects for KotlinCompile task with gradle 8
- [`KT-56414`](https://youtrack.jetbrains.com/issue/KT-56414) Dependency locking and failed builds with Kotlin 1.8.10
- [`KT-52625`](https://youtrack.jetbrains.com/issue/KT-52625) Compatibility with Gradle 7.4 release
- [`KT-55544`](https://youtrack.jetbrains.com/issue/KT-55544) Gradle: add more debugging information for finding usages of kotlinOptions.freeCompilerArgs
- [`KT-52149`](https://youtrack.jetbrains.com/issue/KT-52149) Gradle: declare shared build services usages with `Task#usesService`
- [`KT-55323`](https://youtrack.jetbrains.com/issue/KT-55323) Gradle: allow to opt-out of reporting compiler arguments to a http statistics service
- [`KT-53811`](https://youtrack.jetbrains.com/issue/KT-53811) Compatibility with Gradle 7.6 release
- [`KT-52998`](https://youtrack.jetbrains.com/issue/KT-52998) Compatibility with Gradle 7.5 release
- [`KT-55741`](https://youtrack.jetbrains.com/issue/KT-55741) Gradle 8: Build service '*' is being used by task '*' without the corresponding declaration via 'Task#usesService'.
- [`KT-55174`](https://youtrack.jetbrains.com/issue/KT-55174) KotlinCompile task produces deprecation "Build service 'variant_impl_factories_...' is being used by task"
- [`KT-54425`](https://youtrack.jetbrains.com/issue/KT-54425) Kotlin Gradle Plugin should not use deprecated UsageContext#getUsage()
- [`KT-54998`](https://youtrack.jetbrains.com/issue/KT-54998) "kotlin.gradle.performance" FUS collector reports data twice
- [`KT-55520`](https://youtrack.jetbrains.com/issue/KT-55520) Add required configuration for Kotlin Gradle Plugin API reference publication
- [`KT-52963`](https://youtrack.jetbrains.com/issue/KT-52963) Build report code breaks Gradle project isolation.
- [`KT-55164`](https://youtrack.jetbrains.com/issue/KT-55164) KGP: "Cannot access project ':' from project ':list'" JVM - Project Isolation with Multi Modules and Configuration Cache fails
- [`KT-52490`](https://youtrack.jetbrains.com/issue/KT-52490) Gradle: [org.jetbrains.kotlin.gradle.testing.internal] TestReport.destinationDir and TestReport.reportOn deprecation warnings
- [`KT-55000`](https://youtrack.jetbrains.com/issue/KT-55000) Include information about the new IC into "kotlin.gradle.performance" FUS collector
- [`KT-54941`](https://youtrack.jetbrains.com/issue/KT-54941) Gradle, Daemon, MacOS M1: "Native integration is not available for Mac OS X aarch64" on first build
- [`KT-45748`](https://youtrack.jetbrains.com/issue/KT-45748) Migrate all Kotlin Gradle plugin Android tests to new test setup
- [`KT-54029`](https://youtrack.jetbrains.com/issue/KT-54029) Validate Binary Compatibility for kotlin-gradle-plugin-api

### Tools. Gradle. Cocoapods

- [`KT-41830`](https://youtrack.jetbrains.com/issue/KT-41830) CocoaPods integration: Support link-only mode for pods
- [`KT-55117`](https://youtrack.jetbrains.com/issue/KT-55117) PodGenTask doesn't declare ouputs properly
- [`KT-55243`](https://youtrack.jetbrains.com/issue/KT-55243) Gradle 7.6: Cocoapods plugin generates invalid podspec when applied in root project
- [`KT-56304`](https://youtrack.jetbrains.com/issue/KT-56304) Podspec generated with new K/N artifact DSL contains wrong artifact names for static and dynamic libraries
- [`KT-56298`](https://youtrack.jetbrains.com/issue/KT-56298) Assemble tasks for native binaries fail if more than one kotlin artifact is declared in one gradle project
- [`KT-55801`](https://youtrack.jetbrains.com/issue/KT-55801) Deprecate useLibraries
- [`KT-55790`](https://youtrack.jetbrains.com/issue/KT-55790) Improper sdk selected for watchosDeviceArm64 target

### Tools. Gradle. JS

#### New Features

- [`KT-25878`](https://youtrack.jetbrains.com/issue/KT-25878) Provide Option to Define Scoped NPM Package
- [`KT-37759`](https://youtrack.jetbrains.com/issue/KT-37759) [Gradle, JS] Support arguments of command line for webpack and nodejs task
- [`KT-33518`](https://youtrack.jetbrains.com/issue/KT-33518) Allow specifying command line args for node in nodejs or mocha tests
- [`KT-46163`](https://youtrack.jetbrains.com/issue/KT-46163) KJS / Ktor: Support run on the next free port if default one is occupied

#### Performance Improvements

- [`KT-55476`](https://youtrack.jetbrains.com/issue/KT-55476) KotlinWebpack should be cacheable
- [`KT-39108`](https://youtrack.jetbrains.com/issue/KT-39108) Kotlin multiplatform plugin targeting js takes too long compared to the old kotlin-frontend plugin
- [`KT-45411`](https://youtrack.jetbrains.com/issue/KT-45411) Investigate memory consumption in npm package
- [`KT-51376`](https://youtrack.jetbrains.com/issue/KT-51376) KJS / Gradle: Dukat tasks make impact on build time even if there's no npm dependencies

#### Fixes

- [`KT-57285`](https://youtrack.jetbrains.com/issue/KT-57285) KJS / Gradle / MPP: FileNotFoundException on publishing empty KJS sourceset
- [`KT-57068`](https://youtrack.jetbrains.com/issue/KT-57068) KJS / Gradle: "Unable to find method 'kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl.testTask'" after updating to 1.8.20-Beta
- [`KT-54445`](https://youtrack.jetbrains.com/issue/KT-54445) KJS Remove dukat integration
- [`KT-56999`](https://youtrack.jetbrains.com/issue/KT-56999) K/JS: KotlinJsIrLink::rootCacheDirectory property must be public
- [`KT-56765`](https://youtrack.jetbrains.com/issue/KT-56765) K/JS: Several binaries use same cache directory
- [`KT-54529`](https://youtrack.jetbrains.com/issue/KT-54529) KJS / IR: generate typescript definitions only on explicit Gradle action
- [`KT-56488`](https://youtrack.jetbrains.com/issue/KT-56488) Debugger won't stop on breakpoints of JS browser test
- [`KT-56719`](https://youtrack.jetbrains.com/issue/KT-56719) KJS / Gradle: Compile sync task has to sync only changed files
- [`KT-56131`](https://youtrack.jetbrains.com/issue/KT-56131) KJS / Gradle: Could not create an instance of type org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinNodeJs when task configuration avoidance is broken
- [`KT-37668`](https://youtrack.jetbrains.com/issue/KT-37668) Kotlin/JS: nodeTest failure due to main() function fail is reported as "BUILD SUCCESSFUL"
- [`KT-35285`](https://youtrack.jetbrains.com/issue/KT-35285) Kotlin/JS + Gradle: browserDevelopementWebpack and browserProductionWebpack could write to different locations
- [`KT-55593`](https://youtrack.jetbrains.com/issue/KT-55593) KotlinJsCompilerType and KotlinJsCompilerTypeHolder LEGACY and BOTH constants should be marked as `@Deprecated` in 1.8
- [`KT-33291`](https://youtrack.jetbrains.com/issue/KT-33291) JS: No build result with gradle parallel build in multiproject build
- [`KT-40925`](https://youtrack.jetbrains.com/issue/KT-40925) KJS: need a way to configure extra environment variables for the test task
- [`KT-47236`](https://youtrack.jetbrains.com/issue/KT-47236) KJS: `kotlinNpmInstall` fails if no yarn is downloaded
- [`KT-53288`](https://youtrack.jetbrains.com/issue/KT-53288) KJS / Gradle: FileNotFoundException when customising moduleName
- [`KT-54511`](https://youtrack.jetbrains.com/issue/KT-54511) Kotlin/JS generated package.json main field is not correctly set if module name contains a `/` (slash)
- [`KT-54421`](https://youtrack.jetbrains.com/issue/KT-54421) KJS / Legacy: Kotlin 1.7.20 fails when running tests on Node 14
- [`KT-54503`](https://youtrack.jetbrains.com/issue/KT-54503) Make the target observer API public in kotlin JS Extension
- [`KT-54418`](https://youtrack.jetbrains.com/issue/KT-54418) KJS: Change test running with kotlin-test adapter
- [`KT-54132`](https://youtrack.jetbrains.com/issue/KT-54132) KJS IR: Sometimes karma failed on teamcity because of uninitialized browser

### Tools. Gradle. Multiplatform

#### New Features

- [`KT-54766`](https://youtrack.jetbrains.com/issue/KT-54766) TCS: external Android Target APIs: Milestone: Compile
- [`KT-50967`](https://youtrack.jetbrains.com/issue/KT-50967) Make c-interop libs resolve robust to build clean
- [`KT-53570`](https://youtrack.jetbrains.com/issue/KT-53570) multiplatform 'natural hierarchy' prototype

#### Fixes

- [`KT-36943`](https://youtrack.jetbrains.com/issue/KT-36943) Gradle Plugin (multiplatform) - Consider publishing a 'sourcesElements' variant for the sources.jar
- [`KT-57460`](https://youtrack.jetbrains.com/issue/KT-57460) Kotlin Gradle Plugin: Null `this` pointer in transformCommonMainDependenciesMetadata
- [`KT-57306`](https://youtrack.jetbrains.com/issue/KT-57306) [Kotlin 1.8.20-RC] GradleException: Could not load the value of field `apiConfiguration` of `org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationConfigurationsContainer`
- [`KTIJ-23750`](https://youtrack.jetbrains.com/issue/KTIJ-23750) KotlinMppModelSerializationService is not invoked in production environment
- [`KTIJ-24223`](https://youtrack.jetbrains.com/issue/KTIJ-24223) Update latest tested KGP version to 1.9.0-dev-764
- [`KT-56536`](https://youtrack.jetbrains.com/issue/KT-56536) Multiplatform: Composite build fails on included build with rootProject.name != buildIdentifier.name
- [`KTIJ-23889`](https://youtrack.jetbrains.com/issue/KTIJ-23889) TCS: Gradle Sync: Match sources.jar dependencies to sourceSetName scoped dependencies
- [`KT-56729`](https://youtrack.jetbrains.com/issue/KT-56729) commonizeCInterop: Duplicated libraries: co.touchlab:sqliter-driver-cinterop-sqlite3
- [`KT-56510`](https://youtrack.jetbrains.com/issue/KT-56510) Import with included plugin build may fail with OverlappingFileLockException during commonizeNativeDistribution
- [`KT-54180`](https://youtrack.jetbrains.com/issue/KT-54180) TCS: Initial external Android Target APIs
- [`KT-55010`](https://youtrack.jetbrains.com/issue/KT-55010) TCS: External Android Target Prototype: Setup Android dependencies
- [`KT-54783`](https://youtrack.jetbrains.com/issue/KT-54783) KotlinJvmWithJavaCompilation mututal .all listener loop creates two compilations
- [`KT-54867`](https://youtrack.jetbrains.com/issue/KT-54867) KotlinWithJavaCompilation does not respect javaSourceSet classpaths
- [`KT-52413`](https://youtrack.jetbrains.com/issue/KT-52413) MPP: Gradle dependency substitution breaks KMP import for native targets
- [`KT-56115`](https://youtrack.jetbrains.com/issue/KT-56115) Multiplatform;Composite Builds: Support import with cinterop commonization enabled
- [`KT-54312`](https://youtrack.jetbrains.com/issue/KT-54312) TCS: Replace CompilationDetails abstract class hierarchy by composable implementation
- [`KT-47441`](https://youtrack.jetbrains.com/issue/KT-47441) MPP: Unresolved reference for dependent on the other module with dependencySubstitution
- [`KT-56429`](https://youtrack.jetbrains.com/issue/KT-56429) Fix flaky: MppIdeDependencyResolutionIT.test cinterops - are stored in root gradle folder
- [`KT-56337`](https://youtrack.jetbrains.com/issue/KT-56337) Unable to import a project with cinterop with enableKgpDependencyResolution
- [`KT-55873`](https://youtrack.jetbrains.com/issue/KT-55873) Unrequested dependencies leaking into common source sets: Regression after 703fd0f2
- [`KT-55891`](https://youtrack.jetbrains.com/issue/KT-55891) Deprecate pre-HMPP flags
- [`KT-56278`](https://youtrack.jetbrains.com/issue/KT-56278) TCS: Gradle Sync: [MISSING_DEPENDENCY_CLASS] on libraries used in shared native source sets
- [`KT-56143`](https://youtrack.jetbrains.com/issue/KT-56143) CInteropDependencyConfiguration and CInteropApiElementsConfiguration are missing attributes defined on target
- [`KT-56285`](https://youtrack.jetbrains.com/issue/KT-56285) TCS: Gradle Sync: IdeProjectToProjectCInteropDependencyResolver: Ensure lenient resolution
- [`KT-56204`](https://youtrack.jetbrains.com/issue/KT-56204) KotlinTargetHierarchy: Changing naming from 'any' to 'with' prefix
- [`KT-56111`](https://youtrack.jetbrains.com/issue/KT-56111) Multiplatform;Composite Builds: Clean builds fail on when 'hostSpecificMetadata' is required
- [`KT-54974`](https://youtrack.jetbrains.com/issue/KT-54974) TCS: Gradle Sync: Implement IdeKotlinDependencyResolvers
- [`KT-38712`](https://youtrack.jetbrains.com/issue/KT-38712) Gradle configuration's name with word "implementation" is camelcased to "İmplementation" if default locale is Turkish
- [`KT-54975`](https://youtrack.jetbrains.com/issue/KT-54975) TCS: Gradle Sync: Implement stdlib-common filter for platform source sets
- [`KT-48839`](https://youtrack.jetbrains.com/issue/KT-48839) Sources.jar of the root artifact of MPP library includes source files from test sourcesets
- [`KT-55492`](https://youtrack.jetbrains.com/issue/KT-55492) TCS: Gradle Sync: Sources and Documentation as extra 'classpath'
- [`KT-55237`](https://youtrack.jetbrains.com/issue/KT-55237) TCS: Gradle Sync: Support stdlib-native sources
- [`KT-55475`](https://youtrack.jetbrains.com/issue/KT-55475) TCS: Gradle Sync: Fine tune jvmAndAndroid source sets
- [`KT-55189`](https://youtrack.jetbrains.com/issue/KT-55189) TCS: Gradle Sync: Support icons (native, js) and global libraries
- [`KT-55218`](https://youtrack.jetbrains.com/issue/KT-55218) KotlinTargetHierarchy: Disambiguate declaring targets vs including targets
- [`KT-55112`](https://youtrack.jetbrains.com/issue/KT-55112) TCS: Gradle Sync: Resolve Source Dependencies
- [`KT-54977`](https://youtrack.jetbrains.com/issue/KT-54977) TCS: Gradle Sync: Implement debugging tools
- [`KT-54948`](https://youtrack.jetbrains.com/issue/KT-54948) TCS: Gradle Sync: Port IdeaKpmPlatformDependencyResolver to TCS
- [`KT-55289`](https://youtrack.jetbrains.com/issue/KT-55289) TCS: Gradle Sync: Prototypical sources jar resolution
- [`KT-55238`](https://youtrack.jetbrains.com/issue/KT-55238) TCS: Gradle Sync: Support commonized native distribution
- [`KT-55230`](https://youtrack.jetbrains.com/issue/KT-55230) Remove metadata dependencies transformation for runtimeOnly scope
- [`KT-53338`](https://youtrack.jetbrains.com/issue/KT-53338) Prettify the message about incompatible AGP and KGP versions
- [`KT-55134`](https://youtrack.jetbrains.com/issue/KT-55134) MPP / Gradle: Cannot read test tasks state
- [`KT-54506`](https://youtrack.jetbrains.com/issue/KT-54506) Test tasks are considered up-to-date after a failure when triggered by `allTests`
- [`KTIJ-23509`](https://youtrack.jetbrains.com/issue/KTIJ-23509) Update latest tested KGP version to 1.8.20-dev-1815
- [`KT-54787`](https://youtrack.jetbrains.com/issue/KT-54787) Test tasks are not up-to-date when an individual test task called after aggregating test task
- [`KT-54033`](https://youtrack.jetbrains.com/issue/KT-54033) Multiplatform/Android Source Set Layout 1: Also support setting source dirs using AGP Apis
- [`KT-54202`](https://youtrack.jetbrains.com/issue/KT-54202) CInterop Commonization fails on first run when native distribution is not yet commonizied
- [`KT-54135`](https://youtrack.jetbrains.com/issue/KT-54135) Add documentation with examples to CompositeMetadataArtifact

### Tools. Gradle. Native

- [`KT-37051`](https://youtrack.jetbrains.com/issue/KT-37051) MPP Gradle plugin: duplicated cinterop libraries in composite build
- [`KT-55650`](https://youtrack.jetbrains.com/issue/KT-55650) Pass through errors from Gradle to Xcode
- [`KT-56205`](https://youtrack.jetbrains.com/issue/KT-56205) Shared Native Compilation: False positive 'w: Could not find' warnings on metadata klibs
- [`KT-54969`](https://youtrack.jetbrains.com/issue/KT-54969) Support podspec generation for the new K/N artifact DSL

### Tools. Incremental Compile

- [`KT-55021`](https://youtrack.jetbrains.com/issue/KT-55021) New IC: "The following LookupSymbols are not yet converted to programSymbols" when removing/renaming file facades
- [`KTIJ-21161`](https://youtrack.jetbrains.com/issue/KTIJ-21161) Incremental build is taking too long when no files have changed in the project
- [`KT-53832`](https://youtrack.jetbrains.com/issue/KT-53832) Enable new incremental compilation by default in Gradle
- [`KT-55622`](https://youtrack.jetbrains.com/issue/KT-55622) MPP: Incremental compilation ignores changes in source set structure
- [`KT-55309`](https://youtrack.jetbrains.com/issue/KT-55309) IC: Get rid of `NonCachingLazyStorage`
- [`KT-53402`](https://youtrack.jetbrains.com/issue/KT-53402) Incremental compilation tries to compile resources
- [`KT-54791`](https://youtrack.jetbrains.com/issue/KT-54791) Incremental compilation in JPS broken in 1.8.20-dev-1815

### Tools. JPS

- [`KT-56165`](https://youtrack.jetbrains.com/issue/KT-56165) Language version 1.9 and 2.0 is absent in Kotlin Compiler settings
- [`KT-51536`](https://youtrack.jetbrains.com/issue/KT-51536) [JPS] Recompile module on facet settings change
- [`KT-53735`](https://youtrack.jetbrains.com/issue/KT-53735) JPS / IC: "IOException: The system cannot find the file specified" on Windows
- [`KT-47983`](https://youtrack.jetbrains.com/issue/KT-47983) [JPS] Adding compilerSettings to Facet should initiate rebuild of module
- [`KT-54449`](https://youtrack.jetbrains.com/issue/KT-54449) Cyrillic characters in a filename break builds on linux

### Tools. Kapt

#### New Features

- [`KT-53135`](https://youtrack.jetbrains.com/issue/KT-53135) Enable JVM IR for KAPT stub generation by default
- [`KT-41129`](https://youtrack.jetbrains.com/issue/KT-41129) kotlin-maven-plugin + kapt - allow aptMode to be set according to docs

#### Fixes

- [`KT-56635`](https://youtrack.jetbrains.com/issue/KT-56635) KAPT / IR: "Unresolved reference: DaggerGeneratedCodeTest_AppComponent " caused by stub generation with Kotlin 1.8.20-Beta
- [`KT-56360`](https://youtrack.jetbrains.com/issue/KT-56360) Kapt with JVM IR changes fields order
- [`KT-54380`](https://youtrack.jetbrains.com/issue/KT-54380) Kapt / IR: Build failed when inheritance by functional interface with suspend modifier
- [`KT-54245`](https://youtrack.jetbrains.com/issue/KT-54245) JVM IR / Kapt / Serialization: NullPointerException in SerializableIrGenerator.kt
- [`KT-33847`](https://youtrack.jetbrains.com/issue/KT-33847) Kapt does not included Filer-generated class files on compilation classpath
- [`KT-55490`](https://youtrack.jetbrains.com/issue/KT-55490) Kapt + JVM IR: "annotation `@Foo` is missing default values"
- [`KT-43786`](https://youtrack.jetbrains.com/issue/KT-43786) KAPT: IllegalStateException: SimpleTypeImpl should not be created for error type: ErrorScope
- [`KT-43117`](https://youtrack.jetbrains.com/issue/KT-43117) Kapt: "System is already defined in this compilation unit"
- [`KT-46966`](https://youtrack.jetbrains.com/issue/KT-46966) Kapt: correctErrorTypes: receiver type is NonExistentClass
- [`KT-46965`](https://youtrack.jetbrains.com/issue/KT-46965) Kapt: correctErrorTypes: custom setter gets Object parameter type
- [`KT-51087`](https://youtrack.jetbrains.com/issue/KT-51087) KAPT: `@JvmRepeatable` annotations are present in inverse order in KAPT stubs
- [`KT-54870`](https://youtrack.jetbrains.com/issue/KT-54870) KAPT stub generation with JVM_IR backend throws exception for delegated properties
- [`KT-44350`](https://youtrack.jetbrains.com/issue/KT-44350) Kapt Gradle integration tests failing with Android Gradle plugin 7.0
- [`KT-54030`](https://youtrack.jetbrains.com/issue/KT-54030) Kapt: annotation processor warnings are displayed as errors on JDK 17+
- [`KT-32596`](https://youtrack.jetbrains.com/issue/KT-32596) kapt replaces class generated by annotation processor with error.NonExistentClass when the class is used as an annotation
- [`KT-37586`](https://youtrack.jetbrains.com/issue/KT-37586) KAPT: When delegated property use an unknown type (to-be-generated class), `correctTypeError` will mess up the `$delegate` field type

### Tools. Maven

- [`KT-56697`](https://youtrack.jetbrains.com/issue/KT-56697) IC: "Incremental compilation was attempted but failed" Failed to get changed files: java.io.IOException: readPrevChunkAddress
- [`KT-55709`](https://youtrack.jetbrains.com/issue/KT-55709) Maven: "java.lang.reflect.InaccessibleObjectException: Unable to make field protected java.io.OutputStream java.io.FilterOutputStream.out accessible"
- [`KT-29346`](https://youtrack.jetbrains.com/issue/KT-29346) Add components.xml to automatically compile kotlin maven projects
- [`KT-13995`](https://youtrack.jetbrains.com/issue/KT-13995) Maven: Kotlin compiler plugin should respect model's compile source roots
- [`KT-54822`](https://youtrack.jetbrains.com/issue/KT-54822) Maven: Too low-level error message "Parameter specified as non-null is null: method kotlin.text.StringsKt__StringsJVMKt.startsWith, parameter <this>" from Kotlin Maven plugin invoking compiler when <arg> tags are empty
- [`KT-47110`](https://youtrack.jetbrains.com/issue/KT-47110) Disable jdk8-specific warnings in kotlin-maven-plugin

### Tools. Scripts

- [`KT-54095`](https://youtrack.jetbrains.com/issue/KT-54095) It is difficult (if not impossible) to use kotlin compiler plugins with scripting
- [`KT-54461`](https://youtrack.jetbrains.com/issue/KT-54461) Warnings and stack traces when executing scripts via kotlin-maven-plugin
- [`KT-54733`](https://youtrack.jetbrains.com/issue/KT-54733) Scripts: ConcurrentModificationException in *.main.kts scripts
- [`KT-53283`](https://youtrack.jetbrains.com/issue/KT-53283) Scripts: main-kts JAR does not relocate embedded SLF4J and jsoup libraries


## 1.8.0

### Analysis API

- [`KT-50255`](https://youtrack.jetbrains.com/issue/KT-50255) Analysis API: Implement standalone mode for the Analysis API

### Analysis API. FIR

- [`KT-54292`](https://youtrack.jetbrains.com/issue/KT-54292) Symbol Light classes: implement PsiVariable.computeConstantValue for light field
- [`KT-54293`](https://youtrack.jetbrains.com/issue/KT-54293) Analysis API: fix constructor symbol creation when its accessed via type alias

### Android

- [`KT-53342`](https://youtrack.jetbrains.com/issue/KT-53342) TCS: New AndroidSourceSet layout for multiplatform
- [`KT-53013`](https://youtrack.jetbrains.com/issue/KT-53013) Increase AGP compile version in KGP to 4.1.3
- [`KT-54013`](https://youtrack.jetbrains.com/issue/KT-54013) Report error when using deprecated Kotlin Android Extensions compiler plugin
- [`KT-53709`](https://youtrack.jetbrains.com/issue/KT-53709) MPP, Android SSL2: Conflicting warnings for `androidTest/kotlin` source set folder

### Backend. Native. Debug

- [`KT-53561`](https://youtrack.jetbrains.com/issue/KT-53561) Invalid LLVM module: "inlinable function call in a function with debug info must have a !dbg location"

### Compiler

#### New Features

- [`KT-52817`](https://youtrack.jetbrains.com/issue/KT-52817) Add `@JvmSerializableLambda` annotation to keep old behavior of non-invokedynamic lambdas
- [`KT-54460`](https://youtrack.jetbrains.com/issue/KT-54460) Implementation of non-local break and continue
- [`KT-53916`](https://youtrack.jetbrains.com/issue/KT-53916) Support Xcode 14 and new Objective-C frameworks in Kotlin/Native compiler
- [`KT-32208`](https://youtrack.jetbrains.com/issue/KT-32208) Generate method annotations into bytecode for suspend lambdas (on invokeSuspend)
- [`KT-53438`](https://youtrack.jetbrains.com/issue/KT-53438) Introduce a way to get SourceDebugExtension attribute value via JVMTI for profiler and coverage

#### Performance Improvements

- [`KT-53347`](https://youtrack.jetbrains.com/issue/KT-53347) Get rid of excess allocations in parser
- [`KT-53689`](https://youtrack.jetbrains.com/issue/KT-53689) JVM: Optimize equality on class literals
- [`KT-53119`](https://youtrack.jetbrains.com/issue/KT-53119) Improve String Concatenation Lowering

#### Fixes

- [`KT-53465`](https://youtrack.jetbrains.com/issue/KT-53465) Unnecessary checkcast to array of reified type is not optimized since Kotlin 1.6.20
- [`KT-49658`](https://youtrack.jetbrains.com/issue/KT-49658) NI: False negative TYPE_MISMATCH on nullable type with `when`
- [`KT-48162`](https://youtrack.jetbrains.com/issue/KT-48162) NON_VARARG_SPREAD isn't reported on *toTypedArray() call
- [`KT-43493`](https://youtrack.jetbrains.com/issue/KT-43493) NI: False negative: no compilation error "Operator '==' cannot be applied to 'Long' and 'Int'" is reported in builder inference lambdas
- [`KT-54393`](https://youtrack.jetbrains.com/issue/KT-54393) Change in behavior from 1.7.10 to 1.7.20 for java field override.
- [`KT-55357`](https://youtrack.jetbrains.com/issue/KT-55357) IllegalStateException when reading a class that delegates to a Java class with a definitely-not-null type with a flexible upper bound
- [`KT-55068`](https://youtrack.jetbrains.com/issue/KT-55068) Kotlin Gradle DSL: No mapping for symbol: VALUE_PARAMETER SCRIPT_IMPLICIT_RECEIVER on JVM IR backend
- [`KT-51284`](https://youtrack.jetbrains.com/issue/KT-51284) SAM conversion doesn't work if method has context receivers
- [`KT-48532`](https://youtrack.jetbrains.com/issue/KT-48532) Remove old JVM backend
- [`KT-55065`](https://youtrack.jetbrains.com/issue/KT-55065) Kotlin Gradle DSL: Reflection cannot find class data for lambda, produced by JVM IR backend
- [`KT-53270`](https://youtrack.jetbrains.com/issue/KT-53270) K1: implement synthetic Enum.entries property
- [`KT-52823`](https://youtrack.jetbrains.com/issue/KT-52823) Cannot access class Thread.State after upgrading to 1.7 from 1.6.1 using -Xjdk-release=1.8
- [`KT-55108`](https://youtrack.jetbrains.com/issue/KT-55108) IR interpreter: Error occurred while optimizing an expression: VARARG
- [`KT-53547`](https://youtrack.jetbrains.com/issue/KT-53547) Missing fun IrBuilderWithScope.irFunctionReference
- [`KT-54884`](https://youtrack.jetbrains.com/issue/KT-54884) "StackOverflowError: null" caused by Enum constant name in constructor of the same Enum constant
- [`KT-47475`](https://youtrack.jetbrains.com/issue/KT-47475) "IncompatibleClassChangeError: disagree on InnerClasses attribute": cross-module inlined WhenMappings has mismatched InnerClasses
- [`KT-55013`](https://youtrack.jetbrains.com/issue/KT-55013) State checker use-after-free with XCode 14.1
- [`KT-54802`](https://youtrack.jetbrains.com/issue/KT-54802) "VerifyError: Bad type on operand stack" for inline functions on arrays
- [`KT-54707`](https://youtrack.jetbrains.com/issue/KT-54707) "VerifyError: Bad type on operand stack" in inline call chain on a nullable array value
- [`KT-48678`](https://youtrack.jetbrains.com/issue/KT-48678) Coroutine debugger: disable "was optimised out" compiler feature
- [`KT-54745`](https://youtrack.jetbrains.com/issue/KT-54745) Restore KtToken constructors without tokenId parameter to preserve back compatibility
- [`KT-54650`](https://youtrack.jetbrains.com/issue/KT-54650) Binary incompatible ABI change in Kotlin 1.7.20
- [`KT-52786`](https://youtrack.jetbrains.com/issue/KT-52786) Frontend / K2: IndexOutOfBoundsException when opting in to K2
- [`KT-54004`](https://youtrack.jetbrains.com/issue/KT-54004) Builder type inference does not work correctly with variable assignment and breaks run-time
- [`KT-54581`](https://youtrack.jetbrains.com/issue/KT-54581) JVM: "VerifyError: Bad type on operand stack" with generic inline function and `when` inside try-catch block
- [`KT-53794`](https://youtrack.jetbrains.com/issue/KT-53794) IAE "Unknown visibility: protected/*protected and package*/" on callable reference to protected member of Java superclass
- [`KT-54600`](https://youtrack.jetbrains.com/issue/KT-54600) NPE on passing nullable Kotlin lambda as Java's generic SAM interface with `super` type bound
- [`KT-54463`](https://youtrack.jetbrains.com/issue/KT-54463) Delegating to a field with a platform type causes java.lang.NoSuchFieldError: value$delegate
- [`KT-54509`](https://youtrack.jetbrains.com/issue/KT-54509) Ir Interpreter: unable to evaluate string concatenation with "this" as argument
- [`KT-54615`](https://youtrack.jetbrains.com/issue/KT-54615) JVM: Internal error in file lowering: java.lang.AssertionError: Error occurred while optimizing an expression
- [`KT-53146`](https://youtrack.jetbrains.com/issue/KT-53146) JVM IR: unnecessary checkcast of null leads to NoClassDefFoundError if the type isn't available at runtime
- [`KT-53712`](https://youtrack.jetbrains.com/issue/KT-53712) Add mode to prevent generating JVM 1.8+ annotation targets (TYPE_USE, TYPE_PARAMETER)
- [`KT-54366`](https://youtrack.jetbrains.com/issue/KT-54366) K2: no JVM BE specific diagnostics (in particular CONFLICTING_JVM_DECLARATIONS) in 1.8
- [`KT-35187`](https://youtrack.jetbrains.com/issue/KT-35187) NullPointerException on compiling suspend inline fun with typealias to suspend function type
- [`KT-54275`](https://youtrack.jetbrains.com/issue/KT-54275) K2: "IllegalArgumentException: KtParameter is not a subtype of class KtAnnotationEntry for factory REPEATED_ANNOTATION"
- [`KT-53656`](https://youtrack.jetbrains.com/issue/KT-53656) "IllegalStateException: typeParameters == null for SimpleFunctionDescriptorImpl" with recursive generic type parameters
- [`KT-46727`](https://youtrack.jetbrains.com/issue/KT-46727) Report warning on contravariant usages of star projected argument from Java
- [`KT-53197`](https://youtrack.jetbrains.com/issue/KT-53197) K2: 'init' hides member of supertype 'UIComponent' and needs 'override' modifier
- [`KT-53867`](https://youtrack.jetbrains.com/issue/KT-53867) K2: `@JvmRecord` does not compile to a java record
- [`KT-53964`](https://youtrack.jetbrains.com/issue/KT-53964) K2 is unable to work with Java records
- [`KT-53349`](https://youtrack.jetbrains.com/issue/KT-53349) K2: TYPE_MISMATCH caused by non-local return
- [`KT-54100`](https://youtrack.jetbrains.com/issue/KT-54100) "Type variable TypeVariable(P) should not be fixed" crash in code with errors
- [`KT-54212`](https://youtrack.jetbrains.com/issue/KT-54212) K2: cannot calculate implicit property type
- [`KT-53699`](https://youtrack.jetbrains.com/issue/KT-53699) K2: Exception during IR lowering in code with coroutines
- [`KT-54192`](https://youtrack.jetbrains.com/issue/KT-54192) Warn about unsupported feature on generic inline class parameters
- [`KT-53723`](https://youtrack.jetbrains.com/issue/KT-53723) Friend modules aren't getting passed to cache build during box tests
- [`KT-53873`](https://youtrack.jetbrains.com/issue/KT-53873) K2: Duplicated diagnostics reported from user type ref checkers
- [`KT-50909`](https://youtrack.jetbrains.com/issue/KT-50909) "VerifyError: Bad type on operand stack" caused by smartcasting for nullable inline class property in class
- [`KT-54115`](https://youtrack.jetbrains.com/issue/KT-54115) Restore Psi2IrTranslator constructor from 1.7.20
- [`KT-53908`](https://youtrack.jetbrains.com/issue/KT-53908) K2: Self-referencing generics in Java class causes New Inference Error (IE: class Foo<T extends Foo<T>>)
- [`KT-53193`](https://youtrack.jetbrains.com/issue/KT-53193) K2: compile error on project that compiles fine with normal 1.7.10
- [`KT-54062`](https://youtrack.jetbrains.com/issue/KT-54062) K2 Invalid serialization for type-aliased suspend function type with extension receiver
- [`KT-53953`](https://youtrack.jetbrains.com/issue/KT-53953) Forbid usages of super or super<Some> if in fact it accesses an abstract member
- [`KT-47473`](https://youtrack.jetbrains.com/issue/KT-47473) NI: Missed UPPER_BOUND_VIOLATED diagnostics if use type aliases with type parameters
- [`KT-54049`](https://youtrack.jetbrains.com/issue/KT-54049) K2: false positive MANY_IMPL_MEMBER_NOT_IMPLEMENTED
- [`KT-30054`](https://youtrack.jetbrains.com/issue/KT-30054) Wrong approximation if nullable anonymous object with implemented interface is used
- [`KT-53751`](https://youtrack.jetbrains.com/issue/KT-53751) Postpone IgnoreNullabilityForErasedValueParameters feature
- [`KT-53324`](https://youtrack.jetbrains.com/issue/KT-53324) Implement Enum.entries lowering on K/N
- [`KT-44441`](https://youtrack.jetbrains.com/issue/KT-44441) K2: report redeclaration error if there is a Java class with the same name as the Kotlin class
- [`KT-53807`](https://youtrack.jetbrains.com/issue/KT-53807) No warning about declaringClass on an enum value
- [`KT-53493`](https://youtrack.jetbrains.com/issue/KT-53493) K2: `val on function parameter` counts as just warning
- [`KT-53435`](https://youtrack.jetbrains.com/issue/KT-53435) K2: "IllegalArgumentException: class KtValueArgument is not a subtype of class KtExpression for factory ANNOTATION_ARGUMENT_MUST_BE_CONST" if string in nested annotation is concatenated
- [`KT-52927`](https://youtrack.jetbrains.com/issue/KT-52927) AssertionError: LambdaKotlinCallArgumentImpl
- [`KT-53922`](https://youtrack.jetbrains.com/issue/KT-53922) Make Enum.entries unstable feature to poison binaries
- [`KT-53783`](https://youtrack.jetbrains.com/issue/KT-53783) Exception during psi2ir when declaring expect data object
- [`KT-53622`](https://youtrack.jetbrains.com/issue/KT-53622) [OVERLOAD_RESOLUTION_AMBIGUITY] when enum entry called 'entries' is present in K2
- [`KT-41670`](https://youtrack.jetbrains.com/issue/KT-41670) JVM IR: AbstractMethodError when using inheritance for fun interfaces
- [`KT-53178`](https://youtrack.jetbrains.com/issue/KT-53178) K2: implement diagnostics for serialization plugin
- [`KT-53804`](https://youtrack.jetbrains.com/issue/KT-53804) Restore old and incorrect logic of generating InnerClasses attributes for kotlin-stdlib
- [`KT-52970`](https://youtrack.jetbrains.com/issue/KT-52970) Default value constant in companion object works on JVM and JS, but fails on native
- [`KT-51114`](https://youtrack.jetbrains.com/issue/KT-51114) FIR: Support DNN checks
- [`KT-27936`](https://youtrack.jetbrains.com/issue/KT-27936) Write InnerClasses attribute for all class names used in a class file
- [`KT-53719`](https://youtrack.jetbrains.com/issue/KT-53719) Parsing regression on function call with type arguments and labeled lambda
- [`KT-53261`](https://youtrack.jetbrains.com/issue/KT-53261) Evaluate effect from <T-unbox> inline for primitive types
- [`KT-53706`](https://youtrack.jetbrains.com/issue/KT-53706) K2: Context receivers are not resolved on properties during type resolution stage
- [`KT-39492`](https://youtrack.jetbrains.com/issue/KT-39492) Kotlin.Metadata's packageName field cannot be an empty string
- [`KT-53664`](https://youtrack.jetbrains.com/issue/KT-53664) Ir Interpreter: unable to evaluate name of function reference marked with JvmStatic from another module
- [`KT-52478`](https://youtrack.jetbrains.com/issue/KT-52478) [Native] Partial linkage: Building native binary from cached KLIBs fails if one library depends on removed nested callable member from another one
- [`KT-48822`](https://youtrack.jetbrains.com/issue/KT-48822) CompilationException: Back-end (JVM) Internal error: Failed to generate expression: KtProperty - ConcurrentModificationException
- [`KT-50281`](https://youtrack.jetbrains.com/issue/KT-50281) IllegalStateException: unsupported call of reified inlined function
- [`KT-50083`](https://youtrack.jetbrains.com/issue/KT-50083) Different error messages in android and JVM (Intrinsics.checkNotNullParameter).
- [`KT-53236`](https://youtrack.jetbrains.com/issue/KT-53236) Support Enum.entries codegen on JVM/IR BE
- [`KT-41017`](https://youtrack.jetbrains.com/issue/KT-41017) FIR: should we support smartcast after null check
- [`KT-53202`](https://youtrack.jetbrains.com/issue/KT-53202) "ISE: Descriptor can be left only if it is last" after direct invoke optimization on a capturing lambda
- [`KT-46969`](https://youtrack.jetbrains.com/issue/KT-46969) `@BuilderInference` with nested DSL scopes cause false-positive scope violation in Kotlin 1.5
- [`KT-53257`](https://youtrack.jetbrains.com/issue/KT-53257) FIR: Improper context receiver argument is chosen when there are two extension receiver candidates
- [`KT-53090`](https://youtrack.jetbrains.com/issue/KT-53090) Anonymous function and extension function literals are generated as classes even with -Xlambdas=indy
- [`KT-53208`](https://youtrack.jetbrains.com/issue/KT-53208) K2: Cannot get annotation for default interface method parameter when compiled with `-Xuse-k2`
- [`KT-53184`](https://youtrack.jetbrains.com/issue/KT-53184) K2: NoSuchMethodError on KProperty1.get() referenced via nullable typealias
- [`KT-53198`](https://youtrack.jetbrains.com/issue/KT-53198) K2: Return type mismatch: expected kotlin/Unit, actual kotlin/Unit?
- [`KT-53100`](https://youtrack.jetbrains.com/issue/KT-53100) Optimization needed: <T-unbox>(CONSTANT_PRIMITIVE(x: T?)) => x
- [`KT-49875`](https://youtrack.jetbrains.com/issue/KT-49875) [FIR] Support infering PRIVATE_TO_THIS visibility
- [`KT-53024`](https://youtrack.jetbrains.com/issue/KT-53024) Refactor FIR renderer to composable architecture
- [`KT-50995`](https://youtrack.jetbrains.com/issue/KT-50995) [FIR] Support SAM with receiver plugin
- [`KT-53148`](https://youtrack.jetbrains.com/issue/KT-53148) K1: introduce warning for inline virtual member in enum
- [`KT-49847`](https://youtrack.jetbrains.com/issue/KT-49847) Devirtualization fails to eliminate boxing in function reference context
- [`KT-52875`](https://youtrack.jetbrains.com/issue/KT-52875) Extension function literal creation with `-Xlambdas=indy` fails with incorrect arguments
- [`KT-53072`](https://youtrack.jetbrains.com/issue/KT-53072) INVALID_IF_AS_EXPRESSION error isn't shown in the IDE (LV 1.8)
- [`KT-52985`](https://youtrack.jetbrains.com/issue/KT-52985) Native: a function with type `T?` returned a `kotlin.Unit` instead of `null`
- [`KT-52020`](https://youtrack.jetbrains.com/issue/KT-52020) FIR warning message includes internal rendering
- [`KT-48778`](https://youtrack.jetbrains.com/issue/KT-48778) -Xtype-enhancement-improvements-strict-mode not respecting `@NonNull` annotation for property accesses?

### IDE

#### Fixes

- [`KTIJ-22357`](https://youtrack.jetbrains.com/issue/KTIJ-22357) CCE “class org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl cannot be cast to class org.jetbrains.kotlin.fir.types.FirResolvedTypeRef” in K2
- [`KT-55150`](https://youtrack.jetbrains.com/issue/KT-55150) Argument for `@NotNull` parameter 'scope' of org/jetbrains/kotlin/resolve/AnnotationResolverImpl.resolveAnnotationType must not be null
- [`KTIJ-22165`](https://youtrack.jetbrains.com/issue/KTIJ-22165) IDE notification to promote users to migrate to the new Kotlin/JS toolchain
- [`KTIJ-22166`](https://youtrack.jetbrains.com/issue/KTIJ-22166) IDE notification (or something else) about JPS and Maven support for Kotlin/JS is deprecated
- [`KT-53543`](https://youtrack.jetbrains.com/issue/KT-53543) Rework light classes for file facade
- [`KT-48773`](https://youtrack.jetbrains.com/issue/KT-48773) Investigate the possibility of removing dependency on old JVM backend in light classes
- [`KTIJ-19699`](https://youtrack.jetbrains.com/issue/KTIJ-19699) IDE: False positive type mismatch in Java code for Kotlin nested class non-direct inheritor from external library
- [`KT-51101`](https://youtrack.jetbrains.com/issue/KT-51101) FIR IDE: Exception on "Show Type Info" action
- [`KTIJ-22295`](https://youtrack.jetbrains.com/issue/KTIJ-22295) MPP, IDE: False positive UPPER_BOUND_VIOLATED when JVM module implements the generic interface from MPP module and the type parameter is not equal to itself.
- [`KT-51656`](https://youtrack.jetbrains.com/issue/KT-51656) FIR IDE: ProgressCancelled exception is masked in the compiler during resolve
- [`KT-51315`](https://youtrack.jetbrains.com/issue/KT-51315) FIR IDE: move out base modules from fe10 plugin to reuse in k2 plugin
- [`KTIJ-22323`](https://youtrack.jetbrains.com/issue/KTIJ-22323) K2: ISE during resolve of stdlib calls from the stdlib
- [`KTIJ-21391`](https://youtrack.jetbrains.com/issue/KTIJ-21391) Generate -> Override methods : don't delegate to abstract methods
- [`KT-53097`](https://youtrack.jetbrains.com/issue/KT-53097) Extract common part of light classes to another module
- [`KTIJ-22354`](https://youtrack.jetbrains.com/issue/KTIJ-22354) FIR LC: annotation owner is always null
- [`KTIJ-22157`](https://youtrack.jetbrains.com/issue/KTIJ-22157) Kotlin call resolver leaks user code when reporting exception

### IDE. Completion

- [`KTIJ-22552`](https://youtrack.jetbrains.com/issue/KTIJ-22552) Kotlin: 'for loop' postfix completion doesn't work - "Fe10SuggestVariableNameMacro must be not requested from main classloader"
- [`KTIJ-22503`](https://youtrack.jetbrains.com/issue/KTIJ-22503) Support code completion for data objects

### IDE. Debugger

- [`KT-51755`](https://youtrack.jetbrains.com/issue/KT-51755) Compilation exception with scripting compilation during debug session
- [`KTIJ-21963`](https://youtrack.jetbrains.com/issue/KTIJ-21963) Debugger / IR: Expression evaluation of the debugger doesn't work

### IDE. Decompiler, Indexing, Stubs

- [`KTIJ-22750`](https://youtrack.jetbrains.com/issue/KTIJ-22750) Initialize Kotlin stub element types lazily
- [`KTIJ-18094`](https://youtrack.jetbrains.com/issue/KTIJ-18094) IDE: "AssertionError: Stub count doesn't match stubbed node length" with minified Android AAR library
- [`KTIJ-17632`](https://youtrack.jetbrains.com/issue/KTIJ-17632) IndexOutOfBoundsException: Cannot decompile a class located in minified AAR

### IDE. Gradle Integration

- [`KT-48135`](https://youtrack.jetbrains.com/issue/KT-48135) In the IDE import, reuse dependency granular source set KLIBs across multi-project build to avoid duplicate external libraries
- [`KTIJ-22345`](https://youtrack.jetbrains.com/issue/KTIJ-22345) False positive unresolved reference for members of subclasses of expect classes.
- [`KT-53514`](https://youtrack.jetbrains.com/issue/KT-53514) HMPP: False positive for `None of the following functions can be called with the arguments supplied.` with Enum in common module
- [`KT-51583`](https://youtrack.jetbrains.com/issue/KT-51583) Gradle 7.4+ | SamplesVariantRule interference: Could not resolve all files for configuration ':kotlinKlibCommonizerClasspath'
- [`KTIJ-21077`](https://youtrack.jetbrains.com/issue/KTIJ-21077) Dependency matrix does not work with Jetpack compose / multiplatform projects

### IDE. Inspections and Intentions

- [`KTIJ-19531`](https://youtrack.jetbrains.com/issue/KTIJ-19531) Adapt changes about new rules for method implementation requirements
- [`KTIJ-22087`](https://youtrack.jetbrains.com/issue/KTIJ-22087) Support IDE inspections for upcoming data objects
- [`KTIJ-20510`](https://youtrack.jetbrains.com/issue/KTIJ-20510) Quick fix to implement and call correct super method in case of inheritance with defaults
- [`KTIJ-20170`](https://youtrack.jetbrains.com/issue/KTIJ-20170) Provide quickfix for deprecated resolution to private constructor of sealed class
- [`KTIJ-22630`](https://youtrack.jetbrains.com/issue/KTIJ-22630) FIR IDE: Lazy resolve exception after invocation of `Override members` action on value class
- [`KT-49643`](https://youtrack.jetbrains.com/issue/KT-49643) Intentions: "Implement members" fails when base type function declaration uses unresolved generic types

### IDE. JS

- [`KTIJ-22167`](https://youtrack.jetbrains.com/issue/KTIJ-22167) Make JS IR default in projects created by wizard
- [`KTIJ-22332`](https://youtrack.jetbrains.com/issue/KTIJ-22332) Wizard: Kotlin/JS projects: cssSupport DSL should be updated

### IDE. KDoc

- [`KTIJ-22324`](https://youtrack.jetbrains.com/issue/KTIJ-22324) K2 IDE: implement reference resolve inside KDocs

### IDE. Multiplatform

- [`KTIJ-19566`](https://youtrack.jetbrains.com/issue/KTIJ-19566) New Project Wizard: Update HMPP-related flags in multiplatform wizards

### IDE. Navigation

- [`KT-51314`](https://youtrack.jetbrains.com/issue/KT-51314) FIR IDE: show Kotlin declarations in search symbol
- [`KTIJ-22755`](https://youtrack.jetbrains.com/issue/KTIJ-22755) Find usage for constructor from kotlin library doesn't work for secondary constructor usages

### IDE. Script

- [`KTIJ-22598`](https://youtrack.jetbrains.com/issue/KTIJ-22598) Add warning for standalone scripts in source roots
- [`KT-54325`](https://youtrack.jetbrains.com/issue/KT-54325) .settings.gradle.kts and .init.gradle.kts are reported as standalone scripts

### IDE. Structural Search

- [`KTIJ-21986`](https://youtrack.jetbrains.com/issue/KTIJ-21986) KSSR: "CodeFragment with non-kotlin context should have fakeContextForJavaFile set:  originalContext = null" warning shows up when replacing

### IDE. Tests Support

- [`KT-50269`](https://youtrack.jetbrains.com/issue/KT-50269) FIR IDE: Allow running tests via gutter

### IDE. Wizards

- [`KTIJ-23537`](https://youtrack.jetbrains.com/issue/KTIJ-23537) Wizard: projects with Android modules require higher sdkCompileVersion
- [`KTIJ-23525`](https://youtrack.jetbrains.com/issue/KTIJ-23525) Wizard: Compose multiplatform: project won't build and require higher compileSdkVersion
- [`KTIJ-22763`](https://youtrack.jetbrains.com/issue/KTIJ-22763) New Project Wizard: remove deprecated Android extensions plugin from Android target in the project constructor
- [`KTIJ-22481`](https://youtrack.jetbrains.com/issue/KTIJ-22481) Wizard: Kotlin -> Browser application (gradle groove). Build error

### JavaScript

#### Fixes

- [`KT-55097`](https://youtrack.jetbrains.com/issue/KT-55097) KJS / IR + IC: Using an internal function from a friend module throws an unbound symbol exception
- [`KT-54406`](https://youtrack.jetbrains.com/issue/KT-54406) Kotlin/JS: build with dependencies fails with "Could not find "kotlin" in [~/.local/share/kotlin/daemon]"
- [`KT-53074`](https://youtrack.jetbrains.com/issue/KT-53074) Make JS IR BE default in toolchain (gradle & CLI)
- [`KT-50589`](https://youtrack.jetbrains.com/issue/KT-50589) UTF-8 Instability in kotlin.js.map
- [`KT-54934`](https://youtrack.jetbrains.com/issue/KT-54934) KJS / IR + IC: Suspend abstract function stubs are generated with unstable lowered ic signatures
- [`KT-54895`](https://youtrack.jetbrains.com/issue/KT-54895) KJS / IR + IC: broken cross module references for function default param wrappers
- [`KT-54520`](https://youtrack.jetbrains.com/issue/KT-54520) KJS / IR Allow IdSignature clashes
- [`KT-54120`](https://youtrack.jetbrains.com/issue/KT-54120) JS IR + IC: pointless invalidation of dependent code after modifying companions
- [`KT-53986`](https://youtrack.jetbrains.com/issue/KT-53986) KJS / IR + IC: compiler produces different JS file names with IC and without IC
- [`KT-54010`](https://youtrack.jetbrains.com/issue/KT-54010) JS IR + IC: Force IC cache invalidation after updating language version or features
- [`KT-53931`](https://youtrack.jetbrains.com/issue/KT-53931) KJS / Gradle: Regression with 1.7.20-RC: ReferenceError: println is not defined
- [`KT-53968`](https://youtrack.jetbrains.com/issue/KT-53968) Kotlin/JS: no UninitializedPropertyAccessException on access to non-initialized lateinit property defined in dependencies
- [`KT-54686`](https://youtrack.jetbrains.com/issue/KT-54686) KJS / IR: Incorrect generation of signatures when one of argument is nested class
- [`KT-54479`](https://youtrack.jetbrains.com/issue/KT-54479) KJS / IR + IC: Adding or removing companion fields leads java.lang.IllegalStateException in the compiler IC infrastructure
- [`KT-54382`](https://youtrack.jetbrains.com/issue/KT-54382) KJS / IR: Wrong type check for inheritors of suspend functions
- [`KT-54323`](https://youtrack.jetbrains.com/issue/KT-54323) KJS / IR + IC: Intrinsics from stdlib may lose their dependencies in incremental rebuild
- [`KT-53361`](https://youtrack.jetbrains.com/issue/KT-53361) KJS / IR: No debug info is generated for in-line js code
- [`KT-53321`](https://youtrack.jetbrains.com/issue/KT-53321) Implement Enum.entries lowering on JS/IR
- [`KT-53112`](https://youtrack.jetbrains.com/issue/KT-53112) KJS IR turn on IC infra by default
- [`KT-50503`](https://youtrack.jetbrains.com/issue/KT-50503) Kotlin/JS: IR + IC: compileTestDevelopmentExecutableKotlinJs fails with ISE: "Could not find library" after removing module dependency
- [`KT-54011`](https://youtrack.jetbrains.com/issue/KT-54011) JS IR + IC: EnumEntries don't work well when IC is enabled
- [`KT-53672`](https://youtrack.jetbrains.com/issue/KT-53672) KJS / IR: "IndexOutOfBoundsException: Index 0 out of bounds for length 0" caused by function reference to extension function of reified type variable
- [`KT-43455`](https://youtrack.jetbrains.com/issue/KT-43455) KJS: IR. Incremental compilation problem with unbound symbols
- [`KT-53539`](https://youtrack.jetbrains.com/issue/KT-53539) KJS: Exported class inherited non-exported class shows warning
- [`KT-53443`](https://youtrack.jetbrains.com/issue/KT-53443) KJS/IR: NullPointerException caused by anonymous objects inside lambdas
- [`KT-52795`](https://youtrack.jetbrains.com/issue/KT-52795) K/JS and K/Native IR-validation/compilation errors for a valid kotlin code
- [`KT-52805`](https://youtrack.jetbrains.com/issue/KT-52805) KJS/IR: Invalid call of inline function in `also` block
- [`KT-51151`](https://youtrack.jetbrains.com/issue/KT-51151) KJS / IR: Wrong overloaded generic method with receiver is called
- [`KT-52830`](https://youtrack.jetbrains.com/issue/KT-52830) KJS/IR: Sourcemap disabling doesn't work
- [`KT-52968`](https://youtrack.jetbrains.com/issue/KT-52968) KJS / IR: Buggy generation of overridden methods
- [`KT-53063`](https://youtrack.jetbrains.com/issue/KT-53063) KJS / IR + IC: undefined cross module reference for implemented interface functions
- [`KT-51099`](https://youtrack.jetbrains.com/issue/KT-51099) KJS / IR + IC: Cache invalidation doesn't check generic class variance annotations (in, out)
- [`KT-51090`](https://youtrack.jetbrains.com/issue/KT-51090) KJS / IR + IC: Cache invalidation doesn't check suspend qualifier
- [`KT-51088`](https://youtrack.jetbrains.com/issue/KT-51088) KJS / IR + IC: Cache invalidation doesn't check class qualifiers (data, inline)
- [`KT-51083`](https://youtrack.jetbrains.com/issue/KT-51083) KJS / IR + IC: Cache invalidation doesn't check inline function which was non inline initially
- [`KT-51896`](https://youtrack.jetbrains.com/issue/KT-51896) KJS / IR + IC: Cache invalidation doesn't trigger rebuild for fake overridden inline functions

### Language Design

- [`KT-48385`](https://youtrack.jetbrains.com/issue/KT-48385) Deprecate confusing grammar in when-with-subject
- [`KT-48516`](https://youtrack.jetbrains.com/issue/KT-48516) Forbid `@Synchronized` annotation on suspend functions
- [`KT-41886`](https://youtrack.jetbrains.com/issue/KT-41886) Ability to require opt-in for interface implementation, but not for usage
- [`KT-34943`](https://youtrack.jetbrains.com/issue/KT-34943) OVERLOAD_RESOLUTION_AMBIGUITY inconsistent with the equivalent Java code
- [`KT-51334`](https://youtrack.jetbrains.com/issue/KT-51334) Implement type-bound label `this@Type`

### Libraries

#### New Features

- [`KT-21007`](https://youtrack.jetbrains.com/issue/KT-21007) Provide Kotlin OSGI Bundle with extensions for JRE8 (and JRE7)
- [`KT-54082`](https://youtrack.jetbrains.com/issue/KT-54082) Comparable and subtractible TimeMarks
- [`KT-52928`](https://youtrack.jetbrains.com/issue/KT-52928) Provide copyToRecursively and deleteRecursively extension functions for java.nio.file.Path
- [`KT-49425`](https://youtrack.jetbrains.com/issue/KT-49425) Update OptIn documentation to reflect latest design changes
- [`KT-54005`](https://youtrack.jetbrains.com/issue/KT-54005) Allow calling `declaringJavaClass` on Enum<E>
- [`KT-52933`](https://youtrack.jetbrains.com/issue/KT-52933) rangeUntil members in built-in types

#### Performance Improvements

- [`KT-53508`](https://youtrack.jetbrains.com/issue/KT-53508) Cache typeOf-related KType instances when kotlin-reflect is used

#### Fixes

- [`KT-51907`](https://youtrack.jetbrains.com/issue/KT-51907) Switch JVM target of the standard libraries to 1.8
- [`KT-54835`](https://youtrack.jetbrains.com/issue/KT-54835) Document that Iterable.all(emptyCollection) returns TRUE.
- [`KT-54168`](https://youtrack.jetbrains.com/issue/KT-54168) Expand on natural order in comparator docs
- [`KT-53277`](https://youtrack.jetbrains.com/issue/KT-53277) Stabilize experimental API for 1.8
- [`KT-53864`](https://youtrack.jetbrains.com/issue/KT-53864) Review deprecations in stdlib for 1.8
- [`KT-47707`](https://youtrack.jetbrains.com/issue/KT-47707) Remove the system property and the brittle `contains` optimization code itself
- [`KT-52336`](https://youtrack.jetbrains.com/issue/KT-52336) Different behavior on JVM and Native in stringBuilder.append(charArray, 0, 1)
- [`KT-53927`](https://youtrack.jetbrains.com/issue/KT-53927) Remove deprecation from ConcurrentModificationException constructors
- [`KT-53152`](https://youtrack.jetbrains.com/issue/KT-53152) Introduce EnumEntries<E> to stdlib as backing implementation of Enum.entries
- [`KT-53134`](https://youtrack.jetbrains.com/issue/KT-53134) stdlib > object Charsets > not thread safe lazy initialization
- [`KT-51063`](https://youtrack.jetbrains.com/issue/KT-51063) Gradle project with JPS runner: "JUnitException: Failed to parse version" JUnit runner internal error with JUnit
- [`KT-52908`](https://youtrack.jetbrains.com/issue/KT-52908) Native: setUnhandledExceptionHook swallows exceptions

### Native

- [`KT-51043`](https://youtrack.jetbrains.com/issue/KT-51043) Kotlin Native: ObjC-Interop: kotlin.ClassCastException: null cannot be cast to kotlin.Function2
- [`KT-50786`](https://youtrack.jetbrains.com/issue/KT-50786) Native: prohibit suspend calls inside autoreleasepool {}
- [`KT-52834`](https://youtrack.jetbrains.com/issue/KT-52834) Implement test infrastructure for K2/Native

### Native. C Export

- [`KT-36878`](https://youtrack.jetbrains.com/issue/KT-36878) Reverse C Interop: incorrect headers generation for primitive unassigned type arrays
- [`KT-53599`](https://youtrack.jetbrains.com/issue/KT-53599) [Reverse C Interop] Provide box/unbox API for unsigned primitive types
- [`KT-41904`](https://youtrack.jetbrains.com/issue/KT-41904) Kotlin/Native : error: duplicate member for interface and function with the same name
- [`KT-42830`](https://youtrack.jetbrains.com/issue/KT-42830) [Reverse C Interop] Add API to get value of boxed primitives
- [`KT-39496`](https://youtrack.jetbrains.com/issue/KT-39496) K/N C: optional unsigned types as function parameters crash the compiler
- [`KT-39015`](https://youtrack.jetbrains.com/issue/KT-39015) Cannot compile native library with nullable inline class

### Native. C and ObjC Import

- [`KT-54738`](https://youtrack.jetbrains.com/issue/KT-54738) Cocoapods cinterop: linking platform.CoreGraphics package
- [`KT-54001`](https://youtrack.jetbrains.com/issue/KT-54001) Kotlin/Native: support header exclusion in cinterop def files
- [`KT-53151`](https://youtrack.jetbrains.com/issue/KT-53151) Native: Custom declarations in .def don't work with modules, only headers

### Native. ObjC Export

- [`KT-53680`](https://youtrack.jetbrains.com/issue/KT-53680) Obj-C refinement annotations
- [`KT-54119`](https://youtrack.jetbrains.com/issue/KT-54119) Native: runtime assertion failed due to missing thread state switch
- [`KT-42641`](https://youtrack.jetbrains.com/issue/KT-42641) Don't export generated component* methods from Kotlin data classes to Obj-C header

### Native. Platform Libraries

- [`KT-54225`](https://youtrack.jetbrains.com/issue/KT-54225) Native: update to Xcode 14.1
- [`KT-54164`](https://youtrack.jetbrains.com/issue/KT-54164) Native: commonizer fails on CoreFoundation types
- [`KT-39747`](https://youtrack.jetbrains.com/issue/KT-39747) Why is there no WinHttp API in Kotlin/Native's Windows API?

### Native. Runtime

- [`KT-49228`](https://youtrack.jetbrains.com/issue/KT-49228) Kotlin/Native: Allow to unset unhandled exception hook
- [`KT-27305`](https://youtrack.jetbrains.com/issue/KT-27305) Fix __FILE__ macro inside `RuntimeCheck` and `RuntimeAssert`

### Native. Runtime. Memory

- [`KT-54498`](https://youtrack.jetbrains.com/issue/KT-54498) Deprecation message of 'FreezingIsDeprecated' is not really helpful
- [`KT-53182`](https://youtrack.jetbrains.com/issue/KT-53182) New memory manager: Unexpected memory usage on IOS

### Native. Stdlib

- [`KT-52429`](https://youtrack.jetbrains.com/issue/KT-52429) Small Usability Improvements for Worker API

### Reflection

- [`KT-54629`](https://youtrack.jetbrains.com/issue/KT-54629) Incorrectly cached class classifier
- [`KT-54611`](https://youtrack.jetbrains.com/issue/KT-54611) `KTypeImpl`  does not take into account class loader from the `classifier` property
- [`KT-48136`](https://youtrack.jetbrains.com/issue/KT-48136) Make `Reflection.getOrCreateKotlinPackage` use cache when `kotlin-reflect` is used
- [`KT-50705`](https://youtrack.jetbrains.com/issue/KT-50705) Use ClassValue to cache KClass objects in kotlin-reflect
- [`KT-53454`](https://youtrack.jetbrains.com/issue/KT-53454) Properly cache the same class's KClass when it's loaded by multiple classloaders in getOrCreateKotlinClass

### Specification

- [`KT-54210`](https://youtrack.jetbrains.com/issue/KT-54210) Update Kotlin specification to mention that since 1.8 generics in value classes are allowed

### Tools. CLI

- [`KT-54116`](https://youtrack.jetbrains.com/issue/KT-54116) Add JVM target bytecode version 19
- [`KT-53278`](https://youtrack.jetbrains.com/issue/KT-53278) Support values 6 and 8 for -Xjdk-release
- [`KT-46312`](https://youtrack.jetbrains.com/issue/KT-46312) CLI: Kotlin runner should use platform class loader to load JDK modules on Java 9+

### Tools. Commonizer

- [`KT-54310`](https://youtrack.jetbrains.com/issue/KT-54310) Commonizer fails on 1.8.0-dev K/N distributions
- [`KT-48576`](https://youtrack.jetbrains.com/issue/KT-48576) [Commonizer] platform.posix.pselect not commonized in Ktor

### Tools. Compiler Plugins

- [`KT-46959`](https://youtrack.jetbrains.com/issue/KT-46959) Kotlin Lombok: Support generated builders (`@Builder`)
- [`KT-53683`](https://youtrack.jetbrains.com/issue/KT-53683) Unresolved reference compilation error occurs if a file is annotated with `@` Singular and has any guava collection type :  ImmutableTable, ImmutableList or else
- [`KT-53657`](https://youtrack.jetbrains.com/issue/KT-53657) [K2] Unresolved reference compilation error occurs if a field is annotated with `@` Singular and has type NavigableMap without explicit types specification
- [`KT-53647`](https://youtrack.jetbrains.com/issue/KT-53647) [K2] Unresolved reference compilation error occurs if a field is annotated with `@` Singular and has type Iterable<>
- [`KT-53724`](https://youtrack.jetbrains.com/issue/KT-53724) Param of the `@` Singular lombok annotation ignoreNullCollections=true is ignored by kotlin compiler
- [`KT-53451`](https://youtrack.jetbrains.com/issue/KT-53451) [K2] References to methods generated by `@` With lombok annotation can't be resolved with enabled K2 compiler
- [`KT-53721`](https://youtrack.jetbrains.com/issue/KT-53721) [K2] There is no compilation error while trying to add null as a param of the field with non-null type
- [`KT-53370`](https://youtrack.jetbrains.com/issue/KT-53370) Kotlin Lombok compiler plugin can't resolve methods generated for java boolean fields annotated with `@` With annotation

### Tools. Compiler plugins. Serialization

- [`KT-54878`](https://youtrack.jetbrains.com/issue/KT-54878) JVM/IR:  java.lang.ClassCastException: class org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl cannot be cast to class org.jetbrains.kotlin.ir.types.IrTypeProjection on serializer<Box<*>>()
- [`KT-55340`](https://youtrack.jetbrains.com/issue/KT-55340) Argument for kotlinx.serialization.UseSerializers does not implement KSerializer or does not provide serializer for concrete type
- [`KT-55296`](https://youtrack.jetbrains.com/issue/KT-55296) Improve exceptions in serialization plugin
- [`KT-55180`](https://youtrack.jetbrains.com/issue/KT-55180) KJS: regression in serialization for Kotlin 1.8.0-beta
- [`KT-53157`](https://youtrack.jetbrains.com/issue/KT-53157) Recursion detected in a lazy value under LockBasedStorageManager in kotlinx.serialization
- [`KT-54297`](https://youtrack.jetbrains.com/issue/KT-54297) Regression in serializable classes with star projections
- [`KT-49660`](https://youtrack.jetbrains.com/issue/KT-49660) kotlinx.serialization: IndexOutOfBoundsException for parameterized sealed class
- [`KT-43910`](https://youtrack.jetbrains.com/issue/KT-43910) JS IR: Serialization with base class: "IndexOutOfBoundsException: Index 0 out of bounds for length 0"

### Tools. Daemon

- [`KT-52622`](https://youtrack.jetbrains.com/issue/KT-52622) Kotlin/JS, Kotlin/Common compilations start Kotlin daemon incompatible with Kotlin/JVM compilation on JDK 8

### Tools. Gradle

#### New Features

- [`KT-27301`](https://youtrack.jetbrains.com/issue/KT-27301) Expose compiler flags via Gradle lazy properties
- [`KT-53357`](https://youtrack.jetbrains.com/issue/KT-53357) Change single build metrics property
- [`KT-50673`](https://youtrack.jetbrains.com/issue/KT-50673) Gradle: KotlinCompile task(s) should use `@NormalizeLineEndings`
- [`KT-34464`](https://youtrack.jetbrains.com/issue/KT-34464) Kotlin build report path not clickable in the IDE

#### Performance Improvements

- [`KT-51525`](https://youtrack.jetbrains.com/issue/KT-51525) [Gradle] Optimize evaluating args for compile tasks
- [`KT-52520`](https://youtrack.jetbrains.com/issue/KT-52520) Remove usage of reflection from CompilerArgumentsGradleInput

#### Fixes

- [`KT-48843`](https://youtrack.jetbrains.com/issue/KT-48843) Add ability to disable Kotlin daemon fallback strategy
- [`KT-55334`](https://youtrack.jetbrains.com/issue/KT-55334) kaptGenerateStubs passes wrong android variant module names to compiler
- [`KT-55255`](https://youtrack.jetbrains.com/issue/KT-55255) Gradle: stdlib version alignment fails build on dynamic stdlib version.
- [`KT-55363`](https://youtrack.jetbrains.com/issue/KT-55363) [K1.8.0-Beta] Command line parsing treats plugin parameters as source files
- [`KT-54993`](https://youtrack.jetbrains.com/issue/KT-54993) Raise kotlin.jvm.target.validation.mode check default level to error when build is running on Gradle 8+
- [`KT-54136`](https://youtrack.jetbrains.com/issue/KT-54136) Duplicated classes cause build failure if a dependency to kotlin-stdlib specified in an android project
- [`KT-50115`](https://youtrack.jetbrains.com/issue/KT-50115) Setting toolchain via Java extension does not configure 'kotlinOptions.jvmTarget' value when Kotlin compilation tasks are created eagerly
- [`KT-55222`](https://youtrack.jetbrains.com/issue/KT-55222) Migrate AndroidDependencyResolver to the new Gradle API
- [`KT-55119`](https://youtrack.jetbrains.com/issue/KT-55119) There is no validation for different jvmTarget and targetCompatibility values in multiplatform projects with jvm target and used java sources
- [`KT-55102`](https://youtrack.jetbrains.com/issue/KT-55102) Compile java task fails with different target version in pure kotlin project
- [`KT-54995`](https://youtrack.jetbrains.com/issue/KT-54995) [1.8.0-Beta] compileAppleMainKotlinMetadata fails on default parameters with `No value passed for parameter 'mustExist'`
- [`KT-35003`](https://youtrack.jetbrains.com/issue/KT-35003) Automatically set targetCompatibility for kotlin-jvm projects to work with gradle 6 metadata
- [`KT-45335`](https://youtrack.jetbrains.com/issue/KT-45335) kotlinOptions.jvmTarget conflicts with Gradle variants
- [`KT-48798`](https://youtrack.jetbrains.com/issue/KT-48798) Android: going from one to more than one productFlavor causes inputs of commonSourceSet$kotlin_gradle_plugin property of compileKotlin task to change
- [`KT-55019`](https://youtrack.jetbrains.com/issue/KT-55019) Gradle sync: UnknownConfigurationException when adding implementation dependencies to a Kotlin with Java compilation
- [`KT-55004`](https://youtrack.jetbrains.com/issue/KT-55004) jvmTarget value is ignored by depending modules if a task "UsesKotlinJavaToolchain" is configured for all project modules using allProjects {}
- [`KT-54888`](https://youtrack.jetbrains.com/issue/KT-54888) Add Gradle property to suppress kotlinOptions.freeCompilerArgs modification on execution phase
- [`KT-54399`](https://youtrack.jetbrains.com/issue/KT-54399) Undeprecate 'kotlinOptions' DSL
- [`KT-54306`](https://youtrack.jetbrains.com/issue/KT-54306) Change the naming of newly added Compiler*Options classes and interfaces
- [`KT-54580`](https://youtrack.jetbrains.com/issue/KT-54580) KotlinOptions in AbstractKotlinCompilation class are deprecated
- [`KT-54653`](https://youtrack.jetbrains.com/issue/KT-54653) java.lang.NoClassDefFoundError: kotlin/jdk7/AutoCloseableKt exception if a dependency to the kotlin-stdlib is added
- [`KT-52624`](https://youtrack.jetbrains.com/issue/KT-52624) Compatibility with Gradle 7.3 release
- [`KT-54703`](https://youtrack.jetbrains.com/issue/KT-54703) Stdlib substitution does not work with JPMS modules
- [`KT-54602`](https://youtrack.jetbrains.com/issue/KT-54602) Prevent leaking Gradle Compile DSL types into compiler cli runtime
- [`KT-54439`](https://youtrack.jetbrains.com/issue/KT-54439) Project failed to sync Native LaguageSettings to compiler options in afterEvaluate
- [`KT-53885`](https://youtrack.jetbrains.com/issue/KT-53885) Bump minimal supported Gradle version to 6.8.3
- [`KT-53773`](https://youtrack.jetbrains.com/issue/KT-53773) Protect and system properties can contain sensitive data
- [`KT-53732`](https://youtrack.jetbrains.com/issue/KT-53732) Add custom values limits for build scan reports
- [`KT-52623`](https://youtrack.jetbrains.com/issue/KT-52623) Compatibility with Gradle 7.2. release
- [`KT-51831`](https://youtrack.jetbrains.com/issue/KT-51831) Gradle: remove `kotlin.compiler.execution.strategy` system property
- [`KT-51679`](https://youtrack.jetbrains.com/issue/KT-51679) Change deprecation level to error for KotlinCompile setClasspath/getClasspath methods
- [`KT-54335`](https://youtrack.jetbrains.com/issue/KT-54335) Kotlin build report configuration. There is no validation for SINGLE_FILE output if the required kotlin.build.report.single_file property is empty or absent
- [`KT-54356`](https://youtrack.jetbrains.com/issue/KT-54356) Kotlin build report configuration. Wrong path is used for the property kotlin.internal.single.build.metrics.file
- [`KT-53617`](https://youtrack.jetbrains.com/issue/KT-53617) KotlinCompilerExecutionStrategy value  is ignored by depending modules if configure once for all project modules using allProjects {}
- [`KT-53823`](https://youtrack.jetbrains.com/issue/KT-53823) Kotlin Gradle Plugin uses deprecated Gradle API: Provider.forUseAtConfigurationTime()
- [`KT-54142`](https://youtrack.jetbrains.com/issue/KT-54142) Increase Kotlin Gradle plugin Gradle target API to 7.5
- [`KT-50161`](https://youtrack.jetbrains.com/issue/KT-50161) Android variant filter breaks KotlinCompile cache compatibility
- [`KT-54113`](https://youtrack.jetbrains.com/issue/KT-54113) LanguageSettings to KotlinNativeLink.toolOptions sync are executed on the wrong context
- [`KT-53830`](https://youtrack.jetbrains.com/issue/KT-53830) Versions of kotlin-stdlib-jdk8 and  kotlin-stdlib-jdk7 aren't overrided if added as transitive dependencies to kotlin-stdlib
- [`KT-54112`](https://youtrack.jetbrains.com/issue/KT-54112) Missing target input on KotlinNativeLink task
- [`KT-45879`](https://youtrack.jetbrains.com/issue/KT-45879) Documentation: Wrong kotlin languageVersion "1.6 (EXPERIMENTAL)"
- [`KT-54103`](https://youtrack.jetbrains.com/issue/KT-54103) Remove JvmTarget.JVM_1_6 from generated Gradle compiler type
- [`KT-52959`](https://youtrack.jetbrains.com/issue/KT-52959) KMP code is breaking Gradle project isolation
- [`KT-50598`](https://youtrack.jetbrains.com/issue/KT-50598) MULTIPLE_KOTLIN_PLUGINS_SPECIFIC_PROJECTS_WARNING is only shown on first build
- [`KT-53246`](https://youtrack.jetbrains.com/issue/KT-53246) Gradle: Special characters in paths of errors and warnings should be escaped
- [`KT-47730`](https://youtrack.jetbrains.com/issue/KT-47730) How to avoid stdlib coming from Kotlin gradle plugin
- [`KT-52209`](https://youtrack.jetbrains.com/issue/KT-52209) Corrupted cache and non-incremental build if produce caches "in process" and restore then compiling with kotlin daemon
- [`KT-41642`](https://youtrack.jetbrains.com/issue/KT-41642) "TaskDependencyResolveException: Could not determine the dependencies" when trying to apply stdlib
- [`KT-53390`](https://youtrack.jetbrains.com/issue/KT-53390) Drop usage of -Xjava-source-roots when passing java sources required for Kotlin compilation
- [`KT-52984`](https://youtrack.jetbrains.com/issue/KT-52984) Kotlin Gradle plugin is misbehaving by resolving DomainObjectCollection early
- [`KT-38622`](https://youtrack.jetbrains.com/issue/KT-38622) Non-incremental compilation because of R.jar with Android Gradle plugin 3.6
- [`KT-38576`](https://youtrack.jetbrains.com/issue/KT-38576) AnalysisResult.RetryWithAdditionalRoots crashes during incremental compilation with java classes in classpath

### Tools. Gradle. Cocoapods

- [`KT-54314`](https://youtrack.jetbrains.com/issue/KT-54314) Cocoapods: Signing pod dependency for Xcode 14
- [`KT-54060`](https://youtrack.jetbrains.com/issue/KT-54060) Xcode 14: disable bitcode embedding for Apple frameworks
- [`KT-53340`](https://youtrack.jetbrains.com/issue/KT-53340) Change default linking type for frameworks registered by cocoapods plugin
- [`KT-53392`](https://youtrack.jetbrains.com/issue/KT-53392) Deprecate and delete downloading pod dependencies by direct link
- [`KT-53695`](https://youtrack.jetbrains.com/issue/KT-53695) Build of macOS application fails if a framework is integrated via Cocoapods plugin

### Tools. Gradle. JS

- [`KT-53367`](https://youtrack.jetbrains.com/issue/KT-53367) KJS: Migrate cssSupport API
- [`KT-45789`](https://youtrack.jetbrains.com/issue/KT-45789) KJS / IR: Transitive NPM dependencies are not included in PublicPackageJsonTask output
- [`KT-55099`](https://youtrack.jetbrains.com/issue/KT-55099) K/JS: Second declaration of JS target without compiler type report warning incorrectly
- [`KT-52951`](https://youtrack.jetbrains.com/issue/KT-52951) [KGP/JS] Browser test target registration via properties
- [`KT-52950`](https://youtrack.jetbrains.com/issue/KT-52950) KJS: Report if yarn.lock was updated during built
- [`KT-53374`](https://youtrack.jetbrains.com/issue/KT-53374) KJS / Gradle: Implement IDEA sync detection logic via ValueSource to improve configuration cache support
- [`KT-53381`](https://youtrack.jetbrains.com/issue/KT-53381) Kotlin/JS: with erased kotlin-js-store/ and reportNewYarnLock = true the task kotlinUpgradeYarnLock always fails
- [`KT-53788`](https://youtrack.jetbrains.com/issue/KT-53788) KJS / Gradle: Disable Gradle build cache for KotlinJsDce when development mode is enabled
- [`KT-53614`](https://youtrack.jetbrains.com/issue/KT-53614) Kotlin/JS upgrade npm dependencies

### Tools. Gradle. Multiplatform

#### New Features

- [`KT-53396`](https://youtrack.jetbrains.com/issue/KT-53396) Support 'implementation platform()' by gradle kotlin mpp plugin for JVM target
- [`KT-40489`](https://youtrack.jetbrains.com/issue/KT-40489) MPP / Gradle: support BOM (enforcedPlatform) artifacts in source set dependencies DSL

#### Performance Improvements

- [`KT-52726`](https://youtrack.jetbrains.com/issue/KT-52726) [MPP] Optimize caching/performance/call-sites of 'compilationsBySourceSets'

#### Fixes

- [`KT-54634`](https://youtrack.jetbrains.com/issue/KT-54634) MPP: Test Failure causes: `KotlinJvmTest$Executor$execute$1 does not define failure`
- [`KT-35916`](https://youtrack.jetbrains.com/issue/KT-35916) Gradle MPP plugin: Configurations for a main compilation and its default source set have different naming
- [`KT-46960`](https://youtrack.jetbrains.com/issue/KT-46960) Repeated kotlin/native external libraries in project
- [`KT-27292`](https://youtrack.jetbrains.com/issue/KT-27292) MPP: jvm { withJava() }: Gradle build: Java source under Kotlin root is resolved while building, but does not produce output class files
- [`KT-34650`](https://youtrack.jetbrains.com/issue/KT-34650) Naming clash in MPP+Android: androidTest vs androidAndroidTest
- [`KT-54387`](https://youtrack.jetbrains.com/issue/KT-54387) Remove MPP alpha stability warning
- [`KT-31468`](https://youtrack.jetbrains.com/issue/KT-31468) Targets disambiguation doesn't work if a depending multiplatform module uses `withJava()` mode
- [`KT-54090`](https://youtrack.jetbrains.com/issue/KT-54090) Take an Apple test device from the device list
- [`KT-54301`](https://youtrack.jetbrains.com/issue/KT-54301) KotlinToolingVersionOrNull: IllegalArgumentException
- [`KT-53256`](https://youtrack.jetbrains.com/issue/KT-53256) Implement K/N compiler downloading for KPM
- [`KT-45412`](https://youtrack.jetbrains.com/issue/KT-45412) KotlinCompilation: Make sure .kotlinSourceSets and .allKotlinSourceSets include the default source set
- [`KT-49202`](https://youtrack.jetbrains.com/issue/KT-49202) Tests on android target can't be executed in multiplatform project if dependency to kotlin-test framework is provided as a single dependency and tests configured to be executed via Junit5

### Tools. Gradle. Native

#### New Features

- [`KT-43293`](https://youtrack.jetbrains.com/issue/KT-43293) Support Gradle configuration caching with Kotlin/Native
- [`KT-53107`](https://youtrack.jetbrains.com/issue/KT-53107) Add arm64 support for watchOS targets (Xcode 14)

#### Fixes

- [`KT-53704`](https://youtrack.jetbrains.com/issue/KT-53704) Native cinterop: eager header path calculation
- [`KT-54814`](https://youtrack.jetbrains.com/issue/KT-54814) Kotlin/Native: Github Actions: Testing watchOSX64 with Xcode 14 — Invalid device: Apple Watch Series 5
- [`KT-54627`](https://youtrack.jetbrains.com/issue/KT-54627) Native: :commonizeNativeDistribution with configuration cache enabled fails even when set to warn on JDK 17
- [`KT-54339`](https://youtrack.jetbrains.com/issue/KT-54339) Link tasks fail if Gradle Configuration Cache is enabled
- [`KT-53191`](https://youtrack.jetbrains.com/issue/KT-53191) Native cinterop sync problem with gradle
- [`KT-54583`](https://youtrack.jetbrains.com/issue/KT-54583) watchosDeviceArm64 target shouldn't register test tasks
- [`KT-52303`](https://youtrack.jetbrains.com/issue/KT-52303) Gradle / Native: Build tasks ignore project.buildDir
- [`KT-54442`](https://youtrack.jetbrains.com/issue/KT-54442) Gradle iOS test tasks fail if a device is not selected explicitly
- [`KT-54177`](https://youtrack.jetbrains.com/issue/KT-54177) Gradle: Deprecate `enableEndorsedLibs` flag
- [`KT-47355`](https://youtrack.jetbrains.com/issue/KT-47355) Support macos target for FatFramework task
- [`KT-53339`](https://youtrack.jetbrains.com/issue/KT-53339) MPP / CocoaPods: The static framework fails to install on a real iOS device
- [`KT-31573`](https://youtrack.jetbrains.com/issue/KT-31573) Missing description for Native Gradle tasks
- [`KT-53131`](https://youtrack.jetbrains.com/issue/KT-53131) Gradle Sync: "NoSuchElementException: Array contains no element matching the predicate" with CocoaPods
- [`KT-53686`](https://youtrack.jetbrains.com/issue/KT-53686) Task assembleReleaseXCFramework fails with "error: the path does not point to a valid framework" if project name contains a dash

### Tools. Incremental Compile

- [`KT-54144`](https://youtrack.jetbrains.com/issue/KT-54144) New IC: "IllegalStateException: The following LookupSymbols are not yet converted to ProgramSymbols" when changing an inline function with custom JvmName
- [`KT-53871`](https://youtrack.jetbrains.com/issue/KT-53871) New IC: "IllegalStateException: The following LookupSymbols are not yet converted to ProgramSymbols" when changing an inline property accessor
- [`KT-19804`](https://youtrack.jetbrains.com/issue/KT-19804) Relocatable IC caches

### Tools. JPS

- [`KT-45474`](https://youtrack.jetbrains.com/issue/KT-45474) False positive NO_ELSE_IN_WHEN on sealed class with incremental compilation
- [`KT-54228`](https://youtrack.jetbrains.com/issue/KT-54228) Switching abstract to sealed classes causes incremental issue
- [`KT-38483`](https://youtrack.jetbrains.com/issue/KT-38483) JPS: Stopping compilation causes IDE CompilationCanceledException
- [`KT-50310`](https://youtrack.jetbrains.com/issue/KT-50310) False positive NO_ELSE_IN_WHEN on incremental build when adding sealed classes
- [`KT-48813`](https://youtrack.jetbrains.com/issue/KT-48813) Move cache version to compiler
- [`KTIJ-921`](https://youtrack.jetbrains.com/issue/KTIJ-921) JPS: FileNotFoundException on project build in mixed Kotlin/Scala project

### Tools. Kapt

- [`KT-54187`](https://youtrack.jetbrains.com/issue/KT-54187) JVM IR + kapt: incorrect modifier `final` is generated for nested enum in interface
- [`KT-48827`](https://youtrack.jetbrains.com/issue/KT-48827) Remove 'kapt.use.worker.api' property

### Tools. Scripts

- [`KT-54355`](https://youtrack.jetbrains.com/issue/KT-54355) Scripts: Internal compiler error (languageVersion=1.9)
- [`KT-53009`](https://youtrack.jetbrains.com/issue/KT-53009) Scripting: NDFDE “Descriptor wasn't found for declaration SCRIPT” on using script definition with kotlin from master


## Recent ChangeLogs:
### [ChangeLog-1.7.X](docs/changelogs/ChangeLog-1.7.X.md)
### [ChangeLog-1.6.X](docs/changelogs/ChangeLog-1.6.X.md)
### [ChangeLog-1.5.X](docs/changelogs/ChangeLog-1.5.X.md)
### [ChangeLog-1.4.X](docs/changelogs/ChangeLog-1.4.X.md)
### [ChangeLog-1.3.X](docs/changelogs/ChangeLog-1.3.X.md)
### [ChangeLog-1.2.X](docs/changelogs/ChangeLog-1.2.X.md)
### [ChangeLog-1.1.X](docs/changelogs/ChangeLog-1.1.X.md)
### [ChangeLog-1.0.X](docs/changelogs/ChangeLog-1.0.X.md)