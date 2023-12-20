## 1.9.22

### JavaScript

- [`KT-63719`](https://youtrack.jetbrains.com/issue/KT-63719) KJS: Test results ignored for ES module kind
- [`KT-63808`](https://youtrack.jetbrains.com/issue/KT-63808) compileTestDevelopmentExecutableKotlinJs failed in JsIntrinsicTransformers

### Native

- [`KT-64139`](https://youtrack.jetbrains.com/issue/KT-64139) Weird bug with while and coroutine in Kotlin Native
- [`KT-63471`](https://youtrack.jetbrains.com/issue/KT-63471) linkDebugTestIosX64 Failed to build cache: NoSuchFileException bitcode_deps
- [`KT-63789`](https://youtrack.jetbrains.com/issue/KT-63789) Native: Incremental compilation problem with compose

### Tools. CLI

- [`KT-64485`](https://youtrack.jetbrains.com/issue/KT-64485) CLI: cache and optimize parsing of command-line arguments

### Tools. Gradle

- [`KT-63990`](https://youtrack.jetbrains.com/issue/KT-63990) "Cannot query the value of property 'buildFlowServiceProperty' because it has no value available" with Isolated Projects

### Tools. Gradle. Native

- [`KT-63363`](https://youtrack.jetbrains.com/issue/KT-63363) Kotlin Gradle Plugin: `KotlinNativeHostSpecificMetadataArtifact` breaks configuration cache, implicitly includes output file as configuration cache input
- [`KT-63742`](https://youtrack.jetbrains.com/issue/KT-63742) Gradle wrongly caches Kotlin/Native compiler flags

### Tools. JPS

- [`KT-64305`](https://youtrack.jetbrains.com/issue/KT-64305) Kotlin JPS builder requests chunk rebuild with graph implementation
- [`KT-64112`](https://youtrack.jetbrains.com/issue/KT-64112) Avoid using IJ's JPS mappings in Kotlin JPS tests
- [`KT-63799`](https://youtrack.jetbrains.com/issue/KT-63799) Make plugin classpath serialization path agnostic


## 1.9.21

### Compiler

- [`KT-62885`](https://youtrack.jetbrains.com/issue/KT-62885) Introduce a language feature entry for expect actual classes for easier configuration of MPP projects
- [`KT-63081`](https://youtrack.jetbrains.com/issue/KT-63081) Optimize new native caches: CachedLibraries.computeVersionedCacheDirectory()

### Docs & Examples

- [`KT-55619`](https://youtrack.jetbrains.com/issue/KT-55619) Document `String.format` function

### IDE. Gradle Integration

- [`KT-62877`](https://youtrack.jetbrains.com/issue/KT-62877) Artifact files collecting for project configuration was finished. Resolution for configuration configuration  X will be skipped

### IDE. Gradle. Script

- [`KT-60813`](https://youtrack.jetbrains.com/issue/KT-60813) Scripts: NoSuchMethodError: 'void org.slf4j.Logger.error(java.lang.String, java.lang.Object)' when dependency uses Slf4j API

### JavaScript

- [`KT-60785`](https://youtrack.jetbrains.com/issue/KT-60785) KJS: Destructured value class in suspend function fails with Uncaught TypeError: can't convert to primitive type error
- [`KT-63207`](https://youtrack.jetbrains.com/issue/KT-63207) KMP / JS: "TypeError: <mangled_name> is not a function" with 1.9.20
- [`KT-62778`](https://youtrack.jetbrains.com/issue/KT-62778) package.json "main" field has .js extension when the result files have .mjs extension
- [`KT-61795`](https://youtrack.jetbrains.com/issue/KT-61795) KJS: Incremental Cache is not invalidated if `useEsClasses` compiler argument was changed
- [`KT-61957`](https://youtrack.jetbrains.com/issue/KT-61957) KJS: "Uncaught ReferenceError: entries is not defined" caused by enum class with `@JsExport` and Enum.entries call
- [`KT-62444`](https://youtrack.jetbrains.com/issue/KT-62444) KJS with commonJS modules should re-export in 1.9.20
- [`KT-63184`](https://youtrack.jetbrains.com/issue/KT-63184) KJS / Serialization: JsExport on serializable interface creates erroneous TypeScript
- [`KT-62190`](https://youtrack.jetbrains.com/issue/KT-62190) KJS: "IllegalStateException: Expect to have either super call or partial linkage stub inside constructor" caused by Compose and useEsModules()
- [`KT-58685`](https://youtrack.jetbrains.com/issue/KT-58685) KJS: "IllegalStateException: Not locked" cused by "unlock" called twice

### Klibs

- [`KT-62515`](https://youtrack.jetbrains.com/issue/KT-62515) Interop klib of concurrent version is not accepted when building dependent project: "The library versions don't match"

### Tools. CLI

- [`KT-63139`](https://youtrack.jetbrains.com/issue/KT-63139) Incorrect kotlin implementation version (1.9.255-SNAPSHOT) in metadata info

### Tools. Gradle

- [`KT-63499`](https://youtrack.jetbrains.com/issue/KT-63499) Gradle: Source sets conventions are still registered

### Tools. Gradle. JS

- [`KT-59523`](https://youtrack.jetbrains.com/issue/KT-59523) MPP / KJS: ESM modules uses incorrect file extension on package.json (.mjs)

### Tools. Gradle. Kapt

- [`KT-63366`](https://youtrack.jetbrains.com/issue/KT-63366) Kapt processing fails with custom source sets

### Tools. Gradle. Multiplatform

- [`KT-32608`](https://youtrack.jetbrains.com/issue/KT-32608) Create JUnit-XML result file in multiplatform gradle build
- [`KT-63315`](https://youtrack.jetbrains.com/issue/KT-63315) Wasm gradle plugin DSL is invalid for parameterless wasmWasi method
- [`KT-63338`](https://youtrack.jetbrains.com/issue/KT-63338) [KMP] metadata task fails to find cinterop classes from dependency projects
- [`KT-63044`](https://youtrack.jetbrains.com/issue/KT-63044) KGP: Multiplatform - 8.4 configuration cache support
- [`KT-63011`](https://youtrack.jetbrains.com/issue/KT-63011) Apple Framework Artifacts is not connected to KotlinNativeTask
- [`KT-62601`](https://youtrack.jetbrains.com/issue/KT-62601) AS/IntelliJ exception after updating a KMP project with a macos target to Kotlin 1.9.20-RC

### Tools. Incremental Compile

- [`KT-61590`](https://youtrack.jetbrains.com/issue/KT-61590) K2/KMP: Expect actual matching is breaking on the incremental compilation

### Tools. JPS

- [`KT-63594`](https://youtrack.jetbrains.com/issue/KT-63594) ClassCastException in JPS statistics
- [`KT-63651`](https://youtrack.jetbrains.com/issue/KT-63651) Fix NPE in Kotlin JPS after enabling graph implementation of JPS

### Tools. Kapt

- [`KT-57389`](https://youtrack.jetbrains.com/issue/KT-57389) KAPT3 uses a Javac API for JCImport which will break in JDK 21
- [`KT-60507`](https://youtrack.jetbrains.com/issue/KT-60507) Kapt: "IllegalAccessError: superclass access check failed" using java 21 toolchain

### Tools. Scripts

- [`KT-54819`](https://youtrack.jetbrains.com/issue/KT-54819) Scripts: Not able to use slf4j in .main.kts
- [`KT-61727`](https://youtrack.jetbrains.com/issue/KT-61727) Scripts: Maven artifacts resolution is slow


## 1.9.20

### Analysis. API

#### New Features

- [`KT-58834`](https://youtrack.jetbrains.com/issue/KT-58834) Analysis API: Add source shadowing feature to resolve extensions

#### Performance Improvements

- [`KT-57515`](https://youtrack.jetbrains.com/issue/KT-57515) LL FIR: Performance bottleneck in `CompositeModificationTracker.getModificationCount`
- [`KT-59266`](https://youtrack.jetbrains.com/issue/KT-59266) K2: optimize FirElementBuilder.getOrBuildFir for elements outside body
- [`KT-59454`](https://youtrack.jetbrains.com/issue/KT-59454) K2: drop resolve from org.jetbrains.kotlin.analysis.api.fir.components.KtFirVisibilityChecker#collectContainingDeclarations
- [`KT-59453`](https://youtrack.jetbrains.com/issue/KT-59453) K2: completion regression from org.jetbrains.kotlin.analysis.api.fir.components.KtFirVisibilityChecker#collectContainingDeclarations
- [`KT-59189`](https://youtrack.jetbrains.com/issue/KT-59189) Analysis API: KtFirKDocReference.resolveToSymbols is slow
- [`KT-58125`](https://youtrack.jetbrains.com/issue/KT-58125) K2: LL FIR: `KtToFirMapping.getElement` is slow for `KtUserType`s due to on-air resolution of types

#### Fixes

- [`KT-59240`](https://youtrack.jetbrains.com/issue/KT-59240) K2: FirLazyResolveContractViolationException: `lazyResolveToPhase(IMPORTS)` cannot be called from a transformer with a phase IMPORTS from superTypes
- [`KT-58499`](https://youtrack.jetbrains.com/issue/KT-58499) K2: FirLazyBlock should be calculated before accessing
- [`KT-57966`](https://youtrack.jetbrains.com/issue/KT-57966) K2: Analysis API: Reference Shortener does not work correctly when called on entire file
- [`KT-60954`](https://youtrack.jetbrains.com/issue/KT-60954) K2: Analysis API: Reference shortener does not work correctly with variable assignments
- [`KT-60940`](https://youtrack.jetbrains.com/issue/KT-60940) K2: Analysis API: Reference shortener incorrectly handles types in vararg parameters declarations
- [`KT-60488`](https://youtrack.jetbrains.com/issue/KT-60488) Analysis API: forbid providing custom KtLifetimeToken for every analyze call
- [`KT-60728`](https://youtrack.jetbrains.com/issue/KT-60728) K2: proper support for scripts in LL FIR transformers
- [`KT-59159`](https://youtrack.jetbrains.com/issue/KT-59159) K2 IDE: declaration is not found exception
- [`KT-59297`](https://youtrack.jetbrains.com/issue/KT-59297) K2: exception from body resolve leads to corrupted state and broken analysis
- [`KT-59077`](https://youtrack.jetbrains.com/issue/KT-59077) KtFirExpressionTypeProvider behaviour for KtSimpleNameReferences in function calls
- [`KT-60586`](https://youtrack.jetbrains.com/issue/KT-60586) K2: forbid analyze from write action
- [`KT-57743`](https://youtrack.jetbrains.com/issue/KT-57743) K2 IDE: StackOverflowError from LLFirSessionCache for simple JPS project with cyclic dependencies
- [`KT-61026`](https://youtrack.jetbrains.com/issue/KT-61026) K2 Scripts: FirLazyExpression should be calculated before accessing from on-air resolve
- [`KT-61009`](https://youtrack.jetbrains.com/issue/KT-61009) K2 Scripts: KtFirExpressionTypeProvider: Expected FirResolvedTypeRef with ConeKotlinType but was FirImplicitTypeRefImplWithoutSource <implicit>
- [`KT-60357`](https://youtrack.jetbrains.com/issue/KT-60357) K2 IDE. Reified types parameters are not resolved in a function body
- [`KT-60317`](https://youtrack.jetbrains.com/issue/KT-60317) K2 IDE. IAE "This method will only work on compiled declarations, but this declaration is not compiled" on invoking Find Usages for enum method in library
- [`KT-60706`](https://youtrack.jetbrains.com/issue/KT-60706) K2 IDE: FirJvmTypeMapper is not found for kotlin.kotlin-stdlib-common
- [`KT-60552`](https://youtrack.jetbrains.com/issue/KT-60552) K2: merge StateKeeper and lazy body calculator for ANNOTATIONS_ARGUMENTS_MAPPING transformer
- [`KT-60641`](https://youtrack.jetbrains.com/issue/KT-60641) Analysis API:  Scope for class org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl not found exception when stdlib is missing
- [`KT-60638`](https://youtrack.jetbrains.com/issue/KT-60638) K2: Expected FirResolvedTypeRef with ConeKotlinType but was FirImplicitTypeRefImplWithoutSource <implicit>
- [`KT-54846`](https://youtrack.jetbrains.com/issue/KT-54846) Analysis API: add isExpect/isActual to KtSymbol
- [`KT-60448`](https://youtrack.jetbrains.com/issue/KT-60448) FirLazyResolveContractViolationException: `lazyResolveToPhase(COMPILER_REQUIRED_ANNOTATIONS)` cannot be called from a transformer with a phase COMPILER_REQUIRED_ANNOTATIONS from AllOpen plugin
- [`KT-59342`](https://youtrack.jetbrains.com/issue/KT-59342) K2 IDE. FirLazyResolveContractViolationException: `lazyResolveToPhase(TYPES)` cannot be called from a transformer with a phase TYPES
- [`KT-59687`](https://youtrack.jetbrains.com/issue/KT-59687) K2: Implement proper body update for in-block modifications
- [`KT-59329`](https://youtrack.jetbrains.com/issue/KT-59329) Resolve Extensions reference resolution breaks Find Usages
- [`KT-60295`](https://youtrack.jetbrains.com/issue/KT-60295) K2: move checkIsResolved for annotations from LLFirAnnotationArgumentsLazyResolver to LLFirTypeLazyResolver
- [`KT-59758`](https://youtrack.jetbrains.com/issue/KT-59758) K2: Expected is FirResolvedTypeRef, but was FirImplicitTypeRefImplWithoutSource from ReturnTypeCalculatorWithJump
- [`KT-60377`](https://youtrack.jetbrains.com/issue/KT-60377) K2 IDE: This method will only work on compiled declarations, but this declaration is not compiled
- [`KT-59685`](https://youtrack.jetbrains.com/issue/KT-59685) K2: rewrite on-air resolution
- [`KT-60132`](https://youtrack.jetbrains.com/issue/KT-60132) K2: properties and functions without a name should be re-analyzable as well
- [`KT-59199`](https://youtrack.jetbrains.com/issue/KT-59199) K2 IDE: PSI changes which do not cause OOB modifications can be unseen from the FIR elements
- [`KT-59667`](https://youtrack.jetbrains.com/issue/KT-59667) Analysis API: PsiInvalidElementAccessException from JavaClassifierTypeImpl.substitutor
- [`KT-59705`](https://youtrack.jetbrains.com/issue/KT-59705) KotlinExceptionWithAttachments: No fir element was found for getter
- [`KT-59697`](https://youtrack.jetbrains.com/issue/KT-59697) AA standalone: JRT module paths are not properly populated in Windows
- [`KT-59505`](https://youtrack.jetbrains.com/issue/KT-59505) K2: implicit type lazy resolution doesn't work for delegated declaration from other module
- [`KT-56426`](https://youtrack.jetbrains.com/issue/KT-56426) K2 IDE: Typealised functional types cannot be rendered
- [`KT-59598`](https://youtrack.jetbrains.com/issue/KT-59598) AA: stackoverflow while simplifying a type with a recursive type parameter
- [`KT-58497`](https://youtrack.jetbrains.com/issue/KT-58497) K2: Expected FirResolvedTypeRef for initializer type of FirPropertyImpl(Source) but FirImplicitTypeRefImplWithoutSource found
- [`KT-59511`](https://youtrack.jetbrains.com/issue/KT-59511) AA standalone mode creates Application Environment for tests
- [`KT-58161`](https://youtrack.jetbrains.com/issue/KT-58161) Analysis API: Make methods in `KtCallResolverMixIn` more distinctive based on their receiver/return type
- [`KT-59093`](https://youtrack.jetbrains.com/issue/KT-59093) Do not throw exception on KtCall resolution, `KtCallElement.resolveCall` should return `null` on unknown cases
- [`KT-59243`](https://youtrack.jetbrains.com/issue/KT-59243) K2: FirLazyResolveContractViolationException: `lazyResolveToPhase(IMPORTS)` cannot be called from a transformer with a phase IMPORTS  from permits types
- [`KT-58194`](https://youtrack.jetbrains.com/issue/KT-58194) K2: Low Level API: use smart pointers to store references to PSI from FIR declarations for JavaElement
- [`KT-59133`](https://youtrack.jetbrains.com/issue/KT-59133) K2: java.lang.IllegalStateException: Fir is not initialized for FirRegularClassSymbol
- [`KT-58174`](https://youtrack.jetbrains.com/issue/KT-58174) K2: LL FIR: Invalid type reference for T & Any type
- [`KT-52615`](https://youtrack.jetbrains.com/issue/KT-52615) LL FIR: build RAW FIR only by stubs
- [`KT-55053`](https://youtrack.jetbrains.com/issue/KT-55053) K2: Exception "lateinit property diagnostic has not been initialized" in FirBuilder
- [`KT-58580`](https://youtrack.jetbrains.com/issue/KT-58580) K2: LL FIR: Declarations provided by resolve extensions from a dependency module are not visible through `LLFirCombinedKotlinSymbolProvider`
- [`KT-58992`](https://youtrack.jetbrains.com/issue/KT-58992) Analysis API: move org.jetbrains.kotlin.analysis.api.fir.utils.addImportToFile out of Analysis API
- [`KT-58727`](https://youtrack.jetbrains.com/issue/KT-58727) K2: AA FIR: implicit type in delegated function treated as error
- [`KT-58653`](https://youtrack.jetbrains.com/issue/KT-58653) K2: Analysis API: add functions for KtScope members access by name
- [`KT-57559`](https://youtrack.jetbrains.com/issue/KT-57559) K2 IDE: KotlinExceptionWithAttachments: Modules are inconsistent on intellij project
- [`KT-58262`](https://youtrack.jetbrains.com/issue/KT-58262) Analysis API: Declarations from Analysis API Resolve Extensions are not seen from completion
- [`KT-57455`](https://youtrack.jetbrains.com/issue/KT-57455) LL FIR: Combine `AbstractFirDeserializedSymbolProvider`s in session dependencies (optimization)
- [`KT-57207`](https://youtrack.jetbrains.com/issue/KT-57207) LL FIR: Combine `JavaSymbolProvider`s in session dependencies (optimization)
- [`KT-58546`](https://youtrack.jetbrains.com/issue/KT-58546) K2: LL FIR: support name collision in a designation path
- [`KT-58495`](https://youtrack.jetbrains.com/issue/KT-58495) K2: Lazy calculation is redundant
- [`KT-58500`](https://youtrack.jetbrains.com/issue/KT-58500) K2: null cannot be cast to non-null type org.jetbrains.kotlin.fir.FirPureAbstractElement
- [`KT-58493`](https://youtrack.jetbrains.com/issue/KT-58493) K2: Expected FirResolvedTypeRef for default value type of FirValueParameterImpl(Source) but FirUserTypeRefImpl found
- [`KT-58496`](https://youtrack.jetbrains.com/issue/KT-58496) K2: Expected FirNamedReference, FirErrorNamedReference or FirFromMissingDependenciesNamedReference, but FirExplicitSuperReference found
- [`KT-58491`](https://youtrack.jetbrains.com/issue/KT-58491) K2: Expected FirResolvedTypeRef or FirImplicitTypeRef for return type of FirDefaultPropertyBackingField(Synthetic) but FirUserTypeRefImpl found
- [`KT-56550`](https://youtrack.jetbrains.com/issue/KT-56550) LL FIR: implement parallel resolve for non-jumping phases
- [`KT-58503`](https://youtrack.jetbrains.com/issue/KT-58503) Analysis API: KtFirNamedClassOrObjectSymbol.visibility/modality do not trigger STATUS resolve
- [`KT-57623`](https://youtrack.jetbrains.com/issue/KT-57623) K2 IDE: ConcurrentModificationException from getSuperConeTypes
- [`KT-58083`](https://youtrack.jetbrains.com/issue/KT-58083) K2: LL FIR: implement FakeOverrideTypeCalculator

### Android

- [`KT-27170`](https://youtrack.jetbrains.com/issue/KT-27170) Android lint tasks fails in Gradle with MPP dependency

### Backend. Native. Debug

- [`KT-61131`](https://youtrack.jetbrains.com/issue/KT-61131) Virtual functions trampolines have invalid debug info

### Backend. Wasm

#### Fixes

- [`KT-60244`](https://youtrack.jetbrains.com/issue/KT-60244) K/Wasm: make the compiler compatible with Wasm GC phase 4 (Final) specification
- [`KT-61262`](https://youtrack.jetbrains.com/issue/KT-61262) K/Wasm: add a way to turn on k2 in wasm examples that don't use compose
- [`KT-61343`](https://youtrack.jetbrains.com/issue/KT-61343) K/Wasm: add a wasi example to kotlin-wasm-examples
- [`KT-62147`](https://youtrack.jetbrains.com/issue/KT-62147) [Kotlin/Wasm] Nothing typed when expression cause a backend error
- [`KT-59720`](https://youtrack.jetbrains.com/issue/KT-59720) K/Wasm: update to final opcodes
- [`KT-60834`](https://youtrack.jetbrains.com/issue/KT-60834) K/Wasm: investigate consequences of stopping using `br_on_cast_fail`
- [`KT-59294`](https://youtrack.jetbrains.com/issue/KT-59294) WASM: localStorage Cannot read properties of undefined (reading 'length')
- [`KT-60835`](https://youtrack.jetbrains.com/issue/KT-60835) K/Wasm: fix compatibility with Node.js 20.*
- [`KT-60113`](https://youtrack.jetbrains.com/issue/KT-60113) K/Wasm: illegal cast when using 1.9.20-dev
- [`KT-60496`](https://youtrack.jetbrains.com/issue/KT-60496) Compose-web Wasm crashes on remember { null } calls
- [`KT-58746`](https://youtrack.jetbrains.com/issue/KT-58746) K/Wasm: Make Arrays' constructors with size and lambda inline (similar to other implementations)
- [`KT-58993`](https://youtrack.jetbrains.com/issue/KT-58993) [K/Wasm] Fix w3c declarations with lambda parameters
- [`KT-59722`](https://youtrack.jetbrains.com/issue/KT-59722) K/Wasm: Support new encoding with flags for br_on_cast and br_on_cast_fail instructions
- [`KT-59713`](https://youtrack.jetbrains.com/issue/KT-59713) K/Wasm: Implement enumEntries intrinsic
- [`KT-59082`](https://youtrack.jetbrains.com/issue/KT-59082) WASM: NullPointerException caused by companion with String type constants
- [`KT-58941`](https://youtrack.jetbrains.com/issue/KT-58941) WASM Hang with extension delegate inside a Class
- [`KT-60200`](https://youtrack.jetbrains.com/issue/KT-60200) K/Wasm: generate types without supertypes properly
- [`KT-52178`](https://youtrack.jetbrains.com/issue/KT-52178) IR dump doesn't seem to work for Kotlin/WASM phases
- [`KT-59556`](https://youtrack.jetbrains.com/issue/KT-59556) Wasm: critical dependency when using with webpack
- [`KT-58681`](https://youtrack.jetbrains.com/issue/KT-58681) K/Wasm: division remainder has a wrong sign
- [`KT-56711`](https://youtrack.jetbrains.com/issue/KT-56711) Wasm: IllegalStateException caused by dynamic type

### Compiler

#### New Features

- [`KT-58551`](https://youtrack.jetbrains.com/issue/KT-58551) KMP: check all annotation from expect declaration are present on actual
- [`KT-58554`](https://youtrack.jetbrains.com/issue/KT-58554) KMP: restrict expect opt-in annotations and actual typealiases to annotations with special meaning
- [`KT-58545`](https://youtrack.jetbrains.com/issue/KT-58545) KMP: prohibit implicit actualization via Java
- [`KT-58536`](https://youtrack.jetbrains.com/issue/KT-58536) KMP: prohibit `expect tailrec` / `expect external`
- [`KT-59764`](https://youtrack.jetbrains.com/issue/KT-59764) Make a frontend checker that reports cast to forward declaration as unchecked
- [`KT-60528`](https://youtrack.jetbrains.com/issue/KT-60528) Updates for JVM/IR backend of kotlin-atomicfu-compiler-plugin
- [`KT-59558`](https://youtrack.jetbrains.com/issue/KT-59558) Add support for creating annotation instances with type parameters
- [`KT-52367`](https://youtrack.jetbrains.com/issue/KT-52367) Devirtualization algorithm improvement
- [`KT-58652`](https://youtrack.jetbrains.com/issue/KT-58652) Native: Implement frontend checkers for HiddenFromObjC on classes

#### Performance Improvements

- [`KT-59600`](https://youtrack.jetbrains.com/issue/KT-59600) K2: CFG: do not add edges to nested classes and functions
- [`KT-57860`](https://youtrack.jetbrains.com/issue/KT-57860) K/N: Functions with default arguments of value/inline class types have poor performance due to value class boxing

#### Fixes

- [`KT-60387`](https://youtrack.jetbrains.com/issue/KT-60387) K2: IDE K2: "org.jetbrains.kotlin.fir.expressions.impl.FirArgumentListImpl cannot be cast to class org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList"
- [`KT-61228`](https://youtrack.jetbrains.com/issue/KT-61228) False positive MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT_WARNING for effectively final properties
- [`KT-61643`](https://youtrack.jetbrains.com/issue/KT-61643) "Argument type mismatch" for mixed Java/Kotlin Project with Java 21
- [`KT-62389`](https://youtrack.jetbrains.com/issue/KT-62389) JDK 21: Cannot access class 'TimeUnit'. Check your module classpath for missing or conflicting dependencies
- [`KT-56768`](https://youtrack.jetbrains.com/issue/KT-56768) K2. No error description on incomplete try catch declaration
- [`KT-52220`](https://youtrack.jetbrains.com/issue/KT-52220) FIR + LightTree - Consider building a single tree on parsing into LightTree
- [`KT-60601`](https://youtrack.jetbrains.com/issue/KT-60601) K2 / Maven: Overload resolution ambiguity between candidates inline method
- [`KT-62027`](https://youtrack.jetbrains.com/issue/KT-62027) "java.lang.IndexOutOfBoundsException: Empty list doesn't contain element at index 0" caused by ClassicExpectActualMatchingContext.kt when annotation `@AllowDifferentMembersInActual` used
- [`KT-62747`](https://youtrack.jetbrains.com/issue/KT-62747) Wrong warning message when overriding vararg with Array during actualization
- [`KT-62655`](https://youtrack.jetbrains.com/issue/KT-62655) Don't report a warning when new members and new supertypes are added to open expect actualization
- [`KT-62313`](https://youtrack.jetbrains.com/issue/KT-62313) Kotlin/Native Compiler crash: ClassCastException in IntrinsicGenerator
- [`KT-60902`](https://youtrack.jetbrains.com/issue/KT-60902) visibility vs upper bound expect actual matching conflict
- [`KT-61095`](https://youtrack.jetbrains.com/issue/KT-61095) K2: "IAE: source must not be null" from FirMultipleDefaultsInheritedFromSupertypesChecker
- [`KT-47567`](https://youtrack.jetbrains.com/issue/KT-47567) 'Val cannot be reassigned' error not reported in unreachable code
- [`KT-59468`](https://youtrack.jetbrains.com/issue/KT-59468) K2: build realm-kotlin
- [`KT-62026`](https://youtrack.jetbrains.com/issue/KT-62026) KMP: Correctly handle a case when annotation on expect declaration is unresolved
- [`KT-59476`](https://youtrack.jetbrains.com/issue/KT-59476) K2: build ClashForAndroid
- [`KT-59487`](https://youtrack.jetbrains.com/issue/KT-59487) K2: build KSP-playground
- [`KT-47409`](https://youtrack.jetbrains.com/issue/KT-47409) K1/K2: Investigate and align inference for equality (==) operator
- [`KT-59393`](https://youtrack.jetbrains.com/issue/KT-59393) K2: Missing TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED
- [`KT-62127`](https://youtrack.jetbrains.com/issue/KT-62127) "NoSuchFieldError: TRUE$delegate" on referencing companion's variable in submodule
- [`KT-62335`](https://youtrack.jetbrains.com/issue/KT-62335) Improve debuggability of code generator crashes
- [`KT-61165`](https://youtrack.jetbrains.com/issue/KT-61165) More than one overridden descriptor declares a default value for 'cause: Throwable?'. As the compiler can not make sure these values agree, this is not allowed
- [`KT-62263`](https://youtrack.jetbrains.com/issue/KT-62263) Turn "different expect/actual members" error into a warning
- [`KT-59969`](https://youtrack.jetbrains.com/issue/KT-59969) K2: Disappeared UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL
- [`KT-61616`](https://youtrack.jetbrains.com/issue/KT-61616) K2: `IrBuiltIns.extensionToString` fails during native compilation
- [`KT-59377`](https://youtrack.jetbrains.com/issue/KT-59377) K2: Missing CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM
- [`KT-61645`](https://youtrack.jetbrains.com/issue/KT-61645) K2/KMP: Set stdlib-native before stdlib-commonMain in dependencies for shared native metadata compilation
- [`KT-61924`](https://youtrack.jetbrains.com/issue/KT-61924) Native: problem with abstract fake override from Any
- [`KT-61933`](https://youtrack.jetbrains.com/issue/KT-61933) K2: "`Argument type mismatch: actual type is 'Foo<kotlin/Function0<kotlin/Unit>>' but 'Foo<kotlin/coroutines/SuspendFunction0<kotlin/Unit>>' was expected`"
- [`KT-59471`](https://youtrack.jetbrains.com/issue/KT-59471) K2: build multiplatform-settings
- [`KT-56077`](https://youtrack.jetbrains.com/issue/KT-56077) K2: build kotlinx.atomicfu
- [`KT-59465`](https://youtrack.jetbrains.com/issue/KT-59465) K2: build kotlinx-datetime
- [`KT-60824`](https://youtrack.jetbrains.com/issue/KT-60824) K2 IDE: FirSyntheticCallGenerator: IAE: List has more than one element
- [`KT-61856`](https://youtrack.jetbrains.com/issue/KT-61856) K2: "KotlinIllegalArgumentExceptionWithAttachments" on usage of javax.validation.constraints.Email.List
- [`KT-54792`](https://youtrack.jetbrains.com/issue/KT-54792) Store program order of properties inside `@kotlin`.Metadata
- [`KT-56083`](https://youtrack.jetbrains.com/issue/KT-56083) K2: build ktor
- [`KT-23861`](https://youtrack.jetbrains.com/issue/KT-23861) Expect annotation should not be applicable wider than the actual one
- [`KT-59466`](https://youtrack.jetbrains.com/issue/KT-59466) K2: build kotlinx-benchmark
- [`KT-60830`](https://youtrack.jetbrains.com/issue/KT-60830) KMP, K2: expect actual annotation IR checker doesn't unwrap actual typealiases to annotations
- [`KT-61668`](https://youtrack.jetbrains.com/issue/KT-61668) Put expect/actual diagnostics introduced in 1.9.20 release under 1.9 Language Version
- [`KT-61725`](https://youtrack.jetbrains.com/issue/KT-61725) KMP: Annotation matching requirement for expect/actual leads to errors for annotations with `@OptionalExpectation`
- [`KT-47892`](https://youtrack.jetbrains.com/issue/KT-47892) False negative BREAK_OR_CONTINUE_OUTSIDE_A_LOOP with `continue` in `init` block inside `for`
- [`KT-61784`](https://youtrack.jetbrains.com/issue/KT-61784) KMP: [DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS] checker missed for companion functions
- [`KT-61173`](https://youtrack.jetbrains.com/issue/KT-61173) K2: FirProperty.hasBackingField is true for an expect val
- [`KT-59743`](https://youtrack.jetbrains.com/issue/KT-59743) K2: erroneous binding of typealias with two type parameters to a class with one type parameter
- [`KT-60650`](https://youtrack.jetbrains.com/issue/KT-60650) KMP: prohibit problematic actual typealiases
- [`KT-61461`](https://youtrack.jetbrains.com/issue/KT-61461) K2: Kotlin native metadata compilation breaks when stdlib is present in -libraries
- [`KT-61270`](https://youtrack.jetbrains.com/issue/KT-61270) Enabling Kotlin/Native caching causes 65K warnings from dsymutil when building Compose iOS app
- [`KT-58229`](https://youtrack.jetbrains.com/issue/KT-58229) K2/MPP/JVM: compiler codegen crash on call of inherited generic class's method with actual-typealias as value parameter
- [`KT-47702`](https://youtrack.jetbrains.com/issue/KT-47702) Support call of Java annotation constructor without specifying a default value
- [`KT-56460`](https://youtrack.jetbrains.com/issue/KT-56460) K2: Do not re-run DiagnosticCollectorVisitor from FirInlineDeclarationChecker.checkChildrenWithCustomVisitor
- [`KT-55933`](https://youtrack.jetbrains.com/issue/KT-55933) K2: False negative Overload resolution ambiguity for call functions with named parameters if one of params is vararg
- [`KT-59548`](https://youtrack.jetbrains.com/issue/KT-59548) FIR2IR: inconsistent generation of dispatch receiver for object methods
- [`KT-55072`](https://youtrack.jetbrains.com/issue/KT-55072) K2: False positive "suspension point is inside a critical section"
- [`KT-58778`](https://youtrack.jetbrains.com/issue/KT-58778) JVM IR inline: add fake variables for debugger
- [`KT-59404`](https://youtrack.jetbrains.com/issue/KT-59404) K2: Missing EXPECT_TYPE_IN_WHEN_WITHOUT_ELSE
- [`KT-59830`](https://youtrack.jetbrains.com/issue/KT-59830) K2. False negative [FINAL_SUPERTYPE] on extending final class through type alias
- [`KT-60580`](https://youtrack.jetbrains.com/issue/KT-60580) K2: Not supported: class org.jetbrains.kotlin.fir.types.ConeFlexibleType
- [`KT-59391`](https://youtrack.jetbrains.com/issue/KT-59391) K2: Missing JS_BUILTIN_NAME_CLASH
- [`KT-59392`](https://youtrack.jetbrains.com/issue/KT-59392) K2: Missing NAME_CONTAINS_ILLEGAL_CHARS
- [`KT-58360`](https://youtrack.jetbrains.com/issue/KT-58360) Intrinsics for atomic update of array elements
- [`KT-59165`](https://youtrack.jetbrains.com/issue/KT-59165) K2: Prohibit class literals with empty left-hand side
- [`KT-60427`](https://youtrack.jetbrains.com/issue/KT-60427) K2 `@Metadata` annotations contain outerType/outerTypeId information for non-inner nested classes
- [`KT-59376`](https://youtrack.jetbrains.com/issue/KT-59376) K2: Missing TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR
- [`KT-55221`](https://youtrack.jetbrains.com/issue/KT-55221) K2: No error reported for self-referencing local function with inferred return type
- [`KT-59586`](https://youtrack.jetbrains.com/issue/KT-59586) K2: support JVM backend diagnostics in light tree mode
- [`KT-57780`](https://youtrack.jetbrains.com/issue/KT-57780) K2: Calling a constructor through a deprecated typealias doesn't report a deprecation
- [`KT-59110`](https://youtrack.jetbrains.com/issue/KT-59110) K2. "NotImplementedError: An operation is not implemented." error on incorrect `@Target` annotation
- [`KT-59249`](https://youtrack.jetbrains.com/issue/KT-59249) K2: Empty varargs are not serialized to KLIB
- [`KT-55373`](https://youtrack.jetbrains.com/issue/KT-55373) K2. Unresolved reference error for type mismatch with callable references
- [`KT-55955`](https://youtrack.jetbrains.com/issue/KT-55955) K2: callable references are not properly resolved when in conflict with expected type
- [`KT-60144`](https://youtrack.jetbrains.com/issue/KT-60144) JVM IR inline: backport primitive boxing in class literals
- [`KT-60779`](https://youtrack.jetbrains.com/issue/KT-60779) K2: missing INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER
- [`KT-60587`](https://youtrack.jetbrains.com/issue/KT-60587) K2: Implement warning NO_REFLECTION_IN_CLASS_PATH
- [`KT-61145`](https://youtrack.jetbrains.com/issue/KT-61145) False negative NOTHING_TO_OVERRIDE when context receivers don't match
- [`KT-59378`](https://youtrack.jetbrains.com/issue/KT-59378) K2: Missing FINITE_BOUNDS_VIOLATION and FINITE_BOUNDS_VIOLATION_IN_JAVA
- [`KT-61163`](https://youtrack.jetbrains.com/issue/KT-61163) Default params on actual check and inheritance by delegation compilation error
- [`KT-60800`](https://youtrack.jetbrains.com/issue/KT-60800) [atomicfu-K/N]: turn on the tests for the K/N part of the compiler plugin
- [`KT-61029`](https://youtrack.jetbrains.com/issue/KT-61029) K2: Duplicates when processing direct overridden callables
- [`KT-55196`](https://youtrack.jetbrains.com/issue/KT-55196) K2: False-negative CONST_VAL_WITH_NON_CONST_INITIALIZER on boolean .not() call
- [`KT-60862`](https://youtrack.jetbrains.com/issue/KT-60862) Kotlin Scripting: NoSuchMethodError for ExternalDependenciesResolver.addRepository
- [`KT-57963`](https://youtrack.jetbrains.com/issue/KT-57963) K2: MPP: Annotation calls should be actualized
- [`KT-60854`](https://youtrack.jetbrains.com/issue/KT-60854) K2: IrActualizer incorrectly generates fake overrides for synthetic java properties
- [`KT-59665`](https://youtrack.jetbrains.com/issue/KT-59665) ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS isn't reported for actual typealias and fake-override actualization
- [`KT-61039`](https://youtrack.jetbrains.com/issue/KT-61039) False positive ABSTRACT_MEMBER_NOT_IMPLEMENTED in K1 when expect actual super types scopes don't match
- [`KT-61166`](https://youtrack.jetbrains.com/issue/KT-61166) Inherited platform declaration clash & accidental override
- [`KT-60531`](https://youtrack.jetbrains.com/issue/KT-60531) K2/JS: Report diagnostics before running FIR2IR
- [`KT-32275`](https://youtrack.jetbrains.com/issue/KT-32275) Embedding kotlin-compiler-embeddable into a Java EE App leads to CDI related deployment error
- [`KT-57845`](https://youtrack.jetbrains.com/issue/KT-57845) K2. Unresolved reference error on calling Java references with fully qualified name
- [`KT-58757`](https://youtrack.jetbrains.com/issue/KT-58757) K2: False-positive NON_PUBLIC_CALL_FROM_PUBLIC_INLINE error in case an inline fun is protected and is a part of an internal abstract class declaration
- [`KT-59736`](https://youtrack.jetbrains.com/issue/KT-59736) kotlinx.serialization + K2 + JS: e: java.lang.IllegalStateException: Symbol for kotlinx.serialization.json.internal/FormatLanguage.<init>|-547215418288530576[1] is unbound
- [`KT-59071`](https://youtrack.jetbrains.com/issue/KT-59071) K2/MPP: internal declarations from common module are invisible in dependent source sets if there is more that one intermediate source set between
- [`KT-61167`](https://youtrack.jetbrains.com/issue/KT-61167) Runtime failure: ReferenceError: MyPromise is not defined
- [`KT-59408`](https://youtrack.jetbrains.com/issue/KT-59408) K2: Missing MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES
- [`KT-61409`](https://youtrack.jetbrains.com/issue/KT-61409) Kotlin/Native: crash in kmm-production-sample (compose-app) with escape analysis enabled
- [`KT-57329`](https://youtrack.jetbrains.com/issue/KT-57329) K/N IR linkage issues due to the combination of static caches w/ Lazy IR & Compose compiler plugin
- [`KT-59247`](https://youtrack.jetbrains.com/issue/KT-59247) Kapt+JVM_IR: AssertionError on anonymous object in enum super constructor call
- [`KT-58576`](https://youtrack.jetbrains.com/issue/KT-58576) K2: IR actualization problems in MPP scenario
- [`KT-61442`](https://youtrack.jetbrains.com/issue/KT-61442) K2: Consider stricter filtering on implicit integer coercion
- [`KT-61441`](https://youtrack.jetbrains.com/issue/KT-61441) K2: Wrong overload is chosen with ImplicitIntegerCoercion enabled
- [`KT-59328`](https://youtrack.jetbrains.com/issue/KT-59328) K2: property with compound getter and without explicit type: compilation failure, IAE "List has more than one element" at FirDeclarationsResolveTransformer.transformFunctionWithGivenSignature()
- [`KT-61159`](https://youtrack.jetbrains.com/issue/KT-61159) K2: OVERLOAD_RESOLUTION_AMBIGUITY between private top-level property in same file and top-level property in different module
- [`KT-59233`](https://youtrack.jetbrains.com/issue/KT-59233) K2: false-negative diagnostic on creating a callable reference to a function with free type variables
- [`KT-61418`](https://youtrack.jetbrains.com/issue/KT-61418) k2: ImplicitIntegerCoercion to List leads to "IllegalStateException: Cannot find cached type parameter by FIR symbol"
- [`KT-61373`](https://youtrack.jetbrains.com/issue/KT-61373) False positive: "The opt-in annotation is redundant: no matching experimental API is used" with multiplatform code.
- [`KT-58884`](https://youtrack.jetbrains.com/issue/KT-58884) K2: NotAMockException for mock testing with lambda expression with Maven
- [`KT-58893`](https://youtrack.jetbrains.com/issue/KT-58893) K2: MockitoException for mock testing with lambda expression with Gradle
- [`KT-59483`](https://youtrack.jetbrains.com/issue/KT-59483) K2: Build a Native app
- [`KT-57738`](https://youtrack.jetbrains.com/issue/KT-57738) K2: unresolved class fields and methods in kotlin scripts
- [`KT-59449`](https://youtrack.jetbrains.com/issue/KT-59449) K2: Diagnostic messages contain debugging-style rendered FIR
- [`KT-59849`](https://youtrack.jetbrains.com/issue/KT-59849) K2: IllegalArgumentException: List has more than one element
- [`KT-57553`](https://youtrack.jetbrains.com/issue/KT-57553) Implement deprecation for open val with backing field and deferred initialization in K1
- [`KT-57230`](https://youtrack.jetbrains.com/issue/KT-57230) Support Kotlin/Wasm in the K2 platform
- [`KT-59409`](https://youtrack.jetbrains.com/issue/KT-59409) K2: Missing DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE
- [`KT-59058`](https://youtrack.jetbrains.com/issue/KT-59058) Companion object is not initialized on class constructor call
- [`KT-61017`](https://youtrack.jetbrains.com/issue/KT-61017) K2: intermediate expect/actual class results in expected class has no actual declaration in module
- [`KT-60181`](https://youtrack.jetbrains.com/issue/KT-60181) K2: "NotImplementedError: An operation is not implemented" with Spring
- [`KT-59472`](https://youtrack.jetbrains.com/issue/KT-59472) K2: build Reaktive
- [`KT-54786`](https://youtrack.jetbrains.com/issue/KT-54786) MPP: "LazyTypeAliasDescriptor cannot be cast to class org.jetbrains.kotlin.descriptors.ClassDescriptor" caused by expected non-constant function argument on iOS if class is type aliased
- [`KT-59753`](https://youtrack.jetbrains.com/issue/KT-59753) K2: NotImplementedError when using annotation with vararg with default value from other module
- [`KT-60883`](https://youtrack.jetbrains.com/issue/KT-60883) K2: Fix `testRequireKotlinCompilerVersion` in LV 2.0 branch
- [`KT-59747`](https://youtrack.jetbrains.com/issue/KT-59747) K2: cannot actualize expect class to Unit via typealias
- [`KT-61054`](https://youtrack.jetbrains.com/issue/KT-61054) K2: "IAE: source must not be null" with -no-reflect on calling property getter with implicit invoke
- [`KT-57126`](https://youtrack.jetbrains.com/issue/KT-57126) [KLIB Reproducibility] Manifest is written using os-dependent line separators
- [`KT-60850`](https://youtrack.jetbrains.com/issue/KT-60850) K2: FIR2IR generates incorrect signature for fake overrides for common declaration if it called from a platform module
- [`KT-59218`](https://youtrack.jetbrains.com/issue/KT-59218) K2: return types of calls to `@PolymorphicSignature` methods inside try-expressions don't resolve to void when required
- [`KT-60002`](https://youtrack.jetbrains.com/issue/KT-60002) K2: Missing UNSUPPORTED_SUSPEND_TEST
- [`KT-61011`](https://youtrack.jetbrains.com/issue/KT-61011) K2 Scripts: FirRecursiveProblemChecker: Expected FirResolvedTypeRef with ConeKotlinType but was FirImplicitTypeRefImplWithoutSource <implicit>
- [`KT-58906`](https://youtrack.jetbrains.com/issue/KT-58906) K2. "Backend Internal error: Exception during IR lowering" instead of CANNOT_INFER_PARAMETER_TYPE error when parameter type missing in lambda
- [`KT-59490`](https://youtrack.jetbrains.com/issue/KT-59490) K2: build km-shop
- [`KT-60163`](https://youtrack.jetbrains.com/issue/KT-60163) K2: vararg annotation argument value is serialized not as an array
- [`KT-59355`](https://youtrack.jetbrains.com/issue/KT-59355) K2: Allow to actual classifier have wider visibility than the corresponding expect class
- [`KT-56179`](https://youtrack.jetbrains.com/issue/KT-56179) [K2/N] `interop_objc_tests/multipleInheritanceClash.kt` test failed
- [`KT-59411`](https://youtrack.jetbrains.com/issue/KT-59411) K2: Missing ENUM_CLASS_CONSTRUCTOR_CALL
- [`KT-59410`](https://youtrack.jetbrains.com/issue/KT-59410) K2: Missing TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE
- [`KT-59382`](https://youtrack.jetbrains.com/issue/KT-59382) K2: Missing PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL
- [`KT-59901`](https://youtrack.jetbrains.com/issue/KT-59901) K2: Disappeared API_NOT_AVAILABLE
- [`KT-60474`](https://youtrack.jetbrains.com/issue/KT-60474) K2: False negative type mismatch for array literal with wrong numeric literal
- [`KT-59610`](https://youtrack.jetbrains.com/issue/KT-59610) K2: Calls to annotations with default values are serialized differently in K1 and K2
- [`KT-60139`](https://youtrack.jetbrains.com/issue/KT-60139) K2: Refactor handling of implicitly actual declarations (annotation & inline class constructors and property of inline class)
- [`KT-60793`](https://youtrack.jetbrains.com/issue/KT-60793) K2: IllegalStateException: Expected FirResolvedTypeRef with ConeKotlinType but was FirJavaTypeRef
- [`KT-60735`](https://youtrack.jetbrains.com/issue/KT-60735) K2: lateinit property diagnostic has not been initialized
- [`KT-60137`](https://youtrack.jetbrains.com/issue/KT-60137) K2: Quite complicated redeclaration error description is displayed for data classes
- [`KT-60639`](https://youtrack.jetbrains.com/issue/KT-60639) K2: IllegalStateException: Unsupported compile-time value GET_CLASS type=kotlin.reflect.KClass<p1.A>
- [`KT-56888`](https://youtrack.jetbrains.com/issue/KT-56888) CFA: Valid green in K1 -> red in K2. `catch_end -> finally -> after_try`
- [`KT-60723`](https://youtrack.jetbrains.com/issue/KT-60723) K2: Nested finally block has extra jump edge if surrounding try block jumps
- [`KT-60573`](https://youtrack.jetbrains.com/issue/KT-60573) K2: False positive/negative CONFLICTING_OVERLOADS for main functions
- [`KT-60124`](https://youtrack.jetbrains.com/issue/KT-60124) K2: Conflicting declarations on extension properties with different upper-bounded type parameter
- [`KT-60259`](https://youtrack.jetbrains.com/issue/KT-60259) K2: Reflection target is missing on adapted function refernces
- [`KT-59036`](https://youtrack.jetbrains.com/issue/KT-59036) InstantiationError when instantiating annotation with a parameter type as a default parameter of another annotation
- [`KT-59094`](https://youtrack.jetbrains.com/issue/KT-59094) K2: Fix Scripting K2 tests
- [`KT-59711`](https://youtrack.jetbrains.com/issue/KT-59711) K/N: Implement enumEntries intrinsic
- [`KT-59748`](https://youtrack.jetbrains.com/issue/KT-59748) K2: Return type mismatch: expected Unit, actual Any? for when with an assignment in branch
- [`KT-60154`](https://youtrack.jetbrains.com/issue/KT-60154) K2: Expected some types error
- [`KT-58139`](https://youtrack.jetbrains.com/issue/KT-58139) K2/MPP/metadata: compiler FIR serialization crash on complex expression as annotation argument
- [`KT-59485`](https://youtrack.jetbrains.com/issue/KT-59485) K2: build Anki-Android
- [`KT-59415`](https://youtrack.jetbrains.com/issue/KT-59415) K2: Missing DATA_CLASS_OVERRIDE_DEFAULT_VALUES_ERROR
- [`KT-59710`](https://youtrack.jetbrains.com/issue/KT-59710) K/JVM: Implement enumEntries intrinsic
- [`KT-57984`](https://youtrack.jetbrains.com/issue/KT-57984) K2/JS fails with IdSignature clash for inherited expect/actual function
- [`KT-59398`](https://youtrack.jetbrains.com/issue/KT-59398) K2: Missing NOT_SUPPORTED_INLINE_PARAMETER_IN_INLINE_PARAMETER_DEFAULT_VALUE
- [`KT-60645`](https://youtrack.jetbrains.com/issue/KT-60645) Native: dynamic caches are broken on Linux
- [`KT-50221`](https://youtrack.jetbrains.com/issue/KT-50221) FIR: handle enhanced/flexible nullability inside withNullability properly
- [`KT-59281`](https://youtrack.jetbrains.com/issue/KT-59281) JVM IR inline: incorrect type of created array
- [`KT-59507`](https://youtrack.jetbrains.com/issue/KT-59507) JVM IR inline: invocation of arrayOfNulls by function reference results in exception
- [`KT-58359`](https://youtrack.jetbrains.com/issue/KT-58359) Allow volatile intrinsics on inline function constant arguments
- [`KT-60598`](https://youtrack.jetbrains.com/issue/KT-60598) K2: add OptIn checkers for command line arguments
- [`KT-59766`](https://youtrack.jetbrains.com/issue/KT-59766) K2:  ISE: Cannot find cached type parameter by FIR symbol during the coroutines library build
- [`KT-59644`](https://youtrack.jetbrains.com/issue/KT-59644) K2: the companion object in an `expect` class requires to be explicitly defined for compileNativeMainKotlinMetadata
- [`KT-59640`](https://youtrack.jetbrains.com/issue/KT-59640) K2: `expect` constructor requires calling `this` or `super` but didn't use to
- [`KT-58883`](https://youtrack.jetbrains.com/issue/KT-58883) K2: False negative type mismatch for generic annotation in collection literal
- [`KT-59581`](https://youtrack.jetbrains.com/issue/KT-59581) K2: Initializer type mismatch: expected Array<KClass<*>>, actual Array<KClass<out Serializable>> in annotation parameter default value using array literal
- [`KT-59069`](https://youtrack.jetbrains.com/issue/KT-59069) K2 does not report EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL
- [`KT-59416`](https://youtrack.jetbrains.com/issue/KT-59416) K2: Missing EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT
- [`KT-59417`](https://youtrack.jetbrains.com/issue/KT-59417) K2: Missing CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE
- [`KT-59381`](https://youtrack.jetbrains.com/issue/KT-59381) K2: Missing CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM
- [`KT-59384`](https://youtrack.jetbrains.com/issue/KT-59384) K2: Missing DYNAMIC_NOT_ALLOWED
- [`KT-59406`](https://youtrack.jetbrains.com/issue/KT-59406) K2: Missing PROPERTY_DELEGATION_BY_DYNAMIC
- [`KT-60247`](https://youtrack.jetbrains.com/issue/KT-60247) K2: order of data class generated member differs in IR
- [`KT-57223`](https://youtrack.jetbrains.com/issue/KT-57223) K2: false-negative INAPPLICABLE_JVM_NAME on non-final properties outside interfaces
- [`KT-60183`](https://youtrack.jetbrains.com/issue/KT-60183) K2: INAPPLICABLE_JVM_NAME on private methods with all-open plugin
- [`KT-60120`](https://youtrack.jetbrains.com/issue/KT-60120) K2 can't get a default parameter value of expect annotation
- [`KT-57240`](https://youtrack.jetbrains.com/issue/KT-57240) K2 MPP: Actualization doesn't work for flexible types
- [`KT-60436`](https://youtrack.jetbrains.com/issue/KT-60436) K2: investigate possible FirJavaTypeRef equals parameter in FirDataFlowAnalyzer.hasEqualsOverride
- [`KT-60299`](https://youtrack.jetbrains.com/issue/KT-60299) K2: when a typealias to `Unit` is returned, an explicit `return` is now required
- [`KT-58005`](https://youtrack.jetbrains.com/issue/KT-58005) K2: Unsupported compile-time value BLOCK for Repeatable annotations
- [`KT-60223`](https://youtrack.jetbrains.com/issue/KT-60223) K2: Wrong import with import alias
- [`KT-54854`](https://youtrack.jetbrains.com/issue/KT-54854) K2. Unresolved reference for not imported declaration when it is already imported as an import alias is absent in K2
- [`KT-59738`](https://youtrack.jetbrains.com/issue/KT-59738) K2: NoSuchElementException from JvmValueClassLoweringDispatcher in MPP environment
- [`KT-59708`](https://youtrack.jetbrains.com/issue/KT-59708) K2: "Property must be initialized or be abstract" occurs due to constructors order
- [`KT-58483`](https://youtrack.jetbrains.com/issue/KT-58483) K2. -Xmulti-platform flag isn't working
- [`KT-53490`](https://youtrack.jetbrains.com/issue/KT-53490) FIR: Refactor augmented assignment resolving code - fix lhs-related problems and combine similar code in array and assign operator handling
- [`KT-59673`](https://youtrack.jetbrains.com/issue/KT-59673) K2: incorrect error message
- [`KT-58578`](https://youtrack.jetbrains.com/issue/KT-58578) K2: Commonize expect-actual logic between FIR and IR actualizer
- [`KT-54989`](https://youtrack.jetbrains.com/issue/KT-54989) FIR2IR: fragile code in postfix op detection
- [`KT-59464`](https://youtrack.jetbrains.com/issue/KT-59464) K2: Investigate cases of implicit type refs in Fir2IrImplicitCastInserter
- [`KT-53898`](https://youtrack.jetbrains.com/issue/KT-53898) K2: False negative VAL_REASSIGNMENT on member vals
- [`KT-57641`](https://youtrack.jetbrains.com/issue/KT-57641) K2: "java.lang.NoSuchFieldException: INSTANCE" in kotlin-reflect for `KClass.objectInstance` on an anonymous object
- [`KT-59299`](https://youtrack.jetbrains.com/issue/KT-59299) [K2] ISE in IrBindablePublicSymbolBase.bind on equals function from companion of serializable class
- [`KT-58844`](https://youtrack.jetbrains.com/issue/KT-58844) Incorrect type mismatch error: "actual type is kotlin/Int but kotlin/Int was expected"
- [`KT-59413`](https://youtrack.jetbrains.com/issue/KT-59413) K2: Missing VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS
- [`KT-56173`](https://youtrack.jetbrains.com/issue/KT-56173) FIR: IrGenerationExtensions cannot see default values from expect declarations
- [`KT-59611`](https://youtrack.jetbrains.com/issue/KT-59611) FIR2IR: Unsupported callable reference for enum entry with clashing name
- [`KT-59858`](https://youtrack.jetbrains.com/issue/KT-59858) Kotlin Native: Compilation failed: Sequence contains more than one matching element, org.jetbrains.kotlin.backend.konan.lower.FunctionReferenceLowering$FunctionReferenceBuilder.buildClass(FunctionReferenceLowering.kt:644)
- [`KT-58539`](https://youtrack.jetbrains.com/issue/KT-58539) [K2] Ir actualization fails to match expect/actual declarations that use custom function types
- [`KT-59775`](https://youtrack.jetbrains.com/issue/KT-59775) 'toString()' on Object returns different result with concatenation
- [`KT-59737`](https://youtrack.jetbrains.com/issue/KT-59737) K2: Actual class 'actual class FastArrayList<E> : AbstractMutableList<E>, MutableListEx<E>, RandomAccess' has no corresponding members for expected class members because of different parameter names in Java
- [`KT-59613`](https://youtrack.jetbrains.com/issue/KT-59613) K2: Unhandled intrinsic in ExpressionCodegen exception in for expect function with default value in parameter
- [`KT-59216`](https://youtrack.jetbrains.com/issue/KT-59216) K2. Unhelpful unresolved reference when inheriting from interface with constructor call (K1 reports NO_CONSTRUCTOR instead)
- [`KT-59057`](https://youtrack.jetbrains.com/issue/KT-59057) Revise muted tests for native backend
- [`KT-57377`](https://youtrack.jetbrains.com/issue/KT-57377) K2/MPP: internal declarations from common module are inivisible for intermediate modules during metadata compilation
- [`KT-59693`](https://youtrack.jetbrains.com/issue/KT-59693) MPP: linkReleaseExecutableLinux fails with IllegalStateException: Drains have not been painted properly
- [`KT-59362`](https://youtrack.jetbrains.com/issue/KT-59362) K2/MPP: `.toByte()` conversion for const val causes SourceCodeAnalysisException: java.lang.NullPointerException: null
- [`KT-51670`](https://youtrack.jetbrains.com/issue/KT-51670) FIR: questionable behavior for deprecated String constructors
- [`KT-35314`](https://youtrack.jetbrains.com/issue/KT-35314) StackOverflowError with nested try-finally and function with contracts
- [`KT-53460`](https://youtrack.jetbrains.com/issue/KT-53460) False positive smartcast warning in if block after if block
- [`KT-40851`](https://youtrack.jetbrains.com/issue/KT-40851) False MUST_BE_INITIALIZED_OR_BE_ABSTRACT error for a property which is initialised in the init block
- [`KT-59695`](https://youtrack.jetbrains.com/issue/KT-59695) K2: false negative NON_PUBLIC_CALL_FROM_PUBLIC_INLINE
- [`KT-41198`](https://youtrack.jetbrains.com/issue/KT-41198) False positive “Variable must be initialized” with assignment in scope function and safe call
- [`KT-58901`](https://youtrack.jetbrains.com/issue/KT-58901) K2. Value parameter default values are not checked for type mismatch
- [`KT-48115`](https://youtrack.jetbrains.com/issue/KT-48115) Member functions with type parameter and contract don't produce smartcasts
- [`KT-59541`](https://youtrack.jetbrains.com/issue/KT-59541) K2: Type checking has run into a recursive problem on code that was compiling with Language 1.9
- [`KT-58943`](https://youtrack.jetbrains.com/issue/KT-58943) K2: Incorrect with K1 priority of "invokeExtension + implicit receiver" candidate
- [`KT-37375`](https://youtrack.jetbrains.com/issue/KT-37375) [FIR] Incorrect invoke resolution
- [`KT-59789`](https://youtrack.jetbrains.com/issue/KT-59789) K2: self-reference does not compile anymore
- [`KT-59286`](https://youtrack.jetbrains.com/issue/KT-59286) JVM IR inline: local property not found
- [`KT-58823`](https://youtrack.jetbrains.com/issue/KT-58823) K2: Android app crashes right after start: java.lang.NoSuchMethodError: No virtual method findViewById(I)Landroid/view/View
- [`KT-57754`](https://youtrack.jetbrains.com/issue/KT-57754) K2: No public signature built for the synthesized delegate field
- [`KT-58533`](https://youtrack.jetbrains.com/issue/KT-58533) K2: "Not enough information to infer type variable T" for generic call in throw expression
- [`KT-34846`](https://youtrack.jetbrains.com/issue/KT-34846) FIR Java: enhance type parameter bounds properly
- [`KT-52043`](https://youtrack.jetbrains.com/issue/KT-52043) FIR: FirValueParameter with SubstitutionOverride does not reference the original FIR declaration
- [`KT-59291`](https://youtrack.jetbrains.com/issue/KT-59291) JVM IR inline: unexpected result of `apiVersionIsAtLeast` invocation
- [`KT-59550`](https://youtrack.jetbrains.com/issue/KT-59550) K2: synthetic property isn't seen through Java
- [`KT-59038`](https://youtrack.jetbrains.com/issue/KT-59038) [K2] IllegalStateException in mixed Java/Kotlin inheritance
- [`KT-59489`](https://youtrack.jetbrains.com/issue/KT-59489) K2: builld spring-petclinic-kotlin
- [`KT-58908`](https://youtrack.jetbrains.com/issue/KT-58908) K2. Internal error "kotlin.UninitializedPropertyAccessException: lateinit property firType has not been initialized" on incomplete `is`
- [`KT-56755`](https://youtrack.jetbrains.com/issue/KT-56755) K2: Investigate failures related to line numbers with LT compilation enabled
- [`KT-56139`](https://youtrack.jetbrains.com/issue/KT-56139) K2: consider adding source element for implicit receivers
- [`KT-57489`](https://youtrack.jetbrains.com/issue/KT-57489) K2: Incorrectly generated line numbers in companion object access inside class
- [`KT-58947`](https://youtrack.jetbrains.com/issue/KT-58947) Run all existing codegen box tests with kapt stub generation
- [`KT-58827`](https://youtrack.jetbrains.com/issue/KT-58827) K2 reports ACTUAL_WITHOUT_EXPECT on the whole class
- [`KT-54917`](https://youtrack.jetbrains.com/issue/KT-54917) K2: ILT leak from a completed generic call
- [`KT-56187`](https://youtrack.jetbrains.com/issue/KT-56187) K2: type parameter's upper bound is ignored in callable references
- [`KT-56186`](https://youtrack.jetbrains.com/issue/KT-56186) K2: lack of type arguments in type constructor is ignored in callable references
- [`KT-59356`](https://youtrack.jetbrains.com/issue/KT-59356) K2: Restrict rules for matching of expect supertypes for actual class
- [`KT-57217`](https://youtrack.jetbrains.com/issue/KT-57217) K2: NoSuchMethodError on `toChar` call on java inheritor of java.lang.Number
- [`KT-58356`](https://youtrack.jetbrains.com/issue/KT-58356) K2: StackOverflowError with OptIn and Deprecated, while compiling Kotlin project
- [`KT-57954`](https://youtrack.jetbrains.com/issue/KT-57954) K2.  Auto-generated "entries" member of enum class has higher priority than user-declared companion object with same name when language version is set to 2.0
- [`KT-59508`](https://youtrack.jetbrains.com/issue/KT-59508) K2: Make sure that warnings-severity nullability annotations are not perceived as reasons for nullability errors
- [`KT-53820`](https://youtrack.jetbrains.com/issue/KT-53820) FIR: mismatching error message for invisible reference/member
- [`KT-58641`](https://youtrack.jetbrains.com/issue/KT-58641) K2: PublishedApi has no effect when internal fun used in the test source set
- [`KT-59461`](https://youtrack.jetbrains.com/issue/KT-59461) K2: Erroneous null check when returning not-null typealias to nullable type
- [`KT-58980`](https://youtrack.jetbrains.com/issue/KT-58980) K2: Import of java field from companion's base breaks the compiler
- [`KT-59140`](https://youtrack.jetbrains.com/issue/KT-59140) K2: "Symbol public final static field is invisible" caused by java static field called in kotlin code
- [`KT-59501`](https://youtrack.jetbrains.com/issue/KT-59501) Escape analysis constructs arrays of negative size
- [`KT-59452`](https://youtrack.jetbrains.com/issue/KT-59452) apiVersionIsAtLeast calls in body of stdlib inline function may be evaluated on compile-time
- [`KT-53967`](https://youtrack.jetbrains.com/issue/KT-53967) [PL] Classifiers: Turning interface from fun to non-fun + adding member function causes Kotlin/JS fail: IAE: "Sequence contains more than one matching element"
- [`KT-59346`](https://youtrack.jetbrains.com/issue/KT-59346) Not working breakpoints on not initialized variables
- [`KT-55993`](https://youtrack.jetbrains.com/issue/KT-55993) Wrong current pointer: strange behaviour of debugger or compiler when two IFs and an uninitialized variable between them
- [`KT-58335`](https://youtrack.jetbrains.com/issue/KT-58335) K2: Exposed typealias from implementation dependency produces type mismatch in dependent module
- [`KT-58719`](https://youtrack.jetbrains.com/issue/KT-58719) K2: false-positive INVISIBLE_REFERENCE error in case of importing an internal abstract class
- [`KT-57694`](https://youtrack.jetbrains.com/issue/KT-57694) K2: False positive [NOTHING_TO_OVERRIDE] for a class overriding 'sort' method from the List collection
- [`KT-58460`](https://youtrack.jetbrains.com/issue/KT-58460) K2. return without argument became allowed for functions with return type Any
- [`KT-49249`](https://youtrack.jetbrains.com/issue/KT-49249) Incorrect nullability inferred for Throwable
- [`KT-57429`](https://youtrack.jetbrains.com/issue/KT-57429) K2: Fix computing a mangled name for members of a generic class that reference the class's type parameters in their signature
- [`KT-57566`](https://youtrack.jetbrains.com/issue/KT-57566) K2: Fix name mangling for functions that have dynamic type in their signature
- [`KT-57818`](https://youtrack.jetbrains.com/issue/KT-57818) K2: Fix FirMangleComputer to not include the "special" package name into mangled names of property accessors on non-JVM platforms
- [`KT-57777`](https://youtrack.jetbrains.com/issue/KT-57777) K2: Fix computing a mangled name for the synthesized `entries` property getter of an enum class
- [`KT-57433`](https://youtrack.jetbrains.com/issue/KT-57433) K2: Fix computing a mangled name for top-level functions and properties
- [`KT-58553`](https://youtrack.jetbrains.com/issue/KT-58553) k2: Annotation type arguments are lost in FIR2IR
- [`KT-58184`](https://youtrack.jetbrains.com/issue/KT-58184) K2: False negative INVISIBLE_MEMBER on destructuring declaration
- [`KT-58637`](https://youtrack.jetbrains.com/issue/KT-58637) K2: False negative ABSTRACT_MEMBER_NOT_IMPLEMENTED on Entry of Enum with abstract member declaration
- [`KT-54952`](https://youtrack.jetbrains.com/issue/KT-54952) JvmSerializationBindings does not work with K2
- [`KT-54844`](https://youtrack.jetbrains.com/issue/KT-54844) FIR/Analysis API: create stubs for equals/hashCode/toString for data classes in FIR
- [`KT-58555`](https://youtrack.jetbrains.com/issue/KT-58555) K2: Generic property reference inside delegation misses type argument
- [`KT-57648`](https://youtrack.jetbrains.com/issue/KT-57648) FIR: move deprecation calculation on COMPILER_REQUIRED_ANNOTATIONS phase
- [`KT-57049`](https://youtrack.jetbrains.com/issue/KT-57049) K2 generates duplicates of symbols/declarations
- [`KT-55723`](https://youtrack.jetbrains.com/issue/KT-55723) K2: deprecations for enum entries are not resolved on the TYPES phase
- [`KT-59033`](https://youtrack.jetbrains.com/issue/KT-59033) Doesn’t support vararg parameter in annotation instantiation with empty arguments
- [`KT-58780`](https://youtrack.jetbrains.com/issue/KT-58780) JVM IR inline: local property delegation is not working for K2
- [`KT-58779`](https://youtrack.jetbrains.com/issue/KT-58779) JVM IR inline: correctly process special inlined block in value class lowering
- [`KT-58720`](https://youtrack.jetbrains.com/issue/KT-58720) Generate full InnerClass attributes for the standard library
- [`KT-58215`](https://youtrack.jetbrains.com/issue/KT-58215) K2: JVM IR produces line numbers for delegation bridges that are not marked with ACC_BRIDGE
- [`KT-42696`](https://youtrack.jetbrains.com/issue/KT-42696) JVM IR generates line numbers for all bridges leading to extra steps in the debugger
- [`KT-57228`](https://youtrack.jetbrains.com/issue/KT-57228) K2: annotations for interface member properties implemented by delegation are copied
- [`KT-57216`](https://youtrack.jetbrains.com/issue/KT-57216) K2: non-trivial enum declaration does not have ACC_FINAL in the bytecode
- [`KT-55866`](https://youtrack.jetbrains.com/issue/KT-55866) K2: Constant as parameter of `@JvmName`: BE: "Unsupported compile-time value CALL private final fun <get-TAG>"
- [`KT-58717`](https://youtrack.jetbrains.com/issue/KT-58717) Object on the left-hand side of callable reference is not initialized if `KCallable.name` optimization is used
- [`KT-59211`](https://youtrack.jetbrains.com/issue/KT-59211) Kapt+JVM_IR: AssertionError on delegating to anonymous object
- [`KT-57251`](https://youtrack.jetbrains.com/issue/KT-57251) K2: weird error message when trying to instantiate an `expect` class without explicit constructor
- [`KT-58623`](https://youtrack.jetbrains.com/issue/KT-58623) Language version 2.0: compiling into common, Native does not report "Protected function call from public-API inline function is prohibited", while JVM, JS do
- [`KT-55945`](https://youtrack.jetbrains.com/issue/KT-55945) NoSuchMethodError when calling method with value class parameter on java class inherited from kotlin class
- [`KT-58840`](https://youtrack.jetbrains.com/issue/KT-58840) K1/K2: false positive EXPOSED_FUNCTION_RETURN_TYPE related to protected lower bound
- [`KT-57243`](https://youtrack.jetbrains.com/issue/KT-57243) K2: no warning or error reported on expect class in CLI, and JVM backend tries to generate it to a .class file
- [`KT-57833`](https://youtrack.jetbrains.com/issue/KT-57833) K2 reports NO_ACTUAL_FOR_EXPECT for inherited properties with the same name
- [`KT-58153`](https://youtrack.jetbrains.com/issue/KT-58153) K2/MPP/JVM&Native: cannot override Any::toString when an expect-supertype has Any::toString override in actual-class
- [`KT-58124`](https://youtrack.jetbrains.com/issue/KT-58124) K2: FIR2IR compiler crash with MPP (Fir2IrSimpleFunctionSymbol is already bound)
- [`KT-58346`](https://youtrack.jetbrains.com/issue/KT-58346) k2: false negative MUST_BE_INITIALIZED for deferred initialization
- [`KT-57803`](https://youtrack.jetbrains.com/issue/KT-57803) K2. "Kotlin: Only the Kotlin standard library is allowed to use the 'kotlin' package" error missing in 2.0
- [`KT-57504`](https://youtrack.jetbrains.com/issue/KT-57504) [K2/N] Wrong coercion of `ILT: 7` to kotlinx.cinterop.COpaquePointer causes `Cannot adapt kotlin.Int to kotlinx.cinterop.CPointer` during autoboxing
- [`KT-57484`](https://youtrack.jetbrains.com/issue/KT-57484) K2: false positive OVERLOAD_RESOLUTION_AMBIGUITY with ImplicitIntegerCoercion
- [`KT-57971`](https://youtrack.jetbrains.com/issue/KT-57971) K1/K2: False positive "Redundant 'suspend' modifier" warning on declaration site when suspend function is also argument
- [`KT-56779`](https://youtrack.jetbrains.com/issue/KT-56779) Checkers false negative: AbstractMethodError when accessing setter via an interface where the member is defined as var, but it's val in implementation
- [`KT-51793`](https://youtrack.jetbrains.com/issue/KT-51793) FIR: Investigate property+invoke resolution priorities
- [`KT-57003`](https://youtrack.jetbrains.com/issue/KT-57003) FIR: missing annotation on parameter of `data` class' synthetic `copy`
- [`KT-57269`](https://youtrack.jetbrains.com/issue/KT-57269) K2: collection stub for `sort` is not generated for custom List subclasses
- [`KT-54748`](https://youtrack.jetbrains.com/issue/KT-54748) K2: incomprehensible errors when type parameter has the same name as a class
- [`KT-50703`](https://youtrack.jetbrains.com/issue/KT-50703) FIR: Improve reporting UPPER_BOUND_VIOLATED for type arguments of typealias constructor calls
- [`KT-57622`](https://youtrack.jetbrains.com/issue/KT-57622) Fix incorrect metadata for data class generated methods
- [`KT-54887`](https://youtrack.jetbrains.com/issue/KT-54887) K2: fix behavior of references to value classes equals/hashCode/toString
- [`KT-58937`](https://youtrack.jetbrains.com/issue/KT-58937) K2: Annotation vararg arguments are incorrectly serialized
- [`KT-58621`](https://youtrack.jetbrains.com/issue/KT-58621) K2: Private class shadows public function defined in the same package
- [`KT-59041`](https://youtrack.jetbrains.com/issue/KT-59041) K2. "IllegalStateException: Expected FirResolvedTypeRef with ConeKotlinType but was FirImplicitTypeRefImplWithoutSource <implicit>" on incorrect collection declaration
- [`KT-58665`](https://youtrack.jetbrains.com/issue/KT-58665) K2: Optional.of incorrectly accepts nullable String
- [`KT-58938`](https://youtrack.jetbrains.com/issue/KT-58938) K2. Abstract class can be invoked using member reference `::` operator
- [`KT-50798`](https://youtrack.jetbrains.com/issue/KT-50798) FIR: False negative UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION
- [`KT-58944`](https://youtrack.jetbrains.com/issue/KT-58944) K2. StackOverflowError on incorrect intersection types
- [`KT-59241`](https://youtrack.jetbrains.com/issue/KT-59241) K2: broken inference of DNN types
- [`KT-58294`](https://youtrack.jetbrains.com/issue/KT-58294) K2 compiler crashes with OOM on deserializing annotation applied to itself with a enum outer/nested parameter
- [`KT-58972`](https://youtrack.jetbrains.com/issue/KT-58972) K2: Error message of PRIVATE_CLASS_MEMBER_FROM_INLINE doesn't mention class members
- [`KT-58989`](https://youtrack.jetbrains.com/issue/KT-58989) K2: Forbid suspend operator get/setValue and provideDelegate
- [`KT-59177`](https://youtrack.jetbrains.com/issue/KT-59177) K2: Report NAMED_ARGUMENTS_NOT_ALLOWED for named parameters in lambdas
- [`KT-57028`](https://youtrack.jetbrains.com/issue/KT-57028) K2: "NSEE: Sequence contains no element matching the predicate" with stream related Java api
- [`KT-58007`](https://youtrack.jetbrains.com/issue/KT-58007) K2: Unsupported compile-time value GET_FIELD FIELD PROPERTY_BACKING_FIELD when const value is default for annotation
- [`KT-58472`](https://youtrack.jetbrains.com/issue/KT-58472) Secondary constructor breaks MUST_BE_INITIALIZED check
- [`KT-59022`](https://youtrack.jetbrains.com/issue/KT-59022) Make is and as behaviour consistent in Native
- [`KT-58902`](https://youtrack.jetbrains.com/issue/KT-58902) K2: Calls to overridden method with default parameter are not compiled
- [`KT-58549`](https://youtrack.jetbrains.com/issue/KT-58549) K2: variable type is infered to non-existing interface
- [`KT-58613`](https://youtrack.jetbrains.com/issue/KT-58613) K2: ConcurrentModificationException from FirSignatureEnhancement.performFirstRoundOfBoundsResolution
- [`KT-55552`](https://youtrack.jetbrains.com/issue/KT-55552) K2. False negative TYPE_MISMATCH in implementation via delegation
- [`KT-57436`](https://youtrack.jetbrains.com/issue/KT-57436) Fix computing mangled names of generic properties from IR-based declaration descriptors
- [`KT-58543`](https://youtrack.jetbrains.com/issue/KT-58543) [K2/N] Rewrite native MPP tests to avoid expect actual in same module
- [`KT-57701`](https://youtrack.jetbrains.com/issue/KT-57701) Unify selection of inherited callable with default implementation among multiple candidates in JVM, Native & JS backends
- [`KT-58444`](https://youtrack.jetbrains.com/issue/KT-58444) K2/MPP/metadata: compiler FIR2IR crash on constant with intrinsic initializer from common source set in Native-shared source set
- [`KT-57756`](https://youtrack.jetbrains.com/issue/KT-57756) K2: Missing syntax errors when light tree parsing is used
- [`KT-57435`](https://youtrack.jetbrains.com/issue/KT-57435) Fix computing mangled names for functions with context receivers
- [`KT-57219`](https://youtrack.jetbrains.com/issue/KT-57219) K2: incorrect relative order of normal and use-site-targeted annotations on property getter in the resulting bytecode
- [`KT-57955`](https://youtrack.jetbrains.com/issue/KT-57955) K2: "ClassCastException: class org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl cannot be cast to class org.jetbrains.kotlin.ir.declarations.IrDeclaration" with property delegate
- [`KT-58583`](https://youtrack.jetbrains.com/issue/KT-58583) K2: false-positive invisible reference error on nested anonymous object literal extending a protected nested class
- [`KT-57425`](https://youtrack.jetbrains.com/issue/KT-57425) K2: False-positive smartcast on property accessed through a property from another module
- [`KT-57844`](https://youtrack.jetbrains.com/issue/KT-57844) K2. Not relevant errors when accessing Java member which have private overloads with argument type mismatch
- [`KT-58584`](https://youtrack.jetbrains.com/issue/KT-58584) K2: "UninitializedPropertyAccessException: lateinit property packageFqName has not been initialized"
- [`KT-58529`](https://youtrack.jetbrains.com/issue/KT-58529) K2: "Extension function type is not allowed as supertypes" compile error
- [`KT-58379`](https://youtrack.jetbrains.com/issue/KT-58379) K2: NEW_INFERENCE_ERROR in sortedBy call with exception in branch
- [`KT-58284`](https://youtrack.jetbrains.com/issue/KT-58284) K2: False negative ITERATOR_MISSING
- [`KT-55078`](https://youtrack.jetbrains.com/issue/KT-55078) K2 IDE: Infinite recursion in `org.jetbrains.kotlin.fir.java.JavaScopeProvider#findJavaSuperClass`
- [`KT-58080`](https://youtrack.jetbrains.com/issue/KT-58080) K2: False-positive TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM on annotated const val
- [`KT-58674`](https://youtrack.jetbrains.com/issue/KT-58674) K2: No expected type for while loop condition
- [`KT-56523`](https://youtrack.jetbrains.com/issue/KT-56523) K2 should report MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED
- [`KT-58238`](https://youtrack.jetbrains.com/issue/KT-58238) Support dumping signatures and mangled names in irText tests
- [`KT-58456`](https://youtrack.jetbrains.com/issue/KT-58456) K2: Custom function type metadata breaks Compose library compatibility
- [`KT-58267`](https://youtrack.jetbrains.com/issue/KT-58267) K/N: do not reference hidden Array.content* functions from the compiler
- [`KT-57791`](https://youtrack.jetbrains.com/issue/KT-57791) Native: Method returning String? leads to exception: Unexpected receiver type: kotlin.String
- [`KT-58437`](https://youtrack.jetbrains.com/issue/KT-58437) K2: Do not use descriptors in KonanSymbols
- [`KT-57432`](https://youtrack.jetbrains.com/issue/KT-57432) K2: Don't create default getters and setters in case when they are not needed
- [`KT-46047`](https://youtrack.jetbrains.com/issue/KT-46047) FIR: incorrect type of integer literals
- [`KT-57487`](https://youtrack.jetbrains.com/issue/KT-57487) [K2/N] Stdlib ArraysTest fails with `Class found but error nodes are not allowed`
- [`KT-56951`](https://youtrack.jetbrains.com/issue/KT-56951) K2: False negative error on compound assignment for property of type Byte
- [`KT-57222`](https://youtrack.jetbrains.com/issue/KT-57222) K2: compiler FIR serialization crash on two functions with captured type and object literal
- [`KT-58224`](https://youtrack.jetbrains.com/issue/KT-58224) K2: deprecation on field is not detected properly
- [`KT-55662`](https://youtrack.jetbrains.com/issue/KT-55662) K2. Incorrect type mismatch error "inferred type is IOT" instead of "inferred type is Int"
- [`KT-55668`](https://youtrack.jetbrains.com/issue/KT-55668) K2. 'in' modifier became applicable to star projection
- [`KT-57064`](https://youtrack.jetbrains.com/issue/KT-57064) K2: hidden internals of dealing with type-aliased primitive types are exposed to user
- [`KT-58252`](https://youtrack.jetbrains.com/issue/KT-58252) K2: Symbol already bound for backing field during building resulting JS artifact for MPP project
- [`KT-56940`](https://youtrack.jetbrains.com/issue/KT-56940) K/Wasm: report compiler errors for unsupported external declarations
- [`KT-56943`](https://youtrack.jetbrains.com/issue/KT-56943) K/Wasm: implement `@WasmImport` diagnostics
- [`KT-55903`](https://youtrack.jetbrains.com/issue/KT-55903) K2: False negative CANNOT_CHECK_FOR_ERASED on is-check for type with reified type arguments
- [`KT-56944`](https://youtrack.jetbrains.com/issue/KT-56944) K/Wasm: implement `@JsFun` diagnostics
- [`KT-58329`](https://youtrack.jetbrains.com/issue/KT-58329) K2: False-positive suspend conversion for anonymous functions
- [`KT-58028`](https://youtrack.jetbrains.com/issue/KT-58028) K2: False-positive TYPE_PARAMETER_IS_NOT_AN_EXPRESSION

### Docs & Examples

- [`KT-60545`](https://youtrack.jetbrains.com/issue/KT-60545) Documentation change on Interoperability with Swift/Objective-C: highlight that it is not normal to suppress errors
- [`KT-50927`](https://youtrack.jetbrains.com/issue/KT-50927) Kotlin / Docs: Delete all the information about old Kotlin/Wasm
- [`KT-61398`](https://youtrack.jetbrains.com/issue/KT-61398) Advertise hierarchy templates in 1.9.20-Beta what's new

### IDE

#### New Features

- [`KTIJ-23199`](https://youtrack.jetbrains.com/issue/KTIJ-23199) K2 IDE: Improve Import quick fix description
- [`KTIJ-26056`](https://youtrack.jetbrains.com/issue/KTIJ-26056) Support highlighting of KNM files

#### Performance Improvements

- [`KTIJ-26688`](https://youtrack.jetbrains.com/issue/KTIJ-26688) UAST: optimize methodNameCanBeOneOf

#### Fixes

- [`KTIJ-26782`](https://youtrack.jetbrains.com/issue/KTIJ-26782) Internal error while highlighting "AndroidHighlighterExtension does not define or inherit highlightDeclaration"
- [`KTIJ-27188`](https://youtrack.jetbrains.com/issue/KTIJ-27188) Bundled DevKit plugin + 1.9.20-Beta* constantly throws exceptions when opening another plugin codebase
- [`KTIJ-25220`](https://youtrack.jetbrains.com/issue/KTIJ-25220) Kotlin not configured dialog does not show if Kotlin stdlib is anywhere on classpath
- [`KTIJ-25563`](https://youtrack.jetbrains.com/issue/KTIJ-25563) Failed cinterop task becomes UP-TO-DATE and successfully passes on the second import
- [`KTIJ-26536`](https://youtrack.jetbrains.com/issue/KTIJ-26536) IDE in Java file resolves to property with the same name instead of method in the nested class from library
- [`KTIJ-25126`](https://youtrack.jetbrains.com/issue/KTIJ-25126) K2 IDE. No import quickfix for Java static members
- [`KT-60341`](https://youtrack.jetbrains.com/issue/KT-60341) K2 IDE: "UnsupportedOperationException: Unknown type CapturedType(*)?"
- [`KTIJ-25960`](https://youtrack.jetbrains.com/issue/KTIJ-25960) K2 IDE: KDoc references to static java methods are not resolved
- [`KTIJ-7642`](https://youtrack.jetbrains.com/issue/KTIJ-7642) HMPP, IDE: False positive ''suspend' modifier is not allowed on a single abstract member' for common code if JVM target present
- [`KTIJ-25745`](https://youtrack.jetbrains.com/issue/KTIJ-25745) K2 IDE: "Type info" intention shows the return type of a functional type instead of the functional type itself
- [`KTIJ-26501`](https://youtrack.jetbrains.com/issue/KTIJ-26501) K2: IDE K2: False positive unused import when declaration used for vararg parameter type
- [`KTIJ-26661`](https://youtrack.jetbrains.com/issue/KTIJ-26661) K2 IDE. PIEAE “Element class CompositeElement of type FUN” after removing/putting back function with operator modifier
- [`KTIJ-26672`](https://youtrack.jetbrains.com/issue/KTIJ-26672) K2 IDE: false positive in optimize import for ambiguity calls
- [`KTIJ-26760`](https://youtrack.jetbrains.com/issue/KTIJ-26760) K2 IDE: OVERLOAD_RESOLUTION_AMBIGUITY false positive
- [`KTIJ-26867`](https://youtrack.jetbrains.com/issue/KTIJ-26867) K2 IDE: rename refactoring doesn't rename subclasses if they are used in import directives
- [`KTIJ-26848`](https://youtrack.jetbrains.com/issue/KTIJ-26848) K2 IDE: index inconsistency in case of "<no name provided>" name
- [`KTIJ-26666`](https://youtrack.jetbrains.com/issue/KTIJ-26666) K2 IDE: changed FirFile is treated as fully resolved after in-block modification
- [`KT-59836`](https://youtrack.jetbrains.com/issue/KT-59836) Symbol Light Classes: Type parameters from the parent interface aren't copied to DefaultImpls methods
- [`KT-28611`](https://youtrack.jetbrains.com/issue/KT-28611) MPP: Gradle -> IDE: settings provided via `compilations` DSL are not imported into common modules facets
- [`KTIJ-25448`](https://youtrack.jetbrains.com/issue/KTIJ-25448) When project JDK is less than one defines in jvmToolchain block, run with Idea fails with `has been compiled by a more recent version of the Java Runtime`
- [`KT-60603`](https://youtrack.jetbrains.com/issue/KT-60603) K2: Investigate intellij tests failures in branch 2.0
- [`KTIJ-25364`](https://youtrack.jetbrains.com/issue/KTIJ-25364) K2 IDE: References to Java records are red: OVERLOAD_RESOLUTION_AMBIGUITY, UNRESOLVED_REFERENCE
- [`KTIJ-24390`](https://youtrack.jetbrains.com/issue/KTIJ-24390) Kotlin assignment plugin: Imports are not recognized in build logic .kt files for Gradle build
- [`KT-60590`](https://youtrack.jetbrains.com/issue/KT-60590) Fix light classes related tests in branch 2.0
- [`KT-60530`](https://youtrack.jetbrains.com/issue/KT-60530) K2 scripting: exception on .gradle.kts opening
- [`KT-60539`](https://youtrack.jetbrains.com/issue/KT-60539) K2: "KtInaccessibleLifetimeOwnerAccessException: org.jetbrains.kotlin.analysis.api.lifetime.KtReadActionConfinementLifetimeToken`@3ce52fd9` is inaccessible: Using KtLifetimeOwner from previous analysis" at highlighting
- [`KTIJ-26276`](https://youtrack.jetbrains.com/issue/KTIJ-26276) K2 IDE: Optimize import drops used import alias
- [`KT-60518`](https://youtrack.jetbrains.com/issue/KT-60518) K2 IDE. False positive [NON_MEMBER_FUNCTION_NO_BODY] when completing function with `Complete current statement`
- [`KT-60323`](https://youtrack.jetbrains.com/issue/KT-60323) K2 IDE. "KotlinExceptionWithAttachments: Unexpected returnTypeRef. Expected is FirResolvedTypeRef, but was FirImplicitTypeRefImpl" exception on contract return type
- [`KT-60352`](https://youtrack.jetbrains.com/issue/KT-60352) K2 IDE. Support Java Records
- [`KT-56503`](https://youtrack.jetbrains.com/issue/KT-56503) K2 IDE: FIR tree is incorrect in a case of ProcessCancelledException was thrown during phase execution
- [`KTIJ-25653`](https://youtrack.jetbrains.com/issue/KTIJ-25653) K2 IDE. "KotlinExceptionWithAttachments: Containing function should be not null for KtParameter" exception on incorrect derived class declaration
- [`KT-59843`](https://youtrack.jetbrains.com/issue/KT-59843) SLC: `KotlinAsJavaSupport.packageExists` (via `KotlinStaticPackageProvider`) said ROOT package doesn't exist if no `KtFile`s are given
- [`KTIJ-26206`](https://youtrack.jetbrains.com/issue/KTIJ-26206) Support retrieving KtType from annotation constructor calls on getters and setters
- [`KT-59445`](https://youtrack.jetbrains.com/issue/KT-59445) Recursion detected on input: JavaAnnotationImpl
- [`KTIJ-26066`](https://youtrack.jetbrains.com/issue/KTIJ-26066) K2 IDE. "KotlinExceptionWithAttachments: Unexpected returnTypeRef. Expected is FirResolvedTypeRef, but was FirImplicitTypeRefImpl" on attempt to set contract
- [`KTIJ-26085`](https://youtrack.jetbrains.com/issue/KTIJ-26085) K2 IDE: treat psi modification of a contact inside a body as OOBM
- [`KTIJ-25869`](https://youtrack.jetbrains.com/issue/KTIJ-25869) K2 IDE. Expected FirResolvedTypeRef for return type of FirValueParameterImpl(Source) but FirImplicitTypeRefImplWithoutSource was found
- [`KTIJ-24272`](https://youtrack.jetbrains.com/issue/KTIJ-24272) K2 IDE: "Expected some types"
- [`KTIJ-24730`](https://youtrack.jetbrains.com/issue/KTIJ-24730) K2 IDE. IllegalStateException on absence of opening bracket in main() function
- [`KT-59533`](https://youtrack.jetbrains.com/issue/KT-59533) AA/SLC: anonymous object appears during PsiType conversion, resulting in IllegalArgumentException:KtFirPsiTypeProviderKt.asPsiTypeElement
- [`KT-59563`](https://youtrack.jetbrains.com/issue/KT-59563) Symbol Light Classes: Incorrect type erasure in $annotations methods for extension properties with generic parameters
- [`KT-57567`](https://youtrack.jetbrains.com/issue/KT-57567) SLC: missing `final` modifier on enum (non-synthetic) members
- [`KT-59537`](https://youtrack.jetbrains.com/issue/KT-59537) SLC: SymbolLightClassForAnonymousObject with null parent
- [`KTIJ-24121`](https://youtrack.jetbrains.com/issue/KTIJ-24121) K2 IDE. "failed to convert element KtLightField" when trying to declare property after function that has return with type mismatch
- [`KTIJ-25335`](https://youtrack.jetbrains.com/issue/KTIJ-25335) K2 IDE. "failed to convert element KtLightField:<no name provided>" on attempt to set property in class with constructor
- [`KT-59293`](https://youtrack.jetbrains.com/issue/KT-59293) Symbol Light Classes: DefaultImpls methods must be static and have an additional $this parameter
- [`KTIJ-25976`](https://youtrack.jetbrains.com/issue/KTIJ-25976) K2 IDE: Fix "Unsupported compiled declaration of type" for type parameters
- [`KT-59325`](https://youtrack.jetbrains.com/issue/KT-59325) Symbol Light Classes: Non-existing fields for properties from companion objects
- [`KT-57579`](https://youtrack.jetbrains.com/issue/KT-57579) SLC: unboxed type argument as method return type
- [`KT-54804`](https://youtrack.jetbrains.com/issue/KT-54804) Generate synthetic functions for annotations on properties in light classes
- [`KT-56200`](https://youtrack.jetbrains.com/issue/KT-56200) Kotlin FIR reference resolve exception leaks user code
- [`KT-58448`](https://youtrack.jetbrains.com/issue/KT-58448) K2 / IDE / SLC: `findAttributeValue` for attribute w/ default value raises ClassCastException

### IDE. Completion

#### Fixes

- [`KTIJ-26518`](https://youtrack.jetbrains.com/issue/KTIJ-26518) K2 IDE: Code completion does not insert import when completing a type in the vararg position
- [`KTIJ-26713`](https://youtrack.jetbrains.com/issue/KTIJ-26713) K2 IDE: Code completion does not insert import when completing a type inside a functional type
- [`KTIJ-26597`](https://youtrack.jetbrains.com/issue/KTIJ-26597) K2 IDE: "Change return type" quick fix adds full qualified name to anonymous function
- [`KTIJ-26384`](https://youtrack.jetbrains.com/issue/KTIJ-26384) K2 IDE: Extension functions completion should recognize context receivers
- [`KTIJ-26419`](https://youtrack.jetbrains.com/issue/KTIJ-26419) K2 IDE: Completion in anonymous function inside when branch expression does not account for smart cast
- [`KTIJ-26629`](https://youtrack.jetbrains.com/issue/KTIJ-26629) K2 IDE: Completion of types in anonymous function return is not shortened
- [`KTIJ-26599`](https://youtrack.jetbrains.com/issue/KTIJ-26599) K2 IDE: Typing `do ... while` statement: InvalidFirElementTypeException: "For DO_WHILE with text... FirExpression expected, but FirDoWhileLoopImpl found"
- [`KTIJ-26113`](https://youtrack.jetbrains.com/issue/KTIJ-26113) K2 IDE: Completion in when branch does not account for smart casts if `else` branch is present
- [`KT-60451`](https://youtrack.jetbrains.com/issue/KT-60451) K2 IDE: FirDeclaration was not found for class org.jetbrains.kotlin.psi.KtProperty, fir is class org.jetbrains.kotlin.fir.expressions.impl.FirBlockImpl
- [`KTIJ-21103`](https://youtrack.jetbrains.com/issue/KTIJ-21103) FIR IDE: implement completion In Kdoc
- [`KTIJ-24096`](https://youtrack.jetbrains.com/issue/KTIJ-24096) K2 IDE: Completion should insert the fully-qualified class name when the short class name clashes with a name from scope
- [`KTIJ-25116`](https://youtrack.jetbrains.com/issue/KTIJ-25116) K2 IDE: Name shortening in constructor's parameters affects constructor
- [`KTIJ-19863`](https://youtrack.jetbrains.com/issue/KTIJ-19863) Bad completion variants inside annotations

### IDE. Decompiler, Indexing, Stubs

- [`KTIJ-26706`](https://youtrack.jetbrains.com/issue/KTIJ-26706) Bytecode viewer: "IllegalStateException: Couldn't find declaration file" for a file with a delegated property with inline accessor in another module
- [`KTIJ-25465`](https://youtrack.jetbrains.com/issue/KTIJ-25465) IDE hangs when indexing Kotlin project
- [`KTIJ-25979`](https://youtrack.jetbrains.com/issue/KTIJ-25979) K2 IDE: 'java.lang.IllegalStateException: Attempt to load decompiled text, please use stubs instead' exception if navigate to the decompiled KGP sources
- [`KTIJ-25985`](https://youtrack.jetbrains.com/issue/KTIJ-25985) Stub mismatch for names with special characters

### IDE. Gradle Integration

- [`KTIJ-25334`](https://youtrack.jetbrains.com/issue/KTIJ-25334) Gradle 8.1: Unresolved references in IDE for build.gradle.kts
- [`KT-61777`](https://youtrack.jetbrains.com/issue/KT-61777) Explicit API mode isn't reflected in IDE settings unless every task is configured with Gradle
- [`KTIJ-26306`](https://youtrack.jetbrains.com/issue/KTIJ-26306) apiLevel (API version) for Kotlin/Native modules is set to 1.8 with KGP 1.9 and IDE Plugin 1.9.0-XXX, if the compiler bundled to IDE Plugin is still 1.8
- [`KT-61172`](https://youtrack.jetbrains.com/issue/KT-61172) MPP: Stacktraces of diagnostics are always printed during IDEA sync
- [`KT-48554`](https://youtrack.jetbrains.com/issue/KT-48554) [Multiplatform Import] Ensure consistency between `GradleImportProperties` and `PropertiesProvider`
- [`KT-36677`](https://youtrack.jetbrains.com/issue/KT-36677) MPP Gradle plugin doesn't respect manually set compiler arg `-opt-in`
- [`KT-58696`](https://youtrack.jetbrains.com/issue/KT-58696) MPP + IDEA: tryK2 does not affect LV value of common facets
- [`KT-53875`](https://youtrack.jetbrains.com/issue/KT-53875) Warn users about erroneously adding dependsOn from `test` to `main`
- [`KTIJ-23890`](https://youtrack.jetbrains.com/issue/KTIJ-23890) Gradle to IDEA import: "You are currently using the Kotlin/JS Legacy toolchain" balloon is shown when I actually use IR

### IDE. Gradle. Script

- [`KTIJ-25523`](https://youtrack.jetbrains.com/issue/KTIJ-25523) Scripts: support for standalone configuration flag
- [`KTIJ-25910`](https://youtrack.jetbrains.com/issue/KTIJ-25910) Scripts: transition to GistStorage
- [`KTIJ-26778`](https://youtrack.jetbrains.com/issue/KTIJ-26778) Gradle 8.3: some parts of build.gradle.kts look unresolved
- [`KTIJ-26308`](https://youtrack.jetbrains.com/issue/KTIJ-26308) IAE “Unable to find script compilation configuration for the script KtFile: build.gradle.kts” on reopening project with build.gradle.kts
- [`KT-60171`](https://youtrack.jetbrains.com/issue/KT-60171) K2 IDE: scripting freeze on kotlin project build.gradle.kts file
- [`KT-60236`](https://youtrack.jetbrains.com/issue/KT-60236) K2 scripting: completion fails with exception
- [`KT-59801`](https://youtrack.jetbrains.com/issue/KT-59801) K2 IDE: Adding of an import with a task name to a build script leads to unresolved references
- [`KT-60749`](https://youtrack.jetbrains.com/issue/KT-60749) Scripting: default definition as a fallback
- [`KT-60199`](https://youtrack.jetbrains.com/issue/KT-60199) K2 scripting: exception on script opening
- [`KT-60193`](https://youtrack.jetbrains.com/issue/KT-60193) K2 scripts: configuration discovery fails silently from time to time

### IDE. Hints. Parameter Info

- [`KTIJ-26824`](https://youtrack.jetbrains.com/issue/KTIJ-26824) K2 IDE: "Parameter Info" shows incorrect overload as selected

### IDE. Inspections and Intentions

#### New Features

- [`KTIJ-26302`](https://youtrack.jetbrains.com/issue/KTIJ-26302) K2 IDE: Support adding a `@OptIn` annotation and suggesting to propagate opt-in requirement in quickFixes
- [`KTIJ-25002`](https://youtrack.jetbrains.com/issue/KTIJ-25002) Provide a quick fix to migrate use-site 'get' annotations on getters

#### Fixes

- [`KTIJ-24832`](https://youtrack.jetbrains.com/issue/KTIJ-24832) K2 IDE: 'Redundant qualifier name' false positive for nested classes from supertypes on the outside of a class
- [`KTIJ-26103`](https://youtrack.jetbrains.com/issue/KTIJ-26103) K2 IDE: False positive in redundant qualifier inspection
- [`KTIJ-26024`](https://youtrack.jetbrains.com/issue/KTIJ-26024) K2 IDE: False positive "Redundant qualifier" inspection on a nested class which extends its outer class
- [`KTIJ-26576`](https://youtrack.jetbrains.com/issue/KTIJ-26576) K2 IDE: "Redundant qualifier" false positive with referring parent's subclass in type constraint
- [`KTIJ-26785`](https://youtrack.jetbrains.com/issue/KTIJ-26785) K2 IDE: False positive "Redundant qualifier" inspection in extension function for Java interface with nested interface
- [`KTIJ-26695`](https://youtrack.jetbrains.com/issue/KTIJ-26695) K2 IDE. False negative "Redundant qualifier" directive for invoke function from object
- [`KTIJ-26627`](https://youtrack.jetbrains.com/issue/KTIJ-26627) K2 IDE: False positive "Redundant qualifier" inspection on extension property called on object when other 'this' is present in scope
- [`KTIJ-23407`](https://youtrack.jetbrains.com/issue/KTIJ-23407) K2 IDE. False positive unused import directive for invoke function from object
- [`KTIJ-26808`](https://youtrack.jetbrains.com/issue/KTIJ-26808) K2 IDE. "Redundant qualifier" inspection on the receiver of static method from Java may change semantic when receiver is not direct parent
- [`KTIJ-26840`](https://youtrack.jetbrains.com/issue/KTIJ-26840) K2 IDE. False positive "Redundant qualifier" inspection when accessing companion object member inside anonymous object and there is a name clash
- [`KTIJ-26498`](https://youtrack.jetbrains.com/issue/KTIJ-26498) KMP: Create expect-actual dialog selects incorrect path on Windows
- [`KTIJ-24877`](https://youtrack.jetbrains.com/issue/KTIJ-24877) K2 IDE. False negative unused import directive when declaration is available in file indirectly
- [`KTIJ-25368`](https://youtrack.jetbrains.com/issue/KTIJ-25368) K2 IDE. Specify type explicitly intention does not work with Java records

### IDE. JS

- [`KTIJ-25023`](https://youtrack.jetbrains.com/issue/KTIJ-25023) K/JS: Remove balloon warning about migration to IR backend

### IDE. Libraries

- [`KTIJ-13660`](https://youtrack.jetbrains.com/issue/KTIJ-13660) MPP library: No gutters for `expect` and `actual` symbols

### IDE. Misc

- [`KT-60053`](https://youtrack.jetbrains.com/issue/KT-60053) IdeaKotlinBinaryCoordinates doesn't respect capabilities and classifier attributes

### IDE. Multiplatform

#### Fixes

- [`KTIJ-26700`](https://youtrack.jetbrains.com/issue/KTIJ-26700) KMP: false positive report of non matching expect and actual annotations if annotation is actual typealias
- [`KTIJ-25997`](https://youtrack.jetbrains.com/issue/KTIJ-25997) KotlinMPPGradleTestTasksProvider: Support jvm targets with other names (such as android)
- [`KT-61686`](https://youtrack.jetbrains.com/issue/KT-61686) Check and update places in compiler and IDE where we are saying that MPP is experimental/Beta/Alpha
- [`KTIJ-27058`](https://youtrack.jetbrains.com/issue/KTIJ-27058) Wizard's KMM application failed to build in 232 AS
- [`KT-59760`](https://youtrack.jetbrains.com/issue/KT-59760) [BUG] Use bundled version of Kotlin IDE Plugin in KMM Tests instead of custom
- [`KT-61520`](https://youtrack.jetbrains.com/issue/KT-61520) Sources.jar is not imported for common and intermediate source-sets from the MPP library
- [`KTIJ-25842`](https://youtrack.jetbrains.com/issue/KTIJ-25842) MPP: New create expect/actual dialog uses deprecated location for android instrumented actual counterpart
- [`KTIJ-25746`](https://youtrack.jetbrains.com/issue/KTIJ-25746) MPP: Unable to distinguish android unit and instrumented tests in new create expect/actual dialog if instrumented tests are depends on common
- [`KT-60410`](https://youtrack.jetbrains.com/issue/KT-60410) Add minimum supported KGP version in intellij.git infrastructure
- [`KT-59794`](https://youtrack.jetbrains.com/issue/KT-59794) Bump used KGP in multiplatform intellij.git tests after release 1.9.0
- [`KT-59518`](https://youtrack.jetbrains.com/issue/KT-59518) Cherry-pick old-import tests into 231-1.9.0/master
- [`KT-56736`](https://youtrack.jetbrains.com/issue/KT-56736) Investigate how-to run multiplatform tests on real devices
- [`KT-59519`](https://youtrack.jetbrains.com/issue/KT-59519) Bump AGP versions in intellij.git tests in master
- [`KTIJ-25591`](https://youtrack.jetbrains.com/issue/KTIJ-25591) MPP: Create expect/actual dialog doesn't allow selecting all targets
- [`KT-56684`](https://youtrack.jetbrains.com/issue/KT-56684) Adopt KMM UI tests to be used with IDEA
- [`KT-50952`](https://youtrack.jetbrains.com/issue/KT-50952) MPP: Commonized cinterops doesn't attach/detach to source set on configuration changes

### IDE. Navigation

- [`KT-61894`](https://youtrack.jetbrains.com/issue/KT-61894) Navigation from java sources leads to Kotlin decompiled code in case of suspend function
- [`KTIJ-27053`](https://youtrack.jetbrains.com/issue/KTIJ-27053) Value parameters documentation of expect isn't shown in actuals
- [`KTIJ-26292`](https://youtrack.jetbrains.com/issue/KTIJ-26292) Documentation for expect/actual comes from a random actual
- [`KTIJ-26441`](https://youtrack.jetbrains.com/issue/KTIJ-26441) K2 IDE: navigation doesn't work when type parameters are missed in annotation call
- [`KTIJ-26566`](https://youtrack.jetbrains.com/issue/KTIJ-26566) K2 IDE: don't show no-name parameters in presentations
- [`KTIJ-25366`](https://youtrack.jetbrains.com/issue/KTIJ-25366) K2 IDE. Go to declaration of Java record shows record and constructor

### IDE. Refactorings. Rename

- [`KTIJ-25762`](https://youtrack.jetbrains.com/issue/KTIJ-25762) K2 IDE. label rename doesn't change it's name in usages after rename refactoring

### IDE. Script

- [`KTIJ-25989`](https://youtrack.jetbrains.com/issue/KTIJ-25989) java.lang.NullPointerException: Cannot invoke "com.intellij.openapi.vfs.VirtualFile.getPath()" because the return value of "java.lang.ThreadLocal.get()" is null
- [`KT-60519`](https://youtrack.jetbrains.com/issue/KT-60519) Analysis API: scripts are not invalidated on PCE
- [`KTIJ-26670`](https://youtrack.jetbrains.com/issue/KTIJ-26670) K2 Scripts: We should be able to find a symbol for <CLASS>
- [`KTIJ-25731`](https://youtrack.jetbrains.com/issue/KTIJ-25731) KtAssignResolutionPresenceService is not available as a service in 231-1.9.20
- [`KT-60307`](https://youtrack.jetbrains.com/issue/KT-60307) K2 IDE. KotlinExceptionWithAttachments in script file

### IDE. Wizards

- [`KTIJ-27005`](https://youtrack.jetbrains.com/issue/KTIJ-27005) Wizards 232: Fix generated kotlin version for 1.9.20-Beta
- [`KTIJ-26846`](https://youtrack.jetbrains.com/issue/KTIJ-26846) Adjust compatibility data for 1.9.20 release
- [`KTIJ-26479`](https://youtrack.jetbrains.com/issue/KTIJ-26479) 1.9.20: Update versions in wizards
- [`KT-59347`](https://youtrack.jetbrains.com/issue/KT-59347) Rename Compose Multiplatform wizard to Compose for Desktop

### IR. Interpreter

- [`KT-60467`](https://youtrack.jetbrains.com/issue/KT-60467) "InternalError: Companion object * cannot be interpreted" caused by java's package name
- [`KT-60744`](https://youtrack.jetbrains.com/issue/KT-60744) Restore binary compatibility of toIrConst function

### IR. Tree

- [`KT-59771`](https://youtrack.jetbrains.com/issue/KT-59771) Restore compatibility of IdSignature.CommonSignature
- [`KT-59772`](https://youtrack.jetbrains.com/issue/KT-59772) Restore compatibility of IrFactory#createFunction
- [`KT-59308`](https://youtrack.jetbrains.com/issue/KT-59308) Auto-generate the IrFactory interface

### JS. Tools

- [`KT-44838`](https://youtrack.jetbrains.com/issue/KT-44838) Kotlin/JS source-map-loader slow performance since 1.4.0

### JavaScript

#### New Features

- [`KT-58684`](https://youtrack.jetbrains.com/issue/KT-58684) KJS: ES15 classes — creating instance by class

#### Performance Improvements

- [`KT-58187`](https://youtrack.jetbrains.com/issue/KT-58187) KJS / IR: Huge performance bottleneck while generating sourceMaps (getCannonicalFile)

#### Fixes

- [`KT-60425`](https://youtrack.jetbrains.com/issue/KT-60425) Kotlin/JS compiler incorrect behavior for object singleton with CompleteableDeferred
- [`KT-62790`](https://youtrack.jetbrains.com/issue/KT-62790) java.lang.ClassCastException in compiler when ::class is used
- [`KT-60495`](https://youtrack.jetbrains.com/issue/KT-60495) K2: Make JS CliTestGenerated working with K2
- [`KT-6168`](https://youtrack.jetbrains.com/issue/KT-6168) Ability to generate one JS file for each Kotlin source file
- [`KT-60667`](https://youtrack.jetbrains.com/issue/KT-60667) K2 / KJS: jsTest fails with "SyntaxError: Unexpected token '}'" on runtime
- [`KT-61581`](https://youtrack.jetbrains.com/issue/KT-61581) KJS: generate separate imports for useEsModules()
- [`KT-56737`](https://youtrack.jetbrains.com/issue/KT-56737) K2: build Space JS
- [`KT-59001`](https://youtrack.jetbrains.com/issue/KT-59001) K/JS: Use open-addressing hash map in JS stdlib
- [`KT-60131`](https://youtrack.jetbrains.com/issue/KT-60131) KJS: Interference between `@JsExport` and final implementation of properties
- [`KT-59712`](https://youtrack.jetbrains.com/issue/KT-59712) K/JS: Implement enumEntries intrinsic
- [`KT-60202`](https://youtrack.jetbrains.com/issue/KT-60202) JsExport.Ignored internal extension still has "JavaScript name (<get-const>) generated for this declaration clashes with another declaration"
- [`KT-51333`](https://youtrack.jetbrains.com/issue/KT-51333) KJS: some `KType` equals `Nothing`'s `KType` throws an exception, breaking its symmetry
- [`KT-58857`](https://youtrack.jetbrains.com/issue/KT-58857) KJS/IR: js file is not generated when source is stored in /var folder
- [`KT-53482`](https://youtrack.jetbrains.com/issue/KT-53482) KJS: Inheritance from JS class fails in ES6, because constructor is not called with new
- [`KT-58891`](https://youtrack.jetbrains.com/issue/KT-58891) K/JS: non-local return in lambda may leave an unreachable JS code after return
- [`KT-49077`](https://youtrack.jetbrains.com/issue/KT-49077) KJS / IR: Wrong method called when using overloaded methods and class with the same name
- [`KT-59718`](https://youtrack.jetbrains.com/issue/KT-59718) K/JS: Concatenating a String with a Char can lead to boxing of the Char
- [`KT-59717`](https://youtrack.jetbrains.com/issue/KT-59717) K/JS: a redundant boxing of a returned Char from an inline function
- [`KT-39506`](https://youtrack.jetbrains.com/issue/KT-39506) Kotlin/JS browser application using JS IR and React fails in runtime with "TypeError: _this__0._set_name__2 is not a function"
- [`KT-59151`](https://youtrack.jetbrains.com/issue/KT-59151) K2 / KJS: NullPointerException in Fir2IrClassifierStorage.preCacheBuiltinClasses
- [`KT-59335`](https://youtrack.jetbrains.com/issue/KT-59335) K/JS ES6 classes: A child constructor, when using parent secondary constructor super call, creates a parent object
- [`KT-58797`](https://youtrack.jetbrains.com/issue/KT-58797) Optimize the code generated for objects on JS and Wasm backends
- [`KT-52339`](https://youtrack.jetbrains.com/issue/KT-52339) FIx failing JS tests after bootstrap update
- [`KT-46643`](https://youtrack.jetbrains.com/issue/KT-46643) KJS / IR: Setter of overridden var of external val is removed
- [`KT-55315`](https://youtrack.jetbrains.com/issue/KT-55315) IR: can't access the `stack` property of `Throwable`
- [`KT-59204`](https://youtrack.jetbrains.com/issue/KT-59204) Automatically generate NATIVE directive in tests for IR signatures
- [`KT-59239`](https://youtrack.jetbrains.com/issue/KT-59239) K/JS: Bridge not generated for checking parameter type in generic class override
- [`KT-57347`](https://youtrack.jetbrains.com/issue/KT-57347) KJS: BE IR Incremental cache invalidation doesn't work after inserting Partial Linkage stub
- [`KT-58599`](https://youtrack.jetbrains.com/issue/KT-58599) KJS: Adding an override method to open class does not rebuild children JS code
- [`KT-58003`](https://youtrack.jetbrains.com/issue/KT-58003) K2/MPP/JS: compiler IR serialization crash on multiple calls to inherited expect-function
- [`KT-38017`](https://youtrack.jetbrains.com/issue/KT-38017) KJS: tests generate invalid code depending on file names
- [`KT-25796`](https://youtrack.jetbrains.com/issue/KT-25796) KJS: Top-level constructs are put in an incorrect order
- [`KT-58396`](https://youtrack.jetbrains.com/issue/KT-58396) KJS / IR: "IllegalStateException: Validation failed in file" with Enum.entries and inheritance

### KMM Plugin

- [`KTIJ-27158`](https://youtrack.jetbrains.com/issue/KTIJ-27158) Import is failing after creation of new module if project don't use versionCatalog
- [`KT-59492`](https://youtrack.jetbrains.com/issue/KT-59492) KMM AS plugin for Canary 231 reports error

### Klibs

- [`KT-58877`](https://youtrack.jetbrains.com/issue/KT-58877) [klib tool] add ability to dump klib ir
- [`KT-54402`](https://youtrack.jetbrains.com/issue/KT-54402) Programmatic API to dump public signatures from KLibs
- [`KT-60576`](https://youtrack.jetbrains.com/issue/KT-60576) Keep supported IR signature versions in manifest
- [`KT-59136`](https://youtrack.jetbrains.com/issue/KT-59136) [PL] Lower the default PL engine messages log level down to INFO
- [`KT-59486`](https://youtrack.jetbrains.com/issue/KT-59486) klib: Serialize mangled names along with signatures

### Language Design

- [`KT-22841`](https://youtrack.jetbrains.com/issue/KT-22841) Prohibit different member scopes for non-final expect and its actual
- [`KT-49175`](https://youtrack.jetbrains.com/issue/KT-49175) Inconsistency with extension super-type allowance between suspend / non-suspend function types
- [`KT-61573`](https://youtrack.jetbrains.com/issue/KT-61573) Emit the compilation warning on expect/actual classes. The warning must mention that expect/actual classes are in Beta
- [`KT-57614`](https://youtrack.jetbrains.com/issue/KT-57614) KMP: consider prohibiting `actual typealias` when the corresponding `expect class` has default arguments
- [`KT-27750`](https://youtrack.jetbrains.com/issue/KT-27750) Reverse reservation of 'yield' as keyword

### Libraries

#### New Features

- [`KT-59440`](https://youtrack.jetbrains.com/issue/KT-59440) Rework Flags API in kotlinx-metadata-jvm

#### Fixes

- [`KT-62381`](https://youtrack.jetbrains.com/issue/KT-62381) K/Wasm: (re)publish libraries with 1.9.20-Beta2 (or newer if available)
- [`KT-62656`](https://youtrack.jetbrains.com/issue/KT-62656) Drop `@AllowDifferentMembersInActual` from stdlib
- [`KT-58887`](https://youtrack.jetbrains.com/issue/KT-58887) Reflection: "IllegalArgumentException: argument type mismatch" when using reflection to invoke a value class returning function that suspends
- [`KT-61507`](https://youtrack.jetbrains.com/issue/KT-61507) Native: enum hashcode is not final
- [`KT-56106`](https://youtrack.jetbrains.com/issue/KT-56106) Migrate stdlib to current Kotlin Multiplatform Plugin
- [`KT-58402`](https://youtrack.jetbrains.com/issue/KT-58402) Migrate Vector128 from kotlin.native to kotlinx.cinterop
- [`KT-60911`](https://youtrack.jetbrains.com/issue/KT-60911) Compatibility publishing of kotlin-stdlib-common
- [`KT-53154`](https://youtrack.jetbrains.com/issue/KT-53154) Deprecate enumValues and replace it with enumEntries in standard library
- [`KT-58123`](https://youtrack.jetbrains.com/issue/KT-58123) Update deprecations in native atomic classes for 1.9.20
- [`KT-60444`](https://youtrack.jetbrains.com/issue/KT-60444) transformJvmMainAtomicfu fails with java.lang.NoSuchMethodError: 'kotlin.Metadata kotlinx.metadata.jvm.KotlinClassMetadata.getAnnotationData()'
- [`KT-61342`](https://youtrack.jetbrains.com/issue/KT-61342) kotlin-test-wasm-* artifacts include test code
- [`KT-61315`](https://youtrack.jetbrains.com/issue/KT-61315) Publish common sources in kotlin-test-js sources jar
- [`KT-56608`](https://youtrack.jetbrains.com/issue/KT-56608) WASI Preview1 version of Kotlin/Wasm stdlib
- [`KT-55765`](https://youtrack.jetbrains.com/issue/KT-55765) Review and stabilize stdlib surface available in K/N
- [`KT-55297`](https://youtrack.jetbrains.com/issue/KT-55297) kotlin-stdlib should declare constraints on kotlin-stdlib-jdk8 and kotlin-stdlib-jdk7
- [`KT-57838`](https://youtrack.jetbrains.com/issue/KT-57838) Native: raise ExperimentalNativeApi opt-in requirement level to ERROR
- [`KT-61028`](https://youtrack.jetbrains.com/issue/KT-61028) Behavioural changes to the Native stdlib API
- [`KT-61024`](https://youtrack.jetbrains.com/issue/KT-61024) Native: Mark the kotlin.native.CName annotation with ExperimentalNativeApi
- [`KT-61025`](https://youtrack.jetbrains.com/issue/KT-61025) Native: Deprecate HashSet.getElement() with WARNING
- [`KT-53791`](https://youtrack.jetbrains.com/issue/KT-53791) Publish standard library as a multiplatform artifact with Gradle metadata
- [`KT-57363`](https://youtrack.jetbrains.com/issue/KT-57363) Remove reified constraint from Array constructors in platforms where Array type parameter is not required to be reified
- [`KT-57401`](https://youtrack.jetbrains.com/issue/KT-57401) Native: Regex matching zero length should split surrogate pairs
- [`KT-57359`](https://youtrack.jetbrains.com/issue/KT-57359) Provide Common StringBuilder.append/insert with primitive type arguments
- [`KT-58264`](https://youtrack.jetbrains.com/issue/KT-58264) K2: republish kotlinx.metadata to support LV 2.0
- [`KT-57710`](https://youtrack.jetbrains.com/issue/KT-57710) Native: Internalize `@Retain` and `@RetainForTarget` annotations
- [`KT-57720`](https://youtrack.jetbrains.com/issue/KT-57720) Native: Consider strictening NativeRuntimeApi opt-in requirement level to ERROR
- [`KT-57837`](https://youtrack.jetbrains.com/issue/KT-57837) Deprecate kotlin.native.SharedImmutable and kotlin.native.concurrent.SharedImmutable
- [`KT-58126`](https://youtrack.jetbrains.com/issue/KT-58126) Wasm: Consider removing Primitive.equals(Primitive) overload on primitive types
- [`KT-53327`](https://youtrack.jetbrains.com/issue/KT-53327) Migrate all usages of 'Enum.values' to 'Enum.entries' in standard library
- [`KT-59366`](https://youtrack.jetbrains.com/issue/KT-59366) Deprecate KmModule.annotations
- [`KT-59365`](https://youtrack.jetbrains.com/issue/KT-59365) Get rid of two-stage parsing in KotlinClassMetadata
- [`KT-35116`](https://youtrack.jetbrains.com/issue/KT-35116) Enum.valueOf throws inconsistent exception across multiple platforms
- [`KT-59223`](https://youtrack.jetbrains.com/issue/KT-59223) Native Enum.hashCode should return identity hash code, similar to JVM
- [`KT-56637`](https://youtrack.jetbrains.com/issue/KT-56637) Native: 'String.indexOf' matches byte sequences not on the char boundary, which also makes the result of 'split' and 'replace' operation incorrect
- [`KT-59192`](https://youtrack.jetbrains.com/issue/KT-59192) Align behavior of collection constructors across platforms

### Native

#### New Features

- [`KT-50463`](https://youtrack.jetbrains.com/issue/KT-50463) Native: Provide a way to control the KONAN_DATA_DIR by the Gradle mechanisms
- [`KT-59448`](https://youtrack.jetbrains.com/issue/KT-59448) K2: IR and FIR signatures are not same for composable functions

#### Fixes

- [`KT-60230`](https://youtrack.jetbrains.com/issue/KT-60230) Native: "unknown options: -ios_simulator_version_min -sdk_version" with Xcode 15 beta 3
- [`KT-62532`](https://youtrack.jetbrains.com/issue/KT-62532) Support Xcode 15.0 frameworks as Kotlin/Native platform libraries
- [`KT-61382`](https://youtrack.jetbrains.com/issue/KT-61382) Linking XCFramework fails with error: Invalid record (Producer: 'LLVM11.1.0' Reader: 'LLVM APPLE_1_1300.0.29.30_0')
- [`KT-61417`](https://youtrack.jetbrains.com/issue/KT-61417) Native: string and array variables are not properly displayed in lldb when compiling with caches with Xcode 15
- [`KT-60758`](https://youtrack.jetbrains.com/issue/KT-60758) Native: Building for 'iOS-simulator', but linking in dylib built for 'iOS' in Xcode 15 beta 4
- [`KT-59149`](https://youtrack.jetbrains.com/issue/KT-59149) Native: check compiler compatibility with Xcode 15 beta 1
- [`KT-58537`](https://youtrack.jetbrains.com/issue/KT-58537) iOS project fails to build with rootProject.name = "Contains Space"
- [`KT-59073`](https://youtrack.jetbrains.com/issue/KT-59073) Native: don't include kotlinx.cli endorsed library into compiler distribution
- [`KT-58707`](https://youtrack.jetbrains.com/issue/KT-58707) [K/N] Compiler crash building generics with redundant cast
- [`KT-58654`](https://youtrack.jetbrains.com/issue/KT-58654) Compiler error from kotlin.collections.Map : "Invalid phi record", while compiling for kotlin native

### Native. C Export

- [`KT-56182`](https://youtrack.jetbrains.com/issue/KT-56182) [K2/N] C export doesn't work for non-root packages with K2

### Native. C and ObjC Import

- [`KT-59642`](https://youtrack.jetbrains.com/issue/KT-59642) Remove ability to import forward declaration by library package name
- [`KT-59643`](https://youtrack.jetbrains.com/issue/KT-59643) K2: Disable merging of forward declaration with real declaration class
- [`KT-52882`](https://youtrack.jetbrains.com/issue/KT-52882) MPP / Native: expect/actual mechanism broken when base contract is NSObjectProtocol
- [`KT-55578`](https://youtrack.jetbrains.com/issue/KT-55578) Custom user message for linker error
- [`KT-58585`](https://youtrack.jetbrains.com/issue/KT-58585) [K2/N] Fix interop issues
- [`KT-56041`](https://youtrack.jetbrains.com/issue/KT-56041) [K2/N] Fix broken __builtin_nanf(String)
- [`KT-57716`](https://youtrack.jetbrains.com/issue/KT-57716) [K2/N] Validation failed in file smoke.kt : unexpected type: expected platform.objc.Protocol?, got objcnames.classes.Protocol?
- [`KT-56028`](https://youtrack.jetbrains.com/issue/KT-56028) [K2/N] `cnames.structs.Foo` does not resolve
- [`KT-59645`](https://youtrack.jetbrains.com/issue/KT-59645) Cast to objective C forward declaration crashes compiler
- [`KT-58793`](https://youtrack.jetbrains.com/issue/KT-58793) [K2/N] Package separators after mangling are different for IR and FIR

### Native. ObjC Export

- [`KT-56090`](https://youtrack.jetbrains.com/issue/KT-56090) [K2/N] Emit DocString klib extensions for ObjCExport

### Native. Runtime. Memory

- [`KT-61914`](https://youtrack.jetbrains.com/issue/KT-61914) Kotlin/Native: massive increase in memory usage
- [`KT-61092`](https://youtrack.jetbrains.com/issue/KT-61092) Kotlin/Native: Adjust initial values for expected heap size
- [`KT-61091`](https://youtrack.jetbrains.com/issue/KT-61091) Kotlin/Native: GC scheduler pauses mutators too aggressively
- [`KT-61741`](https://youtrack.jetbrains.com/issue/KT-61741) Kotlin/Native: tsan error in parallel mark
- [`KT-57773`](https://youtrack.jetbrains.com/issue/KT-57773) Kotlin/Native: track memory in big chunks in the GC scheduler
- [`KT-61089`](https://youtrack.jetbrains.com/issue/KT-61089) Kotlin/Native: fix concurrent weak processing for new allocations
- [`KT-55364`](https://youtrack.jetbrains.com/issue/KT-55364) Implement custom allocator for Kotlin/Native
- [`KT-57772`](https://youtrack.jetbrains.com/issue/KT-57772) Kotlin/Native: concurrently process weak references in GC
- [`KT-57771`](https://youtrack.jetbrains.com/issue/KT-57771) Kotlin/Native: parallel mark in GC

### Native. Stdlib

- [`KT-60608`](https://youtrack.jetbrains.com/issue/KT-60608) Introduce AtomicArrays API in K/N stdlib
- [`KT-59120`](https://youtrack.jetbrains.com/issue/KT-59120) Native: Rewrite stdlib AtomicReference with Volatile instead of custom C++ code

### Reflection

- [`KT-47973`](https://youtrack.jetbrains.com/issue/KT-47973) Reflection: "IllegalArgumentException: argument type mismatch" when using callSuspend to call a function returning value class over primitive
- [`KT-41373`](https://youtrack.jetbrains.com/issue/KT-41373) "KotlinReflectionInternalError: Unresolved class" when inspecting anonymous Java class
- [`KT-61304`](https://youtrack.jetbrains.com/issue/KT-61304) Reflection: Calling data class `copy` method via reflection (callBy) fails when the data class has exactly 64 fields
- [`KT-52071`](https://youtrack.jetbrains.com/issue/KT-52071) Continue gracefully when the system property check "kotlin.ignore.old.metadata" fails

### Tools. CLI

- [`KT-60662`](https://youtrack.jetbrains.com/issue/KT-60662) Add JVM target bytecode version 21
- [`KT-58183`](https://youtrack.jetbrains.com/issue/KT-58183) ParseCommandLineArgumentsKt.parseCommandLineArguments takes ~500ms
- [`KT-58690`](https://youtrack.jetbrains.com/issue/KT-58690) OutOfMemory when compiling in CLI
- [`KT-58065`](https://youtrack.jetbrains.com/issue/KT-58065) K2: Enable light tree instead of PSI for CLI compilation of JS and Native by default

### Tools. CLI. Native

- [`KT-59245`](https://youtrack.jetbrains.com/issue/KT-59245) [K1/N] Compile sources to native binary in two stages
- [`KT-56855`](https://youtrack.jetbrains.com/issue/KT-56855) [K2/N] Command-line compiler doesn't support compiling sources directly to a native binary (without intermediate klib) with `-language-version 2.0`
- [`KT-58979`](https://youtrack.jetbrains.com/issue/KT-58979) [K2/N] FIR frontend cannot resolve symbols from resolved klib having non-normalized path

### Tools. Commonizer

- [`KT-59302`](https://youtrack.jetbrains.com/issue/KT-59302) Commonizer: make sure that opt-in annotation generated by cinterop made it into commonized artifact
- [`KT-62028`](https://youtrack.jetbrains.com/issue/KT-62028) False positive "Unnecessary '`@OptIn`' Annotation" for ExperimentalForeignApi
- [`KT-55757`](https://youtrack.jetbrains.com/issue/KT-55757) `kotlinx.cinterop.UnsafeNumber`: empty opt-in message
- [`KT-59859`](https://youtrack.jetbrains.com/issue/KT-59859) Change the OptIn Level to Error for kotlinx.cinterop.UnsafeNumber
- [`KT-59132`](https://youtrack.jetbrains.com/issue/KT-59132) K2/Native/CInterop: [UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'kotlinx/cinterop/CPointed'
- [`KT-58822`](https://youtrack.jetbrains.com/issue/KT-58822) Kotlin Gradle Plugin: migrate tests off native deprecated targets
- [`KT-47641`](https://youtrack.jetbrains.com/issue/KT-47641) Enabled cInterop commonization triggers native compilation during Gradle sync in IDE

### Tools. Compiler Plugin API

- [`KT-58638`](https://youtrack.jetbrains.com/issue/KT-58638) K2: Annotations generated by IR plugins are not included into metadata
- [`KT-61872`](https://youtrack.jetbrains.com/issue/KT-61872) K2: Adding annotations to metadata from backend plugin doesn't work in the presence of comments on annotated declaration
- [`KT-61833`](https://youtrack.jetbrains.com/issue/KT-61833) K2: annotations added via `addMetadataVisibleAnnotationsToElement` to declarations from common sourceset in MPP project are invisible
- [`KT-60051`](https://youtrack.jetbrains.com/issue/KT-60051) K2: Support metadata serialization of primitive const annotation arguments generated by IR plugins

### Tools. Compiler Plugins

- [`KT-61550`](https://youtrack.jetbrains.com/issue/KT-61550) [atomicfu-compiler-plugin]: check that atomic properties are declared as private or internal val
- [`KT-58079`](https://youtrack.jetbrains.com/issue/KT-58079) K2/atomicfu: JVM IR transformer crash on atomic extension functions
- [`KT-61293`](https://youtrack.jetbrains.com/issue/KT-61293) Usage of atomicfu compiler plugin leads to UnsupportedClassVersionError if Gradle runs on JVM <11
- [`KT-55876`](https://youtrack.jetbrains.com/issue/KT-55876) K2. "[Internal Error] java.lang.NoClassDefFoundError: org/jetbrains/kotlin/com/intellij/openapi/util/UserDataHolderBase" when project with languageVersion 2.0 is Built and Run using Intelij IDEA
- [`KT-58049`](https://youtrack.jetbrains.com/issue/KT-58049) K2: Smartcast of nullable property fails when Spring compiler plugin is present
- [`KT-57468`](https://youtrack.jetbrains.com/issue/KT-57468) Kotlin assignment plugin: operation name cannot be found for reference

### Tools. Compiler plugins. Serialization

- [`KT-58501`](https://youtrack.jetbrains.com/issue/KT-58501) K2/MPP/serialization: several classifier kinds seem to miss generated serializer functions when compiled to K/JS and K/Native targets
- [`KT-59768`](https://youtrack.jetbrains.com/issue/KT-59768) kotlinx.serialization + K2 + JS/Native: Support meta-annotations on sealed interfaces with user-defined companions

### Tools. Gradle

#### New Features

- [`KT-59000`](https://youtrack.jetbrains.com/issue/KT-59000) Default standard library dependency should use the single artifact for all targets
- [`KT-57398`](https://youtrack.jetbrains.com/issue/KT-57398) Add ability to run compilation via build-tools-api
- [`KT-34901`](https://youtrack.jetbrains.com/issue/KT-34901) Gradle testFixtures don't have friendPaths set
- [`KT-44833`](https://youtrack.jetbrains.com/issue/KT-44833) Gradle DSL: Add `languageSettings` accessor to `kotlin` extension that applies to all source sets
- [`KT-58315`](https://youtrack.jetbrains.com/issue/KT-58315) Add build metrics for Kotlin/Native task

#### Performance Improvements

- [`KT-62318`](https://youtrack.jetbrains.com/issue/KT-62318) Android Studio sync memory leak in 1.9.20-Beta
- [`KT-62496`](https://youtrack.jetbrains.com/issue/KT-62496) Configuration time regression with KGP 1.9.20-Beta caused by loading of properties
- [`KT-61426`](https://youtrack.jetbrains.com/issue/KT-61426) Enabling compilation via the build tools API may cause high metaspace usage

#### Fixes

- [`KT-61359`](https://youtrack.jetbrains.com/issue/KT-61359) "Unresolved reference: platform" when enabling Gradle configuration cache
- [`KT-59826`](https://youtrack.jetbrains.com/issue/KT-59826) Update SimpleKotlinGradleIT#testProjectIsolation to run on Gradle 8
- [`KT-57565`](https://youtrack.jetbrains.com/issue/KT-57565) Add ability to capture classpath snapshots via the build-tools-api
- [`KT-51964`](https://youtrack.jetbrains.com/issue/KT-51964) Optimize `kotlin.incremental.useClasspathSnapshot` feature to improve incremental Kotlin compilation
- [`KT-61368`](https://youtrack.jetbrains.com/issue/KT-61368) Native compiler option 'module-name' isn't available within the compilerOptions extension for native target while configuring it inside compilations
- [`KT-61355`](https://youtrack.jetbrains.com/issue/KT-61355) freeCompilerArgs arguments and its values are passed to the compiler 5 times if added through target-level compilerOptions{} extension inside compilations
- [`KT-61273`](https://youtrack.jetbrains.com/issue/KT-61273) KGP: TaskOutputsBackup.createSnapshot was failed by IOException sometimes
- [`KT-58987`](https://youtrack.jetbrains.com/issue/KT-58987) Use some available JVM target if there's no JvmTarget for the inferred toolchain version
- [`KT-58234`](https://youtrack.jetbrains.com/issue/KT-58234) Kotlin Gradle Plugin: Deprecate and remove KotlinCompilation.source API
- [`KT-61401`](https://youtrack.jetbrains.com/issue/KT-61401) The reported language version value for KotlinNativeLink tasks in build reports and build scans is inaccurate
- [`KT-54231`](https://youtrack.jetbrains.com/issue/KT-54231) Compatibility with Gradle 8.0 release
- [`KT-61950`](https://youtrack.jetbrains.com/issue/KT-61950) K/Wasm: Add warning about changed sourceSets
- [`KT-61895`](https://youtrack.jetbrains.com/issue/KT-61895) KotlinTopLevelExtension.useCompilerVersion is not marked as experimental
- [`KT-61303`](https://youtrack.jetbrains.com/issue/KT-61303) The module-name value stays unchanged when configuring it through compiler options  extension specific to the android target
- [`KT-61194`](https://youtrack.jetbrains.com/issue/KT-61194) MPP compiler options: part of JsCompilerOptions set up using js { compilerOptions {} } extension is lost
- [`KT-61253`](https://youtrack.jetbrains.com/issue/KT-61253) CompileExecutableKotlinJs task is skipped while configuring LV either using sourceSets.all {} or both js compiler options extension and base multiplatform compiler options extension
- [`KT-59588`](https://youtrack.jetbrains.com/issue/KT-59588) Upgrade max gradle version to max supported in kapt connected tests
- [`KT-61292`](https://youtrack.jetbrains.com/issue/KT-61292) Gradle: compilation tasks may capture wrong build directory when build directory is changed after task configuration
- [`KT-61193`](https://youtrack.jetbrains.com/issue/KT-61193) Flag kotlin.experimental.tryK2 doesn't set LV 2.0 for tasks of kotlin-js gradle plugin
- [`KT-60541`](https://youtrack.jetbrains.com/issue/KT-60541) Possibility to create a custom usable `KotlinCompile` task without using internals
- [`KT-59451`](https://youtrack.jetbrains.com/issue/KT-59451) [K2][1.9.0-Beta] "Errors were stored into ..." log files never actually exist
- [`KT-48898`](https://youtrack.jetbrains.com/issue/KT-48898) Can't suppress warnings by Optin() in KMM build.gradle.kts or IDEA settings
- [`KT-60660`](https://youtrack.jetbrains.com/issue/KT-60660) konan.data.dir property not provided for K/N Gradle project build (on Linux or Mac) with a dependency from a Maven
- [`KT-56959`](https://youtrack.jetbrains.com/issue/KT-56959) K2: Set up Ktor repo performance benchmarks with K2 enabled
- [`KT-56178`](https://youtrack.jetbrains.com/issue/KT-56178) Compatibility with Gradle 8.1 release
- [`KT-61457`](https://youtrack.jetbrains.com/issue/KT-61457) Kotlin Gradle Plugin should not use internal deprecated StartParameterInternal.isConfigurationCache
- [`KT-60718`](https://youtrack.jetbrains.com/issue/KT-60718) Kotlin Gradle Plugin's incremental compilation violates Project Isolation by accessing the tasks in the task graph that were produced by other projects
- [`KT-60717`](https://youtrack.jetbrains.com/issue/KT-60717) Kotlin Gradle Plugin violates Project Isolation restrictions by dynamically looking up properties in the project
- [`KT-54232`](https://youtrack.jetbrains.com/issue/KT-54232) Don't check if file exists in task file inputs configuration
- [`KT-61066`](https://youtrack.jetbrains.com/issue/KT-61066) [KMP] iOS "Unkown Kotlin JVM target 20"
- [`KT-54160`](https://youtrack.jetbrains.com/issue/KT-54160) New KGP API using lazy properties to add compiler plugin options may remove options with the same pluginId
- [`KT-60839`](https://youtrack.jetbrains.com/issue/KT-60839) KGP provides incorrect default value "ENABLED" for -Xpartial-linkage
- [`KT-15370`](https://youtrack.jetbrains.com/issue/KT-15370) Gradle DSL: add module-level kotlin options
- [`KT-57645`](https://youtrack.jetbrains.com/issue/KT-57645) build_scan failed in testBuildScanReportSmokeTestForConfigurationCache test with Gradle 8.0.2
- [`KT-59827`](https://youtrack.jetbrains.com/issue/KT-59827) Update configuration to validate plugin inputs
- [`KT-59799`](https://youtrack.jetbrains.com/issue/KT-59799) Validate Gralde Integrations tests has only one tag
- [`KT-59117`](https://youtrack.jetbrains.com/issue/KT-59117) Add gradle integration tests for explicit api mode in Android projects
- [`KT-59587`](https://youtrack.jetbrains.com/issue/KT-59587) Upgrade max gradle version to max supported in jvmToolchain connected tests
- [`KT-56636`](https://youtrack.jetbrains.com/issue/KT-56636) Bump max Gradle version for integration tests to 8.0
- [`KT-58353`](https://youtrack.jetbrains.com/issue/KT-58353) Support reporting of diagnostics after projects are evaluated
- [`KT-53822`](https://youtrack.jetbrains.com/issue/KT-53822) Upgrade the `gradle-download-task` dependency of the Kotlin Gradle plugin
- [`KT-58162`](https://youtrack.jetbrains.com/issue/KT-58162) Kotlin Gradle Plugin: Remove kotlinx.coroutines from classpath of KGP
- [`KT-58104`](https://youtrack.jetbrains.com/issue/KT-58104) Check values for MPP_PLATFORMS
- [`KT-58569`](https://youtrack.jetbrains.com/issue/KT-58569) Bump language version for Gradle plugins dependencies to 1.5

### Tools. Gradle. Cocoapods

- [`KT-59263`](https://youtrack.jetbrains.com/issue/KT-59263) Add diagnostic that a dummy framework is not present when build is triggered from Xcode
- [`KT-57741`](https://youtrack.jetbrains.com/issue/KT-57741) KMP importing an iOS project with Xcode 14.3 fails when importing a pod that depends on `libarclite_iphoneos`
- [`KT-60050`](https://youtrack.jetbrains.com/issue/KT-60050) Log reason why podInstall task is skipped
- [`KT-49430`](https://youtrack.jetbrains.com/issue/KT-49430) Stop invalidating iOS framework generated by KMM module on each Gradle Sync
- [`KT-59522`](https://youtrack.jetbrains.com/issue/KT-59522) Set the required environment for cocoapods invocations
- [`KT-59313`](https://youtrack.jetbrains.com/issue/KT-59313) Elevate to error deprecation of useLibraries
- [`KT-58775`](https://youtrack.jetbrains.com/issue/KT-58775) If the pod has a declared dependency on itself, then it will cause StackOverFlow exception while importing of a project

### Tools. Gradle. JS

#### New Features

- [`KT-49789`](https://youtrack.jetbrains.com/issue/KT-49789) KJS / Gradle: Add npm style repository option for YarnRootExtension - and/or don't register github repository when download=false

#### Fixes

- [`KT-60469`](https://youtrack.jetbrains.com/issue/KT-60469) KJS: "Could not serialize value of type Build_gradle" caused by changed name in packageJson task
- [`KT-61623`](https://youtrack.jetbrains.com/issue/KT-61623) K/Wasm: Error with project dependency between modules with both wasmJs and wasmWasi targets
- [`KT-61326`](https://youtrack.jetbrains.com/issue/KT-61326) K/JS: rootPackageJson fails with NLP when testPackageJson skipped and main packageJson up-to-date
- [`KT-60218`](https://youtrack.jetbrains.com/issue/KT-60218) K/JS reports reports deprecation for non-Action dsl params in regular kotlin dsl
- [`KT-56933`](https://youtrack.jetbrains.com/issue/KT-56933) Add Kotlin/JS incremental tests with K2 enabled
- [`KT-58970`](https://youtrack.jetbrains.com/issue/KT-58970) browserTest gradle task fails if karma is used and gradle configuration cache is enabled
- [`KT-42520`](https://youtrack.jetbrains.com/issue/KT-42520) Add a way to setup generating separate js files for each module inside gradle
- [`KT-32086`](https://youtrack.jetbrains.com/issue/KT-32086) Gradle, JS: runTask.enabled = false has no effect on npm dependencies
- [`KT-48358`](https://youtrack.jetbrains.com/issue/KT-48358) KJS: Circular dependency when multiple second-level Gradle modules have the same name
- [`KT-50530`](https://youtrack.jetbrains.com/issue/KT-50530) Kotlin/JS: enabling `kotlin.js.ir.output.granularity=whole-program` does not remove superfluous .js output files
- [`KT-50442`](https://youtrack.jetbrains.com/issue/KT-50442) KJS / Gradle: webpack plugin errors not logged
- [`KT-46003`](https://youtrack.jetbrains.com/issue/KT-46003) KJS / IR: Should provide single distributions folder for production and development similarly to Legacy
- [`KT-47319`](https://youtrack.jetbrains.com/issue/KT-47319) KJS: Error when project contains two modules with same name
- [`KT-46010`](https://youtrack.jetbrains.com/issue/KT-46010) KJS / Gradle: Can't find a file on building on Windows
- [`KT-48923`](https://youtrack.jetbrains.com/issue/KT-48923) KJS / Gradle: No `Webpack` error messages when Node.js process exits unexpected
- [`KT-51942`](https://youtrack.jetbrains.com/issue/KT-51942) KJS / Gradle: fails with two projects with the same name, but different paths
- [`KT-51372`](https://youtrack.jetbrains.com/issue/KT-51372) Kotlin/JS: Gradle compileKotlinJs processes directory just excluded from source set
- [`KT-52134`](https://youtrack.jetbrains.com/issue/KT-52134) KJS: the default generated JS module name in a Gradle project with multiple subprojects is incomplete, which might cause duplicate names and build conflicts
- [`KT-52776`](https://youtrack.jetbrains.com/issue/KT-52776) KJS / Gradle: Webpack version update despite yarn.lock breaks Kotlin/JS build
- [`KT-39173`](https://youtrack.jetbrains.com/issue/KT-39173) Kotlin/JS: kotlinNpmInstall fails with Gradle plugin 1.4-M2 and transitive dependency to 1.4-M1
- [`KT-55216`](https://youtrack.jetbrains.com/issue/KT-55216) KJS / IR: Transitive NPM dependencies between projects are not included in jsPublicPackageJsonTask
- [`KT-54182`](https://youtrack.jetbrains.com/issue/KT-54182) MPP / JS: `StackOverflowError` when in a Gradle multi-project and Kotlin Multiplatform build with the JS IR target which depends on another with the same subproject name via a renamed published Maven artifact
- [`KT-58250`](https://youtrack.jetbrains.com/issue/KT-58250) The `NodeJsExec` tasks are not compatible with Gradle configuration cache
- [`KT-58256`](https://youtrack.jetbrains.com/issue/KT-58256) The `D8Exec` tasks are not compatible with Gradle configuration cache

### Tools. Gradle. Multiplatform

#### New Features

- [`KT-60441`](https://youtrack.jetbrains.com/issue/KT-60441) KGP based dependency resolution: Support 'idea.gradle.download.sources' flag

#### Fixes

- [`KT-59316`](https://youtrack.jetbrains.com/issue/KT-59316) Deprecate multiple ‘same’ targets
- [`KT-59042`](https://youtrack.jetbrains.com/issue/KT-59042) "Cannot build 'KotlinProjectStructureMetadata' during project configuration phase" when configuration cache enabled
- [`KT-58029`](https://youtrack.jetbrains.com/issue/KT-58029) Emit warning when using experimental artifacts DSL
- [`KT-60763`](https://youtrack.jetbrains.com/issue/KT-60763) Evaluate user feedback after switch to `java-base` plugin for KotlinJvmTarget.withJava
- [`KT-62029`](https://youtrack.jetbrains.com/issue/KT-62029) Kotlin 1.9.20-Beta fails to detect some transitive dependency references in JVM+Android source set
- [`KT-61652`](https://youtrack.jetbrains.com/issue/KT-61652) MPP ConcurrentModificationException on transformCommonMainDependenciesMetadata
- [`KT-61622`](https://youtrack.jetbrains.com/issue/KT-61622) Upgrading to Kotlin 1.9 prevents commonMain sourceset classes from being processed by kapt/ksp (dagger/Hilt)
- [`KT-59321`](https://youtrack.jetbrains.com/issue/KT-59321) Deprecate targets.presets
- [`KT-58759`](https://youtrack.jetbrains.com/issue/KT-58759) Deprecate `platform`, `enforcedPlatform` and other related to Gradle DependencyHandler methods in `KotlinDependencyHandler`
- [`KT-60579`](https://youtrack.jetbrains.com/issue/KT-60579) Multiplatform;Composite Builds: prepareKotlinIdeaImport called by wrong path for nested build, causing sync fail
- [`KT-61540`](https://youtrack.jetbrains.com/issue/KT-61540) K2: KMP/K2: Metadata compilations: Discriminate expect over actual by sorting compile path using refines edges
- [`KTIJ-26340`](https://youtrack.jetbrains.com/issue/KTIJ-26340) Bump Kotlin Gradle Plugin version to '1.9.20-dev-6845'
- [`KT-59020`](https://youtrack.jetbrains.com/issue/KT-59020) 1.9.0 Beta Kotlin plugin Gradle sync fails with intermediate JVM + Android source set
- [`KT-60198`](https://youtrack.jetbrains.com/issue/KT-60198) Stop publishing the org.jetbrains.kotlin.multiplatform.pm20
- [`KT-60596`](https://youtrack.jetbrains.com/issue/KT-60596) Return `targetHierarchy` with deprecation to kotlin dsl for smooth migration
- [`KT-59595`](https://youtrack.jetbrains.com/issue/KT-59595) KotlinJvmTarget.withJava: Switch implementation from `java` to `java-base` plugin
- [`KT-58800`](https://youtrack.jetbrains.com/issue/KT-58800) Add some cocoapods will cause Xcode preview into build loop since Xcode 14.3
- [`KT-58489`](https://youtrack.jetbrains.com/issue/KT-58489) MPP: Add an error if SourceLayoutV1 is used
- [`KT-59774`](https://youtrack.jetbrains.com/issue/KT-59774) MPP: Print stacktraces of diagnostics only when `--stacktrace` (or higher) is used
- [`KT-60158`](https://youtrack.jetbrains.com/issue/KT-60158) KotlinJvmTarget.withJava: Ensure java source sets are created eagerly
- [`KT-58316`](https://youtrack.jetbrains.com/issue/KT-58316) Gradle 8: ':podDebugFrameworkIosFat' and [configuration ':debugFrameworkIosFat'] contain identical attribute sets
- [`KT-59615`](https://youtrack.jetbrains.com/issue/KT-59615) renderReportedDiagnostics: Rename 'isVerbose' to 'renderForTests'
- [`KT-60943`](https://youtrack.jetbrains.com/issue/KT-60943) K2/KMP: compileCommonMainKotlinMetadata fails with resolution ambiguity between candidates from stdlib
- [`KT-60491`](https://youtrack.jetbrains.com/issue/KT-60491) KMP: Add documentation link to the warning message `w: The Default Kotlin Hierarchy was not applied to `
- [`KT-57521`](https://youtrack.jetbrains.com/issue/KT-57521) MPP: Compiler options declared in places other than languageSettings don't reach shared source sets
- [`KT-58676`](https://youtrack.jetbrains.com/issue/KT-58676) Enable default Kotlin Target Hierarchy by default
- [`KT-61056`](https://youtrack.jetbrains.com/issue/KT-61056) Native-shared source sets don't receive dependency on a commonMain of 1.9.20 stdlib
- [`KT-58712`](https://youtrack.jetbrains.com/issue/KT-58712) Enable commonization by default if the CocoaPods plugin provided the dependencies that should be commonized
- [`KT-59317`](https://youtrack.jetbrains.com/issue/KT-59317) Deprecate ios()  preset in favor of target hierarchy
- [`KT-47144`](https://youtrack.jetbrains.com/issue/KT-47144) [Multiplatform] Warn about setting *Test.dependsOn(*Main)
- [`KT-59733`](https://youtrack.jetbrains.com/issue/KT-59733) Make KotlinDefaultHierarchyFallbackIllegalTargetNames to check target names disregarding the case
- [`KT-55787`](https://youtrack.jetbrains.com/issue/KT-55787) Deprecate dependsOn edges ending at declared source sets of platform compilations
- [`KT-58872`](https://youtrack.jetbrains.com/issue/KT-58872) MPP: kotlin-test library reported as published in the legacy mode
- [`KT-59863`](https://youtrack.jetbrains.com/issue/KT-59863) pluginManagement.includeBuild doesn't work when kotlin("multiplatform") applied to one of subprojects of included build
- [`KT-58729`](https://youtrack.jetbrains.com/issue/KT-58729) Investigate failures in KMM UI tests
- [`KT-59661`](https://youtrack.jetbrains.com/issue/KT-59661) Bump kotlin.git AGP version in tests to 8
- [`KT-58753`](https://youtrack.jetbrains.com/issue/KT-58753) Add Hedgehog AS in KMM UI tests
- [`KT-58731`](https://youtrack.jetbrains.com/issue/KT-58731) Fix failues in Mac tests in kt-master, kt-231-1.9.0 and kt-223-1.9.0
- [`KT-59248`](https://youtrack.jetbrains.com/issue/KT-59248) Fix failures in Mac tests and Android Tests in kt-223-master and kt-231-master
- [`KT-58732`](https://youtrack.jetbrains.com/issue/KT-58732) Run kt-master Mac tests on cloud Mac
- [`KT-57686`](https://youtrack.jetbrains.com/issue/KT-57686) Fix KMM UI tests again
- [`KT-60134`](https://youtrack.jetbrains.com/issue/KT-60134) MPP: Include user attributes to host-specific metadata dependency configurations
- [`KT-60233`](https://youtrack.jetbrains.com/issue/KT-60233) Investigate publication of TargetJvmEnivornment on Kotlin configuration
- [`KT-59311`](https://youtrack.jetbrains.com/issue/KT-59311) Elevate to Error `commonMain.dependsOn(anything)`
- [`KT-59320`](https://youtrack.jetbrains.com/issue/KT-59320) Elevate to Error usage of jvmWithJava
- [`KT-45877`](https://youtrack.jetbrains.com/issue/KT-45877) MPP / Gradle: "GradleException: Please initialize at least one Kotlin target" isn't user-friendly
- [`KT-59844`](https://youtrack.jetbrains.com/issue/KT-59844) KGP-based import: Reduce 'red wall of errors' on dependency resolution failures
- [`KT-60462`](https://youtrack.jetbrains.com/issue/KT-60462) jvm().withJava(): Zombie instance returned for compilations created after withJava call
- [`KT-58471`](https://youtrack.jetbrains.com/issue/KT-58471) Kotlin Multiplatform plugin resolves configurations during configuration
- [`KT-59578`](https://youtrack.jetbrains.com/issue/KT-59578) External Android Target: Implement integration tests
- [`KT-58737`](https://youtrack.jetbrains.com/issue/KT-58737) Develop tests for old import in new intellij.git test infra
- [`KT-59268`](https://youtrack.jetbrains.com/issue/KT-59268) Run multiplatform tests in intellij.git with XCode 15.0 manually
- [`KT-58220`](https://youtrack.jetbrains.com/issue/KT-58220) Kotlin Gradle Plugin: Kotlin 1.9 release grooming
- [`KT-58305`](https://youtrack.jetbrains.com/issue/KT-58305) Investigate KotlinAndroidMppIT.testCustomAttributesInAndroidTargets: being broken for AGP 7.0.4
- [`KT-58255`](https://youtrack.jetbrains.com/issue/KT-58255) Kotlin Gradle Plugin Lifecycle: Remove LifecycleAwareProperty
- [`KT-55312`](https://youtrack.jetbrains.com/issue/KT-55312) Replace "ALL_COMPILE_DEPENDENCIES_METADATA" configuration with set of metadata dependencies configurations associated per set
- [`KT-54825`](https://youtrack.jetbrains.com/issue/KT-54825) TCS: Gradle Sync: Dependency resolution in KGP

### Tools. Gradle. Native

- [`KT-54362`](https://youtrack.jetbrains.com/issue/KT-54362) Support Gradle Configuration caching in Xcode integration tasks and in CocoaPods plugin
- [`KT-61700`](https://youtrack.jetbrains.com/issue/KT-61700) Native: linkDebugExecutableNative has duplicated freeCompilerArgs
- [`KT-58519`](https://youtrack.jetbrains.com/issue/KT-58519) Migrate Xcode/CocoaPods warnings to the new diagnostics infra
- [`KT-61154`](https://youtrack.jetbrains.com/issue/KT-61154) NativeCompilerDownloader adds .konan/kotlin-native-prebuilt-linux-x86_64-1.9.0/konan/lib/kotlin-native-compiler-embeddable.jar as configuration cache input
- [`KT-59252`](https://youtrack.jetbrains.com/issue/KT-59252) Support configuration cache in Xcode/CocoaPods tasks with Gradle 8.1

### Tools. Incremental Compile

- [`KT-61852`](https://youtrack.jetbrains.com/issue/KT-61852) Kotlin 1.9.20-Beta: incremental compilation fails with files outside of the project folder
- [`KT-19745`](https://youtrack.jetbrains.com/issue/KT-19745) After konverting java to kotlin, kapt3 throws duplicate class exception
- [`KT-58547`](https://youtrack.jetbrains.com/issue/KT-58547) "has several compatible actual declarations in <module>, <same module>"

### Tools. JPS

- [`KT-58026`](https://youtrack.jetbrains.com/issue/KT-58026) Add basic JPS build performance metrics
- [`KT-57039`](https://youtrack.jetbrains.com/issue/KT-57039) K2 IDE. Cannot compile gradle project with LV=2.0 via JPS with NoClassDefFoundError: org/jetbrains/kotlin/com/intellij/openapi/util/UserDataHolderBase
- [`KT-58314`](https://youtrack.jetbrains.com/issue/KT-58314) kotlin-build-statistics is missing for kotlin-jps-plugin

### Tools. Kapt

#### Fixes

- [`KT-62438`](https://youtrack.jetbrains.com/issue/KT-62438) Change experimental K2 kapt diagnostic message
- [`KT-61879`](https://youtrack.jetbrains.com/issue/KT-61879) K2 Kapt: java.lang.NoSuchMethodError during stub generation
- [`KT-58326`](https://youtrack.jetbrains.com/issue/KT-58326) KAPT / Gradle: argument changes are ignored
- [`KT-61114`](https://youtrack.jetbrains.com/issue/KT-61114) K2 Kapt: add a Gradle property `kapt.use.k2` to enable K2 Kapt
- [`KT-57594`](https://youtrack.jetbrains.com/issue/KT-57594) K2: Investigate the Kapt features used in the quality gates projects
- [`KT-59754`](https://youtrack.jetbrains.com/issue/KT-59754) K2: KAPT4 generates non-compilable code for nested data classes, annotated by Moshi's `@JsonClass`
- [`KT-60270`](https://youtrack.jetbrains.com/issue/KT-60270) K2: KAPT4 tries to generate metadata for local variables
- [`KT-60293`](https://youtrack.jetbrains.com/issue/KT-60293) K2: KAPT4 fails to generate metadata for const vals
- [`KT-59704`](https://youtrack.jetbrains.com/issue/KT-59704) KAPT4 does not support Dagger's `@Inject` lateinit properties
- [`KT-59745`](https://youtrack.jetbrains.com/issue/KT-59745) K2: KAPT4: ISE when passing moshi's `@JsonClass` to Class<T>
- [`KT-59756`](https://youtrack.jetbrains.com/issue/KT-59756) K2: KAPT4 with Moshi generates `@Transient` fields
- [`KT-59757`](https://youtrack.jetbrains.com/issue/KT-59757) K2: KAPT4: Generic interfaces with default methods lead to static usage of type parameters
- [`KT-59703`](https://youtrack.jetbrains.com/issue/KT-59703) KAPT4 generates old metadata version

### Tools. Maven

- [`KT-26156`](https://youtrack.jetbrains.com/issue/KT-26156) Maven Kotlin Plugin should not WARN when no sources found

### Tools. Parcelize

- [`KT-57795`](https://youtrack.jetbrains.com/issue/KT-57795) Add for-ide support for Parcelize K2 jars

### Tools. Scripts

- [`KT-57877`](https://youtrack.jetbrains.com/issue/KT-57877) K2 / Script: "IllegalArgumentException: source must not be null" during compilation
- [`KT-58817`](https://youtrack.jetbrains.com/issue/KT-58817) K2: support for .kts highlighting and completion

### Tools. Wasm

- [`KT-62128`](https://youtrack.jetbrains.com/issue/KT-62128) Wasm tests (still) do not work on Kotlin 1.9.20-Beta2
- [`KT-61973`](https://youtrack.jetbrains.com/issue/KT-61973) K/Wasm: wasmWasiNodeRun is missed
- [`KT-61971`](https://youtrack.jetbrains.com/issue/KT-61971) K/Wasm: wasmWasiTest should depends on kotlinNodeJsSetup
- [`KT-60654`](https://youtrack.jetbrains.com/issue/KT-60654) Wasm: split wasm target into wasm-js and wasm-wasi
- [`KT-57058`](https://youtrack.jetbrains.com/issue/KT-57058) Do not require a return value for DOM event listeners with Kotlin/Wasm
- [`KT-59062`](https://youtrack.jetbrains.com/issue/KT-59062) WASM: Report errors when calling WebAssembly.instantiateStreaming in tests
- [`KTIJ-25207`](https://youtrack.jetbrains.com/issue/KTIJ-25207) Collect stats about Kotlin/Wasm usage


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