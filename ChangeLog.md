# CHANGELOG

## 1.3.10

### Compiler

- [`KT-27758`](https://youtrack.jetbrains.com/issue/KT-27758) Kotlin 1.3 breaks compilation of calling of function named 'contract' with block as a last parameter
- [`KT-27895`](https://youtrack.jetbrains.com/issue/KT-27895) Kotlin 1.3.0 broken runtime annotation issue

### IDE

- [`KT-27230`](https://youtrack.jetbrains.com/issue/KT-27230) Freeze on paste
- [`KT-27907`](https://youtrack.jetbrains.com/issue/KT-27907) Exception on processing auto-generated classes from AS

### IDE. Debugger

- [`KT-27540`](https://youtrack.jetbrains.com/issue/KT-27540) 2018.3 and 2019.1 Debugger: Evaluating anything fails with KNPE in LabelNormalizationMethodTransformer
- [`KT-27833`](https://youtrack.jetbrains.com/issue/KT-27833) Evaluate exception in 183/191 with `asm-7.0-beta1`/'asm-7.0'
- [`KT-27965`](https://youtrack.jetbrains.com/issue/KT-27965) Sequence debugger does not work in Android Studio
- [`KT-27980`](https://youtrack.jetbrains.com/issue/KT-27980) Kotlin sequence debugger throws IDE exception in IDEA 183

### IDE. Gradle

- [`KT-27265`](https://youtrack.jetbrains.com/issue/KT-27265) Unresolved reference in IDE on calling JVM source set members of a multiplatform project with Android target from a plain Kotlin/JVM module
- [`KT-27849`](https://youtrack.jetbrains.com/issue/KT-27849) IntelliJ: Wrong scope of JVM platform MPP dependency

### IDE. Inspections and Intentions

- [`KT-26481`](https://youtrack.jetbrains.com/issue/KT-26481) Flaky false positive "Receiver parameter is never used" for local extension function
- [`KT-27357`](https://youtrack.jetbrains.com/issue/KT-27357) Function with inline class type value parameters is marked as unused by IDE
- [`KT-27434`](https://youtrack.jetbrains.com/issue/KT-27434) False positive "Unused symbol" inspection for functions and secondary constructors of inline classes
- [`KT-27945`](https://youtrack.jetbrains.com/issue/KT-27945) Quick-fix whitespace bug in KtPrimaryConstructor.addAnnotationEntry()

### IDE. Scratch

- [`KT-27746`](https://youtrack.jetbrains.com/issue/KT-27746) Scratch: "Cannot pop operand off an empty stack" in a new scratch file

### IDE. Tests Support

- [`KT-27371`](https://youtrack.jetbrains.com/issue/KT-27371) Common tests can not be launched from gutter in MPP Android/iOS project

### Reflection

- [`KT-27878`](https://youtrack.jetbrains.com/issue/KT-27878) Spring: "AssertionError: Non-primitive type name passed: void"

### Tools. Gradle

- [`KT-27160`](https://youtrack.jetbrains.com/issue/KT-27160) Kotlin Gradle plugin 1.3 resolves script configurations during project evaluation
- [`KT-27803`](https://youtrack.jetbrains.com/issue/KT-27803) CInterop input configuration has 'java-api' as a Usage attribute value in new MPP
- [`KT-27984`](https://youtrack.jetbrains.com/issue/KT-27984) Kotlin Gradle Plugin: Circular dependency

### Tools. JPS

- [`KT-26489`](https://youtrack.jetbrains.com/issue/KT-26489) JPS: support -Xcommon-sources for multiplatform projects (JVM)
- [`KT-27037`](https://youtrack.jetbrains.com/issue/KT-27037) Incremental compilation failed after update to 1.3.0-rc-60
- [`KT-27792`](https://youtrack.jetbrains.com/issue/KT-27792) Incremental compilation failed with NullPointerException in KotlinCompileContext.markChunkForRebuildBeforeBuild

### Tools. kapt

- [`KT-27126`](https://youtrack.jetbrains.com/issue/KT-27126) kapt: class implementing List<T> generates bad stub


## 1.3.0

### IDE

- [`KT-25429`](https://youtrack.jetbrains.com/issue/KT-25429) Replace update channel in IDE plugin
- [`KT-27793`](https://youtrack.jetbrains.com/issue/KT-27793) kotlinx.android.synthetic is unresolved on project reopening

### IDE. Inspections and Intentions

- [`KT-27619`](https://youtrack.jetbrains.com/issue/KT-27619) Inspection "Invalid property key" should check whether reference is soft or not

## 1.3-RC4

### Compiler

#### Fixes

- [`KT-26858`](https://youtrack.jetbrains.com/issue/KT-26858) Inline class access to private companion object value fails with NSME
- [`KT-27030`](https://youtrack.jetbrains.com/issue/KT-27030) Non-capturing lambda in inline class members fails with internal error (NPE in genClosure)
- [`KT-27031`](https://youtrack.jetbrains.com/issue/KT-27031) Inline extension lambda in inline class fun fails with internal error (wrong bytecode generated)
- [`KT-27033`](https://youtrack.jetbrains.com/issue/KT-27033) Anonymous object in inline class fun fails with internal error (NPE in generateObjectLiteral/.../writeOuterClassAndEnclosingMethod)
- [`KT-27096`](https://youtrack.jetbrains.com/issue/KT-27096) AnalyzerException: Error at instruction 71: Expected I, but found . when function takes unsigned type with default value and returns nullable inline class
- [`KT-27130`](https://youtrack.jetbrains.com/issue/KT-27130) Suspension point is inside a critical section regression
- [`KT-27132`](https://youtrack.jetbrains.com/issue/KT-27132) CCE when inline class is boxed
- [`KT-27258`](https://youtrack.jetbrains.com/issue/KT-27258) Report diagnostic for suspension point inside critical section for crossinline suspend lambdas
- [`KT-27393`](https://youtrack.jetbrains.com/issue/KT-27393) Incorrect inline class type coercion in '==' with generic call
- [`KT-27484`](https://youtrack.jetbrains.com/issue/KT-27484) Suspension points in synchronized blocks checker crashes
- [`KT-27502`](https://youtrack.jetbrains.com/issue/KT-27502) Boxed inline class backed by Any is not unboxed before method invocation
- [`KT-27526`](https://youtrack.jetbrains.com/issue/KT-27526) Functional type with inline class argument and suspend modified expects unboxed value while it is boxed
- [`KT-27615`](https://youtrack.jetbrains.com/issue/KT-27615) Double wrap when inline class is printing if it was obtained from list/map
- [`KT-27620`](https://youtrack.jetbrains.com/issue/KT-27620) Report error when using value of kotlin.Result type as an extension receiver with safe call

### IDE

- [`KT-27298`](https://youtrack.jetbrains.com/issue/KT-27298) Deadlock on project open
- [`KT-27329`](https://youtrack.jetbrains.com/issue/KT-27329) Migration doesn't work for kts projects when versions are stored in kt files inside buildSrc directory
- [`KT-27355`](https://youtrack.jetbrains.com/issue/KT-27355) Assertion error from light classes (expected callable member was null) for type alias in JvmMultifileClass annotated file
- [`KT-27456`](https://youtrack.jetbrains.com/issue/KT-27456) New Project wizard: Kotlin (Multiplatform Library): consider generating source files with different names to work around KT-21186
- [`KT-27473`](https://youtrack.jetbrains.com/issue/KT-27473) "Gradle sync failed: Already disposed: Module: 'moduleName-app_commonMain'" on reimport of a multiplatform project with Android target between different IDEs
- [`KT-27485`](https://youtrack.jetbrains.com/issue/KT-27485) Gradle import failed with "Already disposed" error on reopening of a multiplatform project with Android target
- [`KT-27572`](https://youtrack.jetbrains.com/issue/KT-27572) ISE: "Could not generate LightClass for entry declared in <null>" at CompilationErrorHandler.lambda$static$0()

### IDE. Android

- [`KT-26975`](https://youtrack.jetbrains.com/issue/KT-26975) CNFDE KotlinAndroidGradleOrderEnumerationHandler$FactoryImpl in AS 3.3 with Kotlin 1.3.0-rc-51
- [`KT-27451`](https://youtrack.jetbrains.com/issue/KT-27451) `main` target platform selection is not working in a multiplatform project with Android and JVM targets in Android Studio

### IDE. Gradle

- [`KT-27365`](https://youtrack.jetbrains.com/issue/KT-27365) Dependencies between Java project and MPP one are not respected by import
- [`KT-27643`](https://youtrack.jetbrains.com/issue/KT-27643) First import of Android project miss skips some dependencies in IDEA 183

### IDE. Multiplatform

- [`KT-27356`](https://youtrack.jetbrains.com/issue/KT-27356) Use `kotlin-stdlib` instead of `kotlin-stdlib-jdk8` in Android-related MPP templates

### IDE. Scratch

- [`KT-24180`](https://youtrack.jetbrains.com/issue/KT-24180) Add key shortcut and action for running a kotlin scratch file (green arrow button in the editor tool-buttons)

### JavaScript

- [`KT-26320`](https://youtrack.jetbrains.com/issue/KT-26320) JS: forEach + firstOrNull + when combination does not compile correctly
- [`KT-26787`](https://youtrack.jetbrains.com/issue/KT-26787) Incorrect JS code translation: `when` statement inside `for` loop breaks out of the loop

### Libraries

- [`KT-27508`](https://youtrack.jetbrains.com/issue/KT-27508) Rename Random companion object to Default

### Tools. Gradle

- [`KT-26758`](https://youtrack.jetbrains.com/issue/KT-26758) Unify Gradle DSL for compiler flags in new multiplatform model
- [`KT-26840`](https://youtrack.jetbrains.com/issue/KT-26840) Support -Xuse-experimental in the new MPP language settings DSL
- [`KT-27278`](https://youtrack.jetbrains.com/issue/KT-27278) New MPP plugin is binary-incompatible with Gradle 5.0
- [`KT-27499`](https://youtrack.jetbrains.com/issue/KT-27499) In new MPP, support compiler plugins (subplugins) options import into the IDE for each source set

### Tools. JPS

- [`KT-27044`](https://youtrack.jetbrains.com/issue/KT-27044) JPS rebuilds twice when dependency is updated

### Tools. kapt

- [`KT-27119`](https://youtrack.jetbrains.com/issue/KT-27119) kapt: val without explicit type that is assigned an object expression implementing a generic interface breaks compilation


## 1.3-RC3

### Compiler

- [`KT-26300`](https://youtrack.jetbrains.com/issue/KT-26300) Smartcasts don't work if pass same fields of instances of the same class in contract function with conjunction not-null condition
- [`KT-27221`](https://youtrack.jetbrains.com/issue/KT-27221) Incorrect smart cast for sealed classes with a multilevel hierarchy

### IDE

- [`KT-27163`](https://youtrack.jetbrains.com/issue/KT-27163) Replace coroutine migration dialog with notification
- [`KT-27200`](https://youtrack.jetbrains.com/issue/KT-27200) New MPP wizard: mobile library
- [`KT-27201`](https://youtrack.jetbrains.com/issue/KT-27201) MPP library wizards: provide maven publishing
- [`KT-27214`](https://youtrack.jetbrains.com/issue/KT-27214) Android test source directories are not recognised in IDE
- [`KT-27351`](https://youtrack.jetbrains.com/issue/KT-27351) Better fix for coroutines outdated versions in Gradle and Maven

### IDE. Android

- [`KT-27331`](https://youtrack.jetbrains.com/issue/KT-27331) Missing dependencies in Android project depending on MPP project

### IDE. Inspections and Intentions

- [`KT-27164`](https://youtrack.jetbrains.com/issue/KT-27164) Create a quick fix for replacing obsolete coroutines in the whole project

### IDE. Multiplatform

- [`KT-27029`](https://youtrack.jetbrains.com/issue/KT-27029) Multiplatform project is unloaded if Gradle refresh/reimport is failed

### Libraries

- [`KT-22869`](https://youtrack.jetbrains.com/issue/KT-22869) Improve docs of assertFailsWith function

### Tools. CLI

- [`KT-27218`](https://youtrack.jetbrains.com/issue/KT-27218) From @<argfile> not all whitespace characters are parsed correctly

### Tools. Compiler Plugins

- [`KT-27166`](https://youtrack.jetbrains.com/issue/KT-27166) Disable kotlinx.serialization plugin in IDE by default

## 1.3-RC2

### Android

- [`KT-27006`](https://youtrack.jetbrains.com/issue/KT-27006) Android extensions are not recognised by IDE in multiplatform projects
- [`KT-27008`](https://youtrack.jetbrains.com/issue/KT-27008) Compiler plugins are not working in multiplatform projects with Android target

### Compiler

- [`KT-24415`](https://youtrack.jetbrains.com/issue/KT-24415) Remove bridge flag from default methods
- [`KT-24510`](https://youtrack.jetbrains.com/issue/KT-24510) Coroutines make Android's D8 angry
- [`KT-25545`](https://youtrack.jetbrains.com/issue/KT-25545) Import statement of `@Experimental` element causes compiler warning/error, but annotation can't be used to avoid it
- [`KT-26382`](https://youtrack.jetbrains.com/issue/KT-26382) Wrong smartcast if used safe call + returnsNull effect
- [`KT-26640`](https://youtrack.jetbrains.com/issue/KT-26640) Check inference behaviour for coroutines that it's possible to improve it in compatible way
- [`KT-26804`](https://youtrack.jetbrains.com/issue/KT-26804) Make sure @PublishedAPI is retained in binary representation of a primary constructor of an inline class
- [`KT-27079`](https://youtrack.jetbrains.com/issue/KT-27079) Allow using extensions without opt-in in builder-inference if they add only trivial constraints
- [`KT-27084`](https://youtrack.jetbrains.com/issue/KT-27084) smart cast to non-nullable regression from 1.2.70 to 1.3.0-rc-57
- [`KT-27117`](https://youtrack.jetbrains.com/issue/KT-27117) IllegalAccessError when using private Companion field inside inline lambda
- [`KT-27121`](https://youtrack.jetbrains.com/issue/KT-27121) Illegal field modifiers in class for a field of an interface companion
- [`KT-27161`](https://youtrack.jetbrains.com/issue/KT-27161) Getting "Backend Internal error: Descriptor can be left only if it is last" using new when syntax

### IDE

#### New Features

- [`KT-26313`](https://youtrack.jetbrains.com/issue/KT-26313) Support ResolveScopeEnlarger in Kotlin IDE
- [`KT-26786`](https://youtrack.jetbrains.com/issue/KT-26786) MPP builders: create not only build.gradle but some example files also

#### Fixes

- [`KT-13948`](https://youtrack.jetbrains.com/issue/KT-13948) IDE plugins: improve description
- [`KT-14981`](https://youtrack.jetbrains.com/issue/KT-14981) IDE should accept only its variant of plugin, as possible
- [`KT-23864`](https://youtrack.jetbrains.com/issue/KT-23864) Copyright message is duplicated in kotlin file in root package after updating copyright
- [`KT-24907`](https://youtrack.jetbrains.com/issue/KT-24907) please remove usages of com.intellij.openapi.vfs.StandardFileSystems#getJarRootForLocalFile deprecated long ago
- [`KT-25449`](https://youtrack.jetbrains.com/issue/KT-25449) Mark classes loaded by custom class loader with @DynamicallyLoaded annotation for the sake of better static analysis
- [`KT-25463`](https://youtrack.jetbrains.com/issue/KT-25463) API version in Kotlin facets isn't automatically set to 1.3 when importing a project in Gradle
- [`KT-25952`](https://youtrack.jetbrains.com/issue/KT-25952) New Project Wizard: generate MPP in a new way
- [`KT-26501`](https://youtrack.jetbrains.com/issue/KT-26501) Fix "IDEA internal actions" group text to "Kotlin internal actions"
- [`KT-26695`](https://youtrack.jetbrains.com/issue/KT-26695) IDEA takes 1.3-M2-release plugin as more recent than any 1.3.0-dev-nnn or 1.3.0-rc-nnn plugin
- [`KT-26763`](https://youtrack.jetbrains.com/issue/KT-26763) Compiler options are not imported into Kotlin facet for a Native module
- [`KT-26774`](https://youtrack.jetbrains.com/issue/KT-26774) Create IDE setting for experimental inline classes
- [`KT-26889`](https://youtrack.jetbrains.com/issue/KT-26889) Don't show migration dialog if no actual migrations are available
- [`KT-26933`](https://youtrack.jetbrains.com/issue/KT-26933) No jre -> jdk fix in Gradle file if version isn't written explicitly
- [`KT-26937`](https://youtrack.jetbrains.com/issue/KT-26937) MPP: Gradle import: adding `target` definition after importing its `sourceSet` does not correct the module SDK
- [`KT-26953`](https://youtrack.jetbrains.com/issue/KT-26953) New MPP project wrong formatting
- [`KT-27021`](https://youtrack.jetbrains.com/issue/KT-27021) Wrong JVM target if no Kotlin facet is specified
- [`KT-27100`](https://youtrack.jetbrains.com/issue/KT-27100) Version migration dialog is not shown in Studio 3.3
- [`KT-27145`](https://youtrack.jetbrains.com/issue/KT-27145) Gradle import: JVM modules gets no JDK in dependencies
- [`KT-27177`](https://youtrack.jetbrains.com/issue/KT-27177) MPP wizards: use Gradle 4.7 only
- [`KT-27193`](https://youtrack.jetbrains.com/issue/KT-27193) Gradle import: with Kotlin configured Android module gets non-Android JDK

### IDE. Code Style, Formatting

- [`KT-27027`](https://youtrack.jetbrains.com/issue/KT-27027) Formatter puts when subject variable on a new line

### IDE. Completion

- [`KT-25313`](https://youtrack.jetbrains.com/issue/KT-25313) Autocomplete generates incorrect code on fields overriding by `expected` class

### IDE. Hints

- [`KT-26057`](https://youtrack.jetbrains.com/issue/KT-26057) (arguably) redundant hint shown for enum value when qualified with enum class

### IDE. Inspections and Intentions

- [`KT-14929`](https://youtrack.jetbrains.com/issue/KT-14929) Deprecated ReplaceWith for type aliases
- [`KT-25251`](https://youtrack.jetbrains.com/issue/KT-25251) Create intention for migration coroutines from experimental to released state
- [`KT-26027`](https://youtrack.jetbrains.com/issue/KT-26027) False positive from "Nested lambda has shadowed implicit parameter" inspection for SAM conversion
- [`KT-26268`](https://youtrack.jetbrains.com/issue/KT-26268) Inspection "Nested lambda has shadowed implicit parameter" should only warn if parameter is used
- [`KT-26775`](https://youtrack.jetbrains.com/issue/KT-26775) Create quick fix that enable or disable experimental inline classes in project
- [`KT-26991`](https://youtrack.jetbrains.com/issue/KT-26991) ReplaceWith for object doesn't work anymore

### IDE. Multiplatform

- [`KT-24060`](https://youtrack.jetbrains.com/issue/KT-24060) `main` function in common part of MPP project: allow user to choose between platform modules to run it from
- [`KT-26647`](https://youtrack.jetbrains.com/issue/KT-26647) Warn user about incompatible/ignored Native targets on Gradle build of a project with the new multiplatform model
- [`KT-26690`](https://youtrack.jetbrains.com/issue/KT-26690) IDE significantly slows down having Native target in a multiplatform project
- [`KT-26872`](https://youtrack.jetbrains.com/issue/KT-26872) MPP: JS: Node.js run configuration is created with not existing JavaScript file
- [`KT-26942`](https://youtrack.jetbrains.com/issue/KT-26942) MPP IDE: JS test configuration removes gutter actions from common module
- [`KT-27010`](https://youtrack.jetbrains.com/issue/KT-27010) New mpp: missing run gutters in common code when relevant platform roots do not exist
- [`KT-27133`](https://youtrack.jetbrains.com/issue/KT-27133) IDE requires `actual` implementations to be also present in test source sets
- [`KT-27172`](https://youtrack.jetbrains.com/issue/KT-27172) ISE: "The provided plugin org.jetbrains.kotlin.android.synthetic.AndroidComponentRegistrar is not compatible with this version of compiler" on build of a multiplatform project with iOS and Android

### IDE. Navigation

- [`KT-25055`](https://youtrack.jetbrains.com/issue/KT-25055) Android modules are named same as JVM ones in `actual` gutter tooltip
- [`KT-26004`](https://youtrack.jetbrains.com/issue/KT-26004) IDE: Unable to navigate to common library declaration from platform code (not necessarily in an MPP project)

### IDE. Tests Support

- [`KT-23884`](https://youtrack.jetbrains.com/issue/KT-23884) Running common module test in IDE results in "no JDK specified" error
- [`KT-23911`](https://youtrack.jetbrains.com/issue/KT-23911) Cannot jump to source from common test function in Run tool window

### Libraries

- [`KT-18608`](https://youtrack.jetbrains.com/issue/KT-18608) Result type for Kotlin (aka Try monad)
- [`KT-26666`](https://youtrack.jetbrains.com/issue/KT-26666) Add documentation for contract DSL

### Reflection

- [`KT-24170`](https://youtrack.jetbrains.com/issue/KT-24170) Instance parameter of inherited declaration should have the type of subclass, not the base class

### Tools. Compiler Plugins

- [`KT-24444`](https://youtrack.jetbrains.com/issue/KT-24444) Do not store proxy objects from Gradle importer in the project model

### Tools. Gradle

- [`KT-25200`](https://youtrack.jetbrains.com/issue/KT-25200) Report a warning when building multiplatform code in Gradle
- [`KT-26390`](https://youtrack.jetbrains.com/issue/KT-26390) Implement source JARs building and publishing in new MPP
- [`KT-26771`](https://youtrack.jetbrains.com/issue/KT-26771) New Native MPP Gradle plugin creates publications only for host system
- [`KT-26834`](https://youtrack.jetbrains.com/issue/KT-26834) Gradle compilation of multimodule project fails with Could not resolve all files for configuration ':example-v8:apiDependenciesMetadata'
- [`KT-27111`](https://youtrack.jetbrains.com/issue/KT-27111) `org.jetbrains.kotlin.platform.type` is not set for some Gradle configurations in multiplatform plugin
- [`KT-27196`](https://youtrack.jetbrains.com/issue/KT-27196) Support Kotlin/JS DCE in new MPP

### Tools. Scripts

- [`KT-26828`](https://youtrack.jetbrains.com/issue/KT-26828) main-kts test fails with "Error processing script definition class"
- [`KT-27015`](https://youtrack.jetbrains.com/issue/KT-27015) Scripting sample from 1.3 RC blogpost does not work
- [`KT-27050`](https://youtrack.jetbrains.com/issue/KT-27050) 1.3-RC Scripting @file:Repository and @file:DependsOn annotations are not repeatable

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

## Previous releases

This release also includes the fixes and improvements from the previous releases: [1.3-M1](https://github.com/JetBrains/kotlin/blob/1.3-M1/ChangeLog.md) and [1.3-M2](https://github.com/JetBrains/kotlin/blob/1.3-M2/ChangeLog.md)
