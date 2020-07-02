# CHANGELOG

## 1.4-M3

### Compiler

#### New Features

- [`KT-23575`](https://youtrack.jetbrains.com/issue/KT-23575) Deprecate with replacement and SinceKotlin 
- [`KT-38652`](https://youtrack.jetbrains.com/issue/KT-38652) Do not generate optional annotations to class files on JVM
- [`KT-38777`](https://youtrack.jetbrains.com/issue/KT-38777) Hide Throwable.addSuppressed member and prefer extension instead

#### Performance Improvements

- [`KT-38489`](https://youtrack.jetbrains.com/issue/KT-38489) Compilation of kotlin html DSL increasingly slow
- [`KT-28650`](https://youtrack.jetbrains.com/issue/KT-28650) Type inference for argument type is very slow if several interfaces with a type parameter is used as an upper bound of a type parameter

#### Fixes

- [`KT-15971`](https://youtrack.jetbrains.com/issue/KT-15971) Incorrect bytecode generated when inheriting default arguments not from the first supertype
- [`KT-25290`](https://youtrack.jetbrains.com/issue/KT-25290) NI: "AssertionError: If original type is SAM type, then candidate should have same type constructor" on out projection of Java class
- [`KT-28672`](https://youtrack.jetbrains.com/issue/KT-28672) Contracts on calls with implicit receivers
- [`KT-30279`](https://youtrack.jetbrains.com/issue/KT-30279) Support non-reified type parameters in typeOf
- [`KT-31908`](https://youtrack.jetbrains.com/issue/KT-31908) NI: CCE on passing lambda to function which accepts vararg SAM interface
- [`KT-32156`](https://youtrack.jetbrains.com/issue/KT-32156) New inference issue with generics
- [`KT-32229`](https://youtrack.jetbrains.com/issue/KT-32229) New inference algorithm not taking into account the upper bound class
- [`KT-33455`](https://youtrack.jetbrains.com/issue/KT-33455) Override equals/hashCode in functional interface wrappers
- [`KT-34902`](https://youtrack.jetbrains.com/issue/KT-34902) AnalyzerException: Argument 1: expected I, but found R for unsigned types in generic data class
- [`KT-35075`](https://youtrack.jetbrains.com/issue/KT-35075) AssertionError: "No resolved call for ..." with conditional function references
- [`KT-35468`](https://youtrack.jetbrains.com/issue/KT-35468) Overcome ambiguity between typealias kotlin.Throws and the aliased type kotlin.jvm.Throws
- [`KT-35681`](https://youtrack.jetbrains.com/issue/KT-35681) Wrong common supertype between raw and integer literal type leads to unsound code
- [`KT-35937`](https://youtrack.jetbrains.com/issue/KT-35937) Error "Declaration has several compatible actuals" on incremental build
- [`KT-36013`](https://youtrack.jetbrains.com/issue/KT-36013) Functional interface conversion not happens on a value of functional type with smart cast to a relevant functional type
- [`KT-36045`](https://youtrack.jetbrains.com/issue/KT-36045) Do not depend on the order of lambda arguments to coerce result to `Unit`
- [`KT-36448`](https://youtrack.jetbrains.com/issue/KT-36448) NI: fix tests after enabling NI in the compiler
- [`KT-36706`](https://youtrack.jetbrains.com/issue/KT-36706) Prohibit functional interface constructor references
- [`KT-36969`](https://youtrack.jetbrains.com/issue/KT-36969) Generate @NotNull on instance parameters of Interface$DefaultImpls methods
- [`KT-37058`](https://youtrack.jetbrains.com/issue/KT-37058) Incorrect overload resolution ambiguity on callable reference in a conditional expression with new inference
- [`KT-37149`](https://youtrack.jetbrains.com/issue/KT-37149) Conversion when generic specified by type argument of SAM type
- [`KT-37249`](https://youtrack.jetbrains.com/issue/KT-37249) false TYPE_MISMATCH when When-expression branches have try-catch blocks
- [`KT-37341`](https://youtrack.jetbrains.com/issue/KT-37341) NI: Type mismatch with combination of lambda and function reference
- [`KT-37436`](https://youtrack.jetbrains.com/issue/KT-37436) AME: "Receiver class does not define or inherit an implementation of the resolved method" in runtime on usage of non-abstract method of fun interface
- [`KT-37510`](https://youtrack.jetbrains.com/issue/KT-37510) NI infers `java.lang.Void` from the expression in a lazy property delegate and throws ClassCastException at runtime
- [`KT-37541`](https://youtrack.jetbrains.com/issue/KT-37541) SAM conversion with fun interface without a function fails on compiling and IDE analysis in SamAdapterFunctionsScope.getSamConstructor()
- [`KT-37574`](https://youtrack.jetbrains.com/issue/KT-37574) NI: Type mismatch with Kotlin object extending functional type passed as @FunctionalInterface to Java
- [`KT-37630`](https://youtrack.jetbrains.com/issue/KT-37630) NI: ILT suitability in a call is broken if there are CST calculation and calling function's type parameters
- [`KT-37665`](https://youtrack.jetbrains.com/issue/KT-37665) NI: applicability error due to implicitly inferred Nothing for returning T with expected type
- [`KT-37712`](https://youtrack.jetbrains.com/issue/KT-37712) No extension receiver in functional interface created with lambda
- [`KT-37715`](https://youtrack.jetbrains.com/issue/KT-37715) NI: VerifyError: Bad type on operand stack with varargs generic value when type is inferred
- [`KT-37721`](https://youtrack.jetbrains.com/issue/KT-37721) NI: Function reference with vararg parameter treated as array and missing default parameter is rejected
- [`KT-37887`](https://youtrack.jetbrains.com/issue/KT-37887) NI: Smart casting for Map doesn't work if the variable is already  "smart casted"
- [`KT-37914`](https://youtrack.jetbrains.com/issue/KT-37914) NI: broken inference for a casting to subtype function within the common constraint system with this subtype
- [`KT-37952`](https://youtrack.jetbrains.com/issue/KT-37952) NI: improve lambdas completion through separation the lambdas analysis into several steps
- [`KT-38069`](https://youtrack.jetbrains.com/issue/KT-38069) Callable reference adaptation should have dependency on API version 1.4
- [`KT-38143`](https://youtrack.jetbrains.com/issue/KT-38143) New type inference fails when calling extension function defined on generic type with type arguments nested too deep
- [`KT-38156`](https://youtrack.jetbrains.com/issue/KT-38156) FIR Metadata generation
- [`KT-38197`](https://youtrack.jetbrains.com/issue/KT-38197) java.lang.OutOfMemoryError: Java heap space: failed reallocation of scalar replaced objects
- [`KT-38259`](https://youtrack.jetbrains.com/issue/KT-38259) NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER for provideDelegate
- [`KT-38337`](https://youtrack.jetbrains.com/issue/KT-38337) Map delegation fails for inline classes
- [`KT-38401`](https://youtrack.jetbrains.com/issue/KT-38401) FIR: protected effective visibility is handled unprecisely
- [`KT-38416`](https://youtrack.jetbrains.com/issue/KT-38416) FIR: infinite loop in BB coroutine test 'overrideDefaultArgument.kt'
- [`KT-38432`](https://youtrack.jetbrains.com/issue/KT-38432) FIR: incorrect effective visibility in anonymous object
- [`KT-38434`](https://youtrack.jetbrains.com/issue/KT-38434) Implement resolution of suspend-conversion on FE only, but give error if suspend conversion is called
- [`KT-38439`](https://youtrack.jetbrains.com/issue/KT-38439) NI: anonymous functions without receiver is allowed if there is an expected type with receiver
- [`KT-38473`](https://youtrack.jetbrains.com/issue/KT-38473) FIR: ConeIntegerLiteralType in signature
- [`KT-38537`](https://youtrack.jetbrains.com/issue/KT-38537) IllegalArgumentException: "marginPrefix must be non-blank string" with raw strings and space as margin prefix in trimMargin() call
- [`KT-38604`](https://youtrack.jetbrains.com/issue/KT-38604) Implicit suspend conversion on call arguments doesn't work on vararg elements
- [`KT-38680`](https://youtrack.jetbrains.com/issue/KT-38680) NSME when calling generic interface method with default parameters overriden with inline class type argument
- [`KT-38681`](https://youtrack.jetbrains.com/issue/KT-38681) Wrong bytecode generated when calling generic interface method with default parameters overriden with primitive type argument
- [`KT-38691`](https://youtrack.jetbrains.com/issue/KT-38691) NI: overload resolution ambiguity if take `R` and `() -> R`, and pass literal lambda, which returns `R`
- [`KT-38799`](https://youtrack.jetbrains.com/issue/KT-38799) False positive USELESS_CAST for lambda parameter
- [`KT-38802`](https://youtrack.jetbrains.com/issue/KT-38802) Generated code crashes by ClassCastException when delegating with inline class
- [`KT-38853`](https://youtrack.jetbrains.com/issue/KT-38853) Backend Internal error: Error type encountered: Unresolved type for nested class used in an annotation argument on an interface method
- [`KT-38890`](https://youtrack.jetbrains.com/issue/KT-38890) NI: false negative Type mismatch for values with fun keyword
- [`KT-39010`](https://youtrack.jetbrains.com/issue/KT-39010) NI: Regression with false-positive smartcast on var of generic type
- [`KT-39013`](https://youtrack.jetbrains.com/issue/KT-39013) 202, ASM 8: "AnalyzerException: Execution can fall off the end of the code"
- [`KT-39260`](https://youtrack.jetbrains.com/issue/KT-39260) "AssertionError: Unsigned type expected: Int" in range
- [`KT-39305`](https://youtrack.jetbrains.com/issue/KT-39305) NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER: unable to infer deeply nested type bound when class implements generic interface
- [`KT-39408`](https://youtrack.jetbrains.com/issue/KT-39408) Using unsigned arrays as generics fails in 1.4-M2 with class cast exception
- [`KT-39533`](https://youtrack.jetbrains.com/issue/KT-39533) NI: Wrong overload resolution for methods with SAM converted function reference arguments
- [`KT-39535`](https://youtrack.jetbrains.com/issue/KT-39535) NI: Inference fails for the parameters of SAM converted lambdas with type parameters
- [`KT-39603`](https://youtrack.jetbrains.com/issue/KT-39603) Require explicit override in JVM default compatibility mode on implicit generic specialization of inherited methods in classes
- [`KT-39671`](https://youtrack.jetbrains.com/issue/KT-39671) Couldn't inline method call 'expectBody'
- [`KT-39816`](https://youtrack.jetbrains.com/issue/KT-39816) NI:ClassCastException and no IDE error with provideDelegate when DELEGATE_SPECIAL_FUNCTION_MISSING in OI
- [`KT-28483`](https://youtrack.jetbrains.com/issue/KT-28483) Override of generic-return-typed function with inline class should lead to a boxing
- [`KT-32779`](https://youtrack.jetbrains.com/issue/KT-32779) `Rewrite at slice` in array access resolution in coroutine inference
- [`KT-33226`](https://youtrack.jetbrains.com/issue/KT-33226) Object INSTANCE field not annotated with NotNull in generated bytecode
- [`KT-36968`](https://youtrack.jetbrains.com/issue/KT-36968) Public instance field for non-public companion object not generated as deprecated in JVM_IR with language version 1.3
- [`KT-36985`](https://youtrack.jetbrains.com/issue/KT-36985) $default method for deprecated Kotlin function with default parameters is not generated as deprecated in JVM_IR
- [`KT-36990`](https://youtrack.jetbrains.com/issue/KT-36990) FIR: Rebase test-data branch to master
- [`KT-37005`](https://youtrack.jetbrains.com/issue/KT-37005) Private method generated for @InlineOnly fun in multifile facade in JVM_IR
- [`KT-37010`](https://youtrack.jetbrains.com/issue/KT-37010) Synthetic 'this$0' field is generated as @NotNull in JVM_IR
- [`KT-37011`](https://youtrack.jetbrains.com/issue/KT-37011) Synthetic (constructor) accessor parameters are generated with nullability annotations in JVM_IR
- [`KT-37013`](https://youtrack.jetbrains.com/issue/KT-37013) 'constructor-impl' method for inline class is generated as 'final' in JVM_IR
- [`KT-37015`](https://youtrack.jetbrains.com/issue/KT-37015) 'constructor-impl$default' in inline class is generated with 'java.lang.Object' marker parameter in JVM_IR
- [`KT-37047`](https://youtrack.jetbrains.com/issue/KT-37047) Synthetic 'main(String[])' has @NotNull on parameter in JVM_IR
- [`KT-37085`](https://youtrack.jetbrains.com/issue/KT-37085) Field for captured values are generated with nullablity annotations on JVM_IR
- [`KT-37120`](https://youtrack.jetbrains.com/issue/KT-37120) [FIR] False UNRESOLVED_REFERENCE for public and protected member functions and properties which are declared in object inner class
- [`KT-37397`](https://youtrack.jetbrains.com/issue/KT-37397) JVM_IR: synthetic methods do not have nullability annotations
- [`KT-37963`](https://youtrack.jetbrains.com/issue/KT-37963) ClassCastException: Value of inline class represented as 'java.lang.Object' is not boxed properly on return from lambda
- [`KT-38437`](https://youtrack.jetbrains.com/issue/KT-38437) [FIR] String(CharArray) is resolved to java.lang.String constructor instead of kotlin.text.String pseudo-constructor
- [`KT-39040`](https://youtrack.jetbrains.com/issue/KT-39040) FIR: Deserialize annotations from compiled Kotlin binaries
- [`KT-39229`](https://youtrack.jetbrains.com/issue/KT-39229) NI: resolution to wrong candidate (SAM-type against similar functional type)
- [`KT-39387`](https://youtrack.jetbrains.com/issue/KT-39387) Can't build Kotlin project due to overload resolution ambiguity on flatMap calls
- [`KT-39793`](https://youtrack.jetbrains.com/issue/KT-39793) Don't generate nullability annotations on '...$delegate' fields

### Docs & Examples

- [`KT-36245`](https://youtrack.jetbrains.com/issue/KT-36245) Document that @kotlin.native.ThreadLocal annotation doesn't work anywhere except in Kotlin/Native
- [`KT-37943`](https://youtrack.jetbrains.com/issue/KT-37943) Conflicting overloads in the factory functions sample code in Coding Conventions Page

### IDE

#### New Features

- [`KT-10974`](https://youtrack.jetbrains.com/issue/KT-10974) Add Code Style: Import Layout Configuration Table
- [`KT-39065`](https://youtrack.jetbrains.com/issue/KT-39065) "Join lines" should remove trailing comma on call site

#### Fixes

- [`KT-9065`](https://youtrack.jetbrains.com/issue/KT-9065) Wrong result when move statement through if block with call with lambda
- [`KT-14757`](https://youtrack.jetbrains.com/issue/KT-14757) Move statement up breaks code in function parameter list
- [`KT-14946`](https://youtrack.jetbrains.com/issue/KT-14946) Move statement up/down (with Ctrl+Shift+Up/Down) messes with empty lines
- [`KT-15143`](https://youtrack.jetbrains.com/issue/KT-15143) Kotlin: Colors&Fonts -> "Enum entry" should use Language Default -> Classes - Static field
- [`KT-17887`](https://youtrack.jetbrains.com/issue/KT-17887) Moving statement (Ctrl/Cmd+Shift+Down) messes with use block
- [`KT-34187`](https://youtrack.jetbrains.com/issue/KT-34187) UAST cannot get type of array access
- [`KT-34524`](https://youtrack.jetbrains.com/issue/KT-34524) "PSI and index do not match" and IDE freeze with library import from `square/workflow`
- [`KT-35574`](https://youtrack.jetbrains.com/issue/KT-35574) UAST: UBreakExpression in when expression should be UYieldExpression
- [`KT-36801`](https://youtrack.jetbrains.com/issue/KT-36801) IDE: Unsupported language version value is represented with "latest stable" in GUI
- [`KT-37378`](https://youtrack.jetbrains.com/issue/KT-37378) Remove IDE option "Enable new type inference algorithm..." in 1.4
- [`KT-38003`](https://youtrack.jetbrains.com/issue/KT-38003) "Analyze Data Flow from Here" should work on parameter of abstract method
- [`KT-38173`](https://youtrack.jetbrains.com/issue/KT-38173) Reified types do no have extends information
- [`KT-38217`](https://youtrack.jetbrains.com/issue/KT-38217) Make Kotlin plugin settings searchable
- [`KT-38247`](https://youtrack.jetbrains.com/issue/KT-38247) "IncorrectOperationException: Incorrect expression" through UltraLightUtils.kt: inlined string is not escaped before parsing
- [`KT-38293`](https://youtrack.jetbrains.com/issue/KT-38293) Throwable: "'codestyle.name.kotlin' is not found in java.util.PropertyResourceBundle" at KotlinLanguageCodeStyleSettingsProvider.getConfigurableDisplayName()
- [`KT-38407`](https://youtrack.jetbrains.com/issue/KT-38407) Drop components from plugin.xml
- [`KT-38443`](https://youtrack.jetbrains.com/issue/KT-38443) No error on change in property initializer
- [`KT-38521`](https://youtrack.jetbrains.com/issue/KT-38521) ISE: Loop in parent structure when converting a DOT_QUALIFIED_EXPRESSION with parent ANNOTATED_EXPRESSION
- [`KT-38571`](https://youtrack.jetbrains.com/issue/KT-38571) Rework deprecated EPs
- [`KT-38632`](https://youtrack.jetbrains.com/issue/KT-38632) Change the code style to official in tests

### IDE. Code Style, Formatting

#### Fixes

- [`KT-24750`](https://youtrack.jetbrains.com/issue/KT-24750) Formatter: Minimum blank lines after class header does nothing
- [`KT-31169`](https://youtrack.jetbrains.com/issue/KT-31169) IDEA settings search fails to find "Tabs and Indents" tab in Kotlin code style settings
- [`KT-35359`](https://youtrack.jetbrains.com/issue/KT-35359) Incorrect indent for multiline expression in string template
- [`KT-37420`](https://youtrack.jetbrains.com/issue/KT-37420) Add setting to disable inserting empty line between declaration and declaration with comment
- [`KT-37891`](https://youtrack.jetbrains.com/issue/KT-37891) Formatter inserts empty lines between annotated properties
- [`KT-38036`](https://youtrack.jetbrains.com/issue/KT-38036) Use trailing comma setting does not apply to code example in Settings dialog
- [`KT-38568`](https://youtrack.jetbrains.com/issue/KT-38568) False positive: weak warning "Missing line break" on -> in when expression
- [`KT-39024`](https://youtrack.jetbrains.com/issue/KT-39024) Add option for blank lines before declaration with comment or annotation on separate line
- [`KT-39079`](https://youtrack.jetbrains.com/issue/KT-39079) Trailing comma: add base support for call site
- [`KT-39123`](https://youtrack.jetbrains.com/issue/KT-39123) Option `Align 'when' branches in columns` does nothing
- [`KT-39180`](https://youtrack.jetbrains.com/issue/KT-39180) Move trailing comma settings in Other tab

### IDE. Completion

- [`KT-18538`](https://youtrack.jetbrains.com/issue/KT-18538) Completion of static members of grand-super java class inserts unnecessary qualifier
- [`KT-38445`](https://youtrack.jetbrains.com/issue/KT-38445) Fully qualified class name is used instead after insertion of `delay` method

### IDE. Debugger

#### Fixes

- [`KT-14057`](https://youtrack.jetbrains.com/issue/KT-14057) Debugger couldn't step into Reader.read
- [`KT-14828`](https://youtrack.jetbrains.com/issue/KT-14828) Bad step into/over behavior for functions with default parameters
- [`KT-36403`](https://youtrack.jetbrains.com/issue/KT-36403) Method breakpoints don't work for libraries
- [`KT-36404`](https://youtrack.jetbrains.com/issue/KT-36404) Evaluate: "AssertionError: Argument expression is not saved for a SAM constructor"
- [`KT-37486`](https://youtrack.jetbrains.com/issue/KT-37486) Kotlin plugin keeps reference to stream debugger support classes after stream debugger plugin is disabled
- [`KT-38484`](https://youtrack.jetbrains.com/issue/KT-38484) Coroutines Debugger: IAE “Requested element count -1 is less than zero.” is thrown by calling dumpCoroutines
- [`KT-38606`](https://youtrack.jetbrains.com/issue/KT-38606) Coroutine Debugger: OCE from org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.BaseMirror.isCompatible
- [`KT-39143`](https://youtrack.jetbrains.com/issue/KT-39143) NPE on setCurrentStackFrame to Kotlin inner compiled class content
- [`KT-39412`](https://youtrack.jetbrains.com/issue/KT-39412) Failed to find Premain-Class manifest attribute when debugging main method with ktor
- [`KT-39634`](https://youtrack.jetbrains.com/issue/KT-39634) (CoroutineDebugger) Agent doesn't start if using kotlinx-coroutines-core only dependency
- [`KT-39648`](https://youtrack.jetbrains.com/issue/KT-39648) Coroutines debugger doesn't see stacktraces in case of the project has kotlinx-coroutines-debug dependency

### IDE. Gradle Integration

#### Performance Improvements

- [`KT-39059`](https://youtrack.jetbrains.com/issue/KT-39059) Poor performance of `modifyDependenciesOnMppModules`

#### Fixes

- [`KT-35921`](https://youtrack.jetbrains.com/issue/KT-35921) Gradle Import fails with "Unsupported major.minor version 52.0" on pure Java project in case "Gradle JDK" is lower 1.8 and Kotlin plugin is enabled
- [`KT-36673`](https://youtrack.jetbrains.com/issue/KT-36673) Gradle Project importing: move ModelBuilders and ModelProviders to kotlin-gradle-tooling jar
- [`KT-36792`](https://youtrack.jetbrains.com/issue/KT-36792) IDEA 2020.1: Some module->module dependencies in HMPP project are missed after import from Gradle
- [`KT-37125`](https://youtrack.jetbrains.com/issue/KT-37125) Imported modules structure for MPP project is displayed messy in UI in IDEA 2020.1
- [`KT-37428`](https://youtrack.jetbrains.com/issue/KT-37428) NPE at KotlinFacetSettings.setLanguageLevel() on the first project import
- [`KT-38706`](https://youtrack.jetbrains.com/issue/KT-38706) IDE Gradle import creates 4 JavaScript modules for MPP source sets with BOTH compiler type
- [`KT-38767`](https://youtrack.jetbrains.com/issue/KT-38767) Published hierarchical multiplatform library symbols are unresolved in IDE (master)
- [`KT-38842`](https://youtrack.jetbrains.com/issue/KT-38842) False positive [INVISIBLE_MEMBER] for `internal` declaration of commonMain called from commonTest
- [`KT-39213`](https://youtrack.jetbrains.com/issue/KT-39213) IDE: references from MPP project to JavaScript library are unresolved, when project and library are compiled with "both" mode
- [`KT-39657`](https://youtrack.jetbrains.com/issue/KT-39657) Language settings for intermediate source-sets are lost during import

### IDE. Gradle. Script

#### New Features

- [`KT-34481`](https://youtrack.jetbrains.com/issue/KT-34481) *.gradle.kts: use Intellij IDEA Gradle project sync mechanics for updating script configuration
- [`KT-34444`](https://youtrack.jetbrains.com/issue/KT-34444) *.gradle.kts: special storage of all scripts configuration on one file
- [`KT-35153`](https://youtrack.jetbrains.com/issue/KT-35153) build.gradle.kts: scripts in removed subproject remain imported, but shouldn't
- [`KT-35573`](https://youtrack.jetbrains.com/issue/KT-35573) Request for gradle build script configuration only after explicit click on notification
- [`KT-38875`](https://youtrack.jetbrains.com/issue/KT-38875) Deadlock in ScriptClassRootsUpdater.checkInvalidSdks

#### Performance Improvements

- [`KT-34138`](https://youtrack.jetbrains.com/issue/KT-34138) Deadlock in `ScriptTemplatesFromDependenciesProvider`

#### Fixes

- [`KT-34265`](https://youtrack.jetbrains.com/issue/KT-34265) Bogus "build configuration failed, run 'gradle tasks' for more information" message and other issues related to "script dependencies"
- [`KT-34444`](https://youtrack.jetbrains.com/issue/KT-34444) *.gradle.kts: special storage of all scripts configuration on one file
- [`KT-35153`](https://youtrack.jetbrains.com/issue/KT-35153) build.gradle.kts: scripts in removed subproject remain imported, but shouldn't
- [`KT-35573`](https://youtrack.jetbrains.com/issue/KT-35573) Request for gradle build script configuration only after explicit click on notification
- [`KT-36675`](https://youtrack.jetbrains.com/issue/KT-36675) move .gradle.kts ModelBuilders and ModelProviders to kotlin-gradle-tooling jar
- [`KT-37178`](https://youtrack.jetbrains.com/issue/KT-37178) build.gradle.kts: Rework the notification for scripts out of project
- [`KT-37631`](https://youtrack.jetbrains.com/issue/KT-37631) Unnecessary loading dependencies after opening build.gradle.kts after project import with Gradle 6
- [`KT-37863`](https://youtrack.jetbrains.com/issue/KT-37863) Scanning dependencies for script definitions takes too long or indefinitely during Gradle import
- [`KT-38296`](https://youtrack.jetbrains.com/issue/KT-38296) MISSING_DEPENDENCY_SUPERCLASS in the build.gradle.kts editor while Gradle runs Ok
- [`KT-38541`](https://youtrack.jetbrains.com/issue/KT-38541) "Invalid file" exception in ScriptChangeListener.getAnalyzableKtFileForScript()
- [`KT-38875`](https://youtrack.jetbrains.com/issue/KT-38875) Deadlock in ScriptClassRootsUpdater.checkInvalidSdks
- [`KT-39104`](https://youtrack.jetbrains.com/issue/KT-39104) “Gradle Kotlin DSL script configuration is missing” after importing project in IJ201, Gradle 6.3
- [`KT-39469`](https://youtrack.jetbrains.com/issue/KT-39469) Gradle version is not updated in script dependencies if the version of gradle was changed in gradle-wrapper.properties
- [`KT-39771`](https://youtrack.jetbrains.com/issue/KT-39771) Freeze 30s from org.jetbrains.kotlin.scripting.resolve.ApiChangeDependencyResolverWrapper.resolve on loading script configuration with Gradle 5.6.4

### IDE. Inspections and Intentions

#### New Features

- [`KT-14884`](https://youtrack.jetbrains.com/issue/KT-14884) Intention to add missing "class" keyword for enum and annotation top-level declarations
- [`KT-17209`](https://youtrack.jetbrains.com/issue/KT-17209) Provide intention to fix platform declaration clash (CONFLICTING_JVM_DECLARATIONS)
- [`KT-24522`](https://youtrack.jetbrains.com/issue/KT-24522) Suggest to move typealias outside the class
- [`KT-30263`](https://youtrack.jetbrains.com/issue/KT-30263) Detect redundant conversions of unsigned types
- [`KT-35893`](https://youtrack.jetbrains.com/issue/KT-35893) Support Inspection for unnecessary asSequence() call
- [`KT-38559`](https://youtrack.jetbrains.com/issue/KT-38559) "Change JVM name" (@JvmName) quickfix: improve name suggester for generic functions
- [`KT-38597`](https://youtrack.jetbrains.com/issue/KT-38597) Expand Boolean intention
- [`KT-38982`](https://youtrack.jetbrains.com/issue/KT-38982) Add "Logger initialized with foreign class" inspection
- [`KT-39131`](https://youtrack.jetbrains.com/issue/KT-39131) TrailingCommaInspection: should suggest fixes for call-site without warnings

#### Fixes

- [`KT-5271`](https://youtrack.jetbrains.com/issue/KT-5271) Missing QuickFix for Multiple supertypes available
- [`KT-11865`](https://youtrack.jetbrains.com/issue/KT-11865) "Create secondary constructor" quick fix always inserts parameter-less call to `this()`
- [`KT-14021`](https://youtrack.jetbrains.com/issue/KT-14021) Quickfix to add parameter to function gives strange name to parameter
- [`KT-17121`](https://youtrack.jetbrains.com/issue/KT-17121) "Implement members" quick fix is not suggested
- [`KT-17222`](https://youtrack.jetbrains.com/issue/KT-17222) "Convert reference to lambda" creates red code for method with default argument values
- [`KT-17368`](https://youtrack.jetbrains.com/issue/KT-17368) Don't highlight members annotated with @JsName as unused
- [`KT-20795`](https://youtrack.jetbrains.com/issue/KT-20795) "replace explicit parameter with it" creates invalid code in case of overload ambiguities
- [`KT-22014`](https://youtrack.jetbrains.com/issue/KT-22014) Intention "convert lambda to reference" should be available for implicit 'this'
- [`KT-22015`](https://youtrack.jetbrains.com/issue/KT-22015) Intention "Convert lambda to reference" should be available in spite of the lambda in or out of parentheses
- [`KT-22142`](https://youtrack.jetbrains.com/issue/KT-22142) Intentions: "Convert to primary constructor" changes semantics for property with custom setter
- [`KT-22878`](https://youtrack.jetbrains.com/issue/KT-22878) Empty argument list at the call site of custom function named "suspend" shouldn't be reported as unnecessary
- [`KT-24138`](https://youtrack.jetbrains.com/issue/KT-24138) Incorrect behavior in "convert reference to lambda" with new inference enabled, on function reference with default arguments 
- [`KT-24281`](https://youtrack.jetbrains.com/issue/KT-24281) Importing of invoke() from the same file is reported as unused even if it isn't
- [`KT-25050`](https://youtrack.jetbrains.com/issue/KT-25050) False-positive inspection "Call replaceable with binary operator" for 'equals'
- [`KT-26361`](https://youtrack.jetbrains.com/issue/KT-26361) @Deprecated "ReplaceWith" quickfix inserts 'this' incorrectly when using function imports
- [`KT-27651`](https://youtrack.jetbrains.com/issue/KT-27651) 'Condition is always true' inspection should not be triggered when the condition has references to a named constant
- [`KT-29934`](https://youtrack.jetbrains.com/issue/KT-29934) False negative `Change type` quickfix on primary constructor override val parameter when it has wrong type
- [`KT-31682`](https://youtrack.jetbrains.com/issue/KT-31682) 'Convert lambda to reference' intention inside class with function which return object produces uncompilable code
- [`KT-31760`](https://youtrack.jetbrains.com/issue/KT-31760) Implement Abstract Function/Property intentions position generated member improperly
- [`KT-32511`](https://youtrack.jetbrains.com/issue/KT-32511) Create class quick fix is not suggested in super type list in case of missing primary constructor
- [`KT-32565`](https://youtrack.jetbrains.com/issue/KT-32565) False positive "Variable is the same as 'credentials' and should be inlined" with object declared and returned from lambda
- [`KT-32801`](https://youtrack.jetbrains.com/issue/KT-32801) False positive "Call on collection type may be reduced" with mapNotNull, generic lambda block and new inference
- [`KT-33951`](https://youtrack.jetbrains.com/issue/KT-33951) ReplaceWith quickfix with unqualified object member call doesn't substitute argument for parameter
- [`KT-34378`](https://youtrack.jetbrains.com/issue/KT-34378) "Convert lambda to reference" refactoring does not work for suspend functions
- [`KT-34677`](https://youtrack.jetbrains.com/issue/KT-34677) False positive "Collection count can be converted to size" with `Iterable`
- [`KT-34696`](https://youtrack.jetbrains.com/issue/KT-34696) Wrong 'Redundant qualifier name' for 'MyEnum.values' usage
- [`KT-34713`](https://youtrack.jetbrains.com/issue/KT-34713) "Condition is always 'false'": quickfix "Delete expression" doesn't remove `else` keyword (may break control flow)
- [`KT-35015`](https://youtrack.jetbrains.com/issue/KT-35015) ReplaceWith doesn't substitute parameters with argument expressions
- [`KT-35329`](https://youtrack.jetbrains.com/issue/KT-35329) Replace 'when' with 'if' intention: do not suggest if 'when' is used as expression and it has no 'else' branch
- [`KT-36194`](https://youtrack.jetbrains.com/issue/KT-36194) "Add braces to 'for' statement" inserts extra line break and moves the following single-line comment
- [`KT-36406`](https://youtrack.jetbrains.com/issue/KT-36406) "To ordinary string literal" intention adds unnecessary escapes to characters in template expression
- [`KT-36461`](https://youtrack.jetbrains.com/issue/KT-36461) "Create enum constant" quick fix adds after semicolon, if the last entry has a comma
- [`KT-36462`](https://youtrack.jetbrains.com/issue/KT-36462) "Create enum constant" quick fix doesn't add trailing comma
- [`KT-36508`](https://youtrack.jetbrains.com/issue/KT-36508) False positive "Replace 'to' with infix form" when 'to' lambda generic type argument is specified explicitly
- [`KT-36930`](https://youtrack.jetbrains.com/issue/KT-36930) Intention "Specify type explicitly" adds NotNull annotation when calling java method with the annotation
- [`KT-37148`](https://youtrack.jetbrains.com/issue/KT-37148) "Remove redundant `.let` call doesn't remove extra calls
- [`KT-37156`](https://youtrack.jetbrains.com/issue/KT-37156) "Unused unary operator" inspection highlighting is hard to see
- [`KT-37173`](https://youtrack.jetbrains.com/issue/KT-37173) "Replace with string templates" intention for String.format produces uncompilable string template
- [`KT-37181`](https://youtrack.jetbrains.com/issue/KT-37181) Don't show "Remove redundant qualifier name" inspection on qualified Companion imported with star import
- [`KT-37214`](https://youtrack.jetbrains.com/issue/KT-37214) "Convert lambda to reference" with a labeled "this" receiver fails
- [`KT-37256`](https://youtrack.jetbrains.com/issue/KT-37256) False positive `PlatformExtensionReceiverOfInline` inspection if a platform type value is passed to a nullable receiver
- [`KT-37744`](https://youtrack.jetbrains.com/issue/KT-37744) "Convert lambda to reference" inspection quick fix create incompilable code when type is inferred from lambda parameter
- [`KT-37746`](https://youtrack.jetbrains.com/issue/KT-37746) "Redundant suspend modifier" should not be reported for functions with actual keyword
- [`KT-37842`](https://youtrack.jetbrains.com/issue/KT-37842) "Convert to anonymous function" creates broken code with suspend functions
- [`KT-37908`](https://youtrack.jetbrains.com/issue/KT-37908) "Convert to anonymous object" quickfix: false negative when interface has concrete functions
- [`KT-37967`](https://youtrack.jetbrains.com/issue/KT-37967) Replace 'invoke' with direct call intention adds unnecessary parenthesis
- [`KT-37977`](https://youtrack.jetbrains.com/issue/KT-37977) "Replace 'invoke' with direct call" intention: false positive when function is not operator
- [`KT-38062`](https://youtrack.jetbrains.com/issue/KT-38062) Reactor Quickfix throws `NotImplementedError` for Kotlin
- [`KT-38240`](https://youtrack.jetbrains.com/issue/KT-38240) False positive redundant semicolon with `as` cast and `not` unary operator on next line
- [`KT-38261`](https://youtrack.jetbrains.com/issue/KT-38261) Redundant 'let' call removal leaves ?. operator and makes code uncompilable
- [`KT-38310`](https://youtrack.jetbrains.com/issue/KT-38310) Remove explicit type annotation intention drops 'suspend'
- [`KT-38492`](https://youtrack.jetbrains.com/issue/KT-38492) False positive "Add import" intention for already imported class
- [`KT-38520`](https://youtrack.jetbrains.com/issue/KT-38520) SetterBackingFieldAssignmentInspection throws exception
- [`KT-38649`](https://youtrack.jetbrains.com/issue/KT-38649) False positive quickfix "Assignment should be lifted out of when" in presence of smartcasts
- [`KT-38677`](https://youtrack.jetbrains.com/issue/KT-38677) Invalid psi tree after `Lift assigment out of...`
- [`KT-38790`](https://youtrack.jetbrains.com/issue/KT-38790) "Convert sealed subclass to object" for data classes doesn't remove 'data' keyword
- [`KT-38829`](https://youtrack.jetbrains.com/issue/KT-38829) 'Remove redundant backticks' can be broken with @ in name
- [`KT-38831`](https://youtrack.jetbrains.com/issue/KT-38831) 'Replace with assignment' can be broken with fast code change
- [`KT-38832`](https://youtrack.jetbrains.com/issue/KT-38832) "Remove curly braces" intention may produce CCE
- [`KT-38948`](https://youtrack.jetbrains.com/issue/KT-38948) False positive quickfix "Make containing function suspend" for anonymous function
- [`KT-38961`](https://youtrack.jetbrains.com/issue/KT-38961) "Useless call on collection type" for filterNotNull on non-null array where list return type is expected
- [`KT-39069`](https://youtrack.jetbrains.com/issue/KT-39069) Improve TrailingCommaInspection
- [`KT-39151`](https://youtrack.jetbrains.com/issue/KT-39151) False positive inspection to replace Java forEach with Kotlin forEach when using ConcurrentHashMap

### IDE. JS

- [`KT-39275`](https://youtrack.jetbrains.com/issue/KT-39275) Kotlin JS Browser template for kotlin dsl doesn't include index.html

### IDE. KDoc

- [`KT-32163`](https://youtrack.jetbrains.com/issue/KT-32163) Open Quick Documentation when cursor inside function / constructor brackets

### IDE. Navigation

- [`KT-32245`](https://youtrack.jetbrains.com/issue/KT-32245) Method in Kotlin class is not listed among implementing methods
- [`KT-33510`](https://youtrack.jetbrains.com/issue/KT-33510) There is no gutter icon to navigate from `actual` to `expect` if `expect` and the corresponding `actual` declarations are in the same file
- [`KT-38260`](https://youtrack.jetbrains.com/issue/KT-38260) Navigation bar doesn't show directories of files with a single top level Kotlin class
- [`KT-38466`](https://youtrack.jetbrains.com/issue/KT-38466) Top level functions/properties aren't shown in navigation panel

### IDE. Project View

- [`KT-36444`](https://youtrack.jetbrains.com/issue/KT-36444) Structure view: add ability to sort by visibility
- [`KT-38276`](https://youtrack.jetbrains.com/issue/KT-38276) Structure view: support visibility filter for class properties

### IDE. REPL

- [`KT-38454`](https://youtrack.jetbrains.com/issue/KT-38454) Kotlin REPL in IntelliJ doesn't take module's JVM target setting into account

### IDE. Refactorings

- [`KT-12878`](https://youtrack.jetbrains.com/issue/KT-12878) "Change signature" forces line breaks after every parameter declaration
- [`KT-30128`](https://youtrack.jetbrains.com/issue/KT-30128) Change Signature should move lambda outside of parentheses if the arguments are reordered so that the lambda goes last
- [`KT-35338`](https://youtrack.jetbrains.com/issue/KT-35338) Move/rename refactorings mess up code formatting by wrapping lines
- [`KT-38449`](https://youtrack.jetbrains.com/issue/KT-38449) Extract variable refactoring is broken by NPE
- [`KT-38543`](https://youtrack.jetbrains.com/issue/KT-38543) Copy can't work to package with escaped package
- [`KT-38627`](https://youtrack.jetbrains.com/issue/KT-38627) Rename package refactorings mess up code formatting by wrapping lines

### IDE. Run Configurations

- [`KT-34516`](https://youtrack.jetbrains.com/issue/KT-34516) Don't suggest incompatible targets in a drop-down list for run test gutter icon in multiplatform projects
- [`KT-38102`](https://youtrack.jetbrains.com/issue/KT-38102) DeprecatedMethodException ConfigurationFactory.getId

### IDE. Scratch

- [`KT-38455`](https://youtrack.jetbrains.com/issue/KT-38455) Kotlin scratch files don't take module's JVM target setting into account

### IDE. Script

- [`KT-39791`](https://youtrack.jetbrains.com/issue/KT-39791) Kotlin plugin loads VFS in the output directories

### IDE. Structural Search

- [`KT-39721`](https://youtrack.jetbrains.com/issue/KT-39721) Optimize Kotlin SSR by using the index
- [`KT-39733`](https://youtrack.jetbrains.com/issue/KT-39733) Augmented assignment matching
- [`KT-39769`](https://youtrack.jetbrains.com/issue/KT-39769) "When expressions" predefined template doesn't match all when expressions

### IDE. Wizards

- [`KT-38810`](https://youtrack.jetbrains.com/issue/KT-38810) Incorrect order of build phases in Xcode project from new wizard
- [`KT-38952`](https://youtrack.jetbrains.com/issue/KT-38952) Remove old new_project_wizards
- [`KT-39503`](https://youtrack.jetbrains.com/issue/KT-39503) New Project wizard 1.4+: release kotlinx.html version is added to dependencies with milestone IDE plugin
- [`KT-39700`](https://youtrack.jetbrains.com/issue/KT-39700) Wizard: group project templates on the first step by the project type
- [`KT-39770`](https://youtrack.jetbrains.com/issue/KT-39770) CSS Support in Kotlin wizards
- [`KT-39843`](https://youtrack.jetbrains.com/issue/KT-39843) Change imports in JS/browser wizard
- [`KT-38158`](https://youtrack.jetbrains.com/issue/KT-38158) java.lang.NullPointerException when try to create new project via standard wizard on Mac os
- [`KT-38673`](https://youtrack.jetbrains.com/issue/KT-38673) New Project Wizard: multiplatform templates are generated having unsupported Gradle version in a wrapper
- [`KT-39826`](https://youtrack.jetbrains.com/issue/KT-39826) Fix Android app in New Template Wizard

### JS. Tools

- [`KT-32273`](https://youtrack.jetbrains.com/issue/KT-32273) Kotlin/JS console error on hot reload

### JavaScript

- [`KT-29916`](https://youtrack.jetbrains.com/issue/KT-29916) Implement `typeOf` on JS
- [`KT-35857`](https://youtrack.jetbrains.com/issue/KT-35857) Kotlin/JS CLI bundled to IDEA plugin can't compile using IR back-end out of the box
- [`KT-36798`](https://youtrack.jetbrains.com/issue/KT-36798) KJS: prohibit using @JsExport on a non-top-level declaration
- [`KT-37771`](https://youtrack.jetbrains.com/issue/KT-37771) KJS: Generated TypeScript does not recursively export base classes (can fail with generics)
- [`KT-38765`](https://youtrack.jetbrains.com/issue/KT-38765) [JS / IR] AssertionError: class EventEmitter: Super class should be any: with nested class extending parent class
- [`KT-38768`](https://youtrack.jetbrains.com/issue/KT-38768) KJS IR: generate ES2015 (aka ES6) classes
- [`KT-39088`](https://youtrack.jetbrains.com/issue/KT-39088) [ KJS / IR ] IllegalStateException: Concrete fake override IrBasedFunctionHandle

### Libraries

#### New Features

- [`KT-11253`](https://youtrack.jetbrains.com/issue/KT-11253) Function to sum long or other numeric property of items in a collection
- [`KT-28933`](https://youtrack.jetbrains.com/issue/KT-28933) capitalize() with Locale argument in the JDK stdlib
- [`KT-34142`](https://youtrack.jetbrains.com/issue/KT-34142) Create SortedMap with Comparator and items
- [`KT-34506`](https://youtrack.jetbrains.com/issue/KT-34506) Add Sequence.flatMap overload that works on Iterable
- [`KT-36894`](https://youtrack.jetbrains.com/issue/KT-36894) Support flatMapIndexed in the Collections API
- [`KT-38480`](https://youtrack.jetbrains.com/issue/KT-38480) Introduce experimental annotation for enabling overload resolution by lambda result
- [`KT-38708`](https://youtrack.jetbrains.com/issue/KT-38708) minOf/maxOf functions to return min/max value provided by selector
- [`KT-39707`](https://youtrack.jetbrains.com/issue/KT-39707) Make some interfaces in stdlib functional

#### Performance Improvements

- [`KT-23142`](https://youtrack.jetbrains.com/issue/KT-23142) toHashSet is suboptimal for inputs with a lot of duplicates

#### Fixes

- [`KT-21266`](https://youtrack.jetbrains.com/issue/KT-21266) Add module-info for standard library artifacts
- [`KT-23322`](https://youtrack.jetbrains.com/issue/KT-23322) Document 'reduce' operation behavior on empty collections
- [`KT-28753`](https://youtrack.jetbrains.com/issue/KT-28753) Comparing floating point values in array/list operations 'contains', 'indexOf', 'lastIndexOf': IEEE 754 or total order
- [`KT-30083`](https://youtrack.jetbrains.com/issue/KT-30083) Annotate KTypeProjection.STAR with JvmField in a compatible way
- [`KT-30084`](https://youtrack.jetbrains.com/issue/KT-30084) Annotate functions in KTypeProjection.Companion with JvmStatic
- [`KT-31343`](https://youtrack.jetbrains.com/issue/KT-31343) Deprecate old String <-> CharArray, ByteArray conversion api
- [`KT-34596`](https://youtrack.jetbrains.com/issue/KT-34596) Add some validation to KTypeProjection constructor
- [`KT-35978`](https://youtrack.jetbrains.com/issue/KT-35978) Review and remove experimental stdlib API status for 1.4
- [`KT-38388`](https://youtrack.jetbrains.com/issue/KT-38388) Document `fromIndex` and `toIndex` parameters
- [`KT-38566`](https://youtrack.jetbrains.com/issue/KT-38566) Kotlin/JS IR: kx.serialization & ktor+JsonFeature: SerializationException: Can't locate argument-less serializer for class
- [`KT-38854`](https://youtrack.jetbrains.com/issue/KT-38854) Gradually change the return type of collection min/max functions to non-nullable
- [`KT-39023`](https://youtrack.jetbrains.com/issue/KT-39023) Document split(Pattern) extension differences from Pattern.split
- [`KT-39064`](https://youtrack.jetbrains.com/issue/KT-39064) Introduce minOrNull and maxOrNull extension functions on collections
- [`KT-39235`](https://youtrack.jetbrains.com/issue/KT-39235) Lift experimental annotation from bit operations
- [`KT-39237`](https://youtrack.jetbrains.com/issue/KT-39237) Lift experimental annotation from common StringBuilder
- [`KT-39238`](https://youtrack.jetbrains.com/issue/KT-39238) Appendable.appendRange - remove nullability
- [`KT-39239`](https://youtrack.jetbrains.com/issue/KT-39239) Lift experimental annotation from String <-> utf8 conversion api
- [`KT-39244`](https://youtrack.jetbrains.com/issue/KT-39244) KJS: update polyfills, all or most of them must not be enumerable
- [`KT-39330`](https://youtrack.jetbrains.com/issue/KT-39330) Migrate declarations from kotlin.dom and kotlin.browser packages to kotlinx.*

### Middle-end. IR

- [`KT-31088`](https://youtrack.jetbrains.com/issue/KT-31088) need a way to compute fake overrides for pure IR
- [`KT-33207`](https://youtrack.jetbrains.com/issue/KT-33207) Kotlin/Native: KNPE during deserialization of an inner class
- [`KT-33267`](https://youtrack.jetbrains.com/issue/KT-33267) Kotlin/Native: Deserialization error for an "inner" extension property imported from a class
- [`KT-37255`](https://youtrack.jetbrains.com/issue/KT-37255) Make psi2ir aware of declarations provided by compiler plugins

### Reflection

- [`KT-22936`](https://youtrack.jetbrains.com/issue/KT-22936) Not all things can be changed to `createType` yet, and now `defaultType` methods are starting to fail
- [`KT-32241`](https://youtrack.jetbrains.com/issue/KT-32241) Move KType.javaType into stdlib from reflect
- [`KT-34344`](https://youtrack.jetbrains.com/issue/KT-34344) KType.javaType implementation throws when invoked with a typeOf<T>()
- [`KT-38491`](https://youtrack.jetbrains.com/issue/KT-38491) IllegalArgumentException when using callBy on function with inline class parameters and default arguments
- [`KT-38881`](https://youtrack.jetbrains.com/issue/KT-38881) Add KClass.isFun modifier of functional interfaces to reflection

### Tools. Android Extensions

- [`KT-25807`](https://youtrack.jetbrains.com/issue/KT-25807) Kotlin extension annotation @Parcelize in AIDL returns Object instead of original T

### Tools. CLI

- [`KT-15661`](https://youtrack.jetbrains.com/issue/KT-15661) Metadata compiler friend paths support
- [`KT-30211`](https://youtrack.jetbrains.com/issue/KT-30211) Support a way to pass arguments to the underlying JVM in kotlinc batch scripts on Windows
- [`KT-30778`](https://youtrack.jetbrains.com/issue/KT-30778) kotlin-compiler.jar contains shaded but not relocated kotlinx.coroutines
- [`KT-38070`](https://youtrack.jetbrains.com/issue/KT-38070) Compiler option to bypass prerelease metadata incompatibility error
- [`KT-38413`](https://youtrack.jetbrains.com/issue/KT-38413) Add JVM target bytecode version 14

### Tools. Compiler Plugins

- [`KT-39274`](https://youtrack.jetbrains.com/issue/KT-39274) [KJS / IR] Custom serializer for class without zero argument constructor doesn't compile

### Tools. Gradle

- [`KT-25428`](https://youtrack.jetbrains.com/issue/KT-25428) Kotlin Gradle Plugin: Use new Gradle API for Lazy tasks
- [`KT-34487`](https://youtrack.jetbrains.com/issue/KT-34487) Gradle build fails with "Cannot run program "java": error=7, Argument list too long
- [`KT-35341`](https://youtrack.jetbrains.com/issue/KT-35341) KotlinCompile: Symlinked friend paths are no longer supported
- [`KT-35957`](https://youtrack.jetbrains.com/issue/KT-35957) MPP IC fails with "X has several compatible actual declarations" error
- [`KT-38250`](https://youtrack.jetbrains.com/issue/KT-38250) Drop support for Gradle versions older than 5.3 in the Kotlin Gradle plugin

### Tools. Gradle. JS

#### New Features

- [`KT-30619`](https://youtrack.jetbrains.com/issue/KT-30619) Support NPM transitive dependencies in multi-platform JS target

#### Fixes

- [`KT-32531`](https://youtrack.jetbrains.com/issue/KT-32531) [Gradle/JS] Add scoped NPM dependencies
- [`KT-34832`](https://youtrack.jetbrains.com/issue/KT-34832) [Kotlin/JS] Failed build after webpack run (Karma not found)
- [`KT-35194`](https://youtrack.jetbrains.com/issue/KT-35194) Kotlin/JS: browserRun fails with "address already in use" when trying to connect to local server
- [`KT-35611`](https://youtrack.jetbrains.com/issue/KT-35611) Kotlin Gradle plugin should report `kotlin2js` plugin ID as deprecated
- [`KT-35641`](https://youtrack.jetbrains.com/issue/KT-35641) Kotlin Gradle plugin should report `kotlin-dce-js` plugin ID as deprecated
- [`KT-36410`](https://youtrack.jetbrains.com/issue/KT-36410) JS: Collect stats about IR backend usage
- [`KT-36451`](https://youtrack.jetbrains.com/issue/KT-36451) KJS Adding npm dependency breaks Webpack devserver reloading
- [`KT-37258`](https://youtrack.jetbrains.com/issue/KT-37258) Kotlin/JS + Gradle: in continuous mode kotlinNpmInstall time to time outputs "ENOENT: no such file or directory" error
- [`KT-38331`](https://youtrack.jetbrains.com/issue/KT-38331) Add an ability to control generating externals for npm deps individually
- [`KT-38485`](https://youtrack.jetbrains.com/issue/KT-38485) [Gradle, JS] Unable to configure JS compiler with string
- [`KT-38683`](https://youtrack.jetbrains.com/issue/KT-38683) Remove possibility to set NPM dependency without version
- [`KT-38990`](https://youtrack.jetbrains.com/issue/KT-38990) Support multiple range versions for NPM dependencies
- [`KT-38994`](https://youtrack.jetbrains.com/issue/KT-38994) Remove possibility to set NPM dependency with npm(org, name, version)
- [`KT-39109`](https://youtrack.jetbrains.com/issue/KT-39109) ArithmeticException: "/ by zero" caused by kotlinNodeJsSetup task with enabled gradle caching on Windows
- [`KT-39377`](https://youtrack.jetbrains.com/issue/KT-39377) Use standard source-map-loader instead of custom one
- [`KT-31669`](https://youtrack.jetbrains.com/issue/KT-31669) Gradle/JS: rise error when plugin loaded more than once
- [`KT-38109`](https://youtrack.jetbrains.com/issue/KT-38109) [Gradle, JS] Error handling on Karma launcher problems
- [`KT-38286`](https://youtrack.jetbrains.com/issue/KT-38286) [Gradle, JS] Error handling on Webpack problems
- [`KT-39210`](https://youtrack.jetbrains.com/issue/KT-39210) Kotlin/JS: with both JS and MPP modules in the same project Gradle configuration fails on `nodejs {}` and `browser {}`


### Tools. Gradle. Multiplatform

- [`KT-39184`](https://youtrack.jetbrains.com/issue/KT-39184) Support publication of Kotlin-distributed libraries with Gradle Metadata
- [`KT-39304`](https://youtrack.jetbrains.com/issue/KT-39304) Gradle import error `java.util.NoSuchElementException: Key source set foo is missing in the map` on unused source set

### Tools. Gradle. Native

- [`KT-38991`](https://youtrack.jetbrains.com/issue/KT-38991) Gradle MPP plugin: Enable parallel in-process execution for K/N compiler
- [`KT-39935`](https://youtrack.jetbrains.com/issue/KT-39935) Support overriding the `KotlinNativeCompile` task sources
- [`KT-37512`](https://youtrack.jetbrains.com/issue/KT-37512) Cocoapods Gradle plugin: Improve error logging for external tools
- [`KT-37514`](https://youtrack.jetbrains.com/issue/KT-37514) CocoaPods Gradle plugin: Support building from terminal projects for several platforms
- [`KT-38440`](https://youtrack.jetbrains.com/issue/KT-38440) Make error message about missing Podfile path for cocoapods integration actionable for a user

### Tools. J2K

- [`KT-35169`](https://youtrack.jetbrains.com/issue/KT-35169) Do not show "Inline local variable" popup during "Cleaning up code" phase of J2K
- [`KT-38004`](https://youtrack.jetbrains.com/issue/KT-38004) J2K breaks java getter call in java code
- [`KT-38450`](https://youtrack.jetbrains.com/issue/KT-38450) J2K should convert Java SAM interfaces to Kotlin fun interfaces

### Tools. JPS

- [`KT-27458`](https://youtrack.jetbrains.com/issue/KT-27458) The Kotlin standard library is not found in the module graph ... in a non-Kotlin project.
- [`KT-29552`](https://youtrack.jetbrains.com/issue/KT-29552) Project is completely rebuilt after each gradle sync.

### Tools. Scripts

- [`KT-37766`](https://youtrack.jetbrains.com/issue/KT-37766) Impossible to apply compiler plugins onto scripts with the new scripting API

### Tools. kapt

- [`KT-29355`](https://youtrack.jetbrains.com/issue/KT-29355) Provide access to default values for primary constructor properties

