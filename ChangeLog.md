## 2.4.0-Beta2

### Analysis API

- [`KT-65683`](https://youtrack.jetbrains.com/issue/KT-65683) Analysis API: Dangling file session creation causes a `computeIfAbsent` contract violation

### Analysis API. FIR

- [`KT-70896`](https://youtrack.jetbrains.com/issue/KT-70896) AA: False positive deprecation warning with override of built-in method in JDK mapped class
- [`KT-84625`](https://youtrack.jetbrains.com/issue/KT-84625) Analysis API: collectDesignationPath fails for nested classes inside plugin-generated top-level classes

### Analysis API. Infrastructure

- [`KT-84913`](https://youtrack.jetbrains.com/issue/KT-84913) Extract compiler classes used by the PSI & Analysis API to a separate module
- [`KT-64986`](https://youtrack.jetbrains.com/issue/KT-64986) Analysis API: Implement Analysis API tests for different KMP Platforms
- [`KT-80379`](https://youtrack.jetbrains.com/issue/KT-80379) Extract per-module test generators for AA tests

### Analysis API. PSI

- [`KT-84715`](https://youtrack.jetbrains.com/issue/KT-84715) removeModifier doesn't delete whitespaces around the removed modifier
- [`KT-84564`](https://youtrack.jetbrains.com/issue/KT-84564) KtEnumEntry.delete deletes semicolon
- [`KT-84781`](https://youtrack.jetbrains.com/issue/KT-84781) Use computed properties in KotlinElementTypeProviderImpl

### Analysis API. Stubs and Decompilation

- [`KT-85371`](https://youtrack.jetbrains.com/issue/KT-85371) StackOverflowError from LLKotlinStubBasedLibrarySymbolProvider and StubBasedClassDeserialization
- [`KT-83935`](https://youtrack.jetbrains.com/issue/KT-83935) Support KDoc loading in decompiled stubs

### Analysis API. Surface

- [`KT-82519`](https://youtrack.jetbrains.com/issue/KT-82519) Automatically recognize the appropriate analysis mode for in-memory file copies based on their content
- [`KT-85239`](https://youtrack.jetbrains.com/issue/KT-85239) Streaming version of collectDiagnostics()
- [`KT-83921`](https://youtrack.jetbrains.com/issue/KT-83921) Extend KaKDocProvider to read Kdoc from KLIB metadata
- [`KT-77426`](https://youtrack.jetbrains.com/issue/KT-77426) KaFirCompilerFacility uses an arbitrary JVM counterpart for common sources
- [`KT-84737`](https://youtrack.jetbrains.com/issue/KT-84737) KaCallableSymbol#directlyOverriddenSymbols doesn't work for java overrides of kotlin properties
- [`KT-84621`](https://youtrack.jetbrains.com/issue/KT-84621) Migrate symbol tests to ManagedTest properly
- [`KT-80575`](https://youtrack.jetbrains.com/issue/KT-80575) KaFirJavaInteroperabilityComponent#getJavaGetterName should not throw exception on incomplete code

### Backend. Wasm

- [`KT-76205`](https://youtrack.jetbrains.com/issue/KT-76205) K/Wasm: stabilize and turn on incremental compilation by default
- [`KT-83728`](https://youtrack.jetbrains.com/issue/KT-83728) [Wasm] Invalid Ir type while suspend call with blocked if null comprehansion
- [`KT-81637`](https://youtrack.jetbrains.com/issue/KT-81637) K/JS/Wasm interop: Inconsistent behavior of `is`/`as` operations for `JsReference<C>` and `C`

### Compiler

#### New Features

- [`KT-84484`](https://youtrack.jetbrains.com/issue/KT-84484) Companion Extensions Analysis & Resolution
- [`KT-84298`](https://youtrack.jetbrains.com/issue/KT-84298) K2: Generate IR for Companion Blocks & Extensions
- [`KT-84292`](https://youtrack.jetbrains.com/issue/KT-84292) Enforce Companion Blocks & Extensions Language Feature during Resolution
- [`KT-84291`](https://youtrack.jetbrains.com/issue/KT-84291) Companion Blocks & Extensions Checkers
- [`KT-84290`](https://youtrack.jetbrains.com/issue/KT-84290) Callable References to Companion Block Declarations & Extensions
- [`KT-84287`](https://youtrack.jetbrains.com/issue/KT-84287) Build Raw FIR for Companion Blocks & Extensions
- [`KT-73256`](https://youtrack.jetbrains.com/issue/KT-73256) Implement `all` meta-target for annotations
- [`KT-84319`](https://youtrack.jetbrains.com/issue/KT-84319) Add JVM target bytecode version 26
- [`KT-84297`](https://youtrack.jetbrains.com/issue/KT-84297) Serialize & Deserialize Companion Block Declarations & Extensions to/from Metadata
- [`KT-84199`](https://youtrack.jetbrains.com/issue/KT-84199) Implement DontMakeExplicitNullableJavaTypeArgumentsFlexible feature

#### Fixes

- [`KT-80489`](https://youtrack.jetbrains.com/issue/KT-80489) Collection literals: experimental version (Frontend)
- [`KT-84566`](https://youtrack.jetbrains.com/issue/KT-84566) Prevent launching Default dispatcher threads from IJ SDK in kotlin compiler
- [`KT-85358`](https://youtrack.jetbrains.com/issue/KT-85358) Native: roll back the workaround for KT-84678 once MapLibre has been properly fixed
- [`KT-84931`](https://youtrack.jetbrains.com/issue/KT-84931) Incorrect type nullability in SAM super type in anonymous class-based SAM conversion
- [`KT-83920`](https://youtrack.jetbrains.com/issue/KT-83920) False positive "modifier 'value' is not applicable to 'local variable'" with soft keyword in positional destructuring (square bracket) declaration
- [`KT-85626`](https://youtrack.jetbrains.com/issue/KT-85626)  `@JvmRecord` in commonMain breaks compileCommonMainKotlinMetadata with "Cannot access 'java.lang.Record'"
- [`KT-52673`](https://youtrack.jetbrains.com/issue/KT-52673) Don't report deprecation warning/error on imports
- [`KT-84991`](https://youtrack.jetbrains.com/issue/KT-84991) Improve `Argument type mismatch` diagnostics
- [`KT-82216`](https://youtrack.jetbrains.com/issue/KT-82216) Sanitize '.kotlin_module' filename
- [`KT-84678`](https://youtrack.jetbrains.com/issue/KT-84678) K/N: Undefined symbol from SPM-added ObjC frameworks when linking iOS target
- [`KT-77726`](https://youtrack.jetbrains.com/issue/KT-77726) Move FirUnusedExpressionChecker to the default checkers list
- [`KT-85354`](https://youtrack.jetbrains.com/issue/KT-85354) checkPsiTypeConsistency: add psi text attachments
- [`KT-85479`](https://youtrack.jetbrains.com/issue/KT-85479) Improve diagnostic messages for upper bound violations
- [`KT-84585`](https://youtrack.jetbrains.com/issue/KT-84585) Upper bound violated warning for expansion of type alias in LHS
- [`KT-84924`](https://youtrack.jetbrains.com/issue/KT-84924) Native: stdlib-cache.lock used by mulitple processes
- [`KT-85244`](https://youtrack.jetbrains.com/issue/KT-85244) False positive DUPLICATE_BRANCH_CONDITION_IN_WHEN with guard condition
- [`KT-78432`](https://youtrack.jetbrains.com/issue/KT-78432) No-arg constructor should be generated for regular classes with a value class parameter in case of JvmExposeBoxed
- [`KT-85487`](https://youtrack.jetbrains.com/issue/KT-85487) Investigate why WrapContinuationForTailCallFunctions does not work in Android Test
- [`KT-59633`](https://youtrack.jetbrains.com/issue/KT-59633) K2: Implement running AndroidRunner tests with FIR
- [`KT-85392`](https://youtrack.jetbrains.com/issue/KT-85392) Native: concurrency issues in per-file caches
- [`KT-76237`](https://youtrack.jetbrains.com/issue/KT-76237) Store File-level annotations in KLIB metadata separately
- [`KT-85162`](https://youtrack.jetbrains.com/issue/KT-85162) Introduce diagnostics to refine numeric types casting
- [`KT-80060`](https://youtrack.jetbrains.com/issue/KT-80060) False positive REDUNDANT_CALL_OF_CONVERSION_METHOD in case of overloads
- [`KT-85289`](https://youtrack.jetbrains.com/issue/KT-85289) False-positive smartcast from == with type parameter based variable
- [`KT-83890`](https://youtrack.jetbrains.com/issue/KT-83890) return-value-checker: false positive "Unused return value of 'context'" on kotlin.context() functions
- [`KT-84106`](https://youtrack.jetbrains.com/issue/KT-84106) False negative "NON_EXHAUSTIVE_WHEN": "NoWhenBranchMatchedException" at runtime with sealed and platform type
- [`KT-85005`](https://youtrack.jetbrains.com/issue/KT-85005) Consider `all:` target in the checker of repeatable annotations
- [`KT-85210`](https://youtrack.jetbrains.com/issue/KT-85210) Enabling -XXLanguage:+IntrinsicConstEvaluation breaks highlighting on some broken code
- [`KT-85217`](https://youtrack.jetbrains.com/issue/KT-85217) Rework implementation supporting simple-to-suspend function conversion
- [`KT-85036`](https://youtrack.jetbrains.com/issue/KT-85036) Introduce a proper handling of optional expectation annotations in platform checkers during metadata compilation
- [`KT-85086`](https://youtrack.jetbrains.com/issue/KT-85086) False-negative JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME
- [`KT-84082`](https://youtrack.jetbrains.com/issue/KT-84082) [OPT_IN_USAGE_ERROR] duplicates for destructuring declaration
- [`KT-84732`](https://youtrack.jetbrains.com/issue/KT-84732) Collection literals: "Expected `FirCollectionLiteralImpl` to be resolved" in RHS of equality operator
- [`KT-84841`](https://youtrack.jetbrains.com/issue/KT-84841) Collection literals: Drop special treatment of `when` with expected type
- [`KT-85007`](https://youtrack.jetbrains.com/issue/KT-85007) Properly implement special rules for `kotlin.Result` in `@JvmExposeBoxed` support
- [`KT-74383`](https://youtrack.jetbrains.com/issue/KT-74383) Support new callable reference nodes in JVM backend
- [`KT-84828`](https://youtrack.jetbrains.com/issue/KT-84828) Cleanup JVM backend from the old callable references-related code
- [`KT-85006`](https://youtrack.jetbrains.com/issue/KT-85006) Refine error messages for `INAPPLICABLE_ALL_TARGET` diagnostic
- [`KT-84296`](https://youtrack.jetbrains.com/issue/KT-84296) Support Companion Blocks in CFG
- [`KT-85058`](https://youtrack.jetbrains.com/issue/KT-85058) Remove final field modification in DescriptorRendererOptionsImpl to prevent warnings on JDK 26+
- [`KT-85021`](https://youtrack.jetbrains.com/issue/KT-85021) False positive SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC error in multi-module project
- [`KT-84727`](https://youtrack.jetbrains.com/issue/KT-84727) [K/N] Segfault when returning null as generic Int type from dynamic framework
- [`KT-85062`](https://youtrack.jetbrains.com/issue/KT-85062) Deprecate language version 2.1
- [`KT-83460`](https://youtrack.jetbrains.com/issue/KT-83460) Deprecation from `@all`:Deprecated is not propagated to property accessors/backing fields
- [`KT-84859`](https://youtrack.jetbrains.com/issue/KT-84859) Skip deprecation phase for generic arguments in qualifier receiver of static call for companion block members and extensions
- [`KT-85050`](https://youtrack.jetbrains.com/issue/KT-85050) [Swift Export] usage of inline classes with ref types crashes at runtime
- [`KT-84983`](https://youtrack.jetbrains.com/issue/KT-84983) Type parameter annotations are lost for local functions
- [`KT-78800`](https://youtrack.jetbrains.com/issue/KT-78800) Investigate FirMissingDependencySupertypeInQualifiedAccessExpressionsChecker
- [`KT-73945`](https://youtrack.jetbrains.com/issue/KT-73945) K2 IDE: Duplicated inspections for redundant 'open' in interface member
- [`KT-84294`](https://youtrack.jetbrains.com/issue/KT-84294) Ensure Context Sensitive Resolution works with Companion Blocks & Extensions
- [`KT-81675`](https://youtrack.jetbrains.com/issue/KT-81675) Improve message for CONTEXTUAL_OVERLOAD_SHADOWED
- [`KT-84994`](https://youtrack.jetbrains.com/issue/KT-84994) Rework optimization for companion extension resolution
- [`KT-81598`](https://youtrack.jetbrains.com/issue/KT-81598) incorrect type mismatch error messages for generic calls with explicit type arguments
- [`KT-83587`](https://youtrack.jetbrains.com/issue/KT-83587) K2: Missing null-check when using == on Short! and Byte! platform types
- [`KT-261`](https://youtrack.jetbrains.com/issue/KT-261) Can't specify function return type in a subclass

### Compose compiler

#### New features
- [`c1bbb47`](https://github.com/JetBrains/kotlin/commit/c1bbb479ed4d37b19407917bb7c3bad14f99406c) Started inferring the stability of all interfaces to be
      `Stability.Unknown`, expect for those explicitly marked as known
      stable.
      
#### Fixes
- [`b/504284805`](https://issuetracker.google.com/issues/504284805) Fix indentation for generated proguard mappings
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

- [`KT-85605`](https://youtrack.jetbrains.com/issue/KT-85605) "Local delegated property has not delegate" exception when calling inline function containing delegated property in a lambda from within an inline lambda

### IR. Interpreter

- [`KT-80804`](https://youtrack.jetbrains.com/issue/KT-80804) Enable constant evaluation for more standard library
- [`KT-83514`](https://youtrack.jetbrains.com/issue/KT-83514) Get rid of `EvaluatedConstTracker`

### IR. Tree

- [`KT-79663`](https://youtrack.jetbrains.com/issue/KT-79663) KLIB-based compilers: Promote partial linkage to "always on"
- [`KT-76934`](https://youtrack.jetbrains.com/issue/KT-76934) Drop old IR parameter API
- [`KT-74763`](https://youtrack.jetbrains.com/issue/KT-74763) Build: refactor ':compiler:backend.common' and ':compiler:ir.backend.common' modules

### JVM. Reflection

- [`KT-85550`](https://youtrack.jetbrains.com/issue/KT-85550) Reflection: KParameter.type.classifier returns boxed KClass for non-nullable primitive types
- [`KT-85285`](https://youtrack.jetbrains.com/issue/KT-85285) Reflection: InvocationTargetException (UInt cannot be cast to Integer) when reading UInt annotation property via getter
- [`KT-85322`](https://youtrack.jetbrains.com/issue/KT-85322) Reflection: KotlinReflectionInternalError when loading ProGuard-obfuscated code compiled before 2.3.20
- [`KT-84679`](https://youtrack.jetbrains.com/issue/KT-84679) Reflection: confusing "Kotlin reflection is not yet supported for synthetic Java properties" for reference to Java enum's entries property
- [`KT-85025`](https://youtrack.jetbrains.com/issue/KT-85025) `KTypeParameter` instances not equal to each other for the same type parameter in member specialization `KFunction`
- [`KT-85091`](https://youtrack.jetbrains.com/issue/KT-85091) Reflection: "KotlinReflectionInternalError: Unsupported parameter owner: null" on attempt to get annotations of annotation constructor parameter
- [`KT-84382`](https://youtrack.jetbrains.com/issue/KT-84382) Reflection: raw list in Java type is transformed to List instead of MutableList

### JavaScript

#### Fixes

- [`KT-82395`](https://youtrack.jetbrains.com/issue/KT-82395) Support top-level declarations from compiler plugins in JS incremental compilation
- [`KT-81787`](https://youtrack.jetbrains.com/issue/KT-81787) KJS: Value class type lost when using JsExport on interface
- [`KT-85411`](https://youtrack.jetbrains.com/issue/KT-85411) Fix conversionCombinations.kt tests for the JS target
- [`KT-72198`](https://youtrack.jetbrains.com/issue/KT-72198) KJS: ES2015 interop with ValueClass
- [`KT-15101`](https://youtrack.jetbrains.com/issue/KT-15101) js: Same callable references are not equal
- [`KT-84810`](https://youtrack.jetbrains.com/issue/KT-84810) [K/JS] Callable references operator produces duplicates
- [`KT-85323`](https://youtrack.jetbrains.com/issue/KT-85323) JsClass optimization doesn't work well for primitives
- [`KT-60651`](https://youtrack.jetbrains.com/issue/KT-60651) KJS / ES6: init block and constructor are not called
- [`KT-84601`](https://youtrack.jetbrains.com/issue/KT-84601) K/JS: `KClass<>` reference doesn't work in JS counterside as a `new` target in ES6 mode
- [`KT-85099`](https://youtrack.jetbrains.com/issue/KT-85099) KotlinJS: JsPlainObject from the js-plain-objects plugin does not respect overrides
- [`KT-84615`](https://youtrack.jetbrains.com/issue/KT-84615) KJS: Forbid `@JsStatic` on extension functions/properties
- [`KT-84633`](https://youtrack.jetbrains.com/issue/KT-84633) Kotlin/JS: "Serializer for class not found" error when IR output granularity is `whole-program`
- [`KT-85038`](https://youtrack.jetbrains.com/issue/KT-85038) Kotlin/JS: `@JsExport` on sealed external interface with companion object causes NPE
- [`KT-85047`](https://youtrack.jetbrains.com/issue/KT-85047) Kotlin/JS: `@JsStatic` on suspend fun of class companion generates incorrect d.ts
- [`KT-84517`](https://youtrack.jetbrains.com/issue/KT-84517) K/JS: bad mappings data in outputted Kotlin stdlib source map

### Klibs

- [`KT-84415`](https://youtrack.jetbrains.com/issue/KT-84415) Ineffective hashMap usage in IrSymbolDeserializer
- [`KT-84511`](https://youtrack.jetbrains.com/issue/KT-84511) [Native][Tests] Improve descriptor-related logic in NativeCliBasedFacades.kt
- [`KT-85017`](https://youtrack.jetbrains.com/issue/KT-85017) [PL] Add test for added `internal abstract fun`
- [`KT-84488`](https://youtrack.jetbrains.com/issue/KT-84488) Export in previous version: Prohibit using on 2nd stage
- [`KT-85149`](https://youtrack.jetbrains.com/issue/KT-85149) Klib Dump parser: fix parsing of qualified names adjacent to vararg symbol
- [`KT-85129`](https://youtrack.jetbrains.com/issue/KT-85129) Klib Dump parser: fix enum names parsing
- [`KT-84684`](https://youtrack.jetbrains.com/issue/KT-84684) Remove `UserVisibleIrModulesSupport` from IR linker
- [`KT-84820`](https://youtrack.jetbrains.com/issue/KT-84820) [K/N] Load `libcallbacks` and `libllvmstubs` from configured path

### Language Design

- [`KT-73821`](https://youtrack.jetbrains.com/issue/KT-73821) Decide the future of the ForbidUsingSupertypesWithInaccessibleContentInTypeArguments language feature
- [`KT-80852`](https://youtrack.jetbrains.com/issue/KT-80852) Version overloading: generate overloads corresponding to different versions of a function whose parameters are annotated with `@IntroducedAt`(<version>)
- [`KT-85120`](https://youtrack.jetbrains.com/issue/KT-85120) `@IntroducedAt` on expect parameter cannot be properly actualized

### Libraries

- [`KT-85122`](https://youtrack.jetbrains.com/issue/KT-85122) Deprecate kotlin.io.readLine with WARNING
- [`KT-84970`](https://youtrack.jetbrains.com/issue/KT-84970) Deprecate AbstractCoroutineContextKey and associated API
- [`KT-84818`](https://youtrack.jetbrains.com/issue/KT-84818) [Regex] Native and Wasm: Decomposed Unicode character are incorrectly process with CANON_EQ flag
- [`KT-80772`](https://youtrack.jetbrains.com/issue/KT-80772) K/N: Regex: improve look behind matching performance for "fixed-length" patterns
- [`KT-81395`](https://youtrack.jetbrains.com/issue/KT-81395) Stabilize kotlin.uuid.Uuid API
- [`KT-85127`](https://youtrack.jetbrains.com/issue/KT-85127) Remove kotlin.test.assert*NoInline hidden functions
- [`KT-84264`](https://youtrack.jetbrains.com/issue/KT-84264) Add appropiate `@SinceKotlin` to new contracts
- [`KT-84921`](https://youtrack.jetbrains.com/issue/KT-84921) Add 'returnsResultOf' contract to appropriate declarations in the stdlib
- [`KT-84697`](https://youtrack.jetbrains.com/issue/KT-84697) Update the list of JDKs the stdlib is tested with

### Native

- [`KT-83914`](https://youtrack.jetbrains.com/issue/KT-83914) Native: when loading JNI libraries, java.library.path can contain system directories with libraries with same names
- [`KT-84826`](https://youtrack.jetbrains.com/issue/KT-84826) Bump the minimum deployment version of Apple targets
- [`KT-83133`](https://youtrack.jetbrains.com/issue/KT-83133) Native: don't use `sun.misc.Unsafe` in the compiler and cinterop when running on JDK 25+
- [`KT-84686`](https://youtrack.jetbrains.com/issue/KT-84686) Removing x64 in gradle file breaks builds on certain platforms
- [`KT-83648`](https://youtrack.jetbrains.com/issue/KT-83648) Native: don't use `sun.misc.Unsafe` in `NativeMemoryAllocator` when running on JDK 25+
- [`KT-83647`](https://youtrack.jetbrains.com/issue/KT-83647) Native: don't use `sun.misc.Unsafe` in `nativeMemUtils` when running on JDK 25+

### Native. Build Infrastructure

- [`KT-85191`](https://youtrack.jetbrains.com/issue/KT-85191) K/N: Dependency cycle in libclangInterop
- [`KT-84937`](https://youtrack.jetbrains.com/issue/KT-84937) Kotlin/Native: non-reproducible .bc for mingw_x64

### Native. C Export

- [`KT-61748`](https://youtrack.jetbrains.com/issue/KT-61748) KMM- warnings when compiling native targets (Kotlin 1.9.0)

### Native. C and ObjC Import

- [`KT-85705`](https://youtrack.jetbrains.com/issue/KT-85705) Swift-generated headers with external_source_symbol produce duplicate enum declarations
- [`KT-85399`](https://youtrack.jetbrains.com/issue/KT-85399) Kotlin/Native: TypeCastException when casting ObjC Protocol MetaClass with genericSafeCasts enabled
- [`KT-85508`](https://youtrack.jetbrains.com/issue/KT-85508) K/N: TypeCastException when using nw_parameters_create_secure_tcp block parameter on 2.3.20
- [`KT-84023`](https://youtrack.jetbrains.com/issue/KT-84023) Modular import fails with an obscure error when the failing module is not the last one

### Native. ObjC Export

- [`KT-85171`](https://youtrack.jetbrains.com/issue/KT-85171) Red Swift code in Native UI Multiplatform App project from Template Gallery

### Native. Runtime

- [`KT-84331`](https://youtrack.jetbrains.com/issue/KT-84331) Kotlin/Native: RunLoopFinalizerProcessor needs initialized runtime before it has any jobs

### Native. Runtime. Memory

- [`KT-83670`](https://youtrack.jetbrains.com/issue/KT-83670) K/N: gc concurrent mark phase assert Failed to terminate mark in STW in a single iteration

### Native. Swift Export

#### New Features

- [`KT-82705`](https://youtrack.jetbrains.com/issue/KT-82705) Support convenient export of Flow types in Swift export
- [`KT-85130`](https://youtrack.jetbrains.com/issue/KT-85130) [Swift Export] Preserve TypeInfo on SharedFlow
- [`KT-84361`](https://youtrack.jetbrains.com/issue/KT-84361) [Swift Export] Preserve TypeInfo on StateFlow

#### Fixes

- [`KT-84317`](https://youtrack.jetbrains.com/issue/KT-84317) Swift Export: "protocol members can only be marked unavailable in an '`@objc`' protocol" in generated code for kotlinx-coroutines
- [`KT-85380`](https://youtrack.jetbrains.com/issue/KT-85380) [Swift Export] Attempt to bridge unbridgeable type: SirUnsupportedType
- [`KT-85704`](https://youtrack.jetbrains.com/issue/KT-85704) [Swift Export] cannot infer generic type of function returning a generic type
- [`KT-85711`](https://youtrack.jetbrains.com/issue/KT-85711) [Swift Export] suspend function returning non-null generic fails to compile
- [`KT-85715`](https://youtrack.jetbrains.com/issue/KT-85715) [Swift Export] generic interface in typealias fails to compile
- [`KT-85714`](https://youtrack.jetbrains.com/issue/KT-85714) [Swift Export] unsupported input type param in functional receiver
- [`KT-85458`](https://youtrack.jetbrains.com/issue/KT-85458) [Swift Export] value of a closure returning a closure generates invalid swift code
- [`KT-85521`](https://youtrack.jetbrains.com/issue/KT-85521) [Swift Export] conflicting overloads for generated Kotlin bridges
- [`KT-85293`](https://youtrack.jetbrains.com/issue/KT-85293) SwiftExportCoroutinesWithResultValidationTest.testCoroutines fails after cross-push
- [`KT-84515`](https://youtrack.jetbrains.com/issue/KT-84515) [Swift Export] suspend functional parameter generates invalid Swift code
- [`KT-82282`](https://youtrack.jetbrains.com/issue/KT-82282) Swift Export: suspend function returning Array leads to incompilable code
- [`KT-81540`](https://youtrack.jetbrains.com/issue/KT-81540) Swift Export: using interface in Set generates incompilable code
- [`KT-80305`](https://youtrack.jetbrains.com/issue/KT-80305) Support coroutines in Swift Export
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

### Tools. BCV

- [`KT-83476`](https://youtrack.jetbrains.com/issue/KT-83476) Use Maven publications as dump input [ABI Validation]

### Tools. Build Tools API

#### New Features

- [`KT-82791`](https://youtrack.jetbrains.com/issue/KT-82791) BTA: introduce an option for `ExecutionPolicy.WithDaemon` to control the daemon log files path
- [`KT-80963`](https://youtrack.jetbrains.com/issue/KT-80963) BTA: Add structured information about reported messages to KotlinLogger
- [`KT-73037`](https://youtrack.jetbrains.com/issue/KT-73037) Add input (like compiler arguments) changes tracking

#### Fixes

- [`KT-84228`](https://youtrack.jetbrains.com/issue/KT-84228) BTA: Improving KDoc generation for Enums and Custom Types
- [`KT-85738`](https://youtrack.jetbrains.com/issue/KT-85738) BTA forward compatibility: NoSuchFieldError on X_IGNORED_ANNOTATIONS_FOR_BRIDGES when API 2.3.0 is used with impl 2.4.0
- [`KT-85082`](https://youtrack.jetbrains.com/issue/KT-85082) Make Xignored-annotations-for-bridges type safe
- [`KT-82390`](https://youtrack.jetbrains.com/issue/KT-82390) [BTA] Remove deprecated non-builder factory functions and classes
- [`KT-85072`](https://youtrack.jetbrains.com/issue/KT-85072) AbstractMethodError when calling discoverScriptExtensionsOperationBuilder with pre-2.4.0 compiler
- [`KT-85447`](https://youtrack.jetbrains.com/issue/KT-85447) BTA: deprecate JvmCompilerArguments.contains (warning)
- [`KT-85092`](https://youtrack.jetbrains.com/issue/KT-85092) [BTA] Update BTA Backward Compatibility Testing: 2.3.20-RC → 2.3.20
- [`KT-85439`](https://youtrack.jetbrains.com/issue/KT-85439) BTA: Warn or error when incompatible compiler arguments are passed via applyArgumentStrings
- [`KT-75540`](https://youtrack.jetbrains.com/issue/KT-75540) Build Tools API Should Reject -Xbuild-file Argument
- [`KT-85391`](https://youtrack.jetbrains.com/issue/KT-85391) [BTA] Hide boilerplate required to load isolated BTA implementation
- [`KT-80679`](https://youtrack.jetbrains.com/issue/KT-80679) Add support  for the Build Tools API [ABI Validation]
- [`KT-85035`](https://youtrack.jetbrains.com/issue/KT-85035) Don't expose X_COMPILER_PLUGIN_ORDER in CommonCompilerArguments
- [`KT-84738`](https://youtrack.jetbrains.com/issue/KT-84738) Make Xscript-resolver-environment type safe
- [`KT-85204`](https://youtrack.jetbrains.com/issue/KT-85204) Make Xdump-directory type safe
- [`KT-85205`](https://youtrack.jetbrains.com/issue/KT-85205) Make Xdump-perf type safe
- [`KT-85069`](https://youtrack.jetbrains.com/issue/KT-85069) Make Xnullability-annotations type safe
- [`KT-85167`](https://youtrack.jetbrains.com/issue/KT-85167) Make Xjsr305 type safe
- [`KT-85094`](https://youtrack.jetbrains.com/issue/KT-85094) Make Xwarning-level type safe
- [`KT-85294`](https://youtrack.jetbrains.com/issue/KT-85294) BTA: Replace hardcoded `@since` in KDoc with dynamic versioning
- [`KT-85333`](https://youtrack.jetbrains.com/issue/KT-85333) Add BTA tests for BACKUP_CLASSES and KEEP_IC_CACHES_IN_MEMORY behavior after compilation error
- [`KT-84770`](https://youtrack.jetbrains.com/issue/KT-84770) BTA: default options cannot be retrieved from many option objects
- [`KT-85224`](https://youtrack.jetbrains.com/issue/KT-85224) Add `@ExperimentalArgumentApi` to compiler argument DSL types
- [`KT-84322`](https://youtrack.jetbrains.com/issue/KT-84322) Make X_PROFILE BTA compiler argument type safe
- [`KT-84953`](https://youtrack.jetbrains.com/issue/KT-84953) Fail TC build if generated files change
- [`KT-85189`](https://youtrack.jetbrains.com/issue/KT-85189) Refactor path argument types: flatten hierarchy and improve naming
- [`KT-84984`](https://youtrack.jetbrains.com/issue/KT-84984) Runtime NPEs caused by null return in CompilerMessageRenderer implementation
- [`KT-84015`](https://youtrack.jetbrains.com/issue/KT-84015) Introduce detection of custom script names to new BTA API

### Tools. CLI

- [`KT-85414`](https://youtrack.jetbrains.com/issue/KT-85414) Argument DSL: `delimiter = KotlinCompilerArgument.Delimiter.PathSeparator` generates invalid Kotlin code
- [`KT-85001`](https://youtrack.jetbrains.com/issue/KT-85001) Convert `ImplicitJvmExposeBoxed` language feature to analysis flag
- [`KT-84999`](https://youtrack.jetbrains.com/issue/KT-84999) Don't poison binaries with `ImplicitJvmExposeBoxed` language feature
- [`KT-85004`](https://youtrack.jetbrains.com/issue/KT-85004) Set proper since version for language feature about property annotation targeting
- [`KT-56850`](https://youtrack.jetbrains.com/issue/KT-56850) Separate K/Wasm CLI entry point from K/JS CLI

### Tools. Compiler Plugin API

- [`KT-85133`](https://youtrack.jetbrains.com/issue/KT-85133) Drop deprecated K1 specific methods from IrPluginContext

### Tools. Compiler Plugins

- [`KT-75656`](https://youtrack.jetbrains.com/issue/KT-75656) PowerAssert: Create runtime library
- [`KT-75873`](https://youtrack.jetbrains.com/issue/KT-75873) PowerAssert: display callable reference value under '::'
- [`KT-85151`](https://youtrack.jetbrains.com/issue/KT-85151) PowerAssert: Surround string and character values with quotes
- [`KT-85184`](https://youtrack.jetbrains.com/issue/KT-85184) PowerAssert: Annotation may only be used on expect and non-override functions
- [`KT-69036`](https://youtrack.jetbrains.com/issue/KT-69036) Power-Assert indent multiline values
- [`KT-85089`](https://youtrack.jetbrains.com/issue/KT-85089) PowerAssert: Wasm CompileError when using `PowerAssert.explanation`

### Tools. Gradle

#### New Features

- [`KT-76197`](https://youtrack.jetbrains.com/issue/KT-76197) Write Kotlin compiler warnings and errors to Problems API

#### Fixes

- [`KT-85412`](https://youtrack.jetbrains.com/issue/KT-85412) Module name is not sanitized with older Kotlin compiler versions
- [`KT-65566`](https://youtrack.jetbrains.com/issue/KT-65566) Use the new ConfigurationContainer consumable method to create consumable configurations
- [`KT-85509`](https://youtrack.jetbrains.com/issue/KT-85509) Remove deprecated API in the 2.4.0 release
- [`KT-83858`](https://youtrack.jetbrains.com/issue/KT-83858) Compatibility with Gradle 9.4.0 release
- [`KT-69830`](https://youtrack.jetbrains.com/issue/KT-69830) Support Gradle `com.gradle.develocity` plugin in KGP
- [`KT-85433`](https://youtrack.jetbrains.com/issue/KT-85433) Gradle: deprecate non-BTA JVM compiler execution mode
- [`KT-80448`](https://youtrack.jetbrains.com/issue/KT-80448) Remove internal & deprecated API from ExtrasProperty.kt
- [`KT-69701`](https://youtrack.jetbrains.com/issue/KT-69701) Gradle: module name is passed inconsistently to different types of compilations
- [`KT-83860`](https://youtrack.jetbrains.com/issue/KT-83860) Run tests against Gradle 9.4.0
- [`KT-83859`](https://youtrack.jetbrains.com/issue/KT-83859) Compile against Gradle API 9.4.0
- [`KT-84729`](https://youtrack.jetbrains.com/issue/KT-84729) Update Gradle plugin-publish version to enable configuration cache badge on Gradle plugins portal

### Tools. Gradle. BCV

- [`KT-83999`](https://youtrack.jetbrains.com/issue/KT-83999) ABI validation: Groovy DSL doesn’t deprecate included/excluded filters, allowing four filter configs instead of two
- [`KT-84461`](https://youtrack.jetbrains.com/issue/KT-84461) Remove the use of abi-tools-api from KGP [ABI Validation]
- [`KT-84100`](https://youtrack.jetbrains.com/issue/KT-84100) Add Deprecated annotation to legacyDump block and property [ABI Validation]

### Tools. Gradle. Compiler plugins

- [`KT-85343`](https://youtrack.jetbrains.com/issue/KT-85343) Update Compose Gradle plugin deprecations before 2.4

### Tools. Gradle. JS

- [`KT-84753`](https://youtrack.jetbrains.com/issue/KT-84753) Deprecate `KotlinJsCompilerType` and `KotlinProjectExtension` methods using it
- [`KT-81033`](https://youtrack.jetbrains.com/issue/KT-81033) K/JS, Wasm: Remove deprecated wasm declarations in "js" package
- [`KT-81034`](https://youtrack.jetbrains.com/issue/KT-81034) K/JS, Wasm: Remove deprecated public constructors of JS declarations
- [`KT-81030`](https://youtrack.jetbrains.com/issue/KT-81030) K/JS, Wasm: remove deprecated NodeJsExec.create
- [`KT-81037`](https://youtrack.jetbrains.com/issue/KT-81037) K/JS, Wasm: Remove deprecated internal JS functions

### Tools. Gradle. Multiplatform

- [`KT-84767`](https://youtrack.jetbrains.com/issue/KT-84767) K/N: associateWith triggers warning about friend-modules libs not included in -library argument
- [`KT-69571`](https://youtrack.jetbrains.com/issue/KT-69571) compileNativeMainKotlinMetadata not handling project/prebuilt substitutions
- [`KT-84533`](https://youtrack.jetbrains.com/issue/KT-84533) KMP: compileCommonMainKotlinMetadata: "Unresolved reference" for androidx.savedstate from Maven (works with project() dependency)
- [`KT-84669`](https://youtrack.jetbrains.com/issue/KT-84669) SPM import: If iosApp dir located outside of the project, checkSyntheticImportProjectIsCorrectlyIntegrated will fail
- [`KT-84085`](https://youtrack.jetbrains.com/issue/KT-84085) Remove deprecated gradle property kotlin.kmp.isolated-projects.support
- [`KT-84597`](https://youtrack.jetbrains.com/issue/KT-84597) Remove trailing comma for dependencies blocks settings in Package.swift
- [`KT-82895`](https://youtrack.jetbrains.com/issue/KT-82895) kotlin-stdlib import is flaky in commonTest in 2.1.21

### Tools. Gradle. Native

#### New Features

- [`KT-83873`](https://youtrack.jetbrains.com/issue/KT-83873) Redo how dynamic library linkage and promotion are handled

#### Fixes

- [`KT-69896`](https://youtrack.jetbrains.com/issue/KT-69896) Native: output to stderr ends up in the Gradle log
- [`KT-85708`](https://youtrack.jetbrains.com/issue/KT-85708) [KGP] dSYM copy task ignores `isStatic` due to eager read before framework configuration
- [`KT-84262`](https://youtrack.jetbrains.com/issue/KT-84262) integrateEmbedAndSign produces an incorrect Gradle call for the root project
- [`KT-84730`](https://youtrack.jetbrains.com/issue/KT-84730) Add Kdocs to SwiftPM import APIs
- [`KT-85502`](https://youtrack.jetbrains.com/issue/KT-85502) Swift PM Import: "Library not loaded": KotlinMultiplatformLinkedPackage.framework is not copied next to the executable
- [`KT-85510`](https://youtrack.jetbrains.com/issue/KT-85510) Cleanup native tasks API
- [`KT-82824`](https://youtrack.jetbrains.com/issue/KT-82824) Make linker hack path relative
- [`KT-85128`](https://youtrack.jetbrains.com/issue/KT-85128) Refactor SwiftPM import lock tests and test utils
- [`KT-83874`](https://youtrack.jetbrains.com/issue/KT-83874) Linker hack doesn't work when clang uses response files
- [`KT-83681`](https://youtrack.jetbrains.com/issue/KT-83681) Parallelize parts of SwiftPM import pipeline that are called during import

### Tools. Gradle. Wasm

- [`KT-85046`](https://youtrack.jetbrains.com/issue/KT-85046) K/Wasm: Wasm per-module Gradle integration tests on Windows

### Tools. Incremental Compile

- [`KT-85387`](https://youtrack.jetbrains.com/issue/KT-85387) BTA: switch the default value of `MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION` to `true`
- [`KT-85386`](https://youtrack.jetbrains.com/issue/KT-85386) BTA JVM IC: 'moduleName' is null!
- [`KT-84450`](https://youtrack.jetbrains.com/issue/KT-84450) Star imports are not reported via FirImportTrackerComponent

### Tools. JPS

- [`KT-81579`](https://youtrack.jetbrains.com/issue/KT-81579) JPS: -Xwarning-level=DEPRECATION:warning not supported

### Tools. Kapt

- [`KT-32743`](https://youtrack.jetbrains.com/issue/KT-32743) Kapt, Maven: Do not include compile classpath entries in the annotation processing classpath
- [`KT-41217`](https://youtrack.jetbrains.com/issue/KT-41217) Running kapt with Maven does not seem to include the compilation classpath

### Tools. Maven

- [`KT-84386`](https://youtrack.jetbrains.com/issue/KT-84386) Support Maven Toolchains in kotlin-maven-plugin
- [`KT-76062`](https://youtrack.jetbrains.com/issue/KT-76062) Maven: remove Kotlin script execution support
- [`KT-85317`](https://youtrack.jetbrains.com/issue/KT-85317) Auto‑align jvmTarget with the project’s Java level
- [`KT-84101`](https://youtrack.jetbrains.com/issue/KT-84101) Maven: compile and test-compile handle sourceDirs inconsistently
- [`KT-84653`](https://youtrack.jetbrains.com/issue/KT-84653) Add integration test for KAPT with smart defaults in mixed Kotlin+Java projects
- [`KT-85121`](https://youtrack.jetbrains.com/issue/KT-85121) Maven: enable configuration inputs tracking in BTA
- [`KT-84778`](https://youtrack.jetbrains.com/issue/KT-84778) Add integration test for auto-bind execution order in mixed Kotlin+Java projects
- [`KT-85146`](https://youtrack.jetbrains.com/issue/KT-85146) Maven: Adding stdlib as smart-default may break maven dependency resolution for other plugins
- [`KT-83109`](https://youtrack.jetbrains.com/issue/KT-83109) Remove beanshell and groovy verification in kotlin-maven-plugin-test

### Tools. REPL

- [`KT-77816`](https://youtrack.jetbrains.com/issue/KT-77816) REPL: Support for `const` properties
- [`KT-84483`](https://youtrack.jetbrains.com/issue/KT-84483) [K2 Repl] NullPointerException in Analysis when using custom classes
- [`KT-84803`](https://youtrack.jetbrains.com/issue/KT-84803) [REPL] FirReplSnippet: provide the eval function symbol instead of the name (`evalFunctionName`)

### Tools. Scripts

- [`KT-85105`](https://youtrack.jetbrains.com/issue/KT-85105) Scripts: JVM backend internal error (IR lowering) when scratch file contains anonymous object
- [`KT-85103`](https://youtrack.jetbrains.com/issue/KT-85103) Exception while generating code when explain destructuring decls
- [`KT-85029`](https://youtrack.jetbrains.com/issue/KT-85029) Kotlin Scripting: ScriptDiagnostic reports "at null" instead of error location

### Tools. Statistics (FUS)

- [`KT-85628`](https://youtrack.jetbrains.com/issue/KT-85628) KGP: composite build FUS metrics fail on access of 'configurationTimeMetrics'

### Tools. Wasm

- [`KT-75086`](https://youtrack.jetbrains.com/issue/KT-75086) Wasm: Deprecate and remove D8 in js packages
- 
## 2.4.0-Beta1

### Analysis API

- [`KT-83867`](https://youtrack.jetbrains.com/issue/KT-83867) OVERLOAD_RESOLUTION_AMBIGUITY false positive with assertEquals in IJ repo
- [`KT-83723`](https://youtrack.jetbrains.com/issue/KT-83723) [Analysis API] Enable experimental KDoc resolver by default
- [`KT-83388`](https://youtrack.jetbrains.com/issue/KT-83388) Analysis API: properly support KMP in KotlinPackageProvider

### Analysis API. Code Compilation

- [`KT-78946`](https://youtrack.jetbrains.com/issue/KT-78946) Evaluation of variable with local class in type parameter leads to InventNamesForLocalClasses exception

### Analysis API. FIR

- [`KT-84711`](https://youtrack.jetbrains.com/issue/KT-84711) K2 IDE sometimes loses FIR plugin-generated declarations after file changes
- [`KT-84596`](https://youtrack.jetbrains.com/issue/KT-84596) Improve K2 Jooq completion performance
- [`KT-84525`](https://youtrack.jetbrains.com/issue/KT-84525) KaValueParameterSymbol#getHasSynthesizedName returns false for FirDeclarationOrigin.SubstitutionOverride.DeclarationSite
- [`KT-68260`](https://youtrack.jetbrains.com/issue/KT-68260) K2 AA: InvalidFirElementTypeException “For CALLABLE_REFERENCE_EXPRESSION with text `::lam1`, unexpected element of type: no element found” with illegal callable reference call
- [`KT-83546`](https://youtrack.jetbrains.com/issue/KT-83546) Kotlin analysis reach ClsCustomNavigationPolicy
- [`KT-84259`](https://youtrack.jetbrains.com/issue/KT-84259) Move CommonDefaultImportsProvider to the frontend independent module
- [`KT-82945`](https://youtrack.jetbrains.com/issue/KT-82945) Analysis API: KotlinIllegalArgumentExceptionWithAttachments: Expected FirResolvedTypeRef with ConeKotlinType but was FirUserTypeRefImpl
- [`KT-71135`](https://youtrack.jetbrains.com/issue/KT-71135) AA: exception from sealed inheritors checker when `analyzeCopy`

### Analysis API. Infrastructure

- [`KT-84776`](https://youtrack.jetbrains.com/issue/KT-84776) The test data manager misses the redundancy check in the update mode
- [`KT-84962`](https://youtrack.jetbrains.com/issue/KT-84962) The test data manager misses -ea flag
- [`KT-84388`](https://youtrack.jetbrains.com/issue/KT-84388) Preserve the EOF status in the test data manager to avoid extra changes
- [`KT-83905`](https://youtrack.jetbrains.com/issue/KT-83905) Analysis API: Improve UX with test data
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

- [`KT-83846`](https://youtrack.jetbrains.com/issue/KT-83846) Set up guidelines for PSI
- [`KT-84135`](https://youtrack.jetbrains.com/issue/KT-84135) Deprecate KtSelfType

### Analysis API. Providers and Caches

- [`KT-82731`](https://youtrack.jetbrains.com/issue/KT-82731) Analysis API: Limit granular tree change processing to a few files
- [`KT-79234`](https://youtrack.jetbrains.com/issue/KT-79234) Analysis API: Usage of `asMap()` on Caffeine caches bypasses stats counters
- [`KT-74090`](https://youtrack.jetbrains.com/issue/KT-74090) Analysis API: Support dumb mode (restricted analysis)

### Analysis API. Standalone

- [`KT-83801`](https://youtrack.jetbrains.com/issue/KT-83801) Nested typealiases are not correctly indexed in standalone mode

### Analysis API. Surface

#### New Features

- [`KT-73534`](https://youtrack.jetbrains.com/issue/KT-73534) SAM method API
- [`KT-82993`](https://youtrack.jetbrains.com/issue/KT-82993) Support explicit backing fields in the Analysis API

#### Fixes

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
- [`KT-79186`](https://youtrack.jetbrains.com/issue/KT-79186) KtCompletionExtensionCandidateChecker does not work for extensions when using callable references of a type
- [`KT-83777`](https://youtrack.jetbrains.com/issue/KT-83777) Analysis API: The resolution scope of a context module accepts elements from associated dangling files
- [`KT-82571`](https://youtrack.jetbrains.com/issue/KT-82571) No expected type for overridden property without explicit type
- [`KT-83759`](https://youtrack.jetbrains.com/issue/KT-83759) Analysis API: Mark platform interface APIs with `@KaPlatformInterface`
- [`KT-83223`](https://youtrack.jetbrains.com/issue/KT-83223) Support "Explicit context arguments" in the Analysis API
- [`KT-65186`](https://youtrack.jetbrains.com/issue/KT-65186) K2: Analysis API: KtExpressionTypeProvider.getExpectedType works incorrectly for the right hand side of assignment expressions
- [`KT-76011`](https://youtrack.jetbrains.com/issue/KT-76011) `KaFirNamedClassSymbol#companionObject` doesn't provide generated objects generated by compiled plugins
- [`KT-73290`](https://youtrack.jetbrains.com/issue/KT-73290) Analysis API: Improve the architecture of content scopes and resolution scopes

### Backend. Native. Debug

- [`KT-83804`](https://youtrack.jetbrains.com/issue/KT-83804) Native: debug information generator converts relative paths to absolute ones

### Backend. Wasm

- [`KT-83162`](https://youtrack.jetbrains.com/issue/KT-83162) K/Wasm: renaming temporary and synthetic variables in the Chrome debugger
- [`KT-85008`](https://youtrack.jetbrains.com/issue/KT-85008) Develop and publish a demo app using an early version of the component model support
- [`KT-65030`](https://youtrack.jetbrains.com/issue/KT-65030) K/Wasm: memory allocator for Component Model ABI
- [`KT-83607`](https://youtrack.jetbrains.com/issue/KT-83607) WasmJS: Production build eliminates 'else if' branch when 'else' is not wrapped with curly braces
- [`KT-83728`](https://youtrack.jetbrains.com/issue/KT-83728) [Wasm] Invalid Ir type while suspend call with blocked if null comprehansion
- [`KT-82803`](https://youtrack.jetbrains.com/issue/KT-82803) Kotlin/WASM: Failed to compile the doResume function with if inside catch block
- [`KT-83800`](https://youtrack.jetbrains.com/issue/KT-83800) [Wasm] Closed world per-module compilation

### Compiler

#### New Features

- [`KT-84319`](https://youtrack.jetbrains.com/issue/KT-84319) Add JVM target bytecode version 26
- [`KT-83165`](https://youtrack.jetbrains.com/issue/KT-83165) Collection literals: treat Deprecated(HIDDEN) operators `of` reasonably
- [`KT-84487`](https://youtrack.jetbrains.com/issue/KT-84487) "-Xcollection-literals" compiler flag
- [`KT-84072`](https://youtrack.jetbrains.com/issue/KT-84072) Collection literals: treat visibility of `of` during resolve correctly
- [`KT-80500`](https://youtrack.jetbrains.com/issue/KT-80500) Collection literals: Analyze `ConeCollectionLiteralAtom` in cases their expected type is not fully known
- [`KT-80491`](https://youtrack.jetbrains.com/issue/KT-80491) Implement fallback mechanism for collection literals
- [`KT-80490`](https://youtrack.jetbrains.com/issue/KT-80490) Implement overload resolution mechanism for collection literals
- [`KT-84484`](https://youtrack.jetbrains.com/issue/KT-84484) Companion Extensions Analysis & Resolution
- [`KT-84199`](https://youtrack.jetbrains.com/issue/KT-84199) Implement DontMakeExplicitNullableJavaTypeArgumentsFlexible feature
- [`KT-83765`](https://youtrack.jetbrains.com/issue/KT-83765) Make -Xsuppress-version-warnings have a diagnostic ID
- [`KT-84288`](https://youtrack.jetbrains.com/issue/KT-84288) Companion Blocks Analysis & Resolution
- [`KT-84287`](https://youtrack.jetbrains.com/issue/KT-84287) Build Raw FIR for Companion Blocks & Extensions
- [`KT-84286`](https://youtrack.jetbrains.com/issue/KT-84286) Parse Companion Blocks & Extensions
- [`KT-66344`](https://youtrack.jetbrains.com/issue/KT-66344) K1 & K2: False positive WRONG_NUMBER_OF_TYPE_ARGUMENTS in callable reference to inner class member
- [`KT-76766`](https://youtrack.jetbrains.com/issue/KT-76766) Warning is missing for wrong subclass checking
- [`KT-74049`](https://youtrack.jetbrains.com/issue/KT-74049) Introduce special override rule to allow overriding T! with T & Any

#### Performance Improvements

- [`KT-84412`](https://youtrack.jetbrains.com/issue/KT-84412) iOS release build time dramatically increases with 2.3.20-Beta2 compared to 2.3.10
- [`KT-80367`](https://youtrack.jetbrains.com/issue/KT-80367) Reduce memory consumption of DevirtualizationAnalysis
- [`KT-82559`](https://youtrack.jetbrains.com/issue/KT-82559) linkDebugTest*X64 tasks are slower for Kotlin 2.3 than for 2.2
- [`KT-84095`](https://youtrack.jetbrains.com/issue/KT-84095) Improve Unit tail-call optimization to support inline generic functions similar to `suspendCoroutine`

#### Fixes

- [`KT-84559`](https://youtrack.jetbrains.com/issue/KT-84559) `@OptIn` on collection literal and context-sensitive does not work
- [`KT-84675`](https://youtrack.jetbrains.com/issue/KT-84675) Collection literals: 'Not singleClassifierType superType: TypeVariable(S)' in PCLA
- [`KT-84547`](https://youtrack.jetbrains.com/issue/KT-84547) Collection literals: "Expected expression 'FirCollectionLiteralImpl' to be resolved" in elvis expression
- [`KT-83920`](https://youtrack.jetbrains.com/issue/KT-83920) False positive "modifier 'value' is not applicable to 'local variable'" with soft keyword in positional destructuring (square bracket) declaration
- [`KT-84190`](https://youtrack.jetbrains.com/issue/KT-84190) Implement basic functionality for returnsResultOf contract
- [`KT-85058`](https://youtrack.jetbrains.com/issue/KT-85058) Remove final field modification in DescriptorRendererOptionsImpl to prevent warnings on JDK 26+
- [`KT-72710`](https://youtrack.jetbrains.com/issue/KT-72710) Incorrect behaviour of tail call suspend functions optimization
- [`KT-80590`](https://youtrack.jetbrains.com/issue/KT-80590) Drop language version 1.9 for JVM
- [`KT-83904`](https://youtrack.jetbrains.com/issue/KT-83904) [Inliner] Inline function overrides an abstract method with a default value in an inheritance chain
- [`KT-77584`](https://youtrack.jetbrains.com/issue/KT-77584) Support scripts built from LT in scripting API
- [`KT-84185`](https://youtrack.jetbrains.com/issue/KT-84185) Type arguments are wrongly allowed in receivers of static calls
- [`KT-83441`](https://youtrack.jetbrains.com/issue/KT-83441) False positive: REDUNDANT_CALL_OF_CONVERSION_METHOD
- [`KT-83587`](https://youtrack.jetbrains.com/issue/KT-83587) K2: Missing null-check when using == on Short! and Byte! platform types
- [`KT-84860`](https://youtrack.jetbrains.com/issue/KT-84860) False positive UNINITIALIZED_ENUM_COMPANION in enum access with explicit receiver in enum initializer when enum class has a companion
- [`KT-84405`](https://youtrack.jetbrains.com/issue/KT-84405) ClassCastException with conflicting projection on the LHS of a callable reference
- [`KT-84866`](https://youtrack.jetbrains.com/issue/KT-84866) Reserve CoroutineContext as context parameter for future use
- [`KT-84717`](https://youtrack.jetbrains.com/issue/KT-84717) Provide information for qualified expressions that might be replaced with context-sensitive simple names in IDE mode
- [`KT-65239`](https://youtrack.jetbrains.com/issue/KT-65239) K2: Render FIR declaration instead of IR-based descriptors in IR signature clash diagnostics
- [`KT-84743`](https://youtrack.jetbrains.com/issue/KT-84743) Type parameter declared as 'in' can be used in 'out' position in DNN & flexible types
- [`KT-84720`](https://youtrack.jetbrains.com/issue/KT-84720) "Unused return value" is not reported inside used if/when multi-statement blocks
- [`KT-84198`](https://youtrack.jetbrains.com/issue/KT-84198) Support multiple embedded .let-like calls with returnsResultOf contract
- [`KT-84310`](https://youtrack.jetbrains.com/issue/KT-84310) No Warning Emitted For Deprecated Java Enum Value Usage
- [`KT-81871`](https://youtrack.jetbrains.com/issue/KT-81871) Drop context receiver tests
- [`KT-80113`](https://youtrack.jetbrains.com/issue/KT-80113) Consider improving diagnostic messages related to `==`/`===`/`is`/`as`
- [`KT-84714`](https://youtrack.jetbrains.com/issue/KT-84714) KJS: Forbid exporting properties with context parameters
- [`KT-84380`](https://youtrack.jetbrains.com/issue/KT-84380) Type alias to non-generic class can have (arbitrary number of) type arguments in LHS of `::class`
- [`KT-84366`](https://youtrack.jetbrains.com/issue/KT-84366) Invalid name for captured `this` in bytecode
- [`KT-80701`](https://youtrack.jetbrains.com/issue/KT-80701) Native: `-Xbinary=cCallMode` is not integrated with compiler caches
- [`KT-84000`](https://youtrack.jetbrains.com/issue/KT-84000) Native: test pre-codegen inliner on CI
- [`KT-57557`](https://youtrack.jetbrains.com/issue/KT-57557) Implement getAndSet for AtomicNativePtr via getAndSetField intrinsic
- [`KT-84352`](https://youtrack.jetbrains.com/issue/KT-84352) `createUninitializedInstance` generates invalid LLVM for value classes
- [`KT-84411`](https://youtrack.jetbrains.com/issue/KT-84411) Confusing message for the class reference of the inner class with the type parameter
- [`KT-84280`](https://youtrack.jetbrains.com/issue/KT-84280) Standalone `Unit` qualifier allows type arguments: `Unit<Any>`
- [`KT-84281`](https://youtrack.jetbrains.com/issue/KT-84281) Standalone typealias-to-object qualifier allows type arguments and has type `Unit` in this case
- [`KT-84594`](https://youtrack.jetbrains.com/issue/KT-84594) EBF is smartcasted in inline function with `@PiblishedApi`
- [`KT-83938`](https://youtrack.jetbrains.com/issue/KT-83938) Missing Tail call optimization in reference classes returning Unit
- [`KT-83989`](https://youtrack.jetbrains.com/issue/KT-83989) Update coroutines-codegen.md after changes of Unit tailcall optimization
- [`KT-83988`](https://youtrack.jetbrains.com/issue/KT-83988) Remove extraneous POP+GETSTATIC Unit for calls of Unit-returning suspend functions
- [`KT-80925`](https://youtrack.jetbrains.com/issue/KT-80925) Replace "useless" in diagnostic messages
- [`KT-83646`](https://youtrack.jetbrains.com/issue/KT-83646) Native: don't use `sun.misc.Unsafe` in `ByteArrayStream` when running on JVM 24+
- [`KT-82122`](https://youtrack.jetbrains.com/issue/KT-82122) Prohibit arbitrary placement of type parameters in callable reference LHS
- [`KT-82574`](https://youtrack.jetbrains.com/issue/KT-82574) Fixation: consider preferring EQUALS constraints to LOWER ones
- [`KT-83564`](https://youtrack.jetbrains.com/issue/KT-83564) Consider dropping `HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT`
- [`KT-84213`](https://youtrack.jetbrains.com/issue/KT-84213) Flaky incremental compilation behaviour with EBF
- [`KT-84133`](https://youtrack.jetbrains.com/issue/KT-84133) Adopt `initInstance` to handle value classes
- [`KT-24840`](https://youtrack.jetbrains.com/issue/KT-24840) Square bracket escaping in KDoc
- [`KT-82123`](https://youtrack.jetbrains.com/issue/KT-82123) KDoc: references that goes after markdown blocks don't have links
- [`KT-84196`](https://youtrack.jetbrains.com/issue/KT-84196) Handle multiple entry/exit points for returnsResultOf functions
- [`KT-84195`](https://youtrack.jetbrains.com/issue/KT-84195) Handle function references in returnsResultOf
- [`KT-84167`](https://youtrack.jetbrains.com/issue/KT-84167) Invalid type references with type arguments in package parts compile without diagnostics
- [`KT-37179`](https://youtrack.jetbrains.com/issue/KT-37179) false-positive shadowing warning on local and member extension functions in presence of member extension property with invoke operator
- [`KT-84209`](https://youtrack.jetbrains.com/issue/KT-84209) False negative ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT on context parameters of function types
- [`KT-83354`](https://youtrack.jetbrains.com/issue/KT-83354) Wrong position for lambda context type error
- [`KT-84206`](https://youtrack.jetbrains.com/issue/KT-84206) Remove forcesPreReleaseBinaries = true from ExplicitBackingFields
- [`KT-83524`](https://youtrack.jetbrains.com/issue/KT-83524) An anonymous function with named parameters throws FileAnalysisException
- [`KT-84155`](https://youtrack.jetbrains.com/issue/KT-84155) K2: NO_CONTEXT_ARGUMENT caused by stale value in `NewConstraintSystemImpl.hasContradictionInForkPointsCache`
- [`KT-83829`](https://youtrack.jetbrains.com/issue/KT-83829) False-negative INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE
- [`KT-83842`](https://youtrack.jetbrains.com/issue/KT-83842) KIAEWA: Exception in expression checkers for `@OptIn`(markerClass=[…])
- [`KT-84045`](https://youtrack.jetbrains.com/issue/KT-84045) Evaluate default arguments of annotation's parameters using FIR evaluator
- [`KT-70562`](https://youtrack.jetbrains.com/issue/KT-70562) `@SubclassOptInRequired` cannot accept multiple experimental marker
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

### IR. Inlining

- [`KT-84112`](https://youtrack.jetbrains.com/issue/KT-84112) Intra-module inliner: No container found for type parameter 'T'
- [`KT-84416`](https://youtrack.jetbrains.com/issue/KT-84416) High memory usage for IrFileEntry after enabling inliner
- [`KT-75396`](https://youtrack.jetbrains.com/issue/KT-75396) [IR] Pass LoweringContext to inline and serialization checkers
- [`KT-73708`](https://youtrack.jetbrains.com/issue/KT-73708) Use some marker in KLIBs produced with IR inliner

### IR. Interpreter

- [`KT-84561`](https://youtrack.jetbrains.com/issue/KT-84561) K2: Convert evaluated constant by default in FIR2IR

### IR. Tree

- [`KT-79663`](https://youtrack.jetbrains.com/issue/KT-79663) KLIB-based compilers: Promote partial linkage to "always on"
- [`KT-76634`](https://youtrack.jetbrains.com/issue/KT-76634) PL: Don't report warnings in cases that don't lead to runtime errors
- [`KT-72950`](https://youtrack.jetbrains.com/issue/KT-72950) Partial Linkage: Change the semantics of `-Xpartial-linkage-loglevel`
- [`KT-79801`](https://youtrack.jetbrains.com/issue/KT-79801) KLIBs: Implement checks for symbols loaded by the compiler on 1st and 2nd phases
- [`KT-72812`](https://youtrack.jetbrains.com/issue/KT-72812) IR serializer: Don't serialize any cinterop fake overrides to Klibs

### JVM. Reflection

- [`KT-85091`](https://youtrack.jetbrains.com/issue/KT-85091) Reflection: "KotlinReflectionInternalError: Unsupported parameter owner: null" on attempt to get annotations of annotation constructor parameter
- [`KT-84796`](https://youtrack.jetbrains.com/issue/KT-84796) Reflection: mutable flexibility is lost for K1-based types in KClass.allSupertypes
- [`KT-84494`](https://youtrack.jetbrains.com/issue/KT-84494) Reflection: Java Collections have differences in kotlin supertypes from old K1 reflection
- [`KT-84382`](https://youtrack.jetbrains.com/issue/KT-84382) Reflection: raw list in Java type is transformed to List instead of MutableList
- [`KT-84492`](https://youtrack.jetbrains.com/issue/KT-84492) Reflection: supertypes of raw list in Java type are not raw
- [`KT-84076`](https://youtrack.jetbrains.com/issue/KT-84076) Reflection: list in Java type is transformed to flexible instead of mutable list
- [`KT-14990`](https://youtrack.jetbrains.com/issue/KT-14990) 'callBy' for inner class constructor fails at run-time
- [`KT-82881`](https://youtrack.jetbrains.com/issue/KT-82881) Reflection: update KCallable.callBy kdoc to mention vararg parameters
- [`KT-84075`](https://youtrack.jetbrains.com/issue/KT-84075) Reflection: wildcard in Java type is transformed to `out Any!` instead of star projection
- [`KT-82659`](https://youtrack.jetbrains.com/issue/KT-82659) Reflection: IAE on a call to a Java inner class constructor

### JavaScript

#### New Features

- [`KT-51292`](https://youtrack.jetbrains.com/issue/KT-51292) Proposed behavior of `@JsExport` on interfaces and classes with companion objects
- [`KT-82128`](https://youtrack.jetbrains.com/issue/KT-82128) [K/JS] Allow named companion objects in exported interfaces
- [`KT-21626`](https://youtrack.jetbrains.com/issue/KT-21626) Support ES2015 syntax in `js` function
- [`KT-83452`](https://youtrack.jetbrains.com/issue/KT-83452) K/JS: Support ES6 array destructuring in js() calls
- [`KT-83451`](https://youtrack.jetbrains.com/issue/KT-83451) K/JS: Support ES6 object destructuring in js() calls

#### Performance Improvements

- [`KT-77646`](https://youtrack.jetbrains.com/issue/KT-77646) KJS: optimize Byte/Char/Short/Int/Float/DoubleArray.copyOf(newSize)

#### Fixes

- [`KT-84002`](https://youtrack.jetbrains.com/issue/KT-84002) Bump version from 2.3 to 2.4 for JsNoRuntime-related annotations
- [`KT-84090`](https://youtrack.jetbrains.com/issue/KT-84090) Save variance in the generated TypeScript
- [`KT-84332`](https://youtrack.jetbrains.com/issue/KT-84332) KJS: Reconsider disallowing nested classes in exported interfaces
- [`KT-56618`](https://youtrack.jetbrains.com/issue/KT-56618) KJS/IR: Support external interfaces from common code (via annotation?)
- [`KT-84474`](https://youtrack.jetbrains.com/issue/KT-84474) Kotlin/JS: Long::class becomes null when passing the value to a generic function with -Xes-long-as-bigint
- [`KT-83701`](https://youtrack.jetbrains.com/issue/KT-83701) Escaped identifier with a quote cause an invalid d.ts file
- [`KT-84647`](https://youtrack.jetbrains.com/issue/KT-84647) K/JS: Class expressions are not supported in js() calls
- [`KT-68281`](https://youtrack.jetbrains.com/issue/KT-68281) K/JS: Order of classes in initMetadataForClass are not deterministic
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
- [`KT-69353`](https://youtrack.jetbrains.com/issue/KT-69353) KJS / d.ts: Kotlin does not export base collection classes along with their mutable collection counterparts

### Klibs

#### Performance Improvements

- [`KT-84451`](https://youtrack.jetbrains.com/issue/KT-84451) [Klib] Use varint encoding for element sizes in IR tables
- [`KT-80903`](https://youtrack.jetbrains.com/issue/KT-80903) [Klib] Optimize size of serialized IR element coordinates
- [`KT-84400`](https://youtrack.jetbrains.com/issue/KT-84400) [Klib] Optimize size of serialized IrExpression
- [`KT-79675`](https://youtrack.jetbrains.com/issue/KT-79675) K/N: Uncached ZipFIleSystemAccessor

#### Fixes

- [`KT-82471`](https://youtrack.jetbrains.com/issue/KT-82471) [K/N] Klib forward compatibility testing with codegen tests
- [`KT-83807`](https://youtrack.jetbrains.com/issue/KT-83807) Restore non-nullability of symbols not available in 2.3.0 stdlib
- [`KT-83929`](https://youtrack.jetbrains.com/issue/KT-83929) Add tests for IR signatures of static properties and functions
- [`KT-83012`](https://youtrack.jetbrains.com/issue/KT-83012) Export in previous version (Native): add the checker for incompatible Kotlin stdlib/compiler pairs
- [`KT-82469`](https://youtrack.jetbrains.com/issue/KT-82469) [K/N] Klib backward compatibility testing with codegen tests
- [`KT-84341`](https://youtrack.jetbrains.com/issue/KT-84341) Fix detection of box function in forward compatibility tests
- [`KT-81411`](https://youtrack.jetbrains.com/issue/KT-81411) Merge `KonanLibrary` to `KotlinLibrary` to simplify adoption of `KlibLoader` in the Kotlin/Native compiler
- [`KT-83748`](https://youtrack.jetbrains.com/issue/KT-83748) Bump versions in JS Klib compatibility testing
- [`KT-78188`](https://youtrack.jetbrains.com/issue/KT-78188) [JS] Klib backward and forward compatibility testing
- [`KT-83724`](https://youtrack.jetbrains.com/issue/KT-83724) Fix & unmute stdlib & kotlin-test compatibility tests
- [`KT-83151`](https://youtrack.jetbrains.com/issue/KT-83151) Restore non-nullability of symbols available since 2.3

### Language Design

- [`KT-80852`](https://youtrack.jetbrains.com/issue/KT-80852) Version overloading: generate overloads corresponding to different versions of a function whose parameters are annotated with `@IntroducedAt`(<version>)

### Libraries

#### New Features

- [`KT-73111`](https://youtrack.jetbrains.com/issue/KT-73111) No UInt.toBigInteger() and ULong.toBigInteger() conversion function
- [`KT-78499`](https://youtrack.jetbrains.com/issue/KT-78499) Add isSorted() extension to standard library

#### Fixes

- [`KT-83956`](https://youtrack.jetbrains.com/issue/KT-83956) Clarify joinToString behavior when the receiver is empty
- [`KT-71848`](https://youtrack.jetbrains.com/issue/KT-71848) Kotlinx.metadata: Add `CompilerPluginData` into Km API
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

- [`KT-84826`](https://youtrack.jetbrains.com/issue/KT-84826) Bump the minimum deployment version of Apple targets
- [`KT-78686`](https://youtrack.jetbrains.com/issue/KT-78686) LLVM update Q1 2026
- [`KT-82674`](https://youtrack.jetbrains.com/issue/KT-82674) Native: dyld[...]: Symbol not found: _mach_vm_reclaim_update_kernel_accounting_trap on macOS
- [`KT-81748`](https://youtrack.jetbrains.com/issue/KT-81748) Create a phased CLI for Native klib compilation
- [`KT-82879`](https://youtrack.jetbrains.com/issue/KT-82879) Native: DLLs in the Windows distribution are not reproducible
- [`KT-83283`](https://youtrack.jetbrains.com/issue/KT-83283) Test Kotlin/Native performance tests compilation in Gradle 9.0
- [`KT-82872`](https://youtrack.jetbrains.com/issue/KT-82872) Native: make Kotlin/Native distribution compiler cache reproducible for Linux
- [`KT-82871`](https://youtrack.jetbrains.com/issue/KT-82871) Native: cstubs.bc for android_* platform libraries contain absolute paths in string literals
- [`KT-34467`](https://youtrack.jetbrains.com/issue/KT-34467) Cinterop: Clang crashes when -fmodule-map-file is specified (SIGSEGV)

### Native. Build Infrastructure

- [`KT-80072`](https://youtrack.jetbrains.com/issue/KT-80072) Make Kotlin/Native distribution reproducible
- [`KT-81771`](https://youtrack.jetbrains.com/issue/KT-81771) konanc failing to load native libraries
- [`KT-84503`](https://youtrack.jetbrains.com/issue/KT-84503) Duplicate META-INF/serialization.shadow.kotlin_module entry in kotlin-native-compiler-embeddable jar

### Native. C and ObjC Import

- [`KT-81433`](https://youtrack.jetbrains.com/issue/KT-81433) Generate C-interop KLIBs in previous ABI version in Kotlin 2.4.0
- [`KT-82766`](https://youtrack.jetbrains.com/issue/KT-82766) K/N: external_source_symbol clang attribute causes cinterops with -fmodules to downgrade to forward declaration
- [`KT-82402`](https://youtrack.jetbrains.com/issue/KT-82402) Inter-cinterop type reuse with -fmodules uses forward declaration when an actual declaration is available
- [`KT-82377`](https://youtrack.jetbrains.com/issue/KT-82377) Fix ObjC forward declaration handling in modular cinterops
- [`KT-81695`](https://youtrack.jetbrains.com/issue/KT-81695) Repeated typedefs across multiple clang modules break cinterop with -fmodules
- [`KT-81752`](https://youtrack.jetbrains.com/issue/KT-81752) Native: investigate and remove filtering of `-fmodule-map-file` in cinterop
- [`KT-82379`](https://youtrack.jetbrains.com/issue/KT-82379) Introduce lenient modular cinterop mode
- [`KT-83814`](https://youtrack.jetbrains.com/issue/KT-83814) Native: includedHeaders= in platform libs manifests is not reproducible when modules= is used

### Native. Runtime. Memory

- [`KT-80770`](https://youtrack.jetbrains.com/issue/KT-80770) Kotlin/Native: revise ObjC refcount methods called in runnable state
- [`KT-84640`](https://youtrack.jetbrains.com/issue/KT-84640) Native: comment for `kotlin.native.runtime.SweepStatistics` misses the word "number"

### Native. Swift Export

#### New Features

- [`KT-82598`](https://youtrack.jetbrains.com/issue/KT-82598) Swift Export: Custom name translation
- [`KT-66821`](https://youtrack.jetbrains.com/issue/KT-66821) Swift Export: value class
- [`KT-84263`](https://youtrack.jetbrains.com/issue/KT-84263) [Swift Export] Context Parameters on Functional Types
- [`KT-69431`](https://youtrack.jetbrains.com/issue/KT-69431) Swift export: inline functions

#### Fixes

- [`KT-81593`](https://youtrack.jetbrains.com/issue/KT-81593) Swift Export: suspend function returning Unit leads to incompilable code
- [`KT-84359`](https://youtrack.jetbrains.com/issue/KT-84359) [Swift Export] nested functional type with Unit parameter
- [`KT-84358`](https://youtrack.jetbrains.com/issue/KT-84358) [Swift Export] functional type with Unit parameter
- [`KT-84356`](https://youtrack.jetbrains.com/issue/KT-84356) [Swift Export] functional type with single Unit parameter
- [`KT-83567`](https://youtrack.jetbrains.com/issue/KT-83567) Swift Export: "IllegalStateException: Internal compiler error: doesn't correspond to any C type: kotlin.Unit": invalid closure is generated for suspend function which returns Unit
- [`KT-83397`](https://youtrack.jetbrains.com/issue/KT-83397) [Swift Export] Functional return type with Unit parameter is emitted as invalid void parameter list ('void' must be the first and only parameter)
- [`KT-83743`](https://youtrack.jetbrains.com/issue/KT-83743) Swift export: type arguments expected for generic typealias
- [`KT-84243`](https://youtrack.jetbrains.com/issue/KT-84243) [Swift Export] Returning value of suspending functional type from suspending function yields invalid code
- [`KT-82568`](https://youtrack.jetbrains.com/issue/KT-82568) Swift Export: Context Parameters
- [`KT-83398`](https://youtrack.jetbrains.com/issue/KT-83398) [Swift export] converting non-escaping parameter to generic parameter may allow it to escape
- [`KT-83389`](https://youtrack.jetbrains.com/issue/KT-83389) Swift Export: "ClassCastException" caused by suspend fun throwing Error
- [`KT-83116`](https://youtrack.jetbrains.com/issue/KT-83116) Swift export generates bridges incompatible with language version 2.4
- [`KT-83749`](https://youtrack.jetbrains.com/issue/KT-83749) [Swift Export] varargs and List uses the same mangling on bridges
- [`KT-83712`](https://youtrack.jetbrains.com/issue/KT-83712) Swift Export ignores internal setter and generates invalid bridge code

### Tools. BCV

- [`KT-78341`](https://youtrack.jetbrains.com/issue/KT-78341) Outer scope's visibility is not considered when dumping const vals [ABI Validation JVM]
- [`KT-78305`](https://youtrack.jetbrains.com/issue/KT-78305) Private constructor is written in ABI dump
- [`KT-82724`](https://youtrack.jetbrains.com/issue/KT-82724) BCV incorrectly reports generated `@JvmOverloads` declarations as public
- [`KT-78367`](https://youtrack.jetbrains.com/issue/KT-78367) Internal constructor infiltrated into a dump
- [`KT-78366`](https://youtrack.jetbrains.com/issue/KT-78366) Protected method of enum should not be included into a dump

### Tools. Build Tools API

#### New Features

- [`KT-80963`](https://youtrack.jetbrains.com/issue/KT-80963) BTA: Add structured information about reported messages to KotlinLogger
- [`KT-84453`](https://youtrack.jetbrains.com/issue/KT-84453) SSoT: provide a unified way to convert Enums to Strings

#### Fixes

- [`KT-82335`](https://youtrack.jetbrains.com/issue/KT-82335) Promote the deprecation level for BTA prototype to the ERROR level
- [`KT-84015`](https://youtrack.jetbrains.com/issue/KT-84015) Introduce detection of custom script names to new BTA API
- [`KT-83972`](https://youtrack.jetbrains.com/issue/KT-83972) BTA: use isolated classloader for loading the BTA implementation in integration tests
- [`KT-84906`](https://youtrack.jetbrains.com/issue/KT-84906) Make enum-based common arguments type-safe
- [`KT-75837`](https://youtrack.jetbrains.com/issue/KT-75837) IC: Shrunk classpath snapshot name is hardcoded
- [`KT-84867`](https://youtrack.jetbrains.com/issue/KT-84867) Make Xphases-to-* arguments type-safe
- [`KT-84850`](https://youtrack.jetbrains.com/issue/KT-84850) Make kotlin-home type safe
- [`KT-84825`](https://youtrack.jetbrains.com/issue/KT-84825) Make script-templates type safe
- [`KT-84546`](https://youtrack.jetbrains.com/issue/KT-84546) Replace raw string path arguments with type-safe PathListType
- [`KT-84705`](https://youtrack.jetbrains.com/issue/KT-84705) Make Xjdk-release to type-safe
- [`KT-84181`](https://youtrack.jetbrains.com/issue/KT-84181) More verbose warning when CRI is enabled without using BTA
- [`KT-84436`](https://youtrack.jetbrains.com/issue/KT-84436) Сompiler warnings are missing under Gradle -q option with -Werror
- [`KT-84324`](https://youtrack.jetbrains.com/issue/KT-84324) Make X_ADD_MODULES BTA compiler argument type safe
- [`KT-84338`](https://youtrack.jetbrains.com/issue/KT-84338) Make enum BTA JVM compiler argument type safe
- [`KT-84325`](https://youtrack.jetbrains.com/issue/KT-84325) Make JVM_DEFAULT BTA compiler argument type safe
- [`KT-84449`](https://youtrack.jetbrains.com/issue/KT-84449) Platform-Specific File.pathSeparator Hardcoded During SSOT Generation
- [`KT-84523`](https://youtrack.jetbrains.com/issue/KT-84523) Add more forward compatibility tests
- [`KT-84249`](https://youtrack.jetbrains.com/issue/KT-84249) Fix hardcoded path separator in -Xprofile argument to support absolute paths on Windows
- [`KT-84187`](https://youtrack.jetbrains.com/issue/KT-84187) [BTA] Add more build operation immutability tests
- [`KT-84219`](https://youtrack.jetbrains.com/issue/KT-84219) [BTA] Add additional tests on basic metrics collection
- [`KT-83781`](https://youtrack.jetbrains.com/issue/KT-83781) Add additional tests for KT-79975 (BTA ability to cancel build operations)

### Tools. CLI

- [`KT-84188`](https://youtrack.jetbrains.com/issue/KT-84188) Create CLI argument for explicit context parameters
- [`KT-84609`](https://youtrack.jetbrains.com/issue/KT-84609) Remove Nullability from Array-based CLI Compiler Arguments
- [`KT-84220`](https://youtrack.jetbrains.com/issue/KT-84220) Enable Context Parameters by default in LV 2.4
- [`KT-84132`](https://youtrack.jetbrains.com/issue/KT-84132) CLI: regression in deduplication of same-value arguments
- [`KT-83261`](https://youtrack.jetbrains.com/issue/KT-83261) No error if pass an arbitrary string to a CLI argument that changes language features
- [`KT-83172`](https://youtrack.jetbrains.com/issue/KT-83172) Boolean CLI argument for a language feature with explicit false value is allowed but has no effect
- [`KT-83341`](https://youtrack.jetbrains.com/issue/KT-83341) Don't use the extension point registration mechanism from Intellij for K2 extensions

### Tools. CLI. Native

- [`KT-82482`](https://youtrack.jetbrains.com/issue/KT-82482) Compiler plugins are not propagated to frontend environment in ONE_STAGE_MULTI_MODULE Native mode

### Tools. Compiler Plugins

- [`KT-66807`](https://youtrack.jetbrains.com/issue/KT-66807) PowerAssert: Improve output diagram formatting
- [`KT-75266`](https://youtrack.jetbrains.com/issue/KT-75266) PowerAssert: arrayOf() isn't displayed on the diagram
- [`KT-66808`](https://youtrack.jetbrains.com/issue/KT-66808) PowerAssert: Add support for third-party assertion libraries
- [`KT-67332`](https://youtrack.jetbrains.com/issue/KT-67332) "IndexOutOfBoundsException: Cannot pop operand off an empty stack." caused by function reference
- [`KT-83931`](https://youtrack.jetbrains.com/issue/KT-83931) Power Assert: Compilation fails when using the metro plugin
- [`KT-83330`](https://youtrack.jetbrains.com/issue/KT-83330) Lombok.  An add methods with `@Singular` annotation in Java record doesn't work from kotlin
- [`KT-83204`](https://youtrack.jetbrains.com/issue/KT-83204) Lombok. If `@Data` and `@NoArgsConstructor` are used together, then the constructor from `@Data` shouldn't be available
- [`KT-83336`](https://youtrack.jetbrains.com/issue/KT-83336) Lombok. IllegalAccessError for constructor if `@Value` and `@Builder` are applied and used from another package
- [`KT-83352`](https://youtrack.jetbrains.com/issue/KT-83352) Lombok. FileAnalysisException when `@SuperBuilder` is used with `@Builder`
- [`KT-83325`](https://youtrack.jetbrains.com/issue/KT-83325) Lombok. Constructor with parameters is unavailable for a class with `@Builder`

### Tools. Gradle

#### Fixes

- [`KT-74451`](https://youtrack.jetbrains.com/issue/KT-74451) Deprecate access to Kotlin source sets in Android extension
- [`KT-82847`](https://youtrack.jetbrains.com/issue/KT-82847) Raise deprecation to error for LanguageSettings.enableLanguageFeature DSL
- [`KT-84053`](https://youtrack.jetbrains.com/issue/KT-84053) Deprecate support for Gradle 7.6-8.13 versions
- [`KT-78659`](https://youtrack.jetbrains.com/issue/KT-78659) Remove 'kotlin-android-extensions' plugin id
- [`KT-79924`](https://youtrack.jetbrains.com/issue/KT-79924) Make enableKotlinToolingMetadataArtifact deprecated
- [`KT-82933`](https://youtrack.jetbrains.com/issue/KT-82933) Add a tab with results in TC
- [`KT-83130`](https://youtrack.jetbrains.com/issue/KT-83130) [ToolingDiagnostic] incorrect problem ID formatting for acronyms and undefined locations in Gradle8 problems reports
- [`KT-84144`](https://youtrack.jetbrains.com/issue/KT-84144) Bump the minimal supported AGP version to 8.5.2
- [`KT-84143`](https://youtrack.jetbrains.com/issue/KT-84143) Reduce usage of Project in Tooling Diagnostics
- [`KT-83126`](https://youtrack.jetbrains.com/issue/KT-83126) Remove out-of-process compilation mode
- [`KT-80466`](https://youtrack.jetbrains.com/issue/KT-80466) Gradle: remove getPluginArtifactForNative()
- [`KT-81834`](https://youtrack.jetbrains.com/issue/KT-81834) Compile against AGP 8.13 API
- [`KT-82960`](https://youtrack.jetbrains.com/issue/KT-82960) Remove deprecated enableKotlinToolingMetadataArtifact in 2.4.0
- [`KT-75004`](https://youtrack.jetbrains.com/issue/KT-75004) KGP: improve messaging when multiplatform tasks are disabled on incompatible OSes
- [`KT-77498`](https://youtrack.jetbrains.com/issue/KT-77498) Test .swiftmodules more accurate in SwiftExportIT
- [`KT-84377`](https://youtrack.jetbrains.com/issue/KT-84377) Broken package-list file on KGP/CMPG documentation page
- [`KT-84141`](https://youtrack.jetbrains.com/issue/KT-84141) Add convenient host check
- [`KT-83592`](https://youtrack.jetbrains.com/issue/KT-83592) Enable AFU in FusStatisticsIT.testKotlinxPlugins test after next AFU release
- [`KT-83775`](https://youtrack.jetbrains.com/issue/KT-83775) Migrate KGP functionalTest to junit5

### Tools. Gradle. BCV

- [`KT-83486`](https://youtrack.jetbrains.com/issue/KT-83486) Create tasks only if abiValidation block called explicitly [ABI Validation]
- [`KT-84365`](https://youtrack.jetbrains.com/issue/KT-84365) Gradle plugin of abi-validation should precisely define output files
- [`KT-80685`](https://youtrack.jetbrains.com/issue/KT-80685) Simplify Gradle DSL [ABI Validation]
- [`KT-82410`](https://youtrack.jetbrains.com/issue/KT-82410) Remove word `legacy` from  DSL [ABI Validation]
- [`KT-83898`](https://youtrack.jetbrains.com/issue/KT-83898) Classes produced by JvmMultifileClass ignore filters

### Tools. Gradle. Dokka

- [`KT-82984`](https://youtrack.jetbrains.com/issue/KT-82984) Support AGP9 in Dokka Gradle Plugin

### Tools. Gradle. JS

- [`KT-81036`](https://youtrack.jetbrains.com/issue/KT-81036) K/JS, Wasm: Remove deprecated ExperimentalDceDsl
- [`KT-64275`](https://youtrack.jetbrains.com/issue/KT-64275) Gradle: remove deprecated symbols related to the legacy JS target
- [`KT-81040`](https://youtrack.jetbrains.com/issue/KT-81040) Gradle: Remove deprecated Kotlin/JS tasks constructors

### Tools. Gradle. Multiplatform

- [`KT-82265`](https://youtrack.jetbrains.com/issue/KT-82265) Remove Android source set layout v1
- [`KT-82230`](https://youtrack.jetbrains.com/issue/KT-82230) Cleanup 'org.jetbrains.gradle.apple.applePlugin' plugin usage
- [`KT-81958`](https://youtrack.jetbrains.com/issue/KT-81958) Redundant “android target already exists” error when migrating to com.android.kotlin.multiplatform.library with androidTarget {}

### Tools. Gradle. Native

- [`KT-84558`](https://youtrack.jetbrains.com/issue/KT-84558) Upstream SwiftPM import work
- [`KT-84656`](https://youtrack.jetbrains.com/issue/KT-84656) Concurrent issue in downloadKotlinNativeDistribution
- [`KT-84508`](https://youtrack.jetbrains.com/issue/KT-84508) Add a warning on usage macos_x64 as host
- [`KT-84692`](https://youtrack.jetbrains.com/issue/KT-84692) Misleading error message for disableNativeCache DSL without required Opt-In
- [`KT-83680`](https://youtrack.jetbrains.com/issue/KT-83680) Remove trailing commas from the package manifest to be compatible with pre-16.3 Xcode

### Tools. Gradle. Wasm

- [`KT-83566`](https://youtrack.jetbrains.com/issue/KT-83566) K/Wasm: Support Wasm per module/klib compilation in Gradle plugin
- [`KT-84137`](https://youtrack.jetbrains.com/issue/KT-84137) K/Wasm: Support binaryen run with multiple files
- [`KT-84230`](https://youtrack.jetbrains.com/issue/KT-84230) Wasm: Fix test WasmYarnGradlePluginIT.testWasmUsePredefinedTooling

### Tools. Kapt

- [`KT-84094`](https://youtrack.jetbrains.com/issue/KT-84094) Kotlin daemon holds file locks for too long
- [`KT-80569`](https://youtrack.jetbrains.com/issue/KT-80569) K2 KAPT: Class Literals Missing in Explicit Annotation Value Parameters
- [`KT-18791`](https://youtrack.jetbrains.com/issue/KT-18791) Kapt: Constants from R class should not be inlined

### Tools. Maven

- [`KT-84793`](https://youtrack.jetbrains.com/issue/KT-84793) Use kotlin bootstrap to build kotlin-maven-plugin
- [`KT-83110`](https://youtrack.jetbrains.com/issue/KT-83110) Remove dependency to intellij platform from kotlin-maven-plugin-test
- [`KT-83113`](https://youtrack.jetbrains.com/issue/KT-83113) Configure kotlin.git/.idea to work nicely with maven-kotlin-plugin-test tests
- [`KT-83114`](https://youtrack.jetbrains.com/issue/KT-83114) Migrate kotlin-maven-plugin-test from maven.invoker to junit6 + maven-verifier

### Tools. Performance benchmarks

- [`KT-82928`](https://youtrack.jetbrains.com/issue/KT-82928) Support local run for new benchmarks infra
- [`KT-84283`](https://youtrack.jetbrains.com/issue/KT-84283) Add scenario generator for performance tests
- [`KT-83257`](https://youtrack.jetbrains.com/issue/KT-83257) Parse gradle profile report

### Tools. REPL

- [`KT-84160`](https://youtrack.jetbrains.com/issue/KT-84160) [REPL] Resolve eval function during implicit body
- [`KT-74683`](https://youtrack.jetbrains.com/issue/KT-74683) [K2 Repl] Does not support suspend functions
- [`KT-83689`](https://youtrack.jetbrains.com/issue/KT-83689) [K2 REPL] Create raw FIR tests for repl snippets
- [`KT-82554`](https://youtrack.jetbrains.com/issue/KT-82554) [REPL] Fix unresolved reference when using dataframe compiler-plugin
- [`KT-82578`](https://youtrack.jetbrains.com/issue/KT-82578) [K2 REPL] Split snippet property declaration and initialization
- [`KT-82503`](https://youtrack.jetbrains.com/issue/KT-82503) [K2 Repl] Nested class annotations are not available in the next snippet

### Tools. Wasm

- [`KT-84396`](https://youtrack.jetbrains.com/issue/KT-84396) [Wasm] Support multimodule  in incremental compilation
