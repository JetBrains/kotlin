## Changelog

## 1.3.73 - IDE plugins update

### Backend. JVM

- [`KT-39013`](https://youtrack.jetbrains.com/issue/KT-39013) 202, ASM 8: "AnalyzerException: Execution can fall off the end of the code"

### IDE. Decompiler, Indexing, Stubs

- [`KT-37896`](https://youtrack.jetbrains.com/issue/KT-37896) IAE: "Argument for @NotNull parameter 'file' of IndexTodoCacheManagerImpl.getTodoCount must not be null" through KotlinTodoSearcher.processQuery()

### IDE. Gradle Integration 

- [`KT-38037`](https://youtrack.jetbrains.com/issue/KT-38037) UnsupportedOperationException on sync gradle Kotlin project with at least two multiplatform modules

### IDE. Highlighting 

- [`KT-39590`](https://youtrack.jetbrains.com/issue/KT-39590) Turn new inference in IDE for 1.3.70 version off

### IDE. Refactorings 

- [`KT-38527`](https://youtrack.jetbrains.com/issue/KT-38527) Move nested class to upper level fails silently: MissingResourceException

### Tools.JPS

- [`KT-27458`](https://youtrack.jetbrains.com/issue/KT-27458) The Kotlin standard library is not found in the module graph ... in a non-Kotlin project.


## 1.3.72

### Compiler

- [`KT-37107`](https://youtrack.jetbrains.com/issue/KT-37107) kotlinc allows calling default constructor of class with no constructors
- [`KT-37406`](https://youtrack.jetbrains.com/issue/KT-37406) NI: "UnsupportedOperationException: no descriptor for type constructor of TypeVariable(T)" when compiling `*.gradle.kts` file

### IDE

- [`KT-37483`](https://youtrack.jetbrains.com/issue/KT-37483) Kotlin localisation
- [`KT-37629`](https://youtrack.jetbrains.com/issue/KT-37629) False positive "Unsupported [literal prefixes and suffixes]" for infix function
- [`KT-37808`](https://youtrack.jetbrains.com/issue/KT-37808) "Resolve pasted references" dialog freezes UI for 20 seconds when pasting kotlin code

### IDE. Completion

- [`KT-37144`](https://youtrack.jetbrains.com/issue/KT-37144) Completion goes into an infinite loop with Arrow 0.10.4 dependency

### IDE. Debugger

- [`KT-37767`](https://youtrack.jetbrains.com/issue/KT-37767) Debugger, NPE happens while stepping.

### IDE. Editing

- [`KT-35135`](https://youtrack.jetbrains.com/issue/KT-35135) UI freeze: not enough checkCancelled on resolve path

### IDE. Inspections and Intentions

- [`KT-37217`](https://youtrack.jetbrains.com/issue/KT-37217) Replace UseExperimental with OptIn intention removes target prefixes for annotations

### IDE. Native

- [`KT-38079`](https://youtrack.jetbrains.com/issue/KT-38079) IDEA navigates to wrong line of source code from Native stack trace

### IDE. Navigation

- [`KT-37487`](https://youtrack.jetbrains.com/issue/KT-37487) Destructuring declarations are called "destruction declarations" in UI

### Tools. Gradle. Native

- [`KT-37696`](https://youtrack.jetbrains.com/issue/KT-37696) MPP Gradle plugin: False positive parallel execution detection if build started with --continue

## 1.3.71

### Compiler

- [`KT-36095`](https://youtrack.jetbrains.com/issue/KT-36095) 201: False positive OVERLOAD_RESOLUTION_AMBIGUITY with Java `Enum.valueOf` and `Enum.values()` reference
- [`KT-37040`](https://youtrack.jetbrains.com/issue/KT-37040) 'No explicit visibility in API mode' should not be reported on enum members
- [`KT-37204`](https://youtrack.jetbrains.com/issue/KT-37204) AssertionError: "No delegated property metadata owner for" with lazy inside inline function

### Docs & Examples

- [`KT-37029`](https://youtrack.jetbrains.com/issue/KT-37029) Kotlin full stack app demo: update all involving versions to work with 1.3.70 release

### IDE

- [`KT-34759`](https://youtrack.jetbrains.com/issue/KT-34759) "PSI and index do not match" and high CPU usage when the library has `enum` with elements in quotes and `internal lazy val` in another part of code
- [`KT-37200`](https://youtrack.jetbrains.com/issue/KT-37200) StackOverflowError in LightMethodBuilder.equals when analysing Kotlin files
- [`KT-37229`](https://youtrack.jetbrains.com/issue/KT-37229) java.lang.NoSuchMethodError: 'com.intellij.psi.impl.light.LightJavaModule, com.intellij.psi.impl.light.LightJavaModule.findModule after updating kotlin plugin to 1.3.70
- [`KT-37273`](https://youtrack.jetbrains.com/issue/KT-37273) No error in editor when typing unresolved reference in super constructor lambda
- [`KT-37414`](https://youtrack.jetbrains.com/issue/KT-37414) Not all imports are added on paste if code is formatted after paste
- [`KT-37553`](https://youtrack.jetbrains.com/issue/KT-37553) Run inspections after general highlight pass

### IDE. Code Style, Formatting

- [`KT-37545`](https://youtrack.jetbrains.com/issue/KT-37545) Continuation indent for method's parameter changes in call chain

### IDE. Hints

- [`KT-37537`](https://youtrack.jetbrains.com/issue/KT-37537) IDE is missing or swallowing keystrokes when hint popups are displayed

### IDE. Inspections and Intentions

- [`KT-36478`](https://youtrack.jetbrains.com/issue/KT-36478) IDE suggests to use 'OptIn' annotation when it is not available in the used version of kotlin-stdlib
- [`KT-37294`](https://youtrack.jetbrains.com/issue/KT-37294) False positive "Unused unary operator" on negative long annotation value

### IDE. Navigation

- [`KT-36657`](https://youtrack.jetbrains.com/issue/KT-36657) KotlinFindUsagesHandler#processElementUsages always return false if options.isSearchForTextOccurrences is false

### IDE. Refactorings

- [`KT-37451`](https://youtrack.jetbrains.com/issue/KT-37451) Change of signature error: Type of parameter cannot be resolved
- [`KT-37597`](https://youtrack.jetbrains.com/issue/KT-37597) Support Suggest rename and change signature refactorings in Kotlin for IDEA 2020.1

### IDE. Run Configurations

- [`KT-36781`](https://youtrack.jetbrains.com/issue/KT-36781) Override ConfigurationFactory::getId method in Kotlin plugin to avoid problems with localizations

### JavaScript

- [`KT-37386`](https://youtrack.jetbrains.com/issue/KT-37386) Incorrect JS generated by the compiler: function is erased by the function parameter

### Tools. Gradle. JS

- [`KT-36196`](https://youtrack.jetbrains.com/issue/KT-36196) Investigate performance problems while resolving in projects with npm dependencies

### Tools. Gradle. Multiplatform

- [`KT-37264`](https://youtrack.jetbrains.com/issue/KT-37264) In intermediate common source sets, internals are not visible from their dependsOn source sets during Gradle build

### Tools. Gradle. Native

- [`KT-37565`](https://youtrack.jetbrains.com/issue/KT-37565) MPP plugin: Forbid parallel in-process execution of the Kotlin/Native compiler

### Tools. kapt

- [`KT-37241`](https://youtrack.jetbrains.com/issue/KT-37241) Kapt: Classpath entry points to a non-existent location: ...build/intermediates/javac/debug/classes...

## 1.3.70

### Compiler

#### New Features

- [`KT-34648`](https://youtrack.jetbrains.com/issue/KT-34648) Support custom messages for @RequiresOptIn-marked annotations

#### Performance Improvements

- [`KT-14513`](https://youtrack.jetbrains.com/issue/KT-14513) Suboptimal compilation of lazy delegated properties with inline getValue

#### Fixes

- [`KT-19234`](https://youtrack.jetbrains.com/issue/KT-19234) Improve "Supertypes of the following classes cannot be resolved" diagnostic
- [`KT-21178`](https://youtrack.jetbrains.com/issue/KT-21178) Prohibit access of protected members inside public inline members
- [`KT-24461`](https://youtrack.jetbrains.com/issue/KT-24461) Expect interface with suspend function with default arguments causes runtime error
- [`KT-25514`](https://youtrack.jetbrains.com/issue/KT-25514) Support usage of function reference with vararg where function of array is expected in new inference
- [`KT-26435`](https://youtrack.jetbrains.com/issue/KT-26435) Bad frame merge after inline
- [`KT-27825`](https://youtrack.jetbrains.com/issue/KT-27825) Gradually prohibit non-abstract classes containing abstract members invisible from that classes (internal/package-private)
- [`KT-27999`](https://youtrack.jetbrains.com/issue/KT-27999) Generic type is fixed too early for lambda arguments
- [`KT-28940`](https://youtrack.jetbrains.com/issue/KT-28940) Concurrency issue for lazy values with the post-computation phase
- [`KT-29242`](https://youtrack.jetbrains.com/issue/KT-29242) Conditional with generic type Nothing inside inline function throws `java.lang.VerifyError: Bad return type`
- [`KT-30244`](https://youtrack.jetbrains.com/issue/KT-30244) Unable to infer common return type for two postponed arguments
- [`KT-30245`](https://youtrack.jetbrains.com/issue/KT-30245) Wrong type is inferred for lambda if it has expected type with an extension receiver
- [`KT-30277`](https://youtrack.jetbrains.com/issue/KT-30277) Relax the "no reflection found in class path" warning for KType and related API
- [`KT-30744`](https://youtrack.jetbrains.com/issue/KT-30744) Invoking Interface Static Method from Extension method generates incorrect jvm bytecode
- [`KT-30953`](https://youtrack.jetbrains.com/issue/KT-30953) Missing unresolved if callable reference is used in the place in which common super type is computing
- [`KT-31227`](https://youtrack.jetbrains.com/issue/KT-31227) Prohibit using array based on non-reified type parameters as reified type arguments on JVM
- [`KT-31242`](https://youtrack.jetbrains.com/issue/KT-31242) "Can't find enclosing method" proguard compilation exception with inline and crossinline
- [`KT-31411`](https://youtrack.jetbrains.com/issue/KT-31411) Support mode of compiler where it analyses source-set as platform one, but produces only metadata for that specific source-set
- [`KT-31653`](https://youtrack.jetbrains.com/issue/KT-31653) Incorrect transformation of the try-catch cover when inlining
- [`KT-31923`](https://youtrack.jetbrains.com/issue/KT-31923) Outer finally block inserted before return instruction is not excluded from catch interval of inner try (without finally) block
- [`KT-31975`](https://youtrack.jetbrains.com/issue/KT-31975) No diagnostic on error type
- [`KT-32106`](https://youtrack.jetbrains.com/issue/KT-32106) New type inference: IDE shows error but the code compiles succesfully
- [`KT-32138`](https://youtrack.jetbrains.com/issue/KT-32138) New type inference: Invoking type-aliased extension function red in IDE, but compiles
- [`KT-32168`](https://youtrack.jetbrains.com/issue/KT-32168) Problem in IDE with new type inference and delegate provider
- [`KT-32243`](https://youtrack.jetbrains.com/issue/KT-32243) New type inference: Type mistmatch in collection type usage
- [`KT-32345`](https://youtrack.jetbrains.com/issue/KT-32345) New type inference: Error when using helper method to create delegate provider
- [`KT-32372`](https://youtrack.jetbrains.com/issue/KT-32372) Type inference errors in IDE
- [`KT-32415`](https://youtrack.jetbrains.com/issue/KT-32415) Type mismatch on argument of super constructor of inner class call
- [`KT-32423`](https://youtrack.jetbrains.com/issue/KT-32423) New type inference: IllegalStateException: Error type encountered: org.jetbrains.kotlin.types.ErrorUtils$UninferredParameterTypeConstructor@211a538e (ErrorType)
- [`KT-32435`](https://youtrack.jetbrains.com/issue/KT-32435) New inference preserves platform types while old inference can substitute them with the nullable result type
- [`KT-32456`](https://youtrack.jetbrains.com/issue/KT-32456) New type inference: "IllegalStateException: Error type encountered" when adding emptyList to mutableList
- [`KT-32499`](https://youtrack.jetbrains.com/issue/KT-32499) Kotlin/JS 1.3.40 - new type inference with toTypedArray() failure
- [`KT-32742`](https://youtrack.jetbrains.com/issue/KT-32742) Gradle/JS "Unresolved Reference" when accessing setting field of Dynamic object w/ React
- [`KT-32818`](https://youtrack.jetbrains.com/issue/KT-32818) Type inference failed with elvis operator
- [`KT-32862`](https://youtrack.jetbrains.com/issue/KT-32862) New type inference: Compilation error "IllegalArgumentException: ClassicTypeSystemContextForCS couldn't handle" with overloaded generic extension function reference passed as parameter
- [`KT-33033`](https://youtrack.jetbrains.com/issue/KT-33033) New type inference: Nothing incorrectly inferred as return type when null passed to generic function with expression if statement body
- [`KT-33197`](https://youtrack.jetbrains.com/issue/KT-33197) Expression with branch resolving to List<…> ultimately resolves to MutableList<…>
- [`KT-33263`](https://youtrack.jetbrains.com/issue/KT-33263) "IllegalStateException: Type variable TypeVariable(T) should not be fixed!" with generic extension function and in variance
- [`KT-33542`](https://youtrack.jetbrains.com/issue/KT-33542) Compilation failed with "AssertionError: Suspend functions may be called either as suspension points or from another suspend function"
- [`KT-33544`](https://youtrack.jetbrains.com/issue/KT-33544) "UnsupportedOperationException: no descriptor for type constructor of TypeVariable(R)?" with BuilderInference and elvis operator
- [`KT-33592`](https://youtrack.jetbrains.com/issue/KT-33592) New type inference: Missed error in IDE — Unsupported [Collection literals outside of annotations]
- [`KT-33932`](https://youtrack.jetbrains.com/issue/KT-33932) Compiler fails when it encounters inaccessible classes in javac integration mode
- [`KT-34029`](https://youtrack.jetbrains.com/issue/KT-34029) StackOverflowError for access to nested object inheriting from containing generic class at `org.jetbrains.kotlin.descriptors.impl.LazySubstitutingClassDescriptor.getTypeConstructor`
- [`KT-34060`](https://youtrack.jetbrains.com/issue/KT-34060) UNUSED_PARAMETER is not reported on unused parameters of non-operator getValue/setValue/prodiveDelegate functions
- [`KT-34282`](https://youtrack.jetbrains.com/issue/KT-34282) Missing diagnostic of unresolved for callable references with overload resolution ambiguity
- [`KT-34391`](https://youtrack.jetbrains.com/issue/KT-34391) New type inference: False negative EXPERIMENTAL_API_USAGE_ERROR with callable reference
- [`KT-34395`](https://youtrack.jetbrains.com/issue/KT-34395) KtWhenConditionInRange.isNegated() doesn't work
- [`KT-34500`](https://youtrack.jetbrains.com/issue/KT-34500) CompilationException when loop range is DoubleArray and loop parameter is casted to super-type (e.g. Any, Number, etc.)
- [`KT-34647`](https://youtrack.jetbrains.com/issue/KT-34647) Gradually rename experimentality annotations
- [`KT-34649`](https://youtrack.jetbrains.com/issue/KT-34649) Deprecate `-Xexperimental` flag
- [`KT-34779`](https://youtrack.jetbrains.com/issue/KT-34779) JVM: "get()" is not invoked in optimized "for" loop over CharSequence.withIndex() with unused variable ("_") for the element in destructuring declaration
- [`KT-34786`](https://youtrack.jetbrains.com/issue/KT-34786) Flaky type inference for lambda expressions
- [`KT-34820`](https://youtrack.jetbrains.com/issue/KT-34820) New type inference: Red code when expanding type-aliased extension function in LHS position of elvis
- [`KT-34888`](https://youtrack.jetbrains.com/issue/KT-34888) Kotlin REPL ignores compilation errors in class declaration
- [`KT-35035`](https://youtrack.jetbrains.com/issue/KT-35035) Incorrect state-machine generated for suspend lambda inside inline lambda
- [`KT-35101`](https://youtrack.jetbrains.com/issue/KT-35101) "AssertionError: Mapping ranges should be presented in inline lambda" with a callable reference argument to inline lambda
- [`KT-35168`](https://youtrack.jetbrains.com/issue/KT-35168) New type inference: "UninitializedPropertyAccessException: lateinit property subResolvedAtoms has not been initialized"
- [`KT-35172`](https://youtrack.jetbrains.com/issue/KT-35172) New type inference: False positive type mismatch if nullable type after elvis and safe call inside lambda is returning (expected type is specified explicitly)
- [`KT-35224`](https://youtrack.jetbrains.com/issue/KT-35224) New type inference: Java call candidate with varargs as Array<something> isn't present if SAM type was used in this call
- [`KT-35262`](https://youtrack.jetbrains.com/issue/KT-35262) Suspend function with Unit return type returns non-unit value if it is derived from function with non-unit return type
- [`KT-35426`](https://youtrack.jetbrains.com/issue/KT-35426) `IncompatibleClassChangeError: Method 'int java.lang.Object.hashCode()' must be Methodref constant` when invoking on super with explicit  generic type
- [`KT-35843`](https://youtrack.jetbrains.com/issue/KT-35843) Emit type annotations in JVM bytecode with target 1.8+ on basic constructions
- [`KT-36297`](https://youtrack.jetbrains.com/issue/KT-36297) New type inference: ClassNotFoundException: compiler emits reference to nonexisting class for code with nested inline lambdas
- [`KT-36719`](https://youtrack.jetbrains.com/issue/KT-36719) Enable new inference in IDE since 1.3.70

### Docs & Examples

- [`KT-31118`](https://youtrack.jetbrains.com/issue/KT-31118) Provide missing documentation for StringBuilder members

### IDE

#### New Features

- [`KT-27496`](https://youtrack.jetbrains.com/issue/KT-27496) Color Scheme: allow changing style for suspend function calls
- [`KT-30806`](https://youtrack.jetbrains.com/issue/KT-30806) Add IntelliJ Color Scheme rules for property declarations
- [`KT-34303`](https://youtrack.jetbrains.com/issue/KT-34303) IDE should suggest to import an extension iterator function when using for loop with a range
- [`KT-34567`](https://youtrack.jetbrains.com/issue/KT-34567) Feature: Auto add val keyword on typing data/inline class ctor parameters
- [`KT-34667`](https://youtrack.jetbrains.com/issue/KT-34667) Add auto-import quickfix for overloaded generic function

#### Performance Improvements

- [`KT-30726`](https://youtrack.jetbrains.com/issue/KT-30726) Editor is laggy if the code below a current line has unresolved reference
- [`KT-30863`](https://youtrack.jetbrains.com/issue/KT-30863) IDE freeze on editing with "Add unambiguous imports on the fly" turned on
- [`KT-32868`](https://youtrack.jetbrains.com/issue/KT-32868) Provide incremental analysis of file when it is applicable
- [`KT-33250`](https://youtrack.jetbrains.com/issue/KT-33250) KtLightClassForSourceDeclaration.isFinal() can be very slow (with implications for class inheritor search)
- [`KT-33905`](https://youtrack.jetbrains.com/issue/KT-33905) Optimize imports under reasonable progress
- [`KT-33939`](https://youtrack.jetbrains.com/issue/KT-33939) Copy action leads to freezes
- [`KT-34956`](https://youtrack.jetbrains.com/issue/KT-34956) UI Freeze: PlainTextPasteImportResolver
- [`KT-35121`](https://youtrack.jetbrains.com/issue/KT-35121) Add support for KtSecondaryConstructors into incremental analysis
- [`KT-35189`](https://youtrack.jetbrains.com/issue/KT-35189) Support incremental analysis of comment and kdoc
- [`KT-35590`](https://youtrack.jetbrains.com/issue/KT-35590) UI freeze in kotlin.idea.core.script.ScriptConfigurationMemoryCache when editing file

#### Fixes

- [`KT-10478`](https://youtrack.jetbrains.com/issue/KT-10478) Move-statement doesn't work for methods with single-expression body and lambda as returning type
- [`KT-13344`](https://youtrack.jetbrains.com/issue/KT-13344) Reduce visual distraction of val keyword
- [`KT-14758`](https://youtrack.jetbrains.com/issue/KT-14758) Move statement up shouldn't move top level declarations above package and import directives
- [`KT-23305`](https://youtrack.jetbrains.com/issue/KT-23305) We should be able to see platform-specific errors in common module
- [`KT-24399`](https://youtrack.jetbrains.com/issue/KT-24399) No scrollbar in Kotlin compiler settings
- [`KT-27806`](https://youtrack.jetbrains.com/issue/KT-27806) UAST: @Deprecated(level=DeprecationLevel.HIDDEN) makes method disappear
- [`KT-28708`](https://youtrack.jetbrains.com/issue/KT-28708) Java IDE fails to understand @JvmDefault on properties from binaries
- [`KT-30489`](https://youtrack.jetbrains.com/issue/KT-30489) Kotlin functions are represented in UAST as UAnnotationMethods
- [`KT-31037`](https://youtrack.jetbrains.com/issue/KT-31037) Lambda expression default parameter 'it' sometimes is not highlighted in a call chain
- [`KT-31365`](https://youtrack.jetbrains.com/issue/KT-31365) IDE does not resolve references to stdlib symbols in certain packages (kotlin.jvm) when using OSGi bundle
- [`KT-32031`](https://youtrack.jetbrains.com/issue/KT-32031) UAST: Method body missing for suspend functions
- [`KT-32540`](https://youtrack.jetbrains.com/issue/KT-32540) Ultra light class support for compiler plugins
- [`KT-33820`](https://youtrack.jetbrains.com/issue/KT-33820) Stop using `com.intellij.codeInsight.AnnotationUtil#isJetbrainsAnnotation`
- [`KT-33846`](https://youtrack.jetbrains.com/issue/KT-33846) Stop using `com.intellij.openapi.vfs.newvfs.BulkFileListener.Adapter`
- [`KT-33888`](https://youtrack.jetbrains.com/issue/KT-33888) Bad indentation when copy-paste to trimIndent()
- [`KT-34081`](https://youtrack.jetbrains.com/issue/KT-34081) Kotlin constants used in Java annotation attributes trigger "Attribute value must be constant" error
- [`KT-34316`](https://youtrack.jetbrains.com/issue/KT-34316) UAST: reified methods no longer visible in UAST
- [`KT-34337`](https://youtrack.jetbrains.com/issue/KT-34337) Descriptors Leak in UltraLightClasses
- [`KT-34379`](https://youtrack.jetbrains.com/issue/KT-34379) "Implement members" with unspecified type argument: "AssertionError: 2 declarations in override fun"
- [`KT-34785`](https://youtrack.jetbrains.com/issue/KT-34785) Enter handler: do not add 'trimIndent()' in const
- [`KT-34914`](https://youtrack.jetbrains.com/issue/KT-34914) Analysis sometimes isn't rerun until an out of code block change
- [`KT-35222`](https://youtrack.jetbrains.com/issue/KT-35222) SQL language is not injected to String array attribute of Java annotation
- [`KT-35266`](https://youtrack.jetbrains.com/issue/KT-35266) Kotlin-specific setting "Optimize imports on the fly" is useless
- [`KT-35454`](https://youtrack.jetbrains.com/issue/KT-35454) Weird implementation of KtUltraLightFieldImpl.isEquivalentTo
- [`KT-35673`](https://youtrack.jetbrains.com/issue/KT-35673) ClassCastException on destructuring declaration with annotation
- [`KT-36008`](https://youtrack.jetbrains.com/issue/KT-36008) IDEA 201: NSME: "com.intellij.openapi.progress.util.ProgressIndicatorUtils.awaitWithCheckCanceled(Future)" at org.jetbrains.kotlin.idea.util.ProgressIndicatorUtils.awaitWithCheckCanceled()

### IDE. Code Style, Formatting

#### New Features

- [`KT-35088`](https://youtrack.jetbrains.com/issue/KT-35088) Insert empty line between a declaration and declaration with comment
- [`KT-35106`](https://youtrack.jetbrains.com/issue/KT-35106) Insert empty line between a declaration and declaration with annotation

#### Fixes

- [`KT-4194`](https://youtrack.jetbrains.com/issue/KT-4194) Code formatter should not move end of line comment after `if` condition to the next line
- [`KT-12490`](https://youtrack.jetbrains.com/issue/KT-12490) Formatter inserts empty line between single-line declarations in presence of comment
- [`KT-22273`](https://youtrack.jetbrains.com/issue/KT-22273) Labeled statements are formatted incorrectly
- [`KT-22362`](https://youtrack.jetbrains.com/issue/KT-22362) Formatter breaks up infix function used in elvis operator
- [`KT-23811`](https://youtrack.jetbrains.com/issue/KT-23811) Formatter: Constructor parameters are joined with previous line if prefixed with an annotation
- [`KT-23929`](https://youtrack.jetbrains.com/issue/KT-23929) Formatter: chained method calls: "Chop down if long" setting is ignored
- [`KT-23957`](https://youtrack.jetbrains.com/issue/KT-23957) Formatter tears comments away from file annotations
- [`KT-30393`](https://youtrack.jetbrains.com/issue/KT-30393) Remove unnecessary whitespaces between property accessor and its parameter list in formatter
- [`KT-31881`](https://youtrack.jetbrains.com/issue/KT-31881) Redundant indent for single-line comments inside lamdba
- [`KT-32277`](https://youtrack.jetbrains.com/issue/KT-32277) Space before by delegate keyword on property is not formatted
- [`KT-32324`](https://youtrack.jetbrains.com/issue/KT-32324) Formatter doesn't insert space after safe cast operator `as?`
- [`KT-33553`](https://youtrack.jetbrains.com/issue/KT-33553) Formater does not wrap function chained expression body despite "chained function calls" settings
- [`KT-34049`](https://youtrack.jetbrains.com/issue/KT-34049) Formatter breaks string inside template expression with elvis operator
- [`KT-35093`](https://youtrack.jetbrains.com/issue/KT-35093) Formatter inserts empty line between single-line declarations in presence of annotation
- [`KT-35199`](https://youtrack.jetbrains.com/issue/KT-35199) Wrong formatting for lambdas in chain calls

### IDE. Completion

#### Fixes

- [`KT-15286`](https://youtrack.jetbrains.com/issue/KT-15286) Support import auto-completion for extension functions declared in objects
- [`KT-23026`](https://youtrack.jetbrains.com/issue/KT-23026) Code completion: Incorrect `const` in class declaration line
- [`KT-23834`](https://youtrack.jetbrains.com/issue/KT-23834) Code completion and auto import do not suggest extension that differs from member only in type parameter
- [`KT-25732`](https://youtrack.jetbrains.com/issue/KT-25732) `null` keyword should have priority in completion sort
- [`KT-29840`](https://youtrack.jetbrains.com/issue/KT-29840) `const` is suggested inside the class body, despite it's illegal
- [`KT-29926`](https://youtrack.jetbrains.com/issue/KT-29926) Suggest lambda parameter names in IDE to improve DSL adoption
- [`KT-31762`](https://youtrack.jetbrains.com/issue/KT-31762) Completion: Parameter name is suggested instead of enum entry in entry constructor
- [`KT-32615`](https://youtrack.jetbrains.com/issue/KT-32615) PIEAE for smart completion of anonymous function with importing name inside of function
- [`KT-33979`](https://youtrack.jetbrains.com/issue/KT-33979) No completion for functions from nested objects
- [`KT-34150`](https://youtrack.jetbrains.com/issue/KT-34150) No completion for object methods that override something
- [`KT-34386`](https://youtrack.jetbrains.com/issue/KT-34386) Typo in Kotlin arg postfix completion
- [`KT-34414`](https://youtrack.jetbrains.com/issue/KT-34414) Completion works differently for suspend and regular lambda functions
- [`KT-34644`](https://youtrack.jetbrains.com/issue/KT-34644) Code completion list sorting: do not put method before "return" keyword
- [`KT-35042`](https://youtrack.jetbrains.com/issue/KT-35042) Selecting completion variant works differently for suspend and regular lambda parameter
- [`KT-36306`](https://youtrack.jetbrains.com/issue/KT-36306) Code completion inlines content of FQN class if completion called in string

### IDE. Debugger

#### Fixes

- [`KT-12242`](https://youtrack.jetbrains.com/issue/KT-12242) Breakpoint in a class is not hit if the class was first accessed in Evaluate Expression
- [`KT-16277`](https://youtrack.jetbrains.com/issue/KT-16277) Can't set breakpoint for object construction
- [`KT-20342`](https://youtrack.jetbrains.com/issue/KT-20342) Step Over jumps to wrong position (KotlinUFile)
- [`KT-30909`](https://youtrack.jetbrains.com/issue/KT-30909) "Kotlin variables" button looks inconsistent with panel style
- [`KT-32704`](https://youtrack.jetbrains.com/issue/KT-32704) ISE "Descriptor can be left only if it is last" on calling function with expression body inside Evaluate Expression window
- [`KT-32736`](https://youtrack.jetbrains.com/issue/KT-32736) Evaluate Expression on statement makes error or shows nothing
- [`KT-32741`](https://youtrack.jetbrains.com/issue/KT-32741) "Anonymous functions with names are prohibited" on evaluating functions in Expression mode
- [`KT-33303`](https://youtrack.jetbrains.com/issue/KT-33303) "Smart step into" doesn't work for library declarations
- [`KT-33304`](https://youtrack.jetbrains.com/issue/KT-33304) Can't put a breakpoint to the first line in file
- [`KT-33728`](https://youtrack.jetbrains.com/issue/KT-33728) Smart Step Into doesn't work for @InlineOnly functions
- [`KT-35316`](https://youtrack.jetbrains.com/issue/KT-35316) IndexNotReadyException on function breakpoint

### IDE. Folding

- [`KT-6316`](https://youtrack.jetbrains.com/issue/KT-6316) Folding of multiline functions which don't have curly braces (expression-body functions)

### IDE. Gradle Integration

- [`KT-35442`](https://youtrack.jetbrains.com/issue/KT-35442) KotlinMPPGradleModelBuilder shows warnings on import because it can't find  a not existing directory

### IDE. Gradle. Script

- [`KT-31976`](https://youtrack.jetbrains.com/issue/KT-31976) Adding a space in build.gradle.kts leads to 'Gradle projects need to be imported' notification
- [`KT-34441`](https://youtrack.jetbrains.com/issue/KT-34441) *.gradle.kts: load all scripts configuration at project import
- [`KT-34442`](https://youtrack.jetbrains.com/issue/KT-34442) *.gradle.kts: avoid just-in-case script configuration request to Gradle
- [`KT-34530`](https://youtrack.jetbrains.com/issue/KT-34530) Equal duplicate script definitions are listed three times in Preferences
- [`KT-34740`](https://youtrack.jetbrains.com/issue/KT-34740) Implement completion for implicit receivers in scripts with new scripting API
- [`KT-34795`](https://youtrack.jetbrains.com/issue/KT-34795) Gradle Kotlin DSL new project template: don't use `setUrl` syntax in `settings.gradle.kts` `pluginManagement` block
- [`KT-35096`](https://youtrack.jetbrains.com/issue/KT-35096) Duplicated “Kotlin Script” definition for Gradle/Kotlin projects
- [`KT-35149`](https://youtrack.jetbrains.com/issue/KT-35149) build.graldle.kts settings importing: configuration for buildSrc/prepare-deps/build.gradle.kts not loaded
- [`KT-35205`](https://youtrack.jetbrains.com/issue/KT-35205) *.gradle.kts: avoid just-in-case script configuration request to Gradle while loading from FS
- [`KT-35563`](https://youtrack.jetbrains.com/issue/KT-35563) Track script modifications between IDE restarts

### IDE. Hints. Parameter Info

- [`KT-34992`](https://youtrack.jetbrains.com/issue/KT-34992) UI Freeze: Show parameter info leads to freezes

### IDE. Inspections and Intentions

#### New Features

- [`KT-8478`](https://youtrack.jetbrains.com/issue/KT-8478) Make 'Add parameter to function' quick fix work to parameters other than last
- [`KT-12073`](https://youtrack.jetbrains.com/issue/KT-12073) Report IDE inspection warning on pointless unary operators on numbers
- [`KT-18536`](https://youtrack.jetbrains.com/issue/KT-18536) Provide proper quick fix for accidental override error
- [`KT-34218`](https://youtrack.jetbrains.com/issue/KT-34218) Merge 'else if' intention
- [`KT-36018`](https://youtrack.jetbrains.com/issue/KT-36018) 'Missing visibility' and 'missing explicit return type' compiler and IDE diagnostics for explicit API mode

#### Fixes

- [`KT-17659`](https://youtrack.jetbrains.com/issue/KT-17659) Cannot access internal Kotlin declaration from Java test code within the same module
- [`KT-25271`](https://youtrack.jetbrains.com/issue/KT-25271) "Remove redundant '.let' call" may introduce expression side effects several times
- [`KT-29737`](https://youtrack.jetbrains.com/issue/KT-29737) "Make internal/private/protected" intention works for either `expect` or `actual` side
- [`KT-31967`](https://youtrack.jetbrains.com/issue/KT-31967) Typo in inspection name: "'+=' create new list under the hood"
- [`KT-32582`](https://youtrack.jetbrains.com/issue/KT-32582) Ambiguous message for [AMBIGUOUS_ACTUALS] error (master)
- [`KT-33109`](https://youtrack.jetbrains.com/issue/KT-33109) "Add constructor parameters" quick fix should add default parameters from super class
- [`KT-33123`](https://youtrack.jetbrains.com/issue/KT-33123) False positive "Redundant qualifier name" with inner class as constructor parameter for outer
- [`KT-33297`](https://youtrack.jetbrains.com/issue/KT-33297) Improve parameter name in `Add parameter to constructor` quick fix
- [`KT-33526`](https://youtrack.jetbrains.com/issue/KT-33526) False positive "Redundant qualifier name" with enum constant initialized with companion object field
- [`KT-33580`](https://youtrack.jetbrains.com/issue/KT-33580) False positive "Redundant visibility modifier" overriding property with protected set visibility
- [`KT-33771`](https://youtrack.jetbrains.com/issue/KT-33771) False positive "Redundant Companion reference" with Java synthetic property and same-named object property
- [`KT-33796`](https://youtrack.jetbrains.com/issue/KT-33796) INVISIBLE_SETTER: quick fix "Make '<set-attribute>' public" does not remove redundant setter
- [`KT-33902`](https://youtrack.jetbrains.com/issue/KT-33902) False positive for "Remove explicit type specification" with type alias as return type
- [`KT-33933`](https://youtrack.jetbrains.com/issue/KT-33933) "Create expect" quick fix generates the declaration in a default source set even if an alternative is chosen
- [`KT-34078`](https://youtrack.jetbrains.com/issue/KT-34078) ReplaceWith does not work if replacement is fun in companion object
- [`KT-34297`](https://youtrack.jetbrains.com/issue/KT-34297) "Add 'replaceWith' argument" inserts positional instead of named argument
- [`KT-34325`](https://youtrack.jetbrains.com/issue/KT-34325) "Control flow with empty body" inspection should not report ifs with comments
- [`KT-34411`](https://youtrack.jetbrains.com/issue/KT-34411) Create expect/actual quick fix: focus is lost in the editor (193 IDEA)
- [`KT-34432`](https://youtrack.jetbrains.com/issue/KT-34432) Replace with safe call intention inserts redundant elvis operator
- [`KT-34603`](https://youtrack.jetbrains.com/issue/KT-34603) "Remove redundant '.let' call" false negative for reference expression
- [`KT-34694`](https://youtrack.jetbrains.com/issue/KT-34694) "Terminate preceding call with semicolon" breaks lambda formatting
- [`KT-34784`](https://youtrack.jetbrains.com/issue/KT-34784) "Indent raw string" intention: do not suggest in const
- [`KT-34894`](https://youtrack.jetbrains.com/issue/KT-34894) Action "Add not-null asserted (!!) call" doesn't fix error for properties with omitted `this`
- [`KT-35022`](https://youtrack.jetbrains.com/issue/KT-35022) Quickfix "change to var" doesn't remove `const` modifier
- [`KT-35208`](https://youtrack.jetbrains.com/issue/KT-35208) NPE from PerModulePackageCacheService
- [`KT-35242`](https://youtrack.jetbrains.com/issue/KT-35242) Text-range based inspection range shifts wrongly due to incremental analysis of whitespace and comments
- [`KT-35288`](https://youtrack.jetbrains.com/issue/KT-35288) False positive "Remove braces from 'when' entry" in 'when' expression which returns lambda
- [`KT-35837`](https://youtrack.jetbrains.com/issue/KT-35837) Editing Introduce import alias does not affect KDoc
- [`KT-36020`](https://youtrack.jetbrains.com/issue/KT-36020) Intention 'Add public modifier' is not available for highlighted declaration in explicit api mode
- [`KT-36021`](https://youtrack.jetbrains.com/issue/KT-36021) KDoc shouldn't be highlighted on 'visibility must be specified' warning in explicit api mode
- [`KT-36307`](https://youtrack.jetbrains.com/issue/KT-36307) False positive "Remove redundant '.let' call" for nested lambda change scope reference

### IDE. Multiplatform

- [`KT-33321`](https://youtrack.jetbrains.com/issue/KT-33321) In IDE, actuals of intermediate test source set are incorrectly matched against parent main source-set (not test one)

### IDE. Navigation

- [`KT-30736`](https://youtrack.jetbrains.com/issue/KT-30736) References for import alias from kotlin library not found using ReferencesSearch.search
- [`KT-35310`](https://youtrack.jetbrains.com/issue/KT-35310) PIEAE: "During querying provider Icon preview" at ClsJavaCodeReferenceElementImpl.multiResolve() on navigation to Kotlin declaration

### IDE. Refactorings

#### Performance Improvements

- [`KT-24122`](https://youtrack.jetbrains.com/issue/KT-24122) Long pauses with "removing redundant imports" dialog on rename refactoring for IDEA Kotlin Plugin

#### Fixes

- [`KT-18191`](https://youtrack.jetbrains.com/issue/KT-18191) Refactor / Copy multiple files/classes: package statements are not updated
- [`KT-18539`](https://youtrack.jetbrains.com/issue/KT-18539) Default implement fun/property text shouldn't contain scary comment
- [`KT-28607`](https://youtrack.jetbrains.com/issue/KT-28607) Extract/Introduce variable fails if caret is just after expression
- [`KT-32514`](https://youtrack.jetbrains.com/issue/KT-32514) Moving file in with 'search for references' inlines contents in referred source code
- [`KT-32601`](https://youtrack.jetbrains.com/issue/KT-32601) Introduce variable in unformatted lambda causes PIEAE
- [`KT-32999`](https://youtrack.jetbrains.com/issue/KT-32999) Renaming parameter does not rename usage in named argument in a different file
- [`KT-33372`](https://youtrack.jetbrains.com/issue/KT-33372) Rename resource cause its content to be replaced
- [`KT-34415`](https://youtrack.jetbrains.com/issue/KT-34415) Refactor > Change signature of an overridden actual function from a platform class: "org.jetbrains.kotlin.descriptors.InvalidModuleException: Accessing invalid module descriptor"
- [`KT-34419`](https://youtrack.jetbrains.com/issue/KT-34419) Refactor > Change signature > add a function parameter: "org.jetbrains.kotlin.descriptors.InvalidModuleException: Accessing invalid module descriptor"
- [`KT-34459`](https://youtrack.jetbrains.com/issue/KT-34459) Change method signature with unresolved lambda type leads to error
- [`KT-34971`](https://youtrack.jetbrains.com/issue/KT-34971) Refactor / Copy for declarations from different sources throws IAE: "unexpected element" at CopyFilesOrDirectoriesHandler.getCommonParentDirectory()
- [`KT-35689`](https://youtrack.jetbrains.com/issue/KT-35689) Change Signature:  "InvalidModuleException: Accessing invalid module descriptor" on attempt to change receiver type of a member abstract function
- [`KT-35903`](https://youtrack.jetbrains.com/issue/KT-35903) Change Signature refactoring crashes by InvalidModuleException on simplest examples

### IDE. Run Configurations

- [`KT-34632`](https://youtrack.jetbrains.com/issue/KT-34632) Kotlin/JS: Can not run single test method
- [`KT-35038`](https://youtrack.jetbrains.com/issue/KT-35038) Running a test in a multi-module MPP project via IntelliJ Idea gutter action produces incorrect Gradle Run configuration

### IDE. Script

- [`KT-34688`](https://youtrack.jetbrains.com/issue/KT-34688) Many "scanning dependencies for script definitions progresses at the same time
- [`KT-35886`](https://youtrack.jetbrains.com/issue/KT-35886) UI Freeze: ScriptClassRootsCache.hasNotCachedRoots 25 seconds

### IDE. Tests Support

- [`KT-33787`](https://youtrack.jetbrains.com/issue/KT-33787) IDE tests: Not able to run single test using JUnit

### IDE. Wizards

#### New Features

- [`KT-36043`](https://youtrack.jetbrains.com/issue/KT-36043) Gradle, JS: Add continuous-mode run configuration in New Project Wizard templates

#### Fixes

- [`KT-35584`](https://youtrack.jetbrains.com/issue/KT-35584) New Project wizard: module names restrictions are too strong with no reason
- [`KT-35690`](https://youtrack.jetbrains.com/issue/KT-35690) New Project wizard: artifact and group fields are mixed up
- [`KT-35694`](https://youtrack.jetbrains.com/issue/KT-35694) New Project wizard creates settings.gradle.kts even for Groovy DSL
- [`KT-35695`](https://youtrack.jetbrains.com/issue/KT-35695) New Project wizard uses `kotlin ()` call for dependencies in non-MPP Groovy-DSL JVM project
- [`KT-35710`](https://youtrack.jetbrains.com/issue/KT-35710) New Project wizard creates non-Java source/resource roots for Kotlin/JVM JPS
- [`KT-35711`](https://youtrack.jetbrains.com/issue/KT-35711) New Project wizard: Maven: "Kotlin Test framework" template adds wrong dependency
- [`KT-35712`](https://youtrack.jetbrains.com/issue/KT-35712) New Project wizard: source root templates: switching focus from root reverts custom settings to default
- [`KT-35713`](https://youtrack.jetbrains.com/issue/KT-35713) New Project wizard: custom settings for project name, artifact and group ID are reverted to default on Previous/Next
- [`KT-35715`](https://youtrack.jetbrains.com/issue/KT-35715) New Project wizard: Maven: custom repository required for template (ktor) is not added to pom.xml
- [`KT-35718`](https://youtrack.jetbrains.com/issue/KT-35718) New Project wizard: Gradle: ktor: not existing repository is added
- [`KT-35719`](https://youtrack.jetbrains.com/issue/KT-35719) New Project wizard: Multiplatform library: entryPoint specifies not existing class name
- [`KT-35720`](https://youtrack.jetbrains.com/issue/KT-35720) New Project wizard: Multiplatform library: Groovy DSL: improve the script for nativeTarget calculation

### JS. Tools

- [`KT-35198`](https://youtrack.jetbrains.com/issue/KT-35198) Kotlin/JS: with references to NPM/.kjsm library DCE produces invalid resulting JavaScript
- [`KT-36349`](https://youtrack.jetbrains.com/issue/KT-36349) KJS: JS DCE use file's timestamps to compare files. It conflicts with gradle configuration 'preserveFileTimestamps = false'.

### JavaScript

- [`KT-30517`](https://youtrack.jetbrains.com/issue/KT-30517) KJS generates wrong call for secondary constructor w/ default argument when class inherited by object expression
- [`KT-33149`](https://youtrack.jetbrains.com/issue/KT-33149) Lambda is not a subtype of `Function<*>`
- [`KT-33327`](https://youtrack.jetbrains.com/issue/KT-33327) JS IR backend works incorrectly when function and property have the same name
- [`KT-33334`](https://youtrack.jetbrains.com/issue/KT-33334) JS IR backend can't access private var from internal inline function

### Libraries

#### New Features

- [`KT-7657`](https://youtrack.jetbrains.com/issue/KT-7657) `scan()` functions for Sequences and Iterable
- [`KT-15363`](https://youtrack.jetbrains.com/issue/KT-15363) Builder functions for basic containers
- [`KT-21327`](https://youtrack.jetbrains.com/issue/KT-21327) Add Deque & ArrayDeque to Kotlin standard library
- [`KT-33069`](https://youtrack.jetbrains.com/issue/KT-33069) StringBuilder common functions
- [`KT-33761`](https://youtrack.jetbrains.com/issue/KT-33761) reduceOrNull: reduce that doesn't throw on empty input
- [`KT-35347`](https://youtrack.jetbrains.com/issue/KT-35347) Create method Collection.randomOrNull()
- [`KT-36118`](https://youtrack.jetbrains.com/issue/KT-36118) Provide API for subtyping relationship between CoroutineContextKey and elements associated with this key

#### Fixes

- [`KT-17544`](https://youtrack.jetbrains.com/issue/KT-17544) JS: document array destructuring behavior
- [`KT-33141`](https://youtrack.jetbrains.com/issue/KT-33141) UnderMigration annotation is defined in Kotlin, but supposed to be used from Java
- [`KT-33447`](https://youtrack.jetbrains.com/issue/KT-33447) runCatching docs suggests it catches exceptions but it catches throwables
- [`KT-35175`](https://youtrack.jetbrains.com/issue/KT-35175) Clarify documentation for XorWowRandom
- [`KT-35299`](https://youtrack.jetbrains.com/issue/KT-35299) Float.rangeTo(Float): ClosedFloatingPointRange<Float> doesn't exist in the common stdlib.

### Reflection

- [`KT-14720`](https://youtrack.jetbrains.com/issue/KT-14720) Move KClass.cast / KClass.isInstance into kotlin-stdlib
- [`KT-33646`](https://youtrack.jetbrains.com/issue/KT-33646) Make KClass.simpleName available on JVM without kotlin-reflect.jar
- [`KT-34586`](https://youtrack.jetbrains.com/issue/KT-34586) Make KClass.qualifiedName available on JVM without kotlin-reflect.jar

### Tools. CLI

- [`KT-29933`](https://youtrack.jetbrains.com/issue/KT-29933) Support relative paths in -Xfriend-paths
- [`KT-34119`](https://youtrack.jetbrains.com/issue/KT-34119) Add JVM target bytecode version 13
- [`KT-34240`](https://youtrack.jetbrains.com/issue/KT-34240) CLI kotlinc help -include-runtime has redundant space

### Tools. Gradle

- [`KT-25206`](https://youtrack.jetbrains.com/issue/KT-25206) Delegate build/run to gradle results regularly in cannot delete proto.tab.value.s
- [`KT-35181`](https://youtrack.jetbrains.com/issue/KT-35181) Make kapt Gradle tasks compatible with instant execution

### Tools. Gradle. JS

#### New Features

- [`KT-30659`](https://youtrack.jetbrains.com/issue/KT-30659) Run NodeJS debugger when running debug gradle task from IDEA
- [`KT-32129`](https://youtrack.jetbrains.com/issue/KT-32129) Karma: support debugging
- [`KT-32179`](https://youtrack.jetbrains.com/issue/KT-32179) DSL: allow npm in root dependencies section of single platform projects
- [`KT-32283`](https://youtrack.jetbrains.com/issue/KT-32283) Webpack: Allow to configure Webpack mode
- [`KT-32323`](https://youtrack.jetbrains.com/issue/KT-32323) Webpack: support optimized webpack bundle
- [`KT-32785`](https://youtrack.jetbrains.com/issue/KT-32785) Webpack: Asset bundling in distributions folder

#### Fixes

- [`KT-30917`](https://youtrack.jetbrains.com/issue/KT-30917) Tests: Inner classes mapped incorrectly in short test fail message
- [`KT-31894`](https://youtrack.jetbrains.com/issue/KT-31894) ithout Kotlin sources `browserRun` makes the build fail
- [`KT-34946`](https://youtrack.jetbrains.com/issue/KT-34946) DCE require some/all transitive dependencies. Invalid compilation result otherwise
- [`KT-35318`](https://youtrack.jetbrains.com/issue/KT-35318) IllegalStateException on clean build with `left-pad` package and generateKotlinExternals=true
- [`KT-35428`](https://youtrack.jetbrains.com/issue/KT-35428) Gradle dependency with invalid package.json
- [`KT-35598`](https://youtrack.jetbrains.com/issue/KT-35598) Actualize NPM dependencies in 1.3.70
- [`KT-35599`](https://youtrack.jetbrains.com/issue/KT-35599) Actualize Node and Yarn versions in 1.3.70
- [`KT-36714`](https://youtrack.jetbrains.com/issue/KT-36714) Webpack output doesn't consider Kotlin/JS exports (library mode)

### Tools. Gradle. Multiplatform

- [`KT-31570`](https://youtrack.jetbrains.com/issue/KT-31570) Deprecate Kotlin 1.2.x MPP Gradle plugins
- [`KT-35126`](https://youtrack.jetbrains.com/issue/KT-35126) Support Gradle instant execution for Kotlin/JVM and Android tasks
- [`KT-36469`](https://youtrack.jetbrains.com/issue/KT-36469) Dependencies with compileOnly scope are not visible in Gradle build of MPP with source set hierarchies support

### Tools. Gradle. Native

- [`KT-29395`](https://youtrack.jetbrains.com/issue/KT-29395) Allow setting custom destination directory for Kotlin/Native binaries
- [`KT-31542`](https://youtrack.jetbrains.com/issue/KT-31542) Allow changing a name of a framework created by CocoaPods Gradle plugin
- [`KT-32750`](https://youtrack.jetbrains.com/issue/KT-32750) Support subspecs in CocoaPods plugin
- [`KT-35352`](https://youtrack.jetbrains.com/issue/KT-35352) MPP Gradle plugin: Support exporting K/N dependencies to shared and static libraries
- [`KT-35934`](https://youtrack.jetbrains.com/issue/KT-35934) Gradle MPP plugin: Spaces are not escaped in K/N compiler parameters
- [`KT-35958`](https://youtrack.jetbrains.com/issue/KT-35958) Kotlin/Native: Gradle: compiling test sources with no sources in main roots halts the Gradle daemon

### Tools. J2K

#### New Features

- [`KT-21811`](https://youtrack.jetbrains.com/issue/KT-21811) Convert string concatenation into multiline string

#### Performance Improvements

- [`KT-16774`](https://youtrack.jetbrains.com/issue/KT-16774) UI Freeze: J2K, PlainTextPasteImportResolve: IDEA freezes for 10+ seconds when copy-pasting Java code from external source to Kotlin file

#### Fixes

- [`KT-18001`](https://youtrack.jetbrains.com/issue/KT-18001) Multi-line comments parsed inside Kdoc comments
- [`KT-19574`](https://youtrack.jetbrains.com/issue/KT-19574) Code with inferred default parameters and parameter vs property name clashes
- [`KT-32551`](https://youtrack.jetbrains.com/issue/KT-32551) Non-canonical modifiers order inspection is not applied during convertion of inner super class
- [`KT-33637`](https://youtrack.jetbrains.com/issue/KT-33637) Property with getter is converted into incompailable code if backing field was not generated
- [`KT-34673`](https://youtrack.jetbrains.com/issue/KT-34673) First comment in function (if, for, while) block is moved to declaration line of block
- [`KT-35081`](https://youtrack.jetbrains.com/issue/KT-35081) Invalid code with block comment (Javadoc)
- [`KT-35152`](https://youtrack.jetbrains.com/issue/KT-35152) J2K breaks formatting by moving subsequent single line comments to first column
- [`KT-35395`](https://youtrack.jetbrains.com/issue/KT-35395) UninitializedPropertyAccessException through `org.jetbrains.kotlin.nj2k.conversions.ImplicitCastsConversion` when anonymous inner class passes itself as argument to outer method
- [`KT-35431`](https://youtrack.jetbrains.com/issue/KT-35431) "Invalid PSI class com.intellij.psi.PsiLambdaParameterType" with lambda argument in erroneous code
- [`KT-35476`](https://youtrack.jetbrains.com/issue/KT-35476) Expression with compound assignment logical operator is changing operator precedence without parentheses
- [`KT-35478`](https://youtrack.jetbrains.com/issue/KT-35478) Single line comment before constructor results in wrong code
- [`KT-35739`](https://youtrack.jetbrains.com/issue/KT-35739) Line break is not inserted for private property getter
- [`KT-35831`](https://youtrack.jetbrains.com/issue/KT-35831) Error on inserting plain text with `\r` char

### Tools. Scripts

- [`KT-34274`](https://youtrack.jetbrains.com/issue/KT-34274) Add support for `@CompilerOptions` annotation in `kotlin-main-kts`
- [`KT-34716`](https://youtrack.jetbrains.com/issue/KT-34716) Implement default cache in main-kts
- [`KT-34893`](https://youtrack.jetbrains.com/issue/KT-34893) Update apache ivy version in kotlin-main-kts
- [`KT-35413`](https://youtrack.jetbrains.com/issue/KT-35413) Implement "evaluate expression" command line parameter and functionality in the JVM cli compiler
- [`KT-35415`](https://youtrack.jetbrains.com/issue/KT-35415) Implement script and expression evaluation in the `kotlin` runner
- [`KT-35416`](https://youtrack.jetbrains.com/issue/KT-35416) load main-kts script definition by default in the jvm compiler, if the jar is available

### Tools. kapt

- [`KT-30164`](https://youtrack.jetbrains.com/issue/KT-30164) Default field value not transmitted to Java source model for mutable properties
- [`KT-30368`](https://youtrack.jetbrains.com/issue/KT-30368) Deprecated information not transmitted to Java source model
- [`KT-32832`](https://youtrack.jetbrains.com/issue/KT-32832) Turn worker API on by default
- [`KT-33617`](https://youtrack.jetbrains.com/issue/KT-33617) Java 9+: "IllegalStateException: Should not be called!"
- [`KT-34167`](https://youtrack.jetbrains.com/issue/KT-34167) Annotation Processor incorrectly marked as isolating causes full rebuild silently.
- [`KT-34258`](https://youtrack.jetbrains.com/issue/KT-34258) `kapt.incremental.apt=true` makes build failed after moving annotation processor files
- [`KT-34569`](https://youtrack.jetbrains.com/issue/KT-34569) Kapt doesn't handle methods with both the @Override annotation and `override` keyword
- [`KT-36113`](https://youtrack.jetbrains.com/issue/KT-36113) Enabling kapt.incremental.apt makes remote build cache miss via `classpathStructure$kotlin_gradle_plugin` property


