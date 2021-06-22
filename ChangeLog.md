# CHANGELOG

## 1.5.20

### Compiler

#### New Features

- [`KT-43262`](https://youtrack.jetbrains.com/issue/KT-43262) No error for Java generic class @NotNull type parameter used in Kotlin with nullable type argument
- [`KT-44373`](https://youtrack.jetbrains.com/issue/KT-44373) FIR: support error / warning suppression
- [`KT-45189`](https://youtrack.jetbrains.com/issue/KT-45189) Support nullability annotations at module level
- [`KT-45284`](https://youtrack.jetbrains.com/issue/KT-45284) Emit warnings based on jspecify annotations
- [`KT-45525`](https://youtrack.jetbrains.com/issue/KT-45525) Allow to omit JvmInline annotation for expect value classes
- [`KT-46545`](https://youtrack.jetbrains.com/issue/KT-46545) Emit annotations on function type parameters into bytecode for -jvm-target 1.8 and above

#### Performance Improvements

- [`KT-36646`](https://youtrack.jetbrains.com/issue/KT-36646) Don't box primitive values in equality comparison with objects in JVM_IR

#### Fixes

- [`KT-8325`](https://youtrack.jetbrains.com/issue/KT-8325) Unresolved annotation should be an error
- [`KT-19455`](https://youtrack.jetbrains.com/issue/KT-19455) Type annotation unresolved on a type parameter of a supertype in anonymous object expression
- [`KT-24643`](https://youtrack.jetbrains.com/issue/KT-24643) Prohibit using a type parameter declared for an extension property inside delegate
- [`KT-25876`](https://youtrack.jetbrains.com/issue/KT-25876) Annotations on return types and supertypes are not analyzed
- [`KT-28449`](https://youtrack.jetbrains.com/issue/KT-28449) Annotation target is not analyzed in several cases for type annotations
- [`KT-36770`](https://youtrack.jetbrains.com/issue/KT-36770) Prohibit unsafe calls with expected @NotNull T and given Kotlin generic parameter with nullable bound
- [`KT-36880`](https://youtrack.jetbrains.com/issue/KT-36880) K/N IR: Reference to expect property in actual declaration is not remapped
- [`KT-38325`](https://youtrack.jetbrains.com/issue/KT-38325) IllegalStateException: No parameter with index 0-0 when iterating Scala 2.12.11 List
- [`KT-38342`](https://youtrack.jetbrains.com/issue/KT-38342) FIR: Consider renaming diagnostic from AMBIGUITY to OVERLOAD_RESOLUTION_AMBIGUITY
- [`KT-38476`](https://youtrack.jetbrains.com/issue/KT-38476) [FIR] Forgotten type approximation
- [`KT-38540`](https://youtrack.jetbrains.com/issue/KT-38540) Kotlin/Native Set<T>.contains fails with specific enum setup
- [`KT-40425`](https://youtrack.jetbrains.com/issue/KT-40425) IrGenerationExtension. Support simple reporting to compiler output (for development/debug)
- [`KT-41620`](https://youtrack.jetbrains.com/issue/KT-41620) ClassCastException: Class cannot be cast to java.lang.Void
- [`KT-41679`](https://youtrack.jetbrains.com/issue/KT-41679) NI: TYPE_MISMATCH wrong type inference of collection with type Any and integer literal
- [`KT-41818`](https://youtrack.jetbrains.com/issue/KT-41818) NI: False positive IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION leads to NothingValueException on delegated properties
- [`KT-42239`](https://youtrack.jetbrains.com/issue/KT-42239) IR: Report compilation error instead of throwing an exception (effectively crash compiler) when some declaration wasn't found while deserialization
- [`KT-42631`](https://youtrack.jetbrains.com/issue/KT-42631) ArrayIndexOutOfBoundsException was thrown during IR lowering
- [`KT-43258`](https://youtrack.jetbrains.com/issue/KT-43258) NI: False positive "Suspend function 'invoke' should be called only from a coroutine or another suspend function" when calling suspend operator fun on object property from last expression of a crossinlined suspend lambda
- [`KT-44036`](https://youtrack.jetbrains.com/issue/KT-44036) Enum initialization order
- [`KT-44511`](https://youtrack.jetbrains.com/issue/KT-44511) FIR DFA: smartcast after `if (nullable ?: boolean)`
- [`KT-44554`](https://youtrack.jetbrains.com/issue/KT-44554) RAW FIR: NPE in RawFirBuilder
- [`KT-44682`](https://youtrack.jetbrains.com/issue/KT-44682) raw FIR: incorrect source for qualified access
- [`KT-44695`](https://youtrack.jetbrains.com/issue/KT-44695) *_TYPE_MISMATCH_ON_OVERRIDE checkers do not work for anonymous objects
- [`KT-44699`](https://youtrack.jetbrains.com/issue/KT-44699) FIR: incorrect lambda return type (led to a false alarm: PROPERTY_TYPE_MISMATCH_ON_OVERRIDE)
- [`KT-44802`](https://youtrack.jetbrains.com/issue/KT-44802) FIR bootstrap: trying to access package private class
- [`KT-44813`](https://youtrack.jetbrains.com/issue/KT-44813) FIR bootstrap: various errors in collection-like classes
- [`KT-44814`](https://youtrack.jetbrains.com/issue/KT-44814) FIR bootstrap: incorrect cast in when branch
- [`KT-44942`](https://youtrack.jetbrains.com/issue/KT-44942) [FIR] ClassCastException in boostrap tests
- [`KT-44995`](https://youtrack.jetbrains.com/issue/KT-44995) FIR: false positive for ANNOTATION_ARGUMENT_MUST_BE_CONST
- [`KT-45010`](https://youtrack.jetbrains.com/issue/KT-45010) FIR: lambda arguments of inapplicable call is not resolved
- [`KT-45048`](https://youtrack.jetbrains.com/issue/KT-45048) FIR bootstrap: VerifyError on KtUltraLightClass
- [`KT-45052`](https://youtrack.jetbrains.com/issue/KT-45052) FIR bootstrap: inapplicable candidate in GenerateSpecTests.kt
- [`KT-45121`](https://youtrack.jetbrains.com/issue/KT-45121) FIR IDE: redundant vararg parameter type transformation
- [`KT-45136`](https://youtrack.jetbrains.com/issue/KT-45136) Native: dividing Int.MIN_VALUE by -1 crashes or hangs
- [`KT-45236`](https://youtrack.jetbrains.com/issue/KT-45236) JVM / IR: "IllegalStateException: Symbol with IrTypeParameterSymbolImpl is unbound" caused by contracts and sealed class
- [`KT-45308`](https://youtrack.jetbrains.com/issue/KT-45308) Psi2ir: "AssertionError: TypeAliasDescriptor expected" caused by using typealias from one module as a type in another module without a transitive dependency
- [`KT-45316`](https://youtrack.jetbrains.com/issue/KT-45316) [FIR] Ambiguity between two implicit invokes with receiver
- [`KT-45344`](https://youtrack.jetbrains.com/issue/KT-45344) FIR: Wrong inferred type for nullable type parameter
- [`KT-45385`](https://youtrack.jetbrains.com/issue/KT-45385) FIR: false positive MUST_BE_INITIALIZED_OR_BE_ABSTRACT after rethrow
- [`KT-45475`](https://youtrack.jetbrains.com/issue/KT-45475) [FIR] No smartcast after throw in if inside try block
- [`KT-45508`](https://youtrack.jetbrains.com/issue/KT-45508) False negative ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED on a fake override with an abstract super class member
- [`KT-45578`](https://youtrack.jetbrains.com/issue/KT-45578) REPL: Unresolved imports are cached for the subsequent compilations
- [`KT-45685`](https://youtrack.jetbrains.com/issue/KT-45685) JVM IR: capturing a variable into crossinline suspend lambda makes the function inside inline function no longer unbox Result
- [`KT-45584`](https://youtrack.jetbrains.com/issue/KT-45584) [FIR] Fix overriding property and java function in java class
- [`KT-45697`](https://youtrack.jetbrains.com/issue/KT-45697) JVM IR: ISE "Function has no body" on getter of private field in a class present both in sources and dependencies
- [`KT-45842`](https://youtrack.jetbrains.com/issue/KT-45842) Compiler doesn't allow a shared class to inherit a platform-specific sealed class
- [`KT-45848`](https://youtrack.jetbrains.com/issue/KT-45848) False negative [SEALED_INHERITOR_IN_DIFFERENT_MODULE] error in compiler for a platform-specific inheritor of a shared sealed class
- [`KT-45931`](https://youtrack.jetbrains.com/issue/KT-45931) There is no warning based on nullability java annotation
- [`KT-45998`](https://youtrack.jetbrains.com/issue/KT-45998) JVM IR: AE when an accessor to a protected companion object member is being generated in child class
- [`KT-46048`](https://youtrack.jetbrains.com/issue/KT-46048) Enum entries init order in companion object
- [`KT-46074`](https://youtrack.jetbrains.com/issue/KT-46074) FIR: private-in-file fun interface is considered invisible in this file
- [`KT-46173`](https://youtrack.jetbrains.com/issue/KT-46173) No error reporting on annotations on target type of `as` expression in return
- [`KT-46235`](https://youtrack.jetbrains.com/issue/KT-46235) JVM IR: Stack overflow error on large expressions
- [`KT-46270`](https://youtrack.jetbrains.com/issue/KT-46270) [FIR] Support `@PublishedAPI` in inline checker
- [`KT-46539`](https://youtrack.jetbrains.com/issue/KT-46539) Generate annotations on type parameters bounds in bytecode
- [`KT-46578`](https://youtrack.jetbrains.com/issue/KT-46578) JVM IR: IllegalAccessError accessing property delegated to java super class protected field reference
- [`KT-46597`](https://youtrack.jetbrains.com/issue/KT-46597) JVM IR: AssertionError: SyntheticAccessorLowering should not attempt to modify other files - crossinline accessor
- [`KT-46601`](https://youtrack.jetbrains.com/issue/KT-46601) JVM / IR: IllegalStateException: "Can't find method 'invokeinvoke`" when default lambda takes inline class parameters
- [`KT-46670`](https://youtrack.jetbrains.com/issue/KT-46670) StackOverflowError on inheritance from raw type where class has protobuf-like recursive generics
- [`KT-46715`](https://youtrack.jetbrains.com/issue/KT-46715) JVM / IR: "AssertionError: Unbound symbols not allowed IrConstructorSymbolImpl" with enum classes with the same name in test and src folders
- [`KT-46759`](https://youtrack.jetbrains.com/issue/KT-46759) JVM IR: CCE in LateinitUsageLowering on @JvmStatic lateinit property in object
- [`KT-46777`](https://youtrack.jetbrains.com/issue/KT-46777) [Native] [IR] Support suspend function as super type
- [`KT-46802`](https://youtrack.jetbrains.com/issue/KT-46802) JVM / IR: "UnsupportedOperationException: Unknown structure of ADAPTER_FOR_CALLABLE_REFERENCE" caused by function reference on @JvmStatic function with unused default parameters
- [`KT-46813`](https://youtrack.jetbrains.com/issue/KT-46813) JVM / IR: "ClassCastException: Integer cannot be cast to class Result" with Flow and `fold` method
- [`KT-46822`](https://youtrack.jetbrains.com/issue/KT-46822) JVM IR: StackOverflowError on compiling a large data class
- [`KT-46837`](https://youtrack.jetbrains.com/issue/KT-46837) Backend Internal error: Exception during IR lowering: assert at IrOverridingUtilKt.buildFakeOverrideMember
- [`KT-46921`](https://youtrack.jetbrains.com/issue/KT-46921) JVM / IR: "IndexOutOfBoundsException: Cannot pop operand off an empty stack" caused by crossinline parameter and label return
- [`KT-46984`](https://youtrack.jetbrains.com/issue/KT-46984) Type parameter bounds aren't used to report corresponding mismatch warnings
- [`KT-46985`](https://youtrack.jetbrains.com/issue/KT-46985) There aren't warnings by java nullability annotations
- [`KT-46986`](https://youtrack.jetbrains.com/issue/KT-46986) There aren't warnings by java nullability annotations
- [`KT-46989`](https://youtrack.jetbrains.com/issue/KT-46989) There aren't warnings by java nullability annotations
- [`KT-46990`](https://youtrack.jetbrains.com/issue/KT-46990) There aren't warnings by java nullability annotations on method's violated type arguments
- [`KT-47019`](https://youtrack.jetbrains.com/issue/KT-47019) K/N: IrProperty.overriddenSymbols can't be used in common IR backend modules yet because it doesn't fully work in Native

### Docs & Examples

- [`KT-33783`](https://youtrack.jetbrains.com/issue/KT-33783) Document when a range created with rangeTo is empty

### IDE

- [`KT-44638`](https://youtrack.jetbrains.com/issue/KT-44638) `clone()` call is unresolved in JVM module of a multiplatform project
- [`KT-45629`](https://youtrack.jetbrains.com/issue/KT-45629) [ULC] KtUltraLightFieldForSourceDeclaration.nameIdentifier returns null
- [`KT-44825`](https://youtrack.jetbrains.com/issue/KT-44825) Can't open Kotlin compiler settings in newly created project
- [`KT-45908`](https://youtrack.jetbrains.com/issue/KT-45908) Reproduciable 'org.jetbrains.kotlin.idea.caches.resolve.KotlinIdeaResolutionException: Kotlin resolution encountered a problem while analyzing KtNameReferenceExpression'

### IDE. FIR

- [`KT-45175`](https://youtrack.jetbrains.com/issue/KT-45175) FIR IDE: Exception with local property in `init` block
- [`KT-45199`](https://youtrack.jetbrains.com/issue/KT-45199) FIR IDE: Error while collecting diagnostic on stale element after replacing element in quickfix
- [`KT-45312`](https://youtrack.jetbrains.com/issue/KT-45312) FIR IDE: FIR plugin throws exception on synthetic function

### IDE. Gradle Integration

- [`KT-34401`](https://youtrack.jetbrains.com/issue/KT-34401) KotlinGradleModelBuilder builds models for non-kotlin modules and always trigger full task configuration.
- [`KT-45277`](https://youtrack.jetbrains.com/issue/KT-45277) Wrong jvm target in gradle module in IDEA
- [`KT-46488`](https://youtrack.jetbrains.com/issue/KT-46488) Import of a multiplatform project with MPP module depending on Kotlin/JVM one fails

### IDE. Inspections and Intentions

- [`KT-45075`](https://youtrack.jetbrains.com/issue/KT-45075) Inspection: Redundant creation of Json format
- [`KT-45347`](https://youtrack.jetbrains.com/issue/KT-45347) Sealed interfaces: quickfix to move to package/module of sealed class/interface should not be shown in case of read-only declaration
- [`KT-45348`](https://youtrack.jetbrains.com/issue/KT-45348) Sealed interfaces: show error for usage of sealed class/interface from a library in Java source code
- [`KT-46063`](https://youtrack.jetbrains.com/issue/KT-46063) In multiplatform code, suggest to generate remaining `when` branches at least for shared sealed classes

### IDE. Refactorings

- [`KT-44431`](https://youtrack.jetbrains.com/issue/KT-44431) Quickfix to move class/interface to proper location: it is allowed to choose test source in JPS project while compiler does not allow it

### IDE. Native

- [`KT-39320`](https://youtrack.jetbrains.com/issue/KT-39320) [Commonizer] Reduce memory consumption

### JavaScript

- [`KT-40235`](https://youtrack.jetbrains.com/issue/KT-40235) KJS: IR. Broken support for external interface companion
- [`KT-40689`](https://youtrack.jetbrains.com/issue/KT-40689) KJS / IR: strange and slow code for kotlin.math.max and kotlin.math.min for Double
- [`KT-44138`](https://youtrack.jetbrains.com/issue/KT-44138) KJS / IR: Constant folding works incorrectly with unsigned arithmetic
- [`KT-44394`](https://youtrack.jetbrains.com/issue/KT-44394) KJS / IR: `null` companion object for existed stdlib interfaces `NodeFilter` and `SVGUnitTypes`
- [`KT-44950`](https://youtrack.jetbrains.com/issue/KT-44950) KJS / IR: "IllegalStateException: Can't find name for declaration" in case of extending export declared class without @JsExport annotation
- [`KT-45057`](https://youtrack.jetbrains.com/issue/KT-45057) KJS / IR: "ClassCastException" when using `js` function in init block
- [`KT-45361`](https://youtrack.jetbrains.com/issue/KT-45361) KJS / IR: `IrConstructorCall` representing annotation always returns `Unit`
- [`KT-46608`](https://youtrack.jetbrains.com/issue/KT-46608) KJS: "Could not load content..." for source maps
- [`KT-45655`](https://youtrack.jetbrains.com/issue/KT-45655) KJS: "REINTERPRET_CAST" is not copyable
- [`KT-45866`](https://youtrack.jetbrains.com/issue/KT-45866) Default parameter with generic in expect-actual declarations
- [`KT-46859`](https://youtrack.jetbrains.com/issue/KT-46859) Exception during IR lowering: NullPointerException was thrown at: optimizations.FoldConstantLowering.tryFoldingUnaryOps

### KMM Plugin

- [`KT-43899`](https://youtrack.jetbrains.com/issue/KT-43899) KMM: Fix "stale framework" usage by XCode & AppCode in default build script

### Libraries

- [`KT-43701`](https://youtrack.jetbrains.com/issue/KT-43701) Stdlib: Expand KDoc of inc() and dec() for operators
- [`KT-46002`](https://youtrack.jetbrains.com/issue/KT-46002) Support all Unicode digit chars in digitToInt (JS and Native)
- [`KT-46183`](https://youtrack.jetbrains.com/issue/KT-46183) Add default value for ignoreCase in K/N String.replace/replaceFirst
- [`KT-46184`](https://youtrack.jetbrains.com/issue/KT-46184) Equivalize isLowerCase and isUpperCase behavior in all platforms

### Native

- [`KT-33175`](https://youtrack.jetbrains.com/issue/KT-33175) IR: String constants with incorrect surrogate pairs aren't preserved during serialization/deserialization
- [`KT-44799`](https://youtrack.jetbrains.com/issue/KT-44799) Different behavior with functional interfaces in Kotlin/Native on iOS

### Native. C Export

- [`KT-42796`](https://youtrack.jetbrains.com/issue/KT-42796) [Reverse C Interop] Package with no public methods generate empty struct in the header, leading to an error

### Native. ObjC Export

- [`KT-38600`](https://youtrack.jetbrains.com/issue/KT-38600) Kotlin MP iOS Target doesn't contain kdoc comments
- [`KT-45127`](https://youtrack.jetbrains.com/issue/KT-45127) KMM: hard to pass an error to Kotlin code from implementation of Kotlin method in Swift code

### Native. Runtime. Memory

- [`KT-45063`](https://youtrack.jetbrains.com/issue/KT-45063) Profiling indicates that a lot of time is spent on updateHeapRef on Apple platforms when running KMP code

### Reflection

- [`KT-10838`](https://youtrack.jetbrains.com/issue/KT-10838) Provide sensible toString() for property accessors in reflection
- [`KT-13490`](https://youtrack.jetbrains.com/issue/KT-13490) Equality doesn't work for KProperty.Accessor implementations

### Tools. CLI

- [`KT-14772`](https://youtrack.jetbrains.com/issue/KT-14772) ISE (FNFE "Not a directory") on compilation with destination argument clashing with an existing file which is not a directory
- [`KT-18184`](https://youtrack.jetbrains.com/issue/KT-18184) CompileEnvironmentException: Invalid jar path on "-d" with .jar in non-existing directory
- [`KT-40977`](https://youtrack.jetbrains.com/issue/KT-40977) Report a readable diagnostic on empty -J argument in CLI

### Tools. Commonizer

- [`KT-45497`](https://youtrack.jetbrains.com/issue/KT-45497) [Commonizer] c-interop commonization: Dependency commonization
- [`KT-46077`](https://youtrack.jetbrains.com/issue/KT-46077) [Commonizer] Add `commonizer_target` to commonized klib's manifest
- [`KT-46107`](https://youtrack.jetbrains.com/issue/KT-46107) [Commonizer] CInteropCommonizerTask receives faulty dependencies in multi module projects containing multiple c-interops
- [`KT-46248`](https://youtrack.jetbrains.com/issue/KT-46248) MPP: Compile KotlinMetadata fails with Unresolved reference if only one native platform from shared source set is available
- [`KT-46856`](https://youtrack.jetbrains.com/issue/KT-46856) [Commonizer] Many targets can fail with 'filename too long'

### Tools. Compiler Plugins

- [`KT-7112`](https://youtrack.jetbrains.com/issue/KT-7112) Support calling Lombok-generated methods within same module
- [`KT-45538`](https://youtrack.jetbrains.com/issue/KT-45538) Serialization, JVM IR: "AssertionError: No such type argument slot in IrConstructorCallImpl" with inner classes
- [`KT-45541`](https://youtrack.jetbrains.com/issue/KT-45541) JVM / IR / Serialization: NullPointerException caused by "Serializable" annotation and local data class
- [`KT-46469`](https://youtrack.jetbrains.com/issue/KT-46469) Kotlin Lombok: accessors with `AccessLevel.MODULE` fail to resolve
- [`KT-46529`](https://youtrack.jetbrains.com/issue/KT-46529) Kotlin Lombok: with `@Accessors` without explicit `prefix` the prefix from lombok.config is not taken into account
- [`KT-46531`](https://youtrack.jetbrains.com/issue/KT-46531) Kotlin Lombok: `lombok.getter.noIsPrefix` is processed depending on character case
- [`KT-46920`](https://youtrack.jetbrains.com/issue/KT-46920) NullPointerException in CodeGeneratorVisitor when packing for xcode

### Tools. Gradle

- [`KT-24533`](https://youtrack.jetbrains.com/issue/KT-24533) Kapt should not run when annotation processors are not configured
- [`KT-43988`](https://youtrack.jetbrains.com/issue/KT-43988) Enable plugin validation during build
- [`KT-45301`](https://youtrack.jetbrains.com/issue/KT-45301) Gradle: Empty `build/kotlin` dir with custom build directory
- [`KT-45519`](https://youtrack.jetbrains.com/issue/KT-45519) loadAndroidPluginVersion() impacts performance negatively and noticeably in multimodule Android build
- [`KT-45744`](https://youtrack.jetbrains.com/issue/KT-45744) Create Kotlin Gradle Plugin JUnit5 basic test setup
- [`KT-45834`](https://youtrack.jetbrains.com/issue/KT-45834) Gradle Plugin read system property related to kotlinCompilerClasspath breaks use of configuration cache
- [`KT-46401`](https://youtrack.jetbrains.com/issue/KT-46401) Deprecate 'kotlin.parallel.tasks.in.project' build property
- [`KT-46820`](https://youtrack.jetbrains.com/issue/KT-46820) Gradle: kotlinc (1.5.0) race condition causes a NullPointerException
- [`KT-47317`](https://youtrack.jetbrains.com/issue/KT-47317) Restore 'kotlinPluginVersion' property in 'KotlinBasePluginWrapper'

### Tools. Gradle. JS

- [`KT-42911`](https://youtrack.jetbrains.com/issue/KT-42911) Support Gradle configuration cache for K/JS tasks
- [`KT-45294`](https://youtrack.jetbrains.com/issue/KT-45294) KJS / Gradle: Number of modules in project affects JS tasks configuration cache state size
- [`KT-45754`](https://youtrack.jetbrains.com/issue/KT-45754) KJS / IR: Remove adding option of source maps in Gradle plugin
- [`KT-46178`](https://youtrack.jetbrains.com/issue/KT-46178) KJS / Dukat: Added as a dependency always without condition
- [`KT-46976`](https://youtrack.jetbrains.com/issue/KT-46976) KJS: Broken support for dynamically created `webpack.config.d`
- [`KT-47045`](https://youtrack.jetbrains.com/issue/KT-47045) [Gradle, JS] Task requirements are added to all compilations with same name

### Tools. Gradle. Multiplatform

- [`KT-36679`](https://youtrack.jetbrains.com/issue/KT-36679) MPP Gradle plugin: Improve messaging for the commonizer
- [`KT-45832`](https://youtrack.jetbrains.com/issue/KT-45832) CInteropCommonization: Filter out illegal dependencies
- [`KT-46394`](https://youtrack.jetbrains.com/issue/KT-46394) Multiplatform: Gradle 7 support
- [`KT-46517`](https://youtrack.jetbrains.com/issue/KT-46517) Add kotlin-project-model as api dependency to kotlin-gradle-plugin-api

### Tools. Gradle. Native

- [`KT-27240`](https://youtrack.jetbrains.com/issue/KT-27240) MPP Gradle plugin: Provide a framework packing task for Kotlin/Native
- [`KT-39016`](https://youtrack.jetbrains.com/issue/KT-39016) Missing stdlib when the downloading process was aborted
- [`KT-40907`](https://youtrack.jetbrains.com/issue/KT-40907) Xcode error after switching between device and simulator: Building for iOS, but the linked and embedded framework was built for iOS Simulator.
- [`KT-44059`](https://youtrack.jetbrains.com/issue/KT-44059) iosSimTest tasks are never up-to-date
- [`KT-45801`](https://youtrack.jetbrains.com/issue/KT-45801) compileIosMainKotlinMetadata compilation property of gradle task is not initialized and fails with  `Execution failed for task ':shared:generateProjectStructureMetadata'`
- [`KT-46680`](https://youtrack.jetbrains.com/issue/KT-46680) Register concrete "embedAndSign" tasks instead umbrella
- [`KT-46892`](https://youtrack.jetbrains.com/issue/KT-46892) Kotlin Multiplatform Gradle Plugin: EmbedAndSign task always contains a default framework name

### Tools. Incremental Compile

- [`KT-44741`](https://youtrack.jetbrains.com/issue/KT-44741) Incremental compilation: inspectClassesForKotlinIC doesn't determine changes with imported constant

### Tools. JPS

- [`KT-34351`](https://youtrack.jetbrains.com/issue/KT-34351) KotlinTargetsIndex creation takes too long even if project doesn't have any kotlin
- [`KT-45191`](https://youtrack.jetbrains.com/issue/KT-45191) [JPS] Marking method as "default" in Java SAM interface doesn't affect dependencies
- [`KT-46242`](https://youtrack.jetbrains.com/issue/KT-46242) Support Lombok kotlin plugin in JPS and maven

### Tools. kapt

#### Performance Improvements

- [`KT-28901`](https://youtrack.jetbrains.com/issue/KT-28901) Consider caching annotation processors classloaders

#### Fixes

- [`KT-27123`](https://youtrack.jetbrains.com/issue/KT-27123) kapt: missing space in error log makes location non-clickable
- [`KT-29929`](https://youtrack.jetbrains.com/issue/KT-29929) [Kapt] Stub generator uses constant value in method annotation instead of constant name 2.
- [`KT-31146`](https://youtrack.jetbrains.com/issue/KT-31146) kapt: executableElement.getAnnotation(JvmOverloads::class.java) returns null
- [`KT-32202`](https://youtrack.jetbrains.com/issue/KT-32202) Gradle task kaptKotlin fails: "module not found" in Java 11 modular application
- [`KT-34838`](https://youtrack.jetbrains.com/issue/KT-34838) Kapt: 'cannot find symbol' for a top-level property with anonymous delegate
- [`KT-35104`](https://youtrack.jetbrains.com/issue/KT-35104) Support @JvmStatic in KAPT stubs
- [`KT-35167`](https://youtrack.jetbrains.com/issue/KT-35167) Kapt Gradle plugin doesn't handle --module-path javac argument
- [`KT-37586`](https://youtrack.jetbrains.com/issue/KT-37586) KAPT: When delegated property use an unknown type (to-be-generated class), `correctTypeError` will mess up the `$delegate` field type
- [`KT-39060`](https://youtrack.jetbrains.com/issue/KT-39060) Kapt: correctErrorTypes don't retain return type of getter in Java stub
- [`KT-39715`](https://youtrack.jetbrains.com/issue/KT-39715) KaptGenerateStubsTask resolves annotation processor options too early (before execution time)
- [`KT-41581`](https://youtrack.jetbrains.com/issue/KT-41581) Kapt doesn't have line breaks between warnings
- [`KT-43804`](https://youtrack.jetbrains.com/issue/KT-43804) Kapt fails to preserve parameter names in open suspend functions
- [`KT-43686`](https://youtrack.jetbrains.com/issue/KT-43686) KaptWithoutKotlincTask should use `@CompileClasspath` for `kotlinStdlibClasspath` for cache relocateability.
- [`KT-45032`](https://youtrack.jetbrains.com/issue/KT-45032) Kapt: NullPointerException: insnList.first must not be null
- [`KT-46176`](https://youtrack.jetbrains.com/issue/KT-46176) Kapt: "java.lang.IndexOutOfBoundsException: Index -1 out of bounds for length 3" with delegation and property reference

## 1.5.20-RC

### Backend. IR

- [`KT-42239`](https://youtrack.jetbrains.com/issue/KT-42239) IR: Report compilation error instead of throwing an exception (effectively crash compiler) when some declaration wasn't found while deserialization

### Compiler

#### New Features

- [`KT-44373`](https://youtrack.jetbrains.com/issue/KT-44373) FIR: support error / warning suppression

#### Fixes

- [`KT-38342`](https://youtrack.jetbrains.com/issue/KT-38342) FIR: Consider renaming diagnostic from AMBIGUITY to OVERLOAD_RESOLUTION_AMBIGUITY
- [`KT-38476`](https://youtrack.jetbrains.com/issue/KT-38476) [FIR] Forgotten type approximation
- [`KT-44682`](https://youtrack.jetbrains.com/issue/KT-44682) raw FIR: incorrect source for qualified access
- [`KT-44813`](https://youtrack.jetbrains.com/issue/KT-44813) FIR bootstrap: various errors in collection-like classes
- [`KT-45508`](https://youtrack.jetbrains.com/issue/KT-45508) False negative ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED on a fake override with an abstract super class member
- [`KT-45578`](https://youtrack.jetbrains.com/issue/KT-45578) REPL: Unresolved imports are cached for the subsequent compilations
- [`KT-45685`](https://youtrack.jetbrains.com/issue/KT-45685) JVM IR: capturing a variable into crossinline suspend lambda makes the function inside inline function no longer unbox Result
- [`KT-46235`](https://youtrack.jetbrains.com/issue/KT-46235) JVM IR: Stack overflow error on large expressions
- [`KT-46578`](https://youtrack.jetbrains.com/issue/KT-46578) JVM IR: IllegalAccessError accessing property delegated to java super class protected field reference
- [`KT-46597`](https://youtrack.jetbrains.com/issue/KT-46597) JVM IR: AssertionError: SyntheticAccessorLowering should not attempt to modify other files - crossinline accessor
- [`KT-46601`](https://youtrack.jetbrains.com/issue/KT-46601) JVM / IR: IllegalStateException: "Can't find method 'invokeinvoke`" when default lambda takes inline class parameters
- [`KT-46759`](https://youtrack.jetbrains.com/issue/KT-46759) JVM IR: CCE in LateinitUsageLowering on @JvmStatic lateinit property in object
- [`KT-46777`](https://youtrack.jetbrains.com/issue/KT-46777) [Native] [IR] Support suspend function as super type
- [`KT-46802`](https://youtrack.jetbrains.com/issue/KT-46802) JVM / IR: "UnsupportedOperationException: Unknown structure of ADAPTER_FOR_CALLABLE_REFERENCE" caused by function reference on @JvmStatic function with unused default parameters
- [`KT-46813`](https://youtrack.jetbrains.com/issue/KT-46813) JVM / IR: "ClassCastException: Integer cannot be cast to class Result" with Flow and `fold` method
- [`KT-46822`](https://youtrack.jetbrains.com/issue/KT-46822) JVM IR: StackOverflowError on compiling a large data class
- [`KT-46837`](https://youtrack.jetbrains.com/issue/KT-46837) Backend Internal error: Exception during IR lowering: assert at IrOverridingUtilKt.buildFakeOverrideMember
- [`KT-46921`](https://youtrack.jetbrains.com/issue/KT-46921) JVM / IR: "IndexOutOfBoundsException: Cannot pop operand off an empty stack" caused by crossinline parameter and label return

### Docs & Examples

- [`KT-33783`](https://youtrack.jetbrains.com/issue/KT-33783) Document when a range created with rangeTo is empty

### IDE

- [`KT-44825`](https://youtrack.jetbrains.com/issue/KT-44825) Can't open Kotlin compiler settings in newly created project

### IDE. Gradle Integration

- [`KT-45277`](https://youtrack.jetbrains.com/issue/KT-45277) Wrong jvm target in gradle module in IDEA

### IDE. Native

- [`KT-39320`](https://youtrack.jetbrains.com/issue/KT-39320) [Commonizer] Reduce memory consumption

### JavaScript

- [`KT-44394`](https://youtrack.jetbrains.com/issue/KT-44394) KJS / IR: `null` companion object for existed stdlib interfaces `NodeFilter` and `SVGUnitTypes`
- [`KT-45361`](https://youtrack.jetbrains.com/issue/KT-45361) KJS / IR: `IrConstructorCall` representing annotation always returns `Unit`
- [`KT-46859`](https://youtrack.jetbrains.com/issue/KT-46859) Exception during IR lowering: NullPointerException was thrown at: optimizations.FoldConstantLowering.tryFoldingUnaryOps

### Libraries

- [`KT-46002`](https://youtrack.jetbrains.com/issue/KT-46002) Support all Unicode digit chars in digitToInt (JS and Native)
- [`KT-46184`](https://youtrack.jetbrains.com/issue/KT-46184) Equivalize isLowerCase and isUpperCase behavior in all platforms

### Middle-end. IR

- [`KT-40425`](https://youtrack.jetbrains.com/issue/KT-40425) IrGenerationExtension. Support simple reporting to compiler output (for development/debug)
- [`KT-45308`](https://youtrack.jetbrains.com/issue/KT-45308) Psi2ir: "AssertionError: TypeAliasDescriptor expected" caused by using typealias from one module as a type in another module without a transitive dependency

### Native. ObjC Export

- [`KT-38600`](https://youtrack.jetbrains.com/issue/KT-38600) Kotlin MP iOS Target doesn't contain kdoc comments

### Reflection

- [`KT-10838`](https://youtrack.jetbrains.com/issue/KT-10838) Provide sensible toString() for property accessors in reflection

### Tools. CLI

- [`KT-14772`](https://youtrack.jetbrains.com/issue/KT-14772) ISE (FNFE "Not a directory") on compilation with destination argument clashing with an existing file which is not a directory
- [`KT-18184`](https://youtrack.jetbrains.com/issue/KT-18184) CompileEnvironmentException: Invalid jar path on "-d" with .jar in non-existing directory
- [`KT-40977`](https://youtrack.jetbrains.com/issue/KT-40977) Report a readable diagnostic on empty -J argument in CLI

### Tools. Compiler Plugins

- [`KT-45538`](https://youtrack.jetbrains.com/issue/KT-45538) Serialization, JVM IR: "AssertionError: No such type argument slot in IrConstructorCallImpl" with inner classes
- [`KT-46469`](https://youtrack.jetbrains.com/issue/KT-46469) Kotlin Lombok: accessors with `AccessLevel.MODULE` fail to resolve
- [`KT-46529`](https://youtrack.jetbrains.com/issue/KT-46529) Kotlin Lombok: with `@Accessors` without explicit `prefix` the prefix from lombok.config is not taken into account
- [`KT-46531`](https://youtrack.jetbrains.com/issue/KT-46531) Kotlin Lombok: `lombok.getter.noIsPrefix` is processed depending on character case

### Tools. Gradle

- [`KT-24533`](https://youtrack.jetbrains.com/issue/KT-24533) Kapt should not run when annotation processors are not configured
- [`KT-46401`](https://youtrack.jetbrains.com/issue/KT-46401) Deprecate 'kotlin.parallel.tasks.in.project' build property
- [`KT-46820`](https://youtrack.jetbrains.com/issue/KT-46820) Gradle: kotlinc (1.5.0) race condition causes a NullPointerException

### Tools. Gradle. JS

- [`KT-46976`](https://youtrack.jetbrains.com/issue/KT-46976) KJS: Broken support for dynamically created `webpack.config.d`

### Tools. Gradle. Multiplatform

- [`KT-46394`](https://youtrack.jetbrains.com/issue/KT-46394) Multiplatform: Gradle 7 support
- [`KT-46517`](https://youtrack.jetbrains.com/issue/KT-46517) Add kotlin-project-model as api dependency to kotlin-gradle-plugin-api

### Tools. Gradle. Native

- [`KT-46680`](https://youtrack.jetbrains.com/issue/KT-46680) Register concrete "embedAndSign" tasks instead umbrella
- [`KT-46892`](https://youtrack.jetbrains.com/issue/KT-46892) Kotlin Multiplatform Gradle Plugin: EmbedAndSign task always contains a default framework name

### Tools. kapt

- [`KT-31146`](https://youtrack.jetbrains.com/issue/KT-31146) kapt: executableElement.getAnnotation(JvmOverloads::class.java) returns null
- [`KT-35167`](https://youtrack.jetbrains.com/issue/KT-35167) Kapt Gradle plugin doesn't handle --module-path javac argument
- [`KT-41581`](https://youtrack.jetbrains.com/issue/KT-41581) Kapt doesn't have line breaks between warnings


## 1.5.20-M1

### Compiler

#### New Features

- [`KT-45189`](https://youtrack.jetbrains.com/issue/KT-45189) Support nullability annotations at module level
- [`KT-45525`](https://youtrack.jetbrains.com/issue/KT-45525) Allow to omit JvmInline annotation for expect value classes
- [`KT-46545`](https://youtrack.jetbrains.com/issue/KT-46545) Emit annotations on function type parameters into bytecode for -jvm-target 1.8 and above

#### Performance Improvements

- [`KT-36646`](https://youtrack.jetbrains.com/issue/KT-36646) Don't box primitive values in equality comparison with objects in JVM_IR

#### Fixes

- [`KT-8325`](https://youtrack.jetbrains.com/issue/KT-8325) Unresolved annotation should be an error
- [`KT-19455`](https://youtrack.jetbrains.com/issue/KT-19455) Type annotation unresolved on a type parameter of a supertype in anonymous object expression
- [`KT-24643`](https://youtrack.jetbrains.com/issue/KT-24643) Prohibit using a type parameter declared for an extension property inside delegate
- [`KT-25876`](https://youtrack.jetbrains.com/issue/KT-25876) Annotations on return types and supertypes are not analyzed
- [`KT-28449`](https://youtrack.jetbrains.com/issue/KT-28449) Annotation target is not analyzed in several cases for type annotations
- [`KT-36770`](https://youtrack.jetbrains.com/issue/KT-36770) Prohibit unsafe calls with expected @NotNull T and given Kotlin generic parameter with nullable bound
- [`KT-38325`](https://youtrack.jetbrains.com/issue/KT-38325) IllegalStateException: No parameter with index 0-0 when iterating Scala 2.12.11 List
- [`KT-38540`](https://youtrack.jetbrains.com/issue/KT-38540) Kotlin/Native Set<T>.contains fails with specific enum setup
- [`KT-41620`](https://youtrack.jetbrains.com/issue/KT-41620) ClassCastException: Class cannot be cast to java.lang.Void
- [`KT-41679`](https://youtrack.jetbrains.com/issue/KT-41679) NI: TYPE_MISMATCH wrong type inference of collection with type Any and integer literal
- [`KT-41818`](https://youtrack.jetbrains.com/issue/KT-41818) NI: False positive IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION leads to NothingValueException on delegated properties
- [`KT-42631`](https://youtrack.jetbrains.com/issue/KT-42631) ArrayIndexOutOfBoundsException was thrown during IR lowering
- [`KT-43258`](https://youtrack.jetbrains.com/issue/KT-43258) NI: False positive "Suspend function 'invoke' should be called only from a coroutine or another suspend function" when calling suspend operator fun on object property from last expression of a crossinlined suspend lambda
- [`KT-44036`](https://youtrack.jetbrains.com/issue/KT-44036) Enum initialization order
- [`KT-44511`](https://youtrack.jetbrains.com/issue/KT-44511) FIR DFA: smartcast after `if (nullable ?: boolean)`
- [`KT-44554`](https://youtrack.jetbrains.com/issue/KT-44554) RAW FIR: NPE in RawFirBuilder
- [`KT-44695`](https://youtrack.jetbrains.com/issue/KT-44695) *_TYPE_MISMATCH_ON_OVERRIDE checkers do not work for anonymous objects
- [`KT-44699`](https://youtrack.jetbrains.com/issue/KT-44699) FIR: incorrect lambda return type (led to a false alarm: PROPERTY_TYPE_MISMATCH_ON_OVERRIDE)
- [`KT-44802`](https://youtrack.jetbrains.com/issue/KT-44802) FIR bootstrap: trying to access package private class
- [`KT-44814`](https://youtrack.jetbrains.com/issue/KT-44814) FIR bootstrap: incorrect cast in when branch
- [`KT-44942`](https://youtrack.jetbrains.com/issue/KT-44942) [FIR] ClassCastException in boostrap tests
- [`KT-44995`](https://youtrack.jetbrains.com/issue/KT-44995) FIR: false positive for ANNOTATION_ARGUMENT_MUST_BE_CONST
- [`KT-45010`](https://youtrack.jetbrains.com/issue/KT-45010) FIR: lambda arguments of inapplicable call is not resolved
- [`KT-45048`](https://youtrack.jetbrains.com/issue/KT-45048) FIR bootstrap: VerifyError on KtUltraLightClass
- [`KT-45052`](https://youtrack.jetbrains.com/issue/KT-45052) FIR bootstrap: inapplicable candidate in GenerateSpecTests.kt
- [`KT-45121`](https://youtrack.jetbrains.com/issue/KT-45121) FIR IDE: redundant vararg parameter type transformation
- [`KT-45136`](https://youtrack.jetbrains.com/issue/KT-45136) Native: dividing Int.MIN_VALUE by -1 crashes or hangs
- [`KT-45316`](https://youtrack.jetbrains.com/issue/KT-45316) [FIR] Ambiguity between two implicit invokes with receiver
- [`KT-45344`](https://youtrack.jetbrains.com/issue/KT-45344) FIR: Wrong inferred type for nullable type parameter
- [`KT-45475`](https://youtrack.jetbrains.com/issue/KT-45475) [FIR] No smartcast after throw in if inside try block
- [`KT-45584`](https://youtrack.jetbrains.com/issue/KT-45584) [FIR] Fix overriding property and java function in java class
- [`KT-45697`](https://youtrack.jetbrains.com/issue/KT-45697) JVM IR: ISE "Function has no body" on getter of private field in a class present both in sources and dependencies
- [`KT-45842`](https://youtrack.jetbrains.com/issue/KT-45842) Compiler doesn't allow a shared class to inherit a platform-specific sealed class
- [`KT-45848`](https://youtrack.jetbrains.com/issue/KT-45848) False negative [SEALED_INHERITOR_IN_DIFFERENT_MODULE] error in compiler for a platform-specific inheritor of a shared sealed class
- [`KT-46048`](https://youtrack.jetbrains.com/issue/KT-46048) Enum entries init order in companion object
- [`KT-46074`](https://youtrack.jetbrains.com/issue/KT-46074) FIR: private-in-file fun interface is considered invisible in this file
- [`KT-46173`](https://youtrack.jetbrains.com/issue/KT-46173) No error reporting on annotations on target type of `as` expression in return
- [`KT-46270`](https://youtrack.jetbrains.com/issue/KT-46270) [FIR] Support `@PublishedAPI` in inline checker
- [`KT-46539`](https://youtrack.jetbrains.com/issue/KT-46539) Generate annotations on type parameters bounds in bytecode
- [`KT-46670`](https://youtrack.jetbrains.com/issue/KT-46670) StackOverflowError on inheritance from raw type where class has protobuf-like recursive generics
- [`KT-46715`](https://youtrack.jetbrains.com/issue/KT-46715) JVM / IR: "AssertionError: Unbound symbols not allowed IrConstructorSymbolImpl" with enum classes with the same name in test and src folders

### IDE

- [`KT-44638`](https://youtrack.jetbrains.com/issue/KT-44638) `clone()` call is unresolved in JVM module of a multiplatform project
- [`KT-45629`](https://youtrack.jetbrains.com/issue/KT-45629) [ULC] KtUltraLightFieldForSourceDeclaration.nameIdentifier returns null
- [`KT-45908`](https://youtrack.jetbrains.com/issue/KT-45908) Reproduciable 'org.jetbrains.kotlin.idea.caches.resolve.KotlinIdeaResolutionException: Kotlin resolution encountered a problem while analyzing KtNameReferenceExpression'

### IDE. FIR

- [`KT-45175`](https://youtrack.jetbrains.com/issue/KT-45175) FIR IDE: Exception with local property in `init` block
- [`KT-45199`](https://youtrack.jetbrains.com/issue/KT-45199) FIR IDE: Error while collecting diagnostic on stale element after replacing element in quickfix
- [`KT-45312`](https://youtrack.jetbrains.com/issue/KT-45312) FIR IDE: FIR plugin throws exception on synthetic function

### IDE. Gradle Integration

- [`KT-34401`](https://youtrack.jetbrains.com/issue/KT-34401) KotlinGradleModelBuilder builds models for non-kotlin modules and always trigger full task configuration.
- [`KT-46488`](https://youtrack.jetbrains.com/issue/KT-46488) Import of a multiplatform project with MPP module depending on Kotlin/JVM one fails

### IDE. Inspections and Intentions

- [`KT-45075`](https://youtrack.jetbrains.com/issue/KT-45075) Inspection: Redundant creation of Json format
- [`KT-45347`](https://youtrack.jetbrains.com/issue/KT-45347) Sealed interfaces: quickfix to move to package/module of sealed class/interface should not be shown in case of read-only declaration
- [`KT-45348`](https://youtrack.jetbrains.com/issue/KT-45348) Sealed interfaces: show error for usage of sealed class/interface from a library in Java source code
- [`KT-46063`](https://youtrack.jetbrains.com/issue/KT-46063) In multiplatform code, suggest to generate remaining `when` branches at least for shared sealed classes

### IDE. Refactorings

- [`KT-44431`](https://youtrack.jetbrains.com/issue/KT-44431) Quickfix to move class/interface to proper location: it is allowed to choose test source in JPS project while compiler does not allow it

### JavaScript

- [`KT-40235`](https://youtrack.jetbrains.com/issue/KT-40235) KJS: IR. Broken support for external interface companion
- [`KT-40689`](https://youtrack.jetbrains.com/issue/KT-40689) KJS / IR: strange and slow code for kotlin.math.max and kotlin.math.min for Double
- [`KT-44138`](https://youtrack.jetbrains.com/issue/KT-44138) KJS / IR: Constant folding works incorrectly with unsigned arithmetics
- [`KT-44950`](https://youtrack.jetbrains.com/issue/KT-44950) KJS / IR: "IllegalStateException: Can't find name for declaration" in case of extending export declared class without @JsExport annotation
- [`KT-45057`](https://youtrack.jetbrains.com/issue/KT-45057) KJS / IR: "ClassCastException" when using `js` function in init block
- [`KT-45655`](https://youtrack.jetbrains.com/issue/KT-45655) KJS: "REINTERPRET_CAST" is not copyable
- [`KT-45866`](https://youtrack.jetbrains.com/issue/KT-45866) Default parameter with generic in expect-actual declarations

### Libraries

- [`KT-43701`](https://youtrack.jetbrains.com/issue/KT-43701) Stdlib: Expand KDoc of inc() and dec() for operators

### Middle-end. IR

- [`KT-36880`](https://youtrack.jetbrains.com/issue/KT-36880) K/N IR: Reference to expect property in actual declaration is not remapped
- [`KT-45236`](https://youtrack.jetbrains.com/issue/KT-45236) JVM / IR: "IllegalStateException: Symbol with IrTypeParameterSymbolImpl is unbound" caused by contracts and sealed class

### Native

- [`KT-33175`](https://youtrack.jetbrains.com/issue/KT-33175) IR: String constants with incorrect surrogate pairs aren't preserved during serialization/deserialization
- [`KT-44799`](https://youtrack.jetbrains.com/issue/KT-44799) Different behavior with functional interfaces in Kotlin/Native on iOS

### Native. C Export

- [`KT-42796`](https://youtrack.jetbrains.com/issue/KT-42796) [Reverse C Interop] Package with no public methods generate empty struct in the header, leading to an error

### Native. ObjC Export

- [`KT-45127`](https://youtrack.jetbrains.com/issue/KT-45127) KMM: hard to pass an error to Kotlin code from implementation of Kotlin method in Swift code

### Native. Runtime. Memory

- [`KT-45063`](https://youtrack.jetbrains.com/issue/KT-45063) Profiling indicates that a lot of time is spent on updateHeapRef on Apple platforms when running KMP code

### Reflection

- [`KT-13490`](https://youtrack.jetbrains.com/issue/KT-13490) Equality doesn't work for KProperty.Accessor implementations

### Tools. Commonizer

- [`KT-45497`](https://youtrack.jetbrains.com/issue/KT-45497) [Commonizer] c-interop commonization: Dependency commonization
- [`KT-46077`](https://youtrack.jetbrains.com/issue/KT-46077) [Commonizer] Add `commonizer_target` to commonized klib's manifest
- [`KT-46107`](https://youtrack.jetbrains.com/issue/KT-46107) [Commonizer] CInteropCommonizerTask receives faulty dependencies in multi module projects containing multiple c-interops
- [`KT-46248`](https://youtrack.jetbrains.com/issue/KT-46248) MPP: Compile KotlinMetadata fails with Unresolved reference if only one native platform from shared source set is available

### Tools. Compiler Plugins

- [`KT-7112`](https://youtrack.jetbrains.com/issue/KT-7112) Support calling Lombok-generated methods within same module
- [`KT-45541`](https://youtrack.jetbrains.com/issue/KT-45541) JVM / IR / Serialization: NullPointerException caused by "Serializable" annotation and local data class

### Tools. Gradle

- [`KT-43988`](https://youtrack.jetbrains.com/issue/KT-43988) Enable plugin validation during build
- [`KT-45301`](https://youtrack.jetbrains.com/issue/KT-45301) Gradle: Empty `build/kotlin` dir with custom build directory
- [`KT-45519`](https://youtrack.jetbrains.com/issue/KT-45519) loadAndroidPluginVersion() impacts performance negatively and noticeably in multimodule Android build
- [`KT-45744`](https://youtrack.jetbrains.com/issue/KT-45744) Create Kotlin Gradle Plugin JUnit5 basic test setup
- [`KT-45834`](https://youtrack.jetbrains.com/issue/KT-45834) Gradle Plugin read system property related to kotlinCompilerClasspath breaks use of configuration cache

### Tools. Gradle. JS

- [`KT-42911`](https://youtrack.jetbrains.com/issue/KT-42911) Support Gradle configuration cache for K/JS tasks
- [`KT-45294`](https://youtrack.jetbrains.com/issue/KT-45294) KJS / Gradle: Number of modules in project affects JS tasks configuration cache state size
- [`KT-45754`](https://youtrack.jetbrains.com/issue/KT-45754) KJS / IR: Remove adding option of source maps in Gradle plugin
- [`KT-46178`](https://youtrack.jetbrains.com/issue/KT-46178) KJS / Dukat: Added as a dependency always without condition

### Tools. Gradle. Multiplatform

- [`KT-36679`](https://youtrack.jetbrains.com/issue/KT-36679) MPP Gradle plugin: Improve messaging for the commonizer
- [`KT-45832`](https://youtrack.jetbrains.com/issue/KT-45832) CInteropCommonization: Filter out illegal dependencies

### Tools. Gradle. Native

- [`KT-39016`](https://youtrack.jetbrains.com/issue/KT-39016) Missing stdlib when the downloading process was aborted
- [`KT-44059`](https://youtrack.jetbrains.com/issue/KT-44059) iosSimTest tasks are never up-to-date
- [`KT-45801`](https://youtrack.jetbrains.com/issue/KT-45801) compileIosMainKotlinMetadata compilation property of gradle task is not initialized and fails with  `Execution failed for task ':shared:generateProjectStructureMetadata'`

### Tools. Incremental Compile

- [`KT-44741`](https://youtrack.jetbrains.com/issue/KT-44741) Incremental compilation: inspectClassesForKotlinIC doesn't determine changes with imported constant

### Tools. JPS

- [`KT-34351`](https://youtrack.jetbrains.com/issue/KT-34351) KotlinTargetsIndex creation takes too long even if project doesn't have any kotlin
- [`KT-45191`](https://youtrack.jetbrains.com/issue/KT-45191) [JPS] Marking method as "default" in Java SAM interface doesn't affect dependencies
- [`KT-46242`](https://youtrack.jetbrains.com/issue/KT-46242) Support Lombok kotlin plugin in JPS and maven

### Tools. kapt

#### Performance Improvements

- [`KT-28901`](https://youtrack.jetbrains.com/issue/KT-28901) Consider caching annotation processors classloaders

#### Fixes

- [`KT-27123`](https://youtrack.jetbrains.com/issue/KT-27123) kapt: missing space in error log makes location non-clickable
- [`KT-29929`](https://youtrack.jetbrains.com/issue/KT-29929) [Kapt] Stub generator uses constant value in method annotation instead of constant name 2.
- [`KT-32202`](https://youtrack.jetbrains.com/issue/KT-32202) Gradle task kaptKotlin fails: "module not found" in Java 11 modular application
- [`KT-34838`](https://youtrack.jetbrains.com/issue/KT-34838) Kapt: 'cannot find symbol' for a top-level property with anonymous delegate
- [`KT-35104`](https://youtrack.jetbrains.com/issue/KT-35104) Support @JvmStatic in KAPT stubs
- [`KT-37586`](https://youtrack.jetbrains.com/issue/KT-37586) KAPT: When delegated property use an unknown type (to-be-generated class), `correctTypeError` will mess up the `$delegate` field type
- [`KT-39060`](https://youtrack.jetbrains.com/issue/KT-39060) Kapt: correctErrorTypes don't retain return type of getter in Java stub
- [`KT-39715`](https://youtrack.jetbrains.com/issue/KT-39715) KaptGenerateStubsTask resolves annotation processor options too early (before execution time)
- [`KT-43804`](https://youtrack.jetbrains.com/issue/KT-43804) Kapt fails to preserve parameter names in open suspend functions
- [`KT-45032`](https://youtrack.jetbrains.com/issue/KT-45032) Kapt: NullPointerException: insnList.first must not be null
- [`KT-46176`](https://youtrack.jetbrains.com/issue/KT-46176) Kapt: "java.lang.IndexOutOfBoundsException: Index -1 out of bounds for length 3" with delegation and property reference


## Recent ChangeLogs:
### [ChangeLog-1.4.X](docs/changelogs/ChangeLog-1.4.X.md)
### [ChangeLog-1.3.X](docs/changelogs/ChangeLog-1.3.X.md)
### [ChangeLog-1.2.X](docs/changelogs/ChangeLog-1.2.X.md)
### [ChangeLog-1.1.X](docs/changelogs/ChangeLog-1.1.X.md)
### [ChangeLog-1.0.X](docs/changelogs/ChangeLog-1.0.X.md)
