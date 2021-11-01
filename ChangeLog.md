## 1.6.0-RC2

### Compiler

#### New Features

- [`KT-43919`](https://youtrack.jetbrains.com/issue/KT-43919) Support loading Java annotations on base classes and implementing interfaces'  type arguments

#### Performance Improvements

- [`KT-45185`](https://youtrack.jetbrains.com/issue/KT-45185) FIR2IR: get rid of IrBuiltIns usages

#### Fixes

- [`KT-49477`](https://youtrack.jetbrains.com/issue/KT-49477) Has ran into recursion problem with two interdependant delegates
- [`KT-49371`](https://youtrack.jetbrains.com/issue/KT-49371) JVM / IR: "NoSuchMethodError" with multiple inheritance
- [`KT-49294`](https://youtrack.jetbrains.com/issue/KT-49294) Turning FlowCollector into 'fun interface' leads to AbstractMethodError
- [`KT-18282`](https://youtrack.jetbrains.com/issue/KT-18282) Companion object referencing it's own method during construction compiles successfully but fails at runtime with VerifyError
- [`KT-25289`](https://youtrack.jetbrains.com/issue/KT-25289) Prohibit access to class members in the super constructor call of its companion and nested object
- [`KT-32753`](https://youtrack.jetbrains.com/issue/KT-32753) Prohibit @JvmField on property in primary constructor that overrides interface property
- [`KT-43433`](https://youtrack.jetbrains.com/issue/KT-43433) `Suspend conversion is disabled` message in cases where it is not supported and quickfix to update language version is suggested
- [`KT-49209`](https://youtrack.jetbrains.com/issue/KT-49209) Default upper bound for type variables should be non-null
- [`KT-22562`](https://youtrack.jetbrains.com/issue/KT-22562) Deprecate calls to "suspend" named functions with single dangling lambda argument
- [`KT-49335`](https://youtrack.jetbrains.com/issue/KT-49335) NPE in `RepeatedAnnotationLowering.wrapAnnotationEntriesInContainer` when using `@Repeatable` annotation from different file
- [`KT-49322`](https://youtrack.jetbrains.com/issue/KT-49322) Postpone promoting warnings to errors for `ProperTypeInferenceConstraintsProcessing` feature
- [`KT-49285`](https://youtrack.jetbrains.com/issue/KT-49285) Exception on nested builder inference calls
- [`KT-49101`](https://youtrack.jetbrains.com/issue/KT-49101) IllegalArgumentException: ClassicTypeSystemContext couldn't handle: Captured(out Number)
- [`KT-36399`](https://youtrack.jetbrains.com/issue/KT-36399) Gradually support TYPE_USE nullability annotations read from class-files
- [`KT-11454`](https://youtrack.jetbrains.com/issue/KT-11454) Load annotations on TYPE_USE/TYPE_PARAMETER positions from Java class-files
- [`KT-18768`](https://youtrack.jetbrains.com/issue/KT-18768) @Notnull annotation from Java does not work with varargs
- [`KT-24392`](https://youtrack.jetbrains.com/issue/KT-24392) Nullability of Java arrays is read incorrectly if @Nullable annotation has both targets TYPE_USE and VALUE_PARAMETER
- [`KT-48157`](https://youtrack.jetbrains.com/issue/KT-48157) FIR: incorrect resolve with built-in names in use
- [`KT-46409`](https://youtrack.jetbrains.com/issue/KT-46409) FIR: erroneous resolve to qualifier instead of extension
- [`KT-44566`](https://youtrack.jetbrains.com/issue/KT-44566) `FirConflictsChecker` do not check for conflicting overloads across multiple files
- [`KT-37318`](https://youtrack.jetbrains.com/issue/KT-37318) FIR: Discuss treating flexible bounded constraints in inference
- [`KT-45989`](https://youtrack.jetbrains.com/issue/KT-45989) FIR: wrong callable reference type inferred
- [`KT-46058`](https://youtrack.jetbrains.com/issue/KT-46058) [FIR] Remove state from some checkers
- [`KT-45973`](https://youtrack.jetbrains.com/issue/KT-45973) FIR: wrong projection type inferred
- [`KT-43083`](https://youtrack.jetbrains.com/issue/KT-43083) [FIR] False positive 'HIDDEN' on internal
- [`KT-46727`](https://youtrack.jetbrains.com/issue/KT-46727) Report warning on contravariant usages of star projected argument from Java
- [`KT-40668`](https://youtrack.jetbrains.com/issue/KT-40668) FIR: Ambiguity on qualifier when having multiple different same-named objects in near scopes
- [`KT-37081`](https://youtrack.jetbrains.com/issue/KT-37081) [FIR] errors NO_ELSE_IN_WHEN and INCOMPATIBLE_TYPES absence
- [`KT-48162`](https://youtrack.jetbrains.com/issue/KT-48162) NON_VARARG_SPREAD isn't reported on *toTypedArray() call
- [`KT-45118`](https://youtrack.jetbrains.com/issue/KT-45118) ClassCastException caused by parent and child class in if-else
- [`KT-47605`](https://youtrack.jetbrains.com/issue/KT-47605) Kotlin/Native: switch to LLD linker for MinGW targets
- [`KT-44436`](https://youtrack.jetbrains.com/issue/KT-44436) Support default not null annotations to enhance T into T!!
- [`KT-49190`](https://youtrack.jetbrains.com/issue/KT-49190) Increase stub versions

### Docs & Examples

- [`KT-48534`](https://youtrack.jetbrains.com/issue/KT-48534) Wrong compiler argument for RequiresOptIn

### IDE. Debugger

- [`KT-47970`](https://youtrack.jetbrains.com/issue/KT-47970) AE: "Either library or explicit name have to be provided <built-ins module>" in IR debugger tests

### JavaScript

- [`KT-43783`](https://youtrack.jetbrains.com/issue/KT-43783) KJS / IR: companion object and nested objects are not exported
- [`KT-47524`](https://youtrack.jetbrains.com/issue/KT-47524) KJS / IR: Treat protected members as part of exported API

### Libraries

- [`KT-46229`](https://youtrack.jetbrains.com/issue/KT-46229) Bring back Duration factory extension properties

### Native

- [`KT-49384`](https://youtrack.jetbrains.com/issue/KT-49384) Kotlin/Native: Unexpected variance in super type argument: out @0
- [`KT-49234`](https://youtrack.jetbrains.com/issue/KT-49234) SIGSEGV using the new memory manager in release in Kotlin 1.6.0-RC in MacosX64

### Tools. Gradle

- [`KT-49189`](https://youtrack.jetbrains.com/issue/KT-49189) In Gradle, dependencies on an MPP with Android+JVM fail to resolve in pure-Java projects
- [`KT-48830`](https://youtrack.jetbrains.com/issue/KT-48830) Change deprecation level to 'ERROR' for 'KotlinGradleSubplugin'
- [`KT-48264`](https://youtrack.jetbrains.com/issue/KT-48264) Cannot write Kotlin build report unless directory exists
- [`KT-45504`](https://youtrack.jetbrains.com/issue/KT-45504) Deprecate Gradle option KotlinJvmOptions.useIR since 1.5

### Tools. Gradle. JS

- [`KT-49124`](https://youtrack.jetbrains.com/issue/KT-49124) KJS / Gradle: Unable to load '@webpack-cli/serve' command
- [`KT-49201`](https://youtrack.jetbrains.com/issue/KT-49201) KJS / Gradle: NPM dependencies resolution may fail on parallel builds

### Tools. Gradle. Multiplatform

- [`KT-48709`](https://youtrack.jetbrains.com/issue/KT-48709) MPP: Task compileKotlinMacosX64 fails on matching native variants if ktlint presented

### Tools. REPL

- [`KT-47783`](https://youtrack.jetbrains.com/issue/KT-47783) REPL: Keywords completion appears after numeric and string literals

### Tools. Scripts

- [`KT-49400`](https://youtrack.jetbrains.com/issue/KT-49400) Script resolver options can't take values with special symbols (/, \, $, :, .) in them

## 1.6.0-RC

### Compiler

#### Fixes

- [`KT-49157`](https://youtrack.jetbrains.com/issue/KT-49157) Tail-call optimization miss with cast to type parameter
- [`KT-48778`](https://youtrack.jetbrains.com/issue/KT-48778) -Xtype-enhancement-improvements-strict-mode not respecting @NonNull annotation for property accesses?
- [`KT-46437`](https://youtrack.jetbrains.com/issue/KT-46437) NI: "Throwable: Resolution error of this type shouldn't occur for resolve if as a call" caused by reflectively accessing private property inside "if/else" or "when" expression
- [`KT-48590`](https://youtrack.jetbrains.com/issue/KT-48590) IllegalArgumentException: ClassicTypeSystemContext couldn't handle: Captured(*) reified type class reference
- [`KT-48261`](https://youtrack.jetbrains.com/issue/KT-48261) "overload resolution ambiguity" for JSpecify+jsr305-annotated Java List implementation
- [`KT-48633`](https://youtrack.jetbrains.com/issue/KT-48633) Can't infer builder inference's type argument across local class
- [`KT-49136`](https://youtrack.jetbrains.com/issue/KT-49136) JVM IR: NPE with safe call chain and property set to null by reflection
- [`KT-48912`](https://youtrack.jetbrains.com/issue/KT-48912) K/N `Symbol with IrSimpleFunctionSymbolImpl is unbound` and `JS Validation failed in file shaders.kt`
- [`KT-48928`](https://youtrack.jetbrains.com/issue/KT-48928) Prohibit using old JVM backend with language version >= 1.6
- [`KT-41978`](https://youtrack.jetbrains.com/issue/KT-41978) NI: Kotlin fails  to infer type of function argument
- [`KT-48732`](https://youtrack.jetbrains.com/issue/KT-48732) JVM / IR: MalformedParameterizedTypeException is thrown when a Spring Bean of suspending function type is registered
- [`KT-47841`](https://youtrack.jetbrains.com/issue/KT-47841) Turning LV to 1.6 breaks some diagnostics based on jspecify annotations
- [`KT-48498`](https://youtrack.jetbrains.com/issue/KT-48498) JVM IR: IllegalAccessError with inline function call and property delegation from different module
- [`KT-48319`](https://youtrack.jetbrains.com/issue/KT-48319) JVM / IR: AssertionError: FUN caused by suspend lambda inside anonymous function
- [`KT-48835`](https://youtrack.jetbrains.com/issue/KT-48835) Psi2ir: vararg parameter value is lost when translating adapted function reference to base class member
- [`KT-46908`](https://youtrack.jetbrains.com/issue/KT-46908) JVM / IR: do not wrap fun interface implementation into another SAM adapter if it inherits from a functional type
- [`KT-48927`](https://youtrack.jetbrains.com/issue/KT-48927) JVM IR: "VerifyError: Bad invokespecial instruction: current class isn't assignable to reference class" when up-casting and read a base class's private property that has a custom getter in the base class's public function
- [`KT-48992`](https://youtrack.jetbrains.com/issue/KT-48992) Postpone migration to new operator resolution scheme for integer literals
- [`KT-48290`](https://youtrack.jetbrains.com/issue/KT-48290) Type bounds warning based on Java annotations not issues with language level 1.6
- [`KT-47920`](https://youtrack.jetbrains.com/issue/KT-47920) There is no warning on violated nullability of type parameter in accordance with java nullability annotation
- [`KT-48851`](https://youtrack.jetbrains.com/issue/KT-48851) Keep using warn mode for jspecify in 1.6
- [`KT-46829`](https://youtrack.jetbrains.com/issue/KT-46829) IR: NullPointerException caused by setting scoped generic extension var
- [`KT-44843`](https://youtrack.jetbrains.com/issue/KT-44843) PSI2IR: "org.jetbrains.kotlin.psi2ir.generators.ErrorExpressionException: null: KtCallExpression" with delegate who has name or parameter with the same name as a property
- [`KT-42972`](https://youtrack.jetbrains.com/issue/KT-42972) Forbid protected constructor calls from public inline functions
- [`KT-45378`](https://youtrack.jetbrains.com/issue/KT-45378) Prohibit super calls in public-api inline functions
- [`KT-48515`](https://youtrack.jetbrains.com/issue/KT-48515) JSpecify: If a class has a @Nullable type-parameter bound, Kotlin should still treat unbounded wildcards like platform types
- [`KT-48825`](https://youtrack.jetbrains.com/issue/KT-48825) JVM IR: NPE with delegated property "by this" to base class
- [`KT-48535`](https://youtrack.jetbrains.com/issue/KT-48535) Make EXPERIMENTAL_ANNOTATION_ON_OVERRIDE warning
- [`KT-48478`](https://youtrack.jetbrains.com/issue/KT-48478) JVM IR: Coroutines 1.5.1 + Kotlin 1.5.30 - ClassCastException: CompletedContinuation cannot be cast to DispatchedContinuation
- [`KT-48671`](https://youtrack.jetbrains.com/issue/KT-48671) JVM / IR: "AssertionError: Primitive array expected: CLASS IR_EXTERNAL_DECLARATION_STUB CLASS"
- [`KT-46181`](https://youtrack.jetbrains.com/issue/KT-46181) JVM IR: private @JvmStatic function is generated in the outer class instead of companion object, which breaks existing calls via JNI or reflection (e.g. JUnit @MethodSource)
- [`KT-48736`](https://youtrack.jetbrains.com/issue/KT-48736) JVM IR: assert in SyntheticAccessorLowering when inline function attempts to access package-private field from Java
- [`KT-20542`](https://youtrack.jetbrains.com/issue/KT-20542) IllegalAccessError on calling private function with default parameters from internal inline function used in another package
- [`KT-48331`](https://youtrack.jetbrains.com/issue/KT-48331) JVM / IR: "VerifyError: Bad access to protected data in invokevirtual" when a sealed class uses another sealed class in its same hierarchy level as a constructor parameter
- [`KT-48659`](https://youtrack.jetbrains.com/issue/KT-48659) JVM / IR: Referential equality returns true for different instances
- [`KT-48606`](https://youtrack.jetbrains.com/issue/KT-48606) [1.6] Instantiated annotations do not implement hashCode correctly/consistently
- [`KT-48316`](https://youtrack.jetbrains.com/issue/KT-48316) "No value passed for parameter" regression with Java annotation default values with JSR-305
- [`KT-48391`](https://youtrack.jetbrains.com/issue/KT-48391) JVM / IR: "AssertionError: SyntheticAccessorLowering should not attempt to modify other files!" caused by class which inherits interface which has default function with default argument from companion const val

### IDE. Gradle Integration

- [`KT-46273`](https://youtrack.jetbrains.com/issue/KT-46273) MPP: Don't fail import for case of missed platform in source set structure
- [`KT-48823`](https://youtrack.jetbrains.com/issue/KT-48823) Improve error reporting on import when configuration phase in Gradle failed
- [`KT-48504`](https://youtrack.jetbrains.com/issue/KT-48504) MPP: UninitializedPropertyAccessException on import if new hierarchical mpp flag conflicts with other flags

### Libraries

#### New Features

- [`KT-46423`](https://youtrack.jetbrains.com/issue/KT-46423) infix extension fun Comparable<T>.compareTo
- [`KT-47421`](https://youtrack.jetbrains.com/issue/KT-47421) Stabilize collection builders

#### Performance Improvements

- [`KT-45438`](https://youtrack.jetbrains.com/issue/KT-45438) Remove brittle ?contains? optimization in minus/removeAll/retainAll

#### Fixes

- [`KT-47304`](https://youtrack.jetbrains.com/issue/KT-47304) Random#nextLong generates value outside provided range
- [`KT-48999`](https://youtrack.jetbrains.com/issue/KT-48999) Align behavior of some JS functions with their JVM counterpart
- [`KT-28378`](https://youtrack.jetbrains.com/issue/KT-28378) Different behavior of Regex replace function in Java and JS when replacement string contains group reference
- [`KT-46229`](https://youtrack.jetbrains.com/issue/KT-46229) Bring back Duration factory extension properties
- [`KT-46243`](https://youtrack.jetbrains.com/issue/KT-46243) Typography.leftGuillemete and Typography.rightGuillemete are named inconsistent with standard
- [`KT-46101`](https://youtrack.jetbrains.com/issue/KT-46101) Review deprecations in stdlib for 1.6
- [`KT-48456`](https://youtrack.jetbrains.com/issue/KT-48456) Introduce Common (multi-platform) readln() and readlnOrNull() top-level functions
- [`KT-38754`](https://youtrack.jetbrains.com/issue/KT-38754) Deprecate appendln in favor of appendLine

### Native

- [`KT-48807`](https://youtrack.jetbrains.com/issue/KT-48807) Cinterop: cannot create bindings for a framework when Xcode 13 RC is installed

### Tools. CLI

- [`KT-49007`](https://youtrack.jetbrains.com/issue/KT-49007) Support three previous API versions

### Tools. Compiler Plugins

- [`KT-48842`](https://youtrack.jetbrains.com/issue/KT-48842) Compiler crash: Symbol with IrFieldSymbolImpl is unbound
- [`KT-48117`](https://youtrack.jetbrains.com/issue/KT-48117) Kotlin AllOpen Plugin should open private methods

### Tools. Gradle

- [`KT-48745`](https://youtrack.jetbrains.com/issue/KT-48745) JVM target compatibility check should be disabled when Java sources are empty
- [`KT-49066`](https://youtrack.jetbrains.com/issue/KT-49066) Setting kotlinOptions.modulePath in an android project breaks incremental compilation
- [`KT-48847`](https://youtrack.jetbrains.com/issue/KT-48847) Remove deprecated kotlin options marked for removal after 1.5
- [`KT-48245`](https://youtrack.jetbrains.com/issue/KT-48245) KGP makes compileOnly configuration resolvable
- [`KT-48768`](https://youtrack.jetbrains.com/issue/KT-48768) Misleading 'jdkHome' deprecation message

### Tools. Gradle. Multiplatform

- [`KT-48919`](https://youtrack.jetbrains.com/issue/KT-48919) Gradle multiplatform plugin 1.6.0-M1 does not accept apiVersion = "1.7"

### Tools. Scripts

- [`KT-49012`](https://youtrack.jetbrains.com/issue/KT-49012) Compiling .kts script with inner class declaration fails with Backend Internal Error caused by AE: "Local class constructor can't have dispatch receiver"

### Tools. kapt

- [`KT-45545`](https://youtrack.jetbrains.com/issue/KT-45545) Kapt is not compatible with JDK 16+
- [`KT-47934`](https://youtrack.jetbrains.com/issue/KT-47934) KaptJavaLog is unable to map stub back to the kotlin source

## 1.6.0-M1

### Android

- [`KT-48019`](https://youtrack.jetbrains.com/issue/KT-48019) Bundle Kotlin Tooling Metadata into apk artifacts
- [`KT-47733`](https://youtrack.jetbrains.com/issue/KT-47733) JVM / IR: Android Synthetic don't generate _findCachedViewById function

### Compiler

#### New Features

- [`KT-12794`](https://youtrack.jetbrains.com/issue/KT-12794) Allow runtime retention repeatable annotations when compiling under Java 8
- [`KT-47984`](https://youtrack.jetbrains.com/issue/KT-47984) In-place arguments inlining for @InlineOnly functions
- [`KT-48194`](https://youtrack.jetbrains.com/issue/KT-48194) Try to resolve calls where we don't have enough type information, using the builder inference despite the presence of the annotation
- [`KT-26245`](https://youtrack.jetbrains.com/issue/KT-26245) Add ability to specify generic type parameters as not-null
- [`KT-45949`](https://youtrack.jetbrains.com/issue/KT-45949) Kotlin/Native: Improve bound check elimination
- [`KT-47699`](https://youtrack.jetbrains.com/issue/KT-47699) Support programmatic creation of class annotations and corresponding feature flag on JVM
- [`KT-47736`](https://youtrack.jetbrains.com/issue/KT-47736) Support conversion from regular functional types to suspending ones in JVM IR
- [`KT-39055`](https://youtrack.jetbrains.com/issue/KT-39055) Support property delegate created via synthetic method instead of field

#### Performance Improvements

- [`KT-33835`](https://youtrack.jetbrains.com/issue/KT-33835) Bytecode including unnecessary null checks for safe calls where left-hand side is non-nullable
- [`KT-41510`](https://youtrack.jetbrains.com/issue/KT-41510) Compilation of kotlin html DSL is still too slow
- [`KT-48211`](https://youtrack.jetbrains.com/issue/KT-48211) We spend a lot of time in ExpectActual declaration checker when there is very small amount of actual/expect declaration
- [`KT-39054`](https://youtrack.jetbrains.com/issue/KT-39054) Optimize delegated properties which call get/set on the given KProperty instance on JVM
- [`KT-47918`](https://youtrack.jetbrains.com/issue/KT-47918) JVM / IR: Performance degradation with const-bound for-cycles
- [`KT-47785`](https://youtrack.jetbrains.com/issue/KT-47785) Compilation time increased when trying to compile AssertJ DB expression in 1.5.21
- [`KT-46615`](https://youtrack.jetbrains.com/issue/KT-46615) Don't generate nullability assertions in methods for directly invoked lambdas

#### Fixes

- [`KT-48523`](https://youtrack.jetbrains.com/issue/KT-48523) Kotlin/Native: cross-compilation from Linux to MinGW not working when `platform.posix` is used
- [`KT-48295`](https://youtrack.jetbrains.com/issue/KT-48295) JVM / IR: VerifyError: Bad access to protected data in getfield
- [`KT-48440`](https://youtrack.jetbrains.com/issue/KT-48440) JVM IR: Missing checkcast in generated bytecode causes VerifyError in Kotlin 1.5.30
- [`KT-48794`](https://youtrack.jetbrains.com/issue/KT-48794) Breaking change in 1.5.30: Builder inference lambda contains inapplicable calls so {1} cant be inferred
- [`KT-48653`](https://youtrack.jetbrains.com/issue/KT-48653) Warnings on non-exhaustive when statements missing in some cases with 1.6
- [`KT-48394`](https://youtrack.jetbrains.com/issue/KT-48394) JVM: Invalid locals caused by unboxing bytecode optimization
- [`KT-48380`](https://youtrack.jetbrains.com/issue/KT-48380) kotlin.RuntimeException: Unexpected receiver type
- [`KT-47855`](https://youtrack.jetbrains.com/issue/KT-47855) Kotlin/Native: compilation fails due to Escape Analysis
- [`KT-48291`](https://youtrack.jetbrains.com/issue/KT-48291) False positive [ACTUAL_MISSING] Declaration must be marked with 'actual' when implementing actual interface
- [`KT-48613`](https://youtrack.jetbrains.com/issue/KT-48613) Kotlin/Native fails to compile debug binaries for watchosArm64 target
- [`KT-48618`](https://youtrack.jetbrains.com/issue/KT-48618) Enable by default "suspend conversion" feature in 1.6
- [`KT-48543`](https://youtrack.jetbrains.com/issue/KT-48543) Native compiler crashes because of bridges for $default stubs
- [`KT-47328`](https://youtrack.jetbrains.com/issue/KT-47328) JVM / IR: NoSuchFieldError with missing CHECKCAST
- [`KT-47638`](https://youtrack.jetbrains.com/issue/KT-47638) Drop EXPERIMENTAL_IS_NOT_ENABLED diagnostic
- [`KT-48349`](https://youtrack.jetbrains.com/issue/KT-48349) OptIn markers are forbidden on local variable / value parameter / property getter only in presence of explicit Target annotation
- [`KT-48589`](https://youtrack.jetbrains.com/issue/KT-48589) KotlinTypeRefiner is lost, leading to TYPE_MISMATCH and OVERLOAD_RESOLUTION_AMBIGUITY issues with MPP projects
- [`KT-48615`](https://youtrack.jetbrains.com/issue/KT-48615) Inconsistent behavior with integer literals overflow (Implementation)
- [`KT-47937`](https://youtrack.jetbrains.com/issue/KT-47937) Implement deprecation of computing constant values of complex boolean expressions in when condition branches and conditions of loops
- [`KT-47772`](https://youtrack.jetbrains.com/issue/KT-47772) False negative WRONG_ANNOTATION_TARGET on type argument to function call
- [`KT-48552`](https://youtrack.jetbrains.com/issue/KT-48552) Kotlin/Native: iosArm64 debug build fails in 1.6.0-M1-139
- [`KT-46182`](https://youtrack.jetbrains.com/issue/KT-46182) Native: prohibit using dots in identifiers
- [`KT-47917`](https://youtrack.jetbrains.com/issue/KT-47917) JVM: "UTF8 string too large" caused by a big string
- [`KT-46230`](https://youtrack.jetbrains.com/issue/KT-46230) JVM IR: "IllegalArgumentException: Null argument in ExpressionCodegen for parameter VALUE_PARAMETER MOVED_DISPATCH_RECEIVER" with value class overriding function with default parameter
- [`KT-48302`](https://youtrack.jetbrains.com/issue/KT-48302) FIR: Investigate not-null assertion on generic Java method
- [`KT-47422`](https://youtrack.jetbrains.com/issue/KT-47422) -Xjspecify-annotations: If a class has a @Nullable type-parameter bound, Kotlin should still treat some users' type arguments as platform types
- [`KT-48500`](https://youtrack.jetbrains.com/issue/KT-48500) AE: "Last parameter type of suspend function must be Continuation, but it is kotlin.coroutines.experimental.Continuation" for `kotlin-stdlib-common` library
- [`KT-48469`](https://youtrack.jetbrains.com/issue/KT-48469) Problem with properties lazy initialization while using kotlinx.serialization plugin
- [`KT-48432`](https://youtrack.jetbrains.com/issue/KT-48432) Regression in IntRange.contains (and probably other ranges too) when used in-place
- [`KT-48361`](https://youtrack.jetbrains.com/issue/KT-48361) INTEGER_OPERATOR_RESOLVE_WILL_CHANGE is not reported in some positions
- [`KT-44855`](https://youtrack.jetbrains.com/issue/KT-44855) "AssertionError: SyntheticAccessorLowering should not attempt to modify other files" on smart cast of protected field owner
- [`KT-47499`](https://youtrack.jetbrains.com/issue/KT-47499) JVM / IR: java.lang.VerifyError: Bad access to protected data in invokevirtual when trying to clone the result of lambda invocation that is "this"  in an extension function
- [`KT-46451`](https://youtrack.jetbrains.com/issue/KT-46451) JVM Debugging: stepping on finally block end brace line before stepping into finally code
- [`KT-48329`](https://youtrack.jetbrains.com/issue/KT-48329) It's impossible to infer a type variables based on several builder inference lambdas
- [`KT-48193`](https://youtrack.jetbrains.com/issue/KT-48193) Don't use the builder inference for calls which can be resolved without it
- [`KT-46450`](https://youtrack.jetbrains.com/issue/KT-46450) JVM Debugging: some break statements in catch blocks have no line numbers and you cannot set breakpoints on them
- [`KT-48172`](https://youtrack.jetbrains.com/issue/KT-48172) "IllegalStateException: Cannot serialize error type: [ERROR : <LOOP IN SUPERTYPES>]" in 1.5.21 with java kotlin interop
- [`KT-48262`](https://youtrack.jetbrains.com/issue/KT-48262) "Inconsistent type" with JSpecify @NullMarked
- [`KT-46697`](https://youtrack.jetbrains.com/issue/KT-46697) IllegalStateException: IrTypeAliasSymbol expected: Unbound public symbol for public kotlinx.coroutines/CancellationException|null[0] compiling KMM module for Kotlin/Native with Kotlin 1.5
- [`KT-47285`](https://youtrack.jetbrains.com/issue/KT-47285) IR deserialization exception when dependency KLIB has class instead of typealias
- [`KT-41378`](https://youtrack.jetbrains.com/issue/KT-41378) Compilation failed: Deserializer for declaration public kotlinx.coroutines/SingleThreadDispatcher|null[0] is not found
- [`KT-47988`](https://youtrack.jetbrains.com/issue/KT-47988) JVM / IR: "VerifyError: Bad type on operand stack" when invoking apply with a local method reference
- [`KT-47833`](https://youtrack.jetbrains.com/issue/KT-47833) False positive "Type argument is not within its bounds " with upcasting in 1.5.30-M1
- [`KT-47911`](https://youtrack.jetbrains.com/issue/KT-47911) Native compiler on ios_arm64 target generates movi.2d instructions, which are mishandled by Apple hardware
- [`KT-14392`](https://youtrack.jetbrains.com/issue/KT-14392) Repeated annotation with use site target is not detected for getter and setter
- [`KT-47493`](https://youtrack.jetbrains.com/issue/KT-47493) Missed frontend diagnostic in try/catch
- [`KT-47597`](https://youtrack.jetbrains.com/issue/KT-47597) JVM IR: if statement doesn't eval correctly on 1.5.20 possible nullable type differences.
- [`KT-47922`](https://youtrack.jetbrains.com/issue/KT-47922) False negative type mismatch on empty when as last statement of lambda
- [`KT-47830`](https://youtrack.jetbrains.com/issue/KT-47830) Some code doesn't compile with unrestricted builder inference
- [`KT-34594`](https://youtrack.jetbrains.com/issue/KT-34594) Do not generate fake debugger variables initialization for @InlineOnly functions
- [`KT-42139`](https://youtrack.jetbrains.com/issue/KT-42139) NI: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER for emptyList / listOf (with no arguments) / emptyMap / mapOf (with no arguments) inside if block inside `sequence` block
- [`KT-47749`](https://youtrack.jetbrains.com/issue/KT-47749) Incorrect scope of a local variable inside the coroutine
- [`KT-47527`](https://youtrack.jetbrains.com/issue/KT-47527) JVM / IR ClassCastException: "kotlin.Unit cannot be cast to java.lang.String"
- [`KT-47941`](https://youtrack.jetbrains.com/issue/KT-47941) "IllegalStateException: Expected some types" on a call with several excepted type constraints
- [`KT-47854`](https://youtrack.jetbrains.com/issue/KT-47854) "IllegalArgumentException: Type is inconsistent" with Android's @Nullable annotation starting in Kotlin 1.5.20
- [`KT-47899`](https://youtrack.jetbrains.com/issue/KT-47899) "AssertionError: Intersection type should not be marked nullable" with 1.5.21
- [`KT-47846`](https://youtrack.jetbrains.com/issue/KT-47846) Stack overflow when handling enhanced recursive type parameter
- [`KT-47747`](https://youtrack.jetbrains.com/issue/KT-47747) Introduce specific error for calls which could be resolved only with unrestricted builder inference
- [`KT-47840`](https://youtrack.jetbrains.com/issue/KT-47840) JVM / IR: "IllegalStateException: No mapping for symbol: VALUE_PARAMETER name: x" in nested local functions with recursive calls
- [`KT-46448`](https://youtrack.jetbrains.com/issue/KT-46448) JVM Debugging: Locals in finally blocks not always duplicated when the finally block is
- [`KT-47396`](https://youtrack.jetbrains.com/issue/KT-47396) <?> in @NullMarked code should permit nullable types
- [`KT-47716`](https://youtrack.jetbrains.com/issue/KT-47716) JVM / IR: NoSuchMethodError when trying to get MAX_VALUE from ULong in non-trivial try/finally context
- [`KT-47762`](https://youtrack.jetbrains.com/issue/KT-47762) JVM / IR: Properties with the same signatures in inline class and its companion object crashes the compiler with NullPointerException
- [`KT-47729`](https://youtrack.jetbrains.com/issue/KT-47729) False positive INTEGER_OPERATOR_RESOLVE_WILL_CHANGE warning: "expression will be resolved to Int in future releases"
- [`KT-47741`](https://youtrack.jetbrains.com/issue/KT-47741) JVM / IR: VerifyError: Bad type on operand stack with iterator and invoking method reference to IntIterator
- [`KT-43696`](https://youtrack.jetbrains.com/issue/KT-43696) ClassFormatError on @JvmStatic external fun in interface companion object
- [`KT-47715`](https://youtrack.jetbrains.com/issue/KT-47715) JVM / IR, R8: External getter cannot be represented in dex format
- [`KT-47744`](https://youtrack.jetbrains.com/issue/KT-47744) UninitializedPropertyAccessException compiler exception on nested builder inference calls
- [`KT-47724`](https://youtrack.jetbrains.com/issue/KT-47724) Type inference: False positive "Returning type parameter has been inferred to Nothing implicitly"
- [`KT-47684`](https://youtrack.jetbrains.com/issue/KT-47684) Add warning on `is` checks which are always false
- [`KT-47685`](https://youtrack.jetbrains.com/issue/KT-47685) False positive CAST_NEVER_SUCCEEDS on variable of intersection type
- [`KT-32188`](https://youtrack.jetbrains.com/issue/KT-32188) NI: False positive "This cast can never succeed"
- [`KT-35687`](https://youtrack.jetbrains.com/issue/KT-35687) NI: Poor cast can never succeed [CAST_NEVER_SUCCEEDS]
- [`KT-41331`](https://youtrack.jetbrains.com/issue/KT-41331) False negative USELESS_IS_CHECK with null
- [`KT-47609`](https://youtrack.jetbrains.com/issue/KT-47609) JVM IR: "AssertionError: Unexpected number of type arguments" when compiling an extension property with annotation and it extends a value class with a generic parameter
- [`KT-47589`](https://youtrack.jetbrains.com/issue/KT-47589) Using RequiresOptIn annotation on constructor property results in error even if the annotation has no VALUE_PARAMETER target
- [`KT-47413`](https://youtrack.jetbrains.com/issue/KT-47413) FIR: Rework FirDelegatedScope
- [`KT-47120`](https://youtrack.jetbrains.com/issue/KT-47120) JVM IR: NoClassDefFoundError when there are an extension and a regular function with the same name
- [`KT-47492`](https://youtrack.jetbrains.com/issue/KT-47492) Illegal use of DUP

### IDE. Gradle Integration

- [`KT-47463`](https://youtrack.jetbrains.com/issue/KT-47463) MPP: Import fails with `Task 'runCommonizer' not found in root project` if Kotlin configured only in module

### IDE. JS

- [`KT-47557`](https://youtrack.jetbrains.com/issue/KT-47557) KJS: With NPM dependency IDEA import fails when performed before Gradle build

### IDE. Multiplatform

- [`KT-47604`](https://youtrack.jetbrains.com/issue/KT-47604) kotlin-stdlib-common leaks into dependencies of Android-specific source sets

### JavaScript

- [`KT-47700`](https://youtrack.jetbrains.com/issue/KT-47700) Support instantiation of annotation classes on JS
- [`KT-46204`](https://youtrack.jetbrains.com/issue/KT-46204) KJS / IR: Support `SuspendFunctionN` as super type
- [`KT-48344`](https://youtrack.jetbrains.com/issue/KT-48344) KJS / IR: incorrect call with vararg argument from suspend function
- [`KT-46551`](https://youtrack.jetbrains.com/issue/KT-46551) KJS / IR: Add a basic sourcemap generation
- [`KT-47751`](https://youtrack.jetbrains.com/issue/KT-47751) Kotlin/JS: IR + IC: "argument has no effect without source map" warnings on build

### Libraries

- [`KT-48587`](https://youtrack.jetbrains.com/issue/KT-48587) Deprecate some of JS-only stdlib API
- [`KT-48584`](https://youtrack.jetbrains.com/issue/KT-48584) Introduce JVM readln() and readlnOrNull() top-level functions
- [`KT-39328`](https://youtrack.jetbrains.com/issue/KT-39328) Make builder collection implementations serializable
- [`KT-47676`](https://youtrack.jetbrains.com/issue/KT-47676) K/JS: MatchResult.next() returns no expected next match if called after `matchEntire`
- [`KT-39166`](https://youtrack.jetbrains.com/issue/KT-39166) Nothing is silently mapped to Void in arguments of the type passed to typeOf

### Native

- [`KT-48566`](https://youtrack.jetbrains.com/issue/KT-48566) ExceptionInInitializerError when configuring Gradle project with kotlin-multiplatform plugin on a host unsupported by Kotlin/Native
- [`KT-48591`](https://youtrack.jetbrains.com/issue/KT-48591) Kotlin/Native: Char.isHighSurrogate and Char.isLowSurrogate return wrong result for macosArm64 and iosArm64 with compiler cache enabled
- [`KT-48491`](https://youtrack.jetbrains.com/issue/KT-48491) CInterop broke in Kotlin 1.5.30
- [`KT-48039`](https://youtrack.jetbrains.com/issue/KT-48039) Native: support shaded (aka embeddable) compiler jar in Gradle plugin
- [`KT-42693`](https://youtrack.jetbrains.com/issue/KT-42693) Remove dependency on ncurses5 library
- [`KT-47424`](https://youtrack.jetbrains.com/issue/KT-47424) StackOverflowError in IR hashCode() methods compiling KMM module for Kotlin/Native with Kotlin 1.5.0+

### Native. C Export

- [`KT-47209`](https://youtrack.jetbrains.com/issue/KT-47209) kotlin-native fails to generate valid C header if a setter takes anonymous parameter (_)

### Native. C and ObjC Import

- [`KT-48074`](https://youtrack.jetbrains.com/issue/KT-48074) Native: cinterop: __flexarr support

### Native. ObjC Export

- [`KT-47809`](https://youtrack.jetbrains.com/issue/KT-47809) Kotlin/Native: ObjC-export module name usage in klib compilation

### Native. Platforms

- [`KT-43024`](https://youtrack.jetbrains.com/issue/KT-43024) Kotlin/Native: Windows as cross-compilation target

### Native. Runtime

- [`KT-48452`](https://youtrack.jetbrains.com/issue/KT-48452) Kotlin/Native: Support thread state switching in termination handlers for the new MM

### Native. Runtime. Memory

- [`KT-48143`](https://youtrack.jetbrains.com/issue/KT-48143) Kotlin/Native: test fails with assert with new MM and state checker
- [`KT-48364`](https://youtrack.jetbrains.com/issue/KT-48364) Uninitialized top-level properties in new MM
- [`KT-44283`](https://youtrack.jetbrains.com/issue/KT-44283) staticCFunction with CValue parameter crashes when invoked off the main thread

### Native. Stdlib

- [`KT-47662`](https://youtrack.jetbrains.com/issue/KT-47662) [Native, All platforms] Incorrect parsing of long strings to Float and Double

### Reflection

- [`KT-47650`](https://youtrack.jetbrains.com/issue/KT-47650) KClass::nestedClasses throws ClassCastException for script classes with type aliases
- [`KT-45066`](https://youtrack.jetbrains.com/issue/KT-45066) Support flexible types (nullability, mutability, raw) in typeOf
- [`KT-35877`](https://youtrack.jetbrains.com/issue/KT-35877) typeOf<MutableList<*>> cannot be distinguished from typeOf<List<*>> in Kotlin/JVM

### Tools. CLI

- [`KT-47623`](https://youtrack.jetbrains.com/issue/KT-47623) Deprecate -Xuse-experimental
- [`KT-32376`](https://youtrack.jetbrains.com/issue/KT-32376) “no main manifest attribute” on running the jar for cli-compiled Kotlin objects with main function
- [`KT-48026`](https://youtrack.jetbrains.com/issue/KT-48026) Add the compiler X-flag to enable self upper bound type inference
- [`KT-47640`](https://youtrack.jetbrains.com/issue/KT-47640) CLI: support -option=value format as for -Xoption=value
- [`KT-47099`](https://youtrack.jetbrains.com/issue/KT-47099) Add a stable compiler argument for opt-in requirements as soon as they are stable
- [`KT-30778`](https://youtrack.jetbrains.com/issue/KT-30778) kotlin-compiler.jar contains shaded but not relocated kotlinx.coroutines

### Tools. Commonizer

#### New Features

- [`KT-47433`](https://youtrack.jetbrains.com/issue/KT-47433) [Commonizer] Commonize functions/properties with TA/Class types in signature
- [`KT-47691`](https://youtrack.jetbrains.com/issue/KT-47691) [Commonizer] Commonize `var` and `val`  properties
- [`KT-47434`](https://youtrack.jetbrains.com/issue/KT-47434) [Commonizer] Commonize parameterized (type-alias + class) types
- [`KT-47432`](https://youtrack.jetbrains.com/issue/KT-47432) [Commonizer] Commonize (type-alias + class) types used in functions

#### Fixes

- [`KT-47523`](https://youtrack.jetbrains.com/issue/KT-47523) MPP: Unable to resolve c-interop dependency if platform is included in an intermediate source set with the only target
- [`KT-48278`](https://youtrack.jetbrains.com/issue/KT-48278) [Commonizer] platform.posix.usleep not commonized in sqliter
- [`KT-46691`](https://youtrack.jetbrains.com/issue/KT-46691) MPP: Type mismatch for hierarchically commonized typealiases
- [`KT-47221`](https://youtrack.jetbrains.com/issue/KT-47221) C-interop commonization fails if few targets reuse same source set
- [`KT-47775`](https://youtrack.jetbrains.com/issue/KT-47775) Commonizer don't run for shared native code if test source set depends on main
- [`KT-47053`](https://youtrack.jetbrains.com/issue/KT-47053) MPP: Unable to resolve c-interop commonized code from shared test source set
- [`KT-48118`](https://youtrack.jetbrains.com/issue/KT-48118) Commonized c-interop lib is not attached to common main source set
- [`KT-47641`](https://youtrack.jetbrains.com/issue/KT-47641) Enabled cInterop commonization triggers native compilation during Gradle sync in IDE
- [`KT-47056`](https://youtrack.jetbrains.com/issue/KT-47056) MPP: Change naming for folder with commonized c-interop libraries

### Tools. Compiler Plugins

- [`KT-40340`](https://youtrack.jetbrains.com/issue/KT-40340) jvm-abi-gen plugin: failure with Android D8 (Dexer) tool
- [`KT-40133`](https://youtrack.jetbrains.com/issue/KT-40133) jvm-abi-gen plugin: fails for inline function containing apply block with anonymous object
- [`KT-28704`](https://youtrack.jetbrains.com/issue/KT-28704) jvm-abi-gen plugin: avoid calling codegen twice per module
- [`KT-48111`](https://youtrack.jetbrains.com/issue/KT-48111) JVM / IR: "IllegalAccessError: tried to access method" with NoArg plugin and sealed class

### Tools. Gradle

#### Fixes

- [`KT-45202`](https://youtrack.jetbrains.com/issue/KT-45202) Kapt crashes with java.io.UTFDataFormatException
- [`KT-46719`](https://youtrack.jetbrains.com/issue/KT-46719) Remove 'kotlin.useFallbackCompilerSearch' build option
- [`KT-27687`](https://youtrack.jetbrains.com/issue/KT-27687) Empty directories in source set causes gradle cache miss for KotlinCompile task
- [`KT-48226`](https://youtrack.jetbrains.com/issue/KT-48226) Kotlin toolchain does not set 'jvmTarget' for Kotlin tasks on configuration cache reuse
- [`KT-47792`](https://youtrack.jetbrains.com/issue/KT-47792) KGP should ignore ProjectDependency when customize kotlin Dependencies
- [`KT-47867`](https://youtrack.jetbrains.com/issue/KT-47867) Replace usages of IncrementalTaskInputs with InputChanges
- [`KT-47940`](https://youtrack.jetbrains.com/issue/KT-47940) Kotlin JVM toolchain breaks configuration cache
- [`KT-47520`](https://youtrack.jetbrains.com/issue/KT-47520) Kotlin and Java target compatibility check produces false positive on using Gradle toolchains
- [`KT-46978`](https://youtrack.jetbrains.com/issue/KT-46978) Duplicate resource errors on gradle 7 with multi-module multiplatform project with withJava
- [`KT-47635`](https://youtrack.jetbrains.com/issue/KT-47635) Kotlin version conflict on using 'noarg' Gradle plugin
- [`KT-47636`](https://youtrack.jetbrains.com/issue/KT-47636) Kotlin version conflict on using 'sam-with-receiver' Gradle plugin
- [`KT-47354`](https://youtrack.jetbrains.com/issue/KT-47354) Kotlin version conflict on using 'allopen' Gradle plugin
- [`KT-46972`](https://youtrack.jetbrains.com/issue/KT-46972) Migrate Kotlin repo to use Gradle toolchain feature

### Tools. Gradle. JS

- [`KT-48332`](https://youtrack.jetbrains.com/issue/KT-48332) Make NodeJsSetupTask and YarnSetupTask not cacheable
- [`KT-48241`](https://youtrack.jetbrains.com/issue/KT-48241) KJS / Gradle: NPM test dependency may break Gradle configuration cache
- [`KT-32071`](https://youtrack.jetbrains.com/issue/KT-32071) Possibility to disable downloading of Node.js and Yarn
- [`KT-37895`](https://youtrack.jetbrains.com/issue/KT-37895) KJS: NPM Post-install Scripts sometimes print "node: not found"
- [`KT-34985`](https://youtrack.jetbrains.com/issue/KT-34985) kotlin-gradle-plugin: Should align ways NodeJs and Yarn are downloaded

### Tools. Gradle. Multiplatform

- [`KT-46343`](https://youtrack.jetbrains.com/issue/KT-46343) [Commonizer] Use lockfile for NativeDistributionCommonizationCache
- [`KT-48427`](https://youtrack.jetbrains.com/issue/KT-48427) Execution failed for task ':commonizeNativeDistribution'. > java.io.FileNotFoundException lock (No such file or directory)
- [`KT-48513`](https://youtrack.jetbrains.com/issue/KT-48513) Commonized platform libraries are unresolved in modules for new hierarchical MPP flag
- [`KT-48138`](https://youtrack.jetbrains.com/issue/KT-48138) CInteropCommonizer: Missing commonization request if test source set has different targets than associated main
- [`KT-35832`](https://youtrack.jetbrains.com/issue/KT-35832) Gradle: MPP plugin operates with -Xuse-experimental and not with -Xopt-in
- [`KT-38111`](https://youtrack.jetbrains.com/issue/KT-38111) Gradle DSL: rename useExperimentalAnnotation function
- [`KT-47612`](https://youtrack.jetbrains.com/issue/KT-47612) Task :buildKotlinToolingMetadata is incompatible with Gradle configuration cache
- [`KT-47611`](https://youtrack.jetbrains.com/issue/KT-47611) Task :generateMetadataFileForKotlinMultiplatformPublication is incompatible with Gradle configuration cache

### Tools. Gradle. Native

- [`KT-47362`](https://youtrack.jetbrains.com/issue/KT-47362) Cocoapods plugin: add error reporting for case when pod is not installed on user machine
- [`KT-37513`](https://youtrack.jetbrains.com/issue/KT-37513) CocoaPods Gradle plugin: Support building tests from terminal for projects depending on pods
- [`KT-47078`](https://youtrack.jetbrains.com/issue/KT-47078) Support Apple Silicon in cocoapods gradle plugin

### Tools. JPS

- [`KT-46804`](https://youtrack.jetbrains.com/issue/KT-46804) Slow Kotlin incremental build: LookupStorage operations

### Tools. Scripts

- [`KT-48025`](https://youtrack.jetbrains.com/issue/KT-48025) JVM / IR / Script: IllegalStateException: No mapping for symbol: VALUE_PARAMETER INSTANCE_RECEIVER caused by method tnat returns outer function
- [`KT-47927`](https://youtrack.jetbrains.com/issue/KT-47927) Script: memory leak with new engines
- [`KT-48303`](https://youtrack.jetbrains.com/issue/KT-48303) main.kts script fails to detect vanished dependencies if run from the cache
- [`KT-48177`](https://youtrack.jetbrains.com/issue/KT-48177) Scripts: OutOfMemoryException with circular @file:Import
- [`KT-46645`](https://youtrack.jetbrains.com/issue/KT-46645) Scripts: "IllegalStateException: No mapping for symbol: VALUE_PARAMETER INSTANCE_RECEIVER" caused by get accessor
- [`KT-43917`](https://youtrack.jetbrains.com/issue/KT-43917) Gradle dependency conflict with resolutionStrategy failOnVersionConflict and kotlin 1.4

### Tools. kapt

- [`KT-47853`](https://youtrack.jetbrains.com/issue/KT-47853) `KaptWithoutKotlincTask` eagerly resolves dependencies during construction/configuration and can cause deadlocks
- [`KT-48195`](https://youtrack.jetbrains.com/issue/KT-48195) Kapt causes dead lock in DefaultFileLockManager
- [`KT-47347`](https://youtrack.jetbrains.com/issue/KT-47347) KAPT: Stub generation in Gradle cache is not consistently relocatable

## Recent ChangeLogs:
### [ChangeLog-1.5.X](docs/changelogs/ChangeLog-1.5.X.md)
### [ChangeLog-1.4.X](docs/changelogs/ChangeLog-1.4.X.md)
### [ChangeLog-1.3.X](docs/changelogs/ChangeLog-1.3.X.md)
### [ChangeLog-1.2.X](docs/changelogs/ChangeLog-1.2.X.md)
### [ChangeLog-1.1.X](docs/changelogs/ChangeLog-1.1.X.md)
### [ChangeLog-1.0.X](docs/changelogs/ChangeLog-1.0.X.md)