# CHANGELOG

## 1.5.0-M2

### Compiler

#### New Features

- [`KT-30222`](https://youtrack.jetbrains.com/issue/KT-30222) Support JVM target version selection in Kotlin bytecode tool window
- [`KT-41884`](https://youtrack.jetbrains.com/issue/KT-41884) Support 'file' target for JvmSynthetic annotation
- [`KT-44278`](https://youtrack.jetbrains.com/issue/KT-44278) Generate SAM-converted lambdas and function references using 'invokedynamic' on JDK 1.8+

#### Performance Improvements

- [`KT-26060`](https://youtrack.jetbrains.com/issue/KT-26060) Support a compiler mode to compile lambda expressions using `invokedynamic` instruction
- [`KT-42621`](https://youtrack.jetbrains.com/issue/KT-42621) Kotlin binary size considerably larger for code extensively using stream API

#### Fixes

- [`KT-20306`](https://youtrack.jetbrains.com/issue/KT-20306) Make 'when' over an 'expect' enum class non-exhaustive
- [`KT-20869`](https://youtrack.jetbrains.com/issue/KT-20869) kotlin.jvm.internal.DefaultConstructorMarker should be public
- [`KT-26592`](https://youtrack.jetbrains.com/issue/KT-26592) Do not generate private suspend functions as synthetic package-private
- [`KT-38100`](https://youtrack.jetbrains.com/issue/KT-38100) Support local delegated properties (not inlined) in new JVM default modes
- [`KT-40392`](https://youtrack.jetbrains.com/issue/KT-40392) Deprecate JvmDefault annotation and old -Xjvm-default modes
- [`KT-41758`](https://youtrack.jetbrains.com/issue/KT-41758) Deprecate kotlin.Metadata.bytecodeVersion and avoid using it in the compiler
- [`KT-42092`](https://youtrack.jetbrains.com/issue/KT-42092) JVM / IR: "AnalyzerException: Argument 1: expected R, but found J" when trying to add to ArrayList the result of a function applied to int
- [`KT-42321`](https://youtrack.jetbrains.com/issue/KT-42321) JVM IR: do not cast integer value based on the type of a literal receiver of an operator call
- [`KT-42404`](https://youtrack.jetbrains.com/issue/KT-42404) "Supertypes of the following classes cannot be resolved" in Rider project
- [`KT-42472`](https://youtrack.jetbrains.com/issue/KT-42472) No TYPE_INFERENCE_UPPER_BOUND_VIOLATED for Delegated Properties do not check types (in Kotlin 1.4.10)
- [`KT-43167`](https://youtrack.jetbrains.com/issue/KT-43167) JVM IR, serialization: "No mapping for symbol: VALUE_PARAMETER INSTANCE_RECEIVER" with data class containing property defined in body
- [`KT-43303`](https://youtrack.jetbrains.com/issue/KT-43303) NI: False negative TYPE_INFERENCE_UPPER_BOUND_VIOLATED when inferred type argument is not a subtype of type parameter upper bound
- [`KT-44368`](https://youtrack.jetbrains.com/issue/KT-44368) "IllegalStateException: Error type encountered" when inlining 'invoke' operator without enough information on type variable
- [`KT-44412`](https://youtrack.jetbrains.com/issue/KT-44412) JVM IR backend fails to compile break in condition of do while
- [`KT-44471`](https://youtrack.jetbrains.com/issue/KT-44471) Fix failing script tests after switching to 1.5
- [`KT-44474`](https://youtrack.jetbrains.com/issue/KT-44474) Compiler expects sealed type inheritors from platform specific source-sets in when expression in common source-set
- [`KT-44483`](https://youtrack.jetbrains.com/issue/KT-44483) JVM IR: CCE on calling generic vararg function reference with Array expected type
- [`KT-44550`](https://youtrack.jetbrains.com/issue/KT-44550) KotlinBinaryClassCache leaks Kotlin plugin classloader on plugin unload
- [`KT-44546`](https://youtrack.jetbrains.com/issue/KT-44546) NI: changed variable fixation order (that can lead to changed resolution)
- [`KT-44583`](https://youtrack.jetbrains.com/issue/KT-44583) "Supertypes of the following classes cannot be resolved" error message gives no context
- [`KT-44627`](https://youtrack.jetbrains.com/issue/KT-44627) JVM IR: ACCIDENTAL_OVERRIDE when overriding a generic field where the type parameter has a primitive bound
- [`KT-44703`](https://youtrack.jetbrains.com/issue/KT-44703) JVM / IR: "IllegalStateException: Unhandled special name in mangledNameFor" caused by a reference to inline class inside interface's companion with lazy initialization
- [`KT-44714`](https://youtrack.jetbrains.com/issue/KT-44714) Debugger / Coroutines: Local variables are trimmed out too aggressively
- [`KT-44722`](https://youtrack.jetbrains.com/issue/KT-44722) JVM IR: ClassCastException with inline class, let and bound function reference
- [`KT-44726`](https://youtrack.jetbrains.com/issue/KT-44726) JVM IR: Incorrect KType nullability for platform type reified as non-null
- [`KT-44798`](https://youtrack.jetbrains.com/issue/KT-44798) JVM IR: Inherited platform declarations clash for class implementing both List and Set
- [`KT-44801`](https://youtrack.jetbrains.com/issue/KT-44801) 1.4.30 JVM IR: Unbound symbols not allowed with anonymous object
- [`KT-44803`](https://youtrack.jetbrains.com/issue/KT-44803) FIR bootstrap: incorrect nullability is set for type alias-based type
- [`KT-44827`](https://youtrack.jetbrains.com/issue/KT-44827) Non-existing outer class is written in anonymous class for SAM wrapper in inline lambda with capture
- [`KT-44878`](https://youtrack.jetbrains.com/issue/KT-44878) JVM_IR: "IllegalStateException: Unexpected types" when checking non-nullable variable is `in` range between nullable ones with smart-cast
- [`KT-44926`](https://youtrack.jetbrains.com/issue/KT-44926) MPP: Actual typealias to compiled inline class incompatible with expect inline class
- [`KT-44974`](https://youtrack.jetbrains.com/issue/KT-44974) LambdaConversionException in case of SAM-converted captuing extension lambda
- [`KT-45008`](https://youtrack.jetbrains.com/issue/KT-45008) JVM IR: hashCode is generated as invokeinterface if smart cast to interface is present
- [`KT-45011`](https://youtrack.jetbrains.com/issue/KT-45011) JVM / IR: "AssertionError: Unbound symbols not allowed"
- [`KT-45022`](https://youtrack.jetbrains.com/issue/KT-45022) IR: "AssertionError: Undefined variable referenced" from psi2ir caused by plusAssign operator of object
- [`KT-45064`](https://youtrack.jetbrains.com/issue/KT-45064) JVM IR: "java.lang.AssertionError: SyntheticAccessorLowering should not attempt to modify other files!" with member reference to property in another file with private setter
- [`KT-45069`](https://youtrack.jetbrains.com/issue/KT-45069) JVM / IR: New SAM conversions mode fails when converting from Unit to Any
- [`KT-45131`](https://youtrack.jetbrains.com/issue/KT-45131) JVM / IR: "RuntimeException: Lambda, SAM or anonymous object should have only one constructor" caused by inline class that type cast to reified type parameter inside lambda in inline function
- [`KT-45243`](https://youtrack.jetbrains.com/issue/KT-45243) "IllegalStateException: Lambdas shouldn't be visited by ESExpressionVisitor" caused by lambda inside `kotlin.test.assertNotNull`
- [`KT-45259`](https://youtrack.jetbrains.com/issue/KT-45259) JVM: ClassCastException caused by Result as lambda parameter type
- [`KT-45292`](https://youtrack.jetbrains.com/issue/KT-45292) AssertionError with recursive inline extension property
- [`KT-45300`](https://youtrack.jetbrains.com/issue/KT-45300) Deprecate super calls in public-api inline functions
- [`KT-45409`](https://youtrack.jetbrains.com/issue/KT-45409) Rename jspecify annotations’ package and default not null annotation

### IDE

- [`KT-44487`](https://youtrack.jetbrains.com/issue/KT-44487) MPP, IDE: No error in IDE when sealed class inheritor from common source-set is not used in exhaustive when expression in platform source-set
- [`KT-45254`](https://youtrack.jetbrains.com/issue/KT-45254) Highlighting for files with certain errors appears only on second opening

### IDE. Decompiler, Indexing, Stubs

- [`KT-43699`](https://youtrack.jetbrains.com/issue/KT-43699) IDE: Unresolved extension method from Java code for simple class with typealias and generics (IllegalStateException: Unknown type parameter)
- [`KT-44756`](https://youtrack.jetbrains.com/issue/KT-44756) Infinite "UpToDateStubIndexMismatch: PSI and index do not match." with IDEA 2021.1 EAP upon attempt to open "org.gradle.configurationcache" even they seem to be the same

### IDE. Gradle Integration

- [`KT-37127`](https://youtrack.jetbrains.com/issue/KT-37127) Implement precise importing of platforms of root source sets (commonMain/commonTest) when hierarchical multiplatform support is enabled

### IDE. Misc

- [`KT-44675`](https://youtrack.jetbrains.com/issue/KT-44675) Incorrect reference to resource into 202 plugin

### IDE. Refactorings

- [`KT-44839`](https://youtrack.jetbrains.com/issue/KT-44839) Sealed interfaces: move refactoring warnings works with "more freedom for sealed classes" rules for language level < 1.5

### IDE. Script

- [`KT-43288`](https://youtrack.jetbrains.com/issue/KT-43288) Allow push notifications about script configuration /dependencies changes via the `ScriptDefinitionsProvider` EP

### JavaScript

- [`KT-39272`](https://youtrack.jetbrains.com/issue/KT-39272) KJS / IR: Can't use javascript keywords as JsName
- [`KT-41650`](https://youtrack.jetbrains.com/issue/KT-41650) JS IR BE: `default` should be a reserved identifier
- [`KT-44433`](https://youtrack.jetbrains.com/issue/KT-44433) KJS IR: support function interfaces with suspend member
- [`KT-45059`](https://youtrack.jetbrains.com/issue/KT-45059) KJS / IR: Add possibility for runtime diagnostics of DCE result

### Libraries

- [`KT-12109`](https://youtrack.jetbrains.com/issue/KT-12109) Add stdlib method that combines mapNotNull() and first/firstOrNull()
- [`KT-26234`](https://youtrack.jetbrains.com/issue/KT-26234) Floored division and remainder function for numeric types
- [`KT-32996`](https://youtrack.jetbrains.com/issue/KT-32996) kotlin.test: add assertContentEquals for comparing content of arrays, iterables, sequences
- [`KT-42071`](https://youtrack.jetbrains.com/issue/KT-42071) Strict version of String.toBoolean()
- [`KT-42720`](https://youtrack.jetbrains.com/issue/KT-42720) Kotlin ArrayDeque on JVM: provide optimized toArray method
- [`KT-42840`](https://youtrack.jetbrains.com/issue/KT-42840) Commonize and generalize String.contentEquals that is currently JVM-only
- [`KT-44369`](https://youtrack.jetbrains.com/issue/KT-44369) Commonize Char.titlecaseChar() and Char.titlecase() that are currently JVM-only
- [`KT-44783`](https://youtrack.jetbrains.com/issue/KT-44783) Add IS_VALUE flag for value classes to kotlinx-metadata-jvm
- [`KT-44815`](https://youtrack.jetbrains.com/issue/KT-44815) Remove kotlin-annotations-android and JVM compiler support for @ParameterName/@DefaultValue/@DefaultNull
- [`KT-45213`](https://youtrack.jetbrains.com/issue/KT-45213) Update Unicode version used in K/N for Char and String case conversion functions

### Middle-end. IR

- [`KT-45170`](https://youtrack.jetbrains.com/issue/KT-45170) IR: "AssertionError: Single expression value for GET_OBJECT" caused by inc operator of field inside scope function inside object

### Native

- [`KT-44295`](https://youtrack.jetbrains.com/issue/KT-44295) 1.4.21 Kotlin native ndk compiler crash
- [`KT-44774`](https://youtrack.jetbrains.com/issue/KT-44774) ld fails with CALL16 reloc at 0x48f00 not against global symbol (Linux MIPS)

### Native. C and ObjC Import

- [`KT-44824`](https://youtrack.jetbrains.com/issue/KT-44824) cinterop tool no longer appends .klib to produced klibs

### Native. ObjC Export

- [`KT-44549`](https://youtrack.jetbrains.com/issue/KT-44549) In the Xcode debug session, call stack is missing a frame when the iOS app fails

### Native. Platforms

- [`KT-45094`](https://youtrack.jetbrains.com/issue/KT-45094) Fail to compile Kotlin Native sources under Oracle Linux 7

### Reflection

- [`KT-44594`](https://youtrack.jetbrains.com/issue/KT-44594) Avoid using unnecessary array types reflection in kotlin-reflect
- [`KT-44782`](https://youtrack.jetbrains.com/issue/KT-44782) Add KClass.isValue to kotlin-reflect

### Tools. CLI

- [`KT-45566`](https://youtrack.jetbrains.com/issue/KT-45566) JDK 16 - e: java.lang.NoClassDefFoundError: Could not initialize class org.jetbrains.kotlin.com.intellij.pom.java.LanguageLevel

### Tools. Gradle

- [`KT-44361`](https://youtrack.jetbrains.com/issue/KT-44361) Gradle: deprecate options includeRuntime, noStdlib, noReflect
- [`KT-44834`](https://youtrack.jetbrains.com/issue/KT-44834) Gradle Kotlin DSL: Add `languageSettings` configuration lambda without `apply` call

### Tools. Gradle. JS

- [`KT-45574`](https://youtrack.jetbrains.com/issue/KT-45574) Sync Kotlin/JS compile tasks into one folder (build/js/packages/<package>/kotlin)

### Tools. JPS

- [`KT-44644`](https://youtrack.jetbrains.com/issue/KT-44644) Mark all `@JvmMultifileClass` parts compiled in the previous round as dirty in the JPS plugin, similarly to how it’s done in the Gradle plugin

### Tools. Scripts

- [`KT-45194`](https://youtrack.jetbrains.com/issue/KT-45194) KT: Generate Kotlin Entities script: it doesn't work
- [`KT-44580`](https://youtrack.jetbrains.com/issue/KT-44580) Scripts: Unable to set new file annotation hooks after first snippet compilation

### Tools. kapt

- [`KT-45168`](https://youtrack.jetbrains.com/issue/KT-45168) KAPT: Java stubs generated for Kotlin files generated by annotation processors


## Recent ChangeLogs:
### [ChangeLog-1.3.X](docs/changelogs/ChangeLog-1.3.X.md)
### [ChangeLog-1.2.X](docs/changelogs/ChangeLog-1.2.X.md)
### [ChangeLog-1.1.X](docs/changelogs/ChangeLog-1.1.X.md)
### [ChangeLog-1.0.X](docs/changelogs/ChangeLog-1.0.X.md)
