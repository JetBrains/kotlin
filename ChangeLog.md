# CHANGELOG

## 1.3-RC

### Compiler

#### New Features

- [`KT-17679`](https://youtrack.jetbrains.com/issue/KT-17679) Support suspend fun main in JVM
- [`KT-24854`](https://youtrack.jetbrains.com/issue/KT-24854) Support suspend function types for arities bigger than 22
- [`KT-26574`](https://youtrack.jetbrains.com/issue/KT-26574) Support main entry-point without arguments in frontend, IDE and JVM

#### Performance Improvements

- [`KT-26490`](https://youtrack.jetbrains.com/issue/KT-26490) Change boxing technique: instead of calling `valueOf`, allocate new wrapper type

#### Fixes

- [`KT-22069`](https://youtrack.jetbrains.com/issue/KT-22069) Array class literals are always loaded as `Array<*>` from deserialized annotations
- [`KT-22892`](https://youtrack.jetbrains.com/issue/KT-22892) Call of `invoke` function with lambda parameter on a field named `suspend` should be reported
- [`KT-24708`](https://youtrack.jetbrains.com/issue/KT-24708) Incorrect WhenMappings code generated in case of mixed enum classes in when conditions
- [`KT-24853`](https://youtrack.jetbrains.com/issue/KT-24853) Forbid KSuspendFunctionN and SuspendFunctionN to be used as supertypes
- [`KT-24866`](https://youtrack.jetbrains.com/issue/KT-24866) Review support of all operators for suspend function and forbid all unsupported
- [`KT-25461`](https://youtrack.jetbrains.com/issue/KT-25461) Mangle names of functions that have top-level inline class types in their signatures to allow non-trivial non-public constructors
- [`KT-25855`](https://youtrack.jetbrains.com/issue/KT-25855) Load Java declarations which reference kotlin.jvm.functions.FunctionN as Deprecated with level ERROR
- [`KT-26071`](https://youtrack.jetbrains.com/issue/KT-26071) Postpone conversions from signed constant literals to unsigned ones
- [`KT-26141`](https://youtrack.jetbrains.com/issue/KT-26141) actual typealias for expect sealed class results in error "This type is sealed, so it can be inherited by only its own nested classes or objects"
- [`KT-26200`](https://youtrack.jetbrains.com/issue/KT-26200) Forbid suspend functions annotated with @kotlin.test.Test
- [`KT-26219`](https://youtrack.jetbrains.com/issue/KT-26219) Result of unsigned predecrement/preincrement is not boxed as expected
- [`KT-26223`](https://youtrack.jetbrains.com/issue/KT-26223) Inline lambda arguments of inline class types are passed incorrectly
- [`KT-26291`](https://youtrack.jetbrains.com/issue/KT-26291) Boxed/primitive types clash when overriding Kotlin from Java with common generic supertype with inline class type argument
- [`KT-26403`](https://youtrack.jetbrains.com/issue/KT-26403) Add `-impl` suffix to `box`/`unbox` methods and make them synthetic
- [`KT-26404`](https://youtrack.jetbrains.com/issue/KT-26404) Mangling: setters for properties of inline class types
- [`KT-26409`](https://youtrack.jetbrains.com/issue/KT-26409) implies in CallsInPlace effect isn't supported
- [`KT-26437`](https://youtrack.jetbrains.com/issue/KT-26437) Generate constructors containing inline classes as parameter types as private with synthetic accessors
- [`KT-26449`](https://youtrack.jetbrains.com/issue/KT-26449) Prohibit equals-like and hashCode-like declarations inside inline classes
- [`KT-26451`](https://youtrack.jetbrains.com/issue/KT-26451) Generate static methods with equals/hashCode implementations
- [`KT-26452`](https://youtrack.jetbrains.com/issue/KT-26452) Get rid of $Erased nested class in ABI of inline classes
- [`KT-26453`](https://youtrack.jetbrains.com/issue/KT-26453) Generate all static methods in inline classes with “-impl” suffix
- [`KT-26454`](https://youtrack.jetbrains.com/issue/KT-26454) Prohibit @JvmName on functions that are assumed to be mangled
- [`KT-26468`](https://youtrack.jetbrains.com/issue/KT-26468) Inline class ABI: Constructor invocation is not represented in bytecode
- [`KT-26480`](https://youtrack.jetbrains.com/issue/KT-26480) Report error from compiler when suspension point is located between corresponding MONITORENTER/MONITOREXIT
- [`KT-26538`](https://youtrack.jetbrains.com/issue/KT-26538) Prepare kotlin.Result to publication in 1.3
- [`KT-26558`](https://youtrack.jetbrains.com/issue/KT-26558) Inline Classes: IllegalStateException when invoking secondary constructor for a primitive underlying type
- [`KT-26570`](https://youtrack.jetbrains.com/issue/KT-26570) Inline classes ABI
- [`KT-26573`](https://youtrack.jetbrains.com/issue/KT-26573) Reserve box, unbox, equals and hashCode methods inside inline class for future releases
- [`KT-26575`](https://youtrack.jetbrains.com/issue/KT-26575) Reserve bodies of secondary constructors for inline classes
- [`KT-26576`](https://youtrack.jetbrains.com/issue/KT-26576) Generate stubs for box/unbox/equals/hashCode inside inline classes
- [`KT-26580`](https://youtrack.jetbrains.com/issue/KT-26580) Add version to kotlin.coroutines.jvm.internal.DebugMetadata
- [`KT-26659`](https://youtrack.jetbrains.com/issue/KT-26659) Prohibit using kotlin.Result as a return type and with special operators
- [`KT-26687`](https://youtrack.jetbrains.com/issue/KT-26687) Stdlib contracts have no effect in common code
- [`KT-26707`](https://youtrack.jetbrains.com/issue/KT-26707) companion val of primitive type is not treated as compile time constant
- [`KT-26720`](https://youtrack.jetbrains.com/issue/KT-26720) Write language version requirement on inline classes and on declarations that use inline classes
- [`KT-26859`](https://youtrack.jetbrains.com/issue/KT-26859) Inline class misses unboxing when using indexer into an ArrayList
- [`KT-26936`](https://youtrack.jetbrains.com/issue/KT-26936) Report warning instead of error on usages of Experimental/UseExperimental
- [`KT-26958`](https://youtrack.jetbrains.com/issue/KT-26958) Introduce builder-inference with an explicit opt-in for it

### IDE

#### New Features

- [`KT-26525`](https://youtrack.jetbrains.com/issue/KT-26525) "Move Element Right/Left": Support type parameters in `where` clause (multiple type constraints)

#### Fixes

- [`KT-22491`](https://youtrack.jetbrains.com/issue/KT-22491) MPP new project/new module templates are not convenient
- [`KT-26428`](https://youtrack.jetbrains.com/issue/KT-26428) Kotlin Migration in AS32 / AS33 fails to complete after "Indexing paused due to batch update" event
- [`KT-26484`](https://youtrack.jetbrains.com/issue/KT-26484) Do not show `-Xmulti-platform` option in facets for common modules of multiplatform projects with the new model
- [`KT-26584`](https://youtrack.jetbrains.com/issue/KT-26584) @Language prefix and suffix are ignored for function arguments
- [`KT-26679`](https://youtrack.jetbrains.com/issue/KT-26679) Coroutine migrator should rename buildSequence/buildIterator to their new names
- [`KT-26732`](https://youtrack.jetbrains.com/issue/KT-26732) Kotlin language version from IDEA settings is not taken into account when working with Java code
- [`KT-26770`](https://youtrack.jetbrains.com/issue/KT-26770) Android module in a multiplatform project isn't recognised as a multiplatform module
- [`KT-26794`](https://youtrack.jetbrains.com/issue/KT-26794) Bad version detection during migration in Android Studio 3.2
- [`KT-26823`](https://youtrack.jetbrains.com/issue/KT-26823) Fix deadlock in databinding with AndroidX which led to Android Studio hanging
- [`KT-26827`](https://youtrack.jetbrains.com/issue/KT-26827) ISE “Error type encountered: [ERROR : UInt] (UnresolvedType)” for data inline class wrapped unsigned type
- [`KT-26829`](https://youtrack.jetbrains.com/issue/KT-26829) ISE “Error type encountered: [ERROR : UInt] (UnresolvedType)” for using as a field inline class wrapped unsigned type
- [`KT-26843`](https://youtrack.jetbrains.com/issue/KT-26843) `LazyLightClassMemberMatchingError$NoMatch: Couldn't match ClsMethodImpl:getX MemberIndex(index=1) (with 0 parameters)` on inline class overriding inherited interface method defined in different files
- [`KT-26895`](https://youtrack.jetbrains.com/issue/KT-26895) Exception while building light class for @Serializable annotated class

### IDE. Android

- [`KT-26169`](https://youtrack.jetbrains.com/issue/KT-26169) Android extensions are not recognised by IDE in multiplatform projects
- [`KT-26813`](https://youtrack.jetbrains.com/issue/KT-26813) Multiplatform projects without Android target are not imported properly into Android Studio

### IDE. Code Style, Formatting

- [`KT-22322`](https://youtrack.jetbrains.com/issue/KT-22322) Incorrect indent after pressing Enter after annotation entry
- [`KT-26377`](https://youtrack.jetbrains.com/issue/KT-26377) Formatter does not add blank line between annotation and type alias (or secondary constructor)

### IDE. Decompiler

- [`KT-25853`](https://youtrack.jetbrains.com/issue/KT-25853) IDEA hangs when Kotlin bytecode tool window open while editing a class with secondary constructor

### IDE. Gradle

- [`KT-26634`](https://youtrack.jetbrains.com/issue/KT-26634) Do not generate module for metadataMain compilation on new MPP import
- [`KT-26675`](https://youtrack.jetbrains.com/issue/KT-26675) Gradle: Dependency on multiple files gets duplicated on import

### IDE. Inspections and Intentions

#### New Features

- [`KT-17687`](https://youtrack.jetbrains.com/issue/KT-17687) Quickfix for "Interface doesn't have constructors" to convert to anonymous object
- [`KT-24728`](https://youtrack.jetbrains.com/issue/KT-24728) Add quickfix to remove single explicit & unused lambda parameter
- [`KT-25533`](https://youtrack.jetbrains.com/issue/KT-25533) An intention to create `actual` implementations for `expect` members annotated with @OptionalExpectation
- [`KT-25621`](https://youtrack.jetbrains.com/issue/KT-25621) Inspections for functions returning SuccessOrFailure
- [`KT-25969`](https://youtrack.jetbrains.com/issue/KT-25969) Add an inspection for 'flatMap { it }'
- [`KT-26230`](https://youtrack.jetbrains.com/issue/KT-26230) Inspection: replace safe cast (as?) with `if` (instance check + early return)

#### Fixes

- [`KT-13343`](https://youtrack.jetbrains.com/issue/KT-13343) Remove explicit type specification breaks code if initializer omits generics
- [`KT-19586`](https://youtrack.jetbrains.com/issue/KT-19586) Create actual implementation does nothing when platform module has no source directories.
- [`KT-22361`](https://youtrack.jetbrains.com/issue/KT-22361) Multiplatform: "Generate equals() and hashCode()" intention generates JVM specific code for arrays in common module
- [`KT-22552`](https://youtrack.jetbrains.com/issue/KT-22552) SimplifiableCallChain should keep formatting and comments
- [`KT-24129`](https://youtrack.jetbrains.com/issue/KT-24129) Multiplatform quick fix add implementation suggests generated source location
- [`KT-24405`](https://youtrack.jetbrains.com/issue/KT-24405) False "redundant overriding method" for abstract / default interface method combination
- [`KT-24978`](https://youtrack.jetbrains.com/issue/KT-24978) Do not highlight foldable if-then for is checks
- [`KT-25228`](https://youtrack.jetbrains.com/issue/KT-25228) "Create function" from a protected inline method should not produce a private method
- [`KT-25525`](https://youtrack.jetbrains.com/issue/KT-25525) `@Experimental`-related quick fixes are not suggested for usages in top-level property
- [`KT-25526`](https://youtrack.jetbrains.com/issue/KT-25526) `@Experimental`-related quick fixes are not suggested for usages in type alias
- [`KT-25548`](https://youtrack.jetbrains.com/issue/KT-25548) `@Experimental` API usage: "Add annotation" quick fix incorrectly modifies primary constructor
- [`KT-25609`](https://youtrack.jetbrains.com/issue/KT-25609) "Unused symbol" inspection reports annotation used only in `-Xexperimental`/`-Xuse-experimental` settings
- [`KT-25711`](https://youtrack.jetbrains.com/issue/KT-25711) "Deferred result is never used" inspection: remove `experimental` package (or whole FQN) from description
- [`KT-25712`](https://youtrack.jetbrains.com/issue/KT-25712) "Redundant 'async' call" inspection quick fix action label looks too long
- [`KT-25883`](https://youtrack.jetbrains.com/issue/KT-25883) False "redundant override" reported on boxed parameters
- [`KT-25886`](https://youtrack.jetbrains.com/issue/KT-25886) False positive "Replace 'if' with elvis operator" for nullable type
- [`KT-25968`](https://youtrack.jetbrains.com/issue/KT-25968) False positive "Remove redundant backticks" with keyword `yield`
- [`KT-26009`](https://youtrack.jetbrains.com/issue/KT-26009) "Convert to 'also'" intention adds an extra `it` expression
- [`KT-26015`](https://youtrack.jetbrains.com/issue/KT-26015) Intention to move property to constructor adds @field: qualifier to annotations
- [`KT-26179`](https://youtrack.jetbrains.com/issue/KT-26179) False negative "Boolean expression that can be simplified" for `!true`
- [`KT-26181`](https://youtrack.jetbrains.com/issue/KT-26181) Inspection for unused Deferred result: report for all functions by default
- [`KT-26185`](https://youtrack.jetbrains.com/issue/KT-26185) False positive "redundant semicolon" with if-else
- [`KT-26187`](https://youtrack.jetbrains.com/issue/KT-26187) "Cascade if can be replaced with when" loses lambda curly braces
- [`KT-26289`](https://youtrack.jetbrains.com/issue/KT-26289) Redundant let with call expression: don't report for long call chains
- [`KT-26306`](https://youtrack.jetbrains.com/issue/KT-26306) "Add annotation target" quick fix adds EXPRESSION annotation, but not SOURCE retention
- [`KT-26343`](https://youtrack.jetbrains.com/issue/KT-26343) "Replace 'if' expression with elvis expression" produces wrong code in extension function with not null type parameter
- [`KT-26353`](https://youtrack.jetbrains.com/issue/KT-26353) "Make variable immutable" is a bad name for a quickfix that changes 'var' to 'val'
- [`KT-26472`](https://youtrack.jetbrains.com/issue/KT-26472) "Maven dependency is incompatible with Kotlin 1.3+ and should be updated" inspection is not included into Kotlin Migration
- [`KT-26492`](https://youtrack.jetbrains.com/issue/KT-26492) "Make private" on annotated annotation produces nasty new line
- [`KT-26599`](https://youtrack.jetbrains.com/issue/KT-26599) "Foldable if-then" inspection marks if statements that cannot be folded using ?. operator
- [`KT-26674`](https://youtrack.jetbrains.com/issue/KT-26674) Move lambda out of parentheses is not proposed for suspend lambda
- [`KT-26676`](https://youtrack.jetbrains.com/issue/KT-26676) ReplaceWith always puts suspend lambda in parentheses
- [`KT-26810`](https://youtrack.jetbrains.com/issue/KT-26810) "Incompatible kotlinx.coroutines dependency" inspections report library built for 1.3-RC with 1.3-RC plugin

### IDE. Multiplatform

- [`KT-20368`](https://youtrack.jetbrains.com/issue/KT-20368) Unresolved reference to declarations from kotlin.reflect in common code in multi-platform project: no "Add import" quick-fix
- [`KT-26356`](https://youtrack.jetbrains.com/issue/KT-26356) New MPP doesn't work with Android projects
- [`KT-26369`](https://youtrack.jetbrains.com/issue/KT-26369) Library dependencies don't transitively pass for custom source sets at new MPP import to IDE
- [`KT-26414`](https://youtrack.jetbrains.com/issue/KT-26414) Remove old multiplatform modules templates from New Project/New Module wizard
- [`KT-26517`](https://youtrack.jetbrains.com/issue/KT-26517) `Create actual ...` generates default constructor parameter values
- [`KT-26585`](https://youtrack.jetbrains.com/issue/KT-26585) Stdlib annotations annotated with @OptionalExpectation are reported with false positive error in common module

### IDE. Navigation

- [`KT-18490`](https://youtrack.jetbrains.com/issue/KT-18490) Multiplatform project: Set text cursor correctly to file with header on navigation from impl side

### IDE. Refactorings

- [`KT-17124`](https://youtrack.jetbrains.com/issue/KT-17124) Change signature refactoring dialog unescapes escaped parameter names
- [`KT-25454`](https://youtrack.jetbrains.com/issue/KT-25454) Extract function: make default visibility private
- [`KT-26533`](https://youtrack.jetbrains.com/issue/KT-26533) Move refactoring on interface shows it as "abstract interface" in the dialog

### IDE. Tests Support

- [`KT-26793`](https://youtrack.jetbrains.com/issue/KT-26793) Left gutter run icon does not appear for JS tests in old MPP

### IDE. Ultimate

- [`KT-19309`](https://youtrack.jetbrains.com/issue/KT-19309) Spring JPA Repository IntelliJ tooling with Kotlin

### JavaScript

- [`KT-26466`](https://youtrack.jetbrains.com/issue/KT-26466) Uncaught ReferenceError: println is not defined
- [`KT-26572`](https://youtrack.jetbrains.com/issue/KT-26572) Support suspend fun main in JS
- [`KT-26628`](https://youtrack.jetbrains.com/issue/KT-26628) Support main entry-point without arguments in JS

### Libraries

#### New Features

- [`KT-25039`](https://youtrack.jetbrains.com/issue/KT-25039) Any?.hashCode() extension
- [`KT-26359`](https://youtrack.jetbrains.com/issue/KT-26359) Use JvmName on parameters of kotlin.Metadata to improve the public API
- [`KT-26398`](https://youtrack.jetbrains.com/issue/KT-26398) Coroutine context shall perform structural equality comparison on keys
- [`KT-26598`](https://youtrack.jetbrains.com/issue/KT-26598) Introduce ConcurrentModificationException actual typealias in the JVM library

#### Performance Improvements

- [`KT-18483`](https://youtrack.jetbrains.com/issue/KT-18483) Check to contains value in range can be dramatically slow 

#### Fixes

- [`KT-17716`](https://youtrack.jetbrains.com/issue/KT-17716) JS: Some kotlin.js.Math methods break Integer type safety
- [`KT-21703`](https://youtrack.jetbrains.com/issue/KT-21703) Review deprecations in stdlib for 1.3
- [`KT-21784`](https://youtrack.jetbrains.com/issue/KT-21784) Deprecate and remove org.jetbrains.annotations from kotlin-stdlib in compiler distribution
- [`KT-22423`](https://youtrack.jetbrains.com/issue/KT-22423) Deprecate mixed integer/floating point overloads of ClosedRange.contains operator
- [`KT-25217`](https://youtrack.jetbrains.com/issue/KT-25217) Raise deprecation level for mod operators to ERROR
- [`KT-25935`](https://youtrack.jetbrains.com/issue/KT-25935) Move kotlin.reflect interfaces to kotlin-stdlib-common
- [`KT-26358`](https://youtrack.jetbrains.com/issue/KT-26358) Rebuild anko for new coroutines API
- [`KT-26388`](https://youtrack.jetbrains.com/issue/KT-26388) Specialize contentDeepEquals/HashCode/ToString for arrays of unsigned types
- [`KT-26523`](https://youtrack.jetbrains.com/issue/KT-26523) EXACTLY_ONCE contract in runCatching doesn't consider lambda exceptions are caught
- [`KT-26591`](https://youtrack.jetbrains.com/issue/KT-26591) Add primitive boxing functions to stdlib
- [`KT-26594`](https://youtrack.jetbrains.com/issue/KT-26594) Change signed-to-unsigned widening conversions to sign extending
- [`KT-26595`](https://youtrack.jetbrains.com/issue/KT-26595) Deprecate common 'synchronized(Any) { }' function
- [`KT-26596`](https://youtrack.jetbrains.com/issue/KT-26596) Rename Random.nextInt/Long/Double parameters
- [`KT-26678`](https://youtrack.jetbrains.com/issue/KT-26678) Rename buildSequence/buildIterator to sequence/iterator
- [`KT-26929`](https://youtrack.jetbrains.com/issue/KT-26929) Kotlin Reflect and Proguard: can’t find referenced class  kotlin.annotations.jvm.ReadOnly/Mutable

### Reflection

- [`KT-25499`](https://youtrack.jetbrains.com/issue/KT-25499) Use-site targeted annotations on property accessors are not visible in Kotlin reflection if there's also an annotation on the property
- [`KT-25500`](https://youtrack.jetbrains.com/issue/KT-25500) Annotations on parameter setter are not visible through reflection
- [`KT-25664`](https://youtrack.jetbrains.com/issue/KT-25664) Inline classes don't work properly with reflection
- [`KT-26293`](https://youtrack.jetbrains.com/issue/KT-26293) Incorrect javaType for suspend function's returnType

### Tools. CLI

- [`KT-24613`](https://youtrack.jetbrains.com/issue/KT-24613) Support argfiles in kotlinc with "@argfile"
- [`KT-25862`](https://youtrack.jetbrains.com/issue/KT-25862) Release '-Xprogressive' as '-progressive'
- [`KT-26122`](https://youtrack.jetbrains.com/issue/KT-26122) Support single quotation marks in argfiles

### Tools. Gradle

- [`KT-25680`](https://youtrack.jetbrains.com/issue/KT-25680) Gradle plugin: version with non-experimental coroutines and no related settings still runs compiler with `-Xcoroutines` option
- [`KT-26253`](https://youtrack.jetbrains.com/issue/KT-26253) New MPP model shouldn't generate `metadataMain` and `metadataTest` source sets on IDE import
- [`KT-26383`](https://youtrack.jetbrains.com/issue/KT-26383) Common modules dependencies are not mapped at import of a composite multiplatform project with project dependencies into IDE
- [`KT-26515`](https://youtrack.jetbrains.com/issue/KT-26515) Support -Xcommon-sources in new MPP
- [`KT-26641`](https://youtrack.jetbrains.com/issue/KT-26641) In new MPP, Gradle task for building classes has a name unexpected for GradleProjectTaskRunner
- [`KT-26784`](https://youtrack.jetbrains.com/issue/KT-26784) Support non-kts scripts discovery and compilation in gradle

### Tools. JPS

- [`KT-26072`](https://youtrack.jetbrains.com/issue/KT-26072) MPP compilation issue
- [`KT-26254`](https://youtrack.jetbrains.com/issue/KT-26254) JPS build for new MPP model doesn't work: kotlinFacet?.settings?.sourceSetNames is empty

### Tools. kapt

- [`KT-25374`](https://youtrack.jetbrains.com/issue/KT-25374) Kapt: Build fails with Unresolved local class
- [`KT-26540`](https://youtrack.jetbrains.com/issue/KT-26540) kapt3 fails to handle to-be-generated superclasses
